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
package megamek.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the Construction Factor bands to the table they come from: Total Warfare, p. 168, Building Type/Original CF.
 *
 * <p>Light is 1-15, medium 16-40, heavy 41-90 and hardened 91-150. These bound what a building may be built to, and
 * the gamemaster tools refuse anything above them, so a typo here would quietly change what the rules allow.</p>
 */
class BuildingTypeTest {

    /** The types the table covers, weakest first. */
    private static final BuildingType[] TYPES_IN_THE_TABLE = { BuildingType.LIGHT, BuildingType.MEDIUM,
                                                               BuildingType.HEAVY, BuildingType.HARDENED };

    @Test
    void theBandsAreTheOnesInTheTable() {
        assertEquals(1, BuildingType.LIGHT.getMinimumCF(), "light buildings start at 1");
        assertEquals(15, BuildingType.LIGHT.getMaximumCF(), "and run to 15");
        assertEquals(16, BuildingType.MEDIUM.getMinimumCF(), "medium buildings start where light ones stop");
        assertEquals(40, BuildingType.MEDIUM.getMaximumCF(), "and run to 40");
        assertEquals(41, BuildingType.HEAVY.getMinimumCF(), "heavy buildings start where medium ones stop");
        assertEquals(90, BuildingType.HEAVY.getMaximumCF(), "and run to 90");
        assertEquals(91, BuildingType.HARDENED.getMinimumCF(), "hardened buildings start where heavy ones stop");
        assertEquals(150, BuildingType.HARDENED.getMaximumCF(), "and run to 150");
    }

    @Test
    void theBandsMeetWithoutGapsOrOverlaps() {
        for (int index = 1; index < TYPES_IN_THE_TABLE.length; index++) {
            assertEquals(TYPES_IN_THE_TABLE[index - 1].getMaximumCF() + 1, TYPES_IN_THE_TABLE[index].getMinimumCF(),
                  TYPES_IN_THE_TABLE[index] + " should start where " + TYPES_IN_THE_TABLE[index - 1]
                        + " stops, so every construction factor belongs to exactly one type");
        }
    }

    @Test
    void theAssumedFactorSitsInsideTheBand() {
        // the same page says a hex is assumed to be 15, 40, 90 or 120 when the board does not say otherwise
        for (BuildingType type : TYPES_IN_THE_TABLE) {
            assertTrue(type.getDefaultCF() >= type.getMinimumCF(),
                  type + " is assumed to be " + type.getDefaultCF() + ", which is below its own band");
            assertTrue(type.getDefaultCF() <= type.getMaximumCF(),
                  type + " is assumed to be " + type.getDefaultCF() + ", which is above its own band");
        }
    }

    @Test
    void anUnknownTypeHasNoBandToHoldTo() {
        assertEquals(-1, BuildingType.UNKNOWN.getMaximumCF(),
              "a type that is not known cannot bound anything, so nothing is refused on its account");
    }
}
