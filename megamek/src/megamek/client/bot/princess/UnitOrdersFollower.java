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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import megamek.client.bot.Messages;
import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.pathfinder.MovementType;
import megamek.common.units.Entity;
import megamek.common.util.BoardUtilities;
import megamek.logging.MMLogger;
import megamek.server.commands.UnitOrderCommand;
import org.apache.logging.log4j.Level;

/**
 * Carries out the standing orders players give a bot's units ({@link Entity#getUnitOrders()}): holds paused and
 * stopped units, sends units with an edge order to that edge, advances routes as waypoints are reached, measures how
 * far a position is from the next waypoint by the real route, and says which way an ordered unit should face.
 *
 * <p>The orders are stored on the units, so every change the bot makes - reaching a waypoint, dropping one it cannot
 * reach - is applied to its own copy at once and sent to the server with the same {@code /unitOrder} command a
 * player uses. Both Princess and CASPAR follow orders through this class.</p>
 */
public class UnitOrdersFollower {

    private static final MMLogger LOGGER = MMLogger.create(UnitOrdersFollower.class);

    /** How much more an Imperative route's pull counts than a Normal one in the path score. */
    static final double IMPERATIVE_ROUTE_WEIGHT = 3.0;

    /** How much a unit on an Imperative route weighs the damage it expects: half, so it walks past enemies. */
    static final double IMPERATIVE_DAMAGE_WEIGHT = 0.5;

    /** How much a unit on a Normal route weighs the damage it expects: a little more, so it takes cover on the way. */
    static final double NORMAL_ROUTE_DAMAGE_WEIGHT = 1.25;

    /** Hex facings on each side of the ordered one that still count as its front arc. */
    private static final int FRONT_ARC_HALF_WIDTH = 1;

    private final Princess owner;
    private final Set<Integer> arrivedUnitIds = new HashSet<>();
    private final Map<String, WaypointDistanceField> distanceFields = new HashMap<>();
    private int distanceFieldsRound = -1;

    /**
     * @param owner the bot whose units follow orders
     */
    UnitOrdersFollower(Princess owner) {
        this.owner = owner;
    }

    private int currentRound() {
        return owner.getGame().getCurrentRound();
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if a Pause order, or a Stop order given this round, holds the unit where it is
     */
    public boolean isHolding(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        return orders.isPaused() || orders.isStoppedInRound(currentRound()) || isHoldingRouteEnd(entity);
    }

    /**
     * A unit that has reached the last hex of its route holds it while no enemy is within reach of its weapons. When
     * one is, the unit is free to fight; once the enemy is gone, the last hex pulls it back.
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is at the end of its route with no enemy in range
     */
    public boolean isHoldingRouteEnd(Entity entity) {
        return isAtRouteEnd(entity) && !isEnemyInRange(entity);
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is within {@link Princess#DISTANCE_TO_WAYPOINT} of the last hex of its route
     */
    public boolean isAtRouteEnd(Entity entity) {
        List<Coords> route = entity.getUnitOrders().getRoute();
        return (route.size() == 1) && (entity.getPosition() != null)
              && (entity.getPosition().distance(route.get(0)) <= Princess.DISTANCE_TO_WAYPOINT);
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if a known enemy is within the unit's longest weapon range
     */
    public boolean isEnemyInRange(Entity entity) {
        int weaponRange = owner.getMaxWeaponRange(entity);
        for (Entity enemy : owner.getEnemyEntities()) {
            if ((enemy.getPosition() != null) && (enemy.getBoardId() == entity.getBoardId())
                  && (enemy.getPosition().distance(entity.getPosition()) <= weaponRange)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How much a unit weighs the damage it expects on the way along a player's route: a Normal route a little more
     * than usual, so the unit takes cover, an Imperative one half as much, so it pushes past the enemy (issue #7615).
     *
     * @param entity a unit of the bot
     *
     * @return the factor for the damage the unit expects to take, 1 for a unit not following a route
     */
    double damageWeight(Entity entity) {
        if (owner.getUnitBehaviorTracker().getActiveWaypoint(entity, owner).isEmpty() || isAtRouteEnd(entity)) {
            return 1.0;
        }
        return (entity.getUnitOrders().getPriority() == OrderPriority.IMPERATIVE) ? IMPERATIVE_DAMAGE_WEIGHT
              : NORMAL_ROUTE_DAMAGE_WEIGHT;
    }

    /**
     * Gives every one of the bot's units on the board an order to exit by an edge, replacing their routes: what the
     * bot-wide flee order now means. {@link CardinalEdge#NEAREST} sends each unit to its own nearest edge.
     *
     * @param edge the edge to flee toward
     */
    public void orderAllToExit(CardinalEdge edge) {
        for (Entity entity : owner.getEntitiesOwned()) {
            if ((entity.getPosition() == null) || entity.isAirborne()) {
                continue;
            }
            CardinalEdge unitEdge = (edge == CardinalEdge.NEAREST) ? BoardUtilities.getClosestEdge(entity) : edge;
            OffBoardDirection direction = toOffBoardDirection(unitEdge);
            if (direction == OffBoardDirection.NONE) {
                continue;
            }
            entity.setUnitOrders(UnitOrderAction.EXIT_BY_EDGE.apply(entity.getUnitOrders(), List.of(), direction,
                  UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, currentRound()));
            owner.sendChat(UnitOrderCommand.commandText(entity.getId(), UnitOrderAction.EXIT_BY_EDGE,
                  UnitOrderCommand.EDGE + '=' + direction.name()));
        }
        LOGGER.info("[BotOrders] {}: flee order - every unit exits by the {} edge", owner.getName(), edge);
    }

    /**
     * Clears the edge orders a flee order gave, when the flee order is cancelled.
     */
    public void cancelExitOrders() {
        for (Entity entity : owner.getEntitiesOwned()) {
            if (entity.getUnitOrders().getEdgeOrder() == EdgeOrder.EXIT_BY) {
                change(entity, UnitOrderAction.CLEAR);
            }
        }
        LOGGER.info("[BotOrders] {}: flee order cancelled - exit orders cleared", owner.getName());
    }

    /**
     * @param edge an edge as the bot's movement code names it
     *
     * @return the same edge as orders store it; {@link OffBoardDirection#NONE} for none or nearest
     */
    static OffBoardDirection toOffBoardDirection(CardinalEdge edge) {
        return switch (edge) {
            case NORTH -> OffBoardDirection.NORTH;
            case SOUTH -> OffBoardDirection.SOUTH;
            case EAST -> OffBoardDirection.EAST;
            case WEST -> OffBoardDirection.WEST;
            default -> OffBoardDirection.NONE;
        };
    }

    /**
     * @param entity a unit of the bot
     *
     * @return the edge the unit is ordered to move to or exit by, if it has an edge order
     */
    public Optional<CardinalEdge> getOrderedEdge(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        if (orders.getEdgeOrder() == EdgeOrder.NONE) {
            return Optional.empty();
        }
        return Optional.of(toCardinalEdge(orders.getEdge()));
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is ordered to leave the board by an edge
     */
    public boolean isOrderedToExit(Entity entity) {
        return entity.getUnitOrders().getEdgeOrder() == EdgeOrder.EXIT_BY;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is ordered to an edge to hold there, and so must not leave the board
     */
    public boolean isOrderedToHoldAtEdge(Entity entity) {
        return entity.getUnitOrders().getEdgeOrder() == EdgeOrder.MOVE_TO;
    }

    /**
     * @param edge an edge as orders store it
     *
     * @return the same edge as the bot's movement code names it
     */
    static CardinalEdge toCardinalEdge(OffBoardDirection edge) {
        return switch (edge) {
            case NORTH -> CardinalEdge.NORTH;
            case SOUTH -> CardinalEdge.SOUTH;
            case EAST -> CardinalEdge.EAST;
            case WEST -> CardinalEdge.WEST;
            case NONE -> CardinalEdge.NONE;
        };
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
        if (distanceFieldsRound != currentRound()) {
            distanceFields.clear();
            distanceFieldsRound = currentRound();
        }
        String key = waypoint.getBoardNum() + '|' + MovementType.getMovementType(mover) + '|' + mover.getBoardId()
              + '|' + mover.getMaxElevationChange();
        WaypointDistanceField field = distanceFields.get(key);
        if (field == null) {
            field = WaypointDistanceField.build(mover, waypoint);
            distanceFields.put(key, field);
        }
        return field.costFrom(position);
    }

    /**
     * @param entity a unit of the bot
     *
     * @return how strongly the unit's route pulls in its path score: {@link #IMPERATIVE_ROUTE_WEIGHT} for an
     *       Imperative route, 1 otherwise
     */
    double routeWeight(Entity entity) {
        return (entity.getUnitOrders().getPriority() == OrderPriority.IMPERATIVE) ? IMPERATIVE_ROUTE_WEIGHT : 1.0;
    }

    /**
     * The facing the player ordered for the end of this move, or {@link UnitOrders#FACING_AUTO}. A move that ends at
     * the last waypoint, or a unit holding, takes the "when stopped" facing; any other move takes the "while moving"
     * facing.
     *
     * @param entity    the unit
     * @param finalHex  where the move ends
     *
     * @return the ordered facing 0-5, or {@link UnitOrders#FACING_AUTO}
     */
    int orderedFacing(Entity entity, Coords finalHex) {
        UnitOrders orders = entity.getUnitOrders();
        List<Coords> route = orders.getRoute();
        boolean endsStopped = route.isEmpty()
              || ((route.size() == 1) && (finalHex.distance(route.get(0)) <= Princess.DISTANCE_TO_WAYPOINT));
        return endsStopped ? orders.getFacingWhenStopped() : orders.getFacingWhileMoving();
    }

    /**
     * Decides whether the player's ordered facing stands, given where the bot expects fire from. The order stands
     * while that fire falls in the ordered facing's front arc; otherwise the bot turns to the threat, which protects
     * the unit's side and rear armor.
     *
     * @param orderedFacing the ordered facing 0-5, or {@link UnitOrders#FACING_AUTO}
     * @param position      where the unit ends its move
     * @param threat        where the bot expects fire from, or {@code null} when it knows of no enemy
     *
     * @return the ordered facing if it stands, otherwise {@link UnitOrders#FACING_AUTO}
     */
    static int facingThatStands(int orderedFacing, Coords position, @Nullable Coords threat) {
        if (orderedFacing == UnitOrders.FACING_AUTO) {
            return UnitOrders.FACING_AUTO;
        }
        if ((threat == null) || threat.equals(position)) {
            return orderedFacing;
        }
        int threatDirection = position.direction(threat);
        int sidesApart = Math.abs(threatDirection - orderedFacing) % 6;
        sidesApart = Math.min(sidesApart, 6 - sidesApart);
        return (sidesApart <= FRONT_ARC_HALF_WIDTH) ? orderedFacing : UnitOrders.FACING_AUTO;
    }

    /**
     * @param position a hex
     * @param path     a candidate move
     *
     * @return {@code true} if the move ends within {@link Princess#DISTANCE_TO_WAYPOINT} of the hex
     */
    static boolean endsAt(Coords position, MovePath path) {
        return path.getFinalCoords().distance(position) <= Princess.DISTANCE_TO_WAYPOINT;
    }

    /**
     * Counts the steps of a move that take the unit further from its target than the step before. A unit with
     * movement to spare near a waypoint otherwise runs a loop past it and back, to bank the defence bonus for hexes
     * moved; each step away is scored like a hex further from the destination.
     *
     * @param path   a candidate move
     * @param target the waypoint or slot the unit is heading for
     *
     * @return the number of steps that moved away from the target
     */
    static int backtrackSteps(MovePath path, Coords target) {
        Coords previous = path.getStartCoords();
        if (previous == null) {
            return 0;
        }
        int previousDistance = previous.distance(target);
        int stepsAway = 0;
        for (MoveStep step : path.getStepVector()) {
            Coords position = step.getPosition();
            if ((position == null) || position.equals(previous)) {
                continue;
            }
            int distance = position.distance(target);
            if (distance > previousDistance) {
                stepsAway++;
            }
            previous = position;
            previousDistance = distance;
        }
        return stepsAway;
    }

    /**
     * Takes reached waypoints off the front of every route, after the bot's units have moved. A unit that reaches
     * its last waypoint holds there, facing as ordered, until it gets new orders.
     */
    void advanceRoutes() {
        for (Entity entity : owner.getEntitiesOwned()) {
            if (entity.getPosition() == null) {
                continue;
            }
            Optional<Coords> waypoint = entity.getUnitOrders().getNextWaypoint();
            if (waypoint.isEmpty() || (waypoint.get().distance(entity.getPosition()) > Princess.DISTANCE_TO_WAYPOINT)) {
                continue;
            }
            if (entity.getUnitOrders().getRoute().size() > 1) {
                LOGGER.info("[BotOrders] {} (ID {}) reached waypoint {}", entity.getDisplayName(), entity.getId(),
                      waypoint.get().getBoardNum());
                change(entity, UnitOrderAction.REACHED);
            } else if (arrivedUnitIds.add(entity.getId())) {
                // the last hex stays in the route: the unit holds it and comes back to it after a fight
                LOGGER.info("[BotOrders] {} (ID {}) reached the end of its route at {}", entity.getDisplayName(),
                      entity.getId(), waypoint.get().getBoardNum());
                owner.sendChat(Messages.getString("Princess.orders.arrived", entity.getDisplayName(),
                      waypoint.get().getBoardNum()), Level.INFO);
            }
        }
    }

    /**
     * Drops the unit's next waypoint because the unit cannot reach it, and says so.
     *
     * @param entity the unit
     */
    void dropUnreachableWaypoint(Entity entity) {
        Optional<Coords> waypoint = entity.getUnitOrders().getNextWaypoint();
        if (waypoint.isEmpty()) {
            return;
        }
        LOGGER.info("[BotOrders] {} (ID {}): waypoint {} cannot be reached; dropping it", entity.getDisplayName(),
              entity.getId(), waypoint.get().getBoardNum());
        owner.sendChat(Messages.getString("Princess.orders.unreachable", entity.getDisplayName(),
              waypoint.get().getBoardNum()), Level.INFO);
        change(entity, UnitOrderAction.SKIP);
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

    /**
     * Replaces the unit's route.
     *
     * @param entity the unit
     * @param hexes  the new route; must not be empty
     */
    void setRoute(Entity entity, List<Coords> hexes) {
        change(entity, UnitOrderAction.ROUTE, hexes);
    }

    /**
     * Adds hexes to the end of the unit's route.
     *
     * @param entity the unit
     * @param hexes  the hexes to add; must not be empty
     */
    void addToRoute(Entity entity, List<Coords> hexes) {
        change(entity, UnitOrderAction.ADD, hexes);
    }

    /**
     * Makes a change to a unit's orders the bot itself decided on, or that came in through the bot's own chat
     * commands: applies it to the bot's copy of the unit at once and sends it to the server for everyone else.
     *
     * @param entity the unit
     * @param action the change; one that needs no edge, facing or priority
     */
    void change(Entity entity, UnitOrderAction action) {
        change(entity, action, List.of());
    }

    private void change(Entity entity, UnitOrderAction action, List<Coords> hexes) {
        UnitOrders newOrders = action.apply(entity.getUnitOrders(), hexes, OffBoardDirection.NONE,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, currentRound());
        entity.setUnitOrders(newOrders);
        String command = hexes.isEmpty()
              ? UnitOrderCommand.commandText(entity.getId(), action)
              : UnitOrderCommand.commandText(entity.getId(), action, UnitOrderCommand.hexesArgument(hexes));
        owner.sendChat(command);
    }
}
