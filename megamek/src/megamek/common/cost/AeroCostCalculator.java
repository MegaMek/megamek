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
import megamek.common.EquipmentType;

public class AeroCostCalculator {

    public static double calculateCost(Aero aero, CalculationReport costReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(costReport, aero);
        double cost = 0;

        // Cockpit
        cost += 200000 + 50000 + (2000 * aero.getWeight());

        // Structural integrity
        cost += 50000 * aero.getSI();

        // Additional flight systems (attitude thruster and landing gear)
        cost += 25000 + (10 * aero.getWeight());

        // Engine
        if (aero.hasEngine()) {
            cost += (aero.getEngine().getBaseCost() * aero.getEngine().getRating() * aero.getWeight()) / 75.0;
        }

        // Fuel tanks
        cost += (200 * aero.getFuel()) / 80.0;

        // Armor
        if (aero.hasPatchworkArmor()) {
            for (int loc = 0; loc < aero.locations(); loc++) {
                cost += aero.getArmorWeight(loc) * EquipmentType.getArmorCost(aero.getArmorType(loc));
            }

        } else {
            cost += aero.getArmorWeight() * EquipmentType.getArmorCost(aero.getArmorType(0));
        }

        // Heat sinks
        int sinkCost = 2000 + (4000 * aero.getHeatType());
        cost += sinkCost * aero.getHeatSinks();

        double weaponCost = CostCalculator.getWeaponsAndEquipmentCost(aero, ignoreAmmo);
        cost += weaponCost;

        return Math.round(cost * aero.getPriceMultiplier());
    }
}
