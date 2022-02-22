/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @author Nicholas Walczak (walczak@cs.umn.edu)
 * @since 06/10/14
 */
@RunWith(value = JUnit4.class)
public class TeamTest {
    @Test
    public void testTotalInitBonus() {
        Team testTeam = new Team(1);
        // Setup Player 1
        Player mockPlayer1 = Mockito.mock(Player.class);
        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer1.getTurnInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer1.getInitCompensationBonus()).thenReturn(0);
        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(0);
        // Setup Player 2
        Player mockPlayer2 = Mockito.mock(Player.class);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(0);
        // Setup Player 3
        Player mockPlayer3 = Mockito.mock(Player.class);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(0);

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        // Sanity test
        int initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(initBonus, 0);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        Mockito.when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(-1, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(0);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(0, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(1, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(2);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(1, initBonus);

        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(1);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(1, initBonus);

        Mockito.when(mockPlayer3.getCommandBonus()).thenReturn(4);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(3, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(0);
        Mockito.when(mockPlayer3.getCommandBonus()).thenReturn(0);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(3, initBonus);

        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getCommandBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(6, initBonus);

        Mockito.when(mockPlayer1.getTurnInitBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getTurnInitBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(12, initBonus);

        Mockito.when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        Mockito.when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(false);
        Assert.assertEquals(0, initBonus);

        initBonus = testTeam.getTotalInitBonus(true);
        Assert.assertEquals(0, initBonus);

        Mockito.when(mockPlayer1.getInitCompensationBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(-2);
        Mockito.when(mockPlayer3.getInitCompensationBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(true);
        Assert.assertEquals(0, initBonus);

        Mockito.when(mockPlayer1.getInitCompensationBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getInitCompensationBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(true);
        Assert.assertEquals(3, initBonus);        
    }
}
