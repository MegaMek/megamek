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
import java.util.List;
import java.util.Optional;

import megamek.common.board.Coords;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.orders.FightState;
import megamek.common.orders.LanceRole;
import megamek.common.orders.UnitOrderAction;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;

/**
 * The behaviour tracker for a bot that follows unit orders ({@link Princess#followsUnitOrders()}, which is CASPAR).
 *
 * <p>Waypoints are not held in the bot's memory: they are part of the unit orders stored on each unit, so they are
 * saved with the game and every client on the bot's side sees them. Changes go through
 * {@link UnitOrdersFollower}, which also sends them to the server.</p>
 */
public class UnitOrdersBehavior extends UnitBehavior {
    private static final MMLogger LOGGER = MMLogger.create(UnitOrdersBehavior.class);

    // the active waypoint of the unit whose moves are being ranked, worked out once for all of them
    private final OrderSnapshot rankingSnapshot = new OrderSnapshot();
    private Princess rankedBy;

    /**
     * Worker function that calculates a unit's desired behavior.
     *
     * <p>Order of precedence: a gamemaster's order to withdraw; the player's edge order for the unit; the unit's own
     * Forced Withdrawal, unless the player has given it, or the formation leader it keeps up with, a route; the
     * bot-wide flee order; the unit's route, or its slot round its routed leader; and then the bot's own judgement. A
     * Pause or Stop order is handled before this, by holding the unit in place.</p>
     */
    @Override
    protected BehaviorType calculateUnitBehavior(Entity entity, Princess owner) {
        // the same test as every other withdrawal decision, so crew-crippled Meks and gamemaster orders count too
        boolean isWithdrawing = owner.getForcedWithdrawalTracker().isWithdrawing(entity);
        boolean isFleeOrdered = isFleeOrdered(owner);
        boolean isGamemasterWithdraw = entity.getForcedWithdrawalOrder() == ForcedWithdrawalOrder.WITHDRAW;
        Optional<CardinalEdge> orderedEdge = owner.getUnitOrdersFollower().getOrderedEdge(entity);

        if (isGamemasterWithdraw) {
            return edgeBehavior(entity, owner, "GAMEMASTER_WITHDRAW", BehaviorType.ForcedWithdrawal);
        } else if (orderedEdge.isPresent()) {
            String rule = owner.getUnitOrdersFollower().isOrderedToExit(entity) ? "EXIT_BY_EDGE" : "MOVE_TO_EDGE";
            return edgeBehavior(entity, owner, isWithdrawing ? rule + " over FORCED_WITHDRAWAL" : rule,
                  BehaviorType.MoveToDestination);
        } else if (isWithdrawing && !isFollowingOrdersOverWithdrawal(entity, owner)) {
            return edgeBehavior(entity, owner, "FORCED_WITHDRAWAL", BehaviorType.ForcedWithdrawal);
        } else if (isFleeOrdered) {
            return edgeBehavior(entity, owner, "FLEE_ORDER", BehaviorType.MoveToDestination);
        } else if (entity.getUnitOrders().getFightState().orElse(null) == FightState.FIGHTING) {
            // hit on its way with Break and fight set: the lance fights its attackers; the route waits
            logDecision(entity, "UNDER_FIRE", "breaking off the route to fight");
            return BehaviorType.Engaged;
        } else if (owner.getUnitOrdersFollower().getEscortPlace(entity).isPresent()) {
            if (owner.getUnitOrdersFollower().isEscortFightingBriefly(entity)) {
                // Break and fight, leaving Briefly: the escort engages the threat, its moves held near its place
                // (QA playtest, #9524: escorts that only shifted their places never fought)
                logDecision(entity, "ESCORT_FIGHT", "engaging, within " + LanceRole.BRIEF_CHASE_HEXES
                      + " hexes of its place at " + owner.getUnitOrdersFollower().getEscortPlace(entity).get()
                      .getBoardNum());
                return BehaviorType.Engaged;
            }
            // an escort keeps its place round its convoy; it has no route of its own
            logDecision(entity, "ESCORT", "head " + owner.getUnitOrdersFollower().getEscortPlace(entity).get()
                  .getBoardNum());
            return BehaviorType.MoveToDestination;
        } else if (entity.getUnitOrders().hasRoute()) {
            while (getWaypointForEntity(entity).isPresent()
                  && !owner.getUnitOrdersFollower().canReach(entity, getWaypointForEntity(entity).get())) {
                owner.getUnitOrdersFollower().dropUnreachableWaypoint(entity);
            }
            boolean isConvoy = owner.getUnitOrdersFollower().isConvoy(entity);
            if (owner.getUnitOrdersFollower().isAtRouteEnd(entity) && !isConvoy
                  && owner.getUnitOrdersFollower().isEnemyInRange(entity)) {
                // holding the end of its route, with an enemy in range: fight, then the route end pulls it back
                logDecision(entity, "ROUTE_END", "engaging, will return to "
                      + getWaypointForEntity(entity).get().getBoardNum());
                return BehaviorType.Engaged;
            }
            if (getWaypointForEntity(entity).isPresent()) {
                String waypoint = getWaypointForEntity(entity).get().getBoardNum();
                String priority = entity.getUnitOrders().getPriority().name();
                logDecision(entity, "PLAYER_ROUTE", isWithdrawing
                      ? "head " + waypoint + " (" + priority + ") over FORCED_WITHDRAWAL"
                      : "head " + waypoint + " (" + priority + ")");
                return BehaviorType.MoveToDestination;
            }

            logDecision(entity, "PLAYER_ROUTE", "no reachable waypoint left");
            return BehaviorType.NoPathToDestination;
        } else if (owner.getUnitOrdersFollower().isFollowingPlayerUnit(entity)
              || owner.getUnitOrdersFollower().routedFormationLeader(entity).isPresent()) {
            // a follower with no route of its own heads for its slot round its leader, a player's unit or a bot's
            // unit on a route; it was left with no orders at all (QA playtest, #9524)
            String rule = owner.getUnitOrdersFollower().isFollowingPlayerUnit(entity) ? "FOLLOW_UNIT"
                  : "FORMATION_SLOT";
            Optional<Coords> slot = owner.getUnitOrdersFollower().getFormationSlot(entity);
            if (slot.isPresent()) {
                logDecision(entity, rule, isWithdrawing ? "head " + slot.get().getBoardNum() + " over FORCED_WITHDRAWAL"
                      : "head " + slot.get().getBoardNum());
                return BehaviorType.MoveToDestination;
            }
            // the formation broke on contact: fight, and fall back in once the enemy is gone
            logDecision(entity, rule, "engaging");
            return BehaviorType.Engaged;
        } else if ((entity instanceof Mek) && ((Mek) entity).isJustMovedIntoIndustrialKillingWater()) {
            return edgeBehavior(entity, owner, "INDUSTRIAL_WATER", BehaviorType.ForcedWithdrawal);
        } else {
            // if we can't see anyone, move to contact
            if (!entity.getGame().getAllEnemyEntities(entity).hasNext()) {
                LOGGER.debug("[BotOrders] {} (ID {}): no orders, moving to contact", entity.getDisplayName(),
                      entity.getId());
                return BehaviorType.MoveToContact;
            }

            LOGGER.debug("[BotOrders] {} (ID {}): no orders, engaged", entity.getDisplayName(), entity.getId());
            return BehaviorType.Engaged;
        }
    }

    /**
     * Decides the behavior of a unit heading for its home edge, or that it has no path there.
     */
    private BehaviorType edgeBehavior(Entity entity, Princess owner, String rule, BehaviorType behaviorWithPath) {
        CardinalEdge homeEdge = owner.getHomeEdge(entity);
        if (owner.getClusterTracker().getDestinationCoords(entity, homeEdge, true).isEmpty()) {
            logDecision(entity, rule, "no path to the " + homeEdge + " edge");
            return BehaviorType.NoPathToDestination;
        }
        logDecision(entity, rule, "toward the " + homeEdge + " edge");
        return behaviorWithPath;
    }

    /**
     * Returns whether a withdrawing unit follows the player's orders instead of heading for its retreat edge.
     *
     * <p>A player's direct order outranks the unit's own Forced Withdrawal: a crippled unit sent to a hex or an edge
     * goes there (issue #9038). So does a formation follower keeping up with a leader on a route: only the leader
     * carries the route, and the follower keeps to it with its lance (QA playtest, #9524). A gamemaster's order to
     * withdraw still wins over everything, and the bot-wide flee order wins over a route, sending a withdrawing unit
     * toward the ordered edge instead.</p>
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     *
     * @return {@code true} if the unit is withdrawing under the bot's rules but has orders to follow instead
     */
    @Override
    public boolean isFollowingOrdersOverWithdrawal(Entity entity, Princess owner) {
        if (!owner.getForcedWithdrawalTracker().isWithdrawing(entity)) {
            return false;
        }
        if (entity.getForcedWithdrawalOrder() == ForcedWithdrawalOrder.WITHDRAW) {
            return false;
        }
        if (owner.getUnitOrdersFollower().getOrderedEdge(entity).isPresent()) {
            return true;
        }
        return !isFleeOrdered(owner) && (entity.getUnitOrders().hasRoute()
              || owner.getUnitOrdersFollower().isKeepingUpWithRoutedLeader(entity));
    }

    /**
     * Returns the waypoint that decides where the unit moves this phase, if a waypoint does. An edge order, a
     * bot-wide flee order and a withdrawal the player has not overridden all send the unit to an edge instead, so
     * they return empty.
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     *
     * @return the unit's current waypoint when it is following its route, otherwise empty
     */
    @Override
    public Optional<Coords> getActiveWaypoint(Entity entity, Princess owner) {
        if (rankingSnapshot.holds(entity)) {
            return rankingSnapshot.activeWaypoint();
        }
        return workOutActiveWaypoint(entity, owner);
    }

    /**
     * Works out a unit's active waypoint once before its moves are ranked, and answers with it until
     * {@link #endRanking()}: the path ranker asks for it four or five times for every move it scores.
     *
     * @param entity the unit about to have its moves ranked
     * @param owner  the bot that owns the unit
     */
    @Override
    public void beginRanking(Entity entity, Princess owner) {
        rankingSnapshot.clear();
        owner.getUnitOrdersFollower().setRanking(false);
        rankingSnapshot.take(entity, workOutActiveWaypoint(entity, owner));
        owner.getUnitOrdersFollower().setRanking(true);
        rankedBy = owner;
    }

    /** Ends the waypoint snapshot {@link #beginRanking} took, once the unit's moves are ranked. */
    @Override
    public void endRanking() {
        rankingSnapshot.clear();
        if (rankedBy != null) {
            rankedBy.getUnitOrdersFollower().setRanking(false);
            rankedBy = null;
        }
    }

    private Optional<Coords> workOutActiveWaypoint(Entity entity, Princess owner) {
        if (entity.getForcedWithdrawalOrder() == ForcedWithdrawalOrder.WITHDRAW) {
            return Optional.empty();
        }
        if (owner.getUnitOrdersFollower().getOrderedEdge(entity).isPresent() || isFleeOrdered(owner)) {
            return Optional.empty();
        }
        if (owner.getForcedWithdrawalTracker().isWithdrawing(entity)
              && !isFollowingOrdersOverWithdrawal(entity, owner)) {
            return Optional.empty();
        }
        if (entity.getUnitOrders().getFightState().orElse(null) == FightState.FIGHTING) {
            // fighting off its route: nothing pulls it back to the route until the fight is over
            return Optional.empty();
        }
        Optional<Coords> escortPlace = owner.getUnitOrdersFollower().getEscortPlace(entity);
        if (escortPlace.isPresent()) {
            // an escort fighting briefly is not pulled back to its place while it fights; the leash on its moves
            // keeps it near (QA playtest, #9524)
            return owner.getUnitOrdersFollower().isEscortFightingBriefly(entity) ? Optional.empty() : escortPlace;
        }
        if (entity.getUnitOrders().hasRoute() || owner.getUnitOrdersFollower().isFollowingPlayerUnit(entity)
              || owner.getUnitOrdersFollower().routedFormationLeader(entity).isPresent()) {
            // a formation unit heads for its slot beside the leader rather than the waypoint itself
            Optional<Coords> formationSlot = owner.getUnitOrdersFollower().getFormationSlot(entity);
            if (formationSlot.isPresent()) {
                return formationSlot;
            }
        }
        return getWaypointForEntity(entity);
    }

    /**
     * @param entity the unit
     *
     * @return the next waypoint of the unit's route, which is stored on the unit, or empty when it has none
     */
    @Override
    public Optional<Coords> getWaypointForEntity(Entity entity) {
        return entity.getUnitOrders().getNextWaypoint();
    }

    /**
     * Keeps only the waypoints the unit can reach, logging the others.
     */
    private List<Coords> reachableWaypoints(Entity entity, List<Coords> waypoints, Princess owner) {
        List<Coords> reachable = new ArrayList<>();
        for (Coords waypoint : waypoints) {
            if (!owner.getUnitOrdersFollower().canReach(entity, waypoint)) {
                // just discard any invalid waypoint
                LOGGER.info("[BotOrders] {}: waypoint {} cannot be reached; not used", entity.getDisplayName(),
                      waypoint.getBoardNum());
                continue;
            }
            reachable.add(waypoint);
        }
        return reachable;
    }

    /**
     * Adds waypoints to the end of the unit's route, dropping any the unit cannot reach. The route is stored on the
     * unit and sent to the server.
     *
     * @return how many of the given waypoints were kept; {@code 0} when none of them can be reached
     */
    @Override
    public int addEntityWaypoint(Entity entity, List<Coords> waypoints, Princess owner) {
        List<Coords> reachable = reachableWaypoints(entity, waypoints, owner);
        if (!reachable.isEmpty()) {
            owner.getUnitOrdersFollower().addToRoute(entity, reachable);
        }
        LOGGER.info("[BotOrders] {}: added {} of {} waypoints: {}", entity.getDisplayName(), reachable.size(),
              waypoints.size(), reachable);
        return reachable.size();
    }

    /**
     * Removes the last waypoint of the unit's route, good for an "undo" behavior.
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     */
    @Override
    public void removeTailWaypoint(Entity entity, Princess owner) {
        LOGGER.info("[BotOrders] {}: removing the last waypoint", entity.getDisplayName());
        owner.getUnitOrdersFollower().change(entity, UnitOrderAction.REMOVE_LAST);
    }

    /**
     * Replaces the unit's route with the given waypoints, dropping any the unit cannot reach. The route is stored on
     * the unit and sent to the server.
     *
     * @return how many of the given waypoints were kept; {@code 0} when none of them can be reached, in which case the
     *       unit's orders are left as they were
     */
    @Override
    public int setEntityWaypoints(Entity entity, List<Coords> waypoints, Princess owner) {
        List<Coords> reachable = reachableWaypoints(entity, waypoints, owner);
        if (!reachable.isEmpty()) {
            owner.getUnitOrdersFollower().setRoute(entity, reachable);
        }
        LOGGER.info("[BotOrders] {}: set {} of {} waypoints: {}", entity.getDisplayName(), reachable.size(),
              waypoints.size(), reachable);
        return reachable.size();
    }

    /**
     * Clears all of one unit's orders.
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     */
    @Override
    public void clearWaypoints(Entity entity, Princess owner) {
        LOGGER.info("[BotOrders] {}: clearing all orders", entity.getDisplayName());
        if (!entity.getUnitOrders().isEmpty()) {
            owner.getUnitOrdersFollower().change(entity, UnitOrderAction.CLEAR);
        }
    }

    /**
     * Clears the orders of every unit of the bot.
     *
     * @param owner the bot
     */
    @Override
    public void clearWaypoints(Princess owner) {
        LOGGER.info("[BotOrders] {}: clearing all orders for every unit", owner.getName());
        for (Entity entity : owner.getEntitiesOwned()) {
            clearWaypoints(entity, owner);
        }
    }
}
