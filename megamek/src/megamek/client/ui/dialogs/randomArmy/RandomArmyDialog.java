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

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ratgenerator.GenerationContext;
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
            Client selectedClient = selectedClient();
            forceGeneratorPanel.addChosenUnits((String) playerChooser.getSelectedItem(), clientGui);
            // The Force Generator knows more about what it rolled than any other tab, so it records
            // the same context as the rest rather than being the one source that reports nothing.
            recordGenerationContext(selectedClient);
        } else {
            ArrayList<Entity> entities = new ArrayList<>(chosenUnitsModel.getAllUnits().size());
            Client selectedClient = selectedClient();
            recordGenerationContext(selectedClient);
            for (MekSummary ms : chosenUnitsModel.getAllUnits()) {
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

    /** @return the client the generated units belong to: a chosen local bot, or this player */
    private Client selectedClient() {
        if (playerChooser.getSelectedIndex() > 0) {
            Client botClient = (Client) clientGui.getLocalBots().get((String) playerChooser.getSelectedItem());
            if (botClient != null) {
                return botClient;
            }
        }
        return client;
    }

    /**
     * Remembers what the units were rolled for, so later work can organize them the way that faction
     * organizes its own. The team faction is set from the same context, which is what the munition
     * autoconfigurator and the name generator read.
     */
    private void recordGenerationContext(Client selectedClient) {
        GenerationContext context = getGenerationContext();
        Player owner = selectedClient.getLocalPlayer();

        // Only a generator that asked the player anything gets recorded. A tab that knows nothing
        // has nothing to say, and recording its default would erase a real choice made on an earlier
        // roll - topping a ComStar force up from the plain RAT tab would file it as Inner Sphere.
        if (context.source() != GenerationContext.Source.UNSPECIFIED) {
            clientGui.setGenerationContext(owner.getId(), context);
            clientGui.getClient().getGame().getTeamForPlayer(owner).setFaction(context.faction());
            // The year goes in as text: message formatting would group the digits into "3,067".
            String year = String.valueOf(context.year());
            String message = (context.rating() == null)
                  ? Messages.getString("RandomArmyDialog.generatedFor",
                        clientGui.getClient().getLocalPlayer().getName(), owner.getName(),
                        context.factionDisplayName(), year)
                  : Messages.getString("RandomArmyDialog.generatedForRated",
                        clientGui.getClient().getLocalPlayer().getName(), owner.getName(),
                        context.factionDisplayName(), year, context.rating());
            clientGui.getClient().sendServerChat(Player.PLAYER_NONE, message);
        }
        LOGGER.debug("[ForceGen][Context] {} for player {}: {}", context.source(), owner.getName(),
              context.describe());
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
            // Re-pull the current game options every time the dialog is opened so the Force
            // Generator's Year field defaults to the current game year. The dialog is constructed
            // once at ClientGUI startup, before the user has set the year in the lobby, and the
            // gameListener is only installed on first show — so without this, year changes made
            // before the first open never propagate. Still user-editable after defaulting.
            setGameOptions(client.getGame().getOptions());
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
                    setGameOptions(client.getGame().getOptions());
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
