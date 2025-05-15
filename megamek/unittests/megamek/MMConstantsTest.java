/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * There really isn't a point to these tests other than... to boost coverage. Seriously. CodeCov will complain about
 * lack of coverage for these items so just building the test so it shuts up.
 *
 * @author rjhancock
 * @since 0.50.06
 */
class MMConstantsTest {

    @Test
    void testProjectName() {
        assertEquals("MegaMek", MMConstants.PROJECT_NAME);
    }

    @Test
    void testMulUrlPrefix() {
        assertEquals("http://www.masterunitlist.info/Unit/Details/", MMConstants.MUL_URL_PREFIX);
    }

    @Test
    void testBtUrlShrapnel() {
        assertEquals("https://bg.battletech.com/shrapnel/", MMConstants.BT_URL_SHRAPNEL);
    }

    @Test
    void testBtUrlShrapnelPrefix() {
        assertEquals("Shrapnel", MMConstants.SOURCE_TEXT_SHRAPNEL);
    }

    @Test
    void testNagMode() {
        assertEquals("megamek/prefs/nags", MMConstants.NAG_NODE);
    }

    @Test
    void testNagBotReadme() {
        assertEquals("nagBotReadme", MMConstants.NAG_BOT_README);
    }

    @Test
    void testNameFactionsDirectoryPath() {
        assertEquals("data/names/factions", MMConstants.NAME_FACTIONS_DIRECTORY_PATH);
    }

    @Test
    void testCallsignFilePath() {
        assertEquals("data/names/callsigns.csv", MMConstants.CALLSIGN_FILE_PATH);
    }

    @Test
    void testGivenNameFemaleFilePath() {
        assertEquals("data/names/femaleGivenNames.csv", MMConstants.GIVEN_NAME_FEMALE_FILE);
    }

    @Test
    void testHistoricalEthnicityFilePath() {
        assertEquals("data/names/historicalEthnicity.csv", MMConstants.HISTORICAL_ETHNICITY_FILE);
    }

    @Test
    void testGivenNameMaleFilePath() {
        assertEquals("data/names/maleGivenNames.csv", MMConstants.GIVEN_NAME_MALE_FILE);
    }

    @Test
    void testSurnameFilePath() {
        assertEquals("data/names/surnames.csv", MMConstants.SURNAME_FILE);
    }

    @Test
    void testBotReadmeFilePath() {
        assertEquals("docs/Bot Stuff/Princess Notes.md", MMConstants.BOT_README_FILE_PATH);
    }

    @Test
    void testBoardReadmeFilePath() {
        assertEquals("docs/Archive Stuff/maps/Map Editor-readme.txt", MMConstants.BOARD_README_FILE_PATH);
    }

    @Test
    void testMegamekReadmeFilePath() {
        assertEquals("docs/README.md", MMConstants.MEGAMEK_README_FILE_PATH);
    }

    @Test
    void testUserDirReadmeFilePath() {
        assertEquals("docs/UserDirHelp.html", MMConstants.USER_DIR_README_FILE);
    }

    @Test
    void testUserNameFactionsDirectoryPath() {
        assertEquals("userdata/data/names/factions", MMConstants.USER_NAME_FACTIONS_DIRECTORY_PATH);
    }

    @Test
    void testUserCallsignFilePath() {
        assertEquals("userdata/data/names/callsigns.csv", MMConstants.USER_CALLSIGN_FILE_PATH);
    }

    @Test
    void testUserGivenNameFemaleFilePath() {
        assertEquals("userdata/data/names/femaleGivenNames.csv", MMConstants.USER_GIVEN_NAME_FEMALE_FILE);
    }

    @Test
    void testUserHistoricalEthnicityFilePath() {
        assertEquals("userdata/data/names/historicalEthnicity.csv", MMConstants.USER_HISTORICAL_ETHNICITY_FILE);
    }

    @Test
    void testUserGivenNameMaleFilePath() {
        assertEquals("userdata/data/names/maleGivenNames.csv", MMConstants.USER_GIVEN_NAME_MALE_FILE);
    }

    @Test
    void testUserSurnameFilePath() {
        assertEquals("userdata/data/names/surnames.csv", MMConstants.USER_SURNAME_FILE);
    }

    @Test
    void testErasFilePath() {
        assertEquals("data/universe/eras.xml", MMConstants.ERAS_FILE_PATH);
    }

    @Test
    void testUserLoadoutsDir() {
        assertEquals("userdata/data", MMConstants.USER_LOADOUTS_DIR);
    }

    @Test
    void testDefaultPort() {
        assertEquals(2346, MMConstants.DEFAULT_PORT);
    }

    @Test
    void testMinPort() {
        assertEquals(1, MMConstants.MIN_PORT);
    }

    @Test
    void testMinPortForQuickGame() {
        assertEquals(1024, MMConstants.MIN_PORT_FOR_QUICK_GAME);
    }

    @Test
    void testMaxPort() {
        assertEquals(65535, MMConstants.MAX_PORT);
    }

    @Test
    void testDefaultPlayerName() {
        assertEquals("Player", MMConstants.DEFAULT_PLAYERNAME);
    }

    @Test
    void testLocalhost() {
        assertEquals("localhost", MMConstants.LOCALHOST);
    }

    @Test
    void testLocalhostIp() {
        assertEquals("127.0.0.1", MMConstants.LOCALHOST_IP);
    }

    @Test
    void testSaveGameDir() {
        assertEquals("savegames", MMConstants.SAVEGAME_DIR);
    }

    @Test
    void testDefaultSaveGameName() {
        assertEquals("savegame.sav", MMConstants.DEFAULT_SAVEGAME_NAME);
    }

    @Test
    void testQuickSaveFile() {
        assertEquals("quicksave", MMConstants.QUICKSAVE_FILE);
    }

    @Test
    void testQuickSavePath() {
        assertEquals("./savegames", MMConstants.QUICKSAVE_PATH);
    }

    @Test
    void testSafeFileExt() {
        assertEquals(".sav", MMConstants.SAVE_FILE_EXT);
    }

    @Test
    void testGzExt() {
        assertEquals(".gz", MMConstants.GZ_FILE_EXT);
    }

    @Test
    void testSafeFileGZExt() {
        assertEquals(".sav.gz", MMConstants.SAVE_FILE_GZ_EXT);
    }

    @Test
    void testDiveBombMinAltitude() {
        assertEquals(3, MMConstants.DIVE_BOMB_MIN_ALTITUDE);
    }

    @Test
    void testDiveBombMaxAltitude() {
        assertEquals(5, MMConstants.DIVE_BOMB_MAX_ALTITUDE);
    }

    @Test
    void testInfantryPrimaryWeaponDamageCap() {
        assertEquals(0.6, MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP);
    }

    @Test
    void testTSEMPTEffectNone() {
        assertEquals(0, MMConstants.TSEMP_EFFECT_NONE);
    }

    @Test
    void testTSEMPTEffectInterference() {
        assertEquals(1, MMConstants.TSEMP_EFFECT_INTERFERENCE);
    }

    @Test
    void testTSEMPTEffectShutdown() {
        assertEquals(2, MMConstants.TSEMP_EFFECT_SHUTDOWN);
    }

    @Test
    void testCLKeyFileExtensionBoard() {
        assertEquals(".board", MMConstants.CL_KEY_FILEEXTENTION_BOARD);
    }
}
