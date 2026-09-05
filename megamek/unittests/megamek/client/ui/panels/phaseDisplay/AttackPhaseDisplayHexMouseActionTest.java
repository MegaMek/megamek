/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.panels.phaseDisplay;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.Coords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AttackPhaseDisplay#applyHexMouseAction(BoardViewEvent, boolean)}, the single place an attack phase
 * display turns a hex mouse event into a board action.
 * <p>
 * The rule these tests protect: one click selects the hex at most once. A second selection fires a second
 * hex-selected event, and the displays answer that by opening the target window again (issue #8781).
 * </p>
 */
@DisplayName("AttackPhaseDisplay hex mouse action")
class AttackPhaseDisplayHexMouseActionTest {

    private static final Coords CLICKED_HEX = new Coords(5, 10);

    private BoardView boardView;

    @BeforeEach
    void setUp() {
        boardView = mock(BoardView.class);
    }

    private BoardViewEvent hexEvent(int eventType, int modifiers, int mouseButton) {
        BoardViewEvent event = mock(BoardViewEvent.class);
        when(event.getType()).thenReturn(eventType);
        when(event.getCoords()).thenReturn(CLICKED_HEX);
        when(event.getModifiers()).thenReturn(modifiers);
        when(event.getButton()).thenReturn(mouseButton);
        when(event.getBoardView()).thenReturn(boardView);
        return event;
    }

    @Test
    @DisplayName("A left click selects the clicked hex exactly once")
    void leftClickSelectsTheHexOnce() {
        BoardViewEvent leftClick = hexEvent(BoardViewEvent.BOARD_HEX_CLICKED, 0, MouseEvent.BUTTON1);

        AttackPhaseDisplay.applyHexMouseAction(leftClick, false);

        verify(boardView, times(1)).select(CLICKED_HEX);
        verify(boardView, never()).cursor(any());
    }

    @Test
    @DisplayName("A drag moves the cursor and never selects")
    void dragOnlyMovesTheCursor() {
        BoardViewEvent drag = hexEvent(BoardViewEvent.BOARD_HEX_DRAGGED, 0, MouseEvent.BUTTON1);

        AttackPhaseDisplay.applyHexMouseAction(drag, false);

        verify(boardView, times(1)).cursor(CLICKED_HEX);
        verify(boardView, never()).select(any());
    }

    @Test
    @DisplayName("A click does not select while the torso twist modifier is held")
    void twistModifierSuppressesSelection() {
        BoardViewEvent shiftClick = hexEvent(BoardViewEvent.BOARD_HEX_CLICKED,
              InputEvent.SHIFT_DOWN_MASK,
              MouseEvent.BUTTON1);

        AttackPhaseDisplay.applyHexMouseAction(shiftClick, true);

        verify(boardView, never()).select(any());
    }

    @Test
    @DisplayName("An ALT click leaves the hex alone for the ruler")
    void altClickDoesNotSelect() {
        BoardViewEvent altClick = hexEvent(BoardViewEvent.BOARD_HEX_CLICKED,
              InputEvent.ALT_DOWN_MASK,
              MouseEvent.BUTTON1);

        AttackPhaseDisplay.applyHexMouseAction(altClick, false);

        verify(boardView, never()).select(any());
    }

    @Test
    @DisplayName("A middle click does not select")
    void middleClickDoesNotSelect() {
        BoardViewEvent middleClick = hexEvent(BoardViewEvent.BOARD_HEX_CLICKED, 0, MouseEvent.BUTTON2);

        AttackPhaseDisplay.applyHexMouseAction(middleClick, false);

        verify(boardView, never()).select(any());
    }

    @Test
    @DisplayName("Board events that are not a click or a drag touch neither cursor nor selection")
    void otherEventsAreIgnored() {
        BoardViewEvent popup = hexEvent(BoardViewEvent.BOARD_HEX_POPUP, 0, MouseEvent.BUTTON3);

        AttackPhaseDisplay.applyHexMouseAction(popup, false);

        verify(boardView, never()).select(any());
        verify(boardView, never()).cursor(any());
    }
}
