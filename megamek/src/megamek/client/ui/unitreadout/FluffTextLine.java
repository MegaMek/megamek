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

/**
 * This Element is a specific for fluff text. It emphasizes the label and cleans HTML tags from the text for output in
 * formats other than HTML (in other words, allows HTML tags in the text).
 */
class FluffTextLine implements ViewElement {

    private static final String PLAIN_FORMAT = "%s: %s\n";
    private static final String HTML_FORMAT = "<B>%s</B>: %s<BR>\n";

    private final String label;
    private final String value;

    FluffTextLine(String label, String value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String toPlainText() {
        return PLAIN_FORMAT.formatted(label, htmlCleanedText());
    }

    @Override
    public String toHTML() {
        return HTML_FORMAT.formatted(label, value);
    }

    @Override
    public String toDiscord() {
        return DiscordFormat.BOLD + label + DiscordFormat.RESET + ": "
              + DiscordFormat.highlightNumbersForDiscord(htmlCleanedText()) + '\n';
    }

    private String htmlCleanedText() {
        return value.replaceAll("<[Bb][Rr]> *", "\n")
              .replaceAll("<[Pp]> *", "\n\n")
              .replaceAll("</[Pp]> *", "\n")
              .replaceAll("<[^>]*>", "");
    }
}
