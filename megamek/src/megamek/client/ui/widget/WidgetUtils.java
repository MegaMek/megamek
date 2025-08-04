/*
 * Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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
import java.awt.Dimension;
import java.awt.FontMetrics;

import megamek.common.annotations.Nullable;

public class WidgetUtils {
    public static void setAreaColor(PMSimplePolygonArea ha, PMValueLabel l,
          double percentRemaining) {
        if (percentRemaining <= 0) {
            ha.backColor = Color.darkGray.brighter();
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;
        } else if (percentRemaining <= .25) {
            ha.backColor = Color.red.brighter();
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;
        } else if (percentRemaining <= .75) {
            ha.backColor = Color.yellow;
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;
        } else {
            ha.backColor = Color.gray.brighter();
            l.setColor(Color.red);
            ha.highlightBorderColor = Color.red;
        }
    }

    public static PMSimpleLabel createLabel(String s, FontMetrics fm,
          Color color, int x, int y) {
        PMSimpleLabel l = new PMSimpleLabel(s, fm, color);
        centerLabelAt(l, x, y);
        return l;
    }

    public static PMValueLabel createValueLabel(int x, int y, String v, FontMetrics fm) {
        PMValueLabel l = new PMValueLabel(fm, Color.red);
        centerLabelAt(l, x, y);
        l.setValue(v);
        return l;
    }

    public static void centerLabelAt(@Nullable PMSimpleLabel l, int x, int y) {
        if (l == null) {
            return;
        }
        Dimension d = l.getSize();
        l.moveTo(x - d.width / 2, y + d.height / 2);
    }
}
