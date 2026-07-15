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
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the autocannon jam Edge reroll ({@link UltraWeaponHandler#rerollJamCheckWithEdge}), shared by Ultra,
 * Rotary, and rapid-fire AC handlers. A jam check may be rerolled with Edge, but only once; the helper returns the
 * rerolled value (or -1 when Edge is not used) for the caller to re-evaluate against its own jam threshold.
 */
class UltraWeaponHandlerEdgeTest {

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    private Entity attacker(boolean jamTrigger) {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        PilotOptions crewOptions = mock(PilotOptions.class);
        lenient().when(crewOptions.intOption(OptionsConstants.EDGE)).thenReturn(1);
        lenient().when(crew.getOptions()).thenReturn(crewOptions);
        lenient().when(entity.getCrew()).thenReturn(crew);
        lenient().when(entity.shouldUseEdge(OptionsConstants.EDGE_WHEN_AC_JAMS_OR_MALFUNCTIONS)).thenReturn(jamTrigger);
        return entity;
    }

    @Test
    @DisplayName("The reroll value is returned and one Edge is spent")
    void rerollReturnsValueAndSpendsEdge() {
        Entity attacker = attacker(true);
        Vector<Report> reports = new Vector<>();
        Roll clearingRoll = rollOf(7);

        int result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(clearingRoll);
            result = UltraWeaponHandler.rerollJamCheckWithEdge(attacker, 1, reports);
        }

        assertEquals(7, result, "The rerolled value should be returned for the caller to evaluate");
        verify(attacker.getCrew(), times(1)).decreaseEdge();
        assertFalse(reports.isEmpty(), "Edge reports should be added");
    }

    @Test
    @DisplayName("A reroll that still jams returns its value and spends one Edge")
    void rerollThatStillJamsReturnsValue() {
        Entity attacker = attacker(true);
        Vector<Report> reports = new Vector<>();
        Roll jammingRoll = rollOf(2);

        int result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(jammingRoll);
            result = UltraWeaponHandler.rerollJamCheckWithEdge(attacker, 1, reports);
        }

        assertEquals(2, result, "The rerolled value is returned even when it still jams");
        verify(attacker.getCrew(), times(1)).decreaseEdge();
    }

    @Test
    @DisplayName("With the trigger disabled, Edge is not used and -1 is returned")
    void triggerDisabledDoesNotUseEdge() {
        Entity attacker = attacker(false);
        Vector<Report> reports = new Vector<>();

        int result = UltraWeaponHandler.rerollJamCheckWithEdge(attacker, 1, reports);

        assertEquals(-1, result, "Without the trigger no reroll happens (-1 sentinel)");
        verify(attacker.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No reports should be added when the trigger is disabled");
    }

    @Test
    @DisplayName("Without Edge (-1) the jam always stands, at any threshold")
    void noEdgeAlwaysJams() {
        assertTrue(UltraWeaponHandler.acStillJams(-1, 2), "No Edge should leave an Ultra AC jammed");
        assertTrue(UltraWeaponHandler.acStillJams(-1, 4), "No Edge should leave a RAC jammed");
    }

    @Test
    @DisplayName("An Ultra AC (threshold 2) jams only on a reroll of 2")
    void ultraThresholdReroll() {
        assertTrue(UltraWeaponHandler.acStillJams(2, 2), "A reroll of 2 still jams an Ultra AC");
        assertFalse(UltraWeaponHandler.acStillJams(3, 2), "A reroll of 3 clears an Ultra AC jam");
        assertFalse(UltraWeaponHandler.acStillJams(11, 2), "A high reroll clears an Ultra AC jam");
    }
}
