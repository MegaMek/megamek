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
package megamek.client.ui.swing.tooltip;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Configuration;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;

/** Provides static helper functions for creating entity and crew tooltips. */
public final class TipUtil {
    
    final static boolean BR = true;
    final static boolean NOBR = false;
    final static String TABLE_BEGIN = "<TABLE CELLSPACING=0 CELLPADDING=0><TBODY><TR><TD VALIGN=TOP>"; 
    final static String TABLE_END = "</TR></TBODY></TABLE>";
    /** The style = font-size: xx value corresponding to a GUI scale of 1 */
    final static int FONT_SCALE1 = 14;

    
    /** 
     * Returns an HTML FONT tag setting the font face to Dialog 
     * and the font size according to GUIScale. 
     */
    static String getFontHTML() {
        return "<FONT FACE=Dialog " + sizeString() + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the color to the given col,
     * the font face to Dialog and the font size according to GUIScale. 
     */
    static String getFontHTML(Color col) {
        return "<FONT FACE=Dialog " + sizeString() + colorString(col) + ">";
    }
    
    /** 
     * Returns an HTML FONT tag setting the font face to Dialog 
     * and the font size 1 step smaller than GUIScale. 
     */
    static String getSmallFontHTML() {
        return "<FONT FACE=Dialog " + smallSizeString() + ">";
    }
    
    /** 
     * Helper method to place Strings in lines according to length. The Strings
     * in origList will be added to one line with separator sep between them as 
     * long as the total length does not exceed maxLength. If it exceeds maxLength, 
     * a new line is begun. All lines but the last will end with sep if sepAtEnd is true. 
     */
    static ArrayList<String> arrangeInLines(List<String> origList, int maxLength, 
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
     * Returns an HTML String listing the options given as optGroups, which
     * is e.g. crew.getOptions().getGroups() or entity.getQuirks().getGroups().
     * A counter function for the options of a group must be supplied, in the form of
     * e.g. e -> crew.countOptions(e) or e -> entity.countQuirks(e).
     * A namer function for the group names must be supplied, e.g. (e) -> weapon.getDesc().
     * The group names are italicized.
     * The list is 40 characters wide with \u2B1D as option separator.
     */
    static String getOptionList(Enumeration<IOptionGroup> optGroups, Function<String, Integer> counter, 
            Function<IOptionGroup, String> namer, boolean detailed) {
        if (detailed) {
            return optionListFull(optGroups, counter, namer);
        } else {
            return optionListShort(optGroups, counter, namer);
        }
    }
    
    /** 
     * Returns an HTML String listing the options given as optGroups, which
     * is e.g. crew.getOptions().getGroups() or entity.getQuirks().getGroups().
     * A counter function for the options of a group must be supplied, in the form of
     * e.g. e -> crew.countOptions(e) or e -> entity.countQuirks(e).
     * The list is 40 characters wide with \u2B1D as option separator.
     */
    static String getOptionList(Enumeration<IOptionGroup> optGroups, 
            Function<String, Integer> counter, boolean detailed) {
        if (detailed) {
            return optionListFull(optGroups, counter, e -> e.getDisplayableName());
        } else {
            return optionListShort(optGroups, counter, e -> e.getDisplayableName());
        }
    }

    static String getScaledHTMLSpacer(final int unscaledSize) {
        int scaledSize = (int)(GUIPreferences.getInstance().getGUIScale() * unscaledSize);  
        return "<P><IMG SRC=FILE:" + Configuration.widgetsDir() + "/Tooltip/TT_Spacer.png "
                + "WIDTH=" + scaledSize + " HEIGHT=" + scaledSize + "></P>";
    }
        
    // PRIVATE
    
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
    
    private static String optionListFull(Enumeration<IOptionGroup> advGroups, 
            Function<String, Integer> counter, Function<IOptionGroup, String> namer) {
        StringBuilder result = new StringBuilder();
        
        boolean firstGroup = true;
        while (advGroups.hasMoreElements()) {
            IOptionGroup advGroup = advGroups.nextElement();
            if (counter.apply(advGroup.getKey()) > 0) {
                // Add a <BR> before each group but the first
                result.append(firstGroup ? "" : "<BR>");
                firstGroup = false;

                // Group title
                result.append("<I>" + namer.apply(advGroup) + ":</I>");
                
                // Gather the group options
                List<String> origList = new ArrayList<String>();
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    IOption adv = advs.nextElement();
                    if (adv.booleanValue()) {
                        origList.add(adv.getDisplayableNameWithValue());
                    }
                }
                
                // Arrange the options in lines according to length
                List<String> advLines = arrangeInLines(origList, 40, " \u2B1D ", false);
                for (String line: advLines) {
                    result.append("<BR>&nbsp;&nbsp;" + line);
                }
            }
        }
        return result.toString();
    }
    
    private static String optionListShort(Enumeration<IOptionGroup> advGroups, 
            Function<String, Integer> counter, Function<IOptionGroup, String> namer) {
        StringBuilder result = new StringBuilder();
        
        // Gather the option groups and option count per group
        List<String> origList = new ArrayList<String>();
        while (advGroups.hasMoreElements()) {
            IOptionGroup advGroup = advGroups.nextElement();
            int numOpts = counter.apply(advGroup.getKey());
            if (numOpts > 0) {
                origList.add(namer.apply(advGroup) + " (" + numOpts + ")");
            }
        }
        
        // Arrange the option groups in lines according to length
        for (String line: arrangeInLines(origList, 40, "; ", true)) {
            result.append("&nbsp;&nbsp;" + line + "<BR>");
        }
        return result.toString();
    }

}
