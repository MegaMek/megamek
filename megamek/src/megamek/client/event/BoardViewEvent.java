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

import megamek.client.ui.swing.boardview.BoardView;
import megamek.common.BoardLocation;
import megamek.common.Coords;
import megamek.common.Entity;

import java.io.Serial;
import java.util.Optional;

/**
 * Instances of this class are sent as a result of changes in BoardView
 *
 * @see BoardViewListener
 */
public class BoardViewEvent extends java.util.EventObject {
    @Serial
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

    private Coords coords;
    private final int type;
    private int modifiers;
    private int entityId;
    private int mouseButton = 0;

    public BoardViewEvent(BoardView source, Coords coords, int type, int modifiers) {
        super(source);
        this.coords = coords;
        this.type = type;
        this.modifiers = modifiers;
    }

    public BoardViewEvent(BoardView source, int type) {
        this(source, type, Entity.NONE);
    }

    public BoardViewEvent(BoardView source, int type, int entityId) {
        super(source);
        this.type = type;
        this.entityId = entityId;
    }

    public BoardViewEvent(BoardView source, Coords coords, int type, int modifiers, int mouseButton) {
        this(source, coords, type, modifiers);
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
     * @return the coordinate where this event occurred, if applicable; null otherwise.
     */
    public Coords getCoords() {
        return coords;
    }

    /**
     * @return the entity ID associated with this event, if applicable; 0 otherwise.
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * @return the id of the mouse button associated with this event if any.
     *       <ul>
     *       <li> 0 no button
     *       <li> 1 Button 1
     *       <li> 2 Button 2
     *       <li> 3 Button 3
     *       <li> 4 Button greater than 3
     *       <li> 5 Button greater than 3
     *       </ul>
     *       <p>
     */
    public int getButton() {
        return mouseButton;
    }

    /**
     * Returns this event's position as a board location with board ID. If this event has no coords (depending on its
     * type or the coords are null for some other reason, the returned Optional is empty.
     *
     * @return This event's location
     */
    public Optional<BoardLocation> getBoardLocation() {
        if (coords == null) {
            return Optional.empty();
        } else {
            return Optional.of(new BoardLocation(coords, getBoardView().getBoardId()));
        }
    }

    public BoardView getBoardView() {
        return (BoardView) getSource();
    }
}
