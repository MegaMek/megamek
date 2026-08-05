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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Covers chat command lookup, in particular that the abbreviation the mutual support command answered to before it was
 * renamed still resolves.
 */
class ChatCommandsTest {

    @Test
    void theMutualSupportCommandResolvesFromItsAbbreviation() {
        assertEquals(ChatCommands.MUTUAL_SUPPORT, ChatCommands.getByValue("ms"));
    }

    /**
     * Players have the old abbreviation in muscle memory and in saved macros, so typing it must keep working even
     * though the command is now called mutual support everywhere it is displayed.
     */
    @Test
    void theAbbreviationUsedBeforeTheRenameStillResolves() {
        assertEquals(ChatCommands.MUTUAL_SUPPORT, ChatCommands.getByValue("he"));
    }

    @Test
    void anUnknownAbbreviationStillResolvesToNothing() {
        assertNull(ChatCommands.getByValue("zzz"));
    }

    @Test
    void everyCommandKeepsAUniqueAbbreviation() {
        for (ChatCommands command : ChatCommands.values()) {
            assertEquals(command,
                  ChatCommands.getByValue(command.getAbbreviation()),
                  command + " does not resolve from its own abbreviation, so another command has taken it");
        }
    }
}
