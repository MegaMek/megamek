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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import megamek.common.Configuration;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A program that saves a unit file and then looks it up by name, as a MekHQ refit does, needs to know beforehand
 * whether the unit list reads the folder it saves to (MegaMek/mekhq#11303).
 */
class MekSummaryCacheUnitFolderTest {
    @TempDir
    Path outsideFolder;

    private String savedUserDirectory;

    @BeforeEach
    void rememberUserDirectory() {
        savedUserDirectory = PreferenceManager.getClientPreferences().getUserDir();
    }

    @AfterEach
    void restoreUserDirectory() {
        PreferenceManager.getClientPreferences().setUserDir(savedUserDirectory);
    }

    private static File inUnitsFolder(String... names) {
        File file = Configuration.unitsDir();
        for (String name : names) {
            file = new File(file, name);
        }
        return file;
    }

    @Test
    void aFileInTheCustomsFolderIsRead() {
        assertTrue(MekSummaryCache.isInUnitListFolder(inUnitsFolder("customs", "Nightstar NSR-9J Mk II.mtf")));
    }

    @Test
    void aFileOutsideEveryUnitFolderIsNotRead() {
        File docsFile = outsideFolder.resolve("docs").resolve("Nightstar NSR-9J Mk II.mtf").toFile();

        assertFalse(MekSummaryCache.isInUnitListFolder(docsFile));
    }

    @Test
    void aPathThatStepsOutOfTheUnitsFolderIsNotRead() {
        assertFalse(MekSummaryCache.isInUnitListFolder(inUnitsFolder("..", "docs", "Nightstar NSR-9J Mk II.mtf")));
    }

    @Test
    void foldersTheUnitListSkipsAreNotRead() {
        assertFalse(MekSummaryCache.isInUnitListFolder(inUnitsFolder("unsupported", "Test.mtf")));
        assertFalse(MekSummaryCache.isInUnitListFolder(inUnitsFolder("customs", ".mml_tmp", "Test.mtf")));
        assertFalse(MekSummaryCache.isInUnitListFolder(inUnitsFolder(".svn", "Test.mtf")));
    }

    @Test
    void theUserDirectoryIsReadOnceItIsSet() {
        File userDirectoryFile = outsideFolder.resolve("My Units").resolve("Nightstar NSR-9J Mk II.mtf").toFile();
        assertTrue(userDirectoryFile.getParentFile().mkdirs());
        ClientPreferences clientPreferences = PreferenceManager.getClientPreferences();
        clientPreferences.setUserDir("");
        assertFalse(MekSummaryCache.isInUnitListFolder(userDirectoryFile), "Not read before the user directory is set");

        clientPreferences.setUserDir(outsideFolder.toString());

        assertTrue(MekSummaryCache.isInUnitListFolder(userDirectoryFile));
    }
}
