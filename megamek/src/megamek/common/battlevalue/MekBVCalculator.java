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
import megamek.common.weapons.autocannons.HVACWeapon;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserLarge;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserMedium;
import megamek.common.weapons.lasers.CLImprovedHeavyLaserSmall;
import megamek.common.weapons.lasers.ISRISCHyperLaser;
import megamek.common.weapons.other.ISMekTaser;
import megamek.common.weapons.other.TSEMPWeapon;
import megamek.common.weapons.ppc.PPCWeapon;
import megamek.common.weapons.prototypes.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class MekBVCalculator extends BVCalculator {

    public static int calculateBV(Mech mek, boolean ignoreC3, boolean ignoreSkill, StringBuffer bvText) {
        bvText.delete(0, bvText.length());
        bvText.append("<HTML><BODY><CENTER><b>Battle Value Calculations For ");
        bvText.append(mek.getChassis());
        bvText.append(" ");
        bvText.append(mek.getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv; // offensive bv

        double armorMultiplier = 1.0;
        bvText.append(startTable);
        for (int loc = 0; loc < mek.locations(); loc++) {
            // total armor points
            switch (mek.getArmorType(loc)) {
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
                case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                    armorMultiplier = 1.1;
                    break;
                default:
                    armorMultiplier = 1.0;
                    break;
            }

            if (mek.hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
                armorMultiplier += 0.2;
            }
            if (mek.countWorkingMisc(MiscType.F_HARJEL_II, loc) > 0) {
                armorMultiplier *= 1.1;
            }
            if (mek.countWorkingMisc(MiscType.F_HARJEL_III, loc) > 0) {
                armorMultiplier *= 1.2;
            }

            // BV for torso mounted cockpit.
            if ((mek.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)
                    && (loc == Mech.LOC_CT)) {
                bvText.append(startRow);
                bvText.append(startColumn);
                double cockpitArmor = mek.getArmor(Mech.LOC_CT)
                        + mek.getArmor(Mech.LOC_CT, true);
                cockpitArmor *= armorMultiplier;
                bvText.append("extra BV for torso mounted cockpit");
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(cockpitArmor);
                bvText.append(endColumn);
                bvText.append(endRow);
                dbv += cockpitArmor;
            }
            int modularArmor = 0;
            for (Mounted mounted : mek.getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == loc)) {
                    modularArmor += mounted.getBaseDamageCapacity()
                            - mounted.getDamageTaken();
                }
            }
            int armor = mek.getArmor(loc)
                    + (mek.hasRearArmor(loc) ? mek.getArmor(loc, true) : 0);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Armor "
                    + mek.getLocationAbbr(loc)
                    + " ("
                    + armor
                    + (modularArmor > 0 ? " +" + modularArmor + " modular" : "")
                    + ") x ");
            bvText.append(armorMultiplier);
            bvText.append(endColumn);
            bvText.append(startColumn);
            double armorBV = (armor + modularArmor) * armorMultiplier;
            dbv += armorBV;
            bvText.append(armorBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total modified armor BV x 2.5 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        dbv *= 2.5;
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        // total internal structure
        double internalMultiplier = 1.0;
        if ((mek.getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL)
                || (mek.getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE)) {
            internalMultiplier = 0.5;
        } else if (mek.getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED) {
            internalMultiplier = 2.0;
        }
        if (mek.hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
            internalMultiplier += 0.2;
        }

        dbv += mek.getTotalInternal() * internalMultiplier * 1.5
                * (mek.hasEngine() ? mek.getEngine().getBVMultiplier() : 1.0);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total I.S. Points x IS Multipler x 1.5 x Engine Multipler");

        bvText.append(endColumn + startColumn);
        bvText.append(mek.getTotalInternal());
        bvText.append(" x ");
        bvText.append(internalMultiplier);
        bvText.append(" x ");
        bvText.append("1.5 x ");
        bvText.append(mek.hasEngine() ? mek.getEngine().getBVMultiplier() : 1.0);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(mek.getTotalInternal() * internalMultiplier * 1.5
                * (mek.hasEngine() ? mek.getEngine().getBVMultiplier() : 1.0));
        bvText.append(endColumn);
        bvText.append(endRow);

        // add gyro
        dbv += mek.getWeight() * mek.getGyroMultiplier();

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Weight x Gyro Multipler ");
        bvText.append(endColumn + startColumn);
        bvText.append(mek.getWeight());
        bvText.append(" x ");
        bvText.append(mek.getGyroMultiplier());
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(mek.getWeight() * mek.getGyroMultiplier());
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Defensive Equipment:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(endColumn);
        bvText.append(endRow);
        double amsAmmoBV = 0;
        for (Mounted mounted : mek.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                amsAmmoBV += atype.getBV(mek);
            }
        }
        double amsBV = 0;
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : mek.getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)
                    || etype.hasFlag(WeaponType.F_M_POD) || etype.hasFlag(WeaponType.F_B_POD)))
                    || ((etype instanceof MiscType) && (etype.hasFlag(MiscType.F_ECM)
                    || etype.hasFlag(MiscType.F_BAP)
                    || etype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                    || etype.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                    || etype.hasFlag(MiscType.F_AP_POD)
                    || etype.hasFlag(MiscType.F_MASS)
                    || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_CHAFF_POD)
                    || etype.hasFlag(MiscType.F_HARJEL_II)
                    || etype.hasFlag(MiscType.F_HARJEL_III)
                    || etype.hasFlag(MiscType.F_SPIKES) || (etype.hasFlag(MiscType.F_CLUB) && (etype.hasSubType(MiscType.S_SHIELD_LARGE)
                    || etype.hasSubType(MiscType.S_SHIELD_MEDIUM) || etype.hasSubType(MiscType.S_SHIELD_SMALL)))))) {
                double bv = etype.getBV(mek);
                if (etype instanceof WeaponType) {
                    WeaponType wtype = (WeaponType) etype;
                    if (wtype.hasFlag(WeaponType.F_AMS)
                            && ((wtype.getAmmoType() == AmmoType.T_AMS) || (wtype.getAmmoType() == AmmoType.T_APDS))) {
                        amsBV += bv;
                    }
                }
                dEquipmentBV += bv;
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mounted.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);

                bvText.append("+");
                bvText.append(bv);
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

        dbv += dEquipmentBV;

        bvText.append(startRow);
        bvText.append(startColumn);

        double armoredBVCal = mek.getArmoredComponentBV();

        if (armoredBVCal > 0) {
            bvText.append("Armored Components BV Modification");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("+");
            bvText.append(armoredBVCal);
            bvText.append(endColumn);
            bvText.append(endRow);
            dbv += armoredBVCal;
        }

        bvText.append("Total BV of all Defensive Equipment ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(dEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // subtract for explosive ammo
        double ammoPenalty = 0;
        for (Mounted mounted : mek.getEquipment()) {
            int loc = mounted.getLocation();
            int toSubtract = 15;
            EquipmentType etype = mounted.getType();

            // only count explosive ammo
            if (!etype.isExplosive(mounted, true)) {
                continue;
            }

            // don't count oneshot ammo
            if (loc == Mech.LOC_NONE) {
                continue;
            }

            if (!hasExplosiveEquipmentPenalty(mek, loc) && !hasExplosiveEquipmentPenalty(mek, mounted.getSecondLocation())) {
                continue;
            }

            // gauss rifles only subtract 1 point per slot, same for HVACs and
            // iHeavy Lasers and mektasers
            if ((etype instanceof GaussWeapon) || (etype instanceof HVACWeapon)
                    || (etype instanceof CLImprovedHeavyLaserLarge)
                    || (etype instanceof CLImprovedHeavyLaserMedium)
                    || (etype instanceof CLImprovedHeavyLaserSmall)
                    || (etype instanceof ISRISCHyperLaser)
                    || (etype instanceof TSEMPWeapon)
                    || (etype instanceof ISMekTaser)
                    || (etype.hasFlag(WeaponType.F_B_POD)
                    || (etype.hasFlag(WeaponType.F_M_POD)))) {
                toSubtract = 1;
            }

            // PPCs with capacitors subtract 1
            if (etype instanceof PPCWeapon) {
                if (mounted.getLinkedBy() != null) {
                    toSubtract = 1;
                } else {
                    continue;
                }
            }
            if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_PPC_CAPACITOR)
                    || etype.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)
                    || etype.hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)
                    || etype.hasFlag(MiscType.F_JUMP_JET))) {
                toSubtract = 1;
            }

            if (etype instanceof AmmoType
                    && ((AmmoType) mounted.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                toSubtract = 1;
            }


            if ((etype instanceof MiscType)
                    && etype.hasFlag(MiscType.F_BLUE_SHIELD)) {
                // blue shield needs to be special cased, because it's one
                // mounted with lots of locations,
                // and some of those could be protected by cas
                toSubtract = 0;
            }

            // RACs, LACs and ACs don't really count
            if ((etype instanceof WeaponType) && ((((WeaponType) etype).getAmmoType() == AmmoType.T_AC_ROTARY)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_LAC)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC_IMP)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC_PRIMITIVE)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_PAC))) {
                toSubtract = 0;
            }

            // empty ammo shouldn't count
            if ((etype instanceof AmmoType)
                    && (mounted.getUsableShotsLeft() == 0)) {
                continue;
            }

            // we subtract per critical slot
            int criticals;
            if (mounted.isSplit()) {
                criticals = 0;
                for (int l = 0; l < mek.locations(); l++) {
                    if (((l == mounted.getLocation()) || (l == mounted.getSecondLocation()))
                            && hasExplosiveEquipmentPenalty(mek, l)) {
                        for (int i = 0; i < mek.getNumberOfCriticals(l); i++) {
                            CriticalSlot slot = mek.getCritical(l, i);
                            if ((slot != null) && mounted.equals(slot.getMount())) {
                                criticals++;
                            }
                        }
                    }
                }
            } else if (mounted.getType() instanceof HVACWeapon) {
                // HVAC are only -1 total, regardless of number of crits. None are large enough to be splittable.
                criticals = 1;
            } else {
                criticals = mounted.getCriticals();
            }
            toSubtract *= criticals;
            ammoPenalty += toSubtract;
        }
        // special case for blueshield, need to check each non-head location
        // seperately for CASE
        if (mek.hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
            int unProtectedCrits = 0;
            for (int loc = Mech.LOC_CT; loc <= Mech.LOC_LLEG; loc++) {
                if (mek.hasCASEII(loc)) {
                    continue;
                }
                if (mek.isClan()) {
                    // Clan mechs only count ammo in ct, legs or head (per
                    // BMRr).
                    // Also count ammo in side torsos if mech has xxl engine
                    // (extrapolated from rule intent - not covered in rules)
                    if (((loc != Mech.LOC_CT) && (loc != Mech.LOC_RLEG)
                            && (loc != Mech.LOC_LLEG) && (loc != Mech.LOC_HEAD))
                            && !(((loc == Mech.LOC_RT) || (loc == Mech.LOC_LT)) && mek.hasEngine() && (mek.getEngine()
                            .getSideTorsoCriticalSlots().length > 2))) {
                        continue;
                    }
                } else {
                    // inner sphere with XL or XXL counts everywhere
                    if (mek.hasEngine() && (mek.getEngine().getSideTorsoCriticalSlots().length <= 2)) {
                        // without XL or XXL, only count torsos if not CASEed,
                        // and arms if arm & torso not CASEed
                        if (((loc == Mech.LOC_RT) || (loc == Mech.LOC_LT))
                                && mek.locationHasCase(loc)) {
                            continue;
                        } else if ((loc == Mech.LOC_LARM)
                                && (mek.locationHasCase(loc) || mek.locationHasCase(Mech.LOC_LT))) {
                            continue;
                        } else if ((loc == Mech.LOC_RARM)
                                && (mek.locationHasCase(loc) || mek.locationHasCase(Mech.LOC_RT))) {
                            continue;
                        }
                    }
                }
                unProtectedCrits++;
            }
            ammoPenalty += unProtectedCrits;
        }
        dbv = Math.max(1, dbv - ammoPenalty);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Explosive Weapons/Equipment Penalty ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= -");
        bvText.append(ammoPenalty);
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

        // adjust for target movement modifier
        // we use full possible movement, ignoring gravity, heat and modular
        // armor, but taking into account hit actuators
        int bvWalk = mek.getWalkMP(false, true, true);
        int airmechMP = 0;
        if (((mek.getEntityType() & Entity.ETYPE_LAND_AIR_MECH) != 0)) {
            bvWalk = ((LandAirMech) mek).getBVWalkMP();
            if (((LandAirMech) mek).getLAMType() == LandAirMech.LAM_STANDARD) {
                airmechMP = ((LandAirMech) mek).getAirMechFlankMP();
            }
        } else if (((mek.getEntityType() & Entity.ETYPE_QUADVEE) != 0)
                && (mek.getMovementMode() == EntityMovementMode.WHEELED)) {
            // Don't use bonus cruise MP in calculating BV
            bvWalk = Math.max(0, mek.getOriginalWalkMP());
        }
        int runMP;
        if (mek.hasTSM(false)) {
            bvWalk++;
        }
        Entity.MPBoosters mpBooster = mek.getMPBoosters();
        if (mpBooster.hasMASCAndSupercharger()) {
            runMP = (int) Math.ceil(bvWalk * 2.5);
        } else if (mpBooster.hasMASCXorSupercharger()) {
            runMP = bvWalk * 2;
        } else {
            runMP = (int) Math.ceil(bvWalk * 1.5);
        }
        if (mek.hasMPReducingHardenedArmor()) {
            runMP--;
        }
        int tmmRan = Compute.getTargetMovementModifier(runMP, false, false,
                mek.getGame()).getValue();
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Run MP");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(runMP);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Target Movement Modifier For Run");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmRan);
        bvText.append(endColumn);
        bvText.append(endRow);

        // Calculate modifiers for jump and UMU movement where applicable.
        final int jumpMP = Math.max(mek.getJumpMP(false, true), airmechMP);
        final int tmmJumped = (jumpMP > 0) ?
                Compute.getTargetMovementModifier(jumpMP, true, false, mek.getGame()).getValue()
                : 0;

        final int umuMP = mek.getActiveUMUCount();
        final int tmmUMU = (umuMP > 0) ?
                Compute.getTargetMovementModifier(umuMP, false, false, mek.getGame()).getValue()
                : 0;

        bvText.append(startRow);
        bvText.append(startColumn);

        if (airmechMP == 0) {
            bvText.append("Target Movement Modifier For Jumping");
        } else {
            bvText.append("Target Movement Modifier For AirMech Flank");
        }
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmJumped);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append("Target Movement Modifier For UMUs");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(tmmUMU);
        bvText.append(endColumn);
        bvText.append(endRow);

        double targetMovementModifier = Math.max(tmmRan, Math.max(tmmJumped,
                tmmUMU));

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Target Movement Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(targetMovementModifier);
        bvText.append(endColumn);
        bvText.append(endRow);

        // Try to find a Mek Stealth or similar system.
        if (mek.hasStealth() || mek.hasNullSig()) {
            targetMovementModifier += 2;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Stealth +2");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("+2");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if (mek.hasChameleonShield()) {
            targetMovementModifier += 2;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Chameleon +2");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("+2");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if (mek.hasVoidSig()) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Void Sig");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(endColumn);
            bvText.append(startColumn);

            if (targetMovementModifier < 3) {
                targetMovementModifier = 3;
                bvText.append("3");
            } else if (targetMovementModifier == 3) {
                targetMovementModifier++;
                bvText.append("+1");
            } else {
                bvText.append("-");
            }

            bvText.append(endColumn);
            bvText.append(endRow);
        }

        double tmmFactor = 1 + (targetMovementModifier / 10);
        dbv *= tmmFactor;

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
        bvText.append(startColumn);

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
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
        int mechHeatEfficiency = 6 + mek.getHeatCapacity();
        if ((mek instanceof LandAirMech) && (((LandAirMech) mek).getLAMType() == LandAirMech.LAM_STANDARD)) {
            mechHeatEfficiency += 3;
        }

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        double coolantPods = 0;
        for (Mounted ammo : mek.getAmmo()) {
            if (((AmmoType) ammo.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                coolantPods++;
            }
        }

        // account for coolant pods
        if (coolantPods > 0) {
            mechHeatEfficiency += (int) Math.ceil((mek.getNumberOfSinks() * coolantPods) / 5d);
            bvText.append(" + Coolant Pods ");
        }
        if (mek.hasWorkingMisc(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
            mechHeatEfficiency += 4;
            bvText.append(" + RISC Emergency Coolant System");
        }

        int moveHeat;
        if ((mek instanceof LandAirMech) && (((LandAirMech) mek).getLAMType() == LandAirMech.LAM_STANDARD)) {
            moveHeat = (int) Math.round(((LandAirMech) mek).getAirMechFlankMP(false, true) / 3d);
        } else if ((mek.getJumpMP(false, true) > 0)
                && (mek.getJumpHeat(mek.getJumpMP(false, true)) > mek.getRunHeat())) {
            moveHeat = mek.getJumpHeat(mek.getJumpMP(false, true));
            bvText.append(" - Jump Heat ");
        } else {
            moveHeat = mek.getRunHeat();
            if (mek.hasSCM()) {
                moveHeat = 0;
            }
            bvText.append(" - Run Heat ");
        }
        mechHeatEfficiency -= moveHeat;
        if (mek.hasStealth()) {
            mechHeatEfficiency -= 10;
            bvText.append(" - Stealth Heat ");
        }
        if (mek.hasChameleonShield()) {
            mechHeatEfficiency -= 6;
            bvText.append(" - Chameleon LPS Heat ");
        }
        if (mek.hasNullSig()) {
            mechHeatEfficiency -= 10;
            bvText.append(" - Null-signature system Heat ");
        }
        if (mek.hasVoidSig()) {
            mechHeatEfficiency -= 10;
            bvText.append(" - Void-signature system Heat ");
        }

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(6 + mek.getHeatCapacity());

        if (coolantPods > 0) {
            bvText.append(" + ");
            bvText.append(Math.ceil((mek.getNumberOfSinks() * coolantPods) / 5));
        }

        if (mek.hasWorkingMisc(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
            mechHeatEfficiency += 4;
            bvText.append(" + 4");
        }

        bvText.append(" - ").append(moveHeat);
        if (mek.hasStealth()) {
            bvText.append(" - 10");
        }

        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(mechHeatEfficiency);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Unmodified Weapon BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        double weaponBV = 0;
        boolean hasTargComp = mek.hasTargComp();
        // first, add up front-faced and rear-faced unmodified BV,
        // to know wether front- or rear faced BV should be halved
        double bvFront = 0, bvRear = 0, nonArmFront = 0, nonArmRear = 0, bvTurret = 0;
        ArrayList<Mounted> weapons = mek.getWeaponList();
        for (Mounted weapon : weapons) {
            WeaponType wtype = (WeaponType) weapon.getType();
            if (wtype.hasFlag(WeaponType.F_B_POD)
                    || wtype.hasFlag(WeaponType.F_M_POD)) {
                continue;
            }
            double dBV = wtype.getBV(mek);

            // don't count destroyed equipment
            if (weapon.isDestroyed()) {
                continue;
            }
            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : weapon.getBayWeapons()) {
                    Mounted mg = mek.getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(mek);
                    }
                }
                dBV = mgBV * 0.67;
            }
            String name = wtype.getName();
            // artemis bumps up the value
            // PPC caps do, too
            if (weapon.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) weapon.getLinkedBy().getType()).getBV(
                            mek, weapon);
                    name = name.concat(" with Capacitor");
                }
                Mounted mLinker = weapon.getLinkedBy();
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
                    name = name.concat(" with RISC Laser Pulse Module");
                }
            }

            if (mek.hasFunctionalArmAES(weapon.getLocation())) {
                dBV *= 1.25;
                name = name.concat(" augmented by AES");
            }

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(name);
            boolean rearVGL = false;
            if (weapon.getType().hasFlag(WeaponType.F_VGL)) {
                // vehicular grenade launchers facing to the rear sides count
                // for rear BV, too
                if ((weapon.getFacing() == 2) || (weapon.getFacing() == 4)) {
                    rearVGL = true;
                }
            }
            if (weapon.isMechTurretMounted()) {
                bvTurret += dBV;
                bvText.append(" (T)");
            } else if (weapon.isRearMounted() || rearVGL) {
                bvRear += dBV;
                bvText.append(" (R)");
            } else {
                bvFront += dBV;
            }
            if (!mek.isArm(weapon.getLocation()) && !weapon.isMechTurretMounted()) {
                if (weapon.isRearMounted() || rearVGL) {
                    nonArmRear += dBV;
                } else {
                    nonArmFront += dBV;
                }
            }

            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(dBV);
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

        bvText.append("Unmodified Front BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvFront);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Unmodfied Rear BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvRear);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Unmodfied Turret BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvTurret);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Unmodfied BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(bvRear + bvFront + bvTurret);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append("Unmodified Front non-arm BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(nonArmFront);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Unmodfied Rear non-arm BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(nonArmRear);
        bvText.append(endColumn);
        bvText.append(endRow);

        boolean halveRear = true;
        boolean turretFront = true;
        if (nonArmFront <= nonArmRear) {
            halveRear = false;
            turretFront = false;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("halving front instead of rear weapon BVs");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("turret mounted weapon BVs count as rear firing");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapon Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // here we store the modified BV and heat of all heat-using weapons,
        // to later be sorted by BV
        ArrayList<ArrayList<Object>> heatBVs = new ArrayList<>();
        // BVs of non-heat-using weapons
        ArrayList<ArrayList<Object>> nonHeatBVs = new ArrayList<>();
        // total up maximum heat generated
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();
        double maximumHeat = 0;
        for (Mounted mounted : mek.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if (wtype.hasFlag(WeaponType.F_B_POD)
                    || wtype.hasFlag(WeaponType.F_M_POD)) {
                continue;
            }
            double weaponHeat = wtype.getHeat();

            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed()
                    || mounted.isBreached()) {
                continue;
            }

            // one shot weapons count 1/4
            if ((wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER)
                    || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // 1d6 extra heat; add half for heat calculations (1d3/+2 for small pulse)
            if ((wtype instanceof ISERLaserLargePrototype)
                    || (wtype instanceof ISPulseLaserLargePrototype)
                    || (wtype instanceof ISPulseLaserMediumPrototype)
                    || (wtype instanceof ISPulseLaserMediumRecovered)) {
                weaponHeat += 3;
            } else if (wtype instanceof ISPulseLaserSmallPrototype) {
                weaponHeat += 2;
            }

            String name = wtype.getName();


            // RISC laser pulse module adds 2 heat
            if ((wtype.hasFlag(WeaponType.F_LASER)) && (mounted.getLinkedBy() != null)
                    && (mounted.getLinkedBy().getType() instanceof MiscType)
                    && (mounted.getLinkedBy().getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE))) {
                name = name.concat(" with RISC Laser Pulse Module");
                weaponHeat += 2;
            }

            // laser insulator reduce heat by 1, to a minimum of 1
            if (wtype.hasFlag(WeaponType.F_LASER)
                    && (mounted.getLinkedBy() != null)
                    && !mounted.getLinkedBy().isInoperable()
                    && (mounted.getLinkedBy().getType() instanceof MiscType)
                    && mounted.getLinkedBy().getType()
                    .hasFlag(MiscType.F_LASER_INSULATOR)) {
                weaponHeat -= 1;
                if (weaponHeat == 0) {
                    weaponHeat++;
                }
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_IATM)) {
                weaponHeat *= 0.5;
            }
            // check to see if the weapon is a PPC and has a Capacitor attached
            // to it
            if (wtype.hasFlag(WeaponType.F_PPC)
                    && (mounted.getLinkedBy() != null)) {
                name = name.concat(" with Capacitor");
                weaponHeat += 5;
            }

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(name);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+ ");
            bvText.append(weaponHeat);
            bvText.append(endColumn);
            bvText.append(endRow);

            double dBV = wtype.getBV(mek);
            if (mek.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                dBV *= 0.8;
            }
            String weaponName = mounted.getName()
                    + (mounted.isRearMounted() ? "(R)" : "");

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgBV = 0;
                for (int eqNum : mounted.getBayWeapons()) {
                    Mounted mg = mek.getEquipment(eqNum);
                    if ((mg != null) && (!mg.isDestroyed())) {
                        mgBV += mg.getType().getBV(mek);
                    }
                }
                dBV = mgBV * 0.67;
            }

            // artemis bumps up the value
            // PPC caps do, too
            if (mounted.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(
                            mek, mounted);
                    weaponName = weaponName.concat(" with Capacitor");
                }
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    weaponName = weaponName.concat(" with Artemis IV");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    weaponName = weaponName.concat(" with Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    weaponName = weaponName.concat(" with Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                    weaponName = weaponName.concat(" with RISC Laser Pulse Module");
                }
            }
            // if linked to AES, multiply by 1.25
            if (mek.hasFunctionalArmAES(mounted.getLocation())) {
                dBV *= 1.25;
            }
            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
            }
            // half for being rear mounted (or front mounted, when more rear-
            // than front-mounted un-modded BV
            // or for being turret mounted, when more rear-mounted BV than front
            // mounted BV
            if ((!mek.isArm(mounted.getLocation())
                    && !mounted.isMechTurretMounted() && (mounted.isRearMounted() ? halveRear : !halveRear))
                    || (mounted.isMechTurretMounted() && (turretFront ? !halveRear : halveRear))) {
                dBV /= 2;
            }

            // ArrayList that stores weapon values
            // stores a double first (BV), then an Integer (heat),
            // then a String (weapon name)
            // for 0 heat weapons, just stores BV and name
            ArrayList<Object> weaponValues = new ArrayList<>();
            if (weaponHeat > 0) {
                // store heat and BV, for sorting a few lines down;
                weaponValues.add(dBV);
                weaponValues.add(weaponHeat);
                weaponValues.add(weaponName);
                heatBVs.add(weaponValues);
            } else {
                weaponValues.add(dBV);
                weaponValues.add(weaponName);
                nonHeatBVs.add(weaponValues);
            }

            maximumHeat += weaponHeat;
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
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(mek));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(mek)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        if (mek.hasVibroblades()) {
            for (int location = Mech.LOC_RARM; location <= Mech.LOC_LARM; location++) {
                for (int slot = 0; slot < mek.locations(); slot++) {
                    CriticalSlot cs = mek.getCritical(location, slot);

                    if ((cs != null)
                            && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                        Mounted mount = cs.getMount();
                        if ((mount.getType() instanceof MiscType)
                                && mount.getType().hasFlag(MiscType.F_CLUB)
                                && ((MiscType) mount.getType()).isVibroblade()) {
                            ArrayList<Object> weaponValues = new ArrayList<>();
                            double dBV = mount.getType().getBV(mek);
                            if (mek.hasFunctionalArmAES(mount.getLocation())) {
                                dBV *= 1.25;
                            }
                            weaponValues.add(dBV);
                            weaponValues.add((double) mek.getActiveVibrobladeHeat(location, true));
                            weaponValues.add(mount.getName());
                            heatBVs.add(weaponValues);

                            bvText.append(startRow);
                            bvText.append(startColumn);

                            bvText.append(mount.getName());
                            bvText.append(endColumn);
                            bvText.append(startColumn);
                            bvText.append(endColumn);
                            bvText.append(startColumn);
                            bvText.append("+ ");
                            bvText.append(mek.getActiveVibrobladeHeat(location, true));
                            bvText.append(endColumn);
                            bvText.append(endRow);
                            maximumHeat += mek.getActiveVibrobladeHeat(location, true);
                            break;
                        }
                    }
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

        bvText.append("Total Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(maximumHeat);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapons with no heat at full BV:");
        bvText.append(endColumn);
        bvText.append(endRow);
        // count heat-free weapons always at full modified BV
        for (ArrayList<Object> nonHeatWeapon : nonHeatBVs) {
            weaponBV += (Double) nonHeatWeapon.get(0);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(nonHeatWeapon.get(1));
            if (nonHeatWeapon.get(1).toString().length() < 8) {
                bvText.append("\t");
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(nonHeatWeapon.get(0));
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Heat Modified Weapons BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        if (maximumHeat > mechHeatEfficiency) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("(Heat Exceeds Mech Heat Efficiency) ");

            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if (maximumHeat <= mechHeatEfficiency) {
            // count all weapons equal
            for (ArrayList<Object> weaponValues : heatBVs) {
                // name
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(2));
                weaponBV += (Double) weaponValues.get(0);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(0));
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        } else {
            // this will count heat-generating weapons at full modified BV until
            // heat efficiency is reached or passed with one weapon

            // sort the heat-using weapons by modified BV
            heatBVs.sort((obj1, obj2) -> {
                Double obj1BV = (Double) obj1.get(0); // BV
                Double obj2BV = (Double) obj2.get(0); // BV

                // first element in the ArrayList is BV, second is heat
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
            // heat efficiency is reached or
            // passed with one weapon
            double heatAdded = 0;
            for (ArrayList<Object> weaponValues : heatBVs) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(2));
                bvText.append(endColumn);
                bvText.append(startColumn);

                double dBV = (Double) weaponValues.get(0);
                if (heatAdded >= mechHeatEfficiency) {
                    if (mek.useReducedOverheatModifierBV()) {
                        dBV /= 10;
                    } else {
                        dBV /= 2;
                    }
                }
                if (heatAdded >= mechHeatEfficiency) {
                    if (mek.useReducedOverheatModifierBV()) {
                        bvText.append("Heat efficiency reached, BV * 0.1");
                    } else {
                        bvText.append("Heat efficiency reached, half BV");
                    }
                }
                heatAdded += (Double) weaponValues.get(1);
                weaponBV += dBV;
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(dBV);
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append("Heat count: ").append(heatAdded);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
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

        bvText.append("Total Weapons BV Adjusted For Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Misc Offensive Equipment: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : mek.getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }
            // vibroblades have been counted under weapons
            if ((mounted.getType() instanceof MiscType)
                    && mounted.getType().hasFlag(MiscType.F_CLUB)
                    && ((MiscType) mounted.getType()).isVibroblade()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_ECM)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                    || mtype.hasFlag(MiscType.F_MASS)
                    || mtype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_CHAFF_POD)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)
                    || mtype.hasFlag(MiscType.F_SPIKES)
                    || mtype.hasFlag(MiscType.F_HARJEL_II)
                    || mtype.hasFlag(MiscType.F_HARJEL_III)
                    || (mtype.hasFlag(MiscType.F_CLUB) && (mtype.hasSubType(MiscType.S_SHIELD_LARGE)
                    || mtype.hasSubType(MiscType.S_SHIELD_MEDIUM) || mtype.hasSubType(MiscType.S_SHIELD_SMALL)))) {
                continue;
            }
            double bv = mtype.getBV(mek);
            // if physical weapon linked to AES, multiply by 1.25
            if ((mtype.hasFlag(MiscType.F_CLUB) || mtype.hasFlag(MiscType.F_HAND_WEAPON))
                    && mek.hasFunctionalArmAES(mounted.getLocation())) {
                bv *= 1.25;
            }

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

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        for (Mounted mounted : mek.getAmmo()) {
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
                Player tmpP = mek.getOwner();

                if (tmpP != null) {
                    // Okay, actually check for friendly TAG.
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(mek);
                    } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (mek.getGame() != null)) {
                        for (Enumeration<Team> e = mek.getGame().getTeams(); e.hasMoreElements(); ) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(mek.getGame())) {
                                    tagBV += atype.getBV(mek);
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
                ammo.put(key, atype.getBV(mek));
            } else {
                ammo.put(key, atype.getBV(mek) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // of that
        // type on the mech is reached.
        for (String key : keys) {

            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    ammoBV += ammo.get(key);
                }
            } else {
                // Ammo with no matching weapons counts 0, unless it's a coolant
                // pod
                // because coolant pods have no matching weapon
                if (key.equals(Integer.valueOf(AmmoType.T_COOLANT_POD).toString() + "1")) {
                    ammoBV += ammo.get(key);
                }
            }
        }
        weaponBV += ammoBV;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Ammo BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(ammoBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        double aesMultiplier = 1;
        if (mek.hasFunctionalArmAES(Mech.LOC_LARM)) {
            aesMultiplier += 0.1;
        }
        if (mek.hasFunctionalArmAES(Mech.LOC_RARM)) {
            aesMultiplier += 0.1;
        }
        if (mek.hasFunctionalLegAES()) {
            if (mek instanceof BipedMech) {
                aesMultiplier += 0.2;
            } else if (mek instanceof QuadMech) {
                aesMultiplier += 0.4;
            }
        }

        double weight = mek.getWeight() * aesMultiplier;

        if (aesMultiplier > 1) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Weight x AES Multiplier ");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(mek.getWeight());
            bvText.append(" x ");
            bvText.append(aesMultiplier);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(" = ");
            bvText.append(weight);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        // add tonnage, adjusted for TSM
        if (mek.hasTSM(true)) {
            weaponBV += weight * 1.5;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Add weight + TSM Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight);
            bvText.append(" * 1.5");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight * 1.5);
            bvText.append(endColumn);
            bvText.append(endRow);
        } else if (mek.hasIndustrialTSM()) {
            weaponBV += weight * 1.15;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Add weight + Industrial TSM Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight);
            bvText.append(" * 1.115");
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(weight * 1.15);
            bvText.append(endColumn);
            bvText.append(endRow);
        } else {
            weaponBV += weight;
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("Add weight");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+");
            bvText.append(weight);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if ((mek.getCockpitType() == Mech.COCKPIT_INDUSTRIAL)
                || (mek.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            // industrial without advanced firing control get's 0.9 mod to
            // offensive BV
            bvText.append("Weapon BV * Firing Control Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(weaponBV);
            bvText.append(" * ");
            bvText.append("0.9");
            bvText.append(endColumn);
            weaponBV *= 0.9;
            bvText.append(startColumn);
            bvText.append(" = ");
            bvText.append(weaponBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        double speedFactor = Math
                .pow(1 + ((((double) runMP + (Math
                        .round(Math.max(jumpMP, umuMP) / 2.0))) - 5) / 10), 1.2);
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

        if (mek.useGeometricMeanBV()) {
            bvText.append("2 * sqrt(Offensive BV * Defensive BV");
        } else {
            bvText.append("Offensive BV + Defensive BV");
        }
        bvText.append(endColumn);
        bvText.append(startColumn);

        double finalBV;
        if (mek.useGeometricMeanBV()) {
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
        double totalBV = finalBV;

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        double cockpitMod = 1;
        if ((mek.getCockpitType() == Mech.COCKPIT_SMALL)
                || (mek.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)
                || (mek.getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE)) {
            cockpitMod = 0.95;
            finalBV *= cockpitMod;
        } else if (mek.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            finalBV *= 0.95;
        } else if (mek.getCockpitType() == Mech.COCKPIT_INTERFACE) {
            cockpitMod = 1.3;
            finalBV *= cockpitMod;
        }
        finalBV = Math.round(finalBV);
        bvText.append("Total BV * Cockpit Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(totalBV);
        bvText.append(" * ");
        bvText.append(cockpitMod);
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
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        if (!ignoreC3) {
            xbv += mek.getExtraC3BV((int) Math.round(finalBV));
        }
        finalBV = (int) Math.round(finalBV + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = mek.getCrew().getBVSkillMultiplier(mek.getGame());
        }

        return (int) Math.round(finalBV * pilotFactor);
    }

    /**
     * Used in BV calculations. Any equipment that will destroy the unit or leg it if it explodes
     * decreases the defensive battle rating. This is anything in the head, CT, or leg,
     * or side torso if it has >= 3 engine crits, or any location that can transfer damage to that
     * location.
     *
     * @param loc The location index
     * @return    Whether explosive equipment in the location should decrease BV
     */
    private static boolean hasExplosiveEquipmentPenalty(Mech mek, int loc) {
        if ((loc == Entity.LOC_NONE) || mek.hasCASEII(loc)) {
            return false;
        }
        if (!mek.entityIsQuad() && ((loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM))) {
            return !mek.locationHasCase(loc) && hasExplosiveEquipmentPenalty(mek, mek.getTransferLocation(loc));
        } else if ((loc == Mech.LOC_RT) || (loc == Mech.LOC_LT)) {
            return !mek.locationHasCase(loc) || (mek.getEngine().getSideTorsoCriticalSlots().length >= 3);
        } else {
            return true;
        }
    }
}
