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

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.ProtoMek;
import megamek.common.units.Terrains;

/**
 * Whether going down one level takes a unit from a building's roof into the building. Only infantry, battle armour
 * included, and ProtoMeks may change levels in a building hex (TW p. 169), so for them the step down from the roof is
 * entering the building, and the movement display names it that way rather than "Go Down".
 */
public final class BuildingRoofEntry {

    private BuildingRoofEntry() {}

    /**
     * Returns {@code true} if the unit stands on the roof of a building at the given place and may go down into it.
     *
     * @param entity    the moving unit
     * @param elevation the unit's elevation at the end of its movement so far
     * @param coords    the hex it is in at the end of its movement so far, or {@code null} if it is off the board
     * @param boardId   the board that hex is on
     *
     * @return {@code true} if going down enters the building from its roof
     */
    public static boolean isEnteringFromRoof(Entity entity, int elevation, @Nullable Coords coords, int boardId) {
        boolean canChangeLevelsInBuildings = (entity instanceof Infantry) || (entity instanceof ProtoMek);
        if (!canChangeLevelsInBuildings) {
            return false;
        }
        Game game = entity.getGame();
        if ((coords == null) || (game == null)) {
            return false;
        }
        if (!game.hasBoardLocation(coords, boardId)) {
            return false;
        }
        Hex hex = game.getBoard(boardId).getHex(coords);
        boolean isBuildingHex = (hex != null) && hex.containsTerrain(Terrains.BLDG_ELEV);
        if (!isBuildingHex) {
            return false;
        }
        boolean isOnTheRoof = elevation == hex.terrainLevel(Terrains.BLDG_ELEV);
        return isOnTheRoof && entity.canGoDown(elevation, coords, boardId);
    }
}
