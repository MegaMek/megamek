/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * This command starts a vote to allow a player to assume the elevated Game Master role, or gives the role up when
 * the player already holds it. The vote runs the same way in the lobby and in play; the game options decide whether
 * it needs to be unanimous or a majority.
 *
 * @author arlith
 */
public class GameMasterCommand extends ServerCommand {

    public GameMasterCommand(Server server) {
        super(server, "gm", Messages.getString("Gamemaster.vote.help.request"));
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
            server.sendServerChat(connId, Messages.getString("Gamemaster.vote.noArguments"));
            server.sendServerChat(connId, getHelp());
            return;
        }

        TWGameManager gameManager = (TWGameManager) server.getGameManager();
        Player currentGameMaster = gameManager.getGameMaster();
        if (player.getGameMaster()) {
            // giving up the role is always allowed, even in a game that no longer allows the role at all
            gameManager.setGameMaster(player, false);
        } else if (!server.getGame().getOptions().booleanOption(OptionsConstants.GAME_MASTER_ALLOW)) {
            // whether a game has a gamemaster is a rule of the game, so it is the game's option that decides,
            // not each player's client
            server.sendServerChat(connId, Messages.getString("Gamemaster.vote.notAllowed"));
        } else if (currentGameMaster != null) {
            // only one Game Master is allowed at a time
            server.sendServerChat(connId,
                  Messages.getString("Gamemaster.vote.roleHeld", currentGameMaster.getName()));
        } else {
            gameManager.startGameMasterVote(player);
        }
    }
}
