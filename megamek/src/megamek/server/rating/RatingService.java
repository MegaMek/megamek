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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Player;

/**
 * In-memory per-player ratings; humans and bots use the same code paths ({@link Player#equals(Object)} is id-based).
 */
public final class RatingService {

    public static final int DEFAULT_RATING = 1000;

    private final RatingFormula formula;
    private final Map<Player, Integer> ratings = new HashMap<>();

    public RatingService(RatingFormula formula) {
        this.formula = formula;
    }

    public int getRating(Player player) {
        return ratings.computeIfAbsent(player, p -> DEFAULT_RATING);
    }

    public void setRating(Player player, int rating) {
        ratings.put(player, rating);
    }

    /**
     * Draw: each unordered pair is updated with {@link MatchOutcome#DRAW} for both sides.
     * Win: each winner is paired against each non-winner with WIN/LOSS (sequential updates).
     */
    public void updateRatings(MatchResult result) {
        List<Player> participants = result.participants();
        if (participants.size() < 2) {
            return;
        }
        participants.forEach(this::getRating);

        if (result.draw()) {
            for (int i = 0; i < participants.size(); i++) {
                for (int j = i + 1; j < participants.size(); j++) {
                    applyDrawPair(participants.get(i), participants.get(j));
                }
            }
            return;
        }

        List<Player> winners = result.winners();
        if (winners.isEmpty()) {
            return;
        }

        List<Player> losers = new ArrayList<>();
        for (Player p : participants) {
            if (!winners.contains(p)) {
                losers.add(p);
            }
        }
        if (losers.isEmpty()) {
            return;
        }

        for (Player winner : winners) {
            for (Player loser : losers) {
                applyWinLossPair(winner, loser);
            }
        }
    }

    private void applyDrawPair(Player a, Player b) {
        int ra = getRating(a);
        int rb = getRating(b);
        RatingDelta deltaA = formula.calculate(ra, rb, MatchOutcome.DRAW);
        RatingDelta deltaB = formula.calculate(rb, ra, MatchOutcome.DRAW);
        setRating(a, deltaA.newRating());
        setRating(b, deltaB.newRating());
    }

    private void applyWinLossPair(Player winner, Player loser) {
        int rw = getRating(winner);
        int rl = getRating(loser);
        RatingDelta deltaW = formula.calculate(rw, rl, MatchOutcome.WIN);
        RatingDelta deltaL = formula.calculate(rl, rw, MatchOutcome.LOSS);
        setRating(winner, deltaW.newRating());
        setRating(loser, deltaL.newRating());
    }
}
