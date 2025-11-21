/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.preference;

import static megamek.client.bot.princess.BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Objects;

import megamek.MMConstants;
import megamek.common.Configuration;
import megamek.common.moves.MovePath;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

public class ClientPreferences extends PreferenceStoreProxy {
    private static final MMLogger logger = MMLogger.create(ClientPreferences.class);

    // region Variable Declarations
    public static final String LAST_CONNECT_ADDR = "LastConnectAddr";
    public static final String LAST_CONNECT_PORT = "LastConnectPort";
    public static final String LAST_PLAYER_NAME = "LastPlayerName";
    public static final String LAST_SERVER_PASS = "LastServerPass";
    public static final String LAST_SERVER_PORT = "LastServerPort";
    public static final String LOCALE = "Locale";
    public static final String MAP_TILESET = "MapTileset";
    public static final String MINIMAP_THEME = "MinimapTheme";
    public static final String STRATEGIC_VIEW_THEME = "StrategicViewTheme";
    public static final String MAX_PATHFINDER_TIME = "MaxPathfinderTime";
    public static final String DATA_DIRECTORY = "DataDirectory";
    public static final String LOG_DIRECTORY = "LogDirectory";
    public static final String MEK_DIRECTORY = "MekDirectory";
    public static final String MEK_HIT_LOC_LOG = "MekHitLocLog";
    public static final String MEMORY_DUMP_ON = "MemoryDumpOn";
    public static final String DEBUG_OUTPUT_ON = "DebugOutputOn";
    public static final String GAME_LOG_KEEP = "KeepGameLog";
    public static final String GAME_LOG_FILENAME = "GameLogFilename";
    public static final String AUTO_RESOLVE_GAME_LOG_FILENAME = "AutoResolveGameLogFilename";
    public static final String STAMP_FILENAMES = "StampFilenames";
    public static final String DATA_LOGGING = "GameDatasetLogging";
    public static final String STAMP_FORMAT = "StampFormat";
    public static final String SHOW_UNIT_ID = "ShowUnitId";
    public static final String UNIT_START_CHAR = "UnitStartChar";
    public static final String DEFAULT_AUTO_EJECT_DISABLED = "DefaultAutoejectDisabled";
    public static final String USE_AVERAGE_SKILLS = "UseAverageSkills";
    public static final String USE_GP_IN_UNIT_SELECTION = "UseGPinUnitSelection";
    public static final String GENERATE_NAMES = "GenerateNames";
    public static final String METASERVER_NAME = "MetaServerName";
    public static final String GOAL_PLAYERS = "GoalPlayers";
    public static final String GUI_NAME = "GUIName";
    public static final String PRINT_ENTITY_CHANGE = "PrintEntityChange";
    public static final String BOARD_WIDTH = "BoardWidth";
    public static final String BOARD_HEIGHT = "BoardHeight";
    public static final String MAP_WIDTH = "MapWidth";
    public static final String MAP_HEIGHT = "MapHeight";
    public static final String REPORT_KEYWORDS = "ReportKeywords";
    private static final String REPORT_KEYWORDS_DEFAULTS = "Needs\nRolls\nTakes\nHit\nFalls\nSkill Roll\nPilot "
          + "Skill\nPhase\nDestroyed\nDamage";
    public static final String REPORT_FILTER_KEYWORDS = "ReportFilterKeywords";
    private static final String REPORT_FILTER_KEYWORDS_DEFAULTS = "Fire Hit Damage\nHit Damage";
    public static final String IP_ADDRESSES_IN_CHAT = "IPAddressesInChat";
    public static final String START_SEARCHLIGHTS_ON = "StartSearchlightsOn";
    public static final String SPRITES_ONLY = "SpritesOnly";
    public static final String ENABLE_EXPERIMENTAL_BOT_FEATURES = "EnableExperimentalBotFeatures";
    public static final String NAG_ASK_FOR_VICTORY_LIST = "AskForVictoryList";
    public static final String SHOW_AUTO_RESOLVE_PANEL = "ShowAutoResolvePanel";
    public static final String FAVORITE_PRINCESS_BEHAVIOR_SETTING = "FavoritePrincessBehaviorSetting";
    public static final String LAST_SCENARIO = "LastScenario";

    /**
     * A user-specified directory, typically outside the MM directory, where content may be loaded from.
     */
    public static final String USER_DIR = "UserDir";

    public static final String MML_PATH = "MmlPath";

    // endregion Variable Declarations

    // region Constructors
    public ClientPreferences(IPreferenceStore store) {
        this.store = store;
        store.setDefault(LAST_CONNECT_ADDR, MMConstants.LOCALHOST);
        store.setDefault(LAST_CONNECT_PORT, MMConstants.DEFAULT_PORT);
        store.setDefault(LAST_SERVER_PORT, MMConstants.DEFAULT_PORT);
        store.setDefault(MAP_TILESET, "saxarba.tileset");
        store.setDefault(MINIMAP_THEME, "default.theme");
        store.setDefault(STRATEGIC_VIEW_THEME, "gbc green.theme");
        store.setDefault(MAX_PATHFINDER_TIME, MovePath.DEFAULT_PATHFINDER_TIME_LIMIT);
        store.setDefault(DATA_DIRECTORY, "data");
        store.setDefault(LOG_DIRECTORY, "logs");
        store.setDefault(MEK_DIRECTORY, store.getDefaultString(DATA_DIRECTORY) + File.separator + "mekfiles");
        store.setDefault(METASERVER_NAME, "https://api.megamek.org/servers/announce");
        store.setDefault(GAME_LOG_KEEP, true);
        store.setDefault(GAME_LOG_FILENAME, "gamelog.html");
        store.setDefault(AUTO_RESOLVE_GAME_LOG_FILENAME, "simulation.html");
        store.setDefault(STAMP_FORMAT, "_yyyy-MM-dd_HH-mm-ss");
        store.setDefault(UNIT_START_CHAR, 'A');
        store.setDefault(GUI_NAME, "swing");
        store.setDefault(USE_AVERAGE_SKILLS, true);
        store.setDefault(USE_GP_IN_UNIT_SELECTION, false);
        store.setDefault(GENERATE_NAMES, true);
        store.setDefault(PRINT_ENTITY_CHANGE, false);
        store.setDefault(BOARD_WIDTH, 16);
        store.setDefault(BOARD_HEIGHT, 17);
        store.setDefault(MAP_WIDTH, 1);
        store.setDefault(MAP_HEIGHT, 1);
        store.setDefault(DEBUG_OUTPUT_ON, false);
        store.setDefault(MEMORY_DUMP_ON, false);
        store.setDefault(REPORT_KEYWORDS, REPORT_KEYWORDS_DEFAULTS);
        store.setDefault(REPORT_FILTER_KEYWORDS, REPORT_FILTER_KEYWORDS_DEFAULTS);
        store.setDefault(IP_ADDRESSES_IN_CHAT, false);
        store.setDefault(START_SEARCHLIGHTS_ON, true);
        store.setDefault(SPRITES_ONLY, false);
        store.setDefault(ENABLE_EXPERIMENTAL_BOT_FEATURES, false);
        store.setDefault(USER_DIR, "");
        store.setDefault(MML_PATH, "");
        store.setDefault(NAG_ASK_FOR_VICTORY_LIST, true);
        store.setDefault(DATA_LOGGING, true);
        store.setDefault(SHOW_AUTO_RESOLVE_PANEL, false);
        store.setDefault(STAMP_FILENAMES, false);
        store.setDefault(FAVORITE_PRINCESS_BEHAVIOR_SETTING, DEFAULT_BEHAVIOR_DESCRIPTION);
        store.setDefault(LAST_SCENARIO, "");

        setLocale(store.getString(LOCALE));
        setMekHitLocLog();
    }
    // endregion Constructors

    public boolean getPrintEntityChange() {
        return store.getBoolean(PRINT_ENTITY_CHANGE);
    }

    @Override
    public String[] getAdvancedProperties() {
        return store.getAdvancedProperties();
    }

    public boolean defaultAutoEjectDisabled() {
        return store.getBoolean(DEFAULT_AUTO_EJECT_DISABLED);
    }

    public boolean useAverageSkills() {
        return store.getBoolean(USE_AVERAGE_SKILLS);
    }

    public boolean useGPinUnitSelection() {
        return store.getBoolean(USE_GP_IN_UNIT_SELECTION);
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

    public String getMekDirectory() {
        return store.getString(MEK_DIRECTORY);
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
        return store.getString(GAME_LOG_FILENAME);
    }

    public String getAutoResolveGameLogFilename() {
        return store.getString(AUTO_RESOLVE_GAME_LOG_FILENAME);
    }

    public boolean dataLoggingEnabled() {
        return store.getBoolean(DATA_LOGGING);
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
        return store.getBoolean(GAME_LOG_KEEP);
    }

    public boolean memoryDumpOn() {
        return store.getBoolean(MEMORY_DUMP_ON);
    }

    public boolean debugOutputOn() {
        return store.getBoolean(DEBUG_OUTPUT_ON);
    }

    public void setDefaultAutoEjectDisabled(boolean state) {
        store.setValue(DEFAULT_AUTO_EJECT_DISABLED, state);
    }

    public void setUseAverageSkills(boolean state) {
        store.setValue(USE_AVERAGE_SKILLS, state);
    }

    public void setUseGpInUnitSelection(boolean state) {
        store.setValue(USE_GP_IN_UNIT_SELECTION, state);
    }

    public void setGenerateNames(boolean state) {
        store.setValue(GENERATE_NAMES, state);
    }

    public void setKeepGameLog(boolean state) {
        store.setValue(GAME_LOG_KEEP, state);
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
        store.setValue(GAME_LOG_FILENAME, name);
    }

    public void setAutoResolveGameLogFilename(String name) {
        store.setValue(AUTO_RESOLVE_GAME_LOG_FILENAME, name);
    }

    public void setPrintEntityChange(boolean print) {
        store.setValue(PRINT_ENTITY_CHANGE, print);
    }

    public void setStampFilenames(boolean state) {
        store.setValue(STAMP_FILENAMES, state);
    }

    public void setDataLogging(boolean state) {
        store.setValue(DATA_LOGGING, state);
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

    public String getReportKeywords() {
        return store.getString(REPORT_KEYWORDS);
    }

    public void setReportKeywords(String s) {
        store.setValue(REPORT_KEYWORDS, s);
    }

    public String getReportFilterKeywords() {
        return store.getString(REPORT_FILTER_KEYWORDS);
    }

    public void setReportFilterKeywords(String s) {
        store.setValue(REPORT_FILTER_KEYWORDS, s);
    }

    public boolean getShowIPAddressesInChat() {
        return store.getBoolean(IP_ADDRESSES_IN_CHAT);
    }

    public void setShowIPAddressesInChat(boolean value) {
        store.setValue(IP_ADDRESSES_IN_CHAT, value);
    }

    public boolean getStartSearchlightsOn() {
        return store.getBoolean(START_SEARCHLIGHTS_ON);
    }

    public void setStartSearchlightsOn(boolean value) {
        store.setValue(START_SEARCHLIGHTS_ON, value);
    }

    public boolean getSpritesOnly() {
        return store.getBoolean(SPRITES_ONLY);
    }

    public void setSpritesOnly(boolean value) {
        store.setValue(SPRITES_ONLY, value);
    }

    public void setEnableExperimentalBotFeatures(boolean value) {
        store.setValue(ENABLE_EXPERIMENTAL_BOT_FEATURES, value);
    }

    public boolean getEnableExperimentalBotFeatures() {
        return store.getBoolean(ENABLE_EXPERIMENTAL_BOT_FEATURES);
    }

    public void setStrategicViewTheme(String theme) {
        store.setValue(STRATEGIC_VIEW_THEME, theme);
    }

    public File getStrategicViewTheme() {
        return new MegaMekFile(Configuration.minimapThemesDir(), store.getString(STRATEGIC_VIEW_THEME)).getFile();
    }

    public void setMinimapTheme(String theme) {
        if (theme == null) {
            return;
        }
        store.setValue(MINIMAP_THEME, theme);
    }

    public File getMinimapTheme() {
        return new MegaMekFile(Configuration.minimapThemesDir(), store.getString(MINIMAP_THEME)).getFile();
    }

    protected Locale locale = null;

    public void setLocale(String l) {
        locale = new Locale(l);
        store.setValue(LOCALE, getLocaleString());
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
                logger.error("", t);
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

    /**
     * @return The absolute user directory path (usually outside of MM). Does not end in a slash or backslash.
     */
    public String getUserDir() {
        return store.getString(USER_DIR);
    }

    public void setUserDir(String userDir) {
        // remove directory separators at the end
        while (!userDir.isBlank() && (userDir.endsWith("/") || userDir.endsWith("\\"))) {
            userDir = userDir.substring(0, userDir.length() - 1);
        }
        store.setValue(USER_DIR, userDir);
    }

    public String getMmlPath() {
        return store.getString(MML_PATH);
    }

    public void setMmlPath(String mmlPath) {
        store.setValue(MML_PATH, mmlPath.isBlank() ? "" : new File(mmlPath).getAbsolutePath());
    }

    public boolean askForVictoryList() {
        return store.getBoolean(NAG_ASK_FOR_VICTORY_LIST);
    }

    public void setAskForVictoryList(boolean value) {
        store.setValue(NAG_ASK_FOR_VICTORY_LIST, value);
    }

    public void setShowAutoResolvePanel(boolean value) {
        store.setValue(SHOW_AUTO_RESOLVE_PANEL, value);
    }

    public boolean getShowAutoResolvePanel() {
        return store.getBoolean(SHOW_AUTO_RESOLVE_PANEL);
    }

    public String getFavoritePrincessBehaviorSetting() {
        return store.getString(FAVORITE_PRINCESS_BEHAVIOR_SETTING);
    }

    public void setFavoritePrincessBehaviorSetting(String name) {
        store.setValue(FAVORITE_PRINCESS_BEHAVIOR_SETTING, name);
    }

    public void setLastScenario(String scenario) {
        store.setValue(LAST_SCENARIO, scenario);
    }

    public String getLastScenario() {
        return Objects.requireNonNullElse(store.getString(LAST_SCENARIO), "");
    }
}
