/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.boardview.LabelDisplayStyle;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.EntityMovementType;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.preference.PreferenceManager;
import megamek.common.preference.PreferenceStoreProxy;

import javax.swing.*;
import java.awt.*;

public class GUIPreferences extends PreferenceStoreProxy {

    /*
     * --Begin advanced settings section-- Options with the "ADVANCED" prefix
     * are treated specially. They are quick and easy to add, at the expense of
     * some user-friendliness (that's why they are "advanced"<grin>). Only the
     * appropriate declaration below and the default value (further down in this
     * file) need be added. The code will then automatically add the option to
     * the advanced tab of the client settings. In order to retrieve one of
     * these settings, use a line like:
     * GUIPreferences.getInstance().getInt("AdvancedWhateverOption"), where
     * getInt is replaced with getBoolean, getString, etc as necessary. The
     * reason these options were made this way is that GUI options have a way of
     * quickly multiplying and we need a quick and dirty way of adding them
     * without having to code too many new lines. In addition, keeping them
     * separated in the settings dialog shields new users from unnecessary
     * complication.
     */
    public static final String ADVANCED_MECH_DISPLAY_ARMOR_LARGE_FONT_SIZE = "AdvancedMechDisplayArmorLargeFontSize";
    public static final String ADVANCED_MECH_DISPLAY_ARMOR_MEDIUM_FONT_SIZE = "AdvancedMechDisplayArmorMediumFontSize";
    public static final String ADVANCED_MECH_DISPLAY_ARMOR_SMALL_FONT_SIZE = "AdvancedMechDisplayArmorSmallFontSize";
    public static final String ADVANCED_MECH_DISPLAY_LARGE_FONT_SIZE = "AdvancedMechDisplayLargeFontSize";
    public static final String ADVANCED_MECH_DISPLAY_MEDIUM_FONT_SIZE = "AdvancedMechDisplayMediumFontSize";
    public static final String ADVANCED_MOVE_DEFAULT_CLIMB_MODE = "AdvancedMoveDefaultClimbMode";
    public static final String ADVANCED_MOVE_DEFAULT_COLOR = "AdvancedMoveDefaultColor";
    public static final String ADVANCED_MOVE_ILLEGAL_COLOR = "AdvancedMoveIllegalColor";
    public static final String ADVANCED_MOVE_JUMP_COLOR = "AdvancedMoveJumpColor";
    public static final String ADVANCED_MOVE_MASC_COLOR = "AdvancedMoveMASCColor";
    public static final String ADVANCED_MOVE_RUN_COLOR = "AdvancedMoveRunColor";
    public static final String ADVANCED_MOVE_BACK_COLOR = "AdvancedMoveBackColor";
    public static final String ADVANCED_MOVE_SPRINT_COLOR = "AdvancedMoveSprintColor";
    public static final String ADVANCED_MOVE_FONT_TYPE = "AdvancedMoveFontType";
    public static final String ADVANCED_MOVE_FONT_SIZE = "AdvancedMoveFontSize";
    public static final String ADVANCED_MOVE_FONT_STYLE = "AdvancedMoveFontStyle";
    public static final String ADVANCED_MOVE_STEP_DELAY = "AdvancedMoveStepDelay";
    public static final String ADVANCED_FIRE_SOLN_CANSEE_COLOR = "AdvancedFireSolnCanSeeColor";
    public static final String ADVANCED_FIRE_SOLN_NOSEE_COLOR = "AdvancedFireSolnNoSeeColor";
    public static final String ADVANCED_DARKEN_MAP_AT_NIGHT = "AdvancedDarkenMapAtNight";
    public static final String ADVANCED_MAPSHEET_COLOR = "AdvancedMapsheetColor";
    public static final String ADVANCED_TRANSLUCENT_HIDDEN_UNITS = "AdvancedTranslucentHiddenUnits";
    public static final String ADVANCED_ATTACK_ARROW_TRANSPARENCY = "AdvancedAttackArrowTransparency";
    public static final String ADVANCED_BUILDING_TEXT_COLOR = "AdvancedBuildingTextColor";
    public static final String ADVANCED_CHATBOX2_BACKCOLOR = "AdvancedChatbox2BackColor";
    public static final String ADVANCED_CHATBOX2_TRANSPARANCY = "AdvancedChatbox2Transparancy";
    public static final String ADVANCED_CHATBOX2_AUTOSLIDEDOWN = "AdvancedChatbox2AutoSlidedown";
    public static final String ADVANCED_ECM_TRANSPARENCY = "AdvancedECMTransparency";
    public static final String ADVANCED_UNITOVERVIEW_SELECTED_COLOR = "AdvancedUnitOverviewSelectedColor";
    public static final String ADVANCED_UNITOVERVIEW_VALID_COLOR = "AdvancedUnitOverviewValidColor";
    public static final String ADVANCED_KEY_REPEAT_DELAY = "AdvancedKeyRepeatDelay";
    public static final String ADVANCED_KEY_REPEAT_RATE = "AdvancedKeyRepeatRate";
    public static final String ADVANCED_SHOW_FPS = "AdvancedShowFPS";
    public static final String ADVANCED_BUTTONS_PER_ROW = "AdvancedButtonsPerRow";
    public static final String ADVANCED_ARMORMINI_UNITS_PER_BLOCK = "AdvancedArmorMiniUnitsPerBlock";
    public static final String ADVANCED_ARMORMINI_ARMOR_CHAR = "AdvancedArmorMiniArmorChar";
    public static final String ADVANCED_ARMORMINI_CAP_ARMOR_CHAR = "AdvancedArmorMiniCapArmorChar";
    public static final String ADVANCED_ARMORMINI_IS_CHAR = "AdvancedArmorMiniISChar";
    public static final String ADVANCED_ARMORMINI_DESTROYED_CHAR = "AdvancedArmorMiniDestroyedChar";
    public static final String ADVANCED_ARMORMINI_COLOR_INTACT = "AdvancedArmorMiniColorIntact";
    public static final String ADVANCED_ARMORMINI_COLOR_PARTIAL_DMG = "AdvancedArmorMiniColorPartialDmg";
    public static final String ADVANCED_ARMORMINI_COLOR_DAMAGED = "AdvancedArmorMiniColorDamaged";
    public static final String ADVANCED_ARMORMINI_FONT_SIZE_MOD = "AdvancedArmorMiniFrontSizeMod";
    public static final String ADVANCED_ROUND_REPORT_SPRITES = "AdvancedRoundReportSprites";
    public static final String ADVANCED_LOW_FOLIAGE_COLOR = "AdvancedLowFoliageColor";
    public static final String ADVANCED_NO_SAVE_NAG = "AdvancedNoSaveNag";
    public static final String ADVANCED_USE_CAMO_OVERLAY = "AdvancedUseCamoOverlay";
    public static final String ADVANCED_MAP_TEXT_COLOR = "AdvancedMapTextColor";
    public static final String ADVANCED_WARNING_COLOR = "AdvancedWarningColor";
    public static final String ADVANCED_TMM_PIP_MODE = "AdvancedTmmPipMode";
    public static final String ADVANCED_HEAT_COLOR_4 = "AdvancedHeatColor4";
    public static final String ADVANCED_HEAT_COLOR_7 = "AdvancedHeatColor7";
    public static final String ADVANCED_HEAT_COLOR_9 = "AdvancedHeatColor9";
    public static final String ADVANCED_HEAT_COLOR_12 = "AdvancedHeatColor12";
    public static final String ADVANCED_HEAT_COLOR_13 = "AdvancedHeatColor13";
    public static final String ADVANCED_HEAT_COLOR_14 = "AdvancedHeatColor14";
    public static final String ADVANCED_HEAT_COLOR_OVERHEAT = "AdvancedHeatColorOverheat";
    public static final String ADVANCED_REPORT_COLOR_LINK = "AdvancedReportColorLink";

    public static final String ADVANCED_PLANETARY_CONDITIONS_COLOR_TITLE = "AdvancedPlanetaryConditionsColorTitle";
    public static final String ADVANCED_PLANETARY_CONDITIONS_COLOR_TEXT = "AdvancedPlanetaryConditionsColorText";
    public static final String ADVANCED_PLANETARY_CONDITIONS_COLOR_COLD = "AdvancedPlanetaryConditionsColorCold";
    public static final String ADVANCED_PLANETARY_CONDITIONS_COLOR_HOT = "AdvancedPlanetaryConditionsColorHot";
    public static final String ADVANCED_PLANETARY_CONDITIONS_COLOR_BACKGROUND = "AdvancedPlanetaryConditionsColorBackground";
    public static final String ADVANCED_PLANETARY_CONDITIONS_SHOW_DEFAULTS = "AdvancedPlanetaryConditionsShowDefaults";
    public static final String ADVANCED_PLANETARY_CONDITIONS_SHOW_HEADER = "AdvancedPlanetaryConditionsShowHeader";
    public static final String ADVANCED_PLANETARY_CONDITIONS_SHOW_LABELS = "AdvancedPlanetaryConditionsShowLabels";
    public static final String ADVANCED_PLANETARY_CONDITIONS_SHOW_VALUES = "AdvancedPlanetaryConditionsShowValues";
    public static final String ADVANCED_PLANETARY_CONDITIONS_SHOW_INDICATORS = "AdvancedPlanetaryConditionsShowIndicators";
    public static final String ADVANCED_UNITTOOLTIP_SEENBYRESOLUTION = "AdvancedUnitToolTipSeenByResolution";
    public static final String ADVANCED_DOCK_ON_LEFT = "AdvancedDockOnLeft";
    public static final String ADVANCED_DOCK_MULTIPLE_ON_Y_AXIS = "AdvancedDockMultipleOnYAxis";
    public static final String ADVANCED_PLAYERS_REMAINING_TO_SHOW = "AdvancedPlayersRemainingToShow";
    public static final String ADVANCED_UNIT_DISPLAY_WEAPON_LIST_HEIGHT = "AdvancedUnitDisplayWeaponListHeight";

    /* --End advanced settings-- */

    public static final String SHOW_COORDS = "showCoords";
    public static final String ANTIALIASING = "AntiAliasing";
    public static final String SHADOWMAP = "ShadowMap";
    public static final String INCLINES = "Inclines";
    public static final String AOHEXSHADOWS = "AoHexShadows";
    public static final String LEVELHIGHLIGHT = "LevelHighlight";
    public static final String FLOATINGISO = "FloatingIsometric";
    public static final String MMSYMBOL = "MmSymbol";
    public static final String SOFTCENTER = "SoftCenter";
    public static final String AUTO_END_FIRING = "AutoEndFiring";
    public static final String AUTO_DECLARE_SEARCHLIGHT = "AutoDeclareSearchlight";
    public static final String CUSTOM_UNIT_HEIGHT = "CustomUnitDialogSizeHeight";
    public static final String CUSTOM_UNIT_WIDTH = "CustomUnitDialogSizeWidth";
    public static final String UNIT_DISPLAY_POS_X = "UnitDisplayPosX";
    public static final String UNIT_DISPLAY_POS_Y = "UnitDisplayPosY";
    public static final String UNIT_DISPLAY_NONTABBED_POS_X = "UnitDisplayNontabbedPosX";
    public static final String UNIT_DISPLAY_NONTABBED_POS_Y = "UnitDisplayNontabbedPosY";
    public static final String UNIT_DISPLAY_START_TABBED = "UnitDisplayStartTabbed";
    public static final String UNIT_DISPLAY_SPLIT_ABC_LOC = "UnitDisplaySplitABCLoc";
    public static final String UNIT_DISPLAY_SPLIT_BC_LOC = "UnitDisplaySplitBCLoc";
    public static final String UNIT_DISPLAY_SPLIT_A1_LOC = "UnitDisplaySplitA1Loc";
    public static final String UNIT_DISPLAY_SPLIT_B1_LOC = "UnitDisplaySplitB1Loc";
    public static final String UNIT_DISPLAY_SPLIT_C1_LOC = "UnitDisplaySplitC2Loc";
    public static final String UNIT_DISPLAY_SIZE_HEIGHT = "UnitDisplaySizeHeight";
    public static final String UNIT_DISPLAY_SIZE_WIDTH = "UnitDisplaySizeWidth";
    public static final String UNIT_DISPLAY_NONTABBED_SIZE_HEIGHT = "UnitDisplayNonTabbedSizeHeight";
    public static final String UNIT_DISPLAY_NONTABBED_SIZE_WIDTH = "UnitDisplayNontabbedSizeWidth";
    public static final String UNIT_DISPLAY_AUTO_DISPLAY_REPORT_PHASE = "UnitDisplayAutoDiplayReportPhase";
    public static final String UNIT_DISPLAY_AUTO_DISPLAY_NONREPORT_PHASE = "UnitDisplayAutoDiplayNonReportPhase";
    public static final String UNIT_DISPLAY_ENABLED = "UnitDisplayEnabled";
    public static final String UNIT_DISPLAY_LOCATION = "UnitDisplayLocation";
    public static final String SPLIT_PANE_A_DIVIDER_LOCATION = "SplitPaneADividerLocation";
    public static final String GAME_SUMMARY_BOARD_VIEW = "GameSummaryBoardView";
    public static final String GAME_SUMMARY_MINIMAP = "GameSummaryMinimap";
    public static final String ENTITY_OWNER_LABEL_COLOR = "EntityOwnerLabelColor";
    public static final String UNIT_LABEL_BORDER = "EntityOwnerLabelColor";
    public static final String TEAM_COLORING = "EntityTeamLabelColor";
    public static final String FOCUS = "Focus";
    public static final String FIRING_SOLUTIONS = "FiringSolutions";
    public static final String MOVE_ENVELOPE = "MoveEnvelope";
    public static final String FOV_HIGHLIGHT = "FovHighlight";
    public static final String FOV_HIGHLIGHT_ALPHA = "FovHighlightAlpha";
    //Rings' sizes (measured in distance to center) separated by whitespace.
    public static final String FOV_HIGHLIGHT_RINGS_RADII = "FovHighlightRingsRadii";
    //Rings' colors in the HSB format.
    //Each hsb color is separated by a semicolon, particular h, s and b values are whitespace separated.
    public static final String FOV_HIGHLIGHT_RINGS_COLORS_HSB = "FovHighlightRingsColorsInHSB";
    public static final String FOV_DARKEN = "FovDarken";
    public static final String FOV_DARKEN_ALPHA = "FovDarkenAlpha";
    public static final String FOV_STRIPES = "FoVFogStripes";
    public static final String FOV_GRAYSCALE = "FoVFogGrayscale";
    public static final String GUI_SCALE = "GUIScale";
    public static final String LOBBY_MEKTABLE_UNIT_WIDTH = "LobbyMektableUnitWidth";
    public static final String LOBBY_MEKTABLE_PILOT_WIDTH = "LobbyMektablePilotWidth";
    public static final String LOBBY_MEKTABLE_PLAYER_WIDTH = "LobbyMektablePlayerWidth";
    public static final String LOBBY_MEKTABLE_BV_WIDTH = "LobbyMektableBVWidth";
    public static final String MAP_ZOOM_INDEX = "MapZoomIndex";
    public static final String MECH_SELECTOR_INCLUDE_MODEL = "MechSelectorIncludeModel";
    public static final String MECH_SELECTOR_INCLUDE_NAME = "MechSelectorIncludeName";
    public static final String MECH_SELECTOR_INCLUDE_TONS = "MechSelectorIncludeTons";
    public static final String MECH_SELECTOR_INCLUDE_BV = "MechSelectorIncludeBV";
    public static final String MECH_SELECTOR_INCLUDE_YEAR = "MechSelectorIncludeYear";
    public static final String MECH_SELECTOR_INCLUDE_LEVEL = "MechSelectorIncludeLevel";
    public static final String MECH_SELECTOR_INCLUDE_COST = "MechSelectorIncludeCost";
    public static final String MECH_SELECTOR_SHOW_ADVANCED = "MechSelectorShowAdvanced";
    public static final String MECH_SELECTOR_UNIT_TYPE = "MechSelectorUnitType";
    public static final String MECH_SELECTOR_WEIGHT_CLASS = "MechSelectorWeightClass";
    public static final String MECH_SELECTOR_RULES_LEVELS = "MechSelectorRuleType";
    public static final String MECH_SELECTOR_SORT_COLUMN = "MechSelectorSortColumn";
    public static final String MECH_SELECTOR_SORT_ORDER = "MechSelectorSortOrder";
    public static final String MECH_SELECTOR_SIZE_HEIGHT = "MechSelectorSizeHeight";
    public static final String MECH_SELECTOR_SIZE_WIDTH = "MechSelectorSizeWidth";
    public static final String MECH_SELECTOR_POS_X = "MechSelectorPosX";
    public static final String MECH_SELECTOR_POS_Y = "MechSelectorPosY";
    public static final String MECH_SELECTOR_SPLIT_POS = "MechSelectorSplitPos";
    public static final String MINI_REPORT_POS_X = "MiniReportPosX";
    public static final String MINI_REPORT_POS_Y = "MiniReportPosY";
    public static final String MINI_REPORT_SIZE_HEIGHT = "MiniReportSizeHeight";
    public static final String MINI_REPORT_SIZE_WIDTH = "MiniReportSizeWidth";
    public static final String MINI_REPORT_ENABLED = "MiniReportEnabled";
    public static final String MINI_REPORT_AUTO_DISPLAY_REPORT_PHASE = "MiniReportAutoDiplayReportPhase";
    public static final String MINI_REPORT_AUTO_DISPLAY_NONREPORT_PHASE = "MiniReportAutoDiplayNonReportPhase";
    public static final String MINI_REPORT_LOCATION = "MiniReportLocation";
    public static final String PLAYER_LIST_POS_X = "PlayerListPosX";
    public static final String PLAYER_LIST_POS_Y = "PlayerListPosY";
    public static final String PLAYER_LIST_ENABLED = "PlayerListEnabled";
    public static final String PLAYER_LIST_AUTO_DISPLAY_REPORT_PHASE = "PlayerListAutoDiplayReportPhase";
    public static final String PLAYER_LIST_AUTO_DISPLAY_NONREPORT_PHASE = "PlayerListAutoDiplayNonReportPhase";
    public static final String MINI_MAP_COLOURS = "MinimapColours";
    public static final String MINI_MAP_ENABLED = "MinimapEnabled";
    public static final String MINI_MAP_POS_X = "MinimapPosX";
    public static final String MINI_MAP_POS_Y = "MinimapPosY";
    public static final String MINI_MAP_ZOOM = "MinimapZoom";
    public static final String MINI_MAP_HEIGHT_DISPLAY_MODE = "MinimapHeightDisplayMode";
    public static final String MINI_MAP_AUTO_DISPLAY_REPORT_PHASE = "MinimapAutoDiplayReportPhase";
    public static final String MINI_MAP_AUTO_DISPLAY_NONREPORT_PHASE = "MinimapAutoDiplayNonReportPhase";
    public static final String MINIMUM_SIZE_HEIGHT = "MinimumSizeHeight";
    public static final String MINIMUM_SIZE_WIDTH = "MinimumSizeWidth";
    public static final String MOUSE_WHEEL_ZOOM = "MouseWheelZoom";
    public static final String MOUSE_WHEEL_ZOOM_FLIP = "MouseWheelZoomFlip";
    public static final String NAG_FOR_CRUSHING_BUILDINGS = "NagForCrushingBuildings";
    public static final String NAG_FOR_MAP_ED_README = "NagForMapEdReadme";
    public static final String NAG_FOR_MASC = "NagForMASC";
    public static final String NAG_FOR_NO_ACTION = "NagForNoAction";
    public static final String NAG_FOR_NO_UNJAMRAC = "NagForNoUNJAMRAC";
    public static final String NAG_FOR_PSR = "NagForPSR";
    public static final String NAG_FOR_README = "NagForReadme";
    public static final String NAG_FOR_SPRINT = "NagForSprint";
    public static final String NAG_FOR_OVERHEAT = "NagForOverHeat";
    public static final String NAG_FOR_LAUNCH_DOORS = "NagForLaunchDoors";
    public static final String NAG_FOR_MECHANICAL_FALL_DAMAGE = "NagForMechanicalFallDamage";
    public static final String NAG_FOR_DOOMED = "NagForDoomed";
    public static final String NAG_FOR_WIGE_LANDING = "NagForWiGELanding";
    public static final String RULER_COLOR_1 = "RulerColor1";
    public static final String RULER_COLOR_2 = "RulerColor2";
    public static final String RULER_POS_X = "RulerPosX";
    public static final String RULER_POS_Y = "RulerPosY";
    public static final String RULER_SIZE_HEIGHT = "RulerSizeHeight";
    public static final String RULER_SIZE_WIDTH = "RulerSizeWidth";
    public static final String SCROLL_SENSITIVITY = "ScrollSensitivity";
    public static final String SHOW_FIELD_OF_FIRE = "ShowFieldOfFire";
    public static final String SHOW_MAPHEX_POPUP = "ShowMapHexPopup";
    public static final String SHOW_WPS_IN_TT = "ShowWpsinTT";
    public static final String SHOW_ARMOR_MINIVIS_TT = "showArmorMiniVisTT";
    public static final String SHOW_PILOT_PORTRAIT_TT = "showPilotPortraitTT";
    public static final String SHOW_MOVE_STEP = "ShowMoveStep";
    public static final String SHOW_WRECKS = "ShowWrecks";
    public static final String SOUND_BING_FILENAME_CHAT = "SoundBingFilenameChat";
    public static final String SOUND_BING_FILENAME_MY_TURN = "SoundBingFilenameMyTurn";
    public static final String SOUND_BING_FILENAME_OTHERS_TURN = "SoundBingFilenameOthersTurn";
    public static final String SOUND_MUTE_CHAT = "SoundMuteChat";
    public static final String SOUND_MUTE_MY_TURN = "SoundMuteMyTurn";
    public static final String SOUND_MUTE_OTHERS_TURN = "SoundMuteOthersTurn";
    public static final String TOOLTIP_DELAY = "TooltipDelay";
    public static final String TOOLTIP_DISMISS_DELAY = "TooltipDismissDelay";
    public static final String TOOLTIP_DIST_SUPRESSION = "TooltipDistSupression";
    public static final String WINDOW_POS_X = "WindowPosX";
    public static final String WINDOW_POS_Y = "WindowPosY";
    public static final String WINDOW_SIZE_HEIGHT = "WindowSizeHeight";
    public static final String WINDOW_SIZE_WIDTH = "WindowSizeWidth";
    public static final String RND_ARMY_SIZE_HEIGHT = "RndArmySizeHeight";
    public static final String RND_ARMY_SIZE_WIDTH = "RndArmySizeWidth";
    public static final String RND_ARMY_POS_X = "RndArmyPosX";
    public static final String RND_ARMY_POS_Y = "RndArmyPosY";
    public static final String RND_ARMY_SPLIT_POS = "RndArmySplitPos";
    public static final String RND_MAP_POS_X = "RndMapPosX";
    public static final String RND_MAP_POS_Y = "RndMapPosY";
    public static final String RND_MAP_SIZE_HEIGHT = "RndMapSizeHeight";
    public static final String RND_MAP_SIZE_WIDTH = "RndMapSizeWidth";
    public static final String RND_MAP_ADVANCED = "RndMapAdvanced";
    public static final String LOS_MECH_IN_FIRST = "LOSMechInFirst";
    public static final String LOS_MECH_IN_SECOND = "LOSMechInSecond";
    public static final String SHOW_MAPSHEETS = "ShowMapsheets";
    public static final String USE_ISOMETRIC = "UseIsometric";
    public static final String SHOW_UNIT_OVERVIEW = "ShowUnitOverview";
    public static final String SHOW_DAMAGE_LEVEL = "ShowDamageLevel";
    public static final String SHOW_DAMAGE_DECAL = "ShowDamageDecal";
    public static final String SKIN_FILE = "SkinFile";
    public static final String DEFAULT_WEAPON_SORT_ORDER = "DefaultWeaponSortOrder";
    public static final String UI_THEME = "UITheme";
    public static final String BOARDEDIT_LOAD_SIZE_HEIGHT = "BoardEditLoadSizeHeight";
    public static final String BOARDEDIT_LOAD_SIZE_WIDTH = "BoardEditLoadSizeWidth";
    public static final String BOARDEDIT_RNDDIALOG_START = "BoardEditRandomDialogStart";
    public static final String ALLY_UNIT_COLOR = "AllyUnitColor";
    public static final String MY_UNIT_COLOR = "MyUnitColor";
    public static final String ENEMY_UNIT_COLOR = "EnemyUnitColor";
    public static final String SHOW_KEYBINDS_OVERLAY = "ShowKeybindsOverlay";
    public static final String SHOW_PLANETARYCONDITIONS_OVERLAY = "ShowPlanetaryConditionsOverlay";
    public static final String UNIT_LABEL_STYLE = "UnitLabelStyle";
    public static final String AS_CARD_FONT = "AsCardFont";
    public static final String AS_CARD_SIZE = "AsCardSize";

    // RAT dialog preferences
    public static String RAT_TECH_LEVEL = "RATTechLevel";
    public static String RAT_BV_MIN = "RATBVMin";
    public static String RAT_BV_MAX = "RATBVMax";
    public static String RAT_NUM_MECHS = "RATNumMechs";
    public static String RAT_NUM_VEES = "RATNumVees";
    public static String RAT_NUM_BA = "RATNumBA";
    public static String RAT_NUM_INF = "RATNumInf";
    public static String RAT_YEAR_MIN = "RATYearMin";
    public static String RAT_YEAR_MAX = "RATYearMax";
    public static String RAT_PAD_BV = "RATPadBV";
    public static String RAT_SELECTED_RAT = "RATSelectedRAT";

    // common colors
    public static final Color DEFAULT_WHITE = Color.WHITE;
    public static final Color DEFAULT_BLACK = Color.BLACK;
    public static final Color DEFAULT_DARK_GRAY = new Color(64, 64, 64);
    public static final Color DEFAULT_LIGHT_GRAY = new Color(196, 196, 196);

    // Text colors that read over light and dark backgrounds
    private static final Color DEFAULT_CYAN = new Color(0, 228, 228);
    private static final Color DEFAULT_MAGENTA = new Color(228, 0, 228);
    private static final Color DEFAULT_PINK = new Color(228, 20, 147);
    private static final Color DEFAULT_RED = new Color(196, 0, 0);
    private static final Color DEFAULT_GREEN = new Color(0, 212, 0);
    private static final Color DEFAULT_BLUE = new Color(64, 96, 228);
    private static final Color DEFAULT_MEDIUM_DARK_RED = new Color(150, 80, 80);  // medium dark red
    private static final Color DEFAULT_MEDIUM_YELLOW = new Color(180, 180, 100);
    private static final Color DEFAULT_ORANGE = new Color(248, 140, 0);
    private static final Color DEFAULT_YELLOW = new Color(216, 200, 0);
    private static final Color DEFAULT_MEDIUM_GREEN = new Color(100, 180, 100);

    // Heat Scale
    private static final Color DEFAULT_HEAT_4_COLOR = new Color(64, 128, 255);
    private static final Color DEFAULT_HEAT_7_COLOR = new Color(64, 164, 128);
    private static final Color DEFAULT_HEAT_9_COLOR = new Color(48, 212, 48);
    private static final Color DEFAULT_HEAT_12_COLOR = new Color(228, 198, 0);
    private static final Color DEFAULT_HEAT_13_COLOR = new Color(248, 128, 0);
    private static final Color DEFAULT_HEAT_14_COLOR = new Color(248, 64, 64);
    private static final Color DEFAULT_HEAT_OVERHEAT_COLOR = new Color(248, 12, 12);

    private static final Color DEFAULT_PLANETARY_CONDITIONS_TEXT_COLOR = new Color(200, 250, 200);
    private static final Color DEFAULT_PLANETARY_CONDITIONS_COLD_COLOR = new Color(173, 216, 230);
    private static final Color DEFAULT_PLANETARY_CONDITIONS_HOT_COLOR = new Color(255, 204, 203);
    private static final Color DEFAULT_PLANETARY_CONDITIONS_BACKGROUND_COLOR = new Color(80, 80, 80);

    // Report Color
    private static final Color DEFAULT_REPORT_LINK_COLOR = new Color(73, 102, 230);


    // Map colors
    private static final Color DEFAULT_MAP_BRIGHT_GREEN = new Color(80, 230, 80);
    private static final Color DEFAULT_MAP_BLUE = new Color(60, 140, 240);  // greenish blue
    private static final Color DEFAULT_MAP_RED = new Color(200, 40, 40); // red
    private static final Color DEFAULT_MAP_GREEN = new Color(40, 210, 40);  // light green

    protected static GUIPreferences instance = new GUIPreferences();

    public static final int HIDE = 0;
    public static final int SHOW = 1;
    public static final int MAUNAL = 2;

    public static GUIPreferences getInstance() {
        return instance;
    }

    protected GUIPreferences() {
        store = PreferenceManager.getInstance().getPreferenceStore(getClass().getName());

        store.setDefault(ADVANCED_MECH_DISPLAY_ARMOR_LARGE_FONT_SIZE, 12);
        store.setDefault(ADVANCED_MECH_DISPLAY_ARMOR_MEDIUM_FONT_SIZE, 10);
        store.setDefault(ADVANCED_MECH_DISPLAY_ARMOR_SMALL_FONT_SIZE, 9);
        store.setDefault(ADVANCED_MECH_DISPLAY_LARGE_FONT_SIZE, 12);
        store.setDefault(ADVANCED_MECH_DISPLAY_MEDIUM_FONT_SIZE, 10);
        store.setDefault(BOARDEDIT_RNDDIALOG_START, false);
        setDefault(ADVANCED_MOVE_DEFAULT_CLIMB_MODE, true);
        setDefault(ADVANCED_MOVE_DEFAULT_COLOR, DEFAULT_CYAN.CYAN);
        setDefault(ADVANCED_MOVE_ILLEGAL_COLOR, DEFAULT_DARK_GRAY);
        setDefault(ADVANCED_MOVE_JUMP_COLOR, DEFAULT_RED);
        setDefault(ADVANCED_MOVE_MASC_COLOR, DEFAULT_ORANGE);
        setDefault(ADVANCED_MOVE_RUN_COLOR, DEFAULT_YELLOW);
        setDefault(ADVANCED_MOVE_BACK_COLOR, DEFAULT_YELLOW);
        setDefault(ADVANCED_MOVE_SPRINT_COLOR, DEFAULT_PINK);
        setDefault(ADVANCED_UNITOVERVIEW_SELECTED_COLOR, DEFAULT_MAGENTA);
        setDefault(ADVANCED_UNITOVERVIEW_VALID_COLOR, DEFAULT_CYAN);
        setDefault(ADVANCED_FIRE_SOLN_CANSEE_COLOR, DEFAULT_CYAN);
        setDefault(ADVANCED_FIRE_SOLN_NOSEE_COLOR, DEFAULT_RED);
        setDefault(ADVANCED_ARMORMINI_UNITS_PER_BLOCK, 10);
        setDefault(ADVANCED_ARMORMINI_ARMOR_CHAR, "\u2B1B"); // Centered Filled Square
        setDefault(ADVANCED_ARMORMINI_CAP_ARMOR_CHAR, "\u26CA"); // Shield
        setDefault(ADVANCED_ARMORMINI_IS_CHAR, "\u25A3"); // Centered Square with Dot
        setDefault(ADVANCED_ARMORMINI_DESTROYED_CHAR, "\u2715"); // Centered x
        setDefault(ADVANCED_ARMORMINI_COLOR_INTACT, DEFAULT_MEDIUM_GREEN);
        setDefault(ADVANCED_ARMORMINI_COLOR_PARTIAL_DMG, DEFAULT_MEDIUM_YELLOW);
        setDefault(ADVANCED_ARMORMINI_COLOR_DAMAGED, DEFAULT_MEDIUM_DARK_RED);
        setDefault(ADVANCED_ARMORMINI_FONT_SIZE_MOD, -2);
        setDefault(ADVANCED_WARNING_COLOR, DEFAULT_RED);
        setDefault(ADVANCED_TMM_PIP_MODE, 2); // show pips with colors based on move type
        setDefault(ADVANCED_LOW_FOLIAGE_COLOR, DEFAULT_MAP_BRIGHT_GREEN);
        setDefault(ADVANCED_NO_SAVE_NAG, false);
        setDefault(ADVANCED_USE_CAMO_OVERLAY, true);

        setDefault(ADVANCED_MOVE_FONT_TYPE, Font.SANS_SERIF);
        setDefault(ADVANCED_MOVE_FONT_SIZE, 26);
        setDefault(ADVANCED_MOVE_FONT_STYLE, Font.BOLD);

        store.setDefault(ADVANCED_MOVE_STEP_DELAY, 50);
        store.setDefault(ADVANCED_DARKEN_MAP_AT_NIGHT, false);
        setDefault(ADVANCED_MAPSHEET_COLOR, DEFAULT_BLUE);
        store.setDefault(ADVANCED_TRANSLUCENT_HIDDEN_UNITS, true);
        store.setDefault(ADVANCED_ATTACK_ARROW_TRANSPARENCY, 0x80);
        setDefault(ADVANCED_BUILDING_TEXT_COLOR, DEFAULT_BLUE);
        setDefault(ADVANCED_CHATBOX2_BACKCOLOR, DEFAULT_WHITE);
        store.setDefault(ADVANCED_CHATBOX2_TRANSPARANCY, 50);
        store.setDefault(ADVANCED_CHATBOX2_AUTOSLIDEDOWN, true);
        store.setDefault(ADVANCED_ECM_TRANSPARENCY, 0x80);
        store.setDefault(ADVANCED_KEY_REPEAT_DELAY, 0);
        store.setDefault(ADVANCED_KEY_REPEAT_RATE, 20);
        store.setDefault(ADVANCED_SHOW_FPS, false);
        store.setDefault(SHOW_COORDS, true);
        store.setDefault(ADVANCED_BUTTONS_PER_ROW, 12);
        store.setDefault(ADVANCED_ROUND_REPORT_SPRITES, true);

        setDefault(ADVANCED_HEAT_COLOR_4, DEFAULT_HEAT_4_COLOR);
        setDefault(ADVANCED_HEAT_COLOR_7, DEFAULT_HEAT_7_COLOR);
        setDefault(ADVANCED_HEAT_COLOR_9, DEFAULT_HEAT_9_COLOR);
        setDefault(ADVANCED_HEAT_COLOR_12, DEFAULT_HEAT_12_COLOR);
        setDefault(ADVANCED_HEAT_COLOR_13, DEFAULT_HEAT_13_COLOR);
        setDefault(ADVANCED_HEAT_COLOR_14, DEFAULT_HEAT_14_COLOR);
        setDefault(ADVANCED_HEAT_COLOR_OVERHEAT, DEFAULT_HEAT_OVERHEAT_COLOR);

        setDefault(ADVANCED_PLANETARY_CONDITIONS_COLOR_TITLE, Color.WHITE);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_COLOR_TEXT, DEFAULT_PLANETARY_CONDITIONS_TEXT_COLOR);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_COLOR_COLD, DEFAULT_PLANETARY_CONDITIONS_COLD_COLOR);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_COLOR_HOT, DEFAULT_PLANETARY_CONDITIONS_HOT_COLOR);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_COLOR_BACKGROUND, DEFAULT_PLANETARY_CONDITIONS_BACKGROUND_COLOR);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_SHOW_DEFAULTS, true);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_SHOW_HEADER, true);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_SHOW_LABELS, true);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_SHOW_VALUES, true);
        setDefault(ADVANCED_PLANETARY_CONDITIONS_SHOW_INDICATORS, true);

        setDefault(ADVANCED_REPORT_COLOR_LINK, DEFAULT_REPORT_LINK_COLOR);
        setDefault(ADVANCED_UNITTOOLTIP_SEENBYRESOLUTION, 3);
        setDefault(ADVANCED_DOCK_ON_LEFT, true);
        setDefault(ADVANCED_DOCK_MULTIPLE_ON_Y_AXIS, true);
        setDefault(ADVANCED_PLAYERS_REMAINING_TO_SHOW, 3);

        setDefault(ADVANCED_UNIT_DISPLAY_WEAPON_LIST_HEIGHT, 200);

        store.setDefault(FOV_HIGHLIGHT_RINGS_RADII, "5 10 15 20 25");
        store.setDefault(FOV_HIGHLIGHT_RINGS_COLORS_HSB, "0.3 1.0 1.0 ; 0.45 1.0 1.0 ; 0.6 1.0 1.0 ; 0.75 1.0 1.0 ; 0.9 1.0 1.0 ; 1.05 1.0 1.0 ");
        store.setDefault(FOV_HIGHLIGHT, false);
        store.setDefault(FOV_HIGHLIGHT_ALPHA, 40);
        store.setDefault(FOV_DARKEN, true);
        store.setDefault(FOV_DARKEN_ALPHA, 100);
        store.setDefault(FOV_STRIPES, 35);
        store.setDefault(FOV_GRAYSCALE, false);

        store.setDefault(ANTIALIASING, true);
        store.setDefault(AOHEXSHADOWS, false);
        store.setDefault(SHADOWMAP, true);
        store.setDefault(INCLINES, true);
        store.setDefault(FLOATINGISO, false);
        store.setDefault(LEVELHIGHLIGHT, false);

        store.setDefault(AUTO_END_FIRING, true);
        store.setDefault(AUTO_DECLARE_SEARCHLIGHT, true);
        store.setDefault(CUSTOM_UNIT_HEIGHT, 400);
        store.setDefault(CUSTOM_UNIT_WIDTH, 600);

        store.setDefault(UNIT_DISPLAY_SIZE_HEIGHT, 500);
        store.setDefault(UNIT_DISPLAY_SIZE_WIDTH, 300);
        store.setDefault(UNIT_DISPLAY_NONTABBED_SIZE_HEIGHT, 900);
        store.setDefault(UNIT_DISPLAY_NONTABBED_SIZE_WIDTH, 900);
        store.setDefault(UNIT_DISPLAY_START_TABBED, true);
        store.setDefault(UNIT_DISPLAY_SPLIT_ABC_LOC, 300);
        store.setDefault(UNIT_DISPLAY_SPLIT_BC_LOC, 300);
        store.setDefault(UNIT_DISPLAY_SPLIT_A1_LOC, 900);
        store.setDefault(UNIT_DISPLAY_SPLIT_B1_LOC, 500);
        store.setDefault(UNIT_DISPLAY_SPLIT_C1_LOC, 500);
        store.setDefault(UNIT_DISPLAY_AUTO_DISPLAY_REPORT_PHASE, 0);
        store.setDefault(UNIT_DISPLAY_AUTO_DISPLAY_NONREPORT_PHASE, 1);
        store.setDefault(UNIT_DISPLAY_ENABLED, true);
        store.setDefault(UNIT_DISPLAY_LOCATION, 0);
        store.setDefault(SPLIT_PANE_A_DIVIDER_LOCATION, 300);

        store.setDefault(GAME_SUMMARY_BOARD_VIEW, false);
        store.setDefault(ENTITY_OWNER_LABEL_COLOR, true);
        store.setDefault(UNIT_LABEL_BORDER, true);
        store.setDefault(UNIT_LABEL_STYLE, LabelDisplayStyle.NICKNAME.name());
        store.setDefault(FIRING_SOLUTIONS, true);
        store.setDefault(GUI_SCALE, 1);
        store.setDefault(LOBBY_MEKTABLE_UNIT_WIDTH, 170);
        store.setDefault(LOBBY_MEKTABLE_PILOT_WIDTH, 80);
        store.setDefault(LOBBY_MEKTABLE_PLAYER_WIDTH, 50);
        store.setDefault(LOBBY_MEKTABLE_BV_WIDTH, 50);
        setDefault(ADVANCED_MAP_TEXT_COLOR, DEFAULT_BLACK);
        store.setDefault(MAP_ZOOM_INDEX, 7);
        store.setDefault(MECH_SELECTOR_INCLUDE_MODEL, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_NAME, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_TONS, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_BV, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_YEAR, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_LEVEL, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_COST, true);
        store.setDefault(MECH_SELECTOR_UNIT_TYPE, 0);
        store.setDefault(MECH_SELECTOR_WEIGHT_CLASS, 15);
        store.setDefault(MECH_SELECTOR_RULES_LEVELS, "[0]");
        store.setDefault(MECH_SELECTOR_SORT_COLUMN, 0);
        store.setDefault(MECH_SELECTOR_SORT_ORDER, "ASCENDING");
        store.setDefault(MECH_SELECTOR_SHOW_ADVANCED, false);
        store.setDefault(MECH_SELECTOR_SIZE_HEIGHT, 600);
        store.setDefault(MECH_SELECTOR_SIZE_WIDTH, 800);
        store.setDefault(MECH_SELECTOR_POS_X, 200);
        store.setDefault(MECH_SELECTOR_POS_Y, 200);
        store.setDefault(MECH_SELECTOR_SPLIT_POS, 300);
        store.setDefault(RND_ARMY_SIZE_HEIGHT, 600);
        store.setDefault(RND_ARMY_SIZE_WIDTH, 800);
        store.setDefault(RND_ARMY_POS_X, 200);
        store.setDefault(RND_ARMY_POS_Y, 200);
        store.setDefault(RND_ARMY_SPLIT_POS, 300);

        store.setDefault(MINI_MAP_COLOURS, "defaultminimap.txt");
        store.setDefault(MINI_MAP_ENABLED, true);
        store.setDefault(MINI_MAP_AUTO_DISPLAY_REPORT_PHASE, 0);
        store.setDefault(MINI_MAP_AUTO_DISPLAY_NONREPORT_PHASE, 1);

        store.setDefault(MMSYMBOL, true);
        store.setDefault(MINIMUM_SIZE_HEIGHT, 200);
        store.setDefault(MINIMUM_SIZE_WIDTH, 120);

        store.setDefault(MINI_REPORT_POS_X, 200);
        store.setDefault(MINI_REPORT_POS_Y, 150);
        store.setDefault(MINI_REPORT_SIZE_HEIGHT, 300);
        store.setDefault(MINI_REPORT_SIZE_WIDTH, 400);
        store.setDefault(MINI_REPORT_ENABLED, true);
        store.setDefault(MINI_REPORT_AUTO_DISPLAY_REPORT_PHASE, 1);
        store.setDefault(MINI_REPORT_AUTO_DISPLAY_NONREPORT_PHASE, 0);
        store.setDefault(MINI_REPORT_LOCATION, 0);

        store.setDefault(PLAYER_LIST_ENABLED, true);
        store.setDefault(PLAYER_LIST_POS_X, 200);
        store.setDefault(PLAYER_LIST_POS_Y, 150);
        store.setDefault(PLAYER_LIST_AUTO_DISPLAY_REPORT_PHASE, 1);
        store.setDefault(PLAYER_LIST_AUTO_DISPLAY_NONREPORT_PHASE, 0);

        store.setDefault(MOUSE_WHEEL_ZOOM, true);
        store.setDefault(MOUSE_WHEEL_ZOOM_FLIP, true);

        store.setDefault(NAG_FOR_CRUSHING_BUILDINGS, true);
        store.setDefault(NAG_FOR_MAP_ED_README, true);
        store.setDefault(NAG_FOR_MASC, true);
        store.setDefault(NAG_FOR_NO_ACTION, true);
        store.setDefault(NAG_FOR_NO_UNJAMRAC, true);
        store.setDefault(NAG_FOR_PSR, true);
        store.setDefault(NAG_FOR_README, true);
        store.setDefault(NAG_FOR_SPRINT, true);
        store.setDefault(NAG_FOR_OVERHEAT, true);
        store.setDefault(NAG_FOR_LAUNCH_DOORS, true);
        store.setDefault(NAG_FOR_MECHANICAL_FALL_DAMAGE, true);
        store.setDefault(NAG_FOR_DOOMED, true);
        store.setDefault(NAG_FOR_WIGE_LANDING, true);

        setDefault(RULER_COLOR_1, DEFAULT_CYAN);
        setDefault(RULER_COLOR_2, DEFAULT_MAGENTA);
        store.setDefault(RULER_POS_X, 0);
        store.setDefault(RULER_POS_Y, 0);
        store.setDefault(RULER_SIZE_HEIGHT, 300);
        store.setDefault(RULER_SIZE_WIDTH, 500);

        store.setDefault(SCROLL_SENSITIVITY, 3);
        store.setDefault(SHOW_FIELD_OF_FIRE, true);
        store.setDefault(SHOW_MAPHEX_POPUP, true);
        store.setDefault(SHOW_MOVE_STEP, true);
        store.setDefault(SHOW_WRECKS, true);
        store.setDefault(SOUND_BING_FILENAME_CHAT, "data/sounds/call.wav");
        store.setDefault(SOUND_BING_FILENAME_MY_TURN, "data/sounds/call.wav");
        store.setDefault(SOUND_BING_FILENAME_OTHERS_TURN, "data/sounds/call.wav");
        store.setDefault(SOUND_MUTE_CHAT, true);
        store.setDefault(SOUND_MUTE_MY_TURN, false);
        store.setDefault(SOUND_MUTE_OTHERS_TURN, true);

        store.setDefault(TOOLTIP_DELAY, 1000);
        store.setDefault(TOOLTIP_DISMISS_DELAY, -1);
        store.setDefault(TOOLTIP_DIST_SUPRESSION, BoardView.HEX_DIAG);
        store.setDefault(SHOW_WPS_IN_TT, true);
        store.setDefault(SHOW_ARMOR_MINIVIS_TT, true);
        store.setDefault(SHOW_PILOT_PORTRAIT_TT, true);

        store.setDefault(USE_ISOMETRIC, false);

        store.setDefault(WINDOW_SIZE_HEIGHT, 600);
        store.setDefault(WINDOW_SIZE_WIDTH, 800);

        store.setDefault(RND_MAP_SIZE_HEIGHT, 500);
        store.setDefault(RND_MAP_SIZE_WIDTH, 500);
        store.setDefault(RND_MAP_POS_X, 400);
        store.setDefault(RND_MAP_POS_Y, 400);
        store.setDefault(RND_MAP_ADVANCED, false);
        store.setDefault(BOARDEDIT_LOAD_SIZE_WIDTH, 400);
        store.setDefault(BOARDEDIT_LOAD_SIZE_HEIGHT, 300);

        store.setDefault(SHOW_MAPSHEETS, false);

        store.setDefault(SHOW_UNIT_OVERVIEW, true);
        store.setDefault(DEFAULT_WEAPON_SORT_ORDER, WeaponSortOrder.DEFAULT.name());
        store.setDefault(SHOW_DAMAGE_LEVEL, false);
        store.setDefault(SHOW_DAMAGE_DECAL, true);
        store.setDefault(SKIN_FILE, "BW - Default.xml");
        store.setDefault(SOFTCENTER, false);
        store.setDefault(UI_THEME, UIManager.getSystemLookAndFeelClassName());

        store.setDefault(RAT_TECH_LEVEL, 0);
        store.setDefault(RAT_BV_MIN, "5800");
        store.setDefault(RAT_BV_MAX, "6000");
        store.setDefault(RAT_NUM_MECHS, "4");
        store.setDefault(RAT_NUM_VEES, "0");
        store.setDefault(RAT_NUM_BA, "0");
        store.setDefault(RAT_NUM_INF, "0");
        store.setDefault(RAT_YEAR_MIN, "2300");
        store.setDefault(RAT_YEAR_MAX, "3175");
        store.setDefault(RAT_PAD_BV, false);
        store.setDefault(RAT_SELECTED_RAT, "");

        setDefault(ALLY_UNIT_COLOR, DEFAULT_MAP_BLUE);
        setDefault(ENEMY_UNIT_COLOR, DEFAULT_MAP_RED);
        setDefault(MY_UNIT_COLOR, DEFAULT_MAP_GREEN);
        setDefault(TEAM_COLORING, true);

        setDefault(SHOW_KEYBINDS_OVERLAY, true);
        setDefault(SHOW_PLANETARYCONDITIONS_OVERLAY, true);

        setDefault(AS_CARD_FONT, "");
        setDefault(AS_CARD_SIZE, 0.75f);
    }

    public void setDefault(String name, Color color) {
        store.setDefault(name, getColorString(color));
    }

    @Override
    public String[] getAdvancedProperties() {
        return store.getAdvancedProperties();
    }

    public boolean getAntiAliasing() {
        return store.getBoolean(ANTIALIASING);
    }

    public boolean getAOHexShadows() {
        return store.getBoolean(AOHEXSHADOWS);
    }

    public boolean getFloatingIso() {
        return store.getBoolean(FLOATINGISO);
    }

    public boolean getMmSymbol() {
        return store.getBoolean(MMSYMBOL);
    }

    public boolean getShadowMap() {
        return store.getBoolean(SHADOWMAP);
    }

    public boolean getHexInclines() {
        return store.getBoolean(INCLINES);
    }

    public boolean getLevelHighlight() {
        return store.getBoolean(LEVELHIGHLIGHT);
    }

    public boolean getAutoEndFiring() {
        return store.getBoolean(AUTO_END_FIRING);
    }

    public boolean getTeamColoring() {
        return store.getBoolean(TEAM_COLORING);
    }

    public boolean getAutoDeclareSearchlight() {
        return store.getBoolean(AUTO_DECLARE_SEARCHLIGHT);
    }

    public int getCustomUnitHeight() {
        return store.getInt(CUSTOM_UNIT_HEIGHT);
    }

    public int getCustomUnitWidth() {
        return store.getInt(CUSTOM_UNIT_WIDTH);
    }

    public int getUnitDisplayPosX() {
        return store.getInt(UNIT_DISPLAY_POS_X);
    }

    public int getUnitDisplayPosY() {
        return store.getInt(UNIT_DISPLAY_POS_Y);
    }

    public int getUnitDisplayNontabbedPosX() {
        return store.getInt(UNIT_DISPLAY_NONTABBED_POS_X);
    }

    public int getUnitDisplayNontabbedPosY() {
        return store.getInt(UNIT_DISPLAY_NONTABBED_POS_Y);
    }

    public boolean getUnitDisplayStartTabbed() {
        return store.getBoolean(UNIT_DISPLAY_START_TABBED);
    }

    public int getUnitDisplaySplitABCLoc() {
        return store.getInt(UNIT_DISPLAY_SPLIT_ABC_LOC);
    }

    public int getUnitDisplaySplitBCLoc() {
        return store.getInt(UNIT_DISPLAY_SPLIT_BC_LOC);
    }

    public int getUnitDisplaySplitA1Loc() {
        return store.getInt(UNIT_DISPLAY_SPLIT_A1_LOC);
    }

    public int getUnitDisplaySplitB1Loc() {
        return store.getInt(UNIT_DISPLAY_SPLIT_B1_LOC);
    }

    public int getUnitDisplaySplitC1Loc() {
        return store.getInt(UNIT_DISPLAY_SPLIT_C1_LOC);
    }

    public int getUnitDisplaySizeHeight() {
        return store.getInt(UNIT_DISPLAY_SIZE_HEIGHT);
    }

    public int getUnitDisplaySizeWidth() {
        return store.getInt(UNIT_DISPLAY_SIZE_WIDTH);
    }

    public int getUnitDisplayNonTabbedSizeHeight() {
        return store.getInt(UNIT_DISPLAY_NONTABBED_SIZE_HEIGHT);
    }

    public int getUnitDisplayNonTabbedSizeWidth() {
        return store.getInt(UNIT_DISPLAY_NONTABBED_SIZE_WIDTH);
    }

    public int getUnitDisplayAutoDisplayReportPhase() {
        return store.getInt(UNIT_DISPLAY_AUTO_DISPLAY_REPORT_PHASE);
    }

    public int getUnitDisplayAutoDisplayNonReportPhase() {
        return store.getInt(UNIT_DISPLAY_AUTO_DISPLAY_NONREPORT_PHASE);
    }

    public boolean getUnitDisplayEnabled() {
        return store.getBoolean(UNIT_DISPLAY_ENABLED);
    }

    public int getUnitDisplayWeaponListHeight() {
        return store.getInt(ADVANCED_UNIT_DISPLAY_WEAPON_LIST_HEIGHT);
    }

    public int getUnitDisplayLocaton() {
        return store.getInt(UNIT_DISPLAY_LOCATION);
    }

    public int getSplitPaneADividerLocaton() {
        return store.getInt(SPLIT_PANE_A_DIVIDER_LOCATION);
    }

    public boolean getCoordsEnabled() {
        return store.getBoolean(SHOW_COORDS);
    }

    public boolean getGameSummaryBoardView() {
        return store.getBoolean(GAME_SUMMARY_BOARD_VIEW);
    }

    public boolean getGameSummaryMinimap() {
        return store.getBoolean(GAME_SUMMARY_MINIMAP);
    }

    public boolean getEntityOwnerLabelColor() {
        return store.getBoolean(ENTITY_OWNER_LABEL_COLOR);
    }

    public boolean getUnitLabelBorder() {
        return store.getBoolean(UNIT_LABEL_BORDER);
    }

    public boolean getFocus() {
        return store.getBoolean(FOCUS);
    }

    public boolean getFiringSolutions() {
        return store.getBoolean(FIRING_SOLUTIONS);
    }

    public boolean getMoveEnvelope() {
        return store.getBoolean(MOVE_ENVELOPE);
    }

    public boolean getFovHighlight() {
        return store.getBoolean(FOV_HIGHLIGHT);
    }

    public int getFovHighlightAlpha() {
        return store.getInt(FOV_HIGHLIGHT_ALPHA);
    }

    public String getFovHighlightRingsRadii() {
        return store.getString(FOV_HIGHLIGHT_RINGS_RADII);
    }

    public String getFovHighlightRingsColorsHsb() {
        return store.getString(FOV_HIGHLIGHT_RINGS_COLORS_HSB);
    }

    public boolean getFovDarken() {
        return store.getBoolean(FOV_DARKEN);
    }

    public int getFovDarkenAlpha() {
        return store.getInt(FOV_DARKEN_ALPHA);
    }

    public int getFovStripes() {
        return store.getInt(FOV_STRIPES);
    }

    public boolean getFovGrayscale() {
        return store.getBoolean(FOV_GRAYSCALE);
    }

    public Color getMapTextColor() {
        return getColor(ADVANCED_MAP_TEXT_COLOR);
    }

    public int getMapZoomIndex() {
        return store.getInt(MAP_ZOOM_INDEX);
    }

    public boolean getMechSelectorIncludeModel() {
        return store.getBoolean(MECH_SELECTOR_INCLUDE_MODEL);
    }

    public boolean getMechSelectorIncludeName() {
        return store.getBoolean(MECH_SELECTOR_INCLUDE_NAME);
    }

    public boolean getMechSelectorIncludeTons() {
        return store.getBoolean(MECH_SELECTOR_INCLUDE_TONS);
    }

    public boolean getMechSelectorIncludeBV() {
        return store.getBoolean(MECH_SELECTOR_INCLUDE_BV);
    }

    public boolean getMechSelectorIncludeYear() {
        return store.getBoolean(MECH_SELECTOR_INCLUDE_YEAR);
    }

    public boolean getMechSelectorIncludeLevel() {
        return store.getBoolean(MECH_SELECTOR_INCLUDE_LEVEL);
    }

    public boolean getMechSelectorIncludeCost() {
        return store.getBoolean(MECH_SELECTOR_INCLUDE_COST);
    }

    public boolean getMechSelectorShowAdvanced() {
        return store.getBoolean(MECH_SELECTOR_SHOW_ADVANCED);
    }

    public int getMechSelectorUnitType() {
        return store.getInt(MECH_SELECTOR_UNIT_TYPE);
    }

    public int getMechSelectorWeightClass() {
        return store.getInt(MECH_SELECTOR_WEIGHT_CLASS);
    }

    public String getMechSelectorRulesLevels() {
        return store.getString(MECH_SELECTOR_RULES_LEVELS);
    }

    public int getMechSelectorSortColumn() {
        return store.getInt(MECH_SELECTOR_SORT_COLUMN);
    }

    public int getMechSelectorDefaultSortColumn() {
        return store.getDefaultInt(MECH_SELECTOR_SORT_COLUMN);
    }

    public String getMechSelectorSortOrder() {
        return store.getString(MECH_SELECTOR_SORT_ORDER);
    }

    public String getMechSelectorDefaultSortOrder() {
        return store.getDefaultString(MECH_SELECTOR_SORT_ORDER);
    }

    public int getMechSelectorSizeHeight() {
        return store.getInt(MECH_SELECTOR_SIZE_HEIGHT);
    }

    public int getMechSelectorSizeWidth() {
        return store.getInt(MECH_SELECTOR_SIZE_WIDTH);
    }

    public int getMechSelectorPosX() {
        return store.getInt(MECH_SELECTOR_POS_X);
    }

    public int getMechSelectorPosY() {
        return store.getInt(MECH_SELECTOR_POS_Y);
    }

    public int getMechSelectorSplitPos() {
        return store.getInt(MECH_SELECTOR_SPLIT_POS);
    }

    public int getRndArmySizeHeight() {
        return store.getInt(RND_ARMY_SIZE_HEIGHT);
    }

    public int getRndArmySizeWidth() {
        return store.getInt(RND_ARMY_SIZE_WIDTH);
    }

    public int getRndArmyPosX() {
        return store.getInt(RND_ARMY_POS_X);
    }

    public int getRndArmyPosY() {
        return store.getInt(RND_ARMY_POS_Y);
    }

    public int getRndArmySplitPos() {
        return store.getInt(RND_ARMY_SPLIT_POS);
    }

    public String getMinimapColours() {
        return store.getString(MINI_MAP_COLOURS);
    }

    public boolean getMinimapEnabled() {
        return store.getBoolean(MINI_MAP_ENABLED);
    }

    public int getMinimapAutoDisplayReportPhase() {
        return store.getInt(MINI_MAP_AUTO_DISPLAY_REPORT_PHASE);
    }

    public int getMinimapAutoDisplayNonReportPhase() {
        return store.getInt(MINI_MAP_AUTO_DISPLAY_NONREPORT_PHASE);
    }

    public int getMinimapPosX() {
        return store.getInt(MINI_MAP_POS_X);
    }

    public int getMinimapPosY() {
        return store.getInt(MINI_MAP_POS_Y);
    }

    public int getMinimapZoom() {
        return store.getInt(MINI_MAP_ZOOM);
    }

    public int getMinimapHeightDisplayMode() {
        return store.getInt(MINI_MAP_HEIGHT_DISPLAY_MODE);
    }

    public boolean getMiniReportEnabled() {
        return store.getBoolean(MINI_REPORT_ENABLED);
    }

    public int getMiniReportAutoDisplayReportPhase() {
        return store.getInt(MINI_REPORT_AUTO_DISPLAY_REPORT_PHASE);
    }

    public int getMiniReportAutoDisplayNonReportPhase() {
        return store.getInt(MINI_REPORT_AUTO_DISPLAY_NONREPORT_PHASE);
    }

    public int getMiniReportLocaton() {
        return store.getInt(MINI_REPORT_LOCATION);
    }

    public boolean getPlayerListEnabled() {
        return store.getBoolean(PLAYER_LIST_ENABLED);
    }

    public int getPlayerListPosX() {
        return store.getInt(PLAYER_LIST_POS_X);
    }

    public int getPlayerListPosY() {
        return store.getInt(PLAYER_LIST_POS_Y);
    }

    public int getPlayerListAutoDisplayReportPhase() {
        return store.getInt(PLAYER_LIST_AUTO_DISPLAY_REPORT_PHASE);
    }

    public int getPlayerListAutoDisplayNonReportPhase() {
        return store.getInt(PLAYER_LIST_AUTO_DISPLAY_NONREPORT_PHASE);
    }

    public boolean getIsometricEnabled() {
        return store.getBoolean(USE_ISOMETRIC);
    }

    public int getMinimumSizeHeight() {
        return store.getInt(MINIMUM_SIZE_HEIGHT);
    }

    public int getMinimumSizeWidth() {
        return store.getInt(MINIMUM_SIZE_WIDTH);
    }

    public int getBoardEditLoadHeight() {
        return store.getInt(BOARDEDIT_LOAD_SIZE_HEIGHT);
    }

    public int getBoardEditLoadWidth() {
        return store.getInt(BOARDEDIT_LOAD_SIZE_WIDTH);
    }

    public int getMiniReportPosX() {
        return store.getInt(MINI_REPORT_POS_X);
    }

    public int getMiniReportPosY() {
        return store.getInt(MINI_REPORT_POS_Y);
    }

    public int getMiniReportSizeHeight() {
        return store.getInt(MINI_REPORT_SIZE_HEIGHT);
    }

    public int getMiniReportSizeWidth() {
        return store.getInt(MINI_REPORT_SIZE_WIDTH);
    }

    public boolean getMouseWheelZoom() {
        return store.getBoolean(MOUSE_WHEEL_ZOOM);
    }

    public boolean getMouseWheelZoomFlip() {
        return store.getBoolean(MOUSE_WHEEL_ZOOM_FLIP);
    }

    public boolean getNagForCrushingBuildings() {
        return store.getBoolean(NAG_FOR_CRUSHING_BUILDINGS);
    }

    public boolean getNagForMapEdReadme() {
        return store.getBoolean(NAG_FOR_MAP_ED_README);
    }

    public boolean getNagForMASC() {
        return store.getBoolean(NAG_FOR_MASC);
    }

    public boolean getNagForNoAction() {
        return store.getBoolean(NAG_FOR_NO_ACTION);
    }

    public boolean getNagForNoUnJamRAC() {
        return store.getBoolean(NAG_FOR_NO_UNJAMRAC);
    }

    public boolean getNagForPSR() {
        return store.getBoolean(NAG_FOR_PSR);
    }

    public boolean getNagForReadme() {
        return store.getBoolean(NAG_FOR_README);
    }

    public boolean getNagForSprint() {
        return store.getBoolean(NAG_FOR_SPRINT);
    }

    public boolean getNagForOverheat() {
        return store.getBoolean(NAG_FOR_OVERHEAT);
    }

    public boolean getNagForLaunchDoors() {
        return store.getBoolean(NAG_FOR_LAUNCH_DOORS);
    }

    public boolean getNagForMechanicalJumpFallDamage() {
        return store.getBoolean(NAG_FOR_MECHANICAL_FALL_DAMAGE);
    }

    public boolean getNagForDoomed() {
        return store.getBoolean(NAG_FOR_DOOMED);
    }

    public boolean getNagForWiGELanding() {
        return store.getBoolean(NAG_FOR_WIGE_LANDING);
    }

    public Color getRulerColor1() {
        return getColor(RULER_COLOR_1);
    }

    public Color getRulerColor2() {
        return getColor(RULER_COLOR_2);
    }

    public int getRulerPosX() {
        return store.getInt(RULER_POS_X);
    }

    public int getRulerPosY() {
        return store.getInt(RULER_POS_Y);
    }

    public int getRulerSizeHeight() {
        return store.getInt(RULER_SIZE_HEIGHT);
    }

    public int getRulerSizeWidth() {
        return store.getInt(RULER_SIZE_WIDTH);
    }

    public int getScrollSensitivity() {
        return store.getInt(SCROLL_SENSITIVITY);
    }

    public boolean getShowFieldOfFire() {
        return store.getBoolean(SHOW_FIELD_OF_FIRE);
    }

    public boolean getShowMapHexPopup() {
        return store.getBoolean(SHOW_MAPHEX_POPUP);
    }

    public boolean getShowWpsinTT() {
        return store.getBoolean(SHOW_WPS_IN_TT);
    }

    public boolean getshowArmorMiniVisTT() {
        return store.getBoolean(SHOW_ARMOR_MINIVIS_TT);
    }

    public boolean getshowPilotPortraitTT() {
        return store.getBoolean(SHOW_PILOT_PORTRAIT_TT);
    }

    public boolean getShowMoveStep() {
        return store.getBoolean(SHOW_MOVE_STEP);
    }

    public boolean getShowWrecks() {
        return store.getBoolean(SHOW_WRECKS);
    }

    public String getSoundBingFilenameChat() {
        return store.getString(SOUND_BING_FILENAME_CHAT);
    }

    public String getSoundBingFilenameMyTurn() {
        return store.getString(SOUND_BING_FILENAME_MY_TURN);
    }

    public String getSoundBingFilenameOthersTurn() {
        return store.getString(SOUND_BING_FILENAME_OTHERS_TURN);
    }

    public boolean getSoundMuteChat() {
        return store.getBoolean(SOUND_MUTE_CHAT);
    }

    public boolean getSoundMuteMyTurn() {
        return store.getBoolean(SOUND_MUTE_MY_TURN);
    }

    public boolean getSoundMuteOthersTurn() {
        return store.getBoolean(SOUND_MUTE_OTHERS_TURN);
    }

    public int getTooltipDelay() {
        return store.getInt(TOOLTIP_DELAY);
    }

    public int getTooltipDismissDelay() {
        return store.getInt(TOOLTIP_DISMISS_DELAY);
    }

    public int getTooltipDistSuppression() {
        return store.getInt(TOOLTIP_DIST_SUPRESSION);
    }

    public float getGUIScale() {
        return store.getFloat(GUI_SCALE);
    }

    public int getWindowPosX() {
        return store.getInt(WINDOW_POS_X);
    }

    public int getWindowPosY() {
        return store.getInt(WINDOW_POS_Y);
    }

    public int getWindowSizeHeight() {
        return store.getInt(WINDOW_SIZE_HEIGHT);
    }

    public int getWindowSizeWidth() {
        return store.getInt(WINDOW_SIZE_WIDTH);
    }

    public boolean getMechInFirst() {
        return store.getBoolean(LOS_MECH_IN_FIRST);
    }

    public boolean getMechInSecond() {
        return store.getBoolean(LOS_MECH_IN_SECOND);
    }

    public boolean getShowMapsheets() {
        return store.getBoolean(SHOW_MAPSHEETS);
    }

    public boolean getShowUnitOverview() {
        return store.getBoolean(SHOW_UNIT_OVERVIEW);
    }

    public String getSkinFile() {
        return store.getString(SKIN_FILE);
    }

    public String getUITheme() {
        return store.getString(UI_THEME);
    }

    public WeaponSortOrder getDefaultWeaponSortOrder() {
        return WeaponSortOrder.valueOf(store.getString(DEFAULT_WEAPON_SORT_ORDER));
    }

    public String getAsCardFont() {
        return store.getString(AS_CARD_FONT);
    }

    public float getAsCardSize() {
        return store.getFloat(AS_CARD_SIZE);
    }

    public void setDefaultWeaponSortOrder(final WeaponSortOrder weaponSortOrder) {
        store.setValue(DEFAULT_WEAPON_SORT_ORDER, weaponSortOrder.name());
    }

    public boolean getBoardEdRndStart() {
        return store.getBoolean(BOARDEDIT_RNDDIALOG_START);
    }

    public void setAntiAliasing(boolean state) {
        store.setValue(ANTIALIASING, state);
    }

    public void setShadowMap(boolean state) {
        store.setValue(SHADOWMAP, state);
    }

    public void setHexInclines(boolean state) {
        store.setValue(INCLINES, state);
    }

    public void setAOHexShadows(boolean state) {
        store.setValue(AOHEXSHADOWS, state);
    }

    public void setFloatingIso(boolean state) {
        store.setValue(FLOATINGISO, state);
    }

    public void setMmSymbol(boolean state) {
        store.setValue(MMSYMBOL, state);
    }

    public void setLevelHighlight(boolean state) {
        store.setValue(LEVELHIGHLIGHT, state);
    }

    public boolean getShowDamageLevel() {
        return store.getBoolean(SHOW_DAMAGE_LEVEL);
    }

    public boolean getShowDamageDecal() {
        return store.getBoolean(SHOW_DAMAGE_DECAL);
    }

    public void setAutoEndFiring(boolean state) {
        store.setValue(AUTO_END_FIRING, state);
    }

    public void setAutoDeclareSearchlight(boolean state) {
        store.setValue(AUTO_DECLARE_SEARCHLIGHT, state);
    }

    public void setCustomUnitHeight(int state) {
        store.setValue(CUSTOM_UNIT_HEIGHT, state);
    }

    public void setCustomUnitWidth(int state) {
        store.setValue(CUSTOM_UNIT_WIDTH, state);
    }

    public void setUnitDisplayPosX(int i) {
        store.setValue(UNIT_DISPLAY_POS_X, i);
    }

    public void setUnitDisplayPosY(int i) {
        store.setValue(UNIT_DISPLAY_POS_Y, i);
    }

    public void setUnitDisplayNontabbedPosX(int i) {
        store.setValue(UNIT_DISPLAY_NONTABBED_POS_X, i);
    }

    public void setUnitDisplayNontabbedPosY(int i) {
        store.setValue(UNIT_DISPLAY_NONTABBED_POS_Y, i);
    }
    public void setUnitDisplayStartTabbed(boolean state) {
        store.setValue(UNIT_DISPLAY_START_TABBED, state);
    }

    public void setUnitDisplaySplitABCLoc(int i) {
        store.setValue(UNIT_DISPLAY_SPLIT_ABC_LOC, i);
    }

    public void setUnitDisplaySplitBCLoc(int i) {
        store.setValue(UNIT_DISPLAY_SPLIT_BC_LOC, i);
    }

    public void setUnitDisplaySplitA1Loc(int i) {
        store.setValue(UNIT_DISPLAY_SPLIT_A1_LOC, i);
    }

    public void setUnitDisplaySplitB1Loc(int i) {
        store.setValue(UNIT_DISPLAY_SPLIT_B1_LOC, i);
    }

    public void setUnitDisplaySplitC2Loc(int i) {
        store.setValue(UNIT_DISPLAY_SPLIT_C1_LOC, i);
    }

    public void setUnitDisplaySizeHeight(int i) {
        store.setValue(UNIT_DISPLAY_SIZE_HEIGHT, i);
    }

    public void setUnitDisplaySizeWidth(int i) {
        store.setValue(UNIT_DISPLAY_SIZE_WIDTH, i);
    }

    public void setUnitDisplayNonTabbedSizeHeight(int i) {
        store.setValue(UNIT_DISPLAY_NONTABBED_SIZE_HEIGHT, i);
    }

    public void setUnitDisplayNonTabbedSizeWidth(int i) {
        store.setValue(UNIT_DISPLAY_NONTABBED_SIZE_WIDTH, i);
    }

    public void setUnitDisplayAutoDisplayReportPhase(int i) {
        store.setValue(UNIT_DISPLAY_AUTO_DISPLAY_REPORT_PHASE, i);
    }

    public void setUnitDisplayAutoDisplayNonReportPhase(int i) {
        store.setValue(UNIT_DISPLAY_AUTO_DISPLAY_NONREPORT_PHASE, i);
    }

    public void toggleUnitDisplay() {
        store.setValue(UNIT_DISPLAY_ENABLED, !getBoolean(UNIT_DISPLAY_ENABLED));
    }

    public void setUnitDisplayEnabled(boolean b) {
        store.setValue(UNIT_DISPLAY_ENABLED, b);
    }

    public void setAdvancedUnitDisplayWeaponListHeight(int i) {
        store.setValue(ADVANCED_UNIT_DISPLAY_WEAPON_LIST_HEIGHT, i);
    }

    public void toggleUnitDisplayLocation() {
        store.setValue(UNIT_DISPLAY_LOCATION, ((getInt(UNIT_DISPLAY_LOCATION)+1)%2));
    }

    public void setUnitDisplayLocation(int i) {
        store.setValue(UNIT_DISPLAY_LOCATION, i);
    }

    public void setSplitPaneALocation(int i) {
        store.setValue(SPLIT_PANE_A_DIVIDER_LOCATION, i);
    }

    public void toggleCoords() {
        store.setValue(SHOW_COORDS, !getBoolean(SHOW_COORDS));
    }

    public void setCoordsEnabled(boolean b) {
        store.setValue(SHOW_COORDS, b);
    }

    public void setGameSummaryBoardView(boolean state) {
        store.setValue(GAME_SUMMARY_BOARD_VIEW, state);
    }

    public void setGameSummaryMinimap(boolean state) {
        store.setValue(GAME_SUMMARY_MINIMAP, state);
    }

    public void setEntityOwnerLabelColor(boolean i) {
        store.setValue(ENTITY_OWNER_LABEL_COLOR, i);
    }

    public void setUnitLabelBorder(boolean i) {
        store.setValue(UNIT_LABEL_BORDER, i);
    }

    public void setGetFocus(boolean state) {
        store.setValue(FOCUS, state);
    }

    public void setFiringSolutions(boolean state) {
        store.setValue(FIRING_SOLUTIONS, state);
    }

    public void setMoveEnvelope(boolean state) {
        store.setValue(MOVE_ENVELOPE, state);
    }

    public void setFovHighlight(boolean state) {
        store.setValue(FOV_HIGHLIGHT, state);
    }

    public void setFovHighlightAlpha(int i) {
        store.setValue(FOV_HIGHLIGHT_ALPHA, i);
    }

    public void setFovHighlightRingsRadii(String s) {
        store.setValue(FOV_HIGHLIGHT_RINGS_RADII, s);
    }

    public void setFovHighlightRingsColorsHsb(String s) {
        store.setValue(FOV_HIGHLIGHT_RINGS_COLORS_HSB, s);
    }

    public void setFovDarken(boolean state) {
        store.setValue(FOV_DARKEN, state);
    }

    public void setFovDarkenAlpha(int i) {
        store.setValue(FOV_DARKEN_ALPHA, i);
    }

    public void setFovStripes(int i) {
        store.setValue(FOV_STRIPES, i);
    }

    public void setFovGrayscale(boolean state) {
        store.setValue(FOV_GRAYSCALE, state);
    }

    public void setMapZoomIndex(int zoomIndex) {
        store.setValue(MAP_ZOOM_INDEX, zoomIndex);
    }

    public void setMechSelectorIncludeModel(boolean includeModel) {
        store.setValue(MECH_SELECTOR_INCLUDE_MODEL, includeModel);
    }

    public void setMechSelectorIncludeName(boolean includeName) {
        store.setValue(MECH_SELECTOR_INCLUDE_NAME, includeName);
    }

    public void setMechSelectorIncludeTons(boolean includeTons) {
        store.setValue(MECH_SELECTOR_INCLUDE_TONS, includeTons);
    }

    public void setMechSelectorIncludeBV(boolean includeBV) {
        store.setValue(MECH_SELECTOR_INCLUDE_BV, includeBV);
    }

    public void setMechSelectorIncludeYear(boolean includeYear) {
        store.setValue(MECH_SELECTOR_INCLUDE_YEAR, includeYear);
    }

    public void setMechSelectorIncludeLevel(boolean includeLevel) {
        store.setValue(MECH_SELECTOR_INCLUDE_LEVEL, includeLevel);
    }

    public void setMechSelectorIncludeCost(boolean includeCost) {
        store.setValue(MECH_SELECTOR_INCLUDE_COST, includeCost);
    }

    public void setMechSelectorShowAdvanced(boolean showAdvanced) {
        store.setValue(MECH_SELECTOR_SHOW_ADVANCED, showAdvanced);
    }

    public void setMechSelectorUnitType(int unitType) {
        store.setValue(MECH_SELECTOR_UNIT_TYPE, unitType);
    }

    public void setMechSelectorWeightClass(int weightClass) {
        store.setValue(MECH_SELECTOR_WEIGHT_CLASS, weightClass);
    }

    public void setMechSelectorRulesLevels(String rulesLevels) {
        store.setValue(MECH_SELECTOR_RULES_LEVELS, rulesLevels);
    }

    public void setMechSelectorSortColumn(int columnId) {
        store.setValue(MECH_SELECTOR_SORT_COLUMN, columnId);
    }

    public void setMechSelectorSortOrder(String order) {
        store.setValue(MECH_SELECTOR_SORT_ORDER, order);
    }

    public void setMechSelectorSizeHeight(int i) {
        store.setValue(MECH_SELECTOR_SIZE_HEIGHT, i);
    }

    public void setMechSelectorSizeWidth(int i) {
        store.setValue(MECH_SELECTOR_SIZE_WIDTH, i);
    }

    public void setMechSelectorPosX(int i) {
        store.setValue(MECH_SELECTOR_POS_X, i);
    }

    public void setMechSelectorSplitPos(int i) {
        store.setValue(MECH_SELECTOR_SPLIT_POS, i);
    }

    public void setMechSelectorPosY(int i) {
        store.setValue(MECH_SELECTOR_POS_Y, i);
    }

    public void setRndArmySizeHeight(int i) {
        store.setValue(RND_ARMY_SIZE_HEIGHT, i);
    }

    public void setRndArmySizeWidth(int i) {
        store.setValue(RND_ARMY_SIZE_WIDTH, i);
    }

    public void setRndArmyPosX(int i) {
        store.setValue(RND_ARMY_POS_X, i);
    }

    public void setRndArmySplitPos(int i) {
        store.setValue(RND_ARMY_SPLIT_POS, i);
    }

    public void setRndArmyPosY(int i) {
        store.setValue(RND_ARMY_POS_Y, i);
    }

    public void setMinimapEnabled(boolean b) {
        store.setValue(MINI_MAP_ENABLED, b);
    }

    public void toggleMinimapEnabled() {
        setMinimapEnabled(!getMinimapEnabled());
    }

    public void setMinimapPosX(int i) {
        store.setValue(MINI_MAP_POS_X, i);
    }

    public void setMinimapPosY(int i) {
        store.setValue(MINI_MAP_POS_Y, i);
    }

    public void setMinimapZoom(int zoom) {
        store.setValue(MINI_MAP_ZOOM, zoom);
    }

    public void setMinimapHeightDisplayMode(int zoom) {
        store.setValue(MINI_MAP_HEIGHT_DISPLAY_MODE, zoom);
    }

    public void setMinimapAutoDisplayReportPhase(int i) {
        store.setValue(MINI_MAP_AUTO_DISPLAY_REPORT_PHASE, i);
    }

    public void setMinimapAutoDisplayNonReportPhase(int i) {
        store.setValue(MINI_MAP_AUTO_DISPLAY_NONREPORT_PHASE, i);
    }

    public void setMiniReportEnabled(boolean b) {
        store.setValue(MINI_REPORT_ENABLED, b);
    }

    public void toggleRoundReportEnabled() {
        setMiniReportEnabled(!getMiniReportEnabled());
    }

    public void setMiniReportPosX(int i) {
        store.setValue(MINI_REPORT_POS_X, i);
    }

    public void setMiniReportPosY(int i) {
        store.setValue(MINI_REPORT_POS_Y, i);
    }

    public void setMiniReportAutoDisplayReportPhase(int i) {
        store.setValue(MINI_REPORT_AUTO_DISPLAY_REPORT_PHASE, i);
    }

    public void setMiniReportAutoDisplayNonReportPhase(int i) {
        store.setValue(MINI_REPORT_AUTO_DISPLAY_NONREPORT_PHASE, i);
    }

    public void toggleMiniReportLocation() {
        store.setValue(MINI_REPORT_LOCATION, ((getInt(MINI_REPORT_LOCATION)+1)%2));
    }

    public void setMiniReportLocation(int i) {
        store.setValue(MINI_REPORT_LOCATION, i);
    }

    public void setPlayerListEnabled(boolean b) {
        store.setValue(PLAYER_LIST_ENABLED, b);
    }

    public void togglePlayerListEnabled() {
        setPlayerListEnabled(!getPlayerListEnabled());
    }

    public void setPlayerListPosX(int i) {
        store.setValue(PLAYER_LIST_POS_X, i);
    }

    public void setPlayerListPosY(int i) {
        store.setValue(PLAYER_LIST_POS_Y, i);
    }

    public void setPlayerListAutoDisplayReportPhase(int i) {
        store.setValue(PLAYER_LIST_AUTO_DISPLAY_REPORT_PHASE, i);
    }

    public void setPlayerListAutoDisplayNonReportPhase(int i) {
        store.setValue(PLAYER_LIST_AUTO_DISPLAY_NONREPORT_PHASE, i);
    }

    public void setBoardEditLoadHeight(int i) {
        store.setValue(BOARDEDIT_LOAD_SIZE_HEIGHT, i);
    }

    public void setBoardEditLoadWidth(int i) {
        store.setValue(BOARDEDIT_LOAD_SIZE_WIDTH, i);
    }

    public void setTeamColoring(boolean bt) {
        store.setValue(TEAM_COLORING, bt);
    }

    public void setMiniReportSizeHeight(int i) {
        store.setValue(MINI_REPORT_SIZE_HEIGHT, i);
    }

    public void setMiniReportSizeWidth(int i) {
        store.setValue(MINI_REPORT_SIZE_WIDTH, i);
    }

    public void setMouseWheelZoom(boolean b) {
        store.setValue(MOUSE_WHEEL_ZOOM, b);
    }

    public void setMouseWheelZoomFlip(boolean b) {
        store.setValue(MOUSE_WHEEL_ZOOM_FLIP, b);
    }

    public void setNagForCrushingBuildings(boolean b) {
        store.setValue(NAG_FOR_CRUSHING_BUILDINGS, b);
    }

    public void setNagForMapEdReadme(boolean b) {
        store.setValue(NAG_FOR_MAP_ED_README, b);
    }

    public void setNagForMASC(boolean b) {
        store.setValue(NAG_FOR_MASC, b);
    }

    public void setNagForNoAction(boolean b) {
        store.setValue(NAG_FOR_NO_ACTION, b);
    }

    public void setNagForNoUnJamRAC(boolean b) {
        store.setValue(NAG_FOR_NO_UNJAMRAC, b);
    }

    public void setNagForPSR(boolean b) {
        store.setValue(NAG_FOR_PSR, b);
    }

    public void setNagForReadme(boolean b) {
        store.setValue(NAG_FOR_README, b);
    }

    public void setNagForSprint(boolean b) {
        store.setValue(NAG_FOR_SPRINT, b);
    }

    public void setNagForOverheat(boolean b) {
        store.setValue(NAG_FOR_OVERHEAT, b);
    }

    public void setNagForLaunchDoors(boolean b) {
        store.setValue(NAG_FOR_LAUNCH_DOORS, b);
    }

    public void setNagForMechanicalJumpFallDamage(boolean b) {
        store.setValue(NAG_FOR_MECHANICAL_FALL_DAMAGE, b);
    }

    public void setNagForDoomed(boolean b) {
        store.setValue(NAG_FOR_DOOMED, b);
    }

    public void setNagForWiGELanding(boolean b) {
        store.setValue(NAG_FOR_WIGE_LANDING, b);
    }

    public void setRulerPosX(int i) {
        store.setValue(RULER_POS_X, i);
    }

    public void setRulerPosY(int i) {
        store.setValue(RULER_POS_Y, i);
    }

    public void setRulerSizeHeight(int i) {
        store.setValue(RULER_SIZE_HEIGHT, i);
    }

    public void setRulerSizeWidth(int i) {
        store.setValue(RULER_SIZE_WIDTH, i);
    }

    public void setScrollSensitivity(int i) {
        store.setValue(SCROLL_SENSITIVITY, i);
    }

    public void setShowFieldOfFire(boolean state) {
        store.setValue(SHOW_FIELD_OF_FIRE, state);
    }

    public void setShowMapHexPopup(boolean state) {
        store.setValue(SHOW_MAPHEX_POPUP, state);
    }

    public void setShowWpsinTT(boolean state) {
        store.setValue(SHOW_WPS_IN_TT, state);
    }

    public void setshowArmorMiniVisTT(boolean state) {
        store.setValue(SHOW_ARMOR_MINIVIS_TT, state);
    }

    public void setshowPilotPortraitTT(boolean state) {
        store.setValue(SHOW_PILOT_PORTRAIT_TT, state);
    }

    public void setShowMoveStep(boolean state) {
        store.setValue(SHOW_MOVE_STEP, state);
    }

    public void setShowWrecks(boolean state) {
        store.setValue(SHOW_WRECKS, state);
    }

    public void setSoundBingFilenameChat(String name) {
        store.setValue(SOUND_BING_FILENAME_CHAT, name);
    }

    public void setSoundBingFilenameMyTurn(String name) {
        store.setValue(SOUND_BING_FILENAME_MY_TURN, name);
    }

    public void setSoundBingFilenameOthersTurn(String name) {
        store.setValue(SOUND_BING_FILENAME_OTHERS_TURN, name);
    }

    public void setSoundMuteChat(boolean state) {
        store.setValue(SOUND_MUTE_CHAT, state);
    }

    public void setSoundMuteMyTurn(boolean state) {
        store.setValue(SOUND_MUTE_MY_TURN, state);
    }

    public void setSoundMuteOthersTurn(boolean state) {
        store.setValue(SOUND_MUTE_OTHERS_TURN, state);
    }

    public void setTooltipDelay(int i) {
        store.setValue(TOOLTIP_DELAY, i);
        ToolTipManager.sharedInstance().setInitialDelay(i);
    }

    public void setTooltipDismissDelay(int i) {
        store.setValue(TOOLTIP_DISMISS_DELAY, i);
        if (i > 0) {
            ToolTipManager.sharedInstance().setDismissDelay(i);
        }
    }

    public void setTooltipDistSuppression(int i) {
        store.setValue(TOOLTIP_DIST_SUPRESSION, i);
    }

    public void setWindowPosX(int i) {
        store.setValue(WINDOW_POS_X, i);
    }

    public void setWindowPosY(int i) {
        store.setValue(WINDOW_POS_Y, i);
    }

    public void setWindowSizeHeight(int i) {
        store.setValue(WINDOW_SIZE_HEIGHT, i);
    }

    public void setWindowSizeWidth(int i) {
        store.setValue(WINDOW_SIZE_WIDTH, i);
    }

    public void setMechInFirst(boolean b) {
        store.setValue(LOS_MECH_IN_FIRST, b);
    }

    public void setMechInSecond(boolean b) {
        store.setValue(LOS_MECH_IN_SECOND, b);
    }

    public void setShowMapsheets(boolean b) {
        store.setValue(SHOW_MAPSHEETS, b);
    }

    public void setIsometricEnabled(boolean b) {
        store.setValue(USE_ISOMETRIC, b);
    }

    public void setShowUnitOverview(boolean b) {
        store.setValue(SHOW_UNIT_OVERVIEW, b);
    }

    public void setShowDamageLevel(boolean b) {
        store.setValue(SHOW_DAMAGE_LEVEL, b);
    }

    public void setShowDamageDecal(boolean b) {
        store.setValue(SHOW_DAMAGE_DECAL, b);
    }

    public void setSkinFile(String s) {
        store.setValue(SKIN_FILE, s);
    }

    public void setUITheme(String s) {
        store.setValue(UI_THEME, s);
    }

    public void setAsCardFont(String asCardFont) {
        store.setValue(AS_CARD_FONT, asCardFont);
    }

    public void setAsCardSize(float size) {
        store.setValue(AS_CARD_SIZE, size);
    }

    public int getRATTechLevel() {
        return store.getInt(RAT_TECH_LEVEL);
    }

    public void setRATTechLevel(int v) {
        store.setValue(RAT_TECH_LEVEL, v);
    }

    public String getRATBVMin() {
        return store.getString(RAT_BV_MIN);
    }

    public void setRATBVMin(String v) {
        store.setValue(RAT_BV_MIN, v);
    }

    public String getRATBVMax() {
        return store.getString(RAT_BV_MAX);
    }

    public void setRATBVMax(String v) {
        store.setValue(RAT_BV_MAX, v);
    }

    public String getRATNumMechs() {
        return store.getString(RAT_NUM_MECHS);
    }

    public void setRATNumMechs(String v) {
        store.setValue(RAT_NUM_MECHS, v);
    }

    public String getRATNumVees() {
        return store.getString(RAT_NUM_VEES);
    }

    public void setRATNumVees(String v) {
        store.setValue(RAT_NUM_VEES, v);
    }

    public String getRATNumBA() {
        return store.getString(RAT_NUM_BA);
    }

    public void setRATNumBA(String v) {
        store.setValue(RAT_NUM_BA, v);
    }

    public String getRATNumInf() {
        return store.getString(RAT_NUM_INF);
    }

    public void setRATNumInf(String v) {
        store.setValue(RAT_NUM_INF, v);
    }

    public String getRATYearMin() {
        return store.getString(RAT_YEAR_MIN);
    }

    public void setRATYearMin(String v) {
        store.setValue(RAT_YEAR_MIN, v);
    }

    public String getRATYearMax() {
        return store.getString(RAT_YEAR_MAX);
    }

    public void setRATYearMax(String v) {
        store.setValue(RAT_YEAR_MAX, v);
    }

    public boolean getRATPadBV() {
        return store.getBoolean(RAT_PAD_BV);
    }

    public void setRATPadBV(boolean v) {
        store.setValue(RAT_PAD_BV, v);
    }

    public String getRATSelectedRAT() {
        return store.getString(RAT_SELECTED_RAT);
    }

    public void setRATSelectedRAT(String v) {
        store.setValue(RAT_SELECTED_RAT, v);
    }

    public void setBoardEdRndStart(boolean b) {
        store.setValue(BOARDEDIT_RNDDIALOG_START, b);
    }

    //region Colours
    public Color getMyUnitColor() {
        return getColor(MY_UNIT_COLOR);
    }

    public void setMyUnitColor(Color col) {
        store.setValue(MY_UNIT_COLOR, getColorString(col));
    }

    public void setEnemyUnitColor(Color col) {
        store.setValue(ENEMY_UNIT_COLOR, getColorString(col));
    }

    public Color getEnemyUnitColor() {
        return getColor(ENEMY_UNIT_COLOR);
    }

    public void setAllyUnitColor(Color col) {
        store.setValue(ALLY_UNIT_COLOR, getColorString(col));
    }

    public Color getAllyUnitColor() {
        return getColor(ALLY_UNIT_COLOR);
    }

    public Color getWarningColor() {
        return getColor(ADVANCED_WARNING_COLOR);
    }

    public void setWarningColor(Color color) {
        store.setValue(ADVANCED_WARNING_COLOR, getColorString(color));
    }

    public Color getReportLinkColor() {
        return getColor(ADVANCED_REPORT_COLOR_LINK);
    }

    public int getAdvancedUnitToolTipSeenByResolution() {
        return getInt(ADVANCED_UNITTOOLTIP_SEENBYRESOLUTION);
    }

    public boolean getAdvancedDockOnLeft() {
        return getBoolean(ADVANCED_DOCK_ON_LEFT);
    }

    public boolean getAdvancedDockMultipleOnYAxis() {
        return getBoolean(ADVANCED_DOCK_MULTIPLE_ON_Y_AXIS);
    }

    public int getAdvancedPlayersRemainingToShow() {
        return getInt(ADVANCED_PLAYERS_REMAINING_TO_SHOW);
    }

    public void setReportLinkColor(Color color) {
        store.setValue(ADVANCED_REPORT_COLOR_LINK, getColorString(color));
    }

    public Color getPlanetaryConditionsColorTitle() {
        return getColor(ADVANCED_PLANETARY_CONDITIONS_COLOR_TITLE);
    }

    public Color getPlanetaryConditionsColorText() {
        return getColor(ADVANCED_PLANETARY_CONDITIONS_COLOR_TEXT);
    }

    public Color getPlanetaryConditionsColorCold() {
        return getColor(ADVANCED_PLANETARY_CONDITIONS_COLOR_COLD);
    }

    public Color getPlanetaryConditionsColorHot() {
        return getColor(ADVANCED_PLANETARY_CONDITIONS_COLOR_HOT);
    }

    public Color getPlanetaryConditionsColorBackground() {
        return getColor(ADVANCED_PLANETARY_CONDITIONS_COLOR_BACKGROUND);
    }

    public Boolean getAdvancedPlanetaryConditionsShowDefaults() {
        return getBoolean(ADVANCED_PLANETARY_CONDITIONS_SHOW_DEFAULTS);
    }

    public Boolean getAdvancedPlanetaryConditionsShowHeader() {
        return getBoolean(ADVANCED_PLANETARY_CONDITIONS_SHOW_HEADER);
    }

    public Boolean getAdvancedPlanetaryConditionsShowLabels() {
        return getBoolean(ADVANCED_PLANETARY_CONDITIONS_SHOW_LABELS);
    }

    public Boolean getAdvancedPlanetaryConditionsShowValues() {
        return getBoolean(ADVANCED_PLANETARY_CONDITIONS_SHOW_VALUES);
    }

    public Boolean getAdvancedPlanetaryConditionsShowIndicators() {
        return getBoolean(ADVANCED_PLANETARY_CONDITIONS_SHOW_INDICATORS);
    }

    public void setPlanetaryConditionsColorTitle(Color color) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_COLOR_TITLE, getColorString(color));
    }

    public void setPlanetaryConditionsColorText(Color color) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_COLOR_TEXT, getColorString(color));
    }

    public void setPlanetaryConditionsColorCold(Color color) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_COLOR_COLD, getColorString(color));
    }

    public void setPlanetaryConditionsColorHot(Color color) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_COLOR_HOT, getColorString(color));
    }

    public void setPlanetaryConditionsColorBackground(Color color) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_COLOR_BACKGROUND, getColorString(color));
    }

    public void setAdvancedPlanetaryConditionsHideDefaults(Boolean state) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_SHOW_DEFAULTS, state);
    }

    public void setAdvancedPlanetaryConditionsHideHeader(Boolean state) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_SHOW_HEADER, state);
    }

    public void setAdvancedPlanetaryConditionsHideLabels(Boolean state) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_SHOW_LABELS, state);
    }

    public void setAdvancedPlanetaryConditionsHideValues(Boolean state) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_SHOW_VALUES, state);
    }

    public void setAdvancedPlanetaryConditionsHideIndicators(Boolean state) {
        store.setValue(ADVANCED_PLANETARY_CONDITIONS_SHOW_INDICATORS, state);
    }

    public void setAdvancedUnitToolTipSeenByResolution(int i) {
        store.setValue(ADVANCED_UNITTOOLTIP_SEENBYRESOLUTION, i);
    }

    public void setAdvancedDockOnLeft(Boolean state) {
        store.setValue(ADVANCED_DOCK_ON_LEFT, state);
    }

    public void setAdvancedDockMultipleOnYAxis(Boolean state) {
        store.setValue(ADVANCED_DOCK_MULTIPLE_ON_Y_AXIS, state);
    }

    public void setAdvancedPlayersRemainingToShow(Boolean state) {
        store.setValue(ADVANCED_PLAYERS_REMAINING_TO_SHOW, state);
    }

    /**
     * Toggles the state of the user preference for the Keybinds overlay.
     */
    public void toggleKeybindsOverlay() {
        store.setValue(SHOW_KEYBINDS_OVERLAY, !getBoolean(SHOW_KEYBINDS_OVERLAY));
    }

    public void togglePlanetaryConditionsOverlay() {
        store.setValue(SHOW_PLANETARYCONDITIONS_OVERLAY, !getBoolean(SHOW_PLANETARYCONDITIONS_OVERLAY));
    }

    public LabelDisplayStyle getUnitLabelStyle() {
        try {
            return LabelDisplayStyle.valueOf(store.getString(UNIT_LABEL_STYLE));
        } catch (Exception e) {
            return LabelDisplayStyle.FULL;
        }
    }

    /**
     * @return The color associated with this movement type
     */
    public Color getColorForMovement(EntityMovementType movementType) {
        switch (movementType) {
            case MOVE_RUN:
            case MOVE_VTOL_RUN:
            case MOVE_OVER_THRUST:
                return getColor(ADVANCED_MOVE_RUN_COLOR);
            case MOVE_JUMP:
                return getColor(ADVANCED_MOVE_JUMP_COLOR);
            case MOVE_SPRINT:
            case MOVE_VTOL_SPRINT:
                return getColor(ADVANCED_MOVE_SPRINT_COLOR);
            case MOVE_ILLEGAL:
                return getColor(ADVANCED_MOVE_ILLEGAL_COLOR);
            default:
                return getColor(ADVANCED_MOVE_DEFAULT_COLOR);
        }
    }

    /**
     * @return The color associated with a movement type
     */
    public Color getColorForMovement(EntityMovementType movementType, boolean isMASCOrSuperCharger, boolean isBackwards) {
        if (movementType != EntityMovementType.MOVE_ILLEGAL) {
            if (isMASCOrSuperCharger) {
                return getColor(ADVANCED_MOVE_MASC_COLOR);
            } else if (isBackwards) {
                return getColor(ADVANCED_MOVE_BACK_COLOR);
            }
        }
        return getColorForMovement(movementType);
    }

    /**
     * @return The color associated with a heat in the range 0-30
     */
    public Color getColorForHeat(int heat) {
        return getColorForHeat(heat, DEFAULT_LIGHT_GRAY);
    }

    /**
     * @return The color associated with a heat in the range 0-30
     */
    public Color getColorForHeat(int heat, Color defaultColor) {
        if (heat <= 0) {
            return defaultColor;
        } else if (heat <= 4) {
            return getColor(ADVANCED_HEAT_COLOR_4);
        } else if (heat <= 7) {
            return getColor(ADVANCED_HEAT_COLOR_7);
        } else if (heat <= 9) {
            return getColor(ADVANCED_HEAT_COLOR_9);
        } else if (heat <= 12) {
            return  getColor(ADVANCED_HEAT_COLOR_12);
        } else if (heat <= 13) {
            return  getColor(ADVANCED_HEAT_COLOR_13);
        } else if (heat <= 14) {
            return  getColor(ADVANCED_HEAT_COLOR_14);
        }
        return  getColor(ADVANCED_HEAT_COLOR_OVERHEAT);
    }

    public void setUnitLabelStyle(LabelDisplayStyle style) {
        store.setValue(UNIT_LABEL_STYLE, style.name());
    }

    public Color getColor(String name) {
        final String text = store.getString(name);
        final Color colour = parseRGB(text);
        return (colour == null) ? PlayerColour.parseFromString(text).getColour() : colour;
    }

    protected String getColorString(Color colour) {
        return colour.getRed() + " " + colour.getGreen() + " " + colour.getBlue();
    }

    protected Color parseRGB(String text) {
        final String[] codesText = text.split(" ");
        if (codesText.length == 3) {
            int[] codes = new int[codesText.length];
            for (int i = 0; i < codesText.length; i++) {
                codes[i] = Integer.parseInt(codesText[i]);
            }
            return new Color(codes[0], codes[1], codes[2]);
        }
        return Color.BLUE;
    }
    //endregion Colours

    /**
     * Activates AntiAliasing for the <code>Graphics</code> graph
     * if AA is activated in the Client settings.
     *
     * @param graph Graphics context to activate AA for
     */
    public static void AntiAliasifSet(Graphics graph) {
        if (getInstance().getAntiAliasing()) {
            ((Graphics2D) graph).setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

}
