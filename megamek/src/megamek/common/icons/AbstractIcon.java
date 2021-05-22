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

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

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

    protected AbstractIcon(@Nullable String category) {
        this(category, DEFAULT_ICON_FILENAME);
    }

    protected AbstractIcon(@Nullable String category, @Nullable String filename) {
        setCategory(category);
        setFilename(filename);
    }
    //endregion Constructors

    //region Getters/Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = (category == null) ? ROOT_CATEGORY : category;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(@Nullable String filename) {
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
    protected boolean isScaled(int width, int height) {
        return (width == -1) || (height == -1);
    }

    /**
     * @return the ImageIcon for the Image stored by the AbstractIcon. May be null for non-existent
     * files
     */
    public @Nullable ImageIcon getImageIcon() {
        Image image = getImage();
        return (image == null) ? null : new ImageIcon(image);
    }

    public @Nullable ImageIcon getImageIcon(int size) {
        Image image = getImage(size);
        return (image == null) ? null : new ImageIcon(image);
    }

    public @Nullable Image getImage() {
        return getImage(0, 0);
    }

    public @Nullable Image getImage(int size) {
        return getImage(size, -1);
    }

    public @Nullable Image getImage(int width, int height) {
        return getImage(getBaseImage(), width, height);
    }

    /**
     * This is used to create the proper image and scale it if required. It also handles null protection
     * by creating a blank image if required.
     * @return the created image
     */
    protected Image getImage(Image image, int width, int height) {
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

    @Override
    public String toString() {
        return hasDefaultCategory() ? getFilename()
                : (getCategory().endsWith("/") ? getCategory() : getCategory() + "/") + getFilename();
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
