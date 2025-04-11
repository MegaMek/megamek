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
 */
package megamek.common.cost;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.Dropship;
import megamek.common.bays.BattleArmorBay;
import megamek.common.bays.Bay;
import megamek.common.bays.InfantryBay;

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
        int bayDoors = 0;
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure and don't add to the cost
        for (Bay bay : dropShip.getTransportBays()) {
            bayDoors += bay.getDoors();
            if (!bay.isQuarters() && !(bay instanceof InfantryBay) && !(bay instanceof BattleArmorBay)) {
                bayCost += bay.getCost();
            }
        }
        costs[idx++] = bayCost + (bayDoors * 1000L);
        costs[idx++] = quartersCost;

        // Lifeboats and Escape Pods
        costs[idx++] += 5000 * (dropShip.getLifeBoats() + dropShip.getEscapePods());

        // For all additive costs - replace negatives with 0 to separate from multipliers
        CostCalculator.removeNegativeAdditiveCosts(costs);

        costs[idx] = -dropShip.getPriceMultiplier();
        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Bridge", "Computer", "Life Support", "Sensors", "Fire Control Computer",
                                 "Gunnery Control Systems", "Structural Integrity", "Attitude Thruster", "Landing Gear",
                                 "Engine", "Drive Unit", "Fuel Tanks", "Armor", "Heat Sinks", "Weapons/Equipment",
                                 "Docking Collar", "Bays", "Quarters", "Life Boats/Escape Pods", "Final Multiplier" };
        CostCalculator.fillInReport(costReport, dropShip, ignoreAmmo, systemNames, 14, cost, costs);
        return Math.round(cost);
    }
}
