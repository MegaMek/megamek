/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.util;

import java.util.Hashtable;

import megamek.common.annotations.Nullable;

/**
 * An ImageCache that keeps a Hashtable of mapped keys and values.
 *
 * @author Arlith
 */
public class ImageCache<K, V> {
    /**
     * Default maximum size
     */
    public static int MAX_SIZE = 30000;


    /**
     * The cache of Key/Value pairs.
     */
    private final Hashtable<K, V> cache;

    /**
     * Create a cache with the default maximum size.
     */
    public ImageCache() {
        cache = new Hashtable<>(MAX_SIZE * 5 / 4, .75f);
    }


    public ImageCache(int max) {
        cache = new Hashtable<>(max * 5 / 4, .75f);
    }

    /**
     * Adds a new key/value pair into the cache.
     *
     */
    public synchronized @Nullable V put(@Nullable K key, @Nullable V value) {
        if ((key == null) || (value == null)) {
            return null;
        }

        cache.put(key, value);
        return value;
    }

    public synchronized @Nullable V get(K key) {
        return cache.getOrDefault(key, null);
    }

    public synchronized void remove(K key) {
        if (key == null) {
            return;
        }
        cache.remove(key);
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }
}
