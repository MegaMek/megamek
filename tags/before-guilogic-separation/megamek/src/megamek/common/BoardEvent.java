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

package megamek.common;

public class BoardEvent
    extends java.util.EventObject
{
    public static final int        BOARD_HEX_CLICKED        = 0;
    public static final int        BOARD_HEX_DOUBLECLICKED    = 1;
    public static final int        BOARD_HEX_DRAGGED        = 2;
    
    public static final int        BOARD_HEX_CURSOR         = 3;
    public static final int        BOARD_HEX_HIGHLIGHTED    = 4;
    public static final int        BOARD_HEX_SELECTED        = 5;
    
    public static final int        BOARD_NEW_BOARD            = 6;
    //public static final int        BOARD_NEW_ENTITIES        = 7;
    
    public static final int        BOARD_CHANGED_HEX        = 9;
    public static final int        BOARD_CHANGED_ENTITY    = 10;

    public static final int        BOARD_FIRST_LOS_HEX        = 11;
    public static final int        BOARD_SECOND_LOS_HEX        = 12;
    public static final int            BOARD_NEW_ATTACK                = 13;

    private Coords        c;
    private Entity            entity;
    private int                type;
    private int                modifiers;
    
    public BoardEvent(Object source, Coords c, Entity entity, int type, int modifiers) {
        super(source);
        this.c = c;
        this.entity = entity;
        this.type = type;
        this.modifiers = modifiers;
    }
    
    /**
     * Returns the type of event that this is
     */
    public int getType() {
        return type;
    }
    
    /**
     * Returns the type of event that this is
     */
    public int getModifiers() {
        return modifiers;
    }
    
    /**
     * Returns the coordinate where this event occurred, if
     * applicable; null otherwise.
     */
    public Coords getCoords() {
        return c;
    }
    
    /**
     * Returns the entity associated with this event, if
     * applicable; null otherwise.
     */
    public Entity getEntity() {
        return entity;
    }
}
