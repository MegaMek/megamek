/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.tooltip;

import static megamek.client.ui.util.UIUtil.DOT_SPACER;
import static megamek.client.ui.util.UIUtil.uiWhite;

import java.awt.Point;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FlareSprite;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.ReportMessages;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.BasementType;
import megamek.common.equipment.FuelTank;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Minefield;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.IlluminationLevel;
import megamek.common.units.BuildingTarget;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;


public final class HexTooltip {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public static String getHexTip(Hex mhex, @Nullable Client client, int boardId) {
        StringBuilder result = new StringBuilder();
        Coords mcoords = mhex.getCoords();
        // All of the following can be null even if there's a ClientGUI!
        Game game = (client != null) ? client.getGame() : null;
        Player localPlayer = (client != null) ? client.getLocalPlayer() : null;
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());

        // Fuel Tank
        if (mhex.containsTerrain(Terrains.FUEL_TANK)) {
            String sFuelTank;
            // In at least the BoardEditor and lobby map preview, buildings have no entry in the
            // buildings list of the board, so get the info from the hex
            if ((game == null) || (game.getBoard(boardId).getBuildingAt(mcoords) == null)) {
                sFuelTank = Messages.getString("BoardView1.Tooltip.FuelTank",
                      mhex.terrainLevel(Terrains.FUEL_TANK_ELEV),
                      Terrains.getEditorName(Terrains.FUEL_TANK),
                      mhex.terrainLevel(Terrains.FUEL_TANK_CF),
                      mhex.terrainLevel(Terrains.FUEL_TANK_MAGN));
            } else {
                FuelTank bldg = (FuelTank) game.getBoard(boardId).getBuildingAt(mcoords);
                sFuelTank = Messages.getString("BoardView1.Tooltip.FuelTank",
                      mhex.terrainLevel(Terrains.FUEL_TANK_ELEV),
                      bldg.toString(),
                      bldg.getCurrentCF(mcoords),
                      bldg.getMagnitude());
            }

            String attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getUnitToolTipBuildingFGColor()));
            sFuelTank = UIUtil.tag("FONT", attr, sFuelTank);
            sFuelTank = UIUtil.tag("span", fontSizeAttr, sFuelTank);
            String col = UIUtil.tag("TD", "", sFuelTank);
            String row = UIUtil.tag("TR", "", col);
            attr = String.format("CELLSPACING=0 CELLPADDING=0 BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipBuildingBGColor()));
            String table = UIUtil.tag("TABLE", attr, row);
            result.append(table);
        }

        // Building
        if (mhex.containsTerrain(Terrains.BUILDING)) {
            String sBuilding;
            // In at least the BoardEditor and lobby map preview, buildings have no entry in the
            // buildings list of the board, so get the info from the hex
            if ((game == null) || (game.getBoard(boardId).getBuildingAt(mcoords) == null)) {
                sBuilding = Messages.getString("BoardView1.Tooltip.Building",
                      mhex.terrainLevel(Terrains.BLDG_ELEV),
                      Terrains.getEditorName(Terrains.BUILDING),
                      mhex.terrainLevel(Terrains.BLDG_CF),
                      Math.max(mhex.terrainLevel(Terrains.BLDG_ARMOR), 0),
                      BasementType.getType(mhex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).toString());
            } else {
                IBuilding bldg = game.getBoard(boardId).getBuildingAt(mcoords);
                sBuilding = Messages.getString("BoardView1.Tooltip.Building",
                      mhex.terrainLevel(Terrains.BLDG_ELEV),
                      bldg.toString(),
                      bldg.getCurrentCF(mcoords),
                      bldg.getArmor(mcoords),
                      bldg.getBasement(mcoords).toString());

                if (bldg.getBasementCollapsed(mcoords)) {
                    sBuilding += Messages.getString("BoardView1.Tooltip.BldgBasementCollapsed");
                }
            }

            String attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getUnitToolTipBuildingFGColor()));
            sBuilding = UIUtil.tag("FONT", attr, sBuilding);
            sBuilding = UIUtil.tag("span", fontSizeAttr, sBuilding);
            String col = UIUtil.tag("TD", "", sBuilding);
            String row = UIUtil.tag("TR", "", col);
            attr = String.format("CELLSPACING=0 CELLPADDING=0 BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipBuildingBGColor()));
            String table = UIUtil.tag("TABLE", attr, row);
            result.append(table);
        }

        // Bridge
        if (mhex.containsTerrain(Terrains.BRIDGE)) {
            String sBridge;
            // In at least the BoardEditor and lobby map preview, buildings have no entry in the
            // buildings list of the board, so get the info from the hex
            if ((game == null) || (game.getBoard(boardId).getBuildingAt(mcoords) == null)) {
                sBridge = Messages.getString("BoardView1.Tooltip.Bridge",
                      mhex.terrainLevel(Terrains.BRIDGE_ELEV),
                      Terrains.getEditorName(Terrains.BRIDGE),
                      mhex.terrainLevel(Terrains.BRIDGE_CF));
            } else {
                IBuilding bldg = game.getBoard(boardId).getBuildingAt(mcoords);
                sBridge = Messages.getString("BoardView1.Tooltip.Bridge",
                      mhex.terrainLevel(Terrains.BRIDGE_ELEV),
                      bldg.toString(),
                      bldg.getCurrentCF(mcoords));
            }

            String attr = String.format("FACE=Dialog COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getUnitToolTipBuildingFGColor()));
            sBridge = UIUtil.tag("FONT", attr, sBridge);
            sBridge = UIUtil.tag("span", fontSizeAttr, sBridge);
            String col = UIUtil.tag("TD", "", sBridge);
            String row = UIUtil.tag("TR", "", col);
            attr = String.format("CELLSPACING=0 CELLPADDING=0 BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipBuildingBGColor()));
            String table = UIUtil.tag("TABLE", attr, row);
            result.append(table);
        }

        if ((game != null) && game.containsMinefield(mcoords)) {
            Vector<Minefield> minefields = game.getMinefields(mcoords);
            for (int i = 0; i < minefields.size(); i++) {
                Minefield mf = minefields.elementAt(i);
                Player owner = game.getPlayer(mf.getPlayerId());
                String ownerName = (owner != null) ?
                      " (" + owner.getName() + ')' :
                      ReportMessages.getString("BoardView1.Tooltip.unknownOwner");
                String sMinefield = mf.getName()
                      + ' '
                      + Messages.getString("BoardView1.minefield")
                      + " ("
                      + mf.getDensity()
                      + ')';

                switch (mf.getType()) {
                    case Minefield.TYPE_CONVENTIONAL:
                    case Minefield.TYPE_COMMAND_DETONATED:
                    case Minefield.TYPE_ACTIVE:
                    case Minefield.TYPE_INFERNO:
                        sMinefield += ' ' + ownerName;
                        break;
                    case Minefield.TYPE_VIBRABOMB:
                        if (mf.getPlayerId() == localPlayer.getId()) {
                            sMinefield += "(" + mf.getSetting() + ") " + ownerName;
                        } else {
                            sMinefield += ownerName;
                        }
                        break;
                    default:
                        break;
                }

                String attr = String.format("FACE=Dialog COLOR=%s",
                      UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
                sMinefield = UIUtil.tag("FONT", attr, sMinefield);
                result.append(sMinefield);
                result.append("<BR>");
            }
        }

        if ((game != null) && !game.getGroundObjects(mcoords).isEmpty()) {
            for (ICarryable groundObject : game.getGroundObjects(mcoords)) {
                result.append("&nbsp");
                String groundObj = groundObject.specificName();
                String attr = String.format("FACE=Dialog COLOR=%s",
                      UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
                groundObj = UIUtil.tag("FONT", attr, groundObj);
                result.append(groundObj);
                result.append("<BR/>");
            }
        }

        return result.toString();
    }

    public static String getBuildingTargetTip(BuildingTarget target, Board board) {
        if ((target == null) || (board == null)) {
            return "Error - Could not create building tooltip";
        }
        Coords mcoords = target.getPosition();
        IBuilding bldg = board.getBuildingAt(mcoords);
        Hex mhex = board.getHex(mcoords);
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());

        String sBuilding = Messages.getString("BoardView1.Tooltip.Building",
              mhex.terrainLevel(Terrains.BLDG_ELEV), bldg.toString(), bldg.getCurrentCF(mcoords),
              bldg.getArmor(mcoords), bldg.getBasement(mcoords).toString());

        if (bldg.getBasementCollapsed(mcoords)) {
            sBuilding += Messages.getString("BoardView1.Tooltip.BldgBasementCollapsed");
        }
        String attr = String.format("FACE=Dialog COLOR=%s",
              UIUtil.toColorHexString(GUIP.getUnitToolTipBuildingFGColor()));
        sBuilding = UIUtil.tag("FONT", attr, sBuilding);
        sBuilding = UIUtil.tag("span", fontSizeAttr, sBuilding);
        String col = UIUtil.tag("TD", "", sBuilding);
        String row = UIUtil.tag("TR", "", col);
        attr = String.format("CELLSPACING=0 CELLPADDING=0 BORDER=0 BGCOLOR=%s width=100%%",
              GUIPreferences.hexColor(GUIP.getUnitToolTipBuildingBGColor()));

        return UIUtil.tag("TABLE", attr, row);
    }

    public static String getOneLineSummary(BuildingTarget target, Board board) {
        String result = "";
        Coords mcoords = target.getPosition();
        IBuilding bldg = board.getBuildingAt(mcoords);
        Hex mhex = board.getHex(mcoords);
        result += Messages.getString("BoardView1.Tooltip.BuildingLine",
              mhex.terrainLevel(Terrains.BLDG_ELEV),
              bldg.getCurrentCF(mcoords),
              bldg.getArmor(mcoords));

        return result;
    }

    public static String getTerrainTip(Hex mhex, int boardId, Game game) {
        boolean inAtmosphere = game.getBoard(boardId).isLowAltitude();
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        Coords mcoords = mhex.getCoords();
        String indicator = IlluminationLevel.determineIlluminationLevel(game, boardId, mcoords).getIndicator();
        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getCautionColor()));
        String illuminated = UIUtil.tag("FONT", attr, " " + indicator);
        illuminated = DOT_SPACER + illuminated;

        String result;
        StringBuilder sTerrain = new StringBuilder(
              Messages.getString(
                    (inAtmosphere) ? "BoardView1.Tooltip.HexAlt" : "BoardView1.Tooltip.Hex",
                    mcoords.getBoardNum(),
                    mhex.getLevel()
              ) + illuminated + "<BR>"
        );
        // Types that represent Elevations need converting and possibly zeroing if board is in Atmosphere (Low Alt.)
        List<Integer> typesThatNeedAltitudeChecked = List.of(
              Terrains.INDUSTRIAL, Terrains.BLDG_ELEV, Terrains.BRIDGE_ELEV, Terrains.FOLIAGE_ELEV
        );

        // cycle through the terrains and report types found
        for (int terType : mhex.getTerrainTypes()) {
            int tf = mhex.getTerrain(terType).getTerrainFactor();
            int ttl = mhex.getTerrain(terType).getLevel();
            if (typesThatNeedAltitudeChecked.contains(terType)) {
                ttl = Terrains.getTerrainElevation(terType, ttl, inAtmosphere);
            }
            String name = Terrains.getDisplayName(terType, ttl);

            if (name != null) {
                String msg_tf = Messages.getString("BoardView1.Tooltip.TF");
                name += (tf > 0) ? " (" + msg_tf + ": " + tf + ')' : "";
                sTerrain.append(name).append("<BR>");
            }
        }

        attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipTerrainFGColor()));
        result = UIUtil.tag("FONT", attr, sTerrain.toString());
        result = UIUtil.tag("span", fontSizeAttr, result);

        return result;
    }

    public static String getDistanceTip(GUIPreferences GUIP, int distance) {
        String sTerrain = "";
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());

        if (distance == 1) {
            sTerrain += Messages.getString("BoardView1.Tooltip.Distance1");
        } else {
            sTerrain += Messages.getString("BoardView1.Tooltip.DistanceN", distance);
        }

        String attr = String.format("FACE=Dialog COLOR=%s",
              UIUtil.toColorHexString(GUIP.getUnitToolTipTerrainFGColor()));
        String result = UIUtil.tag("FONT", attr, sTerrain);
        result = UIUtil.tag("span", fontSizeAttr, result);

        return result;
    }

    public static String getSensorRangeTip(GUIPreferences GUIP, int distance, int minSensorRange, int maxSensorRange,
          int disPM, boolean isMovement) {
        String sTerrain = "";
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        sTerrain += "<BR>";
        if ((distance > minSensorRange) && (distance <= maxSensorRange)) {
            sTerrain += Messages.getString("BoardView1.Tooltip.SensorsHexInRange");
        } else {
            sTerrain += Messages.getString("BoardView1.Tooltip.SensorsHexNotInRange1");
            String tmp = Messages.getString("BoardView1.Tooltip.SensorsHexNotInRange2");

            String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getWarningColor()));
            sTerrain += UIUtil.tag("FONT", attr, tmp);
            sTerrain += Messages.getString("BoardView1.Tooltip.SensorsHexNotInRange3");
        }

        if (isMovement) {
            sTerrain += "<BR>";
            String sDistanceMove;
            if (disPM == 1) {
                sDistanceMove = Messages.getString("BoardView1.Tooltip.DistanceMove1");
            } else {
                sDistanceMove = Messages.getString("BoardView1.Tooltip.DistanceMoveN", disPM);
            }
            sTerrain += UIUtil.tag("I", "", sDistanceMove);
        }

        String attr = String.format("FACE=Dialog COLOR=%s",
              UIUtil.toColorHexString(GUIP.getUnitToolTipTerrainFGColor()));
        sTerrain = UIUtil.tag("FONT", attr, sTerrain);
        sTerrain = UIUtil.tag("span", fontSizeAttr, sTerrain);

        return sTerrain;
    }

    public static String getArtilleryHit(Game game, Coords coords, int boardId) {
        StringBuilder sArtilleryAutoHits = new StringBuilder();
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        boolean foundPlayer = false;
        for (Player player : game.getPlayersList()) {
            // loop through all players
            if (game.getBoard(boardId).isLegalDeployment(coords, player)) {
                if (!foundPlayer) {
                    foundPlayer = true;
                    sArtilleryAutoHits.append(Messages.getString("BoardView1.Tooltip.ArtyAutoHeader")).append("<BR>");
                }

                String sName = "&nbsp;&nbsp;" + player.getName();
                String attr = String.format("FACE=Dialog COLOR=%s",
                      UIUtil.toColorHexString(player.getColour().getColour()));
                sName = UIUtil.tag("FONT", attr, sName);
                sArtilleryAutoHits.append(UIUtil.tag("B", "", sName));
                sArtilleryAutoHits.append("<BR>");
            }
        }
        if (foundPlayer) {
            sArtilleryAutoHits.append("<BR>");
        }

        // Add a hint with keybind that the zones can be shown graphically
        String keybindText = KeyCommandBind.getDesc(KeyCommandBind.getBindByCmd("autoArtyDeployZone"));
        String msgArtyAutoHit = Messages.getString("BoardView1.Tooltip.ArtyAutoHint1") + "<BR>";
        msgArtyAutoHit += Messages.getString("BoardView1.Tooltip.ArtyAutoHint2") + "<BR>";
        msgArtyAutoHit += Messages.getString("BoardView1.Tooltip.ArtyAutoHint3", keybindText);
        sArtilleryAutoHits = new StringBuilder(UIUtil.tag("I", "", msgArtyAutoHit));

        String attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
        sArtilleryAutoHits = new StringBuilder(UIUtil.tag("FONT", attr, sArtilleryAutoHits.toString()));

        attr = String.format("FACE=Dialog COLOR=%s", UIUtil.toColorHexString(uiWhite()));
        sArtilleryAutoHits = new StringBuilder(UIUtil.tag("FONT", attr, sArtilleryAutoHits.toString()));
        sArtilleryAutoHits = new StringBuilder(UIUtil.tag("span", fontSizeAttr, sArtilleryAutoHits.toString()));
        String col = UIUtil.tag("TD", "", sArtilleryAutoHits.toString());
        String row = UIUtil.tag("TR", "", col);
        attr = String.format("CELLSPACING=0 CELLPADDING=0 BORDER=0 BGCOLOR=%s width=100%%",
              GUIPreferences.hexColor(GUIP.getUnitToolTipBGColor()));

        return UIUtil.tag("TABLE", attr, row);
    }

    public static String getFlares(GUIPreferences GUIP, BoardView bv, Point point) {
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());

        String result = bv.getAllSprites().stream()
              .filter(sprite -> sprite instanceof FlareSprite)
              .filter(sprite -> sprite.isInside(point))
              .map(Sprite::getTooltip)
              .collect(Collectors.joining());

        result = UIUtil.tag("span", fontSizeAttr, result);

        return result;
    }

    public static String getWrecks(GUIPreferences GUIP, BoardView bv, Coords coords) {
        String result = "";
        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        var wreckList = bv.useIsometric() ? bv.getIsoWreckSprites() : bv.getWreckSprites();
        for (var wSprite : wreckList) {
            if (wSprite.getPosition().equals(coords)) {
                String sWreck = wSprite.getTooltip().toString();
                String attr = String.format("FACE=Dialog COLOR=%s",
                      UIUtil.toColorHexString(GUIP.getUnitToolTipAltFGColor()));
                sWreck = UIUtil.tag("FONT", attr, sWreck);
                sWreck = UIUtil.tag("span", fontSizeAttr, sWreck);
                String col = UIUtil.tag("TD", "", sWreck);
                String row = UIUtil.tag("TR", "", col);
                String rows = row;

                if (!wSprite.getEntity().getCrew().isEjected()) {
                    String sPilot = PilotToolTip.getPilotTipShort(wSprite.getEntity(),
                          GUIP.getShowPilotPortraitTT(), false).toString();

                    attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString(GUIP.getUnitToolTipAltFGColor()));
                    sPilot = UIUtil.tag("FONT", attr, sPilot);
                    sPilot = UIUtil.tag("span", fontSizeAttr, sPilot);
                    col = UIUtil.tag("TD", "", sPilot);
                    row = UIUtil.tag("TR", "", col);
                    rows += row;
                }

                attr = String.format("CELLSPACING=0 CELLPADDING=0 BORDER=0 BGCOLOR=%s width=100%%",
                      GUIPreferences.hexColor(GUIP.getUnitToolTipAltBGColor()));
                result = UIUtil.tag("TABLE", attr, rows);
            }
        }

        return result;
    }

    private HexTooltip() {}
}
