/*
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.randomArmy;

import megamek.client.generator.RandomUnitGenerator;
import megamek.client.generator.RandomUnitGenerator.RatTreeNode;
import megamek.client.ratgenerator.*;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.advancedsearch.AdvancedSearchDialog;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;
import megamek.client.ui.models.UnitTableModel;
import megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility;
import megamek.client.ui.util.ScalingPopup;
import megamek.client.ui.util.UIUtil;
import megamek.common.TechConstants;
import megamek.common.loaders.MekSummary;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.common.util.RandomArmyCreator;
import megamek.logging.MMLogger;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * This class is the framework for the random army dialog that is most prominently used in MM's lobby. Subclasses of
 * it can be used anywhere. It requires the data/names and data/rat folders for full functionality. Subclasses can
 * generate a button panel that allows interaction with the results (see the present subclasses for examples).
 * Subclasses or callers can also supply GameOptions that influence some of the generators.
 */
public abstract class AbstractRandomArmyDialog extends JDialog implements ActionListener, TreeSelectionListener {
    private static final MMLogger LOGGER = MMLogger.create(AbstractRandomArmyDialog.class);

    // TODO: separate the panels
    // TODO: provide a common results API

    protected static final int TAB_BV_MATCHING = 0;
    protected static final int TAB_RAT = 1;
    protected static final int TAB_RAT_GENERATOR = 2;
    protected static final int TAB_FORMATION_BUILDER = 3;
    protected static final int TAB_FORCE_GENERATOR = 4;

    private static final String CARD_PREVIEW = "card_preview";
    private static final String CARD_FORCE_TREE = "card_force_tree";

    protected final JFrame parentFrame;

    AdvancedSearchDialog asd;

    private MekSearchFilter searchFilter;

    private final JComboBox<String> m_chType = new JComboBox<>();

    private final JTree m_treeRAT = new JTree();
    protected final JTabbedPane tabbedPane = new JTabbedPane();
    private final JPanel m_pRAT = new JPanel();
    private final JPanel m_pRATGen = new JPanel();
    private final JPanel m_pFormations = new JPanel();
    protected final ForceGeneratorViewUi m_pForceGen;
    private ForceGenerationOptionsPanel m_pRATGenOptions;
    private final JPanel m_pUnitTypeOptions = new JPanel(new CardLayout());
    protected ForceGenerationOptionsPanel m_pFormationOptions;
    private final JPanel m_pParameters = new JPanel();
    private final JPanel m_pPreview = new JPanel();
    private final JPanel m_pAdvSearch = new JPanel();
    private final JButton m_bAdvSearch = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearch"));
    private final JButton m_bAdvSearchClear = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearchClear"));
    private final JLabel m_lMekCount = new JLabel();
    private final JLabel m_lVehicleCount = new JLabel();
    private final JLabel m_lBattleArmorCount = new JLabel();
    private final JLabel m_lInfantryCount = new JLabel();
    private final JButton m_bGenerate = new JButton(Messages.getString("RandomArmyDialog.Generate"));
    private final JButton m_bAddToForce = new JButton(Messages.getString("RandomArmyDialog.AddToForce"));

    private final CardLayout m_lRightCards = new CardLayout();
    private final JPanel m_pRightPane = new JPanel(m_lRightCards);
    private final JSplitPane m_pSplit;

    private final JButton m_bAddAll = new JButton(Messages.getString("RandomArmyDialog.AddAll"));
    private final JButton m_bAdd = new JButton(Messages.getString("RandomArmyDialog.AddSelected"));
    private final JButton m_bRoll = new JButton(Messages.getString("RandomArmyDialog.Roll"));
    private final JButton m_bClear = new JButton(Messages.getString("RandomArmyDialog.Clear"));

    private JTable m_lArmy;
    private final RandomArmyTableMouseAdapter armyTableMouseAdapter = new RandomArmyTableMouseAdapter();
    protected TableRowSorter<UnitTableModel> armySorter;
    protected JLabel m_lArmyBVTotal;
    private JTable m_lUnits;
    private final RandomArmyTableMouseAdapter unitsTableMouseAdapter = new RandomArmyTableMouseAdapter();
    protected TableRowSorter<UnitTableModel> unitsSorter;
    protected JLabel m_lUnitsBVTotal;
    protected JTable m_lRAT;
    private final RandomArmyTableMouseAdapter ratTableMouseAdapter = new RandomArmyTableMouseAdapter();
    protected TableRowSorter<RATTableModel> ratSorter;

    protected UnitTableModel armyModel;
    protected UnitTableModel unitsModel;
    private RATTableModel ratModel;

    private final JLabel m_labBV = new JLabel(Messages.getString("RandomArmyDialog.BV"));
    private final JLabel m_labYear = new JLabel(Messages.getString("RandomArmyDialog.Year"));
    private final JLabel m_labMeks = new JLabel(Messages.getString("RandomArmyDialog.Meks"));
    private final JLabel m_labVees = new JLabel(Messages.getString("RandomArmyDialog.Vees"));
    private final JLabel m_labBA = new JLabel(Messages.getString("RandomArmyDialog.BA"));
    private final JLabel m_labInfantry = new JLabel(Messages.getString("RandomArmyDialog.Infantry"));
    private final JLabel m_labTech = new JLabel(Messages.getString("RandomArmyDialog.Tech"));
    private final JLabel m_labUnits = new JLabel(Messages.getString("RandomArmyDialog.Unit"));
    private final JLabel m_ratStatus;

    private final JTextField m_tBVMin = new JTextField(6);
    private final JTextField m_tBVMax = new JTextField(6);
    private final JTextField m_tMinYear = new JTextField(4);
    private final JTextField m_tMaxYear = new JTextField(4);
    private final JTextField m_tMeks = new JTextField(3);
    private final JTextField m_tVees = new JTextField(3);
    private final JTextField m_tBA = new JTextField(3);
    private final JTextField m_tInfantry = new JTextField(3);
    private final JTextField m_tUnits = new JTextField(3);
    private final JCheckBox m_chkPad = new JCheckBox(Messages.getString("RandomArmyDialog.Pad"));
    private final JCheckBox m_chkCanon = new JCheckBox(Messages.getString("RandomArmyDialog.Canon"));

    protected final RandomUnitGenerator rug;
    private UnitTable generatedRAT;

    private JComponent buttonPanel;

    static final String UNITS_BV = "UNITS_BV";
    static final String UNITS_COST = "UNITS_COST";
    static final String UNITS_VIEW = "UNITS_VIEW";
    static final String ARMY_BV = "ARMY_BV";
    static final String ARMY_COST = "ARMY_COST";
    static final String ARMY_VIEW = "ARMY_VIEW";
    static final String RAT_BV = "RAT_BV";
    static final String RAT_COST = "RAT_COST";
    static final String RAT_VIEW = "RAT_VIEW";

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private GameOptions gameOptions = new GameOptions();

    public AbstractRandomArmyDialog(JFrame parent) {
        super(parent, Messages.getString("RandomArmyDialog.title"), ModalityType.APPLICATION_MODAL);
        parentFrame = parent;

        rug = RandomUnitGenerator.getInstance();
        rug.registerListener(this);
        if (rug.isInitialized()) {
            m_ratStatus = new JLabel(Messages.getString("RandomArmyDialog.ratStatusDoneLoading"));
        } else {
            m_ratStatus = new JLabel(Messages.getString("RandomArmyDialog.ratStatusLoading"));
        }

        createBVPanel();
        createRATPanel();
        createRATGenPanel();
        createFormationPanel();
        createPreviewPanel();
        m_pForceGen = new ForceGeneratorViewUi(parentFrame, gameOptions);

        tabbedPane.addTab(Messages.getString("RandomArmyDialog.BVtab"), new JScrollPane(m_pParameters));
        tabbedPane.addTab(Messages.getString("RandomArmyDialog.RATtab"), m_pRAT);
        tabbedPane.addTab(Messages.getString("RandomArmyDialog.RATGentab"), m_pRATGen);
        tabbedPane.addTab(Messages.getString("RandomArmyDialog.Formationtab"), m_pFormations);
        tabbedPane.addTab(Messages.getString("RandomArmyDialog.Forcetab"), m_pForceGen.getLeftPanel());
        tabbedPane.addChangeListener(ev -> {
            if (tabbedPane.getSelectedIndex() == TAB_FORCE_GENERATOR) {
                m_lRightCards.show(m_pRightPane, CARD_FORCE_TREE);
            } else {
                m_lRightCards.show(m_pRightPane, CARD_PREVIEW);
            }
        });

        m_pRightPane.add(m_pPreview, CARD_PREVIEW);
        m_pRightPane.add(m_pForceGen.getRightPanel(), CARD_FORCE_TREE);
        m_pRightPane.setMinimumSize(new Dimension(0, 0));

        m_pSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, m_pRightPane);
        m_pSplit.setOneTouchExpandable(false);
        m_pSplit.setResizeWeight(0.5);

        // construct the main dialog
        setLayout(new BorderLayout());
        add(m_pSplit, BorderLayout.CENTER);
        validate();
        setLocationRelativeTo(parentFrame);

        m_pSplit.setDividerLocation(GUIP.getRndArmySplitPos());
        setSize(GUIP.getRndArmySizeWidth(), GUIP.getRndArmySizeHeight());
        setLocation(GUIP.getRndArmyPosX(), GUIP.getRndArmyPosY());

        String closeAction = "closeAction";
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
        getRootPane().getActionMap().put(closeAction, new CloseAction(this));

        addWindowListener(windowListener);
    }

    public void setGameOptions(GameOptions newOptions) {
        gameOptions = newOptions;
        updateRATYear();
    }

    /**
     * Override to add buttons or other components to the bottom of the dialog. The returned panel is added when
     * setVisible(true) is called for the first time; it is added as BorderLayout.SOUTH and is therefore stretched to
     * the width of the dialog.
     *
     * @return A button panel for the dialog
     */
    protected abstract JComponent createButtonsPanel();

    private void createBVPanel() {
        asd = new AdvancedSearchDialog(parentFrame, gameOptions.intOption(OptionsConstants.ALLOWED_YEAR));

        // set defaults
        m_tMeks.setText(GUIP.getRATNumMeks());
        m_tBVMin.setText(GUIP.getRATBVMin());
        m_tBVMax.setText(GUIP.getRATBVMax());
        m_tVees.setText(GUIP.getRATNumVees());
        m_tBA.setText(GUIP.getRATNumBA());
        m_tMinYear.setText(GUIP.getRATYearMin());
        m_tMaxYear.setText(GUIP.getRATYearMax());
        m_tInfantry.setText(GUIP.getRATNumInf());
        m_chkPad.setSelected(GUIP.getRATPadBV());
        m_chkCanon.setSelected(gameOptions.booleanOption(OptionsConstants.ALLOWED_CANON_ONLY));
        updateTechChoice();

        // construct the Adv Search Panel
        m_pAdvSearch.setLayout(new FlowLayout(FlowLayout.LEADING));
        m_pAdvSearch.add(m_bAdvSearch);
        m_pAdvSearch.add(m_bAdvSearchClear);
        m_bAdvSearchClear.setEnabled(false);
        m_bAdvSearch.addActionListener(this);
        m_bAdvSearchClear.addActionListener(this);

        // construct the parameters panel
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        m_pParameters.setLayout(layout);
        constraints.insets = new Insets(5, 5, 0, 0);
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
        layout.setConstraints(m_tBVMin, constraints);
        m_pParameters.add(m_tBVMin);
        JLabel dash = new JLabel("-");
        layout.setConstraints(dash, constraints);
        m_pParameters.add(dash);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_tBVMax, constraints);
        m_pParameters.add(m_tBVMax);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labMeks, constraints);
        m_pParameters.add(m_labMeks);
        layout.setConstraints(m_tMeks, constraints);
        m_pParameters.add(m_tMeks);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_lMekCount, constraints);
        m_pParameters.add(m_lMekCount);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labVees, constraints);
        m_pParameters.add(m_labVees);
        layout.setConstraints(m_tVees, constraints);
        m_pParameters.add(m_tVees);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_lVehicleCount, constraints);
        m_pParameters.add(m_lVehicleCount);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labBA, constraints);
        m_pParameters.add(m_labBA);
        layout.setConstraints(m_tBA, constraints);
        m_pParameters.add(m_tBA);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_lBattleArmorCount, constraints);
        m_pParameters.add(m_lBattleArmorCount);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layout.setConstraints(m_labInfantry, constraints);
        m_pParameters.add(m_labInfantry);
        layout.setConstraints(m_tInfantry, constraints);
        m_pParameters.add(m_tInfantry);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layout.setConstraints(m_lInfantryCount, constraints);
        m_pParameters.add(m_lInfantryCount);
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
    }

    private void createRATPanel() {
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
        c.insets = new Insets(5, 5, 5, 5);

        m_treeRAT.setRootVisible(false);
        m_treeRAT.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_treeRAT.addTreeSelectionListener(this);

        JScrollPane treeViewRAT = new JScrollPane(m_treeRAT);
        treeViewRAT.setPreferredSize(new Dimension(300, 200));
        m_pRAT.add(treeViewRAT, c);
    }

    private void createRATGenPanel() {
        // construct the RAT Generator panel
        m_pRATGen.setLayout(new GridBagLayout());
        // put the general options and the unit-specific options into a single panel so
        // they scroll together.
        JPanel pRATGenTop = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        m_pRATGen.add(new JScrollPane(pRATGenTop), gridBagConstraints);

        m_pRATGenOptions = new ForceGenerationOptionsPanel(ForceGenerationOptionsPanel.Use.RAT_GENERATOR);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.EAST;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        pRATGenTop.add(m_pRATGenOptions, gridBagConstraints);
        m_pRATGenOptions.setYear(gameOptions.intOption("year"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.EAST;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        pRATGenTop.add(m_pUnitTypeOptions, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        m_pRATGen.add(m_bGenerate, gridBagConstraints);
        m_bGenerate.setToolTipText(Messages.getString("RandomArmyDialog.Generate.tooltip"));
        m_bGenerate.addActionListener(this);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        m_pRATGen.add(m_bAddToForce, gridBagConstraints);
        m_bAddToForce.setToolTipText(Messages.getString("RandomArmyDialog.AddToForce.tooltip"));
        m_bAddToForce.addActionListener(this);

        ratModel = new RATTableModel();
        m_lRAT = new JTable();
        m_lRAT.setName("RAT");
        m_lRAT.addMouseListener(ratTableMouseAdapter);
        m_lRAT.setModel(ratModel);
        ratSorter = new TableRowSorter<>(ratModel);
        m_lRAT.setRowSorter(ratSorter);
        m_lRAT.setIntercellSpacing(new Dimension(5, 0));
        m_lRAT.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        for (int i = 0; i < ratModel.getColumnCount(); i++) {
            m_lRAT.getColumnModel().getColumn(i).setPreferredWidth(ratModel.getPreferredWidth(i));
        }
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        m_lRAT.getColumnModel().getColumn(RATTableModel.COL_BV).setCellRenderer(rightRenderer);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        m_pRATGen.add(new JScrollPane(m_lRAT), gridBagConstraints);
    }

    private void createFormationPanel() {
        // formation builder tab
        m_pFormationOptions = new ForceGenerationOptionsPanel(ForceGenerationOptionsPanel.Use.FORMATION_BUILDER);
        m_pFormationOptions.setYear(gameOptions.intOption("year"));
        m_pFormations.setLayout(new BorderLayout());
        m_pFormations.add(new JScrollPane(m_pFormationOptions), BorderLayout.CENTER);
    }

    private void createPreviewPanel() {
        // construct the preview panel
        unitsModel = new UnitTableModel();
        m_lUnits = new JTable();
        m_lUnits.setName("Units");
        m_lUnits.addMouseListener(unitsTableMouseAdapter);
        m_lUnits.setModel(unitsModel);
        unitsSorter = new TableRowSorter<>(unitsModel);
        m_lUnits.setRowSorter(unitsSorter);
        m_lUnits.setIntercellSpacing(new Dimension(0, 0));
        m_lUnits.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(m_lUnits);
        scroll.setBorder(BorderFactory.createTitledBorder(Messages.getString("RandomArmyDialog.Army")));
        m_lUnitsBVTotal = new JLabel("BV Total: 0");
        armyModel = new UnitTableModel();
        m_lArmy = new JTable();
        m_lArmy.setName("Army");
        m_lArmy.addMouseListener(armyTableMouseAdapter);
        m_lArmy.setModel(armyModel);
        armySorter = new TableRowSorter<>(armyModel);
        m_lArmy.setRowSorter(armySorter);
        m_lArmy.setIntercellSpacing(new Dimension(0, 0));
        m_lArmy.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_lArmyBVTotal = new JLabel("BV Total: 0");
        JScrollPane scrollArmy = new JScrollPane(m_lArmy);
        scrollArmy.setBorder(BorderFactory.createTitledBorder(Messages.getString("RandomArmyDialog.SelectedUnits")));
        m_bRoll.addActionListener(this);
        m_bAddAll.addActionListener(this);
        m_bAdd.addActionListener(this);
        m_bClear.addActionListener(this);

        m_pPreview.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        m_pPreview.add(scroll, gridBagConstraints);
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        m_pPreview.add(m_lUnitsBVTotal, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        m_pPreview.add(m_bRoll, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        m_pPreview.add(m_bAddAll, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        m_pPreview.add(m_bAdd, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        m_pPreview.add(m_bClear, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        m_pPreview.add(scrollArmy, gridBagConstraints);
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        m_pPreview.add(m_lArmyBVTotal, gridBagConstraints);
        m_pPreview.setMinimumSize(new Dimension(0, 0));
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

    private int calculateTotal(JTable t, int col) {
        int total = 0;
        for (int i = 0; i < t.getRowCount(); i++) {
            try {
                total += Integer.parseInt(t.getValueAt(i, col) + "");
            } catch (Exception ignored) {
            }
        }
        return total;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String msg_bvTotal = Messages.getString("RandomArmyDialog.BVTotal");
        StringTokenizer stringTokenizer = new StringTokenizer(actionEvent.getActionCommand(), "|");
        String command = "";

        if (stringTokenizer.hasMoreTokens()) {
            command = stringTokenizer.nextToken();
        }

        if (actionEvent.getSource().equals(m_bClear)) {
            clearData();
        } else if (actionEvent.getSource().equals(m_bAddAll)) {
            for (MekSummary m : unitsModel.getAllUnits()) {
                armyModel.addUnit(m);
            }

            m_lArmyBVTotal.setText(msg_bvTotal + calculateTotal(m_lArmy, 1));
        } else if (actionEvent.getSource().equals(m_bAdd)) {
            for (int sel : m_lUnits.getSelectedRows()) {
                sel = m_lUnits.convertRowIndexToModel(sel);
                MekSummary m = unitsModel.getUnitAt(sel);
                armyModel.addUnit(m);
            }

            m_lArmyBVTotal.setText(msg_bvTotal + calculateTotal(m_lArmy, 1));
        } else if (actionEvent.getSource().equals(m_bAdvSearch)) {
            asd.showDialog();
            searchFilter = asd.getTWAdvancedSearch().getMekSearchFilter();
            m_bAdvSearchClear.setEnabled(searchFilter != null);
        } else if (actionEvent.getSource().equals(m_bAdvSearchClear)) {
            asd.clearSearches();
            searchFilter = null;
            m_bAdvSearchClear.setEnabled(false);
        } else if (actionEvent.getSource().equals(m_bRoll)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                if (tabbedPane.getSelectedIndex() == TAB_RAT) {
                    int units = Integer.parseInt(m_tUnits.getText());
                    if (units > 0) {
                        unitsModel.setData(RandomUnitGenerator.getInstance().generate(units));
                    }
                } else if (tabbedPane.getSelectedIndex() == TAB_RAT_GENERATOR) {
                    int units = m_pRATGenOptions.getNumUnits();
                    if (units > 0 && generatedRAT != null && generatedRAT.getNumEntries() > 0) {
                        unitsModel.setData(generatedRAT.generateUnits(units));
                    }
                    // generateUnits removes salvage entries that have no units meeting criteria
                    ratModel.refreshData();
                } else if (tabbedPane.getSelectedIndex() == TAB_FORMATION_BUILDER) {
                    ArrayList<MekSummary> unitList = new ArrayList<>();
                    FactionRecord fRec = m_pFormationOptions.getFaction();
                    FormationType ft = FormationType.getFormationType(m_pFormationOptions.getStringOption(
                          "formationType"));
                    List<Parameters> params = new ArrayList<>();
                    params.add(new Parameters(fRec,
                          m_pFormationOptions.getUnitType(),
                          m_pFormationOptions.getYear(),
                          m_pFormationOptions.getRating(),
                          null,
                          ModelRecord.NETWORK_NONE,
                          EnumSet.noneOf(EntityMovementMode.class),
                          EnumSet.noneOf(MissionRole.class),
                          0,
                          fRec));
                    List<Integer> numUnits = new ArrayList<>();
                    numUnits.add(m_pFormationOptions.getNumUnits());

                    if (m_pFormationOptions.getIntegerOption("numOtherUnits") > 0) {
                        if (m_pFormationOptions.getIntegerOption("otherUnitType") >= 0) {
                            params.add(new Parameters(fRec,
                                  m_pFormationOptions.getIntegerOption("otherUnitType"),
                                  m_pFormationOptions.getYear(),
                                  m_pFormationOptions.getRating(),
                                  null,
                                  ModelRecord.NETWORK_NONE,
                                  EnumSet.noneOf(EntityMovementMode.class),
                                  EnumSet.noneOf(MissionRole.class),
                                  0,
                                  fRec));
                            numUnits.add(m_pFormationOptions.getIntegerOption("numOtherUnits"));
                        } else if (m_pFormationOptions.getBooleanOption("mekBA")) {
                            // Make sure at least a number of units equals to the number of BA points/squads
                            // are omni
                            numUnits.set(0,
                                  Math.min(m_pFormationOptions.getIntegerOption("numOtherUnits"),
                                        m_pFormationOptions.getNumUnits()));
                            if (m_pFormationOptions.getNumUnits() >
                                  m_pFormationOptions.getIntegerOption("numOtherUnits")) {
                                params.add(params.get(0).copy());
                                numUnits.add(m_pFormationOptions.getNumUnits() -
                                      m_pFormationOptions.getIntegerOption("numOtherUnits"));
                            }
                            params.get(0).getRoles().add(MissionRole.MECHANIZED_BA);
                            // BA do not count for formation rules; add as a separate formation
                        }
                    }

                    if (ft != null) {
                        unitList.addAll(ft.generateFormation(params,
                              numUnits,
                              m_pFormationOptions.getIntegerOption("network"),
                              false));
                        if (!unitList.isEmpty() && (m_pFormationOptions.getIntegerOption("numOtherUnits") > 0)) {
                            if (m_pFormationOptions.getBooleanOption("mekBA")) {
                                // Try to generate the BA portion using the same formation type as
                                // the parent, otherwise generate randomly.
                                Parameters p = new Parameters(fRec,
                                      UnitType.BATTLE_ARMOR,
                                      m_pFormationOptions.getYear(),
                                      m_pFormationOptions.getRating(),
                                      null,
                                      ModelRecord.NETWORK_NONE,
                                      EnumSet.noneOf(EntityMovementMode.class),
                                      EnumSet.of(MissionRole.MECHANIZED_BA),
                                      0,
                                      fRec);
                                List<MekSummary> ba = ft.generateFormation(p,
                                      m_pFormationOptions.getIntegerOption("numOtherUnits"),
                                      ModelRecord.NETWORK_NONE,
                                      true);
                                if (ba.isEmpty()) {
                                    ba = UnitTable.findTable(p)
                                          .generateUnits(m_pFormationOptions.getIntegerOption("numOtherUnits"));
                                }
                                unitList.addAll(ba);
                            } else if (m_pFormationOptions.getBooleanOption("airLance")) {
                                UnitTable t = UnitTable.findTable(fRec,
                                      UnitType.AEROSPACE_FIGHTER,
                                      m_pFormationOptions.getYear(),
                                      m_pFormationOptions.getRating(),
                                      null,
                                      ModelRecord.NETWORK_NONE,
                                      EnumSet.noneOf(EntityMovementMode.class),
                                      EnumSet.noneOf(MissionRole.class),
                                      0,
                                      fRec);
                                MekSummary unit = t.generateUnit();
                                if (unit != null) {
                                    unitList.add(unit);
                                    MekSummary unit2 = t.generateUnit(ms -> ms.getChassis().equals(unit.getChassis()));
                                    unitList.add(Objects.requireNonNullElse(unit2, unit));
                                }
                            }
                        }
                    } else {
                        LOGGER.error("Could not find formation type {}",
                              m_pFormationOptions.getStringOption("formationType"));
                    }
                    unitsModel.setData(unitList);
                    m_pFormationOptions.updateGeneratedUnits(unitList);
                } else {
                    StringBuilder sbMek = new StringBuilder();
                    StringBuilder sbVehicle = new StringBuilder();
                    StringBuilder sbBattleArmor = new StringBuilder();
                    StringBuilder sbInfantry = new StringBuilder();
                    RandomArmyCreator.Parameters parameters = new RandomArmyCreator.Parameters();
                    parameters.advancedSearchFilter = searchFilter;
                    parameters.asPanel = asd.getASAdvancedSearch();
                    parameters.meks = Integer.parseInt(m_tMeks.getText());
                    parameters.tanks = Integer.parseInt(m_tVees.getText());
                    parameters.ba = Integer.parseInt(m_tBA.getText());
                    parameters.infantry = Integer.parseInt(m_tInfantry.getText());
                    parameters.canon = m_chkCanon.isSelected();
                    parameters.maxBV = Integer.parseInt(m_tBVMax.getText());
                    parameters.minBV = Integer.parseInt(m_tBVMin.getText());
                    parameters.padWithInfantry = m_chkPad.isSelected();
                    parameters.tech = m_chType.getSelectedIndex();
                    parameters.minYear = Integer.parseInt(m_tMinYear.getText());
                    parameters.maxYear = Integer.parseInt(m_tMaxYear.getText());
                    unitsModel.setData(RandomArmyCreator.generateArmy(parameters,
                          sbMek,
                          sbVehicle,
                          sbBattleArmor,
                          sbInfantry));
                    String msg_outOf = Messages.getString("RandomArmyDialog.OutOf");
                    m_lMekCount.setText(String.format(msg_outOf + sbMek));
                    m_lVehicleCount.setText(String.format(msg_outOf + sbVehicle));
                    m_lBattleArmorCount.setText(String.format(msg_outOf + sbBattleArmor));
                    m_lInfantryCount.setText(String.format(msg_outOf + sbInfantry));
                }

                m_lUnitsBVTotal.setText(msg_bvTotal + calculateTotal(m_lUnits, 1));
            } catch (NumberFormatException ignored) {

            } finally {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        } else if (actionEvent.getSource().equals(m_bGenerate)) {
            generateRAT();
        } else if (actionEvent.getSource().equals(m_bAddToForce)) {
            if (generatedRAT != null) {
                for (int sel : m_lRAT.getSelectedRows()) {
                    sel = m_lRAT.convertRowIndexToModel(sel);
                    MekSummary ms = generatedRAT.getMekSummary(sel);
                    if (ms != null) {
                        armyModel.addUnit(ms);
                    }
                }

                m_lArmyBVTotal.setText(msg_bvTotal + calculateTotal(m_lArmy, 1));
            }
        } else if (actionEvent.getSource().equals(rug)) {
            m_ratStatus.setText(Messages.getString("RandomArmyDialog.ratStatusDoneLoading"));
            updateRATs();
        } else if (command.equals(UNITS_VIEW)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), unitsModel);
            LobbyUtility.mekReadoutAction(entities, true, true, parentFrame);
        } else if (command.equals(ARMY_VIEW)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), armyModel);
            LobbyUtility.mekReadoutAction(entities, true, true, parentFrame);
        } else if (command.equals(RAT_VIEW)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), ratModel);
            LobbyUtility.mekReadoutAction(entities, true, true, parentFrame);
        } else if (command.equals(UNITS_BV)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), unitsModel);
            LobbyUtility.mekBVAction(entities, true, true, parentFrame);
        } else if (command.equals(ARMY_BV)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), armyModel);
            LobbyUtility.mekBVAction(entities, true, true, parentFrame);
        } else if (command.equals(RAT_BV)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), ratModel);
            LobbyUtility.mekBVAction(entities, true, true, parentFrame);
        } else if (command.equals(UNITS_COST)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), unitsModel);
            LobbyUtility.mekCostAction(entities, true, true, parentFrame);
        } else if (command.equals(ARMY_COST)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), armyModel);
            LobbyUtility.mekCostAction(entities, true, true, parentFrame);
        } else if (command.equals(RAT_COST)) {
            // The entities list may be empty
            Set<Entity> entities = LobbyUtility.getEntities(stringTokenizer.nextToken(), ratModel);
            LobbyUtility.mekCostAction(entities, true, true, parentFrame);
        }
    }

    WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent evt) {
            saveWindowSettings();
        }

        private void saveWindowSettings() {
            GUIP.setRndArmySizeHeight(getSize().height);
            GUIP.setRndArmySizeWidth(getSize().width);
            GUIP.setRndArmyPosX(getLocation().x);
            GUIP.setRndArmyPosY(getLocation().y);
            GUIP.setRndArmySplitPos(m_pSplit.getDividerLocation());
        }
    };

    private void updateTechChoice() {
        final int gameTL = TechConstants.getSimpleLevel(gameOptions
              .stringOption(OptionsConstants.ALLOWED_TECH_LEVEL));
        final int maxTech = switch (gameTL) {
            case TechConstants.T_SIMPLE_INTRO -> TechConstants.T_INTRO_BOX_SET;
            case TechConstants.T_SIMPLE_ADVANCED -> TechConstants.T_CLAN_ADVANCED;
            case TechConstants.T_SIMPLE_EXPERIMENTAL -> TechConstants.T_CLAN_EXPERIMENTAL;
            case TechConstants.T_SIMPLE_UNOFFICIAL -> TechConstants.T_ALL;
            default -> TechConstants.T_TW_ALL;
        };

        m_chType.removeAllItems();
        for (int i = 0; i <= maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        m_chType.setSelectedIndex(Math.min(GUIP.getRATTechLevel(), maxTech));
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

        String selectedRATPath = GUIP.getRATSelectedRAT();
        if (!selectedRATPath.isBlank()) {
            String[] nodes = selectedRATPath.replace('[', ' ').replace(']', ' ').split(",");
            TreePath path = findPathByName(nodes);
            m_treeRAT.setSelectionPath(path);
        }
    }

    protected void updateRATYear() {
        int gameYear = gameOptions.intOption("year");
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
            generatedRAT = UnitTable.findTable(fRec,
                  m_pRATGenOptions.getUnitType(),
                  m_pRATGenOptions.getYear(),
                  m_pRATGenOptions.getRating(),
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
            if (buttonPanel == null) {
                buttonPanel = createButtonsPanel();
                add(buttonPanel, BorderLayout.SOUTH);
            }
            updateTechChoice();
            updateRATs();
        }

        super.setVisible(show);
    }

    static ScalingPopup getPopup(List<Integer> entities, ActionListener listener, String tableName) {
        ScalingPopup popup = new ScalingPopup();

        // All command strings should follow the layout COMMAND|INFO|ID1,ID2,I3...
        // and use -1 when something is not needed (COMMAND|-1|-1)
        String eIds = LobbyUtility.enToken(entities);

        String view = "";
        String cost = "";
        String bv = "";

        switch (tableName) {
            case "Units" -> {
                bv = UNITS_BV;
                cost = UNITS_COST;
                view = UNITS_VIEW;
            }
            case "Army" -> {
                bv = ARMY_BV;
                cost = ARMY_COST;
                view = ARMY_VIEW;
            }
            case "RAT" -> {
                bv = RAT_BV;
                cost = RAT_COST;
                view = RAT_VIEW;
            }
        }

        String msg_view = Messages.getString("RandomArmyDialog.View");
        String msg_viewBV = Messages.getString("RandomArmyDialog.ViewBV");
        String msg_viewCost = Messages.getString("RandomArmyDialog.ViewCost");

        popup.add(UIUtil.menuItem(msg_view, view + eIds, true, listener, KeyEvent.VK_V));
        popup.add(UIUtil.menuItem(msg_viewBV, bv + eIds, true, listener, KeyEvent.VK_B));
        popup.add(UIUtil.menuItem(msg_viewCost, cost + eIds, true, listener, Integer.MIN_VALUE));

        return popup;
    }

    /** Shows the right-click menu on the mek table */
    private static void showPopup(MouseEvent e, ActionListener listener) {
        if (e.getSource() instanceof JTable sTable) {

            if (sTable.getSelectedRowCount() == 0) {
                return;
            }

            List<Integer> entities = LobbyUtility.getSelectedEntities(sTable);
            ScalingPopup popup = getPopup(entities, listener, sTable.getName());
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public class RandomArmyTableMouseAdapter extends MouseInputAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e, AbstractRandomArmyDialog.this);
            }
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
        private static final int COL_UNIT_ROLE = 4;
        private static final int N_COL = 5;

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
            return switch (col) {
                case COL_WEIGHT -> 12;
                case COL_UNIT -> 240;
                case COL_BV -> 18;
                case COL_CL_IS, COL_UNIT_ROLE -> 20;
                default -> 0;
            };
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_WEIGHT -> Messages.getString("RandomArmyDialog.colWeight");
                case COL_UNIT -> Messages.getString("RandomArmyDialog.colUnit");
                case COL_BV -> Messages.getString("RandomArmyDialog.colBV");
                case COL_CL_IS -> Messages.getString("RandomArmyDialog.colCLIS");
                case COL_UNIT_ROLE -> Messages.getString("RandomArmyDialog.colUnitRole");
                default -> "??";
            };
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (generatedRAT != null && generatedRAT.getNumEntries() > 0) {
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
                    case COL_UNIT_ROLE:
                        return generatedRAT.getUnitRole(row);
                }
            }
            return "";
        }

        public MekSummary getUnitAt(int row) {
            if (generatedRAT != null && generatedRAT.getNumEntries() > 0) {
                return generatedRAT.getMekSummary(row);
            }
            return null;
        }
    }

    /**
     * Clears all rolled results from all of the tabs.
     */
    protected void clearData() {
        armyModel.clearData();
        unitsModel.clearData();
        String msg_bvTotal = Messages.getString("RandomArmyDialog.BVTotal");
        m_lUnitsBVTotal.setText(msg_bvTotal + "0");
        m_lArmyBVTotal.setText(msg_bvTotal + "0");
    }
}
