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
package megamek.common.net.enums;

import megamek.common.net.ConnectionListener;
import megamek.common.net.events.ConnectedEvent;
import megamek.common.net.events.AbstractConnectionEvent;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import org.apache.logging.log4j.LogManager;

public enum ConnectionEventType {
    //region Enum Declarations
    CONNECTED,
    PACKET_RECEIVED,
    DISCONNECTED;
    //endregion Enum Declarations

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
                LogManager.getLogger().error("Received unknown connection event type of " + event.getType().name());
                break;
        }
    }
}
