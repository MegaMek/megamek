/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * This component draws a dashed horizontal line, similarly to a JSeparator. Its color and line strength (stroke width)
 * can be set. Also, its width (the amount of available horizontal space it covers) can be set.
 */
public class DashedSeparator extends JComponent {

    private final Color color;
    private final float relativeWidth;
    private final float strokeWidth;

    /**
     * Creates a dashed separator that uses the UI LaF's separator color and a line width of 1. It uses the available
     * horizontal space.
     */
    public DashedSeparator() {
        this(UIManager.getColor("Separator.foreground"));
    }

    /**
     * Creates a dashed separator of the given color and a line width of 1. It uses the available horizontal space.
     */
    public DashedSeparator(Color color) {
        this(color, 1, 1);
    }

    /**
     * Creates a dashed separator that uses the UI LaF's separator color and the given values for the stroke width (line
     * thickness) and the relative horizontal length. The values for relativeWidth should be between 0 and 1; 1 meaning
     * the line extends for all the horizontal width; 0.5 meaning only half the horizontal space is used (the line is
     * centered).
     */
    public DashedSeparator(float relativeWidth, float strokeWidth) {
        this(UIManager.getColor("Separator.foreground"), relativeWidth, strokeWidth);
    }

    /**
     * Creates a dashed separator of the given color and the given values for the stroke width (line thickness) and the
     * relative horizontal length. The values for relativeWidth should be between 0 and 1; 1 meaning the line extends
     * for all the horizontal width; 0.5 meaning only half the horizontal space is used (the line is centered).
     */
    public DashedSeparator(Color color, float relativeWidth, float strokeWidth) {
        this.color = color;
        this.relativeWidth = relativeWidth;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(super.getMaximumSize().width, (int) (strokeWidth + 2));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, (int) (strokeWidth + 2));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(super.getMinimumSize().width, (int) (strokeWidth + 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Stroke dashed = new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                  0, new float[] { 9 }, 0);
            g2.setStroke(dashed);
            g2.setColor(color);
            g2.drawLine((int) (getWidth() * (1 - relativeWidth) / 2), 0,
                  (int) (getWidth() * (1 + relativeWidth) / 2), 0);
        } finally {
            g2.dispose();
        }
    }
}
