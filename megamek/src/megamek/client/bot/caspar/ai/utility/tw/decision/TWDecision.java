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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.Decision;
import megamek.ai.utility.DecisionScoreEvaluator;
import megamek.client.bot.princess.RankedPath;
import megamek.common.Entity;

@JsonTypeName("TWDecision")
public class TWDecision extends Decision<Entity, Entity> {

    @JsonIgnore
    private RankedPath rankedPath;

    public TWDecision() {
    }

    public TWDecision(String name, String description, double weight, DecisionScoreEvaluator<Entity, Entity> decisionScoreEvaluator) {
        super(name, description, weight, decisionScoreEvaluator);
    }

    public TWDecision(String name, String description, double weight) {
        this(name, description, weight, new TWDecisionScoreEvaluator());
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @JsonIgnore
    public RankedPath getRankedPath() {
        return rankedPath;
    }

    @Override
    public TWDecision copy() {
        return new TWDecision(getName(), getDescription(), getWeight(), getDecisionScoreEvaluator().copy());
    }

    @JsonIgnore
    private void setRankedPath(RankedPath rankedPath) {
        this.rankedPath = rankedPath;
    }
}
