/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package megamek;

import java.nio.file.Paths;

/**
 * These are constants that hold across MegaMek.
 */
public final class MMConstants extends SuiteConstants {
    private MMConstants() {
        throw new IllegalStateException("MMConstants Utility Class");
    }

    // region General Constants
    public static final String PROJECT_NAME = "MegaMek";
    public static final String MUL_URL_PREFIX = "http://www.masterunitlist.info/Unit/Details/";
    public static final String BT_URL_SHRAPNEL = "https://bg.battletech.com/shrapnel/";
    /**
     * When this text is found in the source field, the Mek View will display a link
     * to {@link #BT_URL_SHRAPNEL}
     */
    public static final String SOURCE_TEXT_SHRAPNEL = "Shrapnel";
    // endregion General Constants

    // region GUI Constants
    // endregion GUI Constants

    // region MMOptions
    // region Nag Tab
    public static final String NAG_NODE = "megamek/prefs/nags";
    public static final String NAG_BOT_README = "nagBotReadme";
    // endregion Nag Tab
    // endregion MMOptions

    // region File Paths
    // This holds all required file paths not saved as part of MegaMek Options
    public static final String NAME_FACTIONS_DIRECTORY_PATH = Paths.get("data/names/factions/").toString();
    public static final String CALLSIGN_FILE_PATH = Paths.get("data/names/callsigns.csv").toString();
    public static final String GIVEN_NAME_FEMALE_FILE = Paths.get("data/names/femaleGivenNames.csv").toString();
    public static final String HISTORICAL_ETHNICITY_FILE = Paths.get("data/names/historicalEthnicity.csv").toString();
    public static final String GIVEN_NAME_MALE_FILE = Paths.get("data/names/maleGivenNames.csv").toString();
    public static final String SURNAME_FILE = Paths.get("data/names/surnames.csv").toString();
    public static final String BOT_README_FILE_PATH = Paths.get("docs/Bot Stuff/Princess Notes.txt").toString();
    public static final String BOARD_README_FILE_PATH = Paths.get("docs/Archive Stuff/maps/Map Editor-readme.txt").toString();
    public static final String MEGAMEK_README_FILE_PATH = Paths.get("docs/1-Readme/readme.txt").toString();
    public static final String USER_DIR_README_FILE = Paths.get("docs/UserDirHelp.html").toString();
    public static final String SERIALKILLER_CONFIG_FILE = Paths.get("mmconf/serialkiller.xml").toString();
    public static final String USER_NAME_FACTIONS_DIRECTORY_PATH = Paths.get("userdata/data/names/factions/").toString();
    public static final String USER_CALLSIGN_FILE_PATH = Paths.get("userdata/data/names/callsigns.csv").toString();
    public static final String USER_GIVEN_NAME_FEMALE_FILE = Paths.get("userdata/data/names/femaleGivenNames.csv").toString();
    public static final String USER_HISTORICAL_ETHNICITY_FILE = Paths.get("userdata/data/names/historicalEthnicity.csv").toString();
    public static final String USER_GIVEN_NAME_MALE_FILE = Paths.get("userdata/data/names/maleGivenNames.csv").toString();
    public static final String USER_SURNAME_FILE = Paths.get("userdata/data/names/surnames.csv").toString();
    public static final String ERAS_FILE_PATH = Paths.get("data/universe/eras.xml").toString();
    public static final String USER_LOADOUTS_DIR = Paths.get("userdata/data/").toString();
    // endregion File Paths

    // region ClientServer
    public static final int DEFAULT_PORT = 2346;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;
    public static final String DEFAULT_PLAYERNAME = "Player";
    public static final String LOCALHOST = "localhost";
    // endregion ClientServer

    // region SaveGame
    public static final String SAVEGAME_DIR = "savegames";
    public static final String DEFAULT_SAVEGAME_NAME = "savegame.sav";
    public static final String QUICKSAVE_FILE = "quicksave";
    public static final String QUICKSAVE_PATH = "./" + SAVEGAME_DIR;
    public static final String SAVE_FILE_EXT = ".sav";
    public static final String GZ_FILE_EXT = ".gz";
    public static final String SAVE_FILE_GZ_EXT = SAVE_FILE_EXT + GZ_FILE_EXT;
    // endregion SaveGame

    // region Unsorted Constants
    public static final int DIVE_BOMB_MIN_ALTITUDE = 3;
    public static final int DIVE_BOMB_MAX_ALTITUDE = 5;
    public static final double INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP = 0.6;
    // endregion Unsorted Constants

    // region Magic Numbers That Should Be Enums
    // FIXME : TSEMP Constants
    public static final int TSEMP_EFFECT_NONE = 0;
    public static final int TSEMP_EFFECT_INTERFERENCE = 1;
    public static final int TSEMP_EFFECT_SHUTDOWN = 2;
    // endregion Magic Numbers That Should Be Enums
}
