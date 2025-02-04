/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.swing.ClientGUI.*;

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
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.KeyBindParser;
import megamek.common.enums.GamePhase;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.logging.MMLogger;

/**
 * The menu bar that is used across MM, i.e. in the main menu, the board editor
 * and
 * the lobby and game.
 */
public class CommonMenuBar extends JMenuBar implements ActionListener, IPreferenceChangeListener {
    private final static MMLogger logger = MMLogger.create(CommonMenuBar.class);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** True when this menu is attached to the board editor. */
    private boolean isBoardEditor = false;

    /** True when this menu is attached to the AI editor. */
    private boolean isAiEditor = false;

    /** True when this menu is attached to the game's main menu. */
    private boolean isMainMenu = false;

    /** True when this menu is attached to a client (lobby or ingame). */
    private boolean isGame = false;

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
    private final JMenuItem fileRefreshCache = new JMenuItem(getString("CommonMenuBar.fileUnitsRefreshUnitCache"));
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
    private final JMenuItem boardResize = new JMenuItem(getString("CommonMenuBar.boardResize"));
    private final JMenuItem boardValidate = new JMenuItem(getString("CommonMenuBar.boardValidate"));
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
    private final JCheckBoxMenuItem viewForceDisplay = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewForceDisplay"));
    private final JMenuItem viewAccessibilityWindow = new JMenuItem(getString("CommonMenuBar.viewAccessibilityWindow"));
    private final JCheckBoxMenuItem viewKeybindsOverlay = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewKeyboardShortcuts"));
    private final JCheckBoxMenuItem viewPlanetaryConditionsOverlay = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewPlanetaryConditions"));
    private final JMenuItem viewZoomIn = new JMenuItem(getString("CommonMenuBar.viewZoomIn"));
    private final JMenuItem viewZoomOut = new JMenuItem(getString("CommonMenuBar.viewZoomOut"));
    private final JMenuItem viewLabels = new JMenuItem(getString("CommonMenuBar.viewLabels"));
    private final JCheckBoxMenuItem viewBotCommands = new JCheckBoxMenuItem(getString("CommonMenuBar.viewBotCommands"));
    private final JMenuItem viewResetWindowPositions = new JMenuItem(getString("CommonMenuBar.viewResetWindowPos"));
    private final JCheckBoxMenuItem toggleIsometric = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleIsometric"));
    private final JCheckBoxMenuItem toggleHexCoords = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleHexCoords"));
    private final JCheckBoxMenuItem toggleSensorRange = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleSensorRange"));
    private final JCheckBoxMenuItem toggleFieldOfFire = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleFieldOfFire"));
    private final JMenuItem toggleFleeZone = new JMenuItem(getString("CommonMenuBar.viewToggleFleeZone"));
    private final JCheckBoxMenuItem toggleFovHighlight = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleFovHighlight"));
    private final JCheckBoxMenuItem toggleFovDarken = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleFovDarken"));
    private final JCheckBoxMenuItem toggleFiringSolutions = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleFiringSolutions"));
    private final JCheckBoxMenuItem toggleCFWarning = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewToggleCFWarning"));
    private final JCheckBoxMenuItem viewMovementEnvelope = new JCheckBoxMenuItem(
            getString("CommonMenuBar.movementEnvelope"));
    private final JCheckBoxMenuItem viewTurnDetailsOverlay = new JCheckBoxMenuItem(
            getString("CommonMenuBar.turnDetailsOverlay"));
    private final JMenuItem viewMovModEnvelope = new JMenuItem(getString("CommonMenuBar.movementModEnvelope"));
    private final JMenuItem viewLOSSetting = new JMenuItem(getString("CommonMenuBar.viewLOSSetting"));
    private final JCheckBoxMenuItem viewUnitOverview = new JCheckBoxMenuItem(
            getString("CommonMenuBar.viewUnitOverview"));
    private final JMenuItem viewClientSettings = new JMenuItem(getString("CommonMenuBar.viewClientSettings"));
    private final JMenuItem viewIncGUIScale = new JMenuItem(getString("CommonMenuBar.viewIncGUIScale"));
    private final JMenuItem viewDecGUIScale = new JMenuItem(getString("CommonMenuBar.viewDecGUIScale"));

    // The AI Editor Menu
    private final JMenuItem aiEditorNew = new JMenuItem(getString("CommonMenuBar.aiEditor.New"));
    private final JMenuItem aiEditorOpen = new JMenuItem(getString("CommonMenuBar.aiEditor.Open"));
    private final JMenu aiEditorRecentProfile = new JMenu(getString("CommonMenuBar.aiEditor.RecentProfile"));
    private final JMenuItem aiEditorSave = new JMenuItem(getString("CommonMenuBar.aiEditor.Save"));
    private final JMenuItem aiEditorSaveAs = new JMenuItem(getString("CommonMenuBar.aiEditor.SaveAs"));
    private final JMenuItem aiEditorReloadFromDisk = new JMenuItem(getString("CommonMenuBar.aiEditor.ReloadFromDisk"));
    private final JMenuItem aiEditorUndo = new JMenuItem(getString("CommonMenuBar.aiEditor.Undo"));
    private final JMenuItem aiEditorRedo = new JMenuItem(getString("CommonMenuBar.aiEditor.Redo"));
    private final JMenuItem aiEditorNewDecision = new JMenuItem(getString("CommonMenuBar.aiEditor.NewDecision"));
    private final JMenuItem aiEditorNewConsideration = new JMenuItem(getString("CommonMenuBar.aiEditor.NewConsideration"));
    private final JMenuItem aiEditorNewDecisionScoreEvaluator = new JMenuItem(
            getString("CommonMenuBar.aiEditor.NewDecisionScoreEvaluator"));
    private final JMenuItem aiEditorExport = new JMenuItem(getString("CommonMenuBar.aiEditor.Export"));
    private final JMenuItem aiEditorImport = new JMenuItem(getString("CommonMenuBar.aiEditor.Import"));

    // The Help menu
    private final JMenuItem helpContents = new JMenuItem(getString("CommonMenuBar.helpContents"));
    private final JMenuItem helpSkinning = new JMenuItem(getString("CommonMenuBar.helpSkinning"));
    private final JMenuItem helpAbout = new JMenuItem(getString("CommonMenuBar.helpAbout"));
    private final JMenuItem helpResetNags = new JMenuItem(getString("CommonMenuBar.helpResetNags"));

    // The Firing Action menu
    private final JMenuItem fireSaveWeaponOrder = new JMenuItem(getString("CommonMenuBar.fireSaveWeaponOrder"));

    /**
     * Contains all ActionListeners that have registered themselves with this menu
     * bar.
     */
    private final List<ActionListener> actionListeners = new ArrayList<>();

    /** Maps the Action Command to the respective MenuItem. */
    private final Map<String, JMenuItem> itemMap = new HashMap<>();

    public static CommonMenuBar getMenuBarForGame() {
        var menuBar = new CommonMenuBar();
        menuBar.isGame = true;
        menuBar.updateEnabledStates();
        return menuBar;
    }

    public static CommonMenuBar getMenuBarForBoardEditor() {
        var menuBar = new CommonMenuBar();
        menuBar.isBoardEditor = true;
        menuBar.updateEnabledStates();
        return menuBar;
    }

    public static CommonMenuBar getMenuBarForAiEditor() {
        var menuBar = new CommonMenuBar();
        menuBar.isAiEditor = true;
        menuBar.updateEnabledStates();
        return menuBar;
    }

    public static CommonMenuBar getMenuBarForMainMenu() {
        var menuBar = new CommonMenuBar();
        menuBar.isMainMenu = true;
        menuBar.updateEnabledStates();
        return menuBar;
    }

    /** Creates the common MegaMek menu bar. */
    public CommonMenuBar() {
        // Create the Game menu
        JMenu menu = new JMenu(Messages.getString("CommonMenuBar.FileMenu"));
        menu.setMnemonic(VK_F);
        add(menu);
        initMenuItem(gameStart, menu, FILE_GAME_NEW);
        initMenuItem(gameLoad, menu, FILE_GAME_LOAD, VK_L);
        initMenuItem(gameSave, menu, FILE_GAME_SAVE, VK_S);
        initMenuItem(gameQSave, menu, FILE_GAME_QSAVE);
        initMenuItem(gameQLoad, menu, FILE_GAME_QLOAD);
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
        initMenuItem(fileUnitsReinforceRAT, menu, FILE_UNITS_REINFORCE_RAT);
        initMenuItem(fileUnitsSave, menu, FILE_UNITS_SAVE);
        menu.addSeparator();

        initMenuItem(fileRefreshCache, menu, FILE_REFRESH_CACHE);
        initMenuItem(fileUnitsBrowse, menu, FILE_UNITS_BROWSE);
        // The accelerator overlaps with that for changing label style but they are never active at the same time
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
        initMenuItem(boardValidate, menu, BOARD_VALIDATE);
        initMenuItem(boardSourceFile, menu, BOARD_SOURCEFILE);
        menu.addSeparator();

        initMenuItem(boardSaveAsImage, menu, BOARD_SAVE_AS_IMAGE);
        boardSaveAsImage.setToolTipText(Messages.getString("CommonMenuBar.fileBoardSaveAsImage.tooltip"));
        initMenuItem(boardSaveAsImageUnits, menu, BOARD_SAVE_AS_IMAGE_UNITS);
        boardSaveAsImageUnits.setToolTipText(Messages.getString("CommonMenuBar.fileBoardSaveAsImageUnits.tooltip"));
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
        initMenuItem(viewIncGUIScale, menu, VIEW_INCGUISCALE);
        initMenuItem(viewDecGUIScale, menu, VIEW_DECGUISCALE);
        menu.addSeparator();

        initMenuItem(viewMekDisplay, menu, VIEW_UNIT_DISPLAY, VK_D);
        GUIP.setUnitDisplayEnabled(false);
        viewMekDisplay.setSelected(false);
        initMenuItem(viewMinimap, menu, VIEW_MINI_MAP, VK_M);
        GUIP.setMinimapEnabled(false);
        viewMinimap.setSelected(false);
        initMenuItem(gameRoundReport, menu, VIEW_ROUND_REPORT);
        GUIP.setMiniReportEnabled(false);
        gameRoundReport.setSelected(false);
        initMenuItem(gamePlayerList, menu, VIEW_PLAYER_LIST);
        GUIP.setPlayerListEnabled(false);
        gamePlayerList.setSelected(false);
        initMenuItem(viewForceDisplay, menu, VIEW_FORCE_DISPLAY);
        GUIP.setForceDisplayEnabled(false);
        viewForceDisplay.setSelected(false);
        initMenuItem(viewBotCommands, menu, VIEW_BOT_COMMANDS, VK_G);
        GUIP.setBotCommandsEnabled(false);
        viewBotCommands.setSelected(false);
        menu.addSeparator();

        initMenuItem(viewKeybindsOverlay, menu, VIEW_KEYBINDS_OVERLAY);
        viewKeybindsOverlay.setSelected(GUIP.getShowKeybindsOverlay());
        initMenuItem(viewPlanetaryConditionsOverlay, menu, VIEW_PLANETARYCONDITIONS_OVERLAY);
        viewPlanetaryConditionsOverlay.setSelected(GUIP.getShowPlanetaryConditionsOverlay());
        initMenuItem(viewTurnDetailsOverlay, menu, VIEW_TURN_DETAILS_OVERLAY);
        viewTurnDetailsOverlay.setSelected(GUIP.getTurnDetailsOverlay());
        initMenuItem(viewUnitOverview, menu, VIEW_UNIT_OVERVIEW);
        menu.addSeparator();

        initMenuItem(viewResetWindowPositions, menu, VIEW_RESET_WINDOW_POSITIONS);
        initMenuItem(viewAccessibilityWindow, menu, VIEW_ACCESSIBILITY_WINDOW, VK_A);
        viewAccessibilityWindow.setMnemonic(KeyEvent.VK_A);
        menu.addSeparator();

        viewUnitOverview.setSelected(GUIP.getShowUnitOverview());
        initMenuItem(viewZoomIn, menu, VIEW_ZOOM_IN);
        initMenuItem(viewZoomOut, menu, VIEW_ZOOM_OUT);
        initMenuItem(toggleIsometric, menu, VIEW_TOGGLE_ISOMETRIC, VK_T);
        toggleIsometric.setSelected(GUIP.getIsometricEnabled());
        initMenuItem(toggleHexCoords, menu, VIEW_TOGGLE_HEXCOORDS, VK_G);
        initMenuItem(viewLabels, menu, VIEW_LABELS);
        menu.addSeparator();

        initMenuItem(toggleFovDarken, menu, VIEW_TOGGLE_FOV_DARKEN);
        toggleFovDarken.setSelected(GUIP.getFovDarken());
        toggleFovDarken.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFovDarkenTooltip"));
        initMenuItem(viewLOSSetting, menu, VIEW_LOS_SETTING);
        initMenuItem(toggleFovHighlight, menu, VIEW_TOGGLE_FOV_HIGHLIGHT);
        toggleFovHighlight.setSelected(GUIP.getFovHighlight());
        initMenuItem(viewMovementEnvelope, menu, VIEW_MOVE_ENV);
        viewMovementEnvelope.setSelected(GUIP.getMoveEnvelope());
        initMenuItem(viewMovModEnvelope, menu, VIEW_MOVE_MOD_ENV);
        menu.addSeparator();

        initMenuItem(toggleSensorRange, menu, VIEW_TOGGLE_SENSOR_RANGE);
        toggleSensorRange.setSelected(GUIP.getShowSensorRange());
        toggleSensorRange.setToolTipText(Messages.getString("CommonMenuBar.viewToggleSensorRangeToolTip"));
        initMenuItem(toggleFieldOfFire, menu, VIEW_TOGGLE_FIELD_OF_FIRE);
        toggleFieldOfFire.setSelected(GUIP.getShowFieldOfFire());
        toggleFieldOfFire.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFieldOfFireToolTip"));
        initMenuItem(toggleFleeZone, menu, VIEW_TOGGLE_FLEE_ZONE);
        toggleFleeZone.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFleeZoneToolTip"));
        initMenuItem(toggleFiringSolutions, menu, VIEW_TOGGLE_FIRING_SOLUTIONS);
        toggleFiringSolutions.setToolTipText(Messages.getString("CommonMenuBar.viewToggleFiringSolutionsToolTip"));
        toggleFiringSolutions.setSelected(GUIP.getShowFiringSolutions());

        initMenuItem(toggleCFWarning, menu, VIEW_TOGGLE_CF_WARNING);
        toggleCFWarning.setToolTipText(Messages.getString("CommonMenuBar.viewToggleCFWarningToolTip"));
        toggleCFWarning.setSelected(GUIP.getShowCFWarnings());

        menu = new JMenu(Messages.getString("CommonMenuBar.AIEditorMenu"));
        menu.setMnemonic(VK_A);
        add(menu);

        initMenuItem(aiEditorNew, menu, AI_EDITOR_NEW);
        initMenuItem(aiEditorOpen, menu, AI_EDITOR_OPEN);
        initMenuItem(aiEditorRecentProfile, menu, AI_EDITOR_RECENT_PROFILE);
        initializeRecentAiProfilesMenu();
        menu.addSeparator();

        initMenuItem(aiEditorSave, menu, AI_EDITOR_SAVE);
//        initMenuItem(aiEditorSaveAs, menu, AI_EDITOR_SAVE_AS);
        initMenuItem(aiEditorReloadFromDisk, menu, AI_EDITOR_RELOAD_FROM_DISK);
        menu.addSeparator();

        initMenuItem(aiEditorUndo, menu, AI_EDITOR_UNDO);
        initMenuItem(aiEditorRedo, menu, AI_EDITOR_REDO);
        menu.addSeparator();

        initMenuItem(aiEditorNewConsideration, menu, AI_EDITOR_NEW_CONSIDERATION);
        aiEditorNewConsideration.setMnemonic(VK_U);
        initMenuItem(aiEditorNewDecisionScoreEvaluator, menu, AI_EDITOR_NEW_DECISION_SCORE_EVALUATOR);
        aiEditorNewDecisionScoreEvaluator.setMnemonic(VK_I);
        menu.addSeparator();

        initMenuItem(aiEditorExport, menu, AI_EDITOR_EXPORT);
        initMenuItem(aiEditorImport, menu, AI_EDITOR_IMPORT);



        // Create the Help menu
        menu = new JMenu(Messages.getString("CommonMenuBar.HelpMenu"));
        menu.setMnemonic(VK_H);
        add(menu);
        initMenuItem(helpResetNags, menu, HELP_RESETNAGS);
        initMenuItem(helpContents, menu, HELP_CONTENTS);
        initMenuItem(helpSkinning, menu, HELP_SKINNING);
        menu.addSeparator();
        initMenuItem(helpAbout, menu, HELP_ABOUT);

        setKeyBinds();
        GUIP.addPreferenceChangeListener(this);
        KeyBindParser.addPreferenceChangeListener(this);
        RecentBoardList.addListener(this);
    }

    /** Sets/updates the accelerators from the KeyCommandBinds preferences. */
    private void setKeyBinds() {
        toggleSensorRange.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.SENSOR_RANGE));
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
        viewIncGUIScale.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.INC_GUISCALE));
        viewDecGUIScale.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.DEC_GUISCALE));
        viewClientSettings.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.CLIENT_SETTINGS));
        boardUndo.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.UNDO));
        boardRedo.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.REDO));
        gameRoundReport.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.ROUND_REPORT));
        viewZoomIn.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.ZOOM_IN));
        viewZoomOut.setAccelerator(KeyCommandBind.keyStroke(KeyCommandBind.ZOOM_OUT));
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
        // Boardview and others may listen to PreferenceChanges to detect these
        if (event.getActionCommand().equals(ClientGUI.VIEW_INCGUISCALE)) {
            float guiScale = GUIP.getGUIScale();
            if (guiScale < ClientGUI.MAX_GUISCALE) {
                GUIP.setValue(GUIPreferences.GUI_SCALE, guiScale + 0.1);
            }
        } else if (event.getActionCommand().equals(ClientGUI.VIEW_DECGUISCALE)) {
            float guiScale = GUIP.getGUIScale();
            if (guiScale > ClientGUI.MIN_GUISCALE) {
                GUIP.setValue(GUIPreferences.GUI_SCALE, guiScale - 0.1);
            }
        } else if (event.getActionCommand().equals(ClientGUI.VIEW_PLANETARYCONDITIONS_OVERLAY)) {
            GUIP.togglePlanetaryConditionsOverlay();

        } else if (event.getActionCommand().equals(VIEW_KEYBINDS_OVERLAY)) {
            GUIP.toggleKeybindsOverlay();

        } else if (event.getActionCommand().equals(VIEW_TURN_DETAILS_OVERLAY)) {
            GUIP.setTurnDetailsOverlay(!GUIP.getTurnDetailsOverlay());

        } else if (event.getActionCommand().equals(ClientGUI.VIEW_LABELS)) {
            GUIP.setUnitLabelStyle(GUIP.getUnitLabelStyle().next());

        } else if (event.getActionCommand().equals(VIEW_UNIT_OVERVIEW)) {
            GUIP.setShowUnitOverview(!GUIP.getShowUnitOverview());

        } else if (event.getActionCommand().equals(ClientGUI.HELP_RESETNAGS)) {
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
     * Register an object that wishes to be alerted when an item on this menu
     * bar has been selected.
     * <p>
     * Please note, the ActionCommand property of
     * the action event will inform the listener as to which menu item that has
     * been selected. Not all listeners will be interested in all menu items.
     *
     * @param listener - the <code>ActionListener</code> that wants to
     *                 register itself.
     */
    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    /**
     * Remove an object that was being alerted when an item on this menu bar was
     * selected.
     *
     * @param listener - the <code>ActionListener</code> that wants to be
     *                 removed.
     */
    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    /**
     * Manages the enabled states of the menu items depending on where the menu is
     * employed.
     */
    private synchronized void updateEnabledStates() {
        boolean isLobby = isGame && phase.isLounge();
        boolean isInGame = isGame && phase.isDuringOrAfter(GamePhase.DEPLOYMENT);
        boolean isInGameBoardView = isInGame && phase.isOnMap();
        boolean isBoardView = isInGameBoardView || isBoardEditor;
        boolean canSave = !phase.isUnknown() && !phase.isSelection() && !phase.isExchange()
                && !phase.isVictory() && !phase.isStartingScenario();
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
        boardResize.setEnabled(isBoardEditor);
        boardSourceFile.setEnabled(isBoardEditor);
        gameQLoad.setEnabled(isMainMenu);
        gameLoad.setEnabled(isMainMenu);
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
        fileUnitsPaste.setEnabled(isLobby);
        fileUnitsCopy.setEnabled(isLobby);
        fileUnitsReinforce.setEnabled((isInGame) && isNotVictory);
        fileUnitsReinforceRAT.setEnabled((isLobby || isInGame) && isNotVictory);
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
        toggleFiringSolutions.setEnabled(isInGameBoardView);
        toggleCFWarning.setEnabled(isInGameBoardView);
        viewMovementEnvelope.setEnabled(isInGameBoardView);
        viewTurnDetailsOverlay.setEnabled(isInGameBoardView);
        viewMovModEnvelope.setEnabled(isInGameBoardView);
        gameRoundReport.setEnabled(isInGame);
        viewMekDisplay.setEnabled(isInGameBoardView);
        viewForceDisplay.setEnabled(isInGameBoardView);
        fireSaveWeaponOrder.setEnabled(isInGameBoardView);

        aiEditorExport.setEnabled(isAiEditor);
        aiEditorImport.setEnabled(isAiEditor);
        aiEditorNew.setEnabled(isAiEditor);
        aiEditorOpen.setEnabled(isAiEditor);
        aiEditorRecentProfile.setEnabled(isAiEditor);
        aiEditorSave.setEnabled(isAiEditor);
        aiEditorSaveAs.setEnabled(isAiEditor);
        aiEditorReloadFromDisk.setEnabled(isAiEditor);
        aiEditorUndo.setEnabled(isAiEditor);
        aiEditorRedo.setEnabled(isAiEditor);
        aiEditorNewDecision.setEnabled(isAiEditor);
        aiEditorNewConsideration.setEnabled(isAiEditor);
        aiEditorNewDecisionScoreEvaluator.setEnabled(isAiEditor);

        viewBotCommands.setEnabled(isInGame);
    }

    /**
     * Identify to the menu bar which phase is currently in progress
     *
     * @param current - the <code>int</code> value of the current phase (the
     *                valid values for this argument are defined as constants in the
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
        if (e.getName().equals(GUIPreferences.USE_ISOMETRIC)) {
            toggleIsometric.setSelected((Boolean) e.getNewValue());
        } else if (e.getName().equals(GUIPreferences.SHOW_FIELD_OF_FIRE)) {
            toggleFieldOfFire.setSelected((Boolean) e.getNewValue());
        } else if (e.getName().equals(GUIPreferences.SHOW_SENSOR_RANGE)) {
            toggleSensorRange.setSelected((Boolean) e.getNewValue());
        } else if (e.getName().equals(GUIPreferences.SHOW_KEYBINDS_OVERLAY)) {
            viewKeybindsOverlay.setSelected((Boolean) e.getNewValue());
        } else if (e.getName().equals(GUIPreferences.SHOW_PLANETARYCONDITIONS_OVERLAY)) {
            viewPlanetaryConditionsOverlay.setSelected((Boolean) e.getNewValue());
        } else if (e.getName().equals(GUIPreferences.TURN_DETAILS_OVERLAY)) {
            viewTurnDetailsOverlay.setSelected((Boolean) e.getNewValue());
        } else if (e.getName().equals(GUIPreferences.SHOW_UNIT_OVERVIEW)) {
            viewUnitOverview.setSelected((Boolean) e.getNewValue());
        } else if (e.getName().equals(GUIPreferences.MINI_MAP_ENABLED)) {
            viewMinimap.setSelected(GUIP.getMinimapEnabled());
        } else if (e.getName().equals(GUIPreferences.SHOW_COORDS)) {
            toggleHexCoords.setSelected(GUIP.getCoordsEnabled());
        } else if (e.getName().equals(KeyBindParser.KEYBINDS_CHANGED)) {
            setKeyBinds();
        } else if (e.getName().equals(GUIPreferences.UNIT_DISPLAY_ENABLED)) {
            viewMekDisplay.setSelected(GUIP.getUnitDisplayEnabled());
        } else if (e.getName().equals(GUIPreferences.FORCE_DISPLAY_ENABLED)) {
            viewForceDisplay.setSelected(GUIP.getForceDisplayEnabled());
        } else if (e.getName().equals(GUIPreferences.MINI_REPORT_ENABLED)) {
            gameRoundReport.setSelected(GUIP.getMiniReportEnabled());
        } else if (e.getName().equals(GUIPreferences.PLAYER_LIST_ENABLED)) {
            gamePlayerList.setSelected(GUIP.getPlayerListEnabled());
        } else if (e.getName().equals(RecentBoardList.RECENT_BOARDS_UPDATED)) {
            initializeRecentBoardsMenu();
        } else if (e.getName().equals(GUIPreferences.BOT_COMMANDS_ENABLED)) {
            viewBotCommands.setSelected(GUIP.getBotCommandsEnabled());
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

    private void initializeRecentAiProfilesMenu() {
        List<String> recentProfiles = RecentProfileList.getRecentProfiles();
        aiEditorRecentProfile.removeAll();
        for (String recentProfile : recentProfiles) {
            JMenuItem item = new JMenuItem(recentProfile);
            initMenuItem(item, aiEditorRecentProfile, AI_EDITOR_RECENT_PROFILE + "|" + recentProfile);
        }
        aiEditorRecentProfile.setEnabled(!recentProfiles.isEmpty());
    }
}
