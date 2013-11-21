/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.util;

import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.logging.LogLevel;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/9/13 8:41 AM
 */
public class AddBotUtil {
    private final List<String> results = new ArrayList<String>();
    public static final String COMMAND = "replacePlayer";
    public static final String USAGE = "Replaces a player who is a ghost with a bot." +
                                       "\nUsage /replacePlayer <-b:TestBot/Princess> <-c:Config> <-v:Verbosity> " +
                                       "<-p:>name." +
                                       "\n  <-b> Specifies use if either TestBot or Princess.  If left out, " +
                                       "TestBot will be used." +
                                       "\n  <-c> Specifies a saved configuration to be used by Princess.  If left out" +
                                       " DEFAULT will be used." +
                                       "\n  <-v> Specifies the verbosity level for Princess " +
                                       "(DEBUG/INFO/WARNING/ERROR)." +
                                       "\n  <-p> Specifies the player name.  The '-p' is only required when the '-c' " +
                                       "or '-v' parameters are also used.";

    private String concatResults() {
        StringBuilder output = new StringBuilder();
        for (String r : results) {
            output.append(r).append("\n");
        }
        return output.toString();
    }

    public String addBot(String[] args, IGame game, String host, int port) {
        if (args.length < 2) {
            results.add(USAGE);
            return concatResults();
        }

        String botName = "TestBot";
        String configName = "";
        String playerName = "";
        LogLevel verbosity = null;

        if (args.length == 2) {
            playerName = args[1];
        }

        boolean parsingBot = false;
        boolean parsingConfig = false;
        boolean parsingPlayer = false;
        StringBuilder fullLine = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; i++) {
            fullLine.append(" ").append(args[i]);
        }
        String[] splitArgs = fullLine.toString().split("-");
        for (String arg : splitArgs) {
            if (arg.toLowerCase().startsWith("b:")) {
                botName = arg.replaceFirst("b:", "").split(" ")[0].trim();
                parsingBot = true;
                parsingConfig = false;
                parsingPlayer = false;
            } else if (arg.toLowerCase().startsWith("c:")) {
                configName = arg.replaceFirst("c:", "").trim();
                parsingBot = false;
                parsingConfig = true;
                parsingPlayer = false;
            } else if (arg.toLowerCase().startsWith("p:")) {
                playerName = arg.replaceFirst("p:", "").trim();
                parsingBot = false;
                parsingConfig = false;
                parsingPlayer = true;
            } else if (arg.toLowerCase().startsWith("v:")) {
                String verbose = arg.replaceFirst("v:", "").trim();
                if (StringUtil.isNumeric(verbose)) {
                    verbosity = LogLevel.getLogLevel(Integer.parseInt(verbose));
                } else {
                    verbosity = LogLevel.getLogLevel(verbose);
                }
                if (verbosity == null) {
                    results.add("Invalid Verbosity: '" + verbose + "'.  Defaulting to WARN.");
                }
                results.add("Verbosity set to '" + verbosity.toString() + "'.");
                parsingBot = false;
                parsingConfig = false;
                parsingPlayer = false;
            } else if (parsingBot) {
                botName += " " + arg;
            } else if (parsingConfig) {
                configName += " " + arg;
            } else if (parsingPlayer) {
                playerName += " " + arg;
            }
        }

        if (StringUtil.isNullOrEmpty(playerName)) {
            String argLine = fullLine.toString();
            argLine = argLine.replaceFirst("/replacePlayer", "");
            argLine = argLine.replaceFirst("-b:" + botName, "");
            argLine = argLine.replaceFirst("-c:" + configName, "");
            argLine = argLine.replaceFirst("-v:" + verbosity, "");
            playerName = argLine.trim();
        }

        IPlayer target = null;
        for (Enumeration<IPlayer> i = game.getPlayers(); i.hasMoreElements(); ) {
            IPlayer player = i.nextElement();
            if (player.getName().equals(playerName)) {
                target = player;
            }
        }

        if (target == null) {
            results.add("No player with the name '" + playerName + "'.");
            return concatResults();
        }
        int playerId = target.getId();

        if (!target.isGhost()) {
            results.add("Player " + target.getName() + " is not a ghost.");
            return concatResults();
        }

        BotClient botClient;
        if ("Princess".equalsIgnoreCase(botName)) {
            botClient = makeNewPrincessClient(target, verbosity, host, port);
            if (!StringUtil.isNullOrEmpty(configName)) {
                BehaviorSettings behavior = BehaviorSettingsFactory.getInstance().getBehavior(configName);
                if (behavior != null) {
                    ((Princess) botClient).setBehaviorSettings(behavior);
                } else {
                    results.add("Unrecognized Behavior Setting: '" + configName + "'.  Using DEFAULT.");
                    ((Princess) botClient).setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
                }
            } else {
                ((Princess) botClient).setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
            }
        } else if ("TestBot".equalsIgnoreCase(botName)) {
            botClient = makeNewTestBotClient(target, host, port);
        } else {
            results.add("Unrecognized bot: '" + botName + "'.  Defaulting to TestBot.");
            botName = "TestBot";
            botClient = makeNewTestBotClient(target, host, port);
        }
        botClient.getGame().addGameListener(new BotGUI(botClient));
        try {
            botClient.connect();
        } catch (Exception e) {
            results.add(botName + " failed to connect.");
            return concatResults();
        }
        botClient.retrieveServerInfo();
        botClient.setLocal_pn(playerId);

        StringBuilder result = new StringBuilder(botName);
        result.append(" has replaced ").append(target.getName()).append(".");
        if (botClient instanceof Princess) {
            result.append("  Config: ").append(((Princess) botClient).getBehaviorSettings().getDescription()).append
                    (".");
            result.append("  Verbosity: ").append(((Princess) botClient).getVerbosity()).append(".");
        }
        results.add(result.toString());
        return concatResults();
    }

    protected BotClient makeNewPrincessClient(IPlayer target, LogLevel verbosity, String host, int port) {
        return new Princess(target.getName(), host, port, (verbosity == null ? LogLevel.WARNING : verbosity));
    }

    protected BotClient makeNewTestBotClient(IPlayer target, String host, int port) {
        return new TestBot(target.getName(), host, port);
    }
}
