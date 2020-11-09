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

import java.awt.Image;
import java.io.File;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.*;
import megamek.common.util.CrewSkillSummaryUtil;

import static megamek.client.ui.swing.tooltip.TipUtil.*;
import static megamek.client.ui.swing.util.UIUtil.*;

public final class PilotToolTip {
    
    // CONTROL

    /** the portrait base size */
    private final static int PORTRAIT_BASESIZE = 72;

    // PUBLIC
    
    public static String getPilotTipDetailed(Entity entity) {
        return getPilotTip(entity, true);
    }
    
    public static String getPilotTipShort(Entity entity) {
        return getPilotTip(entity, false);
    }
    
    // PRIVATE

    private static String getPilotTip(Entity entity, boolean detailed) {
        StringBuilder result = new StringBuilder();
        
        // The crew info (names etc.) and portraits, if shown, are placed
        // in a table side by side
        result.append(TABLE_BEGIN);
        result.append(crewInfo(entity));
        
        if (GUIPreferences.getInstance().getBoolean(GUIPreferences.SHOW_PILOT_PORTRAIT_TT)) {
            // Add a spacer cell
            int dist = (int)(GUIPreferences.getInstance().getGUIScale() * 10);
            result.append("<TD WIDTH=" + dist + "></TD>");
            result.append(crewPortraits(entity));
        }
        result.append(TABLE_END);
        
        // The crew advantages and MD
        result.append(scaledHTMLSpacer(3));
        result.append(crewAdvs(entity, detailed));
        return result.toString();
    }
    
    /** Returns a tooltip part with names and skills of the crew. */
    private static String crewInfo(Entity entity) {
        Crew crew = entity.getCrew();
        IGame game = entity.getGame();
        StringBuilder result = new StringBuilder();
        result.append(guiScaledFontHTML());
        
        // Name / Callsign and Status for each crew member
        for (int i = 0; i < crew.getSlotCount(); i++) {
            if (crew.isMissing(i)) {
                continue;
            }

            if ((crew.getNickname(i) != null) && !crew.getNickname(i).equals("")) {
                result.append(guiScaledFontHTML(UIUtil.uiNickColor()) + "<B>'" 
                        + crew.getNickname(i).toUpperCase() + "'</B></FONT>");
            } else if ((crew.getName(i) != null) && !crew.getName(i).equals("")) {
                result.append(crew.getName(i));
            } else {
                result.append("Pilot");
            }

            if (crew.getSlotCount() > 1) {
                result.append(" \u2B1D " + crew.getCrewType().getRoleName(i));
            }
            
            if (!crew.getStatusDesc(i).isBlank()) {
                result.append("<BR>" + guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
                result.append(" (" + crew.getStatusDesc(i) + ")</FONT>");
            }
            result.append("<BR>");
        }
        
        // Effective entity skill for the whole crew
        boolean rpg_skills = game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        result.append(CrewSkillSummaryUtil.getSkillNames(entity) + ": " + crew.getSkillsAsString(rpg_skills));
        
        result.append("</FONT>");
        return result.toString();
    }
    
    /** Returns a tooltip part with crew portraits. */
    private static String crewPortraits(Entity entity) {
        Crew crew = entity.getCrew();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < crew.getSlotCount(); i++) {
            String category = crew.getPortraitCategory(i);
            String file = crew.getPortraitFileName(i);
            if ((category == null) || (file == null)) {
                continue;
            }
            String imagePath = Configuration.portraitImagesDir() + "/" + category + file;
            File f = new File(imagePath);
            if (f.exists()) {
                // HACK: Get the real portrait to find the size of the image
                // and scale the tooltip HTML IMG accordingly
                Image portrait = MMStaticDirectoryManager.getUnscaledPortraitImage(category, file);
                // adjust the size to the GUI scale and number of pilots
                float base = GUIPreferences.getInstance().getGUIScale() * PORTRAIT_BASESIZE;
                base /= 0.2f * (crew.getSlotCount() - 1) + 1;
                if (portrait.getWidth(null) > portrait.getHeight(null)) {
                    float h = base * portrait.getHeight(null) / portrait.getWidth(null);
                    addToTT(result, "PilotPortrait", NOBR, imagePath, (int) base, (int) h);
                } else {
                    float w = base * portrait.getWidth(null) / portrait.getHeight(null);
                    addToTT(result, "PilotPortrait", NOBR, imagePath, (int) w, (int) base);
                }
                result.append("<TD WIDTH=3></TD>");
            }
        }
        return result.toString();
    }
    
    /** 
     * Returns a tooltip part with crew advantages. When detailed is
     * true, the advantages will be fully listed, otherwise only the
     * groups and number of advantages per group are given.
     */
    private static String crewAdvs(Entity entity, boolean detailed) {
        Crew crew = entity.getCrew();
        StringBuilder result = new StringBuilder();
        result.append(guiScaledFontHTML(UIUtil.uiQuirksColor(), UnitToolTip.QUIRKS_FONTDELTA));
        result.append(getOptionList(crew.getOptions().getGroups(), grp -> crew.countOptions(grp), detailed));
        result.append("</FONT>");
        return result.toString(); 
    }
    
    /** Helper method to shorten repetitive calls. */
    private static void addToTT(StringBuilder tip, String tipName, boolean startBR, Object... ttO) {
        if (startBR == BR) {
            tip.append("<BR>");
        }
        if (ttO != null) {
            tip.append(Messages.getString("BoardView1.Tooltip." + tipName, ttO));
        } else {
            tip.append(Messages.getString("BoardView1.Tooltip." + tipName));
        }
    }
    
    
}
