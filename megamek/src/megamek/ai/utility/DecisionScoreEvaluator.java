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
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionScoreEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TWDecisionScoreEvaluator.class, name = "TWDecisionScoreEvaluator"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> implements NamedObject, ScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> {
    private String name;
    private String description;
    private String notes;
    private ScoreType scoreType;
    private final List<Consideration<IN_GAME_OBJECT, TARGETABLE>> considerations = new ArrayList<>();
    @JsonIgnore
    private transient ScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator;

    public DecisionScoreEvaluator() {
        // no-args constructor for Jackson
    }

    public DecisionScoreEvaluator(String name, String description, String notes, ScoreType scoreType) {
        this(name, description, notes, scoreType, Collections.emptyList());
    }

    public DecisionScoreEvaluator(
        String name, String description,
        String notes,
        ScoreType scoreType,
        List<Consideration<IN_GAME_OBJECT, TARGETABLE>> considerations) {
        this.name = name;
        this.description = description;
        this.notes = notes;
        this.considerations.addAll(considerations);
        this.scoreType = scoreType;
        this.scoreEvaluator = getScoreEvaluator(scoreType, this);
    }

    @JsonIgnore
    public double score(DecisionContext<IN_GAME_OBJECT, TARGETABLE> context, double bonus, IDebugReporter debugReport) {
        return scoreEvaluator.score(context, bonus, debugReport);
    }

    public List<Consideration<IN_GAME_OBJECT, TARGETABLE>> getConsiderations() {
        return considerations;
    }

    public void addConsideration(Consideration<IN_GAME_OBJECT, TARGETABLE> consideration) {
        considerations.add(0, consideration);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ScoreType getScoreType() {
        return scoreType;
    }

    public void setScoreType(ScoreType scoreType) {
        this.scoreType = scoreType;
        this.scoreEvaluator = getScoreEvaluator(this.scoreType, this);
    }

    public abstract DecisionScoreEvaluator<IN_GAME_OBJECT, TARGETABLE> copy();

    @Override
    public String toString() {
        return this.name;
    }
}
