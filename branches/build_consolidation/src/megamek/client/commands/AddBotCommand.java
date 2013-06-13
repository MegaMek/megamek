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
import megamek.client.bot.princess.Princess;
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
                "Replaces a player who is a ghost with a bot. Usage /replacePlayer <-b:TestBot/Princess> name, to replace the player named name. They must be a ghost.  If the -b argument is left out, the TestBot will be used by default.");
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

        String botName = "TestBot";
        int playerListStart = 1;
        if (args[1].toLowerCase().startsWith("-b:")) {
            botName = args[1].replaceFirst("-b:", "");
            playerListStart = 2;
        }

        String playerName = args[playerListStart];
        for(int i = (playerListStart + 1); i < args.length; i++) {
            playerName = playerName + " " + args[i];
        }

        Player target = null;
        for (Enumeration<Player> i = client.game.getPlayers(); i
                .hasMoreElements();) {
            Player player = i.nextElement();
            if (player.getName().equals(playerName)) {
                target = player;
            }
        }

        if (target == null) {
            return "No player with the name '" + args[1] + "'.";
        }

        if (target.isGhost()) {
            BotClient c = null;
            if ("Princess".equalsIgnoreCase(botName)) {
                c = new Princess(target.getName(), client.getHost(), client.getPort(), Princess.LogLevel.ERROR);
            } else if ("TestBot".equalsIgnoreCase(botName)) {
                c = new TestBot(target.getName(), client.getHost(), client.getPort());
            } else {
                client.sendChat("Unrecognized bot: '" + botName + "'.  Defaulting to TestBot.");
                botName = "TestBot";
                c = new TestBot(target.getName(), client.getHost(), client.getPort());
            }
            c.game.addGameListener(new BotGUI(c));
            try {
                c.connect();
            } catch (Exception e) {
                return botName + " failed to connect.";
            }
            c.retrieveServerInfo();
            return botName + " has replaced " + target.getName() + ".";
        }

        return "Player " + target.getName() + " is not a ghost.";
    }

}
