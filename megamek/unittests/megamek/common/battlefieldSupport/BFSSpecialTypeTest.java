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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BFSSpecialTypeTest {

    @Test
    void forCodeMatchesCanonicalCaseInsensitively() {
        assertEquals(BFSSpecialType.INDIRECT_FIRE, BFSSpecialType.forCode("if").orElseThrow());
        assertEquals(BFSSpecialType.ARTILLERY, BFSSpecialType.forCode("Artillery").orElseThrow());
    }

    @Test
    void forCodeMatchesAliasesAndIgnoresSpacingAndPunctuation() {
        assertEquals(BFSSpecialType.NO_TURRET, BFSSpecialType.forCode("noturret").orElseThrow());
        assertEquals(BFSSpecialType.NO_TURRET, BFSSpecialType.forCode("No Turret").orElseThrow());
        assertEquals(BFSSpecialType.CRIT_SEEKER, BFSSpecialType.forCode("crit seeker").orElseThrow());
    }

    @Test
    void forCodeReturnsEmptyForUnknown() {
        assertEquals(java.util.Optional.empty(), BFSSpecialType.forCode("Nonexistent"));
        assertEquals(java.util.Optional.empty(), BFSSpecialType.forCode(null));
    }

    @Test
    void takesValueReflectsParameterizedSpecials() {
        assertEquals(true, BFSSpecialType.INDIRECT_FIRE.takesValue());
        assertEquals(true, BFSSpecialType.ARTILLERY.takesValue());
        assertEquals(false, BFSSpecialType.TAG.takesValue());
    }

    @Test
    void artilleryTypeParsesCodesAndDisplayNames() {
        assertEquals(BFSArtilleryType.LONG_TOM, BFSArtilleryType.fromString("LT"));
        assertEquals(BFSArtilleryType.THUMPER, BFSArtilleryType.fromString("Thumper"));
        assertEquals(BFSArtilleryType.SNIPER, BFSArtilleryType.fromString("s"));
        assertNull(BFSArtilleryType.fromString("Bogus"));
    }
}
