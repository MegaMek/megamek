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
package megamek.server.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@code /ping}: who receives the ping packet, what the sender is told, and the per-sender cooldown.
 */
class PingCommandTest {

    private static final int HOST_CONNECTION = 0;
    private static final int GUEST_CONNECTION = 1;
    private static final int OBSERVER_CONNECTION = 2;
    private static final int GHOST_CONNECTION = 3;
    private static final String GUEST_NAME = "Clan Wolf";

    private Server server;
    private PingCommand command;
    private long nowMillis;

    @BeforeEach
    void setUp() {
        Game game = new Game();
        game.addPlayer(HOST_CONNECTION, new Player(HOST_CONNECTION, "Davion Guards"));
        game.addPlayer(GUEST_CONNECTION, new Player(GUEST_CONNECTION, GUEST_NAME));
        Player observer = new Player(OBSERVER_CONNECTION, "Watcher");
        observer.setObserver(true);
        game.addPlayer(OBSERVER_CONNECTION, observer);
        Player ghost = new Player(GHOST_CONNECTION, "Gone");
        ghost.setGhost(true);
        game.addPlayer(GHOST_CONNECTION, ghost);

        server = mock(Server.class);
        when(server.getGame()).thenReturn(game);
        nowMillis = 100_000;
        command = new PingCommand(server, () -> nowMillis);
    }

    private void runAs(int connection, String commandText) {
        command.run(connection, commandText.substring(1).split(" "));
    }

    private static boolean isPingFrom(Packet packet, int senderId, boolean toEveryone) {
        return (packet.command() == PacketCommand.PLAYER_PING)
              && Integer.valueOf(senderId).equals(packet.getObject(0))
              && Boolean.valueOf(toEveryone).equals(packet.getObject(1));
    }

    @Test
    void pingingOnePlayerSendsThePingToThemAlone() {
        runAs(HOST_CONNECTION, "/ping " + GUEST_NAME);

        verify(server).send(eq(GUEST_CONNECTION), argThat(packet -> isPingFrom(packet, HOST_CONNECTION, false)));
        verify(server, times(1)).send(anyInt(), any(Packet.class));
        verify(server).sendServerChat(HOST_CONNECTION, Messages.getString("PingCommand.sent", GUEST_NAME));
    }

    @Test
    void namesMatchIgnoringCase() {
        runAs(HOST_CONNECTION, "/ping clan WOLF");

        verify(server).send(eq(GUEST_CONNECTION), argThat(packet -> isPingFrom(packet, HOST_CONNECTION, false)));
    }

    @Test
    void pingingEveryoneSkipsTheSenderAndDisconnectedPlayers() {
        runAs(HOST_CONNECTION, "/ping");

        verify(server).send(eq(GUEST_CONNECTION), argThat(packet -> isPingFrom(packet, HOST_CONNECTION, true)));
        verify(server).send(eq(OBSERVER_CONNECTION), argThat(packet -> isPingFrom(packet, HOST_CONNECTION, true)));
        verify(server, never()).send(eq(HOST_CONNECTION), any(Packet.class));
        verify(server, never()).send(eq(GHOST_CONNECTION), any(Packet.class));
        verify(server).sendServerChat(HOST_CONNECTION, Messages.getString("PingCommand.sentEveryone"));
    }

    @Test
    void anObserverCanPing() {
        runAs(OBSERVER_CONNECTION, "/ping " + GUEST_NAME);

        verify(server).send(eq(GUEST_CONNECTION), argThat(packet -> isPingFrom(packet, OBSERVER_CONNECTION, false)));
    }

    @Test
    void aSecondPingWithinTheCooldownIsRefusedWithTheTimeLeft() {
        runAs(HOST_CONNECTION, "/ping " + GUEST_NAME);
        nowMillis += 2_500;
        runAs(HOST_CONNECTION, "/ping");

        verify(server, times(1)).send(anyInt(), any(Packet.class));
        verify(server).sendServerChat(HOST_CONNECTION, Messages.getString("PingCommand.cooldown", 8L));
    }

    @Test
    void aPingIsAllowedAgainOnceTheCooldownHasPassed() {
        runAs(HOST_CONNECTION, "/ping " + GUEST_NAME);
        nowMillis += PingCommand.COOLDOWN_MILLIS;
        runAs(HOST_CONNECTION, "/ping " + GUEST_NAME);

        verify(server, times(2)).send(eq(GUEST_CONNECTION), any(Packet.class));
    }

    @Test
    void eachSenderHasTheirOwnCooldown() {
        runAs(HOST_CONNECTION, "/ping " + GUEST_NAME);
        runAs(GUEST_CONNECTION, "/ping Davion Guards");

        verify(server).send(eq(HOST_CONNECTION), argThat(packet -> isPingFrom(packet, GUEST_CONNECTION, false)));
    }

    @Test
    void anUnknownNameSendsNothingAndDoesNotStartTheCooldown() {
        runAs(HOST_CONNECTION, "/ping Nobody Here");
        runAs(HOST_CONNECTION, "/ping " + GUEST_NAME);

        verify(server).sendServerChat(HOST_CONNECTION, Messages.getString("PingCommand.noSuchPlayer", "Nobody Here"));
        verify(server, times(1)).send(anyInt(), any(Packet.class));
    }

    @Test
    void aPlayerCannotPingThemselves() {
        runAs(GUEST_CONNECTION, "/ping " + GUEST_NAME);

        verify(server, never()).send(anyInt(), any(Packet.class));
        verify(server).sendServerChat(GUEST_CONNECTION, Messages.getString("PingCommand.self"));
    }

    @Test
    void theMenuBuildsTheSameCommandAPlayerWouldType() {
        assertEquals("/ping", PingCommand.commandText(null));
        assertEquals("/ping " + GUEST_NAME, PingCommand.commandText(new Player(GUEST_CONNECTION, GUEST_NAME)));
    }

    private Player limitPingsToHost() {
        Player host = server.getGame().getPlayer(HOST_CONNECTION);
        host.setGameMaster(true);
        host.setPingGameMasterOnly(true);
        return host;
    }

    @Test
    void whileTheGameMasterHasLimitedPingsOtherPlayersAreRefused() {
        limitPingsToHost();

        runAs(GUEST_CONNECTION, "/ping Davion Guards");
        runAs(OBSERVER_CONNECTION, "/ping");

        verify(server, never()).send(anyInt(), any(Packet.class));
        verify(server).sendServerChat(GUEST_CONNECTION,
              Messages.getString("PingCommand.gameMasterOnly", "Davion Guards"));
    }

    @Test
    void theGameMasterCanStillPingWhilePingsAreLimited() {
        limitPingsToHost();

        runAs(HOST_CONNECTION, "/ping " + GUEST_NAME);

        verify(server).send(eq(GUEST_CONNECTION), argThat(packet -> isPingFrom(packet, HOST_CONNECTION, false)));
    }

    @Test
    void aDisconnectedGameMasterNoLongerLimitsPings() {
        limitPingsToHost().setGhost(true);

        runAs(GUEST_CONNECTION, "/ping Davion Guards");

        verify(server).send(eq(HOST_CONNECTION), argThat(packet -> isPingFrom(packet, GUEST_CONNECTION, false)));
    }

    @Test
    void givingUpTheGameMasterRoleLiftsTheLimit() {
        limitPingsToHost().setGameMaster(false);

        runAs(GUEST_CONNECTION, "/ping Davion Guards");

        verify(server).send(eq(HOST_CONNECTION), argThat(packet -> isPingFrom(packet, GUEST_CONNECTION, false)));
    }
}
