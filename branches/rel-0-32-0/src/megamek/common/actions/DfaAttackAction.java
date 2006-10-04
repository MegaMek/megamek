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

/*
 * DfaAttackAction.java
 *
 * Created on March 16, 2002, 11:43 AM
 */

package megamek.common.actions;

import java.util.Enumeration;

import megamek.common.*;

/**
 *
 * @author  Ben
 * @version 
 */
public class DfaAttackAction extends DisplacementAttackAction {

    /** Creates new DfaAttackAction */
    public DfaAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public DfaAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }
    
    /**
     * Damage done to a mech after a successful DFA.
     */
    public static int getDamageTakenBy(Entity entity) {
        return (int)Math.ceil(entity.getWeight() / 5.0);
    }
    
    /**
     * Damage that a mech does with a successful DFA.
     */
    public static int getDamageFor(Entity entity) {
        return (int)Math.ceil((entity.getWeight() / 10.0) * 3.0);
    }
    
    /**
     * Checks if a death from above attack can hit the target, including movement
     */
    public static ToHitData toHit(IGame game, int attackerId, Targetable target, MovePath md) {
        final Entity ae = game.getEntity(attackerId);

        // Do to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is null");
        }

        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
        }
        Coords chargeSrc = ae.getPosition();
        MoveStep chargeStep = null;

        // Infantry CAN'T dfa!!!
        if ( ae instanceof Infantry ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't D.F.A.");
        }

        if ( ae.getJumpType() == Mech.JUMP_BOOSTER) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Can't D.F.A. using mechanical jump boosters.");
        }

        // let's just check this
        if (!md.contains(MovePath.STEP_DFA)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. action not found in movment path");
        }

        // have to jump
        if (!md.contains(MovePath.STEP_START_JUMP)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. must involve jumping");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // determine last valid step
        md.compile(game, ae);
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            if (!step.isLegal()) {
                break;
            }
			if (step.getType() == MovePath.STEP_DFA) {
			    chargeStep = step;
			} else {
			    chargeSrc = step.getPosition();
			}
        }

        // need to reach target
        if (chargeStep == null || !target.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Could not reach target with movement");
        }

        // target must have moved already
        if ( te != null && !te.isDone() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be done with movement");
        }

        return toHit(game, attackerId, target, chargeSrc);
    }

    public ToHitData toHit(IGame game) {
        final Entity entity = game.getEntity(getEntityId());
        return toHit(game, getEntityId(),
                        game.getTarget(getTargetType(), getTargetId()),
                        entity.getPosition());
    }

    /**
     * To-hit number for a death from above attack, assuming that movement has
     * been handled
     */
    public static ToHitData toHit(IGame game, int attackerId, Targetable target, Coords src) {
        final Entity ae = game.getEntity(attackerId);
        
        // arguments legal?
        if ( ae == null ) {
            throw new IllegalArgumentException("Attacker is null");
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
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        ToHitData toHit = null;

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // Infantry CAN'T dfa!!!
        if ( ae instanceof Infantry ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't dfa");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // can't dfa while prone, even if you somehow did manage to jump
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // can't attack mech making a different displacement attack
        if ( te != null && te.hasDisplacementAttack() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }

        // can't attack the target of another displacement attack
        if ( te != null && te.isTargetOfDisplacementAttack() &&
             te.findTargetedDisplacement().getEntityId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
        }

        // Attacks against adjacent buildings automatically hit.
        if (target.getTargetType() == Targetable.TYPE_BUILDING
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                || target instanceof GunEmplacement) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
        int base = 5;

        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting();
        }

        toHit = new ToHitData(base, "base");

          // BMR(r), page 33. +3 modifier for DFA on infantry.
        if ( te instanceof Infantry ) {
            toHit.addModifier( 3, "Infantry target" );
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId, IEntityMovementType.MOVE_JUMP));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // piloting skill differential
        if (ae.getCrew().getPiloting() != te.getCrew().getPiloting()) {
            toHit.addModifier(ae.getCrew().getPiloting() - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // If it has a torso-mounted cockpit and two head sensor hits or three sensor hits...
        // It gets a =4 penalty for being blind!
        if ((ae instanceof Mech) && (((Mech)ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        toHit.append(nightModifiers(game, target, null, ae));
        
        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        if (te instanceof Tank) {
            toHit.setSideTable(ToHitData.SIDE_FRONT);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        else if (te.isProne()) {
            toHit.setSideTable(ToHitData.SIDE_REAR);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        else {
            toHit.setSideTable(te.sideTable(src));
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }

        // done!
        return toHit;
    }

}
