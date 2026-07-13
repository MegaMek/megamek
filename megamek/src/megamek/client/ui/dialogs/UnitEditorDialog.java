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

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.ArmorPanel;
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

/**
 * This dialog will allow the user to edit the damage and status characteristics of a unit. This is designed for use in
 * both MegaMek and MHQ so don't go messing things up for MHQ by changing a bunch of stuff
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitEditorDialog extends JDialog implements LocationSelectListener {
    @Serial
    private static final long serialVersionUID = 8144354264100884817L;

    private final Entity entity;

    /** Key for the general panel in the location chooser; locations use their own index. */
    private static final int GENERAL_PANEL_KEY = -1;

    /** How much the armor diagram is enlarged even when there is no panel height to fill. */
    private static final double MIN_PAPERDOLL_SCALE = 1.6;

    /** How far the armor diagram may be enlarged, past which it turns blocky. */
    private static final double MAX_PAPERDOLL_SCALE = 2.5;

    /** How much of the screen height the enlarged armor diagram may take. */
    private static final double MAX_SCREEN_FRACTION = 0.7;

    /** One panel per unit location, holding that location's armor, structure, systems and equipment. */
    private JPanel[] locationPanels;
    /** Panel for systems that have no single location, such as a tank's engine or an aero's avionics. */
    private JPanel panGeneral;
    /** The next free row in each panel, used when appending label and control rows. */
    private final Map<JPanel, Integer> panelRows = new HashMap<>();

    /** The unit's armor diagram, the same one the unit display uses. Clicking a location shows its panel. */
    private ArmorPanel paperdoll;
    /** Holds the location panels, one shown at a time. */
    private JPanel panCards;
    private final CardLayout cardLayout = new CardLayout();
    /** Chooses which location panel is shown; kept in step with the armor diagram. */
    private JComboBox<LocationChoice> comboLocation;

    JSpinner[] spnInternal;
    JSpinner[] spnArmor;
    JSpinner[] spnRear;

    HashMap<Integer, CheckCritPanel> equipCrits;

    /* system crits */ CheckCritPanel engineCrit;
    CheckCritPanel leftEngineCrit;
    CheckCritPanel rightEngineCrit;
    CheckCritPanel centerEngineCrit;
    CheckCritPanel gyroCrit;
    CheckCritPanel sensorCrit;
    CheckCritPanel lifeSupportCrit;
    CheckCritPanel cockpitCrit;
    Map<Integer, CheckCritPanel> lamAvionicsCrit;
    Map<Integer, CheckCritPanel> lamLandingGearCrit;
    CheckCritPanel[][] actuatorCrits;
    CheckCritPanel turretLockCrit;
    CheckCritPanel motiveCrit;
    CheckCritPanel[] stabilizerCrits;
    CheckCritPanel flightStabilizerCrit;
    CheckCritPanel avionicsCrit;
    CheckCritPanel fcsCrit;
    CheckCritPanel cicCrit;
    CheckCritPanel gearCrit;
    CheckCritPanel leftThrusterCrit;
    CheckCritPanel rightThrusterCrit;
    CheckCritPanel kfBoomCrit;
    CheckCritPanel dockCollarCrit;
    CheckCritPanel gravDeckCrit;
    JSpinner[] bayDamage;
    CheckCritPanel[] bayDoorCrit;
    JSpinner collarDamage;
    JSpinner kfDamage;
    CheckCritPanel driveCoilCrit;
    CheckCritPanel chargingSystemCrit;
    CheckCritPanel fieldInitiatorCrit;
    CheckCritPanel driveControllerCrit;
    CheckCritPanel heliumTankCrit;
    CheckCritPanel lfBatteryCrit;
    JSpinner sailDamage;
    CheckCritPanel[] protoCrits;

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
        super(parent, true);
        this.entity = entity;
        this.gameMaster = gameMaster;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        getContentPane().setLayout(new BorderLayout());

        setTitle("Edit damage for " + entity.getDisplayName());

        JPanel panMain = new JPanel(new GridBagLayout());
        JPanel panButtons = new JPanel(new GridLayout(1, 2));

        equipCrits = new HashMap<>();
        if (entity.isConventionalInfantry()) {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            panMain.add(initInfantryPanel(), gridBagConstraints);
        } else {
            initLocationPanels();
            initSystemCrits();
            initEquipCrits();
            layoutLocationPanels(panMain);
        }

        getContentPane().add(new JScrollPane(panMain), BorderLayout.CENTER);

        JButton butOK = new JButton(Messages.getString("Okay"));
        butOK.addActionListener(evt -> {
            btnOkayActionPerformed(evt);
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
        return panPreExisting;
    }

    /**
     * Sets the dialog controls to a fully repaired unit: every spinner (armor, structure, bay capacity, drive
     * integrity) to its maximum and every critical hit cleared. Only pressing Okay commits the values.
     */
    private void restoreUnitToFactoryNew() {
        restoreSpinnersToMaximum(getContentPane());
        snapshotCritHits.keySet().forEach(critPanel -> critPanel.setHits(0));
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
            if (null != spnArmor[location]) {
                snapshotArmor[location] = (Integer) spnArmor[location].getValue();
            }
            if (null != spnRear[location]) {
                snapshotRear[location] = (Integer) spnRear[location].getValue();
            }
            if (null != spnInternal[location]) {
                snapshotInternal[location] = (Integer) spnInternal[location].getValue();
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
            if (null != spnArmor[location]) {
                spnArmor[location].setValue(snapshotArmor[location]);
            }
            if (null != spnRear[location]) {
                spnRear[location].setValue(snapshotRear[location]);
            }
            if (null != spnInternal[location]) {
                spnInternal[location].setValue(snapshotInternal[location]);
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
            if (null != spnArmor[location]) {
                spnArmor[location].setValue(result.armor()[location]);
            }
            if (null != spnRear[location]) {
                spnRear[location].setValue(result.rearArmor()[location]);
            }
        }
        if (entity instanceof Aero) {
            if (null != spnInternal[0]) {
                spnInternal[0].setValue(result.structuralIntegrity());
            }
        } else {
            for (int location = 0; location < entity.locations(); location++) {
                if (null != spnInternal[location]) {
                    spnInternal[location].setValue(result.internal()[location]);
                }
            }
        }
        for (CritAssignment assignment : result.critAssignments()) {
            applyCritAssignment(assignment);
        }
    }

    private void applyCritAssignment(CritAssignment assignment) {
        switch (assignment) {
            case CritAssignment.EquipmentCrit(int equipmentNumber) -> incrementCrit(equipCrits.get(equipmentNumber));
            case CritAssignment.MekSystemCrit(int system, int location) -> applyMekSystemCrit(system, location);
            case CritAssignment.VehicleCrit(CritAssignment.VehicleCritKind kind, int location) ->
                  applyVehicleCrit(kind, location);
            case CritAssignment.AeroFighterCrit(CritAssignment.AeroFighterCritKind kind) -> applyFighterCrit(kind);
        }
    }

    private void applyMekSystemCrit(int system, int location) {
        if (entity instanceof LandAirMek) {
            if (system == LandAirMek.LAM_AVIONICS) {
                incrementCrit(lamAvionicsCrit.get(location));
                return;
            }
            if (system == LandAirMek.LAM_LANDING_GEAR) {
                incrementCrit(lamLandingGearCrit.get(location));
                return;
            }
        }
        if ((entity instanceof QuadVee) && (system == QuadVee.SYSTEM_CONVERSION_GEAR)) {
            incrementCrit(actuatorCrits[location - Mek.LOC_RIGHT_ARM][Mek.ACTUATOR_FOOT - Mek.ACTUATOR_HIP + 1]);
            return;
        }
        switch (system) {
            case Mek.SYSTEM_ENGINE -> {
                switch (location) {
                    case Mek.LOC_LEFT_TORSO -> incrementCrit(leftEngineCrit);
                    case Mek.LOC_RIGHT_TORSO -> incrementCrit(rightEngineCrit);
                    default -> incrementCrit(centerEngineCrit);
                }
            }
            case Mek.SYSTEM_GYRO -> incrementCrit(gyroCrit);
            case Mek.SYSTEM_SENSORS -> incrementCrit(sensorCrit);
            case Mek.SYSTEM_LIFE_SUPPORT -> incrementCrit(lifeSupportCrit);
            default -> applyActuatorCrit(system, location);
        }
    }

    private void applyActuatorCrit(int actuator, int location) {
        if ((actuator < Mek.ACTUATOR_SHOULDER) || (actuator > Mek.ACTUATOR_FOOT)) {
            return;
        }
        int row = location - Mek.LOC_RIGHT_ARM;
        if ((row < 0) || (row >= actuatorCrits.length)) {
            return;
        }
        int start = ((location >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek))
              ? Mek.ACTUATOR_HIP : Mek.ACTUATOR_SHOULDER;
        int column = actuator - start;
        if ((column < 0) || (column >= actuatorCrits[row].length)) {
            return;
        }
        incrementCrit(actuatorCrits[row][column]);
    }

    private void applyVehicleCrit(CritAssignment.VehicleCritKind kind, int location) {
        switch (kind) {
            case TURRET_LOCK -> incrementCrit(turretLockCrit);
            case SENSORS -> incrementCrit(sensorCrit);
            case MOTIVE -> incrementCrit(motiveCrit);
            case STABILIZER -> {
                if ((entity instanceof VTOL) && (location == VTOL.LOC_ROTOR)) {
                    incrementCrit(flightStabilizerCrit);
                } else if ((null != stabilizerCrits) && (location >= 0) && (location < stabilizerCrits.length)) {
                    incrementCrit(stabilizerCrits[location]);
                }
            }
        }
    }

    private void applyFighterCrit(CritAssignment.AeroFighterCritKind kind) {
        switch (kind) {
            case AVIONICS -> incrementCrit(avionicsCrit);
            case FIRE_CONTROL_SYSTEM -> incrementCrit(fcsCrit);
            case SENSORS -> incrementCrit(sensorCrit);
            case ENGINE -> incrementCrit(engineCrit);
            case LANDING_GEAR -> incrementCrit(gearCrit);
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
            if (null != spnArmor[location]) {
                spnArmor[location].addChangeListener(event -> refreshDamageDisplay());
            }
            if (null != spnRear[location]) {
                spnRear[location].addChangeListener(event -> refreshDamageDisplay());
            }
            if (null != spnInternal[location]) {
                spnInternal[location].addChangeListener(event -> refreshDamageDisplay());
            }
        }
        refreshDamageDisplay();
    }

    private void refreshDamageDisplay() {
        refreshDamageColoring();
        refreshPaperdoll();
    }

    private void refreshDamageColoring() {
        for (int location = 0; location < entity.locations(); location++) {
            Color worstColor = colorSpinner(spnArmor[location], entity.getOArmor(location, false), null);
            worstColor = colorSpinner(spnRear[location], entity.getOArmor(location, true), worstColor);
            if (!(entity instanceof Aero)) {
                worstColor = colorSpinner(spnInternal[location], entity.getOInternal(location), worstColor);
            }
            if ((null != locationLabels) && (null != locationLabels[location]) && (null != worstColor)) {
                locationLabels[location].setForeground(worstColor);
            }
        }
        if ((entity instanceof Aero aero) && (null != structuralIntegrityLabel) && (null != spnInternal[0])) {
            Color siColor = colorSpinner(spnInternal[0], aero.getOSI(), null);
            if (null != siColor) {
                structuralIntegrityLabel.setForeground(siColor);
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
    private void initLocationPanels() {
        locationPanels = new JPanel[entity.locations()];
        spnArmor = new JSpinner[entity.locations()];
        spnInternal = new JSpinner[entity.locations()];
        spnRear = new JSpinner[entity.locations()];
        locationLabels = new JLabel[entity.locations()];

        boolean isAero = entity instanceof Aero;
        for (int location = 0; location < entity.locations(); location++) {
            // some units have hidden locations, skip these
            if (isAero ? (entity.getOArmor(location) <= 0) : (entity.getOInternal(location) <= 0)) {
                continue;
            }
            locationLabels[location] = new JLabel(entity.getLocationName(location));
            locationPanels[location] = createTitledPanel(locationLabels[location]);

            if (!isAero) {
                int internal = Math.max(entity.getInternal(location), 0);
                spnInternal[location] = new JSpinner(new SpinnerNumberModel(internal,
                      0,
                      entity.getOInternal(location),
                      1));
                addLabeledRow(locationPanels[location],
                      Messages.getString("UnitEditorDialog.internal"),
                      spnInternal[location]);
            }

            int armor = Math.max(entity.getArmor(location, false), 0);
            spnArmor[location] = new JSpinner(new SpinnerNumberModel(armor, 0, entity.getOArmor(location), 1));
            boolean hasRear = entity.hasRearArmor(location);
            addLabeledRow(locationPanels[location],
                  Messages.getString(hasRear ? "UnitEditorDialog.armorFront" : "UnitEditorDialog.armor"),
                  spnArmor[location]);
            if (hasRear) {
                int rear = Math.max(entity.getArmor(location, true), 0);
                spnRear[location] = new JSpinner(new SpinnerNumberModel(rear,
                      0,
                      entity.getOArmor(location, true),
                      1));
                addLabeledRow(locationPanels[location],
                      Messages.getString("UnitEditorDialog.armorRear"),
                      spnRear[location]);
            }
        }

        if (isAero) {
            Aero aero = (Aero) entity;
            int structuralIntegrity = Math.max(aero.getSI(), 0);
            spnInternal[0] = new JSpinner(new SpinnerNumberModel(structuralIntegrity, 0, aero.getOSI(), 1));
            structuralIntegrityLabel = new JLabel("<html><b>" +
                  Messages.getString("UnitEditorDialog.structuralIntegrity") +
                  "</b></html>");
            addRow(generalPanel(), structuralIntegrityLabel, spnInternal[0]);
        }
    }

    private JPanel initInfantryPanel() {
        Infantry infantry = (Infantry) entity;

        spnArmor = new JSpinner[entity.locations()];
        spnInternal = new JSpinner[entity.locations()];
        spnRear = new JSpinner[entity.locations()];

        int men = Math.max(infantry.getShootingStrength(), 0);
        spnInternal[0] = new JSpinner(new SpinnerNumberModel(men,
              0,
              infantry.getSquadCount() * infantry.getSquadSize(),
              1));
        JPanel panel = createTitledPanel(new JLabel(Messages.getString("UnitEditorDialog.troopersLeft")));
        addLabeledRow(panel, Messages.getString("UnitEditorDialog.menLeft"), spnInternal[0]);
        return panel;
    }

    /** Creates an empty location-style panel with the given label as its bold title row. */
    private JPanel createTitledPanel(JLabel titleLabel) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(2, 5, 2, 5);
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gridBagConstraints);
        panelRows.put(panel, 1);
        return panel;
    }

    /** Returns the panel for systems that have no single location, creating it on first use. */
    private JPanel generalPanel() {
        if (panGeneral == null) {
            panGeneral = createTitledPanel(new JLabel(Messages.getString("UnitEditorDialog.general")));
        }
        return panGeneral;
    }

    /** Returns the panel for the given location, or the general panel when that location has none. */
    private JPanel targetPanel(int location) {
        if ((location >= 0) && (location < locationPanels.length) && (locationPanels[location] != null)) {
            return locationPanels[location];
        }
        return generalPanel();
    }

    /** Appends a bold label and a control as the next row of the given panel. */
    private void addLabeledRow(JPanel panel, String labelText, JComponent control) {
        addRow(panel, new JLabel("<html><b>" + labelText + "</b></html>"), control);
    }

    private void addRow(JPanel panel, JLabel label, JComponent control) {
        int row = panelRows.merge(panel, 1, Integer::sum) - 1;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = row;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.0;
        panel.add(label, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panel.add(control, gridBagConstraints);
    }

    /**
     * Lays out the armor diagram on the left and the location panels on the right, where one location shows at a
     * time. The diagram is the same one the unit display uses, so every unit type that has an armor readout gets
     * one. Double-clicking a location on the diagram brings up that location's panel; the chooser above the panel
     * does the same for units whose diagram has no clickable locations, and reaches the general panel.
     */
    private void layoutLocationPanels(JPanel panMain) {
        panCards = new JPanel(cardLayout);
        comboLocation = new JComboBox<>();

        for (int location = 0; location < locationPanels.length; location++) {
            if (locationPanels[location] != null) {
                panCards.add(locationPanels[location], cardName(location));
                comboLocation.addItem(new LocationChoice(location, entity.getLocationName(location)));
            }
        }
        if (panGeneral != null) {
            panCards.add(panGeneral, cardName(GENERAL_PANEL_KEY));
            comboLocation.addItem(new LocationChoice(GENERAL_PANEL_KEY,
                  Messages.getString("UnitEditorDialog.general")));
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

        JPanel panRight = new JPanel(new BorderLayout());
        panRight.add(panChooser, BorderLayout.PAGE_START);
        panRight.add(new JScrollPane(panCards), BorderLayout.CENTER);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        panMain.add(paperdoll, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panMain.add(panRight, gridBagConstraints);
    }

    /** The diagram is here to pick locations with, so a single click is enough to select one. */
    @Override
    public boolean selectsOnSingleClick() {
        return true;
    }

    /** Shows the panel of the location the user picked in the armor diagram. */
    @Override
    public void locationSelected(int location) {
        if ((location >= 0) && (location < locationPanels.length) && (locationPanels[location] != null)) {
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

        try {
            applyPendingValuesForDisplay();
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
        }
    }

    /** Writes the spinner values into the unit so the armor diagram can be drawn from them. */
    private void applyPendingValuesForDisplay() {
        for (int location = 0; location < entity.locations(); location++) {
            if (null != spnArmor[location]) {
                entity.setArmor((Integer) spnArmor[location].getValue(), location, false);
            }
            if (null != spnRear[location]) {
                entity.setArmor((Integer) spnRear[location].getValue(), location, true);
            }
            if ((null != spnInternal[location]) && !(entity instanceof Aero)) {
                entity.setInternal((Integer) spnInternal[location].getValue(), location);
            }
        }
        if ((entity instanceof Aero aero) && (null != spnInternal[0])) {
            aero.setSI((Integer) spnInternal[0].getValue());
        }
    }


    /** Adds a crit row for each piece of hittable equipment to the panel of the location it is mounted in. */
    private void initEquipCrits() {
        for (Mounted<?> mounted : entity.getEquipment()) {
            if ((mounted.getLocation() == Entity.LOC_NONE) ||
                  !mounted.getType().isHittable() ||
                  mounted.isWeaponGroup()) {
                continue;
            }
            if (mounted.getType() instanceof InfantryAttack) {
                continue;
            }
            int nCrits = mounted.getNumCriticalSlots();
            int eqNum = entity.getEquipmentNum(mounted);
            int hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, mounted.getLocation());
            if (mounted.isSplit()) {
                hits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, mounted.getSecondLocation());
            }
            if ((mounted.getType() instanceof MiscType) && (mounted.getType().hasFlag(MiscType.F_PARTIAL_WING))) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, Mek.LOC_LEFT_TORSO);
                hits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, Mek.LOC_RIGHT_TORSO);
            }

            if (!(entity instanceof Mek)) {
                nCrits = 1;
                if (hits > 1) {
                    hits = 1;
                }
            }
            CheckCritPanel crit = new CheckCritPanel(nCrits, hits);
            equipCrits.put(eqNum, crit);
            String label = mounted.getName();
            if (mounted.isSplit()) {
                label += " (" + entity.getLocationAbbr(mounted.getLocation()) + "/"
                      + entity.getLocationAbbr(mounted.getSecondLocation()) + ")";
            }
            addLabeledRow(targetPanel(mounted.getLocation()), label, crit);
        }
    }

    /** Adds the unit-type specific system crits to the location panels they belong to. */
    private void initSystemCrits() {
        if (entity instanceof Mek) {
            setupMekSystemCrits();
        } else if (entity instanceof VTOL) {
            setupVtolSystemCrits();
        } else if (entity instanceof Tank) {
            setupTankSystemCrits();
        } else if (entity instanceof Aero) {
            setupAeroSystemCrits();
        } else if (entity instanceof ProtoMek) {
            setupProtoSystemCrits();
        }
    }

    private void setupMekSystemCrits() {
        /*
         * For the moment, I am going to cap out the number of hits at what the
         * record sheets show (i.e. 3 for engines). If we want to switch this to
         * the actual number then we can, see
         * enginePart.updateConditionFromEntity in MekHQ for an example of how
         * to retrieve all the available system crits
         */
        int centerEngineHits = 0;
        int leftEngineHits = 0;
        int rightEngineHits = 0;
        int gyroHits = 0;
        int cockpitHits = 0;
        int sensorHits = 0;
        int lifeSupportHits = 0;

        int centerEngineCrits = 0;
        int leftEngineCrits = 0;
        int rightEngineCrits = 0;
        int gyroCrits = 0;
        int cockpitCrits = 0;
        int sensorCrits = 0;
        int lifeSupportCrits = 0;
        for (int i = 0; i < entity.locations(); i++) {
            if (i == Mek.LOC_CENTER_TORSO) {
                centerEngineHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                centerEngineCrits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            if (i == Mek.LOC_LEFT_TORSO) {
                leftEngineHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                leftEngineCrits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            if (i == Mek.LOC_RIGHT_TORSO) {
                rightEngineHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                rightEngineCrits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            gyroHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, i);
            gyroCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, i);
            cockpitHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
            cockpitCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
            sensorHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, i);
            sensorCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, i);
            lifeSupportHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i);
            lifeSupportCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i);
        }
        centerEngineCrit = new CheckCritPanel(centerEngineCrits, centerEngineHits);
        addLabeledRow(targetPanel(Mek.LOC_CENTER_TORSO),
              Messages.getString("UnitEditorDialog.engine"),
              centerEngineCrit);

        if (entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_RIGHT_TORSO) > 0) {
            leftEngineCrit = new CheckCritPanel(leftEngineCrits, leftEngineHits);
            addLabeledRow(targetPanel(Mek.LOC_LEFT_TORSO),
                  Messages.getString("UnitEditorDialog.engine"),
                  leftEngineCrit);

            rightEngineCrit = new CheckCritPanel(rightEngineCrits, rightEngineHits);
            addLabeledRow(targetPanel(Mek.LOC_RIGHT_TORSO),
                  Messages.getString("UnitEditorDialog.engine"),
                  rightEngineCrit);
        }

        gyroCrit = new CheckCritPanel(gyroCrits, gyroHits);
        addLabeledRow(targetPanel(Mek.LOC_CENTER_TORSO), Messages.getString("UnitEditorDialog.gyro"), gyroCrit);

        sensorCrit = new CheckCritPanel(sensorCrits, sensorHits);
        addLabeledRow(targetPanel(Mek.LOC_HEAD), Messages.getString("UnitEditorDialog.sensor"), sensorCrit);

        lifeSupportCrit = new CheckCritPanel(lifeSupportCrits, lifeSupportHits);
        addLabeledRow(targetPanel(Mek.LOC_HEAD), Messages.getString("UnitEditorDialog.lifeSupport"), lifeSupportCrit);

        cockpitCrit = new CheckCritPanel(cockpitCrits, cockpitHits);
        addLabeledRow(targetPanel(Mek.LOC_HEAD), Messages.getString("UnitEditorDialog.cockpit"), cockpitCrit);

        if (entity instanceof LandAirMek) {
            lamAvionicsCrit = new TreeMap<>();
            lamLandingGearCrit = new TreeMap<>();
            for (int loc = 0; loc < entity.locations(); loc++) {
                int crits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, loc);
                if (crits > 0) {
                    int hits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, loc);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    lamAvionicsCrit.put(loc, critPanel);
                    addLabeledRow(targetPanel(loc), Messages.getString("UnitEditorDialog.avionics"), critPanel);
                }
                crits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, loc);
                if (crits > 0) {
                    int hits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, loc);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    lamLandingGearCrit.put(loc, critPanel);
                    addLabeledRow(targetPanel(loc), Messages.getString("UnitEditorDialog.landingGear"), critPanel);
                }
            }
        }

        final boolean tripod = entity.hasETypeFlag(Entity.ETYPE_TRIPOD_MEK);
        if (tripod) {
            actuatorCrits = new CheckCritPanel[5][4];
        } else if (entity instanceof QuadVee) {
            actuatorCrits = new CheckCritPanel[4][5];
        } else {
            actuatorCrits = new CheckCritPanel[4][4];
        }

        for (int loc = Mek.LOC_RIGHT_ARM; loc <= (tripod ? Mek.LOC_CENTER_LEG : Mek.LOC_LEFT_LEG); loc++) {
            int start = Mek.ACTUATOR_SHOULDER;
            int end = Mek.ACTUATOR_HAND;
            if ((loc >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek)) {
                start = Mek.ACTUATOR_HIP;
                end = Mek.ACTUATOR_FOOT;
            }

            for (int i = start; i <= end; i++) {
                if (!entity.hasSystem(i, loc)) {
                    continue;
                }
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                      entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, i, loc));
                actuatorCrits[loc - Mek.LOC_RIGHT_ARM][i - start] = actuatorCrit;
                addLabeledRow(targetPanel(loc), ((Mek) entity).getSystemName(i), actuatorCrit);
            }

            if (entity instanceof QuadVee) {
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                      entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, loc));
                actuatorCrits[loc - Mek.LOC_RIGHT_ARM][Mek.ACTUATOR_FOOT - Mek.ACTUATOR_HIP + 1] = actuatorCrit;
                addLabeledRow(targetPanel(loc),
                      ((Mek) entity).getSystemName(QuadVee.SYSTEM_CONVERSION_GEAR),
                      actuatorCrit);
            }
        }
    }

    private void setupTankSystemCrits() {
        Tank tank = (Tank) entity;

        int lock = 0;
        if (tank.isTurretLocked(0)) {
            lock = 1;
        }
        turretLockCrit = new CheckCritPanel(1, lock);
        int turretLocation = tank.hasNoTurret() ? Entity.LOC_NONE : tank.getLocTurret();
        addLabeledRow(targetPanel(turretLocation), Messages.getString("UnitEditorDialog.turretLock"), turretLockCrit);

        engineCrit = new CheckCritPanel(1, tank.getEngineHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.engine"), engineCrit);

        sensorCrit = new CheckCritPanel(Tank.CRIT_SENSOR_MAX, tank.getSensorHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sensor"), sensorCrit);

        int motiveHits = 0;
        // Do not check the crew when determining if we're immobile here
        if (tank.isImmobile(false)) {
            motiveHits = 4;
        } else if (tank.hasHeavyMovementDamage()) {
            motiveHits = 3;
        } else if (tank.hasModerateMovementDamage()) {
            motiveHits = 2;
        } else if (tank.hasMinorMovementDamage()) {
            motiveHits = 1;
        }
        motiveCrit = new CheckCritPanel(4, motiveHits);
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.motiveDamage"), motiveCrit);

        stabilizerCrits = new CheckCritPanel[tank.locations()];
        for (int loc = 0; loc < tank.locations(); loc++) {
            if ((loc == Tank.LOC_BODY) || (loc == tank.getLocTurret()) || (loc == tank.getLocTurret2())) {
                continue;
            }
            int hits = 0;
            if (tank.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            stabilizerCrits[loc] = stabCrit;
            addLabeledRow(targetPanel(loc), Messages.getString("UnitEditorDialog.stabilizer"), stabCrit);
        }
    }

    private void setupProtoSystemCrits() {
        ProtoMek proto = (ProtoMek) entity;

        protoCrits = new CheckCritPanel[proto.locations()];

        for (int loc = 0; loc < proto.locations(); loc++) {
            if ((loc == ProtoMek.LOC_MAIN_GUN) || (loc == ProtoMek.LOC_NEAR_MISS)) {
                continue;
            }
            int hits = 0;
            if ((loc == ProtoMek.LOC_LEFT_ARM) || (loc == ProtoMek.LOC_RIGHT_ARM)) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARM_CRIT, loc);
            }
            if (loc == ProtoMek.LOC_LEG) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEG_CRIT, loc);
            }
            if (loc == ProtoMek.LOC_HEAD) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_HEAD_CRIT, loc);
            }
            if (loc == ProtoMek.LOC_TORSO) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_CRIT, loc);
            }
            int nCrits = 2;
            if (loc == ProtoMek.LOC_LEG) {
                nCrits = 3;
            }
            CheckCritPanel protoCrit = new CheckCritPanel(nCrits, hits);
            protoCrits[loc] = protoCrit;
            addLabeledRow(targetPanel(loc), Messages.getString("UnitEditorDialog.crits"), protoCrit);
        }
    }

    private void setupVtolSystemCrits() {
        VTOL vtol = (VTOL) entity;

        int flightStabHit = 0;
        if (vtol.isStabiliserHit(VTOL.LOC_ROTOR)) {
            flightStabHit = 1;
        }
        flightStabilizerCrit = new CheckCritPanel(1, flightStabHit);
        addLabeledRow(targetPanel(VTOL.LOC_ROTOR),
              Messages.getString("UnitEditorDialog.flightStabilizer"),
              flightStabilizerCrit);

        engineCrit = new CheckCritPanel(1, vtol.getEngineHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.engine"), engineCrit);

        sensorCrit = new CheckCritPanel(Tank.CRIT_SENSOR_MAX, vtol.getSensorHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sensor"), sensorCrit);

        stabilizerCrits = new CheckCritPanel[vtol.locations()];
        for (int loc = 0; loc < vtol.locations(); loc++) {
            if ((loc == Tank.LOC_BODY) || (loc == VTOL.LOC_ROTOR)) {
                continue;
            }
            int hits = 0;
            if (vtol.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            stabilizerCrits[loc] = stabCrit;
            addLabeledRow(targetPanel(loc), Messages.getString("UnitEditorDialog.stabilizer"), stabCrit);
        }
    }

    private void setupAeroSystemCrits() {
        Aero aero = (Aero) entity;

        avionicsCrit = new CheckCritPanel(3, aero.getAvionicsHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.avionics"), avionicsCrit);

        if (aero instanceof Jumpship) {
            cicCrit = new CheckCritPanel(3, aero.getCICHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.cic"), cicCrit);
        } else {
            fcsCrit = new CheckCritPanel(3, aero.getFCSHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.fcs"), fcsCrit);
        }

        sensorCrit = new CheckCritPanel(3, aero.getSensorHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sensor"), sensorCrit);

        engineCrit = new CheckCritPanel(3, aero.getEngineHits());
        if ((aero instanceof Dropship) || (aero instanceof Jumpship)) {
            engineCrit = new CheckCritPanel(6, aero.getEngineHits());
        }
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.engine"), engineCrit);

        if (!(aero instanceof Jumpship)) {
            int gearHits = 0;
            if (aero.isGearHit()) {
                gearHits = 1;
            }
            gearCrit = new CheckCritPanel(1, gearHits);
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.landingGear"), gearCrit);
        }

        int lifeHits = 0;
        if (!aero.hasLifeSupport()) {
            lifeHits = 1;
        }
        lifeSupportCrit = new CheckCritPanel(1, lifeHits);
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.lifeSupport"), lifeSupportCrit);

        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            leftThrusterCrit = new CheckCritPanel(4, aero.getLeftThrustHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.leftThruster"), leftThrusterCrit);

            rightThrusterCrit = new CheckCritPanel(4, aero.getRightThrustHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.rightThruster"), rightThrusterCrit);
        }

        if (aero instanceof Jumpship js) {
            gravDeckCrit = new CheckCritPanel(js.getTotalGravDeck(), js.getTotalDamagedGravDeck());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.gravDecks"), gravDeckCrit);

            Vector<DockingCollar> collars = aero.getDockingCollars();
            int damagedCollars = 0;
            for (DockingCollar nextDC : aero.getDockingCollars()) {
                if (nextDC.isDamaged()) {
                    damagedCollars++;
                }
            }
            collarDamage = new JSpinner(new SpinnerNumberModel(collars.size() - damagedCollars,
                  0,
                  collars.size(),
                  1.0));
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.dockingCollars"), collarDamage);

            kfDamage = new JSpinner(new SpinnerNumberModel(js.getKFIntegrity(), 0, js.getOKFIntegrity(), 1.0));
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.kfIntegrity"), kfDamage);

            // K-F Drive Components (Optional)
            if (entity.getGame()
                  .getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_EXPANDED_KF_DRIVE_DAMAGE)) {
                int driveCoilHits = 0;
                if (js.getKFDriveCoilHit()) {
                    driveCoilHits = 1;
                }
                driveCoilCrit = new CheckCritPanel(1, driveCoilHits);
                addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.driveCoil"), driveCoilCrit);

                int chargingSystemHits = 0;
                if (js.getKFChargingSystemHit()) {
                    chargingSystemHits = 1;
                }
                chargingSystemCrit = new CheckCritPanel(1, chargingSystemHits);
                addLabeledRow(generalPanel(),
                      Messages.getString("UnitEditorDialog.chargingSystem"),
                      chargingSystemCrit);

                int fieldInitiatorHits = 0;
                if (js.getKFFieldInitiatorHit()) {
                    fieldInitiatorHits = 1;
                }
                fieldInitiatorCrit = new CheckCritPanel(1, fieldInitiatorHits);
                addLabeledRow(generalPanel(),
                      Messages.getString("UnitEditorDialog.fieldInitiator"),
                      fieldInitiatorCrit);

                int driveControllerHits = 0;
                if (js.getKFDriveControllerHit()) {
                    driveControllerHits = 1;
                }
                driveControllerCrit = new CheckCritPanel(1, driveControllerHits);
                addLabeledRow(generalPanel(),
                      Messages.getString("UnitEditorDialog.driveController"),
                      driveControllerCrit);

                int heliumTankHits = 0;
                if (js.getKFHeliumTankHit()) {
                    heliumTankHits = 1;
                }
                heliumTankCrit = new CheckCritPanel(1, heliumTankHits);
                addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.heliumTank"), heliumTankCrit);

                if (js.hasLF()) {
                    int lfBatteryHits = 0;
                    if (js.getLFBatteryHit()) {
                        lfBatteryHits = 1;
                    }
                    lfBatteryCrit = new CheckCritPanel(1, lfBatteryHits);
                    addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.lfBattery"), lfBatteryCrit);
                }
            }

            sailDamage = new JSpinner(new SpinnerNumberModel(js.getSailIntegrity(), 0, js.getOSailIntegrity(), 1.0));
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sailIntegrity"), sailDamage);
        }

        if (aero instanceof Dropship) {
            int collarHits = 0;
            if (((Dropship) aero).isDockCollarDamaged()) {
                collarHits = 1;
            }
            dockCollarCrit = new CheckCritPanel(1, collarHits);
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.dropshipCollar"), dockCollarCrit);

            int kfBoomHits = 0;
            if (((Dropship) aero).isKFBoomDamaged()) {
                kfBoomHits = 1;
            }
            kfBoomCrit = new CheckCritPanel(1, kfBoomHits);
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.kfBoom"), kfBoomCrit);
        }

        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            int b = 0;
            Vector<Bay> bays = aero.getTransportBays();
            bayDamage = new JSpinner[bays.size()];
            bayDoorCrit = new CheckCritPanel[bays.size()];
            for (Bay nextbay : bays) {
                JSpinner bayCrit = new JSpinner(new SpinnerNumberModel(nextbay.getCapacity() - nextbay.getBayDamage(),
                      0,
                      nextbay.getCapacity(),
                      nextbay.isCargo() ? 0.5 : 1.0));
                bayDamage[b] = bayCrit;
                addLabeledRow(generalPanel(),
                      String.format(Messages.getString("UnitEditorDialog.bayCrit"),
                            nextbay.getTransporterType(),
                            nextbay.getBayNumber()),
                      bayCrit);

                CheckCritPanel doorCrit = new CheckCritPanel(nextbay.getDoors(),
                      (nextbay.getDoors() - nextbay.getCurrentDoors()));
                bayDoorCrit[b] = doorCrit;
                addLabeledRow(generalPanel(),
                      String.format(Messages.getString("UnitEditorDialog.bayDoorCrit"), nextbay.getBayNumber()),
                      doorCrit);
                b++;
            }
        }
    }

    /**
     * Applies the given number of total crits to a Super-Cooled Myomer (which is spread over 6 locations).
     */
    public void damageSCM(Entity entity, int eqNum, int hits) {
        int numHits = 0;
        Mounted<?> m = entity.getEquipment(eqNum);
        for (int loc = 0; loc < entity.locations(); loc++) {
            for (int i = 0; i < entity.getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot cs = entity.getCritical(loc, i);
                if ((cs == null) ||
                      (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) ||
                      ((m != cs.getMount()) && (m != cs.getMount2()))) {
                    continue;
                }

                if (numHits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    numHits++;
                } else {
                    cs.setHit(false);
                    cs.setDestroyed(false);
                    cs.setRepairable(true);
                }
            }
        }
    }

    private void btnOkayActionPerformed(java.awt.event.ActionEvent actionEvent) {
        for (int i = 0; i < entity.locations(); i++) {
            if (null != spnInternal[i]) {
                int internal = (Integer) spnInternal[i].getModel().getValue();
                if (internal <= 0) {
                    internal = IArmorState.ARMOR_DESTROYED;
                }
                if ((entity instanceof Aero) && (i == 0)) {
                    ((Aero) entity).setSI(internal);
                } else {
                    entity.setInternal(internal, i);
                }
            }
            if (null != spnArmor[i]) {
                int armor = (Integer) spnArmor[i].getModel().getValue();
                if (armor <= 0) {
                    armor = IArmorState.ARMOR_DESTROYED;
                }
                entity.setArmor(armor, i);
            }
            if (entity.hasRearArmor(i) && (null != spnRear[i])) {
                int rear = (Integer) spnRear[i].getModel().getValue();
                if (rear <= 0) {
                    rear = IArmorState.ARMOR_DESTROYED;
                }
                entity.setArmor(rear, i, true);
            }
        }
        for (Mounted<?> m : entity.getEquipment()) {
            int eqNum = entity.getEquipmentNum(m);
            CheckCritPanel crit = equipCrits.get(eqNum);
            if (null != crit) {
                int hits = crit.getHits();
                if (m.is(EquipmentTypeLookup.SCM)) {
                    m.setDestroyed(hits >= 6);
                    m.setHit(hits >= 6);
                    damageSCM(entity, eqNum, hits);
                } else {
                    m.setDestroyed(hits > 0);
                    m.setHit(hits > 0);
                    entity.damageSystem(CriticalSlot.TYPE_EQUIPMENT, eqNum, hits);
                }
            }
        }
        if (entity instanceof ConvInfantry infantry) {
            infantry.damageOrRestoreFieldWeapons();
            entity.applyDamage();
        }

        // now systems
        if (entity instanceof Mek) {
            if (null != centerEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_CENTER_TORSO,
                      centerEngineCrit.getHits());
            }
            if (null != leftEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_LEFT_TORSO,
                      leftEngineCrit.getHits());
            }
            if (null != rightEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_RIGHT_TORSO,
                      rightEngineCrit.getHits());
            }
            if (null != gyroCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, gyroCrit.getHits());
            }
            if (null != sensorCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, sensorCrit.getHits());
            }
            if (null != lifeSupportCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, lifeSupportCrit.getHits());
            }
            if (null != cockpitCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, cockpitCrit.getHits());
            }
            if (null != lamAvionicsCrit && !lamAvionicsCrit.isEmpty()) {
                for (int loc : lamAvionicsCrit.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          LandAirMek.LAM_AVIONICS,
                          loc,
                          lamAvionicsCrit.get(loc).getHits());
                }
            }
            if (null != lamLandingGearCrit && !lamLandingGearCrit.isEmpty()) {
                for (int loc : lamLandingGearCrit.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          LandAirMek.LAM_LANDING_GEAR,
                          loc,
                          lamLandingGearCrit.get(loc).getHits());
                }
            }

            for (int i = 0; i < actuatorCrits.length; i++) {
                for (int j = 0; j < actuatorCrits[i].length; j++) {
                    CheckCritPanel actuatorCrit = actuatorCrits[i][j];
                    if (null == actuatorCrit) {
                        continue;
                    }
                    int loc = i + Mek.LOC_RIGHT_ARM;
                    int actuator = j + Mek.ACTUATOR_SHOULDER;
                    if ((loc >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek)) {
                        actuator = j + Mek.ACTUATOR_HIP;
                    }
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator, loc, actuatorCrit.getHits());
                }

                if (entity instanceof QuadVee) {
                    for (int j = 0; j < actuatorCrits.length; j++) {
                        CheckCritPanel actuatorCrit = actuatorCrits[i][4];
                        if (null == actuatorCrit) {
                            continue;
                        }
                        int loc = i + Mek.LOC_RIGHT_ARM;
                        int actuator = QuadVee.SYSTEM_CONVERSION_GEAR;
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator, loc, actuatorCrit.getHits());
                    }
                }
            }
        } else if (entity instanceof ProtoMek) {
            for (int loc = 0; loc < entity.locations(); loc++) {
                if (null == protoCrits[loc]) {
                    continue;
                }
                if ((loc == ProtoMek.LOC_LEFT_ARM) || (loc == ProtoMek.LOC_RIGHT_ARM)) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_ARM_CRIT,
                          loc,
                          protoCrits[loc].getHits());
                }
                if (loc == ProtoMek.LOC_LEG) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_LEG_CRIT,
                          loc,
                          protoCrits[loc].getHits());
                }
                if (loc == ProtoMek.LOC_HEAD) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_HEAD_CRIT,
                          loc,
                          protoCrits[loc].getHits());
                }
                if (loc == ProtoMek.LOC_TORSO) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_TORSO_CRIT,
                          loc,
                          protoCrits[loc].getHits());
                }
            }
        } else if (entity instanceof Tank tank) {
            if (null != engineCrit) {
                if (engineCrit.getHits() > 0) {
                    tank.engineHit();
                } else {
                    tank.engineFix();
                }
            }
            if (null != turretLockCrit) {
                if (turretLockCrit.getHits() > 0) {
                    tank.lockTurret(0);
                } else {
                    tank.unlockTurret();
                }
            }
            if (null != sensorCrit) {
                tank.setSensorHits(sensorCrit.getHits());
            }
            if (null != motiveCrit) {
                tank.resetMovementDamage();
                tank.addMovementDamage(motiveCrit.getHits());

                // Apply movement damage immediately in case we've decided to immobilize the
                // tank
                tank.applyMovementDamage();
            }
            if ((tank instanceof VTOL) && (null != flightStabilizerCrit)) {
                if (flightStabilizerCrit.getHits() > 0) {
                    tank.setStabiliserHit(VTOL.LOC_ROTOR);
                } else {
                    tank.clearStabiliserHit(VTOL.LOC_ROTOR);
                }
            }
            for (int loc = 0; loc < tank.locations(); loc++) {
                CheckCritPanel stabCrit = stabilizerCrits[loc];
                if (null == stabCrit) {
                    continue;
                }
                if (stabCrit.getHits() > 0) {
                    tank.setStabiliserHit(loc);
                } else {
                    tank.clearStabiliserHit(loc);
                }
            }
        } else if (entity instanceof Aero aero) {
            if (null != avionicsCrit) {
                aero.setAvionicsHits(avionicsCrit.getHits());
            }
            if (null != fcsCrit) {
                aero.setFCSHits(fcsCrit.getHits());
            }
            if (null != cicCrit) {
                aero.setCICHits(cicCrit.getHits());
            }
            if (null != engineCrit) {
                aero.setEngineHits(engineCrit.getHits());
            }
            if (null != sensorCrit) {
                aero.setSensorHits(sensorCrit.getHits());
            }
            if (null != gearCrit) {
                aero.setGearHit(gearCrit.getHits() > 0);
            }
            if (null != lifeSupportCrit) {
                aero.setLifeSupport(lifeSupportCrit.getHits() == 0);
            }
            if (null != leftThrusterCrit) {
                aero.setLeftThrustHits(leftThrusterCrit.getHits());
            }
            if (null != rightThrusterCrit) {
                aero.setRightThrustHits(rightThrusterCrit.getHits());
            }
            if ((null != dockCollarCrit) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageDockCollar(dockCollarCrit.getHits() > 0);
            }
            if ((null != kfBoomCrit) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageKFBoom(kfBoomCrit.getHits() > 0);
            }
            // cargo bays and bay doors
            if ((aero instanceof Dropship) || (aero instanceof Jumpship)) {
                int b = 0;
                for (Bay bay : aero.getTransportBays()) {
                    JSpinner bayCrit = bayDamage[b];
                    if (null == bayCrit) {
                        continue;
                    }
                    bay.setBayDamage(bay.getCapacity() - (Double) bayCrit.getModel().getValue());
                    CheckCritPanel doorCrit = bayDoorCrit[b];
                    if (null == doorCrit) {
                        continue;
                    }
                    if ((bay.getCurrentDoors() > 0) && (doorCrit.getHits() > 0)) {
                        bay.setCurrentDoors(bay.getDoors() - doorCrit.getHits());

                    } else if (doorCrit.getHits() == 0) {
                        bay.setCurrentDoors(bay.getDoors());
                    }
                    // for ASF and SC bays, we have to update recovery slots as doors are changed
                    if (bay instanceof ASFBay asfBay) {
                        asfBay.initializeRecoverySlots();
                    }
                    if (bay instanceof SmallCraftBay smallCraftBay) {
                        smallCraftBay.initializeRecoverySlots();
                    }
                    b++;
                }
            }
            // Jumpship Docking Collars, KF Drive, Sail and Grav Decks
            if (aero instanceof Jumpship jumpship) {
                JSpinner collarCrit = collarDamage;
                CheckCritPanel deckCrit = gravDeckCrit;
                double damagedCollars = 0.0;
                int damagedDecks = 0;
                if (null != collarCrit) {
                    damagedCollars = (aero.getDockingCollars().size() - (double) collarCrit.getModel().getValue());
                }
                // First, reset damaged collars to undamaged. Otherwise, you get weirdness when
                // running this dialogue multiple times
                for (DockingCollar collar : aero.getDockingCollars()) {
                    collar.setDamaged(false);
                }
                // Otherwise, run through the list and damage one until the spinner value is
                // satisfied
                for (DockingCollar collar : aero.getDockingCollars()) {
                    if (damagedCollars <= 0) {
                        break;
                    }
                    collar.setDamaged(true);
                    damagedCollars--;
                }
                if (null != deckCrit) {
                    damagedDecks = deckCrit.getHits();
                }
                // reset all grav decks to undamaged
                for (int i = 0; i < jumpship.getTotalGravDeck(); i++) {
                    jumpship.setGravDeckDamageFlag(i, 0);
                }
                if (damagedDecks > 0) {
                    // loop through the grav decks from #1 and damage them
                    for (int i = 0; i < damagedDecks; i++) {
                        jumpship.setGravDeckDamageFlag(i, 1);
                    }
                }
                // KF Drive and Sail
                if (null != kfDamage) {
                    double kfIntegrity = (double) kfDamage.getModel().getValue();
                    jumpship.setKFIntegrity((int) kfIntegrity);
                }
                if (null != chargingSystemCrit) {
                    jumpship.setKFChargingSystemHit(chargingSystemCrit.getHits() > 0);
                }
                if (null != driveCoilCrit) {
                    jumpship.setKFDriveCoilHit(driveCoilCrit.getHits() > 0);
                }
                if (null != driveControllerCrit) {
                    jumpship.setKFDriveControllerHit(driveControllerCrit.getHits() > 0);
                }
                if (null != fieldInitiatorCrit) {
                    jumpship.setKFFieldInitiatorHit(fieldInitiatorCrit.getHits() > 0);
                }
                if (null != heliumTankCrit) {
                    jumpship.setKFHeliumTankHit(heliumTankCrit.getHits() > 0);
                }
                if (null != lfBatteryCrit) {
                    jumpship.setLFBatteryHit(lfBatteryCrit.getHits() > 0);
                }
                if (null != sailDamage) {
                    double sailIntegrity = (double) sailDamage.getModel().getValue();
                    jumpship.setSailIntegrity((int) sailIntegrity);
                }
            }
        }

    }

    private static class CheckCritPanel extends JPanel {

        /**
         *
         */
        @Serial
        private static final long serialVersionUID = 8662728291188274362L;

        private final ArrayList<JCheckBox> checks = new ArrayList<>();

        public CheckCritPanel(int crits, int current) {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            for (int i = 0; i < crits; i++) {
                JCheckBox check = new JCheckBox("");
                check.setActionCommand(Integer.toString(i));
                check.addActionListener(this::checkBoxes);
                checks.add(check);
                add(check);
            }

            if (current > 0) {
                for (int i = 0; i < current && i < checks.size(); i++) {
                    checks.get(i).setSelected(true);
                }
            }
        }

        public int getHits() {
            int hits = 0;
            for (JCheckBox check : checks) {
                if (check.isSelected()) {
                    hits++;
                }
            }
            return hits;
        }

        public void setHits(int hits) {
            for (int i = 0; i < checks.size(); i++) {
                checks.get(i).setSelected(i < hits);
            }
        }

        private void checkBoxes(ActionEvent evt) {
            int hits = MathUtility.parseInt(evt.getActionCommand());
            boolean selected = checks.get(hits).isSelected();
            if (selected) {
                // check all those up to this one
                for (int i = 0; i < hits; i++) {
                    checks.get(i).setSelected(true);
                }
            } else if (hits < checks.size()) {
                // deselect any above this one
                for (int i = hits; i < checks.size(); i++) {
                    checks.get(i).setSelected(false);
                }
            }

        }
    }
}
