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
 * The attacker grapples the target.
 */
public class GrappleAttackAction extends PhysicalAttackAction
{
    
    public GrappleAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }
    
    public GrappleAttackAction(int entityId, int targetType, int targetId) {
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

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getElevation();
        //final int attackerHeight = attackerElevation + ae.getHeight();
        final int targetElevation = target.getElevation() + targHex.getElevation();
        //final int targetHeight = targetElevation + target.getHeight();
        ToHitData toHit;

        // non-mechs can't grapple or be grappled
        if (!(ae instanceof BipedMech) || !(target instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Only biped mechs can trip other mechs");
        }

        final boolean counter = ((Mech)ae).getGrappled() != Entity.NONE
        && !((Mech)ae).isGrappleAttacker();

        // requires 2 good arms
        if (ae.isLocationBad(Mech.LOC_LARM)
            || ae.isLocationBad(Mech.LOC_RARM)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
        }

        if(!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)
                || !ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder missing/destroyed");
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if(range != 1 && !counter) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        } 

        // check elevation (attacker must be able to enter target hex)
        if (Math.abs(attackerElevation - targetElevation) > ae.getMaxElevationChange()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        } 

        // check facing
        if (!counter && !Compute.isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // can't grapple while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        if(((Entity)target).isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
        }
        
        // check if attacker has fired any weapons
        if(!counter) {
            for (Mounted mounted : ae.getWeaponList()) {
                if (mounted.isUsedThisRound()) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Fired weapons");
                }
            }
        }
        
        //already done?
        int atGr = ((Mech)ae).getGrappled();
        int deGr = ((Mech)target).getGrappled();
        if((atGr != Entity.NONE
                || deGr != Entity.NONE)
                && atGr != target.getTargetId()
                && ((Mech)target).isGrappleAttacker()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Already grappled");
        }

        //Set the base BTH
        int base = 5;

        // Level 3 rule: the BTH is PSR 
        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting();
        }

        // Start the To-Hit
        toHit = new ToHitData(base, "base");
        
        setCommonModifiers(toHit, game, ae, target);
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left lower arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
            toHit.addModifier(1, "Left hand actuator destroyed");
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
