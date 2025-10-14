/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.util.List;
import java.util.TreeSet;

import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.game.Game;
import megamek.common.units.Targetable;
import megamek.common.moves.MovePath;

public interface IPathRanker {

    TreeSet<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange, double fallTolerance,
          List<Entity> enemies, List<Entity> friends);

    /**
     * Performs initialization to help speed later calls of rankPath for this unit on this turn. Rankers that extend
     * this class should override this function
     */
    void initUnitTurn(Entity unit, Game game);

    double distanceToClosestEnemy(Entity entity, Coords position, Game game);

    /**
     * Returns distance to the unit's home edge. Gives the distance to the closest edge
     *
     * @param position Final coordinates of the proposed move.
     * @param boardId  The board on which to look
     * @param homeEdge Unit's home edge.
     * @param game     The current {@link Game}
     *
     * @return The distance, in hexes to the unit's home edge.
     */
    int distanceToHomeEdge(Coords position, int boardId, CardinalEdge homeEdge, Game game);

    /**
     * Returns the best path of a list of ranked paths.
     *
     * @param ps The list of ranked paths to process
     *
     * @return "Best" out of those paths
     */
    RankedPath getBestPath(TreeSet<RankedPath> ps);

    /**
     * Find the closest enemy to a unit with a path
     */
    Targetable findClosestEnemy(Entity me, Coords position, Game game);

    /**
     * Find the closest enemy to a unit with a path
     */
    default Targetable findClosestEnemy(Entity me, Coords position, Game game, boolean includeStrategicTargets) {
        return findClosestEnemy(me, position, game, includeStrategicTargets, 0);
    }

    /**
     * Find the closest enemy to a unit with a path
     */
    Targetable findClosestEnemy(Entity me, Coords position, Game game, boolean includeStrategicTargets, int minRange);
}
