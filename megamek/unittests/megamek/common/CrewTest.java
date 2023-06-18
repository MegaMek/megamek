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

import megamek.common.battlevalue.BVCalculator;
import megamek.common.options.GameOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 10/30/13 9:25 AM
 */
public class CrewTest {

    @Test
    public void testInfantryCrewFatigue() {
        Infantry inf = mock(Infantry.class);
        Crew crew = getInfantryCrewWithCombatTurns(17);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(5);
        inf.getCrew().setPiloting(8);
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertTrue(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(4);
        inf.getCrew().setPiloting(2);
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertFalse(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(1);
        assertFalse(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setFatigue(15);
        assertTrue(inf.getCrew().isPilotingFatigued());

        crew = getInfantryCrewWithCombatTurns(16);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(2);
        inf.getCrew().setPiloting(8);
        assertFalse(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setFatigue(4);
        assertFalse(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setFatigue(5);
        assertTrue(inf.getCrew().isPilotingFatigued());
    }

    @Test
    public void testMekCrewFatigue() {
        Mech inf = mock(Mech.class);
        Crew crew = getMekCrewWithCombatTurns(17);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(5);
        inf.getCrew().setPiloting(8);
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertTrue(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(4);
        inf.getCrew().setPiloting(2);
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertFalse(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(1);
        assertTrue(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setFatigue(15);
        assertTrue(inf.getCrew().isPilotingFatigued());

        crew = getMekCrewWithCombatTurns(16);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(2);
        inf.getCrew().setPiloting(8);
        assertTrue(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setFatigue(4);
        assertTrue(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setFatigue(5);
        assertTrue(inf.getCrew().isPilotingFatigued());
    }

    Crew getInfantryCrewWithCombatTurns(int turnsActive) {
        return getCrewWithCombatTurns(turnsActive, CrewType.INFANTRY_CREW);
    }

    Crew getMekCrewWithCombatTurns(int turnsActive) {
        return getCrewWithCombatTurns(turnsActive, CrewType.SINGLE);
    }

    Crew getCrewWithCombatTurns(int turnsActive, CrewType crewType) {
        Crew crew = new Crew(crewType);
        for (int rounds = 0; rounds < turnsActive; rounds++) {
            crew.incrementFatigueCount();
        }
        return crew;
    }

    @Test
    public void testGetBVSkillMultiplier() {
        int gunnery = 4;
        int piloting = 5;

        // Test the default case.
        Game mockGame = null;
        double expected = 1.0;
        double actual = BVCalculator.bvSkillMultiplier(gunnery, piloting);
        assertEquals(expected, actual, 0.001);

        mockGame = mock(Game.class);
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);
        expected = 1.0;
        actual = BVCalculator.bvSkillMultiplier(gunnery, piloting);
        assertEquals(expected, actual, 0.001);

        // Test a 3/4 pilot.
        gunnery = 3;
        piloting = 4;
        when(mockOptions.booleanOption(eq("alternate_pilot_bv_mod"))).thenReturn(false);
        expected = 1.32;
        actual = BVCalculator.bvSkillMultiplier(gunnery, piloting);
        assertEquals(expected, actual, 0.001);

        // Test a 5/6 pilot.
        gunnery = 5;
        piloting = 6;
        when(mockOptions.booleanOption(eq("alternate_pilot_bv_mod"))).thenReturn(false);
        expected = 0.86;
        actual = BVCalculator.bvSkillMultiplier(gunnery, piloting);
        assertEquals(expected, actual, 0.001);

        // Test a 2/6 pilot.
        gunnery = 2;
        piloting = 6;
        when(mockOptions.booleanOption(eq("alternate_pilot_bv_mod"))).thenReturn(false);
        expected = 1.35;
        actual = BVCalculator.bvSkillMultiplier(gunnery, piloting);
        assertEquals(expected, actual, 0.001);

        // Test a 0/0 pilot.
        gunnery = 0;
        piloting = 0;
        when(mockOptions.booleanOption(eq("alternate_pilot_bv_mod"))).thenReturn(false);
        expected = 2.42;
        actual = BVCalculator.bvSkillMultiplier(gunnery, piloting);
        assertEquals(expected, actual, 0.001);
    }
}
