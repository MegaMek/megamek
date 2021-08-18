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

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.IPlayer;

public class ReplacePlayersDialog extends AbstractButtonDialog {
    
    private static final String PRINCESS_STRING = Messages.getString("ReplacePlayersDialog.princess");
    private static final String NOREPLACE_STRING = Messages.getString("ReplacePlayersDialog.noReplacement");
    
    /** A ClientGUI given to the dialog. */
    private final ClientGUI clientGui;
    
    /** Convenience field for clientGui.getClient().getGame(). */
    private final IGame game;
    
    /** The list of displayed ghost players */
    private Set<IPlayer> ghostPlayers;
    
    /** Maps a ghost player to the combobox that sets its replacement */
    private Map<IPlayer, JComboBox<String>> playerChoosers = new HashMap<>();
    
    /** Maps a ghost player to the config button for the bot settings */
    private Map<IPlayer, JButton> configButtons = new HashMap<>();
    
    /** Maps a ghost player to bot settings chosen for it */
    private Map<IPlayer, BehaviorSettings> botConfigs = new HashMap<>();

    protected ReplacePlayersDialog(JFrame frame, ClientGUI cg) {
        super(frame, "ReplacePlayersDialog", "ReplacePlayersDialog.title");
        clientGui = cg;
        game = clientGui.getClient().getGame();
        ghostPlayers = game.getPlayersVector().stream()
                .filter(IPlayer::isGhost).collect(Collectors.toSet());
        // Add a new Princess behavior for each ghost. It will be overwritten by savegame behaviors
        ghostPlayers.forEach(p -> botConfigs.put(p, new BehaviorSettings()));
        initialize();
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        UIUtil.adjustDialog(this.getContentPane());
        finalizeInitialization();
    }

    @Override
    protected Container createCenterPane() {
        // Construct the available replacements for the combobox chooser
        Vector<String> replacements = new Vector<String>();
        replacements.add(NOREPLACE_STRING);
        replacements.add(PRINCESS_STRING);
        
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
        for (IPlayer ghost : ghostPlayers) {
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
            var chooser = new JComboBox<String>(replacements);
            playerChoosers.put(ghost, chooser);
            if (savedSettingsExist) {
                chooser.setSelectedItem(PRINCESS_STRING);
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
    private void callConfig(IPlayer ghost) {
        var bcd = new BotConfigDialog(getFrame(), ghost.getName(), botConfigs.get(ghost), clientGui);
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CONFIRMED) {
            botConfigs.put(ghost, bcd.getBehaviorSettings());
        }
    }
    
    /** Updates the config button enabled states (only enabled when Princess bot is selected). */
    private void updateButtonStates() {
        for (IPlayer ghost : ghostPlayers) {
            JButton button = configButtons.get(ghost);
            button.setEnabled(playerChoosers.get(ghost).getSelectedItem().equals(PRINCESS_STRING));
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
        for (IPlayer ghost : ghostPlayers) {
            JComboBox<String> chooser = playerChoosers.get(ghost);
            if (chooser.getSelectedItem().equals(PRINCESS_STRING)) {
                result.put(ghost.getName(), botConfigs.get(ghost));
            }
        }
        return result;
    }
}