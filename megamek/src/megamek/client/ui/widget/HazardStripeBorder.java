/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import javax.swing.border.AbstractBorder;

import megamek.client.ui.util.UIUtil;

/**
 * A border of diagonal yellow and red stripes, in the manner of hazard tape.
 *
 * <p>It marks the one control in a group that the player should be able to find in a hurry: the bug reporting button
 * in its dialog, which sits beside nine buttons that only open a link. Its in-game counterpart on the command strip
 * is a {@link HazardButton}, which carries the same colours on the phase display skin instead.</p>
 */
public class HazardStripeBorder extends AbstractBorder {

    /** The yellow of the stripes. */
    public static final Color HAZARD_YELLOW = new Color(255, 204, 0);
    /** The red of the stripes. */
    public static final Color HAZARD_RED = new Color(204, 34, 34);

    private static final int UNSCALED_THICKNESS = 5;

    private final int thickness;

    /** Creates a border of the standard stripe width. */
    public HazardStripeBorder() {
        this(UNSCALED_THICKNESS);
    }

    /**
     * @param unscaledThickness the stripe width in unscaled pixels; the GUI scale is applied here, and the result is
     *                          never less than one pixel, since the stripe loop steps by the thickness
     */
    public HazardStripeBorder(int unscaledThickness) {
        thickness = Math.max(1, UIUtil.scaleForGUI(unscaledThickness));
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D stripeGraphics = (Graphics2D) graphics.create();
        try {
            Area frame = new Area(new Rectangle(x, y, width, height));
            frame.subtract(new Area(new Rectangle(x + thickness, y + thickness,
                  width - (2 * thickness), height - (2 * thickness))));
            stripeGraphics.clip(frame);
            stripeGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            stripeGraphics.setColor(HAZARD_YELLOW);
            stripeGraphics.fillRect(x, y, width, height);

            // Diagonals are drawn from off the left edge so that the stripes carry across the whole frame.
            stripeGraphics.setColor(HAZARD_RED);
            stripeGraphics.setStroke(new BasicStroke(thickness));
            for (int offset = -height; offset < width; offset += thickness * 2) {
                stripeGraphics.drawLine(x + offset, y + height, x + offset + height, y);
            }
        } finally {
            stripeGraphics.dispose();
        }
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        insets.set(thickness, thickness, thickness, thickness);
        return insets;
    }
}
