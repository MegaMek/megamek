/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server.commands;

import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.PlayerArgument;
import megamek.server.commands.arguments.TeamArgument;
import megamek.server.totalwarfare.TWGameManager;

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
              new PlayerArgument(PLAYER_ID, Messages.getString("Gamemaster.cmd.changeteam.playerID")),
              new TeamArgument(TEAM_ID, Messages.getString("Gamemaster.cmd.changeteam.teamID")));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        int playerID = ((PlayerArgument) args.get(PLAYER_ID)).getValue();
        int teamID = ((TeamArgument) args.get(TEAM_ID)).getValue();

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

        server.sendServerChat(connId,
              Messages.getString("Gamemaster.cmd.changeteam.success", player.getName(), teamID));
    }
}
