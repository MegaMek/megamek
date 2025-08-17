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
package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.game.Game;
import megamek.common.units.IAero;
import megamek.common.moves.MovePath;
import megamek.common.moves.MovePath.MoveStepType;
import megamek.common.moves.MoveStep;
import megamek.common.pathfinder.MovePathFinder.CoordsWithFacing;

/**
 * This class is intended to be used by the bot for generating possible paths for aerospace units on a low-altitude
 * atmospheric map.
 *
 * @author NickAragua
 */
public class AeroLowAltitudePathFinder extends AeroGroundPathFinder {
    protected AeroLowAltitudePathFinder(Game game) {
        super(game);
    }

    public static AeroLowAltitudePathFinder getInstance(Game game) {
        return new AeroLowAltitudePathFinder(game);
    }

    @Override
    protected int getMinimumVelocity(IAero mover) {
        return 1;
    }

    @Override
    protected int getMaximumVelocity(IAero mover) {
        return mover.getCurrentThrust() * 2;
    }

    /**
     * Generate all possible paths given a starting movement path. This includes increases and decreases in elevation.
     */
    @Override
    protected List<MovePath> GenerateAllPaths(MovePath mp) {
        List<MovePath> altitudePaths = AeroPathUtil.generateValidAltitudeChanges(mp);
        List<MovePath> fullMovePaths = new ArrayList<>();

        for (MovePath altitudePath : altitudePaths) {
            fullMovePaths.addAll(super.GenerateAllPaths(altitudePath.clone()));
        }

        List<MovePath> fullMovePathsWithTurns = new ArrayList<>();

        for (MovePath movePath : fullMovePaths) {
            fullMovePathsWithTurns.add(movePath);

            MoveStep lastStep = movePath.getLastStep();

            if ((lastStep != null) && lastStep.canAeroTurn(game)) {
                MovePath left = movePath.clone();
                left.addStep(MoveStepType.TURN_LEFT);
                fullMovePathsWithTurns.add(left);

                MovePath right = movePath.clone();
                right.addStep(MoveStepType.TURN_RIGHT);
                fullMovePathsWithTurns.add(right);
            }
        }

        return fullMovePathsWithTurns;
    }

    /**
     * Get a list of movement paths with end-of-path altitude adjustments. Irrelevant for low-atmo maps, so simply
     * returns the passed-in list.
     */
    @Override
    protected List<MovePath> getAltitudeAdjustedPaths(List<MovePath> startingPaths) {
        return startingPaths;
    }

    // this data structure maps a set of coordinates with facing
    // to a map between height and "used MP".
    private Map<CoordsWithFacing, Map<Integer, Integer>> visitedCoords = new HashMap<>();

    /**
     * Determines whether or not the given move path is "redundant". In this situation, "redundant" means "there is
     * already a shorter path that goes to the ending coordinates/facing/height" combo.
     */
    @Override
    protected boolean pathIsRedundant(MovePath mp) {
        if (!mp.fliesOffBoard()) {
            CoordsWithFacing destinationCoords = new CoordsWithFacing(mp);
            if (!visitedCoords.containsKey(destinationCoords)) {
                visitedCoords.put(destinationCoords, new HashMap<>());
            }

            // we may or may not have been to these coordinates before, but we haven't been to this height. Not redundant. 
            if (!visitedCoords.get(destinationCoords).containsKey(mp.getFinalAltitude())) {
                visitedCoords.get(destinationCoords).put(mp.getFinalAltitude(), mp.getMpUsed());
                return false;
                // we *have* been to these coordinates and height before. This is redundant if the previous visit used less MP.
            } else {
                return visitedCoords.get(destinationCoords).get(mp.getFinalAltitude()) < mp.getMpUsed();
            }
        } else {
            return false;
        }
    }
}
