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

import megamek.common.Messages;
import megamek.common.equipment.Mounted;
import megamek.common.options.OptionsConstants;
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

        // Cybernetic Gas Effuser (Pheromone): +0.05 per trooper to Defensive BR (IO pg 79)
        if (infantry.hasAbility(OptionsConstants.MD_GAS_EFFUSER_PHEROMONE)) {
            double pheromoneBonus = men * 0.05;
            defensiveValue += pheromoneBonus;
            bvReport.addLine("Gas Effuser (Pheromone):",
                  men + " x 0.05",
                  "= +" + formatForReport(pheromoneBonus));
        }
    }

    @Override
    protected void processArmor() {
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        // Dermal Camo provides +3 when stationary, use as potential max TMM
        int maxTmm = Math.max(tmmRunning, Math.max(tmmJumping, tmmUmu));
        if (infantry.hasDermalCamoStealth()) {
            int dermalCamoTmm = 3; // +3 when stationary
            if (dermalCamoTmm > maxTmm) {
                maxTmm = dermalCamoTmm;
                bvReport.addLine("Dermal Camo TMM:", "+3 (stationary)");
            }
        }
        double tmmFactor = 1 + (maxTmm / 10.0);

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
            double preAntiMekBV = offensiveValue;
            if (primaryWeapon != null && !primaryWeapon.hasFlag(InfantryWeapon.F_INF_ARCHAIC)) {
                Mounted<?> primaryWeaponMounted = Mounted.createMounted(infantry, primaryWeapon);
                processWeapon(primaryWeaponMounted, true, true, primaryShooterCount);
            }
            if (secondaryWeapon != null && !secondaryWeapon.hasFlag(InfantryWeapon.F_INF_ARCHAIC)) {
                Mounted<?> secondaryWeaponMounted = Mounted.createMounted(infantry, secondaryWeapon);
                processWeapon(secondaryWeaponMounted, true, true, secondaryShooterCount);
            }

            // Apply 1.2x multiplier to Anti-Mek BR if unit has Grappler or Climbing Claws (IO p.84)
            double antiMekMultiplier = infantry.getAntiMekBvMultiplier();
            if (antiMekMultiplier > 1.0) {
                double antiMekBV = offensiveValue - preAntiMekBV;
                double multipliedAntiMekBV = antiMekBV * antiMekMultiplier;
                double bonus = multipliedAntiMekBV - antiMekBV;
                offensiveValue += bonus;
                bvReport.addLine("Anti-Mek x " + formatForReport(antiMekMultiplier) + " (" +
                            infantry.getBestProstheticAntiMekName() + "):",
                      formatForReport(antiMekBV) + " x " + formatForReport(antiMekMultiplier),
                      "= +" + formatForReport(bonus));
            }
        }

        // Cybernetic Gas Effuser (Toxin): +0.23 per trooper to Offensive BR (IO pg 79)
        if (infantry.hasAbility(OptionsConstants.MD_GAS_EFFUSER_TOXIN)) {
            double toxinBonus = originalTroopers * 0.23;
            offensiveValue += toxinBonus;
            bvReport.addLine("Gas Effuser (Toxin):",
                  originalTroopers + " x 0.23",
                  "= +" + formatForReport(toxinBonus));
        }

        int troopers = Math.max(0, infantry.getInternal(Infantry.LOC_INFANTRY));
        if (troopers < originalTroopers) {
            bvReport.addLine("Surviving troopers:",
                  formatForReport(offensiveValue) + " x " + troopers + " / " + originalTroopers,
                  "= " + formatForReport(offensiveValue * troopers / originalTroopers));
            offensiveValue *= (double) troopers / originalTroopers;
        }

        // TSM Implant adds +0.1 per trooper to Weapon Battle Value
        if (entity.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            double tsmBonus = troopers * 0.1;
            offensiveValue += tsmBonus;
            bvReport.addLine(Messages.getString("BV.TSMImplant"),
                  troopers + " x 0.1",
                  "= +" + formatForReport(tsmBonus));
        }

        // Explosive Suicide Implant: +0.12 per trooper to Weapon Battle Value (IO pg 83)
        // Only applies to conventional infantry
        if (infantry.isConventionalInfantry() && infantry.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS)) {
            double suicideImplantBonus = troopers * 0.12;
            offensiveValue += suicideImplantBonus;
            bvReport.addLine(Messages.getString("BV.SuicideImplant"),
                  troopers + " x 0.12",
                  "= +" + formatForReport(suicideImplantBonus));
        }

        // Prosthetic Enhancement adds damage bonus per trooper to Offensive BV (IO p.84)
        // Only applies if the unit has the MD_PL_ENHANCED or MD_PL_I_ENHANCED ability
        // Sum damage from both slots for BV calculation
        boolean hasProstheticAbility = infantry.hasAbility(OptionsConstants.MD_PL_ENHANCED)
              || infantry.hasAbility(OptionsConstants.MD_PL_I_ENHANCED);
        if (hasProstheticAbility) {
            double prostheticDamagePerTrooper = infantry.getProstheticDamageBonus();
            if (prostheticDamagePerTrooper > 0) {
                double prostheticBonus = troopers * prostheticDamagePerTrooper;
                offensiveValue += prostheticBonus;
                bvReport.addLine(Messages.getString("BV.ProstheticEnhancement"),
                      troopers + " x " + formatForReport(prostheticDamagePerTrooper),
                      "= +" + formatForReport(prostheticBonus));
            }
        }

        // Prosthetic Tail, Enhanced: +0.2 per trooper to Offensive Battle Value (IO p.85)
        // Only applies to conventional infantry
        if (infantry.isConventionalInfantry() && infantry.hasAbility(OptionsConstants.MD_PL_TAIL)) {
            double tailBonus = troopers * 0.2;
            offensiveValue += tailBonus;
            bvReport.addLine(Messages.getString("BV.ProstheticTail"),
                  troopers + " x 0.2",
                  "= +" + formatForReport(tailBonus));
        }

        // Note: Prosthetic Glider Wings (MD_PL_GLIDER) have no impact on BV per IO p.85
        // Note: Prosthetic Powered Flight Wings (MD_PL_FLIGHT) DO affect BV per IO p.85 -
        // the 2 VTOL MP is accounted for in getJumpMP() which contributes to the defensive
        // TMM factor via processDefensiveFactor().

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
