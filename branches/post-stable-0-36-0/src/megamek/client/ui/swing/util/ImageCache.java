/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

/**
 * An ImageCache that keeps a Hashtable of mapped keys and values.  The cache is
 * prevented from exceeding a set maximum.  If the cache exceeds its maximum 
 * size, items are removed based by looking at access times: the items with the
 * oldest access time are removed first.
 * 
 * @author pjm1
 * @author Arlith
 */
public class ImageCache<K, V> {
    
    /**
     * Default maximum size
     */
    public static int MAX_SIZE = 20000;
    
    /**
     * Stores the current maximum size
     */
    private int maxSize;
    
    /**
     * The cache of Key/Value pairs.
     */
    private Hashtable<K, V> cache;
    
    /**
     * Keeps track of the access times for each key in the cache.
     */
    private HashSet<KeyTimestampPair> times;

    
    /**
     * Create a cache with the default maximum size.
     */
    public ImageCache() {
        cache = new Hashtable<K, V>(MAX_SIZE * 5 / 4, .75f);
        times = new HashSet<KeyTimestampPair>(MAX_SIZE * 5 / 4, .75f);
        maxSize = MAX_SIZE;
    }

    
    public ImageCache(int max) {
        cache = new Hashtable<K, V>(max * 5 / 4, .75f);
        times = new HashSet<KeyTimestampPair>(max * 5 / 4, .75f);
        maxSize = max;
    }

    /**
     * Adds a new key/value pair into the cache.  A timestamp is stored and used
     * to determine which objects to remove if the cache gets too large.
     * 
     * @param key
     * @param value
     * @return
     */
    public synchronized V put(K key, V value) {
        if ((key == null) || (value == null))
            return null;

        if (cache.size() >= maxSize) { // must remove one element
            System.out.println("!ImageCache Max Size reached!");
            Object[] timestamps = 
                    (Object[])times.toArray();
            Arrays.sort(timestamps);
            
            @SuppressWarnings("unchecked")
            K keyToNix = ((KeyTimestampPair)timestamps[0]).key;
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
        cache.put(key, value);
        return value;
    }

    public synchronized V get(K key) {
        if (!cache.containsKey(key))
            return null;
        
        KeyTimestampPair ktp = 
                new KeyTimestampPair(key,System.currentTimeMillis());        
        if (times.contains(ktp)){
            // If the set already contains this key, update the timestmap
            times.remove(ktp);
            times.add(ktp);
        } else {
            times.add(ktp);
        }
        return cache.get(key);
    }

    public void remove(Object key) {
        cache.remove(key);
    }
    
    /**
     * Class used to store a key and timestamp
     * 
     * @author walczak
     *
     */
    private class KeyTimestampPair implements Comparable<KeyTimestampPair>{
        public K key;
        long timestamp;

        public KeyTimestampPair(K k, long ts){
            key = k;
            timestamp = ts;
        }

        @SuppressWarnings("unchecked")
        public boolean equals(Object o){
            try {
                KeyTimestampPair other = (KeyTimestampPair)o;
                return this.equals(other.key);
            } catch (Exception e){
                return false;
            }
        }
        @Override
        public int compareTo(KeyTimestampPair other) {            
            return (int)(timestamp - other.timestamp);
        }
        
        public String toString(){
            return timestamp + "";
        }
    }
}
