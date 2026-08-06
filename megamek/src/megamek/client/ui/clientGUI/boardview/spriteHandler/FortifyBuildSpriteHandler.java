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

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FortifyBuildSprite;
import megamek.common.board.BoardLocation;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Fortifiable;
import megamek.common.units.Infantry;

/**
 * Manages fortification-under-construction indicator sprites on the board view (TO:AUE Trench/Fieldworks Engineers and
 * Vehicles &amp; Fieldworks). Creates and removes {@link FortifyBuildSprite} instances on the hexes of all units that are
 * currently building a fortified hex; the build state is read from the synced entities.
 */
public class FortifyBuildSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public FortifyBuildSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Rebuilds the fortification construction sprites from the current entity states: one indicator on the hex of every
     * infantry platoon or vehicle that is building a fortified hex, showing the stage reached and the total stages
     * required.
     */
    public void updateFortifyBuildSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        for (Entity entity : game.getEntitiesVector()) {
            int stage = fortifyStageOf(entity);
            if ((stage <= 0) || (entity.getPosition() == null)) {
                continue;
            }
            BoardLocation location = BoardLocation.of(entity.getPosition(), entity.getBoardId());
            BoardView boardView = (BoardView) clientGUI.getBoardView(location);
            if (boardView != null) {
                currentSprites.add(new FortifyBuildSprite(boardView, entity.getPosition(),
                      stage, fortifyTotalStagesOf(entity)));
            }
        }
        currentSprites.forEach(sprite -> sprite.bv.addSprite(sprite));
    }

    /**
     * @return the in-progress fortification stage of the entity (1+), or 0 if it is not building a fortified hex
     */
    private static int fortifyStageOf(Entity entity) {
        return switch (entity) {
            case Infantry infantry -> infantry.getFortifyStage();
            case Fortifiable fortifiable -> fortifiable.getFortifyStage();
            default -> 0;
        };
    }

    /**
     * @return the total stages a finished fortified hex needs for the entity (the progress denominator)
     */
    private static int fortifyTotalStagesOf(Entity entity) {
        return switch (entity) {
            case Infantry infantry -> infantry.getFortifyTotalStages();
            case Fortifiable fortifiable -> fortifiable.getFortifyTotalStages();
            default -> 0;
        };
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
        updateFortifyBuildSprites();
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        updateFortifyBuildSprites();
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        // A completed fortification changes the board (FORTIFIED terrain added) and ends the build; refresh so
        // the ghost indicator disappears once the real terrain is in place.
        updateFortifyBuildSprites();
    }
}
