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

package megamek.common.pathfinder;

import java.util.Comparator;

import megamek.common.moves.MovePath;
import megamek.common.units.Tank;

/**
 * Relaxes edge by favouring MovePaths that end in a not prone stance.
 */
public class MovePathRelaxer implements EdgeRelaxer<MovePath, MovePath> {
    @Override
    public MovePath doRelax(MovePath v, MovePath e, Comparator<MovePath> comparator) {
        if (v == null) {
            return e;
        }

        // We have to be standing to be able to move
        // Maybe I should replace this extra condition with a flag in node(?)
        boolean vprone = v.getFinalProne(), eprone = e.getFinalProne();
        if (vprone != eprone) {
            return vprone ? e : null;
        }
        if (!(v.getEntity() instanceof Tank)) {
            boolean vhdown = v.getFinalHullDown(), ehdown = e.getFinalHullDown();
            if (vhdown != ehdown) {
                return vhdown ? e : null;
            }
        }

        return comparator.compare(e, v) < 0 ? e : null;
    }
}
