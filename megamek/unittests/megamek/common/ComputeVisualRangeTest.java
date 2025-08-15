/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Vector;

import megamek.common.options.GameOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import org.junit.jupiter.api.Test;

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
        when(mockBoard.isSpace()).thenReturn(false);

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
        when(mockBoard.isSpace()).thenReturn(false);

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
        when(mockBoard.isSpace()).thenReturn(false);

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
