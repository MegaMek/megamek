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


package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the abstract base class EntityDataMap.
 */
class EntityDataMapTest {

    enum TestField {
        FIELD_A, FIELD_B, FIELD_C
    }

    static class ConcreteEntityDataMap extends EntityDataMap<TestField> {
        protected ConcreteEntityDataMap() {
            super(TestField.class);
        }
    }

    @Test
    void testPutAndGet() {
        ConcreteEntityDataMap map = new ConcreteEntityDataMap();
        map.put(TestField.FIELD_A, "ValueA");
        map.put(TestField.FIELD_B, 123);

        assertEquals("ValueA", map.get(TestField.FIELD_A));
        assertEquals(123, map.get(TestField.FIELD_B));
        assertNull(map.get(TestField.FIELD_C));
    }

    @Test
    void testTypedGet() {
        ConcreteEntityDataMap map = new ConcreteEntityDataMap();
        map.put(TestField.FIELD_A, "ValueA");
        map.put(TestField.FIELD_B, 123);

        assertEquals("ValueA", map.get(TestField.FIELD_A, String.class));
        assertEquals(123, map.get(TestField.FIELD_B, Integer.class));

        // Correct field, wrong type
        assertNull(map.get(TestField.FIELD_A, Integer.class));
        assertNull(map.get(TestField.FIELD_B, String.class));

        // Non-existent field
        assertNull(map.get(TestField.FIELD_C, String.class));
    }

    @Test
    void testGetAllFields() {
        ConcreteEntityDataMap map = new ConcreteEntityDataMap();
        map.put(TestField.FIELD_A, "ValueA");
        map.put(TestField.FIELD_B, 123);

        Map<TestField, Object> allFields = map.getAllFields();
        assertEquals(2, allFields.size());
        assertEquals("ValueA", allFields.get(TestField.FIELD_A));
        assertEquals(123, allFields.get(TestField.FIELD_B));

        // Verify it's a copy
        allFields.put(TestField.FIELD_C, "New");
        assertNull(map.get(TestField.FIELD_C));
    }

    @Test
    void testGetFieldOrder() {
        ConcreteEntityDataMap map = new ConcreteEntityDataMap();
        map.put(TestField.FIELD_B, 123);
        map.put(TestField.FIELD_A, "ValueA");
        map.put(TestField.FIELD_B, 456); // Duplicate put

        List<TestField> fieldOrder = map.getFieldOrder();
        assertEquals(2, fieldOrder.size());
        assertEquals(TestField.FIELD_B, fieldOrder.getFirst());
        assertEquals(TestField.FIELD_A, fieldOrder.get(1));
    }

    @Test
    void testGetFieldEnumClass() {
        ConcreteEntityDataMap map = new ConcreteEntityDataMap();
        assertEquals(TestField.class, map.getFieldEnumClass());
    }

    @Test
    void testGetVersionedClassName() {
        ConcreteEntityDataMap map = new ConcreteEntityDataMap();
        // Since it's a nested class in the test, simple name might be slightly different depending on environment
        // but it should follow the pattern [SimpleName].[VERSION]
        String versionedName = map.getVersionedClassName();
        assertNotNull(versionedName);
        assertTrue(versionedName.startsWith("ConcreteEntityDataMap."));
        assertTrue(versionedName.endsWith("31052025"));
    }
}
