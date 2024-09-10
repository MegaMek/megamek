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

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mek;
import megamek.common.ProtoMek;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.options.OptionsConstants;

/**
 * The attacker grapples the target.
 */
public class BreakGrappleAttackAction extends PhysicalAttackAction {
    private static final long serialVersionUID = 5615694825997720537L;

    public BreakGrappleAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public BreakGrappleAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Generates the to hit data for this action.
     *
     * @param game The current {@link Game}
     * @return the to hit data object for this action.
     * @see #toHit(Game, int, Targetable)
     */
    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * To-hit number
     * 
     * @param game The current {@link Game}
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
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
        if ((impossible != null) && !impossible.equals("Locked in Grapple")) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if ((ae.getGrappled() != Entity.NONE) && ae.isChainWhipGrappled()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "cannot break free from a chain whip grapple");
        }

        ToHitData toHit;

        // non-meks can't grapple or be grappled
        if (!(ae instanceof Mek) && !(ae instanceof ProtoMek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Only meks and protomeks can be grappled");
        }

        if (ae.getGrappled() != target.getId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Not grappled");
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        PhysicalAttackAction.setCommonModifiers(toHit, game, ae, target);

        if (ae.isGrappleAttacker()) {
            toHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "original attacker");
            return toHit;
        }

        setCommonModifiers(toHit, game, ae, target);

        if (ae instanceof Mek) {
            // damaged or missing actuators
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_LARM)) {
                toHit.addModifier(2, "Left shoulder actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LARM)) {
                toHit.addModifier(2, "Left upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)) {
                toHit.addModifier(2, "Left lower arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM)) {
                toHit.addModifier(1, "Left hand actuator destroyed");
            }

            if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_RARM)) {
                toHit.addModifier(2, "Right shoulder actuator destroyed");
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
        }
        Entity te = (Entity) target;
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

}
