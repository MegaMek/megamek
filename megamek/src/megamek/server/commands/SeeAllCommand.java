/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * SeeAllCommand.java
 *
 * Created on April 28, 2003, 9:00  PM
 */

package megamek.server.commands;

import megamek.server.Server;

/**
 * Allows an observer to see all units
 * 
 * @author Dave Smith
 * @version
 */
public class SeeAllCommand extends ServerCommand {

    /** Creates new SeeAllCommand */
    public SeeAllCommand(Server server) {
        super(
                server,
                "seeall",
                "Allows player to see all in double blind game if you are "
                + "an observer.  Usage: /seeall <password> <player id#>.   "
                + "For a list of player id #s, use the /who command "
                + "(default is yourself)");
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        boolean doBlind = server.getGame().getOptions().booleanOption(
                "double_blind");

        int playerArg = server.isPassworded() ? 2 : 1;

        // If not double blind, this command does nothing
        if (!doBlind) {
            server.sendServerChat(connId, "Double Blind rules not in effect.");
            return;
        }
        if (server.isPassworded()
                && (args.length < 2 || !server.isPassword(args[1]))) {
            server.sendServerChat(connId, "The password is incorrect.  "
                    + "Usage: /seeall <password> <id#>");
        } else
            try {
                int playerId;
                String give_take;
                boolean has_see_all;
                // No playerArg provided. Use connId as playerId
                if (args.length <= playerArg) {
                    playerId = connId;
                } else {
                    playerId = Integer.parseInt(args[playerArg]);
                }

                has_see_all = server.getPlayer(playerId).getSeeAll();

                if (has_see_all) {
                    give_take = " no longer has";
                } else {
                    give_take = " has been granted";
                }

                if (playerId == connId) {
                    server.sendServerChat(server.getPlayer(playerId).getName()
                            + give_take + " vision of the entire map");
                } else {
                    server.sendServerChat(server.getPlayer(playerId).getName()
                            + give_take + " vision of the entire map by "
                            + server.getPlayer(connId).getName());
                }

                server.getPlayer(playerId).setSeeAll(!has_see_all);
                server.sendEntities(playerId);

            } catch (ArrayIndexOutOfBoundsException ex) {
                server.sendServerChat("/seeall : seeall failed.  "
                        + "Type /who for a list of players with id #s.");
            } catch (NumberFormatException ex) {
                server.sendServerChat("/seeall : seeall failed.  "
                        + "Type /who for a list of players with id #s.");
            } catch (NullPointerException ex) {
                server.sendServerChat("/seeall : seeall failed.  "
                        + "Type /who for a list of players with id #s.");
            }
    }

}
