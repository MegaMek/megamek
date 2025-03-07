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
package megamek.client.bot.caspar;

import megamek.client.bot.common.Agent;
import megamek.client.bot.common.GameState;
import megamek.client.bot.princess.PathEnumerator;
import megamek.common.Entity;
import megamek.common.MovePath;

import java.util.List;

/**
 * Default implementation that uses the Princess PathEnumerator.
 */
public class DefaultPathfindingEngine implements PathfindingEngine {
    private final PathEnumerator pathEnumerator;

    public DefaultPathfindingEngine(Agent agent) {
        this.pathEnumerator = new PathEnumerator(agent);
    }

    @Override
    public List<MovePath> generatePaths(Entity unit, GameState gameState, double depth) {
        // Use Princess's PathEnumerator to generate paths
        pathEnumerator.recalculateMovesFor(unit);
        return pathEnumerator.getUnitPaths().getOrDefault(unit.getId(), List.of());
    }
}
