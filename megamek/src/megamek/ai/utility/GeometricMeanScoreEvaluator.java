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

import static megamek.codeUtilities.MathUtility.clamp01;

public class GeometricMeanScoreEvaluator implements ScoreEvaluator {

    private final DecisionScoreEvaluator decisionScoreEvaluator;
    private final double[] scores;

    public GeometricMeanScoreEvaluator(DecisionScoreEvaluator decisionScoreEvaluator) {
        this.decisionScoreEvaluator = decisionScoreEvaluator;
        this.scores = new double[decisionScoreEvaluator.getConsiderations().size()];
    }

    @Override
    public double score(DecisionContext context, double bonus, IDebugReporter debugReport) {
        var considerationSize = getConsiderations().size();
        debugReport.newLineIndent().append("Consideration size: ").append(considerationSize)
            .newLineIndent(2).append("Bonus: ").append(bonus);
        int resultIndex = 0;
        for (var consideration : getConsiderations()) {
            var score = consideration.score(context);
            var response = consideration.computeResponseCurve(score);
            scores[resultIndex++] = clamp01(response);

            debugReport.newLineIndent(2).append("Consideration: ").append(consideration);
            debugReport.newLineIndent(3).append("Score: ").append(score);
            debugReport.newLineIndent(3).append("curve: ").append(response);
            debugReport.newLineIndent(3).append("currentScore: not available");
        }

        double logSum = 0.0 + Math.log(bonus);
        int count = 0;
        for (double f : scores) {
            double safeFactor = Math.max(0.01, f);
            logSum += Math.log(safeFactor);
            count++;
        }
        double geometricMean = Math.exp(logSum / count);

        debugReport.newLineIndent(2).append("Geometric Mean Final score: ").append(geometricMean).newLine();
        return geometricMean;
    }

    @Override
    public List<Consideration> getConsiderations() {
        return decisionScoreEvaluator.getConsiderations();
    }
}
