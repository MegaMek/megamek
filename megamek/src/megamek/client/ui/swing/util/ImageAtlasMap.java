/*
 * MegaMek - Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
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

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Class to encapsulate a map that maps old image paths to the subsequent
 * location in an image atlas. This allows us to keep the old MekSets while
 * still packaging the images into an atlas.
 *
 * There's a potential cross-platform path issue as the Java <code>File</code>
 * class uses the current system's file system to do file comparisons. If we
 * write windows-style path strings to a file and read that in with UNIX, it can
 * cause comparisons to fail. Because of this, the internal map is stored with
 * file paths represented as strings, but they are passed in as paths which then
 * are explicitly converted to UNIX-style file paths.
 *
 * @author arlith
 */

public class ImageAtlasMap {
    private static final MMLogger logger = MMLogger.create(ImageAtlasMap.class);
    private ImageAtlasRecords imgFileToAtlasMap = new ImageAtlasRecords();

    public ImageAtlasMap() {
    }

    public ImageAtlasMap(ImageAtlasRecords imgFileToAtlasMap) {
        this.imgFileToAtlasMap = imgFileToAtlasMap;
    }

    public Map<String, String> getImgFileToAtlasMap() {
        return imgFileToAtlasMap.toMap();
    }

    public void setImgFileToAtlasMap(Map<String, String> map) {
        imgFileToAtlasMap.fromMap(map);
    }

    /**
     * Insert new values into the atlas map, using Paths which get converted to
     * UNIX-style path strings.
     *
     * @param value
     * @param key
     */
    public void put(Path value, Path key) {
        String valueString = FilenameUtils.separatorsToUnix(value.toString());
        String keyString = FilenameUtils.separatorsToUnix(key.toString());
        imgFileToAtlasMap.addRecord(valueString, keyString);
    }

    public String get(Path key) {
        String keyString = FilenameUtils.separatorsToUnix(key.toString());
        return imgFileToAtlasMap.get(keyString);
    }

    /**
     * Return true if the atlas map contains the given path, which is converted to
     * UNIX-style path strings.
     *
     * @param key
     *
     * @return
     */
    public boolean containsKey(Path key) {
        String valueString = FilenameUtils.separatorsToUnix(key.toString());
        return imgFileToAtlasMap.containsKey(valueString);
    }

    public void writeToFile() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        try {
            mapper.writeValue(Configuration.imageFileAtlasMapFile(), imgFileToAtlasMap);
        } catch (Exception e) {
            logger.error("Unable to write to Image Atlas Map File", e);
        }
    }

    public static @Nullable ImageAtlasMap readFromFile() {
        if (!Configuration.imageFileAtlasMapFile().exists()) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        try {
            ImageAtlasRecords imgFileToAtlasMap = mapper.readValue(Configuration.imageFileAtlasMapFile(),
                    ImageAtlasRecords.class);
            return new ImageAtlasMap(imgFileToAtlasMap);
        } catch (Exception e) {
            logger.error("Unable to read to Image Atlas Map File", e);
        }

        return null;
    }
}
