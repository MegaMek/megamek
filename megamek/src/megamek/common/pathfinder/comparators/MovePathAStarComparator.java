/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.pathfinder.comparators;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.pathfinder.ShortestPathFinder;
import megamek.common.units.IAero;

/**
 * A MovePath comparator that compares movement points spent and distance to destination. If those are equal then
 * MovePaths with more HexesMoved are Preferred. This should considerably speed A* when multiple shortest paths are
 * present.
 * <p>
 * This comparator is used by A* algorithm.
 */
public class MovePathAStarComparator implements Comparator<MovePath>, Serializable {
    @Serial
    private static final long serialVersionUID = -2116704925028576850L;
    Coords destination;
    MoveStepType stepType;
    Board board;

    public MovePathAStarComparator(Coords destination, MoveStepType stepType, Board board) {
        this.destination = Objects.requireNonNull(destination);
        this.stepType = stepType;
        this.board = board;
    }

    @Override
    public int compare(MovePath first, MovePath second) {
        int h1 = 0, h2 = 0;
        // We cannot estimate the needed cost for aerospace
        // However, DropShips basically follow ground movement rules
        if ((first.getEntity().isAero())
              && !((IAero) first.getEntity()).isSpheroid()) {
            // We want to pick paths that use fewer MP, and are also shorter
            // unlike ground units which could benefit from better target
            // movement modifiers for longer paths
            int dd = (first.getMpUsed() + h1) - (second.getMpUsed() + h2);
            if (dd != 0) {
                return dd;
            } else {
                // Pick the shortest path
                int hexesMovedDiff = first.getHexesMoved() - second.getHexesMoved();
                if (hexesMovedDiff != 0) {
                    return hexesMovedDiff;
                }
                // If both are the same length, pick one with fewer steps
                return (first.length() - second.length());
            }
        } else if (first.getEntity().getWalkMP() != 0) {
            boolean backwards = stepType == MoveStepType.BACKWARDS;
            h1 = first.getFinalCoords().distance(destination)
                  + ShortestPathFinder.getFacingDiff(first, destination, backwards)
                  + ShortestPathFinder.getLevelDiff(first, destination, board, first.isJumping())
                  + ShortestPathFinder.getElevationDiff(first, destination, board, first.getEntity());
            h2 = second.getFinalCoords().distance(destination)
                  + ShortestPathFinder.getFacingDiff(second, destination, backwards)
                  + ShortestPathFinder.getLevelDiff(second, destination, board, second.isJumping())
                  + ShortestPathFinder.getElevationDiff(second, destination, board, second.getEntity());
        }

        int dd = (first.getMpUsed() + h1) - (second.getMpUsed() + h2);

        if (dd != 0) {
            return dd;
        } else {
            return first.getHexesMoved() - second.getHexesMoved();
        }
    }
}
