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

import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.VTOL;

/**
 * The attacker punches the target.
 */
public class PunchAttackAction extends PhysicalAttackAction {
    /**
     *
     */
    private static final long serialVersionUID = 3684646558944678180L;
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private int arm;
    //booleans for retractable blade extension
    private boolean leftBlade = false;
    private boolean rightBlade = false;

    public PunchAttackAction(int entityId, int targetId, int arm) {
        super(entityId, targetId);
        this.arm = arm;
    }

    public PunchAttackAction(int entityId, int targetType, int targetId, int arm, boolean leftBlade, boolean rightBlade) {
        super(entityId, targetType, targetId);
        this.arm = arm;
        this.leftBlade = leftBlade;
        this.rightBlade = rightBlade;
    }

    public int getArm() {
        return arm;
    }

    public void setArm(int arm) {
        this.arm = arm;
    }

    public boolean isBladeExtended(int arm) {
        if(arm == LEFT) {
            return leftBlade;
        }
        if(arm == RIGHT) {
            return rightBlade;
        }
        return false;
    }

    public ToHitData toHit(IGame game) {
        return PunchAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(),
                getTargetId()), getArm());
    }

    /**
     * punches are impossible when physical attacks are impossible, or a
     * retractable blade is extended
     *
     * @param game
     * @param ae
     * @param target
     * @return
     */
    protected static String toHitIsImpossible(IGame game, Entity ae,
            Targetable target, int arm) {
        String physicalImpossible = PhysicalAttackAction.toHitIsImpossible(
                game, ae, target);
        if (physicalImpossible != null) {
            return physicalImpossible;
        }
        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerHeight = ae.absHeight() + attHex.getElevation();
        final int targetElevation = target.getElevation()
                + targHex.getElevation();
        final int targetHeight = targetElevation + target.getHeight();
        final int armLoc = (arm == PunchAttackAction.RIGHT) ? Mech.LOC_RARM
                : Mech.LOC_LARM;
        if (ae.getGrappled() != Entity.NONE
                && ((ae.getGrappleSide() == Entity.GRAPPLE_LEFT && arm == Mech.LOC_LARM))
                || (ae.getGrappleSide() == Entity.GRAPPLE_RIGHT && arm == Mech.LOC_RARM)) {
            return "grappled with punching arm";
        }
        if (ae instanceof Mech && ((Mech) ae).hasExtendedRetractableBlade()) {
            return "Extended retractable blade";
        }
        // non-mechs can't punch
        if (!(ae instanceof Mech)) {
            return "Non-mechs can't punch";
        }

        // Quads can't punch
        if (ae.entityIsQuad()) {
            return "Attacker is a quad";
        }

        // Can't punch with flipped arms
        if (ae.getArmsFlipped()) {
            return "Arms are flipped to the rear. Can not punch.";
        }

        // check if arm is present
        if (ae.isLocationBad(armLoc)) {
            return "Arm missing";
        }

        // check if shoulder is functional
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            return "Shoulder destroyed";
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(armLoc)) {
            return "Weapons fired from arm this turn";
        }

        // check elevation
        if (target instanceof VTOL && ((VTOL)target).isFlying()) {
            if (targetElevation - attackerHeight > 2 || targetElevation - attackerHeight < 1) {
                return "Target elevation not in range";
            }
        } else if (attackerHeight < targetElevation || attackerHeight > targetHeight) {
            return "Target elevation not in range";
        }

        // Cannot punch with an arm that has an active shield on it.
        if (ae.hasActiveShield(armLoc)) {
            return "Cannot punch with shield in active mode";
        }
        return null;
    }

    /**
     * To-hit number for the specified arm to punch
     */
    public static ToHitData toHit(IGame game, int attackerId,
            Targetable target, int arm) {
        final Entity ae = game.getEntity(attackerId);

        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }
        String impossible = PunchAttackAction.toHitIsImpossible(game, ae, target, arm);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, impossible);
        }

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerHeight = ae.absHeight() + attHex.getElevation();
        final int targetElevation = target.getElevation()
                + targHex.getElevation();
        final int armArc = (arm == PunchAttackAction.RIGHT) ? Compute.ARC_RIGHTARM
                : Compute.ARC_LEFTARM;

        ToHitData toHit;

        // arguments legal?
        if (arm != PunchAttackAction.RIGHT && arm != PunchAttackAction.LEFT) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }



        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        toHit = new ToHitData(base, "base");

        PhysicalAttackAction.setCommonModifiers(toHit, game, ae, target);

        // Prone Meks can only punch vehicles in the same hex.
        if (ae.isProne()) {
            // The Mek must have both arms, the target must
            // be a tank, and both must be in the same hex.
            if (!ae.isLocationBad(Mech.LOC_RARM)
                    && !ae.isLocationBad(Mech.LOC_LARM)
                    && target instanceof Tank
                    && ae.getPosition().distance(target.getPosition()) == 0) {
                toHit.addModifier(2, "attacker is prone");
            } else {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
            }
        }

        // Check facing if the Mek is not prone.
        else if (!Compute.isInArc(ae.getPosition(), ae.getSecondaryFacing(),
                target.getPosition(), armArc)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // Attacks against adjacent buildings automatically hit.
        if (target.getTargetType() == Targetable.TYPE_BUILDING
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                || target instanceof GunEmplacement) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                    "Targeting adjacent building.");
        }

        final int armLoc = (arm == PunchAttackAction.RIGHT) ? Mech.LOC_RARM
                : Mech.LOC_LARM;

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            toHit.addModifier(2, "Upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            toHit.addModifier(2, "Lower arm actuator missing or destroyed");
        }

        if ( ae.hasFunctionalArmAES(armLoc) ) {
            toHit.addModifier(-1,"AES modifer");
        }

        // Claws replace Actuators, but they are Equipment vs System as they
        // take up multiple crits.
        // Rules state +1 bth with claws and if claws are critted then you get
        // the normal +1 bth for missing hand actuator.
        // Damn if you do damned if you dont. --Torren.
        final boolean hasClaws = ((BipedMech) ae).hasClaw(armLoc);
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc) && !hasClaws) {
            toHit.addModifier(1, "Hand actuator missing or destroyed");
        }
        if (hasClaws) {
            toHit.addModifier(1, "Using Claws");
        }

        // elevation
        if (attackerHeight == targetElevation && !ae.isHullDown()) {
            if (target.getHeight() == 0) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            if ( ae.isHullDown() ) {
                toHit.setHitTable(ToHitData.HIT_KICK);
            } else {
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            }
        }

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae, target));

        // done!
        return toHit;
    }

    /**
     * Damage that the specified mech does with a punch.
     */
    public static int getDamageFor(Entity entity, int arm,
            boolean targetInfantry) {
        final int armLoc = (arm == PunchAttackAction.RIGHT) ? Mech.LOC_RARM
                : Mech.LOC_LARM;
        int damage = (int) Math.ceil(entity.getWeight() / 10.0);

        // Rules state tonnage/7 for claws
        if (((BipedMech) entity).hasClaw(armLoc)) {
            damage = (int) Math.ceil(entity.getWeight() / 7.0);
        }

        float multiplier = 1.0f;

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            damage = 0;
        }
        if (entity.heat >= 9 && ((Mech) entity).hasTSM()) {
            multiplier *= 2.0f;
        }
        int toReturn = (int) Math.floor(damage * multiplier)
                + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
        // underwater damage is half, round up (see bug 1110692)
        if (entity.getLocationStatus(armLoc) == ILocationExposureStatus.WET) {
            toReturn = (int) Math.ceil(toReturn * 0.5f);
        }
        if (targetInfantry) {
            toReturn = Math.max(1, toReturn / 10);
        }
        return toReturn;
    }
}
