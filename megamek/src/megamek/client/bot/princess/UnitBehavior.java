/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

import java.util.*;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Mek;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

public class UnitBehavior {
    private final static MMLogger logger = MMLogger.create(UnitBehavior.class);

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
        BehaviorSettings botSettings = owner.getBehaviorSettings();

        if (botSettings.isForcedWithdrawal() && entity.isCrippled()) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                return BehaviorType.NoPathToDestination;
            }

            return BehaviorType.ForcedWithdrawal;
        } else if (botSettings.shouldAutoFlee() && botSettings.getDestinationEdge() != CardinalEdge.NONE) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                return BehaviorType.NoPathToDestination;
            }

            return BehaviorType.MoveToDestination;
        } else if (entityWaypoints.containsKey(entity.getId()) && getWaypointForEntity(entity).isPresent()) {
            while (getWaypointForEntity(entity).isPresent() &&
                owner.getClusterTracker().getDestinationCoords(entity, getWaypointForEntity(entity).get(), true).isEmpty()) {
                removeHeadWaypoint(entity);
            }
            if (getWaypointForEntity(entity).isPresent()) {
                return BehaviorType.MoveToDestination;
            }

            return BehaviorType.NoPathToDestination;
        } else if ((entity instanceof Mek) && ((Mek) entity).isJustMovedIntoIndustrialKillingWater()) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                return BehaviorType.NoPathToDestination;
            }

            return BehaviorType.ForcedWithdrawal;
        } else {
            // if we can't see anyone, move to contact
            if (!entity.getGame().getAllEnemyEntities(entity).hasNext()) {
                return BehaviorType.MoveToContact;
            }

            return BehaviorType.Engaged;
        }
    }

    public Optional<Coords> getWaypointForEntity(Entity entity) {
        return Optional.ofNullable(entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).peek());
    }

    public boolean isDestinationValidForEntity(Entity entity, Coords destination, Princess owner) {
        var value = owner.getClusterTracker().getDestinationCoords(entity, destination, true).isEmpty();
        logger.debug("Checking if destination is valid for entity " + entity.getId() + ": " + destination + " -> " + value);
        return value;
    }

    public boolean addEntityWaypoint(Entity entity, List<Coords> waypoints, Princess owner) {
        var coords = new ArrayList<Coords>();
        for (var waypoint : waypoints) {
            if (isDestinationValidForEntity(entity, waypoint, owner)) {
                // just discard any invalid waypoint
                logger.info("Discarding invalid waypoint for entity " + entity.getId() + ": " + waypoint);
                continue;
            }
            coords.add(waypoint);
        }
        entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).addAll(coords);
        logger.info("Adding waypoints for entity " + entity.getId() + ": " + coords);
        return true;
    }


    public boolean addEntityWaypoint(Entity entity, Coords destination, Princess owner) {
        return addEntityWaypoint(entity, List.of(destination), owner);
    }

    /**
     * Removes the head waypoint from the entity's waypoint queue
     * If waypoints were added (1,1) then (2,2), then (3,3), this would remove (1,1)
     * @param entity the entity to remove the waypoint from
     */
    public Optional<Coords> removeHeadWaypoint(Entity entity) {
        logger.info("Removing head waypoint for entity " + entity.getId());
        return Optional.ofNullable(entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).pollFirst());
    }


    /**
     * Removes the head waypoint from the entity's waypoint queue
     * If waypoints were added (1,1) then (2,2) then (3,3), this would remove (3,3), good for an "undo" behavior.
     * @param entity the entity to remove the waypoint from
     */
    public Optional<Coords> removeTailWaypoint(Entity entity) {
        logger.info("Removing tail waypoint for entity " + entity.getId());
        return Optional.ofNullable(entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).pollLast());
    }

    /**
     * Sets the entity's waypoints to the given destination
     * @param entity
     * @param waypoints
     * @param owner
     * @return
     */
    public boolean setEntityWaypoints(Entity entity, List<Coords> waypoints, Princess owner) {
        var deque = new ArrayDeque<Coords>();
        for (var waypoint : waypoints) {
            if (isDestinationValidForEntity(entity, waypoint, owner)) {
                // just discard any invalid waypoint
                logger.info("Discarding invalid waypoint for entity " + entity.getId() + ": " + waypoint);
                continue;
            }
            deque.add(waypoint);
        }
        logger.debug("Setting waypoints for entity " + entity.getId() + ": " + deque);
        entityWaypoints.put(entity.getId(), deque);
        return true;
    }

    public void clearWaypoints() {
        logger.debug("Clearing all waypoints");
        entityWaypoints.clear();
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

    public void overrideBehaviorType(Entity entity, BehaviorType behaviorType) {
        entityBehaviors.put(entity.getId(), behaviorType);
    }

    /**
     * Clears the entity behavior cache, should be done at the start of each movement phase
     */
    public void clear() {
        entityBehaviors.clear();
    }

}
