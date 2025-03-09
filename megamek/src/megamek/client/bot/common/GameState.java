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

import megamek.client.bot.common.formation.Formation;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.pathfinder.BoardClusterTracker;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for the game state.
 * @author Luana Coppio
 */
public interface GameState {

    /**
     * Returns the enemy units that are available to the agent at this moment in time.
     * @return A list of enemy units
     */
    List<Entity> getEnemyUnits();

    /**
     * Returns the unexplored areas of the map. This has the coordinates that were not yet seen or reached by the
     * agent units
     * @return A list of coordinates
     */
    Set<Coords> getUnexploredAreas();

    /**
     * Returns the friendly units that are available to the agent at this moment in time. This does include own units.
     * @return A list of friendly units including the agent own units.
     */
    List<Entity> getFriendlyUnits();

    /**
     * Returns the units owned by this current player.
     * @return A list of owned units
     */
    List<Entity> getOwnedUnits();

    /**
     * Returns the units owned and friendly to this current player.
     * @return A list of owned units that are not in the agent's team
     */
    List<Entity> getMyTeamUnits();

    /**
     * Get the StructOfUnitArrays for the enemy units, this is a special data structure that allows for quick access
     * to units and their positional data for fast processing.
     * @return The StructOfUnitArrays for the enemy units
     */
    StructOfUnitArrays getEnemyUnitsSOU();

    /**
     * Get the StructOfUnitArrays for the friendly units, this is a special data structure that allows for quick access
     * to units and their positional data for fast processing.
     * @return The StructOfUnitArrays for the friendly units
     */
    StructOfUnitArrays getFriendlyUnitsSOU();

    /**
     * Get the StructOfUnitArrays for the own units, this is a special data structure that allows for quick access
     * to units and their positional data for fast processing.
     * @return The StructOfUnitArrays for the own units
     */
    StructOfUnitArrays getOwnUnitsSOU();

    /**
     * Returns the strategic points of the map. Strategic points are hexes that are important to the agent, like a
     * building, a turret, elevator, etc.
     * @return A list of hexes
     */
    List<Hex> getStrategicPoints();

    // Basic game information
    Game getGame();
    BoardQuickRepresentation getBoardQuickRepresentation();
    Player getLocalPlayer();

    // Pathfinding support
    BoardClusterTracker getClusterTracker();

    // Formation support
    Optional<Formation> getFormationFor(Entity unit);
}
