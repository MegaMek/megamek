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

public class BattleArmorCostCalculator {

    public static double calculateCost(BattleArmor battleArmor, CalculationReport costReport, boolean ignoreAmmo,
                                       boolean includeTrainingAndClan) {
        double[] costs = new double[15];
        int idx = 0;
        
        switch (battleArmor.getWeightClass()) {
            case EntityWeightClass.WEIGHT_MEDIUM:
                costs[idx++] = 100000;
                if (battleArmor.getMovementMode() == EntityMovementMode.VTOL) {
                    costs[idx++] = battleArmor.getOriginalJumpMP() * 100000;
                } else {
                    costs[idx++] = battleArmor.getOriginalJumpMP() * 75000;
                }
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                costs[idx++] = 200000;
                if (battleArmor.getMovementMode() == EntityMovementMode.INF_UMU) {
                    costs[idx++] = battleArmor.getOriginalJumpMP() * 100000;
                } else {
                    costs[idx++] = battleArmor.getOriginalJumpMP() * 150000;
                }
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
                costs[idx++] = 400000;
                if (battleArmor.getMovementMode() == EntityMovementMode.INF_UMU) {
                    costs[idx++] = battleArmor.getOriginalJumpMP() * 150000;
                } else {
                    costs[idx++] = battleArmor.getOriginalJumpMP() * 300000;
                }
                break;
            default:
                costs[idx++] = 50000;
                costs[idx++] = 50000 * battleArmor.getOriginalJumpMP();
                break;
        }
        costs[idx++] = 25000 * (battleArmor.getOriginalWalkMP() - 1);

        long manipulatorCost = 0;
        for (Mounted mounted : battleArmor.getEquipment()) {
            if ((mounted.getType() instanceof MiscType)
                    && mounted.getType().hasFlag(MiscType.F_BA_MANIPULATOR)) {
                long itemCost = (long) mounted.getCost();
                manipulatorCost += itemCost;
            }

        }
        costs[idx++] = manipulatorCost;

        double baseArmorCost;
        switch (battleArmor.getArmorType(BattleArmor.LOC_TROOPER_1)) {
            case EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED:
                baseArmorCost = 12500;
                break;
            case EquipmentType.T_ARMOR_BA_MIMETIC:
            case EquipmentType.T_ARMOR_BA_STEALTH:
                baseArmorCost = 15000;
                break;
            case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
                baseArmorCost = 12000;
                break;
            case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
                baseArmorCost = 20000;
                break;
            case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
                baseArmorCost = 50000;
                break;
            case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
            case EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE:
            case EquipmentType.T_ARMOR_BA_STANDARD:
            default:
                baseArmorCost = 10000;
                break;
        }

        costs[idx++] = (baseArmorCost * battleArmor.getOArmor(BattleArmor.LOC_TROOPER_1));

        // training cost and clan mod
        if (includeTrainingAndClan) {
            if (battleArmor.isClan()) {
                costs[idx++] = -1.1;
                costs[idx++] = 200000;
            } else {
                costs[idx++] = 0;
                costs[idx++] = 150000;
            }
        }

        // TODO : we do not track the modular weapons mount for 1000 C-bills in the unit files
        costs[idx++] = CostCalculator.getWeaponsAndEquipmentCost(battleArmor, ignoreAmmo);
        costs[idx++] = -battleArmor.getSquadSize();

        double cost = 0; // calculate the total
        for (int x = 0; x < idx; x++) {
            if (costs[x] < 0) {
                cost *= -costs[x];
            } else {
                cost += costs[x];
            }
        }

        String[] systemNames = { "Chassis", "Jumping/VTOL/UMU", "Ground Movement", "Manipulators", "Armor",
                "Clan Structure Multiplier", "Training", "Equipment", "Troopers" };
        CostCalculator.fillInReport(costReport, battleArmor, ignoreAmmo, systemNames, 7, cost, costs);

        return cost;
    }
}
