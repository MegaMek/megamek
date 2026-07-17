/*
 * Copyright (C) 2002, 2004 Josh Yockey
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
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
    private final JToggleButton buttonGPToggle = new JToggleButton(Messages.getString("MekSelectorDialog.ToggleGP"));
    protected JList<String> listTechLevel = new JList<>();
    private final JComponent gpLine = new JPanel();
    private final JLabel lblCount = new JLabel();
    /**
     * We need to map the selected index of listTechLevel to the actual TL it belongs to
     */
    protected Map<Integer, Integer> techLevelListToIndex = new HashMap<>();
    protected JComboBox<String> comboUnitType = new JComboBox<>();
    protected JComboBox<String> comboWeight = new JComboBox<>();
    protected JLabel labelImage = new JLabel(""); // inline to avoid potential null pointer issues
    protected JTable tableUnits;
    protected JTextField textFilter;
    protected JTextField textGunnery = new JTextField("4", 3);
    protected JTextField textPilot = new JTextField("5", 3);
    private final FlatTriStateCheckBox checkboxCanonOnly = createSourceFilterCheckbox(
          Messages.getString("MekSelectorDialog.chkCanonOnly"));
    private final FlatTriStateCheckBox checkboxHasPublishedRecordSheet = createSourceFilterCheckbox(
          Messages.getString("MekSelectorDialog.chkHasPublishedRecordSheet"));
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
    private Predicate<MekSummary> unitSelectionScopeFilter = Objects::nonNull;
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

    private static FlatTriStateCheckBox createSourceFilterCheckbox(String text) {
        FlatTriStateCheckBox checkbox = new FlatTriStateCheckBox(text, FlatTriStateCheckBox.State.UNSELECTED);
        checkbox.setAltStateCycleOrder(true);
        return checkbox;
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
        if (canonOnly) {
            checkboxCanonOnly.setState(FlatTriStateCheckBox.State.SELECTED);
        }
        checkboxCanonOnly.setEnabled(!canonOnly);
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

        JPanel panelSourceFilters = new JPanel(new GridBagLayout());
        checkboxCanonOnly.setName("checkboxCanonOnly");
        checkboxCanonOnly.addActionListener(this);
        checkboxHasPublishedRecordSheet.setName("checkboxHasPublishedRecordSheet");
        checkboxHasPublishedRecordSheet.addActionListener(this);

        GridBagConstraints checkboxConstraints = new GridBagConstraints();
        checkboxConstraints.anchor = GridBagConstraints.WEST;
        checkboxConstraints.gridx = 0;
        checkboxConstraints.gridy = 0;
        panelSourceFilters.add(checkboxCanonOnly, checkboxConstraints);
        checkboxConstraints.gridx = 1;
        checkboxConstraints.insets = new Insets(0, 10, 0, 0);
        panelSourceFilters.add(checkboxHasPublishedRecordSheet, checkboxConstraints);

        gridBagConstraintsWest.gridx = 1;
        gridBagConstraintsWest.gridy = 3;
        panelFilterButtons.add(panelSourceFilters, gridBagConstraintsWest);

        JLabel labelFilter = new JLabel(Messages.getString("MekSelectorDialog.m_labelFilter"));
        labelFilter.setName("labelFilter");
        gridBagConstraintsWest.gridx = 0;
        gridBagConstraintsWest.gridy = 4;
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
        gridBagConstraintsWest.gridy = 4;
        gridBagConstraintsWest.fill = GridBagConstraints.HORIZONTAL;
        panelFilterButtons.add(textFilter, gridBagConstraintsWest);
        gridBagConstraintsWest.fill = GridBagConstraints.NONE;

        // Gunnery and Piloting textfields and labels
        JLabel lblGun = new JLabel(Messages.getString("MekSelectorDialog.m_labelGunnery"));
        lblGun.setName("lblGun");
        textGunnery.setName("textGunnery");
        textGunnery.getDocument().addDocumentListener(new GPDocumentListener());

        JLabel lblPilot = new JLabel(Messages.getString("MekSelectorDialog.m_labelPiloting"));
        lblGun.setName("lblPilot");
        textPilot.setName("textPilot");
        textPilot.getDocument().addDocumentListener(new GPDocumentListener());

        gpLine.add(lblGun);
        gpLine.add(textGunnery);
        gpLine.add(Box.createHorizontalStrut(10));
        gpLine.add(lblPilot);
        gpLine.add(textPilot);
        gpLine.setVisible(CLIENT_PREFERENCES.useGPinUnitSelection());

        labelImage.setHorizontalAlignment(SwingConstants.CENTER);
        labelImage.setName("labelImage");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelFilterButtons.add(labelImage, gridBagConstraints);

        JPanel panelSearchButtons = new JPanel(new GridBagLayout());
        gridBagConstraintsWest.insets = new Insets(0, 7, 0, 0);

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

        buttonGPToggle.setSelected(CLIENT_PREFERENCES.useGPinUnitSelection());
        buttonGPToggle.addActionListener(e -> toggleGP());
        gridBagConstraintsWest.gridx++;
        panelSearchButtons.add(buttonGPToggle, gridBagConstraintsWest);

        gridBagConstraintsWest.gridx++;
        panelSearchButtons.add(lblCount, gridBagConstraintsWest);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(10, 10, 5, 0);
        selectionPanel.add(panelFilterButtons, gridBagConstraints);

        gridBagConstraints.insets = new Insets(10, 10, 0, 0);
        selectionPanel.add(panelSearchButtons, gridBagConstraints);
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        selectionPanel.add(gpLine, gridBagConstraints);

        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
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

        sorter.addRowSorterListener(event -> {
            if (event.getType() == RowSorterEvent.Type.SORTED) {
                updateUnitCount();
            }
        });
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

    private void toggleGP() {
        CLIENT_PREFERENCES.setUseGpInUnitSelection(buttonGPToggle.isSelected());
        gpLine.setVisible(CLIENT_PREFERENCES.useGPinUnitSelection());
        // Reset the values when hidden so there isn't an invisible BV modifier
        if (!CLIENT_PREFERENCES.useGPinUnitSelection()) {
            textGunnery.setText("4");
            textPilot.setText("5");
        }
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
                                                                && matchesCanonFilter(mek)
                                /* Published Record Sheet */
                                                                && matchesPublishedRecordSheetFilter(mek)
                                /* Invalid units */
                                && (allowInvalid || !mek.getLevel().equals("F"))
                                /* Weight */
                                && ((nClass == EntityWeightClass.SIZE) || (nClass == mek.getWeightClass()))
                                /* Technology Level */
                                && (techLevelMatch)
                                /* Support Vehicles */
                                && ((nUnit == -1) || (checkSupportVee && mek.isSupport())
                                || (!checkSupportVee && mek.getUnitType().equals(UnitType.getTypeName(nUnit))))
                                /* Additional caller-specific restrictions */
                                && unitSelectionScopeFilter.test(mek)
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
    }

    protected void updateUnitCount() {
        lblCount.setText(Messages.getString("MekSelectorDialog.UnitCount", sorter.getViewRowCount()));
    }

    private boolean matchesCanonFilter(MekSummary mek) {
        if (canonOnly) {
            return !mek.isNonCanonBySource();
        }

        return switch (checkboxCanonOnly.getState()) {
            case SELECTED -> !mek.isNonCanonBySource();
            case INDETERMINATE -> mek.isNonCanonBySource();
            case UNSELECTED -> true;
        };
    }

    private boolean matchesPublishedRecordSheetFilter(MekSummary mek) {
        return switch (checkboxHasPublishedRecordSheet.getState()) {
            case SELECTED -> mek.hasPublishedRecordSheet();
            case INDETERMINATE -> !mek.hasPublishedRecordSheet();
            case UNSELECTED -> true;
        };
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
     * Sets additional unit-list restrictions beyond the shared selector controls. The base selector accepts every
     * candidate that passed the built-in filters; callers can use this to constrain specialized pickers without
     * duplicating the full filter pipeline.
     *
     * @param unitSelectionScopeFilter Predicate that returns true when a candidate should remain visible
     */
    protected void setUnitSelectionScopeFilter(Predicate<MekSummary> unitSelectionScopeFilter) {
        this.unitSelectionScopeFilter = Objects.requireNonNull(unitSelectionScopeFilter, "unitSelectionScopeFilter");
    }

    /**
     * Allows subclasses to keep filter controls in sync with additional unit-list restrictions after persisted user
     * preferences have been restored.
     */
    protected void configureUnitSelectionScope() {}

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
        return summaries.getFirst();
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
            configureUnitSelectionScope();
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
            if (comboUnitType.isEnabled()) {
                GUIP.setMekSelectorUnitType(comboUnitType.getSelectedIndex());
            }
            GUIP.setMekSelectorWeightClass(comboWeight.getSelectedIndex());
            GUIP.setMekSelectorRulesLevels(Arrays.toString(listTechLevel.getSelectedIndices()));
            GUIP.setMekSelectorSortColumn(tableUnits.getRowSorter().getSortKeys().getFirst().getColumn());
            GUIP.setMekSelectorSortOrder(tableUnits.getRowSorter().getSortKeys().getFirst().getSortOrder().name());
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
        long curTime = java.lang.System.currentTimeMillis();
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
        if (ev.getSource().equals(comboWeight) || ev.getSource().equals(comboUnitType)
              || ev.getSource().equals(checkboxCanonOnly)
              || ev.getSource().equals(checkboxHasPublishedRecordSheet)) {
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
                int gunnery = parseSkillValue(textGunnery, 4);
                int piloting = parseSkillValue(textPilot, 5);
                double gp_multiply = BVCalculator.bvSkillMultiplier(gunnery, piloting);
                return (int) Math.round(ms.getBV() * gp_multiply);

            } else if (col == COL_PV) {
                //FIXME It uses Gunnery as the skill - should be improved to show an AS "Skill" instead
                int gunnery = parseSkillValue(textGunnery, 4);

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

        public TonnageRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final @Nullable Object value,
              final boolean isSelected, final boolean hasFocus, final int row, final int column) {

            String text = "?";
            if (value instanceof Double weight) {
                if (weight < 2) {
                    text = String.format(MegaMek.getMMOptions().getLocale(), "%,d kg ", (int) (weight * 1000));
                } else if (Math.round(weight) == weight) {
                    text = String.format(MegaMek.getMMOptions().getLocale(), "%,d t ", Math.round(weight));
                } else {
                    text = String.format(MegaMek.getMMOptions().getLocale(), "%.1f t ", weight);
                }
            }
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }

    /** A specialized renderer for the mek table (formats the unit price). */
    public static class PriceRenderer extends DefaultTableCellRenderer {

        public PriceRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
              int row, int column) {

            String text = "?";
            if (value instanceof Long price) {
                text = String.format(MegaMek.getMMOptions().getLocale(), "%,d ", price);
            }
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Refreshes the table's BV/PV columns and the preview's Analysis tab when gunnery/piloting is
     * changed.
     */
    private class GPDocumentListener implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent event) {
            skillValuesChanged();
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            skillValuesChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            skillValuesChanged();
        }

        private void skillValuesChanged() {
            sorter.allRowsChanged();
            panePreview.setAnalysisGunnery(parseSkillValue(textGunnery, 4));
        }
    }

    private int parseSkillValue(JTextField field, int defaultValue) {
        try {
            int value = Integer.parseInt(field.getText());
            boolean valid = value >= 0 && value <= 8;
            field.putClientProperty(FlatClientProperties.OUTLINE, valid ? null : FlatClientProperties.OUTLINE_ERROR);
            return valid ? value : defaultValue;
        } catch (NumberFormatException ignored) {
            field.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            return defaultValue;
        }
    }
}
