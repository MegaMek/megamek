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

import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * An AbstractIcon is an abstract class that ensures standardized and isolated image code for icons
 * across the suite. It handles creating the images and image icons, scaling and centering them,
 * handles base comparisons and object overrides, and implements basic File I/O. It also gracefully
 * handles any failures to prevent a hard crash when an image or icon cannot be created.
 */
public abstract class AbstractIcon implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 870271199001476289L;

    public static final String ROOT_CATEGORY = "-- General --";
    public static final String DEFAULT_ICON_FILENAME = "None";

    private String category;
    protected String filename;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractIcon() {
        this(ROOT_CATEGORY, DEFAULT_ICON_FILENAME);
    }

    protected AbstractIcon(final @Nullable String category) {
        this(category, DEFAULT_ICON_FILENAME);
    }

    protected AbstractIcon(final @Nullable String category, final @Nullable String filename) {
        setCategory(category);
        setFilename(filename);
    }
    //endregion Constructors

    //region Getters/Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(final @Nullable String category) {
        this.category = (category == null) ? ROOT_CATEGORY : category;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final @Nullable String filename) {
        this.filename = (filename == null) ? DEFAULT_ICON_FILENAME : filename;
    }
    //endregion Getters/Setters

    //region Boolean Methods
    public boolean isDefault() {
        return hasDefaultCategory() && hasDefaultFilename();
    }

    public boolean hasDefaultCategory() {
        return ROOT_CATEGORY.equals(getCategory()) || getCategory().isBlank();
    }

    public boolean hasDefaultFilename() {
        return DEFAULT_ICON_FILENAME.equals(getFilename()) || getFilename().isBlank();
    }
    //endregion Boolean Methods

    /**
     * This is used to determine whether the created image should be scaled or not by checking the
     * Height and Width values. If either is -1, then we need to scale the produced image
     * @return whether to scale the image or not
     */
    protected boolean isScaled(final int width, final int height) {
        return (width == -1) || (height == -1);
    }

    /**
     * @return the ImageIcon for the Image stored by the AbstractIcon. May be null for non-existent
     * files
     */
    public @Nullable ImageIcon getImageIcon() {
        final Image image = getImage();
        return (image == null) ? null : new ImageIcon(image);
    }

    /**
     * @param size the width to scale and center the ImageIcon to
     * @return the created ImageIcon, scaled and centered to size width. This may be null for
     * non-existent files
     */
    public @Nullable ImageIcon getImageIcon(final int size) {
        final Image image = getImage(size);
        return (image == null) ? null : new ImageIcon(image);
    }

    /**
     * @return the created image, which may only be null for a UnitIcon with no icon selected
     */
    public @Nullable Image getImage() {
        return getImage(0, 0);
    }

    /**
     * @param size the width to scale and center the image to
     * @return the created image, scaled and centered to size width. This may only be null for a
     * UnitIcon with no icon selected
     */
    public @Nullable Image getImage(final int size) {
        return getImage(size, -1);
    }

    /**
     * @param width the width of the image, or -1 if this is the non-scaling factor
     * @param height the height of the image, or -1 if this is the non-scaling factor
     * @return the image, scaled and centered if either the width or height are -1. This may only be
     * null for a UnitIcon with no icon selected
     */
    public @Nullable Image getImage(final int width, final int height) {
        return getImage(getBaseImage(), width, height);
    }

    /**
     * This is used to create the proper image and scale it if required. It also handles null
     * protection by creating a blank image if required.
     * @return the created image
     */
    protected Image getImage(final @Nullable Image image, final int width, final int height) {
        if (image == null) {
            return ImageUtil.failStandardImage();
        } else if (isScaled(width, height)) {
            return scaleAndCenter(image, (width != -1) ? width : height);
        } else {
            return image;
        }
    }

    /**
     * @return a square BufferedImage of the given size. Scales the given image to fit into the
     * square and centers it on a transparent background.
     */
    public static BufferedImage scaleAndCenter(Image image, final int size) {
        final BufferedImage result = ImageUtil.createAcceleratedImage(size, size);
        final Graphics graphics = result.getGraphics();
        if (image.getWidth(null) > image.getHeight(null)) {
            image = image.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
            graphics.drawImage(image, 0, (size - image.getHeight(null)) / 2, null);
        } else {
            image = image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
            graphics.drawImage(image, (size - image.getWidth(null)) / 2, 0, null);
        }
        return result;
    }

    /**
     * This is abstract to allow for different formats for determining the image in question
     * @return the Image stored by the AbstractIcon
     */
    public abstract Image getBaseImage();

    //region File I/O
    public abstract void writeToXML(final PrintWriter pw, final int indent);

    protected void writeToXML(final PrintWriter pw, int indent, final String name) {
        if (isDefault()) {
            return;
        }

        MMXMLUtility.writeSimpleXMLOpenTag(pw, indent++, name);
        writeBodyToXML(pw, indent);
        MMXMLUtility.writeSimpleXMLCloseTag(pw, --indent, name);
    }

    protected void writeBodyToXML(final PrintWriter pw, int indent) {
        if (!hasDefaultCategory()) {
            MMXMLUtility.writeSimpleXMLTag(pw, indent, "category", getCategory());
        }

        if (!hasDefaultFilename()) {
            MMXMLUtility.writeSimpleXMLTag(pw, indent, "filename", getFilename());
        }
    }

    public void parseNodes(final NodeList nl) {
        for (int x = 0; x < nl.getLength(); x++) {
            parseNode(nl.item(x));
        }
    }

    protected void parseNode(final Node wn) {
        switch (wn.getNodeName()) {
            case "category":
                setCategory(MMXMLUtility.unEscape(wn.getTextContent().trim()));
                break;
            case "filename":
                setFilename(MMXMLUtility.unEscape(wn.getTextContent().trim()));
                break;
            default:
                break;
        }
    }
    //endregion File I/O

    @Override
    public String toString() {
        return "[Camouflage] Category: " + category + "; Filename: " + filename;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof AbstractIcon) {
            final AbstractIcon dOther = (AbstractIcon) other;
            return dOther.getCategory().equals(getCategory()) && dOther.getFilename().equals(getFilename());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (getCategory() + getFilename()).hashCode();
    }
}
