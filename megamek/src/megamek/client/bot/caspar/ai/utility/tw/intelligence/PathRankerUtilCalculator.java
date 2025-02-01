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

package megamek.client.bot.caspar.ai.utility.tw.intelligence;

import megamek.client.bot.princess.*;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;

import java.util.List;

public interface PathRankerUtilCalculator {
    FiringPhysicalDamage damageCalculator(MovePath path, List<Entity> enemies);

    double getMovePathSuccessProbability(MovePath path);

    int distanceToHomeEdge(Coords position, CardinalEdge homeEdge, Game game);

    double calculateMovePathPSRDamage(Entity movingUnit, MovePath pathCopy);

    double checkPathForHazards(MovePath path, Entity movingUnit, Game game);

    FiringPhysicalDamage calcDamageToStrategicTargets(MovePath path, Game game, FireControlState fireControlState, FiringPhysicalDamage damageStructure);

    boolean evaluateAsMoved(Entity enemy);

    EntityEvaluationResponse evaluateMovedEnemy(Entity enemy, MovePath path, Game game);

    EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, boolean extremeRange, boolean losRange);
}
