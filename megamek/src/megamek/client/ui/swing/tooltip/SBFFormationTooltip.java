/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.tooltip;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.FontHandler;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.awt.*;
import java.text.MessageFormat;
import java.util.Set;

public final class SBFFormationTooltip {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final String BR = "<BR>";
    private static final String SPACER = "&nbsp;&nbsp;&nbsp;";
    private static final String SHORT = "&nbsp;";
    private static final String TABLE = "<TABLE>";

    private static final Set<String> ABBREV_NAME_PARTS_UNIT = Set.of("Lance", "Squadron", "Wing", "Flight");

    private SBFFormationTooltip() { }

    public static String getTooltip(SBFFormation formation, @Nullable IGame game) {
        StringBuilder result = new StringBuilder("<HTML><HEAD>");
        result.append(styles());
        result.append("</HEAD><BODY>");
        Player owner = (game != null) ? game.getPlayer(formation.getOwnerId()) : null;
        Color ownerColor = (owner != null) ? owner.getColour().getColour() : Color.BLACK;
        String styleColor = Integer.toHexString(ownerColor.getRGB() & 0xFFFFFF);
        result.append("<div style=\"padding:0 10; border:2; border-style:solid; border-color:" + styleColor + ";\">");
        result.append(getDisplayNames(formation, game));
        result.append(formationStats(formation));
        result.append("</div>");
        result.append("</BODY></HTML>");
        return result.toString();
    }

    private static String asCSS(String cssClass, String content) {
        return "<span class=\"" + cssClass + "\">" + content + "</span>";
    }

    private static String tdCSS(String cssClass, String content) {
        return "<TD class=\"" + cssClass + "\">" + content + "</TD>";
    }

    private static String tdCSS(String cssClass, int content) {
        return "<TD class=\"" + cssClass + "\">" + content + "</TD>";
    }

    private static String asCSS(String cssClass, int content) {
        return "<span class=\"" + cssClass + "\">" + content + "</span>";
    }

    private static StringBuilder styles() {
        StringBuilder result = new StringBuilder("<style>");
        result.append(".value { font-family:Exo; font-size:20; }");
        result.append(".label { font-family:Noto Sans; font-size:14; color:gray; }");
        result.append(".idnum { font-family:Exo; font-size:14; color:gray; }");
        result.append(".unitname { font-family:Noto Sans; font-size:16; }");
        result.append(".valuecell { font-family:Exo; font-size:20; text-align: center; }");
        result.append(".speccell { font-family:Exo; font-size:14; }");
        result.append("th, td { padding:0 5; }");
        return result;
    }

    private static StringBuilder getDisplayNames(InGameObject unit, @Nullable IGame game) {
        StringBuilder result = new StringBuilder();
        Player owner = (game != null) ? game.getPlayer(unit.getOwnerId()) : null;
        Color ownerColor = (owner != null) ? owner.getColour().getColour() : GUIP.getUnitToolTipFGColor();
        result.append(idString(unit));
        result.append(guiScaledFontHTML(ownerColor));
        result.append(unitName(unit));
        String ownerName = (owner != null) ? owner.getName() : ReportMessages.getString("BoardView1.Tooltip.unknownOwner");
        result.append(BR).append(ownerName);
        result.append("</FONT>");
        return result;
    }

    private static StringBuilder formationStats(SBFFormation formation) {
        StringBuilder result = new StringBuilder();

        result.append(BR)
                .append(asCSS("label", "TP")).append(SHORT)
                .append(asCSS("value", formation.getType().toString())).append(SPACER)
                .append(asCSS("label", "SZ")).append(SHORT)
                .append(asCSS("value", formation.getSize())).append(SPACER)
                .append(asCSS("label", "PV")).append(SHORT)
                .append(asCSS("value", formation.getStrength()));

        result.append(BR)
                .append(asCSS("label", "MV")).append(SHORT)
                .append(asCSS("value", "" + formation.getMovement() + formation.getMovementCode()));

        if (formation.getJumpMove() > 0) {
            result.append(SPACER).append(asCSS("label", "JUMP")).append(SHORT)
                    .append(asCSS("value", formation.getJumpMove()));
        }

        if (formation.getTrspMovement() != formation.getMovement()) {
            result.append(SPACER).append(asCSS("label", "Trsp MV")).append(SHORT)
                    .append(asCSS("value", "" + formation.getTrspMovement() + formation.getTrspMovementCode()));
        }

        result.append(BR)
                .append(asCSS("label", "SPEC")).append(SHORT)
                .append(asCSS("value", formation.getSpecialsDisplayString(formation)));

        result.append(BR)
                .append(asCSS("label", "TC")).append(SHORT)
                .append(asCSS("value", formation.getTactics())).append(SPACER)
                .append(asCSS("label", "MO")).append(SHORT)
                .append(asCSS("value", formation.getMorale())).append(SPACER)
                .append(asCSS("label", "TMM")).append(SHORT)
                .append(asCSS("value", formation.getTmm())).append(SPACER)
                .append(asCSS("label", "Skill")).append(SHORT)
                .append(asCSS("value", formation.getSkill()));

        result.append(unitsStats(formation));

        return result;
    }

    private static StringBuilder unitsStats(SBFFormation formation) {
        StringBuilder result = new StringBuilder();
//        String style = " style=\"color:red;border-style:solid;border-width:0px;padding:0;margin:0;\">";
        result.append("<TABLE>");
        formation.getUnits().forEach(unit -> result.append(unitLine(unit)));
        result.append("</TABLE>");
        return result;
    }

    private static StringBuilder unitLine(SBFUnit unit) {
        StringBuilder result = new StringBuilder();
        result.append("<TR>");
        result.append(tdCSS("unitname", abbrevUnitName(unit.getName())))
                .append(tdCSS("label", "Ärmör"))
                .append(tdCSS("valuecell", unit.getArmor()))
                .append(tdCSS("label", "Dmg"))
                .append(tdCSS("valuecell", unit.getDamage().toString()))
                .append(tdCSS("label", "SPÉC"))
                .append(tdCSS("speccell", unit.getSpecialsDisplayString(unit)));
        result.append("</TR>");
        return result;
    }

    private static String unitName(InGameObject unit) {
        return (unit.generalName() + " " + unit.specificName()).trim();
    }

    private static String idString(InGameObject unit) {
        String id = MessageFormat.format("[ID: {0}]", unit.getId());
        return "<span class=idnum>" + id + "&nbsp;</span>";
    }

    private static void appendAsCells(StringBuilder result, int content, int... moreContents) {
        result.append(cellCenter(Integer.toString(content)));
        for (int moreContent : moreContents) {
            result.append(cellCenter(Integer.toString(moreContent)));
        }
    }

    private static void appendAsCells(StringBuilder result, String content, String... moreContents) {
        result.append(cellCenter(content));
        for (String moreContent : moreContents) {
            result.append(cellCenter(moreContent));
        }
    }

    private static String cellCenter(String content) {
        return "<TD ALIGN=CENTER>" + content + "</TD>";
    }

    private static String cellLeft(String content) {
        return "<TD>" + content + "</TD>";
    }

    public static String guiScaledFontHTML(Color col) {
        return "<FONT " + sizeString() + colorString(col) + ">";
    }

    private static String sizeString() {
        int fontSize = (int) (GUIPreferences.getInstance().getGUIScale() * UIUtil.FONT_SCALE1);
        return " style=font-size:" + fontSize + " ";
    }

    /** Returns an HTML FONT Color String, e.g. COLOR=#FFFFFF according to the given color. */
    public static String colorString(Color col) {
        return " COLOR=" + Integer.toHexString(col.getRGB() & 0xFFFFFF) + " ";
    }

    private static String abbrevUnitName(String unitName) {
        String result = unitName;
        for (String token : ABBREV_NAME_PARTS_UNIT) {
            result = result.replace(" " + token, " " + token.charAt(0) + ".");
        }
        return result;
    }
}
