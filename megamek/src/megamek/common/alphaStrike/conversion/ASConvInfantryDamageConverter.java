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
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.InfantryAttack;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASConvInfantryDamageConverter extends ASDamageConverter2 {

    private final Infantry infantry;

    protected ASConvInfantryDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
        infantry = (Infantry) entity;
    }

    @Override
    protected void processDamage() {
        report.addEmptyLine();
        report.addLine("--- Damage:", "");
        int baseRange = 0;
        if ((infantry.getSecondaryWeapon() != null) && (infantry.getSecondaryN() >= 2)) {
            baseRange = infantry.getSecondaryWeapon().getInfantryRange();
        } else if (infantry.getPrimaryWeapon() != null) {
            baseRange = infantry.getPrimaryWeapon().getInfantryRange();
        }
        int range = baseRange * 3;
        finalSDamage = ASDamage.createDualRoundedUp(getConvInfantryStandardDamage());
        String maxRangeText = "Range: S";
        if (range > 3) {
            finalMDamage = finalSDamage;
            maxRangeText = "Ranges: S, M";
        }
        if (range > 15) {
            finalLDamage = finalSDamage;
            maxRangeText = "Ranges: S, M, L";
        }
        report.addLine("Final Damage", "", finalSDamage + "");
        report.addLine("Range:", range + " hexes", maxRangeText);

        processHT();
    }

    private double getConvInfantryStandardDamage() {
        int troopFactor = TROOP_FACTOR[Math.min(infantry.getShootingStrength(), 30)];
        double damagePerTrooper = infantry.getDamagePerTrooper();
        double damage = damagePerTrooper * troopFactor / 10;
        report.addLine("Damage",
                formatForReport(damagePerTrooper) + " x " + troopFactor + " / 10",
                "= " + formatForReport(damage));
        return damage;
    }

    @Override
    protected void assignSpecialAbilities(Mounted weapon, WeaponType weaponType) {
        super.assignSpecialAbilities(weapon, weaponType);

        if (weaponType instanceof InfantryAttack) {
            assignToLocations(weapon, AM);
        }
    }

    @Override
    protected void processHT() {
        report.startTentativeSection();
        report.addEmptyLine();
        report.addLine("--- Heat Damage (HT):", "");
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            if ((weaponType.hasFlag(WeaponType.F_FLAMER) || weaponType.hasFlag(WeaponType.F_PLASMA))
                    && (ASLocationMapper.damageLocationMultiplier(entity, 0, weapon) > 0)) {
                //TODO: Rules are unclear how to deal with an S damage value of 0*
                if (finalSDamage.damage < 1) {
                    report.addLine("No S damage", "No HT", "");
                } else {
                    report.addLine(weapon.getName(), "(has heat damage)", "");
                    ASDamageVector finalHtValue = ASDamageVector.createNormRndDmg(Math.min(2, finalSDamage.damage), 0, 0);
                    element.getSpecialAbilities().addSPA(HT, finalHtValue);
                    report.addLine("Final Ability", "", "HT" + finalHtValue);
                }
                report.endTentativeSection();
                return;
            }
        }
        report.discardTentativeSection();
    }
}