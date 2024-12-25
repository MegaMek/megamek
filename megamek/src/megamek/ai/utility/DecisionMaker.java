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

import java.util.*;

public interface DecisionMaker<IN_GAME_OBJECT, TARGETABLE> {

    default Optional<Decision<IN_GAME_OBJECT, TARGETABLE>> pickOne(PriorityQueue<ScoredDecision<IN_GAME_OBJECT, TARGETABLE>> scoredDecisions) {
        if (scoredDecisions.isEmpty()) {
            return Optional.empty();
        }

        if (scoredDecisions.size() == 1) {
            return Optional.of(scoredDecisions.poll().getDecisionScoreEvaluator());
        }

        var decisionScoreEvaluators = new ArrayList<Decision<IN_GAME_OBJECT, TARGETABLE>>();
        if (getTopN() > 0) {
            for (int i = 0; i < getTopN(); i++) {
                var scoredDecision = Optional.ofNullable(scoredDecisions.poll());
                if (scoredDecision.isEmpty()) {
                    break;
                }
                decisionScoreEvaluators.add(scoredDecision.get().getDecisionScoreEvaluator());
            }
            Collections.shuffle(decisionScoreEvaluators);
            return Optional.of(decisionScoreEvaluators.get(0));
        } else {
            scoredDecisions.stream().map(ScoredDecision::getDecisionScoreEvaluator).forEach(decisionScoreEvaluators::add);
        }

        Collections.shuffle(decisionScoreEvaluators);
        return Optional.of(decisionScoreEvaluators.get(0));
    }

    default int getTopN() {
        return 1;
    };

    default void scoreAllDecisions(List<Decision<IN_GAME_OBJECT, TARGETABLE>> decisions, DecisionContext<IN_GAME_OBJECT, TARGETABLE> lastContext) {

        double cutoff = 0.0d;
        for (var decision : decisions) {
            double bonus = decision.getDecisionContext().getBonusFactor(lastContext);
            if (bonus < cutoff) {
                continue;
            }
            var decisionScoreEvaluator = decision.getDecisionScoreEvaluator();
            var score = decisionScoreEvaluator.score(decision.getDecisionContext(), getBonusFactor(decision), 0.0d);
            decision.setScore(score);
        }
    }

    double getBonusFactor(Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator);

}
