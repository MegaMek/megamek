/*
 * MegaMek - Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.util;

import com.thoughtworks.xstream.XStream;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to encapsulate a map that maps old image paths to the subsequent location in an image atlas.  This allows us
 * to keep the old mechsets while still packaging the images into an atlas.
 * 
 * There's a potential cross-platform path issue as the Java <code>File</code> class uses the current system's file
 * system to do file comparisons.  If we write windows-style path strings to a file and read that in with UNIX, it can
 * cause comparisons to fail.  Because of this, the internal map is stored with filepaths represented as strings, but
 * they are passed in as paths which then are expicitly converted to UNIX-style filepaths.
 *
 * @author arlith
 */
public class ImageAtlasMap {
    Map<String, String> imgFileToAtlasMap = new HashMap<>();

    public ImageAtlasMap() {

    }

    private  ImageAtlasMap(Map<String, String> map) {
        imgFileToAtlasMap = map;
    }

    /**
     * Insert new values into the atlas map, using Paths which get converted to UNIX-style path strings.
     *
     * @param value
     * @param key
     */
    public void put(Path value, Path key) {
        imgFileToAtlasMap.put(convertPathToLinux(value), convertPathToLinux(key));
    }

    /**
     * Return true if the atlas map contains the given path, which is converted to UNIX-style path strings.
     *
     * @param key
     * @return
     */
    public boolean containsKey(Path key) {
        return imgFileToAtlasMap.containsKey(convertPathToLinux(key));
    }

    /**
     * Internal convenience method for converting a <code>Path</code> to UNIX-style path strings.
     *
     * @param p
     * @return
     */
    private String convertPathToLinux(Path p) {
        // Generate a canonical path
        StringBuilder v = new StringBuilder();
        int numNames = p.getNameCount() - 1;
        for (int i = 0; i < numNames; i++) {
            v.append(p.getName(i));
            v.append("/");
        }
        v.append(p.getFileName());
        return v.toString();
    }

    public String get(Path key) {
        return imgFileToAtlasMap.get(convertPathToLinux(key));
    }

    public boolean writeToFile() {
        XStream xstream = new XStream();
        try (OutputStream fos = new FileOutputStream(Configuration.imageFileAtlasMapFile());
             Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            xstream.toXML(imgFileToAtlasMap, writer);
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable ImageAtlasMap readFromFile() {
        if (!Configuration.imageFileAtlasMapFile().exists()) {
            return null;
        }

        try (InputStream is = new FileInputStream(Configuration.imageFileAtlasMapFile())) {
            XStream xstream = new XStream();
            return new ImageAtlasMap((Map<String, String>) xstream.fromXML(is));
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
    }
}
