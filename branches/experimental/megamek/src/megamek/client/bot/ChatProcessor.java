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
package megamek.client.bot;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.logging.LogLevel;
import megamek.common.util.StringUtil;
import megamek.server.commands.DefeatCommand;

public class ChatProcessor {

    protected boolean shouldBotAcknowledgeDefeat(String message, BotClient bot) {
        boolean result = false;
        if (!StringUtil.isNullOrEmpty(message) &&
            (message.contains("declares individual victory at the end of the turn.")
             || message.contains("declares team victory at the end of the turn."))) {
            String[] splitMessage = message.split(" ");
            int i = 1;
            String name = splitMessage[i];
            while (!splitMessage[i + 1].equals("declares")) {
                name += " " + splitMessage[i + 1];
                i++;
            }
            for (IPlayer p : bot.getGame().getPlayersVector()) {
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

    protected boolean shouldBotAcknowledgeVictory(String message, BotClient bot) {
        boolean result = false;

        if (!StringUtil.isNullOrEmpty(message) &&
            (message.contains(DefeatCommand.wantsDefeat) || message.contains(DefeatCommand.admitsDefeat))) {
            String[] splitMessage = message.split(" ");
            int i = 1;
            String name = splitMessage[i];
            while (!splitMessage[i + 1].equals("wants")
                   && !splitMessage[i + 1].equals("admits")) {
                name += " " + splitMessage[i + 1];
                i++;
            }
            for (IPlayer p : bot.getGame().getPlayersVector()) {
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
        StringTokenizer st = new StringTokenizer(ge.getMessage(), ":"); //$NON-NLS-1$
        if (!st.hasMoreTokens()) {
            return;
        }
        String name = st.nextToken().trim();
        // who is the message from?
        Enumeration<IPlayer> e = bot.getGame().getPlayers();
        IPlayer p = null;
        while (e.hasMoreElements()) {
            p = e.nextElement();
            if (name.equalsIgnoreCase(p.getName())) {
                break;
            }
        }
        if (p == null) {
            return;
        }
        if (bot instanceof TestBot) {
            additionalTestBotCommands(st, (TestBot) bot, p);
        } else if (bot instanceof Princess) {
            additionalPrincessCommands(ge, (Princess) bot);
        }
    }

    private void additionalTestBotCommands(StringTokenizer st, TestBot tb,
                                           IPlayer p) {
        try {
            if (st.hasMoreTokens()
                && st.nextToken().trim()
                     .equalsIgnoreCase(tb.getLocalPlayer().getName())) {
                if (!p.isEnemyOf(tb.getLocalPlayer())) {
                    if (st.hasMoreTokens()) {
                        String command = st.nextToken().trim();
                        boolean understood = false;
                        // should create a command factory and a
                        // command object...
                        if (command.equalsIgnoreCase("echo")) { //$NON-NLS-1$
                            understood = true;
                        }
                        if (command.equalsIgnoreCase("calm down")) { //$NON-NLS-1$
                            Iterator<Entity> i = tb.getEntitiesOwned()
                                                   .iterator();
                            while (i.hasNext()) {
                                CEntity cen = tb.centities.get(i.next());
                                if (cen.strategy.attack > 1) {
                                    cen.strategy.attack = 1;
                                }
                            }
                            understood = true;
                        } else if (command.equalsIgnoreCase("be aggressive")) { //$NON-NLS-1$
                            Iterator<Entity> i = tb.getEntitiesOwned()
                                                   .iterator();
                            while (i.hasNext()) {
                                CEntity cen = tb.centities.get(i.next());
                                cen.strategy.attack = Math.min(
                                        cen.strategy.attack * 1.2, 1.5);
                            }
                            understood = true;
                        } else if (command.equalsIgnoreCase("attack")) { //$NON-NLS-1$
                            int x = Integer.parseInt(st.nextToken().trim());
                            int y = Integer.parseInt(st.nextToken().trim());
                            Entity en = tb.getGame().getFirstEntity(new Coords(
                                    x - 1, y - 1));
                            if (en != null) {
                                if (en.isEnemyOf(tb.getEntitiesOwned().get(0))) {
                                    CEntity cen = tb.centities.get(en);
                                    cen.strategy.target += 3;
                                    System.out.println(cen.entity
                                                          .getShortName()
                                                       + " " + cen.strategy.target); //$NON-NLS-1$
                                    understood = true;
                                }
                            }
                        }
                        if (understood) {
                            tb.sendChat("Understood " + p.getName());
                        }
                    }
                } else {
                    tb.sendChat("I can't do that, " + p.getName());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private IPlayer getPlayer(IGame game, String playerName) {
        Enumeration<IPlayer> players = game.getPlayers();
        while (players.hasMoreElements()) {
            IPlayer testPlayer = players.nextElement();
            if (playerName.equalsIgnoreCase(testPlayer.getName())) {
                return testPlayer;
            }
        }
        return null;
    }

    protected void additionalPrincessCommands(GamePlayerChatEvent chatEvent, Princess princess) {
        final String METHOD_NAME = "additionalPrincessCommands(GamePlayerChatEvent, Princess, IPlayer)";

        // Commands should be sent in this format:
        //   <botName>: <command> : <arguments>

        StringTokenizer tokenizer = new StringTokenizer(chatEvent.getMessage(), ":");
        if (tokenizer.countTokens() < 3) {
            return;
        }

        String msg = "Received message: \"" + chatEvent.getMessage() + "\".\tMessage Type: " + chatEvent.getEventName();
        princess.log(getClass(), METHOD_NAME, LogLevel.INFO, msg);

        String from = tokenizer.nextToken().trim(); // First token should be who sent the message.
        String sentTo = tokenizer.nextToken().trim(); // Second token should be the player name the message is directed
                                                      // to.
        String command = tokenizer.nextToken().trim(); // The third token should be the actual command.
        if (command.length() < 2) {
            princess.sendChat("I do not recognize that command.");
        }
        String[] arguments = null; // Any remaining tokens should be the command arguments.
        if (tokenizer.hasMoreElements()) {
            arguments = tokenizer.nextToken().trim().split(" ");
        }

        // Make sure the command is directed at the Princess player.
        IPlayer speakerPlayer = chatEvent.getPlayer();
        if (speakerPlayer == null) {
            speakerPlayer = getPlayer(princess.getGame(), from);
            if (speakerPlayer == null) {
                princess.log(getClass(), METHOD_NAME, LogLevel.ERROR, "speakerPlayer is NULL.");
                return;
            }
        }
        IPlayer princessPlayer = princess.getLocalPlayer();
        if (princessPlayer == null) {
            princess.log(getClass(), METHOD_NAME, LogLevel.ERROR, "Princess Player is NULL.");
            return;
        }
        String princessName = princessPlayer.getName();
        if (!princessName.equalsIgnoreCase(sentTo)) {
            return;
        }

        // Make sure the command came from my team.
        int speakerTeam = speakerPlayer.getTeam();
        int princessTeam = princessPlayer.getTeam();
        if (princessTeam != speakerTeam) {
            return;
        }

        // If instructed to, flee.
        if (command.toLowerCase().startsWith(Princess.CMD_FLEE)) {
            msg = "Received flee order!";
            princess.log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
            princess.sendChat("Run Away!");
            princess.setShouldFlee(true, msg);
            return;
        }

        // Change verbosity level.
        if (command.toLowerCase().startsWith(Princess.CMD_VERBOSE)) {
            if (arguments == null || arguments.length == 0) {
                msg = "No log level specified.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }
            LogLevel newLevel = LogLevel.getLogLevel(arguments[0].trim());
            if (newLevel == null) {
                msg = "Invalid verbosity specified: " + arguments[0];
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg);
                princess.sendChat(msg);
                return;
            }
            princess.setVerbosity(newLevel);
            msg = "Verbosity set to " + princess.getVerbosity().toString();
            princess.log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
            princess.sendChat(msg);
            return;
        }

        // Load a new behavior.
        if (command.toLowerCase().startsWith(Princess.CMD_BEHAVIOR)) {
            if (arguments == null || arguments.length == 0) {
                msg = "No new behavior specified.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }
            String behaviorName = arguments[0].trim();
            BehaviorSettings newBehavior = BehaviorSettingsFactory.getInstance().getBehavior(behaviorName);
            if (newBehavior == null) {
                msg = "Behavior '" + behaviorName + "' does not exist.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg);
                princess.sendChat(msg);
                return;
            }
            princess.setBehaviorSettings(newBehavior);
            msg = "Behavior changed to " + princess.getBehaviorSettings().getDescription();
            princess.sendChat(msg);
            return;
        }

        // Adjust fall shame.
        if (command.toLowerCase().startsWith(Princess.CMD_CAUTION)) {
            if (arguments == null || arguments.length == 0) {
                msg = "Invalid Syntax.  Should be 'princessName : caution : <+/->'.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }

            String adjustment = arguments[0];
            int currentFallShame = princess.getBehaviorSettings().getFallShameIndex();
            int newFallShame = currentFallShame;
            newFallShame += princess.calculateAdjustment(adjustment);
            princess.getBehaviorSettings().setFallShameIndex(newFallShame);
            msg = "Piloting Caution changed from " + currentFallShame + " to " +
                  princess.getBehaviorSettings().getFallShameIndex();
            princess.sendChat(msg);
        }

        // Adjust self preservation.
        if (command.toLowerCase().startsWith(Princess.CMD_AVOID)) {
            if (arguments == null || arguments.length == 0) {
                msg = "Invalid Syntax.  Should be 'princessName : avoid : <+/->'.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }

            String adjustment = arguments[0];
            int currentSelfPreservation = princess.getBehaviorSettings().getSelfPreservationIndex();
            int newSelfPreservation = currentSelfPreservation;
            newSelfPreservation += princess.calculateAdjustment(adjustment);
            princess.getBehaviorSettings().setSelfPreservationIndex(newSelfPreservation);
            msg = "Self Preservation changed from " + currentSelfPreservation + " to " +
                  princess.getBehaviorSettings().getSelfPreservationIndex();
            princess.sendChat(msg);
        }

        // Adjust aggression.
        if (command.toLowerCase().startsWith(Princess.CMD_AGGRESSION)) {
            if (arguments == null || arguments.length == 0) {
                msg = "Invalid Syntax.  Should be 'princessName : aggression : <+/->'.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }

            String adjustment = arguments[0];
            int currentAggression = princess.getBehaviorSettings().getHyperAggressionIndex();
            int newAggression = currentAggression;
            newAggression += princess.calculateAdjustment(adjustment);
            princess.getBehaviorSettings().setHyperAggressionIndex(newAggression);
            msg = "Aggression changed from " + currentAggression + " to " +
                  princess.getBehaviorSettings().getHyperAggressionIndex();
            princess.sendChat(msg);
        }

        // Adjust herd mentality.
        if (command.toLowerCase().startsWith(Princess.CMD_HERDING)) {
            if (arguments == null || arguments.length == 0) {
                msg = "Invalid Syntax.  Should be 'princessName : herding : <+/->'.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }

            String adjustment = arguments[0];
            int currentHerding = princess.getBehaviorSettings().getHerdMentalityIndex();
            int newHerding = currentHerding;
            newHerding += princess.calculateAdjustment(adjustment);
            princess.getBehaviorSettings().setHerdMentalityIndex(newHerding);
            msg = "Herding changed from " + currentHerding + " to " +
                  princess.getBehaviorSettings().getHerdMentalityIndex();
            princess.sendChat(msg);
        }

        // Adjust bravery.
        if (command.toLowerCase().startsWith(Princess.CMD_BRAVERY)) {
            if (arguments == null || arguments.length == 0) {
                msg = "Invalid Syntax.  Should be 'princessName : brave : <+/->'.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }

            String adjustment = arguments[0];
            int currentBravery = princess.getBehaviorSettings().getBraveryIndex();
            int newBravery = currentBravery;
            newBravery += princess.calculateAdjustment(adjustment);
            princess.getBehaviorSettings().setBraveryIndex(newBravery);
            msg = "Bravery changed from " + currentBravery + " to " +
                  princess.getBehaviorSettings().getBraveryIndex();
            princess.sendChat(msg);
        }

        if (command.toLowerCase().startsWith(Princess.CMD_TARGET)) {
            if (arguments == null || arguments.length == 0) {
                msg = "Invalid syntax.  Should be 'princessName : target : hexNumber'.";
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }

            String hex = arguments[0];
            if (hex.length() != 4 || !StringUtil.isPositiveInteger(hex)) {
                msg = "Invalid hex number: " + hex;
                princess.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg + "\n" + chatEvent.getMessage());
                princess.sendChat(msg);
                return;
            }

            princess.getBehaviorSettings().addStrategicTarget(hex);
            msg = "Hex " + hex + " added to strategic targets list.";
            princess.sendChat(msg);
        }
    }
}