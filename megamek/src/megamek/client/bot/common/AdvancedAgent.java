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

import megamek.client.bot.caspar.TacticalPlanner;
import megamek.client.bot.common.formation.Formation;
import megamek.common.Entity;

import java.util.Optional;

public interface AdvancedAgent extends Agent {


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

    BoardQuickRepresentation getBoardQuickRepresentation();

    Optional<Formation> getFormationFor(Entity unit);

    TacticalPlanner getTacticalPlanner();
}
