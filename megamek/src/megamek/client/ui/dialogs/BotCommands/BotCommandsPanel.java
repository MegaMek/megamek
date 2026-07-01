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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

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
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.SkinSpecification;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.game.InGameObject;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * The Bot Commands Panel contains a small set of buttons which allow the player to change the configuration of any bot
 * they have control over during game play. It also allows for some orders to be given to the bots, like telling them to
 * ignore a target, change priority over another.
 *
 * @author Luana Coppio
 */
public class BotCommandsPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(BotCommandsPanel.class);

    private final AbstractClient client;
    private final AudioService audioService;
    private final MegaMekController controller;
    private final ClientGUI clientGUI;
    private final MegaMekButton miscButton =
          new MegaMekButton("", SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
    // This latch is used only to change the state of the button from pause to continue and back
    private boolean pauseLatch = false;
    private MegaMekButton pauseContinue;
    private List<MegaMekButton> commandButtons = List.of();

    /**
     * Bot Commands Panel constructor.
     *
     * @param client       reference to the client instance
     * @param audioService reference to the instance of the AudioService
     * @param controller   reference to the MegaMekController for key binds, or {@code null} when key binds are not used
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
     * @param controller   reference to the MegaMekController for key binds, or {@code null} when key binds are not used
     * @param clientGUI    reference to the ClientGUI for toast notifications, or {@code null} when toasts are unavailable
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

    /**
     * Switches the panel between its floating and docked layouts. The floating layout keeps the original two-row grid
     * and size; the docked layout uses a single row sized for the thin strip above the board. Only the arrangement and
     * size hints change - the same buttons and their actions are preserved in both modes.
     *
     * @param docked {@code true} to use the single-row docked layout, {@code false} to use the floating two-row layout
     */
    public void setDockedLayout(boolean docked) {
        applyLayout(docked);
        revalidate();
        repaint();
    }

    /**
     * Applies the layout manager and size hints for the requested mode.
     *
     * @param docked {@code true} for the single-row docked layout, {@code false} for the floating two-row layout
     */
    private void applyLayout(boolean docked) {
        if (docked) {
            // One row, columns grow to fit the buttons - a thin strip that steals little height from the board.
            this.setLayout(new GridLayout(1, 0, 2, 2));
            this.setMinimumSize(UIUtil.scaleForGUI(600, 40));
            this.setPreferredSize(new Dimension(-1, UIUtil.scaleForGUI(40)));
            this.setMaximumSize(new Dimension(-1, UIUtil.scaleForGUI(40)));
        } else {
            this.setLayout(new GridLayout(2, 4, 2, 2));
            this.setMinimumSize(UIUtil.scaleForGUI(600, 80));
            this.setPreferredSize(new Dimension(-1, UIUtil.scaleForGUI(80)));
            this.setMaximumSize(new Dimension(-1, UIUtil.scaleForGUI(80)));
        }
    }

    private void initialize() {
        applyLayout(false);
        applyTopBarBackground();
        var retreat = createButton("Retreat");
        pauseContinue = createButton("PauseGame");
        var maneuver = createButton("Maneuver");
        var priorityTarget = createButton("PriorityTarget");
        var ignoreTarget = createButton("IgnoreTarget");
        var setBehavior = createButton("SetBehavior");
        var artillery = createButton("Artillery");
        var waypoints = createButton("Waypoints");
        commandButtons = List.of(retreat, maneuver, priorityTarget, ignoreTarget, setBehavior, artillery,
              waypoints);

        maneuver.addActionListener(evt -> showButtonPopup(maneuver, this::createManeuverPopup));
        priorityTarget.addActionListener(evt -> showButtonPopup(priorityTarget, this::createPriorityTargetPopup));
        ignoreTarget.addActionListener(evt -> showButtonPopup(ignoreTarget, this::createIgnoreTargetPopup));
        retreat.addActionListener(evt -> showButtonPopup(retreat, this::createRetreatPopup));
        setBehavior.addActionListener(evt -> showButtonPopup(setBehavior, this::createSelectBehaviorPopup));
        artillery.addActionListener(evt -> showButtonPopup(artillery, this::createArtilleryPopup));
        waypoints.addActionListener(evt -> showButtonPopup(waypoints, this::createWaypointsPopup));
        pauseContinue.addActionListener(evt -> pauseUnpause());

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
        for (MegaMekButton commandButton : commandButtons) {
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

    private MegaMekButton createButton(String messageKey) {
        MegaMekButton button = new MegaMekButton(Messages.getString("BotCommandPanel." + messageKey + ".title"),
              SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
        button.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        return button;
    }

    /**
     * Builds a button's popup and shows it, logging the reason instead of silently doing nothing if the build throws or
     * the popup ends up empty. Popups are built lazily on click, so an exception there is otherwise lost on the AWT
     * event thread and leaves the button looking dead (e.g. clicking it does nothing).
     *
     * @param button       The button the popup is anchored to
     * @param popupFactory Supplies the popup to display
     */
    private void showButtonPopup(MegaMekButton button, Supplier<JPopupMenu> popupFactory) {
        try {
            JPopupMenu popup = popupFactory.get();
            if (popup.getComponentCount() == 0) {
                LOGGER.info("[BotPanel] {} popup has no entries to show (no commandable bots or applicable options)",
                      button.getText());
                return;
            }
            popup.show(button, 0, button.getHeight());
        } catch (Exception exception) {
            LOGGER.error(exception, "[BotPanel] Failed to build the {} popup", button.getText());
        }
    }

    /**
     * Sets the panel background to the top menu bar color so the docked strip blends with the bar above it. Falls back
     * to the generic control color when the theme does not define a menu bar background.
     */
    private void applyTopBarBackground() {
        Color barBackground = UIManager.getColor("MenuBar.background");
        if (barBackground == null) {
            barBackground = UIManager.getColor("control");
        }
        setOpaque(true);
        setBackground(barBackground);
    }

    private JPopupMenu createSelectBehaviorPopup() {
        var behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();
        return createBotFirstPopup((botMenu, botPlayer) -> {
            for (String behaviorName : behaviorSettingsFactory.getBehaviorNameList()) {
                JMenuItem behaviorItem = new JMenuItem(behaviorName);
                behaviorItem.addActionListener(evt -> {
                    setBehavior(new PlayerBehavior(botPlayer, behaviorName));
                    acknowledgeOrder(botPlayer,
                          Messages.getString("BotCommandPanel.toast.behavior", behaviorName));
                });
                botMenu.add(behaviorItem);
            }
            botMenu.addSeparator();
            addBotAction(botMenu, botPlayer, "ShowStatus", this::requestStatus);
        }, 15);
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
        return createBotFirstPopup((botMenu, botPlayer) -> {
            addRetreatAction(botMenu, botPlayer, CardinalEdge.NORTH, this::retreatNorth);
            addRetreatAction(botMenu, botPlayer, CardinalEdge.EAST, this::retreatEast);
            addRetreatAction(botMenu, botPlayer, CardinalEdge.SOUTH, this::retreatSouth);
            addRetreatAction(botMenu, botPlayer, CardinalEdge.WEST, this::retreatWest);
            addRetreatAction(botMenu, botPlayer, CardinalEdge.NEAREST, this::retreatNearestEdge);
            addRetreatAction(botMenu, botPlayer, CardinalEdge.NONE, this::noRetreat);
            botMenu.addSeparator();
            addBotAction(botMenu, botPlayer, "HoldPosition", this::holdPosition);
            addBotAction(botMenu, botPlayer, "ResumeMovement", this::resumeMovement);
            // Shoot-and-scoot only moves on-board artillery; gray it out for a bot whose artillery is all off-board
            // (off-board units cannot move). Evaluated per bot, since a player may command a mix of on- and off-board
            // artillery bots.
            boolean canScoot = hasMovableArtillery(botPlayer);
            JMenuItem shootAndScootItem = addBotAction(botMenu, botPlayer, "ShootAndScoot", this::enableShootAndScoot);
            JMenuItem stopShootAndScootItem = addBotAction(botMenu, botPlayer, "StopShootAndScoot",
                  this::disableShootAndScoot);
            JMenuItem scootToHexItem = new JMenuItem(Messages.getString("BotCommandPanel.ScootToHex.title"));
            scootToHexItem.setToolTipText(Messages.getString("BotCommandPanel.ScootToHex.tooltip"));
            scootToHexItem.addActionListener(evt -> scootToHex(botPlayer));
            botMenu.add(scootToHexItem);
            if (!canScoot) {
                String reason = Messages.getString("BotCommandPanel.ShootAndScoot.noOnBoardArtillery");
                for (JMenuItem item : List.of(shootAndScootItem, stopShootAndScootItem, scootToHexItem)) {
                    item.setEnabled(false);
                    item.setToolTipText(reason);
                }
            }
        }, this::hasOnBoardUnits);
    }

    /**
     * @param botPlayer The bot to inspect
     *
     * @return {@code true} if the bot has at least one deployed on-board unit, i.e. movement commands make sense for it
     */
    private boolean hasOnBoardUnits(Player botPlayer) {
        for (InGameObject unit : getUnitsOwnedBy(botPlayer)) {
            if ((unit instanceof Entity entity) && !entity.isOffBoard()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param botPlayer The bot to inspect
     *
     * @return {@code true} if the bot has at least one on-board artillery unit with usable ammo, i.e. artillery that
     *       shoot-and-scoot can actually move
     */
    private boolean hasMovableArtillery(Player botPlayer) {
        for (InGameObject unit : getUnitsOwnedBy(botPlayer)) {
            if ((unit instanceof Entity entity) && !entity.isOffBoard() && (countReadyArtilleryWeapons(entity) > 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param botPlayer The bot to inspect
     *
     * @return {@code true} if the bot owns at least one unit with an artillery weapon (on or off board), so the
     *       artillery orders menu is worth offering for it - a bot with no artillery is left out of the popup entirely
     */
    private boolean botHasArtillery(Player botPlayer) {
        for (InGameObject unit : getUnitsOwnedBy(botPlayer)) {
            if ((unit instanceof Entity entity) && entity.hasArtillery()) {
                return true;
            }
        }
        return false;
    }

    private void holdPosition(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.HOLD_POSITION);
    }

    private void resumeMovement(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.HOLD_POSITION, "false");
    }

    private void enableShootAndScoot(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.SHOOT_AND_SCOOT);
    }

    private void disableShootAndScoot(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.SHOOT_AND_SCOOT, "false");
    }


    private void scootToHex(Player botPlayer) {
        pickTargetHexes(Messages.getString("BotCommandPanel.ScootToHex.title") + " - " + botPlayer.getName(),
              true, 1,
              "BotCommandPanel.ScootToHexPrompt.message",
              targets -> {
                  sendChatCommand(botPlayer, ChatCommands.SCOOT_TO_HEX, targets);
                  acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.scootToHex", targets));
              });
    }

    private JPopupMenu createManeuverPopup() {
        return createBotFirstPopup((botMenu, botPlayer) -> {
            addBotAction(botMenu, botPlayer, "AlphaStrike", this::alphaStrikeManeuver);
            addBotAction(botMenu, botPlayer, "NoPrisoners", this::noPrisonersManeuver);
            addBotAction(botMenu, botPlayer, "StayAtRange", this::stayAtRangeManeuver);
            addBotAction(botMenu, botPlayer, "Disperse", this::disperseManeuver);
            addBotAction(botMenu, botPlayer, "TightFormation", this::tightFormationManeuver);
            addBotAction(botMenu, botPlayer, "LooseFormation", this::looseFormationManeuver);
            addBotAction(botMenu, botPlayer, "HoldTheLine", this::holdTheLineManeuver);
            addBotAction(botMenu, botPlayer, "DoubleTimeMarch", this::doubleTimeMarchManeuver);
            addBotAction(botMenu, botPlayer, "FinalGlory", this::finalGloryManeuver);
            addBotAction(botMenu, botPlayer, "FallBack", this::fallBackManeuver);
            addBotAction(botMenu, botPlayer, "EvasiveAction", this::evasiveActionManeuver);
            addBotAction(botMenu, botPlayer, "CarefulAim", this::carefulAimManeuver);
            botMenu.addSeparator();
            botMenu.add(createFineTuneMenu(botPlayer));
        });
    }

    /**
     * Creates the fine-tuning menu for one bot, which exposes the five behavior dials the maneuver presets are built
     * from so each can be set directly.
     *
     * @param botPlayer The bot the dials will be applied to
     *
     * @return The created menu
     */
    private JMenu createFineTuneMenu(Player botPlayer) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.FineTune.title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel.FineTune.tooltip"));
        menu.add(createBehaviorDialMenu(botPlayer, "Bot.commands.caution", ChatCommands.CAUTION));
        menu.add(createBehaviorDialMenu(botPlayer, "Bot.commands.avoid", ChatCommands.AVOID));
        menu.add(createBehaviorDialMenu(botPlayer, "Bot.commands.aggression", ChatCommands.AGGRESSION));
        menu.add(createBehaviorDialMenu(botPlayer, "Bot.commands.herding", ChatCommands.HERDING));
        menu.add(createBehaviorDialMenu(botPlayer, "Bot.commands.bravery", ChatCommands.BRAVERY));
        return menu;
    }

    /**
     * Creates a menu for one behavior dial of one bot: a value from 0 to 10.
     *
     * @param botPlayer   The bot the dial will be applied to
     * @param labelKey    The resource key for the dial's display name
     * @param dialCommand The behavior index chat command to send
     *
     * @return The created menu
     */
    private JMenu createBehaviorDialMenu(Player botPlayer, String labelKey, ChatCommands dialCommand) {
        JMenu menu = new JMenu(Messages.getString(labelKey));
        for (int value = 0; value <= 10; value++) {
            final int dialValue = value;
            JMenuItem valueItem = new JMenuItem(String.valueOf(dialValue));
            valueItem.addActionListener(evt -> {
                sendChatCommand(botPlayer, dialCommand, dialValue);
                acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.fineTune",
                      Messages.getString(labelKey), dialValue));
            });
            menu.add(valueItem);
        }
        return menu;
    }

    /**
     * Builds a popup whose top level is one submenu per commandable bot, each populated with the command's actions
     * already bound to that bot. When the player commands exactly one bot, the bot level is skipped and that bot's
     * actions are inlined directly into the popup so no extra navigation is needed.
     *
     * @param populator Fills a given bot's submenu with the command's actions, bound to that bot
     *
     * @return The created popup
     */
    private JPopupMenu createBotFirstPopup(BiConsumer<JMenu, Player> populator) {
        return createBotFirstPopup(populator, 0, botPlayer -> true);
    }

    /**
     * Builds a bot-first popup including only bots that pass the given filter - used to drop bots from menus that do
     * not make sense for them (e.g. excluding fully off-board bots from movement menus).
     *
     * @param populator Fills a given bot's submenu with the command's actions, bound to that bot
     * @param botFilter Only bots passing this test get a submenu
     *
     * @return The created popup
     */
    private JPopupMenu createBotFirstPopup(BiConsumer<JMenu, Player> populator, Predicate<Player> botFilter) {
        return createBotFirstPopup(populator, 0, botFilter);
    }

    /**
     * Builds a bot-first popup (see {@link #createBotFirstPopup(BiConsumer)}) and, when {@code scrollThreshold} is
     * positive, attaches a scroller so long per-bot action lists stay usable.
     *
     * @param populator       Fills a given bot's submenu with the command's actions, bound to that bot
     * @param scrollThreshold The maximum number of visible items before scrolling, or 0 for no scroller
     *
     * @return The created popup
     */
    private JPopupMenu createBotFirstPopup(BiConsumer<JMenu, Player> populator, int scrollThreshold) {
        return createBotFirstPopup(populator, scrollThreshold, botPlayer -> true);
    }

    /**
     * Builds a bot-first popup with both a scroll threshold and a bot filter.
     *
     * @param populator       Fills a given bot's submenu with the command's actions, bound to that bot
     * @param scrollThreshold The maximum number of visible items before scrolling, or 0 for no scroller
     * @param botFilter       Only bots passing this test get a submenu
     *
     * @return The created popup
     */
    private JPopupMenu createBotFirstPopup(BiConsumer<JMenu, Player> populator, int scrollThreshold,
          Predicate<Player> botFilter) {
        JPopupMenu popup = new JPopupMenu();
        List<Player> botPlayers = getBotPlayersUnderYourCommand().stream().filter(botFilter).toList();
        if (botPlayers.size() == 1) {
            // Single-bot shortcut: inline the only bot's actions, dropping the redundant bot submenu level.
            JMenu botMenu = new JMenu();
            populator.accept(botMenu, botPlayers.getFirst());
            for (Component item : botMenu.getMenuComponents()) {
                popup.add(item);
            }
            if (scrollThreshold > 0) {
                MenuScroller.setScrollerFor(popup, scrollThreshold);
            }
        } else {
            for (Player botPlayer : botPlayers) {
                JMenu botMenu = new JMenu(botPlayer.getName());
                populator.accept(botMenu, botPlayer);
                if (botMenu.getItemCount() > 0) {
                    if (scrollThreshold > 0) {
                        MenuScroller.setScrollerFor(botMenu, scrollThreshold);
                    }
                    popup.add(botMenu);
                }
            }
        }
        return popup;
    }

    /**
     * Adds a single action item to a bot's submenu. The item is titled and tooltipped from the command's resource keys
     * and, when clicked, runs the action against the bot and shows a confirmation toast.
     *
     * @param botMenu     The bot submenu to add the item to
     * @param botPlayer   The bot the action will be issued to
     * @param commandName The resource key prefix for the item's title and tooltip
     * @param action      The action to run against the bot
     *
     * @return The created menu item, so callers can adjust it (e.g. disable it)
     */
    private JMenuItem addBotAction(JMenu botMenu, Player botPlayer, String commandName, Consumer<Player> action) {
        JMenuItem menuItem = new JMenuItem(Messages.getString("BotCommandPanel." + commandName + ".title"));
        menuItem.setToolTipText(Messages.getString("BotCommandPanel." + commandName + ".tooltip"));
        menuItem.addActionListener(evt -> {
            action.accept(botPlayer);
            acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel." + commandName + ".title"));
        });
        botMenu.add(menuItem);
        return menuItem;
    }

    /**
     * Adds a retreat-edge action item to a bot's submenu.
     *
     * @param botMenu      The bot submenu to add the item to
     * @param botPlayer    The bot the retreat order will be issued to
     * @param cardinalEdge The edge to retreat toward
     * @param action       The action to run against the bot
     */
    private void addRetreatAction(JMenu botMenu, Player botPlayer, CardinalEdge cardinalEdge,
          Consumer<Player> action) {
        JMenuItem menuItem = new JMenuItem(cardinalEdge.toString());
        menuItem.addActionListener(evt -> {
            action.accept(botPlayer);
            acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.retreat", cardinalEdge));
        });
        botMenu.add(menuItem);
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
        return createBotFirstPopup((botMenu, botPlayer) -> {
            addEnemyUnitMenu(botMenu, botPlayer, "PriorityTargetMenu", this::setPriorityTarget);
            addEnemyUnitMenu(botMenu, botPlayer, "TagTargetMenu", this::setTagTarget);
            addStrategicTargetItem(botMenu, botPlayer);
            addEnemyPlayerMenu(botMenu, botPlayer, "BloodFeud", ChatCommands.BLOOD_FEUD,
                  "BotCommandPanel.toast.bloodFeud");
        });
    }

    private void setTagTarget(PlayerInGameObject playerInGameObject) {
        sendChatCommand(playerInGameObject.player(), ChatCommands.TAG_TARGET,
              String.valueOf(playerInGameObject.inGameObject().getId()));
        acknowledgeOrder(playerInGameObject.player(),
              Messages.getString("BotCommandPanel.toast.unitOrder",
                    Messages.getString("BotCommandPanel.TagTargetMenu.title"),
                    playerInGameObject.inGameObject().getId(),
                    playerInGameObject.inGameObject().generalName()));
    }

    /**
     * Adds an item that, when selected, prompts for strategic target hexes and orders the given bot to attack them
     * (buildings or hexes, used by both regular weapons and artillery).
     *
     * @param botMenu   The bot submenu to add the item to
     * @param botPlayer The bot the order will be issued to
     */
    private void addStrategicTargetItem(JMenu botMenu, Player botPlayer) {
        JMenuItem strategicTargetItem = new JMenuItem(Messages.getString("BotCommandPanel.StrategicTarget.title"));
        strategicTargetItem.setToolTipText(Messages.getString("BotCommandPanel.StrategicTarget.tooltip"));
        strategicTargetItem.addActionListener(evt -> promptAndSendStrategicTargets(botPlayer));
        botMenu.add(strategicTargetItem);
    }

    /**
     * Prompts the player for strategic target hexes and sends one target-ground order per hex to the given bot.
     *
     * @param botPlayer The bot to receive the orders
     */
    private void promptAndSendStrategicTargets(Player botPlayer) {
        pickTargetHexes(Messages.getString("BotCommandPanel.StrategicTarget.title"), false, 0,
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
        return createBotFirstPopup((botMenu, botPlayer) -> {
            addEnemyUnitMenu(botMenu, botPlayer, "IgnoreTargetMenu", this::setIgnoreTarget);
            addEnemyPlayerMenu(botMenu, botPlayer, "IgnorePlayer", ChatCommands.IGNORE_PLAYER,
                  "BotCommandPanel.toast.ignorePlayer");
            addBotAction(botMenu, botPlayer, "IgnoreTurrets", this::ignoreTurrets);
            addBotAction(botMenu, botPlayer, "ClearIgnoredTargets", this::clearIgnoredTargetsOrder);
        });
    }

    /**
     * Adds a submenu listing each enemy player of the given bot; selecting one sends the given chat command with that
     * player's ID to the bot (e.g. ignore-player or blood-feud). The submenu is omitted when the bot has no enemy
     * players.
     *
     * @param botMenu    The bot submenu to add the menu to
     * @param botPlayer  The bot the order will be issued to
     * @param messageKey The resource key for the menu title and tooltip
     * @param command    The chat command to send with the enemy player's ID
     * @param toastKey   The resource key for the confirmation toast (formatted with the enemy player's name)
     */
    private void addEnemyPlayerMenu(JMenu botMenu, Player botPlayer, String messageKey, ChatCommands command,
          String toastKey) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        for (Player enemyPlayer : client.getGame().getPlayersList()) {
            if (!enemyPlayer.isEnemyOf(botPlayer)) {
                continue;
            }
            JMenuItem playerEntry = new JMenuItem(enemyPlayer.getName());
            playerEntry.addActionListener(evt -> {
                sendChatCommand(botPlayer, command, String.valueOf(enemyPlayer.getId()));
                acknowledgeOrder(botPlayer, Messages.getString(toastKey, enemyPlayer.getName()));
            });
            menu.add(playerEntry);
        }
        if (menu.getItemCount() > 0) {
            botMenu.add(menu);
        }
    }

    private void ignoreTurrets(Player botPlayer) {
        sendChatCommand(botPlayer, ChatCommands.IGNORE_TURRETS);
    }

    private void clearIgnoredTargetsOrder(Player botPlayer) {
        clearIgnoredTargets(botPlayer);
    }

    private JPopupMenu createArtilleryPopup() {
        return createBotFirstPopup((botMenu, botPlayer) -> {
            addBotAction(botMenu, botPlayer, "ArtilleryHalt",
                  bot -> sendArtilleryOrder(bot, ArtilleryOrder.HALT, SpecialAmmo.STANDARD, ""));
            addBotAction(botMenu, botPlayer, "ArtilleryAuto",
                  bot -> sendArtilleryOrder(bot, ArtilleryOrder.AUTO, SpecialAmmo.STANDARD, ""));
            botMenu.addSeparator();
            botMenu.add(createArtilleryFireMissionMenu(botPlayer, "ArtillerySingle", ArtilleryOrder.SINGLE));
            botMenu.add(createArtilleryFireMissionMenu(botPlayer, "ArtilleryVolley", ArtilleryOrder.VOLLEY));
            botMenu.add(createArtilleryFireMissionMenu(botPlayer, "ArtilleryBarrage", ArtilleryOrder.BARRAGE));
            botMenu.add(createCounterBatteryMenu(botPlayer));
        }, this::botHasArtillery);
    }

    /**
     * Creates a fire mission menu for one bot and the given artillery order: the player picks an ammo type, then is
     * prompted for the target hexes.
     *
     * @param botPlayer  The bot the fire mission will be issued to
     * @param messageKey The resource key for the menu title and tooltip
     * @param order      The artillery order this menu issues
     *
     * @return The created menu
     */
    private JMenu createArtilleryFireMissionMenu(Player botPlayer, String messageKey, ArtilleryOrder order) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        Set<SpecialAmmo> loadedAmmo = availableArtilleryAmmo(botPlayer);
        for (SpecialAmmo ammo : SpecialAmmo.values()) {
            if (!loadedAmmo.contains(ammo)) {
                // only offer ammo the bot's artillery actually carries
                continue;
            }
            JMenuItem ammoItem = new JMenuItem(
                  Messages.getString("BotCommandPanel.ArtilleryAmmo." + ammo.name()));
            ammoItem.addActionListener(evt -> promptAndSendFireMission(botPlayer, order, ammo));
            menu.add(ammoItem);
        }
        return menu;
    }

    /**
     * Builds the counter-battery fire menu: one item per loaded ammo type the bot can use against an observed off-board
     * enemy battery. Homing is excluded (there is no off-board TAG to guide it) and utility (zero-damage) munitions are
     * excluded (they cannot silence a battery). Selecting an ammo puts the bot into forced counter-battery mode with
     * that ammo; unlike the aimed fire missions it needs no target hexes, since it fires back at whatever enemy battery
     * has been observed.
     *
     * @param botPlayer The bot whose counter-battery mode is being set
     *
     * @return The counter-battery fire menu
     */
    private JMenu createCounterBatteryMenu(Player botPlayer) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.ArtilleryCounterBattery.title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel.ArtilleryCounterBattery.tooltip"));
        Set<SpecialAmmo> loadedAmmo = availableArtilleryAmmo(botPlayer);
        for (SpecialAmmo ammo : SpecialAmmo.values()) {
            if (!loadedAmmo.contains(ammo) || (ammo == SpecialAmmo.HOMING) || ammo.isUtility()) {
                // only damaging ammo the bot actually carries, and never homing (no off-board TAG to guide it)
                continue;
            }
            JMenuItem ammoItem = new JMenuItem(
                  Messages.getString("BotCommandPanel.ArtilleryAmmo." + ammo.name()));
            ammoItem.addActionListener(evt -> sendArtilleryOrder(botPlayer, ArtilleryOrder.COUNTER_BATTERY, ammo, ""));
            menu.add(ammoItem);
        }
        // Only offer counter-battery when it can actually be used: the bot needs suitable ammo and an observed enemy
        // battery to shoot back at. Otherwise gray the menu out with the reason, instead of issuing an order that
        // silently does nothing.
        if (menu.getItemCount() == 0) {
            menu.setEnabled(false);
            menu.setToolTipText(Messages.getString("BotCommandPanel.ArtilleryCounterBattery.noAmmo"));
        } else if (!hasObservedEnemyBattery(botPlayer)) {
            menu.setEnabled(false);
            menu.setToolTipText(Messages.getString("BotCommandPanel.ArtilleryCounterBattery.noTarget"));
        }
        return menu;
    }

    /**
     * @param botPlayer The bot to inspect
     *
     * @return {@code true} if at least one off-board enemy battery has been observed by the bot's team and is therefore
     *       a valid counter-battery target
     */
    private boolean hasObservedEnemyBattery(Player botPlayer) {
        for (InGameObject unit : client.getInGameObjects()) {
            if (!(unit instanceof Entity enemy)) {
                continue;
            }
            Player enemyOwner = enemy.getOwner();
            if (enemy.isOffBoard()
                  && (enemyOwner != null) && enemyOwner.isEnemyOf(botPlayer)
                  && enemy.isOffBoardObserved(botPlayer.getTeam())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collects the special-ammo types actually loaded (with shots remaining) across all of the given bot's artillery
     * units, so the fire-mission menu only offers ammo the bot can really fire.
     *
     * @param botPlayer The bot whose artillery ammunition is inspected
     *
     * @return The set of available special-ammo types (empty if the bot has no loaded artillery ammo)
     */
    private Set<SpecialAmmo> availableArtilleryAmmo(Player botPlayer) {
        Set<SpecialAmmo> available = EnumSet.noneOf(SpecialAmmo.class);
        boolean nukesAllowed = client.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ALLOW_NUKES);
        for (InGameObject unit : getUnitsOwnedBy(botPlayer)) {
            if (unit instanceof Entity entity) {
                collectLoadedArtilleryAmmo(entity, nukesAllowed, available);
            }
        }
        return available;
    }

    /**
     * Adds to {@code available} every special-ammo category that the given entity has loaded (with shots remaining)
     * for one of its artillery weapons.
     *
     * @param entity       The unit whose artillery weapons are inspected
     * @param nukesAllowed Whether the nuke game option is enabled (gates Davy Crockett rounds)
     * @param available    The accumulating set of available special-ammo categories
     */
    private void collectLoadedArtilleryAmmo(Entity entity, boolean nukesAllowed, Set<SpecialAmmo> available) {
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (!weapon.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                continue;
            }
            for (AmmoMounted ammo : entity.getAmmo(weapon)) {
                addUsableAmmoCategory(ammo, nukesAllowed, available);
            }
        }
    }

    /**
     * Adds the special-ammo category of a single ammo bin to {@code available}, provided the bin has shots remaining
     * and is not a Davy Crockett round while nukes are disabled.
     *
     * @param ammo         The ammo bin to categorize
     * @param nukesAllowed Whether the nuke game option is enabled (gates Davy Crockett rounds)
     * @param available    The accumulating set of available special-ammo categories
     */
    private void addUsableAmmoCategory(AmmoMounted ammo, boolean nukesAllowed, Set<SpecialAmmo> available) {
        if (ammo.getUsableShotsLeft() <= 0) {
            return;
        }
        SpecialAmmo category = SpecialAmmo.forMunitions(ammo.getType().getMunitionType());
        boolean isDisallowedNuke = (category == SpecialAmmo.DAVY_CROCKETT) && !nukesAllowed;
        if (isDisallowedNuke) {
            // a tactical nuke only fires when the nuke game option is enabled
            return;
        }
        available.add(category);
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
        // A volley feeds one shot to one tube across distinct hexes, so cap the picks at the bot's available tubes.
        int maxHexes = (order == ArtilleryOrder.VOLLEY) ? maxArtilleryShots(botPlayer) : 0;
        pickTargetHexes(Messages.getString("BotCommandPanel.HexPicker.artilleryOrder",
                    artilleryProWord(order), botPlayer.getName()),
              order == ArtilleryOrder.SINGLE,
              maxHexes,
              "BotCommandPanel.ArtilleryTargetPrompt.message",
              targets -> sendArtilleryOrder(botPlayer, order, ammo, targets));
    }

    /**
     * Computes the largest number of ready artillery tubes on any single one of the given bot's units. This is the
     * number of distinct hexes a volley fire mission can usefully target, since each unit feeds one shot to one tube
     * across distinct hexes.
     *
     * @param botPlayer The bot whose artillery units are counted
     *
     * @return The most ready artillery weapons on a single unit, or 0 if none (treated as "no limit")
     */
    private int maxArtilleryShots(Player botPlayer) {
        int maxShots = 0;
        for (InGameObject unit : getUnitsOwnedBy(botPlayer)) {
            if (unit instanceof Entity entity) {
                maxShots = Math.max(maxShots, countReadyArtilleryWeapons(entity));
            }
        }
        return maxShots;
    }

    /**
     * Counts the artillery weapons on the given entity that have usable ammunition.
     *
     * @param entity The unit to inspect
     *
     * @return The number of ready artillery weapons
     */
    private int countReadyArtilleryWeapons(Entity entity) {
        int readyWeapons = 0;
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_ARTILLERY) && hasUsableAmmo(entity, weapon)) {
                readyWeapons++;
            }
        }
        return readyWeapons;
    }

    /**
     * @param entity The unit carrying the weapon
     * @param weapon The artillery weapon
     *
     * @return {@code true} if the weapon has at least one ammo bin with shots remaining
     */
    private boolean hasUsableAmmo(Entity entity, WeaponMounted weapon) {
        for (AmmoMounted ammo : entity.getAmmo(weapon)) {
            if (ammo.getUsableShotsLeft() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lets the player designate target hexes by clicking them on the board. When no board view is available (e.g. in
     * the Commander GUI), falls back to a typed hex number prompt.
     *
     * @param orderDescription  Human-readable description of the order shown while picking
     * @param singleHex         {@code true} to finish after the first hex is picked
     * @param maxHexes          The maximum number of hexes that may be picked, or 0 for no limit
     * @param fallbackPromptKey The resource key for the typed prompt message used as fallback
     * @param onTargetsSelected Called with the picked hexes as dash-separated hex numbers
     */
    private void pickTargetHexes(String orderDescription, boolean singleHex, int maxHexes, String fallbackPromptKey,
          Consumer<String> onTargetsSelected) {
        BoardView boardView = null;
        if (clientGUI != null) {
            boardView = clientGUI.getCurrentBoardView()
                  .filter(BoardView.class::isInstance)
                  .map(BoardView.class::cast)
                  .orElse(null);
        }
        if (boardView == null) {
            String targets = promptForHexNumbers(fallbackPromptKey, "BotCommandPanel.TargetHexPrompt.title", maxHexes);
            if (targets != null) {
                onTargetsSelected.accept(targets);
            }
            return;
        }
        new HexTargetPicker(clientGUI, boardView, orderDescription, singleHex, maxHexes, onTargetsSelected).start();
    }

    /**
     * Prompts the player for hex numbers, accepting them separated by spaces or commas (e.g. "0810 0811"), and
     * validates them. Shows an error toast on invalid input.
     *
     * @param promptMessageKey The resource key for the prompt message
     * @param promptTitleKey   The resource key for the prompt dialog title
     * @param maxHexes         The maximum number of hexes accepted, or 0 for no limit
     *
     * @return The entered hex numbers joined with dashes, or {@code null} if the player canceled or the input was invalid
     */
    private @Nullable String promptForHexNumbers(String promptMessageKey, String promptTitleKey, int maxHexes) {
        String input = JOptionPane.showInputDialog(this,
              Messages.getString(promptMessageKey),
              Messages.getString(promptTitleKey),
              JOptionPane.QUESTION_MESSAGE);
        if ((input == null) || input.isBlank()) {
            return null;
        }
        String[] hexNumbers = input.trim().split("[\\s,]+");
        String targets = String.join("-", hexNumbers);
        if (!isValidHexInput(targets)) {
            if (clientGUI != null) {
                clientGUI.addToast(ToastLevel.ERROR,
                      Messages.getString("BotCommandPanel.ArtilleryTargetPrompt.invalid", input));
            }
            return null;
        }
        if ((maxHexes > 0) && (hexNumbers.length > maxHexes)) {
            if (clientGUI != null) {
                clientGUI.addToast(ToastLevel.ERROR,
                      Messages.getString("BotCommandPanel.HexPicker.tooMany", maxHexes));
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
     * @return {@code true} if all hex numbers are valid
     */
    private boolean isValidHexInput(String targets) {
        Board board = client.getGame().getBoard();
        try {
            for (String hexNumber : targets.split("-")) {
                Coords coords = Coords.parseHexNumber(hexNumber);
                if ((coords.getX() < 0) || (coords.getY() < 0)) {
                    return false;
                }
                // Reject hexes that parse but fall outside the current board (e.g. "9999"), when a board is available.
                if ((board != null) && !board.contains(coords)) {
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
        boolean counterBattery = (order == ArtilleryOrder.COUNTER_BATTERY);
        // halt/auto take no ammo or targets; counter-battery takes ammo but no targets (it fires at whatever enemy
        // battery is observed); aimed fire missions take both ammo and target hexes.
        String arguments;
        if (counterBattery) {
            arguments = order.name() + " " + ammo.name();
        } else if (targets.isBlank()) {
            arguments = order.name();
        } else {
            arguments = order.name() + " " + ammo.name() + " " + targets;
        }
        sendChatCommand(botPlayer, ChatCommands.ARTILLERY, arguments);
        if (counterBattery) {
            acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.counterBattery",
                  artilleryAmmoProWord(ammo)));
        } else if (targets.isBlank()) {
            // Control order (halt/auto): no grid, just the warning order pro-word
            acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.fireMissionControl",
                  artilleryProWord(order)));
        } else {
            acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.fireMission",
                  artilleryProWord(order), artilleryAmmoProWord(ammo), toGrid(targets)));
        }
    }

    /**
     * @param order The artillery warning order
     *
     * @return The call-for-fire pro-word for the warning order (e.g. "Fire for effect")
     */
    private String artilleryProWord(ArtilleryOrder order) {
        return Messages.getString("BotCommandPanel.artilleryProWord." + order.name());
    }

    /**
     * @param ammo The special ammunition selected for the fire mission
     *
     * @return The call-for-fire pro-word for the ammunition (e.g. "HE", "Illum")
     */
    private String artilleryAmmoProWord(SpecialAmmo ammo) {
        return Messages.getString("BotCommandPanel.artilleryAmmoProWord." + ammo.name());
    }

    /**
     * Converts the internal dash-separated target hex list (e.g. "0810-0811") into a readable grid string (e.g. "0810,
     * 0811") for call-for-fire messages.
     *
     * @param targets The dash-separated target hex list
     *
     * @return The comma-separated grid string
     */
    private String toGrid(String targets) {
        return targets.replace("-", ", ");
    }

    private JPopupMenu createWaypointsPopup() {
        return createBotFirstPopup((botMenu, botPlayer) -> {
            if (!getUnitsOwnedBy(botPlayer).isEmpty()) {
                botMenu.add(createWaypointHexOrderMenu(botPlayer, "SetWaypoints", ChatCommands.SET_WAYPOINT));
                botMenu.add(createWaypointHexOrderMenu(botPlayer, "AddWaypoint", ChatCommands.ADD_WAYPOINT));
                botMenu.addSeparator();
                botMenu.add(createWaypointUnitOrderMenu(botPlayer, "RemoveWaypoint", ChatCommands.REMOVE_WAYPOINT));
                botMenu.add(createWaypointUnitOrderMenu(botPlayer, "ClearWaypoints", ChatCommands.CLEAR_WAYPOINT));
            }
            addBotAction(botMenu, botPlayer, "ClearAllWaypoints", this::clearAllWaypoints);
        }, this::hasOnBoardUnits);
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
     * Creates a waypoint menu for one bot that needs target hexes: pick one of the bot's units, then enter the hexes.
     *
     * @param botPlayer       The bot the waypoint order will be issued to
     * @param messageKey      The resource key for the menu title and tooltip
     * @param waypointCommand The waypoint chat command to send (set-waypoints or add-waypoint)
     *
     * @return The created menu
     */
    private JMenu createWaypointHexOrderMenu(Player botPlayer, String messageKey, ChatCommands waypointCommand) {
        return createWaypointMenu(botPlayer, messageKey, (bot, unit) ->
              promptAndSendWaypoints(bot, unit, waypointCommand,
                    Messages.getString("BotCommandPanel." + messageKey + ".title")));
    }

    /**
     * Creates a waypoint menu for one bot that acts on a unit without needing hexes (remove last / clear waypoints).
     *
     * @param botPlayer       The bot the waypoint order will be issued to
     * @param messageKey      The resource key for the menu title and tooltip
     * @param waypointCommand The waypoint chat command to send (remove-waypoint or clear-waypoints)
     *
     * @return The created menu
     */
    private JMenu createWaypointUnitOrderMenu(Player botPlayer, String messageKey, ChatCommands waypointCommand) {
        return createWaypointMenu(botPlayer, messageKey, (bot, unit) -> {
            sendChatCommand(bot, waypointCommand, String.valueOf(unit.getId()));
            acknowledgeOrder(bot, Messages.getString("BotCommandPanel.toast.unitOrder",
                  Messages.getString("BotCommandPanel." + messageKey + ".title"),
                  unit.getId(), unit.generalName()));
        });
    }

    /**
     * Creates a menu listing one bot's units; selecting a unit runs the given action.
     *
     * @param botPlayer  The bot whose units are listed
     * @param messageKey The resource key for the menu title and tooltip
     * @param unitAction The action to run with the bot and the chosen unit
     *
     * @return The created menu
     */
    private JMenu createWaypointMenu(Player botPlayer, String messageKey, BiConsumer<Player, InGameObject> unitAction) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        for (InGameObject unit : getUnitsOwnedBy(botPlayer)) {
            JMenuItem unitItem = new JMenuItem("ID:" + unit.getId() + " " + unit.generalName());
            unitItem.addActionListener(evt -> unitAction.accept(botPlayer, unit));
            menu.add(unitItem);
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
        pickTargetHexes(orderTitle + " - " + unit.generalName(), false, 0,
              "BotCommandPanel.WaypointPrompt.message",
              targets -> {
                  sendChatCommand(botPlayer, waypointCommand, unit.getId() + " " + targets);
                  acknowledgeOrder(botPlayer, Messages.getString("BotCommandPanel.toast.unitOrder",
                        orderTitle, unit.getId(), unit.generalName()));
              });
    }

    private record PlayerInGameObject(Player player, InGameObject inGameObject) {}

    /**
     * Adds a submenu listing every enemy unit of the given bot; selecting a unit runs the action against that bot and
     * unit (e.g. prioritize or ignore). The submenu is omitted when the bot has no enemy units, and a scroller keeps
     * long unit lists usable.
     *
     * @param botMenu    The bot submenu to add the menu to
     * @param botPlayer  The bot the order will be issued to
     * @param messageKey The resource key for the menu title and tooltip
     * @param action     The action to run with the chosen bot and unit
     */
    private void addEnemyUnitMenu(JMenu botMenu, Player botPlayer, String messageKey,
          Consumer<PlayerInGameObject> action) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel." + messageKey + ".title"));
        menu.setToolTipText(Messages.getString("BotCommandPanel." + messageKey + ".tooltip"));
        var playerMap = new HashMap<Integer, Player>();
        client.getGame().getPlayersList().forEach(player -> playerMap.put(player.getId(), player));
        for (var inGameObject : client.getInGameObjects()) {
            Player owner = playerMap.get(inGameObject.getOwnerId());
            if (owner != null && owner.isEnemyOf(botPlayer)) {
                var unitEntry = new JMenuItem("ID:" + inGameObject.getId() + " " + inGameObject.generalName());
                unitEntry.addActionListener(evt -> action.accept(new PlayerInGameObject(botPlayer, inGameObject)));
                menu.add(unitEntry);
            }
        }
        if (menu.getItemCount() > 0) {
            MenuScroller.setScrollerFor(menu, 15);
            botMenu.add(menu);
        }
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
