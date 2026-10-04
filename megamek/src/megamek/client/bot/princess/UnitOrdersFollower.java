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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import megamek.client.bot.Messages;
import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.force.Force;
import megamek.common.game.GameTurn;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;
import megamek.common.orders.NavPoint;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.PhaseLine;
import megamek.common.orders.RouteStyle;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
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

    // a planned turning point counts as passed this near: the unit makes for the next one, it need not stand on it
    private static final int TURN_POINT_RADIUS = 2;

    private final Princess owner;
    private final Set<Integer> arrivedUnitIds = new HashSet<>();

    /** Each unit's route as last seen, by unit id, to tell a new order from waypoints ticked off; not saved. */
    private final Map<Integer, List<Coords>> knownRoutes = new HashMap<>();

    // what the bot knows of each convoy: its units, front, heading and exit; the one source for convoys and escorts
    private final ConvoyTracker convoys;
    // the waypoint each unit last planned its way to, so a leg is planned once
    private final Map<Integer, Coords> legsPlannedTo = new HashMap<>();
    // units out of formation because they are leaving the board, so it is logged once
    private final Set<Integer> releasedToLeave = new HashSet<>();
    // true while one unit's moves are ranked (see setRanking)
    private boolean isRanking;
    // each lance's role by owner and force, worked out once a turn (see roleOf)
    private final Map<String, LanceRole> lanceRoles = new HashMap<>();
    private String lanceRolesMoment = "";

    // who was hit, and lances breaking off their route to fight and holding after it
    private final FireReaction fireReaction;

    // which way an ordered unit faces, and when an ordered facing gives way to a threat
    private final OrderedFacing facings;

    // how far a hex is from a waypoint or edge by the real route, the route fields shared a round
    private final RouteDistances distances;

    // convoys leaving by their edge, and escorts keeping their places round them
    private final ConvoyEscortFollower convoyEscorts;

    // where ordered units deploy: reachable, with their lance, in formation
    private final DeploymentPlanner deployment;

    // lances holding at a phase line until every lance on it is in
    private final PhaseLineCoordinator phaseLines;

    // who is in which formation, who leads it, and who is out of action or falling behind
    private final FormationRoster roster;

    // each formation's leg to its next flag: the places at the flag, and town legs
    private final FormationLegs townLegs;

    // each formation unit's slot round its leader's flag, and the way the formation faces
    private final FormationSlots slots;

    // formations forming up, waiting at flags and holds, keeping pace, and leaving together
    private final FormationMarch march;

    /**
     * @param owner the bot whose units follow orders
     */
    UnitOrdersFollower(Princess owner) {
        this.owner = owner;
        this.convoys = new ConvoyTracker(owner);
        this.fireReaction = new FireReaction(owner, this);
        this.facings = new OrderedFacing(owner, this);
        this.distances = new RouteDistances(owner, this);
        this.convoyEscorts = new ConvoyEscortFollower(owner, this);
        this.deployment = new DeploymentPlanner(owner, this);
        this.phaseLines = new PhaseLineCoordinator(owner, this);
        this.roster = new FormationRoster(owner, this);
        this.townLegs = new FormationLegs(owner, this);
        this.slots = new FormationSlots(owner, this);
        this.march = new FormationMarch(owner, this);
    }

    /**
     * @return who was hit, and lances breaking off to fight
     */
    FireReaction fireReaction() {
        return fireReaction;
    }

    /**
     * @return how far hexes are from waypoints and edges by the real route
     */
    RouteDistances distances() {
        return distances;
    }

    /**
     * @return what the bot knows of each convoy
     */
    ConvoyTracker convoys() {
        return convoys;
    }

    /**
     * @return who is in which formation, and who is out of action or falling behind
     */
    FormationRoster roster() {
        return roster;
    }

    /**
     * @return each formation's leg to its next flag, and town legs
     */
    FormationLegs townLegs() {
        return townLegs;
    }

    /**
     * @return each formation unit's slot
     */
    FormationSlots slots() {
        return slots;
    }

    /**
     * @return convoys leaving by their edge, and escorts round them
     */
    ConvoyEscortFollower convoyEscorts() {
        return convoyEscorts;
    }

    /**
     * Starts or ends the window in which one unit's moves are ranked. Nobody moves while it is open, so the formation
     * members and slots are worked out once and kept, rather than for every move the path ranker scores.
     *
     * @param isRanking {@code true} as the unit's moves are about to be ranked, {@code false} once they are
     */
    void setRanking(boolean isRanking) {
        this.isRanking = isRanking;
        roster.forgetRankingAnswers();
        slots.forgetRankingAnswers();
    }

    /**
     * @return {@code true} while one unit's moves are being ranked
     */
    boolean isRanking() {
        return isRanking;
    }

    /**
     * @return where ordered units deploy
     */
    DeploymentPlanner deployment() {
        return deployment;
    }

    /**
     * @return the game's current round
     */
    int currentRound() {
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
              || march.isWaitingForFormation(entity) || phaseLines.isWaitingAtPhaseLine(entity)
              || isHoldingRouteEnd(entity) || convoyEscorts.isConvoyWaitingForOrders(entity);
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
        Optional<Entity> leader = roster.formationLeaderOf(entity);
        if (leader.isEmpty() || !leader.get().getUnitOrders().isHoldingAtWaypoint(round)
              || (entity.getPosition() == null)) {
            return false;
        }
        Optional<Coords> slot = slots.getFormationSlot(entity);
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
        // a lance waiting at a flag for its formation to re-form stops there too, and should face on toward the next
        // flag rather than the way it came in (HammerGS's playtest, 2026-09-27: backs turned to the next flag)
        boolean isWaitingAtFlag = march.isWaitingForFormation(roster.formationLeaderOf(entity).orElse(entity));
        boolean isStoppedAtWaypoint = orders.hasRoute()
              && ((orders.getRoute().size() == 1) || isHoldingAtWaypoint(entity) || isWaitingAtFlag);
        if (isStoppedAtWaypoint) {
            // a formation unit holding with its leader faces the way set on the leader's waypoint
            Entity waypointOwner = roster.formationLeaderOf(entity).orElse(entity);
            UnitOrders waypointOrders = waypointOwner.getUnitOrders();
            int waypointFacing = waypointOrders.getWaypointOrder(0).getFacing();
            if (waypointFacing != UnitOrders.FACING_AUTO) {
                return waypointFacing;
            }
            // a hold part-way along the route faces on toward the next flag, unless the player set a facing
            boolean isPartWay = waypointOrders.getRoute().size() > 1;
            if (isPartWay && (orders.getFacingWhenStopped() == UnitOrders.FACING_AUTO)
                  && (entity.getPosition() != null) && !entity.getPosition().equals(waypointOrders.getRoute().get(1))) {
                return entity.getPosition().direction(waypointOrders.getRoute().get(1));
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
        Optional<Entity> leader = roster.formationLeaderOf(entity);
        if (leader.isPresent()) {
            // a formation unit's route ends in its slot beside the leader, once the leader has arrived; checking the
            // waypoint itself stopped the whole formation wherever it stood when the waypoint came within reach
            Optional<Coords> slot = slots.getFormationSlot(entity);
            // exactly on the slot: a unit one hex off counted as arrived and held there, leaving the column
            // ragged (HammerGS's playtest, 2026-09-26); a blocked slot has already moved to a free hex beside it
            return isAtRouteEnd(leader.get()) && slot.isPresent() && entity.getPosition().equals(slot.get());
        }
        int arrivalRadius = isLeadingFormationToLastWaypoint(entity) ? 0 : Princess.DISTANCE_TO_WAYPOINT;
        return entity.getPosition().distance(route.get(0)) <= arrivalRadius;
    }

    /**
     * The formation a unit travels in on its current leg: the one set on its formation leader's next waypoint, else
     * its own formation order. A leg set to travel out of formation has none, nor has a unit leaving the board.
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
        Optional<CardinalEdge> leavingBy = getOrderedEdge(entity);
        if (leavingBy.isPresent()) {
            // leaving the board comes first: a unit on its way off waits for nobody's pace or place, and an escort
            // following its convoy off is no longer escorting (HammerGS, 2026-10-04)
            if (releasedToLeave.add(entity.getId())) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_RELEASED - leaving by the {} edge, out of "
                      + "formation", entity.getDisplayName(), entity.getId(), currentRound(), leavingBy.get());
            }
            return Optional.empty();
        }
        releasedToLeave.remove(entity.getId());
        // the leader's route sets the leg; the unit's own route stands in step for it once the leader is gone
        Entity leader = owner.getGame().getEntity(base.get().getLeaderId());
        boolean isLeaderRouted = (leader != null) && !leader.isDestroyed() && leader.getUnitOrders().hasRoute();
        UnitOrders legOrders = isLeaderRouted ? leader.getUnitOrders() : entity.getUnitOrders();
        // a waypoint set to change shape there: once the leader has reached it, the units re-form in the new shape
        // around it before moving on, having kept the old shape on the way (HammerGS, 2026-09-27)
        Entity legLeader = isLeaderRouted ? leader : entity;
        WaypointFormation arrival = legOrders.hasRoute() ? legOrders.getWaypointOrder(0).getArrivalFormation() : null;
        if ((arrival != null) && (legLeader.getPosition() != null)
              && (legLeader.getPosition().distance(legOrders.getRoute().get(0)) <= Princess.DISTANCE_TO_WAYPOINT)) {
            return arrival.isNone() ? Optional.empty() : Optional.of(arrival.applyTo(base.get()));
        }
        WaypointFormation leg = legOrders.hasRoute() ? legOrders.getWaypointOrder(0).getFormation() : null;
        if (leg == null) {
            return base;
        }
        return leg.isNone() ? Optional.empty() : Optional.of(leg.applyTo(base.get()));
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
     * @return {@code true} if the unit is in a convoy lance
     */
    boolean isConvoy(Entity entity) {
        LanceRole role = roleOf(entity);
        return (role != null) && role.isConvoy();
    }

    /**
     * The role of a unit's lance, as {@link LanceRoles#effectiveRole} gives it, from a table worked out once a turn:
     * the path ranker asks for every move it scores, and each answer went through every unit in the game.
     *
     * @param entity a unit
     *
     * @return the role, or {@code null} for none
     */
    @Nullable LanceRole roleOf(Entity entity) {
        if ((entity.getLanceRole() != null) || (entity.getForceId() == Force.NO_FORCE)) {
            return entity.getLanceRole();
        }
        // a turn is one unit's move or deployment: the table holds while that unit's moves are scored
        String moment = currentRound() + ":" + owner.getGame().getPhase() + ":" + owner.getGame().getTurnIndex();
        if (!moment.equals(lanceRolesMoment)) {
            lanceRoles.clear();
            for (Entity unit : owner.getGame().getEntitiesVector()) {
                if ((unit.getLanceRole() != null) && (unit.getForceId() != Force.NO_FORCE)) {
                    lanceRoles.putIfAbsent(lanceKey(unit), unit.getLanceRole());
                }
            }
            lanceRolesMoment = moment;
        }
        return lanceRoles.get(lanceKey(entity));
    }

    private static String lanceKey(Entity unit) {
        return unit.getOwnerId() + ":" + unit.getForceId();
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
    void orderExit(Entity entity, OffBoardDirection direction) {
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
                // only the exit goes: the unit keeps its formation, facings and priority
                change(entity, UnitOrderAction.EDGE_OFF);
            }
        }
        LOGGER.info("[BotOrders] {}: flee order cancelled - exit orders called off", owner.getName());
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
            // an escort set to Follow leaves by the edge its convoy left by
            return convoyEscorts.escortExitEdge(entity).map(UnitOrdersFollower::toCardinalEdge);
        }
        return Optional.of(toCardinalEdge(orders.getEdge()));
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is ordered to leave the board by an edge
     */
    public boolean isOrderedToExit(Entity entity) {
        return (entity.getUnitOrders().getEdgeOrder() == EdgeOrder.EXIT_BY)
              || ((entity.getUnitOrders().getEdgeOrder() == EdgeOrder.NONE)
              && convoyEscorts.escortExitEdge(entity).isPresent());
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
     * @param entity a unit of the bot
     *
     * @return how strongly the unit's route pulls in its path score: {@link #IMPERATIVE_ROUTE_WEIGHT} for an
     *       Imperative route, 1 otherwise
     */
    double routeWeight(Entity entity) {
        return (entity.getUnitOrders().getPriority() == OrderPriority.IMPERATIVE) ? IMPERATIVE_ROUTE_WEIGHT : 1.0;
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
        fireReaction.reactToFire();
        roster.trackFormationUnits();
        for (Entity entity : owner.getEntitiesOwned()) {
            if (entity.getPosition() == null) {
                continue;
            }
            if (entity.getUnitOrders().getFightState().isPresent()) {
                // fighting, or holding after the fight for the Resume order: the route waits
                continue;
            }
            fireReaction.resumeAfterFight(entity);
            convoyEscorts.routeConvoyOut(entity);
            Optional<Coords> waypoint = entity.getUnitOrders().getNextWaypoint();
            if (waypoint.isEmpty()) {
                knownRoutes.remove(entity.getId());
                continue;
            }
            announceNewRoute(entity);
            if (planRouteLeg(entity)) {
                waypoint = entity.getUnitOrders().getNextWaypoint();
            }
            boolean isPartWay = entity.getUnitOrders().getRoute().size() > 1;
            if (isPartWay && slots.getFormationSlot(entity).isPresent()) {
                // a unit in formation takes its route from its leader's; ticking off a waypoint it merely passed
                // near sent it on toward the next one, ahead of the formation (HammerGS's playtest, 2026-09-26)
                continue;
            }
            if (isPartWay && phaseLines.isWaitingAtPhaseLine(entity)) {
                // at a phase line: the route waits until every lance on the line is in
                continue;
            }
            if (isPartWay && entity.getUnitOrders().getWaypointOrder(0).isHold()) {
                march.advanceHold(entity, waypoint.get());
                continue;
            }
            boolean isPlannedTurn = entity.getUnitOrders().getWaypointOrder(0).isPlannedTurn();
            int reachedWithin = isPlannedTurn ? TURN_POINT_RADIUS
                  : (isLeadingFormationOnRoute(entity) ? flagRadius(entity, waypoint.get())
                        : Princess.DISTANCE_TO_WAYPOINT);
            if (waypoint.get().distance(entity.getPosition()) > reachedWithin) {
                if (march.endReformWait(entity)) {
                    // a leader that began waiting beside an occupied flag must not go on waiting once the flag is
                    // clear: the wait held it in place, so it never stepped onto the flag, and nothing looked at the
                    // wait again (HammerGS's playtest, 2026-09-27: the lance formed a Line round 1223 and never left)
                    LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_WAIT at {} ends - the flag is clear; "
                                + "stepping onto it", entity.getDisplayName(), entity.getId(), currentRound(),
                          waypoint.get().getBoardNum());
                }
                continue;
            }
            // a planned turning point is passed straight through, but for the first, where a lance not yet formed up
            // assembles like at any first waypoint: passing it, assembly never ended and the column raced flag to flag
            // (HammerGS's playtest, 2026-10-03)
            boolean isPassedThrough = isPlannedTurn && !march.isAssembling(entity);
            if (isPartWay && !isPassedThrough && march.shouldWaitForFormation(entity, waypoint.get())) {
                continue;
            }
            if (isPartWay) {
                LOGGER.info("[BotOrders] {} (ID {}) reached waypoint {}", entity.getDisplayName(), entity.getId(),
                      waypoint.get().getBoardNum());
                change(entity, UnitOrderAction.REACHED);
            } else if (isAtRouteEnd(entity) && entity.getUnitOrders().getWaypointOrder(0).isExitBoard()
                  && !roster.isFormationFollower(entity)) {
                // the route ends by leaving the board: a formation keeping together first assembles, then leaves as one
                if (!march.shouldWaitForFormation(entity, waypoint.get())) {
                    march.exitWithFormation(entity, waypoint.get());
                }
            } else if (isAtRouteEnd(entity) && arrivedUnitIds.add(entity.getId())) {
                // the last hex stays in the route: the unit holds it and comes back to it after a fight
                String arrivalHex = entity.getPosition().getBoardNum();
                LOGGER.info("[BotOrders] {} (ID {}) reached the end of its route at {}", entity.getDisplayName(),
                      entity.getId(), arrivalHex);
                owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.ARRIVED,
                      navLabel(entity, waypoint.get()));
            }
        }
        syncFollowerRoutes();
    }

    /**
     * Plans the ways the bot's units take this movement phase, before any of them moves: a convoy whose route is done
     * is sent off by its exit edge, and a leg set to be planned gets its turning points. Done only once the units had
     * moved, round 1 - and the round after any new order - was driven without a plan.
     */
    void planRoutes() {
        for (Entity entity : owner.getEntitiesOwned()) {
            if ((entity.getPosition() == null) || entity.getUnitOrders().getFightState().isPresent()) {
                continue;
            }
            convoyEscorts.routeConvoyOut(entity);
            if (entity.getUnitOrders().getNextWaypoint().isEmpty()) {
                continue;
            }
            // a new order is called, and the lance set to assemble, before its planned turning points go on it
            announceNewRoute(entity);
            planRouteLeg(entity);
        }
    }

    /**
     * Plans the way to the unit's next waypoint when that waypoint is set to be planned: works out the whole way once,
     * and puts the turning points on it on the route as waypoints, so the unit drives straight from one to the next
     * instead of along whatever hill is in front of it (HammerGS, 2026-10-03). Planned once a leg; a new order plans
     * again. Only a formation's leader, or a unit in none, plans: the formation follows it.
     *
     * @param entity a unit of the bot with a route
     *
     * @return {@code true} if turning points were put on the route
     */
    private boolean planRouteLeg(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        if (!orders.hasRoute() || roster.isFormationFollower(entity) || (entity.getPosition() == null)
              || DeploymentPlanner.isFlying(entity)
              || (orders.getWaypointOrder(0).getRoutePlan() != WaypointOrder.RoutePlan.PLAN_LEG)) {
            return false;
        }
        if (orders.isPaused() || orders.isStoppedInRound(currentRound())) {
            // a new route sent to the server clears the pause: planning waits until the player lets the lance go on
            LOGGER.debug("[BotOrders] ROUTE_PLAN {} (ID {}) round {}: paused or stopped; the leg is planned once it "
                  + "moves on", entity.getDisplayName(), entity.getId(), currentRound());
            return false;
        }
        Coords target = orders.getRoute().get(0);
        if (target.equals(legsPlannedTo.get(entity.getId()))) {
            return false;
        }
        legsPlannedTo.put(entity.getId(), target);
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return false;
        }
        RouteStyle style = orders.getWaypointOrder(0).getRouteStyle();
        List<Coords> planned = RoutePlanner.plan(entity, entity.getPosition(), target, style);
        List<Coords> turns = planned.subList(0, planned.size() - 1);
        if (turns.isEmpty()) {
            LOGGER.info("[BotOrders] ROUTE_PLAN {} (ID {}) round {}: the way to {} is a straight line",
                  entity.getDisplayName(), entity.getId(), currentRound(), target.getBoardNum());
            return false;
        }
        List<Coords> hexes = new ArrayList<>(turns);
        hexes.addAll(orders.getRoute());
        // the turning points travel the leg's own formation, passed straight through
        WaypointOrder turnOrder = new WaypointOrder(UnitOrders.FACING_AUTO, WaypointOrder.HoldMode.PASS, 0,
              orders.getWaypointOrder(0).getFormation(), false).withRoutePlan(WaypointOrder.RoutePlan.TURN_POINT);
        List<WaypointOrder> waypointOrders = new ArrayList<>();
        for (int index = 0; index < turns.size(); index++) {
            waypointOrders.add(turnOrder);
        }
        waypointOrders.addAll(orders.getWaypointOrders());
        entity.setUnitOrders(UnitOrderAction.ROUTE.apply(orders, hexes, waypointOrders, OffBoardDirection.NONE,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, currentRound(), null));
        owner.sendChat(UnitOrderCommand.commandText(entity.getId(), UnitOrderAction.ROUTE,
              UnitOrderCommand.hexesArgument(hexes, waypointOrders)));
        // the planned route is the same order: the lance does not assemble again for it
        knownRoutes.put(entity.getId(), entity.getUnitOrders().getRoute());
        List<String> turnNames = new ArrayList<>();
        for (Coords turn : turns) {
            turnNames.add(turn.getBoardNum());
        }
        LOGGER.info("[BotOrders] ROUTE_PLAN {} (ID {}) round {}: planned the {} way to {} - turning at {}",
              entity.getDisplayName(), entity.getId(), currentRound(), style, target.getBoardNum(),
              String.join(", ", turnNames));
        return true;
    }

    /**
     * Calls a new route on the radio: "Charlie Lance, proceeding to Nav Point Alpha (1625)." A route that is the one
     * seen before with waypoints ticked off the front is not new. The formation's leader calls for the lance.
     */
    private void announceNewRoute(Entity entity) {
        List<Coords> route = entity.getUnitOrders().getRoute();
        List<Coords> known = knownRoutes.put(entity.getId(), route);
        boolean isSameOrder = (known != null) && (route.size() <= known.size())
              && known.subList(known.size() - route.size(), known.size()).equals(route);
        if (isSameOrder || roster.isFormationFollower(entity)) {
            return;
        }
        legsPlannedTo.remove(entity.getId());
        if (known != null) {
            // a changed order: the lance assembles again at its new first waypoint before it marches. Every lance
            // starts out assembling, so the first order needs nothing here
            march.assembleAgain(entity);
        }
        String firstNavPoint = navLabel(entity, route.get(0));
        LOGGER.info("[BotOrders] {} (ID {}) round {}: NEW_ROUTE - {} waypoint(s), first {}", entity.getDisplayName(),
              entity.getId(), currentRound(), route.size(), firstNavPoint);
        owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.ORDERED, firstNavPoint);
    }

    /**
     * @param entity a unit of the bot
     * @param hex    a hex on its route
     *
     * @return the hex as the radio names it: its nav point and hex, e.g. {@code Nav Point Gamma (1617)}; the hex
     *       alone for a waypoint with no nav point name, or a hex not on the route
     */
    String navLabel(Entity entity, Coords hex) {
        List<Coords> route = entity.getUnitOrders().getRoute();
        for (int index = 0; index < route.size(); index++) {
            if (route.get(index).equals(hex)) {
                int navNumber = entity.getUnitOrders().getWaypointOrder(index).getNavNumber();
                if (navNumber != NavPoint.UNNAMED) {
                    return Messages.getString("Princess.radio.navPoint", NavPoint.name(navNumber), hex.getBoardNum());
                }
                break;
            }
        }
        return hex.getBoardNum();
    }

    /**
     * @param entity a unit of the bot
     *
     * @return how close counts as arrived: 0 for a formation slot and a waypoint set to hold, which must be reached
     *       exactly; for a formation's leader, its flag (see {@link #flagRadius}); else
     *       {@link Princess#DISTANCE_TO_WAYPOINT}
     */
    int arrivalRadius(Entity entity) {
        if (slots.getFormationSlot(entity).isPresent() || isHeadingForHold(entity)) {
            return 0;
        }
        if (entity.getUnitOrders().hasRoute() && entity.getUnitOrders().getWaypointOrder(0).isPlannedTurn()) {
            return TURN_POINT_RADIUS;
        }
        if (convoyEscorts.isEscorting(entity)) {
            // a hex either side of its place keeps an escort in its band without dancing for the exact hex
            return 1;
        }
        Optional<Coords> waypoint = entity.getUnitOrders().getNextWaypoint();
        if (waypoint.isPresent() && isLeadingFormationOnRoute(entity)) {
            return flagRadius(entity, waypoint.get());
        }
        return Princess.DISTANCE_TO_WAYPOINT;
    }

    /**
     * A formation's leader walks onto every flag of its route, so the lance traces the route and a Column's head is
     * on the flag; cutting a corner by the usual three hexes left the column's commander off the route (HammerGS's
     * playtest, 2026-09-27).
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit leads a formation of two or more units along a route
     */
    boolean isLeadingFormationOnRoute(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || !entity.getUnitOrders().hasRoute()) {
            return false;
        }
        List<Entity> members = roster.formationMembers(entity, formation.get().getLeaderId());
        return (members.size() >= 2) && (members.get(0).getId() == entity.getId());
    }

    /**
     * @return how near a leader must come to its flag to have reached it: on it, or next to it when another unit
     *       stands there
     */
    int flagRadius(Entity leader, Coords flag) {
        for (Entity occupant : owner.getGame().getEntitiesVector(flag, leader.getBoardId())) {
            if (occupant.getId() != leader.getId()) {
                return 1;
            }
        }
        return 0;
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
        List<Entity> members = roster.formationMembers(entity, formation.get().getLeaderId());
        return (members.size() >= 2) && (members.get(0).getId() == entity.getId());
    }

    /**
     * @param entity a unit holding in place under its orders
     *
     * @return why it holds, for the log: paused, stopped, holding at a waypoint or holding the end of its route
     */
    String holdReason(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        if (orders.isPaused()) {
            return "paused";
        }
        if (orders.isStoppedInRound(currentRound())) {
            return "stopped this round";
        }
        if (isHoldingAtWaypoint(entity)) {
            return "holding at waypoint " + entity.getPosition().getBoardNum();
        }
        if (march.isWaitingForFormation(entity)) {
            return "waiting at " + entity.getPosition().getBoardNum() + " for the formation";
        }
        if (phaseLines.isWaitingAtPhaseLine(entity)) {
            return "holding at " + PhaseLine.display(entity.getUnitOrders().getWaypointOrder(0).getPhaseLine())
                  + " for the other lances on it";
        }
        return "holding the end of its route at " + entity.getPosition().getBoardNum();
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
            List<Entity> members = roster.formationMembers(entity, formation.get().getLeaderId());
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
        owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.UNREACHABLE, navLabel(entity, waypoint.get()));
        change(entity, UnitOrderAction.SKIP);
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

    void change(Entity entity, UnitOrderAction action, List<Coords> hexes) {
        UnitOrders newOrders = action.apply(entity.getUnitOrders(), hexes, OffBoardDirection.NONE,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, currentRound());
        entity.setUnitOrders(newOrders);
        String command = hexes.isEmpty()
              ? UnitOrderCommand.commandText(entity.getId(), action)
              : UnitOrderCommand.commandText(entity.getId(), action, UnitOrderCommand.hexesArgument(hexes));
        owner.sendChat(command);
    }

    /**
     * See {@code OrderedFacing.facingThatStandsFor}.
     */
    public int facingThatStandsFor(Entity entity, int orderedFacing, Coords position, @Nullable Coords threat) {
        return facings.facingThatStandsFor(entity, orderedFacing, position, threat);
    }

    /**
     * See {@code OrderedFacing.orderedFacing}.
     */
    int orderedFacing(Entity entity, Coords finalHex) {
        return facings.orderedFacing(entity, finalHex);
    }

    /**
     * See {@code OrderedFacing.orderedTwist}.
     */
    public int orderedTwist(Entity entity) {
        return facings.orderedTwist(entity);
    }

    /**
     * See {@code OrderedFacing.twistAllowance}.
     */
    int twistAllowance(Entity entity, Coords finalHex) {
        return facings.twistAllowance(entity, finalHex);
    }

    /**
     * See {@code RouteDistances.canReach}.
     */
    boolean canReach(Entity entity, Coords waypoint) {
        return distances.canReach(entity, waypoint);
    }

    /**
     * See {@code RouteDistances.edgeCostFrom}.
     */
    int edgeCostFrom(Entity mover, CardinalEdge edge, Coords position) {
        return distances.edgeCostFrom(mover, edge, position);
    }

    /**
     * See {@code RouteDistances.hasWalkingRoute}.
     */
    boolean hasWalkingRoute(Entity mover) {
        return distances.hasWalkingRoute(mover);
    }

    /**
     * See {@code RouteDistances.routeCostFrom}.
     */
    int routeCostFrom(Entity mover, Coords waypoint, Coords position) {
        return distances.routeCostFrom(mover, waypoint, position);
    }


    /**
     * See {@code ConvoyEscortFollower.getEscortDeploymentPlace}.
     */
    public Optional<Coords> getEscortDeploymentPlace(Entity entity) {
        return convoyEscorts.getEscortDeploymentPlace(entity);
    }

    /**
     * See {@code ConvoyEscortFollower.getEscortPlace}.
     */
    public Optional<Coords> getEscortPlace(Entity entity) {
        return convoyEscorts.getEscortPlace(entity);
    }


    /**
     * See {@code DeploymentPlanner.chooseUnitToDeploy}.
     */
    public int chooseUnitToDeploy(int firstDeployable, @Nullable GameTurn turn) {
        return deployment.chooseUnitToDeploy(firstDeployable, turn);
    }

    /**
     * See {@code DeploymentPlanner.getDeploymentAnchor}.
     */
    public Optional<Coords> getDeploymentAnchor(Entity entity) {
        return deployment.getDeploymentAnchor(entity);
    }

    /**
     * See {@code DeploymentPlanner.getDeploymentSlot}.
     */
    public Optional<Coords> getDeploymentSlot(Entity entity, List<Coords> legalHexes) {
        return deployment.getDeploymentSlot(entity, legalHexes);
    }

    /**
     * See {@code DeploymentPlanner.keepReachable}.
     */
    public List<Coords> keepReachable(Entity unit, List<Coords> hexes) {
        return deployment.keepReachable(unit, hexes);
    }

    /**
     * See {@code DeploymentPlanner.leaderStillToPlace}.
     */
    public Optional<Entity> leaderStillToPlace(Entity entity) {
        return deployment.leaderStillToPlace(entity);
    }

    /**
     * See {@code DeploymentPlanner.preferDeploymentSlot}.
     */
    public List<Coords> preferDeploymentSlot(Entity entity, List<Coords> possibleDeployCoords) {
        return deployment.preferDeploymentSlot(entity, possibleDeployCoords);
    }

    /**
     * See {@code DeploymentPlanner.preferFormationFit}.
     */
    public List<Coords> preferFormationFit(Entity entity, List<Coords> possibleDeployCoords) {
        return deployment.preferFormationFit(entity, possibleDeployCoords);
    }

    /**
     * See {@code DeploymentPlanner.setDeploymentAnchor}.
     */
    public void setDeploymentAnchor(Entity leader, Coords hex, Entity pickedBy) {
        deployment.setDeploymentAnchor(leader, hex, pickedBy);
    }

    /**
     * See {@code PhaseLineCoordinator.isWaitingAtPhaseLine}.
     */
    boolean isWaitingAtPhaseLine(Entity entity) {
        return phaseLines.isWaitingAtPhaseLine(entity);
    }

    /**
     * See {@code PhaseLineCoordinator.stillComingToPhaseLine}.
     */
    List<Entity> stillComingToPhaseLine(Entity entity, String phaseLine) {
        return phaseLines.stillComingToPhaseLine(entity, phaseLine);
    }



    /**
     * See {@code FormationRoster.isFallingBehind}.
     */
    boolean isFallingBehind(Entity unit) {
        return roster.isFallingBehind(unit);
    }

    /**
     * See {@code FormationRoster.isFollowingPlayerUnit}.
     */
    public boolean isFollowingPlayerUnit(Entity entity) {
        return roster.isFollowingPlayerUnit(entity);
    }


    /**
     * See {@code FormationRoster.isOutOfAction}.
     */
    boolean isOutOfAction(Entity unit) {
        return roster.isOutOfAction(unit);
    }

    /**
     * See {@code FormationLegs.isOnTownLeg}.
     */
    boolean isOnTownLeg(Entity entity) {
        return townLegs.isOnTownLeg(entity);
    }




    /**
     * See {@code FormationLegs.townCoverDiscount}.
     */
    int townCoverDiscount(Entity entity, Coords finalHex) {
        return townLegs.townCoverDiscount(entity, finalHex);
    }




    /**
     * See {@code FormationSlots.getFormationSlot}.
     */
    public Optional<Coords> getFormationSlot(Entity entity) {
        return slots.getFormationSlot(entity);
    }


    /**
     * See {@code FormationMarch.estimatedAssemblyTurns}.
     */
    int estimatedAssemblyTurns(Entity leader) {
        return march.estimatedAssemblyTurns(leader);
    }

    /**
     * See {@code FormationMarch.isAssembling}.
     */
    boolean isAssembling(Entity entity) {
        return march.isAssembling(entity);
    }

    /**
     * See {@code FormationMarch.isWaitingForFormation}.
     */
    public boolean isWaitingForFormation(Entity entity) {
        return march.isWaitingForFormation(entity);
    }

    /**
     * See {@code FormationMarch.limitToFormationPace}.
     */
    List<MovePath> limitToFormationPace(Entity entity, List<MovePath> paths) {
        return march.limitToFormationPace(entity, paths);
    }

    /**
     * See {@code FormationMarch.noteAssembled}.
     */
    void noteAssembled(int formationId) {
        march.noteAssembled(formationId);
    }
}
