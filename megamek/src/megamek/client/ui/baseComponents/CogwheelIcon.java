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

import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * This is a cogwheel icon that usually indicates some sort of config menu.
 */
public class CogwheelIcon extends FlatAbstractIcon {

    private final int size;
    private final int radius;
    private final int teeth;
    private final int toothDepth;
    private final int holeRadius;
    private Shape shape;

    /**
     * Creates a cogwheel icon at the standard size (16) suitable to place it in line with, e.g. a JTextfield and with a
     * standard FlatLaf color.
     */
    public CogwheelIcon() {
        this(16);
    }

    /**
     * Creates a cogwheel icon of the given size and with a standard FlatLaf color.
     */
    public CogwheelIcon(int size) {
        this(size, size * 6 / 16, 6, size * 2 / 16, size * 3 / 16);
    }

    /**
     * Creates a configurable cogwheel icon at given size and with the given values.
     */
    public CogwheelIcon(int size, int radius, int teeth, int toothDepth, int holeRadius) {
        super(size, size, UIManager.getColor("Actions.Grey"));
        this.size = size;
        this.radius = radius;
        this.teeth = teeth;
        this.toothDepth = toothDepth;
        this.holeRadius = holeRadius;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        if (shape == null) {
            shape = createCogwheel(size / 2, size / 2, radius, teeth, toothDepth, holeRadius);
        }
        g.fill(shape);
    }

    private Shape createCog(int cx, int cy, int radius, int teeth, int toothDepth) {
        double angleStep = 2 * Math.PI / teeth;
        double toothWidthAngle = angleStep * 0.6; // fraction of arc used by tooth
        double dx = Math.sin(toothWidthAngle / 2) * radius;
        double Y1 = cy - radius * Math.cos(toothWidthAngle / 2);
        Area cog = new Area(new RoundRectangle2D.Double(cx - dx, Y1 - toothDepth, 2 * dx,
              toothDepth * 0.5, toothDepth, toothDepth));
        cog.add(new Area(new Rectangle2D.Double(cx - dx, Y1 - toothDepth * 0.75, 2 * dx, toothDepth)));
        return cog;
    }

    private Shape createCogwheel(int cx, int cy, int radius, int teeth,
          int toothDepth, int holeRadius) {
        double angleStep = 2 * Math.PI / teeth;
        Area cogArea = new Area();
        Shape wheel = new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius);
        Shape hole = new Ellipse2D.Double(cx - holeRadius, cy - holeRadius, 2 * holeRadius, 2 * holeRadius);
        cogArea.add(new Area(wheel));
        cogArea.subtract(new Area(hole));

        Shape baseCog = createCog(cx, cy, radius, teeth, toothDepth);
        var rotateTransform = new AffineTransform();
        for (int i = 0; i < teeth; i++) {
            Shape rotatedCog = rotateTransform.createTransformedShape(baseCog);
            cogArea.add(new Area(rotatedCog));
            rotateTransform.rotate(angleStep, cx, cy);
        }

        return cogArea;
    }
}

