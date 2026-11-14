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

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.logging.MMLogger;

/**
 * Keeps the number of lobby backups in the save folder under control. With the "Save lobby backup when game starts"
 * client setting on, every game start writes a new {@code Lobby_Save_<timestamp>} file; without this cleanup they
 * pile up forever.
 */
final class LobbyBackupRotation {
    private static final MMLogger LOGGER = MMLogger.create(LobbyBackupRotation.class);

    /** The file name prefix of every lobby backup; only files starting with it are ever deleted. */
    static final String LOBBY_BACKUP_PREFIX = "Lobby_Save_";

    private LobbyBackupRotation() {}

    /**
     * Deletes the oldest lobby backups in the given folder so that, once the backup about to be written is added,
     * no more than {@code backupsToKeep} remain. Backups are ordered by name, which is chronological because the
     * name carries a {@code yyyy-MM-dd_HHmmss} timestamp. Other files in the folder are never touched.
     *
     * @param saveDirectory the folder the lobby backups are written to
     * @param backupsToKeep how many backups to keep, counting the one about to be written; {@code 0} or less keeps
     *                      every backup
     *
     * @return the number of backups deleted
     */
    static int pruneBeforeNewBackup(File saveDirectory, int backupsToKeep) {
        if (backupsToKeep <= 0) {
            LOGGER.debug("[LobbySave] Keeping every lobby backup (keep count is {})", backupsToKeep);
            return 0;
        }
        List<File> backups = findBackupsOldestFirst(saveDirectory);
        int backupsToDelete = backups.size() - (backupsToKeep - 1);
        int deletedCount = 0;
        for (int index = 0; index < backupsToDelete; index++) {
            File backup = backups.get(index);
            if (backup.delete()) {
                deletedCount++;
            } else {
                LOGGER.warn("[LobbySave] Could not delete old lobby backup {}", backup.getAbsolutePath());
            }
        }
        if (deletedCount > 0) {
            LOGGER.info("[LobbySave] Deleted {} old lobby backups to keep the newest {}", deletedCount, backupsToKeep);
        } else {
            LOGGER.debug("[LobbySave] Found {} lobby backups, keeping {}, nothing to delete",
                  backups.size(), backupsToKeep);
        }
        return deletedCount;
    }

    private static List<File> findBackupsOldestFirst(File saveDirectory) {
        List<File> backups = new ArrayList<>();
        File[] files = saveDirectory.listFiles();
        if (files == null) {
            LOGGER.debug("[LobbySave] Save folder {} does not exist or cannot be read; nothing to clean up",
                  saveDirectory.getAbsolutePath());
            return backups;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(LOBBY_BACKUP_PREFIX)) {
                backups.add(file);
            }
        }
        backups.sort(Comparator.comparing(File::getName));
        return backups;
    }
}
