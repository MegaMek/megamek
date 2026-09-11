/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.moves;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.MobileStructure;

/** Local ground support, including the one-third-width sublevel exception (TO:AUE p.33). */
public final class MobileStructureSupport {
    private MobileStructureSupport() { }

    public static int level(Game game, MobileStructure unit, List<Coords> footprint, Coords coords) {
        int ground = MobileStructureMovement.terrain(game, unit, coords).floor();
        int supported = ground;
        for (int axis = 0; axis < 3; axis++) {
            List<Coords> line = new ArrayList<>();
            line.add(coords);
            for (int direction : new int[] {axis, axis + 3}) {
                for (Coords next = coords.translated(direction); footprint.contains(next); next = next.translated(direction)) {
                    line.add(next);
                }
            }
            if (line.size() < 3) {
                continue;
            }
            for (Coords support : line) {
                int candidate = MobileStructureMovement.terrain(game, unit, support).floor();
                if (candidate <= supported) {
                    continue;
                }
                int span = 1;
                for (int direction : new int[] {axis, axis + 3}) {
                    for (Coords next = coords.translated(direction); footprint.contains(next)
                          && MobileStructureMovement.terrain(game, unit, next).floor() < candidate;
                          next = next.translated(direction)) {
                        span++;
                    }
                }
                if (span * 3 <= line.size()) {
                    supported = candidate;
                }
            }
        }
        return supported;
    }
}
