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

import megamek.MMConstants;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Messages;
import org.apache.logging.log4j.LogManager;

import java.awt.*;

public enum PlayerColour {
    //region Enum Declarations
    BLUE(PlayerColour.PLAYERCOLOUR_BLUE),
    RED(PlayerColour.PLAYERCOLOUR_RED),
    GREEN(PlayerColour.PLAYERCOLOUR_GREEN),
    CYAN(PlayerColour.PLAYERCOLOUR_CYAN),
    PINK(PlayerColour.PLAYERCOLOUR_PINK),
    ORANGE(PlayerColour.PLAYERCOLOUR_ORANGE),
    GRAY(PlayerColour.PLAYERCOLOUR_GRAY),
    BROWN(PlayerColour.PLAYERCOLOUR_BROWN),
    PURPLE(PlayerColour.PLAYERCOLOUR_PURPLE),
    TURQUOISE(PlayerColour.PLAYERCOLOUR_TURQUOISE),
    MAROON(PlayerColour.PLAYERCOLOUR_MAROON),
    SPRING_GREEN(PlayerColour.PLAYERCOLOUR_SPRING_GREEN),
    GOLD(PlayerColour.PLAYERCOLOUR_GOLD),
    SIENNA(PlayerColour.PLAYERCOLOUR_SIENNA),
    VIOLET(PlayerColour.PLAYERCOLOUR_VIOLET),
    NAVY(PlayerColour.PLAYERCOLOUR_NAVY),
    OLIVE_DRAB(PlayerColour.PLAYERCOLOUR_OLIVE_DRAB),
    FUCHSIA(PlayerColour.PLAYERCOLOUR_FUCHSIA),
    FIRE_BRICK(PlayerColour.PLAYERCOLOUR_FIRE_BRICK),
    DARK_GOLDEN_ROD(PlayerColour.PLAYERCOLOUR_DARK_GOLDEN_ROD),
    CORAL(PlayerColour.PLAYERCOLOUR_CORAL),
    CHARTREUSE(PlayerColour.PLAYERCOLOUR_CHARTREUSE),
    DEEP_PURPLE(PlayerColour.PLAYERCOLOUR_DEEP_PURPLE),
    YELLOW(PlayerColour.PLAYERCOLOUR_YELLOW);
    //endregion Enum Declarations


    //region Variable Declarations
    private final String text;
    private final String name;
    public static final String PLAYERCOLOUR_BLUE = "PlayerColour.BLUE.text";
    public static final String PLAYERCOLOUR_RED = "PlayerColour.RED.text";
    public static final String PLAYERCOLOUR_GREEN = "PlayerColour.GREEN.text";
    public static final String PLAYERCOLOUR_CYAN = "PlayerColour.CYAN.text";
    public static final String PLAYERCOLOUR_PINK = "PlayerColour.PINK.text";
    public static final String PLAYERCOLOUR_ORANGE = "PlayerColour.ORANGE.text";
    public static final String PLAYERCOLOUR_GRAY = "PlayerColour.GRAY.text";
    public static final String PLAYERCOLOUR_BROWN = "layerColour.BROWN.text";
    public static final String PLAYERCOLOUR_PURPLE = "PlayerColour.PURPLE.text";
    public static final String PLAYERCOLOUR_TURQUOISE = "PlayerColour.TURQUOISE.text";
    public static final String PLAYERCOLOUR_MAROON = "PlayerColour.MAROON.text";
    public static final String PLAYERCOLOUR_SPRING_GREEN = "PlayerColour.SPRING_GREEN.text";
    public static final String PLAYERCOLOUR_GOLD = "PlayerColour.GOLD.text";
    public static final String PLAYERCOLOUR_SIENNA = "PlayerColour.SIENNA.text";
    public static final String PLAYERCOLOUR_VIOLET = "PlayerColour.VIOLET.text";
    public static final String PLAYERCOLOUR_NAVY = "PlayerColour.NAVY.text";
    public static final String PLAYERCOLOUR_OLIVE_DRAB = "PlayerColour.OLIVE_DRAB.text";
    public static final String PLAYERCOLOUR_FUCHSIA = "PlayerColour.FUCHSIA.text";
    public static final String PLAYERCOLOUR_FIRE_BRICK = "PlayerColour.FIRE_BRICK.text";
    public static final String PLAYERCOLOUR_DARK_GOLDEN_ROD = "PlayerColour.DARK_GOLDEN_ROD.text";
    public static final String PLAYERCOLOUR_CORAL = "PlayerColour.CORAL.text";
    public static final String PLAYERCOLOUR_CHARTREUSE = "PlayerColour.CHARTREUSE.text";
    public static final String PLAYERCOLOUR_DEEP_PURPLE = "PlayerColour.DEEP_PURPLE.text";
    public static final String PLAYERCOLOUR_YELLOW = "PlayerColour.YELLOW.text";
    //endregion Variable Declarations

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    //region Constructors
    PlayerColour(String text) {
        this.text = text;
        this.name = Messages.getString(text);
    }
    //endregion Constructors

    //region Getters
    public String getText() {
        return this.text;
    }

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

    public int getColorToHex(Color c) {
        return c.getRGB() & 0xFFFFFF;
    }

    public int getHex() {
        return getColorToHex(GUIP.getColor(this.text));
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
