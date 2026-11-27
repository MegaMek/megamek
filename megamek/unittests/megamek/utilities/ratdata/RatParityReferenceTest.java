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
package megamek.utilities.ratdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Checks that test cases written by the website's parity script read back as the same RAT Generator request.
 */
class RatParityReferenceTest {

    @Test
    void caseWithEveryFilterIsRead() {
        RatParityReference.ParityCase parityCase = RatParityReference.ParityCase.parse(
              "FS 3039 C Tank recon,cavalry 3 1,2 HOVER,TRACKED 2");

        assertEquals("FS", parityCase.faction());
        assertEquals(3039, parityCase.year());
        assertEquals("C", parityCase.rating());
        assertEquals(UnitType.TANK, parityCase.unitType());
        assertEquals(List.of(MissionRole.RECON, MissionRole.CAVALRY), parityCase.roles());
        assertEquals(3, parityCase.roleStrictness());
        assertEquals(List.of(1, 2), parityCase.weightClasses());
        assertEquals(List.of(EntityMovementMode.HOVER, EntityMovementMode.TRACKED), parityCase.movementModes());
        assertEquals(2, parityCase.networkMask());
    }

    @Test
    void dashesMeanNoneAndUnderscoresMeanSpaces() {
        RatParityReference.ParityCase parityCase = RatParityReference.ParityCase.parse(
              "CSJ 3052 Front_Line Small_Craft - 1 - - 0");

        assertEquals("Front Line", parityCase.rating());
        assertEquals(UnitType.SMALL_CRAFT, parityCase.unitType());
        assertTrue(parityCase.roles().isEmpty());
        assertTrue(parityCase.weightClasses().isEmpty());
        assertTrue(parityCase.movementModes().isEmpty());
    }

    @Test
    void pinnedRatingIsNull() {
        assertNull(RatParityReference.ParityCase.parse("AB 3076 - Mek - 2 - - 0").rating());
    }

    @Test
    void badLinesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> RatParityReference.ParityCase.parse("FS 3039 C Mek"));
        assertThrows(IllegalArgumentException.class,
              () -> RatParityReference.ParityCase.parse("FS 3039 C Hovercraft - 1 - - 0"));
        assertThrows(IllegalArgumentException.class,
              () -> RatParityReference.ParityCase.parse("FS 3039 C Mek bodyguard 1 - - 0"));
        assertThrows(IllegalArgumentException.class,
              () -> RatParityReference.ParityCase.parse("FS soon C Mek - 1 - - 0"));
    }
}
