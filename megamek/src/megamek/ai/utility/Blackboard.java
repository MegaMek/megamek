/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.ai.utility;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blackboard is a centralized store for data that can be read/written by various AI components.
 * It can hold arbitrary key-value pairs. Typically, keys might be strings (for attribute names)
 * and values might be objects, numbers, or domain-specific classes.
 * @author Luana Coppio
 */
public class Blackboard {

    // Internally, we can store data in a thread-safe map or a regular map if not accessed concurrently.
    // For simplicity, let's use a plain HashMap here.
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    /**
     * Stores or updates a value in the blackboard under the given key.
     * @param key The unique name of the attribute
     * @param value The data to store
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Retrieves a value associated with the given key.
     * Returns null if key not found. Could also throw if that’s preferred.
     * @param key The attribute name to look up
     * @return The stored value, or null if not present
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * Convenience method for retrieving numeric data (e.g. health ratios, distances).
     * Returns a default value if the attribute is missing or not a Number.
     */
    public double getDouble(String key, double defaultValue) {
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Convenience method for retrieving numeric data (e.g. health ratios, distances).
     * Returns a default value if the attribute is missing or not a Number.
     */
    public long getLong(String key, long defaultValue) {
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return defaultValue;
    }

    /**
     * Convenience method for retrieving string data.
     */
    public String getString(String key, String defaultValue) {
        Object val = data.get(key);
        return val instanceof String ? (String)val : defaultValue;
    }

    /**
     * Convenience method for retrieving boolean data.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = data.get(key);
        return val instanceof Boolean ? (boolean) val : defaultValue;
    }

    /**
     * Checks if the blackboard contains a certain key.
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    /**
     * Provides an immutable snapshot of the blackboard data.
     * Might be useful for debugging or passing consistent snapshots around.
     */
    public Map<String, Object> snapshot() {
        return Map.copyOf(data);
    }
}
