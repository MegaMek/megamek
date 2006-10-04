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

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Targetable;
import megamek.common.ToHitData;

/**
 * The attacker grapples the target.
 */
public class BreakGrappleAttackAction extends PhysicalAttackAction
{
    
    public BreakGrappleAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }
    
    public BreakGrappleAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }
       
    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * To-hit number
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                      Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null)
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't attack from a null entity!");

        if(!game.getOptions().booleanOption("maxtech_new_physicals"))
            return new ToHitData(ToHitData.IMPOSSIBLE, "no MaxTech physicals");
        
        String impossible = toHitIsImpossible(game, ae, target);
        if(impossible != null
                && !impossible.equals("Locked in Grapple")) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "impossible");
        }

        ToHitData toHit;

        // non-mechs can't grapple or be grappled
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Only mechs should be grappled");
        }
        
        if (((Mech)ae).getGrappled() != target.getTargetId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Not grappled");
        }
        
        //Set the base BTH
        int base = 5;

        // Level 3 rule: the BTH is PSR 
        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting();
        }

        // Start the To-Hit
        toHit = new ToHitData(base, "base");
        
        if(((Mech)ae).isGrappleAttacker()) {
            toHit.addModifier(ToHitData.AUTOMATIC_SUCCESS, "original attacker");
            return toHit;
        }
        
        setCommonModifiers(toHit, game, ae, target);
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left shoulder actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left lower arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
            toHit.addModifier(1, "Left hand actuator destroyed");
        }
        
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)) {
            toHit.addModifier(2, "Right shoulder actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
            toHit.addModifier(2, "Right upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
            toHit.addModifier(2, "Right lower arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
            toHit.addModifier(1, "Right hand actuator destroyed");
        }
        
        //Weight class difference
        int wmod = ((Entity)target).getWeightClass() - ae.getWeightClass();
        if(wmod != 0) {
            toHit.addModifier(wmod, "Weight class difference");
        }
        // done!
        return toHit;
    }
    
}
