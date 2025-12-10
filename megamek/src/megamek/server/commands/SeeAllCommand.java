/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Allows an observer to see all units
 *
 * @author Dave Smith
 * @since April 28, 2003, 9:00pm
 */
public class SeeAllCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public SeeAllCommand(Server server, TWGameManager gameManager) {
        super(server,
              "seeall",
              "Allows a player to see all in double blind game if you are an observer. Usage: /seeall <password> <player id#>. For a list of player id #s, use the /who command (default is yourself)");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String... args) {
        boolean doBlind = server.getGame().getOptions().booleanOption(
              OptionsConstants.ADVANCED_DOUBLE_BLIND);

        int playerArg = server.isPassworded() ? 2 : 1;

        // If not double-blind, this command does nothing
        if (!doBlind) {
            server.sendServerChat(connId, "Double Blind rules not in effect.");
            return;
        }

        if (server.isPassworded()
              && (args.length < 2 || !server.isPassword(args[1]))) {
            server.sendServerChat(connId, "The password is incorrect. Usage: /seeall <password> <id#>");
        } else {
            try {
                int playerId;
                String give_take;
                // No playerArg provided. Use connId as playerId
                if (args.length <= playerArg) {
                    playerId = connId;
                } else {
                    playerId = Integer.parseInt(args[playerArg]);
                }

                Player player = server.getPlayer(playerId);

                boolean has_see_all = player.getSeeAll();
                if (has_see_all) {
                    give_take = " no longer has";
                } else {
                    if ((!player.isSeeAllPermitted())) {
                        server.sendServerChat(connId, player.getName()
                              + " is not an Observer or Game Master so may not be given /seeall");
                        return;
                    }
                    give_take = " has been granted";
                }

                if (playerId == connId) {
                    server.sendServerChat(player.getName()
                          + give_take + " vision of the entire map");
                } else {
                    server.sendServerChat(player.getName()
                          + give_take + " vision of the entire map by "
                          + server.getPlayer(connId).getName());
                }

                gameManager.setSeeAll(player, !has_see_all);
                gameManager.sendEntities(playerId);
            } catch (Exception ex) {
                server.sendServerChat("/seeall : seeall failed. Type /who for a list of players with id #s.");
            }
        }
    }
}
