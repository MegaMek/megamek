/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.client.ui.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import javax.swing.border.AbstractBorder;

public class LocationBorder extends AbstractBorder {

    private final static float CL = 40;
    private final static float HCL = 10;
    private final static float HCH = 5;

    /**
     * Thickness of the border.
     */
    protected float thickness;

    /**
     * Color of the border.
     */
    protected Color lineColor;

    public LocationBorder(Color color, float thickness) {
        this.thickness = thickness;
        lineColor = color;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if ((thickness > 0) && (lineColor != null) && (width > 0) && (height > 0) && (g instanceof Graphics2D)) {
            Graphics2D g2d = (Graphics2D) g;

            Color oldColor = g2d.getColor();
            Stroke oldStroke = g2d.getStroke();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(this.lineColor);
            g2d.setStroke(new BasicStroke(thickness));

            Path2D.Float line = new Path2D.Float();
            float xc = x + thickness;
            float xw = x + width - thickness;
            float yc = y + thickness;
            float yh = y + height - thickness;

            if (width < 2 * CL) {
                float hcl = 0.5f * HCL / CL * width;
                line.moveTo(xc, yc + HCH);
                line.curveTo(xc + hcl, yc, xc + hcl, yc, width / 2.0d, yc);
                line.curveTo(xw - hcl, yc, xw - hcl, yc, xw, yc + HCH);
                line.lineTo(xw, yh - HCH);
                line.curveTo(xw - hcl, yh, xw - hcl, yh, width / 2.0d, yh);
                line.curveTo(xc + hcl, yh, xc + hcl, yh, xc, yh - HCH);
            } else {
                line.moveTo(xc, yc + HCH);
                line.curveTo(xc + HCL, yc, xc + HCL, yc, xc + CL, yc);
                line.lineTo(xw - CL, yc);
                line.curveTo(xw - HCL, yc, xw - HCL, yc, xw, yc + HCH);
                line.lineTo(xw, yh - HCH);
                line.curveTo(xw - HCL, yh, xw - HCL, yh, xw - CL, yh);
                line.lineTo(xc + CL, yh);
                line.curveTo(xc + HCL, yh, xc + HCL, yh, xc, yh - HCH);
            }
            line.closePath();
            g2d.draw(line);

            g2d.setStroke(oldStroke);
            g2d.setColor(oldColor);
        }
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set((int) (HCH + 1.5 * thickness), (int) (3 + 1.5 * thickness),
              (int) (HCH + 1.5 * thickness), (int) (3 + 1.5 * thickness));
        return insets;
    }

}
