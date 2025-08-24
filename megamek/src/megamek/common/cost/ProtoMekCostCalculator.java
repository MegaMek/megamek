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
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.ProtoMek;

public class ProtoMekCostCalculator {

    public static double calculateCost(ProtoMek protoMek, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[15];
        int idx = 0;

        // Add the cockpit, a constant cost.
        if (protoMek.getWeight() >= 10) {
            costs[idx++] = 800000;
        } else {
            costs[idx++] = 500000;
        }

        // Add life support, a constant cost.
        costs[idx++] = 75000;

        // Sensor cost is based on tonnage.
        costs[idx++] = 2000 * protoMek.getWeight();

        // Musculature cost is based on tonnage.
        costs[idx++] = 2000 * protoMek.getWeight();

        // Internal Structure cost is based on tonnage.
        if (protoMek.isGlider()) {
            costs[idx++] = 600 * protoMek.getWeight();
        } else if (protoMek.isQuad()) {
            costs[idx++] = 500 * protoMek.getWeight();
        } else {
            costs[idx++] = 400 * protoMek.getWeight();
        }

        // Arm actuators are based on tonnage.
        costs[idx++] = 2 * 180 * protoMek.getWeight();

        // Leg actuators are based on tonnage.
        costs[idx++] = 540 * protoMek.getWeight();

        // Engine cost is based on tonnage and rating.
        if (protoMek.hasEngine()) {
            costs[idx++] = (5000 * protoMek.getWeight() * protoMek.getEngine().getRating()) / 75;
        }

        // Jump jet cost is based on tonnage and jump MP.
        costs[idx++] = protoMek.getWeight() * protoMek.getJumpMP() * protoMek.getJumpMP() * 200;

        // Heat sinks is constant per sink. Per the construction rules, we need enough
        // sinks to sink all energy
        // weapon heat, so we just calculate the cost that way.
        int sinks = 0;
        for (Mounted<?> mount : protoMek.getWeaponList()) {
            if (mount.getType().hasFlag(WeaponType.F_ENERGY)) {
                WeaponType weaponType = (WeaponType) mount.getType();
                sinks += weaponType.getHeat();
            }
        }
        costs[idx++] = 2000 * sinks;

        // Armor is linear on the armor value of the ProtoMek
        costs[idx++] = protoMek.getTotalArmor() * ArmorType.forEntity(protoMek).getCost();

        // Weapons and equipment
        costs[idx++] = CostCalculator.getWeaponsAndEquipmentCost(protoMek, ignoreAmmo);

        // For all additive costs - replace negatives with 0 to separate from multipliers
        CostCalculator.removeNegativeAdditiveCosts(costs);

        costs[idx] = -protoMek.getPriceMultiplier();
        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Cockpit", "Life Support", "Sensors", "Musculature", "Structure", "Arm Actuators",
                                 "Leg Actuators", "Engine", "Jump Jets", "Heatsinks", "Armor", "Equipment",
                                 "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, protoMek, ignoreAmmo, systemNames, 11, cost, costs);

        return cost;
    }
}
