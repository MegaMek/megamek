/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.common.minefield;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Hex;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Minefield deployment planner that randomly selects positions on the board. It focuses on
 * positions that are paved or have vegetation, but will also select clear hexes if it runs out of
 * other options.
 * @author Luana Coppio
 */
public class RandomMinefieldDeploymentPlanner implements MinefieldDeploymentPlanner {

    private final Board board;

    public RandomMinefieldDeploymentPlanner(Board board) {
        this.board = board;
    }

    @Override
    public Deque<Coords> getRandomMinefieldPositions(int numberOfCoords) {
        Set<Coords> coordsSet = new HashSet<>();
        int maxTries = numberOfCoords * 50;

        while (coordsSet.size() < numberOfCoords && maxTries > 0) {
            Coords coords = new Coords(Compute.randomInt(board.getWidth()),
                  Compute.randomInt(board.getHeight()));
            Hex hex = board.getHex(coords);
            if ((hex != null)
                  && !hex.hasDepth1WaterOrDeeper()
                  && (hex.hasPavementOrRoad() || hex.hasVegetation()
                  || (hex.isClearHex() && (maxTries < numberOfCoords * 10))))
            {
                coordsSet.add(coords);
            }
            maxTries--;
        }
        return new ArrayDeque<>(coordsSet);
    }
}
