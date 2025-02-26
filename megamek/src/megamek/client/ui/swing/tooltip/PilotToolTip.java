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

import static megamek.client.ui.swing.tooltip.TipUtil.getOptionList;
import static megamek.client.ui.swing.tooltip.TipUtil.htmlSpacer;
import static megamek.client.ui.swing.util.UIUtil.*;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.imageio.ImageIO;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.InGameObject;
import megamek.common.MekWarrior;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.options.OptionsConstants;
import megamek.common.util.CrewSkillSummaryUtil;
import megamek.logging.MMLogger;

public final class PilotToolTip {
    private static final MMLogger logger = MMLogger.create(PilotToolTip.class);

    /** the portrait base size */
    public static final int PORTRAIT_BASESIZE = 72;
    static final String TEMP_DIR = "/temp/";
    static final String PORTRAIT_PREFIX = "TT_Portrait_";
    static final String PNG_EXT = ".png";

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public static StringBuilder lobbyTip(InGameObject unit) {
        if (unit instanceof Entity) {
            return getPilotTipDetailed((Entity) unit, true);
        } else if (unit instanceof AlphaStrikeElement) {
            // TODO : Provide a suitable tip
            return new StringBuilder("AlphaStrikeElement " + ((AlphaStrikeElement) unit).getName());
        } else {
            return new StringBuilder("This type of object has currently tooltip.");
        }
    }

    public static StringBuilder getPilotTipDetailed(Entity entity, boolean showPortrait) {
        return getPilotTip(entity, true, showPortrait, true, false);
    }

    public static StringBuilder getPilotTipShort(Entity entity, boolean showPortrait, boolean report) {
        return getPilotTip(entity, false, showPortrait, false, report);
    }

    public static StringBuilder getPilotTipLine(Entity entity) {
        return crewInfoLine(entity);
    }

    // PRIVATE

    private static StringBuilder getPilotTip(final Entity entity, boolean detailed, boolean showPortrait,
            boolean showDefaultPortrait, boolean report) {
        String result = "";

        if (!detailed) {
            result += "<HR STYLE=WIDTH:90% />";
        }

        String rows = "";
        String row = "";
        String cols = "";

        // The crew info (names etc.) and portraits, if shown, are placed
        // in a table side by side
        if (showPortrait) {
            cols = crewPortraits(entity, showDefaultPortrait, report).toString();
        }

        cols += crewInfoCell(entity);
        cols += crewPickedUpCell(entity);
        row = UIUtil.tag("TR", "", cols);
        rows += row;
        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=2 BORDER=0", rows);
        result +=  UIUtil.tag("<DIV", "width=100%", table);

        if (!detailed) {
            result += "<HR STYLE=WIDTH:90% />";
        } else {
            result += htmlSpacer(3);
        }

        return new StringBuilder().append(result);
    }

    /** The crew advantages and MD */
    public static StringBuilder getCrewAdvs(Entity entity, boolean detailed) {
        String sCrewAdvs = crewAdvs(entity, detailed).toString();
        String result = htmlSpacer(3) + sCrewAdvs;

        return new StringBuilder().append(result);
    }

    private static StringBuilder crewInfoLine(final Entity entity) {
        Crew crew = entity.getCrew();
        Game game = entity.getGame();
        // Effective entity skill for the whole crew
        boolean rpg_skills = game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        String col = CrewSkillSummaryUtil.getSkillNames(entity) + ": " + crew.getSkillsAsString(rpg_skills);
        col = UIUtil.tag("TD", "", col);
        String row = UIUtil.tag("TR", "", col);
        String rows = row;
        String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0 BORDER=0", rows);
        return new StringBuilder(table);
    }

    /** Returns a tooltip part with names and skills of the crew. */
    private static StringBuilder crewInfoCell(final Entity entity) {
        Crew crew = entity.getCrew();
        Game game = entity.getGame();
        String result = "";

        // Name / Callsign and Status for each crew member
        for (int i = 0; i < crew.getSlotCount(); i++) {
            String sCrew = "";
            if (crew.isMissing(i)) {
                continue;
            }

            if ((crew.getNickname(i) != null) && !crew.getNickname(i).isBlank()) {
                String sNickName = UIUtil.tag("B", "", crew.getNickname(i).toUpperCase());
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(UIUtil.uiNickColor()));
                sCrew += UIUtil.tag("FONT", attr,  sNickName);
            } else if ((crew.getName(i) != null) && !crew.getName(i).isBlank()) {
                sCrew += crew.getName(i) + " ";
            } else {
                sCrew += Messages.getString("BoardView1.Tooltip.Pilot");
            }

            if (crew.getSlotCount() > 1) {
                sCrew += " \u2B1D " + crew.getCrewType().getRoleName(i) + " ";
            }

            if (!crew.getStatusDesc(i).isEmpty()) {
                String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getWarningColor()));
                sCrew += UIUtil.tag("FONT", attr,  crew.getStatusDesc(i));
            }
            result += sCrew + "<BR>";
        }

        // Effective entity skill for the whole crew
        boolean rpg_skills = game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        result += CrewSkillSummaryUtil.getSkillNames(entity) + ": " + crew.getSkillsAsString(rpg_skills);
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        result = UIUtil.tag("span", fontSizeAttr, result);
        String col = UIUtil.tag("TD", "align=\"left\"", result);

        return new StringBuilder().append(col);
    }

    /** Returns a tooltip part with any pilots picked up by this unit. */
    private static StringBuilder crewPickedUpCell(final Entity entity) {
        Game game = entity.getGame();

        String pickedUp = game.getEntitiesVector().stream()
                .filter(e -> (e.isDeployed()
                        && ((e instanceof MekWarrior) && ((MekWarrior) e).getPickedUpById() == entity.getId())))
                .map(e -> e.getCrew().getName())
                .collect(Collectors.joining(", "));

        String col = "";

        if (!pickedUp.isEmpty()) {
            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getCautionColor()));
            pickedUp = UIUtil.tag("FONT", attr,  Messages.getString("BoardView1.Tooltip.PickedUp") + pickedUp);
            String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
            pickedUp = UIUtil.tag("span", fontSizeAttr, pickedUp);
            col = UIUtil.tag("TD", "", pickedUp);
        }

        return new StringBuilder().append(col);
    }

    /** Returns a tooltip part with crew portraits. */
    private static StringBuilder crewPortraits(final Entity entity, boolean showDefaultPortrait, boolean report) {
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
                Image portrait = crew.getPortrait(i).getBaseImage().getScaledInstance(-1, (int) imgSize,
                        Image.SCALE_SMOOTH);
                String img = "";

                if (!report) {
                    try {
                        // Write the scaled portrait to file
                        // This is done to avoid using HTML rescaling on the portrait which does
                        // not do any smoothing and has extremely ugly results
                        String tempPath = Configuration.imagesDir() + TEMP_DIR + PORTRAIT_PREFIX
                            + crew.getExternalIdAsString() + "_" + i + PNG_EXT;
                        File tempFile = new File(tempPath);
                        if (!tempFile.exists()) {
                            BufferedImage bufferedImage = new BufferedImage(portrait.getWidth(null),
                                portrait.getHeight(null), BufferedImage.TYPE_INT_RGB);
                            bufferedImage.getGraphics().drawImage(portrait, 0, 0, null);
                            ImageIO.write(bufferedImage, "PNG", tempFile);
                        }
                        img = "<IMG SRC=file:" + tempPath + ">";
                    } catch (Exception e) {
                        // when an error occurs, do not display any image
                    }
                } else {
                    // span crew tag replaced later in Client.receiveReport with crew portrait
                    img = UIUtil.tag("span", "crew='" + entity.getId() + ":" + i + "'", "");
                }

                col += UIUtil.tag("TD", "VALIGN=TOP", img);
            } catch (Exception e) {
                logger.error("", e);
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
        String sOptionList = "";
        Crew crew = entity.getCrew();
        sOptionList = getOptionList(crew.getOptions().getGroups(), crew::countOptions, detailed);
        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipQuirkColor()));
        sOptionList = UIUtil.tag("FONT", attr,  sOptionList);
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        result = UIUtil.tag("span", fontSizeAttr, sOptionList);

        return new StringBuilder().append(result);
    }

    private PilotToolTip() {
    }

    public static void deleteImageCache() {
        String tempPath = Configuration.imagesDir() + TEMP_DIR;
        String filter = PORTRAIT_PREFIX + "*" + PNG_EXT;

        try {
            StreamSupport.stream(Files.newDirectoryStream(Paths.get(tempPath), filter).spliterator(), true)
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception ex) {
                        }
                    });
        } catch (Exception ex) {
        }
    }

    public static void deleteImageCache(Crew crew, int pos) {
        String tempPath = Configuration.imagesDir() + TEMP_DIR + PORTRAIT_PREFIX + crew.getExternalIdAsString() + "_"
                + pos + PNG_EXT;
        File tempFile = new File(tempPath);
        try {
            Files.deleteIfExists(tempFile.toPath());
        } catch (Exception ex) {
        }
    }
}
