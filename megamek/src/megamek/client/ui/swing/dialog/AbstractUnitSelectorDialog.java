/*
 *  MechSelectorDialog.java - Copyright (C) 2002,2004 Josh Yockey
 *  Renamed UnitSelectorDialog - Jay Lawson <jaylawson39 at yahoo.com>
 *  Renamed AbstractUnitSelectorDialog - Copyright (c) 2020 - The MegaMek Team
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
package megamek.client.ui.swing.dialog;

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
import java.util.*;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter.SortKey;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
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
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.templates.TROView;
import megamek.common.util.sorter.NaturalOrderComparator;

/**
 * This is a heavily reworked version of the original MechSelectorDialog which
 * brings up a list of units for the player to select to add to their forces.
 * The original list has been changed to a sortable table and a text filter
 * is used for advanced searching.
 */
public abstract class AbstractUnitSelectorDialog extends JDialog implements Runnable, KeyListener,
        ActionListener, ListSelectionListener {
    //region Variable Declarations
    private static final long serialVersionUID = 8144354264100884817L;

    public static final String CLOSE_ACTION = "closeAction";
    public static final String SELECT_ACTION = "selectAction";

    public static final int ALLOWED_YEAR_ANY = 999999;

    protected static final int TECH_LEVEL_DISPLAY_IS = 0;
    protected static final int TECH_LEVEL_DISPLAY_CLAN = 1;
    protected static final int TECH_LEVEL_DISPLAY_IS_CLAN = 2;

    protected JButton buttonSelectClose;
    protected JButton buttonSelect;
    protected JButton buttonClose;
    protected JButton buttonShowBV;

    private JButton buttonAdvancedSearch;
    private JButton buttonResetSearch;
    protected JList<String> listTechLevel = new JList<>();
    /**
     * We need to map the selected index of listTechLevel to the actual TL it
     * belongs to
     */
    protected Map<Integer, Integer> techLevelListToIndex = new HashMap<>();
    protected JComboBox<String> comboUnitType = new JComboBox<>();
    protected JComboBox<String> comboWeight = new JComboBox<>();
    protected JLabel labelImage = new JLabel(""); //inline to avoid potential null pointer issues
    protected JTable tableUnits;
    protected JTextField textFilter;
    private MechViewPanel panelMechView;
    private MechViewPanel panelTROView;

    private StringBuffer searchBuffer = new StringBuffer();
    private long lastSearch = 0;
    // how long after a key is typed does a new search begin
    private static final int KEY_TIMEOUT = 1000;

    protected static MechSummaryCache mscInstance = MechSummaryCache.getInstance();
    protected MechSummary[] mechs;

    private MechTableModel unitModel = new MechTableModel();
    protected MechSearchFilter searchFilter;

    private UnitLoadingDialog unitLoadingDialog;
    private AdvancedSearchDialog asd;
    protected JFrame frame;

    protected TableRowSorter<MechTableModel> sorter;

    protected GameOptions gameOptions = null;
    protected boolean enableYearLimits = false;
    protected int allowedYear = ALLOWED_YEAR_ANY;
    protected boolean canonOnly = false;
    protected int gameTechLevel = TechConstants.T_SIMPLE_INTRO;
    protected int techLevelDisplayType = TECH_LEVEL_DISPLAY_IS_CLAN;
    //endregion Variable Declarations

    protected AbstractUnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog) {
        super(frame, Messages.getString("MechSelectorDialog.title"), true); //$NON-NLS-1$
        setName("UnitSelectorDialog");
        this.frame = frame;
        this.unitLoadingDialog = unitLoadingDialog;
        super.setVisible(false);
    }

    /**
     * This is used to update any values that are set based on individual options
     */
    public abstract void updateOptionValues();

    /**
     * This has been set up to permit preference implementation in anything that extends this
     */
    private void setUserPreferences() {
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        setSize(guiPreferences.getMechSelectorSizeWidth(), guiPreferences.getMechSelectorSizeHeight());

        comboUnitType.setSelectedIndex(guiPreferences.getMechSelectorUnitType());

        comboWeight.setSelectedIndex(guiPreferences.getMechSelectorWeightClass());

        updateTypeCombo(guiPreferences);

        List<SortKey> sortList = new ArrayList<>();
        try {
            sortList.add(new SortKey(guiPreferences.getMechSelectorSortColumn(),
                    SortOrder.valueOf(guiPreferences.getMechSelectorSortOrder())));
        } catch (Exception e) {
            MegaMek.getLogger().error("Failed to set based on user preferences, attempting to use default", e);

            sortList.add(new SortKey(guiPreferences.getMechSelectorDefaultSortColumn(),
                    SortOrder.valueOf(guiPreferences.getMechSelectorDefaultSortOrder())));
        }
        tableUnits.getRowSorter().setSortKeys(sortList);
        ((DefaultRowSorter<?, ?>) tableUnits.getRowSorter()).sort();

        tableUnits.invalidate(); // force re-layout of window
        pack();
    }

    protected void initialize() {
        initComponents();

        setLocationRelativeTo(frame);
        asd = new AdvancedSearchDialog(frame, allowedYear);
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        // To use the below you MUST AND ONLY modify the gridx and gridy components
        GridBagConstraints gridBagConstraintsWest = new GridBagConstraints();
        gridBagConstraintsWest.anchor = GridBagConstraints.WEST;

        setMinimumSize(new Dimension(640, 480));
        getContentPane().setLayout(new GridBagLayout());

        //region Unit Preview Pane
        JTabbedPane panePreview = new JTabbedPane();

        panelMechView = new MechViewPanel();
        panelMechView.setMinimumSize(new Dimension(300, 500));
        panelMechView.setPreferredSize(new Dimension(300, 600));
        panePreview.addTab("Summary", panelMechView);

        panelTROView = new MechViewPanel();
        panePreview.addTab("TRO", panelTROView);
        //endregion Unit Preview Pane

        //region Selection Panel
        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setMinimumSize(new Dimension(500, 500));
        selectionPanel.setPreferredSize(new Dimension(500, 600));

        tableUnits = new JTable(unitModel);
        tableUnits.setName("tableUnits");
        tableUnits.addKeyListener(this);
        tableUnits.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "");
        tableUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(unitModel);
        sorter.setComparator(MechTableModel.COL_CHASSIS, new NaturalOrderComparator());
        sorter.setComparator(MechTableModel.COL_MODEL, new NaturalOrderComparator());
        tableUnits.setRowSorter(sorter);
        tableUnits.getSelectionModel().addListSelectionListener(
                evt -> {
                    // There can be multiple events for one selection. Check to see if this is the last.
                    if (!evt.getValueIsAdjusting()) {
                        refreshUnitView();
                    }
                });
        TableColumn column;
        for (int i = 0; i < MechTableModel.N_COL; i++) {
            column = tableUnits.getColumnModel().getColumn(i);
            if (i == MechTableModel.COL_CHASSIS) {
                column.setPreferredWidth(125);
            } else if ((i == MechTableModel.COL_MODEL) || (i == MechTableModel.COL_COST)) {
                column.setPreferredWidth(75);
            } else if ((i == MechTableModel.COL_WEIGHT) || (i == MechTableModel.COL_BV)) {
                column.setPreferredWidth(50);
            } else {
                column.setPreferredWidth(25);
            }
        }
        tableUnits.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$

        JScrollPane scrollTableUnits = new JScrollPane(tableUnits);
        scrollTableUnits.setName("scrollTableUnits");
        scrollTableUnits.setMinimumSize(new Dimension(500, 400));
        scrollTableUnits.setPreferredSize(new Dimension(500, 400));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        selectionPanel.add(scrollTableUnits, gridBagConstraints);

        JPanel panelFilterButtons = new JPanel(new GridBagLayout());
        panelFilterButtons.setMinimumSize(new Dimension(300, 180));
        panelFilterButtons.setPreferredSize(new Dimension(300, 180));

        JLabel labelType = new JLabel(Messages.getString("MechSelectorDialog.m_labelType")); //$NON-NLS-1$
        labelType.setToolTipText(Messages.getString("MechSelectorDialog.m_labelType.ToolTip")); //$NON-NLS-1$
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 2;
        panelFilterButtons.add(labelType, gridBagConstraintsWest);

        listTechLevel.setToolTipText(Messages.getString("MechSelectorDialog.m_labelType.ToolTip")); //$NON-NLS-1$
        JScrollPane techLevelScroll = new JScrollPane(listTechLevel);
        techLevelScroll.setMinimumSize(new Dimension(300, 100));
        techLevelScroll.setPreferredSize(new Dimension(300, 100));
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 2;
        panelFilterButtons.add(techLevelScroll, gridBagConstraintsWest);

        JLabel labelWeight = new JLabel(Messages.getString("MechSelectorDialog.m_labelWeightClass")); //$NON-NLS-1$
        labelWeight.setName("labelWeight");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 1;
        panelFilterButtons.add(labelWeight, gridBagConstraintsWest);

        DefaultComboBoxModel<String> weightModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            weightModel.addElement(EntityWeightClass.getClassName(i));
        }
        weightModel.addElement(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        comboWeight.setModel(weightModel);
        comboWeight.setName("comboWeight");
        comboWeight.setMinimumSize(new Dimension(300, 27));
        comboWeight.setPreferredSize(new Dimension(300, 27));
        comboWeight.addActionListener(this);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 1;
        panelFilterButtons.add(comboWeight, gridBagConstraintsWest);

        JLabel labelUnitType = new JLabel(Messages.getString("MechSelectorDialog.m_labelUnitType")); //$NON-NLS-1$
        labelUnitType.setName("labelUnitType");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(labelUnitType, gridBagConstraints);

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        unitTypeModel.addElement(Messages.getString("MechSelectorDialog.All"));
        for (int i = 0; i < UnitType.SIZE; i++) {
            unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
        }
        unitTypeModel.addElement(Messages.getString("MechSelectorDialog.SupportVee"));
        comboUnitType.setModel(unitTypeModel);
        comboUnitType.setName("comboUnitType");
        comboUnitType.setMinimumSize(new Dimension(300, 27));
        comboUnitType.setPreferredSize(new Dimension(300, 27));
        comboUnitType.addActionListener(this);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 0;
        panelFilterButtons.add(comboUnitType, gridBagConstraintsWest);

        textFilter = new JTextField("");
        textFilter.setName("textFilter");
        textFilter.setMinimumSize(new Dimension(300, 28));
        textFilter.setPreferredSize(new Dimension(300, 28));
        textFilter.getDocument().addDocumentListener(new DocumentListener() {
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
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 3;
        panelFilterButtons.add(textFilter, gridBagConstraintsWest);

        JLabel labelFilter = new JLabel(Messages.getString("MechSelectorDialog.m_labelFilter")); //$NON-NLS-1$
        labelFilter.setName("labelFilter");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 3;
        panelFilterButtons.add(labelFilter, gridBagConstraintsWest);

        labelImage.setHorizontalAlignment(SwingConstants.CENTER);
        labelImage.setName("labelImage");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelFilterButtons.add(labelImage, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(10, 10, 5, 0);
        selectionPanel.add(panelFilterButtons, gridBagConstraints);

        JPanel panelSearchButtons = new JPanel(new GridBagLayout());

        buttonAdvancedSearch = new JButton(Messages.getString("MechSelectorDialog.AdvSearch"));
        buttonAdvancedSearch.setName("buttonAdvancedSearch");
        buttonAdvancedSearch.addActionListener(this);
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 0;
        panelSearchButtons.add(buttonAdvancedSearch, gridBagConstraintsWest);

        buttonResetSearch = new JButton(Messages.getString("MechSelectorDialog.Reset"));
        buttonResetSearch.setName("buttonResetSearch");
        buttonResetSearch.addActionListener(this);
        buttonResetSearch.setEnabled(false);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 0;
        panelSearchButtons.add(buttonResetSearch, gridBagConstraintsWest);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelSearchButtons, gridBagConstraints);
        //endregion Selection Panel

        JPanel panelButtons = createButtonsPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                selectionPanel, panePreview);
        splitPane.setResizeWeight(0);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = gridBagConstraints.weighty = 1;
        getContentPane().add(splitPane, gridBagConstraints);

        gridBagConstraints.insets = new Insets(5,0,5,0);
        gridBagConstraints.weightx = gridBagConstraints.weighty = 0;
        gridBagConstraints.gridy = 1;
        getContentPane().add(panelButtons, gridBagConstraints);

        pack();

        // Escape keypress
        Action closeAction = new AbstractAction() {
            private static final long serialVersionUID = 2587225044226668664L;

            public void actionPerformed(ActionEvent e) {
                close();
            }
        };

        Action selectAction = new AbstractAction() {
            private static final long serialVersionUID = 4043951169453748540L;

            public void actionPerformed(ActionEvent e) {
                select(false);
            }
        };

        JRootPane rootPane = getRootPane();
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escape, CLOSE_ACTION);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, CLOSE_ACTION);
        rootPane.getInputMap(JComponent.WHEN_FOCUSED).put(escape, CLOSE_ACTION);
        rootPane.getActionMap().put(CLOSE_ACTION, closeAction);

        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, SELECT_ACTION);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, SELECT_ACTION);
        rootPane.getInputMap(JComponent.WHEN_FOCUSED).put(enter, SELECT_ACTION);
        rootPane.getActionMap().put(SELECT_ACTION, selectAction);
    }

    private void updateTypeCombo(GUIPreferences preferences) {
        listTechLevel.removeListSelectionListener(this);
        int[] selectedIndices = listTechLevel.getSelectedIndices();

        if (selectedIndices.length == 0) {
            String option = preferences.getMechSelectorRulesLevels().replaceAll("[\\[\\]]", "");
            if (option.length() > 0) {
                String[] strSelections = option.split("[,]");
                selectedIndices = new int[strSelections.length];
                for (int i = 0; i < strSelections.length; i++) {
                    selectedIndices[i] = Integer.parseInt(strSelections[i].trim());
                }
            }
        }

        int maxTech;
        switch (gameTechLevel) {
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
            default:
                maxTech = TechConstants.T_CLAN_UNOFFICIAL;
                break;
        }

        techLevelListToIndex.clear();
        DefaultComboBoxModel<String> techModel = new DefaultComboBoxModel<>();
        int selectionIdx = 0;
        for (int tl = 0; tl <= maxTech; tl++) {
            if ((tl != TechConstants.T_IS_TW_ALL) && (tl != TechConstants.T_TW_ALL)) {
                switch (techLevelDisplayType) {
                    case TECH_LEVEL_DISPLAY_IS:
                        if (TechConstants.getTechName(tl).equals("Inner Sphere")
                                || TechConstants.getTechName(tl).contains("IS")) {
                            techLevelListToIndex.put(selectionIdx, tl);
                            techModel.addElement(TechConstants.getLevelDisplayableName(tl));
                            selectionIdx++;
                        }
                        break;
                    case TECH_LEVEL_DISPLAY_CLAN:
                        if (TechConstants.getTechName(tl).contains("Clan")) {
                            techLevelListToIndex.put(selectionIdx, tl);
                            techModel.addElement(TechConstants.getLevelDisplayableName(tl));
                            selectionIdx++;
                        }
                        break;
                    case TECH_LEVEL_DISPLAY_IS_CLAN:
                    default:
                        techLevelListToIndex.put(selectionIdx, tl);
                        techModel.addElement(TechConstants.getLevelDisplayableName(tl));
                        selectionIdx++;
                        break;
                }
            }
        }
        listTechLevel.setModel(techModel);

        listTechLevel.setSelectedIndices(selectedIndices);
        listTechLevel.addListSelectionListener(this);
    }

    /**
     * This is used to create the bottom row of buttons for the interface
     * @return the panel containing the buttons to place in the interface
     */
    protected abstract JPanel createButtonsPanel();

    /**
     * This is the function to add a unit to the current interface. That could be a purchase (MekHQ),
     * addition (MekHQ), or unit selection (MegaMek/MegaMekLab)
     * @param modifier a boolean to modify how the function will work. In MegaMek this is used to
     *                 close the dialog, in MekHQ to GM add.
     */
    protected abstract void select(boolean modifier);

    /**
     * This filters the units on the display. It is overwritten for MekHQ
     */
    protected void filterUnits() {
        RowFilter<MechTableModel, Integer> unitTypeFilter;

        List<Integer> techLevels = new ArrayList<>();
        for (Integer selectedIdx : listTechLevel.getSelectedIndices()) {
            techLevels.add(techLevelListToIndex.get(selectedIdx));
        }
        final Integer[] nTypes = new Integer[techLevels.size()];
        techLevels.toArray(nTypes);

        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        final boolean checkSupportVee = Messages.getString("MechSelectorDialog.SupportVee")
                .equals(comboUnitType.getSelectedItem());
        //If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<MechTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends MechTableModel, ? extends Integer> entry) {
                    MechTableModel mechModel = entry.getModel();
                    MechSummary mech = mechModel.getMechSummary(entry.getIdentifier());
                    boolean techLevelMatch = false;
                    int type = enableYearLimits ? mech.getType(allowedYear) : mech.getType();
                    for (int tl : nTypes) {
                        if (type == tl) {
                            techLevelMatch = true;
                            break;
                        }
                    }
                    if (
                            /* Year Limits */
                            (!enableYearLimits || (mech.getYear() <= allowedYear))
                            /* Canon */
                            && (!canonOnly || mech.isCanon())
                            /* Weight */
                            && ((nClass == EntityWeightClass.SIZE) || (nClass == mech.getWeightClass()))
                            /* Technology Level */
                            && (techLevelMatch)
                            /* Support Vehicles */
                            && ((nUnit == -1)
                                    || (!checkSupportVee && mech.getUnitType().equals(UnitType.getTypeName(nUnit)))
                                    || (checkSupportVee && mech.isSupport()))
                            /* Advanced Search */
                            && ((searchFilter == null) || MechSearchFilter.isMatch(mech, searchFilter))
                    ) {
                        if (textFilter.getText().length() > 0) {
                            String text = textFilter.getText();
                            return mech.getName().toLowerCase().contains(text.toLowerCase());
                        }
                        return true;
                    }
                    return false;
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }
        sorter.setRowFilter(unitTypeFilter);
    }

    /**
     * @return the selected entity (required for MekHQ/MegaMek overrides)
     */
    protected Entity refreshUnitView() {
        boolean populateTextFields = true;

        Entity selectedEntity = getSelectedEntity();
        // null entity, so load a default unit.
        if (selectedEntity == null) {
            panelMechView.reset();
            labelImage.setIcon(null);
            return null;
        }

        MechView mechView = null;
        TROView troView = null;
        try {
            mechView = new MechView(selectedEntity, false);
            troView = TROView.createView(selectedEntity, true);
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
            // error: unit didn't load right. this is bad news.
            populateTextFields = false;
        }
        if (populateTextFields) {
            panelMechView.setMech(selectedEntity, mechView);
            panelTROView.setMech(selectedEntity, troView);
        } else {
            panelMechView.reset();
            panelTROView.reset();
        }

        return selectedEntity;
    }

    /**
     * @return the selected entity
     */
    public Entity getSelectedEntity() {
        int view = tableUnits.getSelectedRow();
        if (view < 0) {
            // selection got filtered away
            return null;
        }
        int selected = tableUnits.convertRowIndexToModel(view);
        MechSummary ms = mechs[selected];
        try {
            // For some unknown reason the base path gets screwed up after you
            // print so this sets the source file to the full path.
            return new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (EntityLoadingException e) {
            MegaMek.getLogger().error("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName()
                            + ": " + e.getMessage(), e);
            return null;
        }
    }

    public void run() {
        // Loading mechs can take a while, so it will have its own thread for MegaMek
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.
        mechs = mscInstance.getAllMechs();
        unitLoadingDialog.setVisible(false);

        // break out if there are no units to filter
        if (mechs == null) {
            MegaMek.getLogger().error("No mechs were loaded");
        } else {
            unitModel.setData(mechs);
        }
    }

    /**
     *
     * @param visible whether or not to make the GUI visible
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setUserPreferences();
        }
        asd.clearValues();
        searchFilter = null;
        buttonResetSearch.setEnabled(false);
        filterUnits();

        super.setVisible(visible);
    }

    /**
     * This handles processing windows events
     * @param e the event to process
     */
    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            GUIPreferences guiPreferences = GUIPreferences.getInstance();
            guiPreferences.setMechSelectorUnitType(comboUnitType.getSelectedIndex());
            guiPreferences.setMechSelectorWeightClass(comboWeight.getSelectedIndex());
            guiPreferences.setMechSelectorRulesLevels(Arrays.toString(listTechLevel.getSelectedIndices()));
            guiPreferences.setMechSelectorSortColumn(tableUnits.getRowSorter().getSortKeys().get(0).getColumn());
            guiPreferences.setMechSelectorSortOrder(tableUnits.getRowSorter().getSortKeys().get(0).getSortOrder().name());
            guiPreferences.setMechSelectorSizeHeight(getSize().height);
            guiPreferences.setMechSelectorSizeWidth(getSize().width);
        }
    }

    /**
     * This handles key released events
     * @param ke the key that was released
     */
    public void keyReleased(KeyEvent ke) {
    }

    /**
     * This handles key pressed events
     * @param ke the pressed key
     */
    public void keyPressed(KeyEvent ke) {
        long curTime = System.currentTimeMillis();
        if ((curTime - lastSearch) > KEY_TIMEOUT) {
            searchBuffer = new StringBuffer();
        }
        lastSearch = curTime;
        searchBuffer.append(ke.getKeyChar());
        searchFor(searchBuffer.toString().toLowerCase());
    }

    /**
     * Searches the table for any entity with a name that starts with the search string
     * @param search the search parameters
     */
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

    /**
     * This handles key typed events
     * @param ke the typed key
     */
    public void keyTyped(KeyEvent ke) {
    }

    /**
     * This handles the primary action events (any that can come from buttons in this class)
     * @param ev the event containing the performed action
     */
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(comboWeight) || ev.getSource().equals(comboUnitType)) {
            filterUnits();
        } else if (ev.getSource().equals(buttonSelect)) {
            select(false);
        } else if (ev.getSource().equals(buttonSelectClose)) {
            select(true);
        } else if (ev.getSource().equals(buttonClose)) {
            close();
        } else if (ev.getSource().equals(buttonShowBV)) {
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
        } else if (ev.getSource().equals(buttonAdvancedSearch)) {
            searchFilter = asd.showDialog();
            buttonResetSearch.setEnabled((searchFilter != null) && !searchFilter.isDisabled);
            filterUnits();
        } else if (ev.getSource().equals(buttonResetSearch)) {
            asd.clearValues();
            searchFilter = null;
            buttonResetSearch.setEnabled(false);
            filterUnits();
        }
    }

    private void close() {
        setVisible(false);
    }

    /**
     * This handles list selection events, which are only thrown by MegaMek/MegaMekLab
     * @param evt the event to process
     */
    @Override
    public void valueChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting() && evt.getSource().equals(listTechLevel)) {
            filterUnits();
        }
    }

    /**
     * A table model for displaying work items
     */
    protected class MechTableModel extends AbstractTableModel {
        //region Variable Declarations
        private static final long serialVersionUID = -5457068129532709857L;
        private static final int COL_CHASSIS = 0;
        private static final int COL_MODEL = 1;
        private static final int COL_WEIGHT = 2;
        private static final int COL_BV = 3;
        private static final int COL_YEAR = 4;
        private static final int COL_COST = 5;
        private static final int COL_LEVEL = 6;
        private static final int N_COL = 7;

        private MechSummary[] data = new MechSummary[0];
        //endregion Variable Declarations

        public int getRowCount() {
            return data.length;
        }

        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
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
        public Class<?> getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public MechSummary getMechSummary(int i) {
            return data[i];
        }

        // fill table with values
        public void setData(MechSummary[] ms) {
            data = ms;
            fireTableDataChanged();
        }

        public Object getValueAt(int row, int col) {
            if (data.length <= row) {
                return "?";
            }
            MechSummary ms = data[row];
            if (col == COL_MODEL) {
                return ms.getModel();
            } else if (col == COL_CHASSIS) {
                return ms.getChassis();
            } else if (col == COL_WEIGHT) {
                if ((gameOptions != null) && ms.getUnitType().equals("BattleArmor")) {
                    if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TACOPS_BA_WEIGHT)) {
                        return ms.getTOweight();
                    } else {
                        return ms.getTWweight();
                    }
                }
                return ms.getTons();
            } else if (col == COL_BV) {
                if ((gameOptions != null)
                        && gameOptions.booleanOption(OptionsConstants.ADVANCED_GEOMETRIC_MEAN_BV)) {
                    if (gameOptions.booleanOption(OptionsConstants.ADVANCED_REDUCED_OVERHEAT_MODIFIER_BV)) {
                        return ms.getRHGMBV();
                    } else {
                        return ms.getGMBV();
                    }
                } else if ((gameOptions != null)
                        && gameOptions.booleanOption(OptionsConstants.ADVANCED_REDUCED_OVERHEAT_MODIFIER_BV)) {
                    return ms.getRHBV();
                } else {
                    return ms.getBV();
                }
            } else if (col == COL_YEAR) {
                return ms.getYear();
            } else if (col == COL_COST) {
                return ms.getCost();
            } else if (col == COL_LEVEL) {
                if ((gameOptions != null)
                        && gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED)) {
                    return ms.getLevel(gameOptions.intOption(OptionsConstants.ALLOWED_YEAR));
                } else {
                    return ms.getLevel();
                }
            } else {
                return "?";
            }
        }
    }
}
