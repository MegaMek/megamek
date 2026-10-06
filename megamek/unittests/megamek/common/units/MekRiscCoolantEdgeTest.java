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

package megamek.common.units;

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
import megamek.common.rolls.Roll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the RISC Emergency Coolant System Edge reroll ({@link Mek#rerollRiscCoolantWithEdge}), which shares the
 * {@code EDGE_WHEN_RISC_FAIL} trigger with the RISC laser malfunctions. A failed coolant check may be rerolled with
 * Edge, but only once.
 */
class MekRiscCoolantEdgeTest {

    private static final int REQUIRED_ROLL = 8;

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    private Entity coolantMek(boolean riscTrigger) {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        PilotOptions crewOptions = mock(PilotOptions.class);
        lenient().when(crewOptions.intOption(OptionsConstants.EDGE)).thenReturn(1);
        lenient().when(crew.getOptions()).thenReturn(crewOptions);
        lenient().when(entity.getCrew()).thenReturn(crew);
        lenient().when(entity.shouldUseEdge(OptionsConstants.EDGE_WHEN_RISC_FAIL)).thenReturn(riscTrigger);
        return entity;
    }

    @Test
    @DisplayName("A failed coolant check is rerolled with Edge")
    void failedCheckIsRerolled() {
        Entity entity = coolantMek(true);
        Vector<Report> reports = new Vector<>();
        Roll failed = rollOf(5);
        Roll reroll = rollOf(10);

        Roll result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(reroll);
            result = Mek.rerollRiscCoolantWithEdge(entity, REQUIRED_ROLL, failed, reports);
        }

        assertSame(reroll, result, "The rerolled result should replace the failed roll");
        verify(entity.getCrew(), times(1)).decreaseEdge();
        assertFalse(reports.isEmpty(), "An Edge-use report should be added");
    }

    @Test
    @DisplayName("A reroll that also fails is still returned, spending exactly one Edge")
    void rerollThatAlsoFailsSpendsOneEdge() {
        Entity entity = coolantMek(true);
        Vector<Report> reports = new Vector<>();
        Roll failed = rollOf(5);
        Roll rerollAlsoFails = rollOf(4);

        Roll result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(rerollAlsoFails);
            result = Mek.rerollRiscCoolantWithEdge(entity, REQUIRED_ROLL, failed, reports);
        }

        assertSame(rerollAlsoFails, result, "The single reroll stands even when it also fails");
        verify(entity.getCrew(), times(1)).decreaseEdge();
    }

    @Test
    @DisplayName("A successful coolant check never spends Edge")
    void passedCheckDoesNotUseEdge() {
        Entity entity = coolantMek(true);
        Vector<Report> reports = new Vector<>();
        Roll passed = rollOf(10);

        Roll result = Mek.rerollRiscCoolantWithEdge(entity, REQUIRED_ROLL, passed, reports);

        assertSame(passed, result, "A successful roll should be returned unchanged");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No Edge-use report should be added on a successful check");
    }

    @Test
    @DisplayName("A failed coolant check is not rerolled when the trigger is disabled")
    void failedCheckKeepsResultWhenTriggerOff() {
        Entity entity = coolantMek(false);
        Vector<Report> reports = new Vector<>();
        Roll failed = rollOf(5);

        Roll result = Mek.rerollRiscCoolantWithEdge(entity, REQUIRED_ROLL, failed, reports);

        assertSame(failed, result, "Without the trigger the failed roll should stand");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No Edge-use report should be added when the trigger is disabled");
    }
}
