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
import java.util.HashMap;
import java.util.Map;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class ProtoMekBVCalculator {

    public static int calculateBV(Protomech protoMek, boolean ignoreSkill, CalculationReport bvReport) {
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(protoMek.getChassis() + " " + protoMek.getModel());
        bvReport.addSubHeader("Defensive Battle Rating Calculation:");

        double dbv = 0; // defensive battle value
        double obv; // offensive bv

        dbv += protoMek.getTotalArmor() * 2.5;
        bvReport.addLine("Total Armor (" + protoMek.getTotalArmor() + ") x 2.5", "", protoMek.getTotalArmor() * 2.5);
        dbv += protoMek.getTotalInternal() * 1.5;
        bvReport.addLine("Total I.S. Points (" + protoMek.getTotalInternal() +") x 1.5", "", protoMek.getTotalInternal() * 1.5);

        // add defensive equipment
        double dEquipmentBV = 0;
        double amsBV = 0;
        double amsAmmoBV = 0;
        for (Mounted mounted : protoMek.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                amsAmmoBV += atype.getBV(protoMek);
            }
        }
        for (Mounted mounted : protoMek.getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_AMS))
                    || ((etype instanceof MiscType) && (etype.hasFlag(MiscType.F_ECM)
                    || etype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                    || etype.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                    || etype.hasFlag(MiscType.F_BAP)))) {
                dEquipmentBV += etype.getBV(protoMek);
            }
            if (etype instanceof WeaponType) {
                WeaponType wtype = (WeaponType) etype;
                if (wtype.hasFlag(WeaponType.F_AMS)
                        && (wtype.getAmmoType() == AmmoType.T_AMS)) {
                    amsBV += etype.getBV(protoMek);
                }
            }
        }
        if (amsAmmoBV > 0) {
            dEquipmentBV += Math.min(amsBV, amsAmmoBV);
        }
        dbv += dEquipmentBV;
        bvReport.addLine("Total Equipment BV", "", dEquipmentBV);
        bvReport.addResultLine("", dbv);

        // adjust for target movement modifier
        double tmmRan = Compute.getTargetMovementModifier(protoMek.getRunMP(false, true, true), false, false, protoMek.getGame()).getValue();
        // Gliders get +1 for being airborne.
        if (protoMek.isGlider()) {
            tmmRan++;
        }

        final int jumpMP = protoMek.getJumpMP(false);
        final int tmmJumped = (jumpMP > 0) ?
                Compute.getTargetMovementModifier(jumpMP, true, false, protoMek.getGame()).getValue()
                : 0;

        final int umuMP = protoMek.getActiveUMUCount();
        final int tmmUMU = (umuMP > 0) ?
                Compute.getTargetMovementModifier(umuMP, false, false, protoMek.getGame()).getValue()
                : 0;

        double tmmFactor = 1 + (Math.max(tmmRan, Math.max(tmmJumped, tmmUMU)) / 10.0) + 0.1;
        // Round to 4 decimal places, just to cut off some numeric error
        tmmFactor = Math.round(tmmFactor * 1000) / 1000.0;
        dbv *= tmmFactor;

        bvReport.addLine("Target Movement Modifer For Run", "", tmmRan);
        bvReport.addLine("Target Movement Modifer For Jumping", "", tmmJumped);
        bvReport.addLine("Target Movement Modifer For UMUs", "", tmmUMU);
        bvReport.addLine("Multiply by Defensive Movement Factor of ", "x ", tmmFactor);
        bvReport.addResultLine("Defensive Battle Value", "= ", dbv);
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Offensive Battle Rating Calculation:");

        double weaponBV = 0;

        // figure out base weapon bv
        boolean hasTargComp = protoMek.hasTargComp();
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();
        for (Mounted mounted : protoMek.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(protoMek);

            String name = mounted.getName();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    name = name.concat(" with Artemis IV");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    dBV *= 1.2;
                    name = name.concat(" with Artemis IV Prototype");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    name = name.concat(" with Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    name = name.concat(" with Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                }
            }

            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
                name = name.concat(" with Targeting Computer");
            }
            weaponBV += dBV;
            bvReport.addLine(name, "", dBV);

            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!(wtype.hasFlag(WeaponType.F_ENERGY)
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(protoMek));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(protoMek)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        bvReport.addResultLine("Total Weapons BV", "", weaponBV);

        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on our team
        Map<String, Double> ammo = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        for (Mounted mounted : protoMek.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }

            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getProtoBV(mounted.getUsableShotsLeft()));
            } else {
                ammo.put(key, atype.getProtoBV(mounted.getUsableShotsLeft()) + ammo.get(key));
            }
        }
        // excessive ammo rule:
        // only count BV for ammo for a weapontype until the BV of all weapons of that
        // type on the mech is reached
        for (String key : keys) {
            if (weaponsForExcessiveAmmo.containsKey(key)
                    && (ammo.get(key) > weaponsForExcessiveAmmo.get(key))) {
                ammoBV += weaponsForExcessiveAmmo.get(key);
            } else {
                ammoBV += ammo.get(key);
            }
        }
        weaponBV += ammoBV;
        bvReport.addLine("Total Ammo BV", String.format("%.1f", ammoBV));

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM - BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : protoMek.getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_ECM)
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)) {
                continue;
            }
            oEquipmentBV += mtype.getBV(protoMek);
            bvReport.addLine(mounted.getName(), "", mtype.getBV(protoMek));
        }

        weaponBV += oEquipmentBV;
        bvReport.addResultLine("Total Equipment BV", "", oEquipmentBV);

        // adjust further for speed factor
        int mp = protoMek.getRunMPwithoutMyomerBooster(false, true, true)
                + (int) Math.round(Math.max(jumpMP, umuMP) / 2.0);
        // Unlike MASC and superchargers, which use walk x 2 for speed factor, myomer booster adds
        // one to run + 1/2 jump MP
        if (protoMek.hasMyomerBooster()) {
            mp++;
        }
        double speedFactor = Math.round(Math.pow(1 + ((mp - 5) / 10.0), 1.2) * 100.0) / 100.0;

        obv = weaponBV * speedFactor;
        bvReport.addResultLine("", weaponBV);
        bvReport.addLine("Multiply by Speed Factor of ", "" + speedFactor, "x ", speedFactor);
        bvReport.addResultLine("Offensive Battle Value", "= ", obv);
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Extra Battle Rating Calculation:");

        bvReport.addEmptyLine();
        bvReport.addSubHeader("Final BV Calculation:");
        bvReport.addLine("Defensive BV", "", dbv);
        bvReport.addLine("Offensive BV", "", obv);

        double finalBV = dbv + obv;
        bvReport.addResultLine("Sum", "= ", finalBV);

        // Force Bonuses
        double tagBonus = BVCalculator.bvTagBonus(protoMek);
        if (tagBonus > 0) {
            finalBV += tagBonus;
            bvReport.addEmptyLine();
            bvReport.addLine("Force Bonus (TAG):",
                    "+ " + formatForReport(tagBonus), "= " + formatForReport(finalBV));
        }

        double pilotFactor = ignoreSkill ? 1 : BVCalculator.bvMultiplier(protoMek);
        if (pilotFactor != 1) {
            finalBV *= pilotFactor;
            bvReport.addEmptyLine();
            bvReport.addLine("Pilot Modifier:",
                    "x " + formatForReport(pilotFactor), "= " + formatForReport(finalBV));
        }

        int finalAdjustedBV = (int) Math.round(finalBV);
        bvReport.addResultLine("Final BV", "= ", finalAdjustedBV);
        return finalAdjustedBV;
    }
}
