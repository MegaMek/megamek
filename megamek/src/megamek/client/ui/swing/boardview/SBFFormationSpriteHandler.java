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

import megamek.client.SBFClient;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;

public class SBFFormationSpriteHandler extends BoardViewSpriteHandler {

    private final SBFGame game;

    public SBFFormationSpriteHandler(BoardView boardView, SBFClient client) {
        super(boardView);
        game = client.getGame();
    }

    public void update() {
        clear();
        game.getInGameObjects().stream()
                .filter(SBFFormation.class::isInstance)
                .map(f -> new SBFFormationSprite(boardView, (SBFFormation) f, game.getPlayer(f.getOwnerId())))
                .forEach(currentSprites::add);
        boardView.addSprites(currentSprites);
    }

    @Override
    public void initialize() { }

    @Override
    public void dispose() {
        clear();
    }
}
