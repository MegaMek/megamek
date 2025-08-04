/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import megamek.common.annotations.Nullable;

/**
 * Provides a means to track unit names for collisions. This API is not thread safe.
 */
public class UnitNameTracker {
    private final Map<String, List<Entity>> entityMap = new HashMap<>();

    /**
     * Adds an entity to the name tracker.
     *
     * @param entity The entity to track for name collisions.
     */
    public void add(Entity entity) {
        List<Entity> entities = entityMap.computeIfAbsent(entity.getShortNameRaw(), k -> new ArrayList<>());
        entities.add(entity);
        entity.setDuplicateMarker(entities.size());
    }

    /**
     * Removes an entity from the name tracker.
     *
     * @param entity The entity to remove from the name tracker.
     *
     * @return A value indicating whether or not the entity was removed.
     */
    public boolean remove(Entity entity) {
        return remove(entity, null);
    }

    /**
     * Removes an entity from the name tracker.
     *
     * @param entity          The entity to remove from the name tracker.
     * @param onEntityUpdated An optional function to execute when an entity is updated due to a duplicate name change.
     *
     * @return A value indicating whether or not the entity was removed.
     */
    public boolean remove(Entity entity, @Nullable Consumer<Entity> onEntityUpdated) {
        String rawName = entity.getShortNameRaw();
        int removedDuplicateMarker = entity.getDuplicateMarker();

        // Decrease the number of duplicate names, removing it if there was only one left
        List<Entity> entities = entityMap.get(rawName);
        if ((entities == null) || !entities.remove(entity)) {
            return false;
        }

        // If there are more than one entities with this raw name,
        // go through the list of matching entities and update their
        // duplicate number
        for (Entity e : entities) {
            boolean updated = e.updateDuplicateMarkerAfterDelete(removedDuplicateMarker);
            if (updated && (onEntityUpdated != null)) {
                onEntityUpdated.accept(e);
            }
        }

        return true;
    }

    /**
     * Clears the unit name tracker of all tracked entities.
     */
    public void clear() {
        entityMap.clear();
    }
}
