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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.sprite.HexFlagSprite;
import megamek.client.ui.clientGUI.boardview.sprite.RouteDotSprite;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.game.Game;
import megamek.common.orders.NavPoint;
import megamek.common.orders.PhaseLine;
import megamek.common.orders.RouteGroups;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
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

    /**
     * @param clientGUI the client GUI whose board views show the flags
     * @param game      the game whose bot units' routes are shown
     */
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
     * @param step       the waypoint's name: its nav point, as the radio calls it ({@code Gamma}), or its place in
     *                   the route, from 1, when it has none
     * @param facing     the facing set on the waypoint, 0-5, or {@link UnitOrders#FACING_AUTO}
     * @param holdTurns  the turns set to hold there; 0 passes through
     * @param isAssemble {@code true} if the units wait there until in position rather than for a fixed delay
     * @param isExit     {@code true} if the units leave the board from there, at the end of the route
     * @param phaseLine  the phase line the waypoint is on, or {@code null}
     * @param isPlanned  {@code true} for a turning point the bot planned, drawn as a dot rather than a flag
     */
    record RouteFlag(Coords hex, int boardId, int colorIndex, String label, String step, int facing,
          int holdTurns, boolean isAssemble, boolean isExit, @Nullable String phaseLine, boolean isPlanned) {

        /**
         * A waypoint the player set.
         */
        RouteFlag(Coords hex, int boardId, int colorIndex, String label, String step, int facing, int holdTurns,
              boolean isAssemble, boolean isExit, @Nullable String phaseLine) {
            this(hex, boardId, colorIndex, label, step, facing, holdTurns, isAssemble, isExit, phaseLine, false);
        }

        /**
         * @return the line under the unit's name: the waypoint's name, and what the units do there if they stop or
         *       leave, e.g. {@code Beta hold 2}, {@code Beta form up} or {@code Delta exit}
         */
        String progressText() {
            String text;
            if (isExit) {
                text = Messages.getString("BotCommandPanel.MoveOrder.flagExit", step);
            } else if (isAssemble) {
                text = Messages.getString("BotCommandPanel.MoveOrder.flagAssemble", step);
            } else {
                text = (holdTurns > 0) ? Messages.getString("BotCommandPanel.MoveOrder.flagHold", step, holdTurns)
                      : step;
            }
            // a phase line is named on its flag, so "holding at PL Bravo" can be found on the map
            return (phaseLine == null) ? text
                  : Messages.getString("BotCommandPanel.MoveOrder.flagPhaseLine", text, PhaseLine.display(phaseLine));
        }
    }

    /**
     * A hex on the way between two points of a route, marked with a small dot so the route reads as a line.
     *
     * @param hex        the hex
     * @param boardId    its board
     * @param colorIndex which route's colour it takes
     */
    record TrailDot(Coords hex, int boardId, int colorIndex) {}

    /**
     * Works out the dots between the points of each route the viewer may see: every hex a straight line crosses from
     * the lead unit to its first waypoint, and from each waypoint to the next, the points themselves left out
     * (HammerGS, 2026-10-03).
     *
     * @param units  every unit in the game, in id order
     * @param viewer the player at this client, or {@code null} for none
     *
     * @return the dots, route by route, in the colours {@link #routeFlags} gives the routes
     */
    static List<TrailDot> routeTrails(List<Entity> units, @Nullable Player viewer) {
        List<TrailDot> dots = new ArrayList<>();
        int colorIndex = 0;
        for (RouteGroups.RouteGroup group : RouteGroups.visibleTo(units, viewer)) {
            Entity guide = group.guide();
            List<Coords> points = new ArrayList<>();
            if ((guide.getPosition() != null) && guide.isDeployed()) {
                points.add(guide.getPosition());
            }
            points.addAll(guide.getUnitOrders().getRoute());
            Set<Coords> marked = new HashSet<>(points);
            for (int index = 1; index < points.size(); index++) {
                for (Coords hex : stepsBetween(points.get(index - 1), points.get(index))) {
                    if (marked.add(hex)) {
                        dots.add(new TrailDot(hex, guide.getBoardId(), colorIndex));
                    }
                }
            }
            colorIndex++;
        }
        return dots;
    }

    /**
     * The hexes a unit passes going straight from one hex to another, a step at a time toward the target: one hex a
     * row, with a single jog where the way changes column. Every hex the straight line touches zig-zagged between two
     * columns where the line ran along their shared edge, and the trail read as two lines of dots side by side
     * (HammerGS's playtest, 2026-10-04).
     *
     * @param from the hex the way starts at
     * @param to   the hex it ends at
     *
     * @return the hexes, both ends included
     */
    static List<Coords> stepsBetween(Coords from, Coords to) {
        List<Coords> steps = new ArrayList<>();
        Coords position = from;
        steps.add(position);
        // each step toward the target brings it one hex nearer; the cap only guards against a loop
        int stepsLeft = from.distance(to);
        while (!position.equals(to) && (stepsLeft-- > 0)) {
            position = position.translated(position.direction(to));
            steps.add(position);
        }
        return steps;
    }

    private void renewSprites() {
        clear();
        List<Entity> units = new ArrayList<>(game.getEntitiesVector());
        units.sort(Comparator.comparingInt(Entity::getId));
        for (TrailDot dot : routeTrails(units, localPlayer())) {
            if (clientGUI.getBoardView(dot.boardId()) instanceof BoardView tacticalBoardView) {
                RouteDotSprite sprite = new RouteDotSprite(tacticalBoardView, dot.hex(),
                      ROUTE_COLORS.get(dot.colorIndex() % ROUTE_COLORS.size()), RouteDotSprite.TRAIL_DIAMETER);
                currentSprites.add(sprite);
                tacticalBoardView.addSprites(List.of(sprite));
            }
        }
        for (RouteFlag flag : routeFlags(units, localPlayer())) {
            IBoardView boardView = clientGUI.getBoardView(flag.boardId());
            if (flag.isPlanned() && (boardView instanceof BoardView tacticalBoardView)) {
                // a turning point the bot planned is a dot on the way, not a flag of its own
                RouteDotSprite sprite = new RouteDotSprite(tacticalBoardView, flag.hex(),
                      ROUTE_COLORS.get(flag.colorIndex() % ROUTE_COLORS.size()), RouteDotSprite.TURN_DIAMETER);
                currentSprites.add(sprite);
                tacticalBoardView.addSprites(List.of(sprite));
                continue;
            }
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
                // the last waypoint is held until new orders or left by the board edge: it has no hold count to show
                boolean isLast = step == route.size() - 1;
                int holdTurns = isLast ? 0 : waypointOrder.getHoldTurns();
                // the flag carries the nav point name the radio uses, so "Nav Point Gamma" can be found on the map
                int navNumber = waypointOrder.getNavNumber();
                String stepName = (navNumber == NavPoint.UNNAMED) ? String.valueOf(step + 1)
                      : NavPoint.name(navNumber);
                if (waypointOrder.isPlannedTurn()) {
                    // a turning point the bot planned on its way, not one the player set
                    stepName = Messages.getString("BotCommandPanel.MoveOrder.flagPlanned");
                }
                flags.add(new RouteFlag(route.get(step), guide.getBoardId(), colorIndex, group.label(), stepName,
                      waypointOrder.getFacing(), holdTurns, !isLast && waypointOrder.isAssemble(),
                      isLast && waypointOrder.isExitBoard(), waypointOrder.getPhaseLine(),
                      waypointOrder.isPlannedTurn()));
            }
            colorIndex++;
        }
        return flags;
    }

    /**
     * The orders at a waypoint, for the hex tooltip when the viewer hovers over its flag: which units, the nav point,
     * the formation on the way or re-formed into there, the facing on arrival, what the units do there and the
     * route's priority. One block per route that has a waypoint on the hex.
     *
     * @param units   every unit in the game, in id order
     * @param viewer  the player at this client, or {@code null} for none
     * @param hex     the hex hovered over
     * @param boardId the board it is on
     *
     * @return the summary as lines joined by {@code <br>}, one blank line between routes; empty when no route the
     *       viewer may see has a waypoint there
     */
    public static String tooltipFor(List<Entity> units, @Nullable Player viewer, Coords hex, int boardId) {
        List<String> blocks = new ArrayList<>();
        for (RouteGroups.RouteGroup group : RouteGroups.visibleTo(units, viewer)) {
            Entity guide = group.guide();
            UnitOrders orders = guide.getUnitOrders();
            List<Coords> route = orders.getRoute();
            for (int step = 0; step < route.size(); step++) {
                if ((guide.getBoardId() == boardId) && route.get(step).equals(hex)) {
                    blocks.add(describeWaypoint(group, orders, step));
                }
            }
        }
        return String.join("<br><br>", blocks);
    }

    private static String describeWaypoint(RouteGroups.RouteGroup group, UnitOrders orders, int step) {
        WaypointOrder waypointOrder = orders.getWaypointOrder(step);
        Coords hex = orders.getRoute().get(step);
        List<String> lines = new ArrayList<>();
        int navNumber = waypointOrder.getNavNumber();
        lines.add("<b>" + ((navNumber == NavPoint.UNNAMED)
              ? Messages.getString("BotCommandPanel.Waypoint.tooltip.step", step + 1, hex.getBoardNum(), group.label())
              : Messages.getString("BotCommandPanel.Waypoint.tooltip.nav", NavPoint.name(navNumber),
                    hex.getBoardNum(), group.label())) + "</b>");
        // HammerGS: say which formation the lance moves here in, and where it changes shape
        String place = placeName(waypointOrder, step, hex);
        lines.add(Messages.getString("BotCommandPanel.Waypoint.tooltip.formation", place,
              formationText(legFormation(orders, step))));
        if (waypointOrder.getArrivalFormation() != null) {
            lines.add(Messages.getString("BotCommandPanel.Waypoint.tooltip.reform", place,
                  formationText(waypointOrder.getArrivalFormation())));
        } else if (step < orders.getRoute().size() - 1) {
            WaypointFormation nextLeg = legFormation(orders, step + 1);
            if (!formationText(nextLeg).equals(formationText(legFormation(orders, step)))) {
                Coords nextHex = orders.getRoute().get(step + 1);
                lines.add(Messages.getString("BotCommandPanel.Waypoint.tooltip.changeAfter",
                      placeName(orders.getWaypointOrder(step + 1), step + 1, nextHex), formationText(nextLeg)));
            }
        }
        String facingText = (waypointOrder.getFacing() == UnitOrders.FACING_AUTO)
              ? Messages.getString("BotCommandPanel.MoveOrder.facing.next")
              : Messages.getString("BotCommandPanel.Orders.facing." + waypointOrder.getFacing());
        lines.add(Messages.getString("BotCommandPanel.Waypoint.tooltip.facing", facingText));
        lines.add(Messages.getString("BotCommandPanel.Waypoint.tooltip.then",
              thenText(waypointOrder, step == orders.getRoute().size() - 1)));
        if (waypointOrder.getPhaseLine() != null) {
            lines.add(Messages.getString("BotCommandPanel.Waypoint.tooltip.phaseLine",
                  PhaseLine.display(waypointOrder.getPhaseLine())));
        }
        lines.add(Messages.getString("BotCommandPanel.Waypoint.tooltip.priority",
              Messages.getString("BotCommandPanel.Orders.priority." + orders.getPriority().name())));
        return String.join("<br>", lines);
    }

    /**
     * @return the formation for the leg ending at that waypoint: the one set on it, else the units' own
     */
    private static WaypointFormation legFormation(UnitOrders orders, int step) {
        WaypointFormation leg = orders.getWaypointOrder(step).getFormation();
        if (leg != null) {
            return leg;
        }
        return orders.getFormation().map(unitsOwn -> new WaypointFormation(unitsOwn.getShape(), unitsOwn.getSpacing(),
              unitsOwn.getPace(), unitsOwn.getContactRule(), unitsOwn.isKeepTogether())).orElse(WaypointFormation.NONE);
    }

    /**
     * @return the waypoint as the summary names it: {@code Nav Point Beta}, or {@code waypoint 2} when it has no name
     */
    private static String placeName(WaypointOrder waypointOrder, int step, Coords hex) {
        int navNumber = waypointOrder.getNavNumber();
        return (navNumber == NavPoint.UNNAMED)
              ? Messages.getString("BotCommandPanel.Waypoint.tooltip.placeStep", step + 1, hex.getBoardNum())
              : Messages.getString("BotCommandPanel.Waypoint.tooltip.placeNav", NavPoint.name(navNumber));
    }

    private static String thenText(WaypointOrder waypointOrder, boolean isLast) {
        if (isLast) {
            return Messages.getString(waypointOrder.isExitBoard() ? "BotCommandPanel.MoveOrder.then.EXIT"
                  : "BotCommandPanel.MoveOrder.then.STAY");
        }
        if (waypointOrder.isAssemble()) {
            return Messages.getString("BotCommandPanel.Waypoint.tooltip.upTo",
                  Messages.getString("BotCommandPanel.MoveOrder.then.ASSEMBLE"), waypointOrder.getHoldTurns());
        }
        if (waypointOrder.isHold()) {
            return Messages.getString("BotCommandPanel.MoveOrder.then.HOLD") + ' '
                  + Messages.getString("BotCommandPanel.MoveOrder.holdTurns", waypointOrder.getHoldTurns());
        }
        return Messages.getString("BotCommandPanel.MoveOrder.then.PASS");
    }

    private static String formationText(WaypointFormation formation) {
        if (formation.isNone()) {
            return Messages.getString("BotCommandPanel.Waypoint.tooltip.noFormation");
        }
        return Messages.getString("BotCommandPanel.Formations.shape." + formation.getShape()) + " ("
              + Messages.getString("BotCommandPanel.Formations.spacing.hexes", formation.getSpacing()) + ", "
              + Messages.getString("BotCommandPanel.Formations.pace." + formation.getPace().name()) + ", "
              + Messages.getString("BotCommandPanel.Formations.contact." + formation.getContactRule().name())
              + (formation.isKeepTogether() ? Messages.getString("BotCommandPanel.Waypoint.tooltip.together") : "")
              + ")";
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
