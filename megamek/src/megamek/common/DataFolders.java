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

package megamek.common;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

/**
 * Resolves the data folders that game data is read from. A data folder has the same layout as the installation's
 * {@code data/} folder, e.g. {@code universe/factions/} or {@code unitfluff/}. The data folders form layers, like mods
 * in a game's load order: data is read from the lowest layer up, and a higher layer overrides what a lower layer
 * provides. From lowest to highest:
 * <ol>
 *     <li>{@link Layer#OFFICIAL}: the installation's {@code data/} folder</li>
 *     <li>{@link Layer#LAUNCHER}: a data folder provided by the MegaMek Launcher, e.g. for optional downloads</li>
 *     <li>{@link Layer#USER_DIR}: {@code data/} in the User Directory, shared by every installation that uses it</li>
 *     <li>{@link Layer#USER_DATA}: {@code userdata/data/} in this installation</li>
 * </ol>
 * The more specific a layer is, the higher it is: the player's own data overrides the official data, and the data of
 * one installation overrides the data shared by all installations. What the launcher provides is official data that
 * is not part of the installation, so it goes directly above the official data and below the player's own. A host
 * application may add a layer above all of these, such as a MekHQ campaign; future layers such as mods go below the
 * player's own data.
 *
 * <p>Folders that don't exist are left out, so callers don't need to check for them.</p>
 */
public final class DataFolders {

    /** The name of the data folder within the User Directory and within the {@code userdata/} folder. */
    public static final String DATA_DIRECTORY_NAME = "data";

    /**
     * The system property with which the MegaMek Launcher names the data folder it provides. The launcher only adds it
     * to the launch command of applications whose jar manifest carries {@code MegaMek-Launcher-Data-Dir: 1}. Without
     * the property, as when started any other way, there is no {@link Layer#LAUNCHER} layer.
     */
    public static final String LAUNCHER_DATA_DIR_PROPERTY = "megamek.launcher.dataDir";

    private static final MMLogger LOGGER = MMLogger.create(DataFolders.class);
    private static final AtomicBoolean launcherDataDirLogged = new AtomicBoolean();

    /** The data layers, from lowest to highest. */
    public enum Layer {
        /** The installation's {@code data/} folder. Only releases change it; it is never written to. */
        OFFICIAL,
        /** The data folder named by {@link #LAUNCHER_DATA_DIR_PROPERTY}. The launcher owns it; it is only read. */
        LAUNCHER,
        /** The {@code data/} folder in the User Directory, shared by every installation that uses it. */
        USER_DIR,
        /** The {@code userdata/data/} folder of this installation. */
        USER_DATA
    }

    /**
     * A data folder.
     *
     * @param layer     The layer the folder belongs to
     * @param directory The folder; it has the same layout as the installation's {@code data/} folder
     */
    public record DataFolder(Layer layer, File directory) {

        /**
         * @param relativePath A path relative to the data folder, e.g. {@code "unitfluff"}
         *
         * @return The given path within this data folder; it may not exist
         */
        public File resolve(String relativePath) {
            return new File(directory, relativePath);
        }
    }

    private DataFolders() {}

    /**
     * @return The existing data folders from the lowest layer to the highest. Read data in this order when a later
     *       folder's data replaces or merges into an earlier folder's data.
     */
    public static List<DataFolder> loadOrder() {
        String launcherDataDir = System.getProperty(LAUNCHER_DATA_DIR_PROPERTY);
        logLauncherDataDirOnce(launcherDataDir);
        return loadOrder(Configuration.dataDir(),
              launcherDataDir,
              PreferenceManager.getClientPreferences().getUserDir(),
              Configuration.userDataDir());
    }

    /**
     * @return The existing data folders from the highest layer to the lowest. Search them in this order when the first
     *       match wins, e.g. when looking for a single file.
     */
    public static List<DataFolder> lookupOrder() {
        return reversed(loadOrder());
    }

    /**
     * @param relativePath A file path relative to a data folder, e.g. {@code "unitfluff/<uuid>.yml"}
     *
     * @return The file from the highest data folder that has it, or {@code null} if no data folder has it
     */
    public static @Nullable File findFile(String relativePath) {
        for (DataFolder folder : lookupOrder()) {
            File file = folder.resolve(relativePath);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * @param officialDataDir The installation's data folder
     * @param launcherDataDir The data folder provided by the launcher, or {@code null} or blank when there is none
     * @param userDir         The User Directory, or {@code null} or blank when none is set
     * @param userDataDir     The installation's {@code userdata/} folder
     *
     * @return The existing data folders from the lowest layer to the highest; a folder that is the same as a lower
     *       one is left out
     */
    static List<DataFolder> loadOrder(File officialDataDir, @Nullable String launcherDataDir,
          @Nullable String userDir, File userDataDir) {
        List<DataFolder> result = new ArrayList<>(Layer.values().length);
        Set<Path> seen = new HashSet<>();
        addIfPresent(result, seen, Layer.OFFICIAL, officialDataDir);
        if ((launcherDataDir != null) && !launcherDataDir.isBlank()) {
            addIfPresent(result, seen, Layer.LAUNCHER, new File(launcherDataDir));
        }
        if ((userDir != null) && !userDir.isBlank()) {
            addIfPresent(result, seen, Layer.USER_DIR, new File(userDir, DATA_DIRECTORY_NAME));
        }
        addIfPresent(result, seen, Layer.USER_DATA, new File(userDataDir, DATA_DIRECTORY_NAME));
        return result;
    }

    static List<DataFolder> reversed(List<DataFolder> folders) {
        List<DataFolder> result = new ArrayList<>(folders.size());
        for (int index = folders.size() - 1; index >= 0; index--) {
            result.add(folders.get(index));
        }
        return result;
    }

    /** Logs once whether the launcher provided a data folder, so that a test can be read from the log. */
    private static void logLauncherDataDirOnce(@Nullable String launcherDataDir) {
        if (!launcherDataDirLogged.compareAndSet(false, true)) {
            return;
        }
        if ((launcherDataDir == null) || launcherDataDir.isBlank()) {
            LOGGER.info("[DataFolders] No launcher data folder: {} is not set", LAUNCHER_DATA_DIR_PROPERTY);
        } else if (new File(launcherDataDir).isDirectory()) {
            LOGGER.info("[DataFolders] Launcher data folder in use: {}", launcherDataDir);
        } else {
            LOGGER.warn("[DataFolders] Launcher data folder ignored, not a folder: {}", launcherDataDir);
        }
    }

    private static void addIfPresent(List<DataFolder> folders, Set<Path> seen, Layer layer, File directory) {
        if (directory.isDirectory() && seen.add(directory.toPath().toAbsolutePath().normalize())) {
            folders.add(new DataFolder(layer, directory));
        }
    }
}
