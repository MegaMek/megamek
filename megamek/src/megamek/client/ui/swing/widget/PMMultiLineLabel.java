/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a label that can stretch across multiple lines
 * 
 * @author NickAragua
 *
 */
public class PMMultiLineLabel extends PMSimpleLabel {
    private List<String> labels = new ArrayList<>();

    /**
     * Constructs a new multi-line label
     * 
     * @param fm Font metrics object
     * @param c  Color for the text on this label
     */
    public PMMultiLineLabel(FontMetrics fm, Color c) {
        super("", fm, c);
    }

    /**
     * Clear the contents of this multi-line label
     */
    public void clear() {
        labels.clear();
        height = 0;
        width = 0;
    }

    /**
     * Add a string to this multi-line label
     * 
     * @param s The string to add
     */
    public void addString(String s) {
        labels.add(s);

        int newWidth = fm.stringWidth(s);
        if (newWidth > width) {
            width = newWidth;
        }

        height += fm.getHeight();
    }

    /*
     * Draw the label.
     */
    @Override
    public void drawInto(Graphics g) {
        if (!visible) {
            return;
        }
        Font font = g.getFont();
        Color temp = g.getColor();
        g.setColor(color);
        g.setFont(fm.getFont());

        int currentY = y;

        for (String s : labels) {
            g.drawString(s, x, currentY);
            currentY += fm.getHeight();
        }

        g.setColor(temp);
        g.setFont(font);
    }

}
