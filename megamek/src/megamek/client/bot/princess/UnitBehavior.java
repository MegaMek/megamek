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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.ForcedWithdrawalOrder;
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
    private final Map<Integer, Deque<Coords>> entityWaypoints = new HashMap<>();

    /**
     * Worker function that calculates a unit's desired behavior
     */
    private BehaviorType calculateUnitBehavior(Entity entity, Princess owner) {
        // the same test as every other withdrawal decision, so crew-crippled Meks and gamemaster orders count too
        boolean isWithdrawing = owner.getForcedWithdrawalTracker().isWithdrawing(entity);
        boolean isFleeOrdered = isFleeOrdered(owner);

        if (isWithdrawing && !isFollowingWaypointOverWithdrawal(entity, owner)) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                logDecision(entity, "FORCED_WITHDRAWAL", "no path to the " + owner.getHomeEdge(entity) + " edge");
                return BehaviorType.NoPathToDestination;
            }

            logDecision(entity, "FORCED_WITHDRAWAL", "toward the " + owner.getHomeEdge(entity) + " edge");
            return BehaviorType.ForcedWithdrawal;
        } else if (isFleeOrdered) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                logDecision(entity, "FLEE_ORDER", "no path to the " + owner.getHomeEdge(entity) + " edge");
                return BehaviorType.NoPathToDestination;
            }

            logDecision(entity, "FLEE_ORDER", "toward the " + owner.getHomeEdge(entity) + " edge");
            return BehaviorType.MoveToDestination;
        } else if (entityWaypoints.containsKey(entity.getId()) && getWaypointForEntity(entity).isPresent()) {
            while (getWaypointForEntity(entity).isPresent() &&
                  owner.getClusterTracker()
                        .getDestinationCoords(entity, getWaypointForEntity(entity).get(), true)
                        .isEmpty()) {
                LOGGER.info("[BotOrders] {}: waypoint {} cannot be reached; dropping it",
                      entity.getDisplayName(), getWaypointForEntity(entity).get().toFriendlyString());
                removeHeadWaypoint(entity);
            }
            if (getWaypointForEntity(entity).isPresent()) {
                String waypoint = getWaypointForEntity(entity).get().toFriendlyString();
                logDecision(entity, "PLAYER_WAYPOINT",
                      isWithdrawing ? "head " + waypoint + " over FORCED_WITHDRAWAL" : "head " + waypoint);
                return BehaviorType.MoveToDestination;
            }

            logDecision(entity, "PLAYER_WAYPOINT", "no reachable waypoint left");
            return BehaviorType.NoPathToDestination;
        } else if ((entity instanceof Mek) && ((Mek) entity).isJustMovedIntoIndustrialKillingWater()) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                logDecision(entity, "INDUSTRIAL_WATER", "no path to the " + owner.getHomeEdge(entity) + " edge");
                return BehaviorType.NoPathToDestination;
            }

            logDecision(entity, "INDUSTRIAL_WATER", "toward the " + owner.getHomeEdge(entity) + " edge");
            return BehaviorType.ForcedWithdrawal;
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
     * Returns whether a withdrawing unit follows the player's waypoints instead of heading for its retreat edge.
     *
     * <p>A player's direct order outranks the unit's own Forced Withdrawal: a crippled unit sent to a hex goes there
     * (issue #9038). Two orders still win over the waypoints: a gamemaster's order to withdraw, and the bot-wide flee
     * order, which a withdrawing unit follows toward the ordered edge.</p>
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     *
     * @return {@code true} if the unit is withdrawing under the bot's rules but has a waypoint to follow instead
     */
    public boolean isFollowingWaypointOverWithdrawal(Entity entity, Princess owner) {
        if (!owner.getForcedWithdrawalTracker().isWithdrawing(entity)) {
            return false;
        }
        if (entity.getForcedWithdrawalOrder() == ForcedWithdrawalOrder.WITHDRAW) {
            return false;
        }
        if (isFleeOrdered(owner)) {
            return false;
        }
        Deque<Coords> waypoints = entityWaypoints.get(entity.getId());
        return (waypoints != null) && !waypoints.isEmpty();
    }

    /**
     * Returns the waypoint that decides where the unit moves this phase, if a waypoint does. A bot-wide flee order and
     * a withdrawal the player has not overridden both send the unit to an edge instead, so they return empty.
     *
     * @param entity the unit
     * @param owner  the bot that owns the unit
     *
     * @return the unit's current waypoint when it is following its waypoints, otherwise empty
     */
    public Optional<Coords> getActiveWaypoint(Entity entity, Princess owner) {
        if (isFleeOrdered(owner)) {
            return Optional.empty();
        }
        if (owner.getForcedWithdrawalTracker().isWithdrawing(entity)
              && !isFollowingWaypointOverWithdrawal(entity, owner)) {
            return Optional.empty();
        }
        Deque<Coords> waypoints = entityWaypoints.get(entity.getId());
        if (waypoints == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(waypoints.peek());
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

    public Optional<Coords> getWaypointForEntity(Entity entity) {
        return Optional.ofNullable(entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).peek());
    }

    public boolean isDestinationValidForEntity(Entity entity, Coords destination, Princess owner) {
        var value = owner.getClusterTracker().getDestinationCoords(entity, destination, true).isEmpty();
        LOGGER.debug("Checking if destination is valid for entity {}: {} -> {}", entity.getId(), destination, value);
        return value;
    }

    /**
     * Adds waypoints to the end of the unit's list, dropping any the unit cannot reach.
     *
     * @return how many of the given waypoints were kept; {@code 0} when none of them can be reached
     */
    public int addEntityWaypoint(Entity entity, List<Coords> waypoints, Princess owner) {
        var coords = new ArrayList<Coords>();
        for (var waypoint : waypoints) {
            if (isDestinationValidForEntity(entity, waypoint, owner)) {
                // just discard any invalid waypoint
                LOGGER.info("[BotOrders] {}: waypoint {} cannot be reached; not added", entity.getDisplayName(),
                      waypoint.toFriendlyString());
                continue;
            }
            coords.add(waypoint);
        }
        entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).addAll(coords);
        LOGGER.info("[BotOrders] {}: added {} of {} waypoints: {}", entity.getDisplayName(), coords.size(),
              waypoints.size(), coords);
        return coords.size();
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean addEntityWaypoint(Entity entity, Coords destination, Princess owner) {
        return addEntityWaypoint(entity, List.of(destination), owner) > 0;
    }

    /**
     * Removes the head waypoint from the entity's waypoint queue If waypoints were added (1,1) then (2,2), then (3,3),
     * this would remove (1,1)
     *
     * @param entity the entity to remove the waypoint from
     */
    public void removeHeadWaypoint(Entity entity) {
        LOGGER.info("Removing head waypoint for entity {}", entity.getId());
        entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>())
              .pollFirst();
    }

    /**
     * Removes the tail waypoint from the entity's waypoint queue If waypoints were added (1,1) then (2,2) then (3,3),
     * this would remove (3,3), good for an "undo" behavior.
     *
     * @param entity the entity to remove the waypoint from
     */
    public void removeTailWaypoint(Entity entity) {
        LOGGER.info("Removing tail waypoint for entity {}", entity.getId());
        entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).pollLast();
    }

    /**
     * Replaces the unit's waypoints with the given ones, dropping any the unit cannot reach.
     *
     * @return how many of the given waypoints were kept; {@code 0} when none of them can be reached
     */
    public int setEntityWaypoints(Entity entity, List<Coords> waypoints, Princess owner) {
        var deque = new ArrayDeque<Coords>();
        for (var waypoint : waypoints) {
            if (isDestinationValidForEntity(entity, waypoint, owner)) {
                // just discard any invalid waypoint
                LOGGER.info("[BotOrders] {}: waypoint {} cannot be reached; not set", entity.getDisplayName(),
                      waypoint.toFriendlyString());
                continue;
            }
            deque.add(waypoint);
        }
        LOGGER.info("[BotOrders] {}: set {} of {} waypoints: {}", entity.getDisplayName(), deque.size(),
              waypoints.size(), deque);
        entityWaypoints.put(entity.getId(), deque);
        return deque.size();
    }


    public void clearWaypoints(Entity entity) {
        LOGGER.debug("Clearing all waypoints for entity {}", entity.getDisplayName());
        entityWaypoints.put(entity.getId(), new ArrayDeque<>());
    }

    public void clearWaypoints() {
        LOGGER.debug("Clearing all waypoints");
        entityWaypoints.clear();
    }


    /**
     * Clears the entity behavior cache, should be done at the start of each movement phase
     */
    public void clear() {
        entityBehaviors.clear();
    }
}
