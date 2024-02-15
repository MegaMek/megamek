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
import megamek.common.Aero;
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
            int armorcost = 0;
            for (int loc = 0; loc < aero.locations(); loc++) {
                armorcost += aero.getArmorWeight(loc) * ArmorType.forEntity(aero, loc).getCost();
            }
            costs[idx++] = armorcost;
        } else {
            costs[idx++] = aero.getArmorWeight() * ArmorType.forEntity(aero).getCost();
        }

        // Heat sinks
        int sinkCost = 2000 + (4000 * aero.getHeatType());
        costs[idx++] = sinkCost * aero.getHeatSinks();

        costs[idx++] = CostCalculator.getWeaponsAndEquipmentCost(aero, ignoreAmmo);
        costs[idx] = -aero.getPriceMultiplier();
        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Cockpit", "Life Support", "Sensors", "Structure", "Flight Systems", "Engine",
                "Fuel Tanks", "Armor", "Heat Sinks", "Equipment", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, aero, ignoreAmmo, systemNames, 9, cost, costs);

        return Math.round(cost);
    }
}
