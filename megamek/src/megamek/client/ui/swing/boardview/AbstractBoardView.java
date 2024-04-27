/*
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
package megamek.client.ui.swing.boardview;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.IDisplayable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class AbstractBoardView implements IBoardView {

    protected final List<BoardViewListener> boardViewListeners = new ArrayList<>();
    protected final LinkedHashSet<IDisplayable> overlays = new LinkedHashSet<>();

    /**
     * Notifies attached BoardViewListeners of the event.
     *
     * @param event the board event.
     */
    public void processBoardViewEvent(BoardViewEvent event) {
        // Copy the listener list to allow concurrent modification
        for (BoardViewListener l : new ArrayList<>(boardViewListeners)) {
            switch (event.getType()) {
                case BoardViewEvent.BOARD_HEX_CLICKED:
                case BoardViewEvent.BOARD_HEX_DOUBLECLICKED:
                case BoardViewEvent.BOARD_HEX_DRAGGED:
                case BoardViewEvent.BOARD_HEX_POPUP:
                    l.hexMoused(event);
                    break;
                case BoardViewEvent.BOARD_HEX_CURSOR:
                    l.hexCursor(event);
                    break;
                case BoardViewEvent.BOARD_HEX_HIGHLIGHTED:
                    l.boardHexHighlighted(event);
                    break;
                case BoardViewEvent.BOARD_HEX_SELECTED:
                    l.hexSelected(event);
                    break;
                case BoardViewEvent.BOARD_FIRST_LOS_HEX:
                    l.firstLOSHex(event);
                    break;
                case BoardViewEvent.BOARD_SECOND_LOS_HEX:
                    l.secondLOSHex(event);
                    break;
                case BoardViewEvent.FINISHED_MOVING_UNITS:
                    l.finishedMovingUnits(event);
                    break;
                case BoardViewEvent.SELECT_UNIT:
                    l.unitSelected(event);
                    break;
            }
        }
    }

    @Override
    public final void addBoardViewListener(BoardViewListener listener) {
        if (!boardViewListeners.contains(listener)) {
            boardViewListeners.add(listener);
        }
    }

    @Override
    public final void removeBoardViewListener(BoardViewListener listener) {
        boardViewListeners.remove(listener);
    }

    @Override
    public void dispose() {
        boardViewListeners.clear();
    }

    @Override
    public final void addOverlay(IDisplayable overlay) {
        overlays.add(overlay);
    }

    @Override
    public final void removeOverlay(IDisplayable overlay) {
        overlays.remove(overlay);
    }
}