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

import megamek.client.bot.Messages;
import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
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

    /** How far off its slot a formation unit may stand when the slot itself is blocked. */
    static final int FORMATION_SLACK = 1;

    /** An enemy this close to a formation's leader counts as contact. */
    static final int CONTACT_RANGE = 12;

    private final Princess owner;
    private final Set<Integer> arrivedUnitIds = new HashSet<>();
    private final Map<String, WaypointDistanceField> distanceFields = new HashMap<>();
    private int distanceFieldsRound = -1;
    private final Map<Integer, SlotChoice> slotChoices = new HashMap<>();

    /**
     * A formation unit's worked-out slot, kept until the round or its leader's position changes.
     */
    private record SlotChoice(int round, Coords leaderPosition, boolean hasLeaderMoved, @Nullable Coords slot) {}

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
        syncFollowerRoutes();
    }


    /**
     * The hex a formation unit should head for this move: its slot beside the formation's leader. Empty for a unit
     * not in a formation, for the unit leading it, and while a formation that breaks on contact has an enemy near.
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
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
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
        if ((formation.get().getContactRule() == ContactRule.BREAK) && isEnemyNear(leader)) {
            LOGGER.debug("[BotOrders] {} (ID {}): formation broken - enemy within {} of {}", entity.getDisplayName(),
                  entity.getId(), CONTACT_RANGE, leader.getDisplayName());
            return Optional.empty();
        }
        SlotChoice cached = slotChoices.get(entity.getId());
        if ((cached != null) && (cached.round() == currentRound())
              && cached.leaderPosition().equals(leader.getPosition()) && (cached.hasLeaderMoved() == leader.isDone())) {
            return Optional.ofNullable(cached.slot());
        }
        Coords slot = chooseSlot(entity, leader, members, formation.get(), members.indexOf(entity));
        slotChoices.put(entity.getId(), new SlotChoice(currentRound(), leader.getPosition(), leader.isDone(), slot));
        return Optional.ofNullable(slot);
    }

    private @Nullable Coords chooseSlot(Entity entity, Entity leader, List<Entity> members, FormationOrder formation,
          int slotIndex) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return null;
        }
        Optional<Coords> leaderWaypoint = leader.getUnitOrders().getNextWaypoint()
              .filter(waypoint -> !waypoint.equals(leader.getPosition()));
        int heading = leaderWaypoint.map(leader.getPosition()::direction).orElse(leader.getFacing());
        Coords leaderPosition = projectedLeaderPosition(leader, leaderWaypoint, heading, board,
              paceLimit(members, formation.getPace()));
        Coords ideal = FormationPlanner.idealSlot(leaderPosition, heading, formation.getShape(),
              formation.getSpacing(), slotIndex);
        Coords settled = settle(entity, board, leaderPosition, ideal);
        if (settled != null) {
            LOGGER.debug("[BotOrders] {} (ID {}): {} slot {} at {}", entity.getDisplayName(), entity.getId(),
                  formation.getShape(), slotIndex, settled.getBoardNum());
            return settled;
        }
        Coords columnSlot = settle(entity, board, leaderPosition, FormationPlanner.idealSlot(leaderPosition, heading,
              FormationShape.COLUMN, formation.getSpacing(), slotIndex));
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_FOLD - {} slot {} blocked, folding to column at {}",
              entity.getDisplayName(), entity.getId(), currentRound(), formation.getShape(), slotIndex,
              (columnSlot == null) ? "the leader" : columnSlot.getBoardNum());
        return (columnSlot == null) ? leaderPosition : columnSlot;
    }

    /**
     * @return the ideal hex if the unit can stand there, else the best hex within {@link #FORMATION_SLACK} of it,
     *       else {@code null}
     */
    private static @Nullable Coords settle(Entity entity, Board board, Coords leaderPosition, Coords ideal) {
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

    private static boolean isUsableSlot(Entity entity, Board board, Coords leaderPosition, Coords slot) {
        return board.contains(slot) && !slot.equals(leaderPosition) && !entity.isLocationProhibited(slot)
              && FormationSide.sameSide(board, leaderPosition, slot);
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

    private boolean isEnemyNear(Entity leader) {
        for (Entity enemy : owner.getEnemyEntities()) {
            if ((enemy.getPosition() != null) && (enemy.getBoardId() == leader.getBoardId())
                  && (enemy.getPosition().distance(leader.getPosition()) <= CONTACT_RANGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return how close counts as arrived: {@link #FORMATION_SLACK} for a formation slot, else
     *       {@link Princess#DISTANCE_TO_WAYPOINT}
     */
    int arrivalRadius(Entity entity) {
        return getFormationSlot(entity).isPresent() ? FORMATION_SLACK : Princess.DISTANCE_TO_WAYPOINT;
    }

    /**
     * Where the leader will stand at the end of this movement phase. Units move one at a time, so a follower may move
     * before its leader; lining up on the leader's current hex would leave it a full move behind once the leader goes.
     * Until the leader has moved, it is expected to advance toward its next waypoint by the formation's pace; after
     * that, its actual hex is used.
     *
     * @return the hex the formation lines up on
     */
    private Coords projectedLeaderPosition(Entity leader, Optional<Coords> leaderWaypoint, int heading, Board board,
          int paceLimit) {
        Coords leaderPosition = leader.getPosition();
        if (leader.isDone() || leaderWaypoint.isEmpty() || !owner.getGame().getPhase().isMovement()) {
            return leaderPosition;
        }
        int expectedAdvance = Math.min(paceLimit, leaderPosition.distance(leaderWaypoint.get()));
        Coords projected = leaderPosition.translated(heading, expectedAdvance);
        return board.contains(projected) ? projected : leaderPosition;
    }

    /**
     * @return the movement points the slowest member walks, or runs at a Run pace
     */
    private static int paceLimit(List<Entity> members, FormationPace pace) {
        boolean isRunning = pace == FormationPace.RUN;
        int paceLimit = Integer.MAX_VALUE;
        for (Entity member : members) {
            paceLimit = Math.min(paceLimit, isRunning ? member.getRunMP() : member.getWalkMP());
        }
        return paceLimit;
    }

    /**
     * Keeps a formation's leader to the pace of its slowest unit, so the formation stays together: its moves may not
     * use more movement points than that unit walks, or runs at a Run pace. A leader with no move within the pace
     * keeps every move.
     *
     * @param entity the unit about to move
     * @param paths  its candidate moves
     *
     * @return the moves within the formation's pace, or all of them when the unit leads no formation
     */
    List<MovePath> limitToFormationPace(Entity entity, List<MovePath> paths) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty()) {
            return paths;
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        if ((members.size() < 2) || (members.get(0).getId() != entity.getId())) {
            return paths;
        }
        int paceLimit = paceLimit(members, formation.get().getPace());
        List<MovePath> pacedPaths = new ArrayList<>();
        for (MovePath path : paths) {
            if (path.getMpUsed() <= paceLimit) {
                pacedPaths.add(path);
            }
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_PACE - {} {} MP, {} of {} moves kept",
              entity.getDisplayName(), entity.getId(), currentRound(), formation.get().getPace(), paceLimit,
              pacedPaths.size(), paths.size());
        return pacedPaths.isEmpty() ? paths : pacedPaths;
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
