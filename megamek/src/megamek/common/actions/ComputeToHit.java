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

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.battlearmor.CLBALBX;
import megamek.common.weapons.bayweapons.ScreenLauncherBayWeapon;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import megamek.common.weapons.lasers.ISBombastLaser;
import megamek.common.weapons.lasers.VariableSpeedPulseLaserWeapon;
import megamek.common.weapons.lrms.LRTWeapon;
import megamek.common.weapons.srms.SRTWeapon;
import megamek.logging.MMLogger;

import java.util.EnumSet;
import java.util.List;

class ComputeToHit {

    private static final MMLogger logger = MMLogger.create(ComputeToHit.class);

    /**
     * To-hit number for attacker firing a weapon at the target.
     */
    static ToHitData toHitCalc(Game game, int attackerId, Targetable target, int weaponId, int aimingAt,
          AimingMode aimingMode, boolean isNemesisConfused, boolean exchangeSwarmTarget, Targetable oldTarget,
          Targetable originalTarget, boolean isStrafing, boolean isPointblankShot, List<ECMInfo> allECMInfo,
          boolean evenIfAlreadyFired, int ammoId, int ammoCarrier) {
        final Entity ae = game.getEntity(attackerId);
        final WeaponMounted weapon = (WeaponMounted) ae.getEquipment(weaponId);
        if (weapon == null) {
            logger.error("Attempted toHit calculation with a null weapon!");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No weapon");
        }
        final AmmoMounted linkedAmmo;
        if (ammoId == WeaponAttackAction.UNASSIGNED) {
            linkedAmmo = weapon.getLinkedAmmo();
        } else {
            Entity carrier = (ammoCarrier == WeaponAttackAction.UNASSIGNED) ? ae : game.getEntity(ammoCarrier);
            linkedAmmo = (carrier == null) ? null : carrier.getAmmo(ammoId);
        }

        final WeaponType wtype = weapon.getType();

        if (target == null) {
            logger.error(attackerId + "Attempting to attack null target");
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, Messages.getString("MovementDisplay.NoTarget"));
        }

        Targetable swarmSecondaryTarget = target;
        if (exchangeSwarmTarget) {
            // this is a swarm attack against a new target
            // first, exchange original and new targets to get all mods
            // as if firing against original target.
            // at the end of this function, we remove target terrain
            // and movement mods, and add those for the new target
            target = originalTarget;
        }

        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean isAttackerInfantry = ae instanceof Infantry;

        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY) && !ae.isSupportVehicle();

        boolean isWeaponFieldGuns = isAttackerInfantry && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS);
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        // 2002-09-16 Infantry weapons have unlimited ammo.

        final boolean usesAmmo = (wtype.getAmmoType() != AmmoType.AmmoTypeEnum.NA) && !isWeaponInfantry;

        final AmmoMounted ammo = usesAmmo ? linkedAmmo : null;

        final AmmoType atype = ammo == null ? null : ammo.getType();

        EnumSet<AmmoType.Munitions> munition = EnumSet.of(AmmoType.Munitions.M_STANDARD);
        if (atype != null) {
            munition = atype.getMunitionType();
        }

        final boolean targetInBuilding = Compute.isInBuilding(game, te);

        boolean bMekTankStealthActive = false;
        if ((ae instanceof Mek) || (ae instanceof Tank)) {
            bMekTankStealthActive = ae.isStealthActive();
        }

        boolean isFlakAttack = (te != null) &&
                                     Compute.isFlakAttack(ae, te) &&
                                     (wtype instanceof CLBALBX ||
                                            ((atype != null) &&
                                                   ((((atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX) ||
                                                            (atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX_THB) ||
                                                            (atype.getAmmoType() == AmmoType.AmmoTypeEnum.SBGAUSS)) &&
                                                           (munition.contains(AmmoType.Munitions.M_CLUSTER))) ||
                                                          munition.contains(AmmoType.Munitions.M_FLAK) ||
                                                          (atype.getAmmoType() == AmmoType.AmmoTypeEnum.HAG) ||
                                                          atype.countsAsFlak())));

        boolean isIndirect = weapon.hasModes() && (weapon.curMode().isIndirect());

        // BMM p. 31, semi-guided indirect missile attacks vs tagged targets ignore
        // terrain modifiers
        boolean semiGuidedIndirectVsTaggedTarget = isIndirect &&
                                                         (atype != null) &&
                                                         atype.getMunitionType()
                                                               .contains(AmmoType.Munitions.M_SEMIGUIDED) &&
                                                         Compute.isTargetTagged(target, game);

        boolean isInferno = ((atype != null) &&
                                   ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) ||
                                          (atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_IMP) ||
                                          (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML)) &&
                                   (atype.getMunitionType().contains(AmmoType.Munitions.M_INFERNO)) ||
                                   (isWeaponInfantry && (wtype.hasFlag(WeaponType.F_INFERNO))));

        boolean isArtilleryDirect = (wtype.hasFlag(WeaponType.F_ARTILLERY) ||
                                           (wtype instanceof CapitalMissileWeapon &&
                                                  Compute.isGroundToGround(ae, target))) && game.getPhase().isFiring();

        boolean isArtilleryIndirect = (wtype.hasFlag(WeaponType.F_ARTILLERY) ||
                                             (wtype instanceof CapitalMissileWeapon &&
                                                    Compute.isGroundToGround(ae, target))) &&
                                            (game.getPhase().isTargeting() || game.getPhase().isOffboard());

        boolean isBearingsOnlyMissile = (weapon.isInBearingsOnlyMode()) &&
                                              (game.getPhase().isTargeting() || game.getPhase().isFiring());

        boolean isCruiseMissile = (weapon.getType().hasFlag(WeaponType.F_CRUISE_MISSILE) ||
                                         (wtype instanceof CapitalMissileWeapon &&
                                                Compute.isGroundToGround(ae, target)));

        // hack, otherwise when actually resolves shot labeled impossible.
        boolean isArtilleryFLAK = isArtilleryDirect &&
                                        (te != null) &&
                                        Compute.isFlakAttack(ae, te) &&
                                        (atype != null) &&
                                        (usesAmmo && (atype.countsAsFlak()));

        boolean isHaywireINarced = ae.isINarcedWith(INarcPod.HAYWIRE);

        boolean isINarcGuided = false;

        // for attacks where ECM along flight path makes a difference
        boolean isECMAffected = ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition(), allECMInfo);

        // for attacks where only ECM on the target hex makes a difference
        boolean isTargetECMAffected = ComputeECM.isAffectedByECM(ae,
              target.getPosition(),
              target.getPosition(),
              allECMInfo);

        boolean isTAG = wtype.hasFlag(WeaponType.F_TAG);

        // target type checked later because its different for
        // direct/indirect (BMRr p77 on board arrow IV)
        boolean isHoming = ammo != null && ammo.isHomingAmmoInHomingMode();

        boolean bHeatSeeking = (atype != null) &&
                                     ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) ||
                                            (atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_IMP) ||
                                            (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                                            (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                                            (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP)) &&
                                     (munition.contains(AmmoType.Munitions.M_HEAT_SEEKING));

        boolean bFTL = (atype != null) &&
                             ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                                    (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                                    (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP)) &&
                             (munition.contains(AmmoType.Munitions.M_FOLLOW_THE_LEADER) &&
                                    !ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition()));

        Mounted<?> mLinker = weapon.getLinkedBy();

        boolean bApollo = ((mLinker != null) &&
                                 (mLinker.getType() instanceof MiscType) &&
                                 !mLinker.isDestroyed() &&
                                 !mLinker.isMissing() &&
                                 !mLinker.isBreached() &&
                                 mLinker.getType().hasFlag(MiscType.F_APOLLO)) &&
                                (atype != null) &&
                                (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MRM);

        boolean bArtemisV = ((mLinker != null) &&
                                   (mLinker.getType() instanceof MiscType) &&
                                   !mLinker.isDestroyed() &&
                                   !mLinker.isMissing() &&
                                   !mLinker.isBreached() &&
                                   mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V) &&
                                   !isECMAffected &&
                                   !bMekTankStealthActive &&
                                   (atype != null) &&
                                   (munition.contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)));

        if (ae.usesWeaponBays()) {
            for (WeaponMounted bayW : weapon.getBayWeapons()) {
                Mounted<?> bayWAmmo = bayW.getLinked();

                if (bayWAmmo == null) {
                    // At present, all weapons below using mLinker use ammo, so this won't be a
                    // problem
                    continue;
                }
                AmmoType bAmmo = (AmmoType) bayWAmmo.getType();

                // If we're using optional rules and firing Arrow Homing missiles from a bay...
                isHoming = bAmmo != null && bAmmo.getMunitionType().contains(AmmoType.Munitions.M_HOMING);

                // If the artillery bay is firing cruise missiles, they have some special rules
                // It is possible to combine cruise missiles and other artillery in a bay, so
                // set this to true if any of the weapons are cruise missile launchers.
                if (bayW.getType().hasFlag(WeaponType.F_CRUISE_MISSILE)) {
                    isCruiseMissile = true;
                }

                mLinker = bayW.getLinkedBy();
                bApollo = ((mLinker != null) &&
                                 (mLinker.getType() instanceof MiscType) &&
                                 !mLinker.isDestroyed() &&
                                 !mLinker.isMissing() &&
                                 !mLinker.isBreached() &&
                                 mLinker.getType().hasFlag(MiscType.F_APOLLO)) &&
                                (bAmmo != null) &&
                                (bAmmo.getAmmoType() == AmmoType.AmmoTypeEnum.MRM);

                bArtemisV = ((mLinker != null) &&
                                   (mLinker.getType() instanceof MiscType) &&
                                   !mLinker.isDestroyed() &&
                                   !mLinker.isMissing() &&
                                   !mLinker.isBreached() &&
                                   mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V) &&
                                   !isECMAffected &&
                                   !bMekTankStealthActive &&
                                   (atype != null) &&
                                   (bAmmo != null) &&
                                   (bAmmo.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)));
            }
        }

        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);

        // Set up the target's relative elevation/depth
        int targEl;

        if (te == null) {
            Hex hex = game.getHexOf(target);

            targEl = hex == null ? 0 : -hex.depth();
        } else {
            targEl = te.relHeight();
        }

        // is this attack originating from underwater
        // TODO: assuming that torpedoes are underwater attacks even if fired
        // from surface vessel, awaiting rules clarification
        // http://www.classicbattletech.com/forums/index.php/topic,48744.0.html
        boolean underWater = (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET) ||
                                   (wtype instanceof SRTWeapon) ||
                                   (wtype instanceof LRTWeapon);

        if (te != null) {
            if (!isTargetECMAffected &&
                      te.isINarcedBy(ae.getOwner().getTeam()) &&
                      (atype != null) &&
                      ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_IMP) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.NLRM)) &&
                      (munition.contains(AmmoType.Munitions.M_NARC_CAPABLE))) {
                isINarcGuided = true;
            }
        }

        // Convenience variable to test the targetable type value
        final int ttype = target.getTargetType();

        // if we're doing indirect fire, find a spotter
        Entity spotter = null;
        boolean narcSpotter = false;
        if (isIndirect && !ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
            if ((target instanceof Entity) &&
                      !isTargetECMAffected &&
                      (te != null) &&
                      (atype != null) &&
                      usesAmmo &&
                      (munition.contains(AmmoType.Munitions.M_NARC_CAPABLE) &&
                             (te.isNarcedBy(ae.getOwner().getTeam()) || te.isINarcedBy(ae.getOwner().getTeam())))) {
                spotter = te;
                narcSpotter = true;
            } else {
                spotter = Compute.findSpotter(game, ae, target);
            }
            if ((spotter == null) &&
                      (atype != null) &&
                      ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.NLRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MEK_MORTAR)) &&
                      (munition.contains(AmmoType.Munitions.M_SEMIGUIDED))) {
                for (TagInfo ti : game.getTagInfo()) {
                    if (target.getId() == ti.target.getId()) {
                        spotter = game.getEntity(ti.attackerId);
                    }
                }
            }
        }

        // EI system
        // 0 if no EI (or switched off)
        // 1 if no intervening light woods
        // 2 if intervening light woods (because target in woods + intervening
        // woods is only +1 total)
        int eistatus = 0;

        boolean mpMelevationHack = false;
        if (usesAmmo &&
                  ((wtype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) || (wtype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP)) &&
                  (atype != null) &&
                  (munition.contains(AmmoType.Munitions.M_MULTI_PURPOSE)) &&
                  (ae.getElevation() == -1) &&
                  (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)) {
            mpMelevationHack = true;
            // surface to fire
            ae.setElevation(0);
        }

        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;
        if (isIndirect && ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER) && !underWater) {
            los = new LosEffects();
            losMods = new ToHitData();
        } else if (!isIndirect || (spotter == null)) {
            if (!exchangeSwarmTarget) {
                los = LosEffects.calculateLOS(game, game.getEntity(attackerId), target);
            } else {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (oldTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLOS(game, game.getEntity(oldTarget.getId()), swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLOS(game, game.getEntity(swarmSecondaryTarget.getId()), oldTarget);
                }
            }

            if (ae.hasActiveEiCockpit()) {
                if (los.getLightWoods() > 0) {
                    eistatus = 2;
                } else {
                    eistatus = 1;
                }
            }

            if (wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT) && isIndirect) {
                los.setArcedAttack(true);
            }

            losMods = los.losModifiers(game, eistatus, underWater);
        } else {
            if (exchangeSwarmTarget) {
                // Swarm should draw LoS between targets, not attacker, since we don't want LoS to be blocked
                if (oldTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLOS(game, (Entity) oldTarget, swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLOS(game, (Entity) swarmSecondaryTarget, oldTarget);
                }
            } else {
                // For everything else, set up a plain old LOS
                los = LosEffects.calculateLOS(game, spotter, target, true);
            }

            // do not count attacker partial cover in indirect fire
            los.setAttackerCover(LosEffects.COVER_NONE);

            if (!narcSpotter && spotter.hasActiveEiCockpit()) {
                if (los.getLightWoods() > 0) {
                    eistatus = 2;
                } else {
                    eistatus = 1;
                }
            }

            if (wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT) || semiGuidedIndirectVsTaggedTarget) {
                los.setArcedAttack(true);
            }

            losMods = los.losModifiers(game, underWater);
        }
        if (mpMelevationHack) {
            // return to depth 1
            ae.setElevation(-1);
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int distance = Compute.effectiveDistance(game, ae, target);

        // Set up our initial toHit data
        ToHitData toHit = new ToHitData();

        // Check to see if this attack is impossible and return the reason code
        String reasonImpossible = ComputeToHitIsImpossible.toHitIsImpossible(game,
              ae,
              attackerId,
              target,
              ttype,
              los,
              losMods,
              toHit,
              distance,
              spotter,
              wtype,
              weapon,
              weaponId,
              atype,
              ammo,
              munition,
              isFlakAttack,
              isArtilleryDirect,
              isArtilleryFLAK,
              isArtilleryIndirect,
              isAttackerInfantry,
              isBearingsOnlyMissile,
              isCruiseMissile,
              exchangeSwarmTarget,
              isHoming,
              isInferno,
              isIndirect,
              isStrafing,
              isTAG,
              targetInBuilding,
              usesAmmo,
              underWater,
              evenIfAlreadyFired);
        if (reasonImpossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, reasonImpossible);
        }

        // Check to see if this attack is automatically successful and return the reason
        // code
        String reasonAutoHit = toHitIsAutomatic(game,
              ae,
              target,
              ttype,
              los,
              distance,
              wtype,
              weapon,
              isBearingsOnlyMissile);
        if (reasonAutoHit != null) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, reasonAutoHit);
        }

        SpecialResolutionTracker srt = new SpecialResolutionTracker();
        srt.setSpecialResolution(false);
        // Is this an infantry leg/swarm attack?
        toHit = handleInfantrySwarmAttacks(game, ae, target, ttype, toHit, wtype, srt);
        if (srt.isSpecialResolution()) {
            return toHit;
        }

        // Check to see if this attack was made with a weapon that has special to-hit
        // rules
        toHit = handleSpecialWeaponAttacks(game, ae, target, ttype, los, toHit, wtype, atype, srt);
        if (srt.isSpecialResolution()) {
            return toHit;
        }

        // This attack has now tested possible and doesn't follow any weird special
        // rules,
        // so let's start adding up the to-hit numbers

        // Start with the attacker's weapon skill
        toHit = new ToHitData(ae.getCrew().getGunnery(), Messages.getString("WeaponAttackAction.GunSkill"));
        if (game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                toHit = new ToHitData(ae.getCrew().getGunneryL(), Messages.getString("WeaponAttackAction.GunLSkill"));
            }
            if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                toHit = new ToHitData(ae.getCrew().getGunneryM(), Messages.getString("WeaponAttackAction.GunMSkill"));
            }
            if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                toHit = new ToHitData(ae.getCrew().getGunneryB(), Messages.getString("WeaponAttackAction.GunBSkill"));
            }
        }
        if (wtype.hasFlag(WeaponType.F_ARTILLERY) &&
                  game.getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            toHit = new ToHitData(ae.getCrew().getArtillery(), Messages.getString("WeaponAttackAction.ArtySkill"));
        }

        // Is this an Artillery attack?
        if (isArtilleryDirect || isArtilleryIndirect) {
            toHit = handleArtilleryAttacks(game,
                  ae,
                  target,
                  ttype,
                  losMods,
                  toHit,
                  wtype,
                  weapon,
                  atype,
                  isArtilleryDirect,
                  isArtilleryFLAK,
                  isArtilleryIndirect,
                  isHoming,
                  usesAmmo,
                  srt);
        }
        if (srt.isSpecialResolution()) {
            return toHit;
        }

        // Mine launchers have their own base to-hit, but can still be affected by
        // terrain and movement modifiers
        // thus, they don't qualify for special weapon handling
        if (BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName())) {
            toHit = new ToHitData(8, Messages.getString("WeaponAttackAction.MagMine"));
        }

        // TODO: mek making DFA could be higher if DFA target hex is higher
        // BMRr pg. 43, "attacking unit is considered to be in the air
        // above the hex, standing on an elevation 1 level higher than
        // the target hex or the elevation of the hex the attacker is
        // in, whichever is higher."
        // Ancient rules - have we implemented this per TW?

        // Store the thruBldg state, for later processing
        toHit.setThruBldg(los.getThruBldg());

        // Collect the modifiers for the environment
        toHit = ComputeEnvironmentalToHitMods.compileEnvironmentalToHitMods(game, ae, target, wtype, atype, toHit,
              isArtilleryIndirect);

        // Collect the modifiers for the crew/pilot
        toHit = ComputeAttackerToHitMods.compileCrewToHitMods(game, ae, te, toHit, weapon);

        // Collect the modifiers for the attacker's condition/actions
        if (ae != null) {
            // Conventional fighter, Aerospace and fighter LAM attackers
            if (ae.isAero()) {
                toHit = ComputeAeroAttackerToHitMods.compileAeroAttackerToHitMods(game,
                      ae,
                      target,
                      ttype,
                      toHit,
                      aimingAt,
                      aimingMode,
                      eistatus,
                      wtype,
                      weapon,
                      atype,
                      munition,
                      isArtilleryIndirect,
                      isFlakAttack,
                      isNemesisConfused,
                      isStrafing,
                      usesAmmo);
                // Everyone else
            } else {
                toHit = ComputeAttackerToHitMods.compileAttackerToHitMods(game,
                      ae,
                      target,
                      los,
                      toHit,
                      aimingAt,
                      aimingMode,
                      wtype,
                      weapon,
                      weaponId,
                      atype,
                      munition,
                      isFlakAttack,
                      isHaywireINarced,
                      isNemesisConfused,
                      isWeaponFieldGuns,
                      usesAmmo);
            }
        }

        // "hack" to cover the situation where the target is standing in a short
        // building which provides it partial cover. Unlike other partial cover
        // situations,
        // this occurs regardless of other LOS consideration.
        if (WeaponAttackAction.targetInShortCoverBuilding(target)) {
            Building currentBuilding = game.getBuildingAt(target.getPosition(), target.getBoardId()).get();

            LosEffects shortBuildingLos = new LosEffects();
            shortBuildingLos.setTargetCover(LosEffects.COVER_HORIZONTAL);
            shortBuildingLos.setDamagableCoverTypePrimary(LosEffects.DAMAGABLE_COVER_BUILDING);
            shortBuildingLos.setCoverBuildingPrimary(currentBuilding);
            shortBuildingLos.setCoverLocPrimary(target.getPosition());

            los.add(shortBuildingLos);
            toHit.append(shortBuildingLos.losModifiers(game));
        }

        // Collect the modifiers for the target's condition/actions
        toHit = ComputeTargetToHitMods.compileTargetToHitMods(game,
              ae,
              target,
              ttype,
              los,
              toHit,
              aimingAt,
              aimingMode,
              distance,
              wtype,
              weapon,
              atype,
              munition,
              isArtilleryDirect,
              isArtilleryIndirect,
              isAttackerInfantry,
              exchangeSwarmTarget,
              isIndirect,
              isPointblankShot,
              usesAmmo);

        // Collect the modifiers for terrain and line-of-sight. This includes any
        // related to-hit table changes
        toHit = ComputeTerrainMods.compileTerrainAndLosToHitMods(game,
              ae,
              target,
              ttype,
              aElev,
              tElev,
              targEl,
              distance,
              los,
              toHit,
              losMods,
              eistatus,
              wtype,
              weapon,
              weaponId,
              atype,
              ammo,
              munition,
              isAttackerInfantry,
              inSameBuilding,
              isIndirect,
              isPointblankShot,
              underWater);

        // If this is a swarm LRM secondary attack, remove old target movement and
        // terrain mods, then
        // add those for new target.
        if (exchangeSwarmTarget) {
            toHit = handleSwarmSecondaryAttacks(game,
                  ae,
                  target,
                  oldTarget,
                  swarmSecondaryTarget,
                  toHit,
                  eistatus,
                  aimingAt,
                  aimingMode,
                  weapon,
                  atype,
                  munition,
                  isECMAffected,
                  inSameBuilding,
                  underWater);
        }

        // Collect the modifiers specific to the weapon the attacker is using
        toHit = compileWeaponToHitMods(game,
              ae,
              spotter,
              target,
              ttype,
              toHit,
              wtype,
              weapon,
              atype,
              ammo,
              munition,
              isFlakAttack,
              isIndirect,
              narcSpotter);

        // Collect the modifiers specific to the ammo the attacker is using
        toHit = compileAmmoToHitMods(game,
              ae,
              target,
              ttype,
              toHit,
              wtype,
              weapon,
              atype,
              munition,
              bApollo,
              bArtemisV,
              bFTL,
              bHeatSeeking,
              isECMAffected,
              isINarcGuided);

        // okay!
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the ammunition being used Using precision AC
     * rounds that get a -1 TH bonus? You'll find that here. Bonuses related to the attacker's condition? Using a weapon
     * with a TH penalty? Those are in other methods.
     *
     * @param game          The current {@link Game}
     * @param ae            The Entity making this attack
     * @param target        The Targetable object being attacked
     * @param ttype         The targetable object type
     * @param toHit         The running total ToHitData for this WeaponAttackAction
     * @param wtype         The WeaponType of the weapon being used
     * @param weapon        The Mounted weapon being used
     * @param atype         The AmmoType being used for this attack
     * @param munition      Long indicating the munition type flag being used, if applicable
     * @param bApollo       flag that indicates whether the attacker is using an Apollo FCS for MRMs
     * @param bArtemisV     flag that indicates whether the attacker is using an Artemis V FCS
     * @param bFTL          flag that indicates whether the attacker is using FTL missiles
     * @param bHeatSeeking  flag that indicates whether the attacker is using Heat Seeking missiles
     * @param isECMAffected flag that indicates whether the target is inside an ECM bubble
     * @param isINarcGuided flag that indicates whether the target is broadcasting an iNarc beacon
     */
    private static ToHitData compileAmmoToHitMods(Game game, Entity ae, Targetable target, int ttype, ToHitData toHit,
          WeaponType wtype, Mounted<?> weapon, AmmoType atype, EnumSet<AmmoType.Munitions> munition, boolean bApollo,
          boolean bArtemisV, boolean bFTL, boolean bHeatSeeking, boolean isECMAffected, boolean isINarcGuided) {
        if (ae == null || atype == null) {
            // Can't calculate ammo mods without valid ammo and an attacker to fire it
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            // Some ammo can only target valid entities
            te = (Entity) target;
        }

        // Autocannon Munitions

        // Armor Piercing ammo is a flat +1
        if (((atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC) ||
                   (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LAC) ||
                   (atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_IMP) ||
                   (atype.getAmmoType() == AmmoType.AmmoTypeEnum.PAC)) &&
                  (munition.contains(AmmoType.Munitions.M_ARMOR_PIERCING))) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.ApAmmo"));
        }

        // Bombs

        // Air-to-air Arrow and Light Air-to-air missiles
        if (((atype.getAmmoType() == AmmoType.AmmoTypeEnum.AAA_MISSILE) || (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LAA_MISSILE)) &&
                  Compute.isAirToGround(ae, target)) {
            // +4 penalty if trying to use one against a ground target that is not flying
            // (Errata: https://bg.battletech.com/forums/index.php?topic=87401.msg2060972#msg2060972 )
            if (!target.isAirborneVTOLorWIGE()) {
                toHit.addModifier(+4, Messages.getString("WeaponAttackAction.AaaGroundAttack"));
            }
            // +3 additional if the attacker is flying at Altitude 3 or less
            if (ae.getAltitude() < 4) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.AaaLowAlt"));
            }
        }

        // Flat modifiers defined in AmmoType
        if (atype.getToHitModifier() != 0) {
            toHit.addModifier(atype.getToHitModifier(),
                  atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
        }

        // Missile Munitions
        boolean isIndirect = weapon.hasModes() && (weapon.curMode().isIndirect());

        // Apollo FCS for MRMs
        if (bApollo) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ApolloFcs"));
        }

        // add Artemis V bonus
        if (bArtemisV && !isIndirect) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ArtemisV"));
        }

        // Follow-the-leader LRMs
        if (bFTL) {
            toHit.addModifier(2, atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
        }

        // Heat Seeking Missiles
        if (bHeatSeeking) {
            Hex hexTarget = game.getHexOf(target);
            // -2 bonus if shooting at burning hexes or buildings
            if (te == null && hexTarget.containsTerrain(Terrains.FIRE)) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AmmoMod"));
            }
            if (te != null) {
                // -2 bonus if the target is on fire
                if (te.infernos.isStillBurning()) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AmmoMod"));

                }
                if ((te.isAirborne()) && (toHit.getSideTable() == ToHitData.SIDE_REAR)) {
                    // -2 bonus if shooting an Aero through the rear arc
                    toHit.addModifier(-2,
                          atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
                } else if (te.heat == 0) {
                    // +1 penalty if shooting at a non-heat-tracking unit or a heat-tracking unit at
                    // 0 heat
                    toHit.addModifier(1, atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
                } else {
                    // -1 bonus for each -1MP the target would get due to heat
                    toHit.addModifier(-te.getHeatMPReduction(),
                          atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
                }
            }

            // +2 penalty if shooting into or through a burning hex
            if (LosEffects.hasFireBetween(ae.getBoardLocation(), target.getBoardLocation(), game)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.HsmThruFire"));
            }
        }

        // Narc-capable missiles homing on an iNarc beacon
        if (isINarcGuided) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.iNarcHoming"));
        }

        // Listen-Kill ammo from War of 3039 sourcebook?
        if (!isECMAffected &&
                  ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                         (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP) ||
                         (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                         (atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) ||
                         (atype.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_IMP)) &&
                  (munition.contains(AmmoType.Munitions.M_LISTEN_KILL)) &&
                  !((te != null) && te.isClan())) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ListenKill"));
        }

        return toHit;
    }

    /**
     * Method that tests each attack to see if it would automatically hit. If so, a reason string will be returned. A
     * null return means we can continue processing the attack
     *
     * @param game                  The current {@link Game}
     * @param ae                    The Entity making this attack
     * @param target                The Targetable object being attacked
     * @param ttype                 The targetable object type
     * @param los                   The calculated LOS between attacker and target
     * @param distance              The distance in hexes from attacker to target
     * @param wtype                 The WeaponType of the weapon being used
     * @param weapon                The Mounted weapon being used
     * @param isBearingsOnlyMissile flag that indicates whether this is a bearings-only capital missile attack
     */
    private static String toHitIsAutomatic(Game game, Entity ae, Targetable target, int ttype, LosEffects los,
          int distance, WeaponType wtype, Mounted<?> weapon, boolean isBearingsOnlyMissile) {

        // Buildings

        // Attacks against adjacent buildings automatically hit.
        if ((distance == 1) &&
                  ((ttype == Targetable.TYPE_BUILDING) ||
                         (ttype == Targetable.TYPE_BLDG_IGNITE) ||
                         (ttype == Targetable.TYPE_FUEL_TANK) ||
                         (ttype == Targetable.TYPE_FUEL_TANK_IGNITE) ||
                         (target instanceof GunEmplacement))) {
            return Messages.getString("WeaponAttackAction.AdjBuilding");
        }

        // Attacks against buildings from inside automatically hit.
        if ((null != los.getThruBldg()) &&
                  ((ttype == Targetable.TYPE_BUILDING) ||
                         (ttype == Targetable.TYPE_BLDG_IGNITE) ||
                         (ttype == Targetable.TYPE_FUEL_TANK) ||
                         (ttype == Targetable.TYPE_FUEL_TANK_IGNITE) ||
                         (target instanceof GunEmplacement))) {
            return Messages.getString("WeaponAttackAction.InsideBuilding");
        }

        // Special Weapon Rules

        // B-Pod firing at infantry in the same hex autohit
        if (wtype != null &&
                  wtype.hasFlag(WeaponType.F_B_POD) &&
                  (target instanceof Infantry) &&
                  target.getPosition().equals(ae.getPosition())) {
            return Messages.getString("WeaponAttackAction.BPodAtInf");
        }

        // Capital Missiles in bearings-only mode target hexes and always hit them
        if (isBearingsOnlyMissile) {
            if (game.getPhase().isTargeting() && (distance >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM)) {
                return Messages.getString("WeaponAttackAction.BoMissileHex");
            }
        }

        // Screen launchers target hexes and hit automatically (if in range)
        if (wtype != null &&
                  ((wtype.getAmmoType() == AmmoType.AmmoTypeEnum.SCREEN_LAUNCHER) || (wtype instanceof ScreenLauncherBayWeapon)) &&
                  distance <= wtype.getExtremeRange()) {
            return Messages.getString("WeaponAttackAction.ScreenAutoHit");
        }

        // Vehicular grenade launchers
        if (weapon != null && weapon.getType().hasFlag(WeaponType.F_VGL)) {
            int facing = weapon.getFacing();
            if (ae.isSecondaryArcWeapon(ae.getEquipmentNum(weapon))) {
                facing = (facing + ae.getSecondaryFacing()) % 6;
            }
            Coords c = ae.getPosition().translated(facing);
            if ((target instanceof HexTarget) && target.getPosition().equals(c)) {
                return Messages.getString("WeaponAttackAction.Vgl");
            }
        }

        // If we get here, the shot isn't an auto-hit
        return null;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to infantry/BA swarm attacks
     *
     * @param game   The current {@link Game}
     * @param ae     The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit  The running total ToHitData for this WeaponAttackAction
     * @param wtype  The WeaponType of the weapon being used
     * @param srt    Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData handleInfantrySwarmAttacks(Game game, Entity ae, Targetable target, int ttype,
          ToHitData toHit, WeaponType wtype, SpecialResolutionTracker srt) {
        if (ae == null) {
            // *Should* be impossible at this point in the process
            return toHit;
        }
        if (target == null || ttype != Targetable.TYPE_ENTITY) {
            // Can only swarm a valid entity target
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = (Entity) target;
        // Leg attacks and Swarm attacks have their own base toHit values
        if (Infantry.LEG_ATTACK.equals(wtype.getInternalName())) {
            toHit = Compute.getLegAttackBaseToHit(ae, te, game);
            if ((te instanceof Mek) && ((Mek) te).isSuperHeavy()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMek"));
            }
            if (te.isProne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TargetProne"));
            }
            if (te.isImmobile()) {
                toHit.addModifier(-4, Messages.getString("WeaponAttackAction.TargetImmobile"));
            }
            srt.setSpecialResolution(true);
            return toHit;

        } else if (Infantry.SWARM_MEK.equals(wtype.getInternalName())) {
            toHit = Compute.getSwarmMekBaseToHit(ae, te, game);
            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                srt.setSpecialResolution(true);
                return toHit;
            }

            if (te instanceof Tank) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TeVehicle"));
            }
            if ((te instanceof Mek) && ((Mek) te).isSuperHeavy()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMek"));
            }
            if (te.isProne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TargetProne"));
            }
            if (te.isImmobile()) {
                toHit.addModifier(-4, Messages.getString("WeaponAttackAction.TargetImmobile"));
            }

            // If the defender carries mekanized BA, they can fight off the
            // swarm
            for (Entity e : te.getExternalUnits()) {
                if (e instanceof BattleArmor) {
                    BattleArmor ba = (BattleArmor) e;
                    int def = ba.getShootingStrength();
                    int att = ((Infantry) ae).getShootingStrength();
                    if (!(ae instanceof BattleArmor)) {
                        if (att >= 28) {
                            att = 5;
                        } else if (att >= 24) {
                            att = 4;
                        } else if (att >= 21) {
                            att = 3;
                        } else if (att >= 18) {
                            att = 2;
                        } else {
                            att = 1;
                        }
                    }
                    def = (def + 2) - att;
                    if (def > 0) {
                        toHit.addModifier(def, Messages.getString("WeaponAttackAction.DefendingBA"));
                    }
                }
            }
            srt.setSpecialResolution(true);
            return toHit;
        } else if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            srt.setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.EndSwarm"));
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ((ae.getSwarmTargetId() == te.getId())) {
            int side = te instanceof Tank ? ToHitData.SIDE_RANDOM : ToHitData.SIDE_FRONT;
            if (ae instanceof BattleArmor) {
                if (!Infantry.SWARM_WEAPON_MEK.equals(wtype.getInternalName()) && !(wtype instanceof InfantryAttack)) {
                    srt.setSpecialResolution(true);
                    return new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.WrongSwarmUse"));
                }
                srt.setSpecialResolution(true);
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                      Messages.getString("WeaponAttackAction.SwarmingAutoHit"),
                      ToHitData.HIT_SWARM,
                      side);
            }
            srt.setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                  Messages.getString("WeaponAttackAction.SwarmingAutoHit"),
                  ToHitData.HIT_SWARM_CONVENTIONAL,
                  side);
        }
        // If we get here, no swarm attack applies
        return toHit;
    }

    /**
     * Method to handle modifiers for swarm missile secondary targets
     *
     * @param game                 The current {@link Game}
     * @param ae                   The Entity making this attack
     * @param target               The Targetable object being attacked
     * @param swarmPrimaryTarget   The original Targetable object being attacked
     * @param swarmSecondaryTarget The current Targetable object being attacked
     * @param toHit                The running total ToHitData for this WeaponAttackAction
     * @param eistatus             An int value representing the ei cockpit/pilot upgrade status - used for terrain
     *                             calculation
     * @param aimingAt             An int value representing the location being aimed at - used for immobile target
     * @param aimingMode           An int value that determines the reason aiming is allowed - used for immobile target
     * @param weapon               The Mounted weapon being used
     * @param atype                The AmmoType being used for this attack
     * @param munition             Long indicating the munition type flag being used, if applicable
     * @param isECMAffected        flag that indicates whether the target is inside an ECM bubble
     * @param inSameBuilding       flag that indicates whether this attack originates from within the same building
     * @param underWater           flag that indicates whether the weapon being used is underwater
     */
    private static ToHitData handleSwarmSecondaryAttacks(Game game, Entity ae, Targetable target,
          Targetable swarmPrimaryTarget, Targetable swarmSecondaryTarget, ToHitData toHit, int eistatus, int aimingAt,
          AimingMode aimingMode, Mounted<?> weapon, AmmoType atype, EnumSet<AmmoType.Munitions> munition,
          boolean isECMAffected, boolean inSameBuilding, boolean underWater) {
        if (ae == null || swarmPrimaryTarget == null || swarmSecondaryTarget == null) {
            // This method won't work without these 3 things
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Remove extraneous mods
        toHit.adjustSwarmToHit();

        toHit.append(Compute.getImmobileMod(swarmSecondaryTarget, aimingAt, aimingMode));
        toHit.append(Compute.getTargetTerrainModifier(game,
              game.getTarget(swarmSecondaryTarget.getTargetType(), swarmSecondaryTarget.getId()),
              eistatus,
              inSameBuilding,
              underWater));
        toHit.setCover(LosEffects.COVER_NONE);

        Hex targHex = game.getHexOf(swarmSecondaryTarget);
        int targEl = swarmSecondaryTarget.relHeight();
        int distance = Compute.effectiveDistance(game, ae, swarmSecondaryTarget);

        // We might not attack the new target from the same side as the
        // old, so recalculate; the attack *direction* is still traced from
        // the original source.
        toHit.setSideTable(ComputeSideTable.sideTable(ae, swarmSecondaryTarget));

        // Secondary swarm LRM attacks are never called shots even if the
        // initial one was.
        if (weapon != null && weapon.getCalledShot().getCall() != CalledShot.CALLED_NONE) {
            weapon.getCalledShot().reset();
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        LosEffects swarmlos;
        // TO make it seem like the terrain modifiers should be between the
        // attacker and the secondary target, but we have received rules
        // clarifications on the old forums indicating that this is correct
        if (swarmPrimaryTarget.getTargetType() != Targetable.TYPE_ENTITY) {
            swarmlos = LosEffects.calculateLOS(game, (Entity) swarmSecondaryTarget, target);
        } else {
            swarmlos = LosEffects.calculateLOS(game, (Entity) swarmPrimaryTarget, swarmSecondaryTarget);
        }

        // reset cover
        if (swarmlos.getTargetCover() != LosEffects.COVER_NONE) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER)) {
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(swarmlos.getTargetCover());
            } else {
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(LosEffects.COVER_HORIZONTAL);
            }
        }
        // target in water?
        if (swarmSecondaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity oldEnt = game.getEntity(swarmSecondaryTarget.getId());
            toHit.append(Compute.getTargetMovementModifier(game, oldEnt.getId()));
            // target in partial water - depth 1 for most units
            int partialWaterLevel = 1;
            // Depth 2 for superheavy meks
            if ((target instanceof Mek) && ((Mek) target).isSuperHeavy()) {
                partialWaterLevel = 2;
            }
            if (targHex.containsTerrain(Terrains.WATER) &&
                      (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel) &&
                      (targEl == 0) &&
                      (oldEnt.height() > 0)) {
                toHit.setCover(toHit.getCover() | LosEffects.COVER_HORIZONTAL);
            }
            // Prone
            ToHitData proneMod = new ToHitData();
            if (oldEnt.isProne()) {
                // easier when point-blank
                if (distance <= 1) {
                    proneMod = new ToHitData(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
                } else {
                    // Harder at range.
                    proneMod = new ToHitData(1, Messages.getString("WeaponAttackAction.ProneRange"));
                }
            }
            // I-Swarm bonus
            toHit.append(proneMod);
            if (!isECMAffected &&
                      (atype != null) &&
                      !oldEnt.isEnemyOf(ae) &&
                      !(oldEnt.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD) > 0) &&
                      (munition.contains(AmmoType.Munitions.M_SWARM_I))) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.SwarmIFriendly"));
            }
        }
        return toHit;
    }

    /**
     * If you're using a weapon that does something totally special and doesn't apply mods like everything else, look
     * here
     *
     * @param game   The current {@link Game}
     * @param ae     The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param los    The calculated LOS between attacker and target
     * @param toHit  The running total ToHitData for this WeaponAttackAction
     * @param wtype  The WeaponType of the weapon being used
     * @param atype  The AmmoType being used for this attack
     * @param srt    Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData handleSpecialWeaponAttacks(Game game, Entity ae, Targetable target, int ttype,
          LosEffects los, ToHitData toHit, WeaponType wtype, AmmoType atype, SpecialResolutionTracker srt) {
        if (ae == null) {
            // *Should* be impossible at this point in the process
            return toHit;
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            // Some of these weapons only target valid entities
            te = (Entity) target;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Battle Armor bomb racks (Micro bombs) use gunnery skill and no other mods per
        // TWp228 2018 errata
        if ((atype != null) && (atype.getAmmoType() == AmmoType.AmmoTypeEnum.BA_MICRO_BOMB)) {
            if (ae.getPosition().equals(target.getPosition())) {
                toHit = new ToHitData(ae.getCrew().getGunnery(), Messages.getString("WeaponAttackAction.GunSkill"));
            } else {
                toHit = new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.OutOfRange"));
            }
            srt.setSpecialResolution(true);
            return toHit;
        }

        // Engineer's fire extinguisher has fixed to hit number,
        // Note that coolant trucks make a regular attack.
        if (wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
            toHit = new ToHitData(8, Messages.getString("WeaponAttackAction.FireExt"));
            if (((target instanceof Entity) && ((Entity) target).infernos.isStillBurning()) ||
                      ((target instanceof Tank) && ((Tank) target).isInfernoFire())) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.PutOutInferno"));
            }
            if ((target.getTargetType() == Targetable.TYPE_HEX_EXTINGUISH) &&
                      game.getBoard(target).isInfernoBurning(target.getPosition())) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.PutOutInferno"));
            }
            srt.setSpecialResolution(true);
            return toHit;
        }

        // if this is a space bombing attack then get the to hit and return
        if (wtype.hasFlag(WeaponType.F_SPACE_BOMB)) {
            if (te != null) {
                toHit = Compute.getSpaceBombBaseToHit(ae, te, game);
                srt.setSpecialResolution(true);
                return toHit;
            }
        }

        // If we get here, no special weapons apply. Return the input data and continue
        // on
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the weapon being fired Got a heavy large laser
     * that gets a +1 TH penalty? You'll find that here. Bonuses related to the attacker's condition? Ammunition being
     * used? Those are in other methods.
     *
     * @param game         The current {@link Game}
     * @param ae           The attacking entity
     * @param spotter      The spotting entity, if using indirect fire
     * @param target       The Targetable object being attacked
     * @param ttype        The Targetable object type
     * @param toHit        The running total ToHitData for this WeaponAttackAction
     * @param wtype        The WeaponType of the weapon being used
     * @param weapon       The Mounted weapon being used for this attack
     * @param atype        The AmmoType being used for this attack
     * @param munition     Long indicating the munition type flag being used, if applicable
     * @param isFlakAttack flag that indicates whether the attacker is using Flak against an airborne target
     * @param isIndirect   flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param narcSpotter  flag that indicates whether this spotting entity is using NARC equipment
     */
    private static ToHitData compileWeaponToHitMods(Game game, Entity ae, Entity spotter, Targetable target, int ttype,
          ToHitData toHit, WeaponType wtype, Mounted<?> weapon, AmmoType atype, Mounted<?> ammo,
          EnumSet<AmmoType.Munitions> munition, boolean isFlakAttack, boolean isIndirect, boolean narcSpotter) {
        if (ae == null || wtype == null || weapon == null) {
            // Can't calculate weapon mods without a valid weapon and an attacker to fire it
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            // Some of these weapons only target valid entities
            te = (Entity) target;
        }

        // +4 for trying to fire ASEW or antiship missile at a target of < 500 tons
        if ((wtype.hasFlag(WeaponType.F_ANTI_SHIP) || wtype.getAmmoType() == AmmoType.AmmoTypeEnum.ASEW_MISSILE) &&
                  (te != null) &&
                  (te.getWeight() < 500)) {
            toHit.addModifier(4, Messages.getString("WeaponAttackAction.TeTooSmallForASM"));
        }

        // AAA mode makes targeting large craft more difficult
        if (weapon.hasModes() &&
                  weapon.curMode().equals(Weapon.MODE_CAP_LASER_AAA) &&
                  te != null &&
                  te.isLargeCraft()) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AAALaserAtShip"));
        }

        // Bombast Lasers
        if (wtype instanceof ISBombastLaser) {
            double damage = Compute.dialDownDamage(weapon, wtype);
            damage = Math.ceil((damage - 7) / 2);

            if (damage > 0) {
                toHit.addModifier((int) damage, Messages.getString("WeaponAttackAction.WeaponMod"));
            }
        }

        // Bracketing modes
        if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_80)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.Bracket80"));
        }
        if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_60)) {
            toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Bracket60"));
        }
        if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_40)) {
            toHit.addModifier(-3, Messages.getString("WeaponAttackAction.Bracket40"));
        }

        // Capital ship mass driver penalty. YOU try hitting a maneuvering target with a
        // spinal-mount weapon!
        if (wtype.hasFlag(WeaponType.F_MASS_DRIVER)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.MassDriver"));
        }

        // Capital missiles in waypoint launch mode
        if (weapon.isInWaypointLaunchMode()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.WaypointLaunch"));
        }

        // Capital weapon (except missiles) penalties at small targets
        if (wtype.isCapital() &&
                  (wtype.getAtClass() != WeaponType.CLASS_CAPITAL_MISSILE) &&
                  (wtype.getAtClass() != WeaponType.CLASS_AR10) &&
                  te != null &&
                  !te.isLargeCraft()) {
            // Capital Lasers have an AAA mode for shooting at small targets
            int aaaMod = 0;
            if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAP_LASER_AAA)) {
                aaaMod = 2;
            }
            if (wtype.isSubCapital()) {
                toHit.addModifier(3 - aaaMod, Messages.getString("WeaponAttackAction.SubCapSmallTe"));
            } else {
                toHit.addModifier(5 - aaaMod, Messages.getString("WeaponAttackAction.CapSmallTe"));
            }
        }

        // Check whether we're eligible for a flak bonus...
        if (isFlakAttack) {
            // ...and if so, which one (HAGs get an extra -1 as per TW p. 136
            // that's not covered by anything else).
            if (atype != null && atype.getAmmoType() == AmmoType.AmmoTypeEnum.HAG) {
                toHit.addModifier(-3, Messages.getString("WeaponAttackAction.HagFlak"));
            } else {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Flak"));
            }
        }

        // Flat to hit modifiers defined in WeaponType
        if (wtype.getToHitModifier(weapon) != 0) {
            int modifier = wtype.getToHitModifier(weapon);
            if (wtype instanceof VariableSpeedPulseLaserWeapon) {
                int nRange = ae.getPosition().distance(target.getPosition());
                int[] nRanges = wtype.getRanges(weapon, ammo);

                if (nRange <= nRanges[RangeType.RANGE_SHORT]) {
                    modifier += RangeType.RANGE_SHORT;
                } else if (nRange <= nRanges[RangeType.RANGE_MEDIUM]) {
                    modifier += RangeType.RANGE_MEDIUM;
                } else {
                    modifier += RangeType.RANGE_LONG;
                }
            }
            toHit.addModifier(modifier, Messages.getString("WeaponAttackAction.WeaponMod"));
        }

        // Indirect fire (LRMs, mortars and the like) has a +1 mod
        if (isIndirect) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.Indirect"));
            // Unless the attacker has the Oblique Attacker SPA
            if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ObliqueAttacker"));
            }
        }

        // Indirect fire suffers a +1 penalty if the spotter is making attacks of its
        // own
        if (isIndirect) {
            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null) &&
                      ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.NLRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MEK_MORTAR)) &&
                      (munition.contains(AmmoType.Munitions.M_SEMIGUIDED))) {

                if (Compute.isTargetTagged(target, game)) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SemiGuidedIndirect"));
                }
            } else if (!narcSpotter && (spotter != null)) {
                // Unless the target has been tagged, or the spotter has an active command
                // console
                toHit.append(Compute.getSpotterMovementModifier(game, spotter.getId()));
                if (spotter.isAttackingThisTurn() &&
                          !spotter.getCrew().hasActiveCommandConsole() &&
                          !Compute.isTargetTagged(target, game)) {
                    toHit.addModifier(1, Messages.getString("WeaponAttackAction.SpotterAttacking"));
                }
            }
        }

        // And if this is a Mek Mortar
        if (wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT)) {
            if (isIndirect) {
                // +2 penalty if there's no spotting entity
                if (spotter == null) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.NoSpotter"));
                }
            } else {
                // +3 penalty for a direct-fire shot
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.DirectMortar"));
            }
        }

        // +1 to hit if the Kinder Rapid-Fire ACs optional rule is turned on, but only
        // Jams on a 2.
        // See TacOps Autocannons for the rest of the rules
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC) &&
                  weapon.curMode().equals(Weapon.MODE_AC_RAPID)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AcRapid"));
        }

        // VSP Lasers
        // Quirks and SPAs now handled in toHit

        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to artillery attacks
     *
     * @param game                The current {@link Game}
     * @param ae                  The Entity making this attack
     * @param target              The Targetable object being attacked
     * @param ttype               The targetable object type
     * @param toHit               The running total ToHitData for this WeaponAttackAction
     * @param wtype               The WeaponType of the weapon being used
     * @param weapon              The Mounted weapon being used
     * @param atype               The AmmoType being used for this attack
     * @param isArtilleryDirect   flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryFLAK     flag that indicates whether this is a flak artillery attack
     * @param isArtilleryIndirect flag that indicates whether this is an indirect-fire artillery attack
     * @param isHoming            flag that indicates whether this is a homing missile/copperhead shot
     * @param usesAmmo            flag that indicates if the WeaponType being used is ammo-fed
     * @param srt                 Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData handleArtilleryAttacks(Game game, Entity ae, Targetable target, int ttype,
          ToHitData losMods, ToHitData toHit, WeaponType wtype, WeaponMounted weapon, AmmoType atype,
          boolean isArtilleryDirect, boolean isArtilleryFLAK, boolean isArtilleryIndirect, boolean isHoming,
          boolean usesAmmo, SpecialResolutionTracker srt) {
        if ((target instanceof HexTarget hexTarget) && !game.isOnGroundMap(hexTarget)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.ArtyCanOnlyHitGroundHexes"));
        }
        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }

        // Homing warheads just need a flat 4 to seek out a successful TAG
        if (isHoming) {
            srt.setSpecialResolution(true);
            String msg = Messages.getString("WeaponAttackAction.HomingArty");
            return new ToHitData(4, msg);
        }

        // Don't bother adding up modifiers if the target hex has been hit before
        if (game.getEntity(ae.getId()).getOwner().getArtyAutoHitHexes().contains(target.getBoardLocation())
                  && !isArtilleryFLAK) {
            srt.setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.ArtyDesTarget"));
        }

        // If we're not skipping To-Hit calculations, ensure that we have a toHit
        // instance
        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            return artilleryDirectToHit(game,
                  ae,
                  target,
                  ttype,
                  losMods,
                  toHit,
                  wtype,
                  weapon,
                  atype,
                  isArtilleryFLAK,
                  usesAmmo,
                  srt);
        } else if (isArtilleryIndirect) {
            // And now for indirect artillery fire; process quirks and SPAs here or they'll
            // be missed
            // Quirks
            ComputeAbilityMods.processAttackerQuirks(toHit, ae, te, weapon);

            // SPAs
            ComputeAbilityMods.processAttackerSPAs(toHit, ae, te, weapon, game);
            ComputeAbilityMods.processDefenderSPAs(toHit, ae, te, weapon, game);

            return artilleryIndirectToHit(ae, target, toHit, wtype, weapon, srt);
        }

        // If we get here, this isn't an artillery attack
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to direct artillery attacks
     *
     * @param game            The current {@link Game}
     * @param ae              The Entity making this attack
     * @param target          The Targetable object being attacked
     * @param ttype           The targetable object type
     * @param toHit           The running total ToHitData for this WeaponAttackAction
     * @param wtype           The WeaponType of the weapon being used
     * @param weapon          The Mounted weapon being used
     * @param atype           The AmmoType being used for this attack
     * @param isArtilleryFLAK flag that indicates whether this is a flak artillery attack
     * @param usesAmmo        flag that indicates if the WeaponType being used is ammo-fed
     * @param srt             Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData artilleryDirectToHit(Game game, Entity ae, Targetable target, int ttype, ToHitData losMods,
          ToHitData toHit, WeaponType wtype, WeaponMounted weapon, AmmoType atype, boolean isArtilleryFLAK,
          boolean usesAmmo, SpecialResolutionTracker srt) {

        if (null == atype) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "No ammo type!");
        }
        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
            // Air-Defense Arrow missiles use a simplified to-hit calculation because they:
            // A) are Flak; B) are not Artillery, C) use three ranges (same low-alt hex, 1
            // hex, 2 hexes)
            // as S/M/L
            // Per TO:AR 6th printing, p153, ADA Missiles should use TW Flak rules rather
            // than TO Artillery Flak rules.
            // Per TW pg 114, all other mods _should_ be included.

            // Special range calc
            int distance = ae.getPosition().distance(target.getPosition());
            toHit.addModifier(Compute.getADARangeModifier(distance),
                  Messages.getString("WeaponAttackAction.ADARangeBracket"));

            // Return without SRT set so that regular to-hit mods get applied.
            return toHit;
        }

        // ADA has its to-hit mods calculated separately; handle other direct artillery
        // quirk and SPA mods here:
        // Quirks
        ComputeAbilityMods.processAttackerQuirks(toHit, ae, te, weapon);

        // SPAs
        ComputeAbilityMods.processAttackerSPAs(toHit, ae, te, weapon, game);
        ComputeAbilityMods.processDefenderSPAs(toHit, ae, te, weapon, game);

        // If an airborne unit occupies the target hex, standard artillery ammo makes a
        // flak attack against it
        // TN is a flat 3 + the altitude mod + the attacker's weapon skill - 2 for Flak
        // Grounded/destroyed/landed/wrecked ASF/VTOL/WiGE should be treated as normal.
        if ((isArtilleryFLAK || (atype.countsAsFlak())) && te != null) {
            if (te.isAirborne() || te.isAirborneVTOLorWIGE()) {
                srt.setSpecialResolution(true);
                if (losMods.cannotSucceed()) {
                    // Direct fire requires LOS
                    toHit.addModifier(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.FlakIndirect"));
                    return toHit;
                }
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.ArtyFlak"));
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Flak"));
                if (te.getAltitude() > 3) {
                    if (te.getAltitude() > 9) {
                        toHit.addModifier(3, Messages.getString("WeaponAttackAction.AeroTeAlt10"));
                    } else if (te.getAltitude() > 6) {
                        toHit.addModifier(2, Messages.getString("WeaponAttackAction.AeroTeAlt79"));
                    } else if (te.getAltitude() > 3) {
                        toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeroTeAlt46"));
                    }
                }
                return toHit;
            }
        }

        // All other direct fire artillery attacks
        toHit.addModifier(4, Messages.getString("WeaponAttackAction.DirectArty"));
        toHit.append(Compute.getAttackerMovementModifier(game, ae.getId()));
        // without LOS, it is a short-range indirect attack that ignores LOS modifiers, TO:AR p.153
        if (!losMods.cannotSucceed()) {
            toHit.append(losMods);
        }
        toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        // actuator & sensor damage to attacker
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
        }
        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }
        // weapon to-hit modifier
        if (wtype.getToHitModifier(weapon) != 0) {
            toHit.addModifier(wtype.getToHitModifier(weapon), Messages.getString("WeaponAttackAction.WeaponMod"));
        }
        // ammo to-hit modifier
        if (usesAmmo && (atype.getToHitModifier() != 0)) {
            toHit.addModifier(atype.getToHitModifier(),
                  atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
        }
        srt.setSpecialResolution(true);
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to indirect artillery attacks
     *
     * @param ae     The Entity making this attack
     * @param target The Targetable object being attacked
     * @param toHit  The running total ToHitData for this WeaponAttackAction
     * @param wtype  The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param srt    Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData artilleryIndirectToHit(Entity ae, Targetable target, ToHitData toHit, WeaponType wtype,
          Mounted<?> weapon, SpecialResolutionTracker srt) {

        // See MegaMek/megamek#5168
        int mod = (ae.getPosition().distance(target.getPosition()) <= 17) ? 4 : 7;
        if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
            mod--;
        }
        toHit.addModifier(mod, Messages.getString("WeaponAttackAction.IndirectArty"));
        int adjust = 0;
        if (weapon != null) {
            adjust = ae.aTracker.getModifier(weapon, target.getPosition());
        }
        boolean spotterIsForwardObserver = ae.aTracker.getSpotterHasForwardObs();
        if (adjust == TargetRoll.AUTOMATIC_SUCCESS) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "Artillery firing at target that's been hit before.");
        } else if (adjust != 0) {
            toHit.addModifier(adjust, Messages.getString("WeaponAttackAction.AdjustedFire"));
            if (spotterIsForwardObserver) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.FooSpotter"));
            }
        }
        // Capital missiles used for surface-to-surface artillery attacks
        // See SO p110
        // Start with a flat +2 modifier
        if (wtype instanceof CapitalMissileWeapon && Compute.isGroundToGround(ae, target)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.SubCapArtillery"));
            // +3 additional modifier if fired underwater
            if (ae.isUnderwater()) {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.SubCapUnderwater"));
            }
            // +1 modifier if attacker cruised/walked
            if (ae.moved == EntityMovementType.MOVE_WALK) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.Walked"));
            } else if (ae.moved == EntityMovementType.MOVE_RUN) {
                // +2 modifier if attacker ran
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.Ran"));
            }
        } else if (ae.isAirborne()) {
            if (ae.getAltitude() > 6) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.Altitude"));
            } else if (ae.getAltitude() > 3) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Altitude"));
            }
        }
        srt.setSpecialResolution(true);
        return toHit;
    }

    private ComputeToHit() { }
}
