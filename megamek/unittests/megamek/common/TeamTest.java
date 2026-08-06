/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import megamek.utils.MockGenerators;
import org.junit.jupiter.api.Test;

/**
 * @author Nicholas Walczak (walczak@cs.umn.edu)
 * @since 06/10/14
 */
class TeamTest {
    @Test
    void testTeamOfThreeWithNoBonus() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        // Sanity test
        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(0, initBonus);

        assertEquals(3, testTeam.size());
        assertEquals(3, testTeam.getNonObserverSize());
        assertFalse(testTeam.isObserverTeam());
    }

    @Test
    void testTeamWithNegativeInitBonus() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(-1, initBonus);
    }

    @Test
    void testTeamWithTwoNegativeAndOneZero() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(0);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(0, initBonus);
    }

    @Test
    void testTeamWithTwoNegativeAndOnePositive() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(1, initBonus);
    }

    @Test
    void testTeamWithACommandBonus() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(1, initBonus);
    }

    @Test
    void testTeamWithAllNegativeAndTwoCommandBonus() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(1, initBonus);
    }

    @Test
    void testTeamWithAllNegativeInitAndAllCommandBonus() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(4);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(3, initBonus);  // max(-1,-2,-3) + max(1,2,4) = -1 + 4 = 3
    }

    @Test
    void testTeamWithAllPositiveInitAndNoCommandBonus() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        // Command bonuses default to 0 (unmocked)

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(3, initBonus);  // max(1,2,3) = 3
    }

    @Test
    void testTeamWithAllPositiveInitAndAllCommandBonus() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        // Per Xotl ruling: only highest positive applies (across all types)
        assertEquals(3, initBonus);
    }

    @Test
    void testTeamWithConstantCommandAndQuirkBonuses() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(3);
        when(mockPlayer1.getQuirkInitBonus()).thenReturn(1);
        when(mockPlayer2.getQuirkInitBonus()).thenReturn(2);
        when(mockPlayer3.getQuirkInitBonus()).thenReturn(3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        // Per Xotl ruling: only highest positive applies (across all types)
        assertEquals(3, initBonus);
    }

    @Test
    void testTeamWithConstantAndCommandBonusesNoQuirk() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(3);
        // Quirk bonus is 0 by default (not mocked)

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        // Per Xotl ruling: only highest positive applies (across all types)
        assertEquals(3, initBonus);
    }

    @Test
    void testTeamWithConstantAndCommandBonusesWithCompensation() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = true;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(3);
        // Quirk bonus is 0 by default (not mocked)

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        // Per Xotl ruling: only highest positive applies (across all types)
        assertEquals(3, initBonus);
    }

    @Test
    void testTeamWithConstantAndCommandBonusesWithNegativeCompensation() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = true;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(3);
        // Quirk bonus is 0 by default (not mocked)
        when(mockPlayer1.getInitCompensationBonus()).thenReturn(-1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(-2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(-3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        // Per Xotl ruling: only highest positive applies, negatives stack
        // Highest positive is 3, negatives sum to -6, but comp bonus negative is clamped
        assertEquals(3, initBonus);
    }

    @Test
    void testTeamWithConstantAndCommandBonusesWithPositiveCompensation() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = true;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(3);
        // Quirk bonus is 0 by default (not mocked)
        when(mockPlayer1.getInitCompensationBonus()).thenReturn(1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        // Per Xotl ruling: only highest positive applies (across all types)
        assertEquals(3, initBonus);
    }

    @Test
    void testTeamInitBonusIgnoresObserverPlayers() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();
        Player mockPlayer4 = MockGenerators.mockPlayer();
        when(mockPlayer4.isObserver()).thenReturn(true);

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);
        testTeam.addPlayer(mockPlayer4);

        boolean useInitCompBonus = true;

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer4.getConstantInitBonus()).thenReturn(0);
        when(mockPlayer1.getCommandConsoleBonus()).thenReturn(1);
        when(mockPlayer2.getCommandConsoleBonus()).thenReturn(2);
        when(mockPlayer3.getCommandConsoleBonus()).thenReturn(3);
        when(mockPlayer4.getCommandConsoleBonus()).thenReturn(0);
        // Note: quirk bonus is 0 by default (unmocked)
        when(mockPlayer1.getInitCompensationBonus()).thenReturn(1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(3);
        when(mockPlayer4.getInitCompensationBonus()).thenReturn(0);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        // Per Xotl ruling: only highest positive applies (across all types)
        assertEquals(3, initBonus);
    }

    @Test
    void addingNullPlayerDoesNotImpactTeamSize() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer1 = MockGenerators.mockPlayer();
        Player mockPlayer2 = MockGenerators.mockPlayer();
        Player mockPlayer3 = MockGenerators.mockPlayer();
        Player mockPlayer4 = MockGenerators.mockPlayer();
        when(mockPlayer4.isObserver()).thenReturn(true);

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);
        testTeam.addPlayer(mockPlayer4);

        testTeam.addPlayer(null);
        assertEquals(4, testTeam.size());
    }

    @Test
    void testTeamAsObserverTeam() {
        Team testTeam = new Team(1);
        assertTrue(testTeam.isEmpty());

        Player mockPlayer = MockGenerators.mockPlayer();
        when(mockPlayer.isObserver()).thenReturn(true);

        testTeam.addPlayer(mockPlayer);

        assertEquals(0, testTeam.getNonObserverSize());
        assertTrue(testTeam.isObserverTeam());
    }
}
