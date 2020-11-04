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

import megamek.MegaMek;
import megamek.common.util.fileUtils.ImageFileFactory;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.util.fileUtils.ScaledImageFileFactory;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.DirectoryItems;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MMStaticDirectoryManager {
    //region Variable Declarations
    // Directories
    private static DirectoryItems portraitDirectory;
    private static DirectoryItems camouflageDirectory;
    private static MechTileset mechTileset;

    // Re-parsing Prevention Variables: The are True at startup and when the specified directory
    // should be re-parsed, and are used to avoid re-parsing the directory repeatedly when there's an error.
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
                        "", new ImageFileFactory());
            } catch (Exception e) {
                MegaMek.getLogger().error("Could not parse the portraits directory!", e);
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
                camouflageDirectory = new DirectoryItems(Configuration.camoDir(), "",
                        new ScaledImageFileFactory());
            } catch (Exception e) {
                MegaMek.getLogger().error("Could not parse the camo directory!", e);
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
                mechTileset.loadFromFile("mechset.txt");
            } catch (Exception e) {
                MegaMek.getLogger().error("Unable to load mech tileset", e);
            }
        }
    }
    //endregion Initialization

    //region Getters
    /**
     * Returns a DirectoryItems object containing all portrait image filenames
     * found in MM's portrait images folder.
     * @return a DirectoryItems object with the portrait folders and filenames.
     * May be null if the directory cannot be parsed.
     */
    public static @Nullable DirectoryItems getPortraits() {
        initializePortraits();
        return portraitDirectory;
    }

    /**
     * Returns a DirectoryItems object containing all camo image filenames
     * found in MM's camo images folder.
     * @return a DirectoryItems object with the camo folders and filenames.
     * May be null if the directory cannot be parsed.
     */
    public static @Nullable DirectoryItems getCamouflage() {
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
     * Re-reads MM's camo images folder and returns the updated
     * DirectoryItems object. This will update the DirectoryItems object
     * with changes to the camos (like added image files and folders)
     * while MM is running.
     *
     * @see #getCamouflage()
     */
    public static DirectoryItems refreshCamouflageDirectory() {
        parseCamouflageDirectory = true;
        return getCamouflage();
    }

    /**
     * Re-reads MM's portrait images folder and returns the updated
     * DirectoryItems object. This will update the DirectoryItems object
     * with changes to the portraits (like added image files and folders)
     * while MM is running.
     *
     * @see #getPortraits()
     */
    public static DirectoryItems refreshPortraitDirectory() {
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

    //region Camouflage
    /**
     * Returns an Image of the camo pattern given
     * by its category (aka directory) and name (aka filename).
     * Will try to return an image if the category indicates
     * that a color was selected.
     * When the camo image cannot be created, a placeholder
     * "fail" image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    @Deprecated
    public static Image getCamoImage(String category, String name) {
        // Return a fail image when parameters are null
        if ((category == null) || (name == null)) {
            return ImageUtil.failStandardImage();
        }

        // Make sure the camoDirectory has been initialized
        // If the camoDirectory is still null, there's an error
        // loading it which has been logged already
        initializeCamouflage();
        if (camouflageDirectory == null) {
            return ImageUtil.failStandardImage();
        }

        // Try to get the camo image
        try {
            // A color was selected
            if (Camouflage.NO_CAMOUFLAGE.equals(category)) {
                return colorCamoImage(PlayerColors.getColor(name));
            }

            // Translate the root camo directory name.
            // This could be improved by translating before saving it
            if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
                category = "";
            }

            Image image = (Image) camouflageDirectory.getItem(category, name);
            // When getItem() doesn't find the category+name, it returns null
            if (image != null) {
                return image;
            }
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        }

        // An error must have occurred, fall back to the fail image
        MegaMek.getLogger().error("Could not create camo image! Category: " + category + "; Name: " + name);
        return ImageUtil.failStandardImage();
    }

    /**
     * Returns an Image of the camo pattern or player color
     * for the given entity.
     * When the camo image cannot be created, a placeholder
     * "fail" image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    @Deprecated
    public static Image getEntityCamoImage(Entity entity) {
        return getCamoImage(entity.getCamoCategory(), entity.getCamoFileName());
    }

    /**
     * Returns a standard size (84x72) image of a single color.
     * When color is null, a "fail" standard image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    @Deprecated
    public static BufferedImage colorCamoImage(Color color) {
        if (color == null) {
            MegaMek.getLogger().error("A null color was passed.");
            return ImageUtil.failStandardImage();
        }
        BufferedImage result = new BufferedImage(84, 72, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, 84, 72);
        return result;
    }
    //endregion Camouflage
}
