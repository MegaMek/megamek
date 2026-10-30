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
package megamek.client.ui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import megamek.server.commands.KickCommand;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.Test;

/**
 * The Kick Player button opens the command dialog. On a server with no password the password field is left blank,
 * and the dialog used to send it anyway as "password=", which the server refused with "Positional arguments cannot
 * come after named arguments".
 */
class ClientCommandDialogArgumentsTest {

    private static final String KICKED_PLAYER = "2";
    private static final String PASSWORD = "password";

    /** @return the chat command split the way the server splits it, for parsing */
    private static String[] asTheServerSplitsIt(String chatCommand) {
        return chatCommand.split("\\s+");
    }

    /**
     * Finds an argument by its name, so the tests do not depend on the order {@link KickCommand} lists them in.
     *
     * @param definitions  The command's argument definitions
     * @param argumentName The name of the argument wanted
     *
     * @return The argument with that name
     */
    private static Argument<?> argumentNamed(List<Argument<?>> definitions, String argumentName) {
        for (Argument<?> argument : definitions) {
            if (argument.getName().equals(argumentName)) {
                return argument;
            }
        }
        throw new IllegalStateException("KickCommand has no argument named " + argumentName);
    }

    /** @return the Kick command's optional server password argument */
    private static Argument<?> kickPasswordArgument() {
        return argumentNamed(new KickCommand(null, null).defineArguments(), PASSWORD);
    }

    @Test
    void aBlankFieldIsLeftOut() {
        Argument<?> password = kickPasswordArgument();

        assertEquals("", ClientCommandDialog.namedArgumentUnlessBlank(password, ""));
        assertEquals("", ClientCommandDialog.namedArgumentUnlessBlank(password, "   "));
    }

    @Test
    void aFilledFieldIsSentByName() {
        Argument<?> password = kickPasswordArgument();

        assertEquals("password=secret", ClientCommandDialog.namedArgumentUnlessBlank(password, "secret"));
    }

    @Test
    void aKickWithNoPasswordNowParses() {
        List<Argument<?>> definitions = new KickCommand(null, null).defineArguments();
        String chatCommand = "/kick playerID=" + KICKED_PLAYER + " "
              + ClientCommandDialog.namedArgumentUnlessBlank(argumentNamed(definitions, PASSWORD), "");

        Arguments parsed = ArgumentsParser.parse(asTheServerSplitsIt(chatCommand), definitions);

        assertEquals(2, (int) parsed.get("playerID").getValue(), "the chosen player is the one kicked");
        assertTrue(((Optional<?>) parsed.get("password").getValue()).isEmpty(),
              "a blank password reaches the server as no password at all");
    }

    @Test
    void theOldBlankPasswordFormIsWhatTheServerRefused() {
        // pins the cause, so the test above is known to cover the reported failure and not a neighbouring one
        List<Argument<?>> definitions = new KickCommand(null, null).defineArguments();
        String[] oldForm = asTheServerSplitsIt("/kick playerID=" + KICKED_PLAYER + " password=");

        assertThrows(IllegalArgumentException.class, () -> ArgumentsParser.parse(oldForm, definitions));
    }
}
