/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */

package megamek.client.bot.common;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.FireControlState;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.pathfinder.BoardClusterTracker;

import java.util.List;
import java.util.Set;

public interface Agent {
    Coords getWaypointForEntity(Entity mover);

    Player getLocalPlayer();

    UnitBehavior.BehaviorType getBehaviorType(Entity mover);
    // get homeEdge
    Set<Coords> getDestinationCoords(Entity mover);
    // get homeEdge
    Set<Coords> getDestinationCoordsWithTerrainReduction(Entity mover);
    // CardinalEdge oppositeEdge = BoardUtilities.determineOppositeEdge(mover);
    Set<Coords> getOppositeSideDestinationCoordsWithTerrainReduction(Entity mover);

    /**
     * Returns the edge of the map that the unit should move towards to reach its home edge.
     * @param unit The unit to move
     * @return The edge of the map that the unit should move towards
     */
    CardinalEdge getHomeEdge(Entity unit);

    List<Coords> getEnemyHotSpots();

    FireControlState getFireControlState();

    BoardClusterTracker getClusterTracker();

    Game getGame();

    List<Entity> getEnemyEntities();

    List<Entity> getEntitiesOwned();

    List<Entity> getFriendEntities();
}
