/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.commands;

import java.util.Enumeration;

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.AWT.BotGUI;
import megamek.common.Player;

/**
 * @author dirk
 */
public class AddBotCommand extends ClientCommand {
    /**
     * @param client the client this command will be registered to.
     */
    public AddBotCommand(Client client) {
        super(
                client,
                "replacePlayer",
                "Replaces a player who is a ghost with a bot. Usage #replacePlayer name, to replace the player named name. they must be a ghost.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        if (args.length < 2) {
            return "You must specify a player name.";
        }
        Player target = null;
        for (Enumeration<Player> i = client.game.getPlayers(); i
                .hasMoreElements();) {
            Player player = i.nextElement();
            if (player.getName().equals(args[1])) {
                target = player;
            }
        }

        if (target == null) {
            return "No player with the name '" + args[1] + "'.";
        }

        if (target.isGhost()) {
            BotClient c = new TestBot(target.getName(), client.getHost(),
                    client.getPort());
            c.game.addGameListener(new BotGUI(c));
            try {
                c.connect();
            } catch (Exception e) {
                return "Bot failed to connect.";
            }
            c.retrieveServerInfo();
            return "Bot has replaced " + target.getName() + ".";
        }

        return "Player " + target.getName() + " is not a ghost.";
    }

}
