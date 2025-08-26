/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.codeUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import megamek.common.compute.Compute;
import megamek.common.util.sorter.NaturalOrderComparator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ObjectUtilityTest {

    @Test
    void testNonNullTwoArgs() {
        final String first = "first";
        final String second = "second";
        assertEquals(first, ObjectUtility.nonNull(first, null));
        assertEquals(first, ObjectUtility.nonNull(first, second));
        assertEquals(second, ObjectUtility.nonNull(null, second));
        assertNull(ObjectUtility.nonNull(null, null));
    }

    @Test
    void testNonNullManyArgs() {
        final String first = "first";
        final String second = "second";
        final String third = "third";
        final String fourth = "fourth";
        final String nullString = null;
        assertEquals(first, ObjectUtility.nonNull(first, null, third, fourth));
        assertEquals(first, ObjectUtility.nonNull(first, second, third, fourth));
        assertEquals(second, ObjectUtility.nonNull(null, second, third, fourth));
        assertEquals(third, ObjectUtility.nonNull(null, null, third, null));
        assertEquals(third, ObjectUtility.nonNull(null, null, third, fourth));
        assertEquals(fourth, ObjectUtility.nonNull(null, null, null, fourth));
        assertEquals(fourth, ObjectUtility.nonNull(null, null, nullString, fourth));
        assertNull(ObjectUtility.nonNull(null, null, nullString));
        assertNull(ObjectUtility.nonNull(null, null, null, null));
    }

    @Test
    void testCompareNullable() {
        final NaturalOrderComparator mockNaturalOrderComparator = mock(NaturalOrderComparator.class);
        final String test = "test";
        assertEquals(0, ObjectUtility.compareNullable(null, null, mockNaturalOrderComparator));
        assertEquals(0, ObjectUtility.compareNullable(test, test, mockNaturalOrderComparator));
        assertEquals(0, ObjectUtility.compareNullable("", "", mockNaturalOrderComparator));
        assertEquals(1, ObjectUtility.compareNullable(null, "", mockNaturalOrderComparator));
        assertEquals(-1, ObjectUtility.compareNullable("", null, mockNaturalOrderComparator));
        when(mockNaturalOrderComparator.compare(any(), any())).thenReturn(-1);
        assertEquals(-1, ObjectUtility.compareNullable(test, "", mockNaturalOrderComparator));
    }

    @Test
    void testGetRandomItemFromCollection() {
        final Collection<String> nullCollection = null;
        assertNull(ObjectUtility.getRandomItem(nullCollection));
        final Collection<String> collection = new HashSet<>();
        assertNull(ObjectUtility.getRandomItem(collection));
        collection.add("a");
        collection.add("b");
        collection.add("c");
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);
            assertEquals("a", ObjectUtility.getRandomItem(collection));
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(1);
            assertEquals("b", ObjectUtility.getRandomItem(collection));
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(2);
            assertEquals("c", ObjectUtility.getRandomItem(collection));
        }
    }

    @Test
    void testGetRandomItemFromList() {
        final List<String> nullList = null;
        assertNull(ObjectUtility.getRandomItem(nullList));
        final List<String> list = new ArrayList<>();
        assertNull(ObjectUtility.getRandomItem(list));
        list.add("a");
        list.add("b");
        list.add("c");
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);
            assertEquals("a", ObjectUtility.getRandomItem(list));
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(1);
            assertEquals("b", ObjectUtility.getRandomItem(list));
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(2);
            assertEquals("c", ObjectUtility.getRandomItem(list));
        }
    }
}
