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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.util.CrewSkillSummaryUtil;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import static megamek.client.ui.swing.tooltip.TipUtil.getOptionList;
import static megamek.client.ui.swing.tooltip.TipUtil.scaledHTMLSpacer;
import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiQuirksColor;

public final class PilotToolTip {
    
    /** the portrait base size */
    private final static int PORTRAIT_BASESIZE = 72;
    final static String BG_COLOR = "#313131";

    public static StringBuilder getPilotTipDetailed(Entity entity, boolean showPortrait) {
        return getPilotTip(entity, true, showPortrait, true);
    }
    
    public static StringBuilder getPilotTipShort(Entity entity, boolean showPortrait) {
        return getPilotTip(entity, false, showPortrait, false);
    }

    // PRIVATE

    private static StringBuilder getPilotTip(final Entity entity, boolean detailed, boolean showPortrait, boolean showDefaultPortrait) {
        String result = "";
        
        if (!detailed) {
            result += "<HR STYLE=WIDTH:90% />";
        }

        // The crew info (names etc.) and portraits, if shown, are placed
        // in a table side by side
        String cols = "";
        if (showPortrait) {
            cols = crewPortraits(entity, showDefaultPortrait).toString();
        }

        cols += crewInfoCell(entity).toString();
        String row = "<TR>" + cols + "</TR>";
        String table = "<TABLE BORDER=0 BGCOLOR=" + BG_COLOR + " >" + row + "</TABLE>";
        result += "<DIV BGCOLOR=" + BG_COLOR + "  width=100% >" + table + "</DIV>";

        if (!detailed) {
            result += "<HR STYLE=WIDTH:90% />";
        } else {
            result += scaledHTMLSpacer(3);
        }
        return new StringBuilder().append(result);
    }

    /** The crew advantages and MD */
    public static StringBuilder getCrewAdvs(Entity entity, boolean detailed) {
        String result = "";
        String f = "";

        f = crewAdvs(entity, detailed).toString();
        result = scaledHTMLSpacer(3) + f +  "</FONT>";

        return new StringBuilder().append(result);
    }
    
    /** Returns a tooltip part with names and skills of the crew. */
    private static StringBuilder crewInfoCell(final Entity entity) {
        Crew crew = entity.getCrew();
        Game game = entity.getGame();
        String f = "";
        String result = "";
        
        // Name / Callsign and Status for each crew member
        for (int i = 0; i < crew.getSlotCount(); i++) {
            if (crew.isMissing(i)) {
                continue;
            }

            if ((crew.getNickname(i) != null) && !crew.getNickname(i).isBlank()) {
                f = "<B>'" + crew.getNickname(i).toUpperCase() + "'</B>";
                result += guiScaledFontHTML(UIUtil.uiNickColor()) + f + "</FONT>";
            } else if ((crew.getName(i) != null) && !crew.getName(i).isBlank()) {
                result += crew.getName(i);
            } else {
                result += Messages.getString("BoardView1.Tooltip.Pilot");
            }

            if (crew.getSlotCount() > 1) {
                result += " \u2B1D " + crew.getCrewType().getRoleName(i);
            }
            
            if (!crew.getStatusDesc(i).isEmpty()) {
                result += guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()) + " (" + crew.getStatusDesc(i) + ")</FONT>";
            }
            result += "<BR>";
        }
        
        // Effective entity skill for the whole crew
        boolean rpg_skills = game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        result += CrewSkillSummaryUtil.getSkillNames(entity) + ": " + crew.getSkillsAsString(rpg_skills);

        result = guiScaledFontHTML() + result + "</FONT>";

        String col = "<TD>" + result + "</TD>";
        return new StringBuilder().append(col);
    }
    
    /** Returns a tooltip part with crew portraits. */
    private static StringBuilder crewPortraits(final Entity entity, boolean showDefaultPortrait) {
        Crew crew = entity.getCrew();
        String col = "";

        for (int i = 0; i < crew.getSlotCount(); i++) {
            if ((!showDefaultPortrait) && crew.getPortrait(i).isDefault()) {
                continue;
            }

            try {
                // Adjust the portrait size to the GUI scale and number of pilots
                float imgSize = UIUtil.scaleForGUI(PORTRAIT_BASESIZE);
                imgSize /= 0.2f * (crew.getSlotCount() - 1) + 1;
                Image portrait = crew.getPortrait(i).getBaseImage().getScaledInstance(-1, (int) imgSize, Image.SCALE_SMOOTH);
                // Write the scaled portrait to file
                // This is done to avoid using HTML rescaling on the portrait which does
                // not do any smoothing and has extremely ugly results
                String tempPath = Configuration.imagesDir() + "/temp/TT_Portrait_" + entity.getExternalIdAsString() + "_" + i + ".png";
                File tempFile = new File(tempPath);
                if (!tempFile.exists()) {
                    BufferedImage bufferedImage = new BufferedImage(portrait.getWidth(null), portrait.getHeight(null), BufferedImage.TYPE_INT_RGB);
                    bufferedImage.getGraphics().drawImage(portrait, 0, 0, null);
                    ImageIO.write(bufferedImage, "PNG", tempFile);
                }
                String img = "<IMG SRC=file:" + tempPath + ">";
                col += "<TD VALIGN=TOP>" + img + "</TD>";
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
        return new StringBuilder().append(col);
    }
    
    /** 
     * Returns a tooltip part with crew advantages. When detailed is
     * true, the advantages will be fully listed, otherwise only the
     * groups and number of advantages per group are given.
     */
    private static StringBuilder crewAdvs(final Entity entity, boolean detailed) {
        String result = "";
        String f = "";
        Crew crew = entity.getCrew();
        f = getOptionList(crew.getOptions().getGroups(), crew::countOptions, detailed);
        result = guiScaledFontHTML(uiQuirksColor(), UnitToolTip.TT_SMALLFONT_DELTA) + f + "</FONT>";
        return new StringBuilder().append(result);
    }
    
}
