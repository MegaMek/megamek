/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014 Nicholas Walczak (walczak@cs.umn.edu)
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

import megamek.common.Player;
import megamek.server.Server;

/**
 * This command allows a player to become a Gamemaster,
 *
 * @author arlith
 */
public class GameMasterCommand extends ServerCommand {

    public static String SERVER_VOTE_PROMPT_MSG = "All players with an assigned team "
            + "must allow this change.  Use /allowGM "
            + "to allow this change.";

    public GameMasterCommand(Server server) {
        super(server, "gm", "Gives a player game master authority "
                + "Usage: /gm ");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        Player player = server.getPlayer(connId);
        int numEntities = server.getGame().getEntitiesOwnedBy(player);

        if (args.length != 1) {
            server.sendServerChat(connId, "Incorrect number of arguments "
                    + "for gm command!  Expected 0, received, "
                    + (args.length - 1) + ".");
            server.sendServerChat(connId, getHelp());
            return;
        }

        //            int teamId = Integer.parseInt(args[1]);
        //
        //            if ((Player.TEAM_UNASSIGNED == teamId) && (numEntities != 0)) {
        //                server.sendServerChat(connId, "Player must have no more " +
        //                        "units to join the unassigned team!");
        //                return;
        //            }
        //            String teamString = "join Team " + teamId + ".  ";
        //            if (teamId == Player.TEAM_UNASSIGNED) {
        //                teamString = " leave their team and go unassigned.  ";
        //            } else if (teamId == Player.TEAM_NONE) {
        //                teamString = " go lone wolf!  ";
        //            }

        // TODO allow GM to be demoted to regular player without a vote

        for (Player p : server.getGame().getPlayersVector()) {
            if (p.getId() != player.getId()) {
                server.sendServerChat(p.getId(), player.getName()
                        + " wants to become a Game Master"
                        + SERVER_VOTE_PROMPT_MSG);
            }
        }

        server.requestGameMaster(player);

        for (Player p : server.getGame().getPlayersVector()) {
            p.setAllowGameMaster(false);
        }
        player.setAllowGameMaster(true);
    }

}
