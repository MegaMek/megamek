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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the bulldozer rubble-clearing turn schedule (TacOps): the rubble of a light structure takes two turns to clear
 * and each heavier class doubles the time (medium 4, heavy 8, hardened 16), capped at 16 for any heavier (wall / ultra)
 * rubble.
 */
class RubbleClearingHandlerTest {

    @Test
    @DisplayName("Light structure rubble (level 1) clears in 2 turns")
    void lightRubbleTakesTwoTurns() {
        assertEquals(2, RubbleClearingHandler.clearingTurnsFor(1));
    }

    @Test
    @DisplayName("Medium structure rubble (level 2) clears in 4 turns")
    void mediumRubbleTakesFourTurns() {
        assertEquals(4, RubbleClearingHandler.clearingTurnsFor(2));
    }

    @Test
    @DisplayName("Heavy structure rubble (level 3) clears in 8 turns")
    void heavyRubbleTakesEightTurns() {
        assertEquals(8, RubbleClearingHandler.clearingTurnsFor(3));
    }

    @Test
    @DisplayName("Hardened structure rubble (level 4) clears in 16 turns")
    void hardenedRubbleTakesSixteenTurns() {
        assertEquals(16, RubbleClearingHandler.clearingTurnsFor(4));
    }

    @Test
    @DisplayName("Wall rubble (level 5) is capped at 16 turns")
    void wallRubbleCappedAtSixteen() {
        assertEquals(16, RubbleClearingHandler.clearingTurnsFor(5));
    }

    @Test
    @DisplayName("Ultra-rubble (level 6) is capped at 16 turns")
    void ultraRubbleCappedAtSixteen() {
        assertEquals(16, RubbleClearingHandler.clearingTurnsFor(6));
    }
}
