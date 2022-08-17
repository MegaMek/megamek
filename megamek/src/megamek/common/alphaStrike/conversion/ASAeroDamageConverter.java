/*
 *
 *  * Copyright (c) 14.08.22, 09:43 - The MegaMek Team. All Rights Reserved.
 *  *
 *  * This file is part of MegaMek.
 *  *
 *  * MegaMek is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MegaMek is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.bayweapons.BayWeapon;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.AlphaStrikeElement.EXTREME_RANGE;
import static megamek.common.alphaStrike.AlphaStrikeElement.LONG_RANGE;
import static megamek.common.alphaStrike.BattleForceSUA.OVL;
import static megamek.common.alphaStrike.BattleForceSUA.PNT;

class ASAeroDamageConverter extends ASDamageConverter2 {

    ASAeroDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processDamage() {
        processMDamage();
        processSDamage();
        processLDamage();
        processEDamage();
    }

    @Override
    protected void processEDamage() {
        report.addEmptyLine();
        report.addLine("--- Extreme Range Damage:", "");
        double eDamage = calculateFrontDamage(weaponsList, EXTREME_RANGE);

        if (element.hasSUA(OVL)) {
            report.addLine("Adjusted Damage: ",
                    formatForReport(eDamage) + " x (see M)",
                    "= " + formatForReport(eDamage * heatAdjustFactorLE));
            eDamage = eDamage * heatAdjustFactorLE;
        }

        finalEDamage = ASDamage.createDualRoundedUp(eDamage);
        report.addLine("Final E damage:",
                formatForReport(eDamage) + ", dual rounded", "= " + finalEDamage.toStringWithZero());
    }

    @Override
    protected void processAMS(Mounted weapon, WeaponType weaponType) {
        if (weaponType.hasFlag(WeaponType.F_AMS)) {
            assignToLocations(weapon, PNT, 0.3);
        }
    }

    @Override
    protected void processArtillery(Mounted weapon, WeaponType weaponType) {
        if ((weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY) && !isArtilleryCannon(weaponType)) {
            assignToLocations(weapon, getArtilleryType(weaponType), 1);
        }
    }

    @Override
    protected int getHeatGeneration(boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = entity.hasWorkingMisc(MiscType.F_STEALTH, -1) ? 10 : 0;

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    totalHeat += aeroWeaponHeat(entity.getEquipment(index), onlyRear, onlyLongRange);
                }
            } else {
                totalHeat += aeroWeaponHeat(mount, onlyRear, onlyLongRange);
            }
        }
        return totalHeat;
    }

    private static int aeroWeaponHeat(Mounted weapon, boolean onlyRear, boolean onlyLongRange) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_ONESHOT)
                || (onlyRear && !weapon.isRearMounted() && (weapon.getLocation() != Aero.LOC_AFT))
                || (!onlyRear && (weapon.isRearMounted() || (weapon.getLocation() == Aero.LOC_AFT)))
                || (onlyLongRange && weaponType.getBattleForceDamage(LONG_RANGE) == 0)) {
            return 0;
        } else {
            return weaponHeat(weaponType);
        }
    }
}
