/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot;

import megamek.client.bot.princess.*;
import megamek.codeUtilities.StringUtility;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.commands.DefeatCommand;
import megamek.server.commands.GameMasterCommand;
import megamek.server.commands.JoinTeamCommand;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChatProcessor {
    private final static MMLogger logger = MMLogger.create(ChatProcessor.class);

    boolean shouldBotAcknowledgeDefeat(String message, BotClient bot) {
        boolean result = false;
        if (!StringUtility.isNullOrBlank(message) &&
                (message.contains("declares individual victory at the end of the turn.")
                        || message.contains("declares team victory at the end of the turn."))) {
            String[] splitMessage = message.split(" ");
            int i = 1;
            String name = splitMessage[i];
            while (!splitMessage[i + 1].equals("declares")) {
                name += " " + splitMessage[i + 1];
                i++;
            }
            for (Player p : bot.getGame().getPlayersList()) {
                if (p.getName().equals(name)) {
                    if (p.isEnemyOf(bot.getLocalPlayer())) {
                        bot.sendChat("/defeat");
                        result = true;
                    }
                    break;
                }
            }
        }
        return result;
    }

    boolean shouldBotAcknowledgeVictory(String message, BotClient bot) {
        boolean result = false;

        if (!StringUtility.isNullOrBlank(message) && message.contains(DefeatCommand.wantsDefeat)) {
            String[] splitMessage = message.split(" ");
            int i = 1;
            String name = splitMessage[i];
            while (!splitMessage[i + 1].equals("wants")
                    && !splitMessage[i + 1].equals("admits")) {
                name += " " + splitMessage[i + 1];
                i++;
            }
            for (Player p : bot.getGame().getPlayersList()) {
                if (p.getName().equals(name)) {
                    if (p.isEnemyOf(bot.getLocalPlayer())) {
                        bot.sendChat("/victory");
                        result = true;
                    }
                    break;
                }
            }
        }

        return result;
    }

    public void processChat(GamePlayerChatEvent ge, BotClient bot) {
        if (bot.getLocalPlayer() == null) {
            return;
        }

        String message = ge.getMessage();
        if (shouldBotAcknowledgeDefeat(message, bot)) {
            return;
        }
        if (shouldBotAcknowledgeVictory(message, bot)) {
            return;
        }

        // Check for end of message.
        StringTokenizer st = new StringTokenizer(ge.getMessage(), ":");
        if (!st.hasMoreTokens()) {
            return;
        }
        String name = st.nextToken().trim();
        // who is the message from?
        Player player = null;
        for (Player gamePlayer : bot.getGame().getPlayersList()) {
            if (name.equalsIgnoreCase(gamePlayer.getName())) {
                player = gamePlayer;
                break;
            }
        }

        if (name.equals(Server.ORIGIN)) {
            String msg = st.nextToken();
            if (msg.contains(JoinTeamCommand.SERVER_VOTE_PROMPT_MSG)) {
                bot.sendChat("/allowTeamChange");
            } else if (msg.contains(GameMasterCommand.SERVER_VOTE_PROMPT_MSG)) {
                bot.sendChat("/allowGM");
            }
            return;
        } else if (player == null) {
            return;
        }

        additionalPrincessCommands(ge, (Princess) bot);
    }

    private Player getPlayer(Game game, String playerName) {
        for (Player player : game.getPlayersList()) {
            if (playerName.equalsIgnoreCase(player.getName())) {
                return player;
            }
        }

        return null;
    }

    void additionalPrincessCommands(GamePlayerChatEvent chatEvent, Princess princess) {
        // Commands should be sent in this format:
        // <botName>: <command> : <arguments>

        StringTokenizer tokenizer = new StringTokenizer(chatEvent.getMessage(), ":");
        if (tokenizer.countTokens() < 3) {
            return;
        }

        String msg = "Received message: \"" + chatEvent.getMessage() + "\".\tMessage Type: " + chatEvent.getEventName();
        logger.info(msg);

        // First token should be who sent the message.
        String from = tokenizer.nextToken().trim();

        // Second token should be the player name the message is directed to.
        String sentTo = tokenizer.nextToken().trim();
        Player princessPlayer = princess.getLocalPlayer();
        if (princessPlayer == null) {
            logger.error("Princess Player is NULL.");
            return;
        }
        String princessName = princessPlayer.getName(); // Make sure the command is directed at the Princess player.
        if (!princessName.equalsIgnoreCase(sentTo)) {
            return;
        }

        // The third token should be the actual command.
        String command = tokenizer.nextToken().trim();
        if (command.length() < 2) {
            princess.sendChat("I do not recognize that command.");
        }

        // Any remaining tokens should be the command arguments.
        String[] arguments = new String[] { command };
        if (tokenizer.hasMoreElements()) {
            String[] additionalArguments = tokenizer.nextToken().trim().split(" ");
            arguments = Stream.concat(Arrays.stream(arguments), Arrays.stream(additionalArguments))
                .toArray(String[]::new);
        }


        // Make sure the speaker is a real player.
        Player speakerPlayer = chatEvent.getPlayer();
        if (speakerPlayer == null) {
            speakerPlayer = getPlayer(princess.getGame(), from);
            if (speakerPlayer == null) {
                logger.info("speakerPlayer is NULL.");
                return;
            }
        }

        // Make sure the command came from my team.
        int speakerTeam = speakerPlayer.getTeam();
        int princessTeam = princessPlayer.getTeam();
        if ((princessTeam != speakerTeam) && !speakerPlayer.getGameMaster()) {
            msg = "Only my teammates and game-masters can command me.";
            princess.sendChat(msg);
            logger.info(msg);
            return;
        }

        processChatCommand(princess, command, arguments);
    }

    private static void processChatCommand(Princess princess, String command, String[] arguments) {
        for (ChatCommands cmd : ChatCommands.values()) {
            if (command.toLowerCase().equalsIgnoreCase(cmd.getAbbreviation())
                || command.toLowerCase().equalsIgnoreCase(cmd.getCommand())) {
                try {
                    Arguments args = ArgumentsParser.parse(arguments, cmd.getChatCommand().defineArguments());
                    cmd.getChatCommand().execute(princess, args);
                } catch (IllegalArgumentException e) {
                    princess.sendChat("Invalid arguments for command: " + command);
                    return;
                }
                return;
            }
        }
        princess.sendChat("I do not recognize that command.");
    }
}
