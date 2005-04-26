/**
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

public class BoardViewEvent
    extends java.util.EventObject
{
    public static final int     FINISHED_MOVING_UNITS   = 0;
    public static final int     SELECT_UNIT     = 1;

    private int                 type;
    private int                 entityId;
    
    public BoardViewEvent(Object source, int type) {
        super(source);
        this.type = type;
        entityId = megamek.common.Entity.NONE;
    }
    
    public BoardViewEvent(Object source, int type, int entityId) {
        super(source);
        this.type = type;
        this.entityId = entityId;
    }

    /**
     * Returns the type of event that this is
     */
    public int getType() {
        return type;
    }    

    public int getEntityId() {
        return entityId;
    }    
}
