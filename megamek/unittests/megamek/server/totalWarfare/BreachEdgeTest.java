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
 * Tests for the location-breach Edge trigger ({@link TWGameManager#applyBreachEdge}). A breach check that would breach
 * a location may be rerolled with Edge, but only once.
 */
class BreachEdgeTest {

    private static final int TARGET = 10;

    private final TWGameManager gameManager = new TWGameManager();

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    private Entity breachingUnit(boolean breachTrigger) {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        PilotOptions crewOptions = mock(PilotOptions.class);
        lenient().when(crewOptions.intOption(OptionsConstants.EDGE)).thenReturn(1);
        lenient().when(crew.getOptions()).thenReturn(crewOptions);
        lenient().when(entity.getCrew()).thenReturn(crew);
        lenient().when(entity.getLocationAbbr(0)).thenReturn("CT");
        lenient().when(entity.shouldUseEdge(OptionsConstants.EDGE_WHEN_BREACH)).thenReturn(breachTrigger);
        return entity;
    }

    @Test
    @DisplayName("A breaching roll is rerolled once with Edge")
    void breachingRollIsRerolled() {
        Entity entity = breachingUnit(true);
        Vector<Report> reports = new Vector<>();
        Roll safeReroll = rollOf(4);

        int result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(safeReroll);
            // Original roll of 11 breaches (>= 10); the Edge reroll comes up safe (4).
            result = gameManager.applyBreachEdge(entity, 0, TARGET, 11, reports);
        }

        assertEquals(4, result, "The rerolled breach roll should replace the breaching roll");
        verify(entity.getCrew(), times(1)).decreaseEdge();
        assertFalse(reports.isEmpty(), "Edge-use and reroll reports should be added");
    }

    @Test
    @DisplayName("A non-breaching roll never spends Edge")
    void safeRollDoesNotUseEdge() {
        Entity entity = breachingUnit(true);
        Vector<Report> reports = new Vector<>();

        // Roll of 5 is below the target of 10, so no breach and no reroll.
        int result = gameManager.applyBreachEdge(entity, 0, TARGET, 5, reports);

        assertEquals(5, result, "A non-breaching roll should be returned unchanged");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No reports should be added for a non-breaching roll");
    }

    @Test
    @DisplayName("A breaching roll is not rerolled when the trigger is disabled")
    void breachingRollKeepsResultWhenTriggerOff() {
        Entity entity = breachingUnit(false);
        Vector<Report> reports = new Vector<>();

        int result = gameManager.applyBreachEdge(entity, 0, TARGET, 11, reports);

        assertEquals(11, result, "Without the trigger the breaching roll should stand");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No reports should be added when the trigger is disabled");
    }

    @Test
    @DisplayName("Edge is spent only once even if the reroll also breaches")
    void rerollThatAlsoBreachesSpendsOneEdge() {
        Entity entity = breachingUnit(true);
        Vector<Report> reports = new Vector<>();
        Roll breachingReroll = rollOf(12);

        int result;
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(breachingReroll);
            result = gameManager.applyBreachEdge(entity, 0, TARGET, 11, reports);
        }

        assertEquals(12, result, "The single reroll result stands even if it also breaches");
        verify(entity.getCrew(), times(1)).decreaseEdge();
    }
}
