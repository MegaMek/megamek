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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.IPlayer;

public final class UIUtil {
    
    /** The style = font-size: xx value corresponding to a GUI scale of 1 */
    public final static int FONT_SCALE1 = 14;
    public final static String ECM_SIGN = " \u24BA ";
    public final static String LOADED_SIGN = " \u26DF ";
    public final static String UNCONNECTED_SIGN = " \u26AC";
    public final static String CONNECTED_SIGN = " \u26AF ";
    public final static String WARNING_SIGN = " \u26A0 ";
    public final static String QUIRKS_SIGN = " \u24E0 ";
    public static final String DOT_SPACER = " \u2B1D ";
    
    public static String repeat(String str, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(str);
        }
        return result.toString();
    }
    
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
    
    /** Returns the yellow and gui-scaled warning sign. */
    public static String warningSign() {
        return guiScaledFontHTML(uiYellow()) + WARNING_SIGN + "</FONT>";
    }
    
    /** Returns the (usually) red and gui-scaled warning sign. */
    public static String criticalSign() {
        return guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()) + WARNING_SIGN + "</FONT>";
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
            if (curr.isEmpty()) {
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
    
    /** 
     * Returns a UIManager Color that can be used as an alternate row color in a table
     * to offset each other row.
     */
    public static Color alternateTableBGColor() {
        Color result = UIManager.getColor("Table.alternateRowColor");
        if (result != null) {
            return result;
        }
        result = UIManager.getColor("controlHighlight");
        if (result != null) {
            return result;
        }
        result = UIManager.getColor("Table.background");
        if (result != null) {
            return result;
        }
        // The really last fallback position
        return uiGray();
    }
    
    /** 
     * Returns the Color associated with either enemies, allies or 
     * oneself from the GUIPreferences depending on the relation
     * of the given player1 and player2. 
     */
    public static Color teamColor(IPlayer player1, IPlayer player2) {
        if (player1.getId() == player2.getId()) {
            return GUIPreferences.getInstance().getMyUnitColor();
        } else if (player1.isEnemyOf(player2)) {
            return GUIPreferences.getInstance().getEnemyUnitColor();
        } else {
            return GUIPreferences.getInstance().getAllyUnitColor();
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
    
    public static Dimension scaleForGUI(Dimension dim) {
        float scale = GUIPreferences.getInstance().getGUIScale();
        return new Dimension((int)(scale * dim.width), (int)(scale * dim.height));
    }
    
    /** 
     * Returns the provided color with its alpha value set to the provided alpha.
     * alpha should be from 0 to 255 with 0 meaning transparent. 
     */
    public static Color addAlpha(Color color, int alpha) {
        Objects.requireNonNull(color);
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("Alpha value out of range: " + alpha);
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
    
    /** 
     * Returns a grayed-out version of the given color. gray should be from 0 to 255
     * with 255 meaning completely gray. Does not change the brightness, nor alpha. 
     */
    public static Color addGray(Color color, int gray) {
        Objects.requireNonNull(color);
        if (gray < 0 || gray > 255) {
            throw new IllegalArgumentException("Gray value out of range: " + gray);
        }
        int mid = (color.getRed() + color.getGreen() + color.getBlue()) * gray / 3;
        int red = (color.getRed() * (255 - gray) + mid) / 255;
        int green = (color.getGreen() * (255 - gray) + mid) / 255;
        int blue = (color.getBlue() * (255 - gray) + mid) / 255;
        return new Color(red, green, blue, color.getAlpha());
    }
    
    /** Returns the given String str enclosed in HTML tags and with a font tag according to the guiScale. */ 
    public static String scaleStringForGUI(String str) {
        return "<HTML>" + UIUtil.guiScaledFontHTML() + str + "</FONT></HTML>";
    }
    
    /** Returns the given String str enclosed in HTML tags and with a font tag according to the guiScale. */ 
    public static String scaleMessageForGUI(String str) {
        return "<HTML>" + UIUtil.guiScaledFontHTML() + Messages.getString(str) + "</FONT></HTML>";
    }
    
    /** 
     * Applies the current gui scale to a given dialog or whatever Container is given.
     * For a dialog, pass getContentPane(). This can work well for simple dialogs,
     * but it is of course "experimental". Complex dialogs must be hand-adapted to the 
     * gui scale.
     */
    public static void adjustDialog(Container contentPane) {
        Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        Component[] allComps = contentPane.getComponents();
        for (Component comp: allComps) {
            if ((comp instanceof JButton) || (comp instanceof JLabel)
                    || (comp instanceof JComboBox<?>) || (comp instanceof JCheckBox)
                    || (comp instanceof JTextField) || (comp instanceof JSlider)
                    || (comp instanceof JSpinner) || (comp instanceof JRadioButton)
                    || (comp instanceof JTextArea) || (comp instanceof JTextPane)
                    || (comp instanceof JToggleButton)) {
                comp.setFont(scaledFont);
            }
            if (comp instanceof JScrollPane 
                    && ((JScrollPane)comp).getViewport().getView() instanceof JComponent) {
                adjustDialog((JViewport)((JScrollPane)comp).getViewport());
            }
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel)comp;
                Border border = panel.getBorder();
                if ((border != null) && (border instanceof TitledBorder)) {
                    ((TitledBorder)border).setTitleFont(scaledFont);
                }
                if ((border != null) && (border instanceof EmptyBorder)) {
                    Insets i = ((EmptyBorder)border).getBorderInsets();
                    int top = scaleForGUI(i.top);
                    int bottom = scaleForGUI(i.bottom);
                    int left = scaleForGUI(i.left);
                    int right = scaleForGUI(i.right);
                    panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
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
    
    /** A specialized panel for the header of a section. */
    public static class Header extends JPanel {
        private static final long serialVersionUID = -6235772150005269143L;
        
        public Header(String text) {
            super();
            setLayout(new GridLayout(1, 1, 0, 0));
            add(new JLabel("\u29C9  " + Messages.getString(text)));
            setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBackground(alternateTableBGColor());
        }
    }
    
    /** A panel for the content of a subsection of the dialog. */
    public static class Content extends JPanel {
        private static final long serialVersionUID = -6605053283642217306L;

        public Content(LayoutManager layout) {
            this();
            setLayout(layout);
        }
        
        public Content() {
            super();
            setBorder(BorderFactory.createEmptyBorder(8, 8, 5, 8));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }
    
    /** A panel for a subsection of the dialog, e.g. Minefields. */
    public static class OptionPanel extends FixedYPanel {
        private static final long serialVersionUID = -7168700339882132428L;

        public OptionPanel(String header) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            add(new Header(header));
        }
    }

    /** A JPanel that does not stretch vertically beyond its preferred height. */
    public static class FixedYPanel extends JPanel {
        private static final long serialVersionUID = -8805710112708937089L;
        
        public FixedYPanel(LayoutManager layout) {
            super(layout);
        }
        
        public FixedYPanel() {
            super();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
        }
    }
    
    /** A JPanel that does not stretch horizontally beyond its preferred width. */
    public static class FixedXPanel extends JPanel {
        private static final long serialVersionUID = -4634244641653743910L;

        public FixedXPanel(LayoutManager layout) {
            super(layout);
        }
        
        public FixedXPanel() {
            super();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(getPreferredSize().width, super.getMaximumSize().height);
        }
    }
    
    /** 
     * A JLabel with a specialized tooltip display. Displays the tooltip to the right side
     * of the parent dialog, not following the mouse. 
     * Used in the player settings and planetary settings dialogs.
     */
    public static class TipLabel extends JLabel {
        private static final long serialVersionUID = -338233022633675883L;
        
        private JDialog parentDialog;

        public TipLabel(String text, int align, JDialog parent) {
            super(text, align);
            parentDialog = parent;
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            int x = -getLocation().x + parentDialog.getWidth();
            int y = 0;
            return new Point(x, y);
        }
        
        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }
    }
    
    /** 
     * A JButton with a specialized tooltip display. Displays the tooltip to the right side
     * of the parent dialog, not following the mouse. 
     * Used in the player settings and planetary settings dialogs.
     */
    public static class TipButton extends JButton {
        private static final long serialVersionUID = 9076500965039634219L;
        
        private JDialog parentDialog;

        public TipButton(String text, JDialog parent) {
            super(text);
            parentDialog = parent;
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            int x = -getLocation().x + parentDialog.getWidth();
            int y = 0;
            return new Point(x, y);
        }
        
        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }
    }
    
    /** 
     * A JComboBox with a specialized tooltip display. Displays the tooltip to the right side
     * of the parent dialog, not following the mouse. 
     * Used in the player settings and planetary settings dialogs.
     */
    public static class TipCombo<E> extends JComboBox<E> {
        private static final long serialVersionUID = 8663494450966107303L;
        
        private JDialog parentDialog;

        public TipCombo(JDialog parent) {
            super();
            parentDialog = parent;
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            int x = -getLocation().x + parentDialog.getWidth();
            int y = 0;
            return new Point(x, y);
        }
        
        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }
    }
    
    /** 
     * A JTextField with a specialized tooltip display. Displays the tooltip to the right side
     * of the parent dialog, not following the mouse. 
     * Used in the player settings and planetary settings dialogs.
     */
    public static class TipTextField extends JTextField {
        private static final long serialVersionUID = -2226586551388519966L;
        
        private JDialog parentDialog;

        public TipTextField(int n, JDialog parent) {
            super(n);
            parentDialog = parent;
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            int x = -getLocation().x + parentDialog.getWidth();
            int y = 0;
            return new Point(x, y);
        }
        
        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }
    }
    
    /**
     * This is a specialized JPanel for use with a button bar at the bottom
     * of a dialog for when it's possible that the button bar has to wrap (is
     * wider than the dialog and needs to use two or more rows for the buttons).
     * With a normal JPanel the wrapped buttons just disappear. 
     * This Panel tries to detect when wrapping occurs and then extends vertically.
     * Note that it will only extend to two rows, not more. But if three rows of 
     * buttons are used, this will be very obvious.
     * The native FlowLayout should be kept for the buttons. 
     */
    public static class WrappingButtonPanel extends JPanel {
        private static final long serialVersionUID = -6966176665047676553L;

        @Override
        public Dimension getPreferredSize() {
            int height = super.getPreferredSize().height; 
            if (getSize().width < super.getPreferredSize().width) {
                height = height * 2;
            }
            return new Dimension(super.getPreferredSize().width, height);
        }
        
        @Override
        public Dimension getMinimumSize() {
            return new Dimension(super.getMinimumSize().width, getPreferredSize().height);
        }
        
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
        }
    };
    
    /**
     * Returns a single menu item with the given text, the given command string
     * cmd, the given enabled state, and assigned the given listener.
     */
    public static JMenuItem menuItem(String text, String cmd, boolean enabled, 
            ActionListener listener) {

        return menuItem(text, cmd, enabled, listener, Integer.MIN_VALUE);
    }

    /**
     * Returns a single menu item with the given text, the given command string
     * cmd, the given enabled state, and assigned the given listener. Also assigns
     * the given key mnemonic.
     */
    public static JMenuItem menuItem(String text, String cmd, boolean enabled, 
            ActionListener listener, int mnemonic) {

        JMenuItem result = new JMenuItem(text);
        result.setActionCommand(cmd);
        result.addActionListener(listener);
        result.setEnabled(enabled);
        if (mnemonic != Integer.MIN_VALUE) {
            result.setMnemonic(mnemonic);
        }
        return result;
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
    public static String colorString(Color col) {
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
