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

import java.util.EnumSet;
import java.util.function.Predicate;

import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.Jumpship;
import megamek.common.units.Targetable;
import megamek.common.units.Warship;

public class ComputeAeroAttackerToHitMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition, if the attacker is
     * an aero Attacker has damaged sensors? You'll find that here. Defender's a superheavy mek? Using a weapon with a
     * TH penalty? Those are in other methods.
     *
     * @param game                 The current {@link Game}
     * @param attacker             The Entity making this attack
     * @param target               The Targetable object being attacked
     * @param targetableTargetType The targetable object type
     * @param toHit                The running total ToHitData for this WeaponAttackAction
     * @param aimingAt             An int value representing the location being aimed at
     * @param aimingMode           An int value that determines the reason aiming is allowed
     * @param eiPilotUpgradeStatus An int value representing the ei cockpit/pilot upgrade status
     * @param weaponType           The WeaponType of the weapon being used
     * @param weapon               The Mounted weapon being used
     * @param ammoType             The AmmoType being used for this attack
     * @param munition             Long indicating the munition type flag being used, if applicable
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     * @param isFlakAttack         flag that indicates whether the attacker is using Flak against an airborne target
     * @param isNemesisConfused    flag that indicates whether the attack is affected by an iNarc Nemesis pod
     * @param isStrafing           flag that indicates whether this is an aero strafing attack
     * @param usesAmmo             flag that indicates if the WeaponType being used is ammo-fed
     */
    public static ToHitData compileAeroAttackerToHitMods(Game game, Entity attacker, Targetable target,
          int targetableTargetType, ToHitData toHit, int aimingAt, AimingMode aimingMode, int eiPilotUpgradeStatus,
          WeaponType weaponType, WeaponMounted weapon, AmmoType ammoType, EnumSet<AmmoType.Munitions> munition,
          boolean isArtilleryIndirect, boolean isFlakAttack, boolean isNemesisConfused, boolean isStrafing,
          boolean usesAmmo) {

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (targetableTargetType == Targetable.TYPE_ENTITY) {
            // Some of these weapons only target valid entities
            te = (Entity) target;
        }

        boolean isBombing = (weaponType != null) &&
              (weaponType.hasFlag(WeaponType.F_ALT_BOMB) || weaponType.hasFlag(WeaponType.F_DIVE_BOMB));

        // Generic modifiers that apply to airborne and ground attackers

        // actuator & sensor damage to attacker (includes partial repairs)
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(attacker, weapon));
        }

        // heat
        if (attacker.getHeatFiringModifier() != 0) {
            toHit.addModifier(attacker.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }

        // Secondary targets modifier, if this is not a iNarc Nemesis confused attack
        // Also does not apply to attacks that are intrinsically multi-target (altitude bombing and strafing)
        if (!isNemesisConfused && weaponType != null && !weaponType.hasFlag(WeaponType.F_ALT_BOMB) && !isStrafing) {
            toHit.append(Compute.getSecondaryTargetMod(game, attacker, target));
        }

        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode.isTargetingComputer() && (aimingAt != Entity.LOC_NONE)) {
            if (attacker.hasActiveEiCockpit()) {
                if (attacker.hasTargComp()) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.AimWithTCompEi"));
                } else {
                    toHit.addModifier(6, Messages.getString("WeaponAttackAction.AimWithEiOnly"));
                }
            } else if (attacker.hasTCPAimedShotCapability() && attacker.hasTargComp()) {
                // TCP+VDNI with actual TC gets additional -1 per IO pg 81
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.AimWithTCPAndTC"));
            } else if (attacker.hasTCPAimedShotCapability()) {
                // TCP+VDNI without TC acts as if equipped with TC per IO pg 81
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.AimWithTCPOnly"));
            } else {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.AimWithTCompOnly"));
            }
        } else if (attacker.hasTargComp()
              && (weaponType != null)
              && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
              && !weaponType.hasFlag(WeaponType.F_CWS)
              && !weaponType.hasFlag(WeaponType.F_TASER)) {

            // LB-X cluster, HAG flak, flak ammo ineligible for TC bonus
            boolean usesLBXCluster = usesAmmo &&
                  (ammoType != null) &&
                  (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX ||
                        ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX_THB) &&
                  munition.contains(AmmoType.Munitions.M_CLUSTER);
            boolean usesHAGFlak = usesAmmo && (ammoType != null) && (ammoType.getAmmoType()
                  == AmmoType.AmmoTypeEnum.HAG)
                  && isFlakAttack;
            boolean isSBGauss = usesAmmo && (ammoType != null) && (ammoType.getAmmoType()
                  == AmmoType.AmmoTypeEnum.SBGAUSS);
            boolean isFlakAmmo = usesAmmo && (ammoType != null) && (munition.contains(AmmoType.Munitions.M_FLAK));
            if (!usesAmmo || !(usesLBXCluster || usesHAGFlak || isSBGauss || isFlakAmmo)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TComp"));
            }
        }

        // Modifiers for aero units, including fighter LAMs
        if (attacker.isAero()) {
            IAero aero = (IAero) attacker;

            // check for heavy gauss rifle on fighter or small craft
            // Arguably a weapon effect, except that it only applies when used by a fighter (isn't recoil fun?)
            // So it's here instead of with other weapon mods that apply across the board
            if ((weaponType != null)
                  && ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.GAUSS_HEAVY)
                  || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.IGAUSS_HEAVY))
                  && !(attacker instanceof Dropship)
                  && !(attacker instanceof Jumpship)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.FighterHeavyGauss"));
            }

            // Space ECM
            if (attacker.isSpaceborne() && game.onTheSameBoard(attacker, target)
                  && game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
                int ecm = ComputeECM.getLargeCraftECM(attacker, attacker.getPosition(), target.getPosition());
                if (!attacker.isLargeCraft()) {
                    ecm += ComputeECM.getSmallCraftECM(attacker, attacker.getPosition(), target.getPosition());
                }
                ecm = Math.min(4, ecm);
                int eccm = 0;
                if (attacker.isLargeCraft()) {
                    eccm = ((Aero) attacker).getECCMBonus();
                }
                if (ecm > 0) {
                    toHit.addModifier(ecm, Messages.getString("WeaponAttackAction.ECM"));
                    if (eccm > 0) {
                        toHit.addModifier(-1 * Math.min(ecm, eccm),
                              Messages.getString("WeaponAttackAction.ECCM"));
                    }
                }
            }

            // +4 attack penalty for locations hit by ASEW missiles
            if (attacker instanceof Dropship dropship) {
                if (weapon != null) {
                    if (dropship.getASEWAffected(weapon.getLocation()) > 0) {
                        toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                    }
                }
            } else if (attacker instanceof Jumpship jumpship) {
                if (weapon != null) {
                    if (jumpship.getASEWAffected(weapon.getLocation()) > 0) {
                        toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                    }
                }
            } else {
                if (attacker.getASEWAffected() > 0) {
                    toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeAsewAffected"));
                }
            }

            // Altitude-related mods for air-to-air combat
            if (Compute.isAirToAir(game, attacker, target)) {
                if (target.isAirborneVTOLorWIGE()) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.TeNonAeroAirborne"));
                }
                if (attacker.isNOE()) {
                    if (attacker.isOmni()) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeOmniNoe"));
                    } else {
                        toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeNoe"));
                    }
                }
            }

            // A2G attacks
            if (Compute.isAirToGround(attacker, target) || (attacker.isMakingVTOLGroundAttack())) {
                // TW p.243
                if (isBombing) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.Bombing"));
                    if (weaponType.hasFlag(WeaponType.F_ALT_BOMB)) {
                        toHit.addModifier(attacker.getAltitude(),
                              Messages.getString("WeaponAttackAction.BombAltitude"));
                    }
                    // CO p.75
                    if (attacker.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GoldenGoose"));
                    }

                } else if (isStrafing) {
                    toHit.addModifier(+4, Messages.getString("WeaponAttackAction.Strafing"));
                    if (attacker.isNOE()) {
                        toHit.addModifier(+2, Messages.getString("WeaponAttackAction.StrafingNoe"));
                        // Nap-of-Earth terrain effects
                        Coords prevCoords = attacker.passedThroughPrevious(target.getPosition());
                        Hex prevHex = game.getHex(prevCoords, attacker.getPassedThroughBoardId());
                        toHit.append(Compute.getStrafingTerrainModifier(game, eiPilotUpgradeStatus, prevHex));
                    }

                } else { // Striking
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AtgStrike"));
                    // CO p.75
                    if (attacker.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GoldenGoose"));
                    }
                }
            }

            // units making A2G attacks are easier to hit by A2A attacks, TW p.241
            if (Compute.isAirToAir(game, attacker, target) && (te != null)) {
                for (EntityAction action : game.getActionsVector()) {
                    if (action instanceof WeaponAttackAction attack) {
                        if ((attack.getEntityId() == te.getId()) && attack.isAirToGround(game)) {
                            toHit.addModifier(-3, Messages.getString("WeaponAttackAction.TeGroundAttack"));
                            break;
                        }
                    }
                }
            }

            // grounded aero
            if (!attacker.isAirborne() && !attacker.isSpaceborne()) {
                if (attacker.isFighter()) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.GroundedAero"));
                } else if (!target.isAirborne() && !isArtilleryIndirect) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GroundedDs"));
                }
            }

            // out of control
            if (aero.isOutControlTotal()) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeroOoc"));
            }
        }

        // Situational modifiers for aero units, not including LAMs.
        if (attacker instanceof Aero aero) {

            // sensor hits
            int sensors = aero.getSensorHits();

            if (!aero.isCapitalFighter()) {
                if ((sensors > 0) && (sensors < 3)) {
                    toHit.addModifier(sensors, Messages.getString("WeaponAttackAction.SensorDamage"));
                }
                if (sensors > 2) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.SensorDestroyed"));
                }
            }

            // FCS hits
            int fireControlHits = aero.getFCSHits();

            if ((fireControlHits > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(fireControlHits * 2, Messages.getString("WeaponAttackAction.FcsDamage"));
            }

            // CIC hits
            if (aero instanceof Jumpship jumpship) {
                int cicHits = jumpship.getCICHits();
                if (cicHits > 0) {
                    toHit.addModifier(cicHits * 2, Messages.getString("WeaponAttackAction.CicDamage"));
                }
            }

            // targeting mods for evasive action by large craft
            // Per TW, this does not apply when firing Capital Missiles
            if (aero.isEvading() && (weaponType != null)
                  && (!(weaponType.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE ||
                  weaponType.getAtClass() == WeaponType.CLASS_AR10 ||
                  weaponType.getAtClass() == WeaponType.CLASS_TELE_MISSILE))) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeEvading"));
            }

            // SO p.113: ECHO maneuvers for large craft
            if (((aero instanceof Warship) || (aero instanceof Dropship))
                  && (aero.getFacing() != aero.getSecondaryFacing())) {
                // if we're computing this for an "attack preview", then we add 2 MP to the mp used, as we haven't
                // used the MP yet. If we're actually processing the attack, then the entity will be marked as 'done'
                // and we have already added the 2 MP, so we don't need to double-count it
                int extraMP = aero.isDone() ? 0 : 2;
                boolean willUseRunMP = aero.mpUsed + extraMP > aero.getWalkMP();
                int mod = willUseRunMP ? 2 : 1;
                toHit.addModifier(mod, Messages.getString("WeaponAttackAction.LargeCraftEcho"));
            }

            // check for particular kinds of weapons in weapon bays
            if (attacker.usesWeaponBays() && (weaponType != null) && (weapon != null)) {

                // any heavy lasers
                if (weaponType.hasFlag(WeaponTypeFlag.HEAVY_LASER)) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.HeavyLaserInBay"));
                }

                // barracuda missiles
                else if (weaponType.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE) {
                    if (bayHasOnlyAmmoType(weapon, t -> t.getAmmoType() == AmmoType.AmmoTypeEnum.BARRACUDA)) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Barracuda"));
                    }
                }

                // barracuda missiles in an AR10 launcher (must all be barracuda)
                else if (weaponType.getAtClass() == WeaponType.CLASS_AR10) {
                    if (bayHasOnlyAmmoType(weapon, t -> t.hasFlag(AmmoType.F_AR10_BARRACUDA))) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Barracuda"));
                    }
                }

                // LBX cluster
                else if (weaponType.getAtClass() == WeaponType.CLASS_LBX_AC) {
                    if (bayHasOnlyAmmoType(weapon, t -> t.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ClusterAmmo"));
                    }
                }
            }
        }
        return toHit;
    }

    /**
     * @param weaponBay The bay to test
     * @param ammoTest  An AmmoType test
     *
     * @return True when all weapons in the given weapon bay are linked to ammo that satisfy the given ammoTest. Weapons
     *       that are not linked to ammo are ignored.
     */
    private static boolean bayHasOnlyAmmoType(WeaponMounted weaponBay, Predicate<AmmoType> ammoTest) {
        for (WeaponMounted weaponInBay : weaponBay.getBayWeapons()) {
            AmmoMounted linkedAmmo = weaponInBay.getLinkedAmmo();
            if (linkedAmmo != null) {
                AmmoType linkedAmmoType = linkedAmmo.getType();
                if (!ammoTest.test(linkedAmmoType)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ComputeAeroAttackerToHitMods() {}
}
