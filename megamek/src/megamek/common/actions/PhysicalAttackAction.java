/*
* MegaMek -
* Copyright (C) 2001-2004 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common.actions;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

public class PhysicalAttackAction extends AbstractAttackAction {
    private static final long serialVersionUID = -4702357516725749181L;

    public PhysicalAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public PhysicalAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Common checking whether is it possible to physically attack the target
     *
     * @param game The current {@link Game}
     * @param ae the attacking {@link Entity}, which may be null
     * @param target the attack's target
     * @return reason the attack is impossible, or null if it is possible
     */
    protected static @Nullable String toHitIsImpossible(Game game, @Nullable Entity ae,
                                                        Targetable target) {
        if (target == null) {
            return "target is null";
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                && ((((Entity) target).getOwnerId() == ae.getOwnerId())
                    || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                        && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                        && (ae.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return "A friendly unit can never be the target of a direct attack.";
            }
        }

        // check range
        if (Compute.effectiveDistance(game, ae, target) > 1) {
            return "Target not in range";
        }

        // can't make a physical attack if you are evading
        if (ae.isEvading()) {
            return "Attacker is evading.";
        }

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            // Checks specific to entity targets
            Entity te = (Entity) target;

            // Can't target a transported entity.
            if (Entity.NONE != te.getTransportId()) {
                return "Target is a passenger.";
            }

            // can't target yourself
            if (ae.equals(te)) {
                return "You can't target yourself";
            }

            // can't target airborne aeros
            if (te.isAirborne()) {
                return "can't target airborne units";
            }

            // Can't target a entity conducting a swarm attack.
            if (Entity.NONE != te.getSwarmTargetId()) {
                return "Target is swarming a Mek.";
            }

            if ((ae.getGrappled() != Entity.NONE) && (ae.getGrappleSide() == Entity.GRAPPLE_BOTH)) {
                return "Locked in Grapple";

            }

            // target unit in building checks
            final boolean targetInBuilding = Compute.isInBuilding(game, te);
            if (targetInBuilding) {
                Building TargBldg = game.getBoard().getBuildingAt(te.getPosition());

                // Can't target units in buildings (from the outside).
                if (!Compute.isInBuilding(game, ae)) {
                    return "Target is inside building";
                } else if (!game.getBoard().getBuildingAt(ae.getPosition()).equals(TargBldg)) {
                    return "Target is inside different building";
                }
            }

            // can't physically attack mechs making dfa attacks
            if (te.isMakingDfa()) {
                return "Target is making a DFA attack";
            }
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
            || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
            || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return "Invalid attack";
        }

        return null;
    }

    protected static void setCommonModifiers(ToHitData toHit, Game game, Entity ae, Targetable target) {
        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, target);
        int attackerId = ae.getId();
        int targetId = target.getId();
        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (target instanceof BattleArmor) {
            toHit.addModifier(1, "battle armor target");
        }

        // Infantry squads are also hard to hit -- including for other infantry,
        // it seems (the rule is "all attacks"). However, this only applies to
        // proper squads deployed as such.
        if (target.isConventionalInfantry() && ((Infantry) target).isSquad()) {
            toHit.addModifier(1, "infantry squad target");
        }

        // Ejected MechWarriors are also more difficult targets.
        if (target instanceof MechWarrior) {
            toHit.addModifier(2, "ejected Pilot target");
        }
        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, target, 0, inSameBuilding));

        if (ae.hasModularArmor()) {
            toHit.addModifier(1, "Modular Armor");
        }

        if ((ae instanceof Mech) && ae.isSuperHeavy()) {
            toHit.addModifier(1, "attacker is superheavy mech");
        }
        
        if ((ae instanceof TripodMech) && ae.getCrew().hasDedicatedPilot()) {
            toHit.addModifier(-1, "attacker is tripod with dedicated pilot");
        }

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                 Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Sensors Completely Destroyed for Torso-Mounted Cockpit");
                return;
            } else if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        // if we're spotting for indirect fire, add +1
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()
                && game.getTagInfo().stream().noneMatch(inf -> inf.attackerId == ae.getId())) {
            toHit.addModifier(+1, "attacker is spotting for indirect LRM fire");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(target));

        toHit.append(nightModifiers(game, target, null, ae, false));

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            // Checks specific to entity targets
            Entity te = (Entity) target;

            // target movement
            toHit.append(Compute.getTargetMovementModifier(game, targetId));

            // target prone
            if (te.isProne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
            }

            if ((te.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) && !te.isAirborne() && !te.isSpaceborne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TeLargeSupportUnit"));
            }

            if (te instanceof SmallCraft) {
                if (te instanceof Dropship) {
                    toHit.addModifier(-4, Messages.getString("WeaponAttackAction.ImmobileDs"));
                } else {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TeGroundedSmallCraft"));
                }
            }

            Hex targHex = game.getBoard().getHex(te.getPosition());
            // water partial cover?
            if ((te.height() > 0) && (te.getElevation() == -1)
                    && (targHex.terrainLevel(Terrains.WATER) == te.height())) {
                toHit.addModifier(1, "target has partial cover");
            }

            // Pilot skills
            Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

            // Attacking Weight Class Modifier.
            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)) {
                if (ae.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                    toHit.addModifier(-2, "Weight Class Attack Modifier");
                } else if (ae.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                    toHit.addModifier(-1, "Weight Class Attack Modifier");
                }
            }

            // evading bonuses
            if (te.isEvading()) {
                toHit.addModifier(te.getEvasionBonus(), "target is evading");
            }

            if (te.isStealthActive()) {
                toHit.append(te.getStealthModifier(RangeType.RANGE_MINIMUM, ae));
            }
        }
        
        if ((ae instanceof Mech) && ((Mech) ae).hasIndustrialTSM()) {
            toHit.addModifier(2, "industrial TSM");
        }
    }
}
