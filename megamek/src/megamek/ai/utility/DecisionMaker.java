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

    default Optional<Decision<IN_GAME_OBJECT, TARGETABLE>> pickOne(TreeSet<ScoredDecision<IN_GAME_OBJECT, TARGETABLE>> scoredDecisions) {
        if (scoredDecisions.isEmpty()) {
            return Optional.empty();
        }

        if (scoredDecisions.size() == 1) {
            return Optional.of(scoredDecisions.first().getDecisionScoreEvaluator());
        }

        var decisionScoreEvaluators = new ArrayList<Decision<IN_GAME_OBJECT, TARGETABLE>>();
        if (getTopN() > 0) {
            for (int i = 0; i < getTopN(); i++) {
                if (scoredDecisions.isEmpty()) {
                    break;
                }
                var scoredDecision = scoredDecisions.first();
                decisionScoreEvaluators.add(scoredDecision.getDecisionScoreEvaluator());
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

    default void scoreAllDecisions(List<Decision<IN_GAME_OBJECT, TARGETABLE>> decisions, List<DecisionContext<IN_GAME_OBJECT, TARGETABLE>> contexts) {
        var debugReporter = new DebugReporter();
        for (var context : contexts) {
            double cutoff = 0.0d;
            for (var decision : decisions) {
                double bonus = decision.getDecisionContext().getBonusFactor(context);
                if (bonus < cutoff) {
                    continue;
                }
                var decisionScoreEvaluator = decision.getDecisionScoreEvaluator();
                var score = decisionScoreEvaluator.score(decision.getDecisionContext(), getBonusFactor(decision), debugReporter);
                decision.setScore(score);
            }
        }
    }

    double getBonusFactor(Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator);

}
