/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

public record AutosaveService(AbstractGameManager gameManager) {
    private static final MMLogger LOGGER = MMLogger.create(AutosaveService.class);

    public static final String FILENAME_FORMAT = "Round-%d-autosave%s.sav.gz";

    public void performRollingAutosave() {
        final int maxNumberAutoSaves = gameManager.getGame().getOptions()
              .intOption(OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES);
        if (maxNumberAutoSaves > 0) {
            try {
                final String fileName = getAutosaveFilename();
                if (!StringUtility.isNullOrBlank(fileName)) {
                    gameManager.saveGame(fileName,
                          gameManager.getGame().getOptions().booleanOption(OptionsConstants.BASE_AUTOSAVE_MSG));
                } else {
                    LOGGER.error("Unable to perform an autosave because of a null or empty file name");
                }
            } catch (Exception ex) {
                LOGGER.error("", ex);
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
            final int maxNumberAutoSaves = gameManager.getGame().getOptions()
                  .intOption(OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES);

            int index = 0;
            while ((autosaveFiles.size() >= maxNumberAutoSaves) && (autosaveFiles.size() > index)) {
                if (autosaveFiles.get(index).delete()) {
                    autosaveFiles.remove(index);
                } else {
                    LOGGER.error("Unable to delete file {}", autosaveFiles.get(index).getName());
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
