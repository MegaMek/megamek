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

public class SmallCraftCostCalculator {

    public static double calculateCost(SmallCraft smallCraft, CalculationReport costReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(costReport, smallCraft);
        double cost = 0;

        // add in controls
        // bridge
        cost += 200000 + (10 * smallCraft.getWeight());
        // computer
        cost += 200000;
        // life support
        cost += 5000 * (smallCraft.getNCrew() + smallCraft.getNPassenger());
        // sensors
        cost += 80000;
        // fcs
        cost += 100000;
        // gunnery/control systems
        cost += 10000 * smallCraft.getArcswGuns();

        // structural integrity
        cost += 100000 * smallCraft.getSI();

        // additional flight systems (attitude thruster and landing gear)
        cost += 25000 + (10 * smallCraft.getWeight());

        // engine
        double engineMultiplier = 0.065;
        if (smallCraft.isClan()) {
            engineMultiplier = 0.061;
        }
        double engineWeight = smallCraft.getOriginalWalkMP() * smallCraft.getWeight() * engineMultiplier;
        cost += engineWeight * 1000;
        // drive unit
        cost += (500 * smallCraft.getOriginalWalkMP() * smallCraft.getWeight()) / 100.0;

        // fuel tanks
        cost += (200 * smallCraft.getFuel()) / 80.0 * 1.02;

        // armor
        cost += smallCraft.getArmorWeight() * EquipmentType.getArmorCost(smallCraft.getArmorType(0));

        // heat sinks
        int sinkCost = 2000 + (4000 * smallCraft.getHeatType());
        cost += sinkCost * smallCraft.getHeatSinks();

        // weapons
        cost += CostCalculator.getWeaponsAndEquipmentCost(smallCraft, ignoreAmmo);

        return Math.round(cost * smallCraft.getPriceMultiplier());
    }
}
