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

import java.util.Enumeration;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;

/**
 * The attacker kicks the target.
 */
public class KickAttackAction extends AbstractAttackAction
{
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
     * Damage that the specified mech does with a kick
     */
    public static int getDamageFor(Entity entity, int leg) {
        int[] kickLegs = new int[2];
        if ( entity.entityIsQuad() ) {
          kickLegs[0] = Mech.LOC_RARM;
          kickLegs[1] = Mech.LOC_LARM;
        } else {
          kickLegs[0] = Mech.LOC_RLEG;
          kickLegs[1] = Mech.LOC_LLEG;
        }

        final int legLoc = (leg == KickAttackAction.RIGHT) ? kickLegs[0] : kickLegs[1];
        int damage = (int)Math.floor(entity.getWeight() / 5.0);
        float multiplier = 1.0f;

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_HIP, legLoc)) {
            damage = 0;
        }
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            multiplier *= 2.0f;
        }
        int toReturn = (int)Math.floor(damage * multiplier) + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
        // underwater damage is half, round up (see bug 1110692)
        if (entity.getLocationStatus(legLoc) == ILocationExposureStatus.WET) {
            toReturn = (int)Math.ceil(toReturn * 0.5f);
        }
        return toReturn;
    }
    
    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()), getLeg());
    }

    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                      Targetable target, int leg) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(te.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getElevation();
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation() + targHex.getElevation();
        final int targetHeight = targetElevation + target.getHeight();
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.getBoard().getBuildingAt( te.getPosition() );
        }
        int mule = 0;
        boolean mulekick = game.getOptions().booleanOption("maxtech_mulekicks");
        int[] kickLegs = new int[2];
        if ( ae.entityIsQuad() ) {
            if (mulekick && (leg==KickAttackAction.LEFTMULE || leg==KickAttackAction.RIGHTMULE)) {
                kickLegs[0] = Mech.LOC_RLEG;
                kickLegs[1] = Mech.LOC_LLEG;
                mule = 1; // To-hit modifier
            } else {
                kickLegs[0] = Mech.LOC_RARM;
                kickLegs[1] = Mech.LOC_LARM;
            }
        } else {
            kickLegs[0] = Mech.LOC_RLEG;
            kickLegs[1] = Mech.LOC_LLEG;
        }
        final int legLoc = 
            ( (leg == KickAttackAction.RIGHTMULE) || (leg == KickAttackAction.RIGHT) ) ? kickLegs[0] : kickLegs[1];

        ToHitData toHit;

        // arguments legal?
        // By allowing mulekicks, this gets a little more complicated :(
        if (leg != KickAttackAction.RIGHT && leg != KickAttackAction.LEFT) {
            if (!game.getOptions().booleanOption("maxtech_mulekicks")) {
                throw new IllegalArgumentException("Leg must be LEFT or RIGHT");
            } else if (leg != KickAttackAction.RIGHTMULE && leg != KickAttackAction.LEFTMULE) {
                throw new IllegalArgumentException("Leg must be one of LEFT, RIGHT, LEFTMULE, or RIGHTMULE");
            }
        }
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-mechs can't kick
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't kick");
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

        // check if both legs are present & working
        if (ae.isLocationBad(kickLegs[0])
            || ae.isLocationBad(kickLegs[1])) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Leg missing");
        }

        // check if both hips are operational
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, kickLegs[0])
            || !ae.hasWorkingSystem(Mech.ACTUATOR_HIP, kickLegs[1])) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Hip destroyed");
        }

        // check if attacker has fired leg-mounted weapons
        for (Enumeration i = ae.getWeapons(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.isUsedThisRound() && mounted.getLocation() == legLoc) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from leg this turn");
            }
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if ( range > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        else if ( te instanceof Infantry && 1 == range ) {
            // As per Randall in the post, http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=626894&page=1&view=collapsed&sb=5&o=0&fpart=
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Can only stomp Infantry in same hex");
        }

        // check elevation
        if (attackerElevation < targetElevation || attackerElevation > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
        // Don't check arc for stomping infantry or tanks.
        if (0 != range && mule != 1 &&
            !Compute.isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // check facing, part 2: Mule kick
        if (0 != range && mule == 1 &&
            !Compute.isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_REAR) &&
            !Compute.isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_LEFTSIDE) &&
            !Compute.isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_RIGHTSIDE)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // can't kick while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // Can't target units in buildings (from the outside).
        if ( 0 != range && targetInBuilding ) {
            if ( !Compute.isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.getBoard().getBuildingAt( ae.getPosition() )
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
        int base = 3;

        // Level 3 rule: the BTH is PSR - 2
        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 2;
        }

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        // BMR(r), page 33. +3 modifier for kicking infantry.
        if ( te instanceof Infantry ) {
            toHit.addModifier( 3, "Stomping Infantry" );
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

        // Mulekick?
        if (mulekick && mule!=0) {
            toHit.addModifier(mule, "Quad Mek making a mule kick");
        }
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
            toHit.addModifier(2, "Upper leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
            toHit.addModifier(2, "Lower leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_FOOT, legLoc)) {
            toHit.addModifier(1, "Foot actuator destroyed");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        if (te.height() > 0 && targHex.terrainLevel(Terrains.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        toHit.append(nightModifiers(game, target, null));
        
        // target immobile
        toHit.append(Compute.getImmobileMod(te));

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

        // BMRr pg. 42, "The side on which a vehicle takes damage is determined
        // randomly if the BattleMech is attacking from the same hex."
        if ( 0 == range && te instanceof Tank ) {
            toHit.setSideTable( ToHitData.SIDE_RANDOM );
        }

        // done!
        return toHit;
    }
}
