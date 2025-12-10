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

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * An icon showing stylized books and a mouse arrow. Used for sourcebook selection.
 */
public class BooksIcon extends FlatAbstractIcon {
    private final int size;

    /**
     * Creates a books icon at the standard size (16) suitable to place it in line with, e.g. a JTextfield and with a
     * standard FlatLaf color.
     */
    public BooksIcon() {
        this(16);
    }

    /**
     * Creates a books icon of the given size and with a standard FlatLaf color.
     */
    public BooksIcon(int size) {
        super(size, size, UIManager.getColor("Actions.Grey"));
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics2D g) {
        double baseY = size;
        double x = size * 0.05;
        double baseHeight = size;
        double height1 = baseHeight * 0.7;
        double height2 = baseHeight * 0.9;
        double height3 = baseHeight * 0.83;
        double baseThickness = size * 0.14;
        double thickness1 = baseThickness * 1.1;
        double thickness2 = baseThickness * 1.4;
        double thickness3 = baseThickness * 0.9;
        double distance = size * 0.1;
        Area booksArea = new Area();
        booksArea.add(new Area(new RoundRectangle2D.Double(x, baseY - height1, thickness1, height1, 1, 1)));
        x += thickness1 + distance;
        booksArea.add(new Area(new RoundRectangle2D.Double(x, baseY - height2, thickness2, height2, 1, 1)));
        x += thickness2 + distance;
        booksArea.add(new Area(new RoundRectangle2D.Double(x, baseY - height3, thickness3, height3, 1, 1)));

        // Arrow
        Path2D.Double path = new Path2D.Double();
        double h = size * 0.5;
        double q = size * 0.2;
        double tipX = size * 0.75;
        double tipY = size * 0.75;
        double a = (h * h - q * q) / (2 * h);
        path.moveTo(0, a - h);
        path.lineTo(-q, a);
        path.lineTo(q, a);
        path.closePath();

        double arrowScale = 1;
        double outlineScale = arrowScale * 1.8;
        double angle = -38;

        Area arrowOutline = new Area(path);
        arrowOutline.transform(AffineTransform.getScaleInstance(outlineScale, outlineScale));
        arrowOutline.transform(AffineTransform.getRotateInstance(Math.toRadians(angle)));
        arrowOutline.transform(AffineTransform.getTranslateInstance(tipX, tipY));

        Area arrowArea = new Area(path);
        arrowArea.transform(AffineTransform.getScaleInstance(arrowScale, arrowScale));
        arrowArea.transform(AffineTransform.getRotateInstance(Math.toRadians(angle)));
        arrowArea.transform(AffineTransform.getTranslateInstance(tipX, tipY));

        booksArea.subtract(arrowOutline);
        booksArea.add(arrowArea);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setClip(0, 0, size, size);
        g2d.fill(booksArea);
        g2d.dispose();
    }
}
