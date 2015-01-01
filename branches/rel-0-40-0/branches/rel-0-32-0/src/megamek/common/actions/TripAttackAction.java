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
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.ToHitData;

/**
 * The attacker kicks the target.
 */
public class TripAttackAction extends PhysicalAttackAction
{
    
    public TripAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }
    
    public TripAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }
       
    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                      Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null)
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't attack from a null entity!");

        if(!game.getOptions().booleanOption("maxtech_new_physicals"))
            return new ToHitData(ToHitData.IMPOSSIBLE, "no MaxTech physicals");
        
        String impossible = toHitIsImpossible(game, ae, target);
        if(impossible != null) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "impossible");
        }

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getElevation();
        final int attackerHeight = attackerElevation + ae.getHeight();
        final int targetElevation = target.getElevation() + targHex.getElevation();

        ToHitData toHit;

        // non-mechs can't trip or be tripped
        if (!(ae instanceof BipedMech) || !(target instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Only biped mechs can trip other mechs");
        }

        // described as a leg hook / clothesline,
        // so it should need a working leg + a working arm
        // and 2 legs present
        if (ae.isLocationBad(Mech.LOC_LLEG)
            || ae.isLocationBad(Mech.LOC_RLEG)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Leg missing");
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if(range > 1) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target out of range");
        }

        int limb1 = Entity.LOC_NONE;
        int limb2 = Entity.LOC_NONE;
        // check elevation (target equal or 1 higher - ankle grab)
        if (attackerHeight < targetElevation || attackerElevation > targetElevation) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        } 

        // check facing
        if (!Compute.isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // can't trip while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        if(((Entity)target).isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
        }

        // check if attacker has fired leg-mounted weapons
        boolean usedWeapons[] = new boolean[ae.locations()];
        for(int i=0;i<ae.locations();i++) {
            usedWeapons[i] = false;
        }
        
        for (Mounted mounted : ae.getWeaponList()) {
            if (mounted.isUsedThisRound()) {
                int loc = mounted.getLocation();
                if(loc != Entity.LOC_NONE)
                    usedWeapons[loc] = true;
            }
        }
        
        //check for good hips / shoulders
        if(!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, Mech.LOC_RLEG))
            usedWeapons[Mech.LOC_RLEG] = true;
        if(!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, Mech.LOC_LLEG))
            usedWeapons[Mech.LOC_LLEG] = true;
        if(!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM))
            usedWeapons[Mech.LOC_RARM] = true;
        if(!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM))
            usedWeapons[Mech.LOC_LARM] = true;
        
        //to ankle grab, need both arms
        if(attackerElevation < targetElevation) { 
            if(usedWeapons[Mech.LOC_RARM] || usedWeapons[Mech.LOC_LARM]) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "both arms unusable");
            }
            limb1 = Mech.LOC_LARM;
            limb2 = Mech.LOC_RARM;
        } else {
            //normal attack uses one leg and one arm
            if(usedWeapons[Mech.LOC_RLEG]) {
                if(usedWeapons[Mech.LOC_LLEG]) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "both legs unusable");
                }
                limb1 = Mech.LOC_LLEG;
            }
            if(usedWeapons[Mech.LOC_RARM]) {
                if(usedWeapons[Mech.LOC_LARM]) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "both arms unusable");
                }
                limb2 = Mech.LOC_LARM;
            }
        }

        //Set the base BTH
        int base = 4;

        // Level 3 rule: the BTH is PSR - 1
        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
        }

        // Start the To-Hit
        toHit = new ToHitData(base, "base");
        
        setCommonModifiers(toHit, game, ae, target);
        
        // Get best leg
        if(limb1 == Entity.LOC_NONE) {
            ToHitData left = getLimbModifier(Mech.LOC_LLEG, ae);
            ToHitData right = getLimbModifier(Mech.LOC_RLEG, ae);
            if(left.getValue() < right.getValue())
                toHit.append(left);
            else
                toHit.append(right);
        } else {
            toHit.append(getLimbModifier(limb1, ae));
        }
        
        // Get best arm
        if(limb2 == Entity.LOC_NONE) {
            ToHitData left = getLimbModifier(Mech.LOC_LARM, ae);
            ToHitData right = getLimbModifier(Mech.LOC_RARM, ae);
            if(left.getValue() < right.getValue())
                toHit.append(left);
            else
                toHit.append(right);
        } else {
            toHit.append(getLimbModifier(limb2, ae));
        }
        
        // done!
        return toHit;
    }
    
    private static ToHitData getLimbModifier(int loc, Entity ae) {
        ToHitData toHit = new ToHitData();
        if(loc == Mech.LOC_LLEG || loc == Mech.LOC_RLEG) {
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
        }
        else if(loc == Mech.LOC_RARM || loc == Mech.LOC_LARM) {
            // damaged or missing actuators
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, loc)) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, loc)) {
                toHit.addModifier(2, "Lower arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, loc)) {
                toHit.addModifier(1, "Hand actuator destroyed");
            }
        }
        else toHit.addModifier(ToHitData.IMPOSSIBLE, "not limb");
        return toHit;
    }
}
