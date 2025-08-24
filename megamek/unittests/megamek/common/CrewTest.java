/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.battleValue.BVCalculator;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 10/30/13 9:25 AM
 */
class CrewTest {

    @Test
    void testInfantryCrewFatigue() {
        Infantry inf = mock(Infantry.class);
        Crew crew = getInfantryCrewWithCombatTurns(17);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(5, crew.getCrewType().getGunnerPos());
        inf.getCrew().setPiloting(8, crew.getCrewType().getPilotPos());
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertTrue(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(4, crew.getCrewType().getGunnerPos());
        inf.getCrew().setPiloting(2, crew.getCrewType().getPilotPos());
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertFalse(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(1, crew.getCrewType().getGunnerPos());
        assertFalse(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setCrewFatigue(15, 0);
        assertTrue(inf.getCrew().isPilotingFatigued());

        crew = getInfantryCrewWithCombatTurns(16);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(2, crew.getCrewType().getGunnerPos());
        inf.getCrew().setPiloting(8, crew.getCrewType().getPilotPos());
        assertFalse(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setCrewFatigue(4, 0);
        assertFalse(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setCrewFatigue(5, 0);
        assertTrue(inf.getCrew().isPilotingFatigued());
    }

    @Test
    void testMekCrewFatigue() {
        Mek inf = mock(Mek.class);
        Crew crew = getMekCrewWithCombatTurns(17);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(5, crew.getCrewType().getGunnerPos());
        inf.getCrew().setPiloting(8, crew.getCrewType().getPilotPos());
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertTrue(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(4, crew.getCrewType().getGunnerPos());
        inf.getCrew().setPiloting(2, crew.getCrewType().getPilotPos());
        assertTrue(inf.getCrew().isPilotingFatigued());
        assertFalse(inf.getCrew().isGunneryFatigued());

        inf.getCrew().setGunnery(1, crew.getCrewType().getGunnerPos());
        assertTrue(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setCrewFatigue(15, 0);
        assertTrue(inf.getCrew().isPilotingFatigued());

        crew = getMekCrewWithCombatTurns(16);
        when(inf.getCrew()).thenReturn(crew);
        inf.getCrew().setGunnery(2, crew.getCrewType().getGunnerPos());
        inf.getCrew().setPiloting(8, crew.getCrewType().getPilotPos());
        assertTrue(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setCrewFatigue(4, 0);
        assertTrue(inf.getCrew().isPilotingFatigued());

        inf.getCrew().setCrewFatigue(5, 0);
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
    void testGetBVSkillMultiplier() {
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
