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
package megamek.client.ui.swing.boardview;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.FontHandler;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;

public class BoardEditorTooltip implements BoardViewTooltipProvider {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int BASE_PADDING = 8;

    private final IGame game;
    private final IBoardView bv;

    public BoardEditorTooltip(IGame game, IBoardView boardView) {
        this.game = game;
        this.bv = boardView;
    }

    @Override
    public String getTooltip(Point point, Coords movementTarget) {
        final Coords coords = bv.getCoordsAt(point);
        if (!game.getBoard().contains(coords)) {
            return null;
        }
        Hex hex = game.getBoard().getHex(coords);
        if (hex == null) {
            return "Error: No hex found at " + coords;
        }

        int padding = UIUtil.scaleForGUI(BASE_PADDING);

        StringBuilder result = new StringBuilder();
        result.append("<TABLE WIDTH=100% BORDER=0 "
                + "BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipTerrainBGColor())
                + "><TR><TD>");
        result.append(guiScaledFontHTML(GUIP.getUnitToolTipTerrainFGColor()));
        result.append("<FONT FACE=" + FontHandler.getNotoFont().getName() + ">");

        // Hex 0405 - Level: 3
        result.append(Messages.getString("BoardView1.Tooltip.Hex", coords.getBoardNum(), hex.getLevel()));

        // The terrain and auto terrain lines
        List<String> terrainLines = new ArrayList<>();
        List<String> autoTerrainLines = new ArrayList<>();
        for (int type: hex.getTerrainTypes()) {
            String line = String.join(MekTableModel.DOT_SPACER, createLine(hex, type));
            (Terrains.AUTOMATIC.contains(type) ? autoTerrainLines : terrainLines).add(line);
        }

        result.append("<p style=padding-top:" + padding + ">");
        if (terrainLines.isEmpty()) {
            result.append(boldHTML("Clear"));
        } else {
            result.append(String.join("<BR>", terrainLines));
        }
        result.append("</p>");

        if (!autoTerrainLines.isEmpty()) {
            result.append("<p style=padding-top:" + padding + ">")
                    .append("Automated Terrains:<BR>")
                    .append(String.join("<BR>", autoTerrainLines))
                    .append("</p>");
        }

        // Invalid hex notification if invalid
        StringBuffer errBuff = new StringBuffer();
        if (!hex.isValid(errBuff)) {
            String errors = errBuff.toString().replace("\n", "<BR>");
            result.append("<p style=padding-top:" + padding + ">")
                    .append(guiScaledFontHTML(GUIP.getWarningColor())).append(UIUtil.WARNING_SIGN).append("</FONT>")
                    .append(Messages.getString("BoardView1.invalidHex")).append("<BR>")
                    .append(errors)
                    .append("</p>");
        }

        result.append("</FONT></FONT></TD></TR></TABLE>");
        return UnitToolTip.wrapWithHTML(result.toString());
    }

    private List<String> createLine(Hex hex, int terrainType) {
        Terrain terrain = hex.getTerrain(terrainType);
        int tf = terrain.getTerrainFactor();
        int level = terrain.getLevel();
        int exits = terrain.getExits();
        boolean isAutoTerrain = Terrains.AUTOMATIC.contains(terrainType);
        String name = Terrains.getDisplayName(terrainType, level);
        if (name == null) {
            name = Terrains.getEditorName(terrainType);
        }

        if (!isAutoTerrain) {
            name = boldHTML(name);
        }

        String internalString = Terrains.getName(terrainType) + ":" + level;
        String tfString = (tf > 0) ? "(TF: " + tf + ")" : "";
        String exitsString = "";
        if ((exits > 0) && Terrains.exitableTerrain(terrainType)) {
            exitsString = "(";
            exitsString += terrain.hasExitsSpecified() ? "Exits: " : "Auto Exits: ";
            exitsString += String.join(", ", exitsAsString(exits)) + ")";
        }
        if ((exits > 0) && terrain.hasExitsSpecified()) {
            internalString += ":" + exits;
        }
        List<String> lineContents = new ArrayList<>();
        lineContents.add(name);
        if (!tfString.isBlank()) {
            lineContents.add(tfString);
        }
        if (!exitsString.isBlank()) {
            lineContents.add(exitsString);
        }
        lineContents.add("[" + internalString + "]");
        return lineContents;
    }

    private List<String> exitsAsString(int exits) {
        List<String> results = new ArrayList<>();
        if (exits == 63) {
            results.add("All");
            return results;
        } else {
            if ((exits & 1) != 0) {
                results.add("N");
            }
            if ((exits & 2) != 0) {
                results.add("NE");
            }
            if ((exits & 4) != 0) {
                results.add("SE");
            }
            if ((exits & 8) != 0) {
                results.add("S");
            }
            if ((exits & 16) != 0) {
                results.add("SW");
            }
            if ((exits & 32) != 0) {
                results.add("NW");
            }
        }
        return results;
    }

    private String boldHTML(String text) {
        return "<B>" + text + "</B>";
    }
}
