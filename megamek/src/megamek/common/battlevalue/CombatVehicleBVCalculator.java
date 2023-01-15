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

public class CombatVehicleBVCalculator {

    public static int calculateBV(Tank combatVee, boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {
        if (combatVee.isCarcass() && !ignoreSkill) { // TODO : why is this only done for Tanks? Necessary at all?
            return 0;
        }
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(combatVee.getChassis() + " " + combatVee.getModel());

        bvReport.addSubHeader("Defensive Battle Rating Calculation:");
        double dbv = 0; // defensive battle value
        boolean blueShield = combatVee.hasWorkingMisc(MiscType.F_BLUE_SHIELD);
        double armorMultiplier;

        for (int loc = 1; loc < combatVee.locations(); loc++) {
            int modularArmor = 0;
            for (Mounted mounted : combatVee.getEquipment()) {
                if ((mounted.getType() instanceof MiscType)
                        && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == loc)) {
                    modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
                }
            }

            // total armor points
            switch (combatVee.getArmorType(loc)) {
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

            if (blueShield) {
                armorMultiplier += 0.2;
            }
            int armor = combatVee.getArmor(loc) + modularArmor;
            double armorBV = (combatVee.getArmor(loc) + modularArmor) * armorMultiplier * (combatVee.getBARRating(loc) / 10.0);
            dbv += armorBV;
            bvReport.addLine("Total Armor " + combatVee.getLocationAbbr(loc) + " ("
                    + armor + ") x " + armorMultiplier + " x " + combatVee.getBARRating(loc) + "/10", "", armorBV);
        }
        dbv *= 2.5;
        bvReport.addLine("Total modified armor BV x 2.5", "= ", dbv);
        bvReport.addLine("Total I.S. Points x 1.5 x Blue Shield Multipler",
                combatVee.getTotalInternal() + " x 1.5 x " + (blueShield ? 1.2 : 1),
                "= ", combatVee.getTotalInternal() * 1.5 * (blueShield ? 1.2 : 1));
        // total internal structure
        dbv += combatVee.getTotalInternal() * 1.5 * (blueShield ? 1.2 : 1);
        bvReport.addResultLine("Defensive Equipment", "", "");

        double amsAmmoBV = 0;
        for (Mounted mounted : combatVee.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                amsAmmoBV += atype.getBV(combatVee);
            }
        }
        double amsBV = 0;
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : combatVee.getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS) ||
                    etype.hasFlag(WeaponType.F_B_POD) || etype.hasFlag(WeaponType.F_M_POD)))) {
                bvReport.addLine(etype.getName(), "", etype.getBV(combatVee));
                dEquipmentBV += etype.getBV(combatVee);
                WeaponType wtype = (WeaponType) etype;
                if ((wtype.hasFlag(WeaponType.F_AMS)
                        && (wtype.getAmmoType() == AmmoType.T_AMS)) || (wtype.getAmmoType() == AmmoType.T_APDS)) {
                    amsBV += etype.getBV(combatVee);
                }
            } else if ((etype instanceof MiscType) &&
                    (etype.hasFlag(MiscType.F_ECM)
                            || etype.hasFlag(MiscType.F_AP_POD)
                            || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                            || etype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                            || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                            || etype.hasFlag(MiscType.F_BULLDOZER)
                            || etype.hasFlag(MiscType.F_CHAFF_POD)
                            || etype.hasFlag(MiscType.F_BAP)
                            || etype.hasFlag(MiscType.F_MINESWEEPER))) {
                MiscType mtype = (MiscType) etype;
                double bv = mtype.getBV(combatVee, mounted.getLocation());
                dEquipmentBV += bv;
                bvReport.addLine(mounted.getName(), "", bv);
            }
        }
        if (amsAmmoBV > 0) {
            bvReport.addLine("AMS Ammo (to a maximum of AMS BV)", "+ ", Math.min(amsBV, amsAmmoBV));
            dEquipmentBV += Math.min(amsBV, amsAmmoBV);
        }
        bvReport.addLine("", "= ", dEquipmentBV);
        dbv += dEquipmentBV;

        double typeModifier;
        switch (combatVee.getMovementMode()) {
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
            default:
                typeModifier = 0.6;
        }

        if (!combatVee.isSupportVehicle()) {
            for (Mounted m : combatVee.getMisc()) {
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
        bvReport.addLine("x Body Type Modifier", "x " + typeModifier, "");
        dbv *= typeModifier;

        // adjust for target movement modifier
        double tmmRan = Compute.getTargetMovementModifier(
                combatVee.getRunMP(false, true, true), combatVee instanceof VTOL,
                combatVee instanceof VTOL, combatVee.getGame()).getValue();
        // for the future, when we implement jumping tanks
        double tmmJumped = (combatVee.getJumpMP() > 0) ? Compute.
                getTargetMovementModifier(combatVee.getJumpMP(), true, false, combatVee.getGame()).
                getValue() : 0;
        if (combatVee.hasStealth()) {
            tmmRan += 2;
            tmmJumped += 2;
        }
        if (combatVee.getMovementMode() == EntityMovementMode.WIGE) {
            tmmRan += 1;
            tmmJumped += 1;
        }
        double tmmFactor = 1 + (Math.max(tmmRan, tmmJumped) / 10);
        dbv *= tmmFactor;
        // Deal with floating point errors
        dbv = Math.round(dbv * 100000.0) / 100000.0;
        bvReport.addLine("x Target Movement modifier", "x " + tmmFactor, "");
        bvReport.addResultLine("", dbv);

        // figure out base weapon bv
        double weaponBV = 0;
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = combatVee.hasTargComp();
        double targetingSystemBVMod = 1.0;

        if (combatVee.isSupportVehicle()) {
            if (combatVee.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)) {
                targetingSystemBVMod = 0.9;
            } else if (!combatVee.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                targetingSystemBVMod = 0.8;
            }
        }
        bvReport.addLine("Weapons", "", "");

        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();
        for (Mounted mounted : combatVee.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(combatVee);

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
                // check to see if the weapon is a PPC and has a Capacitor attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(
                            combatVee, mounted);
                    weaponName = weaponName.concat(" with Capacitor");
                }
            }

            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : mounted.getBayWeapons()) {
                    Mounted mg = combatVee.getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(combatVee);
                    }
                }
                dBV = mgBV * 0.67;
            }

            String calculationText = "" + dBV;
            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    calculationText += " x 1.2 Artemis IV";
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    dBV *= 1.1;
                    calculationText += " x 1.1 Artemis IV Prototype";
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    calculationText += " x 1.3 Artemis V";
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    calculationText += " x 1.15 Apollo";
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                    calculationText += " x 1.15 RISC Laser Pulse Module";
                }
            }
            if (combatVee.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                dBV *= 0.8;
                calculationText += " x 0.8 Drone OS";
            }

            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
                calculationText += " x 1.25 Direct Fire and TC";
            } else if (combatVee.isSupportVehicle() && !wtype.hasFlag(WeaponType.F_INFANTRY)) {
                dBV *= targetingSystemBVMod;
                calculationText += " x " + targetingSystemBVMod + " Targeting System";
            }
            if (mounted.getLocation() == (combatVee instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR
                    : combatVee instanceof LargeSupportTank ? LargeSupportTank.LOC_REAR
                    : Tank.LOC_REAR)) {
                weaponsBVRear += dBV;
                calculationText += " Rear";
            } else if (mounted.getLocation() == Tank.LOC_FRONT) {
                weaponsBVFront += dBV;
                calculationText += " Front";
            } else {
                weaponBV += dBV;
                calculationText += " Side/Turret";
            }
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !((wtype.getAmmoType() == AmmoType.T_PLASMA)
                    || (wtype.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_HEAVY_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_CHEMICAL_LASER)))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(combatVee));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(combatVee) + weaponsForExcessiveAmmo.get(key));
                }
            }
            bvReport.addLine(weaponName, calculationText, "", dBV);
        }

        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += weaponsBVRear * 0.5;
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += weaponsBVFront * 0.5;
        }

        bvReport.addLine("", "", "= ", weaponBV);
        bvReport.addLine("Ammo BV", "", "");

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on our team
        double tagBV = 0;
        String tagText = "";
        Map<String, Double> ammo = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        for (Mounted mounted : combatVee.getAmmo()) {
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

            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = combatVee.getOwner();
                // Okay, actually check for friendly TAG.
                if (tmpP != null) {
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(combatVee);
                    } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (combatVee.getGame() != null)) {
                        for (Team m : combatVee.getGame().getTeams()) {
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(combatVee.getGame())) {
                                    tagBV += atype.getBV(combatVee);
                                    tagText = "Tag: ";
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
                ammo.put(key, atype.getBV(combatVee));
            } else {
                ammo.put(key, atype.getBV(combatVee) + ammo.get(key));
            }
            bvReport.addLine(atype.getName(), tagText, "BV: ", atype.getBV(combatVee));
        }
        // excessive ammo rule:
        // only count BV for ammo for a weapontype until the BV of all weapons of that
        // type on the mech is reached
        for (String key : keys) {
            // They dont exist in either hash then dont bother adding nulls.
            if (!ammo.containsKey(key) || !weaponsForExcessiveAmmo.containsKey(key)) {
                continue;
            }
            if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                ammoBV += weaponsForExcessiveAmmo.get(key);
            } else {
                ammoBV += ammo.get(key);
            }
        }
        ammoBV *= targetingSystemBVMod;
        bvReport.addLine("", "", "= ", ammoBV);
        weaponBV += ammoBV;
        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM - BMR p152)
        bvReport.addLine("Offensive Equipment", "", "");

        double oEquipmentBV = 0;
        for (Mounted mounted : combatVee.getMisc()) {
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
            double bv = mtype.getBV(combatVee, mounted.getLocation());
            // we need to special case watchdog, because it has both offensive
            // and defensive BV
            if (mtype.hasFlag(MiscType.F_WATCHDOG)) {
                bv = 7;
            }
            oEquipmentBV += bv;
            bvReport.addLine(mounted.getName(), "" + bv, "");
        }

        bvReport.addLine("", "", "= ", oEquipmentBV);
        weaponBV += oEquipmentBV;
        bvReport.addResultLine("+ weight / 2",
                combatVee.getWeight() + " / 2 ",
                "= ", combatVee.getWeight() / 2);
        weaponBV += combatVee.getWeight() / 2;

        // adjust further for speed factor
        double runMP = combatVee.getRunMP(false, true, true);

        // Trains use cruise instead of flank MP for speed factor
        if (combatVee.getMovementMode().isTrain()) {
            runMP = combatVee.getWalkMP(false, true, true);
        }
        // trailers have original run MP of 0, but should count at 1 for speed factor calculation
        if (combatVee.getOriginalRunMP() == 0) {
            runMP = 1;
        }
        double speedFactor = Math.pow(1 + (((runMP + (Math.round(combatVee.getJumpMP(false) / 2.0))) - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        double obv; // offensive bv
        obv = weaponBV * speedFactor;
        bvReport.addLine("+ weapons bv * speed factor", weaponBV + " * " + speedFactor, "= ", obv);

        double finalBV;
        finalBV = dbv + obv;
        double totalBV = finalBV;

        if (combatVee.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            finalBV *= 0.95;
            finalBV = Math.round(finalBV);
            bvReport.addLine("Total BV * Drone Operating System Modifier", totalBV + " x 0.95", "= ", finalBV);
        }
        bvReport.addResultLine("Final BV", "", finalBV);

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        if (!ignoreC3) {
            xbv += combatVee.getExtraC3BV((int) Math.round(finalBV));
        }

        finalBV = Math.round(finalBV + xbv);

        double pilotFactor = ignoreSkill ? 1 : BvMultiplier.bvMultiplier(combatVee);
        int finalAdjustedBV = (int) Math.round(finalBV * pilotFactor);
        bvReport.addResultLine("Final Adjusted BV", "= ", finalAdjustedBV);
        return finalAdjustedBV;
    }
}
