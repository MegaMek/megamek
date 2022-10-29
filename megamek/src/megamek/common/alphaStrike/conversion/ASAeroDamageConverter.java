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

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

class ASAeroDamageConverter extends ASDamageConverter {

    ASAeroDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processDamage() {
        calculateHeatAdjustment();
        processSDamage();
        processMDamage();
        processLDamage();
        processEDamage();
        processHT();
        processSpecialDamage(REAR, rearLocation);
        processFrontSpecialDamage(TOR);
        processFrontSpecialDamage(REL);
        processFrontSpecialDamage(PNT);
        processFrontSpecialDamage(FLK);
    }

    @Override
    protected void processEDamage() {
        report.addEmptyLine();
        report.addLine("--- Extreme Range Damage:", "");
        double eDamage = assembleFrontDamage(EXTREME_RANGE);

        if (element.hasSUA(OVL)) {
            report.addLine("Adjusted Damage: ",
                    formatForReport(eDamage) + " x (see M)",
                    "= " + formatForReport(eDamage * heatAdjustFactor));
            eDamage = eDamage * heatAdjustFactor;
        } else if (heatAdjustFactorLE < 1) {
            report.addLine("Adjusted Damage: ",
                    formatForReport(eDamage) + " x (see L)",
                    "= " + formatForReport(eDamage * heatAdjustFactorLE));
            eDamage = eDamage * heatAdjustFactor;
        }

        finalEDamage = ASDamage.createDualRoundedUp(eDamage);
        report.addLine("Final E damage:",
                formatForReport(eDamage) + ", " + rdUp, "= " + finalEDamage.toStringWithZero());
    }

    @Override
    protected void processAMS(Mounted weapon, WeaponType weaponType) { }

    @Override
    protected double determineDamage(Mounted weapon, int range) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.getBattleForceClass() == WeaponType.BFCLASS_TORP) {
            return 0;
        }
        return ((WeaponType) weapon.getType()).getBattleForceDamage(range, weapon.getLinkedBy());
    }

    @Override
    protected double determineSpecialsDamage(WeaponType weaponType, Mounted linked, int range, BattleForceSUA dmgType) {
        if ((dmgType == PNT) && weaponType.hasFlag(WeaponType.F_AMS)) {
            return range == SHORT_RANGE ? 0.3 : 0;
        } else {
            return super.determineSpecialsDamage(weaponType, linked, range, dmgType);
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
        for (Mounted mount : weaponsList) {
            totalHeat += weaponHeat(mount, onlyRear, onlyLongRange);
        }
        return totalHeat;
    }

    @Override
    protected int weaponHeat(Mounted weapon, boolean onlyRear, boolean onlyLongRange) {
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