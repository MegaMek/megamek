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

public class UtilitarianScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> implements ScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> {

    private final DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> decisionScoreEvaluator;

    public UtilitarianScoreEvaluator(DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> decisionScoreEvaluator) {
        this.decisionScoreEvaluator = decisionScoreEvaluator;
    }

    @Override
    public double score(DecisionContext<IN_GAME_OBJECT, TARGETABLE> context, double bonus, IDebugReporter debugReport) {
        var finalScore = bonus;
        var considerationSize = getConsiderations().size();
        debugReport.newLineIndent().append("Consideration size: ").append(considerationSize)
            .newLineIndent(2).append("Bonus: ").append(bonus);

        for (var consideration : getConsiderations()) {
            debugReport.newLineIndent(2).append("Consideration: ").append(consideration);
            if (0.0f >= finalScore) {
                debugReport.newLineIndent(2).append("Final score is 0 or less.");
                break;
            }
            var score = consideration.score(context);
            var response = consideration.computeResponseCurve(score);
            finalScore *= clamp01(response);

            debugReport.newLineIndent(3).append("Score: ").append(score)
                .newLineIndent(3).append("curve: ").append(response)
                .newLineIndent(3).append("currentScore: ").append(finalScore);
        }
        debugReport.newLineIndent(2).append("Utilitarian Final score: ").append(finalScore).newLine();
        return finalScore;
    }

    @Override
    public List<Consideration<IN_GAME_OBJECT, TARGETABLE>> getConsiderations() {
        return decisionScoreEvaluator.getConsiderations();
    }
}
