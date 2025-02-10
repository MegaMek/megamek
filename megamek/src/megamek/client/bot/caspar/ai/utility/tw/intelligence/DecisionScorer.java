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
 package megamek.client.bot.caspar.ai.utility.tw.intelligence;

 import megamek.ai.utility.*;
 import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
 import megamek.client.bot.princess.*;
 import megamek.common.*;
 import megamek.logging.MMLogger;

 import java.util.*;

 /**
  * This class is responsible for scoring all decisions.
  * Implements DecisionMaker interface to provide a common interface for decision scoring.
  */
class DecisionScorer implements DecisionMaker<Entity, Entity, RankedPath> {
    private final static MMLogger logger = MMLogger.create(DecisionScorer.class);

    public DecisionScorer() {
    }

    @Override
    public TreeSet<RankedPath> scoreAllDecisions(List<Decision<Entity, Entity>> decisions, List<DecisionContext<Entity, Entity>> contexts) {
        TreeSet<RankedPath> returnPaths = new TreeSet<>(Collections.reverseOrder());
        for (var decision : decisions) {
            double cutoff = 0.0d;
            for (var context : contexts) {
                TWDecisionContext decisionContext = (TWDecisionContext) context;
                decision.setDecisionContext(decisionContext);
                double bonus = decisionContext.getBonusFactor();
                if (bonus < cutoff) {
                    continue;
                }
                var decisionScoreEvaluator = decision.getDecisionScoreEvaluator();
                var debugReporter = initializeDebugReporterPreAllocated(decision);
                var score = decisionScoreEvaluator.score(decisionContext, getBonusFactor(decision), debugReporter) * bonus;
                var rankedPath = new RankedPath(score, decisionContext.getMovePath(), debugReporter.getReport(), decisionContext.getExpectedDamage());
                if (debugReporter.enabled()) {
                    logger.info(debugReporter.getReport());
                }
                returnPaths.add(rankedPath);
            }
        }
        return returnPaths;
    }

    private IDebugReporter initializeDebugReporterPreAllocated(Decision<Entity, Entity> decision) {
        if (!logger.isDebugEnabled()) {
            return IDebugReporter.noOp;
        }
        int projectedSizeForReport = 150 + 256 * decision.getDecisionScoreEvaluator().getConsiderations().size();
        var debugReporter = new DebugReporter(projectedSizeForReport);
        return debugReporter.append(decision.getName()).append("::").append(decision.getDecisionScoreEvaluator().getName());
    }

    @Override
    public Optional<RankedPath> pickOne(TreeSet<RankedPath> scoredDecisions) {
        if (scoredDecisions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(scoredDecisions.first());
    }

    @Override
    public double getBonusFactor(Decision<Entity, Entity> scoreEvaluator) {
        return scoreEvaluator.getWeight();
    }
}
