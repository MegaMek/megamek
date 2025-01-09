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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory class to store data for state processing and decision-making.
 * The memory class is not much dissimilar from the "Options" that many objects have, the difference is that the memory is supposed to be
 * volatile, it is not saved anywhere, it is just a temporary storage for the AI to use during the execution of the AI.
 * The memory will usually keep tabs on "things" that happened, so you can iteratively build on top of what happened in the past to
 * determine future events. One example would be to keep track of the last target that was attacked, so you can avoid attacking the same
 * twice in a roll, or maybe you have a penalty if you were shot at last turn by a specific weapon or in a specific place, this avoids
 * having to keep track of all this information in the object itself, adding an indefinite amount of flags and toggles, and instead
 * allowing for arbitrary information to be stored "on the fly" and used as needed. And of course, to be forgotten when done with.
 * To push anything into memory all you have to do is get a reference to the memory and put the information in it.
 * Every memory has to have a key, which is a string that identifies the memory, and a value, which can be any object, but it is recommended
 * to use the same type of object for the same key, as the memory does not enforce any type of type safety, and you will have to remember
 * the type of the object you put in the memory when you get it back, otherwise you may get a ClassCastException if you try to do it by
 * hand, or an optional empty if you use the "get" method with the wrong type.
 * The use of the memory is safe, the memory will return an empty optional if the key is not found, and you can check if the key is present with "containsKey". Requesting for a specific return type will also return
 * an empty optional if the key is found but the type is not the one you requested, which hardens even more the feature against clerical
 * mistakes.
 * @author Luana Coppio
 */
public class Memory {

    private final Map<String, Object> memory = new ConcurrentHashMap<>();

    /**
     * Removes a memory from the memory.
     * @param key the key to remove
     */
    public void remove(String key) {
        memory.remove(key);
    }

    /**
     * Clears all memories that start with the given prefix.
     * @param prefix the prefix to clear
     */
    public void clear(String prefix) {
        memory.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }

    /**
     * Clears all memories.
     */
    public void clear() {
        memory.clear();
    }

    /**
     * Puts something in the memory.
     * @param key the key to which the value will be associated, if the key already exists, the value will be replaced
     * @param value the value to be stored. It can be any object, but it is recommended to use the same type of object for the same key
     */
    public void put(String key, Object value) {
        memory.put(key, value);
    }

    /**
     * Gets a memory. It will return an optional to protect you from null pointers, so you have to check prior to accessing it.
     * @param key the key to get the memory from
     * @return an optional with the stored value, or an empty optional if the key is not found
     */
    public Optional<Object> get(String key) {
        return Optional.ofNullable(memory.getOrDefault(key, null));
    }

    /**
     * Checks if the memory contains a specific key.
     * @param key the key to check
     * @return true if the key is present, false otherwise
     */
    public boolean containsKey(String key) {
        return memory.containsKey(key);
    }

    /**
     * Gets a memory as a string.
     * @param key the key to get the memory from
     * @return an optional with the stored value as a string, or an empty optional if the key is not found or the value is not a string
     */
    public Optional<String> getString(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof String value) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * Gets a memory as an integer.
     * @param key the key to get the memory from
     * @return an optional with the stored value as an integer, or an empty optional if the key is not found or the value is not an integer
     */
    public OptionalInt getInt(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Integer value) {
            return OptionalInt.of(value);
        }
        return OptionalInt.empty();
    }

    /**
     * Gets a memory as a double.
     * @param key the key to get the memory from
     * @return an optional with the stored value as a double, or an empty optional if the key is not found or the value is not a double
     */
    public OptionalDouble getDouble(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Double value) {
            return OptionalDouble.of(value);
        }
        return OptionalDouble.empty();
    }

    /**
     * Gets a memory as a long.
     * @param key the key to get the memory from
     * @return an optional with the stored value as a long, or an empty optional if the key is not found or the value is not a long
     */
    public OptionalLong getLong(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Long value) {
            return OptionalLong.of(value);
        }
        return OptionalLong.empty();
    }

    /**
     * Gets a memory as a boolean.
     * @param key the key to get the memory from
     * @return an optional with the stored value as a boolean, or an empty optional if the key is not found or the value is not a boolean
     */
    public Optional<Boolean> getBoolean(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Boolean value) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * Checks a memory as a boolean.
     * @param key the key to get the memory from
     * @return returns false if the memory isn't present or if the value is `falsy`, otherwise returns true
     */
    public boolean isSet(String key) {
        if (memory.containsKey(key)) {
            var memo = memory.get(key);
            if (memo instanceof Boolean value) {
                return value;
            } else if (memo instanceof Number value) {
                return value.doubleValue() != 0;
            } else if (memo instanceof String value) {
                return !value.isEmpty();
            } else if (memo instanceof Collection<?> value) {
                return !value.isEmpty();
            } else if (memo instanceof Map<?,?> value) {
                return !value.isEmpty();
            } else if (memo instanceof Object[] value) {
                return value.length > 0;
            } else return memo != null;
        }
        return false;
    }
}
