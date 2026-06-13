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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Martin "sleet01" Metke
 * @since 2024/02/12 2138 PST
 */
class ComputeArtilleryTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testSimpleLeadCalculations() {
        // Test lead from various locations against various speeds.
        Coords shooterPos = new Coords(15, 0);
        Coords targetPos = new Coords(15, 33);

        // Immobile target
        Coords leadPos = Compute.calculateArtilleryLead(targetPos, 0, 0);
        assertEquals(leadPos, targetPos);

        // MP 1 target
        leadPos = Compute.calculateArtilleryLead(targetPos, 0, 1);
        assertEquals(leadPos.getX(), targetPos.getX());
        assertEquals(leadPos.getY(), targetPos.getY() - 1);

        // MP 4 target with flight time == 1
        leadPos = Compute.calculateArtilleryLead(targetPos, 0, 8);
        assertEquals(leadPos.getX(), targetPos.getX());
        assertEquals(leadPos.getY(), targetPos.getY() - 8);

        // MP 8 target moving away
        leadPos = Compute.calculateArtilleryLead(targetPos, 3, 16);
        assertEquals(leadPos.getX(), targetPos.getX());
        assertEquals(leadPos.getY(), targetPos.getY() + 16);

        // MP 5 target moving NW; x <- 10, y ^ (10/2 + 1)
        leadPos = Compute.calculateArtilleryLead(targetPos, 5, 10);
        assertEquals(leadPos.getX(), targetPos.getX() - 10);
        assertEquals(leadPos.getY(), targetPos.getY() - 5);

        // Reversed fire, MP 2, flight time 1
        leadPos = Compute.calculateArtilleryLead(shooterPos, 3, 4);
        assertEquals(leadPos.getX(), shooterPos.getX());
        assertEquals(leadPos.getY(), shooterPos.getY() + 4);
    }

    private void setupTarget(Entity target, Coords targetPos, Coords oldTargetPos) {
        when(target.getPosition()).thenReturn(targetPos);
        when(target.getPriorPosition()).thenReturn(oldTargetPos);

    }

    @Test
    void testComplexCalculateLead() {
        // Mock the board
        Board mockBoard = mock(Board.class);
        Game mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.onConnectedBoards(any(Targetable.class), any(Targetable.class))).thenReturn(true);
        when(mockGame.getBoard(any(Targetable.class))).thenReturn(mockBoard);

        Entity shooter = mock(Entity.class);
        Entity target = mock(Entity.class);
        Coords shooterPos = new Coords(15, 0);
        when(shooter.getPosition()).thenReturn(shooterPos);

        // Immobile target <1 map sheet away to the S (3) direction
        setupTarget(target, new Coords(15, 17), new Coords(15, 17));
        Coords leadPos = Compute.calculateArtilleryLead(mockGame, shooter, target, false);
        assertEquals(15, leadPos.getX());
        assertEquals(17, leadPos.getY());

        // Mobile target <1 map sheet away to the S (3) direction, speed 4, non-homing
        setupTarget(target, new Coords(15, 17), new Coords(15, 21));
        leadPos = Compute.calculateArtilleryLead(mockGame, shooter, target, false);
        assertEquals(15, leadPos.getX());
        assertEquals(9, leadPos.getY());

        // Mobile target 1 map sheet away to the S (3) direction, speed 4, non-homing
        setupTarget(target, new Coords(15, 25), new Coords(15, 29));
        leadPos = Compute.calculateArtilleryLead(mockGame, shooter, target, false);
        assertEquals(15, leadPos.getX());
        assertEquals(13, leadPos.getY());

        // Mobile target 1 map sheet away to the S (3) direction, speed 4, homing,
        // should be closer to shooter
        // for better chance to catch mobile unit in TAG-able area
        setupTarget(target, new Coords(15, 25), new Coords(15, 29));
        leadPos = Compute.calculateArtilleryLead(mockGame, shooter, target, true);
        assertEquals(15, leadPos.getX());
        assertEquals(9, leadPos.getY());

        // Mobile target 1 map sheet away to the NE (1) direction, speed 4, non-homing
        shooterPos = new Coords(0, 35);
        when(shooter.getPosition()).thenReturn(shooterPos);
        setupTarget(target, new Coords(32, 1), new Coords(36, 0));
        leadPos = Compute.calculateArtilleryLead(mockGame, shooter, target, false);
        assertEquals(20, leadPos.getX());
        assertEquals(7, leadPos.getY());
    }
}
