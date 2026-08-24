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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class HazardStripeBorderTest {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    @Test
    void insetsMatchTheScaledStripeWidth() {
        int expected = UIUtil.scaleForGUI(5);
        HazardStripeBorder border = new HazardStripeBorder();

        Insets insets = border.getBorderInsets(new JPanel());
        assertEquals(new Insets(expected, expected, expected, expected), insets);

        Insets reused = border.getBorderInsets(new JPanel(), new Insets(99, 99, 99, 99));
        assertEquals(new Insets(expected, expected, expected, expected), reused);
    }

    @Test
    void aZeroThicknessIsClampedToOnePixelSoPaintingTerminates() {
        HazardStripeBorder border = new HazardStripeBorder(0);
        assertEquals(new Insets(1, 1, 1, 1), border.getBorderInsets(new JPanel()));

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        border.paintBorder(new JPanel(), graphics, 0, 0, WIDTH, HEIGHT);
        graphics.dispose();
    }

    @Test
    void paintsTheFrameAndLeavesTheInsideAlone() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        Color untouched = new Color(1, 2, 3);
        graphics.setColor(untouched);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        HazardStripeBorder border = new HazardStripeBorder();
        border.paintBorder(new JPanel(), graphics, 0, 0, WIDTH, HEIGHT);
        graphics.dispose();

        // The frame itself is painted in the hazard colours...
        Color corner = new Color(image.getRGB(1, 1), true);
        assertNotEquals(untouched, corner);
        assertTrue(corner.equals(HazardStripeBorder.HAZARD_YELLOW) || corner.equals(HazardStripeBorder.HAZARD_RED)
              || isBlendOfHazardColors(corner), "corner pixel should be a hazard colour, was " + corner);

        // ...and the area inside the stripes is left for the component to paint.
        assertEquals(untouched, new Color(image.getRGB(WIDTH / 2, HEIGHT / 2), true));
    }

    /** Antialiasing can land a pixel between the two stripe colours; red and green channels stay in their range. */
    private static boolean isBlendOfHazardColors(Color color) {
        return (color.getRed() >= HazardStripeBorder.HAZARD_RED.getRed())
              && (color.getGreen() >= HazardStripeBorder.HAZARD_RED.getGreen())
              && (color.getGreen() <= HazardStripeBorder.HAZARD_YELLOW.getGreen())
              && (color.getBlue() <= HazardStripeBorder.HAZARD_RED.getBlue());
    }
}
