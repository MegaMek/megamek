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
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.util.List;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Set;

public final class SBFFormationTooltip {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final String SHORT = "&nbsp;";
    private static final Set<String> ABBREV_NAME_PARTS_UNIT = Set.of("Lance", "Squadron", "Wing", "Flight");

    public static String getTooltip(List<SBFFormation> formations, @Nullable IGame game) {
        StringBuilder result = new StringBuilder("<HTML><HEAD>");
        result.append(styles());
        result.append("</HEAD><BODY>");
        for (SBFFormation formation : formations) {
            Player owner = (game != null) ? game.getPlayer(formation.getOwnerId()) : null;
            Color ownerColor = (owner != null) ? owner.getColour().getColour() : Color.BLACK;
            String styleColor = Integer.toHexString(ownerColor.getRGB() & 0xFFFFFF);
            result.append("<div style=\"padding:0 10; border:2; margin: 5 0; border-style:solid; border-color:" + styleColor + ";\">");
            result.append(nameLines(formation, game));
            result.append(formationStats(formation));
            result.append("</div>");
        }
        result.append("</BODY></HTML>");
        return result.toString();
    }

    public static String getTooltip(SBFFormation formation, @Nullable IGame game) {
        StringBuilder result = new StringBuilder("<HTML><HEAD>");
        result.append(styles());
        result.append("</HEAD><BODY>");
        Player owner = (game != null) ? game.getPlayer(formation.getOwnerId()) : null;
        Color ownerColor = (owner != null) ? owner.getColour().getColour() : Color.BLACK;
        String styleColor = Integer.toHexString(ownerColor.getRGB() & 0xFFFFFF);
        result.append("<div style=\"padding:0 10; border:2; border-style:solid; border-color:" + styleColor + ";\">");
        result.append(nameLines(formation, game));
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
        float base = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        int labelSize = (int) (0.8 * base);
        int valueSize = (int) (1.1 * base);
        int nameSize = (int) (1.3 * base);

        StringBuilder result = new StringBuilder("<style>");
        result.append(".value { font-family:Exo; font-size:20; }");
        result.append(".label { font-family:Noto Sans; font-size:" + labelSize + "; color:gray; }");
        result.append(".idnum { font-family:Exo; font-size:" + labelSize + "; color:gray; text-align:right; }");
        result.append(".unitname { padding-right:10; font-family:Noto Sans; font-size:" + valueSize + "; }");
        result.append(".valuecell { padding-right:10; font-family:Exo; font-size:" + valueSize + "; text-align: center; }");
        result.append(".pvcell { font-family:Exo; font-size:" + nameSize + "; text-align: right; }");
        result.append(".speccell { font-family:Exo; font-size:" + labelSize + "; }");
        result.append(".fullwidth { width:100%; }");
        result.append(".formation { font-family:Noto Sans; font-size:" + nameSize + "; }");
        result.append("th, td { padding:0 2; }");
        return result;
    }

    private static StringBuilder nameLines(SBFFormation formation, @Nullable IGame game) {
        StringBuilder result = new StringBuilder();
        Player owner = (game != null) ? game.getPlayer(formation.getOwnerId()) : null;
        Color ownerColor = (owner != null) ? owner.getColour().getColour() : GUIP.getUnitToolTipFGColor();

        String pvCell = asCSS("label", "PV") + SHORT + asCSS("pvcell", formation.getStrength());
        result.append("<TABLE class=fullwidth><TR>");
        result.append(tdCSS("formation", unitName(formation)))
                .append(tdCSS("pvcell", pvCell));
        result.append("</TR></TABLE>");

        String ownerName = (owner != null) ? owner.getName() : ReportMessages.getString("BoardView1.Tooltip.unknownOwner");
        result.append("<TABLE class=fullwidth><TR>");
        result.append(tdCSS("unitname", ownerName))
                .append(tdCSS("idnum", idString(formation)));
        result.append("</TR></TABLE>");
        return result;
    }

    private static StringBuilder formationStats(SBFFormation formation) {
        StringBuilder result = new StringBuilder();

        result.append("<TABLE><TR>");
        result.append(tdCSS("label", "TP"))
                .append(tdCSS("valuecell", formation.getType().toString()))
                .append(tdCSS("label", "SZ"))
                .append(tdCSS("valuecell", formation.getSize()))
                .append(tdCSS("label", "MO"))
                .append(tdCSS("valuecell", formation.getMorale()));
        result.append("</TR></TABLE>");

        result.append("<TABLE><TR>");
        result.append(tdCSS("label", "MV"))
                .append(tdCSS("valuecell", "" + formation.getMovement() + formation.getMovementCode()));

        if (formation.getJumpMove() > 0) {
            result.append(tdCSS("label", "JUMP"))
                    .append(tdCSS("valuecell", formation.getJumpMove()));
        }

        if (formation.getTrspMovement() != formation.getMovement()) {
            result.append(tdCSS("label", "Trsp MV"))
                    .append(tdCSS("valuecell", "" + formation.getTrspMovement() + formation.getTrspMovementCode()));
        }

        result.append(tdCSS("label", "TC"))
                .append(tdCSS("valuecell", formation.getTactics()));
        result.append("</TR></TABLE>");

        result.append("<TABLE><TR>");
        result.append(tdCSS("label", "TMM"))
                .append(tdCSS("valuecell", formation.getTmm()))
                .append(tdCSS("label", "Skill"))
                .append(tdCSS("valuecell", formation.getSkill()))
                .append(tdCSS("label", "SPEC"))
                .append(tdCSS("valuecell", formation.getSpecialsDisplayString(formation)));
        result.append("</TR></TABLE>");
        result.append(unitsStats(formation));
        return result;
    }

    private static StringBuilder unitsStats(SBFFormation formation) {
        StringBuilder result = new StringBuilder();
        result.append("<TABLE>");
        formation.getUnits().forEach(unit -> result.append(unitLine(unit)));
        result.append("</TABLE>");
        return result;
    }

    private static StringBuilder unitLine(SBFUnit unit) {
        StringBuilder result = new StringBuilder();
        result.append("<TR>");
        result.append(tdCSS("unitname", abbrevUnitName(unit.getName())))
                .append(tdCSS("label", "Armor"))
                .append(tdCSS("valuecell", unit.getArmor()))
                .append(tdCSS("label", "Dmg"))
                .append(tdCSS("valuecell", unit.getDamage().toString()))
                .append(tdCSS("label", "SPEC"))
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

    private static String abbrevUnitName(String unitName) {
        String result = unitName;
        for (String token : ABBREV_NAME_PARTS_UNIT) {
            result = result.replace(" " + token, " " + token.charAt(0) + ".");
        }
        return result;
    }

    private SBFFormationTooltip() { }
}
