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
package megamek.client.ui.swing.boardview.spriteHandler;

import megamek.client.ui.swing.AbstractClientGUI;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.boardview.sprite.FlareSprite;
import megamek.common.Flare;
import megamek.common.Game;
import megamek.common.event.GameBoardChangeEvent;

import java.util.Collection;

public class FlareSpritesHandler extends BoardViewSpriteHandler {

    private final Game game;

    public FlareSpritesHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    public void renewSprites(Collection<Flare> flares) {
        clear();
        for (Flare flare : flares) {
            IBoardView iBoardView = clientGUI.getBoardView(flare.getBoardId());
            if (iBoardView instanceof BoardView boardView) {
                var sprite = new FlareSprite(boardView, flare);
                currentSprites.add(sprite);
                boardView.addSprite(sprite);
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
    public void gameBoardChanged(GameBoardChangeEvent e) {
        renewSprites(game.getFlares());
    }
}
