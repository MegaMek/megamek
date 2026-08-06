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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import megamek.common.compute.Compute;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Regression test for the Edge blackout (consciousness) reroll: a failed consciousness roll may be rerolled with Edge
 * only once, even when the crew still has multiple Edge points remaining.
 *
 * @see TWGameManager#resolveCrewDamage(megamek.common.units.Entity, int, int)
 */
class CrewBlackoutEdgeTest {

    private final TWGameManager gameManager = new TWGameManager();

    @BeforeEach
    void setUp() {
        gameManager.getGame().setOptions(new GameOptions());
        // Edge rerolls require the Edge game option to be enabled.
        gameManager.getGame().getOptions().getOption(OptionsConstants.EDGE).setValue(true);
    }

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    @Test
    @DisplayName("A failed blackout check is rerolled with Edge at most once, even with several Edge points")
    void blackoutCheckIsRerolledOnlyOnce() {
        // Real crew so Edge is really decremented and hasEdgeRemaining() reflects it; five points available.
        Crew crew = new Crew(CrewType.SINGLE);
        crew.getOptions().getOption(OptionsConstants.EDGE).setValue(5);
        crew.getOptions().getOption(OptionsConstants.EDGE_WHEN_KO).setValue(true);
        crew.setHits(1, 0);

        Mek mek = mock(Mek.class);
        when(mek.getCrew()).thenReturn(crew);
        when(mek.isTargetable()).thenReturn(true);
        // Tie the Edge decision to the real (depleting) crew Edge state.
        lenient().when(mek.shouldUseEdge(OptionsConstants.EDGE_WHEN_KO)).thenAnswer(inv -> crew.hasEdgeRemaining());
        lenient().when(mek.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_KO)).thenReturn(false);

        Roll failedRoll = rollOf(2);

        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            // Every consciousness roll fails (2 vs. a target of 3 for a single hit)...
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(failedRoll);
            // ...but keep the real consciousness-number lookup.
            mockedCompute.when(() -> Compute.getConsciousnessNumber(1)).thenCallRealMethod();

            gameManager.resolveCrewDamage(mek, 1, 0);
        }

        // Exactly one Edge point should have been spent (5 -> 4). Before the fix this drained all the way to 0.
        assertEquals(4, crew.getOptions().intOption(OptionsConstants.EDGE),
              "Only one Edge point should be spent rerolling a single blackout check");
    }
}
