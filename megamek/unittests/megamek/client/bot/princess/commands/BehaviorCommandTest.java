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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Regression tests for {@link BehaviorCommand}: saved behavior names may contain spaces, and the command must still
 * resolve them even though chat command arguments are split on spaces.
 *
 * @author HammerGS
 */
class BehaviorCommandTest {

    private static final String MULTI_WORD_BEHAVIOR = "TEST SNIPER BEHAVIOR";

    private BehaviorCommand behaviorCommand;
    private Princess mockPrincess;

    @BeforeEach
    void beforeEach() throws Exception {
        behaviorCommand = new BehaviorCommand();
        mockPrincess = mock(Princess.class);

        BehaviorSettings multiWordBehavior = new BehaviorSettings();
        multiWordBehavior.setDescription(MULTI_WORD_BEHAVIOR);
        BehaviorSettingsFactory.getInstance().addBehavior(multiWordBehavior);
    }

    @AfterEach
    void afterEach() {
        BehaviorSettingsFactory.getInstance().removeBehavior(MULTI_WORD_BEHAVIOR);
    }

    private Arguments parseArguments(String... behaviorNameWords) {
        String[] chatArguments = new String[behaviorNameWords.length + 1];
        chatArguments[0] = "be";
        System.arraycopy(behaviorNameWords, 0, chatArguments, 1, behaviorNameWords.length);
        return ArgumentsParser.parse(chatArguments, behaviorCommand.defineArguments());
    }

    @Test
    void testMultiWordBehaviorNameIsResolved() {
        // chat arguments arrive split on spaces: "be : TEST SNIPER BEHAVIOR" -> [TEST, SNIPER, BEHAVIOR]
        behaviorCommand.execute(mockPrincess, parseArguments("TEST", "SNIPER", "BEHAVIOR"));

        ArgumentCaptor<BehaviorSettings> captor = ArgumentCaptor.forClass(BehaviorSettings.class);
        verify(mockPrincess).setBehaviorSettings(captor.capture());
        assertEquals(MULTI_WORD_BEHAVIOR, captor.getValue().getDescription());
    }

    @Test
    void testSingleWordBehaviorNameStillWorks() {
        behaviorCommand.execute(mockPrincess, parseArguments(
              BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION));

        ArgumentCaptor<BehaviorSettings> captor = ArgumentCaptor.forClass(BehaviorSettings.class);
        verify(mockPrincess).setBehaviorSettings(captor.capture());
        assertEquals(BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION, captor.getValue().getDescription());
    }

    @Test
    void testUnknownBehaviorDoesNotChangeSettings() {
        behaviorCommand.execute(mockPrincess, parseArguments("NO", "SUCH", "BEHAVIOR"));

        verify(mockPrincess, never()).setBehaviorSettings(any(BehaviorSettings.class));
    }
}
