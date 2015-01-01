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

import megamek.client.Client;
import megamek.client.ui.AWT.Messages;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.IOffBoardDirections;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.preference.PreferenceManager;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * A dialog that a player can use to customize his mech before battle.
 * Currently, changing pilots, setting up C3 networks, changing ammunition,
 * deploying artillery offboard, setting MGs to rapidfire, setting auto-eject
 * is supported.
 *
 * @author Ben
 */
public class CustomMechDialog
        extends ClientDialog implements ActionListener, DialogOptionListener {

    private JLabel labName = new JLabel(Messages.getString("CustomMechDialog.labName"), JLabel.RIGHT); //$NON-NLS-1$
    private JTextField fldName = new JTextField(20);
    private JLabel labGunnery = new JLabel(Messages.getString("CustomMechDialog.labGunnery"), JLabel.RIGHT); //$NON-NLS-1$
    private JTextField fldGunnery = new JTextField(3);
    private JLabel labPiloting = new JLabel(Messages.getString("CustomMechDialog.labPiloting"), JLabel.RIGHT); //$NON-NLS-1$
    private JTextField fldPiloting = new JTextField(3);
    private JLabel labC3 = new JLabel(Messages.getString("CustomMechDialog.labC3"), JLabel.RIGHT); //$NON-NLS-1$
    private JComboBox choC3 = new JComboBox();
    private int[] entityCorrespondance;
    private JLabel labCallsign = new JLabel(Messages.getString("CustomMechDialog.labCallsign"), JLabel.CENTER); //$NON-NLS-1$
    private JLabel labUnitNum = new JLabel(Messages.getString("CustomMechDialog.labUnitNum"), JLabel.CENTER); //$NON-NLS-1$
    private JComboBox choUnitNum = new JComboBox();
    private ArrayList entityUnitNum = new ArrayList();
    private JLabel labDeployment = new JLabel(Messages.getString("CustomMechDialog.labDeployment"), JLabel.RIGHT); //$NON-NLS-1$
    private JComboBox choDeployment = new JComboBox();
    private JLabel labAutoEject = new JLabel(Messages.getString("CustomMechDialog.labAutoEject"), JLabel.RIGHT); //$NON-NLS-1$
    private JCheckBox chAutoEject = new JCheckBox();
    private JLabel labSearchlight = new JLabel(Messages.getString("CustomMechDialog.labSearchlight"), JLabel.RIGHT); //$NON-NLS-1$
    private JCheckBox chSearchlight = new JCheckBox();

    private JLabel labOffBoard = new JLabel(Messages.getString("CustomMechDialog.labOffBoard"), JLabel.RIGHT); //$NON-NLS-1$
    private JCheckBox chOffBoard = new JCheckBox();
    private JLabel labOffBoardDirection = new JLabel(Messages.getString("CustomMechDialog.labOffBoardDirection"), JLabel.RIGHT); //$NON-NLS-1$
    private JComboBox choOffBoardDirection = new JComboBox();
    private JLabel labOffBoardDistance = new JLabel(Messages.getString("CustomMechDialog.labOffBoardDistance"), JLabel.RIGHT); //$NON-NLS-1$
    private JTextField fldOffBoardDistance = new JTextField(4);
    private JButton butOffBoardDistance = new JButton("0");

    private JLabel labTargSys = new JLabel(Messages.getString("CustomMechDialog.labTargSys"), JLabel.RIGHT);
    private JComboBox choTargSys = new JComboBox();

    private JPanel panButtons = new JPanel();
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
    private JButton butNext = new JButton(Messages.getString("Next"));
    private JButton butPrev = new JButton(Messages.getString("Previous"));

    private ArrayList m_vMunitions = new ArrayList();
    private JPanel panMunitions = new JPanel();
    private ArrayList m_vMGs = new ArrayList();
    private JPanel panRapidfireMGs = new JPanel();
    private ArrayList m_vMines = new ArrayList();
    private JPanel panMines = new JPanel();

    private Entity entity;
    private boolean okay;
    private ClientGUI clientgui;
    private Client client;

    private PilotOptions options;

    private ArrayList optionComps = new ArrayList();

    private JPanel panOptions = new JPanel();
    private JScrollPane scrOptions;

    private JScrollPane scrAll;

    private JTextArea texDesc = new JTextArea(Messages.getString("CustomMechDialog.texDesc"), 3, 35); //$NON-NLS-1$

    private boolean editable;

    private int direction = -1;
    private int distance = 17;

    /**
     * Creates new CustomMechDialog
     */
    public CustomMechDialog(ClientGUI clientgui, Client client, Entity entity, boolean editable) {
        super(clientgui.frame, Messages.getString("CustomMechDialog.title"), true); //$NON-NLS-1$

        JPanel tempPanel = new JPanel();
        this.entity = entity;
        this.clientgui = clientgui;
        this.client = client;
        options = entity.getCrew().getOptions();
        this.editable = editable;

        texDesc.setEditable(false);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        tempPanel.setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(5, 5, 5, 5);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labName, c);
        tempPanel.add(labName);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldName, c);
        tempPanel.add(fldName);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labGunnery, c);
        tempPanel.add(labGunnery);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldGunnery, c);
        tempPanel.add(fldGunnery);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labPiloting, c);
        tempPanel.add(labPiloting);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldPiloting, c);
        tempPanel.add(fldPiloting);
        
        // Auto-eject checkbox.
        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;
            // Torso-mounted cockpits can't eject, so lets not bother showing this.
            if (mech.getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED) {
                c.gridwidth = 1;
                c.anchor = GridBagConstraints.EAST;
                gridbag.setConstraints(labAutoEject, c);
                tempPanel.add(labAutoEject);

                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(chAutoEject, c);
                tempPanel.add(chAutoEject);
                chAutoEject.setSelected(!mech.isAutoEject());
            }
        }

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labDeployment, c);
        tempPanel.add(labDeployment);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choDeployment, c);
        tempPanel.add(choDeployment);
        refreshDeployment();

        if (clientgui.getClient().game.getOptions().booleanOption("pilot_advantages")) { //$NON-NLS-1$
            scrOptions = new JScrollPane(panOptions);

            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridbag.setConstraints(scrOptions, c);
            tempPanel.add(scrOptions);

            c.weightx = 1.0;
            c.weighty = 0.0;
            gridbag.setConstraints(texDesc, c);
            tempPanel.add(new JScrollPane(texDesc));
        }

        if (entity.hasC3() || entity.hasC3i()) {
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labC3, c);
            tempPanel.add(labC3);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(choC3, c);
            tempPanel.add(choC3);
            refreshC3();
        }
        boolean eligibleForOffBoard = false;
        for (Mounted mounted : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                eligibleForOffBoard = true;
            }
        }
        if (eligibleForOffBoard) {
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoard, c);
            tempPanel.add(labOffBoard);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(chOffBoard, c);
            tempPanel.add(chOffBoard);
            chOffBoard.setSelected(entity.isOffBoard());

            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoardDirection, c);
            tempPanel.add(labOffBoardDirection);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(choOffBoardDirection, c);
            choOffBoardDirection.addItem(Messages.getString("CustomMechDialog.North")); //$NON-NLS-1$
            choOffBoardDirection.addItem(Messages.getString("CustomMechDialog.South")); //$NON-NLS-1$
            choOffBoardDirection.addItem(Messages.getString("CustomMechDialog.East")); //$NON-NLS-1$
            choOffBoardDirection.addItem(Messages.getString("CustomMechDialog.West")); //$NON-NLS-1$
            direction = entity.getOffBoardDirection();
            if (IOffBoardDirections.NONE == direction) {
                direction = IOffBoardDirections.NORTH;
            }
            choOffBoardDirection.setSelectedIndex(direction);
            tempPanel.add(choOffBoardDirection);

            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labOffBoardDistance, c);
            tempPanel.add(labOffBoardDistance);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;

            butOffBoardDistance.addActionListener(this);
            gridbag.setConstraints(butOffBoardDistance, c);
            butOffBoardDistance.setText(Integer.toString(distance));
            tempPanel.add(butOffBoardDistance);
        }

        if (!(entity.hasTargComp()) && (clientgui.getClient().game.getOptions().booleanOption("allow_level_3_targsys")) && (entity instanceof Mech)) {
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labTargSys, c);
            tempPanel.add(labTargSys);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            choTargSys.addItem(MiscType.getTargetSysName(MiscType.T_TARGSYS_STANDARD));
            choTargSys.addItem(MiscType.getTargetSysName(MiscType.T_TARGSYS_LONGRANGE));
            choTargSys.addItem(MiscType.getTargetSysName(MiscType.T_TARGSYS_SHORTRANGE));
            choTargSys.addItem(MiscType.getTargetSysName(MiscType.T_TARGSYS_ANTI_AIR));
//            choTargSys.add(MiscType.getTargetSysName(MiscType.T_TARGSYS_MULTI_TRAC));
            gridbag.setConstraints(choTargSys, c);
            tempPanel.add(choTargSys);

            choTargSys.setSelectedItem(MiscType.getTargetSysName(entity.getTargSysType()));
        }

        if (entity instanceof Protomech) {
            // All Protomechs have a callsign.
            StringBuffer callsign = new StringBuffer(Messages.getString("CustomMechDialog.Callsign")); //$NON-NLS-1$
            callsign.append(": ");  //$NON-NLS-1$
            callsign.append((char) (this.entity.getUnitNumber() +
                    PreferenceManager.getClientPreferences().getUnitStartChar()))
                    .append('-')
                    .append(this.entity.getId());
            labCallsign.setText(callsign.toString());
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(labCallsign, c);
            tempPanel.add(labCallsign);

            // Get the Protomechs of this entity's player
            // that *aren't* in the entity's unit.
            Enumeration otherUnitEntities = client.game.getSelectedEntities
                    (new EntitySelector() {
                        private final int ownerId =
                                CustomMechDialog.this.entity.getOwnerId();
                        private final char unitNumber =
                                CustomMechDialog.this.entity.getUnitNumber();

                        public boolean accept(Entity entity) {
                            if (entity instanceof Protomech &&
                                    ownerId == entity.getOwnerId() &&
                                    unitNumber != entity.getUnitNumber())
                                return true;
                            return false;
                        }
                    });

            // If we got any other entites, show the unit number controls.
            if (otherUnitEntities.hasMoreElements()) {
                c.gridwidth = 1;
                c.anchor = GridBagConstraints.EAST;
                gridbag.setConstraints(labUnitNum, c);
                tempPanel.add(labUnitNum);

                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(choUnitNum, c);
                tempPanel.add(choUnitNum);
                refreshUnitNum(otherUnitEntities);
            }
        }

        // Can't set up munitions on infantry.
        if (!(entity instanceof Infantry)) {
            setupMunitions();
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(panMunitions, c);
            tempPanel.add(panMunitions);
        }
        
        // Set up rapidfire mg
        if (clientgui.getClient().game.getOptions().booleanOption("maxtech_burst")) { //$NON-NLS-1$
            c.gridwidth = 1;
            setupRapidfireMGs();
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(panRapidfireMGs, c);
            tempPanel.add(panRapidfireMGs);
        }
        
        // Set up searchlight
        if (clientgui.getClient().game.getOptions().booleanOption("night_battle")) { //$NON-NLS-1$
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(labSearchlight, c);
            tempPanel.add(labSearchlight);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(chSearchlight, c);
            tempPanel.add(chSearchlight);
            chSearchlight.setSelected(entity.hasSpotlight());
        }

        // Set up mines
        setupMines();
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panMines, c);
        tempPanel.add(panMines);

        setupButtons();

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panButtons, c);
        tempPanel.add(panButtons);

        fldName.setText(entity.getCrew().getName());
        fldName.addActionListener(this);
        fldGunnery.setText(Integer.toString(entity.getCrew().getGunnery()));
        fldGunnery.addActionListener(this);
        fldPiloting.setText(Integer.toString(entity.getCrew().getPiloting()));
        fldPiloting.addActionListener(this);

        if (!editable) {
            fldName.setEnabled(false);
            fldGunnery.setEnabled(false);
            fldPiloting.setEnabled(false);
            choC3.setEnabled(false);
            choDeployment.setEnabled(false);
            chAutoEject.setEnabled(false);
            chSearchlight.setEnabled(false);
            choTargSys.setEnabled(false);
            disableMunitionEditing();
            disableMGSetting();
            disableMineSetting();
            chOffBoard.setEnabled(false);
            choOffBoardDirection.setEnabled(false);
            fldOffBoardDistance.setEnabled(false);
        }
        scrAll = new JScrollPane(tempPanel);

        // add the scrollable panel
        getContentPane().add(scrAll);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();

        //Why do we have to add all this stuff together to get the
        // right size?  I hate GUI programming...especially AWT.
        int w = tempPanel.getPreferredSize().width +
                scrAll.getInsets().right;
        int h = tempPanel.getPreferredSize().height +
                panButtons.getPreferredSize().height +
                scrAll.getInsets().bottom;
        setLocationAndSize(w, h);
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

    private void setupRapidfireMGs() {
        GridBagLayout gbl = new GridBagLayout();
        panRapidfireMGs.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (!wtype.hasFlag(WeaponType.F_MG)) {
                continue;
            }
            gbc.gridy = row++;
            RapidfireMGPanel rmp = new RapidfireMGPanel(m);
            gbl.setConstraints(rmp, gbc);
            panRapidfireMGs.add(rmp);
            m_vMGs.add(rmp);
        }
    }

    private void setupMines() {
        GridBagLayout gbl = new GridBagLayout();
        panMines.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getMisc()) {
            if (!m.getType().hasFlag((MiscType.F_MINE))) {
                continue;
            }

            gbc.gridy = row++;
            MineChoicePanel mcp = new MineChoicePanel(m);
            gbl.setConstraints(mcp, gbc);
            panMines.add(mcp);
            m_vMines.add(mcp);
        }
    }

    private void setupMunitions() {

        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getAmmo()) {
            AmmoType at = (AmmoType) m.getType();
            ArrayList vTypes = new ArrayList();
            Vector vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
            if (vAllTypes == null) {
                continue;
            }

            for (int x = 0, n = vAllTypes.size(); x < n; x++) {
                AmmoType atCheck = (AmmoType) vAllTypes.elementAt(x);
                boolean bTechMatch = TechConstants.isLegal(entity.getTechLevel(), atCheck.getTechLevel());
                                
                // allow all lvl2 IS units to use level 1 ammo
                // lvl1 IS units don't need to be allowed to use lvl1 ammo,
                // because there is no special lvl1 ammo, therefore it doesn't
                // need to show up in this display.
                if (!bTechMatch && entity.getTechLevel() == TechConstants.T_IS_LEVEL_2 &&
                        atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_1) {
                    bTechMatch = true;
                }
                
                // if is_eq_limits is unchecked allow l1 guys to use l2 stuff
                if (!clientgui.getClient().game.getOptions().booleanOption("is_eq_limits") //$NON-NLS-1$
                        && entity.getTechLevel() == TechConstants.T_IS_LEVEL_1
                        && atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_2) {
                    bTechMatch = true;
                }

                // Possibly allow level 3 ammos, possibly not.
                if (clientgui.getClient().game.getOptions().booleanOption("allow_level_3_ammo")) {
                    if (!clientgui.getClient().game.getOptions().booleanOption("is_eq_limits")) {
                        if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2
                                && atCheck.getTechLevel() == TechConstants.T_CLAN_LEVEL_3) {
                            bTechMatch = true;
                        }
                        if (((entity.getTechLevel() == TechConstants.T_IS_LEVEL_1) || (entity.getTechLevel() == TechConstants.T_IS_LEVEL_2))
                                && (atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_3)) {
                            bTechMatch = true;
                        }
                    }
                } else if ((atCheck.getTechLevel() == TechConstants.T_IS_LEVEL_3) || (atCheck.getTechLevel() == TechConstants.T_CLAN_LEVEL_3)) {
                    bTechMatch = false;
                }
                
                //allow mixed Tech Mechs to use both IS and Clan ammo of any
                // level (since mixed tech is always level 3)
                if (entity.isMixedTech()) {
                    bTechMatch = true;
                }

                // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                //      to be combined to other munition types.
                long muniType = atCheck.getMunitionType();
                muniType &= ~AmmoType.M_INCENDIARY_LRM;
                if (!clientgui.getClient().game.getOptions().booleanOption("clan_ignore_eq_limits") //$NON-NLS-1$
                        && entity.isClan()
                        && (muniType == AmmoType.M_SEMIGUIDED || 
                        muniType == AmmoType.M_SWARM_I ||
                        muniType == AmmoType.M_FLARE ||
                        muniType == AmmoType.M_FRAGMENTATION ||
                        muniType == AmmoType.M_THUNDER_AUGMENTED ||
                        muniType == AmmoType.M_THUNDER_INFERNO ||
                        muniType == AmmoType.M_THUNDER_VIBRABOMB ||
                        muniType == AmmoType.M_THUNDER_ACTIVE ||
                        muniType == AmmoType.M_INFERNO_IV ||
                        muniType == AmmoType.M_VIBRABOMB_IV ||
                        muniType == AmmoType.M_LISTEN_KILL ||
                        muniType == AmmoType.M_ANTI_TSM)) {
                    bTechMatch = false;
                }

                if (!clientgui.getClient().game.getOptions().booleanOption("minefields") && //$NON-NLS-1$
                        AmmoType.canDeliverMinefield(atCheck)) {
                    continue;
                }

                // Only Protos can use Proto-specific ammo
                if (atCheck.hasFlag(AmmoType.F_PROTOMECH) &&
                        !(entity instanceof Protomech)) {
                    continue;
                }

                // When dealing with machine guns, Protos can only
                //  use proto-specific machine gun ammo
                if (entity instanceof Protomech &&
                        atCheck.hasFlag(AmmoType.F_MG) &&
                        !atCheck.hasFlag(AmmoType.F_PROTOMECH)) {
                    continue;
                }

                // Battle Armor ammo can't be selected at all.
                // All other ammo types need to match on rack size and tech.
                if (bTechMatch &&
                        atCheck.getRackSize() == at.getRackSize() &&
                        !atCheck.hasFlag(AmmoType.F_BATTLEARMOR) &&
                        atCheck.getTonnage(entity) == at.getTonnage(entity)) {
                    vTypes.add(atCheck);
                }
            }
            if (vTypes.size() < 2 && !client.game.getOptions().booleanOption("lobby_ammo_dump")) { //$NON-NLS-1$
                continue;
            }

            gbc.gridy = row++;
            // Protomechs need special choice panels.
            MunitionChoicePanel mcp;
            if (entity instanceof Protomech) {
                mcp = new ProtomechMunitionChoicePanel(m, vTypes);
            } else {
                mcp = new MunitionChoicePanel(m, vTypes);
            }
            gbl.setConstraints(mcp, gbc);
            panMunitions.add(mcp);
            m_vMunitions.add(mcp);
        }
    }

    class MineChoicePanel extends JPanel {
        private JComboBox m_choice;
        private Mounted m_mounted;

        MineChoicePanel(Mounted m) {
            m_mounted = m;
            m_choice = new JComboBox();
            m_choice.addItem(Messages.getString("CustomMechDialog.Conventional")); //$NON-NLS-1$
            m_choice.addItem(Messages.getString("CustomMechDialog.Vibrabomb")); //$NON-NLS-1$
            //m_choice.add("Messages.getString("CustomMechDialog.Command-detonated")); //$NON-NLS-1$
            int loc;
            loc = m.getLocation();
            String sDesc = '(' + entity.getLocationAbbr(loc) + ')'; //$NON-NLS-1$ //$NON-NLS-2$
            JLabel lLoc = new JLabel(sDesc);
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(lLoc, c);
            add(lLoc);
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(m_choice, c);
            m_choice.setSelectedIndex(m.getMineType());
            add(m_choice);
        }

        public void applyChoice() {
            m_mounted.setMineType(m_choice.getSelectedIndex());
        }

        public void setEnabled(boolean enabled) {
            m_choice.setEnabled(enabled);
        }
    }

    class MunitionChoicePanel extends JPanel {
        private ArrayList m_vTypes;
        private JComboBox m_choice;
        private Mounted m_mounted;

        JLabel labDump = new JLabel(Messages.getString("CustomMechDialog.labDump")); //$NON-NLS-1$
        JCheckBox chDump = new JCheckBox();
        JLabel labHotLoad= new JLabel(Messages.getString("CustomMechDialog.switchToHotLoading")); //$NON-NLS-1$
        JCheckBox chHotLoad = new JCheckBox();

        MunitionChoicePanel(Mounted m, ArrayList vTypes) {
            m_vTypes = vTypes;
            m_mounted = m;
            AmmoType curType = (AmmoType) m.getType();
            m_choice = new JComboBox();
            Iterator e = m_vTypes.iterator();
            for (int x = 0; e.hasNext(); x++) {
                AmmoType at = (AmmoType) e.next();
                m_choice.addItem(at.getName());
                if (at.getMunitionType() == curType.getMunitionType()) {
                    m_choice.setSelectedIndex(x);
                }
            }
            int loc;
            if (m.getLocation() == Entity.LOC_NONE) {
                // oneshot weapons don't have a location of their own
                Mounted linkedBy = m.getLinkedBy();
                loc = linkedBy.getLocation();
            } else {
                loc = m.getLocation();
            }
            String sDesc = '(' + entity.getLocationAbbr(loc) + ')'; //$NON-NLS-1$ //$NON-NLS-2$
            JLabel lLoc = new JLabel(sDesc);
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(lLoc, c);
            add(lLoc);
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(m_choice, c);
            add(m_choice);
            if (clientgui.getClient().game.getOptions().booleanOption("lobby_ammo_dump") ) { //$NON-NLS-1$
                c.gridx = 0;
                c.gridy = 1;
                c.anchor = GridBagConstraints.EAST;
                g.setConstraints(labDump, c);
                add(labDump);
                c.gridx = 1;
                c.gridy = 1;
                c.anchor = GridBagConstraints.WEST;
                g.setConstraints(chDump, c);
                add(chDump);
                if (clientgui.getClient().game.getOptions().booleanOption("maxtech_hotload") 
                        && curType.hasFlag(AmmoType.F_HOTLOAD) ) { //$NON-NLS-1$
                    c.gridx = 0;
                    c.gridy = 2;
                    c.anchor = GridBagConstraints.EAST;
                    g.setConstraints(labHotLoad, c);
                    add(labHotLoad);
                    c.gridx = 1;
                    c.gridy = 2;
                    c.anchor = GridBagConstraints.WEST;
                    g.setConstraints(chHotLoad, c);
                    add(chHotLoad);
                }
            }else if (clientgui.getClient().game.getOptions().booleanOption("maxtech_hotload") 
                        && curType.hasFlag(AmmoType.F_HOTLOAD) ) { //$NON-NLS-1$
                    c.gridx = 0;
                    c.gridy = 1;
                    c.anchor = GridBagConstraints.EAST;
                    g.setConstraints(labHotLoad, c);
                    add(labHotLoad);
                    c.gridx = 1;
                    c.gridy = 1;
                    c.anchor = GridBagConstraints.WEST;
                    g.setConstraints(chHotLoad, c);
                    add(chHotLoad);
                }
        }

        public void applyChoice() {
            int n = m_choice.getSelectedIndex();
            AmmoType at = (AmmoType) m_vTypes.get(n);
            m_mounted.changeAmmoType(at);
            if (chDump.isSelected()) {
                m_mounted.setShotsLeft(0);
            }
            if ( clientgui.getClient().game.getOptions().booleanOption("maxtech_hotload") ){
                if ( chHotLoad.isSelected() != m_mounted.isHotLoaded() )
                    m_mounted.setHotLoad(chHotLoad.isSelected());
            }
        }

        public void setEnabled(boolean enabled) {
            m_choice.setEnabled(enabled);
        }

        /**
         * Get the number of shots in the mount.
         *
         * @return the <code>int</code> number of shots in the mount.
         */
        /* package */
        int getShotsLeft() {
            return m_mounted.getShotsLeft();
        }

        /**
         * Set the number of shots in the mount.
         *
         * @param shots the <code>int</code> number of shots for the mount.
         */
        /* package */
        void setShotsLeft(int shots) {
            m_mounted.setShotsLeft(shots);
        }
    }

    /**
     * When a Protomech selects ammo, you need to adjust the shots on the
     * unit for the weight of the selected munition.
     */
    class ProtomechMunitionChoicePanel extends MunitionChoicePanel {
        private final float m_origShotsLeft;
        private final AmmoType m_origAmmo;

        ProtomechMunitionChoicePanel(Mounted m, ArrayList vTypes) {
            super(m, vTypes);
            m_origAmmo = (AmmoType) m.getType();
            m_origShotsLeft = m.getShotsLeft();
        }

        /**
         * All ammo must be applied in ratios to the starting load.
         */
        public void applyChoice() {
            super.applyChoice();

            // Calculate the number of shots for the new ammo.
            // N.B. Some special ammos are twice as heavy as normal
            // so they have half the number of shots (rounded down).
            setShotsLeft(Math.round(getShotsLeft() * m_origShotsLeft /
                    m_origAmmo.getShots()));
            if (chDump.isSelected()) {
                setShotsLeft(0);
            }
        }
    }

    class RapidfireMGPanel extends JPanel {
        private Mounted m_mounted;

        JCheckBox chRapid = new JCheckBox();

        RapidfireMGPanel(Mounted m) {
            m_mounted = m;
            int loc = m.getLocation();
            String sDesc = Messages.getString("CustomMechDialog.switchToRapidFire", new Object[]{entity.getLocationAbbr(loc)}); //$NON-NLS-1$
            JLabel labRapid = new JLabel(sDesc);
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(labRapid, c);
            add(labRapid);
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(chRapid, c);
            chRapid.setSelected(m.isRapidfire());
            add(chRapid);
        }

        public void applyChoice() {
            boolean b = chRapid.isSelected();
            m_mounted.setRapidfire(b);
        }

        public void setEnabled(boolean enabled) {
            chRapid.setEnabled(enabled);
        }
    }

    private void disableMunitionEditing() {
        for (int i = 0; i < m_vMunitions.size(); i++) {
            ((MunitionChoicePanel) m_vMunitions.get(i)).setEnabled(false);
        }
    }

    private void disableMGSetting() {
        for (int i = 0; i < m_vMGs.size(); i++) {
            ((RapidfireMGPanel) m_vMGs.get(i)).setEnabled(false);
        }
    }

    private void disableMineSetting() {
        for (int i = 0; i < m_vMines.size(); i++) {
            ((MineChoicePanel) m_vMines.get(i)).setEnabled(false);
        }
    }

    private void setOptions() {
        IOption option;
        for (final Object newVar : optionComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue() == Messages.getString("CustomMechDialog.None"))) { //NON-NLS-$1
                entity.getCrew().getOptions().getOption(option.getName()).setValue("None"); //NON-NLS-$1
            } else
                entity.getCrew().getOptions().getOption(option.getName()).setValue(comp.getValue());
        }
    }

    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new ArrayList();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

        for (Enumeration i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = (IOptionGroup) i.nextElement();

            addGroup(group, gridbag, c);

            for (Enumeration j = group.getOptions(); j.hasMoreElements();) {
                IOption option = (IOption) j.nextElement();

                addOption(option, gridbag, c, editable);
            }
        }

        validate();
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridbag, GridBagConstraints c) {
        JLabel groupLabel = new JLabel(group.getDisplayableName());

        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridbag, GridBagConstraints c, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, editable);

        if ("weapon_specialist".equals(option.getName())) { //$NON-NLS-1$
            optionComp.addValue(Messages.getString("CustomMechDialog.None")); //$NON-NLS-1$
            HashMap uniqueWeapons = new HashMap();
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                Mounted m = entity.getWeaponList().get(i);
                uniqueWeapons.put(m.getName(), Boolean.valueOf(true));
            }
            for (final Object newVar : uniqueWeapons.keySet()) {
                optionComp.addValue((String) newVar);
            }
            optionComp.setSelected(option.stringValue());
        }

        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);

        optionComps.add(optionComp);
    }

    public void showDescFor(IOption option) {
        texDesc.setText(option.getDescription());
    }

    // TODO : implement me!!!
    public void optionClicked(DialogOptionComponent comp,
                              IOption option, boolean state) {
    }

    public boolean isOkay() {
        return okay;
    }

    private void refreshDeployment() {
        choDeployment.removeAll();
        choDeployment.addItem(Messages.getString("CustomMechDialog.StartOfGame")); //$NON-NLS-1$

        if (entity.getDeployRound() < 1)
            choDeployment.setSelectedIndex(0);

        for (int i = 1; i <= 15; i++) {
            choDeployment.addItem(Messages.getString("CustomMechDialog.AfterRound") + i); //$NON-NLS-1$

            if (entity.getDeployRound() == i)
                choDeployment.setSelectedIndex(i);
        }
    }

    private void refreshC3() {
        choC3.removeAll();
        int listIndex = 0;
        entityCorrespondance = new int[client.game.getNoOfEntities() + 2];

        if (entity.hasC3i()) {
            choC3.addItem(Messages.getString("CustomMechDialog.CreateNewNetwork")); //$NON-NLS-1$
            if (entity.getC3Master() == null) choC3.setSelectedIndex(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();
        } else if (entity.hasC3MM()) {
            int mNodes = entity.calculateFreeC3MNodes();
            int sNodes = entity.calculateFreeC3Nodes();

            choC3.addItem(Messages.getString("CustomMechDialog.setCompanyMaster", new Object[]{new Integer(mNodes), new Integer(sNodes)})); //$NON-NLS-1$

            if (entity.C3MasterIs(entity)) choC3.setSelectedIndex(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.addItem(Messages.getString("CustomMechDialog.setIndependentMaster", new Object[]{new Integer(sNodes)})); //$NON-NLS-1$
            if (entity.getC3Master() == null) choC3.setSelectedIndex(listIndex);
            entityCorrespondance[listIndex++] = -1;

        } else if (entity.hasC3M()) {
            int nodes = entity.calculateFreeC3Nodes();

            choC3.addItem(Messages.getString("CustomMechDialog.setCompanyMaster1", new Object[]{new Integer(nodes)})); //$NON-NLS-1$
            if (entity.C3MasterIs(entity)) choC3.setSelectedIndex(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.addItem(Messages.getString("CustomMechDialog.setIndependentMaster", new Object[]{new Integer(nodes)})); //$NON-NLS-1$
            if (entity.getC3Master() == null) choC3.setSelectedIndex(listIndex);
            entityCorrespondance[listIndex++] = -1;

        }
        for (Enumeration i = client.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity) i.nextElement();
            // ignore enemies or self
            if (entity.isEnemyOf(e) || entity.equals(e)) {
                continue;
            }
            // c3i only links with c3i
            if (entity.hasC3i() != e.hasC3i()) {
                continue;
            }
            int nodes = e.calculateFreeC3Nodes();
            if (e.hasC3MM() && entity.hasC3M() && e.C3MasterIs(e)) {
                nodes = e.calculateFreeC3MNodes();
            }
            if (entity.C3MasterIs(e) && !entity.equals(e)) {
                nodes++;
            }
            if (entity.hasC3i() && (entity.onSameC3NetworkAs(e) || entity.equals(e))) {
                nodes++;
            }
            if (nodes == 0) {
                continue;
            }
            if (e.hasC3i()) {
                if (entity.onSameC3NetworkAs(e)) {
                    choC3.addItem(Messages.getString("CustomMechDialog.join1", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes - 1)})); //$NON-NLS-1$
                    choC3.setSelectedIndex(listIndex);
                } else {
                    choC3.addItem(Messages.getString("CustomMechDialog.join2", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes)})); //$NON-NLS-1$
                }
                entityCorrespondance[listIndex++] = e.getId();
            } else if (e.C3MasterIs(e) && e.hasC3MM()) {
                // Company masters with 2 computers can have
                // *both* sub-masters AND slave units.
                choC3.addItem(Messages.getString("CustomMechDialog.connect2", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes)})); //$NON-NLS-1$
                entityCorrespondance[listIndex] = e.getId();
                if (entity.C3MasterIs(e)) {
                    choC3.setSelectedIndex(listIndex);
                }
                listIndex++;
            } else if (e.C3MasterIs(e) != entity.hasC3M()) {
                // If we're a slave-unit, we can only connect to sub-masters,
                // not main masters likewise, if we're a master unit, we can
                // only connect to main master units, not sub-masters.
            } else if (entity.C3MasterIs(e)) {
                choC3.addItem(Messages.getString("CustomMechDialog.connect1", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes - 1)})); //$NON-NLS-1$
                choC3.setSelectedIndex(listIndex);
                entityCorrespondance[listIndex++] = e.getId();
            } else {
                choC3.addItem(Messages.getString("CustomMechDialog.connect2", new Object[]{e.getDisplayName(), e.getC3NetId(), new Integer(nodes)})); //$NON-NLS-1$
                entityCorrespondance[listIndex++] = e.getId();
            }
        }
    }

    /**
     * Populate the list of entities in other units from the given enumeration.
     *
     * @param others the <code>Enumeration</code> containing entities in
     *               other units.
     */
    private void refreshUnitNum(Enumeration others) {
        // Clear the list of old values
        choUnitNum.removeAll();
        entityUnitNum.clear();

        // Make an entry for "no change".
        choUnitNum.addItem(Messages.getString("CustomMechDialog.doNotSwapUnits")); //$NON-NLS-1$
        entityUnitNum.add(entity);

        // Walk through the other entities.
        while (others.hasMoreElements()) {
            // Track the position of the next other entity.
            final Entity other = (Entity) others.nextElement();
            entityUnitNum.add(other);

            // Show the other entity's name and callsign.
            StringBuffer callsign = new StringBuffer(other.getDisplayName());
            callsign.append(" (")//$NON-NLS-1$
                    .append((char) (other.getUnitNumber() +
                    PreferenceManager.getClientPreferences().getUnitStartChar()))
                    .append('-')
                    .append(other.getId())
                    .append(')');
            choUnitNum.addItem(callsign.toString());
        }
        choUnitNum.setSelectedIndex(0);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(butOffBoardDistance)) {
            int maxDistance = 19 * 17; // Long Tom
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
            Slider sl = new Slider(clientgui.frame, Messages.getString("CustomMechDialog.offboardDistanceTitle"), Messages.getString("CustomMechDialog.offboardDistanceQuestion"),
                    entity.getOffBoardDistance(), 17, maxDistance);
            if (!sl.showDialog()) return;
            distance = sl.getValue();
            butOffBoardDistance.setText(Integer.toString(distance));
            // butOffBoardDistance = new JButton (Integer.toString(sl.getValue()));
            // butOffBoardDistance.addActionListener(this);
            return;
        }
        if (!actionEvent.getSource().equals(butCancel)) {
            // get values
            String name = fldName.getText();
            int gunnery;
            int piloting;
            int offBoardDistance;
            boolean autoEject = chAutoEject.isSelected();
            try {
                gunnery = Integer.parseInt(fldGunnery.getText());
                piloting = Integer.parseInt(fldPiloting.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("CustomMechDialog.EnterValidSkills"), Messages.getString("CustomMechDialog.NumberFormatError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            
            // keep these reasonable, please
            if (gunnery < 0 || gunnery > 7 || piloting < 0 || piloting > 7) {
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("CustomMechDialog.EnterSkillsBetween0_7"), Messages.getString("CustomMechDialog.NumberFormatError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            if (chOffBoard.isSelected()) {
                try {
                    offBoardDistance = distance;
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("CustomMechDialog.EnterValidSkills"), Messages.getString("CustomMechDialog.NumberFormatError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
                if (offBoardDistance < 17) {
                    JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("CustomMechDialog.OffboardDistance"), Messages.getString("CustomMechDialog.NumberFormatError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
                entity.setOffBoard(offBoardDistance,
                        choOffBoardDirection.getSelectedIndex());
            } else {
                entity.setOffBoard(0, Entity.NONE);
            }

            // change entity
            entity.setCrew(new Pilot(name, gunnery, piloting));
            if (entity instanceof Mech) {
                Mech mech = (Mech) entity;
                mech.setAutoEject(!autoEject);
            }
            if (entity.hasC3() && choC3.getSelectedIndex() > -1) {
                Entity chosen = client.getEntity
                        (entityCorrespondance[choC3.getSelectedIndex()]);
                int entC3nodeCount =
                        client.game.getC3SubNetworkMembers(entity).size();
                int choC3nodeCount =
                        client.game.getC3NetworkMembers(chosen).size();
                if (entC3nodeCount + choC3nodeCount <= Entity.MAX_C3_NODES) {
                    entity.setC3Master(chosen);
                } else {
                    String message = Messages.getString("CustomMechDialog.NetworkTooBig.message", new Object[]{//$NON-NLS-1$
                        entity.getShortName(), chosen.getShortName(),
                        new Integer(entC3nodeCount), new Integer(choC3nodeCount),
                        new Integer(Entity.MAX_C3_NODES)});
                    clientgui.doAlertDialog(Messages.getString("CustomMechDialog.NetworkTooBig.title"), //$NON-NLS-1$
                            message);
                    refreshC3();
                }
            } else if (entity.hasC3i() && choC3.getSelectedIndex() > -1) {
                entity.setC3NetId(client.getEntity(entityCorrespondance[choC3.getSelectedIndex()]));
            }

            // Update the entity's targeting system type.
            if (!(entity.hasTargComp()) && (clientgui.getClient().game.getOptions().booleanOption("allow_level_3_targsys"))) {
                int targSysIndex = MiscType.T_TARGSYS_STANDARD;
                if (choTargSys.getSelectedItem() != null)
                    targSysIndex = MiscType.getTargetSysType((String) choTargSys.getSelectedItem());
                if (targSysIndex >= 0)
                    entity.setTargSysType(targSysIndex);
                else {
                    System.err.println("Illegal targeting system index: " + targSysIndex);
                    entity.setTargSysType(MiscType.T_TARGSYS_STANDARD);
                }
            }

            // If the player wants to swap unit numbers, update both
            // entities and send an update packet for the other entity.
            if (!entityUnitNum.isEmpty() &&
                    choUnitNum.getSelectedIndex() > 0) {
                Entity other = (Entity) entityUnitNum.get
                        (choUnitNum.getSelectedIndex());
                char temp = entity.getUnitNumber();
                entity.setUnitNumber(other.getUnitNumber());
                other.setUnitNumber(temp);
                client.sendUpdateEntity(other);
            }

            // Set the entity's deployment round.
            //entity.setDeployRound((choDeployment.getSelectedIndex() == 0?0:choDeployment.getSelectedIndex()+1));
            entity.setDeployRound(choDeployment.getSelectedIndex());

            // update munitions selections
            for (final Object newVar2 : m_vMunitions) {
                ((MunitionChoicePanel) newVar2).applyChoice();
            }
            // update MG rapid fire settings
            for (final Object newVar1 : m_vMGs) {
                ((RapidfireMGPanel) newVar1).applyChoice();
            }
            // update mines setting
            for (final Object newVar : m_vMines) {
                ((MineChoicePanel) newVar).applyChoice();
            }
            // update searchlight setting
            entity.setSpotlight(chSearchlight.isSelected());
            entity.setSpotlightState(chSearchlight.isSelected());
            setOptions();

            okay = true;
            clientgui.chatlounge.refreshEntities();
        }
        setVisible(false);
        Entity nextOne = null;
        if (actionEvent.getSource().equals(butPrev)) {
            nextOne = getNextEntity(false);
        } else if (actionEvent.getSource().equals(butNext)) {
            nextOne = getNextEntity(true);
        }
        if (nextOne != null) {
            clientgui.chatlounge.customizeMech(nextOne);
        }
    }

    private Entity getNextEntity(boolean forward) {
        IGame game = client.game;
        boolean bd = game.getOptions().booleanOption("blind_drop"); //$NON-NLS-1$
        boolean rbd = game.getOptions().booleanOption("real_blind_drop"); //$NON-NLS-1$
        Player p = client.getLocalPlayer();

        Entity nextOne;
        if (forward) {
            nextOne = game.getNextEntityFromList(entity);
        } else {
            nextOne = game.getPreviousEntityFromList(entity);
        }
        while (nextOne != null && !nextOne.equals(entity)) {
            if (nextOne.getOwner().equals(p) || !(bd || rbd)) {
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
}
