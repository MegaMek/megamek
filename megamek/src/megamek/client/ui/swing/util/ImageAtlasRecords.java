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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Class to encapsulate a list of image atlas records for the purposes of
 * serialization. Contains methods for easily adding, removing, and accessing
 * individual records within the list.
 *
 * @author rjhancock
 */
@JsonDeserialize(as = ImageAtlasRecords.class)
@JsonSerialize(as = ImageAtlasRecords.class)
public class ImageAtlasRecords {
    private List<ImageAtlasRecord> records = new ArrayList<>();

    /**
     * Default constructor.
     */
    public ImageAtlasRecords() {
    }

    /**
     * Constructor that takes an existing list of @see ImageAtlasRecord objects.
     *
     * @param records
     */
    public ImageAtlasRecords(List<ImageAtlasRecord> records) {
        this.records = records;
    }

    /**
     * Adds a new record to the list.
     *
     * @param originalFilePath
     * @param atlasFilePath
     */
    public void addRecord(String originalFilePath, String atlasFilePath) {
        ImageAtlasRecord atlasRecord = new ImageAtlasRecord(originalFilePath, atlasFilePath);
        records.add(atlasRecord);
    }

    /**
     * Returns the Atlas File Path for an Original File Path.
     *
     * @param originalFilePath
     */
    public String get(String originalFilePath) {
        for (ImageAtlasRecord atlasRecord : records) {
            if (atlasRecord.getOriginalFilePath().equals(originalFilePath)) {
                return atlasRecord.getAtlasFilePath();
            }
        }

        return null;
    }

    /**
     * Returns true if the AtlasMap contains the given Original File Path.
     *
     * @param originalFilePath
     */
    public Boolean containsKey(String originalFilePath) {
        for (ImageAtlasRecord atlasRecord : records) {
            if (atlasRecord.getOriginalFilePath().equals(originalFilePath)) {
                return true;
            }
        }

        return false;
    }
}
