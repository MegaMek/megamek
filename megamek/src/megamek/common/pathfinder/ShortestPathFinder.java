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

package megamek.common.pathfinder;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.pathfinder.comparators.MovePathAStarComparator;
import megamek.common.pathfinder.comparators.MovePathGreedyComparator;
import megamek.common.pathfinder.comparators.MovePathLengthComparator;
import megamek.common.pathfinder.comparators.MovePathMPCostComparator;
import megamek.common.pathfinder.filters.MovePathGreedyFilter;
import megamek.common.pathfinder.filters.MovePathLegalityFilter;
import megamek.common.pathfinder.filters.MovePathLengthFilter;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Implementation of MovePathFinder designed to find the shortest path between two hexes or finding the shortest paths
 * from a single hex to all surrounding.
 *
 * @author Saginatio
 */
public class ShortestPathFinder extends MovePathFinder<MovePath> {
    private static final MMLogger logger = MMLogger.create(ShortestPathFinder.class);

    private ShortestPathFinder(EdgeRelaxer<MovePath, MovePath> costRelaxer,
          Comparator<MovePath> comparator, final MoveStepType stepType, Game game) {
        super(costRelaxer, new NextStepsAdjacencyMap(stepType), comparator, game);
    }

    /**
     * Produces a new instance of shortest path between starting points and a destination finder. Algorithm will halt
     * after reaching destination.
     * <p>
     * Current implementation uses AStar algorithm.
     *
     * @param game The current {@link Game}
     */
    public static ShortestPathFinder newInstanceOfAStar(final Coords destination,
          final MoveStepType stepType, final Game game, int boardId) {
        final ShortestPathFinder spf = new ShortestPathFinder(
              new MovePathRelaxer(),
              new MovePathAStarComparator(destination, stepType, game.getBoard(boardId)),
              stepType, game);

        spf.addStopCondition(new DestinationReachedStopCondition(destination));
        spf.addFilter(new MovePathLegalityFilter(game));
        return spf;
    }

    /**
     * Produces new instance of shortest path searcher. It will find all the shortest paths between starting points that
     * are reachable with at most maxMp move points.
     *
     * @param maxMP    maximum MP that entity can use
     * @param stepType the type of step to use
     * @param game     The current {@link Game}
     */
    public static ShortestPathFinder newInstanceOfOneToAll(int maxMP, MoveStepType stepType, Game game) {
        var shortestPathFinder = new ShortestPathFinder(new MovePathRelaxer(), new MovePathMPCostComparator(),
              stepType, game);
        shortestPathFinder.addFilter(new MovePathLengthFilter(maxMP));
        shortestPathFinder.addFilter(new MovePathLegalityFilter(game));
        return shortestPathFinder;
    }

    /**
     * See {@link ShortestPathFinder#newInstanceOfOneToAll} - this returns a customized ShortestPathFinder to support
     * Aerodyne units.
     *
     * @param maxMP maximum MP that entity can use
     * @param game  The current {@link Game}
     *
     * @return - Customized ShortestPathFinder specifically for Aerodyne unit move envelope.
     */
    public static ShortestPathFinder newInstanceOfOneToAllAero(final int maxMP, final MoveStepType stepType,
          final Game game) {
        final ShortestPathFinder spf = new ShortestPathFinder(
              new AeroMovePathRelaxer(),
              new MovePathLengthComparator(),
              stepType, game);
        spf.addFilter(new MovePathLengthFilter(maxMP));
        spf.addFilter(new MovePathLegalityFilter(game));
        return spf;
    }

    /**
     * Constructs a greedy algorithms. It considers only the moves end closer to destination. Ignores legality of the
     * move. Stops after reaching destination.
     */
    public static ShortestPathFinder newInstanceOfGreedy(final Coords destination, final MoveStepType stepType,
          final Game game) {

        final ShortestPathFinder spf = new ShortestPathFinder(new MovePathRelaxer(),
              new MovePathGreedyComparator(destination),
              stepType, game);

        spf.addStopCondition(new DestinationReachedStopCondition(destination));
        spf.addFilter(new MovePathGreedyFilter(destination));
        return spf;
    }

    /**
     * Returns the shortest move path to a hex at given coordinates or {@code null} if none is present. If multiple path
     * are present with different final facings, the minimal one is chosen.
     *
     * @param coordinates - the coordinates of the hex
     *
     * @return the shortest move path to hex at given coordinates or {@code null}
     */
    public MovePath getComputedPath(Coords coordinates) {
        return getCost(coordinates, getComparator());
    }

    /**
     * Returns a map of all computed shortest paths. If multiple paths to a single hex, each with different final
     * facing, are present, then the minimal one is chosen for each hex.
     *
     * @return a map of all computed shortest paths.
     */
    public Map<Coords, MovePath> getAllComputedPaths() {
        return super.getAllComputedCosts(getComparator());
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return getPathCostMap().values();
    }

    public static int getFacingDiff(final MovePath mp, Coords dest,
          boolean backward) {
        // Facing doesn't matter for jumping
        if (mp.isJumping()) {
            return 0;
        }

        // Ignore facing if we are on the destination
        if (mp.getFinalCoords().equals(dest)) {
            return 0;
        }

        // Direction dest hex is from current location, in hex facings
        int destDir = mp.getFinalCoords().direction(dest);
        // Direction dest hex is from current location, in degrees
        int destAngle = mp.getFinalCoords().degree(dest);
        // Estimate the number of facing changes to reach destination
        int firstFacing = Math.abs(((destDir + (backward ? 3 : 0)) % 6)
              - mp.getFinalFacing());

        // Adjust for circular nature of facing
        if (firstFacing > 3) {
            firstFacing = 6 - firstFacing;
        }

        // Ability to shift can ease facing costs
        if (mp.canShift()) {
            firstFacing = Math.max(0, firstFacing - 1);
        }

        // If the angle to the goal doesn't fall along a hex facing, then we
        // need to make another facing change, which needs to be accounted for
        if ((destAngle % 60) != 0) {
            firstFacing++;
        }
        return firstFacing;
    }

    /**
     * Computes the difference in levels between the current location and the goal location. This prevents the heuristic
     * from under-estimating when a unit is on top of a hill.
     *
     * @param mp     MovePath to evaluate
     * @param dest   Destination coordinates
     * @param board  Board on which the move path takes place
     * @param ignore Whether to ignore this calculation and return 0
     *
     * @return level difference between the final coordinates of the given move path and the destination coordinates
     */
    public static int getLevelDiff(final MovePath mp, Coords dest, Board board, boolean ignore) {
        // Ignore level differences if we're not on the ground
        if (ignore || !board.isGround() || (mp.getFinalElevation() != 0)) {
            return 0;
        }
        Hex currHex = board.getHex(mp.getFinalCoords());
        if (currHex == null) {
            logger.debug("getLevelDiff: currHex was null!\nStart: {}\ncurrHex:  {}\nPath: {}",
                  mp.getStartCoords(),
                  mp.getFinalCoords(),
                  mp);
            return 0;
        }
        Hex destHex = board.getHex(dest);
        if (destHex == null) {
            logger.debug("getLevelDiff: destHex was null!\nStart: {}\ndestHex: {}\nPath: {}",
                  mp.getStartCoords(),
                  dest,
                  mp);
            return 0;
        }
        return Math.abs(destHex.getLevel() - currHex.getLevel());
    }

    /**
     * Computes the difference in elevation between the current location and the goal location. This is important for
     * using determining when elevation change steps should be used.
     *
     */
    public static int getElevationDiff(final MovePath mp, Coords dest, Board board, Entity ent) {
        // We can ignore this if we're jumping
        if (mp.isJumping()) {
            return 0;
        }
        Hex destHex = board.getHex(dest);
        int currElevation = mp.getFinalElevation();
        // Get elevation in destination hex, ignoring buildings
        int destElevation = ent.elevationOccupied(destHex, mp.getFinalElevation());
        // If there's a building, we could stand on it
        if (destHex.containsTerrain(Terrains.BLDG_ELEV)) {
            // Assume that we stay on same level if building is high enough
            if (destHex.terrainLevel(Terrains.BLDG_ELEV) >= currElevation) {
                destElevation = currElevation;
            }
            // If there's a bridge, we could stand on it
        } else if (destHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
            // Assume that we stay on same level if bridge is high enough
            if (destHex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation) {
                destElevation = currElevation;
            }
        }
        int elevationDiff = Math.abs(currElevation - destElevation);
        // Infantry elevation changes are doubled
        if (ent instanceof Infantry) {
            elevationDiff *= 2;
        }
        // Penalty for standing
        if (ent.isProne() && !(mp.contains(MoveStepType.GET_UP) || mp.contains(MoveStepType.CAREFUL_STAND))) {
            elevationDiff += 2;
        }
        return elevationDiff;
    }
}
