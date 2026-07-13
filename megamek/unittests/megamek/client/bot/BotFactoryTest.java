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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.client.bot.caspar.Caspar;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.PrincessException;
import org.junit.jupiter.api.Test;

class BotFactoryTest {

    private static final String TEST_HOST = "localhost";
    private static final int TEST_PORT = 2020;

    @Test
    void createBotBuildsPrincessForPrincessType() {
        BotClient bot = BotFactory.createBot(AIType.PRINCESS, "TestBot", TEST_HOST, TEST_PORT);
        try {
            assertNotNull(bot);
            assertInstanceOf(Princess.class, bot);
            assertEquals("TestBot", bot.getName());
        } finally {
            if (bot != null) {
                bot.die();
            }
        }
    }

    @Test
    void createBotBuildsCasparForCasparType() {
        BotClient bot = BotFactory.createBot(AIType.CASPAR, "TestBot", TEST_HOST, TEST_PORT);
        try {
            assertNotNull(bot);
            assertInstanceOf(Caspar.class, bot);
            assertEquals("TestBot", bot.getName());
        } finally {
            if (bot != null) {
                bot.die();
            }
        }
    }

    @Test
    void createBotAppliesBehaviorSettings() throws PrincessException {
        BehaviorSettings behavior = new BehaviorSettings();
        behavior.setDescription("FactoryTestBehavior");
        BotClient bot = BotFactory.createBot(AIType.PRINCESS, "TestBot", TEST_HOST, TEST_PORT, behavior);
        try {
            assertEquals("FactoryTestBehavior", bot.getBehaviorSettings().getDescription());
        } finally {
            if (bot != null) {
                bot.die();
            }
        }
    }
}
