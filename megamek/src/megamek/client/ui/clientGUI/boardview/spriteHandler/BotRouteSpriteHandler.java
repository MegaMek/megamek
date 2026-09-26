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

import java.awt.Color;
import java.util.List;

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.sprite.TextMarkerSprite;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Shows the route a player has ordered for the selected bot unit: each waypoint hex is numbered in the order the unit
 * will visit it. The route is read from the unit's own orders, which every client receives with the unit, so it shows
 * the same for every teammate and follows the unit as the bot works through the route.
 */
public class BotRouteSpriteHandler extends BoardViewSpriteHandler {

    private static final Color ROUTE_COLOR = new Color(230, 140, 40);
    private static final int NO_UNIT = -1;

    private final Game game;
    private int shownUnitId = NO_UNIT;

    public BotRouteSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * Shows the ordered route of the given unit, or nothing if it is not a bot unit with a route.
     *
     * @param unitId the selected unit, or a negative number for none
     */
    public void showRouteFor(int unitId) {
        shownUnitId = unitId;
        renewSprites();
    }

    private void renewSprites() {
        clear();
        Entity unit = (shownUnitId == NO_UNIT) ? null : game.getEntity(shownUnitId);
        if ((unit == null) || (unit.getPosition() == null)) {
            return;
        }
        Player owner = unit.getOwner();
        if ((owner == null) || !owner.isBot()) {
            return;
        }
        List<Coords> route = unit.getUnitOrders().getRoute();
        IBoardView boardView = clientGUI.getBoardView(unit.getBoardId());
        if (route.isEmpty() || !(boardView instanceof BoardView tacticalBoardView)) {
            return;
        }
        for (int index = 0; index < route.size(); index++) {
            currentSprites.add(new TextMarkerSprite(tacticalBoardView, route.get(index), String.valueOf(index + 1),
                  ROUTE_COLOR));
        }
        tacticalBoardView.addSprites(currentSprites);
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent event) {
        if ((event.getEntity() != null) && (event.getEntity().getId() == shownUnitId)) {
            renewSprites();
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
}
