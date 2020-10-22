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
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.IPlayer;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.DirectoryItem;
import megamek.common.util.fileUtils.DirectoryItems;

import javax.swing.*;
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

    //region Portraits
    /** Holds a drawn "fail" image that can be used when image loading fails. */
    public static BufferedImage failPortrait;

    /**
     * Returns an Image of the portrait given
     * by its category (aka directory) and name (aka filename).
     * The image is not scaled in any way.
     * When the portrait cannot be created, the default portrait
     * or - if that cannot be found - a placeholder "fail" image
     * is returned.
     *
     * @see #getDefaultPortrait()
     * @see #failPortrait()
     */
    public static Image getUnscaledPortraitImage(String category, String name) {
        // Return the default portrait when parameters are null
        // or no portrait is selected
        if ((category == null) || (name == null)
                || AbstractIcon.DEFAULT_ICON_FILENAME.equals(category)) {
            return getDefaultPortrait();
        }

        // Make sure the portraitDirectory has been initialized
        // If the portraitDirectory is still null, there's an error
        // loading it which has been logged already
        initializePortraits();
        if (portraitDirectory == null) {
            return getDefaultPortrait();
        }

        // Try to get the portrait
        try {
            // Translate the root portrait directory name and DEFAULT_ICON_FILENAME
            // This could be improved by not passing around ROOT_CATEGORY
            if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
                category = "";
            }

            if (AbstractIcon.DEFAULT_ICON_FILENAME.equals(name)) {
                name = Portrait.DEFAULT_PORTRAIT_FILENAME;
            }

            Image image = (Image) portraitDirectory.getItem(category, name);

            // When getItem() doesn't find the category+name, it returns null
            if (image != null) {
                return image;
            }
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        }

        // An error must have occurred, fall back to the default portrait
        MegaMek.getLogger().error("Could not load portrait image! Category: " + category + "; Name: " + name);
        return getDefaultPortrait();
    }

    /**
     * Returns an Image of the portrait given
     * by its category (aka directory) and name (aka filename).
     * The image will be scaled and centered to 72x72 pixels.
     * When the portrait cannot be created, the default portrait
     * or - if that cannot be found - a placeholder "fail" image
     * is returned.
     *
     * @see #getDefaultPortrait()
     * @see #failPortrait()
     */
    public static Image getPortraitImage(String category, String name) {
        return scaleAndCenter(getUnscaledPortraitImage(category, name), 72);
    }

    /**
     * Returns an Icon of the portrait given
     * by its category (aka directory) and name (aka filename).
     * The image will be scaled to be 72 pixels high.
     * When the portrait cannot be created, the default portrait
     * or - if that cannot be found - a placeholder "fail" image
     * is returned.
     *
     * @see #getDefaultPortrait()
     * @see #failPortrait()
     */
    public static Icon getPortraitIcon(String category, String name) {
        return new ImageIcon(getPortraitImage(category, name));
    }

    /**
     * Returns an Icon of the portrait given
     * by its crew/pilot and slot.
     * The image will be scaled to be 72 pixels high.
     * When the portrait cannot be created, the default portrait
     * or - if that cannot be found - a placeholder "fail" image
     * is returned.
     *
     * @see #getDefaultPortrait()
     * @see #failPortrait()
     */
    public static Image getPortraitImage(Crew crew, int slot) {
        String category = crew.getPortraitCategory(slot);
        String filename = crew.getPortraitFileName(slot);
        return getPortraitImage(category, filename);
    }

    /**
     * Returns an Icon of the portrait given
     * by its crew/pilot and slot.
     * The image will be scaled to be 72 pixels high.
     * When the portrait cannot be created, the default portrait
     * or - if that cannot be found - a placeholder "fail" image
     * is returned.
     *
     * @see #getDefaultPortrait()
     * @see #failPortrait()
     */
    public static Icon getPortraitIcon(Crew crew, int slot) {
        return new ImageIcon(getPortraitImage(crew, slot));
    }

    /**
     * Returns the default portrait (default.gif) or
     * the failPortrait() in case of an error.
     */
    public static Image getDefaultPortrait() {
        try {
            Image image = (Image) (portraitDirectory.getItem("", Portrait.DEFAULT_PORTRAIT_FILENAME));

            // When getItem() doesn't find the default portrait, it returns null
            // In that case, fall back to the failPortrait
            if (image != null) {
                return image;
            }
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        }

        MegaMek.getLogger().error("Could not load default portrait image!");
        return failPortrait();
    }

    /**
     * Returns a square (72x72) "fail" image having a gray on dark gray cross.
     * The image is drawn, not loaded and should therefore work in almost all cases.
     */
    public static Image failPortrait() {
        if (failPortrait == null) {
            failPortrait = new BufferedImage(72, 72, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = failPortrait.createGraphics();
            graphics.setColor(Color.DARK_GRAY);
            graphics.fillRect(0, 0, 72, 72);
            graphics.setStroke(new BasicStroke(4f));
            graphics.setColor(Color.GRAY);
            graphics.drawLine(56, 56, 16, 16);
            graphics.drawLine(56, 16, 16, 56);
        }
        return failPortrait;
    }

    /**
     * Returns the portrait image scaled to 50x50. The aspect ratio
     * of the image is preserved and when the portrait is not square,
     * it is centered as necessary on a transparent background.
     */
    public static Image getPreviewPortraitImage(String category, String name) {
        return scaleAndCenter(getUnscaledPortraitImage(category, name), 50);
    }

    /**
     * Returns a square BufferedImage of the given size.
     * Scales the given image to fit into the square and centers it
     * on a transparent background.
     */
    private static BufferedImage scaleAndCenter(Image image, int size) {
        BufferedImage result = ImageUtil.createAcceleratedImage(size, size);
        Graphics g = result.getGraphics();
        if (image.getWidth(null) > image.getHeight(null)) {
            image = image.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
            g.drawImage(image, 0, (size - image.getHeight(null)) / 2, null);
        } else {
            image = image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
            g.drawImage(image, (size - image.getWidth(null)) / 2, 0, null);
        }
        return result;
    }
    //endregion Portraits

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
     * Returns an Image of the camo pattern given by a DirectoryItem object.
     * When the camo image cannot be created, a placeholder
     * "fail" image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    public static Image getCamoImage(DirectoryItem name) {
        return getCamoImage(name.getCategory(), name.getItem());
    }

    /**
     * Returns an Icon of the camo pattern given
     * by its category (aka directory) and name (aka filename).
     * When the camo image cannot be created, a placeholder
     * "fail" image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    public static Icon getCamoIcon(String category, String name) {
        return new ImageIcon(getCamoImage(category, name));
    }

    /**
     * Returns an Image of the camo pattern or player color
     * for the given IPlayer.
     * When the camo image cannot be created, a placeholder
     * "fail" image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    public static Image getPlayerCamoImage(IPlayer player) {
        String category = player.getCamoCategory();

        if (Camouflage.NO_CAMOUFLAGE.equals(category)) { // Colour Camouflage
            return colorCamoImage(PlayerColors.getColor(player.getColorIndex()));
        }

        if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
            category = "";
        }

        // A camo was selected
        return getCamoImage(category, player.getCamoFileName());
    }

    /**
     * Returns an Icon of the camo pattern or player color
     * for the given IPlayer.
     * When the camo image cannot be created, a placeholder
     * "fail" image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    public static Icon getPlayerCamoIcon(IPlayer player) {
        return new ImageIcon(getPlayerCamoImage(player));
    }

    /**
     * Returns an Image of the camo pattern or player color
     * for the given entity.
     * When the camo image cannot be created, a placeholder
     * "fail" image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    public static Image getEntityCamoImage(Entity entity) {
        return getCamoImage(entity.getCamoCategory(), entity.getCamoFileName());
    }

    /**
     * Returns a standard size (84x72) image of a single color.
     * When color is null, a "fail" standard image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
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

    /**
     * Returns a standard size (84x72) icon of a single color.
     * When color is null, a "fail" standard image is returned.
     *
     * @see ImageUtil#failStandardImage()
     */
    public static Icon colorCamoIcon(Color color) {
        return new ImageIcon(colorCamoImage(color));
    }
    //endregion Camouflage
}
