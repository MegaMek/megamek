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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;

/** Provides static helper functions for creating entity and crew tooltips. */
public final class TipUtil {
    
    final static boolean BR = true;
    final static boolean NOBR = false;
    final static String TABLE_BEGIN = "<TABLE CELLSPACING=0 CELLPADDING=0><TBODY><TR><TD VALIGN=TOP>"; 
    final static String TABLE_END = "</TR></TBODY></TABLE>";
    
    /** 
     * Returns a List wherein each element consists of an option group of the given 
     * optGroups, which is e.g. crew.getOptions().getGroups() or entity.getQuirks().getGroups()
     * as well as the count of active options within that group, e.g. "Manei Domini (2)".
     * A counter function for the options of a group must be supplied, in the form of
     * e.g. e -> crew.countOptions(e) or e -> entity.countQuirks(e).
     * A namer function for the group names must be supplied, e.g. (e) -> weapon.getDesc().
     */
    public static List<String> getOptionListArray(Enumeration<IOptionGroup> optGroups, 
            Function<String, Integer> counter, Function<IOptionGroup, String> namer) {
        
        List<String> result = new ArrayList<String>();
        while (optGroups.hasMoreElements()) {
            IOptionGroup advGroup = optGroups.nextElement();
            int numOpts = counter.apply(advGroup.getKey());
            if (numOpts > 0) {
                result.add(namer.apply(advGroup) + " (" + numOpts + ")");
            }
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
    public static String getOptionList(Enumeration<IOptionGroup> optGroups, Function<String, Integer> counter, 
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
    public static String getOptionList(Enumeration<IOptionGroup> optGroups, 
            Function<String, Integer> counter, boolean detailed) {
        if (detailed) {
            return optionListFull(optGroups, counter, e -> e.getDisplayableName());
        } else {
            return optionListShort(optGroups, counter, e -> e.getDisplayableName());
        }
    }

    static String scaledHTMLSpacer(final int unscaledSize) {
        int scaledSize = (int)(GUIPreferences.getInstance().getGUIScale() * unscaledSize);  
        return "<P><IMG SRC=FILE:" + Configuration.widgetsDir() + "/Tooltip/TT_Spacer.png "
                + "WIDTH=" + scaledSize + " HEIGHT=" + scaledSize + "></P>";
    }
        
    // PRIVATE
    
    private static String optionListFull(Enumeration<IOptionGroup> advGroups, 
            Function<String, Integer> counter, Function<IOptionGroup, String> namer) {
        StringBuilder result = new StringBuilder();
        
        while (advGroups.hasMoreElements()) {
            IOptionGroup advGroup = advGroups.nextElement();
            if (counter.apply(advGroup.getKey()) > 0) {
                // Group title
                result.append("<I>" + namer.apply(advGroup) + ":</I><BR>");
                
                // Gather the group options
                List<String> origList = new ArrayList<String>();
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    IOption adv = advs.nextElement();
                    if (adv.booleanValue()) {
                        origList.add(adv.getDisplayableNameWithValue());
                    }
                }
                
                // Arrange the options in lines according to length
                List<String> advLines = UIUtil.arrangeInLines(origList, 40, " \u2B1D ", false);
                for (String line: advLines) {
                    result.append("&nbsp;&nbsp;" + line + "<BR>");
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
        for (String line: UIUtil.arrangeInLines(origList, 40, "; ", true)) {
            result.append(line + "<BR>");
        }
        return result.toString();
    }

}
