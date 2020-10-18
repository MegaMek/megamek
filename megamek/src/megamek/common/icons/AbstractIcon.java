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
import megamek.common.util.ImageUtil;
import megamek.utils.MegaMekXmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.Serializable;

public abstract class AbstractIcon implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 870271199001476289L;

    public static final String ROOT_CATEGORY = "-- General --";
    public static final String DEFAULT_ICON_FILENAME = "None";
    public static final int DEFAULT_IMAGE_SCALE = 75;

    private String category;
    private String filename;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractIcon() {
        this(ROOT_CATEGORY, DEFAULT_ICON_FILENAME);
    }

    protected AbstractIcon(String category, String filename) {
        setCategory(category);
        setFilename(filename);
    }
    //endregion Constructors

    //region Getters/Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    //endregion Getters/Setters

    //region Boolean Methods
    public boolean isDefault() {
        return hasDefaultCategory() && hasDefaultFilename();
    }

    public boolean hasDefaultCategory() {
        return ROOT_CATEGORY.equals(getCategory());
    }

    public boolean hasDefaultFilename() {
        return DEFAULT_ICON_FILENAME.equals(getFilename());
    }
    //endregion Boolean Methods

    /**
     * This is used to determine whether the created image should be scaled or not by checking the
     * Height and Width values. If either is -1, then we need to scale the produced image
     * @return whether to scale the image or not
     */
    protected boolean isScaled(int width, int height) {
        return (width == -1) || (height == -1);
    }

    /**
     * @return the ImageIcon for the Image stored by the AbstractIcon
     */
    public ImageIcon getImageIcon() {
        return new ImageIcon(getImage());
    }

    public Image getImage() {
        return getImage(0, 0);
    }

    public Image getImage(int size) {
        return getImage(size, size);
    }

    /**
     * This is used to create the proper image and scale it if required. It also handles null protection
     * by creating a blank image if required.
     * @return the created image
     */
    public Image getImage(int width, int height) {
        Image image = getBaseImage();

        if (image == null) {
            return ImageUtil.failStandardImage();
        } else if (isScaled(width, height)) {
            return scaleAndCenter(image, (width != -1) ? width : height);
        } else {
            return image;
        }
    }

    /**
     * Returns a square BufferedImage of the given size.
     * Scales the given image to fit into the square and centers it
     * on a transparent background.
     */
    private static BufferedImage scaleAndCenter(Image image, int size) {
        BufferedImage result = ImageUtil.createAcceleratedImage(size, size);
        Graphics g = result.getGraphics();
        if (image.getWidth(null) > image.getHeight(null)) {
            image = image.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
            g.drawImage(image, 0, (size - image.getHeight(null)) / 2, null);
        } else {
            image = image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
            g.drawImage(image, (size - image.getWidth(null)) / 2, 0, null);
        }
        return result;
    }

    /**
     * This is abstract to allow for different formats for determining the image in question
     * @return the Image stored by the AbstractIcon
     */
    public abstract Image getBaseImage();

    //region File IO
    /**
     * This writes the AbstractIcon to XML
     * @param pw1 the PrintWriter to write to
     * @param indent the indentation of the first line
     */
    public void writeToXML(PrintWriter pw1, int indent) {
        MegaMekXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "AbstractIcon");
        MegaMekXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "category", getCategory());
        MegaMekXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "filename", getFilename());
        MegaMekXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "AbstractIcon");
    }

    /**
     * This is used to parse an AbstractIcon from a saved XML node
     * @param retVal the AbstractIcon to parse into
     * @param wn the node to parse from
     * @return the parsed AbstractIcon
     */
    public static AbstractIcon parseFromXML(AbstractIcon retVal, Node wn) {
        try {
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("category")) {
                    retVal.setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("filename")) {
                    retVal.setFilename(wn2.getTextContent().trim());
                }
            }
        } catch (Exception e) {
            MegaMek.getLogger().error("Failed to parse icon from nodes", e);
        }

        return retVal;
    }
    //endregion File IO

    @Override
    public String toString() {
        return getCategory() + "/" + getFilename();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof AbstractIcon) {
            AbstractIcon dOther = (AbstractIcon) other;
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
