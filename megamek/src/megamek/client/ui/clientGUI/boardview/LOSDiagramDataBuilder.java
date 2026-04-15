/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.ui.clientGUI.boardview.LOSDiagramData.HexRow;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;

/**
 * Builds {@link LOSDiagramData} from game state and attack information. Extracts the intervening hex path and terrain
 * data needed for the elevation diagram.
 */
final class LOSDiagramDataBuilder {

    private LOSDiagramDataBuilder() {
        // utility class
    }

    /**
     * Builds the diagram data for a LOS path between attacker and target.
     *
     * @param game               the current game state
     * @param attackInfo         the attack information containing positions and heights
     * @param attackerIsHullDown whether the attacker is hull-down (TW p.100)
     * @param targetIsHullDown   whether the target is hull-down (TW p.100)
     * @param attackerUnitType   the attacker's unit type for silhouette rendering
     * @param targetUnitType     the target's unit type for silhouette rendering
     * @param attackerName       display name of the attacker entity, or empty if none
     * @param targetName         display name of the target entity, or empty if none
     *
     * @return the diagram data ready for rendering
     */
    public static LOSDiagramData build(Game game, LosEffects.AttackInfo attackInfo,
          boolean attackerIsHullDown, boolean targetIsHullDown,
          DiagramUnitType attackerUnitType, DiagramUnitType targetUnitType,
          boolean attackerAtAltitude, boolean targetAtAltitude,
          String attackerName, String targetName) {
        LosEffects losEffects = LosEffects.calculateLos(game, attackInfo);
        boolean losBlocked = !losEffects.canSee();

        return buildWithLosResult(game, attackInfo, losBlocked,
              attackerIsHullDown, targetIsHullDown,
              attackerUnitType, targetUnitType,
              attackerAtAltitude, targetAtAltitude,
              attackerName, targetName);
    }

    /**
     * Builds diagram data with a pre-computed LOS blocked result. Use this when the LOS calculation was already
     * performed via the entity-based path (fire phase code), so the diagram doesn't re-compute with the manual
     * AttackInfo (which may produce different results).
     */
    public static LOSDiagramData buildWithLosResult(Game game, LosEffects.AttackInfo attackInfo,
          boolean losBlocked,
          boolean attackerIsHullDown, boolean targetIsHullDown,
          DiagramUnitType attackerUnitType, DiagramUnitType targetUnitType,
          boolean attackerAtAltitude, boolean targetAtAltitude,
          String attackerName, String targetName) {
        Board board = game.getBoard();
        Coords attackPos = attackInfo.attackPos;
        Coords targetPos = attackInfo.targetPos;

        // Get the non-split path to identify which hexes are "normal"
        List<Coords> normalPath = Coords.intervening(attackPos, targetPos);
        Set<Coords> normalPathSet = new HashSet<>(normalPath);

        // Get the split-aware path to detect hex-edge LOS
        List<Coords> splitPath = Coords.intervening(attackPos, targetPos, true);

        // Identify split hex pairs: hexes in the split path not in the normal path
        Set<Coords> splitHexCoords = new HashSet<>();
        for (Coords coord : splitPath) {
            if (!normalPathSet.contains(coord)) {
                splitHexCoords.add(coord);
            }
        }

        // Build hex row data for each hex in the normal path
        List<HexRow> hexPath = new ArrayList<>();
        for (int i = 0; i < normalPath.size(); i++) {
            Coords coords = normalPath.get(i);
            Hex hex = board.getHex(coords);
            if (hex == null) {
                continue;
            }

            int groundElevation = hex.getLevel();
            int buildingHeight = getBuildingHeight(hex);
            int woodsHeight = getWoodsHeight(hex);
            int woodsLevel = getTerrainLevel(hex, Terrains.WOODS);
            int jungleLevel = getTerrainLevel(hex, Terrains.JUNGLE);
            int waterDepth = getWaterDepth(hex);
            int smokeLevel = getTerrainLevel(hex, Terrains.SMOKE);
            int industrialHeight = getIndustrialHeight(hex);
            boolean hasScreen = hex.containsTerrain(Terrains.SCREEN);
            boolean hasFields = hex.containsTerrain(Terrains.FIELDS);
            boolean hasFire = hex.containsTerrain(Terrains.FIRE);

            // Calculate the interpolated LOS line elevation at this hex
            double losLineElevation = calculateLosLineElevation(
                  attackInfo, coords, attackPos, targetPos);

            // Determine if this hex's solid terrain (ground + building) blocks LOS.
            // Woods, smoke, and other soft terrain add modifiers but don't block
            // by elevation alone - only accumulated intervening counts block.
            int solidTerrainHeight = groundElevation + buildingHeight;
            boolean blocksLos = solidTerrainHeight >= losLineElevation
                  && !coords.equals(attackPos)
                  && !coords.equals(targetPos);

            // Check if this hex has a split alternate
            boolean isSplitHex = false;
            Coords splitAlternate = null;
            if (i > 0 && i < normalPath.size() - 1) {
                // Look for a split hex that would pair with this position
                for (Coords splitCoord : splitHexCoords) {
                    if (isAdjacentAlongPath(coords, splitCoord, attackPos, targetPos)) {
                        isSplitHex = true;
                        splitAlternate = splitCoord;
                        break;
                    }
                }
            }

            hexPath.add(new HexRow(
                  coords,
                  groundElevation,
                  buildingHeight,
                  woodsHeight,
                  woodsLevel,
                  jungleLevel,
                  waterDepth,
                  smokeLevel,
                  industrialHeight,
                  hasScreen,
                  hasFields,
                  hasFire,
                  isSplitHex,
                  splitAlternate,
                  blocksLos,
                  losLineElevation
            ));
        }

        // The + 1 converts from code's 0-indexed heights to TW unit heights,
        // matching the LosEffects interpolation formula (LosEffects line 1332)
        return new LOSDiagramData(
              List.copyOf(hexPath),
              attackInfo.attackAbsHeight + 1,
              attackInfo.targetAbsHeight + 1,
              attackPos,
              targetPos,
              losBlocked,
              attackerUnitType,
              targetUnitType,
              attackerIsHullDown,
              targetIsHullDown,
              attackerAtAltitude,
              targetAtAltitude,
              attackerName,
              targetName
        );
    }

    /**
     * Calculates the interpolated LOS line elevation at a given hex position. Uses the same weighted average formula as
     * {@link LosEffects}, including the {@code + 1} correction that converts from the code's 0-indexed unit heights to
     * TW's unit heights (e.g., Mek = hex level + 2, Vehicle = hex level + 1).
     *
     * @param attackInfo the attack info with absolute heights
     * @param coords     the hex to calculate for
     * @param attackPos  the attacker position
     * @param targetPos  the target position
     *
     * @return the interpolated LOS elevation at the given hex
     */
    private static double calculateLosLineElevation(LosEffects.AttackInfo attackInfo,
          Coords coords, Coords attackPos, Coords targetPos) {
        if (coords.equals(attackPos)) {
            return attackInfo.attackAbsHeight + 1;
        }
        if (coords.equals(targetPos)) {
            return attackInfo.targetAbsHeight + 1;
        }

        double distanceFromAttacker = attackPos.distance(coords);
        double distanceFromTarget = targetPos.distance(coords);
        double totalDistance = distanceFromAttacker + distanceFromTarget;

        if (totalDistance == 0) {
            return attackInfo.attackAbsHeight + 1;
        }

        // Weighted height interpolation matching LosEffects formula (line 1332)
        // The + 1 corrects from code's 0-indexed heights to TW unit heights
        double weightedHeight = (attackInfo.targetAbsHeight * distanceFromAttacker)
              + (attackInfo.attackAbsHeight * distanceFromTarget);
        return 1 + weightedHeight / totalDistance;
    }

    /**
     * Checks if two hexes are adjacent and both lie along the LOS path direction. Used to identify split hex pairs.
     */
    private static boolean isAdjacentAlongPath(Coords hexOnPath, Coords candidate,
          Coords attackPos, Coords targetPos) {
        if (hexOnPath.distance(candidate) != 1) {
            return false;
        }

        // The candidate should be at roughly the same distance along the path
        int hexDistFromAttacker = attackPos.distance(hexOnPath);
        int candidateDistFromAttacker = attackPos.distance(candidate);
        int hexDistFromTarget = targetPos.distance(hexOnPath);
        int candidateDistFromTarget = targetPos.distance(candidate);

        return Math.abs(hexDistFromAttacker - candidateDistFromAttacker) <= 1
              && Math.abs(hexDistFromTarget - candidateDistFromTarget) <= 1;
    }

    private static int getBuildingHeight(Hex hex) {
        if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
            return hex.terrainLevel(Terrains.BLDG_ELEV);
        }
        if (hex.containsTerrain(Terrains.FUEL_TANK_ELEV)) {
            return hex.terrainLevel(Terrains.FUEL_TANK_ELEV);
        }
        return 0;
    }

    private static int getWoodsHeight(Hex hex) {
        if (hex.containsTerrain(Terrains.FOLIAGE_ELEV)) {
            return hex.terrainLevel(Terrains.FOLIAGE_ELEV);
        }
        return 0;
    }

    private static int getWaterDepth(Hex hex) {
        if (hex.containsTerrain(Terrains.WATER)) {
            return hex.terrainLevel(Terrains.WATER);
        }
        return 0;
    }

    private static int getIndustrialHeight(Hex hex) {
        if (hex.containsTerrain(Terrains.INDUSTRIAL)) {
            return hex.terrainLevel(Terrains.INDUSTRIAL);
        }
        return 0;
    }

    private static int getTerrainLevel(Hex hex, int terrainType) {
        int level = hex.terrainLevel(terrainType);
        return (level == Terrain.LEVEL_NONE) ? 0 : level;
    }
}
