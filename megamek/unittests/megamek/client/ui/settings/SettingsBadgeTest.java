/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.util.List;

import org.junit.jupiter.api.Test;

class SettingsBadgeTest {
    @Test
    void formatHtmlPreservesOrderAndOptionalColor() {
        SettingsBadge inheritedColor = new SettingsBadge(0xE002, null, "Important");
        SettingsBadge colored = new SettingsBadge(0xE838, new Color(0x12, 0x34, 0x56), "Recent");

        assertEquals(
              " <font face=\"Material Symbols Rounded\">\uE002</font>"
                    + " <font face=\"Material Symbols Rounded\" color=\"#123456\">\uE838</font>",
              SettingsBadge.formatHtml(List.of(inheritedColor, colored)));
    }

    @Test
    void formatHtmlHandlesMissingBadges() {
        assertEquals("", SettingsBadge.formatHtml(null));
        assertEquals("", SettingsBadge.formatHtml(List.of()));
    }

    @Test
    void badgeRejectsInvalidCodePoint() {
        assertThrows(IllegalArgumentException.class, () -> new SettingsBadge(-1, null, "Invalid"));
    }
}
