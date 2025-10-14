/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.tileset;

import java.io.File;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.fileUtils.AbstractDirectory;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.fileUtils.ImageFileFactory;
import megamek.common.util.fileUtils.ScaledImageFileFactory;
import megamek.logging.MMLogger;

public class MMStaticDirectoryManager {
    private static final MMLogger logger = MMLogger.create(MMStaticDirectoryManager.class);

    // region Variable Declarations
    // Directories
    private static DirectoryItems portraitDirectory;
    private static DirectoryItems camouflageDirectory;
    private static MekTileset mekTileset;

    // Reparsing Prevention Variables: They are True at startup and when the specified directory should be reparsed,
    // and are used to avoid reparsing the directory repeatedly when there's an error.
    private static boolean parsePortraitDirectory = true;
    private static boolean parseCamouflageDirectory = true;
    private static boolean parseMekTileset = true;
    // endregion Variable Declarations

    // region Constructors
    protected MMStaticDirectoryManager() {
        // This class is not to be instantiated
    }
    // endregion Constructors

    // region Initialization

    /**
     * This initialized all the directories under this manager
     */
    public static void initialize() {
        initializePortraits();
        initializeCamouflage();
        initializeMekTileset();
    }

    /**
     * Parses MM's portraits folder when first called or when it was refreshed.
     *
     * @see #refreshPortraitDirectory()
     */
    private static void initializePortraits() {
        // Read in and parse MM's portrait folder only when first called or when
        // refreshed
        if (parsePortraitDirectory) {
            // Set parsePortraitDirectory too false to avoid parsing repeatedly when
            // something fails
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

                // check for portraits in story arcs subdirectories
                File storyArcsDir = Configuration.storyArcsDir();
                if (storyArcsDir.exists() && storyArcsDir.isDirectory()) {
                    File[] fileList = storyArcsDir.listFiles();

                    if (fileList != null) {
                        for (File file : fileList) {
                            if (file.isDirectory()) {
                                File storyArcPortraitDir = new File(file.getPath() + "/data/images/portraits");
                                if (storyArcPortraitDir.exists() && storyArcPortraitDir.isDirectory()) {
                                    DirectoryItems storyArcPortraits = new DirectoryItems(storyArcPortraitDir,
                                          new ImageFileFactory());
                                    portraitDirectory.merge(storyArcPortraits);
                                }
                            }
                        }

                    }
                }
            } catch (Exception e) {
                logger.error("Could not parse the portraits directory!", e);
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
            // Set parseCamouflageDirectory too false to avoid parsing repeatedly when
            // something fails
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

                // check for camouflage in story arcs subdirectories
                File storyArcsDir = Configuration.storyArcsDir();
                if (storyArcsDir.exists() && storyArcsDir.isDirectory()) {
                    File[] fileList = storyArcsDir.listFiles();
                    if (fileList != null) {
                        for (File file : fileList) {
                            if (file.isDirectory()) {
                                File storyArcCamoDir = new File(file.getPath() + "/data/images/camo");
                                if (storyArcCamoDir.exists() && storyArcCamoDir.isDirectory()) {
                                    DirectoryItems storyArcCamo = new DirectoryItems(storyArcCamoDir,
                                          new ScaledImageFileFactory());
                                    camouflageDirectory.merge(storyArcCamo);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Could not parse the camo directory!", e);
            }
        }
    }

    /**
     * Parses MM's mek tileset when first called or when it was refreshed.
     *
     * @see #refreshMekTileset()
     */
    private static void initializeMekTileset() {
        if (parseMekTileset) {
            // Set parseMekTileset too false to avoid parsing repeatedly when something fails
            parseMekTileset = false;
            mekTileset = new MekTileset(Configuration.unitImagesDir());
            try {
                mekTileset.loadFromFile("mekset.txt");// TODO : Remove inline file path
            } catch (Exception e) {
                logger.error("Unable to load mek tileset", e);
            }
        }
    }
    // endregion Initialization

    // region Getters

    /**
     * Returns an AbstractDirectory object containing all portrait image filenames found in MM's portrait images
     * folder.
     *
     * @return an AbstractDirectory object with the portrait folders and filenames. May be null if the directory cannot
     *       be parsed.
     */
    public static @Nullable AbstractDirectory getPortraits() {
        initializePortraits();
        return portraitDirectory;
    }

    /**
     * Returns an AbstractDirectory object containing all camo image filenames found in MM's camo images folder.
     *
     * @return an AbstractDirectory object with the camo folders and filenames. May be null if the directory cannot be
     *       parsed.
     */
    public static @Nullable AbstractDirectory getCamouflage() {
        initializeCamouflage();
        return camouflageDirectory;
    }

    /**
     * @return a MekTileset object. May be null if the directory cannot be parsed
     */
    public static @Nullable MekTileset getMekTileset() {
        initializeMekTileset();
        return mekTileset;
    }
    // endregion Getters

    // region Refreshers

    /**
     * Re-reads MM's camo images folder and returns the updated AbstractDirectory object. This will update the
     * AbstractDirectory object with changes to the camos (like added image files and folders) while MM is running.
     *
     * @see #getCamouflage()
     */
    public static @Nullable AbstractDirectory refreshCamouflageDirectory() {
        parseCamouflageDirectory = true;
        return getCamouflage();
    }

    /**
     * Re-reads MM's portrait images folder and returns the updated AbstractDirectory object. This will update the
     * AbstractDirectory object with changes to the portraits (like added image files and folders) while MM is running.
     *
     * @see #getPortraits()
     */
    public static @Nullable AbstractDirectory refreshPortraitDirectory() {
        parsePortraitDirectory = true;
        return getPortraits();
    }

    /**
     * Reloads the MekTileset and returns the updated MekTileset object. This will update the MekTileset object with
     * changes to the mek tileset (like added image files and changes to the tileset text file) while MM is running.
     *
     * @see #getMekTileset()
     */
    public static MekTileset refreshMekTileset() {
        parseMekTileset = true;
        return getMekTileset();
    }
    // endregion Refreshers
}
