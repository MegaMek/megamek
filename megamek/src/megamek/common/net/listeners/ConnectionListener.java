/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
package megamek.common.net.listeners;

import megamek.common.net.events.AbstractConnectionEvent;
import megamek.common.net.events.ConnectedEvent;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;

import java.util.EventListener;

/**
 * Classes that wish to deal with <code>ConnectionEvent</code>s can extend this class and override
 * only the methods which they are interested in.
 *
 * Classes which extend this class provide methods that deal with the events that are generated when
 * something happened in the connection.
 *
 * After creating an instance of this class or one of its child, it can then be added to a
 * Connection using the <code>addConnectionListener</code> method and removed using the
 * <code>removeConnectionListener</code> method. When the Connection is changed the appropriate
 * method will be invoked.
 *
 * @see EventListener
 * @see AbstractConnectionEvent
 */
public class ConnectionListener implements EventListener {
    /**
     * Called when connection is established. The default behavior is to do nothing.
     * @param evt connection event
     */
    public void connected(ConnectedEvent evt) {

    }

    /**
     * Called when connection is closed. The default behavior is to do nothing.
     * @param evt connection event
     */
    public void disconnected(DisconnectedEvent evt) {

    }

    /**
     * Called when packed is received. The default behavior is to do nothing.
     * @param evt connection event
     */
    public void packetReceived(PacketReceivedEvent evt) {

    }
}
