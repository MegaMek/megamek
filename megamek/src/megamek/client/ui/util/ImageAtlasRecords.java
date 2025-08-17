/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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


package megamek.client.ui.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Class to encapsulate a list of image atlas records for the purposes of serialization. Contains methods for easily
 * adding, removing, and accessing individual records within the list.
 *
 * @author rjhancock
 */
@JsonDeserialize(as = ImageAtlasRecords.class)
@JsonSerialize(as = ImageAtlasRecords.class)

public class ImageAtlasRecords {
    @JsonProperty("records")
    private List<ImageAtlasRecord> records = new ArrayList<>();

    /**
     * Default constructor.
     */
    public ImageAtlasRecords() {
    }

    /**
     * Constructor that takes an existing list of @see ImageAtlasRecord objects.
     *
     */
    public ImageAtlasRecords(List<ImageAtlasRecord> records) {
        this.records = records;
    }

    /**
     * Adds a new record to the list.
     *
     */
    public void addRecord(String originalFilePath, String atlasFilePath) {
        ImageAtlasRecord atlasRecord = new ImageAtlasRecord(originalFilePath, atlasFilePath);
        records.add(atlasRecord);
    }

    /**
     * Returns the Atlas File Path for an Original File Path.
     *
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
