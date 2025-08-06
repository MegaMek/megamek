/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.Game;
import megamek.common.IAero;
import megamek.common.moves.MovePath;
import megamek.common.moves.MovePath.MoveStepType;
import megamek.common.pathfinder.MovePathFinder.CoordsWithFacing;

/**
 * This class generates move paths suitable for use by an aerospace unit operating on a space map, with 'advanced
 * flight' turned off.
 *
 * @author NickAragua
 */
public class AeroSpacePathFinder extends NewtonianAerospacePathFinder {
    protected AeroSpacePathFinder(Game game) {
        super(game);
    }

    /**
     * Worker method to put together a pre-defined array of possible moves
     */
    @Override
    protected void initializeMoveList() {
        moves = new ArrayList<>();
        moves.add(MoveStepType.TURN_RIGHT);
        moves.add(MoveStepType.TURN_LEFT);
        moves.add(MoveStepType.FORWARDS);
    }

    public static AeroSpacePathFinder getInstance(Game game) {
        return new AeroSpacePathFinder(game);
    }

    /**
     * Generates a list of possible step combinations that should be done at the beginning of a path This implementation
     * generates exactly one path, which is either no moves or one hex forward when velocity &gt; 0
     *
     * @return "List" of all possible "starting" paths
     */
    @Override
    protected List<MovePath> generateStartingPaths(MovePath startingEdge) {
        List<MovePath> startingPaths = new ArrayList<>();

        // calculate max and min safe velocity
        // in space, we can go as slow or as fast as we want.
        IAero aero = (IAero) startingEdge.getEntity();
        int maxThrust = AeroPathUtil.calculateMaxSafeThrust(aero);
        int maxVelocity = aero.getCurrentVelocity() + maxThrust;
        int minVelocity = Math.max(0, aero.getCurrentVelocity() - maxThrust);
        startingPaths.addAll(AeroPathUtil.generateValidAccelerations(startingEdge, minVelocity, maxVelocity));

        // all non-zero-velocity paths must move at least one hex forward
        for (MovePath path : startingPaths) {
            if (path.getFinalVelocity() > 0) {
                path.addStep(MoveStepType.FORWARDS);
            }
        }

        return startingPaths;
    }

    /**
     * "Worker" function to determine whether the path being examined is an intermediate path. This means that the path,
     * as is, is not a valid path, but its children may be. This mainly applies to aero paths that have not used all
     * their velocity.
     *
     * @param path The move path to consider.
     *
     * @return Whether it is an intermediate path or not.
     */
    @Override
    protected boolean isIntermediatePath(MovePath path) {
        return path.getFinalVelocityLeft() > 0;
    }

    /**
     * Worker function to determine whether we should discard the current path (due to it being illegal or redundant) or
     * keep generating child nodes
     *
     * @param path The move path to consider
     *
     * @return Whether to keep or dicsard.
     */
    @Override
    protected boolean discardPath(MovePath path, CoordsWithFacing pathDestination) {
        boolean maxMPExceeded = path.getMpUsed() > path.getEntity().getRunMP();

        // having generated the child, we add it and (recursively) any of its children
        // to the list of children to be returned
        // unless it moves too far or exceeds max thrust
        if (path.getFinalVelocityLeft() < 0 || maxMPExceeded) {
            return true;
        }

        // terminator conditions:
        // we've visited this hex already and the path we are considering is longer than
        // the previous path that visited this hex
        if (visitedCoords.containsKey(pathDestination)
              && (visitedCoords.get(pathDestination) < path.getMpUsed())) {
            return true;
        }

        // there's no reason to consider off-board paths in the standard flight model.
        if (!path.getGame().getBoard().contains(pathDestination.getCoords())) {
            return true;
        }

        return false;
    }
}
