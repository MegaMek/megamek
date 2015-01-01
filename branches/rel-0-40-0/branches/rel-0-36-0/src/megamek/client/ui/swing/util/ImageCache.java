/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing.util;

import java.awt.Image;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * An ImageCache that keeps a Hashtable of mapped keys and values.
 * 
 * @author Arlith
 */
public class ImageCache<K, V> {

    public static int MAX_SIZE = 20000;
    private int maxSize;
    private Hashtable<K, V> cache;
    private LinkedList<K> lru = new LinkedList<K>();

    public ImageCache() {
        cache = new Hashtable<K, V>(MAX_SIZE * 5 / 4, .75f);
        maxSize = MAX_SIZE;
    }

    public ImageCache(int max) {
        cache = new Hashtable<K, V>(max * 5 / 4, .75f);
        maxSize = max;
    }

    /**
     * Adds a new key/value pair into the cache.
     * 
     * @param key
     * @param value
     * @return
     */
    public synchronized V put(K key, V value) {
        if ((key == null) || (value == null))
            return null;

        if (cache.containsKey(key)) {
            lru.remove(key);
        } else {
            if (cache.size() == maxSize) { // must remove one element
                K keyToNix = lru.removeFirst();
                V valToNix = cache.get(key);
                cache.remove(keyToNix);
                // Images must be flushed before dereference
                if (valToNix instanceof Image) {
                    ((Image) valToNix).flush();
                } else if (valToNix instanceof List) {
                    for (Object o : ((List<?>) valToNix)) {
                        if (o instanceof Image) {
                            ((Image) o).flush();
                        }
                    }
                }
            }
        }
        lru.addLast(key);
        cache.put(key, value);

        return value;
    }

    public synchronized V get(K key) {
        if (!cache.containsKey(key))
            return null;
        lru.remove(key);
        lru.addLast(key);
        return cache.get(key);
    }

    public void remove(Object key) {
        cache.remove(key);
    }
    public int size(){
        return cache.size();
    }
    
}
