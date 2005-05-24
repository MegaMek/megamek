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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;

/**
 * The attacker punches the target.
 */
public class PunchAttackAction
    extends AbstractAttackAction
{
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    
    private int arm;
    
    public PunchAttackAction(int entityId, int targetId, int arm) {
        super(entityId, targetId);
        this.arm = arm;
    }
    
    public PunchAttackAction(int entityId, int targetType, int targetId, int arm) {
        super(entityId, targetType, targetId);
        this.arm = arm;
    }
    
    public int getArm() {
        return arm;
    }
    
    public void setArm(int arm) {
        this.arm = arm;
    }
    
    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId() ),
                getArm());
    }

    /**
     * To-hit number for the specified arm to punch
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                       Targetable target, int arm) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerHeight = ae.absHeight();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final int armLoc = (arm == PunchAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        final int armArc = (arm == PunchAttackAction.RIGHT)
                           ? Compute.ARC_RIGHTARM : Compute.ARC_LEFTARM;
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.getBoard().getBuildingAt( te.getPosition() );
        }
        final int nightModifier = (game.getOptions().booleanOption("night_battle")) ? +2 : 0;
        ToHitData toHit;

        // arguments legal?
        if (arm != PunchAttackAction.RIGHT && arm != PunchAttackAction.LEFT) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-mechs can't punch
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't punch");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        //Quads can't punch
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        //Can't punch with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arms are flipped to the rear. Can not punch.");
        }

        // check if arm is present
        if (ae.isLocationBad(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
        }

        // check if shoulder is functional
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder destroyed");
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
        }

        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // check elevation
        if (attackerHeight < targetElevation || attackerHeight > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            if ( !Compute.isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.getBoard().getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }


        //Set the base BTH
          int base = 4;

          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
          }

          toHit = new ToHitData(base, "base");

        // Prone Meks can only punch vehicles in the same hex.
        if (ae.isProne()) {
            // The Mek must have both arms, the target must
            // be a tank, and both must be in the same hex.
            if ( !ae.isLocationBad(Mech.LOC_RARM) &&
                 !ae.isLocationBad(Mech.LOC_LARM) &&
                 te instanceof Tank &&
                 ae.getPosition().distance( target.getPosition() ) == 0 ) {
                toHit.addModifier( 2, "attacker is prone" );
            } else {
                return new ToHitData(ToHitData.IMPOSSIBLE,"Attacker is prone");
            }
        }

        // Check facing if the Mek is not prone.
        else if ( !Compute.isInArc(ae.getPosition(), ae.getSecondaryFacing(),
                           target.getPosition(), armArc) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }


        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te));

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            toHit.addModifier(2, "Upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            toHit.addModifier(2, "Lower arm actuator missing or destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc)) {
            toHit.addModifier(1, "Hand actuator missing or destroyed");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        IHex targHex = game.getBoard().getHex(te.getPosition());
        if (te.height() > 0 && targHex.terrainLevel(Terrains.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        if (nightModifier>0) {
            toHit.addModifier(nightModifier, "Night Battle, no Spotlight");
        }
        
        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // elevation
        if (attackerHeight == targetElevation) {
            if (te.height() == 0) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae,te));

        // done!
        return toHit;
    }

    /**
     * Damage that the specified mech does with a punch.
     */
    public static int getDamageFor(Entity entity, int arm) {
        final int armLoc = (arm == PunchAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        int damage = (int)Math.ceil(entity.getWeight() / 10.0);
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
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            multiplier *= 2.0f;
        }
        int toReturn = (int)Math.floor(damage * multiplier) + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
        // underwater damage is half, round up (see bug 1110692)
        if (entity.getLocationStatus(armLoc) == Entity.LOC_WET) {
            toReturn = (int)Math.ceil(toReturn * 0.5f);
        }
        return toReturn;
    }
}
