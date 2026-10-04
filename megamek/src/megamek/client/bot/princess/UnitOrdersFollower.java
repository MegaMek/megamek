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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import megamek.client.bot.Messages;
import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.GameTurn;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.LanceRole;
import megamek.common.orders.NavPoint;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.PhaseLine;
import megamek.common.orders.RouteStyle;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;
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

    /** How far off its slot a formation unit may stand when the slot itself is blocked. */
    static final int FORMATION_SLACK = 1;

    /** A formation that keeps together counts as formed when every unit is this close to its slot. */
    static final int REFORM_SLACK = 2;

    /**
     * The most rounds a leader waits at a waypoint for its formation, however far the last unit has to come, so one
     * stuck unit cannot hold the rest.
     */
    static final int MAXIMUM_REFORM_WAIT_ROUNDS = 6;

    /** The hexes of a leader's walk a Column remembers, enough for a long column at wide spacing. */
    private static final int MAXIMUM_TRAIL_LENGTH = 48;

    /** The longest move filled in hex by hex; anything longer, such as a unit set down elsewhere, restarts the trail. */
    private static final int MAXIMUM_TRAIL_GAP = 20;

    /** The least extra movement a slot may cost over the unit's column place before the unit folds into the column. */
    private static final int MINIMUM_FOLD_MARGIN_MP = 3;

    /** Rounds a unit may lie prone before its lance counts it out of action and moves on without it. */
    static final int PRONE_ROUNDS_BEFORE_DROPPED = 2;

    /** Rounds a unit may go without getting any closer to its slot before its lance stops waiting for it. */
    static final int ROUNDS_WITHOUT_PROGRESS = 3;

    /** The place of a lance's second-in-command, who takes command when the commander is lost. */
    private static final int SECOND_IN_COMMAND_PLACE = 2;
    // a planned turning point counts as passed this near: the unit makes for the next one, it need not stand on it
    private static final int TURN_POINT_RADIUS = 2;

    // marks a cached Column slot taken from the leader's trail rather than laid out by heading
    private static final int TRAIL_HEADING = -1;

    private final Princess owner;
    private final Set<Integer> arrivedUnitIds = new HashSet<>();
    private final Map<Integer, SlotChoice> slotChoices = new HashMap<>();
    // slots laid out around a leader still on its way, never ahead of it; kept like slotChoices
    private final Map<Integer, SlotChoice> movingSlotChoices = new HashMap<>();
    // the hexes each formation leader has walked, newest first and hex by hex, for a Column to follow; not saved
    private final Map<Integer, Deque<Coords>> leaderTrails = new HashMap<>();

    /**
     * A formation unit's worked-out slot, kept while the leader's waypoint, the formation's heading and the unit's
     * place in it stay the same. A unit leaving or joining the formation moves the others up or down a place.
     *
     * @param anchor      the leader's waypoint the slot is laid out around
     * @param heading     the formation's heading, 0-5
     * @param slotIndex   the unit's place in the formation, the leader being 0
     * @param memberCount how many units the formation had
     * @param slot        the slot, or {@code null} when no hex would do
     */
    private record SlotChoice(Coords anchor, int heading, int slotIndex, int memberCount, @Nullable Coords slot) {}

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

    /** Each unit's route as last seen, by unit id, to tell a new order from waypoints ticked off; not saved. */
    private final Map<Integer, List<Coords>> knownRoutes = new HashMap<>();

    /** The leaders holding at a waypoint until their formation assembles, by unit id; not saved. */
    private final Map<Integer, ReformWait> assemblyWaits = new HashMap<>();

    /**
     * How a formation unit is getting on toward its slot: the best it has done, and since when.
     *
     * @param slot       the slot it is making for
     * @param bestCost   the least movement it has needed from where it stood to reach the slot
     * @param sinceRound the round it last got closer
     */
    private record SlotProgress(Coords slot, int bestCost, int sinceRound) {}

    /** Each formation unit's progress toward its slot, by unit id; not saved. */
    private final Map<Integer, SlotProgress> slotProgress = new HashMap<>();

    /** The round each formation unit was first seen prone, by unit id; not saved. */
    private final Map<Integer, Integer> proneSinceRounds = new HashMap<>();

    /** Units already called out of action, so the radio calls it once; not saved. */
    private final Set<Integer> outOfActionUnitIds = new HashSet<>();

    /** Units already called as falling behind, so the radio calls it once; not saved. */
    private final Set<Integer> fallingBehindUnitIds = new HashSet<>();

    /** The unit last seen leading each formation, by the formation's leader id, to call a change of command. */
    private final Map<Integer, Integer> commandingUnitIds = new HashMap<>();

    /**
     * A formation's leg to its leader's next flag, worked out once when the leg starts.
     *
     * @param anchor    the flag the leg leads to
     * @param memberIds the formation's units in action when it was worked out, the leader first
     * @param isTown    {@code true} if the way runs through a town, so the lance breaks formation until the flag
     * @param spots     each unit's place at the flag by unit id, the leader's being the flag; on a town leg the unit
     *                  makes straight for it
     * @param places    each unit's place in the formation by unit id, the leader's being 0
     * @param round     the round the places were last shared out
     */
    private record FormationLeg(Coords anchor, List<Integer> memberIds, boolean isTown, Map<Integer, Coords> spots,
          Map<Integer, Integer> places, int round) {}

    /** The movement points a new sharing out of places must save before units still coming change places. */
    private static final int REPAIR_MARGIN_MP = 2;

    /** The round each unit was last found past its place in a column, by unit id, to log it once a round. */
    private final Map<Integer, Integer> heldPastPlaceRounds = new HashMap<>();

    /**
     * The formations that have formed up since their route was given, by the formation's leader id; not saved. Until
     * then the lance is assembling: deployment is usually done before the move order, so the lance starts scattered and
     * its first waypoint is where it assembles (HammerGS, 2026-10-01).
     */
    private final Set<Integer> assembledFormations = new HashSet<>();

    /** The most rounds a lance assembling at its first waypoint waits for its last unit; a stuck unit drops out sooner. */
    static final int MAXIMUM_ASSEMBLY_WAIT_ROUNDS = 12;

    // what the bot knows of each convoy: its units, front, heading and exit; the one source for convoys and escorts
    private final ConvoyTracker convoys;
    // the waypoint each unit last planned its way to, so a leg is planned once
    private final Map<Integer, Coords> legsPlannedTo = new HashMap<>();

    /** Each formation's current leg, by the formation's leader id; not saved. */
    private final Map<Integer, FormationLeg> formationLegs = new HashMap<>();

    /** The narrow hexes of each board, by board id, worked out once a round; not saved. */
    private final Map<Integer, Map<Coords, Integer>> narrowHexesByBoard = new HashMap<>();
    private int narrowHexesRound = -1;

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

    /**
     * @param owner the bot whose units follow orders
     */
    UnitOrdersFollower(Princess owner) {
        this.owner = owner;
        this.convoys = new ConvoyTracker(owner);
        this.phaseLines = new PhaseLineCoordinator(owner, this);
        this.deployment = new DeploymentPlanner(owner, this);
        this.convoyEscorts = new ConvoyEscortFollower(owner, this);
        this.distances = new RouteDistances(owner, this);
        this.facings = new OrderedFacing(owner, this);
        this.fireReaction = new FireReaction(owner, this);
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
              || isWaitingForFormation(entity) || phaseLines.isWaitingAtPhaseLine(entity)
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
        // a lance waiting at a flag for its formation to re-form stops there too, and should face on toward the next
        // flag rather than the way it came in (HammerGS's playtest, 2026-09-27: backs turned to the next flag)
        boolean isWaitingAtFlag = isWaitingForFormation(formationLeaderOf(entity).orElse(entity));
        boolean isStoppedAtWaypoint = orders.hasRoute()
              && ((orders.getRoute().size() == 1) || isHoldingAtWaypoint(entity) || isWaitingAtFlag);
        if (isStoppedAtWaypoint) {
            // a formation unit holding with its leader faces the way set on the leader's waypoint
            Entity waypointOwner = formationLeaderOf(entity).orElse(entity);
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
     * @return the unit leading the unit's formation, if the unit is in one and is not leading it
     */
    Optional<Entity> formationLeaderOf(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || activeFormation(entity).isEmpty()) {
            // out of formation on this leg: the unit follows its own route like a unit on its own
            return Optional.empty();
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        if ((members.size() < 2) || (members.get(0).getId() == entity.getId()) || !members.contains(entity)) {
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
     * @return {@code true} if the unit is in a convoy lance
     */
    static boolean isConvoy(Entity entity) {
        LanceRole role = entity.getLanceRole();
        return (role != null) && role.isConvoy();
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
              || ((entity.getUnitOrders().getEdgeOrder() == EdgeOrder.NONE) && convoyEscorts.escortExitEdge(entity).isPresent());
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
        trackFormationUnits();
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
            if (isPartWay && getFormationSlot(entity).isPresent()) {
                // a unit in formation takes its route from its leader's; ticking off a waypoint it merely passed
                // near sent it on toward the next one, ahead of the formation (HammerGS's playtest, 2026-09-26)
                continue;
            }
            if (isPartWay && phaseLines.isWaitingAtPhaseLine(entity)) {
                // at a phase line: the route waits until every lance on the line is in
                continue;
            }
            if (isPartWay && entity.getUnitOrders().getWaypointOrder(0).isHold()) {
                advanceHold(entity, waypoint.get());
                continue;
            }
            boolean isPlannedTurn = entity.getUnitOrders().getWaypointOrder(0).isPlannedTurn();
            int reachedWithin = isPlannedTurn ? TURN_POINT_RADIUS
                  : (isLeadingFormationOnRoute(entity) ? flagRadius(entity, waypoint.get())
                        : Princess.DISTANCE_TO_WAYPOINT);
            if (waypoint.get().distance(entity.getPosition()) > reachedWithin) {
                if (reformWaits.remove(entity.getId()) != null) {
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
            boolean isPassedThrough = isPlannedTurn && !isAssembling(entity);
            if (isPartWay && !isPassedThrough && shouldWaitForFormation(entity, waypoint.get())) {
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
                owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.ARRIVED,
                      navLabel(entity, waypoint.get()));
            }
        }
        syncFollowerRoutes();
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
        if (!orders.hasRoute() || isFormationFollower(entity) || (entity.getPosition() == null) || DeploymentPlanner.isFlying(entity)
              || (orders.getWaypointOrder(0).getRoutePlan() != WaypointOrder.RoutePlan.PLAN_LEG)) {
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
        if (isSameOrder || isFormationFollower(entity)) {
            return;
        }
        legsPlannedTo.remove(entity.getId());
        if (known != null) {
            // a changed order: the lance assembles again at its new first waypoint before it marches. Every lance
            // starts out assembling, so the first order needs nothing here
            entity.getUnitOrders().getFormation().ifPresent(order -> assembledFormations.remove(order.getLeaderId()));
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
     * @return {@code true} if the unit leads a formation that keeps together and is waiting at a waypoint for the
     *       others to form up
     */
    public boolean isWaitingForFormation(Entity entity) {
        ReformWait wait = reformWaits.get(entity.getId());
        Optional<Coords> waypoint = entity.getUnitOrders().getNextWaypoint();
        return (wait != null) && waypoint.isPresent() && wait.waypoint().equals(waypoint.get());
    }

    /**
     * Decides, once a formation leader that keeps together, or that changes shape at this waypoint, has reached a
     * waypoint part-way along its route, whether it waits there for its formation: until every other unit is within
     * {@link #REFORM_SLACK} of its slot, for at most {@link #MAXIMUM_REFORM_WAIT_ROUNDS} rounds. Then the formation
     * moves on to the next waypoint together.
     *
     * @return {@code true} to wait another round
     */
    private boolean shouldWaitForFormation(Entity leader, Coords waypoint) {
        Optional<FormationOrder> formation = activeFormation(leader);
        // a formation that changes shape at this waypoint re-forms here before moving on, kept together or not
        boolean isReformingHere = leader.getUnitOrders().hasRoute()
              && (leader.getUnitOrders().getWaypointOrder(0).getArrivalFormation() != null);
        boolean isAssembling = isAssembling(leader);
        if (formation.isEmpty() || (!formation.get().isKeepTogether() && !isReformingHere && !isAssembling)) {
            return false;
        }
        // one kept together stops to re-form only where it matters: at the end of the route, and where its shape
        // changes. Stopping at every waypoint along the way cost 12 of 16 rounds on a route with waypoints a few hexes
        // apart; between the stops the leader keeps pace with the last unit instead (HammerGS, 2026-09-27)
        List<Coords> route = leader.getUnitOrders().getRoute();
        boolean isRouteEnd = route.size() <= 1;
        if (!isRouteEnd && !isReformingHere && !changesShapeAfter(leader.getUnitOrders())) {
            String reasonToStop = isAssembling ? "assembling at the first waypoint since the order"
                  : reasonToFormUpHere(leader, formation.get(), waypoint, route);
            if (reasonToStop == null) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_PASS at {} - same shape on the next leg and "
                            + "the lance is together; not stopping to re-form", leader.getDisplayName(),
                      leader.getId(), currentRound(), waypoint.getBoardNum());
                reformWaits.remove(leader.getId());
                return false;
            }
            if (!reformWaits.containsKey(leader.getId())) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_STOP at {} - {}; forming up before moving on",
                      leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(), reasonToStop);
            }
        }
        List<Entity> members = formationMembers(leader, formation.get().getLeaderId());
        boolean isLeading = (members.size() >= 2) && (members.get(0).getId() == leader.getId());
        boolean isBroken = FireReaction.isBrokenToFight(leader);
        if (!isLeading || isBroken) {
            reformWaits.remove(leader.getId());
            return false;
        }
        int outOfPlace = countOutOfPlace(members);
        ReformWait wait = reformWaits.get(leader.getId());
        if ((wait == null) || !wait.waypoint().equals(waypoint)) {
            wait = new ReformWait(waypoint, currentRound(), assemblyWaitRounds(leader, waypoint,
                  isAssembling ? MAXIMUM_ASSEMBLY_WAIT_ROUNDS : MAXIMUM_REFORM_WAIT_ROUNDS));
        }
        int roundsWaited = currentRound() - wait.sinceRound();
        if (outOfPlace == 0) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_FORMED at {} - moving on together{}",
                  leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(),
                  isAssembling ? "; assembled, now marching" : "");
            reformWaits.remove(leader.getId());
            noteAssembled(formation.get().getLeaderId());
            if (route.size() > 1) {
                // at the end of the route the exit call says it all
                owner.getOrdersRadio().report(leader, OrdersRadio.RadioEvent.FORMED, navLabel(leader, waypoint),
                      navLabel(leader, route.get(1)));
            }
            return false;
        }
        if (roundsWaited >= wait.maxRounds()) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_WAIT_OVER at {} - {} unit(s) still out of place "
                        + "after {} round(s); moving on", leader.getDisplayName(), leader.getId(), currentRound(),
                  waypoint.getBoardNum(), outOfPlace, roundsWaited);
            reformWaits.remove(leader.getId());
            noteAssembled(formation.get().getLeaderId());
            return false;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_WAIT at {} - {} of {} unit(s) still forming up",
              leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(), outOfPlace,
              members.size() - 1);
        reformWaits.put(leader.getId(), wait);
        return true;
    }

    /**
     * Why a lance keeping together should stop to form up at a flag where its shape does not change, or {@code null}
     * to pass on. It stops where it has come apart - a unit more than a turn's move from its place - and before a leg
     * through a town, as a unit assembles before going through the doors (HammerGS's playtest, 2026-10-01: a lance
     * that never formed up passed its first flag and went into the town strung out over half the map). A lance that
     * is together still passes through.
     */
    private @Nullable String reasonToFormUpHere(Entity leader, FormationOrder formation, Coords flag,
          List<Coords> route) {
        Board board = owner.getGame().getBoard(leader);
        if ((board != null) && (route.size() > 1) && (formation.getShape() != FormationShape.COLUMN)) {
            Coords nextFlag = route.get(1);
            int townHexes = TownLegPlanner.hexesBesideBuildings(board, flag, nextFlag,
                  hex -> distances.routeCostFrom(leader, nextFlag, hex));
            if (townHexes >= TownLegPlanner.TOWN_HEXES) {
                return "the next leg runs through a town (" + townHexes + " hexes in or beside buildings)";
            }
        }
        for (Entity member : formationMembers(leader, formation.getLeaderId())) {
            if ((member.getId() == leader.getId()) || isFallingBehind(member)) {
                continue;
            }
            Optional<Coords> slot = getFormationSlot(member);
            if (slot.isEmpty() || (member.getPosition() == null)) {
                continue;
            }
            int cost = distances.routeCostFrom(member, slot.get(), member.getPosition());
            int turnsMove = Math.max(1, paceMovementPoints(member, formation.getPace()));
            if ((cost != WaypointDistanceField.UNREACHABLE) && (cost > turnsMove)) {
                return member.getShortName() + " is " + cost + " MP from its place at " + slot.get().getBoardNum();
            }
        }
        return null;
    }

    /**
     * @return {@code true} if the unit is more than a turn's walk from its place in its formation
     */
    private boolean isLagging(Entity entity) {
        Optional<Coords> slot = getFormationSlot(entity);
        if (slot.isEmpty() || (entity.getPosition() == null) || entity.getPosition().equals(slot.get())) {
            return false;
        }
        int cost = distances.routeCostFrom(entity, slot.get(), entity.getPosition());
        if (cost == WaypointDistanceField.UNREACHABLE) {
            cost = entity.getPosition().distance(slot.get());
        }
        return cost > entity.getWalkMP();
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit's formation has not yet formed up since its route was given, so it is assembling
     *       at its first waypoint rather than marching
     */
    boolean isAssembling(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        return formation.isPresent() && !assembledFormations.contains(formation.get().getLeaderId());
    }

    /**
     * Marks a formation as formed up: from here on it marches, keeping its places beside its leader.
     *
     * @param formationId the formation's leader id
     */
    void noteAssembled(int formationId) {
        assembledFormations.add(formationId);
    }

    /**
     * @return {@code true} if the leg after the next waypoint is set to travel in a different formation from the leg
     *       ending there, so the lance changes shape at the waypoint
     */
    private static boolean changesShapeAfter(UnitOrders orders) {
        if (orders.getRoute().size() < 2) {
            return false;
        }
        return !Objects.equals(orders.getWaypointOrder(0).getFormation(), orders.getWaypointOrder(1).getFormation());
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
            if (slot.isEmpty() || member.getPosition().equals(slot.get()) || isFallingBehind(member)) {
                continue;
            }
            int cost = distances.routeCostFrom(member, slot.get(), member.getPosition());
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
        int rounds = Math.clamp(estimatedAssemblyTurns(leader) + 1, 1, Math.max(1, cap));
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
            if (slot.isPresent() && (member.getPosition().distance(slot.get()) > REFORM_SLACK)
                  && !isFallingBehind(member)) {
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
    boolean isFormationFollower(Entity entity) {
        return formationLeaderOf(entity).isPresent();
    }

    /**
     * Orders a unit off the board by the edge nearest the last waypoint of its route, and with it every unit of the
     * formation it leads.
     */
    private void exitWithFormation(Entity entity, Coords lastWaypoint) {
        // a convoy's exit hex sits on its exit edge, but a corner is as near another; it leaves by its own
        OffBoardDirection edge = isConvoy(entity) ? entity.getLanceRole().getExitEdge()
              : nearestEdge(entity, lastWaypoint);
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
        owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.EXITING, edge.name().toLowerCase(Locale.ROOT));
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
                owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.HOLDING, navLabel(entity, waypoint));
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
        if (FireReaction.isBrokenToFight(leader)) {
            LOGGER.debug("[BotOrders] {} (ID {}): formation broken - the lance is fighting", entity.getDisplayName(),
                  entity.getId());
            return Optional.empty();
        }
        Optional<Coords> leaderWaypoint = leader.getUnitOrders().getNextWaypoint();
        Coords anchor = leaderWaypoint.orElse(leader.getPosition());
        int heading = formationHeading(leader, anchor);
        // the place in the formation counts only the units still in it, so a unit that leaves closes up the gap
        if (!members.contains(entity)) {
            // out of action: it holds where it is
            return Optional.empty();
        }
        FormationLeg leg = formationLeg(members, anchor, heading, formation.get());
        // the place each unit takes is shared out by the way each would go there, the last arrival soonest
        int slotIndex = leg.places().getOrDefault(entity.getId(), members.indexOf(entity));
        if ((leg.isTown() || isAssembling(entity)) && leg.spots().containsKey(entity.getId())) {
            // through a town the lance breaks formation, and assembling at its first waypoint it has not formed yet:
            // either way each unit makes straight for its place at the flag
            return Optional.of(leg.spots().get(entity.getId()));
        }
        if ((formation.get().getShape() == FormationShape.COLUMN) && !isStoppedOnFlag(leader)) {
            // a Column on the move falls in behind its commander, on the hexes the commander walked (HammerGS,
            // 2026-09-27); stopped on a flag it forms straight behind the way it faces there, below
            Coords trailSlot = trailSlot(entity, leader, anchor, formation.get(), slotIndex);
            if (trailSlot != null) {
                return Optional.of(holdIfPastPlace(entity, anchor, trailSlot));
            }
        }
        SlotChoice cached = slotChoices.get(entity.getId());
        if ((cached != null) && cached.anchor().equals(anchor) && (cached.heading() == heading)
              && (cached.slotIndex() == slotIndex) && (cached.memberCount() == members.size())) {
            // the cached slot is laid out round the flag; a leader still on its way there still leads, so the
            // unit keeps its place beside the leader. Returning the cached slot as it was sent every unit but the
            // first call's on to the flag ahead of its leader (HammerGS's playtest, 2026-09-27: the Griffin stood in
            // its slot at 1610 four rounds before its Stalker came round the buildings)
            return Optional.ofNullable(behindMovingLeader(entity, leader, anchor, cached.slot(), formation.get(),
                  slotIndex));
        }
        if ((cached != null) && (cached.slotIndex() != slotIndex)) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: moves from place {} to place {} of {} in the formation",
                  entity.getDisplayName(), entity.getId(), currentRound(), cached.slotIndex(), slotIndex,
                  members.size());
        }
        Coords slot = chooseSlot(entity, anchor, heading, formation.get(), slotIndex);
        slotChoices.put(entity.getId(), new SlotChoice(anchor, heading, slotIndex, members.size(), slot));
        return Optional.ofNullable(behindMovingLeader(entity, leader, anchor, slot, formation.get(), slotIndex));
    }

    /**
     * A slot laid out around the leader's next flag can lie ahead of a leader still on its way there, and the unit
     * then ran ahead of its commander (HammerGS's playtest, 2026-09-27: a Centurion two hexes in front of the
     * Grasshopper leading it). While the leader is further from the flag than the slot is, the shape is laid out
     * around the leader instead, facing the flag; it settles onto the flag as the leader comes up to it.
     */
    private @Nullable Coords behindMovingLeader(Entity entity, Entity leader, Coords flag, @Nullable Coords slot,
          FormationOrder formation, int slotIndex) {
        Coords leaderPosition = leader.getPosition();
        if ((slot == null) || leaderPosition.equals(flag) || (slot.distance(flag) >= leaderPosition.distance(flag))) {
            return slot;
        }
        int heading = leaderPosition.direction(flag);
        SlotChoice cached = movingSlotChoices.get(entity.getId());
        if ((cached != null) && cached.anchor().equals(leaderPosition) && (cached.heading() == heading)
              && (cached.slotIndex() == slotIndex)) {
            return cached.slot();
        }
        Board board = owner.getGame().getBoard(entity);
        Coords ideal = FormationPlanner.idealSlot(leaderPosition, heading, formation.getShape(), formation.getSpacing(),
              slotIndex);
        Coords moving = (board == null) ? null : settle(entity, board, leaderPosition, ideal);
        Coords chosen = (moving == null) ? slot : moving;
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_BEHIND - {} slot {} at {} beside {}, which is still "
                    + "on its way to {}", entity.getDisplayName(), entity.getId(), currentRound(), formation.getShape(),
              slotIndex, chosen.getBoardNum(), leader.getDisplayName(), flag.getBoardNum());
        movingSlotChoices.put(entity.getId(), new SlotChoice(leaderPosition, heading, slotIndex, 0, chosen));
        return chosen;
    }

    /**
     * The way a unit in formation faces: the way the formation itself faces - the facing set where it stops, the next
     * leg where it passes a flag, else from its leader toward the leader's flag. Facing the flag's hex instead turned
     * a unit whose slot lay beyond the flag round to look back at it (HammerGS's town playtest, 2026-09-27: the
     * Centurion at 1410 faced south toward 1512).
     *
     * @return the facing 0-5, or {@link UnitOrders#FACING_AUTO} for a unit not following a formation leader
     */
    int formationFacing(Entity entity) {
        Optional<Entity> leader = formationLeaderOf(entity);
        if (leader.isEmpty() || (leader.get().getPosition() == null)) {
            return UnitOrders.FACING_AUTO;
        }
        List<Coords> route = leader.get().getUnitOrders().getRoute();
        if (route.isEmpty()) {
            return UnitOrders.FACING_AUTO;
        }
        Coords flag = route.get(0);
        if (isStoppedOnFlag(leader.get())) {
            return formationHeading(leader.get(), flag);
        }
        if (leader.get().getPosition().equals(flag)) {
            return (route.size() > 1) ? flag.direction(route.get(1)) : UnitOrders.FACING_AUTO;
        }
        return leader.get().getPosition().direction(flag);
    }

    /**
     * @return {@code true} if the leader stands on its flag and the lance stops there: the end of the route, a hold,
     *       or a wait for the formation to re-form. A Column stopped there forms straight behind the facing set on the
     *       flag, not back along the way it came in (HammerGS's playtest, 2026-09-27: ordered to face north at
     *       2403, the Column trailed off to the south-west along its approach)
     */
    private boolean isStoppedOnFlag(Entity leader) {
        List<Coords> route = leader.getUnitOrders().getRoute();
        if (route.isEmpty() || (leader.getPosition() == null) || !route.get(0).equals(leader.getPosition())) {
            return false;
        }
        return (route.size() == 1) || leader.getUnitOrders().getWaypointOrder(0).isHold()
              || isWaitingForFormation(leader);
    }

    /**
     * A unit already further along the way than its place in the column holds where it is and lets the column come up
     * to it, rather than walking back to its place: a Stalker last in a column walked back south from 1127 to 1129 to
     * stand six hexes behind its commander (HammerGS's playtest, 2026-10-01).
     *
     * @return the unit's own hex when it is past its place, else its place
     */
    private Coords holdIfPastPlace(Entity entity, Coords flag, Coords place) {
        Coords position = entity.getPosition();
        if (position.equals(place)) {
            return place;
        }
        int fromHere = distances.routeCost(entity, flag, position, false, false);
        int fromPlace = distances.routeCost(entity, flag, place, false, false);
        if ((fromHere == WaypointDistanceField.UNREACHABLE) || (fromPlace == WaypointDistanceField.UNREACHABLE)
              || (fromHere >= fromPlace)) {
            return place;
        }
        Integer lastLogged = heldPastPlaceRounds.put(entity.getId(), currentRound());
        if ((lastLogged == null) || (lastLogged != currentRound())) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_HOLD - already past its column place at {}; holding "
                        + "at {} for the column to come up", entity.getDisplayName(), entity.getId(), currentRound(),
                  place.getBoardNum(), position.getBoardNum());
        }
        return position;
    }

    /**
     * A Column's slot: the hex its commander walked {@code place x spacing} hexes back, so the column snakes through
     * the ground behind it rather than lining up behind the next flag. Until the commander has walked that far, the
     * slot lies straight behind it, facing the flag.
     *
     * @return the slot, or {@code null} when neither the trail nor the hexes behind the commander will do
     */
    private @Nullable Coords trailSlot(Entity entity, Entity leader, Coords flag, FormationOrder formation,
          int slotIndex) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return null;
        }
        SlotChoice cached = movingSlotChoices.get(entity.getId());
        if ((cached != null) && cached.anchor().equals(leader.getPosition()) && (cached.slotIndex() == slotIndex)
              && (cached.heading() == TRAIL_HEADING)) {
            return cached.slot();
        }
        Deque<Coords> trail = recordTrail(leader);
        int hexesBack = slotIndex * formation.getSpacing();
        Coords ideal = null;
        int stepsBack = 0;
        for (Coords walked : trail) {
            if (stepsBack == hexesBack) {
                ideal = walked;
                break;
            }
            stepsBack++;
        }
        if (ideal == null) {
            int heading = leader.getPosition().equals(flag) ? leader.getFacing() : leader.getPosition().direction(flag);
            ideal = FormationPlanner.idealSlot(leader.getPosition(), heading, FormationShape.COLUMN,
                  formation.getSpacing(), slotIndex);
        }
        Coords settled = settle(entity, board, leader.getPosition(), ideal);
        if (settled != null) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_TRAIL - column place {} at {}, {} hex(es) behind "
                        + "{} along its path", entity.getDisplayName(), entity.getId(), currentRound(), slotIndex,
                  settled.getBoardNum(), hexesBack, leader.getDisplayName());
            movingSlotChoices.put(entity.getId(), new SlotChoice(leader.getPosition(), TRAIL_HEADING, slotIndex, 0,
                  settled));
        }
        return settled;
    }

    /**
     * Adds the leader's hex to its trail if it has moved, filling in the hexes between so the trail runs hex by hex.
     * A jump too long to fill in starts the trail again.
     *
     * @return the trail, newest first
     */
    private Deque<Coords> recordTrail(Entity leader) {
        Deque<Coords> trail = leaderTrails.computeIfAbsent(leader.getId(), id -> new ArrayDeque<>());
        Coords now = leader.getPosition();
        Coords last = trail.peekFirst();
        if (now.equals(last)) {
            return trail;
        }
        if ((last != null) && (last.distance(now) <= MAXIMUM_TRAIL_GAP)) {
            for (Coords between : Coords.intervening(last, now)) {
                if (!between.equals(trail.peekFirst())) {
                    trail.addFirst(between);
                }
            }
        } else {
            trail.clear();
        }
        if (!now.equals(trail.peekFirst())) {
            trail.addFirst(now);
        }
        while (trail.size() > MAXIMUM_TRAIL_LENGTH) {
            trail.removeLast();
        }
        return trail;
    }

    /**
     * The way the formation faces around its leader's waypoint: where the lance stops there (a hold, or the end of the
     * route), the facing set on that waypoint; else toward the next waypoint while more follow; at the last one, the
     * leader's ordered stopped facing, or else the direction of the last leg, fixed the first time it is worked out so
     * the shape does not swing as the leader closes in.
     *
     * <p>A waypoint the lance only passes through lays the shape along the way it is going. Its facing is where the
     * units face if they stop there; turning the shape by it sent the tail of a Column seven hexes off the route
     * (HammerGS's playtest, 2026-09-27).</p>
     */
    private int formationHeading(Entity leader, Coords anchor) {
        List<Coords> leaderRoute = leader.getUnitOrders().getRoute();
        WaypointOrder headOrder = leader.getUnitOrders().getWaypointOrder(0);
        boolean isStopHere = headOrder.isHold() || (leaderRoute.size() == 1);
        if (!leaderRoute.isEmpty() && anchor.equals(leaderRoute.get(0)) && isStopHere
              && (headOrder.getFacing() != UnitOrders.FACING_AUTO)) {
            return headOrder.getFacing();
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
        Coords columnSlot = settle(entity, board, anchor, FormationPlanner.idealSlot(anchor, heading,
              FormationShape.COLUMN, formation.getSpacing(), slotIndex));
        if ((settled != null) && !isFarHarderThan(entity, settled, columnSlot)) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_SLOT - {} slot {} at {} (around {}, facing {})",
                  entity.getDisplayName(), entity.getId(), currentRound(), formation.getShape(), slotIndex,
                  settled.getBoardNum(), anchor.getBoardNum(), heading);
            return settled;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_FOLD - {} slot {} blocked, folding to column at {}",
              entity.getDisplayName(), entity.getId(), currentRound(), formation.getShape(), slotIndex,
              (columnSlot == null) ? anchor.getBoardNum() : columnSlot.getBoardNum());
        owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.FOLD, navLabel(entity, anchor));
        return (columnSlot == null) ? anchor : columnSlot;
    }

    /**
     * A slot the unit can stand in may still lie behind a row of buildings or across water, a long way round for a
     * short distance, while its place in a Column behind the leader is along the way the leader came. Such a slot
     * counts as blocked, so the unit folds into the Column and the shape opens out again beyond the obstacle
     * (HammerGS's town playtest, 2026-09-27: a Longbow waded a hex a turn toward a Line slot behind a building row
     * while its lance waited five rounds).
     *
     * @return {@code true} if reaching the slot costs more than a turn's walk beyond reaching the column place
     */
    private boolean isFarHarderThan(Entity entity, Coords slot, @Nullable Coords columnSlot) {
        if ((columnSlot == null) || slot.equals(columnSlot) || (entity.getPosition() == null)) {
            return false;
        }
        int toSlot = distances.routeCostFrom(entity, slot, entity.getPosition());
        int toColumn = distances.routeCostFrom(entity, columnSlot, entity.getPosition());
        if (toColumn == WaypointDistanceField.UNREACHABLE) {
            return false;
        }
        int margin = Math.max(MINIMUM_FOLD_MARGIN_MP, entity.getWalkMP());
        boolean isFarHarder = (toSlot == WaypointDistanceField.UNREACHABLE) || (toSlot > toColumn + margin);
        if (isFarHarder) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: slot {} costs {} MP to reach, its column place {} only {} MP; "
                        + "folding", entity.getDisplayName(), entity.getId(), currentRound(), slot.getBoardNum(),
                  (toSlot == WaypointDistanceField.UNREACHABLE) ? "unreachable" : String.valueOf(toSlot),
                  columnSlot.getBoardNum(), toColumn);
        }
        return isFarHarder;
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
        // a slot in a building would have the unit walk into it, damaging it or bringing it down; the best hex next to
        // it is taken instead
        return board.contains(slot) && !slot.equals(leaderPosition) && !entity.isLocationProhibited(slot)
              && !board.getHex(slot).containsTerrain(Terrains.BUILDING)
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
     * Works out a formation's leg to its leader's next flag once, when the leg starts or the lance changes: whether the
     * way runs through a town and, if so, which unit takes which place at the flag. A Column is already single file
     * and keeps following its commander.
     */
    private FormationLeg formationLeg(List<Entity> members, Coords anchor, int heading, FormationOrder formation) {
        Entity leader = members.get(0);
        int formationId = formation.getLeaderId();
        List<Integer> memberIds = new ArrayList<>();
        for (Entity member : members) {
            memberIds.add(member.getId());
        }
        FormationLeg cached = formationLegs.get(formationId);
        if ((cached != null) && cached.anchor().equals(anchor) && cached.memberIds().equals(memberIds)) {
            if (cached.round() != currentRound()) {
                return repairStillComing(cached, members, formationId);
            }
            return cached;
        }
        Map<Integer, Coords> spots = pairPlaces(members, anchor, heading, formation);
        Map<Integer, Integer> places = placeNumbers(spots, members, anchor, heading, formation);
        FormationLeg leg = new FormationLeg(anchor, memberIds, false, spots, places, currentRound());
        Board board = owner.getGame().getBoard(leader);
        if ((board != null) && (formation.getShape() != FormationShape.COLUMN)) {
            int mostTownHexes = 0;
            for (Entity member : members) {
                Coords position = member.getPosition();
                if (position != null) {
                    mostTownHexes = Math.max(mostTownHexes, TownLegPlanner.hexesBesideBuildings(board, position,
                          anchor, hex -> distances.routeCostFrom(member, anchor, hex)));
                }
            }
            if (mostTownHexes >= TownLegPlanner.TOWN_HEXES) {
                leg = new FormationLeg(anchor, memberIds, true, spots, places, currentRound());
            }
            LOGGER.info("[BotOrders] {} (ID {}) round {}: {} to {} - {} hex(es) in or beside buildings on the way "
                        + "(a town leg from {}){}", leader.getDisplayName(), leader.getId(), currentRound(),
                  leg.isTown() ? "TOWN_LEG" : "OPEN_LEG", anchor.getBoardNum(), mostTownHexes,
                  TownLegPlanner.TOWN_HEXES, "; places " + describeSpots(leg.spots()));
        } else {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: COLUMN_LEG to {}; places {}", leader.getDisplayName(),
                  leader.getId(), currentRound(), anchor.getBoardNum(), describeSpots(leg.spots()));
        }
        formationLegs.put(formationId, leg);
        return leg;
    }

    /**
     * @return the formation's slots at the flag, place 1 first, as the shape lays them out
     */
    private List<Coords> slotsAtFlag(List<Entity> members, Coords anchor, int heading, FormationOrder formation) {
        Board board = owner.getGame().getBoard(members.get(0));
        List<Coords> slots = new ArrayList<>();
        for (int place = 1; place < members.size(); place++) {
            Coords ideal = FormationPlanner.idealSlot(anchor, heading, formation.getShape(), formation.getSpacing(),
                  place);
            Coords settled = (board == null) ? null : settle(members.get(place), board, anchor, ideal);
            slots.add((settled == null) ? ideal : settled);
        }
        return slots;
    }

    /**
     * @return each unit's number in the formation from its place at the flag: the leader 0, the others by which of the
     *       shape's slots they were given
     */
    private Map<Integer, Integer> placeNumbers(Map<Integer, Coords> spots, List<Entity> members, Coords anchor,
          int heading, FormationOrder formation) {
        List<Coords> slots = slotsAtFlag(members, anchor, heading, formation);
        Map<Integer, Integer> places = new HashMap<>();
        places.put(members.get(0).getId(), 0);
        for (int index = 1; index < members.size(); index++) {
            int slot = slots.indexOf(spots.get(members.get(index).getId()));
            places.put(members.get(index).getId(), (slot < 0) ? index : (slot + 1));
        }
        return places;
    }

    /**
     * @return the pace's movement points a turn for each unit, for pairing units with places by when they arrive
     */
    private int turnsMovement(Entity unit) {
        Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
        return paceMovementPoints(unit, formation.map(FormationOrder::getPace).orElse(FormationPace.WALK));
    }

    /**
     * The places at the flag: the leader's is the flag itself, and the others' are the formation's slots, shared out
     * by the way each unit would go to them so the last one arrives soonest. HammerGS did not mind which Mek took
     * which place, only that they could support each other (2026-10-01); kept in lobby order, the slow Stalker
     * deployed furthest west drew the far east end of the Line and crossed the whole lance.
     */
    private Map<Integer, Coords> pairPlaces(List<Entity> members, Coords anchor, int heading,
          FormationOrder formation) {
        List<Entity> followers = members.subList(1, members.size());
        List<Coords> slots = slotsAtFlag(members, anchor, heading, formation);
        Map<Integer, Coords> spots = TownLegPlanner.assignSpots(followers, slots, this::pairingCost,
              this::turnsMovement);
        if (spots.isEmpty()) {
            // too many to pair by trying every way: each keeps its own place
            for (int place = 1; place < members.size(); place++) {
                spots.put(members.get(place).getId(), slots.get(place - 1));
            }
        }
        spots.put(members.get(0).getId(), anchor);
        return spots;
    }

    /**
     * Shares out again, once a round, the places of the units still coming among the places still free, from where
     * they stand now. A unit in its place keeps it. HammerGS did not fix the places at the start of his walk through
     * the town: the first Mek through took the nearest place and the last took what was left (2026-10-01). The units
     * change places only when that saves {@link #REPAIR_MARGIN_MP} or more, so two do not swap back and forth.
     */
    private FormationLeg repairStillComing(FormationLeg leg, List<Entity> members, int formationId) {
        Map<Integer, Coords> spots = new HashMap<>(leg.spots());
        List<Entity> stillComing = new ArrayList<>();
        List<Coords> freeSpots = new ArrayList<>();
        for (Entity member : members.subList(1, members.size())) {
            Coords spot = spots.get(member.getId());
            if ((spot == null) || spot.equals(member.getPosition())) {
                continue;
            }
            stillComing.add(member);
            freeSpots.add(spot);
        }
        Map<Integer, Integer> places = new HashMap<>(leg.places());
        if (stillComing.size() >= 2) {
            Map<Integer, Coords> pairing = TownLegPlanner.assignSpots(stillComing, freeSpots, this::pairingCost,
                  this::turnsMovement);
            int costNow = 0;
            int costAfter = 0;
            int lastArrivalNow = 0;
            int lastArrivalAfter = 0;
            for (Entity unit : stillComing) {
                int perTurn = Math.max(1, turnsMovement(unit));
                int now = pairingCost(unit, spots.get(unit.getId()));
                costNow += now;
                lastArrivalNow = Math.max(lastArrivalNow, (now + perTurn - 1) / perTurn);
                if (!pairing.isEmpty()) {
                    int after = pairingCost(unit, pairing.get(unit.getId()));
                    costAfter += after;
                    lastArrivalAfter = Math.max(lastArrivalAfter, (after + perTurn - 1) / perTurn);
                }
            }
            boolean isQuicker = (lastArrivalAfter < lastArrivalNow)
                  || ((lastArrivalAfter == lastArrivalNow) && ((costAfter + REPAIR_MARGIN_MP) <= costNow));
            if (!pairing.isEmpty() && isQuicker) {
                Map<Coords, Integer> placeOfSpot = new HashMap<>();
                for (Entity unit : stillComing) {
                    placeOfSpot.put(spots.get(unit.getId()), places.get(unit.getId()));
                }
                for (Entity unit : stillComing) {
                    Coords newSpot = pairing.get(unit.getId());
                    spots.put(unit.getId(), newSpot);
                    places.put(unit.getId(), placeOfSpot.getOrDefault(newSpot, places.get(unit.getId())));
                }
                String gain = (lastArrivalAfter < lastArrivalNow)
                      ? ("the last in by turn " + lastArrivalAfter + " instead of " + lastArrivalNow)
                      : ("the last in by the same turn, " + (costNow - costAfter) + " MP less in all");
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_PLACES - the units still coming change places, "
                            + "{}: {}", members.get(0).getDisplayName(), members.get(0).getId(), currentRound(), gain,
                      describeSpots(pairing));
            }
        }
        FormationLeg repaired = new FormationLeg(leg.anchor(), leg.memberIds(), leg.isTown(), spots, places,
              currentRound());
        formationLegs.put(formationId, repaired);
        return repaired;
    }

    /**
     * @return the movement points from where the unit stands to a place, by the way it can really go; a place it
     *       cannot reach counts as far off
     */
    private int pairingCost(Entity unit, Coords spot) {
        int cost = distances.routeCost(unit, spot, unit.getPosition(), false, isSlowestOfLance(unit));
        return (cost == WaypointDistanceField.UNREACHABLE) ? UNREACHABLE_PLACE_COST : cost;
    }

    // a place a unit cannot reach, counted as far off rather than overflowing a sum
    private static final int UNREACHABLE_PLACE_COST = 10_000;

    /**
     * @return {@code true} if the unit is one of its lance's slowest at the lance's pace, with a faster unit in it
     */
    boolean isSlowestOfLance(Entity unit) {
        Optional<FormationOrder> formation = activeFormation(unit);
        if (formation.isEmpty()) {
            return false;
        }
        FormationPace pace = formation.get().getPace();
        return TownLegPlanner.isSlowest(unit, formationMembers(unit, formation.get().getLeaderId()),
              member -> paceMovementPoints(member, pace));
    }

    /**
     * @return the narrow hexes of the unit's board, worked out once a round since buildings can come down
     */
    Map<Coords, Integer> narrowHexes(Entity unit) {
        Board board = owner.getGame().getBoard(unit);
        if (board == null) {
            return Map.of();
        }
        if (narrowHexesRound != currentRound()) {
            narrowHexesByBoard.clear();
            narrowHexesRound = currentRound();
        }
        return narrowHexesByBoard.computeIfAbsent(unit.getBoardId(), ignored -> TownLegPlanner.narrowHexes(board));
    }

    /**
     * The movement points a move ending in cover counts as saving, for a unit on a town leg: where two moves are about
     * as good, the one ending in woods or on higher ground wins (HammerGS's town walk, 2026-10-01: the Griffin stopped
     * on the wooded hill at 1420 on purpose).
     *
     * @param entity   the unit about to move
     * @param finalHex where the move ends
     *
     * @return {@link TownLegPlanner#COVER_DISCOUNT_MP} for a move ending in cover on a town leg, else 0
     */
    int townCoverDiscount(Entity entity, Coords finalHex) {
        Board board = owner.getGame().getBoard(entity);
        if ((board == null) || (entity.getPosition() == null) || (finalHex == null) || !isOnTownLeg(entity)
              || !board.contains(entity.getPosition())) {
            return 0;
        }
        int startLevel = board.getHex(entity.getPosition()).getLevel();
        return TownLegPlanner.isCover(board, finalHex, startLevel) ? TownLegPlanner.COVER_DISCOUNT_MP : 0;
    }

    private String describeSpots(Map<Integer, Coords> spots) {
        StringBuilder description = new StringBuilder();
        for (Map.Entry<Integer, Coords> spot : spots.entrySet()) {
            Entity unit = owner.getGame().getEntity(spot.getKey());
            description.append((description.length() == 0) ? "" : ", ")
                  .append((unit == null) ? ("unit " + spot.getKey()) : unit.getShortName())
                  .append(' ').append(spot.getValue().getBoardNum());
        }
        return description.toString();
    }

    /**
     * @return {@code true} if the unit's formation is on a town leg, broken up until its flag
     */
    boolean isOnTownLeg(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || activeFormation(entity).isEmpty()) {
            return false;
        }
        FormationLeg leg = formationLegs.get(formation.get().getLeaderId());
        Optional<Coords> anchor = formationAnchor(entity, formation.get());
        return (leg != null) && leg.isTown() && anchor.isPresent() && leg.anchor().equals(anchor.get())
              && leg.spots().containsKey(entity.getId());
    }

    /**
     * @return the unit's own place at its flag while its formation is on a town leg
     */
    Optional<Coords> townSpotOf(Entity entity) {
        if (!isOnTownLeg(entity)) {
            return Optional.empty();
        }
        return Optional.of(formationLegs.get(entity.getUnitOrders().getFormation().get().getLeaderId()).spots()
              .get(entity.getId()));
    }

    boolean isTownSpotOf(Entity entity, Coords hex) {
        // the path ranker asks this for every move it weighs: rule most hexes out before looking at the lance
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty()) {
            return false;
        }
        FormationLeg leg = formationLegs.get(formation.get().getLeaderId());
        if ((leg == null) || !leg.isTown() || !hex.equals(leg.spots().get(entity.getId()))) {
            return false;
        }
        return isOnTownLeg(entity);
    }

    private Optional<Coords> formationAnchor(Entity entity, FormationOrder formation) {
        List<Entity> members = formationMembers(entity, formation.getLeaderId());
        if (members.isEmpty()) {
            return Optional.empty();
        }
        Entity leader = members.get(0);
        return Optional.of(leader.getUnitOrders().getNextWaypoint().orElse(leader.getPosition()));
    }

    /**
     * @return the other units of the unit's lance standing in their places on a town leg
     */
    List<Entity> unitsInPlaceBeside(Entity entity) {
        List<Entity> inPlace = new ArrayList<>();
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty()) {
            return inPlace;
        }
        FormationLeg leg = formationLegs.get(formation.get().getLeaderId());
        if (leg == null) {
            return inPlace;
        }
        for (Entity member : formationMembers(entity, formation.get().getLeaderId())) {
            Coords spot = leg.spots().get(member.getId());
            if ((member.getId() != entity.getId()) && (spot != null) && spot.equals(member.getPosition())) {
                inPlace.add(member);
            }
        }
        return inPlace;
    }

    /**
     * On a town leg, units whose ways into the streets go through the same door stack up outside it and go through
     * one after another, the nearest first (HammerGS, 2026-10-01: "when infantry are going through a door, they queue
     * up then go in"). See {@link TownLegPlanner#keepPlaceInStack}.
     */
    private List<MovePath> stackAtDoor(Entity entity, List<Entity> members, List<MovePath> paths) {
        Board board = owner.getGame().getBoard(entity);
        FormationLeg leg = formationLegs.get(entity.getUnitOrders().getFormation().map(FormationOrder::getLeaderId)
              .orElse(-1));
        if ((board == null) || (leg == null) || !leg.isTown()) {
            return paths;
        }
        Coords ownDoor = null;
        int ownStepsToDoor = 0;
        List<Entity> ahead = new ArrayList<>();
        Map<Integer, Integer> stepsToDoor = new HashMap<>();
        Map<Integer, Coords> doors = new HashMap<>();
        for (Entity member : members) {
            Coords spot = leg.spots().get(member.getId());
            Coords position = member.getPosition();
            if ((spot == null) || (position == null) || position.equals(spot)) {
                continue;
            }
            List<Coords> way = TownLegPlanner.wayTo(board, position, spot, hex -> distances.routeCostFrom(member, spot, hex));
            Coords door = TownLegPlanner.doorOn(board, way);
            if (door != null) {
                doors.put(member.getId(), door);
                stepsToDoor.put(member.getId(), way.indexOf(door));
            }
        }
        ownDoor = doors.get(entity.getId());
        if (ownDoor == null) {
            return paths;
        }
        ownStepsToDoor = stepsToDoor.get(entity.getId());
        for (Entity member : members) {
            Integer memberSteps = stepsToDoor.get(member.getId());
            if ((member.getId() == entity.getId()) || !ownDoor.equals(doors.get(member.getId()))) {
                continue;
            }
            boolean isNearer = (memberSteps < ownStepsToDoor)
                  || ((memberSteps == ownStepsToDoor) && (member.getId() < entity.getId()));
            if (isNearer) {
                ahead.add(member);
            }
        }
        if (ahead.isEmpty()) {
            if (stepsToDoor.values().size() > 1) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: TOWN_DOOR - first through the door at {}",
                      entity.getDisplayName(), entity.getId(), currentRound(), ownDoor.getBoardNum());
            }
            return paths;
        }
        List<MovePath> kept = TownLegPlanner.keepPlaceInStack(paths, ownDoor, ahead.size());
        LOGGER.info("[BotOrders] {} (ID {}) round {}: TOWN_DOOR - number {} in the stack for the door at {}, behind {}; "
                    + "{} of {} moves kept", entity.getDisplayName(), entity.getId(), currentRound(), ahead.size() + 1,
              ownDoor.getBoardNum(), ahead.get(ahead.size() - 1).getShortName(), kept.size(), paths.size());
        return kept;
    }

    /**
     * On a town leg, keeps each unit within its leash of a friend (see {@link TownLegPlanner#keepWithinReach}).
     */
    private List<MovePath> keepFriendsInReach(Entity entity, List<Entity> members, List<MovePath> paths) {
        List<Coords> friends = new ArrayList<>();
        for (Entity member : members) {
            if ((member.getId() != entity.getId()) && (member.getPosition() != null)) {
                friends.add(member.getPosition());
            }
        }
        int leash = TownLegPlanner.leash(entity);
        List<MovePath> kept = TownLegPlanner.keepWithinReach(entity, paths, friends, leash);
        LOGGER.info("[BotOrders] {} (ID {}) round {}: TOWN_LEASH - within {} hexes of a friend, {} of {} moves kept",
              entity.getDisplayName(), entity.getId(), currentRound(), leash, kept.size(), paths.size());
        return kept;
    }

    /**
     * A unit its lance no longer counts: one that cannot move - shut down, crew unconscious, stuck, or with no
     * movement left - or one that has lain prone {@link #PRONE_ROUNDS_BEFORE_DROPPED} rounds. It holds and fights
     * where it is while the others close up and move on; once it can move again it rejoins (HammerGS, 2026-09-27).
     *
     * @param unit a unit in a formation
     *
     * @return {@code true} if the unit is out of action
     */
    boolean isOutOfAction(Entity unit) {
        if (unit.isImmobile() || unit.isStuck() || unit.isPermanentlyImmobilized(false)
              || ((unit.getWalkMP() <= 0) && (unit.getJumpMP() <= 0))) {
            return true;
        }
        Integer proneSince = proneSinceRounds.get(unit.getId());
        return unit.isProne() && (proneSince != null)
              && ((currentRound() - proneSince) >= PRONE_ROUNDS_BEFORE_DROPPED);
    }

    /**
     * A unit that has gone {@link #ROUNDS_WITHOUT_PROGRESS} rounds without getting any closer to its slot - wading,
     * blocked, or going back and forth. It keeps its place in the shape and follows on, but its lance no longer waits
     * for it or keeps to its pace; it rejoins once it reaches its slot (HammerGS, 2026-09-27).
     *
     * @param unit a unit in a formation
     *
     * @return {@code true} if the lance has stopped waiting for the unit
     */
    boolean isFallingBehind(Entity unit) {
        SlotProgress progress = slotProgress.get(unit.getId());
        return (progress != null) && ((currentRound() - progress.sinceRound()) >= ROUNDS_WITHOUT_PROGRESS);
    }

    private static int placeOf(Entity member) {
        return member.getUnitOrders().getFormation().map(FormationOrder::getSlot).orElse(Integer.MAX_VALUE);
    }

    /**
     * Keeps count, once the bot's units have moved, of each formation unit lying prone and of its progress toward its
     * slot, and calls on the radio a unit going out of action, one falling behind, and a new commander taking over.
     */
    private void trackFormationUnits() {
        for (Entity unit : owner.getEntitiesOwned()) {
            Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
            if (formation.isEmpty() || (unit.getPosition() == null)) {
                continue;
            }
            if (unit.isProne()) {
                proneSinceRounds.putIfAbsent(unit.getId(), currentRound());
            } else {
                proneSinceRounds.remove(unit.getId());
            }
            trackOutOfAction(unit);
            trackProgress(unit);
            trackCommand(unit, formation.get().getLeaderId());
        }
    }

    private void trackOutOfAction(Entity unit) {
        if (!isOutOfAction(unit)) {
            outOfActionUnitIds.remove(unit.getId());
            return;
        }
        if (outOfActionUnitIds.add(unit.getId())) {
            String hex = unit.getPosition().getBoardNum();
            LOGGER.info("[BotOrders] {} (ID {}) round {}: OUT_OF_ACTION at {} (immobile {}, stuck {}, prone since {}); "
                        + "the lance moves on without it", unit.getDisplayName(), unit.getId(), currentRound(), hex,
                  unit.isImmobile(), unit.isStuck(), proneSinceRounds.get(unit.getId()));
            owner.getOrdersRadio().report(unit, OrdersRadio.RadioEvent.UNIT_DOWN, unit.getShortName(), hex);
        }
    }

    private void trackProgress(Entity unit) {
        Optional<Coords> slot = getFormationSlot(unit);
        if (slot.isEmpty() || unit.getPosition().equals(slot.get())) {
            slotProgress.remove(unit.getId());
            fallingBehindUnitIds.remove(unit.getId());
            return;
        }
        int cost = distances.routeCostFrom(unit, slot.get(), unit.getPosition());
        if (cost == WaypointDistanceField.UNREACHABLE) {
            cost = unit.getPosition().distance(slot.get());
        }
        SlotProgress progress = slotProgress.get(unit.getId());
        if ((progress == null) || !progress.slot().equals(slot.get()) || (cost < progress.bestCost())) {
            // a new slot starts the count again: a leader still moving moves the slot, and a leader held back for
            // this unit holds it still, so a unit truly stuck is still found
            slotProgress.put(unit.getId(), new SlotProgress(slot.get(), cost, currentRound()));
            fallingBehindUnitIds.remove(unit.getId());
            return;
        }
        if (isFallingBehind(unit) && fallingBehindUnitIds.add(unit.getId())) {
            String hex = unit.getPosition().getBoardNum();
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FALLING_BEHIND at {} - no closer to its slot at {} since round "
                        + "{}; the lance stops waiting for it", unit.getDisplayName(), unit.getId(), currentRound(), hex,
                  slot.get().getBoardNum(), progress.sinceRound());
            owner.getOrdersRadio().report(unit, OrdersRadio.RadioEvent.FALLING_BEHIND, unit.getShortName(), hex);
        }
    }

    private void trackCommand(Entity unit, int leaderId) {
        List<Entity> members = formationMembers(unit, leaderId);
        if (members.isEmpty() || (members.get(0).getId() != unit.getId())) {
            return;
        }
        Integer previous = commandingUnitIds.put(leaderId, unit.getId());
        if ((previous != null) && (previous != unit.getId())) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: TAKES_COMMAND of the formation led by unit {}",
                  unit.getDisplayName(), unit.getId(), currentRound(), leaderId);
            owner.getOrdersRadio().report(unit, OrdersRadio.RadioEvent.COMMAND, unit.getShortName());
        }
    }

    /**
     * @return the formation's units still on the board and in action, any owner on the same side, in slot order; the
     *       first is the acting leader, which is the original leader while it is in action, then the
     *       second-in-command, then the next in line. A player's unit being followed leads without a formation order
     *       of its own.
     */
    List<Entity> formationMembers(Entity entity, int leaderId) {
        List<Entity> members = new ArrayList<>();
        for (Entity candidate : owner.getGame().getEntitiesVector()) {
            Optional<FormationOrder> candidateFormation = candidate.getUnitOrders().getFormation();
            if (candidateFormation.isEmpty() || !candidateFormation.get().sharesLeader(leaderId)) {
                continue;
            }
            if ((candidate.getPosition() == null) || candidate.isDestroyed() || candidate.isDoomed()
                  || candidate.getOwner().isEnemyOf(entity.getOwner()) || isOutOfAction(candidate)) {
                continue;
            }
            members.add(candidate);
        }
        members.sort(Comparator.comparingInt(UnitOrdersFollower::placeOf));
        if (!members.isEmpty() && (members.get(0).getId() != leaderId)) {
            // the commander is lost: as in a tank platoon, the second-in-command takes over, and failing that the
            // next in line (HammerGS, 2026-09-27)
            for (Entity member : members) {
                if (placeOf(member) == SECOND_IN_COMMAND_PLACE) {
                    members.remove(member);
                    members.add(0, member);
                    break;
                }
            }
        }
        Entity leader = owner.getGame().getEntity(leaderId);
        if (isFollowablePlayerUnit(leader, entity) && !members.contains(leader)) {
            members.add(0, leader);
        }
        return members;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is ordered to follow a player's unit: its formation leader belongs to a human
     *       player on its side and is on the board
     */
    public boolean isFollowingPlayerUnit(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || (formation.get().getLeaderId() == entity.getId())) {
            return false;
        }
        return isFollowablePlayerUnit(owner.getGame().getEntity(formation.get().getLeaderId()), entity);
    }

    /**
     * @return {@code true} if the leader is a human player's unit on the follower's side, alive and on the board; a
     *       bot's unit leads only while it has a formation order, so a leader that left its formation does not
     */
    private static boolean isFollowablePlayerUnit(@Nullable Entity leader, Entity follower) {
        return (leader != null) && (leader.getPosition() != null) && !leader.isDestroyed() && !leader.isDoomed()
              && (leader.getOwner() != null) && !leader.getOwner().isBot()
              && !leader.getOwner().isEnemyOf(follower.getOwner());
    }

    /**
     * @param entity a unit of the bot
     *
     * @return how close counts as arrived: 0 for a formation slot and a waypoint set to hold, which must be reached
     *       exactly; for a formation's leader, its flag (see {@link #flagRadius}); else
     *       {@link Princess#DISTANCE_TO_WAYPOINT}
     */
    int arrivalRadius(Entity entity) {
        if (getFormationSlot(entity).isPresent() || isHeadingForHold(entity)) {
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
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
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
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
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
        if (isWaitingForFormation(entity)) {
            return "waiting at " + entity.getPosition().getBoardNum() + " for the formation";
        }
        if (phaseLines.isWaitingAtPhaseLine(entity)) {
            return "holding at " + PhaseLine.display(entity.getUnitOrders().getWaypointOrder(0).getPhaseLine())
                  + " for the other lances on it";
        }
        return "holding the end of its route at " + entity.getPosition().getBoardNum();
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
        if ((members.size() < 2) || !members.contains(entity)) {
            return paths;
        }
        Entity leader = members.get(0);
        if (FireReaction.isBrokenToFight(leader)) {
            LOGGER.debug("[BotOrders] {} (ID {}) round {}: the lance is fighting - not paced",
                  entity.getDisplayName(), entity.getId(), currentRound());
            return paths;
        }
        int paceLimit = paceMovementPoints(entity, formation.get().getPace());
        boolean isTownLeg = isOnTownLeg(entity);
        boolean isHeldToSlowest = formation.get().isKeepTogether() && (leader.getId() == entity.getId())
              && !isTownLeg && !isAssembling(entity);
        if ((formation.get().getPace() == FormationPace.WALK) && !isHeldToSlowest && isLagging(entity)) {
            // at a walk, a unit more than a turn's walk from its place may run or jump to catch up (HammerGS,
            // 2026-10-01); one in its place, or nearly, keeps to the walk
            paceLimit = Math.max(entity.getRunMP(), entity.getJumpMP());
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_CATCH_UP - more than a turn's walk from its place; "
                        + "may run or jump, up to {} MP", entity.getDisplayName(), entity.getId(), currentRound(),
                  paceLimit);
        }
        if (isHeldToSlowest) {
            // a formation keeping together advances no faster than its slowest unit can follow
            for (Entity member : members) {
                if (!isFallingBehind(member)) {
                    paceLimit = Math.min(paceLimit, paceMovementPoints(member, formation.get().getPace()));
                }
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
        List<MovePath> keptPaths = pacedPaths.isEmpty() ? paths : pacedPaths;
        keptPaths = withoutBuildingCollapses(entity, keptPaths);
        if (isTownLeg) {
            return keepFriendsInReach(entity, members, stackAtDoor(entity, members, keptPaths));
        }
        return isHeldToSlowest ? keepLastUnitInReach(entity, keptPaths, paceLimit) : keptPaths;
    }

    /**
     * Drops the moves that walk the unit into a building too weak to hold it, while any other move is left. Narrowed
     * to a few moves by the formation's pace, the bot could be left choosing between standing still facing the wrong
     * way and stepping into a light building, and chose the building (HammerGS's playtest, 2026-09-27: an 85-ton
     * Stalker into the CF 15 building at 1119, which came down and left it prone).
     *
     * @return the moves that bring no building down, or all of them when every move would
     */
    private List<MovePath> withoutBuildingCollapses(Entity entity, List<MovePath> paths) {
        Board board = owner.getGame().getBoard(entity);
        if ((board == null) || entity.isAirborne() || entity.hasETypeFlag(Entity.ETYPE_VTOL)) {
            return paths;
        }
        List<MovePath> safePaths = new ArrayList<>();
        for (MovePath path : paths) {
            if (!bringsDownBuilding(entity, board, path)) {
                safePaths.add(path);
            }
        }
        if (safePaths.isEmpty() || (safePaths.size() == paths.size())) {
            return paths;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: BUILDING_AVOIDED - {} of {} moves would bring a building down "
                    + "under it; dropped", entity.getDisplayName(), entity.getId(), currentRound(),
              paths.size() - safePaths.size(), paths.size());
        return safePaths;
    }

    private static boolean bringsDownBuilding(Entity entity, Board board, MovePath path) {
        Coords start = entity.getPosition();
        for (MoveStep step : path.getStepVector()) {
            Coords stepPosition = step.getPosition();
            // a unit already standing in a building is not brought down by leaving it
            if ((stepPosition != null) && !stepPosition.equals(start)
                  && WaypointDistanceField.wouldBringDownBuilding(entity, board, stepPosition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps a leader whose formation keeps together from reaching its waypoint more than a turn before the last of its
     * units can reach its slot there. Holding the leader to the slowest unit's movement points is not enough on its
     * own: when the shape changes, a unit's new slot can lie well off to the side, and in rougher ground, so it has
     * further to go than the leader (HammerGS's playtest, 2026-09-27: a Longbow walking 3 reached its Line slot six
     * turns after the Grasshopper leading it).
     *
     * @param leader      the formation's leader, about to move
     * @param paths       its moves within the pace
     * @param leaderPace  the movement points it may spend a turn
     *
     * @return the moves that leave it at least as many turns from its waypoint as the last unit needs, less one, but
     *       never more than it is now: a leader too far ahead waits where it stands rather than walking back
     */
    private List<MovePath> keepLastUnitInReach(Entity leader, List<MovePath> paths, int leaderPace) {
        Optional<Coords> waypoint = leader.getUnitOrders().getNextWaypoint();
        if (waypoint.isEmpty() || paths.isEmpty()) {
            return paths;
        }
        int lastUnitTurns = estimatedAssemblyTurns(leader);
        // the others may already have moved this turn, so the leader can be one turn ahead of the last of them; a
        // leader already too far ahead is not sent back, only held where it is
        int turnsNow = turnsToWaypoint(leader, waypoint.get(), leader.getPosition(), leaderPace);
        int turnsToKeep = Math.min(lastUnitTurns - 1, turnsNow);
        if (turnsToKeep <= 0) {
            return paths;
        }
        // held back, the leader stays on its hex, turning at most: any move that only kept its distance let it wander
        // sideways, as a Grasshopper did from 1906 to 1604 and back (HammerGS's playtest, 2026-09-27). Allowed to
        // close in, it may go no further back than it is now
        boolean isHeldInPlace = turnsToKeep >= turnsNow;
        Coords position = leader.getPosition();
        List<MovePath> kept = new ArrayList<>();
        for (MovePath path : paths) {
            Coords end = path.getFinalCoords();
            if ((end == null) || end.equals(position)) {
                kept.add(path);
                continue;
            }
            int turnsLeft = turnsToWaypoint(leader, waypoint.get(), end, leaderPace);
            if (!isHeldInPlace && (turnsLeft >= turnsToKeep) && (turnsLeft < turnsNow)) {
                kept.add(path);
            }
        }
        if (kept.isEmpty()) {
            return paths;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_MARCH - the last unit needs {} turn(s) to its slot at "
                    + "{}; keeping {} turn(s) out{}, {} of {} moves kept", leader.getDisplayName(), leader.getId(),
              currentRound(), lastUnitTurns, waypoint.get().getBoardNum(), turnsToKeep,
              isHeldInPlace ? ", holding in place" : "", kept.size(), paths.size());
        return kept;
    }

    /**
     * @return the turns the unit needs from the position to the waypoint by the way it can really go, at the given
     *       movement points a turn; by the straight line where no route is known
     */
    private int turnsToWaypoint(Entity unit, Coords waypoint, Coords position, int movementPointsPerTurn) {
        int cost = distances.routeCostFrom(unit, waypoint, position);
        if (cost == WaypointDistanceField.UNREACHABLE) {
            cost = position.distance(waypoint);
        }
        int perTurn = Math.max(1, movementPointsPerTurn);
        return (cost + perTurn - 1) / perTurn;
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
     * See {@code ConvoyEscortFollower.escortFacing}.
     */
    OptionalInt escortFacing(Entity entity) {
        return convoyEscorts.escortFacing(entity);
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
     * See {@code ConvoyEscortFollower.isEscorting}.
     */
    public boolean isEscorting(Entity entity) {
        return convoyEscorts.isEscorting(entity);
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
}
