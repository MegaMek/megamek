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
package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.common.Coords;
import megamek.common.Entity;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Calculates the environmental hazards around the final position
 * @author Luana Coppio
 */
public class EnvironmentalHazardsCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the potential of the unit to act as a decoy
        float[] hazards = axis();
        Entity unit = pathing.getEntity();
        var boardQuickRepresentation = gameState.getBoardQuickRepresentation();
        if (!boardQuickRepresentation.onGround() || unit.isAirborneAeroOnGroundMap()) {
            return hazards;
        }

        Set<Coords> path = pathing.getCoordsSet();
        AtomicInteger hazardCounter = new AtomicInteger();
        path.forEach(coords -> {
            if (boardQuickRepresentation.hasHazard(coords)) {
                hazardCounter.getAndIncrement();
            }
        });

        boolean hasHazard = pathing.getFinalCoords().allAdjacent().stream().anyMatch(boardQuickRepresentation::hasHazard);

        if (hasHazard) {
            hazardCounter.getAndIncrement();
        }

        hazards[0] = pathing.isJumping() ? hazardCounter.get() * 0.5f : hazardCounter.get();

        return hazards;
    }
}
