/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * protoMek file is part of MegaMek.
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

import megamek.common.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ProtoMekBVCalculator extends BVCalculator {

    public static int calculateBV(Protomech protoMek, boolean ignoreSkill, StringBuffer bvText) {
        bvText.delete(0, bvText.length());
        bvText.append("<HTML><BODY><CENTER><b>Battle Value Calculations For ");
        bvText.append(protoMek.getChassis());
        bvText.append(" ");
        bvText.append(protoMek.getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);
        bvText.append(startTable);

        double dbv = 0; // defensive battle value
        double obv; // offensive bv

        // total armor points
        dbv += protoMek.getTotalArmor() * 2.5;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Armor (" + protoMek.getTotalArmor() +") x 2.5");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(protoMek.getTotalArmor() * 2.5);
        bvText.append(endColumn);
        bvText.append(endRow);

        // total internal structure
        dbv += protoMek.getTotalInternal() * 1.5;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total I.S. Points (" + protoMek.getTotalInternal() +") x 1.5");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(protoMek.getTotalInternal() * 1.5);
        bvText.append(endColumn);
        bvText.append(endRow);

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

            if (((etype instanceof WeaponType) && etype
                    .hasFlag(WeaponType.F_AMS))
                    || ((etype instanceof MiscType) && (etype
                    .hasFlag(MiscType.F_ECM)
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

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Equipment BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);

        // adjust for target movement modifier
        double tmmRan = Compute.getTargetMovementModifier(protoMek.getRunMP(false, true, true), false, false, protoMek.getGame()).getValue();
        // Gliders get +1 for being airborne.
        if (protoMek.isGlider()) {
            tmmRan++;
        }

        final int jumpMP = protoMek.getJumpMP(false);
        final int tmmJumped = (jumpMP > 0) ? Compute.
                getTargetMovementModifier(jumpMP, true, false, protoMek.getGame()).getValue()
                : 0;

        final int umuMP = protoMek.getActiveUMUCount();
        final int tmmUMU = (umuMP > 0) ? Compute.
                getTargetMovementModifier(umuMP, false, false, protoMek.getGame()).getValue()
                : 0;

        double tmmFactor = 1 + (Math.max(tmmRan, Math.max(tmmJumped, tmmUMU))
                / 10.0) + 0.1;
        // Round to 4 decimal places, just to cut off some numeric error
        tmmFactor = Math.round(tmmFactor * 1000) / 1000.0;
        dbv *= tmmFactor;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Target Movement Modifer For Run");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tmmRan);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Target Movement Modifer For Jumping");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tmmJumped);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Target Movement Modifer For UMUs");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tmmUMU);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Defensive Movement Factor of ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tmmFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" x ");
        bvText.append(tmmFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Defensive Battle Value");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("<b>Offensive Battle Rating Calculation:</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

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

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(name);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(dBV);
            bvText.append(endColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endRow);

            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!(wtype.hasFlag(WeaponType.F_ENERGY)
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype
                    .getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(protoMek));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(protoMek)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Weapons BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
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
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = protoMek.getOwner();
                // Okay, actually check for friendly TAG.
                if (tmpP.hasTAG()) {
                    tagBV += atype.getBV(protoMek);
                } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (protoMek.getGame() != null)) {
                    for (Enumeration<Team> e = protoMek.getGame().getTeams(); e.hasMoreElements();) {
                        Team m = e.nextElement();
                        if (m.getId() == tmpP.getTeam()) {
                            if (m.hasTAG(protoMek.getGame())) {
                                tagBV += atype.getBV(protoMek);
                            }
                            // A player can't be on two teams.
                            // If we check his team and don't give the penalty,
                            // that's it.
                            break;
                        }
                    }
                }
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getProtoBV(mounted.getUsableShotsLeft()));
            } else {
                ammo.put(key, atype.getProtoBV(mounted.getUsableShotsLeft())
                        + ammo.get(key));
            }
        }
        // excessive ammo rule:
        // only count BV for ammo for a weapontype until the BV of all weapons
        // of that
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

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Ammo BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(String.format("%.1f", ammoBV));
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        double oEquipmentBV = 0;
        boolean hasMiscEq = false;
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
                // weapons
                continue;
            }
            oEquipmentBV += mtype.getBV(protoMek);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(mounted.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(mtype.getBV(protoMek));
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(endRow);
            hasMiscEq = true;
        }

        weaponBV += oEquipmentBV;

        if (hasMiscEq) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("-------------");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Equipment BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(oEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

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

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Speed Factor of ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" x ");
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append("Offensive Battle Value");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("<b>Extra Battle Rating Calculation:</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Tag BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tagBV);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append("Extra Battle Value");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(xbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("<b>Final BV Calculation:</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Deffensive BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Offensive BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Extra BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(xbv);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        int finalBV;
        if (protoMek.useGeometricMeanBV()) {
            finalBV = (int) Math.round((2 * Math.sqrt(obv * dbv)) + xbv);
            if (finalBV == 0) {
                finalBV = (int) Math.round(dbv + obv);
            }

            bvText.append("Geometric Mean (2Sqrt(O*D) + X");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("= ");
            bvText.append(finalBV);
        } else {
            finalBV = (int) Math.round(dbv + obv + xbv);
            bvText.append("Sum");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("= ");
            bvText.append(finalBV);
        }

        bvText.append(endColumn);
        bvText.append(endRow);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = protoMek.getCrew().getBVSkillMultiplier(protoMek.getGame());
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Pilot Factor of ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(pilotFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" x ");
        bvText.append(pilotFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        bvText.append("<b>Final Battle Value</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(retVal);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        return retVal;
    }
}
