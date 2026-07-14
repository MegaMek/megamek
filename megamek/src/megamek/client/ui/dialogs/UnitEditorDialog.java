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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.*;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.ArmorPanel;
import megamek.client.ui.dialogs.unitEditor.CheckCritPanel;
import megamek.client.ui.dialogs.unitEditor.UnitDamageApplier;
import megamek.client.ui.dialogs.unitEditor.UnitDamageControls;
import megamek.client.ui.dialogs.unitEditor.UnitDamagePanelBuilder;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.picmap.LocationSelectListener;
import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.bays.ASFBay;
import megamek.common.bays.Bay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.compute.damage.CritAssignment;
import megamek.common.compute.damage.PreExistingDamageApplier;
import megamek.common.compute.damage.PreExistingDamageLevel;
import megamek.common.compute.damage.PreExistingDamageResult;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.common.weapons.attacks.InfantryAttack;
import megamek.logging.MMLogger;

/**
 * This dialog will allow the user to edit the damage and status characteristics of a unit. This is designed for use in
 * both MegaMek and MHQ so don't go messing things up for MHQ by changing a bunch of stuff
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitEditorDialog extends JDialog implements LocationSelectListener {
    private static final MMLogger LOGGER = MMLogger.create(UnitEditorDialog.class);

    @Serial
    private static final long serialVersionUID = 8144354264100884817L;

    private final Entity entity;

    /** Names the dialog and its divider for the stored size, position and divider location. */
    private static final String DIALOG_NAME = "unitEditorDialog";
    private static final String SPLIT_PANE_NAME = "unitEditorSplitPane";

    /** The gamemaster commands that have the server take a unit out of play, as used by the map menu. */
    private static final String DESTROY_UNIT_COMMAND = "/kill %d %b";
    private static final String RESCUE_UNIT_COMMAND = "/rescue %d";

    /** How much the armor diagram is enlarged even when there is no panel height to fill. */
    private static final double MIN_PAPERDOLL_SCALE = 1.6;

    /** How far the armor diagram may be enlarged, past which it turns blocky. */
    private static final double MAX_PAPERDOLL_SCALE = 2.5;

    /** How much of the screen height the enlarged armor diagram may take. */
    private static final double MAX_SCREEN_FRACTION = 0.7;

    /** The unit's armor diagram, the same one the unit display uses. Clicking a location shows its panel. */
    private ArmorPanel paperdoll;
    /** Divides the armor diagram from the location panel; the user can drag it. */
    private JSplitPane splitPane;
    /** Holds the location panels, one shown at a time. */
    private JPanel panCards;
    private final CardLayout cardLayout = new CardLayout();
    /** Chooses which location panel is shown; kept in step with the armor diagram. */
    private JComboBox<LocationChoice> comboLocation;

    /** The editor's controls, built by the panel builder and read back by the applier. */
    private final UnitDamageControls controls = new UnitDamageControls();
    /** Builds the controls; also used to reach the panel a location's controls live in. */
    private UnitDamagePanelBuilder panelBuilder;

    /* pre-existing damage (FSW p.144) */
    private JComboBox<PreExistingDamageLevel> choicePreExistingDamage;
    private int[] snapshotArmor;
    private int[] snapshotRear;
    private int[] snapshotInternal;
    private Map<CheckCritPanel, Integer> snapshotCritHits;

    /* damage coloring, using the unit tooltip armor colors */
    private JLabel[] locationLabels;
    private JLabel structuralIntegrityLabel;

    /** Whether the user opening this dialog is a gamemaster; only they may roll pre-existing damage. */
    private final boolean gameMaster;

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
     * @param gameMaster {@code true} if the user opening the dialog is a gamemaster, which enables the pre-existing
     *                   damage roller (FSW p.144)
     */
    public UnitEditorDialog(JFrame parent, Entity entity, boolean gameMaster) {
        this(parent, entity, gameMaster, null);
    }

    /**
     * @param parent     the parent frame
     * @param entity     the unit to edit damage for
     * @param gameMaster {@code true} if the user opening the dialog is a gamemaster, which enables the pre-existing
     *                   damage roller (FSW p.144)
     * @param client     the client to destroy the unit through, or {@code null} where there is no game to destroy it
     *                   in, such as in MekHQ and MegaMekLab. Without one, Destroy Unit is not offered.
     */
    public UnitEditorDialog(JFrame parent, Entity entity, boolean gameMaster, @Nullable Client client) {
        super(parent, true);
        this.entity = entity;
        this.gameMaster = gameMaster;
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
        panelBuilder = new UnitDamagePanelBuilder(entity, controls, gameMaster);
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
            layoutLocationPanels(panMain);
        }

        getContentPane().add(new JScrollPane(panMain), BorderLayout.CENTER);

        JButton butOK = new JButton(Messages.getString("Okay"));
        butOK.addActionListener(evt -> {
            new UnitDamageApplier(entity, controls).applyToEntity();
            setVisible(false);
        });
        JButton butCancel = new JButton(Messages.getString("Cancel"));
        butCancel.addActionListener(evt -> setVisible(false));

        panButtons.add(butOK);
        panButtons.add(butCancel);

        getContentPane().add(panButtons, BorderLayout.PAGE_END);

        if (!entity.isConventionalInfantry()) {
            wireDamageColoring();
        }

        if (showPreExistingDamagePanel()) {
            getContentPane().add(initPreExistingDamagePanel(), BorderLayout.PAGE_START);
            capturePreExistingSnapshot();
        }

        String closeAction = "closeAction";
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
        getRootPane().getActionMap().put(closeAction, new CloseAction(this));

        // The armor diagram builds its map sets when it becomes displayable, so it can only be drawn after the
        // first pack. It sizes itself to its content, so pack again once it has been drawn and enlarged.
        pack();
        refreshPaperdoll();
        enlargePaperdollToFillDialog();
        pack();

        // leave room for scroll bars and never grow beyond the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(getWidth() + UIUtil.scaleForGUI(30), (int) (screenSize.width * 0.9)),
              Math.min(getHeight() + UIUtil.scaleForGUI(30), (int) (screenSize.height * 0.9)));

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
            if (splitPane != null) {
                preferences.manage(new JSplitPanePreference(splitPane));
            }
        } catch (Exception ex) {
            // a dialog that cannot remember where it was is still perfectly usable
            LOGGER.error(ex, "Could not set the preferences of the unit editor dialog");
        }
    }

    /**
     * Enlarges the armor diagram. It is drawn at a fixed size that is small on a modern screen, and smaller still
     * beside a location panel full of equipment, which leaves the dialog with a band of empty space. So it is
     * grown to the height of the tallest location panel, but never less than {@link #MIN_PAPERDOLL_SCALE}, never
     * so far that it turns blocky, and never past the screen.
     */
    private void enlargePaperdollToFillDialog() {
        if ((paperdoll == null) || (panCards == null)) {
            return;
        }
        int drawnHeight = paperdoll.getPreferredSize().height;
        if (drawnHeight <= 0) {
            return;
        }
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        double panelScale = (double) panCards.getPreferredSize().height / drawnHeight;
        double screenScale = (screenHeight * MAX_SCREEN_FRACTION) / drawnHeight;
        double scale = Math.max(panelScale, MIN_PAPERDOLL_SCALE);
        scale = Math.min(scale, Math.min(MAX_PAPERDOLL_SCALE, screenScale));
        if (scale > 1.0) {
            paperdoll.setDisplayScale(scale);
        }
    }

    /**
     * The pre-existing damage panel appears only for the unit types the FSW rules cover and only when the user opening
     * the dialog is a gamemaster; assigning pre-existing damage is a GM or scenario-setup decision.
     */
    private boolean showPreExistingDamagePanel() {
        return gameMaster && PreExistingDamageApplier.isSupported(entity);
    }

    private JPanel initPreExistingDamagePanel() {
        JPanel panPreExisting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panPreExisting.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("UnitEditorDialog.preExistingDamage")));

        choicePreExistingDamage = new JComboBox<>(PreExistingDamageLevel.values());
        choicePreExistingDamage.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PreExistingDamageLevel damageLevel) {
                    setText(Messages.getString("UnitEditorDialog.preExistingDamage." + damageLevel.name()));
                }
                return this;
            }
        });
        choicePreExistingDamage.setToolTipText(Messages.getString("UnitEditorDialog.preExistingDamage.tooltip"));
        panPreExisting.add(choicePreExistingDamage);

        JButton butApplyPreExisting = new JButton(Messages.getString("UnitEditorDialog.preExistingDamage.apply"));
        butApplyPreExisting.setToolTipText(Messages.getString("UnitEditorDialog.preExistingDamage.tooltip"));
        butApplyPreExisting.addActionListener(event -> applyPreExistingDamage());
        panPreExisting.add(butApplyPreExisting);

        JButton butRestoreUnit = new JButton(Messages.getString("UnitEditorDialog.preExistingDamage.reset"));
        butRestoreUnit.setToolTipText(Messages.getString("UnitEditorDialog.preExistingDamage.reset.tooltip"));
        butRestoreUnit.addActionListener(event -> restoreUnitToFactoryNew());
        panPreExisting.add(butRestoreUnit);

        // In the lobby a unit that is not wanted is simply removed, and there is nothing in play to take off the
        // board. Without a client there is no server to take it off either, as in MekHQ and MegaMekLab.
        boolean canRemoveFromPlay = (client != null)
              && (entity.getGame() != null)
              && !entity.getGame().getPhase().isLounge();

        JButton butRescueUnit = new JButton(Messages.getString("UnitEditorDialog.rescueUnit"));
        butRescueUnit.setToolTipText(Messages.getString(canRemoveFromPlay
              ? "UnitEditorDialog.rescueUnit.tooltip"
              : "UnitEditorDialog.rescueUnit.tooltip.lobby"));
        butRescueUnit.setEnabled(canRemoveFromPlay);
        butRescueUnit.addActionListener(event -> rescueUnit());
        panPreExisting.add(butRescueUnit);

        JButton butDestroyUnit = new JButton(Messages.getString("UnitEditorDialog.destroyUnit"));
        butDestroyUnit.setToolTipText(Messages.getString(canRemoveFromPlay
              ? "UnitEditorDialog.destroyUnit.tooltip"
              : "UnitEditorDialog.destroyUnit.tooltip.lobby"));
        butDestroyUnit.setEnabled(canRemoveFromPlay);
        butDestroyUnit.addActionListener(event -> destroyUnit());
        panPreExisting.add(butDestroyUnit);
        return panPreExisting;
    }

    /**
     * Asks first, then has the server rescue the unit, through the same gamemaster command the map menu uses. The
     * unit flees the board and leaves play in one piece, which is the opposite of destroying it: the crew and the
     * unit both survive.
     * <p>
     * There is nothing to mark on the unit here, as there is for a destroyed unit. The server takes the unit out of
     * the game, so the update the caller sends once this dialog closes finds no unit to apply to and is dropped.
     * </p>
     */
    private void rescueUnit() {
        int choice = JOptionPane.showConfirmDialog(this,
              String.format(Messages.getString("UnitEditorDialog.rescueUnit.confirm"), entity.getDisplayName()),
              Messages.getString("UnitEditorDialog.rescueUnit"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        if (client == null) {
            LOGGER.error("Cannot rescue {}: the damage editor was opened without a client", entity.getDisplayName());
            return;
        }
        LOGGER.info("Rescuing {} at the request of the damage editor", entity.getDisplayName());
        client.sendChat(String.format(RESCUE_UNIT_COMMAND, entity.getId()));
        setVisible(false);
    }

    /**
     * Asks first, then has the server destroy the unit, through the same gamemaster command the map menu uses. The
     * server is what destroys a unit: it writes the reports, decides who dies with it and who escapes, and takes it
     * off the board. Zeroing the unit's armor here would leave it standing.
     * <p>
     * The unit is also marked destroyed locally, because the caller sends the unit to the server once this dialog
     * closes. That update would otherwise arrive after the destruction carrying a unit that is still alive, and put
     * it straight back on the board.
     * </p>
     */
    private void destroyUnit() {
        JCheckBox chkEjectCrew = new JCheckBox(Messages.getString("UnitEditorDialog.destroyUnit.eject"));
        chkEjectCrew.setToolTipText(Messages.getString("UnitEditorDialog.destroyUnit.eject.tooltip"));
        chkEjectCrew.setEnabled(canEjectCrew());
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
        entity.setDoomed(true);
        entity.setDestroyed(true);
        client.sendChat(String.format(DESTROY_UNIT_COMMAND, entity.getId(), ejectCrew));
        setVisible(false);
    }

    /** A crew can only leave a unit that has one, that can eject, and that they have not already left. */
    private boolean canEjectCrew() {
        Crew crew = entity.getCrew();
        return (crew != null)
              && !crew.isEjected()
              && !crew.isDead()
              && (entity.isMek() || entity.isAero());
    }

    private void setSpinnerToZero(@Nullable JSpinner spinner) {
        if (null != spinner) {
            spinner.setValue(0);
        }
    }

    /**
     * Sets the dialog controls to a fully repaired unit: every spinner (armor, structure, bay capacity, drive
     * integrity) to its maximum and every critical hit cleared. Only pressing Okay commits the values.
     */
    private void restoreUnitToFactoryNew() {
        restoreSpinnersToMaximum(getContentPane());
        snapshotCritHits.keySet().forEach(critPanel -> critPanel.setHits(0));
        // heat and crew hits are damage counting up, not health counting down, so a repaired unit has none of them
        setSpinnerToZero(controls.spnHeat);
        if (null != controls.spnCrewHits) {
            for (JSpinner crewHits : controls.spnCrewHits) {
                setSpinnerToZero(crewHits);
            }
        }
    }

    private void restoreSpinnersToMaximum(Container container) {
        for (Component component : container.getComponents()) {
            if ((component instanceof JSpinner spinner)
                  && (spinner.getModel() instanceof SpinnerNumberModel model)) {
                spinner.setValue(model.getMaximum());
            } else if (component instanceof Container childContainer) {
                restoreSpinnersToMaximum(childContainer);
            }
        }
    }

    /** Remembers the control values at dialog open, so each pre-existing damage roll starts from the same state. */
    private void capturePreExistingSnapshot() {
        snapshotArmor = new int[entity.locations()];
        snapshotRear = new int[entity.locations()];
        snapshotInternal = new int[entity.locations()];
        for (int location = 0; location < entity.locations(); location++) {
            if (null != controls.spnArmor[location]) {
                snapshotArmor[location] = (Integer) controls.spnArmor[location].getValue();
            }
            if (null != controls.spnRear[location]) {
                snapshotRear[location] = (Integer) controls.spnRear[location].getValue();
            }
            if (null != controls.spnInternal[location]) {
                snapshotInternal[location] = (Integer) controls.spnInternal[location].getValue();
            }
        }
        snapshotCritHits = new HashMap<>();
        collectCritPanels(getContentPane());
    }

    private void collectCritPanels(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof CheckCritPanel critPanel) {
                snapshotCritHits.put(critPanel, critPanel.getHits());
            } else if (component instanceof Container childContainer) {
                collectCritPanels(childContainer);
            }
        }
    }

    private void restorePreExistingSnapshot() {
        for (int location = 0; location < entity.locations(); location++) {
            if (null != controls.spnArmor[location]) {
                controls.spnArmor[location].setValue(snapshotArmor[location]);
            }
            if (null != controls.spnRear[location]) {
                controls.spnRear[location].setValue(snapshotRear[location]);
            }
            if (null != controls.spnInternal[location]) {
                controls.spnInternal[location].setValue(snapshotInternal[location]);
            }
        }
        snapshotCritHits.forEach(CheckCritPanel::setHits);
    }

    /**
     * Rolls pre-existing damage at the selected level and writes it into the dialog's controls. The entity itself is
     * untouched; only pressing Okay commits the values, and Cancel discards them. Each press rerolls from the state the
     * unit had when the dialog opened.
     */
    private void applyPreExistingDamage() {
        restorePreExistingSnapshot();
        PreExistingDamageLevel level = (PreExistingDamageLevel) choicePreExistingDamage.getSelectedItem();
        if ((null == level) || (level == PreExistingDamageLevel.NONE)) {
            return;
        }
        PreExistingDamageResult result = PreExistingDamageApplier.simulate(entity, level);
        for (int location = 0; location < entity.locations(); location++) {
            if (null != controls.spnArmor[location]) {
                controls.spnArmor[location].setValue(result.armor()[location]);
            }
            if (null != controls.spnRear[location]) {
                controls.spnRear[location].setValue(result.rearArmor()[location]);
            }
        }
        if (entity instanceof Aero) {
            if (null != controls.spnInternal[0]) {
                controls.spnInternal[0].setValue(result.structuralIntegrity());
            }
        } else {
            for (int location = 0; location < entity.locations(); location++) {
                if (null != controls.spnInternal[location]) {
                    controls.spnInternal[location].setValue(result.internal()[location]);
                }
            }
        }
        for (CritAssignment assignment : result.critAssignments()) {
            applyCritAssignment(assignment);
        }
    }

    private void applyCritAssignment(CritAssignment assignment) {
        switch (assignment) {
            case CritAssignment.EquipmentCrit(int equipmentNumber) -> incrementCrit(controls.equipCrits.get(equipmentNumber));
            case CritAssignment.MekSystemCrit(int system, int location) -> applyMekSystemCrit(system, location);
            case CritAssignment.VehicleCrit(CritAssignment.VehicleCritKind kind, int location) ->
                  applyVehicleCrit(kind, location);
            case CritAssignment.AeroFighterCrit(CritAssignment.AeroFighterCritKind kind) -> applyFighterCrit(kind);
        }
    }

    private void applyMekSystemCrit(int system, int location) {
        if (entity instanceof LandAirMek) {
            if (system == LandAirMek.LAM_AVIONICS) {
                incrementCrit(controls.lamAvionicsCrit.get(location));
                return;
            }
            if (system == LandAirMek.LAM_LANDING_GEAR) {
                incrementCrit(controls.lamLandingGearCrit.get(location));
                return;
            }
        }
        if ((entity instanceof QuadVee) && (system == QuadVee.SYSTEM_CONVERSION_GEAR)) {
            incrementCrit(controls.actuatorCrits[location - Mek.LOC_RIGHT_ARM][Mek.ACTUATOR_FOOT - Mek.ACTUATOR_HIP + 1]);
            return;
        }
        switch (system) {
            case Mek.SYSTEM_ENGINE -> {
                switch (location) {
                    case Mek.LOC_LEFT_TORSO -> incrementCrit(controls.leftEngineCrit);
                    case Mek.LOC_RIGHT_TORSO -> incrementCrit(controls.rightEngineCrit);
                    default -> incrementCrit(controls.centerEngineCrit);
                }
            }
            case Mek.SYSTEM_GYRO -> incrementCrit(controls.gyroCrit);
            case Mek.SYSTEM_SENSORS -> incrementCrit(controls.sensorCrit);
            case Mek.SYSTEM_LIFE_SUPPORT -> incrementCrit(controls.lifeSupportCrit);
            default -> applyActuatorCrit(system, location);
        }
    }

    private void applyActuatorCrit(int actuator, int location) {
        if ((actuator < Mek.ACTUATOR_SHOULDER) || (actuator > Mek.ACTUATOR_FOOT)) {
            return;
        }
        int row = location - Mek.LOC_RIGHT_ARM;
        if ((row < 0) || (row >= controls.actuatorCrits.length)) {
            return;
        }
        int start = ((location >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek))
              ? Mek.ACTUATOR_HIP : Mek.ACTUATOR_SHOULDER;
        int column = actuator - start;
        if ((column < 0) || (column >= controls.actuatorCrits[row].length)) {
            return;
        }
        incrementCrit(controls.actuatorCrits[row][column]);
    }

    private void applyVehicleCrit(CritAssignment.VehicleCritKind kind, int location) {
        switch (kind) {
            case TURRET_LOCK -> incrementCrit(controls.turretLockCrit);
            case SENSORS -> incrementCrit(controls.sensorCrit);
            case MOTIVE -> incrementCrit(controls.motiveCrit);
            case STABILIZER -> {
                if ((entity instanceof VTOL) && (location == VTOL.LOC_ROTOR)) {
                    incrementCrit(controls.flightStabilizerCrit);
                } else if ((null != controls.stabilizerCrits) && (location >= 0) && (location < controls.stabilizerCrits.length)) {
                    incrementCrit(controls.stabilizerCrits[location]);
                }
            }
        }
    }

    private void applyFighterCrit(CritAssignment.AeroFighterCritKind kind) {
        switch (kind) {
            case AVIONICS -> incrementCrit(controls.avionicsCrit);
            case FIRE_CONTROL_SYSTEM -> incrementCrit(controls.fcsCrit);
            case SENSORS -> incrementCrit(controls.sensorCrit);
            case ENGINE -> incrementCrit(controls.engineCrit);
            case LANDING_GEAR -> incrementCrit(controls.gearCrit);
        }
    }

    private void incrementCrit(@Nullable CheckCritPanel critPanel) {
        if (null != critPanel) {
            critPanel.setHits(critPanel.getHits() + 1);
        }
    }

    /**
     * Keeps the armor diagram and the value coloring in step with the spinners, so the dialog always shows the
     * damage that pressing Okay would apply.
     */
    private void wireDamageColoring() {
        for (int location = 0; location < entity.locations(); location++) {
            if (null != controls.spnArmor[location]) {
                controls.spnArmor[location].addChangeListener(event -> refreshDamageDisplay());
            }
            if (null != controls.spnRear[location]) {
                controls.spnRear[location].addChangeListener(event -> refreshDamageDisplay());
            }
            if (null != controls.spnInternal[location]) {
                controls.spnInternal[location].addChangeListener(event -> refreshDamageDisplay());
            }
        }
        // the diagram carries a heat scale, so it follows the heat control too
        if (null != controls.spnHeat) {
            controls.spnHeat.addChangeListener(event -> refreshDamageDisplay());
        }
        // a location with a critical hit is striped on the diagram, so it follows the crit controls as well
        controls.critsByLocation.values()
              .forEach(crits -> crits.forEach(crit -> crit.addHitsChangedListener(this::refreshDamageDisplay)));
        refreshDamageDisplay();
    }

    private void refreshDamageDisplay() {
        refreshDamageColoring();
        refreshPaperdoll();
    }

    private void refreshDamageColoring() {
        for (int location = 0; location < entity.locations(); location++) {
            Color worstColor = colorSpinner(controls.spnArmor[location], entity.getOArmor(location, false), null);
            worstColor = colorSpinner(controls.spnRear[location], entity.getOArmor(location, true), worstColor);
            if (!(entity instanceof Aero)) {
                worstColor = colorSpinner(controls.spnInternal[location], entity.getOInternal(location), worstColor);
            }
            if ((null != controls.locationLabels) && (null != controls.locationLabels[location]) && (null != worstColor)) {
                controls.locationLabels[location].setForeground(worstColor);
            }
        }
        if ((entity instanceof Aero aero) && (null != controls.structuralIntegrityLabel) && (null != controls.spnInternal[0])) {
            Color siColor = colorSpinner(controls.spnInternal[0], aero.getOSI(), null);
            if (null != siColor) {
                controls.structuralIntegrityLabel.setForeground(siColor);
            }
        }
    }

    /**
     * Colors one spinner's text by how damaged its value is and returns the more severe of that color and the given
     * one, so callers can color the location label by its worst value.
     */
    private @Nullable Color colorSpinner(@Nullable JSpinner spinner, int originalValue, @Nullable Color worstSoFar) {
        if ((null == spinner) || (originalValue <= 0)) {
            return worstSoFar;
        }
        int currentValue = (Integer) spinner.getValue();
        Color color = damageColor(currentValue, originalValue);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setForeground(color);
        }
        return moreSevere(color, worstSoFar);
    }

    private Color damageColor(int currentValue, int originalValue) {
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        if (currentValue <= 0) {
            return guiPreferences.getUnitTooltipArmorMiniColorDamaged();
        } else if (currentValue < originalValue) {
            return guiPreferences.getUnitTooltipArmorMiniColorPartialDamage();
        }
        return guiPreferences.getUnitTooltipArmorMiniColorIntact();
    }

    private @Nullable Color moreSevere(@Nullable Color first, @Nullable Color second) {
        if (null == second) {
            return first;
        }
        if (null == first) {
            return second;
        }
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        Color damaged = guiPreferences.getUnitTooltipArmorMiniColorDamaged();
        Color partial = guiPreferences.getUnitTooltipArmorMiniColorPartialDamage();
        if (damaged.equals(first) || damaged.equals(second)) {
            return damaged;
        }
        if (partial.equals(first) || partial.equals(second)) {
            return partial;
        }
        return first;
    }

    /**
     * Creates one panel per unit location, titled with the location name and holding the location's structure
     * and armor spinners. System and equipment crits are added into these panels afterwards.
     */
    private void layoutLocationPanels(JPanel panMain) {
        panCards = new JPanel(cardLayout);
        comboLocation = new JComboBox<>();

        for (int location = 0; location < controls.locationPanels.length; location++) {
            if (controls.locationPanels[location] != null) {
                panCards.add(controls.locationPanels[location], cardName(location));
                comboLocation.addItem(new LocationChoice(location, entity.getLocationName(location)));
            }
        }
        comboLocation.addActionListener(event -> {
            if (comboLocation.getSelectedItem() instanceof LocationChoice choice) {
                cardLayout.show(panCards, cardName(choice.location()));
            }
        });

        // The diagram sizes itself to its drawn content, so it needs no preferred size of its own.
        paperdoll = new ArmorPanel(entity.getGame(), this);
        paperdoll.setToolTipText(Messages.getString("UnitEditorDialog.paperdoll.tooltip"));

        JPanel panChooser = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panChooser.add(new JLabel(Messages.getString("UnitEditorDialog.location")));
        panChooser.add(comboLocation);

        // The general panel is shown under the chosen location rather than as another location to choose. There is
        // no general part of a unit to click on the diagram, so a panel that had to be chosen there was a panel
        // nobody would find, and what is in it - the unit's condition, its heat, its systems - is always relevant.
        JPanel panPanels = new JPanel();
        panPanels.setLayout(new BoxLayout(panPanels, BoxLayout.PAGE_AXIS));
        panCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        panPanels.add(panCards);
        if (controls.panGeneral != null) {
            controls.panGeneral.setAlignmentX(Component.LEFT_ALIGNMENT);
            panPanels.add(controls.panGeneral);
        }

        JPanel panRight = new JPanel(new BorderLayout());
        panRight.add(panChooser, BorderLayout.PAGE_START);
        panRight.add(new JScrollPane(panPanels), BorderLayout.CENTER);

        // The user can drag the divider to trade diagram size against panel size; the position is remembered.
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(paperdoll), panRight);
        splitPane.setName(SPLIT_PANE_NAME);
        splitPane.setResizeWeight(0.0);
        splitPane.setOneTouchExpandable(true);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panMain.add(splitPane, gridBagConstraints);
    }

    /** The diagram is here to pick locations with, so a single click is enough to select one. */
    @Override
    public boolean selectsOnSingleClick() {
        return true;
    }

    /** The editor's controls, so that a test can set them the way a user would. */
    UnitDamageControls controlsForTesting() {
        return controls;
    }

    /** Shows the panel of the location the user picked in the armor diagram. */
    @Override
    public void locationSelected(int location) {
        if ((location >= 0) && (location < controls.locationPanels.length) && (controls.locationPanels[location] != null)) {
            comboLocation.setSelectedItem(new LocationChoice(location, entity.getLocationName(location)));
        }
    }

    private static String cardName(int location) {
        return "location-" + location;
    }

    /** An entry of the location chooser. Equality is by location so the chooser can be set by location alone. */
    private record LocationChoice(int location, String name) {
        @Override
        public boolean equals(Object other) {
            return (other instanceof LocationChoice choice) && (choice.location == location);
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(location);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Redraws the armor diagram to show the values currently in the spinners, rather than the unit's actual
     * damage.
     * <p>
     * The diagram renders straight off the unit, but the dialog's edits are pending until Okay is pressed, so the
     * pending values are written into the unit, the diagram is redrawn from them, and the unit is put back as it
     * was. Nothing observes the unit in between: armor and structure are plain fields with no listeners, and this
     * runs on the event dispatch thread. Critical hits are not drawn on the diagram, so they need no preview.
     * </p>
     */
    private void refreshPaperdoll() {
        // The diagram builds its map sets when it is added to a displayable window, so there is nothing to draw
        // into before the dialog is packed. initComponents() draws it once that has happened.
        if ((paperdoll == null) || !paperdoll.isDisplayable()) {
            return;
        }
        int[] actualArmor = new int[entity.locations()];
        int[] actualRear = new int[entity.locations()];
        int[] actualInternal = new int[entity.locations()];
        for (int location = 0; location < entity.locations(); location++) {
            actualArmor[location] = entity.getArmor(location, false);
            actualRear[location] = entity.getArmor(location, true);
            actualInternal[location] = entity.getInternal(location);
        }
        int actualStructuralIntegrity = (entity instanceof Aero aero) ? aero.getSI() : 0;
        int actualHeat = entity.heat;

        try {
            applyPendingValuesForDisplay();
            // the crits are not on the unit yet, so the diagram is told which locations to stripe
            paperdoll.setCriticalLocations(controls.locationsWithCrits());
            paperdoll.displayMek(entity);
        } finally {
            for (int location = 0; location < entity.locations(); location++) {
                entity.setArmor(actualArmor[location], location, false);
                entity.setArmor(actualRear[location], location, true);
                entity.setInternal(actualInternal[location], location);
            }
            if (entity instanceof Aero aero) {
                aero.setSI(actualStructuralIntegrity);
            }
            entity.heat = actualHeat;
        }
    }

    /** Writes the spinner values into the unit so the armor diagram can be drawn from them. */
    private void applyPendingValuesForDisplay() {
        for (int location = 0; location < entity.locations(); location++) {
            if (null != controls.spnArmor[location]) {
                entity.setArmor((Integer) controls.spnArmor[location].getValue(), location, false);
            }
            if (null != controls.spnRear[location]) {
                entity.setArmor((Integer) controls.spnRear[location].getValue(), location, true);
            }
            if ((null != controls.spnInternal[location]) && !(entity instanceof Aero)) {
                entity.setInternal((Integer) controls.spnInternal[location].getValue(), location);
            }
        }
        if ((entity instanceof Aero aero) && (null != controls.spnInternal[0])) {
            aero.setSI((Integer) controls.spnInternal[0].getValue());
        }
        if (null != controls.spnHeat) {
            entity.heat = (Integer) controls.spnHeat.getValue();
        }
    }


    /** Adds a crit row for each piece of hittable equipment to the panel of the location it is mounted in. */
}
