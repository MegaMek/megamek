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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.client.bot.princess.IHonorUtil;
import megamek.client.bot.princess.Princess;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BloodFeudCommand}: dishonoring a player must flag it on the bot's {@link IHonorUtil} and report the
 * updated honor state to clients so the dishonor warning stays in sync.
 */
class BloodFeudCommandTest {

    private static final int TARGET_PLAYER_ID = 5;

    private BloodFeudCommand bloodFeudCommand;
    private Princess mockPrincess;
    private Game mockGame;
    private IHonorUtil mockHonorUtil;

    @BeforeEach
    void beforeEach() {
        bloodFeudCommand = new BloodFeudCommand();
        mockPrincess = mock(Princess.class);
        mockGame = mock(Game.class);
        mockHonorUtil = mock(IHonorUtil.class);
        when(mockPrincess.getGame()).thenReturn(mockGame);
        when(mockPrincess.getHonorUtil()).thenReturn(mockHonorUtil);
    }

    private Arguments parseArguments(String playerId) {
        return ArgumentsParser.parse(new String[] { "bf", playerId }, bloodFeudCommand.defineArguments());
    }

    @Test
    void dishonorsPlayerAndReportsWhenPlayerExists() {
        when(mockGame.getPlayer(TARGET_PLAYER_ID)).thenReturn(mock(Player.class));

        bloodFeudCommand.execute(mockPrincess, parseArguments(String.valueOf(TARGET_PLAYER_ID)));

        verify(mockHonorUtil).setEnemyDishonored(TARGET_PLAYER_ID);
        verify(mockPrincess).sendDishonoredData();
    }

    @Test
    void doesNothingWhenPlayerNotFound() {
        when(mockGame.getPlayer(TARGET_PLAYER_ID)).thenReturn(null);

        bloodFeudCommand.execute(mockPrincess, parseArguments(String.valueOf(TARGET_PLAYER_ID)));

        verify(mockHonorUtil, never()).setEnemyDishonored(TARGET_PLAYER_ID);
        verify(mockPrincess, never()).sendDishonoredData();
    }
}
