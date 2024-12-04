/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.Server;
import megamek.server.commands.arguments.*;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;

/**
 * Kicks a player off the server.
 *
 * @author Ben
 * @author Luana Coppio
 * @since April 5, 2002, 8:31 PM
 */
public class KickCommand extends ClientServerCommand {

    /** Creates a new KickCommand */
    public KickCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "kick",
                "Disconnects a player. Usage: /kick <password> [player id number]. For a list of player id #s, use the /who command.", "kick");
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IntegerArgument("playerID", "The player ID to kick."),
            new OptionalPasswordArgument("password", "The server password.")
        );
    }

    @Override
    protected boolean preRun(int connId) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId, "Observers are restricted from kicking others.");
            return false;
        }
        return true;
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        var kickedId = ((IntegerArgument) args.get("playerId")).getValue();
        var passwordOpt = (OptionalPasswordArgument) args.get("password");

        if (serverPasswordCheckFailed(connId, passwordOpt)) {
            // The password failed
            return;
        }

        try {
            if (kickedId == connId) {
                server.sendServerChat("Don't be silly.");
                return;
            }

            server.sendServerChat(server.getPlayer(connId).getName()
                + " attempts to kick player #" + kickedId + " ("
                + server.getPlayer(kickedId).getName() + ")...");

            server.send(kickedId, new Packet(PacketCommand.CLOSE_CONNECTION));

            server.getConnection(kickedId).close();
        } catch (Exception ex) {
            server.sendServerChat("/kick : kick failed. Type /who for a list of players with id #s.");
        }

    }

    /**
     * Checks the password argument given by the player, if the server is passworded and the check fails it returns
     * true
     *
     * @param connId The connection ID of the player issuing the command
     * @param passwordOptArg The password argument
     * @return Returns true if the password fails
     */
    private boolean serverPasswordCheckFailed(int connId, OptionalPasswordArgument passwordOptArg) {
        var passwordOpt = passwordOptArg.getValue();

        if (server.isPassworded()) {
            if (passwordOpt.isEmpty()) {
                server.sendServerChat(connId, "The password is missing. Usage: /kick <password> [id#]");
                return true;
            }
            if (!server.isPassword(passwordOpt.get())) {
                server.sendServerChat(connId, "The password is incorrect. Usage: /kick <password> [id#]");
                return true;
            }
        }

        return false;
    }
}
