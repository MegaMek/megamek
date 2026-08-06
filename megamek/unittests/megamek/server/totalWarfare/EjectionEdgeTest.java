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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the failed-ejection Edge trigger ({@link TWGameManager#applyEjectionEdge}). A failed ejection roll may be
 * rerolled with Edge, but only once.
 */
class EjectionEdgeTest {

    private static final int TARGET = 8;

    private final TWGameManager gameManager = new TWGameManager();

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        lenient().when(roll.getIntValue()).thenReturn(value);
        return roll;
    }

    /**
     * Builds a mocked ejecting unit whose failed-ejection Edge trigger is set as requested, with a crew that returns
     * {@code rerollResult} from any piloting-skill (ejection) reroll.
     */
    private Entity ejectingUnit(boolean ejectTrigger, Roll rerollResult) {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        PilotOptions crewOptions = mock(PilotOptions.class);
        lenient().when(crewOptions.intOption(OptionsConstants.EDGE)).thenReturn(1);
        lenient().when(crew.getOptions()).thenReturn(crewOptions);
        lenient().when(crew.rollPilotingSkill()).thenReturn(rerollResult);
        lenient().when(entity.getCrew()).thenReturn(crew);
        lenient().when(entity.shouldUseEdge(OptionsConstants.EDGE_WHEN_EJECT_FAILS)).thenReturn(ejectTrigger);
        return entity;
    }

    private PilotingRollData target() {
        PilotingRollData rollTarget = mock(PilotingRollData.class);
        lenient().when(rollTarget.getValue()).thenReturn(TARGET);
        return rollTarget;
    }

    @Test
    @DisplayName("A failed ejection roll is rerolled once, spending one Edge")
    void failedRollIsRerolledWithEdge() {
        Roll reroll = rollOf(10);
        Entity entity = ejectingUnit(true, reroll);
        Vector<Report> reports = new Vector<>();

        Roll result = gameManager.applyEjectionEdge(entity, target(), rollOf(4), reports);

        assertSame(reroll, result, "The rerolled result should replace the failed roll");
        verify(entity.getCrew(), times(1)).decreaseEdge();
        assertFalse(reports.isEmpty(), "An Edge-use report should be added");
    }

    @Test
    @DisplayName("A successful ejection roll never spends Edge")
    void passedRollDoesNotUseEdge() {
        Roll passed = rollOf(10);
        Entity entity = ejectingUnit(true, rollOf(2));
        Vector<Report> reports = new Vector<>();

        Roll result = gameManager.applyEjectionEdge(entity, target(), passed, reports);

        assertSame(passed, result, "A successful roll should be returned unchanged");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No Edge-use report should be added on a successful roll");
    }

    @Test
    @DisplayName("A failed ejection roll is not rerolled when the trigger is disabled")
    void failedRollKeepsResultWhenTriggerOff() {
        Roll failed = rollOf(4);
        Entity entity = ejectingUnit(false, rollOf(10));
        Vector<Report> reports = new Vector<>();

        Roll result = gameManager.applyEjectionEdge(entity, target(), failed, reports);

        assertSame(failed, result, "Without the trigger the failed roll should stand");
        verify(entity.getCrew(), never()).decreaseEdge();
        assertTrue(reports.isEmpty(), "No Edge-use report should be added when the trigger is disabled");
    }
}
