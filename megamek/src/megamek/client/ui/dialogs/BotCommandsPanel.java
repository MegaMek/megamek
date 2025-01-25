/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.client.ui.dialogs;

import megamek.client.AbstractClient;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.audio.AudioService;
import megamek.client.ui.swing.audio.SoundType;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.MenuScroller;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Bot Commands Panel contains a small set of buttons which allow the player to change the configuration of any bot they have
 * control over during game play. It also allows for some orders to be given to the bots, like telling them to ignore a target, change
 * priority over another.
 * @author Luana Coppio
 */
public class BotCommandsPanel extends JPanel {
    private final AbstractClient client;
    private final AudioService audioService;
    private final MegaMekController controller;
    private final JButton miscButton = new JButton();
    // This latch is used only to change the state of the button from pause to continue and back
    private boolean pauseLatch = false;
    private JButton pauseContinue;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    /**
     * Bot Commands Panel constructor.
     *
     * @param client       reference to the client instance
     * @param audioService reference to the instance of the AudioService
     */
    public BotCommandsPanel(AbstractClient client, @Nullable AudioService audioService, @Nullable MegaMekController controller) {
        this.client = client;
        this.audioService = audioService;
        this.controller = controller;

        this.initialize();
    }

    /**
     * Returns a non-modal dialog with a minimap for the given game.
     */
    public static JDialog createBotCommandDialog(JFrame parent, AbstractClient client, AudioService audioService, MegaMekController controller) {
        var result = new JDialog(parent, Messages.getString("ClientGUI.BotCommand"), false);

        result.setLocation(GUIP.getBotCommandsPosX(), GUIP.getBotCommandsPosY());
        result.setSize(new Dimension(600, 120));
        result.setMinimumSize(new Dimension(600, 120));
        result.setResizable(true);
        result.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                GUIP.setBotCommandsEnabled(false);
            }
        });

        result.add(new BotCommandsPanel(client, audioService, controller));
        return result;
    }

    private void initialize() {
        this.setLayout(new GridLayout(2, 4, 2, 2));
        this.setMinimumSize(new Dimension(600, 80));
        this.setPreferredSize(new Dimension(-1, 80));
        this.setMaximumSize(new Dimension(-1, 80));
        var retreat = createButton("Retreat");
        pauseContinue = createButton("PauseGame");
        var maneuver = createButton("Maneuver");
        var priorityTarget = createButton("PriorityTarget");
        var ignoreTarget = createButton("IgnoreTarget");
        var setBehavior = createButton("SetBehavior");

        maneuver.addActionListener(e -> {
            JPopupMenu popup = createManeuverPopup();
            popup.show(maneuver, 0, maneuver.getHeight());
        });
        priorityTarget.addActionListener(e -> {
            JPopupMenu popup = createPriorityTargetPopup();
            popup.show(priorityTarget, 0, priorityTarget.getHeight());
        });
        ignoreTarget.addActionListener(e -> {
            JPopupMenu popup = createIgnoreTargetPopup();
            popup.show(ignoreTarget, 0, ignoreTarget.getHeight());
        });
        retreat.addActionListener(e -> {
            JPopupMenu popup = createRetreatPopup();
            popup.show(retreat, 0, retreat.getHeight());
        });
        setBehavior.addActionListener(e -> {
            JPopupMenu popup = createSelectBehaviorPopup();
            popup.show(setBehavior, 0, setBehavior.getHeight());
        });
        pauseContinue.addActionListener(e -> pauseUnpause());

        // Add them to the buttonPanel
        this.add(pauseContinue);
        this.add(maneuver);
        this.add(setBehavior);
        this.add(retreat);
        this.add(priorityTarget);
        this.add(ignoreTarget);
        this.add(miscButton);
        this.add(new JLabel());
        miscButton.setEnabled(false);
        if (controller != null) {
            controller.registerCommandAction(KeyCommandBind.UNPAUSE.cmd, this::pauseUnpause);
            controller.registerCommandAction(KeyCommandBind.PAUSE.cmd, this::pauseUnpause);
        }
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (e.getOldPhase() == GamePhase.LOUNGE) {
                    retreat.setEnabled(true);
                    pauseContinue.setEnabled(true);
                    maneuver.setEnabled(true);
                    priorityTarget.setEnabled(true);
                    ignoreTarget.setEnabled(true);
                    setBehavior.setEnabled(true);
                } else if (client.getGame().getPhase() == GamePhase.LOUNGE) {
                    retreat.setEnabled(false);
                    pauseContinue.setEnabled(false);
                    maneuver.setEnabled(false);
                    priorityTarget.setEnabled(false);
                    ignoreTarget.setEnabled(false);
                    setBehavior.setEnabled(false);
                }
            }
        });
    }

    /**
     * Registers the space key to pause and unpause the game.
     */
    public void useSpaceForPauseUnpause() {
        controller.registerCommandAction(KeyCommandBind.CENTER_ON_SELECTED.cmd, this::pauseUnpause);
    }

    /**
     * Allows for a customizable button, with a title, tooltip, and action listener.
     * It can do whatever you need it to do, so you can have it in different GUIs and environments.
     *
     * @param miscButtonText           localized text of the button
     * @param miscButtonTooltip        localized tooltip text of the button
     * @param miscButtonActionListener action listener for the button
     */
    public void setMiscButton(String miscButtonText, String miscButtonTooltip, ActionListener miscButtonActionListener) {
        this.clearMiscButton();
        this.miscButton.setText(miscButtonText);
        this.miscButton.setToolTipText(miscButtonTooltip);
        this.miscButton.addActionListener(miscButtonActionListener);
        this.miscButton.setEnabled(true);
    }

    /**
     * Sets the misc button to send a chat command to request victory.
     */
    public void setMiscButtonAsRequestVictory() {
        setMiscButton(
            Messages.getString("BotCommandPanel.Victory.title"),
            Messages.getString("BotCommandPanel.Victory.tooltip"),
            evt -> {
                client.sendChat("/victory");
            });
    }

    /**
     * Clears the misc button, removing any text, tooltip, and action listener and disabling it.
     */
    public void clearMiscButton() {
        this.miscButton.setText("");
        this.miscButton.setToolTipText("");
        var actionListeners = miscButton.getActionListeners();
        for (var actionListener : actionListeners) {
            miscButton.removeActionListener(actionListener);
        }
        this.miscButton.setEnabled(false);
    }

    private Collection<Player> getBotPlayersUnderYourCommand() {
        if (client.getLocalPlayer().isGameMaster()) {
            return client.getGame().getPlayersList().stream().filter(Player::isBot).toList();
        }
        return client.getGame().getPlayersList().stream().filter(p -> p.isBot() && !p.isEnemyOf(client.getLocalPlayer())).toList();
    }

    private JButton createButton(String messageKey) {
        JButton button = new JButton(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        button.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        return button;
    }

    private JPopupMenu createSelectBehaviorPopup() {
        JPopupMenu popup = new JPopupMenu();
        var behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();

        for (var behaviorName : behaviorSettingsFactory.getBehaviorNameList()) {
            createMenuItemForSetBehavior(popup, behaviorName);
        }
        MenuScroller.setScrollerFor(popup, 15);
        return popup;
    }

    private JPopupMenu createRetreatPopup() {
        JPopupMenu popup = new JPopupMenu();
        createMenuItem(popup, CardinalEdge.NORTH, this::retreatNorth);
        createMenuItem(popup, CardinalEdge.EAST, this::retreatEast);
        createMenuItem(popup, CardinalEdge.SOUTH, this::retreatSouth);
        createMenuItem(popup, CardinalEdge.WEST, this::retreatWest);
        createMenuItem(popup, CardinalEdge.NEAREST, this::retreatNearestEdge);
        createMenuItem(popup, CardinalEdge.NONE, this::noRetreat);
        return popup;
    }

    private JPopupMenu createManeuverPopup() {
        JPopupMenu popup = new JPopupMenu();
        createMenuItem(popup, "AlphaStrike", this::alphaStrikeManeuver);
        createMenuItem(popup, "NoPrisoners", this::noPrisonersManeuver);
        createMenuItem(popup, "StayAtRange", this::stayAtRangeManeuver);
        createMenuItem(popup, "Disperse", this::disperseManeuver);
        createMenuItem(popup, "TightFormation", this::tightFormationManeuver);
        createMenuItem(popup, "LooseFormation", this::looseFormationManeuver);
        createMenuItem(popup, "HoldTheLine", this::holdTheLineManeuver);
        createMenuItem(popup, "DoubleTimeMarch", this::doubleTimeMarchManeuver);
        createMenuItem(popup, "FinalGlory", this::finalGloryManeuver);
        createMenuItem(popup, "FallBack", this::fallBackManeuver);
        createMenuItem(popup, "EvasiveAction", this::evasiveActionManeuver);
        createMenuItem(popup, "CarefulAim", this::carefulAimManeuver);
        return popup;
    }

    private record ActionPlayerMenu(Consumer<Player> action, Player botPlayer, JMenu menu) {
    }

    private void createMenuItem(JPopupMenu popup, String commandName, Consumer<Player> action) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + commandName + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + commandName + ".tooltip"));
        // Set a sub menu where you select which bot will receive the order
        getBotPlayersUnderYourCommand().stream()
            .map(botPlayer -> new ActionPlayerMenu(action, botPlayer, menu))
            .forEach(this::createMenuItemForBot);
        popup.add(menu);
    }

    private void createMenuItem(JPopupMenu popup, CardinalEdge cardinalEdge, Consumer<Player> action) {
        JMenu menu = new JMenu(cardinalEdge.toString());
        // Set a sub menu where you select which bot will receive the order
        getBotPlayersUnderYourCommand().stream()
            .map(botPlayer -> new ActionPlayerMenu(action, botPlayer, menu))
            .forEach(this::createMenuItemForBot);
        popup.add(menu);
    }

    private void createMenuItemForSetBehavior(JPopupMenu popup, String behaviorName) {
        JMenu menu = new JMenu(behaviorName);
        // Set a sub menu where you select which bot will receive the order
        getBotPlayersUnderYourCommand().stream()
            .map(botPlayer -> new ActionPlayerMenu(player -> setBehavior(new PlayerBehavior(player, behaviorName)), botPlayer, menu))
            .forEach(this::createMenuItemForBot);
        popup.add(menu);
    }

    private void createMenuItemForBot(ActionPlayerMenu actionPlayerMenu) {
        var menuItem = new JMenuItem(actionPlayerMenu.botPlayer.getName());
        menuItem.addActionListener(evt -> actionPlayerMenu.action.accept(actionPlayerMenu.botPlayer));
        actionPlayerMenu.menu.add(menuItem);
    }

    private void carefulAimManeuver(Player botPlayer) {
        setAvoid(botPlayer, 6);
        setBravery(botPlayer, 8);
        setHyperAggression(botPlayer, 3);
        setFallShame(botPlayer, 7);
    }

    private void evasiveActionManeuver(Player botPlayer) {
        setAvoid(botPlayer, 10);
        setBravery(botPlayer, 3);
        setHyperAggression(botPlayer, 6);
        setFallShame(botPlayer, 2);
    }

    private void fallBackManeuver(Player botPlayer) {
        setAvoid(botPlayer, 8);
        setBravery(botPlayer, 3);
        setHyperAggression(botPlayer, 3);
        setFallShame(botPlayer, 7);
    }

    private void finalGloryManeuver(Player botPlayer) {
        setAvoid(botPlayer, 0);
        setBravery(botPlayer, 10);
        setHyperAggression(botPlayer, 10);
        setFallShame(botPlayer, 6);
    }

    private void doubleTimeMarchManeuver(Player botPlayer) {
        setAvoid(botPlayer, 2);
        setFallShame(botPlayer, 2);
    }

    private void holdTheLineManeuver(Player botPlayer) {
        setAvoid(botPlayer, 2);
        setBravery(botPlayer, 4);
        setHyperAggression(botPlayer, 10);
        setFallShame(botPlayer, 7);
    }

    private void looseFormationManeuver(Player botPlayer) {
        setHerdMentality(botPlayer, 3);
    }

    private void tightFormationManeuver(Player botPlayer) {
        setHerdMentality(botPlayer, 9);
    }

    private void stayAtRangeManeuver(Player botPlayer) {
        setBravery(botPlayer, 8);
        setHyperAggression(botPlayer, 3);
        setAvoid(botPlayer, 7);
    }

    private void disperseManeuver(Player botPlayer) {
        setHerdMentality(botPlayer, 0);
    }

    private void noPrisonersManeuver(Player botPlayer) {
        clearIgnoredTargets(botPlayer);
        bloodFeudAgainstAllEnemies(botPlayer);
    }

    private void alphaStrikeManeuver(Player botPlayer) {
        setBravery(botPlayer, 10);
        setHyperAggression(botPlayer, 10);
        setAvoid(botPlayer, 5);
    }

    private void setHerdMentality(Player botPlayer, int value) {
        sendChatCommand(botPlayer, ChatCommands.HERDING, value);
    }

    private void setBravery(Player botPlayer, int value) {
        sendChatCommand(botPlayer, ChatCommands.BRAVERY, value);
    }

    private void setHyperAggression(Player botPlayer, int value) {
        sendChatCommand(botPlayer, ChatCommands.AGGRESSION, value);
    }

    private void setAvoid(Player botPlayer, int value) {
        sendChatCommand(botPlayer, ChatCommands.AVOID, value);
    }

    private void setFallShame(Player botPlayer, int value) {
        sendChatCommand(botPlayer, ChatCommands.CAUTION, value);
    }

    private record PlayerBehavior(Player player, String behavior) {}

    private void setBehavior(PlayerBehavior playerBehavior) {
        setBehavior(playerBehavior.player, playerBehavior.behavior);
    }

    private void setBehavior(Player botPlayer, String behavior) {
        sendChatCommand(botPlayer, ChatCommands.BEHAVIOR, behavior);
    }

    private void bloodFeudAgainstAllEnemies(Player botPlayer) {
        for (var player : client.getGame().getPlayersList()) {
            if (player.isEnemyOf(botPlayer)) {
                sendChatCommand(botPlayer, ChatCommands.BLOOD_FEUD, player.getId());
            }
        }
    }

    private void clearIgnoredTargets(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.CLEAR_IGNORED_TARGETS);
    }

    private JPopupMenu createPriorityTargetPopup() {
        var popup = entitySelectionMenu("PriorityTargetMenu", this::setPriorityTarget);
        MenuScroller.setScrollerFor(popup, 15);
        return popup;
    }

    private JPopupMenu createIgnoreTargetPopup() {
        var popup = entitySelectionMenu("IgnoreTargetMenu", this::setIgnoreTarget);
        MenuScroller.setScrollerFor(popup, 15);
        return popup;
    }

    private record PlayerInGameObject(Player player, InGameObject inGameObject) {}

    private JPopupMenu entitySelectionMenu(String messageKey, Consumer<PlayerInGameObject> action) {
        JPopupMenu popup = new JPopupMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        popup.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        var playerMap = new HashMap<Integer, Player>();
        client.getGame().getPlayersList().forEach(p -> playerMap.put(p.getId(), p));
        for (var inGameObject : client.getInGameObjects()) {
            var isEnemyOfSomeone = false;
            var unit = new JMenu("ID:" + inGameObject.getId() + " " + inGameObject.generalName());
            for (var botPlayer : getBotPlayersUnderYourCommand()) {
                var player = playerMap.get(inGameObject.getOwnerId());
                if (player != null && player.isEnemyOf(botPlayer)) {
                    var botEntry = new JMenuItem(botPlayer.getName());
                    botEntry.addActionListener(e -> action.accept(new PlayerInGameObject(botPlayer, inGameObject)));
                    unit.add(botEntry);
                    isEnemyOfSomeone = true;
                }
            }
            if (isEnemyOfSomeone) {
                popup.add(unit);
            }
        }
        return popup;
    }

    private void setIgnoreTarget(PlayerInGameObject playerInGameObject) {
        client.sendChat(playerInGameObject.player().getName() + ": " + ChatCommands.IGNORE_TARGET.getAbbreviation() + " : " + playerInGameObject.inGameObject().getId());
    }

    private void setPriorityTarget(PlayerInGameObject playerInGameObject) {
        client.sendChat(playerInGameObject.player().getName() + ": " + ChatCommands.PRIORITIZE.getAbbreviation() + " : " + playerInGameObject.inGameObject().getId());
    }

    private void retreatNorth(Player botPlayer) {
        retreatTowards(botPlayer, CardinalEdge.NORTH);
    }

    private void retreatEast(Player botPlayer) {
        retreatTowards(botPlayer, CardinalEdge.EAST);
    }

    private void retreatWest(Player botPlayer) {
        retreatTowards(botPlayer, CardinalEdge.WEST);
    }

    private void retreatSouth(Player botPlayer) {
        retreatTowards(botPlayer, CardinalEdge.SOUTH);
    }

    private void retreatNearestEdge(Player botPlayer) {
        retreatTowards(botPlayer, CardinalEdge.NEAREST);
    }

    private void noRetreat(Player botPlayer) {
        retreatTowards(botPlayer, CardinalEdge.NONE);
    }

    private void retreatTowards(Player botPlayer, CardinalEdge cardinalEdge) {
        sendChatCommand(botPlayer, ChatCommands.FLEE, cardinalEdge.getIndex());
    }

    private void pauseUnpause() {
        if (audioService != null) {
            audioService.playSound(SoundType.BING_OTHERS_TURN);
        }
        if (pauseLatch) {
            client.sendUnpause();
            pauseContinue.setText(Messages.getString("BotCommandPanel.PauseGame.title"));
        } else if (canBePaused()) {
            client.sendPause();
            pauseContinue.setText(Messages.getString("BotCommandPanel.ContinueGame.title"));
        } else {
            return;
        }
        pauseLatch = !pauseLatch;
    }

    private boolean canBePaused() {
        var game = client.getGame();
        List<Player> nonBots = game.getPlayersList().stream().filter(p -> !p.isBot()).toList();
        boolean liveUnitsRemaining = nonBots.stream().anyMatch(p -> game.getEntitiesOwnedBy(p) > 0);
        return  !liveUnitsRemaining;
    }

    @SuppressWarnings("SameParameterValue")
    private void sendChatCommand(Player botPlayer, ChatCommands chatCommand) {
        sendChatCommand(botPlayer, chatCommand, null);
    }

    private void sendChatCommand(Player botPlayer, ChatCommands chatCommand, int value) {
        sendChatCommand(botPlayer, chatCommand, value + "");
    }

    private void sendChatCommand(Player botPlayer, ChatCommands chatCommand, String value) {
        client.sendChat(botPlayer.getName() + ": " + chatCommand.getAbbreviation() + ((value != null) ? " : " + value : ""));
    }
}
