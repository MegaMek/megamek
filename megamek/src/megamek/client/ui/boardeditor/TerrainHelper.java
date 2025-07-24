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
package megamek.client.ui.boardeditor;

import megamek.common.Terrains;

import java.util.Objects;

/**
 * Class to make terrains in JComboBoxes easier. This enables keeping the terrain type int separate from the name that
 * gets displayed and also provides a way to get tooltips.
 *
 * @author arlith
 */
class TerrainHelper implements Comparable<TerrainHelper> {

    private final int terrainType;

    TerrainHelper(int terrain) {
        terrainType = terrain;
    }

    public int getTerrainType() {
        return terrainType;
    }

    @Override
    public String toString() {
        return Terrains.getEditorName(terrainType);
    }

    public String getTerrainTooltip() {
        return Terrains.getEditorTooltip(terrainType);
    }

    @Override
    public int compareTo(TerrainHelper o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Integer) {
            return getTerrainType() == (Integer) other;
        } else if (!(other instanceof TerrainHelper)) {
            return false;
        } else {
            return getTerrainType() == ((TerrainHelper) other).getTerrainType();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTerrainType());
    }
}
