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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Class to map a file path to a location within an image atlas file. Currently
 * stores the original file path along with the path to the atlas with
 * coordinates as the file path and will eventually be converted to better
 * represent the data as individual components to allow more customization.
 *
 * @author rjhancock
 */
@JsonDeserialize(as = ImageAtlasRecord.class)
@JsonSerialize(as = ImageAtlasRecord.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ImageAtlasRecord {
    // Original to ensure current implementation works as expected.
    private String originalFilePath;
    private String atlasFilePath;

    public ImageAtlasRecord() {
    }

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
}
