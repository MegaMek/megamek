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
 * Classes which implement this interface provide methods that deal with the
 * events that are generated when the something happened in the connection.
 * <p>
 * After creating an instance of a class that implements this interface it can
 * be added to a Connection using the <code>addConnectionListener</code>
 * method and removed using the <code>removeConnectionListener</code> method.
 * When the Connection is changed the appropriate method will be invoked.
 * </p>
 * 
 * @see ConnectionListenerAdapter
 * @see ConnectionEvent
 */
public interface ConnectionListener extends java.util.EventListener {
    /**
     * Called when connection is established
     * 
     * @param e connection event
     */
    public void connected(ConnectedEvent e);

    /**
     * Called when connection is closed
     * 
     * @param e connection event
     */
    public void disconnected(DisconnectedEvent e);

    /**
     * Called when packed received
     * 
     * @param e connection event
     */
    public void packetReceived(PacketReceivedEvent e);
}
