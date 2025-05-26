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
import javax.swing.JComponent;

/**
 * A panel that displays a raw image, scaling it to fit the component's size.
 */
public class RawImagePanel extends JComponent {
    private Image image;

    public RawImagePanel(Image image) {
        this.image = image;
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        } else {
            setPreferredSize(new Dimension(0, 0));
        }
    }

    public void setImage(Image image) {
        this.image = image;
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        } else {
            setPreferredSize(new Dimension(0, 0));
        }
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null && getWidth() > 0 && getHeight() > 0) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                // Apply high-quality rendering hints
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Draw the image, scaling it to the component's current bounds
                g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            } finally {
                g2d.dispose();
            }
        }
    }
}
