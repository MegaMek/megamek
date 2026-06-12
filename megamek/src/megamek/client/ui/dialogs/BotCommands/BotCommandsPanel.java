/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.BotCommands;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import megamek.client.AbstractClient;
import megamek.client.bot.princess.ArtilleryCommandAndControl.ArtilleryOrder;
import megamek.client.bot.princess.ArtilleryCommandAndControl.SpecialAmmo;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.audio.AudioService;
import megamek.client.ui.clientGUI.audio.SoundType;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.util.MenuScroller;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.game.InGameObject;

/**
 * The Bot Commands Panel contains a small set of buttons which allow the player to change the configuration of any bot
 * they have control over during game play. It also allows for some orders to be given to the bots, like telling them to
 * ignore a target, change priority over another.
 *
 * @author Luana Coppio
 */
public class BotCommandsPanel extends JPanel {
    private final AbstractClient client;
    private final AudioService audioService;
    private final MegaMekController controller;
    private final ClientGUI clientGUI;
    private final JButton miscButton = new JButton();
    // This latch is used only to change the state of the button from pause to continue and back
    private boolean pauseLatch = false;
    private JButton pauseContinue;
    private List<JButton> commandButtons = List.of();

    /**
     * Bot Commands Panel constructor.
     *
     * @param client       reference to the client instance
     * @param audioService reference to the instance of the AudioService
     * @param controller   reference to the MegaMekController for key binds, or null when key binds are not used
     */
    public BotCommandsPanel(AbstractClient client, @Nullable AudioService audioService,
          @Nullable MegaMekController controller) {
        this(client, audioService, controller, null);
    }

    /**
     * Bot Commands Panel constructor.
     *
     * @param client       reference to the client instance
     * @param audioService reference to the instance of the AudioService
     * @param controller   reference to the MegaMekController for key binds, or null when key binds are not used
     * @param clientGUI    reference to the ClientGUI for toast notifications, or null when toasts are unavailable
     */
    public BotCommandsPanel(AbstractClient client, @Nullable AudioService audioService,
          @Nullable MegaMekController controller, @Nullable ClientGUI clientGUI) {
        this.client = client;
        this.audioService = audioService;
        this.controller = controller;
        this.clientGUI = clientGUI;

        this.initialize();
    }

    /**
     * Shows a toast notification acknowledging that an order was sent to a bot. Falls back to doing nothing when no
     * ClientGUI is available (e.g. in the Commander GUI).
     *
     * @param botPlayer The bot the order was sent to
     * @param orderText The human-readable description of the order
     */
    private void acknowledgeOrder(Player botPlayer, String orderText) {
        if (clientGUI != null) {
            clientGUI.addToast(ToastLevel.SUCCESS,
                  Messages.getString("BotCommandPanel.toast.orderSent", botPlayer.getName(), orderText));
        }
    }

    private void initialize() {
        this.setLayout(new GridLayout(2, 4, 2, 2));
        this.setMinimumSize(new Dimension(750, 80));
        this.setPreferredSize(new Dimension(-1, 80));
        this.setMaximumSize(new Dimension(-1, 80));
        JButton retreat = createButton("Retreat");
        pauseContinue = createButton("PauseGame");
        JButton maneuver = createButton("Maneuver");
        JButton priorityTarget = createButton("PriorityTarget");
        JButton ignoreTarget = createButton("IgnoreTarget");
        JButton setBehavior = createButton("SetBehavior");
        JButton artillery = createButton("Artillery");
        JButton waypoints = createButton("Waypoints");
        commandButtons = List.of(retreat, maneuver, priorityTarget, ignoreTarget, setBehavior, artillery,
              waypoints);

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
        artillery.addActionListener(e -> {
            JPopupMenu popup = createArtilleryPopup();
            popup.show(artillery, 0, artillery.getHeight());
        });
        waypoints.addActionListener(e -> {
            JPopupMenu popup = createWaypointsPopup();
            popup.show(waypoints, 0, waypoints.getHeight());
        });
        pauseContinue.addActionListener(e -> pauseUnpause());

        // Add them to the buttonPanel. With 2 rows set, the grid grows columns as needed.
        this.add(pauseContinue);
        this.add(maneuver);
        this.add(setBehavior);
        this.add(retreat);
        this.add(artillery);
        this.add(priorityTarget);
        this.add(ignoreTarget);
        this.add(waypoints);
        this.add(miscButton);
        // the misc button only becomes visible once a caller configures it (e.g. as Request Victory)
        miscButton.setEnabled(false);
        miscButton.setVisible(false);
        if (controller != null) {
            controller.registerCommandAction(KeyCommandBind.UNPAUSE.cmd, this::pauseUnpause);
            controller.registerCommandAction(KeyCommandBind.PAUSE.cmd, this::pauseUnpause);
        }
        updateButtonStates();
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                updateButtonStates();
            }
        });
    }

    /**
     * Updates the enabled state of all command buttons. Commands cannot be issued in the lobby, and pausing is only
     * possible in games where no human player owns units (e.g. when watching bot-only games), so the pause button is
     * disabled with an explanatory tooltip otherwise.
     */
    private void updateButtonStates() {
        boolean inGame = client.getGame().getPhase() != GamePhase.LOUNGE;
        for (JButton commandButton : commandButtons) {
            commandButton.setEnabled(inGame);
        }
        boolean pauseAllowed = canBePaused();
        pauseContinue.setEnabled(inGame && pauseAllowed);
        pauseContinue.setToolTipText(Messages.getString(pauseAllowed
              ? "BotCommandPanel.PauseGame.tooltip"
              : "BotCommandPanel.PauseGame.unavailable.tooltip"));
    }

    /**
     * Registers the space key to pause and unpause the game.
     */
    public void useSpaceForPauseUnpause() {
        controller.registerCommandAction(KeyCommandBind.CENTER_ON_SELECTED.cmd, this::pauseUnpause);
    }

    /**
     * Allows for a customizable button, with a title, tooltip, and action listener. It can do whatever you need it to
     * do, so you can have it in different GUIs and environments.
     *
     * @param miscButtonText           localized text of the button
     * @param miscButtonTooltip        localized tooltip text of the button
     * @param miscButtonActionListener action listener for the button
     */
    public void setMiscButton(String miscButtonText, String miscButtonTooltip,
          ActionListener miscButtonActionListener) {
        this.clearMiscButton();
        this.miscButton.setText(miscButtonText);
        this.miscButton.setToolTipText(miscButtonTooltip);
        this.miscButton.addActionListener(miscButtonActionListener);
        this.miscButton.setEnabled(true);
        this.miscButton.setVisible(true);
    }

    /**
     * Sets the misc button to send a chat command to request victory.
     */
    public void setMiscButtonAsRequestVictory() {
        setMiscButton(
              Messages.getString("BotCommandPanel.Victory.title"),
              Messages.getString("BotCommandPanel.Victory.tooltip"),
              evt -> client.sendChat("/victory"));
    }

    /**
     * Clears the misc button, removing any text, tooltip, and action listener and disabling it.
     */
    public void clearMiscButton() {
        this.miscButton.setText("");
        this.miscButton.setToolTipText("");
        ActionListener[] actionListeners = miscButton.getActionListeners();
        for (ActionListener actionListener : actionListeners) {
            miscButton.removeActionListener(actionListener);
        }
        this.miscButton.setEnabled(false);
        this.miscButton.setVisible(false);
    }

    private Collection<Player> getBotPlayersUnderYourCommand() {
        if (client.getLocalPlayer().isGameMaster()) {
            return client.getGame().getPlayersList().stream().filter(Player::isBot).toList();
        }
        return client.getGame()
              .getPlayersList()
              .stream()
              .filter(p -> p.isBot() && !p.isEnemyOf(client.getLocalPlayer()))
              .toList();
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
        popup.addSeparator();
        createMenuItem(popup, "ShowStatus", this::requestStatus);
        MenuScroller.setScrollerFor(popup, 15);
        return popup;
    }

    /**
     * Asks the given bot to report its current behavior settings and dishonored players. The bot replies in the chat
     * window.
     *
     * @param botPlayer The bot to query
     */
    private void requestStatus(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.SHOW_BEHAVIOR);
        sendChatCommand(botPlayer, ChatCommands.SHOW_DISHONORED);
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
        popup.addSeparator();
        popup.add(createFineTuneMenu());
        return popup;
    }

    /**
     * Creates the fine-tuning menu, which exposes the five behavior dials the maneuver presets are built from so each
     * can be set directly per bot.
     *
     * @return The created menu
     */
    private JMenu createFineTuneMenu() {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.FineTune.title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel.FineTune.tooltip"));
        menu.add(createBehaviorDialMenu("Bot.commands.caution", ChatCommands.CAUTION));
        menu.add(createBehaviorDialMenu("Bot.commands.avoid", ChatCommands.AVOID));
        menu.add(createBehaviorDialMenu("Bot.commands.aggression", ChatCommands.AGGRESSION));
        menu.add(createBehaviorDialMenu("Bot.commands.herding", ChatCommands.HERDING));
        menu.add(createBehaviorDialMenu("Bot.commands.bravery", ChatCommands.BRAVERY));
        return menu;
    }

    /**
     * Creates a menu for one behavior dial: pick a bot, then a value from 0 to 10.
     *
     * @param labelKey    The resource key for the dial's display name
     * @param dialCommand The behavior index chat command to send
     *
     * @return The created menu
     */
    private JMenu createBehaviorDialMenu(String labelKey, ChatCommands dialCommand) {
        JMenu menu = new JMenu(Messages.getString(labelKey));
        for (Player botPlayer : getBotPlayersUnderYourCommand()) {
            JMenu botMenu = new JMenu(botPlayer.getName());
            for (int value = 0; value <= 10; value++) {
                final int dialValue = value;
                JMenuItem valueItem = new JMenuItem(String.valueOf(dialValue));
                valueItem.addActionListener(evt -> {
                    sendChatCommand(botPlayer, dialCommand, dialValue);
                    acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.fineTune",
                          Messages.getString(labelKey), dialValue));
                });
                botMenu.add(valueItem);
            }
            menu.add(botMenu);
        }
        return menu;
    }

    private record ActionPlayerMenu(Consumer<Player> action, Player botPlayer, JMenu menu) {
    }

    private void createMenuItem(JPopupMenu popup, String commandName, Consumer<Player> action) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + commandName + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + commandName + ".tooltip"));
        Consumer<Player> acknowledgedAction = action.andThen(botPlayer ->
              acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel." + commandName + ".title")));
        // Set a sub menu where you select which bot will receive the order
        getBotPlayersUnderYourCommand().stream()
              .map(botPlayer -> new ActionPlayerMenu(acknowledgedAction, botPlayer, menu))
              .forEach(this::createMenuItemForBot);
        popup.add(menu);
    }

    private void createMenuItem(JPopupMenu popup, CardinalEdge cardinalEdge, Consumer<Player> action) {
        JMenu menu = new JMenu(cardinalEdge.toString());
        Consumer<Player> acknowledgedAction = action.andThen(botPlayer ->
              acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.retreat", cardinalEdge)));
        // Set a sub menu where you select which bot will receive the order
        getBotPlayersUnderYourCommand().stream()
              .map(botPlayer -> new ActionPlayerMenu(acknowledgedAction, botPlayer, menu))
              .forEach(this::createMenuItemForBot);
        popup.add(menu);
    }

    private void createMenuItemForSetBehavior(JPopupMenu popup, String behaviorName) {
        JMenu menu = new JMenu(behaviorName);
        // Set a sub menu where you select which bot will receive the order
        getBotPlayersUnderYourCommand().stream()
              .map(botPlayer -> new ActionPlayerMenu(player -> {
                  setBehavior(new PlayerBehavior(player, behaviorName));
                  acknowledgeOrder(player,
                        Messages.getString("BotCommandPanel.toast.behavior", behaviorName));
              },
                    botPlayer,
                    menu))
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
        popup.addSeparator();
        createStrategicTargetMenu(popup);
        createEnemyPlayerOrderMenu(popup, "BloodFeud", ChatCommands.BLOOD_FEUD,
              "BotCommandPanel.toast.bloodFeud");
        MenuScroller.setScrollerFor(popup, 15);
        return popup;
    }

    /**
     * Adds a menu listing the bots; selecting one prompts for strategic target hexes and orders the bot to attack them
     * (buildings or hexes, used by both regular weapons and artillery).
     *
     * @param popup The popup menu to add the menu to
     */
    private void createStrategicTargetMenu(JPopupMenu popup) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.StrategicTarget.title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel.StrategicTarget.tooltip"));
        for (Player botPlayer : getBotPlayersUnderYourCommand()) {
            JMenuItem botEntry = new JMenuItem(botPlayer.getName());
            botEntry.addActionListener(e -> promptAndSendStrategicTargets(botPlayer));
            menu.add(botEntry);
        }
        popup.add(menu);
    }

    /**
     * Prompts the player for strategic target hexes and sends one target-ground order per hex to the given bot.
     *
     * @param botPlayer The bot to receive the orders
     */
    private void promptAndSendStrategicTargets(Player botPlayer) {
        pickTargetHexes(Messages.getString("BotCommandPanel.StrategicTarget.title"), false,
              "BotCommandPanel.StrategicTargetPrompt.message",
              targets -> {
                  // the target-ground command takes a single hex, so send one command per hex
                  for (String hexNumber : targets.split("-")) {
                      sendChatCommand(botPlayer, ChatCommands.TARGET, hexNumber);
                  }
                  acknowledgeOrder(botPlayer,
                        Messages.getString("BotCommandPanel.toast.strategicTarget", targets));
              });
    }

    private JPopupMenu createIgnoreTargetPopup() {
        var popup = entitySelectionMenu("IgnoreTargetMenu", this::setIgnoreTarget);
        popup.addSeparator();
        createEnemyPlayerOrderMenu(popup, "IgnorePlayer", ChatCommands.IGNORE_PLAYER,
              "BotCommandPanel.toast.ignorePlayer");
        createMenuItem(popup, "IgnoreTurrets", this::ignoreTurrets);
        createMenuItem(popup, "ClearIgnoredTargets", this::clearIgnoredTargetsOrder);
        MenuScroller.setScrollerFor(popup, 15);
        return popup;
    }

    /**
     * Adds a menu listing each enemy player; selecting one sends the given chat command with that player's ID to the
     * chosen bot (e.g. ignore-player or blood-feud).
     *
     * @param popup      The popup menu to add the menu to
     * @param messageKey The resource key for the menu title and tooltip
     * @param command    The chat command to send with the enemy player's ID
     * @param toastKey   The resource key for the confirmation toast (formatted with the enemy player's name)
     */
    private void createEnemyPlayerOrderMenu(JPopupMenu popup, String messageKey, ChatCommands command,
          String toastKey) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        for (Player enemyPlayer : client.getGame().getPlayersList()) {
            JMenu playerMenu = new JMenu(enemyPlayer.getName());
            for (Player botPlayer : getBotPlayersUnderYourCommand()) {
                if (!enemyPlayer.isEnemyOf(botPlayer)) {
                    continue;
                }
                JMenuItem botEntry = new JMenuItem(botPlayer.getName());
                botEntry.addActionListener(e -> {
                    sendChatCommand(botPlayer, command, String.valueOf(enemyPlayer.getId()));
                    acknowledgeOrder(botPlayer, Messages.getString(toastKey, enemyPlayer.getName()));
                });
                playerMenu.add(botEntry);
            }
            if (playerMenu.getItemCount() > 0) {
                menu.add(playerMenu);
            }
        }
        popup.add(menu);
    }

    private void ignoreTurrets(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.IGNORE_TURRETS);
    }

    private void clearIgnoredTargetsOrder(Player botPlayer) {
        clearIgnoredTargets(botPlayer);
    }

    private JPopupMenu createArtilleryPopup() {
        JPopupMenu popup = new JPopupMenu();
        createMenuItem(popup, "ArtilleryHalt",
              botPlayer -> sendArtilleryOrder(botPlayer, ArtilleryOrder.HALT, SpecialAmmo.STANDARD, ""));
        createMenuItem(popup, "ArtilleryAuto",
              botPlayer -> sendArtilleryOrder(botPlayer, ArtilleryOrder.AUTO, SpecialAmmo.STANDARD, ""));
        popup.addSeparator();
        popup.add(createArtilleryFireMissionMenu("ArtilleryBarrage", ArtilleryOrder.BARRAGE));
        popup.add(createArtilleryFireMissionMenu("ArtilleryVolley", ArtilleryOrder.VOLLEY));
        popup.add(createArtilleryFireMissionMenu("ArtillerySingle", ArtilleryOrder.SINGLE));
        return popup;
    }

    /**
     * Creates a fire mission menu for the given artillery order: the player picks a bot and an ammo type, then is
     * prompted for the target hexes.
     *
     * @param messageKey The resource key for the menu title and tooltip
     * @param order      The artillery order this menu issues
     *
     * @return The created menu
     */
    private JMenu createArtilleryFireMissionMenu(String messageKey, ArtilleryOrder order) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        for (Player botPlayer : getBotPlayersUnderYourCommand()) {
            JMenu botMenu = new JMenu(botPlayer.getName());
            for (SpecialAmmo ammo : SpecialAmmo.values()) {
                JMenuItem ammoItem = new JMenuItem(
                      Messages.getString("BotCommandPanel.ArtilleryAmmo." + ammo.name()));
                ammoItem.addActionListener(evt -> promptAndSendFireMission(botPlayer, order, ammo));
                botMenu.add(ammoItem);
            }
            menu.add(botMenu);
        }
        return menu;
    }

    /**
     * Lets the player pick fire mission target hexes by clicking the board, then sends the artillery order. Falls
     * back to a typed hex number prompt when no board view is available.
     *
     * @param botPlayer The bot to receive the order
     * @param order     The artillery order to issue
     * @param ammo      The special ammo to use
     */
    private void promptAndSendFireMission(Player botPlayer, ArtilleryOrder order, SpecialAmmo ammo) {
        pickTargetHexes(Messages.getString("BotCommandPanel.HexPicker.artilleryOrder",
                    order.name(), botPlayer.getName()),
              order == ArtilleryOrder.SINGLE,
              "BotCommandPanel.ArtilleryTargetPrompt.message",
              targets -> sendArtilleryOrder(botPlayer, order, ammo, targets));
    }

    /**
     * Lets the player designate target hexes by clicking them on the board. When no board view is available (e.g. in
     * the Commander GUI), falls back to a typed hex number prompt.
     *
     * @param orderDescription  Human-readable description of the order shown while picking
     * @param singleHex         TRUE to finish after the first hex is picked
     * @param fallbackPromptKey The resource key for the typed prompt message used as fallback
     * @param onTargetsSelected Called with the picked hexes as dash-separated hex numbers
     */
    private void pickTargetHexes(String orderDescription, boolean singleHex, String fallbackPromptKey,
          Consumer<String> onTargetsSelected) {
        BoardView boardView = null;
        if (clientGUI != null) {
            boardView = clientGUI.getCurrentBoardView()
                  .filter(BoardView.class::isInstance)
                  .map(BoardView.class::cast)
                  .orElse(null);
        }
        if (boardView == null) {
            String targets = promptForHexNumbers(fallbackPromptKey, "BotCommandPanel.TargetHexPrompt.title");
            if (targets != null) {
                onTargetsSelected.accept(targets);
            }
            return;
        }
        new HexTargetPicker(clientGUI, boardView, orderDescription, singleHex, onTargetsSelected).start();
    }

    /**
     * Prompts the player for hex numbers, accepting them separated by spaces or commas (e.g. "0810 0811"), and
     * validates them. Shows an error toast on invalid input.
     *
     * @param promptMessageKey The resource key for the prompt message
     * @param promptTitleKey   The resource key for the prompt dialog title
     *
     * @return The entered hex numbers joined with dashes, or null if the player canceled or the input was invalid
     */
    private @Nullable String promptForHexNumbers(String promptMessageKey, String promptTitleKey) {
        String input = JOptionPane.showInputDialog(this,
              Messages.getString(promptMessageKey),
              Messages.getString(promptTitleKey),
              JOptionPane.QUESTION_MESSAGE);
        if ((input == null) || input.isBlank()) {
            return null;
        }
        String targets = String.join("-", input.trim().split("[\\s,]+"));
        if (!isValidHexInput(targets)) {
            if (clientGUI != null) {
                clientGUI.addToast(ToastLevel.ERROR,
                      Messages.getString("BotCommandPanel.ArtilleryTargetPrompt.invalid", input));
            }
            return null;
        }
        return targets;
    }

    /**
     * Checks that every dash-separated hex number in the given target string parses to a valid board coordinate.
     *
     * @param targets Dash-separated hex numbers, e.g. "0810-0811"
     *
     * @return TRUE if all hex numbers are valid
     */
    private boolean isValidHexInput(String targets) {
        try {
            for (String hexNumber : targets.split("-")) {
                Coords coords = Coords.parseHexNumber(hexNumber);
                if ((coords.getX() < 0) || (coords.getY() < 0)) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Sends an artillery order chat command to the given bot and shows a confirmation toast.
     *
     * @param botPlayer The bot to receive the order
     * @param order     The artillery order to issue
     * @param ammo      The special ammo to use
     * @param targets   Dash-separated target hex numbers, or an empty string for orders without targets
     */
    private void sendArtilleryOrder(Player botPlayer, ArtilleryOrder order, SpecialAmmo ammo, String targets) {
        // halt/auto take no ammo or targets, so send just the order to keep the chat echo readable
        String arguments = targets.isBlank()
              ? order.name()
              : order.name() + " " + ammo.name() + " " + targets;
        sendChatCommand(botPlayer, ChatCommands.ARTILLERY, arguments);
        if (!targets.isBlank()) {
            acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.artillery",
                  order.name(), targets));
        }
    }

    private JPopupMenu createWaypointsPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.add(createWaypointHexOrderMenu("SetWaypoints", ChatCommands.SET_WAYPOINT));
        popup.add(createWaypointHexOrderMenu("AddWaypoint", ChatCommands.ADD_WAYPOINT));
        popup.addSeparator();
        popup.add(createWaypointUnitOrderMenu("RemoveWaypoint", ChatCommands.REMOVE_WAYPOINT));
        popup.add(createWaypointUnitOrderMenu("ClearWaypoints", ChatCommands.CLEAR_WAYPOINT));
        createMenuItem(popup, "ClearAllWaypoints", this::clearAllWaypoints);
        return popup;
    }

    private void clearAllWaypoints(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.CLEAR_ALL_WAYPOINTS);
    }

    /**
     * Lists all units owned by the given bot player.
     *
     * @param botPlayer The bot player
     *
     * @return The bot's units
     */
    private List<InGameObject> getUnitsOwnedBy(Player botPlayer) {
        return client.getInGameObjects().stream()
              .filter(unit -> unit.getOwnerId() == botPlayer.getId())
              .toList();
    }

    /**
     * Creates a waypoint menu that needs target hexes: pick a bot, then one of its units, then enter the hexes.
     *
     * @param messageKey      The resource key for the menu title and tooltip
     * @param waypointCommand The waypoint chat command to send (set-waypoints or add-waypoint)
     *
     * @return The created menu
     */
    private JMenu createWaypointHexOrderMenu(String messageKey, ChatCommands waypointCommand) {
        return createWaypointMenu(messageKey, (botPlayer, unit) ->
              promptAndSendWaypoints(botPlayer, unit, waypointCommand,
                    Messages.getString("BotCommandPanel." + messageKey + ".title")));
    }

    /**
     * Creates a waypoint menu that acts on a unit without needing hexes (remove last / clear waypoints).
     *
     * @param messageKey      The resource key for the menu title and tooltip
     * @param waypointCommand The waypoint chat command to send (remove-waypoint or clear-waypoints)
     *
     * @return The created menu
     */
    private JMenu createWaypointUnitOrderMenu(String messageKey, ChatCommands waypointCommand) {
        return createWaypointMenu(messageKey, (botPlayer, unit) -> {
            sendChatCommand(botPlayer, waypointCommand, String.valueOf(unit.getId()));
            acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.unitOrder",
                  Messages.getString("BotCommandPanel." + messageKey + ".title"),
                  unit.getId(), unit.generalName()));
        });
    }

    /**
     * Creates a menu listing each bot and its units; selecting a unit runs the given action.
     *
     * @param messageKey The resource key for the menu title and tooltip
     * @param unitAction The action to run with the chosen bot and unit
     *
     * @return The created menu
     */
    private JMenu createWaypointMenu(String messageKey, BiConsumer<Player, InGameObject> unitAction) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        for (Player botPlayer : getBotPlayersUnderYourCommand()) {
            JMenu botMenu = new JMenu(botPlayer.getName());
            for (InGameObject unit : getUnitsOwnedBy(botPlayer)) {
                JMenuItem unitItem = new JMenuItem("ID:" + unit.getId() + " " + unit.generalName());
                unitItem.addActionListener(evt -> unitAction.accept(botPlayer, unit));
                botMenu.add(unitItem);
            }
            if (botMenu.getItemCount() > 0) {
                menu.add(botMenu);
            }
        }
        return menu;
    }

    /**
     * Prompts the player for waypoint hexes and sends the given waypoint command for the chosen unit.
     *
     * @param botPlayer       The bot to receive the order
     * @param unit            The unit to set waypoints for
     * @param waypointCommand The waypoint chat command to send
     * @param orderTitle      The human-readable order name for the confirmation toast
     */
    private void promptAndSendWaypoints(Player botPlayer, InGameObject unit, ChatCommands waypointCommand,
          String orderTitle) {
        pickTargetHexes(orderTitle + " - " + unit.generalName(), false,
              "BotCommandPanel.WaypointPrompt.message",
              targets -> {
                  sendChatCommand(botPlayer, waypointCommand, unit.getId() + " " + targets);
                  acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.unitOrder",
                        orderTitle, unit.getId(), unit.generalName()));
              });
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
        sendChatCommand(playerInGameObject.player(), ChatCommands.IGNORE_TARGET,
              String.valueOf(playerInGameObject.inGameObject().getId()));
        acknowledgeOrder(playerInGameObject.player(),
              Messages.getString("BotCommandPanel.toast.unitOrder",
                    Messages.getString("BotCommandPanel.IgnoreTarget.title"),
                    playerInGameObject.inGameObject().getId(),
                    playerInGameObject.inGameObject().generalName()));
    }

    private void setPriorityTarget(PlayerInGameObject playerInGameObject) {
        sendChatCommand(playerInGameObject.player(), ChatCommands.PRIORITIZE,
              String.valueOf(playerInGameObject.inGameObject().getId()));
        acknowledgeOrder(playerInGameObject.player(),
              Messages.getString("BotCommandPanel.toast.unitOrder",
                    Messages.getString("BotCommandPanel.PriorityTarget.title"),
                    playerInGameObject.inGameObject().getId(),
                    playerInGameObject.inGameObject().generalName()));
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
        if (pauseLatch) {
            client.sendUnpause();
            pauseContinue.setText(Messages.getString("BotCommandPanel.PauseGame.title"));
        } else if (canBePaused()) {
            client.sendPause();
            pauseContinue.setText(Messages.getString("BotCommandPanel.ContinueGame.title"));
        } else {
            // pausing is not available in this game (a human player still owns units), do nothing
            return;
        }
        if (audioService != null) {
            audioService.playSound(SoundType.BING_OTHERS_TURN);
        }
        pauseLatch = !pauseLatch;
    }

    private boolean canBePaused() {
        var game = client.getGame();
        List<Player> nonBots = game.getPlayersList().stream().filter(p -> !p.isBot()).toList();
        boolean liveUnitsRemaining = nonBots.stream().anyMatch(p -> game.getEntitiesOwnedBy(p) > 0);
        return !liveUnitsRemaining;
    }

    private void sendChatCommand(Player botPlayer, ChatCommands chatCommand) {
        sendChatCommand(botPlayer, chatCommand, null);
    }

    private void sendChatCommand(Player botPlayer, ChatCommands chatCommand, int value) {
        sendChatCommand(botPlayer, chatCommand, value + "");
    }

    private void sendChatCommand(Player botPlayer, ChatCommands chatCommand, String value) {
        client.sendChat(botPlayer.getName() + ": " + chatCommand.getAbbreviation() + ((value != null) ?
              " : " + value :
              ""));
    }
}
