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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecision;

import java.util.StringJoiner;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TWDecision.class, name = "TWDecision"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Decision<IN_GAME_OBJECT, TARGETABLE> implements NamedObject {
    private String name;
    private String description;
    private double weight;
    private DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> decisionScoreEvaluator;
    @JsonIgnore
    private transient double score;
    @JsonIgnore
    private transient DecisionContext<IN_GAME_OBJECT, TARGETABLE> decisionContext;

    public Decision() {
    }

    public Decision(String name, String description, double weight, DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> decisionScoreEvaluator) {
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.decisionScoreEvaluator = decisionScoreEvaluator;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> getDecisionScoreEvaluator() {
        return decisionScoreEvaluator;
    }

    public void setDecisionScoreEvaluator(DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> decisionScoreEvaluator) {
        this.decisionScoreEvaluator = decisionScoreEvaluator;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public DecisionContext<IN_GAME_OBJECT, TARGETABLE> getDecisionContext() {
        return decisionContext;
    }

    public void setDecisionContext(DecisionContext<IN_GAME_OBJECT, TARGETABLE> decisionContext) {
        this.decisionContext = decisionContext;
    }

    public abstract Decision<IN_GAME_OBJECT, TARGETABLE> copy();

    @Override
    public String toString() {
        return new StringJoiner(", ", Decision.class.getSimpleName() + " [", "]")
            .add("weight=" + weight)
            .add("score=" + score)
            .add("decisionScoreEvaluator=" + decisionScoreEvaluator)
            .add("decisionContext=" + decisionContext)
            .toString();
    }
}
