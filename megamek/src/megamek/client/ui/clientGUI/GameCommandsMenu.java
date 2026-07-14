/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.clientGUI;

import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Builds the popup menu of the Commands button in the game commands strip: the game-level commands every player may
 * issue (declaring victory, conceding, skipping a stuck turn, rolling dice) and, below them, the Game Master role and
 * its tools.
 *
 * <p>Every entry is a server chat command, the same one a player could type into the chat box, so the server keeps
 * deciding what is allowed. The menu is rebuilt on every click, because who holds the Game Master role can change from
 * turn to turn.</p>
 */
public class GameCommandsMenu {
    private static final MMLogger LOGGER = MMLogger.create(GameCommandsMenu.class);

    private static final String COMMAND_VICTORY = "/victory";
    private static final String COMMAND_DEFEAT = "/defeat";
    private static final String COMMAND_SKIP = "/skip";
    private static final String COMMAND_ROLL = "/roll 2d6";
    private static final String COMMAND_GAME_MASTER = "/gm";
    private static final String COMMAND_ALLOW_GAME_MASTER = "/allowGM";

    private final ClientGUI clientGUI;

    /**
     * Creates the builder for the game commands popup menu.
     *
     * @param clientGUI The client GUI the commands are sent from and the confirmations are shown on
     */
    public GameCommandsMenu(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    /**
     * Builds the game commands popup menu for the current state of the game.
     *
     * @return The popup menu to show under the Commands button
     */
    public JPopupMenu createPopup() {
        JPopupMenu popup = new JPopupMenu();
        addGameCommands(popup);
        addGameMasterCommands(popup);
        return popup;
    }

    /**
     * Adds the commands that every player in the game may issue.
     *
     * @param popup The popup menu to add the commands to
     */
    private void addGameCommands(JPopupMenu popup) {
        popup.add(createConfirmedCommandItem("RequestVictory", COMMAND_VICTORY));
        popup.add(createConfirmedCommandItem("AdmitDefeat", COMMAND_DEFEAT));
        popup.add(createCommandItem("SkipTurn", COMMAND_SKIP));
        popup.add(createCommandItem("RollDice", COMMAND_ROLL));
    }

    /**
     * Adds the Game Master section. What it offers depends on who holds the role: the player holding it gets the tools
     * and can give the role up, a player who does not hold it is told who does, and when nobody holds it the role can
     * be requested. Players who may not hold the role at all (bots and observers) get no section.
     *
     * @param popup The popup menu to add the section to
     */
    private void addGameMasterCommands(JPopupMenu popup) {
        Player localPlayer = getClient().getLocalPlayer();
        Player gameMaster = findGameMaster(getClient().getGame().getPlayersList());
        boolean localPlayerIsGameMaster = localPlayer.equals(gameMaster);

        if (!localPlayerIsGameMaster && !localPlayer.isGameMasterPermitted()) {
            LOGGER.debug("[GameCommands] {}: no Game Master section - this player may not hold the role",
                  localPlayer.getName());
            return;
        }
        popup.addSeparator();

        if (localPlayerIsGameMaster) {
            popup.add(createCommandItem("GiveUpGameMaster", COMMAND_GAME_MASTER));
            popup.add(GameMasterCommandMenu.createSpecialCommandsMenu(clientGUI, null));
        } else if (gameMaster != null) {
            LOGGER.debug("[GameCommands] {}: Game Master tools hidden - {} holds the role",
                  localPlayer.getName(), gameMaster.getName());
            JMenuItem heldByItem = new JMenuItem(Messages.getString("GameCommands.GameMasterHeldBy.title",
                  gameMaster.getName()));
            heldByItem.setToolTipText(Messages.getString("GameCommands.GameMasterHeldBy.tooltip"));
            heldByItem.setEnabled(false);
            popup.add(heldByItem);
        } else {
            popup.add(createCommandItem("RequestGameMaster", COMMAND_GAME_MASTER));
            popup.add(createCommandItem("AllowGameMaster", COMMAND_ALLOW_GAME_MASTER));
        }
    }

    /**
     * Finds the player currently holding the Game Master role.
     *
     * @param players The players in the game
     *
     * @return The Game Master, or {@code null} if no player currently holds the role
     */
    public static @Nullable Player findGameMaster(List<Player> players) {
        for (Player player : players) {
            if (player.isGameMaster()) {
                return player;
            }
        }
        return null;
    }

    /**
     * Creates a menu item that sends its chat command straight away.
     *
     * @param messageKey  The resource key prefix for the item's title and tooltip
     * @param chatCommand The chat command to send when the item is chosen
     *
     * @return The created menu item
     */
    private JMenuItem createCommandItem(String messageKey, String chatCommand) {
        JMenuItem commandItem = new JMenuItem(Messages.getString("GameCommands." + messageKey + ".title"));
        commandItem.setToolTipText(Messages.getString("GameCommands." + messageKey + ".tooltip"));
        commandItem.addActionListener(evt -> sendChatCommand(messageKey, chatCommand));
        return commandItem;
    }

    /**
     * Creates a menu item that asks for confirmation before sending its chat command. Used for the commands that end
     * the game for everyone, where a misclick cannot be taken back.
     *
     * @param messageKey  The resource key prefix for the item's title, tooltip and confirmation text
     * @param chatCommand The chat command to send once the player confirms
     *
     * @return The created menu item
     */
    private JMenuItem createConfirmedCommandItem(String messageKey, String chatCommand) {
        JMenuItem commandItem = new JMenuItem(Messages.getString("GameCommands." + messageKey + ".title"));
        commandItem.setToolTipText(Messages.getString("GameCommands." + messageKey + ".tooltip"));
        commandItem.addActionListener(evt -> {
            boolean confirmed = clientGUI.doYesNoDialog(
                  Messages.getString("GameCommands." + messageKey + ".confirm.title"),
                  Messages.getString("GameCommands." + messageKey + ".confirm.message"));
            if (!confirmed) {
                LOGGER.debug("[GameCommands] {} canceled at the confirmation dialog", chatCommand);
                return;
            }
            sendChatCommand(messageKey, chatCommand);
        });
        return commandItem;
    }

    /**
     * Sends the given chat command to the server. The server answers in the chat window, both when it carries the
     * command out and when it refuses it.
     *
     * @param messageKey  The resource key prefix of the command, for the log line
     * @param chatCommand The chat command to send
     */
    private void sendChatCommand(String messageKey, String chatCommand) {
        LOGGER.debug("[GameCommands] {}: sending {}", messageKey, chatCommand);
        getClient().sendChat(chatCommand);
    }

    private Client getClient() {
        return clientGUI.getClient();
    }
}
