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
import megamek.common.Game;
import megamek.common.Targetable;

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
    
    public Targetable getTarget(Game g) {
        return g.getTarget(getTargetType(), getTargetId());
    }
    
    public Entity getEntity(Game g) {
        return g.getEntity(getEntityId());
    }
    
}
