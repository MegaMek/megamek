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

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.TeleMissile;
import megamek.common.ToHitData;

/**
 * Represents one tele-controlled missile attack
 * 
 * @author Ben Mazur
 */
public class TeleMissileAttackAction extends AbstractAttackAction {

    /**
     * 
     */
    private static final long serialVersionUID = -1054613811287285482L;

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
        
        if (!game.getOptions().booleanOption("friendly_fire")) {
            // a friendly unit can never be the target of a direct attack.
            if (target.getTargetType() == Targetable.TYPE_ENTITY
                    && (((Entity)target).getOwnerId() == ae.getOwnerId()
                            || (((Entity)target).getOwner().getTeam() != Player.TEAM_NONE
                                    && ae.getOwner().getTeam() != Player.TEAM_NONE
                                    && ae.getOwner().getTeam() == ((Entity)target).getOwner().getTeam())))
                return new ToHitData(TargetRoll.IMPOSSIBLE, "A friendly unit can never be the target of a direct attack.");
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
