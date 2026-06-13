/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Color;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.Messages;
import megamek.logging.MMLogger;

public enum PlayerColour {
    // region Enum Declarations
    BLUE(PlayerColour.PLAYER_COLOUR_BLUE),
    RED(PlayerColour.PLAYER_COLOUR_RED),
    GREEN(PlayerColour.PLAYER_COLOUR_GREEN),
    CYAN(PlayerColour.PLAYER_COLOUR_CYAN),
    PINK(PlayerColour.PLAYER_COLOUR_PINK),
    ORANGE(PlayerColour.PLAYER_COLOUR_ORANGE),
    GRAY(PlayerColour.PLAYER_COLOUR_GRAY),
    BROWN(PlayerColour.PLAYER_COLOUR_BROWN),
    PURPLE(PlayerColour.PLAYER_COLOUR_PURPLE),
    TURQUOISE(PlayerColour.PLAYER_COLOUR_TURQUOISE),
    MAROON(PlayerColour.PLAYER_COLOUR_MAROON),
    SPRING_GREEN(PlayerColour.PLAYER_COLOUR_SPRING_GREEN),
    GOLD(PlayerColour.PLAYER_COLOUR_GOLD),
    SIENNA(PlayerColour.PLAYER_COLOUR_SIENNA),
    VIOLET(PlayerColour.PLAYER_COLOUR_VIOLET),
    NAVY(PlayerColour.PLAYER_COLOUR_NAVY),
    OLIVE_DRAB(PlayerColour.PLAYER_COLOUR_OLIVE_DRAB),
    FUCHSIA(PlayerColour.PLAYER_COLOUR_FUCHSIA),
    FIRE_BRICK(PlayerColour.PLAYER_COLOUR_FIRE_BRICK),
    DARK_GOLDEN_ROD(PlayerColour.PLAYER_COLOUR_DARK_GOLDEN_ROD),
    CORAL(PlayerColour.PLAYER_COLOUR_CORAL),
    CHARTREUSE(PlayerColour.PLAYER_COLOUR_CHARTREUSE),
    DEEP_PURPLE(PlayerColour.PLAYER_COLOUR_DEEP_PURPLE),
    YELLOW(PlayerColour.PLAYER_COLOUR_YELLOW);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String text;
    private final String name;
    public static final String PLAYER_COLOUR_BLUE = "PlayerColour.BLUE.text";
    public static final String PLAYER_COLOUR_RED = "PlayerColour.RED.text";
    public static final String PLAYER_COLOUR_GREEN = "PlayerColour.GREEN.text";
    public static final String PLAYER_COLOUR_CYAN = "PlayerColour.CYAN.text";
    public static final String PLAYER_COLOUR_PINK = "PlayerColour.PINK.text";
    public static final String PLAYER_COLOUR_ORANGE = "PlayerColour.ORANGE.text";
    public static final String PLAYER_COLOUR_GRAY = "PlayerColour.GRAY.text";
    public static final String PLAYER_COLOUR_BROWN = "layerColour.BROWN.text";
    public static final String PLAYER_COLOUR_PURPLE = "PlayerColour.PURPLE.text";
    public static final String PLAYER_COLOUR_TURQUOISE = "PlayerColour.TURQUOISE.text";
    public static final String PLAYER_COLOUR_MAROON = "PlayerColour.MAROON.text";
    public static final String PLAYER_COLOUR_SPRING_GREEN = "PlayerColour.SPRING_GREEN.text";
    public static final String PLAYER_COLOUR_GOLD = "PlayerColour.GOLD.text";
    public static final String PLAYER_COLOUR_SIENNA = "PlayerColour.SIENNA.text";
    public static final String PLAYER_COLOUR_VIOLET = "PlayerColour.VIOLET.text";
    public static final String PLAYER_COLOUR_NAVY = "PlayerColour.NAVY.text";
    public static final String PLAYER_COLOUR_OLIVE_DRAB = "PlayerColour.OLIVE_DRAB.text";
    public static final String PLAYER_COLOUR_FUCHSIA = "PlayerColour.FUCHSIA.text";
    public static final String PLAYER_COLOUR_FIRE_BRICK = "PlayerColour.FIRE_BRICK.text";
    public static final String PLAYER_COLOUR_DARK_GOLDEN_ROD = "PlayerColour.DARK_GOLDEN_ROD.text";
    public static final String PLAYER_COLOUR_CORAL = "PlayerColour.CORAL.text";
    public static final String PLAYER_COLOUR_CHARTREUSE = "PlayerColour.CHARTREUSE.text";
    public static final String PLAYER_COLOUR_DEEP_PURPLE = "PlayerColour.DEEP_PURPLE.text";
    public static final String PLAYER_COLOUR_YELLOW = "PlayerColour.YELLOW.text";
    // endregion Variable Declarations

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    // region Constructors
    PlayerColour(String text) {
        this.text = text;
        this.name = Messages.getString(text);
    }
    // endregion Constructors

    // region Getters
    public String getText() {
        return this.text;
    }

    public Color getColour() {
        return getColour(true);
    }

    public Color getColour(boolean allowTransparency) {
        if (allowTransparency) {
            int transparency = GUIP.getAttackArrowTransparency();
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
    // endregion Getters

    // region File I/O
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

        MMLogger.create(PlayerColour.class)
              .error("Unable to parse PlayerColour from text {}, returning BLUE", text);

        return BLUE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
