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

package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the Rotary AC jam handling expanded to use the Edge reroll: the per-mode jam threshold
 * ({@link RACHandler#racJamThreshold}) and the shared jam decision ({@link UltraWeaponHandler#acStillJams}).
 */
class RACHandlerEdgeTest {

    @ParameterizedTest(name = "{0} shots -> jam threshold {1}")
    @CsvSource({ "6,4", "5,3", "4,3", "3,2", "2,2", "1,0", "0,0", "7,0" })
    @DisplayName("Jam threshold matches the fire mode")
    void jamThresholdByShots(int shots, int expectedThreshold) {
        assertEquals(expectedThreshold, RACHandler.racJamThreshold(shots));
    }

    @Test
    @DisplayName("At 6 shots (threshold 4) a reroll above 4 clears the jam")
    void sixShotThresholdReroll() {
        int threshold = RACHandler.racJamThreshold(6);
        assertTrue(UltraWeaponHandler.acStillJams(4, threshold), "A reroll of 4 still jams at threshold 4");
        assertTrue(UltraWeaponHandler.acStillJams(2, threshold), "A reroll of 2 still jams at threshold 4");
        assertFalse(UltraWeaponHandler.acStillJams(5, threshold), "A reroll of 5 clears the jam at threshold 4");
    }

    @Test
    @DisplayName("At 2 shots (threshold 2) a reroll above 2 clears the jam")
    void twoShotThresholdReroll() {
        int threshold = RACHandler.racJamThreshold(2);
        assertTrue(UltraWeaponHandler.acStillJams(2, threshold), "A reroll of 2 still jams at threshold 2");
        assertFalse(UltraWeaponHandler.acStillJams(3, threshold), "A reroll of 3 clears the jam at threshold 2");
    }

    @Test
    @DisplayName("Without Edge the jam always stands")
    void noEdgeStillJams() {
        assertTrue(UltraWeaponHandler.acStillJams(-1, RACHandler.racJamThreshold(6)));
        assertTrue(UltraWeaponHandler.acStillJams(-1, RACHandler.racJamThreshold(2)));
    }
}
