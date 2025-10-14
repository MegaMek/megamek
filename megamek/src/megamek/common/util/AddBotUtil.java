/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JFrame;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.codeUtilities.StringUtility;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/9/13 8:41 AM
 */
public class AddBotUtil {

    private final List<String> results = new ArrayList<>();
    public static final String COMMAND = "replacePlayer";
    public static final String USAGE = """
          Replaces a player who is a ghost with a bot.\
          
          Usage /replacePlayer <-b:Princess> <-c:Config> <-v:Verbosity> \
          <-p:>name.\
          
            <-b> Specifies use if Princess. \
          
            <-c> Specifies a saved configuration to be used by Princess.  If left out\
           DEFAULT will be used.\
          
            <-v> Specifies the verbosity level for Princess \
          (DEBUG/INFO/WARNING/ERROR).\
          
            <-p> Specifies the player name.  The '-p' is only required when the '-c' \
          or '-v' parameters are also used.""";

    private String concatResults() {
        final StringBuilder output = new StringBuilder();
        for (final String r : results) {
            output.append(r).append("\n");
        }
        return output.toString();
    }

    public String addBot(final String[] args, final Game game, final String host, final int port) {
        if (2 > args.length) {
            results.add(USAGE);
            return concatResults();
        }

        StringBuilder botName = new StringBuilder("Princess");
        StringBuilder configName = new StringBuilder();
        StringBuilder playerName = new StringBuilder();

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
            } else if (parsingBot) {
                botName.append("-").append(arg);
            } else if (parsingConfig) {
                configName.append("-").append(arg);
            } else if (parsingPlayer) {
                playerName.append("-").append(arg);
            }
        }

        if (StringUtility.isNullOrBlank(playerName)) {
            String argLine = fullLine.toString();
            argLine = argLine.replaceFirst("/replacePlayer", "");
            argLine = argLine.replaceFirst("-b:" + botName, "");
            argLine = argLine.replaceFirst("-c:" + configName, "");
            playerName = new StringBuilder(argLine.trim());
        }

        Player target = null;
        for (Player player : game.getPlayersList()) {
            if (player.getName().contentEquals(playerName)) {
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
            botClient = makeNewPrincessClient(target, host, port);
            if (!StringUtility.isNullOrBlank(configName)) {
                final BehaviorSettings behavior = BehaviorSettingsFactory.getInstance()
                      .getBehavior(configName.toString());
                if (null != behavior) {
                    ((Princess) botClient).setBehaviorSettings(behavior);
                } else {
                    results.add("Unrecognized Behavior Setting: '" + configName + "'.  Using DEFAULT.");
                    ((Princess) botClient).setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
                }
            } else {
                ((Princess) botClient).setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
            }
        } else {
            results.add("Unrecognized bot: '" + botName + "'.  Defaulting to Princess.");
            botName = new StringBuilder("Princess");
            botClient = makeNewPrincessClient(target, host, port);
        }

        if (!GraphicsEnvironment.isHeadless()) {
            // GUI to show bot help nag if needed
            // FIXME : I should be able to access the JFrame by proper ways
            botClient.getGame().addGameListener(new BotGUI(new JFrame(), botClient));
        }
        try {
            botClient.connect();
        } catch (final Exception ex) {
            results.add(botName + " failed to connect.");
            return concatResults();
        }
        botClient.setLocalPlayerNumber(playerId);

        final StringBuilder result = new StringBuilder(botName);
        result.append(" has replaced ").append(target.getName()).append(".");
        if (botClient instanceof Princess) {
            result.append("  Config: ")
                  .append(((Princess) botClient).getBehaviorSettings().getDescription())
                  .append(".");
        }
        results.add(result.toString());
        return concatResults();
    }

    public @Nullable Princess replaceGhostWithBot(final BehaviorSettings behavior, final String playerName,
          final Client client, StringBuilder message) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(behavior);

        final Game game = client.getGame();
        final String host = client.getHost();
        final int port = client.getPort();

        Objects.requireNonNull(game);

        Optional<Player> possible = game.getPlayersList()
              .stream()
              .filter(p -> p.getName().equals(playerName))
              .findFirst();
        if (possible.isEmpty()) {
            message.append("No player with the name '").append(playerName).append("'.");
            return null;
        } else if (!possible.get().isGhost()) {
            message.append("Player '").append(playerName).append("' is not a ghost.");
            return null;
        }

        final Player target = possible.get();
        final Princess princess = new Princess(target.getName(), host, port);
        princess.startPrecognition();
        princess.setBehaviorSettings(behavior);

        try {
            princess.connect();
        } catch (final Exception ex) {
            message.append("Princess failed to connect.");
        }
        princess.setLocalPlayerNumber(target.getId());
        message.append("Princess has replaced ").append(playerName).append(".");
        return princess;
    }

    /**
     * Replace a ghost player or an existing Princess bot with a new bot
     */
    public void changeBotSettings(final BehaviorSettings behavior, final String playerName, final Client client,
          StringBuilder message) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(behavior);

        final Game game = client.getGame();
        final String host = client.getHost();
        final int port = client.getPort();

        Objects.requireNonNull(game);

        Optional<Player> possible = game.getPlayersList()
              .stream()
              .filter(p -> p.getName().equals(playerName))
              .findFirst();
        if (possible.isEmpty()) {
            message.append("No player with the name '").append(playerName).append("'.");
            return;
        } else if (!possible.get().isGhost() && !possible.get().isBot()) {
            message.append("Player '").append(playerName).append("' is neither a ghost nor an existing bot.");
            return;
        }

        final Player target = possible.get();
        if (target.isGhost()) {
            final Princess princess = new Princess(target.getName(), host, port);
            princess.setBehaviorSettings(behavior);
            try {
                princess.connect();
                princess.startPrecognition();
            } catch (final Exception ex) {
                message.append("Princess failed to connect.");
            }
            princess.setLocalPlayerNumber(target.getId());
            message.append("Princess has replaced ").append(playerName).append(".");
        } else {
            AbstractClient bot = client.getBots().get(target.getName());
            if (bot == null) {
                message.append("Player '").append(playerName).append("' is not a local bot.");
                return;
            } else if (!(bot instanceof Princess)) {
                message.append("Player '").append(playerName).append("' is not a Princess bot.");
                return;
            }
            Princess princess = (Princess) bot;
            princess.setBehaviorSettings(behavior);
        }
    }

    public void kickBot(final String playerName, final Client client, StringBuilder message) {
        Objects.requireNonNull(client);
        final Game game = client.getGame();
        Objects.requireNonNull(game);

        Optional<Player> possible = game.getPlayersList()
              .stream()
              .filter(p -> p.getName().equals(playerName))
              .findFirst();

        if (possible.isEmpty()) {
            message.append("No player with the name '").append(playerName).append("'.");
            return;
        } else if (possible.get().isGhost()) {
            message.append("Player '").append(playerName).append("' is a ghost.");
            return;
        } else if (!possible.get().isBot()) {
            message.append("Player '").append(playerName).append("' is not a bot.");
            return;
        }

        final Player target = possible.get();
        client.sendChat("/kick " + target.getId());
    }

    BotClient makeNewPrincessClient(final Player target, final String host, final int port) {
        Princess princess = new Princess(target.getName(), host, port);
        princess.startPrecognition();
        return princess;
    }
}
