/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

/*
 * A class for showing centered labels with desired value.
 */

public class PMValueLabel extends PMSimpleLabel {

    /*
     * Create the label.
     */
    PMValueLabel(FontMetrics fm, Color c) {
        super("", fm, c);
    }

    /*
     * Set/change the value displayed in the label.
     */
    void setValue(String v) {
        string = v;
        width = fm.stringWidth(string);
    }

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
    }

    /*
     * Draw the label.
     */
    @Override
    public void drawInto(Graphics g) {
        if (!visible) {
            return;
        }
        Color temp = g.getColor();
        g.setColor(color);
        g.drawString(string, x - width / 2, y - fm.getMaxDescent());
        g.setColor(temp);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x - width / 2, y - height, width, height + descent);
    }
}
