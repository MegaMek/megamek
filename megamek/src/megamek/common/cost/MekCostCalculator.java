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

    public static double calculateCost(Mech mek, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[17 + mek.locations()];
        int i = 0;

        double cockpitCost;
        switch (mek.getCockpitType()) {
            case Mech.COCKPIT_TORSO_MOUNTED:
                cockpitCost = 750000;
                break;
            case Mech.COCKPIT_DUAL:
                // Solaris VII - The Game World (German) This is not actually canonical as it
                // has never been repeated in any English language source including Tech Manual
                cockpitCost = 40000;
                break;
            case Mech.COCKPIT_COMMAND_CONSOLE:
                // Command Consoles are listed as a cost of 500,000.
                // That appears to be in addition to the primary cockpit.
                cockpitCost = 700000;
                break;
            case Mech.COCKPIT_SMALL:
                cockpitCost = 175000;
                break;
            case Mech.COCKPIT_VRRP:
                cockpitCost = 1250000;
                break;
            case Mech.COCKPIT_INDUSTRIAL:
            case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
                cockpitCost = 100000;
                break;
            case Mech.COCKPIT_TRIPOD:
            case Mech.COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL:
                cockpitCost = 400000;
                break;
            case Mech.COCKPIT_TRIPOD_INDUSTRIAL:
            case Mech.COCKPIT_SUPERHEAVY:
                cockpitCost = 300000;
                break;
            case Mech.COCKPIT_QUADVEE:
                cockpitCost = 375000;
                break;
            case Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                // The cost is the sum of both superheavy cockpit and command console
                cockpitCost = 800000;
                break;
            case Mech.COCKPIT_SUPERHEAVY_TRIPOD:
                cockpitCost = 500000;
                break;
            case Mech.COCKPIT_SMALL_COMMAND_CONSOLE:
                // The cost is the sum of both small and command console
                cockpitCost = 675000;
                break;
            default:
                cockpitCost = 200000;
        }
        if (mek.hasEiCockpit()
                && ((null != mek.getCrew()) && mek.hasAbility(OptionsConstants.UNOFF_EI_IMPLANT))) {
            cockpitCost = 400000;
        }
        costs[i++] = cockpitCost;
        costs[i++] = 50000;// life support
        costs[i++] = mek.getWeight() * 2000;// sensors
        int muscCost = mek.hasSCM() ? 10000 : mek.hasTSM(false) ? 16000 :
                mek.hasTSM(true) ? 32000 : mek.hasIndustrialTSM() ? 12000 : 2000;
        costs[i++] = muscCost * mek.getWeight();// musculature
        double structureCost = EquipmentType.getStructureCost(mek.getStructureType()) * mek.getWeight();// IS
        costs[i++] = structureCost;
        costs[i++] = mek.getActuatorCost();// arm and/or leg actuators
        if (mek.hasEngine()) {
            costs[i++] = (mek.getEngine().getBaseCost() * mek.getEngine().getRating() * mek.getWeight()) / 75.0;
        }
        if (mek.getGyroType() == Mech.GYRO_XL) {
            costs[i++] = 750000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 0.5;
        } else if (mek.getGyroType() == Mech.GYRO_COMPACT) {
            costs[i++] = 400000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 1.5;
        } else if (mek.getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            costs[i++] = 500000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 2;
        } else if (mek.getGyroType() == Mech.GYRO_STANDARD) {
            costs[i++] = 300000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f);
        }
        double jumpBaseCost = 200;
        // You cannot have JJ's and UMU's on the same unit.
        if (mek.hasUMU()) {
            costs[i++] = Math.pow(mek.getAllUMUCount(), 2.0) * mek.getWeight() * jumpBaseCost;
            // We could have Jump boosters
            if (mek.getJumpType() == Mech.JUMP_BOOSTER) {
                jumpBaseCost = 150;
                costs[i++] = Math.pow(mek.getOriginalJumpMP(), 2.0) * mek.getWeight() * jumpBaseCost;
            }
        } else {
            if (mek.getJumpType() == Mech.JUMP_BOOSTER) {
                jumpBaseCost = 150;
            } else if (mek.getJumpType() == Mech.JUMP_IMPROVED) {
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

        if (mek instanceof LandAirMech) {
            costs[i++] = (structureCost + weaponCost)
                    * (((LandAirMech) mek).getLAMType() == LandAirMech.LAM_BIMODAL ? 0.65 : 0.75);
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

}
