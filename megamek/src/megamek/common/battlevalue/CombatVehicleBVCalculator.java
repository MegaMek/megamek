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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class CombatVehicleBVCalculator extends BVCalculator {

    public static int calculateBV(Tank tank, boolean ignoreC3, boolean ignoreSkill, StringBuffer bvText) {
        if (tank.isCarcass() && !ignoreSkill) {
            return 0;
        }
        bvText.delete(0, bvText.length());
        bvText.append("<HTML><BODY><CENTER><b>Battle Value Calculations For ");
        bvText.append(tank.getChassis());
        bvText.append(" ");
        bvText.append(tank.getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        boolean blueShield = false;
        // a blueshield system means a +0.2 on the armor and internal modifier,
        // like for mechs
        if (tank.hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
            blueShield = true;
        }

        bvText.append(startTable);
        double armorMultiplier = 1.0;

        for (int loc = 1; loc < tank.locations(); loc++) {
            int modularArmor = 0;
            for (Mounted mounted : tank.getEquipment()) {
                if ((mounted.getType() instanceof MiscType)
                        && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == loc)) {
                    modularArmor += mounted.getBaseDamageCapacity()
                            - mounted.getDamageTaken();
                }
            }
            // total armor points

            switch (tank.getArmorType(loc)) {
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
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    armorMultiplier = 1.2;
                    break;
                default:
                    armorMultiplier = 1.0;
                    break;
            }

            if (tank.hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
                armorMultiplier += 0.2;
            }
            bvText.append(startRow);
            bvText.append(startColumn);

            int armor = tank.getArmor(loc) + modularArmor;
            bvText.append("Total Armor " + tank.getLocationAbbr(loc) + " ("
                    + armor + ") x ");
            bvText.append(armorMultiplier);
            bvText.append(" x ");
            bvText.append(tank.getBARRating(loc));
            bvText.append("/10");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            double armorBV = (tank.getArmor(loc) + modularArmor) * armorMultiplier * (tank.getBARRating(loc) / 10.0);
            bvText.append(armorBV);
            dbv += armorBV;
            bvText.append(endColumn);
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total modified armor BV x 2.5 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        dbv *= 2.5;
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total I.S. Points x 1.5 x Blue Shield Multipler");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tank.getTotalInternal());
        bvText.append(" x 1.5 x ");
        bvText.append((blueShield ? 1.2 : 1));
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(tank.getTotalInternal() * 1.5 * (blueShield ? 1.2 : 1));
        bvText.append(endColumn);
        bvText.append(endRow);
        // total internal structure
        dbv += tank.getTotalInternal() * 1.5 * (blueShield ? 1.2 : 1);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Defensive Equipment");
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        double amsAmmoBV = 0;
        for (Mounted mounted : tank.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                amsAmmoBV += atype.getBV(tank);
            }
        }
        double amsBV = 0;
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : tank.getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS) ||
                    etype.hasFlag(WeaponType.F_B_POD) || etype.hasFlag(WeaponType.F_M_POD)))) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(etype.getBV(tank));
                bvText.append(endColumn);
                bvText.append(endRow);
                dEquipmentBV += etype.getBV(tank);
                WeaponType wtype = (WeaponType) etype;
                if ((wtype.hasFlag(WeaponType.F_AMS)
                        && (wtype.getAmmoType() == AmmoType.T_AMS)) || (wtype.getAmmoType() == AmmoType.T_APDS)) {
                    amsBV += etype.getBV(tank);
                }
            } else if (((etype instanceof MiscType) && (etype
                    .hasFlag(MiscType.F_ECM)
                    || etype.hasFlag(MiscType.F_AP_POD)
                    || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_BULLDOZER)
                    || etype.hasFlag(MiscType.F_CHAFF_POD) || etype.hasFlag(MiscType.F_BAP)))
                    || etype.hasFlag(MiscType.F_MINESWEEPER)) {
                MiscType mtype = (MiscType) etype;
                double bv = mtype.getBV(tank, mounted.getLocation());
                bvText.append(startColumn);
                bvText.append(mounted.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(bv);
                dEquipmentBV += bv;
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        if (amsAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("AMS Ammo (to a maximum of AMS BV)");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+");
            bvText.append(Math.min(amsBV, amsAmmoBV));
            bvText.append(endColumn);
            bvText.append(endRow);
            dEquipmentBV += Math.min(amsBV, amsAmmoBV);
        }
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(dEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);

        dbv += dEquipmentBV;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        double typeModifier;
        switch (tank.getMovementMode()) {
            case TRACKED:
                typeModifier = 0.9;
                break;
            case WHEELED:
                typeModifier = 0.8;
                break;
            case HOVER:
            case VTOL:
            case WIGE:
                typeModifier = 0.7;
                break;
            case NAVAL:
            case RAIL:
                typeModifier = 0.6;
                break;
            default:
                typeModifier = 0.6;
        }

        if (!tank.isSupportVehicle()) {
            for (Mounted m : tank.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_FULLY_AMPHIBIOUS)) {
                    typeModifier += 0.2;
                } else if (m.getType().hasFlag(MiscType.F_LIMITED_AMPHIBIOUS)
                        || m.getType().hasFlag(MiscType.F_DUNE_BUGGY)
                        || m.getType().hasFlag(MiscType.F_FLOTATION_HULL)
                        || m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)
                        || m.getType().hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                    typeModifier += 0.1;
                }
            }
        }
        typeModifier = Math.round(typeModifier * 10.0) / 10.0;
        bvText.append(startColumn);
        bvText.append("x Body Type Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("x ");
        bvText.append(typeModifier);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);

        dbv *= typeModifier;

        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("x Target Movement modifier");
        bvText.append(endColumn);
        // adjust for target movement modifier
        double tmmRan = Compute.getTargetMovementModifier(
                tank.getRunMP(false, true, true), tank instanceof VTOL,
                tank instanceof VTOL, tank.getGame()).getValue();
        // for the future, when we implement jumping tanks
        double tmmJumped = (tank.getJumpMP() > 0) ? Compute.
                getTargetMovementModifier(tank.getJumpMP(), true, false, tank.getGame()).
                getValue() : 0;
        if (tank.hasStealth()) {
            tmmRan += 2;
            tmmJumped += 2;
        }
        if (tank.getMovementMode() == EntityMovementMode.WIGE) {
            tmmRan += 1;
            tmmJumped += 1;
        }
        double tmmFactor = 1 + (Math.max(tmmRan, tmmJumped) / 10);
        dbv *= tmmFactor;
        // Deal with floating point errors
        dbv = Math.round(dbv * 100000.0) / 100000.0;

        bvText.append(startColumn);
        bvText.append("x ");
        bvText.append(tmmFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
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

        double weaponBV = 0;

        // figure out base weapon bv
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = tank.hasTargComp();
        double targetingSystemBVMod = 1.0;

        if (tank.isSupportVehicle()) {
            if (tank.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                targetingSystemBVMod = 1.0;
            } else if (tank.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)) {
                targetingSystemBVMod = .9;
            } else {
                targetingSystemBVMod = .8;
            }
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapons");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();
        for (Mounted mounted : tank.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(tank);

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            if (wtype.hasFlag(WeaponType.F_B_POD)) {
                continue;
            }
            if (wtype.hasFlag(WeaponType.F_M_POD)) {
                continue;
            }
            String weaponName = wtype.getName();
            if (mounted.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(
                            tank, mounted);
                    weaponName = weaponName.concat(" with Capacitor");
                }
            }

            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : mounted.getBayWeapons()) {
                    Mounted mg = tank.getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(tank);
                    }
                }
                dBV = mgBV * 0.67;
            }

            bvText.append(weaponName);
            bvText.append(" ");
            bvText.append(dBV);

            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    bvText.append(" x 1.2 Artemis IV");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    dBV *= 1.1;
                    bvText.append(" x 1.1 Artemis IV Prototype");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    bvText.append(" x 1.3 Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    bvText.append(" x 1.15 Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                    bvText.append(" x 1.15 RISC Laser Pulse Module");
                }
            }
            if (tank.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                dBV *= 0.8;
                bvText.append(" x 0.8 Drone OS");
            }

            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
                bvText.append(" x 1.25 Direct Fire and TC");
            } else if (tank.isSupportVehicle() && !wtype.hasFlag(WeaponType.F_INFANTRY)) {
                dBV *= targetingSystemBVMod;
                bvText.append(" x ");
                bvText.append(targetingSystemBVMod);
                bvText.append(" Targeting System");
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            if (mounted.getLocation() == (tank instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR
                    : tank instanceof LargeSupportTank ? LargeSupportTank.LOC_REAR
                    : Tank.LOC_REAR)) {
                weaponsBVRear += dBV;
                bvText.append(" Rear");
            } else if (mounted.getLocation() == Tank.LOC_FRONT) {
                weaponsBVFront += dBV;
                bvText.append(" Front");
            } else {
                weaponBV += dBV;
                bvText.append(" Side/Turret");
            }
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !((wtype.getAmmoType() == AmmoType.T_PLASMA)
                    || (wtype.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_HEAVY_FLAMER) || (wtype
                    .getAmmoType() == AmmoType.T_CHEMICAL_LASER)))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype
                    .getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(tank));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(tank)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(dBV);
            bvText.append(endColumn);

            bvText.append(endRow);

            bvText.append(startRow);
            bvText.append(startColumn);
        }

        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);

        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }

        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);

        bvText.append("Ammo BV");
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        for (Mounted mounted : tank.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }

            bvText.append(atype.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);

            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = tank.getOwner();
                // Okay, actually check for friendly TAG.
                if (tmpP != null) {
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(tank);
                    } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (tank.getGame() != null)) {
                        for (Enumeration<Team> e = tank.getGame().getTeams(); e.hasMoreElements();) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(tank.getGame())) {
                                    tagBV += atype.getBV(tank);
                                    bvText.append("Tag: ");
                                    bvText.append(atype.getBV(tank));
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the
                                // penalty, that's it.
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
                ammo.put(key, atype.getBV(tank));
            } else {
                ammo.put(key, atype.getBV(tank) + ammo.get(key));
            }
            bvText.append("BV: ");
            bvText.append(atype.getBV(tank));
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
        }
        bvText.append(endColumn);
        bvText.append(endRow);
        // excessive ammo rule:
        // only count BV for ammo for a weapontype until the BV of all weapons
        // of that
        // type on the mech is reached
        for (String key : keys) {
            // They dont exist in either hash then dont bother adding nulls.
            if (!ammo.containsKey(key)
                    || !weaponsForExcessiveAmmo.containsKey(key)) {
                continue;
            }
            if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                ammoBV += weaponsForExcessiveAmmo.get(key);
            } else {
                ammoBV += ammo.get(key);
            }
        }
        ammoBV *= targetingSystemBVMod;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(ammoBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        weaponBV += ammoBV;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Offensive Equipment");
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        double oEquipmentBV = 0;
        for (Mounted mounted : tank.getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if ((mtype.hasFlag(MiscType.F_ECM) && !mtype
                    .hasFlag(MiscType.F_WATCHDOG))
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    || mtype.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_CHAFF_POD)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_BULLDOZER)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)
                    || mtype.hasFlag(MiscType.F_MINESWEEPER)) {
                continue;
            }
            double bv = mtype.getBV(tank, mounted.getLocation());
            // we need to special case watchdog, because it has both offensive
            // and defensive BV
            if (mtype.hasFlag(MiscType.F_WATCHDOG)) {
                bv = 7;
            }
            oEquipmentBV += bv;
            bvText.append(mounted.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(bv);
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
        }

        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(oEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        weaponBV += oEquipmentBV;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("+ weight / 2");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tank.getWeight());
        bvText.append(" / 2 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(tank.getWeight() / 2);
        bvText.append(endColumn);
        bvText.append(endRow);

        weaponBV += tank.getWeight() / 2;

        // adjust further for speed factor
        double runMP = tank.getRunMP(false, true, true);

        // Trains use cruise instead of flank MP for speed factor
        if (tank.getMovementMode().equals(EntityMovementMode.RAIL) || tank.getMovementMode().equals(EntityMovementMode.MAGLEV)) {
            runMP = tank.getWalkMP(false, true, true);
        }
        // trailers have original run MP of 0, but should count at 1 for speed
        // factor calculation
        if (tank.getOriginalRunMP() == 0) {
            runMP = 1;
        }
        double speedFactor = Math
                .pow(1 + (((runMP + (Math.round(tank.getJumpMP(false) / 2.0))) - 5)
                        / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("+ weapons bv * speed factor");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(" * ");
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        double finalBV;
        if (tank.useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
        } else {
            finalBV = dbv + obv;
        }
        double totalBV = finalBV;

        if (tank.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            finalBV *= 0.95;
            finalBV = Math.round(finalBV);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total BV * Drone Operating System Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(totalBV);
            bvText.append(" * ");
            bvText.append("0.95");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(" = ");
            bvText.append(finalBV);
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
        }

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

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        if (!ignoreC3) {
            xbv += tank.getExtraC3BV((int) Math.round(finalBV));
        }

        finalBV = Math.round(finalBV + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill && (null != tank.getCrew())) {
            pilotFactor = tank.getCrew().getBVSkillMultiplier(tank.getGame());
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Final BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(retVal);
        bvText.append(endColumn);
        bvText.append(endRow);

        return retVal;
    }
}
