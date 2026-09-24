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
package megamek.client.ui.dialogs.BotCommands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.IGame;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the static helpers of {@link BotCommandsPanel}:
 * <ul>
 *     <li>{@code buildVictoryCommand} - the "Request Victory" button must send the server password so
 *     {@code VictoryCommand} accepts it on a passworded server (issue #8891).</li>
 *     <li>{@code isGamePausable} - pausing must be refused at victory (issue #8888) and while a human owns units.</li>
 * </ul>
 */
class BotCommandsPanelStaticsTest {

    // region buildVictoryCommand (#8891)

    @Test
    void victoryCommandWithoutPasswordIsBare() {
        assertEquals("/victory", BotCommandsPanel.buildVictoryCommand(""));
    }

    @Test
    void victoryCommandWithNullPasswordIsBare() {
        assertEquals("/victory", BotCommandsPanel.buildVictoryCommand(null));
    }

    @Test
    void victoryCommandWithBlankPasswordIsBare() {
        assertEquals("/victory", BotCommandsPanel.buildVictoryCommand("   "));
    }

    @Test
    void victoryCommandWithPasswordAppendsIt() {
        assertEquals("/victory hunter2", BotCommandsPanel.buildVictoryCommand("hunter2"));
    }

    // endregion

    // region isGamePausable (#8888)

    private static Player bot() {
        Player player = mock(Player.class);
        when(player.isBot()).thenReturn(true);
        return player;
    }

    private static Player human() {
        Player player = mock(Player.class);
        when(player.isBot()).thenReturn(false);
        return player;
    }

    private static IGame gameInPhase(GamePhase phase, List<Player> players) {
        IGame game = mock(IGame.class);
        when(game.getPhase()).thenReturn(phase);
        when(game.getPlayersList()).thenReturn(players);
        return game;
    }

    @Test
    void botOnlyGameInPlayCanBePaused() {
        IGame game = gameInPhase(GamePhase.MOVEMENT, List.of(bot(), bot()));
        assertTrue(BotCommandsPanel.isGamePausable(game));
    }

    @Test
    void gameAtVictoryCannotBePaused() {
        // Even a bot-only game must not be paused once it is over: pausing there freezes the server and blocks the
        // hand-off back to MekHQ. See issue #8888.
        IGame game = gameInPhase(GamePhase.VICTORY, List.of(bot(), bot()));
        assertFalse(BotCommandsPanel.isGamePausable(game));
    }

    @Test
    void gameWithAHumanOwningUnitsCannotBePaused() {
        Player human = human();
        IGame game = gameInPhase(GamePhase.MOVEMENT, List.of(human, bot()));
        when(game.getEntitiesOwnedBy(human)).thenReturn(3);
        assertFalse(BotCommandsPanel.isGamePausable(game));
    }

    @Test
    void gameWithAHumanOwningNoUnitsCanBePaused() {
        Player human = human();
        IGame game = gameInPhase(GamePhase.MOVEMENT, List.of(human, bot()));
        when(game.getEntitiesOwnedBy(human)).thenReturn(0);
        assertTrue(BotCommandsPanel.isGamePausable(game));
    }

    // endregion
}
