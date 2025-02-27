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
import megamek.common.equipment.ArmorType;
import megamek.common.options.OptionsConstants;

public class MekCostCalculator {

    public static double calculateCost(Mek mek, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[17 + mek.locations()];
        int i = 0;

        double cockpitCost = switch (mek.getCockpitType()) {
            case Mek.COCKPIT_TORSO_MOUNTED -> 750000;
            case Mek.COCKPIT_DUAL ->
                // Solaris VII - The Game World (German) This is not actually canonical as it
                // has never been repeated in any English language source including Tech Manual
                40000;
            case Mek.COCKPIT_COMMAND_CONSOLE ->
                // Command Consoles are listed as a cost of 500,000.
                // That appears to be in addition to the primary cockpit.
                700000;
            case Mek.COCKPIT_SMALL -> 175000;
            case Mek.COCKPIT_VRRP -> 1250000;
            case Mek.COCKPIT_INDUSTRIAL, Mek.COCKPIT_PRIMITIVE_INDUSTRIAL -> 100000;
            case Mek.COCKPIT_TRIPOD, Mek.COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL -> 400000;
            case Mek.COCKPIT_TRIPOD_INDUSTRIAL, Mek.COCKPIT_SUPERHEAVY -> 300000;
            case Mek.COCKPIT_QUADVEE -> 375000;
            case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE ->
                // The cost is the sum of both superheavy cockpit and command console
                800000;
            case Mek.COCKPIT_SUPERHEAVY_TRIPOD -> 500000;
            case Mek.COCKPIT_SMALL_COMMAND_CONSOLE ->
                // The cost is the sum of both small and command console
                675000;
            default -> 200000;
        };
        if (mek.hasEiCockpit()
                && ((null != mek.getCrew()) && mek.hasAbility(OptionsConstants.UNOFF_EI_IMPLANT))) {
            cockpitCost = 400000;
        }
        costs[i++] = cockpitCost;
        costs[i++] = 50000;// life support
        costs[i++] = mek.getWeight() * 2000;// sensors
        int muscCost = mek.hasSCM() ? 10000 : mek.hasTSM(false) ? 16000 :
            mek.hasTSM(true) ? 32000 : mek.hasIndustrialTSM() ? 12000 : mek.isSuperHeavy() ? 12000 : 2000;
        costs[i++] = muscCost * mek.getWeight();// musculature
        double structureCost = getStructureCost(mek) * mek.getWeight() * (mek.isTripodMek() ? 1.2 : 1);// IS
        costs[i++] = structureCost;
        costs[i++] = mek.getActuatorCost() * (mek.isSuperHeavy() ? 2 : 1);// arm and/or leg actuators
        if (mek.hasEngine()) {
            costs[i++] = (mek.getEngine().getBaseCost() * mek.getEngine().getRating() * mek.getWeight()) / 75.0;
        }
        if (mek.getGyroType() == Mek.GYRO_XL) {
            costs[i++] = 750000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 0.5;
        } else if (mek.getGyroType() == Mek.GYRO_COMPACT) {
            costs[i++] = 400000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 1.5;
        } else if ((mek.getGyroType() == Mek.GYRO_HEAVY_DUTY) || (mek.getGyroType() == Mek.GYRO_SUPERHEAVY)) {
            costs[i++] = 500000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 2;
        } else if (mek.getGyroType() == Mek.GYRO_STANDARD) {
            costs[i++] = 300000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f);
        }
        double jumpBaseCost = 200;
        // You cannot have JJ's and UMU's on the same unit.
        if (mek.hasUMU()) {
            costs[i++] = Math.pow(mek.getAllUMUCount(), 2.0) * mek.getWeight() * jumpBaseCost;
            // We could have Jump boosters
            if (mek.getJumpType() == Mek.JUMP_BOOSTER) {
                jumpBaseCost = 150;
                costs[i++] = Math.pow(mek.getOriginalJumpMP(), 2.0) * mek.getWeight() * jumpBaseCost;
            }
        } else {
            if (mek.getJumpType() == Mek.JUMP_BOOSTER) {
                jumpBaseCost = 150;
            } else if (mek.getJumpType() == Mek.JUMP_IMPROVED) {
                jumpBaseCost = 500;
            }
            costs[i++] = Math.pow(mek.getOriginalJumpMP(), 2.0) * mek.getWeight() * jumpBaseCost;
        }
        // num of sinks we don't pay for
        int freeSinks = mek.hasDoubleHeatSinks() ? 0 : 10;
        int sinkCost = mek.hasDoubleHeatSinks() ? 6000 : 2000;
        // cost of sinks
        costs[i++] = sinkCost * (mek.heatSinks() - freeSinks);
        costs[i++] = mek.hasFullHeadEject() ? 1725000 : 0;
        // armored components
        int armoredCrits = 0;
        for (int j = 0; j < mek.locations(); j++) {
            int numCrits = mek.getNumberOfCriticals(j);
            for (int k = 0; k < numCrits; k++) {
                CriticalSlot ccs = mek.getCritical(j, k);
                if ((ccs != null) && ccs.isArmored() && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    armoredCrits++;
                }
            }
        }
        costs[i++] = armoredCrits * 150000;

        // armor
        if (mek.hasPatchworkArmor()) {
            for (int loc = 0; loc < mek.locations(); loc++) {
                costs[i++] += mek.getArmorWeight(loc) * ArmorType.forEntity(mek, loc).getCost();
            }
        } else {
            costs[i++] += mek.getArmorWeight() * ArmorType.forEntity(mek).getCost();
        }

        double weaponCost = CostCalculator.getWeaponsAndEquipmentCost(mek, ignoreAmmo);
        costs[i++] = weaponCost;

        if (mek instanceof LandAirMek) {
            costs[i++] = (structureCost + weaponCost)
                    * (((LandAirMek) mek).getLAMType() == LandAirMek.LAM_BIMODAL ? 0.65 : 0.75);
        } else if (mek instanceof QuadVee) {
            costs[i++] = (structureCost + weaponCost) * 0.5;
        } else {
            costs[i++] = 0;
        }

        double cost = 0; // calculate the total
        for (int x = 0; x < i; x++) {
            cost += costs[x];
        }
        // TODO Decouple cost calculation from addCostDetails and eliminate duplicate code in getPriceMultiplier
        double quirkMultiplier = 0;
        if (mek.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_1)) {
            quirkMultiplier = 1.1f;
            cost *= quirkMultiplier;
        } else if (mek.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_2)) {
            quirkMultiplier = 1.25f;
            cost *= quirkMultiplier;
        }
        costs[i++] = -quirkMultiplier; // negative just marks it as multiplier

        double omniMultiplier = 0;
        if (mek.isOmni()) {
            omniMultiplier = 1.25f;
            cost *= omniMultiplier;
        }
        costs[i++] = -omniMultiplier; // negative just marks it as multiplier

        double weightMultiplier = 1 + (mek.getWeight() / 100f);
        if (mek.isIndustrial()) {
            weightMultiplier = 1 + (mek.getWeight() / 400f);
        }
        costs[i] = -weightMultiplier; // negative just marks it as multiplier
        cost = Math.round(cost * weightMultiplier);
        String[] systemNames = { "Cockpit", "Life Support", "Sensors", "Myomer", "Structure", "Actuators",
                "Engine", "Gyro", "Jump Jets", "Heatsinks", "Full Head Ejection System",
                "Armored System Components", "Armor", "Equipment",
                "Conversion Equipment", "Quirk Multiplier", "Omni Multiplier", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, mek, ignoreAmmo, systemNames, 13, cost, costs);
        return cost;
    }

    private static int getStructureCost(Mek mek) {
        if (mek.isSuperHeavy()) {
            return switch (mek.getStructureType()) {
                case EquipmentType.T_STRUCTURE_STANDARD -> 4000;
                case EquipmentType.T_STRUCTURE_INDUSTRIAL -> 3000;
                case EquipmentType.T_STRUCTURE_ENDO_STEEL -> 16000;
                case EquipmentType.T_STRUCTURE_COMPOSITE -> 1600;
                case EquipmentType.T_STRUCTURE_ENDO_COMPOSITE -> 6400;
                default -> 0;
            };
        } else {
            return switch (mek.getStructureType()) {
                case EquipmentType.T_STRUCTURE_STANDARD -> 400;
                case EquipmentType.T_STRUCTURE_INDUSTRIAL -> 300;
                case EquipmentType.T_STRUCTURE_ENDO_STEEL, EquipmentType.T_STRUCTURE_COMPOSITE -> 1600;
                case EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE -> 4800;
                case EquipmentType.T_STRUCTURE_REINFORCED -> 6400;
                case EquipmentType.T_STRUCTURE_ENDO_COMPOSITE -> 3200;
                default -> 0;
            };
        }
    }

}
