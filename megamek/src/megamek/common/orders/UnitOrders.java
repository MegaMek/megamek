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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import megamek.common.OffBoardDirection;
import megamek.common.board.Coords;

/**
 * The standing orders a player has given one bot-controlled unit: a route of waypoints, how hard to push for it, which
 * way to face, whether to pause, and whether to head for a board edge.
 *
 * <p>The orders live on the unit ({@link megamek.common.units.Entity#getUnitOrders()}), not inside the bot, so they
 * are saved with the game, reach every client, and outlast a bot reconnecting or a unit changing hands. Each instance
 * is immutable: every change returns a new {@code UnitOrders}, so a copy held by one client can never change under
 * another.</p>
 *
 * <p>This is a plain class and not a record on purpose: the save format uses XStream, which cannot read records
 * without a custom converter.</p>
 */
public final class UnitOrders implements Serializable {

    @Serial
    private static final long serialVersionUID = 4180923675021856731L;

    /** A facing of this value leaves the choice to the bot. */
    public static final int FACING_AUTO = -1;

    /** No round: the unit has no Stop order in force. */
    public static final int NO_ROUND = -1;

    /** A unit with no orders. */
    public static final UnitOrders NONE = new UnitOrders(new ArrayList<>(), OrderPriority.NORMAL, FACING_AUTO,
          FACING_AUTO, false, EdgeOrder.NONE, OffBoardDirection.NONE, NO_ROUND);

    private static final int FACING_COUNT = 6;

    private final ArrayList<Coords> route;
    private final OrderPriority priority;
    private final int facingWhileMoving;
    private final int facingWhenStopped;
    private final boolean paused;
    private final EdgeOrder edgeOrder;
    private final OffBoardDirection edge;
    private final int stopRound;

    private UnitOrders(List<Coords> route, OrderPriority priority, int facingWhileMoving, int facingWhenStopped,
          boolean paused, EdgeOrder edgeOrder, OffBoardDirection edge, int stopRound) {
        this.route = new ArrayList<>(route);
        this.priority = Objects.requireNonNull(priority);
        this.facingWhileMoving = validFacing(facingWhileMoving);
        this.facingWhenStopped = validFacing(facingWhenStopped);
        this.paused = paused;
        this.edgeOrder = Objects.requireNonNull(edgeOrder);
        this.edge = Objects.requireNonNull(edge);
        this.stopRound = stopRound;
    }

    private static int validFacing(int facing) {
        if ((facing != FACING_AUTO) && ((facing < 0) || (facing >= FACING_COUNT))) {
            throw new IllegalArgumentException("Facing must be 0-5 or FACING_AUTO, was " + facing);
        }
        return facing;
    }

    /**
     * @return the route's waypoints in the order the unit visits them; empty when there is no route
     */
    public List<Coords> getRoute() {
        return Collections.unmodifiableList(route);
    }

    /**
     * @return the waypoint the unit is heading for now, or empty when the route is finished or was never given
     */
    public Optional<Coords> getNextWaypoint() {
        return route.isEmpty() ? Optional.empty() : Optional.of(route.get(0));
    }

    /**
     * @return {@code true} if the unit has at least one waypoint left
     */
    public boolean hasRoute() {
        return !route.isEmpty();
    }

    /**
     * @return how hard the unit pushes to follow its route
     */
    public OrderPriority getPriority() {
        return priority;
    }

    /**
     * @return the facing (0-5) the unit ends each turn in while it is still moving, or {@link #FACING_AUTO}
     */
    public int getFacingWhileMoving() {
        return facingWhileMoving;
    }

    /**
     * @return the facing (0-5) the unit takes once it stops at the end of its route, or {@link #FACING_AUTO}
     */
    public int getFacingWhenStopped() {
        return facingWhenStopped;
    }

    /**
     * @return {@code true} if the unit holds where it is; the route is kept for when it resumes
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * @return the edge order, {@link EdgeOrder#NONE} when there is none
     */
    public EdgeOrder getEdgeOrder() {
        return edgeOrder;
    }

    /**
     * @return the edge of the edge order, {@link OffBoardDirection#NONE} when there is no edge order
     */
    public OffBoardDirection getEdge() {
        return edge;
    }

    /**
     * @return the round in which the unit was told to stop and hold, or {@link #NO_ROUND}
     */
    public int getStopRound() {
        return stopRound;
    }

    /**
     * @param round the current round
     *
     * @return {@code true} if a Stop order holds the unit in place this round
     */
    public boolean isStoppedInRound(int round) {
        return (stopRound != NO_ROUND) && (stopRound == round);
    }

    /**
     * @return {@code true} if the unit has no orders at all
     */
    public boolean isEmpty() {
        return equals(NONE);
    }

    /**
     * @param newRoute the waypoints to follow, in order
     *
     * @return these orders with the route replaced; a new route also ends a pause, a Stop and any edge order
     */
    public UnitOrders withRoute(List<Coords> newRoute) {
        return new UnitOrders(newRoute, priority, facingWhileMoving, facingWhenStopped, false, EdgeOrder.NONE,
              OffBoardDirection.NONE, NO_ROUND);
    }

    /**
     * @param waypoints the waypoints to add after the existing ones
     *
     * @return these orders with the waypoints added to the end of the route
     */
    public UnitOrders withWaypointsAdded(List<Coords> waypoints) {
        List<Coords> newRoute = new ArrayList<>(route);
        newRoute.addAll(waypoints);
        return new UnitOrders(newRoute, priority, facingWhileMoving, facingWhenStopped, paused, edgeOrder, edge,
              stopRound);
    }

    /**
     * @return these orders without the last waypoint of the route; unchanged when the route is empty
     */
    public UnitOrders withLastWaypointRemoved() {
        if (route.isEmpty()) {
            return this;
        }
        List<Coords> newRoute = new ArrayList<>(route);
        newRoute.remove(newRoute.size() - 1);
        return new UnitOrders(newRoute, priority, facingWhileMoving, facingWhenStopped, paused, edgeOrder, edge,
              stopRound);
    }

    /**
     * @return these orders with the waypoint the unit just reached taken off the front of the route
     */
    public UnitOrders withNextWaypointReached() {
        if (route.isEmpty()) {
            return this;
        }
        return new UnitOrders(route.subList(1, route.size()), priority, facingWhileMoving, facingWhenStopped, paused,
              edgeOrder, edge, stopRound);
    }

    /**
     * @param newPriority how hard to push for the route
     *
     * @return these orders with the priority replaced
     */
    public UnitOrders withPriority(OrderPriority newPriority) {
        return new UnitOrders(route, newPriority, facingWhileMoving, facingWhenStopped, paused, edgeOrder, edge,
              stopRound);
    }

    /**
     * @param newFacingWhileMoving facing 0-5 to end each turn in while moving, or {@link #FACING_AUTO}
     * @param newFacingWhenStopped facing 0-5 to take once stopped, or {@link #FACING_AUTO}
     *
     * @return these orders with both facings replaced
     */
    public UnitOrders withFacings(int newFacingWhileMoving, int newFacingWhenStopped) {
        return new UnitOrders(route, priority, newFacingWhileMoving, newFacingWhenStopped, paused, edgeOrder, edge,
              stopRound);
    }

    /**
     * @param isPaused {@code true} to hold in place, {@code false} to carry on with the route
     *
     * @return these orders paused or resumed; the route is kept either way
     */
    public UnitOrders withPaused(boolean isPaused) {
        return new UnitOrders(route, priority, facingWhileMoving, facingWhenStopped, isPaused, edgeOrder, edge,
              stopRound);
    }

    /**
     * @param newEdgeOrder move to or exit by the edge
     * @param newEdge      the edge
     *
     * @return these orders heading for an edge instead of following the route, which is cleared
     */
    public UnitOrders withEdgeOrder(EdgeOrder newEdgeOrder, OffBoardDirection newEdge) {
        if ((newEdgeOrder != EdgeOrder.NONE) && (newEdge == OffBoardDirection.NONE)) {
            throw new IllegalArgumentException("An edge order needs an edge");
        }
        return new UnitOrders(new ArrayList<>(), priority, facingWhileMoving, facingWhenStopped, false,
              newEdgeOrder, (newEdgeOrder == EdgeOrder.NONE) ? OffBoardDirection.NONE : newEdge, NO_ROUND);
    }

    /**
     * @param round the current round
     *
     * @return orders that clear everything and hold the unit in place for the rest of this round; from the next round
     *       the bot handles the unit normally
     */
    public static UnitOrders stoppedInRound(int round) {
        return new UnitOrders(new ArrayList<>(), OrderPriority.NORMAL, FACING_AUTO, FACING_AUTO, false,
              EdgeOrder.NONE, OffBoardDirection.NONE, round);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UnitOrders otherOrders)) {
            return false;
        }
        return (facingWhileMoving == otherOrders.facingWhileMoving)
              && (facingWhenStopped == otherOrders.facingWhenStopped)
              && (paused == otherOrders.paused)
              && (stopRound == otherOrders.stopRound)
              && route.equals(otherOrders.route)
              && (priority == otherOrders.priority)
              && (edgeOrder == otherOrders.edgeOrder)
              && (edge == otherOrders.edge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route, priority, facingWhileMoving, facingWhenStopped, paused, edgeOrder, edge, stopRound);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("UnitOrders[");
        text.append("route=").append(route);
        text.append(", priority=").append(priority);
        text.append(", facingWhileMoving=").append(facingWhileMoving);
        text.append(", facingWhenStopped=").append(facingWhenStopped);
        text.append(", paused=").append(paused);
        text.append(", edgeOrder=").append(edgeOrder).append(' ').append(edge);
        text.append(", stopRound=").append(stopRound);
        return text.append(']').toString();
    }
}
