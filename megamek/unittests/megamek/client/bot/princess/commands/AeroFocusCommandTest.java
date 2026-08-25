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
package megamek.client.bot.princess.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import megamek.client.bot.princess.AerospaceFocus;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AeroFocusCommand}: the chat command must set the bot's standing aerospace focus
 * order for each of the three states. The command is the whole transport for the future bot
 * commands panel control, so a working round-trip here means the order can be issued today from a
 * plain chat line.
 */
class AeroFocusCommandTest {

    private AeroFocusCommand aeroFocusCommand;
    private Princess mockPrincess;

    @BeforeEach
    void beforeEach() {
        aeroFocusCommand = new AeroFocusCommand();
        mockPrincess = mock(Princess.class);
    }

    private Arguments parseArguments(String... chatArgumentsAfterCommand) {
        String[] chatArguments = new String[chatArgumentsAfterCommand.length + 1];
        chatArguments[0] = "af";
        System.arraycopy(chatArgumentsAfterCommand, 0, chatArguments, 1, chatArgumentsAfterCommand.length);
        return ArgumentsParser.parse(chatArguments, aeroFocusCommand.defineArguments());
    }

    @Test
    void focusAerospaceSetsTheAirPriorityOrder() {
        aeroFocusCommand.execute(mockPrincess, parseArguments("AEROSPACE"));

        verify(mockPrincess).setAerospaceFocus(AerospaceFocus.AEROSPACE);
    }

    @Test
    void focusGroundSetsTheGroundSupportOrder() {
        aeroFocusCommand.execute(mockPrincess, parseArguments("GROUND"));

        verify(mockPrincess).setAerospaceFocus(AerospaceFocus.GROUND);
    }

    @Test
    void focusAutoLiftsTheStandingOrder() {
        aeroFocusCommand.execute(mockPrincess, parseArguments("AUTO"));

        verify(mockPrincess).setAerospaceFocus(AerospaceFocus.AUTO);
    }
}
