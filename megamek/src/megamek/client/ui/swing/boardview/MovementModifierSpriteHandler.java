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

import megamek.common.Game;
import megamek.common.MovePath;
import megamek.common.event.GamePhaseChangeEvent;

import java.util.Collection;

public class MovementModifierSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public MovementModifierSpriteHandler(BoardView boardView, Game game) {
        super(boardView);
        this.game = game;
        game.addGameListener(this);
    }

    @Override
    void dispose() {
        clear();
        game.removeGameListener(this);
    }

    public void renewSprites(Collection<MovePath> movePaths) {
        clear();
        movePaths.stream().map(path -> new MovementModifierEnvelopeSprite(boardView, path)).forEach(currentSprites::add);
        boardView.addSprites(currentSprites);
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        clear();
    }
}
