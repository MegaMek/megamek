/*
 * MegaMek -
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
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

import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.generator.RandomUnitGenerator.RatTreeNode;
import megamek.client.ratgenerator.*;
import megamek.client.ratgenerator.UnitTable.Parameters;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.enums.Gender;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.RandomArmyCreator;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

public class RandomArmyDialog extends JDialog implements ActionListener, TreeSelectionListener {
    private static final int TAB_BV_MATCHING       = 0;
    private static final int TAB_RAT               = 1;
    private static final int TAB_RAT_GENERATOR     = 2;
    private static final int TAB_FORMATION_BUILDER = 3;
    private static final int TAB_FORCE_GENERATOR   = 4;
    
    private static final String CARD_PREVIEW = "card_preview";
    private static final String CARD_FORCE_TREE = "card_force_tree";
    
    private ClientGUI m_clientgui;
    private Client m_client;
    AdvancedSearchDialog asd;

    private MechSearchFilter searchFilter;

    private JLabel m_labelPlayer = new JLabel(Messages.getString("RandomArmyDialog.Player"), SwingConstants.RIGHT);

    private JComboBox<String> m_chPlayer = new JComboBox<>();
    private JComboBox<String> m_chType = new JComboBox<>();

    private JTree m_treeRAT = new JTree();
    private JTabbedPane m_pMain = new JTabbedPane();
    private JPanel m_pRAT = new JPanel();
    private JPanel m_pRATGen = new JPanel();
    private JPanel m_pFormations = new JPanel();
    private ForceGeneratorViewUi m_pForceGen;
    private ForceGenerationOptionsPanel m_pRATGenOptions;
    private JPanel m_pUnitTypeOptions = new JPanel(new CardLayout());
    private ForceGenerationOptionsPanel m_pFormationOptions;
    private JPanel m_pParameters = new JPanel();
    private JPanel m_pPreview = new JPanel();
    private JPanel m_pButtons = new JPanel();
    private JPanel m_pAdvSearch = new JPanel();
    private JButton m_bOK = new JButton(Messages.getString("Okay"));
    private JButton m_bCancel = new JButton(Messages.getString("Cancel"));
    private JButton m_bRandomSkills = new JButton(Messages.getString("SkillGenerationDialog.title"));
    private JButton m_bAdvSearch = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearch"));
    private JButton m_bAdvSearchClear = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearchClear"));
    private JButton m_bGenerate = new JButton(Messages.getString("RandomArmyDialog.Generate"));
    private JButton m_bAddToForce = new JButton(Messages.getString("RandomArmyDialog.AddToForce"));

    private CardLayout m_lRightCards = new CardLayout();
    private JPanel m_pRightPane = new JPanel(m_lRightCards);
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
    
    private RandomUnitGenerator rug;
    private UnitTable generatedRAT;

    public RandomArmyDialog(ClientGUI cl) {
        super(cl.frame, Messages.getString("RandomArmyDialog.title"), true);
        m_clientgui = cl;
        m_client = cl.getClient();
        rug = RandomUnitGenerator.getInstance();
        rug.registerListener(this);
        if (rug.isInitialized()) {
            m_ratStatus = new JLabel(Messages.getString("RandomArmyDialog.ratStatusDoneLoading"));            
        } else {
            m_ratStatus = new JLabel(Messages.getString("RandomArmyDialog.ratStatusLoading"));
        }
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
        m_bRandomSkills.addActionListener(this);
        m_pButtons.add(m_labelPlayer);
        m_pButtons.add(m_chPlayer);
        m_pButtons.add(m_bRandomSkills);

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
        JScrollPane m_spParameters = new JScrollPane(m_pParameters);

        // construct the RAT panel
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
        c.insets = new Insets(0, 10, 0, 0);
        m_pRAT.add(m_ratStatus, c);

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

        // construct the RAT Generator panel
        m_pRATGen.setLayout(new GridBagLayout());
        //put the general options and the unit-specific options into a single panel so they scroll together.
        JPanel pRATGenTop = new JPanel(new GridBagLayout());

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0.5;
        m_pRATGen.add(new JScrollPane(pRATGenTop), c);
        
        m_pRATGenOptions = new ForceGenerationOptionsPanel(ForceGenerationOptionsPanel.Use.RAT_GENERATOR);
        m_pFormationOptions = new ForceGenerationOptionsPanel(ForceGenerationOptionsPanel.Use.FORMATION_BUILDER);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.EAST;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0.0;
        pRATGenTop.add(m_pRATGenOptions, c);
        m_pRATGenOptions.setYear(m_clientgui.getClient().getGame().getOptions()
                .intOption("year"));
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.EAST;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0.0;
        pRATGenTop.add(m_pUnitTypeOptions, c);

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

        // formation builder tab
        m_pFormations.setLayout(new BorderLayout());

        m_pFormations.add(new JScrollPane(m_pFormationOptions), BorderLayout.CENTER);
        m_pFormationOptions.setYear(m_clientgui.getClient().getGame().getOptions()
                .intOption("year"));
        
        m_pForceGen = new ForceGeneratorViewUi(cl);

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
        m_pPreview.add(m_bRoll, c);
        c.gridx = 1;
        m_bAddAll.addActionListener(this);
        m_pPreview.add(m_bAddAll, c);
        c.gridx = 2;
        m_bAdd.addActionListener(this);
        m_pPreview.add(m_bAdd, c);
        c.gridx = 3;
        m_bClear.addActionListener(this);
        m_pPreview.add(m_bClear, c);
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
        m_pPreview.setMinimumSize(new Dimension(0,0));


        m_pMain.addTab(Messages.getString("RandomArmyDialog.BVtab"), m_spParameters);
        m_pMain.addTab(Messages.getString("RandomArmyDialog.RATtab"), m_pRAT);
        m_pMain.addTab(Messages.getString("RandomArmyDialog.RATGentab"), m_pRATGen);
        m_pMain.addTab(Messages.getString("RandomArmyDialog.Formationtab"), m_pFormations);
        m_pMain.addTab(Messages.getString("RandomArmyDialog.Forcetab"), m_pForceGen.getLeftPanel());
        m_pMain.addChangeListener(ev -> {
            if (m_pMain.getSelectedIndex() == TAB_FORCE_GENERATOR) {
                m_lRightCards.show(m_pRightPane, CARD_FORCE_TREE);
                this.m_bRandomSkills.setEnabled(false);
            } else {
                m_lRightCards.show(m_pRightPane, CARD_PREVIEW);
                this.m_bRandomSkills.setEnabled(true);
            }
        });
        
        m_pRightPane.add(m_pPreview, CARD_PREVIEW);
        m_pRightPane.add(m_pForceGen.getRightPanel(), CARD_FORCE_TREE);
        m_pRightPane.setMinimumSize(new Dimension(0,0));

        m_pSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_pMain, m_pRightPane);
        m_pSplit.setOneTouchExpandable(false);
        m_pSplit.setResizeWeight(0.5);

        // construct the main dialog
        setLayout(new BorderLayout());
        add(m_pButtons, BorderLayout.SOUTH);
        add(m_pSplit, BorderLayout.CENTER);
        validate();
        setLocationRelativeTo(cl.frame);
        
        m_pSplit.setDividerLocation(guip.getRndArmySplitPos());
        setSize(guip.getRndArmySizeWidth(), guip.getRndArmySizeHeight());
        setLocation(guip.getRndArmyPosX(), guip.getRndArmyPosY());

        adaptToGUIScale();
        
        m_client.getGame().addGameListener(gameListener);
        addWindowListener(windowListener);
    }

    @Override
    public void valueChanged(TreeSelectionEvent ev) {
        if (ev.getSource().equals(m_treeRAT)) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_treeRAT.getLastSelectedPathComponent();
            if (node == null) {
                return;
            }

            Object nodeInfo = node.getUserObject();
            if (node.isLeaf()) {
                String ratName = (String) nodeInfo;
                rug.setChosenRAT(ratName);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(m_bOK)) {
            if (m_pMain.getSelectedIndex() == TAB_FORCE_GENERATOR) {
                m_pForceGen.addChosenUnits((String) m_chPlayer.getSelectedItem());
            } else {
                ArrayList<Entity> entities = new ArrayList<>(
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
                        if (c.getGame().getPhase() != GamePhase.LOUNGE) {
                            e.setDeployRound(c.getGame().getRoundCount() + 1);
                            e.setGame(c.getGame());
                            // Set these to true, otherwise units reinforced in
                            // the movement turn are considered selectable
                            e.setDone(true);
                            e.setUnloaded(true);
                        }
                        entities.add(e);
                    } catch (EntityLoadingException ex) {
                        LogManager.getLogger().error(String.format("Unable to load Mek: %s: %s",
                                ms.getSourceFile(), ms.getEntryName()), ex);
                        return;
                    }
                }
                c.sendAddEntity(entities);
                armyModel.clearData();
                unitsModel.clearData();
            }
            
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
            for (int sel : m_lUnits.getSelectedRows()) {
                MechSummary m = unitsModel.getUnitAt(sel);
                armyModel.addUnit(m);
            }
        } else if (ev.getSource().equals(m_bAdvSearch)) {
            searchFilter=asd.showDialog();
            m_bAdvSearchClear.setEnabled(searchFilter!=null);
        } else if (ev.getSource().equals(m_bAdvSearchClear)) {
            searchFilter=null;
            m_bAdvSearchClear.setEnabled(false);
        } else if (ev.getSource().equals(m_bRoll)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                if (m_pMain.getSelectedIndex() == TAB_RAT) {
                    int units = Integer.parseInt(m_tUnits.getText());
                    if (units > 0) {
                        unitsModel.setData(RandomUnitGenerator.getInstance().generate(units));
                    }
                } else if (m_pMain.getSelectedIndex() == TAB_RAT_GENERATOR) {
                    int units = m_pRATGenOptions.getNumUnits();
                    if (units > 0 && generatedRAT != null && generatedRAT.getNumEntries() > 0) {
                        unitsModel.setData(generatedRAT.generateUnits(units));
                    }
                    //generateUnits removes salvage entries that have no units meeting criteria
                    ratModel.refreshData();
                } else if (m_pMain.getSelectedIndex() == TAB_FORMATION_BUILDER) {
                    ArrayList<MechSummary> unitList = new ArrayList<>();
                    FactionRecord fRec = m_pFormationOptions.getFaction();
                    FormationType ft = FormationType.getFormationType(m_pFormationOptions.getStringOption("formationType"));
                    List<UnitTable.Parameters> params = new ArrayList<>();
                    params.add(new UnitTable.Parameters(fRec,
                            m_pFormationOptions.getUnitType(),
                            m_pFormationOptions.getYear(),
                            m_pFormationOptions.getRating(), null,
                            ModelRecord.NETWORK_NONE,
                            EnumSet.noneOf(EntityMovementMode.class),
                            EnumSet.noneOf(MissionRole.class), 0, fRec));
                    List<Integer> numUnits = new ArrayList<>();
                    numUnits.add(m_pFormationOptions.getNumUnits());
                    
                    if (m_pFormationOptions.getIntegerOption("numOtherUnits") > 0) {
                        if (m_pFormationOptions.getIntegerOption("otherUnitType") >= 0) {
                            params.add(new UnitTable.Parameters(fRec,
                                    m_pFormationOptions.getIntegerOption("otherUnitType"),
                                    m_pFormationOptions.getYear(), m_pFormationOptions.getRating(), null,
                                    ModelRecord.NETWORK_NONE,
                                    EnumSet.noneOf(EntityMovementMode.class),
                                    EnumSet.noneOf(MissionRole.class), 0, fRec));
                            numUnits.add(m_pFormationOptions.getIntegerOption("numOtherUnits"));
                        } else if (m_pFormationOptions.getBooleanOption("mechBA")) {
                            // Make sure at least a number units equals to the number of BA points/squads are omni
                            numUnits.set(0, Math.min(m_pFormationOptions.getIntegerOption("numOtherUnits"),
                                    m_pFormationOptions.getNumUnits()));
                            if (m_pFormationOptions.getNumUnits() > m_pFormationOptions.getIntegerOption("numOtherUnits")) {
                                params.add(params.get(0).copy());
                                numUnits.add(m_pFormationOptions.getNumUnits() - m_pFormationOptions.getIntegerOption("numOtherUnits"));
                            }
                            params.get(0).getRoles().add(MissionRole.MECHANIZED_BA);
                            //BA do not count for formation rules; add as a separate formation
                        }
                    }
                    
                    if (ft != null) {
                        unitList.addAll(ft.generateFormation(params,
                                numUnits, m_pFormationOptions.getIntegerOption("network"), false));
                        if (!unitList.isEmpty() && (m_pFormationOptions.getIntegerOption("numOtherUnits") > 0)) {
                            if (m_pFormationOptions.getBooleanOption("mechBA")) {
                                // Try to generate the BA portion using the same formation type as
                                // the parent, otherwise generate randomly.
                                Parameters p = new Parameters(fRec, UnitType.BATTLE_ARMOR,
                                        m_pFormationOptions.getYear(), m_pFormationOptions.getRating(), null,
                                        ModelRecord.NETWORK_NONE,
                                        EnumSet.noneOf(EntityMovementMode.class),
                                        EnumSet.of(MissionRole.MECHANIZED_BA), 0, fRec);
                                List<MechSummary> ba = ft.generateFormation(p,
                                        m_pFormationOptions.getIntegerOption("numOtherUnits"),
                                        ModelRecord.NETWORK_NONE, true);
                                if (ba.isEmpty()) {
                                    ba = UnitTable.findTable(p).generateUnits(m_pFormationOptions.getIntegerOption("numOtherUnits"));
                                }
                                unitList.addAll(ba);
                            } else if (m_pFormationOptions.getBooleanOption("airLance")) {
                                UnitTable t = UnitTable.findTable(fRec, UnitType.AERO,
                                        m_pFormationOptions.getYear(), m_pFormationOptions.getRating(), null,
                                        ModelRecord.NETWORK_NONE,
                                        EnumSet.noneOf(EntityMovementMode.class),
                                        EnumSet.noneOf(MissionRole.class), 0, fRec);
                                MechSummary unit = t.generateUnit();
                                if (unit != null) {
                                    unitList.add(unit);
                                    MechSummary unit2 = t.generateUnit(ms -> ms.getChassis().equals(unit.getChassis()));
                                    unitList.add(Objects.requireNonNullElse(unit2, unit));
                                }
                            }
                        }
                    } else {
                        LogManager.getLogger().error("Could not find formation type " + m_pFormationOptions.getStringOption("formationType"));
                    }
                    unitsModel.setData(unitList);
                    m_pFormationOptions.updateGeneratedUnits(unitList);
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
            } catch (NumberFormatException ignored) {

            } finally {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        } else if (ev.getSource().equals(m_bGenerate)) {
            generateRAT();
        } else if (ev.getSource().equals(m_bAddToForce)) {
            for (int sel : m_lRAT.getSelectedRows()) {
                MechSummary ms = generatedRAT.getMechSummary(sel);
                if (ms != null) {
                    armyModel.addUnit(ms);
                }
            }
        } else if (ev.getSource().equals(rug)) {
            m_ratStatus.setText(Messages.getString("RandomArmyDialog.ratStatusDoneLoading"));
            updateRATs();
        } else if (ev.getSource().equals(m_bRandomSkills)) {
            new SkillGenerationDialog(m_clientgui.getFrame(), m_clientgui, new ArrayList<>()).showDialog();
        }
    }

    WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent evt) {
            saveWindowSettings();
        }

        private void saveWindowSettings() {
            GUIPreferences guip = GUIPreferences.getInstance();
            guip.setRndArmySizeHeight(getSize().height);
            guip.setRndArmySizeWidth(getSize().width);
            guip.setRndArmyPosX(getLocation().x);
            guip.setRndArmyPosY(getLocation().y);
            guip.setRndArmySplitPos(m_pSplit.getDividerLocation());
        }
    };
    
    private void updatePlayerChoice() {
        String lastChoice = (String) m_chPlayer.getSelectedItem();
        String clientName = m_clientgui.getClient().getName();
        m_chPlayer.removeAllItems();
        m_chPlayer.setEnabled(true);
        m_chPlayer.addItem(clientName);
        for (Client client : m_clientgui.getBots().values()) {
            Player player = m_client.getGame().getPlayer(client.getLocalPlayerNumber());

            if (!player.isObserver()) {
                m_chPlayer.addItem(client.getName());
            }
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
        final int gameTL = TechConstants.getSimpleLevel(m_client.getGame()
                .getOptions().stringOption(OptionsConstants.ALLOWED_TECHLEVEL));
        final int maxTech;
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
                break;
        }

        m_chType.removeAllItems();
        for (int i = 0; i <= maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        m_chType.setSelectedIndex(Math.min(GUIPreferences.getInstance().getRATTechLevel(), maxTech));
    }

    private void updateRATs() {
        Iterator<String> rats = rug.getRatList();
        if (null == rats) {
            return;
        }  

        RatTreeNode ratTree = rug.getRatTree();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ratTree.name);
        createRatTreeNodes(root, ratTree);
        m_treeRAT.setModel(new DefaultTreeModel(root));
        
        String selectedRATPath = GUIPreferences.getInstance().getRATSelectedRAT();
        if (!selectedRATPath.isBlank()) {
            String[] nodes = selectedRATPath.replace('[', ' ')
                    .replace(']', ' ').split(",");
            TreePath path = findPathByName(nodes);
            m_treeRAT.setSelectionPath(path);
        }
    }
    
    private void updateRATYear() {
        int gameYear = m_clientgui.getClient().getGame().getOptions().intOption("year");
        m_pRATGenOptions.setYear(gameYear);
        m_pFormationOptions.setYear(gameYear);
        m_pForceGen.setYear(gameYear);
    }

    private void createRatTreeNodes(DefaultMutableTreeNode parentNode, RatTreeNode ratTreeNode) {
        for (RatTreeNode child : ratTreeNode.children) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(child.name);
            if (!child.children.isEmpty()) {
                createRatTreeNodes(newNode, child);
            }
            parentNode.add(newNode);
        }
    }

    private TreePath findPathByName(String... nodeNames) {
        TreeNode root = (TreeNode) m_treeRAT.getModel().getRoot();
        return findNextNode(new TreePath(root), nodeNames, 0);
    }

    private TreePath findNextNode(TreePath parent, String[] nodes, int depth) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        String currNode = node.toString();

        // If equal, go down the branch
        if (currNode.equals(nodes[depth].trim())) {
            // If at end, return match
            if (depth == nodes.length - 1) {
                return parent;
            }

            // Traverse children
            if (node.getChildCount() >= 0) {
                for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
                    TreeNode n = (TreeNode) e.nextElement();
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

    @SuppressWarnings(value = "unchecked")
    private void generateRAT() {
        FactionRecord fRec = m_pRATGenOptions.getFaction();
        if (fRec != null) {
            generatedRAT = UnitTable.findTable(fRec, m_pRATGenOptions.getUnitType(),
                    m_pRATGenOptions.getYear(), m_pRATGenOptions.getRating(),
                    (List<Integer>) m_pRATGenOptions.getListOption("weightClasses"),
                    m_pRATGenOptions.getIntegerOption("networkMask"),
                    (List<EntityMovementMode>) m_pRATGenOptions.getListOption("motiveTypes"),
                    (List<MissionRole>) m_pRATGenOptions.getListOption("roles"),
                    m_pRATGenOptions.getIntegerOption("roleStrictness"));
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

        adaptToGUIScale();
        super.setVisible(show);
    }

    private void autoSetSkillsAndName(Entity e) {
        ClientPreferences cs = PreferenceManager.getClientPreferences();
        if (cs.useAverageSkills()) {
            m_client.getSkillGenerator().setRandomSkills(e, true);
        }

        for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
            if (cs.generateNames()) {
                Gender gender = RandomGenderGenerator.generate();
                e.getCrew().setGender(gender, i);
                e.getCrew().setName(RandomNameGenerator.getInstance()
                        .generate(gender, (String) m_chPlayer.getSelectedItem()), i);
            }
        }
    }

    /**
     * This class now listens for game settings changes and updates the RAT year whenever
     * the game year changes. Well, whenever any game settings changes.
     */
    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gameSettingsChange(GameSettingsChangeEvent evt) {
            if (!evt.isMapSettingsOnlyChange()) {
                updateRATYear();
            }
        }
    };
    
    /**
     * A table model for displaying units
     */
    public static class UnitTableModel extends AbstractTableModel {
        private static final int COL_UNIT = 0;
        private static final int COL_BV = 1;
        private static final int COL_MOVE = 2;
        private static final int N_COL = 3;

        private List<MechSummary> data;

        public UnitTableModel() {
            data = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        public void clearData() {
            data = new ArrayList<>();
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public void addUnit(MechSummary m) {
            data.add(m);
            fireTableDataChanged();
        }

        public void setData(List<MechSummary> mechs) {
            data = mechs;
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_UNIT:
                    return Messages.getString("RandomArmyDialog.colUnit");
                case COL_MOVE:
                    return Messages.getString("RandomArmyDialog.colMove");
                case COL_BV:
                    return Messages.getString("RandomArmyDialog.colBV");
                default:
                    return "??";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
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

        public List<MechSummary> getAllUnits() {
            return data;
        }
    }
    
    /**
     * A table model for displaying a generated RAT
     */
    public class RATTableModel extends AbstractTableModel {
        private static final int COL_WEIGHT = 0;
        private static final int COL_UNIT = 1;
        private static final int COL_CL_IS = 2;
        private static final int COL_BV = 3;
        private static final int N_COL = 4;

        @Override
        public int getRowCount() {
            if (generatedRAT == null) {
                return 0;
            }
            return generatedRAT.getNumEntries();
        }

        public void refreshData() {
            fireTableDataChanged();
        }

        @Override
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
                case COL_CL_IS:
                    return 20;
                default:
                    return 0;
            }
        }
        
        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_WEIGHT:
                    return Messages.getString("RandomArmyDialog.colWeight");
                case COL_UNIT:
                    return Messages.getString("RandomArmyDialog.colUnit");
                case COL_BV:
                    return Messages.getString("RandomArmyDialog.colBV");
                case COL_CL_IS:
                    return Messages.getString("RandomArmyDialog.colCLIS");
                default:
                    return "??";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
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
                    case COL_CL_IS:
                        return generatedRAT.getTechBase(row);
                }
            }
            return "";
        }
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }
}
