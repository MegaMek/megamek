/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.IPlayer;
import megamek.server.Server;

/**
 * The Server Command "/traitor" that will switch an entity's owner to another player.
 * 
 * @author Jay Lawson (Taharqa)
 */
public class TraitorCommand extends ServerCommand {

    public TraitorCommand(Server server) {
        super(server,
                "traitor",
                "Switches a player's entity to another player during the end phase. "
                        + "Usage: /traitor # # where the first number is the entity id and "
                        + "the second is the new player id.");
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            int eid = Integer.parseInt(args[1]);
            Entity ent = server.getGame().getEntity(eid);
            int pid = Integer.parseInt(args[2]);
            IPlayer player = server.getGame().getPlayer(pid);
            if (null == ent) {
                server.sendServerChat(connId, "No such entity.");
            } else if (ent.getOwner().getId() != connId) {
                server.sendServerChat(connId, "You must own an entity to make it switch sides.");
            } else if (null == player) {
                server.sendServerChat(connId, "No such player.");
            } else if (player.getTeam() == IPlayer.TEAM_UNASSIGNED) {
                server.sendServerChat(connId, "Player must be assigned a team.");
            } else if (pid == connId) {
                server.sendServerChat(connId, "You can't switch to the same side.");
            } else {
                server.sendServerChat(connId, ent.getDisplayName() + " will switch to " + player.getName() + "'s side at the end of this turn.");
                ent.setTraitorId(pid);
            }
        } catch (NumberFormatException ignored) {
        }
    }

}
