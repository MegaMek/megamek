/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.util;

import megamek.common.annotations.Nullable;

import java.util.Hashtable;

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
    private Hashtable<K, V> cache;
        
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
     * @param key
     * @param value
     * @return
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

    public void remove(Object key) {
        cache.remove(key);
    }
    
    public int size() {
        return cache.size();
    }
    
    public void clear() {
        cache.clear();
    }
}
