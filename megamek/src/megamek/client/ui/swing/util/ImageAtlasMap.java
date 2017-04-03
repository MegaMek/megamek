/*
 * MegaMek - Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;

import megamek.common.Configuration;

/**
 * 
 * @author arlith
 *
 */
public class ImageAtlasMap {

    Map<String, String> imgFileToAtlasMap = new HashMap<>();

    public ImageAtlasMap() {
    }

    /**
     * 
     * @param value
     * @param key
     */
    public void put(Path value, Path key) {
        imgFileToAtlasMap.put(convertPathToLinux(value), convertPathToLinux(key));
    }

    /**
     * 
     * @param key
     * @return
     */
    public boolean containsKey(Path key) {
        return imgFileToAtlasMap.containsKey(convertPathToLinux(key));
    }

    /**
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

    /**
     * 
     * @param key
     * @return
     */
    public String get(Path key) {
        return imgFileToAtlasMap.get(convertPathToLinux(key));
    }

    /**
     * 
     * @return
     */
    public boolean writeToFile() {
        XStream xstream = new XStream();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(Configuration.imageFileAtlasMapFile()),
                Charset.forName("UTF-8"));) {
            xstream.toXML(imgFileToAtlasMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 
     * @return
     */
    public static ImageAtlasMap readFromFile() {
        if (!Configuration.imageFileAtlasMapFile().exists()) {
            return null;
        }

        ImageAtlasMap map;
        try (InputStream is = new FileInputStream(Configuration.imageFileAtlasMapFile())) {
            XStream xstream = new XStream();
            map = (ImageAtlasMap) xstream.fromXML(is);
        } catch (FileNotFoundException e) {
            map = null;
            e.printStackTrace();
        } catch (IOException e) {
            map = null;
            e.printStackTrace();
        }
        return map;
    }

}
