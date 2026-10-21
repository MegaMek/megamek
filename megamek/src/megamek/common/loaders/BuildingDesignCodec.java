/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.loaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.bays.Bay;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.Mounted;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingDesign;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.bayWeapons.BayWeapon;

/** Native BLK storage for authored building features. Equipment references use location/ordinal in the native blocks. */
public final class BuildingDesignCodec {
    /** Highest valid wall-sides value: one bit for each of the six hex sides. */
    private static final int MAX_WALL_SIDES_MASK = 63;

    private BuildingDesignCodec() { }

    public static void write(BuildingBlock block, AbstractBuildingEntity entity) {
        var design = entity.getDesign();
        List<String> options = new ArrayList<>();
        if (design.hasEnvironmentalSealing()) {
            options.add("sealing=true");
        }
        if (design.hasHeavyMetal()) {
            options.add("heavy_metal=true");
        }
        if (design.hasCivilianOfficers()) {
            options.add("civilian_officers=true");
        }
        if (design.isTunnel()) {
            options.add("tunnel=true");
        }
        if (design.isOpenSpace()) {
            options.add("open_space=true");
        }
        if (design.hasRoofClearance()) {
            options.add("roof_clearance=true");
        }
        if (design.getCeiling() != BuildingDesign.Ceiling.STANDARD) {
            options.add("ceiling=" + design.getCeiling());
        }
        if (design.getSite() != BuildingDesign.Site.SURFACE) {
            options.add("site=" + design.getSite());
            options.add("depth=" + design.getDepth());
        }
        if (design.getBaseLevel() != null) {
            options.add("base_level=" + design.getBaseLevel());
        }
        write(block, "building_options", options);
        var templates = new ArrayList<String>();
        if (design.getPortalHex2() != null) { templates.add("2;" + cube(design.getPortalHex2())); }
        if (design.getPortalHex3() != null) { templates.add("3;" + cube(design.getPortalHex3())); }
        write(block, "building_portal_templates", templates);
        write(block, "building_wall_sides", entity.getInternalBuilding().getOriginalCoordsList().stream()
              .filter(design.getWallSides()::containsKey).map(hex -> cube(hex) + ";" + design.wallSides(hex)).toList());
        write(block, "building_bridge_decks", entity.getInternalBuilding().getOriginalCoordsList().stream()
              .filter(design.getBridgeDecks()::containsKey).map(hex -> cube(hex) + ";" + design.bridgeDeck(hex)).toList());
        write(block, "building_doors", design.getDoors().stream()
              .map(door -> position(door.position()) + ";" + door.facing() + ";" + door.height()
                    + ";" + door.linkGroup()).toList());
        write(block, "building_elevators", design.getElevators().stream().map(lift -> cube(lift.hex()) + ";" + lift.capacity() + ";"
              + lift.exits().entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(exit -> exit.getKey() + "=" + exit.getValue()).collect(Collectors.joining(","))).toList());
        Map<Integer, List<Mounted<?>>> mountsByLocation = storedMountsByLocation(entity);
        List<String> equipment = new ArrayList<>();
        for (int location = 0; location < entity.locations(); location++) {
            addEquipmentLines(equipment, design, location, mountsByLocation.getOrDefault(location, List.of()));
        }
        write(block, "building_equipment_space", equipment);
        List<String> bays = new ArrayList<>();
        for (int index = 0; index < entity.getTransportBays().size(); index++) {
            var spaces = design.getBaySpace().get(entity.getTransportBays().get(index));
            if (spaces != null) {
                bays.add(index + ";" + spaces.stream().map(space -> position(space.position()) + "=" + space.tons())
                      .collect(Collectors.joining("|")));
            }
        }
        write(block, "building_bay_space", bays);
        write(block, "building_bay_doors", design.getBayDoors().stream()
              .map(door -> door.bayNumber() + ";" + position(door.position()) + ";" + door.facing()).toList());
    }

    /**
     * Writes one line for each mount at the given location that carries authored building data (automation, placed
     * space or a PCMT source). The ordinal written is the mount's index within the stored mounts of its location.
     */
    private static void addEquipmentLines(List<String> equipment, BuildingDesign design, int location,
          List<Mounted<?>> mounts) {
        for (int ordinal = 0; ordinal < mounts.size(); ordinal++) {
            Mounted<?> mount = mounts.get(ordinal);
            boolean automated = design.getAutomatedWeapons().contains(mount);
            List<BuildingDesign.Position> positions = design.getEquipmentSpace().getOrDefault(mount, List.of());
            if (automated || !positions.isEmpty() || design.getPcmtSources().containsKey(mount)) {
                equipment.add(location + "," + ordinal + ";" + automated + ";"
                      + positions.stream().map(BuildingDesignCodec::position).collect(Collectors.joining("|"))
                      + ";" + design.getPcmtSources().getOrDefault(mount, 0.0));
            }
        }
    }

    /**
     * Groups the entity's stored mounts (everything except implicit equipment and bay weapons) by location in one pass,
     * keeping equipment order within each location so a mount's index matches the ordinal used in the native blocks.
     */
    private static Map<Integer, List<Mounted<?>>> storedMountsByLocation(AbstractBuildingEntity entity) {
        Map<Integer, List<Mounted<?>>> mountsByLocation = new HashMap<>();
        for (Mounted<?> mount : entity.getEquipment()) {
            boolean isStored = !BLKFile.isImplicitEquipment(mount) && !(mount.getType() instanceof BayWeapon);
            if (isStored) {
                mountsByLocation.computeIfAbsent(mount.getLocation(), location -> new ArrayList<>()).add(mount);
            }
        }
        return mountsByLocation;
    }

    private static void write(BuildingBlock block, String key, List<String> lines) {
        if (!lines.isEmpty()) {
            block.writeBlockData(key, lines.toArray(String[]::new));
        }
    }

    private static String cube(CubeCoords hex) {
        return "%d,%d,%d".formatted((int) hex.q(), (int) hex.r(), (int) hex.s());
    }

    private static String position(BuildingDesign.Position position) {
        return cube(position.hex()) + "/" + position.level();
    }

    private static CubeCoords cube(String text) {
        String[] axes = parts(text, ",", 3);
        int cubeQ = Integer.parseInt(axes[0]);
        int cubeR = Integer.parseInt(axes[1]);
        int cubeS = Integer.parseInt(axes[2]);
        if ((long) cubeQ + cubeR + cubeS != 0) {
            throw new IllegalArgumentException("Invalid cube coordinates");
        }
        return new CubeCoords(cubeQ, cubeR, cubeS);
    }

    private static BuildingDesign.Position position(String text) {
        String[] values = parts(text, "/", 2);
        return new BuildingDesign.Position(cube(values[0]), Integer.parseInt(values[1]));
    }

    private static String[] parts(String text, String separator, int count) {
        String[] values = text.split(separator, -1);
        if (values.length != count) {
            throw new IllegalArgumentException("Invalid field count");
        }
        return values;
    }

    private static boolean flag(String text) {
        if (!"true".equals(text) && !"false".equals(text)) {
            throw new IllegalArgumentException("Expected true or false");
        }
        return Boolean.parseBoolean(text);
    }

    private static double tons(String text) {
        double tons = Double.parseDouble(text);
        if (!Double.isFinite(tons) || tons < 0) {
            throw new IllegalArgumentException("Expected non-negative tonnage");
        }
        return tons;
    }

    private static List<String> lines(BuildingBlock block, String key) {
        return block.exists(key) ? List.of(block.getDataAsString(key)) : List.of();
    }

    public static void read(BuildingBlock block, AbstractBuildingEntity entity) throws EntityLoadingException {
        BuildingDesign design = entity.getDesign();
        if (block.exists("building_equipment_space") && entity.getFailedEquipment().hasNext()) {
            throw new EntityLoadingException("Cannot resolve building equipment placements while equipment types are missing.");
        }
        Set<CubeCoords> footprint = new HashSet<>(entity.getInternalBuilding().getOriginalCoordsList());
        try {
            readHexValues(block, "building_wall_sides", design.getWallSides(), footprint, MAX_WALL_SIDES_MASK);
            readHexValues(block, "building_bridge_decks", design.getBridgeDecks(), footprint, Integer.MAX_VALUE);
            for (String line : lines(block, "building_options")) {
                String[] pair = parts(line, "=", 2);
                switch (pair[0]) {
                    case "sealing" -> design.setEnvironmentalSealing(flag(pair[1]));
                    case "heavy_metal" -> design.setHeavyMetal(flag(pair[1]));
                    case "civilian_officers" -> design.setCivilianOfficers(flag(pair[1]));
                    case "tunnel" -> design.setTunnel(flag(pair[1]));
                    case "open_space" -> design.setOpenSpace(flag(pair[1]));
                    case "roof_clearance" -> design.setRoofClearance(flag(pair[1]));
                    case "ceiling" -> design.setCeiling(BuildingDesign.Ceiling.valueOf(pair[1]));
                    case "site" -> design.setSite(BuildingDesign.Site.valueOf(pair[1]));
                    case "depth" -> design.setDepth(Integer.parseInt(pair[1]));
                    case "base_level" -> design.setBaseLevel(Integer.parseInt(pair[1]));
                    default -> throw new IllegalArgumentException("Unknown building option " + pair[0]);
                }
            }
            for (String line : lines(block, "building_portal_templates")) {
                String[] values = parts(line, ";", 2);
                CubeCoords hex = cube(values[1]);
                if (!footprint.contains(hex)) {
                    throw new IllegalArgumentException("Portal template is outside the footprint");
                }
                if (values[0].equals("2") && design.getPortalHex2() == null) {
                    design.setPortalHex2(hex);
                } else if (values[0].equals("3") && design.getPortalHex3() == null) {
                    design.setPortalHex3(hex);
                } else {
                    throw new IllegalArgumentException("Invalid or duplicate portal template role");
                }
            }
            for (String line : lines(block, "building_doors")) {
                String[] values = parts(line, ";", 4);
                int group = Integer.parseInt(values[3]);
                if (group < 0) {
                    throw new IllegalArgumentException("Invalid building door link group");
                }
                design.getDoors().add(new BuildingDesign.Door(position(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]), group));
            }
            for (String line : lines(block, "building_elevators")) {
                String[] values = parts(line, ";", 3);
                Map<Integer, Integer> exits = parseElevatorExits(values[2]);
                design.getElevators().add(new BuildingDesign.Elevator(cube(values[0]), tons(values[1]), exits));
            }
            readEquipmentSpace(block, entity, design);
            readBayDoors(block, entity, design, footprint);
            for (String line : lines(block, "building_bay_space")) {
                String[] values = parts(line, ";", 2);
                Bay bay = entity.getTransportBays().get(Integer.parseInt(values[0]));
                if (design.getBaySpace().put(bay, parseBaySpaces(values[1])) != null) {
                    throw new IllegalArgumentException("Duplicate bay placement");
                }
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw new EntityLoadingException("Invalid building construction data: " + ex.getMessage(), ex);
        }
    }

    /**
     * Reads a block of {@code hex;value} lines into the given map. Every hex must lie in the footprint, appear only
     * once and carry a value between 0 and {@code maximumValue}.
     */
    private static void readHexValues(BuildingBlock block, String key, Map<CubeCoords, Integer> values,
          Set<CubeCoords> footprint, int maximumValue) {
        for (String line : lines(block, key)) {
            String[] pair = parts(line, ";", 2);
            CubeCoords hex = cube(pair[0]);
            int value = Integer.parseInt(pair[1]);
            boolean isOutsideFootprint = !footprint.contains(hex);
            if (isOutsideFootprint || (values.putIfAbsent(hex, value) != null) || (value < 0) || (value > maximumValue)) {
                throw new IllegalArgumentException("Invalid or duplicate " + key + " entry");
            }
        }
    }

    /** Parses an elevator's {@code level=exits} list, rejecting a level that is listed twice. */
    private static Map<Integer, Integer> parseElevatorExits(String text) {
        Map<Integer, Integer> exits = new HashMap<>();
        for (String entry : text.split(",")) {
            String[] pair = parts(entry, "=", 2);
            if (exits.put(Integer.parseInt(pair[0]), Integer.parseInt(pair[1])) != null) {
                throw new IllegalArgumentException("Duplicate elevator stop");
            }
        }
        return exits;
    }

    /** Reads the equipment placement lines, resolving each {@code location,ordinal} reference to its mount. */
    private static void readEquipmentSpace(BuildingBlock block, AbstractBuildingEntity entity, BuildingDesign design) {
        List<String> equipmentLines = lines(block, "building_equipment_space");
        if (equipmentLines.isEmpty()) {
            return;
        }
        Map<Integer, List<Mounted<?>>> mountsByLocation = storedMountsByLocation(entity);
        Set<Mounted<?>> seenMounts = new LinkedHashSet<>();
        for (String line : equipmentLines) {
            String[] values = parts(line, ";", 4);
            String[] reference = parts(values[0], ",", 2);
            List<Mounted<?>> mounts = mountsByLocation.getOrDefault(Integer.parseInt(reference[0]), List.of());
            Mounted<?> mount = mounts.get(Integer.parseInt(reference[1]));
            if (!seenMounts.add(mount)) {
                throw new IllegalArgumentException("Duplicate equipment placement");
            }
            if (flag(values[1])) {
                design.getAutomatedWeapons().add(mount);
            }
            if (!values[2].isEmpty()) {
                design.getEquipmentSpace().put(mount, Arrays.stream(values[2].split("\\|"))
                      .map(BuildingDesignCodec::position).toList());
            }
            double source = tons(values[3]);
            if (source > 0) {
                design.getPcmtSources().put(mount, source);
            }
        }
    }

    /** Reads the bay door lines; each door must belong to an existing bay and sit on a real level of the footprint. */
    private static void readBayDoors(BuildingBlock block, AbstractBuildingEntity entity, BuildingDesign design,
          Set<CubeCoords> footprint) {
        Set<Integer> bayNumbers = new HashSet<>();
        for (Bay bay : entity.getTransportBays()) {
            bayNumbers.add(bay.getBayNumber());
        }
        for (String line : lines(block, "building_bay_doors")) {
            String[] values = parts(line, ";", 3);
            int bayNumber = Integer.parseInt(values[0]);
            BuildingDesign.Position position = position(values[1]);
            int facing = Integer.parseInt(values[2]);
            boolean isUnknownBay = !bayNumbers.contains(bayNumber);
            boolean isOutsideFootprint = !footprint.contains(position.hex());
            boolean isInvalidFacing = (facing < 0) || (facing > 5);
            boolean isInvalidLevel = (position.level() < 0)
                  || (position.level() >= entity.getInternalBuilding().getHeight(position.hex()));
            if (isUnknownBay || isOutsideFootprint || isInvalidFacing || isInvalidLevel) {
                throw new IllegalArgumentException("Invalid bay door placement");
            }
            design.getBayDoors().add(new BuildingDesign.BayDoor(bayNumber, position, facing));
        }
    }

    /** Parses a bay's {@code position=tons} entries separated by {@code |}; an empty text means no placed space. */
    private static List<BuildingDesign.Space> parseBaySpaces(String text) {
        List<BuildingDesign.Space> spaces = new ArrayList<>();
        if (!text.isEmpty()) {
            for (String entry : text.split("\\|")) {
                String[] pair = parts(entry, "=", 2);
                spaces.add(new BuildingDesign.Space(position(pair[0]), tons(pair[1])));
            }
        }
        return spaces;
    }
}
