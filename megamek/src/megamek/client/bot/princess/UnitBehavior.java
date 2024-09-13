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

import java.util.HashMap;
import java.util.Map;

import megamek.common.Entity;
import megamek.common.Mek;

public class UnitBehavior {
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

    private Map<Integer, BehaviorType> entityBehaviors = new HashMap<>();

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

    /**
     * Clears the entity behavior cache, should be done at the start of each movement phase
     */
    public void clear() {
        entityBehaviors.clear();
    }

}
