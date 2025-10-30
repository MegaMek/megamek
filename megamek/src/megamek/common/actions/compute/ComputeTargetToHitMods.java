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
package megamek.common.actions.compute;

import static megamek.common.equipment.AmmoType.AmmoTypeEnum.*;

import java.util.EnumSet;

import megamek.client.ui.Messages;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeSideTable;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;

public class ComputeTargetToHitMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the defender's condition and actions -4 for
     * shooting at an immobile target? You'll find that here. Attacker strafing? Using a weapon with a TH penalty? Those
     * are in other methods. For simplicity's sake, Quirks and SPAs now get applied here for general cases (elsewhere
     * for Artillery or ADA)
     *
     * @param game                The current {@link Game}
     * @param attacker            The Entity making this attack
     * @param target              The Targetable object being attacked
     * @param toHit               The running total ToHitData for this WeaponAttackAction
     * @param aimingAt            An int value representing the location being aimed at - used by immobile target
     *                            calculations
     * @param aimingMode          An int value that determines the reason aiming is allowed - used by immobile target
     *                            calculations
     * @param distance            The distance in hexes from attacker to target
     * @param weaponType          The WeaponType of the weapon being used
     * @param weapon              The Mounted weapon being used
     * @param ammoType            The AmmoType being used for this attack
     * @param munition            Long indicating the munition type flag being used, if applicable
     * @param isArtilleryDirect   flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryIndirect flag that indicates whether this is an indirect-fire artillery attack
     * @param isAttackerInfantry  flag that indicates whether the attacker is an infantry/BA unit
     * @param exchangeSwarmTarget flag that indicates whether this is the secondary target of Swarm LRMs
     * @param isIndirect          flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param isPointBlankShot    flag that indicates whether this is a PBS by a hidden unit
     * @param usesAmmo            flag that indicates whether the WeaponType being used is ammo-fed
     */
    public static ToHitData compileTargetToHitMods(Game game, Entity attacker, Targetable target,
          ToHitData toHit, int aimingAt, AimingMode aimingMode, int distance, WeaponType weaponType,
          WeaponMounted weapon, AmmoType ammoType, EnumSet<AmmoType.Munitions> munition, boolean isArtilleryDirect,
          boolean isArtilleryIndirect, boolean isAttackerInfantry, boolean exchangeSwarmTarget, boolean isIndirect,
          boolean isPointBlankShot, boolean usesAmmo) {

        if (attacker == null || target == null) {
            // Can't handle these attacks without a valid attacker and target
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity entityTarget = (target instanceof Entity entity) ? entity : null;

        // Modifiers related to a special action the target is taking

        // evading bonuses
        if ((entityTarget != null) && entityTarget.isEvading()) {
            toHit.addModifier(entityTarget.getEvasionBonus(), Messages.getString("WeaponAttackAction.TeEvading"));
        }

        // Infantry taking cover per TacOps special rules
        if ((entityTarget instanceof Infantry infantry) && infantry.isTakingCover()) {
            if (entityTarget.getPosition().direction(attacker.getPosition()) == entityTarget.getFacing()) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // target prone
        ToHitData proneMod = null;
        if ((entityTarget != null) && entityTarget.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // TW, p.221: Swarm Mek attacks apply prone/immobile mods as normal
                proneMod = new ToHitData(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
            } else {
                // Harder at range
                proneMod = new ToHitData(1, Messages.getString("WeaponAttackAction.ProneRange"));
            }
        }
        if (proneMod != null) {
            toHit.append(proneMod);
        }

        // Special effects affecting the target

        // Target grappled?
        if (entityTarget != null) {
            int grapple = entityTarget.getGrappled();
            if (grapple != Entity.NONE) {
                // -4 bonus if attacking the entity you're grappling
                if ((grapple == attacker.getId()) && (entityTarget.getGrappleSide() == Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-4, Messages.getString("WeaponAttackAction.Grappled"));
                    // -2 bonus if grappling the target at range with a chain whip
                } else if ((grapple == attacker.getId()) && (entityTarget.getGrappleSide() != Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GrappledByChain"));
                    // +1 penalty if firing at a target grappled by another unit. This does not apply to Swarm LRMs
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
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET)
              && !isIndirect
              && !isArtilleryIndirect
              && !isArtilleryDirect) {

            int ghostTargetMod = Compute.getGhostTargetNumber(attacker, attacker.getPosition(), target.getPosition());
            if ((ghostTargetMod > -1) && !attacker.isConventionalInfantry()) {
                int bapMod = 0;
                if (attacker.hasBAP()) {
                    bapMod = 1;
                }
                int tcMod = 0;
                boolean isLBX = (ammoType != null)
                      && ammoType.getAmmoType().isAnyOf(AC_LBX, AC_LBX_THB)
                      && munition.contains(AmmoType.Munitions.M_CLUSTER);

                if (attacker.hasTargComp()
                      && (weaponType != null)
                      && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
                      && !weaponType.hasAnyFlag(WeaponType.F_CWS, WeaponType.F_TASER)
                      && (ammoType != null)
                      && !(usesAmmo && isLBX)) {
                    tcMod = 2;
                }
                int ghostTargetMoF = (attacker.getCrew().getSensorOps() + ghostTargetMod) -
                      (attacker.getGhostTargetOverride() + bapMod + tcMod);
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
        if ((entityTarget != null) && !isPointBlankShot) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game, target.getId());
            toHit.append(thTemp);

            // semi-guided ammo negates this modifier, if TAG succeeded
            if ((ammoType != null)
                  && ammoType.getAmmoType().isAnyOf(LRM, LRM_IMP, MML, NLRM, MEK_MORTAR)
                  && munition.contains(AmmoType.Munitions.M_SEMIGUIDED)
                  && (entityTarget.getTaggedBy() != WeaponAttackAction.UNASSIGNED)) {
                int nAdjust = thTemp.getValue();
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, Messages.getString("WeaponAttackAction.SemiGuidedTag")));
                }
            }

            // precision ammo reduces this modifier
            // PLAYTEST3 for playtest ammo
            else if ((ammoType != null)
                  && ammoType.getAmmoType().isAnyOf(AC, LAC, AC_IMP, PAC)
                  && (munition.contains(AmmoType.Munitions.M_PRECISION) || munition.contains(AmmoType.Munitions.M_PRECISION_PLAYTEST))) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, Messages.getString("WeaponAttackAction.Precision")));
                }
            }
        }

        // Ground-to-air attacks against a target flying at NOE
        if (Compute.isGroundToAir(attacker, target) && (null != entityTarget) && entityTarget.isNOE()) {
            if (entityTarget.passedWithin(attacker.getPosition(), 1)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.TeNoe"));
            } else {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.TeNoe"));
            }
        }

        // Ground-to-air attacks against a target flying at any other altitude (if
        // StratOps Velocity mods are on)
        if (Compute.isGroundToAir(attacker, target)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AA_FIRE)
              && (null != entityTarget)
              && entityTarget.isAero()) {
            int vMod = ((IAero) entityTarget).getCurrentVelocity();
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AA_MOVE_MOD)) {
                vMod = Math.min(vMod / 2, 4);
            }
            toHit.addModifier(vMod, Messages.getString("WeaponAttackAction.TeVelocity"));
        }

        // target immobile
        boolean mekMortarMunitionsIgnoreImmobile = (weaponType != null)
              && weaponType.hasFlag(WeaponType.F_MEK_MORTAR)
              && (ammoType != null)
              && munition.contains(AmmoType.Munitions.M_AIRBURST);
        if (weaponType != null && !(weaponType instanceof ArtilleryCannonWeapon) && !mekMortarMunitionsIgnoreImmobile) {
            ToHitData immobileMod;
            // grounded dropships are treated as immobile as well for purpose of the mods
            if (entityTarget instanceof Dropship && !entityTarget.isAirborne() && !entityTarget.isSpaceborne()) {
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
        if ((entityTarget instanceof Mek) && entityTarget.isSuperHeavy()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMek"));
        }

        // large support tanks get a -1 per TW
        if ((entityTarget != null)
              && (entityTarget.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT)
              && !entityTarget.isAirborne()
              && !entityTarget.isSpaceborne()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeLargeSupportUnit"));
        }

        // "grounded small craft" get a -1 per TW
        if ((entityTarget instanceof SmallCraft)
              && entityTarget.isSmallCraft()
              && !entityTarget.isAirborne()
              && !entityTarget.isSpaceborne()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeGroundedSmallCraft"));
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (!isAttackerInfantry && (entityTarget instanceof BattleArmor)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.BaTarget"));
        }

        if ((entityTarget instanceof Infantry infantry) && entityTarget.isConventionalInfantry()) {
            // infantry squads are also hard to hit
            if (infantry.isSquad()) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.SquadTarget"));
            }
            InfantryMount mount = infantry.getMount();
            if ((mount != null) && (mount.size().toHitMod != 0)) {
                toHit.addModifier(mount.size().toHitMod, Messages.getString("WeaponAttackAction.MountSize"));
            }

            // pl-masc makes foot infantry harder to hit - IntOps p.84
            if (infantry.hasAbility(OptionsConstants.MD_PL_MASC) && infantry.getMovementMode().isLegInfantry()) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.PlMasc"));
            }
        }

        // Ejected MekWarriors are harder to hit
        if (entityTarget instanceof MekWarrior) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.MwTarget"));
        }

        // Aerospace target modifiers
        if ((entityTarget != null) && entityTarget.isAero() && entityTarget.isAirborne()) {
            IAero iAeroTarget = (IAero) entityTarget;

            // is the target at zero velocity
            if ((iAeroTarget.getCurrentVelocity() == 0) && !(iAeroTarget.isSpheroid() && !iAeroTarget.isSpaceborne())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.ImmobileAero"));
            }

            // get mods for direction of attack
            if (!(iAeroTarget.isSpheroid() && !iAeroTarget.isSpaceborne())) {
                int side = ComputeSideTable.sideTable(attacker, entityTarget);

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
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SENSOR_SHADOW) &&
                  iAeroTarget.isSpaceborne()) {
                for (Entity other : Compute.getAdjacentEntitiesAlongAttack(attacker.getPosition(),
                      target.getPosition(),
                      game)) {
                    if (createsSensorShadow(entityTarget, other)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
                for (Entity other : game.getEntitiesVector(target.getPosition(), target.getBoardId())) {
                    if (createsSensorShadow(entityTarget, other) && !other.equals(iAeroTarget)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
            }
        }

        // Quirks
        ComputeAbilityMods.processAttackerQuirks(toHit, attacker, entityTarget, weapon);

        // SPAs
        ComputeAbilityMods.processAttackerSPAs(toHit, attacker, entityTarget, weapon, game);
        ComputeAbilityMods.processDefenderSPAs(toHit, attacker, entityTarget, game);

        return toHit;
    }

    private static boolean createsSensorShadow(Entity target, Entity other) {
        return !other.isEnemyOf(target)
              && other.isLargeCraft()
              && (other.getWeight() - target.getWeight() >= -WeaponAttackAction.STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF);
    }

    private ComputeTargetToHitMods() {}
}
