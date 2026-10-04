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
import java.util.Optional;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.pathfinder.MovementType;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * How far a hex is from a unit's waypoint or from a board edge by the cheapest route the unit can take, in movement
 * points, and whether a unit can get somewhere on foot at all. Units moving the same way share one
 * {@link WaypointDistanceField} a round. Part of {@link UnitOrdersFollower}.
 */
class RouteDistances {

    private static final MMLogger LOGGER = MMLogger.create(RouteDistances.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    private final Map<String, WaypointDistanceField> distanceFields = new HashMap<>();
    private int distanceFieldsRound = -1;

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    RouteDistances(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    private int currentRound() {
        return follower.currentRound();
    }

    /**
     * How far a position is from the unit's next waypoint by the cheapest route the unit can take, in movement
     * points. Units heading for the same waypoint share one {@link WaypointDistanceField}, worked out once a round.
     *
     * @param mover    the unit
     * @param waypoint the waypoint
     * @param position the position to measure from
     *
     * @return the movement points to the waypoint, or {@link WaypointDistanceField#UNREACHABLE}
     */
    int routeCostFrom(Entity mover, Coords waypoint, Coords position) {
        boolean isTownSpot = follower.townLegs().isTownSpotOf(mover, waypoint);
        boolean isSlowest = isTownSpot && follower.townLegs().isSlowestOfLance(mover);
        return routeCost(mover, waypoint, position, isTownSpot, isSlowest);
    }

    /**
     * @param isGoingRoundUnitsInPlace {@code true} to make the hexes in front of units already in their places cost
     *                                 more, for a unit making for its own place on a town leg
     * @param isKeepingOutOfStreets    {@code true} to make narrow streets cost more, for one of a lance's slowest
     *                                 units on a town leg
     */
    int routeCost(Entity mover, Coords waypoint, Coords position, boolean isGoingRoundUnitsInPlace,
          boolean isKeepingOutOfStreets) {
        WaypointDistanceField field = routeField(mover, waypoint, isGoingRoundUnitsInPlace, isKeepingOutOfStreets);
        return (field == null) ? WaypointDistanceField.UNREACHABLE : field.costFrom(position);
    }

    /**
     * @return the unit's route field to the waypoint, worked out once a round for each way of moving; {@code null} for
     *       a unit that flies over the terrain
     */
    private @Nullable WaypointDistanceField routeField(Entity mover, Coords waypoint,
          boolean isGoingRoundUnitsInPlace, boolean isKeepingOutOfStreets) {
        if (MovementType.getMovementType(mover) == MovementType.Flyer) {
            // a VTOL or fighter flies over the terrain; the straight line stands in
            return null;
        }
        if (distanceFieldsRound != currentRound()) {
            distanceFields.clear();
            distanceFieldsRound = currentRound();
        }
        String key = waypoint.getBoardNum() + '|' + moverKey(mover);
        Map<Coords, Integer> extraCost = new HashMap<>();
        if (isGoingRoundUnitsInPlace) {
            Map<Coords, Integer> frontOfUnitsInPlace = TownLegPlanner.frontOfUnitsInPlace(
                  follower.townLegs().unitsInPlaceBeside(mover));
            if (!frontOfUnitsInPlace.isEmpty()) {
                // the field differs with who stands where; keyed so units of one lance share it this round
                key += "|front " + frontOfUnitsInPlace.keySet();
                extraCost.putAll(frontOfUnitsInPlace);
            }
        }
        if (isKeepingOutOfStreets) {
            key += "|narrow";
            for (Map.Entry<Coords, Integer> narrow : follower.townLegs().narrowHexes(mover).entrySet()) {
                extraCost.merge(narrow.getKey(), narrow.getValue(), Integer::sum);
            }
        }
        WaypointDistanceField field = distanceFields.get(key);
        if (field == null) {
            field = WaypointDistanceField.build(mover, waypoint, extraCost);
            distanceFields.put(key, field);
            if (isKeepingOutOfStreets) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: TOWN_LANES - one of the lance's slowest; streets cost {} "
                            + "MP a hex more on its way to {}, so it keeps to the open lanes", mover.getDisplayName(),
                      mover.getId(), currentRound(), TownLegPlanner.NARROW_HEX_COST_FOR_SLOWEST,
                      waypoint.getBoardNum());
            }
        }
        return field;
    }

    /**
     * What a unit's route field depends on, so units that move alike share one: the way it moves, its board, how far
     * it can climb, its weight - a building that bears a 35-ton Panther brings down a 65-ton Longbow, so their routes
     * through a town differ - and whether it keeps to the roads as a convoy.
     *
     * @param mover the unit
     *
     * @return the part of the field's key that comes from the unit
     */
    private String moverKey(Entity mover) {
        return String.valueOf(MovementType.getMovementType(mover)) + '|' + mover.getBoardId() + '|'
              + mover.getMaxElevationChange() + "|t" + mover.getWeight() + (follower.isConvoy(mover) ? "|roads" : "");
    }

    /**
     * Whether a unit following a player's orders can get where it is going on foot, without bringing anything down:
     * the route field reaches it from where the unit stands. Clearing a way through buildings is for when it cannot
     * (HammerGS, 2026-10-01: "the bulldozer plan should be a last resort when no walk path exists").
     *
     * @param mover a unit of the bot
     *
     * @return {@code true} if the unit has a route or edge order and a way on foot to it
     */
    boolean hasWalkingRoute(Entity mover) {
        Coords position = mover.getPosition();
        if (position == null) {
            return false;
        }
        Optional<Coords> destination = owner.getUnitBehaviorTracker().getActiveWaypoint(mover, owner);
        if (destination.isPresent()) {
            return routeCostFrom(mover, destination.get(), position) != WaypointDistanceField.UNREACHABLE;
        }
        Optional<CardinalEdge> edge = follower.getOrderedEdge(mover);
        return edge.isPresent() && (edgeCostFrom(mover, edge.get(), position) != WaypointDistanceField.UNREACHABLE);
    }

    /**
     * How far a position is from a board edge by the cheapest route the unit can take, in movement points, for a unit
     * leaving by or withdrawing to that edge. Units moving the same way share one field a round.
     *
     * @param mover    the unit
     * @param edge     the edge
     * @param position the position to measure from
     *
     * @return the movement points to the nearest hex of the edge the unit can stand on, or
     *       {@link WaypointDistanceField#UNREACHABLE}
     */
    int edgeCostFrom(Entity mover, CardinalEdge edge, Coords position) {
        if (MovementType.getMovementType(mover) == MovementType.Flyer) {
            return WaypointDistanceField.UNREACHABLE;
        }
        if (distanceFieldsRound != currentRound()) {
            distanceFields.clear();
            distanceFieldsRound = currentRound();
        }
        String key = "edge " + edge + '|' + moverKey(mover);
        WaypointDistanceField field = distanceFields.get(key);
        if (field == null) {
            field = WaypointDistanceField.buildToEdge(mover, edge);
            distanceFields.put(key, field);
        }
        return field.costFrom(position);
    }

    /**
     * Whether the unit can get to a hex at all. The bot's quick reachability check, {@link
     * megamek.common.pathfinder.BoardClusterTracker}, can say no for a hex the unit can in fact walk to - for example
     * from a hex its cluster does not join well - so a hex only counts as unreachable when the route distance map
     * agrees that no way there exists.
     *
     * @param entity   the unit
     * @param waypoint the hex
     *
     * @return {@code true} if the unit can reach the hex, or its position is unknown so it cannot be judged
     */
    boolean canReach(Entity entity, Coords waypoint) {
        if (entity.getPosition() == null) {
            return true;
        }
        if (!owner.getClusterTracker().getDestinationCoords(entity, waypoint, true).isEmpty()) {
            return true;
        }
        boolean hasRoute = routeCostFrom(entity, waypoint, entity.getPosition()) != WaypointDistanceField.UNREACHABLE;
        if (hasRoute) {
            LOGGER.info("[BotOrders] {} (ID {}): reachability check refused {} but a route exists; keeping it",
                  entity.getDisplayName(), entity.getId(), waypoint.getBoardNum());
        }
        return hasRoute;
    }
}
