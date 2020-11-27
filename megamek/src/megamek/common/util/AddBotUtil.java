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
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/9/13 8:41 AM
 */
public class AddBotUtil {

    private final List<String> results = new ArrayList<>();
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
        final StringBuilder output = new StringBuilder();
        for (final String r : results) {
            output.append(r).append("\n");
        }
        return output.toString();
    }

    public String addBot(final String[] args,
                         final IGame game,
                         final String host,
                         final int port) {
        if (2 > args.length) {
            results.add(USAGE);
            return concatResults();
        }

        StringBuilder botName = new StringBuilder("TestBot");
        StringBuilder configName = new StringBuilder();
        StringBuilder playerName = new StringBuilder();
        LogLevel verbosity = null;

        if (2 == args.length) {
            playerName = new StringBuilder(args[1]);
        }

        boolean parsingBot = false;
        boolean parsingConfig = false;
        boolean parsingPlayer = false;
        final StringBuilder fullLine = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; i++) {
            fullLine.append(" ").append(args[i]);
        }
        final String[] splitArgs = fullLine.toString().split("-");
        for (final String arg : splitArgs) {
            if (arg.toLowerCase().startsWith("b:")) {
                botName = new StringBuilder(arg.replaceFirst("b:", "").split(" ")[0].trim());
                parsingBot = true;
                parsingConfig = false;
                parsingPlayer = false;
            } else if (arg.toLowerCase().startsWith("c:")) {
                configName = new StringBuilder(arg.replaceFirst("c:", "").trim());
                parsingBot = false;
                parsingConfig = true;
                parsingPlayer = false;
            } else if (arg.toLowerCase().startsWith("p:")) {
                playerName = new StringBuilder(arg.replaceFirst("p:", "").trim());
                parsingBot = false;
                parsingConfig = false;
                parsingPlayer = true;
            } else if (arg.toLowerCase().startsWith("v:")) {
                final String verbose = arg.replaceFirst("v:", "").trim();
                verbosity = LogLevel.getLogLevel(verbose);
                if (null == verbosity) {
                    results.add("Invalid Verbosity: '" + verbose + "'.  " +
                                "Defaulting to " + LogLevel.WARNING + ".");
                    verbosity = LogLevel.WARNING;
                }
                results.add("Verbosity set to '" + verbosity + "'.");
                parsingBot = false;
                parsingConfig = false;
                parsingPlayer = false;
            } else if (parsingBot) {
                botName.append("-").append(arg);
            } else if (parsingConfig) {
                configName.append("-").append(arg);
            } else if (parsingPlayer) {
                playerName.append("-").append(arg);
            }
        }

        if (StringUtil.isNullOrEmpty(playerName)) {
            String argLine = fullLine.toString();
            argLine = argLine.replaceFirst("/replacePlayer", "");
            argLine = argLine.replaceFirst("-b:" + botName, "");
            argLine = argLine.replaceFirst("-c:" + configName, "");
            argLine = argLine.replaceFirst("-v:" + verbosity, "");
            playerName = new StringBuilder(argLine.trim());
        }

        IPlayer target = null;
        for (final Enumeration<IPlayer> i = game.getPlayers(); i.hasMoreElements(); ) {
            final IPlayer player = i.nextElement();
            if (player.getName().equals(playerName.toString())) {
                target = player;
            }
        }

        if (null == target) {
            results.add("No player with the name '" + playerName + "'.");
            return concatResults();
        }
        final int playerId = target.getId();

        if (!target.isGhost()) {
            results.add("Player " + target.getName() + " is not a ghost.");
            return concatResults();
        }

        final BotClient botClient;
        if ("Princess".equalsIgnoreCase(botName.toString())) {
            botClient = makeNewPrincessClient(target, verbosity, host, port);
            if (!StringUtil.isNullOrEmpty(configName)) {
                final BehaviorSettings behavior = BehaviorSettingsFactory.getInstance()
                                                                         .getBehavior(configName.toString());
                if (null != behavior) {
                    if (null != verbosity) {
                        behavior.setVerbosity(verbosity);
                    }
                    ((Princess) botClient).setBehaviorSettings(behavior);
                } else {
                    results.add("Unrecognized Behavior Setting: '" + configName + "'.  Using DEFAULT.");
                    ((Princess) botClient).setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
                }
            } else {
                ((Princess) botClient).setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
            }
        } else if ("TestBot".equalsIgnoreCase(botName.toString())) {
            botClient = makeNewTestBotClient(target, host, port);
        } else {
            results.add("Unrecognized bot: '" + botName + "'.  Defaulting to TestBot.");
            botName = new StringBuilder("TestBot");
            botClient = makeNewTestBotClient(target, host, port);
        }
        botClient.getGame().addGameListener(new BotGUI(botClient));
        try {
            botClient.connect();
        } catch (final Exception e) {
            results.add(botName + " failed to connect.");
            return concatResults();
        }
        botClient.setLocalPlayerNumber(playerId);

        final StringBuilder result = new StringBuilder(botName);
        result.append(" has replaced ").append(target.getName()).append(".");
        if (botClient instanceof Princess) {
            result.append("  Config: ").append(((Princess) botClient).getBehaviorSettings().getDescription()).append
                    (".");
            result.append("  Verbosity: ").append(((Princess) botClient).getVerbosity()).append(".");
        }
        results.add(result.toString());
        return concatResults();
    }

    BotClient makeNewPrincessClient(final IPlayer target,
                                    final LogLevel verbosity,
                                    final String host,
                                    final int port) {
        return new Princess(target.getName(), host, port, (null == verbosity ? LogLevel.WARNING : verbosity));
    }

    BotClient makeNewTestBotClient(final IPlayer target,
                                   final String host,
                                   final int port) {
        return new TestBot(target.getName(), host, port);
    }
}
