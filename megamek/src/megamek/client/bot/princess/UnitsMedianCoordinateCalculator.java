/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

import java.util.List;


/**
 * Calculates the median coordinate of enemy units for tactical decision-making.
 * <p>
 * This utility class provides methods to determine the central position of nearby enemy units. It's primarily used
 * by bot AI to determine which direction to face and prioritize threats.
 * <p>
 * The calculation:
 * <ul>
 *   <li>Filters out enemies without valid positions</li>
 *   <li>Sorts enemies by distance, with adjustments for units that haven't moved</li>
 *   <li>Considers only the closest enemies (limited to a small tactical group)</li>
 *   <li>Calculates the geometric median of these positions</li>
 * </ul>
 *
 * @author Luana Coppio
 * @since 0.50.06
 */
class UnitsMedianCoordinateCalculator {

    private final int numberOfUnitToConsider;

    UnitsMedianCoordinateCalculator(int numberOfUnitToConsider) {
        this.numberOfUnitToConsider = numberOfUnitToConsider;
    }

    /**
     * Calculates the median coordinate of the closest enemy units relative to a position.
     * <p>
     * This method finds the closest enemies to the specified position, with special handling for units that haven't
     * moved yet. Units that can still move are considered to be farther away by their walk MP, anticipating that
     * they might move away from the current position.
     * <p>
     * The calculation only considers the top N closest enemy units to focus on immediate threats rather than the
     * entire battlefield, where N is defined when initializing this calculator.
     *
     * @param enemies  List of enemy entities to evaluate
     * @param position Reference position to calculate distances from
     * @return The median coordinate of the closest enemies, representing the central threat position. Returns null
     * if there are no enemies.
     */
    @Nullable Coords getEnemiesMedianCoordinate(List<Entity> enemies, Coords position) {
        List<Coords> coords = enemies.stream().filter(e -> e.getPosition() != null).sorted((e1, e2) -> {
            // Consider that those who have not moved will move away from me
            boolean hasNotMoved1 = e1.isSelectableThisTurn() && !e1.isImmobile();
            boolean hasNotMoved2 = e2.isSelectableThisTurn() && !e2.isImmobile();
            double bonusDistance1 = hasNotMoved1 ? e1.getWalkMP() : 0;
            double bonusDistance2 = hasNotMoved2 ? e2.getWalkMP() : 0;
            double dist1 = Math.max(0, e1.getPosition().distance(position) + bonusDistance1);
            double dist2 = Math.max(0, e2.getPosition().distance(position) + bonusDistance2);
            return Double.compare(dist1, dist2);
        }).limit(this.numberOfUnitToConsider).map(Entity::getPosition).toList();

        return Coords.median(coords);
    }
}
