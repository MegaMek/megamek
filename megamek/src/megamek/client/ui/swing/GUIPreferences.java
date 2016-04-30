/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.ToolTipManager;

import megamek.client.ui.swing.util.ColorParser;
import megamek.common.Entity;
import megamek.common.preference.PreferenceManager;
import megamek.common.preference.PreferenceStoreProxy;

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
    public static final String ADVANCED_CHATBOX_SIZE = "AdvancedChatboxSize";
    public static final String ADVANCED_CHAT_LOUNGE_TAB_FONT_SIZE = "AdvancedChatLoungeTabFontSize";
    public static final String ADVANCED_MECH_DISPLAY_ARMOR_LARGE_FONT_SIZE = "AdvancedMechDisplayArmorLargeFontSize";
    public static final String ADVANCED_MECH_DISPLAY_ARMOR_MEDIUM_FONT_SIZE = "AdvancedMechDisplayArmorMediumFontSize";
    public static final String ADVANCED_MECH_DISPLAY_ARMOR_SMALL_FONT_SIZE = "AdvancedMechDisplayArmorSmallFontSize";
    public static final String ADVANCED_MECH_DISPLAY_LARGE_FONT_SIZE = "AdvancedMechDisplayLargeFontSize";
    public static final String ADVANCED_MECH_DISPLAY_MEDIUM_FONT_SIZE = "AdvancedMechDisplayMediumFontSize";
    public static final String ADVANCED_MECH_DISPLAY_WRAP_LENGTH = "AdvancedMechDisplayWrapLength";
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
    public static final String ADVANCED_CHATBOX2_FONTSIZE = "AdvancedChatbox2Fontsize";
    public static final String ADVANCED_CHATBOX2_BACKCOLOR = "AdvancedChatbox2BackColor";
    public static final String ADVANCED_CHATBOX2_TRANSPARANCY = "AdvancedChatbox2Transparancy";
    public static final String ADVANCED_CHATBOX2_AUTOSLIDEDOWN = "AdvancedChatbox2AutoSlidedown";
    public static final String ADVANCED_ECM_TRANSPARENCY = "AdvancedECMTransparency";
    public static final String ADVANCED_UNITOVERVIEW_SELECTED_COLOR = "AdvancedUnitOverviewSelectedColor";
    public static final String ADVANCED_UNITOVERVIEW_VALID_COLOR = "AdvancedUnitOverviewValidColor";
    public static final String ADVANCED_KEY_REPEAT_DELAY = "AdvancedKeyRepeatDelay";
    public static final String ADVANCED_KEY_REPEAT_RATE = "AdvancedKeyRepeatRate";
    public static final String ADVANCED_SHOW_FPS = "AdvancedShowFPS";
    public static final String ADVANCED_SHOW_COORDS = "AdvancedShowCoords";
    public static final String ADVANCED_BUTTONS_PER_ROW = "AdvancedButtonsPerRow";
    /* --End advanced settings-- */


    public static final String ANTIALIASING = "AntiAliasing";
    public static final String SHADOWMAP = "ShadowMap";
    public static final String AOHEXSHADOWS = "AoHexShadows";
    public static final String LEVELHIGHLIGHT = "LevelHighlight";
    public static final String FLOATINGISO = "FloatingIsometric";
    public static final String MMSYMBOL = "MmSymbol";
    public static final String SOFTCENTER = "SoftCenter";
    public static final String AUTO_END_FIRING = "AutoEndFiring";
    public static final String AUTO_DECLARE_SEARCHLIGHT = "AutoDeclareSearchlight";
    public static final String CHAT_LOUNGE_TABS = "ChatLoungeTabs";
    public static final String DISPLAY_POS_X = "DisplayPosX";
    public static final String DISPLAY_POS_Y = "DisplayPosY";
    public static final String DISPLAY_SIZE_HEIGHT = "DisplaySizeHeight";
    public static final String DISPLAY_SIZE_WIDTH = "DisplaySizeWidth";
    public static final String FOCUS = "Focus";
    public static final String GAME_OPTIONS_SIZE_HEIGHT = "GameOptionsSizeHeight";
    public static final String GAME_OPTIONS_SIZE_WIDTH = "GameOptionsSizeWidth";
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
    public static final String MAP_TEXT_COLOR = "MapTextColor";
    public static final String MAP_ZOOM_INDEX = "MapZoomIndex";
    public static final String MECH_SELECTOR_INCLUDE_MODEL = "MechSelectorIncludeModel";
    public static final String MECH_SELECTOR_INCLUDE_NAME = "MechSelectorIncludeName";
    public static final String MECH_SELECTOR_INCLUDE_TONS = "MechSelectorIncludeTons";
    public static final String MECH_SELECTOR_INCLUDE_BV = "MechSelectorIncludeBV";
    public static final String MECH_SELECTOR_INCLUDE_YEAR = "MechSelectorIncludeYear";
    public static final String MECH_SELECTOR_INCLUDE_LEVEL = "MechSelectorIncludeLevel";
    public static final String MECH_SELECTOR_INCLUDE_COST = "MechSelectorIncludeCost";
    public static final String MECH_SELECTOR_SHOW_ADVANCED = "MechSelectorShowAdvanced";
    public static final String MECH_SELECTOR_UNIT_TYPE= "MechSelectorUnitType";
    public static final String MECH_SELECTOR_WEIGHT_CLASS= "MechSelectorWeightClass";
    public static final String MECH_SELECTOR_RULES_LEVELS= "MechSelectorRuleType";
    public static final String MECH_SELECTOR_SIZE_HEIGHT = "MechSelectorSizeHeight";
    public static final String MECH_SELECTOR_SIZE_WIDTH = "MechSelectorSizeWidth";
    public static final String MINI_REPORT_POS_X = "MiniReportPosX";
    public static final String MINI_REPORT_POS_Y = "MiniReportPosY";
    public static final String MINI_REPORT_SIZE_HEIGHT = "MiniReportSizeHeight";
    public static final String MINI_REPORT_SIZE_WIDTH = "MiniReportSizeWidth";
    public static final String MINIMAP_COLOURS = "MinimapColours";
    public static final String MINIMAP_ENABLED = "MinimapEnabled";
    public static final String MINIMAP_POS_X = "MinimapPosX";
    public static final String MINIMAP_POS_Y = "MinimapPosY";
    public static final String MINIMAP_ZOOM = "MinimapZoom";
    public static final String MINIMUM_SIZE_HEIGHT = "MinimumSizeHeight";
    public static final String MINIMUM_SIZE_WIDTH = "MinimumSizeWidth";
    public static final String MOUSE_WHEEL_ZOOM = "MouseWheelZoom";
    public static final String MOUSE_WHEEL_ZOOM_FLIP = "MouseWheelZoomFlip";
    public static final String NAG_FOR_BOT_README = "NagForBotReadme";
    public static final String NAG_FOR_CRUSHING_BUILDINGS = "NagForCrushingBuildings";
    public static final String NAG_FOR_MAP_ED_README = "NagForMapEdReadme";
    public static final String NAG_FOR_MASC = "NagForMASC";
    public static final String NAG_FOR_NO_ACTION = "NagForNoAction";
    public static final String NAG_FOR_PSR = "NagForPSR";
    public static final String NAG_FOR_README = "NagForReadme";
    public static final String NAG_FOR_SPRINT = "NagForSprint";
    public static final String NAG_FOR_OVERHEAT = "NagForOverHeat";
    public static final String NAG_FOR_LAUNCH_DOORS = "NagForLaunchDoors";
    public static final String NAG_FOR_MECHANICAL_FALL_DAMAGE = "NagForMechanicalFallDamage";
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
    public static final String SHOW_MOVE_STEP = "ShowMoveStep";
    public static final String SHOW_WRECKS = "ShowWrecks";
    public static final String SOUND_BING_FILENAME = "SoundBingFilename";
    public static final String SOUND_MUTE = "SoundMute";
    public static final String TOOLTIP_DELAY = "TooltipDelay";
    public static final String TOOLTIP_DISMISS_DELAY = "TooltipDismissDelay";
    public static final String WINDOW_POS_X = "WindowPosX";
    public static final String WINDOW_POS_Y = "WindowPosY";
    public static final String WINDOW_SIZE_HEIGHT = "WindowSizeHeight";
    public static final String WINDOW_SIZE_WIDTH = "WindowSizeWidth";
    public static final String LOS_MECH_IN_FIRST = "LOSMechInFirst";
    public static final String LOS_MECH_IN_SECOND = "LOSMechInSecond";
    public static final String SHOW_MAPSHEETS = "ShowMapsheets";
    public static final String USE_ISOMETRIC = "UseIsometric";
    public static final String SHOW_UNIT_OVERVIEW = "ShowUnitOverview";
    public static final String SHOW_DAMAGE_LEVEL = "ShowDamageLevel";
    public static final String SKIN_FILE = "SkinFile";
    public static final String DEFAULT_WEAP_SORT_ORDER = "DefaultWeaponSortOrder";
    
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
    

    protected static GUIPreferences instance = new GUIPreferences();

    public static GUIPreferences getInstance() {
        return instance;
    }

    protected GUIPreferences() {

        store = PreferenceManager.getInstance().getPreferenceStore(
                getClass().getName());

        store.setDefault(ADVANCED_CHATBOX_SIZE, 5);
        store.setDefault(ADVANCED_CHAT_LOUNGE_TAB_FONT_SIZE, 16);
        store.setDefault(ADVANCED_MECH_DISPLAY_ARMOR_LARGE_FONT_SIZE, 12);
        store.setDefault(ADVANCED_MECH_DISPLAY_ARMOR_MEDIUM_FONT_SIZE, 10);
        store.setDefault(ADVANCED_MECH_DISPLAY_ARMOR_SMALL_FONT_SIZE, 9);
        store.setDefault(ADVANCED_MECH_DISPLAY_LARGE_FONT_SIZE, 12);
        store.setDefault(ADVANCED_MECH_DISPLAY_MEDIUM_FONT_SIZE, 10);
        store.setDefault(ADVANCED_MECH_DISPLAY_WRAP_LENGTH, 24);
        setDefault(ADVANCED_MOVE_DEFAULT_COLOR, "cyan");
        setDefault(ADVANCED_MOVE_ILLEGAL_COLOR, "darkGray");
        setDefault(ADVANCED_MOVE_JUMP_COLOR, "red");
        setDefault(ADVANCED_MOVE_MASC_COLOR, new Color(255, 140, 0));
        setDefault(ADVANCED_MOVE_RUN_COLOR, "yellow");
        setDefault(ADVANCED_MOVE_BACK_COLOR, new Color(255, 255, 0));
        setDefault(ADVANCED_MOVE_SPRINT_COLOR, new Color(255, 20, 147));
        setDefault(ADVANCED_UNITOVERVIEW_SELECTED_COLOR, new Color(255,0,255));
        setDefault(ADVANCED_UNITOVERVIEW_VALID_COLOR, "cyan");
        setDefault(ADVANCED_FIRE_SOLN_CANSEE_COLOR, "cyan");
        setDefault(ADVANCED_FIRE_SOLN_NOSEE_COLOR, "red");


        setDefault(ADVANCED_MOVE_FONT_TYPE,"SansSerif");
        setDefault(ADVANCED_MOVE_FONT_SIZE,26);
        setDefault(ADVANCED_MOVE_FONT_STYLE,java.awt.Font.BOLD);


        store.setDefault(ADVANCED_MOVE_STEP_DELAY, 50);
        store.setDefault(ADVANCED_DARKEN_MAP_AT_NIGHT, true);
        setDefault(ADVANCED_MAPSHEET_COLOR, "blue");
        store.setDefault(ADVANCED_TRANSLUCENT_HIDDEN_UNITS, true);
        store.setDefault(ADVANCED_ATTACK_ARROW_TRANSPARENCY, 0x80);
        setDefault(ADVANCED_BUILDING_TEXT_COLOR, "blue");
        setDefault(ADVANCED_CHATBOX2_BACKCOLOR, new Color(255, 255, 255));
        store.setDefault(ADVANCED_CHATBOX2_FONTSIZE, 12);
        store.setDefault(ADVANCED_CHATBOX2_TRANSPARANCY, 50);
        store.setDefault(ADVANCED_CHATBOX2_AUTOSLIDEDOWN, true);
        store.setDefault(ADVANCED_ECM_TRANSPARENCY, 0x80);        
        store.setDefault(ADVANCED_KEY_REPEAT_DELAY, 0);
        store.setDefault(ADVANCED_KEY_REPEAT_RATE, 20);
        store.setDefault(ADVANCED_SHOW_FPS, "false");
        store.setDefault(ADVANCED_SHOW_COORDS, "true");
        store.setDefault(ADVANCED_BUTTONS_PER_ROW, 5);

        store.setDefault(FOV_HIGHLIGHT_RINGS_RADII, "5 10 15 20 25");
        store.setDefault(FOV_HIGHLIGHT_RINGS_COLORS_HSB, "0.3 1.0 1.0 ; 0.45 1.0 1.0 ; 0.6 1.0 1.0 ; 0.75 1.0 1.0 ; 0.9 1.0 1.0 ; 1.05 1.0 1.0 ");


        store.setDefault(ANTIALIASING, true);
        store.setDefault(AOHEXSHADOWS, false);
        store.setDefault(SHADOWMAP, false);
        store.setDefault(FLOATINGISO, false);
        store.setDefault(LEVELHIGHLIGHT, false);
        store.setDefault(AUTO_END_FIRING, true);
        store.setDefault(AUTO_DECLARE_SEARCHLIGHT, true);
        store.setDefault(CHAT_LOUNGE_TABS, true);
        store.setDefault(DISPLAY_SIZE_HEIGHT, 500);
        store.setDefault(DISPLAY_SIZE_WIDTH, 300);
        store.setDefault(GAME_OPTIONS_SIZE_HEIGHT,400);
        store.setDefault(GAME_OPTIONS_SIZE_WIDTH,400);
        store.setDefault(FIRING_SOLUTIONS,true);
        store.setDefault(FOV_HIGHLIGHT,false);
        store.setDefault(FOV_HIGHLIGHT_ALPHA, 40);
        store.setDefault(FOV_DARKEN,true);
        store.setDefault(FOV_DARKEN_ALPHA, 100);
        store.setDefault(FOV_STRIPES, 35);
        store.setDefault(FOV_GRAYSCALE, "false");
        setDefault(MAP_TEXT_COLOR, Color.black);
        store.setDefault(MAP_ZOOM_INDEX, 7);
        store.setDefault(MECH_SELECTOR_INCLUDE_MODEL, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_NAME, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_TONS, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_BV, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_YEAR, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_LEVEL, true);
        store.setDefault(MECH_SELECTOR_INCLUDE_COST, true);
        store.setDefault(MECH_SELECTOR_UNIT_TYPE,0);
        store.setDefault(MECH_SELECTOR_WEIGHT_CLASS,15);
        store.setDefault(MECH_SELECTOR_RULES_LEVELS,"[0]");
        store.setDefault(MECH_SELECTOR_SHOW_ADVANCED, false);
        store.setDefault(MECH_SELECTOR_SIZE_HEIGHT,600);
        store.setDefault(MECH_SELECTOR_SIZE_WIDTH,800);
        store.setDefault(MINIMAP_COLOURS, "defaultminimap.txt");
        store.setDefault(MINIMAP_ENABLED, true);
        store.setDefault(MINIMUM_SIZE_HEIGHT, 200);
        store.setDefault(MINIMUM_SIZE_WIDTH, 120);
        store.setDefault(MINI_REPORT_POS_X, 200);
        store.setDefault(MINI_REPORT_POS_Y, 150);
        store.setDefault(MINI_REPORT_SIZE_HEIGHT, 300);
        store.setDefault(MINI_REPORT_SIZE_WIDTH, 400);
        store.setDefault(MOUSE_WHEEL_ZOOM, false);
        store.setDefault(MOUSE_WHEEL_ZOOM_FLIP, false);
        store.setDefault(NAG_FOR_BOT_README, true);
        store.setDefault(NAG_FOR_CRUSHING_BUILDINGS, true);
        store.setDefault(NAG_FOR_MAP_ED_README, true);
        store.setDefault(NAG_FOR_MASC, true);
        store.setDefault(NAG_FOR_NO_ACTION, true);
        store.setDefault(NAG_FOR_PSR, true);
        store.setDefault(NAG_FOR_README, true);
        store.setDefault(NAG_FOR_SPRINT, true);
        store.setDefault(NAG_FOR_OVERHEAT, true);
        store.setDefault(NAG_FOR_LAUNCH_DOORS, true);
        store.setDefault(NAG_FOR_MECHANICAL_FALL_DAMAGE,true);
        setDefault(RULER_COLOR_1, Color.cyan);
        setDefault(RULER_COLOR_2, Color.magenta);
        store.setDefault(RULER_POS_X, 0);
        store.setDefault(RULER_POS_Y, 0);
        store.setDefault(RULER_SIZE_HEIGHT, 300);
        store.setDefault(RULER_SIZE_WIDTH, 500);
        store.setDefault(SCROLL_SENSITIVITY, 3);
        store.setDefault(SHOW_FIELD_OF_FIRE, true);
        store.setDefault(SHOW_MAPHEX_POPUP, true);
        store.setDefault(SHOW_MOVE_STEP, true);
        store.setDefault(SHOW_WRECKS, true);
        store.setDefault(SOUND_BING_FILENAME, "data/sounds/call.wav");
        store.setDefault(TOOLTIP_DELAY, 1000);
        store.setDefault(TOOLTIP_DISMISS_DELAY, -1);
        store.setDefault(WINDOW_SIZE_HEIGHT, 600);
        store.setDefault(WINDOW_SIZE_WIDTH, 800);
        store.setDefault(SHOW_MAPSHEETS, false);
        store.setDefault(SHOW_WPS_IN_TT, false);
        store.setDefault(USE_ISOMETRIC, false);
        store.setDefault(SHOW_UNIT_OVERVIEW, true);
        store.setDefault(SHOW_DAMAGE_LEVEL, false);
        store.setDefault(SKIN_FILE, "defaultSkin.xml");
        store.setDefault(SOFTCENTER, false);
        
        store.setDefault(RAT_TECH_LEVEL, 0);
        store.setDefault(RAT_BV_MIN, "5800");
        store.setDefault(RAT_BV_MAX, "6000");
        store.setDefault(RAT_NUM_MECHS, "4");
        store.setDefault(RAT_NUM_VEES, "0");
        store.setDefault(RAT_NUM_BA, "0");
        store.setDefault(RAT_NUM_INF, "0");
        store.setDefault(RAT_YEAR_MIN, "2500");
        store.setDefault(RAT_YEAR_MAX, "3100");
        store.setDefault(RAT_PAD_BV, false);
        store.setDefault(RAT_SELECTED_RAT, "");
        
        store.setDefault(DEFAULT_WEAP_SORT_ORDER,
                Entity.WeaponSortOrder.DEFAULT.ordinal());
        
    }

    public void setDefault(String name, Color color) {
        store.setDefault(name, getColorString(color));
    }

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

    public boolean getLevelHighlight() {
        return store.getBoolean(LEVELHIGHLIGHT);
    }

    public boolean getAutoEndFiring() {
        return store.getBoolean(AUTO_END_FIRING);
    }

    public boolean getAutoDeclareSearchlight() {
        return store.getBoolean(AUTO_DECLARE_SEARCHLIGHT);
    }

    public boolean getChatLoungeTabs() {
        return store.getBoolean(CHAT_LOUNGE_TABS);
    }

    public int getDisplayPosX() {
        return store.getInt(DISPLAY_POS_X);
    }

    public int getDisplayPosY() {
        return store.getInt(DISPLAY_POS_Y);
    }

    public int getDisplaySizeHeight() {
        return store.getInt(DISPLAY_SIZE_HEIGHT);
    }

    public int getDisplaySizeWidth() {
        return store.getInt(DISPLAY_SIZE_WIDTH);
    }

    public boolean getFocus() {
        return store.getBoolean(FOCUS);
    }

    public int getGameOptionsSizeHeight(){
        return store.getInt(GAME_OPTIONS_SIZE_HEIGHT);
    }

    public int getGameOptionsSizeWidth(){
        return store.getInt(GAME_OPTIONS_SIZE_WIDTH);
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
        return store.getString( FOV_HIGHLIGHT_RINGS_RADII );
    }

    public String getFovHighlightRingsColorsHsb() {
        return store.getString( FOV_HIGHLIGHT_RINGS_COLORS_HSB );
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
        return getColor(MAP_TEXT_COLOR);
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

    public int getMechSelectorUnitType(){
        return store.getInt(MECH_SELECTOR_UNIT_TYPE);
    }

    public int getMechSelectorWeightClass(){
        return store.getInt(MECH_SELECTOR_WEIGHT_CLASS);
    }

    public String getMechSelectorRulesLevels(){
        return store.getString(MECH_SELECTOR_RULES_LEVELS);
    }


    public int getMechSelectorSizeHeight() {
        return store.getInt(MECH_SELECTOR_SIZE_HEIGHT);
    }

    public int getMechSelectorSizeWidth() {
        return store.getInt(MECH_SELECTOR_SIZE_WIDTH);
    }

    public String getMinimapColours() {
        return store.getString(MINIMAP_COLOURS);
    }

    public boolean getMinimapEnabled() {
        return store.getBoolean(MINIMAP_ENABLED);
    }

    public boolean getIsometricEnabled() {
        return store.getBoolean(USE_ISOMETRIC);
    }

    public int getMinimapPosX() {
        return store.getInt(MINIMAP_POS_X);
    }

    public int getMinimapPosY() {
        return store.getInt(MINIMAP_POS_Y);
    }

    public int getMinimapZoom() {
        return store.getInt(MINIMAP_ZOOM);
    }

    public int getMinimumSizeHeight() {
        return store.getInt(MINIMUM_SIZE_HEIGHT);
    }

    public int getMinimumSizeWidth() {
        return store.getInt(MINIMUM_SIZE_WIDTH);
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

    public boolean getMouseWheelZoomFlip(){
        return store.getBoolean(MOUSE_WHEEL_ZOOM_FLIP);
    }

    public boolean getNagForBotReadme() {
        return store.getBoolean(NAG_FOR_BOT_README);
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

    public boolean getShowMoveStep() {
        return store.getBoolean(SHOW_MOVE_STEP);
    }

    public boolean getShowWrecks() {
        return store.getBoolean(SHOW_WRECKS);
    }

    public String getSoundBingFilename() {
        return store.getString(SOUND_BING_FILENAME);
    }

    public boolean getSoundMute() {
        return store.getBoolean(SOUND_MUTE);
    }

    public int getTooltipDelay() {
        return store.getInt(TOOLTIP_DELAY);
    }

    public int getTooltipDismissDelay() {
        return store.getInt(TOOLTIP_DISMISS_DELAY);
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

    public boolean getShowUnitOverview(){
        return store.getBoolean(SHOW_UNIT_OVERVIEW);
    }
    
    public String getSkinFile() {
        return store.getString(SKIN_FILE);
    }
    
    public int getDefaultWeaponSortOrder() {
        return store.getInt(DEFAULT_WEAP_SORT_ORDER);
    }

    public void setAntiAliasing(boolean state){
        store.setValue(ANTIALIASING, state);
    }
    
    public void setShadowMap(boolean state){
        store.setValue(SHADOWMAP, state);
    }
    
    public void setAOHexShadows(boolean state){
        store.setValue(AOHEXSHADOWS, state);
    }
    
    public void setFloatingIso(boolean state){
        store.setValue(FLOATINGISO, state);
    }

    public void setMmSymbol(boolean state){
        store.setValue(MMSYMBOL, state);
    }

    public void setLevelHighlight(boolean state){
        store.setValue(LEVELHIGHLIGHT, state);
    }

    public boolean getShowDamageLevel(){
        return store.getBoolean(SHOW_DAMAGE_LEVEL);
    }

    public void setAutoEndFiring(boolean state) {
        store.setValue(AUTO_END_FIRING, state);
    }

    public void setAutoDeclareSearchlight(boolean state) {
        store.setValue(AUTO_DECLARE_SEARCHLIGHT, state);
    }

    public void setChatloungeTabs(boolean state) {
        store.setValue(CHAT_LOUNGE_TABS, state);
    }

    public void setDisplayPosX(int i) {
        store.setValue(DISPLAY_POS_X, i);
    }

    public void setDisplayPosY(int i) {
        store.setValue(DISPLAY_POS_Y, i);
    }

    public void setDisplaySizeHeight(int i) {
        store.setValue(DISPLAY_SIZE_HEIGHT, i);
    }

    public void setDisplaySizeWidth(int i) {
        store.setValue(DISPLAY_SIZE_WIDTH, i);
    }

    public void setGetFocus(boolean state) {
        store.setValue(FOCUS, state);
    }

    public void setGameOptionsSizeHeight(int i){
        store.setValue(GAME_OPTIONS_SIZE_HEIGHT,i);
    }

    public void setGameOptionsSizeWidth(int i){
        store.setValue(GAME_OPTIONS_SIZE_WIDTH,i);
    }

    public void setFiringSolutions(boolean state){
        store.setValue(FIRING_SOLUTIONS,state);
    }
    
    public void setMoveEnvelope(boolean state){
        store.setValue(MOVE_ENVELOPE,state);
    }   

    public void setFovHighlight(boolean state){
        store.setValue(FOV_HIGHLIGHT,state);
    }

    public void setFovHighlightAlpha(int i) {
        store.setValue(FOV_HIGHLIGHT_ALPHA,i);
    }

    public void setFovHighlightRingsRadii( String s ) {
        store.setValue(FOV_HIGHLIGHT_RINGS_RADII, s);
    }

    public void setFovHighlightRingsColorsHsb( String s ) {
        store.setValue(FOV_HIGHLIGHT_RINGS_COLORS_HSB, s);
    }

    public void setFovDarken(boolean state){
        store.setValue(FOV_DARKEN,state);
    }

    public void setFovDarkenAlpha(int i) {
        store.setValue(FOV_DARKEN_ALPHA,i);
    }
    
    public void setFovStripes(int i) {
        store.setValue(FOV_STRIPES,i);
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

    public void setMechSelectorUnitType(int unitType){
        store.setValue(MECH_SELECTOR_UNIT_TYPE,unitType);
    }

    public void setMechSelectorWeightClass(int weightClass){
        store.setValue(MECH_SELECTOR_WEIGHT_CLASS,weightClass);
    }

    public void setMechSelectorRulesLevels(String rulesLevels){
        store.setValue(MECH_SELECTOR_RULES_LEVELS,rulesLevels);
    }


    public void setMechSelectorSizeHeight(int i) {
        store.setValue(MECH_SELECTOR_SIZE_HEIGHT, i);
    }

    public void setMechSelectorSizeWidth(int i) {
        store.setValue(MECH_SELECTOR_SIZE_WIDTH, i);
    }

    public void setMinimapEnabled(boolean b) {
        store.setValue(MINIMAP_ENABLED, b);
    }

    public void setMinimapPosX(int i) {
        store.setValue(MINIMAP_POS_X, i);
    }

    public void setMinimapPosY(int i) {
        store.setValue(MINIMAP_POS_Y, i);
    }

    public void setMinimapZoom(int zoom) {
        store.setValue(MINIMAP_ZOOM, zoom);
    }

    public void setMiniReportPosX(int i) {
        store.setValue(MINI_REPORT_POS_X, i);
    }

    public void setMiniReportPosY(int i) {
        store.setValue(MINIMAP_POS_Y, i);
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

    public void setMouseWheelZoomFlip(boolean b){
        store.setValue(MOUSE_WHEEL_ZOOM_FLIP,b);
    }

    public void setNagForBotReadme(boolean b) {
        store.setValue(NAG_FOR_BOT_README, b);
    }

    public void setNagForCrushingBuildings(boolean b){
        store.setValue(NAG_FOR_CRUSHING_BUILDINGS,b);
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

    public void setNagForMechanicalJumpFallDamage(boolean b){
        store.setValue(NAG_FOR_MECHANICAL_FALL_DAMAGE,b);
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

    public void setShowMoveStep(boolean state) {
        store.setValue(SHOW_MOVE_STEP, state);
    }

    public void setShowWrecks(boolean state) {
        store.setValue(SHOW_WRECKS, state);
    }

    public void setSoundBingFilename(String name) {
        store.setValue(SOUND_BING_FILENAME, name);
    }

    public void setSoundMute(boolean state) {
        store.setValue(SOUND_MUTE, state);
    }

    public void setTooltipDelay(int i) {
        store.setValue(TOOLTIP_DELAY, i);
        ToolTipManager.sharedInstance().setInitialDelay(i);
    }

    public void setTooltipDismissDelay(int i) {
        store.setValue(TOOLTIP_DISMISS_DELAY, i);
        if (i > 0){
            ToolTipManager.sharedInstance().setDismissDelay(i);
        }
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

    public void setShowUnitOverview(boolean b){
        store.setValue(SHOW_UNIT_OVERVIEW, b);
    }

    public void setShowDamageLevel(boolean b){
        store.setValue(SHOW_DAMAGE_LEVEL, b);
    }
    
    public void setSkinFile(String s) {
        store.setValue(SKIN_FILE, s);
    }
    
    public void setDefaultWeaponSortOrder(int i) {
        store.setValue(DEFAULT_WEAP_SORT_ORDER, i);
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

    protected ColorParser p = new ColorParser();

    protected String getColorString(Color color) {
        StringBuffer b = new StringBuffer();
        b.append(color.getRed()).append(" ");
        b.append(color.getGreen()).append(" ");
        b.append(color.getBlue());
        return b.toString();
    }

    public Color getColor(String name) {
        String sresult = store.getString(name);
        if (sresult != null) {
            if (!p.parse(sresult)) {
                return p.getColor();
            }
        }
        return Color.black;
    }
    
    /**
     * Activates AntiAliasing for the <code>Graphics</code> graph
     * if AA is activated in the Client settings. 
     * @param graph Graphics context to activate AA for
     */
    public static void AntiAliasifSet(Graphics graph) {
        if (getInstance().getAntiAliasing()) {
            ((Graphics2D)graph).setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

}
