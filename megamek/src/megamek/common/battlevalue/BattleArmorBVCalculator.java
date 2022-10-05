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
package megamek.common.battlevalue;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class BattleArmorBVCalculator extends BVCalculator {

    private final BattleArmor battleArmor;
    private int currentTrooper;

    BattleArmorBVCalculator(Entity entity) {
        super(entity);
        battleArmor = (BattleArmor) entity;
    }

    @Override
    protected int getRunningTMM() {
        int runMP = battleArmor.getWalkMP(false, false, true, true, false);
        return (runMP > 0) ?
                Compute.getTargetMovementModifier(runMP, false, false, battleArmor.getGame()).getValue()
                : 0;
    }

    @Override
    protected int getJumpingTMM() {
        int rawJump = battleArmor.getJumpMP(false, true, true);
        return (rawJump > 0) ?
                Compute.getTargetMovementModifier(rawJump, true, false, battleArmor.getGame()).getValue()
                : 0;
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        List<String> modifierList = new ArrayList<>();
        double tmmFactor = 1 + (Math.max(tmmRunning, Math.max(tmmJumping, tmmUmu)) / 10.0) + 0.1;
        double tmmBonus = 0.1;
        modifierList.add("BA");
        if (battleArmor.hasCamoSystem()) {
            tmmBonus += 0.2;
            modifierList.add("Camo");
        }
        if ((battleArmor.getStealthName() != null)
                && battleArmor.getStealthName().equals(BattleArmor.IMPROVED_STEALTH_ARMOR)) {
            tmmBonus += 0.3;
            modifierList.add("Imp. Stealth");
        } else if (battleArmor.isStealthy()) {
            tmmBonus += 0.2;
            modifierList.add("Stealth");
        }
        if (battleArmor.isMimetic()) {
            tmmBonus += 0.3;
            modifierList.add("Mimetic");
        }
        String calculation = formatForReport(tmmFactor);
        if (tmmBonus > 0) {
            String modifiers = " (" + String.join(", ", modifierList) + ")";
            calculation += " + " + tmmBonus + modifiers;
            tmmFactor += tmmBonus;
        }
        bvReport.addLine("TMM Factor:", calculation, "");
        return tmmFactor;
    }

    @Override
    protected boolean countAsOffensiveWeapon(Mounted equipment) {
        return super.countAsOffensiveWeapon(equipment) && !equipment.getType().hasFlag(WeaponType.F_INFANTRY);
    }

    @Override
    protected void processWeapons() {
        List<String> modifierList = new ArrayList<>();
        double antiMek = 0;
        for (Mounted weapon : battleArmor.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_INFANTRY) || !countAsOffensiveWeapon(weapon)) {
                continue;
            }

            double weaponBV = weapon.getType().getBV(battleArmor);
            String name = weapon.getType().getName();
            modifierList.clear();
            if ((weapon.getLocation() != BattleArmor.LOC_SQUAD) || (weapon.isSquadSupportWeapon())) {
                weaponBV /= battleArmor.getTotalOInternal();
                modifierList.add("Support");
                name += " (Support)";
            }
            if (battleArmor.canMakeAntiMekAttacks() && !weapon.getType().hasFlag(WeaponType.F_MISSILE)
                    && !weapon.isBodyMounted()) {
                antiMek += weaponBV;
                modifierList.add("counts for AM");
            }
            offensiveValue += weaponBV;
            String modifiers = modifierList.isEmpty() ? "" : " (" + String.join(", ", modifierList) + ")";
            bvReport.addLine(name, "+ " + formatForReport(weaponBV) + modifiers,
                    "= " + formatForReport(offensiveValue));
        }
        for (Mounted misc : battleArmor.getMisc()) {
            if ((misc.getLocation() == BattleArmor.LOC_SQUAD) || (misc.getLocation() == currentTrooper)) {
                if (misc.getType().hasFlag(MiscType.F_MAGNET_CLAW) || misc.getType().hasFlag(MiscType.F_VIBROCLAW)) {
                    antiMek += misc.getType().getBV(battleArmor);
                    bvReport.addLine(misc.getType().getName(),
                            "+ " + formatForReport(misc.getType().getBV(battleArmor)) + " (only AM)", "");
                }
            }
        }

        offensiveValue += antiMek;
        if (battleArmor.canMakeAntiMekAttacks()) {
            bvReport.addLine("Anti-Mek", "+ " + formatForReport(antiMek), "= " + offensiveValue);
        }
    }

    @Override
    protected void processCalculations() {
        // Test if all troopers are exactly the same
        CalculationReport saveReport = bvReport;
        bvReport = new DummyCalculationReport();
        Set<Double> trooperBVs = new HashSet<>();
        for (currentTrooper = 1; currentTrooper < battleArmor.locations(); currentTrooper++) {
            processTrooper();
            trooperBVs.add(baseBV);
        }
        bvReport = saveReport;

        // Write a single trooper in the report if they're all the same
        if (trooperBVs.size() == 1) {
            currentTrooper = 1;
            processTrooper();
            baseBV *= battleArmor.getShootingStrength();
        } else {
            double bvSum = 0;
            for (currentTrooper = 1; currentTrooper < battleArmor.locations(); currentTrooper++) {
                bvReport.addSubHeader("Trooper " + currentTrooper + ":");
                processTrooper();
                bvSum += baseBV;
            }
            baseBV = bvSum;
        }

        bvReport.addSubHeader("Squad Result:");
        bvReport.addLine("Total Squad BV", "", formatForReport(baseBV));
        // we have now added all troopers, divide by current strength, then multiply by the unit size modifier
        baseBV /= battleArmor.getShootingStrength();
        bvReport.addLine("Average BV per Trooper", "/ " + battleArmor.getShootingStrength(),
                "= " + formatForReport(baseBV));

//        if (singleTrooper) {
//            return (int) Math.round(squadBV);
//        }

        double squadFactor = 1;
        switch (battleArmor.getShootingStrength()) {
            case 1:
                break;
            case 2:
                squadFactor = 2.2;
                break;
            case 3:
                squadFactor = 3.6;
                break;
            case 4:
                squadFactor = 5.2;
                break;
            case 5:
                squadFactor = 7;
                break;
            case 6:
                squadFactor = 9;
                break;
        }
        bvReport.addLine("Squad Size",
                "x " + squadFactor, "= " + formatForReport(baseBV * squadFactor));
        baseBV *= squadFactor;
    }

    private double processTrooper() {
        offensiveValue = 0;
        defensiveValue = 0;
        baseBV = 0;

        if (battleArmor.getInternal(currentTrooper) <= 0) {
            bvReport.addLine("N/A", "", "");
            bvReport.addEmptyLine();
            return 0;
        }
        super.processCalculations();

        for (Mounted misc : battleArmor.getMisc()) {
            if (!misc.getType().hasFlag(MiscType.F_MINE) && !misc.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                continue;
            }

            double miscBV = misc.getType().getBV(battleArmor);
            String name = misc.getType().getName();
            if (misc.getLocation() != BattleArmor.LOC_SQUAD) {
                miscBV /= battleArmor.getTotalOInternal();
                name += " (Support)";
            }
            baseBV += miscBV;
            bvReport.addLine(name, "+ " + formatForReport(miscBV), "= " + formatForReport(baseBV));
        }

        return baseBV;
    }

    @Override
    protected boolean ammoCounts(Mounted ammo) {
        return super.ammoCounts(ammo)
                && ((ammo.getLocation() == BattleArmor.LOC_SQUAD) || (ammo.getLocation() == currentTrooper));
    }

    @Override
    protected void processArmor() {
        String modifier = "";
        double armorBV = 2.5;
        if (battleArmor.isFireResistant() || battleArmor.isReflective() || battleArmor.isReactive()) {
            armorBV = 3.5;
            modifier = " (Fire-Res./Refl./React.)";
        }
        defensiveValue += battleArmor.getArmor(currentTrooper) * armorBV + 1;
        String calculation = "1 + " + battleArmor.getArmor(currentTrooper) + " x " + formatForReport(armorBV) + modifier;
        bvReport.addLine("Armor:", calculation, formatForReport(defensiveValue));
    }

    @Override
    protected void processStructure() { }

    @Override
    protected void processDefensiveEquipment() {
        List<String> modifierList = new ArrayList<>();
        int bonus = 0;
        if (battleArmor.hasImprovedSensors()) {
            bonus += 1;
            modifierList.add("Imp. Sens.");
        }
        if (battleArmor.hasActiveProbe()) {
            bonus += 1;
            modifierList.add("AP");
        }
        for (Mounted mounted : battleArmor.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_ECM)) {
                if (mounted.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                    bonus += 2;
                    modifierList.add("Angel ECM");
                } else {
                    bonus += 1;
                    modifierList.add("ECM");
                }
                break;
            }
        }
        if (bonus > 0) {
            offensiveValue += bonus;
            String calculation = "+ " + bonus + " (" + String.join(", ", modifierList) + ")";
            bvReport.addLine("Systems:", calculation, "= " + formatForReport(offensiveValue));
        }
        double amsBonus = 0;
        for (Mounted weapon : battleArmor.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_AMS)) {
                if (weapon.getLocation() == BattleArmor.LOC_SQUAD) {
                    amsBonus += weapon.getType().getBV(battleArmor);
                } else {
                    // squad support, count at 1/troopercount
                    amsBonus += weapon.getType().getBV(battleArmor) / battleArmor.getTotalOInternal();
                }
            }
        }
        if (amsBonus > 0) {
            offensiveValue += amsBonus;
            bvReport.addLine("AMS:", "+ " + formatForReport(amsBonus),
                    "= " + formatForReport(offensiveValue));
        }
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        return Math.max(battleArmor.getWalkMP(false, false, true, true, false),
                Math.max(battleArmor.getJumpMP(false, true, true), battleArmor.getActiveUMUCount()));
    }
}