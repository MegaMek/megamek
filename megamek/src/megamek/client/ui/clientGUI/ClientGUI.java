/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.MMConstants;
import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.TimerSingleton;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.commands.*;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.MekDisplayEvent;
import megamek.client.event.MekDisplayListener;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.audio.AudioService;
import megamek.client.ui.clientGUI.audio.SoundManager;
import megamek.client.ui.clientGUI.audio.SoundType;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.CollapseWarning;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.overlay.ChatterBoxOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.OffBoardTargetOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.PlanetaryConditionsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.TurnDetailsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.UnitOverviewOverlay;
import megamek.client.ui.clientGUI.boardview.spriteHandler.*;
import megamek.client.ui.clientGUI.boardview.toolTip.TWBoardViewTooltip;
import megamek.client.ui.dialogs.AccessibilityDialog;
import megamek.client.ui.dialogs.BotCommands.BotCommandsDialog;
import megamek.client.ui.dialogs.BotCommands.BotCommandsPanel;
import megamek.client.ui.dialogs.ChoiceDialog;
import megamek.client.ui.dialogs.CommonAboutDialog;
import megamek.client.ui.dialogs.ConfirmDialog;
import megamek.client.ui.dialogs.InformDialog;
import megamek.client.ui.dialogs.PlayerListDialog;
import megamek.client.ui.dialogs.RandomNameDialog;
import megamek.client.ui.dialogs.RulerDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.buttonDialogs.EditBotsDialog;
import megamek.client.ui.dialogs.buttonDialogs.GameOptionsDialog;
import megamek.client.ui.dialogs.buttonDialogs.LOSDialog;
import megamek.client.ui.dialogs.forceDisplay.ForceDisplayDialog;
import megamek.client.ui.dialogs.forceDisplay.ForceDisplayPanel;
import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.client.ui.dialogs.helpDialogs.HelpDialog;
import megamek.client.ui.dialogs.helpDialogs.MMReadMeHelpDialog;
import megamek.client.ui.dialogs.miniReport.MiniReportDisplayDialog;
import megamek.client.ui.dialogs.miniReport.MiniReportDisplayPanel;
import megamek.client.ui.dialogs.minimap.MinimapDialog;
import megamek.client.ui.dialogs.minimap.MinimapPanel;
import megamek.client.ui.dialogs.phaseDisplay.NovaNetworkViewDialog;
import megamek.client.ui.dialogs.randomArmy.RandomArmyDialog;
import megamek.client.ui.dialogs.unitDisplay.IHasUnitDisplay;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayDialog;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.dialogs.unitSelectorDialogs.MegaMekUnitSelectorDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.ReceivingGameDataPanel;
import megamek.client.ui.panels.StartingScenarioPanel;
import megamek.client.ui.panels.WaitingForServerPanel;
import megamek.client.ui.panels.phaseDisplay.*;
import megamek.client.ui.panels.phaseDisplay.lobby.ChatLounge;
import megamek.client.ui.panels.phaseDisplay.lobby.PlayerSettingsDialog;
import megamek.client.ui.tileset.EntityImage;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.tileset.TilesetManager;
import megamek.client.ui.util.BASE64ToolKit;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameScriptedEvent;
import megamek.common.event.GameScriptedMessageEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.board.GameBoardNewEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.event.player.GamePlayerChangeEvent;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.event.player.GamePlayerDisconnectedEvent;
import megamek.common.game.Game;
import megamek.common.hexArea.HexArea;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.moves.MovePath;
import megamek.common.options.GameOptions;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;
import megamek.common.units.IBomber;
import megamek.common.units.Targetable;
import megamek.common.util.AddBotUtil;
import megamek.common.util.Distractable;
import megamek.common.util.StringUtil;
import megamek.common.weapons.handlers.WeaponOrderHandler;
import megamek.logging.MMLogger;

public class ClientGUI extends AbstractClientGUI
      implements BoardViewListener, ActionListener, IPreferenceChangeListener, MekDisplayListener, ILocalBots,
                 IDisconnectSilently, IHasUnitDisplay, IHasBoardView, IHasMenuBar, IHasCurrentPanel {
    private final static MMLogger logger = MMLogger.create(ClientGUI.class);

    // region Variable Declarations
    // region action commands
    // region main menu
    // Note: anything located in menu bars is not located here but in their menu
    public static final String MAIN_SKIN_NEW = "mainSkinNew";
    public static final String MAIN_QUIT = "mainQuit";
    // endregion
    // region file menu
    // game submenu
    public static final String FILE_GAME_NEW = "fileGameNew";
    public static final String FILE_GAME_SAVE = "fileGameSave";
    public static final String FILE_GAME_LOAD = "fileGameLoad";
    public static final String FILE_GAME_SAVE_SERVER = "fileGameSaveServer";
    public static final String FILE_GAME_QUICK_SAVE = "fileGameQSave";
    public static final String FILE_GAME_QUICK_LOAD = "fileGameQLoad";
    public static final String FILE_GAME_SCENARIO = "fileGameScenario";
    public static final String FILE_GAME_CONNECT_BOT = "fileGameConnectBot";
    public static final String FILE_GAME_CONNECT = "fileGameConnect";
    public static final String FILE_GAME_CONNECT_SBF = "fileGameConnectSbf";
    public static final String FILE_GAME_EDIT_BOTS = "editBots";
    // board submenu
    public static final String BOARD_NEW = "fileBoardNew";
    public static final String BOARD_OPEN = "fileBoardOpen";
    public static final String BOARD_RECENT = "recent";
    public static final String BOARD_SAVE = "fileBoardSave";
    public static final String BOARD_SAVE_AS = "fileBoardSaveAs";
    public static final String BOARD_SAVE_AS_IMAGE = "fileBoardSaveAsImage";
    public static final String BOARD_SAVE_AS_IMAGE_UNITS = "fileBoardSaveAsImageUnits";
    public static final String BOARD_RESIZE = "boardResize";
    public static final String BOARD_VALIDATE = "boardValidate";
    public static final String BOARD_SOURCE_FILE = "boardSourcefile";
    public static final String BOARD_UNDO = "boardUndo";
    public static final String BOARD_REDO = "boardRedo";
    public static final String BOARD_RAISE = "boardRaise";
    public static final String BOARD_CLEAR = "boardClear";
    public static final String BOARD_FLOOD = "boardFlood";
    public static final String BOARD_REMOVE_FORESTS = "boardRemoveForests";
    public static final String BOARD_REMOVE_ROADS = "boardRemoveRoads";
    public static final String BOARD_REMOVE_WATER = "boardRemoveWater";
    public static final String BOARD_REMOVE_BUILDINGS = "boardRemoveBuildings";
    public static final String BOARD_FLATTEN = "boardFlatten";

    // unit list submenu
    public static final String FILE_UNITS_REINFORCE = "fileUnitsReinforce";
    public static final String FILE_UNITS_REINFORCE_RAT = "fileUnitsReinforceRAT";
    public static final String FILE_REFRESH_CACHE = "fileRefreshCache";
    public static final String FILE_UNITS_BROWSE = "fileUnitsBrowse";
    public static final String FILE_UNITS_OPEN = "fileUnitsOpen";
    public static final String FILE_UNITS_SAVE = "fileUnitsSave";
    public static final String FILE_UNITS_PASTE = "fileUnitsPaste";
    public static final String FILE_UNITS_COPY = "fileUnitsCopy";
    // endregion file menu

    // region view menu
    public static final String VIEW_INC_GUI_SCALE = "viewIncGUIScale";
    public static final String VIEW_DEC_GUI_SCALE = "viewDecGUIScale";
    public static final String VIEW_FORCE_DISPLAY = "viewForceDisplay";
    public static final String VIEW_NOVA_NETWORKS = "viewNovaNetworks";
    public static final String VIEW_UNIT_DISPLAY = "viewMekDisplay";
    public static final String VIEW_ACCESSIBILITY_WINDOW = "viewAccessibilityWindow";
    public static final String VIEW_KEYBINDS_OVERLAY = "viewKeyboardShortcuts";
    public static final String VIEW_PLANETARY_CONDITIONS_OVERLAY = "viewPlanetaryConditions";
    public static final String VIEW_TRACE_OVERLAY = "viewTraceOverlay";
    public static final String VIEW_MINI_MAP = "viewMinimap";
    public static final String VIEW_UNIT_OVERVIEW = "viewUnitOverview";
    public static final String VIEW_ZOOM_IN = "viewZoomIn";
    public static final String VIEW_ZOOM_OUT = "viewZoomOut";
    public static final String VIEW_ZOOM_OVERVIEW_TOGGLE = "viewZoomOverviewToggle";
    public static final String VIEW_TOGGLE_ISOMETRIC = "viewToggleIsometric";
    public static final String VIEW_TOGGLE_HEX_COORDS = "viewToggleHexCoords";
    public static final String VIEW_LABELS = "viewLabels";
    public static final String VIEW_TOGGLE_FIELD_OF_FIRE = "viewToggleFieldOfFire";
    public static final String VIEW_TOGGLE_FLEE_ZONE = "viewToggleFleeZone";
    public static final String VIEW_TOGGLE_SENSOR_RANGE = "viewToggleSensorRange";
    public static final String VIEW_TOGGLE_FOV_DARKEN = "viewToggleFovDarken";
    public static final String VIEW_TOGGLE_FOV_HIGHLIGHT = "viewToggleFovHighlight";
    public static final String VIEW_TOGGLE_FIRING_SOLUTIONS = "viewToggleFiringSolutions";
    public static final String VIEW_TOGGLE_CF_WARNING = "viewToggleCFWarnings";
    public static final String VIEW_MOVE_ENV = "viewMovementEnvelope";
    public static final String VIEW_TURN_DETAILS_OVERLAY = "viewTurnDetailsOverlay";
    public static final String VIEW_MOVE_MOD_ENV = "viewMovModEnvelope";
    public static final String VIEW_CHANGE_THEME = "viewChangeTheme";
    public static final String VIEW_ROUND_REPORT = "viewRoundReport";
    public static final String VIEW_GAME_OPTIONS = "viewGameOptions";
    public static final String VIEW_CLIENT_SETTINGS = "viewClientSettings";
    public static final String VIEW_LOS_SETTING = "viewLOSSetting";
    public static final String VIEW_PLAYER_SETTINGS = "viewPlayerSettings";
    public static final String VIEW_PLAYER_LIST = "viewPlayerList";
    public static final String VIEW_RESET_WINDOW_POSITIONS = "viewResetWindowPos";
    public static final String VIEW_BOT_COMMANDS = "viewBotCommands";
    // endregion view menu

    // region fire menu
    public static final String FIRE_SAVE_WEAPON_ORDER = "saveWeaponOrder";
    // endregion fire menu

    // region help menu
    public static final String HELP_CONTENTS = "helpContents";
    public static final String HELP_SKINNING = "helpSkinning";
    public static final String HELP_ABOUT = "helpAbout";
    public static final String HELP_RESET_NAGS = "helpResetNags";
    // endregion help menu
    // endregion action commands

    public static final String CG_BOARD_VIEW = "BoardView";
    public static final String CG_CHAT_LOUNGE = "ChatLounge";
    public static final String CG_STARTING_SCENARIO = "JLabel-StartingScenario";
    public static final String CG_EXCHANGE = "JLabel-Exchange";
    public static final String CG_SELECT_ARTY_AUTO_HIT_HEX_DISPLAY = "SelectArtyAutoHitHexDisplay";
    public static final String CG_DEPLOY_MINEFIELD_DISPLAY = "DeployMinefieldDisplay";
    public static final String CG_DEPLOYMENT_DISPLAY = "DeploymentDisplay";
    public static final String CG_TARGETING_PHASE_DISPLAY = "TargetingPhaseDisplay";
    public static final String CG_PREMOVEMENT_DISPLAY = "PremovementDisplay";
    public static final String CG_MOVEMENT_DISPLAY = "MovementDisplay";
    public static final String CG_OFFBOARD_DISPLAY = "OffboardDisplay";
    public static final String CG_PRE_FIRING = "Prefiring";
    public static final String CG_FIRING_DISPLAY = "FiringDisplay";
    public static final String CG_POINTBLANK_SHOT_DISPLAY = "PointblankShotDisplay";
    public static final String CG_PHYSICAL_DISPLAY = "PhysicalDisplay";
    public static final String CG_REPORT_DISPLAY = "ReportDisplay";
    public static final String CG_DEFAULT = "JLabel-Default";

    public static final String CG_CHAT_COMMAND_SAVE = "/save";
    public static final String CG_CHAT_COMMAND_LOCAL_SAVE = "/localsave";

    public static final String CG_FILE_URL_START = "file:///";
    public static final String CG_FILEPATH_USER_DIR = "user.dir";
    public static final String CG_FILEPATH_BOARDS = "boards";
    public static final String CG_FILEPATH_DATA = "data";
    public static final String CG_FILENAME_SALVAGE = "salvage";
    public static final String CG_FILEPATH_MUL = "mul";
    public static final String CG_FILE_EXTENSION_BOARD = ".board";
    public static final String CG_FILE_EXTENSION_MUL = ".mul";
    public static final String CG_FILE_EXTENSION_XML = ".xml";
    public static final String CG_FILE_EXTENSION_PNG = ".png";
    public static final String CG_FILE_FORMAT_NAME_PNG = "png";

    // a frame, to show stuff in
    private final JPanel clientGuiPanel = new JPanel();

    // A menu bar to contain all actions.
    protected CommonMenuBar menuBar;
    private AbstractHelpDialog help;
    private CommonSettingsDialog commonSettingsDialog;
    private AccessibilityDialog aw;

    public MegaMekController controller;
    private ChatterBox cb;
    private boolean wasBoardFocused = false;
    private MovementEnvelopeSpriteHandler movementEnvelopeHandler;
    private MovementModifierSpriteHandler movementModifierSpriteHandler;
    private FleeZoneSpriteHandler fleeZoneSpriteHandler;
    private SensorRangeSpriteHandler sensorRangeSpriteHandler;
    private CollapseWarningSpriteHandler collapseWarningSpriteHandler;
    private GroundObjectSpriteHandler groundObjectSpriteHandler;
    private FiringSolutionSpriteHandler firingSolutionSpriteHandler;
    private FiringArcSpriteHandler firingArcSpriteHandler;

    private JPanel panTop;
    private JSplitPane splitPaneA;
    private JPanel panA1;
    private JPanel panA2;

    private final UnitDisplayPanel unitDisplayPanel;
    private UnitDisplayDialog unitDisplayDialog;

    private BotCommandsDialog botCommandsDialog;

    public ForceDisplayPanel forceDisplayPanel;
    private ForceDisplayDialog forceDisplayDialog;
    private MapMenu popup;
    private RulerDialog ruler;
    protected JComponent curPanel;
    public ChatLounge chatlounge;
    private OffBoardTargetOverlay offBoardOverlay;

    // some dialogs...
    private GameOptionsDialog gameOptionsDialog;
    private MegaMekUnitSelectorDialog mekSelectorDialog;
    private PlayerListDialog playerListDialog;
    private RandomArmyDialog randomArmyDialog;
    /**
     * Save and Open dialogs for MegaMek Unit List (mul) files.
     */
    private JFileChooser dlgLoadList;
    private JFileChooser dlgSaveList;
    private JFileChooser dlgSaveGifList;
    private final Client client;

    private File curFileBoardImage;
    private File curFileBoard;

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private final Map<String, String> mainNames = new HashMap<>();

    private MiniReportDisplayPanel miniReportDisplayPanel;
    private MiniReportDisplayDialog miniReportDisplayDialog;

    /**
     * Boolean indicating whether client should be disconnected without a pop-up warning
     **/
    private boolean disconnectQuietly = false;

    /**
     * The <code>JPanel</code> containing the main display area.
     */
    private final JPanel panMain = new JPanel();

    /**
     * The <code>CardLayout</code> of the main display area.
     */
    private final CardLayout cardsMain = new CardLayout();

    /**
     * Map each phase to the name of the card for the secondary area.
     */
    private final Map<String, String> secondaryNames = new HashMap<>();

    /**
     * The <code>JPanel</code> containing the secondary display area.
     */
    private final JPanel panSecondary = new JPanel();

    private ReportDisplay reportDisplay;

    private StatusBarPhaseDisplay currPhaseDisplay;

    /**
     * The <code>CardLayout</code> of the secondary display area.
     */
    private final CardLayout cardsSecondary = new CardLayout();

    /**
     * Map phase component names to phase component objects.
     */
    private final Map<String, JComponent> phaseComponents = new HashMap<>();

    /**
     * Flag that indicates whether hotkeys should be ignored or not. This is used for disabling hot keys when various
     * dialogs are displayed.
     */
    private boolean ignoreHotKeys = false;

    /**
     * Keeps track of the Entity ID for the entity currently taking a pointblank shot.
     */
    private int pointblankEID = Entity.NONE;

    /**
     * Audio system
     */
    private final AudioService audioService = new SoundManager();

    private Coords currentHex;

    private boolean showFleeZone = false;

    private final TilesetManager tilesetManager;

    // endregion Variable Declarations

    /**
     * Construct a client which will display itself in a new frame. It will not try to connect to a server yet. When the
     * frame closes, this client will clean up after itself as much as possible, but will not call System.exit().
     */
    public ClientGUI(Client client, MegaMekController c) {
        super(client);
        this.client = client;
        controller = c;
        initializeSpriteHandlers();
        panMain.setLayout(cardsMain);
        panSecondary.setLayout(cardsSecondary);

        clientGuiPanel.setLayout(new BorderLayout());
        ComponentListener resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                boardViewsContainer.getPanel().setPreferredSize(clientGuiPanel.getSize());
            }
        };
        clientGuiPanel.addComponentListener(resizeListener);
        clientGuiPanel.add(panMain, BorderLayout.CENTER);
        clientGuiPanel.add(panSecondary, BorderLayout.SOUTH);

        audioService.loadSoundFiles();

        unitDisplayPanel = new UnitDisplayPanel(this, controller);
        try {
            tilesetManager = new TilesetManager(client.getGame());
        } catch (IOException e) {
            // this is fatal
            throw new RuntimeException(e);
        }

        registerCommand(new HelpCommand(this));
        registerCommand(new MoveCommand(this));
        registerCommand(new RulerCommand(this));
        registerCommand(new ShowEntityCommand(this));
        registerCommand(new FireCommand(this));
        registerCommand(new DeployCommand(this));
        registerCommand(new AddBotCommand(this));
        registerCommand(new AssignNovaNetworkCommand(this));
        registerCommand(new SituationReportCommand(this));
        registerCommand(new LookCommand(this));
        registerCommand(new ChatCommand(this));
        registerCommand(new DoneCommand(this));
        ShowTileCommand tileCommand = new ShowTileCommand(this);
        registerCommand(tileCommand);
        for (String direction : ShowTileCommand.directions) {
            clientCommands.put(direction.toLowerCase(), tileCommand);
        }
        registerCommand(new HelpCommand(this));
        registerCommand(new BotHelpCommand(this));
    }

    private void initializeFocusTracking() {
        boardViewsContainer.getPanel().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                wasBoardFocused = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                wasBoardFocused = false;
                setChatBoxActive(false);
            }
        });
    }

    @Override
    public BoardView getBoardView() {
        return (BoardView) boardViews.get(0);
    }

    @Override
    public UnitDisplayPanel getUnitDisplay() {
        return unitDisplayPanel;
    }

    public UnitDisplayDialog getUnitDisplayDialog() {
        return unitDisplayDialog;
    }

    public void setUnitDisplayDialog(final UnitDisplayDialog unitDisplayDialog) {
        this.unitDisplayDialog = unitDisplayDialog;
        this.unitDisplayDialog.setFocusableWindowState(false);
    }

    public ForceDisplayPanel getForceDisplayPanel() {
        return forceDisplayPanel;
    }

    public void setForceDisplayPanel(final ForceDisplayPanel forceDisplayPanel) {
        this.forceDisplayPanel = forceDisplayPanel;
    }

    public ForceDisplayDialog getForceDisplayDialog() {
        return forceDisplayDialog;
    }

    public void setForceDisplayDialog(final ForceDisplayDialog forceDisplayDialog) {
        this.forceDisplayDialog = forceDisplayDialog;
        this.forceDisplayDialog.setFocusableWindowState(false);
    }

    public MinimapDialog getMiniMapDialog() {
        return miniMaps.get(0);
    }

    public BotCommandsDialog getBotCommandsDialog() {
        return botCommandsDialog;
    }

    public void setBotCommandsDialog(BotCommandsDialog botCommandsDialog) {
        this.botCommandsDialog = botCommandsDialog;
    }

    public MiniReportDisplayPanel getMiniReportDisplay() {
        return miniReportDisplayPanel;
    }

    public void setMiniReportDisplay(final MiniReportDisplayPanel miniReportDisplayPanel) {
        this.miniReportDisplayPanel = miniReportDisplayPanel;
    }

    public MiniReportDisplayDialog getMiniReportDisplayDialog() {
        return miniReportDisplayDialog;
    }

    public void setMiniReportDisplayDialog(final MiniReportDisplayDialog miniReportDisplayDialog) {
        this.miniReportDisplayDialog = miniReportDisplayDialog;
        this.miniReportDisplayDialog.setFocusableWindowState(false);
    }

    public PlayerListDialog getPlayerListDialog() {
        return playerListDialog;
    }

    public void setPlayerListDialog(final PlayerListDialog playerListDialog) {
        this.playerListDialog = playerListDialog;
        this.playerListDialog.setFocusableWindowState(false);
    }

    @Override
    public void setDisconnectQuietly(boolean quietly) {
        disconnectQuietly = quietly;
    }

    /**
     * Display a system message in the chat box.
     *
     * @param message the <code>String</code> message to be shown.
     */
    public void systemMessage(String message) {
        client.getGame().fireGameEvent(new GamePlayerChatEvent(this, null,
              "\n" + Messages.getString("ChatterBox.MegaMek") + " " + message));
    }

    /**
     * Initializes a number of things about this frame.
     */
    @Override
    protected void initializeFrame() {
        super.initializeFrame();
        menuBar = CommonMenuBar.getMenuBarForGame();
        frame.setJMenuBar(menuBar);
    }

    /**
     * Lays out the frame by setting this Client object to take up the full frame display area.
     */
    private void layoutFrame() {
        updateFrameTitle();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(clientGuiPanel, BorderLayout.CENTER);
        frame.validate();
    }

    /**
     * Updates the frame title to show the current round and phase information. The title format is: "PlayerName - Round
     * X - Phase phase - MegaMek" For phases before the game starts (lobby, selection, etc.), only shows: "PlayerName -
     * MegaMek"
     */
    private void updateFrameTitle() {
        StringBuilder title = new StringBuilder(client.getName());

        GamePhase phase = client.getGame().getPhase();
        int round = client.getGame().getCurrentRound();

        // Only show round/phase info for in-game phases (after round 0)
        if ((round > 0) && phase.isOnMap()) {
            title.append(Messages.getString("ClientGUI.titleRoundPhase", round, phase.localizedName()));
        }

        title.append(Messages.getString("ClientGUI.clientTitleSuffix"));
        frame.setTitle(title.toString());
    }

    private void initializeSpriteHandlers() {
        movementEnvelopeHandler = new MovementEnvelopeSpriteHandler(this, client.getGame());
        movementModifierSpriteHandler = new MovementModifierSpriteHandler(this, client.getGame());
        FlareSpritesHandler flareSpritesHandler = new FlareSpritesHandler(this, client.getGame());
        sensorRangeSpriteHandler = new SensorRangeSpriteHandler(this, client.getGame());
        collapseWarningSpriteHandler = new CollapseWarningSpriteHandler(this);
        groundObjectSpriteHandler = new GroundObjectSpriteHandler(this, client.getGame());
        firingSolutionSpriteHandler = new FiringSolutionSpriteHandler(this, client);
        firingArcSpriteHandler = new FiringArcSpriteHandler(this);
        fleeZoneSpriteHandler = new FleeZoneSpriteHandler(this);

        spriteHandlers.addAll(List.of(movementEnvelopeHandler,
              movementModifierSpriteHandler,
              sensorRangeSpriteHandler,
              flareSpritesHandler,
              collapseWarningSpriteHandler,
              groundObjectSpriteHandler,
              firingSolutionSpriteHandler,
              firingArcSpriteHandler,
              fleeZoneSpriteHandler));
        spriteHandlers.forEach(BoardViewSpriteHandler::initialize);
    }

    @Override
    public void initialize() {
        menuBar = CommonMenuBar.getMenuBarForGame();
        frame.setJMenuBar(menuBar);
        initializeFrame();
        super.initialize();
        boardViewsContainer.setName(CG_BOARD_VIEW);
        try {
            client.getGame().addGameListener(gameListener);

            panTop = new JPanel(new BorderLayout());
            panA1 = new JPanel();
            panA1.setVisible(false);
            panA2 = new JPanel();
            panA2.setVisible(false);

            splitPaneA = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

            splitPaneA.setDividerSize(10);
            splitPaneA.setResizeWeight(0.5);

            splitPaneA.setLeftComponent(panA1);
            splitPaneA.setRightComponent(panA2);

            panTop.add(splitPaneA, BorderLayout.CENTER);
            initializeFocusTracking();

            cb = new ChatterBox(this);

        } catch (Exception ex) {
            logger.fatal(ex, "initialize");
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"),
                  Messages.getString("ClientGUI.FatalError.message") + ex);
            die();
        }

        layoutFrame();
        menuBar.addActionListener(this);

        aw = new AccessibilityDialog(this);
        aw.setLocation(0, 0);
        aw.setSize(300, 300);

        unitDisplayPanel.addMekDisplayListener(this);
        setUnitDisplayDialog(new UnitDisplayDialog(getFrame(), this));
        getUnitDisplayDialog().setVisible(false);

        setForceDisplayPanel(new ForceDisplayPanel(this));
        setForceDisplayDialog(new ForceDisplayDialog(getFrame(), this));
        getForceDisplayDialog().add(getForceDisplayPanel(), BorderLayout.CENTER);
        getForceDisplayDialog().setVisible(false);

        setMiniReportDisplay(new MiniReportDisplayPanel(this));
        setMiniReportDisplayDialog(new MiniReportDisplayDialog(getFrame(), this));
        getMiniReportDisplayDialog().setVisible(false);
        setMiniReportVisible(GUIP.getMiniReportEnabled());

        setPlayerListDialog(new PlayerListDialog(frame, client, false));

        RulerDialog.color1 = GUIP.getRulerColor1();
        RulerDialog.color2 = GUIP.getRulerColor2();

        setBotCommandsDialog(new BotCommandsDialog(frame, this));
        getBotCommandsDialog().add(new BotCommandsPanel(getClient(), audioService, null));

        client.changePhase(GamePhase.UNKNOWN);
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MekSummaryCache.getInstance().isInitialized()) {
            unitLoadingDialog.setVisible(true);
        }
        mekSelectorDialog = new MegaMekUnitSelectorDialog(this, unitLoadingDialog);
        randomArmyDialog = new RandomArmyDialog(this);
        new Thread(mekSelectorDialog, Messages.getString("ClientGUI.mekSelectorDialog")).start();
        frame.setVisible(true);
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public CommonMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    private void showAbout() {
        new CommonAboutDialog(frame).setVisible(true);
    }

    /**
     * Shows a confirmation dialog when attempting to load a game from the lobby.
     *
     * @return true if the user confirms, false otherwise
     */
    private boolean confirmLoadFromLobby() {
        int result = JOptionPane.showConfirmDialog(
              frame,
              Messages.getString("ClientGUI.LoadFromLobby.message"),
              Messages.getString("ClientGUI.LoadFromLobby.title"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Handles loading a game from the lobby. Shows file chooser first, then confirms disconnect, then loads directly.
     */
    private void loadGameFromLobby() {
        // Show file chooser first (before closing lobby)
        JFileChooser fileChooser = new JFileChooser(MMConstants.SAVEGAME_DIR);
        fileChooser.setDialogTitle(Messages.getString("MegaMek.SaveGameDialog.title"));
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(MMConstants.SAVE_FILE_EXT)
                      || f.getName().endsWith(MMConstants.SAVE_FILE_GZ_EXT)
                      || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Savegames";
            }
        });

        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();

        // Now confirm disconnect
        if (!confirmLoadFromLobby()) {
            return;
        }

        // Close lobby and load the selected file directly (skip HostDialog)
        controller.setPostUnlaunchAction(() ->
              controller.megaMekGUI.loadGameFile(selectedFile));
        setDisconnectQuietly(true);
        die();
    }

    /**
     * Called when the user selects the "Help->Contents" menu item.
     * <p>
     * This method can be called by subclasses.
     */
    private void showHelp() {
        // Do we need to create the "help" dialog?
        if (help == null) {
            help = new MMReadMeHelpDialog(frame);
        }
        // Show the help dialog.
        help.setVisible(true);
    }

    private void showSkinningHowTo() {
        try {
            // Get the correct help file.
            StringBuilder helpPath = new StringBuilder(CG_FILE_URL_START);
            helpPath.append(java.lang.System.getProperty(CG_FILEPATH_USER_DIR));
            if (!helpPath.toString().endsWith(File.separator)) {
                helpPath.append(File.separator);
            }
            helpPath.append(Messages.getString("ClientGUI.skinningHelpPath"));
            URL helpUrl = new URL(helpPath.toString());

            // Launch the help dialog.
            HelpDialog helpDialog = new HelpDialog(Messages.getString("ClientGUI.skinningHelpPath.title"),
                  helpUrl,
                  frame);
            helpDialog.setVisible(true);
        } catch (MalformedURLException ex) {
            logger.error(ex, "showSkinningHowTo");
            doAlertDialog(ex.getMessage(), Messages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    private void showSettings() {
        // Do we need to create the "settings" dialog?
        if (commonSettingsDialog == null) {
            commonSettingsDialog = new CommonSettingsDialog(frame, this);
        }

        // Show the settings dialog.
        commonSettingsDialog.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Game Options" menu item.
     */
    private void showOptions() {
        getGameOptionsDialog().setEditable(client.getGame().getPhase().isLounge());
        // Display the game options' dialog.
        getGameOptionsDialog().update(client.getGame().getOptions());
        getGameOptionsDialog().setVisible(true);
    }

    public void customizePlayer() {
        PlayerSettingsDialog psd = new PlayerSettingsDialog(this, client, (BoardView) boardViews.get(0));
        psd.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Player List" menu item.
     */
    public void showPlayerList() {
        if (getPlayerListDialog() == null) {
            setPlayerListDialog(new PlayerListDialog(frame, client, false));
        }
        getPlayerListDialog().setVisible(true);
        conditionalRequestFocus(true);
    }

    public void miniReportDisplayAddReportPages() {
        ignoreHotKeys = true;
        if (getMiniReportDisplay() != null) {
            getMiniReportDisplay().addReportPages(client.getGame().getPhase());
        }
        ignoreHotKeys = false;
    }

    public void reportDisplayResetDone() {
        if ((reportDisplay != null) && (!getClient().getLocalPlayer().isDone())) {
            reportDisplay.setDoneEnabled(true);
        }
    }

    public void reportDisplayResetRerollInitiative() {
        if ((reportDisplay != null) && (!getClient().getLocalPlayer().isDone()) && (getClient().getGame()
              .hasTacticalGenius(getClient().getLocalPlayer()))) {
            reportDisplay.resetRerollInitiativeEnabled();
        }
    }

    private boolean resetMiniMapZoom(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof MinimapPanel minimapPanel) {
                minimapPanel.resetZoom();
                return true;
            } else {
                if (resetMiniMapZoom((Container) comp)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void resetWindowPositions() {
        miniMaps.values().forEach(miniMap -> {
            miniMap.setBounds(0, 0, miniMap.getWidth(), miniMap.getHeight());
            resetMiniMapZoom(miniMap);
        });
        if (getUnitDisplayDialog() != null) {
            getUnitDisplayDialog().setBounds(0, 0, getUnitDisplay().getWidth(), getUnitDisplay().getHeight());
        }
        if (getForceDisplayDialog() != null) {
            getForceDisplayDialog().setBounds(0,
                  0,
                  getForceDisplayPanel().getWidth(),
                  getForceDisplayPanel().getHeight());
        }
        if (getMiniReportDisplayDialog() != null) {
            getMiniReportDisplayDialog().setBounds(0,
                  0,
                  getMiniReportDisplayDialog().getWidth(),
                  getMiniReportDisplayDialog().getHeight());
        }
        if (getPlayerListDialog() != null) {
            getPlayerListDialog().setBounds(0, 0, getPlayerListDialog().getWidth(), getPlayerListDialog().getHeight());
        }
        if (gameOptionsDialog != null) {
            gameOptionsDialog.setBounds(0, 0, gameOptionsDialog.getWidth(), gameOptionsDialog.getHeight());
        }
        if (commonSettingsDialog != null) {
            commonSettingsDialog.setBounds(0, 0, commonSettingsDialog.getWidth(), commonSettingsDialog.getHeight());
        }
        if (getBotCommandsDialog() != null) {
            getBotCommandsDialog().setBounds(0,
                  0,
                  getBotCommandsDialog().getWidth(),
                  getBotCommandsDialog().getHeight());
        }
    }

    /**
     * Implement the <code>ActionListener</code> interface.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        switch (event.getActionCommand()) {
            case VIEW_RESET_WINDOW_POSITIONS:
                resetWindowPositions();
                break;
            case FILE_GAME_SAVE:
                saveGame();
                break;
            case FILE_GAME_QUICK_SAVE:
                quickSaveGame();
                break;
            case FILE_GAME_SAVE_SERVER:
                ignoreHotKeys = true;
                String filename = (String) JOptionPane.showInputDialog(frame,
                      Messages.getString("ClientGUI.FileSaveServerDialog.message"),
                      Messages.getString("ClientGUI.FileSaveServerDialog.title"),
                      JOptionPane.QUESTION_MESSAGE,
                      null,
                      null,
                      MMConstants.DEFAULT_SAVEGAME_NAME);
                if (filename != null) {
                    client.sendChat(CG_CHAT_COMMAND_SAVE + " " + filename);
                }
                ignoreHotKeys = false;
                break;
            case FILE_GAME_LOAD:
                loadGameFromLobby();
                break;
            case FILE_GAME_QUICK_LOAD:
                if (confirmLoadFromLobby()) {
                    controller.setPostUnlaunchAction(() -> controller.megaMekGUI.quickLoadGame());
                    setDisconnectQuietly(true);
                    die();
                }
                break;
            case HELP_ABOUT:
                showAbout();
                break;
            case HELP_SKINNING:
                showSkinningHowTo();
                break;
            case HELP_CONTENTS:
                showHelp();
                break;
            case FILE_UNITS_SAVE:
                ignoreHotKeys = true;
                doSaveUnit();
                ignoreHotKeys = false;
                break;
            case FILE_UNITS_PASTE:
                if (curPanel instanceof ChatLounge) {
                    ignoreHotKeys = true;
                    ((ChatLounge) curPanel).importClipboard();
                    ignoreHotKeys = false;
                }
                break;
            case FILE_UNITS_COPY:
                if (curPanel instanceof ChatLounge) {
                    ignoreHotKeys = true;
                    ((ChatLounge) curPanel).copyToClipboard();
                    ignoreHotKeys = false;
                }
                break;
            case FILE_UNITS_OPEN:
                ignoreHotKeys = true;
                loadListFile();
                ignoreHotKeys = false;
                break;
            case FILE_UNITS_REINFORCE:
                ignoreHotKeys = true;
                PlayerListDialog pld = new PlayerListDialog(frame, client, true);
                pld.setVisible(true);
                loadListFile(pld.getSelected(), true);
                ignoreHotKeys = false;
                break;
            case FILE_UNITS_REINFORCE_RAT:
                ignoreHotKeys = true;
                if (client.getLocalPlayer().getTeam() == Player.TEAM_UNASSIGNED) {
                    doAlertDialog(Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceMessage"),
                          Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceTitle"),
                          JOptionPane.ERROR_MESSAGE);
                    return;
                }
                getRandomArmyDialog().setVisible(true);
                ignoreHotKeys = false;
                break;
            case FILE_REFRESH_CACHE:
                MekSummaryCache.refreshUnitData(false);
                new Thread(mekSelectorDialog, Messages.getString("ClientGUI.mekSelectorDialog")).start();
                break;
            case VIEW_CLIENT_SETTINGS:
                showSettings();
                break;
            case VIEW_GAME_OPTIONS:
                showOptions();
                break;
            case VIEW_PLAYER_SETTINGS:
                customizePlayer();
                break;
            case VIEW_PLAYER_LIST:
                GUIP.togglePlayerListEnabled();
                break;
            case VIEW_ROUND_REPORT:
                GUIP.toggleRoundReportEnabled();
                break;
            case VIEW_UNIT_DISPLAY:
                GUIP.toggleUnitDisplay();
                break;
            case VIEW_FORCE_DISPLAY:
                GUIP.toggleForceDisplay();
                break;
            case VIEW_NOVA_NETWORKS:
                showNovaNetworkViewDialog();
                break;
            case VIEW_MINI_MAP:
                GUIP.toggleMinimapEnabled();
                break;
            case VIEW_BOT_COMMANDS:
                GUIP.toggleBotCommandsEnabled();
                break;
            case VIEW_TOGGLE_HEX_COORDS:
                GUIP.toggleCoords();
                break;
            case BOARD_SAVE:
                ignoreHotKeys = true;
                boardSave(client.getGame());
                ignoreHotKeys = false;
                break;
            case BOARD_SAVE_AS:
                ignoreHotKeys = true;
                boardSaveAs(client.getGame());
                ignoreHotKeys = false;
                break;
            case BOARD_SAVE_AS_IMAGE:
                ignoreHotKeys = true;
                boardSaveAsImage(true);
                ignoreHotKeys = false;
                break;
            case BOARD_SAVE_AS_IMAGE_UNITS:
                ignoreHotKeys = true;
                boardSaveAsImage(false);
                ignoreHotKeys = false;
                break;
            case FILE_GAME_EDIT_BOTS:
                editBots();
                break;
            case VIEW_ACCESSIBILITY_WINDOW:
                toggleAccessibilityWindow();
                break;
            case VIEW_LOS_SETTING:
                showLOSSettingDialog();
                break;
            case VIEW_ZOOM_IN:
                boardViews.get(0).zoomIn();
                break;
            case VIEW_ZOOM_OUT:
                boardViews.get(0).zoomOut();
                break;
            case VIEW_ZOOM_OVERVIEW_TOGGLE:
                boardViews.get(0).zoomOverviewToggle();
                break;
            case VIEW_TOGGLE_ISOMETRIC:
                GUIP.setIsometricEnabled(!GUIP.getIsometricEnabled());
                break;
            case VIEW_TOGGLE_FOV_HIGHLIGHT:
                GUIP.setFovHighlight(!GUIP.getFovHighlight());
                boardViews.get(0).refreshDisplayables();
                if (client.getGame().getPhase().isMovement()) {
                    ((BoardView) boardViews.get(0)).clearHexImageCache();
                }
                break;
            case VIEW_TOGGLE_FIELD_OF_FIRE:
                GUIP.setShowFieldOfFire(!GUIP.getShowFieldOfFire());
                boardViews.get(0).getPanel().repaint();
                break;
            case VIEW_TOGGLE_FLEE_ZONE:
                toggleFleeZone();
                break;
            case VIEW_TOGGLE_SENSOR_RANGE:
                GUIP.setShowSensorRange(!GUIP.getShowSensorRange());
                break;
            case VIEW_TOGGLE_FOV_DARKEN:
                GUIP.setFovDarken(!GUIP.getFovDarken());
                boardViews.get(0).refreshDisplayables();
                if (client.getGame().getPhase().isMovement()) {
                    ((BoardView) boardViews.get(0)).clearHexImageCache();
                }
                break;
            case VIEW_TOGGLE_FIRING_SOLUTIONS:
                GUIP.setShowFiringSolutions(!GUIP.getShowFiringSolutions());
                break;
            case VIEW_TOGGLE_CF_WARNING:
                CollapseWarning.handleActionPerformed();
                break;
            case VIEW_MOVE_ENV:
                GUIP.setMoveEnvelope(!GUIP.getMoveEnvelope());
                if (curPanel instanceof MovementDisplay movementDisplay) {
                    Entity entity = getUnitDisplay().getCurrentEntity();
                    movementDisplay.computeMovementEnvelope(entity);
                }
                break;
            case VIEW_MOVE_MOD_ENV:
                if (curPanel instanceof MovementDisplay movementDisplay) {
                    movementDisplay.computeModifierEnvelope();
                }
                break;
            case VIEW_CHANGE_THEME:
                ((BoardView) boardViews.get(0)).changeTheme();
                break;
            case FIRE_SAVE_WEAPON_ORDER:
                Entity ent = getUnitDisplay().getCurrentEntity();
                if (ent != null) {
                    WeaponOrderHandler.setWeaponOrder(ent.getChassis(),
                          ent.getModel(),
                          ent.getWeaponSortOrder(),
                          ent.getCustomWeaponOrder());
                    client.sendEntityWeaponOrderUpdate(ent);
                }
                break;
        }
    }

    /**
     * Save all the current in use Entities each grouped by player name
     * <p>
     * and a file for salvage
     */
    public void doSaveUnit() {
        for (Player p : getClient().getGame().getPlayersList()) {
            ArrayList<Entity> l = getClient().getGame().getPlayerEntities(p, false);
            // Be sure to include all units that have retreated.
            for (Enumeration<Entity> iter2 = getClient().getGame().getRetreatedEntities(); iter2.hasMoreElements(); ) {
                Entity e = iter2.nextElement();
                if (e.getOwnerId() == p.getId()) {
                    l.add(e);
                }
            }
            saveListFile(l, p.getName());
        }

        // save all destroyed units in a separate "salvage MUL"
        ArrayList<Entity> destroyed = new ArrayList<>();
        Enumeration<Entity> graveyard = getClient().getGame().getGraveyardEntities();
        while (graveyard.hasMoreElements()) {
            Entity entity = graveyard.nextElement();
            if (entity.isSalvage()) {
                destroyed.add(entity);
            }
        }

        if (!destroyed.isEmpty()) {
            String sLogDir = CP.getLogDirectory();
            File logDir = new File(sLogDir);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            String fileName = CG_FILENAME_SALVAGE + CG_FILE_EXTENSION_MUL;
            if (CP.stampFilenames()) {
                fileName = StringUtil.addDateTimeStamp(fileName);
            }
            File unitFile = new File(sLogDir + File.separator + fileName);
            try {
                // Save the destroyed entities to the file.
                EntityListFile.saveTo(unitFile, destroyed);
            } catch (Exception ex) {
                logger.error(ex, "doSaveUnit");
                doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
            }
        }
    }

    /**
     * Saves the current settings to the cfg file.
     */
    void saveSettings() {
        super.saveSettings();

        // Minimap Dialog
        if (getMiniMapDialog() != null) {
            getMiniMapDialog().saveSettings();
        }

        // Unit Display Dialog
        if (getUnitDisplayDialog() != null) {
            getUnitDisplayDialog().saveSettings();
            saveSplitPaneLocations();
        }

        // Force Display Dialog
        if (getForceDisplayDialog() != null) {
            getForceDisplayDialog().saveSettings();
        }

        // Mini Report Dialog
        if (getMiniReportDisplayDialog() != null) {
            getMiniReportDisplayDialog().saveSettings();
        }

        // Player List Dialog
        if (getPlayerListDialog() != null) {
            getPlayerListDialog().saveSettings();
        }

        // Ruler display
        if ((ruler != null) && (ruler.getSize().width != 0) && (ruler.getSize().height != 0)) {
            GUIP.setRulerPosX(ruler.getLocation().x);
            GUIP.setRulerPosY(ruler.getLocation().y);
            GUIP.setRulerSizeWidth(ruler.getSize().width);
            GUIP.setRulerSizeHeight(ruler.getSize().height);
        }

        // BotCommands Dialog
        if (getBotCommandsDialog() != null) {
            getBotCommandsDialog().saveSettings();
        }
    }

    @Override
    public void die() {
        if (client.isAwaitingSave()) {
            SwingUtilities.invokeLater(this::die);
            return;
        }

        // Tell all the displays to remove themselves as listeners.
        boolean reportHandled = false;
        boardViews().forEach(IBoardView::dispose);

        for (String s : phaseComponents.keySet()) {
            JComponent component = phaseComponents.get(s);
            if (component instanceof ReportDisplay) {
                if (reportHandled) {
                    continue;
                }
                reportHandled = true;
            }
            if (component instanceof Distractable) {
                ((Distractable) component).removeAllListeners();
            }
        }
        phaseComponents.clear();

        client.die();

        TimerSingleton.getInstance().killTimer();

        if (controller != null) {
            controller.removeAllActions();
            controller.clientGUI = null;
        }

        if (menuBar != null) {
            menuBar.die();
            menuBar = null;
        }

        if (curPanel instanceof StatusBarPhaseDisplay) {
            ((StatusBarPhaseDisplay) curPanel).stopTimer();
        }

        GUIP.removePreferenceChangeListener(this);
        super.die();
    }

    public GameOptionsDialog getGameOptionsDialog() {
        if (gameOptionsDialog == null) {
            gameOptionsDialog = new GameOptionsDialog(this);
        }
        return gameOptionsDialog;
    }

    public MegaMekUnitSelectorDialog getMekSelectorDialog() {
        return mekSelectorDialog;
    }

    public void switchPanel(GamePhase phase) {
        // Clear the old panel's listeners.
        if (curPanel instanceof BoardViewListener) {
            boardViews().forEach(b -> b.removeBoardViewListener((BoardViewListener) curPanel));
        }

        if (curPanel instanceof ActionListener) {
            menuBar.removeActionListener((ActionListener) curPanel);
        }

        if (curPanel instanceof Distractable) {
            ((Distractable) curPanel).setIgnoringEvents(true);
        }

        // Get the new panel.
        String name = String.valueOf(phase);
        curPanel = phaseComponents.get(name);
        if (curPanel == null) {
            curPanel = initializePanel(phase);
        }

        // Handle phase-specific items.
        switch (phase) {
            case LOUNGE:
                // reset old report tabs and images, if any
                ChatLounge cl = (ChatLounge) phaseComponents.get(String.valueOf(GamePhase.LOUNGE));
                cb.setDoneButton(cl.getButDone());
                cl.setBottom(cb.getComponent());
                boardViews().forEach(bv -> ((BoardView) bv).getTilesetManager().reset());
                break;
            case POINTBLANK_SHOT:
            case SET_ARTILLERY_AUTO_HIT_HEXES:
            case DEPLOY_MINEFIELDS:
            case DEPLOYMENT:
            case TARGETING:
            case PREMOVEMENT:
            case MOVEMENT:
            case OFFBOARD:
            case PRE_FIRING:
            case FIRING:
            case PHYSICAL:
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
            default:
                break;
        }

        maybeShowMinimap();
        maybeShowBotCommands();
        maybeShowUnitDisplay();
        maybeShowForceDisplay();
        maybeShowMiniReport();
        maybeShowPlayerList();

        cardsMain.show(panMain, mainNames.get(name));
        String secondaryToShow = secondaryNames.get(name);
        // only show the secondary component if there is one to show
        if (secondaryToShow != null) {
            panSecondary.setVisible(true);
            cardsSecondary.show(panSecondary, secondaryNames.get(name));
        } else {
            // otherwise, hide the panel
            panSecondary.setVisible(false);
        }

        // Set the new panel's listeners
        if (curPanel instanceof BoardViewListener listener) {
            boardViews().forEach(b -> b.addBoardViewListener(listener));
        }

        if (curPanel instanceof ActionListener) {
            menuBar.addActionListener((ActionListener) curPanel);
        }

        if (curPanel instanceof Distractable) {
            ((Distractable) curPanel).setIgnoringEvents(false);
        }

        // Make the new panel the focus, if the Client option says so
        if (GUIP.getFocus() && !(client instanceof BotClient)) {
            curPanel.requestFocus();
        }
    }

    public void updateButtonPanel(GamePhase phase) {
        if ((currPhaseDisplay != null) && client.getGame().getPhase().equals(phase)) {
            currPhaseDisplay.setupButtonPanel();
        }
    }

    private JComponent initializePanel(GamePhase phase) {
        // Create the components for this phase.
        String name = String.valueOf(phase);
        JComponent component;
        String secondary = null;
        String main;
        switch (phase) {
            case LOUNGE:
                component = new ChatLounge(this);
                chatlounge = (ChatLounge) component;
                main = CG_CHAT_LOUNGE;
                component.setName(main);
                panMain.add(component, main);
                break;
            case STARTING_SCENARIO:
                component = new StartingScenarioPanel();
                main = CG_STARTING_SCENARIO;
                component.setName(main);
                panMain.add(component, main);
                break;
            case EXCHANGE:
                chatlounge.killPreviewBV();
                component = new ReceivingGameDataPanel();
                main = CG_EXCHANGE;
                component.setName(main);
                panMain.add(component, main);
                break;
            case SET_ARTILLERY_AUTO_HIT_HEXES:
                component = new SelectArtyAutoHitHexDisplay(this);
                main = CG_BOARD_VIEW;
                secondary = CG_SELECT_ARTY_AUTO_HIT_HEX_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOY_MINEFIELDS:
                component = new DeployMinefieldDisplay(this);
                main = CG_BOARD_VIEW;
                secondary = CG_DEPLOY_MINEFIELD_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOYMENT:
                component = new DeploymentDisplay(this);
                main = CG_BOARD_VIEW;
                secondary = CG_DEPLOYMENT_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case TARGETING:
                component = new TargetingPhaseDisplay(this, false);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = CG_BOARD_VIEW;
                secondary = CG_TARGETING_PHASE_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                offBoardOverlay.setTargetingPhaseDisplay((TargetingPhaseDisplay) component);
                break;
            case PREMOVEMENT:
                component = new PrephaseDisplay(this, GamePhase.PREMOVEMENT);
                ((PrephaseDisplay) component).initializeListeners();
                main = CG_BOARD_VIEW;
                secondary = CG_PREMOVEMENT_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case MOVEMENT:
                component = new MovementDisplay(this);
                main = CG_BOARD_VIEW;
                secondary = CG_MOVEMENT_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case OFFBOARD:
                component = new TargetingPhaseDisplay(this, true);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = CG_BOARD_VIEW;
                secondary = CG_OFFBOARD_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PRE_FIRING:
                component = new PrephaseDisplay(this, GamePhase.PRE_FIRING);
                ((PrephaseDisplay) component).initializeListeners();
                main = CG_BOARD_VIEW;
                secondary = CG_PRE_FIRING;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case FIRING:
                component = new FiringDisplay(this);
                main = CG_BOARD_VIEW;
                secondary = CG_FIRING_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case POINTBLANK_SHOT:
                component = new PointblankShotDisplay(this);
                main = CG_BOARD_VIEW;
                secondary = CG_POINTBLANK_SHOT_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PHYSICAL:
                component = new PhysicalDisplay(this);
                main = CG_BOARD_VIEW;
                secondary = CG_PHYSICAL_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
                main = CG_BOARD_VIEW;
                secondary = CG_REPORT_DISPLAY;
                if (reportDisplay == null) {
                    reportDisplay = new ReportDisplay(this);
                    reportDisplay.setName(secondary);
                }
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = reportDisplay;
                component = reportDisplay;
                if (!secondaryNames.containsValue(secondary)) {
                    panSecondary.add(reportDisplay, secondary);
                }
                break;
            default:
                component = new WaitingForServerPanel();
                main = CG_DEFAULT;
                secondary = main;
                component.setName(main);
                panMain.add(main, component);
                break;
        }
        phaseComponents.put(name, component);
        mainNames.put(name, main);
        if (secondary != null) {
            secondaryNames.put(name, secondary);
        }

        return component;
    }

    protected void showBoardPopup(BoardViewEvent event) {
        if (fillPopup(event)) {
            event.getBoardView().showPopup(popup, event.getCoords());
        }
    }

    /**
     * Switches the Minimap and the UnitDisplay an and off together. If the UnitDisplay is active, both will be hidden,
     * else both will be shown.
     */
    public void toggleMMUDDisplays() {
        GUIP.toggleUnitDisplay();
        GUIP.setMinimapEnabled(GUIP.getUnitDisplayEnabled());
    }

    /**
     * Requests focus for the board view if the board was previously focused and the specified visibility condition is
     * met.
     *
     * @param visible a boolean indicating whether the board view should be visible. If true and the board was
     *                previously focused, focus will be requested.
     */
    private void conditionalRequestFocus(boolean visible) {
        if (wasBoardFocused && visible) {
            requestFocus();
        }
    }

    private void requestFocus() {
        frame.requestFocusInWindow();
        boardViewsContainer.getPanel().requestFocusInWindow();
    }

    /**
     * Toggles the accessibility window
     */
    private void toggleAccessibilityWindow() {
        aw.setVisible(!aw.isVisible());
        if (aw.isVisible()) {
            frame.requestFocus();
        }
    }

    private void maybeShowBotCommands() {
        GamePhase phase = getClient().getGame().getPhase();
        if (phase.isReport()) {
            int action = GUIP.getBotCommandsAutoDisplayReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setBotCommandsEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setBotCommandsEnabled(false);
            }
        } else if (phase.isOnMap()) {
            int action = GUIP.getBotCommandsAutoDisplayNonReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setBotCommandsEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setBotCommandsEnabled(false);
            }
        } else {
            GUIP.setBotCommandsEnabled(false);
        }
    }

    /** Shows or hides the minimap based on the current menu setting. */
    private void maybeShowMinimap() {
        GamePhase phase = getClient().getGame().getPhase();

        if (phase.isReport()) {
            int action = GUIP.getMinimapAutoDisplayReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setMinimapEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setMinimapEnabled(false);
            }
        } else if (phase.isOnMap()) {
            int action = GUIP.getMinimapAutoDisplayNonReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setMinimapEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setMinimapEnabled(false);
            }
        } else {
            GUIP.setMinimapEnabled(false);
        }
    }

    private void maybeShowMiniReport() {
        GamePhase phase = getClient().getGame().getPhase();

        if (phase.isReport()) {
            int action = GUIP.getMiniReportAutoDisplayReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setMiniReportEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setMiniReportEnabled(false);
            }
        } else if (phase.isOnMap()) {
            int action = GUIP.getMiniReportAutoDisplayNonReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setMiniReportEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setMiniReportEnabled(false);
            }
        } else {
            GUIP.setMiniReportEnabled(false);
        }
    }

    private void maybeShowPlayerList() {
        GamePhase phase = getClient().getGame().getPhase();

        if (phase.isReport()) {
            int action = GUIP.getPlayerListAutoDisplayReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setPlayerListEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setPlayerListEnabled(false);
            }
        } else if (phase.isOnMap()) {
            int action = GUIP.getPlayerListAutoDisplayNonReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setPlayerListEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setPlayerListEnabled(false);
            }
        } else {
            GUIP.setPlayerListEnabled(false);
        }
    }

    /**
     * Shows or hides the Minimap based on the given visible. This works independently of the current menu setting, so
     * it should be used only when the Minimap is to be shown or hidden without regard for the user setting, e.g. hiding
     * it in the lobby or a report phase. Does not change the menu setting.
     */
    void setMapVisible(boolean visible) {
        miniMaps.values().forEach(miniMap -> miniMap.setVisible(visible));
        if (getMiniMapDialog() != null) {
            getMiniMapDialog().setVisible(visible);
            conditionalRequestFocus(visible);
        }
    }

    void setMiniReportVisible(boolean visible) {
        if (getMiniReportDisplayDialog() != null) {
            setMiniReportLocation(visible);
            conditionalRequestFocus(visible);
        }
    }

    void setPlayerListVisible(boolean visible) {
        conditionalRequestFocus(visible);
        if (visible) {
            showPlayerList();
        } else if (getPlayerListDialog() != null) {
            getPlayerListDialog().setVisible(false);
        }
    }

    /** Shows or hides the Unit Display based on the current menu setting. */
    public void maybeShowUnitDisplay() {
        GamePhase phase = getClient().getGame().getPhase();

        if (phase.isReport()) {
            int action = GUIP.getUnitDisplayAutoDisplayReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setUnitDisplayEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setUnitDisplayEnabled(false);
            }
        } else if (phase.isOnMap()) {
            int action = GUIP.getUnitDisplayAutoDisplayNonReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setUnitDisplayEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setUnitDisplayEnabled(false);
            }
        } else {
            GUIP.setUnitDisplayEnabled(false);
        }
    }

    /** Shows or hides the Unit Display based on the current menu setting. */
    public void maybeShowForceDisplay() {
        GamePhase phase = getClient().getGame().getPhase();

        if (phase.isReport()) {
            int action = GUIP.getForceDisplayAutoDisplayReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setForceDisplayEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setForceDisplayEnabled(false);
            }
        } else if (phase.isOnMap()) {
            int action = GUIP.getForceDisplayAutoDisplayNonReportPhase();
            if (action == GUIPreferences.SHOW) {
                GUIP.setForceDisplayEnabled(true);
            } else if (action == GUIPreferences.HIDE) {
                GUIP.setForceDisplayEnabled(false);
            }
        } else {
            GUIP.setForceDisplayEnabled(false);
        }
    }

    /**
     * Shows or hides the Unit Display based on the given visible. This works independently of the current menu setting,
     * so it should be used only when the Unit Display is to be shown or hidden without regard for the user setting,
     * e.g. hiding it in the lobby or a report phase. Does not change the menu setting.
     */
    public void setUnitDisplayVisible(boolean visible) {
        // If no unit displayed, select a unit so display can be safely shown
        // This can happen when using mouse button 4
        if (visible && (getUnitDisplay().getCurrentEntity() == null) && (getClient() != null) && (getClient().getGame()
              != null)) {
            List<Entity> es = getClient().getGame().getEntitiesVector();
            if (!es.isEmpty()) {
                getUnitDisplay().displayEntity(es.get(0));
            }
        }

        if (getUnitDisplayDialog() != null) {
            setUnitDisplayLocation(visible);
        }
    }

    void setBotCommandsDialogVisible(boolean visible) {
        if (getBotCommandsDialog() != null) {
            getBotCommandsDialog().setVisible(visible);
            conditionalRequestFocus(visible);
        }
    }

    private void saveSplitPaneLocations() {
        if ((panA1.isVisible()) && (panA2.isVisible())) {
            GUIP.setSplitPaneALocation(splitPaneA.getDividerLocation());
        }
    }

    private void setDividerLocations() {
        splitPaneA.setDividerLocation(GUIP.getSplitPaneADividerLocation());
    }

    public void setForceDisplayVisible(boolean visible) {
        if (getForceDisplayDialog() != null) {
            getForceDisplayDialog().setVisible(visible);
            conditionalRequestFocus(visible);
        }
    }

    private void hideEmptyPanel(JPanel p, JSplitPane sp, Double d) {
        boolean b = false;

        for (Component comp : p.getComponents()) {
            if (comp.isVisible()) {
                b = true;
                break;
            }
        }

        if (!b) {
            p.setVisible(false);
            sp.setDividerLocation(d);
        } else {
            p.setVisible(true);
        }
    }

    private void revalidatePanels() {
        getUnitDisplay().setMinimumSize(new Dimension(0, (int) (panTop.getHeight() * 0.7)));
        getUnitDisplay().setPreferredSize(new Dimension(0, (int) (panTop.getHeight() * 0.7)));
        getMiniReportDisplay().setMinimumSize(new Dimension(0, (int) (panTop.getHeight() * 0.3)));
        getMiniReportDisplay().setPreferredSize(new Dimension(0, (int) (panTop.getHeight() * 0.3)));

        getUnitDisplayDialog().revalidate();
        getUnitDisplayDialog().repaint();
        panA1.revalidate();
        panA1.repaint();
        panA2.revalidate();
        panA2.repaint();
    }

    private void setDockAxis() {
        if (GUIP.getDockMultipleOnYAxis()) {
            panA1.setLayout(new BoxLayout(panA1, BoxLayout.Y_AXIS));
            panA2.setLayout(new BoxLayout(panA2, BoxLayout.Y_AXIS));
        } else {
            panA1.setLayout(new BoxLayout(panA1, BoxLayout.X_AXIS));
            panA2.setLayout(new BoxLayout(panA2, BoxLayout.X_AXIS));
        }
    }

    public void setUnitDisplayLocation(boolean visible) {
        saveSplitPaneLocations();
        setDockAxis();

        if (GUIP.getDockOnLeft()) {
            switch (GUIP.getUnitDisplayLocation()) {
                case 0:
                    panA2.add(boardViewsContainer.getPanel());
                    panA2.setVisible(true);
                    getUnitDisplayDialog().add(getUnitDisplay(), BorderLayout.CENTER);
                    getUnitDisplayDialog().setVisible(visible);
                    getUnitDisplay().setVisible(visible);
                    getUnitDisplay().setTitleVisible(false);
                    hideEmptyPanel(panA1, splitPaneA, 0.0);
                    conditionalRequestFocus(visible);
                    break;
                case 1:
                    panA2.add(boardViewsContainer.getPanel());
                    panA2.setVisible(true);
                    panA1.add(getUnitDisplay());
                    getUnitDisplayDialog().setVisible(false);
                    getUnitDisplay().setVisible(visible);
                    getUnitDisplay().setTitleVisible(true);
                    hideEmptyPanel(panA1, splitPaneA, 0.0);
                    conditionalRequestFocus(visible);
                    break;
            }
        } else {
            switch (GUIP.getUnitDisplayLocation()) {
                case 0:
                    panA1.add(boardViewsContainer.getPanel());
                    panA1.setVisible(true);
                    getUnitDisplayDialog().add(getUnitDisplay(), BorderLayout.CENTER);
                    getUnitDisplayDialog().setVisible(visible);
                    getUnitDisplay().setVisible(visible);
                    getUnitDisplay().setTitleVisible(false);
                    hideEmptyPanel(panA2, splitPaneA, 1.0);
                    conditionalRequestFocus(visible);
                    break;
                case 1:
                    panA1.add(boardViewsContainer.getPanel());
                    panA1.setVisible(true);
                    panA2.add(getUnitDisplay());
                    getUnitDisplayDialog().setVisible(false);
                    getUnitDisplay().setVisible(visible);
                    getUnitDisplay().setTitleVisible(true);
                    hideEmptyPanel(panA2, splitPaneA, 1.0);
                    conditionalRequestFocus(visible);
                    break;
            }
        }

        setDividerLocations();
        revalidatePanels();
    }

    public void setMiniReportLocation(boolean visible) {
        saveSplitPaneLocations();
        setDockAxis();

        if (GUIP.getDockOnLeft()) {
            switch (GUIP.getMiniReportLocation()) {
                case 0:
                    panA2.add(boardViewsContainer.getPanel());
                    panA2.setVisible(true);
                    getMiniReportDisplayDialog().add(getMiniReportDisplay(), BorderLayout.CENTER);
                    getMiniReportDisplayDialog().setVisible(visible);
                    getMiniReportDisplay().setVisible(visible);
                    hideEmptyPanel(panA1, splitPaneA, 0.0);
                    conditionalRequestFocus(visible);
                    break;
                case 1:
                    panA2.add(boardViewsContainer.getPanel());
                    panA2.setVisible(true);
                    panA1.add(getMiniReportDisplay());
                    getMiniReportDisplayDialog().setVisible(false);
                    getMiniReportDisplay().setVisible(visible);
                    hideEmptyPanel(panA1, splitPaneA, 0.0);
                    conditionalRequestFocus(visible);
                    break;
            }
        } else {
            switch (GUIP.getMiniReportLocation()) {
                case 0:
                    panA1.add(boardViewsContainer.getPanel());
                    panA1.setVisible(true);
                    getMiniReportDisplayDialog().add(getMiniReportDisplay(), BorderLayout.CENTER);
                    getMiniReportDisplayDialog().setVisible(visible);
                    getMiniReportDisplay().setVisible(visible);
                    hideEmptyPanel(panA2, splitPaneA, 1.0);
                    conditionalRequestFocus(visible);
                    break;
                case 1:
                    panA1.add(boardViewsContainer.getPanel());
                    panA1.setVisible(true);
                    panA2.add(getMiniReportDisplay());
                    getMiniReportDisplayDialog().setVisible(false);
                    getMiniReportDisplay().setVisible(visible);
                    hideEmptyPanel(panA2, splitPaneA, 1.0);
                    conditionalRequestFocus(visible);
                    break;
            }
        }

        revalidatePanels();
        setDividerLocations();
        conditionalRequestFocus(true);
    }

    private boolean fillPopup(BoardViewEvent event) {
        popup = new MapMenu(event.getCoords(), event.getBoardView().getBoardId(), curPanel, this);
        return popup.getHasMenu();
    }

    /**
     * Pops up a dialog box giving the player a series of choices that are not mutually exclusive.
     *
     * @param title    the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or "No" answer. The question will be split
     *                 across multiple line on the '\n' characters.
     * @param choices  the array of <code>String</code> choices that the player can select from.
     *
     * @return The array of the <code>int</code> indexes from the input array that match the selected choices. If no
     *       choices were available, if the player did not select a choice, or if the player canceled the choice, a
     *       <code>null</code> value is returned.
     */
    public @Nullable int[] doChoiceDialog(String title, String question, String... choices) {
        ChoiceDialog choice = new ChoiceDialog(frame, title, question, choices);
        choice.setVisible(true);
        return choice.getChoices();
    }

    /**
     * Pops up a dialog box showing an alert
     */
    public void doAlertDialog(String title, String message) {
        doAlertDialog(title, message, JOptionPane.ERROR_MESSAGE);
    }


    /**
     * Brings up a dialog that displays a message using a default icon determined by the <code>messageType</code>
     * parameter.
     *
     * @param message     the <code>Object</code> to display
     * @param title       the title string for the dialog
     * @param messageType the type of message to be displayed:
     *                    <code>ERROR_MESSAGE</code>,
     *                    <code>INFORMATION_MESSAGE</code>,
     *                    <code>WARNING_MESSAGE</code>,
     *                    <code>QUESTION_MESSAGE</code>,
     *                    or <code>PLAIN_MESSAGE</code>
     *
     * @throws HeadlessException if
     *                           <code>GraphicsEnvironment.isHeadless</code> returns
     *                           <code>true</code>
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public void doAlertDialog(String title, String message, int messageType) {
        JTextPane textArea = new JTextPane();
        Report.setupStylesheet(textArea);
        BASE64ToolKit toolKit = new BASE64ToolKit();
        textArea.setEditorKit(toolKit);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textArea.setText("<pre>" + message + "</pre>");
        scrollPane.setPreferredSize(new Dimension((int) (clientGuiPanel.getSize().getWidth() / 1.5),
              (int) (clientGuiPanel.getSize().getHeight() / 1.5)));
        JOptionPane.showMessageDialog(frame, scrollPane, title, messageType);
    }

    /**
     * Pops up a dialog box giving the player information on a subject. It also includes a checkbox if they don't want
     * to be bothered again by it.
     *
     * @param title           the <code>String</code> title of the dialog box.
     * @param message         the <code>String</code> message to display.
     * @param includeCheckBox if <code>true</code>, a checkbox will be included in the dialog box
     *
     * @return {@link InformDialog} containing the player's responses. The dialog will already have been shown
     */
    public InformDialog doInformBotherDialog(String title, String message, boolean includeCheckBox) {
        InformDialog informDialog = new InformDialog(frame, title, message, includeCheckBox);
        informDialog.setVisible(true);
        informDialog.dispose();
        return informDialog;
    }

    /**
     * Pops up a dialog box asking a yes/no question
     *
     * @param title    the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or "No" answer. The question will be split
     *                 across multiple line on the '\n' characters.
     *
     * @return <code>true</code> if yes
     */
    public boolean doYesNoDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question);
        confirm.setVisible(true);
        return confirm.getAnswer();
    }

    /**
     * Pops up a dialog box asking a yes/no question
     * <p>
     * The player will be given a chance to not show the dialog again.
     *
     * @param title    the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or "No" answer. The question will be split
     *                 across multiple lines on the '\n' characters.
     *
     * @return the <code>ConfirmDialog</code> containing the player's responses. The dialog will already have been shown
     *       to the player, and is only being returned so the calling function can see the answer to the question and
     *       the state of the "Show again?" question.
     */
    public ConfirmDialog doYesNoBotherDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question, true);
        confirm.setVisible(true);
        confirm.dispose();
        return confirm;
    }

    /**
     * Pops up a dialog box asking a yes/no question
     * <p>
     * The player will be given a chance to not show the dialog again.
     *
     * @param title    the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or "No" answer. The question will be split
     *                 across multiple line on the '\n' characters.c tr23ty
     *
     * @return the <code>ConfirmDialog</code> containing the player's responses. The dialog will already have been shown
     *       to the player, and is only being returned so the calling function can see the answer to the question and
     *       the state of the "Show again?" question.
     */
    public ConfirmDialog doWarnNoBotherDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question, true);
        confirm.setVisible(true);
        confirm.dispose();
        return confirm;
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The {@link Entity}s in the file will replace any
     * that the player has already selected. As such, this method should only be called in the chat lounge. The file can
     * record damage sustained, non-standard munitions selected, and ammunition expended in a prior engagement.
     */
    protected void loadListFile() {
        loadListFile(client.getLocalPlayer());
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The {@link Entity}s in the file will replace any
     * that the player has already selected. As such, this method should only be called in the chat lounge. The file can
     * record damage sustained, non-standard munitions selected, and ammunition expended in a prior engagement.
     *
     * @param player The player to add the units to
     */
    public void loadListFile(Player player) {
        loadListFile(player, false);
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat lounge. The file can record damage
     * sustained, non-standard munitions selected, and ammunition expended in a prior engagement.
     *
     * @param player The player to add the units to
     */
    protected void loadListFile(Player player, boolean reinforce) {
        if (player != null) {
            boolean addedUnits = false;

            if (reinforce && (player.getTeam() == Player.TEAM_UNASSIGNED)) {
                doAlertDialog(Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceMessage"),
                      Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceTitle"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Build the "load unit" dialog, if necessary.
            if (dlgLoadList == null) {
                dlgLoadList = new JFileChooser(".");
                dlgLoadList.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
                dlgLoadList.setDialogTitle(Messages.getString("ClientGUI.openUnitListFileDialog.title"));
                dlgLoadList.setFileFilter(new FileNameExtensionFilter("MUL files", "mul", "mmu"));
            }
            // Default to the player's name.
            dlgLoadList.setSelectedFile(new File(player.getName() + CG_FILE_EXTENSION_MUL));

            int returnVal = dlgLoadList.showOpenDialog(frame);
            if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgLoadList.getSelectedFile() == null)) {
                return;
            }

            // Did the player select a file?
            File unitFile = dlgLoadList.getSelectedFile();

            try {
                // Read the units from the file.
                final Vector<Entity> loadedUnits = new MULParser(unitFile,
                      (GameOptions) getClient().getGame().getOptions()).getEntities();

                // in the Lounge, set default deployment to "Before Game Start", round 0
                // but in a game in-progress, deploy at the start of next round
                final int deployRound = client.getGame().getRoundCount() + ((client.getGame().getPhase()
                      == GamePhase.LOUNGE) ? 0 : 1);

                // Add the units from the file.
                for (Entity entity : loadedUnits) {
                    entity.setOwner(player);
                    if (reinforce) {
                        entity.setDeployRound(deployRound);
                        entity.setGame(client.getGame());
                        // Set these to true, otherwise units reinforced in
                        // the movement turn are considered selectable
                        entity.setDone(true);
                        entity.setUnloaded(true);
                        if (entity instanceof IBomber && (client.getGame().getPhase() != GamePhase.LOUNGE)) {
                            // Only apply bombs when we're going straight into the game; doing this in the
                            // lounge
                            // breaks the bombs completely.
                            ((IBomber) entity).applyBombs();
                        }
                    }
                }

                if (!loadedUnits.isEmpty()) {
                    client.sendAddEntity(loadedUnits);
                    String msg = client.getLocalPlayer()
                          + " loaded MUL file for player: "
                          + player.getName()
                          + " ["
                          + loadedUnits.size()
                          + " units]";
                    client.sendServerChat(Player.PLAYER_NONE, msg);
                    addedUnits = true;
                }
            } catch (Exception ex) {
                logger.error(ex, "loadListFile");
                doAlertDialog(Messages.getString("ClientGUI.errorLoadingFile"), ex.getMessage());
            }

            // If we've added reinforcements, then we need to set the round deployment up
            // again.
            if (addedUnits && reinforce) {
                client.getGame().setupDeployment();
                client.sendResetRoundDeployment();
            }
        } else {
            doAlertDialog(Messages.getString("ClientGUI.errorLoadingFile"),
                  Messages.getString("ClientGUI.errorSelectingPlayer"));
        }
    }

    @Override
    public boolean saveGame() {
        ignoreHotKeys = true;
        JFileChooser fc = new JFileChooser(MMConstants.SAVEGAME_DIR);
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGUI.FileSaveDialog.title"));

        int returnVal = fc.showSaveDialog(frame);
        ignoreHotKeys = false;
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, know!
            return false;
        }
        String file = fc.getSelectedFile().getName();
        // stupid hack to allow for savegames in folders with spaces in
        // the name
        String path = fc.getSelectedFile().getParentFile().getPath();
        path = path.replace(" ", "|");
        client.sendChat(CG_CHAT_COMMAND_LOCAL_SAVE + " " + file + " " + path);
        client.setAwaitingSave(true);
        return true;
    }

    /** Developer Utility: Save game to quicksave.sav.gz without any prompts. */
    private void quickSaveGame() {
        client.sendChat(CG_CHAT_COMMAND_LOCAL_SAVE
              + " "
              + MMConstants.QUICKSAVE_FILE
              + " "
              + MMConstants.QUICKSAVE_PATH);
    }

    /**
     * Allow the player to save a list of entities to a MegaMek Unit List file. A "Save As" dialog will be displayed
     * that allows the user to select the file's name and directory. The player can later load this file to quickly
     * select the units for a new game. The file will record damage sustained, non-standard munitions selected, and
     * ammunition expended during the course of the current engagement.
     *
     * @param unitList - the <code>Vector</code> of <code>Entity</code>s to be saved to a file. If this value is
     *                 <code>null</code> or empty, the "Save As" dialog will not be displayed.
     * @param filename The filename to save to
     */
    public void saveListFile(ArrayList<Entity> unitList, String filename) {
        // Handle empty lists.
        if ((unitList == null) || unitList.isEmpty()) {
            return;
        }

        // Build the "save unit" dialog, if necessary.
        if (dlgSaveList == null) {
            dlgSaveList = new JFileChooser(".");
            dlgSaveList.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
            dlgSaveList.setDialogTitle(Messages.getString("ClientGUI.saveUnitListFileDialog.title"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString(
                  "ClientGUI.descriptionMULFiles"), CG_FILEPATH_MUL);
            dlgSaveList.setFileFilter(filter);
        }
        // Default to the player's name.
        dlgSaveList.setSelectedFile(new File(filename + CG_FILE_EXTENSION_MUL));

        int returnVal = dlgSaveList.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgSaveList.getSelectedFile() == null)) {
            // I want a file, know!
            return;
        }

        // Did the player select a file?
        File unitFile = dlgSaveList.getSelectedFile();
        if (unitFile != null) {
            if (!(unitFile.getName().toLowerCase().endsWith(CG_FILE_EXTENSION_MUL) || unitFile.getName()
                  .toLowerCase()
                  .endsWith(CG_FILE_EXTENSION_XML))) {
                try {
                    unitFile = new File(unitFile.getCanonicalPath() + CG_FILE_EXTENSION_MUL);
                } catch (Exception ignored) {
                    // nothing needs to be done here
                    return;
                }
            }

            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo(unitFile, unitList);
            } catch (Exception ex) {
                logger.error(ex, "saveListFile");
                doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
            }
        }
    }

    private ProcessBuilder printToMegaMekLab(ArrayList<Entity> unitList, File mmlExecutable, boolean autodetected) {
        boolean jarFile;
        try (var ignored = new JarFile(mmlExecutable)) {
            jarFile = true;
        } catch (IOException ignored) {
            jarFile = false;
        }

        File unitFile;
        try {
            unitFile = File.createTempFile("MegaMekPrint", ".mul");
            EntityListFile.saveTo(unitFile, unitList);
            unitFile.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] command;

        if (!jarFile) {
            if (!mmlExecutable.canExecute()) {
                String dialogTitle = "Error printing unit list";
                if (autodetected) {
                    logger.errorDialog(dialogTitle,
                          "Could not auto-detect MegaMekLab! Please configure the "
                                + "path to the MegaMekLab executable in the settings.");
                } else {
                    logger.errorDialog(dialogTitle,
                          "{} does not appear to be an executable! You may need to set execute permission or "
                                + "configure the path to the MegaMekLab executable in the settings.",
                          mmlExecutable.getName());
                }
                return null;
            }

            if (mmlExecutable.getName().toLowerCase().contains("gradle")) {
                // If the executable is `gradlew`/`gradelw.bat`, assume it's the gradle wrapper
                // which comes in the MML git repo. Compile and run MML from source in order to print units.
                command = new String[] { mmlExecutable.getAbsolutePath(), "run",
                                         "--args=%s --no-startup".formatted(unitFile.getAbsolutePath()) };
            } else {
                // Start mml normally. "--no-startup" tells MML to exit after the user closes the
                // print dialog (by printing or cancelling)
                command = new String[] { mmlExecutable.getAbsolutePath(), unitFile.getAbsolutePath(), "--no-startup" };
            }
        } else {
            if (!mmlExecutable.exists()) {
                String dialogTitle = "Error printing unit list";

                if (autodetected) {
                    logger.errorDialog(dialogTitle,
                          "Could not auto-detect MegaMekLab! Please configure the path to the MegaMekLab executable "
                                + "in the settings.");
                } else {
                    logger.errorDialog(dialogTitle,
                          "{} does not appear to exist! Please configure the path to the MegaMekLab executable in "
                                + "the settings.",
                          mmlExecutable.getName());
                }
                return null;
            }

            // The executable is a jarFile, so let's execute it.
            var javaExecutable = ProcessHandle.current().info().command().orElse("java");
            command = new String[] { javaExecutable, "-jar", mmlExecutable.getAbsolutePath(),
                                     unitFile.getAbsolutePath(), "--no-startup" };

        }

        return new ProcessBuilder(command).directory(mmlExecutable.getAbsoluteFile().getParentFile()).inheritIO();
    }

    /**
     * Request MegaMekLab to print out record sheets for the current player's selected units. The method will try to
     * find MML either automatically or based on a configured client setting.
     *
     * @param unitList The list of units to print
     * @param button   This should always be ChatLounge#butPrintList, if you need to trigger this method from somewhere
     *                 else, override it.
     */
    public void printList(ArrayList<Entity> unitList, JButton button) {
        // Do nothing if there are no units to print
        if ((unitList == null) || unitList.isEmpty()) {
            return;
        }

        // Detect the MML executable.
        // If the user hasn't set this manually, try to pick "MegaMakLab.exe"/".sh"
        // from the same directory that MM is in
        var mmlPath = CP.getMmlPath();
        var autodetect = false;
        if (null == mmlPath || mmlPath.isBlank()) {
            autodetect = true;
            mmlPath = "MegaMekLab.jar";
        }

        var pb = printToMegaMekLab(unitList, new File(mmlPath), autodetect);
        if (pb == null) {
            return;
        }

        try {
            // It sometimes takes a while for MML to start, so we change the text of the button
            // to let the user know that something is happening
            button.setText(Messages.getString("ChatLounge.butPrintList.printing"));

            logger.info("Running command: {}", String.join(" ", pb.command()));


            var p = pb.start();

            // This thread's only purpose is to wait for the MML process to finish and change the button's text back to
            // its original value.
            new Thread(() -> {
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    logger.error(e);
                } finally {
                    button.setText(Messages.getString("ChatLounge.butPrintList"));
                }
            }).start();

        } catch (Exception e) {
            // If something goes wrong, probably ProcessBuild.start if anything,
            // Make sure to set the button text back to what it started as no matter what.
            button.setText(Messages.getString("ChatLounge.butPrintList"));
            logger.errorDialog(e, "Operation failed", "Error printing unit list");

        }
    }


    protected void saveVictoryList() {
        String filename = client.getLocalPlayer().getName();

        // Build the "save unit" dialog, if necessary.
        if (dlgSaveList == null) {
            dlgSaveList = new JFileChooser(".");
            dlgSaveList.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
            dlgSaveList.setDialogTitle(Messages.getString("ClientGUI.saveUnitListFileDialog.title"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString(
                  "ClientGUI.descriptionMULFiles"), CG_FILEPATH_MUL);
            dlgSaveList.setFileFilter(filter);
        }
        // Default to the player's name.
        dlgSaveList.setSelectedFile(new File(filename + CG_FILE_EXTENSION_MUL));

        int returnVal = dlgSaveList.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgSaveList.getSelectedFile() == null)) {
            return;
        }

        // Did the player select a file?
        File unitFile = dlgSaveList.getSelectedFile();
        if (unitFile != null) {
            if (!(unitFile.getName().toLowerCase().endsWith(CG_FILE_EXTENSION_MUL) || unitFile.getName()
                  .toLowerCase()
                  .endsWith(CG_FILE_EXTENSION_XML))) {
                try {
                    unitFile = new File(unitFile.getCanonicalPath() + CG_FILE_EXTENSION_MUL);
                } catch (Exception ignored) {
                    // nothing needs to be done here
                    return;
                }
            }

            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo(unitFile, getClient());
            } catch (Exception ex) {
                logger.error(ex, "saveVictoryList");
                doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
            }
        }
    }

    /**
     * Shows a dialog where the player can select the entity types used in the LOS tool.
     */
    private void showLOSSettingDialog() {
        LOSDialog ld = new LOSDialog(frame, GUIP.getMekInFirst(), GUIP.getMekInSecond());
        ignoreHotKeys = true;
        if (ld.showDialog().isConfirmed()) {
            GUIP.setMekInFirst(ld.getMekInFirst());
            GUIP.setMekInSecond(ld.getMekInSecond());
        }
        ignoreHotKeys = false;
    }

    /**
     * Shows the Nova CEWS network view dialog (read-only).
     */
    private void showNovaNetworkViewDialog() {
        NovaNetworkViewDialog dialog = new NovaNetworkViewDialog(frame, this);
        dialog.setVisible(true);
    }

    /**
     * Loads a preview image of the unit into the BufferedPanel.
     *
     * @param bp     The JLabel to set the image as icon to
     * @param entity The unit
     */
    public void loadPreviewImage(JLabel bp, Entity entity) {
        Camouflage camouflage = client.getLocalPlayer().getCamouflage();
        if (entity.hasOwner()) {
            camouflage = entity.getCamouflageOrElseOwners();
        }
        final Image base = MMStaticDirectoryManager.getMekTileset().imageFor(entity);
        final Image icon = new EntityImage(base, camouflage, frame, entity).loadPreviewImage(true);
        bp.setIcon((icon == null) ? null : new ImageIcon(icon));
    }

    /**
     * Make a "bing" sound.
     */
    public void bingChat() {
        audioService.playSound(SoundType.BING_CHAT);
    }

    public void bingMyTurn() {
        audioService.playSound(SoundType.BING_MY_TURN);
    }

    public void bingOthersTurn() {
        audioService.playSound(SoundType.BING_OTHERS_TURN);
    }

    private void setWeaponOrderPrefs(boolean prefChange) {
        for (Entity entity : client.getGame().getEntitiesVector()) {
            if ((entity.getOwner().equals(client.getLocalPlayer()))
                  && (!entity.getWeaponSortOrder().isCustom())
                  && ((!entity.isDeployed()) || (prefChange))) {
                entity.setWeaponSortOrder(GUIP.getDefaultWeaponSortOrder());
                client.sendEntityWeaponOrderUpdate(entity);
            }
        }
    }

    private final GameListener gameListener = new GameListenerAdapter() {

        @Override
        public void gameBoardNew(GameBoardNewEvent e) {
            Board newBoard = e.getNewBoard();
            final int boardId = e.getBoardId();

            if (newBoard != null) {
                try {
                    if (boardViews.containsKey(boardId)) {
                        boardViews.get(boardId).removeBoardViewListener(ClientGUI.this);
                        boardViews.get(boardId).dispose();
                    }
                    if (miniMaps.containsKey(boardId)) {
                        miniMaps.get(boardId).setVisible(false);
                        miniMaps.get(boardId).dispose();
                    }
                    BoardView boardView = new BoardView(client.getGame(), controller, ClientGUI.this, boardId);
                    MinimapDialog newMinimap = new MinimapDialog(frame);
                    newMinimap.add(new MinimapPanel(newMinimap,
                          client.getGame(),
                          boardView,
                          ClientGUI.this,
                          null,
                          boardId));
                    newMinimap.setVisible(true);
                    miniMaps.put(boardId, newMinimap);
                    boardViews.put(boardId, boardView);
                    boardView.getPanel().setPreferredSize(clientGuiPanel.getSize());
                    boardView.addBoardViewListener(ClientGUI.this);
                    var cb2 = new ChatterBoxOverlay(ClientGUI.this, boardView, controller, cb);
                    offBoardOverlay = new OffBoardTargetOverlay(ClientGUI.this);
                    boardView.getPanel().addKeyListener(cb2);
                    boardView.addOverlay(cb2);
                    boardView.addOverlay(new UnitOverviewOverlay(ClientGUI.this));
                    boardView.addOverlay(offBoardOverlay);
                    boardView.addOverlay(new KeyBindingsOverlay(boardView));
                    boardView.addOverlay(new PlanetaryConditionsOverlay(boardView));
                    boardView.addOverlay(new TurnDetailsOverlay(boardView));
                    boardView.setTooltipProvider(new TWBoardViewTooltip(client.getGame(), ClientGUI.this, boardView));
                    boardViewsContainer.updateMapTabs();
                    ruler = new RulerDialog(frame, client, boardView, client.getGame());
                    ruler.setLocation(GUIP.getRulerPosX(), GUIP.getRulerPosY());
                    ruler.setSize(GUIP.getRulerSizeHeight(), GUIP.getRulerSizeWidth());
                    UIUtil.updateWindowBounds(ruler);
                    boardView.addBoardViewListener(ClientGUI.this);
                } catch (IOException ex) {
                    // this is likely fatal anyway
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public void gamePlayerChange(GamePlayerChangeEvent evt) {
            if (playerListDialog != null) {
                playerListDialog.refreshPlayerList();

                if (currPhaseDisplay != null) {
                    currPhaseDisplay.setStatusBarWithNotDonePlayers();
                }
            }
        }

        @Override
        public void gamePlayerDisconnected(GamePlayerDisconnectedEvent evt) {
            if (!disconnectQuietly) {
                doAlertDialog(Messages.getString("ClientGUI.Disconnected.message"),
                      Messages.getString("ClientGUI.Disconnected.title"),
                      JOptionPane.ERROR_MESSAGE);
            }
            frame.setVisible(false);
            die();
        }

        @Override
        public void gamePlayerChat(GamePlayerChatEvent e) {
            bingChat();
        }

        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            for (IBoardView bv : boardViews()) {
                // This is a really lame place for this, but I couldn't find a
                // better one without making massive changes (which didn't seem
                // worth it for one little feature).
                if (bv.getLocalPlayer() != client.getLocalPlayer()) {
                    // The address based comparison is somewhat important.
                    // Use of the /reset command can cause the player to get reset,
                    // and the equals function of Player isn't powerful enough.
                    bv.setLocalPlayer(client.getLocalPlayer().getId());
                }
                if (bv instanceof BoardView boardView) {
                    // Make sure the ChatterBox starts out deactivated.
                    boardView.setChatterBoxActive(false);
                }
            }

            // Swap to this phase's panel.
            GamePhase phase = getClient().getGame().getPhase();
            switchPanel(phase);

            if (phase.isDeployment()) {
                setWeaponOrderPrefs(false);
            }

            menuBar.setPhase(phase);

            // Update the frame title to show current round and phase
            updateFrameTitle();

            // Update Nova Networks menu based on whether Nova CEWS units exist
            updateNovaNetworksMenu();

            clientGuiPanel.validate();
            cb.moveToEnd();
            hideFleeZone();
        }

        @Override
        public void gameEntityChange(GameEntityChangeEvent e) {
            if ((unitDisplayPanel != null)
                  && (unitDisplayPanel.getCurrentEntity() != null)
                  && (e.getEntity() != null)
                  && (unitDisplayPanel.getCurrentEntity().getId() == e.getEntity().getId())) {
                // underlying object may have changed, so reset
                unitDisplayPanel.displayEntity(e.getEntity());
            }
        }

        @Override
        public void gameEntityNew(GameEntityNewEvent e) {
            updateNovaNetworksMenu();
        }

        @Override
        public void gameEntityRemove(GameEntityRemoveEvent e) {
            updateNovaNetworksMenu();
        }

        @Override
        public void gameReport(GameReportEvent e) {
            // Normally the Report Display is updated when the panel is
            // switched during a phase change.
            // This update is for reports that get sent at odd times,
            // currently Tactical Genius reroll requests and when
            // a player wishes to continue moving after a fall.
            if (getClient().getGame().getPhase().isInitiativeReport()) {
                miniReportDisplayAddReportPages();
                reportDisplayResetDone();
                // Check if the player deserves an active reroll button
                // (possible, if he gets one which he didn't use, and his
                // opponent got and used one) and if so activates it.
                reportDisplayResetRerollInitiative();

                if (!(getClient() instanceof BotClient)) {
                    doAlertDialog(Messages.getString("ClientGUI.dialogTacticalGeniusReport"), e.getReport());
                }
            } else {
                // Continued movement after getting up
                if (!(getClient() instanceof BotClient)) {
                    doAlertDialog(Messages.getString("ClientGUI.dialogMovementReport"), e.getReport());
                }
            }
        }

        @Override
        public void gameEnd(GameEndEvent e) {
            getBoardView().clearMovementData();
            clearFieldOfFire();
            clearTemporarySprites();
            getLocalBots().values().forEach(AbstractClient::die);
            getLocalBots().clear();

            // Make a list of the player's living units.
            ArrayList<Entity> living = getClient().getGame().getPlayerEntities(getClient().getLocalPlayer(), false);

            // Be sure to include all units that have retreated.
            for (Enumeration<Entity> iter = getClient().getGame().getRetreatedEntities(); iter.hasMoreElements(); ) {
                Entity ent = iter.nextElement();
                if (ent.getOwnerId() == getClient().getLocalPlayer().getId()) {
                    living.add(ent);
                }
            }

            if (PreferenceManager.getClientPreferences().askForVictoryList()) {
                // Ask if you want to persist the final unit list from a battle encounter
                if (doYesNoDialog(Messages.getString("ClientGUI.SaveUnitsDialog.title"),
                      Messages.getString("ClientGUI.SaveUnitsDialog.message"))) {
                    saveVictoryList();
                }
            }

            // save all destroyed units in a separate "salvage MUL"
            ArrayList<Entity> destroyed = new ArrayList<>();
            Enumeration<Entity> graveyard = getClient().getGame().getGraveyardEntities();
            while (graveyard.hasMoreElements()) {
                Entity entity = graveyard.nextElement();
                if (entity.isSalvage()) {
                    destroyed.add(entity);
                }
            }

            if (!destroyed.isEmpty()) {
                String sLogDir = CP.getLogDirectory();
                File logDir = new File(sLogDir);
                if (!logDir.exists()) {
                    logDir.mkdir();
                }
                String fileName = CG_FILENAME_SALVAGE + CG_FILE_EXTENSION_MUL;
                if (CP.stampFilenames()) {
                    fileName = StringUtil.addDateTimeStamp(fileName);
                }
                File unitFile = new File(sLogDir + File.separator + fileName);
                try {
                    // Save the destroyed entities to the file.
                    EntityListFile.saveTo(unitFile, destroyed);
                } catch (IOException ex) {
                    logger.error(ex, "gameEnd");
                    doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
                }
            }

        }

        @Override
        public void gameSettingsChange(GameSettingsChangeEvent evt) {
            if ((gameOptionsDialog != null) && gameOptionsDialog.isVisible() && !evt.isMapSettingsOnlyChange()) {
                gameOptionsDialog.update(getClient().getGame().getOptions());
            }

            if (curPanel instanceof ChatLounge cl) {
                cl.updateMapSettings(getClient().getMapSettings());
            }
        }

        @Override
        public void gameScriptedEvent(GameScriptedEvent event) {
            if (event instanceof GameScriptedMessageEvent) {
                showScriptedMessage((GameScriptedMessageEvent) event);
            }
        }

        @Override
        public void gameClientFeedbackRequest(GameCFREvent gameCFREvent) {
            // Note: entity may be null for CFR types that don't use entityId (e.g., TAG_TARGET, TELEGUIDED_TARGET)
            // Each case handles null checking as appropriate
            Entity entity = client.getGame().getEntity(gameCFREvent.getEntityId());

            Object result;
            String input;

            switch (gameCFREvent.getCFRType()) {
                case CFR_DOMINO_EFFECT:
                    // If the client connects to a game as a bot, it's possible to have the bot respond AND have the
                    // client ask the player. This is bad, ignore this if the client is a bot
                    if (client instanceof BotClient) {
                        return;
                    }
                    if (entity == null) {
                        return;
                    }

                    MovePath stepForward = new MovePath(client.getGame(), entity);
                    MovePath stepBackward = new MovePath(client.getGame(), entity);
                    stepForward.addStep(MoveStepType.FORWARDS);
                    stepBackward.addStep(MoveStepType.BACKWARDS);
                    stepForward.compile(client.getGame(), entity, false);
                    stepBackward.compile(client.getGame(), entity, false);
                    Object[] options;
                    MovePath[] paths;
                    int optionType;
                    if (stepForward.isMoveLegal() && stepBackward.isMoveLegal()) {
                        options = new Object[3];
                        paths = new MovePath[3];
                        options[0] = Messages.getString("CFRDomino.Forward", stepForward.getMpUsed());
                        options[1] = Messages.getString("CFRDomino.Backward", stepForward.getMpUsed());
                        options[2] = Messages.getString("CFRDomino.NoAction");
                        paths[0] = stepForward;
                        paths[1] = stepBackward;
                        paths[2] = null;
                        optionType = JOptionPane.YES_NO_CANCEL_OPTION;
                    } else if (stepForward.isMoveLegal()) {
                        options = new Object[2];
                        paths = new MovePath[2];
                        options[0] = Messages.getString("CFRDomino.Forward", stepForward.getMpUsed());
                        options[1] = Messages.getString("CFRDomino.NoAction");
                        paths[0] = stepForward;
                        paths[1] = null;
                        optionType = JOptionPane.YES_NO_OPTION;
                    } else {
                        // No request is sent if both moves are illegal
                        options = new Object[2];
                        paths = new MovePath[2];
                        options[0] = Messages.getString("CFRDomino.Backward", stepForward.getMpUsed());
                        options[1] = Messages.getString("CFRDomino.NoAction");
                        paths[0] = stepBackward;
                        paths[1] = null;
                        optionType = JOptionPane.YES_NO_OPTION;
                    }
                    int choice = JOptionPane.showOptionDialog(frame,
                          Messages.getFormattedString("CFRDomino.Message", entity.getDisplayName()),
                          Messages.getString("CFRDomino.Title"),
                          optionType,
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          options,
                          options[0]);
                    // If they closed it, assume no action
                    if (choice == JOptionPane.CLOSED_OPTION) {
                        choice = options.length - 1;
                    }
                    client.sendDominoCFRResponse(paths[choice]);
                    break;
                case CFR_AMS_ASSIGN:
                    if (entity == null) {
                        return;
                    }
                    ArrayList<String> amsOptions = new ArrayList<>();
                    amsOptions.add(Messages.getString("NONE"));
                    for (WeaponAttackAction waa : gameCFREvent.getWAAs()) {
                        Entity ae = waa.getEntity(client.getGame());
                        String waaMsg;
                        if (ae != null) {
                            Mounted<?> weapon = ae.getEquipment(waa.getWeaponId());
                            waaMsg = weapon.getDesc() + " " + Messages.getString("FROM") + "  " + ae.getDisplayName();
                        } else {
                            waaMsg = Messages.getString("ClientGUI.missilesFromAnUnknownAttacker");
                        }
                        amsOptions.add(waaMsg);
                    }

                    result = JOptionPane.showInputDialog(frame,
                          Messages.getString("CFRAMSAssign.Message", entity.getDisplayName()),
                          Messages.getString("CFRAMSAssign.Title", entity.getDisplayName()),
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          amsOptions.toArray(),
                          null);
                    // If they closed it, assume no action
                    if ((result == null) || result.equals(Messages.getString("NONE"))) {
                        client.sendAMSAssignCFRResponse(null);
                    } else {
                        client.sendAMSAssignCFRResponse(amsOptions.indexOf(result) - 1);
                    }
                    break;
                case CFR_APDS_ASSIGN:
                    if (entity == null) {
                        return;
                    }
                    ArrayList<String> apdsOptions = new ArrayList<>();
                    apdsOptions.add(Messages.getString("NONE"));
                    Iterator<Integer> distIt = gameCFREvent.getApdsDistances().iterator();
                    for (WeaponAttackAction waa : gameCFREvent.getWAAs()) {
                        Entity ae = waa.getEntity(client.getGame());
                        int dist = distIt.next();
                        String waaMsg;
                        if (ae != null) {
                            Mounted<?> weapon = ae.getEquipment(waa.getWeaponId());
                            waaMsg = weapon.getDesc()
                                  + " "
                                  + Messages.getString("FROM")
                                  + " "
                                  + ae.getDisplayName()
                                  + " ("
                                  + Messages.getString("ClientGUI.distance")
                                  + " "
                                  + dist
                                  + ")";
                        } else {
                            waaMsg = Messages.getString("ClientGUI.missilesFromAnUnknownAttacker");
                        }
                        apdsOptions.add(waaMsg);
                    }

                    result = JOptionPane.showInputDialog(frame,
                          Messages.getString("CFRAPDSAssign.Message", entity.getDisplayName()),
                          Messages.getString("CFRAPDSAssign.Title", entity.getDisplayName()),
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          apdsOptions.toArray(),
                          null);
                    // If they closed it, assume no action
                    if ((result == null) || result.equals(Messages.getString("NONE"))) {
                        client.sendAPDSAssignCFRResponse(null);
                    } else {
                        client.sendAPDSAssignCFRResponse(apdsOptions.indexOf(result) - 1);
                    }
                    break;
                case CFR_HIDDEN_PBS:
                    Entity attacker = client.getGame().getEntity(gameCFREvent.getEntityId());
                    Entity target = client.getGame().getEntity(gameCFREvent.getTargetId());
                    // Are we not the client handling the PBS?
                    if ((attacker == null) || (target == null)) {
                        if (curPanel instanceof StatusBarPhaseDisplay) {
                            ((StatusBarPhaseDisplay) curPanel).setStatusBarText(Messages.getString(
                                  "StatusBarPhaseDisplay.pointblankShot"));
                        }
                        return;
                    }
                    // Confirm if these units can be part of a PBS at all
                    if (!Compute.canPointBlankShot(attacker, target)) {
                        // If we are the correct client but the PBS is not legal, return a cancellation
                        logger.error("Received request to handle an illegal pointblank shot ({} @ {} -> {} @ {})",
                              attacker.getDisplayName(),
                              attacker.getPosition().toFriendlyString(),
                              target.getDisplayName(),
                              target.getPosition().toFriendlyString());
                        client.sendHiddenPBSCFRResponse(null);
                        return;
                    }
                    // If this is the client to handle the PBS, take care of it
                    getBoardView().centerOnHex(attacker.getPosition());
                    getBoardView().highlight(attacker.getPosition());
                    getBoardView().select(target.getPosition());
                    getBoardView().cursor(target.getPosition());

                    // Ask whether the player wants to take a PBS or not
                    int pbsChoice = JOptionPane.showConfirmDialog(frame,
                          Messages.getString("ClientGUI.PointBlankShot.Message",
                                target.getShortName(),
                                attacker.getShortName()),
                          Messages.getString("ClientGUI.PointBlankShot.Title"),
                          JOptionPane.YES_NO_OPTION,
                          JOptionPane.QUESTION_MESSAGE);
                    // Process the PBS - switch to PointblankShotDisplay
                    if (pbsChoice == JOptionPane.YES_OPTION) {
                        // Send a non-null response to indicate PBS is accepted
                        // This allows the servers to notify the clients,
                        // as they may be in for a wait
                        client.sendHiddenPBSCFRResponse(new Vector<>());
                        // Used to indicate it's this player's turn
                        setPointblankEID(gameCFREvent.getEntityId());
                        // Switch to the right display
                        switchPanel(GamePhase.POINTBLANK_SHOT);
                        PointblankShotDisplay currentDisplay = ((PointblankShotDisplay) curPanel);
                        // Set targeting info
                        currentDisplay.beginMyTurn();
                        currentDisplay.selectEntity(gameCFREvent.getEntityId());
                        currentDisplay.target(target);
                        getBoardView().select(target.getPosition());
                    } else { // PBS declined
                        client.sendHiddenPBSCFRResponse(null);
                    }
                    break;
                case CFR_TELEGUIDED_TARGET:
                    logger.debug("CFR_TELEGUIDED_TARGET: processing teleguided missile target selection");
                    List<Integer> targetIds = gameCFREvent.getTelemissileTargetIds();
                    List<Integer> toHitValues = gameCFREvent.getTmToHitValues();
                    List<String> targetDescriptions = new ArrayList<>();
                    for (int i = 0; i < targetIds.size(); i++) {
                        int id = targetIds.get(i);
                        int th = toHitValues.get(i);
                        Entity tgt = client.getGame().getEntity(id);
                        if (tgt != null) {
                            targetDescriptions.add(Messages.getFormattedString("TeleMissileTargetDialog.target",
                                  tgt.getDisplayName(),
                                  th));
                        }
                    }
                    logger.debug("CFR_TELEGUIDED_TARGET: showing dialog with {} targets", targetDescriptions.size());
                    // Set up the selection pane
                    input = (String) JOptionPane.showInputDialog(frame,
                          Messages.getString("TeleMissileTargetDialog.message"),
                          Messages.getString("TeleMissileTargetDialog.title"),
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          targetDescriptions.toArray(),
                          targetDescriptions.get(0));
                    if (input != null) {
                        for (int i = 0; i < targetDescriptions.size(); i++) {
                            if (input.equals(targetDescriptions.get(i))) {
                                logger.debug("CFR_TELEGUIDED_TARGET: user selected target index {}", i);
                                client.sendTelemissileTargetCFRResponse(i);
                                break;
                            }
                        }
                    } else {
                        // If input is null, as in the case of pressing the close or cancel buttons...
                        // Just pick the first target in the list, or server will be left waiting
                        // indefinitely.
                        logger.debug("CFR_TELEGUIDED_TARGET: dialog cancelled, defaulting to first target");
                        client.sendTelemissileTargetCFRResponse(0);
                    }
                    break;
                case CFR_TAG_TARGET:
                    logger.debug("CFR_TAG_TARGET: processing TAG target selection");
                    List<Integer> TAGTargets = gameCFREvent.getTAGTargets();
                    List<Integer> TAGTargetTypes = gameCFREvent.getTAGTargetTypes();
                    List<String> TAGTargetDescriptions = new ArrayList<>();
                    for (int i = 0; i < TAGTargets.size(); i++) {
                        int id = TAGTargets.get(i);
                        int nType = TAGTargetTypes.get(i);
                        Targetable tgt = client.getGame().getTarget(nType, id);
                        if (tgt != null) {
                            TAGTargetDescriptions.add(tgt.getDisplayName());
                        }
                    }
                    logger.debug("CFR_TAG_TARGET: showing dialog with {} targets", TAGTargetDescriptions.size());
                    // Set up the selection pane
                    input = (String) JOptionPane.showInputDialog(frame,
                          Messages.getString("TAGTargetDialog.message"),
                          Messages.getString("TAGTargetDialog.title"),
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          TAGTargetDescriptions.toArray(),
                          TAGTargetDescriptions.get(0));
                    if (input != null) {
                        for (int i = 0; i < TAGTargetDescriptions.size(); i++) {
                            if (input.equals(TAGTargetDescriptions.get(i))) {
                                logger.debug("CFR_TAG_TARGET: user selected target index {}", i);
                                client.sendTAGTargetCFRResponse(i);
                                break;
                            }
                        }
                    } else {
                        // If input IS null, as in the case of pressing the close or cancel buttons...
                        // Just pick the first target in the list, or server will be left waiting
                        // indefinitely.
                        logger.debug("CFR_TAG_TARGET: dialog cancelled, defaulting to first target");
                        client.sendTAGTargetCFRResponse(0);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public Client getClient() {
        return client;
    }

    @Override
    public JComponent turnTimerComponent() {
        return menuBar;
    }

    @Override
    public void setChatBoxActive(boolean active) {
        getBoardView().setChatterBoxActive(active);
    }

    @Override
    public void clearChatBox() {
        Optional<IBoardView> ibv = getCurrentBoardView();
        if (ibv.isPresent() && ibv.get() instanceof BoardView bv) {
            bv.setChatterBoxActive(false);
        }
    }

    @Override
    public boolean isChatBoxActive() {
        Optional<IBoardView> ibv = getCurrentBoardView();
        return ibv.isPresent() && ibv.get() instanceof BoardView bv && bv.getChatterBoxActive();
    }

    @Override
    public Map<String, AbstractClient> getLocalBots() {
        return client.getBots();
    }

    /**
     * @param selectedEntityNum The selectedEntityNum to set.
     */
    public void setSelectedEntityNum(int selectedEntityNum) {
        boardViews().stream()
              .filter(bv -> bv instanceof BoardView)
              .map(bv -> (BoardView) bv)
              .forEach(bv -> bv.selectEntity(client.getGame().getEntity(selectedEntityNum)));
    }

    public RandomArmyDialog getRandomArmyDialog() {
        return randomArmyDialog;
    }

    public RandomNameDialog getRandomNameDialog() {
        return new RandomNameDialog(this);
    }

    /**
     * Checks to see if there is already a path and name stored; if not, calls "save as"; otherwise, saves the board to
     * the specified file.
     */
    private void boardSave(Game game) {
        if (curFileBoard == null) {
            boardSaveAs(game);
            return;
        }

        // save!
        try (OutputStream os = new FileOutputStream(curFileBoard)) {
            game.getBoard().save(os);
        } catch (Exception ex) {
            logger.error(ex, "Failed to save board!");
        }
    }

    /**
     * Saves the board in PNG image format.
     */
    private void boardSaveImage(boolean ignoreUnits) {
        if (curFileBoardImage == null) {
            boardSaveAsImage(ignoreUnits);
            return;
        }
        JDialog waitD = new JDialog(frame, Messages.getString("BoardEditor.waitDialog.title"));
        waitD.add(new JLabel(Messages.getString("BoardEditor.waitDialog.message")));
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation((frame.getSize().width / 2) - (waitD.getSize().width / 2),
              (frame.getSize().height / 2) - (waitD.getSize().height / 2));
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        try {
            ImageIO.write(boardViews.get(0).getEntireBoardImage(ignoreUnits, false),
                  CG_FILE_FORMAT_NAME_PNG,
                  curFileBoardImage);
        } catch (IOException e) {
            logger.error(e, "boardSaveImage");
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to the file.
     */
    public void boardSaveAs(Game game) {
        JFileChooser fc = new JFileChooser(CG_FILEPATH_DATA + File.separator + CG_FILEPATH_BOARDS);
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveBoardAs"));
        fc.setFileFilter(new BoardFileFilter());
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return;
        }
        curFileBoard = fc.getSelectedFile();

        // make sure the file ends in board
        if (!curFileBoard.getName().toLowerCase(Locale.ENGLISH).endsWith(CG_FILE_EXTENSION_BOARD)) {
            try {
                curFileBoard = new File(curFileBoard.getCanonicalPath() + CG_FILE_EXTENSION_BOARD);
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        boardSave(game);
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to the file as an image. Useful for printing
     * boards.
     */
    private void boardSaveAsImage(boolean ignoreUnits) {
        JFileChooser fc = new JFileChooser(".");
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveAsImage"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return (dir.getName().endsWith(CG_FILE_EXTENSION_PNG) || dir.isDirectory());
            }

            @Override
            public String getDescription() {
                return CG_FILE_EXTENSION_PNG;
            }
        });
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return;
        }
        curFileBoardImage = fc.getSelectedFile();

        // make sure the file ends in png
        if (!curFileBoardImage.getName().toLowerCase(Locale.ENGLISH).endsWith(CG_FILE_EXTENSION_PNG)) {
            try {
                curFileBoardImage = new File(curFileBoardImage.getCanonicalPath() + CG_FILE_EXTENSION_PNG);
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        boardSaveImage(ignoreUnits);
    }

    @Override
    public void hexMoused(BoardViewEvent event) {
        if (event.getType() == BoardViewEvent.BOARD_HEX_POPUP) {
            showBoardPopup(event);
        }
    }

    @Override
    public void hexCursor(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void boardHexHighlighted(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void hexSelected(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void firstLOSHex(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void secondLOSHex(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // ignored
    }

    @Override
    public boolean shouldIgnoreHotKeys() {
        return ignoreHotKeys
              || ((gameOptionsDialog != null) && gameOptionsDialog.isVisible())
              || UIUtil.isModalDialogDisplayed()
              || ((help != null) && help.isVisible())
              || ((commonSettingsDialog != null) && commonSettingsDialog.isVisible())
              || ((aw != null) && aw.isVisible());
    }

    void editBots() {
        var rpd = new EditBotsDialog(frame, this);
        rpd.setVisible(true);
        if (rpd.getResult() == DialogResult.CANCELLED) {
            return;
        }

        AddBotUtil util = new AddBotUtil();
        Map<String, BehaviorSettings> newBotSettings = rpd.getNewBots();
        for (String ghostName : newBotSettings.keySet()) {
            StringBuilder message = new StringBuilder();
            Princess princess = util.replaceGhostWithBot(newBotSettings.get(ghostName), ghostName, client, message);
            systemMessage(message.toString());
            // Make this princess a locally owned bot. This way it can be configured, and if on the lobby
            // it will faithfully press Done when the local player does.
            if (princess != null) {
                getLocalBots().put(ghostName, princess);
            }
        }

        Map<String, BehaviorSettings> changedBots = rpd.getChangedBots();
        for (String botName : changedBots.keySet()) {
            StringBuilder message = new StringBuilder();
            util.changeBotSettings(changedBots.get(botName), botName, client, message);
            systemMessage(message.toString());
        }

        Set<String> kickBotNames = rpd.getKickBots();
        for (String botName : kickBotNames) {
            StringBuilder message = new StringBuilder();
            util.kickBot(botName, client, message);
            systemMessage(message.toString());
        }
    }

    @Override
    public JComponent getCurrentPanel() {
        return curPanel;
    }

    public boolean isProcessingPointblankShot() {
        return pointblankEID != Entity.NONE;
    }

    public void setPointblankEID(int eid) {
        pointblankEID = eid;
    }

    public int getPointblankEID() {
        return pointblankEID;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        switch (e.getName()) {
            case GUIPreferences.MINI_MAP_ENABLED -> setMapVisible(GUIP.getMinimapEnabled());
            case GUIPreferences.PLAYER_LIST_ENABLED -> setPlayerListVisible(GUIP.getPlayerListEnabled());
            case GUIPreferences.UNIT_DISPLAY_ENABLED, GUIPreferences.UNIT_DISPLAY_LOCATION ->
                  setUnitDisplayVisible(GUIP.getUnitDisplayEnabled());
            case GUIPreferences.FORCE_DISPLAY_ENABLED -> setForceDisplayVisible(GUIP.getForceDisplayEnabled());
            case GUIPreferences.MINI_REPORT_ENABLED, GUIPreferences.MINI_REPORT_LOCATION ->
                  setMiniReportVisible(GUIP.getMiniReportEnabled());
            case GUIPreferences.DOCK_ON_LEFT, GUIPreferences.DOCK_MULTIPLE_ON_Y_AXIS -> {
                setUnitDisplayVisible(GUIP.getUnitDisplayEnabled());
                setMiniReportVisible(GUIP.getMiniReportEnabled());
            }
            case GUIPreferences.DEFAULT_WEAPON_SORT_ORDER -> {
                setWeaponOrderPrefs(true);
                getUnitDisplay().displayEntity(getUnitDisplay().getCurrentEntity());
            }
            case GUIPreferences.SOUND_BING_FILENAME_CHAT,
                 GUIPreferences.SOUND_BING_FILENAME_MY_TURN,
                 GUIPreferences.SOUND_BING_FILENAME_OTHERS_TURN -> audioService.loadSoundFiles();
            case GUIPreferences.MASTER_VOLUME -> audioService.setVolume();
            case GUIPreferences.BOT_COMMANDS_ENABLED -> setBotCommandsDialogVisible(GUIP.getBotCommandsEnabled());
        }
    }

    /**
     * Shows the movement envelope in the BoardView for the given entity. The movement envelope data is a map of move
     * end Coords to movement points used.
     *
     * @param entity    The entity for which the movement envelope is
     * @param mvEnvData The movement envelope data
     * @param gear      The move gear, MovementDisplay.GEAR_LAND or GEAR_JUMP
     */
    public void showMovementEnvelope(Entity entity, Map<Coords, Integer> mvEnvData, int gear) {
        movementEnvelopeHandler.setMovementEnvelope(mvEnvData,
              entity.getBoardId(),
              entity.getWalkMP(),
              entity.getRunMP(),
              entity.getAnyTypeMaxJumpMP(),
              gear);
    }

    /**
     * Removes visibility to the Movement Envelope.
     */
    public void clearMovementEnvelope() {
        movementEnvelopeHandler.clear();
    }

    /**
     * Removes all temporary sprites from the board, such as pending actions, movement envelope, collapse warnings etc.
     * Does not remove game-state sprites such as units or flares.
     */
    public void clearTemporarySprites() {
        clearMovementEnvelope();
        movementModifierSpriteHandler.clear();
        sensorRangeSpriteHandler.clear();
        collapseWarningSpriteHandler.clear();
        firingSolutionSpriteHandler.clear();
        firingArcSpriteHandler.clear();
    }

    /**
     * Shows the optimal available movement modifiers in the BoardView.
     *
     * @param movePaths The available longest move paths.
     */
    public void showMovementModifiers(Collection<MovePath> movePaths) {
        movementModifierSpriteHandler.renewSprites(movePaths);
    }

    /**
     * Shows the sensor/visual ranges for the given entity on its own position in the BoardView
     *
     * @param entity The entity that is looking/sensing
     */
    public void showSensorRanges(Entity entity) {
        showSensorRanges(entity, entity.getPosition());
    }

    /**
     * Shows the sensor/visual ranges for the given entity in the BoardView. The ranges are centered on the given
     * assumedPosition rather than the entity's own position.
     *
     * @param entity          The entity that is looking/sensing
     * @param assumedPosition The position to center all ranges on
     */
    public void showSensorRanges(Entity entity, Coords assumedPosition) {
        sensorRangeSpriteHandler.setSensorRange(entity, assumedPosition);
    }

    /**
     * Shows collapse warnings in the given list of Coords in the BoardView
     *
     * @param warnList The list of coordinates to show the warning on
     */
    public void showCollapseWarning(Collection<BoardLocation> warnList) {
        collapseWarningSpriteHandler.setCFWarningSprites(warnList);
    }

    /**
     * Shows ground object icons in the given list of Coords in the BoardView
     *
     * @param groundObjectList The list of coordinates to show
     */
    public void showGroundObjects(Map<Coords, List<ICarryable>> groundObjectList) {
        groundObjectSpriteHandler.setGroundObjectSprites(groundObjectList);
    }

    /**
     * Shows firing solutions from the viewpoint of the given entity on targets
     *
     * @param entity The attacking entity
     */
    public void showFiringSolutions(Entity entity) {
        firingSolutionSpriteHandler.showFiringSolutions(entity);
    }

    public JPanel getMainPanel() {
        return clientGuiPanel;
    }

    /**
     * @return The unit currently shown in the Unit Display. Note: This can be a unit than the one that is selected to
     *       move or fire.
     */
    @Nullable
    public Entity getDisplayedUnit() {
        return unitDisplayPanel.getCurrentEntity();
    }

    /**
     * Returns the weapon that is currently selected in the Unit Display. The selection can be void for various reasons,
     * therefore this returns it as an Optional. Note: this method does some additional checks to avoid bugs where the
     * weapon of the same ID on the unit is different from the selected weapon or is not even present on the unit. Also,
     * the displayed unit is checked to be an active unit (i.e. can be found in game.getEntity()). It will log an error
     * and return null otherwise. Using the returned weapon should be fairly safe.
     *
     * @return The weapon that is currently selected in the Unit Display, if any
     */
    public Optional<WeaponMounted> getDisplayedWeapon() {
        WeaponMounted weapon = unitDisplayPanel.wPan.getSelectedWeapon();
        if ((getDisplayedUnit() == null) || (weapon == null) || (client.getGame().getEntity(getDisplayedUnit().getId())
              == null)) {
            return Optional.empty();
        }
        Mounted<?> weaponOnUnit = getDisplayedUnit().getEquipment(unitDisplayPanel.wPan.getSelectedWeaponNum());
        if (weaponOnUnit == weapon) {
            return Optional.of(weapon);
        } else if (weapon.getEntity() instanceof HandheldWeapon hhw && hhw.getAttackingEntity()
              .equals(getDisplayedUnit())) {
            return Optional.of(weapon);
        } else {
            logger.error("Unsafe selected weapon. Returning null instead. Equipment ID {} on unit {}",
                  unitDisplayPanel.wPan.getSelectedWeaponNum(),
                  getDisplayedUnit());
            return Optional.empty();
        }
    }

    public Optional<AmmoMounted> getDisplayedAmmo() {
        return unitDisplayPanel.wPan.getSelectedAmmo();
    }

    @Override
    public void weaponSelected(MekDisplayEvent b) {
        setSelectedEntityNum(b.getEntityId());
        updateFiringArc(b.getEntity());
    }

    /**
     * Updates the shown firing arc. The given entity should be the one that has taken an action such as moving or torso
     * twisting or the unit whose selected weapon has changed. This method will check if the given unit is the one
     * displayed in the unit viewer and/or the currently acting unit and update or remove the firing arcs accordingly.
     *
     * @param entity The unit that has acted or is otherwise the origin of the update
     */
    public void updateFiringArc(Entity entity) {
        if ((entity == null) || (getDisplayedUnit() == null) || getDisplayedWeapon().isEmpty()) {
            // with no unit given or no unit displayed or no weapon selected, clear the
            // firing arcs
            clearFieldOfFire();
            return;

        } else if (!entity.equals(getDisplayedUnit())) {
            // the update is not for the displayed unit; therefore do not update the firing
            // arc
            return;
        }

        if (curPanel instanceof MovementDisplay md) {
            if (entity.getId() == md.getCurrentEntity()) {
                firingArcSpriteHandler.update(entity, getDisplayedWeapon().get(), md.getPlannedMovement());
                return;
            }
        }

        // not in an ActionPhase - or - the unit is not the acting unit:
        // show for viewed entity, no move or actions to be taken into account
        firingArcSpriteHandler.update(entity, getDisplayedWeapon().get());
        showFiringSolutions(entity);
    }

    /**
     * Removes the field of fire from the BoardView and clears the cached values in the sprite handler.
     */
    public void clearFieldOfFire() {
        firingArcSpriteHandler.clearValues();
    }

    /**
     * Updates the Nova Networks menu enablement based on whether the local player
     * has any Nova CEWS units in their force.
     */
    private void updateNovaNetworksMenu() {
        boolean hasNovaUnits = client.getGame().getEntitiesVector().stream()
            .filter(entity -> entity.getOwner().equals(client.getLocalPlayer()))
            .anyMatch(Entity::hasNovaCEWS);
        menuBar.setEnabled(VIEW_NOVA_NETWORKS, hasNovaUnits);
    }

    /**
     * Return the Current Hex, used by client commands for the visually impaired
     *
     * @return the current Hex
     */
    public Coords getCurrentHex() {
        return currentHex;
    }

    /**
     * Set the Current Hex, used by client commands for the visually impaired
     */
    public void setCurrentHex(Hex hex) {
        if (hex != null) {
            currentHex = hex.getCoords();
        }
    }

    public void setCurrentHex(Coords hex) {
        currentHex = hex;
    }

    private void toggleFleeZone() {
        showFleeZone = !showFleeZone;
        Entity entity = unitDisplayPanel.getCurrentEntity();
        if (showFleeZone && entity != null) {
            Game game = client.getGame();
            Board board = game.getBoard(entity);
            HexArea fleeZone = game.getFleeZone(entity);
            fleeZoneSpriteHandler.renewSprites(fleeZone.getCoords(board), board.getBoardId());
        } else {
            fleeZoneSpriteHandler.clear();
        }
    }

    public void hideFleeZone() {
        showFleeZone = false;
        if (fleeZoneSpriteHandler != null) {
            fleeZoneSpriteHandler.clear();
        }
    }

    /**
     * Returns true when the currently shown {@link BoardView} is showing some animation and should not be centered on
     * another hex or be hidden right now. An example is showing a unit's move animation.
     *
     * @return True when the currently shown BoardView is in the process of showing some animation
     */
    public boolean isCurrentBoardViewShowingAnimation() {
        return getCurrentBoardView().filter(IBoardView::isShowingAnimation).isPresent();
    }

    /**
     * Shows the BoardView of the given location, if it exists, and centers on its coords.
     *
     * @param boardLocation The location to show and center on
     */
    public void centerOnHex(@Nullable BoardLocation boardLocation) {
        if (client.getGame().hasBoardLocation(boardLocation)) {
            showBoardView(boardLocation.boardId());
            getBoardView(boardLocation).centerOnHex(boardLocation.coords());
        }
    }

    /**
     * Shows the BoardView of the given targetable, if it exists, and centers on its position, if that exists.
     *
     * @param targetable The unit or target to show and center on
     */
    public void centerOnUnit(@Nullable Targetable targetable) {
        if (targetable != null) {
            centerOnHex(targetable.getBoardLocation());
        }
    }

    @Override
    public BoardView getBoardView(int boardId) {
        // TW games use only standard BoardViews
        return (BoardView) super.getBoardView(boardId);
    }

    @Override
    public BoardView getBoardView(Targetable entity) {
        // TW games use only standard BoardViews
        return (BoardView) super.getBoardView(entity);
    }

    @Override
    public BoardView getBoardView(BoardLocation boardLocation) {
        // TW games use only standard BoardViews
        return (BoardView) super.getBoardView(boardLocation);
    }

    public void onAllBoardViews(Consumer<BoardView> consumer) {
        boardViews().forEach(boardView -> consumer.accept((BoardView) boardView));
    }

    public void suspendBoardTooltips() {
        onAllBoardViews(BoardView::suspendTooltip);
        // hide any currently shown tooltip, but don't disable tooltips entirely:
        ToolTipManager.sharedInstance().setEnabled(false);
        ToolTipManager.sharedInstance().setEnabled(true);
    }

    public void activateBoardTooltips() {
        onAllBoardViews(BoardView::activateTooltip);
    }

    public TilesetManager getTilesetManager() {
        return tilesetManager;
    }
}
