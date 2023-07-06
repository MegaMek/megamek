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

public class ManagePlayersDialog extends AbstractButtonDialog {
    private static final int PRINCESS_INDEX = 1;
    private static final int KICK_INDEX = 1;

    /** A ClientGUI given to the dialog. */
    private final ClientGUI clientGui;

    /** Convenience field for clientGui.getClient().getGame(). */
    private final Game game;

    /** The list of displayed ghost players */
    private List<Player> allPlayers = new ArrayList<Player>();

    /** Maps a ghost player to the combobox that sets its replacement */
    private Map<Player, JComboBox<String>> ghostChoosers = new HashMap<>();

    /** Maps a bot player to the combobox that allows it to be kicked */
    private Map<Player, JComboBox<String>> botChoosers = new HashMap<>();

    /** Maps a ghost player to the config button for the bot settings */
    private Map<Player, JButton> configButtons = new HashMap<>();

    /** Maps a ghost player to bot settings chosen for it */
    private Map<Player, BehaviorSettings> botConfigs = new HashMap<>();

    protected ManagePlayersDialog(JFrame frame, ClientGUI clientGUI) {
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
            LogManager.getLogger().error("Error finalizing the ManagePlayersDialog. Returning the created dialog, but this is likely to cause some oddities.", ex);
        }
    }

    protected void refreshPlayers() {
//        allPlayers = game.getPlayersVector().stream()
//                .collect(Collectors.toSet());
        // Add a default Princess behavior for each
        // It will be overwritten by existing bot or savegame behaviors
        allPlayers.clear();
        allPlayers.addAll(game.getPlayersList());
        allPlayers.sort(Comparator.comparingInt(Player::getId));
        allPlayers.forEach(p -> botConfigs.put(p, new BehaviorSettings()));
    }
    @Override
    protected Container createCenterPane() {
        // Construct the available replacements for the ComboBox chooser
        Vector<String> ghostReplacements = new Vector<>();
        ghostReplacements.add(Messages.getString("ReplacePlayersDialog.noReplacement"));
        ghostReplacements.add(Messages.getString("ReplacePlayersDialog.princess"));

        Vector<String> botReplacments = new Vector<>();
        botReplacments.add(Messages.getString("ReplacePlayersDialog.keep"));
        botReplacments.add(Messages.getString("ReplacePlayersDialog.kick"));


        var gridPanel = new JPanel();
//        gridPanel.setLayout(new GridLayout(allPlayers.size() + 2, 5, 2, 2));
        gridPanel.setLayout(new GridLayout(0, 5, 2,2));
        //column labels
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.ghostPlayerHeader")));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.ghostPlayerHeader")));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.configAvailableHeader")));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.chooseReplacementHeader")));
        gridPanel.add(new JLabel("Button"));
        //spacer row
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());

        // The rows for the ghost players
        Map<String, BehaviorSettings> savedSettings = game.getBotSettings();
        for (Player player : allPlayers) {
            // Name
            gridPanel.add(new JLabel(player.toString()));

            if (player.isGhost()) {
                gridPanel.add(new JLabel( "Ghost"));

                var ghostChooser = new JComboBox<>(ghostReplacements);
                ghostChoosers.put(player, ghostChooser);

                var princessConfigButton = new JButton(Messages.getString("ReplacePlayersDialog.config"));
                configButtons.put(player, princessConfigButton);

                boolean savedSettingsExist = (savedSettings != null) && savedSettings.containsKey(player.getName());
                if (savedSettingsExist) {
                    // it was presumably Princess before, so default to replace
                    ghostChooser.setSelectedIndex(PRINCESS_INDEX);
                    botConfigs.put(player, savedSettings.get(player.getName()));
                }
                ghostChooser.addActionListener(e -> updateButtonStates());
                if (savedSettingsExist) {
                    gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.configAvailable")));
                } else {
                    gridPanel.add(new JLabel());
                }
                var cPanel = new JPanel();
                cPanel.add(ghostChooser);
                gridPanel.add(cPanel);

                var panel = new JPanel();
                panel.add(princessConfigButton);
                gridPanel.add(panel);

            } else if (clientGui.getClient().isLocalBot(player)) {
                Client bot = clientGui.getClient().getBotClient(player);
                if (bot instanceof Princess) {
                    gridPanel.add(new JLabel( "Local Princess"));
                    Princess princess = (Princess) bot;
                    var botChooser = new JComboBox<>(botReplacments);
                    botChoosers.put(player, botChooser);

                    botChooser.setSelectedIndex(0);
                    botChooser.addActionListener(e -> updateButtonStates());
                    botConfigs.put(player, princess.getBehaviorSettings());
                    var princessConfigButton = new JButton(Messages.getString("ReplacePlayersDialog.config"));
                    princessConfigButton.addActionListener(e -> callConfig(player));

                    gridPanel.add(new JLabel( ""));

                    var cPanel = new JPanel();
                    cPanel.add(botChooser);
                    gridPanel.add(cPanel);

                    var panel = new JPanel();
                    panel.add(princessConfigButton);
                    gridPanel.add(panel);

                } else if (bot == null){
                    gridPanel.add(new JLabel("Local Null Bot"));
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                } else {
                    // Not a Ghost or Princess, something else, maybe a TestBot
                    gridPanel.add(new JLabel( "Local "+bot.getClass().toString()));
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                    gridPanel.add(new JLabel());
                }
            } else {
                gridPanel.add(new JLabel(
                        (clientGui.getClient().getLocalPlayer().equals(player) ? "Local " : "Remote ")
                                + (player.isBot() ? "Bot" : player.isGameMaster() ? "GM" : player.isObserver() ? "Observer" : "Player"))
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

    /** Called from the config buttons. Opens a BotConfig Dialog and saves the result, if any. */
    private void callConfig(Player ghost) {
        var bcd = new BotConfigDialog(getFrame(), ghost.getName(), botConfigs.get(ghost), clientGui);
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CONFIRMED) {
            botConfigs.put(ghost, bcd.getBehaviorSettings());
        }
    }

    /** Updates the config button enabled states (only enabled when Princess bot is selected). */
    private void updateButtonStates() {
        for (Player ghost : ghostChoosers.keySet()) {
            JButton button = configButtons.get(ghost);
            button.setEnabled(ghostChoosers.get(ghost).getSelectedIndex() == PRINCESS_INDEX);
        }
    }

    /**
     * Returns the result of the dialog with respect to selected ghosts and bots to be replaced or
     * updated by princess bots.
     * @return a map that links zero, one or more BehaviorSettings (a Princess configuration)
     * to the ghost or princess player name they were chosen for. The returned map only
     * includes entries for those ghost players that had a Princess Bot replacement selected
     * and all current Princess Bots.
     * The result may be empty, but not null.
     */
    public Map<String, BehaviorSettings> getNewBots() {
        var result = new HashMap<String, BehaviorSettings>();
        for (Player ghost : ghostChoosers.keySet()) {
            JComboBox<String> chooser = ghostChoosers.get(ghost);
            if (chooser.getSelectedIndex() == PRINCESS_INDEX) {
                result.put(ghost.getName(), botConfigs.get(ghost));
            }
        }

        //Update all existing bots but ignore bots that will be kicked
        for (Player bot : botChoosers.keySet()) {
            JComboBox<String> chooser = botChoosers.get(bot);
            if (chooser.getSelectedIndex() != KICK_INDEX) {
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
        for (Player bot : botChoosers.keySet()) {
            JComboBox<String> chooser = botChoosers.get(bot);
            if (chooser.getSelectedIndex() != KICK_INDEX) {
                result.add(bot.getName());
            }
        }
        return result;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}