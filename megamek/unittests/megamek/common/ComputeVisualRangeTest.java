/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common;

import megamek.common.options.GameOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComputeVisualRangeTest {

    /**
     * Testing under normal planetary conditions.
     */
    @Test
    void testVisualRange() {
        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = new PlanetaryConditions();


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getFlares()).thenReturn(new Vector<>());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);


        Tank mockAttackingEntity = mock(Tank.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));

        Tank mockTarget = mock(Tank.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 7));
        when(mockTarget.isIlluminated()).thenReturn(true);

        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target too far away
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 61));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target just close enough
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 60));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));
    }


    /**
     * Testing under normal planetary conditions, air to ground
     */
    @Test
    void testVisualRangeAirToGround() {
        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = new PlanetaryConditions();


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getFlares()).thenReturn(new Vector<>());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);


        Entity mockAttackingEntity = mock(Aero.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getAltitude()).thenReturn(5);
        when(mockAttackingEntity.isAirborne()).thenReturn(true);
        when(mockAttackingEntity.isAero()).thenReturn(true);

        Entity mockTarget = mock(Tank.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 7));
        when(mockTarget.isIlluminated()).thenReturn(true);
        when(mockTarget.isAirborne()).thenReturn(false);

        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target too far away
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 120));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target just close enough
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 110));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Let's change our altitude - if we go altitude 10 we can't see any ground units
        when(mockAttackingEntity.getAltitude()).thenReturn(10);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 10));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Let's get low - if we go to NOE we should use "ground" distance
        when(mockAttackingEntity.getAltitude()).thenReturn(1);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 90));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // But if we bring the enemy a little closer, we'll be able to see them
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 58));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));
    }


    /**
     * Testing under normal planetary conditions, ground to air
     */
    @Test
    void testVisualRangeGroundToAir() {
        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = new PlanetaryConditions();


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getFlares()).thenReturn(new Vector<>());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);


        Tank mockAttackingEntity = mock(Tank.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.isAirborne()).thenReturn(false);


        Aero mockTarget = mock(Aero.class);
        when(mockTarget.getAltitude()).thenReturn(5);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 7));
        when(mockTarget.isIlluminated()).thenReturn(true);
        when(mockTarget.isAirborne()).thenReturn(true);
        when(mockTarget.isAero()).thenReturn(true);
        when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 7))));

        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target too far away
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 60));
        when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 60))));
        assertFalse(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

        // Move our target just close enough
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 50));
        when(mockTarget.getPassedThrough()).thenReturn(new Vector<>(Collections.singleton(new Coords(0, 50))));
        assertTrue(Compute.inVisualRange(mockGame, mockLos, mockAttackingEntity, mockTarget));

    }
}
