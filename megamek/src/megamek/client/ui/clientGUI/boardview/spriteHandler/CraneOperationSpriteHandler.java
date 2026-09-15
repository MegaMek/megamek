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
import megamek.client.ui.clientGUI.boardview.sprite.CraneOperationSprite;
import megamek.common.board.BoardLocation;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.CraneOperation;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;
import megamek.logging.MMLogger;

/**
 * Manages the crane work sprites (TW p.90-91): a countdown on the hex of every unit a grounded Small Craft or DropShip
 * is loading or unloading by crane, with an unloading unit fading in on its hex. The state is read from the synced
 * carriers.
 */
public class CraneOperationSpriteHandler extends BoardViewSpriteHandler {

    private static final MMLogger LOGGER = MMLogger.create(CraneOperationSpriteHandler.class);

    private final Game game;

    public CraneOperationSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /** Rebuilds the crane work sprites from the crane operations of every carrier. */
    public void updateCraneOperationSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        for (Entity entity : game.getEntitiesVector()) {
            if (entity instanceof SmallCraft carrier) {
                for (CraneOperation operation : carrier.getCraneOperations().getOperations()) {
                    addOperationSprite(carrier, operation);
                }
            }
        }
        currentSprites.forEach(sprite -> sprite.bv.addSprite(sprite));
    }

    private void addOperationSprite(SmallCraft carrier, CraneOperation operation) {
        Entity unit = game.getEntity(operation.getUnitId());
        BoardView boardView = (BoardView) clientGUI.getBoardView(
              BoardLocation.of(operation.getUnitPosition(), carrier.getBoardId()));
        if ((unit == null) || (boardView == null)) {
            LOGGER.debug("[Crane] {}: no sprite for {} (unit known {}, board shown {})", carrier.getDisplayName(),
                  operation, unit != null, boardView != null);
            return;
        }
        if (operation.isLoading() && !CraneOperationSprite.isWaitingInPlace(unit, operation)) {
            LOGGER.debug("[Crane] {}: {} moved away, so no countdown is shown; the End Phase cancels the loading",
                  carrier.getDisplayName(), unit.getDisplayName());
            return;
        }
        Entity fadingUnit = operation.isLoading() ? null : unit;
        LOGGER.debug("[Crane] {}: sprite for {} ({} turn(s) to go)", carrier.getDisplayName(), operation,
              operation.getTurnsRequired() - operation.getTurnsCompleted());
        currentSprites.add(new CraneOperationSprite(boardView, operation.getUnitPosition(), fadingUnit,
              operation.getUnloadFacing(), operation.getTurnsCompleted(), operation.getTurnsRequired()));
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
        updateCraneOperationSprites();
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        updateCraneOperationSprites();
    }
}
