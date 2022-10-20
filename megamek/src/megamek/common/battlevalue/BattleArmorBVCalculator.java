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
import java.util.function.Predicate;

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
        double tmmFactor = 1 + (Math.max(tmmRunning, Math.max(tmmJumping, tmmUmu)) / 10.0);
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
            calculation += " + " + formatForReport(tmmBonus) + modifiers;
            tmmFactor += tmmBonus;
        }
        bvReport.addLine("TMM Factor:", calculation, "");
        return tmmFactor;
    }

    @Override
    protected boolean countAsOffensiveWeapon(Mounted equipment) {
        // see https://bg.battletech.com/forums/ground-combat/battle-armor-bv/
        return super.countAsOffensiveWeapon(equipment) && !equipment.getType().isAnyOf(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
    }

    protected void reportPossibleWeaponSection(String name, Predicate<Mounted> weaponFilter) {
        bvReport.startTentativeSection();
        bvReport.addLine(name, "", "");
        double resultingBV = processWeaponSection(true, weaponFilter, true);
        bvReport.finalizeTentativeSection(resultingBV > 0);
    }

    Predicate<Mounted> weaponFilter = m -> (m.getLocation() == BattleArmor.LOC_SQUAD)
            && !m.isSquadSupportWeapon();

    Predicate<Mounted> supportFilter = m -> (m.getLocation() == currentTrooper) && !m.getType().hasFlag(WeaponType.F_INFANTRY)
            && ((m.getLocation() != BattleArmor.LOC_SQUAD) || m.isSquadSupportWeapon());

    Predicate<Mounted> antiMekClawFilter = m -> (m.getType() instanceof MiscType)
            && ((m.getLocation() == BattleArmor.LOC_SQUAD) || (m.getLocation() == currentTrooper))
            && (m.getType().hasFlag(MiscType.F_MAGNET_CLAW) || m.getType().hasFlag(MiscType.F_VIBROCLAW));

    Predicate<Mounted> antiMekWeaponFilter = m -> (m.getType() instanceof WeaponType)
            && !m.getType().hasFlag(WeaponType.F_INFANTRY) && !m.getType().hasFlag(WeaponType.F_MISSILE)
            && !m.isBodyMounted();

    Predicate<Mounted> antiMekFilter = m -> antiMekClawFilter.test(m) || antiMekWeaponFilter.test(m);

    @Override
    protected void processWeapons() {
        reportPossibleWeaponSection("Weapons:", weaponFilter);
        reportPossibleWeaponSection("Squad Support:", supportFilter);
        if (battleArmor.canMakeAntiMekAttacks()) {
            reportPossibleWeaponSection("Anti-Mek:", antiMekFilter);
        }
    }

    @Override
    protected void processCalculations() {
        // Test if all troopers are exactly the same without writing to the real report
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

        bvReport.addEmptyLine();
        bvReport.addSubHeader("Squad Battle Value:");
        bvReport.addLine("Total Squad BV:", "", formatForReport(baseBV));
        // we have now added all troopers, divide by current strength, then multiply by the unit size modifier
        baseBV /= battleArmor.getShootingStrength();
        bvReport.addLine("Average BV per Trooper", "/ " + battleArmor.getShootingStrength(),
                "= " + formatForReport(baseBV));

        double squadFactor = (0.9 + 0.1 * battleArmor.getShootingStrength()) * battleArmor.getShootingStrength();
        bvReport.addLine("Squad Size",
                "x " + squadFactor, "= " + formatForReport(baseBV * squadFactor));
        baseBV *= squadFactor;
        bvReport.addLine("--- Base Unit BV:",
                formatForReport(baseBV) + ", rn",
                "= " + (int) Math.round(baseBV));
    }

    private void processTrooper() {
        offensiveValue = 0;
        defensiveValue = 0;
        baseBV = 0;

        if (battleArmor.getInternal(currentTrooper) <= 0) {
            bvReport.addLine("N/A", "", "");
            bvReport.addEmptyLine();
        } else {
            super.processCalculations();
        }
    }

    @Override
    protected double offensiveEquipmentBV(MiscType misc, int location) {
        return location == currentTrooper ? super.offensiveEquipmentBV(misc, location) : 0;
    }

    @Override
    protected void processSummarize() {
        baseBV = defensiveValue + offensiveValue;
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Trooper Battle Value:");
        bvReport.addLine("",
                formatForReport(defensiveValue) + " + " + formatForReport(offensiveValue) + ", rn",
                "= " + formatForReport(baseBV));
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
            final String armorName = EquipmentType.getArmorTypeName(battleArmor.getArmorType(BattleArmor.LOC_TROOPER_1),
                    TechConstants.isClan(battleArmor.getArmorTechLevel(BattleArmor.LOC_TROOPER_1)));
            final EquipmentType armor = EquipmentType.get(armorName);
            modifier = " (" + armor.getName().replaceAll("^BA\\s+", "") + ")";
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
            defensiveValue += bonus;
            String calculation = "+ " + bonus + " (" + String.join(", ", modifierList) + ")";
            bvReport.addLine("Systems:", calculation, "= " + formatForReport(defensiveValue));
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
            defensiveValue += amsBonus;
            bvReport.addLine("AMS:", "+ " + formatForReport(amsBonus),
                    "= " + formatForReport(defensiveValue));
        }
    }

    @Override
    protected double getAmmoBV(Mounted ammo) {
        return ((AmmoType) ammo.getType()).getProtoBV(ammo.getUsableShotsLeft());
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        return Math.max(battleArmor.getWalkMP(false, false, true, true, false),
                Math.max(battleArmor.getJumpMP(false, true, true), battleArmor.getActiveUMUCount()));
    }

    @Override
    protected String equipmentDescriptor(Mounted mounted) {
        return mounted.getType().getShortName();
    }
}