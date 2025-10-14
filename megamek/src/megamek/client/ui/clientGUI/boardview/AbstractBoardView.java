/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.common.Player;

public abstract class AbstractBoardView implements IBoardView {

    protected final List<BoardViewListener> boardViewListeners = new ArrayList<>();
    protected final LinkedHashSet<IDisplayable> overlays = new LinkedHashSet<>();
    protected final TreeSet<Sprite> allSprites = new TreeSet<>();
    protected final int boardId;

    // the player who owns this BoardView's client
    protected Player localPlayer = null;

    AbstractBoardView(int boardId) {
        this.boardId = boardId;
    }

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
                case BoardViewEvent.BOARD_HEX_DOUBLE_CLICKED:
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

    @Override
    public void addSprites(Collection<? extends Sprite> sprites) {
        allSprites.addAll(sprites);
        repaint();
    }

    @Override
    public void removeSprites(Collection<? extends Sprite> sprites) {
        allSprites.removeAll(sprites);
        repaint();
    }

    /**
     * Removes all sprites from this BoardView. This includes (possibly) sprites for units, attacks etc. Note that this
     * is not communicated to the SpriteHandlers.
     */
    public void clearSprites() {
        allSprites.clear();
        repaint();
    }

    /**
     * Returns an unmodifiable view of this BoardView's sprites.
     */
    public Set<Sprite> getAllSprites() {
        return Collections.unmodifiableSet(allSprites);
    }

    @Override
    public Player getLocalPlayer() {
        return localPlayer;
    }

    public void setLocalPlayer(Player p) {
        localPlayer = p;
    }

    @Override
    public int getBoardId() {
        return boardId;
    }
}
