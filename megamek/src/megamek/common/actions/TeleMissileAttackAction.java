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
 * TeleMissileAttackAction.java
 * 
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
public class TeleMissileAttackAction extends AbstractAttackAction {

    public TeleMissileAttackAction(Entity attacker, Targetable target) {
        super(attacker.getId(), target.getTargetType(), target.getTargetId());
    }

    public static int getDamageFor(Entity entity) {  	
        if(entity instanceof TeleMissile) {
            return ((TeleMissile)entity).getDamageValue();
        } else {
            return 0;
        }
    }
    
    /**
     * To-hit number for a charge, assuming that movement has been handled
     */
    public ToHitData toHit(IGame game) {
        return toHit(game, game.getTarget(getTargetType(), getTargetId()));
    }
    
    public ToHitData toHit(IGame game, Targetable target) {
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

        //set the to-hit
        ToHitData toHit = new ToHitData(2, "base");

        TeleMissile tm = (TeleMissile)ae;
        
        //thrust used
        if(ae.mpUsed > 0) 
            toHit.addModifier(ae.mpUsed, "thrust used");
        
        //out of fuel
        if(tm.getFuel() <= 0) 
        	toHit.addModifier(+6, "out of fuel");
        
        //modifiers for the originating unit need to be added later, because
        //they may change as a result of damage
        
        // done!
        return toHit;
    }

}
