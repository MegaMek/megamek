/*
* MegaMek -
* Copyright (C) 2002, 2003, 2004 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018, 2020 The MegaMek Team
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.util.ImageUtil;

/** Handles the rotated and damaged and preview images for a unit. */
public class EntityImage {
    
    // Control values for applying bigger and smaller smoke
    private static final int SMOKE_THREE = 70;
    private static final int SMOKE_TWO = 40;
    
    // Damage decal images
    private static final File FILENAME_DAMAGEDECAL_LIGHT = new File("units/DamageDecals", "DmgLight.png"); //$NON-NLS-1$
    private static final File FILENAME_DAMAGEDECAL_MODERATE = new File("units/DamageDecals", "DmgModerate.png"); //$NON-NLS-1$
    private static final File FILENAME_DAMAGEDECAL_HEAVY = new File("units/DamageDecals", "DmgHeavy.png"); //$NON-NLS-1$
    private static final File FILENAME_DAMAGEDECAL_CRIPPLED = new File("units/DamageDecals", "DmgCrippled.png"); //$NON-NLS-1$
    private static final File FILE_SMOKE_SML = new File("units/DamageDecals", "Smoke1.png"); //$NON-NLS-1$
    private static final File FILE_SMOKE_MED = new File("units/DamageDecals", "Smoke2.png"); //$NON-NLS-1$
    private static final File FILE_SMOKE_LRG = new File("units/DamageDecals", "Smoke3.png"); //$NON-NLS-1$
    private static final File FILE_SMOKEFIRE_SML = new File("units/DamageDecals", "SmokeFire1.png"); //$NON-NLS-1$
    private static final File FILE_SMOKEFIRE_MED = new File("units/DamageDecals", "SmokeFire2.png"); //$NON-NLS-1$
    private static final File FILE_SMOKEFIRE_LRG = new File("units/DamageDecals", "SmokeFire3.png"); //$NON-NLS-1$
    private static final File FILE_DAMAGEDECAL_EMPTY = new File("units/DamageDecals", "Transparent.png"); //$NON-NLS-1$
    
    private static Image dmgLight;
    private static Image dmgModerate;
    private static Image dmgHeavy;
    private static Image dmgCrippled;
    private static Image SmokeSml;
    private static Image SmokeMed;
    private static Image SmokeLrg;
    private static Image SmokeFireSml;
    private static Image SmokeFireMed;
    private static Image SmokeFireLrg;
    private static Image dmgEmpty;
    private static boolean decalLoaded = false;

    // Individual entity images
    private Image base;
    private Image wreck;
    
    /** A smaller icon used for the unit overview. */
    private Image icon;
    /** A color used instead of a camo. */
    int tint;
    private Image camo;
    private Image[] facings = new Image[6];
    private Image[] wreckFacings = new Image[6];
    private Component parent;
    /** The damage level, from none to crippled. */
    private int dmgLevel;
    /** The tonnage of the unit. */
    private double weight;
    /** True for units of class or subclass of Infantry. */
    private boolean isInfantry;
    /** True when the image is for an additional hex of multi-hex units. */
    private boolean isSecondaryPos;
    /** True when the image is for the lobby. */
    private boolean isPreview;
    /** True when the unit is likely to be more long than wide (e.g. tanks). */
    private boolean isSlim;
    /** True when the unit is likely to be very narrow (VTOL). */
    private boolean isVerySlim;
    

    private final int IMG_WIDTH = HexTileset.HEX_W;
    private final int IMG_HEIGHT = HexTileset.HEX_H;
    private final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;

    public EntityImage(Image base, int tint, Image camo, Component comp, Entity entity) {
        this(base, null, tint, camo, comp, entity, -1, true);
    }
    
    public EntityImage(Image base, Image wreck, int tint, Image camo,
            Component comp, Entity entity, int secondaryPos) {
        this(base, wreck, tint, camo, comp, entity, secondaryPos, false);
    }
    
    public EntityImage(Image base, Image wreck, int tint, Image camo,
            Component comp, Entity entity, int secondaryPos, boolean preview) {
        this.base = base;
        this.tint = tint;
        this.camo = camo;
        parent = comp;
        this.wreck = wreck;
        this.dmgLevel = entity.getDamageLevel();
        this.weight = entity.getWeight();
        isInfantry = entity instanceof Infantry;
        isSecondaryPos = secondaryPos != 0 && secondaryPos != -1;
        isPreview = preview;
        isSlim = entity instanceof Tank || entity instanceof Aero;
        isVerySlim = entity instanceof VTOL;
    }

    public Image getCamo() {
        return camo;
    }
    
    public int getDmgLvl() {
        return dmgLevel;
    }

    /** Creates images applying damage decals, rotating and scaling. */
    public void loadFacings() {
        if (base == null) {
            return;
        }
        
        // Apply the player/unit camo or color
        base = applyColor(base);
        
        // Save a small icon (without damage decals) for the unit overview
        icon = ImageUtil.getScaledImage(base,  56, 48);
        
        // All hexes of a multi-hex unit get scars; also in the lobby
        if (!isInfantry && GUIPreferences.getInstance().getShowDamageDecal()) {
            base = applyDamageDecal(base);
        }
        
        // Only the center hex in multi-hex units gets smoke,
        // as the other hexes sometimes contain only small parts of the unit
        // No smoke in the lobby
        if (!isInfantry 
                && !isSecondaryPos 
                && !isPreview 
                && GUIPreferences.getInstance().getShowDamageDecal()) {
            base = applyDamageSmoke(base);
        }
        
        // Generate rotated images for the unit and for a wreck
        for (int i = 0; i < 6; i++) {
            facings[i] = rotateImage(base, i);
        }

        if (wreck != null) {
            wreck = applyColor(wreck);
            for (int i = 0; i < 6; i++) {
                wreckFacings[i] = rotateImage(wreck, i);
            }
        }
    }
    
    /** Rotates a given unit image into direction dir. */
    private BufferedImage rotateImage(Image img, int dir) {
        double cx = base.getWidth(parent) / 2.0;
        double cy = base.getHeight(parent) / 2.0;
        AffineTransformOp xform = new AffineTransformOp(
                AffineTransform.getRotateInstance(
                        (-Math.PI / 3) * (6 - dir), cx, cy),
                AffineTransformOp.TYPE_BICUBIC);
        BufferedImage src;
        if (img instanceof BufferedImage) {
            src = (BufferedImage) img;
        } else {
            src = ImageUtil.createAcceleratedImage(img);
        }
        BufferedImage dst = ImageUtil.createAcceleratedImage(
                src.getWidth(), src.getHeight());
        xform.filter(src, dst);
        return dst;
    }

    public Image getFacing(int facing) {
        return facings[facing];
    }

    public Image getWreckFacing(int facing) {
        return wreckFacings[facing];
    }

    public Image getBase() {
        return base;
    }

    public Image getIcon() {
        return icon;
    }

    private Image applyColor(Image image) {
        if (image == null) {
            return null;
        }
        boolean useCamo = (camo != null);
        
        // Prepare the images for access
        int[] pMech = new int[IMG_SIZE];
        int[] pCamo = new int[IMG_SIZE];
        try {
            grabImagePixels(image, pMech);
            if (useCamo) {
                grabImagePixels(camo, pCamo);
            }
        } catch (Exception e) {
            System.err.println("TilesetManager.EntityImage: " //$NON-NLS-1$
                    + "Failed to grab pixels for image. " //$NON-NLS-1$
                    + e.getMessage());
            return image;
        }

        // Overlay the camo or color  
        for (int i = 0; i < IMG_SIZE; i++) {
            int pixel = pMech[i];
            int alpha = (pixel >> 24) & 0xff;
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = (pixel) & 0xff;
            
            // Don't apply the camo over colored (not gray) pixels
            if (!(red == green && green == blue)) {
                continue;
            }
            
            // Apply the camo only on the icon pixels, not on transparent pixels
            if (alpha != 0) {
                int pixel1 = useCamo ? pCamo[i] : tint;
                int red1 = (pixel1 >> 16) & 0xff;
                int green1 = (pixel1 >> 8) & 0xff;
                int blue1 = (pixel1) & 0xff;

                int red2 = red1 * blue / 255;
                int green2 = green1 * blue / 255;
                int blue2 = blue1 * blue / 255;

                pMech[i] = (alpha << 24) | (red2 << 16) | (green2 << 8) | blue2;
            }
        }
        
        Image result = parent.createImage(new MemoryImageSource(IMG_WIDTH,
                IMG_HEIGHT, pMech, 0, IMG_WIDTH));
        return ImageUtil.createAcceleratedImage(result);
    }
    
    /** Applies decal images based on the damage and weight of the unit. */
    private Image applyDamageDecal(Image image) {
        if (image == null) {
            return null;
        }
        
        if (!decalLoaded) {
            loadDecals();
        }

        // Get the damage decal; will be null for undamaged
        Image dmgDecal = getDamageDecal();
        if (dmgDecal == null) {
            return image;
        }
        
        // Prepare the images for access
        int[] pUnit = new int[IMG_SIZE];
        int[] pDmgD = new int[IMG_SIZE];
        try {
            grabImagePixels(image, pUnit);
            grabImagePixels(dmgDecal, pDmgD);
        } catch (Exception e) {
            System.err.println("TilesetManager.EntityImage: " //$NON-NLS-1$
                    + "Failed to grab pixels for image. " //$NON-NLS-1$
                    + e.getMessage());
            return image;
        }

        // Overlay the damage decal where the unit image 
        // is not transparent
        for (int i = 0; i < IMG_SIZE; i++) {
            int alp = (pUnit[i] >> 24) & 0xff;
            int alpD = (pDmgD[i] >> 24) & 0xff;
            
            if (alp != 0 && alpD != 0) {
                int red = (pUnit[i] >> 16) & 0xff;
                int grn = (pUnit[i] >> 8) & 0xff;
                int blu = (pUnit[i]) & 0xff;
                int redD = (pDmgD[i] >> 16) & 0xff;
                int grnD = (pDmgD[i] >> 8) & 0xff;
                int bluD = (pDmgD[i]) & 0xff;

                red = Math.min(255, (red * (255 - alpD) + redD * alpD ) / 255);
                grn = Math.min(255, (grn * (255 - alpD) + grnD * alpD ) / 255);
                blu = Math.min(255, (blu * (255 - alpD) + bluD * alpD ) / 255);
                
                pUnit[i] = (alp << 24) | (red << 16) | (grn << 8) | blu;
            }
        }
        
        Image temp = parent.createImage(new MemoryImageSource(IMG_WIDTH,
                IMG_HEIGHT, pUnit, 0, IMG_WIDTH));
        return ImageUtil.createAcceleratedImage(temp);
    }
    
    /** Applies decal images based on the damage and weight of the unit. */
    private Image applyDamageSmoke(Image image) {
        if (image == null) {
            return null;
        }
        
        if (!decalLoaded) {
            loadDecals();
        }

        // Get the smoke image for heavier damage; is transparent for lighter damage
        Image smokeImg = chooseSmokeOverlay();
        if (smokeImg == null) {
            System.err.println("TilesetManager.EntityImage: " //$NON-NLS-1$
                    + "Smoke decal image is null."); //$NON-NLS-1$
            return image;
        }
        
        // Overlay the smoke image
        Image result = ImageUtil.createAcceleratedImage(base);
        Graphics g = result.getGraphics();
        g.drawImage(smokeImg, 0, 0, null);
        
        return result;
    }
    
    /** Inititates the PixelGrabber for the given image and int array. */
    private void grabImagePixels(Image img, int[] pixels) 
    throws InterruptedException, RuntimeException {
        PixelGrabber pg = new PixelGrabber(img, 0, 0, IMG_WIDTH,
                IMG_HEIGHT, pixels, 0, IMG_WIDTH);
        pg.grabPixels();
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            throw new RuntimeException("ImageObserver aborted.");
        }
    }
    
    /** Returns the smoke overlay or a transparent image based on damage level and weight. */
    private Image chooseSmokeOverlay() {
        if (dmgLevel == Entity.DMG_NONE 
                || dmgLevel == Entity.DMG_LIGHT
                || dmgLevel == Entity.DMG_MODERATE) {
            return dmgEmpty;
        }
        
        if (weight > SMOKE_THREE && !isSlim) {
            return dmgLevel == Entity.DMG_HEAVY ? SmokeLrg : SmokeFireLrg;
        } else if (weight > SMOKE_TWO && !isVerySlim) {
            return dmgLevel == Entity.DMG_HEAVY ? SmokeMed : SmokeFireMed;
        } else {
            return dmgLevel == Entity.DMG_HEAVY ? SmokeSml : SmokeFireSml;
        }
    }

    /** Returns the damage decal based on damage level. */
    private Image getDamageDecal() {
        switch (dmgLevel) {
        case Entity.DMG_LIGHT:
            return dmgLight;
        case Entity.DMG_MODERATE:
            return dmgModerate;
        case Entity.DMG_HEAVY:
            return dmgHeavy;
        case Entity.DMG_CRIPPLED:
            return dmgCrippled;
        default: // DMG_NONE:
            return null;
        }
    }
    
    /** Loads the damage decal images. */
    private static void loadDecals() {
        // Damage scars
        dmgLight = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILENAME_DAMAGEDECAL_LIGHT.toString());
        dmgModerate = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILENAME_DAMAGEDECAL_MODERATE.toString());
        dmgHeavy = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILENAME_DAMAGEDECAL_HEAVY.toString());
        dmgCrippled = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILENAME_DAMAGEDECAL_CRIPPLED.toString());
        
        // Smoke and fire
        SmokeSml = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILE_SMOKE_SML.toString());
        SmokeMed = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILE_SMOKE_MED.toString());
        SmokeLrg = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILE_SMOKE_LRG.toString());
        SmokeFireSml = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILE_SMOKEFIRE_SML.toString());
        SmokeFireMed = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILE_SMOKEFIRE_MED.toString());
        SmokeFireLrg = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILE_SMOKEFIRE_LRG.toString());
        
        // A transparent image (helper)
        dmgEmpty = TilesetManager.LoadSpecificImage(Configuration.imagesDir(), FILE_DAMAGEDECAL_EMPTY.toString());
        
        decalLoaded = true;
    }
    
}
