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

package megamek.client.bot.queen.ai.utility.tw.intelligence;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.RankedPath;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;

import java.util.List;
import java.util.Optional;

public interface PathRankerUtilCalculator {
    SimpleIntelligence.FiringPhysicalDamage damageCalculator(MovePath path, List<Entity> enemies);

    double getMovePathSuccessProbability(MovePath path, StringBuilder report);

    int distanceToHomeEdge(Coords position, CardinalEdge homeEdge, Game game);

    Optional<RankedPath> previousRankedPath(Entity entity);
}
