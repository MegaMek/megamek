/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Protomech;
import megamek.common.Targetable;
import megamek.common.Terrain;
import megamek.common.ToHitData;

/**
 * The attacking Protomech makes it's combo physical attack action.
 */
public class ProtomechPhysicalAttackAction
    extends AbstractAttackAction
{
 
    public ProtomechPhysicalAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }
    
    public ProtomechPhysicalAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }
    
    /**
     * Damage a Protomech does with its Combo-physicalattack.
     */
    public static int getDamageFor(Entity entity) {
        int toReturn;
        if (entity.getWeight() >= 2 && entity.getWeight() < 6) {
            toReturn = 1;
        } else toReturn = 2;
        // underwater damage is half, round up (see bug 1110692)
        if (entity.getLocationStatus(Protomech.LOC_TORSO) == Entity.LOC_WET) {
            toReturn = (int)Math.ceil(toReturn * 0.5f);
        }
        return toReturn;
    }
    
    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), 
            game.getTarget(getTargetType(), getTargetId()));
    }

    
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        final int nightModifier = (game.getOptions().booleanOption("night_battle")) ? +2 : 0;
        ToHitData toHit;

        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-protos can't make protomech-physicalattacks
        if (!(ae instanceof Protomech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-protos can't make proto-physicalattacks");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
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
        final int range = ae.getPosition().distance(target.getPosition());
        if ( range > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // check elevation
        if (attackerElevation < targetElevation || attackerElevation > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }
        
        //can only target targets in adjacent hexes, not in same hex
        if (range == 0) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in adjacent hex");
        }
        
        // check facing
        // Don't check arc for stomping infantry or tanks.
        if (0 != range &&
            !Compute.isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // Can't target units in buildings (from the outside).
        if ( 0 != range && targetInBuilding ) {
            if ( !Compute.isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
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

        //Set the base BTH
        int base = 4;

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te));

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

        if (nightModifier>0) {
            toHit.addModifier(nightModifier, "Night Battle, no Spotlight");
        }
        
        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // elevation
        if (attackerElevation < targetHeight) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else if (te.height() > 0) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae,te));

        // done!
        return toHit;
    }

}