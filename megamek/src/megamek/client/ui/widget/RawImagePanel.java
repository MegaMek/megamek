/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.widget;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.VolatileImage;

import javax.swing.JComponent;

/**
 * @author drake
 * 
 * A panel that displays a raw image, scaling it to fit the component's size.
 */
public class RawImagePanel extends JComponent {
    private Image originalImage;
    private VolatileImage cachedImage;
    private Dimension lastSize;

    public RawImagePanel(Image image) {
        setImage(image);
        setDoubleBuffered(true);
        setOpaque(false);
    }

    public void setImage(Image image) {
        this.originalImage = image;
        invalidateCache();
        
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        } else {
            setPreferredSize(new Dimension(0, 0));
        }
        revalidate();
        repaint();
    }

    private void invalidateCache() {
        if (cachedImage != null) {
            cachedImage.flush();
            cachedImage = null;
        }
        lastSize = null;
    }

    private VolatileImage createScaledImage(int width, int height) {
        if (originalImage == null || width <= 0 || height <= 0) {
            return null;
        }

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        
        VolatileImage scaledImage = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
        
        do {
            int validationCode = scaledImage.validate(gc);
            if (validationCode == VolatileImage.IMAGE_RESTORED) {
                // Image was restored, need to re-render
                renderScaledImage(scaledImage, width, height);
            } else if (validationCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                // Image is incompatible, create a new one
                scaledImage.flush();
                scaledImage = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
                renderScaledImage(scaledImage, width, height);
            }
        } while (scaledImage.contentsLost());

        if (scaledImage.validate(gc) == VolatileImage.IMAGE_OK) {
            renderScaledImage(scaledImage, width, height);
        }

        return scaledImage;
    }

        private void renderScaledImage(VolatileImage targetImage, int width, int height) {
        Graphics2D g2d = targetImage.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            
            // Clear the image
            g2d.setComposite(java.awt.AlphaComposite.Clear);
            g2d.fillRect(0, 0, width, height);
            g2d.setComposite(java.awt.AlphaComposite.SrcOver);
            
            // Draw the scaled image
            g2d.drawImage(originalImage, 0, 0, width, height, null);
        } finally {
            g2d.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (originalImage == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Dimension currentSize = new Dimension(getWidth(), getHeight());
        if (cachedImage == null || lastSize == null || !lastSize.equals(currentSize)) {
            invalidateCache();
            cachedImage = createScaledImage(getWidth(), getHeight());
            lastSize = currentSize;
        }
        // Validate the cached image before drawing
        if (cachedImage != null) {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc != null) {
                int validationCode = cachedImage.validate(gc);
                if (validationCode == VolatileImage.IMAGE_RESTORED || 
                    validationCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                    // Recreate the image if it was lost or is incompatible
                    cachedImage.flush();
                    cachedImage = createScaledImage(getWidth(), getHeight());
                }
            }
        }

        // Draw the cached image
        if (cachedImage != null && !cachedImage.contentsLost()) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                // Final draw
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.drawImage(cachedImage, 0, 0, null);
            } finally {
                g2d.dispose();
            }
        } else {
            // Fallback to direct drawing if cached image is lost
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.drawImage(originalImage, 0, 0, getWidth(), getHeight(), null);
            } finally {
                g2d.dispose();
            }
        }
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        invalidateCache();
    }
}
