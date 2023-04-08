/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.util;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Messages;
import org.apache.logging.log4j.LogManager;

import java.awt.*;

public enum PlayerColour {
    //region Enum Declarations
    BLUE("PlayerColour.BLUE.text", 0x8686BF),
    RED("PlayerColour.RED.text", 0xCC6666),
    GREEN("PlayerColour.GREEN.text", 0x87BF86),
    CYAN("PlayerColour.CYAN.text", 0x8FCCCC),
    PINK("PlayerColour.PINK.text", 0xF29DC8),
    ORANGE("PlayerColour.ORANGE.text", 0xF2AA61),
    GRAY("PlayerColour.GRAY.text", 0xBEBEBE),
    BROWN("PlayerColour.BROWN.text", 0x98816B),
    PURPLE("PlayerColour.PURPLE.text", 0x800080),
    TURQUOISE("PlayerColour.TURQUOISE.text", 0x40E0D0),
    MAROON("PlayerColour.MAROON.text", 0x800000),
    SPRING_GREEN("PlayerColour.SPRING_GREEN.text", 0x00FF7F),
    GOLD("PlayerColour.GOLD.text", 0xFFD700),
    SIENNA("PlayerColour.SIENNA.text", 0xA0522D),
    VIOLET("PlayerColour.VIOLET.text", 0xEE82EE),
    NAVY("PlayerColour.NAVY.text", 0x000080),
    OLIVE_DRAB("PlayerColour.OLIVE_DRAB.text", 0x6B8E23),
    FUCHSIA("PlayerColour.FUCHSIA.text", 0xFF00FF),
    FIRE_BRICK("PlayerColour.FIRE_BRICK.text", 0xB22222),
    DARK_GOLDEN_ROD("PlayerColour.DARK_GOLDEN_ROD.text", 0xB8860B),
    CORAL("PlayerColour.CORAL.text", 0xFF7F50),
    CHARTREUSE("PlayerColour.CHARTREUSE.text", 0x7FFF00),
    DEEP_PURPLE("PlayerColour.DEEP_PURPLE.text", 0x9400D3),
    YELLOW("PlayerColour.YELLOW.text", 0xF2F261);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final int hex;
    //endregion Variable Declarations

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    //region Constructors
    PlayerColour(String name, int hex) {
        this.name = Messages.getString(name);
        this.hex = hex;
    }
    //endregion Constructors

    //region Getters
    public Color getColour() {
        return getColour(true);
    }

    public Color getColour(boolean allowTransparency) {
        if (allowTransparency) {
            int transparency = GUIP.getAttachArrowTransparency();
            return new Color(getHex() | (transparency << 24), true);
        } else {
            return new Color(getHex());
        }
    }

    public int getHex() {
        return hex;
    }

    public String getHexString() {
        return Integer.toHexString(getHex());
    }

    public String getHexString(int hex) {
        return Integer.toHexString(getHex() & hex);
    }
    //endregion Getters

    //region File I/O
    public static PlayerColour parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Text Name Parser
        PlayerColour[] playerColours = values();
        try {
            for (PlayerColour playerColour : playerColours) {
                if (playerColour.toString().equals(text)) {
                    return playerColour;
                }
            }
        } catch (Exception ignored) {

        }

        // Index Parser
        try {
            // This is meant to crash if for any reason the index is higher than the set
            return playerColours[Integer.parseInt(text)];
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse PlayerColour from text " + text + ", returning BLUE");

        return BLUE;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
