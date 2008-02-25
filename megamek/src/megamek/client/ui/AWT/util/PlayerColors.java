/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.AWT.util;

import java.awt.Color;

import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.Player;

public class PlayerColors {

    public static final String colorNames[] = Player.colorNames;

    protected static final int colorRGBs[] = { 0x8686BF, 0xF2F261, 0xCC6666,
            0x87BF86, 0xFFFFFF, 0x8FCCCC, 0xF29DC8, 0xF2AA61, 0xBEBEBE,
            0x98816B, 0x800080 };

    public static Color getColor(int colorIndex) {
        int colour = colorRGBs[colorIndex];
        int transparency = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_ATTACK_ARROW_TRANSPARENCY);
        return new Color(colour | (transparency << 24), true);
    }

    public static int getColorRGB(int colorIndex) {
        return colorRGBs[colorIndex];
    }

}
