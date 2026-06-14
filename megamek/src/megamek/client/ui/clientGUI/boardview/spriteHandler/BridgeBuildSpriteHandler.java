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
import megamek.client.ui.clientGUI.boardview.sprite.BridgeBuildSprite;
import megamek.common.board.BoardLocation;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;

/**
 * Manages bridge-under-construction indicator sprites on the board view (TO:AUE Bridge-Building Engineers). Creates and
 * removes {@link BridgeBuildSprite} instances on the target hexes of all platoons that are currently raising a bridge;
 * the build state is read from the synced ConvInfantry entities.
 */
public class BridgeBuildSpriteHandler extends BoardViewSpriteHandler {

    private final Game game;

    public BridgeBuildSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Rebuilds the bridge construction sprites from the current entity states: one indicator on the target hex of every
     * platoon that is raising a bridge, showing the build turn in progress and the turns required.
     */
    public void updateBridgeBuildSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        for (Entity entity : game.getEntitiesVector()) {
            if (!(entity instanceof ConvInfantry convInfantry) || !convInfantry.hasBridgeInProgress()
                  || (convInfantry.getBridgeTargetCoords() == null)) {
                continue;
            }
            BoardLocation location = BoardLocation.of(convInfantry.getBridgeTargetCoords(), entity.getBoardId());
            BoardView boardView = (BoardView) clientGUI.getBoardView(location);
            if (boardView != null) {
                // Both build and dismantling show the standing structure on the same N / build-required scale: the
                // build counts it up as turns are banked, the dismantling counts the same number back down to zero.
                int turnsRequired = convInfantry.getBridgeBuildRequiredTurns();
                int turnsStanding = convInfantry.isDismantlingBridge()
                      ? convInfantry.getBridgeDismantleRemaining()
                      : Math.min(convInfantry.getBridgeBuildTurns(), turnsRequired);
                currentSprites.add(new BridgeBuildSprite(boardView, convInfantry.getBridgeTargetCoords(),
                      turnsStanding, turnsRequired, convInfantry.getBridgeExits()));
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
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        updateBridgeBuildSprites();
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        updateBridgeBuildSprites();
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        // A completed bridge changes the board and ends the build; refresh so the indicator disappears
        updateBridgeBuildSprites();
    }
}
