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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import megamek.MegaMek;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.DirectoryItems;

/** 
 * Manages the portraits, parsing the portraits directory and
 * providing the images.
 *   
 * @author Juliez
 */
public class PortraitManager {

    // This class is not to be instantiated
    private PortraitManager() {}

    /** The DirectoryItems object holding all portrait file information */ 
    private static DirectoryItems portraitDirectory;
    
    /** 
     * True at startup and when the portrait directory should be re-parsed.
     * Used to avoid re-parsing the directory repeatedly when there's an error. */
    private static boolean parseDirectory = true; 

    /** 
     * Returns a DirectoryItems object containing all portrait image filenames
     * found in MM's portrait images folder. 
     * @return a DirectoryItems object with the portrait folders and filenames. 
     * May be null if the directory cannot be parsed.
     */
    public static DirectoryItems getPortraits() {
        initializePortraits();
        return portraitDirectory;
    }
    
    /** Holds a drawn "fail" image that can be used when image loading fails. */ 
    public static BufferedImage failPortrait;

    /** 
     * Parses MM's portraits folder when first called
     * or when it was refreshed. 
     * 
     * @see #refreshDirectory()
     */
    private static void initializePortraits() {
        // Read in and parse MM's portrait folder only when first called
        // or when refreshed
        if (parseDirectory) {
            // Set parseDirectory to false to avoid parsing repeatedly when something fails
            parseDirectory = false;
            try {
                portraitDirectory = new DirectoryItems(Configuration.portraitImagesDir(), "", 
                        ImageFileFactory.getInstance());
            } catch (Exception e) {
                MegaMek.getLogger().error(PortraitManager.class, "Could not parse the portraits directory!");
                e.printStackTrace();
                // This could be improved by obtaining an empty DirectoryItems to avoid returning null
            }
        }
    }
    
    /** 
     * Re-reads MM's portrait images folder and returns the updated
     * DirectoryItems object. This will update the DirectoryItems object
     * with changes to the portraits (like added image files and folders) 
     * while MM is running.
     * 
     * @see #getPortraits()
     */
    public static DirectoryItems refreshDirectory() {
        parseDirectory = true;
        return getPortraits();
    }
    
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
        if (category == null 
                || name == null
                || category.equals(Crew.PORTRAIT_NONE)) {
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
            // Translate the root portrait directory name and PORTRAIT_NONE
            // This could be improved by not passing around ROOT_PORTRAIT
            if (category.equals(Crew.ROOT_PORTRAIT)) {
                category = "";
            }
            if (name.equals(Crew.PORTRAIT_NONE)) {
                name = "default.gif";
            }
            
            Image image = (Image) portraitDirectory.getItem(category, name);
            
            // When getItem() doesn't find the category+name, it returns null
            if (image != null) {
                return image;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // An error must have occured, fall back to the default portrait
        MegaMek.getLogger().error(PortraitManager.class, 
                "Could not load portrait image! Category: " + category + "; Name: " + name);
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
            Image image = (Image) (portraitDirectory.getItem("", "default.gif"));

            // When getItem() doesn't find the default portrait, it returns null
            // In that case, fall back to the failPortrait
            if (image != null) {
                return image;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        MegaMek.getLogger().error(PortraitManager.class, "Could not load default portrait image!");
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
            g.drawImage(image, 0, (size-image.getHeight(null))/2, null);
        } else {
            image = image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
            g.drawImage(image, (size-image.getWidth(null))/2, 0, null);
        }
        return result;
    }


}
