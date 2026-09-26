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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.pathfinder.BoardEdgePathFinder;
import megamek.common.pathfinder.MovementType;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * How many movement points every hex of a board is from one waypoint, by the cheapest way a unit can actually walk,
 * drive or hover there. It is worked out once, outward from the waypoint, so every unit heading for the same waypoint
 * shares it.
 *
 * <p>The bot still picks each turn's move from the moves it can legally make; this only tells it which of those
 * moves leave it closest to the waypoint by the real route. That is what gets a unit round a horseshoe lake or out of
 * a dead-end street: the hexes along the way round really are fewer movement points from the waypoint than the shore
 * facing it across the water (issue #7615). The technique is a "Dijkstra map", as described at
 * https://www.redblobgames.com/pathfinding/tower-defense/.</p>
 *
 * <p>Entering a hex costs one point, plus the terrain's extra cost, plus one per level of elevation change. The
 * hexes a unit can enter and the climbs it can make follow {@link BoardClusterTracker}, so this field never treats as
 * reachable a hex the bot's reachability check would refuse. The costs are a guide, not the rules: facing changes
 * and movement-mode details are left to the legal moves the bot chooses among.</p>
 */
final class WaypointDistanceField {

    private static final MMLogger LOGGER = MMLogger.create(WaypointDistanceField.class);

    /** The cost of a hex the unit cannot reach from the waypoint at all. */
    static final int UNREACHABLE = Integer.MAX_VALUE;

    // the waypoint measured to, or null for a field to a board edge (MM @Nullable is not applicable to fields)
    private final Coords waypoint;
    private final Map<Coords, Integer> costToWaypoint;

    private WaypointDistanceField(@Nullable Coords waypoint, Map<Coords, Integer> costToWaypoint) {
        this.waypoint = waypoint;
        this.costToWaypoint = costToWaypoint;
    }

    /**
     * Works out the field for one unit's way of moving.
     *
     * @param mover    a unit whose movement type decides which hexes can be entered and climbed
     * @param waypoint the hex to measure to
     *
     * @return the field
     */
    static WaypointDistanceField build(Entity mover, Coords waypoint) {
        Board board = (mover.getGame() == null) ? null : mover.getGame().getBoard(mover);
        if ((board == null) || !board.contains(waypoint) || !isEnterable(mover, MovementType.getMovementType(mover),
              waypoint)) {
            LOGGER.debug("[BotOrders] {}: waypoint {} cannot be entered; no distance field", mover.getDisplayName(),
                  waypoint.getBoardNum());
            return new WaypointDistanceField(waypoint, new HashMap<>());
        }
        return new WaypointDistanceField(waypoint, fill(mover, board, List.of(waypoint), waypoint.getBoardNum()));
    }

    /**
     * Works out the field to a whole board edge: the movement points from every hex to the nearest hex of the edge the
     * unit can stand on, by the cheapest way round. A unit behind a lake then takes the way round to the edge instead
     * of freezing on the shore nearest it (HammerGS's playtest, 2026-09-26).
     *
     * @param mover a unit whose movement type decides which hexes can be entered and climbed
     * @param edge  the edge to measure to
     *
     * @return the field; it reaches nothing when the unit can stand on no hex of the edge
     */
    static WaypointDistanceField buildToEdge(Entity mover, CardinalEdge edge) {
        Board board = (mover.getGame() == null) ? null : mover.getGame().getBoard(mover);
        if (board == null) {
            return new WaypointDistanceField(null, new HashMap<>());
        }
        MovementType movementType = MovementType.getMovementType(mover);
        List<Coords> edgeHexes = new ArrayList<>();
        for (Coords hex : edgeHexes(board, edge)) {
            if (isEnterable(mover, movementType, hex)) {
                edgeHexes.add(hex);
            }
        }
        return new WaypointDistanceField(null, fill(mover, board, edgeHexes, edge.name() + " edge"));
    }

    private static List<Coords> edgeHexes(Board board, CardinalEdge edge) {
        List<Coords> hexes = new ArrayList<>();
        int width = board.getWidth();
        int height = board.getHeight();
        switch (edge) {
            case NORTH -> {
                for (int x = 0; x < width; x++) {
                    hexes.add(new Coords(x, 0));
                }
            }
            case SOUTH -> {
                for (int x = 0; x < width; x++) {
                    hexes.add(new Coords(x, height - 1));
                }
            }
            case WEST -> {
                for (int y = 0; y < height; y++) {
                    hexes.add(new Coords(0, y));
                }
            }
            case EAST -> {
                for (int y = 0; y < height; y++) {
                    hexes.add(new Coords(width - 1, y));
                }
            }
            default -> {
            }
        }
        return hexes;
    }

    /**
     * Spreads the cost outward from the goal hexes, which cost nothing, walking backward: a unit in each neighbour
     * would step into the hex already reached.
     */
    private static Map<Coords, Integer> fill(Entity mover, Board board, List<Coords> goals, String goalName) {
        Map<Coords, Integer> costToGoal = new HashMap<>();
        MovementType movementType = MovementType.getMovementType(mover);
        boolean isHovercraft = movementType == MovementType.Hover;
        boolean isAmphibious = (movementType == MovementType.WheeledAmphibious)
              || (movementType == MovementType.TrackedAmphibious);
        int maxElevationChange = mover.getMaxElevationChange();

        PriorityQueue<long[]> frontier = new PriorityQueue<>((first, second) -> Long.compare(first[0], second[0]));
        for (Coords goal : goals) {
            costToGoal.put(goal, 0);
            frontier.add(new long[] { 0, goal.getX(), goal.getY() });
        }
        while (!frontier.isEmpty()) {
            long[] entry = frontier.poll();
            int cost = (int) entry[0];
            Coords current = new Coords((int) entry[1], (int) entry[2]);
            if (cost > costToGoal.getOrDefault(current, UNREACHABLE)) {
                continue;
            }
            Hex currentHex = board.getHex(current);
            int currentElevation = BoardEdgePathFinder.calculateUnitElevationInHex(currentHex, mover, isHovercraft,
                  isAmphibious);
            for (int direction = 0; direction < 6; direction++) {
                Coords neighbor = current.translated(direction);
                if (!board.contains(neighbor) || !isEnterable(mover, movementType, neighbor)) {
                    continue;
                }
                int neighborElevation = BoardEdgePathFinder.calculateUnitElevationInHex(board.getHex(neighbor),
                      mover, isHovercraft, isAmphibious);
                int elevationChange = Math.abs(currentElevation - neighborElevation);
                if (elevationChange > maxElevationChange) {
                    continue;
                }
                int stepCost = 1 + Math.max(0, currentHex.movementCost(mover)) + elevationChange;
                int neighborCost = cost + stepCost;
                if (neighborCost < costToGoal.getOrDefault(neighbor, UNREACHABLE)) {
                    costToGoal.put(neighbor, neighborCost);
                    frontier.add(new long[] { neighborCost, neighbor.getX(), neighbor.getY() });
                }
            }
        }
        LOGGER.debug("[BotOrders] distance field to {} for {}: {} hexes reachable", goalName, movementType,
              costToGoal.size());
        return costToGoal;
    }

    private static boolean isEnterable(Entity mover, MovementType movementType, Coords coords) {
        return !mover.isLocationProhibited(coords)
              && !BoardClusterTracker.buildingPlowThroughRequired(mover, movementType, coords);
    }

    /**
     * @return the waypoint this field measures to, or {@code null} for a field to a board edge
     */
    @Nullable Coords getWaypoint() {
        return waypoint;
    }

    /**
     * @param position a hex on the board
     *
     * @return the movement points from that hex to the waypoint by the cheapest route, or {@link #UNREACHABLE}
     */
    int costFrom(Coords position) {
        return costToWaypoint.getOrDefault(position, UNREACHABLE);
    }
}
