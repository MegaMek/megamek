/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.event;

import megamek.common.Coords;
import megamek.common.Entity;

/**
 * Instances of this class are sent as a result of changes in BoardView
 * 
 * @see BoardViewListener
 */
public class BoardViewEvent extends java.util.EventObject {
    /**
     * 
     */
    private static final long serialVersionUID = -4823618884833399318L;
    public static final int BOARD_HEX_CLICKED = 0;
    public static final int BOARD_HEX_DOUBLECLICKED = 1;
    public static final int BOARD_HEX_DRAGGED = 2;

    public static final int BOARD_HEX_CURSOR = 3;
    public static final int BOARD_HEX_HIGHLIGHTED = 4;
    public static final int BOARD_HEX_SELECTED = 5;

    public static final int BOARD_FIRST_LOS_HEX = 6;
    public static final int BOARD_SECOND_LOS_HEX = 7;

    public static final int FINISHED_MOVING_UNITS = 8;
    public static final int SELECT_UNIT = 9;
    public static final int BOARD_HEX_POPUP = 10;

    private Coords c;
    private Entity entity;
    private int type;
    private int modifiers;
    private int entityId;

    public BoardViewEvent(Object source, Coords c, Entity entity, int type,
            int modifiers) {
        super(source);
        this.c = c;
        this.entity = entity;
        this.type = type;
        this.modifiers = modifiers;
    }

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

    /**
     * Returns the type of event that this is
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * @return the coordinate where this event occurred, if applicable;
     *         <code>null</code> otherwise.
     */
    public Coords getCoords() {
        return c;
    }

    /**
     * @return the entity associated with this event, if applicable;
     *         <code>null</code> otherwise.
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * @return the entity ID associated with this event, if applicable; 0
     *         otherwise.
     */
    public int getEntityId() {
        return entityId;
    }
}
