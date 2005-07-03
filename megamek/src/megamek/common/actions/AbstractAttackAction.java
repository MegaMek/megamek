/**
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
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.AmmoType;

/**
 * Abstract superclass for any action where an entity is attacking another
 * entity.
 */
public abstract class AbstractAttackAction
    extends AbstractEntityAction
    implements AttackAction
{
    private int targetType;
    private int targetId;
    
    // default to attacking an entity, since this is what most of them are
    public AbstractAttackAction(int entityId, int targetId) {
        super(entityId);
        this.targetType = Targetable.TYPE_ENTITY;
        this.targetId = targetId;
    }
    
    public AbstractAttackAction(int entityId, int targetType, int targetId) {
        super(entityId);
        this.targetType = targetType;
        this.targetId = targetId;
    }
    
    public int getTargetType() {
        return targetType;
    }
    
    public int getTargetId() {
        return targetId;
    }
    
    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }
    
    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
    
    public Targetable getTarget(IGame g) {
        return g.getTarget(getTargetType(), getTargetId());
    }
    
    public Entity getEntity(IGame g) {
        return g.getEntity(getEntityId());
    }
    
    /**
     * used by the toHit of derived classes
     * atype may be null if not using an ammo based weapon
    */
    public static ToHitData nightModifiers(IGame game, Targetable target, AmmoType atype) {
        ToHitData toHit = null;
        if(game.getOptions().booleanOption("night_battle")) {
            Entity te = null;
            if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
                te = (Entity) target;
            }
            toHit = new ToHitData();

            //The base night penalty
            int night_modifier;
            if(game.getOptions().booleanOption("dusk")) {
                night_modifier = 1;
                toHit.addModifier(night_modifier, "Dusk");
            } else {
                night_modifier = 2;
                toHit.addModifier(night_modifier, "Night");
            }

            //Searchlights reduce the penalty to zero
            if(te!=null && te.isUsingSpotlight()) {
                toHit.addModifier(-night_modifier, "target using searchlight");
            }
            else if(te!=null && te.isIlluminated()) {
                toHit.addModifier(-night_modifier, "target illuminated by searchlight");
            }
            //So do flares
            else if(game.isPositionIlluminated(target.getPosition())) {
                toHit.addModifier(-night_modifier, "target illuminated by flare");
            }
            //Certain ammunitions reduce the penalty
            else if(atype != null) {
                if(atype.getAmmoType() == AmmoType.T_AC &&
                   (atype.getMunitionType() == AmmoType.M_INCENDIARY_AC || 
                   atype.getMunitionType() == AmmoType.M_TRACER)) {
                    toHit.addModifier(-1, "incendiary/tracer ammo");
                    night_modifier--;
                }
            }
        }
        return toHit;
    }
    
}
