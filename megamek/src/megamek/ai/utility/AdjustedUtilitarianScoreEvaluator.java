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

public class AdjustedUtilitarianScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> extends UtilitarianScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> {

    public AdjustedUtilitarianScoreEvaluator(DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> decisionScoreEvaluator) {
        super(decisionScoreEvaluator);
    }

    @Override
    public double score(DecisionContext<IN_GAME_OBJECT, TARGETABLE> context, double bonus, IDebugReporter debugReport) {
        double unadjustedScore = super.score(context, bonus, debugReport);
        var modificationFactor = 1 - (1 / getConsiderations().size());
        var makeUpValue = (1 - unadjustedScore) * modificationFactor;
        double finalScore = unadjustedScore + (makeUpValue * unadjustedScore);
        debugReport.newLineIndent(2).append("Adjusted Utilitarian Final score: ").append(finalScore).newLine();
        return finalScore;
    }
}
