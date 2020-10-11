/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.icons;

import megamek.MegaMek;
import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.Crew;

import java.awt.*;

public class Portrait extends AbstractIcon {
    private static final long serialVersionUID = -7562297705213174435L;
    public static final String DEFAULT_PORTRAIT_FILENAME = "default.gif";

    @Override
    public Image getBaseImage() {
        String category = (Crew.ROOT_PORTRAIT.equals(getCategory())) ? "" : getCategory();
        String filename = getFileName();

        // Return the default image if the player has selected no portrait file.
        if ((category == null) || (filename == null) || Crew.PORTRAIT_NONE.equals(filename)) {
            filename = DEFAULT_PORTRAIT_FILENAME;
        }

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) MMStaticDirectoryManager.getPortraits().getItem(category, filename);
            if (portrait == null) {
                portrait = (Image) MMStaticDirectoryManager.getPortraits().getItem("", DEFAULT_PORTRAIT_FILENAME);
            }
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        }

        return portrait;
    }
}
