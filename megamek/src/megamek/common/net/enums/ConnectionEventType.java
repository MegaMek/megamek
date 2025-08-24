/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.net.enums;

import megamek.common.net.events.AbstractConnectionEvent;
import megamek.common.net.events.ConnectedEvent;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.listeners.ConnectionListener;
import megamek.logging.MMLogger;

public enum ConnectionEventType {
    // region Enum Declarations
    CONNECTED,
    PACKET_RECEIVED,
    DISCONNECTED;
    // endregion Enum Declarations

    public void processListener(final AbstractConnectionEvent event,
          final ConnectionListener listener) {
        switch (this) {
            case CONNECTED:
                listener.connected((ConnectedEvent) event);
                break;
            case PACKET_RECEIVED:
                listener.packetReceived((PacketReceivedEvent) event);
                break;
            case DISCONNECTED:
                listener.disconnected((DisconnectedEvent) event);
                break;
            default:
                MMLogger.create(ConnectionEventType.class)
                      .error("Received unknown connection event type of {}", event.getType().name());
                break;
        }
    }
}
