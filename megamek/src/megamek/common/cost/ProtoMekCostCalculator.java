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

public class ProtoMekCostCalculator {

    public static double calculateCost(Protomech protoMek, CalculationReport costReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(costReport, protoMek);
        double cost = 0;

        // Add the cockpit, a constant cost.
        if (protoMek.getWeight() >= 10) {
            cost += 800000;
        } else {
            cost += 500000;
        }

        // Add life support, a constant cost.
        cost += 75000;

        // Sensor cost is based on tonnage.
        cost += 2000 * protoMek.getWeight();

        // Musculature cost is based on tonnage.
        cost += 2000 * protoMek.getWeight();

        // Internal Structure cost is based on tonnage.
        if (protoMek.isGlider()) {
            cost += 600 * protoMek.getWeight();
        } else if (protoMek.isQuad()) {
            cost += 500 * protoMek.getWeight();
        } else {
            cost += 400 * protoMek.getWeight();
        }

        // Arm actuators are based on tonnage.
        // Their cost is listed separately?
        cost += 2 * 180 * protoMek.getWeight();

        // Leg actuators are based on tonnage.
        cost += 540 * protoMek.getWeight();

        // Engine cost is based on tonnage and rating.
        if (protoMek.hasEngine()) {
            cost += (5000 * protoMek.getWeight() * protoMek.getEngine().getRating()) / 75;
        }

        // Jump jet cost is based on tonnage and jump MP.
        cost += protoMek.getWeight() * protoMek.getJumpMP() * protoMek.getJumpMP() * 200;

        // Heat sinks is constant per sink.
        // per the construction rules, we need enough sinks to sink all energy
        // weapon heat, so we just calculate the cost that way.
        int sinks = 0;
        for (Mounted mount : protoMek.getWeaponList()) {
            if (mount.getType().hasFlag(WeaponType.F_ENERGY)) {
                WeaponType wtype = (WeaponType) mount.getType();
                sinks += wtype.getHeat();
            }
        }
        cost += 2000 * sinks;

        // Armor is linear on the armor value of the Protomech
        cost += protoMek.getTotalArmor() *
                EquipmentType.getProtomechArmorCostPerPoint(protoMek.getArmorType(protoMek.firstArmorIndex()));

        // Add in equipment cost.
        cost += CostCalculator.getWeaponsAndEquipmentCost(protoMek, ignoreAmmo);

        // Finally, apply the Final ProtoMech Cost Multiplier
        cost *= protoMek.getPriceMultiplier();

        return cost;
    }
}
