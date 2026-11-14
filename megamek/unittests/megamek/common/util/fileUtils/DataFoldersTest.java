/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.util.fileUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataFoldersTest {

    private static final String FACTIONS = "data/universe/factions";

    @TempDir
    Path tempDir;

    private String previousOverride;
    private ClientPreferences preferences;
    private DataFolders dataFolders;

    @BeforeEach
    void beforeEach() {
        previousOverride = System.getProperty(ClientPreferences.USER_DIR_PROPERTY);
        System.clearProperty(ClientPreferences.USER_DIR_PROPERTY);
        preferences = new ClientPreferences(new PreferenceStore());
        dataFolders = new DataFolders(preferences);
    }

    @AfterEach
    void afterEach() {
        if (previousOverride == null) {
            System.clearProperty(ClientPreferences.USER_DIR_PROPERTY);
        } else {
            System.setProperty(ClientPreferences.USER_DIR_PROPERTY, previousOverride);
        }
    }

    @Test
    void noFolderWhenNothingIsChosen() {
        assertEquals(DataFolders.Source.NOT_SET, dataFolders.myDataSource());
        assertNull(dataFolders.myData());
        assertTrue(dataFolders.configuredFolders().isEmpty());
        assertTrue(dataFolders.folders().isEmpty());
        assertTrue(dataFolders.subfolders(FACTIONS).isEmpty());
        assertNull(dataFolders.findFile(FACTIONS + "/FS.yml"));
    }

    @Test
    void savedSettingIsMyData() {
        Path saved = tempDir.resolve("saved");
        preferences.setUserDir(saved.toString());

        assertEquals(DataFolders.Source.SETTING, dataFolders.myDataSource());
        assertEquals(saved.toFile(), dataFolders.myData());
    }

    @Test
    void launcherWinsOverSavedSetting() {
        Path saved = tempDir.resolve("saved");
        Path launcher = tempDir.resolve("launcher");
        preferences.setUserDir(saved.toString());
        System.setProperty(ClientPreferences.USER_DIR_PROPERTY, launcher.toString());

        assertEquals(DataFolders.Source.LAUNCHER, dataFolders.myDataSource());
        assertEquals(launcher.toFile(), dataFolders.myData());
    }

    @Test
    void missingFolderIsConfiguredButNotListed() {
        Path missing = tempDir.resolve("missing");
        preferences.setUserDir(missing.toString());

        assertEquals(List.of(missing.toFile()), dataFolders.configuredFolders());
        assertTrue(dataFolders.folders().isEmpty());
        assertTrue(dataFolders.subfolders(FACTIONS).isEmpty());
    }

    @Test
    void existingFolderIsListed() throws IOException {
        Path myData = Files.createDirectory(tempDir.resolve("myData"));
        preferences.setUserDir(myData.toString());

        assertEquals(List.of(myData.toFile()), dataFolders.folders());
    }

    @Test
    void missingSubfolderIsSkippedQuietly() throws IOException {
        Path myData = Files.createDirectory(tempDir.resolve("myData"));
        preferences.setUserDir(myData.toString());

        assertTrue(dataFolders.subfolders(FACTIONS).isEmpty());
    }

    @Test
    void existingSubfolderIsListed() throws IOException {
        Path myData = Files.createDirectory(tempDir.resolve("myData"));
        Path factions = Files.createDirectories(myData.resolve(FACTIONS));
        preferences.setUserDir(myData.toString());

        assertEquals(List.of(factions.toFile()), dataFolders.subfolders(FACTIONS));
    }

    @Test
    void findFileReturnsOnlyExistingFiles() throws IOException {
        Path myData = Files.createDirectory(tempDir.resolve("myData"));
        Path logos = Files.createDirectories(myData.resolve("data/images/universe/factions"));
        Path logo = Files.createFile(logos.resolve("logo_FS.png"));
        preferences.setUserDir(myData.toString());

        assertEquals(logo.toFile(), dataFolders.findFile("data/images/universe/factions/logo_FS.png"));
        assertNull(dataFolders.findFile("data/images/universe/factions/logo_LA.png"));
        assertNull(dataFolders.findFile("data/images/universe/factions"));
    }
}
