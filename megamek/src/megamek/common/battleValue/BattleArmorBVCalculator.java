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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.MPCalculationSetting;
import megamek.common.Messages;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;

public class BattleArmorBVCalculator extends BVCalculator {

    private int currentTrooper;

    BattleArmorBVCalculator(Entity entity) {
        super(entity);
    }

    /**
     * Calculates the Battle Value of a single trooper of this BattleArmor. This value is not influenced by the pilot
     * skill or any force bonuses.
     *
     * @return The BV of a single trooper of this BattleArmor
     */
    public int singleTrooperBattleValue() {
        reset();
        bvReport = new DummyCalculationReport();
        currentTrooper = 1;
        processTrooper();
        return (int) Math.round(baseBV);
    }

    @Override
    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        BattleArmor battleArmor = (BattleArmor) entity;
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
        String modifiers = " (" + String.join(", ", modifierList) + ")";
        calculation += " + " + formatForReport(tmmBonus) + modifiers;
        tmmFactor += tmmBonus;
        bvReport.addLine("TMM Factor:", calculation, "");
        return tmmFactor;
    }

    @Override
    protected boolean countAsOffensiveWeapon(Mounted<?> equipment) {
        // explanation from https://bg.battletech.com/forums/ground-combat/battle-armor-bv/
        // may not be available anymore
        return super.countAsOffensiveWeapon(equipment)
              && !equipment.getType().isAnyOf(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
    }

    protected void reportPossibleWeaponSection(String name, Predicate<Mounted<?>> weaponFilter) {
        bvReport.startTentativeSection();
        bvReport.addLine(name, "", "");
        double resultingBV = processWeaponSection(true, weaponFilter, true);
        bvReport.finalizeTentativeSection(resultingBV > 0);
    }

    private boolean isAnyBattleClaw(Mounted<?> mounted) {
        EquipmentType type = mounted.getType();
        return (type instanceof MiscType)
              && (type.hasFlag(MiscType.F_VIBROCLAW) || type.hasFlag(MiscType.F_MAGNET_CLAW));
    }

    Predicate<Mounted<?>> weaponFilter = m -> (m.getLocation() == BattleArmor.LOC_SQUAD)
          && !m.isSquadSupportWeapon() && !isAnyBattleClaw(m);

    Predicate<Mounted<?>> supportFilter = m -> (m instanceof WeaponMounted) && !m.getType()
          .hasFlag(WeaponType.F_INFANTRY)
          && ((m.getLocation() == currentTrooper) || m.isSquadSupportWeapon());

    Predicate<Mounted<?>> antiMekClawFilter = m -> (m instanceof MiscMounted)
          && ((m.getLocation() == BattleArmor.LOC_SQUAD) || (m.getLocation() == currentTrooper))
          && (isAnyBattleClaw(m));

    Predicate<Mounted<?>> antiMekWeaponFilter = m -> (m instanceof WeaponMounted)
          && !m.getType().hasFlag(WeaponType.F_INFANTRY) && !m.getType().hasFlag(WeaponType.F_MISSILE)
          && !m.isBodyMounted()
          && ((m.getLocation() == BattleArmor.LOC_SQUAD) || (m.getLocation() == currentTrooper));

    Predicate<Mounted<?>> antiMekFilter = m -> antiMekClawFilter.test(m) || antiMekWeaponFilter.test(m);

    @Override
    protected void processWeapons() {
        BattleArmor battleArmor = (BattleArmor) entity;
        reportPossibleWeaponSection("Weapons:", weaponFilter);
        reportPossibleWeaponSection("Squad Support:", supportFilter);
        if (battleArmor.canMakeAntiMekAttacks()) {
            reportPossibleWeaponSection("Anti-Mek:", antiMekFilter);
        }
    }

    @Override
    protected void processCalculations() {
        // Test if all troopers are exactly the same without writing to the real report
        BattleArmor battleArmor = (BattleArmor) entity;
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
        // we have now added all troopers, divide by current strength, then multiply by
        // the unit size modifier
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
        BattleArmor battleArmor = (BattleArmor) entity;
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

        // TSM Implant adds +1 BV per trooper before skill modifiers
        if (entity.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            baseBV += 1;
            bvReport.addLine(Messages.getString("BV.TSMImplant"), "+1", "= " + formatForReport(baseBV));
        }

        // Dermal Armor adds +3 BV per trooper before skill modifiers (IO p.78)
        if (entity.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
            baseBV += 3;
            bvReport.addLine(Messages.getString("BV.DermalArmor"), "+3", "= " + formatForReport(baseBV));
        }
    }

    @Override
    protected boolean ammoCounts(AmmoMounted ammo) {
        return ((ammo.getLocation() == BattleArmor.LOC_SQUAD) || (ammo.getLocation() == currentTrooper))
              && super.ammoCounts(ammo);
    }

    @Override
    protected void processArmor() {
        BattleArmor battleArmor = (BattleArmor) entity;

        String modifier = "";
        double armorBV = 2.5;
        if (battleArmor.isFireResistant() || battleArmor.isReflective() || battleArmor.isReactive()) {
            armorBV = 3.5;
            final String armorName = EquipmentType.getArmorTypeName(battleArmor.getArmorType(BattleArmor.LOC_TROOPER_1),
                  TechConstants.isClan(battleArmor.getArmorTechLevel(BattleArmor.LOC_TROOPER_1)));
            final EquipmentType armor = EquipmentType.get(armorName);
            modifier = " (" + armor.getName().replaceAll("^BA\\s+", "") + ")";
        }
        int currentArmor = Math.max(0, battleArmor.getArmor(currentTrooper));
        defensiveValue += currentArmor * armorBV + 1;
        String calculation = "1 + " + currentArmor + " x " + formatForReport(armorBV)
              + modifier;
        bvReport.addLine("Armor:", calculation, formatForReport(defensiveValue));
    }

    @Override
    protected void processStructure() {
    }

    @Override
    protected void processDefensiveEquipment() {
        BattleArmor battleArmor = (BattleArmor) entity;
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
        for (Mounted<?> mounted : battleArmor.getMisc()) {
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
        for (Mounted<?> weapon : battleArmor.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_AMS)) {
                if (weapon.getLocation() == BattleArmor.LOC_SQUAD) {
                    amsBonus += weapon.getType().getBV(battleArmor);
                } else {
                    // squad support, count at 1 / trooper count
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
    protected double getAmmoBV(Mounted<?> ammo) {
        return ((AmmoType) ammo.getType()).getKgPerShotBV(ammo.getUsableShotsLeft());
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        BattleArmor battleArmor = (BattleArmor) entity;
        return Math.max(battleArmor.getWalkMP(MPCalculationSetting.BV_CALCULATION),
              Math.max(battleArmor.getJumpMP(MPCalculationSetting.BV_CALCULATION), battleArmor.getActiveUMUCount()));
    }

    @Override
    protected String equipmentDescriptor(Mounted<?> mounted) {
        return mounted.getType().getShortName();
    }
}
