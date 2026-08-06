/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.ai.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Memory}.
 */
class MemoryTest {

    private Memory memory;

    @BeforeEach
    void setUp() {
        memory = new Memory();
    }

    @Test
    void testPutAndGet() {
        Object value = new Object();
        memory.put("key", value);
        assertEquals(Optional.of(value), memory.get("key"));
        assertTrue(memory.containsKey("key"));
    }

    @Test
    void testGetEmpty() {
        assertFalse(memory.get("nonexistent").isPresent());
        assertFalse(memory.containsKey("nonexistent"));
    }

    @Test
    void testRemove() {
        memory.put("key", "value");
        assertTrue(memory.containsKey("key"));
        memory.remove("key");
        assertFalse(memory.containsKey("key"));
    }

    @Test
    void testClearPrefix() {
        memory.put("pref1_a", 1);
        memory.put("pref1_b", 2);
        memory.put("pref2_c", 3);

        memory.clear("pref1");

        assertFalse(memory.containsKey("pref1_a"));
        assertFalse(memory.containsKey("pref1_b"));
        assertTrue(memory.containsKey("pref2_c"));
    }

    @Test
    void testClearAll() {
        memory.put("a", 1);
        memory.put("b", 2);
        memory.clear();
        assertFalse(memory.containsKey("a"));
        assertFalse(memory.containsKey("b"));
    }

    @Test
    void testGetString() {
        memory.put("s", "value");
        memory.put("i", 123);

        assertEquals(Optional.of("value"), memory.getString("s"));
        assertEquals(Optional.empty(), memory.getString("i"));
        assertEquals(Optional.empty(), memory.getString("nonexistent"));
    }

    @Test
    void testGetInt() {
        memory.put("i", 123);
        memory.put("s", "value");

        assertEquals(OptionalInt.of(123), memory.getInt("i"));
        assertEquals(OptionalInt.empty(), memory.getInt("s"));
        assertEquals(OptionalInt.empty(), memory.getInt("nonexistent"));
    }

    @Test
    void testGetDouble() {
        memory.put("d", 1.23);
        memory.put("i", 123);

        assertEquals(OptionalDouble.of(1.23), memory.getDouble("d"));
        assertEquals(OptionalDouble.empty(), memory.getDouble("i"));
        assertEquals(OptionalDouble.empty(), memory.getDouble("nonexistent"));
    }

    @Test
    void testGetLong() {
        memory.put("l", 123L);
        memory.put("i", 123);

        assertEquals(OptionalLong.of(123L), memory.getLong("l"));
        assertEquals(OptionalLong.empty(), memory.getLong("i"));
        assertEquals(OptionalLong.empty(), memory.getLong("nonexistent"));
    }

    @Test
    void testGetBoolean() {
        memory.put("b", true);
        memory.put("s", "true");

        assertEquals(Optional.of(true), memory.getBoolean("b"));
        assertEquals(Optional.empty(), memory.getBoolean("s"));
        assertEquals(Optional.empty(), memory.getBoolean("nonexistent"));
    }

    @Test
    void testIsSet() {
        // Boolean
        memory.put("bTrue", true);
        memory.put("bFalse", false);
        assertTrue(memory.isSet("bTrue"));
        assertFalse(memory.isSet("bFalse"));

        // Number
        memory.put("nNonZero", 1);
        memory.put("nZero", 0.0);
        assertTrue(memory.isSet("nNonZero"));
        assertFalse(memory.isSet("nZero"));

        // String
        memory.put("sNonEmpty", "val");
        memory.put("sEmpty", "");
        assertTrue(memory.isSet("sNonEmpty"));
        assertFalse(memory.isSet("sEmpty"));

        // Collection
        List<String> list = new ArrayList<>();
        memory.put("cEmpty", list);
        assertFalse(memory.isSet("cEmpty"));
        list.add("item");
        assertTrue(memory.isSet("cEmpty"));

        // Map
        Map<String, String> map = new HashMap<>();
        memory.put("mEmpty", map);
        assertFalse(memory.isSet("mEmpty"));
        map.put("k", "v");
        assertTrue(memory.isSet("mEmpty"));

        // Array
        memory.put("aEmpty", new Object[0]);
        memory.put("aNonEmpty", new Object[] { 1 });
        assertFalse(memory.isSet("aEmpty"));
        assertTrue(memory.isSet("aNonEmpty"));

        // Generic Object
        memory.put("obj", new Object());
        assertTrue(memory.isSet("obj"));

        // Not present
        assertFalse(memory.isSet("nonexistent"));
    }
}
