/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Player;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

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

        TWGameManager gameManager = (TWGameManager) server.getGameManager();
        if (player.getGameMaster()) {
            // toggling off game master requires no vote
            gameManager.setGameMaster(player, false);
        } else if (gameManager.getGame() != null && gameManager.getGame().getPhase().isLounge()) {
            // becoming GameMaster in Lobby is always permitted
            server.sendServerChat(player.getName() + " will become Game Master without vote.");
            gameManager.setGameMaster(player, true);
        } else {
            // becoming GameMaster in regular gameplay requires unanimous human player
            // voting
            for (Player p : server.getGame().getPlayersList()) {
                if (p.getId() != player.getId()) {
                    server.sendServerChat(p.getId(),
                          player.getName() + " wants to become a Game Master" + SERVER_VOTE_PROMPT_MSG);
                }
            }

            server.requestGameMaster(player);

            for (Player p : server.getGame().getPlayersList()) {
                p.setVotedToAllowGameMaster(false);
            }

            // requester automatically votes yes
            AllowGameMasterCommand.voteYes(server, player);
        }
    }

}
