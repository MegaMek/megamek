package megamek.client;

import java.awt.Color;

import megamek.client.util.ColorParser;
import megamek.common.preference.PreferenceStoreProxy;
import megamek.common.preference.PreferenceManager;

public class GUIPreferences extends PreferenceStoreProxy {

    public static final String ALWAYS_RIGHT_CLICK_SCROLL="AlwaysRightClickScroll";
    public static final String AUTO_EDGE_SCROLL = "AutoEdgeScroll";
    public static final String AUTO_END_FIRING = "AutoEndFiring";
    public static final String CHAT_LOUNGE_TAB_FONT_SIZE="ChatLoungeTabFontSize";
    public static final String CHAT_LOUNGE_TABS = "ChatLoungeTabs";
    public static final String CLICK_EDGE_SCROLL = "ClickEdgeScroll";
    public static final String CTL_SCROLL = "CtlScroll";
    public static final String DISPLAY_POS_X = "DisplayPosX";
    public static final String DISPLAY_POS_Y = "DisplayPosY";
    public static final String DISPLAY_SIZE_HEIGHT = "DisplaySizeHeight";
    public static final String DISPLAY_SIZE_WIDTH = "DisplaySizeWidth";
    public static final String FOCUS = "Focus";    
    public static final String MAP_TEXT_COLOR = "MapTextColor";
    public static final String MAP_ZOOM_INDEX = "MapZoomIndex";
    public static final String MECH_DISPLAY_ARMOR_LARGE_FONT_SIZE="MechDisplayArmorLargeFontSize";
    public static final String MECH_DISPLAY_ARMOR_MEDIUM_FONT_SIZE="MechDisplayArmorMediumFontSize";
    public static final String MECH_DISPLAY_ARMOR_SMALL_FONT_SIZE="MechDisplayArmorSmallFontSize";
    public static final String MECH_DISPLAY_LARGE_FONT_SIZE="MechDisplayLargeFontSize";
    public static final String MECH_DISPLAY_MEDIUM_FONT_SIZE="MechDisplayMediumFontSize";
    public static final String MINI_REPORT_POS_X = "MiniReportPosX";
    public static final String MINI_REPORT_POS_Y = "MiniReportPosY";
    public static final String MINI_REPORT_SIZE_HEIGHT="MiniReportSizeHeight";
    public static final String MINI_REPORT_SIZE_WIDTH="MiniReportSizeWidth";
    public static final String MINIMAP_COLOURS = "MinimapColours";
    public static final String MINIMAP_ENABLED = "MinimapEnabled";
    public static final String MINIMAP_POS_X = "MinimapPosX";
    public static final String MINIMAP_POS_Y = "MinimapPosY";
    public static final String MINIMAP_ZOOM = "MinimapZoom";
    public static final String MINIMUM_SIZE_HEIGHT = "MinimumSizeHeight";
    public static final String MINIMUM_SIZE_WIDTH = "MinimumSizeWidth";
    public static final String MOVE_DEFAULT_COLOR = "MoveDefaultColor";
    public static final String MOVE_ILLEGAL_COLOR = "MoveIllegalColor";
    public static final String MOVE_JUMP_COLOR = "MoveJumpColor";
    public static final String MOVE_MASC_COLOR = "MoveMASCColor";
    public static final String MOVE_RUN_COLOR = "MoveRunColor";
    public static final String MOVE_STEP_DELAY = "MoveStepDelay";
    public static final String NAG_FOR_BOT_README = "NagForBotReadme";
    public static final String NAG_FOR_MAP_ED_README="NagForMapEdReadme";
    public static final String NAG_FOR_MASC = "NagForMASC";
    public static final String NAG_FOR_NO_ACTION = "NagForNoAction";
    public static final String NAG_FOR_PSR = "NagForPSR";
    public static final String NAG_FOR_README = "NagForReadme";
    public static final String RIGHT_DRAG_SCROLL = "RightDragScroll";
    public static final String RULER_COLOR_1 = "RulerColor1";
    public static final String RULER_COLOR_2 = "RulerColor2";
    public static final String RULER_POS_X = "RulerPosX";
    public static final String RULER_POS_Y = "RulerPosY";
    public static final String RULER_SIZE_HEIGHT = "RulerSizeHeight";
    public static final String RULER_SIZE_WIDTH = "RulerSizeWidth";
    public static final String SCROLL_SENSITIVITY = "ScrollSensitivity";
    public static final String SHOW_MAPHEX_POPUP = "ShowMapHexPopup";
    public static final String SHOW_MOVE_STEP = "ShowMoveStep";
    public static final String SHOW_WRECKS = "ShowWrecks";
    public static final String SOUND_BING_FILENAME = "SoundBingFilename";
    public static final String SOUND_MUTE = "SoundMute";
    public static final String TOOLTIP_DELAY = "TooltipDelay";
    public static final String WINDOW_POS_X = "WindowPosX";
    public static final String WINDOW_POS_Y = "WindowPosY";
    public static final String WINDOW_SIZE_HEIGHT = "WindowSizeHeight";
    public static final String WINDOW_SIZE_WIDTH = "WindowSizeWidth";

    protected static GUIPreferences instance = new GUIPreferences();
    
    public static GUIPreferences getInstance() {
        return instance;
    }

    protected GUIPreferences() {

        store = PreferenceManager.getInstance().getPreferenceStore(getClass().getName());

        store.setDefault(AUTO_END_FIRING, true);
        store.setDefault(CHAT_LOUNGE_TAB_FONT_SIZE, 16);
        store.setDefault(CHAT_LOUNGE_TABS, true);
        store.setDefault(DISPLAY_SIZE_HEIGHT, 370);
        store.setDefault(DISPLAY_SIZE_WIDTH, 235);
        setDefault(MAP_TEXT_COLOR, Color.black);
        store.setDefault(MAP_ZOOM_INDEX, 7);
        store.setDefault(MECH_DISPLAY_ARMOR_LARGE_FONT_SIZE, 12);
        store.setDefault(MECH_DISPLAY_ARMOR_MEDIUM_FONT_SIZE, 10);
        store.setDefault(MECH_DISPLAY_ARMOR_SMALL_FONT_SIZE, 9);
        store.setDefault(MECH_DISPLAY_LARGE_FONT_SIZE, 12);
        store.setDefault(MECH_DISPLAY_MEDIUM_FONT_SIZE, 10);
        store.setDefault(MINIMAP_COLOURS,"defaultminimap.txt");
        store.setDefault(MINIMAP_ENABLED, true);
        store.setDefault(MINIMUM_SIZE_HEIGHT, 200);
        store.setDefault(MINIMUM_SIZE_WIDTH, 120);
        store.setDefault(MINI_REPORT_POS_X, 200);
        store.setDefault(MINI_REPORT_POS_Y, 150);
        store.setDefault(MINI_REPORT_SIZE_HEIGHT, 300);
        store.setDefault(MINI_REPORT_SIZE_WIDTH, 400);
        setDefault(MOVE_DEFAULT_COLOR, Color.cyan);
        setDefault(MOVE_ILLEGAL_COLOR, Color.darkGray);
        setDefault(MOVE_JUMP_COLOR, Color.red);
        setDefault(MOVE_MASC_COLOR, new Color(255,140,0));
        setDefault(MOVE_RUN_COLOR, Color.yellow);
        setDefault(MOVE_STEP_DELAY, 500);
        store.setDefault(NAG_FOR_BOT_README, true);
        store.setDefault(NAG_FOR_MAP_ED_README, true);
        store.setDefault(NAG_FOR_MASC, true);
        store.setDefault(NAG_FOR_NO_ACTION, true);
        store.setDefault(NAG_FOR_PSR, true);
        store.setDefault(NAG_FOR_README, true);
        store.setDefault(RIGHT_DRAG_SCROLL, true);
        setDefault(RULER_COLOR_1, Color.cyan);
        setDefault(RULER_COLOR_2, Color.magenta);
        store.setDefault(RULER_POS_X, 0);
        store.setDefault(RULER_POS_Y, 0);
        store.setDefault(RULER_SIZE_HEIGHT, 240);
        store.setDefault(RULER_SIZE_WIDTH, 350);
        store.setDefault(SCROLL_SENSITIVITY, 3);
        store.setDefault(SHOW_MAPHEX_POPUP, true);
        store.setDefault(SHOW_MOVE_STEP, true);
        store.setDefault(SHOW_WRECKS, true);
        store.setDefault(SOUND_BING_FILENAME,"data/sounds/call.wav");
        store.setDefault(TOOLTIP_DELAY, 1000);
        store.setDefault(WINDOW_SIZE_HEIGHT, 600);
        store.setDefault(WINDOW_SIZE_WIDTH, 800);
        
    }

    public void setDefault(String name, Color color) {
        store.setDefault(name, getColorString(color));
    }

    public boolean getAlwaysRightClickScroll() {
        return store.getBoolean(ALWAYS_RIGHT_CLICK_SCROLL);
    }

    public boolean getAutoEdgeScroll() {
        return store.getBoolean(AUTO_EDGE_SCROLL);
    }

    public boolean getAutoEndFiring() {
        return store.getBoolean(AUTO_END_FIRING);
    }

    public int getChatLoungeTabFontSize() {
        return store.getInt(CHAT_LOUNGE_TAB_FONT_SIZE);
    }

    public boolean getChatLoungeTabs() {
        return store.getBoolean(CHAT_LOUNGE_TABS);
    }

    public boolean getClickEdgeScroll() {
        return store.getBoolean(CLICK_EDGE_SCROLL);
    }

    public boolean getCtlScroll() {
        return store.getBoolean(CTL_SCROLL);
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

    public Color getMapTextColor() {
        return getColor(MAP_TEXT_COLOR);
    }

    public int getMapZoomIndex() {
        return store.getInt(MAP_ZOOM_INDEX);
    }

    public int getMechDisplayArmorLargeFontSize() {
        return store.getInt(MECH_DISPLAY_ARMOR_LARGE_FONT_SIZE);
    }

    public int getMechDisplayArmorMediumFontSize() {
        return store.getInt(MECH_DISPLAY_ARMOR_MEDIUM_FONT_SIZE);
    }

    public int getMechDisplayArmorSmallFontSize() {
        return store.getInt(MECH_DISPLAY_ARMOR_SMALL_FONT_SIZE);
    }

    public int getMechDisplayLargeFontSize() {
        return store.getInt(MECH_DISPLAY_LARGE_FONT_SIZE);
    }

    public int getMechDisplayMediumFontSize() {
        return store.getInt(MECH_DISPLAY_MEDIUM_FONT_SIZE);
    }

    public String getMinimapColours() {
        return store.getString(MINIMAP_COLOURS);
    }

    public boolean getMinimapEnabled() {
        return store.getBoolean(MINIMAP_ENABLED);
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

    public Color getMoveDefaultColor() {
        return getColor(MOVE_DEFAULT_COLOR);
    }

    public Color getMoveIllegalColor() {
        return getColor(MOVE_ILLEGAL_COLOR);
    }

    public Color getMoveJumpColor() {
        return getColor(MOVE_JUMP_COLOR);
    }

    public Color getMoveMASCColor() {
        return getColor(MOVE_MASC_COLOR);
    }

    public Color getMoveRunColor() {
        return getColor(MOVE_RUN_COLOR);
    }

    public int getMoveStepDelay() {
        return store.getInt(MOVE_STEP_DELAY);
    }

    public boolean getNagForBotReadme() {
        return store.getBoolean(NAG_FOR_BOT_README);
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

    public boolean getRightDragScroll() {
        return store.getBoolean(RIGHT_DRAG_SCROLL);
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

    public boolean getShowMapHexPopup() {
        return store.getBoolean(SHOW_MAPHEX_POPUP);
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

    public void setAlwaysRightClickScroll(boolean state) {
        store.setValue(ALWAYS_RIGHT_CLICK_SCROLL, state);
    }

    public void setAutoEdgeScroll(boolean state) {
        store.setValue(AUTO_EDGE_SCROLL, state);
    }

    public void setAutoEndFiring(boolean state) {
        store.setValue(AUTO_END_FIRING, state);
    }

    public void setClickEdgeScroll(boolean state) {
        store.setValue(CLICK_EDGE_SCROLL, state);
    }

    public void setCtlScroll(boolean state) {
        store.setValue(CTL_SCROLL, state);
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

    public void setMapZoomIndex(int zoomIndex) {
        store.setValue(MAP_ZOOM_INDEX, zoomIndex);
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

    public void setNagForBotReadme(boolean b) {
        store.setValue(NAG_FOR_BOT_README, b);
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

    public void setRightDragScroll(boolean state) {
        store.setValue(RIGHT_DRAG_SCROLL, state);
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

    public void setShowMapHexPopup(boolean state) {
        store.setValue(SHOW_MAPHEX_POPUP, state);
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
    
    protected ColorParser p = new ColorParser();    

    protected String getColorString(Color color) {
        StringBuffer b = new StringBuffer();
        b.append(color.getRed()).append(" ");
        b.append(color.getGreen()).append(" ");
        b.append(color.getBlue());
        return b.toString();
    }

    protected Color getColor(String name) {
        String sresult = store.getString(name);
        if (sresult != null) {
            if (!p.parse(sresult)) {
                return p.getColor();
            }
        }
        return Color.black;
   
    }

}
