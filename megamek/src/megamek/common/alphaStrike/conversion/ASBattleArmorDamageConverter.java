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
import megamek.common.weapons.InfantryAttack;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASBattleArmorDamageConverter extends ASDamageConverter2 {

    private static final double AP_MOUNT_DAMAGE = 0.05;

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
    }

    @Override
    protected double determineDamage(Mounted weapon, int range) {
        if (weapon.isAPMMounted()) {
            double apDamage = AP_MOUNT_DAMAGE;
            //TODO: What does the "plus 1" mean in "Battle armor units apply the damage value for Anti-Personnel
            // weapon once per AP weapon mount on the suit itself, plus 1 if the suit has
            // at least 1 armored glove manipulator."
            apDamage += (entity.hasWorkingMisc(MiscType.F_ARMORED_GLOVE)) ? AP_MOUNT_DAMAGE : 0;
            //TODO: To what range are AP mounted weapons effective?
            return range == 0 ? apDamage : 0;
        } else {
            return super.determineDamage(weapon, range);
        }
    }

    protected void processSDamage() {
        report.addLine("--- Short Range Damage:", "");
        double sDamage = assembleFrontDamage(SHORT_RANGE);

        if (sDamage > 0) {
            report.addLine("Troop Factor", formatForReport(sDamage) + " x " + formatForReport(troopFactor),
                    "= " + formatForReport(sDamage * troopFactor));
            sDamage *= troopFactor;
        }

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

    protected void processMDamage() {
        report.addEmptyLine();
        report.addLine("--- Medium Range Damage:", "");
        double mDamage = assembleFrontDamage(MEDIUM_RANGE);
        if (mDamage > 0) {
            report.addLine("Troop Factor", formatForReport(mDamage) + " x " + formatForReport(troopFactor),
                    "= " + formatForReport(mDamage * troopFactor));
            mDamage *= troopFactor;
            finalMDamage = ASDamage.createDualRoundedUp(mDamage);
            report.addLine("Final M damage:",
                    formatForReport(mDamage) + ", dual rounded", "= " + finalMDamage.toStringWithZero());
        } else {
            report.addLine("None.", "");
        }
    }

    protected void processLDamage() {
        report.addEmptyLine();
        report.addLine("--- Long Range Damage:", "");
        double lDamage = assembleFrontDamage(LONG_RANGE);
        if (lDamage > 0) {
            report.addLine("Troop Factor", formatForReport(lDamage) + " x " + formatForReport(troopFactor),
                    "= " + formatForReport(lDamage * troopFactor));
            lDamage *= troopFactor;
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
            element.getSpecialAbilities().addSPA(BOMB, bombValue);
            report.addLine("BA bomb racks", "", "BOMB" + bombValue);
        }
    }
}