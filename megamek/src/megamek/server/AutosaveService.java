/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.server;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import megamek.MMConstants;
import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

public class AutosaveService {
    private static final MMLogger logger = MMLogger.create(AutosaveService.class);

    public static final String FILENAME_FORMAT = "Round-%d-autosave%s.sav.gz";

    private final AbstractGameManager gameManager;

    AutosaveService(AbstractGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void performRollingAutosave() {
        final int maxNumberAutosaves = gameManager.getGame().getOptions()
                .intOption(OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES);
        if (maxNumberAutosaves > 0) {
            try {
                final String fileName = getAutosaveFilename();
                if (!StringUtility.isNullOrBlank(fileName)) {
                    gameManager.saveGame(fileName,
                            gameManager.getGame().getOptions().booleanOption(OptionsConstants.BASE_AUTOSAVE_MSG));
                } else {
                    logger.error("Unable to perform an autosave because of a null or empty file name");
                }
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }
    }

    private @Nullable String getAutosaveFilename() {
        // Get all autosave files in ascending order of date creation
        final String savesDirectoryPath = MMConstants.SAVEGAME_DIR;
        final File folder = new File(savesDirectoryPath);
        final File[] files = folder.listFiles();
        if (files != null) {
            final List<File> autosaveFiles = Arrays.stream(files)
                    .filter(f -> f.getName().startsWith("Round-"))
                    .sorted(Comparator.comparing(File::lastModified))
                    .collect(Collectors.toList());

            // Delete older autosave files if needed
            final int maxNumberAutosaves = gameManager.getGame().getOptions()
                    .intOption(OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES);

            int index = 0;
            while ((autosaveFiles.size() >= maxNumberAutosaves) && (autosaveFiles.size() > index)) {
                if (autosaveFiles.get(index).delete()) {
                    autosaveFiles.remove(index);
                } else {
                    logger.error("Unable to delete file {}", autosaveFiles.get(index).getName());
                    index++;
                }
            }

            // Find a unique name for this autosave
            String fileName = null;

            boolean repeatedName = true;
            String localDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(
                    PreferenceManager.getClientPreferences().getStampFormat()));
            while (repeatedName) {
                fileName = String.format(
                        FILENAME_FORMAT,
                        gameManager.getGame().getCurrentRound(),
                        localDateTime);

                repeatedName = false;
                for (final File file : autosaveFiles) {
                    if (file.getName().compareToIgnoreCase(fileName) == 0) {
                        repeatedName = true;
                        break;
                    }
                }
            }
            // Don't prepend the save dir as we're going to let saveGame handle that.
            return Paths.get(fileName).toString();
        }
        return null;
    }
}
