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
import megamek.common.annotations.Nullable;
import megamek.common.event.GameEvent;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFUnitPlaceHolder;

public class SBFFormationSpriteHandler extends BoardViewSpriteHandler {

    private final SBFGame game;

    public SBFFormationSpriteHandler(BoardView boardView, SBFClient client) {
        super(boardView);
        game = client.getGame();
    }

    /**
     * Clears the current sprites and creates new sprites for all formations.
     */
    public void update() {
        clear();
        game.getInGameObjects().stream()
                .filter(SBFFormation.class::isInstance)
                .filter(f -> ((SBFFormation) f).getPosition() != null)
                .map(f -> new SBFFormationSprite(boardView, (SBFFormation) f, game.getPlayer(f.getOwnerId()), game))
                .forEach(currentSprites::add);

        game.getInGameObjects().stream()
                .filter(SBFUnitPlaceHolder.class::isInstance)
                .filter(f -> ((SBFUnitPlaceHolder) f).getPosition() != null)
                .map(f -> new SBFPlaceHolderSprite(boardView, (SBFUnitPlaceHolder) f, game.getPlayer(f.getOwnerId()), game))
                .forEach(currentSprites::add);

        boardView.addSprites(currentSprites);
    }

    /**
     * Sets the given formation as the selected formation and renews the sprites accordingly. The previously
     * selected formation, if any, will no longer be selected. When the given formation is null,
     * all formations will be deselected.
     *
     * @param formation The formation to draw as selected, or null for no selection
     */
    public void setSelectedFormation(@Nullable SBFFormation formation) {
        for (Sprite sprite : currentSprites) {
            if (sprite instanceof SBFFormationSprite formationSprite) {
                formationSprite.setSelected(formationSprite.getFormation().equals(formation));
            }
        }
        boardView.repaint();
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
    public void gameUnitChange(GameEvent event) {
        super.gameUnitChange(event);
        update();
    }

}
