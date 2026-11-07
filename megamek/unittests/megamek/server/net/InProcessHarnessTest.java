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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import megamek.client.HeadlessClient;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.player.GamePlayerChangeEvent;
import megamek.common.event.player.GamePlayerChatEvent;
import org.junit.jupiter.api.Test;

/**
 * The first test on the shared in-process harness: a real server and two real clients agree on who is in the lobby,
 * and a chat line sent by one arrives at the other through the full send and receive path. Later work (instrumentation
 * counts, reconnect, fault injection) extends {@link InProcessHarness} rather than starting from scratch.
 */
class InProcessHarnessTest {

    @Test
    void twoClientsSeeTheSameLobby() throws Exception {
        try (InProcessHarness harness = new InProcessHarness()) {
            HeadlessClient alice = harness.connect("Alice");
            HeadlessClient bob = harness.connect("Bob");

            harness.awaitPlayerListsInSync();

            assertEquals(2, harness.serverGame().getPlayersList().size());
            assertNotEquals(alice.getLocalPlayerNumber(), bob.getLocalPlayerNumber());
            assertEquals("Alice", alice.getLocalPlayer().getName());
            assertEquals("Bob", bob.getLocalPlayer().getName());
            // Each client sees the other under the id the server assigned
            assertNotNull(alice.getGame().getPlayer(bob.getLocalPlayerNumber()));
            assertEquals("Bob", alice.getGame().getPlayer(bob.getLocalPlayerNumber()).getName());
            assertEquals("Alice", bob.getGame().getPlayer(alice.getLocalPlayerNumber()).getName());
        }
    }

    @Test
    void chatFromOneClientReachesTheOther() throws Exception {
        try (InProcessHarness harness = new InProcessHarness()) {
            HeadlessClient alice = harness.connect("Alice");
            HeadlessClient bob = harness.connect("Bob");
            harness.awaitPlayerListsInSync();
            CopyOnWriteArrayList<String> bobHeard = new CopyOnWriteArrayList<>();
            bob.getGame().addGameListener(new GameListenerAdapter() {
                @Override
                public void gamePlayerChat(GamePlayerChatEvent event) {
                    bobHeard.add(event.getMessage());
                }
            });

            alice.sendChat("ready when you are");

            InProcessHarness.await(() -> bobHeard.stream().anyMatch(line -> line.contains("ready when you are")),
                  "Bob to receive Alice's chat", 10, TimeUnit.SECONDS);
            assertTrue(bobHeard.stream().anyMatch(line -> line.startsWith("Alice")),
                  "the server prefixes chat with the sender's name");
        }
    }

    // Once failed with Alice losing her own player: the previous test's server, still processing its own
    // disconnects, broadcast PLAYER_REMOVE through Server.getServerInstance(), which by then was this test's server.
    // InProcessHarness.close() now waits for the server to go quiet; ServerInstanceLeakTest pins the leak itself.
    @Test
    void aClientThatLeavesIsRemovedFromEveryoneElsesLobby() throws Exception {
        try (InProcessHarness harness = new InProcessHarness()) {
            HeadlessClient alice = harness.connect("Alice");
            HeadlessClient bob = harness.connect("Bob");
            harness.awaitPlayerListsInSync();
            int alicesNumber = alice.getLocalPlayerNumber();
            int bobsNumber = bob.getLocalPlayerNumber();
            CopyOnWriteArrayList<String> aliceSaw = new CopyOnWriteArrayList<>();
            alice.getGame().addGameListener(new GameListenerAdapter() {
                @Override
                public void gamePlayerChange(GamePlayerChangeEvent event) {
                    aliceSaw.add("playerChange " + (event.getPlayer() == null ? "null" : event.getPlayer().getName()
                          + "#" + event.getPlayer().getId()) + " -> " + describe(alice));
                }

                @Override
                public void gamePhaseChange(GamePhaseChangeEvent event) {
                    aliceSaw.add("phase " + event.getOldPhase() + " -> " + event.getNewPhase());
                }
            });

            bob.die();

            InProcessHarness.await(() -> harness.serverGame().getPlayer(bobsNumber) == null,
                  "the server to drop Bob");
            InProcessHarness.await(() -> alice.getGame().getPlayer(bobsNumber) == null,
                  "Alice to be told Bob left");
            String diagnostics = "Alice connected=" + alice.isConnected() + " localPn=" + alicesNumber
                  + " sees " + describe(alice) + "; server has " + names(harness.serverGame())
                  + "; events: " + aliceSaw;
            assertNotNull(alice.getGame().getPlayer(alicesNumber), "Alice must still see herself. " + diagnostics);
            assertEquals(1, alice.getGame().getPlayersList().size(), diagnostics);
        }
    }

    private static String describe(HeadlessClient client) {
        return names(client.getGame()) + " phase=" + client.getGame().getPhase();
    }

    private static String names(megamek.common.game.Game game) {
        return game.getPlayersList().stream().map(p -> p.getName() + "#" + p.getId()).toList().toString();
    }
}
