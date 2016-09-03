/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.actions;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.options.OptionsConstants;

/**
 * The attacker kicks the target.
 */
public class TripAttackAction extends PhysicalAttackAction {

    /**
     *
     */
    private static final long serialVersionUID = -8639566786588420601L;

    public TripAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public TripAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    public ToHitData toHit(IGame game) {
        return TripAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHit(IGame game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't attack from a null entity!");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_TRIP_ATTACK)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "no Trip attack");
        }

        String impossible = PhysicalAttackAction.toHitIsImpossible(game, ae, target);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if ( ae.getGrappled() != Entity.NONE ) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if (target.getTargetType() == Targetable.TYPE_ENTITY
                    && (((Entity)target).getOwnerId() == ae.getOwnerId()
                            || (((Entity)target).getOwner().getTeam() != IPlayer.TEAM_NONE
                                    && ae.getOwner().getTeam() != IPlayer.TEAM_NONE
                                    && ae.getOwner().getTeam() == ((Entity)target).getOwner().getTeam()))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "A friendly unit can never be the target of a direct attack.");
            }
        }

        ToHitData toHit;

        // non-mechs can't trip or be tripped
        if (!(ae instanceof Mech) || !(target instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Only mechs can trip other mechs");
        }

        // described as a leg hook
        // needs 2 legs present
        if (ae.isLocationBad(Mech.LOC_LLEG) || ae.isLocationBad(Mech.LOC_RLEG)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Leg missing");
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if (range > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target out of range");
        }

        int limb1 = Entity.LOC_NONE;

        // check facing
        if (!Compute.isInArc(ae.getPosition(), ae.getFacing(), target, Compute.ARC_FORWARD)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // can't trip while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }
        if (((Entity) target).isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is prone");
        }

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        final int targetElevation = target.getElevation()
                + targHex.getLevel();

        if (attackerElevation != targetElevation){
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker and Target must be at the same elevation");
        }

        // check if attacker has fired leg-mounted weapons
        boolean usedWeapons[] = new boolean[ae.locations()];
        for (int i = 0; i < ae.locations(); i++) {
            usedWeapons[i] = false;
        }

        for (Mounted mounted : ae.getWeaponList()) {
            if (mounted.isUsedThisRound()) {
                int loc = mounted.getLocation();
                if (loc != Entity.LOC_NONE) {
                    usedWeapons[loc] = true;
                }
            }
        }

        // check for good hips / shoulders
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, Mech.LOC_RLEG)) {
            usedWeapons[Mech.LOC_RLEG] = true;
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, Mech.LOC_LLEG)) {
            usedWeapons[Mech.LOC_LLEG] = true;
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, Mech.LOC_RARM)) {
            usedWeapons[Mech.LOC_RARM] = true;
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, Mech.LOC_LARM)) {
            usedWeapons[Mech.LOC_LARM] = true;
        }

        if (ae instanceof QuadMech) {
            if (usedWeapons[Mech.LOC_RARM]) {
                if (usedWeapons[Mech.LOC_LARM]) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "both legs unusable");
                }
                limb1 = Mech.LOC_LARM;
            }
        }
        // normal attack uses both legs
        else if (usedWeapons[Mech.LOC_RLEG]) {
            if (usedWeapons[Mech.LOC_LLEG]) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "both legs unusable");
            }
            limb1 = Mech.LOC_LLEG;
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting() - 1;

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        PhysicalAttackAction.setCommonModifiers(toHit, game, ae, target);

        // Get best leg
        if ( ae instanceof QuadMech ) {
            if (limb1 == Entity.LOC_NONE) {
                ToHitData left = TripAttackAction.getLimbModifier(Mech.LOC_LARM, ae);
                ToHitData right = TripAttackAction.getLimbModifier(Mech.LOC_RARM, ae);
                if (left.getValue() < right.getValue()) {
                    toHit.append(left);
                } else {
                    toHit.append(right);
                }
            } else {
                toHit.append(TripAttackAction.getLimbModifier(limb1, ae));
            }
        }
        else if (limb1 == Entity.LOC_NONE) {
            ToHitData left = TripAttackAction.getLimbModifier(Mech.LOC_LLEG, ae);
            ToHitData right = TripAttackAction.getLimbModifier(Mech.LOC_RLEG, ae);
            if (left.getValue() < right.getValue()) {
                toHit.append(left);
            } else {
                toHit.append(right);
            }
        } else {
            toHit.append(TripAttackAction.getLimbModifier(limb1, ae));
        }

        if ( ae.hasFunctionalLegAES() ) {
            toHit.addModifier(-1, "AES modifer");
        }

        // done!
        return toHit;
    }

    private static ToHitData getLimbModifier(int loc, Entity ae) {
        ToHitData toHit = new ToHitData();
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, loc)) {
            toHit.addModifier(2, "Upper leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, loc)) {
            toHit.addModifier(2, "Lower leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_FOOT, loc)) {
            toHit.addModifier(1, "Foot actuator destroyed");
        }
        return toHit;
    }
}
