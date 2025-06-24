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
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;

import java.util.EnumSet;

class ComputeTargetToHitMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the defender's condition and actions -4 for
     * shooting at an immobile target? You'll find that here. Attacker strafing? Using a weapon with a TH penalty? Those
     * are in other methods. For simplicity's sake, Quirks and SPAs now get applied here for general cases (elsewhere
     * for Artillery or ADA)
     *
     * @param game                The current {@link Game}
     * @param ae                  The Entity making this attack
     * @param target              The Targetable object being attacked
     * @param ttype               The targetable object type
     * @param los                 The calculated LOS between attacker and target
     * @param toHit               The running total ToHitData for this WeaponAttackAction
     * @param aimingAt            An int value representing the location being aimed at - used by immobile target
     *                            calculations
     * @param aimingMode          An int value that determines the reason aiming is allowed - used by immobile target
     *                            calculations
     * @param distance            The distance in hexes from attacker to target
     * @param wtype               The WeaponType of the weapon being used
     * @param weapon              The Mounted weapon being used
     * @param atype               The AmmoType being used for this attack
     * @param munition            Long indicating the munition type flag being used, if applicable
     * @param isArtilleryDirect   flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryIndirect flag that indicates whether this is an indirect-fire artillery attack
     * @param isAttackerInfantry  flag that indicates whether the attacker is an infantry/BA unit
     * @param exchangeSwarmTarget flag that indicates whether this is the secondary target of Swarm LRMs
     * @param isIndirect          flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param isPointBlankShot    flag that indicates whether or not this is a PBS by a hidden unit
     * @param usesAmmo            flag that indicates whether or not the WeaponType being used is ammo-fed
     */
    static ToHitData compileTargetToHitMods(Game game, Entity ae, Targetable target, int ttype, LosEffects los,
          ToHitData toHit, int aimingAt, AimingMode aimingMode, int distance, WeaponType wtype, WeaponMounted weapon,
          AmmoType atype, EnumSet<AmmoType.Munitions> munition, boolean isArtilleryDirect, boolean isArtilleryIndirect,
          boolean isAttackerInfantry, boolean exchangeSwarmTarget, boolean isIndirect, boolean isPointBlankShot,
          boolean usesAmmo) {
        if (ae == null || target == null) {
            // Can't handle these attacks without a valid attacker and target
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            // Some weapons only target valid entities
            te = (Entity) target;
        }

        // Modifiers related to a special action the target is taking

        // evading bonuses
        if ((te != null) && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), Messages.getString("WeaponAttackAction.TeEvading"));
        }

        // Infantry taking cover per TacOps special rules
        if ((te instanceof Infantry) && ((Infantry) te).isTakingCover()) {
            if (te.getPosition().direction(ae.getPosition()) == te.getFacing()) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // target prone
        ToHitData proneMod = null;
        if ((te != null) && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // TW, pg. 221: Swarm Mek attacks apply prone/immobile mods as normal.
                proneMod = new ToHitData(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
            } else {
                // Harder at range.
                proneMod = new ToHitData(1, Messages.getString("WeaponAttackAction.ProneRange"));
            }
        }
        if (proneMod != null) {
            toHit.append(proneMod);
        }

        // Special effects affecting the target

        // Target grappled?
        if (te != null) {
            int grapple = te.getGrappled();
            if (grapple != Entity.NONE) {
                // -4 bonus if attacking the entity you're grappling
                if ((grapple == ae.getId()) && (te.getGrappleSide() == Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-4, Messages.getString("WeaponAttackAction.Grappled"));
                    // -2 bonus if grappling the target at range with a chain whip
                } else if ((grapple == ae.getId()) && (te.getGrappleSide() != Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GrappledByChain"));
                    // +1 penalty if firing at a target grappled by another unit. This does not
                    // apply to Swarm LRMs
                } else if (!exchangeSwarmTarget) {
                    toHit.addModifier(1, Messages.getString("WeaponAttackAction.FireIntoMelee"));
                } else {
                    // this -1 cancels the original +1
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.FriendlyFire"));
                }
            }
        }

        // Special Equipment and Quirks that the target possesses

        // ECM suite generating Ghost Targets
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET) &&
                  !isIndirect &&
                  !isArtilleryIndirect &&
                  !isArtilleryDirect) {
            int ghostTargetMod = Compute.getGhostTargetNumber(ae, ae.getPosition(), target.getPosition());
            if ((ghostTargetMod > -1) && !ae.isConventionalInfantry()) {
                int bapMod = 0;
                if (ae.hasBAP()) {
                    bapMod = 1;
                }
                int tcMod = 0;
                if (ae.hasTargComp() &&
                          wtype != null &&
                          wtype.hasFlag(WeaponType.F_DIRECT_FIRE) &&
                          !wtype.hasFlag(WeaponType.F_CWS) &&
                          !wtype.hasFlag(WeaponType.F_TASER) &&
                          (atype != null) &&
                          (!usesAmmo ||
                                 !(((atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX) ||
                                          (atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX_THB)) &&
                                         (munition.contains(AmmoType.Munitions.M_CLUSTER))))) {
                    tcMod = 2;
                }
                int ghostTargetMoF = (ae.getCrew().getSensorOps() + ghostTargetMod) -
                                           (ae.getGhostTargetOverride() + bapMod + tcMod);
                if (ghostTargetMoF > 1) {
                    // according to this rules clarification the +4 max is on
                    // the PSR not on the to-hit roll
                    // http://www.classicbattletech.com/forums/index.php?topic=66036.0
                    // unofficial rule to cap the ghost target to-hit penalty
                    int mod = ghostTargetMoF / 2;
                    if (game.getOptions().intOption(OptionsConstants.ADVANCED_GHOST_TARGET_MAX) > 0) {
                        mod = Math.min(mod, game.getOptions().intOption(OptionsConstants.ADVANCED_GHOST_TARGET_MAX));
                    }
                    toHit.addModifier(mod, Messages.getString("WeaponAttackAction.GhostTargets"));
                }
            }
        }

        // Movement and Position modifiers

        // target movement - ignore for pointblank shots from hidden units
        if ((te != null) && !isPointBlankShot) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game, target.getId());
            toHit.append(thTemp);

            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null) &&
                      ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.NLRM) ||
                             (atype.getAmmoType() == AmmoType.AmmoTypeEnum.MEK_MORTAR)) &&
                      (munition.contains(AmmoType.Munitions.M_SEMIGUIDED)) &&
                      (te.getTaggedBy() != WeaponAttackAction.UNASSIGNED)) {
                int nAdjust = thTemp.getValue();
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, Messages.getString("WeaponAttackAction.SemiGuidedTag")));
                }
            }
            // precision ammo reduces this modifier
            else if ((atype != null) &&
                           ((atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC) ||
                                  (atype.getAmmoType() == AmmoType.AmmoTypeEnum.LAC) ||
                                  (atype.getAmmoType() == AmmoType.AmmoTypeEnum.AC_IMP) ||
                                  (atype.getAmmoType() == AmmoType.AmmoTypeEnum.PAC)) &&
                           (munition.contains(AmmoType.Munitions.M_PRECISION))) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, Messages.getString("WeaponAttackAction.Precision")));
                }
            }
        }

        // Ground-to-air attacks against a target flying at NOE
        if (Compute.isGroundToAir(ae, target) && (null != te) && te.isNOE()) {
            if (te.passedWithin(ae.getPosition(), 1)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.TeNoe"));
            } else {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.TeNoe"));
            }
        }

        // Ground-to-air attacks against a target flying at any other altitude (if
        // StratOps Velocity mods are on)
        if (Compute.isGroundToAir(ae, target) &&
                  game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_AA_FIRE) &&
                  (null != te) &&
                  (te.isAero())) {
            int vMod = ((IAero) te).getCurrentVelocity();
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AA_MOVE_MOD)) {
                vMod = Math.min(vMod / 2, 4);
            }
            toHit.addModifier(vMod, Messages.getString("WeaponAttackAction.TeVelocity"));
        }

        // target immobile
        boolean mekMortarMunitionsIgnoreImmobile = wtype != null &&
                                                         wtype.hasFlag(WeaponType.F_MEK_MORTAR) &&
                                                         (atype != null) &&
                                                         (munition.contains(AmmoType.Munitions.M_AIRBURST));
        if (wtype != null && !(wtype instanceof ArtilleryCannonWeapon) && !mekMortarMunitionsIgnoreImmobile) {
            ToHitData immobileMod;
            // grounded dropships are treated as immobile as well for purpose of
            // the mods
            if ((null != te) && !te.isAirborne() && !te.isSpaceborne() && (te instanceof Dropship)) {
                immobileMod = new ToHitData(-4, Messages.getString("WeaponAttackAction.ImmobileDs"));
            } else {
                if (Compute.allowAimedShotWith(weapon, aimingMode)) {
                    immobileMod = Compute.getImmobileMod(target, aimingAt, aimingMode);
                } else {
                    immobileMod = Compute.getImmobileMod(target, aimingAt, AimingMode.NONE);
                }
            }

            if (immobileMod != null) {
                toHit.append(immobileMod);
            }
        }

        // Unit-specific modifiers

        // -1 to hit a SuperHeavy mek
        if ((te instanceof Mek) && ((Mek) te).isSuperHeavy()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMek"));
        }

        // large support tanks get a -1 per TW
        if ((te != null) &&
                  (te.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) &&
                  !te.isAirborne() &&
                  !te.isSpaceborne()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeLargeSupportUnit"));
        }

        // "grounded small craft" get a -1 per TW
        if ((te instanceof SmallCraft) &&
                  (te.getUnitType() == UnitType.SMALL_CRAFT) &&
                  !te.isAirborne() &&
                  !te.isSpaceborne()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeGroundedSmallCraft"));
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (!isAttackerInfantry && (te instanceof BattleArmor)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.BaTarget"));
        }

        if ((te instanceof Infantry) && te.isConventionalInfantry()) {
            // infantry squads are also hard to hit
            if (((Infantry) te).isSquad()) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.SquadTarget"));
            }
            InfantryMount mount = ((Infantry) te).getMount();
            if ((mount != null) && (mount.getSize().toHitMod != 0)) {
                toHit.addModifier(mount.getSize().toHitMod, Messages.getString("WeaponAttackAction.MountSize"));
            }
        }

        // pl-masc makes foot infantry harder to hit - IntOps p.84
        if ((te instanceof Infantry) &&
                  te.hasAbility(OptionsConstants.MD_PL_MASC) &&
                  te.getMovementMode().isLegInfantry() &&
                  te.isConventionalInfantry()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.PlMasc"));
        }

        // Ejected MekWarriors are harder to hit
        if (te instanceof MekWarrior) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.MwTarget"));
        }

        // Aerospace target modifiers
        if (te != null && te.isAero() && te.isAirborne()) {
            IAero a = (IAero) te;

            // is the target at zero velocity
            if ((a.getCurrentVelocity() == 0) && !(a.isSpheroid() && !a.isSpaceborne())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.ImmobileAero"));
            }

            // get mods for direction of attack
            if (!(a.isSpheroid() && !a.isSpaceborne())) {
                int side = ComputeSideTable.sideTable(ae, te);

                // +1 if shooting at an aero approaching nose-on
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeroNoseAttack"));
                }
                // +2 if shooting at the side as it flashes by
                if ((side == ToHitData.SIDE_LEFT) || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeroSideAttack"));
                }
            }

            // Target hidden in the sensor shadow of a larger spacecraft
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW) &&
                      a.isSpaceborne()) {
                for (Entity en : Compute.getAdjacentEntitiesAlongAttack(ae.getPosition(), target.getPosition(), game)) {
                    if (!en.isEnemyOf(te) &&
                              en.isLargeCraft() &&
                              ((en.getWeight() - te.getWeight()) >= -WeaponAttackAction.STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
                for (Entity en : game.getEntitiesVector(target.getPosition())) {
                    if (!en.isEnemyOf(te) &&
                              en.isLargeCraft() &&
                              !en.equals((Entity) a) &&
                              ((en.getWeight() - te.getWeight()) >= -WeaponAttackAction.STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
            }
        }

        // Quirks
        ComputeAbilityMods.processAttackerQuirks(toHit, ae, te, weapon);

        // SPAs
        ComputeAbilityMods.processAttackerSPAs(toHit, ae, te, weapon, game);
        ComputeAbilityMods.processDefenderSPAs(toHit, ae, te, weapon, game);

        return toHit;
    }
}
