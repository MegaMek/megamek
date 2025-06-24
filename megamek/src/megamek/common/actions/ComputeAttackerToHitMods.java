/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.actions;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.InfantryAttack;

import java.util.EnumSet;

class ComputeAttackerToHitMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition Attacker has damaged
     * sensors? You'll find that here. Defender's a superheavy mek? Using a weapon with a TH penalty? Those are in other
     * methods.
     *
     * @param game              The current {@link Game}
     * @param ae                The Entity making this attack
     * @param target            The Targetable object being attacked
     * @param los               The calculated LOS between attacker and target
     * @param toHit             The running total ToHitData for this WeaponAttackAction
     * @param aimingAt          An int value representing the location being aimed at
     * @param aimingMode        An int value that determines the reason aiming is allowed
     * @param wtype             The WeaponType of the weapon being used
     * @param weapon            The Mounted weapon being used
     * @param weaponId          The id number of the weapon being used - used by some external calculations
     * @param atype             The AmmoType being used for this attack
     * @param munition          Long indicating the munition type flag being used, if applicable
     * @param isFlakAttack      flag that indicates whether the attacker is using Flak against an airborne target
     * @param isHaywireINarced  flag that indicates whether the attacker is affected by an iNarc Haywire pod
     * @param isNemesisConfused flag that indicates whether the attack is affected by an iNarc Nemesis pod
     * @param isWeaponFieldGuns flag that indicates whether the attack is being made with infantry field guns
     * @param usesAmmo          flag that indicates if the WeaponType being used is ammo-fed
     */
    static ToHitData compileAttackerToHitMods(Game game, Entity ae, Targetable target, LosEffects los,
          ToHitData toHit, int aimingAt, AimingMode aimingMode, WeaponType wtype, Mounted<?> weapon, int weaponId,
          AmmoType atype, EnumSet<AmmoType.Munitions> munition, boolean isFlakAttack, boolean isHaywireINarced,
          boolean isNemesisConfused, boolean isWeaponFieldGuns, boolean usesAmmo) {
        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // if we don't have a weapon, that we are attacking with, then the rest of this
        // is
        // either meaningless or likely to fail
        if (weaponId == WeaponType.WEAPON_NA) {
            return toHit;
        }

        // Modifiers related to an action the attacker is taking

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, ae.getId()));

        // attacker prone
        if (weaponId > WeaponType.WEAPON_NA) {
            toHit.append(Compute.getProneMods(game, ae, weaponId));
        }

        // add penalty for called shots and change hit table, if necessary
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS) && weapon != null) {
            int call = weapon.getCalledShot().getCall();
            if ((call > CalledShot.CALLED_NONE) && !aimingMode.isNone()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      megamek.client.ui.Messages.getString("WeaponAttackAction.CantAimAndCallShots"));
            }

            switch (call) {
                case CalledShot.CALLED_NONE:
                    break;
                case CalledShot.CALLED_HIGH:
                    toHit.addModifier(+3, megamek.client.ui.Messages.getString("WeaponAttackAction.CalledHigh"));
                    toHit.setHitTable(ToHitData.HIT_ABOVE);
                    break;
                case CalledShot.CALLED_LOW:
                    if (los.getTargetCover() == LosEffects.COVER_HORIZONTAL) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                              megamek.client.ui.Messages.getString("WeaponAttackAction.CalledLowPartCover"));
                    }
                    toHit.addModifier(+3, megamek.client.ui.Messages.getString("WeaponAttackAction.CalledLow"));
                    toHit.setHitTable(ToHitData.HIT_BELOW);
                    break;
                case CalledShot.CALLED_LEFT:
                    // handled by Compute#targetSideTable
                    toHit.addModifier(+3, megamek.client.ui.Messages.getString("WeaponAttackAction.CalledLeft"));
                    break;
                case CalledShot.CALLED_RIGHT:
                    // handled by Compute#targetSideTable
                    toHit.addModifier(+3, megamek.client.ui.Messages.getString("WeaponAttackAction.CalledRight"));
                    break;
            }
        }

        // Dropping units get hit with a +2 dropping penalty AND the +3 Jumping penalty
        // (SO p22)
        if (ae.isAirborne() && !ae.isAero()) {
            toHit.addModifier(+2, megamek.client.ui.Messages.getString("WeaponAttackAction.Dropping"));
            toHit.addModifier(+3, megamek.client.ui.Messages.getString("WeaponAttackAction.Jumping"));
        }

        // Infantry taking cover suffer a +1 penalty
        if ((ae instanceof Infantry) && ((Infantry) ae).isTakingCover()) {
            if (ae.getPosition().direction(target.getPosition()) == ae.getFacing()) {
                toHit.addModifier(+1, megamek.client.ui.Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // Quadvee converting to a new mode
        if (ae instanceof QuadVee && ae.isConvertingNow()) {
            toHit.addModifier(+3, megamek.client.ui.Messages.getString("WeaponAttackAction.QuadVeeConverting"));
        }

        // we are bracing
        if (ae.isBracing() && (ae.braceLocation() == weapon.getLocation())) {
            toHit.addModifier(-2, megamek.client.ui.Messages.getString("WeaponAttackAction.Bracing"));
        }

        // Secondary targets modifier,
        // if this is not a iNarc Nemesis confused attack
        // Inf field guns don't get secondary target mods, TO pg 311
        if (!isNemesisConfused && !isWeaponFieldGuns) {
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        }

        // if we're spotting for indirect fire, add +1
        if (ae.isSpotting() &&
                  !ae.getCrew().hasActiveCommandConsole() &&
                  game.getTagInfo().stream().noneMatch(inf -> inf.attackerId == ae.getId())) {
            toHit.addModifier(+1, megamek.client.ui.Messages.getString("WeaponAttackAction.AeSpotting"));
        }

        // Special effects (like tasers) affecting the attacker

        // Attacker is battle armor and affected by BA taser feedback
        if (ae.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.AeTaserFeedback"));
        }

        // If a unit is suffering from electromagnetic interference, they get a
        // blanket +2. Sucks to be them.
        if (ae.isSufferingEMI()) {
            toHit.addModifier(+2, megamek.client.ui.Messages.getString("WeaponAttackAction.EMI"));
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), megamek.client.ui.Messages.getString("WeaponAttackAction.Heat"));
        }

        // Attacker hit with an iNarc Haywire pod
        if (isHaywireINarced) {
            toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.iNarcHaywire"));
        }

        // Attacker affected by Taser interference
        if (ae.getTaserInterferenceRounds() > 0) {
            toHit.addModifier(ae.getTaserInterference(), megamek.client.ui.Messages.getString("WeaponAttackAction.AeHitByTaser"));
        }

        // Attacker affected by TSEMP interference
        if (ae.getTsempEffect() == MMConstants.TSEMP_EFFECT_INTERFERENCE) {
            toHit.addModifier(+2, megamek.client.ui.Messages.getString("WeaponAttackAction.AeTsemped"));
        }

        // Special Equipment that that attacker possesses

        // Attacker has an AES system
        if (weapon != null && ae.hasFunctionalArmAES(weapon.getLocation()) && !weapon.isSplit()) {
            toHit.addModifier(-1, megamek.client.ui.Messages.getString("WeaponAttackAction.AES"));
        }

        // Heavy infantry have +1 penalty
        if ((ae instanceof Infantry) && ae.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.HeavyArmor"));
        }

        // industrial cockpit: +1 to hit, +2 for primitive
        if ((ae instanceof Mek) && (((Mek) ae).getCockpitType() == Mek.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            toHit.addModifier(2, megamek.client.ui.Messages.getString("WeaponAttackAction.PrimIndustrialNoAfc"));
        } else if ((ae instanceof Mek) && !((Mek) ae).hasAdvancedFireControl()) {
            toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.IndustrialNoAfc"));
        }

        // primitive industrial cockpit with advanced firing control: +1 to hit
        if ((ae instanceof Mek) &&
                  (((Mek) ae).getCockpitType() == Mek.COCKPIT_PRIMITIVE) &&
                  ((Mek) ae).isIndustrial()) {
            toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.PrimIndustrialAfc"));
        }

        // Support vehicle basic/advanced fire control systems
        if ((ae instanceof SupportTank) || (ae instanceof SupportVTOL)) {
            if (!ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL) &&
                      !ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                toHit.addModifier(2, megamek.client.ui.Messages.getString("WeaponAttackAction.SupVeeNoFc"));
            } else if (ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL) &&
                             !(ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL))) {
                toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.SupVeeBfc"));
            }
        }

        // Is the attacker hindered by a shield?
        if (ae.hasShield() && weapon != null) {
            // active shield has already been checked as it makes shots
            // impossible
            // time to check passive defense and no defense

            if (ae.hasPassiveShield(weapon.getLocation(), weapon.isRearMounted())) {
                toHit.addModifier(+2, megamek.client.ui.Messages.getString("WeaponAttackAction.PassiveShield"));
            } else if (ae.hasNoDefenseShield(weapon.getLocation())) {
                toHit.addModifier(+1, megamek.client.ui.Messages.getString("WeaponAttackAction.Shield"));
            }
        }

        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode.isTargetingComputer() && (aimingAt != Entity.LOC_NONE)) {
            if (ae.hasActiveEiCockpit()) {
                if (ae.hasTargComp()) {
                    toHit.addModifier(2, megamek.client.ui.Messages.getString("WeaponAttackAction.AimWithTCompEi"));
                } else {
                    toHit.addModifier(6, megamek.client.ui.Messages.getString("WeaponAttackAction.AimWithEiOnly"));
                }
            } else {
                toHit.addModifier(3, megamek.client.ui.Messages.getString("WeaponAttackAction.AimWithTCompOnly"));
            }
        } else {
            // LB-X cluster, HAG flak, flak ammo ineligible for TC bonus
            boolean usesLBXCluster = usesAmmo &&
                                           (atype != null) &&
                                           (atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX ||
                                                  atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX_THB) &&
                                           munition.contains(AmmoType.Munitions.M_CLUSTER);
            boolean usesHAGFlak = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.AmmoTypeEnum.HAG && isFlakAttack;
            boolean isSBGauss = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.AmmoTypeEnum.SBGAUSS;
            boolean isFlakAmmo = usesAmmo && (atype != null) && (munition.contains(AmmoType.Munitions.M_FLAK));
            if (ae.hasTargComp() &&
                      wtype != null &&
                      wtype.hasFlag(WeaponType.F_DIRECT_FIRE) &&
                      !wtype.hasFlag(WeaponType.F_CWS) &&
                      !wtype.hasFlag(WeaponType.F_TASER) &&
                      (!usesAmmo || !(usesLBXCluster || usesHAGFlak || isSBGauss || isFlakAmmo))) {
                toHit.addModifier(-1, megamek.client.ui.Messages.getString("WeaponAttackAction.TComp"));
            }
        }

        // penalty for an active void signature system
        if (ae.isVoidSigActive()) {
            toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.AeVoidSig"));
        }

        // Critical damage effects

        // actuator & sensor damage to attacker (includes partial repairs)
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
        }

        // Vehicle criticals
        if (ae instanceof Tank) {
            Tank tank = (Tank) ae;
            int sensors = tank.getSensorHits();
            if (sensors > 0) {
                toHit.addModifier(sensors, megamek.client.ui.Messages.getString("WeaponAttackAction.SensorDamage"));
            }
            if (weapon != null && tank.isStabiliserHit(weapon.getLocation())) {
                toHit.addModifier(Compute.getAttackerMovementModifier(game, tank.getId()).getValue(),
                      "stabiliser damage");
            }
        }

        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's crew/pilot Pilot wounded? Has
     * an SPA? You'll find that here. Defender's a superheavy mek? Using a weapon with a TH penalty? Those are in other
     * methods.
     *
     * @param game   The current {@link Game}
     * @param ae     The Entity making this attack
     * @param te     The target Entity
     * @param toHit  The running total ToHitData for this WeaponAttackAction
     * @param weapon The weapon being used (it's type should be WeaponType!)
     */
    static ToHitData compileCrewToHitMods(Game game, Entity ae, Entity te, ToHitData toHit, Mounted<?> weapon) {

        if (ae == null) {
            // These checks won't work without a valid attacker
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Now for modifiers affecting the attacker's crew

        // Bonuses for dual cockpits, etc
        // Bonus to gunnery if both crew members are active; a pilot who takes the
        // gunner's role get +1.
        if (ae instanceof Mek && ((Mek) ae).getCockpitType() == Mek.COCKPIT_DUAL) {
            if (!ae.getCrew().isActive(ae.getCrew().getCrewType().getGunnerPos())) {
                toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.GunnerHit"));
            } else if (ae.getCrew().hasDedicatedGunner()) {
                toHit.addModifier(-1, megamek.client.ui.Messages.getString("WeaponAttackAction.DualCockpit"));
            }
        }

        // The pilot or technical officer can take over the gunner's duties but suffers
        // a +2 penalty.
        if ((ae instanceof TripodMek || ae instanceof QuadVee) && !ae.getCrew().hasDedicatedGunner()) {
            toHit.addModifier(+2, megamek.client.ui.Messages.getString("WeaponAttackAction.GunnerHit"));
        }

        // Fatigue
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_FATIGUE) &&
                  ae.getCrew().isGunneryFatigued()) {
            toHit.addModifier(1, megamek.client.ui.Messages.getString("WeaponAttackAction.Fatigue"));
        }

        // Injuries

        // Aero unit pilot/crew hits
        if (ae instanceof Aero) {
            int pilothits = ae.getCrew().getHits();
            if ((pilothits > 0) && !ae.isCapitalFighter()) {
                toHit.addModifier(pilothits, megamek.client.ui.Messages.getString("WeaponAttackAction.PilotHits"));
            }
        }

        // Vehicle crew hits
        if (ae instanceof Tank) {
            Tank tank = (Tank) ae;
            if (tank.isCommanderHit()) {
                if (ae instanceof VTOL) {
                    toHit.addModifier(+1, megamek.client.ui.Messages.getString("WeaponAttackAction.CopilotHit"));
                } else {
                    toHit.addModifier(+1, megamek.client.ui.Messages.getString("WeaponAttackAction.CmdrHit"));
                }
            }
        }

        // Manei Domini Upgrades

        // VDNI
        if (ae.hasAbility(OptionsConstants.MD_VDNI) || ae.hasAbility(OptionsConstants.MD_BVDNI)) {
            toHit.addModifier(-1, megamek.client.ui.Messages.getString("WeaponAttackAction.Vdni"));
        }

        WeaponType wtype = ((weapon != null) && (weapon.getType() instanceof WeaponType)) ?
                                 (WeaponType) weapon.getType() :
                                 null;

        if (ae.isConventionalInfantry()) {
            // check for cyber eye laser sighting on ranged attacks
            if (ae.hasAbility(OptionsConstants.MD_CYBER_IMP_LASER) && !(wtype instanceof InfantryAttack)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.MdEye"));
            }
        }

        return toHit;
    }

    private ComputeAttackerToHitMods() { }
}
