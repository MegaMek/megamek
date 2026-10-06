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

import megamek.client.bot.princess.AerospaceGroundOrder;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AeroLiftCommand}: the ground-or-sky order for DropShips and small craft must
 * round-trip from a chat line for all three states - the whole transport for the future bot
 * commands panel control.
 */
class AeroLiftCommandTest {

    private AeroLiftCommand aeroLiftCommand;
    private Princess mockPrincess;

    @BeforeEach
    void beforeEach() {
        aeroLiftCommand = new AeroLiftCommand();
        mockPrincess = mock(Princess.class);
    }

    private Arguments parseArguments(String... chatArgumentsAfterCommand) {
        String[] chatArguments = new String[chatArgumentsAfterCommand.length + 1];
        chatArguments[0] = "al";
        System.arraycopy(chatArgumentsAfterCommand, 0, chatArguments, 1, chatArgumentsAfterCommand.length);
        return ArgumentsParser.parse(chatArguments, aeroLiftCommand.defineArguments());
    }

    @Test
    void liftOffOrdersTheShipsUp() {
        aeroLiftCommand.execute(mockPrincess, parseArguments("LIFT_OFF"));

        verify(mockPrincess).setAerospaceGroundOrder(AerospaceGroundOrder.LIFT_OFF);
    }

    @Test
    void landOrdersTheShipsDown() {
        aeroLiftCommand.execute(mockPrincess, parseArguments("LAND"));

        verify(mockPrincess).setAerospaceGroundOrder(AerospaceGroundOrder.LAND);
    }

    @Test
    void autoReleasesTheShipsToDoctrine() {
        aeroLiftCommand.execute(mockPrincess, parseArguments("AUTO"));

        verify(mockPrincess).setAerospaceGroundOrder(AerospaceGroundOrder.AUTO);
    }
}
