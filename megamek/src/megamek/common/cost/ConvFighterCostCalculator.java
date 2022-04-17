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
import megamek.common.ConvFighter;
import megamek.common.EquipmentType;
import megamek.common.verifier.TestEntity;

public class ConvFighterCostCalculator {

    public static double calculateCost(ConvFighter fighter, CalculationReport costReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(costReport, fighter);
        double cost = 0;

        // add in cockpit
        double avionicsWeight = Math.ceil(fighter.getWeight() / 5) / 2;
        cost += 4000 * avionicsWeight;

        // add VSTOL gear if applicable
        if (fighter.isVSTOL()) {
            double vstolWeight = Math.ceil(fighter.getWeight() / 10) / 2;
            cost += 5000 * vstolWeight;
        }

        // Structural integrity
        cost += 4000 * fighter.getSI();

        // additional flight systems (attitude thruster and landing gear)
        cost += 25000 + (10 * fighter.getWeight());

        // engine
        if (fighter.hasEngine()) {
            cost += (fighter.getEngine().getBaseCost() * fighter.getEngine().getRating() * fighter.getWeight()) / 75.0;
        }

        // fuel tanks
        cost += (200 * fighter.getFuel()) / 160.0;

        // armor
        if (fighter.hasPatchworkArmor()) {
            for (int loc = 0; loc < fighter.locations(); loc++) {
                cost += fighter.getArmorWeight(loc) * EquipmentType.getArmorCost(fighter.getArmorType(loc));
            }
        } else {
            cost += fighter.getArmorWeight() * EquipmentType.getArmorCost(fighter.getArmorType(0));
        }

        // heat sinks
        int sinkCost = 2000 + (4000 * fighter.getHeatType());
        cost += sinkCost * TestEntity.calcHeatNeutralHSRequirement(fighter);

        // weapons
        cost += CostCalculator.getWeaponsAndEquipmentCost(fighter, ignoreAmmo);

        // power amplifiers, if any
        cost += 20000 * fighter.getPowerAmplifierWeight();

        return Math.round(cost * fighter.getPriceMultiplier());
    }
}
