/*
 * Copyright (c) 2023-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.codeUItilities;

import megamek.codeUtilities.StringUtility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StringUtilityTest {
    @Test
    public void testIsNullOrBlankString() {
        final String nullString = null;
        assertTrue(StringUtility.isNullOrBlank(nullString));
        assertTrue(StringUtility.isNullOrBlank(""));
        assertTrue(StringUtility.isNullOrBlank("  "));
        assertFalse(StringUtility.isNullOrBlank("test"));
    }

    @Test
    public void testIsNullOrBlankStringBuilder() {
        final StringBuilder nullStringBuilder = null;
        assertTrue(StringUtility.isNullOrBlank(nullStringBuilder));
        final StringBuilder mockStringBuilder = mock(StringBuilder.class);
        when(mockStringBuilder.toString()).thenReturn(null);
        assertTrue(StringUtility.isNullOrBlank(mockStringBuilder));
        when(mockStringBuilder.toString()).thenReturn("");
        assertTrue(StringUtility.isNullOrBlank(mockStringBuilder));
        when(mockStringBuilder.toString()).thenReturn("  ");
        assertTrue(StringUtility.isNullOrBlank(mockStringBuilder));
        when(mockStringBuilder.toString()).thenReturn("test");
        assertFalse(StringUtility.isNullOrBlank(mockStringBuilder));
    }
}
