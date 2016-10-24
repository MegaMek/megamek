/*
 * MegaMek -
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import megamek.client.Client;
import megamek.client.RandomUnitGenerator;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSearchFilter;
import megamek.common.MechSummary;
import megamek.common.TechConstants;
import megamek.common.IGame.Phase;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.RandomArmyCreator;

public class RandomArmyDialog extends JDialog implements ActionListener,
WindowListener, TreeSelectionListener, FocusListener {

    /**
     *
     */
    private static final long serialVersionUID = 4072453002423681675L;
    private ClientGUI m_clientgui;
    private Client m_client;
    AdvancedSearchDialog asd;

    private MechSearchFilter searchFilter;

    private JLabel m_labelPlayer = new JLabel(Messages
            .getString("RandomArmyDialog.Player"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JComboBox<String> m_chPlayer = new JComboBox<String>();
    private JComboBox<String> m_chType = new JComboBox<String>();

    private JTree m_treeRAT = new JTree();
    //private JScrollPane m_treeViewRAT = new JScrollPane(m_treeRAT);
    private JTabbedPane m_pMain = new JTabbedPane();
    private JPanel m_pRAT = new JPanel();
    private JPanel m_pRATGen = new JPanel();
    private JPanel m_pRATGenOptions = new JPanel();
    private JPanel m_pUnitTypeOptions = new JPanel(new CardLayout());
    private JPanel m_pParameters = new JPanel();
    private JPanel m_pPreview = new JPanel();
    private JPanel m_pButtons = new JPanel();
    private JPanel m_pAdvSearch = new JPanel();
    private JButton m_bOK = new JButton(Messages.getString("Okay"));
    private JButton m_bCancel = new JButton(Messages.getString("Cancel"));
    private JButton m_bAdvSearch = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearch"));
    private JButton m_bAdvSearchClear = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearchClear"));
    private JButton m_bGenerate = new JButton(Messages.getString("RandomArmyDialog.Generate"));
    private JButton m_bAddToForce = new JButton(Messages.getString("RandomArmyDialog.AddToForce"));

    private JSplitPane m_pSplit;

    private JButton m_bAddAll = new JButton(Messages.getString("RandomArmyDialog.AddAll"));
    private JButton m_bAdd = new JButton(Messages.getString("RandomArmyDialog.AddSelected"));
    private JButton m_bRoll = new JButton(Messages.getString("RandomArmyDialog.Roll"));
    private JButton m_bClear = new JButton(Messages.getString("RandomArmyDialog.Clear"));

    private JTable m_lArmy;
    private JTable m_lUnits;
    private JTable m_lRAT;

    private UnitTableModel armyModel;
    private UnitTableModel unitsModel;
    private RATTableModel ratModel;

    private JLabel m_labBV = new JLabel(Messages
            .getString("RandomArmyDialog.BV"));
    private JLabel m_labYear = new JLabel(Messages
            .getString("RandomArmyDialog.Year"));
    private JLabel m_labMechs = new JLabel(Messages
            .getString("RandomArmyDialog.Mechs"));
    private JLabel m_labVees = new JLabel(Messages
            .getString("RandomArmyDialog.Vees"));
    private JLabel m_labBA = new JLabel(Messages
            .getString("RandomArmyDialog.BA"));
    private JLabel m_labInfantry = new JLabel(Messages
            .getString("RandomArmyDialog.Infantry"));
    private JLabel m_labTech = new JLabel(Messages
            .getString("RandomArmyDialog.Tech"));
    private JLabel m_labUnits = new JLabel(Messages
            .getString("RandomArmyDialog.Unit"));
    private JLabel m_labFaction = new JLabel(Messages
            .getString("RandomArmyDialog.Faction"));
    private JLabel m_labCommand = new JLabel(Messages
            .getString("RandomArmyDialog.Command"));
    private JLabel m_labUnitType = new JLabel(Messages
            .getString("RandomArmyDialog.UnitType"));
    private JLabel m_labRating = new JLabel(Messages
            .getString("RandomArmyDialog.Rating"));
    private JLabel m_ratStatus;

    private JTextField m_tBVmin = new JTextField(6);
    private JTextField m_tBVmax = new JTextField(6);
    private JTextField m_tMinYear = new JTextField(4);
    private JTextField m_tMaxYear = new JTextField(4);
    private JTextField m_tMechs = new JTextField(3);
    private JTextField m_tVees = new JTextField(3);
    private JTextField m_tBA = new JTextField(3);
    private JTextField m_tInfantry = new JTextField(3);
    private JTextField m_tUnits = new JTextField(3);
    private JCheckBox m_chkPad = new JCheckBox(Messages
            .getString("RandomArmyDialog.Pad"));
    private JCheckBox m_chkCanon = new JCheckBox(Messages
            .getString("RandomArmyDialog.Canon"));

    private JTextField m_tRGUnits = new JTextField(3);
    private JTextField m_tYear = new JTextField(4);
    private JComboBox<FactionRecord> m_chFaction = new JComboBox<FactionRecord>();
    private JComboBox<FactionRecord> m_chSubfaction = new JComboBox<FactionRecord>();
    private JCheckBox m_chkShowMinor = new JCheckBox(Messages
    		.getString("RandomArmyDialog.ShowMinorFactions"));
    private JComboBox<String> m_chUnitType = new JComboBox<String>();
    private JComboBox<String> m_chRating = new JComboBox<String>();
    private HashMap<String,UnitTypeOptionsPanel> unitTypeCards = 
    		new HashMap<String,UnitTypeOptionsPanel>();
    
    private RandomUnitGenerator rug;
    private RATGenerator rg;
    private int ratGenYear;
    private UnitTable generatedRAT;

    public RandomArmyDialog(ClientGUI cl) {
        super(cl.frame, Messages.getString("RandomArmyDialog.title"), true); //$NON-NLS-1$
        m_clientgui = cl;
        m_client = cl.getClient();
        rug = RandomUnitGenerator.getInstance();
        rug.registerListener(this);
        if (rug.isInitialized()){
            m_ratStatus = new JLabel(Messages
                    .getString("RandomArmyDialog.ratStatusDoneLoading"));            
        } else {
            m_ratStatus = new JLabel(Messages
                    .getString("RandomArmyDialog.ratStatusLoading"));
        }
        ratGenYear = m_clientgui.getClient().getGame().getOptions()
                .intOption("year");
        rg = RATGenerator.getInstance();
        rg.registerListener(this);
        updatePlayerChoice();
        asd = new AdvancedSearchDialog(m_clientgui.frame,
                m_client.getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR));
        
        GUIPreferences guip = GUIPreferences.getInstance();
        // set defaults
        m_tMechs.setText(guip.getRATNumMechs());
        m_tBVmin.setText(guip.getRATBVMin());
        m_tBVmax.setText(guip.getRATBVMax());
        m_tVees.setText(guip.getRATNumVees());
        m_tBA.setText(guip.getRATNumBA());
        m_tMinYear.setText(guip.getRATYearMin());
        m_tMaxYear.setText(guip.getRATYearMax());
        m_tInfantry.setText(guip.getRATNumInf());
        m_chkPad.setSelected(guip.getRATPadBV());
        m_chkCanon.setSelected(m_client.getGame().getOptions().booleanOption(
        OptionsConstants.ALLOWED_CANON_ONLY));
        updateTechChoice();

        // construct the buttons panel
        m_pButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        m_pButtons.add(m_bOK);
        m_bOK.addActionListener(this);
        m_pButtons.add(m_bCancel);
        m_bCancel.addActionListener(this);
        m_pButtons.add(m_labelPlayer);
        m_pButtons.add(m_chPlayer);

        // construct the Adv Search Panel
        m_pAdvSearch.setLayout(new FlowLayout(FlowLayout.LEADING));
        m_pAdvSearch.add(m_bAdvSearch);
        m_pAdvSearch.add(m_bAdvSearchClear);
        m_bAdvSearchClear.setEnabled(false);
        m_bAdvSearch.addActionListener(this);
        m_bAdvSearchClear.addActionListener(this);

        // construct the parameters panel
        GridBagLayout layout = new GridBagLayout();
        m_pParameters.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        layout.setConstraints(m_labTech, constraints);
        m_pParameters.add(m_labTech);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_chType, constraints);
        m_pParameters.add(m_chType);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labBV, constraints);
        m_pParameters.add(m_labBV);
        m_pParameters.add(m_tBVmin);
        JLabel dash = new JLabel("-");
        layout.setConstraints(dash, constraints);
        m_pParameters.add(dash);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_tBVmax, constraints);
        m_pParameters.add(m_tBVmax);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labMechs, constraints);
        m_pParameters.add(m_labMechs);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_tMechs, constraints);
        m_pParameters.add(m_tMechs);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labVees, constraints);
        m_pParameters.add(m_labVees);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_tVees, constraints);
        m_pParameters.add(m_tVees);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labBA, constraints);
        m_pParameters.add(m_labBA);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_tBA, constraints);
        m_pParameters.add(m_tBA);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labInfantry, constraints);
        m_pParameters.add(m_labInfantry);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_tInfantry, constraints);
        m_pParameters.add(m_tInfantry);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labYear, constraints);
        m_pParameters.add(m_labYear);
        layout.setConstraints(m_tMinYear, constraints);
        m_pParameters.add(m_tMinYear);
        dash = new JLabel("-");
        layout.setConstraints(dash, constraints);
        m_pParameters.add(dash);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_tMaxYear, constraints);
        m_pParameters.add(m_tMaxYear);
        layout.setConstraints(m_chkPad, constraints);
        m_pParameters.add(m_chkPad);
        layout.setConstraints(m_chkCanon, constraints);
        m_pParameters.add(m_chkCanon);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weighty = 1.0;
        layout.setConstraints(m_pAdvSearch, constraints);
        m_pParameters.add(m_pAdvSearch);

        //construct the RAT panel
        m_pRAT.setLayout(new GridBagLayout());
        m_tUnits.setText("4");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRAT.add(m_labUnits, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRAT.add(m_tUnits, c);
        
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(0,10,0,0);
        m_pRAT.add(m_ratStatus,c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new java.awt.Insets(5, 5, 5, 5);


        m_treeRAT.setRootVisible(false);
        m_treeRAT.getSelectionModel().setSelectionMode
        (TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_treeRAT.addTreeSelectionListener(this);
        //m_treeRAT.setPreferredSize(new Dimension(200, 100));

        JScrollPane treeViewRAT = new JScrollPane(m_treeRAT);
        treeViewRAT.setPreferredSize(new Dimension(300, 200));
        m_pRAT.add(treeViewRAT, c);

        //construct the RAT Generator panel
        m_pRATGen.setLayout(new GridBagLayout());

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0.5;
        m_pRATGen.add(new JScrollPane(m_pRATGenOptions), c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGen.add(m_bGenerate, c);
        m_bGenerate.setToolTipText(Messages.getString("RandomArmyDialog.Generate.tooltip"));
        m_bGenerate.addActionListener(this);
        
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGen.add(m_bAddToForce, c);
        m_bAddToForce.setToolTipText(Messages.getString("RandomArmyDialog.AddToForce.tooltip"));
        m_bAddToForce.addActionListener(this);
        
        ratModel = new RATTableModel();
        m_lRAT = new JTable();
        m_lRAT.setModel(ratModel);
        m_lRAT.setIntercellSpacing(new Dimension(5, 0));
        m_lRAT.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        for (int i = 0; i < ratModel.getColumnCount(); i++) {
        	m_lRAT.getColumnModel().getColumn(i).setPreferredWidth(ratModel.getPreferredWidth(i));
        }
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
		m_lRAT.getColumnModel().getColumn(RATTableModel.COL_BV).setCellRenderer(rightRenderer);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0.5;
        m_pRATGen.add(new JScrollPane(m_lRAT), c);

        
        m_pRATGenOptions.setLayout(new GridBagLayout());
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_labYear, c);

        m_tYear.setText(String.valueOf(ratGenYear));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_tYear, c);
        m_tYear.addFocusListener(this);
        
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(new JLabel(Messages.getString("RandomArmyDialog.Unit")), c);

        m_tRGUnits.setText("4");

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_tRGUnits, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_labFaction, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_chFaction, c);
        m_chFaction.setRenderer(factionCbRenderer);
        m_chFaction.addActionListener(this);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_labCommand, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_chSubfaction, c);
        m_chSubfaction.setRenderer(factionCbRenderer);
        m_chSubfaction.addActionListener(this);        
        
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_chkShowMinor, c);
        m_chkShowMinor.addActionListener(this);
        
		for (int i = 0; i < UnitType.SIZE; i++) {
			if (i != UnitType.GUN_EMPLACEMENT
					&& i != UnitType.SPACE_STATION) {
				m_chUnitType.addItem(UnitType.getTypeName(i));
				UnitTypeOptionsPanel card = new UnitTypeOptionsPanel(i);
				unitTypeCards.put(UnitType.getTypeName(i), card);
			}
		}
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_labUnitType, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_chUnitType, c);
        m_chUnitType.addActionListener(this);
        
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_labRating, c);

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_pRATGenOptions.add(m_chRating, c);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 1.0;
        m_pRATGenOptions.add(m_pUnitTypeOptions, c);
        
        for (String unitType : unitTypeCards.keySet()) {
        	m_pUnitTypeOptions.add(unitTypeCards.get(unitType), unitType);
        }

        m_chUnitType.setSelectedIndex(0);

        // construct the preview panel
        m_pPreview.setLayout(new GridBagLayout());
        unitsModel = new UnitTableModel();
        m_lUnits = new JTable();
        m_lUnits.setModel(unitsModel);
        m_lUnits.setIntercellSpacing(new Dimension(0, 0));
        m_lUnits.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(m_lUnits);
        scroll.setBorder(BorderFactory.createTitledBorder(Messages.getString("RandomArmyDialog.SelectedUnits")));
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        m_pPreview.add(scroll, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        m_bRoll.addActionListener(this);
        m_pPreview.add(m_bRoll,c);
        c.gridx = 1;
        m_bAddAll.addActionListener(this);
        m_pPreview.add(m_bAddAll,c);
        c.gridx = 2;
        m_bAdd.addActionListener(this);
        m_pPreview.add(m_bAdd,c);
        c.gridx = 3;
        m_bClear.addActionListener(this);
        m_pPreview.add(m_bClear,c);
        armyModel = new UnitTableModel();
        m_lArmy = new JTable();
        m_lArmy.setModel(armyModel);
        m_lArmy.setIntercellSpacing(new Dimension(0, 0));
        m_lArmy.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        scroll = new JScrollPane(m_lArmy);
        scroll.setBorder(BorderFactory.createTitledBorder(Messages.getString("RandomArmyDialog.Army")));
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        m_pPreview.add(scroll, c);

        m_pMain.addTab(Messages.getString("RandomArmyDialog.BVtab"), m_pParameters);
        m_pMain.addTab(Messages.getString("RandomArmyDialog.RATtab"), m_pRAT);
        m_pMain.addTab(Messages.getString("RandomArmyDialog.RATGentab"), m_pRATGen);

        m_pSplit = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,m_pMain, m_pPreview);
        m_pSplit.setOneTouchExpandable(false);
        m_pSplit.setResizeWeight(0.5);

        // construct the main dialog
        setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(800,500));
        add(m_pButtons, BorderLayout.SOUTH);
        add(m_pSplit, BorderLayout.CENTER);
        validate();
        pack();
        setLocationRelativeTo(cl.frame);
    }

    public void valueChanged(TreeSelectionEvent ev) {
        if (ev.getSource().equals(m_treeRAT)) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_treeRAT.getLastSelectedPathComponent();
            if (node == null) {
                return;
            }

            Object nodeInfo = node.getUserObject();
            if (node.isLeaf()) {
                String ratName = (String)nodeInfo;
                rug.setChosenRAT(ratName);
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(m_bOK)) {
            ArrayList<Entity> entities = new ArrayList<Entity>(
                    armyModel.getAllUnits().size());
            Client c = null;
            if (m_chPlayer.getSelectedIndex() > 0) {
                String name = (String) m_chPlayer.getSelectedItem();
                c = m_clientgui.getBots().get(name);
            }
            if (c == null) {
                c = m_client;
            }
            for (MechSummary ms : armyModel.getAllUnits()) {
                try {
                    Entity e = new MechFileParser(ms.getSourceFile(), 
                            ms.getEntryName()).getEntity();
  
                    autoSetSkillsAndName(e);
                    e.setOwner(c.getLocalPlayer());
                    if (c.getGame().getPhase() != Phase.PHASE_LOUNGE){
                        e.setDeployRound(c.getGame().getRoundCount()+1);
                        e.setGame(c.getGame());
                        // Set these to true, otherwise units reinforced in
                        // the movement turn are considered selectable
                        e.setDone(true);
                        e.setUnloaded(true);
                    }
                    entities.add(e);
                } catch (EntityLoadingException ex) {
                    System.out.println("Unable to load mech: " + //$NON-NLS-1$ 
                            ms.getSourceFile() + ": " + ms.getEntryName() + //$NON-NLS-1$
                            ": " + ex.getMessage()); //$NON-NLS-1$ 
                    ex.printStackTrace();
                    return;
                }
            }
            c.sendAddEntity(entities);
            armyModel.clearData();
            unitsModel.clearData();
            
            // Save preferences
            GUIPreferences guip = GUIPreferences.getInstance();
            guip.setRATBVMin(m_tBVmin.getText());
            guip.setRATBVMax(m_tBVmax.getText());
            guip.setRATNumMechs(m_tMechs.getText());
            guip.setRATNumVees(m_tVees.getText());
            guip.setRATNumBA(m_tBA.getText());
            guip.setRATNumInf(m_tInfantry.getText());
            guip.setRATYearMin(m_tMinYear.getText());
            guip.setRATYearMax(m_tMaxYear.getText());
            guip.setRATPadBV(m_chkPad.isSelected());
            guip.setRATTechLevel(m_chType.getSelectedIndex());
            if (m_treeRAT.getSelectionPath() != null) {
                guip.setRATSelectedRAT(m_treeRAT.getSelectionPath().toString());
            } else {
                guip.setRATSelectedRAT("");
            }
            
            setVisible(false);
        } else if (ev.getSource().equals(m_bClear)) {
            armyModel.clearData();
            unitsModel.clearData();
        } else if (ev.getSource().equals(m_bCancel)) {
             armyModel.clearData();
             unitsModel.clearData();
            setVisible(false);
        } else if (ev.getSource().equals(m_bAddAll)) {
            for (MechSummary m : unitsModel.getAllUnits()) {
                armyModel.addUnit(m);
            }
        } else if (ev.getSource().equals(m_bAdd)) {
            for(int sel : m_lUnits.getSelectedRows()) {
                MechSummary m = unitsModel.getUnitAt(sel);
                armyModel.addUnit(m);
            }
        } else if (ev.getSource().equals(m_bAdvSearch)){
            searchFilter=asd.showDialog();
            m_bAdvSearchClear.setEnabled(searchFilter!=null);
        } else if (ev.getSource().equals(m_bAdvSearchClear)){
            searchFilter=null;
            m_bAdvSearchClear.setEnabled(false);
        } else if (ev.getSource().equals(m_bRoll)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                if(m_pMain.getSelectedIndex() == 1) {
                    int units = Integer.parseInt(m_tUnits.getText());
                    if(units > 0) {
                        unitsModel.setData(RandomUnitGenerator.getInstance().generate(units));
                    }
                } else if (m_pMain.getSelectedIndex() == 2) {
                	int units = Integer.parseInt(m_tRGUnits.getText());
                	if (units > 0 && generatedRAT != null && generatedRAT.getNumEntries() > 0) {
                		unitsModel.setData(generatedRAT.generateUnits(units));
                	}
                	//generateUnits removes salvage entries that have no units meeting criteria
                	ratModel.refreshData();
                } else {
                    RandomArmyCreator.Parameters p = new RandomArmyCreator.Parameters();
                    p.advancedSearchFilter=searchFilter;
                    p.mechs = Integer.parseInt(m_tMechs.getText());
                    p.tanks = Integer.parseInt(m_tVees.getText());
                    p.ba = Integer.parseInt(m_tBA.getText());
                    p.infantry = Integer.parseInt(m_tInfantry.getText());
                    p.canon = m_chkCanon.isSelected();
                    p.maxBV = Integer.parseInt(m_tBVmax.getText());
                    p.minBV = Integer.parseInt(m_tBVmin.getText());
                    p.padWithInfantry = m_chkPad.isSelected();
                    p.tech = m_chType.getSelectedIndex();
                    p.minYear = Integer.parseInt(m_tMinYear.getText());
                    p.maxYear = Integer.parseInt(m_tMaxYear.getText());
                    unitsModel.setData(RandomArmyCreator.generateArmy(p));
                }
            } catch (NumberFormatException ex) {
                //ignored
            }finally{
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        } else if (ev.getSource().equals(m_chFaction)) {
        	updateSubfactionChoice();
        } else if (ev.getSource().equals(m_chSubfaction)) {
        	updateRatingChoice();
        } else if (ev.getSource().equals(m_chkShowMinor)) {
        	updateFactionChoice();
        } else if (ev.getSource().equals(m_chUnitType)) {
        	CardLayout layout = (CardLayout)m_pUnitTypeOptions.getLayout();
        	layout.show(m_pUnitTypeOptions, (String)m_chUnitType.getSelectedItem());
        } else if (ev.getSource().equals(m_bGenerate)) {
        	generateRAT();
        } else if (ev.getSource().equals(m_bAddToForce)) {
            for(int sel : m_lRAT.getSelectedRows()) {
                MechSummary ms = generatedRAT.getMechSummary(sel);
                if (ms != null) {
                	armyModel.addUnit(ms);
                }
            }
        } else if (ev.getSource().equals(rug)) {
            m_ratStatus.setText(Messages
                    .getString("RandomArmyDialog.ratStatusDoneLoading"));
            updateRATs();
        } else if (ev.getSource().equals(rg)) {
        	if (ev.getActionCommand().equals("ratGenInitialized")) {
        		rg.loadYear(ratGenYear);
        	} else if (ev.getActionCommand().equals("ratGenEraLoaded")) {
        		updateFactionChoice();
        	}
        }
    }

    public void windowActivated(WindowEvent arg0) {
        //ignored
    }

    public void windowClosed(WindowEvent arg0) {
        //ignored
    }

    public void windowClosing(WindowEvent arg0) {
        setVisible(false);
    }

    public void windowDeactivated(WindowEvent arg0) {
        //ignored
    }

    public void windowDeiconified(WindowEvent arg0) {
        //ignored
    }

    public void windowIconified(WindowEvent arg0) {
        //ignored
    }

    public void windowOpened(WindowEvent arg0) {
        //ignored
    }


	@Override
	public void focusGained(FocusEvent e) {
		//ignored
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource().equals(m_tYear)) {
			try {
				ratGenYear = Integer.parseInt(m_tYear.getText());
				if (ratGenYear < rg.getEraSet().first()) {
					ratGenYear = rg.getEraSet().first();
				} else if (ratGenYear > rg.getEraSet().last()) {
					ratGenYear = rg.getEraSet().last();
				}
			} catch (NumberFormatException ex) {
				//ignore and restore to previous value
			}
			m_tYear.setText(String.valueOf(ratGenYear));
			rg.loadYear(ratGenYear);
		}
	}

	private void updatePlayerChoice() {
        String lastChoice = (String) m_chPlayer.getSelectedItem();
        String clientName = m_clientgui.getClient().getName();
        m_chPlayer.removeAllItems();
        m_chPlayer.setEnabled(true);
        m_chPlayer.addItem(clientName);
        for (Iterator<Client> i = m_clientgui.getBots().values().iterator(); i
        .hasNext();) {
            m_chPlayer.addItem(i.next().getName());
        }
        if (m_chPlayer.getItemCount() == 1) {
            m_chPlayer.setEnabled(false);
        }
        m_chPlayer.setSelectedItem(lastChoice);
        if (m_chPlayer.getSelectedIndex() < 0) {
            m_chPlayer.setSelectedIndex(0);
        }
    }

    private void updateTechChoice() {
        int gameTL = TechConstants.getSimpleLevel(m_client.getGame()
                .getOptions().stringOption("techlevel"));
        int maxTech = 0;
        switch (gameTL) {
            case TechConstants.T_SIMPLE_INTRO:
                maxTech = TechConstants.T_INTRO_BOXSET;
                break;
            case TechConstants.T_SIMPLE_STANDARD:
                maxTech = TechConstants.T_TW_ALL;
                break;
            case TechConstants.T_SIMPLE_ADVANCED:
                maxTech = TechConstants.T_CLAN_ADVANCED;
                break;
            case TechConstants.T_SIMPLE_EXPERIMENTAL:
                maxTech = TechConstants.T_CLAN_EXPERIMENTAL;
                break;
            case TechConstants.T_SIMPLE_UNOFFICIAL:
                maxTech = TechConstants.T_ALL;
                break;
            default:
                maxTech = TechConstants.T_TW_ALL;
        }

        m_chType.removeAllItems();
        for (int i = 0; i <= maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        int savedSelection = GUIPreferences.getInstance().getRATTechLevel();
        savedSelection = Math.min(savedSelection, maxTech - 1);
        m_chType.setSelectedIndex(savedSelection);
    }

    private void updateRATs() {
        Iterator<String> rats = rug.getRatList();
        if(null == rats) {
            return;
        }  
        
        RandomUnitGenerator.RatTreeNode ratTree = rug.getRatTree();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ratTree.name);
        createRatTreeNodes(root, ratTree);
        m_treeRAT.setModel(new DefaultTreeModel(root));
        
        String selectedRATPath = 
                GUIPreferences.getInstance().getRATSelectedRAT();
        if (!selectedRATPath.equals("")) {
            String[] nodes = selectedRATPath.replace('[', ' ')
                    .replace(']', ' ').split(",");
            TreePath path = findPathByName(nodes);
            m_treeRAT.setSelectionPath(path);
        }
    }

    private void createRatTreeNodes(DefaultMutableTreeNode parentNode, RandomUnitGenerator.RatTreeNode ratTreeNode) {
        for (RandomUnitGenerator.RatTreeNode child : ratTreeNode.children) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(child.name);
            if (child.children.size() > 0) {
                createRatTreeNodes(newNode, child);
            }
            parentNode.add(newNode);
        }
    }
    
    private TreePath findPathByName(String[] nodeNames) {
        TreeNode root = (TreeNode)m_treeRAT.getModel().getRoot();
        return findNextNode(new TreePath(root), nodeNames, 0);
    }
    
    private TreePath findNextNode(TreePath parent, String[] nodes, int depth) {
        TreeNode node = (TreeNode)parent.getLastPathComponent();
        String currNode = node.toString();

        // If equal, go down the branch
        if (currNode.equals(nodes[depth].trim())) {
            // If at end, return match
            if (depth == nodes.length-1) {
                return parent;
            }

            // Traverse children
            if (node.getChildCount() >= 0) {
                for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
                    TreeNode n = (TreeNode)e.nextElement();
                    TreePath path = parent.pathByAddingChild(n);
                    TreePath result = findNextNode(path, nodes, depth + 1);
                    // Found a match
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        // No match at this branch
        return null;
    }
    
    private void updateFactionChoice() {
    	FactionRecord old = (FactionRecord)m_chFaction.getSelectedItem();
    	m_chFaction.removeActionListener(this);
    	m_chFaction.removeAllItems();
    	ArrayList<FactionRecord> recs = new ArrayList<>();
    	for (FactionRecord fRec : rg.getFactionList()) {
    		if ((!fRec.isMinor() || m_chkShowMinor.isSelected())
    				&& !fRec.getKey().contains(".") && fRec.isActiveInYear(ratGenYear)) {
    			recs.add(fRec);
    		}
    	}
    	Collections.sort(recs, factionSorter);
    	for (FactionRecord fRec : recs) {
    		m_chFaction.addItem(fRec);
    	}
    	m_chFaction.setSelectedItem(old);
    	if (m_chFaction.getSelectedItem() == null) {
    		m_chFaction.setSelectedItem(rg.getFaction("IS"));
    	}
    	updateSubfactionChoice();
    	m_chFaction.addActionListener(this);
    }
    
    private void updateSubfactionChoice() {
    	FactionRecord old = (FactionRecord)m_chSubfaction.getSelectedItem();
    	m_chSubfaction.removeActionListener(this);
    	m_chSubfaction.removeAllItems();
    	FactionRecord selectedFaction = (FactionRecord)m_chFaction.getSelectedItem();
    	if (selectedFaction != null) {
	    	ArrayList<FactionRecord> recs = new ArrayList<>();
	    	for (FactionRecord fRec : rg.getFactionList()) {
	    		if (fRec.getKey().startsWith(selectedFaction.getKey() + ".")
	    				&& fRec.isActiveInYear(ratGenYear)) {
	    			recs.add(fRec);
	    		}
	    	}
	    	Collections.sort(recs, factionSorter);
	    	m_chSubfaction.addItem(null); //No specific subcommand.
	    	for (FactionRecord fRec : recs) {
	    		m_chSubfaction.addItem(fRec);
	    	}
    	}
    	m_chSubfaction.setSelectedItem(old);
    	updateRatingChoice();
    	m_chSubfaction.addActionListener(this);
    }
    
    /**
     * When faction or subfaction is changed, refresh ratings combo box with appropriate
     * values for selected faction.
     * 
     */
    
    private void updateRatingChoice() {
    	int current = m_chRating.getSelectedIndex();
    	m_chRating.removeAllItems();
    	FactionRecord fRec = (FactionRecord)m_chSubfaction.getSelectedItem();
    	if (fRec == null) {
    		// Subfaction is "general"
    		fRec = (FactionRecord)m_chFaction.getSelectedItem();
    	}
    	ArrayList<String> ratingLevels = fRec.getRatingLevels();
    	if (ratingLevels.isEmpty()) {
    		// Get rating levels from parent faction(s)
    		ratingLevels = fRec.getRatingLevelSystem();
    	}
    	if (ratingLevels.size() > 1) {
			for (int i = ratingLevels.size() - 1; i >= 0; i--) {
				m_chRating.addItem(ratingLevels.get(i));
			}
    	}
		if (current < 0 && m_chRating.getItemCount() > 0) {
			m_chRating.setSelectedIndex(0);
		} else {
			m_chRating.setSelectedIndex(Math.min(current, m_chRating.getItemCount() - 1));
		}
    }
    
    private DefaultListCellRenderer factionCbRenderer = new DefaultListCellRenderer() {
		private static final long serialVersionUID = -333065979253244440L;

		@Override
		public Component getListCellRendererComponent(JList<?> list,
				Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			if (value == null) {
				setText("General");
			} else {
				setText(((FactionRecord)value).getName(ratGenYear));
			}
			return this;
		}    	
    };
    
    private Comparator<FactionRecord> factionSorter = new Comparator<FactionRecord>() {
		@Override
		public int compare(FactionRecord o1, FactionRecord o2) {
			return o1.getName(ratGenYear).compareTo(o2.getName(ratGenYear));
		}    	
    };
    
    private void generateRAT() {
    	FactionRecord fRec = (FactionRecord)(m_chSubfaction.getSelectedItem() == null?
    			m_chFaction.getSelectedItem() : m_chSubfaction.getSelectedItem());
    	if (fRec != null) {
			UnitTypeOptionsPanel panOptions = unitTypeCards.get((String)m_chUnitType.getSelectedItem());
			generatedRAT = UnitTable.findTable(fRec, ModelRecord.parseUnitType((String)m_chUnitType.getSelectedItem()),
					ratGenYear, (String)m_chRating.getSelectedItem(),
					panOptions.getSelectedWeights(),
					panOptions.getNetworkMask(), panOptions.getMotiveTypes(),
					panOptions.getSelectedRoles(), panOptions.getRoleStrictness());
			ratModel.refreshData();
    	}
    }

    @Override
    public void setVisible(boolean show) {
        if (show) {
            updatePlayerChoice();
            updateTechChoice();
            updateRATs();
        }
        super.setVisible(show);
    }

    private void autoSetSkillsAndName(Entity e) {
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        if(cs.useAverageSkills()) {
            int skills[] = m_client.getRandomSkillsGenerator().getRandomSkills(e, true);

            int gunnery = skills[0];
            int piloting = skills[1];

            e.getCrew().setGunnery(gunnery);
            e.getCrew().setPiloting(piloting);
        }
        if(cs.generateNames()) {
            e.getCrew().setName(m_client.getRandomNameGenerator().generate());
        }
    }

    /**
     * A table model for displaying units
     */
    public class UnitTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 4819661751806908535L;

        private static final int COL_UNIT = 0;
        private static final int COL_BV = 1;
        private static final int COL_MOVE = 2;
        private static final int N_COL = 3;

        private ArrayList<MechSummary> data;

        public UnitTableModel() {
            data = new ArrayList<MechSummary>();
        }

        public int getRowCount() {
            return data.size();
        }

        public void clearData() {
            data = new ArrayList<MechSummary>();
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return N_COL;
        }

        public void addUnit(MechSummary m) {
            data.add(m);
            fireTableDataChanged();
        }

        public void setData(ArrayList<MechSummary> mechs) {
            data = mechs;
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case (COL_UNIT):
                    return Messages.getString("RandomArmyDialog.colUnit");
                case (COL_MOVE):
                    return Messages.getString("RandomArmyDialog.colMove");
                case (COL_BV):
                    return Messages.getString("RandomArmyDialog.colBV");
            }
            return "??";
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Object getValueAt(int row, int col) {
            MechSummary m = getUnitAt(row);
            String value = "";
            if (col == COL_BV) {
                value += m.getBV();
            } else if (col == COL_MOVE) {
                value += m.getWalkMp() + "/" + m.getRunMp() + "/" + m.getJumpMp();
            } else {
                return m.getName();
            }
            return value;
        }

        public MechSummary getUnitAt(int row) {
            return data.get(row);
        }

        public ArrayList<MechSummary> getAllUnits() {
            return data;
        }
    }

    /**
     * A table model for displaying a generated RAT
     */
    public class RATTableModel extends AbstractTableModel {

        /**
		 * 
		 */
		private static final long serialVersionUID = 7807207311532173654L;
		
		private static final int COL_WEIGHT = 0;
        private static final int COL_UNIT = 1;
        private static final int COL_BV = 2;
        private static final int N_COL = 3;

        public int getRowCount() {
        	if (generatedRAT == null) {
        		return 0;
        	}
            return generatedRAT.getNumEntries();
        }

        public void refreshData() {
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return N_COL;
        }
        
        public int getPreferredWidth(int col) {
        	switch (col) {
        	case COL_WEIGHT:
        		return 12;
        	case COL_UNIT:
        		return 240;
        	case COL_BV:
        		return 18;
        	}
        	return 0;
        }
        
        @Override
        public String getColumnName(int column) {
            switch (column) {
                case (COL_WEIGHT):
                    return Messages.getString("RandomArmyDialog.colWeight");
                case (COL_UNIT):
                    return Messages.getString("RandomArmyDialog.colUnit");
                case (COL_BV):
                    return Messages.getString("RandomArmyDialog.colBV");
            }
            return "??";
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Object getValueAt(int row, int col) {
        	if (generatedRAT != null) {
		    	switch (col) {
		    	case COL_WEIGHT:
		    		return generatedRAT.getEntryWeight(row);
		    	case COL_UNIT:
		    		return generatedRAT.getEntryText(row);
		    	case COL_BV:
		    		int bv = generatedRAT.getBV(row);
		    		if (bv > 0) {
		    			return String.valueOf(bv);
		    		}
		    	}
        	}
		   	return "";
        }
    }
    
    /**
     * Options that vary according to unit type
     *
     */
    
    public class UnitTypeOptionsPanel extends JPanel {
    	/**
		 * 
		 */
		private static final long serialVersionUID = -3961143911841133921L;

		private JComboBox<String> cbWeightClass = new JComboBox<String>();
		private ArrayList<JCheckBox> weightChecks = new ArrayList<JCheckBox>();
		private JComboBox<String> cbRoleStrictness = new JComboBox<String>();
		private ArrayList<JCheckBox> roleChecks = new ArrayList<JCheckBox>();
		private ButtonGroup networkButtons = new ButtonGroup();
		private ArrayList<JCheckBox> subtypeChecks = new ArrayList<JCheckBox>();
    	
    	public UnitTypeOptionsPanel(int unitType) {
    		super(new BorderLayout());
    		
    		JPanel panWeightClass = new JPanel(new GridBagLayout());
            panWeightClass.setBorder(BorderFactory.createTitledBorder(Messages
            		.getString("RandomArmyDialog.WeightClass")));
    		add(panWeightClass, BorderLayout.WEST);
    		
    		JPanel panRoles = new JPanel(new GridBagLayout());
    		panRoles.setBorder(BorderFactory.createTitledBorder(Messages
            		.getString("RandomArmyDialog.MissionRole")));
            
            JPanel panStrictness = new JPanel();
            panStrictness.add(new JLabel(Messages.getString("RandomArmyDialog.Strictness")));
            cbRoleStrictness.setToolTipText(Messages.getString("RandomArmyDialog.Strictness.tooltip"));
    		cbRoleStrictness.addItem(Messages.getString("RandomArmyDialog.Low"));
    		cbRoleStrictness.addItem(Messages.getString("RandomArmyDialog.Medium"));
    		cbRoleStrictness.addItem(Messages.getString("RandomArmyDialog.High"));
    		cbRoleStrictness.setSelectedIndex(1);
    		panStrictness.add(cbRoleStrictness);
    		
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;
            panRoles.add(panStrictness, c);
            
            add(panRoles, BorderLayout.CENTER);

            JPanel panNetwork = new JPanel(new GridBagLayout());
            panNetwork.setBorder(BorderFactory.createTitledBorder(Messages
            		.getString("RandomArmyDialog.Network")));
    		add(panNetwork, BorderLayout.EAST);
    		
    		JPanel panMotive = new JPanel();
    		add(panMotive, BorderLayout.NORTH);
    		
    		switch(unitType) {
    		case UnitType.MEK:
    			addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_ULTRA_LIGHT,
    					EntityWeightClass.WEIGHT_COLOSSAL, false);
    			break;
    		case UnitType.TANK:
    		case UnitType.NAVAL:
    			addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_LIGHT,
    					EntityWeightClass.WEIGHT_ASSAULT, false);
    			break;
    		case UnitType.PROTOMEK:    			
    			addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_LIGHT,
    					EntityWeightClass.WEIGHT_ASSAULT, true);
    			break;
    		case UnitType.BATTLE_ARMOR:
    			addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_ULTRA_LIGHT,
    					EntityWeightClass.WEIGHT_ASSAULT, true);
    			break;
    		case UnitType.AERO:
    			addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_LIGHT,
    					EntityWeightClass.WEIGHT_HEAVY, false);
    			break;
    		case UnitType.DROPSHIP:
    			addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_SMALL_DROP,
    					EntityWeightClass.WEIGHT_LARGE_DROP, true);
    			break;
    		case UnitType.WARSHIP:
    			addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_SMALL_WAR,
    					EntityWeightClass.WEIGHT_LARGE_WAR, true);
    			break;
    		default:
    			panWeightClass.setVisible(false);
    		}
    		
    		for (MissionRole role : MissionRole.values()) {
    			if (role.fitsUnitType(unitType)) {
    				JCheckBox chk = new JCheckBox(Messages.getString("MissionRole."
    						+ role.toString()));
    				chk.setToolTipText(Messages.getString("MissionRole."
    						+ role.toString() + ".tooltip"));
    				chk.setName(role.toString());
    				roleChecks.add(chk);
    			}
    		}
    		Collections.sort(roleChecks, (c1, c2) -> c1.getText().compareTo(c2.getText()));
            c = new GridBagConstraints();
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;
			for (int i = 0; i < roleChecks.size(); i++) {
				c.gridx = i % 3;
				c.gridy = i / 3 + 1;
				if (c.gridx == 2) {
    				c.weightx = 1.0;
				} else {
					c.weightx = 0.0;
				}
    			if (i == roleChecks.size() - 1) {
    				c.weighty = 1.0;
    			}
				panRoles.add(roleChecks.get(i), c);
			}
			
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;

			switch (unitType) {
			case UnitType.MEK:
			case UnitType.TANK:
			case UnitType.VTOL:
			case UnitType.NAVAL:
			case UnitType.CONV_FIGHTER:
			case UnitType.AERO:
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.NoNetwork"),
						ModelRecord.NETWORK_NONE);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3S"),
						ModelRecord.NETWORK_C3_SLAVE);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3M"),
						ModelRecord.NETWORK_C3_MASTER);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3I"),
						ModelRecord.NETWORK_C3I);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3SB"),
						ModelRecord.NETWORK_BOOSTED_SLAVE);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3MB"),
						ModelRecord.NETWORK_BOOSTED_MASTER);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3CC"),
						ModelRecord.NETWORK_COMPANY_COMMAND);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3CCB"),
						ModelRecord.NETWORK_COMPANY_COMMAND|ModelRecord.NETWORK_BOOSTED);
				c.weighty = 1.0;
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.Nova"),
						ModelRecord.NETWORK_NOVA);
				break;
			case UnitType.BATTLE_ARMOR:
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.NoNetwork"),
						ModelRecord.NETWORK_NONE);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3BA"),
						ModelRecord.NETWORK_BA_C3);
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3BAB"),
						ModelRecord.NETWORK_BOOSTED_SLAVE);
				c.weighty = 1.0;
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3I"),
						ModelRecord.NETWORK_C3I);
				break;
			case UnitType.DROPSHIP:
			case UnitType.JUMPSHIP:
			case UnitType.WARSHIP:
			case UnitType.SPACE_STATION:
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.NoNetwork"),
						ModelRecord.NETWORK_NONE);
				c.weighty = 1.0;
				addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3N"),
						ModelRecord.NETWORK_NAVAL_C3);
				break;
			}
			
			switch(unitType) {
			case UnitType.TANK:
				panMotive.add(createSubtypeCheck("hover", true));
				panMotive.add(createSubtypeCheck("tracked", true));
				panMotive.add(createSubtypeCheck("wheeled", true));
				panMotive.add(createSubtypeCheck("wige", true));
				panMotive.add(createSubtypeCheck("vtol", false));
				break;
			case UnitType.INFANTRY:
				panMotive.add(createSubtypeCheck("leg", true));
				panMotive.add(createSubtypeCheck("jump", true));
				panMotive.add(createSubtypeCheck("motorized", true));
				panMotive.add(createSubtypeCheck(Messages.getString("RandomArmyDialog.Mech.hover"),
						"hover", true));
				panMotive.add(createSubtypeCheck(Messages.getString("RandomArmyDialog.Mech.tracked"),
						"tracked", true));
				panMotive.add(createSubtypeCheck(Messages.getString("RandomArmyDialog.Mech.wheeled"),
						"wheeled", true));
				break;
			case UnitType.BATTLE_ARMOR:
				panMotive.add(createSubtypeCheck("leg", true));
				panMotive.add(createSubtypeCheck("jump", true));
				panMotive.add(createSubtypeCheck("umu", true));
				break;
			case UnitType.NAVAL:
				panMotive.add(createSubtypeCheck("naval", true));
				panMotive.add(createSubtypeCheck("hydrofoil", true));
				panMotive.add(createSubtypeCheck("submarine", true));
				break;
			case UnitType.DROPSHIP:
				panMotive.add(createSubtypeCheck("aerodyne", true));
				panMotive.add(createSubtypeCheck("spheroid", true));
				break;
			}
    	}
    	
    	private void addWeightClasses(JPanel panel, int start, int end, boolean all) {
            cbWeightClass.addItem(Messages.getString("RandomArmyDialog.Mixed"));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;
    		panel.add(cbWeightClass);
    		
    		c.gridx = 0;
    		c.gridwidth = 2;
    		for(int i = start; i <= end; i++) {
    			String name = Messages.getString("RandomArmyDialog.weight_class_" + i);
    			cbWeightClass.addItem(name);
    			JCheckBox chk = new JCheckBox(name);
    			chk.setName(String.valueOf(i));
    			chk.setSelected(all);
    			weightChecks.add(chk);
    			c.gridy++;
    			if (i == end) {
    				c.weightx = 1.0;
    				c.weighty = 1.0;
    			}
    			panel.add(chk, c);
    		}
    		cbWeightClass.addActionListener(e -> {
    			for (JCheckBox chk : weightChecks) {
    				chk.setEnabled(cbWeightClass.getSelectedIndex() == 0);
    			}
    		});
    		if (all) {
    			cbWeightClass.setSelectedIndex(0);
    		} else if (start > EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
    			cbWeightClass.setSelectedIndex(1);
    		} else {
    			cbWeightClass.setSelectedIndex(2);
    		}
    	}
    	
    	private void addNetworkButton(JPanel panel, GridBagConstraints constraints,
    			ButtonGroup group, String text, int mask) {
    		JRadioButton btn = new JRadioButton(text);
    		btn.setActionCommand(String.valueOf(mask));
    		btn.setSelected(mask == ModelRecord.NETWORK_NONE);
    		panel.add(btn, constraints);
    		group.add(btn);
    		constraints.gridy++;
    	}
    	
    	private JCheckBox createSubtypeCheck(String name, boolean select) {
    		return createSubtypeCheck(Messages.getString("RandomArmyDialog.Motive." + name),
    				name, select);
    	}
    	
    	private JCheckBox createSubtypeCheck(String text, String name, boolean select) {
    		JCheckBox chk = new JCheckBox(text);
    		chk.setName(name);
    		chk.setSelected(select);
    		subtypeChecks.add(chk);
    		return chk;
    	}
    	
    	public List<Integer> getSelectedWeights() {
    		if (cbWeightClass.getSelectedIndex() > 0) {
    			ArrayList<Integer> retVal = new ArrayList<Integer>();
    			retVal.add(Integer.parseInt(weightChecks
    					.get(cbWeightClass.getSelectedIndex() - 1).getName()));
    			return retVal;
    		}
    		return weightChecks.stream().filter(chk -> chk.isSelected())
    				.map(chk -> Integer.parseInt(chk.getName())).collect(Collectors.toList());
    	}

    	public List<MissionRole> getSelectedRoles() {
    		return roleChecks.stream().filter(chk -> chk.isSelected())
    				.map(chk -> MissionRole.parseRole(chk.getName()))
    					.filter(role -> role != null).collect(Collectors.toList());
    	}
    	
    	public int getRoleStrictness() {
    		return cbRoleStrictness.getSelectedIndex() + 1;
    	}
    	
    	public int getNetworkMask() {
    		if (networkButtons.getSelection() != null) {
    			return Integer.valueOf(networkButtons.getSelection().getActionCommand());
    		}
    		return ModelRecord.NETWORK_NONE;
    	}
    	
    	public List<EntityMovementMode> getMotiveTypes() {
    		return subtypeChecks.stream().filter(chk -> chk.isSelected())
    				.map(chk -> EntityMovementMode.getMode(chk.getName())).collect(Collectors.toList());
    	}
    }
}