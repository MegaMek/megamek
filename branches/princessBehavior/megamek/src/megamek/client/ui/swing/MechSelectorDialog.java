/*
 * MechSelectorDialog.java - Copyright (C) 2002,2004 Josh Yockey
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter.SortKey;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSearchFilter;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechView;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 * This is a heavily reworked version of the original MechSelectorDialog which
 * brings up a list of units for the player to select to add to their forces.
 * The original list has been changed to a sortable table and a text filter
 * is used for advanced searching.
 */
public class MechSelectorDialog extends JDialog implements Runnable,
    KeyListener, ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 8144354264100884817L;

    private JButton btnSelectClose;
    private JButton btnSelect;
    private JButton btnClose;
    private JButton btnShowBV;
    private JButton btnAdvSearch;
    private JButton btnResetSearch;
    private JComboBox comboType;
    private JComboBox comboUnitType;
    private JComboBox comboWeight;
    private JLabel lblFilter;
    private JLabel lblImage;
    private JLabel lblType;
    private JLabel lblUnitType;
    private JLabel lblWeight;
    private JPanel panelFilterBtns;
    private JPanel panelSearchBtns;
    private JPanel panelOKBtns;
    private JScrollPane scrTableUnits;
    private JTable tableUnits;
    JTextField txtFilter;
    private MechViewPanel panelMekView;
    private JLabel lblPlayer;
    private JComboBox comboPlayer;
    private JPanel selectionPanel;
    private JSplitPane splitPane;

    private StringBuffer searchBuffer = new StringBuffer();
    private long lastSearch = 0;
    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;

    private MechSummary[] mechs;

    private MechTableModel unitModel;
    private MechSearchFilter searchFilter;

    Client client;
    private ClientGUI clientgui;
    private UnitLoadingDialog unitLoadingDialog;
    AdvancedSearchDialog asd;

    private TableRowSorter<MechTableModel> sorter;

    /** Creates new form UnitSelectorDialog */
    public MechSelectorDialog(ClientGUI cl, UnitLoadingDialog uld) {
        super(cl.frame, Messages.getString("MechSelectorDialog.title"), true); //$NON-NLS-1$
        client = cl.getClient();
        clientgui = cl;
        unitLoadingDialog = uld;

        unitModel = new MechTableModel();
        initComponents();
        setLocationRelativeTo(cl.frame);
        asd = new AdvancedSearchDialog(cl.frame);
    }

    private void initComponents() {
        setMinimumSize(new java.awt.Dimension(800, 600));

        GridBagConstraints c;

        selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setMinimumSize(new java.awt.Dimension(500, 500));
        selectionPanel.setPreferredSize(new java.awt.Dimension(500, 600));

        panelFilterBtns = new JPanel();
        panelSearchBtns = new JPanel();
        panelOKBtns = new JPanel();

        scrTableUnits = new JScrollPane();
        tableUnits = new JTable();
        tableUnits.addKeyListener(this);
        panelMekView = new MechViewPanel();
        panelMekView.setMinimumSize(new java.awt.Dimension(300, 500));
        panelMekView.setPreferredSize(new java.awt.Dimension(300, 600));

        comboType = new JComboBox();
        comboWeight = new JComboBox();
        comboUnitType = new JComboBox();
        txtFilter = new JTextField();

        btnSelect = new JButton();
        btnSelectClose = new JButton();
        btnClose = new JButton();
        btnShowBV = new JButton();
        btnAdvSearch = new JButton();
        btnResetSearch = new JButton();

        lblType = new JLabel(
                Messages.getString("MechSelectorDialog.m_labelType"));
        lblWeight = new JLabel(
                Messages.getString("MechSelectorDialog.m_labelWeightClass"));
        lblUnitType = new JLabel(
                Messages.getString("MechSelectorDialog.m_labelUnitType"));
        lblFilter = new JLabel(
                Messages.getString("MechSelectorDialog.m_labelFilter"));
        lblImage = new JLabel();
        lblPlayer = new JLabel(
                Messages.getString("MechSelectorDialog.m_labelPlayer"), SwingConstants.RIGHT); //$NON-NLS-1$
        comboPlayer = new JComboBox();

        getContentPane().setLayout(new GridBagLayout());

        scrTableUnits.setMinimumSize(new java.awt.Dimension(500, 400));
        scrTableUnits.setPreferredSize(new java.awt.Dimension(500, 400));

        tableUnits.setModel(unitModel);
        tableUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<MechTableModel>(unitModel);
        tableUnits.setRowSorter(sorter);
        tableUnits.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        // There can be multiple events for one selection. Check
                        // to
                        // see if this is the last.
                        if (!evt.getValueIsAdjusting()) {
                            refreshUnitView();
                        }
                    }
                });
        TableColumn column = null;
        for (int i = 0; i < MechTableModel.N_COL; i++) {
            column = tableUnits.getColumnModel().getColumn(i);
            if (i == MechTableModel.COL_CHASSIS) {
                column.setPreferredWidth(125);
            } else if ((i == MechTableModel.COL_MODEL)
                    || (i == MechTableModel.COL_COST)) {
                column.setPreferredWidth(75);
            } else if ((i == MechTableModel.COL_WEIGHT)
                    || (i == MechTableModel.COL_BV)) {
                column.setPreferredWidth(50);
            } else {
                column.setPreferredWidth(25);
            }
        }
        tableUnits.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        scrTableUnits.setViewportView(tableUnits);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        selectionPanel.add(scrTableUnits, c);

        panelFilterBtns.setMinimumSize(new java.awt.Dimension(300, 120));
        panelFilterBtns.setPreferredSize(new java.awt.Dimension(300, 120));
        panelFilterBtns.setLayout(new GridBagLayout());

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(lblType, c);

        DefaultComboBoxModel techModel = new DefaultComboBoxModel();
        for (int i = 0; i < TechConstants.SIZE; i++) {
            techModel.addElement(TechConstants.getLevelDisplayableName(i));
        }
        techModel.setSelectedItem(TechConstants.getLevelDisplayableName(0));
        comboType.setModel(techModel);
        comboType.setMinimumSize(new java.awt.Dimension(200, 27));
        comboType.setPreferredSize(new java.awt.Dimension(200, 27));
        comboType.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(comboType, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(lblWeight, c);

        DefaultComboBoxModel weightModel = new DefaultComboBoxModel();
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            weightModel.addElement(EntityWeightClass.getClassName(i));
        }
        weightModel.addElement(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        weightModel.setSelectedItem(EntityWeightClass.getClassName(0));
        comboWeight.setModel(weightModel);
        comboWeight.setSelectedItem(Messages
                .getString("MechSelectorDialog.All"));
        comboWeight.setMinimumSize(new java.awt.Dimension(200, 27));
        comboWeight.setPreferredSize(new java.awt.Dimension(200, 27));
        comboWeight.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(comboWeight, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(lblUnitType, c);

        DefaultComboBoxModel unitTypeModel = new DefaultComboBoxModel();
        unitTypeModel.addElement(Messages.getString("MechSelectorDialog.All"));
        unitTypeModel.setSelectedItem(Messages
                .getString("MechSelectorDialog.All"));
        for (int i = 0; i < UnitType.SIZE; i++) {
            unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
        }
        comboUnitType.setModel(unitTypeModel);
        comboUnitType.setMinimumSize(new java.awt.Dimension(200, 27));
        comboUnitType.setPreferredSize(new java.awt.Dimension(200, 27));
        comboUnitType.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(comboUnitType, c);

        txtFilter.setText("");
        txtFilter.setMinimumSize(new java.awt.Dimension(200, 28));
        txtFilter.setPreferredSize(new java.awt.Dimension(200, 28));
        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                filterUnits();
            }

            public void insertUpdate(DocumentEvent e) {
                filterUnits();
            }

            public void removeUpdate(DocumentEvent e) {
                filterUnits();
            }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(txtFilter, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        panelFilterBtns.add(lblFilter, c);

        lblImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblImage.setText(""); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 4;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panelFilterBtns.add(lblImage, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.insets = new java.awt.Insets(10, 10, 10, 0);
        selectionPanel.add(panelFilterBtns, c);

        panelSearchBtns.setLayout(new GridBagLayout());

        btnAdvSearch
                .setText(Messages.getString("MechSelectorDialog.AdvSearch")); //$NON-NLS-1$
        btnAdvSearch.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelSearchBtns.add(btnAdvSearch, c);

        btnResetSearch.setText(Messages.getString("MechSelectorDialog.Reset")); //$NON-NLS-1$
        btnResetSearch.addActionListener(this);
        btnResetSearch.setEnabled(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridwidth = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelSearchBtns.add(btnResetSearch, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.insets = new java.awt.Insets(10, 10, 10, 0);
        selectionPanel.add(panelSearchBtns, c);

        panelOKBtns.setLayout(new GridBagLayout());

        btnSelect.setText(Messages.getString("MechSelectorDialog.m_bPick"));
        btnSelect.addActionListener(this);
        panelOKBtns.add(btnSelect, new GridBagConstraints());

        btnSelectClose.setText(Messages
                .getString("MechSelectorDialog.m_bPickClose"));
        btnSelectClose.addActionListener(this);
        panelOKBtns.add(btnSelectClose, new GridBagConstraints());

        btnClose.setText(Messages.getString("Close"));
        btnClose.addActionListener(this);
        panelOKBtns.add(btnClose, new GridBagConstraints());

        updatePlayerChoice();
        panelOKBtns.add(lblPlayer, new GridBagConstraints());
        panelOKBtns.add(comboPlayer, new GridBagConstraints());

        btnShowBV.setText(Messages.getString("MechSelectorDialog.BV")); //$NON-NLS-1$
        btnShowBV.addActionListener(this);
        panelOKBtns.add(btnShowBV, new GridBagConstraints());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                selectionPanel, panelMekView);
        splitPane.setResizeWeight(0);
        c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1;
        getContentPane().add(splitPane, c);
        c.insets = new Insets(5,0,5,0);
        c.weightx = c.weighty = 0;
        c.gridy = 1;
        getContentPane().add(panelOKBtns, c);

        pack();
    }

    void select(boolean close) {
        Entity e = getSelectedEntity();
        if (null != e) {
            Client c = null;
            if (comboPlayer.getSelectedIndex() > 0) {
                String name = (String) comboPlayer.getSelectedItem();
                c = clientgui.getBots().get(name);
            }
            if (c == null) {
                c = client;
            }
            autoSetSkillsAndName(e);
            e.setOwner(c.getLocalPlayer());
            c.sendAddEntity(e);
        }
        if (close) {
            setVisible(false);
        }
    }

    void filterUnits() {
        RowFilter<MechTableModel, Integer> unitTypeFilter = null;
        final int nType = comboType.getSelectedIndex();
        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        // If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<MechTableModel, Integer>() {
                @Override
                public boolean include(
                        Entry<? extends MechTableModel, ? extends Integer> entry) {
                    MechTableModel mechModel = entry.getModel();
                    MechSummary mech = mechModel.getMechSummary(entry
                            .getIdentifier());
                    if (/* Weight */
                    ((nClass == EntityWeightClass.SIZE) || (mech
                            .getWeightClass() == nClass))
                            &&
                            /* Canon */
                            (!client.game.getOptions().booleanOption(
                                    "canon_only") || mech.isCanon())
                            &&
                            /* Technology Level */
                            ((nType == TechConstants.T_ALL)
                                    || (nType == mech.getType())
                                    || ((nType == TechConstants.T_IS_TW_ALL) && ((mech
                                            .getType() <= TechConstants.T_IS_TW_NON_BOX) || (mech
                                            .getType() == TechConstants.T_INTRO_BOXSET)))
                                    || ((nType == TechConstants.T_TW_ALL) && ((mech
                                            .getType() <= TechConstants.T_IS_TW_NON_BOX)
                                            || (mech.getType() <= TechConstants.T_INTRO_BOXSET) || (mech
                                            .getType() <= TechConstants.T_CLAN_TW)))
                                    || ((nType == TechConstants.T_ALL_IS) && ((mech
                                            .getType() <= TechConstants.T_IS_TW_NON_BOX)
                                            || (mech.getType() == TechConstants.T_INTRO_BOXSET)
                                            || (mech.getType() == TechConstants.T_IS_ADVANCED)
                                            || (mech.getType() == TechConstants.T_IS_EXPERIMENTAL) || (mech
                                            .getType() == TechConstants.T_IS_UNOFFICIAL))) || ((nType == TechConstants.T_ALL_CLAN) && ((mech
                                    .getType() == TechConstants.T_CLAN_TW)
                                    || (mech.getType() == TechConstants.T_CLAN_ADVANCED)
                                    || (mech.getType() == TechConstants.T_CLAN_EXPERIMENTAL) || (mech
                                    .getType() == TechConstants.T_CLAN_UNOFFICIAL))))
                            && ((nUnit == -1) || mech.getUnitType().equals(
                                    UnitType.getTypeName(nUnit)))
                            /* Advanced Search */
                            && ((searchFilter == null) || MechSearchFilter
                                    .isMatch(mech, searchFilter))) {
                        // yuck, I have to pull up a full Entity to get MechView
                        // to search in
                        // TODO: why not put mechview into the mech summary
                        // itself?
                        if (txtFilter.getText().length() > 0) {
                            // TODO: this search routine is too slow
                            // I think putting a copy of the mechreadout in
                            // the mechsummary would speed things up enormously
                            // NOTE: now getting weirdness on txtFilter when I
                            // do this
                            String text = txtFilter.getText();
                            // String [] ind_words = text.split(" "); //split
                            // with regex as space
                            /*
                             * MechView mv = null; try { Entity entity = new
                             * MechFileParser(mech.getSourceFile(),
                             * mech.getEntryName()).getEntity(); mv = new
                             * MechView(entity, true); } catch
                             * (EntityLoadingException ex) { // do nothing, I
                             * guess } if(null == mv) { return false; }
                             */
                            /*
                             * boolean match = true; for(int i = 0; i <
                             * ind_words.length; i++) {
                             * if(!mv.getMechReadout().contains(ind_words[i])) {
                             * match = false; break; } } return match;
                             */
                            return mech.getName().toLowerCase()
                                    .contains(text.toLowerCase());
                        }
                        return true;
                    }
                    return false;
                }
            };
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(unitTypeFilter);
    }

    private void updatePlayerChoice() {
        String lastChoice = (String) comboPlayer.getSelectedItem();
        String clientName = clientgui.getClient().getName();
        comboPlayer.removeAllItems();
        comboPlayer.setEnabled(true);
        comboPlayer.addItem(clientName);
        for (Client client : clientgui.getBots().values()) {
            comboPlayer.addItem(client.getName());
        }
        if (comboPlayer.getItemCount() == 1) {
            comboPlayer.setEnabled(false);
        }
        comboPlayer.setSelectedItem(lastChoice);
        if (comboPlayer.getSelectedIndex() < 0) {
            comboPlayer.setSelectedIndex(0);
        }
    }

    void refreshUnitView() {
        boolean populateTextFields = true;

        Entity selectedUnit = getSelectedEntity();
        // null entity, so load a default unit.
        if (selectedUnit == null) {
            panelMekView.reset();
            lblImage.setIcon(null);
            return;
        }

        MechView mechView = null;
        try {
            mechView = new MechView(selectedUnit, false);
        } catch (Exception e) {
            e.printStackTrace();
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }
        if (populateTextFields && (mechView != null)) {
            panelMekView.setMech(selectedUnit, mechView);
        } else {
            panelMekView.reset();
        }

        clientgui.loadPreviewImage(lblImage, selectedUnit,
                client.getLocalPlayer());
    }

    public Entity getSelectedEntity() {
        int view = tableUnits.getSelectedRow();
        if (view < 0) {
            // selection got filtered away
            return null;
        }
        int selected = tableUnits.convertRowIndexToModel(view);
        // else
        MechSummary ms = mechs[selected];
        try {
            // For some unknown reason the base path gets screwed up after you
            // print so this sets the source file to the full path.
            Entity entity = new MechFileParser(ms.getSourceFile(),
                    ms.getEntryName()).getEntity();
            return entity;
        } catch (EntityLoadingException ex) {
            System.out.println("Unable to load mech: " + ms.getSourceFile()
                    + ": " + ms.getEntryName() + ": " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private void autoSetSkillsAndName(Entity e) {
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        if (cs.useAverageSkills()) {
            int skills[] = client.getRandomSkillsGenerator().getRandomSkills(e,
                    true);

            int gunnery = skills[0];
            int piloting = skills[1];

            e.getCrew().setGunnery(gunnery);
            e.getCrew().setPiloting(piloting);
        }
        if(cs.generateNames()) {
            e.getCrew().setName(client.getRandomNameGenerator().generate());
        }
    }

     public void run() {
         // Loading mechs can take a while, so it will have its own thread.
         // This prevents the UI from freezing, and allows the
         // "Please wait..." dialog to behave properly on various Java VMs.
         MechSummaryCache mscInstance = MechSummaryCache.getInstance();
         mechs = mscInstance.getAllMechs();

         // break out if there are no units to filter
         if (mechs == null) {
             System.err.println("No units to filter!");
         } else {
             unitModel.setData(mechs);
         }
         filterUnits();

         //initialize with the units sorted alphabetically by chassis
         ArrayList<SortKey> sortlist = new ArrayList<SortKey>();
         sortlist.add(new SortKey(MechTableModel.COL_CHASSIS,SortOrder.ASCENDING));
         //sortlist.add(new RowSorter.SortKey(MechTableModel.COL_MODEL,SortOrder.ASCENDING));
         tableUnits.getRowSorter().setSortKeys(sortlist);
         ((DefaultRowSorter<?, ?>)tableUnits.getRowSorter()).sort();

         tableUnits.invalidate(); // force re-layout of window
         pack();
         //setLocation(computeDesiredLocation());

         unitLoadingDialog.setVisible(false);

         // In some cases, it's possible to get here without an initialized 
         // instance (loading a saved game without a cahce).  In these cases,
         // we dn't care about the failed loads.
         if (mscInstance.isInitialized())
         {
             final Map<String, String> hFailedFiles = 
                 MechSummaryCache.getInstance().getFailedFiles();
             if ((hFailedFiles != null) && (hFailedFiles.size() > 0)) {
                 // self-showing dialog
                 new UnitFailureDialog(clientgui.frame, hFailedFiles);
             }
         }
     }

     @Override
     public void setVisible(boolean visible) {
         if (visible){             
             GUIPreferences guip = GUIPreferences.getInstance();
             comboUnitType.setSelectedIndex(guip.getMechSelectorUnitType());
             comboWeight.setSelectedIndex(guip.getMechSelectorWeightClass());
             comboType.setSelectedIndex(guip.getMechSelectorRulesLevel());
         }
         asd.clearValues();
         searchFilter=null;
         btnResetSearch.setEnabled(false);
         updatePlayerChoice();
         //FIXME: this is not updating the table when canonicity is selected/deselected until user clicks it
         filterUnits();
         super.setVisible(visible);
     }
     
     protected void processWindowEvent(WindowEvent e){
         super.processWindowEvent(e);         
         if (e.getID() == WindowEvent.WINDOW_DEACTIVATED){
             GUIPreferences guip = GUIPreferences.getInstance();
             guip.setMechSelectorUnitType(comboUnitType.getSelectedIndex());
             guip.setMechSelectorWeightClass(comboWeight.getSelectedIndex());
             guip.setMechSelectorRulesLevel(comboType.getSelectedIndex()); 
         }
     }


    /**
     * A table model for displaying work items
     */
    public class MechTableModel extends AbstractTableModel {

            /**
             *
             */
            private static final long serialVersionUID = -5457068129532709857L;
            private final static int COL_CHASSIS = 0;
            private final static int COL_MODEL = 1;
            private final static int COL_WEIGHT = 2;
            private final static int COL_BV = 3;
            private final static int COL_YEAR = 4;
            private final static int COL_COST = 5;
            private final static int COL_LEVEL = 6;
            private final static int N_COL = 7;

            private MechSummary[] data = new MechSummary[0];

            public int getRowCount() {
                return data.length;
            }

            public int getColumnCount() {
                return N_COL;
            }

            @Override
            public String getColumnName(int column) {
                switch(column) {
                    case COL_MODEL:
                        return "Model";
                    case COL_CHASSIS:
                        return "Chassis";
                    case COL_WEIGHT:
                        return "Weight";
                    case COL_BV:
                        return "BV";
                    case COL_YEAR:
                        return "Year";
                    case COL_COST:
                        return "Price";
                    case COL_LEVEL:
                         return "Level";
                    default:
                        return "?";
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

            public MechSummary getMechSummary(int i) {
                return data[i];
            }

            //fill table with values
            public void setData(MechSummary[] ms) {
                data = ms;
                fireTableDataChanged();
            }

            public Object getValueAt(int row, int col) {
                if (data.length <= row)
                    return "?";
                
                MechSummary ms = data[row];
                if(col == COL_MODEL) {
                    return ms.getModel();
                }
                if(col == COL_CHASSIS) {
                    return ms.getChassis();
                }
                if(col == COL_WEIGHT) {
                    return ms.getTons();
                }
                if(col == COL_BV) {
                    return ms.getBV();
                }
                if(col == COL_YEAR) {
                    return ms.getYear();
                }
                if(col == COL_COST) {
                    //return NumberFormat.getInstance().format(ms.getCost());
                    return ms.getCost();
                }
                if (col == COL_LEVEL) {
                    return ms.getLevel();
                }
                return "?";
            }

    }

    public void keyReleased(KeyEvent ke) {
    }

    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(btnSelect,
                    ActionEvent.ACTION_PERFORMED, ""); //$NON-NLS-1$
            actionPerformed(event);
        }
        long curTime = System.currentTimeMillis();
        if ((curTime - lastSearch) > KEY_TIMEOUT) {
            searchBuffer = new StringBuffer();
        }
        lastSearch = curTime;
        searchBuffer.append(ke.getKeyChar());
        searchFor(searchBuffer.toString().toLowerCase());
    }

    public void keyTyped(KeyEvent ke) {
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(comboType)
                || ev.getSource().equals(comboWeight)
                || ev.getSource().equals(comboUnitType)) {
            filterUnits();
        } else if (ev.getSource().equals(btnSelect)) {
            select(false);
        } else if (ev.getSource().equals(btnSelectClose)) {
            select(true);
        } else if (ev.getSource().equals(btnClose)) {
            setVisible(false);
        } else if (ev.getSource().equals(btnShowBV)) {
            JEditorPane tEditorPane = new JEditorPane();
            tEditorPane.setContentType("text/html");
            tEditorPane.setEditable(false);
            Entity e = getSelectedEntity();
            if (null == e) {
                return;
            }
            e.calculateBattleValue();
            tEditorPane.setText(e.getBVText());
            tEditorPane.setCaretPosition(0);
            JScrollPane tScroll = new JScrollPane(tEditorPane,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Dimension size = new Dimension(550, 300);
            tScroll.setPreferredSize(size);
            JOptionPane.showMessageDialog(null, tScroll, "BV",
                    JOptionPane.INFORMATION_MESSAGE, null);
        } else if (ev.getSource().equals(btnAdvSearch)) {
            searchFilter = asd.showDialog();
            btnResetSearch.setEnabled(searchFilter != null);
            // TurretFacingDialog tfd = new TurretFacingDialog(clientgui.frame,
            // "test", "test2", (Mech)getSelectedEntity(), null, clientgui);
            // tfd.setVisible(true);
            filterUnits();
        } else if (ev.getSource().equals(btnResetSearch)) {
            asd.clearValues();
            searchFilter = null;
            btnResetSearch.setEnabled(false);
            filterUnits();
        }
    }

    private void searchFor(String search) {
        for (int i = 0; i < mechs.length; i++) {
            if (mechs[i].getName().toLowerCase().startsWith(search)) {
                int selected = tableUnits.convertRowIndexToView(i);
                if (selected > -1) {
                    tableUnits.changeSelection(selected, 0, false, false);
                    break;
                }
            }
        }
    }

    public void enableResetButton(boolean b) {
        btnResetSearch.setEnabled(b);
    }
 }
