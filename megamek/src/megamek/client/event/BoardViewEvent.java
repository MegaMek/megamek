/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.event;

import java.awt.event.InputEvent;
import java.io.Serial;
import java.util.EventObject;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.units.Entity;

/**
 * Instances of this class are sent as a result of changes in BoardView
 *
 * @see BoardViewListener
 */
public class BoardViewEvent extends EventObject {
    @Serial
    private static final long serialVersionUID = -4823618884833399318L;

    /**
     * This event type is sent when the mouse button is released (BOARD_HEX_DRAGGED is sent when the button is pressed)
     */
    public static final int BOARD_HEX_CLICKED = 0;
    public static final int BOARD_HEX_DOUBLE_CLICKED = 1;

    /**
     * This event type is sent when the mouse button is pressed (BOARD_HEX_CLICKED is sent when the button is released)
     */
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
    public BoardLocation getBoardLocation() {
        return BoardLocation.of(coords, getBoardView().getBoardId());
    }

    public BoardView getBoardView() {
        return (BoardView) getSource();
    }

    public int getBoardId() {
        return getBoardView().getBoardId();
    }

    public boolean isShiftHeld() {
        return (getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
    }

    public boolean isAltHeld() {
        return (getModifiers() & InputEvent.ALT_DOWN_MASK) != 0;
    }

    public boolean isCtrlHeld() {
        return (getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0;
    }
}
