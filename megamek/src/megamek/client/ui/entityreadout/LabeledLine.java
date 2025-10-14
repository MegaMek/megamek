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
package megamek.client.ui.entityreadout;

import megamek.client.ui.util.DiscordFormat;

/**
 * Basic one-line entry consisting of a label, a colon, and a value.
 */
class LabeledLine implements ViewElement {

    private static final String PLAIN_FORMAT = "%s: %s\n";
    private static final String HTML_FORMAT = "%s: <B>%s</B><BR>\n";

    protected final String label;
    protected final ViewElement value;

    LabeledLine(String label, String value) {
        this.label = label;
        this.value = new PlainElement(value);
    }

    LabeledLine(String label, ViewElement value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String toPlainText() {
        return PLAIN_FORMAT.formatted(label, value.toPlainText());
    }

    @Override
    public String toHTML() {
        return HTML_FORMAT.formatted(label, value.toHTML());
    }

    @Override
    public String toDiscord() {
        return label + ": " + DiscordFormat.highlightNumbersForDiscord(value.toDiscord()) + '\n';
    }
}
