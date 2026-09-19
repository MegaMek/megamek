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

import megamek.client.Client;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.ScanSprite;
import megamek.common.Player;
import megamek.common.actions.ScanAction;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Draws the sensor sweep of every scan order this player has given, from the scanning unit to what it was pointed at.
 *
 * <p>Only the player who gave the order sees it. A scan order the other side could read off the board would tell them
 * which of their units, or which hex, this player believes is worth knowing about, and that is worth more to them than
 * the reading is to the scout.</p>
 *
 * @author Claude Code (Opus 5)
 */
public class ScanSpriteHandler extends BoardViewSpriteHandler {

    private static final MMLogger LOGGER = MMLogger.create(ScanSpriteHandler.class);

    private final Client client;
    private final Game game;

    public ScanSpriteHandler(AbstractClientGUI clientGUI, Client client) {
        super(clientGUI);
        this.client = client;
        this.game = client.getGame();
    }

    /** Rebuilds the sweeps from the scan orders currently standing on this player's units. */
    public void updateScanSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }
        int drawn = 0;
        for (Entity scanner : game.getEntitiesVector()) {
            ScanAction order = scanner.getPendingScan();
            boolean isOursAndOrdered = (order != null) && localPlayer.equals(scanner.getOwner());
            if (!isOursAndOrdered || (scanner.getPosition() == null)) {
                continue;
            }
            Coords targetHex = order.resolveTargetPosition(game);
            if (targetHex == null) {
                continue;
            }
            BoardLocation location = BoardLocation.of(scanner.getPosition(), scanner.getBoardId());
            BoardView boardView = (BoardView) clientGUI.getBoardView(location);
            if (boardView == null) {
                continue;
            }
            currentSprites.add(new ScanSprite(boardView, scanner.getPosition(), targetHex,
                  localPlayer.getColour().getColour()));
            drawn++;
        }
        if (drawn > 0) {
            LOGGER.debug("[Scan] drawing {} sweep(s) for {}", drawn, localPlayer.getName());
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
        updateScanSprites();
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent event) {
        // The server confirms, withdraws and spends an order by sending the unit back, so this is where a sweep
        // appears and where it goes away again.
        updateScanSprites();
    }
}
