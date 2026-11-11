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

package megamek.common.units;

import java.util.List;

import megamek.common.bays.Bay;
import megamek.common.board.Coords;
import megamek.common.moves.MobileStructureLinkage;

/** Authored transport doors, including the joined-linkage restriction in TO:AUE p.84. */
public final class BuildingBayDoors {
    private BuildingBayDoors() { }

    public static List<BuildingDesign.BayDoor> placements(AbstractBuildingEntity building, Bay bay) {
        return building.getDesign().getBayDoors().stream().filter(door -> door.bayNumber() == bay.getBayNumber()).toList();
    }

    /** The two board hexes sharing a door edge, interior first. */
    public static List<Coords> boardEdge(AbstractBuildingEntity building, BuildingDesign.BayDoor door) {
        Coords inside = building.relativeToBoard(door.position().hex());
        return List.of(inside, inside.translated(Math.floorMod(door.facing() + building.getFacing(), 6)));
    }

    public static boolean isUsable(AbstractBuildingEntity building, BuildingDesign.BayDoor door) {
        return physicallyAvailable(building, door, building.getPosition(), building.getFacing())
              && building.getBuildingRuntimeState().getDamagedBayDoors().getOrDefault(door, 0)
                    < building.getDesign().getBayDoors().stream().filter(door::equals).count();
    }

    /** The saved floor identity follows settling floors; a destroyed floor has no operational doorway. */
    public static int physicalLevel(AbstractBuildingEntity building, BuildingDesign.BayDoor door) {
        return BuildingElevation.currentFloor(building, door.position().hex(), door.position().level());
    }

    private static boolean physicallyAvailable(AbstractBuildingEntity building, BuildingDesign.BayDoor door,
          Coords position, int facing) {
        var hex = door.position().hex();
        var board = building.getGame() == null ? null : building.getGame().getBoard(building);
        Coords inside = position == null ? null : position.toCube().add(MobileStructureLinkage.rotate(hex, facing)).toOffset();
        return building.getInternalBuilding().getCoordsList().contains(hex)
              && building.getInternalBuilding().getCurrentCF(hex) > 0
              && physicalLevel(building, door) >= 0
              && (board == null || inside == null || board.contains(inside)
                    && board.contains(inside.translated(Math.floorMod(door.facing() + facing, 6))))
              && !(building instanceof MobileStructure mobile && MobileStructureLinkage.occupiedPort(mobile, hex));
    }

    private static List<Integer> undamagedPlacementIndices(AbstractBuildingEntity building, Bay bay) {
        var damage = new java.util.HashMap<>(building.getBuildingRuntimeState().getDamagedBayDoors());
        var result = new java.util.ArrayList<Integer>();
        var placements = placements(building, bay);
        for (int index = 0; index < placements.size(); index++) {
            var door = placements.get(index);
            int count = damage.getOrDefault(door, 0);
            if (count > 0) { damage.put(door, count - 1); }
            else { result.add(index); }
        }
        return result;
    }

    private static List<BuildingDesign.BayDoor> undamagedPlacements(AbstractBuildingEntity building, Bay bay) {
        var placements = placements(building, bay);
        return undamagedPlacementIndices(building, bay).stream().map(placements::get).toList();
    }

    /** Indices within this bay's authored placement list; equal doors always lose their first intact occurrence. */
    public static List<Integer> usablePlacementIndices(AbstractBuildingEntity building, Bay bay) {
        return usablePlacementIndices(building, bay, building.getPosition(), building.getFacing());
    }

    public static List<Integer> usablePlacementIndices(AbstractBuildingEntity building, Bay bay,
          Coords position, int facing) {
        var placements = placements(building, bay);
        return undamagedPlacementIndices(building, bay).stream()
              .filter(index -> physicallyAvailable(building, placements.get(index), position, facing))
              .limit(bay.getCurrentDoors()).toList();
    }

    /** One entry per operational physical door, including identical Hangar placements. */
    public static List<BuildingDesign.BayDoor> usablePlacements(AbstractBuildingEntity building, Bay bay) {
        return usablePlacements(building, bay, building.getPosition(), building.getFacing());
    }

    public static List<BuildingDesign.BayDoor> usablePlacements(AbstractBuildingEntity building, Bay bay,
          Coords position, int facing) {
        var placements = placements(building, bay);
        return usablePlacementIndices(building, bay, position, facing).stream().map(placements::get).toList();
    }

    /** Damage the used door, or choose an intact door for an untargeted critical hit. Legacy bays retain their count. */
    public static boolean damage(AbstractBuildingEntity building, Bay bay, BuildingDesign.BayDoor door) {
        if (!building.getTransportBays().contains(bay) || bay.getCurrentDoors() <= 0) { return false; }
        var undamaged = undamagedPlacements(building, bay);
        if (door != null && !undamaged.contains(door)) { return false; }
        if (door == null && !undamaged.isEmpty()) {
            door = usablePlacements(building, bay).stream().findFirst().orElse(undamaged.getFirst());
        }
        if (door != null) { building.getBuildingRuntimeState().getDamagedBayDoors().merge(door, 1, Integer::sum); }
        bay.setCurrentDoors(bay.getCurrentDoors() - 1);
        bay.setDoorsNext(Math.min(bay.getDoorsNext(), bay.getCurrentDoors()));
        return true;
    }

    /** Repair only this bay's recorded physical damage; obstructions and recovery timers remain independent. */
    public static int restore(AbstractBuildingEntity building, Bay bay, int count) {
        if (!building.getTransportBays().contains(bay)) { return 0; }
        int restored = Math.min(Math.max(0, count), bay.getDoors() - bay.getCurrentDoors());
        int remaining = restored;
        var damage = building.getBuildingRuntimeState().getDamagedBayDoors();
        var doors = placements(building, bay);
        for (int index = doors.size() - 1; index >= 0 && remaining > 0; index--) {
            var door = doors.get(index);
            int damaged = damage.getOrDefault(door, 0);
            int repaired = Math.min(remaining, damaged);
            if (repaired > 0) {
                if (repaired == damaged) { damage.remove(door); }
                else { damage.put(door, damaged - repaired); }
                remaining -= repaired;
            }
        }
        bay.setCurrentDoors(bay.getCurrentDoors() + restored);
        bay.setDoorsNext(bay.getCurrentDoors());
        return restored;
    }

    /** Explicitly clearing all door damage also reconciles old count-only repairs with authored state. */
    public static void restoreAll(AbstractBuildingEntity building, Bay bay) {
        if (!building.getTransportBays().contains(bay)) { return; }
        building.getBuildingRuntimeState().getDamagedBayDoors().keySet()
              .removeIf(door -> door.bayNumber() == bay.getBayNumber());
        bay.setCurrentDoors(bay.getDoors());
        bay.setDoorsNext(bay.getDoors());
    }

    /** Damage remains Bay's own state; temporary obstruction never destroys a door. Legacy unplaced bays retain it. */
    public static int usableDoors(AbstractBuildingEntity building, Bay bay) {
        return usableDoors(building, bay, building.getPosition(), building.getFacing());
    }

    public static int usableDoors(AbstractBuildingEntity building, Bay bay, Coords position, int facing) {
        var placements = placements(building, bay);
        if (placements.isEmpty()) {
            // No position can be inferred from a distributed bay's tonnage. A joined, unplaced bay cannot use a
            // guessed unblocked edge; assigning its doors resolves this without changing any old BLK on load.
            if (building instanceof MobileStructure mobile && MobileStructureLinkage.group(mobile).size() > 1) {
                return 0;
            }
            return bay.getCurrentDoors();
        }
        return Math.min(bay.getCurrentDoors(), usablePlacements(building, bay, position, facing).size());
    }
    /** Construction constraints concern exterior edges, regardless of which floors the doors use. */
    public static List<String> validationIssues(AbstractBuildingEntity building) {
        return validationIssues(building, true);
    }

    public static List<String> validationIssues(AbstractBuildingEntity building, boolean requireComplete) {
        var issues = new java.util.ArrayList<String>();
        var doors = building.getDesign().getBayDoors();
        var hexes = building.getInternalBuilding().getOriginalCoordsList();
        for (var door : doors) {
            if (building.getTransportBays().stream().noneMatch(bay -> bay.getBayNumber() == door.bayNumber())
                  || !hexes.contains(door.position().hex()) || door.position().level() < 0
                  || door.position().level() >= building.getInternalBuilding().getHeight(door.position().hex())
                  || door.facing() < 0 || door.facing() > 5
                  || hexes.contains(door.position().hex().toOffset().translated(door.facing()).toCube())) {
                issues.add("Bay doors must name an existing bay and an exterior edge on an occupied floor.");
                break;
            }
        }
        boolean modular = building instanceof MobileStructure && building.getEquipment().stream().anyMatch(mount ->
              mount.getType() instanceof megamek.common.equipment.BuildingEquipmentType facility
                    && facility.getFacility() == megamek.common.equipment.BuildingEquipmentType.Facility.MODULAR_LINKAGE);
        for (var bay : building.getTransportBays()) {
            long count = doors.stream().filter(door -> door.bayNumber() == bay.getBayNumber()).count();
            if (count > bay.getDoors() || requireComplete && (count > 0 || modular && !bay.isQuarters()) && count != bay.getDoors()) {
                issues.add("Assign all " + bay.getDoors() + " doors of bay " + bay.getBayNumber()
                      + " to exterior edges; bay space does not determine door positions.");
            }
        }
        if (building instanceof MobileStructure) {
            record Edge(megamek.common.board.CubeCoords hex, int facing) { }
            var counts = doors.stream().collect(java.util.stream.Collectors.groupingBy(
                  door -> new Edge(door.position().hex(), door.facing()), java.util.stream.Collectors.counting()));
            long exterior = hexes.stream().flatMap(hex -> hex.neighbors().stream()).filter(hex -> !hexes.contains(hex)).count();
            int perSide = building.getBldgClass() == IBuilding.HANGAR ? 2 : 1;
            if (counts.size() > exterior / 2 || counts.values().stream().anyMatch(count -> count > perSide)) {
                issues.add("Mobile bay doors may use half the exterior hexsides (round down), with " + perSide + " door(s) per designated side.");
            }
        }
        return List.copyOf(issues);
    }
}
