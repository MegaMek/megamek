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
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DropShipBVCalculator {

    public static int calculateBV(Dropship dropShip, boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(dropShip.getChassis() + " " + dropShip.getModel());

        bvReport.addSubHeader("Defensive Battle Rating Calculation:");
        double dbv = 0; // defensive battle value
        int modularArmor = 0;
        for (Mounted mounted : dropShip.getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        dbv += dropShip.getTotalArmor() + modularArmor;
        dbv *= 2.5;
        bvReport.addLine("Total Armor Factor x 2.5 ",
                (dropShip.getTotalArmor() + modularArmor) + " x 2.5", "= ", dbv);
        double dbvSI = dropShip.getSI() * 2.0;
        dbv += dbvSI;
        bvReport.addLine("Total SI x 2", dropShip.getSI() + " x 2", "= ", dbvSI);

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
                bvReport.addLine(etype.getName(), "+ " + etype.getBV(dropShip), "");
            } else if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) {
                // we need to deal with cases where ammo is loaded in multi-ton increments
                // (on dropships and jumpships) - lets take the ratio of shots to shots left
                double ratio = mounted.getUsableShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                amsAmmoBV += ratio * etype.getBV(dropShip);
                bvReport.addLine(etype.getName(), "+ " + (ratio * etype.getBV(dropShip)), "");
            } else if ((etype instanceof AmmoType)
                    && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)) {
                // we need to deal with cases where ammo is loaded in multi-ton increments
                // (on dropships and jumpships) - lets take the ratio of shots to shots left
                double ratio = mounted.getUsableShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                screenAmmoBV += ratio * etype.getBV(dropShip);
                bvReport.addLine(etype.getName(), "+ " + (ratio * etype.getBV(dropShip)), "");
            } else if ((etype instanceof WeaponType)
                    && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += etype.getBV(dropShip);
                bvReport.addLine(etype.getName(), "+ " + etype.getBV(dropShip), "");
            } else if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_BAP))) {
                defEqBV += etype.getBV(dropShip);
                bvReport.addLine(mounted.getName(), "+ " + etype.getBV(dropShip), "");
            }
        }
        if (amsBV > 0) {
            dbv += amsBV;
            bvReport.addLine("Total AMS BV:", "", amsBV);
        }
        if (screenBV > 0) {
            dbv += screenBV;
            bvReport.addLine("Total Screen BV:", "", screenBV);
        }
        if (amsAmmoBV > 0) {
            dbv += Math.min(amsBV, amsAmmoBV);
            bvReport.addLine("Total AMS Ammo BV (to a maximum of AMS BV):", "", Math.min(amsBV, amsAmmoBV));
        }
        if (screenAmmoBV > 0) {
            dbv += Math.min(screenBV, screenAmmoBV);
            bvReport.addLine("Total Screen Ammo BV (to a maximum of Screen BV):", "", Math.min(screenBV, screenAmmoBV));
        }
        if (defEqBV > 0) {
            dbv += defEqBV;
            bvReport.addLine("Total misc defensive equipment BV:", "", defEqBV);
        }
        bvReport.addResultLine("", dbv);
        dbv *= dropShip.getBVTypeModifier();
        bvReport.addLine("Multiply by Unit type Modifier", "" + dropShip.getBVTypeModifier(),
                "x ", dropShip.getBVTypeModifier());
        bvReport.addResultLine("", dbv);

        bvReport.addSubHeader("Offensive Battle Rating Calculation:");
        // calculate heat efficiency
        int aeroHeatEfficiency = dropShip.getHeatCapacity();
        bvReport.addLine("Base Heat Efficiency ", "", aeroHeatEfficiency);

        // get arc BV and heat
        double[] arcBVs = new double[dropShip.locations() + 2];
        double[] arcHeats = new double[dropShip.locations() + 2];
        double[] ammoBVs = new double[dropShip.locations() + 2];
        bvReport.addLine("Arc BV and Heat", "", "");

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
            bvReport.addLine(dropShip.getLocationName(l) + rear, "BV", "Heat");
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
                bvReport.addLine(wtype.getName(), "+ " + dBV, "+ ", weaponHeat);
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
                // we need to deal with cases where ammo is loaded in multi-ton increments
                // (on dropships and jumpships) - lets take the ratio of shots to shots left
                double ratio = mounted.getUsableShotsLeft() / atype.getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since we don't make that adjustment elsewhere
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

                // don't count oneshot ammo, it's considered part of the launcher.
                if (mounted.getLocation() == Entity.LOC_NONE) {
                    // assumption: ammo without a location is for a oneshot weapon
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
                if (weaponsForExcessiveAmmo.get(key) != null) {
                    if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                        bvReport.addLine(k[0], "+ " + weaponsForExcessiveAmmo.get(key) + "*", "");
                        arcAmmoBV += weaponsForExcessiveAmmo.get(key);
                    } else {
                        bvReport.addLine(k[0], "+ " + ammo.get(key), "");
                        arcAmmoBV += ammo.get(key);
                    }
                }
            }
            bvReport.addLine(dropShip.getLocationName(l) + rear + " Weapon Totals", "" + arcBV, "", arcHeat);
            bvReport.addLine(dropShip.getLocationName(l) + rear + " Ammo Totals", "" + arcAmmoBV, "");
            bvReport.addLine(dropShip.getLocationName(l) + rear + " Totals", "" + (arcBV + arcAmmoBV), "");
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
        double totalHeat;
        if (highArc > Integer.MIN_VALUE) {
            // ok now add the BV from this arc and reset to zero
            totalHeat = arcHeats[highArc];
            bvReport.addLine("Highest BV Arc (" + getArcName(dropShip, highArc) + ")" + arcBVs[highArc] + "*1.0",
                    "+ " + arcBVs[highArc], "Total Heat: " + totalHeat);
            weaponBV += arcBVs[highArc];
            arcBVs[highArc] = 0.0;

            if (adjArcH > Integer.MIN_VALUE) {
                totalHeat += arcHeats[adjArcH];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvReport.addLine("Adjacent High BV Arc (" + getArcName(dropShip, adjArcH) + ") " + arcBVs[adjArcH] + "*" + adjArcHMult,
                        "+ " + (arcBVs[adjArcH] * adjArcHMult),
                        "Total Heat: " + totalHeat + over);
                weaponBV += adjArcHMult * arcBVs[adjArcH];
                arcBVs[adjArcH] = 0.0;
            }

            if (adjArcL > Integer.MIN_VALUE) {
                totalHeat += arcHeats[adjArcL];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvReport.addLine("Adjacent Low BV Arc (" + getArcName(dropShip, adjArcL) + ") " + arcBVs[adjArcL] + "*" + adjArcLMult,
                        "+ " + (adjArcLMult * arcBVs[adjArcL]),
                        "Total Heat: " + totalHeat + over);
                weaponBV += adjArcLMult * arcBVs[adjArcL];
                arcBVs[adjArcL] = 0.0;
            }
            // ok now we can cycle through the rest and add 25%
            bvReport.addLine("Remaining Arcs", "", "");
            for (int loc = 0; loc < arcBVs.length; loc++) {
                if (arcBVs[loc] <= 0) {
                    continue;
                }
                bvReport.addLine(getArcName(dropShip, loc) + " " + arcBVs[loc] + "*0.25",
                        "+" + (0.25 * arcBVs[loc]), "");
                weaponBV += (0.25 * arcBVs[loc]);
            }
        }
        bvReport.addLine("Total Weapons BV Adjusted For Heat:", "", weaponBV);
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
                bvReport.addLine(mounted.getName(), "", bv);
            }
            oEquipmentBV += bv;
        }
        bvReport.addLine("Total Misc Offensive Equipment BV: ", "", oEquipmentBV);
        weaponBV += oEquipmentBV;

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) dropShip.getRunMP() - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        bvReport.addLine("Final Speed Factor: ", "", speedFactor);
        double obv; // offensive bv
        obv = weaponBV * speedFactor;
        bvReport.addLine("Weapons BV * Speed Factor ",
                weaponBV + " x " + speedFactor, "= ", obv);

        double finalBV;
        if (dropShip.useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
            bvReport.addLine("2 * sqrt(Offensive BV * Defensive BV)",
                    "2 * sqrt(" + obv + " + " + dbv + ")", "= ", finalBV);
        } else {
            finalBV = dbv + obv;
            bvReport.addLine("Offensive BV + Defensive BV",
                    obv + " + " + dbv, "= ", finalBV);
        }

        finalBV = Math.round(finalBV);
        bvReport.addResultLine("Final BV", "", finalBV);

        // we get extra bv from some stuff
        double xbv = 0.0;

        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3 times
        if (!ignoreC3 && (dropShip.getGame() != null)) {
            xbv += dropShip.getExtraC3BV((int) Math.round(finalBV));
        }
        finalBV += xbv;

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = dropShip.getCrew().getBVSkillMultiplier(dropShip.getGame());
        }

        return (int) Math.round(finalBV * pilotFactor);
    }

    private static String getArcName(Dropship dropShip, int loc) {
        if (loc < dropShip.locations()) {
            return dropShip.getLocationName(loc);
        }
        return dropShip.getLocationName(loc - 3) + " (R)";
    }
}