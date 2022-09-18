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
import megamek.common.*;

import java.util.ArrayList;
import java.util.List;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class BattleArmorBVCalculator {

    public static int calculateBV(BattleArmor battleArmor, boolean ignoreC3,
                                  boolean ignoreSkill, CalculationReport bvReport) {
        return calculateBV(battleArmor, ignoreC3, ignoreSkill, bvReport, false);
    }

    public static int calculateBV(BattleArmor battleArmor, boolean ignoreC3, boolean ignoreSkill,
                                  CalculationReport bvReport, boolean singleTrooper) {
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(battleArmor.getChassis() + " " + battleArmor.getModel());

        double squadBV = 0;
        for (int i = 1; i < battleArmor.locations(); i++) {
            bvReport.addSubHeader("Trooper " + i + ":");
            if (battleArmor.getInternal(i) <= 0) {
                bvReport.addLine("N/A", "", "");
                bvReport.addEmptyLine();
                continue;
            }

            // --- Defensive Value
            bvReport.addLine("--- Defensive Battle Rating:", "");
            List<String> modifierList = new ArrayList<>();
            double armorBV = 2.5;
            if (battleArmor.isFireResistant() || battleArmor.isReflective() || battleArmor.isReactive()) {
                armorBV = 3.5;
                modifierList.add("Fire-Res./Refl./React.");
            }
            double dBV = battleArmor.getArmor(i) * armorBV + 1;
            String modifiers = modifierList.isEmpty() ? "" : " (" + String.join(", ", modifierList) + ")";
            String calculation = battleArmor.getArmor(i) + " x " + formatForReport(armorBV) + " + 1" + modifiers;
            bvReport.addLine("Armor:", calculation, formatForReport(dBV));
            modifierList.clear();

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
                dBV += bonus;
                calculation = "+ " + bonus + " (" + String.join(", ", modifierList) + ")";
                bvReport.addLine("Systems:", calculation, "= " + formatForReport(dBV));
            }
            modifierList.clear();

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
                dBV += amsBonus;
                bvReport.addLine("AMS:", "+ " + formatForReport(amsBonus), "= " + formatForReport(dBV));
            }

            int runMP = battleArmor.getWalkMP(false, false, true, true, false);
            int umuMP = battleArmor.getActiveUMUCount();
            int tmmRan = Compute.getTargetMovementModifier(Math.max(runMP, umuMP), false, false, battleArmor.getGame()).getValue();
            // get jump MP, ignoring burden
            int rawJump = battleArmor.getJumpMP(false, true, true);
            int tmmJumped = (rawJump > 0) ?
                    Compute.getTargetMovementModifier(rawJump, true, false, battleArmor.getGame()).getValue()
                    : 0;
            double targetMovementModifier = Math.max(tmmRan, tmmJumped);
            double tmmFactor = 1 + (targetMovementModifier / 10) + 0.1;
            double tmmBonus = 0;
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
            calculation = formatForReport(tmmFactor);
            if (tmmBonus > 0) {
                modifiers = " (" + String.join(", ", modifierList) + ")";
                calculation += " + " + tmmBonus + modifiers;
                tmmFactor += tmmBonus;
            }
            bvReport.addLine("TMM Factor:", calculation, "");

            bvReport.addLine("Defensive Battle Rating:",
                    formatForReport(dBV) + " x " + formatForReport(tmmFactor),
                    "= " + formatForReport(dBV * tmmFactor));
            dBV *= tmmFactor;

            // --- Offensive Value
            bvReport.addEmptyLine();
            bvReport.addLine("--- Offensive Battle Rating:", "");
            double oBV = 0;
            double antiMek = 0;
            for (Mounted weapon : battleArmor.getWeaponList()) {
                // infantry weapons don't count at all
                if (weapon.getType().hasFlag(WeaponType.F_INFANTRY) || weapon.getType().hasFlag(WeaponType.F_AMS)
                        || (weapon.getType().getBV(battleArmor) == 0)) {
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
                    modifierList.add("count for AM");
                }
                oBV += weaponBV;
                modifiers = modifierList.isEmpty() ? "" : " (" + String.join(", ", modifierList) + ")";
                bvReport.addLine(name, "+ " + formatForReport(weaponBV) + modifiers,
                        "= " + formatForReport(oBV));
            }

            for (Mounted misc : battleArmor.getMisc()) {
                if ((misc.getLocation() == BattleArmor.LOC_SQUAD) || (misc.getLocation() == i)) {
                    if (misc.getType().hasFlag(MiscType.F_MAGNET_CLAW) || misc.getType().hasFlag(MiscType.F_VIBROCLAW)) {
                        antiMek += misc.getType().getBV(battleArmor);
                        bvReport.addLine(misc.getType().getName(),
                                "+ " + formatForReport(misc.getType().getBV(battleArmor)) + " (only AM)", "");
                    }
                }
            }

            oBV += antiMek;
            if (battleArmor.canMakeAntiMekAttacks()) {
                bvReport.addLine("Anti-Mek", "+ " + formatForReport(antiMek), "= " + oBV);
            }

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
                oBV += miscBV;
                bvReport.addLine(name, "+ " + formatForReport(miscBV), "= " + formatForReport(oBV));
            }

            for (Mounted ammo : battleArmor.getAmmo()) {
                int loc = ammo.getLocation();
                // don't count oneshot ammo
                if (loc == Entity.LOC_NONE) {
                    continue;
                }
                if ((loc == BattleArmor.LOC_SQUAD) || (loc == i)) {
                    double ammoBV = ((AmmoType) ammo.getType()).getBABV();
                    oBV += ammoBV;
                    bvReport.addLine(ammo.getType().getName(), "+ " + formatForReport(ammoBV),
                            "= " + formatForReport(oBV));
                }
            }

            // getJumpMP won't return UMU MP, so weed need to count that extra
            int movement = Math.max(battleArmor.getWalkMP(false, false, true, true, false),
                    Math.max(battleArmor.getJumpMP(false, true, true), battleArmor.getActiveUMUCount()));
            double speedFactor = Math.pow(1 + ((double) (movement - 5) / 10), 1.2);
            speedFactor = Math.round(speedFactor * 100) / 100.0;
            bvReport.addLine("Speed Factor", formatForReport(speedFactor), "");
            bvReport.addLine("Offensive Battle Rating:",
                    formatForReport(oBV) + " x " + formatForReport(speedFactor),
                    "= " + formatForReport(oBV * speedFactor));
            oBV *= speedFactor;

            double soldierBV = oBV + dBV;
            bvReport.addEmptyLine();
            bvReport.addResultLine("Trooper BV", formatForReport(oBV) + " + " + formatForReport(dBV),
                    "= " + formatForReport(soldierBV));
            squadBV += soldierBV;
            bvReport.addEmptyLine();
        }

        bvReport.addSubHeader("Squad Result:");
        bvReport.addLine("Total Squad BV", "", formatForReport(squadBV));
        // we have now added all troopers, divide by current strength, then multiply by the unit size modifier
        squadBV /= battleArmor.getShootingStrength();
        bvReport.addLine("Average BV per Trooper", "/ " + battleArmor.getShootingStrength(),
                "= " + formatForReport(squadBV));

        if (singleTrooper) {
            return (int) Math.round(squadBV);
        }

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
                "x " + squadFactor, "= " + formatForReport(squadBV * squadFactor));
        squadBV *= squadFactor;

        // Force Bonuses
        double tagBonus = BVCalculator.bvTagBonus(battleArmor);
        if (tagBonus > 0) {
            squadBV += tagBonus;
            bvReport.addEmptyLine();
            bvReport.addLine("Force Bonus (TAG):",
                    "+ " + formatForReport(tagBonus), "= " + formatForReport(squadBV));
        }

        double c3Bonus = ignoreC3 ? 0 : battleArmor.getExtraC3BV((int) Math.round(squadBV));
        if (c3Bonus > 0) {
            squadBV += c3Bonus;
            bvReport.addEmptyLine();
            bvReport.addLine("Force Bonus (C3):",
                    "+ " + formatForReport(c3Bonus), "= " + formatForReport(squadBV));
        }

        double pilotFactor = ignoreSkill ? 1 : BVCalculator.bvMultiplier(battleArmor);
        if (pilotFactor != 1) {
            squadBV *= pilotFactor;
            bvReport.addEmptyLine();
            bvReport.addLine("Pilot Modifier:",
                    "x " + formatForReport(pilotFactor), "= " + formatForReport(squadBV));
        }

        int finalAdjustedBV = (int) Math.round(squadBV);
        bvReport.addResultLine("Final BV", "= ", finalAdjustedBV);
        return finalAdjustedBV;
    }
}
