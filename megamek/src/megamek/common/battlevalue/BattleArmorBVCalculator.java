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
public class BattleArmorBVCalculator {

    public static int calculateBV(BattleArmor battleArmor, boolean ignoreC3,
                                  boolean ignoreSkill, CalculationReport bvReport) {
        return calculateBV(battleArmor, ignoreC3, ignoreSkill, bvReport, false);
    }

    public static int calculateBV(BattleArmor battleArmor, boolean ignoreC3, boolean ignoreSkill,
                                  CalculationReport bvReport, boolean singleTrooper) {
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(battleArmor.getChassis() + " " + battleArmor.getModel());
        bvReport.addLine("There is currently no report available for BattleArmor.");

        double squadBV = 0;
        for (int i = 1; i < battleArmor.locations(); i++) {
            if (battleArmor.getInternal(i) <= 0) {
                continue;
            }
            double dBV = 0;
            double armorBV = 2.5;
            if (battleArmor.isFireResistant() || battleArmor.isReflective() || battleArmor.isReactive()) {
                armorBV = 3.5;
            }
            dBV += (battleArmor.getArmor(i) * armorBV) + 1;
            // improved sensors add 1
            if (battleArmor.hasImprovedSensors()) {
                dBV += 1;
            }
            // active probes add 1
            if (battleArmor.hasActiveProbe()) {
                dBV += 1;
            }
            // ECM adds 1
            for (Mounted mounted : battleArmor.getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_ECM)) {
                    if (mounted.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                        dBV += 2;
                    } else {
                        dBV += 1;
                    }
                    break;
                }
            }
            for (Mounted weapon : battleArmor.getWeaponList()) {
                if (weapon.getType().hasFlag(WeaponType.F_AMS)) {
                    if (weapon.getLocation() == BattleArmor.LOC_SQUAD) {
                        dBV += weapon.getType().getBV(battleArmor);
                    } else {
                        // squad support, count at 1/troopercount
                        dBV += weapon.getType().getBV(battleArmor) / battleArmor.getTotalOInternal();
                    }
                }
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
            if (battleArmor.hasCamoSystem()) {
                tmmFactor += 0.2;
            }
            if (battleArmor.isStealthy()) {
                tmmFactor += 0.2;
            }
            // improved stealth get's an extra 0.1, for 0.3 total
            if ((battleArmor.getStealthName() != null) && battleArmor.getStealthName().equals(BattleArmor.IMPROVED_STEALTH_ARMOR)) {
                tmmFactor += 0.1;
            }
            if (battleArmor.isMimetic()) {
                tmmFactor += 0.3;
            }

            dBV *= tmmFactor;
            double oBV = 0;
            for (Mounted weapon : battleArmor.getWeaponList()) {
                // infantry weapons don't count at all
                if (weapon.getType().hasFlag(WeaponType.F_INFANTRY) || weapon.getType().hasFlag(WeaponType.F_AMS)) {
                    continue;
                }

                if (weapon.getLocation() == BattleArmor.LOC_SQUAD) {
                    // Squad support, count at 1/troopercount
                    if (weapon.isSquadSupportWeapon()) {
                        oBV += weapon.getType().getBV(battleArmor) / battleArmor.getTotalOInternal();
                    } else {
                        oBV += weapon.getType().getBV(battleArmor);
                    }
                } else {
                    oBV += weapon.getType().getBV(battleArmor) / battleArmor.getTotalOInternal();
                }
            }

            for (Mounted misc : battleArmor.getMisc()) {
                if (misc.getType().hasFlag(MiscType.F_MINE)) {
                    if (misc.getLocation() == BattleArmor.LOC_SQUAD) {
                        oBV += misc.getType().getBV(battleArmor);
                    } else {
                        oBV += misc.getType().getBV(battleArmor) / battleArmor.getTotalOInternal();
                    }
                }
                if (misc.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    if (misc.getLocation() == BattleArmor.LOC_SQUAD) {
                        oBV += misc.getType().getBV(battleArmor);
                    } else {
                        oBV += misc.getType().getBV(battleArmor) / battleArmor.getTotalOInternal();
                    }
                }
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
                }
            }
            if (battleArmor.canMakeAntiMekAttacks()) {
                // all non-missile and non-body mounted direct fire weapons counted again
                for (Mounted weapon : battleArmor.getWeaponList()) {
                    // infantry weapons don't count at all
                    if (weapon.getType().hasFlag(WeaponType.F_INFANTRY) || weapon.getType().hasFlag(WeaponType.F_AMS)) {
                        continue;
                    }

                    if (weapon.getLocation() == BattleArmor.LOC_SQUAD) {
                        if (!weapon.getType().hasFlag(WeaponType.F_MISSILE) && !weapon.isBodyMounted()) {
                            oBV += weapon.getType().getBV(battleArmor);
                        }
                    } else {
                        // squad support, count at 1/troopercount
                        oBV += weapon.getType().getBV(battleArmor) / battleArmor.getTotalOInternal();
                    }
                }
                // magnetic claws and vibro claws counted again
                for (Mounted misc : battleArmor.getMisc()) {
                    if ((misc.getLocation() == BattleArmor.LOC_SQUAD) || (misc.getLocation() == i)) {
                        if (misc.getType().hasFlag(MiscType.F_MAGNET_CLAW) || misc.getType().hasFlag(MiscType.F_VIBROCLAW)) {
                            oBV += misc.getType().getBV(battleArmor);
                        }
                    }
                }
            }
            // getJumpMP won't return UMU MP, so weed need to count that extra
            int movement = Math.max(battleArmor.getWalkMP(false, false, true, true, false),
                    Math.max(battleArmor.getJumpMP(false, true, true), battleArmor.getActiveUMUCount()));
            double speedFactor = Math.pow(1 + ((double) (movement - 5) / 10), 1.2);
            speedFactor = Math.round(speedFactor * 100) / 100.0;
            oBV *= speedFactor;

            double soldierBV = oBV + dBV;
            squadBV += soldierBV;
        }
        // we have now added all troopers, divide by current strength to then
        // multiply by the unit size mod
        squadBV /= battleArmor.getShootingStrength();
        // we might want to get just the BV of a single trooper
        if (singleTrooper) {
            return (int) Math.round(squadBV);
        }

        switch (battleArmor.getShootingStrength()) {
            case 1:
                break;
            case 2:
                squadBV *= 2.2;
                break;
            case 3:
                squadBV *= 3.6;
                break;
            case 4:
                squadBV *= 5.2;
                break;
            case 5:
                squadBV *= 7;
                break;
            case 6:
                squadBV *= 9;
                break;
        }

        if (!ignoreC3) {
            squadBV += battleArmor.getExtraC3BV((int) Math.round(squadBV));
        }

        double pilotFactor = ignoreSkill ? 1 : SkillBVModifier.getBVSkillMultiplier(battleArmor);
        return (int) Math.round(squadBV * pilotFactor);
    }
}
