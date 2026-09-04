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
import java.util.List;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ratgenerator.ExistingLift;
import megamek.client.ratgenerator.GenerationContext;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.UnitRecipients;
import megamek.client.ui.dialogs.buttonDialogs.SkillGenerationDialog;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.annotations.Nullable;
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
    /**
     * The player a caller asked this dialog to open on, or {@code null} when it was opened by itself.
     *
     * <p>{@link UnitRecipients} decides what being asked for is worth: a gamemaster tool naming a player who is on
     * no team yet gets them offered anyway, while an ordinary player whose lobby happened to have the host
     * highlighted does not get the host. The decision is made there, once, so that this dialog and the unit
     * selector can never answer it differently.</p>
     */
    private Player explicitlyRequestedPlayer;

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
        forceGeneratorPanel.setHostLiftSupplier(this::liftAlreadyInGame);
    }

    /**
     * The free bays and docking collars on the ships the chosen player already has in the game, so a force generated
     * on top of an earlier one draws only the lift it still lacks.
     *
     * @return the lift already in the game for the player who will own the units
     */
    private ExistingLift liftAlreadyInGame() {
        Player owner = selectedPlayer();
        List<Entity> owned = client.getGame().getEntitiesVector().stream()
              .filter(entity -> entity.getOwnerId() == owner.getId())
              .toList();
        return ExistingLift.of(owned);
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
            forceGeneratorPanel.addChosenUnits(selectedPlayer(), clientGui);
            // The Force Generator knows more about what it rolled than any other tab, so it records
            // the same context as the rest rather than being the one source that reports nothing.
            recordGenerationContext(selectedPlayer());
        } else {
            ArrayList<Entity> entities = new ArrayList<>(chosenUnitsModel.getAllUnits().size());
            Client selectedClient = selectedClient();
            Player owner = selectedPlayer();
            recordGenerationContext(owner);
            for (MekSummary ms : chosenUnitsModel.getAllUnits()) {
                try {
                    Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

                    // skills still come from the chosen bot's own generator where there is one; only who owns
                    // the unit has moved, because a remote player has no client here to generate from
                    autoSetSkillsAndName(entity, selectedClient);
                    entity.setOwner(owner);
                    if (!client.getGame().getPhase().isLounge()) {
                        entity.setDeployRound(client.getGame().getRoundCount() + 1);
                        entity.setGame(client.getGame());
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
            // sent over this machine's own connection whoever the units are for
            client.sendAddEntity(entities);
            String msg = "%s loaded Units from Random Army for player: %s [%d units]"
                  .formatted(client.getLocalPlayer(), owner.getName(), entities.size());
            client.sendServerChat(Player.PLAYER_NONE, msg);
            clearData();
        }

        setVisible(false);
    }

    /**
     * The player the generated units should belong to.
     *
     * <p>That is whoever the chooser names, when they are still in the game and the local player may add units to
     * them. Otherwise it is the local player: the chooser only offers permitted players, so anything else here
     * means the game changed under the open dialog, and units must not go to somebody else on the strength of a
     * stale entry.</p>
     *
     * @return the player who will own the units
     */
    private Player selectedPlayer() {
        Player localPlayer = client.getLocalPlayer();
        String chosenName = (String) playerChooser.getSelectedItem();
        Player chosen = client.getGame()
              .getPlayersList()
              .stream()
              .filter(player -> player.getName().equals(chosenName))
              .findFirst()
              .orElse(null);
        if (chosen == null) {
            LOGGER.warn("[GMAddUnit] the chooser names {}, who is no longer in the game; the units go to {} instead",
                  chosenName, localPlayer.getName());
            return localPlayer;
        }
        boolean isPermitted = UnitRecipients.mayAddUnitsTo(localPlayer, chosen, clientGui.getLocalBots().keySet());
        if (!isPermitted) {
            LOGGER.warn("[GMAddUnit] the chooser names {}, whom {} may not add units to; the units go to {} instead",
                  chosen.getName(), localPlayer.getName(), localPlayer.getName());
            return localPlayer;
        }
        LOGGER.info("[GMAddUnit] {} is generating units owned by {}, sent over their own connection",
              localPlayer.getName(), chosen.getName());
        return chosen;
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
    private void recordGenerationContext(Player owner) {
        GenerationContext context = getGenerationContext();

        // Only a generator that asked the player anything gets recorded. A tab that knows nothing
        // has nothing to say, and recording its default would erase a real choice made on an earlier
        // roll - topping a ComStar force up from the plain RAT tab would file it as Inner Sphere.
        if (context.source() != GenerationContext.Source.UNSPECIFIED) {
            clientGui.setGenerationContext(owner.getId(), context);
            // a player on no team has no team faction to set, which is the ordinary state of an observer being
            // handed their first force: the faction is recorded against the player either way, and the team picks
            // it up when they are put on one
            Team team = clientGui.getClient().getGame().getTeamForPlayer(owner);
            if (team != null) {
                team.setFaction(context.faction());
            } else {
                LOGGER.info("[GMAddUnit] {} is on no team, so the rolled faction {} is recorded against the player "
                            + "only", owner.getName(), context.faction());
            }
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
        playerChooser.setEnabled(false);
        playerChooser.removeAllItems();
        List<Player> offered = UnitRecipients.availableTo(client.getLocalPlayer(),
              client.getGame().getPlayersList(),
              clientGui.getLocalBots().keySet(),
              !client.getGame().getPhase().isLounge(),
              explicitlyRequestedPlayer);
        for (Player player : offered) {
            playerChooser.addItem(player.getName());
        }
        if (playerChooser.getItemCount() > 1) {
            playerChooser.setEnabled(true);
        }
        if (selectionName == null) {
            // the first opening has no previous choice to keep, and the local player is always first
            playerChooser.setSelectedIndex(0);
            LOGGER.debug("[GMAddUnit] no previous choice, so the chooser starts on {}", playerChooser.getItemAt(0));
        } else {
            playerChooser.setSelectedItem(selectionName);
        }
        if (playerChooser.getSelectedIndex() < 0) {
            // never fall back in silence: units quietly going to the wrong player looks exactly like them going to
            // the right one, and is only noticed a turn later
            LOGGER.warn("[GMAddUnit] {} is not in the player list, so the chooser fell back to {}",
                  selectionName, playerChooser.getItemAt(0));
            playerChooser.setSelectedIndex(0);
        }
    }

    private void updatePlayerChoice() {
        String lastChoice = (String) playerChooser.getSelectedItem();
        updatePlayerChoice(lastChoice);
    }

    /**
     * Points the player chooser at the given player, so a dialog opened from a chosen player opens on them.
     *
     * <p>Asking is not the same as getting: if the local player may not add units to that player, the chooser is
     * left on the person using it, and the log says why.</p>
     *
     * @param player The player to select, or {@code null} to leave the chooser where it was
     */
    public void setPlayerFrom(@Nullable Player player) {
        explicitlyRequestedPlayer = player;
        if (player == null) {
            LOGGER.debug("[GMAddUnit] random army dialog opened with no player asked for; the chooser stays where "
                  + "it was");
            updatePlayerChoice();
        } else {
            LOGGER.debug("[GMAddUnit] random army dialog opened asking for {}", player.getName());
            updatePlayerChoice(player.getName());
        }
    }

    /**
     * @param clientToSelect The client whose player to select, or {@code null} to leave the chooser where it was
     *
     * @deprecated since 0.51.01 - use {@link #setPlayerFrom(Player)}. A client cannot name a remote player,
     *       because there is none on this machine, so anything asking for one silently fell back to the local
     *       player.
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPlayerFromClient(@Nullable Client clientToSelect) {
        // named apart from this dialog's own client field, which it would otherwise shadow
        setPlayerFrom((clientToSelect == null) ? null : clientToSelect.getLocalPlayer());
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
