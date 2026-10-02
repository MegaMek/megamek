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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import megamek.common.BulldozerMovePath;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.pathfinder.DestructionAwareDestinationPathfinder;
import megamek.common.pathfinder.PathDecorator;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SupportTank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Issue #9114: the bot path generators never added an UP step, so a bot WiGE on the ground (1 MP) could never take off
 * again. These tests run the real Princess path generation (shared by CASPAR) for a WiGE with 8/12 MP on open ground.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class GroundedWiGETakeoffPathTest extends GameBoardTestCase {

    static {
        initializeBoard("OPEN_GROUND", """
              size 1 14
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              hex 0106 0 "" ""
              hex 0107 0 "" ""
              hex 0108 0 "" ""
              hex 0109 0 "" ""
              hex 0110 0 "" ""
              hex 0111 0 "" ""
              hex 0112 0 "" ""
              hex 0113 0 "" ""
              hex 0114 0 "" ""
              end""");
    }

    @BeforeEach
    void setUp() {
        setBoard("OPEN_GROUND");
    }

    /** Places a WiGE vehicle in the top hex, facing down the board, at the given elevation. */
    private SupportTank placeWiGE(int elevation) {
        SupportTank wige = new SupportTank();
        getMovePathFor(wige, elevation, EntityMovementMode.WIGE);
        return wige;
    }

    /** A Princess that predicts paths for a unit it does not own, so no long-range planning is needed. */
    private PathEnumerator pathEnumerator() {
        Princess princess = mock(Princess.class);
        UnitBehavior behaviorTracker = mock(UnitBehavior.class);
        when(behaviorTracker.getWaypointForEntity(any())).thenReturn(Optional.empty());
        when(princess.getUnitBehaviorTracker()).thenReturn(behaviorTracker);
        when(princess.getLocalPlayer()).thenReturn(new Player(99, "Observer"));
        when(princess.getGame()).thenReturn(getGame());
        return new PathEnumerator(princess, getGame());
    }

    private static boolean takesOff(MovePath path) {
        for (MoveStep step : path.getStepVector()) {
            if (step.getType() == MoveStepType.UP) {
                return true;
            }
        }
        return false;
    }

    private static int longestTakeoffDistance(List<MovePath> paths) {
        int longest = -1;
        for (MovePath path : paths) {
            if (takesOff(path)) {
                longest = Math.max(longest, path.getHexesMoved());
            }
        }
        return longest;
    }

    @Test
    void groundedWiGEIsOfferedTakeoffPaths() {
        SupportTank wige = placeWiGE(0);
        PathEnumerator pathEnumerator = pathEnumerator();

        pathEnumerator.recalculateMovesFor(wige);
        List<MovePath> paths = pathEnumerator.getUnitPaths().get(wige.getId());

        assertNotNull(paths);
        // 12 MP: 5 to take off, then 7 hexes in the air
        assertEquals(7, longestTakeoffDistance(paths), "a grounded WiGE must be able to take off and fly");
    }

    @Test
    void groundedWiGEKeepsItsGroundMove() {
        SupportTank wige = placeWiGE(0);
        PathEnumerator pathEnumerator = pathEnumerator();

        pathEnumerator.recalculateMovesFor(wige);
        boolean hasGroundMove = false;
        for (MovePath path : pathEnumerator.getUnitPaths().get(wige.getId())) {
            if (!takesOff(path) && (path.getHexesMoved() == 1)) {
                hasGroundMove = true;
            }
        }

        assertTrue(hasGroundMove, "staying on the ground and moving 1 hex is still an option");
    }

    @Test
    void airborneWiGEIsNotGivenATakeoff() {
        SupportTank wige = placeWiGE(1);
        PathEnumerator pathEnumerator = pathEnumerator();

        pathEnumerator.recalculateMovesFor(wige);

        assertEquals(-1, longestTakeoffDistance(pathEnumerator.getUnitPaths().get(wige.getId())),
              "a WiGE already in the air has nothing to take off from");
    }

    @Test
    void longRangePathTakesOffFirst() {
        SupportTank wige = placeWiGE(0);
        DestructionAwareDestinationPathfinder pathfinder = new DestructionAwareDestinationPathfinder();

        BulldozerMovePath longRangePath = pathfinder.findPathToCoords(wige, Set.of(new Coords(0, 13)),
              mock(BoardClusterTracker.class));
        assertNotNull(longRangePath);
        BulldozerMovePath thisTurn = longRangePath.clone();
        thisTurn.clipToPossible();

        assertTrue(takesOff(thisTurn), "the route to a far destination starts with a takeoff");
        assertTrue(thisTurn.getHexesMoved() > 1, "after taking off the WiGE flies more than 1 hex this turn");
    }

    @Test
    void takeoffIsOnlyAddedForWiGEs() {
        SupportTank hover = new SupportTank();
        MovePath path = getMovePathFor(hover, 0, EntityMovementMode.HOVER);

        assertFalse(PathDecorator.addWiGETakeoff(path));
        assertEquals(0, path.length(), "the path is left unchanged");
    }
}
