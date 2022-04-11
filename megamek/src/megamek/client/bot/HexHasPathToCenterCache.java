/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot;

import megamek.codeUtilities.StringUtility;
import megamek.common.Coords;
import megamek.common.EntityMovementMode;
import megamek.common.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a cache of hexes and movement type against whether or not they have a valid path to the
 * center of the board. This class does not maintain the actual movement path but rather a simple
 * boolean flag of whether a path to center exists.
 *
 * @author Deric Page (dericpage@users.sourceforge.net)
 * @since 10/20/2014 10:31 AM
 */
public final class HexHasPathToCenterCache {

    private final Map<Key, Boolean> cache = new ConcurrentHashMap<>();

    /**
     * Creates a new cache.
     */
    HexHasPathToCenterCache() {

    }

    /**
     * Will return TRUE if it is known that a path exists to the center of the board or FALSE if it known such a path
     * does not exist.  A NULL value will be returned in the case of not knowing one way or the other.
     *
     * @param key The {@link Key} describing the starting hex coords and movement type for a theoretical path.
     * @return TRUE if the path exists, FALSE if it does not or NULL if it is unknown whether or not a path exists.
     */
    @Nullable
    Boolean hasPathToCenter(Key key) {
        return cache.get(key);
    }

    /**
     * Adds or updates the cache with the existence of a path to the center of the board from the coordinate hex using
     * the given movement type.
     *
     * @param key             The {@link Key} describing the starting hex coords and movement type for a theoretical path.
     * @param hasPathToCenter TRUE if the path exists or FALSE if it does not.
     */
    void addMember(Key key, boolean hasPathToCenter) {
        cache.put(key, hasPathToCenter);
    }

    /**
     * Removes all references in the cache.
     */
    void clearCache() {
        cache.clear();
    }

    /**
     * Removes the specified reference from the cache.
     *
     * @param key The specific coordinates/movement-mode combination to remove from the cache.
     */
    void remove(Key key) {
        cache.remove(key);
    }

    /**
     * Removes all entries in the cache relating to the given starting hex.
     *
     * @param hexCoords The coordinates, {@link Coords#toFriendlyString()}, of the starting hex.
     */
    void remove(String hexCoords) {
        if (StringUtility.isNullOrBlank(hexCoords)) {
            return;
        }
        Set<Key> keySet = new HashSet<>(cache.keySet());
        for (Key key : keySet) {
            if (key.getHexCoords().equals(hexCoords)) {
                remove(key);
            }
        }
    }

    /**
     * Defines the key value for the {@link HexHasPathToCenterCache}.  Keys are sorted first by hexCoords followed by
     * movementMode.
     */
    static class Key implements Comparable<Key> {
        private final String hexCoords;
        private final EntityMovementMode movementMode;

        /**
         * Creates a new key.
         *
         * @param hexCoords    The starting coordinates, {@link Coords#toFriendlyString()}, for the path to the center.
         * @param movementMode The type of movement used in generating the path.
         * @throws IllegalArgumentException if hexCoords is NULL or Empty or movementMode is NULL.
         */
        Key(String hexCoords, EntityMovementMode movementMode) {
            if (StringUtility.isNullOrBlank(hexCoords)) {
                throw new IllegalArgumentException("Starting Coords is NULL or Empty.");
            }
            if (movementMode == null) {
                throw new IllegalArgumentException("Movement Type is NULL.");
            }
            this.hexCoords = hexCoords;
            this.movementMode = movementMode;
        }

        /**
         * Returns the starting coordinates for the path to the center.
         *
         * @return The starting coordinates, {@link Coords#toFriendlyString()}, for the path to the center.
         */
        String getHexCoords() {
            return hexCoords;
        }

        /**
         * Returns the type of movement used in generating the path.
         *
         * @return The type of movement used in generating the path.
         */
        EntityMovementMode getMovementMode() {
            return movementMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }

            Key key = (Key) o;

            if (movementMode != key.movementMode) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (!hexCoords.equals(key.hexCoords)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = hexCoords.hashCode();
            result = 31 * result + movementMode.hashCode();
            return result;
        }

        @Override
        public int compareTo(Key o) {
            int result = getHexCoords().compareTo(o.getHexCoords());
            if (result != 0) {
                return result;
            }
            return getMovementMode().compareTo(o.getMovementMode());
        }

        @Override
        public String toString() {
            return getHexCoords() + "_" + getMovementMode().toString();
        }
    }
}
