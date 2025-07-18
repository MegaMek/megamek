/*
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
package megamek.client.ui.util;

import java.util.regex.Pattern;

public enum DiscordFormat {
    // Text colors
    GRAY(30), RED(31), GREEN(32), YELLOW(33), BLUE(34), PINK(35), CYAN(36), WHITE(37),

    // Background colors
    BG_BLUEISH_BLACK(40), BG_RUST(41), BG_DARKER_GREY(42), BG_DARK_GREY(43), BG_GREY(44), BG_BLURPLE(45), BG_LIGHT_GREY(46), BG_OFF_WHITE(47),

    // Font effects
    BOLD(1), UNDERLINE(4),

    RESET(0);

    private final int code;

    DiscordFormat(int code) {
        this.code = code;
    }

    public String toString() {
        if (this == RESET) {
            return "\u001b[" + code + 'm' + WHITE;
        }
        return "\u001b[" + code + 'm';
    }

    public static final DiscordFormat NUMBER_COLOR = YELLOW;
    public static final DiscordFormat ROW_SHADING = BG_BLUEISH_BLACK;
    private static final Pattern numberPattern = Pattern.compile("\\b\\d+\\b");

    public static String highlightNumbersForDiscord(String original) {
        return numberPattern.matcher(original).replaceAll(DiscordFormat.NUMBER_COLOR + "$0" + DiscordFormat.WHITE);
    }
}
