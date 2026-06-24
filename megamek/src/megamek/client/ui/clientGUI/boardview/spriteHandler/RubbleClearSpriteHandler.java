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
import megamek.client.ui.clientGUI.boardview.sprite.RubbleClearSprite;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.RubbleClearer;

/**
 * Manages rubble-clearing-in-progress indicator sprites on the board view (TacOps bulldozer rubble clearing). Creates
 * and removes {@link RubbleClearSprite} instances on the rubble hex of every vehicle that is currently clearing it,
 * showing the turns of work banked so far and the total required; the clearing state is read from the synced entities.
 * Mirrors {@link FortifyBuildSpriteHandler}.
 */
public class RubbleClearSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public RubbleClearSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Rebuilds the rubble-clearing sprites from the current entity states: one indicator on the rubble hex of every
     * vehicle that is clearing it, showing the turns banked so far and the total required.
     */
    public void updateRubbleClearSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        for (Entity entity : game.getEntitiesVector()) {
            if (!(entity instanceof RubbleClearer clearer) || !clearer.isClearingRubble()) {
                continue;
            }
            Coords target = clearer.getRubbleClearTarget();
            if (target == null) {
                continue;
            }
            BoardLocation location = BoardLocation.of(target, entity.getBoardId());
            BoardView boardView = (BoardView) clientGUI.getBoardView(location);
            if (boardView != null) {
                int turnsCompleted = clearer.getRubbleClearTurnsCompleted();
                int turnsRequired = clearer.getRubbleClearTurnsRequired();
                // Two layers: the cleared-path fade behind the unit, and the progress counter over it so it reads
                // clearly without the fade dimming the vehicle (QA feedback).
                currentSprites.add(new RubbleClearSprite(boardView, target, turnsCompleted, turnsRequired, false));
                currentSprites.add(new RubbleClearSprite(boardView, target, turnsCompleted, turnsRequired, true));
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
    public void gamePhaseChange(GamePhaseChangeEvent event) {
        updateRubbleClearSprites();
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent event) {
        updateRubbleClearSprites();
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent event) {
        // A finished clear removes the RUBBLE terrain and ends the work; refresh so the indicator disappears once
        // the hex is opened up.
        updateRubbleClearSprites();
    }
}
