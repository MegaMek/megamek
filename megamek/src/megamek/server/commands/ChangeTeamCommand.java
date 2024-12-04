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

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;

/**
 * The Server Command "/changeOwner" that will switch an entity's owner to another player.
 *
 * @author Luana Coppio
 */
public class ChangeTeamCommand extends GamemasterServerCommand {

    public static final String PLAYER_ID = "playerID";
    public static final String TEAM_ID = "teamID";

    public ChangeTeamCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            "changeTeam",
            Messages.getString("Gamemaster.cmd.changeteam.help"),
            Messages.getString("Gamemaster.cmd.changeteam.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IntegerArgument(PLAYER_ID, Messages.getString("Gamemaster.cmd.changeteam.playerID")),
            new IntegerArgument(TEAM_ID, Messages.getString("Gamemaster.cmd.changeteam.teamID")));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        int teamID = ((IntegerArgument) args.get(TEAM_ID)).getValue();
        int playerID = ((IntegerArgument) args.get(PLAYER_ID)).getValue();

        Player player = server.getGame().getPlayer(playerID);
        if (null == player) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeteam.playerNotFound"));
            return;
        }

        int numEntities = server.getGame().getEntitiesOwnedBy(player);
        if ((Player.TEAM_UNASSIGNED == teamID) && (numEntities != 0)) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeteam.playerCantJoinUnassigned"));
            return;
        }

        server.requestTeamChangeForPlayer(teamID, player);
        gameManager.allowTeamChange();

        server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeteam.success", player.getName(), teamID));
    }
}
