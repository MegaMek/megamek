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
import megamek.client.ui.swing.util.PlayerColors;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Camouflage extends AbstractIcon {
    //region Variable Declarations
    private static final long serialVersionUID = 1093277025745250375L;

    public static final String NO_CAMOUFLAGE = "-- No Camo --";
    //endregion Variable Declarations

    //region Constructors
    public Camouflage() {
        super();
    }

    public Camouflage(String category, String filename) {
        super(category, filename);
    }
    //endregion Constructors

    @Override
    public Image getBaseImage() {
        if (MMStaticDirectoryManager.getCamouflage() == null) {
            return null;
        } else if (Camouflage.NO_CAMOUFLAGE.equals(getCategory())) {
            return getColourCamouflageImage(PlayerColors.getColor(getFilename()));
        }

        final String category = AbstractIcon.ROOT_CATEGORY.equals(getCategory()) ? "" : getCategory();
        Image camouflage = null;

        try {
            camouflage = (Image) MMStaticDirectoryManager.getCamouflage().getItem(category, getFilename());
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        }

        return camouflage;
    }

    private Image getColourCamouflageImage(Color colour) {
        if (colour == null) {
            MegaMek.getLogger().error("A null colour was passed.");
            return null;
        }
        BufferedImage result = new BufferedImage(84, 72, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(colour);
        graphics.fillRect(0, 0, 84, 72);
        return result;
    }
}
