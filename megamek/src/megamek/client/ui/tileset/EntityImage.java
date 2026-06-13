/*
 * Copyright (C) 2002-2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.image.RescaleOp;
import java.io.File;
import java.util.Iterator;
import java.util.Objects;
import javax.swing.ImageIcon;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.PlayerColour;
import megamek.codeUtilities.MathUtility;
import megamek.common.Configuration;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.Infantry;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.AbstractDirectory;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.fileUtils.ImageFileFactory;
import megamek.logging.MMLogger;

/** Handles the rotated and damaged and preview images for a unit. */
public class EntityImage {
    private static final MMLogger logger = MMLogger.create(EntityImage.class);

    // Control values for applying bigger and smaller smoke
    private static final int SMOKE_THREE = 70;
    private static final int SMOKE_TWO = 40;

    // Damage decal images
    private static final File DECAL_PATH = new File(Configuration.imagesDir(), "units/DamageDecals");
    private static final File FILE_DAMAGE_DECAL_EMPTY = new File("Transparent.png");

    // Directory paths within DECAL_PATH
    private static final String PATH_FIRE1 = "Fire1/";
    private static final String PATH_FIRE2 = "Fire2/";
    private static final String PATH_FIRE3 = "Fire3/";
    private static final String PATH_FIRE_MULTI = "FireMulti/";

    private static final String PATH_SMOKE1 = "Smoke1/";
    private static final String PATH_SMOKE2 = "Smoke2/";
    private static final String PATH_SMOKE3 = "Smoke3/";
    private static final String PATH_SMOKE_MULTI = "SmokeMulti/";

    private static final String PATH_LIGHT = "Light/";
    private static final String PATH_MODERATE = "Moderate/";
    private static final String PATH_HEAVY = "Heavy/";
    private static final String PATH_CRIPPLED = "Crippled/";

    /** A transparent image used as a no-damage decal. */
    private static final Image dmgEmpty;

    private static final int[] X_POS = { 0, 0, 63, 63, 0, -63, -63 };
    private static final int[] Y_POS = { 0, -72, -36, 36, 72, 36, -36 };

    private static final int IMG_WIDTH = HexTileset.HEX_W;
    private static final int IMG_HEIGHT = HexTileset.HEX_H;
    private static final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;

    private static final float SHADOW_INTENSITY = 0.7f; // 0 = no shadow, 1 = black shadow
    private static final float SHADOW_OFFSET = 5f; // due to unit height
    private static final RescaleOp BLACK_FILTER = new RescaleOp(new float[] { 0, 0, 0, SHADOW_INTENSITY },
          new float[] { 0, 0, 0, 1 - SHADOW_INTENSITY }, null);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final GraphicsConfiguration GRAPHICS_CONFIGURATION = GraphicsEnvironment
          .getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

    /** Facing-dependent camo overlays (add shadows and highlighting) */
    private static final int[][] pOverlays = new int[6][IMG_SIZE];

    static {
        try {
            for (int i = 0; i < 6; i++) {
                var overlay = new ImageIcon(Configuration.miscImagesDir() + "/camo_overlay" + i + ".png");
                grabImagePixels(overlay.getImage(), pOverlays[i]);
            }
        } catch (Exception e) {
            logger.error("Failed to grab pixels for the camo overlay.{}", e.getMessage());
        }
    }

    /** All damage decal/fire/smoke files in DECAL_PATH. */
    private static AbstractDirectory DecalImages;

    static {
        try {
            DecalImages = new DirectoryItems(DECAL_PATH, new ImageFileFactory());
        } catch (Exception e) {
            DecalImages = null;
            logger.warn("Failed to find the damage decal images.{}", e.getMessage());
        }
        dmgEmpty = TilesetManager.loadSpecificImage(DECAL_PATH, FILE_DAMAGE_DECAL_EMPTY.toString());
    }

    /** The base (unit) image used for this icon. */
    protected Image base;
    /** The wreck base image used for this icon. */
    private Image wreck;
    /** The damage decal image used for this icon. */
    private final Image decal;
    /** The smoke image used for this icon. */
    private final Image smoke;
    /** A smaller icon used for the unit overview. */
    protected Image icon;
    private Camouflage camouflage;
    protected Image[] facings = new Image[6];
    private final Image[] wreckFacings = new Image[6];
    /** The damage level, from none to crippled. */
    private final int dmgLevel;
    /** The tonnage of the unit. */
    private final double weight;
    /** True for units of class or subclass of Infantry. */
    private final boolean isInfantry;
    /** True when the image is for the lobby. */
    private final boolean isPreview;
    /** True when the unit is likely to be more long than wide (e.g. tanks). */
    private final boolean isSlim;
    /** True when the unit is likely to be very narrow (VTOL). */
    private final boolean isVerySlim;
    /** The position in multi-hex units. */
    private final int pos;
    /** True for units that occupy one hex (all but some dropships). */
    private final boolean isSingleHex;
    private final int unitHeight;
    private final int unitElevation;
    private final boolean withShadows;

    public static EntityImage createIcon(Image base, Camouflage camouflage, Entity entity) {
        return createIcon(base, null, camouflage, entity, -1, true, true);
    }

    public static EntityImage createIcon(Image base, Camouflage camouflage, Entity entity, boolean withShadows) {
        return createIcon(base, null, camouflage, entity, -1, true, withShadows);
    }

    public static EntityImage createLobbyIcon(Image base, Camouflage camouflage, Entity entity) {
        return createIcon(base, null, camouflage, entity, -1, true, true);
    }

    public static EntityImage createIcon(Image base, Image wreck, Camouflage camouflage,
          Entity entity, int secondaryPos) {
        return createIcon(base, wreck, camouflage, entity, secondaryPos, false, true);
    }

    public static EntityImage createIcon(Image base, Image wreck, Camouflage camouflage,
          Entity entity, int secondaryPos, boolean preview, boolean withShadows) {
        if (entity instanceof FighterSquadron) {
            return new FighterSquadronIcon(base, wreck, camouflage, entity, secondaryPos, preview, withShadows);
        } else {
            return new EntityImage(base, wreck, camouflage, entity, secondaryPos, preview, withShadows);
        }
    }

    public EntityImage(Image base, Camouflage camouflage, Component comp, Entity entity) {
        this(base, null, camouflage, entity, -1, true, true);
    }

    public EntityImage(Image base, Image wreck, Camouflage camouflage, Component comp, Entity entity,
          int secondaryPos) {
        this(base, wreck, camouflage, entity, secondaryPos, false, true);
    }

    public EntityImage(Image base, Image wreck, Camouflage camouflage,
          Entity entity, int secondaryPos, boolean preview, boolean withShadows) {
        this(base, wreck, camouflage, null, entity, secondaryPos, preview, withShadows);
    }

    public EntityImage(Image base, Image wreck, Camouflage camouflage, Component comp, Entity entity, int secondaryPos,
          boolean preview, boolean withShadows) {
        this.base = base;
        setCamouflage(camouflage);
        this.wreck = wreck;
        this.withShadows = withShadows;
        this.dmgLevel = calculateDamageLevel(entity);
        // hack: gun emplacements are pretty beefy but have weight 0
        this.weight = entity.isBuildingEntityOrGunEmplacement() ? SMOKE_THREE + 1 : entity.getWeight();
        isInfantry = entity instanceof Infantry;
        // True for tanks
        boolean isTank = entity instanceof Tank;
        isPreview = preview;
        isSlim = (isTank && !(entity instanceof GunEmplacement));
        isVerySlim = entity instanceof VTOL;
        pos = secondaryPos;
        isSingleHex = secondaryPos == -1;
        decal = getDamageDecal(entity, secondaryPos);
        smoke = getSmokeImage(entity, secondaryPos);
        unitHeight = entity.height();
        unitElevation = entity.getElevation();
    }

    /**
     * Worker function that calculates the entity's damage level for the purposes of displaying damage to avoid
     * particularly dumb-looking situations
     */
    private int calculateDamageLevel(Entity entity) {
        // gun emplacements don't show up as crippled when destroyed, which leads to
        // them looking pristine
        if ((entity.isBuildingEntityOrGunEmplacement()) && entity.isDestroyed()) {
            return Entity.DMG_CRIPPLED;
        }

        // aerospace fighters where the pilot ejects look pretty dumb without any damage
        // decals so let's give them at least some damage
        if (entity.isAirborne() && entity.getCrew().isEjected()) {
            return Math.max(Entity.DMG_HEAVY, entity.getDamageLevel(false));
        }

        return getCalculatedDamageLevel(entity);

    }

    private static int getCalculatedDamageLevel(Entity entity) {
        int calculatedDamageLevel = entity.getDamageLevel();

        // entities may be "damaged" or "crippled" due to harmless weapon jams, being
        // out of ammo or otherwise but not having taken any actual damage. In this
        // case, it looks stupid for the entity to be all shot up, so we pretend there's
        // no damage.
        if ((calculatedDamageLevel > Entity.DMG_NONE) &&
              (entity.getArmorRemainingPercent() >= 1.0) &&
              (entity.getInternalRemainingPercent() >= 1.0)) {
            calculatedDamageLevel = Entity.DMG_NONE;
        }
        return calculatedDamageLevel;
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public void setCamouflage(Camouflage camouflage) {
        this.camouflage = Objects.requireNonNull(camouflage);
    }

    public int getDmgLvl() {
        return dmgLevel;
    }

    /** Creates images applying damage decals, rotating and scaling. */
    public void loadFacings() {
        if (base == null) {
            return;
        }

        for (int i = 0; i < 6; i++) {
            // Apply the player/unit camouflage
            Image fImage = applyColor(base, i);

            // Add damage scars and smoke/fire; not to Infantry
            if (!isInfantry && GUIP.getShowDamageDecal()) {
                fImage = applyDamageDecal(fImage);
                // No smoke in the lobby
                if (!isPreview) {
                    fImage = applyDamageSmoke(fImage);
                }
            }

            // Generate rotated images for the unit and for a wreck
            fImage = rotateImage(fImage, i);
            if (GUIP.getShadowMap() && isSingleHex && withShadows) {
                facings[i] = applyDropShadow(fImage);
            } else {
                facings[i] = fImage;
            }
        }

        // Apply the player/unit camouflage
        base = applyColor(base, 0);

        // Save a small icon (without damage decals) for the unit overview
        icon = ImageUtil.getScaledImage(base, 56, 48);

        if (wreck != null) {
            wreck = applyColor(wreck, 0);

            // Add damage scars and smoke/fire; not to Infantry
            if (!isInfantry && GUIP.getShowDamageDecal()) {
                wreck = applyDamageDecal(wreck);
                // No smoke in the lobby
                if (!isPreview) {
                    wreck = applyDamageSmoke(wreck);
                }
            }

            for (int i = 0; i < 6; i++) {
                wreckFacings[i] = rotateImage(wreck, i);
            }
        }
    }

    /** Rotates a given unit image into direction dir. */
    protected BufferedImage rotateImage(Image img, int dir) {
        double cx = base.getWidth(null) / 2.0;
        double cy = base.getHeight(null) / 2.0;
        AffineTransformOp affineTransform = new AffineTransformOp(
              AffineTransform.getRotateInstance((-Math.PI / 3) * (6 - dir), cx, cy), AffineTransformOp.TYPE_BICUBIC);

        BufferedImage src;
        if (img instanceof BufferedImage bufferedImage) {
            src = bufferedImage;
        } else {
            src = ImageUtil.createAcceleratedImage(img);
        }

        if (src != null) {
            BufferedImage dst = ImageUtil.createAcceleratedImage(src.getWidth(), src.getHeight());
            affineTransform.filter(src, dst);
            return dst;
        }

        return null;
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

    public @Nullable Image loadPreviewImage(final boolean showDamage) {
        if (base == null) {
            return null;
        }

        base = applyColor(getBase(), 0);

        // Add damage scars and smoke/fire; not to Infantry
        if (showDamage && !isInfantry && GUIP.getShowDamageDecal()) {
            base = applyDamageDecal(getBase());
            // No smoke in the lobby
            if (!isPreview) {
                base = applyDamageSmoke(getBase());
            }
        }
        return getBase();
    }

    /** Applies the unit individual or player camouflage to the icon. */
    protected Image applyColor(Image image, int facing) {
        if (image == null) {
            return null;
        }
        final boolean hasCamouflage = !getCamouflage().hasDefaultCategory();
        final boolean colourCamouflage = getCamouflage().isColourCamouflage();
        final int colour = colourCamouflage ? PlayerColour.parseFromString(getCamouflage().getFilename()).getHex() : -1;

        // Prepare the images for access
        int[] pMek = new int[IMG_SIZE];
        int[] pCamo = new int[IMG_SIZE];
        try {
            grabImagePixels(image, pMek);
            if (!colourCamouflage && hasCamouflage) {
                grabImagePixels(getCamouflage().getImage(), pCamo);
            }
        } catch (Exception ex) {
            logger.error(ex, "Failed to grab pixels for an image to apply the camo.");
            return image;
        }

        if (hasCamouflage) {
            // Overlay the camo or color
            for (int i = 0; i < IMG_SIZE; i++) {
                int pixel = pMek[i];
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
                    int pixel1 = colourCamouflage ? colour
                          : rotatedAndScaledColor(i, getCamouflage().getRotationRadians(),
                          getCamouflage().getScaleFactor(), pCamo);
                    int red1 = (pixel1 >> 16) & 0xff;
                    int green1 = (pixel1 >> 8) & 0xff;
                    int blue1 = (pixel1) & 0xff;

                    // Pretreat with the camo overlay (but not Infantry, they're too small, it'll just darken them)
                    int overlayAlpha = 128;
                    if (GUIP.getUseCamoOverlay() && !isInfantry && isSingleHex) {
                        overlayAlpha = pOverlays[facing][i] & 0xff;
                    }

                    // "Overlay" image combination formula
                    if (overlayAlpha < 128) {
                        red1 = red1 * 2 * overlayAlpha / 255;
                        green1 = green1 * 2 * overlayAlpha / 255;
                        blue1 = blue1 * 2 * overlayAlpha / 255;
                    } else {
                        red1 = 255 - 2 * (255 - red1) * (255 - overlayAlpha) / 255;
                        green1 = 255 - 2 * (255 - green1) * (255 - overlayAlpha) / 255;
                        blue1 = 255 - 2 * (255 - blue1) * (255 - overlayAlpha) / 255;
                    }

                    int red2 = red1 * blue / 255;
                    int green2 = green1 * blue / 255;
                    int blue2 = blue1 * blue / 255;
                    pMek[i] = (alpha << 24) | (red2 << 16) | (green2 << 8) | blue2;
                }
            }
        }
        ImageProducer producer = new MemoryImageSource(IMG_WIDTH, IMG_HEIGHT, pMek, 0, IMG_WIDTH);
        Image result = Toolkit.getDefaultToolkit().createImage(producer);
        return ImageUtil.createAcceleratedImage(result);
    }

    /**
     * Returns a color from the given camoImage lookup array where the given lookup index is rotated by the given angle
     * and scaled by the given scale.
     *
     * @param originalIndex The original lookup index (0 ... 84 x 72 - 1)
     * @param angle         The rotation angle in radians (0 = no change)
     * @param scale         The scale factor (1 = no change)
     * @param camoImage     The image pixel grabber lookup array
     *
     * @return The rotated and scaled image pixel color as an int value (as from the image lookup array)
     */
    private int rotatedAndScaledColor(int originalIndex, double angle, double scale, int[] camoImage) {
        // get the pixel coordinates
        int y = originalIndex / 84;
        int x = originalIndex - y * 84;
        // center coordinates to rotate around the image center
        double cy = y - 35.5;
        double cx = x - 41.5;
        // rotate, scale and remove centering
        double ry = (Math.sin(angle) * cx + Math.cos(angle) * cy) / scale + 35.5;
        double rx = (Math.cos(angle) * cx - Math.sin(angle) * cy) / scale + 41.5;
        // interpolate between the four surrounding actual image pixels
        int rx1 = (int) rx;
        int rx2 = rx1 + 1;
        int ry1 = (int) ry;
        int ry2 = ry1 + 1;
        double dx = rx - rx1;
        double dy = ry - ry1;
        int pixel11 = camoImage[mirroredIndex(rx1, ry1)];
        int red11 = (pixel11 >> 16) & 0xff;
        int green11 = (pixel11 >> 8) & 0xff;
        int blue11 = (pixel11) & 0xff;

        int pixel12 = camoImage[mirroredIndex(rx1, ry2)];
        int red12 = (pixel12 >> 16) & 0xff;
        int green12 = (pixel12 >> 8) & 0xff;
        int blue12 = (pixel12) & 0xff;

        int pixel21 = camoImage[mirroredIndex(rx2, ry1)];
        int red21 = (pixel21 >> 16) & 0xff;
        int green21 = (pixel21 >> 8) & 0xff;
        int blue21 = (pixel21) & 0xff;

        int pixel22 = camoImage[mirroredIndex(rx2, ry2)];
        int red22 = (pixel22 >> 16) & 0xff;
        int green22 = (pixel22 >> 8) & 0xff;
        int blue22 = (pixel22) & 0xff;

        int redX1 = MathUtility.lerp(red11, red21, dx);
        int redX2 = MathUtility.lerp(red12, red22, dx);
        int red2 = MathUtility.lerp(redX1, redX2, dy);

        int greenX1 = MathUtility.lerp(green11, green21, dx);
        int greenX2 = MathUtility.lerp(green12, green22, dx);
        int green2 = MathUtility.lerp(greenX1, greenX2, dy);

        int blueX1 = MathUtility.lerp(blue11, blue21, dx);
        int blueX2 = MathUtility.lerp(blue12, blue22, dx);
        int blue2 = MathUtility.lerp(blueX1, blueX2, dy);
        return (red2 << 16) | (green2 << 8) | blue2;
    }

    /**
     * Returns a valid lookup index for the pixel array for any values of x and y. The lookup is calculated so that the
     * image is mirrored vertically and horizontally to avoid unnecessary seams at the image border. Note that for a
     * pixel at (0,0) the pixel color extends from -0.5, -0.5 to +0.5, +0.5.
     *
     * @param x the image x coordinate (any value allowed)
     * @param y the image y coordinate (any value allowed)
     *
     * @return a lookup within allowed values (0 ... 84 x 72 - 1)
     */
    private int mirroredIndex(double x, double y) {
        // double x values may range from -0.5 to 83.5
        if (x <= -0.5) {
            x = -1 - x;
        }
        if (x > 0) {
            x = x % 167; // 2 * (84 - 1) + 1
        }
        if (x >= 83.5) {
            x = 167 - x;
        }
        // double y values may range from -0.5 to 71.5
        if (y <= -0.5) {
            y = -1 - y;
        }
        if (y > 0) {
            y = y % 143; // 2 * (72 - 1) + 1
        }
        if (y >= 71.5) {
            y = 143 - y;
        }
        return (int) (Math.round(x) + Math.round(y) * 84);
    }

    /** Applies the damage decal image to the icon. */
    private Image applyDamageDecal(Image image) {
        if (image == null) {
            return null;
        }

        // Get the damage decal; will be null for undamaged
        if (decal == null) {
            return image;
        }

        // Prepare the images for access
        int[] pUnit = new int[IMG_SIZE];
        int[] pDmgD = new int[IMG_SIZE];
        try {
            grabImagePixels(image, pUnit);
            grabImagePixels(decal, pDmgD);
        } catch (Exception e) {
            logger.error("Failed to grab pixels for an image to apply the decal. {}", e.getMessage());
            return image;
        }

        // Overlay the damage decal where the unit image
        // is not transparent
        for (int i = 0; i < IMG_SIZE; i++) {
            int alp = (pUnit[i] >> 24) & 0xff;
            int alpD = (pDmgD[i] >> 24) & 0xff;

            // Don't apply the decal over semi-transparent pixels
            // as these are normally the drop shadow
            if (alp > 220 && alpD != 0) {
                int red = (pUnit[i] >> 16) & 0xff;
                int grn = (pUnit[i] >> 8) & 0xff;
                int blu = (pUnit[i]) & 0xff;
                int redD = (pDmgD[i] >> 16) & 0xff;
                int grnD = (pDmgD[i] >> 8) & 0xff;
                int bluD = (pDmgD[i]) & 0xff;

                red = Math.min(255, (red * (255 - alpD) + redD * alpD) / 255);
                grn = Math.min(255, (grn * (255 - alpD) + grnD * alpD) / 255);
                blu = Math.min(255, (blu * (255 - alpD) + bluD * alpD) / 255);

                pUnit[i] = (alp << 24) | (red << 16) | (grn << 8) | blu;
            }
        }

        ImageProducer producer = new MemoryImageSource(IMG_WIDTH, IMG_HEIGHT, pUnit, 0, IMG_WIDTH);
        Image temp = Toolkit.getDefaultToolkit().createImage(producer);
        return ImageUtil.createAcceleratedImage(temp);
    }

    /** Applies the smoke/fire image to the icon. */
    private Image applyDamageSmoke(Image image) {
        if (image == null) {
            return null;
        }

        // Get the smoke image for heavier damage; is transparent for lighter damage
        if (smoke == null) {
            logger.error("Smoke decal image is null.");
            return image;
        }

        // Overlay the smoke image
        Image result = ImageUtil.createAcceleratedImage(image);
        if (result != null) {
            Graphics graphics = result.getGraphics();
            if (isSingleHex) {
                graphics.drawImage(smoke, 0, 0, null);
            } else {
                // Draw the right section of the bigger smoke/fire image
                int smokeWidth = smoke.getWidth(null) / 2 - IMG_WIDTH / 2 + X_POS[pos];
                int smokeHeight = smoke.getHeight(null) / 2 - IMG_HEIGHT / 2 + Y_POS[pos];
                graphics.drawImage(smoke,
                      0,
                      0,
                      IMG_WIDTH,
                      IMG_HEIGHT,
                      smokeWidth,
                      smokeHeight,
                      smokeWidth + IMG_WIDTH,
                      smokeHeight + IMG_HEIGHT,
                      null);
            }
        }
        return result;
    }

    /** Initiates the PixelGrabber for the given image and int array. */
    private static void grabImagePixels(Image img, int[] pixels) throws InterruptedException, RuntimeException {
        PixelGrabber pg = new PixelGrabber(img, 0, 0, IMG_WIDTH, IMG_HEIGHT, pixels, 0, IMG_WIDTH);
        pg.grabPixels();
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            throw new RuntimeException("ImageObserver aborted.");
        }
    }

    /** Returns the damage decal based on damage level. */
    private Image getDamageDecal(Entity entity, int pos) {
        try {
            return switch (dmgLevel) {
                case Entity.DMG_LIGHT -> getIM(PATH_LIGHT, entity.getShortName(), pos);
                case Entity.DMG_MODERATE -> getIM(PATH_MODERATE, entity.getShortName(), pos);
                case Entity.DMG_HEAVY -> getIM(PATH_HEAVY, entity.getShortName(), pos);
                case Entity.DMG_CRIPPLED -> getIM(PATH_CRIPPLED, entity.getShortName(), pos);
                default -> // DMG_NONE:
                      null;
            };
        } catch (Exception e) {
            logger.error(e, "Could not load decal image.");
        }

        return null;
    }

    /**
     * @return the smoke/fire image based on damage level.
     */
    private Image getSmokeImage(Entity entity, int pos) {
        try {
            // No smoke and fire for damage up to moderate
            if (dmgLevel == Entity.DMG_NONE
                  || dmgLevel == Entity.DMG_LIGHT
                  || dmgLevel == Entity.DMG_MODERATE) {
                return dmgEmpty;
            }

            String path = getPath(pos);
            // Use the same smoke image for all positions of multi-hex units (pos = 0)!
            return getIM(path, entity.getShortName(), 0);
        } catch (Exception e) {
            logger.error(e, "Could not load smoke/fire image.");
        }
        return null;
    }

    private String getPath(int pos) {
        String path;
        if (pos > -1) {
            // Multi-hex units get their own overlays
            path = dmgLevel == Entity.DMG_HEAVY ? PATH_SMOKE_MULTI : PATH_FIRE_MULTI;
        } else {
            // Three stacks of smoke and fire for wide and heavy units,
            // two for slimmer and medium units and one for very slim
            // and light units
            if (weight > SMOKE_THREE && !isSlim) {
                path = dmgLevel == Entity.DMG_HEAVY ? PATH_SMOKE3 : PATH_FIRE3;
            } else if (weight > SMOKE_TWO && !isVerySlim) {
                path = dmgLevel == Entity.DMG_HEAVY ? PATH_SMOKE2 : PATH_FIRE2;
            } else {
                path = dmgLevel == Entity.DMG_HEAVY ? PATH_SMOKE1 : PATH_FIRE1;
            }
        }

        return path;
    }

    protected Image applyDropShadow(Image image) {
        if (image == null) {
            return null;
        }

        // Create a copy to change into a drop shadow
        BufferedImage copy = ImageUtil.getScaledImage(image, image.getWidth(null), image.getHeight(null));

        // Set the color of all pixels to black, keeping the alpha
        BufferedImage blackedOut = BLACK_FILTER.filter(copy, null);

        // Blur operation setup
        int radius = 5;
        float sigma = radius / 2f * (unitHeight + 1);
        if (unitElevation != 0) {
            radius = 1;
        }
        ConvolveOp op = new ConvolveOp(ImageUtil.getGaussKernel(2 * radius + 1, sigma), ConvolveOp.EDGE_NO_OP, null);

        // blurring requires a slightly bigger image
        BufferedImage temp = GRAPHICS_CONFIGURATION.createCompatibleImage(
              IMG_WIDTH + radius * 2, IMG_HEIGHT + radius * 2, Transparency.TRANSLUCENT);
        Graphics g = temp.getGraphics();
        g.drawImage(blackedOut, radius, radius, null);
        g.dispose();
        BufferedImage shadow = op.filter(temp, null);

        // reduce back to the correct image size
        BufferedImage result = GRAPHICS_CONFIGURATION.createCompatibleImage(IMG_WIDTH, IMG_HEIGHT,
              Transparency.TRANSLUCENT);
        Graphics gResult = result.getGraphics();
        int xOffset = 0;
        if (unitElevation == 0) {
            xOffset = (int) (SHADOW_OFFSET * (unitHeight + 1));
        }
        int yOffset = xOffset * 7 / 19; // values taken from the light direction used in terrain shadows
        gResult.drawImage(shadow, -xOffset, yOffset, IMG_WIDTH - 1 - xOffset, IMG_HEIGHT - 1 + yOffset,
              radius, radius, IMG_WIDTH + radius - 1, IMG_HEIGHT + radius - 1, null);

        // re-apply the actual icon on top of the shadow
        gResult.drawImage(image, 0, 0, null);
        gResult.dispose();
        return ImageUtil.createAcceleratedImage(result);
    }

    /**
     * @return a random image of all the images in the category (= directory) cat. To have reproducible images for
     *       individual units the image is chosen based on the hash value of the name (and the hex in multi-hex units).
     */
    private static Image getIM(String cat, String name, int pos) throws Exception {
        int img = Math.abs((name + pos).hashCode()) % getSize(DecalImages.getItemNames(cat));
        Iterator<String> iter = DecalImages.getItemNames(cat);
        String n = "";
        for (int i = 0; i <= img; i++) {
            n = iter.next();
        }
        return (Image) DecalImages.getItem(cat, n);
    }

    /**
     * @return the size of the collection of an iterator. Local helper function for DirectoryItems.
     */
    private static <T> int getSize(Iterator<T> iter) {
        int result = 0;
        for (; iter.hasNext(); iter.next()) {
            result++;
        }
        return result;
    }
}
