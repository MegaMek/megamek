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
import megamek.server.RefusedConnection;
import org.junit.jupiter.api.Test;

class RefusedConnectionNotifierTest {
    private static final Version CLIENT_VERSION = new Version("0.51.01.1");
    private static final Version SERVER_VERSION = new Version("0.51.01.2");

    @Test
    void hostMessageNamesThePlayerAndBothVersions() {
        RefusedConnection refused = RefusedConnection.fromReceived("Natasha Kerensky", "/203.0.113.7",
              CLIENT_VERSION, SERVER_VERSION);
        String message = RefusedConnectionNotifier.buildHostMessage(refused);
        assertTrue(message.contains("Natasha Kerensky"));
        assertTrue(message.contains("203.0.113.7"));
        assertTrue(message.contains(CLIENT_VERSION.toString()));
        assertTrue(message.contains(SERVER_VERSION.toString()));
    }

    @Test
    void hostMessageFallsBackToTheAddressForOlderClients() {
        RefusedConnection refused = RefusedConnection.fromReceived(null, "/203.0.113.7", CLIENT_VERSION,
              SERVER_VERSION);
        assertTrue(RefusedConnectionNotifier.buildHostMessage(refused).contains("203.0.113.7"));
    }

    @Test
    void hostMessageIsNeverRenderedAsHtml() {
        // Swing renders dialog text beginning with <html> as HTML, and the player picks their own name.
        RefusedConnection refused = RefusedConnection.fromReceived("<html><b>Gotcha</b>", "/203.0.113.7",
              CLIENT_VERSION, SERVER_VERSION);
        assertFalse(RefusedConnectionNotifier.buildHostMessage(refused).toLowerCase().startsWith("<html>"));
    }
}
