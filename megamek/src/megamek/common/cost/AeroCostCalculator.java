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
import megamek.common.units.Aero;
import megamek.common.equipment.ArmorType;

public class AeroCostCalculator {

    public static double calculateCost(Aero aero, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[15];
        int idx = 0;

        // Cockpit, Life Support, Sensors, Structure and Additional Flight Systems
        costs[idx++] = 200000;
        costs[idx++] = 50000;
        costs[idx++] = 2000 * aero.getWeight();
        costs[idx++] = 50000 * aero.getSI();
        costs[idx++] = 25000 + 10 * aero.getWeight();

        // Engine
        if (aero.hasEngine()) {
            costs[idx++] = (aero.getEngine().getBaseCost() * aero.getEngine().getRating() * aero.getWeight()) / 75.0;
        }

        // Fuel tanks
        costs[idx++] = (200 * aero.getFuel()) / 80.0;

        // Armor
        if (aero.hasPatchworkArmor()) {
            double armorCost = 0;
            for (int loc = 0; loc < aero.locations(); loc++) {
                armorCost += aero.getArmorWeight(loc) * ArmorType.forEntity(aero, loc).getCost();
            }
            costs[idx++] = armorCost;
        } else {
            costs[idx++] = aero.getArmorWeight() * ArmorType.forEntity(aero).getCost();
        }

        // Heat sinks
        int sinkCost = 2000 + (4000 * aero.getHeatType());
        costs[idx++] = sinkCost * aero.getHeatSinks();

        // Weapons and equipment
        costs[idx++] = CostCalculator.getWeaponsAndEquipmentCost(aero, ignoreAmmo);

        // For all additive costs - replace negatives with 0 to separate from multipliers
        CostCalculator.removeNegativeAdditiveCosts(costs);

        costs[idx] = -aero.getPriceMultiplier();
        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Cockpit", "Life Support", "Sensors", "Structure", "Flight Systems", "Engine",
                                 "Fuel Tanks", "Armor", "Heat Sinks", "Equipment", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, aero, ignoreAmmo, systemNames, 9, cost, costs);

        return Math.round(cost);
    }
}
