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
import megamek.common.SmallCraft;
import megamek.common.equipment.ArmorType;
import megamek.common.verifier.TestSmallCraft;

public class SmallCraftCostCalculator {

    public static double calculateCost(SmallCraft smallCraft, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[16];
        int idx = addBaseCosts(costs, smallCraft, ignoreAmmo);

        // For all additive costs - replace negatives with 0 to separate from multipliers
        CostCalculator.removeNegativeAdditiveCosts(costs);

        costs[idx] = -smallCraft.getPriceMultiplier();
        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Bridge", "Computer", "Life Support", "Sensors", "Fire Control Computer",
                                 "Gunnery Control Systems", "Structure", "Attitude Thruster", "Landing Gear", "Engine",
                                 "Drive Unit", "Fuel Tanks", "Armor", "Heat Sinks", "Equipment", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, smallCraft, ignoreAmmo, systemNames, 14, cost, costs);
        return Math.round(cost);
    }

    /**
     * Adds those costs to the given costs array that are used in both DropShips and SmallCraft.
     *
     * @param costs      The costs array used to store individual cost items. Should be empty
     * @param smallCraft The SmallCraft or DropShip
     * @param ignoreAmmo When true, ammo will not be added with the weapons and equipment
     *
     * @return The last index used in the costs array + 1, i.e. the first free index
     */
    static int addBaseCosts(double[] costs, SmallCraft smallCraft, boolean ignoreAmmo) {
        int idx = 0;
        TestSmallCraft testSmallCraft = new TestSmallCraft(smallCraft, null, null);

        // Bridge, Computer, Life Support, Sensors, Fire Control Computer, Gunnery Control Systems,
        costs[idx++] = 200000 + (10 * smallCraft.getWeight());
        costs[idx++] = 200000;
        costs[idx++] = 5000 * (smallCraft.getNCrew() + smallCraft.getNPassenger());
        costs[idx++] = 80000;
        costs[idx++] = 100000;
        costs[idx++] = 10000 * smallCraft.getArcswGuns();

        // Structure, Attitude Thruster, Landing Gear
        costs[idx++] = 100000 * smallCraft.getSI();
        costs[idx++] = 25000;
        costs[idx++] = 10 * smallCraft.getWeight();

        // Engine, Drive Unit, Fuel Tanks, Armor, Heat sinks, Equipment
        costs[idx++] = testSmallCraft.getWeightEngine() * 1000;
        costs[idx++] = (500 * smallCraft.getOriginalWalkMP() * smallCraft.getWeight()) / 100.0;
        costs[idx++] = 200 * testSmallCraft.getWeightFuel();
        costs[idx++] = smallCraft.getArmorWeight() * ArmorType.forEntity(smallCraft).getCost();
        int sinkCost = 2000 + (4000 * smallCraft.getHeatType());
        costs[idx++] = sinkCost * smallCraft.getHeatSinks();
        costs[idx++] = CostCalculator.getWeaponsAndEquipmentCost(smallCraft, ignoreAmmo);
        return idx;
    }
}
