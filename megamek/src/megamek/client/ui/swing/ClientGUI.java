/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.TimerSingleton;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.client.ui.dialogs.helpDialogs.MMReadMeHelpDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.client.ui.swing.dialog.MegaMekUnitSelectorDialog;
import megamek.client.ui.swing.lobby.ChatLounge;
import megamek.client.ui.swing.lobby.PlayerSettingsDialog;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.BASE64ToolKit;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.icons.Camouflage;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.AddBotUtil;
import megamek.common.util.Distractable;
import megamek.common.util.StringUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ClientGUI extends JPanel implements BoardViewListener,
                    ActionListener, ComponentListener, IPreferenceChangeListener {
    //region Variable Declarations
    private static final long serialVersionUID = 3913466735610109147L;
    
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";
    
    /** The smallest GUI scaling value; smaller will make text unreadable */  
    public static final float MIN_GUISCALE = 0.7f;
    /** The highest GUI scaling value; increase this for 16K monitors */  
    public static final float MAX_GUISCALE = 2.4f;

    //region action commands
    //region main menu
    //Note: anything located in menu bars is not located here but in their menu
    public static final String MAIN_SKIN_NEW = "mainSkinNew";
    public static final String MAIN_QUIT = "mainQuit";
    //region file menu
    //game submenu
    public static final String FILE_GAME_NEW = "fileGameNew";
    public static final String FILE_GAME_SAVE = "fileGameSave";
    public static final String FILE_GAME_LOAD = "fileGameLoad";
    public static final String FILE_GAME_SAVE_SERVER = "fileGameSaveServer";
    public static final String FILE_GAME_QSAVE = "fileGameQSave";
    public static final String FILE_GAME_QLOAD = "fileGameQLoad";
    public static final String FILE_GAME_SCENARIO = "fileGameScenario";
    public static final String FILE_GAME_CONNECT_BOT = "fileGameConnectBot";
    public static final String FILE_GAME_CONNECT = "fileGameConnect";
    public static final String FILE_GAME_REPLACE_PLAYER = "replacePlayer";
    // board submenu
    public static final String BOARD_NEW = "fileBoardNew";
    public static final String BOARD_OPEN = "fileBoardOpen";
    public static final String BOARD_SAVE = "fileBoardSave";
    public static final String BOARD_SAVE_AS = "fileBoardSaveAs";
    public static final String BOARD_SAVE_AS_IMAGE = "fileBoardSaveAsImage";
    public static final String BOARD_SAVE_AS_IMAGE_UNITS = "fileBoardSaveAsImageUnits";
    public static final String BOARD_RESIZE = "boardResize";
    public static final String BOARD_VALIDATE = "boardValidate";
    public static final String BOARD_SOURCEFILE = "boardSourcefile";
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
    
    //unit list submenu
    public static final String FILE_UNITS_REINFORCE = "fileUnitsReinforce";
    public static final String FILE_UNITS_REINFORCE_RAT = "fileUnitsReinforceRAT";
    public static final String FILE_REFRESH_CACHE = "fileRefreshCache";
    public static final String FILE_UNITS_OPEN = "fileUnitsOpen";
    public static final String FILE_UNITS_SAVE = "fileUnitsSave";
    public static final String FILE_UNITS_PASTE = "fileUnitsPaste";
    public static final String FILE_UNITS_COPY = "fileUnitsCopy";
    //endregion file menu

    //region view menu
    public static final String VIEW_INCGUISCALE = "viewIncGUIScale";
    public static final String VIEW_DECGUISCALE = "viewDecGUIScale";
    public static final String VIEW_UNIT_DISPLAY = "viewMekDisplay";
    public static final String VIEW_ACCESSIBILITY_WINDOW = "viewAccessibilityWindow";
    public static final String VIEW_KEYBINDS_OVERLAY = "viewKeyboardShortcuts";
    public static final String VIEW_MINI_MAP = "viewMiniMap";
    public static final String VIEW_UNIT_OVERVIEW = "viewUnitOverview";
    public static final String VIEW_ZOOM_IN = "viewZoomIn";
    public static final String VIEW_ZOOM_OUT = "viewZoomOut";
    public static final String VIEW_TOGGLE_ISOMETRIC = "viewToggleIsometric";
    public static final String VIEW_TOGGLE_HEXCOORDS = "viewToggleHexCoords";
    public static final String VIEW_LABELS = "viewLabels";
    public static final String VIEW_TOGGLE_FIELD_OF_FIRE = "viewToggleFieldOfFire";
    public static final String VIEW_TOGGLE_FOV_DARKEN = "viewToggleFovDarken";
    public static final String VIEW_TOGGLE_FOV_HIGHLIGHT = "viewToggleFovHighlight";
    public static final String VIEW_TOGGLE_FIRING_SOLUTIONS = "viewToggleFiringSolutions";
    public static final String VIEW_MOVE_ENV = "viewMovementEnvelope";
    public static final String VIEW_MOVE_MOD_ENV = "viewMovModEnvelope";
    public static final String VIEW_CHANGE_THEME = "viewChangeTheme";
    public static final String VIEW_ROUND_REPORT = "viewRoundReport";
    public static final String VIEW_GAME_OPTIONS = "viewGameOptions";
    public static final String VIEW_CLIENT_SETTINGS = "viewClientSettings";
    public static final String VIEW_LOS_SETTING = "viewLOSSetting";
    public static final String VIEW_PLAYER_SETTINGS = "viewPlayerSettings";
    public static final String VIEW_PLAYER_LIST = "viewPlayerList";
    public static final String VIEW_RESET_WINDOW_POSITIONS = "viewResetWindowPos";
    //endregion view menu

    //region fire menu
    public static final String FIRE_SAVE_WEAPON_ORDER = "saveWeaponOrder";
    //endregion fire menu

    //region help menu
    public static final String HELP_CONTENTS = "helpContents";
    public static final String HELP_SKINNING = "helpSkinning";
    public static final String HELP_ABOUT = "helpAbout";
    //endregion help menu
    //endregion action commands

    // a frame, to show stuff in
    public JFrame frame;

    // A menu bar to contain all actions.
    protected CommonMenuBar menuBar;
    private CommonAboutDialog about;
    private AbstractHelpDialog help;
    private CommonSettingsDialog setdlg;
    private AccessibilityWindow aw;

    public MegaMekController controller;
    private ChatterBox cb;
    public ChatterBox2 cb2;
    private BoardView bv;
    private Component bvc;
    private UnitDetailPane unitDetailPane;
    public UnitDisplay mechD;
    public JDialog minimapW;
    private MapMenu popup;
    private UnitOverview uo;
    private Ruler ruler;
    protected JComponent curPanel;
    public ChatLounge chatlounge;
    private OffBoardTargetOverlay offBoardOverlay;

    // some dialogs...
    private GameOptionsDialog gameOptionsDialog;
    private AbstractUnitSelectorDialog mechSelectorDialog;
    private PlayerListDialog playerListDialog;
    private RandomArmyDialog randomArmyDialog;
    private PlanetaryConditionsDialog conditionsDialog;
    /**
     * Save and Open dialogs for MegaMek Unit List (mul) files.
     */
    private JFileChooser dlgLoadList;
    private JFileChooser dlgSaveList;
    private Client client;

    private File curfileBoardImage;
    private File curfileBoard;

    /**
     * Cache for the "bing" soundclip.
     */
    private AudioClip bingClip;

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private Map<String, String> mainNames = new HashMap<>();

    /**
     * The <code>JPanel</code> containing the main display area.
     */
    private JPanel panMain = new JPanel();

    /**
     * The <code>CardLayout</code> of the main display area.
     */
    private CardLayout cardsMain = new CardLayout();

    /**
     * Map each phase to the name of the card for the secondary area.
     */
    private Map<String, String> secondaryNames = new HashMap<>();

    /**
     * The <code>JPanel</code> containing the secondary display area.
     */
    private JPanel panSecondary = new JPanel();
    
    private StatusBarPhaseDisplay currPhaseDisplay;

    /**
     * The <code>CardLayout</code> of the secondary display area.
     */
    private CardLayout cardsSecondary = new CardLayout();

    /**
     * Map phase component names to phase component objects.
     */
    private Map<String, JComponent> phaseComponents = new HashMap<>();

    /**
     * Current Selected entity
     */
    private int selectedEntityNum = Entity.NONE;

    /**
     * Flag that indicates whether hotkeys should be ignored or not. This is
     * used for disabling hot keys when various dialogs are displayed.
     */
    private boolean ignoreHotKeys = false;

    /**
     * Keeps track of the Entity ID for the entity currently taking a pointblank
     * shot.
     */
    private int pointblankEID = Entity.NONE;
    //endregion Variable Declarations

    /**
     * Construct a client which will display itself in a new frame. It will not
     * try to connect to a server yet. When the frame closes, this client will
     * clean up after itself as much as possible, but will not call
     * System.exit().
     */
    public ClientGUI(Client client, MegaMekController c) {
        super(new BorderLayout());
        this.addComponentListener(this);
        this.client = client;
        controller = c;
        loadSoundClip();
        panMain.setLayout(cardsMain);
        panSecondary.setLayout(cardsSecondary);
        JPanel panDisplay = new JPanel(new BorderLayout());
        panDisplay.add(panMain, BorderLayout.CENTER);
        panDisplay.add(panSecondary, BorderLayout.SOUTH);
        add(panDisplay, BorderLayout.CENTER);
    }

    public BoardView getBoardView() {
        return bv;
    }

    public UnitDetailPane getUnitDetailPane() {
        return this.unitDetailPane;
    }

    /**
     * Try to load the "bing" sound clip.
     */
    private void loadSoundClip() {
        if (GUIPreferences.getInstance().getSoundBingFilename() == null) {
            return;
        }
        try {
            File file = new File(GUIPreferences.getInstance().getSoundBingFilename());
            if (!file.exists()) {
                LogManager.getLogger().error("Failed to load audio file: " + GUIPreferences.getInstance().getSoundBingFilename());
                return;
            }
            bingClip = Applet.newAudioClip(file.toURI().toURL());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * Display a system message in the chat box.
     *
     * @param message the <code>String</code> message to be shown.
     */
    public void systemMessage(String message) {
        cb.systemMessage(message);
        cb2.addChatMessage("MegaMek: " + message);
    }

    /**
     * Initializes a number of things about this frame.
     */
    private void initializeFrame() {
        frame = new JFrame(Messages.getString("ClientGUI.title"));
        frame.setJMenuBar(menuBar);

        var prefs = GUIPreferences.getInstance();
        if (prefs.getWindowSizeHeight() != 0) {
            frame.setLocation(
                prefs.getWindowPosX(),
                prefs.getWindowPosY()
            );
            frame.setSize(
                prefs.getWindowSizeWidth(),
                prefs.getWindowSizeHeight()
            );
        } else {
            frame.setSize(800, 600);
        }
        frame.setMinimumSize(new Dimension(640, 480));
        UIUtil.updateWindowBounds(frame);

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        List<Image> iconList = new ArrayList<>();
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_16X16).toString()
        ));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_32X32).toString()
        ));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_48X48).toString()
        ));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_256X256).toString()
        ));
        frame.setIconImages(iconList);
    }

    /**
     * Lays out the frame by setting this Client object to take up the full
     * frame display area.
     */
    private void layoutFrame() {
        frame.setTitle(client.getName() + Messages.getString("ClientGUI.clientTitleSuffix"));
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();
    }

    /**
     * Have the client register itself as a listener wherever it's needed.
     * <p>
     * According to
     * http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a
     * major bad no-no to perform these registrations before the constructor
     * finishes, so this function has to be called after the <code>Client</code>
     * is created.
     */
    public void initialize() {
        var prefs = GUIPreferences.getInstance();

        menuBar = new CommonMenuBar(getClient());
        initializeFrame();
        try {
            client.getGame().addGameListener(gameListener);
            // Create the board viewer.
            bv = new BoardView(client.getGame(), controller, this);
            bv.setPreferredSize(getSize());
            bvc = bv.getComponent();
            bvc.setName("BoardView");
            bv.addBoardViewListener(this);
            client.setBoardView(bv);
        } catch (Exception ex) {
            LogManager.getLogger().fatal(ex);
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"),
                    Messages.getString("ClientGUI.FatalError.message") + ex);
            die();
        }

        layoutFrame();
        menuBar.addActionListener(this);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!GUIPreferences.getInstance().getBoolean(GUIPreferences.ADVANCED_NO_SAVE_NAG)) {
                    ignoreHotKeys = true;
                    int savePrompt = JOptionPane.showConfirmDialog(null,
                            "Do you want to save the game before quitting MegaMek?",
                            "Save First?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    ignoreHotKeys = false;
                    if (savePrompt == JOptionPane.YES_OPTION) {
                        if (!saveGame()) {
                            // When the user did not actually save the game, don't close MM
                            return;
                        }
                    }
                    if (savePrompt == JOptionPane.NO_OPTION || savePrompt == JOptionPane.YES_OPTION)
                    {
                        frame.setVisible(false);
                        unitDetailPane.setVisible(false);
                        saveSettings();
                        die();
                    }
                } else {
                    frame.setVisible(false);
                    saveSettings();
                    die();
                }
            }
        });
        cb2 = new ChatterBox2(this, bv, controller);
        bv.addDisplayable(cb2);
        bv.addKeyListener(cb2);
        uo = new UnitOverview(this);
        offBoardOverlay = new OffBoardTargetOverlay(this);

        aw = new AccessibilityWindow(this);
        aw.setLocation(0, 0);
        aw.setSize(300, 300);

        bv.addDisplayable(uo);
        bv.addDisplayable(offBoardOverlay);

        mechD = new UnitDisplay(this, controller);
        mechD.addMechDisplayListener(bv);

        this.unitDetailPane = new UnitDetailPane(this.mechD);
        this.unitDetailPane.setVisible(false);
        add(this.unitDetailPane, BorderLayout.EAST);

        Ruler.color1 = GUIPreferences.getInstance().getRulerColor1();
        Ruler.color2 = GUIPreferences.getInstance().getRulerColor2();
        ruler = new Ruler(frame, client, bv);
        ruler.setLocation(
            prefs.getRulerPosX(),
            prefs.getRulerPosY()
        );
        ruler.setSize(
            prefs.getRulerSizeHeight(),
            prefs.getRulerSizeWidth()
        );
        UIUtil.updateWindowBounds(ruler);

        minimapW = MiniMap.createMinimap(frame, getBoardView(), getClient().getGame(), this);
        cb = new ChatterBox(this);
        cb.setChatterBox2(cb2);
        cb2.setChatterBox(cb);
        client.changePhase(GamePhase.UNKNOWN);
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MechSummaryCache.getInstance().isInitialized()) {
            unitLoadingDialog.setVisible(true);
        }
        mechSelectorDialog = new MegaMekUnitSelectorDialog(this, unitLoadingDialog);
        randomArmyDialog = new RandomArmyDialog(this);
        new Thread(mechSelectorDialog, "Mech Selector Dialog").start();
        frame.setVisible(true);
        GUIP.addPreferenceChangeListener(this);
    }

    /**
     * Get the menu bar for this client.
     *
     * @return the <code>CommonMenuBar</code> of this client.
     */
    public CommonMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    private void showAbout() {
        // Do we need to create the "about" dialog?
        if (about == null) {
            about = new CommonAboutDialog(frame);
        }

        // Show the about dialog.
        about.setVisible(true);
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
            StringBuilder helpPath = new StringBuilder("file:///");
            helpPath.append(System.getProperty("user.dir"));
            if (!helpPath.toString().endsWith(File.separator)) {
                helpPath.append(File.separator);
            }
            helpPath.append(Messages.getString("ClientGUI.skinningHelpPath"));
            URL helpUrl = new URL(helpPath.toString());

            // Launch the help dialog.
            HelpDialog helpDialog = new HelpDialog(
                    Messages.getString("ClientGUI.skinningHelpPath.title"),
                    helpUrl);
            helpDialog.setVisible(true);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "ERROR",
                    JOptionPane.ERROR_MESSAGE);
            LogManager.getLogger().error("", e);
        }
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    private void showSettings() {
        // Do we need to create the "settings" dialog?
        if (setdlg == null) {
            setdlg = new CommonSettingsDialog(frame, this);
        }

        // Show the settings dialog.
        setdlg.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Game Options" menu item.
     */
    private void showOptions() {
        getGameOptionsDialog().setEditable(client.getGame().getPhase().isLounge());
        // Display the game options dialog.
        getGameOptionsDialog().update(client.getGame().getOptions());
        getGameOptionsDialog().setVisible(true);
    }

    public void customizePlayer() {
        PlayerSettingsDialog psd = new PlayerSettingsDialog(this, client);
        psd.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Player List" menu item.
     */
    private void showPlayerList() {
        if (playerListDialog == null) {
            playerListDialog = new PlayerListDialog(frame, client);
        }
        playerListDialog.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Round Report" menu item.
     */
    private void showRoundReport() {
        ignoreHotKeys = true;
        new MiniReportDisplay(frame, client).setVisible(true);
        ignoreHotKeys = false;
    }

    /**
     * Implement the <code>ActionListener</code> interface.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        switch (event.getActionCommand()) {
            case VIEW_RESET_WINDOW_POSITIONS:
                minimapW.setBounds(0, 0, minimapW.getWidth(), minimapW.getHeight());
                this.unitDetailPane.getWindow().setLocation(0, 0);
                this.unitDetailPane.getWindow().pack();
                break;
            case FILE_GAME_SAVE:
                saveGame();
                break;
            case FILE_GAME_QSAVE:
                quickSaveGame();
                break;
            case FILE_GAME_SAVE_SERVER:
                ignoreHotKeys = true;
                String filename = (String) JOptionPane.showInputDialog(frame,
                        Messages.getString("ClientGUI.FileSaveServerDialog.message"),
                        Messages.getString("ClientGUI.FileSaveServerDialog.title"),
                        JOptionPane.QUESTION_MESSAGE, null, null,
                        MMConstants.DEFAULT_SAVEGAME_NAME);
                if (filename != null) {
                    client.sendChat("/save " + filename);
                }
                ignoreHotKeys = false;
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
                loadListFile(client.getLocalPlayer(), true);
                ignoreHotKeys = false;
                break;
            case FILE_UNITS_REINFORCE_RAT:
                ignoreHotKeys = true;
                if (client.getLocalPlayer().getTeam() == Player.TEAM_UNASSIGNED) {
                    String title = Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceTitle");
                    String msg = Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceMessage");
                    JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                getRandomArmyDialog().setVisible(true);
                ignoreHotKeys = false;
                break;
            case FILE_REFRESH_CACHE:
                MechSummaryCache.refreshUnitData(false);
                new Thread(mechSelectorDialog, "Mech Selector Dialog").start();
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
                showPlayerList();
                break;
            case VIEW_ROUND_REPORT:
                showRoundReport();
                break;
            case BOARD_SAVE:
                ignoreHotKeys = true;
                boardSave();
                ignoreHotKeys = false;
                break;
            case BOARD_SAVE_AS:
                ignoreHotKeys = true;
                boardSaveAs();
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
            case FILE_GAME_REPLACE_PLAYER:
                replacePlayer();
                break;
            case VIEW_ACCESSIBILITY_WINDOW:
                toggleAccessibilityWindow();
                break;
            case VIEW_UNIT_OVERVIEW:
                toggleUnitOverview();
                break;
            case VIEW_LOS_SETTING:
                showLOSSettingDialog();
                break;
            case VIEW_ZOOM_IN:
                bv.zoomIn();
                break;
            case VIEW_ZOOM_OUT:
                bv.zoomOut();
                break;
            case VIEW_TOGGLE_ISOMETRIC:
                GUIPreferences.getInstance().setIsometricEnabled(bv.toggleIsometric());
                break;
            case VIEW_TOGGLE_FOV_HIGHLIGHT:
                GUIPreferences.getInstance().setFovHighlight(
                        !GUIPreferences.getInstance().getFovHighlight());
                bv.refreshDisplayables();
                if (client.getGame().getPhase() == GamePhase.MOVEMENT) {
                    bv.clearHexImageCache();
                }
                break;
            case VIEW_TOGGLE_FIELD_OF_FIRE:
                GUIPreferences.getInstance().setShowFieldOfFire(
                        !GUIPreferences.getInstance().getShowFieldOfFire());
                bv.repaint();
                break;
            case VIEW_TOGGLE_FOV_DARKEN:
                GUIPreferences.getInstance().setFovDarken(!GUIPreferences.getInstance().getFovDarken());
                bv.refreshDisplayables();
                if (client.getGame().getPhase() == GamePhase.MOVEMENT) {
                    bv.clearHexImageCache();
                }
                break;
            case VIEW_TOGGLE_FIRING_SOLUTIONS:
                GUIPreferences.getInstance().setFiringSolutions(
                        !GUIPreferences.getInstance().getFiringSolutions());
                if (!GUIPreferences.getInstance().getFiringSolutions()) {
                    bv.clearFiringSolutionData();
                } else {
                    if (curPanel instanceof FiringDisplay) {
                        ((FiringDisplay) curPanel).setFiringSolutions();
                    }
                }
                bv.refreshDisplayables();
                break;
            case VIEW_MOVE_ENV:
                if (curPanel instanceof MovementDisplay) {
                    GUIPreferences.getInstance().setMoveEnvelope(
                            !GUIPreferences.getInstance().getMoveEnvelope());
                    ((MovementDisplay) curPanel).computeMovementEnvelope(mechD.getCurrentEntity());
                }
                break;
            case VIEW_MOVE_MOD_ENV:
                if (curPanel instanceof MovementDisplay) {
                    ((MovementDisplay) curPanel).computeModifierEnvelope();
                }
                break;
            case VIEW_CHANGE_THEME:
                bv.changeTheme();
                break;
            case FIRE_SAVE_WEAPON_ORDER:
                Entity ent = mechD.getCurrentEntity();
                if (ent != null) {
                    WeaponOrderHandler.setWeaponOrder(ent.getChassis(), ent.getModel(),
                            ent.getWeaponSortOrder(), ent.getCustomWeaponOrder());
                    client.sendEntityWeaponOrderUpdate(ent);
                }
                break;
        }
    }

    /**
     * Save all the current in use Entities each grouped by
     * player name
     * <p>
     * and a file for salvage
     */
    public void doSaveUnit() {
        for (Enumeration<Player> iter = getClient().getGame().getPlayers(); iter.hasMoreElements(); ) {
            Player p = iter.nextElement();
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
            String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
            File logDir = new File(sLogDir);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            String fileName = "salvage.mul"; // TODO : remove inline filename
            if (PreferenceManager.getClientPreferences().stampFilenames()) {
                fileName = StringUtil.addDateTimeStamp(fileName);
            }
            File unitFile = new File(sLogDir + File.separator + fileName);
            try {
                // Save the destroyed entities to the file.
                EntityListFile.saveTo(unitFile, destroyed);
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
                doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
            }
        }
    }

    /**
     * Saves the current settings to the cfg file.
     */
    void saveSettings() {
        // Frame location
        GUIPreferences.getInstance().setWindowPosX(frame.getLocation().x);
        GUIPreferences.getInstance().setWindowPosY(frame.getLocation().y);
        GUIPreferences.getInstance().setWindowSizeWidth(frame.getSize().width);
        GUIPreferences.getInstance().setWindowSizeHeight(frame.getSize().height);

        // Minimap
        if ((minimapW != null) && ((minimapW.getSize().width * minimapW.getSize().height) > 0)) {
            GUIPreferences.getInstance().setMinimapPosX(minimapW.getLocation().x);
            GUIPreferences.getInstance().setMinimapPosY(minimapW.getLocation().y);
        }

        // Mek display
        var unitDetailWindow = this.unitDetailPane.getWindow();
        if ((unitDetailWindow.getSize().width * unitDetailWindow.getSize().height) > 0) {
            GUIPreferences.getInstance().setUnitDetailPosX(unitDetailWindow.getLocation().x);
            GUIPreferences.getInstance().setUnitDetailPosY(unitDetailWindow.getLocation().y);
            GUIPreferences.getInstance().setUnitDetailSizeWidth(unitDetailWindow.getSize().width);
            GUIPreferences.getInstance().setUnitDetailSizeHeight(unitDetailWindow.getSize().height);
        }

        // Ruler display
        if ((ruler != null) && (ruler.getSize().width != 0) && (ruler.getSize().height != 0)) {
            GUIPreferences.getInstance().setRulerPosX(ruler.getLocation().x);
            GUIPreferences.getInstance().setRulerPosY(ruler.getLocation().y);
            GUIPreferences.getInstance().setRulerSizeWidth(ruler.getSize().width);
            GUIPreferences.getInstance().setRulerSizeHeight(ruler.getSize().height);
        }
    }

    /**
     * Shuts down threads and sockets
     */
    void die() {
        // Tell all the displays to remove themselves as listeners.
        boolean reportHandled = false;
        if (bv != null) {
            // cleanup our timers first
            bv.die();
        }

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

        frame.removeAll();
        frame.setVisible(false);
        try {
            frame.dispose();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        client.die();

        TimerSingleton.getInstance().killTimer();

        if (controller != null) {
            controller.removeAllActions();
            controller.clientgui = null;
        }

        if (menuBar != null) {
            menuBar.die();
            menuBar = null;
        }
        GUIP.removePreferenceChangeListener(this);
    }

    public GameOptionsDialog getGameOptionsDialog() {
        if (gameOptionsDialog == null) {
            gameOptionsDialog = new GameOptionsDialog(this);
        }
        return gameOptionsDialog;
    }

    public AbstractUnitSelectorDialog getMechSelectorDialog() {
        return mechSelectorDialog;
    }

    public PlanetaryConditionsDialog getPlanetaryConditionsDialog() {
        if (conditionsDialog == null) {
            conditionsDialog = new PlanetaryConditionsDialog(this);
        }
        return conditionsDialog;
    }

    void switchPanel(GamePhase phase) {
        // Clear the old panel's listeners.
        if (curPanel instanceof BoardViewListener) {
            bv.removeBoardViewListener((BoardViewListener) curPanel);
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
                ReportDisplay rD = (ReportDisplay) phaseComponents.get(String.valueOf(GamePhase.INITIATIVE_REPORT));
                if (rD != null) {
                    rD.resetTabs();
                }
                ChatLounge cl = (ChatLounge) phaseComponents.get(String.valueOf(GamePhase.LOUNGE));
                cb.setDoneButton(cl.butDone);
                cl.add(cb.getComponent(), BorderLayout.SOUTH);
                getBoardView().getTilesetManager().reset();
                this.unitDetailPane.setVisible(false);
                setMapVisible(false);
                break;
            case POINTBLANK_SHOT:
            case SET_ARTILLERY_AUTOHIT_HEXES:
            case DEPLOY_MINEFIELDS:
            case DEPLOYMENT:
            case TARGETING:
            case PREMOVEMENT:
            case MOVEMENT:
            case OFFBOARD:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
                if (frame.isShowing()) {
                    maybeShowMinimap();
                    maybeShowUnitDisplay();
                }
                break;
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
                rD = (ReportDisplay) phaseComponents.get(String.valueOf(GamePhase.INITIATIVE_REPORT));
                cb.setDoneButton(rD.butDone);
                rD.add(cb.getComponent(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
                setMapVisible(false);
                setUnitDisplayVisible(false);
                break;
            default:
                break;
        }

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
        if (curPanel instanceof BoardViewListener) {
            bv.addBoardViewListener((BoardViewListener) curPanel);
        }

        if (curPanel instanceof ActionListener) {
            menuBar.addActionListener((ActionListener) curPanel);
        }

        if (curPanel instanceof Distractable) {
            ((Distractable) curPanel).setIgnoringEvents(false);
        }

        // Make the new panel the focus, if the Client option says so
        if (GUIPreferences.getInstance().getFocus() && !(client instanceof TestBot)) {
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
                main = "ChatLounge";
                component.setName(main);
                panMain.add(component, main);
                break;
            case STARTING_SCENARIO:
                component = new JLabel(Messages.getString("ClientGUI.StartingScenario"));
                main = "JLabel-StartingScenario";
                component.setName(main);
                panMain.add(component, main);
                break;
            case EXCHANGE:
                chatlounge.killPreviewBV();
                component = new JLabel(Messages.getString("ClientGUI.TransmittingData"));
                main = "JLabel-Exchange";
                component.setName(main);
                panMain.add(component, main);
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
                component = new SelectArtyAutoHitHexDisplay(this);
                main = "BoardView";
                secondary = "SelectArtyAutoHitHexDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOY_MINEFIELDS:
                component = new DeployMinefieldDisplay(this);
                main = "BoardView";
                secondary = "DeployMinefieldDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOYMENT:
                component = new DeploymentDisplay(this);
                main = "BoardView";
                secondary = "DeploymentDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case TARGETING:
                component = new TargetingPhaseDisplay(this, false);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "TargetingPhaseDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                offBoardOverlay.setTargetingPhaseDisplay((TargetingPhaseDisplay) component);
                break;
            case PREMOVEMENT:
                component = new PrephaseDisplay(this, GamePhase.PREMOVEMENT);
                ((PrephaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "PremovementDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case MOVEMENT:
                component = new MovementDisplay(this);
                main = "BoardView";
                secondary = "MovementDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case OFFBOARD:
                component = new TargetingPhaseDisplay(this, true);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "OffboardDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PREFIRING:
                component = new PrephaseDisplay(this, GamePhase.PREFIRING);
                ((PrephaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "Prefiring";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case FIRING:
                component = new FiringDisplay(this);
                main = "BoardView";
                secondary = "FiringDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case POINTBLANK_SHOT:
                component = new PointblankShotDisplay(this);
                main = "BoardView";
                secondary = "PointblankShotDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PHYSICAL:
                component = new PhysicalDisplay(this);
                main = "BoardView";
                secondary = "PhysicalDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case INITIATIVE_REPORT:
                component = new ReportDisplay(this);
                main = "ReportDisplay";
                component.setName(main);
                panMain.add(main, component);
                break;
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
                // Try to reuse the ReportDisplay for other phases...
                component = phaseComponents.get(String.valueOf(GamePhase.INITIATIVE_REPORT));
                if (component == null) {
                    // no ReportDisplay to reuse - get a new one
                    component = initializePanel(GamePhase.INITIATIVE_REPORT);
                }
                main = "ReportDisplay";
                break;
            default:
                component = new JLabel(Messages.getString("ClientGUI.waitingOnTheServer"));
                main = "JLabel-Default";
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

    protected void showBoardPopup(Coords c) {
        if (fillPopup(c)) {
            bv.showPopup(popup, c);
        }
    }

    /** 
     * Switches the Minimap and the MechDisplay an and off together.
     * If the MechDisplay is active, both will be hidden, else
     * both will be shown.
     */
    public void toggleMMUDDisplays() {
        GUIP.toggleUnitDisplay();
        GUIP.setMinimapEnabled(GUIP.getBoolean(GUIPreferences.SHOW_UNIT_DISPLAY));
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

    private void toggleUnitOverview() {
        uo.setVisible(!uo.isVisible());
        GUIPreferences.getInstance().setShowUnitOverview(uo.isVisible());
        bv.refreshDisplayables();
    }

    /** Shows or hides the minimap based on the current menu setting. */
    private void maybeShowMinimap() {
        setMapVisible(GUIP.getMinimapEnabled());
    }

    /** 
     * Shows or hides the Minimap based on the given visible. This works independently 
     * of the current menu setting, so it should be used only when the Minimap is to 
     * be shown or hidden without regard for the user setting, e.g. hiding it in the lobby
     * or a report phase. 
     * Does not change the menu setting. 
     */
    void setMapVisible(boolean visible) {
        minimapW.setVisible(visible);
    }
    
    /** Shows or hides the Unit Display based on the current menu setting. */
    public void maybeShowUnitDisplay() {
        setUnitDisplayVisible(GUIP.getBoolean(GUIPreferences.SHOW_UNIT_DISPLAY));
    }

    /** 
     * Shows or hides the Unit Display based on the given visible. This works independently 
     * of the current menu setting, so it should be used only when the Unit Display is to 
     * be shown or hidden without regard for the user setting, e.g. hiding it in the lobby
     * or a report phase. 
     * Does not change the menu setting. 
     */
    public void setUnitDisplayVisible(boolean visible) {
        // If no unit displayed, select a unit so display can be safely shown
        // This can happen when using mouse button 4
        if (visible && (mechD.getCurrentEntity() == null)
                && (getClient() != null) && (getClient().getGame() != null)) {
            List<Entity> es = getClient().getGame().getEntitiesVector();
            if ((es != null) && !es.isEmpty()) {
                mechD.displayEntity(es.get(0));
            }
        }

        this.unitDetailPane.setVisible(visible);
    }

    private boolean fillPopup(Coords coords) {
        popup = new MapMenu(coords, client, curPanel, this);
        return popup.getHasMenu();
    }

    /**
     * Pops up a dialog box giving the player a series of choices that are not
     * mutually exclusive.
     *
     * @param title    the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or "No"
     *                 answer. The question will be split across multiple line on the
     *                 '\n' characters.
     * @param choices  the array of <code>String</code> choices that the player can
     *                 select from.
     * @return The array of the <code>int</code> indexes from the input array that match the
     * selected choices. If no choices were available, if the player did not select a choice, or if
     * the player canceled the choice, a <code>null</code> value is returned.
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
        JTextPane textArea = new JTextPane();
        ReportDisplay.setupStylesheet(textArea);
        BASE64ToolKit toolKit = new BASE64ToolKit();
        textArea.setEditorKit(toolKit);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textArea.setText("<pre>" + message + "</pre>");
        scrollPane.setPreferredSize(new Dimension(
                (int) (getSize().getWidth() / 1.5), (int) (getSize().getHeight() / 1.5)));
        JOptionPane.showMessageDialog(frame, scrollPane, title,
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Pops up a dialog box asking a yes/no question
     *
     * @param title    the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or "No"
     *                 answer. The question will be split across multiple line on the
     *                 '\n' characters.
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
     * @param question the <code>String</code> question that has a "Yes" or "No"
     *                 answer. The question will be split across multiple line on the
     *                 '\n' characters.
     * @return the <code>ConfirmDialog</code> containing the player's responses.
     *         The dialog will already have been shown to the player, and is
     *         only being returned so the calling function can see the answer to
     *         the question and the state of the "Show again?" question.
     */
    public ConfirmDialog doYesNoBotherDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question, true);
        confirm.setVisible(true);
        return confirm;
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     */
    protected void loadListFile() {
        loadListFile(client.getLocalPlayer());
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     *
     * @param c
     */
    protected void loadListFile(Client c) {
        loadListFile(c.getLocalPlayer());
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     *
     * @param player
     */
    public void loadListFile(Player player) {
        loadListFile(player, false);
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     *
     * @param player
     */
    protected void loadListFile(Player player, boolean reinforce) {
        boolean addedUnits = false;

        if (reinforce && (player.getTeam() == Player.TEAM_UNASSIGNED)) {
            String title = Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceTitle");
            String msg = Messages.getString("ClientGUI.openUnitListFileDialog.noReinforceMessage");
            JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.ERROR_MESSAGE, null);
            return;
        }
        // Build the "load unit" dialog, if necessary.
        if (dlgLoadList == null) {
            dlgLoadList = new JFileChooser(".");
            dlgLoadList.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
            dlgLoadList.setDialogTitle(Messages.getString("ClientGUI.openUnitListFileDialog.title"));
            dlgLoadList.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File dir) {
                    return (dir.getName().endsWith(".mul") || dir.isDirectory());
                }

                @Override
                public String getDescription() {
                    return "*.mul";
                }
            });
            // Default to the player's name.
            dlgLoadList.setSelectedFile(new File(player.getName() + ".mul"));
        }

        int returnVal = dlgLoadList.showOpenDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgLoadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        // Did the player select a file?
        File unitFile = dlgLoadList.getSelectedFile();
        if (unitFile != null) {
            try {
                // Read the units from the file.
                final Vector<Entity> loadedUnits = new MULParser(unitFile,
                        getClient().getGame().getOptions()).getEntities();

                // Add the units from the file.
                for (Entity entity : loadedUnits) {
                    entity.setOwner(player);
                    if (reinforce) {
                        entity.setDeployRound(client.getGame().getRoundCount() + 1);
                        entity.setGame(client.getGame());
                        // Set these to true, otherwise units reinforced in
                        // the movement turn are considered selectable
                        entity.setDone(true);
                        entity.setUnloaded(true);
                        if (entity instanceof IBomber) {
                            ((IBomber) entity).applyBombs();
                        }
                    }
                }

                if (!loadedUnits.isEmpty()) {
                    client.sendAddEntity(loadedUnits);
                    addedUnits = true;
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
                doAlertDialog(Messages.getString("ClientGUI.errorLoadingFile"), ex.getMessage());
            }
        }

        // If we've added reinforcements, then we need to set the round deployment up again.
        if (addedUnits && reinforce) {
            client.getGame().setupRoundDeployment();
            client.sendResetRoundDeployment();
        }
    }

    private boolean saveGame() {
        ignoreHotKeys = true;
        JFileChooser fc = new JFileChooser(MMConstants.SAVEGAME_DIR);
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGUI.FileSaveDialog.title"));

        int returnVal = fc.showSaveDialog(frame);
        ignoreHotKeys = false;
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return false;
        }
        if (fc.getSelectedFile() != null) {
            String file = fc.getSelectedFile().getName();
            // stupid hack to allow for savegames in folders with spaces in
            // the name
            String path = fc.getSelectedFile().getParentFile().getPath();
            path = path.replace(" ", "|");
            client.sendChat("/localsave " + file + " " + path);
            return true;
        }
        return false;
    }
    
    /** Developer Utility: Save game to quicksave.sav.gz without any prompts. */
    private boolean quickSaveGame() {
        client.sendChat("/localsave " + MMConstants.QUICKSAVE_FILE + " " + MMConstants.QUICKSAVE_PATH);
        return true;
    }

    /**
     * Allow the player to save a list of entities to a MegaMek Unit List file.
     * A "Save As" dialog will be displayed that allows the user to select the
     * file's name and directory. The player can later load this file to quickly
     * select the units for a new game. The file will record damage sustained,
     * non-standard munitions selected, and ammunition expended during the
     * course of the current engagement.
     *
     * @param unitList - the <code>Vector</code> of <code>Entity</code>s to be saved
     *                 to a file. If this value is <code>null</code> or empty, the
     *                 "Save As" dialog will not be displayed.
     */
    public void saveListFile(ArrayList<Entity> unitList) {
        saveListFile(unitList, client.getLocalPlayer().getName());
    }

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
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Mul Files", "mul");
            dlgSaveList.setFileFilter(filter);
        }
        // Default to the player's name.
        dlgSaveList.setSelectedFile(new File(filename + ".mul"));

        int returnVal = dlgSaveList.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgSaveList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        // Did the player select a file?
        File unitFile = dlgSaveList.getSelectedFile();
        if (unitFile != null) {
            if (!(unitFile.getName().toLowerCase().endsWith(".mul")
                    || unitFile.getName().toLowerCase().endsWith(".xml"))) {
                try {
                    unitFile = new File(unitFile.getCanonicalPath() + ".mul");
                } catch (Exception ignored) {
                    // nothing needs to be done here
                    return;
                }
            }

            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo(unitFile, unitList);
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
                doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
            }
        }
    }

    protected void saveVictoryList() {
        String filename = client.getLocalPlayer().getName();

        // Build the "save unit" dialog, if necessary.
        if (dlgSaveList == null) {
            dlgSaveList = new JFileChooser(".");
            dlgSaveList.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
            dlgSaveList.setDialogTitle(Messages.getString("ClientGUI.saveUnitListFileDialog.title"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Mul Files", "mul");
            dlgSaveList.setFileFilter(filter);
        }
        // Default to the player's name.
        dlgSaveList.setSelectedFile(new File(filename + ".mul"));

        int returnVal = dlgSaveList.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgSaveList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        // Did the player select a file?
        File unitFile = dlgSaveList.getSelectedFile();
        if (unitFile != null) {
            if (!(unitFile.getName().toLowerCase().endsWith(".mul")
                    || unitFile.getName().toLowerCase().endsWith(".xml"))) {
                try {
                    unitFile = new File(unitFile.getCanonicalPath() + ".mul");
                } catch (Exception ignored) {
                    // nothing needs to be done here
                    return;
                }
            }

            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo(unitFile, getClient());
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
                doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
            }
        }
    }

    /**
     * @return the frame this client is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Shows a dialog where the player can select the entity types
     * used in the LOS tool.
     */
    private void showLOSSettingDialog() {
        GUIPreferences gp = GUIPreferences.getInstance();
        LOSDialog ld = new LOSDialog(frame, gp.getMechInFirst(), gp.getMechInSecond());
        ignoreHotKeys = true;
        if (ld.showDialog().isConfirmed()) {
            gp.setMechInFirst(ld.getMechInFirst());
            gp.setMechInSecond(ld.getMechInSecond());
        }
        ignoreHotKeys = false;
    }

    /**
     * Loads a preview image of the unit into the BufferedPanel.
     *
     * @param bp
     * @param entity
     */
    public void loadPreviewImage(JLabel bp, Entity entity) {
        Player player = client.getGame().getPlayer(entity.getOwnerId());
        loadPreviewImage(bp, entity, player);
    }

    public void loadPreviewImage(JLabel bp, Entity entity, Player player) {
        final Camouflage camouflage = entity.getCamouflageOrElse(player.getCamouflage());
        Image icon = bv.getTilesetManager().loadPreviewImage(entity, camouflage, bp);
        bp.setIcon((icon == null) ? null : new ImageIcon(icon));
    }

    /**
     * Make a "bing" sound.
     */
    void bing() {
        if (!GUIPreferences.getInstance().getSoundMute() && (bingClip != null)) {
            bingClip.play();
        }
    }

    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePlayerChange(GamePlayerChangeEvent evt) {
             if (playerListDialog != null) {
                 playerListDialog.refreshPlayerList();
             }

             if ((curPanel instanceof ReportDisplay) && !client.getLocalPlayer().isDone()) {
                 ((ReportDisplay) curPanel).resetReadyButton();
             }
        }

        @Override
        public void gamePlayerDisconnected(GamePlayerDisconnectedEvent evt) {
            JOptionPane.showMessageDialog(frame,
                Messages.getString("ClientGUI.Disconnected.message"),
                Messages.getString("ClientGUI.Disconnected.title"),
                JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            die();
        }

        @Override
        public void gamePlayerChat(GamePlayerChatEvent e) {
            bing();
        }

        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            // This is a really lame place for this, but I couldn't find a
            // better one without making massive changes (which didn't seem
            // worth it for one little feature).
            if (bv.getLocalPlayer() != client.getLocalPlayer()) {
                // The adress based comparison is somewhat important.
                //  Use of the /reset command can cause the player to get reset,
                //  and the equals function of Player isn't powerful enough.
                bv.setLocalPlayer(client.getLocalPlayer());
            }
            // Make sure the ChatterBox starts out deactived.
            bv.setChatterBoxActive(false);            

            // Swap to this phase's panel.
            switchPanel(getClient().getGame().getPhase());

            menuBar.setPhase(getClient().getGame().getPhase());
            validate();
            cb.moveToEnd();
        }

        @Override
        public void gamePlayerConnected(GamePlayerConnectedEvent e) {
            System.err.println("gamePlayerConnected");
            System.err.flush();
            if (curPanel instanceof ReportDisplay) {
                ((ReportDisplay) curPanel).resetReadyButton();
                System.err.println("resetReadyButton");
                System.err.flush();
            }
        }

        @Override
        public void gameReport(GameReportEvent e) {
            // Normally the Report Display is updated when the panel is
            // switched during a phase change.
            // This update is for reports that get sent at odd times,
            // currently Tactical Genius reroll requests and when
            // a player wishes to continue moving after a fall.
            if (curPanel instanceof ReportDisplay) {
                // Tactical Genius
                ((ReportDisplay) curPanel).appendReportTab(getClient().phaseReport);
                ((ReportDisplay) curPanel).resetReadyButton();
                // Check if the player deserves an active reroll button
                // (possible, if he gets one which he didn't use, and his
                // opponent got and used one) and if so activates it.
                if (getClient().getGame().hasTacticalGenius(getClient().getLocalPlayer())) {
                    if (!((ReportDisplay) curPanel).hasRerolled()) {
                        ((ReportDisplay) curPanel).resetRerollButton();
                    }
                }
                // Show a popup to the players so that we know whats up!
                if (!(getClient() instanceof TestBot)) {
                    doAlertDialog("Tactical Genius Report", e.getReport());
                }
            } else {
                // Continued movement after getting up
                if (!(getClient() instanceof TestBot)) {
                    doAlertDialog("Movement Report", e.getReport());
                }
            }
        }

        @Override
        public void gameEnd(GameEndEvent e) {
            bv.clearMovementData();
            bv.clearFieldofF();
            for (Client client2 : getBots().values()) {
                client2.die();
            }
            getBots().clear();

            // Make a list of the player's living units.
            ArrayList<Entity> living = getClient().getGame().getPlayerEntities(getClient().getLocalPlayer(), false);

            // Be sure to include all units that have retreated.
            for (Enumeration<Entity> iter = getClient().getGame().getRetreatedEntities(); iter.hasMoreElements(); ) {
                Entity ent = iter.nextElement();
                if (ent.getOwnerId() == getClient().getLocalPlayer().getId()) {
                    living.add(ent);
                }
            }

            // Allow players to save their living units to a file.
            // Don't bother asking if none survived.
            if (!living.isEmpty() && doYesNoDialog(Messages.getString("ClientGUI.SaveUnitsDialog.title"),
                    Messages.getString("ClientGUI.SaveUnitsDialog.message"))) {
                // Allow the player to save the units to a file.
                saveVictoryList();
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
                String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
                File logDir = new File(sLogDir);
                if (!logDir.exists()) {
                    logDir.mkdir();
                }
                String fileName = "salvage.mul";
                if (PreferenceManager.getClientPreferences().stampFilenames()) {
                    fileName = StringUtil.addDateTimeStamp(fileName);
                }
                File unitFile = new File(sLogDir + File.separator + fileName);
                try {
                    // Save the destroyed entities to the file.
                    EntityListFile.saveTo(unitFile, destroyed);
                } catch (IOException ex) {
                    LogManager.getLogger().error("", ex);
                    doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
                }
            }

        }

        @Override
        public void gameSettingsChange(GameSettingsChangeEvent evt) {
            if ((gameOptionsDialog != null) && gameOptionsDialog.isVisible() &&
                    !evt.isMapSettingsOnlyChange()) {
                gameOptionsDialog.update(getClient().getGame().getOptions());
            }

            if (curPanel instanceof ChatLounge) {
                ChatLounge cl = (ChatLounge) curPanel;
                cl.updateMapSettings(getClient().getMapSettings());
            }
        }

        @Override
        public void gameMapQuery(GameMapQueryEvent evt) {

        }
        
        @Override
        public void gameClientFeedbackRequest(GameCFREvent evt) {
            Entity e = client.getGame().getEntity(evt.getEntityId());
            Object result;
            switch (evt.getCFRType()) {
                case CFR_DOMINO_EFFECT:
                    // If the client connects to a game as a bot, it's possible
                    // to have the bot respond AND have the client ask the
                    // player. This is bad, ignore this if the client is a bot
                    if (client instanceof BotClient) {
                        return;
                    }
                    MovePath stepForward = new MovePath(client.getGame(), e);
                    MovePath stepBackward = new MovePath(client.getGame(), e);
                    stepForward.addStep(MoveStepType.FORWARDS);
                    stepBackward.addStep(MoveStepType.BACKWARDS);
                    stepForward.compile(client.getGame(), e, false);
                    stepBackward.compile(client.getGame(), e, false);
                    
                    String title = Messages.getString("CFRDomino.Title");
                    String msg = Messages.getString("CFRDomino.Message", e.getDisplayName());
                    int choice;
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
                    } else { // No request is sent if both moves are illegal
                        options = new Object[2];
                        paths = new MovePath[2];
                        options[0] = Messages.getString("CFRDomino.Backward",
                                new Object[] { stepForward.getMpUsed() });
                        options[1] = Messages.getString("CFRDomino.NoAction");
                        paths[0] = stepBackward;
                        paths[1] = null;
                        optionType = JOptionPane.YES_NO_OPTION;
                    }            
                    choice = JOptionPane.showOptionDialog(frame, msg, title, 
                            optionType, JOptionPane.QUESTION_MESSAGE, null, 
                            options, options[0]);
                    // If they closed it, assume no action
                    if (choice == JOptionPane.CLOSED_OPTION) {
                        choice = options.length - 1;
                    }
                    client.sendDominoCFRResponse(paths[choice]);
                    break;
                case CFR_AMS_ASSIGN:
                    ArrayList<String> amsOptions = new ArrayList<>();
                    amsOptions.add("None");
                    for (WeaponAttackAction waa : evt.getWAAs()) {
                        Entity ae = waa.getEntity(client.getGame());
                        String waaMsg;
                        if (ae != null) {
                            Mounted weapon = ae.getEquipment(waa.getWeaponId());
                            waaMsg = weapon.getDesc() + " from "
                                    + ae.getDisplayName();
                        } else {
                            waaMsg = "Missiles from unknown attacker";
                        }
                        amsOptions.add(waaMsg);
                    }
                    
                    optionType = JOptionPane.OK_CANCEL_OPTION;
                    title = Messages.getString("CFRAMSAssign.Title",
                            new Object[] { e.getDisplayName() });
                    msg = Messages.getString("CFRAMSAssign.Message",
                            new Object[] { e.getDisplayName() });
                    result = JOptionPane.showInputDialog(frame, msg, title,
                            JOptionPane.QUESTION_MESSAGE, null, 
                           amsOptions.toArray(), null);
                    // If they closed it, assume no action
                    if ((result == null) || result.equals("None")) {
                        client.sendAMSAssignCFRResponse(null);
                    } else {
                        client.sendAMSAssignCFRResponse(
                                amsOptions.indexOf(result) - 1);                 
                    }
                    break;
                case CFR_APDS_ASSIGN:
                    ArrayList<String> apdsOptions = new ArrayList<>();
                    apdsOptions.add("None");
                    Iterator<Integer> distIt = evt.getApdsDists().iterator();
                    for (WeaponAttackAction waa : evt.getWAAs()) {
                        Entity ae = waa.getEntity(client.getGame());
                        int dist = distIt.next();
                        String waaMsg;
                        if (ae != null) {
                            Mounted weapon = ae.getEquipment(waa.getWeaponId());
                            waaMsg = weapon.getDesc() + " from "
                                    + ae.getDisplayName() + " (distance: "
                                    + dist + ")";
                        } else {
                            waaMsg = "Missiles from unknown attacker";
                        }
                        apdsOptions.add(waaMsg);
                    }

                    optionType = JOptionPane.OK_CANCEL_OPTION;
                    title = Messages.getString("CFRAPDSAssign.Title",
                            new Object[] { e.getDisplayName() });
                    msg = Messages.getString("CFRAPDSAssign.Message",
                            new Object[] { e.getDisplayName() });
                    result = JOptionPane.showInputDialog(frame, msg, title,
                            JOptionPane.QUESTION_MESSAGE, null,
                            apdsOptions.toArray(), null);
                    // If they closed it, assume no action
                    if ((result == null) || result.equals("None")) {
                        client.sendAPDSAssignCFRResponse(null);
                    } else {
                        client.sendAPDSAssignCFRResponse(
                                apdsOptions.indexOf(result) - 1);
                    }
                    break;
                case CFR_HIDDEN_PBS:
                    Entity attacker = client.getGame().getEntity(
                            evt.getEntityId());
                    Entity target = client.getGame().getEntity(
                            evt.getTargetId());
                    // Are we not the client handling the PBS?
                    if ((attacker == null) || (target == null)) {
                        if (curPanel instanceof StatusBarPhaseDisplay) {
                            ((StatusBarPhaseDisplay) curPanel)
                                    .setStatusBarText(Messages
                                            .getString("StatusBarPhaseDisplay.pointblankShot"));
                        }
                        return;
                    }
                    // If this is the client to handle the PBS, take care of it
                    bv.centerOnHex(attacker.getPosition());
                    bv.highlight(attacker.getPosition());
                    bv.select(target.getPosition());
                    bv.cursor(target.getPosition());
                    msg = Messages.getString("ClientGUI.PointBlankShot.Message",
                            target.getShortName(), attacker.getShortName());
                    title = Messages.getString("ClientGUI.PointBlankShot.Title");
                    // Ask whether the player wants to take a PBS or not
                    int pbsChoice = JOptionPane.showConfirmDialog(frame, msg,
                            title, JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    // Process the PBS - switch to PointblankShotDisplay
                    if (pbsChoice == JOptionPane.YES_OPTION) {
                        // Send a non-null response to indicate PBS is accepted
                        // This allows the servers to notify the clients,
                        // as they may be in for a wait
                        client.sendHiddenPBSCFRResponse(new Vector<>());
                        // Used to indicate it's this player's turn
                        setPointblankEID(evt.getEntityId());
                        // Switch to the right display
                        switchPanel(GamePhase.POINTBLANK_SHOT);
                        PointblankShotDisplay curDisp = ((PointblankShotDisplay) curPanel);
                        // Set targeting info
                        curDisp.beginMyTurn();
                        curDisp.selectEntity(evt.getEntityId());
                        curDisp.target(target);
                        bv.select(target.getPosition());
                    } else { // PBS declined
                        client.sendHiddenPBSCFRResponse(null);
                    }
                    break;
                case CFR_TELEGUIDED_TARGET:
                    List<Integer> targetIds = evt.getTelemissileTargetIds();
                    List<Integer> toHitValues = evt.getTmToHitValues();
                    List<String> targetDescriptions = new ArrayList<>();
                    for (int i = 0; i < targetIds.size(); i++) {
                        int id = targetIds.get(i);
                        int th = toHitValues.get(i);
                        Entity tgt = client.getGame().getEntity(id);
                        if (tgt != null) {
                            targetDescriptions.add(String.format(Messages.getString("TeleMissileTargetDialog.target"), tgt.getDisplayName(), th));
                        }
                    }
                    // Set up the selection pane
                    String i18nString = "TeleMissileTargetDialog.message";
                    msg = Messages.getString(i18nString);
                    i18nString = "TeleMissileTargetDialog.title";
                    title = Messages.getString(i18nString);
                    String input = (String) JOptionPane.showInputDialog(frame, msg,
                            title, JOptionPane.QUESTION_MESSAGE, null,
                            targetDescriptions.toArray(), targetDescriptions.get(0));
                    if (input != null) {
                        for (int i = 0; i < targetDescriptions.size(); i++) {
                            if (input.equals(targetDescriptions.get(i))) {
                                client.sendTelemissileTargetCFRResponse(i);
                                break;
                            }
                        }
                    } else {
                        // If input is null, as in the case of pressing the close or cancel buttons...
                        // Just pick the first target in the list, or server will be left waiting indefinitely.
                        client.sendTelemissileTargetCFRResponse(0);
                    }
                    break;
                case CFR_TAG_TARGET:
                    List<Integer> TAGTargets = evt.getTAGTargets();
                    List<Integer> TAGTargetTypes = evt.getTAGTargetTypes();
                    List<String> TAGTargetDescriptions = new ArrayList<>();
                    for (int i = 0; i < TAGTargets.size(); i++) {
                        int id = TAGTargets.get(i);
                        int nType = TAGTargetTypes.get(i);
                        Targetable tgt = client.getGame().getTarget(nType, id);
                        if (tgt != null) {
                            TAGTargetDescriptions.add(tgt.getDisplayName());
                        }
                    }
                    // Set up the selection pane
                    i18nString = "TAGTargetDialog.message";
                    msg = Messages.getString(i18nString);
                    i18nString = "TAGTargetDialog.title";
                    title = Messages.getString(i18nString);
                    input = (String) JOptionPane.showInputDialog(frame, msg,
                            title, JOptionPane.QUESTION_MESSAGE, null,
                            TAGTargetDescriptions.toArray(), TAGTargetDescriptions.get(0));
                    if (input != null) {
                        for (int i = 0; i < TAGTargetDescriptions.size(); i++) {
                            if (input.equals(TAGTargetDescriptions.get(i))) {
                                client.sendTAGTargetCFRResponse(i);
                                break;
                            }
                        }
                    } else {
                        //If input IS null, as in the case of pressing the close or cancel buttons...
                        //Just pick the first target in the list, or server will be left waiting indefinitely.
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

    public Map<String, Client> getBots() {
        return client.bots;
    }

    /**
     * @return Returns the selectedEntityNum.
     */
    public int getSelectedEntityNum() {
        return selectedEntityNum;
    }

    /**
     * @param selectedEntityNum The selectedEntityNum to set.
     */
    public void setSelectedEntityNum(int selectedEntityNum) {
        this.selectedEntityNum = selectedEntityNum;
        bv.selectEntity(client.getGame().getEntity(selectedEntityNum));
    }

    public RandomArmyDialog getRandomArmyDialog() {
        return randomArmyDialog;
    }

    public RandomNameDialog getRandomNameDialog() {
        return new RandomNameDialog(this);
    }

    /**
     * Checks to see if there is already a path and name stored; if not, calls
     * "save as"; otherwise, saves the board to the specified file.
     */
    private void boardSave() {
        if (curfileBoard == null) {
            boardSaveAs();
            return;
        }
        // save!
        try (OutputStream os = new FileOutputStream(curfileBoard)) {
            client.getGame().getBoard().save(os);
        } catch (IOException e) {
            LogManager.getLogger().error("Error opening file to save!", e);
        }
    }

    /**
     * Saves the board in PNG image format.
     */
    private void boardSaveImage(boolean ignoreUnits) {
        if (curfileBoardImage == null) {
            boardSaveAsImage(ignoreUnits);
            return;
        }
        JDialog waitD = new JDialog(frame, Messages.getString("BoardEditor.waitDialog.title"));
        waitD.add(new JLabel(Messages.getString("BoardEditor.waitDialog.message")));
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation(
                (frame.getSize().width / 2) - (waitD.getSize().width / 2), (frame
                .getSize().height
                / 2) - (waitD.getSize().height / 2));
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        try {
            ImageIO.write(bv.getEntireBoardImage(ignoreUnits, false), "png", curfileBoardImage);
        } catch (IOException e) {
            LogManager.getLogger().error("", e);
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file.
     */
    private void boardSaveAs() {
        JFileChooser fc = new JFileChooser("data" + File.separator + "boards");
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveBoardAs"));
        fc.setFileFilter(new BoardFileFilter());
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfileBoard = fc.getSelectedFile();

        // make sure the file ends in board
        if (!curfileBoard.getName().toLowerCase(Locale.ENGLISH).endsWith(".board")) {
            try {
                curfileBoard = new File(curfileBoard.getCanonicalPath() + ".board");
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        boardSave();
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file as an image. Useful for printing boards.
     */
    private void boardSaveAsImage(boolean ignoreUnits) {
        JFileChooser fc = new JFileChooser(".");
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveAsImage"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return (dir.getName().endsWith(".png") || dir.isDirectory());
            }

            @Override
            public String getDescription() {
                return ".png";
            }
        });
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfileBoardImage = fc.getSelectedFile();

        // make sure the file ends in png
        if (!curfileBoardImage.getName().toLowerCase(Locale.ENGLISH).endsWith(".png")) {
            try {
                curfileBoardImage = new File(curfileBoardImage.getCanonicalPath() + ".png");
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        boardSaveImage(ignoreUnits);
    }

    @Override
    public void hexMoused(BoardViewEvent b) {
        if (b.getType() == BoardViewEvent.BOARD_HEX_POPUP) {
            showBoardPopup(b.getCoords());
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
    public void secondLOSHex(BoardViewEvent b, Coords c) {
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
    
    /**
     * Returns true if a dialog is visible on top of the <code>ClientGUI</code>.
     * For example, the <code>MegaMekController</code> should ignore hotkeys
     * if there is a dialog, like the <code>CommonSettingsDialog</code>, open.
     * @return
     */
    public boolean shouldIgnoreHotKeys() {
        return ignoreHotKeys 
                || ((gameOptionsDialog != null) && gameOptionsDialog.isVisible())
                || ((about != null) && about.isVisible())
                || ((help != null) && help.isVisible())
                || ((setdlg != null) && setdlg.isVisible())
                || ((aw != null) && aw.isVisible());
    }

    @Override
    public void componentHidden(ComponentEvent evt) {

    }

    @Override
    public void componentMoved(ComponentEvent evt) {

    }

    @Override
    public void componentResized(ComponentEvent evt) {
        bv.setPreferredSize(getSize());        
    }

    @Override
    public void componentShown(ComponentEvent evt) {

    }

    void replacePlayer() {
        Set<Player> ghostPlayers = client.getGame().getPlayersVector().stream()
                .filter(Player::isGhost).collect(Collectors.toSet());
        if (ghostPlayers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No ghost players to replace.", "No Ghosts",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        var rpd = new ReplacePlayersDialog(frame, this);
        rpd.setVisible(true);
        if (rpd.getResult() == DialogResult.CANCELLED) {
            return;
        }

        AddBotUtil util = new AddBotUtil();
        Map<String, BehaviorSettings> newBotSettings = rpd.getNewBots();
        for (String player : newBotSettings.keySet()) {
            StringBuilder message = new StringBuilder();
            Princess princess = util.addBot(newBotSettings.get(player), player, 
                    client.getGame(), client.getHost(), client.getPort(), message);
            systemMessage(message.toString());
            // Make this princess a locally owned bot if in the lobby. This way it
            // can be configured, and it will faithfully press Done when the local player does.
            if ((princess != null) && client.getGame().getPhase() == GamePhase.LOUNGE) {
                getBots().put(player, princess);   
            } 
        }
    }
    
    /**
     * The ClientGUI is split into the main panel (view) at the top, which takes up the majority of
     * the view and the "current panel" which has different controls based on the phase.
     * 
     * @return the panel for the current phase
     */
    public JComponent getCurrentPanel() {
        return curPanel;
    }

    public boolean isProcessingPointblankShot() {
        return pointblankEID != Entity.NONE;
    }

    public void setPointblankEID(int eid) {
        this.pointblankEID = eid;
    }

    public int getPointblankEID() {
        return pointblankEID;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.MINIMAP_ENABLED)) {
            maybeShowMinimap();
        } else if (e.getName().equals(GUIPreferences.SHOW_UNIT_DISPLAY)) {
            maybeShowUnitDisplay();
        }
        
    }

}
