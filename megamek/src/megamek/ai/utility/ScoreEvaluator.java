/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

/**
 * Interface for scoring a decision.
 * @author Luana Coppio
 */
public interface ScoreEvaluator {

    enum ScoreType {
        GEOMETRIC_MEAN,
        UTILITARIAN,
        ADJUSTED_UTILITARIAN,
        NEURAL_NETWORK,
    }

    default ScoreEvaluator getScoreEvaluator(ScoreType scoreType, DecisionScoreEvaluator decisionScoreEvaluator) {
        return switch (scoreType) {
            case GEOMETRIC_MEAN -> new GeometricMeanScoreEvaluator(decisionScoreEvaluator);
            case UTILITARIAN -> new UtilitarianScoreEvaluator(decisionScoreEvaluator);
            case ADJUSTED_UTILITARIAN -> new AdjustedUtilitarianScoreEvaluator(decisionScoreEvaluator);
            case NEURAL_NETWORK -> new NeuralNetworkScoreEvaluator(decisionScoreEvaluator,
                NeuralNetworkFactory.createNeuralNetworkForConsiderationsAndThreatHeatmap(
                    50,
                    50,
                    12));
        };
    }

    double score(DecisionContext context, double bonus, IDebugReporter debugReport);
    List<Consideration> getConsiderations();
}
