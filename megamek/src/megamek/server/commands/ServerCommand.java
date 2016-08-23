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
 * ServerCommand.java
 *
 * Created on March 30, 2002, 6:55 PM
 */

package megamek.server.commands;

import java.util.Enumeration;

import megamek.common.IPlayer;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Ben
 * @version
 */
public abstract class ServerCommand {

    protected Server server;

    private String name;
    private String helpText;

    /** Creates new ServerCommand */
    public ServerCommand(Server server, String name, String helpText) {
        this.server = server;
        this.name = name;
        this.helpText = helpText;
    }

    /**
     * Return the string trigger for this command
     */
    public String getName() {
        return name;
    }

    /**
     * Returns some help text for this command
     */
    public String getHelp() {
        return helpText;
    }

    /**
     * Run this command with the arguments supplied
     */
    public abstract void run(int connId, String[] args);

    /**
     * Utility Function for "Restricted Commands." Restricted commands are not
     * password-protected, they are restricted to non-Observers. In the case
     * where there are only Ghosts and/or Observers, the Observers can run
     * restricted commands.
     */
    public boolean canRunRestrictedCommand(int connId) {
        if (!server.getGame().getOptions().booleanOption(
                OptionsConstants.BASE_RESTRICT_GAME_COMMANDS)) {
            return true;
        }

        if (server.getPlayer(connId).isGhost())
            return false; // Just in case something funky happens

        if (server.getPlayer(connId).isObserver()) {
            for (Enumeration<IPlayer> e = server.getGame().getPlayers(); e
                    .hasMoreElements();) {
                IPlayer p = e.nextElement();

                if (!p.isObserver() && !p.isGhost()) {
                    // There are non-Observer, non-Ghosts in the game, so
                    // Observers are locked out.
                    return false;
                }
            }
        }

        return true;
    }

}
