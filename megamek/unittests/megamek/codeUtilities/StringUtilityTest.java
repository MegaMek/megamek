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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class StringUtilityTest {
    @Test
    void testIsNullOrBlankString() {
        assertTrue(StringUtility.isNullOrBlank((String) null));
        assertTrue(StringUtility.isNullOrBlank(""));
        assertTrue(StringUtility.isNullOrBlank("  "));
        assertFalse(StringUtility.isNullOrBlank("test"));
    }

    @Test
    void testIsNullOrBlankStringBuilder() {
        assertTrue(StringUtility.isNullOrBlank((StringBuilder) null));
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
