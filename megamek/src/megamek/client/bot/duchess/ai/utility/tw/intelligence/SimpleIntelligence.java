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

package megamek.client.bot.duchess.ai.utility.tw.intelligence;

import megamek.ai.utility.Decision;
import megamek.ai.utility.DecisionMaker;
import megamek.ai.utility.Intelligence;
import megamek.ai.utility.ScoredDecision;
import megamek.common.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * SimpleIntelligence does not implement memory or learning in any way, has no stickyness for decisions, and does not
 * implement any special behaviors, uses BEST for decision process.
 *
 * @author Luana Coppio
 */
public class SimpleIntelligence implements Intelligence<Entity, Entity> {

    private final List<Decision<Entity, Entity>> decisions = new ArrayList<>();
    private final DecisionMaker<Entity, Entity> decisionMaker = new DecisionMaker<>() {
        @Override
        public Optional<Decision<Entity, Entity>> pickOne(PriorityQueue<ScoredDecision<Entity, Entity>> scoredDecisions) {
            return DecisionMaker.super.pickOne(scoredDecisions);
        }

        @Override
        public double getBonusFactor(Decision<Entity, Entity> scoreEvaluator) {
            return 0;
        }
    };

    private SimpleIntelligence() {
        // empty constructor
    }

    @Override
    public void update(Intelligence<Entity, Entity> intelligence) {
        // there is nothing to update
    }

    @Override
    public void addDecisionScoreEvaluator(Decision<Entity, Entity> decision) {
        decisions.add(decision);
    }

    @Override
    public List<Decision<Entity, Entity>> getDecisions() {
        return decisions;
    }

    @Override
    public DecisionMaker<Entity, Entity> getDecisionMaker() {
        return decisionMaker;
    }

    @Override
    public double getBonusFactor(Decision<Entity, Entity> scoreEvaluator) {
        return 0;
    }

    public static SimpleIntelligence create() {
        return new SimpleIntelligence();
    }
}
