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

package megamek.client.bot.caspar.ai.utility.tw.decision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.*;
import megamek.common.Entity;

import java.util.List;

@JsonTypeName("TWDecisionScoreEvaluator")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TWDecisionScoreEvaluator extends DecisionScoreEvaluator<Entity, Entity> {

    private ScoreEvaluator<Entity, Entity> scoreEvaluator;

    public TWDecisionScoreEvaluator() {
    }

    public TWDecisionScoreEvaluator(String name, String description, String notes, ScoreType scoreType) {
        super(name, description, notes, scoreType);
    }

    public TWDecisionScoreEvaluator(String name, String description, String notes, ScoreType scoreType, List<Consideration<Entity, Entity>> considerations) {
        super(name, description, notes, scoreType, considerations);
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context, double bonus, IDebugReporter debugReport) {
        return scoreEvaluator.score(context, bonus, debugReport);
    }

    @Override
    public DecisionScoreEvaluator<Entity, Entity> copy() {
        return new TWDecisionScoreEvaluator(getName(), getDescription(), getNotes(), getScoreType(), getConsiderations().stream().map(Consideration::copy).toList());
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
