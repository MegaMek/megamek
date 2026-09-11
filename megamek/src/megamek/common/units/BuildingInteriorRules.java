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
import megamek.common.bays.CargoBay;
import megamek.common.bays.LiquidCargoBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.PowerGeneratorType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;

/** The contents-sensitive Advanced Building Movement Table, TO:AR pp.117–118. */
public final class BuildingInteriorRules {
    private BuildingInteriorRules() { }

    public record Features(int movement, int piloting, int shooting, boolean empty, boolean equipment, boolean emptyCargo) { }

    public static Features features(AbstractBuildingEntity building, Coords coords, int elevation) {
        CubeCoords hex = building.boardToRelative(coords);
        int level = BuildingElevation.floor(building, coords, elevation);
        boolean unspecified = false;
        boolean specified = false;
        boolean machinery = false;
        boolean emptyCargo = false;
        boolean dryCargo = false;
        boolean liquidCargo = false;
        for (Mounted<?> mount : building.getEquipment()) {
            if (mount.isDestroyed() || mount instanceof WeaponMounted weapon && building.isTurretMounted(weapon)) {
                continue;
            }
            boolean present = BuildingConstruction.equipmentPositions(building, mount).stream()
                  .anyMatch(position -> position.hex().equals(hex) && position.level() == level
                        && position.level() < building.getInternalBuilding().getHeight(hex));
            if (!present) {
                continue;
            }
            if (mount.getType() instanceof BuildingEquipmentType facility
                  && facility.getFacility() == BuildingEquipmentType.Facility.UNSPECIFIED) {
                unspecified = true;
            } else if (mount.getType() instanceof PowerGeneratorType || mount.getType() instanceof WeaponType) {
                machinery = true;
            } else {
                specified = true;
            }
        }
        for (Bay bay : building.getTransportBays()) {
            boolean present = BuildingConstruction.baySpaces(building, bay).stream()
                  .anyMatch(space -> space.tons() > 0 && space.position().hex().equals(hex)
                        && space.position().level() == level);
            if (!present) {
                continue;
            }
            if (bay instanceof CargoBay || bay instanceof LiquidCargoBay) {
                if (bay.getUnused() >= bay.getCapacity()) {
                    emptyCargo = true;
                } else if (bay instanceof LiquidCargoBay) {
                    liquidCargo = true;
                } else {
                    dryCargo = true;
                }
            } else {
                // Accommodation and transport facilities are specified interior equipment.
                specified = true;
            }
        }
        boolean equipment = unspecified || specified || machinery || dryCargo || liquidCargo;
        int movement = (emptyCargo ? -1 : 0) + (liquidCargo ? 1 : 0) + (specified ? 1 : 0) + (machinery ? 2 : 0);
        int piloting = (emptyCargo ? -3 : 0) + (dryCargo ? -2 : 0) + (liquidCargo ? 2 : 0)
              + (specified ? 1 : 0) + (machinery ? 2 : 0);
        int shooting = (unspecified ? 1 : 0) + (specified ? 2 : 0) + (machinery ? 2 : 0);
        return new Features(movement, piloting, shooting, !equipment, equipment, emptyCargo);
    }

    public static boolean hallFits(AbstractBuildingEntity building, Entity unit, Coords coords) {
        return (building.getBldgClass() == IBuilding.HANGAR || building.getDesign().isTunnel()
                    || building.getDesign().isOpenSpace())
              && building.getHeight(coords) >= unit.height() + 1;
    }

    public static boolean exteriorWall(IBuilding building, Coords from, Coords to, int elevation) {
        return BuildingElevation.contains(building, from, elevation) != BuildingElevation.contains(building, to, elevation);
    }

    public static boolean paved(AbstractBuildingEntity building, Entity unit, Coords from, Coords to, int elevation) {
        return hallFits(building, unit, to) && !exteriorWall(building, from, to, elevation)
              && (building.getDesign().isOpenSpace() || features(building, to, elevation).empty());
    }

    public static int movementModifier(AbstractBuildingEntity building, Entity unit, Coords from, Coords to,
          int elevation) {
        Features features = features(building, to, elevation);
        int hall = hallFits(building, unit, to) && features.equipment() && !features.emptyCargo() ? -1 : 0;
        // Empty cargo and an equipped hangar are the same row of the table, applied once.
        return features.movement() + hall
              + building.getDesign().movementMPModifier();
    }

    public static int pilotingModifier(AbstractBuildingEntity building, Entity unit, Coords coords, int elevation) {
        Features features = features(building, coords, elevation);
        int hall = hallFits(building, unit, coords) && features.equipment() && !features.emptyCargo() ? -3 : 0;
        return features.piloting() + hall + building.getDesign().movementPilotingModifier();
    }

    public static int baseMovement(IBuilding building, Entity unit) {
        if (unit instanceof ProtoMek) {
            return 1;
        }
        int type = building.getBuildingType().getTypeValue();
        return type + (building.getBldgClass() == IBuilding.HANGAR ? -1
              : building.getBldgClass() == IBuilding.FORTRESS || building.getBldgClass() == IBuilding.CASTLE_BRIAN ? 1 : 0);
    }

    /** Return the innermost building shared by both endpoints, or their open-space enclosure. */
    public static IBuilding shared(megamek.common.game.Game game, Coords from, int fromElevation, Coords to,
          int toElevation, int boardId) {
        List<IBuilding> candidates = game.getBoard(boardId).getBuildingsAt(from);
        return candidates.stream().filter(building -> BuildingElevation.contains(building, from, fromElevation)
                    && BuildingElevation.contains(building, to, toElevation))
              .min(java.util.Comparator.comparingInt(building -> building instanceof AbstractBuildingEntity authored
                    && authored.getDesign().isOpenSpace() ? Integer.MAX_VALUE : building.getCoordsList().size()))
              .orElse(null);
    }
}
