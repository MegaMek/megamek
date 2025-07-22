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

import megamek.client.ui.util.DiscordFormat;
import megamek.client.ui.util.ViewFormatting;

/**
 * A common interface for the various elements of the unit readout. ways to present data that can be formatted either
 * as HTML or as plain text.
 */
interface ViewElement {

    /**
     * @return The contents of this view element, rendered to plain text. The text contains no tags or other control
     * characters other than '\n' line breaks.
     */
    String toPlainText();

    /**
     * @return The contents of this view element, rendered as HTML. The text does not include the HTML or BODY
     * beginning or end tags but may contain other control tags such as TABLE or FONT. Line breaks use the BR tag
     * (angle brackets omitted as they are tedious in Javadoc).
     */
    String toHTML();

    /**
     * @return The contents of this view element, rendered as a Discord-friendly string.
     */
    String toDiscord();

    /**
     * Marks warning text; in html the text is displayed in red. In plain text it is
     * preceded and followed
     * by an asterisk.
     *
     * @return A String that is used to mark the beginning of a warning.
     */
    static String warningStart(ViewFormatting formatting) {
        return switch (formatting) {
            case HTML -> "<font color=red>";
            case NONE -> "*";
            case DISCORD -> DiscordFormat.RED.toString();
        };
    }

    /**
     * Returns the end element of the warning text.
     *
     * @return A String that is used to mark the end of a warning.
     */
    static String warningEnd(ViewFormatting formatting) {
        return switch (formatting) {
            case HTML -> "</font>";
            case NONE -> "*";
            case DISCORD -> DiscordFormat.RESET.toString();
        };
    }

    static String textWithTooltip(String text, String tooltip, ViewFormatting formatting) {
        return switch (formatting) {
            case HTML -> "<span title=\"" + tooltip + "\">" + text + "*</span>";
            case NONE, DISCORD -> text;
        };
    }

    static String splitDateRange(String text, ViewFormatting formatting) {
        return switch (formatting) {
            case HTML -> text.replace(", ", "<br>");
            case NONE, DISCORD -> text;
        };
    }

    default String applyFormatting(String text, ViewFormatting formatting) {
        return ReadoutMarkup.applyFormatting(text, formatting);
    }
}
