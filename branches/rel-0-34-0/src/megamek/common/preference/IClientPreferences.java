/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.preference;

import java.io.PrintWriter;
import java.util.Locale;

/**
 * Interface for common client settings store
 */
public interface IClientPreferences extends IPreferenceStore {

    public static final String LAST_CONNECT_ADDR = "LastConnectAddr";
    public static final String LAST_CONNECT_PORT = "LastConnectPort";
    public static final String LAST_PLAYER_CAMO_NAME = "LastPlayerCamoName";
    public static final String LAST_PLAYER_CATEGORY = "LastPlayerCategory";
    public static final String LAST_PLAYER_COLOR = "LastPlayerColor";
    public static final String LAST_PLAYER_NAME = "LastPlayerName";
    public static final String LAST_SERVER_PASS = "LastServerPass";
    public static final String LAST_SERVER_PORT = "LastServerPort";
    public static final String LOCALE = "Locale";
    public static final String MAP_TILESET = "MapTileset";
    public static final String MAX_PATHFINDER_TIME = "MaxPathfinderTime";
    public static final String DATA_DIRECTORY = "DataDirectory";
    public static final String LOG_DIRECTORY = "LogDirectory";
    public static final String MECH_DIRECTORY = "MechDirectory";
    public static final String MEK_HIT_LOC_LOG = "MekHitLocLog";
    public static final String MEMORY_DUMP_ON = "MemoryDumpOn";
    public static final String GAMELOG_KEEP = "KeepGameLog";
    public static final String GAMELOG_FILENAME = "GameLogFilename";
    // public static final String GAMELOG_MAX_SIZE = "GameLogMaxSize";
    public static final String STAMP_FILENAMES = "StampFilenames";
    public static final String STAMP_FORMAT = "StampFormat";
    public static final String SHOW_UNIT_ID = "ShowUnitId";
    public static final String UNIT_START_CHAR = "UnitStartChar";
    public static final String DEFAULT_AUTOEJECT_DISABLED = "DefaultAutoejectDisabled";
    public static final String USE_AVERAGE_SKILLS = "UseAverageSkills";
    public static final String METASERVER_NAME = "MetaServerName";
    public static final String GOAL_PLAYERS = "GoalPlayers";
    public static final String GUI_NAME = "GUIName";
    public static final String PRINT_ENTITY_CHANGE = "PrintEntityChange";
    public static final String BOARD_WIDTH = "BoardWidth";
    public static final String BOARD_HEIGHT = "BoardHeight";
    public static final String MAP_WIDTH = "MapWidth";
    public static final String MAP_HEIGHT = "MapHeight";

    boolean getPrintEntityChange();

    boolean defaultAutoejectDisabled();

    boolean useAverageSkills();

    String getLastConnectAddr();

    int getLastConnectPort();

    String getLastPlayerName();

    String getLastServerPass();

    int getLastServerPort();

    Locale getLocale();

    String getLocaleString();

    String getMapTileset();

    int getMaxPathfinderTime();

    String getDataDirectory();

    String getLogDirectory();

    String getMechDirectory();

    PrintWriter getMekHitLocLog();

    String getMetaServerName();

    void setMetaServerName(String name);

    int getGoalPlayers();

    void setGoalPlayers(int n);

    String getGameLogFilename();

    // int getGameLogMaxSize();

    boolean stampFilenames();

    String getStampFormat();

    boolean getShowUnitId();

    char getUnitStartChar();

    boolean keepGameLog();

    boolean memoryDumpOn();

    void setDefaultAutoejectDisabled(boolean state);

    void setUseAverageSkills(boolean state);

    void setKeepGameLog(boolean state);

    void setLastConnectAddr(String serverAddr);

    void setLastConnectPort(int port);

    void setLastPlayerCamoName(String camoFileName);

    void setLastPlayerCategory(String camoCategory);

    void setLastPlayerColor(int colorIndex);

    void setLastPlayerName(String name);

    void setLastServerPass(String serverPass);

    void setLastServerPort(int port);

    void setLocale(String text);

    void setMapTileset(String filename);

    void setMaxPathfinderTime(int i);

    void setGameLogFilename(String text);

    // void setGameLogMaxSize(int i);

    void setStampFilenames(boolean state);

    void setStampFormat(String text);

    void setShowUnitId(boolean state);

    void setUnitStartChar(char c);

    String getGUIName();

    void setGUIName(String guiName);

    int getBoardWidth();

    int getBoardHeight();

    int getMapWidth();

    int getMapHeight();

}
