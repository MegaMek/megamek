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
package megamek.server.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import megamek.client.HeadlessClient;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.server.Server;
import org.junit.jupiter.api.Test;

/**
 * Characterizes a lifecycle defect: {@link megamek.server.AbstractGameManager#send} transmits through the static
 * {@link Server#getServerInstance()}, which is whichever {@code Server} was constructed last, not the server that owns
 * the game manager. When two servers exist in one JVM - a game being re-hosted while the previous one is still
 * winding down is the realistic case - a disconnect processed by the older server is broadcast to the newer server's
 * clients. Because both servers hand out player ids from 0, the newer server's players get told to remove
 * themselves.
 *
 * <p>This test pins the behaviour as it is today so that the lifecycle-hardening fix flips it deliberately. It is
 * deterministic: the second server is fully up before the first server's client leaves.</p>
 */
class ServerInstanceLeakTest {

    @Test
    void aDisconnectOnAnOlderServerIsBroadcastThroughTheNewestServer() throws Exception {
        try (InProcessHarness older = new InProcessHarness()) {
            HeadlessClient alice = older.connect("Alice");
            assertEquals(0, alice.getLocalPlayerNumber(), "first player on the first server is #0");

            try (InProcessHarness newer = new InProcessHarness()) {
                HeadlessClient carol = newer.connect("Carol");
                assertEquals(0, carol.getLocalPlayerNumber(), "first player on the second server is also #0");
                assertSame(newer.server(), Server.getServerInstance(), "the newest server is the static instance");
                CopyOnWriteArrayList<String> carolHeard = new CopyOnWriteArrayList<>();
                carol.getGame().addGameListener(new GameListenerAdapter() {
                    @Override
                    public void gamePlayerChat(GamePlayerChatEvent event) {
                        carolHeard.add(event.getMessage());
                    }
                });

                // Alice leaves the OLDER server. Its game manager handles the disconnect and sends PLAYER_REMOVE 0
                // and "Alice disconnected." - through Server.getServerInstance(), i.e. the NEWER server.
                alice.die();

                InProcessHarness.await(() -> older.serverGame().getNoOfPlayers() == 0,
                      "the older server to process Alice's disconnect");
                InProcessHarness.await(() -> carolHeard.stream().anyMatch(line -> line.contains("Alice disconnected")),
                      "Carol to receive chat about a player who was never on her server", 10, TimeUnit.SECONDS);
                InProcessHarness.await(() -> carol.getGame().getPlayer(0) == null,
                      "Carol's client to remove player 0 - herself", 10, TimeUnit.SECONDS);

                // The newer server itself still has Carol; only her client's copy of the game is now wrong.
                assertNotNull(newer.serverGame().getPlayer(0), "the newer server never lost Carol");
                assertNull(carol.getLocalPlayer(), "but Carol's client no longer knows who it is");
                assertTrue(carol.isConnected(), "and nothing told it anything went wrong");
            }
        }
    }
}
