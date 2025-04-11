/*
 * Copyright (C) 2013 Jay Lawson
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.function.BiConsumer;
import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.bays.ASFBay;
import megamek.common.bays.Bay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.options.OptionsConstants;

/**
 * This dialog will allow the user to edit the damage and status characteristics of a unit. This is designed for use in
 * both MegaMek and MHQ so don't go messing things up for MHQ by changing a bunch of stuff
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitEditorDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 8144354264100884817L;

    private final Entity entity;

    JPanel panArmor;
    JPanel panSystem;
    JPanel panEquip;

    JSpinner[] spnInternal;
    JSpinner[] spnArmor;
    JSpinner[] spnRear;

    HashMap<Integer, CheckCriticalSlotPanel> equipCriticalSlots;

    // system critical slots
    CheckCriticalSlotPanel engineCriticalSlot;
    CheckCriticalSlotPanel leftEngineCriticalSlot;
    CheckCriticalSlotPanel rightEngineCriticalSlots;
    CheckCriticalSlotPanel centerEngineCriticalSlots;
    CheckCriticalSlotPanel gyroCriticalSlots;
    CheckCriticalSlotPanel sensorCriticalSlots;
    CheckCriticalSlotPanel lifeSupportCriticalSlots;
    CheckCriticalSlotPanel cockpitCriticalSlots;
    Map<Integer, CheckCriticalSlotPanel> lamAvionicsCriticalSlots;
    Map<Integer, CheckCriticalSlotPanel> lamLandingGearCriticalSlots;
    CheckCriticalSlotPanel[][] actuatorCriticalSlots;
    CheckCriticalSlotPanel turretLockCriticalSlot;
    CheckCriticalSlotPanel motiveCriticalSlot;
    CheckCriticalSlotPanel[] stabilizerCriticalSlots;
    CheckCriticalSlotPanel flightStabilizerCriticalSlot;
    CheckCriticalSlotPanel avionicsCriticalSlot;
    CheckCriticalSlotPanel fcsCriticalSlot;
    CheckCriticalSlotPanel cicCriticalSlot;
    CheckCriticalSlotPanel gearCriticalSlot;
    CheckCriticalSlotPanel leftThrusterCriticalSlot;
    CheckCriticalSlotPanel rightThrusterCriticalSlot;
    CheckCriticalSlotPanel kfBoomCriticalSlot;
    CheckCriticalSlotPanel dockCollarCriticalSlot;
    CheckCriticalSlotPanel gravDeckCriticalSlot;
    JSpinner[] bayDamage;
    CheckCriticalSlotPanel[] bayDoorCriticalSlot;
    JSpinner collarDamage;
    JSpinner kfDamage;
    CheckCriticalSlotPanel driveCoilCriticalSlot;
    CheckCriticalSlotPanel chargingSystemCriticalSlot;
    CheckCriticalSlotPanel fieldInitiatorCriticalSlot;
    CheckCriticalSlotPanel driveControllerCriticalSlot;
    CheckCriticalSlotPanel heliumTankCriticalSlot;
    CheckCriticalSlotPanel lfBatteryCriticalSlot;
    JSpinner sailDamage;
    CheckCriticalSlotPanel[] protoCriticalSlots;

    public UnitEditorDialog(JFrame parent, Entity m) {
        super(parent, true);
        entity = m;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        getContentPane().setLayout(new BorderLayout());

        setTitle("Edit damage for " + entity.getDisplayName());

        JPanel panMain = new JPanel(new GridBagLayout());
        JPanel panButtons = new JPanel(new GridLayout(1, 2));

        // TODO: ProtoMeks
        initArmorPanel();
        initSystemPanel();
        initEquipPanel();

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panMain.add(panArmor, gridBagConstraints);
        if (!entity.isConventionalInfantry()) {
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weighty = 1.0;
            panMain.add(new JScrollPane(panSystem), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridheight = 2;
            panMain.add(new JScrollPane(panEquip), gridBagConstraints);
        }

        getContentPane().add(panMain, BorderLayout.CENTER);

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

        // TODO: size right

        String closeAction = "closeAction";
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
        getRootPane().getActionMap().put(closeAction, new CloseAction(this));

        pack();
    }

    private void initArmorPanel() {
        if (entity instanceof Aero) {
            initAeroArmorPanel();
            return;
        } else if (entity.isConventionalInfantry()) {
            initInfantryArmorPanel();
            return;
        }

        GridBagConstraints gridBagConstraints;

        panArmor = new JPanel(new GridBagLayout());
        panArmor.setBorder(BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.internalsAndArmor")));

        spnArmor = new JSpinner[entity.locations()];
        spnInternal = new JSpinner[entity.locations()];
        spnRear = new JSpinner[entity.locations()];

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panArmor.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.location") + "</b></html>"),
              gridBagConstraints);
        gridBagConstraints.gridx = 1;
        panArmor.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.internal") + "</b></html>"),
              gridBagConstraints);
        gridBagConstraints.gridx = 2;
        panArmor.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.frontArmor") + "</b></html>"),
              gridBagConstraints);
        gridBagConstraints.gridx = 3;
        panArmor.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.rearArmor") + "</b></html>"),
              gridBagConstraints);

        for (int i = 0; i < entity.locations(); i++) {
            // some units have hidden locations, skip these
            if (entity.getOInternal(i) <= 0) {
                continue;
            }
            int internal = Math.max(entity.getInternal(i), 0);
            int armor = Math.max(entity.getArmor(i, false), 0);
            spnArmor[i] = new JSpinner(new SpinnerNumberModel(armor, 0, entity.getOArmor(i), 1));
            spnInternal[i] = new JSpinner(new SpinnerNumberModel(internal, 0, entity.getOInternal(i), 1));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i + 1;
            gridBagConstraints.insets = new Insets(1, 10, 1, 1);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panArmor.add(new JLabel(entity.getLocationName(i)), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            panArmor.add(spnInternal[i], gridBagConstraints);
            gridBagConstraints.gridx = 2;
            panArmor.add(spnArmor[i], gridBagConstraints);
            if (entity.hasRearArmor(i)) {
                int rear = Math.max(entity.getArmor(i, true), 0);
                spnRear[i] = new JSpinner(new SpinnerNumberModel(rear, 0, entity.getOArmor(i, true), 1));
                gridBagConstraints.gridx = 3;
                panArmor.add(spnRear[i], gridBagConstraints);
            }
        }
    }

    private void initAeroArmorPanel() {
        GridBagConstraints gridBagConstraints;

        Aero aero = (Aero) entity;

        panArmor = new JPanel(new GridBagLayout());
        panArmor.setBorder(BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.siAndArmor")));

        spnArmor = new JSpinner[entity.locations()];
        spnInternal = new JSpinner[entity.locations()];
        spnRear = new JSpinner[entity.locations()];

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panArmor.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.location") + "</b></html>"),
              gridBagConstraints);
        gridBagConstraints.gridx = 1;
        panArmor.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.points") + "</b></html>"),
              gridBagConstraints);

        int si = Math.max(aero.getSI(), 0);
        spnInternal[0] = new JSpinner(new SpinnerNumberModel(si, 0, aero.getOSI(), 1));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panArmor.add(new JLabel(Messages.getString("UnitEditorDialog.structuralIntegrity")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        panArmor.add(spnInternal[0], gridBagConstraints);

        for (int i = 0; i < entity.locations(); i++) {
            // some units have hidden locations, skip these
            if (entity.getOArmor(i) <= 0) {
                continue;
            }
            int armor = Math.max(entity.getArmor(i, false), 0);
            spnArmor[i] = new JSpinner(new SpinnerNumberModel(armor, 0, entity.getOArmor(i), 1));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i + 2;
            gridBagConstraints.insets = new Insets(1, 10, 1, 1);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panArmor.add(new JLabel(entity.getLocationName(i)), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            panArmor.add(spnArmor[i], gridBagConstraints);
        }
    }

    private void initInfantryArmorPanel() {
        GridBagConstraints gridBagConstraints;

        Infantry infantry = (Infantry) entity;

        panArmor = new JPanel(new GridBagLayout());
        panArmor.setBorder(BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.troopersLeft")));

        spnArmor = new JSpinner[entity.locations()];
        spnInternal = new JSpinner[entity.locations()];
        spnRear = new JSpinner[entity.locations()];

        int men = Math.max(infantry.getShootingStrength(), 0);
        spnInternal[0] = new JSpinner(new SpinnerNumberModel(men,
              0,
              infantry.getSquadCount() * infantry.getSquadSize(),
              1));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panArmor.add(new JLabel(Messages.getString("UnitEditorDialog.menLeft")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        panArmor.add(spnInternal[0], gridBagConstraints);
    }

    private void initEquipPanel() {
        equipCriticalSlots = new HashMap<>();
        panEquip = new JPanel();
        panEquip.setLayout(new GridBagLayout());
        panEquip.setBorder(BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.equipment")));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        for (Mounted<?> m : entity.getEquipment()) {
            if ((m.getLocation() == Entity.LOC_NONE) || !m.getType().isHittable() || m.isWeaponGroup()) {
                continue;
            }
            int numCriticalSlots = m.getCriticals();
            int eqNum = entity.getEquipmentNum(m);
            int hits = entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, eqNum, m.getLocation());
            if (m.isSplit()) {
                hits += entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, eqNum, m.getSecondLocation());
            }
            if (m.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, eqNum, Mek.LOC_LT);
                hits += entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, eqNum, Mek.LOC_RT);
            }

            if (!(entity instanceof Mek)) {
                numCriticalSlots = 1;
                if (hits > 1) {
                    hits = 1;
                }
            }
            CheckCriticalSlotPanel checkCriticalPanel = new CheckCriticalSlotPanel(numCriticalSlots, hits);
            equipCriticalSlots.put(eqNum, checkCriticalPanel);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            panEquip.add(new JLabel("<html><b>" +
                                          m.getName() +
                                          "</b><br>" +
                                          entity.getLocationName(m.getLocation()) +
                                          "</html>"), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panEquip.add(checkCriticalPanel, gridBagConstraints);
            gridBagConstraints.gridy++;
        }
    }

    private void initSystemPanel() {

        // systems are the hard part, because these are all unit specific
        // lets start with a mek
        panSystem = new JPanel(new GridBagLayout());
        panSystem.setBorder(BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.system")));

        if (entity instanceof Mek) {
            setupMekSystemPanel();
        } else if (entity instanceof VTOL) {
            setupVtolSystemPanel();
        } else if (entity instanceof Tank) {
            setupTankSystemPanel();
        } else if (entity instanceof Aero) {
            setupAeroSystemPanel();
        } else if (entity instanceof ProtoMek) {
            setupProtoSystemPanel();
        }
    }

    private void setupMekSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        /*
         * For the moment, I am going to cap out the number of hits at what the record sheets show (i.e. 3 for
         * engines). If we want to switch this to the actual number then we can, see <code>enginePart
         * .updateConditionFromEntity</code> in MekHQ for an example of how to retrieve all the available system
         * critical
         *  slots
         */
        int centerEngineHits = 0;
        int leftEngineHits = 0;
        int rightEngineHits = 0;
        int gyroHits = 0;
        int cockpitHits = 0;
        int sensorHits = 0;
        int lifeSupportHits = 0;

        int centerEngineCriticalSlots = 0;
        int leftEngineCriticalSlots = 0;
        int rightEngineCriticalSlots = 0;
        int gyroCriticalSlots = 0;
        int cockpitCriticalSlots = 0;
        int sensorCriticalSlots = 0;
        int lifeSupportCriticalSlots = 0;
        for (int i = 0; i < entity.locations(); i++) {
            if (i == Mek.LOC_CT) {
                centerEngineHits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                centerEngineCriticalSlots = entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            if (i == Mek.LOC_LT) {
                leftEngineHits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                leftEngineCriticalSlots = entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            if (i == Mek.LOC_RT) {
                rightEngineHits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                rightEngineCriticalSlots = entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            gyroHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, i);
            gyroCriticalSlots += entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, i);
            cockpitHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
            cockpitCriticalSlots += entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
            sensorHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, i);
            sensorCriticalSlots += entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, i);
            lifeSupportHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i);
            lifeSupportCriticalSlots += entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_LIFE_SUPPORT,
                  i);
        }
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engine") + "</b><br></html>"),
              gridBagConstraints);
        this.centerEngineCriticalSlots = new CheckCriticalSlotPanel(centerEngineCriticalSlots, centerEngineHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(this.centerEngineCriticalSlots, gridBagConstraints);

        if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_RT) > 0) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engineLT") + "</b><br></html>"),
                  gridBagConstraints);
            leftEngineCriticalSlot = new CheckCriticalSlotPanel(leftEngineCriticalSlots, leftEngineHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(leftEngineCriticalSlot, gridBagConstraints);

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engineRT") + "</b><br></html>"),
                  gridBagConstraints);
            this.rightEngineCriticalSlots = new CheckCriticalSlotPanel(rightEngineCriticalSlots, rightEngineHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(this.rightEngineCriticalSlots, gridBagConstraints);
        }

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.gyro") + "</b><br></html>"),
              gridBagConstraints);
        this.gyroCriticalSlots = new CheckCriticalSlotPanel(gyroCriticalSlots, gyroHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(this.gyroCriticalSlots, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
              gridBagConstraints);
        this.sensorCriticalSlots = new CheckCriticalSlotPanel(sensorCriticalSlots, sensorHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(this.sensorCriticalSlots, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.lifeSupport") + "</b><br></html>"),
              gridBagConstraints);
        this.lifeSupportCriticalSlots = new CheckCriticalSlotPanel(lifeSupportCriticalSlots, lifeSupportHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(this.lifeSupportCriticalSlots, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.cockpit") + "</b><br></html>"),
              gridBagConstraints);
        this.cockpitCriticalSlots = new CheckCriticalSlotPanel(cockpitCriticalSlots, cockpitHits);
        gridBagConstraints.gridx = 1;
        panSystem.add(this.cockpitCriticalSlots, gridBagConstraints);

        if (entity instanceof LandAirMek) {
            lamAvionicsCriticalSlots = new TreeMap<>();
            lamLandingGearCriticalSlots = new TreeMap<>();
            for (int loc = 0; loc < entity.locations(); loc++) {
                int criticalSlots = entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, loc);
                if (criticalSlots > 0) {
                    int hits = entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, loc);
                    CheckCriticalSlotPanel criticalSlotPanel = new CheckCriticalSlotPanel(criticalSlots, hits);
                    lamAvionicsCriticalSlots.put(loc, criticalSlotPanel);
                }
                criticalSlots = entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, loc);
                if (criticalSlots > 0) {
                    int hits = entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, loc);
                    CheckCriticalSlotPanel criticalSlotPanel = new CheckCriticalSlotPanel(criticalSlots, hits);
                    lamLandingGearCriticalSlots.put(loc, criticalSlotPanel);
                }
            }
            BiConsumer<Map.Entry<Integer, CheckCriticalSlotPanel>, String> addToPanel = (entry, critName) -> {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               String.format(Messages.getString("UnitEditorDialog.critStringLocation"),
                                                     critName,
                                                     entity.getLocationAbbr(entry.getKey())) +
                                               "</b><br></html>"), gridBagConstraints);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(entry.getValue(), gridBagConstraints);
            };
            lamAvionicsCriticalSlots.entrySet()
                  .forEach(e -> addToPanel.accept(e, Messages.getString("UnitEditorDialog.avionics")));
            lamLandingGearCriticalSlots.entrySet()
                  .forEach(e -> addToPanel.accept(e, Messages.getString("UnitEditorDialog.landingGear")));
        }

        final boolean tripod = entity.hasETypeFlag(Entity.ETYPE_TRIPOD_MEK);
        if (tripod) {
            actuatorCriticalSlots = new CheckCriticalSlotPanel[5][4];
        } else if (entity instanceof QuadVee) {
            actuatorCriticalSlots = new CheckCriticalSlotPanel[4][5];
        } else {
            actuatorCriticalSlots = new CheckCriticalSlotPanel[4][4];
        }

        for (int loc = Mek.LOC_RARM; loc <= (tripod ? Mek.LOC_CLEG : Mek.LOC_LLEG); loc++) {
            int start = Mek.ACTUATOR_SHOULDER;
            int end = Mek.ACTUATOR_HAND;
            if ((loc >= Mek.LOC_RLEG) || (entity instanceof QuadMek)) {
                start = Mek.ACTUATOR_HIP;
                end = Mek.ACTUATOR_FOOT;
            }

            for (int i = start; i <= end; i++) {
                if (!entity.hasSystem(i, loc)) {
                    continue;
                }
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                gridBagConstraints.weighty = 0.0;
                if ((loc == (tripod ? Mek.LOC_CLEG : Mek.LOC_LLEG)) && (i == end) && !(entity instanceof QuadVee)) {
                    gridBagConstraints.weighty = 1.0;
                }
                panSystem.add(new JLabel("<html><b>" +
                                               String.format(Messages.getString("UnitEditorDialog.critString"),
                                                     entity.getLocationName(loc),
                                                     ((Mek) entity).getSystemName(i)) +
                                               "</b><br></html>"), gridBagConstraints);
                CheckCriticalSlotPanel actuatorCriticalSlotPanel = new CheckCriticalSlotPanel(1,
                      entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, i, loc));
                actuatorCriticalSlots[loc - Mek.LOC_RARM][i - start] = actuatorCriticalSlotPanel;
                gridBagConstraints.gridx = 1;
                panSystem.add(actuatorCriticalSlotPanel, gridBagConstraints);
            }

            if (entity instanceof QuadVee) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                gridBagConstraints.weighty = 0.0;
                if (loc == Mek.LOC_LLEG) {
                    gridBagConstraints.weighty = 1.0;
                }
                panSystem.add(new JLabel("<html><b>" +
                                               String.format(Messages.getString("UnitEditorDialog.critString"),
                                                     entity.getLocationName(loc),
                                                     ((Mek) entity).getSystemName(QuadVee.SYSTEM_CONVERSION_GEAR)) +
                                               "</b><br></html>"), gridBagConstraints);
                CheckCriticalSlotPanel actuatorCriticalSlotPanel = new CheckCriticalSlotPanel(1,
                      entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, loc));
                actuatorCriticalSlots[loc - Mek.LOC_RARM][Mek.ACTUATOR_FOOT - Mek.ACTUATOR_HIP +
                                                                1] = actuatorCriticalSlotPanel;
                gridBagConstraints.gridx = 1;
                panSystem.add(actuatorCriticalSlotPanel, gridBagConstraints);
            }
        }

    }

    private void setupTankSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        Tank tank = (Tank) entity;

        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.turretLock") + "</b><br></html>"),
              gridBagConstraints);
        int lock = 0;
        if (tank.isTurretLocked(0)) {
            lock = 1;
        }
        turretLockCriticalSlot = new CheckCriticalSlotPanel(1, lock);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(turretLockCriticalSlot, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engine") + "</b><br></html>"),
              gridBagConstraints);
        engineCriticalSlot = new CheckCriticalSlotPanel(1, tank.getEngineHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(engineCriticalSlot, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
              gridBagConstraints);
        sensorCriticalSlots = new CheckCriticalSlotPanel(Tank.CRIT_SENSOR_MAX, tank.getSensorHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(sensorCriticalSlots, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.motiveDamage") + "</b><br></html>"),
              gridBagConstraints);
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
        motiveCriticalSlot = new CheckCriticalSlotPanel(4, motiveHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(motiveCriticalSlot, gridBagConstraints);

        stabilizerCriticalSlots = new CheckCriticalSlotPanel[tank.locations()];
        for (int loc = 0; loc < tank.locations(); loc++) {
            if ((loc == Tank.LOC_BODY) || (loc == tank.getLocTurret()) || (loc == tank.getLocTurret2())) {
                continue;
            }
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            if (loc == (tank.locations() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            panSystem.add(new JLabel("<html><b>" +
                                           String.format(Messages.getString("UnitEditorDialog.locationStabilizer"),
                                                 entity.getLocationName(loc)) +
                                           "</b><br></html>"), gridBagConstraints);
            int hits = 0;
            if (tank.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCriticalSlotPanel stabCriticalSlotPanel = new CheckCriticalSlotPanel(1, hits);
            stabilizerCriticalSlots[loc] = stabCriticalSlotPanel;
            gridBagConstraints.gridx = 1;
            panSystem.add(stabCriticalSlotPanel, gridBagConstraints);
        }

    }

    private void setupProtoSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        ProtoMek proto = (ProtoMek) entity;

        protoCriticalSlots = new CheckCriticalSlotPanel[proto.locations()];
        gridBagConstraints.gridy = 0;

        for (int loc = 0; loc < proto.locations(); loc++) {
            if ((loc == ProtoMek.LOC_MAINGUN) || (loc == ProtoMek.LOC_NMISS)) {
                continue;
            }
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            if (loc == (proto.locations() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            panSystem.add(new JLabel("<html><b>" +
                                           String.format(Messages.getString("UnitEditorDialog.protoCritString"),
                                                 entity.getLocationName(loc)) +
                                           "</b><br></html>"), gridBagConstraints);
            int hits = 0;
            if ((loc == ProtoMek.LOC_LARM) || (loc == ProtoMek.LOC_RARM)) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARMCRIT, loc);
            }
            if (loc == ProtoMek.LOC_LEG) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEGCRIT, loc);
            }
            if (loc == ProtoMek.LOC_HEAD) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_HEADCRIT, loc);
            }
            if (loc == ProtoMek.LOC_TORSO) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, loc);
            }
            int numCriticalSlots = 2;
            if (loc == ProtoMek.LOC_LEG) {
                numCriticalSlots = 3;
            }
            CheckCriticalSlotPanel protoCriticalClots = new CheckCriticalSlotPanel(numCriticalSlots, hits);
            protoCriticalSlots[loc] = protoCriticalClots;
            gridBagConstraints.gridx = 1;
            panSystem.add(protoCriticalClots, gridBagConstraints);
        }

    }

    private void setupVtolSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        VTOL vtol = (VTOL) entity;

        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" +
                                       Messages.getString("UnitEditorDialog.flightStabilizer") +
                                       "</b><br></html>"), gridBagConstraints);
        int flightStabHit = 0;
        if (vtol.isStabiliserHit(VTOL.LOC_ROTOR)) {
            flightStabHit = 1;
        }
        flightStabilizerCriticalSlot = new CheckCriticalSlotPanel(1, flightStabHit);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(flightStabilizerCriticalSlot, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engine") + "</b><br></html>"),
              gridBagConstraints);
        engineCriticalSlot = new CheckCriticalSlotPanel(1, vtol.getEngineHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(engineCriticalSlot, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
              gridBagConstraints);
        sensorCriticalSlots = new CheckCriticalSlotPanel(Tank.CRIT_SENSOR_MAX, vtol.getSensorHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(sensorCriticalSlots, gridBagConstraints);

        stabilizerCriticalSlots = new CheckCriticalSlotPanel[vtol.locations()];
        for (int loc = 0; loc < vtol.locations(); loc++) {
            if ((loc == Tank.LOC_BODY) || (loc == VTOL.LOC_ROTOR)) {
                continue;
            }
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            if (loc == (vtol.locations() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            panSystem.add(new JLabel("<html><b>" +
                                           String.format(Messages.getString("UnitEditorDialog.locationStabilizer"),
                                                 entity.getLocationName(loc)) +
                                           "</b><br></html>"), gridBagConstraints);
            int hits = 0;
            if (vtol.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCriticalSlotPanel stabCriticalSlots = new CheckCriticalSlotPanel(1, hits);
            stabilizerCriticalSlots[loc] = stabCriticalSlots;
            gridBagConstraints.gridx = 1;
            panSystem.add(stabCriticalSlots, gridBagConstraints);
        }
    }

    private void setupAeroSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        Aero aero = (Aero) entity;

        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.avionics") + "</b><br></html>"),
              gridBagConstraints);
        avionicsCriticalSlot = new CheckCriticalSlotPanel(3, aero.getAvionicsHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(avionicsCriticalSlot, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;

        if (aero instanceof Jumpship) {
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.cic") + "</b><br></html>"),
                  gridBagConstraints);
            cicCriticalSlot = new CheckCriticalSlotPanel(3, aero.getCICHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(cicCriticalSlot, gridBagConstraints);
        } else {
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.fcs") + "</b><br></html>"),
                  gridBagConstraints);
            fcsCriticalSlot = new CheckCriticalSlotPanel(3, aero.getFCSHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(fcsCriticalSlot, gridBagConstraints);
        }

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
              gridBagConstraints);
        sensorCriticalSlots = new CheckCriticalSlotPanel(3, aero.getSensorHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(sensorCriticalSlots, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engine") + "</b><br></html>"),
              gridBagConstraints);
        engineCriticalSlot = new CheckCriticalSlotPanel(3, aero.getEngineHits());
        if ((aero instanceof Dropship) || (aero instanceof Jumpship)) {
            engineCriticalSlot = new CheckCriticalSlotPanel(6, aero.getEngineHits());
        }
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(engineCriticalSlot, gridBagConstraints);

        if (!(aero instanceof Jumpship)) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.landingGear") +
                                           "</b><br></html>"), gridBagConstraints);
            int gearHits = 0;
            if (aero.isGearHit()) {
                gearHits = 1;
            }
            gearCriticalSlot = new CheckCriticalSlotPanel(1, gearHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(gearCriticalSlot, gridBagConstraints);
        }

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.lifeSupport") + "</b><br></html>"),
              gridBagConstraints);
        int lifeHits = 0;
        if (!aero.hasLifeSupport()) {
            lifeHits = 1;
        }
        lifeSupportCriticalSlots = new CheckCriticalSlotPanel(1, lifeHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(lifeSupportCriticalSlots, gridBagConstraints);

        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.leftThruster") +
                                           "</b><br></html>"), gridBagConstraints);
            leftThrusterCriticalSlot = new CheckCriticalSlotPanel(4, aero.getLeftThrustHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(leftThrusterCriticalSlot, gridBagConstraints);

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.rightThruster") +
                                           "</b><br></html>"), gridBagConstraints);
            rightThrusterCriticalSlot = new CheckCriticalSlotPanel(4, aero.getRightThrustHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(rightThrusterCriticalSlot, gridBagConstraints);
        }

        if (aero instanceof Jumpship js) {
            // Grav Decks
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.gravDecks") +
                                           "</b><br></html>"), gridBagConstraints);
            gravDeckCriticalSlot = new CheckCriticalSlotPanel(js.getTotalGravDeck(), js.getTotalDamagedGravDeck());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(gravDeckCriticalSlot, gridBagConstraints);

            // Docking Collars
            JSpinner collarCriticalSlots;
            Vector<DockingCollar> collars = aero.getDockingCollars();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.dockingCollars") +
                                           "</b><br></html>"), gridBagConstraints);

            int damagedCollars = 0;
            for (DockingCollar nextDC : aero.getDockingCollars()) {
                if (nextDC.isDamaged()) {
                    damagedCollars++;
                }
            }
            collarCriticalSlots = new JSpinner(new SpinnerNumberModel(collars.size() - damagedCollars,
                  0,
                  collars.size(),
                  1.0));
            collarDamage = collarCriticalSlots;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(collarCriticalSlots, gridBagConstraints);

            // K-F Drive Integrity
            JSpinner kfDriveCriticalSlots;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.kfIntegrity") +
                                           "</b><br></html>"), gridBagConstraints);
            kfDriveCriticalSlots = new JSpinner(new SpinnerNumberModel(js.getKFIntegrity(),
                  0,
                  js.getOKFIntegrity(),
                  1.0));
            kfDamage = kfDriveCriticalSlots;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(kfDriveCriticalSlots, gridBagConstraints);

            // K-F Drive Components (Optional)
            // Drive Coil
            if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_EXPANDED_KF_DRIVE_DAMAGE)) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               Messages.getString("UnitEditorDialog.driveCoil") +
                                               "</b><br></html>"), gridBagConstraints);
                int driveCoilHits = 0;
                if (js.getKFDriveCoilHit()) {
                    driveCoilHits = 1;
                }
                driveCoilCriticalSlot = new CheckCriticalSlotPanel(1, driveCoilHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(driveCoilCriticalSlot, gridBagConstraints);

                // Charging System
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               Messages.getString("UnitEditorDialog.chargingSystem") +
                                               "</b><br></html>"), gridBagConstraints);
                int chargingSystemHits = 0;
                if (js.getKFChargingSystemHit()) {
                    chargingSystemHits = 1;
                }
                chargingSystemCriticalSlot = new CheckCriticalSlotPanel(1, chargingSystemHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(chargingSystemCriticalSlot, gridBagConstraints);

                // Field Initiator
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               Messages.getString("UnitEditorDialog.fieldInitiator") +
                                               "</b><br></html>"), gridBagConstraints);
                int fieldInitiatorHits = 0;
                if (js.getKFFieldInitiatorHit()) {
                    fieldInitiatorHits = 1;
                }
                fieldInitiatorCriticalSlot = new CheckCriticalSlotPanel(1, fieldInitiatorHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(fieldInitiatorCriticalSlot, gridBagConstraints);

                // Drive Controller
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               Messages.getString("UnitEditorDialog.driveController") +
                                               "</b><br></html>"), gridBagConstraints);
                int driveControllerHits = 0;
                if (js.getKFDriveControllerHit()) {
                    driveControllerHits = 1;
                }
                driveControllerCriticalSlot = new CheckCriticalSlotPanel(1, driveControllerHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(driveControllerCriticalSlot, gridBagConstraints);

                // Helium Tank
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               Messages.getString("UnitEditorDialog.heliumTank") +
                                               "</b><br></html>"), gridBagConstraints);
                int heliumTankHits = 0;
                if (js.getKFHeliumTankHit()) {
                    heliumTankHits = 1;
                }
                heliumTankCriticalSlot = new CheckCriticalSlotPanel(1, heliumTankHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(heliumTankCriticalSlot, gridBagConstraints);

                // LF Battery
                if (js.hasLF()) {
                    gridBagConstraints.gridx = 0;
                    gridBagConstraints.gridy++;
                    gridBagConstraints.weightx = 0.0;
                    panSystem.add(new JLabel("<html><b>" +
                                                   Messages.getString("UnitEditorDialog.lfBattery") +
                                                   "</b><br></html>"), gridBagConstraints);
                    int lfBatteryHits = 0;
                    if (js.getLFBatteryHit()) {
                        lfBatteryHits = 1;
                    }
                    lfBatteryCriticalSlot = new CheckCriticalSlotPanel(1, lfBatteryHits);
                    gridBagConstraints.gridx = 1;
                    gridBagConstraints.weightx = 1.0;
                    panSystem.add(lfBatteryCriticalSlot, gridBagConstraints);
                }
            }

            // Jump-Sail Integrity
            JSpinner sailCriticalSlots;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.sailIntegrity") +
                                           "</b><br></html>"), gridBagConstraints);
            sailCriticalSlots = new JSpinner(new SpinnerNumberModel(js.getSailIntegrity(),
                  0,
                  js.getOSailIntegrity(),
                  1.0));
            sailDamage = sailCriticalSlots;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(sailCriticalSlots, gridBagConstraints);
        }

        if (aero instanceof Dropship) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" +
                                           Messages.getString("UnitEditorDialog.dropshipCollar") +
                                           "</b><br></html>"), gridBagConstraints);
            int collarHits = 0;
            if (((Dropship) aero).isDockCollarDamaged()) {
                collarHits = 1;
            }
            dockCollarCriticalSlot = new CheckCriticalSlotPanel(1, collarHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(dockCollarCriticalSlot, gridBagConstraints);

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.kfBoom") + "</b><br></html>"),
                  gridBagConstraints);
            int kfBoomHits = 0;
            if (((Dropship) aero).isKFBoomDamaged()) {
                kfBoomHits = 1;
            }
            kfBoomCriticalSlot = new CheckCriticalSlotPanel(1, kfBoomHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(kfBoomCriticalSlot, gridBagConstraints);
        }

        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            int b = 0;
            JSpinner bayCriticalSlots;
            Vector<Bay> bays = aero.getTransportBays();
            bayDamage = new JSpinner[bays.size()];
            bayDoorCriticalSlot = new CheckCriticalSlotPanel[bays.size()];
            for (Bay nextbay : bays) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               String.format(Messages.getString("UnitEditorDialog.bayCrit"),
                                                     nextbay.getType(),
                                                     nextbay.getBayNumber()) +
                                               "</b><br></html>"), gridBagConstraints);

                bayCriticalSlots = new JSpinner(new SpinnerNumberModel(nextbay.getCapacity() - nextbay.getBayDamage(),
                      0,
                      nextbay.getCapacity(),
                      nextbay.isCargo() ? 0.5 : 1.0));
                bayDamage[b] = bayCriticalSlots;
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(bayCriticalSlots, gridBagConstraints);

                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                                               String.format(Messages.getString("UnitEditorDialog.bayDoorCrit"),
                                                     nextbay.getBayNumber()) +
                                               "</b><br></html>"), gridBagConstraints);

                CheckCriticalSlotPanel doorCriticalSlotPanel = new CheckCriticalSlotPanel(nextbay.getDoors(),
                      (nextbay.getDoors() - nextbay.getCurrentDoors()));
                bayDoorCriticalSlot[b] = doorCriticalSlotPanel;
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(doorCriticalSlotPanel, gridBagConstraints);
                b++;
            }
        }
    }

    /**
     * Applies the given number of total critical slots to a Super-Cooled Myomer (which is spread over 6 locations).
     */
    public void damageSCM(Entity entity, int eqNum, int hits) {
        int numHits = 0;
        Mounted<?> m = entity.getEquipment(eqNum);
        for (int loc = 0; loc < entity.locations(); loc++) {
            for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
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

    private void btnOkayActionPerformed(java.awt.event.ActionEvent evt) {
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
            CheckCriticalSlotPanel criticalSlotPanel = equipCriticalSlots.get(eqNum);
            if (null != criticalSlotPanel) {
                int hits = criticalSlotPanel.getHits();
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
        if (entity instanceof Infantry) {
            ((Infantry) entity).damageOrRestoreFieldWeapons();
            entity.applyDamage();
        }

        // now systems
        if (entity instanceof Mek) {
            if (null != centerEngineCriticalSlots) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_CT,
                      centerEngineCriticalSlots.getHits());
            }
            if (null != leftEngineCriticalSlot) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_LT,
                      leftEngineCriticalSlot.getHits());
            }
            if (null != rightEngineCriticalSlots) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_RT,
                      rightEngineCriticalSlots.getHits());
            }
            if (null != gyroCriticalSlots) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, gyroCriticalSlots.getHits());
            }
            if (null != sensorCriticalSlots) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, sensorCriticalSlots.getHits());
            }
            if (null != lifeSupportCriticalSlots) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_LIFE_SUPPORT,
                      lifeSupportCriticalSlots.getHits());
            }
            if (null != cockpitCriticalSlots) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, cockpitCriticalSlots.getHits());
            }
            if (null != lamAvionicsCriticalSlots && !lamAvionicsCriticalSlots.isEmpty()) {
                for (int loc : lamAvionicsCriticalSlots.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          LandAirMek.LAM_AVIONICS,
                          loc,
                          lamAvionicsCriticalSlots.get(loc).getHits());
                }
            }
            if (null != lamLandingGearCriticalSlots && !lamLandingGearCriticalSlots.isEmpty()) {
                for (int loc : lamLandingGearCriticalSlots.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          LandAirMek.LAM_LANDING_GEAR,
                          loc,
                          lamLandingGearCriticalSlots.get(loc).getHits());
                }
            }

            for (int i = 0; i < actuatorCriticalSlots.length; i++) {
                for (int j = 0; j < actuatorCriticalSlots[i].length; j++) {
                    CheckCriticalSlotPanel actuatorCriticalSlotPanel = actuatorCriticalSlots[i][j];
                    if (null == actuatorCriticalSlotPanel) {
                        continue;
                    }
                    int loc = i + Mek.LOC_RARM;
                    int actuator = j + Mek.ACTUATOR_SHOULDER;
                    if ((loc >= Mek.LOC_RLEG) || (entity instanceof QuadMek)) {
                        actuator = j + Mek.ACTUATOR_HIP;
                    }
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator, loc, actuatorCriticalSlotPanel.getHits());
                }

                if (entity instanceof QuadVee) {
                    for (int j = 0; j < actuatorCriticalSlots.length; j++) {
                        CheckCriticalSlotPanel actuatorCriticalSlotPanel = actuatorCriticalSlots[i][4];
                        if (null == actuatorCriticalSlotPanel) {
                            continue;
                        }
                        int loc = i + Mek.LOC_RARM;
                        int actuator = QuadVee.SYSTEM_CONVERSION_GEAR;
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                              actuator,
                              loc,
                              actuatorCriticalSlotPanel.getHits());
                    }
                }
            }
        } else if (entity instanceof ProtoMek) {
            for (int loc = 0; loc < entity.locations(); loc++) {
                if (null == protoCriticalSlots[loc]) {
                    continue;
                }
                if ((loc == ProtoMek.LOC_LARM) || (loc == ProtoMek.LOC_RARM)) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_ARMCRIT,
                          loc,
                          protoCriticalSlots[loc].getHits());
                }
                if (loc == ProtoMek.LOC_LEG) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_LEGCRIT,
                          loc,
                          protoCriticalSlots[loc].getHits());
                }
                if (loc == ProtoMek.LOC_HEAD) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_HEADCRIT,
                          loc,
                          protoCriticalSlots[loc].getHits());
                }
                if (loc == ProtoMek.LOC_TORSO) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_TORSOCRIT,
                          loc,
                          protoCriticalSlots[loc].getHits());
                }
            }
        } else if (entity instanceof Tank tank) {
            if (null != engineCriticalSlot) {
                if (engineCriticalSlot.getHits() > 0) {
                    tank.engineHit();
                } else {
                    tank.engineFix();
                }
            }
            if (null != turretLockCriticalSlot) {
                if (turretLockCriticalSlot.getHits() > 0) {
                    tank.lockTurret(0);
                } else {
                    tank.unlockTurret();
                }
            }
            if (null != sensorCriticalSlots) {
                tank.setSensorHits(sensorCriticalSlots.getHits());
            }
            if (null != motiveCriticalSlot) {
                tank.resetMovementDamage();
                tank.addMovementDamage(motiveCriticalSlot.getHits());

                // Apply movement damage immediately in case we've decided to immobilize the
                // tank
                tank.applyMovementDamage();
            }
            if ((tank instanceof VTOL) && (null != flightStabilizerCriticalSlot)) {
                if (flightStabilizerCriticalSlot.getHits() > 0) {
                    tank.setStabiliserHit(VTOL.LOC_ROTOR);
                } else {
                    tank.clearStabiliserHit(VTOL.LOC_ROTOR);
                }
            }
            for (int loc = 0; loc < tank.locations(); loc++) {
                CheckCriticalSlotPanel stabCriticalSlots = stabilizerCriticalSlots[loc];
                if (null == stabCriticalSlots) {
                    continue;
                }
                if (stabCriticalSlots.getHits() > 0) {
                    tank.setStabiliserHit(loc);
                } else {
                    tank.clearStabiliserHit(loc);
                }
            }
        } else if (entity instanceof Aero aero) {
            if (null != avionicsCriticalSlot) {
                aero.setAvionicsHits(avionicsCriticalSlot.getHits());
            }
            if (null != fcsCriticalSlot) {
                aero.setFCSHits(fcsCriticalSlot.getHits());
            }
            if (null != cicCriticalSlot) {
                aero.setCICHits(cicCriticalSlot.getHits());
            }
            if (null != engineCriticalSlot) {
                aero.setEngineHits(engineCriticalSlot.getHits());
            }
            if (null != sensorCriticalSlots) {
                aero.setSensorHits(sensorCriticalSlots.getHits());
            }
            if (null != gearCriticalSlot) {
                aero.setGearHit(gearCriticalSlot.getHits() > 0);
            }
            if (null != lifeSupportCriticalSlots) {
                aero.setLifeSupport(lifeSupportCriticalSlots.getHits() == 0);
            }
            if (null != leftThrusterCriticalSlot) {
                aero.setLeftThrustHits(leftThrusterCriticalSlot.getHits());
            }
            if (null != rightThrusterCriticalSlot) {
                aero.setRightThrustHits(rightThrusterCriticalSlot.getHits());
            }
            if ((null != dockCollarCriticalSlot) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageDockCollar(dockCollarCriticalSlot.getHits() > 0);
            }
            if ((null != kfBoomCriticalSlot) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageKFBoom(kfBoomCriticalSlot.getHits() > 0);
            }
            // cargo bays and bay doors
            if ((aero instanceof Dropship) || (aero instanceof Jumpship)) {
                int b = 0;
                for (Bay bay : aero.getTransportBays()) {
                    JSpinner bayCriticalSlots = bayDamage[b];
                    if (null == bayCriticalSlots) {
                        continue;
                    }
                    bay.setBayDamage(bay.getCapacity() - (Double) bayCriticalSlots.getModel().getValue());
                    CheckCriticalSlotPanel doorCriticalSlots = bayDoorCriticalSlot[b];
                    if (null == doorCriticalSlots) {
                        continue;
                    }
                    if ((bay.getCurrentDoors() > 0) && (doorCriticalSlots.getHits() > 0)) {
                        bay.setCurrentDoors(bay.getDoors() - doorCriticalSlots.getHits());

                    } else if (doorCriticalSlots.getHits() == 0) {
                        bay.setCurrentDoors(bay.getDoors());
                    }
                    // for ASF and SC bays, we have to update recovery slots as doors are changed
                    if (bay instanceof ASFBay a) {
                        a.initializeRecoverySlots();
                    }
                    if (bay instanceof SmallCraftBay s) {
                        s.initializeRecoverySlots();
                    }
                    b++;
                }
            }
            // Jumpship Docking Collars, KF Drive, Sail and Grav Decks
            if (aero instanceof Jumpship js) {
                JSpinner collarCriticalSlot = collarDamage;
                CheckCriticalSlotPanel deckCriticalSlot = gravDeckCriticalSlot;
                double damagedCollars = 0.0;
                int damagedDecks = 0;
                if (null != collarCriticalSlot) {
                    damagedCollars = (aero.getDockingCollars().size() -
                                            (double) collarCriticalSlot.getModel().getValue());
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
                if (null != deckCriticalSlot) {
                    damagedDecks = deckCriticalSlot.getHits();
                }
                // reset all grav decks to undamaged
                for (int i = 0; i < js.getTotalGravDeck(); i++) {
                    js.setGravDeckDamageFlag(i, 0);
                }
                if (damagedDecks > 0) {
                    // loop through the grav decks from #1 and damage them
                    for (int i = 0; i < damagedDecks; i++) {
                        js.setGravDeckDamageFlag(i, 1);
                    }
                }
                // KF Drive and Sail
                if (null != kfDamage) {
                    double kfIntegrity = (double) kfDamage.getModel().getValue();
                    js.setKFIntegrity((int) kfIntegrity);
                }
                if (null != chargingSystemCriticalSlot) {
                    js.setKFChargingSystemHit(chargingSystemCriticalSlot.getHits() > 0);
                }
                if (null != driveCoilCriticalSlot) {
                    js.setKFDriveCoilHit(driveCoilCriticalSlot.getHits() > 0);
                }
                if (null != driveControllerCriticalSlot) {
                    js.setKFDriveControllerHit(driveControllerCriticalSlot.getHits() > 0);
                }
                if (null != fieldInitiatorCriticalSlot) {
                    js.setKFFieldInitiatorHit(fieldInitiatorCriticalSlot.getHits() > 0);
                }
                if (null != heliumTankCriticalSlot) {
                    js.setKFHeliumTankHit(heliumTankCriticalSlot.getHits() > 0);
                }
                if (null != lfBatteryCriticalSlot) {
                    js.setLFBatteryHit(lfBatteryCriticalSlot.getHits() > 0);
                }
                if (null != sailDamage) {
                    double sailIntegrity = (double) sailDamage.getModel().getValue();
                    js.setSailIntegrity((int) sailIntegrity);
                }
            }
        }

    }

    private static class CheckCriticalSlotPanel extends JPanel {
        @Serial
        private static final long serialVersionUID = 8662728291188274362L;

        private final ArrayList<JCheckBox> checks = new ArrayList<>();

        public CheckCriticalSlotPanel(int criticalSlots, int current) {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            for (int i = 0; i < criticalSlots; i++) {
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

        private void checkBoxes(ActionEvent evt) {
            int hits = Integer.parseInt(evt.getActionCommand());
            boolean selected = checks.get(hits).isSelected();
            if (selected) {
                // check all those up to this one
                for (int i = 0; i < hits; i++) {
                    checks.get(i).setSelected(true);
                }
            } else {
                // deselect any above this one
                for (int i = hits; i < checks.size(); i++) {
                    checks.get(i).setSelected(false);
                }
            }

        }
    }
}
