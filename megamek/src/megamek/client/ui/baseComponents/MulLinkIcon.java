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
package megamek.client.ui.baseComponents;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import megamek.client.ui.util.StringDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * A link icon (two chain links) and the letters "MUL". Clicking it opens the supplied URL
 * in the system browser (if supported).
 */
public class MulLinkIcon extends FlatAbstractIcon {

    private final int size;          // total square size of the icon (pixels)

    /**
     * Creates a MUL link icon at the standard size (16) suitable to place it in line with, e.g. a JTextfield and a
     * standard FlatLaf color.
     */
    public MulLinkIcon() {
        this(16);
    }

    /**
     * Creates a MUL link icon with the given size and the standard FlatLaf color.
     *
     * @param size The pixel size of the icon. Note that this is automatically scaled depending on the GUI scaling
     */
    public MulLinkIcon(int size) {
        this(size, UIManager.getColor("Actions.Grey"));
    }

    /**
     * Creates a MUL link icon with the given size and given color.
     *
     * @param size  The pixel size of the icon. Note that this is automatically scaled depending on the GUI scaling
     * @param color The color to use for drawing the icon's shape
     */
    public MulLinkIcon(int size, Color color) {
        super(size, size, color);
        this.size = size;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        double length = size * 0.5;
        double width = length * 0.6;
        double thickness = width * 0.2;

        AffineTransform translateLink1 = AffineTransform.getTranslateInstance((size - length) * 0.5,
              size * 0.27 - width / 2);
        translateLink1.translate(size * 0.15, size * 0.03);
        AffineTransform translateLink2 = AffineTransform.getTranslateInstance((size - length) * 0.5,
              size * 0.27 - width / 2);
        translateLink2.translate(-size * 0.15, -size * 0.03);

        g.fill(translateLink1.createTransformedShape(createLink(length, width, thickness)));
        g.fill(translateLink2.createTransformedShape(createLink(length, width, thickness)));

        new StringDrawer("MUL").at(size / 2, size * 9 / 10)
              .font(new Font("SansSerif", Font.BOLD, size))
              .centerX()
              .maxWidth(size * 9 / 10)
              .draw(g);
    }


    private static Shape createLink(double length, double width, double thickness) {
        Area link = new Area(new RoundRectangle2D.Double(0, 0, length, width, width, width));
        link.subtract(new Area(new RoundRectangle2D.Double(thickness, thickness, length - 2 * thickness,
              width - 2 * thickness, width - thickness, width - thickness)));
        return link;
    }
}
