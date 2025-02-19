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

public class ConcreteDecisionScoreEvaluator extends DecisionScoreEvaluator {

    private final List<Consideration> considerations;

    public ConcreteDecisionScoreEvaluator(List<Consideration> considerations) {
        this.considerations = considerations;
    }

    @Override
    public List<Consideration> getConsiderations() {
        return considerations;
    }

    @Override
    public DecisionScoreEvaluator copy() {
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }
}
