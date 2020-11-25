/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import static megamek.client.ui.swing.util.UIUtil.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.*;

import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.client.ui.swing.dialog.imageChooser.CamoChooser;
import megamek.client.ui.swing.lobby.*;
import megamek.client.ui.swing.util.*;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.event.*;
import megamek.common.options.*;
import megamek.common.preference.*;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;

public class ChatLounge extends AbstractPhaseDisplay implements  
        ListSelectionListener, MouseListener, IMapSettingsObserver, IPreferenceChangeListener {
    private static final long serialVersionUID = 1454736776730903786L;

    // UI display control values
    private static final int MEKTABLE_ROWHEIGHT_COMPACT = 20;
    private static final int MEKTABLE_ROWHEIGHT_FULL = 65;
    private static final int PLAYERTABLE_ROWHEIGHT = 60;
    
    private JTabbedPane panTabs;
    private JPanel panMain = new JPanel();
    private JPanel panMap = new JPanel();

    // Main panel top bar
    private JButton butOptions = new JButton(Messages.getString("ChatLounge.butOptions"));
    private JLabel lblMapSummary = new JLabel("");
    private JLabel lblGameYear = new JLabel("");
    private JLabel lblTechLevel = new JLabel("");

    /* Unit Configuration Panel */
    private JPanel panUnitInfo = new JPanel();
    private JButton butAdd = new JButton(Messages.getString("ChatLounge.butLoad"));
    private JButton butArmy = new JButton(Messages.getString("ChatLounge.butArmy"));
    private JButton butSkills = new JButton(Messages.getString("ChatLounge.butSkills"));
    private JButton butNames = new JButton(Messages.getString("ChatLounge.butNames"));
    private JButton butLoadList = new JButton(Messages.getString("ChatLounge.butLoadList"));
    private JButton butSaveList = new JButton(Messages.getString("ChatLounge.butSaveList"));
    private JToggleButton butShowUnitID = new JToggleButton(Messages.getString("ChatLounge.butShowUnitID"));

    /* Unit Table */
    private JTable mekTable;
    private JScrollPane scrMekTable;
    private JToggleButton butCompact = new JToggleButton(Messages.getString("ChatLounge.butCompact"));
    private MekTableModel mekModel;

    /* Player Configuration Panel */
    private JPanel panPlayerInfo;
    private JComboBox<String> comboTeam = new JComboBox<String>();
    private JButton butCamo;
    private JButton butAddBot;
    private JButton butRemoveBot;
    private JButton butChangeStart;
    private JTable tablePlayers;
    private JScrollPane scrPlayers;
    private PlayerTableModel playerModel;

    /* Map Settings Panel */
    private MapSettings mapSettings;
    private JButton butConditions;
    private JPanel panGroundMap;
    private JPanel panSpaceMap;
    private JComboBox<String> comboMapType;
    @SuppressWarnings("rawtypes")
    private JComboBox<Comparable> comboMapSizes;
    private JButton butMapSize;
    private JButton butRandomMap;
    private JButton buttonBoardPreview;
    private JScrollPane scrMapButtons;
    private JPanel panMapButtons;
    private JLabel lblBoardsSelected;
    private JList<String> lisBoardsSelected;
    private JScrollPane scrBoardsSelected;
    private JButton butChange;
    private JLabel lblBoardsAvailable;
    private JList<String> lisBoardsAvailable;
    private JScrollPane scrBoardsAvailable;
    private JCheckBox chkRotateBoard;
    private JCheckBox chkIncludeGround;
    private JCheckBox chkIncludeSpace;
    private JButton butSpaceSize;
    private Set<BoardDimensions> mapSizes = new TreeSet<>();
    boolean resetAvailBoardSelection = false;
    boolean resetSelectedBoards = true;
    private JPanel mapPreviewPanel;
    private MiniMap miniMap = null;
    private ClientDialog boardPreviewW;
    private Game boardPreviewGame = new Game();
    private String cmdSelectedTab = null;
    
    private MekTableSorter activeSorter;
    private ArrayList<MekTableSorter> unitSorters = new ArrayList<>();
    private ArrayList<MekTableSorter> bvSorters = new ArrayList<>();

    private MechSummaryCache.Listener mechSummaryCacheListener = new MechSummaryCache.Listener() {
        @Override
        public void doneLoading() {
            butAdd.setEnabled(true);
            butArmy.setEnabled(true);
            butLoadList.setEnabled(true);
        }
    };

    private CamoChooser camoDialog;

    //region Action Commands
    private static final String NAME_COMMAND = "NAME";
    private static final String CALLSIGN_COMMAND = "CALLSIGN";
    //endregion Action Commands

    /**
     * Creates a new chat lounge for the clientgui.getClient().
     */
    public ChatLounge(ClientGUI clientgui) {
        super(clientgui, SkinSpecification.UIComponents.ChatLounge.getComp(),
                SkinSpecification.UIComponents.ChatLoungeDoneButton.getComp());

        setLayout(new BorderLayout());
        panTabs = new JTabbedPane();
        panTabs.add("Select Units", panMain); 
        panTabs.add("Select Map", panMap); 
        add(panTabs, BorderLayout.CENTER);

        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        lblGameYear.setToolTipText(Messages.getString("ChatLounge.GameYearLabelToolTip")); 
        lblTechLevel.setToolTipText(Messages.getString("ChatLounge.TechLevelLabelToolTip")); 

        butDone.setText(Messages.getString("ChatLounge.butDone")); 
        Font font = new Font("sansserif", Font.BOLD, 12); 
        butDone.setFont(font);

        setupSorters();
        setupPlayerInfo();
        refreshGameSettings();
        setupEntities();
        setupUnitConfiguration();
//        refreshEntities();
        setupMainPanel();

        refreshMapSummaryLabel();
        refreshGameYearLabel();
        refreshTechLevelLabel();
        
        adaptToGUIScale();

        GUIPreferences.getInstance().addPreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
        tablePlayers.getSelectionModel().addListSelectionListener(this);
        lisBoardsAvailable.addListSelectionListener(this);
        
        butOptions.addActionListener(lobbyListener);
        butCompact.addActionListener(lobbyListener);
    }
    
    /** Initializes the Mek Table sorters. */
    private void setupSorters() {
        unitSorters.add(new PlayerTransportIDSorter(clientgui));
        unitSorters.add(new IDSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new IDSorter(MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new NameSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new NameSorter(MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new TypeSorter());
        unitSorters.add(new PlayerTonnageSorter(clientgui, MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new PlayerTonnageSorter(clientgui, MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new TonnageSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new TonnageSorter(MekTableSorter.Sorting.DESCENDING));
        bvSorters.add(new PlayerBVSorter(clientgui, MekTableSorter.Sorting.ASCENDING));
        bvSorters.add(new PlayerBVSorter(clientgui, MekTableSorter.Sorting.DESCENDING));
        bvSorters.add(new BVSorter(MekTableSorter.Sorting.ASCENDING));
        bvSorters.add(new BVSorter(MekTableSorter.Sorting.DESCENDING));
        activeSorter = unitSorters.get(0);
    }

    /**
     * Sets up the entities table
     */
    private void setupEntities() {
        
        mekModel = new MekTableModel(clientgui, this);
        mekTable = new JTable(mekModel);
        setTableRowHeights();
        mekTable.setIntercellSpacing(new Dimension(0, 0));
        mekTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        for (int i = 0; i < MekTableModel.N_COL; i++) {
            TableColumn column = mekTable.getColumnModel().getColumn(i);
            column.setCellRenderer(mekModel.getRenderer());
            setColumnWidth(column);
        }
        mekTable.addMouseListener(new MekTableMouseAdapter());
        mekTable.addKeyListener(new MekTableKeyAdapter());
        scrMekTable = new JScrollPane(mekTable);
        scrMekTable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mekTable.getTableHeader().addMouseListener(mekTableHeaderMouseListener);
    }

    /**
     * Sets up the unit configuration panel
     */
    private void setupUnitConfiguration() {

        // Initialize the RandomNameGenerator and RandomCallsignGenerator
        RandomNameGenerator.getInstance();
        RandomCallsignGenerator.getInstance();

        MechSummaryCache mechSummaryCache = MechSummaryCache.getInstance();
        mechSummaryCache.addListener(mechSummaryCacheListener);
        boolean mscLoaded = mechSummaryCache.isInitialized();

        butLoadList.setActionCommand("load_list"); 
        butLoadList.addActionListener(lobbyListener);
        butLoadList.setEnabled(mscLoaded);
        
        butSaveList.setActionCommand("save_list"); 
        butSaveList.addActionListener(lobbyListener);
        butSaveList.setEnabled(false);
        
        butAdd.setEnabled(mscLoaded);
        butAdd.setActionCommand("load_mech"); 
        butAdd.addActionListener(lobbyListener);
        
        butArmy.setEnabled(mscLoaded);
        butArmy.addActionListener(lobbyListener);

        butSkills.addActionListener(lobbyListener);
        
        butNames.addActionListener(lobbyListener);

        panUnitInfo.setBorder(BorderFactory.createTitledBorder("Unit Setup"));

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panUnitInfo.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = 2;
        panUnitInfo.add(butAdd, c);
        
        c.gridy = 1;
        c.gridwidth = 1;
        panUnitInfo.add(butArmy, c);

        c.gridx = 1;
        panUnitInfo.add(butLoadList, c);

        c.gridx = 0;
        c.gridy = 2;
        panUnitInfo.add(butSkills, c);

        c.gridx = 1;
        panUnitInfo.add(butSaveList, c);

        c.gridx = 0;
        c.gridy = 3;
        panUnitInfo.add(butNames, c);
        
        c.gridx = 1;
        panUnitInfo.add(butShowUnitID, c);
    }

    /**
     * Sets up the player info (team, camo) panel
     */
    private void setupPlayerInfo() {

        playerModel = new PlayerTableModel();
        tablePlayers = new JTable(playerModel) {
            private static final long serialVersionUID = 6252953920509362407L;

            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                IPlayer player = playerModel.getPlayerAt(rowIndex);
                if (player == null) {
                    return null;
                }
                int mines = player.getNbrMFConventional() + player.getNbrMFActive() + player.getNbrMFInferno()
                        + player.getNbrMFVibra();
                
                StringBuilder result = new StringBuilder("<HTML>");
                result.append(guiScaledFontHTML(PlayerColors.getColor(player.getColorIndex())));
                result.append(player.getName() + "</FONT>");
                
                result.append(guiScaledFontHTML());
                if (clientgui.getBots().containsKey(player.getName()) ||
                        ((clientgui.getClient() instanceof BotClient) 
                                && (player.equals(clientgui.getClient().getLocalPlayer())))) {
                    result.append(" (Your Bot)");
                } else if (clientgui.getClient().getLocalPlayer().equals(player)) {
                    result.append(" (You)");
                }
                result.append("<BR>");
                result.append(Messages.getString("ChatLounge.tipPlayer", 
                        "", player.getConstantInitBonus(), mines));
                return result.toString();
            }
        };
        tablePlayers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePlayers.addMouseListener(new PlayerTableMouseAdapter());
        for (int i = 0; i < PlayerTableModel.N_COL; i++) {
            TableColumn column = tablePlayers.getColumnModel().getColumn(i);
            column.setCellRenderer(new PlayerRenderer());
//            setColumnWidth(column);
//            column.addPropertyChangeListener(mekTableColumnListener);
        }

        scrPlayers = new JScrollPane(tablePlayers);
        scrPlayers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panPlayerInfo = new JPanel();
        panPlayerInfo.setBorder(BorderFactory.createTitledBorder("Player Setup"));

        butAddBot = new JButton(Messages.getString("ChatLounge.butAddBot")); 
        butAddBot.setActionCommand("add_bot"); 
        butAddBot.addActionListener(lobbyListener);

        butRemoveBot = new JButton(Messages.getString("ChatLounge.butRemoveBot")); 
        butRemoveBot.setEnabled(false);
        butRemoveBot.setActionCommand("remove_bot"); 
        butRemoveBot.addActionListener(lobbyListener);
        
        butShowUnitID.setSelected(PreferenceManager.getClientPreferences().getShowUnitId());
        butShowUnitID.addActionListener(lobbyListener);

        setupTeams();

        butCamo = new JButton();
        butCamo.setPreferredSize(new Dimension(84, 72));
        butCamo.setActionCommand("camo");
        butCamo.addActionListener(e -> {
            // Show the CamoChooser for the selected player
            IPlayer player = getSelectedPlayer().getLocalPlayer();
            int result = camoDialog.showDialog(player);

            // If the dialog was canceled or nothing selected, do nothing
            if ((result == JOptionPane.CANCEL_OPTION) || (camoDialog.getSelectedItem() == null)) {
                return;
            }

            // Update the player from the camo selection
            AbstractIcon selectedItem = camoDialog.getSelectedItem();
            if (Camouflage.NO_CAMOUFLAGE.equals(selectedItem.getCategory())) {
                player.setColorIndex(camoDialog.getSelectedIndex());
            }
            player.setCamoCategory(selectedItem.getCategory());
            player.setCamoFileName(selectedItem.getFilename());
            butCamo.setIcon(player.getCamouflage().getImageIcon());
            getSelectedPlayer().sendPlayerInfo();
        });
        camoDialog = new CamoChooser(clientgui.getFrame());
        refreshCamos();

        butChangeStart = new JButton(Messages.getString("ChatLounge.butChangeStart")); 
        butChangeStart.addActionListener(lobbyListener);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panPlayerInfo.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        panPlayerInfo.add(comboTeam, c);

        c.gridx = 0;
        c.gridy = 1;
        panPlayerInfo.add(butChangeStart, c);

        c.gridx = 0;
        c.gridy = 2;
        panPlayerInfo.add(butAddBot, c);

        c.gridx = 0;
        c.gridy = 3;
        panPlayerInfo.add(butRemoveBot, c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.weighty = 1.0;
        panPlayerInfo.add(butCamo, c);

        refreshPlayerInfo();
    }

    private void setupMainPanel() {
        setupMap();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMain.setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(5, 1, 5, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        gridbag.setConstraints(butOptions, c);
        panMain.add(butOptions);

        JPanel panel1 = new JPanel(new GridBagLayout());
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.WEST;
        panel1.add(lblMapSummary, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 20);
        panel1.add(lblGameYear, c);
        c.gridx = 2;
        panel1.add(lblTechLevel, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHEAST;
        panel1.add(butCompact, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        panMain.add(panel1, c);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridheight = 3;
        panMain.add(scrMekTable, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.1;
        c.weighty = 0.0;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panMain.add(panUnitInfo, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 2;
        c.weighty = 0.0;
        panMain.add(panPlayerInfo, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1.0;
        panMain.add(scrPlayers, c);

    }

    private void setupMap() {

        mapSettings = MapSettings.getInstance(clientgui.getClient().getMapSettings());

        butConditions = new JButton(Messages.getString("ChatLounge.butConditions")); 
        butConditions.addActionListener(lobbyListener);

        butRandomMap = new JButton(Messages.getString("BoardSelectionDialog.GeneratedMapSettings")); 
        butRandomMap.addActionListener(lobbyListener);

        chkIncludeGround = new JCheckBox(Messages.getString("ChatLounge.IncludeGround")); 
        chkIncludeGround.addActionListener(lobbyListener);

        chkIncludeSpace = new JCheckBox(Messages.getString("ChatLounge.IncludeSpace")); 
        chkIncludeSpace.addActionListener(lobbyListener);

        setupGroundMap();
        setupSpaceMap();
        refreshSpaceGround();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMap.setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 1, 5, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(butConditions, c);
        panMap.add(butConditions);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(butRandomMap, c);
        panMap.add(butRandomMap);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;
        c.weighty = 0.75;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(panGroundMap, c);
        panMap.add(panGroundMap);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 0.25;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        gridbag.setConstraints(panSpaceMap, c);
        panMap.add(panSpaceMap);

    }

    /**
     * Sets up the ground map selection panel
     */
    @SuppressWarnings("rawtypes")
    private void setupGroundMap() {

        panGroundMap = new JPanel();
        panGroundMap.setBorder(BorderFactory.createTitledBorder("Planetary Map"));

        panMapButtons = new JPanel();

        comboMapType = new JComboBox<String>();
        setupMapChoice();

        butMapSize = new JButton(Messages.getString("ChatLounge.MapSize")); 
        butMapSize.addActionListener(lobbyListener);

        comboMapSizes = new JComboBox<Comparable>();
        setupMapSizes();

        buttonBoardPreview = new JButton(Messages.getString("BoardSelectionDialog.ViewGameBoard")); 
        buttonBoardPreview.addActionListener(lobbyListener);
        buttonBoardPreview.setToolTipText(Messages.getString("BoardSelectionDialog.ViewGameBoardTooltip"));

        butChange = new JButton("<<");
        butChange.addActionListener(lobbyListener);

        lblBoardsSelected = new JLabel(Messages.getString("BoardSelectionDialog.MapsSelected"), SwingConstants.CENTER); 
        lblBoardsAvailable = new JLabel(Messages.getString("BoardSelectionDialog.mapsAvailable"), 
                SwingConstants.CENTER);

        lisBoardsSelected = new JList<String>(new DefaultListModel<String>());
        lisBoardsSelected.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lisBoardsAvailable = new JList<String>(new DefaultListModel<String>());
        refreshBoardsSelected();
        refreshBoardsAvailable();
        lisBoardsAvailable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lisBoardsAvailable.addMouseListener(this);
        

        chkRotateBoard = new JCheckBox(Messages.getString("BoardSelectionDialog.RotateBoard")); 
        chkRotateBoard.addActionListener(lobbyListener);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panGroundMap.setLayout(gridbag);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(chkIncludeGround, c);
        panGroundMap.add(chkIncludeGround);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(comboMapType, c);
        panGroundMap.add(comboMapType);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(comboMapSizes, c);
        panGroundMap.add(comboMapSizes);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(butMapSize, c);
        panGroundMap.add(butMapSize);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(buttonBoardPreview, c);
        panGroundMap.add(buttonBoardPreview);

        scrMapButtons = new JScrollPane(panMapButtons);
        refreshMapButtons();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(scrMapButtons, c);
        panGroundMap.add(scrMapButtons);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(lblBoardsSelected, c);
        panGroundMap.add(lblBoardsSelected);

        scrBoardsSelected = new JScrollPane(lisBoardsSelected);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(scrBoardsSelected, c);
        panGroundMap.add(scrBoardsSelected);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(butChange, c);
        panGroundMap.add(butChange);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(lblBoardsAvailable, c);
        panGroundMap.add(lblBoardsAvailable);

        scrBoardsAvailable = new JScrollPane(lisBoardsAvailable);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(scrBoardsAvailable, c);
        panGroundMap.add(scrBoardsAvailable);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 4;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(chkRotateBoard, c);
        panGroundMap.add(chkRotateBoard);

        mapPreviewPanel = new JPanel();

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 4;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(mapPreviewPanel, c);
        panGroundMap.add(mapPreviewPanel);

        try {
            miniMap = new MiniMap(mapPreviewPanel, null);
            // Set a default size for the minimap object to ensure it will have
            // space on the screen to be drawn.
            miniMap.setSize(160, 200);
            miniMap.setZoom(2);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, Messages.getString("BoardEditor.CouldNotInitialiseMinimap") + e,
                    Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); 
        }
        mapPreviewPanel.add(miniMap);

        // setup the board preview window.
        boardPreviewW = new ClientDialog(clientgui.frame, 
                Messages.getString("BoardSelectionDialog.ViewGameBoard"), 
                false);
        boardPreviewW.setLocationRelativeTo(clientgui.frame);
        boardPreviewW.setVisible(false);

        try {
            BoardView1 bv = new BoardView1(boardPreviewGame, null, null);
            bv.setDisplayInvalidHexInfo(false);
            bv.setUseLOSTool(false);
            boardPreviewW.add(bv.getComponent(true));
            boardPreviewW.setSize(clientgui.frame.getWidth()/2, clientgui.frame.getHeight()/2);
            bv.zoomOut();
            bv.zoomOut();
            bv.zoomOut();
            bv.zoomOut();
            boardPreviewW.center();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                            Messages.getString("BoardEditor.CouldntInitialize") + e,
                            Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); 
        }

    }

    private void setupSpaceMap() {

        panSpaceMap = new JPanel();
        panSpaceMap.setBorder(BorderFactory.createTitledBorder("Space Map"));

        butSpaceSize = new JButton(Messages.getString("ChatLounge.MapSize"));
        butSpaceSize.addActionListener(lobbyListener);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panSpaceMap.setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panSpaceMap.add(chkIncludeSpace, c);

        c.weighty = 1.0;
        c.gridy = 1;
        panSpaceMap.add(butSpaceSize, c);
    }

    /**
     * Set up the map chooser panel
     */
    private void setupMapChoice() {
        comboMapType.addItem(MapSettings.getMediumName(MapSettings.MEDIUM_GROUND));
        comboMapType.addItem(MapSettings.getMediumName(MapSettings.MEDIUM_ATMOSPHERE));
        comboMapType.addActionListener(lobbyListener);
        refreshMapChoice();
    }

    private void setupMapSizes() {
        int oldSelection = comboMapSizes.getSelectedIndex();
        mapSizes = clientgui.getClient().getAvailableMapSizes();
        comboMapSizes.removeActionListener(lobbyListener);
        comboMapSizes.removeAllItems();
        for (BoardDimensions size : mapSizes) {
            comboMapSizes.addItem(size);
        }
        comboMapSizes.addItem(Messages.getString("ChatLounge.CustomMapSize"));
        comboMapSizes.setSelectedIndex(oldSelection != -1 ? oldSelection : 0);
        comboMapSizes.addActionListener(lobbyListener);
    }

    private void refreshMapChoice() {
        comboMapType.removeActionListener(lobbyListener);
        if (mapSettings.getMedium() < MapSettings.MEDIUM_SPACE) {
            comboMapType.setSelectedIndex(mapSettings.getMedium());
        }
        comboMapType.addActionListener(lobbyListener);
    }

    private void refreshSpaceGround() {
        chkIncludeGround.removeActionListener(lobbyListener);
        chkIncludeSpace.removeActionListener(lobbyListener);
        boolean inSpace = mapSettings.getMedium() == MapSettings.MEDIUM_SPACE;
        chkIncludeSpace.setSelected(inSpace);
        chkIncludeGround.setSelected(!inSpace);
        comboMapType.setEnabled(!inSpace);
        butMapSize.setEnabled(!inSpace);
        comboMapSizes.setEnabled(!inSpace);
        buttonBoardPreview.setEnabled(!inSpace);
        lisBoardsSelected.setEnabled(!inSpace);
        butChange.setEnabled(!inSpace);
        lisBoardsAvailable.setEnabled(!inSpace);
        chkRotateBoard.setEnabled(!inSpace);
        butSpaceSize.setEnabled(inSpace);
        chkIncludeGround.addActionListener(lobbyListener);
        chkIncludeSpace.addActionListener(lobbyListener);
    }

    private void refreshBoardsAvailable() {
        int selectedRow = lisBoardsAvailable.getSelectedIndex();
        ((DefaultListModel<String>) lisBoardsAvailable.getModel()).removeAllElements();
        for (String s : mapSettings.getBoardsAvailableVector()) {
            ((DefaultListModel<String>) lisBoardsAvailable.getModel()).addElement(s);
        }
        if (resetAvailBoardSelection) {
            lisBoardsAvailable.setSelectedIndex(0);
            resetAvailBoardSelection = false;
        } else {
            lisBoardsAvailable.setSelectedIndex(selectedRow);
        }
    }

    private void refreshBoardsSelected() {
        int selectedRow = lisBoardsSelected.getSelectedIndex();
        ((DefaultListModel<String>) lisBoardsSelected.getModel()).removeAllElements();
        int index = 0;
        for (Iterator<String> i = mapSettings.getBoardsSelected(); i.hasNext();) {
            ((DefaultListModel<String>) lisBoardsSelected.getModel()).addElement(index++ + ": " + i.next()); 
        }
        lisBoardsSelected.setSelectedIndex(selectedRow);
        if (resetSelectedBoards) {
            lisBoardsSelected.setSelectedIndex(0);
            resetSelectedBoards = false;
        } else {
            lisBoardsSelected.setSelectedIndex(selectedRow);
        }
    }

    /**
     * Fills the Map Buttons scroll pane with the appropriate amount of buttons
     * in the appropriate layout
     */
    private void refreshMapButtons() {
        panMapButtons.removeAll();

        panMapButtons.setLayout(new GridLayout(mapSettings.getMapHeight(), mapSettings.getMapWidth()));

        for (int i = 0; i < mapSettings.getMapHeight(); i++) {
            for (int j = 0; j < mapSettings.getMapWidth(); j++) {
                JButton button = new JButton(Integer.toString((i * mapSettings.getMapWidth()) + j));
                Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
                button.setFont(scaledFont);
                button.addActionListener(lobbyListener);
                panMapButtons.add(button);
            }
        }

        scrMapButtons.validate();

        lblBoardsAvailable.setText(mapSettings.getBoardWidth() + "x" + mapSettings.getBoardHeight() + " "
                + Messages.getString("BoardSelectionDialog.mapsAvailable"));
        comboMapSizes.removeActionListener(lobbyListener);
        int items = comboMapSizes.getItemCount();

        boolean mapSizeSelected = false;
        for (int i = 0; i < (items - 1); i++) {
            BoardDimensions size = (BoardDimensions) comboMapSizes.getItemAt(i);

            if ((size.width() == mapSettings.getBoardWidth()) && (size.height() == mapSettings.getBoardHeight())) {
                comboMapSizes.setSelectedIndex(i);
                mapSizeSelected = true;
            }
        }
        // If we didn't select a size, select the last item: 'Custom Size'
        if (!mapSizeSelected) {
            comboMapSizes.setSelectedIndex(items - 1);
        }
        comboMapSizes.addActionListener(lobbyListener);

    }

    public void previewMapsheet() {
        String boardName = lisBoardsAvailable.getSelectedValue();
        if (lisBoardsAvailable.getSelectedIndex() > 2) {
            IBoard board = new Board(16, 17);
            board.load(new MegaMekFile(Configuration.boardsDir(), boardName + ".board").getFile());
            if (chkRotateBoard.isSelected()) {
                BoardUtilities.flip(board, true, true);
            }
            if (board.isValid())
                miniMap.setBoard(board);
        }
    }

    public void previewGameBoard() {
        MapSettings temp = mapSettings;
        temp.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
        temp.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        IBoard[] sheetBoards = new IBoard[temp.getMapWidth() * temp.getMapHeight()];
        List<Boolean> rotateBoard = new ArrayList<>();
        for (int i = 0; i < (temp.getMapWidth() * temp.getMapHeight()); i++) {
            sheetBoards[i] = new Board();
            String name = temp.getBoardsSelectedVector().get(i);
            boolean isRotated = false;
            if (name.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                // only rotate boards with an even width
                if ((temp.getBoardWidth() % 2) == 0) {
                    isRotated = true;
                }
                name = name.substring(Board.BOARD_REQUEST_ROTATION.length());
            }
            if (name.startsWith(MapSettings.BOARD_GENERATED) || (temp.getMedium() == MapSettings.MEDIUM_SPACE)) {
                sheetBoards[i] = BoardUtilities.generateRandom(temp);
            } else {
                sheetBoards[i].load(new MegaMekFile(Configuration.boardsDir(), name
                        + ".board").getFile());
                BoardUtilities.flip(sheetBoards[i], isRotated, isRotated);
            }
            rotateBoard.add(isRotated);
        }

        IBoard newBoard = BoardUtilities.combine(temp.getBoardWidth(), temp.getBoardHeight(), temp.getMapWidth(),
                temp.getMapHeight(), sheetBoards, rotateBoard, temp.getMedium());
        
        boardPreviewGame.setBoard(newBoard);
        boardPreviewW.setVisible(true);
    }

    /**
     * Refreshes the game settings with new info from the client
     */
    private void refreshGameSettings() {
        refreshTeams();
        refreshDoneButton();
    }

    /**
     * Refreshes the entities from the client
     */
    public void refreshEntities() {
        mekModel.clearData();
        boolean localUnits = false;
        System.out.println("Refresh Units! ");
        ArrayList<Entity> allEntities = new ArrayList<Entity>();
        for (Entity ent : clientgui.getClient().getEntitiesVector()) {
            allEntities.add(ent);
        }

        Collections.sort(allEntities, activeSorter);

        for (Entity entity : allEntities) {
            // Remember if the local player has units.
            if (!localUnits && entity.getOwner().equals(clientgui.getClient().getLocalPlayer())) {
                localUnits = true;
            }

            if (!clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES)) { 
                entity.getCrew().clearOptions(PilotOptions.LVL3_ADVANTAGES);
            }

            if (!clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.EDGE)) { 
                entity.getCrew().clearOptions(PilotOptions.EDGE_ADVANTAGES);
            }

            if (!clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_MANEI_DOMINI)) { 
                entity.getCrew().clearOptions(PilotOptions.MD_ADVANTAGES);
            }

            if (!clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS)) { 
                entity.clearPartialRepairs();
            }
            // Handle the "Blind Drop" option.
            if (!entity.getOwner().equals(clientgui.getClient().getLocalPlayer())
                    && clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP) 
                    && !clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)) { 

                mekModel.addUnit(entity);
            } else if (entity.getOwner().equals(clientgui.getClient().getLocalPlayer())
                    || (!clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP) 
                    && !clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP))) { 
                mekModel.addUnit(entity);
            }
        }

        // Enable the "Save Unit List..." and "Delete All"
        // buttons if the local player has units.
        butSaveList.setEnabled(localUnits);
        clientgui.getMenuBar().setUnitList(localUnits);
    }
    
    private void toggleCompact() {
        setTableRowHeights();
        mekModel.refreshCells();
    }

    public static final String DOT_SPACER = " \u2B1D ";

    /** Refreshes the player info table. */
    private void refreshPlayerInfo() {
        // Remember the selected player
        Client c = getSelectedPlayer();
        String selPlayer = "";
        if (c != null) {
            selPlayer = c.getLocalPlayer().getName();
        }
        
        // Empty and refill the player table
        playerModel.clearData();
        for (Enumeration<IPlayer> i = clientgui.getClient().getPlayers(); i.hasMoreElements();) {
            final IPlayer player = i.nextElement();
            if (player == null) {
                continue;
            }
            playerModel.addPlayer(player);
        }
        
        // re-select the previously selected player, if possible
        if (c != null && playerModel.getRowCount() > 0) {
            for (int row = 0; row < playerModel.getRowCount(); row++) {
                IPlayer p = playerModel.getPlayerAt(row);
                if (p.getName().equals(selPlayer)) {
                    tablePlayers.setRowSelectionInterval(row, row);
                    break;
                }
            }
        }
    }

    private void refreshCamos() {
        if ((tablePlayers == null) || (playerModel == null) || (tablePlayers.getSelectedRow() == -1)) {
            return;
        }
        IPlayer player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
        if (player != null) {
            butCamo.setIcon(player.getCamouflage().getImageIcon());
        }
    }

    /** Sets up the team choice box. */
    private void setupTeams() {
        for (int i = 0; i < IPlayer.MAX_TEAMS; i++) {
            comboTeam.addItem(IPlayer.teamNames[i]);
        }
        comboTeam.addActionListener(lobbyListener);
    }

    /**
     * Highlight the team the player is playing on.
     */
    private void refreshTeams() {
        comboTeam.removeActionListener(lobbyListener);
        comboTeam.setSelectedIndex(clientgui.getClient().getLocalPlayer().getTeam());
        comboTeam.addActionListener(lobbyListener);
    }

    /**
     * Refreshes the done button. The label will say the opposite of the
     * player's "done" status, indicating that clicking it will reverse the
     * condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone.setText(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone"));
         //$NON-NLS-2$
    }

    private void refreshDoneButton() {
        refreshDoneButton(clientgui.getClient().getLocalPlayer().isDone());
    }

    /**
     * Change local player team.
     */
    private void changeTeam(int team) {
        Client c = getSelectedPlayer();
        
        // If the team was not actually changed or the selected player 
        // is not editable (not the local player or local bot), do nothing
        if ((c == null) || (c.getLocalPlayer().getTeam() == team)) {
            return;
        }

        c.getLocalPlayer().setTeam(team);
        c.sendPlayerInfo();

        // WIP on getting entities to be able to be loaded by teammates
//        for (Entity unit : c.getGame().getPlayerEntities(c.getLocalPlayer(), false)) {
//            // If unit has empty bays it needs to be updated in order for
//            // other entities to be able to load into it.
//            if ((unit.getTransports().size() > 0) && (unit.getLoadedUnits().isEmpty())
//                    && (unit.getTransportId() == Entity.NONE)) {
//                c.sendUpdateEntity(unit);
//            }
//
//            // Unload this unit if its being transported.
//            if (unit.getTransportId() != Entity.NONE) {
//                unloader(unit);
//            }
//        }
//
//        // Loop through everyone elses entities and if they no longer have a
//        // legal loading, remove them.
//        // I am aware this is an odd way to do it, however I couldn't get it
//        // to work by looping through the
//        // unit.getLoadedUnits() - it always returned with an empty list
//        // even when there was loaded units.
//        for (Entity unit : c.getGame().getEntitiesVector()) {
//            if (unit.getOwner().equals(c.getLocalPlayer())) {
//                continue;
//            }
//
//            if ((unit.getTransportId() != Entity.NONE) && (c.getGame().getEntity(unit.getTransportId()).getOwner()
//                    .getTeam() != unit.getOwner().getTeam())) {
//                unloader(unit);
//            }
//        }
    }

    /**
     * Pop up the customize mech dialog
     */

    private void customizeMech() {
        if (mekTable.getSelectedRow() == -1) {
            return;
        }
        customizeMech(mekModel.getEntityAt(mekTable.getSelectedRow()));
    }

    /**
     * Load one unit into another in the chat lounge
     *
     * @param loadee
     *            - an Entity that should be loaded
     * @param loaderId
     *            - the id of the entity that will load
     */
    private void loader(Entity loadee, int loaderId, int bayNumber) {
        Client c = clientgui.getBots().get(loadee.getOwner().getName());
        if (c == null) {
            c = clientgui.getClient();
        }
        Entity loader = clientgui.getClient().getGame().getEntity(loaderId);
        if (loader == null) {
            return;
        }

        // We need to make sure our current bomb choices fit onto the new
        // fighter
        if (loader instanceof FighterSquadron) {
            FighterSquadron fSquad = (FighterSquadron) loader;
            // We can't use Aero.getBombPoints() because the bombs haven't been
            // loaded yet, only selected, so we have to count the choices
            int[] bombChoice = fSquad.getBombChoices();
            int numLoadedBombs = 0;
            for (int i = 0; i < bombChoice.length; i++) {
                numLoadedBombs += bombChoice[i];
            }
            // We can't load all of the squadrons bombs
            if (numLoadedBombs > ((IBomber)loadee).getMaxBombPoints()) {
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("FighterSquadron.bomberror"),
                        Messages.getString("FighterSquadron.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        c.sendLoadEntity(loadee.getId(), loaderId, bayNumber);
        // TODO: it would probably be a good idea to reset deployment
        // info to equal that of the loader, and disable it in customMechDialog
        // I tried doing this but I cant quite figure out the client/server
        // interaction in CustomMechDialog.java
    }

    /** 
     * Have the given entity disembark if it is carried by another unit.
     * Entities that were modified and need an update to be sent to the server
     * are added to the given updateCandidate set. 
     */
    private void disembark(Entity entity, Set<Entity> updateCandidates) {
        Entity carrier = clientgui.getClient().getGame().getEntity(entity.getTransportId());
        if (carrier != null) {
            carrier.unload(entity);
            entity.setTransportId(Entity.NONE);
            updateCandidates.add(entity);
            updateCandidates.add(carrier);
        }
    }
    
    /** 
     * Have the given entity offload all the units it is carrying.
     * Returns a set of entities that need to be sent to the server. 
     */
    private void offloadFrom(Entity entity, Set<Entity> updateCandidates) {
        List<Entity> carriedUnits = entity.getLoadedUnits();
        for (Entity carriedEntity: carriedUnits) {
            disembark(carriedEntity, updateCandidates);
        } 
    }


    /** Change the given entity's controller to the player with ID newOwnerId. */
    private void changeEntityOwner(Entity entity, int newOwnerId) {
        if (!isEditable(entity) || entity.getOwnerId() == newOwnerId) {
            return;
        }
        
        IPlayer new_owner = clientgui.getClient().getGame().getPlayer(newOwnerId);
        // Store entities that need to be sent to the Server to avoid
        // sending them twice which leads to dropped updates
        Set<Entity> updateCandidates = new HashSet<>();
        updateCandidates.add(entity);
        
        // If the unit is switching teams, we need to offload units from it
        // and have it disembark if it is being carried
        if (entity.getOwner().getTeam() != new_owner.getTeam()) {
            offloadFrom(entity, updateCandidates);
            disembark(entity, updateCandidates);
        }
        
        entity.setOwner(new_owner);
        sendUpdate(updateCandidates);
    }
    
    /**
     * swap pilots from one entity to another
     *
     * @param swapee
     *            - an Entity that should be swapped from
     * @param swapperId
     *            - the id of the entity that should be swapped to
     */
    private void swapPilots(Entity swapee, int swapperId) {
        Client c = clientgui.getBots().get(swapee.getOwner().getName());
        if (c == null) {
            c = clientgui.getClient();
        }
        Entity swapper = clientgui.getClient().getGame().getEntity(swapperId);
        if (swapper == null) {
            return;
        }
        Crew temp = swapper.getCrew();
        swapper.setCrew(swapee.getCrew());
        swapee.setCrew(temp);
        c.sendUpdateEntity(swapee);
        c.sendUpdateEntity(swapper);
    }

    /** Sends the entities in the given set to the Server. */
    private void sendUpdate(Set<Entity> updateCandidates) {
        for (Entity entity: updateCandidates) {
            getLocalClient(entity).sendUpdateEntity(entity);
        }
    }

    /** Deletes an entity. */
    private void delete(Entity entity) {
        if (!isEditable(entity)) {
            return;
        }
        
        // Store entities that need to be sent to the Server to avoid
        // sending them twice which leads to dropped updates
        Set<Entity> updateCandidates = new HashSet<>();
        updateCandidates.add(entity);
        
        // Offload units from it and have it disembark if it is being carried
        offloadFrom(entity, updateCandidates);
        disembark(entity, updateCandidates);
        
        sendUpdate(updateCandidates);
        getLocalClient(entity).sendDeleteEntity(entity.getId());
        
        
        
        
//        Client c = clientgui.getBots().get(entity.getOwner().getName());
//        if (c == null) {
//            c = clientgui.getClient();
//        }
//        // first unload any units from this unit
//        if (entity.getLoadedUnits().size() > 0) {
//            for (Entity loaded : entity.getLoadedUnits()) {
//                entity.unload(loaded);
//                loaded.setTransportId(Entity.NONE);
//                c.sendUpdateEntity(loaded);
//            }
//            c.sendUpdateEntity(entity);
//        }
//        // unload this unit from any other units it might be loaded onto
//        if (entity.getTransportId() != Entity.NONE) {
//            Entity loader = clientgui.getClient().getGame().getEntity(entity.getTransportId());
//            if (null != loader) {
//                loader.unload(entity);
//                entity.setTransportId(Entity.NONE);
//                c.sendUpdateEntity(loader);
//                c.sendUpdateEntity(entity);
//            }
//        }
    }

    /**
     *
     * @param entities
     */
    public void customizeMechs(List<Entity> entities) {
        // Only call this for when selecting a valid list of entities
        if (entities.size() < 1) {
            return;
        }
        Set<String> owners = new HashSet<>();
        String ownerName = "";
        int ownerId = -1;
        for (Entity e : entities) {
            ownerName = e.getOwner().getName();
            ownerId = e.getOwner().getId();
            owners.add(ownerName);
        }

        // Error State
        if (owners.size() > 1) {
            return;
        }

        boolean editable = clientgui.getBots().get(ownerName) != null;
        Client client;
        if (editable) {
            client = clientgui.getBots().get(ownerName);
        } else {
            editable |= ownerId == clientgui.getClient().getLocalPlayer().getId();
            client = clientgui.getClient();
        }

        CustomMechDialog cmd = new CustomMechDialog(clientgui, client, entities, editable);
        cmd.setSize(new Dimension(GUIPreferences.getInstance().getCustomUnitWidth(),
                GUIPreferences.getInstance().getCustomUnitHeight()));
        cmd.setTitle(Messages.getString("ChatLounge.CustomizeUnits")); 
        cmd.setVisible(true);
        GUIPreferences.getInstance().setCustomUnitHeight(cmd.getSize().height);
        GUIPreferences.getInstance().setCustomUnitWidth(cmd.getSize().width);
        if (editable && cmd.isOkay()) {
            // send changes
            for (Entity entity : entities) {
                // If a LAM with mechanized BA was changed to non-mech mode, unload the BA.
                if ((entity instanceof LandAirMech)
                        && entity.getConversionMode() != LandAirMech.CONV_MODE_MECH) {
                    for (Entity loadee : entity.getLoadedUnits()) {
                        entity.unload(loadee);
                        loadee.setTransportId(Entity.NONE);
                        client.sendUpdateEntity(loadee);
                    }
                }

                client.sendUpdateEntity(entity);

                // Changing state to a transporting unit can update state of
                // transported units, so update those as well
                for (Transporter transport : entity.getTransports()) {
                    for (Entity loaded : transport.getLoadedUnits()) {
                        client.sendUpdateEntity(loaded);
                    }
                }

                // Customizations to a Squadron can effect the fighters
                if (entity instanceof FighterSquadron) {
                    entity.getSubEntities().ifPresent(ents -> ents.forEach(client::sendUpdateEntity));
                }
            }
        }
        if (cmd.isOkay() && (cmd.getStatus() != CustomMechDialog.DONE)) {
            Entity nextEnt = cmd.getNextEntity(cmd.getStatus() == CustomMechDialog.NEXT);
            customizeMech(nextEnt);
        }
    }

    public void setCMDSelectedTab(String tab) {
        cmdSelectedTab = tab;
    }

    /**
     *
     * @param entity
     */
    public void customizeMech(Entity entity) {
        boolean editable = clientgui.getBots().get(entity.getOwner().getName()) != null;
        Client c;
        if (editable) {
            c = clientgui.getBots().get(entity.getOwner().getName());
        } else {
            editable |= entity.getOwnerId() == clientgui.getClient().getLocalPlayer().getId();
            c = clientgui.getClient();
        }
        // When we customize a single entity's C3 network setting,
        // **ALL** members of the network may get changed.
        Entity c3master = entity.getC3Master();
        ArrayList<Entity> c3members = new ArrayList<Entity>();
        Iterator<Entity> playerUnits = c.getGame().getPlayerEntities(c.getLocalPlayer(), false).iterator();
        while (playerUnits.hasNext()) {
            Entity unit = playerUnits.next();
            if (!entity.equals(unit) && entity.onSameC3NetworkAs(unit)) {
                c3members.add(unit);
            }
        }

        boolean doneCustomizing = false;
        while (!doneCustomizing) {
            // display dialog
            List<Entity> entities = new ArrayList<>();
            entities.add(entity);
            CustomMechDialog cmd = new CustomMechDialog(clientgui, c, entities, editable);
            cmd.setSize(new Dimension(GUIPreferences.getInstance().getCustomUnitWidth(),
                    GUIPreferences.getInstance().getCustomUnitHeight()));
            cmd.refreshOptions();
            cmd.refreshQuirks();
            cmd.refreshPartReps();
            cmd.setTitle(entity.getShortName());
            if (cmdSelectedTab != null) {
                cmd.setSelectedTab(cmdSelectedTab);
            }
            cmd.setVisible(true);
            GUIPreferences.getInstance().setCustomUnitHeight(cmd.getSize().height);
            GUIPreferences.getInstance().setCustomUnitWidth(cmd.getSize().width);
            cmdSelectedTab = cmd.getSelectedTab();
            if (editable && cmd.isOkay()) {
                // If a LAM with mechanized BA was changed to non-mech mode, unload the BA.
                if ((entity instanceof LandAirMech)
                        && entity.getConversionMode() != LandAirMech.CONV_MODE_MECH) {
                    for (Entity loadee : entity.getLoadedUnits()) {
                        entity.unload(loadee);
                        loadee.setTransportId(Entity.NONE);
                        c.sendUpdateEntity(loadee);
                    }
                }

                // send changes
                c.sendUpdateEntity(entity);

                // Changing state to a transporting unit can update state of
                // transported units, so update those as well
                for (Transporter transport : entity.getTransports()) {
                    for (Entity loaded : transport.getLoadedUnits()) {
                        c.sendUpdateEntity(loaded);
                    }
                }

                // Customizations to a Squadron can effect the fighters
                if (entity instanceof FighterSquadron) {
                    entity.getSubEntities().ifPresent(ents -> ents.forEach(c::sendUpdateEntity));
                }

                // Do we need to update the members of our C3 network?
                if (((c3master != null) && !c3master.equals(entity.getC3Master()))
                        || ((c3master == null) && (entity.getC3Master() != null))) {
                    for (Entity unit : c3members) {
                        c.sendUpdateEntity(unit);
                    }
                }
            }
            if (cmd.isOkay() && (cmd.getStatus() != CustomMechDialog.DONE)) {
                entity = cmd.getNextEntity(cmd.getStatus() == CustomMechDialog.NEXT);
            } else {
                doneCustomizing = true;
            }
        }
    }

    /** 
     * Displays a CamoChooser to choose an individual camo for 
     * the given vector of entities. The camo will only be applied
     * to units configurable by the local player, i.e. his own units
     * or those of his bots.
     */
    public void mechCamo(Vector<Entity> entities) {
        if (entities.size() < 1) {
            return;
        }

        // Display the CamoChooser and await the result
        // The dialog is preset to the first selected unit's settings
        CamoChooser mcd = new CamoChooser(clientgui.getFrame());
        int result = mcd.showDialog(entities.get(0));

        // If the dialog was canceled or nothing was selected, do nothing
        if ((result == JOptionPane.CANCEL_OPTION) || (mcd.getSelectedItem() == null)) {
            return;
        }

        // Choosing the player camo resets the units to have no 
        // individual camo.
        AbstractIcon selectedItem = mcd.getSelectedItem();
        IPlayer owner = entities.get(0).getOwner();
        AbstractIcon ownerCamo = owner.getCamouflage();
        boolean noIndividualCamo = selectedItem.equals(ownerCamo);
        
        // Update all allowed entities with the camo
        for (Entity ent : entities) {
            if (isEditable(ent)) {
                if (noIndividualCamo) {
                    ent.setCamoCategory(null);
                    ent.setCamoFileName(null);
                } else {
                    ent.setCamoCategory(selectedItem.getCategory());
                    ent.setCamoFileName(selectedItem.getFilename());
                }
                getLocalClient(ent).sendUpdateEntity(ent);
            }
        }
    }
    
    /** 
     * Returns true when the given entity may be configured
     * by the local player, i.e. if it is his own unit or one
     * of his bot's units.
     */
    private boolean isEditable(Entity entity) {
        return clientgui.getBots().containsKey(entity.getOwner().getName())
                || (entity.getOwnerId() == clientgui.getClient().getLocalPlayer().getId());
    }
    
    /** 
     * Returns the Client associated with a given entity that may be configured
     * by the local player (his own unit or one of his bot's units).
     * For a unit that cannot be configured (owned by a remote player) the client
     * of the local player is returned.
     */
    private Client getLocalClient(Entity entity) {
        if (clientgui.getBots().containsKey(entity.getOwner().getName())) {
            return clientgui.getBots().get(entity.getOwner().getName());
        } else {
            return clientgui.getClient();
        }
    }

    public void mechEdit(Entity entity) {
        boolean editable = clientgui.getBots().get(entity.getOwner().getName()) != null;
        Client c;
        if (editable) {
            c = clientgui.getBots().get(entity.getOwner().getName());
        } else {
            editable |= entity.getOwnerId() == clientgui.getClient().getLocalPlayer().getId();
            c = clientgui.getClient();
        }

        // display dialog
        UnitEditorDialog med = new UnitEditorDialog(clientgui.getFrame(), entity);
        // med.setPlayer(c.getLocalPlayer());
        med.setVisible(true);
        c.sendUpdateEntity(entity);
        /*
         * if (editable && med.isSelect()) { // send changes
         * c.sendUpdateEntity(entity); }
         */
    }

    public void customizePlayer() {
        Client c = getSelectedPlayer();
        if (null != c) {
            PlayerSettingsDialog psd = new PlayerSettingsDialog(clientgui, c);
            psd.setVisible(true);
        }
    }

    /**
     * Pop up the view mech dialog
     */
    private void mechReadout(Entity entity) {
        final JDialog dialog = new JDialog(clientgui.frame, Messages.getString("ChatLounge.quickView"), false); 
        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_SPACE) {
                    e.consume();
                    dialog.setVisible(false);
                } else if (code == KeyEvent.VK_ENTER) {
                    e.consume();
                    dialog.setVisible(false);
                }
            }
        });
        // FIXME: this isn't working right, but is necessary for the key
        // listener to work right
        // dialog.setFocusable(true);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
            }
        });
        MechViewPanel mvp = new MechViewPanel();
        mvp.setMech(entity);
        JButton btn = new JButton(Messages.getString("Okay")); 
        btn.addActionListener(e -> dialog.setVisible(false));

        dialog.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        dialog.getContentPane().add(mvp, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        dialog.getContentPane().add(btn, c);
        dialog.setSize(mvp.getBestWidth(), mvp.getBestHeight() + 75);
        dialog.validate();
        dialog.setVisible(true);
    }

    /**
     * @param entity the entity to display the BV Calculation for
     */
    private void mechBVDisplay(Entity entity) {
        final JDialog dialog = new JDialog(clientgui.frame, "BV Calculation Display", false);
        dialog.getContentPane().setLayout(new GridBagLayout());

        final int width = 500;
        final int height = 400;
        Dimension size = new Dimension(width, height);

        JEditorPane tEditorPane = new JEditorPane();
        tEditorPane.setContentType("text/html");
        tEditorPane.setEditable(false);
        tEditorPane.setBorder(null);
        entity.calculateBattleValue();
        tEditorPane.setText(entity.getBVText());
        tEditorPane.setCaretPosition(0);

        JScrollPane tScroll = new JScrollPane(tEditorPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tScroll.setBorder(null);
        tScroll.setPreferredSize(size);
        tScroll.setMinimumSize(size);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        dialog.getContentPane().add(tScroll, gridBagConstraints);

        JButton button = new JButton(Messages.getString("Okay"));
        button.addActionListener(e -> dialog.setVisible(false));
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.weighty = 0.0;
        dialog.getContentPane().add(button, gridBagConstraints);

        dialog.setSize(new Dimension(width + 25, height + 75));
        dialog.validate();
        dialog.setVisible(true);
    }

    /**
     * Pop up the dialog to load a mech
     */
    private void loadMech() {
        clientgui.getMechSelectorDialog().updateOptionValues();
        clientgui.getMechSelectorDialog().setVisible(true);
    }

    public void loadFS(Vector<Integer> fighterIds) {
        String name = JOptionPane.showInputDialog(clientgui.frame, "Choose a squadron designation");
        if ((name == null) || (name.trim().length() == 0)) {
            name = "Flying Circus";
        }
        FighterSquadron fs = new FighterSquadron(name);
        fs.setOwner(clientgui.getClient().getGame().getEntity(fighterIds.firstElement()).getOwner());
        clientgui.getClient().sendAddSquadron(fs, fighterIds);
    }

    private void loadArmy() {
        clientgui.getRandomArmyDialog().setVisible(true);
    }

    public void loadRandomSkills() {
        clientgui.getRandomSkillDialog().showDialog(clientgui.getClient().getGame().getEntitiesVector());
    }

    public void loadRandomNames() {
        clientgui.getRandomNameDialog().showDialog(clientgui.getClient().getGame().getEntitiesVector());
    }

    /**
     * Changes all selected boards to be the specified board
     */
    private void changeMap(String board) {
        int[] selected = lisBoardsSelected.getSelectedIndices();
        for (final int newVar : selected) {
            String name = board;
            if (!MapSettings.BOARD_RANDOM.equals(name) && !MapSettings.BOARD_SURPRISE.equals(name)
                    && chkRotateBoard.isSelected()) {
                name = Board.BOARD_REQUEST_ROTATION + name;
            }

            // Validate the map
            IBoard b = new Board(16, 17);
            if (!MapSettings.BOARD_GENERATED.equals(board) && !MapSettings.BOARD_RANDOM.equals(board)
                    && !MapSettings.BOARD_SURPRISE.equals(board)) {
                b.load(new MegaMekFile(Configuration.boardsDir(), board + ".board").getFile());
                if (!b.isValid()) {
                    JOptionPane.showMessageDialog(this, "The Selected board is invalid, please select another.");
                    return;
                }
            }
            ((DefaultListModel<String>) lisBoardsSelected.getModel()).setElementAt(newVar + ": " + name, newVar); 
            mapSettings.getBoardsSelectedVector().set(newVar, name);
        }
        lisBoardsSelected.setSelectedIndices(selected);
        clientgui.getClient().sendMapSettings(mapSettings);
        if (boardPreviewW.isVisible()) {
            previewGameBoard();
        }
    }

    //
    // GameListener
    //
    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }
        System.out.println("Player Change!");
        refreshDoneButton();
        clientgui.getClient().getGame().setupTeams();
        refreshPlayerInfo();
        // Update camo info, unless the player is currently making changes
        if ((camoDialog != null) && !camoDialog.isVisible()) {
            refreshCamos();
        }
        refreshEntities();
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }
        
        if (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_LOUNGE) {
            refreshDoneButton();
            refreshGameSettings();
            refreshPlayerInfo();
            refreshTeams();
            refreshCamos();
            refreshEntities();
        }
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshPlayerInfo();
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshPlayerInfo();
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshGameSettings();
        // The table sorting may no longer be allowed (e.g. when blind drop was activated)
        if (!activeSorter.isAllowed(clientgui.getClient().getGame().getOptions())) {
            nextSorter(unitSorters);
        }
        refreshEntities();
        refreshPlayerInfo();
        setupMapSizes();
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
        // Do nothing
    }

    
    private ActionListener lobbyListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent ev) {

            // Are we ignoring events?
            if (isIgnoringEvents()) {
                return;
            }

            System.out.println("Action! " + ev.getSource());
            
            if (ev.getSource().equals(butAdd)) {
                loadMech();
                
            } else if (ev.getSource().equals(butArmy)) {
                loadArmy();
                
            } else if (ev.getSource().equals(butSkills)) {
                loadRandomSkills();
                
            } else if (ev.getSource().equals(butNames)) {
                loadRandomNames();
                
            } else if (ev.getSource().equals(mekTable)) {
                customizeMech();
                
            } else if (ev.getSource().equals(tablePlayers)) {
                customizePlayer();
                
            } else if (ev.getSource().equals(comboTeam)) {
                changeTeam(comboTeam.getSelectedIndex());
                
            } else if (ev.getSource().equals(butOptions)) {
                // Make sure the game options dialog is editable.
                if (!clientgui.getGameOptionsDialog().isEditable()) {
                    clientgui.getGameOptionsDialog().setEditable(true);
                }
                // Display the game options dialog.
                clientgui.getGameOptionsDialog().update(clientgui.getClient().getGame().getOptions());
                clientgui.getGameOptionsDialog().setVisible(true);
                
            } else if (ev.getSource().equals(butCompact)) {
                toggleCompact();
                
            } else if (ev.getSource().equals(butChangeStart)) {
                clientgui.getStartingPositionDialog().update();
                Client c = getSelectedPlayer();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBotOrPlayer"));  //$NON-NLS-2$
                    return;
                }
                clientgui.getStartingPositionDialog().setClient(c);
                clientgui.getStartingPositionDialog().setVisible(true);
                
            } else if (ev.getSource().equals(butLoadList)) {
                // Allow the player to replace their current
                // list of entities with a list from a file.
                Client c = getSelectedPlayer();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBotOrPlayer"));  //$NON-NLS-2$
                    return;
                }
                clientgui.loadListFile(c.getLocalPlayer());
                
            } else if (ev.getSource().equals(butSaveList)) {
                // Allow the player to save their current
                // list of entities to a file.
                Client c = getSelectedPlayer();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBotOrPlayer"));  //$NON-NLS-2$
                    return;
                }
                clientgui.saveListFile(c.getGame().getPlayerEntities(c.getLocalPlayer(), false),
                        c.getLocalPlayer().getName());
                
            } else if (ev.getSource().equals(butAddBot)) {
                BotConfigDialog bcd = new BotConfigDialog(clientgui.frame);
                bcd.setVisible(true);
                if (bcd.dialogAborted) {
                    return; // user didn't click 'ok', add no bot
                }
                if (clientgui.getBots().containsKey(bcd.getBotName())) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertExistsBot.title"),
                            Messages.getString("ChatLounge.AlertExistsBot.message"));  //$NON-NLS-2$
                } else {
                    BotClient c = bcd.getSelectedBot(clientgui.getClient().getHost(), clientgui.getClient().getPort());
                    c.setClientGUI(clientgui);
                    c.getGame().addGameListener(new BotGUI(c));
                    try {
                        c.connect();
                    } catch (Exception e) {
                        clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertBot.title"),
                                Messages.getString("ChatLounge.AlertBot.message"));  //$NON-NLS-2$
                    }
                    clientgui.getBots().put(bcd.getBotName(), c);
                }
            } else if (ev.getSource().equals(butRemoveBot)) {
                Client c = getSelectedPlayer();
                if ((c == null) || c.equals(clientgui.getClient())) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBo"));  //$NON-NLS-2$
                    return;
                }
                c.die();
                clientgui.getBots().remove(c.getName());
                
            } else if (ev.getSource().equals(butShowUnitID)) {
                PreferenceManager.getClientPreferences().setShowUnitId(butShowUnitID.isSelected());
                mekModel.refreshCells();
                repaint();
                
            } else if (ev.getSource() == butConditions) {
                clientgui.getPlanetaryConditionsDialog().update(clientgui.getClient().getGame().getPlanetaryConditions());
                clientgui.getPlanetaryConditionsDialog().setVisible(true);
                
            } else if (ev.getSource() == butRandomMap) {
                RandomMapDialog rmd = new RandomMapDialog(clientgui.frame, ChatLounge.this, clientgui.getClient(), mapSettings);
                rmd.activateDialog(clientgui.getBoardView().getTilesetManager().getThemes());
                
            } else if (ev.getSource().equals(butChange)) {
                if (lisBoardsAvailable.getSelectedIndex() != -1) {
                    changeMap(lisBoardsAvailable.getSelectedValue());
                    lisBoardsSelected.setSelectedIndex(lisBoardsSelected.getSelectedIndex() + 1);
                }
                
            } else if (ev.getSource().equals(buttonBoardPreview)) {
                previewGameBoard();
                
            } else if (ev.getSource().equals(butMapSize) || ev.getSource().equals(butSpaceSize)) {
                MapDimensionsDialog mdd = new MapDimensionsDialog(clientgui, mapSettings);
                mdd.setVisible(true);
                
            } else if (ev.getSource().equals(comboMapSizes)) {
                if ((comboMapSizes.getSelectedItem() != null)
                        && !comboMapSizes.getSelectedItem().equals(Messages.getString("ChatLounge.CustomMapSize"))) {
                    BoardDimensions size = (BoardDimensions) comboMapSizes.getSelectedItem();
                    mapSettings.setBoardSize(size.width(), size.height());
                    resetAvailBoardSelection = true;
                    resetSelectedBoards = true;
                    clientgui.getClient().sendMapSettings(mapSettings);
                }
                
            } else if (ev.getSource().equals(chkRotateBoard) && (lisBoardsAvailable.getSelectedIndex() != -1)) {
                previewMapsheet();
                
            } else if (ev.getSource().equals(comboMapType)) {
                mapSettings.setMedium(comboMapType.getSelectedIndex());
                clientgui.getClient().sendMapSettings(mapSettings);
                
            } else if (ev.getSource().equals(chkIncludeGround)) {
                if (chkIncludeGround.isSelected()) {
                    mapSettings.setMedium(comboMapType.getSelectedIndex());
                } else {
                    mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                    // set default size for space maps
                    mapSettings.setBoardSize(50, 50);
                    mapSettings.setMapSize(1, 1);
                }
                clientgui.getClient().sendMapDimensions(mapSettings);
            } else if (ev.getSource().equals(chkIncludeSpace)) {
                if (chkIncludeSpace.isSelected()) {
                    mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                    // set default size for space maps
                    mapSettings.setBoardSize(50, 50);
                    mapSettings.setMapSize(1, 1);
                } else {
                    mapSettings.setMedium(comboMapType.getSelectedIndex());
                }
                clientgui.getClient().sendMapDimensions(mapSettings);
            }
        }
    };

    @Override
    public void mouseClicked(MouseEvent arg0) {
        if ((arg0.getClickCount() == 1) && arg0.getSource().equals(lisBoardsAvailable)) {
            previewMapsheet();
        }
        if ((arg0.getClickCount() == 2) && arg0.getSource().equals(lisBoardsAvailable)) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                changeMap(lisBoardsAvailable.getSelectedValue());
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        // ignore
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        // ignore
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
        // ignore
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
        // ignore
    }

    /**
     * Updates to show the map settings that have, presumably, just been sent by
     * the server.
     */
    @Override
    public void updateMapSettings(MapSettings newSettings) {
        mapSettings = MapSettings.getInstance(newSettings);
        refreshMapButtons();
        refreshMapChoice();
        refreshSpaceGround();
        refreshBoardsSelected();
        refreshBoardsAvailable();
        refreshMapSummaryLabel();
        refreshGameYearLabel();
        refreshTechLevelLabel();
    }

    public void refreshMapSummaryLabel() {
        String txt = Messages.getString("ChatLounge.MapSummary"); 
        txt += " " + (mapSettings.getBoardWidth() * mapSettings.getMapWidth()) + " x " 
                + (mapSettings.getBoardHeight() * mapSettings.getMapHeight());
        if (chkIncludeGround.isSelected()) {
            txt += " " + (String) comboMapType.getSelectedItem();
        } else {
            txt += " " + "Space Map"; 
        }
        lblMapSummary.setText(txt);

        StringBuilder selectedMaps = new StringBuilder();
        selectedMaps.append("<html>"); 
        selectedMaps.append(Messages.getString("ChatLounge.MapSummarySelectedMaps"));
        selectedMaps.append("<br>"); 
        ListModel<String> model = lisBoardsSelected.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            String map = model.getElementAt(i);
            selectedMaps.append(map);
            if ((i + 1) < model.getSize()) {
                selectedMaps.append("<br>"); 
            }
        }
        lblMapSummary.setToolTipText(scaleStringForGUI(selectedMaps.toString()));
    }

    public void refreshGameYearLabel() {
        String txt = Messages.getString("ChatLounge.GameYear"); 
        txt += " " + clientgui.getClient().getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        lblGameYear.setText(txt);
    }

    public void refreshTechLevelLabel() {
        String tlString;
        IOption tlOpt = clientgui.getClient().getGame().getOptions().getOption("techlevel");
        if (tlOpt != null) {
            tlString = tlOpt.stringValue();
        } else {
            tlString = TechConstants.getLevelDisplayableName(TechConstants.T_TECH_UNKNOWN);
        }
        String txt = Messages.getString("ChatLounge.TechLevel"); 
        txt = txt + " " + tlString; 
        lblTechLevel.setText(txt);
    }

    @Override
    public void ready() {
        final Client client = clientgui.getClient();
        final IGame game = client.getGame();
        final GameOptions gOpts = game.getOptions();
        // enforce exclusive deployment zones in double blind
        if (gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) 
                && gOpts.booleanOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT)) { 
            int i = client.getLocalPlayer().getStartingPos();
            if (i == 0) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.ExclusiveDeploy.title"), 
                        Messages.getString("ChatLounge.ExclusiveDeploy.msg")); 
                return;
            }
            for (Enumeration<IPlayer> e = client.getGame().getPlayers(); e.hasMoreElements();) {
                IPlayer player = e.nextElement();
                if (player.getStartingPos() == 0) {
                    continue;
                }
                // CTR and EDG don't overlap
                if (((player.getStartingPos() == 9) && (i == 10)) || ((player.getStartingPos() == 10) && (i == 9))) {
                    continue;
                }

                // check for overlapping starting directions
                if (((player.getStartingPos() == i) || ((player.getStartingPos() + 1) == i)
                        || ((player.getStartingPos() - 1) == i))
                        && (player.getId() != client.getLocalPlayer().getId())) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.OverlapDeploy.title"), 
                            Messages.getString("ChatLounge.OverlapDeploy.msg")); 
                    return;
                }
            }
        }

        // Make sure player has a commander if Commander killed victory is on
        if (gOpts.booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) {
            List<String> players = new ArrayList<>();
            if ((game.getLiveCommandersOwnedBy(client.getLocalPlayer()) < 1)
                    && (game.getEntitiesOwnedBy(client.getLocalPlayer()) > 0)) {
                players.add(client.getLocalPlayer().getName());
            }
            for (Client bc : clientgui.getBots().values()) {
                if ((game.getLiveCommandersOwnedBy(bc.getLocalPlayer()) < 1)
                        && (game.getEntitiesOwnedBy(bc.getLocalPlayer()) > 0)) {
                    players.add(bc.getLocalPlayer().getName());
                }
            }
            if (players.size() > 0) {
                String title = Messages.getString("ChatLounge.noCmdr.title"); 
                String msg = Messages.getString("ChatLounge.noCmdr.msg"); 
                for (String player : players) {
                    msg += player + "\n";
                }
                clientgui.doAlertDialog(title, msg);
                return;
            }

        }

        boolean done = !client.getLocalPlayer().isDone();
        client.sendDone(done);
        refreshDoneButton(done);
        for (Client client2 : clientgui.getBots().values()) {
            client2.sendDone(done);
        }
    }

    Client getSelectedPlayer() {
        if ((tablePlayers == null) || (tablePlayers.getSelectedRow() == -1)) {
            return clientgui.getClient();
        }
        String name = playerModel.getPlayerAt(tablePlayers.getSelectedRow()).getName();
        BotClient c = (BotClient) clientgui.getBots().get(name);
        if ((c == null) && clientgui.getClient().getName().equals(name)) {
            return clientgui.getClient();
        }
        return c;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        GUIPreferences.getInstance().removePreferenceChangeListener(this);
    }

    /*
     * This is required because the ChatLounge adds the listener to the
     * MechSummaryCache that must be removed explicitly.
     */
    public void die() {
        MechSummaryCache.getInstance().removeListener(mechSummaryCacheListener);
        GUIPreferences.getInstance().removePreferenceChangeListener(this);
    }

    /**
     * Returns true if the given list of entities can be configured as a group.
     * This requires that they all have the same owner, and that none of the
     * units are being transported.
     *
     * @param entities
     * @return
     */
    private boolean canConfigureAll(List<Entity> entities) {
        if (entities.size() == 1) {
            return true;
        }

        Set<Integer> owners = new HashSet<>();
        boolean containsTransportedUnit = false;
        for (Entity e : entities) {
            containsTransportedUnit |= e.getTransportId() != Entity.NONE;
            owners.add(e.getOwner().getId());
        }
        return (owners.size() == 1) && !containsTransportedUnit;
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        
        if (event.getSource().equals(tablePlayers.getSelectionModel()) 
                || event.getSource().equals(butRemoveBot)) { // ??

            Client selClient = getSelectedPlayer();
            comboTeam.setEnabled(selClient != null);
            butChangeStart.setEnabled(selClient != null);
            butCamo.setEnabled(selClient != null);
            refreshCamos();
            butRemoveBot.setEnabled(selClient instanceof BotClient);
            if (selClient != null) {
                IPlayer selPlayer = selClient.getLocalPlayer();
                boolean hasUnits = !selClient.getGame().getPlayerEntities(selPlayer, false).isEmpty();
                butSaveList.setEnabled(hasUnits && unitsVisible(selPlayer));
                setTeamSelectedItem(selPlayer.getTeam());
            }
        } else if (event.getSource().equals(lisBoardsAvailable)) {
            previewMapsheet();
        }
    }
    
    private void setTeamSelectedItem(int team) {
        comboTeam.removeActionListener(lobbyListener);
        comboTeam.setSelectedIndex(team);
        comboTeam.addActionListener(lobbyListener);
    }
    
    /** 
     * Returns false when any blind-drop option is active and player is not the local player; 
     * true otherwise. When true, individual units of the given player should not be shown/saved/etc. 
     */ 
    private boolean unitsVisible(IPlayer player) {
        boolean isBlindDrop = clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP)
                || clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        return player.equals(clientgui.getClient().getLocalPlayer()) || !isBlindDrop;
    }

    /**
     * A table model for displaying players
     */
    public class PlayerTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -1372393680232901923L;

        private static final int COL_PLAYER = 0;
        private static final int COL_FORCE = 1;
        private static final int N_COL = 2;

        private ArrayList<IPlayer> players;
        private ArrayList<Integer> bvs;
        private ArrayList<Long> costs;
        private ArrayList<Double> tons;

        public PlayerTableModel() {
            players = new ArrayList<>();
            bvs = new ArrayList<>();
            costs = new ArrayList<>();
            tons = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return players.size();
        }

        public void clearData() {
            players = new ArrayList<>();
            bvs = new ArrayList<>();
            costs = new ArrayList<>();
            tons = new ArrayList<>();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public void addPlayer(IPlayer player) {
            players.add(player);
            int bv = 0;
            long cost = 0;
            double ton = 0;
            for (Entity entity : clientgui.getClient().getEntitiesVector()) {
                if (entity.getOwner().equals(player)) {
                    bv += entity.calculateBattleValue();
                    cost += entity.getCost(false);
                    ton += entity.getWeight();
                }
            }
            bvs.add(bv);
            costs.add(cost);
            tons.add(ton);
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            String result = "<HTML>" + UIUtil.guiScaledFontHTML();
            switch (column) {
                case (COL_PLAYER):
                    return result + Messages.getString("ChatLounge.colPlayer");
                default:
                    return result + "Force";
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
            StringBuilder result = new StringBuilder("<HTML><NOBR>" + UIUtil.guiScaledFontHTML());
            IPlayer player = getPlayerAt(row);
            boolean realBlindDrop = !player.equals(clientgui.getClient().getLocalPlayer()) 
                    && clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);

            if (col == COL_FORCE) {
                if (realBlindDrop) {
                    result.append("&nbsp;<BR><I>Unknown</I><BR>&nbsp;");
                } else {
                    double ton = tons.get(row);
                    if (ton < 10) {
                        result.append(String.format("%.2f", ton) + " Tons");
                    } else {
                        result.append(String.format("%,d", Math.round(ton)) + " Tons");
                    }
                    if (costs.get(row) < 10000000) {
                        result.append("<BR>" + String.format("%,d", costs.get(row)) + " C-Bills");
                    } else {
                        result.append("<BR>" + String.format("%,d", costs.get(row) / 1000000) + "\u00B7M C-Bills");
                    }
                    result.append("<BR>" + String.format("%,d", bvs.get(row)) + " BV");
                }
            } else {
                result.append(guiScaledFontHTML(0.1f));
                result.append(player.getName() + "</FONT>");
                boolean isEnemy = clientgui.getClient().getLocalPlayer().isEnemyOf(player);
                result.append(guiScaledFontHTML(isEnemy ? Color.RED : uiGreen()));
                result.append("<BR>" + IPlayer.teamNames[player.getTeam()] + "</FONT>");
                result.append(guiScaledFontHTML());
                result.append("<BR>Start: " + IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);
            }
            return result.toString();
        }

        public IPlayer getPlayerAt(int row) {
            return players.get(row);
        }
    }
    
    public class PlayerTableMouseAdapter extends MouseInputAdapter implements ActionListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = tablePlayers.rowAtPoint(e.getPoint());
                IPlayer player = playerModel.getPlayerAt(row);
                if (player != null) {
                    boolean isLocalPlayer = player.equals(clientgui.getClient().getLocalPlayer());
                    boolean isLocalBot = clientgui.getBots().get(player.getName()) != null;
                    if ((isLocalPlayer || isLocalBot)) {
                        customizePlayer();
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        private void showPopup(MouseEvent e) {
            ScalingPopup popup = new ScalingPopup();
            int row = tablePlayers.rowAtPoint(e.getPoint());
            IPlayer player = playerModel.getPlayerAt(row);
            boolean isOwner = player.equals(clientgui.getClient().getLocalPlayer());
            boolean isBot = clientgui.getBots().containsKey(player.getName());

            JMenuItem menuItem = new JMenuItem("Configure ...");
            menuItem.setActionCommand("CONFIGURE|" + row);
            menuItem.addActionListener(this);
            menuItem.setEnabled(isOwner || isBot);
            popup.add(menuItem);

            if (isBot) {
                JMenuItem botConfig = new JMenuItem("Bot Settings ...");
                botConfig.setActionCommand("BOTCONFIG|" + row);
                botConfig.addActionListener(this);
                popup.add(botConfig);
            }

            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        @Override
        public void actionPerformed(ActionEvent action) {
            StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
            String command = st.nextToken();
            if (command.equalsIgnoreCase("CONFIGURE")) {
                customizePlayer();
            } else if (command.equalsIgnoreCase("BOTCONFIG")) {
                int row = Integer.parseInt(st.nextToken());
                IPlayer player = playerModel.getPlayerAt(row);
                BotClient bot = (BotClient) clientgui.getBots().get(player.getName());
                BotConfigDialog bcd = new BotConfigDialog(clientgui.frame, bot);
                bcd.setVisible(true);

                if (bcd.dialogAborted) {
                    return; // user didn't click 'ok', add no bot
                } else if (bot instanceof Princess) {
                    ((Princess) bot).setBehaviorSettings(bcd.getBehaviorSettings());
                    
                    // bookkeeping:
                    clientgui.getBots().remove(player.getName());
                    bot.setName(bcd.getBotName());
                    clientgui.getBots().put(bot.getName(), bot);
                    player.setName(bcd.getBotName());
                    clientgui.chatlounge.refreshPlayerInfo();
                }
            }
        }

    }

    public class MekTableKeyAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (mekTable.getSelectedRowCount() == 0) {
                return;
            }
            List<Entity> entities = getSelectedEntities();
            int code = e.getKeyCode();
            if ((code == KeyEvent.VK_DELETE) || (code == KeyEvent.VK_BACK_SPACE)) {
                e.consume();
                for (Entity entity : entities) {
                    delete(entity);
                }
            } else if (code == KeyEvent.VK_SPACE) {
                e.consume();
                mechReadout(entities.get(0));
            } else if (code == KeyEvent.VK_ENTER) {
                e.consume();
                if (entities.size() == 1) {
                    customizeMech(entities.get(0));
                } else if (canConfigureAll(entities)) {
                    customizeMechs(entities);
                }
            }
        }
    }

    public class MekTableMouseAdapter extends MouseInputAdapter implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent action) {
            StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
            String command = st.nextToken();
            int[] rows = mekTable.getSelectedRows();
            int row = mekTable.getSelectedRow();
            Entity entity = mekModel.getEntityAt(row);
            Vector<Entity> entities = new Vector<>();
            for (int value : rows) {
                entities.add(mekModel.getEntityAt(value));
            }
            if (null == entity) {
                return;
            }
            if (command.equalsIgnoreCase("VIEW")) {
                mechReadout(entity);
            } else if (command.equalsIgnoreCase("BV")) {
                mechBVDisplay(entity);
            } else if (command.equalsIgnoreCase("DAMAGE")) {
                mechEdit(entity);
            } else if (command.equalsIgnoreCase("INDI_CAMO")) {
                mechCamo(entities);
            } else if (command.equalsIgnoreCase("CONFIGURE")) {
                customizeMech(entity);
            } else if (command.equalsIgnoreCase("CONFIGURE_ALL")) {
                customizeMechs(entities);
            } else if (command.equalsIgnoreCase("DELETE")) {
                Client c = clientgui.getBots().get(entity.getOwner().getName());
                if (c == null) {
                    c = clientgui.getClient();
                }
                for (Entity e : entities) {
                    // first unload any units from this unit
                    if (e.getLoadedUnits().size() > 0) {
                        for (Entity loaded : e.getLoadedUnits()) {
                            e.unload(loaded);
                            loaded.setTransportId(Entity.NONE);
                            c.sendUpdateEntity(loaded);
                        }
                        c.sendUpdateEntity(e);
                    }
                    // unload this unit from any other units it might be loaded
                    // onto
                    if (entity.getTransportId() != Entity.NONE) {
                        Entity loader = clientgui.getClient().getGame().getEntity(entity.getTransportId());
                        if (null != loader) {
                            loader.unload(entity);
                            entity.setTransportId(Entity.NONE);
                            c.sendUpdateEntity(loader);
                            c.sendUpdateEntity(entity);
                        }
                    }
                    c.sendDeleteEntity(e.getId());
                }
            } else if (command.equalsIgnoreCase("SKILLS")) {
                Client c = clientgui.getBots().get(entity.getOwner().getName());
                if (c == null) {
                    c = clientgui.getClient();
                }
                for (Entity e : entities) {
                    for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                        int[] skills = c.getRandomSkillsGenerator().getRandomSkills(e, true);
                        e.getCrew().setGunnery(skills[0], i);
                        e.getCrew().setPiloting(skills[1], i);
                        if (e.getCrew() instanceof LAMPilot) {
                            skills = c.getRandomSkillsGenerator().getRandomSkills(e, true);
                            ((LAMPilot) e.getCrew()).setGunneryAero(skills[0]);
                            ((LAMPilot) e.getCrew()).setPilotingAero(skills[1]);
                        }
                    }
                    e.getCrew().sortRandomSkills();
                    c.sendUpdateEntity(e);
                }
            } else if (command.equalsIgnoreCase(NAME_COMMAND)) {
                Client c = clientgui.getBots().get(entity.getOwner().getName());
                if (c == null) {
                    c = clientgui.getClient();
                }
                for (Entity e : entities) {
                    for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                        Gender gender = RandomGenderGenerator.generate();
                        e.getCrew().setGender(gender, i);
                        e.getCrew().setName(RandomNameGenerator.getInstance().generate(gender, e.getOwner().getName()), i);
                    }
                    c.sendUpdateEntity(e);
                }
            } else if (command.equals(CALLSIGN_COMMAND)) {
                Client c = clientgui.getBots().get(entity.getOwner().getName());
                if (c == null) {
                    c = clientgui.getClient();
                }
                for (Entity e : entities) {
                    for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                        e.getCrew().setNickname(RandomCallsignGenerator.getInstance().generate(), i);
                    }
                    c.sendUpdateEntity(e);
                }
            } else if (command.equalsIgnoreCase("LOAD")) {
                StringTokenizer stLoad = new StringTokenizer(st.nextToken(), ":");
                int id = Integer.parseInt(stLoad.nextToken());
                int bayNumber = Integer.parseInt(stLoad.nextToken());
                Entity loadingEntity = clientgui.getClient().getEntity(id);
                boolean loadRear = false;
                if (stLoad.hasMoreTokens()) {
                    loadRear = Boolean.parseBoolean(stLoad.nextToken());
                }

                double capacity;
                boolean hasEnoughCargoCapacity;
                String errorMessage = "";
                if (bayNumber != -1) {
                    Bay bay = loadingEntity.getBayById(bayNumber);
                    if (null != bay) {
                        double loadSize = entities.stream().mapToDouble(bay::spaceForUnit).sum();
                        capacity = bay.getUnused();
                        hasEnoughCargoCapacity = loadSize <= capacity;
                        errorMessage = Messages.getString("LoadingBay.baytoomany") + // $NON-NLS-2$
                                " " + (int) bay.getUnusedSlots()
                                + bay.getDefaultSlotDescription() + ".";
                        // We're also using bay number to distinguish between front and rear locations
                        // for protomech mag clamp systems
                    } else if (loadingEntity.hasETypeFlag(Entity.ETYPE_MECH)
                            && entities.get(0).hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                        capacity = 1;
                        hasEnoughCargoCapacity = entities.size() == 1;
                        errorMessage = Messages.getString("LoadingBay.protostoomany");
                    } else {
                        hasEnoughCargoCapacity = false;
                        errorMessage = Messages.getString("LoadingBay.bayNumberNotFound", bayNumber);
                    }
                } else {
                    HashMap<Long, Double> capacities, counts;
                    capacities = new HashMap<>();
                    counts = new HashMap<>();
                    HashMap<Transporter, Double> potentialLoad = new HashMap<>();
                    // Get the counts and capacities for all present types
                    for (Entity e : entities) {
                        long entityType = e.getEntityType();
                        long loaderType = loadingEntity.getEntityType();
                        double unitSize;
                        if ((entityType & Entity.ETYPE_MECH) != 0) {
                            entityType = Entity.ETYPE_MECH;
                            unitSize = 1;
                        } else if ((entityType & Entity.ETYPE_INFANTRY) != 0) {
                            entityType = Entity.ETYPE_INFANTRY;
                            boolean useCount = true;
                            if ((loaderType & Entity.ETYPE_TANK) != 0) {
                                // This is a super hack... When getting
                                // capacities, troopspace gives unused space in
                                // terms of tons, and BattleArmorHandles gives
                                // it in terms of unit count. If I call
                                // getUnused, it sums these together, and is
                                // meaningless, so we'll go through all
                                // transporters....
                                boolean hasTroopSpace = false;
                                for (Transporter t : loadingEntity.getTransports()) {
                                    if (t instanceof TankTrailerHitch) {
                                        continue;
                                    }
                                    double loadWeight = e.getWeight();
                                    if (potentialLoad.containsKey(t)) {
                                        loadWeight += potentialLoad.get(t);
                                    }
                                    if (!(t instanceof BattleArmorHandlesTank) && t.canLoad(e)
                                            && (loadWeight <= t.getUnused())) {
                                        hasTroopSpace = true;
                                        potentialLoad.put(t, loadWeight);
                                        break;
                                    }
                                }
                                if (hasTroopSpace) {
                                    useCount = false;
                                }
                            }
                            // TroopSpace uses tonnage
                            // bays and BA handlebars use a count
                            if (useCount) {
                                unitSize = 1;
                            } else {
                                unitSize = e.getWeight();
                            }
                        } else if ((entityType & Entity.ETYPE_PROTOMECH) != 0) {
                            entityType = Entity.ETYPE_PROTOMECH;
                            unitSize = 1;
                            // Loading using mag clamps; user can specify front or rear.
                            // Make use of bayNumber field
                            if ((loaderType & Entity.ETYPE_MECH) != 0) {
                                bayNumber = loadRear? 1 : 0;
                            }
                        } else if ((entityType & Entity.ETYPE_DROPSHIP) != 0) {
                            entityType = Entity.ETYPE_DROPSHIP;
                            unitSize = 1;
                        } else if ((entityType & Entity.ETYPE_JUMPSHIP) != 0) {
                            entityType = Entity.ETYPE_JUMPSHIP;
                            unitSize = 1;
                        } else if ((entityType & Entity.ETYPE_AERO) != 0) {
                            entityType = Entity.ETYPE_AERO;
                            unitSize = 1;
                        } else if ((entityType & Entity.ETYPE_TANK) != 0) {
                            entityType = Entity.ETYPE_TANK;
                            unitSize = 1;
                        } else {
                            unitSize = 1;
                        }

                        Double count = counts.get(entityType);
                        if (count == null) {
                            count = 0.0;
                        }
                        count = count + unitSize;
                        counts.put(entityType, count);

                        Double cap = capacities.get(entityType);
                        if (cap == null) {
                            cap = loadingEntity.getUnused(e);
                            capacities.put(entityType, cap);
                        }
                    }
                    hasEnoughCargoCapacity = true;
                    capacity = 0;
                    for (Long typeId : counts.keySet()) {
                        double currCount = counts.get(typeId);
                        double currCapacity = capacities.get(typeId);
                        if (currCount > currCapacity) {
                            hasEnoughCargoCapacity = false;
                            capacity = currCapacity;
                            String messageName;
                            if (typeId == Entity.ETYPE_INFANTRY) {
                                messageName = "LoadingBay.nonbaytoomanyInf";
                            } else {
                                messageName = "LoadingBay.nonbaytoomany";
                            }
                            errorMessage = Messages.getString(messageName, currCount,
                                    Entity.getEntityTypeName(typeId), currCapacity);
                        }
                    }
                }
                if (hasEnoughCargoCapacity) {
                    for (Entity e : entities) {
                        loader(e, id, bayNumber);
                    }
                } else {
                    JOptionPane.showMessageDialog(clientgui.frame, errorMessage, Messages.getString("LoadingBay.error"), // $NON-NLS-2$
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (command.equalsIgnoreCase("UNLOAD")) {
                Set<Entity> updateCandidates = new HashSet<>();
                for (Entity e: entities) {
                    disembark(e, updateCandidates);
                }
                sendUpdate(updateCandidates);
            } else if (command.equalsIgnoreCase("UNLOADALL")) {
                Set<Entity> updateCandidates = new HashSet<>();
                offloadFrom(entity, updateCandidates);
                sendUpdate(updateCandidates);
            } else if (command.equalsIgnoreCase("UNLOADALLFROMBAY")) {
                int id = Integer.parseInt(st.nextToken());
                Bay bay = entity.getBayById(id);
                Set<Entity> updateCandidates = new HashSet<>();
                for (Entity loadee : bay.getLoadedUnits()) {
                    disembark(loadee, updateCandidates);
                }
                sendUpdate(updateCandidates);
            } else if (command.equalsIgnoreCase("SQUADRON")) {
                Vector<Integer> fighters = new Vector<Integer>();
                for (Entity e : entities) {
                    fighters.add(e.getId());
                }
                if ((!clientgui.getClient().getGame().getOptions()
                        .booleanOption(OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS)
                        && (fighters.size() > FighterSquadron.MAX_SIZE))
                        || (clientgui.getClient().getGame().getOptions()
                        .booleanOption(OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS)
                        && (fighters.size() > FighterSquadron.ALTERNATE_MAX_SIZE))) {
                    JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("FighterSquadron.toomany"),
                            Messages.getString("FighterSquadron.error"), JOptionPane.ERROR_MESSAGE); 
                    // //$NON-NLS-2$
                } else {
                    loadFS(fighters);
                }
            } else if (command.equalsIgnoreCase("SWAP")) {
                int id = Integer.parseInt(st.nextToken());
                swapPilots(entity, id);
            } else if (command.equalsIgnoreCase("CHANGE_OWNER")) {
                // Code to swap entities to a player.
                int id = Integer.parseInt(st.nextToken());
                for (Entity e : entities) {
                    changeEntityOwner(e, id);
                }
            } else if (command.equalsIgnoreCase("SAVE_QUIRKS_ALL")) {
                for (Entity e : entities) {
                    QuirksHandler.addCustomQuirk(e, false);
                }
            } else if (command.equalsIgnoreCase("SAVE_QUIRKS_MODEL")) {
                for (Entity e : entities) {
                    QuirksHandler.addCustomQuirk(e, true);
                }
            } else if (command.equalsIgnoreCase("RAPIDFIREMG_OFF") || command.equalsIgnoreCase("RAPIDFIREMG_ON")) {
                boolean rapidFire = command.equalsIgnoreCase("RAPIDFIREMG_ON");
                for (Entity e : entities) {
                    boolean dirty = false;
                    for (Mounted m : e.getWeaponList()) {
                        WeaponType wtype = (WeaponType) m.getType();
                        if (!wtype.hasFlag(WeaponType.F_MG)) {
                            continue;
                        }
                        m.setRapidfire(rapidFire);
                        dirty = true;
                    }
                    if (dirty) {
                        clientgui.getClient().sendUpdateEntity(e);
                    }
                }
            } else if (command.equalsIgnoreCase("HOTLOAD_OFF") || command.equalsIgnoreCase("HOTLOAD_ON")) {
                boolean hotLoad = command.equalsIgnoreCase("HOTLOAD_ON");
                for (Entity e : entities) {
                    boolean dirty = false;
                    for (Mounted m : e.getWeaponList()) {
                        WeaponType wtype = (WeaponType) m.getType();
                        if (!wtype.hasFlag(WeaponType.F_MISSILE)
                                || (wtype.getAmmoType() != AmmoType.T_LRM)) {
                            continue;
                        }
                        m.setHotLoad(hotLoad);
                        dirty = true;
                    }
                    for (Mounted m : e.getAmmo()) {
                        AmmoType atype = (AmmoType) m.getType();
                        if (atype.getAmmoType() != AmmoType.T_LRM) {
                            continue;
                        }
                        m.setHotLoad(hotLoad);
                        // Set the mode too, so vehicles can switch back
                        int numModes = m.getType().getModesCount();
                        for (int i = 0; i < numModes; i++) {
                            if (m.getType().getMode(i).getName().equals("HotLoad")) {
                                m.setMode(i);
                            }
                        }
                        dirty = true;
                    }
                    if (dirty) {
                        clientgui.getClient().sendUpdateEntity(e);
                    }
                }
            } else if (command.equalsIgnoreCase("SEARCHLIGHT_OFF") || command.equalsIgnoreCase("SEARCHLIGHT_ON")) {
                boolean searchLight = command.equalsIgnoreCase("SEARCHLIGHT_ON");
                for (Entity e : entities) {
                    boolean dirty = false;
                    if (!e.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT)) {
                        e.setExternalSearchlight(searchLight);
                        e.setSearchlightState(searchLight);
                        dirty = true;
                    }
                    if (dirty) {
                        clientgui.getClient().sendUpdateEntity(e);
                    }
                }
            }

        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = mekTable.rowAtPoint(e.getPoint());
                Entity entity = mekModel.getEntityAt(row);
                if (entity != null) {
                    boolean isOwner = entity.getOwner().equals(clientgui.getClient().getLocalPlayer());
                    boolean isBot = clientgui.getBots().get(entity.getOwner().getName()) != null;
                    if ((isOwner || isBot)) {
                        customizeMech(entity);
                    }
                }

            }
        }

        @Override
        public void mousePressed(MouseEvent e) { }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                // If the right mouse button is pressed over an unselected entity,
                // clear the selection and select that entity instead
                int row = mekTable.rowAtPoint(e.getPoint());
                if (!mekTable.isRowSelected(row)) {
                    mekTable.changeSelection(row, row, false, false);
                }
                showPopup(e);
            }
        }

        private void showPopup(MouseEvent e) {
            if (mekTable.getSelectedRowCount() == 0) {
                return;
            }
            List<Entity> entities = getSelectedEntities();
            boolean oneSelected = mekTable.getSelectedRowCount() == 1;
            int row = mekTable.getSelectedRow();
            Entity entity = mekModel.getEntityAt(row);
            
            ScalingPopup popup = new ScalingPopup();
            boolean isOwner = entity.getOwner().equals(clientgui.getClient().getLocalPlayer());
            boolean isBot = clientgui.getBots().get(entity.getOwner().getName()) != null;
            boolean blindDrop = clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.BASE_BLIND_DROP);
            boolean isQuirksEnabled = clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
            boolean isRapidFireMG = clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST);
            boolean isHotLoad = clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD);

            boolean allLoaded = true;
            boolean allUnloaded = true;
            boolean allCapFighter = true;
            boolean allDropships = true;
            boolean allInfantry = true;
            boolean allBattleArmor = true;
            boolean allProtomechs = true;
            boolean allSameEntityType = true;
            boolean hasMGs = false;
            boolean hasLRMS = false;
            boolean hasHotLoad = false;
            boolean hasRapidFireMG = false;
            boolean sameSide = true;
            Entity prevEntity = null;
            int prevOwnerId = -1;
            for (Entity en : entities) {
                if (en.getTransportId() == Entity.NONE) {
                    allLoaded = false;
                } else {
                    allUnloaded = false;
                }
                if (!en.isCapitalFighter(true) || (en instanceof FighterSquadron)) {
                    allCapFighter = false;
                }
                if ((prevOwnerId != -1) && (en.getOwnerId() != prevOwnerId)) {
                    sameSide = false;
                }
                prevOwnerId = en.getOwnerId();
                allDropships &= en.hasETypeFlag(Entity.ETYPE_DROPSHIP);
                allInfantry &= en.hasETypeFlag(Entity.ETYPE_INFANTRY);
                allBattleArmor &= en.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
                allProtomechs &= en.hasETypeFlag(Entity.ETYPE_PROTOMECH);
                if ((prevEntity != null) && !en.getClass().equals(prevEntity.getClass()) && !allInfantry) {
                    allSameEntityType = false;
                }
                if (isRapidFireMG || isHotLoad) {
                    for (Mounted m : en.getWeaponList()) {
                        EquipmentType etype = m.getType();
                        if (etype.hasFlag(WeaponType.F_MG)) {
                            hasMGs = true;
                            hasRapidFireMG |= m.isRapidfire();
                        }
                        if (etype.hasFlag(WeaponType.F_MISSILE)) {
                            hasLRMS |= ((WeaponType) etype).getAmmoType() == AmmoType.T_LRM;
                            hasHotLoad |= m.isHotLoaded();
                        }
                        if (etype.hasFlag(WeaponType.F_MISSILE)) {
                            hasLRMS |= ((WeaponType) etype).getAmmoType() == AmmoType.T_LRM_IMP;
                            hasHotLoad |= m.isHotLoaded();
                        }
                    }
                }
                prevEntity = en;
            }
            // This menu uses the following Mnemonics:
            // B, C, D, E, I, O, R, V
            JMenuItem menuItem;
            if (oneSelected) {
                menuItem = new JMenuItem("View...");
                menuItem.setActionCommand("VIEW");
                menuItem.addActionListener(this);
                menuItem.setEnabled(isOwner || isBot || !blindDrop);
                menuItem.setMnemonic(KeyEvent.VK_V);
                popup.add(menuItem);
            }

            if (oneSelected) {
                menuItem = new JMenuItem("Configure...");
                menuItem.setActionCommand("CONFIGURE");
                menuItem.addActionListener(this);
                menuItem.setEnabled(isOwner || isBot);
            } else {
                menuItem = new JMenuItem("Configure all...");
                menuItem.setActionCommand("CONFIGURE_ALL");
                menuItem.addActionListener(this);
                menuItem.setEnabled(canConfigureAll(entities));
            }
            menuItem.setMnemonic(KeyEvent.VK_C);
            popup.add(menuItem);

            if (oneSelected) {
                menuItem = new JMenuItem("Edit Damage...");
                menuItem.setActionCommand("DAMAGE");
                menuItem.addActionListener(this);
                menuItem.setEnabled(isOwner || isBot);
                menuItem.setMnemonic(KeyEvent.VK_E);
                popup.add(menuItem);
            }

            menuItem = new JMenuItem("Set individual camo...");
            menuItem.setActionCommand("INDI_CAMO");
            menuItem.addActionListener(this);
            menuItem.setEnabled(isOwner || isBot);
            menuItem.setMnemonic(KeyEvent.VK_I);
            popup.add(menuItem);

            if (oneSelected) {
                menuItem = new JMenuItem("View BV Calculation...");
                menuItem.setActionCommand("BV");
                menuItem.addActionListener(this);
                menuItem.setMnemonic(KeyEvent.VK_B);
                menuItem.setEnabled(isOwner || isBot || !blindDrop);
                popup.add(menuItem);
            }

            menuItem = new JMenuItem("Delete");
            menuItem.setActionCommand("DELETE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(isOwner || isBot);
            menuItem.setMnemonic(KeyEvent.VK_D);
            popup.add(menuItem);

            //region Randomize Submenu
            // This menu uses the following Mnemonic Keys:
            // C, N, S
            JMenu menu = new JMenu("Randomize");
            menu.setMnemonic(KeyEvent.VK_R);

            menuItem = new JMenuItem("Name");
            menuItem.setActionCommand(NAME_COMMAND);
            menuItem.addActionListener(this);
            menuItem.setEnabled(isOwner || isBot);
            menuItem.setMnemonic(KeyEvent.VK_N);
            menu.add(menuItem);

            menuItem = new JMenuItem("Callsign");
            menuItem.setActionCommand(CALLSIGN_COMMAND);
            menuItem.addActionListener(this);
            menuItem.setEnabled(isOwner || isBot);
            menuItem.setMnemonic(KeyEvent.VK_C);
            menu.add(menuItem);

            menuItem = new JMenuItem("Skills");
            menuItem.setActionCommand("SKILLS");
            menuItem.addActionListener(this);
            menuItem.setEnabled(isOwner || isBot);
            menuItem.setMnemonic(KeyEvent.VK_S);
            menu.add(menuItem);
            menu.setEnabled(isOwner || isBot);
            popup.add(menu);
            //endregion Randomize Submenu

            // Change Owner Menu Item
            menu = new JMenu(Messages.getString("ChatLounge.ChangeOwner"));
            menu.setEnabled(isOwner || isBot);
            menu.setMnemonic(KeyEvent.VK_O);
            
            for (IPlayer player: clientgui.getClient().getGame().getPlayersVector()) {
                if (!player.equals(ownerOf(entity))) {
                    menuItem = new JMenuItem(player.getName());
                    menuItem.setActionCommand("CHANGE_OWNER|" + player.getId());
                    menuItem.addActionListener(this);
                    menuItem.setEnabled((isOwner || isBot));
                    menu.add(menuItem);
                }
            }
            popup.add(menu);
            
            // Loading Submenus
            if (allUnloaded) {
                menu = new JMenu("Load...");
                JMenu menuDocking = new JMenu("Dock With...");
                JMenu menuSquadrons = new JMenu("Join...");
                JMenu menuMounting = new JMenu("Mount...");
                JMenu menuClamp = new JMenu("Mag Clamp...");
                JMenu menuLoadAll = new JMenu("Load All Into");
                boolean canLoad = false;
                boolean allHaveMagClamp = true;
                for (Entity b : entities) {
                    if (b.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)
                            || b.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                        allHaveMagClamp &= b.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP);
                    }
                }
                for (Entity loader : clientgui.getClient().getGame().getEntitiesVector()) {
                    // TODO don't allow capital fighters to load one another
                    // at the moment
                    if (loader.isCapitalFighter() && !(loader instanceof FighterSquadron)) {
                        continue;
                    }
                    boolean loadable = true;
                    for (Entity en : entities) {
                        if (!loader.canLoad(en, false)
                                || (loader.getId() == en.getId())
                                //TODO: support edge case where a support vee with an internal vehicle bay can load trailer internally
                                || (loader.canTow(en.getId()))) {
                            loadable = false;
                            break;
                        }
                    }
                    if (loadable) {
                        canLoad = true;
                        menuItem = new JMenuItem(loader.getShortName());
                        menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
                        menuItem.addActionListener(this);
                        menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                        menuLoadAll.add(menuItem);
                        JMenu subMenu = new JMenu(loader.getShortName());
                        if ((loader instanceof FighterSquadron) && allCapFighter) {
                            menuItem = new JMenuItem("Join " + loader.getShortName());
                            menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
                            menuItem.addActionListener(this);
                            menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                            menuSquadrons.add(menuItem);
                        } else if ((loader instanceof Jumpship) && allDropships) {
                            int freeCollars = 0;
                            for (Transporter t : loader.getTransports()) {
                                if (t instanceof DockingCollar) {
                                    freeCollars += t.getUnused();
                                }
                            }
                            menuItem = new JMenuItem(
                                    loader.getShortName() + " (Free Collars: " + freeCollars + ")");
                            menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
                            menuItem.addActionListener(this);
                            menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                            menuDocking.add(menuItem);
                        } else if (allBattleArmor && allHaveMagClamp && !loader.isOmni()
                                // Only load magclamps if applicable
                                && loader.hasUnloadedClampMount()
                                // Only choose MagClamps as last option
                                && (loader.getUnused(entities.get(0)) < 2)) {
                            for (Transporter t : loader.getTransports()) {
                                if ((t instanceof ClampMountMech) || (t instanceof ClampMountTank)) {
                                    menuItem = new JMenuItem("Onto " + loader.getShortName());
                                    menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
                                    menuItem.addActionListener(this);
                                    menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                                    menuClamp.add(menuItem);
                                }
                            }
                        } else if (allProtomechs && allHaveMagClamp
                                && loader.hasETypeFlag(Entity.ETYPE_MECH)) {
                            Transporter front = null;
                            Transporter rear = null;
                            for (Transporter t : loader.getTransports()) {
                                if (t instanceof ProtomechClampMount) {
                                    if (((ProtomechClampMount) t).isRear()) {
                                        rear = t;
                                    } else {
                                        front = t;
                                    }
                                }
                            }
                            Entity en = entities.get(0);
                            if ((front != null) && front.canLoad(en)
                                    && ((en.getWeightClass() < EntityWeightClass.WEIGHT_SUPER_HEAVY)
                                            || (rear == null) || rear.getLoadedUnits().isEmpty())) {
                                menuItem = new JMenuItem("Onto Front");
                                menuItem.setActionCommand("LOAD|" + loader.getId() + ":0");
                                menuItem.addActionListener(this);
                                menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                                subMenu.add(menuItem);
                            }
                            boolean frontUltra = (front != null)
                                    && front.getLoadedUnits().stream()
                                    .anyMatch(l -> l.getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY);
                            if ((rear != null) && rear.canLoad(en) && !frontUltra) {
                                menuItem = new JMenuItem("Onto Rear");
                                menuItem.setActionCommand("LOAD|" + loader.getId() + ":1");
                                menuItem.addActionListener(this);
                                menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                                subMenu.add(menuItem);
                            }
                            if (subMenu.getItemCount() > 0) {
                                menuClamp.add(subMenu);
                            }
                        } else if (allInfantry) {
                            menuItem = new JMenuItem(loader.getShortName());
                            menuItem.setActionCommand("LOAD|" + loader.getId() + ":-1");
                            menuItem.addActionListener(this);
                            menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                            menuMounting.add(menuItem);
                        }
                        Entity en = entities.get(0);
                        if (allSameEntityType && !allDropships) {
                            for (Transporter t : loader.getTransports()) {
                                if (t.canLoad(en)) {
                                    if (t instanceof Bay) {
                                        Bay bay = (Bay) t;
                                        menuItem = new JMenuItem("Into Bay #" + bay.getBayNumber() + " (Free "
                                                + "Slots: "
                                                + (int) loader.getBayById(bay.getBayNumber()).getUnusedSlots()
                                                + loader.getBayById(bay.getBayNumber()).getDefaultSlotDescription()
                                                + ")");
                                        menuItem.setActionCommand(
                                                "LOAD|" + loader.getId() + ":" + bay.getBayNumber());
                                        /*
                                         * } else { menuItem = new
                                         * JMenuItem(
                                         * t.getClass().getName()+
                                         * "Transporter" );
                                         * menuItem.setActionCommand("LOAD|"
                                         * + loader.getId() + ":-1"); }
                                         */
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled((isOwner || isBot) && allUnloaded);
                                        subMenu.add(menuItem);
                                    }
                                }
                            }
                            if (subMenu.getMenuComponentCount() > 0) {
                                menu.add(subMenu);
                            }
                        }
                    }
                }
                if (canLoad) {
                    if (menu.getMenuComponentCount() > 0) {
                        menu.setEnabled((isOwner || isBot) && allUnloaded);
                        MenuScroller.createScrollBarsOnMenus(menu);
                        popup.add(menu);
                    }
                    if (menuDocking.getMenuComponentCount() > 0) {
                        menuDocking.setEnabled((isOwner || isBot) && allUnloaded);
                        MenuScroller.createScrollBarsOnMenus(menuDocking);
                        popup.add(menuDocking);
                    }
                    if (menuSquadrons.getMenuComponentCount() > 0) {
                        menuSquadrons.setEnabled((isOwner || isBot) && allUnloaded);
                        MenuScroller.createScrollBarsOnMenus(menuSquadrons);
                        popup.add(menuSquadrons);
                    }
                    if (menuMounting.getMenuComponentCount() > 0) {
                        menuMounting.setEnabled((isOwner || isBot) && allUnloaded);
                        MenuScroller.createScrollBarsOnMenus(menuMounting);
                        popup.add(menuMounting);
                    }
                    if (menuClamp.getMenuComponentCount() > 0) {
                        menuClamp.setEnabled((isOwner || isBot) && allUnloaded);
                        MenuScroller.createScrollBarsOnMenus(menuClamp);
                        popup.add(menuClamp);
                    }
                    boolean hasMounting = menuMounting.getMenuComponentCount() > 0;
                    boolean hasSquadrons = menuSquadrons.getMenuComponentCount() > 0;
                    boolean hasDocking = menuDocking.getMenuComponentCount() > 0;
                    boolean hasLoad = menu.getMenuComponentCount() > 0;
                    boolean hasClamp = menuClamp.getMenuComponentCount() > 0;
                    if ((menuLoadAll.getMenuComponentCount() > 0)
                            && !(hasMounting || hasSquadrons || hasDocking || hasLoad || hasClamp)) {
                        menuLoadAll.setEnabled((isOwner || isBot) && allUnloaded);
                        MenuScroller.createScrollBarsOnMenus(menuLoadAll);
                        popup.add(menuLoadAll);
                    }
                }
            } else if (allLoaded) {
                menuItem = new JMenuItem("Unload");
                menuItem.setActionCommand("UNLOAD");
                menuItem.addActionListener(this);
                menuItem.setEnabled((isOwner || isBot) && allLoaded);
                popup.add(menuItem);
            }
            if (oneSelected && (entity.getLoadedUnits().size() > 0)) {
                menuItem = new JMenuItem("Unload All Carried Units");
                menuItem.setActionCommand("UNLOADALL");
                menuItem.addActionListener(this);
                menuItem.setEnabled((isOwner || isBot));
                popup.add(menuItem);
                JMenu subMenu = new JMenu("Unload All From...");
                for (Bay bay : entity.getTransportBays()) {
                    if (bay.getLoadedUnits().size() > 0) {
                        menuItem = new JMenuItem(
                                "Bay # " + bay.getBayNumber() + " (" + bay.getLoadedUnits().size() + " units)");
                        menuItem.setActionCommand("UNLOADALLFROMBAY|" + bay.getBayNumber());
                        menuItem.addActionListener(this);
                        menuItem.setEnabled((isOwner || isBot));
                        subMenu.add(menuItem);
                    }
                }
                if (subMenu.getItemCount() > 0) {
                    subMenu.setEnabled((isOwner || isBot));
                    popup.add(subMenu);
                }
            }
            if (allCapFighter && allUnloaded && sameSide) {
                menuItem = new JMenuItem("Start Fighter Squadron");
                menuItem.setActionCommand("SQUADRON");
                menuItem.addActionListener(this);
                menuItem.setEnabled((isOwner || isBot) && allCapFighter);
                popup.add(menuItem);
            }
            if (oneSelected) {
                menu = new JMenu("Swap pilots with");
                boolean canSwap = false;
                for (Entity swapper : clientgui.getClient().getGame().getEntitiesVector()) {
                    if (swapper.isCapitalFighter()) {
                        continue;
                    }
                    // only swap your own pilots and with the same unit and crew type
                    if ((swapper.getOwnerId() == entity.getOwnerId()) && (swapper.getId() != entity.getId())
                            && (swapper.getUnitType() == entity.getUnitType())
                            && swapper.getCrew().getCrewType() == entity.getCrew().getCrewType()) {
                        canSwap = true;
                        menuItem = new JMenuItem(swapper.getShortName());
                        menuItem.setActionCommand("SWAP|" + swapper.getId());
                        menuItem.addActionListener(this);
                        menuItem.setEnabled((isOwner || isBot));
                        menu.add(menuItem);
                    }
                }
                if (canSwap) {
                    menu.setEnabled((isOwner || isBot) && canSwap);
                    popup.add(menu);
                }
            }

            // Equipment Submenu
            if (isRapidFireMG || isHotLoad) {
                menu = new JMenu(Messages.getString("ChatLounge.Equipment"));
                if (isRapidFireMG && hasMGs) {
                    if (hasRapidFireMG) {
                        menuItem = new JMenuItem(Messages.getString("ChatLounge.RapidFireToggleOff"));
                        menuItem.setActionCommand("RAPIDFIREMG_OFF");
                    } else {
                        menuItem = new JMenuItem(Messages.getString("ChatLounge.RapidFireToggleOn"));
                        menuItem.setActionCommand("RAPIDFIREMG_ON");
                    }
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(isOwner || isBot);
                    menu.add(menuItem);
                }
                if (isHotLoad && hasLRMS) {
                    if (hasHotLoad) {
                        menuItem = new JMenuItem(Messages.getString("ChatLounge.HotLoadToggleOff"));
                        menuItem.setActionCommand("HOTLOAD_OFF");
                    } else {
                        menuItem = new JMenuItem(Messages.getString("ChatLounge.HotLoadToggleOn"));
                        menuItem.setActionCommand("HOTLOAD_ON");
                    }
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(isOwner || isBot);
                    menu.add(menuItem);
                }
                if (menu.getMenuComponentCount() > 0) {
                    popup.add(menu);
                }
            }

            // Quirks submenu
            if (isQuirksEnabled) {
                JMenu quirksMenu = new JMenu(Messages.getString("ChatLounge.popup.quirks"));
                menuItem = new JMenuItem("Save Quirks for Chassis");
                menuItem.setActionCommand("SAVE_QUIRKS_ALL");
                menuItem.addActionListener(this);
                quirksMenu.add(menuItem);
                menuItem = new JMenuItem("Save Quirks for Chassis/Model");
                menuItem.setActionCommand("SAVE_QUIRKS_MODEL");
                menuItem.addActionListener(this);
                quirksMenu.add(menuItem);
                quirksMenu.setEnabled(isOwner || isBot || !blindDrop);
                popup.add(quirksMenu);
            }

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Catches changes to the GUI scale and adapts the UI accordingly
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
            // Makes a new tooltip appear immediately (rescaled and possibly for a different unit)
            ToolTipManager manager = ToolTipManager.sharedInstance();
            long time = System.currentTimeMillis() - manager.getInitialDelay() + 1;
            Point locationOnScreen = MouseInfo.getPointerInfo().getLocation();
            Point locationOnComponent = new Point(locationOnScreen);
            SwingUtilities.convertPointFromScreen(locationOnComponent, mekTable);
            MouseEvent event = new MouseEvent(mekTable, -1, time, 0, 
                    locationOnComponent.x, locationOnComponent.y, 0, 0, 1, false, 0);
            manager.mouseMoved(event);
            
        } else if (e.getName().equals(IClientPreferences.SHOW_UNIT_ID)) {
            setButUnitIDState(PreferenceManager.getClientPreferences().getShowUnitId());
            mekModel.refreshCells();
        }
    }
    
    private void setButUnitIDState(boolean state) {
        butShowUnitID.removeActionListener(lobbyListener);
        butShowUnitID.setSelected(PreferenceManager.getClientPreferences().getShowUnitId());
        butShowUnitID.addActionListener(lobbyListener);
    }
    
    /** Sets the row height of the MekTable according to compact mode and GUI scale */
    private void setTableRowHeights() {
        int rowbaseHeight = butCompact.isSelected() ? MEKTABLE_ROWHEIGHT_COMPACT : MEKTABLE_ROWHEIGHT_FULL;
        mekTable.setRowHeight(UIUtil.scaleForGUI(rowbaseHeight));
        tablePlayers.setRowHeight(UIUtil.scaleForGUI(PLAYERTABLE_ROWHEIGHT));
    }

    /** Renews the table headers of the MekTable and PlayerTable with guiScaled values */
    private void updateTableHeaders() {
        JTableHeader header = mekTable.getTableHeader();
        TableColumnModel colMod = header.getColumnModel();
        for (int i = 0; i < colMod.getColumnCount(); i++) {
            TableColumn tabCol = colMod.getColumn(i);
            String headerText = mekModel.getColumnName(i);
            if (activeSorter.getColumnIndex() == i) {
                headerText += "&nbsp;&nbsp;&nbsp;" + guiScaledFontHTML(uiGray());
                if (activeSorter.getSortingDirection() == MekTableSorter.Sorting.ASCENDING) {
                    headerText += "\u25B4 ";    
                } else {
                    headerText += "\u25BE ";
                }
                 
                headerText += activeSorter.getDisplayName();
            }
            tabCol.setHeaderValue(headerText);
        }
        header.revalidate();
        
        header = tablePlayers.getTableHeader();
        colMod = header.getColumnModel();
        for (int i = 0; i < colMod.getColumnCount(); i++) {
            TableColumn tabCol = colMod.getColumn(i);
            tabCol.setHeaderValue(playerModel.getColumnName(i));
        }
        header.revalidate();
    }

    /** Returns the owner of the given entity. Should be used over entity.getowner(). */
    private IPlayer ownerOf(Entity entity) {
        return clientgui.getClient().getGame().getPlayer(entity.getOwnerId());
    }
    
    /** Sets the column width of the given table column of the MekTable with the value stored in the GUIP. */
    private void setColumnWidth(TableColumn column) {
        String key;
        switch (column.getModelIndex()) {
        case (MekTableModel.COL_PILOT):
            key = GUIPreferences.LOBBY_MEKTABLE_PILOT_WIDTH;
            break;
        case (MekTableModel.COL_UNIT):
            key = GUIPreferences.LOBBY_MEKTABLE_UNIT_WIDTH;
            break;
        case (MekTableModel.COL_PLAYER):
            key = GUIPreferences.LOBBY_MEKTABLE_PLAYER_WIDTH;
            break;
        default:
            key = GUIPreferences.LOBBY_MEKTABLE_BV_WIDTH;
        }
        column.setPreferredWidth(GUIPreferences.getInstance().getInt(key));
    }
    
    /** Adapts the whole Lobby UI (both panels) to the current guiScale. */
    private void adaptToGUIScale() {
        updateTableHeaders();
        setTableRowHeights();
        refreshMapSummaryLabel();
        refreshGameYearLabel();
        refreshTechLevelLabel();
        refreshCamos();
        refreshMapButtons();
        mekModel.refreshCells();
        
        Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        Font scaledBigFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 3));
        
        butChange.setFont(scaledFont);
        butCompact.setFont(scaledFont);
        butOptions.setFont(scaledFont);
        butLoadList.setFont(scaledFont);
        butSaveList.setFont(scaledFont);
        butArmy.setFont(scaledFont);
        butSkills.setFont(scaledFont);
        butNames.setFont(scaledFont);
        butAddBot.setFont(scaledFont);
        butRemoveBot.setFont(scaledFont);
        butShowUnitID.setFont(scaledFont);
        butChangeStart.setFont(scaledFont);
        butMapSize.setFont(scaledFont);
        butConditions.setFont(scaledFont);
        butRandomMap.setFont(scaledFont);
        butSpaceSize.setFont(scaledFont);
        buttonBoardPreview.setFont(scaledFont);
        comboMapSizes.setFont(scaledFont);
        comboMapType.setFont(scaledFont);
        comboTeam.setFont(scaledFont);
        chkIncludeGround.setFont(scaledFont);
        chkIncludeGround.setIconTextGap(UIUtil.scaleForGUI(5));
        chkIncludeSpace.setFont(scaledFont);
        chkIncludeSpace.setIconTextGap(UIUtil.scaleForGUI(5));
        chkRotateBoard.setFont(scaledFont);
        chkRotateBoard.setIconTextGap(UIUtil.scaleForGUI(5));
        lblBoardsAvailable.setFont(scaledFont);
        lblBoardsSelected.setFont(scaledFont);
        lisBoardsAvailable.setFont(scaledFont);
        lisBoardsSelected.setFont(scaledFont);
        lblGameYear.setFont(scaledFont);
        lblMapSummary.setFont(scaledFont);
        lblTechLevel.setFont(scaledFont);
        
        butAdd.setFont(scaledBigFont);
        panTabs.setFont(scaledBigFont);

        ((TitledBorder)panUnitInfo.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder)panPlayerInfo.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder)panGroundMap.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder)panSpaceMap.getBorder()).setTitleFont(scaledFont);
        
        lblGameYear.setToolTipText(scaleStringForGUI(Messages.getString("ChatLounge.GameYearLabelToolTip"))); 
        lblTechLevel.setToolTipText(scaleStringForGUI(Messages.getString("ChatLounge.TechLevelLabelToolTip"))); 
        buttonBoardPreview.setToolTipText(scaleStringForGUI(Messages.getString("BoardSelectionDialog.ViewGameBoardTooltip")));
    }
    
    /** Returns the given String str enclosed in HTML tags and with a font tag according to the guiScale. */ 
    private String scaleStringForGUI(String str) {
        return "<HTML>" + UIUtil.guiScaledFontHTML() + str + "</FONT></HTML>";
    }
    
    /** 
     * Mouse Listener for the table header of the Mek Table.
     * Saves column widths of the Mek Table when the mouse button is released. 
     * Also switches between table sorting types
     */
    MouseListener mekTableHeaderMouseListener = new MouseAdapter()
    {
        @Override
        public void mouseReleased(MouseEvent e)
        {
            // Save table widths
            for (int i = 0; i < MekTableModel.N_COL; i++) {
                TableColumn column = mekTable.getColumnModel().getColumn(i);
                String key;
                switch (column.getModelIndex()) {
                case (MekTableModel.COL_PILOT):
                    key = GUIPreferences.LOBBY_MEKTABLE_PILOT_WIDTH;
                    break;
                case (MekTableModel.COL_UNIT):
                    key = GUIPreferences.LOBBY_MEKTABLE_UNIT_WIDTH;
                    break;
                case (MekTableModel.COL_PLAYER):
                    key = GUIPreferences.LOBBY_MEKTABLE_PLAYER_WIDTH;
                    break;
                default:
                    key = GUIPreferences.LOBBY_MEKTABLE_BV_WIDTH;
                }
                GUIPreferences.getInstance().setValue(key, column.getWidth());
            }
            
            changeMekTableSorter(e);
        }

    };
    
    /** 
     * Sets the sorting used in the Mek Table depending on the column header 
     * that was clicked.  
     */ 
    private void changeMekTableSorter(MouseEvent e) {
        int col = mekTable.columnAtPoint(e.getPoint());
        MekTableSorter previousSorter = activeSorter;
        List<MekTableSorter> sorters;
        
        // find the right list of sorters (or do nothing, if the column is not sortable)
        if (col == MekTableModel.COL_UNIT) {
            sorters = unitSorters;
        } else if (col == MekTableModel.COL_BV) {
            sorters = bvSorters;
        } else {
            return;
        }
        
        // Select the next allowed sorter
        nextSorter(sorters);

        if (activeSorter != previousSorter) {
            refreshEntities();
            updateTableHeaders();
        }
    }
    
    /** Selects the next allowed sorter in the given list of sorters. */
    private void nextSorter(List<MekTableSorter> sorters) {
        // Set the next sorter as active, if this column was already sorted, or
        // the first sorter otherwise
        int index = sorters.indexOf(activeSorter);
        if (index == -1) {
            activeSorter = sorters.get(0);
        } else {
            index = (index + 1) % sorters.size();
            activeSorter = sorters.get(index);
        }
        
        // Find an allowed sorter (e.g. blind drop may disallow some)
        int counter = 0; // Endless loop safeguard
        while (!activeSorter.isAllowed(clientgui.getClient().getGame().getOptions())
                && ++counter < 100) {
            index = (index + 1) % sorters.size();
            activeSorter = sorters.get(index);
        }
    }
    
    public class PlayerRenderer extends JPanel implements TableCellRenderer {
        
        private JLabel lblImage = new JLabel();

        public PlayerRenderer() {
            setLayout(new GridLayout(1,1,5,0));
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            add(lblImage);
        }
        
        private void setImage(Image img) {
            lblImage.setIcon(new ImageIcon(img));
        }

        private static final long serialVersionUID = 4947299735765324311L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            lblImage.setText(value.toString());

            if (column == PlayerTableModel.COL_PLAYER) {
                lblImage.setIconTextGap(scaleForGUI(5));
                IPlayer player = playerModel.getPlayerAt(row);
                if (player == null) {
                    return null;
                }
                Image camo = player.getCamouflage().getImage();
                int size = scaleForGUI(PLAYERTABLE_ROWHEIGHT) / 2;
                setImage(camo.getScaledInstance(-1, size, Image.SCALE_SMOOTH));
            } else {
                lblImage.setIconTextGap(scaleForGUI(5));
                lblImage.setIcon(null);
            }
            

            //                    setToolTipText(formatUnitTooltip(entity));
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                Color background = table.getBackground();
                if (row % 2 != 0) {
                    Color alternateColor = UIManager.getColor("Table.alternateRowColor");
                    if (alternateColor == null) {
                        // If we don't have an alternate row color, use 'controlHighlight'
                        // as it is pretty reasonable across the various themes.
                        alternateColor = UIManager.getColor("controlHighlight");
                    }
                    if (alternateColor != null) {
                        background = alternateColor;
                    }
                }
                setForeground(table.getForeground());
                setBackground(background);
            }

            if (hasFocus) {
                if (!isSelected) {
                    Color col = UIManager.getColor("Table.focusCellForeground");
                    if (col != null) {
                        setForeground(col);
                    }
                    col = UIManager.getColor("Table.focusCellBackground");
                    if (col != null) {
                        setBackground(col);
                    }
                }
            }

            return this;
        }
    }
    
    public boolean isCompact() {
        return butCompact.isSelected();
    }
    
    public MapSettings getMapSettings() {
        return mapSettings;
    }
    
    /** Returns a list of the selected entities in the Mek table. The list may be empty but not null. */
    private List<Entity> getSelectedEntities() {
        ArrayList<Entity> result = new ArrayList<>();
        int[] rows = mekTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            result.add(mekModel.getEntityAt(rows[i]));
        }
        return result;
    }
}
