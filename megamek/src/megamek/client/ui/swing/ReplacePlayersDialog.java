/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.PrincessException;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Game;
import megamek.common.Player;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ReplacePlayersDialog extends AbstractButtonDialog {
    // replace ghost combo box choices
    private static final int REPLACE_WITH_PRINCESS_INDEX = 1;

    // edit existing local bot combo box choices
    private static final int KICK_INDEX = 1;
    private static final int EDIT_CONFIG_INDEX = 2;

    /** A ClientGUI given to the dialog. */
    private final ClientGUI clientGui;

    /** Convenience field for clientGui.getClient().getGame(). */
    private final Game game;

    /** The id ordered list of displayed ghosts and bots players */
    private List<Player> ghostAndBotPlayers;

    /** Maps a ghost player to the combobox that sets its replacement */
    private Map<Player, JComboBox<String>> ghostChoosers = new HashMap<>();

    /** Maps a bot player to the combobox that allows it to be edited or kicked */
    private Map<Player, JComboBox<String>> localBotChoosers = new HashMap<>();
    /** Maps a bot player to the combobox that allows it to be kicked */
    private Map<Player, JComboBox<String>> remoteBotChoosers = new HashMap<>();

    /** Maps a ghost player to the config button for the bot settings */
    private Map<Player, JButton> configButtons = new HashMap<>();

    /** Maps a ghost player to bot settings chosen for it */
    private Map<Player, BehaviorSettings> botConfigs = new HashMap<>();
    private final static String LOCAL = Messages.getString("ReplacePlayersDialog.local");
    private final static String REMOTE = Messages.getString("ReplacePlayersDialog.remote");

    protected ReplacePlayersDialog(JFrame frame, ClientGUI clientGUI) {
        super(frame, "ReplacePlayersDialog", "ReplacePlayersDialog.title");
        this.clientGui = clientGUI;
        game = clientGui.getClient().getGame();
        refreshPlayers();
        initialize();
    }

    @Override
    protected void initialize() {
        super.initialize();
        adaptToGUIScale();
        try {
            finalizeInitialization();
        } catch (Exception ex) {
            LogManager.getLogger().error("Error finalizing the ReplacePlayersDialog. Returning the created dialog, but this is likely to cause some oddities.", ex);
        }
    }

    protected void refreshPlayers() {
        ghostAndBotPlayers = game.getPlayersVector().stream()
                .filter(c -> c.isGhost() || c.isBot()).collect(Collectors.toList());
        ghostAndBotPlayers.sort(Comparator.comparingInt(Player::getId));
        ghostAndBotPlayers.forEach(p -> botConfigs.put(p, new BehaviorSettings()));
    }
    
    @Override
    protected Container createCenterPane() {
        Vector<String> ghostOptions = new Vector<>();
        ghostOptions.add(Messages.getString("ReplacePlayersDialog.optionDoNotReplace"));
        ghostOptions.add(Messages.getString("ReplacePlayersDialog.optionReplace"));

        Vector<String> localBotOptions = new Vector<>();
        localBotOptions.add(Messages.getString("ReplacePlayersDialog.optionNone"));
        localBotOptions.add(Messages.getString("ReplacePlayersDialog.optionKick"));
        localBotOptions.add(Messages.getString("ReplacePlayersDialog.optionEdit"));

        Vector<String> remoteBotOptions = new Vector<>();
        remoteBotOptions.add(Messages.getString("ReplacePlayersDialog.optionNone"));
        remoteBotOptions.add(Messages.getString("ReplacePlayersDialog.optionKick"));

        var gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(0, 5, 2,2));
        //column labels
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.playerNameHeader")));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.playerTypeHeader")));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.chooseActionHeader")));
        gridPanel.add(new JLabel());
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.configAvailableHeader")));
        //spacer row
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());

        // The rows for the ghost players
        Map<String, BehaviorSettings> savedSettings = game.getBotSettings();
        for (Player player : ghostAndBotPlayers) {
            // Name
            gridPanel.add(new JLabel(player.toString()));
            boolean savedSettingsExist = (savedSettings != null) && savedSettings.containsKey(player.getName());

            if (player.isGhost()) {
                gridPanel.add(new JLabel( "Ghost"));

                var ghostChooser = new JComboBox<>(ghostOptions);
                ghostChoosers.put(player, ghostChooser);
                if (savedSettingsExist) {
                    // it was presumably Princess before, so default to replace
                    ghostChooser.setSelectedIndex(REPLACE_WITH_PRINCESS_INDEX);
                    botConfigs.put(player, savedSettings.get(player.getName()));
                    try {
                        // Copy to protect the saved settings
                        botConfigs.put(player, savedSettings.get(player.getName()).getCopy());
                    } catch (PrincessException e) {
                        LogManager.getLogger().error("", e);
                        // fallback to default
                    }
                }
                ghostChooser.addActionListener(e -> updateButtonStates());

                var cPanel = new JPanel();
                cPanel.add(ghostChooser);
                gridPanel.add(cPanel);

                gridPanel.add(configButton(player));
                gridPanel.add(restoreButton(player, savedSettingsExist));
            } else if (clientGui.getClient().isLocalBot(player)) {
                Client bot = clientGui.getClient().getBotClient(player);
                if (bot instanceof Princess) {
                    gridPanel.add(new JLabel( LOCAL + " Princess"));
                    Princess princess = (Princess) bot;
                    try {
                        // Copy to protect the current settings
                        botConfigs.put(player, princess.getBehaviorSettings().getCopy());
                    } catch (PrincessException e) {
                        LogManager.getLogger().error("", e);
                        // fallback to default
                    }

                    var botChooser = new JComboBox<>(localBotOptions);
                    localBotChoosers.put(player, botChooser);
                    botChooser.setSelectedIndex(0);
                    botChooser.addActionListener(e -> updateButtonStates());

                    var cPanel = new JPanel();
                    cPanel.add(botChooser);
                    gridPanel.add(cPanel);

                    gridPanel.add(configButton(player));
                    gridPanel.add(restoreButton(player, savedSettingsExist));
                } else if (bot == null){
                    gridPanel.add(new JLabel(LOCAL + " Null Bot"));
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                } else {
                    // Not a Ghost or Princess, something else, maybe a TestBot
                    gridPanel.add(new JLabel( LOCAL + ' ' +bot.getClass().toString()));
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                }
            } else if (player.isBot()) {
                gridPanel.add(new JLabel(REMOTE + " Bot"));
                var botChooser = new JComboBox<>(remoteBotOptions);
                remoteBotChoosers.put(player, botChooser);
                botChooser.setSelectedIndex(0);
                var cPanel = new JPanel();
                cPanel.add(botChooser);
                gridPanel.add(cPanel);
                gridPanel.add(new JLabel());
                gridPanel.add(new JLabel());
            } else {
                gridPanel.add(new JLabel(
                        (clientGui.getClient().getLocalPlayer().equals(player) ? LOCAL : REMOTE) + ' '
                                + (player.isGameMaster() ? "GM" : player.isObserver() ? "Observer" : "Player"))
                );
                gridPanel.add(new JLabel());
                gridPanel.add(new JLabel());
                gridPanel.add(new JLabel());
            }
        }

        updateButtonStates();

        var result = new JPanel(new FlowLayout(FlowLayout.LEFT));
        result.setBorder(new EmptyBorder(10, 20, 40, 20));
        result.add(gridPanel);

        return new JScrollPane(result);
    }

    private JComponent configButton(Player player) {
        var princessConfigButton = new JButton(Messages.getString("ReplacePlayersDialog.config"));
        configButtons.put(player, princessConfigButton);
        princessConfigButton.addActionListener(e -> callConfig(player));

        var panel = new JPanel();
        panel.add(princessConfigButton);
        return panel;
    }

    private JComponent restoreButton(Player player, boolean savedSettingsExist ) {
        if (savedSettingsExist) {
            JButton restoreButton = new JButton(Messages.getString("ReplacePlayersDialog.configAvailable"));
            restoreButton.addActionListener(e -> callRestoreConfig(player));

            var rPanel = new JPanel();
            rPanel.add(restoreButton);
            return rPanel;
        } else {
            return new JLabel();
        }
    }

    /** Called from the config buttons. Opens a BotConfig Dialog and saves the result, if any. */
    private void callConfig(Player botOrGhost) {
        var bcd = new BotConfigDialog(getFrame(), botOrGhost.getName(), botConfigs.get(botOrGhost), clientGui);
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CONFIRMED) {
            botConfigs.put(botOrGhost, bcd.getBehaviorSettings());
            if (localBotChoosers.containsKey(botOrGhost)) {
                localBotChoosers.get(botOrGhost).setSelectedIndex(EDIT_CONFIG_INDEX);
            }
        }
    }

    private void callRestoreConfig(Player botOrGhost) {
        Map<String, BehaviorSettings> savedSettings = game.getBotSettings();
        if ((savedSettings != null) && savedSettings.containsKey(botOrGhost.getName())) {
            try {
                // Don't change the existing saved settings
                botConfigs.put(botOrGhost, savedSettings.get(botOrGhost.getName()).getCopy());
            } catch (PrincessException e) {
                LogManager.getLogger().error("", e);
                // fallback to default
            }
        }
    }

    /** Updates the config button enabled states (only enabled when Princess bot is selected). */
    private void updateButtonStates() {
        for (Player ghost : ghostChoosers.keySet()) {
            JButton button = configButtons.get(ghost);
            button.setEnabled(ghostChoosers.get(ghost).getSelectedIndex() == REPLACE_WITH_PRINCESS_INDEX);
        }

        for (Player bot : localBotChoosers.keySet()) {
            JButton button = configButtons.get(bot);
            button.setEnabled(localBotChoosers.get(bot).getSelectedIndex() != KICK_INDEX);
        }
    }

    /**
     * @return the result of the dialog with respect to ghost players to be replaced by Princess bots.
     * The returned map links zero, one or more BehaviorSettings (a Princess configuration)
     * to the ghost player name they were chosen for. The returned map only
     * includes entries for those ghost players that had a Princess Bot replacement selected.
     * The result may be empty, but not null.
     */
    public Map<String, BehaviorSettings> getNewBots() {
        var result = new HashMap<String, BehaviorSettings>();
        for (Player ghost : ghostChoosers.keySet()) {
            JComboBox<String> chooser = ghostChoosers.get(ghost);
            if (chooser.getSelectedIndex() == REPLACE_WITH_PRINCESS_INDEX) {
                result.put(ghost.getName(), botConfigs.get(ghost));
            }
        }
        return result;
    }

    /**
     * @return the result of the dialog with respect to Princess bots whose configuration is to be changed
     * The returned map links zero, one or more BehaviorSettings (a Princess configuration)
     * to the Princess player name they were chosen for. The returned map only
     * includes entries for those Princess players that had the Edit option selected.
     * The result may be empty, but not null.
     */
    public Map<String, BehaviorSettings> getChangedBots() {
        // All local bots with edited configs option selected
        var result = new HashMap<String, BehaviorSettings>();
        for (Player bot : localBotChoosers.keySet()) {
            JComboBox<String> chooser = localBotChoosers.get(bot);
            if (chooser.getSelectedIndex() == EDIT_CONFIG_INDEX) {
                result.put(bot.getName(), botConfigs.get(bot));
            }
        }
        return result;
    }

    /**
     * Returns the result of the dialog with respect to selected princess bots to be kicked
     * The result may be empty, but not null.
     * @return a Set of bot player names to be kicked. May be empty but not null
     */
    public Set<String> getKickBots() {
        var result = new HashSet<String>();
        for (Player bot : localBotChoosers.keySet()) {
            JComboBox<String> chooser = localBotChoosers.get(bot);
            if (chooser.getSelectedIndex() == KICK_INDEX) {
                result.add(bot.getName());
            }
        }
        for (Player bot : remoteBotChoosers.keySet()) {
            JComboBox<String> chooser = remoteBotChoosers.get(bot);
            if (chooser.getSelectedIndex() == KICK_INDEX) {
                result.add(bot.getName());
            }
        }
        return result;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}