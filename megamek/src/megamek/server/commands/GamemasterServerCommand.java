/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.commands;

import megamek.server.Server;
import megamek.server.commands.arguments.Arguments;
import megamek.server.totalwarfare.TWGameManager;

import static megamek.server.Server.SERVER_CONN;

/**
 * A ServerCommand that can only be used by Game Masters,
 * This abstract class implements many features that are common to all Game Master commands,
 * like the isGM check for users, it also uses the Argument class for building the command arguments
 * and to abstract the parsing of the arguments, limit assertion and error handling, and for building
 * a more dynamic "help" feature.
 * It also has a more advanced parser and argument handling than the ServerCommand class, which allows for
 * named arguments, positional arguments, optional arguments and default values.
 * named arguments can be passed in any order, and positional arguments are parsed in order and MUST appear before named
 * arguments.
 *
 * @author Luana Coppio
 */
public abstract class GamemasterServerCommand extends ClientServerCommand {

    /**
     * Creates new ServerCommand that can only be used by Game Masters
     *
     * @param server        instance of the server
     * @param gameManager   instance of the game manager
     * @param name          the name of the command
     * @param helpText      the help text for the command
     */
    public GamemasterServerCommand(Server server, TWGameManager gameManager, String name, String helpText, String longName) {
        super(server, gameManager, name, helpText, longName);
    }

    @Override
    protected boolean preRun(int connId) {
        // Override to add pre-run checks
        var userIsGm = connId == SERVER_CONN || isGM(connId);

        if (!userIsGm) {
            server.sendServerChat(connId, "This command is restricted to GMs.");
            return false;
        }

        return true;
    }

}
