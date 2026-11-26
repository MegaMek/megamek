/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.entityreadout;

import java.util.Objects;

import megamek.client.ui.util.DiscordFormat;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Displays a hyperlink.
 */
class HyperLinkLine extends LabeledLine {

    private final String address;

    HyperLinkLine(String label, String address, String displayText) {
        super(Objects.requireNonNullElse(label, ""), displayText);
        this.address = address;
    }

    @Override
    public String toPlainText() {
        String result = label.isBlank() ? "" : label + ": ";
        return result + value.toPlainText() + "\n";
    }

    @Override
    public String toHTML() {
        String result = label.isBlank() ? "" : label + ": ";
        return result + "<B><A " + hrefAttribute(address) + ">" + value.toHTML() + "</A></B><BR>";
    }

    /**
     * @param address A link target, such as a shop URL from a sourcebook file
     *
     * @return The quoted, HTML-escaped {@code HREF} attribute for the address, so a URL with {@code &} or a quote
     *       cannot break the surrounding markup
     */
    static String hrefAttribute(String address) {
        return "HREF=\"" + StringEscapeUtils.escapeHtml4(Objects.requireNonNullElse(address, "")) + "\"";
    }

    @Override
    public String toDiscord() {
        String result = label.isBlank() ? "" : DiscordFormat.BOLD + label + ": " + DiscordFormat.RESET;
        return result + value.toDiscord() + "\n";
    }
}
