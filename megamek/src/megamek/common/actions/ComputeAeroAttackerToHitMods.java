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

import java.util.EnumSet;

class ComputeAeroAttackerToHitMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition, if the attacker is
     * an aero Attacker has damaged sensors? You'll find that here. Defender's a superheavy mek? Using a weapon with a
     * TH penalty? Those are in other methods.
     *
     * @param game                The current {@link Game}
     * @param ae                  The Entity making this attack
     * @param target              The Targetable object being attacked
     * @param ttype               The targetable object type
     * @param toHit               The running total ToHitData for this WeaponAttackAction
     * @param aimingAt            An int value representing the location being aimed at
     * @param aimingMode          An int value that determines the reason aiming is allowed
     * @param eistatus            An int value representing the ei cockpit/pilot upgrade status
     * @param wtype               The WeaponType of the weapon being used
     * @param weapon              The Mounted weapon being used
     * @param atype               The AmmoType being used for this attack
     * @param munition            Long indicating the munition type flag being used, if applicable
     * @param isArtilleryIndirect flag that indicates whether this is an indirect-fire artillery attack
     * @param isFlakAttack        flag that indicates whether the attacker is using Flak against an airborne target
     * @param isNemesisConfused   flag that indicates whether the attack is affected by an iNarc Nemesis pod
     * @param isStrafing          flag that indicates whether this is an aero strafing attack
     * @param usesAmmo            flag that indicates if the WeaponType being used is ammo-fed
     */
    static ToHitData compileAeroAttackerToHitMods(Game game, Entity ae, Targetable target, int ttype,
          ToHitData toHit, int aimingAt, AimingMode aimingMode, int eistatus, WeaponType wtype, WeaponMounted weapon,
          AmmoType atype, EnumSet<AmmoType.Munitions> munition, boolean isArtilleryIndirect, boolean isFlakAttack,
          boolean isNemesisConfused, boolean isStrafing, boolean usesAmmo) {
        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            // Some of these weapons only target valid entities
            te = (Entity) target;
        }

        boolean isBombing = wtype != null &&
                                  (wtype.hasFlag(WeaponType.F_ALT_BOMB) || wtype.hasFlag(WeaponType.F_DIVE_BOMB));

        // Generic modifiers that apply to airborne and ground attackers

        // actuator & sensor damage to attacker (includes partial repairs)
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }

        // Secondary targets modifier, if this is not a iNarc Nemesis confused attack
        // Also does not apply to attacks that are intrinsically multi-target (altitude bombing and strafing)
        if (!isNemesisConfused && wtype != null && !wtype.hasFlag(WeaponType.F_ALT_BOMB) && !isStrafing) {
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        }

        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode.isTargetingComputer() && (aimingAt != Entity.LOC_NONE)) {
            if (ae.hasActiveEiCockpit()) {
                if (ae.hasTargComp()) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.AimWithTCompEi"));
                } else {
                    toHit.addModifier(6, Messages.getString("WeaponAttackAction.AimWithEiOnly"));
                }
            } else {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.AimWithTCompOnly"));
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
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TComp"));
            }
        }

        // Modifiers for aero units, including fighter LAMs
        if (ae.isAero()) {
            IAero aero = (IAero) ae;

            // check for heavy gauss rifle on fighter or small craft
            // Arguably a weapon effect, except that it only applies when used by a fighter
            // (isn't recoil fun?)
            // So it's here instead of with other weapon mods that apply across the board
            if ((wtype != null) &&
                      ((wtype.ammoType == AmmoType.AmmoTypeEnum.GAUSS_HEAVY) || (wtype.ammoType == AmmoType.AmmoTypeEnum.IGAUSS_HEAVY)) &&
                      !(ae instanceof Dropship) &&
                      !(ae instanceof Jumpship)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.FighterHeavyGauss"));
            }

            // Space ECM
            if (ae.isSpaceborne() && game.onTheSameBoard(ae, target) &&
                      game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
                int ecm = ComputeECM.getLargeCraftECM(ae, ae.getPosition(), target.getPosition());
                if (!ae.isLargeCraft()) {
                    ecm += ComputeECM.getSmallCraftECM(ae, ae.getPosition(), target.getPosition());
                }
                ecm = Math.min(4, ecm);
                int eccm = 0;
                if (ae.isLargeCraft()) {
                    eccm = ((Aero) ae).getECCMBonus();
                }
                if (ecm > 0) {
                    toHit.addModifier(ecm, Messages.getString("WeaponAttackAction.ECM"));
                    if (eccm > 0) {
                        toHit.addModifier(-1 * Math.min(ecm, eccm), Messages.getString("WeaponAttackAction.ECCM"));
                    }
                }
            }

            // +4 attack penalty for locations hit by ASEW missiles
            if (ae instanceof Dropship) {
                Dropship d = (Dropship) ae;
                if (weapon != null) {
                    int loc = weapon.getLocation();
                    if (d.getASEWAffected(loc) > 0) {
                        toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                    }
                }
            } else if (ae instanceof Jumpship) {
                Jumpship j = (Jumpship) ae;
                if (weapon != null) {
                    int loc = weapon.getLocation();
                    if (j.getASEWAffected(loc) > 0) {
                        toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                    }
                }
            } else {
                if (ae.getASEWAffected() > 0) {
                    toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeAsewAffected"));
                }
            }
            // Altitude-related mods for air-to-air combat
            if (Compute.isAirToAir(game, ae, target)) {
                if (target.isAirborneVTOLorWIGE()) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.TeNonAeroAirborne"));
                }
                if (ae.isNOE()) {
                    if (ae.isOmni()) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeOmniNoe"));
                    } else {
                        toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeNoe"));
                    }
                }
            }

            // A2G attacks
            if (Compute.isAirToGround(ae, target) || (ae.isMakingVTOLGroundAttack())) {
                // TW p.243
                if (isBombing) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.Bombing"));
                    if (wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                        toHit.addModifier(ae.getAltitude(), Messages.getString("WeaponAttackAction.BombAltitude"));
                    }
                    // CO p.75
                    if (ae.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GoldenGoose"));
                    }
                } else if (isStrafing) {
                    toHit.addModifier(+4, Messages.getString("WeaponAttackAction.Strafing"));
                    if (ae.isNOE()) {
                        toHit.addModifier(+2, Messages.getString("WeaponAttackAction.StrafingNoe"));
                        // Nap-of-Earth terrain effects
                        Coords prevCoords = ae.passedThroughPrevious(target.getPosition());
                        Hex prevHex = game.getHex(prevCoords, ae.getPassedThroughBoardId());
                        toHit.append(Compute.getStrafingTerrainModifier(game, eistatus, prevHex));
                    }
                } else { // Striking
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AtgStrike"));
                    // CO p.75
                    if (ae.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GoldenGoose"));
                    }
                }
            }

            // units making A2G attacks are easier to hit by A2A attacks, TW p.241
            if (Compute.isAirToAir(game, ae, target) && (te != null)) {
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
            if (!ae.isAirborne() && !ae.isSpaceborne()) {
                if (ae.isFighter()) {
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
        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;

            // sensor hits
            int sensors = aero.getSensorHits();

            if (!ae.isCapitalFighter()) {
                if ((sensors > 0) && (sensors < 3)) {
                    toHit.addModifier(sensors, Messages.getString("WeaponAttackAction.SensorDamage"));
                }
                if (sensors > 2) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.SensorDestroyed"));
                }
            }

            // FCS hits
            int fcs = aero.getFCSHits();

            if ((fcs > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(fcs * 2, Messages.getString("WeaponAttackAction.FcsDamage"));
            }

            // CIC hits
            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 0) {
                    toHit.addModifier(cic * 2, Messages.getString("WeaponAttackAction.CicDamage"));
                }
            }

            // targeting mods for evasive action by large craft
            // Per TW, this does not apply when firing Capital Missiles
            if (aero.isEvading() &&
                      wtype != null &&
                      (!(wtype.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE ||
                               wtype.getAtClass() == WeaponType.CLASS_AR10 ||
                               wtype.getAtClass() == WeaponType.CLASS_TELE_MISSILE))) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeEvading"));
            }

            // stratops page 113: ECHO maneuvers for large craft
            if (((aero instanceof Warship) || (aero instanceof Dropship)) &&
                      (aero.getFacing() != aero.getSecondaryFacing())) {
                // if we're computing this for an "attack preview", then we add 2 MP to
                // the mp used, as we haven't used the MP yet. If we're actually processing
                // the attack, then the entity will be marked as 'done' and we have already
                // added
                // the 2 MP, so we don't need to double-count it
                int extraMP = aero.isDone() ? 0 : 2;
                boolean willUseRunMP = aero.mpUsed + extraMP > aero.getWalkMP();
                int mod = willUseRunMP ? 2 : 1;
                toHit.addModifier(mod, Messages.getString("WeaponAttackAction.LargeCraftEcho"));
            }

            // check for particular kinds of weapons in weapon bays
            if (ae.usesWeaponBays() && wtype != null && weapon != null) {

                // any heavy lasers
                if (wtype.getAtClass() == WeaponType.CLASS_LASER &&
                          weapon.getBayWeapons()
                                .stream()
                                .map(WeaponMounted::getType)
                                .map(WeaponType::getInternalName)
                                .anyMatch(i -> i.startsWith("CLHeavyLaser"))) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.HeavyLaserInBay"));
                }
                // barracuda missiles
                else if (wtype.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE) {
                    boolean onlyBarracuda = true;
                    for (WeaponMounted bweap : weapon.getBayWeapons()) {
                        AmmoMounted bammo = bweap.getLinkedAmmo();
                        if (bammo != null) {
                            AmmoType batype = bammo.getType();
                            if (batype.getAmmoType() != AmmoType.AmmoTypeEnum.BARRACUDA) {
                                onlyBarracuda = false;
                            }
                        }
                    }
                    if (onlyBarracuda) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Barracuda"));
                    }
                }
                // barracuda missiles in an AR10 launcher (must all be
                // barracuda)
                else if (wtype.getAtClass() == WeaponType.CLASS_AR10) {
                    boolean onlyBarracuda = true;
                    for (WeaponMounted bweap : weapon.getBayWeapons()) {
                        AmmoMounted bammo = bweap.getLinkedAmmo();
                        if (bammo != null) {
                            AmmoType batype = bammo.getType();
                            if (!batype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                                onlyBarracuda = false;
                            }
                        }
                    }
                    if (onlyBarracuda) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Barracuda"));
                    }
                }
                // LBX cluster
                else if (wtype.getAtClass() == WeaponType.CLASS_LBX_AC) {
                    boolean onlyCluster = true;
                    for (WeaponMounted bweap : weapon.getBayWeapons()) {
                        AmmoMounted bammo = bweap.getLinkedAmmo();
                        if (bammo != null) {
                            AmmoType batype = bammo.getType();
                            if (!batype.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
                                onlyCluster = false;
                                break;
                            }
                        }
                    }
                    if (onlyCluster) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ClusterAmmo"));
                    }
                }
            }
        }
        return toHit;
    }

    private ComputeAeroAttackerToHitMods() { }
}
