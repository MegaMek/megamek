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
import megamek.server.totalwarfare.TWGameManager;

/**
 * The Server Command "/changeOwner" that will switch an entity's owner to another player.
 *
 * @author Luana Scoppio
 */
public class ChangeOwnershipCommand extends ServerCommand implements IsGM {

    private final TWGameManager gameManager;

    public ChangeOwnershipCommand(Server server, TWGameManager gameManager) {
        super(server,
            "changeOwner",
            "Switches ownership of a player's entity to another player during the end phase. "
                + "Usage: /changeOwner <Unit ID> <Player ID>  "
                + "The following is an example of changing unit ID 7 to player ID 2: /changeOwner 7 2 ");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see ServerCommand#run(int, String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            if (!isGM(connId)) {
                server.sendServerChat(connId, "You are not a Game Master.");
                return;
            }

            int eid = Integer.parseInt(args[1]);
            Entity ent = gameManager.getGame().getEntity(eid);
            int pid = Integer.parseInt(args[2]);
            Player player = server.getGame().getPlayer(pid);
            if (null == ent) {
                server.sendServerChat(connId, "No such entity.");
            } else if (null == player) {
                server.sendServerChat(connId, "No such player.");
            } else if (player.getTeam() == Player.TEAM_UNASSIGNED) {
                server.sendServerChat(connId, "Player must be assigned a team.");
            } else {
                server.sendServerChat(connId, ent.getDisplayName() + " will switch to " + player.getName() + "'s side at the end of this turn.");
                ent.setTraitorId(pid);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public TWGameManager getGameManager() {
        return gameManager;
    }
}
