package megamek.client.ui.swing.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;

public final class UIUtil {
    /** 
     * Returns an HTML FONT tag setting the font face to Dialog 
     * and the font size according to GUIScale. 
     */
    public static String getFontHTML() {
        return "<FONT FACE=Dialog " + sizeString() + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the color to the given col,
     * the font face to Dialog and the font size according to GUIScale. 
     */
    public static String getFontHTML(Color col) {
        return "<FONT FACE=Dialog " + sizeString() + colorString(col) + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the font face to Dialog 
     * and the font size 1 step smaller than GUIScale. 
     */
    public static String getSmallFontHTML() {
        return "<FONT FACE=Dialog " + smallSizeString() + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the color to the given col,
     * the font face to Dialog and the font size 1 step smaller than GUIScale. 
     */
    public static String getSmallFontHTML(Color col) {
        return "<FONT FACE=Dialog " + smallSizeString() + colorString(col) + ">";
    }
    
    /** 
     * Helper method to place Strings in lines according to length. The Strings
     * in origList will be added to one line with separator sep between them as 
     * long as the total length does not exceed maxLength. If it exceeds maxLength, 
     * a new line is begun. All lines but the last will end with sep if sepAtEnd is true. 
     */
    public static ArrayList<String> arrangeInLines(List<String> origList, int maxLength, 
            String sep, boolean sepAtEnd) {
        ArrayList<String> result = new ArrayList<>();
        if (origList == null || origList.isEmpty()) {
            return result;
        }
        String currLine = "";
        for (String curr: origList) {
            if (currLine.isEmpty()) {
                // No entry in this line yet
                currLine = curr;
            } else if (currLine.length() + curr.length() + sep.length() <= maxLength) {
                // This line can hold another string
                currLine += sep + curr;
            } else {
                // This line cannot hold another string
                currLine += sepAtEnd ? sep : "";
                result.add(currLine);
                currLine = curr;
            }
        }
        if (!currLine.isEmpty()) {
            // Add the last unfinished line
            result.add(currLine);
        } else if (sepAtEnd) {
            // Remove the last unnecessary sep if there were no more Strings
            String lastLine = result.get(result.size() - 1);
            String newLine = lastLine.substring(0, lastLine.length() - sep.length());
            result.remove(result.size() - 1);
            result.add(newLine);
        }
        return result;
    }
    
    /** 
     * Returns a green color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiGreen() {
        return uiBgBrightness() > 130 ? LIGHTUI_GREEN : DARKUI_GREEN;
    }
    
    /** 
     * Returns a gray color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiGray() {
        return uiBgBrightness() > 130 ? LIGHTUI_GRAY : DARKUI_GRAY;
    }
    
    /** 
     * Returns a light blue color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightBlue() {
        return uiBgBrightness() > 130 ? LIGHTUI_LIGHTBLUE : DARKUI_LIGHTBLUE;
    }
    
    /** 
     * Returns a light red color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightRed() {
        return uiBgBrightness() > 130 ? LIGHTUI_LIGHTRED : DARKUI_LIGHTRED;
    }

    /** 
     * Returns a light red color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightViolet() {
        return uiBgBrightness() > 130 ? LIGHTUI_LIGHTVIOLET : DARKUI_LIGHTVIOLET;
    }
    
    // PRIVATE 
    
    /** The style = font-size: xx value corresponding to a GUI scale of 1 */
    private final static int FONT_SCALE1 = 14;
    
    private final static Color LIGHTUI_GREEN = new Color(20, 140, 20);
    private final static Color DARKUI_GREEN = new Color(40, 180, 40);
    private final static Color LIGHTUI_GRAY = new Color(100, 100, 100);
    private final static Color DARKUI_GRAY = new Color(150, 150, 150);
    private final static Color LIGHTUI_LIGHTBLUE = new Color(100, 100, 150);
    private final static Color DARKUI_LIGHTBLUE = new Color(150, 150, 210);
    private final static Color LIGHTUI_LIGHTRED = new Color(210, 100, 100);
    private final static Color DARKUI_LIGHTRED = new Color(210, 150, 150);
    private final static Color LIGHTUI_LIGHTVIOLET = new Color(180, 100, 220);
    private final static Color DARKUI_LIGHTVIOLET = new Color(180, 150, 220);
    
    
    /** Returns an HTML FONT Size String, e.g. SIZE=+2, according to GUIScale. */
    private static String sizeString() {
        int fontSize = (int)(GUIPreferences.getInstance().getGUIScale() * FONT_SCALE1);
        return " style=font-size:" + fontSize + " ";
    }
    
    /** Returns an HTML FONT Color String, e.g. COLOR=#FFFFFF according to the given color. */
    private static String colorString(Color col) {
        return " COLOR=" + Integer.toHexString(col.getRGB() & 0xFFFFFF) + " ";
    }
    
    /** Returns an HTML FONT Size String, e.g. SIZE=+2, 1 step smaller than GUIScale, */
    private static String smallSizeString() {
        float smallerScale = Math.max(ClientGUI.MIN_GUISCALE, GUIPreferences.getInstance().getGUIScale() - 0.2f);
        int fontSize = (int)(smallerScale * FONT_SCALE1);
        return " style=font-size:" + fontSize + " ";
    }
    
    private static int uiBgBrightness() {
        Color bgColor = UIManager.getColor("Table.background");
        if (bgColor == null) {
            // Try another 
            bgColor = UIManager.getColor("Menu.background");
        }
        if (bgColor == null) {
            return 250;
        } else {
            return colorBrightness(bgColor);
        }
    }
    
    private static int colorBrightness(final Color color) {
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
    }
}
