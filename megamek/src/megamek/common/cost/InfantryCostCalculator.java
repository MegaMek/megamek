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
import megamek.common.EquipmentType;
import megamek.common.Infantry;

public class InfantryCostCalculator {

    public static double calculateCost(Infantry infantry, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[15];
        int idx = 0;

        // Weapon Cost Calculation
        double pweaponCost = 0;  // Primary Weapon Cost
        if (null != infantry.getPrimaryWeapon()) {
            pweaponCost = Math.sqrt(infantry.getPrimaryWeapon().getCost(infantry, false, -1)) * 2000;
        }
        double sweaponCost = 0; // Secondary Weapon Cost
        if (null != infantry.getSecondaryWeapon()) {
            sweaponCost = Math.sqrt(infantry.getSecondaryWeapon().getCost(infantry, false, -1)) * 2000;
        }

        // Determining Break down of who would have primary and secondary weapons.
        double primarySquad = (infantry.getSquadSize() - infantry.getSecondaryWeaponsPerSquad()) * infantry.getSquadCount();
        double secondSquad = infantry.getOInternal(0) - primarySquad; // OInternal = menStarting

        // Squad Cost with just the weapons.
        costs[idx++] = primarySquad * pweaponCost + secondSquad * sweaponCost;

        // Check whether the unit has an armor kit. If not, calculate value for custom armor settings
        double armorCost = 0;
        EquipmentType armor = infantry.getArmorKit();
        if (armor != null) {
            armorCost = armor.getCost(infantry, false, Infantry.LOC_INFANTRY);
        } else {
            // add in infantry armor cost
            if (infantry.getArmorDamageDivisor() > 1) {
                if (infantry.isArmorEncumbering()) {
                    armorCost += 1600;
                } else {
                    armorCost += 4300;
                }
            }
            int nSneak = 0;
            if (infantry.hasSneakCamo()) {
                nSneak++;
            }
            if (infantry.hasSneakECM()) {
                nSneak++;
            }
            if (infantry.hasSneakIR()) {
                nSneak++;
            }

            if (infantry.hasDEST()) {
                armorCost += 50000;
            } else if (nSneak == 1) {
                armorCost += 7000;
            } else if (nSneak == 2) {
                armorCost += 21000;
            } else if (nSneak == 3) {
                armorCost += 28000;
            }

            if (infantry.hasSpaceSuit()) {
                armorCost += 5000;
            }
        }

        // Cost of armor on a per man basis added
        costs[idx++] = armorCost * infantry.getOInternal(0); // OInternal = menStarting

        // Price multiplier includes anti-mech training, motive type, and specializations
        costs[idx++] = -infantry.getPriceMultiplier();

        // add in field gun costs
        costs[idx] = infantry.originalFieldWeapons().stream()
                .mapToDouble(m -> m.getType().getCost(infantry, false, m.getLocation())).sum();

        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Weapons", "Armor", "Multiplier", "Field Gun" };
        CostCalculator.fillInReport(costReport, infantry, ignoreAmmo, systemNames, -1, cost, costs);
        return cost;
    }
}
