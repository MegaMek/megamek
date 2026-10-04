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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import megamek.common.Player;
import megamek.common.game.BotHonorReport;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Tests that the server passes a bot's honor report on to every client, tagged with the bot's player ID, and drops
 * anything a human client or a malformed packet tries to pass off as one. The report is what lets clients tag a bot's
 * withdrawing units and warn before a dishonorable attack.
 */
class HonorReportRelayTest {

    private static final int BOT_ID = 0;
    private static final int HUMAN_ID = 1;

    private TWGameManager gameManager;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));

        Player bot = new Player(BOT_ID, "Princess");
        bot.setBot(true);
        gameManager.getGame().addPlayer(BOT_ID, bot);
        gameManager.getGame().addPlayer(HUMAN_ID, new Player(HUMAN_ID, "Human"));
    }

    private static BotHonorReport withdrawingReport() {
        return new BotHonorReport(List.of(HUMAN_ID), true, Set.of(10, 11));
    }

    @Test
    void aBotsReportIsRelayedToEveryoneWithItsPlayerId() {
        BotHonorReport report = withdrawingReport();

        gameManager.handlePacket(BOT_ID, new Packet(PacketCommand.PRINCESS_DISHONORED, report));

        ArgumentCaptor<Packet> relayed = ArgumentCaptor.forClass(Packet.class);
        verify(gameManager).send(relayed.capture());
        assertEquals(PacketCommand.PRINCESS_DISHONORED, relayed.getValue().command());
        assertEquals(BOT_ID, relayed.getValue().getObject(0));
        assertSame(report, relayed.getValue().getObject(1));
    }

    @Test
    void aHumanCannotReportUnitsAsWithdrawing() {
        gameManager.handlePacket(HUMAN_ID, new Packet(PacketCommand.PRINCESS_DISHONORED, withdrawingReport()));

        verify(gameManager, never()).send(any(Packet.class));
    }

    @Test
    void aBotPacketWithoutAReportIsDropped() {
        // The old format, a bare list of player IDs, carries no Forced Withdrawal state and is no longer relayed.
        gameManager.handlePacket(BOT_ID, new Packet(PacketCommand.PRINCESS_DISHONORED, List.of(HUMAN_ID)));

        verify(gameManager, never()).send(any(Packet.class));
    }
}
