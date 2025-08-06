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
import megamek.common.EquipmentType;
import megamek.common.Infantry;

public class InfantryCostCalculator {

    public static double calculateCost(Infantry infantry, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[15];
        int idx = 0;

        // Weapon Cost Calculation
        double pweaponCost = 0; // Primary Weapon Cost
        if (null != infantry.getPrimaryWeapon()) {
            pweaponCost = Math.sqrt(infantry.getPrimaryWeapon().getCost(infantry, false, -1)) * 2000;
        }
        double sweaponCost = 0; // Secondary Weapon Cost
        if (null != infantry.getSecondaryWeapon()) {
            sweaponCost = Math.sqrt(infantry.getSecondaryWeapon().getCost(infantry, false, -1)) * 2000;
        }

        // Determining Break down of who would have primary and secondary weapons.
        double primarySquad = (infantry.getSquadSize() - infantry.getSecondaryWeaponsPerSquad())
              * infantry.getSquadCount();
        double secondSquad = infantry.getOInternal(0) - primarySquad; // OInternal = menStarting

        // Squad Cost with just the weapons.
        costs[idx++] = primarySquad * pweaponCost + secondSquad * sweaponCost;

        // Check whether the unit has an armor kit. If not, calculate value for custom
        // armor settings
        double armorCost = 0;
        EquipmentType armor = infantry.getArmorKit();
        if (armor != null) {
            armorCost = armor.getCost(infantry, false, Infantry.LOC_INFANTRY);
        } else {
            // add in infantry armor cost
            if (infantry.getCustomArmorDamageDivisor() > 1) {
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

        // For all additive costs - replace negatives with 0 to separate from multipliers
        CostCalculator.removeNegativeAdditiveCosts(costs);

        // Price multiplier includes anti-mek training, motive type, and specializations
        costs[idx++] = -infantry.getPriceMultiplier();

        // add in field gun costs
        costs[idx++] = infantry.originalFieldWeapons().stream()
              .mapToDouble(m -> m.getType().getCost(infantry, false, m.getLocation())).sum();

        costs[idx] = infantry.getMount() == null ? 0 : 5000 * infantry.getWeight();

        double cost = CostCalculator.calculateCost(costs);
        String[] systemNames = { "Weapons", "Armor", "Multiplier", "Field Gun", "Mount" };
        CostCalculator.fillInReport(costReport, infantry, ignoreAmmo, systemNames, -1, cost, costs);
        return cost;
    }
}
