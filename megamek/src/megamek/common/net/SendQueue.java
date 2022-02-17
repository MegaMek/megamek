/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.net;

import megamek.common.net.packets.SendPacket;

import java.util.LinkedList;

/**
 * This is a wrapper around <code>LinkedList</code> for keeping a queue of packets to send.
 * <br>
 * Note that this implementation is not synchronized.
 */
public class SendQueue {
    private LinkedList<SendPacket> queue = new LinkedList<>();
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
    public SendPacket getPacket() {
        return finished ? null : queue.poll();
    }

    /**
     * @return true if this connection has pending data
     */
    public boolean hasPending() {
        return !queue.isEmpty();
    }

    public void reportContents() {
        System.err.print("Contents of Send Queue: ");
        for (SendPacket p : queue) {
            System.err.print(p.getCommand());
        }
        System.err.println();
    }
}
