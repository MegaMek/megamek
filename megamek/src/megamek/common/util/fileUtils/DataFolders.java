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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nullable;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;

/**
 * Finds the player's data folders: folders outside the installation, laid out like the shipped {@code data/} folder,
 * whose content is loaded on top of the shipped data.
 *
 * <p>Today there is at most one, <b>My data</b>: the user directory chosen by the player, either in the MegaMek
 * Launcher (passed with {@code -Dmegamek.userDir}) or in the client settings. When neither is set there is no data
 * folder; the apps never pick one on their own. More folders (per installation, mods) may follow; they come after My
 * data, and a later folder wins over an earlier one.</p>
 *
 * <p>Code that loads player content should find it through this class rather than reading the user directory
 * preference directly. The legacy {@code userdata/} folder inside the installation is not a data folder here; see
 * {@link MegaMekFile}.</p>
 */
public final class DataFolders {

    /** Where the My data folder comes from. */
    public enum Source {
        /** Set by the MegaMek Launcher for this run; overrides the saved setting. */
        LAUNCHER,
        /** The player's saved client setting. */
        SETTING,
        /** Not chosen; there is no My data folder. */
        NOT_SET
    }

    private final ClientPreferences preferences;

    DataFolders(ClientPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    /**
     * @return the data folders of the current client preferences
     */
    public static DataFolders current() {
        return new DataFolders(PreferenceManager.getClientPreferences());
    }

    /**
     * @return where the My data folder comes from
     */
    public Source myDataSource() {
        if (preferences.getUserDir().isBlank()) {
            return Source.NOT_SET;
        }
        return preferences.isUserDirOverridden() ? Source.LAUNCHER : Source.SETTING;
    }

    /**
     * @return the My data folder as chosen by the player, or {@code null} when none is chosen. The folder is not
     *       checked and may not exist (yet).
     */
    public @Nullable File myData() {
        String userDir = preferences.getUserDir();
        return userDir.isBlank() ? null : new File(userDir);
    }

    /**
     * Returns the chosen data folders in load order (a later folder wins) without checking that they exist. Use this
     * only on hot paths that already cope with missing folders, such as cached directory listings; otherwise use
     * {@link #folders()}.
     *
     * @return the chosen data folders in load order, possibly empty
     */
    public List<File> configuredFolders() {
        File myData = myData();
        return (myData == null) ? List.of() : List.of(myData);
    }

    /**
     * @return the chosen data folders that exist as directories, in load order (a later folder wins)
     */
    public List<File> folders() {
        List<File> result = new ArrayList<>();
        for (File folder : configuredFolders()) {
            if (folder.isDirectory()) {
                result.add(folder);
            }
        }
        return result;
    }

    /**
     * Returns the given subfolder of each data folder, where it exists, in load order (a later folder wins). Data
     * folders without that subfolder are skipped quietly, so loaders don't warn about content a player simply hasn't
     * added.
     *
     * @param relativePath the subfolder, relative to a data folder and laid out like the installation, e.g.
     *                     {@code data/universe/factions}
     *
     * @return the existing subfolders in load order, possibly empty
     */
    public List<File> subfolders(String relativePath) {
        List<File> result = new ArrayList<>();
        for (File folder : folders()) {
            File subfolder = new File(folder, relativePath);
            if (subfolder.isDirectory()) {
                result.add(subfolder);
            }
        }
        return result;
    }

    /**
     * @param relativePath the file, relative to a data folder and laid out like the installation
     *
     * @return the file from the last data folder that has it, or {@code null} when no data folder has it
     */
    public @Nullable File findFile(String relativePath) {
        List<File> folders = folders();
        for (File folder : folders.reversed()) {
            File file = new File(folder, relativePath);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }
}
