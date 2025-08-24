/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.pathfinder.comparators;

import java.util.Comparator;
import java.util.Objects;

import megamek.common.board.Coords;
import megamek.common.moves.MovePath;

/**
 * Compares MovePaths based on distance from final position to destination. If those distances are equal then spent
 * movement points are compared.
 */
public record MovePathGreedyComparator(Coords destination) implements Comparator<MovePath> {
    public MovePathGreedyComparator(Coords destination) {
        this.destination = Objects.requireNonNull(destination);
    }

    /**
     * Compares MovePaths based on distance from final position to destination. If those distances are equal then spent
     * movement points are compared.
     */
    @Override
    public int compare(MovePath mp1, MovePath mp2) {
        int d1 = mp1.getFinalCoords().distance(destination);
        int d2 = mp2.getFinalCoords().distance(destination);
        if (d1 != d2) {
            return d1 - d2;
        } else {
            return mp1.getMpUsed() - mp2.getMpUsed();
        }
    }
}
