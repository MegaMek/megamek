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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the zip line Edge rerolls. A failed check may be rerolled with Edge, but only once.
 */
class ZiplineEdgeTest {

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    private Entity unit(String trigger, boolean enabled) {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        PilotOptions crewOptions = mock(PilotOptions.class);
        lenient().when(crewOptions.intOption(OptionsConstants.EDGE)).thenReturn(1);
        lenient().when(crew.getOptions()).thenReturn(crewOptions);
        lenient().when(entity.getCrew()).thenReturn(crew);
        lenient().when(entity.shouldUseEdge(trigger)).thenReturn(enabled);
        return entity;
    }


    private static final int TARGET = 7;

    private PilotingRollData target() {
        PilotingRollData rollTarget = mock(PilotingRollData.class);
        lenient().when(rollTarget.getValue()).thenReturn(TARGET);
        return rollTarget;
    }

    @Test
    @DisplayName("A failed zip line check is rerolled, spending one Edge")
    void failedCheckIsRerolled() {
        Entity entity = unit(OptionsConstants.EDGE_WHEN_ZIPLINE, true);
        Vector<Report> reports = new Vector<>();
        Roll failed = rollOf(4);
        Roll reroll = rollOf(9);

        Roll result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(reroll);
            result = TWGameManager.applyZiplineEdge(entity, target(), failed, reports);
        }

        assertSame(reroll, result, "The rerolled result should replace the failed roll");
        verify(entity.getCrew(), times(1)).decreaseEdge();
        assertFalse(reports.isEmpty(), "An Edge-use report should be added");
    }

    @Test
    @DisplayName("A successful zip line check never spends Edge")
    void successfulCheckDoesNotUseEdge() {
        Entity entity = unit(OptionsConstants.EDGE_WHEN_ZIPLINE, true);
        Vector<Report> reports = new Vector<>();
        Roll passed = rollOf(9);

        Roll result = TWGameManager.applyZiplineEdge(entity, target(), passed, reports);

        assertSame(passed, result, "A successful check should be returned unchanged");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty());
    }

    @Test
    @DisplayName("A failed zip line check is not rerolled when the trigger is disabled")
    void failedCheckKeepsResultWhenTriggerOff() {
        Entity entity = unit(OptionsConstants.EDGE_WHEN_ZIPLINE, false);
        Vector<Report> reports = new Vector<>();
        Roll failed = rollOf(4);

        Roll result = TWGameManager.applyZiplineEdge(entity, target(), failed, reports);

        assertSame(failed, result, "Without the trigger the failed roll should stand");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty());
    }
}
