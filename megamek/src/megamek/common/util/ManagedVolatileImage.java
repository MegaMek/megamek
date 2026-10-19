/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.VolatileImage;

import megamek.logging.MMLogger;

/**
 * A wrapper around VolatileImage that handles validation and recreation automatically.
 */
public class ManagedVolatileImage {
    private static final MMLogger LOGGER = MMLogger.create(ManagedVolatileImage.class);

    private VolatileImage volatileImage;
    private final Image sourceImage;
    private final int width;
    private final int height;
    private final int transparency;
    private GraphicsConfiguration gc;

    @Deprecated(since = "0.51.0", forRemoval = true)
    public ManagedVolatileImage(Image sourceImage) {
        this(sourceImage, Transparency.TRANSLUCENT);
    }

    public ManagedVolatileImage(Image sourceImage, int transparency) {
        this(sourceImage, transparency, sourceImage.getWidth(null), sourceImage.getHeight(null));
    }

    public ManagedVolatileImage(Image sourceImage, int transparency, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive.");
        }
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image cannot be null.");
        }
        this.sourceImage = sourceImage;
        this.width = width;
        this.height = height;
        this.transparency = transparency;
        this.gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
              .getDefaultScreenDevice().getDefaultConfiguration();
        createVolatileImage();
    }

    private void createVolatileImage() {
        if (volatileImage != null) {
            volatileImage.flush();
        }
        volatileImage = gc.createCompatibleVolatileImage(width, height, transparency);
        LOGGER.debug("Created volatile image {}x{} on screen {}", width, height, gc.getDevice().getIDstring());
        renderToVolatileImage();
    }

    private void renderToVolatileImage() {
        Graphics2D g2d = volatileImage.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (transparency != Transparency.OPAQUE) {
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, width, height);
                g2d.setComposite(AlphaComposite.SrcOver);
            }
            g2d.drawImage(sourceImage, 0, 0, width, height, null);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Gets the VolatileImage, ensuring it's valid for the screen it was last created on. Prefer
     * {@link #getImage(GraphicsConfiguration)} when painting, so the image follows a window that moves to another
     * monitor.
     */
    public VolatileImage getImage() {
        return getImage(gc);
    }

    /**
     * Gets the VolatileImage, ensuring it's valid for the screen it is about to be drawn on.
     *
     * <p>A volatile image lives in the memory of one graphics device. Drawing it on another monitor's device can
     * show whatever that device has at the same spot, such as its font cache, so the image is recreated whenever the
     * target screen changes.</p>
     *
     * @param target the configuration of the screen being drawn on, usually the window's
     *               {@link java.awt.Component#getGraphicsConfiguration()}; {@code null} or an offscreen buffer
     *               keeps the current screen
     */
    public VolatileImage getImage(GraphicsConfiguration target) {
        if ((target != null) && isScreen(target) && (target.getDevice() != gc.getDevice())) {
            LOGGER.debug("Volatile image {}x{} moved from screen {} to screen {}; recreating it",
                  width, height, gc.getDevice().getIDstring(), target.getDevice().getIDstring());
            gc = target;
            createVolatileImage();
        }
        do {
            int validation = volatileImage.validate(gc);
            if (validation == VolatileImage.IMAGE_INCOMPATIBLE) {
                LOGGER.debug("Volatile image {}x{} is incompatible with screen {}; recreating it",
                      width, height, gc.getDevice().getIDstring());
                createVolatileImage();
            } else if (validation == VolatileImage.IMAGE_RESTORED || volatileImage.contentsLost()) {
                LOGGER.debug("Volatile image {}x{} lost its contents on screen {}; redrawing it",
                      width, height, gc.getDevice().getIDstring());
                renderToVolatileImage();
            }
        } while (volatileImage.contentsLost());
        return volatileImage;
    }

    /**
     * Swing may paint into an offscreen buffer first, whose configuration is not a monitor; only a real screen
     * should move the image.
     */
    private static boolean isScreen(GraphicsConfiguration configuration) {
        return configuration.getDevice().getType() == GraphicsDevice.TYPE_RASTER_SCREEN;
    }

    public void flush() {
        if (volatileImage != null) {
            volatileImage.flush();
        }
    }
}
