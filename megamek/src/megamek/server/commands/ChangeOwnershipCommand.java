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
import megamek.server.commands.arguments.*;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;

/**
 * The Server Command "/changeOwner" that will switch an entity's owner to another player.
 *
 * @author Luana Coppio
 */
public class ChangeOwnershipCommand extends GamemasterServerCommand {

    public static final String UNIT_ID = "unitID";
    public static final String PLAYER_ID = "playerID";

    public ChangeOwnershipCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            "changeOwner",
            Messages.getString("Gamemaster.cmd.changeownership.help"),
            Messages.getString("Gamemaster.cmd.changeownership.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.changeownership.unitID")),
            new PlayerArgument(PLAYER_ID, Messages.getString("Gamemaster.cmd.changeownership.playerID")));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        var unitID = (UnitArgument) args.get(UNIT_ID);
        var playerID = (PlayerArgument) args.get(PLAYER_ID);

        Entity ent = gameManager.getGame().getEntity(unitID.getValue());
        Player player = server.getGame().getPlayer(playerID.getValue());
        if (null == ent) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeownership.unitNotFound"));
        } else if (null == player) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeownership.playerNotFound"));
        } else if (player.getTeam() == Player.TEAM_UNASSIGNED) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeownership.playerUnassigned"));
        } else {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeownership.success", ent.getDisplayName(), player.getName()));
            ent.setTraitorId(player.getId());
        }
    }
}
