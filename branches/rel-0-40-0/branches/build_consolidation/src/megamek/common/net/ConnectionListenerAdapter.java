/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.net;

/**
 * This adapter class provides default implementations for the methods declared
 * in the <code>ConnectionListener</code> interface.
 * <p>
 * Classes that wish to deal with <code>ConnectionEvent</code>s can extend
 * this class and override only the methods which they are interested in.
 * </p>
 * 
 * @see ConnectionListener
 * @see ConnectionEvent
 */
public class ConnectionListenerAdapter implements ConnectionListener {

    /**
     * Called when connection is established. The default behavior is to do
     * nothing.
     * 
     * @param e connection event
     */
    public void connected(ConnectedEvent e) {
    }

    /**
     * Called when connection is closed. The default behavior is to do nothing.
     * 
     * @param e connection event
     */
    public void disconnected(DisconnectedEvent e) {
    }

    /**
     * Called when packed received The default behavior is to do nothing.
     * 
     * @param e connection event
     */
    public void packetReceived(PacketReceivedEvent e) {
    }

}
