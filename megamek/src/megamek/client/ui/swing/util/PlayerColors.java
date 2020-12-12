/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.util;

import java.awt.Color;

import megamek.client.ui.swing.GUIPreferences;

public class PlayerColors {
    public static final String[] COLOR_NAMES = { "Blue", "Red", "Green", "Cyan",
            "Pink", "Orange", "Gray", "Brown", "Purple", "Turquoise ",
            "Maroon", "Spring Green", "Gold", "Sienna", "Violet", "Navy",
            "Olive Drab", "Fuchsia", "FireBrick", "Dark Golden Rod", "Coral",
            "Chartreuse", "Deep Purple", "Yellow" };

    protected static final int[] COLOR_RGBS = { 0x8686BF, 0xCC6666, 0x87BF86,
            0x8FCCCC, 0xF29DC8, 0xF2AA61, 0xBEBEBE, 0x98816B, 0x800080,
            0x40E0D0, 0x800000, 0x00FF7F, 0xFFD700, 0xA0522D, 0xEE82EE,
            0x000080, 0x6B8E23, 0xFF00FF, 0xB22222, 0xB8860B, 0xFF7F50,
            0x7FFF00, 0x9400D3, 0xF2F261 };

    public static Color getColor(String colorName) {
        for (int idx = 0; idx < COLOR_NAMES.length; idx++) {
            if (COLOR_NAMES[idx].equals(colorName)) {
                return getColor(idx);
            }
        }
        return getColor(0);
    }

    public static Color getColor(int colorIndex) {
        return getColor(colorIndex, true);
    }

    public static Color getColor(int colorIndex, boolean allowTransparency) {
        int colour = COLOR_RGBS[colorIndex];
        if (allowTransparency) {
        int transparency = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_ATTACK_ARROW_TRANSPARENCY);
            return new Color(colour | (transparency << 24), true);
        } else {
            return new Color(colour);
        }
    }

    public static int getColorRGB(int colorIndex) {
        return COLOR_RGBS[colorIndex];
    }
}
