/*
 * MechSelectorDialog.java - Copyright (C) 2002, 2004 Josh Yockey
 * Renamed UnitSelectorDialog - Jay Lawson <jaylawson39 at yahoo.com>
 * Renamed AbstractUnitSelectorDialog - Copyright (c) 2020 - The MegaMek Team
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
package megamek.client.ui.swing.dialog;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.BVDisplayDialog;
import megamek.client.ui.models.XTableColumnModel;
import megamek.client.ui.panes.EntityViewPane;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.util.sorter.NaturalOrderComparator;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.regex.PatternSyntaxException;

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
    private final JToggleButton buttonPvToggle = new JToggleButton(Messages.getString("MechSelectorDialog.TogglePV"));
    protected JList<String> listTechLevel = new JList<>();
    /**
     * We need to map the selected index of listTechLevel to the actual TL it
     * belongs to
     */
    protected Map<Integer, Integer> techLevelListToIndex = new HashMap<>();
    protected JComboBox<String> comboUnitType = new JComboBox<>();
    protected JComboBox<String> comboWeight = new JComboBox<>();
    private JScrollPane techLevelScroll;
    private JPanel panelFilterButtons;
    protected JLabel labelImage = new JLabel(""); //inline to avoid potential null pointer issues
    protected JTable tableUnits;
    protected JTextField textFilter;
    protected EntityViewPane panePreview;
    private JPanel selectionPanel;
    private JSplitPane splitPane;

    private StringBuffer searchBuffer = new StringBuffer();
    private long lastSearch = 0;
    // how long after a key is typed does a new search begin
    private static final int KEY_TIMEOUT = 1000;

    protected static MechSummaryCache mscInstance = MechSummaryCache.getInstance();
    protected MechSummary[] mechs;

    private MechTableModel unitModel = new MechTableModel();
    private XTableColumnModel unitColumnModel = new XTableColumnModel();
    private TableColumn pvColumn;
    private TableColumn bvColumn;
    protected MechSearchFilter searchFilter;

    protected JFrame frame;
    private UnitLoadingDialog unitLoadingDialog;
    private AdvancedSearchDialog2 advancedSearchDialog2;

    protected TableRowSorter<MechTableModel> sorter;
    private JScrollPane scrollTableUnits;

    protected GameOptions gameOptions = null;
    protected boolean enableYearLimits = false;
    protected int allowedYear = ALLOWED_YEAR_ANY;
    protected boolean canonOnly = false;
    protected boolean allowInvalid = true;
    protected int gameTechLevel = TechConstants.T_SIMPLE_INTRO;
    protected int techLevelDisplayType = TECH_LEVEL_DISPLAY_IS_CLAN;
    //endregion Variable Declarations

    protected AbstractUnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog) {
        super(frame, Messages.getString("MechSelectorDialog.title"), true);
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

        comboUnitType.setSelectedIndex(guiPreferences.getMechSelectorUnitType());

        comboWeight.setSelectedIndex(guiPreferences.getMechSelectorWeightClass());

        updateTypeCombo(guiPreferences);

        List<SortKey> sortList = new ArrayList<>();
        try {
            sortList.add(new SortKey(guiPreferences.getMechSelectorSortColumn(),
                    SortOrder.valueOf(guiPreferences.getMechSelectorSortOrder())));
        } catch (Exception e) {
            LogManager.getLogger().error("Failed to set based on user preferences, attempting to use default", e);

            sortList.add(new SortKey(guiPreferences.getMechSelectorDefaultSortColumn(),
                    SortOrder.valueOf(guiPreferences.getMechSelectorDefaultSortOrder())));
        }
        tableUnits.getRowSorter().setSortKeys(sortList);
        ((DefaultRowSorter<?, ?>) tableUnits.getRowSorter()).sort();

        tableUnits.invalidate(); // force re-layout of window
        splitPane.setDividerLocation(guiPreferences.getMechSelectorSplitPos());
        setSize(guiPreferences.getMechSelectorSizeWidth(), guiPreferences.getMechSelectorSizeHeight());
        setLocation(guiPreferences.getMechSelectorPosX(), guiPreferences.getMechSelectorPosY());
    }

    protected void initialize() {
        initComponents();
        setLocationRelativeTo(frame);
    }

    private void initComponents() {
        advancedSearchDialog2 = new AdvancedSearchDialog2(frame, allowedYear);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        // To use the below you MUST AND ONLY modify the gridx and gridy components
        GridBagConstraints gridBagConstraintsWest = new GridBagConstraints();
        gridBagConstraintsWest.anchor = GridBagConstraints.WEST;

        setMinimumSize(new Dimension(640, 480));
        getContentPane().setLayout(new GridBagLayout());

        //region Unit Preview Pane
        panePreview = new EntityViewPane(frame, null);
        panePreview.setMinimumSize(new Dimension(0,0));
        panePreview.setPreferredSize(new Dimension(0,0));
        //endregion Unit Preview Pane

        //region Selection Panel
        selectionPanel = new JPanel(new GridBagLayout());

        tableUnits = new JTable(unitModel);
        tableUnits.setColumnModel(unitColumnModel);
        tableUnits.createDefaultColumnsFromModel();
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

        for (int i = 0; i < unitModel.getColumnCount(); i++) {
            tableUnits.getColumnModel().getColumn(i).setPreferredWidth(unitModel.getPreferredWidth(i));
        }
        bvColumn = tableUnits.getColumnModel().getColumn(MechTableModel.COL_BV);
        pvColumn = tableUnits.getColumnModel().getColumn(MechTableModel.COL_PV);
        tableUnits.setFont(new Font("Monospaced", Font.PLAIN, 12));
        togglePV(false);

        scrollTableUnits = new JScrollPane(tableUnits);
        scrollTableUnits.setName("scrollTableUnits");

        gridBagConstraints.insets = new Insets(5, 0, 0, 0);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        selectionPanel.add(scrollTableUnits, gridBagConstraints);

        panelFilterButtons = new JPanel(new GridBagLayout());

        JLabel labelType = new JLabel(Messages.getString("MechSelectorDialog.m_labelType"));
        labelType.setToolTipText(Messages.getString("MechSelectorDialog.m_labelType.ToolTip"));
        gridBagConstraintsWest.insets = new Insets(5, 0, 0, 0);
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 2;
        panelFilterButtons.add(labelType, gridBagConstraintsWest);

        listTechLevel.setToolTipText(Messages.getString("MechSelectorDialog.m_labelType.ToolTip"));
        techLevelScroll = new JScrollPane(listTechLevel);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 2;
        panelFilterButtons.add(techLevelScroll, gridBagConstraintsWest);

        JLabel labelWeight = new JLabel(Messages.getString("MechSelectorDialog.m_labelWeightClass"));
        labelWeight.setName("labelWeight");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 1;
        panelFilterButtons.add(labelWeight, gridBagConstraintsWest);

        DefaultComboBoxModel<String> weightModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            weightModel.addElement(EntityWeightClass.getClassName(i));
        }
        weightModel.addElement(Messages.getString("MechSelectorDialog.All"));
        comboWeight.setModel(weightModel);
        comboWeight.setName("comboWeight");
        comboWeight.addActionListener(this);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 1;
        panelFilterButtons.add(comboWeight, gridBagConstraintsWest);

        JLabel labelUnitType = new JLabel(Messages.getString("MechSelectorDialog.m_labelUnitType"));
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
        comboUnitType.addActionListener(this);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 0;
        panelFilterButtons.add(comboUnitType, gridBagConstraintsWest);

        textFilter = new JTextField("");
        textFilter.setName("textFilter");
        textFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterUnits();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterUnits();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterUnits();
            }
        });
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 3;
        gridBagConstraintsWest.fill = GridBagConstraints.HORIZONTAL;
        panelFilterButtons.add(textFilter, gridBagConstraintsWest);
        gridBagConstraintsWest.fill = GridBagConstraints.NONE;

        JLabel labelFilter = new JLabel(Messages.getString("MechSelectorDialog.m_labelFilter"));
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

        buttonPvToggle.setName("buttonTogglePV");
        buttonPvToggle.addActionListener(e -> togglePV(buttonPvToggle.isSelected()));
        gridBagConstraintsWest.gridx = 2;
        gridBagConstraintsWest.gridy = 0;
        panelSearchButtons.add(buttonPvToggle, gridBagConstraintsWest);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelSearchButtons, gridBagConstraints);
        //endregion Selection Panel

        JScrollPane selectionScrollPane = new JScrollPane(selectionPanel);
        JScrollPane previewScrollPane = new JScrollPane(panePreview);

        JPanel panelButtons = createButtonsPanel();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                selectionScrollPane, previewScrollPane);
        splitPane.setResizeWeight(0);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = gridBagConstraints.weighty = 1;
        getContentPane().add(splitPane, gridBagConstraints);

        gridBagConstraints.insets = new Insets(5, 0, 5, 0);
        gridBagConstraints.weightx = gridBagConstraints.weighty = 0;
        gridBagConstraints.gridy = 1;
        getContentPane().add(panelButtons, gridBagConstraints);

        pack();

        // Escape keypress
        Action closeAction = new AbstractAction() {
            private static final long serialVersionUID = 2587225044226668664L;

            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        };

        Action selectAction = new AbstractAction() {
            private static final long serialVersionUID = 4043951169453748540L;

            @Override
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
            if (!option.isBlank()) {
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
        // If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<>() {
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
                                    /* Invalid units */
                                    && (allowInvalid || !mech.getLevel().equals("F"))
                                    /* Weight */
                                    && ((nClass == EntityWeightClass.SIZE) || (nClass == mech.getWeightClass()))
                                    /* Technology Level */
                                    && (techLevelMatch)
                                    /* Support Vehicles */
                                    && ((nUnit == -1) || (checkSupportVee && mech.isSupport())
                                            || (!checkSupportVee && mech.getUnitType().equals(UnitType.getTypeName(nUnit))))
                                    /* Advanced Search */
                                    && ((searchFilter == null) || MechSearchFilter.isMatch(mech, searchFilter))
                                    && advancedSearchDialog2.getASAdvancedSearch().matches(mech)) {
                        if (!textFilter.getText().isBlank()) {
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
        Entity selectedEntity = getSelectedEntity();
        panePreview.updateDisplayedEntity(selectedEntity);
        // Empty the unit preview icon if there's no entity selected
        if (selectedEntity == null) {
            labelImage.setIcon(null);
        }
        return selectedEntity;
    }

    /**
     * @return the selected entity
     */
    public @Nullable Entity getSelectedEntity() {
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
        } catch (Exception e) {
            LogManager.getLogger().error("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName()
                            + ": " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void run() {
        // Loading mechs can take a while, so it will have its own thread for MegaMek
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.
        mechs = mscInstance.getAllMechs();
        unitLoadingDialog.setVisible(false);

        // break out if there are no units to filter
        if (mechs == null) {
            LogManager.getLogger().error("No mechs were loaded");
        } else {
            SwingUtilities.invokeLater(() -> unitModel.setData(mechs));
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
        searchFilter = null;
        buttonResetSearch.setEnabled(false);
        filterUnits();

        if (visible) {
            adaptToGUIScale();
        }

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
            guiPreferences.setMechSelectorPosX(getLocation().x);
            guiPreferences.setMechSelectorPosY(getLocation().y);
            guiPreferences.setMechSelectorSplitPos(splitPane.getDividerLocation());
        }
    }

    /**
     * This handles key released events
     * @param ke the key that was released
     */
    @Override
    public void keyReleased(KeyEvent ke) {
    }

    /**
     * This handles key pressed events
     * @param ke the pressed key
     */
    @Override
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
    @Override
    public void keyTyped(KeyEvent ke) {
    }

    /**
     * This handles the primary action events (any that can come from buttons in this class)
     * @param ev the event containing the performed action
     */
    @Override
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
            final Entity entity = getSelectedEntity();
            if (entity != null) {
                new BVDisplayDialog(frame, true, entity).setVisible(true);
            }
        } else if (ev.getSource().equals(buttonAdvancedSearch)) {
            advancedSearchDialog2.setVisible(true);
            searchFilter = advancedSearchDialog2.getTWAdvancedSearch().getMechSearchFilter();
            setResetSearchEnabledStatus();
            filterUnits();
        } else if (ev.getSource().equals(buttonResetSearch)) {
            advancedSearchDialog2.clearSearches();
            searchFilter = null;
            setResetSearchEnabledStatus();
            filterUnits();
        }
    }

    private void setResetSearchEnabledStatus() {
        buttonResetSearch.setEnabled(((searchFilter != null) && !searchFilter.isDisabled)
                || advancedSearchDialog2.getASAdvancedSearch().isActive());
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

    /** Toggles between showing the Point Value column and the Battle Value column. */
    private void togglePV(boolean showPV) {
        unitColumnModel.setColumnVisible(pvColumn, showPV);
        unitColumnModel.setColumnVisible(bvColumn, !showPV);
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
        private static final int COL_PV = 4;
        private static final int COL_YEAR = 5;
        private static final int COL_COST = 6;
        private static final int COL_LEVEL = 7;
        private static final int N_COL = 8;

        private MechSummary[] data = new MechSummary[0];
        //endregion Variable Declarations

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public int getPreferredWidth(int col) {
            switch (col) {
                case COL_MODEL:
                    return 75;
                case COL_CHASSIS:
                    return 125;
                case COL_WEIGHT:
                    return 50;
                case COL_BV:
                    return 25;
                case COL_PV:
                    return 25;
                case COL_YEAR:
                    return 25;
                case COL_COST:
                    return 25;
                case COL_LEVEL:
                    return 25;
                default:
                    return 0;
            }
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
                case COL_PV:
                    return "PV";
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

        @Override
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
                return ms.getBV();
            } else if (col == COL_PV) {
                return ms.getPointValue();
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

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
        textFilter.setMinimumSize(new Dimension(UIUtil.scaleForGUI(200), UIUtil.scaleForGUI(28)));
        textFilter.setPreferredSize(new Dimension(UIUtil.scaleForGUI(200), UIUtil.scaleForGUI(28)));
        techLevelScroll.setMinimumSize(new Dimension(UIUtil.scaleForGUI(300), UIUtil.scaleForGUI(100)));
        techLevelScroll.setPreferredSize(new Dimension(UIUtil.scaleForGUI(300), UIUtil.scaleForGUI(100)));
    }
}
