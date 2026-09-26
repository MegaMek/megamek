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

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import megamek.common.Hex;
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

    private final Coords waypoint;
    private final Map<Coords, Integer> costToWaypoint;

    private WaypointDistanceField(Coords waypoint, Map<Coords, Integer> costToWaypoint) {
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
        Map<Coords, Integer> costToWaypoint = new HashMap<>();
        Board board = (mover.getGame() == null) ? null : mover.getGame().getBoard(mover);
        if (board == null) {
            return new WaypointDistanceField(waypoint, costToWaypoint);
        }
        MovementType movementType = MovementType.getMovementType(mover);
        boolean isHovercraft = movementType == MovementType.Hover;
        boolean isAmphibious = (movementType == MovementType.WheeledAmphibious)
              || (movementType == MovementType.TrackedAmphibious);
        int maxElevationChange = mover.getMaxElevationChange();

        if (!board.contains(waypoint) || !isEnterable(mover, movementType, waypoint)) {
            LOGGER.debug("[BotOrders] {}: waypoint {} cannot be entered; no distance field", mover.getDisplayName(),
                  waypoint.getBoardNum());
            return new WaypointDistanceField(waypoint, costToWaypoint);
        }

        PriorityQueue<long[]> frontier = new PriorityQueue<>((first, second) -> Long.compare(first[0], second[0]));
        costToWaypoint.put(waypoint, 0);
        frontier.add(new long[] { 0, waypoint.getX(), waypoint.getY() });
        while (!frontier.isEmpty()) {
            long[] entry = frontier.poll();
            int cost = (int) entry[0];
            Coords current = new Coords((int) entry[1], (int) entry[2]);
            if (cost > costToWaypoint.getOrDefault(current, UNREACHABLE)) {
                continue;
            }
            Hex currentHex = board.getHex(current);
            int currentElevation = BoardEdgePathFinder.calculateUnitElevationInHex(currentHex, mover, isHovercraft,
                  isAmphibious);
            for (int direction = 0; direction < 6; direction++) {
                // walking backward from the waypoint: a unit in the neighbor would step into the current hex
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
                if (neighborCost < costToWaypoint.getOrDefault(neighbor, UNREACHABLE)) {
                    costToWaypoint.put(neighbor, neighborCost);
                    frontier.add(new long[] { neighborCost, neighbor.getX(), neighbor.getY() });
                }
            }
        }
        LOGGER.debug("[BotOrders] distance field to {} for {}: {} hexes reachable", waypoint.getBoardNum(),
              movementType, costToWaypoint.size());
        return new WaypointDistanceField(waypoint, costToWaypoint);
    }

    private static boolean isEnterable(Entity mover, MovementType movementType, Coords coords) {
        return !mover.isLocationProhibited(coords)
              && !BoardClusterTracker.buildingPlowThroughRequired(mover, movementType, coords);
    }

    /**
     * @return the waypoint this field measures to
     */
    Coords getWaypoint() {
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
