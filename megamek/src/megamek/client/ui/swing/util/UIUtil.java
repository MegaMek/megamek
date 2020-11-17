/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/ 
package megamek.client.ui.swing.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;

public final class UIUtil {
    
    /** The style = font-size: xx value corresponding to a GUI scale of 1 */
    public final static int FONT_SCALE1 = 14;
    
    /** 
     * Returns an HTML FONT tag setting the font face to Dialog 
     * and the font size according to GUIScale. 
     */
    public static String guiScaledFontHTML() {
        return "<FONT FACE=Dialog " + sizeString() + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the color to the given col,
     * the font face to Dialog and the font size according to GUIScale. 
     */
    public static String guiScaledFontHTML(Color col) {
        return "<FONT FACE=Dialog " + sizeString() + colorString(col) + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the font face to Dialog 
     * and the font size according to GUIScale. 
     */
    public static String guiScaledFontHTML(float deltaScale) {
        return "<FONT FACE=Dialog " + sizeString(deltaScale) + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the color to the given col,
     * the font face to Dialog and the font size according to GUIScale. 
     */
    public static String guiScaledFontHTML(Color col, float deltaScale) {
        return "<FONT FACE=Dialog " + sizeString(deltaScale) + colorString(col) + ">";
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
            // Skip empty strings to avoid double separators
            if (curr.isBlank()) {
                continue;
            }
            
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
    
    public static ArrayList<String> arrangeInLines(int maxLength, 
            String sep, boolean sepAtEnd, String... origList) {
        
        return arrangeInLines(Arrays.asList(origList), maxLength, sep, sepAtEnd);
    }
    
    public static String arrangeInLine(String sep, String... origList) {
        ArrayList<String> result = arrangeInLines(Arrays.asList(origList), Integer.MAX_VALUE, sep, false);
        if (!result.isEmpty()) {
            return result.get(0);
        } else {
            return "";
        }
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
     * Returns a light violet color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightViolet() {
        return uiBgBrightness() > 130 ? LIGHTUI_LIGHTVIOLET : DARKUI_LIGHTVIOLET;
    }
    
    /** 
     * Returns a light green color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightGreen() {
        return uiBgBrightness() > 130 ? LIGHTUI_LIGHTGREEN : DARKUI_LIGHTGREEN;
    }
    
    /** 
     * Returns a light red color suitable as a text color. The supplied
     * color depends on the UI look and feel and will be lighter for a 
     * dark UI LAF than for a light UI LAF.
     */
    public static Color uiYellow() {
        return uiBgBrightness() > 130 ? LIGHTUI_YELLOW : DARKUI_YELLOW;
    }
    
    /** 
     * Returns a color for the UI display of Quirks/Advantages. Different 
     * colors will be supplied for a dark and for a light UI look-and-feel.
     */
    public static Color uiQuirksColor() {
        return uiBgBrightness() > 130 ? LIGHTUI_LIGHTCYAN : DARKUI_LIGHTCYAN;
    }
    
    /** 
     * Returns a color for the UI display of Partial Repairs. Different 
     * colors will be supplied for a dark and for a light UI look-and-feel.
     */
    public static Color uiPartialRepairColor() {
        return uiLightRed();
    }
    
    /** 
     * Returns a color for the UI display of C3 Info. Different 
     * colors will be supplied for a dark and for a light UI look-and-feel.
     */
    public static Color uiC3Color() {
        return uiLightViolet();
    }
    
    /** 
     * Returns a color for the UI display of C3 Info. Different 
     * colors will be supplied for a dark and for a light UI look-and-feel.
     */
    public static Color uiNickColor() {
        return uiLightGreen();
    }
    
    /** 
     * Returns a color for the UI display of C3 Info. Different 
     * colors will be supplied for a dark and for a light UI look-and-feel.
     */
    public static Color uiTTWeaponColor() {
        return uiLightBlue();
    }
    
    public static int scaleForGUI(int value) {
        return Math.round(scaleForGUI((float)value));
    }
    
    public static float scaleForGUI(float value) {
        return GUIPreferences.getInstance().getGUIScale() * value;
    }
    
    public static void adjustDialog(Container contentPane) {
        Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        Component[] allComps = contentPane.getComponents();
        for (Component comp: allComps) {
            if ((comp instanceof JButton) || (comp instanceof JLabel)
                    || (comp instanceof JComboBox<?>) || (comp instanceof JCheckBox)
                    || (comp instanceof JTextField) || (comp instanceof JSlider)
                    || (comp instanceof JSpinner) || (comp instanceof JRadioButton)) {
                comp.setFont(scaledFont);
            }
            if (comp instanceof JScrollPane 
                    && ((JScrollPane)comp).getViewport().getView() instanceof JPanel) {
                adjustDialog((JPanel)((JScrollPane)comp).getViewport().getView());
            }
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel)comp;
                Border border = panel.getBorder();
                if ((border != null) && (border instanceof TitledBorder)) {
                    ((TitledBorder)border).setTitleFont(scaledFont);
                }
                adjustDialog((JPanel)comp);
            }
            if (comp instanceof JTabbedPane) {
                comp.setFont(scaledFont);
                JTabbedPane tpane = (JTabbedPane)comp;
                for (int i=0; i<tpane.getTabCount();i++) {
                    Component sc = tpane.getTabComponentAt(i);
                    if (sc instanceof JPanel) {
                        adjustDialog((JPanel)sc);
                    }
                }
                adjustDialog((JTabbedPane)comp);
            }
        }
    }

    /** Adapt a JPopupMenu to the GUI scaling. Use after all menu items have been added. */
    public static void scaleJPopup(final JPopupMenu popup) {
        Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        for (Component comp: popup.getComponents()) {
            if ((comp instanceof JMenuItem)) {
                comp.setFont(scaledFont);
                scaleJMenuItem((JMenuItem)comp);
            } 
        }
    }
    
    // PRIVATE 
    
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
    private final static Color LIGHTUI_YELLOW = new Color(250, 170, 40);
    private final static Color DARKUI_YELLOW = new Color(200, 200, 60);
    private final static Color LIGHTUI_LIGHTCYAN = new Color(40, 130, 130);
    private final static Color DARKUI_LIGHTCYAN = new Color(100, 180, 180);
    private final static Color LIGHTUI_LIGHTGREEN = new Color(80, 180, 80);
    private final static Color DARKUI_LIGHTGREEN = new Color(150, 210, 150);
    
    /** Returns an HTML FONT Size String, according to GUIScale (e.g. "style=font-size:22"). */
    private static String sizeString() {
        int fontSize = (int)(GUIPreferences.getInstance().getGUIScale() * FONT_SCALE1);
        return " style=font-size:" + fontSize + " ";
    }
    
    /** 
     * Returns an HTML FONT Size String, according to GUIScale and deltaScale 
     * (e.g. "style=font-size:22"). The given deltaScale is added to the 
     * GUIScale value, so a positive deltaScale value will increase the font size.
     * The adjusted GUIScale value will be kept within the limits of GUIScale. 
     * Suitable deltaScale values are usually between -0.4 and +0.4
     */
    private static String sizeString(float deltaScale) {
        float guiScale = GUIPreferences.getInstance().getGUIScale();
        float boundedScale = Math.max(ClientGUI.MIN_GUISCALE, guiScale + deltaScale);
        boundedScale = Math.min(ClientGUI.MAX_GUISCALE, boundedScale);
        int fontSize = (int)(boundedScale * FONT_SCALE1);
        return " style=font-size:" + fontSize + " ";
    }
    
    /** Returns an HTML FONT Color String, e.g. COLOR=#FFFFFF according to the given color. */
    private static String colorString(Color col) {
        return " COLOR=" + Integer.toHexString(col.getRGB() & 0xFFFFFF) + " ";
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
    
    /** Internal helper method to adapt items in a JPopupmenu to the GUI scaling. */
    private static void scaleJMenuItem(final JMenuItem menuItem) {
        Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        if (menuItem instanceof JMenu) {
            JMenu menu = (JMenu)menuItem;
            menu.setFont(scaledFont);
            for (int i = 0; i < menu.getItemCount(); i++) {
                scaleJMenuItem(menu.getItem(i));
            }
        } else if (menuItem instanceof JMenuItem) {
            menuItem.setFont(scaledFont);
        } 
    }
    

}
