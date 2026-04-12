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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RatingServiceTest {

    private RatingService service;

    @BeforeEach
    void setUp() {
        service = new RatingService(new SimpleEloRatingFormula());
    }

    @Test
    void humanVsHumanWinnerGainsLoserDrops() {
        Player winner = new Player(0, "HumanA");
        Player loser = new Player(1, "HumanB");
        assertEquals(RatingService.DEFAULT_RATING, service.getRating(winner));
        assertEquals(RatingService.DEFAULT_RATING, service.getRating(loser));

        service.updateRatings(new MatchResult(List.of(winner, loser), List.of(winner), false));

        assertTrue(service.getRating(winner) > RatingService.DEFAULT_RATING);
        assertTrue(service.getRating(loser) < RatingService.DEFAULT_RATING);
    }

    @Test
    void drawBetweenEqualRatingsLeavesRatingsAtDefault() {
        Player a = new Player(0, "P1");
        Player b = new Player(1, "P2");

        service.updateRatings(new MatchResult(List.of(a, b), List.of(), true));

        assertEquals(RatingService.DEFAULT_RATING, service.getRating(a));
        assertEquals(RatingService.DEFAULT_RATING, service.getRating(b));
    }

    @Test
    void botBeatsHumanAdjustsBothRatings() {
        Player human = new Player(0, "Human");
        Player bot = new Player(1, "Princess");
        bot.setBot(true);

        int humanBefore = service.getRating(human);
        int botBefore = service.getRating(bot);

        service.updateRatings(new MatchResult(List.of(human, bot), List.of(bot), false));

        assertTrue(service.getRating(bot) > botBefore);
        assertTrue(service.getRating(human) < humanBefore);
    }

    @Nested
    class ExactValueTests {

        @Test
        void winBetweenEqualRatingsProducesExactValues() {
            Player winner = new Player(0, "W");
            Player loser = new Player(1, "L");

            service.updateRatings(new MatchResult(List.of(winner, loser), List.of(winner), false));

            assertEquals(1016, service.getRating(winner));
            assertEquals(984, service.getRating(loser));
        }

        @Test
        void drawBetweenUnequalRatingsShiftsBothRatings() {
            Player strong = new Player(0, "Strong");
            Player weak = new Player(1, "Weak");
            service.setRating(strong, 1200);
            service.setRating(weak, 800);

            service.updateRatings(new MatchResult(List.of(strong, weak), List.of(), true));

            assertEquals(1187, service.getRating(strong));
            assertEquals(813, service.getRating(weak));
        }
    }

    @Nested
    class NoOpEdgeCases {

        @Test
        void singleParticipantIsNoOp() {
            Player solo = new Player(0, "Solo");

            service.updateRatings(new MatchResult(List.of(solo), List.of(solo), false));

            assertEquals(RatingService.DEFAULT_RATING, service.getRating(solo));
        }

        @Test
        void emptyParticipantListIsNoOp() {
            service.updateRatings(new MatchResult(List.of(), List.of(), false));
        }

        @Test
        void allParticipantsAreWinnersIsNoOp() {
            Player p1 = new Player(0, "P1");
            Player p2 = new Player(1, "P2");

            service.updateRatings(new MatchResult(List.of(p1, p2), List.of(p1, p2), false));

            assertEquals(RatingService.DEFAULT_RATING, service.getRating(p1));
            assertEquals(RatingService.DEFAULT_RATING, service.getRating(p2));
        }

        @Test
        void noWinnersAndNotDrawIsNoOp() {
            Player p1 = new Player(0, "P1");
            Player p2 = new Player(1, "P2");

            service.updateRatings(new MatchResult(List.of(p1, p2), List.of(), false));

            assertEquals(RatingService.DEFAULT_RATING, service.getRating(p1));
            assertEquals(RatingService.DEFAULT_RATING, service.getRating(p2));
        }
    }

    @Nested
    class MultiPlayerMatches {

        @Test
        void threePlayersOneWinnerTwoLosers() {
            Player p1 = new Player(0, "P1");
            Player p2 = new Player(1, "P2");
            Player p3 = new Player(2, "P3");

            service.updateRatings(new MatchResult(List.of(p1, p2, p3), List.of(p1), false));

            assertEquals(1031, service.getRating(p1));
            assertEquals(984, service.getRating(p2));
            assertEquals(985, service.getRating(p3));
        }

        @Test
        void threePlayerDrawAllEqualRatingsUnchanged() {
            Player p1 = new Player(0, "P1");
            Player p2 = new Player(1, "P2");
            Player p3 = new Player(2, "P3");

            service.updateRatings(new MatchResult(List.of(p1, p2, p3), List.of(), true));

            assertEquals(RatingService.DEFAULT_RATING, service.getRating(p1));
            assertEquals(RatingService.DEFAULT_RATING, service.getRating(p2));
            assertEquals(RatingService.DEFAULT_RATING, service.getRating(p3));
        }

        @Test
        void twoWinnersOneLoser() {
            Player w1 = new Player(0, "W1");
            Player w2 = new Player(1, "W2");
            Player loser = new Player(2, "L");

            service.updateRatings(new MatchResult(List.of(w1, w2, loser), List.of(w1, w2), false));

            assertEquals(1016, service.getRating(w1));
            assertEquals(1015, service.getRating(w2));
            assertEquals(969, service.getRating(loser));
        }
    }

    @Nested
    class StateManagement {

        @Test
        void setRatingThenGetRatingReturnsSetValue() {
            Player player = new Player(0, "P");
            service.setRating(player, 1500);
            assertEquals(1500, service.getRating(player));
        }

        @Test
        void getRatingForUnknownPlayerReturnsDefault() {
            Player unknown = new Player(99, "Unknown");
            assertEquals(RatingService.DEFAULT_RATING, service.getRating(unknown));
        }

        @Test
        void ratingsAccumulateAcrossMultipleMatches() {
            Player p1 = new Player(0, "P1");
            Player p2 = new Player(1, "P2");

            service.updateRatings(new MatchResult(List.of(p1, p2), List.of(p1), false));
            assertEquals(1016, service.getRating(p1));
            assertEquals(984, service.getRating(p2));

            service.updateRatings(new MatchResult(List.of(p1, p2), List.of(p1), false));
            assertEquals(1031, service.getRating(p1));
            assertEquals(969, service.getRating(p2));
        }

        @Test
        void preSetRatingsAreUsedInCalculation() {
            Player strong = new Player(0, "Strong");
            Player weak = new Player(1, "Weak");
            service.setRating(strong, 1400);
            service.setRating(weak, 600);

            service.updateRatings(new MatchResult(List.of(strong, weak), List.of(strong), false));

            assertEquals(1400, service.getRating(strong));
            assertEquals(600, service.getRating(weak));
        }
    }

    @Nested
    class FormulaPluggability {

        @Test
        void customFormulaIsUsedInsteadOfElo() {
            RatingFormula stubFormula = (current, opponent, outcome) -> new RatingDelta(current, current + 100);
            RatingService customService = new RatingService(stubFormula);

            Player winner = new Player(0, "W");
            Player loser = new Player(1, "L");

            customService.updateRatings(new MatchResult(List.of(winner, loser), List.of(winner), false));

            assertEquals(RatingService.DEFAULT_RATING + 100, customService.getRating(winner));
            assertEquals(RatingService.DEFAULT_RATING + 100, customService.getRating(loser));
        }
    }
}
