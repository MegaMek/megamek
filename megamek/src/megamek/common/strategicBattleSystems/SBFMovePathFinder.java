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

package megamek.common.strategicBattleSystems;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.game.Game;
import megamek.common.pathfinder.AbstractPathFinder;
import megamek.common.pathfinder.AdjacencyMap;
import megamek.common.pathfinder.DestinationMap;
import megamek.common.pathfinder.EdgeRelaxer;
import megamek.common.pathfinder.filters.Filter;

public class SBFMovePathFinder extends AbstractPathFinder<BoardLocation, SBFMovePath, SBFMovePath> {

    /**
     * @param edgeDestinationMap functional interface for retrieving destination node of an edge.
     * @param edgeRelaxer        functional interface for calculating relaxed cost.
     * @param edgeAdjacencyMap   functional interface for retrieving neighbouring edges.
     * @param edgeComparator     implementation of path comparator. Each path is defined by its last edge. <i>(path:=
     *                           edge concatenated with the best path to the source of the edge)</i>
     */
    public SBFMovePathFinder(SBFGame game,
          DestinationMap<BoardLocation, SBFMovePath> edgeDestinationMap,
          EdgeRelaxer<SBFMovePath, SBFMovePath> edgeRelaxer,
          AdjacencyMap<SBFMovePath> edgeAdjacencyMap,
          Comparator<SBFMovePath> edgeComparator) {
        super(edgeDestinationMap, edgeRelaxer, edgeAdjacencyMap, edgeComparator);
    }

    /**
     * Produces new instance of shortest path searcher. It will find all the shortest paths between starting points that
     * are reachable with at most maxMp move points.
     *
     * @param maxMP maximum MP that entity can use
     * @param game  The {@link SBFGame}
     */
    public static SBFMovePathFinder moveEnvelopeFinder(int maxMP, SBFGame game) {
        SBFMovePathFinder spf = new SBFMovePathFinder(game, SBFMovePath::getLastPosition, new MovePathRelaxer(),
              new GroundMovementAdjacency(game), Comparator.comparingInt(SBFMovePath::getMpUsed));
        spf.addFilter(new MovePathLengthFilter(maxMP));
        spf.addFilter(new MovePathLegalityFilter());
        return spf;
    }

    /**
     * Produces a new instance of shortest path between starting points and a destination finder. Algorithm will halt
     * after reaching destination.
     * <p>
     * Current implementation uses AStar algorithm.
     *
     * @param game The current {@link Game}
     */
    public static SBFMovePathFinder aStarFinder(BoardLocation destination, SBFGame game) {
        SBFMovePathFinder spf = new SBFMovePathFinder(game, SBFMovePath::getLastPosition, new MovePathRelaxer(),
              new GroundMovementAdjacency(game),
              new MovePathAStarComparator(destination, game.getBoard(destination.boardId())));
        spf.addStopCondition(new DestinationReachedStopCondition(destination));
        spf.addFilter(new MovePathLegalityFilter());
        return spf;
    }

    /**
     * Returns a map of all computed shortest paths. If multiple paths to a single hex, each with different final
     * facing, are present, then the minimal one is chosen for each hex.
     *
     * @return a map of all computed shortest paths.
     */
    public Map<BoardLocation, SBFMovePath> getAllComputedPaths() {
        return getAllComputedCosts(getComparator());
    }

    /**
     * Returns a map of all computed shortest paths
     */
    protected Map<BoardLocation, SBFMovePath> getAllComputedCosts(Comparator<SBFMovePath> comp) {
        Set<BoardLocation> allDestinations = new HashSet<>(getPathCostMap().keySet());

        Map<BoardLocation, SBFMovePath> pathsMap = new HashMap<>();
        for (BoardLocation coords : allDestinations) {
            pathsMap.put(coords, getCost(coords, comp));
        }
        return pathsMap;
    }

    /**
     * Returns computed cost to reach the hex at c coordinates. If multiple path are present with different final
     * facings, the one minimal one is chosen. If none paths are present then {@code null} is returned.
     *
     * @param comp comparator used if multiple paths are present
     *
     * @return shortest path to the hex at c coordinates or {@code null}
     */
    protected @Nullable SBFMovePath getCost(BoardLocation coords, Comparator<SBFMovePath> comp) {
        return getCostOf(coords);
    }

    private static class MovePathLegalityFilter extends Filter<SBFMovePath> {

        @Override
        public boolean shouldStay(SBFMovePath mp) {
            return !mp.isIllegal();
        }
    }

    /**
     * A MovePath comparator that compares movement points spent and distance to destination. If those are equal then
     * MovePaths with more HexesMoved are Preferred. This should considerably speed A* when multiple shortest paths are
     * present.
     * <p>
     * This comparator is used by A* algorithm.
     */
    public static class MovePathAStarComparator implements Comparator<SBFMovePath>, Serializable {
        BoardLocation destination;
        Board board;

        public MovePathAStarComparator(BoardLocation destination, Board board) {
            this.destination = Objects.requireNonNull(destination);
            this.board = board;
        }

        @Override
        public int compare(SBFMovePath first, SBFMovePath second) {
            int h1 = 0, h2 = 0;

            int dd = (first.getMpUsed() + h1) - (second.getMpUsed() + h2);

            if (dd != 0) {
                return dd;
            } else {
                return first.getHexesMoved() - second.getHexesMoved();
            }
        }
    }

    /**
     * Returns the shortest move path to a hex at given coordinates or {@code null} if none is present. If multiple path
     * are present with different final facings, the minimal one is chosen.
     *
     * @param coordinates - the coordinates of the hex
     *
     * @return the shortest move path to hex at given coordinates or {@code null}
     */
    public SBFMovePath getComputedPath(BoardLocation coordinates) {
        return getCost(coordinates, getComparator());
    }
}
