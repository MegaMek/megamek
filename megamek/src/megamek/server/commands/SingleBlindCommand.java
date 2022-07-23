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
                String giveTake;
                // No playerArg provided. Use connId as playerId
                if (args.length <= playerArg) {
                    playerId = connId;
                } else {
                    playerId = Integer.parseInt(args[playerArg]);
                }

                Player player = server.getPlayer(playerId);

                boolean hasSingleBlind = player.getSingleBlind();
                if (hasSingleBlind) {
                    giveTake = " no longer has";
                } else {
                    if (!player.isSingleBlindPermitted()) {
                        server.sendServerChat(connId, "/singleblind : singleblind attempt failed. Is the player a Bot? Type /who for a list of players with id #s.");
                        return;
                    }
                    giveTake = " has been granted";
                }

                if (playerId == connId) {
                    server.sendServerChat(player.getName()
                            + giveTake + " vision of the entire map with /singleblind, by "
                            + server.getPlayer(connId).getName());
                } else {
                    server.sendServerChat(player.getName()
                            + giveTake + " vision of the entire map with /singleblind, by "
                            + server.getPlayer(connId).getName());
                }

                gameManager.setSingleBlind(player, !hasSingleBlind);
                gameManager.sendEntities(playerId);
            } catch (Exception ex) {
                server.sendServerChat("/singleblind : singleblind failed. Is the player a Bot? Type /who for a list of players with id #s.");
            }
        }
    }
}
