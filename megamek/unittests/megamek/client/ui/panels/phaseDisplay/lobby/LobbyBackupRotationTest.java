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
package megamek.client.ui.panels.phaseDisplay.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LobbyBackupRotationTest {

    @TempDir
    Path saveDirectory;

    private File createFile(String name) throws IOException {
        return Files.createFile(saveDirectory.resolve(name)).toFile();
    }

    private File createBackups(int count) throws IOException {
        File oldest = null;
        for (int day = 1; day <= count; day++) {
            File backup = createFile(String.format("Lobby_Save_2026-11-%02d_120000.sav.gz", day));
            if (oldest == null) {
                oldest = backup;
            }
        }
        return oldest;
    }

    @Test
    void keepFiveWithSevenBackupsLeavesFourForTheNewOne() throws IOException {
        createBackups(7);

        int deletedCount = LobbyBackupRotation.pruneBeforeNewBackup(saveDirectory.toFile(), 5);

        assertEquals(3, deletedCount);
        assertFalse(saveDirectory.resolve("Lobby_Save_2026-11-01_120000.sav.gz").toFile().exists());
        assertFalse(saveDirectory.resolve("Lobby_Save_2026-11-03_120000.sav.gz").toFile().exists());
        assertTrue(saveDirectory.resolve("Lobby_Save_2026-11-04_120000.sav.gz").toFile().exists());
        assertTrue(saveDirectory.resolve("Lobby_Save_2026-11-07_120000.sav.gz").toFile().exists());
    }

    @Test
    void fewerBackupsThanTheLimitDeletesNothing() throws IOException {
        createBackups(3);

        assertEquals(0, LobbyBackupRotation.pruneBeforeNewBackup(saveDirectory.toFile(), 5));
    }

    @Test
    void zeroKeepsEveryBackup() throws IOException {
        File oldest = createBackups(10);

        assertEquals(0, LobbyBackupRotation.pruneBeforeNewBackup(saveDirectory.toFile(), 0));
        assertTrue(oldest.exists());
    }

    @Test
    void keepOneClearsAllOldBackups() throws IOException {
        createBackups(4);

        assertEquals(4, LobbyBackupRotation.pruneBeforeNewBackup(saveDirectory.toFile(), 1));
    }

    @Test
    void otherSaveFilesAreNeverTouched() throws IOException {
        createBackups(6);
        File roundAutosave = createFile("Round-3-autosave_2026-11-01.sav.gz");
        File manualSave = createFile("MyCampaignGame.sav.gz");

        LobbyBackupRotation.pruneBeforeNewBackup(saveDirectory.toFile(), 1);

        assertTrue(roundAutosave.exists());
        assertTrue(manualSave.exists());
    }

    @Test
    void missingFolderDeletesNothing() {
        File missingDirectory = saveDirectory.resolve("does-not-exist").toFile();

        assertEquals(0, LobbyBackupRotation.pruneBeforeNewBackup(missingDirectory, 5));
    }
}
