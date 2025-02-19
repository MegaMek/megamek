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

import java.util.Arrays;
import java.util.List;

import static megamek.codeUtilities.MathUtility.clamp01;

public class NeuralNetworkScoreEvaluator implements ScoreEvaluator {

    private final DecisionScoreEvaluator decisionScoreEvaluator;
    private final NeuralNetwork neuralNetwork;
    private final double[] inputs;

    public NeuralNetworkScoreEvaluator(
        DecisionScoreEvaluator decisionScoreEvaluator,
        NeuralNetwork neuralNetwork) {
        this.inputs = new double[neuralNetwork.getInputLayerSize()];
        this.decisionScoreEvaluator = decisionScoreEvaluator;
        this.neuralNetwork = neuralNetwork;
        this.neuralNetwork.assertNumberOfInputs(inputs.length);
    }

    @Override
    public double score(DecisionContext context, double bonus, IDebugReporter debugReport) {
        Arrays.fill(inputs, 0.0);
        var finalScore = bonus;
        var considerationSize = getConsiderations().size();
        debugReport.newLineIndent().append("Consideration size: ").append(considerationSize)
            .newLineIndent(2).append("Bonus: ").append(bonus);
        int i = 0;
        // first 100 inputs are the heat map
        for (var heat : context.getHeatmap()) {
            inputs[i++] = heat;
        }
        // the rest is the considerations
        for (var consideration : getConsiderations()) {
            debugReport.newLineIndent(2).append("Consideration: ").append(consideration);
            if (0.0f >= finalScore) {
                debugReport.newLineIndent(2).append("Final score is 0 or less.");
                break;
            }
            var score = consideration.score(context);
            var response = consideration.computeResponseCurve(score);
            debugReport.newLineIndent(3).append("Score: ").append(score)
                .newLineIndent(3).append("curve: ").append(response)
                .newLineIndent(3).append("currentScore: ").append(finalScore);
            inputs[i++] = clamp01(response);
        }


        finalScore = neuralNetwork.predictSingleOutput(inputs);
        debugReport.newLineIndent(2).append("Utilitarian Final score: ").append(finalScore).newLine();
        return finalScore;
    }

    @Override
    public List<Consideration> getConsiderations() {
        return decisionScoreEvaluator.getConsiderations();
    }
}
