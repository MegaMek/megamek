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

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.Infantry;
import megamek.common.LAMPilot;
import megamek.common.MechFileParser;
import megamek.common.MechSearchFilter;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechView;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.MMLogger;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.templates.TROView;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 * This is a heavily reworked version of the original MechSelectorDialog which
 * brings up a list of units for the player to select to add to their forces.
 * The original list has been changed to a sortable table and a text filter
 * is used for advanced searching.
 */
public class UnitSelectorDialog extends JDialog implements Runnable, KeyListener, ActionListener,
        ListSelectionListener {
    //region Variable Declarations
    private static final long serialVersionUID = 8144354264100884817L;

    public static final String CLOSE_ACTION = "closeAction";
    public static final String SELECT_ACTION = "selectAction";

    public static final int ALLOWED_YEAR_ANY = 999999;

    private JButton buttonSelectClose;
    private JButton buttonSelect;
    private JButton buttonClose;
    private JButton buttonShowBV;
    private JButton buttonAdvancedSearch;
    private JButton buttonResetSearch;
    private JList<String> listTechLevel;
    /**
     * We need to map the selected index of listTechLevel to the actual TL it
     * belongs to
     */
    private Map<Integer, Integer> techLevelListToIndex;
    private JComboBox<String> comboUnitType;
    private JComboBox<String> comboWeight;
    private JLabel labelImage;
    private JTable tableUnits;
    private JTextField textFilter;
    private MechViewPanel panelMechView;
    private MechViewPanel panelTROView;
    private JComboBox<String> comboPlayer;

    private StringBuffer searchBuffer = new StringBuffer();
    private long lastSearch = 0;
    // how long after a key is typed does a new search begin
    private static final int KEY_TIMEOUT = 1000;

    private MechSummary[] mechs;

    // For MML
    private Entity chosenEntity;
    private boolean useAlternate = false;

    private MechTableModel unitModel;
    private MechSearchFilter searchFilter;

    private Client client;
    private ClientGUI clientGUI;
    private UnitLoadingDialog unitLoadingDialog;
    private AdvancedSearchDialog asd;
    private JFrame frame;

    private TableRowSorter<MechTableModel> sorter;

    private static MMLogger logger = DefaultMmLogger.getInstance();
    //endregion Variable Declarations

    /** Creates new UnitSelectorDialog form */
    public UnitSelectorDialog(ClientGUI clientGUI, UnitLoadingDialog unitLoadingDialog) {
        super(clientGUI.getFrame(), Messages.getString("MechSelectorDialog.title"), true); //$NON-NLS-1$
        this.unitLoadingDialog = unitLoadingDialog;
        frame = clientGUI.getFrame();
        client = clientGUI.getClient();
        this.clientGUI = clientGUI;

        initialize(client.getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR));
    }

    public UnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog) {
        super(frame, Messages.getString("MechSelectorDialog.title"), true); //$NON-NLS-1$
        this.unitLoadingDialog = unitLoadingDialog;
        this.frame = frame;

        initialize(ALLOWED_YEAR_ANY);

        run();
        setVisible(true);
    }

    private void initialize(int allowedYears) {
        unitModel = new MechTableModel();
        initComponents();
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        setSize(guiPreferences.getMechSelectorSizeWidth(), guiPreferences.getMechSelectorSizeHeight());
        setLocationRelativeTo(frame);
        asd = new AdvancedSearchDialog(frame, allowedYears);
    }

    private void initComponents() {
        setMinimumSize(new Dimension(640, 480));

        GridBagConstraints c;

        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setMinimumSize(new Dimension(500, 500));
        selectionPanel.setPreferredSize(new Dimension(500, 600));

        JScrollPane scrTableUnits = new JScrollPane();
        tableUnits = new JTable();
        tableUnits.addKeyListener(this);
        tableUnits.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "");
        JTabbedPane panPreview = new JTabbedPane();
        panelMechView = new MechViewPanel();
        panelMechView.setMinimumSize(new Dimension(300, 500));
        panelMechView.setPreferredSize(new Dimension(300, 600));
        panPreview.addTab("Summary", panelMechView);

        panelTROView = new MechViewPanel();
        panPreview.addTab("TRO", panelTROView);

        listTechLevel = new JList<>();
        listTechLevel.setToolTipText(Messages.getString("MechSelectorDialog.m_labelType.ToolTip")); //$NON-NLS-1$
        techLevelListToIndex = new HashMap<>();
        comboWeight = new JComboBox<>();
        comboUnitType = new JComboBox<>();
        textFilter = new JTextField();

        JLabel lblType = new JLabel(Messages.getString("MechSelectorDialog.m_labelType")); //$NON-NLS-1$
        lblType.setToolTipText(Messages.getString("MechSelectorDialog.m_labelType.ToolTip")); //$NON-NLS-1$
        JLabel lblWeight = new JLabel(Messages.getString("MechSelectorDialog.m_labelWeightClass")); //$NON-NLS-1$
        JLabel lblUnitType = new JLabel(Messages.getString("MechSelectorDialog.m_labelUnitType")); //$NON-NLS-1$
        JLabel lblFilter = new JLabel(Messages.getString("MechSelectorDialog.m_labelFilter")); //$NON-NLS-1$
        labelImage = new JLabel();
        JLabel lblPlayer = new JLabel(Messages.getString("MechSelectorDialog.m_labelPlayer"), SwingConstants.RIGHT); //$NON-NLS-1$
        lblPlayer.setVisible(!useAlternate);
        comboPlayer = new JComboBox<>();
        comboPlayer.setVisible(!useAlternate);

        getContentPane().setLayout(new GridBagLayout());

        scrTableUnits.setMinimumSize(new Dimension(500, 400));
        scrTableUnits.setPreferredSize(new Dimension(500, 400));

        tableUnits.setModel(unitModel);
        tableUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(unitModel);
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
        scrTableUnits.setViewportView(tableUnits);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        selectionPanel.add(scrTableUnits, c);

        JPanel panelFilterButtons = new JPanel(new GridBagLayout());
        panelFilterButtons.setMinimumSize(new Dimension(300, 180));
        panelFilterButtons.setPreferredSize(new Dimension(300, 180));

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblType, c);

        JScrollPane tlScroll = new JScrollPane(listTechLevel);
        tlScroll.setMinimumSize(new Dimension(300, 100));
        tlScroll.setPreferredSize(new Dimension(300, 100));
        updateTypeCombo();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(tlScroll, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblWeight, c);

        DefaultComboBoxModel<String> weightModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            weightModel.addElement(EntityWeightClass.getClassName(i));
        }
        weightModel.addElement(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        weightModel.setSelectedItem(EntityWeightClass.getClassName(0));
        comboWeight.setModel(weightModel);
        comboWeight.setSelectedItem(Messages.getString("MechSelectorDialog.All"));
        comboWeight.setMinimumSize(new Dimension(300, 27));
        comboWeight.setPreferredSize(new Dimension(300, 27));
        comboWeight.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboWeight, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblUnitType, c);

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        unitTypeModel.addElement(Messages.getString("MechSelectorDialog.All"));
        unitTypeModel.setSelectedItem(Messages.getString("MechSelectorDialog.All"));
        for (int i = 0; i < UnitType.SIZE; i++) {
            unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
        }
        unitTypeModel.addElement(Messages.getString("MechSelectorDialog.SupportVee"));
        comboUnitType.setModel(unitTypeModel);
        comboUnitType.setMinimumSize(new Dimension(300, 27));
        comboUnitType.setPreferredSize(new Dimension(300, 27));
        comboUnitType.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(comboUnitType, c);

        textFilter.setText("");
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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(textFilter, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        panelFilterButtons.add(lblFilter, c);

        labelImage = new JLabel("");
        labelImage.setHorizontalAlignment(SwingConstants.CENTER);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 4;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panelFilterButtons.add(labelImage, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.insets = new Insets(10, 10, 5, 0);
        selectionPanel.add(panelFilterButtons, c);

        JPanel panelSearchButtons = new JPanel(new GridBagLayout());

        buttonAdvancedSearch = new JButton(Messages.getString("MechSelectorDialog.AdvSearch"));
        buttonAdvancedSearch.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelSearchButtons.add(buttonAdvancedSearch, c);

        buttonResetSearch = new JButton(Messages.getString("MechSelectorDialog.Reset"));
        buttonResetSearch.addActionListener(this);
        buttonResetSearch.setEnabled(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridwidth = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        panelSearchButtons.add(buttonResetSearch, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.insets = new Insets(10, 10, 10, 0);
        selectionPanel.add(panelSearchButtons, c);

        JPanel panelOKButtons = new JPanel(new GridBagLayout());

        buttonSelect = new JButton(Messages.getString("MechSelectorDialog.m_bPick"));
        buttonSelect.addActionListener(this);
        buttonSelect.setVisible(!useAlternate);
        panelOKButtons.add(buttonSelect, new GridBagConstraints());

        buttonSelectClose = new JButton(Messages.getString("MechSelectorDialog.m_bPickClose"));
        buttonSelectClose.addActionListener(this);
        panelOKButtons.add(buttonSelectClose, new GridBagConstraints());

        buttonClose = new JButton(Messages.getString("Close"));
        buttonClose.addActionListener(this);
        panelOKButtons.add(buttonClose, new GridBagConstraints());

        if (!useAlternate) {
            updatePlayerChoice();
        }
        panelOKButtons.add(lblPlayer, new GridBagConstraints());
        panelOKButtons.add(comboPlayer, new GridBagConstraints());

        buttonShowBV = new JButton(Messages.getString("MechSelectorDialog.BV"));
        buttonShowBV.addActionListener(this);
        buttonShowBV.setVisible(!useAlternate);
        panelOKButtons.add(buttonShowBV, new GridBagConstraints());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                selectionPanel, panPreview);
        splitPane.setResizeWeight(0);
        c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1;
        getContentPane().add(splitPane, c);
        c.insets = new Insets(5,0,5,0);
        c.weightx = c.weighty = 0;
        c.gridy = 1;
        getContentPane().add(panelOKButtons, c);

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

    private void  updateTypeCombo() {
        listTechLevel.removeListSelectionListener(this);
        int[] selectedIndices = listTechLevel.getSelectedIndices();
        int gameTL;
        if (client != null) {
            gameTL = TechConstants.getSimpleLevel(client.getGame().getOptions()
                    .stringOption("techlevel"));
        } else {
            gameTL = TechConstants.T_SIMPLE_UNOFFICIAL;
        }

        int maxTech;
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
            default:
                maxTech = TechConstants.T_CLAN_UNOFFICIAL;
                break;
        }

        techLevelListToIndex.clear();
        DefaultComboBoxModel<String> techModel = new DefaultComboBoxModel<>();
        int selectionIdx = 0;
        for (int tl = 0; tl <= maxTech; tl++) {
            if ((tl != TechConstants.T_IS_TW_ALL)
                    && (tl != TechConstants.T_TW_ALL)) {
                techLevelListToIndex.put(selectionIdx, tl);
                techModel.addElement(TechConstants.getLevelDisplayableName(tl));
                selectionIdx++;
            }
        }
        techModel.setSelectedItem(TechConstants.getLevelDisplayableName(0));
        listTechLevel.setModel(techModel);

        listTechLevel.setSelectedIndices(selectedIndices);
        listTechLevel.addListSelectionListener(this);
    }

    void select(boolean close) {
        Entity e = getSelectedEntity();
        if (useAlternate) { // For MML
            chosenEntity = e;
        } else if (null != e) {
            Client c = null;
            if (comboPlayer.getSelectedIndex() > 0) {
                String name = (String) comboPlayer.getSelectedItem();
                c = clientGUI.getBots().get(name);
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
        RowFilter<MechTableModel, Integer> unitTypeFilter;

        List<Integer> tlLvls = new ArrayList<>();
        for (Integer selectedIdx : listTechLevel.getSelectedIndices()) {
            tlLvls.add(techLevelListToIndex.get(selectedIdx));
        }
        final Integer[] nTypes = new Integer[tlLvls.size()];
        tlLvls.toArray(nTypes);
        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        final boolean checkSupportVee = Messages.getString("MechSelectorDialog.SupportVee")
                .equals(comboUnitType.getSelectedItem());
        final boolean cannonOnly = (null != client)
                && client.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_CANON_ONLY);
        //If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<MechTableModel,Integer>() {
                @Override
                public boolean include(Entry<? extends MechTableModel, ? extends Integer> entry) {
                    MechTableModel mechModel = entry.getModel();
                    MechSummary mech = mechModel.getMechSummary(entry
                            .getIdentifier());
                    int year = (null != client) ? client.getGame().getOptions()
                            .intOption(OptionsConstants.ALLOWED_YEAR) : 999999;
                    boolean techLevelMatch = false;
                    int type = mech.getType();
                    if (client != null && client.getGame() != null
                            && client.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ERA_BASED)) {
                        type = mech.getType(year);
                    }
                    for (int tl : nTypes) {
                        if (type == tl) {
                            techLevelMatch = true;
                            break;
                        }
                    }
                    if (/* Weight */
                            ((nClass == EntityWeightClass.SIZE) || (mech.getWeightClass() == nClass)) &&
                            /*Canon*/
                            (!cannonOnly || mech.isCanon() || useAlternate) &&
                            /*Technology Level*/
                            (techLevelMatch)
                            && ((nUnit == -1) 
                                    || (!checkSupportVee && mech.getUnitType().equals(UnitType.getTypeName(nUnit)))
                                    || (checkSupportVee && mech.isSupport()))
                            /*Advanced Search*/
                            && ((searchFilter == null) || MechSearchFilter.isMatch(mech, searchFilter))
                            && !(mech.getYear() > year)) {
                        if (textFilter.getText().length() > 0) {
                            String text = textFilter.getText();
                            return mech.getName().toLowerCase().contains(text.toLowerCase());
                    }
                    return true;
                }
                return false;
                }
            };
        } catch (PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(unitTypeFilter);
    }

    private void updatePlayerChoice() {
        String lastChoice = (String) comboPlayer.getSelectedItem();
        String clientName = clientGUI.getClient().getName();
        comboPlayer.removeAllItems();
        comboPlayer.setEnabled(true);
        comboPlayer.addItem(clientName);
        for (Client client : clientGUI.getBots().values()) {
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
            panelMechView.reset();
            labelImage.setIcon(null);
            return;
        }

        MechView mechView = null;
        TROView troView = null;
        try {
            mechView = new MechView(selectedUnit, false);
            troView = TROView.createView(selectedUnit, true);
        } catch (Exception e) {
            logger.error(getClass(), "refreshUnitView", e);
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }
        if (populateTextFields) {
            panelMechView.setMech(selectedUnit, mechView);
            panelTROView.setMech(selectedUnit, troView);
        } else {
            panelMechView.reset();
            panelTROView.reset();
        }

        if (clientGUI != null) {
            clientGUI.loadPreviewImage(labelImage, selectedUnit, client.getLocalPlayer());
        }
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
            return new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (EntityLoadingException e) {
            logger.error(getClass(), "getSelectedEntity",
                    "Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName()
                            + ": " + e.getMessage(), e);
            return null;
        }
    }

    public MechSummary getChosenMechSummary() {
        int view = tableUnits.getSelectedRow();
        if (view < 0) {
            // selection got filtered away
            return null;
        }
        int selected = tableUnits.convertRowIndexToModel(view);
        // else
        return mechs[selected];
    }

    private void autoSetSkillsAndName(Entity e) {
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
            if (cs.useAverageSkills()) {
                int[] skills = client.getRandomSkillsGenerator().getRandomSkills(e, true);
    
                int gunnery = skills[0];
                int piloting = skills[1];
    
                e.getCrew().setGunnery(gunnery, i);
                // For infantry, piloting doubles as anti-mek skill, and this is
                // set based on whether the unit has anti-mek training, which gets
                // set in the BLK file, so we should ignore the defaults
                if (!(e instanceof Infantry)) {
                    e.getCrew().setPiloting(piloting, i);
                }

                if (e.getCrew() instanceof LAMPilot) {
                    skills = client.getRandomSkillsGenerator().getRandomSkills(e, true);
                    ((LAMPilot)e.getCrew()).setGunneryAero(skills[0]);
                    ((LAMPilot)e.getCrew()).setPilotingAero(skills[1]);
                }
            }
            if (cs.generateNames()) {
                boolean isFemale = client.getRandomNameGenerator().isFemale();
                e.getCrew().setGender(isFemale, i);
                e.getCrew().setName(client.getRandomNameGenerator().generate(isFemale), i);
            }
        }
        e.getCrew().sortRandomSkills();
    }

     public void run() {
         // Loading mechs can take a while, so it will have its own thread.
         // This prevents the UI from freezing, and allows the
         // "Please wait..." dialog to behave properly on various Java VMs.
         MechSummaryCache mscInstance = MechSummaryCache.getInstance();
         mechs = mscInstance.getAllMechs();

         // break out if there are no units to filter
         if (mechs == null) {
             logger.error(getClass(), "run", "No units to filter!");
         } else {
             unitModel.setData(mechs);
         }
         filterUnits();

         //initialize with the units sorted alphabetically by chassis
         List<SortKey> sortList = new ArrayList<>();
         sortList.add(new SortKey(MechTableModel.COL_CHASSIS,SortOrder.ASCENDING));
         tableUnits.getRowSorter().setSortKeys(sortList);
         ((DefaultRowSorter<?, ?>) tableUnits.getRowSorter()).sort();

         tableUnits.invalidate(); // force re-layout of window
         pack();

         unitLoadingDialog.setVisible(false);

         // In some cases, it's possible to get here without an initialized
         // instance (loading a saved game without a cache).  In these cases,
         // we dn't care about the failed loads.
         if (mscInstance.isInitialized() && !useAlternate) {
             final Map<String, String> hFailedFiles = MechSummaryCache.getInstance().getFailedFiles();
             if ((hFailedFiles != null) && (hFailedFiles.size() > 0)) {
                 // self-showing dialog
                 new UnitFailureDialog(frame, hFailedFiles);
             }
         }
         GUIPreferences guiPreferences = GUIPreferences.getInstance();
         setSize(guiPreferences.getMechSelectorSizeWidth(), guiPreferences.getMechSelectorSizeHeight());
     }

     @Override
     public void setVisible(boolean visible) {
         updateTypeCombo();

         if (visible) {
             GUIPreferences guiPreferences = GUIPreferences.getInstance();
             comboUnitType.setSelectedIndex(guiPreferences.getMechSelectorUnitType());
             comboWeight.setSelectedIndex(guiPreferences.getMechSelectorWeightClass());
             String option = guiPreferences.getMechSelectorRulesLevels().replaceAll("\\[", "");
             option = option.replaceAll("]", "");
             if (option.length() > 0) {
                 String[] strSelections = option.split("[,]");
                 int[] intSelections = new int[strSelections.length];
                 for (int i = 0; i < strSelections.length; i++) {
                     intSelections[i] = Integer.parseInt(strSelections[i].trim());
                 }
                 listTechLevel.setSelectedIndices(intSelections);
             }
         }
         asd.clearValues();
         searchFilter = null;
         buttonResetSearch.setEnabled(false);
         if (!useAlternate) {
             updatePlayerChoice();
         }
         filterUnits();
         super.setVisible(visible);
     }

     @Override
    protected void processWindowEvent(WindowEvent e) {
         super.processWindowEvent(e);
         if (e.getID() == WindowEvent.WINDOW_DEACTIVATED) {
             GUIPreferences guiPreferences = GUIPreferences.getInstance();
             guiPreferences.setMechSelectorUnitType(comboUnitType.getSelectedIndex());
             guiPreferences.setMechSelectorWeightClass(comboWeight.getSelectedIndex());
             guiPreferences.setMechSelectorRulesLevels(Arrays.toString(listTechLevel.getSelectedIndices()));
             guiPreferences.setMechSelectorSizeHeight(getSize().height);
             guiPreferences.setMechSelectorSizeWidth(getSize().width);
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

        // fill table with values
        public void setData(MechSummary[] ms) {
            data = ms;
            fireTableDataChanged();
        }

        public Object getValueAt(int row, int col) {
            if (data.length <= row) {
                return "?";
            }
            GameOptions opts = null;
            if (client != null) {
                opts = client.getGame().getOptions();
            }
            MechSummary ms = data[row];
            if (col == COL_MODEL) {
                return ms.getModel();
            } else if (col == COL_CHASSIS) {
                return ms.getChassis();
            } else if (col == COL_WEIGHT) {
                if ((opts != null) && ms.getUnitType().equals("BattleArmor")) {
                    if (opts.booleanOption(OptionsConstants.ADVANCED_TACOPS_BA_WEIGHT)) {
                        return ms.getTOweight();
                    } else {
                        return ms.getTWweight();
                    }
                }
                return ms.getTons();
            } else if (col == COL_BV) {
                if ((opts != null)
                        && opts.booleanOption(OptionsConstants.ADVANCED_GEOMETRIC_MEAN_BV)) {
                    if (opts.booleanOption(OptionsConstants.ADVANCED_REDUCED_OVERHEAT_MODIFIER_BV)) {
                        return ms.getRHGMBV();
                    } else {
                        return ms.getGMBV();
                    }
                } else {
                    if ((opts != null)
                            && opts.booleanOption(OptionsConstants.ADVANCED_REDUCED_OVERHEAT_MODIFIER_BV)) {
                        return ms.getRHBV();
                    } else {
                        return ms.getBV();
                    }
                }
            } else if (col == COL_YEAR) {
                return ms.getYear();
            } else if (col == COL_COST) {
                return ms.getCost();
            } else if (col == COL_LEVEL) {
                if ((client != null) && (client.getGame() != null)
                        && client.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ERA_BASED)) {
                    return ms.getLevel(client.getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR));
                }
                return ms.getLevel();
            } else {
                return "?";
            }
        }
    }

    public void keyReleased(KeyEvent ke) {
    }

    public void keyPressed(KeyEvent ke) {
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
            searchFilter=null;
            buttonResetSearch.setEnabled(false);
            filterUnits();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting() && evt.getSource().equals(listTechLevel)) {
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

    /**
     * @return the chosenEntity
     */
    public Entity getChosenEntity() {
        return chosenEntity;
    }

    private void close() {
        setVisible(false);
    }
 }
