/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.LabelDisplayStyle;
import megamek.client.ui.util.PlayerColour;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.preference.PreferenceManager;
import megamek.common.preference.PreferenceStoreProxy;
import megamek.common.units.EntityMovementType;

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
     * getInt is replaced with getBoolean, getString, etc. as necessary. The
     * reason these options were made this way is that GUI options have a way of
     * quickly multiplying, and we need a quick and dirty way of adding them
     * without having to code too many new lines. In addition, keeping them
     * separated in the settings dialog shields new users from unnecessary
     * complication.
     */
    public static final String ADVANCED_MOVE_STEP_DELAY = "AdvancedMoveStepDelay";
    public static final String ADVANCED_CHATBOX2_BACKCOLOR = "AdvancedChatbox2BackColor";
    public static final String ADVANCED_CHATBOX2_TRANSPARENCY = "AdvancedChatbox2Transparency";
    public static final String ADVANCED_CHATBOX2_AUTO_SLIDE_DOWN = "AdvancedChatbox2AutoSlideDown";
    public static final String ADVANCED_KEY_REPEAT_DELAY = "AdvancedKeyRepeatDelay";
    public static final String ADVANCED_KEY_REPEAT_RATE = "AdvancedKeyRepeatRate";
    public static final String ADVANCED_SHOW_FPS = "AdvancedShowFPS";
    public static final String ADVANCED_NO_SAVE_NAG = "AdvancedNoSaveNag";
    public static final String ADVANCED_SAVE_LOBBY_ON_START = "AdvancedSaveLobbyOnStart";

    /* --End advanced settings-- */
    public static final String BOARD_MOVE_DEFAULT_CLIMB_MODE = "BoardMoveDefaultClimbMode";
    public static final String BOARD_MOVE_DEFAULT_COLOR = "BoardMoveDefaultColor";
    public static final String BOARD_MOVE_ILLEGAL_COLOR = "BoardMoveIllegalColor";
    public static final String BOARD_MOVE_JUMP_COLOR = "BoardMoveJumpColor";
    public static final String BOARD_MOVE_MASC_COLOR = "BoardMoveMASCColor";
    public static final String BOARD_MOVE_RUN_COLOR = "BoardMoveRunColor";
    public static final String BOARD_MOVE_BACK_COLOR = "BoardMoveBackColor";
    public static final String BOARD_MOVE_SPRINT_COLOR = "BoardMoveSprintColor";
    public static final String BOARD_MOVE_FONT_TYPE = "BoardMoveFontType";
    public static final String BOARD_MOVE_FONT_SIZE = "BoardMoveFontSize";
    public static final String BOARD_MOVE_FONT_STYLE = "BoardMoveFontStyle";
    public static final String BOARD_FIRE_SOLUTION_CAN_SEE_COLOR = "BoardFireSolutionCanSeeColor";
    public static final String BOARD_FIRE_SOLUTION_NO_SEE_COLOR = "BoardFireSolutionNoSeeColor";
    public static final String BOARD_BUILDING_TEXT_COLOR = "BoardBuildingTextColor";
    public static final String BOARD_LOW_FOLIAGE_COLOR = "BoardLowFoliageColor";
    public static final String BOARD_TEXT_COLOR = "BoardTextColor";
    public static final String BOARD_SPACE_TEXT_COLOR = "BoardSpaceTextColor";
    public static final String BOARD_MAP_SHEET_COLOR = "BoardMapSheetColor";
    public static final String BOARD_FIELD_OF_FIRE_MIN_COLOR = "BoardFieldOfFireMinColor";
    public static final String BOARD_FIELD_OF_FIRE_SHORT_COLOR = "BoardFieldOfFireShortColor";
    public static final String BOARD_FIELD_OF_FIRE_MEDIUM_COLOR = "BoardFieldOfFireMediumColor";
    public static final String BOARD_FIELD_OF_FIRE_LONG_COLOR = "BoardFieldOfFireLongColor";
    public static final String BOARD_FIELD_OF_FIRE_EXTREME_COLOR = "BoardFieldOfFireExtremeColor";
    public static final String BOARD_SENSOR_RANGE_COLOR = "BoardSensorRangeColor";
    public static final String BOARD_VISUAL_RANGE_COLOR = "BoardVisualRangeColor";
    public static final String BOARD_UNIT_SELECTED_COLOR = "BoardUnitSelectedColor";
    public static final String BOARD_UNIT_VALID_COLOR = "BoardUnitValidColor";
    public static final String BOARD_UNIT_TEXT_COLOR = "BoardUnitTextColor";

    public static final String BOARD_ATTACK_ARROW_TRANSPARENCY = "BoardAttackArrowTransparency";
    public static final String BOARD_ECM_TRANSPARENCY = "BoardECMTransparency";
    public static final String BOARD_DARKEN_MAP_AT_NIGHT = "BoardDarkenMapAtNight";
    public static final String BOARD_TRANSLUCENT_HIDDEN_UNITS = "BoardTranslucentHiddenUnits";
    public static final String BOARD_TMM_PIP_MODE = "BoardTmmPipMode";

    public static final String SHOW_ARTILLERY_MISSES = "ShowArtilleryMisses";
    public static final String SHOW_ARTILLERY_DRIFTS = "ShowArtilleryHits";
    public static final String SHOW_BOMB_MISSES = "ShowBombMisses";
    public static final String SHOW_BOMB_DRIFTS = "ShowBombDrifts";
    public static final String SHOW_DEPLOY_ZONES_ARTY_AUTO = "ShowDeployZonesArtyAuto";

    public static final String UNIT_OVERVIEW_TEXT_SHADOW_COLOR = "UnitOverviewTextShadowColor";
    public static final String UNIT_OVERVIEW_CONDITION_SHADOW_COLOR = "UnitOverviewConditionShadowColor";

    public static final String PLANETARY_CONDITIONS_COLOR_TITLE = "PlanetaryConditionsColorTitle";
    public static final String PLANETARY_CONDITIONS_COLOR_TEXT = "PlanetaryConditionsColorText";
    public static final String PLANETARY_CONDITIONS_COLOR_COLD = "PlanetaryConditionsColorCold";
    public static final String PLANETARY_CONDITIONS_COLOR_HOT = "PlanetaryConditionsColorHot";
    public static final String PLANETARY_CONDITIONS_COLOR_BACKGROUND = "PlanetaryConditionsColorBackground";
    public static final String PLANETARY_CONDITIONS_SHOW_DEFAULTS = "PlanetaryConditionsShowDefaults";
    public static final String PLANETARY_CONDITIONS_SHOW_HEADER = "PlanetaryConditionsShowHeader";
    public static final String PLANETARY_CONDITIONS_SHOW_LABELS = "PlanetaryConditionsShowLabels";
    public static final String PLANETARY_CONDITIONS_SHOW_VALUES = "PlanetaryConditionsShowValues";
    public static final String PLANETARY_CONDITIONS_SHOW_INDICATORS = "PlanetaryConditionsShowIndicators";
    public static final String PLANETARY_CONDITIONS_BACKGROUND_TRANSPARENCY =
          "PlanetaryConditionsBackgroundTransparency";

    public static final String TRACE_OVERLAY_TRANSPARENCY = "TraceOverlayTransparency";
    public static final String TRACE_OVERLAY_SCALE = "TraceOverlayScale";
    public static final String TRACE_OVERLAY_ORIGIN_X = "TraceOverlayOriginX";
    public static final String TRACE_OVERLAY_ORIGIN_Y = "TraceOverlayOriginY";
    public static final String TRACE_OVERLAY_IMAGE_FILE = "TraceOverlayImageFile";

    public static final String PLAYERS_REMAINING_TO_SHOW = "PlayersRemainingToShow";
    public static final String BUTTONS_PER_ROW = "ButtonsPerRow";
    public static final String DOCK_ON_LEFT = "DockOnLeft";
    public static final String DOCK_MULTIPLE_ON_Y_AXIS = "DockMultipleOnYAxis";
    public static final String USE_CAMO_OVERLAY = "UseCamoOverlay";

    public static final String SHOW_COORDS = "showCoords";
    public static final String HIGH_QUALITY_GRAPHICS = "HighQualityGraphics";
    public static final String SHADOW_MAP = "ShadowMap";
    public static final String INCLINES = "Inclines";
    public static final String AO_HEX_SHADOWS = "AoHexShadows";
    public static final String LEVEL_HIGHLIGHT = "LevelHighlight";
    public static final String FLOATING_ISO = "FloatingIsometric";
    public static final String MM_SYMBOL = "MmSymbol";
    public static final String SOFT_CENTER = "SoftCenter";
    public static final String AUTO_CENTER = "AutoCenter";
    public static final String AUTO_SELECT_NEXT = "AutoSelectNextUnit";
    public static final String AUTO_END_FIRING = "AutoEndFiring";
    public static final String AUTO_DECLARE_SEARCHLIGHT = "AutoDeclareSearchlight";

    public static final String WARNING_COLOR = "WarningColor";
    public static final String CAUTION_COLOR = "CautionColor";
    public static final String PRECAUTION_COLOR = "PrecautionColor";
    public static final String OK_COLOR = "OkColor";

    public static final String CUSTOM_UNIT_HEIGHT = "CustomUnitDialogSizeHeight";
    public static final String CUSTOM_UNIT_WIDTH = "CustomUnitDialogSizeWidth";

    public static final String FORCE_DISPLAY_POS_X = "ForceDisplayPosX";
    public static final String FORCE_DISPLAY_POS_Y = "ForceDisplayPosY";
    public static final String FORCE_DISPLAY_SIZE_HEIGHT = "ForceDisplaySizeHeight";
    public static final String FORCE_DISPLAY_SIZE_WIDTH = "ForceDisplaySizeWidth";
    public static final String FORCE_DISPLAY_AUTO_DISPLAY_REPORT_PHASE = "ForceDisplayAutoDisplayReportPhase";
    public static final String FORCE_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE = "ForceDisplayAutoDisplayNonReportPhase";
    public static final String FORCE_DISPLAY_ENABLED = "ForceDisplayEnabled";
    public static final String FORCE_DISPLAY_BTN_ID = "ForceDisplayBtnID";
    public static final String FORCE_DISPLAY_BTN_PILOT = "ForceDisplayBtnPilot";
    public static final String FORCE_DISPLAY_BTN_MP = "ForceDisplayBtnMP";
    public static final String FORCE_DISPLAY_BTN_HEAT = "ForceDisplayBtnHeat";
    public static final String FORCE_DISPLAY_BTN_WEAPONS = "ForceDisplayBtnWeapons";
    public static final String FORCE_DISPLAY_BTN_DAMAGE_DESC = "ForceDisplayBtnDamageDesc";
    public static final String FORCE_DISPLAY_BTN_ARMOR = "ForceDisplayBtnArmor";
    public static final String FORCE_DISPLAY_BTN_TONNAGE = "ForceDisplayBtnTonnage";
    public static final String FORCE_DISPLAY_BTN_ROLE = "ForceDisplayBtnRole";
    public static final String FORCE_DISPLAY_BTN_ECM = "ForceDisplayBtnECM";
    public static final String FORCE_DISPLAY_BTN_QUIRKS = "ForceDisplayBtnQuirks";
    public static final String FORCE_DISPLAY_BTN_C3 = "ForceDisplayBtnC3";
    public static final String FORCE_DISPLAY_BTN_MISC = "ForceDisplayBtnMisc";

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
    public static final String UNIT_DISPLAY_AUTO_DISPLAY_REPORT_PHASE = "UnitDisplayAutoDisplayReportPhase";
    public static final String UNIT_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE = "UnitDisplayAutoDisplayNonReportPhase";
    public static final String UNIT_DISPLAY_ENABLED = "UnitDisplayEnabled";
    public static final String UNIT_DISPLAY_LOCATION = "UnitDisplayLocation";
    public static final String UNIT_DISPLAY_HEAT_COLOR_1 = "UnitDisplayHeatColor1";
    public static final String UNIT_DISPLAY_HEAT_COLOR_2 = "UnitDisplayHeatColor2";
    public static final String UNIT_DISPLAY_HEAT_COLOR_3 = "UnitDisplayHeatColor3";
    public static final String UNIT_DISPLAY_HEAT_COLOR_4 = "UnitDisplayHeatColor4";
    public static final String UNIT_DISPLAY_HEAT_COLOR_5 = "UnitDisplayHeatColor5";
    public static final String UNIT_DISPLAY_HEAT_COLOR_6 = "UnitDisplayHeatColor6";
    public static final String UNIT_DISPLAY_HEAT_COLOR_OVERHEAT = "UnitDisplayColorOverheat";
    public static final String UNIT_DISPLAY_HEAT_VALUE_1 = "UnitDisplayHeatValue1";
    public static final String UNIT_DISPLAY_HEAT_VALUE_2 = "UnitDisplayHeatValue2";
    public static final String UNIT_DISPLAY_HEAT_VALUE_3 = "UnitDisplayHeatValue3";
    public static final String UNIT_DISPLAY_HEAT_VALUE_4 = "UnitDisplayHeatValue4";
    public static final String UNIT_DISPLAY_HEAT_VALUE_5 = "UnitDisplayHeatValue5";
    public static final String UNIT_DISPLAY_HEAT_VALUE_6 = "UnitDisplayHeatValue6";
    public static final String UNIT_DISPLAY_WEAPON_LIST_HEIGHT = "UnitDisplayWeaponListHeight";
    public static final String UNIT_DISPLAY_MEK_ARMOR_LARGE_FONT_SIZE = "UnitDisplayMekArmorLargeFontSize";
    public static final String UNIT_DISPLAY_MEK_ARMOR_MEDIUM_FONT_SIZE = "UnitDisplayMekArmorMediumFontSize";
    public static final String UNIT_DISPLAY_MEK_ARMOR_SMALL_FONT_SIZE = "UnitDisplayMekArmorSmallFontSize";
    public static final String UNIT_DISPLAY_MEK_LARGE_FONT_SIZE = "UnitDisplayMekLargeFontSize";
    public static final String UNIT_DISPLAY_MEK_MEDIUM_FONT_SIZE = "UnitDisplayMekMediumFontSize";

    public static final String UNIT_TOOLTIP_SEEN_BY_RESOLUTION = "UnitToolTipSeenByResolution";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_UNITS_PER_BLOCK = "UnitToolTipArmorMiniUnitsPerBlock";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_ARMOR_CHAR = "UnitToolTipArmorMiniArmorChar";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_CAP_ARMOR_CHAR = "UnitToolTipArmorMiniCapArmorChar";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_IS_CHAR = "UnitToolTipArmorMiniISChar";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_CRITICAL_CHAR = "UnitToolTipArmorMiniCriticalChar";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_DESTROYED_CHAR = "UnitToolTipArmorMiniDestroyedChar";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_COLOR_INTACT = "UnitToolTipArmorMiniColorIntact";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_COLOR_PARTIAL_DMG = "UnitToolTipArmorMiniColorPartialDmg";
    public static final String UNIT_TOOLTIP_ARMOR_MINI_COLOR_DAMAGED = "UnitToolTipArmorMiniColorDamaged";
    public static final String UNIT_TOOLTIP_FONT_SIZE_MOD = "UnitToolTipFrontSizeMod";

    public static final String UNIT_TOOLTIP_FG_COLOR = "UnitToolTipFGColor";
    public static final String UNIT_TOOLTIP_LIGHT_FG_COLOR = "UnitToolTipLightFGColor";
    public static final String UNIT_TOOLTIP_BUILDING_FG_COLOR = "UnitToolTipBuildingFGColor";
    public static final String UNIT_TOOLTIP_ALT_FG_COLOR = "UnitToolTipAltFGColor";
    public static final String UNIT_TOOLTIP_BLOCK_FG_COLOR = "UnitToolTipBlockFGColor";
    public static final String UNIT_TOOLTIP_TERRAIN_FG_COLOR = "UnitToolTipTerrainFGColor";
    public static final String UNIT_TOOLTIP_BGCOLOR = "UnitToolTipBGColor";
    public static final String UNIT_TOOLTIP_BUILDING_BGCOLOR = "UnitToolTipBuildingBGColor";
    public static final String UNIT_TOOLTIP_ALT_BGCOLOR = "UnitToolTipAltBGColor";
    public static final String UNIT_TOOLTIP_BLOCK_BGCOLOR = "UnitToolTipBlockBGColor";
    public static final String UNIT_TOOLTIP_TERRAIN_BGCOLOR = "UnitToolTipTerrainBGColor";
    public static final String UNIT_TOOLTIP_HIGHLIGHT_COLOR = "UnitToolTipHighlightColor";
    public static final String UNIT_TOOLTIP_WEAPON_COLOR = "UnitToolTipWeaponColor";
    public static final String UNIT_TOOLTIP_QUIRK_COLOR = "UnitToolTipQuirkColor";

    public static final String SPLIT_PANE_A_DIVIDER_LOCATION = "SplitPaneADividerLocation";
    public static final String GAME_SUMMARY_BOARD_VIEW = "GameSummaryBoardView";
    public static final String GAME_SUMMARY_MINIMAP = "GameSummaryMinimap";
    public static final String GIF_GAME_SUMMARY_MINIMAP = "GifGameSummaryMinimap";
    public static final String SHOW_UNIT_DISPLAY_NAMES_ON_MINIMAP = "ShowUnitDisplayNamesOnMinimap";
    public static final String ENTITY_OWNER_LABEL_COLOR = "EntityOwnerLabelColor";
    public static final String UNIT_LABEL_BORDER = "EntityOwnerLabelColor";
    public static final String TEAM_COLORING = "EntityTeamLabelColor";
    public static final String FOCUS = "Focus";
    public static final String FIRING_SOLUTIONS = "FiringSolutions";
    public static final String CONSTRUCTOR_FACTOR_WARNING = "ConstructionFactorWarning";
    public static final String MOVE_ENVELOPE = "MoveEnvelope";
    public static final String TURN_DETAILS_OVERLAY = "TurnDetailsOverlay";
    public static final String FOV_HIGHLIGHT = "FovHighlight";
    public static final String FOV_HIGHLIGHT_ALPHA = "FovHighlightAlpha";
    // Rings' sizes (measured in distance to center) separated by whitespace.
    public static final String FOV_HIGHLIGHT_RINGS_RADII = "FovHighlightRingsRadii";
    // Rings' colors in the HSB format.
    // Each hsb color is separated by a semicolon, particular h, s and b values are
    // whitespace separated.
    public static final String FOV_HIGHLIGHT_RINGS_COLORS_HSB = "FovHighlightRingsColorsInHSB";
    public static final String FOV_DARKEN = "FovDarken";
    public static final String FOV_DARKEN_ALPHA = "FovDarkenAlpha";
    public static final String FOV_STRIPES = "FoVFogStripes";
    public static final String FOV_GRAYSCALE = "FoVFogGrayscale";
    public static final String GUI_SCALE = "GUIScale";
    public static final String LOBBY_MEK_TABLE_UNIT_WIDTH = "LobbyMekTableUnitWidth";
    public static final String LOBBY_MEK_TABLE_PILOT_WIDTH = "LobbyMekTablePilotWidth";
    public static final String LOBBY_MEK_TABLE_PLAYER_WIDTH = "LobbyMekTablePlayerWidth";
    public static final String LOBBY_MEK_TABLE_BV_WIDTH = "LobbyMekTableBVWidth";
    public static final String MAP_ZOOM_INDEX = "MapZoomIndex";
    public static final String MEK_SELECTOR_INCLUDE_MODEL = "MekSelectorIncludeModel";
    public static final String MEK_SELECTOR_INCLUDE_NAME = "MekSelectorIncludeName";
    public static final String MEK_SELECTOR_INCLUDE_TONS = "MekSelectorIncludeTons";
    public static final String MEK_SELECTOR_INCLUDE_BV = "MekSelectorIncludeBV";
    public static final String MEK_SELECTOR_INCLUDE_YEAR = "MekSelectorIncludeYear";
    public static final String MEK_SELECTOR_INCLUDE_LEVEL = "MekSelectorIncludeLevel";
    public static final String MEK_SELECTOR_INCLUDE_COST = "MekSelectorIncludeCost";
    public static final String MEK_SELECTOR_SHOW_ADVANCED = "MekSelectorShowAdvanced";
    public static final String MEK_SELECTOR_UNIT_TYPE = "MekSelectorUnitType";
    public static final String MEK_SELECTOR_WEIGHT_CLASS = "MekSelectorWeightClass";
    public static final String MEK_SELECTOR_RULES_LEVELS = "MekSelectorRuleType";
    public static final String MEK_SELECTOR_SORT_COLUMN = "MekSelectorSortColumn";
    public static final String MEK_SELECTOR_SORT_ORDER = "MekSelectorSortOrder";
    public static final String MEK_SELECTOR_SIZE_HEIGHT = "MekSelectorSizeHeight";
    public static final String MEK_SELECTOR_SIZE_WIDTH = "MekSelectorSizeWidth";
    public static final String MEK_SELECTOR_POS_X = "MekSelectorPosX";
    public static final String MEK_SELECTOR_POS_Y = "MekSelectorPosY";
    public static final String MEK_SELECTOR_SPLIT_POS = "MekSelectorSplitPos";
    public static final String MINI_REPORT_POS_X = "MiniReportPosX";
    public static final String MINI_REPORT_POS_Y = "MiniReportPosY";
    public static final String MINI_REPORT_SIZE_HEIGHT = "MiniReportSizeHeight";
    public static final String MINI_REPORT_SIZE_WIDTH = "MiniReportSizeWidth";
    public static final String MINI_REPORT_ENABLED = "MiniReportEnabled";
    public static final String MINI_REPORT_AUTO_DISPLAY_REPORT_PHASE = "MiniReportAutoDisplayReportPhase";
    public static final String MINI_REPORT_AUTO_DISPLAY_NON_REPORT_PHASE = "MiniReportAutoDisplayNonReportPhase";
    public static final String MINI_REPORT_LOCATION = "MiniReportLocation";
    public static final String MINI_REPORT_COLOR_LINK = "MiniReportColorLink";
    public static final String MINI_REPORT_COLOR_SUCCESS = "MiniReportColorSuccess";
    public static final String MINI_REPORT_COLOR_MISS = "MiniReportColorMiss";
    public static final String MINI_REPORT_COLOR_INFO = "MiniReportColorInfo";
    public static final String MINI_REPORT_FONT_TYPE = "MiniReportFontType";
    public static final String MINI_ROUND_REPORT_SPRITES = "MiniRoundReportSprites";
    public static final String MINI_REPORT_SHOW_PLAYERS = "MiniReportShowPlayers";
    public static final String MINI_REPORT_SHOW_UNITS = "MiniReportShowUnits";
    public static final String MINI_REPORT_SHOW_KEYWORDS = "MiniReportShowKeywords";
    public static final String MINI_REPORT_SHOW_FILTER = "MiniReportShowFilter";

    public static final String PLAYER_LIST_POS_X = "PlayerListPosX";
    public static final String PLAYER_LIST_POS_Y = "PlayerListPosY";
    public static final String PLAYER_LIST_ENABLED = "PlayerListEnabled";
    public static final String PLAYER_LIST_AUTO_DISPLAY_REPORT_PHASE = "PlayerListAutoDisplayReportPhase";
    public static final String PLAYER_LIST_AUTO_DISPLAY_NON_REPORT_PHASE = "PlayerListAutoDisplayNonReportPhase";
    public static final String MINI_MAP_COLOURS = "MinimapColours";
    public static final String MINI_MAP_ENABLED = "MinimapEnabled";
    public static final String MINI_MAP_POS_X = "MinimapPosX";
    public static final String MINI_MAP_POS_Y = "MinimapPosY";
    public static final String MINI_MAP_ZOOM = "MinimapZoom";
    public static final String MINI_MAP_HEIGHT_DISPLAY_MODE = "MinimapHeightDisplayMode";
    public static final String MINI_MAP_SYMBOLS_DISPLAY_MODE = "MinimapSymbolsDisplayMode";
    public static final String MINI_MAP_AUTO_DISPLAY_REPORT_PHASE = "MinimapAutoDisplayReportPhase";
    public static final String MINI_MAP_AUTO_DISPLAY_NON_REPORT_PHASE = "MinimapAutoDisplayNonReportPhase";
    public static final String MINI_MAP_SHOW_SENSOR_RANGE = "MinimapShowSensorRange";
    public static final String MINI_MAP_SHOW_FACING_ARROW = "MinimapShowFacingArrow";
    public static final String MINI_MAP_PAINT_BORDERS = "MinimapPaintBorders";
    public static final String MINI_MAP_MOVE_PATH_PERSISTENCE = "MinimapMovePathPersistence";
    public static final String FIRE_DISPLAY_TAB_DURING_PHASES = "FireDisplayTabDuringPhases";
    public static final String MOVE_DISPLAY_TAB_DURING_PHASES = "MoveDisplayTabDuringPhases";
    public static final String HIGH_PERFORMANCE_GRAPHICS = "HighPerformanceGraphics";
    public static final String MINIMUM_SIZE_HEIGHT = "MinimumSizeHeight";
    public static final String MINIMUM_SIZE_WIDTH = "MinimumSizeWidth";
    public static final String MOUSE_WHEEL_ZOOM = "MouseWheelZoom";
    public static final String MOUSE_WHEEL_ZOOM_FLIP = "MouseWheelZoomFlip";
    public static final String NAG_FOR_CRUSHING_BUILDINGS = "NagForCrushingBuildings";
    public static final String NAG_FOR_MAP_ED_README = "NagForMapEdReadme";
    public static final String NAG_FOR_MASC = "NagForMASC";
    public static final String NAG_FOR_NO_ACTION = "NagForNoAction";
    public static final String NAG_FOR_NO_UNJAM_RAC = "NagForNoUnjamRAC";
    public static final String NAG_FOR_PSR = "NagForPSR";
    public static final String NAG_FOR_README = "NagForReadme";
    public static final String NAG_FOR_SPRINT = "NagForSprint";
    public static final String NAG_FOR_OVERHEAT = "NagForOverHeat";
    public static final String NAG_FOR_LAUNCH_DOORS = "NagForLaunchDoors";
    public static final String NAG_FOR_MECHANICAL_FALL_DAMAGE = "NagForMechanicalFallDamage";
    public static final String NAG_FOR_DOOMED = "NagForDoomed";
    public static final String NAG_FOR_WIGE_LANDING = "NagForWiGELanding";
    public static final String NAG_FOR_ODD_SIZED_BOARD = "NagForOddSizedBoard";
    public static final String RULER_COLOR_1 = "RulerColor1";
    public static final String RULER_COLOR_2 = "RulerColor2";
    public static final String RULER_POS_X = "RulerPosX";
    public static final String RULER_POS_Y = "RulerPosY";
    public static final String RULER_SIZE_HEIGHT = "RulerSizeHeight";
    public static final String RULER_SIZE_WIDTH = "RulerSizeWidth";
    public static final String SCROLL_SENSITIVITY = "ScrollSensitivity";
    public static final String SHOW_FIELD_OF_FIRE = "ShowFieldOfFire";
    public static final String SHOW_SENSOR_RANGE = "ShowSensorRange";
    public static final String SHOW_MAP_HEX_POPUP = "ShowMapHexPopup";
    public static final String SHOW_WPS_IN_TT = "ShowWpsInTT";
    public static final String SHOW_WPS_LOC_IN_TT = "ShowWpsLocinTT";
    public static final String SHOW_ARMOR_MINI_VIS_TT = "showArmorMiniVisTT";
    public static final String SHOW_PILOT_PORTRAIT_TT = "showPilotPortraitTT";
    public static final String SHOW_MOVE_STEP = "ShowMoveStep";
    public static final String SHOW_WRECKS = "ShowWrecks";
    public static final String SOUND_BING_FILENAME_CHAT = "SoundBingFilenameChat";
    public static final String SOUND_BING_FILENAME_MY_TURN = "SoundBingFilenameMyTurn";
    public static final String SOUND_BING_FILENAME_OTHERS_TURN = "SoundBingFilenameOthersTurn";
    public static final String MASTER_VOLUME = "MasterVolume";
    public static final String SOUND_MUTE_CHAT = "SoundMuteChat";
    public static final String SOUND_MUTE_MY_TURN = "SoundMuteMyTurn";
    public static final String SOUND_MUTE_OTHERS_TURN = "SoundMuteOthersTurn";
    public static final String TOOLTIP_DELAY = "TooltipDelay";
    public static final String TOOLTIP_DISMISS_DELAY = "TooltipDismissDelay";
    public static final String TOOLTIP_DIST_SUPPRESSION = "TooltipDistSuppression";
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
    public static final String LOS_MEK_IN_FIRST = "LOSMekInFirst";
    public static final String LOS_MEK_IN_SECOND = "LOSMekInSecond";
    public static final String SHOW_MAP_SHEETS = "ShowMapSheets";
    public static final String USE_ISOMETRIC = "UseIsometric";
    public static final String SHOW_UNIT_OVERVIEW = "ShowUnitOverview";
    public static final String SHOW_DAMAGE_LEVEL = "ShowDamageLevel";
    public static final String SHOW_DAMAGE_DECAL = "ShowDamageDecal";
    public static final String SKIN_FILE = "SkinFile";
    public static final String DEFAULT_WEAPON_SORT_ORDER = "DefaultWeaponSortOrder";
    public static final String UI_THEME = "UITheme";
    public static final String BOARD_EDIT_LOAD_SIZE_HEIGHT = "BoardEditLoadSizeHeight";
    public static final String BOARD_EDIT_LOAD_SIZE_WIDTH = "BoardEditLoadSizeWidth";
    public static final String BOARD_EDIT_RANDOM_DIALOG_START = "BoardEditRandomDialogStart";
    public static final String ALLY_UNIT_COLOR = "AllyUnitColor";
    public static final String MY_UNIT_COLOR = "MyUnitColor";
    public static final String ENEMY_UNIT_COLOR = "EnemyUnitColor";
    public static final String SHOW_KEYBINDS_OVERLAY = "ShowKeybindsOverlay";
    public static final String SHOW_PLANETARY_CONDITIONS_OVERLAY = "ShowPlanetaryConditionsOverlay";
    public static final String SHOW_TRACE_OVERLAY = "ShowTraceOverlay";
    public static final String UNIT_LABEL_STYLE = "UnitLabelStyle";
    public static final String AS_CARD_FONT = "AsCardFont";
    public static final String AS_CARD_SIZE = "AsCardSize";
    public static final String SBF_SHEET_HEADER_FONT = "SBFSheetHeaderFont";
    public static final String SBF_SHEET_VALUE_FONT = "SBFSheetValueFont";
    public static final String SUMMARY_FONT = "SummaryCardFont";

    public static final String BOT_COMMANDS_ENABLED = "BotCommandsEnabled";
    public static final String BOT_COMMANDS_POS_X = "BotCommandsPosX";
    public static final String BOT_COMMANDS_POS_Y = "BotCommandsPosY";
    public static final String BOT_COMMANDS_WIDTH = "BotCommandsWidth";
    public static final String BOT_COMMANDS_HEIGHT = "BotCommandsHeight";
    public static final String BOT_COMMANDS_AUTO_DISPLAY_NON_REPORT_PHASE = "BotCommandsAutoDisplayNonReportPhase";
    public static final String BOT_COMMANDS_AUTO_DISPLAY_REPORT_PHASE = "BotCommandsAutoDisplayReportPhase";


    // RAT dialog preferences
    public static String RAT_TECH_LEVEL = "RATTechLevel";
    public static String RAT_BV_MIN = "RATBVMin";
    public static String RAT_BV_MAX = "RATBVMax";
    public static String RAT_NUM_MEKS = "RATNumMeks";
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
    private static final Color DEFAULT_RED = new Color(225, 51, 51);
    private static final Color DEFAULT_GREEN = new Color(0, 212, 0);
    private static final Color DEFAULT_BLUE = new Color(64, 96, 228);
    private static final Color DEFAULT_MEDIUM_DARK_RED = new Color(150, 80, 80); // medium dark red
    private static final Color DEFAULT_MEDIUM_YELLOW = new Color(180, 180, 100);
    private static final Color DEFAULT_ORANGE = new Color(248, 140, 0);
    private static final Color DEFAULT_YELLOW = new Color(216, 200, 0);
    private static final Color DEFAULT_MEDIUM_GREEN = new Color(100, 180, 100);

    // Heat Scale
    private static final Color DEFAULT_HEAT_1_COLOR = new Color(64, 128, 255);
    private static final Color DEFAULT_HEAT_2_COLOR = new Color(64, 164, 128);
    private static final Color DEFAULT_HEAT_3_COLOR = new Color(48, 212, 48);
    private static final Color DEFAULT_HEAT_4_COLOR = new Color(228, 198, 0);
    private static final Color DEFAULT_HEAT_5_COLOR = new Color(248, 128, 0);
    private static final Color DEFAULT_HEAT_6_COLOR = new Color(248, 64, 64);
    private static final Color DEFAULT_HEAT_OVERHEAT_COLOR = new Color(248, 12, 12);

    private static final Color DEFAULT_PLANETARY_CONDITIONS_TEXT_COLOR = new Color(200, 250, 200);
    private static final Color DEFAULT_PLANETARY_CONDITIONS_COLD_COLOR = new Color(173, 216, 230);
    private static final Color DEFAULT_PLANETARY_CONDITIONS_HOT_COLOR = new Color(255, 204, 203);
    private static final Color DEFAULT_PLANETARY_CONDITIONS_BACKGROUND_COLOR = new Color(80, 80, 80);

    // Report Color
    private static final Color DEFAULT_REPORT_LINK_COLOR = new Color(5, 160, 255);

    // Map colors
    private static final Color DEFAULT_MAP_BRIGHT_GREEN = new Color(80, 230, 80);
    private static final Color DEFAULT_MAP_BLUE = new Color(60, 140, 240); // greenish blue
    private static final Color DEFAULT_MAP_RED = new Color(200, 40, 40); // red
    private static final Color DEFAULT_MAP_GREEN = new Color(40, 210, 40); // light green

    private static final String DELIMITER = ";";
    private static final String _TAB_ORDER = "_tabOrder";
    private static final String _WINDOW = "_window";

    protected static GUIPreferences instance = new GUIPreferences();

    public static final int HIDE = 0;
    public static final int SHOW = 1;
    public static final int MANUAL = 2;

    public static GUIPreferences getInstance() {
        return instance;
    }

    protected GUIPreferences() {
        store = PreferenceManager.getInstance()
              .getPreferenceStore("GUIPreferences", getClass().getName(), "megamek.client.ui.swing.GUIPreferences");

        store.setDefault(BOARD_EDIT_RANDOM_DIALOG_START, false);
        setDefault(ADVANCED_NO_SAVE_NAG, false);
        store.setDefault(ADVANCED_SAVE_LOBBY_ON_START, false);
        store.setDefault(ADVANCED_MOVE_STEP_DELAY, 50);
        setDefault(ADVANCED_CHATBOX2_BACKCOLOR, DEFAULT_WHITE);
        store.setDefault(ADVANCED_CHATBOX2_TRANSPARENCY, 50);
        store.setDefault(ADVANCED_CHATBOX2_AUTO_SLIDE_DOWN, true);
        store.setDefault(ADVANCED_KEY_REPEAT_DELAY, 0);
        store.setDefault(ADVANCED_KEY_REPEAT_RATE, 20);
        store.setDefault(ADVANCED_SHOW_FPS, false);
        store.setDefault(SHOW_COORDS, true);

        setDefault(PLANETARY_CONDITIONS_COLOR_TITLE, Color.WHITE);
        setDefault(PLANETARY_CONDITIONS_COLOR_TEXT, DEFAULT_PLANETARY_CONDITIONS_TEXT_COLOR);
        setDefault(PLANETARY_CONDITIONS_COLOR_COLD, DEFAULT_PLANETARY_CONDITIONS_COLD_COLOR);
        setDefault(PLANETARY_CONDITIONS_COLOR_HOT, DEFAULT_PLANETARY_CONDITIONS_HOT_COLOR);
        setDefault(PLANETARY_CONDITIONS_COLOR_BACKGROUND, DEFAULT_PLANETARY_CONDITIONS_BACKGROUND_COLOR);
        setDefault(PLANETARY_CONDITIONS_SHOW_DEFAULTS, true);
        setDefault(PLANETARY_CONDITIONS_SHOW_HEADER, true);
        setDefault(PLANETARY_CONDITIONS_SHOW_LABELS, true);
        setDefault(PLANETARY_CONDITIONS_SHOW_VALUES, true);
        setDefault(PLANETARY_CONDITIONS_SHOW_INDICATORS, true);
        setDefault(PLANETARY_CONDITIONS_BACKGROUND_TRANSPARENCY, 200);

        setDefault(TRACE_OVERLAY_TRANSPARENCY, 64);
        setDefault(TRACE_OVERLAY_SCALE, 100);
        setDefault(TRACE_OVERLAY_ORIGIN_X, 0);
        setDefault(TRACE_OVERLAY_ORIGIN_Y, 0);
        setDefault(TRACE_OVERLAY_IMAGE_FILE, "");

        setDefault(WARNING_COLOR, DEFAULT_RED);
        setDefault(CAUTION_COLOR, Color.yellow);
        setDefault(PRECAUTION_COLOR, Color.orange);
        setDefault(OK_COLOR, DEFAULT_GREEN);

        setDefault(PlayerColour.PLAYER_COLOUR_BLUE, new Color(0x8686BF));
        setDefault(PlayerColour.PLAYER_COLOUR_RED, new Color(0xCC6666));
        setDefault(PlayerColour.PLAYER_COLOUR_GREEN, new Color(0x87BF86));
        setDefault(PlayerColour.PLAYER_COLOUR_CYAN, new Color(0x8FCCCC));
        setDefault(PlayerColour.PLAYER_COLOUR_PINK, new Color(0xF29DC8));
        setDefault(PlayerColour.PLAYER_COLOUR_ORANGE, new Color(0xF2AA61));
        setDefault(PlayerColour.PLAYER_COLOUR_GRAY, new Color(0xBEBEBE));
        setDefault(PlayerColour.PLAYER_COLOUR_BROWN, new Color(0x98816B));
        setDefault(PlayerColour.PLAYER_COLOUR_PURPLE, new Color(0x800080));
        setDefault(PlayerColour.PLAYER_COLOUR_TURQUOISE, new Color(0x40E0D0));
        setDefault(PlayerColour.PLAYER_COLOUR_MAROON, new Color(0x800000));
        setDefault(PlayerColour.PLAYER_COLOUR_SPRING_GREEN, new Color(0x00FF7F));
        setDefault(PlayerColour.PLAYER_COLOUR_GOLD, new Color(0xFFD700));
        setDefault(PlayerColour.PLAYER_COLOUR_SIENNA, new Color(0xA0522D));
        setDefault(PlayerColour.PLAYER_COLOUR_VIOLET, new Color(0xEE82EE));
        setDefault(PlayerColour.PLAYER_COLOUR_NAVY, new Color(0x000080));
        setDefault(PlayerColour.PLAYER_COLOUR_OLIVE_DRAB, new Color(0x6B8E23));
        setDefault(PlayerColour.PLAYER_COLOUR_FUCHSIA, new Color(0xFF00FF));
        setDefault(PlayerColour.PLAYER_COLOUR_FIRE_BRICK, new Color(0xB22222));
        setDefault(PlayerColour.PLAYER_COLOUR_DARK_GOLDEN_ROD, new Color(0xB8860B));
        setDefault(PlayerColour.PLAYER_COLOUR_CORAL, new Color(0xFF7F50));
        setDefault(PlayerColour.PLAYER_COLOUR_CHARTREUSE, new Color(0x7FFF00));
        setDefault(PlayerColour.PLAYER_COLOUR_DEEP_PURPLE, new Color(0x9400D3));
        setDefault(PlayerColour.PLAYER_COLOUR_YELLOW, new Color(0xF2F261));

        setDefault(BOARD_MOVE_DEFAULT_CLIMB_MODE, true);
        setDefault(BOARD_MOVE_DEFAULT_COLOR, Color.CYAN);
        setDefault(BOARD_MOVE_ILLEGAL_COLOR, DEFAULT_DARK_GRAY);
        setDefault(BOARD_MOVE_JUMP_COLOR, DEFAULT_RED);
        setDefault(BOARD_MOVE_MASC_COLOR, DEFAULT_ORANGE);
        setDefault(BOARD_MOVE_RUN_COLOR, DEFAULT_YELLOW);
        setDefault(BOARD_MOVE_BACK_COLOR, DEFAULT_YELLOW);
        setDefault(BOARD_MOVE_SPRINT_COLOR, DEFAULT_PINK);
        setDefault(BOARD_FIRE_SOLUTION_CAN_SEE_COLOR, DEFAULT_CYAN);
        setDefault(BOARD_FIRE_SOLUTION_NO_SEE_COLOR, DEFAULT_RED);
        setDefault(BOARD_BUILDING_TEXT_COLOR, DEFAULT_BLUE);
        setDefault(BOARD_LOW_FOLIAGE_COLOR, DEFAULT_MAP_BRIGHT_GREEN);
        setDefault(BOARD_TEXT_COLOR, DEFAULT_BLACK);
        setDefault(BOARD_SPACE_TEXT_COLOR, DEFAULT_LIGHT_GRAY);
        setDefault(BOARD_MAP_SHEET_COLOR, DEFAULT_BLUE);
        setDefault(BOARD_FIELD_OF_FIRE_MIN_COLOR, new Color(255, 100, 100));
        setDefault(BOARD_FIELD_OF_FIRE_SHORT_COLOR, new Color(100, 255, 100));
        setDefault(BOARD_FIELD_OF_FIRE_MEDIUM_COLOR, new Color(80, 200, 80));
        setDefault(BOARD_FIELD_OF_FIRE_LONG_COLOR, new Color(60, 150, 60));
        setDefault(BOARD_FIELD_OF_FIRE_EXTREME_COLOR, new Color(40, 100, 40));
        setDefault(BOARD_SENSOR_RANGE_COLOR, new Color(105, 105, 245));
        setDefault(BOARD_VISUAL_RANGE_COLOR, new Color(255, 204, 255));
        setDefault(BOARD_UNIT_SELECTED_COLOR, DEFAULT_MAGENTA);
        setDefault(BOARD_UNIT_VALID_COLOR, DEFAULT_CYAN);
        setDefault(BOARD_UNIT_TEXT_COLOR, Color.white);

        setDefault(BOARD_MOVE_FONT_TYPE, "SansSerif");
        setDefault(BOARD_MOVE_FONT_SIZE, 26);
        setDefault(BOARD_MOVE_FONT_STYLE, Font.BOLD);
        store.setDefault(BOARD_ATTACK_ARROW_TRANSPARENCY, 0x80);
        store.setDefault(BOARD_ECM_TRANSPARENCY, 0x80);
        store.setDefault(BOARD_DARKEN_MAP_AT_NIGHT, true);
        store.setDefault(BOARD_TRANSLUCENT_HIDDEN_UNITS, true);
        setDefault(BOARD_TMM_PIP_MODE, 2); // show pips with colors based on move type

        store.setDefault(SHOW_ARTILLERY_MISSES, true);
        store.setDefault(SHOW_ARTILLERY_DRIFTS, true);
        store.setDefault(SHOW_BOMB_MISSES, true);
        store.setDefault(SHOW_BOMB_DRIFTS, false);
        store.setDefault(SHOW_DEPLOY_ZONES_ARTY_AUTO, false);

        setDefault(UNIT_OVERVIEW_TEXT_SHADOW_COLOR, Color.black);
        setDefault(UNIT_OVERVIEW_CONDITION_SHADOW_COLOR, Color.darkGray);

        setDefault(PLAYERS_REMAINING_TO_SHOW, 3);
        store.setDefault(BUTTONS_PER_ROW, 12);

        setDefault(DOCK_ON_LEFT, true);
        setDefault(DOCK_MULTIPLE_ON_Y_AXIS, true);
        setDefault(USE_CAMO_OVERLAY, true);

        store.setDefault(FOV_HIGHLIGHT_RINGS_RADII, "5 10 15 20 25");
        store.setDefault(FOV_HIGHLIGHT_RINGS_COLORS_HSB,
              "0.3 1.0 1.0 ; 0.45 1.0 1.0 ; 0.6 1.0 1.0 ; 0.75 1.0 1.0 ; 0.9 1.0 1.0 ; 1.05 1.0 1.0 ");
        store.setDefault(FOV_HIGHLIGHT, false);
        store.setDefault(FOV_HIGHLIGHT_ALPHA, 40);
        store.setDefault(FOV_DARKEN, true);
        store.setDefault(FOV_DARKEN_ALPHA, 100);
        store.setDefault(FOV_STRIPES, 35);
        store.setDefault(FOV_GRAYSCALE, false);

        store.setDefault(HIGH_QUALITY_GRAPHICS, true);
        store.setDefault(AO_HEX_SHADOWS, false);
        store.setDefault(SHADOW_MAP, true);
        store.setDefault(INCLINES, true);
        store.setDefault(FLOATING_ISO, false);
        store.setDefault(LEVEL_HIGHLIGHT, false);

        store.setDefault(AUTO_END_FIRING, true);
        store.setDefault(AUTO_DECLARE_SEARCHLIGHT, true);
        store.setDefault(CUSTOM_UNIT_HEIGHT, 400);
        store.setDefault(CUSTOM_UNIT_WIDTH, 600);

        store.setDefault(FORCE_DISPLAY_AUTO_DISPLAY_REPORT_PHASE, 2);
        store.setDefault(FORCE_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE, 2);
        store.setDefault(FORCE_DISPLAY_ENABLED, false);
        store.setDefault(FORCE_DISPLAY_SIZE_HEIGHT, 500);
        store.setDefault(FORCE_DISPLAY_SIZE_WIDTH, 300);
        store.setDefault(FORCE_DISPLAY_BTN_ID, true);
        store.setDefault(FORCE_DISPLAY_BTN_PILOT, true);
        store.setDefault(FORCE_DISPLAY_BTN_MP, false);
        store.setDefault(FORCE_DISPLAY_BTN_HEAT, false);
        store.setDefault(FORCE_DISPLAY_BTN_WEAPONS, false);
        store.setDefault(FORCE_DISPLAY_BTN_DAMAGE_DESC, true);
        store.setDefault(FORCE_DISPLAY_BTN_ARMOR, false);
        store.setDefault(FORCE_DISPLAY_BTN_TONNAGE, true);
        store.setDefault(FORCE_DISPLAY_BTN_ROLE, true);
        store.setDefault(FORCE_DISPLAY_BTN_ECM, true);
        store.setDefault(FORCE_DISPLAY_BTN_QUIRKS, true);
        store.setDefault(FORCE_DISPLAY_BTN_C3, true);
        store.setDefault(FORCE_DISPLAY_BTN_MISC, true);

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
        store.setDefault(UNIT_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE, 1);
        store.setDefault(UNIT_DISPLAY_ENABLED, true);
        store.setDefault(UNIT_DISPLAY_LOCATION, 0);
        store.setDefault(SPLIT_PANE_A_DIVIDER_LOCATION, 300);
        setDefault(UNIT_DISPLAY_HEAT_COLOR_1, DEFAULT_HEAT_1_COLOR);
        setDefault(UNIT_DISPLAY_HEAT_COLOR_2, DEFAULT_HEAT_2_COLOR);
        setDefault(UNIT_DISPLAY_HEAT_COLOR_3, DEFAULT_HEAT_3_COLOR);
        setDefault(UNIT_DISPLAY_HEAT_COLOR_4, DEFAULT_HEAT_4_COLOR);
        setDefault(UNIT_DISPLAY_HEAT_COLOR_5, DEFAULT_HEAT_5_COLOR);
        setDefault(UNIT_DISPLAY_HEAT_COLOR_6, DEFAULT_HEAT_6_COLOR);
        setDefault(UNIT_DISPLAY_HEAT_COLOR_OVERHEAT, DEFAULT_HEAT_OVERHEAT_COLOR);
        store.setDefault(UNIT_DISPLAY_HEAT_VALUE_1, 4);
        store.setDefault(UNIT_DISPLAY_HEAT_VALUE_2, 7);
        store.setDefault(UNIT_DISPLAY_HEAT_VALUE_3, 9);
        store.setDefault(UNIT_DISPLAY_HEAT_VALUE_4, 12);
        store.setDefault(UNIT_DISPLAY_HEAT_VALUE_5, 13);
        store.setDefault(UNIT_DISPLAY_HEAT_VALUE_6, 14);
        store.setDefault(UNIT_DISPLAY_MEK_ARMOR_LARGE_FONT_SIZE, 12);
        store.setDefault(UNIT_DISPLAY_MEK_ARMOR_MEDIUM_FONT_SIZE, 10);
        store.setDefault(UNIT_DISPLAY_MEK_ARMOR_SMALL_FONT_SIZE, 9);
        store.setDefault(UNIT_DISPLAY_MEK_LARGE_FONT_SIZE, 12);
        store.setDefault(UNIT_DISPLAY_MEK_MEDIUM_FONT_SIZE, 10);
        setDefault(UNIT_DISPLAY_WEAPON_LIST_HEIGHT, 200);

        store.setDefault(UNIT_TOOLTIP_SEEN_BY_RESOLUTION, 3);
        store.setDefault(UNIT_TOOLTIP_ARMOR_MINI_UNITS_PER_BLOCK, 10);
        store.setDefault(UNIT_TOOLTIP_ARMOR_MINI_ARMOR_CHAR, "\u2B1B"); // Centered Filled Square
        store.setDefault(UNIT_TOOLTIP_ARMOR_MINI_CAP_ARMOR_CHAR, "\u26CA"); // Shield
        store.setDefault(UNIT_TOOLTIP_ARMOR_MINI_IS_CHAR, "\u25A3"); // Centered Square with Dot
        store.setDefault(UNIT_TOOLTIP_ARMOR_MINI_CRITICAL_CHAR, "\u27D0"); // Centered Square with Dot
        store.setDefault(UNIT_TOOLTIP_ARMOR_MINI_DESTROYED_CHAR, "\u2715"); // Centered x
        setDefault(UNIT_TOOLTIP_ARMOR_MINI_COLOR_INTACT, DEFAULT_MEDIUM_GREEN);
        setDefault(UNIT_TOOLTIP_ARMOR_MINI_COLOR_PARTIAL_DMG, DEFAULT_MEDIUM_YELLOW);
        setDefault(UNIT_TOOLTIP_ARMOR_MINI_COLOR_DAMAGED, DEFAULT_MEDIUM_DARK_RED);
        store.setDefault(UNIT_TOOLTIP_FONT_SIZE_MOD, "medium");
        setDefault(UNIT_TOOLTIP_FG_COLOR, new Color(0xEEE6D9));
        setDefault(UNIT_TOOLTIP_LIGHT_FG_COLOR, new Color(0x000000));
        setDefault(UNIT_TOOLTIP_BUILDING_FG_COLOR, new Color(0x000000));
        setDefault(UNIT_TOOLTIP_ALT_FG_COLOR, new Color(0xEEE6D9));
        setDefault(UNIT_TOOLTIP_BLOCK_FG_COLOR, new Color(0xEEE6D9));
        setDefault(UNIT_TOOLTIP_TERRAIN_FG_COLOR, new Color(0x000000));
        setDefault(UNIT_TOOLTIP_BGCOLOR, new Color(0x313131));
        setDefault(UNIT_TOOLTIP_BUILDING_BGCOLOR, new Color(0xCCCC99));
        setDefault(UNIT_TOOLTIP_ALT_BGCOLOR, new Color(0x003333));
        setDefault(UNIT_TOOLTIP_BLOCK_BGCOLOR, new Color(0x000060));
        setDefault(UNIT_TOOLTIP_TERRAIN_BGCOLOR, new Color(0x8DAF8D));
        setDefault(UNIT_TOOLTIP_HIGHLIGHT_COLOR, new Color(0xB496DC));
        setDefault(UNIT_TOOLTIP_WEAPON_COLOR, new Color(0x9696D2));
        setDefault(UNIT_TOOLTIP_QUIRK_COLOR, new Color(0x64B4B4));

        store.setDefault(GAME_SUMMARY_BOARD_VIEW, false);
        store.setDefault(ENTITY_OWNER_LABEL_COLOR, true);
        store.setDefault(UNIT_LABEL_BORDER, true);
        store.setDefault(UNIT_LABEL_STYLE, LabelDisplayStyle.NICKNAME.name());
        store.setDefault(FIRING_SOLUTIONS, true);
        store.setDefault(CONSTRUCTOR_FACTOR_WARNING, true);
        store.setDefault(GUI_SCALE, 1);
        store.setDefault(LOBBY_MEK_TABLE_UNIT_WIDTH, 170);
        store.setDefault(LOBBY_MEK_TABLE_PILOT_WIDTH, 80);
        store.setDefault(LOBBY_MEK_TABLE_PLAYER_WIDTH, 50);
        store.setDefault(LOBBY_MEK_TABLE_BV_WIDTH, 50);
        store.setDefault(MAP_ZOOM_INDEX, 7);
        store.setDefault(MEK_SELECTOR_INCLUDE_MODEL, true);
        store.setDefault(MEK_SELECTOR_INCLUDE_NAME, true);
        store.setDefault(MEK_SELECTOR_INCLUDE_TONS, true);
        store.setDefault(MEK_SELECTOR_INCLUDE_BV, true);
        store.setDefault(MEK_SELECTOR_INCLUDE_YEAR, true);
        store.setDefault(MEK_SELECTOR_INCLUDE_LEVEL, true);
        store.setDefault(MEK_SELECTOR_INCLUDE_COST, true);
        store.setDefault(MEK_SELECTOR_UNIT_TYPE, 0);
        store.setDefault(MEK_SELECTOR_WEIGHT_CLASS, 15);
        store.setDefault(MEK_SELECTOR_RULES_LEVELS, "[0]");
        store.setDefault(MEK_SELECTOR_SORT_COLUMN, 0);
        store.setDefault(MEK_SELECTOR_SORT_ORDER, "ASCENDING");
        store.setDefault(MEK_SELECTOR_SHOW_ADVANCED, false);
        store.setDefault(MEK_SELECTOR_SIZE_HEIGHT, 600);
        store.setDefault(MEK_SELECTOR_SIZE_WIDTH, 800);
        store.setDefault(MEK_SELECTOR_POS_X, 200);
        store.setDefault(MEK_SELECTOR_POS_Y, 200);
        store.setDefault(MEK_SELECTOR_SPLIT_POS, 300);
        store.setDefault(RND_ARMY_SIZE_HEIGHT, 600);
        store.setDefault(RND_ARMY_SIZE_WIDTH, 800);
        store.setDefault(RND_ARMY_POS_X, 200);
        store.setDefault(RND_ARMY_POS_Y, 200);
        store.setDefault(RND_ARMY_SPLIT_POS, 300);

        store.setDefault(MINI_MAP_COLOURS, "defaultminimap.txt");
        store.setDefault(MINI_MAP_ENABLED, true);
        store.setDefault(MINI_MAP_AUTO_DISPLAY_REPORT_PHASE, 0);
        store.setDefault(MINI_MAP_AUTO_DISPLAY_NON_REPORT_PHASE, 1);
        store.setDefault(MINI_MAP_SHOW_SENSOR_RANGE, true);
        store.setDefault(MINI_MAP_SHOW_FACING_ARROW, true);
        store.setDefault(MINI_MAP_PAINT_BORDERS, true);
        store.setDefault(MINI_MAP_MOVE_PATH_PERSISTENCE, 2);
        store.setDefault(GIF_GAME_SUMMARY_MINIMAP, true);
        store.setDefault(GAME_SUMMARY_MINIMAP, false);
        store.setDefault(SHOW_UNIT_DISPLAY_NAMES_ON_MINIMAP, false);
        store.setDefault(MOVE_DISPLAY_TAB_DURING_PHASES, true);
        store.setDefault(FIRE_DISPLAY_TAB_DURING_PHASES, true);

        store.setDefault(MM_SYMBOL, true);
        store.setDefault(MINIMUM_SIZE_HEIGHT, 200);
        store.setDefault(MINIMUM_SIZE_WIDTH, 120);

        store.setDefault(HIGH_PERFORMANCE_GRAPHICS, false);
        store.setDefault(MINI_REPORT_POS_X, 200);
        store.setDefault(MINI_REPORT_POS_Y, 150);
        store.setDefault(MINI_REPORT_SIZE_HEIGHT, 300);
        store.setDefault(MINI_REPORT_SIZE_WIDTH, 400);
        store.setDefault(MINI_REPORT_ENABLED, true);
        store.setDefault(MINI_REPORT_AUTO_DISPLAY_REPORT_PHASE, 1);
        store.setDefault(MINI_REPORT_AUTO_DISPLAY_NON_REPORT_PHASE, 0);
        store.setDefault(MINI_REPORT_LOCATION, 0);
        setDefault(MINI_REPORT_COLOR_LINK, DEFAULT_REPORT_LINK_COLOR);
        setDefault(MINI_REPORT_COLOR_SUCCESS, new Color(51, 204, 0));
        setDefault(MINI_REPORT_COLOR_MISS, new Color(128, 128, 128));
        setDefault(MINI_REPORT_COLOR_INFO, new Color(51, 153, 255));
        setDefault(MINI_REPORT_FONT_TYPE, "Segoe UI");
        store.setDefault(MINI_ROUND_REPORT_SPRITES, true);
        store.setDefault(MINI_REPORT_SHOW_PLAYERS, true);
        store.setDefault(MINI_REPORT_SHOW_UNITS, true);
        store.setDefault(MINI_REPORT_SHOW_KEYWORDS, true);
        store.setDefault(MINI_REPORT_SHOW_FILTER, false);

        store.setDefault(PLAYER_LIST_ENABLED, true);
        store.setDefault(PLAYER_LIST_POS_X, 200);
        store.setDefault(PLAYER_LIST_POS_Y, 150);
        store.setDefault(PLAYER_LIST_AUTO_DISPLAY_REPORT_PHASE, 1);
        store.setDefault(PLAYER_LIST_AUTO_DISPLAY_NON_REPORT_PHASE, 0);

        store.setDefault(MOUSE_WHEEL_ZOOM, true);
        store.setDefault(MOUSE_WHEEL_ZOOM_FLIP, true);

        store.setDefault(NAG_FOR_CRUSHING_BUILDINGS, true);
        store.setDefault(NAG_FOR_MAP_ED_README, true);
        store.setDefault(NAG_FOR_MASC, true);
        store.setDefault(NAG_FOR_NO_ACTION, true);
        store.setDefault(NAG_FOR_NO_UNJAM_RAC, true);
        store.setDefault(NAG_FOR_PSR, true);
        store.setDefault(NAG_FOR_README, true);
        store.setDefault(NAG_FOR_SPRINT, true);
        store.setDefault(NAG_FOR_OVERHEAT, true);
        store.setDefault(NAG_FOR_LAUNCH_DOORS, true);
        store.setDefault(NAG_FOR_MECHANICAL_FALL_DAMAGE, true);
        store.setDefault(NAG_FOR_DOOMED, true);
        store.setDefault(NAG_FOR_WIGE_LANDING, true);
        store.setDefault(NAG_FOR_ODD_SIZED_BOARD, true);

        setDefault(RULER_COLOR_1, DEFAULT_CYAN);
        setDefault(RULER_COLOR_2, DEFAULT_MAGENTA);
        store.setDefault(RULER_POS_X, 0);
        store.setDefault(RULER_POS_Y, 0);
        store.setDefault(RULER_SIZE_HEIGHT, 300);
        store.setDefault(RULER_SIZE_WIDTH, 500);

        store.setDefault(SCROLL_SENSITIVITY, 3);
        store.setDefault(SHOW_FIELD_OF_FIRE, true);
        store.setDefault(SHOW_SENSOR_RANGE, true);
        store.setDefault(SHOW_MAP_HEX_POPUP, true);
        store.setDefault(SHOW_MOVE_STEP, true);
        store.setDefault(SHOW_WRECKS, true);

        store.setDefault(MASTER_VOLUME, 100);
        store.setDefault(SOUND_BING_FILENAME_CHAT, "data/sounds/call.wav");
        store.setDefault(SOUND_BING_FILENAME_MY_TURN, "data/sounds/call.wav");
        store.setDefault(SOUND_BING_FILENAME_OTHERS_TURN, "data/sounds/call.wav");
        store.setDefault(SOUND_MUTE_CHAT, true);
        store.setDefault(SOUND_MUTE_MY_TURN, false);
        store.setDefault(SOUND_MUTE_OTHERS_TURN, true);

        store.setDefault(TOOLTIP_DELAY, 1000);
        store.setDefault(TOOLTIP_DISMISS_DELAY, -1);
        store.setDefault(TOOLTIP_DIST_SUPPRESSION, BoardView.HEX_DIAG);
        store.setDefault(SHOW_WPS_IN_TT, true);
        store.setDefault(SHOW_WPS_LOC_IN_TT, true);
        store.setDefault(SHOW_ARMOR_MINI_VIS_TT, true);
        store.setDefault(SHOW_PILOT_PORTRAIT_TT, true);

        store.setDefault(USE_ISOMETRIC, false);

        store.setDefault(WINDOW_SIZE_HEIGHT, 600);
        store.setDefault(WINDOW_SIZE_WIDTH, 800);

        store.setDefault(RND_MAP_SIZE_HEIGHT, 500);
        store.setDefault(RND_MAP_SIZE_WIDTH, 500);
        store.setDefault(RND_MAP_POS_X, 400);
        store.setDefault(RND_MAP_POS_Y, 400);
        store.setDefault(RND_MAP_ADVANCED, false);
        store.setDefault(BOARD_EDIT_LOAD_SIZE_WIDTH, 400);
        store.setDefault(BOARD_EDIT_LOAD_SIZE_HEIGHT, 300);

        store.setDefault(SHOW_MAP_SHEETS, false);

        store.setDefault(SHOW_UNIT_OVERVIEW, true);
        store.setDefault(DEFAULT_WEAPON_SORT_ORDER, WeaponSortOrder.DEFAULT.name());
        store.setDefault(SHOW_DAMAGE_LEVEL, true);
        store.setDefault(SHOW_DAMAGE_DECAL, true);
        store.setDefault(SKIN_FILE, "BW - Default.xml");
        store.setDefault(SOFT_CENTER, false);
        store.setDefault(AUTO_CENTER, true);
        store.setDefault(AUTO_SELECT_NEXT, true);
        store.setDefault(UI_THEME, "com.formdev.flatlaf.FlatDarculaLaf");

        store.setDefault(RAT_TECH_LEVEL, 0);
        store.setDefault(RAT_BV_MIN, "5800");
        store.setDefault(RAT_BV_MAX, "6000");
        store.setDefault(RAT_NUM_MEKS, "4");
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
        setDefault(SHOW_PLANETARY_CONDITIONS_OVERLAY, true);
        setDefault(SHOW_TRACE_OVERLAY, false);

        setDefault(AS_CARD_FONT, "");
        setDefault(AS_CARD_SIZE, 0.75f);
        setDefault(SBF_SHEET_HEADER_FONT, "");
        setDefault(SBF_SHEET_VALUE_FONT, "");
        setDefault(SUMMARY_FONT, "");
    }

    public void setDefault(String name, Color color) {
        store.setDefault(name, getColorString(color));
    }

    @Override
    public String[] getAdvancedProperties() {
        return store.getAdvancedProperties();
    }

    public boolean getAOHexShadows() {
        return store.getBoolean(AO_HEX_SHADOWS);
    }

    public boolean getHighQualityGraphics() {
        return store.getBoolean(HIGH_QUALITY_GRAPHICS);
    }

    public boolean getFloatingIso() {
        return store.getBoolean(FLOATING_ISO);
    }

    public boolean getMmSymbol() {
        return store.getBoolean(MM_SYMBOL);
    }

    public boolean getShadowMap() {
        return store.getBoolean(SHADOW_MAP);
    }

    public boolean getHexInclines() {
        return store.getBoolean(INCLINES);
    }

    public boolean getLevelHighlight() {
        return store.getBoolean(LEVEL_HIGHLIGHT);
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

    public int getForceDisplayPosX() {
        return store.getInt(FORCE_DISPLAY_POS_X);
    }

    public int getForceDisplayPosY() {
        return store.getInt(FORCE_DISPLAY_POS_Y);
    }

    public int getForceDisplaySizeHeight() {
        return store.getInt(FORCE_DISPLAY_SIZE_HEIGHT);
    }

    public int getForceDisplaySizeWidth() {
        return store.getInt(FORCE_DISPLAY_SIZE_WIDTH);
    }

    public int getForceDisplayAutoDisplayReportPhase() {
        return store.getInt(FORCE_DISPLAY_AUTO_DISPLAY_REPORT_PHASE);
    }

    public int getForceDisplayAutoDisplayNonReportPhase() {
        return store.getInt(FORCE_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE);
    }

    public boolean getForceDisplayEnabled() {
        return store.getBoolean(FORCE_DISPLAY_ENABLED);
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
        return store.getInt(UNIT_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE);
    }

    public boolean getUnitDisplayEnabled() {
        return store.getBoolean(UNIT_DISPLAY_ENABLED);
    }

    public int getUnitDisplayWeaponListHeight() {
        return store.getInt(UNIT_DISPLAY_WEAPON_LIST_HEIGHT);
    }

    public int getUnitDisplayLocation() {
        return store.getInt(UNIT_DISPLAY_LOCATION);
    }

    public Color getUnitDisplayHeatLevel1() {
        return getColor(UNIT_DISPLAY_HEAT_COLOR_1);
    }

    public Color getUnitDisplayHeatLevel2() {
        return getColor(UNIT_DISPLAY_HEAT_COLOR_2);
    }

    public Color getUnitDisplayHeatLevel3() {
        return getColor(UNIT_DISPLAY_HEAT_COLOR_3);
    }

    public Color getUnitDisplayHeatLevel4() {
        return getColor(UNIT_DISPLAY_HEAT_COLOR_4);
    }

    public Color getUnitDisplayHeatLevel5() {
        return getColor(UNIT_DISPLAY_HEAT_COLOR_5);
    }

    public Color getUnitDisplayHeatLevel6() {
        return getColor(UNIT_DISPLAY_HEAT_COLOR_6);
    }

    public Color getUnitDisplayHeatLevelOverheat() {
        return getColor(UNIT_DISPLAY_HEAT_COLOR_OVERHEAT);
    }

    public int getUnitDisplayHeatValue1() {
        return getInt(UNIT_DISPLAY_HEAT_VALUE_1);
    }

    public int getUnitDisplayHeatValue2() {
        return getInt(UNIT_DISPLAY_HEAT_VALUE_2);
    }

    public int getUnitDisplayHeatValue3() {
        return getInt(UNIT_DISPLAY_HEAT_VALUE_3);
    }

    public int getUnitDisplayHeatValue4() {
        return getInt(UNIT_DISPLAY_HEAT_VALUE_4);
    }

    public int getUnitDisplayHeatValue5() {
        return getInt(UNIT_DISPLAY_HEAT_VALUE_5);
    }

    public int getUnitDisplayHeatValue6() {
        return getInt(UNIT_DISPLAY_HEAT_VALUE_6);
    }

    public int getUnitDisplayMekArmorLargeFontSize() {
        return getInt(UNIT_DISPLAY_MEK_ARMOR_LARGE_FONT_SIZE);
    }

    public int getUnitDisplayMekArmorMediumFontSize() {
        return getInt(UNIT_DISPLAY_MEK_ARMOR_MEDIUM_FONT_SIZE);
    }

    public int getUnitDisplayMekArmorSmallFontSize() {
        return getInt(UNIT_DISPLAY_MEK_ARMOR_SMALL_FONT_SIZE);
    }

    public int getUnitDisplayMekLargeFontSize() {
        return getInt(UNIT_DISPLAY_MEK_LARGE_FONT_SIZE);
    }

    public int getUnitDisplayMekMediumFontSize() {
        return getInt(UNIT_DISPLAY_MEK_MEDIUM_FONT_SIZE);
    }

    public Color getUnitTooltipArmorMiniColorIntact() {
        return getColor(UNIT_TOOLTIP_ARMOR_MINI_COLOR_INTACT);
    }

    public Color getUnitTooltipArmorMiniColorPartialDamage() {
        return getColor(UNIT_TOOLTIP_ARMOR_MINI_COLOR_PARTIAL_DMG);
    }

    public Color getUnitTooltipArmorMiniColorDamaged() {
        return getColor(UNIT_TOOLTIP_ARMOR_MINI_COLOR_DAMAGED);
    }

    public int getSplitPaneADividerLocation() {
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

    public boolean getGifGameSummaryMinimap() {
        return store.getBoolean(GIF_GAME_SUMMARY_MINIMAP);
    }

    public boolean showUnitDisplayNamesOnMinimap() {
        return store.getBoolean(SHOW_UNIT_DISPLAY_NAMES_ON_MINIMAP);
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

    public boolean getShowFiringSolutions() {
        return store.getBoolean(FIRING_SOLUTIONS);
    }

    public boolean getShowCFWarnings() {
        return store.getBoolean(CONSTRUCTOR_FACTOR_WARNING);
    }

    public boolean getMoveEnvelope() {
        return store.getBoolean(MOVE_ENVELOPE);
    }

    public boolean getTurnDetailsOverlay() {
        return store.getBoolean(TURN_DETAILS_OVERLAY);
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

    public int getMapZoomIndex() {
        return store.getInt(MAP_ZOOM_INDEX);
    }

    public boolean getMekSelectorIncludeModel() {
        return store.getBoolean(MEK_SELECTOR_INCLUDE_MODEL);
    }

    public boolean getMekSelectorIncludeName() {
        return store.getBoolean(MEK_SELECTOR_INCLUDE_NAME);
    }

    public boolean getMekSelectorIncludeTons() {
        return store.getBoolean(MEK_SELECTOR_INCLUDE_TONS);
    }

    public boolean getMekSelectorIncludeBV() {
        return store.getBoolean(MEK_SELECTOR_INCLUDE_BV);
    }

    public boolean getMekSelectorIncludeYear() {
        return store.getBoolean(MEK_SELECTOR_INCLUDE_YEAR);
    }

    public boolean getMekSelectorIncludeLevel() {
        return store.getBoolean(MEK_SELECTOR_INCLUDE_LEVEL);
    }

    public boolean getMekSelectorIncludeCost() {
        return store.getBoolean(MEK_SELECTOR_INCLUDE_COST);
    }

    public boolean getMekSelectorShowAdvanced() {
        return store.getBoolean(MEK_SELECTOR_SHOW_ADVANCED);
    }

    public int getMekSelectorUnitType() {
        return store.getInt(MEK_SELECTOR_UNIT_TYPE);
    }

    public int getMekSelectorWeightClass() {
        return store.getInt(MEK_SELECTOR_WEIGHT_CLASS);
    }

    public String getMekSelectorRulesLevels() {
        return store.getString(MEK_SELECTOR_RULES_LEVELS);
    }

    public int getMekSelectorSortColumn() {
        return store.getInt(MEK_SELECTOR_SORT_COLUMN);
    }

    public int getMekSelectorDefaultSortColumn() {
        return store.getDefaultInt(MEK_SELECTOR_SORT_COLUMN);
    }

    public String getMekSelectorSortOrder() {
        return store.getString(MEK_SELECTOR_SORT_ORDER);
    }

    public String getMekSelectorDefaultSortOrder() {
        return store.getDefaultString(MEK_SELECTOR_SORT_ORDER);
    }

    public int getMekSelectorSizeHeight() {
        return store.getInt(MEK_SELECTOR_SIZE_HEIGHT);
    }

    public int getMekSelectorSizeWidth() {
        return store.getInt(MEK_SELECTOR_SIZE_WIDTH);
    }

    public int getMekSelectorPosX() {
        return store.getInt(MEK_SELECTOR_POS_X);
    }

    public int getMekSelectorPosY() {
        return store.getInt(MEK_SELECTOR_POS_Y);
    }

    public int getMekSelectorSplitPos() {
        return store.getInt(MEK_SELECTOR_SPLIT_POS);
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
        return store.getInt(MINI_MAP_AUTO_DISPLAY_NON_REPORT_PHASE);
    }

    public boolean getFireDisplayTabDuringFiringPhases() {
        return store.getBoolean(FIRE_DISPLAY_TAB_DURING_PHASES);
    }

    public boolean getMoveDisplayTabDuringMovePhases() {
        return store.getBoolean(MOVE_DISPLAY_TAB_DURING_PHASES);
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

    public int getMinimapSymbolsDisplayMode() {
        return store.getInt(MINI_MAP_SYMBOLS_DISPLAY_MODE);
    }

    public boolean getMiniReportEnabled() {
        return store.getBoolean(MINI_REPORT_ENABLED);
    }

    public int getMiniReportAutoDisplayReportPhase() {
        return store.getInt(MINI_REPORT_AUTO_DISPLAY_REPORT_PHASE);
    }

    public int getMiniReportAutoDisplayNonReportPhase() {
        return store.getInt(MINI_REPORT_AUTO_DISPLAY_NON_REPORT_PHASE);
    }

    public int getMiniReportLocation() {
        return store.getInt(MINI_REPORT_LOCATION);
    }

    public boolean getMiniReportShowSprites() {
        return store.getBoolean(MINI_ROUND_REPORT_SPRITES);
    }

    public boolean getMiniReportShowPlayers() {
        return store.getBoolean(MINI_REPORT_SHOW_PLAYERS);
    }

    public boolean getMiniReportShowUnits() {
        return store.getBoolean(MINI_REPORT_SHOW_UNITS);
    }

    public boolean getMiniReportShowKeywords() {
        return store.getBoolean(MINI_REPORT_SHOW_KEYWORDS);
    }

    public boolean getMiniReportShowFilter() {
        return store.getBoolean(MINI_REPORT_SHOW_FILTER);
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
        return store.getInt(PLAYER_LIST_AUTO_DISPLAY_NON_REPORT_PHASE);
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
        return store.getInt(BOARD_EDIT_LOAD_SIZE_HEIGHT);
    }

    public int getBoardEditLoadWidth() {
        return store.getInt(BOARD_EDIT_LOAD_SIZE_WIDTH);
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
        return store.getBoolean(NAG_FOR_NO_UNJAM_RAC);
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

    public boolean getShowSensorRange() {
        return store.getBoolean(SHOW_SENSOR_RANGE);
    }

    public boolean getShowMapHexPopup() {
        return store.getBoolean(SHOW_MAP_HEX_POPUP);
    }

    public boolean getShowWpsInTT() {
        return store.getBoolean(SHOW_WPS_IN_TT);
    }

    public boolean getShowWpsLocinTT() {
        return store.getBoolean(SHOW_WPS_LOC_IN_TT);
    }

    public boolean getShowArmorMiniVisTT() {
        return store.getBoolean(SHOW_ARMOR_MINI_VIS_TT);
    }

    public boolean getShowPilotPortraitTT() {
        return store.getBoolean(SHOW_PILOT_PORTRAIT_TT);
    }

    public boolean getShowMoveStep() {
        return store.getBoolean(SHOW_MOVE_STEP);
    }

    public boolean getShowWrecks() {
        return store.getBoolean(SHOW_WRECKS);
    }

    public int getMasterVolume() {
        return store.getInt(MASTER_VOLUME);
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
        return store.getInt(TOOLTIP_DIST_SUPPRESSION);
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

    public boolean getMekInFirst() {
        return store.getBoolean(LOS_MEK_IN_FIRST);
    }

    public boolean getMekInSecond() {
        return store.getBoolean(LOS_MEK_IN_SECOND);
    }

    public boolean getShowMapSheets() {
        return store.getBoolean(SHOW_MAP_SHEETS);
    }

    public boolean getShowUnitOverview() {
        return store.getBoolean(SHOW_UNIT_OVERVIEW);
    }

    public String getSkinFile() {
        return store.getString(SKIN_FILE);
    }

    public String getDefaultSkinFile() {
        return store.getDefaultString(SKIN_FILE);
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

    public String getSummaryFont() {
        return store.getString(SUMMARY_FONT);
    }

    public String getSbfSheetHeaderFont() {
        return store.getString(SBF_SHEET_HEADER_FONT);
    }

    public String getSbfSheetValueFont() {
        return store.getString(SBF_SHEET_VALUE_FONT);
    }

    public float getAsCardSize() {
        return store.getFloat(AS_CARD_SIZE);
    }

    public int getMoveStepDelay() {
        return store.getInt(ADVANCED_MOVE_STEP_DELAY);
    }

    public boolean getShowFPS() {
        return store.getBoolean(ADVANCED_SHOW_FPS);
    }

    public boolean getSoftCenter() {
        return store.getBoolean(SOFT_CENTER);
    }

    public boolean getAutoCenter() {
        return store.getBoolean(AUTO_CENTER);
    }

    public boolean getAutoSelectNextUnit() {
        return store.getBoolean(AUTO_SELECT_NEXT);
    }

    public void setAutoSelectNextUnit(boolean autoSelectNextUnit) {
        store.setValue(AUTO_SELECT_NEXT, autoSelectNextUnit);
    }

    public boolean getNoSaveNag() {
        return store.getBoolean(ADVANCED_NO_SAVE_NAG);
    }

    public boolean getSaveLobbyOnStart() {
        return store.getBoolean(ADVANCED_SAVE_LOBBY_ON_START);
    }

    public void setSaveLobbyOnStart(boolean value) {
        store.setValue(ADVANCED_SAVE_LOBBY_ON_START, value);
    }

    public boolean getChatbox2AutoSlideDown() {
        return store.getBoolean(ADVANCED_CHATBOX2_AUTO_SLIDE_DOWN);
    }

    public Color getChatbox2BackColor() {
        return getColor(ADVANCED_CHATBOX2_BACKCOLOR);
    }

    public int getChatBox2Transparency() {
        return store.getInt(ADVANCED_CHATBOX2_TRANSPARENCY);
    }

    public void setDefaultWeaponSortOrder(final WeaponSortOrder weaponSortOrder) {
        store.setValue(DEFAULT_WEAPON_SORT_ORDER, weaponSortOrder.name());
    }

    public boolean getBoardEdRndStart() {
        return store.getBoolean(BOARD_EDIT_RANDOM_DIALOG_START);
    }

    public void setShadowMap(boolean state) {
        store.setValue(SHADOW_MAP, state);
    }

    public void setHexInclines(boolean state) {
        store.setValue(INCLINES, state);
    }

    public void setAOHexShadows(boolean state) {
        store.setValue(AO_HEX_SHADOWS, state);
    }

    public void setHighQualityGraphics(boolean state) {
        store.setValue(HIGH_QUALITY_GRAPHICS, state);
    }

    public void setFloatingIso(boolean state) {
        store.setValue(FLOATING_ISO, state);
    }

    public void setMmSymbol(boolean state) {
        store.setValue(MM_SYMBOL, state);
    }

    public void setLevelHighlight(boolean state) {
        store.setValue(LEVEL_HIGHLIGHT, state);
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

    public void setForceDisplayPosX(int i) {
        store.setValue(FORCE_DISPLAY_POS_X, i);
    }

    public void setForceDisplayPosY(int i) {
        store.setValue(FORCE_DISPLAY_POS_Y, i);
    }

    public void setForceDisplaySizeHeight(int i) {
        store.setValue(FORCE_DISPLAY_SIZE_HEIGHT, i);
    }

    public void setForceDisplaySizeWidth(int i) {
        store.setValue(FORCE_DISPLAY_SIZE_WIDTH, i);
    }

    public void setForceDisplayAutoDisplayReportPhase(int i) {
        store.setValue(FORCE_DISPLAY_AUTO_DISPLAY_REPORT_PHASE, i);
    }

    public void setForceDisplayAutoDisplayNonReportPhase(int i) {
        store.setValue(FORCE_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE, i);
    }

    public void setBotCommandAutoDisplayReportPhase(int i) {
        store.setValue(BOT_COMMANDS_AUTO_DISPLAY_REPORT_PHASE, i);
    }

    public void setBotCommandAutoDisplayNonReportPhase(int i) {
        store.setValue(BOT_COMMANDS_AUTO_DISPLAY_NON_REPORT_PHASE, i);
    }

    public void toggleForceDisplay() {
        store.setValue(FORCE_DISPLAY_ENABLED, !getBoolean(FORCE_DISPLAY_ENABLED));
    }

    public void setForceDisplayEnabled(boolean b) {
        store.setValue(FORCE_DISPLAY_ENABLED, b);
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
        store.setValue(UNIT_DISPLAY_AUTO_DISPLAY_NON_REPORT_PHASE, i);
    }

    public void toggleUnitDisplay() {
        store.setValue(UNIT_DISPLAY_ENABLED, !getBoolean(UNIT_DISPLAY_ENABLED));
    }

    public void setUnitDisplayEnabled(boolean b) {
        store.setValue(UNIT_DISPLAY_ENABLED, b);
    }

    public void setUnitDisplayWeaponListHeight(int i) {
        store.setValue(UNIT_DISPLAY_WEAPON_LIST_HEIGHT, i);
    }

    public void toggleUnitDisplayLocation() {
        store.setValue(UNIT_DISPLAY_LOCATION, ((getInt(UNIT_DISPLAY_LOCATION) + 1) % 2));
    }

    public void setUnitDisplayLocation(int i) {
        store.setValue(UNIT_DISPLAY_LOCATION, i);
    }

    public void setUnitDisplayHeatColorLevel1(Color c) {
        store.setValue(UNIT_DISPLAY_HEAT_COLOR_1, getColorString(c));
    }

    public void setUnitDisplayHeatColorLevel2(Color c) {
        store.setValue(UNIT_DISPLAY_HEAT_COLOR_2, getColorString(c));
    }

    public void setUnitDisplayHeatColorLevel3(Color c) {
        store.setValue(UNIT_DISPLAY_HEAT_COLOR_3, getColorString(c));
    }

    public void setUnitDisplayHeatColorLevel4(Color c) {
        store.setValue(UNIT_DISPLAY_HEAT_COLOR_4, getColorString(c));
    }

    public void setUnitDisplayHeatColorLevel5(Color c) {
        store.setValue(UNIT_DISPLAY_HEAT_COLOR_5, getColorString(c));
    }

    public void setUnitDisplayHeatColorLevel6(Color c) {
        store.setValue(UNIT_DISPLAY_HEAT_COLOR_6, getColorString(c));
    }

    public void setUnitDisplayHeatColorLevelOverHeat(Color c) {
        store.setValue(UNIT_DISPLAY_HEAT_COLOR_OVERHEAT, getColorString(c));
    }

    public void setUnitDisplayHeatColorValue1(int i) {
        store.setValue(UNIT_DISPLAY_HEAT_VALUE_1, i);
    }

    public void setUnitDisplayHeatColorValue2(int i) {
        store.setValue(UNIT_DISPLAY_HEAT_VALUE_2, i);
    }

    public void setUnitDisplayHeatColorValue3(int i) {
        store.setValue(UNIT_DISPLAY_HEAT_VALUE_3, i);
    }

    public void setUnitDisplayHeatColorValue4(int i) {
        store.setValue(UNIT_DISPLAY_HEAT_VALUE_4, i);
    }

    public void setUnitDisplayHeatColorValue5(int i) {
        store.setValue(UNIT_DISPLAY_HEAT_VALUE_5, i);
    }

    public void setUnitDisplayHeatColorValue6(int i) {
        store.setValue(UNIT_DISPLAY_HEAT_VALUE_6, i);
    }

    public void setUnitDisplayMekArmorLargeFontSize(int i) {
        store.setValue(UNIT_DISPLAY_MEK_ARMOR_LARGE_FONT_SIZE, i);
    }

    public void setUnitDisplayMekArmorMediumFontSize(int i) {
        store.setValue(UNIT_DISPLAY_MEK_ARMOR_MEDIUM_FONT_SIZE, i);
    }

    public void setUnitDisplayMekArmorSmallFontSize(int i) {
        store.setValue(UNIT_DISPLAY_MEK_ARMOR_SMALL_FONT_SIZE, i);
    }

    public void setUnitDisplayMekLargeFontSize(int i) {
        store.setValue(UNIT_DISPLAY_MEK_LARGE_FONT_SIZE, i);
    }

    public void setUnitDisplayMekMediumFontSize(int i) {
        store.setValue(UNIT_DISPLAY_MEK_MEDIUM_FONT_SIZE, i);
    }

    public void setUnitTooltipArmorMiniColorIntact(Color c) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_COLOR_INTACT, getColorString(c));
    }

    public void setUnitTooltipArmorMiniColorPartialDamage(Color c) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_COLOR_PARTIAL_DMG, getColorString(c));
    }

    public void setUnitTooltipArmorMiniColorDamaged(Color c) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_COLOR_DAMAGED, getColorString(c));
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

    public void setGifGameSummaryMinimap(boolean state) {
        store.setValue(GIF_GAME_SUMMARY_MINIMAP, state);
    }

    public void setShowUnitDisplayNamesOnMinimap(boolean state) {
        store.setValue(SHOW_UNIT_DISPLAY_NAMES_ON_MINIMAP, state);
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

    public void setShowFiringSolutions(boolean state) {
        store.setValue(FIRING_SOLUTIONS, state);
    }

    public void setShowCFWarnings(boolean b) {
        store.setValue(CONSTRUCTOR_FACTOR_WARNING, b);
    }

    public void setMoveEnvelope(boolean state) {
        store.setValue(MOVE_ENVELOPE, state);
    }

    public void setTurnDetailsOverlay(boolean state) {
        store.setValue(TURN_DETAILS_OVERLAY, state);
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

    public void setMekSelectorIncludeModel(boolean includeModel) {
        store.setValue(MEK_SELECTOR_INCLUDE_MODEL, includeModel);
    }

    public void setMekSelectorIncludeName(boolean includeName) {
        store.setValue(MEK_SELECTOR_INCLUDE_NAME, includeName);
    }

    public void setMekSelectorIncludeTons(boolean includeTons) {
        store.setValue(MEK_SELECTOR_INCLUDE_TONS, includeTons);
    }

    public void setMekSelectorIncludeBV(boolean includeBV) {
        store.setValue(MEK_SELECTOR_INCLUDE_BV, includeBV);
    }

    public void setMekSelectorIncludeYear(boolean includeYear) {
        store.setValue(MEK_SELECTOR_INCLUDE_YEAR, includeYear);
    }

    public void setMekSelectorIncludeLevel(boolean includeLevel) {
        store.setValue(MEK_SELECTOR_INCLUDE_LEVEL, includeLevel);
    }

    public void setMekSelectorIncludeCost(boolean includeCost) {
        store.setValue(MEK_SELECTOR_INCLUDE_COST, includeCost);
    }

    public void setMekSelectorShowAdvanced(boolean showAdvanced) {
        store.setValue(MEK_SELECTOR_SHOW_ADVANCED, showAdvanced);
    }

    public void setMekSelectorUnitType(int unitType) {
        store.setValue(MEK_SELECTOR_UNIT_TYPE, unitType);
    }

    public void setMekSelectorWeightClass(int weightClass) {
        store.setValue(MEK_SELECTOR_WEIGHT_CLASS, weightClass);
    }

    public void setMekSelectorRulesLevels(String rulesLevels) {
        store.setValue(MEK_SELECTOR_RULES_LEVELS, rulesLevels);
    }

    public void setMekSelectorSortColumn(int columnId) {
        store.setValue(MEK_SELECTOR_SORT_COLUMN, columnId);
    }

    public void setMekSelectorSortOrder(String order) {
        store.setValue(MEK_SELECTOR_SORT_ORDER, order);
    }

    public void setMekSelectorSizeHeight(int i) {
        store.setValue(MEK_SELECTOR_SIZE_HEIGHT, i);
    }

    public void setMekSelectorSizeWidth(int i) {
        store.setValue(MEK_SELECTOR_SIZE_WIDTH, i);
    }

    public void setMekSelectorPosX(int i) {
        store.setValue(MEK_SELECTOR_POS_X, i);
    }

    public void setMekSelectorSplitPos(int i) {
        store.setValue(MEK_SELECTOR_SPLIT_POS, i);
    }

    public void setMekSelectorPosY(int i) {
        store.setValue(MEK_SELECTOR_POS_Y, i);
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

    public void toggleBotCommandsEnabled() {
        setBotCommandsEnabled(!getBotCommandsEnabled());
    }

    public int getBotCommandsAutoDisplayNonReportPhase() {
        return store.getInt(BOT_COMMANDS_AUTO_DISPLAY_NON_REPORT_PHASE);
    }

    public int getBotCommandsAutoDisplayReportPhase() {
        return store.getInt(BOT_COMMANDS_AUTO_DISPLAY_REPORT_PHASE);
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

    public void setMiniMapSymbolsDisplayMode(int i) {
        store.setValue(MINI_MAP_SYMBOLS_DISPLAY_MODE, i);
    }

    public void setMinimapAutoDisplayReportPhase(int i) {
        store.setValue(MINI_MAP_AUTO_DISPLAY_REPORT_PHASE, i);
    }

    public void setMinimapAutoDisplayNonReportPhase(int i) {
        store.setValue(MINI_MAP_AUTO_DISPLAY_NON_REPORT_PHASE, i);
    }

    public void setFireDisplayTabDuringFiringPhases(boolean b) {
        store.setValue(FIRE_DISPLAY_TAB_DURING_PHASES, b);
    }

    public void setMoveDisplayTabDuringMovePhases(boolean b) {
        store.setValue(MOVE_DISPLAY_TAB_DURING_PHASES, b);
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
        store.setValue(MINI_REPORT_AUTO_DISPLAY_NON_REPORT_PHASE, i);
    }

    public void toggleMiniReportLocation() {
        store.setValue(MINI_REPORT_LOCATION, ((getInt(MINI_REPORT_LOCATION) + 1) % 2));
    }

    public void setMiniReportLocation(int i) {
        store.setValue(MINI_REPORT_LOCATION, i);
    }

    public void setMiniReportShowSprites(boolean b) {
        store.setValue(MINI_ROUND_REPORT_SPRITES, b);
    }

    public void setMiniReportShowPlayers(boolean b) {
        store.setValue(MINI_REPORT_SHOW_PLAYERS, b);
    }

    public void setMiniReportShowUnits(boolean b) {
        store.setValue(MINI_REPORT_SHOW_UNITS, b);
    }

    public void setMiniReportShowKeywords(boolean b) {
        store.setValue(MINI_REPORT_SHOW_KEYWORDS, b);
    }

    public void setMiniReportShowFilter(boolean b) {
        store.setValue(MINI_REPORT_SHOW_FILTER, b);
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
        store.setValue(PLAYER_LIST_AUTO_DISPLAY_NON_REPORT_PHASE, i);
    }

    public void setBoardEditLoadHeight(int i) {
        store.setValue(BOARD_EDIT_LOAD_SIZE_HEIGHT, i);
    }

    public void setBoardEditLoadWidth(int i) {
        store.setValue(BOARD_EDIT_LOAD_SIZE_WIDTH, i);
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
        store.setValue(NAG_FOR_NO_UNJAM_RAC, b);
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

    public void setShowSensorRange(boolean state) {
        store.setValue(SHOW_SENSOR_RANGE, state);
    }

    public void setShowMapHexPopup(boolean state) {
        store.setValue(SHOW_MAP_HEX_POPUP, state);
    }

    public void setShowWpsInTT(boolean state) {
        store.setValue(SHOW_WPS_IN_TT, state);
    }

    public void setShowWpsLocinTT(boolean state) {
        store.setValue(SHOW_WPS_LOC_IN_TT, state);
    }

    public void setShowArmorMiniVisTT(boolean state) {
        store.setValue(SHOW_ARMOR_MINI_VIS_TT, state);
    }

    public void setShowPilotPortraitTT(boolean state) {
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

    public void setMasterVolume(int state) {
        store.setValue(MASTER_VOLUME, state);
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
        store.setValue(TOOLTIP_DIST_SUPPRESSION, i);
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

    public void setMekInFirst(boolean b) {
        store.setValue(LOS_MEK_IN_FIRST, b);
    }

    public void setMekInSecond(boolean b) {
        store.setValue(LOS_MEK_IN_SECOND, b);
    }

    public void setShowMapSheets(boolean b) {
        store.setValue(SHOW_MAP_SHEETS, b);
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

    public void setSoftCenter(boolean b) {
        store.setValue(SOFT_CENTER, b);
    }

    public void setAutoCenter(boolean b) {
        store.setValue(AUTO_CENTER, b);
    }

    public void setAsCardFont(String asCardFont) {
        store.setValue(AS_CARD_FONT, asCardFont);
    }

    public void setSummaryFont(String summaryFont) {
        store.setValue(SUMMARY_FONT, summaryFont);
    }

    public void setAsCardSize(float size) {
        store.setValue(AS_CARD_SIZE, size);
    }

    public void setSbfSheetHeaderFont(String font) {
        store.setValue(SBF_SHEET_HEADER_FONT, font);
    }

    public void setSbfSheetValueFont(String font) {
        store.setValue(SBF_SHEET_VALUE_FONT, font);
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

    public String getRATNumMeks() {
        return store.getString(RAT_NUM_MEKS);
    }

    public void setRATNumMeks(String v) {
        store.setValue(RAT_NUM_MEKS, v);
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
        store.setValue(BOARD_EDIT_RANDOM_DIALOG_START, b);
    }

    // region Colours
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
        return getColor(WARNING_COLOR);
    }

    public void setWarningColor(Color color) {
        store.setValue(WARNING_COLOR, getColorString(color));
    }

    public Color getCautionColor() {
        return getColor(CAUTION_COLOR);
    }

    public void setCautionColor(Color color) {
        store.setValue(CAUTION_COLOR, getColorString(color));
    }

    public Color getPrecautionColor() {
        return getColor(PRECAUTION_COLOR);
    }

    public void setPrecautionColor(Color color) {
        store.setValue(PRECAUTION_COLOR, getColorString(color));
    }

    public Color getOkColor() {
        return getColor(OK_COLOR);
    }

    public void setOkColor(Color color) {
        store.setValue(OK_COLOR, getColorString(color));
    }

    public boolean getMoveDefaultClimbMode() {
        return getBoolean(BOARD_MOVE_DEFAULT_CLIMB_MODE);
    }

    public void setMoveDefaultClimbMode(boolean b) {
        store.setValue(BOARD_MOVE_DEFAULT_CLIMB_MODE, b);
    }

    public Color getMoveDefaultColor() {
        return getColor(BOARD_MOVE_DEFAULT_COLOR);
    }

    public void setMoveDefaultColor(Color color) {
        store.setValue(BOARD_MOVE_DEFAULT_COLOR, getColorString(color));
    }

    public Color getMoveIllegalColor() {
        return getColor(BOARD_MOVE_ILLEGAL_COLOR);
    }

    public void setMoveIllegalColor(Color color) {
        store.setValue(BOARD_MOVE_ILLEGAL_COLOR, getColorString(color));
    }

    public Color getMoveJumpColor() {
        return getColor(BOARD_MOVE_JUMP_COLOR);
    }

    public void setMoveJumpColor(Color color) {
        store.setValue(BOARD_MOVE_JUMP_COLOR, getColorString(color));
    }

    public Color getMoveMASCColor() {
        return getColor(BOARD_MOVE_MASC_COLOR);
    }

    public void setMoveMASCColor(Color color) {
        store.setValue(BOARD_MOVE_MASC_COLOR, getColorString(color));
    }

    public Color getMoveRunColor() {
        return getColor(BOARD_MOVE_RUN_COLOR);
    }

    public void setMoveRunColor(Color color) {
        store.setValue(BOARD_MOVE_RUN_COLOR, getColorString(color));
    }

    public Color getMoveBackColor() {
        return getColor(BOARD_MOVE_BACK_COLOR);
    }

    public void setMoveBackColor(Color color) {
        store.setValue(BOARD_MOVE_BACK_COLOR, getColorString(color));
    }

    public Color getMoveSprintColor() {
        return getColor(BOARD_MOVE_SPRINT_COLOR);
    }

    public void setMoveSprintColor(Color color) {
        store.setValue(BOARD_MOVE_SPRINT_COLOR, getColorString(color));
    }

    public int getMoveFontSize() {
        return getInt(BOARD_MOVE_FONT_SIZE);
    }

    public void setMoveFontSize(int i) {
        store.setValue(BOARD_MOVE_FONT_SIZE, i);
    }

    public int getMoveFontStyle() {
        return getInt(BOARD_MOVE_FONT_STYLE);
    }

    public void setMoveFontStyle(int i) {
        store.setValue(BOARD_MOVE_FONT_STYLE, i);
    }

    public String getMoveFontType() {
        return getString(BOARD_MOVE_FONT_TYPE);
    }

    public void setMoveFontType(String s) {
        store.setValue(BOARD_MOVE_FONT_TYPE, s);
    }

    public Color getFireSolnCanSeeColor() {
        return getColor(BOARD_FIRE_SOLUTION_CAN_SEE_COLOR);
    }

    public void setFireSolutionCanSeeColor(Color color) {
        store.setValue(BOARD_FIRE_SOLUTION_CAN_SEE_COLOR, getColorString(color));
    }

    public Color getFireSolnNoSeeColor() {
        return getColor(BOARD_FIRE_SOLUTION_NO_SEE_COLOR);
    }

    public void setFireSolutionNoSeeColor(Color color) {
        store.setValue(BOARD_FIRE_SOLUTION_NO_SEE_COLOR, getColorString(color));
    }

    public Color getBuildingTextColor() {
        return getColor(BOARD_BUILDING_TEXT_COLOR);
    }

    public void setBuildingTextColor(Color color) {
        store.setValue(BOARD_BUILDING_TEXT_COLOR, getColorString(color));
    }

    public Color getLowFoliageColor() {
        return getColor(BOARD_LOW_FOLIAGE_COLOR);
    }

    public void setLowFoliageColor(Color color) {
        store.setValue(BOARD_LOW_FOLIAGE_COLOR, getColorString(color));
    }

    public Color getBoardTextColor() {
        return getColor(BOARD_TEXT_COLOR);
    }

    public void setBoardTextColor(Color color) {
        store.setValue(BOARD_TEXT_COLOR, getColorString(color));
    }

    public Color getBoardSpaceTextColor() {
        return getColor(BOARD_SPACE_TEXT_COLOR);
    }

    public void setBoardSpaceTextColor(Color color) {
        store.setValue(BOARD_SPACE_TEXT_COLOR, getColorString(color));
    }

    public Color getMapsheetColor() {
        return getColor(BOARD_MAP_SHEET_COLOR);
    }

    public void setMapSheetColor(Color color) {
        store.setValue(BOARD_MAP_SHEET_COLOR, getColorString(color));
    }

    public Color getFieldOfFireMinColor() {
        return getColor(BOARD_FIELD_OF_FIRE_MIN_COLOR);
    }

    public void setFieldOfFireMinColor(Color color) {
        store.setValue(BOARD_FIELD_OF_FIRE_MIN_COLOR, getColorString(color));
    }

    public Color getFieldOfFireShortColor() {
        return getColor(BOARD_FIELD_OF_FIRE_SHORT_COLOR);
    }

    public void setFieldOfFireShortColor(Color color) {
        store.setValue(BOARD_FIELD_OF_FIRE_SHORT_COLOR, getColorString(color));
    }

    public Color getFieldOfFireMediumColor() {
        return getColor(BOARD_FIELD_OF_FIRE_MEDIUM_COLOR);
    }

    public void setBoardFieldOfFireMediumColor(Color color) {
        store.setValue(BOARD_FIELD_OF_FIRE_MEDIUM_COLOR, getColorString(color));
    }

    public Color getFieldOfFireLongColor() {
        return getColor(BOARD_FIELD_OF_FIRE_LONG_COLOR);
    }

    public void setFieldOfFireLongColor(Color color) {
        store.setValue(BOARD_FIELD_OF_FIRE_LONG_COLOR, getColorString(color));
    }

    public Color getFieldOfFireExtremeColor() {
        return getColor(BOARD_FIELD_OF_FIRE_EXTREME_COLOR);
    }

    public void setFieldOfFireExtremeColor(Color color) {
        store.setValue(BOARD_FIELD_OF_FIRE_EXTREME_COLOR, getColorString(color));
    }

    public Color getSensorRangeColor() {
        return getColor(BOARD_SENSOR_RANGE_COLOR);
    }

    public void setSensorRangeColor(Color color) {
        store.setValue(BOARD_SENSOR_RANGE_COLOR, getColorString(color));
    }

    public void setVisualRangeColor(Color color) {
        store.setValue(BOARD_VISUAL_RANGE_COLOR, getColorString(color));
    }

    public Color getVisualRangeColor() {
        return getColor(BOARD_VISUAL_RANGE_COLOR);
    }

    public Color getUnitValidColor() {
        return getColor(BOARD_UNIT_VALID_COLOR);
    }

    public void setUnitValidColor(Color color) {
        store.setValue(BOARD_UNIT_VALID_COLOR, getColorString(color));
    }

    public Color getUnitSelectedColor() {
        return getColor(BOARD_UNIT_SELECTED_COLOR);
    }

    public void setUnitSelectedColor(Color color) {
        store.setValue(BOARD_UNIT_SELECTED_COLOR, getColorString(color));
    }

    public Color getUnitTextColor() {
        return getColor(BOARD_UNIT_TEXT_COLOR);
    }

    public void setUnitOverviewTextColor(Color color) {
        store.setValue(BOARD_UNIT_TEXT_COLOR, getColorString(color));
    }

    public int getAttackArrowTransparency() {
        return getInt(BOARD_ATTACK_ARROW_TRANSPARENCY);
    }

    public void setAttackArrowTransparency(int i) {
        store.setValue(BOARD_ATTACK_ARROW_TRANSPARENCY, i);
    }

    public int getECMTransparency() {
        return getInt(BOARD_ECM_TRANSPARENCY);
    }

    public void setECMTransparency(int i) {
        store.setValue(BOARD_ECM_TRANSPARENCY, i);
    }

    public boolean getDarkenMapAtNight() {
        return getBoolean(BOARD_DARKEN_MAP_AT_NIGHT);
    }

    public void setDarkenMapAtNight(boolean b) {
        store.setValue(BOARD_DARKEN_MAP_AT_NIGHT, b);
    }

    public boolean getTranslucentHiddenUnits() {
        return getBoolean(BOARD_TRANSLUCENT_HIDDEN_UNITS);
    }

    public void setTranslucentHiddenUnits(boolean b) {
        store.setValue(BOARD_TRANSLUCENT_HIDDEN_UNITS, b);
    }

    public void setShowArtilleryMisses(boolean b) {
        store.setValue(SHOW_ARTILLERY_MISSES, b);
    }

    public boolean getShowArtilleryMisses() {
        return getBoolean(SHOW_ARTILLERY_MISSES);
    }

    public void setShowArtilleryDrifts(boolean b) {
        store.setValue(SHOW_ARTILLERY_DRIFTS, b);
    }

    public boolean getShowArtilleryDrifts() {
        return getBoolean(SHOW_ARTILLERY_DRIFTS);
    }

    public void setShowBombMisses(boolean b) {
        store.setValue(SHOW_BOMB_MISSES, b);
    }

    public boolean getShowBombMisses() {
        return getBoolean(SHOW_BOMB_MISSES);
    }

    public void setShowBombDrifts(boolean b) {
        store.setValue(SHOW_BOMB_DRIFTS, b);
    }

    public boolean getShowBombDrifts() {
        return getBoolean(SHOW_BOMB_DRIFTS);
    }

    public int getTMMPipMode() {
        return getInt(BOARD_TMM_PIP_MODE);
    }

    public void setTMMPipMode(int i) {
        store.setValue(BOARD_TMM_PIP_MODE, i);
    }

    public Color getUnitOverviewTextShadowColor() {
        return getColor(UNIT_OVERVIEW_TEXT_SHADOW_COLOR);
    }

    public void setUnitOverviewTextShadowColor(Color color) {
        store.setValue(UNIT_OVERVIEW_TEXT_SHADOW_COLOR, getColorString(color));
    }

    public Color getUnitOverviewConditionShadowColor() {
        return getColor(UNIT_OVERVIEW_CONDITION_SHADOW_COLOR);
    }

    public void setUnitOverviewConditionShadowColor(Color color) {
        store.setValue(UNIT_OVERVIEW_CONDITION_SHADOW_COLOR, getColorString(color));
    }

    public int getButtonsPerRow() {
        return getInt(BUTTONS_PER_ROW);
    }

    public void setButtonsPerRow(int i) {
        store.setValue(BUTTONS_PER_ROW, i);
    }

    public Color getReportLinkColor() {
        return getColor(MINI_REPORT_COLOR_LINK);
    }

    public Color getReportSuccessColor() {
        return getColor(MINI_REPORT_COLOR_SUCCESS);
    }

    public Color getReportMissColor() {
        return getColor(MINI_REPORT_COLOR_MISS);
    }

    public Color getReportInfoColor() {
        return getColor(MINI_REPORT_COLOR_INFO);
    }

    public String getReportFontType() {
        return getString(MINI_REPORT_FONT_TYPE);
    }

    public int getUnitToolTipSeenByResolution() {
        return getInt(UNIT_TOOLTIP_SEEN_BY_RESOLUTION);
    }

    public String getUnitToolTipArmorMiniArmorChar() {
        return getString(UNIT_TOOLTIP_ARMOR_MINI_ARMOR_CHAR);
    }

    public String getUnitToolTipArmorMiniISChar() {
        return getString(UNIT_TOOLTIP_ARMOR_MINI_IS_CHAR);
    }

    public String getUnitToolTipArmorMiniCriticalChar() {
        return getString(UNIT_TOOLTIP_ARMOR_MINI_CRITICAL_CHAR);
    }

    public String getUnitToolTipArmorMiniDestroyedChar() {
        return getString(UNIT_TOOLTIP_ARMOR_MINI_DESTROYED_CHAR);
    }

    public String getUnitToolTipArmorMiniCapArmorChar() {
        return getString(UNIT_TOOLTIP_ARMOR_MINI_CAP_ARMOR_CHAR);
    }

    public int getUnitToolTipArmorMiniUnitsPerBlock() {
        return getInt(UNIT_TOOLTIP_ARMOR_MINI_UNITS_PER_BLOCK);
    }

    public String getUnitToolTipFontSizeMod() {
        return getString(UNIT_TOOLTIP_FONT_SIZE_MOD);
    }

    public Color getUnitToolTipFGColor() {
        return getColor(UNIT_TOOLTIP_FG_COLOR);
    }

    public Color getToolTipLightFGColor() {
        return getColor(UNIT_TOOLTIP_LIGHT_FG_COLOR);
    }

    public Color getUnitToolTipBuildingFGColor() {
        return getColor(UNIT_TOOLTIP_BUILDING_FG_COLOR);
    }

    public Color getUnitToolTipAltFGColor() {
        return getColor(UNIT_TOOLTIP_ALT_FG_COLOR);
    }

    public Color getUnitToolTipBlockFGColor() {
        return getColor(UNIT_TOOLTIP_BLOCK_FG_COLOR);
    }

    public Color getUnitToolTipTerrainFGColor() {
        return getColor(UNIT_TOOLTIP_TERRAIN_FG_COLOR);
    }

    public Color getUnitToolTipBGColor() {
        return getColor(UNIT_TOOLTIP_BGCOLOR);
    }

    public Color getUnitToolTipBuildingBGColor() {
        return getColor(UNIT_TOOLTIP_BUILDING_BGCOLOR);
    }

    public Color getUnitToolTipAltBGColor() {
        return getColor(UNIT_TOOLTIP_ALT_BGCOLOR);
    }

    public Color getUnitToolTipBlockBGColor() {
        return getColor(UNIT_TOOLTIP_BLOCK_BGCOLOR);
    }

    public Color getUnitToolTipTerrainBGColor() {
        return getColor(UNIT_TOOLTIP_TERRAIN_BGCOLOR);
    }

    public Color getUnitToolTipHighlightColor() {
        return getColor(UNIT_TOOLTIP_HIGHLIGHT_COLOR);
    }

    public Color getUnitToolTipWeaponColor() {
        return getColor(UNIT_TOOLTIP_WEAPON_COLOR);
    }

    public Color getUnitToolTipQuirkColor() {
        return getColor(UNIT_TOOLTIP_QUIRK_COLOR);
    }

    public boolean getDockOnLeft() {
        return getBoolean(DOCK_ON_LEFT);
    }

    public boolean getDockMultipleOnYAxis() {
        return getBoolean(DOCK_MULTIPLE_ON_Y_AXIS);
    }

    public boolean getUseCamoOverlay() {
        return getBoolean(USE_CAMO_OVERLAY);
    }

    public int getPlayersRemainingToShow() {
        return getInt(PLAYERS_REMAINING_TO_SHOW);
    }

    public boolean getBotCommandsEnabled() {
        return getBoolean(BOT_COMMANDS_ENABLED);
    }

    public void setReportLinkColor(Color color) {
        store.setValue(MINI_REPORT_COLOR_LINK, getColorString(color));
    }

    public void setReportSuccessColor(Color color) {
        store.setValue(MINI_REPORT_COLOR_SUCCESS, getColorString(color));
    }

    public void setReportMissColor(Color color) {
        store.setValue(MINI_REPORT_COLOR_MISS, getColorString(color));
    }

    public void setReportInfoColo(Color color) {
        store.setValue(MINI_REPORT_COLOR_INFO, getColorString(color));
    }

    public void setReportFontType(String font) {
        store.setValue(MINI_REPORT_FONT_TYPE, font);
    }

    public Color getPlanetaryConditionsColorTitle() {
        return getColor(PLANETARY_CONDITIONS_COLOR_TITLE);
    }

    public Color getPlanetaryConditionsColorText() {
        return getColor(PLANETARY_CONDITIONS_COLOR_TEXT);
    }

    public Color getPlanetaryConditionsColorCold() {
        return getColor(PLANETARY_CONDITIONS_COLOR_COLD);
    }

    public Color getPlanetaryConditionsColorHot() {
        return getColor(PLANETARY_CONDITIONS_COLOR_HOT);
    }

    public Color getPlanetaryConditionsColorBackground() {
        return getColor(PLANETARY_CONDITIONS_COLOR_BACKGROUND);
    }

    public Boolean getPlanetaryConditionsShowDefaults() {
        return getBoolean(PLANETARY_CONDITIONS_SHOW_DEFAULTS);
    }

    public Boolean getPlanetaryConditionsShowHeader() {
        return getBoolean(PLANETARY_CONDITIONS_SHOW_HEADER);
    }

    public Boolean getPlanetaryConditionsShowLabels() {
        return getBoolean(PLANETARY_CONDITIONS_SHOW_LABELS);
    }

    public Boolean getPlanetaryConditionsShowValues() {
        return getBoolean(PLANETARY_CONDITIONS_SHOW_VALUES);
    }

    public Boolean getPlanetaryConditionsShowIndicators() {
        return getBoolean(PLANETARY_CONDITIONS_SHOW_INDICATORS);
    }

    public int getPlanetaryConditionsBackgroundTransparency() {
        return getInt(PLANETARY_CONDITIONS_BACKGROUND_TRANSPARENCY);
    }

    public void setPlanetaryConditionsColorTitle(Color color) {
        store.setValue(PLANETARY_CONDITIONS_COLOR_TITLE, getColorString(color));
    }

    public void setPlanetaryConditionsColorText(Color color) {
        store.setValue(PLANETARY_CONDITIONS_COLOR_TEXT, getColorString(color));
    }

    public void setPlanetaryConditionsColorCold(Color color) {
        store.setValue(PLANETARY_CONDITIONS_COLOR_COLD, getColorString(color));
    }

    public void setPlanetaryConditionsColorHot(Color color) {
        store.setValue(PLANETARY_CONDITIONS_COLOR_HOT, getColorString(color));
    }

    public void setPlanetaryConditionsColorBackground(Color color) {
        store.setValue(PLANETARY_CONDITIONS_COLOR_BACKGROUND, getColorString(color));
    }

    public void setPlanetaryConditionsShowDefaults(Boolean state) {
        store.setValue(PLANETARY_CONDITIONS_SHOW_DEFAULTS, state);
    }

    public void setPlanetaryConditionsShowHeader(Boolean state) {
        store.setValue(PLANETARY_CONDITIONS_SHOW_HEADER, state);
    }

    public void setPlanetaryConditionsShowLabels(Boolean state) {
        store.setValue(PLANETARY_CONDITIONS_SHOW_LABELS, state);
    }

    public void setPlanetaryConditionsShowValues(Boolean state) {
        store.setValue(PLANETARY_CONDITIONS_SHOW_VALUES, state);
    }

    public void setPlanetaryConditionsShowIndicators(Boolean state) {
        store.setValue(PLANETARY_CONDITIONS_SHOW_INDICATORS, state);
    }

    public void setPlanetaryConditionsBackgroundTransparency(int i) {
        store.setValue(PLANETARY_CONDITIONS_BACKGROUND_TRANSPARENCY, i);
    }

    public int getTraceOverlayTransparency() {
        return getInt(TRACE_OVERLAY_TRANSPARENCY);
    }

    public void setTraceOverlayTransparency(int i) {
        store.setValue(TRACE_OVERLAY_TRANSPARENCY, i);
    }

    public int getTraceOverlayScale() {
        return getInt(TRACE_OVERLAY_SCALE);
    }

    public void setTraceOverlayScale(int i) {
        store.setValue(TRACE_OVERLAY_SCALE, i);
    }

    public int getTraceOverlayOriginX() {
        return getInt(TRACE_OVERLAY_ORIGIN_X);
    }

    public void setTraceOverlayOriginX(int i) {
        store.setValue(TRACE_OVERLAY_ORIGIN_X, i);
    }

    public int getTraceOverlayOriginY() {
        return getInt(TRACE_OVERLAY_ORIGIN_Y);
    }

    public void setTraceOverlayOriginY(int i) {
        store.setValue(TRACE_OVERLAY_ORIGIN_Y, i);
    }

    public String getTraceOverlayImageFile() {
        return getString(TRACE_OVERLAY_IMAGE_FILE);
    }

    public void setTraceOverlayImageFile(String s) {
        store.setValue(TRACE_OVERLAY_IMAGE_FILE, s);
    }

    public void setUnitToolTipSeenByResolution(int i) {
        store.setValue(UNIT_TOOLTIP_SEEN_BY_RESOLUTION, i);
    }

    public void setUnitToolTipArmorMiniArmorChar(String s) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_ARMOR_CHAR, s);
    }

    public void setUnitToolTipArmorMiniISChar(String s) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_IS_CHAR, s);
    }

    public void setUnitToolTipArmorMiniCriticalChar(String s) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_CRITICAL_CHAR, s);
    }

    public void setUnitTooltipArmorMiniDestroyedChar(String s) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_DESTROYED_CHAR, s);
    }

    public void setUnitTooltipArmorMiniCapArmorChar(String s) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_CAP_ARMOR_CHAR, s);
    }

    public void setUnitTooltipArmorMiniUnitsPerBlock(int i) {
        store.setValue(UNIT_TOOLTIP_ARMOR_MINI_UNITS_PER_BLOCK, i);
    }

    public void setUnitToolTipFontSize(String s) {
        store.setValue(UNIT_TOOLTIP_FONT_SIZE_MOD, s);
    }

    public void setUnitToolTipFGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_FG_COLOR, getColorString(c));
    }

    public void setUnitTooltipLightFGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_LIGHT_FG_COLOR, getColorString(c));
    }

    public void setUnitTooltipBuildingFGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_BUILDING_FG_COLOR, getColorString(c));
    }

    public void setUnitTooltipAltFGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_ALT_FG_COLOR, getColorString(c));
    }

    public void setUnitTooltipBlockFGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_BLOCK_FG_COLOR, getColorString(c));
    }

    public void setUnitTooltipTerrainFGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_TERRAIN_FG_COLOR, getColorString(c));
    }

    public void setUnitToolTipBGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_BGCOLOR, getColorString(c));
    }

    public void setUnitTooltipBuildingBGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_BUILDING_BGCOLOR, getColorString(c));
    }

    public void setUnitTooltipAltBGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_ALT_BGCOLOR, getColorString(c));
    }

    public void setUnitTooltipBlockBGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_BLOCK_BGCOLOR, getColorString(c));
    }

    public void setUnitTooltipTerrainBGColor(Color c) {
        store.setValue(UNIT_TOOLTIP_TERRAIN_BGCOLOR, getColorString(c));
    }

    public void setUnitTooltipHighlightColor(Color c) {
        store.setValue(UNIT_TOOLTIP_HIGHLIGHT_COLOR, getColorString(c));
    }

    public void setUnitTooltipWeaponColor(Color c) {
        store.setValue(UNIT_TOOLTIP_WEAPON_COLOR, getColorString(c));
    }

    public void setUnitTooltipQuirkColor(Color c) {
        store.setValue(UNIT_TOOLTIP_QUIRK_COLOR, getColorString(c));
    }

    public void setDockOnLeft(Boolean state) {
        store.setValue(DOCK_ON_LEFT, state);
    }

    public void setDockMultipleOnYAxis(Boolean state) {
        store.setValue(DOCK_MULTIPLE_ON_Y_AXIS, state);
    }

    public void setUseCamoOverlay(Boolean state) {
        store.setValue(USE_CAMO_OVERLAY, state);
    }

    public void setPlayersRemainingToShow(int i) {
        store.setValue(PLAYERS_REMAINING_TO_SHOW, i);
    }

    public void setBotCommandsEnabled(boolean state) {
        store.setValue(BOT_COMMANDS_ENABLED, state);
    }

    public void setBotCommandsPosX(int i) {
        store.setValue(BOT_COMMANDS_POS_X, i);
    }

    public void setBotCommandsPosY(int i) {
        store.setValue(BOT_COMMANDS_POS_Y, i);
    }

    public int getBotCommandsPosX() {
        return getInt(BOT_COMMANDS_POS_X);
    }

    public int getBotCommandsPosY() {
        return getInt(BOT_COMMANDS_POS_Y);
    }

    public void setBotCommandsHeight(int i) {
        store.setValue(BOT_COMMANDS_HEIGHT, i);
    }

    public int getBotCommandsHeight() {
        return getInt(BOT_COMMANDS_HEIGHT);
    }

    public void setBotCommandsWidth(int i) {
        store.setValue(BOT_COMMANDS_WIDTH, i);
    }

    public int getBotCommandsWidth() {
        return getInt(BOT_COMMANDS_WIDTH);
    }

    /**
     * Toggles the state of the user preference for the Keybinds overlay.
     */
    public void toggleKeybindsOverlay() {
        store.setValue(SHOW_KEYBINDS_OVERLAY, !getBoolean(SHOW_KEYBINDS_OVERLAY));
    }

    public boolean getShowKeybindsOverlay() {
        return getBoolean(SHOW_KEYBINDS_OVERLAY);
    }

    public void togglePlanetaryConditionsOverlay() {
        store.setValue(SHOW_PLANETARY_CONDITIONS_OVERLAY, !getBoolean(SHOW_PLANETARY_CONDITIONS_OVERLAY));
    }

    public boolean getShowPlanetaryConditionsOverlay() {
        return getBoolean(SHOW_PLANETARY_CONDITIONS_OVERLAY);
    }

    public void toggleTraceConditionsOverlay() {
        store.setValue(SHOW_TRACE_OVERLAY, !getBoolean(SHOW_TRACE_OVERLAY));
    }

    public boolean getShowTraceOverlay() {
        return getBoolean(SHOW_TRACE_OVERLAY);
    }

    public void setShowPlanetaryConditionsOverlay(boolean b) {
        store.setValue(SHOW_PLANETARY_CONDITIONS_OVERLAY, b);
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
        return switch (movementType) {
            case MOVE_RUN, MOVE_VTOL_RUN, MOVE_OVER_THRUST -> getColor(BOARD_MOVE_RUN_COLOR);
            case MOVE_JUMP -> getColor(BOARD_MOVE_JUMP_COLOR);
            case MOVE_SPRINT, MOVE_VTOL_SPRINT -> getColor(BOARD_MOVE_SPRINT_COLOR);
            case MOVE_ILLEGAL -> getColor(BOARD_MOVE_ILLEGAL_COLOR);
            default -> getColor(BOARD_MOVE_DEFAULT_COLOR);
        };
    }

    /**
     * @return The color associated with a movement type
     */
    public Color getColorForMovement(EntityMovementType movementType, boolean isMASCOrSuperCharger,
          boolean isBackwards) {
        if (movementType != EntityMovementType.MOVE_ILLEGAL) {
            if (isMASCOrSuperCharger) {
                return getColor(BOARD_MOVE_MASC_COLOR);
            } else if (isBackwards) {
                return getColor(BOARD_MOVE_BACK_COLOR);
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
        } else if (heat <= getUnitDisplayHeatValue1()) {
            return getColor(UNIT_DISPLAY_HEAT_COLOR_1);
        } else if (heat <= getUnitDisplayHeatValue2()) {
            return getColor(UNIT_DISPLAY_HEAT_COLOR_2);
        } else if (heat <= getUnitDisplayHeatValue3()) {
            return getColor(UNIT_DISPLAY_HEAT_COLOR_3);
        } else if (heat <= getUnitDisplayHeatValue4()) {
            return getColor(UNIT_DISPLAY_HEAT_COLOR_4);
        } else if (heat <= getUnitDisplayHeatValue5()) {
            return getColor(UNIT_DISPLAY_HEAT_COLOR_5);
        } else if (heat <= getUnitDisplayHeatValue6()) {
            return getColor(UNIT_DISPLAY_HEAT_COLOR_6);
        }
        return getColor(UNIT_DISPLAY_HEAT_COLOR_OVERHEAT);
    }

    public void setUnitLabelStyle(LabelDisplayStyle style) {
        store.setValue(UNIT_LABEL_STYLE, style.name());
    }

    public Color getColor(String name) {
        final String text = store.getString(name);
        final Color colour = parseRGB(text);
        return (colour == null) ? PlayerColour.parseFromString(text).getColour() : colour;
    }

    public static String hexColor(Color color) {
        return String.format("#%06x", color.getRGB() & 0x00FFFFFF);
    }

    public void setColor(String name, Color c) {
        store.setValue(name, getColorString(c));
    }

    protected String getColorString(Color colour) {
        return colour.getRed() + " " + colour.getGreen() + " " + colour.getBlue();
    }

    protected Color parseRGB(String text) {
        final String[] codesText = text.split(" ");
        if (codesText.length == 3) {
            int[] codes = new int[codesText.length];
            for (int i = 0;
                  i < codesText.length;
                  i++) {
                codes[i] = Integer.parseInt(codesText[i]);
            }
            return new Color(codes[0], codes[1], codes[2]);
        }
        return Color.BLUE;
    }
    // endregion Colours

    /**
     * @return True when the MM suite supports the given laf, currently all formdev "Flat ..." and the system default.
     */
    public static boolean isSupportedLookAndFeel(UIManager.LookAndFeelInfo lookAndFeelInfo) {
        return lookAndFeelInfo.getClassName().toLowerCase().contains("formdev");
    }

    public File[] getMinimapThemes() {
        // List all .theme files inside the minimap themes folder
        return Configuration.minimapThemesDir().listFiles((dir, name) -> name.endsWith(".theme"));
    }

    public boolean getDrawFacingArrowsOnMiniMap() {
        return getBoolean(MINI_MAP_SHOW_SENSOR_RANGE);
    }

    public boolean getDrawSensorRangeOnMiniMap() {
        return getBoolean(MINI_MAP_SHOW_FACING_ARROW);
    }

    public void setDrawFacingArrowsOnMiniMap(boolean state) {
        store.setValue(MINI_MAP_SHOW_SENSOR_RANGE, state);
    }

    public void setDrawSensorRangeOnMiniMap(boolean state) {
        store.setValue(MINI_MAP_SHOW_FACING_ARROW, state);
    }

    public boolean paintBorders() {
        return getBoolean(MINI_MAP_PAINT_BORDERS);
    }

    public void setPaintBorders(boolean state) {
        store.setValue(MINI_MAP_PAINT_BORDERS, state);
    }

    public int getMovePathPersistenceOnMiniMap() {
        return getInt(MINI_MAP_MOVE_PATH_PERSISTENCE);
    }

    public void setMovePathPersistenceOnMiniMap(int rounds) {
        store.setValue(MINI_MAP_MOVE_PATH_PERSISTENCE, rounds);
    }

    /**
     * Returns the size and position of a saved named window.
     *
     * @param name The name of the window.
     *
     * @return An Optional containing the Rectangle representing the size and position, or an empty Optional if not
     *       found.
     */
    public Optional<Rectangle> getNamedWindowSizeAndPosition(String name) {
        final String storedData = getString(name + _WINDOW);
        if (storedData == null) {
            return Optional.empty();
        }
        String[] windowCoords = storedData.split(DELIMITER);
        if (windowCoords.length < 4) {
            return Optional.empty();
        }
        try {
            int x = Integer.parseInt(windowCoords[0]);
            int y = Integer.parseInt(windowCoords[1]);
            int width = Integer.parseInt(windowCoords[2]);
            int height = Integer.parseInt(windowCoords[3]);
            return Optional.of(new Rectangle(x, y, width, height));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Saves the size and position of a named window.
     *
     * @param name      The name of the window.
     * @param component The component representing the window.
     */
    public void setNamedWindowSizeAndPosition(String name, Window component) {
        Dimension size = component.getSize();
        Point pos = component.getLocation();
        store.setValue(name + _WINDOW, pos.x + DELIMITER + pos.y + DELIMITER + size.width + DELIMITER + size.height);
    }

    /**
     * Returns the tab order for a given saved name (for example: the name of a window).
     *
     * @param name The name of the tab order.
     *
     * @return A List of Strings representing the tab order, or null if not found.
     */
    public @Nullable List<String> getTabOrder(String name) {
        final String storedData = getString(name + _TAB_ORDER);
        if (storedData == null) {
            return null;
        }
        return Arrays.asList(storedData.split(DELIMITER));
    }

    /**
     * Saves the tab order for a given name (for example: the name of a window).
     *
     * @param name  The name of the tab order.
     * @param order A List of Strings representing the tab order.
     */
    public void setTabOrder(String name, List<String> order) {
        final String storedTabOrder = String.join(DELIMITER, order);
        store.setValue(name + _TAB_ORDER, storedTabOrder);
    }

    public boolean getHighPerformanceGraphics() {
        return getBoolean(HIGH_PERFORMANCE_GRAPHICS);
    }

    public void setHighPerformanceGraphics(boolean value) {
        store.setValue(HIGH_PERFORMANCE_GRAPHICS, value);
    }

    public boolean getNagForOddSizedBoard() {
        return getBoolean(NAG_FOR_ODD_SIZED_BOARD);
    }

    public void setNagForOddSizedBoard(boolean value) {
        store.setValue(NAG_FOR_ODD_SIZED_BOARD, value);
    }

    public void setForceDisplayBtnID(boolean value) {store.setValue(FORCE_DISPLAY_BTN_ID, value);}

    public boolean getForceDisplayBtnID() {return getBoolean(FORCE_DISPLAY_BTN_ID);}

    public void setForceDisplayBtnPilot(boolean value) {store.setValue(FORCE_DISPLAY_BTN_PILOT, value);}

    public boolean getForceDisplayBtnPilot() {return getBoolean(FORCE_DISPLAY_BTN_PILOT);}

    public void setForceDisplayBtnMP(boolean value) {store.setValue(FORCE_DISPLAY_BTN_MP, value);}

    public boolean getForceDisplayBtnMP() {return getBoolean(FORCE_DISPLAY_BTN_MP);}

    public void setForceDisplayBtnHeat(boolean value) {store.setValue(FORCE_DISPLAY_BTN_HEAT, value);}

    public boolean getForceDisplayBtnHeat() {return getBoolean(FORCE_DISPLAY_BTN_HEAT);}

    public void setForceDisplayBtnWeapons(boolean value) {store.setValue(FORCE_DISPLAY_BTN_WEAPONS, value);}

    public boolean getForceDisplayBtnWeapons() {return getBoolean(FORCE_DISPLAY_BTN_WEAPONS);}

    public void setForceDisplayBtnDamageDesc(boolean value) {store.setValue(FORCE_DISPLAY_BTN_DAMAGE_DESC, value);}

    public boolean getForceDisplayBtnDamageDesc() {return getBoolean(FORCE_DISPLAY_BTN_DAMAGE_DESC);}

    public void setForceDisplayBtnArmor(boolean value) {store.setValue(FORCE_DISPLAY_BTN_ARMOR, value);}

    public boolean getForceDisplayBtnArmor() {return getBoolean(FORCE_DISPLAY_BTN_ARMOR);}

    public void setForceDisplayBtnTonnage(boolean value) {store.setValue(FORCE_DISPLAY_BTN_TONNAGE, value);}

    public boolean getForceDisplayBtnTonnage() {return getBoolean(FORCE_DISPLAY_BTN_TONNAGE);}

    public void setForceDisplayBtnRole(boolean value) {store.setValue(FORCE_DISPLAY_BTN_ROLE, value);}

    public boolean getForceDisplayBtnRole() {return getBoolean(FORCE_DISPLAY_BTN_ROLE);}

    public void setForceDisplayBtnECM(boolean value) {store.setValue(FORCE_DISPLAY_BTN_ECM, value);}

    public boolean getForceDisplayBtnECM() {return getBoolean(FORCE_DISPLAY_BTN_ECM);}

    public void setForceDisplayBtnQuirks(boolean value) {store.setValue(FORCE_DISPLAY_BTN_QUIRKS, value);}

    public boolean getForceDisplayBtnQuirks() {return getBoolean(FORCE_DISPLAY_BTN_QUIRKS);}

    public void setForceDisplayBtnC3(boolean value) {store.setValue(FORCE_DISPLAY_BTN_C3, value);}

    public boolean getForceDisplayBtnC3() {return getBoolean(FORCE_DISPLAY_BTN_C3);}

    public void setForceDisplayBtnMisc(boolean value) {store.setValue(FORCE_DISPLAY_BTN_MISC, value);}

    public boolean getForceDisplayBtnMisc() {return getBoolean(FORCE_DISPLAY_BTN_MISC);}
}
