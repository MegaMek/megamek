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
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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

    /**
     * The player the chooser named before its latest change, or {@code null} before the chooser is first filled.
     * Chosen units wait for whoever was named when they were picked, and by the time the chooser reports a change it
     * has already moved on, so the name has to be kept here.
     */
    private String previousPlayerChoice;

    /**
     * {@code true} while this dialog is filling or resetting the chooser itself, so that only a change made by the
     * person using the dialog asks what to do with waiting units.
     */
    private boolean isAdjustingPlayerChooser;

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
        playerChooser.addActionListener(event -> playerChoiceChanged());
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
        boolean isCommitted = commitChosenUnits((String) playerChooser.getSelectedItem());
        if (isCommitted) {
            setVisible(false);
        }
    }

    /**
     * Sends the chosen units of the tab on show to the game for the named player and empties that list; on the Force
     * Generator tab the generated force is cleared with it, since it is now in the game. The Okay button does this
     * for whoever the chooser names; changing the chooser while units are waiting can do it for the player they were
     * waiting for.
     *
     * @param chosenName the chooser entry the units are for, or {@code null} when the chooser is empty, which gives
     *                   them to the local player
     *
     * @return {@code true} when the units were sent; {@code false} when one of them could not be loaded, in which
     *       case nothing was sent and the list is left as it was
     */
    private boolean commitChosenUnits(@Nullable String chosenName) {
        Player owner = permittedPlayerNamed(chosenName);
        if (tabbedPane.getSelectedIndex() == TAB_FORCE_GENERATOR) {
            forceGeneratorPanel.addChosenUnits(owner, clientGui);
            // The Force Generator knows more about what it rolled than any other tab, so it records
            // the same context as the rest rather than being the one source that reports nothing.
            recordGenerationContext(owner);
            // Cleared last, because the context above is read from the tree. The command has gone into the game as
            // this player's force; left on show, the next roll would be folded into it and its units, which are
            // the very objects just sent, could be picked and sent a second time.
            forceGeneratorPanel.clearForce();
            return true;
        }

        ArrayList<Entity> entities = new ArrayList<>(chosenUnitsModel.getAllUnits().size());
        Client skillsClient = clientNamed(chosenName);
        recordGenerationContext(owner);
        for (MekSummary unitSummary : chosenUnitsModel.getAllUnits()) {
            try {
                Entity entity = new MekFileParser(unitSummary.getSourceFile(), unitSummary.getEntryName()).getEntity();

                // skills still come from the chosen bot's own generator where there is one; only who owns
                // the unit has moved, because a remote player has no client here to generate from
                autoSetSkillsAndName(entity, skillsClient, chosenName);
                entity.setOwner(owner);
                if (!client.getGame().getPhase().isLounge()) {
                    entity.setDeployRound(client.getGame().getRoundCount() + 1);
                    entity.setGame(client.getGame());
                    // Set these to true, otherwise units reinforced in the movement turn are considered selectable
                    entity.setDone(true);
                    entity.setUnloaded(true);
                }
                entities.add(entity);
            } catch (EntityLoadingException exception) {
                LOGGER.error(exception, "Unable to load Mek: %s: %s".formatted(unitSummary.getSourceFile(),
                      unitSummary.getEntryName()));
                return false;
            }
        }
        // sent over this machine's own connection whoever the units are for
        client.sendAddEntity(entities);
        String chatMessage = Messages.getString("RandomArmyDialog.loadedUnitsChat",
              client.getLocalPlayer(), owner.getName(), entities.size());
        client.sendServerChat(Player.PLAYER_NONE, chatMessage);
        clearData();
        return true;
    }

    /**
     * Called whenever the player chooser changes. Chosen units are not marked with the player they were picked for:
     * the whole list goes to whoever the chooser names when Okay is pressed. So four units picked for one player,
     * followed by four picked for another, would all reach the second player. When the person using the dialog
     * changes the chooser while units are waiting, they are asked whether those units go to the player they were
     * picked for now, or move to the new player, rather than the move happening in silence.
     */
    private void playerChoiceChanged() {
        if (isAdjustingPlayerChooser) {
            return;
        }
        String newChoice = (String) playerChooser.getSelectedItem();
        if ((newChoice == null) || newChoice.equals(previousPlayerChoice)) {
            return;
        }
        int waitingUnitCount = waitingUnitCount();
        if ((waitingUnitCount == 0) || (previousPlayerChoice == null)) {
            LOGGER.debug("[GMAddUnit] chooser moved from {} to {} with no chosen units waiting, so nothing to ask",
                  previousPlayerChoice, newChoice);
            previousPlayerChoice = newChoice;
            return;
        }

        String addNowChoice = Messages.getString("RandomArmyDialog.switchPlayer.addNow", previousPlayerChoice);
        String moveChoice = Messages.getString("RandomArmyDialog.switchPlayer.moveToNew", newChoice);
        String cancelChoice = Messages.getString("Cancel");
        Object[] choices = { addNowChoice, moveChoice, cancelChoice };
        int answer = JOptionPane.showOptionDialog(this,
              Messages.getString("RandomArmyDialog.switchPlayer.message", waitingUnitCount, previousPlayerChoice,
                    newChoice),
              Messages.getString("RandomArmyDialog.switchPlayer.title"),
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              choices,
              addNowChoice);

        boolean isAddNow = (answer == 0);
        boolean isMove = (answer == 1);
        if (isAddNow) {
            addWaitingUnitsBeforeSwitching(waitingUnitCount, newChoice);
        } else if (isMove) {
            LOGGER.info("[GMAddUnit] {} chosen unit(s) picked for {} were moved to {} by choice",
                  waitingUnitCount, previousPlayerChoice, newChoice);
            previousPlayerChoice = newChoice;
        } else {
            LOGGER.debug("[GMAddUnit] switch from {} to {} cancelled; {} chosen unit(s) stay waiting for {}",
                  previousPlayerChoice, newChoice, waitingUnitCount, previousPlayerChoice);
            selectWithoutAsking(previousPlayerChoice);
        }
    }

    /**
     * Sends the waiting units to the player they were picked for, then lets the chooser move on. If the units cannot
     * be sent, the chooser goes back to the player they wait for.
     *
     * @param waitingUnitCount how many units are waiting, for the log
     * @param newChoice        the chooser entry being switched to
     */
    private void addWaitingUnitsBeforeSwitching(int waitingUnitCount, String newChoice) {
        boolean isCommitted = commitChosenUnits(previousPlayerChoice);
        if (!isCommitted) {
            LOGGER.warn("[GMAddUnit] the {} chosen unit(s) for {} could not be added, so the chooser stays on {}",
                  waitingUnitCount, previousPlayerChoice, previousPlayerChoice);
            selectWithoutAsking(previousPlayerChoice);
            return;
        }
        LOGGER.info("[GMAddUnit] {} chosen unit(s) were added for {} before the chooser moved to {}",
              waitingUnitCount, previousPlayerChoice, newChoice);
        previousPlayerChoice = newChoice;
    }

    /** @return how many chosen units the Okay button would send right now, which depends on the tab on show */
    private int waitingUnitCount() {
        if (tabbedPane.getSelectedIndex() == TAB_FORCE_GENERATOR) {
            return forceGeneratorPanel.getChosenUnits().size();
        }
        return chosenUnitsModel.getAllUnits().size();
    }

    /**
     * Points the chooser at the given entry without {@link #playerChoiceChanged()} treating it as a change made by
     * the person using the dialog.
     *
     * @param playerName the chooser entry to select
     */
    private void selectWithoutAsking(String playerName) {
        isAdjustingPlayerChooser = true;
        try {
            playerChooser.setSelectedItem(playerName);
        } finally {
            isAdjustingPlayerChooser = false;
        }
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
        return permittedPlayerNamed((String) playerChooser.getSelectedItem());
    }

    /**
     * The player a chooser entry stands for, under the rule described at {@link #selectedPlayer()}.
     *
     * @param chosenName the chooser entry, or {@code null} when the chooser is empty
     *
     * @return the named player when the local player may add units to them, otherwise the local player
     */
    private Player permittedPlayerNamed(@Nullable String chosenName) {
        Player localPlayer = client.getLocalPlayer();
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

    /**
     * @param chosenName the chooser entry the units are for, or {@code null} when the chooser is empty
     *
     * @return the client the generated units belong to: the local bot of that name, or this player
     */
    private Client clientNamed(@Nullable String chosenName) {
        if (chosenName != null) {
            Client botClient = (Client) clientGui.getLocalBots().get(chosenName);
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
        // refilling the chooser fires the same change events as a person picking from it
        isAdjustingPlayerChooser = true;
        try {
            refillPlayerChooser(selectionName);
        } finally {
            isAdjustingPlayerChooser = false;
        }
        previousPlayerChoice = (String) playerChooser.getSelectedItem();
    }

    private void refillPlayerChooser(String selectionName) {
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

    /**
     * Rolls a new unit's skills and crew names, where the client preferences ask for them.
     *
     * @param entity       the unit to give skills and crew names
     * @param skillsClient the client whose skill generator rolls the skills
     * @param chosenName   the chooser entry the unit is for, which the name generator is keyed on; passed in rather
     *                     than read from the chooser, which may already name somebody else
     */
    private void autoSetSkillsAndName(Entity entity, Client skillsClient, @Nullable String chosenName) {
        ClientPreferences clientPreferences = PreferenceManager.getClientPreferences();

        Arrays.fill(entity.getCrew().getClanPilots(), entity.isClan());
        if (clientPreferences.useAverageSkills()) {
            skillsClient.getSkillGenerator().setRandomSkills(entity);
        }

        for (int slot = 0; slot < entity.getCrew().getSlotCount(); slot++) {
            if (clientPreferences.generateNames()) {
                Gender gender = RandomGenderGenerator.generate();
                entity.getCrew().setGender(gender, slot);
                String name = RandomNameGenerator.getInstance()
                      .generate(gender, entity.getCrew().isClanPilot(slot), chosenName);
                entity.getCrew().setName(name, slot);
            }
        }
    }
}
