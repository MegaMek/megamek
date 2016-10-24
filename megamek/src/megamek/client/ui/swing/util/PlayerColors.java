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

package megamek.client.ui.swing.util;

import java.awt.Color;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.IPlayer;

public class PlayerColors {

    public static final String colorNames[] = IPlayer.colorNames;

    protected static final int colorRGBs[] = { 0x8686BF, 0xCC6666, 0x87BF86,
            0x8FCCCC, 0xF29DC8, 0xF2AA61, 0xBEBEBE, 0x98816B, 0x800080,
            0x40E0D0, 0x800000, 0x00FF7F, 0xFFD700, 0xA0522D, 0xEE82EE,
            0x000080, 0x6B8E23, 0xFF00FF, 0xB22222, 0xB8860B, 0xFF7F50,
            0x7FFF00, 0x9400D3, 0xF2F261 };

    protected static final int advancedColorRGBs[] = {
            0x000000, 0x000033, 0x000066, 0x000099, 0x0000CC, 0x0000FF,
            0x003300, 0x003333, 0x003366, 0x003399, 0x0033CC, 0x0033FF,
            0x006600, 0x006633, 0x006666, 0x006699, 0x0066CC, 0x0066FF,
            0x009900, 0x009933, 0x009966, 0x009999, 0x0099CC, 0x0099FF,
            0x00CC00, 0x00CC33, 0x00CC66, 0x00CC99, 0x00CCCC, 0x00CCFF,
            0x00FF00, 0x00FF33, 0x00FF66, 0x00FF99, 0x00FFCC, 0x00FFFF,
            0x330000, 0x330033, 0x330066, 0x330099, 0x3300CC, 0x3300FF,
            0x333300, 0x333333, 0x333366, 0x333399, 0x3333CC, 0x3333FF,
            0x336600, 0x336633, 0x336666, 0x336699, 0x3366CC, 0x3366FF,
            0x339900, 0x339933, 0x339966, 0x339999, 0x3399CC, 0x3399FF,
            0x33CC00, 0x33CC33, 0x33CC66, 0x33CC99, 0x33CCCC, 0x33CCFF,
            0x33FF00, 0x33FF33, 0x33FF66, 0x33FF99, 0x33FFCC, 0x33FFFF,
            0x660000, 0x660033, 0x660066, 0x660099, 0x6600CC, 0x6600FF,
            0x663300, 0x663333, 0x663366, 0x663399, 0x6633CC, 0x6633FF,
            0x666600, 0x666633, 0x666666, 0x666699, 0x6666CC, 0x6666FF,
            0x669900, 0x669933, 0x669966, 0x669999, 0x6699CC, 0x6699FF,
            0x66CC00, 0x66CC33, 0x66CC66, 0x66CC99, 0x66CCCC, 0x66CCFF,
            0x66FF00, 0x66FF33, 0x66FF66, 0x66FF99, 0x66FFCC, 0x66FFFF,
            0x990000, 0x990033, 0x990066, 0x990099, 0x9900CC, 0x9900FF,
            0x993300, 0x993333, 0x993366, 0x993399, 0x9933CC, 0x9933FF,
            0x996600, 0x996633, 0x996666, 0x996699, 0x9966CC, 0x9966FF,
            0x999900, 0x999933, 0x999966, 0x999999, 0x9999CC, 0x9999FF,
            0x99CC00, 0x99CC33, 0x99CC66, 0x99CC99, 0x99CCCC, 0x99CCFF,
            0x99FF00, 0x99FF33, 0x99FF66, 0x99FF99, 0x99FFCC, 0x99FFFF,
            0xCC0000, 0xCC0033, 0xCC0066, 0xCC0099, 0xCC00CC, 0xCC00FF,
            0xCC3300, 0xCC3333, 0xCC3366, 0xCC3399, 0xCC33CC, 0xCC33FF,
            0xCC6600, 0xCC6633, 0xCC6666, 0xCC6699, 0xCC66CC, 0xCC66FF,
            0xCC9900, 0xCC9933, 0xCC9966, 0xCC9999, 0xCC99CC, 0xCC99FF,
            0xCCCC00, 0xCCCC33, 0xCCCC66, 0xCCCC99, 0xCCCCCC, 0xCCCCFF,
            0xCCFF00, 0xCCFF33, 0xCCFF66, 0xCCFF99, 0xCCFFCC, 0xCCFFFF,
            0xFF0000, 0xFF0033, 0xFF0066, 0xFF0099, 0xFF00CC, 0xFF00FF,
            0xFF3300, 0xFF3333, 0xFF3366, 0xFF3399, 0xFF33CC, 0xFF33FF,
            0xFF6600, 0xFF6633, 0xFF6666, 0xFF6699, 0xFF66CC, 0xFF66FF,
            0xFF9900, 0xFF9933, 0xFF9966, 0xFF9999, 0xFF99CC, 0xFF99FF,
            0xFFCC00, 0xFFCC33, 0xFFCC66, 0xFFCC99, 0xFFCCCC, 0xFFCCFF,
            0xFFFF00, 0xFFFF33, 0xFFFF66, 0xFFFF99, 0xFFFFCC, 0xFFFFFF };

    public static Color getColor(String colorName) {
        for (int idx = 0; idx < colorNames.length; idx++) {
            if (colorNames[idx].equals(colorName)) {
                return getColor(idx);
            }
        }
        return getColor(0);
    }

    public static Color getColor(int colorIndex) {
        return getColor(colorIndex, true);
    }

    public static Color getColor(int colorIndex, boolean allowTransparency) {
        int colour = colorRGBs[colorIndex];
        if (allowTransparency) {
        int transparency = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_ATTACK_ARROW_TRANSPARENCY);
            return new Color(colour | (transparency << 24), true);
        } else {
            return new Color(colour);
        }
    }

    public static int getColorRGB(int colorIndex) {
        return colorRGBs[colorIndex];
    }

    public static Color getAdvColor(int colorIndex, int plrIndex) {
        if (colorIndex > advancedColorRGBs.length) {
            return getColor(plrIndex);
        }
        int colour = advancedColorRGBs[colorIndex];
        int transparency = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_ATTACK_ARROW_TRANSPARENCY);
        return new Color(colour | (transparency << 24), true);
    }

    public static int getAdvColorRGB(int colorIndex, int plrIndex) {
        if (colorIndex > advancedColorRGBs.length) {
            return getColorRGB(plrIndex);
        }
        return advancedColorRGBs[colorIndex];
    }

}
