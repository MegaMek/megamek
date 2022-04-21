/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.cost;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.verifier.SupportVeeStructure;

public class FixedWingSupportCostCalculator {

    public static double calculateCost(FixedWingSupport fixedWingSupport, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[13 + fixedWingSupport.locations()];
        int i = 0;
        // Chassis cost for Support Vehicles

        double chassisCost = 2500 * SupportVeeStructure.getWeightStructure(fixedWingSupport);
        if (fixedWingSupport.hasMisc(MiscType.F_AMPHIBIOUS)) {
            chassisCost *= 1.25;
        }
        if (fixedWingSupport.hasMisc(MiscType.F_ARMORED_CHASSIS)) {
            chassisCost *= 2.0;
        }
        if (fixedWingSupport.hasMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
            chassisCost *= 1.75;
        }
        if (fixedWingSupport.hasMisc(MiscType.F_PROP)) {
            chassisCost *= 0.75;
        }
        if (fixedWingSupport.hasMisc(MiscType.F_STOL_CHASSIS)) {
            chassisCost *= 1.5;
        }
        if (fixedWingSupport.hasMisc(MiscType.F_ULTRA_LIGHT)) {
            chassisCost *= 1.5;
        }
        if (fixedWingSupport.hasMisc(MiscType.F_VSTOL_CHASSIS)) {
            chassisCost *= 2;
        }
        costs[i++] = chassisCost;

        // Engine Costs
        double engineCost = 0.0;
        if (fixedWingSupport.hasEngine()) {
            engineCost = 5000 * fixedWingSupport.getEngine().getWeightEngine(fixedWingSupport)
                    * Engine.getSVCostMultiplier(fixedWingSupport.getEngine().getEngineType());
        }
        costs[i++] = engineCost;

        // armor
        if (fixedWingSupport.getArmorType(fixedWingSupport.firstArmorIndex()) == EquipmentType.T_ARMOR_STANDARD) {
            int totalArmorPoints = 0;
            for (int loc = 0; loc < fixedWingSupport.locations(); loc++) {
                totalArmorPoints += fixedWingSupport.getOArmor(loc);
            }
            costs[i++] = totalArmorPoints *
                    EquipmentType.getSupportVehicleArmorCostPerPoint(fixedWingSupport.getBARRating(fixedWingSupport.firstArmorIndex()));
        } else {
            if (fixedWingSupport.hasPatchworkArmor()) {
                for (int loc = 0; loc < fixedWingSupport.locations(); loc++) {
                    costs[i++] = fixedWingSupport.getArmorWeight(loc) * EquipmentType.getArmorCost(fixedWingSupport.getArmorType(loc));
                }

            } else {
                costs[i++] = fixedWingSupport.getArmorWeight() * EquipmentType.getArmorCost(fixedWingSupport.getArmorType(0));
            }
        }

        // Compute final structural cost
        int structCostIdx = i++;
        for (int c = 0; c < structCostIdx; c++) {
            costs[structCostIdx] += costs[c];
        }
        double techRatingMultiplier = 0.5 + (fixedWingSupport.getStructuralTechRating() * 0.25);
        costs[structCostIdx] *= techRatingMultiplier;

        double freeHeatSinks = (fixedWingSupport.hasEngine() ? fixedWingSupport.getEngine().getWeightFreeEngineHeatSinks() : 0);
        int sinks = 0;
        double paWeight = 0;
        for (Mounted m : fixedWingSupport.getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)) {
                sinks += wt.getHeat();
                paWeight += m.getTonnage() / 10.0;
            }
        }
        paWeight = Math.ceil(paWeight * 2) / 2;
        if ((fixedWingSupport.hasEngine() && (fixedWingSupport.getEngine().isFusion() || fixedWingSupport.getEngine().getEngineType() == Engine.FISSION))
                || fixedWingSupport.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            paWeight = 0;
        }
        costs[i++] = 20000 * paWeight;
        costs[i++] = 2000 * Math.max(0, sinks - freeHeatSinks);

        costs[i++] = CostCalculator.getWeaponsAndEquipmentCost(fixedWingSupport, ignoreAmmo);

        double cost = 0; // calculate the total
        for (int x = structCostIdx; x < i; x++) {
            cost += costs[x];
        }
        if (fixedWingSupport.isOmni()) { // Omni conversion cost goes here.
            cost *= 1.25;
            costs[i++] = -1.25;
        } else {
            costs[i++] = 0;
        }

        cost *= fixedWingSupport.getPriceMultiplier();
        costs[i] = -fixedWingSupport.getPriceMultiplier();

        String[] systemNames = { "Chassis", "Engine", "Armor", "Final Structural Cost", "Power Amplifiers",
                "Heat Sinks", "Equipment", "Omni Multiplier", "Tonnage Multiplier" };
        CostCalculator.fillInReport(costReport, fixedWingSupport, ignoreAmmo, systemNames, 6, cost, costs);
        return Math.round(cost);
    }
}
