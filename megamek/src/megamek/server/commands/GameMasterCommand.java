/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.enums.GamePhase;
import megamek.server.gameManager.GameManager;
import megamek.server.Server;

/**
 * This command starts a vote to allow player to assume the elevated Game Master role
 *
 * @author arlith
 */
public class GameMasterCommand extends ServerCommand {

    public static String SERVER_VOTE_PROMPT_MSG = "All players with an assigned team "
            + "must allow this change.  Use /allowGM "
            + "to allow this change.";

    public GameMasterCommand(Server server) {
        super(server, "gm", "Starts a vote to gives a player game master authority "
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

        if (args.length != 1) {
            server.sendServerChat(connId, "Incorrect number of arguments "
                    + "for gm command!  Expected 0, received, "
                    + (args.length - 1) + ".");
            server.sendServerChat(connId, getHelp());
            return;
        }

        GameManager gameManager = (GameManager) server.getGameManager();
        if (player.getGameMaster()) {
            // toggling off game master requires no vote
            gameManager.setGameMaster(player, false);
        } else if (gameManager.getGame() != null && gameManager.getGame().getPhase().isLounge() ) {
            // becoming GameMaster in Lobby is always permitted
            server.sendServerChat(player.getName() + " will become Game Master without vote.");
            gameManager.setGameMaster(player, true);
        } else {
            // becoming GameMaster in regular gameplay requires unanimous human player voting
            for (Player p : server.getGame().getPlayersVector()) {
                if (p.getId() != player.getId()) {
                    server.sendServerChat(p.getId(), player.getName() + " wants to become a Game Master" + SERVER_VOTE_PROMPT_MSG);
                }
            }

            server.requestGameMaster(player);

            for (Player p : server.getGame().getPlayersVector()) {
                p.setVotedToAllowGameMaster(false);
            }

            // requester automatically votes yes
            AllowGameMasterCommand.voteYes(server, player);
        }
    }

}
