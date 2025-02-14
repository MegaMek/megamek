/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package megamek.client.ui.swing.util;

/**
 * Class to map a file path to a location within an image atlas file. Currently
 * stores the original file path along with the path to the atlas with
 * coordinates as the file path and will eventually be converted to better
 * represent the data as individual components to allow more customization.
 *
 * @author rjhancock
 */
public class ImageAtlasRecord {
    // Original to ensure current implementation works as expected.
    private String originalFilePath;
    private String atlasFilePath;

    // Planned part 2 to simplify the data and processing itself.
    private Integer xCoordinate;
    private Integer yCoordinate;
    private Integer width;
    private Integer height;

    /**
     * Default constructor.
     */
    public ImageAtlasRecord(String originalFilePath, String atlasFilePath) {
        this.originalFilePath = originalFilePath;
        this.atlasFilePath = atlasFilePath;
    }

    /**
     * Get the original file path.
     *
     * @return
     */
    public String getOriginalFilePath() {
        return originalFilePath;
    }

    /**
     * Get the atlas file path.
     *
     * @return
     */
    public String getAtlasFilePath() {
        return atlasFilePath;
    }

    /**
     * Get the x coordinate.
     *
     * @return
     */
    public Integer getXCoordinate() {
        return xCoordinate;
    }

    /**
     * Get the y coordinate.
     *
     * @return
     */
    public Integer getYCoordinate() {
        return yCoordinate;
    }

    /**
     * Get the width.
     *
     * @return
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * Get the height.
     *
     * @return
     */
    public Integer getHeight() {
        return height;
    }

    /**
     * Set the original file path.
     *
     * @param originalFilePath
     */
    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    /**
     * Set the atlas file path.
     *
     * @param atlasFilePath
     */
    public void setAtlasFilePath(String atlasFilePath) {
        this.atlasFilePath = atlasFilePath;
    }

    /**
     * Set the x coordinate.
     *
     * @param xCoordinate
     */
    public void setXCoordinate(Integer xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    /**
     * Set the y coordinate.
     *
     * @param yCoordinate
     */
    public void setYCoordinate(Integer yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    /**
     * Set the width.
     *
     * @param width
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Set the height.
     *
     * @param height
     */
    public void setHeight(Integer height) {
        this.height = height;
    }
}
