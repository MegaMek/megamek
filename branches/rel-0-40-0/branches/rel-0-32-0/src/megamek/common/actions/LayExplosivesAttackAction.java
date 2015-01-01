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

import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MiscType;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.ToHitData;

public class LayExplosivesAttackAction extends AbstractAttackAction
{    
   
    public LayExplosivesAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }
    
    public LayExplosivesAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }
    
    /**
     * Damage that the specified platoon does with explosives
     */
    public static int getDamageFor(Entity entity) {
        if(!(entity instanceof Infantry))
            return 0;
        Infantry inf = (Infantry)entity; 
        return inf.getDamage(inf.getShootingStrength()) << (inf.turnsLayingExplosives - 1);
    }
    
    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * To-hit number, i.e. is the action possible
     */
    public static ToHitData toHit(IGame game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        if ((target.getTargetType() != Targetable.TYPE_BUILDING) || (target.getTargetType() != Targetable.TYPE_FUEL_TANK)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can only target buildings");
        }
        final Building b = game.getBoard().getBuildingAt(ae.getPosition());
        if (ae == null)
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't attack from a null entity!");
//        if(b == null || b.getId() != target.getTargetId())
//            return new ToHitData(ToHitData.IMPOSSIBLE, "Target out of range");
        if(!(ae instanceof Infantry))
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is not infantry");
        Infantry inf = (Infantry) ae;
        if(inf.turnsLayingExplosives > 0)
            return new ToHitData(ToHitData.AUTOMATIC_SUCCESS, "STOP: Expected Damage: "+getDamageFor(ae));
        boolean ok = false;
        for (Mounted m : ae.getMisc()) {
            if(m.getType().hasFlag(MiscType.F_TOOLS) && m.getType().hasSubType(MiscType.S_DEMOLITION_CHARGE)) {
                ok = true;
                break;
            }
        }
        if(!ok)
            return new ToHitData(ToHitData.IMPOSSIBLE, "No explosives carried");
        return new ToHitData(ToHitData.AUTOMATIC_SUCCESS, "START: Can't move or fire while laying explosives");
    }
}
