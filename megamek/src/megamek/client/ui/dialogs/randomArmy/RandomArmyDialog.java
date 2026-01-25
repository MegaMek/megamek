/*
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.randomArmy;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.buttonDialogs.SkillGenerationDialog;
import megamek.common.Player;
import megamek.common.enums.Gender;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * This is the random army dialog shown in MM's lobby and game (reinforcement), where the system was initially
 * developed. This dialog is tied to the ClientGUI, Client and Game states.
 */
public class RandomArmyDialog extends AbstractRandomArmyDialog {
    private static final MMLogger LOGGER = MMLogger.create(RandomArmyDialog.class);

    private final ClientGUI clientGui;
    private final Client client;
    private GameListener gameListener;

    private final JComboBox<String> playerChooser = new JComboBox<>();

    private final JButton okButton = new JButton(Messages.getString("Okay"));
    private final JButton cancelButton = new JButton(Messages.getString("Cancel"));
    private final JButton skillsButton = new JButton(Messages.getString("SkillGenerationDialog.title"));

    /**
     * Creates a random army dialog for the given parent frame and ClientGUI. This dialog is specialized towards use in
     * MM's lobby and game.
     *
     * @param parent    A parent frame for the dialog
     * @param clientGui The ClientGUI this dialog operates on
     */
    public RandomArmyDialog(JFrame parent, ClientGUI clientGui) {
        super(parent);
        this.clientGui = clientGui;
        client = clientGui.getClient();
        setGameOptions(client.getGame().getOptions());
        tabbedPane.addChangeListener(
              ev -> skillsButton.setEnabled(tabbedPane.getSelectedIndex() != TAB_FORCE_GENERATOR));
    }

    @Override
    protected JComponent createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        okButton.addActionListener(e -> okAction());
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(e -> cancelAction());
        skillsButton.addActionListener(e ->
              new SkillGenerationDialog(parentFrame, clientGui, new ArrayList<>()).showDialog());
        JLabel labelPlayer = new JLabel(Messages.getString("RandomArmyDialog.Player"), SwingConstants.RIGHT);
        buttonPanel.add(labelPlayer);
        buttonPanel.add(playerChooser);
        buttonPanel.add(skillsButton);
        return buttonPanel;
    }

    private void cancelAction() {
        clearData();
        setVisible(false);
    }

    private void okAction() {
        if (tabbedPane.getSelectedIndex() == TAB_FORCE_GENERATOR) {
            m_pForceGen.addChosenUnits((String) playerChooser.getSelectedItem(), clientGui);
        } else {
            ArrayList<Entity> entities = new ArrayList<>(armyModel.getAllUnits().size());
            Client selectedClient = null;
            if (playerChooser.getSelectedIndex() > 0) {
                String name = (String) playerChooser.getSelectedItem();
                selectedClient = (Client) clientGui.getLocalBots().get(name);
            }
            if (selectedClient == null) {
                selectedClient = this.client;
            }
            if (m_pFormationOptions.getFaction() != null) {
                // Set faction based on generated RAT faction
                String faction = m_pFormationOptions.getFaction().getKey();
                clientGui.getClient().getGame().getTeamForPlayer(selectedClient.getLocalPlayer()).setFaction(faction);
                String msg = clientGui.getClient().getLocalPlayer() + " set team Faction to: " + faction;
                clientGui.getClient().sendServerChat(Player.PLAYER_NONE, msg);
            }
            for (MekSummary ms : armyModel.getAllUnits()) {
                try {
                    Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

                    autoSetSkillsAndName(entity, selectedClient);
                    entity.setOwner(selectedClient.getLocalPlayer());
                    if (!selectedClient.getGame().getPhase().isLounge()) {
                        entity.setDeployRound(selectedClient.getGame().getRoundCount() + 1);
                        entity.setGame(selectedClient.getGame());
                        // Set these to true, otherwise units reinforced in the movement turn are considered selectable
                        entity.setDone(true);
                        entity.setUnloaded(true);
                    }
                    entities.add(entity);
                } catch (EntityLoadingException ex) {
                    LOGGER.error(ex, "Unable to load Mek: %s: %s".formatted(ms.getSourceFile(), ms.getEntryName()));
                    return;
                }
            }
            selectedClient.sendAddEntity(entities);
            String msg = "%s loaded Units from Random Army for player: %s [%d units]"
                  .formatted(client.getLocalPlayer(), playerChooser.getSelectedItem(), entities.size());
            client.sendServerChat(Player.PLAYER_NONE, msg);
            clearData();
        }

        setVisible(false);
    }

    private void updatePlayerChoice(String selectionName) {
        String clientName = client.getName();
        playerChooser.setEnabled(false);
        playerChooser.removeAllItems();
        playerChooser.addItem(clientName);
        for (AbstractClient botClient : clientGui.getLocalBots().values()) {
            Player player = client.getGame().getPlayer(botClient.getLocalPlayerNumber());

            if (!player.isObserver()) {
                playerChooser.addItem(botClient.getName());
            }
        }
        if (playerChooser.getItemCount() > 1) {
            playerChooser.setEnabled(true);
        }
        playerChooser.setSelectedItem(selectionName);
        if (playerChooser.getSelectedIndex() < 0) {
            playerChooser.setSelectedIndex(0);
        }
    }

    private void updatePlayerChoice() {
        String lastChoice = (String) playerChooser.getSelectedItem();
        updatePlayerChoice(lastChoice);
    }

    public void setPlayerFromClient(Client c) {
        if (c != null) {
            updatePlayerChoice(c.getName());
        } else {
            updatePlayerChoice();
        }
    }

    @Override
    public void setVisible(boolean show) {
        if (show) {
            updatePlayerChoice();
            if (gameListener == null) {
                installGameListener();
            }
        }

        playerChooser.grabFocus();
        super.setVisible(show);
    }

    @Override
    public void dispose() {
        try {
            client.getGame().removeGameListener(gameListener);
        } finally {
            super.dispose();
        }
    }

    private void installGameListener() {
        gameListener = new GameListenerAdapter() {
            @Override
            public void gameSettingsChange(GameSettingsChangeEvent evt) {
                if (!evt.isMapSettingsOnlyChange()) {
                    updateRATYear();
                }
            }
        };
        client.getGame().addGameListener(gameListener);
    }

    private void autoSetSkillsAndName(Entity e, Client client) {
        ClientPreferences cs = PreferenceManager.getClientPreferences();

        Arrays.fill(e.getCrew().getClanPilots(), e.isClan());
        if (cs.useAverageSkills()) {
            client.getSkillGenerator().setRandomSkills(e);
        }

        String faction = (String) playerChooser.getSelectedItem();
        for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
            if (cs.generateNames()) {
                Gender gender = RandomGenderGenerator.generate();
                e.getCrew().setGender(gender, i);
                String name = RandomNameGenerator.getInstance().generate(gender, e.getCrew().isClanPilot(i), faction);
                e.getCrew().setName(name, i);
            }
        }
    }
}
