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
package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.weapons.InfantryAttack;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASBattleArmorDamageConverter extends ASDamageConverter {

    private static final double AP_MOUNT_DAMAGE = 0.05;
    private static final double ARMORED_GLOVE_DAMAGE = 0.1;

    private final double troopFactor = TROOP_FACTOR[Math.min(((BattleArmor) entity).getShootingStrength(), 30)] + 0.5;
    private int bombRacks = 0;

    protected ASBattleArmorDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processDamage() {
        processSDamage();
        processMDamage();
        processLDamage();
        processHT();
        processFrontSpecialDamage(IF);
        processFrontSpecialDamage(FLK);
    }

    /**
     * Specialized method to sum up the squad support weapon damage for the given range.
     * This should always stay similar to the assembleFrontDamage() methods.
     *
     * @param range The range, e.g. AlphaStrikeElement.MEDIUM_RANGE
     * @return The raw damage sum
     */
    protected double assembleSquadSupportDamage(int range) {
        double rawDamage = 0;
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            if (!weapon.isSquadSupportWeapon()) {
                continue;
            }
            double baseDamage = determineDamage(weapon, range);
            if (baseDamage > 0) {
                double damageMultiplier = getDamageMultiplier(weapon, weaponType);
                double modifiedDamage = baseDamage * damageMultiplier;
                String calculation = "+ " + formatForReport(modifiedDamage);
                calculation += (damageMultiplier != 1) ? " (" + formatForReport(baseDamage) + " x " +
                        formatForReport(damageMultiplier) + ")" : "";
                rawDamage += modifiedDamage;
                report.addLine(getWeaponDesc(weapon), calculation, "= " + formatForReport(rawDamage));
            }
        }
        return rawDamage;
    }

    @Override
    protected double determineDamage(Mounted weapon, int range) {
        return weapon.isAPMMounted() ? 0 : super.determineDamage(weapon, range);
    }

    @Override
    protected double[] assembleSpecialDamage(BattleForceSUA dmgType, int location) {
        double[] damage = super.assembleSpecialDamage(dmgType, location);
        String rawValues = formatAsVector(damage[0], damage[1], damage[2], 0, dmgType);
        damage[0] *= troopFactor;
        damage[1] *= troopFactor;
        damage[2] *= troopFactor;
        String multipliedValues = formatAsVector(damage[0], damage[1], damage[2], 0, dmgType);
        report.addLine("Troop Factor", rawValues + " x " + formatForReport(troopFactor),
                "= " + multipliedValues);
        return damage;
    }

    @Override
    protected int[] assembleHeatDamage() {
        int[] heatDmg = super.assembleHeatDamage();
        String rawValues = formatAsVector(heatDmg[0], heatDmg[1], heatDmg[2], 0, HT);
        heatDmg[0] *= troopFactor;
        heatDmg[1] *= troopFactor;
        heatDmg[2] *= troopFactor;
        String multipliedValues = formatAsVector(heatDmg[0], heatDmg[1], heatDmg[2], 0, HT);
        report.addLine("Troop Factor", rawValues + " x " + formatForReport(troopFactor),
                "= " + multipliedValues);
        return heatDmg;
    }

    @Override
    protected void processSDamage() {
        report.addLine("--- Short Range Damage:", "");
        double sDamage = assembleFrontDamage(SHORT_RANGE);

        if (entity.hasMisc(MiscType.F_ARMORED_GLOVE)) {
            sDamage += ARMORED_GLOVE_DAMAGE;
            report.addLine("Armored Glove(s)",
                    "+ " + formatForReport(ARMORED_GLOVE_DAMAGE), "= " + formatForReport(sDamage));
        } else if (entity.hasMisc(MiscType.F_AP_MOUNT)) {
            sDamage += AP_MOUNT_DAMAGE;
            report.addLine("APM",
                    "+ " + formatForReport(AP_MOUNT_DAMAGE), "= " + formatForReport(sDamage));
        }

        if (sDamage > 0) {
            report.addLine("Troop Factor", formatForReport(sDamage) + " x " + formatForReport(troopFactor),
                    "= " + formatForReport(sDamage * troopFactor));
            sDamage *= troopFactor;
        }

        sDamage += assembleSquadSupportDamage(SHORT_RANGE);

        int vibroclaws = entity.countWorkingMisc(MiscType.F_VIBROCLAW);
        sDamage += 0.1 * vibroclaws;
        if (vibroclaws > 0) {
            report.addLine("Vibroclaws", "+ 0." + vibroclaws, "= " + formatForReport(sDamage));
        }

        if (sDamage > 0) {
            finalSDamage = ASDamage.createDualRoundedUp(sDamage);
            report.addLine("Final S damage:",
                    formatForReport(sDamage) + ", dual rounded", "= " + finalSDamage.toStringWithZero());
        } else {
            report.addLine("None.", "");
        }
    }

    @Override
    protected void processMDamage() {
        report.addEmptyLine();
        report.addLine("--- Medium Range Damage:", "");
        double mDamage = assembleFrontDamage(MEDIUM_RANGE);
        if (mDamage > 0) {
            report.addLine("Troop Factor", formatForReport(mDamage) + " x " + formatForReport(troopFactor),
                    "= " + formatForReport(mDamage * troopFactor));
            mDamage *= troopFactor;
            mDamage += assembleSquadSupportDamage(MEDIUM_RANGE);
            finalMDamage = ASDamage.createDualRoundedUp(mDamage);
            report.addLine("Final M damage:",
                    formatForReport(mDamage) + ", dual rounded", "= " + finalMDamage.toStringWithZero());
        } else {
            report.addLine("None.", "");
        }
    }

    @Override
    protected void processLDamage() {
        report.addEmptyLine();
        report.addLine("--- Long Range Damage:", "");
        double lDamage = assembleFrontDamage(LONG_RANGE);
        if (lDamage > 0) {
            report.addLine("Troop Factor", formatForReport(lDamage) + " x " + formatForReport(troopFactor),
                    "= " + formatForReport(lDamage * troopFactor));
            lDamage *= troopFactor;
            lDamage += assembleSquadSupportDamage(LONG_RANGE);
            finalLDamage = ASDamage.createDualRoundedUp(lDamage);
            report.addLine("Final L damage:",
                    formatForReport(lDamage) + ", dual rounded", "= " + finalLDamage.toStringWithZero());
        } else {
            report.addLine("None.", "");
        }
    }

    @Override
    protected void assignSpecialAbilities(Mounted weapon, WeaponType weaponType) {
        super.assignSpecialAbilities(weapon, weaponType);

        if (weaponType.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
            bombRacks++;
        }

        if (weaponType instanceof InfantryAttack) {
            assignToLocations(weapon, AM);
        }
    }

    @Override
    protected void processTaser(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAmmoType() == AmmoType.T_TASER) {
            assignToLocations(weapon, BTAS, 1);
        }
    }

    @Override
    protected void processNarc(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAmmoType() == AmmoType.T_NARC) {
            assignToLocations(weapon, CNARC, 1);
        }
    }

    @Override
    protected void processSpecialAbilities() {
        super.processSpecialAbilities();
        if (bombRacks > 0) {
            int bombValue = bombRacks * ((BattleArmor) entity).getShootingStrength() / 5;
            element.getSpecialAbilities().mergeSUA(BOMB, bombValue);
            report.addLine("BA bomb racks", "", "BOMB" + bombValue);
        }
    }
}