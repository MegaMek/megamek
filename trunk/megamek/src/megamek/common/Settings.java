/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.awt.*;
import java.io.*;

public class Settings
{
    public static String    cfgFileName             = "MegaMek.cfg";
    
    
    public static int       windowPosX              = 0;
    public static int       windowPosY              = 0;
    public static int       windowSizeWidth         = 800;
    public static int       windowSizeHeight        = 600;
    
    public static String    lastPlayerName          = "";
    public static int       lastPlayerColor;
    
    public static String    lastServerPass          = "";
    public static int       lastServerPort          = 2346;
    
    public static String    lastConnectAddr         = "localhost";
    public static int       lastConnectPort         = 2346;
    
    public static Color     mapTextColor            = Color.black;
    
    public static String    mapTileset              = "defaulthexset.txt";
    
    
    /**
     * Loads the settings from disk
     */
    public static void load() {
        // read, read, read!
        try {
            File cfgfile = new File(cfgFileName);
            if(!cfgfile.exists()) {
                return;
            }
            Reader cr = new FileReader(cfgfile);
            StreamTokenizer st = new StreamTokenizer(cr);

            st.lowerCaseMode(true);
            st.quoteChar('"');
            st.commentChar('#');

scan:
            while(true) {
                switch(st.nextToken()) {
                case StreamTokenizer.TT_EOF:
                    break scan;
                case StreamTokenizer.TT_EOL:
                    break scan;
                case StreamTokenizer.TT_WORD:
                    // read in 
                    String key = st.sval;
                    if(key.equals("windowpos")) {
                        st.nextToken();
                        windowPosX = (int)st.nval;
                        st.nextToken();
                        windowPosY = (int)st.nval;
                    }
                    if(key.equals("windowsize")) {
                        st.nextToken();
                        windowSizeWidth = (int)st.nval;
                        st.nextToken();
                        windowSizeHeight = (int)st.nval;
                    }
                    if(key.equals("playername")) {
                        st.nextToken();
                        lastPlayerName = st.sval;
                    }
                    if(key.equals("server")) {
                        st.nextToken();
                        lastServerPass = st.sval;
                        st.nextToken();
                        lastServerPort = (int)st.nval;
                    }
                    if(key.equals("connect")) {
                        st.nextToken();
                        lastConnectAddr = st.sval;
                        st.nextToken();
                        lastConnectPort = (int)st.nval;
                    }
                    if(key.equals("maptext")) {
                        st.nextToken();
                        int red = (int)st.nval;
                        st.nextToken();
                        int green = (int)st.nval;
                        st.nextToken();
                        int blue = (int)st.nval;
                        mapTextColor = new Color(red, green, blue);
                    }
                    if(key.equals("maptileset")) {
                        st.nextToken();
                        mapTileset = st.sval;
                    }
                    
                }
            }
            
            cr.close();
        } catch(Exception e) {
            System.err.println("error reading settings file:");
            System.err.println(e.getMessage());
        }
    }
    
    /**
     * Saves the settings to disk
     */
    public static void save() {
        // yay! file stuff!
        try {
            File cfgfile = new File(cfgFileName);
            Writer cw = new FileWriter(cfgfile);
            
            cw.write("# MegaMek config file" + "\r\n");
            cw.write("# Edit at your own risk" + "\r\n");
            cw.write("\r\n");
            cw.write("windowpos " + windowPosX + " " + windowPosY + "\r\n");
            cw.write("windowsize " + windowSizeWidth + " " + windowSizeHeight + "\r\n");
            cw.write("playername " + "\"" + lastPlayerName + "\"" + "\r\n");
            cw.write("server " + "\"" + lastServerPass + "\" " + lastServerPort + "\r\n");
            cw.write("connect " + "\"" + lastConnectAddr + "\" " + lastConnectPort + "\r\n");
            cw.write("maptext " + mapTextColor.getRed() + " " + mapTextColor.getGreen() + " " + mapTextColor.getBlue() + " " + "\r\n");
            cw.write("maptileset \"" + mapTileset + "\"\r\n");
            
            cw.close();
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
