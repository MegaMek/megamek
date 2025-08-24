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
import megamek.common.units.ConvFighter;
import megamek.common.equipment.ArmorType;
import megamek.common.verifier.TestEntity;

public class ConvFighterCostCalculator {

    public static double calculateCost(ConvFighter fighter, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[15];
        int idx = 0;

        // Avionics
        double avionicsWeight = Math.ceil(fighter.getWeight() / 5) / 2;
        costs[idx++] = 4000 * avionicsWeight;

        // VSTOL
        if (fighter.isVSTOL()) {
            double vstolWeight = Math.ceil(fighter.getWeight() / 10) / 2;
            costs[idx++] = 5000 * vstolWeight;
        } else {
            costs[idx++] = 0;
        }

        // Structure and Additional flight systems
        costs[idx++] = 4000 * fighter.getSI();
        costs[idx++] = 25000 + 10 * fighter.getWeight();

        // Engine
        if (fighter.hasEngine()) {
            costs[idx++] = (fighter.getEngine().getBaseCost() * fighter.getEngine().getRating() * fighter.getWeight()) /
                  75.0;
        }

        // Fuel tanks
        costs[idx++] = (200 * fighter.getFuel()) / 160.0;

        // Armor
        if (fighter.hasPatchworkArmor()) {
            double armorCost = 0;
            for (int loc = 0; loc < fighter.locations(); loc++) {
                armorCost += fighter.getArmorWeight(loc) * ArmorType.forEntity(fighter, loc).getCost();
            }
            costs[idx++] = armorCost;
        } else {
            costs[idx++] = fighter.getArmorWeight() * ArmorType.forEntity(fighter).getCost();
        }

        // Heat sinks
        int sinkCost = 2000 + (4000 * fighter.getHeatType());
        costs[idx++] = sinkCost * TestEntity.calcHeatNeutralHSRequirement(fighter);

        costs[idx++] = CostCalculator.getWeaponsAndEquipmentCost(fighter, ignoreAmmo);

        // Power amplifiers
        costs[idx++] = 20000 * fighter.getPowerAmplifierWeight();

        // For all additive costs - replace negatives with 0 to separate from multipliers
        CostCalculator.removeNegativeAdditiveCosts(costs);

        costs[idx] = -fighter.getPriceMultiplier();
        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Avionics", "VSTOL Gear", "Structure", "Flight Systems", "Engine", "Fuel Tanks",
                                 "Armor", "Heat Sinks", "Equipment", "Power Amplifiers", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, fighter, ignoreAmmo, systemNames, 8, cost, costs);

        return Math.round(cost);
    }
}
