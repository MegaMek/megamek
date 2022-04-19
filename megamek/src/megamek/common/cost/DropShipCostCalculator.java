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

public class DropShipCostCalculator {

    public static double calculateCost(Dropship dropShip, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[20];
        int idx = SmallCraftCostCalculator.addBaseCosts(costs, dropShip, ignoreAmmo);

        // Docking Collar
        if (dropShip.getCollarType() == Dropship.COLLAR_STANDARD) {
            costs[idx++] = 10000;
        } else if (dropShip.getCollarType() == Dropship.COLLAR_PROTOTYPE) {
            costs[idx++] = 1010000;
        }

        // Transport Bays
        int baydoors = 0;
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure
        // and don't add to the cost
        for (Bay bay : dropShip.getTransportBays()) {
            baydoors += bay.getDoors();
            if (!bay.isQuarters() && !(bay instanceof InfantryBay) && !(bay instanceof BattleArmorBay)) {
                bayCost += bay.getCost();
            }
        }
        costs[idx++] = bayCost + (baydoors * 1000L);
        costs[idx++] = quartersCost;

        // Life Boats and Escape Pods
        costs[idx++] += 5000 * (dropShip.getLifeBoats() + dropShip.getEscapePods());

        costs[idx] = -dropShip.getPriceMultiplier();
        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Bridge", "Computer", "Life Support", "Sensors", "Fire Control Computer",
                "Gunnery Control Systems", "Structural Integrity", "Attitude Thruster", "Landing Gear",
                "Engine", "Drive Unit", "Fuel Tanks", "Armor", "Heat Sinks", "Weapons/Equipment", "Docking Collar",
                "Bays", "Quarters", "Life Boats/Escape Pods", "Final Multiplier" };
        CostCalculator.fillInReport(costReport, dropShip, ignoreAmmo, systemNames, 14, cost, costs);
        return Math.round(cost);
    }
}
