/*
 * Copyright (C) 2000-2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.lobby;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.*;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.dialog.MMConfirmDialog;
import megamek.client.ui.swing.lobby.PlayerTable.PlayerTableModel;
import megamek.client.ui.swing.lobby.sorters.*;
import megamek.client.ui.swing.lobby.sorters.MekTableSorter.Sorting;
import megamek.client.ui.swing.minimap.Minimap;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.event.*;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.BoardUtilities;
import megamek.common.util.CollectionUtil;
import megamek.common.util.CrewSkillSummaryUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.utilities.BoardsTagger;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static megamek.client.ui.swing.lobby.LobbyUtility.*;
import static megamek.client.ui.swing.util.UIUtil.*;
import static megamek.common.util.CollectionUtil.theElement;
import static megamek.common.util.CollectionUtil.union;

public class ChatLounge extends AbstractPhaseDisplay implements  
        ListSelectionListener, IMapSettingsObserver, IPreferenceChangeListener {
    private static final long serialVersionUID = 1454736776730903786L;

    // UI display control values
    static final int MEKTABLE_ROWHEIGHT_COMPACT = 20;
    static final int MEKTABLE_ROWHEIGHT_FULL = 65;
    static final int MEKTREE_ROWHEIGHT_FULL = 40;
    private final static int TEAMOVERVIEW_BORDER = 45;
    private final static int MAP_POPUP_OFFSET = -2; // a slight offset so cursor sits inside popup

    private JTabbedPane panTabs = new JTabbedPane();
    private JPanel panUnits = new JPanel();
    private JPanel panMap = new JPanel();
    private JPanel panTeam = new JPanel();
    
    // Labels
    private JLabel lblMapSummary = new JLabel("");
    private JLabel lblGameYear = new JLabel("");
    private JLabel lblTechLevel = new JLabel("");

    // Game Setup
    private JButton butOptions = new JButton(Messages.getString("ChatLounge.butOptions"));
    private JToggleButton butGroundMap = new JToggleButton(Messages.getString("ChatLounge.butGroundMap"));
    private JToggleButton butLowAtmoMap = new JToggleButton(Messages.getString("ChatLounge.name.lowAltitudeMap"));
    private JToggleButton butHighAtmoMap = new JToggleButton(Messages.getString("ChatLounge.name.HighAltitudeMap"));
    private JToggleButton butSpaceMap = new JToggleButton(Messages.getString("ChatLounge.name.spaceMap"));
    private ButtonGroup grpMap = new ButtonGroup();

    /* Unit Configuration Panel */
    private FixedYPanel panUnitInfo = new FixedYPanel();
    private JButton butAdd = new JButton(Messages.getString("ChatLounge.butLoad"));
    private JButton butArmy = new JButton(Messages.getString("ChatLounge.butArmy"));
    private JButton butSkills = new JButton(Messages.getString("ChatLounge.butSkills"));
    private JButton butNames = new JButton(Messages.getString("ChatLounge.butNames"));
    private JButton butLoadList = new JButton(Messages.getString("ChatLounge.butLoadList"));
    private JButton butSaveList = new JButton(Messages.getString("ChatLounge.butSaveList"));

    /* Unit Table */
    private JTable mekTable;
    public JScrollPane scrMekTable;
    private MMToggleButton butCompact = new MMToggleButton(Messages.getString("ChatLounge.butCompact"));
    private MMToggleButton butShowUnitID = new MMToggleButton(Messages.getString("ChatLounge.butShowUnitID"));
    private JToggleButton butListView = new JToggleButton(Messages.getString("ChatLounge.butSortableView"));
    private JToggleButton butForceView = new JToggleButton(Messages.getString("ChatLounge.butForceView"));
    private JButton butCollapse = new JButton(Messages.getString("ChatLounge.butCollapse"));
    private JButton butExpand = new JButton(Messages.getString("ChatLounge.butExpand"));
    private MekTableModel mekModel;
    
    /* Force Tree */
    private MekTreeForceModel mekForceTreeModel;
    JTree mekForceTree;
    private MekForceTreeMouseAdapter mekForceTreeMouseListener = new MekForceTreeMouseAdapter();

    /* Player Configuration Panel */
    private FixedYPanel panPlayerInfo;
    private JComboBox<String> comboTeam = new JComboBox<>();
    private JButton butCamo = new JButton();
    private JButton butAddBot = new JButton(Messages.getString("ChatLounge.butAddBot"));
    private JButton butRemoveBot = new JButton(Messages.getString("ChatLounge.butRemoveBot"));
    private JButton butConfigPlayer = new JButton(Messages.getString("ChatLounge.butConfigPlayer"));
    private JButton butBotSettings = new JButton(Messages.getString("ChatLounge.butBotSettings"));

    private MekTableMouseAdapter mekTableMouseAdapter = new MekTableMouseAdapter();
    private PlayerTableModel playerModel = new PlayerTableModel();
    private PlayerTable tablePlayers = new PlayerTable(playerModel, this);
    private JScrollPane scrPlayers = new JScrollPane(tablePlayers);

    /* Map Settings Panel */
    private JLabel lblMapWidth = new JLabel(Messages.getString("ChatLounge.labMapWidth"));
    private JButton butMapGrowW = new JButton(Messages.getString("ChatLounge.butGrow"));
    private JButton butMapShrinkW = new JButton(Messages.getString("ChatLounge.butShrink"));
    private JTextField fldMapWidth = new JTextField(3);
    private JLabel lblMapHeight = new JLabel(Messages.getString("ChatLounge.labMapHeight"));
    private JButton butMapGrowH = new JButton(Messages.getString("ChatLounge.butGrow"));
    private JButton butMapShrinkH = new JButton(Messages.getString("ChatLounge.butShrink"));
    private JTextField fldMapHeight = new JTextField(3);
    private FixedYPanel panMapHeight = new FixedYPanel();
    private FixedYPanel panMapWidth = new FixedYPanel();
    
    private JLabel lblSpaceBoardWidth = new JLabel(Messages.getString("ChatLounge.labBoardWidth"));
    private JTextField fldSpaceBoardWidth = new JTextField(3);
    private JLabel lblSpaceBoardHeight = new JLabel(Messages.getString("ChatLounge.labBoardHeight"));
    private JTextField fldSpaceBoardHeight = new JTextField(3);
    private FixedYPanel panSpaceBoardHeight = new FixedYPanel();
    private FixedYPanel panSpaceBoardWidth = new FixedYPanel();
    
    private JLabel lblBoardSize = new JLabel(Messages.getString("ChatLounge.labBoardSize"));
    private JButton butHelp = new JButton(" " + Messages.getString("ChatLounge.butHelp") + " ");

    private JButton butConditions = new JButton(Messages.getString("ChatLounge.butConditions"));
    private JButton butRandomMap = new JButton(Messages.getString("BoardSelectionDialog.GeneratedMapSettings"));
    ArrayList<MapPreviewButton> mapButtons = new ArrayList<>(20);
    MapSettings mapSettings;
    private JPanel panGroundMap;
    @SuppressWarnings("rawtypes")
    private JComboBox<Comparable> comMapSizes;
    private JButton butBoardPreview = new JButton(Messages.getString("BoardSelectionDialog.ViewGameBoard"));
    private JPanel panMapButtons = new JPanel();
    private JLabel lblBoardsAvailable = new JLabel();
    private JList<String> lisBoardsAvailable;
    private JScrollPane scrBoardsAvailable;
    private JButton butSpaceSize = new JButton(Messages.getString("ChatLounge.MapSize"));
    private Set<BoardDimensions> mapSizes = new TreeSet<>();
    boolean resetAvailBoardSelection = false;
    boolean resetSelectedBoards = true;
    private ClientDialog boardPreviewW;
    private Game boardPreviewGame = new Game();
    private BoardView previewBV;
    Dimension currentMapButtonSize = new Dimension(0, 0);
    
    private ArrayList<String> invalidBoards = new ArrayList<>();
    private ArrayList<String> serverBoards = new ArrayList<>();
    
    private JSplitPane splGroundMap;
    private JLabel lblSearch = new JLabel(Messages.getString("ChatLounge.labSearch"));
    private JTextField fldSearch = new JTextField(10);
    private JButton butCancelSearch = new JButton(Messages.getString("ChatLounge.butCancelSearch"));

    private MekTableSorter activeSorter;
    private ArrayList<MekTableSorter> unitSorters = new ArrayList<>();
    private ArrayList<MekTableSorter> bvSorters = new ArrayList<>();
    
    private JButton butAddY = new JButton(Messages.getString("ChatLounge.butAdd"));
    private JButton butAddX = new JButton(Messages.getString("ChatLounge.butAdd"));
    private JButton butSaveMapSetup = new JButton(Messages.getString("ChatLounge.map.saveMapSetup") + " *");
    private JButton butLoadMapSetup = new JButton(Messages.getString("ChatLounge.map.loadMapSetup"));
    
    /* Team Overview Panel */
    private TeamOverviewPanel panTeamOverview;
    JButton butDetach = new JButton(Messages.getString("ChatLounge.butDetach"));
    private JSplitPane splitPaneMain;
    ClientDialog teamOverviewWindow;
        
    private ImageLoader loader;
    private Map<String, Image> baseImages = new HashMap<>();
    
    private MapListMouseAdapter mapListMouseListener = new MapListMouseAdapter(); 
    
    LobbyActions lobbyActions = new LobbyActions(this); 
    
    private Map<String, String> boardTags = new HashMap<>();
    
    LobbyKeyDispatcher lobbyKeyDispatcher = new LobbyKeyDispatcher(this);

    private static final String CL_KEY_FILEEXTENTION_BOARD = ".board";
    private static final String CL_KEY_FILEEXTENTION_XML = ".xml";
    private static final String CL_KEY_FILEPATH_MAPASSEMBLYHELP = "docs/Boards Stuff/MapAssemblyHelp.html";
    private static final String CL_KEY_FILEPATH_MAPSETUP = "/mapsetup";
    private static final String CL_KEY_NAMEHELPPANE = "helpPane";

    private static final String CL_ACTIONCOMMAND_LOADLIST =  "load_list";
    private static final String CL_ACTIONCOMMAND_SAVELIST =  "save_list";
    private static final String CL_ACTIONCOMMAND_LOADMECH = "load_mech";
    private static final String CL_ACTIONCOMMAND_ADDBOT = "add_bot";
    private static final String CL_ACTIONCOMMAND_REMOVEBOT = "remove_bot";
    private static final String CL_ACTIONCOMMAND_BOTCONFIG = "BOTCONFIG";
    private static final String CL_ACTIONCOMMAND_CONFIGURE = "CONFIGURE";
    private static final String CL_ACTIONCOMMAND_CAMO = "camo";

    private static final String MSG_MAPSETUPXMLFILES = Messages.getString("ChatLounge.map.SetupXMLfiles");

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** Creates a new chat lounge for the clientgui.getClient(). */
    public ChatLounge(ClientGUI clientgui) {
        super(clientgui, SkinSpecification.UIComponents.ChatLounge.getComp(),
                SkinSpecification.UIComponents.ChatLoungeDoneButton.getComp());

        setLayout(new BorderLayout());
        splitPaneMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneMain.setDividerSize(15);
        splitPaneMain.setResizeWeight(0.95);
        JPanel p = new JPanel(new BorderLayout());
        panTabs.add(Messages.getString("ChatLounge.name.selectUnits"), panUnits);
        panTabs.add(Messages.getString("ChatLounge.name.SelectMap"), panMap);
        panTabs.add(Messages.getString("ChatLounge.name.teamOverview"), panTeam);
        p.add(panTabs, BorderLayout.CENTER);
        splitPaneMain.setTopComponent(p);
        add(splitPaneMain);

        setupSorters();
        setupTeamOverview();
        setupPlayerConfig();
        refreshGameSettings();
        setupEntities();
        setupUnitConfig();
        setupUnitsPanel();
        setupMapPanel();
        refreshLabels();
        adaptToGUIScale();
        setupListeners();
    }

    public void setBottom(JComponent comp) {
        splitPaneMain.setBottomComponent(comp);
    }
    
    /** Sets up all the listeners that the lobby works with. */
    private void setupListeners() {
        // Make sure that no listeners are already registered from calling a refresh... method
        removeAllListeners();
        
        GUIP.addPreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
        MechSummaryCache.getInstance().addListener(mechSummaryCacheListener);
        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);
        
        loader = new ImageLoader();
        loader.execute();

        tablePlayers.getSelectionModel().addListSelectionListener(this);
        tablePlayers.addMouseListener(new PlayerTableMouseAdapter());
        
        lisBoardsAvailable.addListSelectionListener(this);
        lisBoardsAvailable.addMouseListener(mapListMouseListener);
        lisBoardsAvailable.addMouseMotionListener(mapListMouseListener);
        
        teamOverviewWindow.addWindowListener(teamOverviewWindowListener);
        
        mekTable.addMouseListener(mekTableMouseAdapter);
        mekTable.getTableHeader().addMouseListener(mekTableHeaderMouseListener);
        mekTable.addKeyListener(mekTableKeyListener);
        
        mekForceTree.addKeyListener(mekTreeKeyListener);
        mekForceTree.addMouseListener(mekForceTreeMouseListener);
        
        butAdd.addActionListener(lobbyListener);
        butAddBot.addActionListener(lobbyListener);
        butArmy.addActionListener(lobbyListener);
        butBoardPreview.addActionListener(lobbyListener);
        butBotSettings.addActionListener(lobbyListener);
        butCompact.addActionListener(lobbyListener);
        butConditions.addActionListener(lobbyListener);
        butConfigPlayer.addActionListener(lobbyListener);
        butLoadList.addActionListener(lobbyListener);
        butNames.addActionListener(lobbyListener);
        butOptions.addActionListener(lobbyListener);
        butRandomMap.addActionListener(lobbyListener);
        butRemoveBot.addActionListener(lobbyListener);
        butSaveList.addActionListener(lobbyListener);
        butShowUnitID.addActionListener(lobbyListener);
        butSkills.addActionListener(lobbyListener);
        butSpaceSize.addActionListener(lobbyListener);
        butCamo.addActionListener(camoListener);
        butAddX.addActionListener(lobbyListener);
        butAddY.addActionListener(lobbyListener);
        butMapGrowW.addActionListener(lobbyListener);
        butMapShrinkW.addActionListener(lobbyListener);
        butMapGrowH.addActionListener(lobbyListener);
        butMapShrinkH.addActionListener(lobbyListener);
        butGroundMap.addActionListener(lobbyListener);
        butLowAtmoMap.addActionListener(lobbyListener);
        butHighAtmoMap.addActionListener(lobbyListener);
        butSpaceMap.addActionListener(lobbyListener);
        butLoadMapSetup.addActionListener(lobbyListener);
        butSaveMapSetup.addActionListener(lobbyListener);
        butDetach.addActionListener(lobbyListener);
        butCancelSearch.addActionListener(lobbyListener);
        butHelp.addActionListener(lobbyListener);
        butListView.addActionListener(lobbyListener);
        butForceView.addActionListener(lobbyListener);
        butCollapse.addActionListener(lobbyListener);
        butExpand.addActionListener(lobbyListener);
        
        fldMapWidth.addActionListener(lobbyListener);
        fldMapHeight.addActionListener(lobbyListener);
        fldMapWidth.addFocusListener(focusListener);
        fldMapHeight.addFocusListener(focusListener);
        fldSpaceBoardWidth.addActionListener(lobbyListener);
        fldSpaceBoardHeight.addActionListener(lobbyListener);
        fldSpaceBoardWidth.addFocusListener(focusListener);
        fldSpaceBoardHeight.addFocusListener(focusListener);
        
        comboTeam.addActionListener(lobbyListener);
        
        KeyboardFocusManager kbfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kbfm.addKeyEventDispatcher(lobbyKeyDispatcher);
    }

    /** Applies changes to the board and map size when the textfields lose focus. */
    FocusListener focusListener = new FocusAdapter() {
        
        @Override
        public void focusLost(FocusEvent e) {
            if (e.getSource() == fldMapWidth) {
                setManualMapWidth();
            } else if (e.getSource() == fldMapHeight) {
                setManualMapHeight();
            } else if (e.getSource() == fldSpaceBoardWidth) {
                setManualBoardWidth();
            } else if (e.getSource() == fldSpaceBoardHeight) {
                setManualBoardHeight();
            } 
        }
    }; 
    
    /** Shows the camo chooser and sets the selected camo. */
    ActionListener camoListener = e -> {
        // Show the CamoChooser for the selected player
        if (getSelectedClient() == null) {
            return;
        }
        Player player = getSelectedClient().getLocalPlayer();
        CamoChooserDialog ccd = new CamoChooserDialog(clientgui.getFrame(), player.getCamouflage());

        // If the dialog was canceled or nothing selected, do nothing
        if (!ccd.showDialog().isConfirmed()) {
            return;
        }

        // Update the player from the camo selection
        player.setCamouflage(ccd.getSelectedItem());
        butCamo.setIcon(player.getCamouflage().getImageIcon());
        getSelectedClient().sendPlayerInfo();
    };
    
    
    private void setupTeamOverview() {
        panTeamOverview = new TeamOverviewPanel(clientgui);
        FixedYPanel panDetach = new FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        panDetach.add(butDetach);
        
        panTeam.setLayout(new BoxLayout(panTeam, BoxLayout.PAGE_AXIS));
        panTeam.add(panDetach);
        panTeam.add(panTeamOverview);
        
        // setup (but don't show) the detached team overview window
        teamOverviewWindow = new ClientDialog(clientgui.frame, Messages.getString("ChatLounge.name.teamOverview"), false);
        teamOverviewWindow.setSize(clientgui.frame.getWidth() / 2, clientgui.frame.getHeight() / 2);
    }
    
    /** Re-attaches the Team Overview panel to the tab when the detached window is closed. */
    WindowListener teamOverviewWindowListener = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            int i = panTabs.indexOfTab(Messages.getString("ChatLounge.name.teamOverview"));
            Component cp = panTabs.getComponentAt(i);
            if (cp instanceof JPanel) {
                ((JPanel) cp).add(panTeamOverview);
            }
            panTeamOverview.setDetached(false);
            butDetach.setEnabled(true);
            panTabs.repaint();
        }
    };
    
    /** Initializes the Mek Table sorting algorithms. */
    private void setupSorters() {
        unitSorters.add(new PlayerTransportIDSorter(clientgui));
        unitSorters.add(new IDSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new IDSorter(MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new NameSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new NameSorter(MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new TypeSorter());
        unitSorters.add(new PlayerTonnageSorter(clientgui, MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new PlayerTonnageSorter(clientgui, MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new PlayerUnitRoleSorter(clientgui, MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new PlayerUnitRoleSorter(clientgui, MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new TonnageSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new TonnageSorter(MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new C3IDSorter(clientgui));
        bvSorters.add(new PlayerBVSorter(clientgui, MekTableSorter.Sorting.ASCENDING));
        bvSorters.add(new PlayerBVSorter(clientgui, MekTableSorter.Sorting.DESCENDING));
        bvSorters.add(new BVSorter(MekTableSorter.Sorting.ASCENDING));
        bvSorters.add(new BVSorter(MekTableSorter.Sorting.DESCENDING));
        activeSorter = unitSorters.get(0);
    }

    /** Enables buttons to allow adding units when the MSC has finished loading. */
    private MechSummaryCache.Listener mechSummaryCacheListener = () -> {
        butAdd.setEnabled(true);
        butArmy.setEnabled(true);
        butLoadList.setEnabled(true);
    };

    /** Sets up the Mek Table and Mek Tree. */
    private void setupEntities() {
        mekModel = new MekTableModel(clientgui, this);
        mekTable = new MekTable(mekModel);
        mekTable.getTableHeader().setReorderingAllowed(false);
        mekTable.setIntercellSpacing(new Dimension(0, 0));
        mekTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        for (int i = 0; i < MekTableModel.N_COL; i++) {
            TableColumn column = mekTable.getColumnModel().getColumn(i);
            column.setCellRenderer(mekModel.getRenderer());
            setColumnWidth(column);
        }

        mekForceTreeModel = new MekTreeForceModel(this);
        mekForceTree = new JTree(mekForceTreeModel);
        mekForceTree.setRootVisible(false);
        mekForceTree.setDragEnabled(true);
        mekForceTree.setTransferHandler(new MekForceTreeTransferHandler(this, mekForceTreeModel));
        mekForceTree.setCellRenderer(new MekForceTreeRenderer(this));
        mekForceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        mekForceTree.setExpandsSelectedPaths(true);
        ToolTipManager.sharedInstance().registerComponent(mekForceTree);
        
        scrMekTable = new JScrollPane(mekTable);
        scrMekTable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /** Sets up the unit (add unit / add army) panel. */
    private void setupUnitConfig() {
        RandomNameGenerator.getInstance();
        RandomCallsignGenerator.getInstance();

        MechSummaryCache mechSummaryCache = MechSummaryCache.getInstance();
        boolean mscLoaded = mechSummaryCache.isInitialized();

        butLoadList.setActionCommand(CL_ACTIONCOMMAND_LOADLIST);
        butLoadList.setEnabled(mscLoaded);
        butSaveList.setActionCommand(CL_ACTIONCOMMAND_SAVELIST);
        butSaveList.setEnabled(false);
        butAdd.setEnabled(mscLoaded);
        butAdd.setActionCommand(CL_ACTIONCOMMAND_LOADMECH);
        butArmy.setEnabled(mscLoaded);

        panUnitInfo.setBorder(BorderFactory.createTitledBorder(Messages.getString("ChatLounge.name.unitSetup")));
        panUnitInfo.setLayout(new BoxLayout(panUnitInfo, BoxLayout.PAGE_AXIS));
        JPanel panUnitInfoAdd = new JPanel(new GridLayout(2, 1, 2, 2));
        panUnitInfoAdd.setBorder(new EmptyBorder(0, 0, 2, 1));
        panUnitInfoAdd.add(butAdd);
        panUnitInfoAdd.add(butArmy);

        JPanel panUnitInfoGrid = new JPanel(new GridLayout(2, 2, 2, 2));
        panUnitInfoGrid.add(butLoadList);
        panUnitInfoGrid.add(butSaveList);
        panUnitInfoGrid.add(butNames);
        
        panUnitInfo.add(panUnitInfoAdd);
        panUnitInfo.add(panUnitInfoGrid);
    }

    /** Sets up the player configuration (team, camo) panel with the player list. */
    private void setupPlayerConfig() {
        scrPlayers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        butAddBot.setActionCommand(CL_ACTIONCOMMAND_ADDBOT);
        butRemoveBot.setEnabled(false);
        butRemoveBot.setActionCommand(CL_ACTIONCOMMAND_REMOVEBOT);
        butBotSettings.setEnabled(false);
        butBotSettings.setActionCommand(CL_ACTIONCOMMAND_BOTCONFIG);
        butConfigPlayer.setEnabled(false);
        butConfigPlayer.setActionCommand(CL_ACTIONCOMMAND_CONFIGURE);
        setButUnitIDState();
        setupTeamCombo();
        butCamo.setActionCommand(CL_ACTIONCOMMAND_CAMO);
        refreshCamoButton();
        
        panPlayerInfo = new FixedYPanel(new GridLayout(1, 2, 2, 2));
        panPlayerInfo.setBorder(BorderFactory.createTitledBorder(Messages.getString("ChatLounge.name.playerSetup")));
        
        JPanel panPlayerInfoBts = new JPanel(new GridLayout(4, 1, 2, 2));
        panPlayerInfoBts.add(comboTeam);
        panPlayerInfoBts.add(butConfigPlayer);
        panPlayerInfoBts.add(butAddBot);
        panPlayerInfoBts.add(butRemoveBot);
        
        panPlayerInfo.add(panPlayerInfoBts);
        panPlayerInfo.add(butCamo);

        refreshPlayerTable();
    }

    /** Sets up the lobby main panel (units/players). */
    private void setupUnitsPanel() {
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(butListView);
        viewGroup.add(butForceView);
        butListView.setSelected(true);
        
        butCollapse.setEnabled(false);
        butExpand.setEnabled(false);
        
        lblGameYear.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        lblTechLevel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        butOptions.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        
        FixedXPanel leftSide = new FixedXPanel();
        leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.PAGE_AXIS));
        leftSide.add(Box.createVerticalStrut(scaleForGUI(20)));
        leftSide.add(butOptions);
        leftSide.add(lblGameYear);
        leftSide.add(lblTechLevel);
        leftSide.add(Box.createVerticalStrut(scaleForGUI(15)));
        leftSide.add(panUnitInfo);
        leftSide.add(Box.createVerticalStrut(scaleForGUI(5)));
        leftSide.add(panPlayerInfo);
        leftSide.add(Box.createVerticalStrut(scaleForGUI(5)));
        leftSide.add(scrPlayers);
        
        JPanel topRight = new FixedYPanel();
        topRight.add(butListView);
        topRight.add(butForceView);
        topRight.add(Box.createHorizontalStrut(30));
        topRight.add(butCompact);
        topRight.add(butShowUnitID);
        topRight.add(Box.createHorizontalStrut(30));
        topRight.add(butCollapse);
        topRight.add(butExpand);
        
        JPanel rightSide = new JPanel();
        rightSide.setLayout(new BoxLayout(rightSide, BoxLayout.PAGE_AXIS));
        rightSide.add(topRight);
        rightSide.add(scrMekTable);
        
        panUnits.setLayout(new BoxLayout(panUnits, BoxLayout.LINE_AXIS));
        panUnits.add(leftSide);
        panUnits.add(rightSide);
    }

    private void setupMapPanel() {
        mapSettings = MapSettings.getInstance(clientgui.getClient().getMapSettings());
        setupMapAssembly();
        refreshMapUI();

        panMap.setLayout(new BoxLayout(panMap, BoxLayout.PAGE_AXIS));
        
        // Ground, Atmo, Space Map Buttons
        FixedYPanel panMapType = new FixedYPanel();
        panMapType.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        panMapType.add(butGroundMap);
        panMapType.add(butLowAtmoMap);
        panMapType.add(butSpaceMap);
        grpMap.add(butGroundMap);
        grpMap.add(butLowAtmoMap);
        grpMap.add(butHighAtmoMap);
        grpMap.add(butSpaceMap);
        
        // Planetary Conditions and Random Map Settings buttons
        FixedYPanel panSettings = new FixedYPanel();
        panSettings.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        panSettings.add(butConditions);
        panSettings.add(butRandomMap);

        FixedYPanel panTopRows = new FixedYPanel();
        panTopRows.setLayout(new BoxLayout(panTopRows, BoxLayout.PAGE_AXIS));
        panTopRows.add(panMapType);
        panTopRows.add(panSettings);
        
        JPanel panHelp = new JPanel(new GridLayout(1, 1));
        panHelp.add(butHelp);
        
        FixedYPanel panTopRowsHelp = new FixedYPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));
        panTopRowsHelp.add(panTopRows);
        panTopRowsHelp.add(panHelp);
        panMap.add(panTopRowsHelp);
        
        // Main part: Map Assembly
        panMap.add(panGroundMap);

    }

    /**
     * Sets up the ground map selection panel
     */
    @SuppressWarnings("rawtypes")
    private void setupMapAssembly() {
        panGroundMap = new JPanel(new GridLayout(1, 1));
        panGroundMap.setBorder(new EmptyBorder(20, 10, 10, 10));

        panMapButtons.setLayout(new BoxLayout(panMapButtons, BoxLayout.PAGE_AXIS));
        // Resize the preview buttons when the panel is resized
        panMapButtons.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateMapButtons();
            }
        });
        
        panMapWidth.add(lblMapWidth);
        panMapWidth.add(butMapShrinkW);
        panMapWidth.add(fldMapWidth);
        panMapWidth.add(butMapGrowW);
        
        panMapHeight.add(lblMapHeight);
        panMapHeight.add(butMapShrinkH);
        panMapHeight.add(fldMapHeight);
        panMapHeight.add(butMapGrowH);
        
        panSpaceBoardWidth.add(lblSpaceBoardWidth);
        panSpaceBoardWidth.add(fldSpaceBoardWidth);
        panSpaceBoardWidth.setVisible(false);
        
        panSpaceBoardHeight.add(lblSpaceBoardHeight);
        panSpaceBoardHeight.add(fldSpaceBoardHeight);
        panSpaceBoardHeight.setVisible(false);
        
        FixedYPanel bottomPanel = new FixedYPanel();
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        bottomPanel.add(butBoardPreview);
        bottomPanel.add(butSaveMapSetup);
        bottomPanel.add(butLoadMapSetup);

        butBoardPreview.setToolTipText(Messages.getString("BoardSelectionDialog.ViewGameBoardTooltip"));

        // The left side panel including the game map preview
        JPanel panMapPreview = new JPanel();
        panMapPreview.setLayout(new BoxLayout(panMapPreview, BoxLayout.PAGE_AXIS));
        
        panMapPreview.add(panMapWidth);
        panMapPreview.add(panMapHeight);
        panMapPreview.add(panSpaceBoardWidth);
        panMapPreview.add(panSpaceBoardHeight);
        panMapPreview.add(panMapButtons);
        panMapPreview.add(bottomPanel);
        
        // The right side panel including the list of available boards
        comMapSizes = new JComboBox<>();
        refreshMapSizes();

        lisBoardsAvailable = new JList<>(new DefaultListModel<>());
        lisBoardsAvailable.setCellRenderer(new BoardNameRenderer());
        lisBoardsAvailable.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        lisBoardsAvailable.setVisibleRowCount(-1);
        lisBoardsAvailable.setDragEnabled(true);
        lisBoardsAvailable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        scrBoardsAvailable = new JScrollPane(lisBoardsAvailable);
        refreshBoardsAvailable();
        
        JPanel panAvail = new JPanel();
        panAvail.setLayout(new BoxLayout(panAvail, BoxLayout.PAGE_AXIS));
        panAvail.setBorder(new EmptyBorder(0, 20, 0, 0));
        panAvail.add(setupAvailTopPanel());
        panAvail.add(scrBoardsAvailable);
        
        // The splitpane holding the left and right side panels
        splGroundMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panMapPreview, panAvail);
        splGroundMap.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splGroundMap.setDividerLocation(getDividerLocation());
            }
            
            @Override
            public void componentShown(ComponentEvent e) {
                splGroundMap.setDividerLocation(getDividerLocation());
            }
        });
        panGroundMap.add(splGroundMap);

        // setup the board preview window.
        boardPreviewW = new ClientDialog(clientgui.frame,
                Messages.getString("BoardSelectionDialog.ViewGameBoard"), false);
        boardPreviewW.setLocationRelativeTo(clientgui.frame);

        try {
            previewBV = new BoardView(boardPreviewGame, null, null);
            previewBV.setDisplayInvalidHexInfo(false);
            previewBV.setUseLOSTool(false);
            boardPreviewW.add(previewBV.getComponent(true));
            boardPreviewW.setSize(clientgui.frame.getWidth() / 2, clientgui.frame.getHeight() / 2);
            // Most boards will be far too large on the standard zoom
            previewBV.zoomOut();
            previewBV.zoomOut();
            previewBV.zoomOut();
            previewBV.zoomOut();
            boardPreviewW.center();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, Messages.getString("BoardEditor.CouldntInitialize") + e,
                    Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE);
        }
        refreshMapButtons();
    }
    
    /** 
     *  Sets up and returns the panel above the available boards list 
     *  containing the search bar and the map size chooser.  
     */
    private JPanel setupAvailTopPanel() {
        FixedYPanel result = new FixedYPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        result.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        fldSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSearch(fldSearch.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSearch(fldSearch.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSearch(fldSearch.getText());
            }
        });
        
        result.add(lblBoardSize);
        result.add(comMapSizes);
        result.add(new JLabel("    "));
        result.add(lblSearch);
        result.add(fldSearch);
        result.add(butCancelSearch);

        return result;
    }
    
    /** 
     * Reacts to changes in the available boards search field, showing matching boards
     * for the search string when it has at least 3 characters
     * and reverting to all boards when the search string is empty.
     */
    private void updateSearch(String searchString) {
        if (searchString.isEmpty()) {
            refreshBoardsAvailable();
        } else if (searchString.length() > 2) {
            refreshBoardsAvailable(getSearchedItems(searchString));
        }
    }
    
    /** 
     * Returns the available boards that match the given search string
     * (path or file name contains the search string.) 
     * The search string is split at ";" and search results for the tokens
     * are ANDed.
     */
    protected List<String> getSearchedItems(String searchString) {
        String lowerCaseSearchString = searchString.toLowerCase();
        String[] searchStrings = lowerCaseSearchString.split(";");
        List<String> result = mapSettings.getBoardsAvailableVector();
        for (String token : searchStrings) {
            List<String> byFilename = mapSettings.getBoardsAvailableVector().stream()
                    .filter(b -> b.toLowerCase().contains(token) && isBoardFile(b))
                    .collect(Collectors.toList());
            List<String> byTags = boardTags.entrySet().stream()
                    .filter(e -> e.getValue().contains(token))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            List<String> tokenResult = CollectionUtil.union(byFilename, byTags);
            result = result.stream().filter(tokenResult::contains).collect(toList());
        }
        return result;
    }
    
    /** 
     * Returns a suitable divider location for the splitpane that contains
     * the available boards list and the map preview. The divider location
     * gives between 30% and 50% of space to the map preview depending
     * on the width of the game map.
     */
    private double getDividerLocation() {
        double base = 0.3;
        int width = mapSettings.getBoardWidth() * mapSettings.getMapWidth();
        int height = mapSettings.getBoardHeight() * mapSettings.getMapHeight();
        int wAspect = Math.max(1, width / height + 1);
        return Math.min(base + wAspect * 0.05, 0.5);
    }

    /** Updates the ground map type chooser (ground/atmosphere map). */
    private void refreshMapChoice() {
        // refresh UI possibly from a server update
        JToggleButton button = butGroundMap;
        if (mapSettings.getMedium() == MapSettings.MEDIUM_ATMOSPHERE) {
            button = butLowAtmoMap;
        } else if (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            button = butSpaceMap;
        }
        
        if (!button.isSelected()) {
            button.removeActionListener(lobbyListener);
            button.setSelected(true);
            button.addActionListener(lobbyListener);
        }
    }
    
    /** Updates the list of available map sizes. */
    private void refreshMapSizes() {
        int oldSelection = comMapSizes.getSelectedIndex();
        mapSizes = clientgui.getClient().getAvailableMapSizes();
        comMapSizes.removeActionListener(lobbyListener);
        comMapSizes.removeAllItems();
        for (BoardDimensions size : mapSizes) {
            comMapSizes.addItem(size);
        }
        comMapSizes.addItem(Messages.getString("ChatLounge.CustomMapSize"));
        comMapSizes.setSelectedIndex(oldSelection != -1 ? oldSelection : 0);
        comMapSizes.addActionListener(lobbyListener);
    }

    /**
     * Refreshes the map assembly UI from the current map settings. Does NOT trigger further
     * changes or result in packets to the server. 
     */
    private void refreshMapUI() {
        boolean inSpace = mapSettings.getMedium() == MapSettings.MEDIUM_SPACE;
        boolean onGround = mapSettings.getMedium() == MapSettings.MEDIUM_GROUND;
        boolean customSize = Messages.getString("ChatLounge.CustomMapSize").equals(comMapSizes.getSelectedItem());
        lisBoardsAvailable.setEnabled(!inSpace);
        mapIcons.clear();
        butConditions.setEnabled(!inSpace);
        fldSearch.setEnabled(!inSpace);
        butRandomMap.setEnabled(!inSpace);
        panMapHeight.setVisible(!inSpace);
        panMapWidth.setVisible(!inSpace);
        panSpaceBoardWidth.setVisible(inSpace || customSize);
        panSpaceBoardHeight.setVisible(inSpace || customSize);
        comMapSizes.setEnabled(!inSpace);
        lblSearch.setEnabled(!inSpace);
        lblBoardSize.setEnabled(!inSpace);
        butSaveMapSetup.setEnabled(!inSpace);
        butLoadMapSetup.setEnabled(!inSpace);
        butMapShrinkW.setEnabled(mapSettings.getMapWidth() > 1);
        butMapShrinkH.setEnabled(mapSettings.getMapHeight() > 1);
        
        butGroundMap.removeActionListener(lobbyListener);
        butLowAtmoMap.removeActionListener(lobbyListener);
        butHighAtmoMap.removeActionListener(lobbyListener);
        butSpaceMap.removeActionListener(lobbyListener);
        if (onGround) {
            butGroundMap.setSelected(true);
        } else if (inSpace) {
            butSpaceMap.setSelected(true);
        } else {
            butLowAtmoMap.setSelected(true);
        }
        butGroundMap.addActionListener(lobbyListener);
        butLowAtmoMap.addActionListener(lobbyListener);
        butHighAtmoMap.addActionListener(lobbyListener);
        butSpaceMap.addActionListener(lobbyListener);
        
        fldMapWidth.removeActionListener(lobbyListener);
        fldMapHeight.removeActionListener(lobbyListener);
        fldSpaceBoardWidth.removeActionListener(lobbyListener);
        fldSpaceBoardHeight.removeActionListener(lobbyListener);
        fldMapWidth.setText(Integer.toString(mapSettings.getMapWidth()));
        fldMapHeight.setText(Integer.toString(mapSettings.getMapHeight()));
        fldSpaceBoardWidth.setText(Integer.toString(mapSettings.getBoardWidth()));
        fldSpaceBoardHeight.setText(Integer.toString(mapSettings.getBoardHeight()));
        fldMapWidth.addActionListener(lobbyListener);
        fldMapHeight.addActionListener(lobbyListener);
        fldSpaceBoardWidth.addActionListener(lobbyListener);
        fldSpaceBoardHeight.addActionListener(lobbyListener);
    }

    /** 
     * Refreshes the list of available boards with all available boards plus
     * GENERATED. Useful for first setup, when the server transmits new
     * map settings and when the text search field is empty.
     */
    private void refreshBoardsAvailable() {
        if (!lisBoardsAvailable.isEnabled()) {
            return;
        }
        lisBoardsAvailable.setFixedCellHeight(-1);
        lisBoardsAvailable.setFixedCellWidth(-1);
        List<String> availBoards = new ArrayList<>(); 
        availBoards.add(MapSettings.BOARD_GENERATED);
        availBoards.addAll(mapSettings.getBoardsAvailableVector());
        refreshBoardTags();
        refreshBoardsAvailable(availBoards);
    }
    
    private void refreshBoardTags() {
        boardTags.clear();
        for (String boardName : mapSettings.getBoardsAvailableVector()) {
            File boardFile = new MegaMekFile(Configuration.boardsDir(), boardName + CL_KEY_FILEEXTENTION_BOARD).getFile();
            Set<String> tags = Board.getTags(boardFile);
            boardTags.put(boardName, String.join("||", tags).toLowerCase());
        }
    }
    
    /** 
     * Refreshes the list of available maps with the given list of boards. 
     */
    private void refreshBoardsAvailable(List<String> boardList) {
        lisBoardsAvailable.removeListSelectionListener(this);
        // Replace the data model (adding the elements one by one to the existing model
        // in Java 8 style is sluggish because of event firing)
        DefaultListModel<String> newModel = new DefaultListModel<>();
        for (String s: boardList) {
            newModel.addElement(s);
        }
        lisBoardsAvailable.setModel(newModel);
        lisBoardsAvailable.clearSelection();
        lisBoardsAvailable.addListSelectionListener(this);
    }
    
    public boolean isMultipleBoards() {
        return mapSettings.getMapHeight() * mapSettings.getMapWidth() > 1;
    }
    
    MapSettings oldMapSettings = MapSettings.getInstance();

    /**
     * Fills the Map Buttons scroll pane twith the appropriate amount of buttons
     * in the appropriate layout
     */
    private void refreshMapButtons() {
        panMapButtons.removeAll();
        panMapButtons.setVisible(false);
        panMapButtons.add(Box.createVerticalGlue());
        Dimension buttonSize = null;

        // If buttons are unused, remove their image so that they update when they're used once more
        if (mapSettings.getMapHeight() * mapSettings.getMapWidth() < mapButtons.size()) {
            for (MapPreviewButton button: mapButtons.subList(mapSettings.getMapHeight() * mapSettings.getMapWidth(), mapButtons.size())) {
                button.reset();
            }
        }

        // Add new map preview buttons if the map has grown
        while (mapSettings.getMapHeight() * mapSettings.getMapWidth() > mapButtons.size()) {
            mapButtons.add(new MapPreviewButton(this));
        }

        // Re-add the buttons to the panel and update them as necessary
        for (int i = 0; i < mapSettings.getMapHeight(); i++) {
            JPanel row = new FixedYPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            panMapButtons.add(row);
            for (int j = 0; j < mapSettings.getMapWidth(); j++) {
                int index = i * mapSettings.getMapWidth() + j;
                MapPreviewButton button = mapButtons.get(index);
                button.setIndex(index);
                row.add(button);

                // Update the board base image if it's generated and the settings have changed
                // or the board name has changed
                String boardName = mapSettings.getBoardsSelectedVector().get(index);
                if (boardName == null) {
                    continue;
                }
                if (!button.getBoard().equals(boardName) 
                        || oldMapSettings.getMedium() != mapSettings.getMedium()
                        || (!mapSettings.equalMapGenParameters(oldMapSettings) 
                                && mapSettings.getMapWidth() == oldMapSettings.getMapWidth()
                                && mapSettings.getMapHeight() == oldMapSettings.getMapHeight())) {
                    Board buttonBoard;
                    Image image;
                    // Generated and space boards use a generated example
                    if (boardName.startsWith(MapSettings.BOARD_GENERATED) 
                            || (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE)) {
                        buttonBoard = BoardUtilities.generateRandom(mapSettings);
                        image = Minimap.getMinimapImageMaxZoom(buttonBoard);
                    } else { 
                        String boardForImage = boardName;
                        // For a surprise board, just use the first board as example
                        if (boardName.startsWith(MapSettings.BOARD_SURPRISE)) {
                            boardForImage = extractSurpriseMaps(boardName).get(0);
                        }
                        
                        boolean rotateBoard = false;
                        // for a rotation board, set a flag (when appropriate) and fix the name
                        if (boardForImage.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                            // only rotate boards with an even width
                            if ((mapSettings.getBoardWidth() % 2) == 0) {
                                rotateBoard = true;
                            }
                            
                            boardForImage = boardForImage.replace(Board.BOARD_REQUEST_ROTATION, "");
                        }
                        
                        File boardFile = new MegaMekFile(Configuration.boardsDir(), boardForImage + CL_KEY_FILEEXTENTION_BOARD).getFile();
                        if (boardFile.exists()) {
                            buttonBoard = new Board(16, 17);
                            buttonBoard.load(new MegaMekFile(Configuration.boardsDir(), boardForImage + CL_KEY_FILEEXTENTION_BOARD).getFile());
                            StringBuffer errs = new StringBuffer();
                            try (InputStream is = new FileInputStream(new MegaMekFile(Configuration.boardsDir(), boardForImage + CL_KEY_FILEEXTENTION_BOARD).getFile())) {
                                buttonBoard.load(is, errs, true);
                                BoardUtilities.flip(buttonBoard, rotateBoard, rotateBoard);
                            } catch (IOException ex) {
                                buttonBoard = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
                            }
                            image = Minimap.getMinimapImageMaxZoom(buttonBoard);
                        } else {
                            buttonBoard = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
                            BufferedImage emptyBoardMap = Minimap.getMinimapImageMaxZoom(buttonBoard);
                            markServerSideBoard(emptyBoardMap);
                            image = emptyBoardMap;
                        }
                    }
                    button.setImage(image, boardName);
                    buttonSize = optMapButtonSize(image);
                }
                button.scheduleRescale();
            }
        }
        oldMapSettings = MapSettings.getInstance(mapSettings);
        
        if (buttonSize != null) {
            for (MapPreviewButton button: mapButtons) {
                button.setPreviewSize(buttonSize);
            }
        }
        splGroundMap.setDividerLocation(getDividerLocation());

        panMapButtons.add(Box.createVerticalGlue());
        panMapButtons.setVisible(true);

        lblBoardsAvailable.setText(mapSettings.getBoardWidth() + "x" + mapSettings.getBoardHeight() + " "
                + Messages.getString("BoardSelectionDialog.mapsAvailable"));
        comMapSizes.removeActionListener(lobbyListener);
        int items = comMapSizes.getItemCount();

        boolean mapSizeSelected = false;
        for (int i = 0; i < (items - 1); i++) {
            BoardDimensions size = (BoardDimensions) comMapSizes.getItemAt(i);

            if ((size.width() == mapSettings.getBoardWidth()) && (size.height() == mapSettings.getBoardHeight())) {
                comMapSizes.setSelectedIndex(i);
                mapSizeSelected = true;
            }
        }
        // If we didn't select a size, select the last item: 'Custom Size'
        if (!mapSizeSelected) {
            comMapSizes.setSelectedIndex(items - 1);
        }
        comMapSizes.addActionListener(lobbyListener);

    }
    
    private void markServerSideBoard(BufferedImage image) {
        Graphics g = image.getGraphics();
        setHighQualityRendering(g);
        int w = image.getWidth();
        int h = image.getHeight();
        String text = Messages.getString("ChatLounge.board.serverSide");
        int fontSize = Math.min(w / 10, UIUtil.scaleForGUI(16));
        g.setFont(new Font(MMConstants.FONT_DIALOG, Font.ITALIC, fontSize));
        FontMetrics fm = g.getFontMetrics(g.getFont());
        int cx = (w - fm.stringWidth(text)) / 2;
        int cy = h / 10 + fm.getAscent();
        g.setColor(GUIP.getWarningColor());
        g.drawString(text, cx, cy);
        g.dispose();
    }

    public void previewGameBoard() {
        Board newBoard = getPossibleGameBoard(false);
        boardPreviewGame.setBoard(newBoard);
        boardPreviewW.setVisible(true);
    }
    
    /** 
     * Returns the game map as it is currently set in the map settings tab.
     * When onlyFixedBoards is true, all Generated and Surprise boards are 
     * replaced by empty boards, otherwise they are filled with a generated or
     * a choice of the surprise maps.
     */
    public Board getPossibleGameBoard(boolean onlyFixedBoards) {
        mapSettings.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        Board[] sheetBoards = new Board[mapSettings.getMapWidth() * mapSettings.getMapHeight()];
        List<Boolean> rotateBoard = new ArrayList<>();
        for (int i = 0; i < (mapSettings.getMapWidth() * mapSettings.getMapHeight()); i++) {
            sheetBoards[i] = new Board();
            
            String name = mapSettings.getBoardsSelectedVector().get(i);
            if ((name.startsWith(MapSettings.BOARD_GENERATED) || name.startsWith(MapSettings.BOARD_SURPRISE))
                    && onlyFixedBoards) {
                sheetBoards[i] = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
            } else if (name.startsWith(MapSettings.BOARD_GENERATED) 
                    || (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE)) {
                sheetBoards[i] = BoardUtilities.generateRandom(mapSettings);
            } else {
                boolean flipBoard = false;
                
                if (name.startsWith(MapSettings.BOARD_SURPRISE)) {
                    List<String> boardList = extractSurpriseMaps(name);
                    int rnd = (int) (Math.random() * boardList.size());
                    name = boardList.get(rnd);
                } else if (name.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                    // only rotate boards with an even width
                    if ((mapSettings.getBoardWidth() % 2) == 0) {
                        flipBoard = true;
                    }
                    name = name.substring(Board.BOARD_REQUEST_ROTATION.length());
                }

                sheetBoards[i].load(new MegaMekFile(Configuration.boardsDir(), name + CL_KEY_FILEEXTENTION_BOARD).getFile());
                BoardUtilities.flip(sheetBoards[i], flipBoard, flipBoard);
            }
        }

        return BoardUtilities.combine(mapSettings.getBoardWidth(), mapSettings.getBoardHeight(),
                mapSettings.getMapWidth(), mapSettings.getMapHeight(), sheetBoards, rotateBoard,
                mapSettings.getMedium());
    }

    /**
     * Refreshes the game settings with new info from the client
     */
    private void refreshGameSettings() {
        refreshTeams();
        refreshMapSettings();
        refreshDoneButton();
    }
    
    /**
     * Refreshes the Mek Table contents 
     */
    public void refreshEntities() {
        refreshTree();
        refreshMekTable();
    }
    
    private void refreshMekTable() {
        List<Integer> enIds = getSelectedEntities().stream().map(Entity::getId).collect(toList());
        mekModel.clearData();
        ArrayList<Entity> allEntities = new ArrayList<>(clientgui.getClient().getEntitiesVector());
        allEntities.sort(activeSorter);

        boolean localUnits = false;
        GameOptions opts = clientgui.getClient().getGame().getOptions();
        
        for (Entity entity : allEntities) {
            // Remember if the local player has units.
            if (!localUnits && entity.getOwner().equals(localPlayer())) {
                localUnits = true;
            }

            if (!opts.booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES)) { 
                entity.getCrew().clearOptions(PilotOptions.LVL3_ADVANTAGES);
            }

            if (!opts.booleanOption(OptionsConstants.EDGE)) { 
                entity.getCrew().clearOptions(PilotOptions.EDGE_ADVANTAGES);
            }

            if (!opts.booleanOption(OptionsConstants.RPG_MANEI_DOMINI)) { 
                entity.getCrew().clearOptions(PilotOptions.MD_ADVANTAGES);
            }

            if (!opts.booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS)) { 
                entity.clearPartialRepairs();
            }
            
            // Remove some deployment options when a unit is carried
            if (entity.getTransportId() != Entity.NONE) { 
                entity.setHidden(false);
                entity.setProne(false);
                entity.setHullDown(false);
            }
            
            if (!opts.booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) { 
                entity.setHidden(false);
            }
            
            // Handle the "Blind Drop" option. In blind drop, units must be added
            // but they will be obscured in the table. In real blind drop, units
            // don't even get added to the table. Teams see their units in any case.
            boolean localUnit = entity.getOwner().equals(localPlayer());
            boolean teamUnit = !entity.getOwner().isEnemyOf(localPlayer());
            boolean realBlindDrop = opts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
            if (localUnit || teamUnit || !realBlindDrop) {
                mekModel.addUnit(entity);
            }
        }
        // Restore selection
        if (!enIds.isEmpty()) {
            for (int i = 0; i < mekTable.getRowCount(); i++) {
                if (enIds.contains(mekModel.getEntityAt(i).getId())) {
                    mekTable.addRowSelectionInterval(i, i);
                }
            }
        }
    }
    
    /** Adjusts the mektable to compact/normal mode. */
    private void toggleCompact() {
        setTableRowHeights();
        mekModel.refreshCells();
        mekForceTreeModel.nodeChanged((TreeNode) mekForceTreeModel.getRoot());
        
    }

    /** Refreshes the player info table. */
    private void refreshPlayerTable() {
        // Remember the selected players
        var selPlayerIds = getselectedPlayers().stream().map(Player::getId).collect(toSet());

        // Empty and refill the player table
        playerModel.replaceData(game().getPlayersVector());

        // re-select the previously selected players, if possible
        for (int row = 0; row < playerModel.getRowCount(); row++) {
            if (selPlayerIds.contains(playerModel.getPlayerAt(row).getId())) {
                tablePlayers.addRowSelectionInterval(row, row);
            }
        }
        // If no one is selected now (and the table isn't empty), select the first player
        if ((tablePlayers.getSelectedRowCount() == 0) && (tablePlayers.getRowCount() > 0)) {
            tablePlayers.addRowSelectionInterval(0, 0);
        }
    }

    /** Updates the camo button to displays the camo of the currently selected player. */ 
    private void refreshCamoButton() {
        if ((tablePlayers == null) || (playerModel == null) || (tablePlayers.getSelectedRowCount() == 0)) {
            return;
        }
        Player player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
        if (player != null) {
            butCamo.setIcon(player.getCamouflage().getImageIcon());
        }
    }

    /** Sets up the team choice box. */
    private void setupTeamCombo() {
        for (int i = 0; i < Player.TEAM_NAMES.length; i++) {
            comboTeam.addItem(Player.TEAM_NAMES[i]);
        }
    }

    /** Updates the team choice combobox to show the selected player's team. */
    private void refreshTeams() {
        if (localPlayer() != null) {
            comboTeam.removeActionListener(lobbyListener);
            comboTeam.setSelectedIndex(localPlayer().getTeam());
            comboTeam.addActionListener(lobbyListener);
        }
    }

    /** Updates the map settings from the Game */
    private void refreshMapSettings() {
        mapSettings = game().getMapSettings();
    }

    /**
     * Refreshes the Done button. The label will say the opposite of the
     * player's "done" status, indicating that clicking it will reverse the
     * condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone.setText(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone"));
    }

    /** Refreshes the state of the Done button with the state of the local player. */
    private void refreshDoneButton() {
        refreshDoneButton(localPlayer().isDone());
    }

    /**
     * Embarks the given carried Entity onto the carrier given as carrierId.
     */
    void loadOnto(Entity carried, int carrierId, int bayNumber) {
        Entity carrier = game().getEntity(carrierId);
        if (carrier == null || !isLoadable(carried, carrier)) {
            return;
        }

        // We need to make sure our current bomb choices fit onto the new
        // fighter
        if (carrier instanceof FighterSquadron) {
            FighterSquadron fSquad = (FighterSquadron) carrier;
            // We can't use Aero.getBombPoints() because the bombs haven't been
            // loaded yet, only selected, so we have to count the choices
            int[] bombChoice = fSquad.getBombChoices();
            int numLoadedBombs = 0;
            for (int i = 0; i < bombChoice.length; i++) {
                numLoadedBombs += bombChoice[i];
            }
            // We can't load all of the squadrons bombs
            if (numLoadedBombs > ((IBomber) carried).getMaxBombPoints()) {
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("FighterSquadron.bomberror"),
                        Messages.getString("FighterSquadron.bomberror"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        getLocalClient(carried).sendLoadEntity(carried.getId(), carrierId, bayNumber);
        // TODO: it would probably be a good idea 
        // to disable some settings for loaded units in customMechDialog
    }

    /** 
     * Have the given entity disembark if it is carried by another unit.
     * Entities that are modified and need an update to be sent to the server
     * are added to the given updateCandidates. 
     */
    void disembark(Entity entity, Collection<Entity> updateCandidates) {
        if (entity.getTransportId() == Entity.NONE) {
            return;
        }
        Entity carrier = game().getEntity(entity.getTransportId());
        if (carrier != null) {
            carrier.unload(entity);
            entity.setTransportId(Entity.NONE);
            updateCandidates.add(entity);
            updateCandidates.add(carrier);
        }
    }
    
    /** 
     * Have the given entity disembark if it is carried by a unit of another player.
     * Entities that were modified and need an update to be sent to the server
     * are added to the given updateCandidate set. 
     */
    void disembarkDifferentOwner(Entity entity, Collection<Entity> updateCandidates) {
        if (entity.getTransportId() == Entity.NONE) {
            return;
        }
        Entity carrier = clientgui.getClient().getGame().getEntity(entity.getTransportId());
        if (carrier != null && (ownerOf(entity) != ownerOf(carrier))) {
            disembark(entity, updateCandidates);
        }
    }
    
    /** 
     * Have the given entities offload all the units they are carrying.
     * Returns a set of entities that need to be sent to the server. 
     */
    void offloadAll(Collection<Entity> entities, Collection<Entity> updateCandidates) {
        for (Entity carrier: editableEntities(entities)) {
            offloadFrom(carrier, updateCandidates);
        }
    }
    
    /** 
     * Have the given entity offload all the units it is carrying.
     * Returns a set of entities that need to be sent to the server. 
     */
    void offloadFrom(Entity entity, Collection<Entity> updateCandidates) {
        if (isEditable(entity)) {
            for (Entity carriedUnit: entity.getLoadedUnits()) {
                disembark(carriedUnit, updateCandidates);
            } 
        }
    }
    
    /** 
     * Have the given entity offload all units of different players it is carrying.
     * Returns a set of entities that need to be sent to the server. 
     */
    void offloadFromDifferentOwner(Entity entity, Collection<Entity> updateCandidates) {
        for (Entity carriedUnit: entity.getLoadedUnits()) {
            if (ownerOf(carriedUnit) != ownerOf(entity)) {
                disembark(carriedUnit, updateCandidates);
            }
        } 
    }
    
    /** 
     * Sends the entities in the given Collection to the Server. 
     * Sends only those that can be edited, i.e. the player's own
     * or his bots' units. 
     */
    void sendUpdate(Collection<Entity> updateCandidates) {
        for (Entity e: editableEntities(updateCandidates)) {
            getLocalClient(e).sendUpdateEntity(e);
        }
    }
    
    /** 
     * Sends the entities in the given Collection to the Server. 
     * Sends only those that can be edited, i.e. the player's own
     * or his bots' units. Will separate the units into update
     * packets for the local player and any local bots so that the 
     * server accepts all changes (as the server does not know of
     * local bots and rejects updates that are not for the sending client
     * or its teammates. 
     */
    void sendUpdates(Collection<Entity> entities) {
        List<Player> owners = entities.stream().map(Entity::getOwner).distinct().collect(toList());
        for (Player owner: owners) {
            client().sendUpdateEntity(new ArrayList<>(
                    entities.stream().filter(e -> e.getOwner().equals(owner)).collect(toList())));
        }
    }
    
    /** 
     * Disembarks all given entities from any transports they are in. 
     */
    void disembarkAll(Collection<Entity> entities) {
        Set<Entity> updateCandidates = new HashSet<>();
        entities.stream().filter(this::isEditable).forEach(e -> disembark(e, updateCandidates));
        sendUpdate(updateCandidates);
    }

    /** 
     * Returns true when the given entity may be configured by the local player,
     * i.e. if it is his own unit or one of his bot's units.
     * <P>Note that this is more restrictive than the Server is. The Server
     * accepts entity changes also for teammates so that entity updates that 
     * signal transporting a teammate's unit don't get rejected. I feel that
     * configuration other than transporting units should be limited to one's
     * own units (and bots) though.
     */
    boolean isEditable(Entity entity) {
        return clientgui.getBots().containsKey(entity.getOwner().getName())
                || (entity.getOwnerId() == localPlayer().getId());
    }
    
    /** 
     * Returns true when the given entity may NOT be configured by the local player,
     * i.e. if it is not own unit or one of his bot's units.
     * @see #isEditable(Entity)
     */
    boolean isNotEditable(Entity entity) {
        return !isEditable(entity);
    }
    
    /** 
     * Returns true when all given entities may be configured by the local player,
     * i.e. if they are his own units or one of his bot's units.
     * @see #isEditable(Entity)
     */
    boolean isEditable(Collection<Entity> entities) {
        return !entities.stream().anyMatch(this::isNotEditable);
    }
    
    /** 
     * Returns the Client associated with a given entity that may be configured
     * by the local player (his own unit or one of his bot's units).
     * For a unit that cannot be configured (owned by a remote player) the client
     * of the local player is returned.
     */
    Client getLocalClient(Entity entity) {
        if (clientgui.getBots().containsKey(entity.getOwner().getName())) {
            return clientgui.getBots().get(entity.getOwner().getName());
        } else {
            return clientgui.getClient();
        }
    }

    public void configPlayer() {
        Client c = getSelectedClient();
        if (null == c) {
            return;
        }
        
        PlayerSettingsDialog psd = new PlayerSettingsDialog(clientgui, c);
        if (psd.showDialog().isConfirmed()) {
            Player player = c.getLocalPlayer();
            player.setConstantInitBonus(psd.getInit());
            player.setNbrMFConventional(psd.getCnvMines());
            player.setNbrMFVibra(psd.getVibMines());
            player.setNbrMFActive(psd.getActMines());
            player.setNbrMFInferno(psd.getInfMines());
            psd.getSkillGenerationOptionsPanel().updateClient();
            player.setEmail(psd.getEmail());

            // The deployment position
            int startPos = psd.getStartPos();
            final GameOptions gOpts = clientgui.getClient().getGame().getOptions();
            
            player.setStartingPos(startPos);
            player.setStartOffset(psd.getStartOffset());
            player.setStartWidth(psd.getStartWidth());
            c.sendPlayerInfo();
            
            // If the gameoption set_arty_player_homeedge is set, adjust the player's offboard 
            // arty units to be behind the newly selected home edge.
            OffBoardDirection direction = OffBoardDirection.translateStartPosition(startPos);
            if (direction != OffBoardDirection.NONE && 
                    gOpts.booleanOption(OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE)) {
                for (Entity entity: c.getGame().getPlayerEntities(c.getLocalPlayer(), false)) {
                    if (entity.getOffBoardDirection() != OffBoardDirection.NONE) {
                        entity.setOffBoard(entity.getOffBoardDistance(), direction);
                    }
                }
            }
        }
    }

    /**
     * Pop up the dialog to load a mech
     */
    private void addUnit() {
        clientgui.getMechSelectorDialog().updateOptionValues();
        clientgui.getMechSelectorDialog().setVisible(true);
    }
    
    private void createArmy() {
        clientgui.getRandomArmyDialog().setVisible(true);
    }


    public void loadRandomNames() {
        clientgui.getRandomNameDialog().showDialog(clientgui.getClient().getGame().getEntitiesVector());
    }

    void changeMapDnD(String board, JButton button) {
        if (board.contains("\n")) {
            board = MapSettings.BOARD_SURPRISE + board;
        }
        mapSettings.getBoardsSelectedVector().set(mapButtons.indexOf(button), board);
        clientgui.getClient().sendMapSettings(mapSettings);
        if (boardPreviewW.isVisible()) {
            previewGameBoard();
        }

        clientgui.getClient().sendChat(clientgui.getClient().getLocalPlayer() + " changed map to: " + board);
    }

    //
    // GameListener
    //
    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }
        refreshDoneButton();
        clientgui.getClient().getGame().setupTeams();
        refreshPlayerTable();
        refreshPlayerConfig();
        refreshCamoButton();
        refreshEntities();
        panTeamOverview.refreshData();
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }
        
        if (clientgui.getClient().getGame().getPhase().isLounge()) {
            refreshDoneButton();
            refreshGameSettings();
            refreshPlayerTable();
            refreshTeams();
            refreshCamoButton();
            refreshEntities();
            panTeamOverview.refreshData();
        }
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshPlayerTable();
        panTeamOverview.refreshData();
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
            updateTableHeaders();
        }
        refreshEntities();
        refreshPlayerTable();
        refreshMapSizes();
        updateMapSettings(clientgui.getClient().getMapSettings());
        panTeamOverview.refreshData();
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
            
            if (ev.getSource().equals(butAdd)) {
                addUnit();
            } else if (ev.getSource().equals(butArmy)) {
                createArmy();
            } else if (ev.getSource().equals(butSkills)) {
                new SkillGenerationDialog(clientgui.getFrame(), clientgui,
                        clientgui.getClient().getGame().getEntitiesVector()).showDialog();
            } else if (ev.getSource().equals(butNames)) {
                loadRandomNames();
            } else if (ev.getSource().equals(tablePlayers)) {
                configPlayer();
            } else if (ev.getSource().equals(comboTeam)) {
                lobbyActions.changeTeam(getselectedPlayers(), comboTeam.getSelectedIndex());
            } else if (ev.getSource().equals(butConfigPlayer)) {
                configPlayer();
            } else if (ev.getSource().equals(butBotSettings)) {
                doBotSettings();
            } else if (ev.getSource().equals(butOptions)) {
                clientgui.getGameOptionsDialog().setEditable(true);
                clientgui.getGameOptionsDialog().update(clientgui.getClient().getGame().getOptions());
                clientgui.getGameOptionsDialog().setVisible(true);
            } else if (ev.getSource().equals(butCompact)) {
                toggleCompact();
            } else if (ev.getSource().equals(butLoadList)) {
                // Allow the player to replace their current
                // list of entities with a list from a file.
                Client c = getSelectedClient();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBotOrPlayer"));
                    return;
                }
                clientgui.loadListFile(c.getLocalPlayer());
                
            } else if (ev.getSource().equals(butSaveList)) {
                // Allow the player to save their current
                // list of entities to a file.
                Client c = getSelectedClient();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBotOrPlayer"));
                    return;
                }
                clientgui.saveListFile(c.getGame().getPlayerEntities(c.getLocalPlayer(), false),
                        c.getLocalPlayer().getName());
                
            } else if (ev.getSource().equals(butAddBot)) {
                configAndCreateBot(null);
                
            } else if (ev.getSource().equals(butRemoveBot)) {
                removeBot();
                
            } else if (ev.getSource().equals(butShowUnitID)) {
                PreferenceManager.getClientPreferences().setShowUnitId(butShowUnitID.isSelected());
                mekModel.refreshCells();
                repaint();
                
            } else if (ev.getSource() == butConditions) {
                PlanetaryConditionsDialog pcd = new PlanetaryConditionsDialog(clientgui);
                boolean userOkay = pcd.showDialog();
                if (userOkay) {
                    clientgui.getClient().sendPlanetaryConditions(pcd.getConditions());
                }
                
            } else if (ev.getSource() == butRandomMap) {
                RandomMapDialog rmd = new RandomMapDialog(clientgui.frame, ChatLounge.this, clientgui.getClient(), mapSettings);
                rmd.activateDialog(clientgui.getBoardView().getTilesetManager().getThemes());
                
            } else if (ev.getSource().equals(butBoardPreview)) {
                previewGameBoard();
                
            } else if (ev.getSource().equals(comMapSizes)) {
                if (Messages.getString("ChatLounge.CustomMapSize").equals(comMapSizes.getSelectedItem())) {
                    refreshMapUI();
                } else if (comMapSizes.getSelectedItem() != null) {
                    BoardDimensions size = (BoardDimensions) comMapSizes.getSelectedItem();
                    mapSettings.setBoardSize(size.width(), size.height());
                    resetAvailBoardSelection = true;
                    resetSelectedBoards = true;
                    clientgui.getClient().sendMapSettings(mapSettings);
                } 
                
            } else if (ev.getSource() == butGroundMap) {
                mapSettings.setMedium(MapSettings.MEDIUM_GROUND);
                refreshMapUI();
                clientgui.getClient().sendMapSettings(mapSettings);
                
            } else if (ev.getSource() == butSpaceMap) {
                mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                mapSettings.setBoardSize(50, 50);
                mapSettings.setMapSize(1, 1);
                refreshMapUI();
                clientgui.getClient().sendMapDimensions(mapSettings);
                
            } else if (ev.getSource() == butLowAtmoMap) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
                refreshMapUI();
                clientgui.getClient().sendMapSettings(mapSettings);
                
            } else if (ev.getSource() == butAddX || ev.getSource() == butMapGrowW) {
                int newMapWidth = mapSettings.getMapWidth() + 1;
                mapSettings.setMapSize(newMapWidth, mapSettings.getMapHeight());
                clientgui.getClient().sendMapDimensions(mapSettings);
                
            } else if (ev.getSource() == butAddY || ev.getSource() == butMapGrowH) {
                int newMapHeight = mapSettings.getMapHeight() + 1;
                mapSettings.setMapSize(mapSettings.getMapWidth(), newMapHeight);
                clientgui.getClient().sendMapDimensions(mapSettings);
                
            } else if (ev.getSource() == butSaveMapSetup) {
                saveMapSetup();
                
            } else if (ev.getSource() == butLoadMapSetup) {
                loadMapSetup();
                
            } else if (ev.getSource() == fldMapWidth) {
                setManualMapWidth();
                
            } else if (ev.getSource() == fldMapHeight) {
                setManualMapHeight();
                
            } else if (ev.getSource() == fldSpaceBoardWidth) {
                setManualBoardWidth();
                
            } else if (ev.getSource() == fldSpaceBoardHeight) {
                setManualBoardHeight();
                
            } else if (ev.getSource() == butMapShrinkW) {
                if (mapSettings.getMapWidth() > 1) {
                    int newMapWidth = mapSettings.getMapWidth() - 1;
                    mapSettings.setMapSize(newMapWidth, mapSettings.getMapHeight());
                    clientgui.getClient().sendMapDimensions(mapSettings);
                }
                
            } else if (ev.getSource() == butMapShrinkH) {
                if (mapSettings.getMapHeight() > 1) {
                    int newMapHeight = mapSettings.getMapHeight() - 1;
                    mapSettings.setMapSize(mapSettings.getMapWidth(), newMapHeight);
                    clientgui.getClient().sendMapDimensions(mapSettings);
                }
                
            } else if (ev.getSource() == butDetach) {
                butDetach.setEnabled(false);
                panTeam.remove(panTeamOverview);
                panTeam.repaint();
                panTeamOverview.setDetached(true);
                teamOverviewWindow.add(panTeamOverview);
                teamOverviewWindow.center();
                teamOverviewWindow.setVisible(true);
                
            } else if (ev.getSource() == butCancelSearch) {
                fldSearch.setText("");

            } else if (ev.getSource() == butHelp) {
                File helpfile = new File(CL_KEY_FILEPATH_MAPASSEMBLYHELP);
                final JDialog dialog = new ClientDialog(clientgui.frame,
                        Messages.getString("ChatLounge.map.title.mapAssemblyHelp"), true, true);
                final int height = 600;
                final int width = 600;
                
                final JEditorPane pane = new JEditorPane();
                pane.setName(CL_KEY_NAMEHELPPANE);
                pane.setEditable(false);
                pane.setFont(UIUtil.getScaledFont());
                try {
                    pane.setPage(helpfile.toURI().toURL());
                    JScrollPane tScroll = new JScrollPane(pane,
                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    tScroll.getVerticalScrollBar().setUnitIncrement(16);
                    dialog.add(tScroll, BorderLayout.CENTER);
                } catch (Exception e) {
                    dialog.setTitle(Messages.getString("AbstractHelpDialog.noHelp.title"));
                    pane.setText(Messages.getString("AbstractHelpDialog.errorReading") + e.getMessage());
                    LogManager.getLogger().error("", e);
                }

                JButton button = new DialogButton(Messages.getString("Okay"));
                button.addActionListener(e -> dialog.setVisible(false));
                JPanel okayPanel = new JPanel(new FlowLayout());
                okayPanel.add(button);
                dialog.add(okayPanel, BorderLayout.PAGE_END);

                Dimension sz = new Dimension(scaleForGUI(width), scaleForGUI(height));
                dialog.setPreferredSize(sz);
                dialog.setVisible(true);
                
            } else if (ev.getSource() == butListView) {
                scrMekTable.setViewportView(mekTable);
                butCollapse.setEnabled(false);
                butExpand.setEnabled(false);
                
            } else if (ev.getSource() == butForceView) {
                scrMekTable.setViewportView(mekForceTree);
                butCollapse.setEnabled(true);
                butExpand.setEnabled(true);
                
            } else if (ev.getSource() == butCollapse) {
                collapseTree();
                
            } else if (ev.getSource() == butExpand) {
                expandTree();
            } 
        }
    };
    
    /** Expands the Mek Force Tree fully. */
    private void expandTree() {
        for (int i = 0; i < mekForceTree.getRowCount(); i++) {
            mekForceTree.expandRow(i);
        }
    }
    
    /** Collapses the Mek Force Tree fully. */
    private void collapseTree() {
        for (int i = 0; i < mekForceTree.getRowCount(); i++) {
            mekForceTree.collapseRow(i);
        }
    }
    
    private void configAndCreateBot(@Nullable Player toReplace) {
        BehaviorSettings behavior = null;
        String botName = null;
        if (toReplace != null) {
            behavior = game().getBotSettings().get(toReplace.getName());
            botName = toReplace.getName();
        }
        var bcd = new BotConfigDialog(clientgui.frame, botName, behavior, clientgui);
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CANCELLED) {
            return;
        }
        Princess botClient = Princess.createPrincess(bcd.getBotName(), client().getHost(), 
                client().getPort(), bcd.getBehaviorSettings());
        botClient.setClientGUI(clientgui);
        botClient.getGame().addGameListener(new BotGUI(getClientgui().getFrame(), botClient));
        try {
            botClient.connect();
            clientgui.getBots().put(bcd.getBotName(), botClient);
        } catch (Exception e) {
            clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertBot.title"), Messages.getString("ChatLounge.AlertBot.message"));
            botClient.die();
        }
    }

    
    /** 
     * Opens a file chooser and saves the current map setup to the file,
     * if any was chosen.
     * @see MapSetup 
     */
    private void saveMapSetup() {
        JFileChooser fc = new JFileChooser(Configuration.dataDir() + CL_KEY_FILEPATH_MAPSETUP);
        fc.setDialogTitle(Messages.getString("ChatLounge.map.saveMapSetup"));
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(XMLFileFilter);

        int returnVal = fc.showSaveDialog(clientgui.frame);
        File selectedFile = fc.getSelectedFile();
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (selectedFile == null)) {
            return;
        }
        if (!selectedFile.getName().toLowerCase().endsWith(CL_KEY_FILEEXTENTION_XML)) {
            selectedFile = new File(selectedFile.getPath() + CL_KEY_FILEEXTENTION_XML);
        }
        if (selectedFile.exists()) {
            String msg = Messages.getString("ChatLounge.map.saveMapSetupReplace", selectedFile.getName());
            if (!MMConfirmDialog.confirm(clientgui.frame, Messages.getString("ChatLounge.map.confirmReplace"), msg)) {
                return;
            }
        }
        try (OutputStream os = new FileOutputStream(selectedFile)) {
            MapSetup.save(os, mapSettings);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(clientgui.frame,
                    Messages.getString("ChatLounge.map.problemSaving"),
                    Messages.getString("Error"), JOptionPane.ERROR_MESSAGE);
            LogManager.getLogger().error("", ex);
        }
    }

    /** 
     * Opens a file chooser and loads a new map setup from the file,
     * if any was chosen.
     * @see MapSetup 
     */
    private void loadMapSetup() {
        JFileChooser fc = new JFileChooser(Configuration.dataDir() + CL_KEY_FILEPATH_MAPSETUP);
        fc.setDialogTitle(Messages.getString("ChatLounge.map.loadMapSetup"));
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(XMLFileFilter);

        int returnVal = fc.showOpenDialog(clientgui.frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return;
        }
        if (!fc.getSelectedFile().exists()) {
            JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("ChatLounge.fileNotFound"));
            return;
        }
        try (InputStream os = new FileInputStream(fc.getSelectedFile())) {
            MapSetup setup = MapSetup.load(os);
            mapSettings.setMapSize(setup.getMapWidth(), setup.getMapHeight());
            mapSettings.setBoardSize(setup.getBoardWidth(), setup.getBoardHeight());
            mapSettings.setBoardsSelectedVector(setup.getBoards());
            clientgui.getClient().sendMapSettings(mapSettings);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(clientgui.frame,
                    Messages.getString("ChatLounge.map.problemLoadMapSetup"),
                    Messages.getString("Error"), JOptionPane.ERROR_MESSAGE);
            LogManager.getLogger().error("", ex);
        }
    }
    
    private void removeBot() {
        Client c = getSelectedClient();
        if (!client().bots.containsValue(c)) {
            LobbyErrors.showOnlyOwnBot(clientgui.frame);
            return;
        }
        // Delete units first, which safely disembarks and offloads them
        // Don't delete the bot's forces, as that could also delete other players' entitites
        c.die();
        clientgui.getBots().remove(c.getName());
    }
    
    private void doBotSettings() {
        Player player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
        Princess bot = (Princess) clientgui.getBots().get(player.getName());
        var bcd = new BotConfigDialog(clientgui.frame, bot.getLocalPlayer().getName(), bot.getBehaviorSettings(), clientgui);
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CONFIRMED) {
            bot.setBehaviorSettings(bcd.getBehaviorSettings());
        }
    }
    
    // Put a filter on the files that the user can select the proper file.
    FileFilter XMLFileFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return (f.getPath().toLowerCase().endsWith(CL_KEY_FILEEXTENTION_XML) || f.isDirectory());
        }

        @Override
        public String getDescription() {
            return Messages.getString("ChatLounge.map.SetupXMLfiles");
        }
    };
    
    private void setManualMapWidth() {
        try {
            int newMapWidth = Integer.parseInt(fldMapWidth.getText());
            if (newMapWidth >= 1 && newMapWidth <= 20) {
                mapSettings.setMapSize(newMapWidth, mapSettings.getMapHeight());
                clientgui.getClient().sendMapDimensions(mapSettings);
            }
        } catch (NumberFormatException e) {
            // no number, no new map width
        }
    }
    
    private void setManualMapHeight() {
        try {
            int newMapHeight = Integer.parseInt(fldMapHeight.getText());
            if (newMapHeight >= 1 && newMapHeight <= 20) {
                mapSettings.setMapSize(mapSettings.getMapWidth(), newMapHeight);
                clientgui.getClient().sendMapDimensions(mapSettings);
            }
        } catch (NumberFormatException e) {
            // no number, no new map height
        }
    }
    
    private void setManualBoardWidth() {
        try {
            int newBoardWidth = Integer.parseInt(fldSpaceBoardWidth.getText());
            if (newBoardWidth >= 5 && newBoardWidth <= 200) {
                mapSettings.setBoardSize(newBoardWidth, mapSettings.getBoardHeight());
                clientgui.getClient().sendMapSettings(mapSettings);
            }
        } catch (NumberFormatException e) {
            // no number, no new board width
        }
    }
    
    private void setManualBoardHeight() {
        try {
            int newBoardHeight = Integer.parseInt(fldSpaceBoardHeight.getText());
            if (newBoardHeight >= 5 && newBoardHeight <= 200) {
                mapSettings.setBoardSize(mapSettings.getBoardWidth(), newBoardHeight);
                clientgui.getClient().sendMapSettings(mapSettings);
            }
        } catch (NumberFormatException e) {
            // no number, no new board height
        }
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
        refreshMapUI();
        refreshBoardsAvailable();
        updateSearch(fldSearch.getText());
        refreshLabels();
    }


    /**OK Refreshes the Map Summary, Tech Level and Game Year labels. */
    private void refreshLabels() {
        GameOptions opts = clientgui.getClient().getGame().getOptions();

        String txt = Messages.getString("ChatLounge.GameYear");
        txt += opts.intOption(OptionsConstants.ALLOWED_YEAR);
        lblGameYear.setText(txt);
        lblGameYear.setToolTipText(scaleStringForGUI(Messages.getString("ChatLounge.tooltip.techYear")));
        
        String tlString = TechConstants.getLevelDisplayableName(TechConstants.T_TECH_UNKNOWN);
        IOption tlOpt = opts.getOption(OptionsConstants.ALLOWED_TECHLEVEL);
        if (tlOpt != null) {
            tlString = tlOpt.stringValue();
        }
        lblTechLevel.setText(Messages.getString("ChatLounge.TechLevel") + tlString);
        lblTechLevel.setToolTipText(scaleStringForGUI(Messages.getString("ChatLounge.tooltip.techYear")));
        
        txt = Messages.getString("ChatLounge.MapSummary");
        txt += (mapSettings.getBoardWidth() * mapSettings.getMapWidth()) + " x " 
                + (mapSettings.getBoardHeight() * mapSettings.getMapHeight());
        if (butGroundMap.isSelected()) {
            txt += Messages.getString("ChatLounge.name.groundMap");
        } else if (butLowAtmoMap.isSelected()) {
            txt += " " + Messages.getString("ChatLounge.name.atmosphericMap");
        } else {
            txt += " " + Messages.getString("ChatLounge.name.spaceMap");
        }
        lblMapSummary.setText(txt);

        StringBuilder selectedMaps = new StringBuilder();
        selectedMaps.append(Messages.getString("ChatLounge.MapSummarySelectedMaps"));
        for (String map: mapSettings.getBoardsSelectedVector()) {
            selectedMaps.append("&nbsp;&nbsp;");
            if (map.startsWith(MapSettings.BOARD_SURPRISE)) {
                selectedMaps.append(MapSettings.BOARD_SURPRISE);
            } else {
                selectedMaps.append(map);
            }
            selectedMaps.append("<br>"); 
        }
        lblMapSummary.setToolTipText(scaleStringForGUI(selectedMaps.toString()));
    }
    
    @Override
    public void ready() {
        final Client client = clientgui.getClient();
        final Game game = client.getGame();
        final GameOptions gOpts = game.getOptions();
        
        // enforce exclusive deployment zones in double blind
        for (Player player: client.getGame().getPlayersVector()) {
            if (!isValidStartPos(game, player)) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.OverlapDeploy.title"),
                        Messages.getString("ChatLounge.OverlapDeploy.msg"));
                return;
            }
        }

        // Make sure player has a commander if Commander killed victory is on
        if (gOpts.booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) {
            List<String> players = new ArrayList<>();
            if ((game.getLiveCommandersOwnedBy(localPlayer()) < 1)
                    && (game.getEntitiesOwnedBy(localPlayer()) > 0)) {
                players.add(client.getLocalPlayer().getName());
            }

            for (Client bc : clientgui.getBots().values()) {
                if ((game.getLiveCommandersOwnedBy(bc.getLocalPlayer()) < 1)
                        && (game.getEntitiesOwnedBy(bc.getLocalPlayer()) > 0)) {
                    players.add(bc.getLocalPlayer().getName());
                }
            }

            if (!players.isEmpty()) {
                String msg = Messages.getString("ChatLounge.noCmdr.msg");
                for (String player : players) {
                    msg += player + "\n";
                }
                clientgui.doAlertDialog(Messages.getString("ChatLounge.noCmdr.title"), msg);
                return;
            }
        }

        boolean done = !localPlayer().isDone();
        client.sendDone(done);
        refreshDoneButton(done);
        for (Client botClient : clientgui.getBots().values()) {
            botClient.sendDone(done);
        }
    }

    Client getSelectedClient() {
        if ((tablePlayers == null) || (tablePlayers.getSelectedRowCount() == 0)) {
            return null;
        }

        Player player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
        if (localPlayer().equals(player)) {
            return client();
        } else if (client().bots.containsKey(player.getName())) {
            return client().bots.get(player.getName());
        } else {
            return null;
        }
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        GUIP.removePreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().removePreferenceChangeListener(this);
        MechSummaryCache.getInstance().removeListener(mechSummaryCacheListener);
        
        if (loader != null) {
            loader.cancel(true);
        }
        
        tablePlayers.getSelectionModel().removeListSelectionListener(this);
        tablePlayers.removeMouseListener(new PlayerTableMouseAdapter());
        
        lisBoardsAvailable.removeListSelectionListener(this);
        lisBoardsAvailable.removeMouseListener(mapListMouseListener);
        lisBoardsAvailable.removeMouseMotionListener(mapListMouseListener);

        teamOverviewWindow.removeWindowListener(teamOverviewWindowListener);
        
        mekTable.removeMouseListener(mekTableMouseAdapter);
        mekForceTree.removeMouseListener(mekForceTreeMouseListener);
        mekTable.getTableHeader().removeMouseListener(mekTableHeaderMouseListener);
        mekTable.removeKeyListener(mekTableKeyListener);
        mekForceTree.removeKeyListener(mekTreeKeyListener);
        
        butAdd.removeActionListener(lobbyListener);
        butAddBot.removeActionListener(lobbyListener);
        butArmy.removeActionListener(lobbyListener);
        butBoardPreview.removeActionListener(lobbyListener);
        butBotSettings.removeActionListener(lobbyListener);
        butCompact.removeActionListener(lobbyListener);
        butConditions.removeActionListener(lobbyListener);
        butConfigPlayer.removeActionListener(lobbyListener);
        butLoadList.removeActionListener(lobbyListener);
        butNames.removeActionListener(lobbyListener);
        butOptions.removeActionListener(lobbyListener);
        butRandomMap.removeActionListener(lobbyListener);
        butRemoveBot.removeActionListener(lobbyListener);
        butSaveList.removeActionListener(lobbyListener);
        butShowUnitID.removeActionListener(lobbyListener);
        butSkills.removeActionListener(lobbyListener);
        butSpaceSize.removeActionListener(lobbyListener);
        butCamo.removeActionListener(camoListener);
        butAddX.removeActionListener(lobbyListener);
        butAddY.removeActionListener(lobbyListener);
        butMapGrowW.removeActionListener(lobbyListener);
        butMapShrinkW.removeActionListener(lobbyListener);
        butMapGrowH.removeActionListener(lobbyListener);
        butMapShrinkH.removeActionListener(lobbyListener);
        butGroundMap.removeActionListener(lobbyListener);
        butLowAtmoMap.removeActionListener(lobbyListener);
        butHighAtmoMap.removeActionListener(lobbyListener);
        butSpaceMap.removeActionListener(lobbyListener);
        butLoadMapSetup.removeActionListener(lobbyListener);
        butSaveMapSetup.removeActionListener(lobbyListener);
        butDetach.removeActionListener(lobbyListener);
        butCancelSearch.removeActionListener(lobbyListener);
        butHelp.removeActionListener(lobbyListener);
        butListView.removeActionListener(lobbyListener);
        butForceView.removeActionListener(lobbyListener);
        butCollapse.removeActionListener(lobbyListener);
        butExpand.removeActionListener(lobbyListener);
        
        fldMapWidth.removeActionListener(lobbyListener);
        fldMapHeight.removeActionListener(lobbyListener);
        fldSpaceBoardWidth.removeActionListener(lobbyListener);
        fldSpaceBoardHeight.removeActionListener(lobbyListener);
        
        comboTeam.removeActionListener(lobbyListener);
        
        KeyboardFocusManager kbfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kbfm.removeKeyEventDispatcher(lobbyKeyDispatcher);
    }

    /**
     * Returns true if the given list of entities can be configured as a group.
     * This requires that they all have the same owner, and that none of the
     * units are being transported. Also, the owner must be the player or one
     * of his bots. 
     */
    boolean canConfigureMultipleDeployment(Collection<Entity> entities) {
        return haveSingleOwner(entities) 
                && !containsTransportedUnit(entities)
                && canEditAny(entities);
    }
    
    /**
     * Returns true if the given collection contains at least one entity
     * that the local player can edit, i.e. is his own or belongs to
     * one of his bots. Does not check if the units are otherwise configured,
     * e.g. transported.
     * <P>See also {@link #isEditable(Entity)}
     */
    boolean canEditAny(Collection<Entity> entities) {
        return entities.stream().anyMatch(this::isEditable);
    }
    
    /**
     * Returns true if the local player can see all the given entities.
     * This is true except when a blind drop option is active and one or more
     * of the entities are not on his team.
     */
    boolean canSeeAll(Collection<Entity> entities) {
        if (!game().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP)
                && !game().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)) {
            return true;
        }
        for (Entity entity: entities) {
            if (!entityInLocalTeam(entity)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the local player can see the given entity.
     * This is true except when a blind drop option is active and one or more
     * of the entities are not his own.
     */
    boolean canSee(Entity entity) {
        return canSeeAll(Arrays.asList(entity));
    }
    
    boolean entityInLocalTeam(Entity entity) {
        return !localPlayer().isEnemyOf(entity.getOwner());
    }
    

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        
        if (event.getSource().equals(tablePlayers.getSelectionModel())) {
            refreshPlayerConfig();
        }
    }
    
    /** Adapts the enabled state of the player config UI items to the player selection. */
    private void refreshPlayerConfig() {
        var selPlayers = getselectedPlayers();
        var hasSelection = !selPlayers.isEmpty();
        var isSinglePlayer = selPlayers.size() == 1;
        var allConfigurable = hasSelection && selPlayers.stream().allMatch(lobbyActions::isSelfOrLocalBot);
        var isSingleLocalBot = isSinglePlayer && (getSelectedClient() instanceof BotClient);
        comboTeam.setEnabled(allConfigurable);
        butLoadList.setEnabled(allConfigurable && isSinglePlayer);
        butCamo.setEnabled(allConfigurable && isSinglePlayer);
        butConfigPlayer.setEnabled(allConfigurable && isSinglePlayer);
        refreshCamoButton();
        // Disable the Remove Bot button for the "player" of a "Connect As Bot" client
        butRemoveBot.setEnabled(isSingleLocalBot);
        butSaveList.setEnabled(false);
        if (isSinglePlayer) {
            var selPlayer = theElement(selPlayers);
            var hasUnits = !game().getPlayerEntities(selPlayer, false).isEmpty();
            butSaveList.setEnabled(hasUnits && unitsVisible(selPlayer));
            setTeamSelectedItem(selPlayer.getTeam());
        }
    }
    
    /** Sets (without firing events) the team combobox. */
    private void setTeamSelectedItem(int team) {
        comboTeam.removeActionListener(lobbyListener);
        comboTeam.setSelectedIndex(team);
        comboTeam.addActionListener(lobbyListener);
    }
    
    /** 
     * Returns false when any blind-drop option is active and player is not on the local team; 
     * true otherwise. When true, individual units of the given player should not be shown/saved/etc. 
     */ 
    private boolean unitsVisible(Player player) {
        GameOptions opts = clientgui.getClient().getGame().getOptions();
        boolean isBlindDrop = opts.booleanOption(OptionsConstants.BASE_BLIND_DROP)
                || opts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        return !player.isEnemyOf(localPlayer()) || !isBlindDrop;
    }

    public class PlayerTableMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = tablePlayers.rowAtPoint(e.getPoint());
                Player player = playerModel.getPlayerAt(row);
                if (player != null) {
                    boolean isLocalPlayer = player.equals(localPlayer());
                    boolean isLocalBot = clientgui.getBots().get(player.getName()) != null;
                    if ((isLocalPlayer || isLocalBot)) {
                        configPlayer();
                    }
                }
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                // If the right mouse button is pressed over an unselected player,
                // clear the selection and select that entity instead
                int row = tablePlayers.rowAtPoint(e.getPoint());
                if (!tablePlayers.isRowSelected(row)) {
                    tablePlayers.changeSelection(row, row, false, false);
                }
                showPopup(e);
            }
        }

        /** Shows the right-click menu on the mek table */
        private void showPopup(MouseEvent e) {
            if (tablePlayers.getSelectedRowCount() == 0) {
                return;
            }
            ScalingPopup popup = PlayerTablePopup.playerTablePopup(clientgui, 
                    playerTableActionListener, getselectedPlayers());
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    private ActionListener playerTableActionListener = evt -> {
        if (tablePlayers.getSelectedRowCount() == 0) {
            return;
        }

        StringTokenizer st = new StringTokenizer(evt.getActionCommand(), "|");
        String command = st.nextToken();
        switch (command) {
            case PlayerTablePopup.PTP_CONFIG:
                configPlayer();
                break;
            case PlayerTablePopup.PTP_TEAM:
                int newTeam = Integer.parseInt(st.nextToken());
                lobbyActions.changeTeam(getselectedPlayers(), newTeam);
                break;
            case PlayerTablePopup.PTP_BOTREMOVE:
                removeBot();
                break;
            case PlayerTablePopup.PTP_BOTSETTINGS:
                doBotSettings();
                break;
            case PlayerTablePopup.PTP_DEPLOY:
                int startPos = Integer.parseInt(st.nextToken());

                for (Player player: getselectedPlayers()) {
                    if (lobbyActions.isSelfOrLocalBot(player)) {
                        if (client().isLocalBot(player)) {
                            // must use the bot's own player object:
                            client().getBotClient(player).getLocalPlayer().setStartingPos(startPos);
                            client().getBotClient(player).sendPlayerInfo();
                        } else {
                            player.setStartingPos(startPos);
                            client().sendPlayerInfo();
                        }
                    }
                }
                break;
            case PlayerTablePopup.PTP_REPLACE:
                Player player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
                configAndCreateBot(player);
                break;
        }
    };

    private ArrayList<Player> getselectedPlayers() {
        var result = new ArrayList<Player>(); 
        for (int row: tablePlayers.getSelectedRows()) {
            Player player = playerModel.getPlayerAt(row);
            if (player != null) {
                result.add(player);
            }
        }
        return result;
    }

    KeyListener mekTableKeyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent evt) {
            if (mekTable.getSelectedRowCount() == 0) {
                return;
            }
            List<Entity> entities = getSelectedEntities();
            int code = evt.getKeyCode();
            if ((code == KeyEvent.VK_DELETE) || (code == KeyEvent.VK_BACK_SPACE)) {
                evt.consume();
                lobbyActions.delete(new ArrayList<>(), entities, true);
            } else if (code == KeyEvent.VK_SPACE) {
                evt.consume();
                LobbyUtility.mechReadoutAction(entities, canSeeAll(entities), false, getClientgui().getFrame());
            } else if (code == KeyEvent.VK_ENTER) {
                evt.consume();
                if (entities.size() == 1) {
                    lobbyActions.customizeMech(entities.get(0));
                } else if (canConfigureMultipleDeployment(entities)) {
                    lobbyActions.customizeMechs(entities);
                }
            }
        }
    };
    
    /** Copies the selected units, if any, from the displayed Unit Table / Force Tree to the clipboard. */
    public void copyToClipboard() {
        List<Entity> entities = isForceView() ? getTreeSelectedEntities() : getSelectedEntities();
        StringSelection stringSelection = new StringSelection(clipboardString(entities));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
    
    /** Reads the clipboard and adds units, if it can parse them. */
    public void importClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null) &&
                contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        List<Entity> newEntities = new ArrayList<>();
        if (hasTransferableText) {
            try {
                String result = (String) contents.getTransferData(DataFlavor.stringFlavor);
                StringTokenizer lines = new StringTokenizer(result, "\n");
                while (lines.hasMoreTokens()) {
                    String line = lines.nextToken();
                    StringTokenizer tabs = new StringTokenizer(line, "\t");
                    String unit = "";
                    if (tabs.hasMoreTokens()) {
                        unit = tabs.nextToken();
                    }
                    if (tabs.hasMoreTokens()) {
                        unit += " " + tabs.nextToken();
                    }
                    MechSummary ms = MechSummaryCache.getInstance().getMech(unit);
                    if (ms == null) {
                        continue;
                    }
                    Entity newEntity = new MechFileParser(ms.getSourceFile(),
                            ms.getEntryName()).getEntity();
                    if (newEntity != null) {
                        newEntity.setOwner(localPlayer());
                        newEntities.add(newEntity);
                    }
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }

            if (!newEntities.isEmpty()) {
                client().sendAddEntity(newEntities);
                client().sendChat(client().getLocalPlayer() + " loaded units from Clipboard for player: " + localPlayer().getName() + " [units " + newEntities.size() + "]");
            }
        }
    }
    
    /**
     *  @return a String representing the entities to export to the clipboard.
     */
    private String clipboardString(Collection<Entity> entities) {
        StringBuilder result = new StringBuilder();
        for (Entity entity: entities) {
            // Chassis
            result.append(entity.getChassis()).append("\t");
            // Model
            result.append(entity.getModel()).append("\t");
            // Weight; format for locale to avoid wrong ",." etc.
            Locale cl = Locale.getDefault();
            NumberFormat numberFormatter = NumberFormat.getNumberInstance(cl);
            result.append(numberFormatter.format(entity.getWeight())).append("\t");
            // Pilot name
            result.append(entity.getCrew().getName()).append("\t");
            // Crew Skill with text
            result.append(CrewSkillSummaryUtil.getSkillNames(entity)).append(": ")
                    .append(entity.getCrew().getSkillsAsString(false)).append("\t");
            // BV without C3 but with pilot (as that gets exported too)
            result.append(entity.calculateBattleValue(true, false)).append("\t");
            result.append("\n");
        }
        return result.toString();
    }
    
    /** Returns a list of entities selected in the ForceTree. May be empty, but not null. */
    private List<Entity> getTreeSelectedEntities() {
        TreePath[] selection = mekForceTree.getSelectionPaths();
        List<Entity> entities = new ArrayList<>();
        if (selection != null) {
            for (TreePath path: selection) {
                if (path != null) {
                    Object selected = path.getLastPathComponent();
                    if (selected instanceof Entity) {
                        entities.add((Entity) selected);
                    }  
                }
            }
        }
        return entities;
    }
    
    /** Returns a list of forces selected in the ForceTree. May be empty, but not null. */
    private List<Force> getTreeSelectedForces() {
        TreePath[] selection = mekForceTree.getSelectionPaths();
        List<Force> selForces = new ArrayList<>();
        if (selection != null) {
            for (TreePath path: selection) {
                if (path != null) {
                    Object selected = path.getLastPathComponent();
                    if (selected instanceof Force) {
                        selForces.add((Force) selected);
                    } 
                }
            }
        }
        return selForces;
    }
    
    /** The key listener for the Force Tree. */
    KeyListener mekTreeKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e) {
            List<Entity> selEntities = getTreeSelectedEntities();
            List<Force> selForces = getTreeSelectedForces();
            boolean onlyOneEntity = (selEntities.size() == 1) && selForces.isEmpty();
            int code = e.getKeyCode();
            
            if (code == KeyEvent.VK_SPACE) {
                e.consume();
                mechReadoutAction(selEntities, canSeeAll(selEntities), false, getClientgui().getFrame());
                
            } else if (code == KeyEvent.VK_ENTER && onlyOneEntity) {
                e.consume();
                lobbyActions.customizeMech(selEntities.get(0));
                
            } else if (code == KeyEvent.VK_UP && e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
                e.consume();
                lobbyActions.forceMove(selForces, selEntities, true);
                
            } else if (code == KeyEvent.VK_DOWN && e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
                e.consume();
                lobbyActions.forceMove(selForces, selEntities, false);
                
            } else if ((code == KeyEvent.VK_DELETE) || (code == KeyEvent.VK_BACK_SPACE)) {
                e.consume();
                lobbyActions.delete(selForces, selEntities, true);
                
            } else if (code == KeyEvent.VK_RIGHT && e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
                e.consume();
                expandTree();
                
            } else if (code == KeyEvent.VK_LEFT && e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
                e.consume();
                collapseTree();
                
            }
        }
    };


    public class MapListMouseAdapter extends MouseInputAdapter implements ActionListener {
        ScalingPopup popup;

        @Override
        public void actionPerformed(ActionEvent action) {
            String[] command = action.getActionCommand().split(":");

            switch (command[0]) {
                case MapListPopup.MLP_BOARD:
                    boolean rotate = (command.length > 3) && Boolean.parseBoolean(command[3]);
                    String rotateRequest = rotate ? Board.BOARD_REQUEST_ROTATION : "";

                    changeMapDnD(rotateRequest + command[2], mapButtons.get(Integer.parseInt(command[1])));
                    break;

                case MapListPopup.MLP_SURPRISE:
                    changeMapDnD(command[2], mapButtons.get(Integer.parseInt(command[1])));
                    break;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (lisBoardsAvailable.isEnabled()) {
                // If a mouse button is pressed over a map,
                // show the board selection popup
                int index = lisBoardsAvailable.locationToIndex(e.getPoint());
                if (index != -1 && lisBoardsAvailable.getCellBounds(index, index).contains(e.getPoint())) {
                    if (!lisBoardsAvailable.isSelectedIndex(index)) {
                        lisBoardsAvailable.setSelectedIndex(index);
                    }
                    showPopup(e);
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (popup == null) {
                return;
            }
            Point p = e.getLocationOnScreen();
            if (!popup.contains(p)) {
                closePopup();
            }
        }

        private void closePopup() {
            if (popup == null) {
                return;
            }
            popup.setVisible(false);
            popup = null;
        }

        /** Shows the map selection menu on the map table */
        private void showPopup(MouseEvent e) {
            if (lisBoardsAvailable.isSelectionEmpty()) {
                return;
            }
            List<String> boards = lisBoardsAvailable.getSelectedValuesList();
            int activeButtons = mapSettings.getMapWidth() * mapSettings.getMapHeight();
            boolean enableRotation = (mapSettings.getBoardWidth() % 2) == 0;
            popup = MapListPopup.mapListPopup(boards, activeButtons, this, ChatLounge.this, enableRotation);
            popup.show(e.getComponent(), e.getX() + MAP_POPUP_OFFSET, e.getY() + MAP_POPUP_OFFSET);
        }
    }
    
    public class MekForceTreeMouseAdapter extends MouseInputAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = mekForceTree.getRowForLocation(e.getX(), e.getY());
                TreePath path = mekForceTree.getPathForRow(row);
                if (path != null && path.getLastPathComponent() instanceof Entity) {
                    Entity entity = (Entity) path.getLastPathComponent();
                    lobbyActions.customizeMech(entity);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                // If the right mouse button is pressed over an unselected entity,
                // clear the selection and select that entity instead
                int row = mekForceTree.getRowForLocation(e.getX(), e.getY());
                if (!mekForceTree.isRowSelected(row)) {
                    mekForceTree.setSelectionRow(row);
                }
                showPopup(e);
            }
        }

        /** Shows the right-click menu on the mek table */
        private void showPopup(MouseEvent e) {
            TreePath[] selection = mekForceTree.getSelectionPaths();
            List<Entity> entities = new ArrayList<>();
            List<Force> selForces = new ArrayList<>();
            
            if (selection != null) {
                for (TreePath path: selection) {
                    if (path != null) {
                        Object selected = path.getLastPathComponent();
                        if (selected instanceof Entity) {
                            entities.add((Entity) selected);
                        } else if (selected instanceof Force) {
                            selForces.add((Force) selected);
                        } 
                    }
                }
            }
            ScalingPopup popup = LobbyMekPopup.getPopup(entities, selForces, new LobbyMekPopupActions(ChatLounge.this), ChatLounge.this);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public class MekTableMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = mekTable.rowAtPoint(e.getPoint());
                InGameObject entity = mekModel.getEntityAt(row);
                if ((entity instanceof Entity) && isEditable((Entity) entity)) {
                    lobbyActions.customizeMech((Entity) entity);
                }
            }
        }

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

        @Override
        public void mousePressed(MouseEvent e) {
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

        /** Shows the right-click menu on the mek table */
        private void showPopup(MouseEvent e) {
            if (mekTable.getSelectedRowCount() == 0) {
                return;
            }
            List<Entity> entities = getSelectedEntities();
            ScalingPopup popup = LobbyMekPopup.getPopup(entities, new ArrayList<>(), new LobbyMekPopupActions(ChatLounge.this), ChatLounge.this);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /** Refreshes the Mek Tree, restoring expansion state and selection. */
    private void refreshTree() {
        // Refresh the force tree and restore selection/expand status
        HashSet<Object> selections = new HashSet<>();
        if (!mekForceTree.isSelectionEmpty()) {
            for (TreePath path: mekForceTree.getSelectionPaths()) {
                Object sel = path.getLastPathComponent();
                if (sel instanceof Force || sel instanceof Entity) {
                    selections.add(path.getLastPathComponent());
                }
            }
        }
        
        Forces forces = game().getForces();
        List<Integer> expandedForces = new ArrayList<>();
        for (int i = 0; i < mekForceTree.getRowCount(); i++) {
            TreePath currPath = mekForceTree.getPathForRow(i);
            if (mekForceTree.isExpanded(currPath)) {
                Object entry = currPath.getLastPathComponent();
                if (entry instanceof Force) {
                    expandedForces.add(((Force) entry).getId());
                }
            }
        }
        
        mekForceTree.setUI(null);
        try {
            mekForceTreeModel.refreshData();
        } finally {
            mekForceTree.updateUI();
        }
        for (int id: expandedForces) {
            if (!forces.contains(id)) {
                continue;
            }
            mekForceTree.expandPath(getPath(forces.getForce(id)));
        }

        mekForceTree.clearSelection();
        for (Object sel: selections) {
            mekForceTree.addSelectionPath(getPath(sel));
        }

    }
    
    /** 
     * Returns a TreePath in the force tree for a possibly outdated entity
     * or force. Outdated means a new object of the type was sent by the server
     * and has replaced this object. Also works for the game's current objects though. 
     * Uses the force's/entity's id to get the 
     * game's real object with the same id. Used to reconstruct the selection
     * and expansion state of the force tree after an update.
     */
    private TreePath getPath(Object outdatedEntry) {
        Forces forces = game().getForces();
        if (outdatedEntry instanceof Force) {
            if (!forces.contains((Force) outdatedEntry)) {
                return null;
            }
            int forceId = ((Force) outdatedEntry).getId();
            List<Force> chain = forces.forceChain(forces.getForce(forceId));
            Object[] pathObjs = new Object[chain.size() + 1];
            int index = 0;
            pathObjs[index++] = mekForceTreeModel.getRoot();
            for (Force force: chain) {
                pathObjs[index++] = force;
            }
            return new TreePath(pathObjs);
        } else if (outdatedEntry instanceof Entity) {
            int entityId = ((Entity) outdatedEntry).getId();
            if (game().getEntity(entityId) == null) {
                return null;
            }
            List<Force> chain = forces.forceChain(game().getEntity(entityId));
            Object[] pathObjs = new Object[chain.size() + 2];
            int index = 0;
            pathObjs[index++] = mekForceTreeModel.getRoot();
            for (Force force: chain) {
                pathObjs[index++] = force;
            }
            pathObjs[index++] = game().getEntity(entityId);
            return new TreePath(pathObjs);
        } else {
            throw new IllegalArgumentException(Messages.getString("ChatLounge.TreePath.methodRequiresEntityForce"));
        }
    }
    
    /** 
     * Returns a Collection that contains only those of the given entities
     * that the local player can affect, i.e. his units or those of his bots. 
     * The returned Collection is a new Collection and can be safely altered.
     * (The entities are not copies of course.)
     * <P>See also {@link #isEditable(Entity)} 
     */
    private Set<Entity> editableEntities(Collection<Entity> entities) {
        return entities.stream().filter(this::isEditable).collect(Collectors.toSet());
    }
    
   
    /** 
     * Returns true if the given carrier and carried can be edited to have the 
     * carrier transport the given carried entity. That is the case when they 
     * are teammates and one of the entities can be edited by the local player. 
     * Note: this method does NOT check if the loading is rules-valid.
     * <P>See also {@link #isEditable(Entity)}
     */
    private boolean isLoadable(Entity carried, Entity carrier) {
        return !carrier.getOwner().isEnemyOf(carried.getOwner()) 
                && (isEditable(carrier) || isEditable(carried));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        switch (e.getName()) {
            case GUIPreferences.GUI_SCALE:
                adaptToGUIScale();
                break;
            case ClientPreferences.SHOW_UNIT_ID:
                setButUnitIDState();
                mekModel.refreshCells();
                refreshTree();
                break;
            case GUIPreferences.USE_CAMO_OVERLAY:
                clientgui.getBoardView().getTilesetManager().reloadUnitIcons();
                mekModel.refreshCells();
                refreshTree();
                break;
        }
    }
    
    /** Silently adapts the state of the "Show IDs" button to the Client prefs. */
    private void setButUnitIDState() {
        butShowUnitID.removeActionListener(lobbyListener);
        butShowUnitID.setSelected(PreferenceManager.getClientPreferences().getShowUnitId());
        butShowUnitID.addActionListener(lobbyListener);
    }
    
    /** Sets the row height of the MekTable according to compact mode and GUI scale */
    private void setTableRowHeights() {
        int rowbaseHeight = butCompact.isSelected() ? MEKTABLE_ROWHEIGHT_COMPACT : MEKTABLE_ROWHEIGHT_FULL;
        mekTable.setRowHeight(UIUtil.scaleForGUI(rowbaseHeight));
        rowbaseHeight = butCompact.isSelected() ? MEKTABLE_ROWHEIGHT_COMPACT : MEKTREE_ROWHEIGHT_FULL;
        mekForceTree.setRowHeight(UIUtil.scaleForGUI(rowbaseHeight));
        tablePlayers.rescale();
    }

    /** Refreshes the table headers of the MekTable and PlayerTable. */
    private void updateTableHeaders() {
        // The mek table
        JTableHeader header = mekTable.getTableHeader();
        TableColumnModel colMod = header.getColumnModel();
        for (int i = 0; i < colMod.getColumnCount(); i++) {
            TableColumn tabCol = colMod.getColumn(i);
            String headerText = mekModel.getColumnName(i);
            // Add info about the current sorting
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
        header.repaint();
        
        // The player table
        header = tablePlayers.getTableHeader();
        colMod = header.getColumnModel();
        for (int i = 0; i < colMod.getColumnCount(); i++) {
            TableColumn tabCol = colMod.getColumn(i);
            tabCol.setHeaderValue(playerModel.getColumnName(i));
        }
        header.repaint();
    }

    /** Returns the owner of the given entity. Should be used over entity.getowner(). */
    private Player ownerOf(Entity entity) {
        return clientgui.getClient().getGame().getPlayer(entity.getOwnerId());
    }
    
    /** Sets the column width of the given table column of the MekTable with the value stored in the GUIP. */
    private void setColumnWidth(TableColumn column) {
        String key;
        if (column.getModelIndex() == MekTableModel.COL_PILOT) {
            key = GUIPreferences.LOBBY_MEKTABLE_PILOT_WIDTH;
        } else if (column.getModelIndex() == MekTableModel.COL_UNIT) {
            key = GUIPreferences.LOBBY_MEKTABLE_UNIT_WIDTH;
        } else if (column.getModelIndex() == MekTableModel.COL_PLAYER) {
            key = GUIPreferences.LOBBY_MEKTABLE_PLAYER_WIDTH;
        } else if (column.getModelIndex() == MekTableModel.COL_BV) {
            key = GUIPreferences.LOBBY_MEKTABLE_BV_WIDTH;
        } else {
            return;
        }
        column.setPreferredWidth(GUIP.getInt(key));
    }
    
    /** Adapts the whole Lobby UI (both panels) to the current guiScale. */
    private void adaptToGUIScale() {
        updateTableHeaders();
        refreshLabels();
        refreshCamoButton();
        refreshMapButtons();
        mekModel.refreshCells();

        Font scaledFont = UIUtil.getScaledFont();

        UIUtil.adjustContainer(splitPaneMain, UIUtil.FONT_SCALE1);
        UIUtil.scaleComp(butDone, UIUtil.FONT_SCALE2);
        UIUtil.scaleComp(butOptions, UIUtil.FONT_SCALE2);
        UIUtil.scaleComp(butAdd, UIUtil.FONT_SCALE2);
        UIUtil.scaleComp(butArmy, UIUtil.FONT_SCALE2);


        setTableRowHeights();

        String searchTip = Messages.getString("ChatLounge.map.searchTip") + "<BR>";
        searchTip += autoTagHTMLTable();
        fldSearch.setToolTipText(UIUtil.scaleStringForGUI(searchTip));
        
        ((TitledBorder) panUnitInfo.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder) panPlayerInfo.getBorder()).setTitleFont(scaledFont);
        
        int scaledBorder = UIUtil.scaleForGUI(TEAMOVERVIEW_BORDER);
        panTeam.setBorder(new EmptyBorder(scaledBorder, scaledBorder, scaledBorder, scaledBorder));

        butBoardPreview.setToolTipText(scaleStringForGUI(Messages.getString("BoardSelectionDialog.ViewGameBoardTooltip")));
        butSaveMapSetup.setToolTipText(scaleStringForGUI(Messages.getString("ChatLounge.map.saveMapSetupTip")));

        Font scaledHelpFont = new Font(MMConstants.FONT_DIALOG, Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 33));
        butHelp.setFont(scaledHelpFont);

        // Makes a new tooltip appear immediately (rescaled and possibly for a different unit)
        ToolTipManager manager = ToolTipManager.sharedInstance();
        long time = System.currentTimeMillis() - manager.getInitialDelay() + 1;
        Point locationOnScreen = MouseInfo.getPointerInfo().getLocation();
        Point locationOnComponent = new Point(locationOnScreen);
        SwingUtilities.convertPointFromScreen(locationOnComponent, mekTable);
        MouseEvent event = new MouseEvent(mekTable, -1, time, 0, 
                locationOnComponent.x, locationOnComponent.y, 0, 0, 1, false, 0);
        manager.mouseMoved(event);
    }
    
    private String autoTagHTMLTable() {
        String result = "<TABLE><TR>"+ UIUtil.guiScaledFontHTML();
        int colCount = 0;
        var autoTags = BoardsTagger.Tags.values();
        for (BoardsTagger.Tags tag : autoTags) {
            if (colCount == 0) {
                result += "<TR>";
            }
            result += "<TD>" + tag.getName() + "</TD>";
            colCount++;
            if (colCount == 3) {
                colCount = 0;
                result += "</TR>";
            }
        }
        if (colCount != 0) {
            result += "</TR>";
        }
        result += "</TABLE>";
        return result;
    }
    
    
    /** 
     * Mouse Listener for the table header of the Mek Table.
     * Saves column widths of the Mek Table when the mouse button is released. 
     * Also switches between table sorting types
     */
    MouseListener mekTableHeaderMouseListener = new MouseAdapter() {
        private void changeSorter(MouseEvent e) {
            // Save table widths
            for (int i = 0; i < MekTableModel.N_COL; i++) {
                TableColumn column = mekTable.getColumnModel().getColumn(i);
                String key;
                if (column.getModelIndex() == MekTableModel.COL_PILOT) {
                    key = GUIPreferences.LOBBY_MEKTABLE_PILOT_WIDTH;
                } else if (column.getModelIndex() == MekTableModel.COL_UNIT) {
                    key = GUIPreferences.LOBBY_MEKTABLE_UNIT_WIDTH;
                } else if (column.getModelIndex() == MekTableModel.COL_PLAYER) {
                    key = GUIPreferences.LOBBY_MEKTABLE_PLAYER_WIDTH;
                } else if (column.getModelIndex() == MekTableModel.COL_BV) {
                    key = GUIPreferences.LOBBY_MEKTABLE_BV_WIDTH;
                } else {
                    continue;
                }
                GUIP.setValue(key, column.getWidth());
            }
            
            changeMekTableSorter(e);
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                e.consume();
                sorterPopup(e);
            } else {
                changeSorter(e);
            }
        }
        
        private void sorterPopup(MouseEvent e) {
            ScalingPopup popup = new ScalingPopup();
            GameOptions opts = clientgui.getClient().getGame().getOptions();
            for (MekTableSorter sorter: union(unitSorters, bvSorters)) {
                // Offer only allowed sorters and only one sorting direction
                if (sorter.isAllowed(opts) && sorter.getSortingDirection() != Sorting.ASCENDING) {
                    JMenuItem item = new JMenuItem(sorter.getDisplayName());
                    item.addActionListener(mekTableHeaderAListener);
                    item.setActionCommand(sorter.getDisplayName());
                    popup.add(item);
                }
            }
            popup.show(e.getComponent(), e.getX(), e.getY());
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
        
        // Select the next allowed sorter and refresh the display if the sorter was changed
        nextSorter(sorters);
        if (activeSorter != previousSorter) {
            refreshMekTable();
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
        
        // Find an allowed sorter (e.g. blind drop may prohibit some)
        int counter = 0; // Endless loop safeguard
        while (!activeSorter.isAllowed(clientgui.getClient().getGame().getOptions())
                && ++counter < 100) {
            index = (index + 1) % sorters.size();
            activeSorter = sorters.get(index);
        }
    }

    /** Returns true when the compact view is active. */ 
    public boolean isCompact() {
        return butCompact.isSelected();
    }
    
    /** 
     * Returns a list of the selected entities in the Mek table. 
     * The list may be empty but not null. 
     */
    private List<Entity> getSelectedEntities() {
        ArrayList<Entity> result = new ArrayList<>();
        int[] rows = mekTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            InGameObject unit = mekModel.getEntityAt(rows[i]);
            if (unit instanceof Entity) {
                result.add((Entity) unit);
            }
        }
        return result;
    }
    
    /** Helper method to shorten calls. */
    Player localPlayer() {
        return clientgui.getClient().getLocalPlayer();
    }
    
    private void redrawMapTable(Image image) {
        if (image != null) {
            if (lisBoardsAvailable.getFixedCellHeight() != image.getHeight(null) 
                    || lisBoardsAvailable.getFixedCellWidth() != image.getWidth(null)) {
                lisBoardsAvailable.setFixedCellHeight(image.getHeight(null));
                lisBoardsAvailable.setFixedCellWidth(image.getWidth(null));
            }
            lisBoardsAvailable.repaint();
        }
    }

    class ImageLoader extends SwingWorker<Void, Image> {

        private BlockingQueue<String> boards = new LinkedBlockingQueue<>();

        private synchronized void add(String name) {
            if (!boards.contains(name)) {
                try {
                    boards.put(name);
                } catch (Exception e) {
                    LogManager.getLogger().error("", e);
                }
            }
        }
        
        private Image prepareImage(String boardName) {
            File boardFile = new MegaMekFile(Configuration.boardsDir(), boardName + CL_KEY_FILEEXTENTION_BOARD).getFile();
            Board board;
            StringBuffer errs = new StringBuffer();
            if (boardFile.exists()) {
                board = new Board();
                try (InputStream is = new FileInputStream(boardFile)) {
                    board.load(is, errs, true);
                } catch (IOException ex) {
                    board = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
                }
            } else {
                board = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
            }

            // Determine a minimap zoom from the board size and gui scale.
            // This is very magic numbers but currently the minimap has only fixed zoom states.
            int largerEdge = Math.max(board.getWidth(), board.getHeight());
            int zoom = 3;
            if (largerEdge < 17) {
                zoom = 4;
            }
            if (largerEdge > 20) {
                zoom = 2;
            }
            if (largerEdge > 30) {
                zoom = 1;
            }
            if (largerEdge > 40) {
                zoom = 0;
            }
            if (board.getWidth() < 25) {
                zoom = Math.max(zoom, 3);
            }
            float scale = GUIP.getGUIScale();
            zoom = (int) (scale*zoom);
            if (zoom > 6) {
                zoom = 6;
            }
            if (zoom < 0) {
                zoom = 0;
            }
            BufferedImage bufImage = Minimap.getMinimapImage(board, zoom);

            // Add the board name label and the server-side board label if necessary
            String text = LobbyUtility.cleanBoardName(boardName, mapSettings);
            Graphics g = bufImage.getGraphics();
            if (errs.length() != 0) {
                invalidBoards.add(boardName);
            }
            drawMinimapLabel(text, bufImage.getWidth(), bufImage.getHeight(), g, errs.length() != 0);
            if (!boardFile.exists() && !boardName.startsWith(MapSettings.BOARD_GENERATED)) {
                serverBoards.add(boardName);
                markServerSideBoard(bufImage);
            }
            g.dispose();

            synchronized(baseImages) {
                baseImages.put(boardName, bufImage);
            }
            return bufImage;
        }


        @Override
        protected Void doInBackground() throws Exception {
            Image image;
            while (!isCancelled()) {
                // Create thumbnails for the MapSettings boards
                String boardName = boards.poll(1, TimeUnit.SECONDS);
                if (boardName != null && !baseImages.containsKey(boardName)) {
                    image = prepareImage(boardName);
                    ChatLounge.this.redrawMapTable(image);
                }
            }
            return null;
        }
    }

    Map<String, ImageIcon> mapIcons = new HashMap<>();
    
    /** A renderer for the list of available boards. */
    public class BoardNameRenderer extends DefaultListCellRenderer  {
        private static final long serialVersionUID = -3218595828938299222L;
        
        private float oldGUIScale = GUIP.getGUIScale();
        private Image image;
        private ImageIcon icon;
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {

            String board = (String) value;
            // For generated boards, add the size to have different images for different sizes
            if (board.startsWith(MapSettings.BOARD_GENERATED)) {
                board += mapSettings.getBoardSize();
            }
            
            // If the gui scaling has changed, clear out all images, triggering a reload
            float currentGUIScale = GUIP.getGUIScale();
            if (currentGUIScale != oldGUIScale) {
                oldGUIScale = currentGUIScale;
                mapIcons.clear();
                synchronized (baseImages) {
                    baseImages.clear();
                }
            }
            
            // If an icon is present for the current board, use it
            icon = mapIcons.get(board);
            if (icon != null) {
                setIcon(icon);
            } else {
                // The icon is not present, see if there's a base image
                synchronized (baseImages) {
                    image = baseImages.get(board);
                }
                if (image == null) {
                    // There's no base image: trigger loading it and, for now, return the base list's panel
                    // The [GENERATED] entry will always land here as well
                    loader.add(board);
                    setToolTipText(null);
                    return super.getListCellRendererComponent(list, new File(board).getName(), index, isSelected, cellHasFocus);
                } else {
                    // There is a base image: make it into an icon, store it and use it
                    if (!lisBoardsAvailable.isEnabled()) {
                        ImageFilter filter = new GrayFilter(true, 50);  
                        ImageProducer producer = new FilteredImageSource(image.getSource(), filter);  
                        image = Toolkit.getDefaultToolkit().createImage(producer); 
                    }
                    icon = new ImageIcon(image);
                    
                    mapIcons.put(board, icon);
                    setIcon(icon);
                }
            }
            
            // Found or created an icon; finish the panel
            setText("");
            if (lisBoardsAvailable.isEnabled()) {
                setToolTipText(scaleStringForGUI(createBoardTooltip(board)));
            } else {
                setToolTipText(null);
            }
            
            if (isSelected) {
                setForeground(list.getSelectionForeground());
                setBackground(list.getSelectionBackground());
            } else {
                setForeground(list.getForeground());
                setBackground(list.getBackground());
            }
            
            return this;
        }
    }

    private class MekTable extends JTable {
        private static final long serialVersionUID = -4054214297803021212L;
        
        public MekTable(MekTableModel mekModel) {
            super(mekModel);
        }

        /**
         * Places the tooltips to the right of the cell so it doesn't get in the way.
         */
        @Override
        public Point getToolTipLocation(MouseEvent event) {
            int row = rowAtPoint(event.getPoint());
            int col = columnAtPoint(event.getPoint());
            if (col == MekTableModel.COL_UNIT || col == MekTableModel.COL_PILOT) {
                Rectangle cellRect = getCellRect(row, col, true);
                int x = cellRect.x + cellRect.width + 10;
                int y = cellRect.y + 10;
                return new Point(x, y);
            } else {
                // If this is not done for the other cols, small empty tooltips appear!
                return super.getToolTipLocation(event);
            }
        }
    }

    void updateMapButtons() {
        boolean haveImages = false;
        for (MapPreviewButton button: mapButtons) {
            button.scheduleRescale();
            haveImages |= button.hasBoard();
        }
        if (!haveImages) {
            Dimension size  = maxMapButtonSize();
            for (MapPreviewButton button: mapButtons) {
                button.setPreviewSize(size);
            }   
        }
    }
    
    void updateMapButtons(Dimension size) {
        if (!currentMapButtonSize.equals(size)) {
            currentMapButtonSize = size;
            for (MapPreviewButton button: mapButtons) {
                button.setPreviewSize(size);
            }
        }
    }

    Dimension maxMapButtonSize() {
        // minus 1 to ensure that the images actually fit in the frame
        double pw = (double) panMapButtons.getWidth() / mapSettings.getMapWidth() - 1;
        double ph = (double) panMapButtons.getHeight() / mapSettings.getMapHeight() - 1;
        return new Dimension((int) pw, (int) ph);
    }

    Dimension optMapButtonSize(Image image) {
        Dimension optSize = maxMapButtonSize();
        double factorX = (double) optSize.width / image.getWidth(null);
        double factorY = (double) optSize.height / image.getHeight(null);
        double factor = Math.min(factorX, factorY);
        int w = (int) (factor * image.getWidth(null));
        int h = (int) (factor * image.getHeight(null));
        return new Dimension(w, h);
    }
    
    /** 
     * Returns true when the string boardName contains an invalid board. boardName may
     * denote a generated board (which is never invalid) or a surprise board
     * with several actual board names attached which will return true when at least
     * one of the boards is invalid.
     */
    boolean hasInvalidBoard(String boardName) {
        return hasSpecialBoard(boardName, invalidBoards);
    }
    
    /** 
     * Returns true when the string boardName contains a board that isn't present on 
     * the client (only on the server). boardName may denote a generated board 
     * (which is never serverside) or a surprise board with several actual board names 
     * attached which will return true when at least one of the boards is serverside.
     */
    boolean hasServerSideBoard(String boardName) {
        return hasSpecialBoard(boardName, serverBoards);
    }
    
    /** 
     * Returns true when boardName (if a single board) or any of the boards contained
     * in boardName (if a surprise board list) is contained in the provided list. Returns
     * false for generated boards.
     */
    private boolean hasSpecialBoard(String boardName, Collection<String> list) {
        if (boardName.startsWith(MapSettings.BOARD_GENERATED)) {
            return false;
        } else if (boardName.startsWith(MapSettings.BOARD_SURPRISE)) {
            return !Collections.disjoint(extractSurpriseMaps(boardName), list);
        } else {
            return list.contains(boardName);
        }
    }

    /** 
     * Returns a tooltip for the provided boardName that may be a single board or 
     * a generated or surprise board. Adds info for serverside or invalid boards.
     */
    String createBoardTooltip(String boardName) {
        String result;
        if (boardName.startsWith(MapSettings.BOARD_GENERATED)) {
            result = Messages.getString("ChatLounge.board.generatedMessage");
        } else if (boardName.startsWith(MapSettings.BOARD_SURPRISE)) {
            result = Messages.getString("ChatLounge.board.randomlySelectedMessage");
            result += boardName.substring(MapSettings.BOARD_SURPRISE.length()).replace("\n", "<BR>");
        } else {
            result = boardName;
        }

        if (hasInvalidBoard(boardName)) {
            result += invalidBoardTip();
        }

        if (hasServerSideBoard(boardName)) {
            result += Messages.getString("ChatLounge.map.serverSideTip");
        }

        return result;
    }
    
    ActionListener mekTableHeaderAListener = e ->  {
        MekTableSorter previousSorter = activeSorter;
        for (MekTableSorter sorter: union(unitSorters, bvSorters)) {
            if (e.getActionCommand().equals(sorter.getDisplayName())) {
                activeSorter = sorter;
                break;
            }
        }
        if (activeSorter != previousSorter) {
            refreshEntities();
            updateTableHeaders();
        }
    };
    
    Game game() {
        return clientgui.getClient().getGame();
    }
    
    /** Convenience for clientgui.getClient() */
    Client client() {
        return clientgui.getClient();
    }
    
    boolean isForceView() {
        return butForceView.isSelected();
    }
    
    public void killPreviewBV() {
        if (previewBV != null) {
            previewBV.die();
        }
    }
}

