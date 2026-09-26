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
package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link HeatShutdownTargets}: the heat scale's Avoid numbers, the optional Avoiding Shutdown rule, and
 * restarting (issue #9025).
 */
class HeatShutdownTargetsTest {

    /** An average pilot, who gets no Avoiding Shutdown skill modifier. */
    private static final int REGULAR_PILOTING = 5;

    @ParameterizedTest
    @CsvSource({
          // the standard heat scale (TW p.102)
          "14, 4", "17, 4", "18, 6", "22, 8", "26, 10", "29, 10",
          // the Expanded Heat Scale (TO:AR p.102)
          "30, 12", "34, 14", "38, 16", "42, 18", "46, 20", "49, 20" })
    void theAvoidNumberFollowsTheHeatScale(int heat, int expectedAvoidNumber) {
        assertEquals(expectedAvoidNumber, HeatShutdownTargets.avoidNumber(heat));
    }

    @ParameterizedTest
    @CsvSource({ "14, 4", "22, 8", "30, 12", "34, 14", "46, 20" })
    void withoutAvoidingShutdownTheRollIsThePlainAvoidNumber(int heat, int expectedTarget) {
        // Issue #9025: the Expanded Heat Scale alone must not bring in the Avoiding Shutdown modifiers.
        assertEquals(expectedTarget,
              HeatShutdownTargets.shutdownAvoidance(heat, REGULAR_PILOTING, 0, false).getValue());
    }

    @ParameterizedTest
    @CsvSource({ "14, -1", "22, 3", "30, 7", "34, 9", "46, 15" })
    void avoidingShutdownTakesFiveOffTheAvoidNumber(int heat, int expectedTarget) {
        assertEquals(expectedTarget,
              HeatShutdownTargets.shutdownAvoidance(heat, REGULAR_PILOTING, 0, true).getValue());
    }

    @Test
    void theBooksVeteranExampleComesToEight() {
        // TO:AR p.102: at heat 34 the example pilot rolls 14 - 5 - 1 = 8; a skill of 3 carries that -1.
        assertEquals(8, HeatShutdownTargets.shutdownAvoidance(34, 3, 0, true).getValue());
    }

    @ParameterizedTest
    @CsvSource({ "0, -2", "1, -2", "2, -1", "3, -1", "4, 0", "5, 0", "6, 1", "7, 1" })
    void thePilotSkillModifierFollowsTheBooksTable(int pilotingSkill, int expectedModifier) {
        assertEquals(expectedModifier, HeatShutdownTargets.pilotSkillModifier(pilotingSkill));
    }

    @Test
    void theHotDogAbilityLowersTheRoll() {
        assertEquals(3, HeatShutdownTargets.shutdownAvoidance(14, REGULAR_PILOTING, 1, false).getValue());
        assertEquals(-2, HeatShutdownTargets.shutdownAvoidance(14, REGULAR_PILOTING, 1, true).getValue());
    }

    @ParameterizedTest
    @CsvSource({ "14, 4", "30, 12", "34, 14", "46, 20" })
    void restartingIsAlwaysThePlainAvoidNumber(int heat, int expectedTarget) {
        // TW p.102: restart on 2D6 against the Avoid number. Avoiding Shutdown covers only avoiding one.
        assertEquals(expectedTarget, HeatShutdownTargets.restart(heat, 0).getValue());
    }
}
