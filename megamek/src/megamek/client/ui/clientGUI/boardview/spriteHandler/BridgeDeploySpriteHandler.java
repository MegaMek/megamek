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
import megamek.common.board.Coords;
import megamek.common.equipment.BridgeLayerLogic;
import megamek.common.equipment.BridgeLayerState;
import megamek.common.equipment.MiscMounted;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Manages bridge-deployment indicator sprites for Bridge-Layer (AVLB) units (TM p.242 / TW). A unit that has declared a
 * deployment must remain stationary for a turn before the folding bridge is placed; this handler draws a partial bridge
 * (with a turns-to-finish counter) on the target hex of every unit with a pending deployment, reusing the same
 * {@link BridgeBuildSprite} the infantry bridge build uses, so the in-progress deployment is visible just like an
 * engineer-built bridge. The pending state is read from the synced entities' bridgelayer mounts.
 *
 * @author Claude Code (Opus 4.8)
 */
public class BridgeDeploySpriteHandler extends BoardViewSpriteHandler {

    /** A bridgelayer deployment completes one full stationary turn after it is declared. */
    private static final int DEPLOY_TURNS_REQUIRED = 1;

    private final Game game;

    public BridgeDeploySpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Rebuilds the deployment sprites from the current entity states: one partial-bridge indicator on the target hex of
     * every unit with a pending bridgelayer deployment, growing from faint (just declared) to full (the stationary
     * turn, about to be placed).
     */
    public void updateBridgeDeploySprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        for (Entity entity : game.getEntitiesVector()) {
            MiscMounted bridgeLayer = BridgeLayerLogic.getPendingDeployBridgeLayer(entity);
            if (bridgeLayer == null) {
                continue;
            }
            BridgeLayerState bridgeState = bridgeLayer.getBridgeLayerState();
            Coords target = bridgeState.getDeployTarget();
            if (target == null) {
                continue;
            }
            BoardLocation location = BoardLocation.of(target, entity.getBoardId());
            BoardView boardView = (BoardView) clientGUI.getBoardView(location);
            if (boardView != null) {
                // A one-turn deployment has no meaningful build ramp, so reuse the engineers' build sprite at full
                // strength (a solid "1/1" bridge ghost), and ring it with a yellow/black hazard outline so the target
                // hex is unmistakable - playtesters (colour-blind players in particular) flagged the plain ghost as
                // hard to see.
                currentSprites.add(new BridgeBuildSprite(boardView, target, DEPLOY_TURNS_REQUIRED,
                      DEPLOY_TURNS_REQUIRED, bridgeState.getDeployExits(), true));
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
        updateBridgeDeploySprites();
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent event) {
        updateBridgeDeploySprites();
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent event) {
        // A completed deployment changes the board and clears the pending state; refresh so the indicator disappears.
        updateBridgeDeploySprites();
    }
}
