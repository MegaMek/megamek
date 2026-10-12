/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.bot.princess.geometry.CoordFacingCombo;
import megamek.common.BulldozerMovePath;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PathEnumerator#getEntitiesWithLocation(Coords, boolean)}, in particular that a unit is found at
 * a location regardless of which of the six hex facings its potential location uses (issue #8439).
 */
class PathEnumeratorTest {

    @Test
    void getEntitiesWithLocationDetectsEveryFacing() {
        Princess mockPrincess = mock(Princess.class);
        Game mockGame = mock(Game.class);
        PathEnumerator pathEnumerator = new PathEnumerator(mockPrincess, mockGame);

        Coords location = new Coords(3, 4);
        // Hex facings are 0-5. A unit whose only potential location at this hex uses a given facing must be found
        // for every facing; the old loop stopped at 4, so facing 5 (NW) was missed.
        for (int facing = 0; facing < 6; facing++) {
            int entityId = 10 + facing;
            Set<CoordFacingCombo> potentialLocations = new HashSet<>();
            potentialLocations.add(CoordFacingCombo.createCoordFacingCombo(location, facing));
            pathEnumerator.getUnitPotentialLocations().put(entityId, potentialLocations);

            assertTrue(pathEnumerator.getEntitiesWithLocation(location, false).contains(entityId),
                  "A unit whose only potential location faces " + facing + " must be found");
        }
    }

    @Test
    void getEntitiesWithLocationExcludesUnitAtAnotherHex() {
        Princess mockPrincess = mock(Princess.class);
        Game mockGame = mock(Game.class);
        PathEnumerator pathEnumerator = new PathEnumerator(mockPrincess, mockGame);

        Coords queriedLocation = new Coords(3, 4);
        Coords otherLocation = new Coords(9, 9);
        int entityId = 7;
        Set<CoordFacingCombo> potentialLocations = new HashSet<>();
        potentialLocations.add(CoordFacingCombo.createCoordFacingCombo(otherLocation, 2));
        pathEnumerator.getUnitPotentialLocations().put(entityId, potentialLocations);

        assertFalse(pathEnumerator.getEntitiesWithLocation(queriedLocation, false).contains(entityId),
              "A unit with no potential location at the queried hex must not be found");
    }

    @Test
    void longRangePathsAreBuiltOnFirstUseNotOnRecalculation() {
        Hex[] hexes = new Hex[20 * 20];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(20, 20, hexes);
        board.setBoardType(BoardType.GROUND);
        Game game = new Game();
        game.setBoard(board);
        Player owner = new Player(1, "Owner");
        game.addPlayer(1, owner);

        BipedMek mek = spy(new BipedMek());
        mek.setId(1);
        mek.setGame(game);
        mek.setOwner(owner);
        // Weight and internal structure, so the legs count as intact and the walk MP is real
        mek.setWeight(50);
        mek.autoSetInternal();
        mek.setOriginalWalkMP(4);
        mek.setPosition(new Coords(10, 18));
        mek.setFacing(0);
        mek.setDeployed(true);
        doReturn(true).when(mek).isSelectableThisTurn();
        game.addEntity(mek);

        // A unit with no enemy in sight moves to contact, across the board
        Princess princess = mock(Princess.class);
        UnitBehavior behaviorTracker = mock(UnitBehavior.class);
        when(princess.getLocalPlayer()).thenReturn(owner);
        when(princess.getGame()).thenReturn(game);
        when(princess.getUnitBehaviorTracker()).thenReturn(behaviorTracker);
        when(princess.getClusterTracker()).thenReturn(new BoardClusterTracker());
        when(princess.getEnemyHotSpots()).thenReturn(List.of());
        when(behaviorTracker.getBehaviorType(mek, princess)).thenReturn(BehaviorType.MoveToContact);
        when(behaviorTracker.getWaypointForEntity(mek)).thenReturn(Optional.empty());

        PathEnumerator pathEnumerator = new PathEnumerator(princess, game);
        pathEnumerator.recalculateMovesFor(mek);

        assertNotNull(pathEnumerator.getUnitPaths().get(mek.getId()), "the standard paths are still built at once");
        assertNull(pathEnumerator.getLongRangePaths().get(mek.getId()),
              "recalculating must not build the long-range paths, which most units never read");

        List<BulldozerMovePath> longRangePaths = pathEnumerator.getLongRangePathsFor(mek);
        assertNotNull(longRangePaths);
        assertFalse(longRangePaths.isEmpty(), "the first request builds the long-range paths");
        assertSame(longRangePaths, pathEnumerator.getLongRangePathsFor(mek), "a second request must not rebuild them");
    }
}
