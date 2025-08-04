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

package megamek.client.ui.widget;

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
