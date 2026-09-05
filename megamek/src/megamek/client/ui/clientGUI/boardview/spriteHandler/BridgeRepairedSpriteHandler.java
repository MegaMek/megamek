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
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import java.util.Map;

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.BridgeRepairedSprite;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Terrains;

/**
 * Manages the field-repair badges on bridge sections rebuilt in-game by Bridge-Building Engineers (the unofficial
 * bridge-repair option). Places a {@link BridgeRepairedSprite} on every hex carrying the persistent
 * {@link Terrains#BRIDGE_REPAIRED} marker terrain, so a repaired section (with the kit's lower CF) is visibly distinct
 * from an original bridge. Because the marker is part of the hex, the badges survive save games and appear on every
 * client without extra packets.
 */
public class BridgeRepairedSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public BridgeRepairedSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Rebuilds the field-repair badges from the current board state: one badge on every hex that carries the
     * BRIDGE_REPAIRED marker terrain across all boards.
     */
    public void updateRepairedBridgeSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Board> boardEntry : game.getBoards().entrySet()) {
            Board board = boardEntry.getValue();
            // Only boards that actually hold bridges can hold a repaired section; skip the rest cheaply.
            if (!board.containsBridges()) {
                continue;
            }
            addBadgesForBoard(boardEntry.getKey(), board);
        }
        currentSprites.forEach(sprite -> sprite.bv.addSprite(sprite));
    }

    /**
     * Adds a field-repair badge for every BRIDGE_REPAIRED hex on the given board.
     *
     * @param boardId the board's id
     * @param board   the board to scan
     */
    private void addBadgesForBoard(int boardId, Board board) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Hex hex = board.getHex(x, y);
                if ((hex == null) || !hex.containsTerrain(Terrains.BRIDGE_REPAIRED)) {
                    continue;
                }
                Coords coords = new Coords(x, y);
                BoardView boardView = (BoardView) clientGUI.getBoardView(BoardLocation.of(coords, boardId));
                if (boardView != null) {
                    currentSprites.add(new BridgeRepairedSprite(boardView, coords));
                }
            }
        }
    }

    @Override
    public void initialize() {
        game.addGameListener(this);
    }

    @Override
    public void dispose() {
        clear();
        game.removeGameListener(this);
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        updateRepairedBridgeSprites();
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        // A completed repair adds the marker terrain (a board change); refresh so the badge appears.
        updateRepairedBridgeSprites();
    }
}
