/*
 * Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.clientGUI.ClientGUI.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.KeyBindParser;
import megamek.common.enums.GamePhase;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.logging.MMLogger;

/**
 * The menu bar that is used across MM, i.e. in the main menu, the board editor and the lobby and game.
 */
public class CommonMenuBar extends JMenuBar implements ActionListener, IPreferenceChangeListener {
    private final static MMLogger logger = MMLogger.create(CommonMenuBar.class);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** True when this menu is attached to the board editor. */
    private final boolean isBoardEditor;

    /** True when this menu is attached to the game's main menu. */
    private final boolean isMainMenu;

    /** True when this menu is attached to a client (lobby or in game). */
    private final boolean isGame;

    /** The current phase of the game, if any. */
    private GamePhase phase = GamePhase.UNKNOWN;

    // The Game menu
    private final JMenuItem gameStart = new JMenuItem(getString("CommonMenuBar.fileGameStart"));
    private final JMenuItem gameLoad = new JMenuItem(getString("CommonMenuBar.fileGameLoad"));
    private final JMenuItem gameSave = new JMenuItem(getString("CommonMenuBar.fileGameSave"));
    private final JMenuItem gameQSave = new JMenuItem(getString("CommonMenuBar.fileGameQuickSave"));
    private final JMenuItem gameQLoad = new JMenuItem(getString("CommonMenuBar.fileGameQuickLoad"));
    private final JMenuItem gameSaveServer = new JMenuItem(getString("CommonMenuBar.fileGameSaveServer"));
    private final JMenuItem gameConnect = new JMenuItem(getString("CommonMenuBar.fileGameConnect"));
    private final JCheckBoxMenuItem gameRoundReport = new JCheckBoxMenuItem(getString("CommonMenuBar.viewRoundReport"));
    private final JMenuItem gameEditBots = new JMenuItem(getString("CommonMenuBar.editBots"));
    private final JCheckBoxMenuItem gamePlayerList = new JCheckBoxMenuItem(getString("CommonMenuBar.viewPlayerList"));
    private final JMenuItem gameGameOptions = new JMenuItem(getString("CommonMenuBar.viewGameOptions"));
    private final JMenuItem gamePlayerSettings = new JMenuItem(getString("CommonMenuBar.viewPlayerSettings"));

    // The Units menu
    private final JMenuItem fileUnitsReinforce = new JMenuItem(getString("CommonMenuBar.fileUnitsReinforce"));
    private final JMenuItem fileUnitsReinforceRAT = new JMenuItem(getString("CommonMenuBar.fileUnitsReinforceRAT"));
    private final JMenuItem fileCreateRandom = new JMenuItem("Create Random Army");
    private final JMenuItem fileUnitsPaste = new JMenuItem(getString("CommonMenuBar.fileUnitsPaste"));
    private final JMenuItem fileUnitsCopy = new JMenuItem(getString("CommonMenuBar.fileUnitsCopy"));
    private final JMenuItem fileUnitsSave = new JMenuItem(getString("CommonMenuBar.fileUnitsSave"));
    private final JMenuItem fileUnitsBrowse = new JMenuItem(getString("CommonMenuBar.fileUnitsBrowse"));

    // The Board menu
    private final JMenuItem boardNew = new JMenuItem(getString("CommonMenuBar.fileBoardNew"));
    private final JMenuItem boardOpen = new JMenuItem(getString("CommonMenuBar.fileBoardOpen"));
    private final JMenu boardRecent = new JMenu(getString("CommonMenuBar.fileBoardRecent"));
    private final JMenuItem boardSave = new JMenuItem(getString("CommonMenuBar.fileBoardSave"));
    private final JMenuItem boardSaveAs = new JMenuItem(getString("CommonMenuBar.fileBoardSaveAs"));
    private final JMenuItem boardSaveAsImage = new JMenuItem(getString("CommonMenuBar.fileBoardSaveAsImage"));
    private final JMenuItem boardSaveAsImageUnits = new JMenuItem(getString("CommonMenuBar.fileBoardSaveAsImageUnits"));
    private final JCheckBoxMenuItem boardTraceOverlay = new JCheckBoxMenuItem(getString("CommonMenuBar.boardTraceOverlay"));
    private final JMenuItem boardResize = new JMenuItem(getString("CommonMenuBar.boardResize"));
    private final JMenuItem boardValidate = new JMenuItem(getString("CommonMenuBar.boardValidate"));
    private final JMenuItem boardRunBoardTagger = new JMenuItem(getString("CommonMenuBar.boardRunBoardTagger"));
    private final JMenuItem boardSourceFile = new JMenuItem(getString("CommonMenuBar.boardSourceFile"));
    private final JMenuItem boardUndo = new JMenuItem(getString("CommonMenuBar.boardUndo"));
    private final JMenuItem boardRedo = new JMenuItem(getString("CommonMenuBar.boardRedo"));
    private final JMenuItem boardChangeTheme = new JMenuItem(getString("CommonMenuBar.viewChangeTheme"));
    private final JMenuItem boardRaise = new JMenuItem(getString("CommonMenuBar.boardRaise"));
    private final JMenuItem boardClear = new JMenuItem(getString("CommonMenuBar.boardClear"));
    private final JMenuItem boardFlatten = new JMenuItem(getString("CommonMenuBar.boardFlatten"));
    private final JMenuItem boardFlood = new JMenuItem(getString("CommonMenuBar.boardFlood"));
    private JMenu boardRemove = new JMenu(getString("CommonMenuBar.boardRemove"));
    private final JMenuItem boardRemoveForests = new JMenuItem(getString("CommonMenuBar.boardRemoveForests"));
    private final JMenuItem boardRemoveWater = new JMenuItem(getString("CommonMenuBar.boardRemoveWater"));
    private final JMenuItem boardRemoveRoads = new JMenuItem(getString("CommonMenuBar.boardRemoveRoads"));
    private final JMenuItem boardRemoveBuildings = new JMenuItem(getString("CommonMenuBar.boardRemoveBuildings"));

    // The View menu
    private final JCheckBoxMenuItem viewMinimap = new JCheckBoxMenuItem(getString("CommonMenuBar.viewMinimap"));
    private final JCheckBoxMenuItem viewMekDisplay = new JCheckBoxMenuItem(getString("CommonMenuBar.viewMekDisplay"));
    private final JCheckBoxMenuItem viewForceDisplay = new JCheckBoxMenuItem(getString("CommonMenuBar.viewForceDisplay"));
    private final JMenuItem viewNovaNetworks = new JMenuItem(getString("CommonMenuBar.viewNovaNetworks"));
    private final JMenuItem viewAccessibilityWindow = new JMenuItem(getString("CommonMenuBar.viewAccessibilityWindow"));
    private final JCheckBoxMenuItem viewKeybindsOverlay = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewKeyboardShortcuts"));
    private final JCheckBoxMenuItem viewPlanetaryConditionsOverlay = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewPlanetaryConditions"));
    private final JMenuItem viewZoomIn = new JMenuItem(getString("CommonMenuBar.viewZoomIn"));
    private final JMenuItem viewZoomOut = new JMenuItem(getString("CommonMenuBar.viewZoomOut"));
    private final JMenuItem viewZoomOverviewToggle = new JMenuItem(getString("CommonMenuBar.viewZoomOverviewToggle"));
    private final JMenuItem viewLabels = new JMenuItem(getString("CommonMenuBar.viewLabels"));
    private final JCheckBoxMenuItem viewBotCommands = new JCheckBoxMenuItem(getString("CommonMenuBar.viewBotCommands"));
    private final JCheckBoxMenuItem toggleIsometric = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleIsometric"));
    private final JCheckBoxMenuItem toggleHexCoords = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleHexCoords"));
    private final JCheckBoxMenuItem toggleSensorRange = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleSensorRange"));
    private final JCheckBoxMenuItem toggleFieldOfFire = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleFieldOfFire"));
    private final JMenuItem toggleFleeZone = new JMenuItem(getString("CommonMenuBar.viewToggleFleeZone"));
    private final JCheckBoxMenuItem toggleFovHighlight = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleFovHighlight"));
    private final JCheckBoxMenuItem toggleFovDarken = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleFovDarken"));
    private final JCheckBoxMenuItem toggleFovSpotting = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleFovSpotting"));
    private final JCheckBoxMenuItem toggleFiringSolutions = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleFiringSolutions"));
    private final JCheckBoxMenuItem toggleCFWarning = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.viewToggleCFWarning"));
    private final JCheckBoxMenuItem viewMovementEnvelope = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.movementEnvelope"));
    private final JCheckBoxMenuItem viewTurnDetailsOverlay = new JCheckBoxMenuItem(getString(
          "CommonMenuBar.turnDetailsOverlay"));
    private final JMenuItem viewMovModEnvelope = new JMenuItem(getString("CommonMenuBar.movementModEnvelope"));
    private final JMenuItem viewLOSSetting = new JMenuItem(getString("CommonMenuBar.viewLOSSetting"));
    private final JCheckBoxMenuItem viewUnitOverview = new JCheckBoxMenuItem(getString("CommonMenuBar.viewUnitOverview"));
    private final JMenuItem viewClientSettings = new JMenuItem(getString("CommonMenuBar.viewClientSettings"));
    private final JMenuItem viewIncGUIScale = new JMenuItem(getString("CommonMenuBar.viewIncGUIScale"));
    private final JMenuItem viewDecGUIScale = new JMenuItem(getString("CommonMenuBar.viewDecGUIScale"));

    // The Firing Action menu
    private final JMenuItem fireSaveWeaponOrder = new JMenuItem(getString("CommonMenuBar.fireSaveWeaponOrder"));

    /**
     * Contains all ActionListeners that have registered themselves with this menu bar.
     */
    private final List<ActionListener> actionListeners = new ArrayList<>();

    /** Maps the Action Command to the respective MenuItem. */
    private final Map<String, JMenuItem> itemMap = new HashMap<>();

    public static CommonMenuBar getMenuBarForGame() {
        var menuBar = new CommonMenuBar(false, true, false);
        menuBar.updateEnabledStates();
        return menuBar;
    }

    public static CommonMenuBar getMenuBarForBoardEditor() {
        var menuBar = new CommonMenuBar(false, false, true);
        menuBar.updateEnabledStates();
        return menuBar;
    }

    public static CommonMenuBar getMenuBarForMainMenu() {
        var menuBar = new CommonMenuBar(true, false, false);
        menuBar.updateEnabledStates();
        return menuBar;
    }

    /** Creates the common MegaMek menu bar. */
    private CommonMenuBar(boolean isMainMenu, boolean isGame, boolean isBoardEditor) {
        this.isMainMenu = isMainMenu;
        this.isGame = isGame;
        this.isBoardEditor = isBoardEditor;
        // Create the Game menu
        JMenu menu = new JMenu(Messages.getString("CommonMenuBar.FileMenu"));
        menu.setMnemonic(VK_F);
        add(menu);
        initMenuItem(gameStart, menu, FILE_GAME_NEW);
        initMenuItem(gameLoad, menu, FILE_GAME_LOAD, VK_L);
        initMenuItem(gameSave, menu, FILE_GAME_SAVE, VK_S);
        initMenuItem(gameQSave, menu, FILE_GAME_QUICK_SAVE);
        initMenuItem(gameQLoad, menu, FILE_GAME_QUICK_LOAD);
        initMenuItem(gameSaveServer, menu, FILE_GAME_SAVE_SERVER);
        initMenuItem(gameConnect, menu, FILE_GAME_CONNECT);

        // Create the Unit List sub-menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.GameMenu"));
        add(menu);
        menu.setMnemonic(VK_G);

        initMenuItem(gameEditBots, menu, FILE_GAME_EDIT_BOTS, VK_R);
        menu.addSeparator();

        initMenuItem(gameGameOptions, menu, VIEW_GAME_OPTIONS, VK_O);
        initMenuItem(gamePlayerSettings, menu, VIEW_PLAYER_SETTINGS);
        initMenuItem(fileUnitsCopy, menu, FILE_UNITS_COPY);
        fileUnitsCopy.setAccelerator(KeyStroke.getKeyStroke(VK_C, CTRL_DOWN_MASK));
        initMenuItem(fileUnitsPaste, menu, FILE_UNITS_PASTE);
        fileUnitsPaste.setAccelerator(KeyStroke.getKeyStroke(VK_V, CTRL_DOWN_MASK));
        initMenuItem(fileUnitsReinforce, menu, FILE_UNITS_REINFORCE);
        initMenuItem(isMainMenu ? fileCreateRandom : fileUnitsReinforceRAT, menu, FILE_UNITS_REINFORCE_RAT);
        initMenuItem(fileUnitsSave, menu, FILE_UNITS_SAVE);
        menu.addSeparator();

        JMenuItem fileRefreshCache = new JMenuItem(getString("CommonMenuBar.fileUnitsRefreshUnitCache"));
        initMenuItem(fileRefreshCache, menu, FILE_REFRESH_CACHE);
        initMenuItem(fileUnitsBrowse, menu, FILE_UNITS_BROWSE);
        // The accelerator overlaps with that for changing label style, but they are never active at the same time
        fileUnitsBrowse.setAccelerator(KeyStroke.getKeyStroke(VK_B, CTRL_DOWN_MASK));
        menu.addSeparator();

        initMenuItem(fireSaveWeaponOrder, menu, FIRE_SAVE_WEAPON_ORDER);

        // Create the Board sub-menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.BoardMenu"));
        menu.setMnemonic(VK_B);
        add(menu);
        initMenuItem(boardNew, menu, BOARD_NEW);
        initMenuItem(boardOpen, menu, BOARD_OPEN, VK_O);
        initMenuItem(boardRecent, menu, BOARD_OPEN, VK_O);
        initializeRecentBoardsMenu();
        initMenuItem(boardSave, menu, BOARD_SAVE);
        initMenuItem(boardSaveAs, menu, BOARD_SAVE_AS);
        menu.addSeparator();

        initMenuItem(boardValidate, menu, BOARD_VALIDATE);
        initMenuItem(boardSourceFile, menu, BOARD_SOURCE_FILE);
        initMenuItem(boardRunBoardTagger, menu, BOARD_RUN_BOARD_TAGGER);
        menu.addSeparator();

        initMenuItem(boardSaveAsImage, menu, BOARD_SAVE_AS_IMAGE);
        boardSaveAsImage.setToolTipText(Messages.getString("CommonMenuBar.fileBoardSaveAsImage.tooltip"));
        initMenuItem(boardSaveAsImageUnits, menu, BOARD_SAVE_AS_IMAGE_UNITS);
        boardSaveAsImageUnits.setToolTipText(Messages.getString("CommonMenuBar.fileBoardSaveAsImageUnits.tooltip"));
        initMenuItem(boardTraceOverlay, menu, VIEW_TRACE_OVERLAY, GUIP.getShowTraceOverlay());
        menu.addSeparator();

        initMenuItem(boardUndo, menu, BOARD_UNDO);
        initMenuItem(boardRedo, menu, BOARD_REDO);
        menu.addSeparator();

        initMenuItem(boardResize, menu, BOARD_RESIZE);
        initMenuItem(boardChangeTheme, menu, VIEW_CHANGE_THEME);
        initMenuItem(boardRaise, menu, BOARD_RAISE);
        initMenuItem(boardClear, menu, BOARD_CLEAR);
        initMenuItem(boardFlatten, menu, BOARD_FLATTEN);
        initMenuItem(boardFlood, menu, BOARD_FLOOD);
        boardRemove = new JMenu(Messages.getString("CommonMenuBar.boardRemove"));
        menu.add(boardRemove);
        initMenuItem(boardRemoveForests, boardRemove, BOARD_REMOVE_FORESTS);
        initMenuItem(boardRemoveWater, boardRemove, BOARD_REMOVE_WATER);
        initMenuItem(boardRemoveRoads, boardRemove, BOARD_REMOVE_ROADS);
        initMenuItem(boardRemoveBuildings, boardRemove, BOARD_REMOVE_BUILDINGS);

        // Create the view menu.
        menu = new JMenu(Messages.getString("CommonMenuBar.ViewMenu"));
        menu.setMnemonic(VK_V);
        add(menu);
        initMenuItem(viewClientSettings, menu, VIEW_CLIENT_SETTINGS, VK_S);
        initMenuItem(viewIncGUIScale, menu, VIEW_INC_GUI_SCALE);
        initMenuItem(viewDecGUIScale, menu, VIEW_DEC_GUI_SCALE);
        menu.addSeparator();

        GUIP.setUnitDisplayEnabled(false);
        initMenuItem(viewMekDisplay, menu, VIEW_UNIT_DISPLAY, VK_D, GUIP.getUnitDisplayEnabled());
        GUIP.setMinimapEnabled(false);
        initMenuItem(viewMinimap, menu, VIEW_MINI_MAP, VK_M, GUIP.getMinimapEnabled());
        GUIP.setMiniReportEnabled(false);
        initMenuItem(gameRoundReport, menu, VIEW_ROUND_REPORT, GUIP.getMiniReportEnabled());
        GUIP.setPlayerListEnabled(false);
        initMenuItem(gamePlayerList, menu, VIEW_PLAYER_LIST, GUIP.getPlayerListEnabled());
        GUIP.setForceDisplayEnabled(false);
        initMenuItem(viewForceDisplay, menu, VIEW_FORCE_DISPLAY, GUIP.getForceDisplayEnabled());
        initMenuItem(viewNovaNetworks, menu, VIEW_NOVA_NETWORKS);
        GUIP.setBotCommandsEnabled(false);
        initMenuItem(viewBotCommands, menu, VIEW_BOT_COMMANDS, VK_G, GUIP.getBotCommandsEnabled());
        menu.addSeparator();

        initMenuItem(viewKeybindsOverlay, menu, VIEW_KEYBINDS_OVERLAY, GUIP.getShowKeybindsOverlay());
        initMenuItem(viewPlanetaryConditionsOverlay,
              menu,
              VIEW_PLANETARY_CONDITIONS_OVERLAY,
              GUIP.getShowPlanetaryConditionsOverlay());
        initMenuItem(viewTurnDetailsOverlay, menu, VIEW_TURN_DETAILS_OVERLAY, GUIP.getTurnDetailsOverlay());
        initMenuItem(viewUnitOverview, menu, VIEW_UNIT_OVERVIEW, GUIP.getShowUnitOverview());
        menu.addSeparator();

        JMenuItem viewResetWindowPositions = new JMenuItem(getString("CommonMenuBar.viewResetWindowPos"));
        initMenuItem(viewResetWindowPositions, menu, VIEW_RESET_WINDOW_POSITIONS);
        initMenuItem(viewAccessibilityWindow, menu, VIEW_ACCESSIBILITY_WINDOW, VK_A);
        viewAccessibilityWindow.setMnemonic(KeyEvent.VK_A);
        menu.addSeparator();

        initMenuItem(viewZoomIn, menu, VIEW_ZOOM_IN);
        initMenuItem(viewZoomOut, menu, VIEW_ZOOM_OUT);
        initMenuItem(viewZoomOverviewToggle, menu, VIEW_ZOOM_OVERVIEW_TOGGLE);
        initMenuItem(toggleIsometric, menu, VIEW_TOGGLE_ISOMETRIC, VK_T, GUIP.getIsometricEnabled());
        initMenuItem(toggleHexCoords, menu, VIEW_TOGGLE_HEX_COORDS, VK_G, GUIP.getCoordsEnabled());
        initMenuItem(viewLabels, menu, VIEW_LABELS);
        menu.addSeparator();

        initMenuItem(toggleFovDarken, menu, VIEW_TOGGLE_FOV_DARKEN, GUIP.getFovDarken());
        toggleFovDarken.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFovDarkenTooltip"));
        initMenuItem(viewLOSSetting, menu, VIEW_LOS_SETTING);
        initMenuItem(toggleFovHighlight, menu, VIEW_TOGGLE_FOV_HIGHLIGHT, GUIP.getFovHighlight());
        initMenuItem(toggleFovSpotting, menu, VIEW_TOGGLE_FOV_SPOTTING, GUIP.getFovSpottingMode());
        toggleFovSpotting.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFovSpottingTooltip"));
        initMenuItem(viewMovementEnvelope, menu, VIEW_MOVE_ENV, GUIP.getMoveEnvelope());
        initMenuItem(viewMovModEnvelope, menu, VIEW_MOVE_MOD_ENV);
        menu.addSeparator();

        initMenuItem(toggleSensorRange, menu, VIEW_TOGGLE_SENSOR_RANGE, GUIP.getShowSensorRange());
        toggleSensorRange.setToolTipText(Messages.getString("CommonMenuBar.viewToggleSensorRangeToolTip"));
        initMenuItem(toggleFieldOfFire, menu, VIEW_TOGGLE_FIELD_OF_FIRE, GUIP.getShowFieldOfFire());
        toggleFieldOfFire.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFieldOfFireToolTip"));
        initMenuItem(toggleFleeZone, menu, VIEW_TOGGLE_FLEE_ZONE);
        toggleFleeZone.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFleeZoneToolTip"));
        initMenuItem(toggleFiringSolutions, menu, VIEW_TOGGLE_FIRING_SOLUTIONS, GUIP.getShowFiringSolutions());
        toggleFiringSolutions.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFiringSolutionsToolTip"));

        initMenuItem(toggleCFWarning, menu, VIEW_TOGGLE_CF_WARNING, GUIP.getShowCFWarnings());
        toggleCFWarning.setToolTipText(Messages.getString("CommonMenuBar.viewToggleCFWarningToolTip"));

        // Create the Help menu
        menu = new JMenu(Messages.getString("CommonMenuBar.HelpMenu"));
        menu.setMnemonic(VK_H);
        add(menu);
        JMenuItem helpResetNags = new JMenuItem(getString("CommonMenuBar.helpResetNags"));
        initMenuItem(helpResetNags, menu, HELP_RESET_NAGS);
        // The Help menu
        JMenuItem helpContents = new JMenuItem(getString("CommonMenuBar.helpContents"));
        initMenuItem(helpContents, menu, HELP_CONTENTS);
        JMenuItem helpSkinning = new JMenuItem(getString("CommonMenuBar.helpSkinning"));
        initMenuItem(helpSkinning, menu, HELP_SKINNING);
        menu.addSeparator();
        JMenuItem helpAbout = new JMenuItem(getString("CommonMenuBar.helpAbout"));
        initMenuItem(helpAbout, menu, HELP_ABOUT);

        setKeyBinds();
        GUIP.addPreferenceChangeListener(this);
        KeyBindParser.addPreferenceChangeListener(this);
        RecentBoardList.addListener(this);
    }

    /** Sets/updates the accelerators from the KeyCommandBinds preferences. */
    private void setKeyBinds() {
        toggleSensorRange.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.SENSOR_RANGE));
        toggleFovSpotting.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.FOV_SPOTTING));
        toggleFieldOfFire.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.FIELD_FIRE));
        toggleIsometric.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.TOGGLE_ISO));
        viewMovementEnvelope.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.MOVE_ENVELOPE));
        viewMinimap.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.MINIMAP));
        viewLabels.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.DRAW_LABELS));
        toggleHexCoords.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.HEX_COORDS));
        viewMovModEnvelope.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.MOD_ENVELOPE));
        viewKeybindsOverlay.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.KEY_BINDS));
        viewPlanetaryConditionsOverlay.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.PLANETARY_CONDITIONS));
        viewTurnDetailsOverlay.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.TURN_DETAILS));
        viewForceDisplay.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.FORCE_DISPLAY));
        viewMekDisplay.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.UNIT_DISPLAY));
        viewUnitOverview.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.UNIT_OVERVIEW));
        viewLOSSetting.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.LOS_SETTING));
        viewIncGUIScale.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.INC_GUI_SCALE));
        viewDecGUIScale.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.DEC_GUI_SCALE));
        viewClientSettings.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.CLIENT_SETTINGS));
        boardUndo.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.UNDO));
        boardRedo.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.REDO));
        gameRoundReport.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.ROUND_REPORT));
        viewZoomIn.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.ZOOM_IN));
        viewZoomOut.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.ZOOM_OUT));
        viewZoomOverviewToggle.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.ZOOM_OVERVIEW_TOGGLE));
        gameQLoad.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.QUICK_LOAD));
        gameQSave.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.QUICK_SAVE));
        gameSave.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.LOCAL_SAVE));
        gameLoad.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.LOCAL_LOAD));
        gameEditBots.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.REPLACE_PLAYER));
        viewBotCommands.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.BOT_COMMANDS));
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        // Changes that are independent of the current state of MM
        // BoardView and others may listen to PreferenceChanges to detect these
        switch (event.getActionCommand()) {
            case ClientGUI.VIEW_INC_GUI_SCALE -> {
                float guiScale = GUIP.getGUIScale();
                if (guiScale < ClientGUI.MAX_GUI_SCALE) {
                    GUIP.setValue(GUIPreferences.GUI_SCALE, guiScale + 0.1);
                }
            }
            case ClientGUI.VIEW_DEC_GUI_SCALE -> {
                float guiScale = GUIP.getGUIScale();
                if (guiScale > ClientGUI.MIN_GUI_SCALE) {
                    GUIP.setValue(GUIPreferences.GUI_SCALE, guiScale - 0.1);
                }
            }
            case ClientGUI.VIEW_PLANETARY_CONDITIONS_OVERLAY -> GUIP.togglePlanetaryConditionsOverlay();
            case VIEW_TRACE_OVERLAY -> GUIP.toggleTraceConditionsOverlay();
            case VIEW_KEYBINDS_OVERLAY -> GUIP.toggleKeybindsOverlay();
            case VIEW_TURN_DETAILS_OVERLAY -> GUIP.setTurnDetailsOverlay(!GUIP.getTurnDetailsOverlay());
            case ClientGUI.VIEW_LABELS -> GUIP.setUnitLabelStyle(GUIP.getUnitLabelStyle().next());
            case VIEW_UNIT_OVERVIEW -> GUIP.setShowUnitOverview(!GUIP.getShowUnitOverview());
            case ClientGUI.HELP_RESET_NAGS ->
                  MegaMek.getMMOptions().setNagDialogIgnore(MMConstants.NAG_BOT_README, false);
        }

        // Pass the action on to each of our listeners.
        // This is a source of ConcurrentModificationException errors if dialogs are
        // open during events
        // but closed after handling them; catching the CME does not lead to later
        // issues.
        try {
            actionListeners.forEach(l -> l.actionPerformed(event));
        } catch (ConcurrentModificationException e) {
            logger.warn(e, "Probable dialog open during Round Report handling");
            logger.info("Event causing this issue: " + event.getActionCommand());
        }
    }

    /**
     * Register an object that wishes to be alerted when an item on this menu bar has been selected.
     * <p>
     * Please note, the ActionCommand property of the action event will inform the listener as to which menu item that
     * has been selected. Not all listeners will be interested in all menu items.
     *
     * @param listener - the <code>ActionListener</code> that wants to register itself.
     */
    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    /**
     * Remove an object that was being alerted when an item on this menu bar was selected.
     *
     * @param listener - the <code>ActionListener</code> that wants to be removed.
     */
    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    /**
     * Manages the enabled states of the menu items depending on where the menu is employed.
     */
    private synchronized void updateEnabledStates() {
        boolean isLobby = isGame && phase.isLounge();
        boolean isInGame = isGame && phase.isDuringOrAfter(GamePhase.DEPLOYMENT);
        boolean isInGameBoardView = isInGame && phase.isOnMap();
        boolean isBoardView = isInGameBoardView || isBoardEditor;
        boolean canSave = !phase.isUnknown()
              && !phase.isSelection()
              && !phase.isExchange()
              && !phase.isVictory()
              && !phase.isStartingScenario();
        boolean isNotVictory = !phase.isVictory();

        viewAccessibilityWindow.setEnabled(false);

        boardChangeTheme.setEnabled(isBoardEditor);
        boardUndo.setEnabled(isBoardEditor);
        boardRedo.setEnabled(isBoardEditor);
        boardRaise.setEnabled(isBoardEditor);
        boardClear.setEnabled(isBoardEditor);
        boardFlood.setEnabled(isBoardEditor);
        boardRemove.setEnabled(isBoardEditor);
        boardRemoveBuildings.setEnabled(isBoardEditor);
        boardRemoveWater.setEnabled(isBoardEditor);
        boardRemoveRoads.setEnabled(isBoardEditor);
        boardRemoveForests.setEnabled(isBoardEditor);
        boardFlatten.setEnabled(isBoardEditor);
        boardValidate.setEnabled(isBoardEditor);
        boardRunBoardTagger.setEnabled(isBoardEditor);
        boardResize.setEnabled(isBoardEditor);
        boardSourceFile.setEnabled(isBoardEditor);
        gameQLoad.setEnabled(isMainMenu || isLobby);
        gameLoad.setEnabled(isMainMenu || isLobby);
        gameStart.setEnabled(isMainMenu);
        gameConnect.setEnabled(isMainMenu);

        gameSave.setEnabled(isLobby || (isInGame && canSave));
        gameSaveServer.setEnabled(isLobby || (isInGame && canSave));
        gameQSave.setEnabled(isLobby || (isInGame && canSave));
        gameEditBots.setEnabled(isLobby || (isInGame && canSave));
        boardSave.setEnabled(isBoardEditor);
        boardSaveAs.setEnabled(isBoardEditor || isInGame); // TODO: should work in the lobby
        boardSaveAsImage.setEnabled(isBoardEditor || isInGame); // TODO: should work in the lobby
        boardNew.setEnabled(isBoardEditor || isMainMenu);
        boardOpen.setEnabled(isBoardEditor || isMainMenu);
        boardRecent.setEnabled((isBoardEditor || isMainMenu) && !RecentBoardList.getRecentBoards().isEmpty());
        boardTraceOverlay.setEnabled(isBoardEditor);
        fileUnitsPaste.setEnabled(isLobby);
        fileUnitsCopy.setEnabled(isLobby);
        fileUnitsReinforce.setEnabled((isInGame) && isNotVictory);
        fileUnitsReinforceRAT.setEnabled((isMainMenu || isLobby || isInGame) && isNotVictory);
        fileUnitsSave.setEnabled(isLobby || (isInGame && canSave));
        fileUnitsBrowse.setEnabled(isMainMenu);
        boardSaveAsImageUnits.setEnabled(isInGame);
        gamePlayerList.setEnabled(isInGame);
        viewLabels.setEnabled(isInGameBoardView);

        gameGameOptions.setEnabled(isInGame || isLobby);
        gamePlayerSettings.setEnabled(isInGame);

        viewMinimap.setEnabled(isBoardView);
        viewZoomIn.setEnabled(isBoardView);
        viewZoomOut.setEnabled(isBoardView);
        viewZoomOverviewToggle.setEnabled(isBoardView);
        toggleIsometric.setEnabled(isBoardView);
        viewKeybindsOverlay.setEnabled(isBoardView);
        viewPlanetaryConditionsOverlay.setEnabled(isInGameBoardView);
        toggleHexCoords.setEnabled(isBoardView);

        viewLOSSetting.setEnabled(isInGameBoardView);
        viewUnitOverview.setEnabled(isInGameBoardView);
        toggleSensorRange.setEnabled(isInGameBoardView);
        toggleFieldOfFire.setEnabled(isInGameBoardView);
        toggleFleeZone.setEnabled(isInGameBoardView);
        toggleFovHighlight.setEnabled(isInGameBoardView);
        toggleFovDarken.setEnabled(isInGameBoardView);
        toggleFovSpotting.setEnabled(isInGameBoardView);
        toggleFiringSolutions.setEnabled(isInGameBoardView);
        toggleCFWarning.setEnabled(isInGameBoardView);
        viewMovementEnvelope.setEnabled(isInGameBoardView);
        viewTurnDetailsOverlay.setEnabled(isInGameBoardView);
        viewMovModEnvelope.setEnabled(isInGameBoardView);
        gameRoundReport.setEnabled(isInGame);
        viewMekDisplay.setEnabled(isInGameBoardView);
        viewForceDisplay.setEnabled(isInGameBoardView);
        fireSaveWeaponOrder.setEnabled(isInGameBoardView);
        viewBotCommands.setEnabled(isInGame);
    }

    /**
     * Identify to the menu bar which phase is currently in progress
     *
     * @param current - the <code>int</code> value of the current phase (the valid values for this argument are defined
     *                as constants in the
     *                <code>Game</code> class).
     */
    public synchronized void setPhase(GamePhase current) {
        phase = current;
        updateEnabledStates();
    }

    public synchronized void setEnabled(String command, boolean enabled) {
        if (itemMap.containsKey(command)) {
            itemMap.get(command).setEnabled(enabled);
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Adapt the menu checkboxes to a new state where necessary
        switch (e.getName()) {
            case GUIPreferences.USE_ISOMETRIC -> toggleIsometric.setSelected((Boolean) e.getNewValue());
            case GUIPreferences.SHOW_FIELD_OF_FIRE -> toggleFieldOfFire.setSelected((Boolean) e.getNewValue());
            case GUIPreferences.SHOW_SENSOR_RANGE -> toggleSensorRange.setSelected((Boolean) e.getNewValue());
            case GUIPreferences.FOV_SPOTTING_MODE -> {
                // Use invokeLater to avoid interfering with accelerator processing
                final boolean newState = (Boolean) e.getNewValue();
                javax.swing.SwingUtilities.invokeLater(() -> toggleFovSpotting.setSelected(newState));
            }
            case GUIPreferences.SHOW_KEYBINDS_OVERLAY -> viewKeybindsOverlay.setSelected((Boolean) e.getNewValue());
            case GUIPreferences.SHOW_PLANETARY_CONDITIONS_OVERLAY ->
                  viewPlanetaryConditionsOverlay.setSelected((Boolean) e.getNewValue());
            case GUIPreferences.TURN_DETAILS_OVERLAY -> viewTurnDetailsOverlay.setSelected((Boolean) e.getNewValue());
            case GUIPreferences.SHOW_UNIT_OVERVIEW -> viewUnitOverview.setSelected((Boolean) e.getNewValue());
            case GUIPreferences.MINI_MAP_ENABLED -> viewMinimap.setSelected(GUIP.getMinimapEnabled());
            case GUIPreferences.SHOW_COORDS -> toggleHexCoords.setSelected(GUIP.getCoordsEnabled());
            case KeyBindParser.KEYBINDS_CHANGED -> setKeyBinds();
            case GUIPreferences.UNIT_DISPLAY_ENABLED -> viewMekDisplay.setSelected(GUIP.getUnitDisplayEnabled());
            case GUIPreferences.FORCE_DISPLAY_ENABLED -> viewForceDisplay.setSelected(GUIP.getForceDisplayEnabled());
            case GUIPreferences.MINI_REPORT_ENABLED -> gameRoundReport.setSelected(GUIP.getMiniReportEnabled());
            case GUIPreferences.PLAYER_LIST_ENABLED -> gamePlayerList.setSelected(GUIP.getPlayerListEnabled());
            case RecentBoardList.RECENT_BOARDS_UPDATED -> initializeRecentBoardsMenu();
            case GUIPreferences.BOT_COMMANDS_ENABLED -> viewBotCommands.setSelected(GUIP.getBotCommandsEnabled());
        }
    }

    /** Removes this as listener. */
    public void die() {
        GUIP.removePreferenceChangeListener(this);
        KeyBindParser.removePreferenceChangeListener(this);
        RecentBoardList.removeListener(this);
    }

    private void initMenuItem(JMenuItem item, JMenu menu, String command) {
        item.addActionListener(this);
        item.setActionCommand(command);
        itemMap.put(command, item);
        menu.add(item);
    }

    private void initMenuItem(JMenuItem item, JMenu menu, String command, int mnemonic) {
        initMenuItem(item, menu, command);
        item.setMnemonic(mnemonic);
    }

    private void initMenuItem(JMenuItem item, JMenu menu, String command, boolean selected) {
        initMenuItem(item, menu, command);
        item.setSelected(selected);
    }

    private void initMenuItem(JMenuItem item, JMenu menu, String command, int mnemonic, boolean selected) {
        initMenuItem(item, menu, command);
        item.setMnemonic(mnemonic);
        item.setSelected(selected);
    }

    /**
     * Updates the Recent Boards submenu with the current list of recent boards
     */
    private void initializeRecentBoardsMenu() {
        List<String> recentBoards = RecentBoardList.getRecentBoards();
        boardRecent.removeAll();
        for (String recentBoard : recentBoards) {
            File boardFile = new File(recentBoard);
            JMenuItem item = new JMenuItem(boardFile.getName());
            initMenuItem(item, boardRecent, BOARD_RECENT + "|" + recentBoard);
        }
        boardRecent.setEnabled(!recentBoards.isEmpty());
    }
}
