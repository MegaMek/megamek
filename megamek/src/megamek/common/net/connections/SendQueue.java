/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.net.connections;

import java.util.LinkedList;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Wrapper around a LinkedList for keeping a queue of packets to send. Note that this implementation is not
 * synchronized.
 */
class SendQueue {
    private static final MMLogger logger = MMLogger.create(SendQueue.class);

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
        if (!queue.isEmpty()) {
            final String sb = queue.stream()
                  .map(p -> '\n' + p.getCommand().toString()).collect(
                        Collectors.joining("", "Contents of Send Queue:\n", ""));
            logger.warn(sb);
        }
    }
}
