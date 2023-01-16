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

import megamek.client.bot.princess.BehaviorSettings;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

public class ReplacePlayersDialog extends AbstractButtonDialog {
    private static final int PRINCESS_INDEX = 1;
    
    /** A ClientGUI given to the dialog. */
    private final ClientGUI clientGui;
    
    /** Convenience field for clientGui.getClient().getGame(). */
    private final Game game;
    
    /** The list of displayed ghost players */
    private Set<Player> ghostPlayers;
    
    /** Maps a ghost player to the combobox that sets its replacement */
    private Map<Player, JComboBox<String>> playerChoosers = new HashMap<>();
    
    /** Maps a ghost player to the config button for the bot settings */
    private Map<Player, JButton> configButtons = new HashMap<>();
    
    /** Maps a ghost player to bot settings chosen for it */
    private Map<Player, BehaviorSettings> botConfigs = new HashMap<>();

    protected ReplacePlayersDialog(JFrame frame, ClientGUI cg) {
        super(frame, "ReplacePlayersDialog", "ReplacePlayersDialog.title");
        clientGui = cg;
        game = clientGui.getClient().getGame();
        ghostPlayers = game.getPlayersVector().stream()
                .filter(Player::isGhost).collect(Collectors.toSet());
        // Add a new Princess behavior for each ghost. It will be overwritten by savegame behaviors
        ghostPlayers.forEach(p -> botConfigs.put(p, new BehaviorSettings()));
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

    @Override
    protected Container createCenterPane() {
        // Construct the available replacements for the ComboBox chooser
        Vector<String> replacements = new Vector<>();
        replacements.add(Messages.getString("ReplacePlayersDialog.noReplacement"));
        replacements.add(Messages.getString("ReplacePlayersDialog.princess"));
        
        var gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(ghostPlayers.size() + 2, 4, 2, 2));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.ghostPlayerHeader")));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.configAvailableHeader")));
        gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.chooseReplacementHeader")));
        gridPanel.add(new JLabel(""));
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        gridPanel.add(new JSeparator());
        
        // The rows for the ghost players
        Map<String, BehaviorSettings> savedSettings = game.getBotSettings();
        for (Player ghost : ghostPlayers) {
            // Name
            gridPanel.add(new JLabel(ghost.getName()));
            
            boolean savedSettingsExist = (savedSettings != null) && savedSettings.containsKey(ghost.getName());
            
            // Does it have a princess config saved
            if (savedSettingsExist) {
                gridPanel.add(new JLabel(Messages.getString("ReplacePlayersDialog.configAvailable")));
            } else {
                gridPanel.add(new JLabel(""));                
            }
            
            // The replacement chooser
            var chooser = new JComboBox<>(replacements);
            playerChoosers.put(ghost, chooser);
            if (savedSettingsExist) {
                chooser.setSelectedIndex(PRINCESS_INDEX);
                botConfigs.put(ghost, savedSettings.get(ghost.getName()));
            }
            chooser.addActionListener(e -> updateButtonStates());
            var cPanel = new JPanel();
            cPanel.add(chooser);
            gridPanel.add(cPanel);
            
            // The bot config button
            var button = new JButton(Messages.getString("ReplacePlayersDialog.config"));
            button.addActionListener(e -> callConfig(ghost));
            button.setEnabled(savedSettingsExist);
            configButtons.put(ghost, button);
            var panel = new JPanel();
            panel.add(button);
            gridPanel.add(panel);
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
        for (Player ghost : ghostPlayers) {
            JButton button = configButtons.get(ghost);
            button.setEnabled(playerChoosers.get(ghost).getSelectedIndex() == PRINCESS_INDEX);
        }
    }

    /** 
     * Returns the result of the dialog with respect to selected princess bots.
     * The returned map links zero, one or more BehaviorSettings (a Princess configuration) 
     * to the ghost player name they were chosen for. The returned map only 
     * includes entries for those ghost players that had a Princess Bot replacement selected.
     * The result may be empty, but not null.
     */
    public Map<String, BehaviorSettings> getNewBots() {
        var result = new HashMap<String, BehaviorSettings>();
        for (Player ghost : ghostPlayers) {
            JComboBox<String> chooser = playerChoosers.get(ghost);
            if (chooser.getSelectedIndex() == PRINCESS_INDEX) {
                result.put(ghost.getName(), botConfigs.get(ghost));
            }
        }
        return result;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}