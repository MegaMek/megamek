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
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DropShipBVCalculator extends BVCalculator {

    public static int calculateBV(Dropship dropShip, boolean ignoreC3, boolean ignoreSkill, StringBuffer bvText) {
        bvText.delete(0, bvText.length());
        bvText.append("<HTML><BODY><CENTER><b>Battle Value Calculations For ");
        bvText.append(dropShip.getChassis());
        bvText.append(" ");
        bvText.append(dropShip.getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        int modularArmor = 0;
        for (Mounted mounted : dropShip.getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        // no use for armor mods right now but in case we need one later
        double armorMod = 1.0;

        bvText.append(startTable);
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Armor Factor x 2.5 x");
        bvText.append(armorMod);
        bvText.append(endColumn);
        bvText.append(startColumn);

        dbv += (dropShip.getTotalArmor() + modularArmor);

        bvText.append(dbv);
        bvText.append(" x 2.5 x ");
        bvText.append(armorMod);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");

        dbv *= 2.5 * armorMod;

        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total SI x 2");
        bvText.append(endColumn);
        bvText.append(startColumn);

        double dbvSI = dropShip.getSI() * 2.0;
        dbv += dbvSI;

        bvText.append(dropShip.getSI());
        bvText.append(" x 2");
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
        for (Mounted mounted : dropShip.getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }
            if (etype instanceof BayWeapon) {
                continue;
            }
            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))) {
                amsBV += etype.getBV(dropShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(dropShip));
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
                amsAmmoBV += ratio * etype.getBV(dropShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(dropShip));
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
                screenAmmoBV += ratio * etype.getBV(dropShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(dropShip));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof WeaponType)
                    && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += etype.getBV(dropShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(dropShip));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_BAP))) {
                defEqBV += etype.getBV(dropShip);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(mounted.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(dropShip));
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
        bvText.append(dropShip.getBVTypeModifier());
        dbv *= dropShip.getBVTypeModifier();
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("x" + dropShip.getBVTypeModifier());
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
        int aeroHeatEfficiency = dropShip.getHeatCapacity();

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(aeroHeatEfficiency);

        bvText.append(endColumn);
        bvText.append(endRow);

        // get arc BV and heat
        // I am going to redo how I do this to cycle through locations, so I can
        // spit it out
        // to bvText. It will require a little trickery for rear LS/RS since
        // those technically are not
        // locations
        double[] arcBVs = new double[dropShip.locations() + 2];
        double[] arcHeats = new double[dropShip.locations() + 2];
        double[] ammoBVs = new double[dropShip.locations() + 2];

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Arc BV and Heat");
        bvText.append(endColumn);
        bvText.append(endRow);

        // cycle through locations
        for (int loc = 0; loc < (dropShip.locations() + 2); loc++) {
            int l = loc;
            boolean isRear = (loc >= dropShip.locations());
            String rear = "";
            if (isRear) {
                l = l - 3;
                rear = " (R)";
            }
            dropShip.getLocationName(l);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<i>" + dropShip.getLocationName(l) + rear + "</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>BV</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>Heat</i>");
            bvText.append(endColumn);
            bvText.append(endRow);
            double arcBV = 0.0;
            double arcHeat = 0.0;
            double arcAmmoBV = 0.0;
            TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<>();
            for (Mounted mounted : dropShip.getTotalWeaponList()) {
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                // only count non-damaged equipment
                if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                    continue;
                }
                WeaponType wtype = (WeaponType) mounted.getType();
                double weaponHeat = wtype.getHeat();
                double dBV = wtype.getBV(dropShip);
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
                // double heat for ultras
                if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                    weaponHeat *= 2;
                }
                // Six times heat for RAC
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    weaponHeat *= 6;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgBV = 0;
                    for (int eqNum : mounted.getBayWeapons()) {
                        Mounted mg = dropShip.getEquipment(eqNum);
                        if ((mg != null) && (!mg.isDestroyed())) {
                            mgBV += mg.getType().getBV(dropShip);
                        }
                    }
                    dBV = mgBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (dropShip.hasTargComp()) {
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
                // add up BV of ammo-using weapons for each type of weapon,
                // to compare with ammo BV later for excessive ammo BV rule
                if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                        || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY)
                        || (wtype.getAmmoType() == AmmoType.T_NA))) {
                    String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                    if (!weaponsForExcessiveAmmo.containsKey(key)) {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(dropShip));
                    } else {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(dropShip) + weaponsForExcessiveAmmo.get(key));
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
                arcBV += dBV;
                arcHeat += weaponHeat;
            }
            // now ammo
            Map<String, Double> ammo = new HashMap<>();
            ArrayList<String> keys = new ArrayList<>();
            for (Mounted mounted : dropShip.getAmmo()) {
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                AmmoType atype = (AmmoType) mounted.getType();
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to
                // shots left
                double ratio = mounted.getUsableShotsLeft() / atype.getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }

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

                // don't count oneshot ammo, it's considered part of the
                // launcher.
                if (mounted.getLocation() == Entity.LOC_NONE) {
                    // assumption: ammo without a location is for a oneshot
                    // weapon
                    continue;
                }
                double abv = ratio * atype.getBV(dropShip);
                String key = atype.getAmmoType() + ":" + atype.getRackSize();
                String key2 = atype.getName() + ";" + key;
                // MML needs special casing so they don't count double
                if (atype.getAmmoType() == AmmoType.T_MML) {
                    key2 = "MML " + atype.getRackSize() + " Ammo;" + key;
                }
                // same for the different AR10 ammos
                if (atype.getAmmoType() == AmmoType.T_AR10) {
                    key2 = "AR10 Ammo;" + key;
                }
                if (!keys.contains(key2)) {
                    keys.add(key2);
                }
                if (!ammo.containsKey(key)) {
                    ammo.put(key, abv);
                } else {
                    ammo.put(key, abv + ammo.get(key));
                }
            }
            // now cycle through ammo hash and deal with excessive ammo issues
            for (String fullkey : keys) {
                String[] k = fullkey.split(";");
                String key = k[1];
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(k[0]);
                bvText.append(endColumn);
                bvText.append(startColumn);
                if (weaponsForExcessiveAmmo.get(key) != null) {
                    if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                        bvText.append("+" + weaponsForExcessiveAmmo.get(key) + "*");
                        arcAmmoBV += weaponsForExcessiveAmmo.get(key);
                    } else {
                        bvText.append("+" + ammo.get(key));
                        arcAmmoBV += ammo.get(key);
                    }
                }
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("");
                bvText.append(endColumn);
                bvText.append(endRow);
            }
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + dropShip.getLocationName(l) + rear + " Weapon Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcHeat + "</b>");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + dropShip.getLocationName(l) + rear + " Ammo Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcAmmoBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + dropShip.getLocationName(l) + rear + " Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            double tempBV = arcBV + arcAmmoBV;
            bvText.append("<b>" + tempBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("");
            bvText.append(endColumn);
            bvText.append(endRow);
            arcBVs[loc] = arcBV;
            arcHeats[loc] = arcHeat;
            ammoBVs[loc] = arcAmmoBV;
        }

        double weaponBV = 0.0;
        // ok, now lets loop through the arcs and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArcH = Integer.MIN_VALUE;
        int adjArcL = Integer.MIN_VALUE;
        double adjArcHMult = 1.0;
        double adjArcLMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        for (int loc = 0; loc < arcBVs.length; loc++) {
            if (arcBVs[loc] > highBV) {
                highArc = loc;
                highBV = arcBVs[loc];
            }
        }
        // now lets identify the adjacent arcs
        if (highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeats[highArc];
            // now get the BV and heat for the two adjacent arcs
            int adjArcCW = dropShip.getAdjacentLocCW(highArc);
            int adjArcCCW = dropShip.getAdjacentLocCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if (adjArcCW > Integer.MIN_VALUE) {
                adjArcCWBV = arcBVs[adjArcCW];
                adjArcCWHeat = arcHeats[adjArcCW];
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if (adjArcCCW > Integer.MIN_VALUE) {
                adjArcCCWBV = arcBVs[adjArcCCW];
                adjArcCCWHeat = arcHeats[adjArcCCW];
            }
            if (adjArcCWBV > adjArcCCWBV) {
                adjArcH = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
                adjArcL = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCCWHeat;
            } else {
                adjArcH = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
                adjArcL = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCWHeat;
            }
        }

        // ok now add in ammo to arc bvs
        for (int i = 0; i < arcBVs.length; i++) {
            arcBVs[i] = arcBVs[i] + ammoBVs[i];
        }

        // ok now lets go in and add the arcs
        double totalHeat = 0.0;
        if (highArc > Integer.MIN_VALUE) {
            // ok now add the BV from this arc and reset to zero
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Highest BV Arc (" + getArcName(dropShip, highArc) + ")" + arcBVs[highArc] + "*1.0");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+" + arcBVs[highArc]);
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startColumn);
            totalHeat = arcHeats[highArc];
            bvText.append("Total Heat: " + totalHeat);
            bvText.append(endColumn);
            bvText.append(endRow);
            weaponBV += arcBVs[highArc];
            arcBVs[highArc] = 0.0;
            if (adjArcH > Integer.MIN_VALUE) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(
                        "Adjacent High BV Arc (" + getArcName(dropShip, adjArcH) + ") " + arcBVs[adjArcH] + "*" + adjArcHMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (arcBVs[adjArcH] * adjArcHMult));
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeats[adjArcH];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += adjArcHMult * arcBVs[adjArcH];
                arcBVs[adjArcH] = 0.0;
            }
            if (adjArcL > Integer.MIN_VALUE) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(
                        "Adjacent Low BV Arc (" + getArcName(dropShip, adjArcL) + ") " + arcBVs[adjArcL] + "*" + adjArcLMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (adjArcLMult * arcBVs[adjArcL]));
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeats[adjArcL];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += adjArcLMult * arcBVs[adjArcL];
                arcBVs[adjArcL] = 0.0;
            }
            // ok now we can cycle through the rest and add 25%
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Remaining Arcs");
            bvText.append(endColumn);
            bvText.append(endRow);
            for (int loc = 0; loc < arcBVs.length; loc++) {
                if (arcBVs[loc] <= 0) {
                    continue;
                }
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(getArcName(dropShip, loc) + " " + arcBVs[loc] + "*0.25");
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (0.25 * arcBVs[loc]));
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += (0.25 * arcBVs[loc]);
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

        // add offensive misc. equipment BV
        double oEquipmentBV = 0;
        for (Mounted mounted : dropShip.getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_TARGCOMP)) {
                continue;
            }
            double bv = mtype.getBV(dropShip);
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
            }
            oEquipmentBV += bv;
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

        // adjust

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) dropShip.getRunMP() - 5) / 10), 1.2);
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

        if (dropShip.useGeometricMeanBV()) {
            bvText.append("2 * sqrt(Offensive BV * Defensive BV");
        } else {
            bvText.append("Offensive BV + Defensive BV");
        }
        bvText.append(endColumn);
        bvText.append(startColumn);

        double finalBV;
        if (dropShip.useGeometricMeanBV()) {
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
            finalBV = dbv + obv;
            bvText.append(dbv);
            bvText.append(" + ");
            bvText.append(obv);
        }

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        finalBV = Math.round(finalBV);

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
        if (!ignoreC3 && (dropShip.getGame() != null)) {
            xbv += dropShip.getExtraC3BV((int) Math.round(finalBV));
        }
        finalBV += xbv;

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = dropShip.getCrew().getBVSkillMultiplier(dropShip.getGame());
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        return retVal;
    }

    private static String getArcName(Dropship dropShip, int loc) {
        if (loc < dropShip.locations()) {
            return dropShip.getLocationName(loc);
        }
        return dropShip.getLocationName(loc - 3) + " (R)";
    }
}
