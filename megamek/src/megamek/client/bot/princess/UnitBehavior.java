/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.orders.UnitOrderAction;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;

public class UnitBehavior {
    private final static MMLogger LOGGER = MMLogger.create(UnitBehavior.class);

    public enum BehaviorType {
        // this unit is under 'forced withdrawal' due to being crippled
        ForcedWithdrawal,

        // this unit will do its best to get to a destination
        MoveToDestination,

        // this unit will move either toward the nearest enemy or towards the "opposite" edge of the board
        MoveToContact,

        // this unit is engaged in battle
        Engaged,

        // this unit has no path to its destination
        NoPathToDestination
    }

    private final Map<Integer, BehaviorType> entityBehaviors = new HashMap<>();

    /**
     * Worker function that calculates a unit's desired behavior.
     *
     * <p>Order of precedence: a gamemaster's order to withdraw; the player's edge order for the unit; the unit's own
     * Forced Withdrawal, unless the player has given it a route; the bot-wide flee order; the unit's route; and then
     * the bot's own judgement. A Pause or Stop order is handled before this, by holding the unit in place.</p>
     */
    private BehaviorType calculateUnitBehavior(Entity entity, Princess owner) {
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
        } else if (entity.getUnitOrders().hasRoute()) {
            while (getWaypointForEntity(entity).isPresent()
                  && !owner.getUnitOrdersFollower().canReach(entity, getWaypointForEntity(entity).get())) {
                owner.getUnitOrdersFollower().dropUnreachableWaypoint(entity);
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
     * Logs which rule decided a unit's movement this phase, so a playtest log shows why a unit did or did not follow
     * the player's orders.
     */
    private static void logDecision(Entity entity, String rule, String detail) {
        LOGGER.info("[BotOrders] {} (ID {}) round {}: {} - {}", entity.getDisplayName(), entity.getId(),
              entity.getGame().getCurrentRound(), rule, detail);
    }

    /**
     * Returns whether the bot has a standing order to flee toward an edge, given from the Bot Commands panel or the
     * {@code flee} chat command.
     *
     * @param owner the bot
     *
     * @return {@code true} if every unit of the bot is ordered toward the bot's destination edge
     */
    public static boolean isFleeOrdered(Princess owner) {
        BehaviorSettings botSettings = owner.getBehaviorSettings();
        return botSettings.shouldAutoFlee() && (botSettings.getDestinationEdge() != CardinalEdge.NONE);
    }

    /**
     * Returns whether a withdrawing unit follows the player's orders instead of heading for its retreat edge.
     *
     * <p>A player's direct order outranks the unit's own Forced Withdrawal: a crippled unit sent to a hex or an edge
     * goes there (issue #9038). A gamemaster's order to withdraw still wins over everything, and the bot-wide flee
     * order wins over a route, sending a withdrawing unit toward the ordered edge instead.</p>
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     *
     * @return {@code true} if the unit is withdrawing under the bot's rules but has orders to follow instead
     */
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
        return !isFleeOrdered(owner) && entity.getUnitOrders().hasRoute();
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
    public Optional<Coords> getActiveWaypoint(Entity entity, Princess owner) {
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
        return getWaypointForEntity(entity);
    }

    /**
     * Gets (and calculates, if necessary), the behavior type for the given entity.
     */
    public BehaviorType getBehaviorType(Entity entity, Princess owner) {
        if (!entityBehaviors.containsKey(entity.getId())) {
            entityBehaviors.put(entity.getId(), calculateUnitBehavior(entity, owner));
        }

        return entityBehaviors.get(entity.getId());
    }

    /**
     * The behavior already worked out for this unit, without working one out if none has been.
     *
     * <p>For logging and analysis only. {@link #getBehaviorType} computes and caches on a miss, and what it
     * computes depends on where everything is standing at the time - so asking early, for a unit that has not
     * moved yet, would pin an answer the bot would otherwise have reached later with better information.</p>
     *
     * @param entity the unit to look up
     *
     * @return the cached behavior, or {@code null} if this unit has not been evaluated this turn
     */
    public @Nullable BehaviorType getCachedBehaviorType(Entity entity) {
        return entityBehaviors.get(entity.getId());
    }

    public void overrideBehaviorType(Entity entity, BehaviorType behaviorType) {
        entityBehaviors.put(entity.getId(), behaviorType);
    }

    /**
     * @param entity the unit
     *
     * @return the next waypoint of the unit's route, which is stored on the unit, or empty when it has none
     */
    public Optional<Coords> getWaypointForEntity(Entity entity) {
        return entity.getUnitOrders().getNextWaypoint();
    }

    public boolean isDestinationValidForEntity(Entity entity, Coords destination, Princess owner) {
        var value = owner.getClusterTracker().getDestinationCoords(entity, destination, true).isEmpty();
        LOGGER.debug("Checking if destination is valid for entity {}: {} -> {}", entity.getId(), destination, value);
        return value;
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
    public int addEntityWaypoint(Entity entity, List<Coords> waypoints, Princess owner) {
        List<Coords> reachable = reachableWaypoints(entity, waypoints, owner);
        if (!reachable.isEmpty()) {
            owner.getUnitOrdersFollower().addToRoute(entity, reachable);
        }
        LOGGER.info("[BotOrders] {}: added {} of {} waypoints: {}", entity.getDisplayName(), reachable.size(),
              waypoints.size(), reachable);
        return reachable.size();
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean addEntityWaypoint(Entity entity, Coords destination, Princess owner) {
        return addEntityWaypoint(entity, List.of(destination), owner) > 0;
    }

    /**
     * Removes the last waypoint of the unit's route, good for an "undo" behavior.
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     */
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
    public void clearWaypoints(Princess owner) {
        LOGGER.info("[BotOrders] {}: clearing all orders for every unit", owner.getName());
        for (Entity entity : owner.getEntitiesOwned()) {
            clearWaypoints(entity, owner);
        }
    }

    /**
     * Clears the entity behavior cache, should be done at the start of each movement phase
     */
    public void clear() {
        entityBehaviors.clear();
    }
}
