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
import static megamek.common.alphaStrike.AlphaStrikeElement.EXTREME_RANGE;
import static megamek.common.alphaStrike.AlphaStrikeElement.LONG_RANGE;
import static megamek.common.alphaStrike.AlphaStrikeElement.SHORT_RANGE;
import static megamek.common.alphaStrike.BattleForceSUA.FLK;
import static megamek.common.alphaStrike.BattleForceSUA.OVL;
import static megamek.common.alphaStrike.BattleForceSUA.PNT;
import static megamek.common.alphaStrike.BattleForceSUA.REAR;
import static megamek.common.alphaStrike.BattleForceSUA.REL;
import static megamek.common.alphaStrike.BattleForceSUA.TOR;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Aero;
import megamek.common.units.Entity;

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
    protected void processAMS(Mounted<?> weapon, WeaponType weaponType) {
    }

    @Override
    protected double determineDamage(Mounted<?> weapon, int range) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.getBattleForceClass() == WeaponType.BF_CLASS_TORPEDO) {
            return 0;
        }
        return ((WeaponType) weapon.getType()).getBattleForceDamage(range, weapon.getLinkedBy());
    }

    @Override
    protected double determineSpecialsDamage(WeaponType weaponType, Mounted<?> linked, int range,
          BattleForceSUA dmgType) {
        if ((dmgType == PNT) && weaponType.hasFlag(WeaponType.F_AMS)) {
            return range == SHORT_RANGE ? 0.3 : 0;
        } else {
            return super.determineSpecialsDamage(weaponType, linked, range, dmgType);
        }
    }

    @Override
    protected void processArtillery(Mounted<?> weapon, WeaponType weaponType) {
        if ((weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY) && !isArtilleryCannon(weaponType)) {
            assignToLocations(weapon, getArtilleryType(weaponType), 1);
        }
    }

    @Override
    protected int getHeatGeneration(boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = entity.hasWorkingMisc(MiscType.F_STEALTH) ? 10 : 0;
        for (Mounted<?> mount : weaponsList) {
            totalHeat += weaponHeat(mount, onlyRear, onlyLongRange);
        }
        return totalHeat;
    }

    @Override
    protected int weaponHeat(Mounted<?> weapon, boolean onlyRear, boolean onlyLongRange) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_ONE_SHOT)
              || (onlyRear && !weapon.isRearMounted() && (weapon.getLocation() != Aero.LOC_AFT))
              || (!onlyRear && (weapon.isRearMounted() || (weapon.getLocation() == Aero.LOC_AFT)))
              || (onlyLongRange && weaponType.getBattleForceDamage(LONG_RANGE) == 0)) {
            return 0;
        } else {
            return weaponHeat(weaponType);
        }
    }
}
