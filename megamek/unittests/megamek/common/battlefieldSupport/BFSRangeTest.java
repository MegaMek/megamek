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
package megamek.common.battlefieldSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BFSRangeTest {

    private static final String EM_DASH = "\u2014";

    @Test
    void numericRangeDisplaysAsSlashSeparated() {
        BFSRange range = new BFSRange(3, 6, 9);
        assertFalse(range.isKeyword());
        assertEquals("3/6/9", range.displayString());
    }

    @Test
    void keywordRangeUsesLabelWhenProvided() {
        assertTrue(BFSRange.KEYWORD.isKeyword());
        assertEquals("Long Tom", BFSRange.KEYWORD.displayString("Long Tom"));
    }

    @Test
    void keywordRangeWithoutLabelIsEmDash() {
        assertEquals(EM_DASH, BFSRange.KEYWORD.displayString());
        assertEquals(EM_DASH, BFSRange.KEYWORD.displayString(null));
        assertEquals(EM_DASH, BFSRange.KEYWORD.displayString(" "));
    }
}
