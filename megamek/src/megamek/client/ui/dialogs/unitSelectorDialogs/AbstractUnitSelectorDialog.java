/*
 * Copyright (C) 2002, 2004 Josh Yockey
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.unitSelectorDialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.abstractDialogs.BVDisplayDialog;
import megamek.client.ui.dialogs.advancedsearch.AdvancedSearchDialog;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;
import megamek.client.ui.models.XTableColumnModel;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.battleValue.BVCalculator;
import megamek.common.internationalization.I18n;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;

/**
 * This is a heavily reworked version of the original MekSelectorDialog which brings up a list of units for the player
 * to select to add to their forces. The original list has been changed to a sortable table and a text filter is used
 * for advanced searching.
 */
public abstract class AbstractUnitSelectorDialog extends JDialog implements Runnable, KeyListener,
                                                                            ActionListener, ListSelectionListener {
    private static final MMLogger logger = MMLogger.create(AbstractUnitSelectorDialog.class);

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
    private final JToggleButton buttonPvToggle = new JToggleButton(Messages.getString("MekSelectorDialog.TogglePV"));
    protected JList<String> listTechLevel = new JList<>();
    private JLabel lblCount;
    /**
     * We need to map the selected index of listTechLevel to the actual TL it belongs to
     */
    protected Map<Integer, Integer> techLevelListToIndex = new HashMap<>();
    protected JComboBox<String> comboUnitType = new JComboBox<>();
    protected JComboBox<String> comboWeight = new JComboBox<>();
    protected JLabel labelImage = new JLabel(""); // inline to avoid potential null pointer issues
    protected JTable tableUnits;
    protected JTextField textFilter;
    protected JTextField textGunnery;
    protected JTextField textPilot;
    protected EntityViewPane panePreview;
    private JSplitPane splitPane;

    private StringBuffer searchBuffer = new StringBuffer();
    private long lastSearch = 0;
    // how long after a key is typed does a new search begin
    private static final int KEY_TIMEOUT = 1000;

    protected final boolean multiSelect;

    protected static MekSummaryCache mscInstance = MekSummaryCache.getInstance();
    protected MekSummary[] meks;

    private final MekTableModel unitModel = new MekTableModel();
    private final XTableColumnModel unitColumnModel = new XTableColumnModel();
    private TableColumn pvColumn;
    private TableColumn bvColumn;
    private TableColumn rulesLevelColumn;
    private TableColumn variableRulesLevelColumn;
    protected MekSearchFilter searchFilter;

    protected JFrame frame;
    private final UnitLoadingDialog unitLoadingDialog;
    private AdvancedSearchDialog advancedSearchDialog;

    protected TableRowSorter<MekTableModel> sorter;

    protected GameOptions gameOptions = null;
    protected boolean enableYearLimits = false;
    protected int allowedYear = ALLOWED_YEAR_ANY;
    protected boolean canonOnly = false;
    protected boolean allowInvalid = true;
    protected int gameTechLevel = TechConstants.T_SIMPLE_INTRO;
    protected int techLevelDisplayType = TECH_LEVEL_DISPLAY_IS_CLAN;
    protected boolean eraBasedTechLevel = false;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    // Create Client Preferences object to read values (for G/P BV #5333)
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();
    // endregion Variable Declarations

    protected AbstractUnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog) {
        this(frame, unitLoadingDialog, false);
    }

    protected AbstractUnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog, boolean multiSelect) {
        super(frame, Messages.getString("MekSelectorDialog.title"), true);
        setName("UnitSelectorDialog");
        this.frame = frame;
        this.unitLoadingDialog = unitLoadingDialog;
        this.multiSelect = multiSelect;
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
        comboUnitType.setSelectedIndex(GUIP.getMekSelectorUnitType());

        comboWeight.setSelectedIndex(GUIP.getMekSelectorWeightClass());

        updateTypeCombo();

        List<SortKey> sortList = new ArrayList<>();
        try {
            sortList.add(new SortKey(GUIP.getMekSelectorSortColumn(),
                  SortOrder.valueOf(GUIP.getMekSelectorSortOrder())));
        } catch (Exception e) {
            logger.error(e, "Failed to set based on user preferences, attempting to use default");

            sortList.add(new SortKey(GUIP.getMekSelectorDefaultSortColumn(),
                  SortOrder.valueOf(GUIP.getMekSelectorDefaultSortOrder())));
        }
        tableUnits.getRowSorter().setSortKeys(sortList);
        ((DefaultRowSorter<?, ?>) tableUnits.getRowSorter()).sort();

        tableUnits.invalidate(); // force re-layout of window
        splitPane.setDividerLocation(GUIP.getMekSelectorSplitPos());
        setSize(GUIP.getMekSelectorSizeWidth(), GUIP.getMekSelectorSizeHeight());
        setLocation(GUIP.getMekSelectorPosX(), GUIP.getMekSelectorPosY());
        toggleVtl(isVTL());
    }

    protected void initialize() {
        initComponents();
        setLocationRelativeTo(frame);
    }

    private void initComponents() {
        advancedSearchDialog = new AdvancedSearchDialog(frame, allowedYear);

        // To use the below you MUST AND ONLY modify the gridx and gridy components
        GridBagConstraints gridBagConstraintsWest = new GridBagConstraints();
        gridBagConstraintsWest.anchor = GridBagConstraints.WEST;

        setMinimumSize(new Dimension(640, 480));
        getContentPane().setLayout(new GridBagLayout());

        // region Unit Preview Pane
        panePreview = new EntityViewPane(frame, null);
        panePreview.setMinimumSize(new Dimension(0, 0));
        // endregion Unit Preview Pane

        // region Selection Panel
        JPanel selectionPanel = new JPanel(new GridBagLayout());

        tableUnits = new JTable(unitModel);
        tableUnits.setColumnModel(unitColumnModel);
        tableUnits.createDefaultColumnsFromModel();
        tableUnits.setName("tableUnits");
        tableUnits.addKeyListener(this);
        tableUnits.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "");
        tableUnits.setDefaultRenderer(Double.class, new TonnageRenderer());
        tableUnits.setDefaultRenderer(Long.class, new PriceRenderer());

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableUnits.setDefaultRenderer(Integer.class, centeredRenderer);
        tableUnits.getColumnModel().getColumn(MekTableModel.COL_LEVEL).setCellRenderer(centeredRenderer);
        tableUnits.getColumnModel().getColumn(MekTableModel.COL_VTL).setCellRenderer(centeredRenderer);

        tableUnits.setSelectionMode(multiSelect ?
              ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
              ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(unitModel);
        sorter.setComparator(MekTableModel.COL_CHASSIS, new NaturalOrderComparator());
        sorter.setComparator(MekTableModel.COL_MODEL, new NaturalOrderComparator());
        tableUnits.setRowSorter(sorter);
        tableUnits.getSelectionModel().addListSelectionListener(
              evt -> {
                  // There can be multiple events for one selection. Check to see if this is the
                  // last.
                  if (!evt.getValueIsAdjusting()) {
                      refreshUnitView();
                  }
              });

        for (int i = 0; i < unitModel.getColumnCount(); i++) {
            tableUnits.getColumnModel().getColumn(i).setPreferredWidth(unitModel.getPreferredWidth(i));
        }
        bvColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_BV);
        pvColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_PV);
        rulesLevelColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_LEVEL);
        variableRulesLevelColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_VTL);
        togglePV(false);
        toggleVtl(isVTL());
        JScrollPane scrollTableUnits = new JScrollPane(tableUnits);
        scrollTableUnits.setName("scrollTableUnits");

        JPanel panelFilterButtons = new JPanel(new GridBagLayout());

        JLabel labelType = new JLabel(Messages.getString("MekSelectorDialog.m_labelType"));
        labelType.setToolTipText(Messages.getString("MekSelectorDialog.m_labelType.ToolTip"));
        gridBagConstraintsWest.insets = new Insets(5, 0, 0, 0);
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 2;
        panelFilterButtons.add(labelType, gridBagConstraintsWest);

        listTechLevel.setToolTipText(Messages.getString("MekSelectorDialog.m_labelType.ToolTip"));
        listTechLevel.setLayoutOrientation(JList.VERTICAL_WRAP);
        listTechLevel.setVisibleRowCount(3);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 2;
        panelFilterButtons.add(listTechLevel, gridBagConstraintsWest);

        JLabel labelWeight = new JLabel(Messages.getString("MekSelectorDialog.m_labelWeightClass"));
        labelWeight.setName("labelWeight");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 1;
        panelFilterButtons.add(labelWeight, gridBagConstraintsWest);

        DefaultComboBoxModel<String> weightModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            weightModel.addElement(EntityWeightClass.getClassName(i));
        }
        weightModel.addElement(Messages.getString("MekSelectorDialog.All"));
        comboWeight.setModel(weightModel);
        comboWeight.setName("comboWeight");
        comboWeight.addActionListener(this);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 1;
        panelFilterButtons.add(comboWeight, gridBagConstraintsWest);

        JLabel labelUnitType = new JLabel(Messages.getString("MekSelectorDialog.m_labelUnitType"));
        labelUnitType.setName("labelUnitType");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(labelUnitType, gridBagConstraints);

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        unitTypeModel.addElement(Messages.getString("MekSelectorDialog.All"));
        for (int i = 0; i < UnitType.SIZE; i++) {
            // the AERO type does not match any units and there are no preconstructed lifeboats or escape pods
            if (i != UnitType.AERO) {
                unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
            }
        }
        unitTypeModel.addElement(Messages.getString("MekSelectorDialog.SupportVee"));
        comboUnitType.setModel(unitTypeModel);
        comboUnitType.setName("comboUnitType");
        comboUnitType.addActionListener(this);
        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 0;
        panelFilterButtons.add(comboUnitType, gridBagConstraintsWest);

        JLabel labelFilter = new JLabel(Messages.getString("MekSelectorDialog.m_labelFilter"));
        labelFilter.setName("labelFilter");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 3;
        panelFilterButtons.add(labelFilter, gridBagConstraintsWest);

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

        // Add the Gunnery and Piloting entry boxes and labels to the filter panel in the UI
        JLabel lblGun = new JLabel(Messages.getString("MekSelectorDialog.m_labelGunnery"));
        lblGun.setName("lblGun");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 4;
        if (CLIENT_PREFERENCES.useGPinUnitSelection()) {
            panelFilterButtons.add(lblGun, gridBagConstraintsWest);
        }

        textGunnery = new JTextField("4");
        textGunnery.setName("textGunnery");
        if (CLIENT_PREFERENCES.useGPinUnitSelection()) {
            textGunnery.getDocument().addDocumentListener(new DocumentListener() {
                // Set the table to refresh when the gunnery is changed
                @Override
                public void changedUpdate(DocumentEvent e) {
                    sorter.allRowsChanged();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    sorter.allRowsChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    sorter.allRowsChanged();
                }

            });
            gridBagConstraintsWest.gridx = 1;
            gridBagConstraintsWest.gridy = 4;
            panelFilterButtons.add(textGunnery, gridBagConstraintsWest);
        }

        JLabel lblPilot = new JLabel(Messages.getString("MekSelectorDialog.m_labelPiloting"));
        lblGun.setName("lblPilot");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 5;
        if (CLIENT_PREFERENCES.useGPinUnitSelection()) {
            panelFilterButtons.add(lblPilot, gridBagConstraintsWest);
        }

        textPilot = new JTextField("5");
        textPilot.setName("textPilot");
        if (CLIENT_PREFERENCES.useGPinUnitSelection()) {
            textPilot.getDocument().addDocumentListener(new DocumentListener() {
                // Set the table to refresh when the piloting is changed
                @Override
                public void changedUpdate(DocumentEvent e) {
                    sorter.allRowsChanged();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    sorter.allRowsChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    sorter.allRowsChanged();
                }
            });
            gridBagConstraintsWest.gridx = 1;
            gridBagConstraintsWest.gridy = 5;
            panelFilterButtons.add(textPilot, gridBagConstraintsWest);
        }

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

        JPanel panelSearchButtons = new JPanel(new GridBagLayout());

        buttonAdvancedSearch = new JButton(Messages.getString("MekSelectorDialog.AdvSearch"));
        buttonAdvancedSearch.setName("buttonAdvancedSearch");
        buttonAdvancedSearch.addActionListener(this);
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 0;
        panelSearchButtons.add(buttonAdvancedSearch, gridBagConstraintsWest);

        buttonResetSearch = new JButton(Messages.getString("MekSelectorDialog.Reset"));
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

        lblCount = new JLabel("");
        gridBagConstraintsWest.gridx = 3;
        gridBagConstraintsWest.gridy = 0;
        panelSearchButtons.add(lblCount, gridBagConstraintsWest);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(10, 10, 5, 0);
        selectionPanel.add(panelFilterButtons, gridBagConstraints);

        gridBagConstraints.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelSearchButtons, gridBagConstraints);

        gridBagConstraints.insets = new Insets(5, 0, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        selectionPanel.add(scrollTableUnits, gridBagConstraints);

        JPanel panelButtons = createButtonsPanel();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
              selectionPanel, panePreview);
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
            @Serial
            private static final long serialVersionUID = 2587225044226668664L;

            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        };

        Action selectAction = new AbstractAction() {
            @Serial
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

    private boolean isVTL() {
        return gameOptions != null && gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
    }

    private void updateTypeCombo() {
        listTechLevel.removeListSelectionListener(this);
        int[] selectedIndices = listTechLevel.getSelectedIndices();

        if (selectedIndices.length == 0) {
            String option = GUIP.getMekSelectorRulesLevels().replaceAll("[\\[\\]]", "");
            if (!option.isBlank()) {
                String[] strSelections = option.split(",");
                selectedIndices = new int[strSelections.length];
                for (int i = 0; i < strSelections.length; i++) {
                    selectedIndices[i] = Integer.parseInt(strSelections[i].trim());
                }
            }
        }

        int maxTech = switch (gameTechLevel) {
            case TechConstants.T_SIMPLE_INTRO -> TechConstants.T_INTRO_BOX_SET;
            case TechConstants.T_SIMPLE_STANDARD -> TechConstants.T_TW_ALL;
            case TechConstants.T_SIMPLE_ADVANCED -> TechConstants.T_CLAN_ADVANCED;
            case TechConstants.T_SIMPLE_EXPERIMENTAL -> TechConstants.T_CLAN_EXPERIMENTAL;
            default -> TechConstants.T_CLAN_UNOFFICIAL;
        };

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
     *
     * @return the panel containing the buttons to place in the interface
     */
    protected abstract JPanel createButtonsPanel();

    /**
     * This is the function to add a unit to the current interface. That could be a purchase (MekHQ), addition (MekHQ),
     * or unit selection (MegaMek/MegaMekLab)
     *
     * @param modifier a boolean to modify how the function will work. In MegaMek this is used to close the dialog, in
     *                 MekHQ to GM add.
     */
    protected abstract void select(boolean modifier);

    /**
     * This filters the units on the display. It is overwritten for MekHQ
     */
    protected void filterUnits() {
        RowFilter<MekTableModel, Integer> unitTypeFilter;

        List<Integer> techLevels = new ArrayList<>();
        for (Integer selectedIdx : listTechLevel.getSelectedIndices()) {
            techLevels.add(techLevelListToIndex.get(selectedIdx));
        }
        final Integer[] nTypes = new Integer[techLevels.size()];
        techLevels.toArray(nTypes);

        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        final boolean checkSupportVee = Messages.getString("MekSelectorDialog.SupportVee")
              .equals(comboUnitType.getSelectedItem());
        // If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends MekTableModel, ? extends Integer> entry) {
                    MekTableModel mekModel = entry.getModel();
                    MekSummary mek = mekModel.getMekSummary(entry.getIdentifier());
                    boolean techLevelMatch = false;
                    int type = eraBasedTechLevel ? mek.getType(allowedYear) : mek.getType();
                    for (int tl : nTypes) {
                        if (type == tl) {
                            techLevelMatch = true;
                            break;
                        }
                    }
                    if (
                        /* Year Limits */
                          (!enableYearLimits || (mek.getYear() <= allowedYear))
                                /* Canon */
                                && (!canonOnly || mek.isCanon())
                                /* Invalid units */
                                && (allowInvalid || !mek.getLevel().equals("F"))
                                /* Weight */
                                && ((nClass == EntityWeightClass.SIZE) || (nClass == mek.getWeightClass()))
                                /* Technology Level */
                                && (techLevelMatch)
                                /* Support Vehicles */
                                && ((nUnit == -1) || (checkSupportVee && mek.isSupport())
                                || (!checkSupportVee && mek.getUnitType().equals(UnitType.getTypeName(nUnit))))
                                /* Advanced Search */
                                && ((searchFilter == null) || MekSearchFilter.isMatch(mek, searchFilter))
                                && advancedSearchDialog.getASAdvancedSearch().matches(mek)) {
                        return matchesTextFilter(mek);
                    }
                    return false;
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }
        sorter.setRowFilter(unitTypeFilter);
        String msgUnitCount = Messages.getString("MekSelectorDialog.UnitCount");
        lblCount.setText(String.format(" %s %d", msgUnitCount, sorter.getViewRowCount()));
    }

    protected boolean matchesTextFilter(MekSummary unit) {
        if (!textFilter.getText().isBlank()) {
            String text = I18n.normalizeTextToASCII(textFilter.getText(), false).toLowerCase();
            String[] tokens = text.split(" ");
            String searchText = I18n.normalizeTextToASCII(unit.getName() + "###" + unit.getModel(), true).toLowerCase();
            for (String token : tokens) {
                if (!searchText.contains(token)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return the selected entity (required for MekHQ/MegaMek overrides)
     */
    protected Entity refreshUnitView() {
        Entity selectedEntity = getSelectedEntity();
        panePreview.updateDisplayedEntity(selectedEntity, getSelectedMekSummary());
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
        MekSummary ms = getSelectedMekSummary();
        if (ms == null) {
            return null;
        }

        try {
            // For some unknown reason the base path gets screwed up after you
            // print so this sets the source file to the full path.
            return new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception e) {
            logger.error(e, "Unable to load mek: {}: {}: {}", ms.getSourceFile(), ms.getEntryName(), e.getMessage());
            return null;
        }
    }

    public ArrayList<Entity> getSelectedEntities() {
        return getSelectedMekSummaries().stream().map(
              ms -> {
                  try {
                      return new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                  } catch (EntityLoadingException e) {
                      logger.error(e, "Unable to load mek: {}: {}", ms.getSourceFile(), ms.getEntryName());
                      return null;
                  }
              }
        ).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
    }

    /** @return The MekSummary for the selected unit. */
    public @Nullable MekSummary getSelectedMekSummary() {
        var summaries = getSelectedMekSummaries();
        if (summaries.size() != 1) {
            return null;
        }
        return summaries.get(0);
    }

    public List<MekSummary> getSelectedMekSummaries() {
        var rows = tableUnits.getSelectedRows();
        return Arrays.stream(rows).map(tableUnits::convertRowIndexToModel).mapToObj(i -> meks[i]).toList();
    }

    @Override
    public void run() {
        // Loading meks can take a while, so it will have its own thread for MegaMek
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.
        meks = mscInstance.getAllMeks();
        unitLoadingDialog.setVisible(false);

        // break out if there are no units to filter
        if (meks == null) {
            logger.error("No meks were loaded");
        } else {
            SwingUtilities.invokeLater(() -> unitModel.setData(meks));
        }
    }

    /**
     * @param visible whether to make the GUI visible
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setUserPreferences();
        }
        searchFilter = null;
        buttonResetSearch.setEnabled(false);
        filterUnits();

        validate();
        repaint();
        super.setVisible(visible);
    }

    /**
     * This handles processing windows events
     *
     * @param e the event to process
     */
    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            GUIP.setMekSelectorUnitType(comboUnitType.getSelectedIndex());
            GUIP.setMekSelectorWeightClass(comboWeight.getSelectedIndex());
            GUIP.setMekSelectorRulesLevels(Arrays.toString(listTechLevel.getSelectedIndices()));
            GUIP.setMekSelectorSortColumn(tableUnits.getRowSorter().getSortKeys().get(0).getColumn());
            GUIP.setMekSelectorSortOrder(tableUnits.getRowSorter().getSortKeys().get(0).getSortOrder().name());
            GUIP.setMekSelectorSizeHeight(getSize().height);
            GUIP.setMekSelectorSizeWidth(getSize().width);
            GUIP.setMekSelectorPosX(getLocation().x);
            GUIP.setMekSelectorPosY(getLocation().y);
            GUIP.setMekSelectorSplitPos(splitPane.getDividerLocation());
        }
    }

    /**
     * This handles key released events
     *
     * @param ke the key that was released
     */
    @Override
    public void keyReleased(KeyEvent ke) {
    }

    /**
     * This handles key pressed events
     *
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
     *
     * @param search the search parameters
     */
    private void searchFor(String search) {
        for (int i = 0; i < meks.length; i++) {
            if (meks[i].getName().toLowerCase().startsWith(search)) {
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
     *
     * @param ke the typed key
     */
    @Override
    public void keyTyped(KeyEvent ke) {
    }

    /**
     * This handles the primary action events (any that can come from buttons in this class)
     *
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
            advancedSearchDialog.setVisible(true);
            searchFilter = advancedSearchDialog.getTWAdvancedSearch().getMekSearchFilter();
            setResetSearchEnabledStatus();
            filterUnits();
        } else if (ev.getSource().equals(buttonResetSearch)) {
            advancedSearchDialog.clearSearches();
            searchFilter = null;
            setResetSearchEnabledStatus();
            filterUnits();
        }
    }

    private void setResetSearchEnabledStatus() {
        buttonResetSearch.setEnabled(((searchFilter != null) && !searchFilter.isDisabled)
              || advancedSearchDialog.getASAdvancedSearch().isActive());
    }

    private void close() {
        setVisible(false);
    }

    /**
     * This handles list selection events, which are only thrown by MegaMek/MegaMekLab
     *
     * @param evt the event to process
     */
    @Override
    public void valueChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting() && evt.getSource().equals(listTechLevel)) {
            filterUnits();
        }
    }

    /**
     * Toggles between showing the Point Value column and the Battle Value column.
     */
    private void togglePV(boolean showPV) {
        unitColumnModel.setColumnVisible(pvColumn, showPV);
        unitColumnModel.setColumnVisible(bvColumn, !showPV);
    }

    private void toggleVtl(boolean showVTL) {
        unitColumnModel.setColumnVisible(rulesLevelColumn, !showVTL);
        unitColumnModel.setColumnVisible(variableRulesLevelColumn, showVTL);
    }

    /**
     * A table model for displaying work items
     */
    protected class MekTableModel extends AbstractTableModel {
        @Serial
        private static final long serialVersionUID = -5457068129532709857L;
        private static final int COL_CHASSIS = 0;
        private static final int COL_MODEL = 1;
        private static final int COL_WEIGHT = 2;
        private static final int COL_BV = 3;
        private static final int COL_PV = 4;
        private static final int COL_YEAR = 5;
        private static final int COL_COST = 6;
        private static final int COL_LEVEL = 7;
        private static final int COL_VTL = 8;
        private static final int N_COL = 9;

        private MekSummary[] data = new MekSummary[0];

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public int getPreferredWidth(int column) {
            return switch (column) {
                case COL_MODEL -> 75;
                case COL_CHASSIS -> 125;
                case COL_COST, COL_WEIGHT -> 50;
                default -> 15;
            };
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_MODEL -> I18n.getTextAt("megamek.client.messages", "MekView.column.model");
                case COL_CHASSIS -> I18n.getTextAt("megamek.client.messages", "MekView.column.chassis");
                case COL_WEIGHT -> I18n.getTextAt("megamek.client.messages", "MekView.column.weight");
                case COL_BV -> I18n.getTextAt("megamek.client.messages", "MekView.column.bv");
                case COL_PV -> I18n.getTextAt("megamek.client.messages", "MekView.column.pv");
                case COL_YEAR -> I18n.getTextAt("megamek.client.messages", "MekView.column.year");
                case COL_COST -> I18n.getTextAt("megamek.client.messages", "MekView.column.price");
                case COL_LEVEL -> I18n.getTextAt("megamek.client.messages", "MekView.column.rulesLevel");
                case COL_VTL -> I18n.getTextAt("megamek.client.messages", "MekView.column.variableTechLevel");
                default -> "?" + column + "?";
            };
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        public MekSummary getMekSummary(int i) {
            return data[i];
        }

        public void setData(MekSummary[] ms) {
            data = ms;
            fireTableDataChanged();
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (data.length <= row) {
                return "?";
            }
            MekSummary ms = data[row];
            if (col == COL_MODEL) {
                return ms.getModel();
            } else if (col == COL_CHASSIS) {
                return ms.getFullChassis();
            } else if (col == COL_WEIGHT) {
                if ((gameOptions != null) && ms.getUnitType().equals("BattleArmor")) {
                    if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_BA_WEIGHT)) {
                        return ms.getTOWeight();
                    } else {
                        return ms.getTWWeight();
                    }
                }
                return ms.getTons();
            } else if (col == COL_BV) {
                // This code allows for Gunnery and BV to be read from the UI, and update the BV values in
                // the table as a result
                int gunnery = 4;
                int piloting = 5;
                if (textGunnery.getText().matches("\\d+")) {
                    gunnery = Integer.parseInt(textGunnery.getText());
                    if (gunnery > 8) {
                        gunnery = 4;
                    }

                }
                if (textPilot.getText().matches("\\d+")) {
                    piloting = Integer.parseInt(textPilot.getText());
                    if (piloting > 8) {
                        piloting = 5;
                    }
                }

                double gp_multiply = BVCalculator.bvSkillMultiplier(gunnery, piloting);
                return (int) Math.round(ms.getBV() * gp_multiply);
            } else if (col == COL_PV) {
                // This code allows for Gunnery to be read from the UI, and update the PV values
                // in the table as a result
                // It uses Gunnery as the skill
                int gunnery = 4;
                if (textGunnery.getText().matches("\\d+")) {
                    gunnery = Integer.parseInt(textGunnery.getText());
                }

                double modifier;
                if (gunnery == 4) {
                    modifier = 1;
                } else if (gunnery == 3) {
                    modifier = 1.2;
                } else if (gunnery == 2) {
                    modifier = 1.4;
                } else if (gunnery == 1) {
                    modifier = 1.6;
                } else if (gunnery == 0) {
                    modifier = 1.8;
                } else if (gunnery == 5) {
                    modifier = 0.9;
                } else if (gunnery == 6) {
                    modifier = 0.8;
                } else if (gunnery == 7) {
                    modifier = 0.7;
                } else {
                    modifier = 1;
                }
                return (int) Math.round(ms.getPointValue() * modifier);
            } else if (col == COL_YEAR) {
                return ms.getYear();
            } else if (col == COL_COST) {
                return ms.getCost();
            } else if (col == COL_LEVEL) {
                return ms.getLevel();
            } else if (col == COL_VTL) {
                if (gameOptions != null) {
                    return ms.getLevel(gameOptions.intOption(OptionsConstants.ALLOWED_YEAR));
                } else {
                    return ms.getLevel(); // fallback to base tech level
                }
            }
            return "?";
        }
    }


    /** A specialized renderer for the mek table (formats the unit tonnage). */
    public static class TonnageRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(final JTable table, final @Nullable Object value,
              final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            if (value instanceof Double) {
                setHorizontalAlignment(JLabel.RIGHT);
                double weight = (Double) value;
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (weight < 2) {
                    setText(String.format(MegaMek.getMMOptions().getLocale(), "%,d kg ", (int) (weight * 1000)));
                } else if (Math.round(weight) == weight) {
                    setText(String.format(MegaMek.getMMOptions().getLocale(), "%,d t ", Math.round(weight)));
                } else {
                    setText(String.format(MegaMek.getMMOptions().getLocale(), "%.1f t ", weight));
                }
                return this;
            } else {
                return null;
            }
        }
    }

    /** A specialized renderer for the mek table (formats the unit price). */
    public static class PriceRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(final JTable table, final @Nullable Object value,
              final boolean isSelected, final boolean hasFocus,
              final int row, final int column) {
            if (value instanceof Long) {
                setHorizontalAlignment(JLabel.RIGHT);
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText(String.format(MegaMek.getMMOptions().getLocale(), "%,d ", (Long) value));
                return this;
            } else {
                return null;
            }
        }
    }
}
