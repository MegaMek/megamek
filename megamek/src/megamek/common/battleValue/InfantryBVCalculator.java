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

package megamek.common.battleValue;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.weapons.infantry.InfantryWeapon;

public class InfantryBVCalculator extends BVCalculator {

    Infantry infantry = (Infantry) entity;

    InfantryBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected void processStructure() {
        int men = Math.max(0, entity.getInternal(Infantry.LOC_INFANTRY));
        double dmgDivisor = infantry.calcDamageDivisor();
        defensiveValue = men * 1.5 * dmgDivisor;
        String calculation = men + " x 1.5";
        calculation += dmgDivisor != 1 ? " x " + formatForReport(dmgDivisor) : "";
        bvReport.addLine("Troopers:", calculation, "= " + formatForReport(defensiveValue));
    }

    @Override
    protected void processArmor() {
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        double tmmFactor = super.tmmFactor(tmmRunning, tmmJumping, tmmUmu);
        if (infantry.hasDEST()) {
            tmmFactor += 0.2;
            bvReport.addLine("DEST:", "+0.2");
        }

        if (infantry.hasSneakCamo()) {
            tmmFactor += 0.2;
            bvReport.addLine("Camo (Sneak):", "+0.2");
        }

        if (infantry.hasSneakIR()) {
            tmmFactor += 0.2;
            bvReport.addLine("Camo (IR):", "+0.2");
        }

        if (infantry.hasSneakECM()) {
            tmmFactor += 0.1;
            bvReport.addLine("Camo (ECM):", "+0.1");
        }
        return tmmFactor;
    }

    @Override
    protected void processWeapons() {
        bvReport.addLine("Weapons:", "", "");
        final InfantryWeapon primaryWeapon = infantry.getPrimaryWeapon();
        final InfantryWeapon secondaryWeapon = infantry.getSecondaryWeapon();
        int originalTroopers = Math.max(0, infantry.getOInternal(Infantry.LOC_INFANTRY));
        int secondaryShooterCount = infantry.getSecondaryWeaponsPerSquad() * infantry.getSquadCount();
        int primaryShooterCount = originalTroopers - secondaryShooterCount;

        // Damage dealt by the troopers is averaged over primary and secondary weapons;
        // therefore calculate
        // weapon damage at full strength and then multiply by the surviving trooper
        // ratio
        if (primaryWeapon != null) {
            Mounted<?> primaryWeaponMounted = Mounted.createMounted(infantry, primaryWeapon);
            processWeapon(primaryWeaponMounted, true, true, primaryShooterCount);
        }
        if (secondaryWeapon != null) {
            Mounted<?> secondaryWeaponMounted = Mounted.createMounted(infantry, secondaryWeapon);
            processWeapon(secondaryWeaponMounted, true, true, secondaryShooterCount);
        }

        if (infantry.canMakeAntiMekAttacks()) {
            bvReport.addLine("Anti-Mek:", "", "");
            if (primaryWeapon != null && !primaryWeapon.hasFlag(InfantryWeapon.F_INF_ARCHAIC)) {
                Mounted<?> primaryWeaponMounted = Mounted.createMounted(infantry, primaryWeapon);
                processWeapon(primaryWeaponMounted, true, true, primaryShooterCount);
            }
            if (secondaryWeapon != null && !secondaryWeapon.hasFlag(InfantryWeapon.F_INF_ARCHAIC)) {
                Mounted<?> secondaryWeaponMounted = Mounted.createMounted(infantry, secondaryWeapon);
                processWeapon(secondaryWeaponMounted, true, true, secondaryShooterCount);
            }
        }

        int troopers = Math.max(0, infantry.getInternal(Infantry.LOC_INFANTRY));
        if (troopers < originalTroopers) {
            bvReport.addLine("Surviving troopers:",
                  formatForReport(offensiveValue) + " x " + troopers + " / " + originalTroopers,
                  "= " + formatForReport(offensiveValue * troopers / originalTroopers));
            offensiveValue *= (double) troopers / originalTroopers;
        }

        bvReport.startTentativeSection();
        bvReport.addLine("Field Guns:", "", "");
        Predicate<Mounted<?>> weaponFilter = m -> countAsOffensiveWeapon(m)
              && m.getLocation() == Infantry.LOC_FIELD_GUNS;
        double fieldGunBV = processWeaponSection(true, weaponFilter, true);
        bvReport.finalizeTentativeSection(fieldGunBV > 0);

        if (offensiveValue == 0) {
            bvReport.addLine("- None.", "", "0");
        }
    }

    @Override
    protected void processSummarize() {
        baseBV = defensiveValue + offensiveValue;
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Battle Value:");
        bvReport.addLine("Defensive BR + Offensive BR:",
              formatForReport(defensiveValue) + " + " + formatForReport(offensiveValue),
              "= " + formatForReport(baseBV));

        List<String> modifierList = new ArrayList<>();
        double typeModifier = 1;

        if (infantry.hasSpecialization(Infantry.COMBAT_ENGINEERS)) {
            typeModifier += 0.1;
            modifierList.add("Combat Eng.");
        }

        if (infantry.hasSpecialization(Infantry.MARINES)) {
            typeModifier += 0.3;
            modifierList.add("Marines");
        }

        if (infantry.hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
            typeModifier += 0.2;
            modifierList.add("Mtn. Troops");
        }

        if (infantry.hasSpecialization(Infantry.PARATROOPS)) {
            typeModifier += 0.1;
            modifierList.add("Paratroops");
        }

        if (infantry.hasSpecialization(Infantry.SCUBA)) {
            typeModifier += 0.1;
            modifierList.add("SCUBA");
        }

        if (infantry.hasSpecialization(Infantry.XCT)) {
            typeModifier += 0.1;
            modifierList.add("XCT");
        }

        if (typeModifier != 1) {
            String calculation = formatForReport(baseBV) + " x " + formatForReport(typeModifier);
            calculation += " (" + String.join(", ", modifierList) + ")";
            bvReport.addLine("Type Modifier:", calculation,
                  "= " + formatForReport(baseBV * typeModifier));
            baseBV *= typeModifier;
        }
        bvReport.addLine("--- Base Unit BV:", "" + (int) Math.round(baseBV));
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        return Math.max(runMP, Math.max(jumpMP, umuMP));
    }

    @Override
    protected String equipmentDescriptor(Mounted<?> mounted) {
        return mounted.getType().getShortName();
    }
}
