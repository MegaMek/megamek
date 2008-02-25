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
 * interface for connections between client and server refactored from the
 * original Connection class which was moved to AbstractConnection
 */
public interface IConnection {
    /**
     * Opens the connection
     * 
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public boolean open();

    /**
     * Closes the socket and releases resources
     */
    public void close();

    /**
     * Returns the connection ID
     * 
     * @return the connection ID
     */
    public int getId();

    /**
     * Sets the connection ID
     * 
     * @param id new connection ID Be careful with this... used by server only.
     *            cannot be moved to constructor,so should be moved to a
     *            separate interface
     */
    public void setId(int id);

    /**
     * @return what?
     */
    public String getInetAddress();

    /**
     * call this to update the connection state. should be called a few times
     * per second normally. This method should not block for too long. Instead
     * it should return 0 and hope to be called asap again.
     * 
     * @return the amount of milliseconds it is approximately ok not to call
     *         this method. Must return >=0
     */
    public long update();

    /**
     * sibling of the update() method. will not read anything, will just flush
     * the pending packets from the queue
     */
    public void flush();

    /**
     * Adds a packet to the send queue to be send on a seperate thread.
     */
    public void send(Packet packet);

    /**
     * Returns <code>true</code> if there are (send)pending packets
     * 
     * @return <code>true</code> if there are pending packets
     */
    public boolean hasPending();

    /**
     * Returns a very approximate count of how many bytes were sent
     * 
     * @return a very approximate count of how many bytes were sent
     */
    public long bytesSent();

    /**
     * Returns a very approximate count of how many bytes were received
     * 
     * @return a very approximate count of how many bytes were received
     */
    public long bytesReceived();

    /**
     * Adds the specified connection listener to receive connection events from
     * connection.
     * 
     * @param listener the connection listener.
     */
    public void addConnectionListener(ConnectionListener listener);

    /**
     * Removes the specified connection listener.
     * 
     * @param listener the connection listener.
     */
    public void removeConnectionListener(ConnectionListener listener);
}
