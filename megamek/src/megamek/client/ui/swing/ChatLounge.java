/*
 * MegaMek -
 *  Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MapSettings;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.options.Quirks;
import megamek.common.util.BoardUtilities;
import megamek.common.util.DirectoryItems;

public class ChatLounge extends AbstractPhaseDisplay implements ActionListener, ItemListener, ListSelectionListener, MouseListener, IMapSettingsObserver {
    /**
     *
     */
    private static final long serialVersionUID = 1454736776730903786L;

    private JButton butOptions;

    private JTabbedPane panTabs;
    private JPanel panMain;
    private JPanel panMap;

    /*Unit Configuration Panel*/
    private JPanel panUnitInfo;
    JButton butLoad;
    JButton butArmy;
    JButton butSkills;
    JButton butLoadCustomFS;
    private JButton butLoadList;
    private JButton butSaveList;
    private JButton butDeleteAll;

    /*Unit Table*/
    JTable tableEntities;
    private JScrollPane scrEntities;
    private JToggleButton butCompact;

    private MekTableModel mekModel;

    /*Player Configuration Panel*/
    private JPanel panPlayerInfo;
    private JComboBox choTeam;
    private JButton butCamo;
    private JButton butAddBot;
    private JButton butRemoveBot;
    private JButton butChangeStart;
    private JTable tablePlayers;
    private JScrollPane scrPlayers;
    private PlayerTableModel playerModel;

    /*Map Settings Panel*/
    private MapSettings mapSettings;
    private JButton butConditions;
    private RandomMapDialog randomMapDialog;
    private JPanel panGroundMap;
    private JPanel panSpaceMap;
    private JComboBox comboMapType;
    private JButton butMapSize;
    private JButton butRandomMap;
    private JButton buttonBoardPreview;
    private JScrollPane scrMapButtons;
    private JPanel panMapButtons;
    private JLabel labBoardsSelected;
    private JList lisBoardsSelected;
    private JScrollPane scrBoardsSelected;
    private JButton butChange;
    private JLabel labBoardsAvailable;
    private JList lisBoardsAvailable;
    private JScrollPane scrBoardsAvailable;
    private JCheckBox chkRotateBoard;
    private JCheckBox chkIncludeGround;
    private JCheckBox chkIncludeSpace;
    private JButton butSpaceSize;

    JPanel mapPreviewPanel;
    MiniMap miniMap = null;
    JDialog gameBoardPreviewW;
    MiniMap gameBoardMap = null;

    // keep track of portrait images
    private DirectoryItems portraits;

    private MechSummaryCache.Listener mechSummaryCacheListener = new MechSummaryCache.Listener() {
        public void doneLoading() {
            butLoad.setEnabled(true);
            butArmy.setEnabled(true);
            butLoadCustomFS.setEnabled(true);
        }
    };

    CamoChoiceDialog camoDialog;

    /**
     * Creates a new chat lounge for the clientgui.getClient().
     */
    public ChatLounge(ClientGUI clientgui) {
        this.clientgui = clientgui;

        // Create a tabbed panel to hold our components.
        panTabs = new JTabbedPane();
        Font tabPanelFont = new Font("Dialog", Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getInt("AdvancedChatLoungeTabFontSize"));
        panTabs.setFont(tabPanelFont);

        try {
            portraits = new DirectoryItems(new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    ImageFileFactory.getInstance());
        } catch (Exception e) {
            portraits = null;
        }

        clientgui.getClient().game.addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        butOptions = new JButton(Messages.getString("ChatLounge.butOptions")); //$NON-NLS-1$
        butOptions.addActionListener(this);

        butCompact = new JToggleButton(Messages.getString("ChatLounge.butCompact")); //$NON-NLS-1$
        butCompact.addActionListener(this);

        butDone.setText(Messages.getString("ChatLounge.butDone")); //$NON-NLS-1$
        Font font = null;
        try {
            font = new Font("sanserif", Font.BOLD, 12); //$NON-NLS-1$
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        if (font == null) {
            System.err.println("Couldn't find the new font for the 'Done' button."); //$NON-NLS-1$
        } else {
            butDone.setFont(font);
        }

        setupPlayerInfo();

        refreshGameSettings();

        setupEntities();
        setupUnitConfiguration();

        refreshEntities();

        setupMainPanel();

        // layout main thing
        setLayout(new BorderLayout());

        if (GUIPreferences.getInstance().getChatLoungeTabs()) {
            add(panTabs, BorderLayout.CENTER);
        } else {
            add(panMain, BorderLayout.CENTER);
        }
    }

    /**
     * Sets up the entities table
     */
    private void setupEntities() {

        mekModel = new MekTableModel();
        tableEntities = new JTable();
        tableEntities.setModel(mekModel);
        tableEntities.setRowHeight(80);
        tableEntities.setIntercellSpacing(new Dimension(0, 0));
        tableEntities.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumn column = null;
        for (int i = 0; i < MekTableModel.N_COL; i++) {
            tableEntities.getColumnModel().getColumn(i).setCellRenderer(mekModel.getRenderer());
            column = tableEntities.getColumnModel().getColumn(i);
            if ((i == MekTableModel.COL_UNIT) || (i == MekTableModel.COL_PILOT)) {
                column.setPreferredWidth(170);
            } else if (i == MekTableModel.COL_PLAYER) {
                column.setPreferredWidth(50);
            } else {
                column.setPreferredWidth(10);
            }
        }
        tableEntities.addMouseListener(new MekTableMouseAdapter());
        tableEntities.addKeyListener(new MekTableKeyAdapter());
        tableEntities.getSelectionModel().addListSelectionListener(this);
        scrEntities = new JScrollPane(tableEntities);
        scrEntities.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    }

    /**
     * Sets up the unit configuration panel
     */
    private void setupUnitConfiguration() {

        butLoadList = new JButton(Messages.getString("ChatLounge.butLoadList")); //$NON-NLS-1$
        butLoadList.setActionCommand("load_list"); //$NON-NLS-1$
        butLoadList.addActionListener(this);

        butSaveList = new JButton(Messages.getString("ChatLounge.butSaveList")); //$NON-NLS-1$
        butSaveList.setActionCommand("save_list"); //$NON-NLS-1$
        butSaveList.addActionListener(this);
        butSaveList.setEnabled(false);

        butLoad = new JButton(Messages.getString("ChatLounge.butLoad")); //$NON-NLS-1$
        butArmy = new JButton(Messages.getString("ChatLounge.butArmy")); //$NON-NLS-1$
        butSkills = new JButton(Messages.getString("ChatLounge.butSkills")); //$NON-NLS-1$
        butLoadCustomFS = new JButton(Messages.getString("ChatLounge.butLoadCustomFS"));

        MechSummaryCache mechSummaryCache = MechSummaryCache.getInstance();
        mechSummaryCache.addListener(mechSummaryCacheListener);
        butLoad.setEnabled(mechSummaryCache.isInitialized());
        butArmy.setEnabled(mechSummaryCache.isInitialized());
        butLoadCustomFS.setEnabled(mechSummaryCache.isInitialized());

        butSkills.setEnabled(true);

        Font font = new Font("Sans Serif", Font.BOLD, 18); //$NON-NLS-1$
        butLoad.setFont(font);
        butLoad.setActionCommand("load_mech"); //$NON-NLS-1$
        butLoad.addActionListener(this);
        butArmy.addActionListener(this);
        butSkills.addActionListener(this);
        butLoadCustomFS.setActionCommand("load_custom_fs"); //$NON-NLS-1$
        butLoadCustomFS.addActionListener(this);

        butDeleteAll = new JButton(Messages.getString("ChatLounge.butDeleteAll")); //$NON-NLS-1$
        butDeleteAll.setActionCommand("delete_all"); //$NON-NLS-1$
        butDeleteAll.addActionListener(this);
        butDeleteAll.setEnabled(false);

        panUnitInfo = new JPanel();
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
        c.gridheight = 1;
        gridbag.setConstraints(butLoad, c);
        panUnitInfo.add(butLoad);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(butArmy, c);
        panUnitInfo.add(butArmy);

        c.gridx = 0;
        c.gridy = 2;
        gridbag.setConstraints(butSkills, c);
        panUnitInfo.add(butSkills);

        c.gridx = 0;
        c.gridy = 3;
        gridbag.setConstraints(butLoadCustomFS, c);
        panUnitInfo.add(butLoadCustomFS);

        c.gridx = 1;
        c.gridy = 1;
        gridbag.setConstraints(butLoadList, c);
        panUnitInfo.add(butLoadList);

        c.gridx = 1;
        c.gridy = 2;
        gridbag.setConstraints(butSaveList, c);
        panUnitInfo.add(butSaveList);

        c.gridx = 1;
        c.gridy = 3;
        gridbag.setConstraints(butDeleteAll, c);
        panUnitInfo.add(butDeleteAll);
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
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColIndex = convertColumnIndexToModel(colIndex);
                Player player =  playerModel.getPlayerAt(rowIndex);
                if(player == null) {
                	return null;
                }
                int mines = player.getNbrMFConventional() + player.getNbrMFActive() + player.getNbrMFInferno() + player.getNbrMFVibra();
                if (realColIndex == PlayerTableModel.COL_PLAYER) {
                    return Messages.getString("ChatLounge.tipPlayer", new Object[]
                        { getValueAt(rowIndex, colIndex), player.getConstantInitBonus(), mines });
                } else if (realColIndex == PlayerTableModel.COL_BV) {
                    int bv = (Integer) getValueAt(rowIndex, colIndex);
                    float ratio = playerModel.getPlayerAt(rowIndex).getForceSizeBVMod();
                    return Messages.getString("ChatLounge.tipBV", new Object[]
                        { bv, ratio });
                } else if (realColIndex == PlayerTableModel.COL_TON) {
                    return Float.toString((Float) getValueAt(rowIndex, colIndex));
                } else if (realColIndex == PlayerTableModel.COL_COST) {
                    return Messages.getString("ChatLounge.tipCost", new Object[]
                        { (Integer) getValueAt(rowIndex, colIndex) });
                } else if (realColIndex == PlayerTableModel.COL_START) {
                        return (String) getValueAt(rowIndex, colIndex);
                } else {
                    return Integer.toString((Integer) getValueAt(rowIndex, colIndex));
                }
            }
        };
        tablePlayers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePlayers.getSelectionModel().addListSelectionListener(this);

        tablePlayers.setModel(playerModel);
        TableColumn column = null;
        for (int i = 0; i < PlayerTableModel.N_COL; i++) {
            column = tablePlayers.getColumnModel().getColumn(i);
            if (i == PlayerTableModel.COL_PLAYER) {
                column.setPreferredWidth(100);
            } else if (i == PlayerTableModel.COL_TEAM) {
                column.setPreferredWidth(5);
            } else if ((i == PlayerTableModel.COL_COST) || (i == PlayerTableModel.COL_START)) {
                column.setPreferredWidth(50);
            } else {
                column.setPreferredWidth(30);
            }
        }

        tablePlayers.addMouseListener(new PlayerTableMouseAdapter());

        scrPlayers = new JScrollPane(tablePlayers);
        scrPlayers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panPlayerInfo = new JPanel();
        panPlayerInfo.setBorder(BorderFactory.createTitledBorder("Player Setup"));

        butAddBot = new JButton(Messages.getString("ChatLounge.butAddBot")); //$NON-NLS-1$
        butAddBot.setActionCommand("add_bot"); //$NON-NLS-1$
        butAddBot.addActionListener(this);

        butRemoveBot = new JButton(Messages.getString("ChatLounge.butRemoveBot")); //$NON-NLS-1$
        butRemoveBot.setEnabled(false);
        butRemoveBot.setActionCommand("remove_bot"); //$NON-NLS-1$
        butRemoveBot.addActionListener(this);

        choTeam = new JComboBox();
        setupTeams();
        choTeam.addItemListener(this);

        butCamo = new JButton();
        butCamo.setPreferredSize(new Dimension(84, 72));
        butCamo.setActionCommand("camo"); //$NON-NLS-1$
        butCamo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                camoDialog.setPlayer(getPlayerSelected().getLocalPlayer());
                camoDialog.setVisible(true);
                getPlayerSelected().sendPlayerInfo();
            }
        });
        camoDialog = new CamoChoiceDialog(clientgui.getFrame(), butCamo);
        refreshCamos();

        butChangeStart = new JButton(Messages.getString("ChatLounge.butChangeStart")); //$NON-NLS-1$
        butChangeStart.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panPlayerInfo.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(choTeam, c);
        panPlayerInfo.add(choTeam);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butChangeStart, c);
        panPlayerInfo.add(butChangeStart);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butAddBot, c);
        panPlayerInfo.add(butAddBot);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butRemoveBot, c);
        panPlayerInfo.add(butRemoveBot);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 3;
        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(butCamo, c);
        panPlayerInfo.add(butCamo);

        refreshPlayerInfo();
    }


    private void setupMainPanel() {
        setupMap();

        panMain = new JPanel();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMain.setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        gridbag.setConstraints(butOptions, c);
        panMain.add(butOptions);

        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        gridbag.setConstraints(butCompact, c);
        panMain.add(butCompact);


        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = 3;
        gridbag.setConstraints(scrEntities, c);
        panMain.add(scrEntities);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(panUnitInfo, c);
        panMain.add(panUnitInfo);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(panPlayerInfo, c);
        panMain.add(panPlayerInfo);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(scrPlayers, c);
        panMain.add(scrPlayers);

        panTabs.add("Select Units", panMain); //$NON-NLS-1$
        panTabs.add("Select Map", panMap); //$NON-NLS-1$
    }

    private void setupMap() {

        panMap = new JPanel();

        mapSettings = (MapSettings) clientgui.getClient().getMapSettings().clone();

        randomMapDialog = new RandomMapDialog(clientgui.frame, this, clientgui.getClient(), mapSettings);

        butConditions  = new JButton(Messages.getString("ChatLounge.butConditions")); //$NON-NLS-1$
        butConditions.addActionListener(this);

        butRandomMap = new JButton(Messages.getString("BoardSelectionDialog.GeneratedMapSettings")); //$NON-NLS-1$
        butRandomMap.addActionListener(this);

        chkIncludeGround  = new JCheckBox(Messages.getString("ChatLounge.IncludeGround")); //$NON-NLS-1$
        chkIncludeGround.addActionListener(this);

        chkIncludeSpace = new JCheckBox(Messages.getString("ChatLounge.IncludeSpace")); //$NON-NLS-1$
        chkIncludeSpace.addActionListener(this);

        setupGroundMap();
        setupSpaceMap();
        refreshSpaceGround();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMap.setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
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
    private void setupGroundMap() {

        panGroundMap = new JPanel();
        panGroundMap.setBorder(BorderFactory.createTitledBorder("Planetary Map"));

        panMapButtons = new JPanel();

        comboMapType = new JComboBox();
        setupMapChoice();

        butMapSize = new JButton(Messages.getString("ChatLounge.MapSize")); //$NON-NLS-1$
        butMapSize.addActionListener(this);

        buttonBoardPreview = new JButton(Messages.getString("BoardSelectionDialog.ViewGameBoard")); //$NON-NLS-1$
        buttonBoardPreview.addActionListener(this);

        butChange = new JButton("<<"); //$NON-NLS-1$
        butChange.addActionListener(this);

        labBoardsSelected = new JLabel(Messages.getString("BoardSelectionDialog.MapsSelected"), SwingConstants.CENTER); //$NON-NLS-1$
        labBoardsAvailable = new JLabel(Messages.getString("BoardSelectionDialog.mapsAvailable"), SwingConstants.CENTER); //$NON-NLS-1$

        lisBoardsSelected = new JList(new DefaultListModel());
        lisBoardsAvailable = new JList(new DefaultListModel());
        refreshBoardsSelected();
        refreshBoardsAvailable();
        lisBoardsAvailable.addMouseListener(this);
        lisBoardsAvailable.addListSelectionListener(this);

        chkRotateBoard = new JCheckBox(Messages.getString("BoardSelectionDialog.RotateBoard")); //$NON-NLS-1$
        chkRotateBoard.addActionListener(this);

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
        gridbag.setConstraints(butMapSize, c);
        panGroundMap.add(butMapSize);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 3;
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
        c.gridy = 4;
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
        gridbag.setConstraints(labBoardsSelected, c);
        panGroundMap.add(labBoardsSelected);

        scrBoardsSelected = new JScrollPane(lisBoardsSelected);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 3;
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
        gridbag.setConstraints(labBoardsAvailable, c);
        panGroundMap.add(labBoardsAvailable);

        scrBoardsAvailable = new JScrollPane(lisBoardsAvailable);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 3;
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
        c.gridheight = 3;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(mapPreviewPanel, c);
        panGroundMap.add(mapPreviewPanel);



        try {
            miniMap = new MiniMap(mapPreviewPanel, null);
            //Set a default size for the minimap object to ensure it will have space on the screen to be drawn.
            miniMap.setSize(160, 200);
            miniMap.setZoom(2);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                            Messages.getString("BoardEditor.CouldNotInitialiseMinimap") + e,
                            Messages.getString("BoardEditor.FatalError"),
                            JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
        }
        mapPreviewPanel.add(miniMap);

        //setup the board preview window.
        gameBoardPreviewW = new JDialog(clientgui.frame, Messages.getString("BoardSelectionDialog.ViewGameBoard"), false); //$NON-NLS-1$

        gameBoardPreviewW.setLocation(GUIPreferences.getInstance().getMinimapPosX(),
                GUIPreferences.getInstance().getMinimapPosY());

        gameBoardPreviewW.setVisible(false);
        try {
            gameBoardMap = new MiniMap(gameBoardPreviewW, null);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, Messages
                                    .getString("BoardEditor.CouldNotInitialiseMinimap") + e,
                                    Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            //this.dispose();
        }
        gameBoardPreviewW.add(gameBoardMap);

        gameBoardPreviewW.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameBoardPreviewW.setVisible(false);
            }
        });
    }

    private void setupSpaceMap() {

        panSpaceMap = new JPanel();
        panSpaceMap.setBorder(BorderFactory.createTitledBorder("Space Map"));

        butSpaceSize = new JButton(Messages.getString("ChatLounge.MapSize"));
        butSpaceSize.addActionListener(this);

        // layout
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
        gridbag.setConstraints(chkIncludeSpace, c);
        panSpaceMap.add(chkIncludeSpace);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(butSpaceSize, c);
        panSpaceMap.add(butSpaceSize);

    }

    /**
     * Set up the map chooser panel
     */
    private void setupMapChoice() {
        comboMapType.addItem(MapSettings.getMediumName(MapSettings.MEDIUM_GROUND));
        comboMapType.addItem(MapSettings
                .getMediumName(MapSettings.MEDIUM_ATMOSPHERE));
        comboMapType.addActionListener(this);
        refreshMapChoice();
    }

    private void refreshMapChoice() {
        comboMapType.removeActionListener(this);
        if(mapSettings.getMedium() < MapSettings.MEDIUM_SPACE) {
            comboMapType.setSelectedIndex(mapSettings.getMedium());
        }
        comboMapType.addActionListener(this);
    }

    private void refreshSpaceGround() {
        chkIncludeGround.removeActionListener(this);
        chkIncludeSpace.removeActionListener(this);
        boolean inSpace = mapSettings.getMedium() == MapSettings.MEDIUM_SPACE;
        chkIncludeSpace.setSelected(inSpace);
        chkIncludeGround.setSelected(!inSpace);
        comboMapType.setEnabled(!inSpace);
        butMapSize.setEnabled(!inSpace);
        buttonBoardPreview.setEnabled(!inSpace);
        lisBoardsSelected.setEnabled(!inSpace);
        butChange.setEnabled(!inSpace);
        lisBoardsAvailable.setEnabled(!inSpace);
        chkRotateBoard.setEnabled(!inSpace);
        butSpaceSize.setEnabled(inSpace);
        chkIncludeGround.addActionListener(this);
        chkIncludeSpace.addActionListener(this);
    }

    private void refreshBoardsAvailable() {
        ((DefaultListModel) lisBoardsAvailable.getModel()).removeAllElements();
        for (Iterator<String> i = mapSettings.getBoardsAvailable(); i.hasNext();) {
            ((DefaultListModel) lisBoardsAvailable.getModel()).addElement(i
                    .next());
        }
    }

    private void refreshBoardsSelected() {
        ((DefaultListModel) lisBoardsSelected.getModel()).removeAllElements();
        int index = 0;
        for (Iterator<String> i = mapSettings.getBoardsSelected(); i.hasNext();) {
            ((DefaultListModel) lisBoardsSelected.getModel())
                    .addElement(index++ + ": " + i.next()); //$NON-NLS-1$
        }
        lisBoardsSelected.setSelectedIndex(0);
    }

    /**
     * Fills the Map Buttons scroll pane with the appropriate amount of buttons
     * in the appropriate layout
     */
    private void refreshMapButtons() {
        panMapButtons.removeAll();

        panMapButtons.setLayout(new GridLayout(mapSettings.getMapHeight(),
                mapSettings.getMapWidth()));

        for (int i = 0; i < mapSettings.getMapHeight(); i++) {
            for (int j = 0; j < mapSettings.getMapWidth(); j++) {
                JButton button = new JButton(Integer.toString(i
                        * mapSettings.getMapWidth() + j));
                button.addActionListener(this);
                panMapButtons.add(button);
            }
        }

        scrMapButtons.validate();

        labBoardsAvailable.setText(mapSettings.getBoardWidth() + "x" + mapSettings.getBoardHeight() + " " + Messages.getString("BoardSelectionDialog.mapsAvailable"));

    }

    public void previewMapsheet() {
        String boardName = (String) lisBoardsAvailable.getSelectedValue();
        if (lisBoardsAvailable.getSelectedIndex() > 2) {
            IBoard board = new Board(16, 17);
            board.load(boardName + ".board");
            if (chkRotateBoard.isSelected()) {
                BoardUtilities.flip(board, true, true);
            }
            miniMap.setBoard(board);
        }
    }

    public void previewGameBoard() {
        MapSettings temp = mapSettings;
        temp.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
        temp.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        IBoard[] sheetBoards = new IBoard[temp.getMapWidth() * temp.getMapHeight()];
        for (int i = 0; i < temp.getMapWidth() * temp.getMapHeight(); i++) {
            sheetBoards[i] = new Board();
            String name = temp.getBoardsSelectedVector().get(i);
            boolean isRotated = false;
            if (name.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                // only rotate boards with an even width
                if (temp.getBoardWidth() % 2 == 0) {
                    isRotated = true;
                }
                name = name.substring(Board.BOARD_REQUEST_ROTATION.length());
            }
            if (name.startsWith(MapSettings.BOARD_GENERATED) || (temp.getMedium() == MapSettings.MEDIUM_SPACE)) {
                sheetBoards[i] = BoardUtilities.generateRandom(temp);
            } else {
                sheetBoards[i].load(name + ".board");
                BoardUtilities.flip(sheetBoards[i], isRotated, isRotated);
            }
        }

        IBoard newBoard = BoardUtilities.combine(temp.getBoardWidth(), temp.getBoardHeight(), temp
                .getMapWidth(), temp.getMapHeight(), sheetBoards, temp.getMedium());
        gameBoardMap.setBoard(newBoard);
        gameBoardPreviewW.setVisible(true);

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

        /*
         * We will attempt to sort by the following criteria: My units first,
         * then my teamates units, then other teams units. We will also sort by
         * player name within the forementioned categories. Finally, a players
         * units will be sorted by the order they were "added" to the list.
         */
        ArrayList<Entity> allEntities = new ArrayList<Entity>();
        for (Enumeration<Entity> i = clientgui.getClient().getEntities(); i.hasMoreElements();) {
            Entity entity = i.nextElement();
            // sortedEntities.add(entity);
            allEntities.add(entity);
        }

        Collections.sort(allEntities, new Comparator<Entity>() {
            public int compare(final Entity a, final Entity b) {
                // entity.getOwner() does not work properly because teams are
                // not updated for
                // entities when the user switches teams
                final Player p_a = clientgui.getClient().game.getPlayer(a.getOwnerId());// a.getOwner();
                final Player p_b = clientgui.getClient().game.getPlayer(b.getOwnerId());// b.getOwner();
                final int t_a = p_a.getTeam();
                final int t_b = p_b.getTeam();
                if (p_a.equals(clientgui.getClient().getLocalPlayer()) && !p_b.equals(clientgui.getClient().getLocalPlayer())) {
                    return -1;
                } else if (p_b.equals(clientgui.getClient().getLocalPlayer()) && !p_a.equals(clientgui.getClient().getLocalPlayer())) {
                    return 1;
                } else if ((t_a == clientgui.getClient().getLocalPlayer().getTeam()) && (t_b != clientgui.getClient().getLocalPlayer().getTeam())) {
                    return -1;
                } else if ((t_b == clientgui.getClient().getLocalPlayer().getTeam()) && (t_a != clientgui.getClient().getLocalPlayer().getTeam())) {
                    return 1;
                } else if (t_a != t_b) {
                    return t_a - t_b;
                } else if (!p_a.equals(p_b)) {
                    return p_a.getName().compareTo(p_b.getName());
                } else {
                    return a.getId() - b.getId();
                }
            }
        });

        for (Entity entity : allEntities) {
            // Remember if the local player has units.
            if (!localUnits && entity.getOwner().equals(clientgui.getClient().getLocalPlayer())) {
                localUnits = true;
            }

            if (!clientgui.getClient().game.getOptions().booleanOption("pilot_advantages")) { //$NON-NLS-1$
                entity.getCrew().clearOptions(PilotOptions.LVL3_ADVANTAGES);
            }

            if (!clientgui.getClient().game.getOptions().booleanOption("manei_domini")) { //$NON-NLS-1$
                entity.getCrew().clearOptions(PilotOptions.MD_ADVANTAGES);
            }

            if (!clientgui.getClient().game.getOptions().booleanOption("stratops_quirks")) { //$NON-NLS-1$
                entity.clearQuirks();
            }

            // Handle the "Blind Drop" option.
            if (!entity.getOwner().equals(clientgui.getClient().getLocalPlayer()) && clientgui.getClient().game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                    && !clientgui.getClient().game.getOptions().booleanOption("real_blind_drop")) { //$NON-NLS-1$

                mekModel.addUnit(entity);
            } else if (entity.getOwner().equals(clientgui.getClient().getLocalPlayer()) || (!clientgui.getClient().game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                    && !clientgui.getClient().game.getOptions().booleanOption("real_blind_drop"))) { //$NON-NLS-1$
                mekModel.addUnit(entity);
            }
        }

        // Enable the "Save Unit List..." and "Delete All"
        // buttons if the local player has units.
        butSaveList.setEnabled(localUnits);
        butDeleteAll.setEnabled(localUnits);
    }

    public static String formatPilotCompact(Pilot pilot, boolean blindDrop) {

        String value = "";
        if (blindDrop) {
            value += Messages.getString("ChatLounge.Unknown");
        } else {
            value += pilot.getDesc();
        }
        value += " (" + pilot.getGunnery() + "/" + pilot.getPiloting() + ")";
        if (pilot.countOptions() > 0) {
            value += " (" + pilot.countOptions() + Messages.getString("ChatLounge.abilities") + ")";
        }

        return value;

    }

    public static String formatPilotHTML(Pilot pilot, boolean blindDrop) {

        int crewAdvCount = pilot.countOptions(PilotOptions.LVL3_ADVANTAGES);
        int implants = pilot.countOptions(PilotOptions.MD_ADVANTAGES);

        String value = "";
        if (blindDrop) {
            value += "<b>" + Messages.getString("ChatLounge.Unknown") + "</b><br>";
        } else {
            value += "<b>" + pilot.getDesc() + "</b><br>";
        }
        value += "" + pilot.getGunnery() + "/" + pilot.getPiloting();
        if (crewAdvCount > 0) {
            value += ", " + crewAdvCount + Messages.getString("ChatLounge.advs");
        }
        value += "<br>";
        if (implants > 0) {
            value += "<i>" + Messages.getString("ChatLounge.md") + "</i>, " + implants + Messages.getString("ChatLounge.implants") + "<br>";
        }

        return value;

    }

    public static String formatPilotTooltip(Pilot pilot, boolean command, boolean init, boolean tough) {

        String value = "<html>";
        value += "<b>" + pilot.getDesc() + "</b><br>";
        if(pilot.getNickname().length() > 0) {
        	value += "<i>" + pilot.getNickname() + "</i><br>";
        }
        if (pilot.getHits() > 0) {
            value += "<font color='red'>" + Messages.getString("ChatLounge.Hits") + pilot.getHits() + "</font><br>";
        }
        value += "" + pilot.getGunnery() + "/" + pilot.getPiloting() + "<br>";
        if (tough) {
            value += Messages.getString("ChatLounge.Tough") + pilot.getToughness() + "<br>";
        }
        if (command) {
            value += Messages.getString("ChatLounge.Command") + pilot.getCommandBonus() + "<br>";
        }
        if (init) {
            value += Messages.getString("ChatLounge.Initiative") + pilot.getInitBonus() + "<br>";
        }
        value += "<br>";
        for (Enumeration<IOptionGroup> advGroups = pilot.getOptions().getGroups(); advGroups.hasMoreElements();) {
            IOptionGroup advGroup = advGroups.nextElement();
            if (pilot.countOptions(advGroup.getKey()) > 0) {
                value += "<b>" + advGroup.getDisplayableName() + "</b><br>";
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    IOption adv = advs.nextElement();
                    if (adv.booleanValue()) {
                        value += "  " + adv.getDisplayableNameWithValue() + "<br>";
                    }
                }
            }
        }
        value += "</html>";
        return value;

    }

    public static String formatUnitTooltip(Entity entity) {

        String value = "<html>";
        value += "<b>" + entity.getChassis() + "  " + entity.getModel() + "</b><br>";
        value += "" + Math.round(entity.getWeight()) + Messages.getString("ChatLounge.Tons") + "<br>";
        value += "" + entity.getTotalArmor() + "/" + entity.getTotalOArmor() + Messages.getString("ChatLounge.armor") + "<br>";
        value += "" + entity.getTotalInternal() + "/" + entity.getTotalOInternal() + Messages.getString("ChatLounge.internal") + "<br>";
        value += "<br>";
        for (Enumeration<IOptionGroup> advGroups = entity.getQuirks().getGroups(); advGroups.hasMoreElements();) {
            IOptionGroup advGroup = advGroups.nextElement();
            if (entity.countQuirks(advGroup.getKey()) > 0) {
                value += "<b>" + advGroup.getDisplayableName() + "</b><br>";
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    IOption adv = advs.nextElement();
                    if (adv.booleanValue()) {
                        value += "  " + adv.getDisplayableNameWithValue() + "<br>";
                    }
                }
            }
        }
        for (Mounted weapon : entity.getWeaponList()) {
            for (Enumeration<IOptionGroup> advGroups = weapon.getQuirks().getGroups(); advGroups.hasMoreElements();) {
                IOptionGroup advGroup = advGroups.nextElement();
                if (entity.countQuirks(advGroup.getKey()) > 0) {
                    value += "<b>" + weapon.getDesc() + "</b><br>";
                    for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                        IOption adv = advs.nextElement();
                        if (adv.booleanValue()) {
                            value += "  " + adv.getDisplayableNameWithValue() + "<br>";
                        }
                    }
                }
            }
        }
        value += "</html>";
        return value;

    }

    public static String formatUnitCompact(Entity entity, boolean blindDrop) {

        String value = "";
        // Reset the tree strings.
        String strTreeSet = ""; //$NON-NLS-1$
        String strTreeView = ""; //$NON-NLS-1$

        if(blindDrop) {
            if (entity instanceof Infantry) {
                value += Messages.getString("ChatLounge.0"); //$NON-NLS-1$
            } else if (entity instanceof Protomech) {
                value += Messages.getString("ChatLounge.1"); //$NON-NLS-1$
            } else if (entity instanceof GunEmplacement) {
                value += Messages.getString("ChatLounge.2"); //$NON-NLS-1$
            } else {
                value += entity.getWeightClassName();
                if (entity instanceof Tank) {
                    value += Messages.getString("ChatLounge.6"); //$NON-NLS-1$
                }
            }
            return value;
        }

        // Set the tree strings based on C3 settings for the unit.
        if (entity.hasC3i()) {
            if (entity.calculateFreeC3Nodes() == 5) {
                strTreeSet = "**"; //$NON-NLS-1$
            }
            strTreeView = " (" + entity.getC3NetId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        } else if (entity.hasC3()) {
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    strTreeSet = "***"; //$NON-NLS-1$
                } else {
                    strTreeSet = "*"; //$NON-NLS-1$
                }
            } else if (!entity.C3MasterIs(entity)) {
                strTreeSet = ">"; //$NON-NLS-1$
                if ((entity.getC3Master().getC3Master() != null) && !entity.getC3Master().C3MasterIs(entity.getC3Master())) {
                    strTreeSet = ">>"; //$NON-NLS-1$
                }
                strTreeView = " -> " + entity.getC3Master().getDisplayName(); //$NON-NLS-1$
            }
        }

        value += strTreeSet + entity.getShortName() + strTreeView;

        if (entity.isOffBoard()) {
            value += " (" + Messages.getString("ChatLounge.deploysOffBoard") + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        } else if (entity.getDeployRound() > 0) {
            value += " (" + Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return value;
    }

    public static String formatUnitHTML(Entity entity, boolean blindDrop) {

        String value = "";

        if (blindDrop) {
            if (entity instanceof Infantry) {
                value += Messages.getString("ChatLounge.0"); //$NON-NLS-1$
            } else if (entity instanceof Protomech) {
                value += Messages.getString("ChatLounge.1"); //$NON-NLS-1$
            } else if (entity instanceof GunEmplacement) {
                value += Messages.getString("ChatLounge.2"); //$NON-NLS-1$
            } else {
                value += entity.getWeightClassName();
                if (entity instanceof Tank) {
                    value += Messages.getString("ChatLounge.6"); //$NON-NLS-1$
                }
            }
            value += "<br>";
        } else {
            String c3network = "";
            if (entity.hasC3i()) {
                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3network += Messages.getString("ChatLounge.C3iNone");
                } else {
                    c3network += c3network += Messages.getString("ChatLounge.C3iNetwork") + entity.getC3NetId();
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.C3Nodes", new Object[]
                            { entity.calculateFreeC3Nodes() });
                    }
                }
            } else if (entity.hasC3()) {
                if (entity.C3MasterIs(entity)) {
                    c3network += Messages.getString("ChatLounge.C3MM");
                    if (entity.calculateFreeC3MNodes() > 0) {
                        c3network += Messages.getString("ChatLounge.C3Nodes", new Object[]
                            { entity.calculateFreeC3Nodes() });
                    }
                } else if (!entity.hasC3S()) {
                    c3network += Messages.getString("ChatLounge.C3Master");
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.C3Nodes", new Object[]
                            { entity.calculateFreeC3Nodes() });
                    }
                    // an independent master might also be a slave to a company
                    // master
                    if (entity.getC3Master() != null) {
                        c3network += "<br>" + Messages.getString("ChatLounge.C3Slave") + entity.getC3Master().getDisplayName(); //$NON-NLS-1$
                    }
                } else if (entity.getC3Master() != null) {
                    c3network += Messages.getString("ChatLounge.C3Slave") + entity.getC3Master().getDisplayName(); //$NON-NLS-1$
                } else {
                    c3network += " not networked";
                }
            }

            int posQuirkCount = entity.countQuirks(Quirks.POS_QUIRKS);
            int negQuirkCount = entity.countQuirks(Quirks.NEG_QUIRKS);

            value += "<b>" + entity.getShortName() + "</b><br>";
            value += "" + Math.round(entity.getWeight()) + Messages.getString("ChatLounge.Tons") + "<br>";
            if (c3network.length() > 0) {
                value += c3network + "<br>";
            }
            if ((posQuirkCount > 0) | (negQuirkCount > 0)) {
                value += Messages.getString("ChatLounge.Quirks") + "+" + posQuirkCount + "/" + "-" + negQuirkCount + "<br>";
            }
        }
        if (entity.isOffBoard()) {
            value += Messages.getString("ChatLounge.deploysOffBoard"); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (entity.getDeployRound() > 0) {
            value += Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return value;
    }

    /**
     * This function is now deprecated and has been replaced by formatUnitHTML,
     * formatPilotHTML, formatUnitTooltip, and formatPilotTooltip. It is however
     * used by other programs so it remains.
     */
    public static String formatUnit(Entity entity, boolean blindDrop, boolean rpgSkills) {
        String value;

        // Reset the tree strings.
        String strTreeSet = ""; //$NON-NLS-1$
        String strTreeView = ""; //$NON-NLS-1$

        // Set the tree strings based on C3 settings for the unit.
        if (entity.hasC3i()) {
            if (entity.calculateFreeC3Nodes() == 5) {
                strTreeSet = "**"; //$NON-NLS-1$
            }
            strTreeView = " (" + entity.getC3NetId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        } else if (entity.hasC3()) {
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    strTreeSet = "***"; //$NON-NLS-1$
                } else {
                    strTreeSet = "*"; //$NON-NLS-1$
                }
            } else if (!entity.C3MasterIs(entity)) {
                strTreeSet = ">"; //$NON-NLS-1$
                if ((entity.getC3Master().getC3Master() != null) && !entity.getC3Master().C3MasterIs(entity.getC3Master())) {
                    strTreeSet = ">>"; //$NON-NLS-1$
                }
                strTreeView = " -> " + entity.getC3Master().getDisplayName(); //$NON-NLS-1$
            }
        }

        int crewAdvCount = entity.getCrew().countOptions(PilotOptions.LVL3_ADVANTAGES);
        boolean isManeiDomini = entity.getCrew().countOptions(PilotOptions.MD_ADVANTAGES) > 0;
        int posQuirkCount = entity.countQuirks(Quirks.POS_QUIRKS);
        int negQuirkCount = entity.countQuirks(Quirks.NEG_QUIRKS);

        String gunnery = Integer.toString(entity.getCrew().getGunnery());
        if (rpgSkills) {
            gunnery = entity.getCrew().getGunneryRPG();
        }

        if (blindDrop) {
            String unitClass;
            if (entity instanceof Infantry) {
                unitClass = Messages.getString("ChatLounge.0"); //$NON-NLS-1$
            } else if (entity instanceof Protomech) {
                unitClass = Messages.getString("ChatLounge.1"); //$NON-NLS-1$
            } else if (entity instanceof GunEmplacement) {
                unitClass = Messages.getString("ChatLounge.2"); //$NON-NLS-1$
            } else {
                unitClass = entity.getWeightClassName();
                if (entity instanceof Tank) {
                    unitClass += Messages.getString("ChatLounge.6"); //$NON-NLS-1$
                }
            }
            value = Messages.getString("ChatLounge.EntityListEntry1", new Object[] {//$NON-NLS-1$
                    entity.getOwner().getName(), gunnery, new Integer(entity.getCrew().getPiloting()), (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            (isManeiDomini ? Messages.getString("ChatLounge.md") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                            unitClass, (posQuirkCount > 0 ? " <" + posQuirkCount + Messages.getString("ChatLounge.pquirk") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            (negQuirkCount > 0 ? " <" + negQuirkCount + Messages.getString("ChatLounge.nquirk") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            ((entity.isOffBoard()) ? Messages.getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                            ((entity.getDeployRound() > 0) ? Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : "") }); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            value = strTreeSet + Messages.getString("ChatLounge.EntityListEntry2", new Object[] {//$NON-NLS-1$
                    entity.getDisplayName(), gunnery, new Integer(entity.getCrew().getPiloting()), (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            (isManeiDomini ? Messages.getString("ChatLounge.md") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                            (posQuirkCount > 0 ? " <" + posQuirkCount + Messages.getString("ChatLounge.pquirk") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            (negQuirkCount > 0 ? " <" + negQuirkCount + Messages.getString("ChatLounge.nquirk") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            new Integer(entity.calculateBattleValue()), strTreeView, ((entity.isOffBoard()) ? Messages.getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                            ((entity.getDeployRound() > 0) ? Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : ""), //$NON-NLS-1$ //$NON-NLS-2$
                            (entity.isDesignValid() ? "" : Messages.getString("ChatLounge.invalidDesign")) }); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return value;
    }

    /**
     * Refreshes the player info
     */
    private void refreshPlayerInfo() {
        playerModel.clearData();
        for (Enumeration<Player> i = clientgui.getClient().getPlayers(); i.hasMoreElements();) {
            final Player player = i.nextElement();
            if (player == null) {
                continue;
            }
            playerModel.addPlayer(player);
        }
    }

    private void refreshCamos() {
        Client c = getPlayerSelected();
        camoDialog.setPlayer(c.getLocalPlayer());
    }

    /**
     * Setup the team choice box
     */
    private void setupTeams() {
        choTeam.removeAllItems();
        for (int i = 0; i < Player.MAX_TEAMS; i++) {
            choTeam.addItem(Player.teamNames[i]);
        }
        if (clientgui.getClient().getLocalPlayer() != null) {
            choTeam.setSelectedIndex(clientgui.getClient().getLocalPlayer().getTeam());
        } else {
            choTeam.setSelectedIndex(0);
        }
    }

    /**
     * Highlight the team the player is playing on.
     */
    private void refreshTeams() {
        choTeam.setSelectedIndex(clientgui.getClient().getLocalPlayer().getTeam());
    }

    /**
     * Refreshes the done button. The label will say the opposite of the
     * player's "done" status, indicating that clicking it will reverse the
     * condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone.setText(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void refreshDoneButton() {
        refreshDoneButton(clientgui.getClient().getLocalPlayer().isDone());
    }

    /**
     * Change local player team.
     */
    private void changeTeam(int team) {
        Client c = getPlayerSelected();
        if ((c != null) && (c.getLocalPlayer().getTeam() != team)) {
            c.getLocalPlayer().setTeam(team);
            c.sendPlayerInfo();
        }
    }

    /**
     * Pop up the customize mech dialog
     */

    private void customizeMech() {
        if (tableEntities.getSelectedRow() == -1) {
            return;
        }
        customizeMech(mekModel.getEntityAt(tableEntities.getSelectedRow()));
    }

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
        Iterator<Entity> playerUnits = c.game.getPlayerEntities(c.getLocalPlayer(), false).iterator();
        while (playerUnits.hasNext()) {
            Entity unit = playerUnits.next();
            if (!entity.equals(unit) && entity.onSameC3NetworkAs(unit)) {
                c3members.add(unit);
            }
        }

        // display dialog
        CustomMechDialog cmd = new CustomMechDialog(clientgui, c, entity, editable);
        cmd.refreshOptions();
        cmd.refreshQuirks();
        cmd.setTitle(entity.getShortName());
        cmd.setVisible(true);
        if (editable && cmd.isOkay()) {
            // send changes
            c.sendUpdateEntity(entity);

            // Do we need to update the members of our C3 network?
            if (((c3master != null) && !c3master.equals(entity.getC3Master())) || ((c3master == null) && (entity.getC3Master() != null))) {
                for (Entity unit : c3members) {
                    c.sendUpdateEntity(unit);
                }
            }
        }
    }

    public void customizePlayer() {
        Client c = getPlayerSelected();
        if(null != c) {
            PlayerSettingsDialog psd = new PlayerSettingsDialog(clientgui, c);
            psd.setVisible(true);
        }
    }

    /**
     * Pop up the view mech dialog
     */
    private void mechReadout(Entity entity) {
        final JDialog dialog = new JDialog(clientgui.frame, Messages.getString("ChatLounge.quickView"), false); //$NON-NLS-1$
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
        JButton btn = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });

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
        /*
         * I don't want to set this anymore because this dialog can get quite
         * large dialog.setLocation(clientgui.frame.getLocation().x +
         * clientgui.frame.getSize().width / 2 - dialog.getSize().width / 2,
         * clientgui.frame.getLocation().y + clientgui.frame.getSize().height /
         * 5 - dialog.getSize().height / 2);
         */
        // TODO: this seems hacky but it does more or less get the window
        // dimension right
        // there must be a better way?
        dialog.setSize(mvp.getBestWidth(), mvp.getBestHeight() + 75);
        dialog.validate();
        dialog.setVisible(true);
    }

    /**
     * Pop up the dialog to load a mech
     */
    private void loadMech() {
        clientgui.getMechSelectorDialog().setVisible(true);
    }

    /*
     * private void loadCustomBA() {
     * clientgui.getCustomBADialog().setVisible(true); }
     */

    public void loadCustomFS() {
        String name = JOptionPane.showInputDialog(clientgui.frame, "Choose a squadron designation");
        if ((name == null) || (name.trim().length() == 0)) {
            name = "";
        }
        FighterSquadron fs = new FighterSquadron(name);
        fs.setOwner(clientgui.getClient().getLocalPlayer());
        clientgui.getClient().sendAddEntity(fs);
    }

    private void loadArmy() {
        clientgui.getRandomArmyDialog().setVisible(true);
    }

    public void loadRandomSkills() {
        clientgui.getRandomSkillDialog().setVisible(true);
    }

    /**
     * Changes all selected boards to be the specified board
     */
    private void changeMap(String board) {
        int[] selected = lisBoardsSelected.getSelectedIndices();
        for (final int newVar : selected) {
            String name = board;
            if (!MapSettings.BOARD_RANDOM.equals(name)
                    && !MapSettings.BOARD_SURPRISE.equals(name)
                    && chkRotateBoard.isSelected()) {
                name = Board.BOARD_REQUEST_ROTATION + name;
            }
            ((DefaultListModel) lisBoardsSelected.getModel()).setElementAt(
                    newVar + ": " + name, newVar); //$NON-NLS-1$
            mapSettings.getBoardsSelectedVector().set(newVar, name);
        }
        lisBoardsSelected.setSelectedIndices(selected);
        clientgui.getClient().sendMapSettings(mapSettings);
    }

    //
    // GameListener
    //
    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshDoneButton();
        clientgui.getClient().game.setupTeams();
        refreshPlayerInfo();
        refreshCamos();
        refreshEntities();
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (clientgui.getClient().game.getPhase() == IGame.Phase.PHASE_LOUNGE) {
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
        refreshEntities();
        refreshPlayerInfo();
    }

    /*
     * NOTE: On linux, this gets called even when programatically updating the
     * list box selected item. Do not let this go into an infinite loop. Do not
     * update the selected item (even indirectly, by sending player info) if it
     * is already selected.
     */
    public void itemStateChanged(ItemEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (ev.getSource().equals(choTeam)) {
            changeTeam(choTeam.getSelectedIndex());
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (ev.getSource().equals(butLoad)) {
            loadMech();
        } else if (ev.getSource().equals(butArmy)) {
            loadArmy();
        } else if (ev.getSource().equals(butSkills)) {
            loadRandomSkills();
            /*
             * } else if (ev.getSource().equals(butLoadCustomBA)) {
             * loadCustomBA();
             */
        } else if (ev.getSource() == butLoadCustomFS) {
            loadCustomFS();
        } else if (ev.getSource().equals(tableEntities)) {
            customizeMech();
        } else if (ev.getSource().equals(tablePlayers)) {
            customizePlayer();
        } else if (ev.getSource().equals(butDeleteAll)) {
            // Build a Vector of this player's entities.
            ArrayList<Entity> currentUnits = clientgui.getClient().game.getPlayerEntities(clientgui.getClient().getLocalPlayer(), false);

            // Walk through the vector, deleting the entities.
            Iterator<Entity> entities = currentUnits.iterator();
            while (entities.hasNext()) {
                final Entity entity = entities.next();
                clientgui.getClient().sendDeleteEntity(entity.getId());
            }
        } else if (ev.getSource().equals(butOptions)) {
            // Make sure the game options dialog is editable.
            if (!clientgui.getGameOptionsDialog().isEditable()) {
                clientgui.getGameOptionsDialog().setEditable(true);
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(clientgui.getClient().game.getOptions());
            clientgui.getGameOptionsDialog().setVisible(true);
        } else if (ev.getSource().equals(butCompact)) {
            if(butCompact.isSelected()) {
                tableEntities.setRowHeight(15);
            } else {
                tableEntities.setRowHeight(80);
            }
            refreshEntities();
        } else if (ev.getSource().equals(butChangeStart)) {
            clientgui.getStartingPositionDialog().update();
            Client c = getPlayerSelected();
            if (c == null) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBotOrPlayer")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            clientgui.getStartingPositionDialog().setClient(c);
            clientgui.getStartingPositionDialog().setVisible(true);
        } else if (ev.getSource().equals(butLoadList)) {
            // Allow the player to replace their current
            // list of entities with a list from a file.
            clientgui.loadListFile();
        } else if (ev.getSource().equals(butSaveList)) {
            // Allow the player to save their current
            // list of entities to a file.
            clientgui.saveListFile(clientgui.getClient().game.getPlayerEntities(clientgui.getClient().getLocalPlayer(), false));
        } else if (ev.getSource().equals(butAddBot)) {
            String name = "Bot" + tablePlayers.getModel().getRowCount(); //$NON-NLS-1$
            name = (String) JOptionPane.showInputDialog(clientgui.frame, Messages.getString("ChatLounge.Name"), Messages.getString("ChatLounge.ChooseBotName"), JOptionPane.QUESTION_MESSAGE, null, null, name);
            if (name == null) {
                return;
            }
            if ("".equals(name.trim())) {
                name = "Bot" + tablePlayers.getModel().getRowCount(); //$NON-NLS-1$
            }

            BotClient c = new TestBot(name, clientgui.getClient().getHost(), clientgui.getClient().getPort());
            c.game.addGameListener(new BotGUI(c));
            try {
                c.connect();
            } catch (Exception e) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertBot.title"), Messages.getString("ChatLounge.AlertBot.message")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            c.retrieveServerInfo();
            clientgui.getBots().put(name, c);
        } else if (ev.getSource().equals(butRemoveBot)) {
            Client c = getPlayerSelected();
            if ((c == null) || c.equals(clientgui.getClient())) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBo")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            c.die();
            clientgui.getBots().remove(c.getName());
        } else if (ev.getSource() == butConditions) {
            clientgui.getPlanetaryConditionsDialog().update(clientgui.getClient().game.getPlanetaryConditions());
            clientgui.getPlanetaryConditionsDialog().setVisible(true);
        } else if (ev.getSource() == butRandomMap) {
            randomMapDialog.setVisible(true);
        } else if (ev.getSource().equals(butChange)) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                changeMap((String) lisBoardsAvailable.getSelectedValue());
            }
        } else if (ev.getSource().equals(buttonBoardPreview)) {
            previewGameBoard();
        } else if (ev.getSource().equals(butMapSize) || ev.getSource().equals(butSpaceSize)) {
            MapDimensionsDialog mdd = new MapDimensionsDialog(clientgui);
            mdd.setVisible(true);
        } else if(ev.getSource().equals(chkRotateBoard) && (lisBoardsAvailable.getSelectedIndex() != -1)) {
            previewMapsheet();
        } else if (ev.getSource().equals(comboMapType)) {
            mapSettings.setMedium(comboMapType.getSelectedIndex());
            clientgui.getClient().sendMapSettings(mapSettings);
        }
        else if (ev.getSource().equals(chkIncludeGround)) {
            if(chkIncludeGround.isSelected()) {
                mapSettings.setMedium(comboMapType.getSelectedIndex());
            } else {
                mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                //set default size for space maps
                mapSettings.setBoardSize(50, 50);
                mapSettings.setMapSize(1, 1);
            }
            clientgui.getClient().sendMapDimensions(mapSettings);
        }
        else if (ev.getSource().equals(chkIncludeSpace)) {
            if(chkIncludeSpace.isSelected()) {
                mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                //set default size for space maps
                mapSettings.setBoardSize(50, 50);
                mapSettings.setMapSize(1, 1);
            } else {
                mapSettings.setMedium(comboMapType.getSelectedIndex());
            }
            clientgui.getClient().sendMapDimensions(mapSettings);
        }
    }

    public void mouseClicked(MouseEvent arg0) {
        if ((arg0.getClickCount() == 1) && arg0.getSource().equals(lisBoardsAvailable)) {
            previewMapsheet();
        }
        if ((arg0.getClickCount() == 2) && arg0.getSource().equals(lisBoardsAvailable)) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                changeMap((String) lisBoardsAvailable.getSelectedValue());
            }
        }
    }

    public void mouseEntered(MouseEvent arg0) {
        // ignore
    }

    public void mouseExited(MouseEvent arg0) {
        // ignore
    }

    public void mousePressed(MouseEvent arg0) {
        // ignore
    }

    public void mouseReleased(MouseEvent arg0) {
        //ignore
    }

    /**
     * Updates to show the map settings that have, presumably, just been sent by
     * the server.
     */
    public void updateMapSettings(MapSettings newSettings) {
        mapSettings = (MapSettings) newSettings.clone();
        refreshMapButtons();
        refreshMapChoice();
        refreshSpaceGround();
        refreshBoardsSelected();
        refreshBoardsAvailable();
    }

    @Override
    public void ready() {
        // enforce exclusive deployment zones in double blind
        if (clientgui.getClient().game.getOptions().booleanOption("double_blind") && clientgui.getClient().game.getOptions().booleanOption("exclusive_db_deployment")) {
            int i = clientgui.getClient().getLocalPlayer().getStartingPos();
            if (i == 0) {
                clientgui.doAlertDialog("Starting Position not allowed", "In Double Blind play, you cannot choose 'Any' as starting position.");
                return;
            }
            for (Enumeration<Player> e = clientgui.getClient().game.getPlayers(); e.hasMoreElements();) {
                Player player = e.nextElement();
                if (player.getStartingPos() == 0) {
                    continue;
                }
                // CTR and EDG don't overlap
                if (((player.getStartingPos() == 9) && (i == 10)) || ((player.getStartingPos() == 10) && (i == 9))) {
                    continue;
                }

                // check for overlapping starting directions
                if (((player.getStartingPos() == i) || (player.getStartingPos() + 1 == i) || (player.getStartingPos() - 1 == i)) && (player.getId() != clientgui.getClient().getLocalPlayer().getId())) {
                    clientgui.doAlertDialog("Must choose exclusive deployment zone", "When using double blind, each player needs to have an exclusive deployment zone.");
                    return;
                }
            }
        }

        boolean done = !clientgui.getClient().getLocalPlayer().isDone();
        clientgui.getClient().sendDone(done);
        refreshDoneButton(done);
        for (Client client2 : clientgui.getBots().values()) {
            client2.sendDone(done);
        }
    }

    Client getPlayerListSelected(JList l) {
        if ((l == null) || (l.getSelectedIndex() == -1)) {
            return clientgui.getClient();
        }
        String name = ((String) l.getSelectedValue()).substring(0, Math.max(0, ((String) l.getSelectedValue()).indexOf(" :"))); //$NON-NLS-1$
        BotClient c = (BotClient) clientgui.getBots().get(name);
        if ((c == null) && clientgui.getClient().getName().equals(name)) {
            return clientgui.getClient();
        }
        return c;
    }

    Client getPlayerSelected() {
        if ((tablePlayers == null) || (tablePlayers.getSelectedRow() == -1)) {
            return clientgui.getClient();
        }
        String name = (String) tablePlayers.getValueAt(tablePlayers.getSelectedRow(), 0);
        BotClient c = (BotClient) clientgui.getBots().get(name);
        if ((c == null) && clientgui.getClient().getName().equals(name)) {
            return clientgui.getClient();
        }
        return c;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        clientgui.getClient().game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }

    // TODO Is there a better solution?
    // This is required because the ChatLounge adds the listener to the
    // MechSummaryCache that must be removed explicitly.
    public void die() {
        MechSummaryCache.getInstance().removeListener(mechSummaryCacheListener);
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource().equals(butRemoveBot)) {
            butRemoveBot.setEnabled(false);
            Client c = getPlayerSelected();
            if (c == null) {

                tablePlayers.removeRowSelectionInterval(tablePlayers.getSelectedRow(), tablePlayers.getSelectedRow());
                return;
            }
            if (c instanceof BotClient) {
                butRemoveBot.setEnabled(true);
            }
            choTeam.setSelectedIndex(c.getLocalPlayer().getTeam());
        } else if (event.getSource().equals(tablePlayers.getSelectionModel())) {
            butRemoveBot.setEnabled(false);
            Client c = getPlayerSelected();
            if (c == null) {
                tablePlayers.removeRowSelectionInterval(tablePlayers.getSelectedRow(), tablePlayers.getSelectedRow());
                return;
            }
            if (c instanceof BotClient) {
                butRemoveBot.setEnabled(true);
            }
            refreshCamos();
            choTeam.setSelectedIndex(c.getLocalPlayer().getTeam());
        } else if (event.getSource().equals(lisBoardsAvailable)) {
            previewMapsheet();
        }
    }

    /**
     * A table model for displaying players
     */
    public class PlayerTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -1372393680232901923L;

        private static final int COL_PLAYER = 0;
        private static final int COL_START = 1;
        private static final int COL_TEAM = 2;
        private static final int COL_BV = 3;
        private static final int COL_TON = 4;
        private static final int COL_COST = 5;
        private static final int N_COL = 6;

        private ArrayList<Player> players;
        private ArrayList<Integer> bvs;
        private ArrayList<Integer> costs;
        private ArrayList<Float> tons;

        public PlayerTableModel() {
            players = new ArrayList<Player>();
            bvs = new ArrayList<Integer>();
            costs = new ArrayList<Integer>();
            tons = new ArrayList<Float>();
        }

        public int getRowCount() {
            return players.size();
        }

        public void clearData() {
            players = new ArrayList<Player>();
            bvs = new ArrayList<Integer>();
            costs = new ArrayList<Integer>();
            tons = new ArrayList<Float>();
        }

        public int getColumnCount() {
            return N_COL;
        }

        public void addPlayer(Player player) {
            players.add(player);
            int bv = 0;
            int cost = 0;
            float ton = 0;
            for (Enumeration<Entity> j = clientgui.getClient().getEntities(); j.hasMoreElements();) {
                Entity entity = j.nextElement();
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
            switch (column) {
                case (COL_PLAYER):
                    return Messages.getString("ChatLounge.colPlayer");
                case (COL_START):
                    return "Start";
                case (COL_TEAM):
                    return "Team";
                case (COL_TON):
                    return Messages.getString("ChatLounge.colTon");
                case (COL_BV):
                    return Messages.getString("ChatLounge.colBV");
                case (COL_COST):
                    return Messages.getString("ChatLounge.colCost");
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
            Player player = getPlayerAt(row);
            boolean blindDrop = !player.equals(clientgui.getClient().getLocalPlayer()) && clientgui.getClient().game.getOptions().booleanOption("real_blind_drop");
            if (col == COL_BV) {
                int bv = Math.round(bvs.get(row) * player.getForceSizeBVMod());
                if (blindDrop) {
                    bv = bv > 0 ? 9999 : 0;
                }
                return bv;
            } else if (col == COL_PLAYER) {
                return player.getName();
            } else if (col == COL_START) {
                return IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()];
            } else if (col == COL_TON) {
                float ton = tons.get(row);
                if (blindDrop) {
                    ton = ton > 0 ? 9999 : 0;
                }
                return ton;
            } else if (col == COL_COST) {
                int cost = costs.get(row);
                if (blindDrop) {
                    cost = cost > 0 ? 9999 : 0;
                }
                return cost;
            } else {
                return player.getTeam();
            }
        }

        public Player getPlayerAt(int row) {
            return players.get(row);
        }
    }

    public class PlayerTableMouseAdapter extends MouseInputAdapter implements ActionListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = tablePlayers.rowAtPoint(e.getPoint());
                Player player = playerModel.getPlayerAt(row);
                if (player != null) {
                    boolean isOwner = player.equals(clientgui.getClient().getLocalPlayer());
                    boolean isBot = clientgui.getBots().get(player.getName()) != null;
                    if ((isOwner || isBot)) {
                        customizePlayer();
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            JPopupMenu popup = new JPopupMenu();
            int row = tablePlayers.rowAtPoint(e.getPoint());
            Player player = playerModel.getPlayerAt(row);
            boolean isOwner = player.equals(clientgui.getClient().getLocalPlayer());
            boolean isBot = clientgui.getBots().get(player.getName()) != null;
            if (e.isPopupTrigger()) {
                JMenuItem menuItem = null;
                // JMenu menu = null;
                menuItem = new JMenuItem("Configure ...");
                menuItem.setActionCommand("CONFIGURE|" + row);
                menuItem.addActionListener(this);
                menuItem.setEnabled(isOwner || isBot);
                popup.add(menuItem);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        public void actionPerformed(ActionEvent action) {
            StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
            String command = st.nextToken();
            if (command.equalsIgnoreCase("CONFIGURE")) {
                customizePlayer();
            }
        }

    }

    /**
     * A table model for displaying units
     */
    public class MekTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 4819661751806908535L;

        private static final int COL_UNIT = 0;
        private static final int COL_PILOT = 1;
        private static final int COL_PLAYER = 2;
        private static final int COL_BV = 3;
        private static final int N_COL = 4;

        private ArrayList<Entity> data;

        public MekTableModel() {
            data = new ArrayList<Entity>();
        }

        public int getRowCount() {
            return data.size();
        }

        public void clearData() {
            data = new ArrayList<Entity>();
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return N_COL;
        }

        public void addUnit(Entity en) {
            data.add(en);
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case (COL_PILOT):
                    return Messages.getString("ChatLounge.colPilot");
                case (COL_UNIT):
                    return Messages.getString("ChatLounge.colUnit");
                case (COL_PLAYER):
                    return Messages.getString("ChatLounge.colPlayer");
                case (COL_BV):
                    return Messages.getString("ChatLounge.colBV");
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
            boolean compact = butCompact.isSelected();
            Entity entity = getEntityAt(row);
            boolean blindDrop = !entity.getOwner().equals(clientgui.getClient().getLocalPlayer()) && clientgui.getClient().game.getOptions().booleanOption("blind_drop");
            String value = "";
            if (col == COL_BV) {
                value += entity.calculateBattleValue();
            } else if (col == COL_PLAYER) {
                if(compact) {
                    value += entity.getOwner().getName();
                } else {
                    value += entity.getOwner().getName() + "<br>Team " + clientgui.getClient().game.getPlayer(entity.getOwnerId()).getTeam();
                }
            } else if (col == COL_PILOT) {
                if(compact) {
                    return formatPilotCompact(entity.crew, blindDrop);
                }
                return formatPilotHTML(entity.crew, blindDrop);
            } else {
                if(compact) {
                    return formatUnitCompact(entity, blindDrop);
                }
                return formatUnitHTML(entity, blindDrop);
            }
            return value;
        }

        public Entity getEntityAt(int row) {
            return data.get(row);
        }

        public MekTableModel.Renderer getRenderer() {
            return new MekTableModel.Renderer();
        }

        public class Renderer extends MekInfo implements TableCellRenderer {

            private static final long serialVersionUID = -9154596036677641620L;

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = this;
                setText(getValueAt(row, column).toString(), isSelected);
                Entity entity = getEntityAt(row);
                boolean isOwner = entity.getOwner().equals(clientgui.getClient().getLocalPlayer());
                boolean blindDrop = clientgui.getClient().game.getOptions().booleanOption("blind_drop");
                boolean compact = butCompact.isSelected();
                if (!isOwner && blindDrop) {
                    if (column == COL_UNIT) {
                        if(compact) {
                            clearImage();
                        } else {
                            Image image = getToolkit().getImage("data/images/misc/unknown_unit.gif"); //$NON-NLS-1$
                            image = image.getScaledInstance(-1, 72, Image.SCALE_DEFAULT);
                            setImage(image);
                        }
                    } else if (column == COL_PILOT) {
                        if(compact) {
                            clearImage();
                        } else {
                            Image image = getToolkit().getImage("data/images/portraits/default.gif"); //$NON-NLS-1$
                            image = image.getScaledInstance(-1, 50, Image.SCALE_DEFAULT);
                            setImage(image);
                        }
                    }
                } else {
                    if (column == COL_UNIT) {
                        if(compact) {
                            clearImage();
                        } else {
                            clientgui.loadPreviewImage(getLabel(), entity);
                        }
                        setToolTipText(formatUnitTooltip(entity));
                    } else if (column == COL_PILOT) {
                        if(compact) {
                            clearImage();
                        } else {
                            setPortrait(entity.crew);
                        }
                        setToolTipText(formatPilotTooltip(entity.crew,
                                clientgui.getClient().game.getOptions().booleanOption("command_init"),
                                clientgui.getClient().game.getOptions().booleanOption("individual_initiative"),
                                clientgui.getClient().game.getOptions().booleanOption("toughness")));
                    }
                }
                if (isSelected) {
                    c.setBackground(Color.DARK_GRAY);
                } else {
                    // tiger stripes
                    if (row % 2 == 0) {
                        c.setBackground(new Color(220, 220, 220));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }

            public void setPortrait(Pilot pilot) {

                String category = pilot.getPortraitCategory();
                String file = pilot.getPortraitFileName();

                // Return a null if the player has selected no portrait file.
                if ((null == category) || (null == file)) {
                    return;
                }

                if (Pilot.ROOT_PORTRAIT.equals(category)) {
                    category = "";
                }

                if (Pilot.PORTRAIT_NONE.equals(file)) {
                    file = "default.gif";
                }

                // Try to get the player's portrait file.
                Image portrait = null;
                try {
                    portrait = (Image) portraits.getItem(category, file);
                    if(null == portrait) {
                        //the image could not be found so switch to default one
                        pilot.setPortraitCategory(Pilot.ROOT_PORTRAIT);
                        category = "";
                        pilot.setPortraitFileName(Pilot.PORTRAIT_NONE);
                        file = "default.gif";
                        portrait = (Image) portraits.getItem(category, file);
                    }
                    // make sure no images are longer than 72 pixels
                    if (null != portrait) {
                        portrait = portrait.getScaledInstance(-1, 50, Image.SCALE_DEFAULT);
                        setImage(portrait);
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

        }
    }

    public class MekTableKeyAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            int row = tableEntities.getSelectedRow();
            if (row == -1) {
                return;
            }
            Entity entity = mekModel.getEntityAt(row);
            int code = e.getKeyCode();
            if ((code == KeyEvent.VK_DELETE) || (code == KeyEvent.VK_BACK_SPACE)) {
                e.consume();
                Client c = clientgui.getBots().get(entity.getOwner().getName());
                if (c == null) {
                    c = clientgui.getClient();
                }
                c.sendDeleteEntity(entity.getId());
            } else if (code == KeyEvent.VK_SPACE) {
                e.consume();
                mechReadout(entity);
            } else if (code == KeyEvent.VK_ENTER) {
                e.consume();
                customizeMech(entity);
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            int row = tableEntities.getSelectedRow();
            if (row == -1) {
                return;
            }
            Entity entity = mekModel.getEntityAt(row);
            char typed = e.getKeyChar();
            if (String.valueOf(typed).equals("v") || String.valueOf(typed).equals("V")) {
                e.consume();
                mechReadout(entity);
            } else if (String.valueOf(typed).equals("c") || String.valueOf(typed).equals("C")) {
                e.consume();
                customizeMech(entity);
            }
        }
    }

    public class MekTableMouseAdapter extends MouseInputAdapter implements ActionListener {

        public void actionPerformed(ActionEvent action) {
            StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
            String command = st.nextToken();
            int row = Integer.parseInt(st.nextToken());
            Entity entity = mekModel.getEntityAt(row);
            if (null == entity) {
                return;
            }
            if (command.equalsIgnoreCase("VIEW")) {
                mechReadout(entity);
            } else if (command.equalsIgnoreCase("CONFIGURE")) {
                customizeMech(entity);
            } else if (command.equalsIgnoreCase("DELETE")) {
                Client c = clientgui.getBots().get(entity.getOwner().getName());
                if (c == null) {
                    c = clientgui.getClient();
                }
                c.sendDeleteEntity(entity.getId());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = tableEntities.rowAtPoint(e.getPoint());
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
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            JPopupMenu popup = new JPopupMenu();
            int row = tableEntities.rowAtPoint(e.getPoint());
            Entity entity = mekModel.getEntityAt(row);
            boolean isOwner = entity.getOwner().equals(clientgui.getClient().getLocalPlayer());
            boolean isBot = clientgui.getBots().get(entity.getOwner().getName()) != null;
            boolean blindDrop = clientgui.getClient().game.getOptions().booleanOption("blind_drop");
            if (e.isPopupTrigger()) {
                JMenuItem menuItem = null;
                // JMenu menu = null;
                menuItem = new JMenuItem("View unit...");
                menuItem.setActionCommand("VIEW|" + row);
                menuItem.addActionListener(this);
                menuItem.setEnabled(isOwner || !blindDrop);
                popup.add(menuItem);
                menuItem = new JMenuItem("Configure unit...");
                menuItem.setActionCommand("CONFIGURE|" + row);
                menuItem.addActionListener(this);
                menuItem.setEnabled(isOwner || isBot);
                popup.add(menuItem);
                menuItem = new JMenuItem("Delete unit...");
                menuItem.setActionCommand("DELETE|" + row);
                menuItem.addActionListener(this);
                menuItem.setEnabled(isOwner || isBot);
                popup.add(menuItem);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

    }

    public class MekInfo extends JPanel {

        /**
         *
         */
        private static final long serialVersionUID = -7337823041775639463L;

        private JLabel lblImage;

        public MekInfo() {

            lblImage = new JLabel();

            setLayout(new java.awt.GridLayout(1, 0));
            add(lblImage);
            lblImage.setBorder(BorderFactory.createEmptyBorder());
        }

        public void setText(String s, boolean isSelected) {
            String color = "black";
            if (isSelected) {
                color = "white";
            }
            lblImage.setText("<html><font size='2' color='" + color + "'>" + s + "</font></html>");
        }

        public void clearImage() {
            lblImage.setIcon(null);
        }

        public void setImage(Image img) {
            lblImage.setIcon(new ImageIcon(img));
        }

        public JLabel getLabel() {
            return lblImage;
        }
    }
}
