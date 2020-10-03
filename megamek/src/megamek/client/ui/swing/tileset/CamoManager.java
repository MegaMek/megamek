/*  
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
 *  
 * This program is free software; you can redistribute it and/or modify it under  
 * the terms of the GNU General Public License as published by the Free Software  
 * Foundation; either version 2 of the License, or (at your option) any later  
 * version.  
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT  
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
 * details.  
 */  
package megamek.client.ui.swing.tileset;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import megamek.MegaMek;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.util.ScaledImageFileFactory;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.IPlayer;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.DirectoryItem;
import megamek.common.util.fileUtils.DirectoryItems;

/** 
 * Manages the camos, parsing the camo directory and
 * providing the images.
 *   
 * @author Juliez
 */
public final class CamoManager {

    // This class is not to be instantiated
    private CamoManager() {}

    /** The DirectoryItems object holding all camo file information */ 
    private static DirectoryItems camoDirectory;
    
    /** 
     * True at startup and when the camo directory should be re-parsed.
     * Used to avoid re-parsing the directory repeatedly when there's an error. 
     */
    private static boolean parseDirectory = true; 

    /** 
     * Returns a DirectoryItems object containing all camo image filenames
     * found in MM's camo images folder. 
     * @return a DirectoryItems object with the camo folders and filenames. 
     * May be null if the directory cannot be parsed.
     */
    public static DirectoryItems getCamos() {
        initializeCamos();
        return camoDirectory;
    }

    /** 
     * Parses MM's camo folder when first called
     * or when it was refreshed. 
     * 
     * @see #refreshDirectory()
     */
    private static void initializeCamos() {
        // Read in and parse MM's camo folder only when first called
        // or when refreshed
        if (parseDirectory) {
            // Set parseDirectory to false to avoid parsing repeatedly when something fails
            parseDirectory = false;
            try {
                camoDirectory = new DirectoryItems(Configuration.camoDir(), "", 
                        ScaledImageFileFactory.getInstance());
            } catch (Exception e) {
                MegaMek.getLogger().error("Could not parse the camo directory!");
                e.printStackTrace();
                // This could be improved by obtaining an empty DirectoryItems to avoid returning null
            }
        }
    }
    
    /** 
     * Re-reads MM's camo images folder and returns the updated
     * DirectoryItems object. This will update the DirectoryItems object
     * with changes to the camos (like added image files and folders) 
     * while MM is running.
     * 
     * @see #getCamos()
     */
    public static DirectoryItems refreshDirectory() {
        parseDirectory = true;
        return getCamos();
    }
    
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
        initializeCamos();
        if (camoDirectory == null) {
            return ImageUtil.failStandardImage();
        }

        // Try to get the camo image
        try {
            // A color was selected
            if (category.equals(IPlayer.NO_CAMO)) {
                return colorCamoImage(PlayerColors.getColor(name));
            }
            
            // Translate the root camo directory name.
            // This could be improved by translating before saving it
            if (IPlayer.ROOT_CAMO.equals(category)) {
                category = "";
            }
            
            Image image = (Image) camoDirectory.getItem(category, name);
            // When getItem() doesn't find the category+name, it returns null
            if (image != null) {
                return image;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // An error must have occured, fall back to the fail image
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
        String cat = player.getCamoCategory();
        String name = player.getCamoFileName();
        if (cat.equals(IPlayer.ROOT_CAMO)) {
            cat = "";
        }
        
        // A color was selected
        if (cat.equals(IPlayer.NO_CAMO)) {
            return colorCamoImage(PlayerColors.getColor(player.getColorIndex()));
        }

        // A camo was selected
        return getCamoImage(cat, name);
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

}
