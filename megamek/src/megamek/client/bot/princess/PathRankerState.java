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

import java.util.HashMap;
import java.util.Map;

import megamek.common.Coords;
import megamek.common.MovePath;

/**
 * This class handles state information for Princess' path ranking algorithms, as the path ranker and its
 * subclasses are intended to be basically stateless.
 *
 */
public class PathRankerState {
    private Map<MovePath.Key, Double> pathSuccessProbabilities = new HashMap<>();
    private Map<Coords, Double> incomingFriendlyArtilleryDamage = new HashMap<>();

    /**
     * The map of success probabilities for given move paths.
     * The calculation of a move success probability is pretty complex, so we can cache them here.
     * @return Map of path keys to success probabilities.
     */
    public Map<MovePath.Key, Double> getPathSuccessProbabilities() {
        return pathSuccessProbabilities;
    }

    /**
     * Map of coordinates to known incoming artillery damage.
     * @return Map of coordinates.
     */
    public Map<Coords, Double> getIncomingFriendlyArtilleryDamage() {
        return incomingFriendlyArtilleryDamage;
    }

    /**
     * Convenience method that clears the current path ranker state.
     */
    public void clearState() {
        pathSuccessProbabilities.clear();
        incomingFriendlyArtilleryDamage.clear();
    }
}
