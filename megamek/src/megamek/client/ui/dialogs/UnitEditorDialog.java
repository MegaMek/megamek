/*
 * Copyright (C) 2013 Jay Lawson
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.common.Player;
import megamek.client.ui.dialogs.unitEditor.DamageEditorDiagram;
import megamek.client.ui.dialogs.unitEditor.PreExistingDamageRoller;
import megamek.client.ui.dialogs.unitEditor.UnitDamageControls;
import megamek.client.ui.dialogs.unitEditor.UnitDamagePanelBuilder;
import megamek.client.ui.dialogs.unitEditor.UnitDamageSpecBuilder;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.compute.damage.PreExistingDamageApplier;
import megamek.common.compute.damage.PreExistingDamageLevel;
import megamek.common.units.*;
import megamek.logging.MMLogger;

/**
 * This dialog will allow the user to edit the damage and status characteristics of a unit. This is designed for use in
 * both MegaMek and MHQ so don't go messing things up for MHQ by changing a bunch of stuff
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitEditorDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(UnitEditorDialog.class);

    @Serial
    private static final long serialVersionUID = 8144354264100884817L;

    private final Entity entity;

    /** Names the dialog for the stored size and position. */
    private static final String DIALOG_NAME = "unitEditorDialog";

    /** The gamemaster commands that have the server take a unit out of play, as used by the map menu. */
    private static final String DESTROY_UNIT_COMMAND = "/kill %d %b";
    /** The server calls it rescue; to a player it is the unit withdrawing from the battlefield. */
    private static final String WITHDRAW_UNIT_COMMAND = "/rescue %d";
    /** Hands the unit to another player, by unit id then player id. */
    private static final String CHANGE_OWNER_COMMAND = "/changeOwner %d %d";

    /** Guards the owner chooser against the listener firing again while its value is put back after a cancel. */
    private boolean reassigningOwner;

    /** The smallest the dialog may be resized to, before GUI scaling, so the paperdoll and panel stay usable. */
    private static final int MIN_DIALOG_WIDTH = 650;
    private static final int MIN_DIALOG_HEIGHT = 500;

    /** The editor's controls, built by the panel builder and read back by the applier. */
    private final UnitDamageControls controls = new UnitDamageControls();
    /** Builds the controls; also used to reach the panel a location's controls live in. */
    private UnitDamagePanelBuilder panelBuilder;

    /** The armor diagram beside the location panels; absent for conventional infantry, which has no diagram. */
    private DamageEditorDiagram diagram;

    /** The pre-existing damage roller; only present when the pre-existing damage panel is shown. */
    private PreExistingDamageRoller preExistingDamageRoller;
    /** The pre-existing damage level chooser (FSW p.144). */
    private JComboBox<PreExistingDamageLevel> choicePreExistingDamage;

    /** Whether to offer the gamemaster editing tools: refilling ammo bins and the pre-existing damage roller. */
    private final boolean offerGameMasterTools;

    /** The client to destroy the unit through; {@code null} where there is no game, as in MekHQ and MegaMekLab. */
    private final Client client;

    /** The window to center on the first time this dialog is opened, before it has a remembered position. */
    private final JFrame parent;

    /**
     * Opens the dialog without the pre-existing damage roller. Used where there is no gamemaster to speak of, such as
     * MekHQ and MegaMekLab.
     *
     * @param parent the parent frame
     * @param entity the unit to edit damage for
     */
    public UnitEditorDialog(JFrame parent, Entity entity) {
        this(parent, entity, false);
    }

    /**
     * @param parent     the parent frame
     * @param entity     the unit to edit damage for
     * @param offerGameMasterTools {@code true} to offer the gamemaster editing tools: refilling ammo bins and the
     *                             pre-existing damage roller (FSW p.144)
     */
    public UnitEditorDialog(JFrame parent, Entity entity, boolean offerGameMasterTools) {
        this(parent, entity, offerGameMasterTools, null);
    }

    /**
     * @param parent     the parent frame
     * @param entity     the unit to edit damage for
     * @param offerGameMasterTools {@code true} to offer the gamemaster editing tools: refilling ammo bins and the
     *                             pre-existing damage roller (FSW p.144)
     * @param client     the client to destroy the unit through, or {@code null} where there is no game to destroy it
     *                   in, such as in MekHQ and MegaMekLab. Without one, Destroy Unit is not offered.
     */
    public UnitEditorDialog(JFrame parent, Entity entity, boolean offerGameMasterTools, @Nullable Client client) {
        super(parent, true);
        this.entity = entity;
        this.offerGameMasterTools = offerGameMasterTools;
        this.client = client;
        this.parent = parent;
        initComponents();
    }

    private void initComponents() {
        getContentPane().setLayout(new BorderLayout());

        setTitle("Edit damage for " + entity.getDisplayName());

        JPanel panMain = new JPanel(new GridBagLayout());
        JPanel panButtons = new JPanel(new GridLayout(1, 2));

        // refilling ammo is a gamemaster's business, and MegaMek's alone: MekHQ counts what is in a bin itself
        panelBuilder = new UnitDamagePanelBuilder(entity, controls, offerGameMasterTools);
        if (entity.isConventionalInfantry()) {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            panMain.add(panelBuilder.initInfantryPanel(), gridBagConstraints);
        } else {
            panelBuilder.build();
            addOwnerReassign();
            // after the owner row, so the modifiers form the general panel's last column instead of trapping the
            // owner chooser in the middle of their column
            panelBuilder.addSkillModifiersColumn();
            diagram = new DamageEditorDiagram(entity, controls);
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.insets = new Insets(4, 4, 4, 4);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            panMain.add(diagram, gridBagConstraints);
        }

        getContentPane().add(new JScrollPane(panMain), BorderLayout.CENTER);

        JButton butOK = new JButton(Messages.getString("Okay"));
        butOK.addActionListener(evt -> commitDamageAndClose());
        JButton butCancel = new JButton(Messages.getString("Cancel"));
        butCancel.addActionListener(evt -> setVisible(false));

        panButtons.add(butOK);
        panButtons.add(butCancel);

        getContentPane().add(panButtons, BorderLayout.PAGE_END);

        if (offerGameMasterTools) {
            preExistingDamageRoller = new PreExistingDamageRoller(entity, controls);
            getContentPane().add(initGamemasterActionPanel(), BorderLayout.PAGE_START);
            preExistingDamageRoller.captureSnapshot(getContentPane());
        }

        String closeAction = "closeAction";
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
        getRootPane().getActionMap().put(closeAction, new CloseAction(this));

        // The armor diagram builds its map sets when it becomes displayable, so it can only be drawn after the
        // first pack. It sizes itself to its content, so pack again once it has been drawn and enlarged.
        pack();
        if (diagram != null) {
            diagram.refresh();
            diagram.enlargeToFillDialog();
        }
        pack();

        // leave room for scroll bars and never grow beyond the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(getWidth() + UIUtil.scaleForGUI(30), (int) (screenSize.width * 0.9)),
              Math.min(getHeight() + UIUtil.scaleForGUI(30), (int) (screenSize.height * 0.9)));

        // Keep a usable floor when the user resizes, but never force the dialog larger than what already fits.
        Dimension minimumSize = UIUtil.scaleForGUI(MIN_DIALOG_WIDTH, MIN_DIALOG_HEIGHT);
        minimumSize.width = Math.min(minimumSize.width, getWidth());
        minimumSize.height = Math.min(minimumSize.height, getHeight());
        setMinimumSize(minimumSize);

        // Center on the parent first, for the first time the dialog is ever opened. setPreferences comes after, so
        // that a size, position and divider the user chose before win over both the centering and the size worked
        // out above; centering afterwards would throw the remembered position away.
        setLocationRelativeTo(parent);
        setPreferences();
    }

    /**
     * Restores the size, position and divider location the user last left the dialog with, and keeps them up to
     * date as the dialog is moved, resized and its divider dragged.
     */
    private void setPreferences() {
        try {
            setName(DIALOG_NAME);
            PreferencesNode preferences = MegaMek.getMMPreferences().forClass(UnitEditorDialog.class);
            preferences.manage(new JWindowPreference(this));
            if (diagram != null) {
                preferences.manage(new JSplitPanePreference(diagram));
            }
        } catch (Exception ex) {
            // a dialog that cannot remember where it was is still perfectly usable
            LOGGER.error(ex, "Could not set the preferences of the unit editor dialog");
        }
    }

    /**
     * The gamemaster action toolbar. It always carries the general actions - restore, withdraw and destroy - and
     * adds the pre-existing damage roller only for the unit types the FSW rules cover, since assigning pre-existing
     * damage is a scenario-setup decision that does not fit every unit (a warship, for one). So a unit without the
     * roller still shows the general actions, rather than losing the whole toolbar with it.
     */
    private JPanel initGamemasterActionPanel() {
        JPanel panActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        boolean preExistingSupported = PreExistingDamageApplier.isSupported(entity);
        panActions.setBorder(preExistingSupported
              ? BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.preExistingDamage"))
              : BorderFactory.createEtchedBorder());

        if (preExistingSupported) {
            choicePreExistingDamage = new JComboBox<>(PreExistingDamageLevel.values());
            choicePreExistingDamage.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                      boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof PreExistingDamageLevel damageLevel) {
                        setText(Messages.getString("UnitEditorDialog.preExistingDamage." + damageLevel.name()));
                    }
                    return this;
                }
            });
            choicePreExistingDamage.setToolTipText(Messages.getString("UnitEditorDialog.preExistingDamage.tooltip"));
            panActions.add(choicePreExistingDamage);

            JButton butRollPreExisting = new JButton(Messages.getString("UnitEditorDialog.preExistingDamage.roll"));
            butRollPreExisting.setToolTipText(Messages.getString("UnitEditorDialog.preExistingDamage.roll.tooltip"));
            butRollPreExisting.addActionListener(event ->
                  preExistingDamageRoller.roll((PreExistingDamageLevel) choicePreExistingDamage.getSelectedItem()));
            panActions.add(butRollPreExisting);

            JButton butApplyPreExisting = new JButton(Messages.getString("UnitEditorDialog.preExistingDamage.apply"));
            butApplyPreExisting.setToolTipText(Messages.getString("UnitEditorDialog.preExistingDamage.apply.tooltip"));
            butApplyPreExisting.addActionListener(event -> commitDamageAndClose());
            panActions.add(butApplyPreExisting);
        }

        JButton butRestoreUnit = new JButton(Messages.getString("UnitEditorDialog.preExistingDamage.reset"));
        butRestoreUnit.setToolTipText(Messages.getString("UnitEditorDialog.preExistingDamage.reset.tooltip"));
        butRestoreUnit.addActionListener(event -> preExistingDamageRoller.restoreToFactoryNew(getContentPane()));
        panActions.add(butRestoreUnit);

        // In the lobby a unit that is not wanted is simply removed, and there is nothing in play to take off the
        // board. Without a client there is no server to take it off either, as in MekHQ and MegaMekLab.
        boolean canRemoveFromPlay = (client != null)
              && (entity.getGame() != null)
              && !entity.getGame().getPhase().isLounge();

        JButton butWithdrawUnit = new JButton(Messages.getString("UnitEditorDialog.withdrawUnit"));
        butWithdrawUnit.setToolTipText(Messages.getString(canRemoveFromPlay
              ? "UnitEditorDialog.withdrawUnit.tooltip"
              : "UnitEditorDialog.withdrawUnit.tooltip.lobby"));
        butWithdrawUnit.setEnabled(canRemoveFromPlay);
        butWithdrawUnit.addActionListener(event -> withdrawUnit());
        panActions.add(butWithdrawUnit);

        JButton butDestroyUnit = new JButton(Messages.getString("UnitEditorDialog.destroyUnit"));
        butDestroyUnit.setToolTipText(Messages.getString(canRemoveFromPlay
              ? "UnitEditorDialog.destroyUnit.tooltip"
              : "UnitEditorDialog.destroyUnit.tooltip.lobby"));
        butDestroyUnit.setEnabled(canRemoveFromPlay);
        butDestroyUnit.addActionListener(event -> destroyUnit());
        panActions.add(butDestroyUnit);
        return panActions;
    }

    /**
     * Commits the shown damage and closes the dialog. Shared by Okay and by the Apply button. In play the edits go
     * to the server as plain values, which applies them to its own authoritative copy of the unit and sends the
     * result back to every client; the unit here stays untouched. In the lobby, and without a game as in MekHQ,
     * they are applied to the unit directly and the caller sends the edited unit on as it always has.
     */
    private void commitDamageAndClose() {
        DamageEditSpec spec = new UnitDamageSpecBuilder(entity, controls).build();
        if (commitsThroughServer()) {
            client.sendDamageEdit(spec);
        } else {
            new DamageEditApplier(entity, spec).applyToEntity();
        }
        setVisible(false);
    }

    /**
     * Whether the edits are committed by sending them to the server rather than applied to the local unit: only in
     * a running game, where the server's copy of the unit is the authoritative one.
     */
    private boolean commitsThroughServer() {
        return (client != null) && (entity.getGame() != null) && !entity.getGame().getPhase().isLounge();
    }

    /**
     * Asks first, then withdraws the unit from the battlefield, through the server's rescue command. The unit flees
     * the board and leaves the game intact: crew, unit, and anything it carries, counted as retreated rather than
     * destroyed. It is the opposite of destroying it, and it does not bring anything back onto the board.
     */
    private void withdrawUnit() {
        int choice = JOptionPane.showConfirmDialog(this,
              String.format(Messages.getString("UnitEditorDialog.withdrawUnit.confirm"), entity.getDisplayName()),
              Messages.getString("UnitEditorDialog.withdrawUnit"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        if (client == null) {
            LOGGER.error("Cannot withdraw {}: the damage editor was opened without a client", entity.getDisplayName());
            return;
        }
        LOGGER.info("Withdrawing {} from the battlefield at the request of the damage editor", entity.getDisplayName());
        client.sendChat(String.format(WITHDRAW_UNIT_COMMAND, entity.getId()));
        setVisible(false);
    }

    /**
     * Asks first, then has the server destroy the unit, through the same gamemaster command the map menu uses. The
     * server is what destroys a unit: it writes the reports, decides who dies with it and who escapes, and takes it
     * off the board. Zeroing the unit's armor here would leave it standing.
     */
    private void destroyUnit() {
        JCheckBox chkEjectCrew = new JCheckBox(Messages.getString("UnitEditorDialog.destroyUnit.eject"));
        chkEjectCrew.setToolTipText(Messages.getString("UnitEditorDialog.destroyUnit.eject.tooltip"));
        chkEjectCrew.setEnabled(entity.canEjectCrew());
        Object[] message = {
              String.format(Messages.getString("UnitEditorDialog.destroyUnit.confirm"), entity.getDisplayName()),
              chkEjectCrew
        };
        int choice = JOptionPane.showConfirmDialog(this,
              message,
              Messages.getString("UnitEditorDialog.destroyUnit"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        if (client == null) {
            LOGGER.error("Cannot destroy {}: the damage editor was opened without a client",
                  entity.getDisplayName());
            return;
        }
        boolean ejectCrew = chkEjectCrew.isEnabled() && chkEjectCrew.isSelected();
        LOGGER.info("Destroying {} at the request of the damage editor, ejecting crew: {}",
              entity.getDisplayName(),
              ejectCrew);
        client.sendChat(String.format(DESTROY_UNIT_COMMAND, entity.getId(), ejectCrew));
        setVisible(false);
    }

    /**
     * Adds an Owner chooser to the general panel that hands the unit to another player. It is offered only in the
     * gamemaster dialog and only in a running game, where there is a server to make the change and more than one
     * player to hand the unit to. Choosing another player asks first, then reassigns the unit.
     */
    private void addOwnerReassign() {
        if (!offerGameMasterTools || (client == null) || (controls.panGeneral == null) || (entity.getGame() == null)) {
            return;
        }
        List<Player> owners = new ArrayList<>();
        for (Player player : entity.getGame().getPlayersList()) {
            if (player.getTeam() != Player.TEAM_UNASSIGNED) {
                owners.add(player);
            }
        }
        Player currentOwner = entity.getOwner();
        // there has to be a current owner to start from and at least one other player to hand the unit to
        if (!owners.contains(currentOwner) || (owners.size() < 2)) {
            return;
        }

        JComboBox<Player> comboOwner = new JComboBox<>(owners.toArray(new Player[0]));
        comboOwner.setSelectedItem(currentOwner);
        comboOwner.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Player player) {
                    setText(player.getName());
                }
                return this;
            }
        });
        comboOwner.addActionListener(event -> reassignOwner(comboOwner, currentOwner));
        panelBuilder.addLabeledRow(controls.panGeneral, Messages.getString("UnitEditorDialog.owner"), comboOwner);
    }

    /**
     * Asks first, then hands the unit to the chosen player through the same gamemaster command the map menu uses.
     * On cancel the chooser is put back to the current owner, without letting that put-back ask again.
     */
    private void reassignOwner(JComboBox<Player> comboOwner, Player currentOwner) {
        if (reassigningOwner) {
            return;
        }
        if (!(comboOwner.getSelectedItem() instanceof Player chosen) || (chosen.getId() == currentOwner.getId())) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
              Messages.getString("Gamemaster.Traitor.confirmation",
                    entity.getDisplayName(),
                    chosen.getName()),
              Messages.getString("Gamemaster.Traitor.confirm"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            LOGGER.info("Reassigning {} to {} at the request of the damage editor",
                  entity.getDisplayName(),
                  chosen.getName());
            client.sendChat(String.format(CHANGE_OWNER_COMMAND, entity.getId(), chosen.getId()));
            setVisible(false);
        } else {
            reassigningOwner = true;
            comboOwner.setSelectedItem(currentOwner);
            reassigningOwner = false;
        }
    }

    /** The editor's controls, so that a test can set them the way a user would. */
    UnitDamageControls controlsForTesting() {
        return controls;
    }

    /** The armor diagram, so that a test can drive it the way a user would; {@code null} for conventional infantry. */
    DamageEditorDiagram diagramForTesting() {
        return diagram;
    }
}
