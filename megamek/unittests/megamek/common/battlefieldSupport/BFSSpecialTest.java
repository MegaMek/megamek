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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class BFSSpecialTest {

    @Test
    void parseNumericSpecialSplitsTrailingDigits() {
        BFSSpecial special = BFSSpecial.parse("IF2");
        assertEquals("IF", special.code());
        assertEquals("2", special.value());
        assertTrue(special.hasNumericValue());
        assertEquals(Optional.of(2), special.intValue());
    }

    @Test
    void parseParenthesizedSpecialExtractsTypeToken() {
        BFSSpecial special = BFSSpecial.parse("Artillery (LT)");
        assertEquals("Artillery", special.code());
        assertEquals("LT", special.value());
        assertFalse(special.hasNumericValue());
        assertEquals(Optional.empty(), special.intValue());
    }

    @Test
    void parseValuelessSpecial() {
        BFSSpecial special = BFSSpecial.parse("No Turret");
        assertEquals("No Turret", special.code());
        assertNull(special.value());
        assertFalse(special.hasValue());
    }

    @Test
    void parseBlankOrNullIsNull() {
        assertNull(BFSSpecial.parse(null));
        assertNull(BFSSpecial.parse("  "));
    }

    @Test
    void displayStringAppendsNumericAndParenthesizesTypeTokens() {
        assertEquals("IF2", BFSSpecial.of("IF", 2).displayString());
        assertEquals("Artillery (LT)", BFSSpecial.of("Artillery", "LT").displayString());
        assertEquals("TAG", BFSSpecial.of("TAG").displayString());
    }

    @Test
    void displayStringRoundTripsThroughParse() {
        for (String token : new String[] { "IF2", "APC1", "Artillery (LT)", "No Turret", "TAG" }) {
            assertEquals(token, BFSSpecial.parse(token).displayString());
        }
    }

    @Test
    void knownTypeResolvesRecognizedCodesAndIgnoresUnknown() {
        assertEquals(Optional.of(BFSSpecialType.INDIRECT_FIRE), BFSSpecial.parse("IF2").knownType());
        assertEquals(Optional.of(BFSSpecialType.NO_TURRET), BFSSpecial.parse("No Turret").knownType());
        assertTrue(BFSSpecial.of("XYZ").knownType().isEmpty());
        assertFalse(BFSSpecial.of("XYZ").isKnown());
    }

    @Test
    void numericValueOutsideIntegerRangeIsEmpty() {
        BFSSpecial special = BFSSpecial.of("ECM", "999999999999999999999999");

        assertTrue(special.hasNumericValue());
        assertEquals(Optional.empty(), special.intValue());
    }
}
