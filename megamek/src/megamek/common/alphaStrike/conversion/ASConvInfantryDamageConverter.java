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

package megamek.common.alphaStrike.conversion;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.BattleForceSUA.AC;
import static megamek.common.alphaStrike.BattleForceSUA.AM;
import static megamek.common.alphaStrike.BattleForceSUA.FLK;
import static megamek.common.alphaStrike.BattleForceSUA.HT;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.weapons.attacks.InfantryAttack;

public class ASConvInfantryDamageConverter extends ASDamageConverter {

    private final Infantry infantry;

    protected ASConvInfantryDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
        infantry = (Infantry) entity;
    }

    @Override
    protected void processDamage() {
        if (infantry.hasFieldWeapon() && !infantry.hasActiveFieldArtillery()) {
            processSDamage();
            processMDamage();
            processLDamage();
            processFrontSpecialDamage(AC);
            processFrontSpecialDamage(FLK);
        } else {
            int baseRange = 0;
            if ((infantry.getSecondaryWeapon() != null) && (infantry.getSecondaryWeaponsPerSquad() >= 2)) {
                baseRange = infantry.getSecondaryWeapon().getInfantryRange();
            } else if (infantry.getPrimaryWeapon() != null) {
                baseRange = infantry.getPrimaryWeapon().getInfantryRange();
            }
            int range = baseRange * 3;
            String maxRangeText;
            finalSDamage = ASDamage.createDualRoundedUp(getConvInfantryStandardDamage());
            if (range > 15) {
                finalLDamage = finalSDamage;
                finalMDamage = finalSDamage;
                maxRangeText = "Ranges: S, M, L";
            } else if (range > 3) {
                finalMDamage = finalSDamage;
                maxRangeText = "Ranges: S, M";
            } else {
                maxRangeText = "Range: S";
            }
            report.addLine("Final Damage", "", finalSDamage + "");
            report.addLine("Range:", range + " hexes", maxRangeText);
        }

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
    protected void assignSpecialAbilities(Mounted<?> weapon, WeaponType weaponType) {
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
        for (Mounted<?> weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            if ((weaponType.hasFlag(WeaponType.F_FLAMER) || weaponType.hasFlag(WeaponType.F_PLASMA))
                  && (ASLocationMapper.damageLocationMultiplier(entity, 0, weapon) > 0)) {
                if (finalSDamage.damage < 1) {
                    report.addLine("Insufficient S damage", "No HT", "");
                } else {
                    report.addLine(weapon.getName(), "(has heat damage)", "");
                    final int heatDmg = Math.min(2, finalSDamage.damage);
                    int mHeatDmg = (finalMDamage != null) ? heatDmg : 0;
                    int lHeatDmg = (finalLDamage != null) ? heatDmg : 0;
                    ASDamageVector finalHtValue = ASDamageVector.createNormRndDmg(heatDmg, mHeatDmg, lHeatDmg);
                    locations[0].setSUA(HT, finalHtValue);
                    report.addLine("Final Ability", "", "HT" + finalHtValue);
                }
                report.endTentativeSection();
                return;
            }
        }
        report.discardTentativeSection();
    }
}
