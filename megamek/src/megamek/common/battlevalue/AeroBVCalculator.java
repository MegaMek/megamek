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
import megamek.common.weapons.ppc.PPCWeapon;

import java.util.*;

public class AeroBVCalculator {

    public static int calculateBV(Aero aero, boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(aero.getChassis() + " " + aero.getModel());

        bvReport.addSubHeader("Defensive Battle Rating Calculation:");
        double dbv = 0; // defensive battle value
        boolean blueShield = aero.hasWorkingMisc(MiscType.F_BLUE_SHIELD);
        double armorMultiplier;

        // Ignore any hull/fuselage or capital fighter wing locations
        for (int loc = 0; loc < aero.locations() - ((aero instanceof SmallCraft) ? 1 : 2); loc++) {
            int modularArmor = 0;
            for (Mounted mounted : aero.getEquipment()) {
                if ((mounted.getType() instanceof MiscType)
                        && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == loc)) {
                    modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
                }
            }

            // total armor points
            switch (aero.getArmorType(loc)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    armorMultiplier = 0.5;
                    break;
                case EquipmentType.T_ARMOR_HARDENED:
                    armorMultiplier = 2.0;
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    armorMultiplier = 1.5;
                    break;
                case EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE:
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    armorMultiplier = 1.2;
                    break;
                default:
                    armorMultiplier = 1.0;
                    break;
            }
            if (aero.hasBARArmor(loc)) {
                armorMultiplier *= aero.getBARRating(loc) / 10.0;
            }

            if (blueShield) {
                armorMultiplier += 0.2;
            }
            int armor = aero.getArmor(loc) + modularArmor;
            double armorBV = (aero.getArmor(loc) + modularArmor) * armorMultiplier;
            dbv += armorBV;
            bvReport.addLine("Total Armor " + aero.getLocationAbbr(loc) + " ("
                    + armor + ") x " + armorMultiplier, "", armorBV);
        }
        dbv *= 2.5;
        bvReport.addLine("Total modified armor BV x 2.5", "= ", dbv);
        double dbvSI = aero.getSI() * 2.0 * (blueShield ? 1.2 : 1);
        dbv += dbvSI;
        bvReport.addLine("Total SI x 2 x SI modifier",
                aero.getSI() + " x 2 x " + (blueShield ? 1.2 : 1),
                "= ", dbvSI);

        // add defensive equipment
        double amsBV = 0;
        double amsAmmoBV = 0;
        double screenBV = 0;
        double screenAmmoBV = 0;
        double defEqBV = 0;
        for (Mounted mounted : aero.getEquipment()) {
            EquipmentType etype = mounted.getType();

            if (mounted.isDestroyed()) {
                continue;
            }
            if (mounted.isWeaponGroup()) {
                continue;
            }
            if ((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_AMS)) {
                amsBV += etype.getBV(aero);
                bvReport.addLine(etype.getName(), "+ " + etype.getBV(aero), "");
            } else if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) {
                amsAmmoBV += etype.getBV(aero);
                bvReport.addLine(etype.getName(), "+ " + etype.getBV(aero), "");
            } else if ((etype instanceof AmmoType)
                    && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)) {
                screenAmmoBV += etype.getBV(aero);
                bvReport.addLine(etype.getName(), "+ " + etype.getBV(aero), "");
            } else if ((etype instanceof WeaponType)
                    && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += etype.getBV(aero);
                bvReport.addLine(etype.getName(), "+ " + etype.getBV(aero), "");
            } else if ((etype instanceof MiscType) && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_BAP)
                    || etype.hasFlag(MiscType.F_CHAFF_POD))) {
                defEqBV += etype.getBV(aero);
                bvReport.addLine(mounted.getName(), "+ " + etype.getBV(aero), "");
            }
        }
        if (amsBV > 0) {
            dbv += amsBV;
            bvReport.addLine("Total AMS BV:", "+ ", amsBV);
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

        // subtract for explosive ammo
        double explosivePenalty = 0;
        // FIXME: Consider new AmmoType::equals / BombType::equals
        Map<AmmoType, Boolean> ammos = new HashMap<>();
        for (Mounted mounted : aero.getEquipment()) {
            int loc = mounted.getLocation();
            int toSubtract = 1;
            EquipmentType etype = mounted.getType();

            if (mounted.isWeaponGroup()) {
                continue;
            }

            // only count explosive ammo
            if (!etype.isExplosive(mounted, true)) {
                continue;
            }
            // PPCs with capacitors subtract 1
            if (etype instanceof PPCWeapon) {
                if (mounted.getLinkedBy() == null) {
                    continue;
                }
            }

            // PPC capacitor does not count separately, it's already counted for
            // with the PPC
            if ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_PPC_CAPACITOR)) {
                continue;
            }

            // don't count oneshot ammo
            if (loc == Entity.LOC_NONE) {
                continue;
            }

            // CASE means no subtraction
            if (aero.hasWorkingMisc(MiscType.F_CASE) || aero.isClan()) {
                continue;
            }

            // RACs, LACs and ACs don't really count
            if ((etype instanceof WeaponType) && ((((WeaponType) etype).getAmmoType() == AmmoType.T_AC_ROTARY)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC_IMP)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC_PRIMITIVE)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_PAC)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_LAC))) {
                toSubtract = 0;
            }

            // empty ammo shouldn't count
            if ((etype instanceof AmmoType) && (mounted.getUsableShotsLeft() == 0)) {
                continue;
            }
            if (etype instanceof AmmoType) {
                ammos.put((AmmoType) etype, true);
            } else {
                explosivePenalty += toSubtract;
            }
        }
        explosivePenalty += ammos.size() * 15;
        dbv = Math.max(1, dbv - explosivePenalty);
        bvReport.addLine("Explosive Weapons / Equipment Penalty ", "= -", explosivePenalty);
        bvReport.addResultLine("", dbv);

        String typeModText = "" + aero.getBVTypeModifier();
        if (aero.hasStealth()) {
            typeModText += "+ 0.3 for Stealth";
        }
        dbv *= (aero.getBVTypeModifier() + (aero.hasStealth() ? 0.3 : 0));
        bvReport.addLine("Multiply by Unit Type Modifier", typeModText,
                "x ", aero.getBVTypeModifier() + (aero.hasStealth() ? 0.3 : 0));
        bvReport.addResultLine("", dbv);

        bvReport.addSubHeader("Offensive Battle Rating Calculation:");
        // calculate heat efficiency
        int aeroHeatEfficiency = 6 + aero.getHeatCapacity();
        bvReport.addLine("Base Heat Efficiency", "", aeroHeatEfficiency);
        bvReport.addLine("Unmodified Weapon BV:", "", "");

        double weaponBV = 0;
        boolean hasTargComp = aero.hasTargComp();
        double targetingSystemBVMod = 1.0;

        if (aero instanceof FixedWingSupport) {
            if (aero.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)) {
                targetingSystemBVMod = 0.9;
            } else if (!aero.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                targetingSystemBVMod = 0.8;
            }
        }

        // first, add up front-faced and rear-faced unmodified BV,
        // to know wether front- or rear faced BV should be halved
        double bvFront = 0, bvRear = 0;
        List<Mounted> weapons = aero.getTotalWeaponList();
        for (Mounted weapon : weapons) {
            WeaponType wtype = (WeaponType) weapon.getType();
            double dBV = wtype.getBV(aero);
            // don't count destroyed equipment
            if (weapon.isDestroyed()) {
                continue;
            }
            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }
            // do not count weapon groups
            if (weapon.isWeaponGroup()) {
                continue;
            }

            String name = wtype.getName();
            // PPC caps bump up the value
            if (weapon.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) weapon.getLinkedBy().getType()).getBV(aero, weapon);
                    name = name.concat(" with Capacitor");
                }
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : weapon.getBayWeapons()) {
                    Mounted mg = aero.getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(aero);
                    }
                }
                dBV = mgBV * 0.67;
            }
            String wName = name;
            if (weapon.isRearMounted() || (weapon.getLocation() == Aero.LOC_AFT)) {
                bvRear += dBV;
                wName += " (R)";
            } else {
                bvFront += dBV;
            }
            bvReport.addLine(wName, "", dBV);
        }
        boolean halveRear = true;
        if (bvFront <= bvRear) {
            halveRear = false;
            bvReport.addLine("halving front instead of rear weapon BVs", "", "");
        }
        bvReport.addLine("Weapon Heat:", "", "");

        // here we store the modified BV and heat of all heat-using weapons, to later be sorted by BV
        ArrayList<ArrayList<Object>> heatBVs = new ArrayList<>();
        // BVs of non-heat-using weapons
        ArrayList<ArrayList<Object>> nonHeatBVs = new ArrayList<>();
        // total up maximum heat generated
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();
        double maximumHeat = 0;
        for (Mounted mounted : aero.getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double weaponHeat = wtype.getHeat();

            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }

            // do not count weapon groups
            if (mounted.isWeaponGroup()) {
                continue;
            }

            // one shot weapons count 1/4
            if ((wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER) || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // laser insulator reduce heat by 1, to a minimum of 1
            if (wtype.hasFlag(WeaponType.F_LASER) && (mounted.getLinkedBy() != null)
                    && !mounted.getLinkedBy().isInoperable()
                    && mounted.getLinkedBy().getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
                weaponHeat -= 1;
                if (weaponHeat == 0) {
                    weaponHeat++;
                }
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK) ||
                    (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                weaponHeat *= 0.5;
            }
            String name = wtype.getName();

            // check to see if the weapon is a PPC and has a Capacitor attached to it
            if (wtype.hasFlag(WeaponType.F_PPC) && (mounted.getLinkedBy() != null)) {
                name = name.concat(" with Capacitor");
                weaponHeat += 5;
            }

            bvReport.addLine(name, "", "+ ", weaponHeat);

            double dBV = wtype.getBV(aero);
            if (aero.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                dBV *= 0.8;
            }

            String weaponName = mounted.getName() + (mounted.isRearMounted() ? "(R)" : "");

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }
            // do not count weapon groups
            if (mounted.isWeaponGroup()) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : mounted.getBayWeapons()) {
                    Mounted mg = aero.getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(aero);
                    }
                }
                dBV = mgBV * 0.67;
            }
            // artemis bumps up the value, PPC caps do, too
            if (mounted.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(aero, mounted);
                    name = name.concat(" with Capacitor");
                }
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    name = name.concat(" with Artemis IV");
                }
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    name = name.concat(" with Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    dBV *= 1.1;
                    name = name.concat(" with Artemis IV Prototype");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    name = name.concat(" with Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                    name = name.concat(" with RISC Laser Pulse Module");
                }
            }

            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
            } else if ((aero instanceof FixedWingSupport) && !wtype.hasFlag(WeaponType.F_INFANTRY)) {
                dBV *= targetingSystemBVMod;
            }

            // half for being rear mounted (or front mounted, when more rear-
            // than front-mounted un-modded BV
            if (((mounted.isRearMounted() || (mounted.getLocation() == Aero.LOC_AFT)) && halveRear)
                    || (!(mounted.isRearMounted() || (mounted.getLocation() == Aero.LOC_AFT)) && !halveRear)) {
                dBV /= 2;
            }

            // ArrayList that stores weapon values
            // stores a double first (BV), then an Integer (heat),
            // then a String (weapon name)
            // for 0 heat weapons, just stores BV and name
            ArrayList<Object> weaponValues = new ArrayList<>();
            weaponValues.add(dBV);
            if (weaponHeat > 0) {
                // store heat and BV, for sorting a few lines down;
                weaponValues.add(weaponHeat);
                weaponValues.add(weaponName);
                heatBVs.add(weaponValues);
            } else {
                weaponValues.add(weaponName);
                nonHeatBVs.add(weaponValues);
            }

            maximumHeat += weaponHeat;
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                    || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(aero));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(aero) + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        bvReport.addResultLine("Total Heat:", "= ", maximumHeat);
        bvReport.addLine("Weapons with no heat at full BV:", "", "");
        // count heat-free weapons always at full modified BV
        for (ArrayList<Object> nonHeatWeapon : nonHeatBVs) {
            weaponBV += (Double) nonHeatWeapon.get(0);
            bvReport.addLine((String) nonHeatWeapon.get(1), "", "", (Double) nonHeatWeapon.get(0));
        }

        bvReport.addLine("Heat Modified Weapons BV: ", "", "");
        if (maximumHeat > aeroHeatEfficiency) {
            bvReport.addLine("(Heat Exceeds Aero Heat Efficiency) ", "", "");
        }

        if (maximumHeat <= aeroHeatEfficiency) {
            // count all weapons equal, adjusting for rear-firing and excessive ammo
            for (ArrayList<Object> weaponValues : heatBVs) {
                weaponBV += (Double) weaponValues.get(0);
                bvReport.addLine((String) weaponValues.get(2), "", (Double) weaponValues.get(0));
            }
        } else {
            // this will count heat-generating weapons at full modified BV until
            // heatefficiency is reached or passed with one weapon

            // sort the heat-using weapons by modified BV
            Collections.sort(heatBVs, (obj1, obj2) -> {
                Double obj1BV = (Double) obj1.get(0); // BV
                Double obj2BV = (Double) obj2.get(0); // BV

                // first element in the the ArrayList is BV, second is heat
                // if same BV, lower heat first
                if (obj1BV.equals(obj2BV)) {
                    Double obj1Heat = (Double) obj1.get(1);
                    Double obj2Heat = (Double) obj2.get(1);

                    return Double.compare(obj1Heat, obj2Heat);
                }

                // higher BV first
                return Double.compare(obj2BV, obj1BV);
            });

            // count heat-generating weapons at full modified BV until
            // heatefficiency is reached or passed with one weapon
            double heatAdded = 0;
            for (ArrayList<Object> weaponValues : heatBVs) {
                double dBV = (Double) weaponValues.get(0);
                if (heatAdded >= aeroHeatEfficiency) {
                    dBV /= 2;
                }
                String heatText = "";
                if (heatAdded >= aeroHeatEfficiency) {
                    heatText = "Heat efficiency reached, half BV";
                }
                heatAdded += (Double) weaponValues.get(1);
                weaponBV += dBV;
                bvReport.addLine((String) weaponValues.get(2), heatText, "", dBV);
                bvReport.addLine("Heat count: " + heatAdded, "", "");
            }
        }
        bvReport.addResultLine("Total Weapons BV Adjusted For Heat:", "", weaponBV);
        bvReport.addLine("Misc Offensive Equipment:", "", "");

        // add offensive misc. equipment BV
        double oEquipmentBV = 0;
        for (Mounted mounted : aero.getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_TARGCOMP) || mtype.hasFlag(MiscType.F_ECM) || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_CHAFF_POD)) {
                continue;
            }
            double bv = mtype.getBV(aero);
            if (bv > 0) {
                bvReport.addLine(mounted.getName(), "", bv);
            }
            oEquipmentBV += bv;
        }
        bvReport.addLine("Total Misc Offensive Equipment BV: ", "", oEquipmentBV);
        weaponBV += oEquipmentBV;

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        for (Mounted mounted : aero.getAmmo()) {
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
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED) || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = aero.getOwner();

                if (tmpP != null) {
                    // Okay, actually check for friendly TAG.
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(aero);
                    } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (aero.getGame() != null)) {
                        for (Enumeration<Team> e = aero.getGame().getTeams(); e.hasMoreElements();) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(aero.getGame())) {
                                    tagBV += atype.getBV(aero);
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the penalty, that's it.
                                break;
                            }
                        }
                    }
                }
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getBV(aero));
            } else {
                ammo.put(key, atype.getBV(aero) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // of that type on the mech is reached.
        for (String key : keys) {
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    ammoBV += ammo.get(key);
                }
            }
        }
        weaponBV += ammoBV;
        bvReport.addLine("Total Ammo BV: ", "", ammoBV);

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) aero.getRunMP() - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        bvReport.addLine("Final Speed Factor: ", "", speedFactor);

        double obv; // offensive bv
        obv = weaponBV * speedFactor;
        bvReport.addLine("Weapons BV * Speed Factor ",
                weaponBV + " x " + speedFactor, "= ", obv);

        double finalBV;
        if (aero.useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
            bvReport.addLine("2 * sqrt(Offensive BV * Defensive BV",
                    "2 * sqrt(" + obv + " + " + dbv + ")", "= ", finalBV);
        } else {
            finalBV = dbv + obv;
            bvReport.addLine("Offensive BV + Defensive BV",
                    obv + " + " + dbv, "= ", finalBV);
        }
        double totalBV = finalBV;

        double cockpitMod = 1;
        if (aero.getCockpitType() == Aero.COCKPIT_SMALL) {
            cockpitMod = 0.95;
            finalBV *= cockpitMod;
        } else if (aero.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            finalBV *= 0.95;
        }
        finalBV = Math.round(finalBV);
        bvReport.addLine("Total BV * Cockpit Modifier",
                totalBV + " x " + cockpitMod, "= ", finalBV);
        bvReport.addResultLine("Final BV", "", finalBV);

        // We need to consider external stores. Per TechManual Pg314,
        // TM BV Errata Pg23, the external stores BV is added to the units base BV
        boolean hasBombs = false;
        double bombBV = 0;
        for (int bombType = 0; bombType < BombType.B_NUM; bombType++) {
            BombType bomb = BombType.createBombByType(bombType);
            bombBV += bomb.getBV(aero) * aero.getBombChoices()[bombType];
            if (aero.getBombChoices()[bombType] > 0) {
                hasBombs = true;
            }
        }
        finalBV += bombBV;
        if (hasBombs) {
            bvReport.addLine("External Stores BV", "", bombBV);
            bvReport.addResultLine("Final BV", "", finalBV);
        }

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3 times
        if (!ignoreC3 && (aero.getGame() != null)) {
            xbv += aero.getExtraC3BV((int) Math.round(finalBV));
        }
        finalBV += xbv;

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = aero.getCrew().getBVSkillMultiplier(aero.getGame());
        }

        return (int) Math.round(finalBV * pilotFactor);
    }
}
