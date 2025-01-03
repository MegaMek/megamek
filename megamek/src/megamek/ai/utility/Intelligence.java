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

import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

public interface Intelligence<IN_GAME_OBJECT, TARGETABLE> {

    void update(Intelligence<IN_GAME_OBJECT, TARGETABLE> intelligence);
    void addDecisionScoreEvaluator(Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator);
    List<Decision<IN_GAME_OBJECT, TARGETABLE>> getDecisions();
    DecisionMaker<IN_GAME_OBJECT, TARGETABLE> getDecisionMaker();
    double getBonusFactor(Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator);

    default void clearDecisionScoreEvaluators() {
        getDecisions().clear();
    }

    default void scoreAllDecisions(DecisionContext<IN_GAME_OBJECT, TARGETABLE> context) {
        getDecisionMaker().scoreAllDecisions(getDecisions(), context);
    }

    default Optional<Decision<IN_GAME_OBJECT, TARGETABLE>> pickOne(PriorityQueue<ScoredDecision<IN_GAME_OBJECT, TARGETABLE>> scoredDecisions) {
        return getDecisionMaker().pickOne(scoredDecisions);
    }
}
