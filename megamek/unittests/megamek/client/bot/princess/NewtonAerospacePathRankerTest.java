/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import megamek.common.Game;
import megamek.common.options.GameOptions;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version $Id$
 * @since 11/22/13 8:33 AM
 */
@RunWith(JUnit4.class)
public class NewtonAerospacePathRankerTest {

    @SuppressWarnings("unused")
    private Princess mockPrincess;
    @SuppressWarnings("unused")
    private NewtonianAerospacePathRanker mockPathRanker;
    @SuppressWarnings("unused")
    private Game mockGame;
    @SuppressWarnings("unused")
    private GameOptions mockGameOptions;
 
    @Before
    public void setUp() {
        /*mockPathRanker = Mockito.mock(NewtonianAerospacePathRanker.class);

        MoralUtil mockMoralUtil = Mockito.mock(MoralUtil.class);

        MMLogger fakeLogger = new FakeLogger();
        mockPrincess = Mockito.mock(Princess.class);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.when(mockPrincess.getPathRanker(PathRankerType.NewtonianAerospace)).thenReturn(mockPathRanker);
        Mockito.when(mockPrincess.getPathRanker(Mockito.any(Entity.class))).thenReturn(mockPathRanker);
        Mockito.when(mockPrincess.getMoralUtil()).thenReturn(mockMoralUtil);
        Mockito.when(mockPrincess.getMyFleeingEntities()).thenReturn(new HashSet<>(0));
        Mockito.when(mockPrincess.getLogger()).thenReturn(fakeLogger);
        
        mockGame = Mockito.mock(Game.class);
        mockGameOptions = Mockito.mock(GameOptions.class);
        Mockito.when(mockGame.getOptions()).thenReturn(mockGameOptions);
        Mockito.when(mockGameOptions.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW)).thenReturn(true);*/
    }

    @Ignore
    @Test
    public void testCalculateSensorShadowMod() {
        /*final MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getGame()).thenReturn(mockGame);
        
        final Aero mockTestCraft = Mockito.mock(Aero.class);
        final Aero mockTestShadowSource = Mockito.mock(Aero.class);
        List<Aero> friendlyEntitiesList = new List<Aero>();
        Mockito.when(mockGame.getFriendlyEntities(Mockito.any(Coords.class), mockTestCraft)).thenReturn(friendlyEntitiesList.iterator());
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path does not end next to any of the allied entities 
        Coords nonAdjacentCoords = new Coords(10, 10);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(nonAdjacentCoords);
        Assert.assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path ends next to an aerospace fighter
        Coords coordsAdjacentToCraft = new Coords(10, 11);
        friendlyEntitiesList.add(mockTestShadowSource);
        Mockito.when(mockTestShadowSource.isLargeCraft()).thenReturn(false);
        Mockito.when(mockTestShadowSource.isDone()).thenReturn(true);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(coordsAdjacentToCraft);
        Assert.assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 1 when the mock path ends next to a large craft
        Mockito.when(mockTestShadowSource.isLargeCraft()).thenReturn(true);
        Mockito.when(mockTestCraft.getWeight()).thenReturn(150000);
        Mockito.when(mockTestShadowSource.getWeight()).thenReturn(200000);
        Assert.assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path ends next to a large craft
        // but the large craft is way lighter than the test craft
        Mockito.when(mockTestShadowSource.getWeight()).thenReturn(30000);
        Assert.assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path ends next to a moved large craft
        // since it will likely move away and not provide the sensor shadow
        Mockito.when(mockTestShadowSource.getWeight()).thenReturn(200000);
        Mockito.when(mockTestShadowSource.isDone()).thenReturn(false);
        Assert.assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);*/
    }
}
