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

import java.util.List;

public class Profile {

    private final String name;
    private String description;
    private final List<DecisionScoreEvaluator> decisionScoreEvaluators;

    public Profile(String name, String description, List<DecisionScoreEvaluator> decisionScoreEvaluators) {
        this.name = name;
        this.description = description;
        this.decisionScoreEvaluators = decisionScoreEvaluators;
    }

    public String getName() {
        return name;
    }

    public List<DecisionScoreEvaluator> getDecisionScoreEvaluators() {
        return decisionScoreEvaluators;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
