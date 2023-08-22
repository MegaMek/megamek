/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * Camouflage is an implementation of AbstractIcon that contains and displays a Camouflage, which
 * may either be a base Camouflage from the Camouflage directory or a Colour camouflage, which is
 * based on the specified PlayerColour and then parsed as an AWT Color.
 * @see AbstractIcon
 */
public class Camouflage extends AbstractIcon {
    private static final long serialVersionUID = 1093277025745250375L;

    public static final String NO_CAMOUFLAGE = "-- No Camo --";
    public static final String COLOUR_CAMOUFLAGE = "-- Colour Camo --";
    public static final String XML_TAG = "camouflage";

    // Rotation and scaling are stored as integers to avoid the usual rounding problems with doubles (e.g.
    // when comparing camos with equals())
    /** The angle in degrees by which to rotate this camo when applying it to units. */
    private int rotationAngle = 0;
    /** The scale times 10 (10 = no scaling) to apply to this camo when applying it to units. */
    private int scale = 10;

    //region Constructors
    public Camouflage() {
        super(NO_CAMOUFLAGE);
    }

    public Camouflage(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }
    //endregion Constructors

    //region Boolean Methods
    public boolean isColourCamouflage() {
        return COLOUR_CAMOUFLAGE.equals(getCategory());
    }

    @Override
    public boolean hasDefaultCategory() {
        return super.hasDefaultCategory() || NO_CAMOUFLAGE.equals(getCategory());
    }
    //endregion Boolean Methods

    @Override
    public @Nullable Image getBaseImage() {
        if (MMStaticDirectoryManager.getCamouflage() == null) {
            return null;
        } else if (COLOUR_CAMOUFLAGE.equals(getCategory()) || NO_CAMOUFLAGE.equals(getCategory())) {
            return getColourCamouflageImage(PlayerColour.parseFromString(getFilename()).getColour());
        }

        final String category = ROOT_CATEGORY.equals(getCategory()) ? "" : getCategory();
        try {
            return (Image) MMStaticDirectoryManager.getCamouflage().getItem(category, getFilename());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        return null;
    }

    /**
     * @param colour the colour of the camouflage. This shouldn't be null, but null values are handled.
     * @return the created colour camouflage, or null if a null colour is provided
     */
    private @Nullable Image getColourCamouflageImage(final @Nullable Color colour) {
        if (colour == null) {
            LogManager.getLogger().error("A null colour was passed.");
            return null;
        }
        BufferedImage result = new BufferedImage(84, 72, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(colour);
        graphics.fillRect(0, 0, 84, 72);
        return result;
    }

    //region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static Camouflage parseFromXML(final Node wn) {
        final Camouflage icon = new Camouflage();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return new Camouflage();
        }
        return icon;
    }
    //endregion File I/O

    @Override
    public Camouflage clone() {
        Camouflage newCamo = new Camouflage(getCategory(), getFilename());
        newCamo.setRotationAngle(rotationAngle);
        newCamo.setScale(scale);
        return newCamo;
    }

    public void setRotationAngle(int rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    public int getRotationAngle() {
        return rotationAngle;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public void resetScale() {
        scale = 10;
    }

    public int getScale() {
        return scale;
    }

    public double getScaleFactor() {
        return scale / 10d;
    }

    public double getRotationRadians() {
        return rotationAngle * Math.PI / 180;
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other) && other instanceof Camouflage) {
            Camouflage otherCamo = (Camouflage) other;
            return (otherCamo.rotationAngle == rotationAngle) && (otherCamo.scale == scale);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilename(), getCategory(), rotationAngle, scale);
    }
}
