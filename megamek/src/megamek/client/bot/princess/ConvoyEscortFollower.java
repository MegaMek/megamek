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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import megamek.common.OffBoardDirection;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.LanceRole;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.commands.UnitOrderCommand;

/**
 * Convoys and their escorts (HammerGS, 2026-10-02): sends a convoy off by its exit edge once its route is done, works
 * out each escort's place round its convoy - ahead, beside or behind, aimed at the convoy's next waypoint - and the way
 * escorts face, deploy and leave once the convoy is gone. What the bot knows of each convoy comes from
 * {@link ConvoyTracker}. Part of {@link UnitOrdersFollower}.
 */
class ConvoyEscortFollower {

    private static final MMLogger LOGGER = MMLogger.create(ConvoyEscortFollower.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    // escorts: each escort lance's places, by its force id; the place each escort keeps, by unit id; escorts already
    // told their convoy is gone
    private final Map<Integer, EscortPlan> escortPlans = new HashMap<>();
    private final Map<Integer, LanceRole.Position> escortPositionsKept = new HashMap<>();
    private final Set<Integer> convoyGoneLogged = new HashSet<>();

    /** How many of the exit edge's hexes, nearest first, a convoy tries before it gives up on the edge. */
    private static final int EXIT_HEXES_TRIED = 12;

    /**
     * The places of one escort lance, worked out once for a moment of the turn: the path ranker asks for a unit's
     * place for every path it scores.
     */
    private record EscortPlan(String moment, Map<Integer, Coords> places) {}

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    ConvoyEscortFollower(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    private int currentRound() {
        return follower.currentRound();
    }

    /**
     * Sends a convoy off the board by its exit edge once its route is done: a waypoint on that edge, set to exit, goes
     * on the end of its route - or makes its route, when it has none - so the column drives there in formation and
     * leaves together, the way any route ending in Exit does (HammerGS, 2026-10-03: a convoy given no route milled at
     * the north edge for twelve rounds). Only the convoy's leader, or a convoy unit in no formation, is routed; the
     * column follows it.
     *
     * @param entity a unit of the bot
     */
    void routeConvoyOut(Entity entity) {
        LanceRole role = entity.getLanceRole();
        UnitOrders orders = entity.getUnitOrders();
        if ((role == null) || !role.isConvoy() || role.isWaitingAtRouteEnd()
              || follower.roster().isFormationFollower(entity) || entity.isAirborne()
              || (orders.getEdgeOrder() != EdgeOrder.NONE) || orders.isPaused()
              || orders.isStoppedInRound(currentRound())) {
            // paused or stopped, it waits where it is like any lance
            return;
        }
        List<Coords> route = orders.getRoute();
        if (!route.isEmpty() && orders.getWaypointOrder(route.size() - 1).isExitBoard()) {
            return;
        }
        Coords from = route.isEmpty() ? entity.getPosition() : route.get(route.size() - 1);
        Optional<Coords> edgeHex = convoyExitHex(entity, from, role.getExitEdge());
        if (edgeHex.isEmpty()) {
            LOGGER.info("[BotOrders] CONVOY_EXIT {} (ID {}) round {}: no hex of the {} edge it can drive to",
                  entity.getDisplayName(), entity.getId(), currentRound(), role.getExitEdge());
            return;
        }
        List<Coords> hexes = new ArrayList<>(route);
        hexes.add(edgeHex.get());
        List<WaypointOrder> waypointOrders = new ArrayList<>();
        for (int index = 0; index < route.size(); index++) {
            waypointOrders.add(orders.getWaypointOrder(index));
        }
        // the last leg travels in the convoy's own formation, as a leg that sets none does; it is planned when the bot
        // made the whole route, else as the player's last leg was
        WaypointOrder.RoutePlan exitPlan = route.isEmpty() ? WaypointOrder.RoutePlan.PLAN_LEG
              : orders.getWaypointOrder(route.size() - 1).getRoutePlan();
        waypointOrders.add(new WaypointOrder(UnitOrders.FACING_AUTO, WaypointOrder.HoldMode.PASS, 0, null, true)
              .withRoutePlan((exitPlan == WaypointOrder.RoutePlan.TURN_POINT) ? WaypointOrder.RoutePlan.NONE
                    : exitPlan));
        entity.setUnitOrders(UnitOrderAction.ROUTE.apply(orders, hexes, waypointOrders, OffBoardDirection.NONE,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, currentRound(), null));
        owner.sendChat(UnitOrderCommand.commandText(entity.getId(), UnitOrderAction.ROUTE,
              UnitOrderCommand.hexesArgument(hexes, waypointOrders)));
        LOGGER.info("[BotOrders] CONVOY_EXIT {} (ID {}) round {}: {} - then off the {} edge at {}",
              entity.getDisplayName(), entity.getId(), currentRound(),
              route.isEmpty() ? "no route" : "following its route", role.getExitEdge(),
              edgeHex.get().getBoardNum());
    }

    /**
     * @return the hex of the edge the convoy can drive to that is nearest the hex it sets out from
     */
    private Optional<Coords> convoyExitHex(Entity entity, Coords from, OffBoardDirection edge) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return Optional.empty();
        }
        List<Coords> edgeHexes = new ArrayList<>();
        boolean isAcross = (edge == OffBoardDirection.NORTH) || (edge == OffBoardDirection.SOUTH);
        int length = isAcross ? board.getWidth() : board.getHeight();
        for (int step = 0; step < length; step++) {
            edgeHexes.add(switch (edge) {
                case NORTH -> new Coords(step, 0);
                case SOUTH -> new Coords(step, board.getHeight() - 1);
                case WEST -> new Coords(0, step);
                default -> new Coords(board.getWidth() - 1, step);
            });
        }
        edgeHexes.sort(Comparator.comparingInt(hex -> hex.distance(from)));
        for (int index = 0; (index < edgeHexes.size()) && (index < EXIT_HEXES_TRIED); index++) {
            Coords hex = edgeHexes.get(index);
            int cost = follower.distances().routeCost(entity, hex, entity.getPosition(), false, false);
            if (cost != WaypointDistanceField.UNREACHABLE) {
                return Optional.of(hex);
            }
        }
        return Optional.empty();
    }

    /**
     * A convoy set to wait, with no route left, holds where it is until given one; set to leave, it would be on its
     * way to its edge.
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is a convoy waiting for orders
     */
    boolean isConvoyWaitingForOrders(Entity entity) {
        LanceRole role = entity.getLanceRole();
        return (role != null) && role.isConvoy() && role.isWaitingAtRouteEnd() && !entity.getUnitOrders().hasRoute()
              && (entity.getUnitOrders().getEdgeOrder() == EdgeOrder.NONE)
              && !follower.roster().isFormationFollower(entity);
    }

    /**
     * The hex an escort heads for this turn: its place round its convoy - ahead of the head, beside the middle or
     * behind the tail, with "ahead" aimed at the convoy's next waypoint (HammerGS, 2026-10-02). Where the convoy has
     * still to move this turn, the places are round where it will be, a walk on toward that waypoint. Once the convoy
     * is gone: an escort set to Follow holds where the convoy fell, or leaves with it by its edge (see
     * {@link UnitOrdersFollower#getOrderedEdge}); one set to Break off fights as an ordinary lance.
     *
     * @param entity a unit of the bot
     *
     * @return the place, or empty for a unit that is not escorting a convoy on the board
     */
    public Optional<Coords> getEscortPlace(Entity entity) {
        LanceRole role = entity.getLanceRole();
        if ((role == null) || !role.isEscort() || (entity.getPosition() == null)) {
            return Optional.empty();
        }
        List<Entity> convoy = follower.convoys().unitsOnBoard(role.getConvoyForceId(), entity.getBoardId());
        if (convoy.isEmpty()) {
            Coords fellAt = follower.convoys().lastCenter(role.getConvoyForceId()).orElse(null);
            if ((role.getWhenConvoyGone() == LanceRole.WhenConvoyGone.FOLLOW) && (fellAt != null)
                  && follower.convoys().exitEdgeLeftBy(role.getConvoyForceId()).isEmpty()) {
                logConvoyGone(entity, "holding where the convoy fell, " + fellAt.getBoardNum());
                return Optional.of(fellAt);
            }
            logConvoyGone(entity, (role.getWhenConvoyGone() == LanceRole.WhenConvoyGone.FOLLOW)
                  ? "following it off the board" : "breaking off to fight as a lance");
            return Optional.empty();
        }
        return Optional.ofNullable(escortPlan(entity, role, convoy).places().get(entity.getId()));
    }

    /**
     * @return {@code true} if the unit escorts a convoy that is on the board
     */
    public boolean isEscorting(Entity entity) {
        return getEscortPlace(entity).isPresent();
    }

    private EscortPlan escortPlan(Entity escort, LanceRole role, List<Entity> convoy) {
        Entity head = ConvoyTracker.head(convoy);
        boolean isConvoyStillToMove = follower.convoys().isStillToMove(head);
        List<Entity> escorts = escortsOf(escort, role);
        String moment = currentRound() + ":" + owner.getGame().getPhase() + ":" + head.getId() + ":"
              + head.getPosition() + ":" + isConvoyStillToMove + ":" + escorts.size() + ":" + convoy.size();
        EscortPlan known = escortPlans.get(escort.getForceId());
        if ((known != null) && known.moment().equals(moment)) {
            return known;
        }
        Board board = owner.getGame().getBoard(head);
        Coords headNow = head.getPosition();
        Optional<Coords> convoyWaypoint = head.getUnitOrders().getNextWaypoint();
        ConvoyTracker.Shape shape = follower.convoys().expectedShape(role.getConvoyForceId(), convoy);
        int heading = shape.heading();
        Coords headThen = shape.head();

        int distance = escortDistance(role);
        List<LanceRole.Position> positions = new ArrayList<>(role.getPositions());
        Map<LanceRole.Position, Coords> placeHexes = new HashMap<>();
        for (LanceRole.Position position : positions) {
            placeHexes.put(position, EscortPlanner.place(shape, position, distance, board));
        }
        Map<Integer, Coords> escortPositions = new HashMap<>();
        for (Entity unit : escorts) {
            escortPositions.put(unit.getId(), unit.getPosition());
        }
        Map<Integer, LanceRole.Position> assigned = EscortPlanner.assign(escortPositions, positions, placeHexes,
              escortPositionsKept);
        escortPositionsKept.putAll(assigned);

        Map<Integer, Coords> places = new HashMap<>();
        StringBuilder detail = new StringBuilder();
        for (LanceRole.Position position : positions) {
            int order = 0;
            for (Entity unit : escorts) {
                if (assigned.get(unit.getId()) == position) {
                    Coords place = EscortPlanner.sharedPlace(placeHexes.get(position), heading, order++);
                    places.put(unit.getId(), place);
                    detail.append(String.format("; %s %s at %s (now %s)", position, unit.getShortName(),
                          place.getBoardNum(), unit.getPosition().getBoardNum()));
                }
            }
        }
        LOGGER.info("[BotOrders] ESCORT_PLACES round {} {}: convoy head {} at {}{} heading {} toward {}, {} hexes "
                    + "out{}",
              currentRound(), owner.getGame().getPhase(), head.getShortName(), headNow.getBoardNum(),
              isConvoyStillToMove ? " (still to move, expected at " + headThen.getBoardNum() + ")" : "", heading,
              convoyWaypoint.map(Coords::getBoardNum).orElse("its exit edge"), distance, detail);
        EscortPlan plan = new EscortPlan(moment, places);
        escortPlans.put(escort.getForceId(), plan);
        return plan;
    }

    /**
     * @return how far out an escort keeps: the middle of its band, 5 hexes for Medium
     */
    private static int escortDistance(LanceRole role) {
        return (role.getDistance().getNearest() + role.getDistance().getFurthest()) / 2;
    }

    /**
     * Where an escort should deploy: at an open place round its convoy, once the convoy is on the board. The places
     * are filled in the role's order as the escorts deploy - the first at Lead, the next at Left, and so on.
     *
     * @param entity a unit about to deploy
     *
     * @return the place, or empty for a unit that is not an escort, or whose convoy has not deployed yet
     */
    public Optional<Coords> getEscortDeploymentPlace(Entity entity) {
        LanceRole role = entity.getLanceRole();
        if ((role == null) || !role.isEscort()) {
            return Optional.empty();
        }
        List<Entity> convoy = follower.convoys().unitsOnBoard(role.getConvoyForceId(), entity.getBoardId());
        if (convoy.isEmpty()) {
            return Optional.empty();
        }
        Board board = owner.getGame().getBoard(ConvoyTracker.head(convoy));
        ConvoyTracker.Shape shape = follower.convoys().currentShape(role.getConvoyForceId(), convoy);
        List<LanceRole.Position> positions = new ArrayList<>(role.getPositions());
        int deployed = 0;
        for (Entity unit : owner.getGame().getEntitiesVector()) {
            if ((unit.getId() != entity.getId()) && (unit.getForceId() == entity.getForceId())
                  && (unit.getLanceRole() != null) && unit.getLanceRole().isEscort()
                  && ConvoyTracker.isOnBoardAndAlive(unit)) {
                deployed++;
            }
        }
        LanceRole.Position position = positions.get(deployed % positions.size());
        Coords place = EscortPlanner.place(shape, position, escortDistance(role), board);
        LOGGER.info("[BotOrders] {} (ID {}): deploying as the convoy's {} escort, place {}", entity.getDisplayName(),
              entity.getId(), position, place.getBoardNum());
        return Optional.of(place);
    }

    /**
     * @return the units of the escort's lance escorting the same convoy, on the board and able to move
     */
    private List<Entity> escortsOf(Entity escort, LanceRole role) {
        List<Entity> escorts = new ArrayList<>();
        for (Entity unit : owner.getGame().getEntitiesVector()) {
            LanceRole unitRole = unit.getLanceRole();
            if ((unit.getForceId() == escort.getForceId()) && (unitRole != null) && unitRole.isEscort()
                  && (unitRole.getConvoyForceId() == role.getConvoyForceId()) && ConvoyTracker.isOnBoardAndAlive(unit)
                  && (unit.getBoardId() == escort.getBoardId())) {
                escorts.add(unit);
            }
        }
        if (escorts.isEmpty()) {
            escorts.add(escort);
        }
        return escorts;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return the way an escort faces: the way its convoy is going, the heading its places are kept by; empty for a
     *       unit not escorting a convoy on the board
     */
    OptionalInt escortFacing(Entity entity) {
        LanceRole role = entity.getLanceRole();
        if ((role == null) || !role.isEscort() || !isEscorting(entity)) {
            return OptionalInt.empty();
        }
        List<Entity> convoy = follower.convoys().unitsOnBoard(role.getConvoyForceId(), entity.getBoardId());
        if (convoy.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(follower.convoys().heading(role.getConvoyForceId(), ConvoyTracker.head(convoy)));
    }

    /**
     * @return the edge an escort set to Follow leaves by: its convoy's, once the convoy has left the board by it
     */
    Optional<OffBoardDirection> escortExitEdge(Entity entity) {
        LanceRole role = entity.getLanceRole();
        if ((role == null) || !role.isEscort() || (role.getWhenConvoyGone() != LanceRole.WhenConvoyGone.FOLLOW)
              || (entity.getPosition() == null) || !follower.convoys().unitsOnBoard(role.getConvoyForceId(),
              entity.getBoardId()).isEmpty()) {
            return Optional.empty();
        }
        return follower.convoys().exitEdgeLeftBy(role.getConvoyForceId());
    }

    private void logConvoyGone(Entity entity, String what) {
        if (convoyGoneLogged.add(entity.getId())) {
            LOGGER.info("[BotOrders] ESCORT_CONVOY_GONE {} (ID {}) round {}: its convoy is off the board; {}",
                  entity.getDisplayName(), entity.getId(), currentRound(), what);
        }
    }
}
