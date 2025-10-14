/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.BulldozerMovePath;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.pathfinder.LongestPathFinder.MovePathMinefieldAvoidanceMinMPMaxDistanceComparator;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Terrains;

/**
 * This class contains functionality that takes a given path and generates a list of child paths that go up to
 * walk/run/run+masc/sprint/sprint+masc MP usage.
 *
 * @author NickAragua
 */
public class PathDecorator {

    public static Set<MovePath> decoratePath(BulldozerMovePath source) {
        Set<MovePath> result = new HashSet<>();

        // paths that aren't on the ground require separate and special logic
        if (source.isJumping()) {
            result.addAll(decorateJumpPath(source));
        } else if (source.getEntity().isAirborne()) {
            result.add(source);
        } else if (source.getGame().useVectorMove()) {
            result.add(source);
        } else {
            result.addAll(decorateGroundPath(source));
        }

        return result;
    }

    /**
     * Takes a given (jumping) path and returns a list of child paths that lead up to max jump MP or max jump MP without
     * gravity.
     */
    public static Set<MovePath> decorateJumpPath(BulldozerMovePath source) {
        Set<MovePath> retVal = new HashSet<>();

        MovePath clippedSource = source.clone();
        clippedSource.clipToPossible();

        // jumping to move paths are pretty easy to clip
        // there are two interesting MP amounts - current jump MP and jump MP without "bonus" for low gravity.
        Set<Integer> desiredMPs = new HashSet<>();
        desiredMPs.add(source.getCachedEntityState().getJumpMP());
        desiredMPs.add(source.getCachedEntityState().getJumpMPNoGravity());

        for (int desiredMP : desiredMPs) {
            List<MovePath> clippedPaths = clipToDesiredMP(clippedSource, desiredMP);
            retVal.addAll(clippedPaths);
        }

        // if there is a bad guy in the last step, clip to one step short and see if we can't get around.
        if ((clippedSource.getLastStep() != null) &&
              clippedSource.getGame()
                    .getFirstEnemyEntity(clippedSource.getLastStep().getPosition(), clippedSource.getEntity())
                    != null) {
            clippedSource.removeLastStep();

            for (int desiredMP : desiredMPs) {
                List<MovePath> clippedPaths = clipToDesiredMP(clippedSource, desiredMP);
                retVal.addAll(clippedPaths);
            }
        }

        return retVal;
    }

    /**
     * Takes the given path and returns a list of child paths that go up to walk/run/run+masc/sprint/sprint+masc MP
     * usage.
     */
    public static Set<MovePath> decorateGroundPath(BulldozerMovePath source) {
        Set<MovePath> retVal = new HashSet<>();

        // we want to generate the following paths and decorations:
        // a "walking" path
        // a "running" path
        // a "running masc" path
        // a "sprinting" path
        // a "sprint with masc" path
        // decorations are movement possibilities that "fill up" any remaining MP with turns and unrelated moves

        MovePath clippedSource = source.clone();
        clippedSource.clipToPossible();

        Set<Integer> desiredMPs = new HashSet<>();
        desiredMPs.add(source.getCachedEntityState().getSprintMP());
        desiredMPs.add(source.getCachedEntityState().getSprintMPWithoutMASC());
        desiredMPs.add(source.getCachedEntityState().getRunMP());
        desiredMPs.add(source.getCachedEntityState().getRunMPWithoutMASC());
        desiredMPs.add(source.getCachedEntityState().getRunMPNoGravity());
        desiredMPs.add(source.getCachedEntityState().getWalkMP());

        for (int desiredMP : desiredMPs) {
            List<MovePath> clippedPaths = clipToDesiredMP(clippedSource, desiredMP);
            retVal.addAll(clippedPaths);
        }

        // if there is a bad guy in the last step, clip to one step short and see if we can't get around.
        if ((clippedSource.getLastStep() != null) &&
              clippedSource.getGame()
                    .getFirstEnemyEntity(clippedSource.getLastStep().getPosition(), clippedSource.getEntity())
                    != null) {
            clippedSource.removeLastStep();

            for (int desiredMP : desiredMPs) {
                List<MovePath> clippedPaths = clipToDesiredMP(clippedSource, desiredMP);
                retVal.addAll(clippedPaths);
            }
        }

        return retVal;
    }

    /**
     * Clips the given path until it only uses the desired MP or less.
     */
    public static List<MovePath> clipToDesiredMP(MovePath source, int desiredMP) {
        MovePath newPath = source.clone();
        while (newPath.getMpUsed() > desiredMP) {
            newPath.removeLastStep();
        }
        return generatePossiblePaths(newPath, desiredMP);
    }

    /**
     * Uses the LongestPathFinder to generate all paths possible from a starting path, up to the desired MP
     */
    public static List<MovePath> generatePossiblePaths(MovePath source, int desiredMP) {
        LongestPathFinder lpf = LongestPathFinder
              .newInstanceOfLongestPath(desiredMP,
                    MoveStepType.FORWARDS, source.getGame());
        lpf.setComparator(new MovePathMinefieldAvoidanceMinMPMaxDistanceComparator());
        lpf.run(source);
        return new ArrayList<>(lpf.getLongestComputedPaths());
    }

    /**
     * For units using VTOL movement, add "UP" steps to the end of the MovePath source so that a forward movement can
     * pass over intervening terrain
     */
    public static void AdjustElevationForForwardMovement(MovePath source) {
        // Do this only for VTOLs
        if (source.getEntity().getMovementMode() != EntityMovementMode.VTOL) {
            return;
        }

        // get the hex that is in the direction we're facing
        Coords destinationCoords = source.getFinalCoords().translated(source.getFinalFacing());
        Board board = source.getGame().getBoard(source.getFinalBoardId());
        Hex destHex = board.getHex(destinationCoords);
        if (destHex == null) {
            return;
        }

        // If the unit cannot go up in its current hex, nothing can be done
        int entityElevation = source.getFinalElevation();
        boolean canGoUp = source.getEntity()
              .canGoUp(entityElevation, source.getFinalCoords(), source.getFinalBoardId());
        if (!canGoUp) {
            return;
        }

        Hex srcHex = board.getHex(source.getFinalCoords());
        int absHeight = srcHex.getLevel() + entityElevation;
        int destElevation = absHeight - destHex.getLevel();
        int safeElevation = destHex.maxTerrainFeatureElevation(false);

        // Add as many UP steps as MP will allow, until able to move forward 
        while (destElevation <= safeElevation) {
            // Do not go up if the unit can go forward before rising above the 
            // maximum terrain elevation, e.g. under a bridge
            // VTOLs shouldn't land in this way, however.
            boolean noLanding = (destElevation >= 1);
            if (destHex.containsTerrain(Terrains.BLDG_ELEV)) {
                noLanding &= destElevation > (destHex.terrainLevel(Terrains.BLDG_ELEV) - destHex.depth());
            }
            if (destHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
                noLanding &= destElevation != destHex.terrainLevel(Terrains.BRIDGE_ELEV);
            }
            if (source.getEntity().isElevationValid(destElevation, destHex) && noLanding) {
                return;
            }

            source.addStep(MoveStepType.UP);

            if (!source.isMoveLegal()) {
                source.removeLastStep();
                return;
            }

            destElevation++;
        }
    }

    private PathDecorator() {}
}
