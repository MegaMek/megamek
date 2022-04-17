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
import megamek.common.Mounted;

public class InfantryCostCalculator {

    public static double calculateCost(Infantry infantry, CalculationReport costReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(costReport, infantry);
        double pweaponCost = 0;  // Primary Weapon Cost
        double sweaponCost = 0; // Secondary Weapon Cost
        double armorcost = 0; // Armor Cost
        double cost; // Total Final Cost of Platoon or Squad.
        double primarySquad; // Number of Troopers with Primary Weapon Only
        double secondSquad; // Number oif Troopers with Secondary Weapon Only.

        // Weapon Cost Calculation
        if (null != infantry.getPrimaryWeapon()) {
            pweaponCost += Math.sqrt(infantry.getPrimaryWeapon().getCost(infantry, false, -1)) * 2000;
        }
        if (null != infantry.getSecondaryWeapon()) {
            sweaponCost += Math.sqrt(infantry.getSecondaryWeapon().getCost(infantry, false, -1)) * 2000;
        }

        // Determining Break down of who would have primary and secondary weapons.
        primarySquad = (infantry.getSquadSize() - infantry.getSecondaryN()) * infantry.getSquadN();
        secondSquad = infantry.getOInternal(0) - primarySquad; // OInternal = menStarting

        // Squad Cost with just the weapons.
        cost = (primarySquad * pweaponCost) + (secondSquad * sweaponCost);

        // Check whether the unit has an armor kit. If not, calculate value for custom armor settings
        EquipmentType armor = infantry.getArmorKit();
        if (armor != null) {
            armorcost = armor.getCost(infantry, false, Infantry.LOC_INFANTRY);
        } else {
            // add in infantry armor cost
            if (infantry.getArmorDamageDivisor() > 1) {
                if (infantry.isArmorEncumbering()) {
                    armorcost += 1600;
                } else {
                    armorcost += 4300;
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
                armorcost += 50000;
            } else if (nSneak == 1) {
                armorcost += 7000;
            } else if (nSneak == 2) {
                armorcost += 21000;
            } else if (nSneak == 3) {
                armorcost += 28000;
            }

            if (infantry.hasSpaceSuit()) {
                armorcost += 5000;
            }
        }

        // Cost of armor on a per man basis added
        cost += (armorcost * infantry.getOInternal(0)); // OInternal = menStarting

        // Price multiplier includes anti-mech training, motive type, and specializations
        cost = cost * infantry.getPriceMultiplier();

        // add in field gun costs
        for (Mounted mounted : infantry.getEquipment()) {
            if (mounted.getLocation() == Infantry.LOC_FIELD_GUNS) {
                cost += Math.floor(mounted.getType().getCost(infantry, false, mounted.getLocation()));
            }
        }
        return cost;
    }
}
