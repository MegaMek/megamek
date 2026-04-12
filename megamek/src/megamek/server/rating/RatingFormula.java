/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 */
package megamek.server.rating;

/**
 * Pluggable rating computation for a single head-to-head update.
 */
public interface RatingFormula {

    /**
     * @param currentRating  rating of the player being updated
     * @param opponentRating rating of the opponent
     * @param outcome        result for the current player
     *
     * @return old and new rating for the current player
     */
    RatingDelta calculate(int currentRating, int opponentRating, MatchOutcome outcome);
}
