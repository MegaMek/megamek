/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
import java.util.Properties;
import java.util.Enumeration;

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
    public static int       minimapZoom             = 0;
    
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
    public static boolean   nagForNoAction          = true;
    
    public static boolean   nagForReadme            = true;
    public static boolean   nagForBotReadme         = true;
    public static boolean   nagForMapEdReadme       = true;

    public static boolean   showMoveStep            = true;
    public static int       moveStepDelay           = 500;
    public static boolean   showWrecks              = true;

    public static boolean   getFocus                = false;

    // scrolling settings
    public  static boolean   rightDragScroll        = true;
    public  static boolean   alwaysRightClickScroll = false;
    public  static boolean   ctlScroll              = false;
    public  static boolean   clickEdgeScroll        = false;
    public  static boolean   autoEdgeScroll         = false;
    private static int       scrollSensitivity      = 3;
    
    public static String    lastPlayerName          = "";
    public static int       lastPlayerColor         = 0;
    public static String    lastPlayerCamoName      = null;
    public static String    lastPlayerCategory      = "";
    
    public static String    lastServerPass          = "";
    public static int       lastServerPort          = 2346;
    
    public static String    lastConnectAddr         = "localhost";
    public static int       lastConnectPort         = 2346;
    
    public static Color     mapTextColor            = Color.black;
    public static int	    mapZoomIndex            = 7;
    
    public static Color     moveDefaultColor        = Color.cyan;
    public static Color     moveRunColor            = Color.yellow;
    public static Color     moveJumpColor           = Color.red;
    public static Color     moveIllegalColor        = Color.darkGray;
    public static Color     moveMASCColor           = new Color(255,140,0);
    
    public static String    mapTileset              = "defaulthexset.txt";
    public static String    minimapColours          = "defaultminimap.txt";    
    
    public static String    mechDirectory           = "data" + File.separator + "mechfiles";

    public static boolean    soundMute              = false;

    public static boolean    keepServerlog          = false;
    public static String     serverlogFilename      = "gamelog.txt";
    public static int        serverlogMaxSize       = 1;

    // I *intentionally* use a hardcoded "/" instead of File.separator.
    // (a) I'm defining an "abstract pathname" that generates a URI, so it
    //          works on systems which have "\" as File.separator.
    // (b) Having "\" in the pathname **does not work**; for reasons that
    //          are *not* obvious to me, the "\" disappears from the
    //          MegaMek.cfg setting after repeated saves.
    public static String    soundBingFilename       = "data/sounds/call.wav";

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

    /** The starting character for unit designations. */
    public static char      unitStartChar            = 'A'; // == '\u0041'

    /** The number of milliseconds to search for an optimum path. */
    public static int       maxPathfinderTime        = MovePath.DEFAULT_PATHFINDER_TIME_LIMIT;

    public static int mechDisplaySmallFontSize = 9;
    public static int mechDisplayMediumFontSize = 10;
    public static int mechDisplayLargeFontSize = 12;

    public static int mechDisplayArmorSmallFontSize = 9; //no effect?
    public static int mechDisplayArmorMediumFontSize = 10;
    public static int mechDisplayArmorLargeFontSize = 12;

    public static int chatLoungeTabFontSize = 16;

    /** Whether to display the Chat Lounge in tabs or not. */
    public static boolean chatLoungeTabs = true;

    /** Enable all automatic ejection by default. */
    public static boolean defaultAutoejectDisabled = false;

    /** Do not show unit Ids by default. */
    public static boolean showUnitId = false;
    
    /** The system defaults for MegaMek settings. */
    private static Properties system = null;

    /**
     * The player's saved values for MegaMek settings.
     * <p/>
     * Under v0.29.x, this list does <strong>not</strong> contain the
     * values of "standard" MegaMek settings.
     */
    private static Properties saved = null;

    /** Any runtime overrides of MegaMek settings. */
    private static Properties runtime = null;

    /** The singleton <code>Settings</code> object. */
    private static final Settings instance = new Settings();

    /** Character used to delinieate strings in config file. */
    private static char quoteChar = '"';

    /** Character used for comments in config file. */
    private static char commentChar = '#';

    /**
     * Create and initialize the singleton instance.
     */
    private Settings() {

        // Load any runtime overrides.
        Settings.runtime = System.getProperties();

        // Remember the location of the default settings.
        String defaultSettings = Settings.cfgFileName;

        // Load the system default settings.
        saved = new Properties();
        Settings.load();

        // Has the player saved settings to a file of their own?
        String playerSettings = Settings.runtime.getProperty
            ( "cfgfilename", 
              Settings.saved.getProperty( "cfgfilename", 
                                          Settings.cfgFileName ) );
        if ( !defaultSettings.equals( playerSettings ) ) {
            // Yup.  Load the player's values and keep the system defaults.
            system = saved;
            Settings.cfgFileName = playerSettings;
            saved = new Properties( system );
            Settings.load();
        }

    }
        
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
            st.quoteChar(quoteChar);
            st.commentChar(commentChar);

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
                    else if(key.equals("minimapzoom")) {
                        st.nextToken();
                        minimapZoom = (int)st.nval;
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
                    else if(key.equals("mapzoomindex")) {
                        st.nextToken();
                        mapZoomIndex = (int)st.nval;
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
                        nagForNoAction = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if (key.equals("nagforreadme")) {
                        st.nextToken();
                        nagForReadme = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if (key.equals("nagforbotreadme")) {
                        st.nextToken();
                        nagForBotReadme = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if (key.equals("nagformapedreadme")) {
                        st.nextToken();
                        nagForMapEdReadme = Boolean.valueOf(st.sval).booleanValue();
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
                    else if(key.equals("minimapcolours")) {
                        st.nextToken();
                        minimapColours = st.sval;
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
                    else if(key.equals("unitstartchar")) {
                        st.nextToken();
                        unitStartChar = (char)st.nval;
                    }
                    else if(key.equals("maxpathfindertime")) {
                        st.nextToken();
                        maxPathfinderTime = (int)st.nval;
                    }
                    else if(key.equals("rightdragscroll")) {
                        st.nextToken();
                        rightDragScroll = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("alwaysrightclickscroll")) {
                        st.nextToken();
                        alwaysRightClickScroll = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("ctlscroll")) {
                        st.nextToken();
                        ctlScroll = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("clickedgescroll")) {
                        st.nextToken();
                        clickEdgeScroll = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("autoedgescroll")) {
                        st.nextToken();
                        autoEdgeScroll = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("scrollsensitivity")) {
                        st.nextToken();
                        setScrollSensitivity((int)st.nval);
                    }
                    else if (key.equals("getfocus")) {
                        st.nextToken();
                        getFocus = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("mechdisplaysmallfontsize")) {
                        st.nextToken();
                        mechDisplaySmallFontSize = (int)st.nval;
                    }
                    else if(key.equals("mechdisplaymediumfontsize")) {
                        st.nextToken();
                        mechDisplayMediumFontSize = (int)st.nval;
                    }
                    else if(key.equals("mechdisplaylargefontsize")) {
                        st.nextToken();
                        mechDisplayLargeFontSize = (int)st.nval;
                    }
                    else if(key.equals("mechdisplayarmorsmallfontsize")) {
                        st.nextToken();
                        mechDisplayArmorSmallFontSize = (int)st.nval;
                    }
                    else if(key.equals("mechdisplayarmormediumfontsize")) {
                        st.nextToken();
                        mechDisplayArmorMediumFontSize = (int)st.nval;
                    }
                    else if(key.equals("mechdisplayarmorlargefontsize")) {
                        st.nextToken();
                        mechDisplayArmorLargeFontSize = (int)st.nval;
                    }
                    else if (key.equals("keepserverlog")) {
                        st.nextToken();
                        keepServerlog = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("serverlogfilename")) {
                        st.nextToken();
                        serverlogFilename = st.sval;
                    }
                    else if(key.equals("serverlogmaxsize")) {
                        st.nextToken();
                        serverlogMaxSize = (int)st.nval;
                    }
                    else if(key.equals("chatloungetabs")) {
                        st.nextToken();
                        chatLoungeTabs = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if(key.equals("chatloungetabfontsize")) {
                        st.nextToken();
                        chatLoungeTabFontSize = (int)st.nval;
                    }
                    else if (key.equals("defaultautoejectdisabled")) {
                        st.nextToken();
                        defaultAutoejectDisabled = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else if (key.equals("showunitid")) {
                        st.nextToken();
                        showUnitId = Boolean.valueOf(st.sval).booleanValue();
                    }
                    else {
                        // Store the key and value in our saved settings.
                        st.nextToken();
                        String value = st.sval;
                        if ( null != value ) {
                            saved.put( key, new String(value) );
                        }
                    }
                }
            }
            
            cr.close();
        } catch(Exception e) {
            System.err.println("error reading settings file:");
//             System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Color loadColor(StreamTokenizer st, Color cDefault) {
        try {
            st.nextToken();
            if (st.ttype == StreamTokenizer.TT_NUMBER) {
                int red = (int)st.nval;
                st.nextToken();
                int green = (int)st.nval;
                st.nextToken();
                int blue = (int)st.nval;
                return new Color(red, green, blue);
            } else if (st.ttype == StreamTokenizer.TT_WORD) {
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
            
            cw.write(commentChar+" MegaMek config file" + "\r\n");
            cw.write(commentChar+" Edit at your own risk" + "\r\n");
            cw.write("\r\n");
            cw.write("windowpos " + windowPosX + " " + windowPosY + "\r\n");
            cw.write("windowsize " + windowSizeWidth + " " + windowSizeHeight + "\r\n");
            cw.write("minimapenabled " + minimapEnabled + "\r\n");
            cw.write("minimappos " + minimapPosX + " " + minimapPosY + "\r\n");
            cw.write("minimapzoom " + minimapZoom + "\r\n");
            cw.write("displaypos " + displayPosX + " " + displayPosY + "\r\n");
            cw.write("displaysize " + displaySizeWidth + " " + displaySizeHeight + "\r\n");
            cw.write("rulerpos " + rulerPosX + " " + rulerPosY + "\r\n");
            cw.write("rulersize " + rulerSizeWidth + " " + rulerSizeHeight + "\r\n");
            cw.write("rulercolors " + writeColor(rulerColor1) + " " + writeColor(rulerColor2) + "\r\n");
            cw.write("autoendfiring " + autoEndFiring + "\r\n");
            cw.write("nagformasc " + nagForMASC + "\r\n");
            cw.write("nagforpsr " + nagForPSR + "\r\n");
            cw.write("nagfornoaction " + nagForNoAction + "\r\n");
            cw.write("nagforreadme " + nagForReadme + "\r\n");
            cw.write("nagforbotreadme " + nagForBotReadme + "\r\n");
            cw.write("nagformapedreadme " + nagForMapEdReadme + "\r\n");
            cw.write("playername " + "\"" + escapeTokeniserChars(lastPlayerName) + "\"" + "\r\n");
            cw.write("server " + "\"" + lastServerPass + "\" " + lastServerPort + "\r\n");
            cw.write("connect " + "\"" + lastConnectAddr + "\" " + lastConnectPort + "\r\n");
            cw.write("maptext " + writeColor(mapTextColor) + "\r\n");
            cw.write("movedefault " + writeColor(moveDefaultColor) + "\r\n");
            cw.write("moverun " + writeColor(moveRunColor) + "\r\n");
            cw.write("movejump " + writeColor(moveJumpColor) + "\r\n");
            cw.write("moveillegal " + writeColor(moveIllegalColor) + "\r\n");
            cw.write("movemasc " + writeColor(moveMASCColor) + "\r\n");
            cw.write("maptileset \"" + mapTileset + "\"\r\n");
            cw.write("minimapcolours \"" + minimapColours + "\"\r\n");
            cw.write("showmovestep " + showMoveStep + "\r\n");
            cw.write("movestepdelay " + moveStepDelay + "\r\n");
            cw.write("showwrecks " + showWrecks + "\r\n");
            cw.write("soundmute " + soundMute + "\r\n");
            cw.write("soundbingfilename \"" + soundBingFilename + "\"\r\n");
            cw.write("memorydumpon " + memoryDumpOn + "\r\n");
            cw.write("minimumdialogsize " + minimumSizeWidth + " " + minimumSizeHeight + "\r\n");
            cw.write("mapzoomindex " + mapZoomIndex + "\r\n");
            if ( mekHitLocLog != null ) {
                mekHitLocLog.flush();
                mekHitLocLog.close();
                cw.write("mekhitloclog \"" + mekHitLocLogName + "\"\r\n");
            }
            cw.write("showmaphexpopup " + showMapHexPopup + "\r\n");
            cw.write("tooltipdelay " + tooltipDelay + "\r\n");
            cw.write("unitstartchar " + (int) unitStartChar + "\r\n");
            cw.write("maxpathfindertime " + maxPathfinderTime + "\r\n" );

            cw.write("rightdragscroll " + rightDragScroll + "\r\n");
            cw.write("alwaysrightclickscroll " + alwaysRightClickScroll + "\r\n");
            cw.write("ctlscroll " + ctlScroll + "\r\n");
            cw.write("clickedgescroll " + clickEdgeScroll + "\r\n");
            cw.write("autoedgescroll " + autoEdgeScroll + "\r\n");
            cw.write("scrollsensitivity " + scrollSensitivity + "\r\n");

            cw.write("getfocus " + getFocus + "\r\n");
            cw.write("mechdisplaysmallfontsize " + mechDisplaySmallFontSize + "\r\n");
            cw.write("mechdisplaymediumfontsize " + mechDisplayMediumFontSize + "\r\n");
            cw.write("mechdisplaylargefontsize " + mechDisplayLargeFontSize + "\r\n");
            cw.write("mechdisplayarmorsmallfontsize " + mechDisplayArmorSmallFontSize + "\r\n");
            cw.write("mechdisplayarmormediumfontsize " + mechDisplayArmorMediumFontSize + "\r\n");
            cw.write("mechdisplayarmorlargefontsize " + mechDisplayArmorLargeFontSize + "\r\n");

            cw.write("keepserverlog " + keepServerlog + "\r\n");
            cw.write("serverlogfilename " + serverlogFilename + "\r\n");
            cw.write("serverlogmaxsize " + serverlogMaxSize + "\r\n");

            cw.write("chatloungetabs " + chatLoungeTabs + "\r\n");
            cw.write("chatloungetabfontsize " + chatLoungeTabFontSize + "\r\n");
            cw.write("defaultautoejectdisabled " + defaultAutoejectDisabled + "\r\n");
            cw.write("showunitid " + showUnitId + "\r\n");
            
            // Store all of our "saved" settings.
            // Need to enclose "/" and "." in quotes
            Enumeration keys = Settings.saved.propertyNames();
            while ( keys.hasMoreElements() ) {
                final String key = keys.nextElement().toString();
                final String value = Settings.saved.getProperty( key );
                boolean escapeValue = ( value.indexOf('/') > -1 ||
                                        value.indexOf('.') > -1 );
                cw.write( key );
                cw.write( " " );
                if ( escapeValue ) cw.write( "\"" );
                cw.write( Settings.saved.getProperty(key) );
                if ( escapeValue ) cw.write( "\"" );
                cw.write( "\r\n" );
            }

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

    /**
     * Get the singleton <code>Settings</code> object.
     *
     * @return  The singleton <code>Settings</code> object.
     */
    public static Settings getInstance() {
        return Settings.instance;
    }

    /**
     * Get the value for the named setting.
     *
     * @param   name the <code>String</code> name of the setting whose
     *          value is needed.  This value may be <code>null</code>.
     * @return  the <code>String</code> value for the named setting.
     *          This value may be <code>null</code>.
     */
    public String get( String name ) {
        String value = null;

        // null in, null out.
        if ( null != name ) {
            // If we have a runtime override for the named setting, use it;
            // otherwise use the saved value (which may have a system default).
            value = Settings.runtime.getProperty( name, null );
            if ( null == value ) {
                value = Settings.saved.getProperty( name );
            }
        }
        return value;
    }

    /**
     * Get the value for the named setting.
     *
     * @param   name the <code>String</code> name of the setting whose
     *          value is needed.  This value may be <code>null</code>.
     * @param   defaultValue the default <code>String</code> value of the
     *          named setting.  This value will be returned if no other
     *          value is available.  This value may be <code>null</code>.
     * @return  the <code>String</code> value for the named setting.
     *          This value may be <code>null</code>.
     */
    public String get( String name, String defaultValue ) {
        String value = null;

        // Try to get the setting.  Use the default if we can't.
        value = this.get( name );
        if ( null == value ) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Set a value for the named setting.  Setting names should include the
     * name of the class (or at least package) that they are used in.  If
     * a setting name does not include a class or package name, then it is
     * assumed to be global to the MegaMek system, and may be set or used by
     * any component in the system.
     * <p/>
     * Values set at runtime will override any saved value for a setting.  All
     * values set by this method will be included in the next save.
     *
     * @param   name the <code>String</code> name of the setting whose
     *          value is needed.  This value may not be <code>null</code>.
     * @param   value the <code>String</code> value to set for the named
     *          setting.  This value may not be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> is thrown if a
     *          <code>null</code> setting name or value is passed.
     */
    public void set( String name, String value ) {
        // Validate input.
        if ( null == name ) {
            throw new IllegalArgumentException
                ( "Settings passed a null name." );
        }
        if ( null == value ) {
            throw new IllegalArgumentException
                ( "Settings passed a null value." );
        }

        // Update the value of any runtime override for the named setting.
        if ( Settings.runtime.contains( name ) ) {
            Settings.runtime.put( name, value );
        }

        // Update the value to be saved for the named setting.
        Settings.saved.put( name, value );
    }

    protected static String escapeTokeniserChars(String string) {
        String escapedString = new String();
        for (int i=0; i < string.length(); i++) {
            if (string.charAt(i) == quoteChar) {
                escapedString = escapedString.concat(""+'\\');
            };
            escapedString = escapedString.concat(""+string.charAt(i));
        };
        return escapedString;
    };
    
    public static void setScrollSensitivity(int sensitivity) {
        if (sensitivity > 0) {
            scrollSensitivity = sensitivity;
        } else {
            scrollSensitivity = 1;
        };
    };
    public static int getScrollSensitivity() {
        return scrollSensitivity;
    };

}
