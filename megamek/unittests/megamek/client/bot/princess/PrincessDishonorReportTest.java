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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import megamek.common.Player;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Princess#resolveDishonoredPlayerIds()}, which builds the list of players a bot reports as dishonored
 * through {@link megamek.common.net.enums.PacketCommand#PRINCESS_DISHONORED}. The list must exactly match the bot's own
 * {@code HonorUtil.isEnemyDishonored} verdict, including the pirate case where the bot has no honor to give.
 */
class PrincessDishonorReportTest {

    private Princess princess;
    private Game mockGame;

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        mockGame = mock(Game.class);
        doReturn(mockGame).when(princess).getGame();
        // Build the player mocks before stubbing getPlayersList; nesting mock()/when() inside the thenReturn argument
        // trips Mockito's UnfinishedStubbingException.
        List<Player> players = List.of(player(1), player(2), player(3));
        when(mockGame.getPlayersList()).thenReturn(players);
    }

    private static Player player(int id) {
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(id);
        return player;
    }

    @Test
    void reportsNobodyWhenNoOneIsDishonored() {
        assertTrue(princess.resolveDishonoredPlayerIds().isEmpty());
    }

    @Test
    void reportsOnlyTheDishonoredPlayer() {
        princess.getHonorUtil().setEnemyDishonored(2);
        assertEquals(List.of(2), princess.resolveDishonoredPlayerIds());
    }

    @Test
    void reportsEveryPlayerAddedToTheGrudgeList() {
        princess.getHonorUtil().setEnemyDishonored(1);
        princess.getHonorUtil().setEnemyDishonored(3);
        assertEquals(List.of(1, 3), princess.resolveDishonoredPlayerIds());
    }

    @Test
    void pirateReportsEveryPlayerAsDishonored() {
        // A pirate has no honor to give, so HonorUtil.isEnemyDishonored is true for everyone.
        ((HonorUtil) princess.getHonorUtil()).setIAmAPirate(true);
        assertEquals(List.of(1, 2, 3), princess.resolveDishonoredPlayerIds());
    }
}
