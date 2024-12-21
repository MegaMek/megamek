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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static megamek.codeUtilities.MathUtility.clamp01;

public class DecisionScoreEvaluator {
    // Represents the decision process
    // Evaluates input via considerations
    //  Scores it
    // if selected result into a decision

    // weight - Weight goes from 1.0 to 5.0
    //      it is an implicit representation of priority
    //      1.0 -> basic action (search enemy)
    //      2.0 -> tactical movement (move to cover, move to flank, move to attack)
    //      3.0 -> Special action usage (spot for indirect fire, LAM conversion)
    //      4.0 -> Emergency (move to cover,
    //      5.0 -> Emergency (move away from orbital strike)
    // name
    // description
    // notes

    // considerations
    //  considerations may have parameters
    // Aggregate all considerations into a single score
    // multiply the score by the weight

    private AbstractAction action;
    private String description;
    private String notes;
    private double weight;
    private final List<Consideration> considerations = new ArrayList<>();

    public DecisionScoreEvaluator(AbstractAction action, String description, String notes, double weight) {
        this(action, description, notes, weight, Collections.emptyList());
    }

    public DecisionScoreEvaluator(AbstractAction action, String description, String notes, double weight, List<Consideration> considerations) {
        this.action = action;
        this.description = description;
        this.notes = notes;
        this.weight = weight;
        this.considerations.addAll(considerations);
    }

    public double score(DecisionContext context, double bonus, double min) {

        var finalScore = bonus;

        for (var consideration : getConsiderations()) {
            if ((0.0f < finalScore) || (0.0f < min)) {
                break;
            }
            var score = consideration.score(context);
            var response = consideration.computeResponseCurve(score);

            finalScore *= clamp01(response);
        }
        // adjustment
        var modificationFactor = 1 - (1 / getConsiderations().size());
        var makeUpValue = (1 - finalScore) * modificationFactor;
        finalScore = finalScore + (makeUpValue * finalScore);

        return finalScore;
    }

    public List<Consideration> getConsiderations() {
        return considerations;
    }

    public DecisionScoreEvaluator addConsideration(Consideration consideration) {
        considerations.add(consideration);
        return this;
    }
}
