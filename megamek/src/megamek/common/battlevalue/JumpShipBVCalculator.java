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

import megamek.common.*;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class JumpShipBVCalculator extends BVCalculator {

    public static int calculateBV(Jumpship jumpShip, boolean ignoreC3, boolean ignoreSkill, StringBuffer bvText) {
        bvText.delete(0, bvText.length());
        bvText.append("<HTML><BODY><CENTER><b>Battle Value Calculations For ");
        bvText.append(jumpShip.getChassis());
        bvText.append(" ");
        bvText.append(jumpShip.getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        bvText.append(startTable);
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Armor Factor x 25");
        bvText.append(endColumn);
        bvText.append(startColumn);

        dbv += jumpShip.getTotalArmor();

        bvText.append(dbv);
        bvText.append(" x 25 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");

        dbv *= 25.0;

        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total SI x 20");
        bvText.append(endColumn);
        bvText.append(startColumn);

        double dbvSI = jumpShip.getSI() * 20.0;
        dbv += dbvSI;

        bvText.append(jumpShip.getSI());
        bvText.append(" x 20");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(dbvSI);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add defensive equipment
        double amsBV = 0;
        double amsAmmoBV = 0;
        double screenBV = 0;
        double screenAmmoBV = 0;
        double defEqBV = 0;
        for (Mounted mounted : jumpShip.getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }
            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))) {
                amsBV += etype.getBV(jumpShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(jumpShip));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) {
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to shots left
                double ratio = mounted.getUsableShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                amsAmmoBV += ratio * etype.getBV(jumpShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(jumpShip));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof AmmoType)
                    && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)) {
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to shots left
                double ratio = mounted.getUsableShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                screenAmmoBV += ratio * etype.getBV(jumpShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(jumpShip));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof WeaponType)
                    && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += etype.getBV(jumpShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(jumpShip));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_BAP))) {
                defEqBV += etype.getBV(jumpShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(mounted.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(jumpShip));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        if (amsBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total AMS BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(amsBV);
            dbv += amsBV;
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (screenBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Screen BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(screenBV);
            dbv += screenBV;
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (amsAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total AMS Ammo BV (to a maximum of AMS BV):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(Math.min(amsBV, amsAmmoBV));
            dbv += Math.min(amsBV, amsAmmoBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (screenAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Screen Ammo BV (to a maximum of Screen BV):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(Math.min(screenBV, screenAmmoBV));
            dbv += Math.min(screenBV, screenAmmoBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (defEqBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total misc defensive equipment BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(defEqBV);
            dbv += defEqBV;
            bvText.append(endColumn);
            bvText.append(endRow);
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
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        // unit type multiplier
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Unit type Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(jumpShip.getBVTypeModifier());
        dbv *= jumpShip.getBVTypeModifier();
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("x" + jumpShip.getBVTypeModifier());
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

        // calculate heat efficiency
        int aeroHeatEfficiency = jumpShip.getHeatCapacity();

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(aeroHeatEfficiency);

        bvText.append(endColumn);
        bvText.append(endRow);

        // get arc BV and heat
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<>();
        TreeMap<Integer, Double> arcBVs = new TreeMap<>();
        TreeMap<Integer, Double> arcHeat = new TreeMap<>();

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Arc BV and Heat");
        bvText.append(endColumn);
        bvText.append(endRow);

        Map<Integer, String> arcNameLookup = new HashMap<>();
        // cycle through locations
        for (int loc = 0; loc < jumpShip.locations(); loc++) {
            int l = loc;
            boolean isRear = (loc >= jumpShip.locations());
            String rear = "";
            if (isRear) {
                l = l - 3;
                rear = " (R)";
            }
            jumpShip.getLocationName(l);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<i>" + jumpShip.getLocationName(l) + rear + "</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>BV</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>Heat</i>");
            bvText.append(endColumn);
            bvText.append(endRow);

            for (Mounted mounted : jumpShip.getTotalWeaponList()) {
                if (mounted.getLocation() != loc) {
                    continue;
                }
                WeaponType wtype = (WeaponType) mounted.getType();
                double weaponHeat = wtype.getHeat();
                int arc = jumpShip.getWeaponArc(jumpShip.getEquipmentNum(mounted));
                arcNameLookup.put(arc, jumpShip.getLocationName(loc));
                double dBV = wtype.getBV(jumpShip);
                // skip bays
                if (wtype instanceof BayWeapon) {
                    continue;
                }
                // don't count defensive weapons
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                // don't count screen launchers, they are defensive
                if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                    continue;
                }
                // only count non-damaged equipment
                if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                    continue;
                }

                // double heat for ultras
                if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                    weaponHeat *= 2;
                }
                // Six times heat for RAC
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    weaponHeat *= 6;
                }
                // add up BV of ammo-using weapons for each type of weapon,
                // to compare with ammo BV later for excessive ammo BV rule
                if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                        || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY)
                        || (wtype.getAmmoType() == AmmoType.T_NA))) {
                    String key = wtype.getAmmoType() + ":" + wtype.getRackSize() + ";" + arc;
                    if (!weaponsForExcessiveAmmo.containsKey(key)) {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(jumpShip));
                    } else {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(jumpShip) + weaponsForExcessiveAmmo.get(key));
                    }
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgaBV = 0;
                    for (Mounted possibleMG : jumpShip.getTotalWeaponList()) {
                        if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                                && (possibleMG.getLocation() == mounted.getLocation())) {
                            mgaBV += possibleMG.getType().getBV(jumpShip);
                        }
                    }
                    dBV = mgaBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (jumpShip.hasTargComp()) {
                        dBV *= 1.25;
                    }
                }
                // artemis bumps up the value
                if (mounted.getLinkedBy() != null) {
                    Mounted mLinker = mounted.getLinkedBy();
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                        dBV *= 1.1;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                        dBV *= 1.3;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                    if ((mLinker.getType() instanceof MiscType)
                            && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                        dBV *= 1.15;
                    }
                }

                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(wtype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + dBV);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + weaponHeat);
                bvText.append(endColumn);
                bvText.append(endRow);

                double currentArcBV = 0.0;
                double currentArcHeat = 0.0;
                if (null != arcBVs.get(arc)) {
                    currentArcBV = arcBVs.get(arc);
                }
                if (null != arcHeat.get(arc)) {
                    currentArcHeat = arcHeat.get(arc);
                }
                arcBVs.put(arc, currentArcBV + dBV);
                arcHeat.put(arc, currentArcHeat + weaponHeat);
            }
        }
        double weaponBV = 0.0;
        // lets traverse the hash and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArc = Integer.MIN_VALUE;
        int oppArc = Integer.MIN_VALUE;
        double adjArcMult = 1.0;
        double oppArcMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        for (int key : arcBVs.keySet()) {
            // Warships only look at nose, aft, and broadsides for primary arc. Jumpships and space stations
            // look at all six arcs.
            if (jumpShip.hasETypeFlag(Entity.ETYPE_WARSHIP)
                    && (key != Compute.ARC_NOSE)
                    && (key != Compute.ARC_LEFT_BROADSIDE)
                    && (key != Compute.ARC_RIGHT_BROADSIDE)
                    && (key != Compute.ARC_AFT)) {
                continue;
            }
            if (arcBVs.get(key) > highBV) {
                highArc = key;
                highBV = arcBVs.get(key);
            }
        }
        // now lets identify the adjacent and opposite arcs
        if (highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeat.getOrDefault(highArc, 0.0);
            // now get the BV and heat for the two adjacent arcs
            int adjArcCW = jumpShip.getAdjacentArcCW(highArc);
            int adjArcCCW = jumpShip.getAdjacentArcCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if ((adjArcCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCW))) {
                adjArcCWBV = arcBVs.get(adjArcCW);
                adjArcCWHeat = arcHeat.getOrDefault(adjArcCW, 0.0);
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if ((adjArcCCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCCW))) {
                adjArcCCWBV = arcBVs.get(adjArcCCW);
                adjArcCCWHeat = arcHeat.getOrDefault(adjArcCCW, 0.0);
            }
            if (adjArcCWBV > adjArcCCWBV) {
                adjArc = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
            } else {
                adjArc = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
            }
            oppArc = jumpShip.getOppositeArc(highArc);
            if ((heatUsed + arcHeat.getOrDefault(oppArc, 0.0)) > aeroHeatEfficiency) {
                oppArcMult = 0.25;
            }
        }
        // According to an email with Welshman, ammo should be now added into
        // each arc BV
        // for the final calculation of BV, including the excessive ammo rule
        Map<String, Double> ammo = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        for (Mounted mounted : jumpShip.getAmmo()) {
            int arc = jumpShip.getWeaponArc(jumpShip.getEquipmentNum(mounted));
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if (atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                continue;
            }
            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize() + ";" + arc;
            String key2 = atype.getName() + ";" + key;
            // MML needs special casing so they don't count double
            if (atype.getAmmoType() == AmmoType.T_MML) {
                key2 = "MML " + atype.getRackSize() + " Ammo;" + key;
            }
            // same for the different AR10 ammos
            if (atype.getAmmoType() == AmmoType.T_AR10) {
                key2 = "AR10 Ammo;" + key;
            }
            double ammoWeight = mounted.getTonnage();
            if (atype.isCapital()) {
                ammoWeight = mounted.getUsableShotsLeft() * atype.getAmmoRatio();
            }
            // new errata: round partial tons of ammo up to the full ton
            ammoWeight = Math.ceil(jumpShip.getWeight());
            if (atype.hasFlag(AmmoType.F_CAP_MISSILE)) {
                ammoWeight = mounted.getUsableShotsLeft();
            }
            if (!keys.contains(key2)) {
                keys.add(key2);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, ammoWeight * atype.getBV(jumpShip));
            } else {
                ammo.put(key, (ammoWeight * atype.getBV(jumpShip)) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // in that arc is reached
        for (String fullkey : keys) {
            double ammoBV = 0.0;
            String[] k = fullkey.split(";");
            String key = k[1] + ";" + k[2];
            int arc = Integer.parseInt(k[2]);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(k[0]);
            bvText.append(endColumn);
            bvText.append(startColumn);
            // get the arc
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    bvText.append("+" + weaponsForExcessiveAmmo.get(key) + "*");
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    bvText.append("+" + ammo.get(key));
                    ammoBV += ammo.get(key);
                }
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("");
            bvText.append(endColumn);
            bvText.append(endRow);
            double currentArcBV = 0.0;
            if (null != arcBVs.get(arc)) {
                currentArcBV = arcBVs.get(arc);
            }
            arcBVs.put(arc, currentArcBV + ammoBV);
        }

        // ok now lets go in and add the arcs
        if (highArc > Integer.MIN_VALUE) {
            // ok now add the BV from this arc and reset to zero
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Highest BV Arc (" + arcNameLookup.get(highArc) + ")" + arcBVs.get(highArc) + "*1.0");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+" + arcBVs.get(highArc));
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startColumn);
            double totalHeat = arcHeat.getOrDefault(highArc, 0.0);
            bvText.append("Total Heat: " + totalHeat);
            bvText.append(endColumn);
            bvText.append(endRow);
            weaponBV += arcBVs.get(highArc);
            arcBVs.put(highArc, 0.0);
            if ((adjArc > Integer.MIN_VALUE) && (null != arcBVs.get(adjArc))) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(
                        "Adjacent High BV Arc (" + arcNameLookup.get(adjArc) + ") " + arcBVs.get(adjArc) + "*" + adjArcMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (arcBVs.get(adjArc) * adjArcMult));
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeat.getOrDefault(adjArc, 0.0);
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += adjArcMult * arcBVs.get(adjArc);
                arcBVs.put(adjArc, 0.0);
            }
            if ((oppArc > Integer.MIN_VALUE) && (null != arcBVs.get(oppArc))) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(
                        "Adjacent Low BV Arc (" + arcNameLookup.get(oppArc) + ") " + arcBVs.get(oppArc) + "*" + oppArcMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (oppArc * arcBVs.get(oppArc)));
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeat.getOrDefault(oppArc, 0.0);
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += oppArcMult * arcBVs.get(oppArc);
                arcBVs.put(oppArc, 0.0);
            }
            // ok now we can cycle through the rest and add 25%
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Remaining Arcs");
            bvText.append(endColumn);
            bvText.append(endRow);
            for (int loc : arcBVs.keySet()) {
                if (arcBVs.get(loc) > 0) {
                    bvText.append(startRow);
                    bvText.append(startColumn);
                    bvText.append(arcNameLookup.get(loc) + " " + arcBVs.get(loc) + "*0.25");
                    bvText.append(endColumn);
                    bvText.append(startColumn);
                    bvText.append("+" + (0.25 * arcBVs.get(loc)));
                    bvText.append(endColumn);
                    bvText.append(endRow);
                    weaponBV += (0.25 * arcBVs.get(loc));
                }
            }
        }

        bvText.append("Total Weapons BV Adjusted For Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : jumpShip.getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_TARGCOMP)) {
                continue;
            }
            double bv = mtype.getBV(jumpShip);
            if (bv > 0) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mounted.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(bv);
                bvText.append(endColumn);
                bvText.append(endRow);

                oEquipmentBV += bv;
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Misc Offensive Equipment BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(oEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);
        weaponBV += oEquipmentBV;

        // adjust further for speed factor
        int runMp = 1;
        if (jumpShip.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            runMp = jumpShip.getRunMP();
        } else if (jumpShip.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            runMp = 0;
        }
        double speedFactor = Math.pow(1 + (((double) runMp - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Final Speed Factor: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        obv = weaponBV * speedFactor;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Weapons BV * Speed Factor ");
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(weaponBV);
        bvText.append(" * ");
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        double finalBV;
        if (jumpShip.useGeometricMeanBV()) {
            bvText.append("2 * sqrt(Offensive BV * Defensive BV");
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
            bvText.append("2 * sqrt(");
            bvText.append(obv);
            bvText.append(" + ");
            bvText.append(dbv);
            bvText.append(")");
        } else {
            bvText.append("Offensive BV + Defensive BV");
            finalBV = dbv + obv;
            bvText.append(obv);
            bvText.append(" + ");
            bvText.append(dbv);
        }

        bvText.append(endColumn);
        bvText.append(startColumn);

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
        bvText.append("Final BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (!ignoreC3 && (jumpShip.getGame() != null)) {
            xbv += jumpShip.getExtraC3BV((int) Math.round(finalBV));
        }

        finalBV = Math.round(finalBV + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = jumpShip.getCrew().getBVSkillMultiplier(jumpShip.getGame());
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        return retVal;
    }
}
