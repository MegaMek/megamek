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
 * Minimal Elo-style update (K=32, standard expected score, 1 / 0 / 0.5 actual scores).
 */
public final class SimpleEloRatingFormula implements RatingFormula {

    private static final int K = 32;

    @Override
    public RatingDelta calculate(int currentRating, int opponentRating, MatchOutcome outcome) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - currentRating) / 400.0));
        double actual = switch (outcome) {
            case WIN -> 1.0;
            case LOSS -> 0.0;
            case DRAW -> 0.5;
        };
        int newRating = (int) Math.round(currentRating + K * (actual - expected));
        return new RatingDelta(currentRating, newRating);
    }
}
