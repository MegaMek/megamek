/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.icons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.Objects;

import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.util.PlayerColour;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import org.w3c.dom.Node;

/**
 * Camouflage is an implementation of AbstractIcon that contains and displays a Camouflage, which may either be a base
 * Camouflage from the Camouflage directory or a Colour camouflage, which is based on the specified PlayerColour and
 * then parsed as an AWT Color.
 *
 * @see AbstractIcon
 */
public class Camouflage extends AbstractIcon {
    private static final MMLogger LOGGER = MMLogger.create(Camouflage.class);

    @Serial
    private static final long serialVersionUID = 1093277025745250375L;

    public static final String NO_CAMOUFLAGE = "-- No Camo --";
    public static final String COLOUR_CAMOUFLAGE = "-- Colour Camo --";
    public static final String XML_TAG = "camouflage";

    // Rotation and scaling are stored as integers to avoid the usual rounding
    // problems with doubles (e.g.
    // when comparing camos with equals())
    /**
     * The angle in degrees by which to rotate this camo when applying it to units.
     */
    protected int rotationAngle = 0;
    /**
     * The scale times 10 (10 = no scaling) to apply to this camo when applying it to units.
     */
    protected int scale = 10;

    // region Constructors
    public Camouflage() {
        super(NO_CAMOUFLAGE);
    }

    /**
     * Constructs a new camo of the "category" (directory, ending with "/") and filename. Can only be used for camos of
     * the directories that are parsed automatically, i.e. the MM-internal camo dir, the user dir and the story arcs
     * directory.
     *
     * @param category the directory, e.g. "Clans/Wolf/Alpha Galaxy/"
     * @param filename the filename, e.g. "Alpha Galaxy.jpg"
     */
    public Camouflage(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }

    /**
     * Constructs a new camo with the given file. Even though a file is accepted, this can only be used for camos of the
     * directories that are parsed automatically, i.e. the MM-internal camo dir, the user dir and the story arcs
     * directory! This method tries to parse the filename to find the camo. This requires replacing Windows backslashes
     * with normal slashes in order to find the file in the way camos are stored (see
     * {@link megamek.common.util.fileUtils.AbstractDirectory})
     *
     * @param file The File, such as a file of "Clans/Wolf/Alpha Galaxy/Alpha Galaxy.jpg"
     */
    public Camouflage(File file) {
        this(getDirectory(file), file.getName());
    }

    /**
     * Returns a new camo of the given PlayerColour.
     *
     * @param color A PlayerColour
     *
     * @return A camo of the given PlayerColour color
     */
    public static Camouflage of(PlayerColour color) {
        return new Camouflage(COLOUR_CAMOUFLAGE, color.name());
    }
    // endregion Constructors

    // region Boolean Methods
    public boolean isColourCamouflage() {
        return COLOUR_CAMOUFLAGE.equals(getCategory());
    }

    @Override
    public boolean hasDefaultCategory() {
        return super.hasDefaultCategory() || NO_CAMOUFLAGE.equals(getCategory());
    }
    // endregion Boolean Methods

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
            LOGGER.error("", ex);
        }

        return null;
    }

    /**
     * @param colour the colour of the camouflage. This shouldn't be null, but null values are handled.
     *
     * @return the created colour camouflage, or null if a null colour is provided
     */
    private @Nullable Image getColourCamouflageImage(final @Nullable Color colour) {
        if (colour == null) {
            LOGGER.error("A null colour was passed.");
            return null;
        }
        BufferedImage result = new BufferedImage(84, 72, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(colour);
        graphics.fillRect(0, 0, 84, 72);
        return result;
    }

    // region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static Camouflage parseFromXML(final Node wn) {
        final Camouflage icon = new Camouflage();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            LOGGER.error("", ex);
            return new Camouflage();
        }
        return icon;
    }
    // endregion File I/O

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

    /**
     * Set the camo scaling to the given scale value. Use for serialization etc.
     *
     * @param scale The scale factor times 10
     */
    public void setScale(int scale) {
        this.scale = scale;
    }

    /** Resets the camo scaling to the neutral value (no scaling). */
    public void resetScale() {
        scale = 10;
    }

    /**
     * @return The camo scaling; this value is 10 times the scaling that is used, so a return value of 10 means no
     *       scaling is applied. Use for serialization etc. Use {@link #getScaleFactor()} to get the value by which to
     *       actually scale the camo.
     */
    public int getScale() {
        return scale;
    }

    /** @return The scaling to apply to the camo. A value of 1 means no scaling. */
    public double getScaleFactor() {
        return scale / 10d;
    }

    /** @return The camo rotation in radians. */
    public double getRotationRadians() {
        return rotationAngle * Math.PI / 180;
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other) && other instanceof Camouflage otherCamo) {
            return (otherCamo.rotationAngle == rotationAngle) && (otherCamo.scale == scale);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilename(), getCategory(), rotationAngle, scale);
    }

    public static String getDirectory(File file) {
        String result = file.getParent().replace("\\", "/");
        return result + (!result.endsWith("/") ? "/" : "");
    }
}
