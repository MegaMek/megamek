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

import megamek.client.ui.Messages;
import megamek.common.*;

/**
 * The attacker kicks the target.
 */
public class KickAttackAction extends PhysicalAttackAction {
    private static final long serialVersionUID = 1697321306815235635L;
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int LEFTMULE = 3;
    public static final int RIGHTMULE = 4;

    private int leg;

    public KickAttackAction(int entityId, int targetId, int leg) {
        super(entityId, targetId);
        this.leg = leg;
    }

    public KickAttackAction(int entityId, int targetType, int targetId, int leg) {
        super(entityId, targetType, targetId);
        this.leg = leg;
    }

    public int getLeg() {
        return leg;
    }

    public void setLeg(int leg) {
        this.leg = leg;
    }

    /**
     * Damage that the specified mek does with a kick
     *
     * @return The kick damage for the 'Mek, or 0 for non-'Mek entities.
     */
    public static int getDamageFor(Entity entity, int leg,
            boolean targetInfantry) {
        if (!(entity instanceof Mek)) {
            return 0; // Non-'Meks can't kick, so can't deal damage this way.
        }
        int[] kickLegs = new int[2];
        if (entity.entityIsQuad() && (leg != LEFTMULE) && (leg != RIGHTMULE)) {
            kickLegs[0] = Mek.LOC_RARM;
            kickLegs[1] = Mek.LOC_LARM;
        } else {
            kickLegs[0] = Mek.LOC_RLEG;
            kickLegs[1] = Mek.LOC_LLEG;
        }

        final int legLoc = ((leg == RIGHT) || (leg == RIGHTMULE)) ? kickLegs[0]
                : kickLegs[1];
        int damage = (int) Math.floor(entity.getWeight() / 5.0);
        float multiplier = 1.0f;

        if (!entity.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mek.ACTUATOR_LOWER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mek.ACTUATOR_HIP, legLoc)) {
            damage = 0;
        }
        if (((Mek) entity).hasActiveTSM()) {
            multiplier *= 2.0f;
        }

        double talonMultiplier = 1;
        if (entity.hasWorkingMisc(MiscType.F_TALON, -1, legLoc) && entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, legLoc)) {
            talonMultiplier += 0.5;
        }

        int toReturn = (int) Math.floor(damage * multiplier);
        toReturn = (int) Math.round(toReturn * talonMultiplier);
        toReturn += entity.modifyPhysicalDamageForMeleeSpecialist();
        // underwater damage is half, round up (see bug 1110692)
        if (entity.getLocationStatus(legLoc) == ILocationExposureStatus.WET) {
            toReturn = (int) Math.ceil(toReturn * 0.5f);
        }
        if (targetInfantry) {
            toReturn = Math.max(1, toReturn / 10);
        }
        return toReturn;
    }

    public ToHitData toHit(Game game) {
        return KickAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(),
                getTargetId()), getLeg());
    }

    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHit(Game game, int attackerId,
            Targetable target, int leg) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "You can't attack from a null entity!");
        }

        if (!(ae instanceof Mek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-'Meks can't kick.");
        }

        if (ae.isStuck()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Bogged-down units can't kick.");
        }

        String impossible = PhysicalAttackAction.toHitIsImpossible(game, ae, target);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        Hex attHex = game.getBoard().getHex(ae.getPosition());
        Hex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        final int targetElevation = target.getElevation()
                + targHex.getLevel();
        final int targetHeight = targetElevation + target.getHeight();

        int mule = 0;
        int[] kickLegs = new int[2];
        if (ae.entityIsQuad()) {
            if ((leg == KickAttackAction.LEFTMULE)
                    || (leg == KickAttackAction.RIGHTMULE)) {
                kickLegs[0] = Mek.LOC_RLEG;
                kickLegs[1] = Mek.LOC_LLEG;
                mule = 1; // To-hit modifier
            } else {
                kickLegs[0] = Mek.LOC_RARM;
                kickLegs[1] = Mek.LOC_LARM;
            }
        } else {
            kickLegs[0] = Mek.LOC_RLEG;
            kickLegs[1] = Mek.LOC_LLEG;
        }
        final int legLoc = ((leg == KickAttackAction.RIGHTMULE) || (leg == KickAttackAction.RIGHT)) ? kickLegs[0]
                : kickLegs[1];

        ToHitData toHit;

        // arguments legal?
        // By allowing mulekicks, this gets a little more complicated :(
        if ((leg != KickAttackAction.RIGHT) && (leg != KickAttackAction.LEFT)
                && (leg != KickAttackAction.RIGHTMULE)
                && (leg != KickAttackAction.LEFTMULE)) {
            throw new IllegalArgumentException(
                    "Leg must be one of LEFT, RIGHT, LEFTMULE, or RIGHTMULE");
        }

        // non-meks can't kick
        if (!(ae instanceof Mek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-meks can't kick");
        }

        // check if all legs are present & working
        if (ae.isLocationBad(Mek.LOC_LLEG) || ae.isLocationBad(Mek.LOC_RLEG)
                || (ae.entityIsQuad()
                        && (ae.isLocationBad(Mek.LOC_LARM)
                                || ae.isLocationBad(Mek.LOC_RARM)))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Leg missing");
        }

        // check if all hips are operational
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LLEG)
                || !ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RLEG)
                || (ae.entityIsQuad()
                        && (!ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LARM)
                                || !ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RARM)))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Hip destroyed");
        }
        // check if attacker has fired leg-mounted weapons
        for (Mounted<?> mounted : ae.getWeaponList()) {
            if (mounted.isUsedThisRound() && (mounted.getLocation() == legLoc)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Weapons fired from leg this turn");
            }
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());

        // check elevation
        if (target.isAirborneVTOLorWIGE()) {
            if (targetElevation - attackerElevation != 0) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target elevation not in range");
            }
        } else if ((attackerElevation < targetElevation)
                || (attackerElevation > targetHeight)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target elevation not in range");
        }

        // check facing
        // Don't check arc for stomping infantry or tanks.
        if ((0 != range)
                && (mule != 1)
                && !Compute.isInArc(ae.getPosition(), ae.getFacing(), target, Compute.ARC_FORWARD)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // check facing, part 2: Mule kick
        if ((0 != range)
                && (mule == 1)
                && !Compute.isInArc(ae.getPosition(), ae.getFacing(), target, Compute.ARC_REAR)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // can't kick while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        if (ae.isHullDown()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is hull down");
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target instanceof GunEmplacement)) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                    "Targeting adjacent building.");
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        toHit.addModifier(-2, "Kick");

        PhysicalAttackAction.setCommonModifiers(toHit, game, ae, target);

        // +3 modifier for kicking infantry in same hex
        // see bug 1749177
        if ((target instanceof Infantry) && (range == 0)) {
            toHit.addModifier(3, "Stomping Infantry");
        }

        // Mulekick?
        if (mule != 0) {
            toHit.addModifier(mule, "Quad Mek making a mule kick");
        }

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, legLoc)) {
            toHit.addModifier(2, "Upper leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_LEG, legLoc)) {
            toHit.addModifier(2, "Lower leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_FOOT, legLoc)) {
            toHit.addModifier(1, "Foot actuator destroyed");
        }

        if (ae.hasFunctionalLegAES()) {
            toHit.addModifier(-1, "AES bonus");
        }

        // elevation
        if (attackerElevation < targetHeight) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else if (target.getHeight() > 0) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        // What to do with grounded dropships? Awaiting rules clarification, but
        // until then, we will assume that if the attacker height is less than half
        // the target elevation, then use HIT_KICK, otherwise HIT_NORMAL
        // See Dropship.rollHitLocation to see how HIT_KICK is handled
        if (target instanceof Dropship) {
            if ((attackerElevation - targetElevation) > (target.getHeight() / 2)) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        }

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae, target));

        // BMRr pg. 42, "The side on which a vehicle takes damage is determined
        // randomly if the BattleMek is attacking from the same hex."
        if ((0 == range) && (target instanceof Tank)) {
            toHit.setSideTable(ToHitData.SIDE_RANDOM);
        }

        // done!
        return toHit;
    }

    @Override
    public String toSummaryString(final Game game) {
        String rollLeft;
        String rollRight;
        String buffer;
        final int leg = this.getLeg();
        switch (leg) {
            case KickAttackAction.BOTH:
                rollLeft = KickAttackAction.toHit(game, this.getEntityId(),
                        game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.LEFT)
                        .getValueAsString();
                rollRight = KickAttackAction.toHit(game, this.getEntityId(),
                        game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.RIGHT)
                        .getValueAsString();
                buffer = Messages.getString("BoardView1.kickBoth", rollLeft, rollRight);
                break;
            case KickAttackAction.LEFT:
                rollLeft = KickAttackAction.toHit(game, this.getEntityId(),
                        game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.LEFT)
                        .getValueAsString();
                buffer = Messages.getString("BoardView1.kickLeft", rollLeft);
                break;
            case KickAttackAction.RIGHT:
                rollRight = KickAttackAction.toHit(game, this.getEntityId(),
                        game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.RIGHT)
                        .getValueAsString();
                buffer = Messages.getString("BoardView1.kickRight", rollRight);
                break;
            default:
                buffer = "Error on kick action";
        }
        return buffer;
    }
}
