/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.net.connections;

import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Wrapper around a LinkedList for keeping a queue of packets
 * to send. Note that this implementation is not synchronized.
 */
class SendQueue {
    private final LinkedList<SendPacket> queue = new LinkedList<>();
    private boolean finished = false;

    public void addPacket(SendPacket packet) {
        queue.add(packet);
    }

    public void finish() {
        queue.clear();
        finished = true;
    }

    /**
     * Waits for a packet to appear in the queue and then returns it.
     *
     * @return the first available packet in the queue or null if none
     */
    public @Nullable SendPacket getPacket() {
        return finished ? null : queue.poll();
    }

    /** @return True if this connection has pending data. */
    public boolean hasPending() {
        return !queue.isEmpty();
    }

    public void reportContents() {
        final String sb = queue.stream()
                .map(p -> '\n' + p.getCommand().toString()).collect(
                        Collectors.joining("", "Contents of Send Queue:\n", ""));
        LogManager.getLogger().warn(sb);
    }
}
