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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import megamek.MMConstants;
import megamek.client.HeadlessClient;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;

/**
 * One in-process {@link Server} with N real {@link HeadlessClient}s connected to it over loopback sockets. This is
 * the shared harness the networking fork builds its integration and fault-injection tests on: every client runs the
 * real receive path (socket, marshaller, EDT dispatch into its own {@link Game}), so assertions can compare what each
 * client believes against the server and against each other.
 *
 * <p>Use it with try-with-resources; {@link #close()} shuts the clients down before the server.</p>
 */
final class InProcessHarness implements AutoCloseable {

    static final long DEFAULT_TIMEOUT_SECONDS = 15;

    private final TWGameManager gameManager;
    private final Server server;
    private final List<HeadlessClient> clients = new ArrayList<>();

    InProcessHarness() throws IOException {
        this(new TWGameManager());
    }

    InProcessHarness(TWGameManager gameManager) throws IOException {
        this.gameManager = gameManager;
        if (gameManager.getGame() == null) {
            gameManager.setGame(new Game());
        }
        gameManager.getGame().setPhase(GamePhase.LOUNGE);
        server = ServerFactory.createServer(gameManager);
    }

    Server server() {
        return server;
    }

    TWGameManager gameManager() {
        return gameManager;
    }

    Game serverGame() {
        return (Game) server.getGame();
    }

    List<HeadlessClient> clients() {
        return Collections.unmodifiableList(clients);
    }

    /**
     * Connects a new headless client under the given name and waits until the server has assigned it a player number
     * and the client has received the lobby state.
     */
    HeadlessClient connect(String name) throws InterruptedException {
        HeadlessClient client = new HeadlessClient(name, MMConstants.LOCALHOST_IP, server.getPort());
        if (!client.connect()) {
            throw new IllegalStateException("Client " + name + " could not open a socket to the server");
        }
        clients.add(client);
        await(() -> client.isConnected() && client.getLocalPlayer() != null,
              "client " + name + " to finish the handshake");
        return client;
    }

    /** Waits until every client's player list has the same ids and names as the server's. */
    void awaitPlayerListsInSync() throws InterruptedException {
        await(() -> clients.stream().allMatch(client -> sameIdsAndNames(serverGame(), client.getGame())),
              "all clients to see the server's player list");
    }

    /** Blocks until the condition holds or the default timeout passes. */
    static void await(BooleanSupplier condition, String what) throws InterruptedException {
        await(condition, what, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    static void await(BooleanSupplier condition, String what, long time, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(time);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for " + what);
            }
            Thread.sleep(20);
        }
    }

    /** @return how many server-side per-connection threads ("Connection 0", "Connection 1", ...) are still alive */
    static long liveConnectionThreads() {
        return Thread.getAllStackTraces().keySet().stream()
              .filter(Thread::isAlive)
              .map(Thread::getName)
              .filter(name -> name.matches("Connection \\d+"))
              .count();
    }

    static boolean sameIdsAndNames(Game expected, Game actual) {
        List<Player> expectedPlayers = new ArrayList<>(expected.getPlayersList());
        List<Player> actualPlayers = new ArrayList<>(actual.getPlayersList());
        if (expectedPlayers.size() != actualPlayers.size()) {
            return false;
        }
        for (Player player : expectedPlayers) {
            Player counterpart = actual.getPlayer(player.getId());
            if ((counterpart == null) || !counterpart.getName().equals(player.getName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        for (HeadlessClient client : clients) {
            try {
                client.die();
            } catch (Exception ignored) {
                // Best effort: the server shutdown below closes the sockets anyway
            }
        }
        clients.clear();
        // Let the server finish processing every disconnect before it dies. Server.die() does not wait for its
        // connection threads, and AbstractGameManager.send() routes through the static Server.getServerInstance(),
        // so disconnect work still running after the next Server is constructed - PLAYER_REMOVE, chat, the reset of
        // an emptied lobby - reaches the wrong server's clients (see ServerInstanceLeakTest). Each disconnect runs
        // to completion on its own "Connection N" thread, so no such thread alive means the server is quiescent.
        try {
            await(() -> liveConnectionThreads() == 0, "the server's connection threads to finish", 10,
                  TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (AssertionError stillBusy) {
            // Fall through and die anyway; the next test may see the leak described above
        }
        server.die();
    }
}
