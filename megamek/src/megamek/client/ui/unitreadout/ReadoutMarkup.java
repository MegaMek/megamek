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
package megamek.client.ui.unitreadout;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.DiscordFormat;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;

import java.util.regex.Pattern;

class ReadoutMarkup {

    private static final String DAMAGED_DELIMITER = "§§";
    private static final String DESTROYED_DELIMITER = "&&";
    private static final String CAPTURE_GROUP = "((?:(?!%s|%s).)*)".formatted(DAMAGED_DELIMITER, DESTROYED_DELIMITER);
    private static final Pattern DAMAGED_PATTERN =
          Pattern.compile(DAMAGED_DELIMITER + CAPTURE_GROUP + DAMAGED_DELIMITER);
    private static final Pattern DESTROYED_PATTERN =
          Pattern.compile(DESTROYED_DELIMITER + CAPTURE_GROUP + DESTROYED_DELIMITER);
    private static final String CAPTURE_REPLACEMENT = "$1";

    private static String cautionReplacement;
    private static String warningReplacement;

    static String applyFormatting(String text, ViewFormatting formatting) {
        String damageReplaced = DAMAGED_PATTERN.matcher(text).replaceAll(asCaution(formatting));
        return DESTROYED_PATTERN.matcher(damageReplaced).replaceAll(asWarning(formatting));
    }

    /**
     * Adds markup for the "damaged" status (e.g., reduced ammo or armor) to the given text. This is removed and
     * replaced with suitable formatting when the readout is exported for a specific format. The color used for HTML
     * output is the caution color (usually yellow) from the GUI Preferences.
     *
     * @param text The text to mark up, e.g. "5"
     *
     * @return The marked-up text, e.g. "§§5§§"
     */
    static String markupDamaged(String text) {
        return DAMAGED_DELIMITER + text + DAMAGED_DELIMITER;
    }

    /**
     * Adds markup for the "destroyed" status to the given text. This is removed and replaced with suitable formatting
     * when the readout is exported for a specific format. The color used for HTML output is the warning color (usually
     * red) from the GUI Preferences.
     *
     * @param text The text to mark up, e.g. "5"
     *
     * @return The marked-up text, e.g. "&&5&&"
     */
    static String markupDestroyed(String text) {
        return DESTROYED_DELIMITER + text + DESTROYED_DELIMITER;
    }

    /**
     * @param formatting The formatting to apply
     *
     * @return The String CAPTURE_REPLACEMENT, formatted appropriately for the given formatting.
     */
    private static String asCaution(ViewFormatting formatting) {
        return switch (formatting) {
            case HTML -> cautionHTML();
            case NONE -> CAPTURE_REPLACEMENT;
            case DISCORD -> cautionDiscord();
        };
    }

    /**
     * @param formatting The formatting to apply
     *
     * @return The String CAPTURE_REPLACEMENT, formatted appropriately for the given formatting.
     */
    private static String asWarning(ViewFormatting formatting) {
        return switch (formatting) {
            case HTML -> warningHTML();
            case NONE -> CAPTURE_REPLACEMENT;
            case DISCORD -> warningDiscord();
        };
    }

    private static String cautionHTML() {
        if (cautionReplacement == null) {
            String formatString = "<FONT %s>%s</FONT>";
            String colorString = UIUtil.colorString(GUIPreferences.getInstance().getCautionColor());
            cautionReplacement = formatString.formatted(colorString, CAPTURE_REPLACEMENT);
        }
        return cautionReplacement;
    }

    private static String warningHTML() {
        if (warningReplacement == null) {
            String formatString = "<FONT %s>%s</FONT>";
            String colorString = UIUtil.colorString(GUIPreferences.getInstance().getWarningColor());
            warningReplacement = formatString.formatted(colorString, CAPTURE_REPLACEMENT);
        }
        return warningReplacement;
    }

    private static String cautionDiscord() {
        return DiscordFormat.YELLOW + "$1";
    }

    private static String warningDiscord() {
        return DiscordFormat.RED + "$1";
    }

    private ReadoutMarkup() {}
}
