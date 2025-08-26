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

import java.util.HashMap;
import java.util.Map;

import megamek.common.board.Coords;
import megamek.common.moves.Key;

/**
 * This class handles state information for Princess' path ranking algorithms, as the path ranker and its subclasses are
 * intended to be basically stateless.
 */
public class PathRankerState {
    private final Map<Key, Double> pathSuccessProbabilities = new HashMap<>();
    private final Map<Coords, Double> incomingFriendlyArtilleryDamage = new HashMap<>();

    /**
     * The map of success probabilities for given move paths. The calculation of a move success probability is pretty
     * complex, so we can cache them here.
     *
     * @return Map of path keys to success probabilities.
     */
    public Map<Key, Double> getPathSuccessProbabilities() {
        return pathSuccessProbabilities;
    }

    /**
     * Map of coordinates to known incoming artillery damage.
     *
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
