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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.factories.ConnectionFactory;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.packets.Packet;
import megamek.common.net.packets.InvalidPacketDataException;

/**
 * A client that speaks the wire protocol and nothing else. It gives tests full control over the handshake, which
 * {@link megamek.client.AbstractClient} performs automatically, and records every packet the server sends in order.
 * Nothing in it touches {@code Game}.
 */
final class RawPacketClient implements AutoCloseable {

    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    private final AbstractConnection connection;
    private final LinkedBlockingQueue<Packet> received = new LinkedBlockingQueue<>();
    private final CountDownLatch disconnected = new CountDownLatch(1);
    private final Thread reader;

    RawPacketClient(String host, int port) {
        connection = ConnectionFactory.getInstance().createClientConnection(host, port, 1);
        if (!connection.open()) {
            throw new IllegalStateException("Could not connect to " + host + ":" + port);
        }
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void packetReceived(PacketReceivedEvent event) {
                received.add(event.getPacket());
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                disconnected.countDown();
            }
        });
        reader = new Thread(connection::update, "RawPacketClient reader");
        reader.setDaemon(true);
        reader.start();
    }

    void send(Packet packet) {
        connection.send(packet);
    }

    /** @return the next packet from the server, or fails if none arrives within the default timeout. */
    Packet next() throws InterruptedException {
        Packet packet = received.poll(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (packet == null) {
            throw new AssertionError("No packet arrived within " + DEFAULT_TIMEOUT_SECONDS + " s");
        }
        return packet;
    }

    /** @return the next packet from the server, or null if none arrives within the given time. */
    Packet poll(long time, TimeUnit unit) throws InterruptedException {
        return received.poll(time, unit);
    }

    /**
     * Drains packets until one with the given command arrives, returning everything up to and including it.
     *
     * @throws AssertionError if the command does not arrive within the default timeout
     */
    List<Packet> drainUntil(PacketCommand command) throws InterruptedException {
        List<Packet> seen = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DEFAULT_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            Packet packet = received.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            if (packet == null) {
                break;
            }
            seen.add(packet);
            if (packet.command() == command) {
                return seen;
            }
        }
        throw new AssertionError("Never received " + command + "; saw " + commandsOf(seen));
    }

    /** Runs the whole upstream handshake and returns the player number the server assigned. */
    int handshake(String name) throws InterruptedException, InvalidPacketDataException {
        Packet versionCheck = next();
        if (versionCheck.command() != PacketCommand.SERVER_VERSION_CHECK) {
            throw new AssertionError("Expected SERVER_VERSION_CHECK first, got " + versionCheck.command());
        }
        send(new Packet(PacketCommand.CLIENT_VERSIONS, megamek.SuiteConstants.VERSION));
        Packet greeting = next();
        if (greeting.command() != PacketCommand.SERVER_GREETING) {
            throw new AssertionError("Expected SERVER_GREETING, got " + greeting.command());
        }
        send(new Packet(PacketCommand.CLIENT_NAME, name, false));
        List<Packet> untilLocalPn = drainUntil(PacketCommand.LOCAL_PN);
        return untilLocalPn.get(untilLocalPn.size() - 1).getIntValue(0);
    }

    boolean awaitDisconnect(long time, TimeUnit unit) throws InterruptedException {
        return disconnected.await(time, unit);
    }

    boolean isClosed() {
        return connection.isClosed();
    }

    static List<PacketCommand> commandsOf(List<Packet> packets) {
        List<PacketCommand> commands = new ArrayList<>(packets.size());
        for (Packet packet : packets) {
            commands.add(packet.command());
        }
        return commands;
    }

    @Override
    public void close() {
        connection.close();
        reader.interrupt();
    }
}
