/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */

/*
 * ChargeAttackAction.java
 * 
 * Created on March 12, 2002, 3:23 PM
 */

package megamek.common.actions;

import java.util.Enumeration;

import megamek.common.*;

/**
 * Represents one unit charging another. Stores information about where the
 * target is supposed to be for the charge to be successful, as well as normal
 * attack info.
 * 
 * @author Ben Mazur
 */
public class ChargeAttackAction extends DisplacementAttackAction {

    public ChargeAttackAction(Entity attacker, Targetable target) {
        this(attacker.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
    }

    public ChargeAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     */
    public ToHitData toHit(Game game) {
        return toHit(game, false);
    }
    
    public ToHitData toHit(Game game, boolean skid) {
        final Entity entity = game.getEntity(getEntityId());
        return toHit(game, game.getTarget(getTargetType(), getTargetId()), entity.getPosition(), entity.moved, skid);
    }
    
    public ToHitData toHit(Game game, Targetable target, Coords src, int movement) {
        return toHit(game, target, src, movement, false);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     */
    public ToHitData toHit(Game game, Targetable target, Coords src, int movement, boolean skid) {
        final Entity ae = getEntity(game);

        // arguments legal?
        if (ae == null) {
            throw new IllegalStateException("Attacker is null");
        }

        // Do to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is null");
        }

        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.elevationOccupied(game.board.getHex(src));
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation();
        final int targetHeight = target.absHeight();
        Building bldg = game.board.getBuildingAt( this.getTargetPos() );
        ToHitData toHit = null;

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // Can't target a transported entity.
        if (te != null && Entity.NONE != te.getTransportId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a entity conducting a swarm attack.
        if (te != null && Entity.NONE != te.getSwarmTargetId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // mechs can only charge standing mechs
        if (ae instanceof Mech) {
            if (te != null && !(te instanceof Mech)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is not a mech");
            }
            if (te != null && te.isProne()) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
            }
        } else if (te instanceof Infantry) {
            // Can't charge infantry.
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is infantry");
        } else if (te instanceof Protomech) {
            // Can't charge protomechs.
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is protomech");
        }

        // target must be within 1 elevation level
        if (attackerElevation > targetHeight || attackerHeight < targetElevation) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be within 1 elevation level");
        }

        // can't charge while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // can't attack mech making a different displacement attack
        if (te != null && te.hasDisplacementAttack()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }

        // target must have moved already, unless it's a skid charge
        if (te != null && !te.isDone() && !skid) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be done with movement");
        }

        // can't attack the target of another displacement attack
        if (te != null
            && te.isTargetOfDisplacementAttack()
            && te.findTargetedDisplacement().getEntityId() != ae.getId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if (null != bldg) {
            if (!Compute.isInBuilding(game, ae)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building");
            } else if (!game.board.getBuildingAt(ae.getPosition()).equals(bldg)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building");
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if (target.getTargetType() == Targetable.TYPE_BUILDING) {
            return new ToHitData(ToHitData.AUTOMATIC_SUCCESS, "Targeting adjacent building.");
        }

        // Can't target woods or ignite a building with a physical.
        if (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE
            || target.getTargetType() == Targetable.TYPE_HEX_CLEAR
            || target.getTargetType() == Targetable.TYPE_HEX_IGNITE) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
        int base = 5;

        if (game.getOptions().booleanOption("maxtech_physical_BTH")) {
            base = ae.getCrew().getPiloting();
        }

        toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, ae.getId(), movement));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, ae.getId()));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te));

        // piloting skill differential
        if (ae.getCrew().getPiloting() != te.getCrew().getPiloting()) {
            toHit.addModifier(ae.getCrew().getPiloting() - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // determine hit direction
        toHit.setSideTable(Compute.targetSideTable(src, te.getPosition(), te.getFacing(), te instanceof Tank));

        // all charges resolved against full-body table, except vehicles
        // and charges against mechs in water partial cover
        if (targHex.levelOf(Terrain.WATER) == te.height() && te.height() > 0) {
        	toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else if (ae.getHeight() < target.getHeight()) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else {
        	toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        // done!
        return toHit;
    }

    /**
     * Checks if a charge can hit the target, taking account of movement
     */
    public ToHitData toHit(Game game, MovePath md) {
        final Entity ae = game.getEntity(getEntityId());
        final Targetable target = getTarget(game);
        Coords chargeSrc = ae.getPosition();
        MoveStep chargeStep = null;

        // let's just check this
        if (!md.contains(MovePath.STEP_CHARGE)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Charge action not found in movment path");
        }

        // no jumping
        if (md.contains(MovePath.STEP_START_JUMP)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "No jumping allowed while charging");
        }

        // no backwards
        if (md.contains(MovePath.STEP_BACKWARDS)
            || md.contains(MovePath.STEP_LATERAL_LEFT_BACKWARDS)
            || md.contains(MovePath.STEP_LATERAL_RIGHT_BACKWARDS)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "No backwards movement allowed while charging");
        }

        // determine last valid step
        md.compile(game, ae);
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep) i.nextElement();
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                if (step.getType() == MovePath.STEP_CHARGE) {
                    chargeStep = step;
                } else {
                    chargeSrc = step.getPosition();
                }
            }
        }

        // need to reach target
        if (chargeStep == null || !target.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Could not reach target with movement");
        }

        return toHit(game, target, chargeSrc, chargeStep.getMovementType());
    }

    /**
      * Damage that a mech does with a successful charge.  Assumes that 
      * delta_distance is correct.
      */
    public static int getDamageFor(Entity entity) {
        return getDamageFor(entity, entity.delta_distance);
    }

    public static int getDamageFor(Entity entity, int hexesMoved) {
        return (int) Math.ceil(
            (entity.getWeight() / 10.0) * (hexesMoved - 1) * (entity.getLocationStatus(1) == Entity.LOC_WET ? 0.5 : 1));
    }

    /**
     * Damage that a mech suffers after a successful charge.
     */
    public static int getDamageTakenBy(Entity entity, Building bldg) {
        // ASSUMPTION: 10% of buildings CF at start of phase, round up.
        return (int) Math.ceil(bldg.getPhaseCF() / 10.0);
    }

    public static int getDamageTakenBy(Entity entity, Entity target) {
        return getDamageTakenBy (entity, target, false, 0);
    }
    
    public static int getDamageTakenBy(Entity entity, Entity target, boolean maxtech) {
        return getDamageTakenBy (entity, target, maxtech, entity.delta_distance);
    }
    
    public static int getDamageTakenBy(Entity entity, Entity target, boolean maxtech, int distance) {
        if (!maxtech) {
            return (int) Math.ceil(target.getWeight() / 10.0 * (entity.getLocationStatus(1) == Entity.LOC_WET ? 0.5 : 1));
        } else {
            return (int) Math.floor(target.getWeight() / 20.0 * distance * (entity.getLocationStatus(1) == Entity.LOC_WET ? 0.5 : 1));
        }
    }

}
