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


public class ScoredDecision implements Comparator<ScoredDecision> {

    private final double score;
    private final Decision scoreEvaluator;

    private ScoredDecision(double score, Decision scoreEvaluator) {
        this.score = score;
        this.scoreEvaluator = scoreEvaluator;
    }

    public double getScore() {
        return score;
    }

    public Decision getDecisionScoreEvaluator() {
        return scoreEvaluator;
    }

    @Override
    public int compare(ScoredDecision o1, ScoredDecision o2) {
         // Scores are negative because the priority queue is a min heap and I want max heap
        return Double.compare(-o1.getScore(), -o2.getScore());
    }

    public static  ScoredDecision of(double score, Decision scoreEvaluator) {
        return new ScoredDecision(score, scoreEvaluator);
    }
}
