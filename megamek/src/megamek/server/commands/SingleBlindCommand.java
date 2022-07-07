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

import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * Allows a bot to see all units via /singleblind command. Toggle. Does not work on human players.
 * 
 * @author copied from seeall command by Dave Smith by Thom293
 * @since July 3, 2022, 9:00pm
 */
public class SingleBlindCommand extends ServerCommand {

    private final GameManager gameManager;

    public SingleBlindCommand(Server server, GameManager gameManager) {
        super(server, "singleblind",
                "Allows a BOT player to see all in double blind game. Usage: /singleblind <password> <player id#>. For a list of player id #s, use the /who command (default is yourself)");
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
            server.sendServerChat(connId, "The password is incorrect. Usage: /singleblind <password> <id#>");
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

                boolean has_single_blind = server.getPlayer(playerId).getSingleBlind();
                if (has_single_blind) {
                    give_take = " no longer has";
                } else {
                    give_take = " has been granted";
                }

                if (playerId == connId) {
                    server.sendServerChat(server.getPlayer(playerId).getName()
                            + give_take + " vision of the entire map IF IT IS A BOT ");
                } else {
                    server.sendServerChat(server.getPlayer(playerId).getName()
                            + give_take + " vision of the entire map, IF IT IS A BOT, by "
                            + server.getPlayer(connId).getName());
                }

                server.getPlayer(playerId).setSingleBlind(!has_single_blind);
                gameManager.sendEntities(playerId);
            } catch (Exception ex) {
                server.sendServerChat("/singleblind : singleblind failed. Is the player a Bot?  Type /who for a list of players with id #s.");
            }
        }
    }
}
