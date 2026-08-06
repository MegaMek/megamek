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
import megamek.client.ui.clientGUI.boardview.sprite.SawClearingSprite;
import megamek.common.board.BoardLocation;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.game.Game;

/**
 * Manages saw clearing indicator sprites on the board view. Creates and removes {@link SawClearingSprite} instances to
 * show which hexes are being cleared by vehicle-mounted saws.
 */
public class SawClearingSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public SawClearingSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Updates the saw clearing sprites to reflect the given cut hex data.
     *
     * @param cutHexes a map of board locations to turns remaining, or null to clear
     */
    public void setSawClearingSprites(Map<BoardLocation, Integer> cutHexes) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        if (cutHexes != null) {
            for (Map.Entry<BoardLocation, Integer> entry : cutHexes.entrySet()) {
                BoardLocation location = entry.getKey();
                BoardView boardView = (BoardView) clientGUI.getBoardView(location);
                if (boardView != null) {
                    SawClearingSprite sprite = new SawClearingSprite(
                          boardView, location.coords(), entry.getValue());
                    currentSprites.add(sprite);
                }
            }
        }
        currentSprites.forEach(sprite -> sprite.bv.addSprite(sprite));
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
    public void gameBoardChanged(GameBoardChangeEvent e) {
        setSawClearingSprites(game.getHexesBeingCut());
    }
}
