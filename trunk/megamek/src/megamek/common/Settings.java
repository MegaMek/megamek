/**
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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
    
    public static boolean   minimapEnabled          = true;
    public static int       minimapPosX             = 0;
    public static int       minimapPosY             = 0;
    public static int       minimapSizeWidth        = 168;
    public static int       minimapSizeHeight       = 204;
    
    public static int       displayPosX             = 0;
    public static int       displayPosY             = 0;
    public static int       displaySizeWidth        = 235;
    public static int       displaySizeHeight       = 370;
    
    public static int       rulerPosX             = 0;
    public static int       rulerPosY             = 0;
    public static int       rulerSizeWidth        = 350;
    public static int       rulerSizeHeight       = 240;
    public static Color     rulerColor1           = Color.cyan;
    public static Color     rulerColor2           = Color.magenta;

    public static int       minimumSizeWidth        = 120;
    public static int       minimumSizeHeight       = 200;

    public static boolean   autoEndFiring           = true;

    public static boolean   nagForMASC              = true;
    public static boolean   nagForPSR               = true;
    public static boolean   nagForNoAction          = false;
    
    public static boolean   showMoveStep            = true;
    public static int       moveStepDelay           = 500;
    public static boolean   showWrecks              = true;
    
    public static String    lastPlayerName          = "";
    public static int       lastPlayerColor         = 0;
    public static String    lastPlayerCamoName      = null;
    public static String    lastPlayerCategory      = "";
    
    public static String    lastServerPass          = "";
    public static int       lastServerPort          = 2346;
    
    public static String    lastConnectAddr         = "localhost";
    public static int       lastConnectPort         = 2346;
    
    public static Color     mapTextColor            = Color.black;
    
    public static Color     moveDefaultColor        = Color.cyan;
    public static Color     moveRunColor            = Color.yellow;
    public static Color     moveJumpColor           = Color.red;
    public static Color     moveIllegalColor        = Color.darkGray;
    public static Color     moveMASCColor           = new Color(255,140,0);
    
    public static String    mapTileset              = "defaulthexset.txt";
    
    public static String    mechDirectory           = "data" + File.separator + "mechfiles";

    public static boolean   soundMute               = false;
    public static String    soundBingFilename       = "data" + File.separator + "sounds" + File.separator + "call.wav";

    /**
     * The player wants to track memory use. Setting this to <code>true</code>
     * will cause the client to dump a snapshot of memory usage to the log
     * file at the beginning of selected phases of each turn.  Very useful
     * in performance analysis.
     * 
     * @see     megamek.client.Client#changePhase(int)
     * @see     megamek.client.Client#memDump(String)
     */
    public static boolean   memoryDumpOn            = false;
    
    /**
     * Controls whether the hex info popup is shown when the mouse hovers over the main map
     * 
     * @see     megamek.client.BoardView1
     */
    public static boolean   showMapHexPopup          = true;

    /**
     * Controls whether the hex info popup is shown when the mouse hovers over the main map
     * 
     * @see     megamek.client.BoardView1
     */
    public static int       tooltipDelay             = 1000;

    /**
     * The MegaMek-standard "newline" string is that of Windows.  Basically,
     * apps written for *nix or MacOS will know how to read Windows newlines,
     * but Windows apps are (usually) unable to read *nix or MacOS newlines.
     */
    public static final String NL = "\r\n";

    private static String[] m_sColorNames = { "black", "blue", "cyan", "darkgray", "gray", 
            "green", "lightgray", "magenta", "orange", "pink", "red", "white", "yellow" };
            
    private static Color[] m_colorValues = { Color.black, Color.blue, Color.cyan,
            Color.darkGray, Color.gray, Color.green, Color.lightGray, 
            Color.magenta, Color.orange, Color.pink, Color.red, Color.white, 
            Color.yellow };
    
    /**
     * Name of file to log all Mek hit location rolls.
     */
    public static String   mekHitLocLogName         = null;
    public static PrintWriter mekHitLocLog          = null;

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
                    else if(key.equals("windowsize")) {
                        st.nextToken();
                        windowSizeWidth = (int)st.nval;
                        st.nextToken();
                        windowSizeHeight = (int)st.nval;
                    }
                    else if (key.equals("minimapenabled")) {
                        st.nextToken();
                        minimapEnabled = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("minimappos")) {
                        st.nextToken();
                        minimapPosX = (int)st.nval;
                        st.nextToken();
                        minimapPosY = (int)st.nval;
                    }
                    else if(key.equals("minimapsize")) {
                        st.nextToken();
                        minimapSizeWidth = (int)st.nval;
                        st.nextToken();
                        minimapSizeHeight = (int)st.nval;
                    }
                    else if(key.equals("displaypos")) {
                        st.nextToken();
                        displayPosX = (int)st.nval;
                        st.nextToken();
                        displayPosY = (int)st.nval;
                    }
                    else if(key.equals("displaysize")) {
                        st.nextToken();
                        displaySizeWidth = (int)st.nval;
                        st.nextToken();
                        displaySizeHeight = (int)st.nval;
                    }
                    else if(key.equals("rulerpos")) {
                        st.nextToken();
                        rulerPosX = (int)st.nval;
                        st.nextToken();
                        rulerPosY = (int)st.nval;
                    }
                    else if(key.equals("rulersize")) {
                        st.nextToken();
                        rulerSizeWidth = (int)st.nval;
                        st.nextToken();
                        rulerSizeHeight = (int)st.nval;
                    }
                    else if(key.equals("rulercolors")) {
                        rulerColor1 = loadColor(st, rulerColor1);
                        rulerColor2 = loadColor(st, rulerColor2);
                    }
                    else if(key.equals("minimumdialogsize")) {
                        st.nextToken();
                        minimumSizeWidth = (int)st.nval;
                        st.nextToken();
                        minimumSizeHeight = (int)st.nval;
                    }
                    else if (key.equals("autoendfiring")) {
                        st.nextToken();
                        autoEndFiring = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if (key.equals("nagformasc")) {
                        st.nextToken();
                        nagForMASC = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if (key.equals("nagforpsr")) {
                        st.nextToken();
                        nagForPSR = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if (key.equals("nagfornoaction")) {
                        st.nextToken();
                        nagForPSR = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("playername")) {
                        st.nextToken();
                        lastPlayerName = st.sval;
                    }
                    else if(key.equals("server")) {
                        st.nextToken();
                        lastServerPass = st.sval;
                        st.nextToken();
                        lastServerPort = (int)st.nval;
                    }
                    else if(key.equals("connect")) {
                        st.nextToken();
                        lastConnectAddr = st.sval;
                        st.nextToken();
                        lastConnectPort = (int)st.nval;
                    }
                    else if(key.equals("maptext")) {
                        mapTextColor = loadColor(st, mapTextColor);
                    }
                    else if (key.equals("movedefault")) {
                        moveDefaultColor = loadColor(st, moveDefaultColor);
                    }
                    else if (key.equals("moverun")) {
                        moveRunColor = loadColor(st, moveRunColor);
                    }
                    else if (key.equals("movejump")) {
                        moveJumpColor = loadColor(st, moveJumpColor);
                    }
                    else if (key.equals("moveillegal")) {
                        moveIllegalColor = loadColor(st, moveIllegalColor);
                    }
                    else if (key.equals("movemasc")) {
                        moveMASCColor = loadColor(st, moveMASCColor);
                    }
                    else if(key.equals("maptileset")) {
                        st.nextToken();
                        mapTileset = st.sval;
                    }
                    else if ( key.equals("mekhitloclog") ) {
                        st.nextToken();
                        mekHitLocLogName = new String(st.sval);
                        try {
                            mekHitLocLog = new PrintWriter
                                ( new BufferedWriter
                                    ( new FileWriter(mekHitLocLogName) ) );
                            mekHitLocLog.println( "Table\tSide\tRoll" );
                        } catch ( Throwable thrown ) {
                            thrown.printStackTrace();
                            mekHitLocLog = null;
                        }
                    }
                    else if ( key.equals("showmovestep")) {
                        st.nextToken();
                        showMoveStep = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("movestepdelay")) {
                        st.nextToken();
                        moveStepDelay = (int)st.nval;
                    }
                    else if(key.equals("showwrecks")) {
                        st.nextToken();
                        showWrecks = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("soundmute")) {
                        st.nextToken();
                        soundMute = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("soundbingfilename")) {
                        st.nextToken();
                        soundBingFilename = st.sval;
                    }
                    else if(key.equals("memorydumpon")) {
                        st.nextToken();
                        memoryDumpOn = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("showmaphexpopup")) {
                        st.nextToken();
                        showMapHexPopup = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("tooltipdelay")) {
                        st.nextToken();
                        tooltipDelay = (int)st.nval;
                    }
                }
            }
            
            cr.close();
        } catch(Exception e) {
            System.err.println("error reading settings file:");
            System.err.println(e.getMessage());
        }
    }
    
    private static Color loadColor(StreamTokenizer st, Color cDefault) {
        try {
            st.nextToken();
            if (st.ttype == st.TT_NUMBER) {
                int red = (int)st.nval;
                st.nextToken();
                int green = (int)st.nval;
                st.nextToken();
                int blue = (int)st.nval;
                return new Color(red, green, blue);
            } else if (st.ttype == st.TT_WORD) {
                String sName = st.sval;
                for (int x = 0; x < m_sColorNames.length; x++) {
                    if (m_sColorNames[x].equalsIgnoreCase(sName)) {
                        return m_colorValues[x];
                    }
                }
                System.out.println("Unrecognized color: " + sName);
            }
        } catch (Exception e) {
            System.err.println("Unable to load color data");
        }
        return cDefault;
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
            cw.write("minimapenabled " + minimapEnabled + "\r\n");
            cw.write("minimappos " + minimapPosX + " " + minimapPosY + "\r\n");
            cw.write("minimapsize " + minimapSizeWidth + " " + minimapSizeHeight + "\r\n");
            cw.write("displaypos " + displayPosX + " " + displayPosY + "\r\n");
            cw.write("displaysize " + displaySizeWidth + " " + displaySizeHeight + "\r\n");
            cw.write("rulerpos " + rulerPosX + " " + rulerPosY + "\r\n");
            cw.write("rulersize " + rulerSizeWidth + " " + rulerSizeHeight + "\r\n");
            cw.write("rulercolors " + writeColor(rulerColor1) + " " + writeColor(rulerColor2) + "\r\n");
            cw.write("autoendfiring " + autoEndFiring + "\r\n");
            cw.write("nagformasc " + nagForMASC + "\r\n");
            cw.write("nagforpsr " + nagForPSR + "\r\n");
            cw.write("nagfornoaction " + nagForNoAction + "\r\n");
            cw.write("playername " + "\"" + lastPlayerName + "\"" + "\r\n");
            cw.write("server " + "\"" + lastServerPass + "\" " + lastServerPort + "\r\n");
            cw.write("connect " + "\"" + lastConnectAddr + "\" " + lastConnectPort + "\r\n");
            cw.write("maptext " + writeColor(mapTextColor) + "\r\n");
            cw.write("movedefault " + writeColor(moveDefaultColor) + "\r\n");
            cw.write("moverun " + writeColor(moveRunColor) + "\r\n");
            cw.write("movejump " + writeColor(moveJumpColor) + "\r\n");
            cw.write("moveillegal " + writeColor(moveIllegalColor) + "\r\n");
            cw.write("movemasc " + writeColor(moveMASCColor) + "\r\n");
            cw.write("maptileset \"" + mapTileset + "\"\r\n");
            cw.write("showmovestep " + showMoveStep + "\r\n");
            cw.write("movestepdelay " + moveStepDelay + "\r\n");
            cw.write("showwrecks " + showWrecks + "\r\n");
            cw.write("soundmute " + soundMute + "\r\n");
            cw.write("soundbingfilename \"" + soundBingFilename + "\"\r\n");
            cw.write("memorydumpon " + memoryDumpOn + "\r\n");
            cw.write("minimumdialogsize " + minimumSizeWidth + " " + minimumSizeHeight + "\r\n");
            if ( mekHitLocLog != null ) {
                mekHitLocLog.flush();
                mekHitLocLog.close();
                cw.write("mekhitloclog \"" + mekHitLocLogName + "\"\r\n");
            }
            cw.write("showmaphexpopup " + showMapHexPopup + "\r\n");
            cw.write("tooltipdelay " + tooltipDelay + "\r\n");
            cw.close();
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    private static String writeColor(Color c) {
        for (int x = 0; x < m_colorValues.length; x++) {
            if (c == m_colorValues[x]) {
                return m_sColorNames[x];
            }
        }
        return c.getRed() + " " + c.getGreen() + " " + c.getBlue();
    }
}
