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

package megamek.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.Version;
import org.junit.jupiter.api.Test;

class RefusedConnectionTest {
    private static final Version CLIENT_VERSION = new Version("0.51.01.1");
    private static final Version SERVER_VERSION = new Version("0.51.01.2");

    @Test
    void nameSentByTheClientIsKept() {
        RefusedConnection refused = RefusedConnection.fromReceived("Natasha Kerensky", "/203.0.113.7",
              CLIENT_VERSION, SERVER_VERSION);
        assertTrue(refused.hasPlayerName());
        assertEquals("Natasha Kerensky", refused.playerName());
    }

    @Test
    void olderClientWithoutANameHasNoPlayerName() {
        RefusedConnection refused = RefusedConnection.fromReceived(null, "/203.0.113.7", CLIENT_VERSION,
              SERVER_VERSION);
        assertFalse(refused.hasPlayerName());
    }

    @Test
    void nameThatIsNotTextIsIgnored() {
        assertNull(RefusedConnection.cleanPlayerName(42));
    }

    @Test
    void blankNameIsIgnored() {
        assertNull(RefusedConnection.cleanPlayerName("   "));
    }

    @Test
    void nameIsFlattenedToOneLine() {
        assertEquals("Line One Line Two", RefusedConnection.cleanPlayerName("  Line One\n\nLine\tTwo  "));
    }

    @Test
    void overlongNameIsCut() {
        String overlongName = "A".repeat(RefusedConnection.MAXIMUM_PLAYER_NAME_LENGTH + 20);
        assertEquals(RefusedConnection.MAXIMUM_PLAYER_NAME_LENGTH,
              RefusedConnection.cleanPlayerName(overlongName).length());
    }

    @Test
    void leadingSlashIsRemovedFromTheAddress() {
        assertEquals("203.0.113.7", RefusedConnection.cleanAddress("/203.0.113.7"));
    }

    @Test
    void missingAddressIsReportedAsUnknown() {
        assertEquals("Unknown", RefusedConnection.cleanAddress(null));
    }
}
