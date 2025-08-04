/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.Engine;
import megamek.common.EntityWeightClass;
import megamek.common.FixedWingSupport;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.equipment.ArmorType;
import megamek.common.verifier.SupportVeeStructure;

public class FixedWingSupportCostCalculator {

    public static double calculateCost(FixedWingSupport fixedWingSupport, CalculationReport costReport,
          boolean ignoreAmmo) {
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
        if (fixedWingSupport.hasPatchworkArmor()) {
            for (int loc = 0; loc < fixedWingSupport.locations(); loc++) {
                costs[i++] = fixedWingSupport.getArmorWeight(loc)
                      * ArmorType.forEntity(fixedWingSupport, loc).getCost();
            }
        } else {
            ArmorType armor = ArmorType.forEntity(fixedWingSupport);
            if (armor.hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR)) {
                costs[i++] = fixedWingSupport.getTotalOArmor() * armor.getCost();
            } else {
                costs[i++] = fixedWingSupport.getArmorWeight() * ArmorType.forEntity(fixedWingSupport).getCost();
            }
        }

        // Compute final structural cost
        int structCostIdx = i++;
        for (int c = 0; c < structCostIdx; c++) {
            costs[structCostIdx] += costs[c];
        }
        double techRatingMultiplier = 0.5 + (fixedWingSupport.getStructuralTechRating().getIndex() * 0.25);
        costs[structCostIdx] *= techRatingMultiplier;

        double freeHeatSinks = (fixedWingSupport.hasEngine()
              ? fixedWingSupport.getEngine().getWeightFreeEngineHeatSinks()
              : 0);
        int sinks = 0;
        double paWeight = 0;
        for (Mounted<?> m : fixedWingSupport.getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)) {
                sinks += wt.getHeat();
                paWeight += m.getTonnage() / 10.0;
            }
        }
        paWeight = Math.ceil(paWeight * 2) / 2;
        if ((fixedWingSupport.hasEngine() && (fixedWingSupport.getEngine().isFusion()
              || fixedWingSupport.getEngine().getEngineType() == Engine.FISSION))
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
