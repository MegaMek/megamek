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

package megamek.client.ui.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.Version;
import org.junit.jupiter.api.Test;

class RefusedConnectionNotifierTest {
    private static final Version REFUSED_VERSION = new Version("0.51.01.1");
    private static final Version GAME_VERSION = new Version("0.51.01.2");

    @Test
    void playerMessageNamesThePlayerAndBothVersions() {
        String message = RefusedConnectionNotifier.buildPlayerMessage("Natasha Kerensky", REFUSED_VERSION,
              GAME_VERSION);
        assertTrue(message.contains("Natasha Kerensky"));
        assertTrue(message.contains(REFUSED_VERSION.toString()));
        assertTrue(message.contains(GAME_VERSION.toString()));
    }

    @Test
    void playerMessageWorksWithoutAName() {
        String message = RefusedConnectionNotifier.buildPlayerMessage(null, REFUSED_VERSION, GAME_VERSION);
        assertTrue(message.contains(REFUSED_VERSION.toString()));
        assertFalse(message.contains("null"));
    }

    @Test
    void playerMessageIsNeverRenderedAsHtml() {
        // Swing renders dialog text beginning with <html> as HTML, and the refused player picks their own name.
        String message = RefusedConnectionNotifier.buildPlayerMessage("<html><b>Gotcha</b>", REFUSED_VERSION,
              GAME_VERSION);
        assertFalse(message.toLowerCase().startsWith("<html>"));
    }
}
