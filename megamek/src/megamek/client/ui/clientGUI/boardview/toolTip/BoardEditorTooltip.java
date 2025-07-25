/*
 * Copyright (C) 2024, 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.toolTip;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;

import java.util.*;
import java.awt.*;
import java.util.List;

import static megamek.client.ui.util.UIUtil.*;

/**
 * This class is a {@link BoardViewTooltipProvider} that is tailored
 * to the Board Editor and lists detailed terrain info about the hovered hex.
 */
public class BoardEditorTooltip implements BoardViewTooltipProvider {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** Tooltip vertical spacer size */
    private static final int BASE_PADDING = 8;

    private static final String GRAYED_DOT_SPACER = "<FONT "
            + UIUtil.colorString(GUIP.getToolTipLightFGColor()) + ">" + DOT_SPACER + "</FONT>";

    /** Auxiliary terrains are listed with an indentation. */
    private static final List<Integer> AUXILIARY_TERRAINS = List.of(Terrains.BLDG_CF, Terrains.BLDG_ELEV,
            Terrains.BLDG_BASEMENT_TYPE, Terrains.BLDG_ARMOR, Terrains.BLDG_CLASS, Terrains.BLDG_BASE_COLLAPSED,
            Terrains.BRIDGE_CF, Terrains.BRIDGE_ELEV, Terrains.FUEL_TANK_CF, Terrains.FUEL_TANK_ELEV,
            Terrains.FUEL_TANK_MAGN, Terrains.BLDG_FLUFF, Terrains.ROAD_FLUFF, Terrains.WATER_FLUFF,
            Terrains.FOLIAGE_ELEV);

    private final BoardView bv;

    public BoardEditorTooltip(BoardView boardView) {
        bv = Objects.requireNonNull(boardView);
    }

    @Override
    public String getTooltip(Point point, Coords movementTarget) {
        final Coords coords = bv.getCoordsAt(point);
        Board board = bv.getBoard();
        Hex hex = board.getHex(coords);
        if (hex == null) {
            return "Error: No hex found at " + coords;
        }

        int padding = UIUtil.scaleForGUI(BASE_PADDING);

        StringBuilder result = new StringBuilder();
        result.append(UIUtil.fontHTML(GUIP.getUnitToolTipTerrainFGColor()));
        result.append("<FONT FACE=").append(FontHandler.notoFont().getName()).append(">");

        // Coordinates and level
        result.append(colorHTML("Hex: ", GUIP.getToolTipLightFGColor()))
                .append(hex.getCoords())
                .append(GRAYED_DOT_SPACER)
                .append(colorHTML("Level: ", GUIP.getToolTipLightFGColor()))
                .append(hex.getLevel());

        // Theme
        if (!StringUtility.isNullOrBlank(hex.getTheme())) {
            result.append(paragraphHTMLOpen(padding))
                    .append(colorHTML("Theme: ", GUIP.getToolTipLightFGColor()))
                    .append(colorHTML(hex.getTheme(), GUIP.getUnitToolTipHighlightColor()))
                    .append("</p>");
        }

        // The terrain and auto terrain lines
        List<String> terrainLines = new ArrayList<>();
        List<String> autoTerrainLines = new ArrayList<>();
        List<Integer> terrains = Arrays.stream(hex.getTerrainTypes()).boxed().sorted(terrainSorter).toList();
        for (int type: terrains) {
            String line = String.join(GRAYED_DOT_SPACER, createLine(hex, type));
            (Terrains.AUTOMATIC.contains(type) ? autoTerrainLines : terrainLines).add(line);
        }

        result.append(paragraphHTMLOpen(padding));
        if (terrainLines.isEmpty()) {
            result.append(colorHTML("Clear", GUIP.getUnitToolTipHighlightColor()));
        } else {
            result.append(String.join("<BR>", terrainLines));
        }
        result.append("</p>");

        if (!autoTerrainLines.isEmpty()) {
            result.append(paragraphHTMLOpen(padding))
                    .append(colorHTMLOpen(GUIP.getToolTipLightFGColor()))
                    .append("Automated Terrains:<BR>")
                    .append(String.join("<BR>", autoTerrainLines))
                    .append("</FONT>")
                    .append("</p>");
        }

        // Invalid hex notification
        List<String> errors = new ArrayList<>();
        if (!hex.isValid(errors)) {
            result.append(paragraphHTMLOpen(padding))
                    .append(UIUtil.fontHTML(GUIP.getWarningColor())).append(UIUtil.WARNING_SIGN).append("</FONT>")
                    .append(Messages.getString("BoardView1.invalidHex")).append("<BR>")
                    .append(String.join("<BR>", errors))
                    .append("</p>");
        }

        result.append("</FONT></FONT>");
        return "<HTML><BODY style=padding:8; BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipTerrainBGColor()) + ">"
                + result + "</BODY></HTML>";
    }

    /** @return The information pieces of a tooltip line (terrain name, TF etc.) as a list. */
    private List<String> createLine(Hex hex, int terrainType) {
        Terrain terrain = hex.getTerrain(terrainType);
        List<String> lineContents = new ArrayList<>();
        lineContents.add(terrainNameLineEntry(terrain));

        int tf = terrain.getTerrainFactor();
        String tfString = (tf > 0) ? "TF: " + tf : "";
        if (!tfString.isBlank()) {
            lineContents.add(tfString);
        }

        String exitsString = exitsLineEntry(terrain);
        if (!exitsString.isBlank()) {
            lineContents.add(exitsString);
        }

        lineContents.add(terrainCodeLineEntry(terrain));
        return lineContents;
    }

    private String terrainNameLineEntry(Terrain terrain) {
        String terrainName = Terrains.getDisplayName(terrain.getType(), terrain.getLevel());
        if (terrainName == null) {
            terrainName = Terrains.getEditorName(terrain.getType());
        }

        if (terrain.getType() == Terrains.DEPLOYMENT_ZONE) {
            // Show the real zones because they're coded into the exits value
            List<Integer> zones = Board.exitsAsIntList(terrain.getExits());
            List<String> zonesAsString = zones.stream().map(String::valueOf).toList();
            if (!zones.isEmpty()) {
                terrainName += " (%s)".formatted(String.join(", ", zonesAsString));
            }
        }

        if (!Terrains.AUTOMATIC.contains(terrain.getType())) {
            terrainName = colorHTML(terrainName, GUIP.getUnitToolTipHighlightColor());
        }

        if (isAuxiliary(terrain.getType())) {
            terrainName = "&nbsp;&nbsp;&nbsp;" + terrainName;
        }
        return terrainName;
    }

    private String terrainCodeLineEntry(Terrain terrain) {
        String internalString = Terrains.getName(terrain.getType()) + ":" + terrain.getLevel();
        if ((terrain.getExits() > 0) && terrain.hasExitsSpecified()) {
            internalString += ":" + terrain.getExits();
        }
        internalString = colorHTML(internalString, GUIP.getToolTipLightFGColor());
        return internalString;
    }

    private String exitsLineEntry(Terrain terrain) {
        String exitsString = "";
        if ((terrain.getExits() > 0) && Terrains.exitableTerrain(terrain.getType())) {
            exitsString = terrain.hasExitsSpecified() ? "Exits: " : "Auto Exits: ";
            exitsString += String.join("+", exitsAsString(terrain.getExits()));
        }
        return exitsString;
    }

    /** @return the exits as a list of "N", "SE" etc. or "All" (exits 63) or an empty list (exits 0). */
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

    private String paragraphHTMLOpen(int padding) {
        return "<p style=padding-top:" + padding + ">";
    }

    private String colorHTML(String text, Color color) {
        return colorHTMLOpen(color) + text + "</FONT>";
    }

    private String colorHTMLOpen(Color color) {
        return String.format("<FONT %s>", UIUtil.colorString(color));
    }

    /**
     * Sorts building and road fluff directly under building and road, resp.
     * Sorts jungle and woods together and foliage elevation directly under woods/jungle
     */
    private final Comparator<Integer> terrainSorter = (o1, o2) -> {
        if (o1 == Terrains.FOLIAGE_ELEV) {
            return (o2 == Terrains.WOODS || o2 == Terrains.JUNGLE) ? 1 : Integer.compare(Terrains.WOODS, o2);
        } else if (o2 == Terrains.FOLIAGE_ELEV) {
            return (o1 == Terrains.WOODS || o1 == Terrains.JUNGLE) ? -1 : Integer.compare(o1, Terrains.WOODS);
        } else if (o1 == Terrains.ROAD_FLUFF) {
            return o2 == Terrains.ROAD ? 1 : Integer.compare(Terrains.ROAD, o2);
        } else if (o2 == Terrains.ROAD_FLUFF) {
            return o1 == Terrains.ROAD ? -1 : Integer.compare(o1, Terrains.ROAD);
        } else if (o1 == Terrains.WOODS) {
            return o2 == Terrains.JUNGLE ? 1 : Integer.compare(Terrains.JUNGLE, o2);
        } else if (o2 == Terrains.WOODS) {
            return o1 == Terrains.JUNGLE ? -1 : Integer.compare(o1, Terrains.JUNGLE);
        } else if (o1 == Terrains.BLDG_FLUFF) {
            return o2 == Terrains.BUILDING ? 1 : Integer.compare(Terrains.BUILDING, o2);
        } else if (o2 == Terrains.BLDG_FLUFF) {
            return o1 == Terrains.BUILDING ? -1 : Integer.compare(o1, Terrains.BUILDING);
        } else {
            return Integer.compare(o1, o2);
        }
    };

    private boolean isAuxiliary(int terrainType) {
        return AUXILIARY_TERRAINS.contains(terrainType);
    }
}
