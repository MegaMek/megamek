/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * CustomMechDialog.java
 *
 * Created on March 18, 2002, 2:56 PM
 */

package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.GunEmplacement;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.Infantry;
import megamek.common.LAMPilot;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.OffBoardDirection;
import megamek.common.Protomech;
import megamek.common.QuadVee;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PartialRepairs;
import megamek.common.options.PilotOptions;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;
import megamek.common.util.MegaMekFile;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestInfantry;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestSupportVehicle;
import megamek.common.verifier.TestTank;

/**
 * A dialog that a player can use to customize his mech before battle.
 * Currently, changing pilots, setting up C3 networks, changing ammunition,
 * deploying artillery offboard, setting MGs to rapidfire, setting auto-eject is
 * supported.
 *
 * @author Ben
 */
public class CustomMechDialog extends ClientDialog implements ActionListener,
        DialogOptionListener, ItemListener {

    /**
     *
     */
    private static final long serialVersionUID = -6809436986445582731L;

    public static int DONE = 0;
    public static int NEXT = 1;
    public static int PREV = 2;

    private CustomPilotView[] panCrewMember;
    private JPanel panDeploy;
    private QuirksPanel panQuirks;
    private JPanel panPartReps;

    private JPanel panOptions;
    // private JScrollPane scrOptions;

    private JTabbedPane tabAll;

    private final JTextField fldFatigue = new JTextField(3);
    private JTextField fldInit = new JTextField(3);
    private JTextField fldCommandInit = new JTextField(3);
    private JCheckBox chCommander = new JCheckBox();

    private JLabel labDeploymentRound = new JLabel(
            Messages.getString("CustomMechDialog.labDeployment"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JLabel labDeploymentZone = new JLabel(
            Messages.getString("CustomMechDialog.labDeploymentZone"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JComboBox<String> choDeploymentRound = new JComboBox<String>();
    
    private JComboBox<String> choDeploymentZone = new JComboBox<String>();

    private JLabel labDeployShutdown = new JLabel(
            Messages.getString("CustomMechDialog.labDeployShutdown"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JCheckBox chDeployShutdown = new JCheckBox();

    private JLabel labDeployProne = new JLabel(
            Messages.getString("CustomMechDialog.labDeployProne"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JCheckBox chDeployProne = new JCheckBox();

    private JLabel labDeployHullDown = new JLabel(
            Messages.getString("CustomMechDialog.labDeployHullDown"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JCheckBox chDeployHullDown = new JCheckBox();

    private JLabel labHidden = new JLabel(
            Messages.getString("CustomMechDialog.labHidden"), //$NON-NLS-1$
            SwingConstants.RIGHT);

    private JCheckBox chHidden = new JCheckBox();

    private JLabel labOffBoard = new JLabel(
            Messages.getString("CustomMechDialog.labOffBoard"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JCheckBox chOffBoard = new JCheckBox();

    private JLabel labOffBoardDirection = new JLabel(
            Messages.getString("CustomMechDialog.labOffBoardDirection"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JComboBox<String> choOffBoardDirection = new JComboBox<String>();

    private JLabel labOffBoardDistance = new JLabel(
            Messages.getString("CustomMechDialog.labOffBoardDistance"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JTextField fldOffBoardDistance = new JTextField(4);

    private JButton butOffBoardDistance = new JButton("0");
    
    private JLabel labStartingMode = new JLabel(
            Messages.getString("CustomMechDialog.labStartingMode"), SwingConstants.RIGHT); //$NON-NLS-1$
    
    private JComboBox<String> choStartingMode = new JComboBox<>();

    private JLabel labStartVelocity = new JLabel(
            Messages.getString("CustomMechDialog.labStartVelocity"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JTextField fldStartVelocity = new JTextField(3);

    private JLabel labStartAltitude = new JLabel(
            Messages.getString("CustomMechDialog.labStartAltitude"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JTextField fldStartAltitude = new JTextField(3);

    private JLabel labStartHeight = new JLabel(
            Messages.getString("CustomMechDialog.labStartHeight"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JTextField fldStartHeight = new JTextField(3);
    
    private JCheckBox chDeployAirborne = new JCheckBox();

    private JPanel panButtons = new JPanel();

    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$

    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private JButton butNext = new JButton(Messages.getString("Next"));

    private JButton butPrev = new JButton(Messages.getString("Previous"));

    private EquipChoicePanel m_equip;

    private JPanel panEquip = new JPanel();

    private List<Entity> entities;

    private boolean okay;

    private int status = CustomMechDialog.DONE;

    ClientGUI clientgui;

    Client client;

    private PilotOptions options;
    private Quirks quirks;
    private PartialRepairs partReps;
    private HashMap<Integer, WeaponQuirks> h_wpnQuirks = new HashMap<Integer, WeaponQuirks>();

    private ArrayList<DialogOptionComponent> optionComps = new ArrayList<DialogOptionComponent>();
    // private ArrayList<DialogOptionComponent> quirkComps = new
    // ArrayList<DialogOptionComponent>();
    private ArrayList<DialogOptionComponent> partRepsComps = new ArrayList<DialogOptionComponent>();
    // private HashMap<Integer, ArrayList<DialogOptionComponent>>
    // h_wpnQuirkComps = new HashMap<Integer,
    // ArrayList<DialogOptionComponent>>();

    private boolean editable;

    private OffBoardDirection direction = OffBoardDirection.NONE;

    private int distance = 17;

    /**
     * Creates new CustomMechDialog
     */
    public CustomMechDialog(ClientGUI clientgui, Client client, List<Entity> entities,
            boolean editable) {
        super(clientgui.frame,
                Messages.getString("CustomMechDialog.title"), true); //$NON-NLS-1$

        this.entities = entities;
        this.clientgui = clientgui;
        this.client = client;

        // Ensure we have at least one passed entity
        //  Anything less makes no sense
        if (entities.size() < 1) {
            throw new IllegalStateException("Must pass at least one Entity!");
        }

        boolean multipleEntities = entities.size() > 1;
        boolean quirksEnabled = clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        boolean partialRepairsEnabled = clientgui.getClient().getGame()
                .getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS);
        final Entity entity = entities.get(0);
        boolean isAero = true;
        boolean isMech = true;
        boolean isVTOL = true;
        boolean isWiGE = true;
        boolean isQuadVee = true;
        boolean isLAM = true;
        boolean isGlider = true;
        boolean eligibleForOffBoard = true;
        
        for (Entity e : entities) {
            isAero &= e instanceof Aero;
            isMech &= e instanceof Mech;
            isVTOL &= e instanceof VTOL;
            isWiGE &= e instanceof Tank && e.getMovementMode() == EntityMovementMode.WIGE;
            isQuadVee &= e instanceof QuadVee;
            isLAM &= e instanceof LandAirMech;
            isGlider &= e instanceof Protomech && e.getMovementMode() == EntityMovementMode.WIGE;
            boolean entityEligibleForOffBoard = false;
            for (Mounted mounted : e.getWeaponList()) {
                WeaponType wtype = (WeaponType) mounted.getType();
                if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                    entityEligibleForOffBoard = true;
                }
            }
            eligibleForOffBoard &= entityEligibleForOffBoard;
        }

        // set up the panels
        JPanel mainPanel = new JPanel(new GridBagLayout());
        tabAll = new JTabbedPane();
        
        JPanel panCrew = new JPanel(new GridBagLayout());
        panCrewMember = new CustomPilotView[entity.getCrew().getSlotCount()];
        for (int i = 0; i < panCrewMember.length; i++) {
            panCrewMember[i] = new CustomPilotView(this, entity, i, editable);
        }
        panDeploy = new JPanel(new GridBagLayout());
        quirks = entity.getQuirks();
        panQuirks = new QuirksPanel(entity, quirks, editable, this, h_wpnQuirks);
        panPartReps = new JPanel(new GridBagLayout());
        setupEquip();
        
        mainPanel.add(tabAll,
                GBC.eol().fill(GridBagConstraints.BOTH).insets(5, 5, 5, 5));
        mainPanel.add(panButtons, GBC.eol().anchor(GridBagConstraints.CENTER));

        JScrollPane scrEquip = new JScrollPane(panEquip);
        if (!multipleEntities) {
            if (panCrewMember.length > 1) {
                for (int i = 0; i < panCrewMember.length; i++) {
                    tabAll.addTab(entity.getCrew().getCrewType().getRoleName(i),
                            new JScrollPane(panCrewMember[i]));
                }
                tabAll.addTab(Messages.getString("CustomMechDialog.tabCrew"), new JScrollPane(panCrew));
            } else {
                panCrew.add(panCrewMember[0], GBC.eop());
                tabAll.addTab(Messages.getString("CustomMechDialog.tabPilot"), new JScrollPane(panCrew));
            }
            tabAll.addTab(Messages.getString("CustomMechDialog.tabEquipment"),
                    scrEquip);
        }
        tabAll.addTab(Messages.getString("CustomMechDialog.tabDeployment"),
                new JScrollPane(panDeploy));
        if (quirksEnabled && !multipleEntities) {
            JScrollPane scrQuirks = new JScrollPane(panQuirks);
            scrQuirks.setPreferredSize(scrEquip.getPreferredSize());
            tabAll.addTab("Quirks", scrQuirks);
        }
        if (partialRepairsEnabled && !multipleEntities) {
            tabAll.addTab(
                    Messages.getString("CustomMechDialog.tabPartialRepairs"),
                    new JScrollPane(panPartReps));
        }
        getContentPane().add(mainPanel);

        options = entity.getCrew().getOptions();
        partReps = entity.getPartialRepairs();
        for (Mounted m : entity.getWeaponList()) {
            h_wpnQuirks.put(entity.getEquipmentNum(m), m.getQuirks());
        }
        // Also need to consider melee weapons
        for (Mounted m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_CLUB)) {
                h_wpnQuirks.put(entity.getEquipmentNum(m), m.getQuirks());
            }
        }
        this.editable = editable;
        
        // **CREW TAB**//
        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_FATIGUE)) {
            panCrew.add(new JLabel(Messages.getString("CustomMechDialog.labFatigue"), SwingConstants.RIGHT),
                    GBC.std()); //$NON-NLS-1$
            panCrew.add(fldFatigue, GBC.eop());
            fldFatigue.setToolTipText(Messages.getString("CustomMechDialog.labFatigueToolTip"));
        }
        fldFatigue.setText(Integer.toString(entity.getCrew().getFatigue()));

        if (clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            panCrew.add(new JLabel(Messages.getString("CustomMechDialog.labInit"), SwingConstants.RIGHT),
                    GBC.std()); //$NON-NLS-1$
            panCrew.add(fldInit, GBC.eop());
        }
        fldInit.setText(Integer.toString(entity.getCrew().getInitBonus()));

        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT)) {
            panCrew.add(new JLabel(Messages.getString("CustomMechDialog.labCommandInit"), SwingConstants.RIGHT),
                    GBC.std()); //$NON-NLS-1$
            panCrew.add(fldCommandInit, GBC.eop());
        }
        fldCommandInit.setText(Integer.toString(entity.getCrew()
                .getCommandBonus()));

        // Set up commanders for commander killed victory condition
        if (clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) { //$NON-NLS-1$
            panCrew.add(new JLabel(Messages.getString("CustomMechDialog.labCommander"), SwingConstants.RIGHT),
                    GBC.std()); //$NON-NLS-1$
            panCrew.add(chCommander, GBC.eol());
            chCommander.setSelected(entity.isCommander());
        }
        panOptions = new JPanel(new GridBagLayout());
        panCrew.add(panOptions, GBC.eop());

        // **DEPLOYMENT TAB**//
        
        if (isQuadVee || isLAM) {
            panDeploy.add(labStartingMode, GBC.std());
            panDeploy.add(choStartingMode, GBC.eol());
            choStartingMode.addItemListener(this);
            labStartingMode.setToolTipText(Messages
                    .getString("CustomMechDialog.startingModeToolTip")); //$NON-NLS-1$
            choStartingMode.setToolTipText(Messages
                    .getString("CustomMechDialog.startingModeToolTip")); //$NON-NLS-1$
            refreshDeployment();
        }
        if (isVTOL || isLAM || isGlider) {
            panDeploy.add(labStartHeight, GBC.std());
            panDeploy.add(fldStartHeight, GBC.eol());
        }
        if (isWiGE) {
            panDeploy.add(new JLabel(Messages.getString(
                    "CustomMechDialog.labDeployAirborne"), SwingConstants.RIGHT), GBC.std()); //$NON-NLS-1$
            panDeploy.add(chDeployAirborne, GBC.eol());
        }
        if (isAero || isLAM) {
            panDeploy.add(labStartVelocity, GBC.std());
            panDeploy.add(fldStartVelocity, GBC.eol());

            panDeploy.add(labStartAltitude, GBC.std());
            panDeploy.add(fldStartAltitude, GBC.eol());
        }

        choDeploymentRound.addItemListener(this);

        panDeploy.add(labDeploymentRound, GBC.std());
        panDeploy.add(choDeploymentRound, GBC.eol());
        panDeploy.add(labDeploymentZone, GBC.std());
        panDeploy.add(choDeploymentZone, GBC.eol());
        if (clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.RPG_BEGIN_SHUTDOWN)
                && !(entity instanceof Infantry)
                && !(entity instanceof GunEmplacement)) {
            panDeploy.add(labDeployShutdown, GBC.std());
            panDeploy.add(chDeployShutdown, GBC.eol());
            chDeployShutdown.setSelected(entity.isManualShutdown());
        }

        if (isMech) {
            panDeploy.add(labDeployHullDown, GBC.std());
            panDeploy.add(chDeployHullDown, GBC.eol());
            chDeployHullDown.setSelected(entity.isHullDown()
                    && !entity.isProne());
            chDeployHullDown.addItemListener(this);

            panDeploy.add(labDeployProne, GBC.std());
            panDeploy.add(chDeployProne, GBC.eol());
            chDeployProne.setSelected(entity.isProne() && !entity.isHullDown());
            chDeployProne.addItemListener(this);
        }

        refreshDeployment();

        if (clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
            panDeploy.add(labHidden, GBC.std());
            panDeploy.add(chHidden, GBC.eol());
            chHidden.setSelected(entity.isHidden());
        }
        
        if (eligibleForOffBoard) {
            panDeploy.add(labOffBoard, GBC.std());
            panDeploy.add(chOffBoard, GBC.eol());
            chOffBoard.setSelected(entity.isOffBoard());

            panDeploy.add(labOffBoardDirection, GBC.std());

            choOffBoardDirection.addItem(Messages
                    .getString("CustomMechDialog.North")); //$NON-NLS-1$
            choOffBoardDirection.addItem(Messages
                    .getString("CustomMechDialog.South")); //$NON-NLS-1$
            choOffBoardDirection.addItem(Messages
                    .getString("CustomMechDialog.East")); //$NON-NLS-1$
            choOffBoardDirection.addItem(Messages
                    .getString("CustomMechDialog.West")); //$NON-NLS-1$
            direction = entity.getOffBoardDirection();
            if (OffBoardDirection.NONE == direction) {
                direction = OffBoardDirection.NORTH;
            }
            choOffBoardDirection.setSelectedIndex(direction.getValue());
            panDeploy.add(choOffBoardDirection, GBC.eol());

            panDeploy.add(labOffBoardDistance, GBC.std());

            butOffBoardDistance.addActionListener(this);
            butOffBoardDistance.setText(Integer.toString(distance));
            panDeploy.add(butOffBoardDistance, GBC.eol());
        }

        setupButtons();

        if (isAero || isLAM) {
            IAero a = (IAero) entity;
            fldStartVelocity.setText(new Integer(a.getCurrentVelocity())
                    .toString());
            fldStartVelocity.addActionListener(this);

            fldStartAltitude.setText(new Integer(entity.getAltitude()).toString());
            fldStartAltitude.addActionListener(this);
        }
        if (isVTOL || isLAM || isGlider) {
            fldStartHeight.setText(new Integer(entity.getElevation()).toString());
            fldStartHeight.addActionListener(this);
        }
        if (isWiGE) {
            chDeployAirborne.setSelected(entity.getElevation() > 0);
        }

        if (!editable) {
            fldFatigue.setEnabled(false);
            fldInit.setEnabled(false);
            fldCommandInit.setEnabled(false);
            chCommander.setEnabled(false);
            choDeploymentRound.setEnabled(false);
            chDeployShutdown.setEnabled(false);
            chDeployProne.setEnabled(false);
            chDeployHullDown.setEnabled(false);
            chCommander.setEnabled(false);
            chHidden.setEnabled(false);
            chOffBoard.setEnabled(false);
            choOffBoardDirection.setEnabled(false);
            fldOffBoardDistance.setEnabled(false);
            fldStartVelocity.setEnabled(false);
            fldStartAltitude.setEnabled(false);
            fldStartHeight.setEnabled(false);
            chDeployAirborne.setEnabled(false);
            m_equip.initialize();
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        mainPanel.setSize(mainPanel.getSize().width,
                Math.min(mainPanel.getSize().height, 400));
        setResizable(true);
        setLocationRelativeTo(clientgui);
    }

    public String getSelectedTab() {
        return tabAll.getTitleAt(tabAll.getSelectedIndex());
    }

    public void setSelectedTab(int idx) {
        if (idx < tabAll.getTabCount()) {
            tabAll.setSelectedIndex(idx);
        }
    }
    
    public void setSelectedTab(String tabName) {
        for (int i = 0; i < tabAll.getTabCount(); i++) {
            if (tabAll.getTitleAt(i).equals(tabName)) {
                tabAll.setSelectedIndex(i);
            }
        }
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        butNext.addActionListener(this);
        butPrev.addActionListener(this);

        // layout
        panButtons.setLayout(new GridLayout(1, 4, 10, 0));
        panButtons.add(butPrev);
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        panButtons.add(butNext);

        butNext.setEnabled(getNextEntity(true) != null);
        butPrev.setEnabled(getNextEntity(false) != null);
    }

    private void setOptions() {
        Entity entity = entities.get(0);
        IOption option;
        for (final Object newVar : optionComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue() == Messages.getString("CustomMechDialog.None"))) { // NON-NLS-$1
                entity.getCrew().getOptions().getOption(option.getName())
                        .setValue("None"); // NON-NLS-$1
            } else {
                entity.getCrew().getOptions().getOption(option.getName())
                        .setValue(comp.getValue());
            }
        }
    }

    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new ArrayList<DialogOptionComponent>();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)
                    && !clientgui.getClient().getGame().getOptions()
                            .booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES)) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PilotOptions.EDGE_ADVANTAGES)
                    && !clientgui.getClient().getGame().getOptions()
                            .booleanOption(OptionsConstants.EDGE)) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES)
                    && !clientgui.getClient().getGame().getOptions()
                            .booleanOption(OptionsConstants.RPG_MANEI_DOMINI)) {
                continue;
            }

            addGroup(group, gridbag, c);

            Entity entity = entities.get(0);
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                if (entity instanceof GunEmplacement) {
                    continue;
                }

                // a bunch of stuf should get disabled for conv infantry
                if ((((entity instanceof Infantry) && !(entity instanceof BattleArmor)))
                        && (option.getName().equals(OptionsConstants.MD_VDNI) || option.getName()
                                .equals(OptionsConstants.MD_BVDNI))) {
                    continue;
                }

                // a bunch of stuff should get disabled for all but conventional
                // infantry
                if (!((entity instanceof Infantry) && !(entity instanceof BattleArmor))
                        && (option.getName().equals(OptionsConstants.MD_PL_ENHANCED)
                                || option.getName().equals(OptionsConstants.MD_PL_MASC)
                                || option.getName().equals(OptionsConstants.MD_CYBER_IMP_AUDIO) || option
                                .getName().equals(OptionsConstants.MD_CYBER_IMP_VISUAL))) {
                    continue;
                }

                addOption(option, gridbag, c, editable);
            }
        }

        validate();
    }

    private void setPartReps() {
        Entity entity = entities.get(0);
        IOption option;
        for (final Object newVar : partRepsComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue() == Messages.getString("CustomMechDialog.None"))) { // NON-NLS-$1
                entity.getPartialRepairs().getOption(option.getName())
                        .setValue("None"); // NON-NLS-$1
            } else {
                entity.getPartialRepairs().getOption(option.getName())
                        .setValue(comp.getValue());
            }
        }

    }

    private void setQuirks() {
        panQuirks.setQuirks();
    }

    public void refreshPartReps() {
        Entity entity = entities.get(0);
        panPartReps.removeAll();
        partRepsComps = new ArrayList<DialogOptionComponent>();
        for (Enumeration<IOptionGroup> i = partReps.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            panPartReps.add(new JLabel(group.getDisplayableName()), GBC.eol());

            for (Enumeration<IOption> j = group.getSortedOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                if (!PartialRepairs.isPartRepLegalFor(option, entity)) {
                    continue;
                }

                addPartRep(option, editable);
            }
        }
        validate();
    }

    public void refreshQuirks() {
        panQuirks.refreshQuirks();
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridbag,
            GridBagConstraints c) {
        JLabel groupLabel = new JLabel(group.getDisplayableName());

        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridbag, GridBagConstraints c, boolean editable) {
        Entity entity = entities.get(0);
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, editable);

        if ((OptionsConstants.GUNNERY_WEAPON_SPECIALIST).equals(option.getName())) { // $NON-NLS-1$
            optionComp.addValue(Messages.getString("CustomMechDialog.None")); //$NON-NLS-1$
            TreeSet<String> uniqueWeapons = new TreeSet<String>();
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                Mounted m = entity.getWeaponList().get(i);
                uniqueWeapons.add(m.getName());
            }
            for (String name : uniqueWeapons) {
                optionComp.addValue(name);
            }
            optionComp.setSelected(option.stringValue());
        }

        if (OptionsConstants.GUNNERY_SPECIALIST               
                .equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue(Crew.SPECIAL_NONE);
            optionComp.addValue(Crew.SPECIAL_ENERGY);
            optionComp.addValue(Crew.SPECIAL_BALLISTIC);
            optionComp.addValue(Crew.SPECIAL_MISSILE);
            optionComp.setSelected(option.stringValue());
        }

        if (OptionsConstants.GUNNERY_RANGE_MASTER.equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue(Crew.RANGEMASTER_NONE);
            optionComp.addValue(Crew.RANGEMASTER_MEDIUM);
            optionComp.addValue(Crew.RANGEMASTER_LONG);
            optionComp.addValue(Crew.RANGEMASTER_EXTREME);
            optionComp.setSelected(option.stringValue());
        }

        if (OptionsConstants.MISC_HUMAN_TRO.equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue(Crew.HUMANTRO_NONE);
            optionComp.addValue(Crew.HUMANTRO_MECH);
            optionComp.addValue(Crew.HUMANTRO_AERO);
            optionComp.addValue(Crew.HUMANTRO_VEE);
            optionComp.addValue(Crew.HUMANTRO_BA);
            optionComp.setSelected(option.stringValue());
        }

        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);

        optionComps.add(optionComp);
    }

    private void addPartRep(IOption option, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this,
                option, editable);
        panPartReps.add(optionComp, GBC.eol());
        partRepsComps.add(optionComp);
    }

    public void optionClicked(DialogOptionComponent comp, IOption option,
            boolean state) {
    }

    public boolean isOkay() {
        return okay;
    }

    public int getStatus() {
        return status;
    }

    private void refreshDeployment() {
        Entity entity = entities.get(0);
        
        if (entity instanceof QuadVee) {
            choStartingMode.removeItemListener(this);
            choStartingMode.removeAllItems();
            choStartingMode.addItem(Messages.getString("CustomMechDialog.ModeQuad"));
            choStartingMode.addItem(Messages.getString("CustomMechDialog.ModeVehicle"));
            if (entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE) {
                choStartingMode.setSelectedIndex(1);
            }
            updateStartingModeOptions();
            choStartingMode.addItemListener(this);
        } else if (entity instanceof LandAirMech) {
            choStartingMode.removeItemListener(this);
            choStartingMode.removeAllItems();
            choStartingMode.addItem(Messages.getString("CustomMechDialog.ModeBiped"));
            if (((LandAirMech)entity).getLAMType() != LandAirMech.LAM_BIMODAL) {
                choStartingMode.addItem(Messages.getString("CustomMechDialog.ModeAirMech"));
            }
            choStartingMode.addItem(Messages.getString("CustomMechDialog.ModeFighter"));
            if (entity.getConversionMode() == LandAirMech.CONV_MODE_AIRMECH) {
                choStartingMode.setSelectedIndex(1);
            } else if (entity.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER) {
                choStartingMode.setSelectedIndex(choStartingMode.getItemCount() - 1);
            }
            updateStartingModeOptions();
            choStartingMode.addItemListener(this);
        }
        
        choDeploymentRound.removeItemListener(this);
        
        choDeploymentRound.removeAllItems();
        choDeploymentRound.addItem(Messages
                .getString("CustomMechDialog.StartOfGame")); //$NON-NLS-1$

        if (entity.getDeployRound() < 1) {
            choDeploymentRound.setSelectedIndex(0);
        }

        for (int i = 1; i <= 40; i++) {
            choDeploymentRound.addItem(Messages
                    .getString("CustomMechDialog.AfterRound") + i); //$NON-NLS-1$

            if (entity.getDeployRound() == i) {
                choDeploymentRound.setSelectedIndex(i);
            }
        }
        if (entity.getTransportId() != Entity.NONE) {
            choDeploymentRound.setEnabled(false);
        }
        
        choDeploymentZone.removeAllItems();
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.useOwners")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployAny")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployNW")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployN")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployNE")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployE")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deploySE")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployS")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deploySW")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployW")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployEdge")); //$NON-NLS-1$
        choDeploymentZone.addItem(Messages
                .getString("CustomMechDialog.deployCenter")); //$NON-NLS-1$
        
        if (entity.getDeployRound() == 0) {
            choDeploymentZone.setEnabled(false);
        } else {
            choDeploymentZone.setEnabled(true);
        }
        choDeploymentZone.setSelectedIndex(entity.getStartingPos(false) + 1);
        
        choDeploymentRound.addItemListener(this);

        chHidden.removeActionListener(this);
        boolean enableHidden = true;
        // Airborne units can't be hidden
        if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            enableHidden = false;
        }
        // Landed dropships can't be hidden
        if ((entity instanceof Dropship)) {
            enableHidden = false;
        }
        labHidden.setEnabled(enableHidden);
        chHidden.setEnabled(enableHidden);
        chHidden.addActionListener(this);
    }

    public void actionPerformed(ActionEvent actionEvent) {

        if (actionEvent.getSource().equals(butOffBoardDistance)) {
            int maxDistance = 19 * 17; // Long Tom
            for (Entity entity : entities) {
                for (Mounted wep : entity.getWeaponList()) {
                    EquipmentType e = wep.getType();
                    WeaponType w = (WeaponType) e;
                    if (w.hasFlag(WeaponType.F_ARTILLERY)) {
                        int nDistance = (w.getLongRange() - 1) * 17;
                        if (nDistance < maxDistance) {
                            maxDistance = nDistance;
                        }
                    }
                }
            }
            // int dist = Math.min(Math.max(entity.getOffBoardDistance(), 17),
            // maxDistance);
            Slider sl = new Slider(
                    clientgui.frame,
                    Messages.getString("CustomMechDialog.offboardDistanceTitle"),
                    Messages.getString("CustomMechDialog.offboardDistanceQuestion"),
                    Math.min(
                            Math.max(entities.get(0).getOffBoardDistance(), 17),
                            maxDistance), 17, maxDistance);
            if (!sl.showDialog()) {
                return;
            }
            distance = sl.getValue();
            butOffBoardDistance.setText(Integer.toString(distance));
            return;
        }
        
        if (actionEvent.getSource().equals(butCancel)) {
            setVisible(false);
            return;
        }
        
        if (actionEvent.getSource().equals(chHidden)) {
            return;
        }
        
        if (actionEvent.getActionCommand().equals("missing")) {
            //If we're down to a single crew member, do not allow any more to be removed.
            final long remaining = Arrays.stream(panCrewMember).filter(p -> !p.getMissing()).count();
            for (CustomPilotView v : panCrewMember) {
                v.enableMissing(remaining > 1 || v.getMissing());
            }
            return;
        }

        // Set instanceof flags
        String msg, title;
        boolean isAero = true;
        boolean isVTOL = true;
        boolean isWiGE = true;
        boolean isQuadVee = true;
        boolean isLAM = true;
        boolean isAirMech = true;
        boolean isGlider = true;
        for (Entity e : entities) {
            isAero &= e instanceof Aero
                    || e instanceof LandAirMech
                        && (choStartingMode.getSelectedIndex() == 2
                            || ((LandAirMech)e).getLAMType() == LandAirMech.LAM_BIMODAL
                                && choStartingMode.getSelectedIndex() == 1);
            isVTOL &= e instanceof VTOL;
            isWiGE &= e instanceof Tank && e.getMovementMode() == EntityMovementMode.WIGE;
            isQuadVee &= e instanceof QuadVee;
            isLAM &= e instanceof LandAirMech;
            isAirMech &= e instanceof LandAirMech
                    && ((LandAirMech)e).getLAMType() == LandAirMech.LAM_STANDARD
                    && choStartingMode.getSelectedIndex() == 1;
            isGlider &= e instanceof Protomech && e.getMovementMode() == EntityMovementMode.WIGE;
        }

        // get values
        int fatigue = 0;
        int init = 0;
        int command = 0;
        int velocity = 0;
        int altitude = 0;
        int height = 0;
        int offBoardDistance;
        try {
            init = Integer.parseInt(fldInit.getText());
            fatigue = Integer.parseInt(fldFatigue.getText());
            command = Integer.parseInt(fldCommandInit.getText());
            if (isAero) {
                velocity = Integer.parseInt(fldStartVelocity.getText());
                altitude = Integer.parseInt(fldStartAltitude.getText());
            }
            if (isVTOL || isAirMech) {
                height = Integer.parseInt(fldStartHeight.getText());
            }
            if (isWiGE) {
                height = chDeployAirborne.isSelected()? 1 : 0;
            }
        } catch (NumberFormatException e) {
            msg = Messages.getString("CustomMechDialog.EnterValidSkills"); //$NON-NLS-1$
            title = Messages.getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
            JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (isAero) {
            if ((velocity > (2 * entities.get(0).getWalkMP()))
                    || (velocity < 0)) {
                msg = Messages
                        .getString("CustomMechDialog.EnterCorrectVelocity"); //$NON-NLS-1$
                title = Messages
                        .getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
                JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if ((altitude < 0) || (altitude > 10)) {
                msg = Messages
                        .getString("CustomMechDialog.EnterCorrectAltitude"); //$NON-NLS-1$
                title = Messages
                        .getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
                JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (isVTOL && (height > 50)
                || (isAirMech && height > 25)
                || (isGlider && height > 12)) {
            msg = Messages.getString("CustomMechDialog.EnterCorrectHeight"); //$NON-NLS-1$
            title = Messages.getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
            JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Apply single-entity settings
        if (entities.size() == 1) {
            Entity entity = entities.get(0);

            for (int i = 0; i < entities.get(0).getCrew().getSlotCount(); i++) {
                String name = panCrewMember[i].getPilotName();
                String nick = panCrewMember[i].getNickname();
                boolean missing = panCrewMember[i].getMissing();
                int gunnery;
                int gunneryL;
                int gunneryM;
                int gunneryB;
                int artillery;
                int piloting;
                int gunneryAero;
                int gunneryAeroL;
                int gunneryAeroM;
                int gunneryAeroB;
                int pilotingAero;
                int tough = 0;
                int backup = panCrewMember[i].getBackup();
                try {
                    gunnery = panCrewMember[i].getGunnery();
                    gunneryL = panCrewMember[i].getGunneryL();
                    gunneryM = panCrewMember[i].getGunneryM();
                    gunneryB = panCrewMember[i].getGunneryB();
                    piloting = panCrewMember[i].getPiloting();
                    gunneryAero = panCrewMember[i].getGunneryAero();
                    gunneryAeroL = panCrewMember[i].getGunneryAeroL();
                    gunneryAeroM = panCrewMember[i].getGunneryAeroM();
                    gunneryAeroB = panCrewMember[i].getGunneryAeroB();
                    pilotingAero = panCrewMember[i].getPilotingAero();
                    artillery = panCrewMember[i].getArtillery();
                    tough = panCrewMember[i].getToughness();
                } catch (NumberFormatException e) {
                    msg = Messages.getString("CustomMechDialog.EnterValidSkills"); //$NON-NLS-1$
                    title = Messages.getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
                    JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
        
                // keep these reasonable, please
                if ((gunnery < 0) || (gunnery > 8) || (piloting < 0) || (piloting > 8)
                        || (gunneryL < 0) || (gunneryL > 8) || (gunneryM < 0)
                        || (gunneryM > 8) || (gunneryB < 0) || (gunneryB > 8)
                        || (gunneryAero < 0) || (gunneryAero > 8) || (pilotingAero < 0) || (pilotingAero > 8)
                        || (gunneryAeroL < 0) || (gunneryAeroL > 8) || (gunneryAeroM < 0)
                        || (gunneryAeroM > 8) || (gunneryAeroB < 0) || (gunneryAeroB > 8)                        
                        || (artillery < 0) || (artillery > 8)) {
                    msg = Messages.getString("CustomMechDialog.EnterSkillsBetween0_8"); //$NON-NLS-1$
                    title = Messages.getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
                    JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (entity.getCrew() instanceof LAMPilot) {
                    LAMPilot pilot = (LAMPilot)entity.getCrew();
                    if (client.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
                        pilot.setGunneryMechL(gunneryL);
                        pilot.setGunneryMechB(gunneryB);
                        pilot.setGunneryMechM(gunneryM);
                        pilot.setGunneryMech((int)Math.round((gunneryL + gunneryB + gunneryM) / 3.0));
                        pilot.setGunneryAeroL(gunneryAeroL);
                        pilot.setGunneryAeroB(gunneryAeroB);
                        pilot.setGunneryAeroM(gunneryAeroM);
                        pilot.setGunneryAero((int)Math.round((gunneryAeroL + gunneryAeroB + gunneryAeroM) / 3.0));
                    } else {
                        pilot.setGunneryMechL(gunnery);
                        pilot.setGunneryMechB(gunnery);
                        pilot.setGunneryMechM(gunnery);
                        pilot.setGunneryMech(gunnery);
                        pilot.setGunneryAeroL(gunneryAero);
                        pilot.setGunneryAeroB(gunneryAero);
                        pilot.setGunneryAeroM(gunneryAero);
                        pilot.setGunneryAero(gunneryAero);
                    }
                    pilot.setPilotingMech(piloting);
                    pilot.setPilotingAero(pilotingAero);
                } else {
                    if (client.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
                        entity.getCrew().setGunneryL(gunneryL, i);
                        entity.getCrew().setGunneryB(gunneryB, i);
                        entity.getCrew().setGunneryM(gunneryM, i);
                        entity.getCrew().setGunnery((int)Math.round((gunneryL + gunneryB + gunneryM) / 3.0), i);
                    } else {
                        entity.getCrew().setGunnery(gunnery, i);
                        entity.getCrew().setGunneryL(gunnery, i);
                        entity.getCrew().setGunneryB(gunnery, i);
                        entity.getCrew().setGunneryM(gunnery, i);
                    }
                    entity.getCrew().setPiloting(piloting, i);
                }
                if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
                    entity.getCrew().setArtillery(artillery, i);
                } else {
                    entity.getCrew().setArtillery(entity.getCrew().getGunnery(i), i);
                }
                entity.getCrew().setMissing(missing, i);
                entity.getCrew().setToughness(tough, i);
                entity.getCrew().setName(name, i);
                entity.getCrew().setNickname(nick, i);
                entity.getCrew().setPortraitCategory(panCrewMember[i].getPortraitCategory(), i);
                entity.getCrew().setPortraitFileName(panCrewMember[i].getPortraitFilename(), i);
                if (backup >= 0) {
                    if (i == entity.getCrew().getCrewType().getPilotPos()) {
                        entity.getCrew().setBackupPilotPos(backup);
                    } else if (i == entity.getCrew().getCrewType().getGunnerPos()) {
                        entity.getCrew().setBackupGunnerPos(backup);
                    }
                }

                // If the player wants to swap unit numbers, update both
                // entities and send an update packet for the other entity.
                Entity other = panCrewMember[i].getEntityUnitNumSwap();
                if (null != other) {
                    short temp = entity.getUnitNumber();
                    entity.setUnitNumber(other.getUnitNumber());
                    other.setUnitNumber(temp);
                    client.sendUpdateEntity(other);
                }
            }
            entity.getCrew().setFatigue(fatigue);
            entity.getCrew().setInitBonus(init);
            entity.getCrew().setCommandBonus(command);

            // update commander status
            entity.setCommander(chCommander.isSelected());

            setOptions();
            setQuirks();
            setPartReps();
            m_equip.applyChoices();

            if (entity instanceof BattleArmor) {
                // have to reset internals because of dermal armor option
                if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_DERMAL_ARMOR)) {
                    ((BattleArmor) entity).setInternal(2);
                } else {
                    ((BattleArmor) entity).setInternal(1);
                }
            } else if (entity instanceof Infantry) {
                // need to reset armor on conventional infantry
                if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_DERMAL_ARMOR)) {
                    entity.initializeArmor(
                            entity.getOInternal(Infantry.LOC_INFANTRY),
                            Infantry.LOC_INFANTRY);
                } else {
                    entity.initializeArmor(0, Infantry.LOC_INFANTRY);
                }
            }
        }

        // Apply multiple-entity settings
        for (Entity entity : entities) {
            entity.setHidden(chHidden.isSelected());

            if (chOffBoard.isSelected()) {
                try {
                    offBoardDistance = distance;
                } catch (NumberFormatException e) {
                    msg = Messages
                            .getString("CustomMechDialog.EnterValidSkills"); //$NON-NLS-1$
                    title = Messages
                            .getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
                    JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (offBoardDistance < 17) {
                    msg = Messages
                            .getString("CustomMechDialog.OffboardDistance"); //$NON-NLS-1$
                    title = Messages
                            .getString("CustomMechDialog.NumberFormatError"); //$NON-NLS-1$
                    JOptionPane.showMessageDialog(clientgui.frame, msg, title,
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                entity.setOffBoard(offBoardDistance, OffBoardDirection
                        .getDirection(choOffBoardDirection.getSelectedIndex()));
            } else {
                entity.setOffBoard(0, OffBoardDirection.NONE);
            }

            if (isAero) {
                IAero a = (IAero) entity;
                a.setCurrentVelocity(velocity);
                a.setNextVelocity(velocity);
                // we need to determine whether this aero is airborne or not in
                // order for prohibited terrain and stacking to work right in
                // the
                // deployment phase this is very tricky because in atmosphere,
                // zero
                // altitude does not necessarily mean grounded
                if (altitude <= 0) {
                    a.land();
                } else {
                    a.liftOff(altitude);
                }
            }

            if (isVTOL || isWiGE || isAirMech || isGlider) {
                entity.setElevation(height);
            }
            
            //Set the entity's starting mode
            if (isQuadVee) {
                entity.setConversionMode(choStartingMode.getSelectedIndex());
            } else if (isLAM) {
                if (choStartingMode.getSelectedIndex() == 2) {
                    entity.setConversionMode(LandAirMech.CONV_MODE_FIGHTER);
                } else if (choStartingMode.getSelectedIndex() == 1) {
                    entity.setConversionMode(LandAirMech.CONV_MODE_FIGHTER);
                    entity.setConversionMode(((LandAirMech)entity).getLAMType() == LandAirMech.LAM_BIMODAL?
                            LandAirMech.CONV_MODE_FIGHTER : LandAirMech.CONV_MODE_AIRMECH);
                } else {
                    entity.setConversionMode(LandAirMech.CONV_MODE_MECH);
                }
            }

            // Set the entity's deployment position and round.
            entity.setStartingPos(choDeploymentZone.getSelectedIndex() - 1);
            entity.setDeployRound(choDeploymentRound.getSelectedIndex());

            // Should the entity begin the game shutdown?
            if (chDeployShutdown.isSelected()
                    && clientgui.getClient().getGame().getOptions()
                            .booleanOption(OptionsConstants.RPG_BEGIN_SHUTDOWN)) {
                entity.performManualShutdown();
            } else { // We need to else this in case someone turned the option
                     // on, set their units, and then turned the option off.
                entity.performManualStartup();
            }

            // LAMs in fighter mode or airborne AirMechs ignore the prone and hull down selections.
            if (!isLAM || (!isAero && entity.getElevation() == 0)) {
                // Should the entity begin the game prone?
                entity.setProne(chDeployProne.isSelected());
    
                // Should the entity begin the game prone?
                entity.setHullDown(chDeployHullDown.isSelected());
            }
        }

        okay = true;
        status = DONE;
        clientgui.chatlounge.refreshEntities();

        // Check validity of units after customization
        for (Entity entity : entities) {
            EntityVerifier verifier = EntityVerifier.getInstance(new MegaMekFile(
                    Configuration.unitsDir(), EntityVerifier.CONFIG_FILENAME).getFile());
            TestEntity testEntity = null;
            if (entity instanceof Mech) {
                testEntity = new TestMech((Mech) entity, verifier.mechOption,
                        null);
            } else if ((entity instanceof Tank)
                    && !(entity instanceof GunEmplacement)) {
                if (entity.isSupportVehicle()) {
                    testEntity = new TestSupportVehicle((Tank) entity,
                            verifier.tankOption, null);
                } else {
                    testEntity = new TestTank((Tank) entity,
                            verifier.tankOption, null);
                }
            } else if (entity.getEntityType() == Entity.ETYPE_AERO
                    && entity.getEntityType() != Entity.ETYPE_DROPSHIP
                    && entity.getEntityType() != Entity.ETYPE_SMALL_CRAFT
                    && entity.getEntityType() != Entity.ETYPE_FIGHTER_SQUADRON
                    && entity.getEntityType() != Entity.ETYPE_JUMPSHIP
                    && entity.getEntityType() != Entity.ETYPE_SPACE_STATION) {
                testEntity = new TestAero((Aero) entity, verifier.mechOption,
                        null);
            } else if (entity instanceof BattleArmor) {
                testEntity = new TestBattleArmor((BattleArmor) entity,
                        verifier.baOption, null);
            } else if (entity instanceof Infantry) {
                testEntity = new TestInfantry((Infantry) entity,
                        verifier.infOption, null);
            }
            int gameTL = TechConstants.getGameTechLevel(client.getGame(),
                    entity.isClan());
            if ((testEntity != null)
                    && !testEntity.correctEntity(new StringBuffer(), gameTL)) {
                entity.setDesignValid(false);
            } else {
                entity.setDesignValid(true);
            }
        }

        if (actionEvent.getSource().equals(butPrev)) {
            status = PREV;
        } else if (actionEvent.getSource().equals(butNext)) {
            status = NEXT;
        }
        setVisible(false);
    }

    public void itemStateChanged(ItemEvent itemEvent) {
        if (itemEvent.getSource().equals(choStartingMode)) {
            updateStartingModeOptions();
        }
        if (itemEvent.getSource().equals(chDeployProne)) {
            chDeployHullDown.setSelected(false);
            return;
        }
        if (itemEvent.getSource().equals(chDeployHullDown)) {
            chDeployProne.setSelected(false);
            return;
        }
        if (itemEvent.getSource().equals(choDeploymentRound)) {
            if (choDeploymentRound.getSelectedIndex() == 0) {
                choDeploymentZone.setEnabled(false);
                choDeploymentZone.setSelectedIndex(0);
            } else {
                choDeploymentZone.setEnabled(true);
            }
        }
    }

    private void updateStartingModeOptions() {
        final int index = choStartingMode.getSelectedIndex();
        if (entities.get(0) instanceof QuadVee) {
            labDeployProne.setEnabled(index == 0);
            chDeployProne.setEnabled(index == 0);
        } else if (entities.get(0) instanceof LandAirMech) {
            int mode = index;
            if (((LandAirMech)entities.get(0)).getLAMType() == LandAirMech.LAM_BIMODAL
                    && mode == LandAirMech.CONV_MODE_AIRMECH) {
                mode = LandAirMech.CONV_MODE_FIGHTER;
            }
            labDeployProne.setEnabled(mode < LandAirMech.CONV_MODE_FIGHTER);
            chDeployProne.setEnabled(mode < LandAirMech.CONV_MODE_FIGHTER);
            labDeployHullDown.setEnabled(mode == LandAirMech.CONV_MODE_MECH);
            chDeployHullDown.setEnabled(mode == LandAirMech.CONV_MODE_MECH);
            labStartHeight.setEnabled(mode == LandAirMech.CONV_MODE_AIRMECH);
            fldStartHeight.setEnabled(mode == LandAirMech.CONV_MODE_AIRMECH);
            labStartVelocity.setEnabled(mode == LandAirMech.CONV_MODE_FIGHTER);
            fldStartVelocity.setEnabled(mode == LandAirMech.CONV_MODE_FIGHTER);
            labStartAltitude.setEnabled(mode == LandAirMech.CONV_MODE_FIGHTER);
            fldStartAltitude.setEnabled(mode == LandAirMech.CONV_MODE_FIGHTER);
        }
    }

    public Entity getNextEntity(boolean forward) {
        IGame game = client.getGame();
        boolean bd = game.getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP); //$NON-NLS-1$
        boolean rbd = game.getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP); //$NON-NLS-1$
        IPlayer p = client.getLocalPlayer();

        Entity nextOne;
        Entity entity;
        if (forward) {
            entity = entities.get(entities.size() - 1);
            nextOne = game.getNextEntityFromList(entity);
        } else {
            entity = entities.get(0);
            nextOne = game.getPreviousEntityFromList(entity);
        }
        while ((nextOne != null) && !entities.contains(nextOne)) {
            if (nextOne.getOwner().equals(p)
                    || (!(bd || rbd) && nextOne.getOwner().equals(
                            entity.getOwner()))) {
                return nextOne;
            }
            if (forward) {
                nextOne = game.getNextEntityFromList(nextOne);
            } else {
                nextOne = game.getPreviousEntityFromList(nextOne);
            }
        }
        return null;
    }

    private void setupEquip() {
        Entity entity = entities.get(0);
        GridBagLayout gbl = new GridBagLayout();
        panEquip.setLayout(gbl);

        m_equip = new EquipChoicePanel(entity, clientgui, client);
        panEquip.add(m_equip, GBC.std());
    }
    
}
