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

package megamek.common.cost;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingConstruction;
import megamek.common.units.IBuilding;

/** Costs for the native static building classifications (TO:AR, Building and Mobile Structure Costs, p. 208). */
public final class BuildingCostCalculator {
    /** C-bills per point of CF per level per hex for a tent (TO:AR p. 208). */
    private static final double TENT_RATE = 1000;
    /** C-bills per point of CF per level per hex for a Castle Brian (TO:AR p. 208). */
    private static final double CASTLE_BRIAN_RATE = 1000000;
    /** C-bills per point of CF per level per hex for a fence (TO:AR p. 208). */
    private static final double FENCE_RATE = 800;
    /** C-bills per point of CF per level per hex for a wall (TO:AR p. 208). */
    private static final double WALL_RATE = 5000;
    /** C-bills per point of CF per level per hex for a bridge (TO:AR p. 208). */
    private static final double BRIDGE_RATE = 12000;
    /** C-bills per point of CF per level per hex for a hangar (TO:AR p. 208). */
    private static final double HANGAR_RATE = 8000;
    /** C-bills per point of CF per level per hex for a gun emplacement or fortress (TO:AR p. 208). */
    private static final double FORTIFIED_RATE = 20000;
    /** C-bills per point of CF per level per hex for every other building classification (TO:AR p. 208). */
    private static final double STANDARD_BUILDING_RATE = 10000;
    /** C-bills per ton of Inner Sphere building armor (TO:AR p. 208). */
    private static final double INNER_SPHERE_ARMOR_COST_PER_TON = 10000;
    /** C-bills per ton of Clan building armor (TO:AR p. 208). */
    private static final double CLAN_ARMOR_COST_PER_TON = 15000;
    /** C-bills per ton of power amplifiers (TO:AR p. 208). */
    private static final double POWER_AMPLIFIER_COST_PER_TON = 20000;
    /** C-bills per ton of turret mechanism (TO:AR p. 208). */
    private static final double TURRET_COST_PER_TON = 5000;
    /** C-bills per ton of pintle mount (TO:AR p. 208). */
    private static final double PINTLE_COST_PER_TON = 1000;

    private BuildingCostCalculator() {
    }

    public static double calculateCost(AbstractBuildingEntity building, CalculationReport report, boolean ignoreAmmo) {
        double rate = building.getBuildingType() == BuildingType.WALL ? WALL_RATE : switch (building.getBldgClass()) {
            case IBuilding.TENT -> TENT_RATE;
            case IBuilding.CASTLE_BRIAN -> CASTLE_BRIAN_RATE;
            case IBuilding.FENCE -> FENCE_RATE;
            case IBuilding.WALL -> WALL_RATE;
            case IBuilding.BRIDGE -> BRIDGE_RATE;
            case IBuilding.HANGAR -> HANGAR_RATE;
            case IBuilding.GUN_EMPLACEMENT, IBuilding.FORTRESS -> FORTIFIED_RATE;
            default -> STANDARD_BUILDING_RATE;
        };
        double structureCost = 0;
        int height = building.getInternalBuilding().getBuildingHeight();
        for (int hex = 0; hex < building.getInternalBuilding().getOriginalCoordsList().size(); hex++) {
            CubeCoords hexCoords = building.getInternalBuilding().getOriginalCoordsList().get(hex);
            // A mobile structure's hex can be shorter than the structure (hex_heights); it pays only for its own levels
            int hexHeight = building.getInternalBuilding().getHeight(hexCoords);
            structureCost += rate * building.getOInternal(hex * height) * hexHeight
                  * BuildingConstruction.segmentsInHex(building, hexCoords);
        }
        double armorCost = building.getArmorWeight() * (building.isClan() ? CLAN_ARMOR_COST_PER_TON : INNER_SPHERE_ARMOR_COST_PER_TON);
        structureCost *= BuildingConstruction.structureMultiplier(building);
        double amplifierCost = building.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(building::getPowerAmplifierWeight).sum() * POWER_AMPLIFIER_COST_PER_TON;
        double turretCost = building.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(hex -> building.getTurretWeight(hex) * TURRET_COST_PER_TON
                    + building.getPintleWeight(hex) * PINTLE_COST_PER_TON).sum();
        double equipmentCost = CostCalculator.getWeaponsAndEquipmentCost(building, ignoreAmmo);
        double additions = BuildingConstruction.additionalCost(building);
        double multiplier = 1 + building.getOInternal(0) * building.getConstructionCFScale() / 100.0;
        double total = (structureCost + armorCost + amplifierCost + turretCost + equipmentCost + additions) * multiplier;
        if (report != null) {
            CostCalculator.fillInReport(report, building, ignoreAmmo,
                  new String[] { "Structure", "Armor", "Power Amplifiers", "Turret Mechanisms", "Equipment", "Construction Options", "CF multiplier" }, 4, total,
                  new double[] { structureCost, armorCost, amplifierCost, turretCost, equipmentCost, additions, -multiplier });
        }
        return total;
    }
}
