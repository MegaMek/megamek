/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * WhoCommand.java
 *
 * Created on March 30, 2002, 7:35 PM
 */

package megamek.server.commands;

import java.util.*;

import megamek.server.*;

/**
 * Lists all the players connected and some info about them.
 * @author  Ben
 * @version 
 */
public class WhoCommand extends ServerCommand {

    /** Creates new WhoCommand */
    public WhoCommand(Server server) {
        super(server, "who", "Lists all of the players connected to the server.");
    }
    
    public void run(int connId, String[] args) {
        server.sendServerChat(connId, "Listing all connections...");
        server.sendServerChat(connId, "[id#] : [name], [address], [pending]");
        for (Enumeration i = server.getConnections(); i.hasMoreElements();) {
            Connection conn = (Connection)i.nextElement();
            server.sendServerChat(connId, conn.getId() + " : " + server.getPlayer(conn.getId()).getName() + ", " + conn.getSocket().getInetAddress() + ", " + conn.hasPending());
        }
        server.sendServerChat(connId, "end list");
    }    
    
    
}
