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
package megamek.common.planetaryConditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AtmosphereTest {

    @Test
    void fromStringExactEnumNames() {
        assertEquals(Atmosphere.VACUUM, Atmosphere.fromString("VACUUM"));
        assertEquals(Atmosphere.TRACE, Atmosphere.fromString("TRACE"));
        assertEquals(Atmosphere.THIN, Atmosphere.fromString("THIN"));
        assertEquals(Atmosphere.STANDARD, Atmosphere.fromString("STANDARD"));
        assertEquals(Atmosphere.HIGH, Atmosphere.fromString("HIGH"));
        assertEquals(Atmosphere.VERY_HIGH, Atmosphere.fromString("VERY_HIGH"));
    }

    @Test
    void fromStringCaseInsensitive() {
        assertEquals(Atmosphere.VACUUM, Atmosphere.fromString("Vacuum"));
        assertEquals(Atmosphere.VACUUM, Atmosphere.fromString("vacuum"));
        assertEquals(Atmosphere.THIN, Atmosphere.fromString("Thin"));
        assertEquals(Atmosphere.STANDARD, Atmosphere.fromString("standard"));
        assertEquals(Atmosphere.VERY_HIGH, Atmosphere.fromString("very_high"));
    }

    @Test
    void fromStringLegacyAliasLow() {
        assertEquals(Atmosphere.THIN, Atmosphere.fromString("Low"));
        assertEquals(Atmosphere.THIN, Atmosphere.fromString("low"));
        assertEquals(Atmosphere.THIN, Atmosphere.fromString("LOW"));
    }

    @Test
    void fromStringTrimsWhitespace() {
        assertEquals(Atmosphere.VACUUM, Atmosphere.fromString("  VACUUM  "));
        assertEquals(Atmosphere.THIN, Atmosphere.fromString(" Low "));
    }

    @Test
    void fromStringRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> Atmosphere.fromString(null));
    }

    @Test
    void fromStringRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Atmosphere.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> Atmosphere.fromString("   "));
    }

    @Test
    void fromStringRejectsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> Atmosphere.fromString("Bogus"));
    }
}
