/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.IAero;
import megamek.logging.MMLogger;

/**
 * This set of classes is intended to be used by AI players to generate paths for units behaving like spheroid DropShips
 * in atmosphere. Remarkably similar to a jumping infantry unit.
 *
 * @author NickAragua
 */
public class SpheroidPathFinder {
    private static final MMLogger logger = MMLogger.create(SpheroidPathFinder.class);

    private final Game game;
    private int direction;
    private List<MovePath> spheroidPaths;

    private final Set<Coords> visitedCoords = new HashSet<>();

    private SpheroidPathFinder(Game game) {
        // Default to heading north
        this(game, 0);
    }

    private SpheroidPathFinder(Game game, int direction) {
        this.game = game;
        this.direction = direction;
    }

    /**
     * We want to be able to set the direction the entity should turn to face
     *
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return this.direction;
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return spheroidPaths;
    }

    /**
     * Computes paths to nodes in the graph. This is an incredibly compute- and memory-intensive process, so we are
     * trimming it down: 1) A Princess spheroid on the ground map can face any direction if it hovers in place. 2) A
     * Princess spheroid on the ground map must choose one facing per path endpoint.
     * <p>
     * The facing will either be the current facing, or a direction determined by the unit's state.
     *
     * @param startingEdge the starting node. Should be empty.
     */
    public void run(MovePath startingEdge) {
        try {
            spheroidPaths = new ArrayList<>();

            // can't do anything if the unit is out of control.
            if (((IAero) startingEdge.getEntity()).isOutControlTotal()) {
                return;
            }

            // total number of paths should be ~217 * n on a ground map or 7 * n on a low
            // atmosphere map
            // where n is the number of possible altitude changes
            List<MovePath> altitudePaths = AeroPathUtil.generateValidAltitudeChanges(startingEdge);
            MovePath hoverPath = generateHoverPath(startingEdge);
            for (MovePath altitudePath : altitudePaths) {
                // since we are considering paths across multiple altitudes that cross the same
                // coordinates we want to clear this out before every altitude to avoid
                // discarding altitude changing paths so that our DropShips can maneuver
                // vertically if necessary. Each path will end with turns to face
                // this.direction.
                visitedCoords.clear();

                // we don't really want to consider a non-hovering path, we will add it as a
                // special case
                if (altitudePath.length() != 0) {
                    spheroidPaths.addAll(generateChildren(altitudePath));
                } else {
                    spheroidPaths.addAll(generateChildren(hoverPath));
                }
            }

            spheroidPaths.addAll(altitudePaths);
            spheroidPaths.add(hoverPath);

            // Allow the entity to rotate any direction if it hovers.
            List<MovePath> validRotations = new ArrayList<>(AeroPathUtil.generateValidRotations(hoverPath));

            spheroidPaths.addAll(validRotations);

            visitedCoords.clear();

            // add "flee" option if we haven't done anything else
            if (game.getBoard().isOnBoardEdge(startingEdge.getFinalCoords())
                  && startingEdge.getStepVector().isEmpty()) {
                MovePath fleePath = startingEdge.clone();
                fleePath.addStep(MoveStepType.FLEE);
                spheroidPaths.add(fleePath);
            }
        } catch (OutOfMemoryError ex) {
            // Some implementations can run out of memory if they consider and save in
            // memory too many paths. Usually we can recover from this by ending prematurely
            // while preserving already computed results.
            final String memoryMessage = "Not enough memory to analyze all options."
                  + " Try setting time limit to lower value, or "
                  + "increase java memory limit.";

            logger.error(memoryMessage, ex);
        } catch (Exception ex) {
            logger.error("", ex); // do something, don't just swallow the exception, good lord
        }
    }

    public static SpheroidPathFinder getInstance(Game game, int direction) {
        return new SpheroidPathFinder(game, direction);
    }

    private MovePath generateHoverPath(MovePath startingPath) {
        MovePath hoverPath = startingPath.clone();
        hoverPath.addStep(MoveStepType.HOVER);

        // If we can hover, then hover. If not (due to battle damage or whatever), then
        // we fall down.
        if (!hoverPath.isMoveLegal()) {
            hoverPath.removeLastStep();
        }

        return hoverPath;
    }

    /**
     * Recursive method that generates the possible child paths from the given path. Eliminates paths to hexes we've
     * already visited. Generates *shortest* paths to destination hexes
     *
     */
    private List<MovePath> generateChildren(MovePath startingPath) {
        List<MovePath> retVal = new ArrayList<>();

        // terminator conditions:
        // - we've visited this hex already
        // - we've moved further than 1 hex on a low-atmosphere map
        // - we've moved further than 8 hexes on a ground map
        if (visitedCoords.contains(startingPath.getFinalCoords()) ||
              (startingPath.getMpUsed() > startingPath.getEntity().getRunMP())) {
            return retVal;
        }

        visitedCoords.add(startingPath.getFinalCoords());

        // generate all possible children, add them to list
        // for units acting as in-atmosphere spheroid JumpShips, facing changes are
        // free, so children are always forward, left-forward, left-left-forward,
        // right-forward, right-right-forward, right-right-right-forward there is never
        // a reason to "back up" there are also very little built-in error control,
        // since these things are flying
        for (int direction = 0; direction <= 5; direction++) {
            MovePath childPath = startingPath.clone();

            // for each child, we first turn in the appropriate direction
            for (MoveStepType stepType : AeroPathUtil.getTurns().get(direction)) {
                childPath.addStep(stepType);
            }

            // If we're right at the end of our possible movement, face our desired heading
            if (childPath.getMpUsed() == startingPath.getEntity().getRunMP() - 1) {
                // End this path with turns to face our desired final direction
                int diff = (6 + (direction - startingPath.getFinalFacing())) % 6;
                for (MoveStepType stepType : AeroPathUtil.getTurns().get(diff)) {
                    startingPath.addStep(stepType);
                }
            } else {
                // Allow continued movement forward
                childPath.addStep(MoveStepType.FORWARDS);
            }

            // having generated the child, we add it and (recursively) any of its children
            // to the list of children to be returned of course, if it winds up not being
            // legal anyway for some other reason, then we discard it and move on
            if (!childPath.isMoveLegal()) {
                continue;
            }

            retVal.add(childPath.clone());
            retVal.addAll(generateChildren(childPath));
        }

        return retVal;
    }
}
