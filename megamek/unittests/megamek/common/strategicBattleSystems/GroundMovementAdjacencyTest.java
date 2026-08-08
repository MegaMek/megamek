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

package megamek.common.strategicBattleSystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroundMovementAdjacencyTest {

    private SBFGame game;
    private SBFFormation formation;

    @BeforeEach
    void setUp() {
        game = new SBFGame();
        Board board = new Board(10, 10);
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setHex(x, y, new Hex());
            }
        }
        game.setBoard(0, board);
        game.addPlayer(1, new Player(1, "Owner"));

        formation = new SBFFormation();
        formation.setId(17);
        formation.setOwnerId(1);
        formation.setMovement(3);
        formation.setType(SBFElementType.BM);
        formation.setMovementMode(SBFMovementMode.MEK_WALK);
        formation.setDeployed(true);
        game.addUnit(formation);
    }

    @Test
    void centerHexProducesAllSixAdjacentPaths() {
        BoardLocation start = BoardLocation.of(new Coords(4, 4), 0);
        Collection<SBFMovePath> adjacentPaths = adjacentPathsFrom(start);

        assertEquals(6, adjacentPaths.size());
        assertTrue(adjacentPaths.stream().allMatch(path -> path.getSteps().size() == 1));
        assertTrue(adjacentPaths.stream().allMatch(path -> game.hasBoardLocation(path.getLastPosition())));
    }

    @Test
    void boardEdgeFiltersOffBoardLocations() {
        BoardLocation start = BoardLocation.of(new Coords(0, 0), 0);
        Collection<SBFMovePath> adjacentPaths = adjacentPathsFrom(start);

        assertFalse(adjacentPaths.isEmpty());
        assertTrue(adjacentPaths.size() < 6);
        assertTrue(adjacentPaths.stream().allMatch(path -> game.hasBoardLocation(path.getLastPosition())));
    }

    private Collection<SBFMovePath> adjacentPathsFrom(BoardLocation start) {
        formation.setPosition(start);
        SBFMovePath path = new SBFMovePath(formation.getId(), start, game);
        return new GroundMovementAdjacency(game).getAdjacent(path);
    }
}