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

import java.awt.Component;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * ListCellRenderer for rendering tooltips for each item in a list or combobox. Code from SourceForge:
 * <a href="https://stackoverflow.com/questions/480261/java-swing-mouseover-text-on-jcombobox-items">...</a>
 */
class TerrainListRenderer extends DefaultListCellRenderer {

    private TerrainHelper[] terrains;
    private List<TerrainTypeHelper> terrainTypes;

    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
          final boolean isSelected, final boolean cellHasFocus) {

        if ((-1 < index) && (value != null)) {
            if (terrainTypes != null) {
                list.setToolTipText(terrainTypes.get(index).getTooltip());
            } else if (terrains != null) {
                list.setToolTipText(terrains[index].getTerrainTooltip());
            }
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }

    public void setTerrains(TerrainHelper... terrains) {
        this.terrains = terrains;
    }

    public void setTerrainTypes(List<TerrainTypeHelper> terrainTypes) {
        this.terrainTypes = terrainTypes;
    }
}
