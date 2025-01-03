/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.ai.utility;

import java.util.Comparator;


public class ScoredDecision<IN_GAME_OBJECT, TARGETABLE> implements Comparator<ScoredDecision<IN_GAME_OBJECT, TARGETABLE>> {

    private final double score;
    private final Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator;

    private ScoredDecision(double score, Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator) {
        this.score = score;
        this.scoreEvaluator = scoreEvaluator;
    }

    public double getScore() {
        return score;
    }

    public Decision<IN_GAME_OBJECT, TARGETABLE> getDecisionScoreEvaluator() {
        return scoreEvaluator;
    }

    @Override
    public int compare(ScoredDecision o1, ScoredDecision o2) {
         // Scores are negative because the priority queue is a min heap and I want max heap
        return Double.compare(-o1.getScore(), -o2.getScore());
    }

    public static <IN_GAME_OBJECT, TARGETABLE> ScoredDecision<IN_GAME_OBJECT, TARGETABLE> of(double score, Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator) {
        return new ScoredDecision<>(score, scoreEvaluator);
    }
}
