/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

public class ImageAtlasRecord {
    // Original to ensure current implementation works as expected.
    private String originalFilePath;
    private String atlasFilePath;

    // Planned part 2 to simplify the data and processing itself.
    private Integer xCoordinate;
    private Integer yCoordinate;
    private Integer width;
    private Integer height;

    public ImageAtlasRecord(String originalFilePath, String atlasFilePath) {
        this.originalFilePath = originalFilePath;
        this.atlasFilePath = atlasFilePath;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public String getAtlasFilePath() {
        return atlasFilePath;
    }

    public Integer getXCoordinate() {
        return xCoordinate;
    }

    public Integer getYCoordinate() {
        return yCoordinate;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public void setAtlasFilePath(String atlasFilePath) {
        this.atlasFilePath = atlasFilePath;
    }

    public void setXCoordinate(Integer xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public void setYCoordinate(Integer yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
