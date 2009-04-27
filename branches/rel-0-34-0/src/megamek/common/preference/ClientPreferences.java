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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

import megamek.common.MovePath;
import megamek.common.util.LocaleParser;

class ClientPreferences extends PreferenceStoreProxy implements
        IClientPreferences {

    ClientPreferences(IPreferenceStore store) {
        this.store = store;
        store.setDefault(LAST_CONNECT_ADDR, "localhost");
        store.setDefault(LAST_CONNECT_PORT, 2346);
        store.setDefault(LAST_SERVER_PORT, 2346);
        store.setDefault(MAP_TILESET, "atmospheric.tileset");
        store.setDefault(MAX_PATHFINDER_TIME,
                MovePath.DEFAULT_PATHFINDER_TIME_LIMIT);
        store.setDefault(DATA_DIRECTORY, "data");
        store.setDefault(LOG_DIRECTORY, "logs");
        store.setDefault(MECH_DIRECTORY, store.getDefaultString(DATA_DIRECTORY)
                + File.separator + "mechfiles");
        store.setDefault(METASERVER_NAME,
                "http://www.damour.info/cgi-bin/james/metaserver");
        store.setDefault(GOAL_PLAYERS, 2);
        store.setDefault(GAMELOG_KEEP, true);
        store.setDefault(GAMELOG_FILENAME, "gamelog.txt");
        // store.setDefault(GAMELOG_MAX_SIZE, 1);
        store.setDefault(STAMP_FORMAT, "_yyyy-MM-dd_HH-mm-ss");
        store.setDefault(UNIT_START_CHAR, 'A');
        store.setDefault(GUI_NAME, "AWT");
        store.setDefault(USE_AVERAGE_SKILLS, true);
        store.setDefault(PRINT_ENTITY_CHANGE, false);
        store.setDefault(BOARD_WIDTH, 16);
        store.setDefault(BOARD_HEIGHT, 17);
        store.setDefault(MAP_WIDTH, 1);
        store.setDefault(MAP_HEIGHT, 1);
        setLocale(store.getString(LOCALE));
        setMekHitLocLog();
    }

    public boolean getPrintEntityChange() {
        return store.getBoolean(PRINT_ENTITY_CHANGE);
    }

    public String[] getAdvancedProperties() {
        return store.getAdvancedProperties();
    }

    public boolean defaultAutoejectDisabled() {
        return store.getBoolean(DEFAULT_AUTOEJECT_DISABLED);
    }

    public boolean useAverageSkills() {
        return store.getBoolean(USE_AVERAGE_SKILLS);
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

    // public int getGameLogMaxSize() {
    // return store.getInt(GAMELOG_MAX_SIZE);
    // }

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

    public void setDefaultAutoejectDisabled(boolean state) {
        store.setValue(DEFAULT_AUTOEJECT_DISABLED, state);
    }

    public void setUseAverageSkills(boolean state) {
        store.setValue(USE_AVERAGE_SKILLS, state);
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

    public void setLastPlayerCamoName(String camoFileName) {
        if (camoFileName != null) {
            store.setValue(LAST_PLAYER_CAMO_NAME, camoFileName);
        }
    }

    public void setLastPlayerCategory(String camoCategory) {
        store.setValue(LAST_PLAYER_CATEGORY, camoCategory);
    }

    public void setLastPlayerColor(int colorIndex) {
        store.setValue(LAST_PLAYER_COLOR, colorIndex);
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

    // public void setGameLogMaxSize(int i) {
    // store.setValue(GAMELOG_MAX_SIZE, i);
    // }

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
            // return Locale.getDefault();
            return Locale.US;
        }
        return locale;
    }

    public String getLocaleString() {
        if (locale == null)
            return "";
        StringBuffer result = new StringBuffer();
        if (locale.getLanguage().length() != 0) {
            result.append(locale.getLanguage());
            if (locale.getCountry().length() != 0) {
                result.append("_" + locale.getCountry());
                if (locale.getVariant().length() != 0) {
                    result.append("_" + locale.getVariant());
                }
            }
        }
        return result.toString();
    }

    protected void setMekHitLocLog() {
        String name = store.getString(MEK_HIT_LOC_LOG);
        if (name.length() != 0) {
            try {
                mekHitLocLog = new PrintWriter(new BufferedWriter(
                        new FileWriter(name)));
                mekHitLocLog.println("Table\tSide\tRoll");
            } catch (Throwable thrown) {
                thrown.printStackTrace();
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
