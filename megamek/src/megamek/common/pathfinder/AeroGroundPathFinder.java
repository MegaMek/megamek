/*
* MegaMek -
* Copyright (C) 2017 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common.pathfinder;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import megamek.common.pathfinder.AbstractPathFinder.Filter;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * This set of classes is intended for use for pathfinding by aerodyne units on ground maps with an atmosphere
 * Usage anywhere else may result in "unpredictable" behavior
 *
 * @author NickAragua
 */
public class AeroGroundPathFinder {

    public static final int OPTIMAL_STRIKE_ALTITUDE = 5; // also great for dive bombs
    public static final int NAP_OF_THE_EARTH = 1;
    public static final int OPTIMAL_STRAFE_ALTITUDE = 3; // future use

    protected int getMinimumVelocity(IAero mover) {
        return 1;
    }

    protected int getMaximumVelocity(IAero mover) {
        return 3;
    }

    protected Game game;
    private List<MovePath> aeroGroundPaths;
    protected int maxThrust;

    protected AeroGroundPathFinder(Game game) {
        this.game = game;
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return aeroGroundPaths;
    }

    /**
     * Computes shortest paths to nodes in the graph.
     *
     * @param startingEdge a collection of possible starting edges.
     */
    public void run(MovePath startingEdge) {
        try {
            aeroGroundPaths = new ArrayList<>();
            visitedCoords.clear();

            // if we're out of control, then we can't actually do anything
            if (((IAero) startingEdge.getEntity()).isOutControlTotal()) {
                return;
            }

            // calculate maximum and minimum safe velocity
            // in the case of aerospace units on ground maps, we want a velocity of at least 1 so we don't stall
            // and at least 3 so we don't pointlessly fly around in circles or consume all available memory
            IAero aero = (IAero) startingEdge.getEntity();
            maxThrust = AeroPathUtil.calculateMaxSafeThrust(aero);
            int maxVelocity = Math.min(getMaximumVelocity(aero), aero.getCurrentVelocity() + maxThrust);
            int minVelocity = Math.max(getMinimumVelocity(aero), aero.getCurrentVelocity() - maxThrust);
            Collection<MovePath> validAccelerations = AeroPathUtil.generateValidAccelerations(startingEdge, minVelocity, maxVelocity);

            for (MovePath acceleratedPath : validAccelerations) {
                aeroGroundPaths.addAll(getAltitudeAdjustedPaths(GenerateAllPaths(acceleratedPath)));
            }
        } catch (OutOfMemoryError e) {
            /*
             * Some implementations can run out of memory if they consider and
             * save in memory too many paths. Usually we can recover from this
             * by ending prematurely while preserving already computed results.
             */

            final String memoryMessage = "Not enough memory to analyse all options."
                    + " Try setting time limit to lower value, or "
                    + "increase java memory limit.";

            LogManager.getLogger().error(memoryMessage, e);
        } catch (Exception e) {
            LogManager.getLogger().error("", e); //do something, don't just swallow the exception, good lord
        }
    }

    public static AeroGroundPathFinder getInstance(Game game) {
        return new AeroGroundPathFinder(game);
    }

    /**
     *
     * @author NickAragua
     *
     * @param <MovePath> a move path
     *
     * This class removes all off-board paths, but keeps track of the shortest of all the paths removed
     */
    public static class AeroGroundOffBoardFilter extends Filter<MovePath> {

        MovePath shortestPath;

        public MovePath getShortestPath() {
            return shortestPath;
        }

        /**
         * Returns filtered collection by removing those objects that fail
         * {@link #shouldStay(T)} test.
         *
         * @param collection collection to be filtered
         * @return filtered collection
         */
        @Override
        public Collection<MovePath> doFilter(Collection<MovePath> collection) {
            List<MovePath> filteredMoves = new ArrayList<>();
            for (MovePath e : collection) {
                // if the path does not go off board, then we add it to the returned collection
                if (shouldStay(e)) {
                    filteredMoves.add(e);
                } else {
                    // if the path *does* go off board, we want to compare its length to
                    // our current shortest off-board path and keep it in mind if it's both shorter and safe
                    // at the end of the process, we will add the shortest path off board to the list of moves
                    if (shortestPath == null ||
                            (shortestPath.length() > e.length()) &&
                            AeroPathUtil.isSafePathOffBoard(e)) {
                        shortestPath = e;
                    }
                }
            }

            return filteredMoves;
        }

        /**
         * Tests if the object should stay in the collection.
         *
         * @param path Path object is taking
         * @return true if the object should stay in the collection
         */
        @Override
        public boolean shouldStay(MovePath path) {
            return !path.fliesOffBoard();
        }

    }

    // This object contains a hex and the shortest non-off-board path that has flown over it.
    Map<Coords, MovePath> visitedHexes;

    /**
     * Given a list of MovePaths, applies adjustTowardsDesiredAltitude to each of them.
     * @param startingPaths The collection of paths to process
     * @return The new collection of resulting paths.
     */
    protected List<MovePath> getAltitudeAdjustedPaths(List<MovePath> startingPaths) {
        List<MovePath> desiredAltitudes = new ArrayList<>();

        for (MovePath start : startingPaths) {
            boolean choppedOffFlyOff = false;

            // if we are going off board, we need to take some special actions, meaning chopping off the tail end
            if (start.fliesOffBoard()) {
                start.removeLastStep();
                choppedOffFlyOff = true;
            }

            // repeat with 1, 3, 7 when we settle things down?
            MovePath desiredAltitudePath = adjustTowardsDesiredAltitude(start, OPTIMAL_STRIKE_ALTITUDE);

            if (choppedOffFlyOff) {
                desiredAltitudePath.addStep(MoveStepType.RETURN);
            }

            desiredAltitudes.add(desiredAltitudePath);
        }

        return desiredAltitudes;
    }

    /**
     * Moves a given path towards the desired altitude.
     * @param startingPath The path to adjust
     * @param desiredAltitude The desired altitude
     * @return New instance of movepath with as much altitude adjustment as possible
     */
    private MovePath adjustTowardsDesiredAltitude(MovePath startingPath, int desiredAltitude) {
        MovePath altitudePath = startingPath.clone();

        // generate a path that involves making changes that go up or down towards the desired altitude as far as possible
        while (altitudePath.getFinalAltitude() != desiredAltitude) {
            // up steps use thrust. Two points, to be exact
            if (altitudePath.getFinalAltitude() < desiredAltitude &&
                    altitudePath.getMpUsed() < maxThrust - 1) {
                altitudePath.addStep(MoveStepType.UP);
            } else if ((altitudePath.getFinalAltitude() > desiredAltitude) &&
                    (altitudePath.getFinalAltitude() >= startingPath.getFinalAltitude() - 1)) {
                // down steps don't use thrust, but if we take more than one, it changes the velocity,
                // so just do one for now
                // if we take more than two, it's a PSR, so definitely avoid that
                altitudePath.addStep(MoveStepType.DOWN);
            } else {
                // we've either reached the desired altitude or have hit some other stopping condition so that's enough of that
                break;
            }
        }

        return altitudePath;
    }

    // the goal here is to generate a "tree-like" structure;
    //
    //               \\\|///
    //                \\|//
    //                 \|/
    //                  |
    //                  |
    //
    //  Then, generate smaller subtrees off the left and right sides of the tree, like so:
    //             /-\\\|///-\
    //            //--\\|//--\\
    //                 \|/
    //                  |
    //                  |
    // And so on until we've run out of velocity
    // if a branch runs off board, we discard it unless it's the shortest one so far, we'll want to keep one of those around
    // if a branch runs out of velocity, then we make note of all the hexes it has visited so far.
    // if a hex has already been visited by another branch, then we set the current one as the visitor only if it's shorter
    protected List<MovePath> GenerateAllPaths(MovePath mp) {
        List<MovePath> retval = new ArrayList<>();

        for (MovePath path : generateSidePaths(mp, MoveStepType.TURN_RIGHT)) {
            // we want to avoid adding paths that don't visit new hexes
            // off-board paths will get thrown out later
            if (path.fliesOffBoard() || !pathIsRedundant(path)) {
                retval.add(path);
            }
        }

        for (MovePath path : generateSidePaths(mp, MoveStepType.TURN_LEFT)) {
            // we want to avoid adding paths that don't visit new hexes
            // off-board paths will get thrown out later
            if (path.fliesOffBoard() || !pathIsRedundant(path)) {
                retval.add(path);
            }
        }

        ForwardToTheEnd(mp);
        if (mp.fliesOffBoard() || !pathIsRedundant(mp)) {
            retval.add(mp);
        }

        return retval;
    }

    // generates and returns all paths that stick straight lines out of the current path to the right or left
    protected List<MovePath> generateSidePaths(MovePath mp, MoveStepType stepType) {
        List<MovePath> retval = new ArrayList<>();
        MovePath straightLine = mp.clone();

        boolean firstTurn = true;
        while (straightLine.getFinalVelocityLeft() > 0 &&
                game.getBoard().contains(straightLine.getFinalCoords())) {

            // little dirty hack to get around the problem where if this is the first step of a path
            // we have no last step
            MoveStep currentStep = straightLine.getLastStep();
            if (currentStep == null) {
                currentStep = new MoveStep(straightLine, MoveStepType.NONE);
            }

            int turnCost = currentStep.asfTurnCost(mp.getGame(), stepType, mp.getEntity());

            // if we can turn
            // *and* it's a legal turn
            // ("early" turns before the "free turn" use thrust, so if we go over max thrust, it's an illegal move
            //  and thus not worth evaluating further)
            if (currentStep.canAeroTurn(game) &&
                    currentStep.getMpUsed() <= mp.getEntity().getRunMP() - turnCost) {
                MovePath tiltedPath = straightLine.clone();
                tiltedPath.addStep(stepType);

                // the first time we add a turn, we recurse in the same direction
                if (firstTurn) {
                    // potential to do, depending on demonstrated princess performance:
                    // If we are going to run ourselves off the board, also generate side paths in the opposite direction of stepType
                    // This *may* result in a drastic increase in generated paths, so maybe hold off on it for now.
                    retval.addAll(generateSidePaths(tiltedPath, stepType));
                }

                ForwardToTheEnd(tiltedPath);
                retval.add(tiltedPath);
                firstTurn = false;
            }

            if (straightLine.nextForwardStepOffBoard()) {
                break;
            } else {
                straightLine.addStep(MoveStepType.FORWARDS);
            }
        }

        return retval;
    }

    // helper function to move the path forward until we run out of velocity or off the board
    // "respect" board edges by attempting a turn
    protected void ForwardToTheEnd(MovePath mp) {
        while (mp.getFinalVelocityLeft() > 0) {
            // don't generate an illegal move that flies off the board
            if (!mp.nextForwardStepOffBoard()) {
                mp.addStep(MoveStepType.FORWARDS);
            }

            // if we have arrived on the edge, and we can turn, then attempt to "bounce off" or "ride" the edge.
            // as long as we're not trying to go off board, then forget about it
            // this slightly increases the area covered by the aero in cases where the path takes it near the edge
            if (mp.nextForwardStepOffBoard()
                    && (mp.getEntity().getDamageLevel() != Entity.DMG_CRIPPLED)) {
                final MoveStep lastStep = mp.getLastStep(); // using to prevent unnecessary computation
                if ((lastStep != null) && lastStep.canAeroTurn(game)) {
                    // we want to generate a path that looks like this:
                    // ||
                    // ||
                    // |\
                    // | \
                    // |  \
                    // at this point, we can know that going forward will take us off board
                    // we can either turn right or left
                    // if turning right and going forwards still takes us off board, then we go left
                    // you can approach the top and bottom edges of the board in such a way that neither left
                    // nor right produces an off-board path
                    // there's probably a better mathematical way to formulate this but I'm not really a math guy.
                    mp.addStep(MoveStepType.TURN_RIGHT);
                    if (mp.nextForwardStepOffBoard()) {
                        mp.removeLastStep();
                    }

                    mp.addStep(MoveStepType.TURN_LEFT);
                    if (mp.nextForwardStepOffBoard()) {
                        mp.removeLastStep();
                    }
                }
            }

            // if we're going off board (even after all this turning stuff)
            if (mp.nextForwardStepOffBoard()) {
                if (mp.getEntity().getDamageLevel() != Entity.DMG_CRIPPLED) {
                    mp.addStep(MoveStepType.RETURN);
                } else {
                    // "I'm too beat up, bugging out!"
                    mp.addStep(MoveStepType.FLEE);
                }
                return;
            }
        }
    }

    private Map<Coords, MovePath> visitedCoords = new HashMap<>();
    /**
     * Determines if the given move path is "redundant".
     * In this case, it means that the path has not visited any new hexes.
     * @param mp The move path to examine.
     * @return Whether or not the move path is redundant.
     */
    // goes through a path.
    // if it does not take us off-board, records the coordinates it visits
    // returns true if this path visits hexes that have not been visited before
    protected boolean pathIsRedundant(MovePath mp) {
        boolean newHexVisited = false;

        if (!mp.fliesOffBoard()) {
            for (MoveStep step : mp.getStepVector()) {
                if (!visitedCoords.containsKey(step.getPosition())) {
                    visitedCoords.put(step.getPosition(), mp);
                    newHexVisited = true;
                }
            }
        }

        return !newHexVisited;
    }
}
