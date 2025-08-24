/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.board.Coords;
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
                  owner.getClusterTracker()
                        .getDestinationCoords(entity, getWaypointForEntity(entity).get(), true)
                        .isEmpty()) {
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

    public Optional<Coords> getWaypointForEntity(Entity entity) {
        return Optional.ofNullable(entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).peek());
    }

    public boolean isDestinationValidForEntity(Entity entity, Coords destination, Princess owner) {
        var value = owner.getClusterTracker().getDestinationCoords(entity, destination, true).isEmpty();
        LOGGER.debug("Checking if destination is valid for entity {}: {} -> {}", entity.getId(), destination, value);
        return value;
    }

    public boolean addEntityWaypoint(Entity entity, List<Coords> waypoints, Princess owner) {
        var coords = new ArrayList<Coords>();
        for (var waypoint : waypoints) {
            if (isDestinationValidForEntity(entity, waypoint, owner)) {
                // just discard any invalid waypoint
                LOGGER.info("addEntityWaypoint - Discarding invalid waypoint for entity {}: {}",
                      entity.getId(),
                      waypoint);
                continue;
            }
            coords.add(waypoint);
        }
        entityWaypoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>()).addAll(coords);
        LOGGER.info("Adding waypoints for entity {}: {}", entity.getId(), coords);
        return true;
    }

    public boolean addEntityWaypoint(Entity entity, Coords destination, Princess owner) {
        return addEntityWaypoint(entity, List.of(destination), owner);
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
     * Sets the entity's waypoints to the given destination
     *
     */
    public void setEntityWaypoints(Entity entity, List<Coords> waypoints, Princess owner) {
        var deque = new ArrayDeque<Coords>();
        for (var waypoint : waypoints) {
            if (isDestinationValidForEntity(entity, waypoint, owner)) {
                // just discard any invalid waypoint
                LOGGER.info("Discarding invalid waypoint for entity {}: {}", entity.getId(), waypoint);
                continue;
            }
            deque.add(waypoint);
        }
        LOGGER.debug("Setting waypoints for entity {}: {}", entity.getId(), deque);
        entityWaypoints.put(entity.getId(), deque);
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
