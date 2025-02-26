/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.actions;

import megamek.common.*;
import megamek.common.options.OptionsConstants;

/**
 * The attacker grapples the target.
 */
public class GrappleAttackAction extends PhysicalAttackAction {
    private static final long serialVersionUID = -4178252788550426489L;

    public GrappleAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public GrappleAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * @param game       The current {@link Game}
     * @param attackerId the attacking entity id
     * @param target     the attack's target
     * @return the to hit number for the current grapple attack
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        return toHit(game, attackerId, target, Entity.GRAPPLE_BOTH, false);
    }

    /**
     * Calculates ToHitData for a grapple attack.
     *
     * @param game        The current {@link Game}
     * @param attackerId
     * @param target
     * @param grappleSide
     * @param isChainWhip
     *                    Flag that determines if the attack is coming from a chain
     *                    whip. If true, ignore illegal cases, as this comes from a
     *                    bonus attack for a chain whip, and the attack should never
     *                    be
     *                    illegal. See TO pg 289.
     * @return
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target, int grappleSide,
            boolean isChainWhip) {
        final Entity ae = game.getEntity(attackerId);

        ToHitData toHit = checkIllegal(game, ae, target, grappleSide);

        if ((toHit != null) && !isChainWhip) {
            return toHit;
        }

        Entity te = (Entity) target;

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        setCommonModifiers(toHit, game, ae, target);

        if ((ae instanceof Mek) && grappleSide == Entity.GRAPPLE_BOTH) {
            // damaged or missing actuators
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LARM)) {
                toHit.addModifier(2, "Left upper arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)) {
                toHit.addModifier(2, "Left lower arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM)) {
                toHit.addModifier(1, "Left hand actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_RARM)) {
                toHit.addModifier(2, "Right upper arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM)) {
                toHit.addModifier(2, "Right lower arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM)) {
                toHit.addModifier(1, "Right hand actuator destroyed");
            }

            if (ae.hasFunctionalArmAES(Mek.LOC_RARM) && ae.hasFunctionalArmAES(Mek.LOC_LARM)) {
                toHit.addModifier(-1, "AES modifer");
            }

        } else if (ae instanceof Mek && grappleSide == Entity.GRAPPLE_RIGHT) {
            // damaged or missing actuators
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_RARM)) {
                toHit.addModifier(2, "Right upper arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM)) {
                toHit.addModifier(2, "Right lower arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM)) {
                toHit.addModifier(1, "Right hand actuator destroyed");
            }

            if (ae.hasFunctionalArmAES(Mek.LOC_RARM)) {
                toHit.addModifier(-1, "AES modifer");
            }

        } else {
            // damaged or missing actuators
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LARM)) {
                toHit.addModifier(2, "Left upper arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)) {
                toHit.addModifier(2, "Left lower arm actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM)) {
                toHit.addModifier(1, "Left hand actuator destroyed");
            }

            if (ae.hasFunctionalArmAES(Mek.LOC_LARM)) {
                toHit.addModifier(-1, "AES modifer");
            }

        }

        if ((grappleSide != Entity.GRAPPLE_BOTH) && (ae instanceof Mek)) {
            Mek attacker = (Mek) ae;
            Mek teMek = (te instanceof Mek) ? (Mek) te : null;
            if (attacker.hasActiveTSM(false)
                    && ((teMek == null) || !teMek.hasActiveTSM(false)
                            || teMek.hasActiveTSM(false))) {
                toHit.addModifier(-2, "TSM Active Bonus");
            }
        }

        // Weight class difference
        int wmod = te.getWeightClass() - ae.getWeightClass();

        if ((te instanceof ProtoMek) && !(ae instanceof ProtoMek)) {
            wmod = ae.getWeightClass() * -1;
        } else if ((ae instanceof ProtoMek) && !(te instanceof ProtoMek)) {
            wmod = te.getWeightClass();
        } else if ((te instanceof ProtoMek) && (ae instanceof ProtoMek)) {
            wmod = 0;
        }

        if (wmod != 0) {
            toHit.addModifier(wmod, "Weight class difference");
        }
        // done!
        return toHit;
    }

    /**
     * Various modifiers to check to see if the grapple attack is illegal.
     *
     * @param game        The current {@link Game}
     * @param ae
     * @param target
     * @param grappleSide
     * @return
     */
    public static ToHitData checkIllegal(Game game, Entity ae, Targetable target, int grappleSide) {
        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't attack from a null entity!");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GRAPPLING)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "grappling attack not allowed");
        }

        // LAM AirMeks can only grapple when grounded.
        if (ae.isAirborneVTOLorWIGE()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot grapple while airborne");
        }

        String impossible = toHitIsImpossible(game, ae, target);
        if (impossible != null && !impossible.equals("Locked in Grapple")) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                    && ((((Entity) target).getOwnerId() == ae.getOwnerId())
                            || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "A friendly unit can never be the target of a direct attack.");
            }
        }

        Hex attHex = game.getBoard().getHex(ae.getPosition());
        Hex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        // final int attackerHeight = attackerElevation + ae.getHeight();
        final int targetElevation = target.getElevation() + targHex.getLevel();
        // final int targetHeight = targetElevation + target.getHeight();

        // non-meks can't grapple or be grappled
        if ((!(ae instanceof BipedMek) && !(ae instanceof ProtoMek))
                || (!(target instanceof Mek) && !(target instanceof ProtoMek))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Only biped meks can grapple 'Meks and ProtoMeks");
        }

        Entity te = (Entity) target;
        final boolean counter = ae.getGrappled() != Entity.NONE
                && !ae.isGrappleAttacker();

        // check for no/minimal arms quirk
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No/minimal arms");
        }

        // requires 2 good arms
        if (grappleSide == Entity.GRAPPLE_BOTH) {
            if (ae.isLocationBad(Mek.LOC_LARM)
                    || ae.isLocationBad(Mek.LOC_RARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_RARM)
                    || !ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Shoulder missing/destroyed");
            }
        } else if (grappleSide == Entity.GRAPPLE_LEFT) {
            if (ae.isLocationBad(Mek.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Shoulder missing/destroyed");
            }
        } else {
            if (ae.isLocationBad(Mek.LOC_RARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_RARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Shoulder missing/destroyed");
            }
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if (range != 1 && !counter) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // check elevation (attacker must be able to enter target hex)
        if (Math.abs(attackerElevation - targetElevation) > ae.getMaxElevationChange()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target elevation not in range");
        }

        // check facing
        if (!counter && !Compute.isInArc(ae.getPosition(), ae.getFacing(), target, Compute.ARC_FORWARD)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // can't grapple while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        if (((Entity) target).isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is prone");
        }

        // check if attacker has fired any weapons
        if (!counter) {
            for (Mounted<?> mounted : ae.getWeaponList()) {
                if (mounted.isUsedThisRound()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Fired weapons");
                }
            }
        }

        // already done?
        int atGr = ae.getGrappled();
        int deGr = te.getGrappled();
        if ((atGr != Entity.NONE || deGr != Entity.NONE)
                && atGr != target.getId() && te.isGrappleAttacker()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Already grappled");
        }

        // Not illegal, return null
        return null;
    }
}
