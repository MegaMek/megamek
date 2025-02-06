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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageAtlasRecords {
    private List<ImageAtlasRecord> records = new ArrayList<>();

    public ImageAtlasRecords() {
    }

    public ImageAtlasRecords(List<ImageAtlasRecord> records) {
        this.records = records;
    }

    public ImageAtlasRecords(Map<String, String> map) {
        fromMap(map);
    }

    public List<ImageAtlasRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ImageAtlasRecord> records) {
        this.records = records;
    }

    public void addRecord(ImageAtlasRecord atlasRecord) {
        records.add(atlasRecord);
    }

    public void addRecord(String originalFilePath, String atlasFilePath) {
        ImageAtlasRecord atlasRecord = new ImageAtlasRecord(originalFilePath, atlasFilePath);
        records.add(atlasRecord);
    }

    public String get(String originalFilePath) {
        for (ImageAtlasRecord atlasRecord : records) {
            if (atlasRecord.getOriginalFilePath().equals(originalFilePath)) {
                return atlasRecord.getAtlasFilePath();
            }
        }

        return null;
    }

    public Boolean containsKey(String originalFilePath) {
        for (ImageAtlasRecord atlasRecord : records) {
            if (atlasRecord.getOriginalFilePath().equals(originalFilePath)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        for (ImageAtlasRecord atlasRecord : records) {
            map.put(atlasRecord.getOriginalFilePath(), atlasRecord.getAtlasFilePath());
        }

        return map;
    }

    public void fromMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            addRecord(entry.getKey(), entry.getValue());
        }
    }

    public void removeRecord(ImageAtlasRecord atlasRecord) {
        records.remove(atlasRecord);
    }
}
