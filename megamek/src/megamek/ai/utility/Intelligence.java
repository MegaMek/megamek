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

import megamek.client.bot.princess.RankedPath;
import megamek.common.MovePath;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public interface Intelligence<IN_GAME_OBJECT, TARGETABLE, RANKED> {

    void update(Intelligence<IN_GAME_OBJECT, TARGETABLE, RANKED> intelligence);
    List<Decision<IN_GAME_OBJECT, TARGETABLE>> getDecisions();
    DecisionMaker<IN_GAME_OBJECT, TARGETABLE, RANKED> getDecisionMaker();
    double getBonusFactor(IN_GAME_OBJECT inGameObject, MovePath movePath);
    Optional<RANKED> getPastRankedPath(IN_GAME_OBJECT inGameObject);
    default TreeSet<RANKED> scoreAllDecisions(List<DecisionContext<IN_GAME_OBJECT, TARGETABLE>> contexts) {
        return getDecisionMaker().scoreAllDecisions(getDecisions(), contexts);
    }
}
