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
import megamek.common.IGame;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.ReportMessages;
import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.*;

import java.awt.*;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Set;

import static megamek.client.ui.swing.util.UIUtil.*;

public final class SBFInGameObjectTooltip {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final String SHORT = "&nbsp;";
    private static final Set<String> ABBREV_NAME_PARTS_UNIT = Set.of("Lance", "Squadron", "Wing", "Flight");

    public static String styles() {
        float base = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        int labelSize = (int) (0.8 * base);
        int valueSize = (int) (1.1 * base);
        int nameSize = (int) (1.3 * base);

        return ".value { font-family:Exo; font-size:20; }" +
                ".label { font-family:Noto Sans; font-size:" + labelSize + "; color:gray; }" +
                ".idnum { font-family:Exo; font-size:" + labelSize + "; color:gray; text-align:right; }" +
                ".unitname { padding-right:10; font-family:Noto Sans; font-size:" + valueSize + "; }" +
                ".valuecell { padding-right:10; font-family:Exo; font-size:" + valueSize + "; text-align: center; }" +
                ".pvcell { font-family:Exo; font-size:" + nameSize + "; text-align: right; }" +
                ".speccell { font-family:Exo; font-size:" + labelSize + "; }" +
                ".fullwidth { width:100%; }" +
                ".formation { font-family:Noto Sans; font-size:" + nameSize + "; }" +
                "th, td { padding:0 2; }";
    }

    public static String getTooltip(InGameObject unit, @Nullable IGame game) {
        StringBuilder result = new StringBuilder();
        Player owner = (game != null) ? game.getPlayer(unit.getOwnerId()) : null;
        Color ownerColor = (owner != null) ? owner.getColour().getColour() : Color.BLACK;
        String styleColor = Integer.toHexString(ownerColor.getRGB() & 0xFFFFFF);
        result.append("<div style=\"padding:0 10; border:2; margin: 5 0; border-style:solid; border-color:")
                .append(styleColor).append(";\">");
        result.append(nameLines(unit, game));
        result.append(formationStats(unit));
        result.append("</div>");
        return result.toString();
    }

    private static StringBuilder nameLines(InGameObject unit, @Nullable IGame game) {
        StringBuilder result = new StringBuilder();
        Player owner = (game != null) ? game.getPlayer(unit.getOwnerId()) : null;
        Color ownerColor = (owner != null) ? owner.getColour().getColour() : GUIP.getUnitToolTipFGColor();

        // obscure values for sensor blips
        String pointValue = (unit instanceof SBFFormation) ? Integer.toString(unit.getStrength()) : "??";

        String pvCell = spanCSS("label", "PV") + SHORT + spanCSS("pvcell", pointValue);
        result.append("<TABLE class=fullwidth><TR>");
        result.append(tdCSS("formation", unitName(unit)))
                .append(tdCSS("pvcell", pvCell));
        result.append("</TR></TABLE>");

        String ownerName = (owner != null) ? owner.getName() : ReportMessages.getString("BoardView1.Tooltip.unknownOwner");
        result.append("<TABLE class=fullwidth><TR>");
        result.append(tdCSS("unitname", ownerName))
                .append(tdCSS("idnum", idString(unit)));
        result.append("</TR></TABLE>");
        return result;
    }

    private record Stats(String type, String size, String morale, String mv, String mvcode, String jump,
                         String trsp, String trspcode, String tactics, String tmm, String spec, String skill) { }

    private static StringBuilder formationStats(InGameObject unit) {
        Stats stats;
        if (unit instanceof SBFFormation formation) {
            stats = statsOf(formation);
        } else if (unit instanceof SBFPartialScanUnitPlaceHolder p) {
            stats = new Stats("" + p.getType(), "" + p.getSize(), "?", "" + p.getMovement(),
                    "", "?", "?", "?", "?", "?", "?", "?");
        } else if (unit instanceof SBFIGotSomethingUnitPlaceholder p) {
            stats = new Stats("" + p.getType(), "" + p.getSize(), "?", "?", "?",
                    "?", "?", "", "?", "?", "?", "?");
        } else if (unit instanceof SBFSomethingOutThereUnitPlaceHolder p) {
            stats = new Stats("" + p.getType(), "?", "?", "?", "", "?", "?",
                    "", "?", "?", "?", "?");
        } else {
            stats = new Stats("?", "?", "?", "?", "", "?", "?",
                    "", "?", "?", "?", "?");
        }

        return formationStats(stats, unit);
    }

    private static Stats statsOf(SBFFormation f) {
        return new Stats("" + f.getType(), "" + f.getSize(), "" + f.getMorale(),
                "" + f.getMovement(), f.getMovementCode(), "" + f.getJumpMove(),
                "" + f.getTrspMovement(), f.getTrspMovementCode(), "" + f.getTactics(),
                "" + f.getTmm(), f.getSpecialsDisplayString(f), "" + f.getSkill());
    }

    private static StringBuilder formationStats(Stats stats, InGameObject unit) {
        StringBuilder result = new StringBuilder();

        result.append("<TABLE><TR>");
        result.append(tdCSS("label", "TP"))
                .append(tdCSS("valuecell", stats.type))
                .append(tdCSS("label", "SZ"))
                .append(tdCSS("valuecell", stats.size))
                .append(tdCSS("label", "MO"))
                .append(tdCSS("valuecell", stats.morale));
        result.append("</TR></TABLE>");

        result.append("<TABLE><TR>");
        result.append(tdCSS("label", "MV"))
                .append(tdCSS("valuecell", "" + stats.mv + stats.mvcode));

        if (!Objects.equals(stats.jump, "0")) {
            result.append(tdCSS("label", "JUMP"))
                    .append(tdCSS("valuecell", stats.jump));
        }

        if (!Objects.equals(stats.trsp, stats.mv)) {
            result.append(tdCSS("label", "Trsp MV"))
                    .append(tdCSS("valuecell", "" + stats.trsp + stats.trspcode));
        }

        result.append(tdCSS("label", "TC"))
                .append(tdCSS("valuecell", stats.tactics));
        result.append("</TR></TABLE>");

        result.append("<TABLE><TR>");
        result.append(tdCSS("label", "TMM"))
                .append(tdCSS("valuecell", stats.tmm))
                .append(tdCSS("label", "Skill"))
                .append(tdCSS("valuecell", stats.skill))
                .append(tdCSS("label", "SPEC"))
                .append(tdCSS("valuecell", stats.spec));
        result.append("</TR></TABLE>");
        if (unit instanceof SBFFormation formation) {
            result.append(unitsStats(formation));
        }
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
        String name= (unit.generalName() + " " + unit.specificName()).trim();
        return name+"&nbsp;".repeat(Math.max(0, 25-name.length()));
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

    private SBFInGameObjectTooltip() { }
}
