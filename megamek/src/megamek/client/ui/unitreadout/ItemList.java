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

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a label (bold for html and discord output) followed by a column of items
 */
class ItemList implements ViewElement {

    private final String heading;
    private final List<String> data = new ArrayList<>();

    ItemList(String heading) {
        this.heading = heading;
    }

    void addItem(String item) {
        data.add(item);
    }

    @Override
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        if (null != heading) {
            sb.append(heading).append("\n");
            sb.append("-".repeat(heading.length()));
            sb.append("\n");
        }
        for (String item : data) {
            sb.append(item).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toHTML() {
        StringBuilder sb = new StringBuilder();
        if (null != heading) {
            sb.append("<b>").append(heading).append("</b><br/>\n");
        }
        for (String item : data) {
            sb.append(item).append("<br/>\n");
        }
        return sb.toString();
    }

    @Override
    public String toDiscord() {
        StringBuilder sb = new StringBuilder();
        if (null != heading) {
            sb.append(DiscordFormat.BOLD).append(heading).append(DiscordFormat.RESET).append('\n');
        }
        boolean evenLine = false;
        for (String item : data) {
            if (evenLine) {
                sb.append(DiscordFormat.ROW_SHADING);
            }
            sb.append(DiscordFormat.highlightNumbersForDiscord(item)).append("\n");
            if (evenLine) {
                sb.append(DiscordFormat.RESET);
            }
            evenLine = !evenLine;
        }
        return sb.toString();
    }
}
