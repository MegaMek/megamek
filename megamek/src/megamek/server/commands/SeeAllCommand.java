/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Player;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * Allows an observer to see all units
 * 
 * @author Dave Smith
 * @since April 28, 2003, 9:00pm
 */
public class SeeAllCommand extends ServerCommand {

    private final GameManager gameManager;

    public SeeAllCommand(Server server, GameManager gameManager) {
        super(server, "seeall",
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
                    if ((!player.isSeeAllPermitted()))
                    {
                        server.sendServerChat(connId, player.getName()
                                + " is not an Observer or Game Master so may be given /seeall");
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

                server.getPlayer(playerId).setSeeAll(!has_see_all);
                gameManager.sendEntities(playerId);
            } catch (Exception ex) {
                server.sendServerChat("/seeall : seeall failed. Type /who for a list of players with id #s.");
            }
        }
    }
}
