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

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.DiscordFormat;
import megamek.client.ui.util.UIUtil;

class DestroyedElement implements ViewElement {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final String HTML_FORMAT = "<FONT %s>%s</FONT>";

    private final String text;

    public DestroyedElement(String text) {
        this.text = text;
    }

    public DestroyedElement(int number) {
        text = String.valueOf(number);
    }

    @Override
    public String toPlainText() {
        return text;
    }

    @Override
    public String toHTML() {
        String colorString = UIUtil.colorString(GUIP.getWarningColor());
        return HTML_FORMAT.formatted(colorString, text);
    }

    @Override
    public String toDiscord() {
        return DiscordFormat.RED + text + DiscordFormat.RESET;
    }
}
