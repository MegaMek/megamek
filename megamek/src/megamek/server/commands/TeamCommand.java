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

import megamek.common.net.Connection;
import megamek.server.*;

/**
 * Team Chat
 * @author  Torren
 * @version 
 */
public class TeamCommand extends ServerCommand {

    /** Creates new WhoCommand */
    public TeamCommand(Server server) {
        super(server, "team", "Allows players on the same team to chat with eachother in the game.");
    }
    
    public void run(int connId, String[] args) {
        
        if (args.length > 1) {
            
            int team = server.getPlayer(connId).getTeam();

            if ( team < 1 || team > 8) {
                server.sendServerChat(connId, "You are not on a team!");
            }
                
            StringBuilder message = new StringBuilder("Team Chat[");

            message.append(server.getPlayer(connId).getName());
            message.append("]");
            
            for ( int pos = 1; pos < args.length;pos++) {
                message.append(" ");
                message.append(args[pos]);
            }
            
            for (Enumeration i = server.getConnections(); i.hasMoreElements();) {
                Connection conn = (Connection)i.nextElement();
                
                if ( server.getPlayer(conn.getId()).getTeam() == team )
                    server.sendServerChat(conn.getId(), message.toString());
            }
        }
    }    
    
    
}
