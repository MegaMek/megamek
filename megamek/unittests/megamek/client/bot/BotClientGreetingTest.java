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
package megamek.client.bot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.bot.caspar.Caspar;
import megamek.client.bot.princess.Princess;
import org.junit.jupiter.api.Test;

/**
 * Tests that a bot's lobby greeting names its AI type, so a player who switches a bot from Princess to CASPAR (or
 * back) can see in the chat which one actually joined.
 */
class BotClientGreetingTest {

    @Test
    void princessGreetingNamesPrincess() {
        BotClient princess = new Princess("Princess", "localhost", 0);
        String princessName = Messages.getString("BotClient.aiType.PRINCESS");

        assertTrue(princess.getLoungeGreeting().contains(princessName),
              "Greeting should name the Princess AI: " + princess.getLoungeGreeting());
    }

    @Test
    void casparGreetingNamesCaspar() {
        BotClient caspar = new Caspar("Princess", "localhost", 0);
        String casparName = Messages.getString("BotClient.aiType.CASPAR");

        assertTrue(caspar.getLoungeGreeting().contains(casparName),
              "Greeting should name the CASPAR AI: " + caspar.getLoungeGreeting());
    }

    @Test
    void sameNamedBotsOfDifferentTypesGreetDifferently() {
        // The reported case: a bot keeps the name "Princess" after being switched to CASPAR
        BotClient princess = new Princess("Princess", "localhost", 0);
        BotClient caspar = new Caspar("Princess", "localhost", 0);

        assertNotEquals(princess.getLoungeGreeting(), caspar.getLoungeGreeting());
    }

    @Test
    void greetingKeepsItsApostropheAndHasNoUnfilledPlaceholder() {
        // MessageFormat drops a single apostrophe and swallows what follows it, so "I'm" must be written "I''m"
        BotClient caspar = new Caspar("Princess", "localhost", 0);
        String greeting = caspar.getLoungeGreeting();

        assertFalse(greeting.contains("{0}"), "Greeting placeholder was not filled: " + greeting);
        assertFalse(greeting.contains("!BotClient."), "Greeting key is missing from the bundle: " + greeting);
        assertTrue(greeting.contains("'"), "Greeting lost its apostrophe: " + greeting);
    }
}
