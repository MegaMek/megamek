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
 * KickCommand.java
 *
 * Created on April 5, 2002, 8:31 PM
 */

package megamek.server.commands;

import megamek.server.Server;

/**
 * Kicks a player off the server.
 * 
 * @author Ben
 * @version
 */
public class KickCommand extends ServerCommand {

    /** Creates new KickCommand */
    public KickCommand(Server server) {
        super(
                server,
                "kick",
                "Disconnects a player.  Usage: /kick <password> [player id number].  For a list of player id #s, use the /who command.");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        int kickArg = server.isPassworded() ? 2 : 1;

        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                    "Observers are restricted from kicking others.");
            return;
        }

        if (server.isPassworded()
                && (args.length < 3 || !server.isPassword(args[1]))) {
            server
                    .sendServerChat(connId,
                            "The password is incorrect.  Usage: /kick <password> [id#]");
        } else
            try {
                int kickedId = Integer.parseInt(args[kickArg]);

                if (kickedId == connId) {
                    server.sendServerChat("Don't be silly.");
                    return;
                }

                server.sendServerChat(server.getPlayer(connId).getName()
                        + " attempts to kick player #" + kickedId + " ("
                        + server.getPlayer(kickedId).getName() + ")...");
                server.getConnection(kickedId).close();

            } catch (ArrayIndexOutOfBoundsException ex) {
                server
                        .sendServerChat("/kick : kick failed.  Type /who for a list of players with id #s.");
            } catch (NumberFormatException ex) {
                server
                        .sendServerChat("/kick : kick failed.  Type /who for a list of players with id #s.");
            } catch (NullPointerException ex) {
                server
                        .sendServerChat("/kick : kick failed.  Type /who for a list of players with id #s.");
            }
    }

}
