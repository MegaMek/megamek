/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.Game;
import megamek.common.options.GameOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/22/13 8:33 AM
 */
public class NewtonAerospacePathRankerTest {

    @SuppressWarnings("unused")
    private static Princess mockPrincess;
    @SuppressWarnings("unused")
    private static NewtonianAerospacePathRanker mockPathRanker;
    @SuppressWarnings("unused")
    private static Game mockGame;
    @SuppressWarnings("unused")
    private static GameOptions mockGameOptions;
 
    @BeforeAll
    public static void beforeAll() {
        /*mockPathRanker = mock(NewtonianAerospacePathRanker.class);

        MoralUtil mockMoralUtil = mock(MoralUtil.class);

        MMLogger fakeLogger = new FakeLogger();
        mockPrincess = mock(Princess.class);
        doNothing().when(mockPrincess).log(any(Class.class), anyString(),
                                                   any(LogLevel.class), anyString());
        when(mockPrincess.getPathRanker(PathRankerType.NewtonianAerospace)).thenReturn(mockPathRanker);
        when(mockPrincess.getPathRanker(any(Entity.class))).thenReturn(mockPathRanker);
        when(mockPrincess.getMoralUtil()).thenReturn(mockMoralUtil);
        when(mockPrincess.getMyFleeingEntities()).thenReturn(new HashSet<>(0));
        when(mockPrincess.getLogger()).thenReturn(fakeLogger);
        
        mockGame = mock(Game.class);
        mockGameOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGameOptions.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW)).thenReturn(true);*/
    }

    @Disabled
    @Test
    public void testCalculateSensorShadowMod() {
        /*final MovePath mockPath = mock(MovePath.class);
        when(mockPath.getGame()).thenReturn(mockGame);
        
        final Aero mockTestCraft = mock(Aero.class);
        final Aero mockTestShadowSource = mock(Aero.class);
        List<Aero> friendlyEntitiesList = new List<Aero>();
        when(mockGame.getFriendlyEntities(any(Coords.class), mockTestCraft)).thenReturn(friendlyEntitiesList.iterator());
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path does not end next to any of the allied entities 
        Coords nonAdjacentCoords = new Coords(10, 10);
        when(mockPath.getFinalCoords()).thenReturn(nonAdjacentCoords);
        assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path ends next to an aerospace fighter
        Coords coordsAdjacentToCraft = new Coords(10, 11);
        friendlyEntitiesList.add(mockTestShadowSource);
        when(mockTestShadowSource.isLargeCraft()).thenReturn(false);
        when(mockTestShadowSource.isDone()).thenReturn(true);
        when(mockPath.getFinalCoords()).thenReturn(coordsAdjacentToCraft);
        assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 1 when the mock path ends next to a large craft
        when(mockTestShadowSource.isLargeCraft()).thenReturn(true);
        when(mockTestCraft.getWeight()).thenReturn(150000);
        when(mockTestShadowSource.getWeight()).thenReturn(200000);
        assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path ends next to a large craft
        // but the large craft is way lighter than the test craft
        when(mockTestShadowSource.getWeight()).thenReturn(30000);
        assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);
        
        // this tests that the sensor shadow mod is returned as 0 when the mock path ends next to a moved large craft
        // since it will likely move away and not provide the sensor shadow
        when(mockTestShadowSource.getWeight()).thenReturn(200000);
        when(mockTestShadowSource.isDone()).thenReturn(false);
        assertEquals(mockPathRanker.calculateSensorShadowMod(mockPath), 0);*/
    }
}
