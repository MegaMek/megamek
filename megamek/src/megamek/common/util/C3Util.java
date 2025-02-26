/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.util;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Entity;
import megamek.common.Game;

public class C3Util {
    /**
     * Adds C3 connections when new units are being added.
     * 
     * @param game   The Game the unit is being added to, the unit should already be
     *               in the Game.
     * @param entity The entity being added.
     * @return A list of units affected
     */
    public static List<Entity> wireC3(Game game, Entity entity) {
        ArrayList<Entity> affectedUnits = new ArrayList<>();
        if (!entity.hasC3() && !entity.hasC3i() && !entity.hasNavalC3()) {
            return affectedUnits;
        }

        boolean C3iSet = false;

        for (Entity e : game.getEntitiesVector()) {

            // C3 Checks
            if (entity.hasC3()) {
                if ((entity.getC3MasterIsUUIDAsString() != null)
                        && entity.getC3MasterIsUUIDAsString().equals(e.getC3UUIDAsString())) {
                    entity.setC3Master(e, false);
                    entity.setC3MasterIsUUIDAsString(null);
                } else if ((e.getC3MasterIsUUIDAsString() != null)
                        && e.getC3MasterIsUUIDAsString().equals(entity.getC3UUIDAsString())) {
                    e.setC3Master(entity, false);
                    e.setC3MasterIsUUIDAsString(null);

                    affectedUnits.add(e);
                }
            }

            // C3i Checks
            if (entity.hasC3i() && !C3iSet) {
                entity.setC3NetIdSelf();
                int pos = 0;
                while (pos < Entity.MAX_C3i_NODES) {
                    // We've found a network, join it.
                    if ((entity.getC3iNextUUIDAsString(pos) != null)
                            && (e.getC3UUIDAsString() != null)
                            && entity.getC3iNextUUIDAsString(pos)
                                    .equals(e.getC3UUIDAsString())) {
                        entity.setC3NetId(e);
                        C3iSet = true;
                        break;
                    }

                    pos++;
                }
            }

            // NC3 Checks
            if (entity.hasNavalC3() && !C3iSet) {
                entity.setC3NetIdSelf();
                int pos = 0;
                while (pos < Entity.MAX_C3i_NODES) {
                    // We've found a network, join it.
                    if ((entity.getNC3NextUUIDAsString(pos) != null)
                            && (e.getC3UUIDAsString() != null)
                            && entity.getNC3NextUUIDAsString(pos)
                                    .equals(e.getC3UUIDAsString())) {
                        entity.setC3NetId(e);
                        C3iSet = true;
                        break;
                    }

                    pos++;
                }
            }
        }

        return affectedUnits;
    }
}
