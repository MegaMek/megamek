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

import megamek.common.Terrain;
import megamek.common.Terrains;

/**
 * Class to make it easier to display a <code>Terrain</code> in a JList or JComboBox.
 *
 * @author arlith
 */
class TerrainTypeHelper implements Comparable<TerrainTypeHelper> {

    Terrain terrain;

    TerrainTypeHelper(Terrain terrain) {
        this.terrain = terrain;
    }

    Terrain getTerrain() {
        return terrain;
    }

    @Override
    public String toString() {
        String baseString = Terrains.getDisplayName(terrain.getType(), terrain.getLevel());
        if (baseString == null) {
            baseString = Terrains.getEditorName(terrain.getType()) + " " + terrain.getLevel();
        }
        if (terrain.hasExitsSpecified()) {
            baseString += " (Exits: %d)".formatted(terrain.getExits());
        }
        return baseString;
    }

    String getTooltip() {
        return terrain.toString();
    }

    @Override
    public int compareTo(TerrainTypeHelper o) {
        return toString().compareTo(o.toString());
    }
}
