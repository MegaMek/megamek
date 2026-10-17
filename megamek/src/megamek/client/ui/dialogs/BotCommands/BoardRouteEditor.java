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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.client.ui.clientGUI.boardview.sprite.TextMarkerSprite;
import megamek.common.RangeType;
import megamek.common.board.Coords;
import megamek.common.util.Distractable;
import megamek.logging.MMLogger;

/**
 * The board side of the Move Order editor: takes the hexes the player clicks while picking, lets a waypoint be dragged
 * to another hex while the editor is open, holds the display under the board still meanwhile so clicks select and move
 * nothing there, and draws the draft route. Split out of {@link BotMoveOrderDialog}, which decides what a picked hex
 * does.
 */
final class BoardRouteEditor {

    private static final MMLogger LOGGER = MMLogger.create(BoardRouteEditor.class);
    private static final int ALL_HEX_BORDERS = 63;

    private final ClientGUI clientGUI;
    private final BoardView boardView;
    private final WaypointTableModel waypoints;
    private final Consumer<Coords> onHexPicked;
    private final IntConsumer onRowChanged;
    private final List<Sprite> routeSprites = new ArrayList<>();
    // dragging a waypoint on the board: its row, or -1; whether it has moved; the display held still while it drags,
    // when picking was not already holding it; and the release that ended the last drag, which is no click
    private final BoardViewListenerAdapter dragListener = new BoardViewListenerAdapter() {
        @Override
        public void hexMoused(BoardViewEvent event) {
            dragWaypoint(event);
        }
    };
    private int draggedRow = -1;
    private boolean hasDragMoved;
    private Distractable dragHeldDisplay;
    private BoardViewEvent dragEndingClick;
    private BoardViewListenerAdapter hexClickListener;
    private Distractable suppressedDisplay;

    /**
     * Starts listening for waypoints dragged on the board.
     *
     * @param clientGUI    the client GUI, whose display is held still while hexes are picked or dragged
     * @param boardView    the board
     * @param waypoints    the draft route
     * @param onHexPicked  what a hex clicked while picking does
     * @param onRowChanged told the row of a waypoint dragged to another hex
     */
    BoardRouteEditor(ClientGUI clientGUI, BoardView boardView, WaypointTableModel waypoints,
          Consumer<Coords> onHexPicked, IntConsumer onRowChanged) {
        this.clientGUI = clientGUI;
        this.boardView = boardView;
        this.waypoints = waypoints;
        this.onHexPicked = onHexPicked;
        this.onRowChanged = onRowChanged;
        boardView.addBoardViewListener(dragListener);
    }

    /**
     * Starts taking each hex the player clicks on the board, holding the display under the board still.
     *
     * @return {@code true} if picking started; {@code false} if it was already on
     */
    boolean startPicking() {
        if (hexClickListener != null) {
            return false;
        }
        if (clientGUI.getCurrentPanel() instanceof Distractable distractable) {
            suppressedDisplay = distractable;
            suppressedDisplay.setIgnoringEvents(true);
        }
        hexClickListener = new BoardViewListenerAdapter() {
            @Override
            public void hexMoused(BoardViewEvent event) {
                if ((event.getType() != BoardViewEvent.BOARD_HEX_CLICKED) || (event.getButton() != MouseEvent.BUTTON1)
                      || (event.getCoords() == null)) {
                    return;
                }
                if ((event == dragEndingClick) || ((draggedRow >= 0) && hasDragMoved)) {
                    // the release at the end of a drag moved a waypoint: it adds none
                    return;
                }
                onHexPicked.accept(event.getCoords());
            }
        };
        boardView.addBoardViewListener(hexClickListener);
        return true;
    }

    /** Stops taking clicked hexes and lets the display under the board go. */
    void stopPicking() {
        if (hexClickListener != null) {
            boardView.removeBoardViewListener(hexClickListener);
            hexClickListener = null;
        }
        if (suppressedDisplay != null) {
            suppressedDisplay.setIgnoringEvents(false);
            suppressedDisplay = null;
        }
    }

    /** Draws the draft route on the board: each waypoint outlined and numbered in route order. */
    void refreshSprites() {
        boardView.removeSprites(routeSprites);
        routeSprites.clear();
        for (int row = 0; row < waypoints.getRowCount(); row++) {
            Coords hex = waypoints.getHex(row);
            routeSprites.add(new FieldOfFireSprite(boardView, RangeType.RANGE_SHORT, hex, ALL_HEX_BORDERS));
            routeSprites.add(new TextMarkerSprite(boardView, hex, String.valueOf(row + 1), Color.WHITE));
        }
        boardView.addSprites(routeSprites);
    }

    /**
     * Drags a waypoint: pressed on one of the route's hexes, it follows the mouse hex by hex, and the release leaves
     * it there. The display under the board is held still while it drags, so the drag selects and moves nothing.
     */
    private void dragWaypoint(BoardViewEvent event) {
        Coords hex = event.getCoords();
        if (hex == null) {
            return;
        }
        if (event.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (draggedRow < 0) {
                // the press: a drag starts only on one of the route's hexes, with the left button
                if (event.getButton() != MouseEvent.BUTTON1) {
                    // the right button pans the board; only the left drags a waypoint
                    return;
                }
                int row = waypoints.rowAt(hex);
                LOGGER.info("[BotOrders] Move Order editor: press at {} - {}", hex.getBoardNum(), (row >= 0)
                      ? "picked up waypoint " + (row + 1) + ", drag it and release"
                      : "not one of the route's " + waypoints.getRowCount() + " waypoint(s), nothing to drag");
                if (row >= 0) {
                    draggedRow = row;
                    hasDragMoved = false;
                    if ((suppressedDisplay == null) && (clientGUI.getCurrentPanel() instanceof Distractable display)) {
                        dragHeldDisplay = display;
                        dragHeldDisplay.setIgnoringEvents(true);
                    }
                }
                return;
            }
            if (!hex.equals(waypoints.getHex(draggedRow))) {
                waypoints.moveWaypoint(draggedRow, hex);
                hasDragMoved = true;
                onRowChanged.accept(draggedRow);
            }
        } else if ((event.getType() == BoardViewEvent.BOARD_HEX_CLICKED) && (draggedRow >= 0)) {
            if (hasDragMoved) {
                dragEndingClick = event;
                LOGGER.info("[BotOrders] Move Order editor: waypoint {} dragged to {}", draggedRow + 1,
                      hex.getBoardNum());
            }
            endDrag();
        }
    }

    private void endDrag() {
        draggedRow = -1;
        hasDragMoved = false;
        if (dragHeldDisplay != null) {
            dragHeldDisplay.setIgnoringEvents(false);
            dragHeldDisplay = null;
        }
    }

    /** Stops all board editing and takes the draft route off the board, as the editor closes. */
    void close() {
        boardView.removeBoardViewListener(dragListener);
        endDrag();
        stopPicking();
        boardView.removeSprites(routeSprites);
        routeSprites.clear();
    }
}
