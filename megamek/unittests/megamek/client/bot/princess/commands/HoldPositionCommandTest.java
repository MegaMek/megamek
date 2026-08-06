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

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HoldPositionCommand}: hold-position must set the bot's hold flag, and hold-position with {@code false} must
 * lift it.
 *
 * @author HammerGS
 */
class HoldPositionCommandTest {

    private HoldPositionCommand holdPositionCommand;
    private Princess mockPrincess;

    @BeforeEach
    void beforeEach() {
        holdPositionCommand = new HoldPositionCommand();
        mockPrincess = mock(Princess.class);
    }

    private Arguments parseArguments(String... chatArgumentsAfterCommand) {
        String[] chatArguments = new String[chatArgumentsAfterCommand.length + 1];
        chatArguments[0] = "hp";
        System.arraycopy(chatArgumentsAfterCommand, 0, chatArguments, 1, chatArgumentsAfterCommand.length);
        return ArgumentsParser.parse(chatArguments, holdPositionCommand.defineArguments());
    }

    @Test
    void testHoldPositionWithoutArgumentHolds() {
        holdPositionCommand.execute(mockPrincess, parseArguments());

        verify(mockPrincess).setHoldPosition(true);
    }

    @Test
    void testHoldPositionTrueHolds() {
        holdPositionCommand.execute(mockPrincess, parseArguments("true"));

        verify(mockPrincess).setHoldPosition(true);
    }

    @Test
    void testHoldPositionFalseResumesMovement() {
        holdPositionCommand.execute(mockPrincess, parseArguments("false"));

        verify(mockPrincess).setHoldPosition(false);
    }
}
