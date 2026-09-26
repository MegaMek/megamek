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
import java.util.Set;

import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.GameTurn;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.orders.ContactRule;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.pathfinder.MovementType;
import megamek.common.units.Entity;
import megamek.common.util.BoardUtilities;
import megamek.logging.MMLogger;
import megamek.server.commands.UnitOrderCommand;

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

    /** How far off its slot a formation unit may stand when the slot itself is blocked. */
    static final int FORMATION_SLACK = 1;

    /** A formation that keeps together counts as formed when every unit is this close to its slot. */
    static final int REFORM_SLACK = 2;

    /**
     * The most rounds a leader waits at a waypoint for its formation, however far the last unit has to come, so one
     * stuck unit cannot hold the rest.
     */
    static final int MAXIMUM_REFORM_WAIT_ROUNDS = 6;

    /** Contact range for a formation none of whose units has a weapon: an enemy this close still breaks it. */
    static final int FALLBACK_CONTACT_RANGE = 12;

    private final Princess owner;
    private final Set<Integer> arrivedUnitIds = new HashSet<>();
    private final Map<String, WaypointDistanceField> distanceFields = new HashMap<>();
    private int distanceFieldsRound = -1;
    private final Map<Integer, SlotChoice> slotChoices = new HashMap<>();

    /**
     * A formation unit's worked-out slot, kept while the leader's waypoint and the formation's heading stay the same.
     */
    private record SlotChoice(Coords anchor, int heading, @Nullable Coords slot) {}

    /** The heading a formation takes at its final waypoint when no stopped facing is ordered, fixed once seen. */
    private final Map<String, Integer> finalHeadings = new HashMap<>();

    /**
     * A formation leader waiting at a waypoint for its formation to form up.
     *
     * @param waypoint   the waypoint it waits at
     * @param sinceRound the round it first waited there
     * @param maxRounds  the rounds to wait at most: until the last unit should have arrived, by its path and speed
     */
    private record ReformWait(Coords waypoint, int sinceRound, int maxRounds) {}

    /** The leaders waiting at a waypoint for their formation, by unit id; the bot's own bookkeeping, not saved. */
    private final Map<Integer, ReformWait> reformWaits = new HashMap<>();

    /** The leaders holding at a waypoint until their formation assembles, by unit id; not saved. */
    private final Map<Integer, ReformWait> assemblyWaits = new HashMap<>();

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
     * @return {@code true} if a Pause order, a Stop order given this round, a hold at a waypoint or the end of the
     *       route holds the unit where it is
     */
    public boolean isHolding(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        return orders.isPaused() || orders.isStoppedInRound(currentRound()) || isHoldingAtWaypoint(entity)
              || isWaitingForFormation(entity)
              || isHoldingRouteEnd(entity);
    }

    /**
     * A unit holds at a waypoint part-way along its route for the turns set on it. It stays in the hex and fires at
     * anything in range, but does not chase. A formation unit on its slot holds while its leader does, so the shape
     * waits together.
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is holding at a waypoint this round
     */
    public boolean isHoldingAtWaypoint(Entity entity) {
        int round = currentRound();
        if (entity.getUnitOrders().isHoldingAtWaypoint(round)) {
            return true;
        }
        Optional<Entity> leader = formationLeaderOf(entity);
        if (leader.isEmpty() || !leader.get().getUnitOrders().isHoldingAtWaypoint(round)
              || (entity.getPosition() == null)) {
            return false;
        }
        Optional<Coords> slot = getFormationSlot(entity);
        return slot.isPresent() && entity.getPosition().equals(slot.get());
    }

    /**
     * The facing a unit takes when it stops: the one set on the waypoint it holds at or ends its route on, else its
     * "when stopped" facing.
     *
     * @param entity a unit of the bot
     *
     * @return the facing 0-5, or {@link UnitOrders#FACING_AUTO}
     */
    public int stoppedFacing(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        boolean isStoppedAtWaypoint = orders.hasRoute()
              && ((orders.getRoute().size() == 1) || isHoldingAtWaypoint(entity));
        if (isStoppedAtWaypoint) {
            // a formation unit holding with its leader faces the way set on the leader's waypoint
            Entity waypointOwner = formationLeaderOf(entity).orElse(entity);
            int waypointFacing = waypointOwner.getUnitOrders().getWaypointOrder(0).getFacing();
            if (waypointFacing != UnitOrders.FACING_AUTO) {
                return waypointFacing;
            }
        }
        return orders.getFacingWhenStopped();
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
     * @return {@code true} if the unit is within {@link Princess#DISTANCE_TO_WAYPOINT} of the last hex of its route;
     *       a formation's leader must stand on that hex, and its other units in their slots around it
     */
    public boolean isAtRouteEnd(Entity entity) {
        List<Coords> route = entity.getUnitOrders().getRoute();
        if ((route.size() != 1) || (entity.getPosition() == null)) {
            return false;
        }
        Optional<Entity> leader = formationLeaderOf(entity);
        if (leader.isPresent()) {
            // a formation unit's route ends in its slot beside the leader, once the leader has arrived; checking the
            // waypoint itself stopped the whole formation wherever it stood when the waypoint came within reach
            Optional<Coords> slot = getFormationSlot(entity);
            // exactly on the slot: a unit one hex off counted as arrived and held there, leaving the column
            // ragged (HammerGS's playtest, 2026-09-26); a blocked slot has already moved to a free hex beside it
            return isAtRouteEnd(leader.get()) && slot.isPresent() && entity.getPosition().equals(slot.get());
        }
        int arrivalRadius = isLeadingFormationToLastWaypoint(entity) ? 0 : Princess.DISTANCE_TO_WAYPOINT;
        return entity.getPosition().distance(route.get(0)) <= arrivalRadius;
    }

    /**
     * The formation a unit travels in on its current leg: the one set on its formation leader's next waypoint, else
     * its own formation order. A leg set to travel out of formation has none.
     *
     * @param entity a unit of the bot
     *
     * @return the formation for this leg, with the unit's own leader and slot; empty out of formation
     */
    Optional<FormationOrder> activeFormation(Entity entity) {
        Optional<FormationOrder> base = entity.getUnitOrders().getFormation();
        if (base.isEmpty()) {
            return base;
        }
        // the leader's route sets the leg; the unit's own route stands in step for it once the leader is gone
        Entity leader = owner.getGame().getEntity(base.get().getLeaderId());
        boolean isLeaderRouted = (leader != null) && !leader.isDestroyed() && leader.getUnitOrders().hasRoute();
        UnitOrders legOrders = isLeaderRouted ? leader.getUnitOrders() : entity.getUnitOrders();
        WaypointFormation leg = legOrders.hasRoute() ? legOrders.getWaypointOrder(0).getFormation() : null;
        if (leg == null) {
            return base;
        }
        return leg.isNone() ? Optional.empty() : Optional.of(leg.applyTo(base.get()));
    }

    /**
     * @param entity a unit of the bot
     *
     * @return the unit leading the unit's formation, if the unit is in one and is not leading it
     */
    private Optional<Entity> formationLeaderOf(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || activeFormation(entity).isEmpty()) {
            // out of formation on this leg: the unit follows its own route like a unit on its own
            return Optional.empty();
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        if ((members.size() < 2) || (members.get(0).getId() == entity.getId())) {
            return Optional.empty();
        }
        return Optional.of(members.get(0));
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
            orderExit(entity, direction);
        }
        LOGGER.info("[BotOrders] {}: flee order - every unit exits by the {} edge", owner.getName(), edge);
    }

    /**
     * Orders one unit off the board by an edge, on this client and to every other.
     */
    private void orderExit(Entity entity, OffBoardDirection direction) {
        entity.setUnitOrders(UnitOrderAction.EXIT_BY_EDGE.apply(entity.getUnitOrders(), List.of(), direction,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, currentRound()));
        owner.sendChat(UnitOrderCommand.commandText(entity.getId(), UnitOrderAction.EXIT_BY_EDGE,
              UnitOrderCommand.EDGE + '=' + direction.name()));
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
        if (distanceFieldsRound != currentRound()) {
            distanceFields.clear();
            distanceFieldsRound = currentRound();
        }
        String key = "edge " + edge + '|' + MovementType.getMovementType(mover) + '|' + mover.getBoardId() + '|'
              + mover.getMaxElevationChange();
        WaypointDistanceField field = distanceFields.get(key);
        if (field == null) {
            field = WaypointDistanceField.buildToEdge(mover, edge);
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
        int facing = playerOrderedFacing(entity, finalHex);
        if ((facing == UnitOrders.FACING_AUTO) && owner.getEnemyEntities().isEmpty()) {
            // with no enemy to face, Auto faces along the route rather than wherever the move happens to end
            return facingAlongRoute(entity, finalHex);
        }
        return facing;
    }

    /**
     * The facing a player set for the end of this move - on the waypoint it ends on, for the end of the route, or
     * while moving - without the bot's own choices.
     *
     * @param entity   the unit
     * @param finalHex where the move ends
     *
     * @return the facing 0-5, or {@link UnitOrders#FACING_AUTO} when the player left it to the bot
     */
    int playerOrderedFacing(Entity entity, Coords finalHex) {
        UnitOrders orders = entity.getUnitOrders();
        List<Coords> route = orders.getRoute();
        // a move that ends on the next waypoint takes the facing set on it
        if (!route.isEmpty() && finalHex.equals(route.get(0))
              && (orders.getWaypointOrder(0).getFacing() != UnitOrders.FACING_AUTO)) {
            return orders.getWaypointOrder(0).getFacing();
        }
        boolean endsStopped = route.isEmpty()
              || ((route.size() == 1) && (finalHex.distance(route.get(0)) <= Princess.DISTANCE_TO_WAYPOINT));
        return endsStopped ? stoppedFacing(entity) : orders.getFacingWhileMoving();
    }

    /**
     * How many hexsides a unit can turn its weapons without turning its legs: one for a torso twist, more for an
     * extended twist, three - any way at all - for a turret; none for a unit that cannot. Turning its weapons is free,
     * where turning in place spends movement and counts as having moved.
     *
     * @param entity the unit
     *
     * @return the hexsides either side of its facing that its torso or turret can reach, 0-3
     */
    static int twistReach(Entity entity) {
        if (!entity.canChangeSecondaryFacing()) {
            return 0;
        }
        int reach = 0;
        for (int sides = 1; sides <= 3; sides++) {
            boolean canReach = entity.isValidSecondaryFacing((entity.getFacing() + sides) % 6)
                  && entity.isValidSecondaryFacing((entity.getFacing() + 6 - sides) % 6);
            if (!canReach) {
                break;
            }
            reach = sides;
        }
        return reach;
    }

    /**
     * @param fromFacing the facing turned from, 0-5
     * @param toFacing   the facing turned to, 0-5
     *
     * @return how many hexsides apart the two are, 0-3
     */
    static int sidesApart(int fromFacing, int toFacing) {
        int sides = Math.abs(fromFacing - toFacing) % 6;
        return Math.min(sides, 6 - sides);
    }

    /**
     * The way a unit should twist its torso or turret this fire phase to face the way a player ordered, when it has
     * nothing better to aim at.
     *
     * @param entity a unit of the bot
     *
     * @return the facing to twist to, 0-5, or {@link UnitOrders#FACING_AUTO} when there is no order it can reach
     */
    public int orderedTwist(Entity entity) {
        if (entity.getPosition() == null) {
            return UnitOrders.FACING_AUTO;
        }
        int ordered = isHolding(entity) ? stoppedFacing(entity) : playerOrderedFacing(entity, entity.getPosition());
        boolean canReach = (ordered != UnitOrders.FACING_AUTO) && (ordered != entity.getSecondaryFacing())
              && entity.canChangeSecondaryFacing() && entity.isValidSecondaryFacing(ordered);
        return canReach ? ordered : UnitOrders.FACING_AUTO;
    }

    /**
     * @return the direction from the hex toward the unit's next target - its slot or waypoint, or the waypoint after
     *       it when the move ends on it - or {@link UnitOrders#FACING_AUTO} when there is none
     */
    private int facingAlongRoute(Entity entity, Coords finalHex) {
        Optional<Coords> target = owner.getUnitBehaviorTracker().getActiveWaypoint(entity, owner);
        if (target.isEmpty()) {
            return UnitOrders.FACING_AUTO;
        }
        if (!target.get().equals(finalHex)) {
            return finalHex.direction(target.get());
        }
        List<Coords> route = entity.getUnitOrders().getRoute();
        return (route.size() > 1) ? finalHex.direction(route.get(1)) : UnitOrders.FACING_AUTO;
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
            if (waypoint.isEmpty()) {
                continue;
            }
            boolean isPartWay = entity.getUnitOrders().getRoute().size() > 1;
            if (isPartWay && getFormationSlot(entity).isPresent()) {
                // a unit in formation takes its route from its leader's; ticking off a waypoint it merely passed
                // near sent it on toward the next one, ahead of the formation (HammerGS's playtest, 2026-09-26)
                continue;
            }
            if (isPartWay && entity.getUnitOrders().getWaypointOrder(0).isHold()) {
                advanceHold(entity, waypoint.get());
                continue;
            }
            if (waypoint.get().distance(entity.getPosition()) > Princess.DISTANCE_TO_WAYPOINT) {
                continue;
            }
            if (isPartWay && shouldWaitForFormation(entity, waypoint.get())) {
                continue;
            }
            if (isPartWay) {
                LOGGER.info("[BotOrders] {} (ID {}) reached waypoint {}", entity.getDisplayName(), entity.getId(),
                      waypoint.get().getBoardNum());
                change(entity, UnitOrderAction.REACHED);
            } else if (isAtRouteEnd(entity) && entity.getUnitOrders().getWaypointOrder(0).isExitBoard()
                  && !isFormationFollower(entity)) {
                // the route ends by leaving the board: a formation keeping together first assembles, then leaves as one
                if (!shouldWaitForFormation(entity, waypoint.get())) {
                    exitWithFormation(entity, waypoint.get());
                }
            } else if (isAtRouteEnd(entity) && arrivedUnitIds.add(entity.getId())) {
                // the last hex stays in the route: the unit holds it and comes back to it after a fight
                String arrivalHex = entity.getPosition().getBoardNum();
                LOGGER.info("[BotOrders] {} (ID {}) reached the end of its route at {}", entity.getDisplayName(),
                      entity.getId(), arrivalHex);
                owner.getOrdersRadio().report(entity, "arrived", arrivalHex);
            }
        }
        syncFollowerRoutes();
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit leads a formation that keeps together and is waiting at a waypoint for the
     *       others to form up
     */
    public boolean isWaitingForFormation(Entity entity) {
        ReformWait wait = reformWaits.get(entity.getId());
        Optional<Coords> waypoint = entity.getUnitOrders().getNextWaypoint();
        return (wait != null) && waypoint.isPresent() && wait.waypoint().equals(waypoint.get());
    }

    /**
     * Decides, once a formation leader that keeps together has reached a waypoint part-way along its route, whether it
     * waits there for its formation: until every other unit is within {@link #REFORM_SLACK} of its slot, for at most
     * {@link #MAXIMUM_REFORM_WAIT_ROUNDS} rounds. Then the formation moves on to the next waypoint together.
     *
     * @return {@code true} to wait another round
     */
    private boolean shouldWaitForFormation(Entity leader, Coords waypoint) {
        Optional<FormationOrder> formation = activeFormation(leader);
        if (formation.isEmpty() || !formation.get().isKeepTogether()) {
            return false;
        }
        List<Entity> members = formationMembers(leader, formation.get().getLeaderId());
        boolean isLeading = (members.size() >= 2) && (members.get(0).getId() == leader.getId());
        boolean isBroken = (formation.get().getContactRule() == ContactRule.BREAK)
              && isEnemyNear(leader, contactRange(members));
        if (!isLeading || isBroken) {
            reformWaits.remove(leader.getId());
            return false;
        }
        int outOfPlace = countOutOfPlace(members);
        ReformWait wait = reformWaits.get(leader.getId());
        if ((wait == null) || !wait.waypoint().equals(waypoint)) {
            wait = new ReformWait(waypoint, currentRound(), assemblyWaitRounds(leader, waypoint,
                  MAXIMUM_REFORM_WAIT_ROUNDS));
        }
        int roundsWaited = currentRound() - wait.sinceRound();
        if (outOfPlace == 0) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_FORMED at {} - moving on together",
                  leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum());
            reformWaits.remove(leader.getId());
            return false;
        }
        if (roundsWaited >= wait.maxRounds()) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_WAIT_OVER at {} - {} unit(s) still out of place "
                        + "after {} round(s); moving on", leader.getDisplayName(), leader.getId(), currentRound(),
                  waypoint.getBoardNum(), outOfPlace, roundsWaited);
            reformWaits.remove(leader.getId());
            return false;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_WAIT at {} - {} of {} unit(s) still forming up",
              leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(), outOfPlace,
              members.size() - 1);
        reformWaits.put(leader.getId(), wait);
        return true;
    }

    /**
     * How many turns until the last of a formation's other units reaches its slot: the movement points of the way it
     * can really go there, over the movement points it spends a turn at the formation's pace. A unit that cannot reach
     * its slot at all is not waited for.
     *
     * @param leader the formation's leader
     *
     * @return the turns until the last one arrives; 0 when all are in place
     */
    int estimatedAssemblyTurns(Entity leader) {
        Optional<FormationOrder> formation = activeFormation(leader);
        if (formation.isEmpty()) {
            return 0;
        }
        List<Entity> members = formationMembers(leader, formation.get().getLeaderId());
        int longest = 0;
        for (Entity member : members.subList(Math.min(1, members.size()), members.size())) {
            Optional<Coords> slot = getFormationSlot(member);
            if (slot.isEmpty() || member.getPosition().equals(slot.get())) {
                continue;
            }
            int cost = routeCostFrom(member, slot.get(), member.getPosition());
            if (cost == WaypointDistanceField.UNREACHABLE) {
                continue;
            }
            int perTurn = Math.max(1, paceMovementPoints(member, formation.get().getPace()));
            int turns = (cost + perTurn - 1) / perTurn;
            LOGGER.info("[BotOrders] {} (ID {}) round {}: ASSEMBLY_ESTIMATE - {} is {} MP from its slot at {}, {} MP a "
                        + "turn: {} turn(s)", leader.getDisplayName(), leader.getId(), currentRound(),
                  member.getDisplayName(), cost, slot.get().getBoardNum(), perTurn, turns);
            longest = Math.max(longest, turns);
        }
        return longest;
    }

    /**
     * @return the rounds to wait at a waypoint for the formation: until the last unit should have arrived, plus one
     *       for luck, at least one and at most the cap
     */
    private int assemblyWaitRounds(Entity leader, Coords waypoint, int cap) {
        int rounds = Math.max(1, Math.min(cap, estimatedAssemblyTurns(leader) + 1));
        LOGGER.info("[BotOrders] {} (ID {}) round {}: waits at {} up to {} round(s) for the formation (at most {})",
              leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(), rounds, cap);
        return rounds;
    }

    /**
     * @return {@code true} if a unit waiting at a waypoint for its formation has waited as long as the last unit should
     *       have needed to arrive
     */
    private boolean isAssemblyWaitOver(Entity entity, Coords waypoint) {
        ReformWait wait = assemblyWaits.get(entity.getId());
        return (wait != null) && wait.waypoint().equals(waypoint)
              && (currentRound() - wait.sinceRound() >= wait.maxRounds());
    }

    /**
     * @param members a formation's units, its leader first
     *
     * @return how many of the others are more than {@link #REFORM_SLACK} hexes from their slots
     */
    private int countOutOfPlace(List<Entity> members) {
        int outOfPlace = 0;
        for (Entity member : members.subList(1, members.size())) {
            Optional<Coords> slot = getFormationSlot(member);
            if (slot.isPresent() && (member.getPosition().distance(slot.get()) > REFORM_SLACK)) {
                outOfPlace++;
            }
        }
        return outOfPlace;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit's formation has assembled: every other unit within {@link #REFORM_SLACK} of
     *       its slot; a unit out of formation, or leading no one, counts as assembled
     */
    boolean isFormationAssembled(Entity entity) {
        Optional<FormationOrder> formation = activeFormation(entity);
        if (formation.isEmpty()) {
            return true;
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        return (members.size() < 2) || (countOutOfPlace(members) == 0);
    }

    /**
     * @return {@code true} if the unit follows a formation leader on this leg, so the leader decides for it
     */
    private boolean isFormationFollower(Entity entity) {
        return formationLeaderOf(entity).isPresent();
    }

    /**
     * Orders a unit off the board by the edge nearest the last waypoint of its route, and with it every unit of the
     * formation it leads.
     */
    private void exitWithFormation(Entity entity, Coords lastWaypoint) {
        OffBoardDirection edge = nearestEdge(entity, lastWaypoint);
        List<Entity> leaving = new ArrayList<>(List.of(entity));
        Optional<FormationOrder> formation = activeFormation(entity);
        if (formation.isPresent()) {
            List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
            if (!members.isEmpty() && (members.get(0).getId() == entity.getId())) {
                leaving = members;
            }
        }
        for (Entity unit : leaving) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: end of route at {} - leaving by the {} edge",
                  unit.getDisplayName(), unit.getId(), currentRound(), lastWaypoint.getBoardNum(), edge);
            orderExit(unit, edge);
        }
        owner.getOrdersRadio().report(entity, "exiting", edge.toString());
    }

    /**
     * @return the board edge nearest the hex; ties go north, south, west, then east
     */
    private OffBoardDirection nearestEdge(Entity entity, Coords hex) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return OffBoardDirection.NORTH;
        }
        int toNorth = hex.getY();
        int toSouth = board.getHeight() - 1 - hex.getY();
        int toWest = hex.getX();
        int toEast = board.getWidth() - 1 - hex.getX();
        int nearest = Math.min(Math.min(toNorth, toSouth), Math.min(toWest, toEast));
        if (toNorth == nearest) {
            return OffBoardDirection.NORTH;
        } else if (toSouth == nearest) {
            return OffBoardDirection.SOUTH;
        } else if (toWest == nearest) {
            return OffBoardDirection.WEST;
        }
        return OffBoardDirection.EAST;
    }

    /**
     * Starts a unit's hold when it stands on a waypoint set to hold, and moves it on once it has held for the turns
     * set: reaching a two-turn hold in round 3, it holds in rounds 4 and 5 and moves on in round 6.
     */
    private void advanceHold(Entity entity, Coords waypoint) {
        UnitOrders orders = entity.getUnitOrders();
        int holdTurns = orders.getWaypointOrder(0).getHoldTurns();
        boolean isAssemble = orders.getWaypointOrder(0).isAssemble();
        if (orders.getHoldSinceRound() == UnitOrders.NO_ROUND) {
            if (entity.getPosition().equals(waypoint) && isAssemble && isFormationAssembled(entity)) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: reached {} with the formation assembled; moving on",
                      entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum());
                change(entity, UnitOrderAction.REACHED);
            } else if (entity.getPosition().equals(waypoint)) {
                if (isAssemble) {
                    // wait until the last unit should have arrived, by its path and speed, never past the turns set
                    assemblyWaits.put(entity.getId(), new ReformWait(waypoint, currentRound(),
                          assemblyWaitRounds(entity, waypoint, holdTurns)));
                }
                LOGGER.info("[BotOrders] {} (ID {}) round {}: reached {} and holds {} turn(s), through round {}",
                      entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum(), holdTurns,
                      currentRound() + holdTurns);
                change(entity, UnitOrderAction.HOLD_STARTED);
                owner.getOrdersRadio().report(entity, "holding", waypoint.getBoardNum());
            }
            return;
        }
        if (orders.isHoldDone(currentRound())) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: held {} turn(s) at {}; moving on", entity.getDisplayName(),
                  entity.getId(), currentRound(), holdTurns, waypoint.getBoardNum());
            change(entity, UnitOrderAction.REACHED);
        } else if (isAssemble && isAssemblyWaitOver(entity, waypoint)) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: the formation should have assembled at {} by now; moving on",
                  entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum());
            assemblyWaits.remove(entity.getId());
            change(entity, UnitOrderAction.REACHED);
        } else if (isAssemble && isFormationAssembled(entity)) {
            assemblyWaits.remove(entity.getId());
            LOGGER.info("[BotOrders] {} (ID {}) round {}: formation assembled at {}; moving on before the {} turn(s) "
                  + "were up", entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum(),
                  holdTurns);
            change(entity, UnitOrderAction.REACHED);
        }
    }

    /**
     * The hex a formation unit heads for: its slot around the hex its leader is heading for, laid out along the
     * leader's heading. It stays the same while the leader moves, so each unit has one fixed hex to make for.
     * Empty for a unit not in a formation, for the unit leading it, and while a formation that breaks on contact has an
     * enemy near.
     *
     * <p>If the slot hex is blocked - off the board, somewhere the unit cannot go, or across deep water from the
     * leader - the unit takes the best hex within {@link #FORMATION_SLACK}. If none will do, the formation folds into
     * a Column behind the leader, which is how it gets through a pass or down a street; it opens out again as soon
     * as the slots are clear.</p>
     *
     * @param entity a unit of the bot
     *
     * @return the hex to head for, or empty when the unit is not following a formation leader
     */
    public Optional<Coords> getFormationSlot(Entity entity) {
        Optional<FormationOrder> formation = activeFormation(entity);
        if (formation.isEmpty() || (entity.getPosition() == null)) {
            return Optional.empty();
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        if (members.size() < 2) {
            return Optional.empty();
        }
        Entity leader = members.get(0);
        if ((leader.getId() == entity.getId()) || (leader.getPosition() == null)) {
            return Optional.empty();
        }
        int contactRange = contactRange(members);
        if ((formation.get().getContactRule() == ContactRule.BREAK) && isEnemyNear(leader, contactRange)) {
            LOGGER.debug("[BotOrders] {} (ID {}): formation broken - enemy within {} of {}", entity.getDisplayName(),
                  entity.getId(), contactRange, leader.getDisplayName());
            return Optional.empty();
        }
        Optional<Coords> leaderWaypoint = leader.getUnitOrders().getNextWaypoint();
        Coords anchor = leaderWaypoint.orElse(leader.getPosition());
        int heading = formationHeading(leader, anchor);
        SlotChoice cached = slotChoices.get(entity.getId());
        if ((cached != null) && cached.anchor().equals(anchor) && (cached.heading() == heading)) {
            return Optional.ofNullable(cached.slot());
        }
        Coords slot = chooseSlot(entity, anchor, heading, formation.get(), members.indexOf(entity));
        slotChoices.put(entity.getId(), new SlotChoice(anchor, heading, slot));
        return Optional.ofNullable(slot);
    }

    /**
     * The way the formation faces around its leader's waypoint: the facing set on that waypoint; else toward the next
     * waypoint while more follow; at the last one, the leader's ordered stopped facing, or else the direction of the last leg, fixed the first time it is
     * worked out so the shape does not swing as the leader closes in.
     */
    private int formationHeading(Entity leader, Coords anchor) {
        List<Coords> leaderRoute = leader.getUnitOrders().getRoute();
        // a facing set on the waypoint lays the shape out that way
        int waypointFacing = leader.getUnitOrders().getWaypointOrder(0).getFacing();
        if (!leaderRoute.isEmpty() && anchor.equals(leaderRoute.get(0))
              && (waypointFacing != UnitOrders.FACING_AUTO)) {
            return waypointFacing;
        }
        if (leaderRoute.size() > 1) {
            return anchor.direction(leaderRoute.get(1));
        }
        int stoppedFacing = leader.getUnitOrders().getFacingWhenStopped();
        if (stoppedFacing != UnitOrders.FACING_AUTO) {
            return stoppedFacing;
        }
        if (leaderRoute.isEmpty()) {
            return leader.getFacing();
        }
        String key = leader.getId() + "|" + anchor.getBoardNum();
        return finalHeadings.computeIfAbsent(key, ignored -> leader.getPosition().equals(anchor)
              ? leader.getFacing() : leader.getPosition().direction(anchor));
    }

    /**
     * Works out a unit's slot around its leader's waypoint. Each unit heads straight for its own slot, so the shape
     * forms where the leader is going, with the full spacing, rather than trailing the leader's moving hex
     * (HammerGS's playtest, 2026-09-26).
     */
    private @Nullable Coords chooseSlot(Entity entity, Coords anchor, int heading, FormationOrder formation,
          int slotIndex) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return null;
        }
        Coords ideal = FormationPlanner.idealSlot(anchor, heading, formation.getShape(), formation.getSpacing(),
              slotIndex);
        Coords settled = settle(entity, board, anchor, ideal);
        if (settled != null) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_SLOT - {} slot {} at {} (around {}, facing {})",
                  entity.getDisplayName(), entity.getId(), currentRound(), formation.getShape(), slotIndex,
                  settled.getBoardNum(), anchor.getBoardNum(), heading);
            return settled;
        }
        Coords columnSlot = settle(entity, board, anchor, FormationPlanner.idealSlot(anchor, heading,
              FormationShape.COLUMN, formation.getSpacing(), slotIndex));
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_FOLD - {} slot {} blocked, folding to column at {}",
              entity.getDisplayName(), entity.getId(), currentRound(), formation.getShape(), slotIndex,
              (columnSlot == null) ? anchor.getBoardNum() : columnSlot.getBoardNum());
        owner.getOrdersRadio().report(entity, "fold", anchor.getBoardNum());
        return (columnSlot == null) ? anchor : columnSlot;
    }

    /**
     * @return the ideal hex if the unit can stand there, else the best hex within {@link #FORMATION_SLACK} of it,
     *       else {@code null}
     */
    private @Nullable Coords settle(Entity entity, Board board, Coords leaderPosition, Coords ideal) {
        if (isUsableSlot(entity, board, leaderPosition, ideal)) {
            return ideal;
        }
        int wantedDistance = ideal.distance(leaderPosition);
        Coords best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int direction = 0; direction < 6; direction++) {
            Coords candidate = ideal.translated(direction, FORMATION_SLACK);
            if (!isUsableSlot(entity, board, leaderPosition, candidate)) {
                continue;
            }
            int score = Math.abs(candidate.distance(leaderPosition) - wantedDistance);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isUsableSlot(Entity entity, Board board, Coords leaderPosition, Coords slot) {
        return board.contains(slot) && !slot.equals(leaderPosition) && !entity.isLocationProhibited(slot)
              && FormationSide.sameSide(board, leaderPosition, slot) && !isHeldByOutsider(entity, slot);
    }

    /**
     * @return {@code true} if a unit from outside the formation stands on the hex. Formation members are left out:
     *       they are on the move to their own slots.
     */
    private boolean isHeldByOutsider(Entity entity, Coords slot) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        for (Entity occupant : owner.getGame().getEntitiesVector(slot, entity.getBoardId())) {
            Optional<FormationOrder> occupantFormation = occupant.getUnitOrders().getFormation();
            boolean isMember = formation.isPresent() && occupantFormation.isPresent()
                  && occupantFormation.get().sharesLeader(formation.get().getLeaderId());
            if ((occupant.getId() != entity.getId()) && !isMember) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the formation's units still on the board, any owner on the same side, in slot order; the first is the
     *       acting leader, which is the original leader while it lives and the next unit after that
     */
    private List<Entity> formationMembers(Entity entity, int leaderId) {
        List<Entity> members = new ArrayList<>();
        for (Entity candidate : owner.getGame().getEntitiesVector()) {
            Optional<FormationOrder> candidateFormation = candidate.getUnitOrders().getFormation();
            if (candidateFormation.isEmpty() || !candidateFormation.get().sharesLeader(leaderId)) {
                continue;
            }
            if ((candidate.getPosition() == null) || candidate.isDestroyed() || candidate.isDoomed()
                  || candidate.getOwner().isEnemyOf(entity.getOwner())) {
                continue;
            }
            members.add(candidate);
        }
        members.sort(Comparator.comparingInt(member -> member.getUnitOrders().getFormation()
              .map(FormationOrder::getSlot).orElse(Integer.MAX_VALUE)));
        return members;
    }

    /**
     * How close an enemy must come to break a formation: the longest effective range among its units, so a lance of
     * missile boats breaks to fight at long range while a lance of brawlers keeps its shape until the enemy is close.
     */
    private static int contactRange(List<Entity> members) {
        int longestRange = 0;
        for (Entity member : members) {
            longestRange = Math.max(longestRange, SupportEnvelope.of(member).effectiveRange());
        }
        return (longestRange > 0) ? longestRange : FALLBACK_CONTACT_RANGE;
    }

    private boolean isEnemyNear(Entity leader, int contactRange) {
        for (Entity enemy : owner.getEnemyEntities()) {
            if ((enemy.getPosition() != null) && (enemy.getBoardId() == leader.getBoardId())
                  && (enemy.getPosition().distance(leader.getPosition()) <= contactRange)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return how close counts as arrived: 0 for a formation slot and for a formation's leader at its last
     *       waypoint, which must be reached exactly, else {@link Princess#DISTANCE_TO_WAYPOINT}
     */
    int arrivalRadius(Entity entity) {
        boolean holdsExactHex = getFormationSlot(entity).isPresent() || isLeadingFormationToLastWaypoint(entity)
              || isHeadingForHold(entity);
        return holdsExactHex ? 0 : Princess.DISTANCE_TO_WAYPOINT;
    }

    /**
     * A waypoint set to hold must be reached exactly: the hold starts only once the unit stands on it.
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit's next waypoint, part-way along its route, is set to hold
     */
    boolean isHeadingForHold(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        return (orders.getRoute().size() > 1) && orders.getWaypointOrder(0).isHold();
    }

    /**
     * A formation's leader must end on its last waypoint exactly, since the shape is laid out around that hex.
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit leads a formation and is heading for the last hex of its route
     */
    boolean isLeadingFormationToLastWaypoint(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || (entity.getUnitOrders().getRoute().size() != 1)) {
            return false;
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        return (members.size() >= 2) && (members.get(0).getId() == entity.getId());
    }

    /**
     * The hex a formation member should deploy in: its slot beside the formation's leader, once the leader is on the
     * board. The slot comes from the member's place in the formation as set in the lobby, since an undeployed unit
     * has no position yet. The shape faces the leader's ordered facing when stopped, else its first waypoint, else
     * the middle of the enemy's deployment zone. Where the formation's own shape does not fit the deployment zone
     * around the leader, the member takes its place in a Line abreast instead, and the formation forms its shape on
     * the move.
     *
     * @param entity     a unit about to deploy
     * @param legalHexes the hexes the unit may deploy in
     *
     * @return the slot hex, or empty for a unit not in a formation, the leader itself, or a leader not yet deployed
     */
    public Optional<Coords> getDeploymentSlot(Entity entity, List<Coords> legalHexes) {
        Optional<FormationOrder> formation = activeFormation(entity);
        if (formation.isEmpty() || (formation.get().getSlot() == 0)
              || (formation.get().getLeaderId() == entity.getId())) {
            return Optional.empty();
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        if ((leader == null) || !leader.isDeployed() || (leader.getPosition() == null)
              || (leader.getBoardId() != entity.getBoardId())) {
            return Optional.empty();
        }
        Board board = owner.getGame().getBoard(leader);
        if (board == null) {
            return Optional.empty();
        }
        Coords leaderPosition = leader.getPosition();
        int heading = deploymentHeading(leader, leaderPosition, deploymentFacingTarget(board));
        FormationShape shape = fittingDeploymentShape(formation.get(), leaderPosition, heading,
              new HashSet<>(legalHexes), memberSlots(leader.getId())).orElse(formation.get().getShape());
        return Optional.of(FormationPlanner.idealSlot(leaderPosition, heading, shape, formation.get().getSpacing(),
              formation.get().getSlot()));
    }

    /**
     * Puts the legal deployment hexes nearest a formation member's slot first, so the bot deploys the lance in its
     * shape. Hexes outside the deployment zone are never added; a slot outside it just draws the member as close as
     * the zone allows.
     *
     * @param entity               the unit about to deploy
     * @param possibleDeployCoords the legal deployment hexes, in the bot's own order
     *
     * @return the same hexes, nearest the slot first, or unchanged for a unit with no deployment slot
     */
    public List<Coords> preferDeploymentSlot(Entity entity, List<Coords> possibleDeployCoords) {
        Optional<Coords> slot = getDeploymentSlot(entity, possibleDeployCoords);
        if (slot.isEmpty()) {
            return possibleDeployCoords;
        }
        List<Coords> ordered = new ArrayList<>(possibleDeployCoords);
        ordered.sort(Comparator.comparingInt(coords -> coords.distance(slot.get())));
        LOGGER.info("[BotOrders] {} (ID {}): deploying in formation, slot {}, nearest legal hex {}",
              entity.getDisplayName(), entity.getId(), slot.get().getBoardNum(),
              ordered.isEmpty() ? "none" : ordered.get(0).getBoardNum());
        return ordered;
    }

    /**
     * Keeps a formation leader's deployment hexes to those its formation fits around, so the members have room for
     * their slots. A shallow zone along a board edge rarely has room for a Vee or a Wedge, whose arms reach several
     * rows; then the leader takes a hex a Line abreast fits around, and the formation forms its shape on the move.
     * The bot still picks among the fitting hexes by terrain.
     *
     * @param entity               the unit about to deploy
     * @param possibleDeployCoords the legal deployment hexes, in the bot's own order
     *
     * @return the hexes the formation fits around, in the same order; unchanged for a unit that leads no formation,
     *       or when the formation fits nowhere
     */
    public List<Coords> preferFormationFit(Entity entity, List<Coords> possibleDeployCoords) {
        Optional<FormationOrder> formation = activeFormation(entity);
        if (formation.isEmpty() || (formation.get().getLeaderId() != entity.getId())) {
            return possibleDeployCoords;
        }
        List<Integer> slots = memberSlots(entity.getId());
        Board board = owner.getGame().getBoard(entity);
        if (slots.isEmpty() || (board == null)) {
            return possibleDeployCoords;
        }
        Set<Coords> legalHexes = new HashSet<>(possibleDeployCoords);
        Coords facingTarget = deploymentFacingTarget(board);
        for (FormationShape shape : List.of(formation.get().getShape(), FormationShape.LINE)) {
            List<Coords> fitting = new ArrayList<>();
            for (Coords candidate : possibleDeployCoords) {
                if (fits(shape, formation.get(), candidate, deploymentHeading(entity, candidate, facingTarget),
                      legalHexes, slots)) {
                    fitting.add(candidate);
                }
            }
            if (!fitting.isEmpty()) {
                LOGGER.info("[BotOrders] {} (ID {}): deploying to lead a {} ({} slots) - {} of {} legal hexes fit it{}",
                      entity.getDisplayName(), entity.getId(), shape, slots.size(), fitting.size(),
                      possibleDeployCoords.size(), (shape == formation.get().getShape()) ? ""
                            : "; the zone is too shallow for a " + formation.get().getShape()
                                  + ", so the formation forms it on the move");
                return fitting;
            }
        }
        LOGGER.info("[BotOrders] {} (ID {}): no legal hex fits a {} or a Line; deploying on terrain alone",
              entity.getDisplayName(), entity.getId(), formation.get().getShape());
        return possibleDeployCoords;
    }

    /**
     * The way a formation faces while it deploys: the leader's ordered facing when stopped, as set in the lobby; else
     * toward its first waypoint; else toward the facing target - the middle of the enemy's deployment zone, the way
     * the bot faces the units it deploys.
     */
    private static int deploymentHeading(Entity leader, Coords leaderPosition, Coords facingTarget) {
        int orderedFacing = leader.getUnitOrders().getFacingWhenStopped();
        if (orderedFacing != UnitOrders.FACING_AUTO) {
            return orderedFacing;
        }
        Optional<Coords> waypoint = leader.getUnitOrders().getNextWaypoint();
        if (waypoint.isPresent() && !waypoint.get().equals(leaderPosition)) {
            return leaderPosition.direction(waypoint.get());
        }
        return facingTarget.equals(leaderPosition) ? leader.getFacing() : leaderPosition.direction(facingTarget);
    }

    /**
     * @return the middle of the enemy's deployment zone, or the middle of the board when no enemy zone is known
     */
    private Coords deploymentFacingTarget(Board board) {
        return owner.getEnemyDeploymentCenter(board)
              .orElse(new Coords(board.getWidth() / 2, board.getHeight() / 2));
    }

    /**
     * @return the formation's own shape if every member's slot around the leader hex is a legal deployment hex, else
     *       a Line if that fits, else empty
     */
    private static Optional<FormationShape> fittingDeploymentShape(FormationOrder formation, Coords leaderPosition,
          int heading, Set<Coords> legalHexes, List<Integer> slots) {
        for (FormationShape shape : List.of(formation.getShape(), FormationShape.LINE)) {
            if (fits(shape, formation, leaderPosition, heading, legalHexes, slots)) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }

    private static boolean fits(FormationShape shape, FormationOrder formation, Coords leaderPosition, int heading,
          Set<Coords> legalHexes, List<Integer> slots) {
        for (int slot : slots) {
            Coords slotHex = FormationPlanner.idealSlot(leaderPosition, heading, shape, formation.getSpacing(), slot);
            if (!legalHexes.contains(slotHex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the slots of a formation's units other than its leader, deployed or not
     */
    private List<Integer> memberSlots(int leaderId) {
        List<Integer> slots = new ArrayList<>();
        for (Entity unit : owner.getGame().getEntitiesVector()) {
            Optional<FormationOrder> unitFormation = unit.getUnitOrders().getFormation();
            if (unitFormation.isPresent() && unitFormation.get().sharesLeader(leaderId)
                  && (unitFormation.get().getSlot() != 0)) {
                slots.add(unitFormation.get().getSlot());
            }
        }
        return slots;
    }

    /**
     * Picks which unit to deploy this turn so a formation's leader goes down before its members, which then deploy
     * in their slots around it. A member due to deploy is swapped for its leader when the leader may deploy this
     * turn too.
     *
     * @param firstDeployable the unit the game would deploy next
     * @param turn            the bot's deployment turn
     *
     * @return the unit to deploy
     */
    public int chooseUnitToDeploy(int firstDeployable, GameTurn turn) {
        Entity unit = owner.getGame().getEntity(firstDeployable);
        if ((unit == null) || (turn == null)) {
            return firstDeployable;
        }
        Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
        if (formation.isEmpty() || (formation.get().getLeaderId() == unit.getId())) {
            return firstDeployable;
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        if ((leader != null) && !leader.isDeployed() && turn.isValidEntity(leader, owner.getGame())
              && leader.shouldDeploy(owner.getGame().getRoundCount())) {
            LOGGER.info("[BotOrders] deploying formation leader {} (ID {}) before {} (ID {})",
                  leader.getDisplayName(), leader.getId(), unit.getDisplayName(), unit.getId());
            return leader.getId();
        }
        return firstDeployable;
    }

    /**
     * Keeps each unit in a formation to the formation's pace, which sets how its units move, not how fast: at a Walk
     * pace every unit, the leader included, may use up to its own walking movement points, and at a Run pace up to
     * its own running movement points. A jump within those movement points is allowed. A formation that has broken on
     * contact is not paced, and a unit with no move within the pace keeps every move.
     *
     * <p>A formation that keeps together also holds its leader to the slowest unit's speed, so the others can keep
     * their slots; without it, a Grasshopper leading a Longbow reached each waypoint in a turn or two and the Longbow,
     * chasing slots around ever-further waypoints, fell hopelessly behind (HammerGS's playtest, 2026-09-26). A
     * formation that does not keep together lets each unit move at its own speed, so units form up sooner.</p>
     *
     * @param entity the unit about to move
     * @param paths  its candidate moves
     *
     * @return the moves within the formation's pace, or all of them when the unit is in no formation
     */
    List<MovePath> limitToFormationPace(Entity entity, List<MovePath> paths) {
        Optional<FormationOrder> formation = activeFormation(entity);
        if (formation.isEmpty()) {
            return paths;
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        if (members.size() < 2) {
            return paths;
        }
        Entity leader = members.get(0);
        if ((formation.get().getContactRule() == ContactRule.BREAK) && (leader.getPosition() != null)
              && isEnemyNear(leader, contactRange(members))) {
            return paths;
        }
        int paceLimit = paceMovementPoints(entity, formation.get().getPace());
        boolean isHeldToSlowest = formation.get().isKeepTogether() && (leader.getId() == entity.getId());
        if (isHeldToSlowest) {
            // a formation keeping together advances no faster than its slowest unit can follow
            for (Entity member : members) {
                paceLimit = Math.min(paceLimit, paceMovementPoints(member, formation.get().getPace()));
            }
        }
        List<MovePath> pacedPaths = new ArrayList<>();
        for (MovePath path : paths) {
            if (path.getMpUsed() <= paceLimit) {
                pacedPaths.add(path);
            }
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_PACE - {} up to {} {} MP, {} of {} moves kept",
              entity.getDisplayName(), entity.getId(), currentRound(), formation.get().getPace(),
              isHeldToSlowest ? "the slowest unit's" : "its own", paceLimit, pacedPaths.size(), paths.size());
        return pacedPaths.isEmpty() ? paths : pacedPaths;
    }

    private static int paceMovementPoints(Entity unit, FormationPace pace) {
        return (pace == FormationPace.RUN) ? unit.getRunMP() : unit.getWalkMP();
    }

    /**
     * Keeps followers' routes in step with their leader's: when the leader moves on to its next waypoint, so do they,
     * since they reach the waypoints beside the leader rather than on them.
     */
    private void syncFollowerRoutes() {
        for (Entity entity : owner.getEntitiesOwned()) {
            Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
            if (formation.isEmpty() || (formation.get().getLeaderId() == entity.getId())) {
                continue;
            }
            List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
            if (members.isEmpty() || (members.get(0).getId() == entity.getId())) {
                continue;
            }
            List<Coords> leaderRoute = members.get(0).getUnitOrders().getRoute();
            List<Coords> ownRoute = entity.getUnitOrders().getRoute();
            int stepsBehind = ownRoute.size() - leaderRoute.size();
            if ((stepsBehind > 0) && ownRoute.subList(stepsBehind, ownRoute.size()).equals(leaderRoute)) {
                for (int step = 0; step < stepsBehind; step++) {
                    change(entity, UnitOrderAction.REACHED);
                }
                LOGGER.info("[BotOrders] {} (ID {}) keeps pace with its leader's route", entity.getDisplayName(),
                      entity.getId());
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
        owner.getOrdersRadio().report(entity, "unreachable", waypoint.get().getBoardNum());
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
