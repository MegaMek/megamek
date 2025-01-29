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
import java.util.Optional;
import java.util.TreeSet;

import megamek.client.bot.princess.RankedPath;

public interface DecisionMaker<IN_GAME_OBJECT, TARGETABLE, RANKED> {

    Optional<RANKED> pickOne(TreeSet<RankedPath> rankedDecisions);

    TreeSet<RANKED> scoreAllDecisions(List<Decision<IN_GAME_OBJECT, TARGETABLE>> decisions, List<DecisionContext<IN_GAME_OBJECT, TARGETABLE>> contexts);

    double getBonusFactor(Decision<IN_GAME_OBJECT, TARGETABLE> scoreEvaluator);
}
