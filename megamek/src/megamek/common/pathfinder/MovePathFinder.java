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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.pathfinder.filters.MovePathLegalityFilter;

/**
 * Generic implementation of AbstractPathFinder when we restrict graph nodes to (coordinates x facing) and edges to
 * MovePaths. Provides useful implementations of functional interfaces defined in AbstractPathFinder.
 */
public class MovePathFinder<C> extends AbstractPathFinder<CoordsWithFacing, C, MovePath> {
    /**
     * This constant defines the maximum number of times it makes sense for a unit to turn in a row - if you're turning
     * right four times, you might as well turn left twice instead.
     */
    static final int MAX_TURN_COUNT = 3;

    /**
     * Creates a new instance of MovePathFinder. Sets DestinationMap to {@link MovePathDestinationMap} and adds
     * {@link MovePathLegalityFilter}. Rest of the methods needed by AbstractPathFinder have to be passed as a
     * parameter.
     */
    public MovePathFinder(EdgeRelaxer<C, MovePath> edgeRelaxer, AdjacencyMap<MovePath> edgeAdjacencyMap,
          Comparator<MovePath> comparator, Game game) {
        super(new MovePathDestinationMap(),
              edgeRelaxer,
              edgeAdjacencyMap,
              comparator);
    }

    /**
     * Returns a map of all computed shortest paths
     */
    protected Map<Coords, C> getAllComputedCosts(Comparator<C> comp) {
        Set<Coords> computedCoords = new HashSet<>();
        Map<CoordsWithFacing, C> nodes = getPathCostMap();
        for (CoordsWithFacing cf : nodes.keySet()) {
            computedCoords.add(cf.coords());
        }

        Map<Coords, C> pathsMap = new HashMap<>();
        for (Coords coords : computedCoords) {
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
    protected @Nullable C getCost(Coords coords, Comparator<C> comp) {
        List<CoordsWithFacing> allFacings = CoordsWithFacing.getAllFacingsAt(coords);
        List<C> paths = new ArrayList<>();
        for (CoordsWithFacing n : allFacings) {
            C cost = getCostOf(n);
            if (cost != null) {
                paths.add(cost);
            }
        }

        return paths.isEmpty() ? null : Collections.min(paths, comp);
    }
}
