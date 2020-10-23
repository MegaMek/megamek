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

import javax.swing.*;
import java.awt.*;

public class Portrait extends AbstractIcon {
    //region Variable Declarations
    private static final long serialVersionUID = -7562297705213174435L;
    public static final String DEFAULT_PORTRAIT_FILENAME = "default.gif";
    //endregion Variable Declarations

    //region Constructors
    public Portrait() {
        super();
    }

    public Portrait(String category, String filename) {
        super(category, filename);
    }
    //endregion Constructors

    //region Boolean Methods
    @Override
    public boolean hasDefaultFilename() {
        return super.hasDefaultFilename() || DEFAULT_PORTRAIT_FILENAME.equals(getFilename());
    }
    //endregion Boolean Methods

    @Override
    public ImageIcon getImageIcon() {
        return getImageIcon(72);
    }

    @Override
    public Image getImage() {
        return getImage(72);
    }

    @Override
    public Image getBaseImage() {
        // If we can't create the portrait directory,
        if (MMStaticDirectoryManager.getPortraits() == null) {
            return null;
        }

        String category = (hasDefaultCategory() || (getCategory() == null)) ? "" : getCategory();
        String filename = (hasDefaultFilename() || (getFilename() == null))
                ? DEFAULT_PORTRAIT_FILENAME : getFilename();

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
