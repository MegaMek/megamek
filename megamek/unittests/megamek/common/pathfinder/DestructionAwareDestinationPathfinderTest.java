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
package megamek.common.pathfinder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import megamek.common.BulldozerMovePath;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the walk-on deployment start of a long-range path. A unit with no enemy in sight moves along these paths, and
 * an undeployed unit's path without a {@code DEPLOY} step reaches the server as a skipped turn.
 */
class DestructionAwareDestinationPathfinderTest {

    private static final int BOARD_WIDTH = 20;
    private static final int BOARD_HEIGHT = 20;

    private Entity mover;
    private final Set<Coords> destination = Set.of(new Coords(10, 2));

    @BeforeEach
    void beforeEach() {
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        board.setBoardType(BoardType.GROUND);

        Game game = new Game();
        game.setBoard(board);
        Player owner = new Player(1, "Owner");
        game.addPlayer(1, owner);

        // An undeployed unit placed at its chosen entry hex, as Princess does before planning a walk-on move
        BipedMek mek = new BipedMek();
        mek.setId(1);
        mek.setGame(game);
        mek.setOwner(owner);
        mek.setOriginalWalkMP(4);
        mek.setPosition(new Coords(10, 18));
        mek.setFacing(0);
        mek.setDeployed(false);
        game.addEntity(mek);
        mover = mek;
    }

    @Test
    void deploymentPathStartsWithDeployStep() {
        BulldozerMovePath path = new DestructionAwareDestinationPathfinder().findPathToCoords(mover,
              destination, false, true, new BoardClusterTracker());

        assertNotNull(path);
        assertTrue(path.contains(MoveStepType.DEPLOY), "a walk-on path must carry the DEPLOY step");
        assertTrue(path.getFinalCoords().distance(new Coords(10, 2)) < 16, "the path should still head out");
    }

    @Test
    void ordinaryPathHasNoDeployStep() {
        BulldozerMovePath path = new DestructionAwareDestinationPathfinder().findPathToCoords(mover,
              destination, false, new BoardClusterTracker());

        assertNotNull(path);
        assertFalse(path.contains(MoveStepType.DEPLOY));
    }
}
