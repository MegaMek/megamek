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
package megamek.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameToastEvent;
import megamek.common.game.BotHonorReport;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@code Client.receivePrincessDishonored} notifies the local player the first time an enemy bot marks them
 * dishonored, and does not re-notify or notify about other players.
 */
class ClientDishonorNoticeTest {

    private static final int LOCAL_PLAYER_ID = 0;
    private static final int BOT_PLAYER_ID = 1;
    private static final int OTHER_PLAYER_ID = 2;

    private Client client;
    private final AtomicInteger toasts = new AtomicInteger();

    @BeforeEach
    void setUp() {
        client = new Client("Test Player", "localhost", 1234);
        client.getGame().addPlayer(LOCAL_PLAYER_ID, new Player(LOCAL_PLAYER_ID, "Test Player"));
        client.getGame().addPlayer(BOT_PLAYER_ID, new Player(BOT_PLAYER_ID, "Princess"));
        client.setLocalPlayerNumber(LOCAL_PLAYER_ID);
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gameToast(GameToastEvent event) {
                toasts.incrementAndGet();
            }
        });
    }

    private Packet dishonoredPacket(int botPlayerId, List<Integer> dishonoredPlayerIds) {
        return new Packet(PacketCommand.PRINCESS_DISHONORED, botPlayerId,
              new BotHonorReport(dishonoredPlayerIds, true, Set.of()));
    }

    @Test
    void notifiesWhenLocalPlayerIsNewlyDishonored() throws Exception {
        client.receivePrincessDishonored(dishonoredPacket(BOT_PLAYER_ID, List.of(LOCAL_PLAYER_ID)));

        assertEquals(1, toasts.get());
        assertTrue(client.getGame().isPlayerDishonoredBy(BOT_PLAYER_ID, LOCAL_PLAYER_ID));
    }

    @Test
    void doesNotNotifyAgainWhenAlreadyDishonored() throws Exception {
        client.receivePrincessDishonored(dishonoredPacket(BOT_PLAYER_ID, List.of(LOCAL_PLAYER_ID)));
        // The bot re-reports the same grudge every round; the player must not be nagged again.
        client.receivePrincessDishonored(dishonoredPacket(BOT_PLAYER_ID, List.of(LOCAL_PLAYER_ID)));

        assertEquals(1, toasts.get());
    }

    @Test
    void doesNotNotifyWhenAnotherPlayerIsDishonored() throws Exception {
        client.receivePrincessDishonored(dishonoredPacket(BOT_PLAYER_ID, List.of(OTHER_PLAYER_ID)));

        assertEquals(0, toasts.get());
        // State is still recorded for the other player, just not surfaced to us.
        assertTrue(client.getGame().isPlayerDishonoredBy(BOT_PLAYER_ID, OTHER_PLAYER_ID));
    }

    @Test
    void doesNotNotifyWhenClearedReportArrivesFirst() throws Exception {
        // A report that does not list the local player is not a transition into dishonor, so no notice.
        client.receivePrincessDishonored(dishonoredPacket(BOT_PLAYER_ID, List.of()));

        assertEquals(0, toasts.get());
    }

    @Test
    void recordsWhichBotUnitsAreWithdrawing() throws Exception {
        Entity withdrawingUnit = mock(Entity.class);
        when(withdrawingUnit.getId()).thenReturn(20);
        when(withdrawingUnit.getOwnerId()).thenReturn(BOT_PLAYER_ID);
        Entity fightingUnit = mock(Entity.class);
        when(fightingUnit.getId()).thenReturn(21);
        when(fightingUnit.getOwnerId()).thenReturn(BOT_PLAYER_ID);

        client.receivePrincessDishonored(new Packet(PacketCommand.PRINCESS_DISHONORED, BOT_PLAYER_ID,
              new BotHonorReport(List.of(), true, Set.of(20))));

        assertTrue(client.getGame().getForcedWithdrawalReports().isWithdrawing(withdrawingUnit));
        assertFalse(client.getGame().getForcedWithdrawalReports().isWithdrawing(fightingUnit));
    }

    @Test
    void rejectsAPacketWithoutAReport() {
        assertThrows(InvalidPacketDataException.class, () -> client.receivePrincessDishonored(
              new Packet(PacketCommand.PRINCESS_DISHONORED, BOT_PLAYER_ID, List.of(LOCAL_PLAYER_ID))));
    }

    @Test
    void returningToTheLobbyForgetsWithdrawingUnits() throws Exception {
        // Seen in play: last game's crippled unit 3 tagged the next game's unhurt unit 3 at deployment.
        Entity unitThree = mock(Entity.class);
        when(unitThree.getId()).thenReturn(3);
        when(unitThree.getOwnerId()).thenReturn(BOT_PLAYER_ID);
        client.receivePrincessDishonored(new Packet(PacketCommand.PRINCESS_DISHONORED, BOT_PLAYER_ID,
              new BotHonorReport(List.of(), true, Set.of(3))));

        client.changePhase(GamePhase.LOUNGE);

        assertFalse(client.getGame().getForcedWithdrawalReports().isWithdrawing(unitThree));
    }
}
