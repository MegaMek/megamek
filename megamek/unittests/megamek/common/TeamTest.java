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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicholas Walczak (walczak@cs.umn.edu)
 * @since 06/10/14
 */
public class TeamTest {

    @Test
    public void testTotalInitBonus() {
        Team testTeam = new Team(1);
        // Setup Player 1
        Player mockPlayer1 = mock(Player.class);
        when(mockPlayer1.getConstantInitBonus()).thenReturn(0);
        when(mockPlayer1.getTurnInitBonus()).thenReturn(0);
        when(mockPlayer1.getInitCompensationBonus()).thenReturn(0);
        when(mockPlayer1.getCommandBonus()).thenReturn(0);
        // Setup Player 2
        Player mockPlayer2 = mock(Player.class);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(0);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(0);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(0);
        when(mockPlayer2.getCommandBonus()).thenReturn(0);
        // Setup Player 3
        Player mockPlayer3 = mock(Player.class);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(0);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(0);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(0);
        when(mockPlayer2.getCommandBonus()).thenReturn(0);

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        boolean useInitCompBonus = false;

        // Sanity test
        int initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(initBonus, 0);

        when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(-1, initBonus);

        when(mockPlayer1.getConstantInitBonus()).thenReturn(0);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(0, initBonus);

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(1, initBonus);

        when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(1, initBonus);

        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(1, initBonus);

        when(mockPlayer3.getCommandBonus()).thenReturn(4);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(3, initBonus);

        when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        when(mockPlayer1.getCommandBonus()).thenReturn(0);
        when(mockPlayer2.getCommandBonus()).thenReturn(0);
        when(mockPlayer3.getCommandBonus()).thenReturn(0);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(3, initBonus);

        when(mockPlayer1.getCommandBonus()).thenReturn(1);
        when(mockPlayer2.getCommandBonus()).thenReturn(2);
        when(mockPlayer3.getCommandBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);

        when(mockPlayer1.getTurnInitBonus()).thenReturn(1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);

        when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);

        useInitCompBonus = true;
 
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);

        when(mockPlayer1.getInitCompensationBonus()).thenReturn(-1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(-2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(6, initBonus);

        when(mockPlayer1.getInitCompensationBonus()).thenReturn(1);
        when(mockPlayer2.getInitCompensationBonus()).thenReturn(2);
        when(mockPlayer3.getInitCompensationBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        assertEquals(9, initBonus);        
    }
}
