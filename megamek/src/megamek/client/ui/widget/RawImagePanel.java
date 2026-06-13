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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import javax.swing.JComponent;

import megamek.common.util.ManagedVolatileImage;

/**
 * @author drake
 *       <p>
 *       A panel that displays a raw image, scaling it to fit the component's size.
 */
public class RawImagePanel extends JComponent {
    private Image originalImage;
    private ManagedVolatileImage cachedImage;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (originalImage == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Dimension currentSize = new Dimension(getWidth(), getHeight());
        if (cachedImage == null || lastSize == null || !lastSize.equals(currentSize)) {
            invalidateCache();
            cachedImage = new ManagedVolatileImage(originalImage, Transparency.OPAQUE, getWidth(), getHeight());
            lastSize = currentSize;
        }
        // Draw
        if (cachedImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                // Enable high-quality rendering
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.drawImage(cachedImage.getImage(), 0, 0, null);
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
