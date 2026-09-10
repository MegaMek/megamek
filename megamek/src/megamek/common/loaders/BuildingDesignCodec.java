/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.common.loaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.board.CubeCoords;
import megamek.common.equipment.Mounted;
import megamek.common.units.BuildingDesign;
import megamek.common.units.BuildingEntity;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.bayWeapons.BayWeapon;

/** Native BLK storage for authored building features. Equipment references use location/ordinal in the native blocks. */
public final class BuildingDesignCodec {
    private BuildingDesignCodec() { }

    public static void write(BuildingBlock block, BuildingEntity entity) {
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
        write(block, "building_options", options);
        write(block, "building_wall_sides", entity.getInternalBuilding().getOriginalCoordsList().stream()
              .filter(design.getWallSides()::containsKey).map(hex -> cube(hex) + ";" + design.wallSides(hex)).toList());
        write(block, "building_bridge_decks", entity.getInternalBuilding().getOriginalCoordsList().stream()
              .filter(design.getBridgeDecks()::containsKey).map(hex -> cube(hex) + ";" + design.bridgeDeck(hex)).toList());
        write(block, "building_doors", design.getDoors().stream()
              .map(door -> position(door.position()) + ";" + door.facing() + ";" + door.height()).toList());
        write(block, "building_elevators", design.getElevators().stream().map(lift -> cube(lift.hex()) + ";" + lift.capacity() + ";"
              + lift.exits().entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(exit -> exit.getKey() + "=" + exit.getValue()).collect(Collectors.joining(","))).toList());
        List<String> equipment = new ArrayList<>();
        for (int loc = 0; loc < entity.locations(); loc++) {
            List<Mounted<?>> mounts = storedMounts(entity, loc);
            for (int ordinal = 0; ordinal < mounts.size(); ordinal++) {
                Mounted<?> mount = mounts.get(ordinal);
                boolean automated = design.getAutomatedWeapons().contains(mount);
                List<BuildingDesign.Position> positions = design.getEquipmentSpace().getOrDefault(mount, List.of());
                if (automated || !positions.isEmpty() || design.getPcmtSources().containsKey(mount)) {
                    equipment.add(loc + "," + ordinal + ";" + automated + ";"
                          + positions.stream().map(BuildingDesignCodec::position).collect(Collectors.joining("|"))
                          + ";" + design.getPcmtSources().getOrDefault(mount, 0.0));
                }
            }
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
    }

    private static List<Mounted<?>> storedMounts(BuildingEntity entity, int location) {
        return entity.getEquipment().stream().filter(m -> m.getLocation() == location && !BLKFile.isImplicitEquipment(m)
              && !(m.getType() instanceof BayWeapon)).toList();
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
        int q = Integer.parseInt(axes[0]);
        int r = Integer.parseInt(axes[1]);
        int s = Integer.parseInt(axes[2]);
        if ((long) q + r + s != 0) {
            throw new IllegalArgumentException("Invalid cube coordinates");
        }
        return new CubeCoords(q, r, s);
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

    public static void read(BuildingBlock block, BuildingEntity entity) throws EntityLoadingException {
        var design = entity.getDesign();
        if (block.exists("building_equipment_space") && entity.getFailedEquipment().hasNext()) {
            throw new EntityLoadingException("Cannot resolve building equipment placements while equipment types are missing.");
        }
        try {
            for (String key : List.of("building_wall_sides", "building_bridge_decks")) {
                var values = "building_wall_sides".equals(key) ? design.getWallSides() : design.getBridgeDecks();
                for (String line : lines(block, key)) {
                    String[] pair = parts(line, ";", 2);
                    CubeCoords hex = cube(pair[0]);
                    int value = Integer.parseInt(pair[1]);
                    if (!entity.getInternalBuilding().getOriginalCoordsList().contains(hex)
                          || values.putIfAbsent(hex, value) != null || value < 0
                          || ("building_wall_sides".equals(key) && value > 63)) {
                        throw new IllegalArgumentException("Invalid or duplicate " + key + " entry");
                    }
                }
            }
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
                    default -> throw new IllegalArgumentException("Unknown building option " + pair[0]);
                }
            }
            for (String line : lines(block, "building_doors")) {
                String[] values = parts(line, ";", 3);
                design.getDoors().add(new BuildingDesign.Door(position(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2])));
            }
            for (String line : lines(block, "building_elevators")) {
                String[] values = parts(line, ";", 3);
                Map<Integer, Integer> exits = new HashMap<>();
                for (String entry : values[2].split(",")) {
                    String[] pair = parts(entry, "=", 2);
                    if (exits.put(Integer.parseInt(pair[0]), Integer.parseInt(pair[1])) != null) {
                        throw new IllegalArgumentException("Duplicate elevator stop");
                    }
                }
                design.getElevators().add(new BuildingDesign.Elevator(cube(values[0]), tons(values[1]), exits));
            }
            var seenMounts = new LinkedHashSet<Mounted<?>>();
            for (String line : lines(block, "building_equipment_space")) {
                String[] values = parts(line, ";", 4);
                String[] reference = parts(values[0], ",", 2);
                Mounted<?> mount = storedMounts(entity, Integer.parseInt(reference[0])).get(Integer.parseInt(reference[1]));
                if (!seenMounts.add(mount)) {
                    throw new IllegalArgumentException("Duplicate equipment placement");
                }
                if (flag(values[1])) {
                    design.getAutomatedWeapons().add(mount);
                }
                if (!values[2].isEmpty()) {
                    design.getEquipmentSpace().put(mount, java.util.Arrays.stream(values[2].split("\\|"))
                          .map(BuildingDesignCodec::position).toList());
                }
                double source = tons(values[3]);
                if (source > 0) {
                    design.getPcmtSources().put(mount, source);
                }
            }
            for (String line : lines(block, "building_bay_space")) {
                String[] values = parts(line, ";", 2);
                var bay = entity.getTransportBays().get(Integer.parseInt(values[0]));
                List<BuildingDesign.Space> spaces = new ArrayList<>();
                if (!values[1].isEmpty()) {
                    for (String entry : values[1].split("\\|")) {
                        String[] pair = parts(entry, "=", 2);
                        spaces.add(new BuildingDesign.Space(position(pair[0]), tons(pair[1])));
                    }
                }
                if (design.getBaySpace().put(bay, spaces) != null) {
                    throw new IllegalArgumentException("Duplicate bay placement");
                }
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw new EntityLoadingException("Invalid building construction data: " + ex.getMessage(), ex);
        }
    }
}
