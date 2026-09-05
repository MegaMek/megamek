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

import static megamek.common.weapons.handlers.RapidFireACWeaponHandler.RapidFireJamResult.CLEARED;
import static megamek.common.weapons.handlers.RapidFireACWeaponHandler.RapidFireJamResult.EXPLODE;
import static megamek.common.weapons.handlers.RapidFireACWeaponHandler.RapidFireJamResult.JAM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.weapons.handlers.RapidFireACWeaponHandler.RapidFireJamResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the rapid-fire AC jam outcome after an optional Edge reroll
 * ({@link RapidFireACWeaponHandler#rapidFireJamOutcome}). The standard jam level is 4; a natural 2 explodes the round
 * in the barrel unless the "kind" rapid-fire rule is in effect (jam level 2, no explosions).
 */
class RapidFireACWeaponHandlerEdgeTest {

    private static final int JAM_LEVEL = 4;
    private static final int KIND_JAM_LEVEL = 2;

    // ---- No Edge used (edgeReroll == -1): behavior must match the original rules ----

    @Test
    @DisplayName("No Edge: a roll of 3-4 jams")
    void noEdgeMidRollJams() {
        assertEquals(JAM, RapidFireACWeaponHandler.rapidFireJamOutcome(-1, 3, JAM_LEVEL, false));
        assertEquals(JAM, RapidFireACWeaponHandler.rapidFireJamOutcome(-1, 4, JAM_LEVEL, false));
    }

    @Test
    @DisplayName("No Edge: a natural 2 explodes the round in the barrel")
    void noEdgeTwoExplodes() {
        assertEquals(EXPLODE, RapidFireACWeaponHandler.rapidFireJamOutcome(-1, 2, JAM_LEVEL, false));
    }

    @Test
    @DisplayName("No Edge with the kind rapid-fire rule: a 2 jams instead of exploding")
    void noEdgeKindTwoJams() {
        assertEquals(JAM, RapidFireACWeaponHandler.rapidFireJamOutcome(-1, 2, KIND_JAM_LEVEL, true));
    }

    // ---- Edge used: the rerolled value replaces the original roll ----

    @Test
    @DisplayName("Edge: a reroll above the jam level clears the jam entirely")
    void edgeHighRerollClears() {
        assertEquals(CLEARED, RapidFireACWeaponHandler.rapidFireJamOutcome(5, 2, JAM_LEVEL, false));
        assertEquals(CLEARED, RapidFireACWeaponHandler.rapidFireJamOutcome(12, 3, JAM_LEVEL, false));
    }

    @Test
    @DisplayName("Edge: a reroll of exactly the jam level still jams")
    void edgeRerollAtJamLevelStillJams() {
        assertEquals(JAM, RapidFireACWeaponHandler.rapidFireJamOutcome(4, 2, JAM_LEVEL, false));
    }

    @Test
    @DisplayName("Edge: a reroll of 3 downgrades a natural-2 explosion to a jam")
    void edgeRerollDowngradesExplosionToJam() {
        // Original roll of 2 would explode; the reroll of 3 is still <= jam level but jams instead of exploding.
        assertEquals(JAM, RapidFireACWeaponHandler.rapidFireJamOutcome(3, 2, JAM_LEVEL, false));
    }

    @Test
    @DisplayName("Edge: a reroll of 2 still explodes (non-kind)")
    void edgeRerollTwoStillExplodes() {
        assertEquals(EXPLODE, RapidFireACWeaponHandler.rapidFireJamOutcome(2, 3, JAM_LEVEL, false));
    }

    @Test
    @DisplayName("Edge: with the kind rule, a reroll of 2 jams (no explosion)")
    void edgeRerollTwoKindJams() {
        assertEquals(JAM, RapidFireACWeaponHandler.rapidFireJamOutcome(2, 2, KIND_JAM_LEVEL, true));
    }

    @Test
    @DisplayName("Every non-cleared outcome is either JAM or EXPLODE")
    void outcomesAreExhaustive() {
        RapidFireJamResult result = RapidFireACWeaponHandler.rapidFireJamOutcome(-1, 4, JAM_LEVEL, false);
        assertEquals(JAM, result);
    }
}
