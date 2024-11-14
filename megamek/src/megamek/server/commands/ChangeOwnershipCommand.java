/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.commands;

import megamek.common.Entity;
import megamek.common.Player;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Server Command "/changeOwner" that will switch an entity's owner to another player.
 *
 * @author Luana Scoppio
 */
public class ChangeOwnershipCommand extends GamemasterServerCommand {

    public ChangeOwnershipCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            "changeOwner",
            "Switches ownership of a player's entity to another player during the end phase. "
            + "Usage: /changeOwner <Unit ID> <Player ID>  "
            + "The following is an example of changing unit ID 7 to player ID 2: /changeOwner 7 2 ");
    }

    @Override
    public List<Argument<?>> defineArguments() {
        List<Argument<?>> arguments = new ArrayList<>();
        arguments.add(new IntegerArgument("unitID", 0, Integer.MAX_VALUE));
        arguments.add(new IntegerArgument("playerID", 0, Integer.MAX_VALUE));
        return arguments;
    }

    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {
        IntegerArgument unitID = (IntegerArgument) args.get("unitID");
        IntegerArgument playerID = (IntegerArgument) args.get("playerID");

        Entity ent = gameManager.getGame().getEntity(unitID.getValue());
        Player player = server.getGame().getPlayer(playerID.getValue());
        if (null == ent) {
            server.sendServerChat(connId, "No such entity.");
        } else if (null == player) {
            server.sendServerChat(connId, "No such player.");
        } else if (player.getTeam() == Player.TEAM_UNASSIGNED) {
            server.sendServerChat(connId, "Player must be assigned a team.");
        } else {
            server.sendServerChat(connId, ent.getDisplayName() + " will switch to " + player.getName() + "'s side at the end of this turn.");
            ent.setTraitorId(player.getId());
        }
    }
}
