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

package megamek.client.ui.dialogs.unitEditor;

import megamek.client.ui.Messages;
import megamek.common.board.Coords;
import megamek.common.units.AbstractBuildingEntity;

/**
 * Names an Advanced Building's hexes for the damage editor.
 *
 * <p>A building's own location names carry its relative cube coordinates, because the unit file is keyed by them,
 * and those are not something a player should have to read. In the editor a hex is named by its number in the
 * building's declaration order - {@code Hex 3} - and, once the building is on the board, by the board hex it stands
 * in as well - {@code Hex 3 (0304)} - so that either can be typed to find it.</p>
 */
final class BuildingHexNames {

    private BuildingHexNames() {}

    /**
     * @param building the building whose hex is being named
     * @param hexIndex the hex, counting from {@code 0} in declaration order
     *
     * @return the hex's display name, numbered from {@code 1}, with its board hex once the building is placed
     */
    static String hexName(AbstractBuildingEntity building, int hexIndex) {
        int hexNumber = hexIndex + 1;
        int groundFloorLocation = hexIndex * building.getInternalBuilding().getBuildingHeight();
        Coords boardHex = building.getLocationCoords(groundFloorLocation);
        if (boardHex == null) {
            return Messages.getString("UnitEditorDialog.building.hex", hexNumber);
        }
        return Messages.getString("UnitEditorDialog.building.hexAt", hexNumber, boardHex.getBoardNum());
    }
}
