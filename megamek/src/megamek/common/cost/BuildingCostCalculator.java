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
import megamek.common.enums.BuildingType;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingConstruction;
import megamek.common.units.IBuilding;

/** Costs for the native static building classifications (TO:AR, Building and Mobile Structure Costs, p. 208). */
public final class BuildingCostCalculator {
    private BuildingCostCalculator() {
    }

    public static double calculateCost(AbstractBuildingEntity building, CalculationReport report, boolean ignoreAmmo) {
        double rate = building.getBuildingType() == BuildingType.WALL ? 5000 : switch (building.getBldgClass()) {
            case IBuilding.TENT -> 1000;
            case IBuilding.CASTLE_BRIAN -> 1000000;
            case IBuilding.FENCE -> 800;
            case IBuilding.WALL -> 5000;
            case IBuilding.BRIDGE -> 12000;
            case IBuilding.HANGAR -> 8000;
            case IBuilding.GUN_EMPLACEMENT, IBuilding.FORTRESS -> 20000;
            default -> 10000;
        };
        double structureCost = 0;
        int height = building.getInternalBuilding().getBuildingHeight();
        for (int hex = 0; hex < building.getInternalBuilding().getOriginalCoordsList().size(); hex++) {
            structureCost += rate * building.getOInternal(hex * height) * height
                  * BuildingConstruction.segmentsInHex(building, building.getInternalBuilding().getOriginalCoordsList().get(hex));
        }
        double armorCost = building.getArmorWeight() * (building.isClan() ? 15000 : 10000);
        structureCost *= BuildingConstruction.structureMultiplier(building);
        double amplifierCost = building.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(building::getPowerAmplifierWeight).sum() * 20000;
        double turretCost = building.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(hex -> building.getTurretWeight(hex) * 5000 + building.getPintleWeight(hex) * 1000).sum();
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
