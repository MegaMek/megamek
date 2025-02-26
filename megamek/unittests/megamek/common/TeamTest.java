/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import megamek.utils.MockGenerators;

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
        assertEquals(initBonus, 0);

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
        when(mockPlayer2.getCommandBonus()).thenReturn(2);

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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);

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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(4);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(3, initBonus);
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
        when(mockPlayer1.getCommandBonus()).thenReturn(0);
        when(mockPlayer2.getCommandBonus()).thenReturn(0);
        when(mockPlayer3.getCommandBonus()).thenReturn(0);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(3, initBonus);
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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);
    }

    @Test
    void testTeamWithAllPositiveInitAndAllCommandBonusAndTurnInitBonus() {
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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);
        when(mockPlayer1.getTurnInitBonus()).thenReturn(1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);
    }

    @Test
    void testTeamWithAllPositiveInitAndAllCommandBonusAndNegativeTurnInitBonus() {
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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);
        when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);
    }

    @Test
    void testTeamWithAllPositiveInitAndAllCommandBonusAndNegativeTurnInitBonusAndCompensationBonus() {
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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);
        when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);
    }

    @Test
    void testTeamWithAllPositiveInitAndAllCommandBonusAndNegativeTurnInitBonusAndNegativeCompensationBonus() {
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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);
        when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);
        when(mockPlayer1.getInitCompensationBonus()).thenReturn(-1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(-2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(-3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);
    }

    @Test
    void testTeamWithAllPositiveInitAndAllCommandBonusAndNegativeTurnInitBonusAndPositiveCompensationBonus() {
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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);
        when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);
        when(mockPlayer1.getInitCompensationBonus()).thenReturn(1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(3);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(9, initBonus);
    }

    @Test
    void testTeamWithAllPositiveInitAndAllCommandBonusAndNegativeTurnInitBonusAndPositiveCompensationBonusAndObserver() {
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
        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);
        when(mockPlayer4.getCommandBonus()).thenReturn(0);
        when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);
        when(mockPlayer4.getTurnInitBonus()).thenReturn(0);
        when(mockPlayer1.getInitCompensationBonus()).thenReturn(1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(3);
        when(mockPlayer4.getInitCompensationBonus()).thenReturn(0);

        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(9, initBonus);
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
