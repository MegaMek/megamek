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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SimpleEloRatingFormulaTest {

    private SimpleEloRatingFormula formula;

    @BeforeEach
    void setUp() {
        formula = new SimpleEloRatingFormula();
    }

    @Test
    void winWithEqualRatingsGives16PointGain() {
        RatingDelta delta = formula.calculate(1000, 1000, MatchOutcome.WIN);
        assertEquals(1000, delta.oldRating());
        assertEquals(1016, delta.newRating());
    }

    @Test
    void lossWithEqualRatingsGives16PointDrop() {
        RatingDelta delta = formula.calculate(1000, 1000, MatchOutcome.LOSS);
        assertEquals(1000, delta.oldRating());
        assertEquals(984, delta.newRating());
    }

    @Test
    void drawWithEqualRatingsProducesNoChange() {
        RatingDelta delta = formula.calculate(1000, 1000, MatchOutcome.DRAW);
        assertEquals(1000, delta.oldRating());
        assertEquals(1000, delta.newRating());
    }

    @Test
    void winAgainstMuchWeakerGivesMinimalGain() {
        RatingDelta delta = formula.calculate(1400, 600, MatchOutcome.WIN);
        assertEquals(1400, delta.oldRating());
        assertEquals(1400, delta.newRating());
    }

    @Test
    void lossAgainstMuchWeakerGivesMaximumDrop() {
        RatingDelta delta = formula.calculate(1400, 600, MatchOutcome.LOSS);
        assertEquals(1400, delta.oldRating());
        assertEquals(1368, delta.newRating());
    }

    @Test
    void winAgainstMuchStrongerGivesMaximumGain() {
        RatingDelta delta = formula.calculate(600, 1400, MatchOutcome.WIN);
        assertEquals(600, delta.oldRating());
        assertEquals(632, delta.newRating());
    }

    @Test
    void lossAgainstMuchStrongerGivesMinimalDrop() {
        RatingDelta delta = formula.calculate(600, 1400, MatchOutcome.LOSS);
        assertEquals(600, delta.oldRating());
        assertEquals(600, delta.newRating());
    }

    @Test
    void winnerGainAndLoserLossSumToZeroForEqualRatings() {
        RatingDelta winDelta = formula.calculate(1000, 1000, MatchOutcome.WIN);
        RatingDelta lossDelta = formula.calculate(1000, 1000, MatchOutcome.LOSS);
        int totalChange = (winDelta.newRating() - 1000) + (lossDelta.newRating() - 1000);
        assertEquals(0, totalChange);
    }

    @Test
    void winnerGainAndLoserLossSumToZeroForUnequalRatings() {
        RatingDelta winDelta = formula.calculate(1200, 800, MatchOutcome.WIN);
        RatingDelta lossDelta = formula.calculate(800, 1200, MatchOutcome.LOSS);
        int totalChange = (winDelta.newRating() - 1200) + (lossDelta.newRating() - 800);
        assertEquals(0, totalChange);
    }

    @Test
    void drawBetweenUnequalRatingsShiftsTowardsParity() {
        RatingDelta strongerDelta = formula.calculate(1200, 800, MatchOutcome.DRAW);
        RatingDelta weakerDelta = formula.calculate(800, 1200, MatchOutcome.DRAW);
        assertEquals(1200, strongerDelta.oldRating());
        assertEquals(1187, strongerDelta.newRating());
        assertEquals(800, weakerDelta.oldRating());
        assertEquals(813, weakerDelta.newRating());
    }

    @Test
    void ratingOfZeroDoesNotPreventCalculation() {
        RatingDelta winDelta = formula.calculate(0, 1000, MatchOutcome.WIN);
        assertEquals(0, winDelta.oldRating());
        assertEquals(32, winDelta.newRating());

        RatingDelta lossDelta = formula.calculate(0, 1000, MatchOutcome.LOSS);
        assertEquals(0, lossDelta.oldRating());
        assertEquals(0, lossDelta.newRating());
    }

    @Test
    void ratingCanGoNegative() {
        RatingDelta delta = formula.calculate(0, 0, MatchOutcome.LOSS);
        assertEquals(0, delta.oldRating());
        assertEquals(-16, delta.newRating());
    }

    @Test
    void oldRatingFieldAlwaysMatchesInput() {
        for (MatchOutcome outcome : MatchOutcome.values()) {
            RatingDelta delta = formula.calculate(1234, 5678, outcome);
            assertEquals(1234, delta.oldRating());
        }
    }

    static Stream<Arguments> drawRatingPairs() {
        return Stream.of(
            Arguments.of(1000, 1000),
            Arguments.of(1200, 800),
            Arguments.of(1500, 500),
            Arguments.of(0, 2000)
        );
    }

    @ParameterizedTest
    @MethodSource("drawRatingPairs")
    void drawPreservesZeroSumForVariousRatingPairs(int ratingA, int ratingB) {
        RatingDelta deltaA = formula.calculate(ratingA, ratingB, MatchOutcome.DRAW);
        RatingDelta deltaB = formula.calculate(ratingB, ratingA, MatchOutcome.DRAW);
        int totalChange = (deltaA.newRating() - ratingA) + (deltaB.newRating() - ratingB);
        assertEquals(0, totalChange);
    }
}
