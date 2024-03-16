/*
 * MechEditor.java - Copyright (C) 2013 Jay Lawson
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.OptionsConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * This dialog will allow the user to edit the damage and status characteristics of a unit.
 * This is designed for use in both MegaMek and MHQ so don't go messing things up for MHQ by
 * changing a bunch of stuff
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitEditorDialog extends JDialog {
    private static final long serialVersionUID = 8144354264100884817L;

    private Entity entity;

    JPanel panArmor;
    JPanel panSystem;
    JPanel panEquip;

    JSpinner[] spnInternal;
    JSpinner[] spnArmor;
    JSpinner[] spnRear;

    HashMap<Integer, CheckCritPanel> equipCrits;

    /* system crits */
    CheckCritPanel engineCrit;
    CheckCritPanel leftEngineCrit;
    CheckCritPanel rightEngineCrit;
    CheckCritPanel centerEngineCrit;
    CheckCritPanel gyroCrit;
    CheckCritPanel sensorCrit;
    CheckCritPanel lifeSupportCrit;
    CheckCritPanel cockpitCrit;
    Map<Integer,CheckCritPanel> lamAvionicsCrit;
    Map<Integer,CheckCritPanel> lamLandingGearCrit;
    CheckCritPanel[][] actuatorCrits;
    CheckCritPanel turretlockCrit;
    CheckCritPanel motiveCrit;
    CheckCritPanel[] stabilizerCrits;
    CheckCritPanel flightStabilizerCrit;
    CheckCritPanel avionicsCrit;
    CheckCritPanel fcsCrit;
    CheckCritPanel cicCrit;
    CheckCritPanel gearCrit;
    CheckCritPanel leftThrusterCrit;
    CheckCritPanel rightThrusterCrit;
    CheckCritPanel kfboomCrit;
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

        // TODO: protomeks
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

        adaptToGUIScale();
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
        panArmor.setBorder(BorderFactory
                .createTitledBorder(Messages.getString("UnitEditorDialog.internalsAndArmor")));

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
            spnArmor[i] = new JSpinner(new SpinnerNumberModel(armor, 0,
                    entity.getOArmor(i), 1));
            spnInternal[i] = new JSpinner(new SpinnerNumberModel(internal, 0,
                    entity.getOInternal(i), 1));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i + 1;
            gridBagConstraints.insets = new Insets(1, 10, 1, 1);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panArmor.add(new JLabel(entity.getLocationName(i)),
                    gridBagConstraints);
            gridBagConstraints.gridx = 1;
            panArmor.add(spnInternal[i], gridBagConstraints);
            gridBagConstraints.gridx = 2;
            panArmor.add(spnArmor[i], gridBagConstraints);
            if (entity.hasRearArmor(i)) {
                int rear = Math.max(entity.getArmor(i, true), 0);
                spnRear[i] = new JSpinner(new SpinnerNumberModel(rear, 0,
                        entity.getOArmor(i, true), 1));
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
        spnInternal[0] = new JSpinner(new SpinnerNumberModel(si, 0,
                aero.get0SI(), 1));
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
            spnArmor[i] = new JSpinner(new SpinnerNumberModel(armor, 0,
                    entity.getOArmor(i), 1));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i + 2;
            gridBagConstraints.insets = new Insets(1, 10, 1, 1);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panArmor.add(new JLabel(entity.getLocationName(i)),
                    gridBagConstraints);
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
        spnInternal[0] = new JSpinner(new SpinnerNumberModel(men, 0,
                infantry.getSquadCount() * infantry.getSquadSize(), 1));
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
        equipCrits = new HashMap<>();
        panEquip = new JPanel();
        panEquip.setLayout(new GridBagLayout());
        panEquip.setBorder(BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.equipment")));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        for (Mounted m : entity.getEquipment()) {
            if ((m.getLocation() == Entity.LOC_NONE)
                    || !m.getType().isHittable() || m.isWeaponGroup()) {
                continue;
            }
            int nCrits = m.getCriticals();
            int eqNum = entity.getEquipmentNum(m);
            int hits = entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT,
                    eqNum, m.getLocation());
            if (m.isSplit()) {
                hits += entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT,
                        eqNum, m.getSecondLocation());
            }
            if (m.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT,
                        eqNum, Mech.LOC_LT);
                hits += entity.getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT,
                        eqNum, Mech.LOC_RT);
            }

            if (!(entity instanceof Mech)) {
                nCrits = 1;
                if (hits > 1) {
                    hits = 1;
                }
            }
            CheckCritPanel crit = new CheckCritPanel(nCrits, hits);
            equipCrits.put(eqNum, crit);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            panEquip.add(new JLabel("<html><b>" + m.getName() + "</b><br>"
                    + entity.getLocationName(m.getLocation()) + "</html>"),
                    gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panEquip.add(crit, gridBagConstraints);
            gridBagConstraints.gridy++;
        }
    }

    private void initSystemPanel() {

        // systems are the hard part, because these are all unit specific
        // lets start with a mech
        panSystem = new JPanel(new GridBagLayout());
        panSystem.setBorder(BorderFactory.createTitledBorder(Messages.getString("UnitEditorDialog.system")));

        if (entity instanceof Mech) {
            setupMechSystemPanel();
        } else if (entity instanceof VTOL) {
            setupVtolSystemPanel();
        } else if (entity instanceof Tank) {
            setupTankSystemPanel();
        } else if (entity instanceof Aero) {
            setupAeroSystemPanel();
        } else if (entity instanceof Protomech) {
            setupProtoSystemPanel();
        }
    }

    private void setupMechSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        /*
         * For the moment, I am going to cap out the number of hits at what the
         * record sheets show (i.e. 3 for engines). If we want to switch this to
         * the actual number then we can, see
         * enginePart.updateConditionFromEntity in MekHQ for an example of how
         * to retrieve all of the available system crits
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
            if (i == Mech.LOC_CT) {
                centerEngineHits = entity.getDamagedCriticals(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
                centerEngineCrits = entity.getNumberOfCriticals(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
            }
            if (i == Mech.LOC_LT) {
                leftEngineHits = entity.getDamagedCriticals(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
                leftEngineCrits = entity.getNumberOfCriticals(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
            }
            if (i == Mech.LOC_RT) {
                rightEngineHits = entity.getDamagedCriticals(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
                rightEngineCrits = entity.getNumberOfCriticals(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
            }
            gyroHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_GYRO, i);
            gyroCrits += entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_GYRO, i);
            cockpitHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_COCKPIT, i);
            cockpitCrits += entity.getNumberOfCriticals(
                    CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i);
            sensorHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_SENSORS, i);
            sensorCrits += entity.getNumberOfCriticals(
                    CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i);
            lifeSupportHits += entity.getDamagedCriticals(
                    CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i);
            lifeSupportCrits += entity.getNumberOfCriticals(
                    CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i);
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
        centerEngineCrit = new CheckCritPanel(centerEngineCrits,
                centerEngineHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(centerEngineCrit, gridBagConstraints);

        if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_RT) > 0) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engineLT") + "</b><br></html>"),
                    gridBagConstraints);
            leftEngineCrit = new CheckCritPanel(leftEngineCrits, leftEngineHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(leftEngineCrit, gridBagConstraints);
    
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engineRT") + "</b><br></html>"),
                    gridBagConstraints);
            rightEngineCrit = new CheckCritPanel(rightEngineCrits, rightEngineHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(rightEngineCrit, gridBagConstraints);
        }

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.gyro") + "</b><br></html>"),
                gridBagConstraints);
        gyroCrit = new CheckCritPanel(gyroCrits, gyroHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(gyroCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
                gridBagConstraints);
        sensorCrit = new CheckCritPanel(sensorCrits, sensorHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(sensorCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.lifeSupport")
                + "</b><br></html>"), gridBagConstraints);
        lifeSupportCrit = new CheckCritPanel(lifeSupportCrits, lifeSupportHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(lifeSupportCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.cockpit") + "</b><br></html>"),
                gridBagConstraints);
        cockpitCrit = new CheckCritPanel(cockpitCrits, cockpitHits);
        gridBagConstraints.gridx = 1;
        panSystem.add(cockpitCrit, gridBagConstraints);
        
        if (entity instanceof LandAirMech) {
            lamAvionicsCrit = new TreeMap<>();
            lamLandingGearCrit = new TreeMap<>();
            for (int loc = 0; loc < entity.locations(); loc++) {
                int crits = entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, loc);
                if (crits > 0) {
                    int hits = entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, loc);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    lamAvionicsCrit.put(loc, critPanel);
                }
                crits = entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR, loc);
                if (crits > 0) {
                    int hits = entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR, loc);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    lamLandingGearCrit.put(loc, critPanel);
                }
            }
            BiConsumer<Map.Entry<Integer,CheckCritPanel>, String> addToPanel = (entry, critName) -> {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" + 
                        String.format(Messages.getString("UnitEditorDialog.critStringLocation"), critName, entity.getLocationAbbr(entry.getKey())) 
                        + "</b><br></html>"),
                        gridBagConstraints);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(entry.getValue(), gridBagConstraints);
            };
            lamAvionicsCrit.entrySet().forEach(e -> addToPanel.accept(e, Messages.getString("UnitEditorDialog.avionics")));
            lamLandingGearCrit.entrySet().forEach(e -> addToPanel.accept(e, Messages.getString("UnitEditorDialog.landingGear")));
        }

        final boolean tripod = entity.hasETypeFlag(Entity.ETYPE_TRIPOD_MECH);
        if (tripod) {
            actuatorCrits = new CheckCritPanel[5][4];
        } else if (entity instanceof QuadVee) {
            actuatorCrits = new CheckCritPanel[4][5];
        } else {
            actuatorCrits = new CheckCritPanel[4][4];
        }

        for (int loc = Mech.LOC_RARM; loc <= (tripod ? Mech.LOC_CLEG : Mech.LOC_LLEG); loc++) {
            int start = Mech.ACTUATOR_SHOULDER;
            int end = Mech.ACTUATOR_HAND;
            if ((loc >= Mech.LOC_RLEG) || (entity instanceof QuadMech)) {
                start = Mech.ACTUATOR_HIP;
                end = Mech.ACTUATOR_FOOT;
            }

            for (int i = start; i <= end; i++) {
                if (!entity.hasSystem(i, loc)) {
                    continue;
                }
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                gridBagConstraints.weighty = 0.0;
                if ((loc == (tripod ? Mech.LOC_CLEG : Mech.LOC_LLEG)) && (i == end)
                        && !(entity instanceof QuadVee)) {
                    gridBagConstraints.weighty = 1.0;
                }
                panSystem.add(
                        new JLabel("<html><b>" + 
                                String.format(Messages.getString("UnitEditorDialog.critString"), 
                                        entity.getLocationName(loc), 
                                        ((Mech) entity).getSystemName(i))
                                + "</b><br></html>"), gridBagConstraints);
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                        entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, i,
                                loc));
                actuatorCrits[loc - Mech.LOC_RARM][i - start] = actuatorCrit;
                gridBagConstraints.gridx = 1;
                panSystem.add(actuatorCrit, gridBagConstraints);
            }

            if (entity instanceof QuadVee) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                gridBagConstraints.weighty = 0.0;
                if (loc == Mech.LOC_LLEG) {
                    gridBagConstraints.weighty = 1.0;
                }
                panSystem.add(
                        new JLabel("<html><b>" + 
                                String.format(Messages.getString("UnitEditorDialog.critString"), 
                                        entity.getLocationName(loc), 
                                        ((Mech) entity).getSystemName(QuadVee.SYSTEM_CONVERSION_GEAR))
                                + "</b><br></html>"), gridBagConstraints);
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                        entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR,
                                loc));
                actuatorCrits[loc - Mech.LOC_RARM][Mech.ACTUATOR_FOOT - Mech.ACTUATOR_HIP + 1] = actuatorCrit;
                gridBagConstraints.gridx = 1;
                panSystem.add(actuatorCrit, gridBagConstraints);
            }
        }

    }

    private void setupTankSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        Tank tank = (Tank) entity;

        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.turretLock")
                + "</b><br></html>"), gridBagConstraints);
        int lock = 0;
        if (tank.isTurretLocked(0)) {
            lock = 1;
        }
        turretlockCrit = new CheckCritPanel(1, lock);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(turretlockCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engine") + "</b><br></html>"),
                gridBagConstraints);
        engineCrit = new CheckCritPanel(1, tank.getEngineHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(engineCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
                gridBagConstraints);
        sensorCrit = new CheckCritPanel(Tank.CRIT_SENSOR_MAX, tank.getSensorHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(sensorCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.motiveDamage")
                + "</b><br></html>"), gridBagConstraints);
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
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(motiveCrit, gridBagConstraints);

        stabilizerCrits = new CheckCritPanel[tank.locations()];
        for (int loc = 0; loc < tank.locations(); loc++) {
            if ((loc == Tank.LOC_BODY) || (loc == tank.getLocTurret())
                    || (loc == tank.getLocTurret2())) {
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
                    String.format(Messages.getString("UnitEditorDialog.locationStabilizer"), entity.getLocationName(loc)) 
                    + "</b><br></html>"), gridBagConstraints);
            int hits = 0;
            if (tank.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            stabilizerCrits[loc] = stabCrit;
            gridBagConstraints.gridx = 1;
            panSystem.add(stabCrit, gridBagConstraints);
        }

    }

    private void setupProtoSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        Protomech proto = (Protomech) entity;

        protoCrits = new CheckCritPanel[proto.locations()];
        gridBagConstraints.gridy = 0;

        for (int loc = 0; loc < proto.locations(); loc++) {
            if ((loc == Protomech.LOC_MAINGUN) || (loc == Protomech.LOC_NMISS)) {
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
                    String.format(Messages.getString("UnitEditorDialog.protoCritString"), entity.getLocationName(loc))  
                    + "</b><br></html>"), gridBagConstraints);
            int hits = 0;
            if ((loc == Protomech.LOC_LARM) || (loc == Protomech.LOC_RARM)) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        Protomech.SYSTEM_ARMCRIT, loc);
            }
            if (loc == Protomech.LOC_LEG) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        Protomech.SYSTEM_LEGCRIT, loc);
            }
            if (loc == Protomech.LOC_HEAD) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        Protomech.SYSTEM_HEADCRIT, loc);
            }
            if (loc == Protomech.LOC_TORSO) {
                hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
                        Protomech.SYSTEM_TORSOCRIT, loc);
            }
            int nCrits = 2;
            if (loc == Protomech.LOC_LEG) {
                nCrits = 3;
            }
            CheckCritPanel protoCrit = new CheckCritPanel(nCrits, hits);
            protoCrits[loc] = protoCrit;
            gridBagConstraints.gridx = 1;
            panSystem.add(protoCrit, gridBagConstraints);
        }

    }

    private void setupVtolSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        VTOL vtol = (VTOL) entity;

        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.flightStabilizer")
                + "</b><br></html>"), gridBagConstraints);
        int flightStabHit = 0;
        if (vtol.isStabiliserHit(VTOL.LOC_ROTOR)) {
            flightStabHit = 1;
        }
        flightStabilizerCrit = new CheckCritPanel(1, flightStabHit);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(flightStabilizerCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engine") + "</b><br></html>"),
                gridBagConstraints);
        engineCrit = new CheckCritPanel(1, vtol.getEngineHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(engineCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
                gridBagConstraints);
        sensorCrit = new CheckCritPanel(Tank.CRIT_SENSOR_MAX, vtol.getSensorHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(sensorCrit, gridBagConstraints);

        stabilizerCrits = new CheckCritPanel[vtol.locations()];
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
                    String.format(Messages.getString("UnitEditorDialog.locationStabilizer"), entity.getLocationName(loc))
                    + "</b><br></html>"), gridBagConstraints);
            int hits = 0;
            if (vtol.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            stabilizerCrits[loc] = stabCrit;
            gridBagConstraints.gridx = 1;
            panSystem.add(stabCrit, gridBagConstraints);
        }
    }

    private void setupAeroSystemPanel() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        Aero aero = (Aero) entity;

        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.avionics") + "</b><br></html>"),
                gridBagConstraints);
        avionicsCrit = new CheckCritPanel(3, aero.getAvionicsHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(avionicsCrit, gridBagConstraints);

        if (aero instanceof Jumpship) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.cic") + "</b><br></html>"),
                    gridBagConstraints);
            cicCrit = new CheckCritPanel(3, aero.getCICHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(cicCrit, gridBagConstraints);
        } else {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.fcs") + "</b><br></html>"),
                    gridBagConstraints);
            fcsCrit = new CheckCritPanel(3, aero.getFCSHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(fcsCrit, gridBagConstraints);
        }

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sensor") + "</b><br></html>"),
                gridBagConstraints);
        sensorCrit = new CheckCritPanel(3, aero.getSensorHits());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(sensorCrit, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.engine") + "</b><br></html>"),
                gridBagConstraints);
        engineCrit = new CheckCritPanel(3, aero.getEngineHits());
        if ((aero instanceof Dropship) || (aero instanceof Jumpship)) {
            engineCrit = new CheckCritPanel(6, aero.getEngineHits());
        }
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(engineCrit, gridBagConstraints);
        
    if (!(aero instanceof Jumpship)) {
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.landingGear")
                + "</b><br></html>"), gridBagConstraints);
        int gearHits = 0;
        if (aero.isGearHit()) {
            gearHits = 1;
        }
        gearCrit = new CheckCritPanel(1, gearHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(gearCrit, gridBagConstraints);
    }
    
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.weightx = 0.0;
        panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.lifeSupport")
                + "</b><br></html>"), gridBagConstraints);
        int lifeHits = 0;
        if (!aero.hasLifeSupport()) {
            lifeHits = 1;
        }
        lifeSupportCrit = new CheckCritPanel(1, lifeHits);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panSystem.add(lifeSupportCrit, gridBagConstraints);

        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.leftThruster")
                    + "</b><br></html>"), gridBagConstraints);
            leftThrusterCrit = new CheckCritPanel(4, aero.getLeftThrustHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(leftThrusterCrit, gridBagConstraints);

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.rightThruster")
                    + "</b><br></html>"), gridBagConstraints);
            rightThrusterCrit = new CheckCritPanel(4, aero.getRightThrustHits());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(rightThrusterCrit, gridBagConstraints);
        }
        
        if (aero instanceof Jumpship) {
            Jumpship js = (Jumpship) aero;
            //Grav Decks
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.gravDecks")
                    + "</b><br></html>"), gridBagConstraints);
            gravDeckCrit = new CheckCritPanel(js.getTotalGravDeck(), js.getTotalDamagedGravDeck());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(gravDeckCrit, gridBagConstraints);
            
            //Docking Collars
            JSpinner collarCrit;
            Vector<DockingCollar> collars = aero.getDockingCollars();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.dockingCollars")
                    + "</b><br></html>"), gridBagConstraints);
            
            int damagedCollars = 0;
            for (DockingCollar nextDC : aero.getDockingCollars()) {
                if (nextDC.isDamaged()) {
                    damagedCollars ++;
                }
            }
            collarCrit = new JSpinner(new SpinnerNumberModel(collars.size() - damagedCollars, 0, collars.size(), 1.0));
            collarDamage = collarCrit;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(collarCrit, gridBagConstraints);
            
            //K-F Drive Integrity
            JSpinner kfDriveCrit;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.kfIntegrity")
                    + "</b><br></html>"), gridBagConstraints);
            kfDriveCrit = new JSpinner(new SpinnerNumberModel(js.getKFIntegrity(), 0, js.getOKFIntegrity(), 1.0));
            kfDamage = kfDriveCrit;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(kfDriveCrit, gridBagConstraints);
            
            //K-F Drive Components (Optional)
            //Drive Coil
            if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_EXPANDED_KF_DRIVE_DAMAGE)) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.driveCoil")
                        + "</b><br></html>"), gridBagConstraints);
                int driveCoilHits = 0;
                if (js.getKFDriveCoilHit()) {
                    driveCoilHits = 1;
                }
                driveCoilCrit = new CheckCritPanel(1, driveCoilHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(driveCoilCrit, gridBagConstraints);
                
                //Charging System
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.chargingSystem")
                        + "</b><br></html>"), gridBagConstraints);
                int chargingSystemHits = 0;
                if (js.getKFChargingSystemHit()) {
                    chargingSystemHits = 1;
                }
                chargingSystemCrit = new CheckCritPanel(1, chargingSystemHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(chargingSystemCrit, gridBagConstraints);
                
                //Field Initiator
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.fieldInitiator")
                        + "</b><br></html>"), gridBagConstraints);
                int fieldInitiatorHits = 0;
                if (js.getKFFieldInitiatorHit()) {
                    fieldInitiatorHits = 1;
                }
                fieldInitiatorCrit = new CheckCritPanel(1, fieldInitiatorHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(fieldInitiatorCrit, gridBagConstraints);
                
                //Drive Controller
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.driveController")
                        + "</b><br></html>"), gridBagConstraints);
                int driveControllerHits = 0;
                if (js.getKFDriveControllerHit()) {
                    driveControllerHits = 1;
                }
                driveControllerCrit = new CheckCritPanel(1, driveControllerHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(driveControllerCrit, gridBagConstraints);
                
                //Helium Tank
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.heliumTank")
                        + "</b><br></html>"), gridBagConstraints);
                int heliumTankHits = 0;
                if (js.getKFHeliumTankHit()) {
                    heliumTankHits = 1;
                }
                heliumTankCrit = new CheckCritPanel(1, heliumTankHits);
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(heliumTankCrit, gridBagConstraints);
                
                //LF Battery
                if (js.hasLF()) {
                    gridBagConstraints.gridx = 0;
                    gridBagConstraints.gridy++;
                    gridBagConstraints.weightx = 0.0;
                    panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.lfBattery")
                            + "</b><br></html>"), gridBagConstraints);
                    int lfBatteryHits = 0;
                    if (js.getLFBatteryHit()) {
                        lfBatteryHits = 1;
                    }
                    lfBatteryCrit = new CheckCritPanel(1, lfBatteryHits);
                    gridBagConstraints.gridx = 1;
                    gridBagConstraints.weightx = 1.0;
                    panSystem.add(lfBatteryCrit, gridBagConstraints);
                }
            }
            
            //Jump Sail Integrity
            JSpinner sailCrit;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.sailIntegrity")
                    + "</b><br></html>"), gridBagConstraints);
            sailCrit = new JSpinner(new SpinnerNumberModel(js.getSailIntegrity(), 0, js.getOSailIntegrity(), 1.0));
            sailDamage = sailCrit;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(sailCrit, gridBagConstraints);
        }

        if (aero instanceof Dropship) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.dropshipCollar")
                    + "</b><br></html>"), gridBagConstraints);
            int collarHits = 0;
            if (((Dropship) aero).isDockCollarDamaged()) {
                collarHits = 1;
            }
            dockCollarCrit = new CheckCritPanel(1, collarHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(dockCollarCrit, gridBagConstraints);
            
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.weightx = 0.0;
            panSystem.add(new JLabel("<html><b>" + Messages.getString("UnitEditorDialog.kfBoom")
                    + "</b><br></html>"), gridBagConstraints);
            int kfboomHits = 0;
            if (((Dropship) aero).isKFBoomDamaged()) {
                kfboomHits = 1;
            }
            kfboomCrit = new CheckCritPanel(1, kfboomHits);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panSystem.add(kfboomCrit, gridBagConstraints);
        }
        
        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            int b = 0;
            JSpinner bayCrit;
            Vector<Bay> bays = aero.getTransportBays();
            bayDamage = new JSpinner[bays.size()];
            bayDoorCrit = new CheckCritPanel [bays.size()];
            for (Bay nextbay : bays) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" +
                        String.format(Messages.getString("UnitEditorDialog.bayCrit"), nextbay.getType(), nextbay.getBayNumber())
                        + "</b><br></html>"), gridBagConstraints);

                bayCrit = new JSpinner(new SpinnerNumberModel(nextbay.getCapacity() - nextbay.getBayDamage(),
                        0, nextbay.getCapacity(), nextbay.isCargo() ? 0.5: 1.0));
                bayDamage[b] = bayCrit;
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(bayCrit, gridBagConstraints);

                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.weightx = 0.0;
                panSystem.add(new JLabel("<html><b>" + 
                        String.format(Messages.getString("UnitEditorDialog.bayDoorCrit"), nextbay.getBayNumber())
                        + "</b><br></html>"), gridBagConstraints);

                CheckCritPanel doorCrit = new CheckCritPanel(nextbay.getDoors(), (nextbay.getDoors() - nextbay.getCurrentDoors()));
                bayDoorCrit[b] = doorCrit;
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                panSystem.add(doorCrit, gridBagConstraints);
                b++;
            }
        }
    }

    /** Applies the given number of total crits to a Super-Cooled Myomer (which is spread over 6 locations). */
    public void damageSCM(Entity entity, int eqNum, int hits) {
        int nhits = 0;
        Mounted m = entity.getEquipment(eqNum);
        for (int loc = 0; loc < entity.locations(); loc++) {
            for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
                CriticalSlot cs = entity.getCritical(loc, i);
                if ((cs == null) || (cs.getType() != CriticalSlot.TYPE_EQUIPMENT)
                        || ((m != cs.getMount()) && (m != cs.getMount2()))) {
                    continue;
                }

                if (nhits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    nhits++;
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
        for (Mounted m : entity.getEquipment()) {
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
        if (entity instanceof Infantry) {
            ((Infantry) entity).damageOrRestoreFieldWeapons();
            entity.applyDamage();
        }

        // now systems
        if (entity instanceof Mech) {
            if (null != centerEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_ENGINE, Mech.LOC_CT,
                        centerEngineCrit.getHits());
            }
            if (null != leftEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_ENGINE, Mech.LOC_LT,
                        leftEngineCrit.getHits());
            }
            if (null != rightEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_ENGINE, Mech.LOC_RT,
                        rightEngineCrit.getHits());
            }
            if (null != gyroCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                        gyroCrit.getHits());
            }
            if (null != sensorCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_SENSORS, sensorCrit.getHits());
            }
            if (null != lifeSupportCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_LIFE_SUPPORT, lifeSupportCrit.getHits());
            }
            if (null != cockpitCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_COCKPIT, cockpitCrit.getHits());
            }
            if (null != lamAvionicsCrit && !lamAvionicsCrit.isEmpty()) {
                for (int loc : lamAvionicsCrit.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS,
                            loc, lamAvionicsCrit.get(loc).getHits());
                }
            }
            if (null != lamLandingGearCrit && !lamLandingGearCrit.isEmpty()) {
                for (int loc : lamLandingGearCrit.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR,
                            loc, lamLandingGearCrit.get(loc).getHits());
                }
            }

            for (int i = 0; i < actuatorCrits.length; i++) {
                for (int j = 0; j < actuatorCrits[i].length; j++) {
                    CheckCritPanel actuatorCrit = actuatorCrits[i][j];
                    if (null == actuatorCrit) {
                        continue;
                    }
                    int loc = i + Mech.LOC_RARM;
                    int actuator = j + Mech.ACTUATOR_SHOULDER;
                    if ((loc >= Mech.LOC_RLEG) || (entity instanceof QuadMech)) {
                        actuator = j + Mech.ACTUATOR_HIP;
                    }
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator,
                            loc, actuatorCrit.getHits());
                }

                if (entity instanceof QuadVee) {
                    for (int j = 0; j < actuatorCrits.length; j++) {
                        CheckCritPanel actuatorCrit = actuatorCrits[i][4];
                        if (null == actuatorCrit) {
                            continue;
                        }
                        int loc = i + Mech.LOC_RARM;
                        int actuator = QuadVee.SYSTEM_CONVERSION_GEAR;
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator,
                                loc, actuatorCrit.getHits());
                    }
                }
            }
        } else if (entity instanceof Protomech) {
            for (int loc = 0; loc < entity.locations(); loc++) {
                if (null == protoCrits[loc]) {
                    continue;
                }
                if ((loc == Protomech.LOC_LARM) || (loc == Protomech.LOC_RARM)) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                            Protomech.SYSTEM_ARMCRIT, loc,
                            protoCrits[loc].getHits());
                }
                if (loc == Protomech.LOC_LEG) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                            Protomech.SYSTEM_LEGCRIT, loc,
                            protoCrits[loc].getHits());
                }
                if (loc == Protomech.LOC_HEAD) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                            Protomech.SYSTEM_HEADCRIT, loc,
                            protoCrits[loc].getHits());
                }
                if (loc == Protomech.LOC_TORSO) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                            Protomech.SYSTEM_TORSOCRIT, loc,
                            protoCrits[loc].getHits());
                }
            }
        } else if (entity instanceof Tank) {
            Tank tank = (Tank) entity;
            if (null != engineCrit) {
                if (engineCrit.getHits() > 0) {
                    tank.engineHit();
                } else {
                    tank.engineFix();
                }
            }
            if (null != turretlockCrit) {
                if (turretlockCrit.getHits() > 0) {
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

                // Apply movement damage immediately in case we've decided to immobilize the tank
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
        } else if (entity instanceof Aero) {
            Aero aero = (Aero) entity;
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
                ((Dropship) aero)
                        .setDamageDockCollar(dockCollarCrit.getHits() > 0);
            }
            if ((null != kfboomCrit) && (aero instanceof Dropship)) {
                ((Dropship) aero)
                        .setDamageKFBoom(kfboomCrit.getHits() > 0);
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
                    if (bay instanceof ASFBay) {
                        ASFBay a = (ASFBay) bay;
                        a.initializeRecoverySlots();
                    }
                    if (bay instanceof SmallCraftBay) {
                        SmallCraftBay s = (SmallCraftBay) bay;
                        s.initializeRecoverySlots();
                    }
                b++;
                }
            }
            // Jumpship Docking Collars, KF Drive, Sail and Grav Decks
            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                JSpinner collarCrit = collarDamage;
                CheckCritPanel deckCrit = gravDeckCrit;
                double damagedCollars = 0.0;
                int damagedDecks = 0;
                if (null != collarCrit) {
                    damagedCollars = (aero.getDockingCollars().size() - (double) collarCrit.getModel().getValue());
                }
                //First, reset damaged collars to undamaged. Otherwise you get weirdness when running this dialogue multiple times
                for (DockingCollar collar : aero.getDockingCollars()) {
                    collar.setDamaged(false);
                }
                //Otherwise, run through the list and damage one until the spinner value is satisfied
                for (DockingCollar collar : aero.getDockingCollars()) {
                    if (damagedCollars <= 0) {
                        break;
                    }
                    collar.setDamaged(true);
                    damagedCollars --;
                }
                if (null != deckCrit) {
                    damagedDecks = deckCrit.getHits();
                }
                //reset all grav decks to undamaged
                for (int i = 0; i < js.getTotalGravDeck(); i++) {
                    js.setGravDeckDamageFlag(i, 0);
                }
                if (damagedDecks > 0) {
                    //loop through the grav decks from #1 and damage them
                    for (int i = 0; i < damagedDecks; i++) {
                        js.setGravDeckDamageFlag(i, 1);
                    }
                }
                //KF Drive and Sail
                if (null != kfDamage) {
                    double kfIntegrity = (double) kfDamage.getModel().getValue();
                    js.setKFIntegrity((int) kfIntegrity);
                }
                if (null != chargingSystemCrit) {
                    js.setKFChargingSystemHit(chargingSystemCrit.getHits() > 0);
                }
                if (null != driveCoilCrit) {
                    js.setKFDriveCoilHit(driveCoilCrit.getHits() > 0);
                }
                if (null != driveControllerCrit) {
                    js.setKFDriveControllerHit(driveControllerCrit.getHits() > 0);
                }
                if (null != fieldInitiatorCrit) {
                    js.setKFFieldInitiatorHit(fieldInitiatorCrit.getHits() > 0);
                }
                if (null != heliumTankCrit) {
                    js.setKFHeliumTankHit(heliumTankCrit.getHits() > 0);
                }
                if (null != lfBatteryCrit) {
                    js.setLFBatteryHit(lfBatteryCrit.getHits() > 0);
                }
                if (null != sailDamage) {
                    double sailIntegrity = (double) sailDamage.getModel().getValue();
                    js.setSailIntegrity((int) sailIntegrity);
                }
            }
        }

    }

    private class CheckCritPanel extends JPanel {

        /**
         *
         */
        private static final long serialVersionUID = 8662728291188274362L;

        private ArrayList<JCheckBox> checks = new ArrayList<>();

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
                for (int i = 0; i < current; i++) {
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
            } else if (hits < checks.size()) {
                // deselect any above this one
                for (int i = hits; i < checks.size(); i++) {
                    checks.get(i).setSelected(false);
                }
            }

        }
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}
