/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.tileset;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.fileUtils.AbstractDirectory;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.fileUtils.ImageFileFactory;
import megamek.common.util.fileUtils.ScaledImageFileFactory;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.util.Map;

public class MMStaticDirectoryManager {
    //region Variable Declarations
    // Directories
    private static DirectoryItems portraitDirectory;
    private static DirectoryItems camouflageDirectory;
    private static MechTileset mechTileset;

    // Re-parsing Prevention Variables: They are True at startup and when the specified directory
    // should be re-parsed, and are used to avoid re-parsing the directory repeatedly when there's
    // an error.
    private static boolean parsePortraitDirectory = true;
    private static boolean parseCamouflageDirectory = true;
    private static boolean parseMechTileset = true;
    //endregion Variable Declarations

    //region Constructors
    protected MMStaticDirectoryManager() {
        // This class is not to be instantiated
    }
    //endregion Constructors

    //region Initialization
    /**
     * This initialized all of the directories under this manager
     */
    public static void initialize() {
        initializePortraits();
        initializeCamouflage();
        initializeMechTileset();
    }

    /**
     * Parses MM's portraits folder when first called or when it was refreshed.
     *
     * @see #refreshPortraitDirectory()
     */
    private static void initializePortraits() {
        // Read in and parse MM's portrait folder only when first called or when refreshed
        if (parsePortraitDirectory) {
            // Set parsePortraitDirectory to false to avoid parsing repeatedly when something fails
            parsePortraitDirectory = false;
            try {
                portraitDirectory = new DirectoryItems(Configuration.portraitImagesDir(),
                        new ImageFileFactory());

                String userDir = PreferenceManager.getClientPreferences().getUserDir();
                File portraitUserDir = new File(userDir + "/" + Configuration.portraitImagesDir());
                if (!userDir.isBlank() && portraitUserDir.isDirectory()) {
                    DirectoryItems userDirPortraits = new DirectoryItems(portraitUserDir, new ImageFileFactory());
                    portraitDirectory.merge(userDirPortraits);
                }
            } catch (Exception e) {
                LogManager.getLogger().error("Could not parse the portraits directory!", e);
            }
        }
    }

    /**
     * Parses MM's camo folder when first called or when it was refreshed.
     *
     * @see #refreshCamouflageDirectory()
     */
    private static void initializeCamouflage() {
        // Read in and parse MM's camo folder only when first called or when refreshed
        if (parseCamouflageDirectory) {
            // Set parseCamouflageDirectory to false to avoid parsing repeatedly when something fails
            parseCamouflageDirectory = false;
            try {
                camouflageDirectory = new DirectoryItems(Configuration.camoDir(),
                        new ScaledImageFileFactory());

                String userDir = PreferenceManager.getClientPreferences().getUserDir();
                File camoUserDir = new File(userDir + "/" + Configuration.camoDir());
                if (!userDir.isBlank() && camoUserDir.isDirectory()) {
                    DirectoryItems userDirCamo = new DirectoryItems(camoUserDir, new ScaledImageFileFactory());
                    camouflageDirectory.merge(userDirCamo);
                }
            } catch (Exception e) {
                LogManager.getLogger().error("Could not parse the camo directory!", e);
            }
        }
    }

    /**
     * Parses MM's mech tileset when first called or when it was refreshed.
     *
     * @see #refreshMechTileset()
     */
    private static void initializeMechTileset() {
        if (parseMechTileset) {
            // Set parseMechTileset to false to avoid parsing repeatedly when something fails
            parseMechTileset = false;
            mechTileset = new MechTileset(Configuration.unitImagesDir());
            try {
                mechTileset.loadFromFile("mechset.txt");// TODO : Remove inline file path
            } catch (Exception e) {
                LogManager.getLogger().error("Unable to load mech tileset", e);
            }
        }
    }
    //endregion Initialization

    //region Getters
    /**
     * Returns an AbstractDirectory object containing all portrait image filenames found in MM's
     * portrait images folder.
     * @return an AbstractDirectory object with the portrait folders and filenames.
     * May be null if the directory cannot be parsed.
     */
    public static @Nullable AbstractDirectory getPortraits() {
        initializePortraits();
        return portraitDirectory;
    }

    /**
     * Returns an AbstractDirectory object containing all camo image filenames found in MM's camo
     * images folder.
     * @return an AbstractDirectory object with the camo folders and filenames.
     * May be null if the directory cannot be parsed.
     */
    public static @Nullable AbstractDirectory getCamouflage() {
        initializeCamouflage();
        return camouflageDirectory;
    }

    /**
     * @return a MechTileset object. May be null if the directory cannot be parsed
     */
    public static @Nullable MechTileset getMechTileset() {
        initializeMechTileset();
        return mechTileset;
    }
    //endregion Getters

    //region Refreshers
    /**
     * Re-reads MM's camo images folder and returns the updated AbstractDirectory object. This will
     * update the AbstractDirectory object with changes to the camos (like added image files and
     * folders) while MM is running.
     *
     * @see #getCamouflage()
     */
    public static @Nullable AbstractDirectory refreshCamouflageDirectory() {
        parseCamouflageDirectory = true;
        return getCamouflage();
    }

    /**
     * Re-reads MM's portrait images folder and returns the updated AbstractDirectory object. This
     * will update the AbstractDirectory object with changes to the portraits (like added image
     * files and folders) while MM is running.
     *
     * @see #getPortraits()
     */
    public static @Nullable AbstractDirectory refreshPortraitDirectory() {
        parsePortraitDirectory = true;
        return getPortraits();
    }

    /**
     * Reloads the MechTileset and returns the updated MechTileset object.
     * This will update the MechTileset object with changes to the mech tileset
     * (like added image files and changes to the tileset text file) while MM is running.
     *
     * @see #getMechTileset()
     */
    public static MechTileset refreshMechTileset() {
        parseMechTileset = true;
        return getMechTileset();
    }
    //endregion Refreshers
}
