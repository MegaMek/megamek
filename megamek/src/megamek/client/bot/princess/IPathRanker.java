/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
*/
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;
import megamek.common.Targetable;

public interface IPathRanker {

    ArrayList<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange, double fallTolerance,
            List<Entity> enemies, List<Entity> friends);

    /**
     * Performs initialization to help speed later calls of rankPath for this
     * unit on this turn. Rankers that extend this class should override this
     * function
     */
    void initUnitTurn(Entity unit, Game game);

    double distanceToClosestEnemy(Entity entity, Coords position, Game game);

    /**
     * Returns distance to the unit's home edge.
     * Gives the distance to the closest edge
     *
     * @param position Final coordinates of the proposed move.
     * @param homeEdge Unit's home edge.
     * @param game The current {@link Game}
     * @return The distance, in hexes to the unit's home edge.
     */
    int distanceToHomeEdge(Coords position, CardinalEdge homeEdge, Game game);

    /**
     * Returns the best path of a list of ranked paths.
     *
     * @param ps The list of ranked paths to process
     * @return "Best" out of those paths
     */
    RankedPath getBestPath(List<RankedPath> ps);

    /**
     * Find the closest enemy to a unit with a path
     */
    Targetable findClosestEnemy(Entity me, Coords position, Game game);

    /**
     * Find the closest enemy to a unit with a path
     */
    Targetable findClosestEnemy(Entity me, Coords position, Game game, boolean includeStrategicTargets);
}
