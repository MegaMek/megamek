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
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
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
    private final JToggleButton buttonAssetCostToggle =
          new JToggleButton(Messages.getString("MekSelectorDialog.ToggleAssetCost"));
    private final JComboBox<String> assetSkillChooser = new JComboBox<>(new String[] {
          Messages.getString("MekSelectorDialog.AssetSkill.Regular"),
          Messages.getString("MekSelectorDialog.AssetSkill.Veteran") });
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

    /** Sentinel type-codes for the unit-type filter combo entries that are not real {@link UnitType} values. */
    static final int UNIT_TYPE_ALL = -1;
    static final int UNIT_TYPE_SUPPORT_VEE = -2;
    /**
     * Maps each unit-type filter combo index to its {@link UnitType} code (or a sentinel above). This decouples the
     * filter from the combo's positional order, which is necessary because some unit types (AERO) are omitted from the
     * combo, so {@code selectedIndex - 1} no longer equals the {@link UnitType} value.
     */
    private final List<Integer> unitTypeComboCodes = new ArrayList<>();

    private final MekTableModel unitModel = new MekTableModel();
    private final XTableColumnModel unitColumnModel = new XTableColumnModel();
    private Predicate<MekSummary> unitSelectionScopeFilter = Objects::nonNull;
    private TableColumn pvColumn;
    private TableColumn bvColumn;
    private TableColumn assetBvColumn;
    private TableColumn assetBspColumn;
    private TableColumn rulesLevelColumn;
    private TableColumn variableRulesLevelColumn;

    /** When true, skill-input listeners skip refreshing the table so a batch of changes triggers a single refresh. */
    private boolean suppressSkillRefresh;
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

        AssetCostRenderer assetCostRenderer = new AssetCostRenderer();
        tableUnits.getColumnModel().getColumn(MekTableModel.COL_ASSET_BV).setCellRenderer(assetCostRenderer);
        tableUnits.getColumnModel().getColumn(MekTableModel.COL_ASSET_BSP).setCellRenderer(assetCostRenderer);

        // Battlefield Support Assets have no BV or PV of their own (their cost is the asset cost column),
        // so those cells are left blank for asset rows.
        BvPvRenderer bvPvRenderer = new BvPvRenderer();
        tableUnits.getColumnModel().getColumn(MekTableModel.COL_BV).setCellRenderer(bvPvRenderer);
        tableUnits.getColumnModel().getColumn(MekTableModel.COL_PV).setCellRenderer(bvPvRenderer);

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
        assetBvColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_ASSET_BV);
        assetBspColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_ASSET_BSP);
        rulesLevelColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_LEVEL);
        variableRulesLevelColumn = tableUnits.getColumnModel().getColumn(MekTableModel.COL_VTL);
        togglePV(false);
        toggleAssetCost(false);
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
        unitTypeComboCodes.clear();
        unitTypeModel.addElement(Messages.getString("MekSelectorDialog.All"));
        unitTypeComboCodes.add(UNIT_TYPE_ALL);
        for (int i = 0; i < UnitType.SIZE; i++) {
            // the AERO type does not match any units and there are no preconstructed lifeboats or escape pods
            if (i != UnitType.AERO) {
                unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
                unitTypeComboCodes.add(i);
            }
        }
        unitTypeModel.addElement(Messages.getString("MekSelectorDialog.SupportVee"));
        unitTypeComboCodes.add(UNIT_TYPE_SUPPORT_VEE);
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
        gpLine.add(Box.createHorizontalStrut(10));
        JLabel lblAssetSkill = new JLabel(Messages.getString("MekSelectorDialog.m_labelAssetSkill"));
        lblAssetSkill.setName("lblAssetSkill");
        gpLine.add(lblAssetSkill);
        assetSkillChooser.setName("assetSkillChooser");
        assetSkillChooser.setToolTipText(Messages.getString("MekSelectorDialog.AssetSkill.ToolTip"));
        // Recompute the asset cost column when the Regular/Veteran selection changes.
        assetSkillChooser.addActionListener(e -> refreshSkillAdjustedColumns());
        gpLine.add(assetSkillChooser);
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

        buttonAssetCostToggle.setName("buttonToggleAssetCost");
        buttonAssetCostToggle.setToolTipText(Messages.getString("MekSelectorDialog.ToggleAssetCost.ToolTip"));
        buttonAssetCostToggle.addActionListener(e -> toggleAssetCost(buttonAssetCostToggle.isSelected()));
        gridBagConstraintsWest.gridx++;
        panelSearchButtons.add(buttonAssetCostToggle, gridBagConstraintsWest);

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
        // Reset the values when hidden so there isn't an invisible BV/cost modifier. Each reset would normally
        // trigger a full table re-sort via its listener; suppress those and do a single refresh at the end, and only
        // when a value actually changed (so closing an unedited Skills row does no work).
        if (!CLIENT_PREFERENCES.useGPinUnitSelection()) {
            boolean changed = !textGunnery.getText().equals("4")
                  || !textPilot.getText().equals("5")
                  || (assetSkillChooser.getSelectedIndex() != 0);
            suppressSkillRefresh = true;
            textGunnery.setText("4");
            textPilot.setText("5");
            assetSkillChooser.setSelectedIndex(0);
            suppressSkillRefresh = false;
            if (changed) {
                sorter.allRowsChanged();
            }
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
        final int selectedTypeCode = unitTypeCodeForComboIndex(comboUnitType.getSelectedIndex());
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
                                /* Unit Type (incl. Support Vehicles and Battlefield Support Assets) */
                                && matchesUnitTypeSelection(mek, selectedTypeCode)
                                /* Additional caller-specific restrictions */
                                && unitSelectionScopeFilter.test(mek)
                                /* Advanced Search */
                                && ((searchFilter == null) || MekSearchFilter.isMatch(mek, searchFilter))
                                && advancedSearchDialog.getASAdvancedSearch().matches(mek)
                                /* Asset Advanced Search: tested against the row's asset form (the row
                                 * itself for a standalone asset, or the linked asset for a base unit) */
                                && advancedSearchDialog.getBFSAdvancedSearch().matches(assetSummaryFor(mek))) {
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

    /**
     * Resolves a unit-type filter combo index to its {@link UnitType} code (or a sentinel: {@link #UNIT_TYPE_ALL},
     * {@link #UNIT_TYPE_SUPPORT_VEE}). Robust to the AERO gap in the combo.
     *
     * @param comboIndex the selected combo index
     *
     * @return the type code, or {@link #UNIT_TYPE_ALL} if the index is out of range
     */
    protected int unitTypeCodeForComboIndex(int comboIndex) {
        if ((comboIndex < 0) || (comboIndex >= unitTypeComboCodes.size())) {
            return UNIT_TYPE_ALL;
        }
        return unitTypeComboCodes.get(comboIndex);
    }

    /**
     * @param mek      the unit summary to test
     * @param typeCode the selected unit-type code (see {@link #unitTypeCodeForComboIndex(int)})
     *
     * @return whether the unit matches the selected unit-type filter. The Battlefield Support Asset filter matches an
     *       asset OR a base unit that has a linked asset, so a linked base/asset pair (shown as the base unit's row)
     *       appears under both its own type filter and the Asset filter.
     */
    protected boolean matchesUnitTypeSelection(MekSummary mek, int typeCode) {
        boolean hasLinkedAsset = !mek.isBattlefieldSupportAsset() && (mscInstance.getLinkedAsset(mek) != null);
        return matchesUnitTypeSelection(typeCode, mek.getUnitType(), mek.isBattlefieldSupportAsset(),
              mek.isSupport(), hasLinkedAsset);
    }

    /**
     * Pure decision function for the unit-type filter (extracted for testability).
     *
     * @param typeCode       the selected unit-type code
     * @param unitTypeName   the unit's {@link UnitType} name
     * @param isAsset        whether the unit is a Battlefield Support Asset
     * @param isSupport      whether the unit is a Support Vehicle
     * @param hasLinkedAsset whether the unit is a base unit with a linked asset
     *
     * @return whether the unit matches the selected filter
     */
    static boolean matchesUnitTypeSelection(int typeCode, String unitTypeName, boolean isAsset, boolean isSupport,
          boolean hasLinkedAsset) {
        if (typeCode == UNIT_TYPE_ALL) {
            return true;
        }
        if (typeCode == UNIT_TYPE_SUPPORT_VEE) {
            return isSupport;
        }
        if (typeCode == UnitType.BATTLEFIELD_SUPPORT_ASSET) {
            return isAsset || hasLinkedAsset;
        }
        return unitTypeName.equals(UnitType.getTypeName(typeCode));
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
            String searchText = I18n.normalizeTextToASCII(unit.getName() + "###" + unit.getModel()
                  + assetNameSearchSuffix(unit), true).toLowerCase();
            for (String token : tokens) {
                if (!searchText.contains(token)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns extra searchable text for the basic name filter so that a Battlefield Support Asset can be found by its
     * card title/subtitle. For a standalone asset row this is the asset's own card title/subtitle; for a base unit row
     * with a linked asset it is the linked asset's; otherwise it is empty.
     *
     * @param unit the unit summary shown in the row
     *
     * @return the extra text (prefixed with a separator), or an empty string when the row has no asset form
     */
    private String assetNameSearchSuffix(MekSummary unit) {
        MekSummary asset = assetSummaryFor(unit);
        if (asset == null) {
            return "";
        }
        String title = (asset.getBfsCardTitle() != null) ? asset.getBfsCardTitle() : "";
        String subtitle = (asset.getBfsCardSubtitle() != null) ? asset.getBfsCardSubtitle() : "";
        return "###" + title + "###" + subtitle;
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
        MekSummary selectedSummary = getSelectedMekSummary();
        // A row that is itself an asset uses the asset Entity for its Summary and TRO as well as its BFS Card.
        boolean standaloneAsset = (selectedSummary != null) && selectedSummary.isBattlefieldSupportAsset();

        Entity baseEntity = standaloneAsset ? null : getSelectedEntity();

        MekSummary assetSummary = getSelectedAssetSummary();
        Entity loadedAsset = (assetSummary != null) ? loadEntity(assetSummary) : null;
        BattlefieldSupportAsset assetEntity =
              (loadedAsset instanceof BattlefieldSupportAsset asset) ? asset : null;

        Entity textualEntity = standaloneAsset ? assetEntity : baseEntity;
        panePreview.updateDisplayedEntity(textualEntity, standaloneAsset ? null : selectedSummary, assetEntity);

        Entity previewEntity = (textualEntity != null) ? textualEntity : assetEntity;
        // Empty the unit preview icon if there's nothing selected
        if (previewEntity == null) {
            labelImage.setIcon(null);
        }
        return previewEntity;
    }

    /**
     * @return the selected entity
     */
    public @Nullable Entity getSelectedEntity() {
        return loadEntity(getSelectedMekSummary());
    }

    /**
     * Loads the {@link Entity} for the given unit summary.
     *
     * @param ms the unit summary to load, or {@code null}
     *
     * @return the loaded entity, or {@code null} if the summary is {@code null} or cannot be loaded
     */
    protected @Nullable Entity loadEntity(@Nullable MekSummary ms) {
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

    /**
     * Returns the Battlefield Support Asset form of every selected row that has one. For a standalone asset row this is
     * the asset itself; for a base unit row with a linked asset this is the linked asset; rows with no asset form are
     * skipped. Callers that offer a "Select as Asset" action use this to obtain the asset entities for the selection.
     *
     * @return the asset entities for the selection (may be empty)
     */
    public ArrayList<Entity> getSelectedAssetEntities() {
        return getSelectedMekSummaries().stream()
              .map(this::assetSummaryFor)
              .filter(Objects::nonNull)
              .map(this::loadEntity)
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * @return true if the current selection is non-empty and every selected row has a standard (TW) unit form, i.e. no
     *       row is a standalone Battlefield Support Asset. Callers use this to enable a "Select as Unit" action.
     */
    public boolean selectionCanSelectAsUnit() {
        List<MekSummary> summaries = getSelectedMekSummaries();
        long assetOnly = summaries.stream().filter(MekSummary::isBattlefieldSupportAsset).count();
        return canSelectSelectionAsUnit(summaries.size(), (int) assetOnly);
    }

    /**
     * @return true if the current selection is non-empty and every selected row has a Battlefield Support Asset form
     *       (either it is an asset itself or has a linked asset). Callers use this to enable a "Select as Asset" action.
     */
    public boolean selectionCanSelectAsAsset() {
        List<MekSummary> summaries = getSelectedMekSummaries();
        long withoutAssetForm = summaries.stream().filter(ms -> assetSummaryFor(ms) == null).count();
        return canSelectSelectionAsAsset(summaries.size(), (int) withoutAssetForm);
    }

    /**
     * Pure decision: a selection may be added in standard (TW) unit form when it is non-empty and none of its rows is a
     * standalone (asset-only) Battlefield Support Asset.
     *
     * @param selectedCount  number of selected rows
     * @param assetOnlyCount number of selected rows that are standalone assets (no unit form)
     *
     * @return true if the selection can be added as units
     */
    static boolean canSelectSelectionAsUnit(int selectedCount, int assetOnlyCount) {
        return (selectedCount > 0) && (assetOnlyCount == 0);
    }

    /**
     * Pure decision: a selection may be added as Battlefield Support Assets when it is non-empty and every row has an
     * asset form.
     *
     * @param selectedCount        number of selected rows
     * @param withoutAssetFormCount number of selected rows that have no asset form
     *
     * @return true if the selection can be added as assets
     */
    static boolean canSelectSelectionAsAsset(int selectedCount, int withoutAssetFormCount) {
        return (selectedCount > 0) && (withoutAssetFormCount == 0);
    }

    /** @return The MekSummary for the selected unit. */
    public @Nullable MekSummary getSelectedMekSummary() {
        var summaries = getSelectedMekSummaries();
        if (summaries.size() != 1) {
            return null;
        }
        return summaries.getFirst();
    }

    /**
     * Returns the Battlefield Support Asset form of the selected row, if any. For a standalone asset row this is the
     * row itself; for a base unit row that has a linked asset this is the linked asset; otherwise {@code null}. Callers
     * that offer a "Select as Asset" action use this to obtain the asset summary.
     *
     * @return the selected asset summary, or {@code null} if the selection has no asset form
     */
    public @Nullable MekSummary getSelectedAssetSummary() {
        return assetSummaryFor(getSelectedMekSummary());
    }

    /**
     * Returns the Battlefield Support Asset form of the given unit summary, if any. For an asset summary this is the
     * summary itself; for a base unit that has a linked asset this is the linked asset; otherwise {@code null}.
     *
     * @param ms a unit summary, or {@code null}
     *
     * @return the asset form of the given summary, or {@code null} if it has none
     */
    protected @Nullable MekSummary assetSummaryFor(@Nullable MekSummary ms) {
        if (ms == null) {
            return null;
        }
        return ms.isBattlefieldSupportAsset() ? ms : mscInstance.getLinkedAsset(ms);
    }

    /**
     * Returns the cost, in Battle Value, to display for the given asset summary, honoring the selected Regular/Veteran
     * asset skill. When Veteran is selected and the asset has a Veteran variant, its Veteran cost is returned;
     * otherwise the Regular cost is returned (for a Veteran selection on an asset with no Veteran variant, the Regular
     * cost is shown with an asterisk by the renderer).
     *
     * @param asset an asset summary, or {@code null}
     *
     * @return the asset cost in Battle Value, or 0 if {@code asset} is {@code null}
     */
    protected int assetCostBv(@Nullable MekSummary asset) {
        if (asset == null) {
            return 0;
        }
        if (showVeteranAssetCost() && (asset.getBfsVeteranBv() > 0)) {
            return asset.getBfsVeteranBv();
        }
        return asset.getBV();
    }

    /** @return true if the asset skill selector is set to Veteran (so Veteran asset costs are shown). */
    protected boolean showVeteranAssetCost() {
        return assetSkillChooser.getSelectedIndex() == 1;
    }

    /** @return the Gunnery skill currently entered in the Skills controls (default 4). */
    public int getSelectedGunnery() {
        return parseSkillValue(textGunnery, 4);
    }

    /** @return the Piloting skill currently entered in the Skills controls (default 5). */
    public int getSelectedPiloting() {
        return parseSkillValue(textPilot, 5);
    }

    /** @return true if the asset skill selector is set to Veteran (rather than Regular). */
    public boolean isVeteranAssetSkillSelected() {
        return showVeteranAssetCost();
    }

    /**
     * @param asset an asset summary, or {@code null}
     *
     * @return true if the asset cost shown for this row is a Regular cost standing in for a missing Veteran cost (i.e.
     *       Veteran is selected but the asset has no Veteran variant) and should therefore be marked with an asterisk
     */
    protected boolean isAssetCostVeteranFallback(@Nullable MekSummary asset) {
        return showVeteranAssetCost() && (asset != null) && (asset.getBfsVeteranBv() == 0);
    }

    /** @return true if the current selection has a Battlefield Support Asset form (see {@link #getSelectedAssetSummary()}). */
    public boolean selectionHasAssetForm() {
        return getSelectedAssetSummary() != null;
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
        meks = condenseLinkedAssets(mscInstance.getAllMeks());
        unitLoadingDialog.setVisible(false);

        // break out if there are no units to filter
        if (meks == null) {
            logger.error("No meks were loaded");
        } else {
            SwingUtilities.invokeLater(() -> unitModel.setData(meks));
        }
    }

    /**
     * Removes Battlefield Support Asset summaries that have a linked base unit, so that a linked base/asset pair is
     * shown as a single row (the base unit's). Standalone assets (no base unit) and all non-asset units are kept.
     * When there are no assets this returns the input unchanged.
     *
     * @param allMeks the full set of unit summaries
     *
     * @return the condensed set, or {@code null} if the input was {@code null}
     */
    protected MekSummary[] condenseLinkedAssets(MekSummary[] allMeks) {
        if (allMeks == null) {
            return null;
        }
        return Arrays.stream(allMeks)
              .filter(ms -> !(ms.isBattlefieldSupportAsset() && (mscInstance.getLinkedBaseUnit(ms) != null)))
              .toArray(MekSummary[]::new);
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
              || advancedSearchDialog.getASAdvancedSearch().isActive()
              || advancedSearchDialog.getBFSAdvancedSearch().isActive());
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

    /**
     * Toggles the Battlefield Support asset cost column between the asset's cost in Battle Value ("BFS BV") and its
     * cost in Battlefield Support Points ("BSP").
     */
    private void toggleAssetCost(boolean showBsp) {
        unitColumnModel.setColumnVisible(assetBspColumn, showBsp);
        unitColumnModel.setColumnVisible(assetBvColumn, !showBsp);
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
        private static final int COL_ASSET_BV = 5;
        private static final int COL_ASSET_BSP = 6;
        private static final int COL_YEAR = 7;
        private static final int COL_COST = 8;
        private static final int COL_LEVEL = 9;
        private static final int COL_VTL = 10;
        private static final int N_COL = 11;

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
                case COL_ASSET_BV -> I18n.getTextAt("megamek.client.messages", "MekView.column.bfsBV");
                case COL_ASSET_BSP -> I18n.getTextAt("megamek.client.messages", "MekView.column.bsp");
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
                // Standalone assets have no Battle Value of their own (their cost lives in the asset cost column); a
                // blank BV cell distinguishes them at a glance from linked units that show both.
                if (ms.isBattlefieldSupportAsset()) {
                    return 0;
                }
                int gunnery = parseSkillValue(textGunnery, 4);
                int piloting = parseSkillValue(textPilot, 5);
                double gp_multiply = BVCalculator.bvSkillMultiplier(gunnery, piloting);
                return (int) Math.round(ms.getBV() * gp_multiply);

            } else if (col == COL_PV) {
                // Point Value is an Alpha Strike concept; assets have none, so their PV cell is blank.
                if (ms.isBattlefieldSupportAsset()) {
                    return 0;
                }
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
            } else if (col == COL_ASSET_BV) {
                // The asset's cost expressed in Battle Value, shown on asset rows and base units that have a linked
                // asset; 0 (rendered blank) for units with no asset form. Reflects the selected Regular/Veteran skill.
                return assetCostBv(assetSummaryFor(ms));
            } else if (col == COL_ASSET_BSP) {
                // The asset's cost in Battlefield Support Points (BV / 20); 0 (rendered blank) when there is no asset.
                int bv = assetCostBv(assetSummaryFor(ms));
                return (bv == 0) ? 0 : bv / BattlefieldSupportAsset.BV_PER_BSP;
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

    /** Renderer for the Battlefield Support asset cost columns: right-aligned, blank when the unit has no asset. */
    public class AssetCostRenderer extends DefaultTableCellRenderer {

        public AssetCostRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
              int row, int column) {

            String text = "";
            if ((value instanceof Integer cost) && (cost != 0)) {
                text = String.format(MegaMek.getMMOptions().getLocale(), "%,d", cost);
                // Mark a Regular cost shown in place of a missing Veteran cost with an asterisk.
                int modelRow = table.convertRowIndexToModel(row);
                MekSummary asset = assetSummaryFor(unitModel.getMekSummary(modelRow));
                if (isAssetCostVeteranFallback(asset)) {
                    text += "*";
                }
            }
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Renderer for the BV and PV columns: centered, but blank for Battlefield Support Asset rows, which have no BV or
     * PV of their own (their cost lives in the asset cost column; PV is an Alpha Strike concept).
     */
    public class BvPvRenderer extends DefaultTableCellRenderer {

        public BvPvRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
              int row, int column) {

            Object display = value;
            int modelRow = table.convertRowIndexToModel(row);
            MekSummary ms = unitModel.getMekSummary(modelRow);
            if ((ms != null) && ms.isBattlefieldSupportAsset()) {
                display = "";
            }
            return super.getTableCellRendererComponent(table, display, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Refreshes the table's BV/PV columns and the preview's Analysis tab when gunnery/piloting is
     * changed.
     */
    private class GPDocumentListener implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            refreshSkillAdjustedColumns();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            refreshSkillAdjustedColumns();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            refreshSkillAdjustedColumns();
        }
    }

    /**
     * Refreshes the skill-adjusted columns (BV and asset cost) after a skill input change, unless refresh is currently
     * suppressed to coalesce a batch of changes into a single refresh (see {@link #toggleGP()}).
     */
    private void refreshSkillAdjustedColumns() {
        if (!suppressSkillRefresh) {
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
