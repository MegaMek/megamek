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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.sprite.HexFlagSprite;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.game.Game;
import megamek.common.orders.RouteGroups;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;

/**
 * Marks the waypoints players have ordered for the bot units on this player's side with flags. Each group of units
 * following the same route - a formation, or units sent along identical hexes - gets its own banner color, with the
 * unit's model under the flag and the waypoint's number below that, with its hold if it has one. An arrow on the hex
 * edge shows the facing set on the waypoint. A player can tell at a glance whose route a flag belongs to, in what
 * order it will be visited and what the units do there.
 *
 * <p>Routes are read from the units' own orders, which every client receives with the units, so the flags follow the
 * bots as they work through their routes. Enemy bots' routes are never drawn.</p>
 */
public class BotRouteSpriteHandler extends BoardViewSpriteHandler {

    /** Banner colors, one per route group in unit order, chosen to stand apart from each other on any terrain. */
    private static final List<Color> ROUTE_COLORS = List.of(
          new Color(240, 140, 30),
          new Color(40, 200, 230),
          new Color(230, 60, 200),
          new Color(250, 230, 40),
          new Color(120, 230, 60),
          new Color(90, 120, 255),
          new Color(255, 120, 130),
          new Color(245, 245, 245));

    private final Game game;

    public BotRouteSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    /**
     * One flag to draw: a waypoint of one route group.
     *
     * @param hex        the waypoint
     * @param boardId    the board it is on
     * @param colorIndex the group's place in unit order, which picks its banner color
     * @param label      who follows the route, e.g. {@code GHR-5H +3}
     * @param stepNumber the waypoint's place in the route, from 1
     * @param facing     the facing set on the waypoint, 0-5, or {@link UnitOrders#FACING_AUTO}
     * @param holdTurns  the turns set to hold there; 0 passes through
     */
    record RouteFlag(Coords hex, int boardId, int colorIndex, String label, int stepNumber, int facing,
          int holdTurns) {

        /**
         * @return the line under the unit's name: the waypoint's number, and its hold if it has one, e.g.
         *       {@code 2 hold 2}
         */
        String progressText() {
            return (holdTurns > 0) ? Messages.getString("BotCommandPanel.MoveOrder.flagHold", stepNumber, holdTurns)
                  : String.valueOf(stepNumber);
        }
    }

    /**
     * Redraws the flags. Kept for the unit selection hooks; the flags no longer depend on which unit is selected.
     *
     * @param unitId the selected unit (unused)
     */
    public void showRouteFor(int unitId) {
        renewSprites();
    }

    private void renewSprites() {
        clear();
        List<Entity> units = new ArrayList<>(game.getEntitiesVector());
        units.sort(Comparator.comparingInt(Entity::getId));
        for (RouteFlag flag : routeFlags(units, localPlayer())) {
            IBoardView boardView = clientGUI.getBoardView(flag.boardId());
            if (boardView instanceof BoardView tacticalBoardView) {
                int arrowFacing = (flag.facing() == UnitOrders.FACING_AUTO) ? HexFlagSprite.NO_FACING
                      : flag.facing();
                HexFlagSprite sprite = new HexFlagSprite(tacticalBoardView, flag.hex(),
                      ROUTE_COLORS.get(flag.colorIndex() % ROUTE_COLORS.size()), flag.label(), flag.progressText(),
                      arrowFacing);
                currentSprites.add(sprite);
                tacticalBoardView.addSprites(List.of(sprite));
            }
        }
    }

    private @Nullable Player localPlayer() {
        for (IBoardView boardView : clientGUI.boardViews()) {
            if (boardView instanceof BoardView tacticalBoardView) {
                return tacticalBoardView.getLocalPlayer();
            }
        }
        return null;
    }

    /**
     * Works out the flags for the routes the viewer may see: those of bot units on the viewer's side. Units in one
     * formation share a route, taken from their leader; other units share a flag when their routes are identical.
     *
     * @param units  every unit in the game, in id order
     * @param viewer the player at this client, or {@code null} for none
     *
     * @return the flags, grouped by route in unit order
     */
    static List<RouteFlag> routeFlags(List<Entity> units, @Nullable Player viewer) {
        List<RouteFlag> flags = new ArrayList<>();
        int colorIndex = 0;
        for (RouteGroups.RouteGroup group : RouteGroups.visibleTo(units, viewer)) {
            Entity guide = group.guide();
            List<Coords> route = guide.getUnitOrders().getRoute();
            for (int step = 0; step < route.size(); step++) {
                WaypointOrder waypointOrder = guide.getUnitOrders().getWaypointOrder(step);
                // the last waypoint is held until new orders: it has no hold count to show
                int holdTurns = (step == route.size() - 1) ? 0 : waypointOrder.getHoldTurns();
                flags.add(new RouteFlag(route.get(step), guide.getBoardId(), colorIndex, group.label(), step + 1,
                      waypointOrder.getFacing(), holdTurns));
            }
            colorIndex++;
        }
        return flags;
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent event) {
        renewSprites();
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent event) {
        renewSprites();
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent event) {
        renewSprites();
    }

    @Override
    public void initialize() {
        game.addGameListener(this);
        renewSprites();
    }

    @Override
    public void dispose() {
        clear();
        game.removeGameListener(this);
    }
}
