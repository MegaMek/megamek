/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.common.cost;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.enums.BuildingType;
import megamek.common.units.BuildingConstruction;
import megamek.common.units.BuildingEntity;
import megamek.common.units.IBuilding;

/** Costs for the native static building classifications (TO:AR, Building and Mobile Structure Costs, p. 208). */
public final class BuildingCostCalculator {
    private BuildingCostCalculator() {
    }

    public static double calculateCost(BuildingEntity building, CalculationReport report, boolean ignoreAmmo) {
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
