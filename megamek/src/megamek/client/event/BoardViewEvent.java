/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
    public static final int BOARD_HEX_DOUBLE_CLICKED = 1;
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
    private int mouseButton = 0;

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
        entityId = Entity.NONE;
    }

    public BoardViewEvent(Object source, int type, int entityId) {
        super(source);
        this.type = type;
        this.entityId = entityId;
    }

    public BoardViewEvent(Object source, Coords c, Entity entity, int type, int modifiers, int mouseButton) {
        this(source, c, entity, type, modifiers);
        this.mouseButton = mouseButton;
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

    /**
     * @return the id of the mouse button associated with this event if any.
     * <ul>
     * <li> 0 no button
     * <li> 1 Button 1
     * <li> 2 Button 2
     * <li> 3 Button 3
     * <li> 4 Button greater than 3
     * <li> 5 Button greater than 3
     * </ul>
     * <p>
     *
     */
    public int getButton() {
        return mouseButton ;
    }
}
