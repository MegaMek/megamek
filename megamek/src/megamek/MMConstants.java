/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

/**
 * These are constants that hold across MegaMek.
 */
public final class MMConstants extends SuiteConstants {
    //region General Constants
    public static final String PROJECT_NAME = "MegaMek";
    public static final String MUL_URL_PREFIX = "http://www.masterunitlist.info/Unit/Details/";
    //endregion General Constants

    //region GUI Constants
    //endregion GUI Constants

    //region MMOptions
    //endregion MMOptions

    //region File Paths
    // This holds all required file paths not saved as part of MegaMek Options
    public static final String NAME_FACTIONS_DIRECTORY_PATH = "data/names/factions/";
    public static final String CALLSIGN_FILE_PATH = "data/names/callsigns.csv";
    public static final String GIVEN_NAME_FEMALE_FILE = "data/names/femaleGivenNames.csv";
    public static final String HISTORICAL_ETHNICITY_FILE = "data/names/historicalEthnicity.csv";
    public static final String GIVEN_NAME_MALE_FILE = "data/names/maleGivenNames.csv";
    public static final String SURNAME_FILE = "data/names/surnames.csv";
    public static final String USER_NAME_FACTIONS_DIRECTORY_PATH = "userdata/data/names/factions/";
    public static final String USER_CALLSIGN_FILE_PATH = "userdata/data/names/callsigns.csv";
    public static final String USER_GIVEN_NAME_FEMALE_FILE = "userdata/data/names/femaleGivenNames.csv";
    public static final String USER_HISTORICAL_ETHNICITY_FILE = "userdata/data/names/historicalEthnicity.csv";
    public static final String USER_GIVEN_NAME_MALE_FILE = "userdata/data/names/maleGivenNames.csv";
    public static final String USER_SURNAME_FILE = "userdata/data/names/surnames.csv";
    public static final String SERIALKILLER_CONFIG_FILE = "mmconf/serialkiller.xml";
    //endregion File Paths

    //region ClientServer
    public static final int DEFAULT_PORT = 2346;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;
    public static final String DEFAULT_PLAYERNAME = "Player";
    public static final String LOCALHOST = "localhost";
    //endregion ClientServer

    //region SaveGame
    public static final String SAVEGAME_DIR = "savegames";
    public static final String DEFAULT_SAVEGAME_NAME = "savegame.sav";
    public static final String QUICKSAVE_FILE = "quicksave";
    public static final String QUICKSAVE_PATH = "./" + SAVEGAME_DIR;
    public static final String SAVE_FILE_EXT = ".sav";
    public static final String GZ_FILE_EXT = ".gz";
    public static final String SAVE_FILE_GZ_EXT = SAVE_FILE_EXT + GZ_FILE_EXT;
    //endregion SaveGame

    //region Unsorted Constants
    public static final int DIVE_BOMB_MIN_ALTITUDE = 3;
    public static final int DIVE_BOMB_MAX_ALTITUDE = 5;
    public static final double INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP = 0.6;
    //endregion Unsorted Constants

    //region Magic Numbers That Should Be Enums
    // FIXME : TSEMP Constants
    public static final int TSEMP_EFFECT_NONE = 0;
    public static final int TSEMP_EFFECT_INTERFERENCE = 1;
    public static final int TSEMP_EFFECT_SHUTDOWN = 2;
    //endregion Magic Numbers That Should Be Enums
}
