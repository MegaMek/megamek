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
package megamek.common.orders;

import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * The changes a player can make to a bot unit's {@link UnitOrders}, and how each one changes them. The server command
 * that carries orders from a client and any test that sets orders both go through {@link #apply}, so there is one
 * definition of what, for example, Stop does.
 */
public enum UnitOrderAction {
    /** Replace the route with the given hexes. */
    ROUTE,
    /** Add the given hexes to the end of the route. */
    ADD,
    /**
     * Replace the route with an edited copy of it, such as a waypoint's facing changed from the map: a pause and a
     * hold under way at the next waypoint carry on.
     */
    EDIT_ROUTE,
    /** Take the last hex off the route. */
    REMOVE_LAST,
    /**
     * The unit reached the first hex of its route: take it off the front. The last hex stays: the unit holds the ground
     * it was sent to, fights anything that comes in range, and returns to it, until given new orders.
     */
    REACHED,
    /** The unit cannot reach the first hex of its route: take it off the front and carry on with the rest. */
    SKIP,
    /** The unit reached a waypoint it holds at: count its hold from this round. */
    HOLD_STARTED,
    /** Clear every order. */
    CLEAR,
    /** Hold in place, keeping the route. */
    PAUSE,
    /** Carry on with the route after a pause. */
    RESUME,
    /** Clear every order and hold in place for the rest of the round. */
    STOP,
    /** Move to an edge and hold there. */
    MOVE_TO_EDGE,
    /** Move to an edge and leave the board by it. */
    EXIT_BY_EDGE,
    /** Set the facing while moving and when stopped. */
    FACING,
    /** Set how hard the unit pushes for its route. */
    PRIORITY,
    /** Put the unit in a formation, or change its place in one. */
    FORMATION,
    /** Take the unit out of its formation. */
    FORMATION_OFF;

    /**
     * Returns the orders after this action.
     *
     * @param current          the unit's orders now
     * @param hexes            the hexes for {@link #ROUTE} and {@link #ADD}
     * @param edge             the edge for {@link #MOVE_TO_EDGE} and {@link #EXIT_BY_EDGE}
     * @param facingWhileMoving the facing while moving for {@link #FACING}, or {@link UnitOrders#FACING_AUTO}
     * @param facingWhenStopped the facing when stopped for {@link #FACING}, or {@link UnitOrders#FACING_AUTO}
     * @param priority         the priority for {@link #PRIORITY}; a {@link #ROUTE} with a priority also sets it
     * @param currentRound     the current round, for {@link #STOP}
     *
     * @return the new orders
     *
     * @throws IllegalArgumentException when the action is missing what it needs, such as a route with no hexes
     */
    public UnitOrders apply(UnitOrders current, List<Coords> hexes, OffBoardDirection edge, int facingWhileMoving,
          int facingWhenStopped, @Nullable OrderPriority priority, int currentRound) {
        return apply(current, hexes, edge, facingWhileMoving, facingWhenStopped, priority, currentRound, null);
    }

    /**
     * Returns the orders after this action, for any action including {@link #FORMATION}.
     *
     * @param current           the unit's orders now
     * @param hexes             the hexes for {@link #ROUTE} and {@link #ADD}
     * @param edge              the edge for {@link #MOVE_TO_EDGE} and {@link #EXIT_BY_EDGE}
     * @param facingWhileMoving the facing while moving for {@link #FACING}, or {@link UnitOrders#FACING_AUTO}
     * @param facingWhenStopped the facing when stopped for {@link #FACING}, or {@link UnitOrders#FACING_AUTO}
     * @param priority          the priority for {@link #PRIORITY}; a {@link #ROUTE} with a priority also sets it
     * @param currentRound      the current round, for {@link #STOP}
     * @param formation         the unit's place in a formation, for {@link #FORMATION}
     *
     * @return the new orders
     *
     * @throws IllegalArgumentException when the action is missing what it needs
     */
    public UnitOrders apply(UnitOrders current, List<Coords> hexes, OffBoardDirection edge, int facingWhileMoving,
          int facingWhenStopped, @Nullable OrderPriority priority, int currentRound,
          @Nullable FormationOrder formation) {
        return apply(current, hexes, List.of(), edge, facingWhileMoving, facingWhenStopped, priority, currentRound,
              formation);
    }

    /**
     * Returns the orders after this action, with what the unit does at each waypoint of a {@link #ROUTE} or
     * {@link #ADD}.
     *
     * @param current           the unit's orders now
     * @param hexes             the hexes for {@link #ROUTE} and {@link #ADD}
     * @param waypointOrders    what to do at each of those hexes, in the same order; missing ones pass through
     * @param edge              the edge for {@link #MOVE_TO_EDGE} and {@link #EXIT_BY_EDGE}
     * @param facingWhileMoving the facing while moving for {@link #FACING}, or {@link UnitOrders#FACING_AUTO}
     * @param facingWhenStopped the facing when stopped for {@link #FACING}, or {@link UnitOrders#FACING_AUTO}
     * @param priority          the priority for {@link #PRIORITY}; a {@link #ROUTE} with a priority also sets it
     * @param currentRound      the current round, for {@link #STOP} and {@link #HOLD_STARTED}
     * @param formation         the unit's place in a formation, for {@link #FORMATION}
     *
     * @return the new orders
     *
     * @throws IllegalArgumentException when the action is missing what it needs
     */
    public UnitOrders apply(UnitOrders current, List<Coords> hexes, List<WaypointOrder> waypointOrders,
          OffBoardDirection edge, int facingWhileMoving, int facingWhenStopped, @Nullable OrderPriority priority,
          int currentRound, @Nullable FormationOrder formation) {
        return switch (this) {
            case ROUTE -> {
                requireHexes(hexes);
                UnitOrders routed = current.withRoute(hexes, waypointOrders);
                yield (priority == null) ? routed : routed.withPriority(priority);
            }
            case ADD -> {
                requireHexes(hexes);
                yield current.withWaypointsAdded(hexes, waypointOrders);
            }
            case EDIT_ROUTE -> current.withRouteEdited(hexes, waypointOrders);
            case REMOVE_LAST -> current.withLastWaypointRemoved();
            case SKIP -> current.withNextWaypointReached();
            case HOLD_STARTED -> current.hasRoute() ? current.withHoldStarted(currentRound) : current;
            case REACHED -> (current.getRoute().size() > 1) ? current.withNextWaypointReached() : current;
            case CLEAR -> UnitOrders.NONE;
            case PAUSE -> current.withPaused(true);
            case RESUME -> current.withPaused(false);
            case STOP -> UnitOrders.stoppedInRound(currentRound);
            case MOVE_TO_EDGE -> current.withEdgeOrder(EdgeOrder.MOVE_TO, requireEdge(edge));
            case EXIT_BY_EDGE -> current.withEdgeOrder(EdgeOrder.EXIT_BY, requireEdge(edge));
            case FACING -> current.withFacings(facingWhileMoving, facingWhenStopped);
            case PRIORITY -> {
                if (priority == null) {
                    throw new IllegalArgumentException("A priority order needs a priority");
                }
                yield current.withPriority(priority);
            }
            case FORMATION -> {
                if (formation == null) {
                    throw new IllegalArgumentException("A formation order needs a shape and a leader");
                }
                yield current.withFormation(formation);
            }
            case FORMATION_OFF -> current.withFormation(null);
        };
    }

    private static void requireHexes(List<Coords> hexes) {
        if (hexes.isEmpty()) {
            throw new IllegalArgumentException("A route order needs at least one hex");
        }
    }

    private static OffBoardDirection requireEdge(OffBoardDirection edge) {
        if (edge == OffBoardDirection.NONE) {
            throw new IllegalArgumentException("An edge order needs an edge");
        }
        return edge;
    }
}
