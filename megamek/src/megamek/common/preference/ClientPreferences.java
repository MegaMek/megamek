/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.preference;

import megamek.MMConstants;
import megamek.common.MovePath;
import megamek.common.util.LocaleParser;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

public class ClientPreferences extends PreferenceStoreProxy {
    //region Variable Declarations
    public static final String LAST_CONNECT_ADDR = "LastConnectAddr";
    public static final String LAST_CONNECT_PORT = "LastConnectPort";
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
    public static final String DEBUG_OUTPUT_ON = "DebugOutputOn";
    public static final String GAMELOG_KEEP = "KeepGameLog";
    public static final String GAMELOG_FILENAME = "GameLogFilename";
    public static final String STAMP_FILENAMES = "StampFilenames";
    public static final String STAMP_FORMAT = "StampFormat";
    public static final String SHOW_UNIT_ID = "ShowUnitId";
    public static final String UNIT_START_CHAR = "UnitStartChar";
    public static final String DEFAULT_AUTOEJECT_DISABLED = "DefaultAutoejectDisabled";
    public static final String USE_AVERAGE_SKILLS = "UseAverageSkills";
    public static final String GENERATE_NAMES = "GenerateNames";
    public static final String METASERVER_NAME = "MetaServerName";
    public static final String GOAL_PLAYERS = "GoalPlayers";
    public static final String GUI_NAME = "GUIName";
    public static final String PRINT_ENTITY_CHANGE = "PrintEntityChange";
    public static final String BOARD_WIDTH = "BoardWidth";
    public static final String BOARD_HEIGHT = "BoardHeight";
    public static final String MAP_WIDTH = "MapWidth";
    public static final String MAP_HEIGHT = "MapHeight";
    public static final String IP_ADDRESSES_IN_CHAT = "IPAddressesInChat";
    //endregion Variable Declarations
    
    //region Constructors
    public ClientPreferences(IPreferenceStore store) {
        this.store = store;
        store.setDefault(LAST_CONNECT_ADDR, MMConstants.LOCALHOST);
        store.setDefault(LAST_CONNECT_PORT, MMConstants.DEFAULT_PORT);
        store.setDefault(LAST_SERVER_PORT, MMConstants.DEFAULT_PORT);
        store.setDefault(MAP_TILESET, "saxarba.tileset");
        store.setDefault(MAX_PATHFINDER_TIME, MovePath.DEFAULT_PATHFINDER_TIME_LIMIT);
        store.setDefault(DATA_DIRECTORY, "data");
        store.setDefault(LOG_DIRECTORY, "logs");
        store.setDefault(MECH_DIRECTORY, store.getDefaultString(DATA_DIRECTORY) + File.separator + "mechfiles");
        store.setDefault(METASERVER_NAME, "https://api.megamek.org/servers/announce");
        store.setDefault(GAMELOG_KEEP, true);
        store.setDefault(GAMELOG_FILENAME, "gamelog.html");
        store.setDefault(STAMP_FORMAT, "_yyyy-MM-dd_HH-mm-ss");
        store.setDefault(UNIT_START_CHAR, 'A');
        store.setDefault(GUI_NAME, "swing");
        store.setDefault(USE_AVERAGE_SKILLS, true);
        store.setDefault(GENERATE_NAMES, true);
        store.setDefault(PRINT_ENTITY_CHANGE, false);
        store.setDefault(BOARD_WIDTH, 16);
        store.setDefault(BOARD_HEIGHT, 17);
        store.setDefault(MAP_WIDTH, 1);
        store.setDefault(MAP_HEIGHT, 1);
        store.setDefault(DEBUG_OUTPUT_ON, false);
        store.setDefault(MEMORY_DUMP_ON, false);
        store.setDefault(IP_ADDRESSES_IN_CHAT, false);
        setLocale(store.getString(LOCALE));
        setMekHitLocLog();
    }
    //endregion Constructors

    public boolean getPrintEntityChange() {
        return store.getBoolean(PRINT_ENTITY_CHANGE);
    }

    @Override
    public String[] getAdvancedProperties() {
        return store.getAdvancedProperties();
    }

    public boolean defaultAutoejectDisabled() {
        return store.getBoolean(DEFAULT_AUTOEJECT_DISABLED);
    }

    public boolean useAverageSkills() {
        return store.getBoolean(USE_AVERAGE_SKILLS);
    }

    public boolean generateNames() {
        return store.getBoolean(GENERATE_NAMES);
    }

    public String getLastConnectAddr() {
        return store.getString(LAST_CONNECT_ADDR);
    }

    public int getLastConnectPort() {
        return store.getInt(LAST_CONNECT_PORT);
    }

    public String getLastPlayerName() {
        return store.getString(LAST_PLAYER_NAME);
    }

    public String getLastServerPass() {
        return store.getString(LAST_SERVER_PASS);
    }

    public int getLastServerPort() {
        return store.getInt(LAST_SERVER_PORT);
    }

    public String getMapTileset() {
        return store.getString(MAP_TILESET);
    }

    public int getMaxPathfinderTime() {
        return store.getInt(MAX_PATHFINDER_TIME);
    }

    public String getDataDirectory() {
        return store.getString(DATA_DIRECTORY);
    }

    public String getLogDirectory() {
        return store.getString(LOG_DIRECTORY);
    }

    public String getMechDirectory() {
        return store.getString(MECH_DIRECTORY);
    }

    protected PrintWriter mekHitLocLog = null;

    public PrintWriter getMekHitLocLog() {
        return mekHitLocLog;
    }

    public String getMetaServerName() {
        return store.getString(METASERVER_NAME);
    }

    public void setMetaServerName(String name) {
        store.setValue(METASERVER_NAME, name);
    }

    public int getGoalPlayers() {
        return store.getInt(GOAL_PLAYERS);
    }

    public void setGoalPlayers(int n) {
        store.setValue(GOAL_PLAYERS, n);
    }

    public String getGameLogFilename() {
        return store.getString(GAMELOG_FILENAME);
    }

    public boolean stampFilenames() {
        return store.getBoolean(STAMP_FILENAMES);
    }

    public String getStampFormat() {
        return store.getString(STAMP_FORMAT);
    }

    public boolean getShowUnitId() {
        return store.getBoolean(SHOW_UNIT_ID);
    }

    public char getUnitStartChar() {
        return (char) store.getInt(UNIT_START_CHAR);
    }

    public boolean keepGameLog() {
        return store.getBoolean(GAMELOG_KEEP);
    }

    public boolean memoryDumpOn() {
        return store.getBoolean(MEMORY_DUMP_ON);
    }

    public boolean debugOutputOn() {
        return store.getBoolean(DEBUG_OUTPUT_ON);
    }

    public void setDefaultAutoejectDisabled(boolean state) {
        store.setValue(DEFAULT_AUTOEJECT_DISABLED, state);
    }

    public void setUseAverageSkills(boolean state) {
        store.setValue(USE_AVERAGE_SKILLS, state);
    }

    public void setGenerateNames(boolean state) {
        store.setValue(GENERATE_NAMES, state);
    }

    public void setKeepGameLog(boolean state) {
        store.setValue(GAMELOG_KEEP, state);
    }

    public void setLastConnectAddr(String serverAddr) {
        store.setValue(LAST_CONNECT_ADDR, serverAddr);
    }

    public void setLastConnectPort(int port) {
        store.setValue(LAST_CONNECT_PORT, port);
    }

    public void setLastPlayerName(String name) {
        store.setValue(LAST_PLAYER_NAME, name);
    }

    public void setLastServerPass(String serverPass) {
        store.setValue(LAST_SERVER_PASS, serverPass);
    }

    public void setLastServerPort(int port) {
        store.setValue(LAST_SERVER_PORT, port);
    }

    public void setMapTileset(String name) {
        store.setValue(MAP_TILESET, name);
    }

    public void setMaxPathfinderTime(int i) {
        store.setValue(MAX_PATHFINDER_TIME, i);
    }

    public void setGameLogFilename(String name) {
        store.setValue(GAMELOG_FILENAME, name);
    }

    public void setPrintEntityChange(boolean print) {
        store.setValue(PRINT_ENTITY_CHANGE, print);
    }

    public void setStampFilenames(boolean state) {
        store.setValue(STAMP_FILENAMES, state);
    }

    public void setStampFormat(String format) {
        store.setValue(STAMP_FORMAT, format);
    }

    public void setShowUnitId(boolean state) {
        store.setValue(SHOW_UNIT_ID, state);
    }

    public void setUnitStartChar(char c) {
        store.setValue(UNIT_START_CHAR, c);
    }

    public String getGUIName() {
        return store.getString(GUI_NAME);
    }

    public void setGUIName(String guiName) {
        store.setValue(GUI_NAME, guiName);
    }

    public boolean getShowIPAddressesInChat() {
        return store.getBoolean(IP_ADDRESSES_IN_CHAT);
    }

    public void setShowIPAddressesInChat(boolean value) {
        store.setValue(IP_ADDRESSES_IN_CHAT, value);
    }

    protected Locale locale = null;

    public void setLocale(String l) {
        LocaleParser p = new LocaleParser();
        if (!p.parse(l)) {
            locale = new Locale(p.getLanguage(), p.getCountry(), p.getVariant());
            store.setValue(LOCALE, getLocaleString());
        }
    }

    public Locale getLocale() {
        if (locale == null) {
            return Locale.US;
        }
        return locale;
    }

    public String getLocaleString() {
        if (locale == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        if (!locale.getLanguage().isBlank()) {
            result.append(locale.getLanguage());
            if (!locale.getCountry().isBlank()) {
                result.append("_").append(locale.getCountry());
                if (!locale.getVariant().isBlank()) {
                    result.append("_").append(locale.getVariant());
                }
            }
        }
        return result.toString();
    }

    protected void setMekHitLocLog() {
        String name = store.getString(MEK_HIT_LOC_LOG);
        if (!name.isEmpty()) {
            try {
                mekHitLocLog = new PrintWriter(new BufferedWriter(new FileWriter(name)));
                mekHitLocLog.println("Table\tSide\tRoll");
            } catch (Throwable t) {
                LogManager.getLogger().error("", t);
                mekHitLocLog = null;
            }
        }
    }

    public int getBoardWidth() {
        return store.getInt(BOARD_WIDTH);
    }

    public int getBoardHeight() {
        return store.getInt(BOARD_HEIGHT);
    }

    public int getMapWidth() {
        return store.getInt(MAP_WIDTH);
    }

    public int getMapHeight() {
        return store.getInt(MAP_HEIGHT);
    }
}
