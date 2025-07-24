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

import static megamek.common.AmmoType.AmmoTypeEnum.*;

class ComputeAttackerToHitMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition Attacker has damaged
     * sensors? You'll find that here. Defender's a superheavy mek? Using a weapon with a TH penalty? Those are in other
     * methods.
     *
     * @param game              The current {@link Game}
     * @param attacker          The Entity making this attack
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
    static ToHitData compileAttackerToHitMods(Game game, Entity attacker, Targetable target, LosEffects los,
          ToHitData toHit, int aimingAt, AimingMode aimingMode, WeaponType wtype, Mounted<?> weapon, int weaponId,
          AmmoType atype, EnumSet<AmmoType.Munitions> munition, boolean isFlakAttack, boolean isHaywireINarced,
          boolean isNemesisConfused, boolean isWeaponFieldGuns, boolean usesAmmo) {
        
        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // if we don't have a weapon, that we are attacking with, then the rest of this is either meaningless or 
        // likely to fail
        if (weaponId == WeaponType.WEAPON_NA) {
            return toHit;
        }

        // Modifiers related to an action the attacker is taking

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attacker.getId()));

        // attacker prone
        toHit.append(Compute.getProneMods(game, attacker, weaponId));

        // add penalty for called shots and change hit table, if necessary
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS) && weapon != null) {
            int call = weapon.getCalledShot().getCall();
            if ((call > CalledShot.CALLED_NONE) && !aimingMode.isNone()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      Messages.getString("WeaponAttackAction.CantAimAndCallShots"));
            }

            switch (call) {
                case CalledShot.CALLED_NONE:
                    break;
                case CalledShot.CALLED_HIGH:
                    toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledHigh"));
                    toHit.setHitTable(ToHitData.HIT_ABOVE);
                    break;
                case CalledShot.CALLED_LOW:
                    if (los.getTargetCover() == LosEffects.COVER_HORIZONTAL) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                              Messages.getString("WeaponAttackAction.CalledLowPartCover"));
                    }
                    toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledLow"));
                    toHit.setHitTable(ToHitData.HIT_BELOW);
                    break;
                case CalledShot.CALLED_LEFT:
                    // handled by Compute#targetSideTable
                    toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledLeft"));
                    break;
                case CalledShot.CALLED_RIGHT:
                    // handled by Compute#targetSideTable
                    toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledRight"));
                    break;
            }
        }

        // Dropping units get hit with a +2 dropping penalty AND the +3 Jumping penalty, SO p.22
        if (attacker.isAirborne() && !attacker.isAero()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.Dropping"));
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.Jumping"));
        }

        // Infantry taking cover suffer a +1 penalty
        if ((attacker instanceof Infantry infantry) && infantry.isTakingCover()) {
            if (attacker.getPosition().direction(target.getPosition()) == attacker.getFacing()) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // Quadvee converting to a new mode
        if (attacker instanceof QuadVee && attacker.isConvertingNow()) {
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.QuadVeeConverting"));
        }

        // we are bracing
        if ((weapon != null) && attacker.isBracing() && (attacker.braceLocation() == weapon.getLocation())) {
            toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Bracing"));
        }

        // Secondary targets modifier,
        // if this is not a iNarc Nemesis confused attack
        // Inf field guns don't get secondary target mods, TO pg 311
        if (!isNemesisConfused && !isWeaponFieldGuns) {
            toHit.append(Compute.getSecondaryTargetMod(game, attacker, target));
        }

        // if we're spotting for indirect fire, add +1
        if (attacker.isSpotting() &&
                  !attacker.getCrew().hasActiveCommandConsole() &&
                  game.getTagInfo().stream().noneMatch(inf -> inf.attackerId == attacker.getId())) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeSpotting"));
        }

        // Special effects (like tasers) affecting the attacker

        // Attacker is battle armor and affected by BA taser feedback
        if (attacker.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeTaserFeedback"));
        }

        // If a unit is suffering from electromagnetic interference, they get a blanket +2. Sucks to be them.
        if (attacker.isSufferingEMI()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.EMI"));
        }

        // heat
        if (attacker.getHeatFiringModifier() != 0) {
            toHit.addModifier(attacker.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }

        // Attacker hit with an iNarc Haywire pod
        if (isHaywireINarced) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.iNarcHaywire"));
        }

        // Attacker affected by Taser interference
        if (attacker.getTaserInterferenceRounds() > 0) {
            toHit.addModifier(attacker.getTaserInterference(), Messages.getString("WeaponAttackAction.AeHitByTaser"));
        }

        // Attacker affected by TSEMP interference
        if (attacker.getTsempEffect() == MMConstants.TSEMP_EFFECT_INTERFERENCE) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeTsemped"));
        }

        // Special Equipment that that attacker possesses

        // Attacker has an AES system
        if ((weapon != null) && attacker.hasFunctionalArmAES(weapon.getLocation()) && !weapon.isSplit()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.AES"));
        }

        // Heavy infantry have +1 penalty
        if ((attacker instanceof Infantry) && attacker.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.HeavyArmor"));
        }

        // industrial cockpit: +1 to hit, +2 for primitive
        if ((attacker instanceof Mek mek) && (mek.getCockpitType() == Mek.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.PrimIndustrialNoAfc"));
        } else if ((attacker instanceof Mek) && !((Mek) attacker).hasAdvancedFireControl()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.IndustrialNoAfc"));
        }

        // primitive industrial cockpit with advanced firing control: +1 to hit
        if ((attacker instanceof Mek mek) && (mek.getCockpitType() == Mek.COCKPIT_PRIMITIVE) && mek.isIndustrial()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.PrimIndustrialAfc"));
        }

        // Support vehicle basic/advanced fire control systems
        if ((attacker instanceof SupportTank) || (attacker instanceof SupportVTOL)) {
            if (!attacker.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL) &&
                      !attacker.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.SupVeeNoFc"));
            } else if (attacker.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL) &&
                             !(attacker.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL))) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.SupVeeBfc"));
            }
        }

        // Is the attacker hindered by a shield?
        if (attacker.hasShield() && (weapon != null)) {
            // active shield has already been checked as it makes shots impossible
            // time to check passive defense and no defense
            if (attacker.hasPassiveShield(weapon.getLocation(), weapon.isRearMounted())) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.PassiveShield"));
            } else if (attacker.hasNoDefenseShield(weapon.getLocation())) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Shield"));
            }
        }

        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode.isTargetingComputer() && (aimingAt != Entity.LOC_NONE)) {
            if (attacker.hasActiveEiCockpit()) {
                if (attacker.hasTargComp()) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.AimWithTCompEi"));
                } else {
                    toHit.addModifier(6, Messages.getString("WeaponAttackAction.AimWithEiOnly"));
                }
            } else {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.AimWithTCompOnly"));
            }
        } else {
            // LB-X cluster, HAG flak, flak ammo ineligible for TC bonus
            boolean usesLBXCluster = usesAmmo
                  && (atype != null)
                  && (atype.getAmmoType() == AC_LBX || atype.getAmmoType() == AC_LBX_THB)
                  && munition.contains(AmmoType.Munitions.M_CLUSTER);
            boolean usesHAGFlak = usesAmmo && (atype != null) && (atype.getAmmoType() == HAG) && isFlakAttack;
            boolean isSBGauss = usesAmmo && (atype != null) && (atype.getAmmoType() == SBGAUSS);
            boolean isFlakAmmo = usesAmmo && (atype != null) && (munition.contains(AmmoType.Munitions.M_FLAK));
            if (attacker.hasTargComp()
                  && (wtype != null)
                  && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                  && !wtype.hasAnyFlag(WeaponType.F_CWS, WeaponType.F_TASER)
                  && (!usesAmmo || !(usesLBXCluster || usesHAGFlak || isSBGauss || isFlakAmmo))) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TComp"));
            }
        }

        // penalty for an active void signature system
        if (attacker.isVoidSigActive()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeVoidSig"));
        }

        // Critical damage effects

        // actuator & sensor damage to attacker (includes partial repairs)
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(attacker, weapon));
        }

        // Vehicle criticals
        if (attacker instanceof Tank tank) {
            int sensorHits = tank.getSensorHits();
            if (sensorHits > 0) {
                toHit.addModifier(sensorHits, Messages.getString("WeaponAttackAction.SensorDamage"));
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
     * @param attacker     The Entity making this attack
     * @param toHit  The running total ToHitData for this WeaponAttackAction
     * @param weapon The weapon being used (it's type should be WeaponType!)
     */
    static ToHitData compileCrewToHitMods(Game game, Entity attacker, ToHitData toHit, Mounted<?> weapon) {

        if (attacker == null) {
            // These checks won't work without a valid attacker
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Now for modifiers affecting the attacker's crew

        // Bonuses for dual cockpits, etc
        // Bonus to gunnery if both crew members are active; a pilot who takes the gunner's role get +1.
        if ((attacker instanceof Mek mek) && (mek.getCockpitType() == Mek.COCKPIT_DUAL)) {
            if (!attacker.getCrew().isActive(attacker.getCrew().getCrewType().getGunnerPos())) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.GunnerHit"));
            } else if (attacker.getCrew().hasDedicatedGunner()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.DualCockpit"));
            }
        }

        // The pilot or technical officer can take over the gunner's duties but suffers a +2 penalty.
        if ((attacker instanceof TripodMek || attacker instanceof QuadVee) && !attacker.getCrew().hasDedicatedGunner()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.GunnerHit"));
        }

        // Fatigue
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_FATIGUE) &&
                  attacker.getCrew().isGunneryFatigued()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.Fatigue"));
        }

        // Injuries

        // Aero unit pilot/crew hits
        if (attacker instanceof Aero) {
            int pilotHits = attacker.getCrew().getHits();
            if ((pilotHits > 0) && !attacker.isCapitalFighter()) {
                toHit.addModifier(pilotHits, Messages.getString("WeaponAttackAction.PilotHits"));
            }
        }

        // Vehicle crew hits
        if (attacker instanceof Tank tank) {
            if (tank.isCommanderHit()) {
                if (attacker instanceof VTOL) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.CopilotHit"));
                } else {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.CmdrHit"));
                }
            }
        }

        // Manei Domini Upgrades

        // VDNI
        if (attacker.hasAbility(OptionsConstants.MD_VDNI) || attacker.hasAbility(OptionsConstants.MD_BVDNI)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.Vdni"));
        }

        // check for cyber eye laser sighting on ranged attacks
        if (attacker.isConventionalInfantry()
              && attacker.hasAbility(OptionsConstants.MD_CYBER_IMP_LASER)
              && (weapon != null)
              && !(weapon.getType() instanceof InfantryAttack)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.MdEye"));
        }

        return toHit;
    }

    private ComputeAttackerToHitMods() { }
}
