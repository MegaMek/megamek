/*
 * MegaMek - Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
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
@RunWith(JUnit4.class)
public class TeamTest {

    @Test
    public void testTotalInitBonus() {
        Team testTeam = new Team(1);
        Player mockPlayer1, mockPlayer2, mockPlayer3;
        // Setup Player 1
        mockPlayer1 = Mockito.mock(Player.class);
        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer1.getTurnInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer1.getInitCompensationBonus()).thenReturn(0);
        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(0);
        // Setup Player 2
        mockPlayer2 = Mockito.mock(Player.class);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(0);
        // Setup Player 3
        mockPlayer3 = Mockito.mock(Player.class);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(0);

        testTeam.addPlayer(mockPlayer1);
        testTeam.addPlayer(mockPlayer2);
        testTeam.addPlayer(mockPlayer3);

        int initBonus;
        boolean useInitCompBonus = false;

        // Sanity test
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(initBonus, 0);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(-2);
        Mockito.when(mockPlayer3.getConstantInitBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(-1, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(0);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(0, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(1, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(2);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(1, initBonus);

        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(1);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(1, initBonus);

        Mockito.when(mockPlayer3.getCommandBonus()).thenReturn(4);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(3, initBonus);

        Mockito.when(mockPlayer1.getConstantInitBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getConstantInitBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getConstantInitBonus()).thenReturn(3);
        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(0);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(0);
        Mockito.when(mockPlayer3.getCommandBonus()).thenReturn(0);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(3, initBonus);

        Mockito.when(mockPlayer1.getCommandBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getCommandBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getCommandBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(6, initBonus);

        Mockito.when(mockPlayer1.getTurnInitBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getTurnInitBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(12, initBonus);

        Mockito.when(mockPlayer1.getTurnInitBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getTurnInitBonus()).thenReturn(-2);
        Mockito.when(mockPlayer3.getTurnInitBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(0, initBonus);

        useInitCompBonus = true;

        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(0, initBonus);

        Mockito.when(mockPlayer1.getInitCompensationBonus()).thenReturn(-1);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(-2);
        Mockito.when(mockPlayer3.getInitCompensationBonus()).thenReturn(-3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(0, initBonus);

        Mockito.when(mockPlayer1.getInitCompensationBonus()).thenReturn(1);
        Mockito.when(mockPlayer2.getInitCompensationBonus()).thenReturn(2);
        Mockito.when(mockPlayer3.getInitCompensationBonus()).thenReturn(3);
        initBonus = testTeam.getTotalInitBonus(useInitCompBonus);
        Assert.assertEquals(3, initBonus);        
    }


}
