/*
 * Copyright (C) 2000-2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.containsTransportedUnit;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.drawMinimapLabel;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.extractSurpriseMaps;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.haveSingleOwner;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.invalidBoardTip;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.isBoardFile;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.isValidStartPos;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.client.ui.util.UIUtil.setHighQualityRendering;
import static megamek.client.ui.util.UIUtil.uiGray;
import static megamek.common.util.CollectionUtil.theElement;
import static megamek.common.util.CollectionUtil.union;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import megamek.MMConstants;
import megamek.SuiteConstants;
import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.DialogButton;
import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.IMapSettingsObserver;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.toolTip.TWBoardViewTooltip;
import megamek.client.ui.dialogs.InformDialog;
import megamek.client.ui.dialogs.MMDialogs.MMConfirmDialog;
import megamek.client.ui.dialogs.RulerDialog;
import megamek.client.ui.dialogs.abstractDialogs.AutoResolveChanceDialog;
import megamek.client.ui.dialogs.abstractDialogs.AutoResolveProgressDialog;
import megamek.client.ui.dialogs.advancedSearchMap.AdvancedSearchMapDialog;
import megamek.client.ui.dialogs.buttonDialogs.BotConfigDialog;
import megamek.client.ui.dialogs.buttonDialogs.SkillGenerationDialog;
import megamek.client.ui.dialogs.clientDialogs.ClientDialog;
import megamek.client.ui.dialogs.clientDialogs.PlanetaryConditionsDialog;
import megamek.client.ui.dialogs.helpDialogs.AutoResolveSimulationLogDialog;
import megamek.client.ui.dialogs.iconChooser.CamoChooserDialog;
import megamek.client.ui.dialogs.minimap.MinimapPanel;
import megamek.client.ui.dialogs.randomMap.RandomMapDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.phaseDisplay.AbstractPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.lobby.PlayerTable.PlayerTableModel;
import megamek.client.ui.panels.phaseDisplay.lobby.sorters.*;
import megamek.client.ui.util.ScalingPopup;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedXPanel;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.client.ui.widget.SkinSpecification;
import megamek.common.Configuration;
import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.autoResolve.converter.MMSetupForces;
import megamek.common.board.Board;
import megamek.common.board.BoardDimensions;
import megamek.common.board.postprocess.TWBoardTransformer;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.BombLoadout;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.player.GamePlayerChangeEvent;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.game.InGameObject;
import megamek.common.internationalization.I18n;
import megamek.common.loaders.MapSettings;
import megamek.common.loaders.MapSetup;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.IBomber;
import megamek.common.util.BoardUtilities;
import megamek.common.util.CollectionUtil;
import megamek.common.util.CrewSkillSummaryUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.server.ServerBoardHelper;

public class ChatLounge extends AbstractPhaseDisplay
      implements ListSelectionListener, IMapSettingsObserver, IPreferenceChangeListener {
    private static final MMLogger LOGGER = MMLogger.create(ChatLounge.class);

    @Serial
    private static final long serialVersionUID = 1454736776730903786L;

    // UI display control values
    static final int MEK_TABLE_ROW_HEIGHT_COMPACT = 20;
    static final int MEK_TABLE_ROW_HEIGHT_FULL = 65;
    static final int MEK_TREE_ROW_HEIGHT_FULL = 40;
    private static final int MAP_POPUP_OFFSET = -2; // a slight offset so cursor sits inside popup
    public static final String HEADER_TEXT_ARROW_UP = "\u25B4 ";
    public static final String HEADER_TEXT_ARROW_DOWN = "\u25BE ";

    private final JTabbedPane panTabs = new JTabbedPane();
    private final JPanel panUnits = new JPanel();
    private final JPanel panMap = new JPanel();
    private final JPanel panTeam = new JPanel();

    // Labels
    private final JLabel lblMapSummary = new JLabel("");
    private final JLabel lblGameYear = new JLabel("");
    private final JLabel lblTechLevel = new JLabel("");

    // Game Setup
    private final JButton butOptions = new JButton(Messages.getString("ChatLounge.butOptions"));
    private final JToggleButton butGroundMap = new JToggleButton(Messages.getString("ChatLounge.butGroundMap"));
    private final JToggleButton butLowAtmosphereMap = new JToggleButton(Messages.getString(
          "ChatLounge.name.lowAltitudeMap"));
    private final JToggleButton butHighAtmosphereMap = new JToggleButton(Messages.getString(
          "ChatLounge.name.HighAltitudeMap"));
    private final JToggleButton butSpaceMap = new JToggleButton(Messages.getString("ChatLounge.name.spaceMap"));
    private final ButtonGroup grpMap = new ButtonGroup();

    /* Unit Configuration Panel */
    private final FixedYPanel panUnitInfo = new FixedYPanel();
    private final JButton butAdd = new JButton(Messages.getString("ChatLounge.butLoad"));
    private final JButton butArmy = new JButton(Messages.getString("ChatLounge.butArmy"));
    private final JButton butSkills = new JButton(Messages.getString("ChatLounge.butSkills"));
    private final JButton butNames = new JButton(Messages.getString("ChatLounge.butNames"));
    private final JButton butLoadList = new JButton(Messages.getString("ChatLounge.butLoadList"));
    private final JButton butSaveList = new JButton(Messages.getString("ChatLounge.butSaveList"));
    private final JButton butPrintList = new JButton(Messages.getString("ChatLounge.butPrintList"));

    /* Unit Table */
    private MekTable mekTable;
    public JScrollPane scrMekTable;
    private final MMToggleButton butCompact = new MMToggleButton(Messages.getString("ChatLounge.butCompact"));
    private final MMToggleButton butShowUnitID = new MMToggleButton(Messages.getString("ChatLounge.butShowUnitID"));
    private final JToggleButton butListView = new JToggleButton(Messages.getString("ChatLounge.butSortableView"));
    private final JToggleButton butForceView = new JToggleButton(Messages.getString("ChatLounge.butForceView"));
    private final JButton butCollapse = new JButton(Messages.getString("ChatLounge.butCollapse"));
    private final JButton butExpand = new JButton(Messages.getString("ChatLounge.butExpand"));
    private MekTableModel mekModel;

    /* Force Tree */
    private MekTreeForceModel mekForceTreeModel;
    JTree mekForceTree;
    private final transient MekForceTreeMouseAdapter mekForceTreeMouseListener = new MekForceTreeMouseAdapter();

    /* Player Configuration Panel */
    private FixedYPanel panPlayerInfo;
    private final JComboBox<String> comboTeam = new JComboBox<>();
    private final JButton butCamo = new JButton();
    private final JButton butAddBot = new JButton(Messages.getString("ChatLounge.butAddBot"));
    private final JButton butRemoveBot = new JButton(Messages.getString("ChatLounge.butRemoveBot"));
    private final JButton butConfigPlayer = new JButton(Messages.getString("ChatLounge.butConfigPlayer"));
    private final JButton butBotSettings = new JButton(Messages.getString("ChatLounge.butBotSettings"));
    PlayerSettingsDialog psd;

    private final transient MekTableMouseAdapter mekTableMouseAdapter = new MekTableMouseAdapter();
    private final PlayerTableModel playerModel = new PlayerTableModel();
    private final PlayerTable tablePlayers = new PlayerTable(playerModel, this);
    private final JScrollPane scrPlayers = new JScrollPane(tablePlayers);

    /* ACAR Settings Panel */
    private FixedYPanel panAutoResolveInfo;
    private final JButton butRunAutoResolve = new JButton(Messages.getString("ChatLounge.butRunAutoResolve"));
    private final JSpinner spnSimulationRuns = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
    private final JSpinner spnThreadNumber = new JSpinner(new SpinnerNumberModel(Runtime.getRuntime()
          .availableProcessors(),
          1,
          Math.max(2, Runtime.getRuntime().availableProcessors() * 2),
          1));
    private final JCheckBox chkAutoResolve = new JCheckBox();

    /* Map Settings Panel */
    private final JLabel lblMapWidth = new JLabel(Messages.getString("ChatLounge.labMapWidth"));
    private final JButton butMapGrowW = new JButton(Messages.getString("ChatLounge.butGrow"));
    private final JButton butMapShrinkW = new JButton(Messages.getString("ChatLounge.butShrink"));
    private final JTextField fldMapWidth = new JTextField(3);
    private final JLabel lblMapHeight = new JLabel(Messages.getString("ChatLounge.labMapHeight"));
    private final JButton butMapGrowH = new JButton(Messages.getString("ChatLounge.butGrow"));
    private final JButton butMapShrinkH = new JButton(Messages.getString("ChatLounge.butShrink"));
    private final JTextField fldMapHeight = new JTextField(3);
    private final FixedYPanel panMapHeight = new FixedYPanel();
    private final FixedYPanel panMapWidth = new FixedYPanel();

    private final JLabel lblSpaceBoardWidth = new JLabel(Messages.getString("ChatLounge.labBoardWidth"));
    private final JTextField fldSpaceBoardWidth = new JTextField(3);
    private final JLabel lblSpaceBoardHeight = new JLabel(Messages.getString("ChatLounge.labBoardHeight"));
    private final JTextField fldSpaceBoardHeight = new JTextField(3);
    private final FixedYPanel panSpaceBoardHeight = new FixedYPanel();
    private final FixedYPanel panSpaceBoardWidth = new FixedYPanel();

    private final JLabel lblBoardSize = new JLabel(Messages.getString("ChatLounge.labBoardSize"));
    private final JButton butHelp = new JButton(" " + Messages.getString("ChatLounge.butHelp") + " ");
    private final JButton butAdvancedSearchMap = new JButton(Messages.getString("AdvancedSearchMapDialog.title"));

    private final JButton butConditions = new JButton(Messages.getString("ChatLounge.butConditions"));
    private final JButton butRandomMap = new JButton(Messages.getString("BoardSelectionDialog.GeneratedMapSettings"));
    ArrayList<MapPreviewButton> mapButtons = new ArrayList<>(20);
    MapSettings mapSettings;
    private JPanel panGroundMap;

    private JComboBox<Comparable<?>> comMapSizes;
    private final JButton butBoardPreview = new JButton(Messages.getString("BoardSelectionDialog.ViewGameBoard"));
    private final JPanel panMapButtons = new JPanel();
    private final JLabel lblBoardsAvailable = new JLabel();
    private JList<String> lisBoardsAvailable;
    private final JButton butSpaceSize = new JButton(Messages.getString("ChatLounge.MapSize"));
    boolean resetAvailBoardSelection = false;
    boolean resetSelectedBoards = true;
    private ClientDialog boardPreviewW;
    private final Game boardPreviewGame = new Game();
    private transient BoardView previewBV;
    Dimension currentMapButtonSize = new Dimension(0, 0);
    private final JCheckBox showPlayerDeployment = new JCheckBox(Messages.getString("ChatLounge.showPlayerDeployment"));

    private final ArrayList<String> invalidBoards = new ArrayList<>();
    private final ArrayList<String> serverBoards = new ArrayList<>();

    private JSplitPane splGroundMap;
    private final JLabel lblSearch = new JLabel(Messages.getString("ChatLounge.labSearch"));
    private final JTextField fldSearch = new JTextField(10);
    private final JButton butCancelSearch = new JButton(Messages.getString("ChatLounge.butCancelSearch"));

    private transient MekTableSorter activeSorter;
    private final transient ArrayList<MekTableSorter> unitSorters = new ArrayList<>();
    private final transient ArrayList<MekTableSorter> bvSorters = new ArrayList<>();

    private final JButton butAddY = new JButton(Messages.getString("ChatLounge.butAdd"));
    private final JButton butAddX = new JButton(Messages.getString("ChatLounge.butAdd"));
    private final JButton butSaveMapSetup = new JButton(Messages.getString("ChatLounge.map.saveMapSetup") + " *");
    private final JButton butLoadMapSetup = new JButton(Messages.getString("ChatLounge.map.loadMapSetup"));

    /* Team Overview Panel */
    private TeamOverviewPanel panTeamOverview;
    JButton butDetach = new JButton(Messages.getString("ChatLounge.butDetach"));
    private final JSplitPane splitPaneMain;
    ClientDialog teamOverviewWindow;

    private transient ImageLoader loader;
    private final transient Map<String, Image> baseImages = new HashMap<>();

    private final transient MapListMouseAdapter mapListMouseListener = new MapListMouseAdapter();

    transient LobbyActions lobbyActions = new LobbyActions(this);

    private final Map<String, String> boardTags = new HashMap<>();

    transient LobbyKeyDispatcher lobbyKeyDispatcher = new LobbyKeyDispatcher(this);

    private static final String CL_KEY_FILE_EXTENSION_XML = ".xml";
    private static final String CL_KEY_FILEPATH_MAP_ASSEMBLY_HELP = "docs/help/en/Map and Board Stuff/MapAssemblyHelp.html";
    private static final String CL_KEY_FILEPATH_MAP_SETUP = "/mapsetup";
    private static final String CL_KEY_NAME_HELP_PANE = "helpPane";

    private static final String CL_ACTION_COMMAND_LOAD_LIST = "load_list";
    private static final String CL_ACTION_COMMAND_SAVE_LIST = "save_list";
    private static final String CL_ACTION_COMMAND_PRINT_LIST = "print_list";
    private static final String CL_ACTION_COMMAND_LOAD_MEK = "load_mek";
    private static final String CL_ACTION_COMMAND_ADD_BOT = "add_bot";
    private static final String CL_ACTION_COMMAND_REMOVE_BOT = "remove_bot";
    private static final String CL_ACTION_COMMAND_BOT_CONFIG = "BOTCONFIG";
    private static final String CL_ACTION_COMMAND_CONFIGURE = "CONFIGURE";
    private static final String CL_ACTION_COMMAND_AUTO_RESOLVE = "AUTORESOLVE";
    private static final String CL_ACTION_COMMAND_CAMO = "camo";

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();
    private transient ClientGUI clientgui;
    private boolean lobbySavePerformed = false;

    /** Creates a new chat lounge for the clientGUI.getClient(). */
    public ChatLounge(ClientGUI clientGUI) {
        super(clientGUI,
              SkinSpecification.UIComponents.ChatLounge.getComp(),
              SkinSpecification.UIComponents.ChatLoungeDoneButton.getComp());
        this.clientgui = clientGUI;
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
        setupAutoResolveConfig();
        refreshGameSettings();
        setupEntities();
        setupUnitConfig();
        setupUnitsPanel();
        setupMapPanel();
        refreshLabels();
        setupListeners();
    }

    public void setBottom(JComponent comp) {
        splitPaneMain.setBottomComponent(comp);
    }

    /** Sets up all the listeners that the lobby works with. */
    private void setupListeners() {
        // Make sure that no listeners are already registered from calling a refresh method
        removeAllListeners();

        GUIP.addPreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
        MekSummaryCache.getInstance().addListener(mekSummaryCacheListener);
        clientgui.getClient().getGame().addGameListener(this);
        clientgui.boardViews().forEach(bv -> bv.addBoardViewListener(this));

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
        butPrintList.addActionListener(lobbyListener);
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
        butLowAtmosphereMap.addActionListener(lobbyListener);
        butHighAtmosphereMap.addActionListener(lobbyListener);
        butSpaceMap.addActionListener(lobbyListener);
        butLoadMapSetup.addActionListener(lobbyListener);
        butSaveMapSetup.addActionListener(lobbyListener);
        butDetach.addActionListener(lobbyListener);
        butCancelSearch.addActionListener(lobbyListener);
        butHelp.addActionListener(lobbyListener);
        butAdvancedSearchMap.addActionListener(lobbyListener);
        butListView.addActionListener(lobbyListener);
        butForceView.addActionListener(lobbyListener);
        butCollapse.addActionListener(lobbyListener);
        butExpand.addActionListener(lobbyListener);
        butRunAutoResolve.addActionListener(lobbyListener);

        fldMapWidth.addActionListener(lobbyListener);
        fldMapHeight.addActionListener(lobbyListener);
        fldMapWidth.addFocusListener(focusListener);
        fldMapHeight.addFocusListener(focusListener);
        fldSpaceBoardWidth.addActionListener(lobbyListener);
        fldSpaceBoardHeight.addActionListener(lobbyListener);
        fldSpaceBoardWidth.addFocusListener(focusListener);
        fldSpaceBoardHeight.addFocusListener(focusListener);

        comboTeam.addActionListener(lobbyListener);

        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.addKeyEventDispatcher(lobbyKeyDispatcher);
    }

    /** Applies changes to the board and map size when the text fields lose focus. */
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
    transient ActionListener camoListener = e -> {
        // Show the CamoChooser for the selected player
        if (getSelectedClient() == null) {
            return;
        }
        Player player = getSelectedClient().getLocalPlayer();
        CamoChooserDialog ccd = new CamoChooserDialog(clientgui.getFrame(), player.getCamouflage());
        try {
            java.util.List<Entity> playerEntities = game().getPlayerEntities(player, false);
            if (!playerEntities.isEmpty()) {
                ccd.setDisplayedEntity(CollectionUtil.anyOneElement(playerEntities));
            }
            // If the dialog was canceled or nothing selected, do nothing
            if (!ccd.showDialog().isConfirmed()) {
                return;
            }

            // Update the player from the camo selection
            player.setCamouflage(ccd.getSelectedItem());
            butCamo.setIcon(player.getCamouflage().getImageIcon());
            getSelectedClient().sendPlayerInfo();
        } finally {
            ccd.dispose();
        }
    };

    private void setupTeamOverview() {
        panTeamOverview = new TeamOverviewPanel(clientgui);
        FixedYPanel panDetach = new FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        panDetach.add(butDetach);

        panTeam.setLayout(new BoxLayout(panTeam, BoxLayout.PAGE_AXIS));
        panTeam.add(panDetach);
        panTeam.add(panTeamOverview);

        // setup (but don't show) the detached team overview window
        teamOverviewWindow = new ClientDialog(clientgui.getFrame(),
              Messages.getString("ChatLounge.name.teamOverview"),
              false);
        teamOverviewWindow.setSize(clientgui.getFrame().getWidth() / 2, clientgui.getFrame().getHeight() / 2);
    }

    /**
     * Re-attaches the Team Overview panel to the tab when the detached window is closed.
     */
    transient WindowListener teamOverviewWindowListener = new WindowAdapter() {
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
        unitSorters.add(new PlayerTonnageSorter(clientgui, Sorting.ASCENDING));
        unitSorters.add(new PlayerTonnageSorter(clientgui, Sorting.DESCENDING));
        unitSorters.add(new PlayerUnitRoleSorter(clientgui, Sorting.ASCENDING));
        unitSorters.add(new PlayerUnitRoleSorter(clientgui, Sorting.DESCENDING));
        unitSorters.add(new IDSorter(Sorting.ASCENDING));
        unitSorters.add(new IDSorter(Sorting.DESCENDING));
        unitSorters.add(new NameSorter(Sorting.ASCENDING));
        unitSorters.add(new NameSorter(Sorting.DESCENDING));
        unitSorters.add(new TypeSorter(Sorting.ASCENDING));
        unitSorters.add(new TypeSorter(Sorting.DESCENDING));
        unitSorters.add(new TonnageSorter(Sorting.ASCENDING));
        unitSorters.add(new TonnageSorter(Sorting.DESCENDING));
        unitSorters.add(new C3IDSorter(clientgui));
        bvSorters.add(new PlayerBVSorter(clientgui, Sorting.ASCENDING));
        bvSorters.add(new PlayerBVSorter(clientgui, Sorting.DESCENDING));
        bvSorters.add(new BVSorter(Sorting.ASCENDING));
        bvSorters.add(new BVSorter(Sorting.DESCENDING));
        activeSorter = unitSorters.get(0);
    }

    /** Enables buttons to allow adding units when the MSC has finished loading. */
    private final transient MekSummaryCache.Listener mekSummaryCacheListener = () -> {
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
        //noinspection ResultOfMethodCallIgnored
        RandomCallsignGenerator.getInstance(); // Method being initialized

        MekSummaryCache mekSummaryCache = MekSummaryCache.getInstance();
        boolean mscLoaded = mekSummaryCache.isInitialized();

        butLoadList.setActionCommand(CL_ACTION_COMMAND_LOAD_LIST);
        butLoadList.setEnabled(mscLoaded);
        butSaveList.setActionCommand(CL_ACTION_COMMAND_SAVE_LIST);
        butSaveList.setEnabled(false);
        butPrintList.setActionCommand(CL_ACTION_COMMAND_PRINT_LIST);
        butPrintList.setEnabled(false);
        butPrintList.setToolTipText(Messages.getString("ChatLounge.butPrintList.tooltip"));
        butAdd.setEnabled(mscLoaded);
        butAdd.setActionCommand(CL_ACTION_COMMAND_LOAD_MEK);
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
        panUnitInfoGrid.add(butPrintList);

        panUnitInfo.add(panUnitInfoAdd);
        panUnitInfo.add(panUnitInfoGrid);
    }

    /** Sets up the player configuration (team, camo) panel with the player list. */
    private void setupPlayerConfig() {
        scrPlayers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        butAddBot.setActionCommand(CL_ACTION_COMMAND_ADD_BOT);
        butRemoveBot.setEnabled(false);
        butRemoveBot.setActionCommand(CL_ACTION_COMMAND_REMOVE_BOT);
        butBotSettings.setEnabled(false);
        butBotSettings.setActionCommand(CL_ACTION_COMMAND_BOT_CONFIG);
        butConfigPlayer.setEnabled(false);
        butConfigPlayer.setActionCommand(CL_ACTION_COMMAND_CONFIGURE);
        setButUnitIDState();
        setupTeamCombo();
        butCamo.setActionCommand(CL_ACTION_COMMAND_CAMO);
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

    /** Sets up the player configuration (team, camo) panel with the player list. */
    private void setupAutoResolveConfig() {
        scrPlayers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        butRunAutoResolve.setActionCommand(CL_ACTION_COMMAND_AUTO_RESOLVE);
        butRunAutoResolve.setEnabled(true);
        butRunAutoResolve.setToolTipText(Messages.getString("ChatLounge.autoresolve.tooltip"));
        spnSimulationRuns.setEnabled(false);
        spnSimulationRuns.setToolTipText(Messages.getString("ChatLounge.autoresolve.numOfSimulations.tooltip"));
        spnThreadNumber.setEnabled(false);
        chkAutoResolve.setEnabled(true);

        chkAutoResolve.addActionListener(e -> {
            boolean enabled = chkAutoResolve.isSelected();
            spnSimulationRuns.setEnabled(enabled);
            spnThreadNumber.setEnabled(enabled);
        });

        JPanel row1 = new JPanel(new GridLayout(1, 1, 2, 2));
        row1.setBorder(new EmptyBorder(0, 2, 0, 1));
        row1.add(new JLabel(Messages.getString("ChatLounge.name.autoResolveSetup.blurb")));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 2, 2));
        row2.setBorder(new EmptyBorder(0, 2, 0, 1));
        row2.add(new JLabel(Messages.getString("ChatLounge.autoresolve.numOfSimulations")));
        row2.add(spnSimulationRuns);

        JPanel row3 = new JPanel(new GridLayout(1, 2, 2, 2));
        row3.setBorder(new EmptyBorder(0, 2, 0, 1));
        row3.add(new JLabel(Messages.getString("ChatLounge.autoresolve.parallelism")));
        row3.add(spnThreadNumber);

        JPanel row4 = new JPanel(new GridLayout(1, 2, 2, 2));
        row4.setBorder(new EmptyBorder(0, 2, 0, 1));
        row4.add(new JLabel(Messages.getString("ChatLounge.chkAutoResolve")));
        row4.add(chkAutoResolve);

        JPanel row5 = new JPanel(new GridLayout(1, 1, 2, 2));
        row5.setBorder(new EmptyBorder(0, 2, 0, 1));
        row5.add(butRunAutoResolve);

        panAutoResolveInfo = new FixedYPanel(new GridLayout(5, 1, 2, 2));
        panAutoResolveInfo.setBorder(BorderFactory.createTitledBorder(Messages.getString(
              "ChatLounge.name.autoResolveSetup")));
        panAutoResolveInfo.setToolTipText(Messages.getString("ChatLounge.name.autoResolveSetup.tooltip"));
        panAutoResolveInfo.add(row1);
        panAutoResolveInfo.add(row2);
        panAutoResolveInfo.add(row3);
        panAutoResolveInfo.add(row4);
        panAutoResolveInfo.add(row5);
        panAutoResolveInfo.setVisible(CLIENT_PREFERENCES.getShowAutoResolvePanel());
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

        lblGameYear.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTechLevel.setAlignmentX(Component.CENTER_ALIGNMENT);
        butOptions.setAlignmentX(Component.CENTER_ALIGNMENT);

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
        leftSide.add(panAutoResolveInfo);
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
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sp.setDividerLocation(400);
        sp.setDividerSize(10);
        sp.setLeftComponent(leftSide);
        sp.setRightComponent(rightSide);
        panUnits.add(sp);
    }

    private void setupMapPanel() {
        mapSettings = MapSettings.getInstance(clientgui.getClient().getMapSettings());
        setupMapAssembly();
        refreshMapUI();

        panMap.setLayout(new BoxLayout(panMap, BoxLayout.PAGE_AXIS));

        // Ground, Atmosphere, Space Map Buttons
        FixedYPanel panMapType = new FixedYPanel();
        panMapType.setAlignmentX(Component.CENTER_ALIGNMENT);
        panMapType.add(butGroundMap);
        panMapType.add(butLowAtmosphereMap);
        panMapType.add(butSpaceMap);
        grpMap.add(butGroundMap);
        grpMap.add(butLowAtmosphereMap);
        grpMap.add(butHighAtmosphereMap);
        grpMap.add(butSpaceMap);

        // Planetary Conditions and Random Map Settings buttons
        FixedYPanel panSettings = new FixedYPanel();
        panSettings.setAlignmentX(Component.CENTER_ALIGNMENT);
        panSettings.add(butConditions);
        panSettings.add(butRandomMap);

        FixedYPanel panTopRows = new FixedYPanel();
        panTopRows.setLayout(new BoxLayout(panTopRows, BoxLayout.PAGE_AXIS));
        panTopRows.add(panMapType);
        panTopRows.add(panSettings);

        JPanel panHelp = new JPanel(new GridLayout(1, 1));
        panHelp.add(butHelp);
        panHelp.add(butAdvancedSearchMap);

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
        JScrollPane scrBoardsAvailable = new JScrollPane(lisBoardsAvailable);
        refreshBoardsAvailable();

        JPanel panAvail = new JPanel();
        panAvail.setLayout(new BoxLayout(panAvail, BoxLayout.PAGE_AXIS));
        panAvail.setBorder(new EmptyBorder(0, 20, 0, 0));
        panAvail.add(setupAvailTopPanel());
        panAvail.add(scrBoardsAvailable);

        // The split pane holding the left and right side panels
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

        // set up the board preview window.
        boardPreviewW = new ClientDialog(clientgui.getFrame(),
              Messages.getString("BoardSelectionDialog.ViewGameBoard"),
              false);
        boardPreviewW.setLocationRelativeTo(clientgui.getFrame());

        try {
            boardPreviewGame.setPhase(GamePhase.LOUNGE);
            previewBV = new BoardView(boardPreviewGame, null, null, 0);
            previewBV.setDisplayInvalidFields(false);
            previewBV.setUseLosTool(false);
            previewBV.setTooltipProvider(new TWBoardViewTooltip(boardPreviewGame, clientgui, previewBV));

            showPlayerDeployment.setSelected(true);
            showPlayerDeployment.addActionListener(e -> previewGameBoard());

            JButton previewSaveAs = new JButton(Messages.getString("BoardSelectionDialog.ViewGameBoardSaveAs"));
            previewSaveAs.addActionListener(e -> clientgui.boardSaveAs(boardPreviewGame));

            JPanel previewSettingsPanel = new JPanel(new FlowLayout());
            previewSettingsPanel.add(showPlayerDeployment);
            previewSettingsPanel.add(previewSaveAs);

            Box previewPanel = Box.createVerticalBox();
            previewPanel.add(previewSettingsPanel);
            previewPanel.add(previewBV.getComponent(true));
            boardPreviewW.add(previewPanel);
            boardPreviewW.setSize(clientgui.getFrame().getWidth() / 2, clientgui.getFrame().getHeight() / 2);

            String closeAction = "closeAction";
            final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            boardPreviewW.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
            boardPreviewW.getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
            boardPreviewW.getRootPane().getActionMap().put(closeAction, new CloseAction(boardPreviewW));

            RulerDialog.color1 = GUIP.getRulerColor1();
            RulerDialog.color2 = GUIP.getRulerColor2();
            RulerDialog ruler = new RulerDialog(clientgui.getFrame(), client(), previewBV, boardPreviewGame);
            ruler.setLocation(GUIP.getRulerPosX(), GUIP.getRulerPosY());
            ruler.setSize(GUIP.getRulerSizeHeight(), GUIP.getRulerSizeWidth());
            UIUtil.updateWindowBounds(ruler);

            // Most boards will be far too large on the standard zoom
            previewBV.zoomOut();
            previewBV.zoomOut();
            previewBV.zoomOut();
            previewBV.zoomOut();
            boardPreviewW.center();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                  Messages.getString("BoardEditor.CouldNotInitialize") + e,
                  Messages.getString("BoardEditor.FatalError"),
                  JOptionPane.ERROR_MESSAGE);
        }
        refreshMapButtons();
    }

    /**
     * Sets up and returns the panel above the available boards list containing the search bar and the map size
     * chooser.
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
     * Reacts to changes in the available boards search field, showing matching boards for the search string when it has
     * at least 3 characters and reverting to all boards when the search string is empty.
     */
    private void updateSearch(String searchString) {
        if (searchString.isEmpty()) {
            refreshBoardsAvailable();
        } else if (searchString.length() > 2) {
            refreshBoardsAvailable(getSearchedItems(searchString));
        }
    }

    /**
     * Returns the available boards that match the given search string (path or file name contains the search string.)
     * The search string is split at ";" and search results for the tokens are ANDed.
     */
    protected List<String> getSearchedItems(String searchString) {
        String lowerCaseSearchString = searchString.toLowerCase();
        String[] searchStrings = lowerCaseSearchString.split(";");
        java.util.List<String> result = mapSettings.getBoardsAvailableVector();
        for (String token : searchStrings) {
            java.util.List<String> byFilename = mapSettings.getBoardsAvailableVector()
                  .stream()
                  .filter(b -> b.toLowerCase().contains(token) && isBoardFile(b))
                  .collect(Collectors.toList());
            java.util.List<String> byTags = boardTags.entrySet()
                  .stream()
                  .filter(e -> e.getValue().contains(token))
                  .map(Map.Entry::getKey)
                  .collect(Collectors.toList());
            java.util.List<String> tokenResult = CollectionUtil.union(byFilename, byTags);
            result = result.stream().filter(tokenResult::contains).collect(toList());
        }
        return result;
    }

    /**
     * Returns a suitable divider location for the split pane that contains the available boards list and the map
     * preview. The divider location gives between 30% and 50% of space to the map preview depending on the width of the
     * game map.
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
            button = butLowAtmosphereMap;
        } else if (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            button = butSpaceMap;
        }

        if (!button.isSelected()) {
            button.removeActionListener(lobbyListener);
            button.setSelected(true);
            button.addActionListener(lobbyListener);
        }
    }

    /**
     * Refreshes the map size selection combo box with all available map sizes, sorted by width and then by height.
     *
     * <p>This method obtains the set of available map sizes from the client, sorts them first by width (ascending)
     * and then by height (ascending), and populates the combo box with these sorted sizes. An additional "Custom Map
     * Size" entry is added at the end of the list. The previously selected index is preserved if possible.</p>
     *
     * <p>The combo box's action listener is temporarily removed during the update to prevent unwanted event
     * firing.</p>
     */
    private void refreshMapSizes() {
        int oldSelection = comMapSizes.getSelectedIndex();
        Set<BoardDimensions> mapSizes = clientgui.getClient().getAvailableMapSizes();

        // Sort the map sizes by width (w()) and then by height (h())
        List<BoardDimensions> sortedSizes = new ArrayList<>(mapSizes);
        sortedSizes.sort(Comparator.comparingInt(BoardDimensions::w)
              .thenComparingInt(BoardDimensions::h));

        comMapSizes.removeActionListener(lobbyListener);
        comMapSizes.removeAllItems();

        for (BoardDimensions size : sortedSizes) {
            comMapSizes.addItem(size);
        }

        comMapSizes.addItem(Messages.getString("ChatLounge.CustomMapSize"));
        comMapSizes.setSelectedIndex(oldSelection != -1 ? oldSelection : 0);
        comMapSizes.addActionListener(lobbyListener);
    }

    /**
     * Refreshes the map assembly UI from the current map settings. Does NOT trigger further changes or result in
     * packets to the server.
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
        butAdvancedSearchMap.setEnabled(!inSpace &&
              (mapSettings.getMapWidth() == 1) &&
              (mapSettings.getMapHeight() == 1));

        butGroundMap.removeActionListener(lobbyListener);
        butLowAtmosphereMap.removeActionListener(lobbyListener);
        butHighAtmosphereMap.removeActionListener(lobbyListener);
        butSpaceMap.removeActionListener(lobbyListener);
        if (onGround) {
            butGroundMap.setSelected(true);
        } else if (inSpace) {
            butSpaceMap.setSelected(true);
        } else {
            butLowAtmosphereMap.setSelected(true);
        }
        butGroundMap.addActionListener(lobbyListener);
        butLowAtmosphereMap.addActionListener(lobbyListener);
        butHighAtmosphereMap.addActionListener(lobbyListener);
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
     * Refreshes the list of available boards with all available boards plus GENERATED. Useful for first setup, when the
     * server transmits new map settings and when the text search field is empty.
     */
    private void refreshBoardsAvailable() {
        if (!lisBoardsAvailable.isEnabled()) {
            return;
        }
        lisBoardsAvailable.setFixedCellHeight(-1);
        lisBoardsAvailable.setFixedCellWidth(-1);
        java.util.List<String> availBoards = new ArrayList<>();
        availBoards.add(MapSettings.BOARD_GENERATED);
        availBoards.addAll(mapSettings.getBoardsAvailableVector());
        refreshBoardTags(); // is this necessary?
        refreshBoardsAvailable(availBoards);
    }

    private void refreshBoardTags() {
        boardTags.clear();
        for (String boardName : mapSettings.getBoardsAvailableVector()) {
            File boardFile = new MegaMekFile(Configuration.boardsDir(),
                  boardName + MMConstants.CL_KEY_FILE_EXTENSION_BOARD).getFile();
            Set<String> tags = Board.getTags(boardFile);
            boardTags.put(boardName, String.join("||", tags).toLowerCase());
        }
    }

    /**
     * Refreshes the list of available maps with the given list of boards.
     */
    private void refreshBoardsAvailable(java.util.List<String> boardList) {
        lisBoardsAvailable.removeListSelectionListener(this);
        // Replace the data model (adding the elements one by one to the existing model
        // in Java 8 style is sluggish because of event firing)
        DefaultListModel<String> newModel = new DefaultListModel<>();
        for (String s : boardList) {
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
     * Fills the Map Buttons scroll pane with the appropriate amount of buttons in the appropriate layout
     */
    private void refreshMapButtons() {
        panMapButtons.removeAll();
        panMapButtons.setVisible(false);
        panMapButtons.add(Box.createVerticalGlue());
        Dimension buttonSize = null;

        // If buttons are unused, remove their image so that they update when they're
        // used once more
        if (mapSettings.getMapHeight() * mapSettings.getMapWidth() < mapButtons.size()) {
            for (MapPreviewButton button : mapButtons.subList(mapSettings.getMapHeight() * mapSettings.getMapWidth(),
                  mapButtons.size())) {
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
                if (!button.getBoard().equals(boardName) ||
                      oldMapSettings.getMedium() != mapSettings.getMedium() ||
                      (!mapSettings.equalMapGenParameters(oldMapSettings) &&
                            mapSettings.getMapWidth() == oldMapSettings.getMapWidth() &&
                            mapSettings.getMapHeight() == oldMapSettings.getMapHeight())) {
                    Board buttonBoard;
                    Image image;
                    // Generated and space boards use a generated example
                    if (boardName.startsWith(MapSettings.BOARD_GENERATED) ||
                          (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE)) {
                        buttonBoard = BoardUtilities.generateRandom(mapSettings);
                        image = MinimapPanel.getMinimapImageMaxZoom(buttonBoard);
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

                        File boardFile = new MegaMekFile(Configuration.boardsDir(),
                              boardForImage + MMConstants.CL_KEY_FILE_EXTENSION_BOARD).getFile();
                        if (boardFile.exists()) {
                            buttonBoard = new Board(16, 17);
                            buttonBoard.load(new MegaMekFile(Configuration.boardsDir(),
                                  boardForImage + MMConstants.CL_KEY_FILE_EXTENSION_BOARD).getFile());
                            try (InputStream is = new FileInputStream(new MegaMekFile(Configuration.boardsDir(),
                                  boardForImage + MMConstants.CL_KEY_FILE_EXTENSION_BOARD).getFile())) {
                                buttonBoard.load(is, null, true);
                                BoardUtilities.flip(buttonBoard, rotateBoard, rotateBoard);
                            } catch (IOException ex) {
                                buttonBoard = Board.createEmptyBoard(mapSettings.getBoardWidth(),
                                      mapSettings.getBoardHeight());
                            }
                            image = MinimapPanel.getMinimapImageMaxZoom(buttonBoard);
                        } else {
                            buttonBoard = Board.createEmptyBoard(mapSettings.getBoardWidth(),
                                  mapSettings.getBoardHeight());
                            BufferedImage emptyBoardMap = MinimapPanel.getMinimapImageMaxZoom(buttonBoard);
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
            for (MapPreviewButton button : mapButtons) {
                button.setPreviewSize(buttonSize);
            }
        }
        splGroundMap.setDividerLocation(getDividerLocation());

        panMapButtons.add(Box.createVerticalGlue());
        panMapButtons.setVisible(true);

        lblBoardsAvailable.setText(mapSettings.getBoardWidth() +
              "x" +
              mapSettings.getBoardHeight() +
              " " +
              Messages.getString("BoardSelectionDialog.mapsAvailable"));
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
        g.setFont(new Font(SuiteConstants.FONT_DIALOG, Font.ITALIC, fontSize));
        FontMetrics fm = g.getFontMetrics(g.getFont());
        int cx = (w - fm.stringWidth(text)) / 2;
        int cy = h / 10 + fm.getAscent();
        g.setColor(GUIP.getWarningColor());
        g.drawString(text, cx, cy);
        g.dispose();
    }

    public void previewGameBoard() {
        Board newBoard = ServerBoardHelper.getPossibleGameBoard(mapSettings, false);
        boardPreviewGame.setBoard(newBoard);
        previewBV.setLocalPlayer(client().getLocalPlayer());
        final var gOpts = game().getOptions();
        boardPreviewGame.setOptions(gOpts);
        previewBV.setShowLobbyPlayerDeployment(showPlayerDeployment.isSelected());

        for (Player player : boardPreviewGame.getPlayersList()) {
            boardPreviewGame.removePlayer(player.getId());
        }
        for (Player player : game().getPlayersList()) {
            boardPreviewGame.setPlayer(player.getId(), player.copy());
        }
        boardPreviewW.setVisible(true);
    }

    /**
     * Refreshes the game settings with new info from the client
     */
    private void refreshGameSettings() {
        refreshTeams();
        refreshMapSettings();
        refreshDoneButton();
        refreshAcar();
    }

    public void refreshAcar() {
        boolean clientEnabledAcar = CLIENT_PREFERENCES.getShowAutoResolvePanel();
        boolean notRealBlindDrop = !game().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        panAutoResolveInfo.setVisible(clientEnabledAcar && notRealBlindDrop);
    }

    /**
     * Refreshes the Mek Table contents
     */
    public void refreshEntities() {
        refreshTree();
        refreshMekTable();
    }

    private void refreshMekTable() {
        java.util.List<Integer> enIds = getSelectedEntities().stream().map(Entity::getId).toList();
        mekModel.clearData();
        ArrayList<Entity> allEntities = new ArrayList<>(clientgui.getClient().getEntitiesVector());
        allEntities.sort(activeSorter);

        boolean localUnits = false;
        var opts = clientgui.getClient().getGame().getOptions();

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

            if (!opts.booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIAL_REPAIRS)) {
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

            // Handle the "Blind Drop" option. In blind drop, units must be added, but they will be obscured in the
            // table. In real blind drop, units don't even get added to the table. Teams see their units in any case.
            boolean localUnit = entity.getOwner().equals(localPlayer());
            boolean teamUnit = !entity.getOwner().isEnemyOf(localPlayer());
            boolean realBlindDrop = opts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
            boolean localGM = localPlayer().isGameMaster();
            if (localUnit || teamUnit || !realBlindDrop || localGM) {
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

    /** Adjusts the MekTable to compact/normal mode. */
    private void toggleCompact() {
        setTableRowHeights();
        mekModel.refreshCells();
        mekForceTreeModel.nodeChanged((TreeNode) mekForceTreeModel.getRoot());

    }

    /** Refreshes the player info table. */
    private void refreshPlayerTable() {
        // Remember the selected players
        var selPlayerIds = getSelectedPlayers().stream().map(Player::getId).collect(toSet());

        // Empty and refill the player table
        playerModel.replaceData(game().getPlayersList());

        // re-select the previously selected players, if possible
        for (int row = 0; row < playerModel.getRowCount(); row++) {
            if (selPlayerIds.contains(playerModel.getPlayerAt(row).getId())) {
                tablePlayers.addRowSelectionInterval(row, row);
            }
        }
        // If no one is selected now (and the table isn't empty), select the first
        // player
        if ((tablePlayers.getSelectedRowCount() == 0) && (tablePlayers.getRowCount() > 0)) {
            tablePlayers.addRowSelectionInterval(0, 0);
        }
    }

    /**
     * Updates the camo button to displays the camo of the currently selected player.
     */
    private void refreshCamoButton() {
        if (tablePlayers.getSelectedRowCount() == 0) {
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
     * Refreshes the Done button. The label will say the opposite of the player's "done" status, indicating that
     * clicking it will reverse the condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone.setText(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone"));
    }

    /**
     * Refreshes the state of the Done button with the state of the local player.
     */
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

        // We need to make sure our current bomb choices fit onto the new fighter
        if (carrier instanceof FighterSquadron fSquad) {
            // We can't use Aero.getBombPoints() because the bombs haven't been loaded yet, only selected, so we have
            // to count the choices
            BombLoadout bombChoices = fSquad.getBombChoices();
            // We can't load all the squadrons bombs
            if (bombChoices.getTotalBombCost() > ((IBomber) carried).getMaxBombPoints()) {
                JOptionPane.showMessageDialog(clientgui.getFrame(),
                      Messages.getString("FighterSquadron.bomberror"),
                      Messages.getString("FighterSquadron.bomberror"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        getLocalClient(carried).sendLoadEntity(carried.getId(), carrierId, bayNumber);
        // TODO: it would probably be a good idea to disable some settings for loaded units in customMekDialog
    }

    /**
     * Have the given entity disembark if it is carried by another unit. Entities that are modified and need an update
     * to be sent to the server are added to the given updateCandidates.
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
     * Have the given entities detach from their tractors if it's a trailer. Entities that are modified and need an
     * update to be sent to the server are added to the given updateCandidates.
     */
    void detachFromTractors(Set<Entity> trailers, Collection<Entity> updateCandidates) {
        for (Entity trailer : trailers) {
            detachFromTractor(trailer, updateCandidates);
        }
    }

    /**
     * Have the given entity detach from its tractor if it's a trailer. Entities that are modified and need an update to
     * be sent to the server are added to the given updateCandidates.
     */
    void detachFromTractor(Entity trailer, Collection<Entity> updateCandidates) {
        if (trailer.getTowedBy() == Entity.NONE) {
            return;
        }
        Entity tractor = game().getEntity(trailer.getTowedBy());
        disconnectTrain(tractor, trailer, updateCandidates);
    }

    /**
     * Have the given entities detach their towed trailers. Entities that are modified and need an update to be sent to
     * the server are added to the given updateCandidates.
     */
    void detachTrailers(Set<Entity> tractors, Collection<Entity> updateCandidates) {
        for (Entity tractor : tractors) {
            detachTrailer(tractor, updateCandidates);
        }
    }

    /**
     * Have the given entity detach a towed trailer. Entities that are modified and need an update to be sent to the
     * server are added to the given updateCandidates.
     */
    void detachTrailer(Entity tractor, Collection<Entity> updateCandidates) {
        if (tractor.getTowing() == Entity.NONE) {
            return;
        }
        Entity trailer = game().getEntity(tractor.getTowing());
        disconnectTrain(tractor, trailer, updateCandidates);
    }

    private void disconnectTrain(Entity tractor, Entity trailer, Collection<Entity> updateCandidates) {
        if (tractor != null && trailer != null) {
            List<Integer> otherTowedUnitIds = tractor.getAllTowedUnits();
            tractor.disconnectUnit(trailer.getId());
            updateCandidates.add(trailer);
            updateCandidates.add(tractor);
            for (int otherTowedUnitId : otherTowedUnitIds) {
                Entity otherTowedUnit = game().getEntity(otherTowedUnitId);
                if (otherTowedUnit != null) {
                    updateCandidates.add(otherTowedUnit);
                }
            }
        }
    }

    /**
     * Have the given entities offload all the units they are carrying. Returns a set of entities that need to be sent
     * to the server.
     */
    void offloadAll(Collection<Entity> entities, Collection<Entity> updateCandidates) {
        for (Entity carrier : editableEntities(entities)) {
            offloadFrom(carrier, updateCandidates);
        }
    }

    /**
     * Have the given entity offload all the units it is carrying. Returns a set of entities that need to be sent to the
     * server.
     */
    void offloadFrom(Entity entity, Collection<Entity> updateCandidates) {
        if (isEditable(entity)) {
            for (Entity carriedUnit : entity.getLoadedUnits()) {
                disembark(carriedUnit, updateCandidates);
            }
        }
    }

    /**
     * Set the provided trailer to be towed by the tractor.
     */
    void towBy(Entity trailer, int tractorId) {
        Entity tractor = game().getEntity(tractorId);
        if (tractor == null || !tractor.canTow(trailer.getId())) {
            return;
        }

        getLocalClient(trailer).sendTowEntity(trailer.getId(), tractor.getId());
        // TODO: it would probably be a good idea to disable some settings for loaded units in customMekDialog
    }

    /**
     * Sends the entities in the given Collection to the Server. Sends only those that can be edited, i.e. the player's
     * own or his bots' units.
     */
    void sendUpdate(Collection<Entity> updateCandidates) {
        for (Entity e : editableEntities(updateCandidates)) {
            getLocalClient(e).sendUpdateEntity(e);
        }
    }

    void sendProxyUpdates(Collection<Entity> updateCandidates, Player player) {
        getLocalClient(player).sendUpdateEntity(updateCandidates);
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
     * Returns true when the given entity may be configured by the local player, i.e. if it is his own unit or one of
     * his bot's units.
     * <p>
     * Note that this is more restrictive than the Server is. The Server accepts entity changes also for teammates so
     * that entity updates that signal transporting a teammate's unit don't get rejected. I feel that configuration
     * other than transporting units should be limited to one's own units (and bots) though.
     */
    boolean isEditable(Entity entity) {
        boolean localGM = clientgui.getClient().getLocalPlayer().isGameMaster();
        return localGM ||
              (clientgui.getLocalBots().containsKey(entity.getOwner().getName()) ||
                    (entity.getOwnerId() == localPlayer().getId()));
    }

    /**
     * Returns true when the given entity may NOT be configured by the local player, i.e. if it is not own unit or one
     * of his bot's units.
     *
     * @see #isEditable(Entity)
     */
    boolean isNotEditable(Entity entity) {
        return !isEditable(entity);
    }

    /**
     * Returns the Client associated with a given entity that may be configured by the local player (his own unit or one
     * of his bot's units). For a unit that cannot be configured (owned by a remote player) the client of the local
     * player is returned.
     */
    Client getLocalClient(Entity entity) {
        if (clientgui.getLocalBots().containsKey(entity.getOwner().getName())) {
            return (Client) clientgui.getLocalBots().get(entity.getOwner().getName());
        } else {
            return clientgui.getClient();
        }
    }

    Client getLocalClient(Player player) {
        if (clientgui.getLocalBots().containsKey(player.getName())) {
            return (Client) clientgui.getLocalBots().get(player.getName());
        } else {
            return clientgui.getClient();
        }
    }

    public void configPlayer() {
        Client c = getSelectedClient();
        if (null == c) {
            return;
        }

        if (psd != null) {
            psd.dispose();
        }

        psd = new PlayerSettingsDialog(clientgui, c, previewBV);
        psd.setModal(false);
        psd.showDialog();
    }

    /**
     * Pop up the dialog to load a mek
     */
    private void addUnit() {
        Client c = getSelectedClient();
        clientgui.getMekSelectorDialog().updateOptionValues();
        clientgui.getMekSelectorDialog().setPlayerFromClient(c);
        clientgui.getMekSelectorDialog().setVisible(true);
    }

    private void createArmy() {
        Client c = getSelectedClient();
        clientgui.getRandomArmyDialog().setPlayerFromClient(c);
        clientgui.getRandomArmyDialog().setVisible(true);
    }

    public void loadRandomNames() {
        clientgui.getRandomNameDialog().showDialog(clientgui.getClient().getGame().getEntitiesVector());
    }

    void changeMapDnD(String board, MapPreviewButton button) {
        if (board.contains("\n")) {
            board = MapSettings.BOARD_SURPRISE + board;
        }
        int indexOfButton = mapButtons.indexOf(button);
        mapSettings.getBoardsSelectedVector().set(indexOfButton, board);
        clientgui.getClient().sendMapSettings(mapSettings);
        if (boardPreviewW.isVisible()) {
            previewGameBoard();
        }

        String msg = clientgui.getClient().getLocalPlayer() + " changed map to: " + board;
        clientgui.getClient().sendServerChat(Player.PLAYER_NONE, msg);
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
            // Only reset the save flag when entering lounge from a different phase
            if (!e.getOldPhase().isLounge()) {
                lobbySavePerformed = false;
            }
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
        // The table sorting may no longer be allowed (e.g. when blind drop was
        // activated)
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

    private void nagAboutNonTileableBoards() {
        boolean isBoardWidthOdd = mapSettings.getBoardWidth() % 2 != 0;
        boolean isMapSizeBiggerThanOne = mapSettings.getMapWidth() > 1;
        if (isBoardWidthOdd && isMapSizeBiggerThanOne && GUIP.getNagForOddSizedBoard()) {
            InformDialog nag = clientgui.doInformBotherDialog(I18n.getTextAt("megamek.client.messages",
                        "ChatLounge.board.warning.title"),
                  I18n.getTextAt("megamek.client.messages", "ChatLounge.board.warning.message"),
                  true);
            // do they want to be bothered again?
            if (!nag.getShowAgain()) {
                GUIP.setNagForOddSizedBoard(false);
            }
        }
    }

    private final ActionListener lobbyListener = new ActionListener() {
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
                new SkillGenerationDialog(clientgui.getFrame(),
                      clientgui,
                      clientgui.getClient().getGame().getEntitiesVector()).showDialog();
            } else if (ev.getSource().equals(butNames)) {
                loadRandomNames();
            } else if (ev.getSource().equals(tablePlayers)) {
                configPlayer();
            } else if (ev.getSource().equals(comboTeam)) {
                lobbyActions.changeTeam(getSelectedPlayers(), comboTeam.getSelectedIndex());
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
                // Allow the player to replace their current list of entities with a list from a file.
                Client c = getSelectedClient();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                          Messages.getString("ChatLounge.SelectBotOrPlayer"));
                    return;
                }
                clientgui.loadListFile(c.getLocalPlayer());

            } else if (ev.getSource().equals(butSaveList) || ev.getSource().equals(butPrintList)) {
                // Allow the player to save their current
                // list of entities to a file.
                Client c = getSelectedClient();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                          Messages.getString("ChatLounge.SelectBotOrPlayer"));
                    return;
                }
                ArrayList<Entity> entities = c.getGame().getPlayerEntities(c.getLocalPlayer(), false);
                for (Entity entity : entities) {
                    entity.setForceString(game().getForces().forceStringFor(entity));
                }
                if (ev.getSource().equals(butSaveList)) {
                    clientgui.saveListFile(entities, c.getLocalPlayer().getName());
                } else {
                    clientgui.printList(entities, (JButton) ev.getSource());
                }

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
                RandomMapDialog rmd = new RandomMapDialog(clientgui.getFrame(),
                      ChatLounge.this,
                      clientgui.getClient(),
                      mapSettings);
                rmd.activateDialog(clientgui.getTilesetManager().getThemes());

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

            } else if (ev.getSource() == butLowAtmosphereMap) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
                refreshMapUI();
                clientgui.getClient().sendMapSettings(mapSettings);

            } else if ((ev.getSource() == butAddX) || (ev.getSource() == butMapGrowW)) {
                int newMapWidth = mapSettings.getMapWidth() + 1;
                mapSettings.setMapSize(newMapWidth, mapSettings.getMapHeight());
                clientgui.getClient().sendMapDimensions(mapSettings);
                nagAboutNonTileableBoards();

            } else if ((ev.getSource() == butAddY) || (ev.getSource() == butMapGrowH)) {
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

            } else if (ev.getSource() == butRunAutoResolve) {
                var simulationRuns = (int) spnSimulationRuns.getValue();
                var threadNumbers = (int) spnThreadNumber.getValue();
                var forcesSetups = new MMSetupForces(game());
                var currentTeam = client().getLocalPlayer().getTeam();

                var board = TWBoardTransformer.instantiateBoard(client().getMapSettings(),
                      client().getGame().getPlanetaryConditions(),
                      client().getGame().getOptions());
                var planetaryConditions = client().getGame().getPlanetaryConditions();
                if (chkAutoResolve.isSelected()) {
                    var proceed = AutoResolveChanceDialog.showDialog(clientgui.getFrame(),
                          simulationRuns,
                          threadNumbers,
                          currentTeam,
                          forcesSetups,
                          board,
                          planetaryConditions
                    ) == JOptionPane.YES_OPTION;

                    if (!proceed) {
                        return;
                    }
                }

                var event = AutoResolveProgressDialog.showDialog(clientgui.getFrame(), forcesSetups, board,
                      new PlanetaryConditions(planetaryConditions));
                if (event != null) {
                    var autoResolveBattleReport = new AutoResolveSimulationLogDialog(clientgui.getFrame(),
                          event.getLogFile());
                    autoResolveBattleReport.setModal(true);
                    autoResolveBattleReport.setVisible(true);
                } else {
                    clientgui.doAlertDialog(Messages.getString("AutoResolveSimulationLogDialog.title"),
                          Messages.getString("AutoResolveSimulationLogDialog.noReport"));
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
                File helpfile = new File(CL_KEY_FILEPATH_MAP_ASSEMBLY_HELP);
                final JDialog dialog = new ClientDialog(clientgui.getFrame(),
                      Messages.getString("ChatLounge.map.title.mapAssemblyHelp"),
                      true);
                final int height = 600;
                final int width = 600;

                final JEditorPane pane = new JEditorPane();
                pane.setName(CL_KEY_NAME_HELP_PANE);
                pane.setEditable(false);
                pane.setFont(UIUtil.getDefaultFont());
                try {
                    pane.setPage(helpfile.toURI().toURL());
                    JScrollPane tScroll = new JScrollPane(pane,
                          ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    tScroll.getVerticalScrollBar().setUnitIncrement(16);
                    dialog.add(tScroll, BorderLayout.CENTER);
                } catch (Exception e) {
                    dialog.setTitle(Messages.getString("AbstractHelpDialog.noHelp.title"));
                    pane.setText(Messages.getString("AbstractHelpDialog.errorReading") + e.getMessage());
                    LOGGER.error(e, "");
                }

                JButton button = new DialogButton(Messages.getString("Okay"));
                button.addActionListener(e -> dialog.setVisible(false));
                JPanel okayPanel = new JPanel(new FlowLayout());
                okayPanel.add(button);
                dialog.add(okayPanel, BorderLayout.PAGE_END);

                Dimension sz = new Dimension(scaleForGUI(width), scaleForGUI(height));
                dialog.setPreferredSize(sz);
                dialog.pack();
                dialog.setVisible(true);

            } else if (ev.getSource() == butAdvancedSearchMap) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                AdvancedSearchMapDialog advancedSearchMapDialog = new AdvancedSearchMapDialog(clientgui.getFrame());
                setCursor(Cursor.getDefaultCursor());

                if (advancedSearchMapDialog.showDialog().isConfirmed()) {
                    String path = advancedSearchMapDialog.getPath();
                    if (path != null) {
                        Board board = new Board(16, 17);
                        board.load(new MegaMekFile(Configuration.boardsDir(), path).getFile());
                        String boardName = path.replace(".board", "");
                        boardName = boardName.replace("\\", "/");
                        mapSettings.getBoardsSelectedVector().clear();
                        mapSettings.setMapSize(1, 1);
                        mapSettings.setBoardSize(board.getWidth(), board.getHeight());
                        clientgui.getClient().sendMapDimensions(mapSettings);
                        mapSettings.getBoardsSelectedVector().set(0, boardName);
                        refreshMapUI();
                        clientgui.getClient().sendMapSettings(mapSettings);

                        if (boardPreviewW.isVisible()) {
                            previewGameBoard();
                        }

                        String msg = clientgui.getClient().getLocalPlayer() + " changed map to: " + boardName;
                        clientgui.getClient().sendServerChat(Player.PLAYER_NONE, msg);
                    }
                }

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

    private BehaviorSettings getFavoriteBehaviorSettings() {
        return BehaviorSettingsFactory.getInstance().getBehaviorOrDefault(
              CLIENT_PREFERENCES.getFavoritePrincessBehaviorSetting(),
              BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
    }

    private void configAndCreateBot(@Nullable Player toReplace) {
        BehaviorSettings behavior = getFavoriteBehaviorSettings();
        String botName = null;
        if (toReplace != null) {
            botName = toReplace.getName();
            behavior = game().getBotSettings().getOrDefault(botName, behavior);
        }
        var bcd = new BotConfigDialog(clientgui.getFrame(), botName, behavior, clientgui);
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CANCELLED) {
            return;
        }
        Princess botClient = Princess.createPrincess(bcd.getBotName(),
              client().getHost(),
              client().getPort(),
              bcd.getBehaviorSettings());
        botClient.setClientGUI(clientgui);
        botClient.getGame().addGameListener(new BotGUI(getClientGUI().getFrame(), botClient));
        try {
            botClient.connect();
            clientgui.getLocalBots().put(bcd.getBotName(), botClient);
        } catch (Exception e) {
            clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertBot.title"),
                  Messages.getString("ChatLounge.AlertBot.message"));
            botClient.die();
        }
    }

    /**
     * Opens a file chooser and saves the current map setup to the file, if any was chosen.
     *
     * @see MapSetup
     */
    private void saveMapSetup() {
        JFileChooser fc = new JFileChooser(Configuration.dataDir() + CL_KEY_FILEPATH_MAP_SETUP);
        fc.setDialogTitle(Messages.getString("ChatLounge.map.saveMapSetup"));
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(XMLFileFilter);

        int returnVal = fc.showSaveDialog(clientgui.getFrame());
        File selectedFile = fc.getSelectedFile();
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (selectedFile == null)) {
            return;
        }
        if (!selectedFile.getName().toLowerCase().endsWith(CL_KEY_FILE_EXTENSION_XML)) {
            selectedFile = new File(selectedFile.getPath() + CL_KEY_FILE_EXTENSION_XML);
        }
        if (selectedFile.exists()) {
            String msg = Messages.getString("ChatLounge.map.saveMapSetupReplace", selectedFile.getName());
            if (!MMConfirmDialog.confirm(clientgui.getFrame(),
                  Messages.getString("ChatLounge.map.confirmReplace"),
                  msg)) {
                return;
            }
        }
        try (OutputStream os = new FileOutputStream(selectedFile)) {
            MapSetup.save(os, mapSettings);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(clientgui.getFrame(),
                  Messages.getString("ChatLounge.map.problemSaving"),
                  Messages.getString("Error"),
                  JOptionPane.ERROR_MESSAGE);
            LOGGER.error(ex, "");
        }
    }

    /**
     * Opens a file chooser and loads a new map setup from the file, if any was chosen.
     *
     * @see MapSetup
     */
    private void loadMapSetup() {
        JFileChooser fc = new JFileChooser(Configuration.dataDir() + CL_KEY_FILEPATH_MAP_SETUP);
        fc.setDialogTitle(Messages.getString("ChatLounge.map.loadMapSetup"));
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(XMLFileFilter);

        int returnVal = fc.showOpenDialog(clientgui.getFrame());
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return;
        }
        if (!fc.getSelectedFile().exists()) {
            JOptionPane.showMessageDialog(clientgui.getFrame(), Messages.getString("ChatLounge.fileNotFound"));
            return;
        }
        try (InputStream os = new FileInputStream(fc.getSelectedFile())) {
            MapSetup setup = MapSetup.load(os);
            mapSettings.setMapSize(setup.getMapWidth(), setup.getMapHeight());
            mapSettings.setBoardSize(setup.getBoardWidth(), setup.getBoardHeight());
            mapSettings.setBoardsSelectedVector(setup.getBoards());
            clientgui.getClient().sendMapSettings(mapSettings);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(clientgui.getFrame(),
                  Messages.getString("ChatLounge.map.problemLoadMapSetup"),
                  Messages.getString("Error"),
                  JOptionPane.ERROR_MESSAGE);
            LOGGER.error(ex, "");
        }
    }

    private void removeBot() {
        Client c = getSelectedClient();
        if (!client().getBots().containsValue(c)) {
            LobbyErrors.showOnlyOwnBot(clientgui.getFrame());
            return;
        }
        // Delete units first, which safely disembarks and offloads them
        // Don't delete the bot's forces, as that could also delete other players'
        // entities
        c.die();
        clientgui.getLocalBots().remove(c.getName());
    }

    private void doBotSettings() {
        Player player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
        Princess bot = (Princess) clientgui.getLocalBots().get(player.getName());
        var bcd = new BotConfigDialog(clientgui.getFrame(),
              bot.getLocalPlayer().getName(),
              bot.getBehaviorSettings(),
              clientgui);
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CONFIRMED) {
            bot.setBehaviorSettings(bcd.getBehaviorSettings());
        }
    }

    // Put a filter on the files that the user can select the proper file.
    transient FileFilter XMLFileFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return (f.getPath().toLowerCase().endsWith(CL_KEY_FILE_EXTENSION_XML) || f.isDirectory());
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
                nagAboutNonTileableBoards();
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
                nagAboutNonTileableBoards();
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
     * Updates to show the map settings that have, presumably, just been sent by the server.
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

    /** OK Refreshes the Map Summary, Tech Level and Game Year labels. */
    private void refreshLabels() {
        var opts = clientgui.getClient().getGame().getOptions();

        String txt = Messages.getString("ChatLounge.GameYear");
        txt += opts.intOption(OptionsConstants.ALLOWED_YEAR);
        lblGameYear.setText(txt);
        lblGameYear.setToolTipText(Messages.getString("ChatLounge.tooltip.techYear"));

        String tlString = TechConstants.getLevelDisplayableName(TechConstants.T_TECH_UNKNOWN);
        IOption tlOpt = opts.getOption(OptionsConstants.ALLOWED_TECH_LEVEL);
        if (tlOpt != null) {
            tlString = tlOpt.stringValue();
        }
        lblTechLevel.setText(Messages.getString("ChatLounge.TechLevel") + tlString);
        lblTechLevel.setToolTipText(Messages.getString("ChatLounge.tooltip.techYear"));

        txt = Messages.getString("ChatLounge.MapSummary");
        txt += (mapSettings.getBoardWidth() * mapSettings.getMapWidth()) +
              " x " +
              (mapSettings.getBoardHeight() * mapSettings.getMapHeight());
        if (butGroundMap.isSelected()) {
            txt += Messages.getString("ChatLounge.name.groundMap");
        } else if (butLowAtmosphereMap.isSelected()) {
            txt += " " + Messages.getString("ChatLounge.name.atmosphericMap");
        } else {
            txt += " " + Messages.getString("ChatLounge.name.spaceMap");
        }
        lblMapSummary.setText(txt);

        StringBuilder selectedMaps = new StringBuilder();
        selectedMaps.append(Messages.getString("ChatLounge.MapSummarySelectedMaps"));
        for (String map : mapSettings.getBoardsSelectedVector()) {
            selectedMaps.append("&nbsp;&nbsp;");
            if (map.startsWith(MapSettings.BOARD_SURPRISE)) {
                selectedMaps.append(MapSettings.BOARD_SURPRISE);
            } else {
                selectedMaps.append(map);
            }
            selectedMaps.append("<br>");
        }
        lblMapSummary.setToolTipText(selectedMaps.toString());
    }

    @Override
    public void ready() {
        final Client client = clientgui.getClient();
        final Game game = client.getGame();
        final var gOpts = game.getOptions();

        // enforce exclusive deployment zones in double blind
        for (Player player : client.getGame().getPlayersList()) {
            if (!isValidStartPos(game, player)) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.OverlapDeploy.title"),
                      Messages.getString("ChatLounge.OverlapDeploy.msg"));
                return;
            }
        }

        // Make sure player has a commander if Commander killed victory is on
        if (gOpts.booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) {
            java.util.List<String> players = new ArrayList<>();
            if ((game.getLiveCommandersOwnedBy(localPlayer()) < 1) && (game.getEntitiesOwnedBy(localPlayer()) > 0)) {
                players.add(client.getLocalPlayer().getName());
            }

            for (AbstractClient bc : clientgui.getLocalBots().values()) {
                if ((game.getLiveCommandersOwnedBy(bc.getLocalPlayer()) < 1) &&
                      (game.getEntitiesOwnedBy(bc.getLocalPlayer()) > 0)) {
                    players.add(bc.getLocalPlayer().getName());
                }
            }

            if (!players.isEmpty()) {
                StringBuilder msg = new StringBuilder(Messages.getString("ChatLounge.noCmdr.msg"));

                for (String player : players) {
                    msg.append(player).append("\n");
                }

                clientgui.doAlertDialog(Messages.getString("ChatLounge.noCmdr.title"), msg.toString());
                return;
            }
        }

        if (psd != null) {
            psd.dispose();
        }

        if (boardPreviewW != null) {
            boardPreviewW.dispose();
        }

        boolean done = !localPlayer().isDone();

        // Save lobby state if setting is enabled, player is marking as done, and we haven't saved yet
        if (done && GUIP.getSaveLobbyOnStart() && !lobbySavePerformed) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
            String filename = "Lobby_Save_" + timestamp;
            String path = "./" + MMConstants.SAVEGAME_DIR;
            client.sendChat(ClientGUI.CG_CHAT_COMMAND_LOCAL_SAVE + " " + filename + " " + path);
            lobbySavePerformed = true;
        }

        client.sendDone(done);
        refreshDoneButton(done);
        for (AbstractClient botClient : clientgui.getLocalBots().values()) {
            botClient.sendDone(done);
        }
    }

    Client getSelectedClient() {
        if (tablePlayers.getSelectedRowCount() == 0) {
            return null;
        }

        Player player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
        if (localPlayer().equals(player)) {
            return client();
        } else if (client().getBots().containsKey(player.getName())) {
            return (Client) client().getBots().get(player.getName());
        } else {
            return null;
        }
    }

    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.boardViews().forEach(bv -> bv.removeBoardViewListener(this));
        GUIP.removePreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().removePreferenceChangeListener(this);
        MekSummaryCache.getInstance().removeListener(mekSummaryCacheListener);

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
        butPrintList.removeActionListener(lobbyListener);
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
        butLowAtmosphereMap.removeActionListener(lobbyListener);
        butHighAtmosphereMap.removeActionListener(lobbyListener);
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
        butRunAutoResolve.removeActionListener(lobbyListener);

        fldMapWidth.removeActionListener(lobbyListener);
        fldMapHeight.removeActionListener(lobbyListener);
        fldSpaceBoardWidth.removeActionListener(lobbyListener);
        fldSpaceBoardHeight.removeActionListener(lobbyListener);

        comboTeam.removeActionListener(lobbyListener);

        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.removeKeyEventDispatcher(lobbyKeyDispatcher);
    }

    /**
     * Returns true if the given list of entities can be configured as a group. This requires that they all have the
     * same owner, and that none of the units are being transported. Also, the owner must be the player or one of his
     * bots.
     */
    boolean canConfigureMultipleDeployment(Collection<Entity> entities) {
        return haveSingleOwner(entities) && !containsTransportedUnit(entities) && canEditAny(entities);
    }

    /**
     * Returns true if the given collection contains at least one entity that the local player can edit, i.e. is his own
     * or belongs to one of his bots. Does not check if the units are otherwise configured, e.g. transported.
     * <p>
     * See also {@link #isEditable(Entity)}
     */
    boolean canEditAny(Collection<Entity> entities) {
        return entities.stream().anyMatch(this::isEditable);
    }

    /**
     * Returns true if the local player can see all the given entities. This is true except when a blind drop option is
     * active and one or more of the entities are not on his team.
     */
    boolean canSeeAll(Collection<Entity> entities) {
        if (!game().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP) &&
              !game().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)) {
            return true;
        }
        for (Entity entity : entities) {
            if (!entityInLocalTeam(entity)) {
                return false;
            }
        }
        return true;
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

    /**
     * Adapts the enabled state of the player config UI items to the player selection.
     */
    private void refreshPlayerConfig() {
        var selPlayers = getSelectedPlayers();
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
        butPrintList.setEnabled(false);
        if (isSinglePlayer) {
            var selPlayer = theElement(selPlayers);
            var hasUnits = !game().getPlayerEntities(selPlayer, false).isEmpty();
            butSaveList.setEnabled(hasUnits && unitsVisible(selPlayer));
            butPrintList.setEnabled(hasUnits && unitsVisible(selPlayer));
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
     * Returns false when any blind-drop option is active and player is not on the local team; true otherwise. When
     * true, individual units of the given player should not be shown/saved/etc.
     */
    private boolean unitsVisible(Player player) {
        var opts = clientgui.getClient().getGame().getOptions();
        boolean isBlindDrop = opts.booleanOption(OptionsConstants.BASE_BLIND_DROP) ||
              opts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
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
                    boolean isLocalBot = clientgui.getLocalBots().get(player.getName()) != null;
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
            JPopupMenu popup = PlayerTablePopup.playerTablePopup(clientgui,
                  playerTableActionListener,
                  getSelectedPlayers(),
                  ServerBoardHelper.getPossibleGameBoard(mapSettings, true));
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private final ActionListener playerTableActionListener = evt -> {
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
                lobbyActions.changeTeam(getSelectedPlayers(), newTeam);
                break;
            case PlayerTablePopup.PTP_BOT_REMOVE:
                removeBot();
                break;
            case PlayerTablePopup.PTP_BOT_SETTINGS:
                doBotSettings();
                break;
            case PlayerTablePopup.PTP_DEPLOY:
                int startPos = Integer.parseInt(st.nextToken());

                for (Player player : getSelectedPlayers()) {
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
            default:
                break;
        }
    };

    private ArrayList<Player> getSelectedPlayers() {
        var result = new ArrayList<Player>();
        for (int row : tablePlayers.getSelectedRows()) {
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
            java.util.List<Entity> entities = getSelectedEntities();
            int code = evt.getKeyCode();
            if ((code == KeyEvent.VK_DELETE) || (code == KeyEvent.VK_BACK_SPACE)) {
                evt.consume();
                lobbyActions.delete(new ArrayList<>(), entities, true);
            } else if (code == KeyEvent.VK_SPACE) {
                evt.consume();
                List<Integer> entityIds = entities.stream().map(Entity::getId).toList();
                LobbyUtility.liveEntityReadoutAction(entityIds, canSeeAll(entities), getClientGUI().getFrame(), game());

            } else if (code == KeyEvent.VK_ENTER) {
                evt.consume();
                if (entities.size() == 1) {
                    lobbyActions.customizeMek(entities.get(0));
                } else if (canConfigureMultipleDeployment(entities)) {
                    lobbyActions.customizeMeks(entities);
                }
            }
        }
    };

    /**
     * Copies the selected units, if any, from the displayed Unit Table / Force Tree to the clipboard.
     */
    public void copyToClipboard() {
        java.util.List<Entity> entities = isForceView() ? getTreeSelectedEntities() : getSelectedEntities();
        StringSelection stringSelection = new StringSelection(clipboardString(entities));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /** Reads the clipboard and adds units, if it can parse them. */
    public void importClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        java.util.List<Entity> newEntities = new ArrayList<>();
        if (hasTransferableText) {
            try {
                String result = (String) contents.getTransferData(DataFlavor.stringFlavor);
                StringTokenizer lines = new StringTokenizer(result, "\n");
                while (lines.hasMoreTokens()) {
                    String line = lines.nextToken();
                    String[] tokens = line.split("\t");
                    if (tokens.length >= 2) {
                        String unitName = (tokens[0] + " " + tokens[1]).trim();
                        MekSummary ms = MekSummaryCache.getInstance().getMek(unitName);
                        if (ms != null) {
                            Entity newEntity = ms.loadEntity();
                            if (newEntity != null) {
                                // Change this to use the player selected in the UI if localPlayer() can do so
                                Client c = getSelectedClient();
                                newEntity.setOwner((c != null) ? c.getLocalPlayer() : localPlayer());
                                newEntities.add(newEntity);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "");
            }

            if (!newEntities.isEmpty()) {
                client().sendAddEntity(newEntities);
                String msg = client().getLocalPlayer() +
                      " loaded units from Clipboard for player: " +
                      localPlayer().getName() +
                      " [" +
                      newEntities.size() +
                      " units]";
                client().sendServerChat(Player.PLAYER_NONE, msg);
            }
        }
    }

    /**
     * @return a String representing the entities to export to the clipboard.
     */
    private String clipboardString(Collection<Entity> entities) {
        StringBuilder result = new StringBuilder();
        for (Entity entity : entities) {
            // Chassis
            result.append(entity.getFullChassis()).append("\t");
            // Model
            result.append(entity.getModel()).append("\t");
            // Weight; format for locale to avoid wrong ",." etc.
            Locale cl = Locale.getDefault();
            NumberFormat numberFormatter = NumberFormat.getNumberInstance(cl);
            result.append(numberFormatter.format(entity.getWeight())).append("\t");
            // Pilot name
            result.append(entity.getCrew().getName()).append("\t");
            // Crew Skill with text
            result.append(CrewSkillSummaryUtil.getSkillNames(entity))
                  .append(": ")
                  .append(entity.getCrew().getSkillsAsString(false))
                  .append("\t");
            // BV without C3 but with pilot (as that gets exported too)
            result.append(entity.calculateBattleValue(true, false)).append("\t");
            result.append("\n");
        }
        return result.toString();
    }

    /**
     * Returns a list of entities selected in the ForceTree. May be empty, but not null.
     */
    private java.util.List<Entity> getTreeSelectedEntities() {
        TreePath[] selection = mekForceTree.getSelectionPaths();
        java.util.List<Entity> entities = new ArrayList<>();
        if (selection != null) {
            for (TreePath path : selection) {
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

    /**
     * Returns a list of forces selected in the ForceTree. May be empty, but not null.
     */
    private java.util.List<Force> getTreeSelectedForces() {
        TreePath[] selection = mekForceTree.getSelectionPaths();
        java.util.List<Force> selForces = new ArrayList<>();
        if (selection != null) {
            for (TreePath path : selection) {
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
            java.util.List<Entity> selEntities = getTreeSelectedEntities();
            java.util.List<Force> selForces = getTreeSelectedForces();
            boolean onlyOneEntity = (selEntities.size() == 1) && selForces.isEmpty();
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_SPACE) {
                e.consume();
                List<Integer> entityIds = selEntities.stream().map(Entity::getId).toList();
                LobbyUtility.liveEntityReadoutAction(entityIds,
                      canSeeAll(selEntities),
                      getClientGUI().getFrame(),
                      game());

            } else if (code == KeyEvent.VK_ENTER && onlyOneEntity) {
                e.consume();
                lobbyActions.customizeMek(selEntities.get(0));

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
                default:
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
            java.util.List<String> boards = lisBoardsAvailable.getSelectedValuesList();
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
                if (path != null && path.getLastPathComponent() instanceof Entity entity) {
                    lobbyActions.customizeMek(entity);
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

        /** Shows the right-click menu on the mek table */
        private void showPopup(MouseEvent e) {
            TreePath path = mekForceTree.getPathForLocation(e.getX(), e.getY());
            // If clicked on a valid row, and it's not selected, select it
            if (path != null && !mekForceTree.isPathSelected(path)) {
                mekForceTree.setSelectionPath(path);
            }

            TreePath[] selection = mekForceTree.getSelectionPaths();

            // If the right mouse button is pressed over an unselected entity,
            // clear the selection and select that entity instead
            List<Entity> entities = new ArrayList<>();
            List<Force> selForces = new ArrayList<>();

            if (selection != null) {
                for (TreePath selPath : selection) {
                    if (selPath != null) {
                        Object selected = selPath.getLastPathComponent();
                        if (selected instanceof Entity) {
                            entities.add((Entity) selected);
                        } else if (selected instanceof Force) {
                            selForces.add((Force) selected);
                        }
                    }
                }
            }

            ScalingPopup popup = LobbyMekPopup.getPopup(entities,
                  selForces,
                  new LobbyMekPopupActions(ChatLounge.this),
                  ChatLounge.this);
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
                    lobbyActions.customizeMek((Entity) entity);
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
            java.util.List<Entity> entities = getSelectedEntities();
            ScalingPopup popup = LobbyMekPopup.getPopup(entities,
                  new ArrayList<>(),
                  new LobbyMekPopupActions(ChatLounge.this),
                  ChatLounge.this);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /** Refreshes the Mek Tree, restoring expansion state and selection. */
    private void refreshTree() {
        // Refresh the force tree and restore selection/expand status
        HashSet<Object> selections = new HashSet<>();
        if (!mekForceTree.isSelectionEmpty()) {
            for (TreePath path : Objects.requireNonNull(mekForceTree.getSelectionPaths())) {
                Object sel = path.getLastPathComponent();
                if (sel instanceof Force || sel instanceof Entity) {
                    selections.add(path.getLastPathComponent());
                }
            }
        }

        Forces forces = game().getForces();
        java.util.List<Integer> expandedForces = new ArrayList<>();
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
        for (int id : expandedForces) {
            if (!forces.contains(id)) {
                continue;
            }
            mekForceTree.expandPath(getPath(forces.getForce(id)));
        }

        mekForceTree.clearSelection();
        for (Object sel : selections) {
            mekForceTree.addSelectionPath(getPath(sel));
        }

    }

    /**
     * Returns a TreePath in the force tree for a possibly outdated entity or force. Outdated means a new object of the
     * type was sent by the server and has replaced this object. Also works for the game's current objects though. Uses
     * the force's/entity's id to get the game's real object with the same id. Used to reconstruct the selection and
     * expansion state of the force tree after an update.
     */
    private TreePath getPath(Object outdatedEntry) {
        Forces forces = game().getForces();
        if (outdatedEntry instanceof Force) {
            if (!forces.contains((Force) outdatedEntry)) {
                return null;
            }
            int forceId = ((Force) outdatedEntry).getId();
            java.util.List<Force> chain = forces.forceChain(forces.getForce(forceId));
            Object[] pathObjs = new Object[chain.size() + 1];
            int index = 0;
            pathObjs[index++] = mekForceTreeModel.getRoot();
            for (Force force : chain) {
                pathObjs[index++] = force;
            }
            return new TreePath(pathObjs);
        } else if (outdatedEntry instanceof Entity) {
            int entityId = ((Entity) outdatedEntry).getId();
            if (game().getEntity(entityId) == null) {
                return null;
            }
            java.util.List<Force> chain = forces.forceChain(game().getEntity(entityId));
            Object[] pathObjs = new Object[chain.size() + 2];
            int index = 0;
            pathObjs[index++] = mekForceTreeModel.getRoot();

            for (Force force : chain) {
                pathObjs[index++] = force;
            }

            pathObjs[index] = game().getEntity(entityId);
            return new TreePath(pathObjs);
        } else {
            throw new IllegalArgumentException(Messages.getString("ChatLounge.TreePath.methodRequiresEntityForce"));
        }
    }

    /**
     * Returns a Collection that contains only those of the given entities that the local player can affect, i.e. his
     * units or those of his bots. The returned Collection is a new Collection and can be safely altered. (The entities
     * are not copies of course.)
     * <p>
     * See also {@link #isEditable(Entity)}
     */
    private Set<Entity> editableEntities(Collection<Entity> entities) {
        return entities.stream().filter(this::isEditable).collect(Collectors.toSet());
    }

    /**
     * Returns true if the given carrier and carried can be edited to have the carrier transport the given carried
     * entity. That is the case when they are teammates and one of the entities can be edited by the local player. Note:
     * this method does NOT check if the loading is rules-valid.
     * <p>
     * See also {@link #isEditable(Entity)}
     */
    private boolean isLoadable(Entity carried, Entity carrier) {
        return !carrier.getOwner().isEnemyOf(carried.getOwner()) && (isEditable(carrier) || isEditable(carried));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        switch (e.getName()) {
            case ClientPreferences.SHOW_UNIT_ID:
                setButUnitIDState();
                mekModel.refreshCells();
                refreshTree();
                break;
            case GUIPreferences.USE_CAMO_OVERLAY:
                clientgui.getTilesetManager().reloadUnitIcons();
                mekModel.refreshCells();
                refreshTree();
                break;
            case ClientPreferences.SHOW_AUTO_RESOLVE_PANEL:
                refreshAcar();
            default:
                break;
        }
    }

    /** Silently adapts the state of the "Show IDs" button to the Client preferences. */
    private void setButUnitIDState() {
        butShowUnitID.removeActionListener(lobbyListener);
        butShowUnitID.setSelected(PreferenceManager.getClientPreferences().getShowUnitId());
        butShowUnitID.addActionListener(lobbyListener);
    }

    /**
     * Sets the row height of the MekTable according to compact mode and GUI scale
     */
    private void setTableRowHeights() {
        mekTable.setRowHeights();
        int rowBaseHeight = butCompact.isSelected() ? MEK_TABLE_ROW_HEIGHT_COMPACT : MEK_TREE_ROW_HEIGHT_FULL;
        mekForceTree.setRowHeight(UIUtil.scaleForGUI(rowBaseHeight));
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
                headerText += "&nbsp;&nbsp;&nbsp;" + UIUtil.fontHTML(uiGray());
                if (activeSorter.getSortingDirection() == Sorting.ASCENDING) {
                    headerText += HEADER_TEXT_ARROW_UP;
                } else {
                    headerText += HEADER_TEXT_ARROW_DOWN;
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

    /**
     * Sets the column width of the given table column of the MekTable with the value stored in the GUIP.
     */
    private void setColumnWidth(TableColumn column) {
        String key;
        if (column.getModelIndex() == MekTableModel.COL_PILOT) {
            key = GUIPreferences.LOBBY_MEK_TABLE_PILOT_WIDTH;
        } else if (column.getModelIndex() == MekTableModel.COL_UNIT) {
            key = GUIPreferences.LOBBY_MEK_TABLE_UNIT_WIDTH;
        } else if (column.getModelIndex() == MekTableModel.COL_PLAYER) {
            key = GUIPreferences.LOBBY_MEK_TABLE_PLAYER_WIDTH;
        } else if (column.getModelIndex() == MekTableModel.COL_BV) {
            key = GUIPreferences.LOBBY_MEK_TABLE_BV_WIDTH;
        } else {
            return;
        }
        column.setPreferredWidth(GUIP.getInt(key));
    }

    /**
     * Mouse Listener for the table header of the Mek Table. Saves column widths of the Mek Table when the mouse button
     * is released. Also switches between table sorting types
     */
    MouseListener mekTableHeaderMouseListener = new MouseAdapter() {
        private void changeSorter(MouseEvent e) {
            // Save table widths
            for (int i = 0; i < MekTableModel.N_COL; i++) {
                TableColumn column = mekTable.getColumnModel().getColumn(i);
                String key;
                if (column.getModelIndex() == MekTableModel.COL_PILOT) {
                    key = GUIPreferences.LOBBY_MEK_TABLE_PILOT_WIDTH;
                } else if (column.getModelIndex() == MekTableModel.COL_UNIT) {
                    key = GUIPreferences.LOBBY_MEK_TABLE_UNIT_WIDTH;
                } else if (column.getModelIndex() == MekTableModel.COL_PLAYER) {
                    key = GUIPreferences.LOBBY_MEK_TABLE_PLAYER_WIDTH;
                } else if (column.getModelIndex() == MekTableModel.COL_BV) {
                    key = GUIPreferences.LOBBY_MEK_TABLE_BV_WIDTH;
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
            var opts = clientgui.getClient().getGame().getOptions();
            for (MekTableSorter sorter : union(unitSorters, bvSorters)) {
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
     * Sets the sorting used in the Mek Table depending on the column header that was clicked.
     */
    private void changeMekTableSorter(MouseEvent e) {
        int col = mekTable.columnAtPoint(e.getPoint());
        MekTableSorter previousSorter = activeSorter;
        java.util.List<MekTableSorter> sorters;

        // find the right list of sorters (or do nothing, if the column is not sortable)
        if (col == MekTableModel.COL_UNIT) {
            sorters = unitSorters;
        } else if (col == MekTableModel.COL_BV) {
            sorters = bvSorters;
        } else {
            return;
        }

        // Select the next allowed sorter and refresh the display if the sorter was
        // changed
        nextSorter(sorters);
        if (activeSorter != previousSorter) {
            refreshMekTable();
            updateTableHeaders();
        }
    }

    /** Selects the next allowed sorter in the given list of sorters. */
    private void nextSorter(java.util.List<MekTableSorter> sorters) {
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
        while (!activeSorter.isAllowed(clientgui.getClient().getGame().getOptions()) && ++counter < 100) {
            index = (index + 1) % sorters.size();
            activeSorter = sorters.get(index);
        }
    }

    /** Returns true when the compact view is active. */
    public boolean isCompact() {
        return butCompact.isSelected();
    }

    /**
     * Returns a list of the selected entities in the Mek table. The list may be empty but not null.
     */
    private java.util.List<Entity> getSelectedEntities() {
        ArrayList<Entity> result = new ArrayList<>();
        int[] rows = mekTable.getSelectedRows();
        for (int row : rows) {
            InGameObject unit = mekModel.getEntityAt(row);
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
            if (lisBoardsAvailable.getFixedCellHeight() != image.getHeight(null) ||
                  lisBoardsAvailable.getFixedCellWidth() != image.getWidth(null)) {
                lisBoardsAvailable.setFixedCellHeight(image.getHeight(null));
                lisBoardsAvailable.setFixedCellWidth(image.getWidth(null));
            }
            lisBoardsAvailable.repaint();
        }
    }

    class ImageLoader extends SwingWorker<Void, Image> {

        private final BlockingQueue<String> boards = new LinkedBlockingQueue<>();

        private synchronized void add(String name) {
            if (!boards.contains(name)) {
                try {
                    boards.put(name);
                } catch (InterruptedException e) {
                    LOGGER.warn(e,
                          "[Thread=({}){}] Failed to load image {} for board, common on startup",
                          Thread.currentThread().getId(),
                          Thread.currentThread().getName(),
                          name);
                } catch (Exception e) {
                    LOGGER.error(e,
                          "[Thread=({}){}] Failed to load image {} for board",
                          Thread.currentThread().getId(),
                          Thread.currentThread().getName(),
                          name);
                }
            }
        }

        private Image prepareImage(String boardName) {
            File boardFile = new MegaMekFile(Configuration.boardsDir(),
                  boardName + MMConstants.CL_KEY_FILE_EXTENSION_BOARD).getFile();
            Board board;
            java.util.List<String> errors = new ArrayList<>();
            if (boardFile.exists()) {
                board = new Board();
                try (InputStream is = new FileInputStream(boardFile)) {
                    board.load(is, errors, true);
                } catch (IOException ex) {
                    board = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
                }
            } else {
                board = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
            }

            // Determine a minimap zoom from the board size and gui scale. This is very magic numbers but currently
            // the minimap has only fixed zoom states.
            int zoom = getZoom(board);
            BufferedImage bufImage = MinimapPanel.getMinimapImage(board, zoom);

            // Add the board name label and the server-side board label if necessary
            String text = LobbyUtility.cleanBoardName(boardName, mapSettings);
            Graphics g = bufImage.getGraphics();
            if (!errors.isEmpty()) {
                invalidBoards.add(boardName);
            }
            drawMinimapLabel(text, bufImage.getWidth(), bufImage.getHeight(), g, !errors.isEmpty());
            if (!boardFile.exists() && !boardName.startsWith(MapSettings.BOARD_GENERATED)) {
                serverBoards.add(boardName);
                markServerSideBoard(bufImage);
            }
            g.dispose();

            synchronized (baseImages) {
                baseImages.put(boardName, bufImage);
            }
            return bufImage;
        }

        private int getZoom(Board board) {
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
            zoom = (int) (scale * zoom);

            if (zoom > 6) {
                zoom = 6;
            }

            if (zoom < 0) {
                zoom = 0;
            }

            return zoom;
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
    public class BoardNameRenderer extends DefaultListCellRenderer {
        @Serial
        private static final long serialVersionUID = -3218595828938299222L;

        private float oldGUIScale = GUIP.getGUIScale();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {

            String board = (String) value;
            // For generated boards, add the size to have different images for different
            // sizes
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
            ImageIcon icon = mapIcons.get(board);
            if (icon != null) {
                setIcon(icon);
            } else {
                // The icon is not present, see if there's a base image
                Image image;

                synchronized (baseImages) {
                    image = baseImages.get(board);
                }

                if (image == null) {
                    // There's no base image: trigger loading it and, for now, return the base list's panel The
                    // [GENERATED] entry will always land here as well
                    loader.add(board);
                    setToolTipText(null);
                    return super.getListCellRendererComponent(list,
                          new File(board).getName(),
                          index,
                          isSelected,
                          cellHasFocus);
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
                setToolTipText(createBoardTooltip(board));
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

        public MekTable(MekTableModel mekModel) {
            super(mekModel);
            mekModel.addTableModelListener(e -> setRowHeights());
            setRowHeights();
        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {
            setRowHeights();
            super.columnMarginChanged(e);
        }

        private void setRowHeights() {
            int rowBaseHeight = butCompact.isSelected() ? MEK_TABLE_ROW_HEIGHT_COMPACT : MEK_TABLE_ROW_HEIGHT_FULL;
            setRowHeight(UIUtil.scaleForGUI(rowBaseHeight));
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
        for (MapPreviewButton button : mapButtons) {
            button.scheduleRescale();
            haveImages |= button.hasBoard();
        }
        if (!haveImages) {
            Dimension size = maxMapButtonSize();
            for (MapPreviewButton button : mapButtons) {
                button.setPreviewSize(size);
            }
        }
    }

    void updateMapButtons(Dimension size) {
        if (!currentMapButtonSize.equals(size)) {
            currentMapButtonSize = size;
            for (MapPreviewButton button : mapButtons) {
                button.setPreviewSize(size);
            }
        }
    }

    Dimension maxMapButtonSize() {
        // minus 1 to ensure that the images actually fit in the frame
        int pw = (int) Math.floor(panMapButtons.getWidth() / (double) mapSettings.getMapWidth()) - 1;
        int ph = (int) Math.floor(panMapButtons.getHeight() / (double) mapSettings.getMapHeight()) - 1;
        return new Dimension(pw, ph);
    }

    Dimension optMapButtonSize(Image image) {
        Dimension optSize = maxMapButtonSize();
        double factorX = optSize.width / (double) image.getWidth(null);
        double factorY = optSize.height / (double) image.getHeight(null);
        double factor = Math.min(factorX, factorY);
        int w = (int) (factor * image.getWidth(null));
        int h = (int) (factor * image.getHeight(null));
        return new Dimension(w, h);
    }

    /**
     * Returns true when the string boardName contains an invalid board. boardName may denote a generated board (which
     * is never invalid) or a surprise board with several actual board names attached which will return true when at
     * least one of the boards is invalid.
     */
    boolean hasInvalidBoard(String boardName) {
        return hasSpecialBoard(boardName, invalidBoards);
    }

    /**
     * Returns true when the string boardName contains a board that isn't present on the client (only on the server).
     * boardName may denote a generated board (which is never serverside) or a surprise board with several actual board
     * names attached which will return true when at least one of the boards is serverside.
     */
    boolean hasServerSideBoard(String boardName) {
        return hasSpecialBoard(boardName, serverBoards);
    }

    /**
     * Returns true when boardName (if a single board) or any of the boards contained in boardName (if a surprise board
     * list) is contained in the provided list. Returns false for generated boards.
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
     * Returns a tooltip for the provided boardName that may be a single board or a generated or surprise board. Adds
     * info for serverside or invalid boards.
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

    ActionListener mekTableHeaderAListener = e -> {
        MekTableSorter previousSorter = activeSorter;
        for (MekTableSorter sorter : union(unitSorters, bvSorters)) {
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

    /** Convenience for clientGUI.getClient() */
    Client client() {
        return clientgui.getClient();
    }

    boolean isForceView() {
        return butForceView.isSelected();
    }

    public void killPreviewBV() {
        if (previewBV != null) {
            previewBV.dispose();
        }
    }

    @Override
    public ClientGUI getClientGUI() {
        return clientgui;
    }
}
