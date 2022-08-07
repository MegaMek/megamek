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

import static megamek.client.ui.swing.calculationReport.CalculationReport.fmt;

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
            String calculation = battleArmor.getArmor(i) + " x " + fmt(armorBV) + " + 1" + modifiers;
            bvReport.addLine("Armor:", calculation, fmt(dBV));
            modifierList.clear();

            int bonus = 0;
            // improved sensors add 1
            if (battleArmor.hasImprovedSensors()) {
                bonus += 1;
                modifierList.add("Imp. Sens.");
            }
            // active probes add 1
            if (battleArmor.hasActiveProbe()) {
                bonus += 1;
                modifierList.add("AP");
            }
            // ECM adds 1
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
                bvReport.addLine("Systems:", calculation, "= " + fmt(dBV));
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
                bvReport.addLine("AMS:", "+ " + fmt(amsBonus), "= " + fmt(dBV));
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
            if ((battleArmor.getStealthName() != null) && battleArmor.getStealthName().equals(BattleArmor.IMPROVED_STEALTH_ARMOR)) {
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
            calculation = fmt(tmmFactor);
            if (tmmBonus > 0) {
                modifiers = " (" + String.join(", ", modifierList) + ")";
                calculation += " + " + tmmBonus + modifiers;
                tmmFactor += tmmBonus;
            }
            bvReport.addLine("TMM Factor:", calculation, "");

            bvReport.addLine("Defensive Battle Rating:",
                    fmt(dBV) + " x " + fmt(tmmFactor), "= " + fmt(dBV * tmmFactor));
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
                bvReport.addLine(name, "+ " + fmt(weaponBV) + modifiers, "= " + fmt(oBV));
            }

            for (Mounted misc : battleArmor.getMisc()) {
                if ((misc.getLocation() == BattleArmor.LOC_SQUAD) || (misc.getLocation() == i)) {
                    if (misc.getType().hasFlag(MiscType.F_MAGNET_CLAW) || misc.getType().hasFlag(MiscType.F_VIBROCLAW)) {
                        antiMek += misc.getType().getBV(battleArmor);
                        bvReport.addLine(misc.getType().getName(),
                                "+ " + fmt(misc.getType().getBV(battleArmor)) + " (only AM)", "");
                    }
                }
            }

            oBV += antiMek;
            if (battleArmor.canMakeAntiMekAttacks()) {
                bvReport.addLine("Anti-Mek", "+ " + fmt(antiMek), "= " + oBV);
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
                bvReport.addLine(name, "+ " + fmt(miscBV), "= " + fmt(oBV));
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
                    bvReport.addLine(ammo.getType().getName(), "+ " + fmt(ammoBV), "= " + fmt(oBV));
                }
            }

            // getJumpMP won't return UMU MP, so weed need to count that extra
            int movement = Math.max(battleArmor.getWalkMP(false, false, true, true, false),
                    Math.max(battleArmor.getJumpMP(false, true, true), battleArmor.getActiveUMUCount()));
            double speedFactor = Math.pow(1 + ((double) (movement - 5) / 10), 1.2);
            speedFactor = Math.round(speedFactor * 100) / 100.0;
            bvReport.addLine("Speed Factor", fmt(speedFactor), "");
            bvReport.addLine("Offensive Battle Rating:",
                    fmt(oBV) + " x " + fmt(speedFactor), "= " + fmt(oBV * speedFactor));
            oBV *= speedFactor;

            double soldierBV = oBV + dBV;
            bvReport.addEmptyLine();
            bvReport.addResultLine("Trooper BV", fmt(oBV) + " + " + fmt(dBV), "= " + fmt(soldierBV));
            squadBV += soldierBV;
            bvReport.addEmptyLine();
        }

        bvReport.addSubHeader("Squad Result:");
        bvReport.addLine("Total Squad BV", "", fmt(squadBV));
        // we have now added all troopers, divide by current strength to then
        // multiply by the unit size mod
        squadBV /= battleArmor.getShootingStrength();
        bvReport.addLine("Average BV per Trooper", "/ " + battleArmor.getShootingStrength(), "= " + fmt(squadBV));
        // we might want to get just the BV of a single trooper
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
                "x " + squadFactor, "= " + fmt(squadBV * squadFactor));
        squadBV *= squadFactor;

        if (!ignoreC3) {
            double c3Bonus = battleArmor.getExtraC3BV((int) Math.round(squadBV));
            squadBV += c3Bonus;
            if (c3Bonus > 0) {
                bvReport.addLine("C3 Bonus", "+ " + fmt(c3Bonus), "= " + fmt(squadBV));
            }
        }

        double pilotFactor = ignoreSkill ? 1 : BvMultiplier.bvMultiplier(battleArmor);
        String pilotCalculation = ignoreSkill ? "" : "x " + fmt(pilotFactor) + " (Skill/Implants), ";
        bvReport.addResultLine("Final BV:",
                pilotCalculation + "round normal", "= " + Math.round(squadBV * pilotFactor));
        return (int) Math.round(squadBV * pilotFactor);
    }
}
