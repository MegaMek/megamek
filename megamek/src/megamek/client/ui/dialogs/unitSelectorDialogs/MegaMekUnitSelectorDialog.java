/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.clientGUI.UnitRecipients;
import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.enums.Gender;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

public class MegaMekUnitSelectorDialog extends AbstractUnitSelectorDialog {

    @Serial
    private static final long serialVersionUID = -5717009055093904636L;
    MMLogger LOGGER = MMLogger.create(MegaMekUnitSelectorDialog.class);
    //region Variable Declarations
    private final ClientGUI clientGUI;
    /**
     * The player a gamemaster tool asked this dialog to open on, or {@code null} when it was opened by itself.
     *
     * <p>Such a player is always offered, even when the ordinary rules would leave them out. A gamemaster who has
     * just put somebody on a team and pressed a reinforcement button has said plainly who the units are for, and the
     * team change does not reach the board until the end of the round - so the rule that hides players who cannot
     * deploy yet would otherwise hide the very person being set up.</p>
     */
    private Player explicitlyRequestedPlayer;

    private final JComboBox<String> comboPlayer = new JComboBox<>();
    private JButton buttonSelectAsset;
    //endregion Variable Declarations

    public MegaMekUnitSelectorDialog(ClientGUI clientGUI, UnitLoadingDialog unitLoadingDialog) {
        super(clientGUI.getFrame(), unitLoadingDialog, true);
        this.clientGUI = clientGUI;

        updateOptionValues();

        initialize();
    }

    @Override
    public void updateOptionValues() {
        gameOptions = clientGUI.getClient().getGame().getOptions();
        enableYearLimits = true;
        allowedYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        canonOnly = gameOptions.booleanOption(OptionsConstants.ALLOWED_CANON_ONLY);
        allowInvalid = gameOptions.booleanOption(OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS);
        gameTechLevel = TechConstants.getSimpleLevel(gameOptions.stringOption("techlevel"));
        eraBasedTechLevel = gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
    }

    //region Button Methods
    @Override
    protected JPanel createButtonsPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel panelButtons = new JPanel(new GridBagLayout());

        buttonSelect = new JButton(Messages.getString("MekSelectorDialog.SelectAsUnit"));
        buttonSelect.setToolTipText(Messages.getString("MekSelectorDialog.SelectAsUnit.ToolTip"));
        buttonSelect.addActionListener(this);
        panelButtons.add(buttonSelect, gbc);

        buttonSelectClose = new JButton(Messages.getString("MekSelectorDialog.m_bPickClose"));
        buttonSelectClose.addActionListener(this);
        panelButtons.add(buttonSelectClose, gbc);

        buttonSelectAsset = new JButton(Messages.getString("MekSelectorDialog.SelectAsAsset"));
        buttonSelectAsset.setToolTipText(Messages.getString("MekSelectorDialog.SelectAsAsset.ToolTip"));
        buttonSelectAsset.addActionListener(e -> selectAsAsset());
        panelButtons.add(buttonSelectAsset, gbc);

        buttonClose = new JButton(Messages.getString("Close"));
        buttonClose.addActionListener(this);
        panelButtons.add(buttonClose, gbc);

        JLabel labelPlayer = new JLabel(Messages.getString("MekSelectorDialog.m_labelPlayer"),
              SwingConstants.RIGHT);
        panelButtons.add(labelPlayer, gbc);

        panelButtons.add(comboPlayer, gbc);

        buttonShowBV = new JButton(Messages.getString("MekSelectorDialog.BV"));
        buttonShowBV.addActionListener(this);
        panelButtons.add(buttonShowBV, gbc);

        return panelButtons;
    }

    @Override
    protected void select(boolean close) {
        addToGame(getSelectedEntities());
        if (close) {
            setVisible(false);
        }
    }

    /**
     * Adds the Battlefield Support Asset form of the current selection to the game (the "Select as Asset" action). Every
     * selected row that has an asset form contributes its asset; the dialog stays open so more can be added.
     */
    private void selectAsAsset() {
        addToGame(getSelectedAssetEntities());
    }

    /**
     * Adds the given entities to the game for the player currently chosen in the player combo, setting their owner and
     * (for non-asset units) auto-generating skills/names, then announcing the addition in chat.
     *
     * @param entities the entities to add (may be empty, in which case nothing happens)
     */
    private void addToGame(ArrayList<Entity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        Player owner = chosenOwner();

        for (Entity entity : entities) {
            autoSetSkillsAndName(entity, owner);
            entity.setOwner(owner);
        }
        // sent over this machine's own connection whoever the units are for, the way reinforcements during a game
        // have always been sent: a remote player has no client here to send through
        clientGUI.getClient().sendAddEntity(entities);

        // named from the owner the units were actually given, not from the chooser: those were two different
        // things to read, and the chat line could name a player who received nothing
        String message = clientGUI.getClient().getLocalPlayer() + " selected "
              + ((entities.size() == 1) ? "a unit" : entities.size() + " units")
              + " for player: " + owner.getName();
        clientGUI.getClient().sendServerChat(Player.PLAYER_NONE, message);
    }

    /**
     * @return the player the chosen units should belong to, which is the local player when the chooser holds
     *       something no longer in the game
     */
    private Player chosenOwner() {
        String chosenName = (String) comboPlayer.getSelectedItem();
        return clientGUI.getClient()
              .getGame()
              .getPlayersList()
              .stream()
              .filter(player -> player.getName().equals(chosenName))
              .findFirst()
              .orElse(clientGUI.getClient().getLocalPlayer());
    }

    private void autoSetSkillsAndName(Entity e, Player player) {
        ClientPreferences cs = PreferenceManager.getClientPreferences();

        Arrays.fill(e.getCrew().getClanPilots(), e.isClan());
        if (e instanceof BattlefieldSupportAsset asset) {
            applyExplicitAssetSkill(asset, isVeteranAssetSkillSelected());
        } else if (cs.useAverageSkills()) {
            clientGUI.getClient().getSkillGenerator().setRandomSkills(e);
        }

        for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
            if (cs.generateNames()) {
                Gender gender = RandomGenderGenerator.generate();
                e.getCrew().setGender(gender, i);
                e.getCrew().setName((player != null)
                      ? RandomNameGenerator.getInstance().generate(gender, e.getCrew().isClanPilot(i), player.getName())
                      : RandomNameGenerator.getInstance().generate(gender, e.getCrew().isClanPilot(i)), i);
            }
        }
    }

    static void applyExplicitAssetSkill(BattlefieldSupportAsset asset, boolean veteranSelected) {
        asset.setVeteranCrew(veteranSelected && asset.hasVeteranProfile());
    }

    private void updatePlayerChoice(String selectionName) {
        comboPlayer.setEnabled(false);
        comboPlayer.removeAllItems();
        List<Player> offered = new ArrayList<>(UnitRecipients.availableTo(clientGUI.getClient().getLocalPlayer(),
              clientGUI.getClient().getGame().getPlayersList(),
              clientGUI.getLocalBots().keySet(),
              !clientGUI.getClient().getGame().getPhase().isLounge()));
        addExplicitlyRequestedPlayer(offered);
        for (Player player : offered) {
            comboPlayer.addItem(player.getName());
        }
        comboPlayer.setSelectedItem(selectionName);
        if (comboPlayer.getSelectedIndex() < 0) {
            // never fall back in silence: units quietly going to the wrong player looks exactly like them going to
            // the right one, and is only noticed a turn later
            LOGGER.warn("[GMAddUnit] {} is not in the player list, so the chooser fell back to {}",
                  selectionName, comboPlayer.getItemAt(0));
            comboPlayer.setSelectedIndex(0);
        }
        if (comboPlayer.getItemCount() > 1) {
            comboPlayer.setEnabled(true);
        }
    }

    /**
     * Puts the player a gamemaster tool asked for into the list when the ordinary rules left them out.
     *
     * @param offered The players the rules offer, added to in place
     */
    private void addExplicitlyRequestedPlayer(List<Player> offered) {
        if (explicitlyRequestedPlayer == null) {
            return;
        }
        boolean alreadyThere = offered.stream()
              .anyMatch(player -> player.getId() == explicitlyRequestedPlayer.getId());
        if (!alreadyThere) {
            LOGGER.info("[GMAddUnit] offering {} because a gamemaster tool asked for them by name",
                  explicitlyRequestedPlayer.getName());
            offered.add(explicitlyRequestedPlayer);
        }
    }

    private void updatePlayerChoice() {
        String lastChoice = (String) comboPlayer.getSelectedItem();
        updatePlayerChoice(lastChoice);
    }

    /**
     * Points the player chooser at the given player, so a dialog opened from a chosen player opens on them.
     *
     * @param player The player to select, or {@code null} to leave the chooser where it was
     */
    public void setPlayerFrom(@Nullable Player player) {
        explicitlyRequestedPlayer = player;
        if (player == null) {
            updatePlayerChoice();
        } else {
            updatePlayerChoice(player.getName());
        }
    }

    /**
     * @param client The client whose player to select, or {@code null} to leave the chooser where it was
     *
     * @deprecated since 0.51.01 - use {@link #setPlayerFrom(Player)}. A client cannot name a remote player,
     *       because there is none on this machine, so anything asking for one silently fell back to the local
     *       player.
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPlayerFromClient(@Nullable Client client) {
        setPlayerFrom((client == null) ? null : client.getLocalPlayer());
    }
    //endregion Button Methods

    @Override
    public @Nullable Entity getSelectedEntity() {
        Entity entity = super.getSelectedEntity();
        // Set game reference (without full restore) so option-dependent state like
        // ProtoMek EI tech level can read game options for the preview display
        if ((entity != null) && (entity.getGame() == null)) {
            entity.setIGame(clientGUI.getClient().getGame());
            entity.recalculateTechAdvancement();
        }
        return entity;
    }

    @Override
    protected Entity refreshUnitView() {
        Entity selectedEntity = super.refreshUnitView();
        if (selectedEntity != null) {
            clientGUI.loadPreviewImage(labelImage, selectedEntity);
        }
        updateSelectButtons();
        return selectedEntity;
    }

    /**
     * Enables the add buttons for the current selection: "Select as Unit"/"Select &amp; Close" require every selected
     * row to have a standard (TW) unit form (no standalone asset); "Select as Asset" requires every selected row to have
     * an asset form.
     */
    private void updateSelectButtons() {
        boolean hasSelection = hasSelectedRows();
        if (buttonSelect != null) {
            buttonSelect.setEnabled(hasSelection);
        }
        if (buttonSelectClose != null) {
            buttonSelectClose.setEnabled(hasSelection);
        }
        if (buttonSelectAsset != null) {
            buttonSelectAsset.setEnabled(selectionCanSelectAsAsset());
        }
    }

    @Override
    public void run() {
        super.run();
        // In some cases, it's possible to get here without an initialized
        // instance (loading a saved game without a cache).  In these cases,
        // we don't care about the failed loads.
        if (mscInstance.isInitialized()) {
            final Map<String, String> hFailedFiles = MekSummaryCache.getInstance().getFailedFiles();
            if ((hFailedFiles != null) && !hFailedFiles.isEmpty()) {
                LOGGER.warn("Unit loading errors: {}", hFailedFiles);
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        updatePlayerChoice();
        comboPlayer.grabFocus();
        super.setVisible(visible);
    }
}
