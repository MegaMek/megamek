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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import megamek.client.Client;
import megamek.client.RandomUnitGenerator;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSearchFilter;
import megamek.common.MechSummary;
import megamek.common.TechConstants;
import megamek.common.IGame.Phase;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.RandomArmyCreator;

public class RandomArmyDialog extends JDialog implements ActionListener,
WindowListener, TreeSelectionListener {

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
    private JPanel m_pParameters = new JPanel();
    private JPanel m_pPreview = new JPanel();
    private JPanel m_pButtons = new JPanel();
    private JPanel m_pAdvSearch = new JPanel();
    private JButton m_bOK = new JButton(Messages.getString("Okay"));
    private JButton m_bCancel = new JButton(Messages.getString("Cancel"));
    private JButton m_bAdvSearch = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearch"));
    private JButton m_bAdvSearchClear = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearchClear"));

    private JSplitPane m_pSplit;

    private JButton m_bAddAll = new JButton(Messages.getString("RandomArmyDialog.AddAll"));
    private JButton m_bAdd = new JButton(Messages.getString("RandomArmyDialog.AddSelected"));
    private JButton m_bRoll = new JButton(Messages.getString("RandomArmyDialog.Roll"));
    private JButton m_bClear = new JButton(Messages.getString("RandomArmyDialog.Clear"));

    private JTable m_lArmy;
    private JTable m_lUnits;

    private UnitTableModel armyModel;
    private UnitTableModel unitsModel;


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
        updatePlayerChoice();
        asd = new AdvancedSearchDialog(m_clientgui.frame,
                m_client.getGame().getOptions().intOption("year"));
        
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
        "canon_only"));
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
        } else if (ev.getSource().equals(rug)) {
            m_ratStatus.setText(Messages
                    .getString("RandomArmyDialog.ratStatusDoneLoading"));
            updateRATs();
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
}