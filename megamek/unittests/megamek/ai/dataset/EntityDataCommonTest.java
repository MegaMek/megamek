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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.units.UnitRole;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EntityDataMap and EntityDataSerializer via concrete implementations.
 */
class EntityDataCommonTest {

    enum TestField {
        BOOL, DOUBLE, FLOAT, ENUM, ROLE, PHASE, AIMING, LIST, STRING
    }

    static class ConcreteDataMap extends EntityDataMap<TestField> {
        ConcreteDataMap() {
            super(TestField.class);
        }
    }

    static class ConcreteSerializer extends EntityDataSerializer<TestField, ConcreteDataMap> {
        ConcreteSerializer() {
            super(TestField.class);
        }

        @Override
        protected String getNullValue(TestField field) {
            if (field == TestField.STRING) {
                return "DEFAULT";
            }
            return super.getNullValue(field);
        }
    }

    @Test
    void testSerializationTypes() {
        ConcreteDataMap data = new ConcreteDataMap();
        data.put(TestField.BOOL, true);
        data.put(TestField.DOUBLE, 1.23);
        data.put(TestField.FLOAT, 5.67f);
        data.put(TestField.ENUM, TestField.BOOL);
        data.put(TestField.ROLE, UnitRole.SCOUT);
        data.put(TestField.PHASE, GamePhase.MOVEMENT);
        data.put(TestField.AIMING, AimingMode.NONE);
        data.put(TestField.LIST, Arrays.asList("A", "B", "C"));
        data.put(TestField.STRING, "Hello");

        ConcreteSerializer serializer = new ConcreteSerializer();
        String serialized = serializer.serialize(data);
        String[] parts = serialized.split("\t");

        // VERSION, BOOL, DOUBLE, FLOAT, ENUM, ROLE, PHASE, AIMING, LIST, STRING
        assertEquals("ConcreteDataMap.31052025", parts[0]);
        assertEquals("1", parts[1]);
        assertEquals("1.23", parts[2]);
        assertEquals("5.67", parts[3]);
        assertEquals("BOOL", parts[4]);
        assertEquals("SCOUT", parts[5]);
        assertEquals("MOVEMENT", parts[6]);
        assertEquals("NONE", parts[7]);
        assertEquals("A B C", parts[8]);
        assertEquals("Hello", parts[9]);
    }

    @Test
    void testListWithEnums() {
        ConcreteDataMap data = new ConcreteDataMap();
        data.put(TestField.LIST, Arrays.asList(GamePhase.MOVEMENT, GamePhase.OFFBOARD));

        ConcreteSerializer serializer = new ConcreteSerializer();
        String serialized = serializer.serialize(data);
        String[] parts = serialized.split("\t");

        // VERSION is index 0. LIST is index 8 (BOOL=1, DBL=2, FLT=3, ENUM=4, ROLE=5, PHASE=6, AIMING=7)
        // Wait, index might be different if fields are missing in data.
        // serialize() iterates over fieldOrder (all enum constants).
        // If field is missing, it gets null value.

        assertEquals("MOVEMENT OFFBOARD", parts[8]);
    }

    @Test
    void testNullValues() {
        ConcreteDataMap data = new ConcreteDataMap();
        ConcreteSerializer serializer = new ConcreteSerializer();
        String serialized = serializer.serialize(data);
        String[] parts = serialized.split("\t");

        assertEquals("", parts[1]); // Default null value
        assertEquals("DEFAULT", parts[9]); // Custom null value
    }

    @Test
    void testAddFormatHandler() {
        ConcreteDataMap data = new ConcreteDataMap();
        data.put(TestField.STRING, "custom");

        ConcreteSerializer serializer = new ConcreteSerializer();
        serializer.addFormatHandler(String.class, obj -> "FIXED");

        String serialized = serializer.serialize(data);
        assertTrue(serialized.contains("FIXED"));
    }

    @Test
    void testGetHeaderLine() {
        ConcreteSerializer serializer = new ConcreteSerializer();
        String header = serializer.getHeaderLine();
        assertTrue(header.startsWith("VERSION\tBOOL\tDOUBLE\tFLOAT\tENUM\tROLE\tPHASE\tAIMING\tLIST\tSTRING"));
    }
}
