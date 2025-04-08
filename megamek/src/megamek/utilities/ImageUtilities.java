/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
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
 */
package megamek.utilities;

import static java.lang.Math.round;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

public class ImageUtilities {
    private static final MMLogger logger = MMLogger.create(ImageUtilities.class);

    /**
     * @deprecated use {@link #scaleImageIcon(ImageIcon, int, boolean)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public static ImageIcon scaleImageIconToWidth(ImageIcon icon, int width) {
        return scaleImageIcon(icon, width, true);
    }

    /**
     * Scales an {@link ImageIcon} proportionally based on either the specified width or height.
     *
     * <p>This method preserves the aspect ratio of the original image while resizing. The size to scale
     * is determined by the {@code size} parameter, and whether scaling is based on width or height is controlled by the
     * {@code scaleByWidth} flag.</p>
     *
     * <p>If the provided {@link ImageIcon} is {@code null}, an empty {@link ImageIcon} will be returned, and
     * an error will be logged.</p>
     *
     * @param icon         The {@link ImageIcon} to be scaled. If {@code null}, an empty {@link ImageIcon} is returned.
     * @param size         The target size to scale to, either width or height depending on the {@code scaleByWidth}
     *                     flag. This value will be scaled for the GUI using {@link UIUtil#scaleForGUI(int)}.
     * @param scaleByWidth A {@code boolean} flag to determine the scaling mode:
     *                     <ul>
     *                       <li>If {@code true}, scales the image by the given width, and calculates the height
     *                           proportionally.</li>
     *                       <li>If {@code false}, scales the image by the given height, and calculates the width
     *                           proportionally.</li>
     *                     </ul>
     *
     * @return A scaled {@link ImageIcon}, resized to the specified target dimension while maintaining the aspect ratio.
     *       If the provided {@link ImageIcon} is {@code null}, returns an empty {@link ImageIcon}.
     */
    public static ImageIcon scaleImageIcon(ImageIcon icon, int size, boolean scaleByWidth) {
        if (icon == null) {
            logger.error(new NullPointerException(),
                  "ImageIcon is null in scaleImageIconHighQuality(ImageIcon, int, boolean). Returning an empty ImageIcon.");
            return new ImageIcon();
        }

        int width, height;

        if (scaleByWidth) {
            width = Math.max(1, UIUtil.scaleForGUI(size));
            height = (int) Math.ceil((double) width * icon.getIconHeight() / icon.getIconWidth());
        } else {
            height = Math.max(1, UIUtil.scaleForGUI(size));
            width = (int) Math.ceil((double) height * icon.getIconWidth() / icon.getIconHeight());
        }

        // Create a new BufferedImage with the desired dimensions
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get the Graphics2D object and set rendering hints for quality
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the scaled image with high-quality rendering
        g2d.drawImage(icon.getImage(), 0, 0, width, height, null);
        g2d.dispose();

        return new ImageIcon(scaledImage);
    }

    /**
     * Adds a default tint to the provided image with standard transparency settings.
     *
     * <p>This is a simplified utility method that applies a given tint color to only the
     * non-transparent portions of an image, with a default transparency level of 75% (25% opaque).</p>
     *
     * @param image     the {@link Image} to which the tint will be applied.
     * @param tintColor the {@link Color} used for the tint.
     *
     * @return an {@link ImageIcon} containing the tinted image.
     *
     * @see #addTintToImageIcon(Image, Color, boolean, Double) for more advanced configurations.
     */
    public static ImageIcon addTintToImageIcon(Image image, Color tintColor) {
        return addTintToImageIcon(image, tintColor, true, null);
    }

    /**
     * Adds a customizable tint to the given image, with options to control the transparency and the areas of the image
     * affected by the tint.
     *
     * <p>This method processes the input {@link Image} and applies a tint (a blend of the
     * given color and transparency) across the image. You may specify whether the tint should apply only to
     * non-transparent areas or the entire image. Additionally, the transparency level can be customized or left as the
     * default (50% transparency, 50% opaque).</p>
     *
     * @param image               the {@link Image} to which the tint will be applied.
     * @param tint                the {@link Color} used for the tint.
     * @param nonTransparentOnly  if {@code true}, applies the tint only to non-transparent areas of the image.
     *                            Otherwise, it applies the tint globally.
     * @param transparencyPercent an optional transparency level for the tint. If {@code null}, it defaults to 50%
     *                            transparency (0.5). Must be between {@code 0.0} and {@code 1.0}.
     *
     * @return an {@link ImageIcon} containing the image with the applied tint.
     *
     * @see #addTintToImageIcon(Image, Color) for default behavior.
     */
    public static ImageIcon addTintToImageIcon(Image image, Color tint, boolean nonTransparentOnly,
          @Nullable Double transparencyPercent) {
        BufferedImage tintedImage = new BufferedImage(image.getWidth(null),
              image.getHeight(null),
              BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = tintedImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);

        if (nonTransparentOnly) {
            // This applies the tint to only the non-transparent areas
            graphics.setComposite(AlphaComposite.SrcAtop);
        }

        graphics.setColor(new Color(tint.getRed(),
              tint.getGreen(),
              tint.getBlue(),
              getAlpha(transparencyPercent == null ? 0.5 : transparencyPercent)));
        graphics.fillRect(0, 0, tintedImage.getWidth(), tintedImage.getHeight());

        // Clean up, so we're not leaving objects in memory everywhere if the player is jumping
        // between a lot of images (such as the personnel table).
        graphics.dispose();

        return new ImageIcon(tintedImage);
    }

    /**
     * Applies a default tint to a {@link BufferedImage} and returns the modified image.
     *
     * <p>This method adds a tint overlay of the specified color to the provided {@link BufferedImage}.
     * By default, the tint is applied only to non-transparent areas, and a transparency level of 50% is used if none is
     * specified.
     *
     * @param image     The {@link BufferedImage} on which the tint will be applied.
     * @param tintColor The {@link Color} to use as the tint.
     *
     * @return A new {@link BufferedImage} containing the original image with the tint applied.
     *
     * @see #addTintToBufferedImage(BufferedImage, Color, boolean, Double)
     */
    public static BufferedImage addTintToBufferedImage(BufferedImage image, Color tintColor) {
        return addTintToBufferedImage(image, tintColor, true, null);
    }

    /**
     * Applies a tint to a BufferedImage and returns the modified image.
     *
     * <p>The method overlays a specified color tint on the original image. You can optionally apply
     * the tint only to non-transparent areas or specify the transparency level of the tint.
     *
     * @param image               The original {@link BufferedImage} to which the tint will be added.
     * @param tint                The {@link Color} to use as the tint.
     * @param nonTransparentOnly  If {@code true}, applies the tint only to non-transparent areas.
     * @param transparencyPercent The transparency level of the tinted overlay (0.0 to 1.0), where {@code 1.0} is fully
     *                            opaque and {@code 0.0} is fully transparent. If {@code null}, a default of 50%
     *                            transparency is applied.
     *
     * @return A new {@link BufferedImage} with the specified tint applied.
     */
    public static BufferedImage addTintToBufferedImage(BufferedImage image, Color tint, boolean nonTransparentOnly,
          @Nullable Double transparencyPercent) {
        // Create a new BufferedImage with the same dimensions and type as the original
        BufferedImage tintedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Draw the original image onto the new BufferedImage
        Graphics2D graphics = tintedImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);

        // If applying the tint only to non-transparent areas, set the appropriate composite
        if (nonTransparentOnly) {
            graphics.setComposite(AlphaComposite.SrcAtop);
        }

        // Generate a new color with the specified transparency
        int alpha = getAlpha(transparencyPercent == null ? 0.5 : transparencyPercent);
        graphics.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha));

        // Apply the tint color to the entire image
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        // Dispose of the Graphics2D object to free resources
        graphics.dispose();

        // Return the tinted BufferedImage
        return tintedImage;
    }

    /**
     * Converts a transparency percentage into an alpha value for ARGB colors.
     *
     * <p>This method maps a transparency percentage (from {@code 0.0} to {@code 1.0})
     * to an integer alpha value (from 0 to 255) usable with ARGB colors.</p>
     * <ul>
     *     <li>A value of {@code 1.0} (fully opaque) will return {@code 0} for maximum alpha.</li>
     *     <li>A value of {@code 0.0} (fully transparent) will return {@code 255} for full transparency.</li>
     * </ul>
     *
     * @param transparencyPercent A percentage representing transparency. Must be between {@code 0.0} and {@code 1.0},
     *                            inclusive.
     *
     * @return An integer alpha value ranging from 0 (fully opaque) to 255 (fully transparent), calculated from the
     *       provided percentage.
     *
     * @throws IllegalArgumentException If {@code transparencyPercent} is outside the range of {@code 0.0} to
     *                                  {@code 1.0}.
     */
    private static int getAlpha(double transparencyPercent) {
        if (transparencyPercent < 0.0 || transparencyPercent > 1.0) {
            throw new IllegalArgumentException("Transparency percent must be between 0.0 and 1.0.");
        }

        return (int) round(255 - (255 * transparencyPercent));
    }
}
