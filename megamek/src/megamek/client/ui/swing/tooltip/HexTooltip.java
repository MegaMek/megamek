/*
 * MegaMek - Copyright (C) 2023 - The MegaMek Team
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

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.BasementType;

import java.util.Vector;

import static megamek.client.ui.swing.tooltip.TipUtil.BUILDING_BGCOLOR;
import static megamek.client.ui.swing.tooltip.TipUtil.LIGHT_BGCOLOR;
import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiBlack;

public final class HexTooltip {

    public static String getHexTip(Hex mhex, @Nullable ClientGUI clientGUI) {
        StringBuilder result = new StringBuilder();
        Coords mcoords = mhex.getCoords();
        // All of the following can be null even if there's a ClientGUI!
        Client client = (clientGUI != null) ? clientGUI.getClient() : null;
        Game game = (client != null) ? client.getGame() : null;
        Player localPlayer = (client != null) ? client.getLocalPlayer() : null;

        // Fuel Tank
        if (mhex.containsTerrain(Terrains.FUEL_TANK)) {
            String sFuelTank = "";
            // In at least the BoardEditor and lobby map preview, buildings have no entry in the
            // buildings list of the board, so get the info from the hex
            if (game == null) {
                sFuelTank = Messages.getString("BoardView1.Tooltip.FuelTank",
                        mhex.terrainLevel(Terrains.FUEL_TANK_ELEV),
                        Terrains.getEditorName(Terrains.FUEL_TANK),
                        mhex.terrainLevel(Terrains.FUEL_TANK_CF),
                        mhex.terrainLevel(Terrains.FUEL_TANK_MAGN));
            } else {
                FuelTank bldg = (FuelTank) game.getBoard().getBuildingAt(mcoords);
                sFuelTank = Messages.getString("BoardView1.Tooltip.FuelTank",
                        mhex.terrainLevel(Terrains.FUEL_TANK_ELEV),
                        bldg.toString(),
                        bldg.getCurrentCF(mcoords),
                        bldg.getMagnitude());
            }

            sFuelTank = guiScaledFontHTML(uiBlack()) + sFuelTank + "</FONT>";
            String col = "<TD>" + sFuelTank + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE BORDER=0 BGCOLOR=" + LIGHT_BGCOLOR + " width=100%>" + row + "</TABLE>";
            result.append(table);
        }

        // Building
        if (mhex.containsTerrain(Terrains.BUILDING)) {
            String sBuilding;
            // In at least the BoardEditor and lobby map preview, buildings have no entry in the
            // buildings list of the board, so get the info from the hex
            if (game == null) {
                sBuilding = Messages.getString("BoardView1.Tooltip.Building",
                        mhex.terrainLevel(Terrains.BLDG_ELEV),
                        Terrains.getEditorName(Terrains.BUILDING),
                        mhex.terrainLevel(Terrains.BLDG_CF),
                        Math.max(mhex.terrainLevel(Terrains.BLDG_ARMOR), 0),
                        BasementType.getType(mhex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).toString());
                sBuilding = guiScaledFontHTML(uiBlack()) + sBuilding + "</FONT>";
                String col = "<TD>" + sBuilding + "</TD>";
                String row = "<TR>" + col + "</TR>";
                String table = "<TABLE BORDER=0 BGCOLOR=" + LIGHT_BGCOLOR + " width=100%>" + row + "</TABLE>";
                result.append(table);
            } else {
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                sBuilding = Messages.getString("BoardView1.Tooltip.Building",
                        mhex.terrainLevel(Terrains.BLDG_ELEV),
                        bldg.toString(),
                        bldg.getCurrentCF(mcoords),
                        bldg.getArmor(mcoords),
                        bldg.getBasement(mcoords).toString());

                if (bldg.getBasementCollapsed(mcoords)) {
                    sBuilding += Messages.getString("BoardView1.Tooltip.BldgBasementCollapsed");
                }
                sBuilding = guiScaledFontHTML(uiBlack()) + sBuilding + "</FONT>";
                String col = "<TD>" + sBuilding + "</TD>";
                String row = "<TR>" + col + "</TR>";
                String table = "<TABLE BORDER=0 BGCOLOR=" + BUILDING_BGCOLOR + " width=100%>" + row + "</TABLE>";
                result.append(table);
            }
        }

        // Bridge
        if (mhex.containsTerrain(Terrains.BRIDGE)) {
            String sBridge;
            // In at least the BoardEditor and lobby map preview, buildings have no entry in the
            // buildings list of the board, so get the info from the hex
            if (game == null) {
                sBridge = Messages.getString("BoardView1.Tooltip.Bridge",
                        mhex.terrainLevel(Terrains.BRIDGE_ELEV),
                        Terrains.getEditorName(Terrains.BRIDGE),
                        mhex.terrainLevel(Terrains.BRIDGE_CF));
            } else {
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                sBridge = Messages.getString("BoardView1.Tooltip.Bridge",
                        mhex.terrainLevel(Terrains.BRIDGE_ELEV),
                        bldg.toString(),
                        bldg.getCurrentCF(mcoords));
            }
            sBridge = guiScaledFontHTML(uiBlack()) + sBridge + "</FONT>";
            String col = "<TD>" + sBridge + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE BORDER=0 BGCOLOR=" + LIGHT_BGCOLOR + " width=100%>" + row + "</TABLE>";
            result.append(table);
        }

        if ((game != null) && game.containsMinefield(mcoords)) {
            Vector<Minefield> minefields = game.getMinefields(mcoords);
            for (int i = 0; i < minefields.size(); i++) {
                Minefield mf = minefields.elementAt(i);
                String owner = " (" + game.getPlayer(mf.getPlayerId()).getName() + ')';
                String sMinefield = mf.getName() + ' ' + Messages.getString("BoardView1.minefield") + " (" + mf.getDensity() + ')';

                switch (mf.getType()) {
                    case Minefield.TYPE_CONVENTIONAL:
                    case Minefield.TYPE_COMMAND_DETONATED:
                    case Minefield.TYPE_ACTIVE:
                    case Minefield.TYPE_INFERNO:
                        sMinefield += ' ' + owner;
                        break;
                    case Minefield.TYPE_VIBRABOMB:
                        if (mf.getPlayerId() == localPlayer.getId()) {
                            sMinefield += "(" + mf.getSetting() + ") " + owner;
                        } else {
                            sMinefield += owner;
                        }
                        break;
                    default:
                        break;
                }

                sMinefield = guiScaledFontHTML(UIUtil.uiWhite()) + sMinefield + "</FONT>";
                result.append(sMinefield);
                result.append("<BR>");
            }
        }
        return result.toString();
    }

    public static String getBuildingTargetTip(BuildingTarget target, Board board) {
        String result = "";
        Coords mcoords = target.getPosition();
        Building bldg = board.getBuildingAt(mcoords);
        Hex mhex = board.getHex(mcoords);
        String sBuilding = Messages.getString("BoardView1.Tooltip.Building",
                mhex.terrainLevel(Terrains.BLDG_ELEV), bldg.toString(), bldg.getCurrentCF(mcoords),
                bldg.getArmor(mcoords), bldg.getBasement(mcoords).toString());

        if (bldg.getBasementCollapsed(mcoords)) {
            sBuilding += Messages.getString("BoardView1.Tooltip.BldgBasementCollapsed");
        }
        sBuilding = guiScaledFontHTML(uiBlack()) + sBuilding + "</FONT>";
        String col = "<TD>" + sBuilding + "</TD>";
        String row = "<TR>" + col + "</TR>";
        String table = "<TABLE BORDER=0 BGCOLOR=" + BUILDING_BGCOLOR + " width=100%>" + row + "</TABLE>";
        result += table;
        return result;
    }

    public static String getOneLineSummary(BuildingTarget target, Board board) {
        String result = "";
        Coords mcoords = target.getPosition();
        Building bldg = board.getBuildingAt(mcoords);
        Hex mhex = board.getHex(mcoords);
        result += Messages.getString("BoardView1.Tooltip.BuildingLine", mhex.terrainLevel(Terrains.BLDG_ELEV), bldg.getCurrentCF(mcoords), bldg.getArmor(mcoords));
        return result;
    }

    public static String getTerrainTip(Hex mhex)
    {
        Coords mcoords = mhex.getCoords();
        String result = "";
        StringBuilder sTerrain = new StringBuilder(Messages.getString("BoardView1.Tooltip.Hex", mcoords.getBoardNum(), mhex.getLevel()) + "<BR>");

        // cycle through the terrains and report types found
        for (int terType: mhex.getTerrainTypes()) {
            int tf = mhex.getTerrain(terType).getTerrainFactor();
            int ttl = mhex.getTerrain(terType).getLevel();
            String name = Terrains.getDisplayName(terType, ttl);
            if (name != null) {
                String msg_tf =  Messages.getString("BoardView1.Tooltip.TF");
                name += (tf > 0) ? " (" + msg_tf + ": " + tf + ')' : "";
                sTerrain.append(name).append("<BR>");
            }
        }

        result += guiScaledFontHTML(UIUtil.uiBlack()) + sTerrain + "</FONT>";
        return result;
    }

    private HexTooltip() { }
}