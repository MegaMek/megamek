/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Packet;
import megamek.server.Connection;

/**
 * Objects that implement this interface can handle the reception of
 * <code>Packet</code>s by a <code>Connection</code>.
 *
 * @author      James Damour <suvarov454@users.sourceforge.net>
 */
public interface ConnectionHandler {

    /**
     * Process the reception of a packet from a connection.
     *
     * @param   id - the <code>int</code> ID the connection that
     *          received the packet.
     * @param   packet - the <code>Packet</code> to be processed.
     */
    public void handle( int id, Packet packet );

    /**
     * Called when a connection has terminated.
     *
     * @param   conn - the <code>Connection</code> that has terminated.
     */ 
    public void disconnected( Connection conn );

}