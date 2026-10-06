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

class BFSDamageTest {

    private static final String EM_DASH = "\u2014";

    @Test
    void displayStringForGroupedDamage() {
        assertEquals("5x4", new BFSDamage(5, 4).displayString());
    }

    @Test
    void displayStringForSingleGroupingOmitsCount() {
        assertEquals("5", new BFSDamage(5, 1).displayString());
    }

    @Test
    void displayStringForNoDamageIsEmDash() {
        assertEquals(EM_DASH, BFSDamage.NONE.displayString());
    }

    @Test
    void hasDamageAndTotal() {
        BFSDamage damage = new BFSDamage(5, 4);
        assertTrue(damage.hasDamage());
        assertEquals(20, damage.total());

        assertFalse(BFSDamage.NONE.hasDamage());
        assertEquals(0, BFSDamage.NONE.total());
    }

    @Test
    void parseGroupedSingleAndNone() {
        assertEquals(new BFSDamage(5, 4), BFSDamage.parse("5x4"));
        assertEquals(new BFSDamage(5, 1), BFSDamage.parse("5"));
        assertEquals(BFSDamage.NONE, BFSDamage.parse("-"));
        assertEquals(BFSDamage.NONE, BFSDamage.parse(EM_DASH));
        assertEquals(BFSDamage.NONE, BFSDamage.parse(""));
        assertEquals(BFSDamage.NONE, BFSDamage.parse(null));
    }

    @Test
    void parseIsWhitespaceAndCaseTolerant() {
        assertEquals(new BFSDamage(10, 3), BFSDamage.parse(" 10 X 3 "));
    }

    @Test
    void parseRoundTripsDisplayString() {
        BFSDamage damage = new BFSDamage(20, 2);
        assertEquals(damage, BFSDamage.parse(damage.displayString()));
    }
}
