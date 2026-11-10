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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
          String attackerName, String targetName,
          LosRuleMode losRuleMode,
          boolean attackerHasMastMount, boolean targetHasMastMount,
          boolean attackerSpottingClear, boolean targetSpottingClear) {
        LosEffects losEffects = LosEffects.calculateLos(game, attackInfo);
        boolean losBlocked = !losEffects.canSee();

        return buildWithLosResult(game, attackInfo, losBlocked, losEffects.isBlockedByDeadZone(),
              losEffects.getSelectedSplitHexes(),
              attackerIsHullDown, targetIsHullDown,
              attackerUnitType, targetUnitType,
              attackerAtAltitude, targetAtAltitude,
              attackerName, targetName,
              losRuleMode,
              attackerHasMastMount, targetHasMastMount,
              attackerSpottingClear, targetSpottingClear);
    }

    /**
     * Builds diagram data with a pre-computed LOS blocked result. Use this when the LOS calculation was already
     * performed via the entity-based path (fire phase code), so the diagram doesn't re-compute with the manual
     * AttackInfo (which may produce different results). {@code losRuleMode} controls the per-hex comparison level
     * the diagram uses to flag blockers; pick it from the active game options via
     * {@link LosRuleMode#fromGameOptions(Game)}. {@code deadZone} comes from the engine's
     * {@link LosEffects#isBlockedByDeadZone()} flag and drives the dead-zone hatch overlay.
     * {@code selectedSplitHexes} identifies the split-path hexes selected by that LOS calculation.
     */
    public static LOSDiagramData buildWithLosResult(Game game, LosEffects.AttackInfo attackInfo,
          boolean losBlocked, boolean deadZone, List<Coords> selectedSplitHexes,
          boolean attackerIsHullDown, boolean targetIsHullDown,
          DiagramUnitType attackerUnitType, DiagramUnitType targetUnitType,
          boolean attackerAtAltitude, boolean targetAtAltitude,
          String attackerName, String targetName,
          LosRuleMode losRuleMode,
          boolean attackerHasMastMount, boolean targetHasMastMount,
          boolean attackerSpottingClear, boolean targetSpottingClear) {
        Board board = game.getBoard();
        Coords attackPos = attackInfo.attackPos;
        Coords targetPos = attackInfo.targetPos;

        // Get the default path to use when the LOS calculation exits before choosing a split side.
        List<Coords> normalPath = Coords.intervening(attackPos, targetPos);
        Set<Coords> normalPathSet = new HashSet<>(normalPath);

        // Get the split-aware path; each split pair must contribute exactly one diagram column.
        List<Coords> splitPath = Coords.intervening(attackPos, targetPos, true);
        Set<Coords> selectedSplitHexSet = new HashSet<>(selectedSplitHexes);

        List<Coords> pathCoords;
        Map<Coords, Coords> splitAlternates;
        if (attackPos.degree(targetPos) % 60 == 30) {
            pathCoords = new ArrayList<>();
            splitAlternates = new HashMap<>();
            appendSelectedSplitPath(splitPath, normalPathSet, selectedSplitHexSet, pathCoords, splitAlternates);
        } else {
            pathCoords = normalPath;
            splitAlternates = Map.of();
        }

        // Build one row per hex on the selected LOS path.
        List<HexRow> hexPath = new ArrayList<>();
        for (Coords coords : pathCoords) {
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
            boolean eruptingGeyser = getTerrainLevel(hex, Terrains.GEYSER) == Terrains.GEYSER_LVL_ACTIVE;

            // The LOS line elevation at this hex is the level the engine compares hex tops against,
            // expressed in BMM LOS-level units (= silhouette top = hex + twHeight). Standard and Dead Zone
            // use the BMM adjacency rule (step function); Diagrammed uses a smooth linear interpolation.
            double losLineElevation = calculateLosLineElevation(
                  attackInfo, coords, attackPos, targetPos, losRuleMode);

            // Solid terrain (ground + building) blocks LOS when its top reaches the line. The >=
            // matches BMM ("equal to or higher than") for Standard/Dead Zone and the engine's >=
            // against (1 + interp(absHeight)) for Diagrammed. Attacker and target hexes never count
            // as intervening terrain.
            int solidTerrainHeight = groundElevation + buildingHeight;
            boolean blocksLos = solidTerrainHeight >= losLineElevation
                  && !coords.equals(attackPos)
                  && !coords.equals(targetPos);

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
                  eruptingGeyser,
                  splitAlternates.containsKey(coords),
                  splitAlternates.get(coords),
                  true,
                  blocksLos,
                  losLineElevation
            ));
        }

        // The dead-zone "victim" is whichever endpoint sits at the lower absHeight - that's the unit
        // inside the shadow cast by the tallest intervening hill. Tied: pick the target arbitrarily.
        Coords deadZoneVictimPos = null;
        if (deadZone) {
            deadZoneVictimPos = attackInfo.attackAbsHeight < attackInfo.targetAbsHeight
                  ? attackPos
                  : targetPos;
        }

        // attackerAbsHeight / targetAbsHeight stored on the data are the BMM LOS levels (= hex + twHeight),
        // which is also the silhouette top in y-coords. The engine's internal absHeight is one less; the +1
        // converts back to BMM units for the diagram.
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
              targetName,
              losRuleMode,
              deadZone,
              deadZoneVictimPos,
              attackerHasMastMount,
              targetHasMastMount,
              attackerSpottingClear,
              targetSpottingClear
        );
    }

    /**
     * Returns the LOS line's reference level at a given hex, in BMM LOS-level units (= hex + twHeight). Standard
     * and Dead Zone use the BMM adjacency rule, which is a step function: the comparison level is the
     * attacker-adjacent unit's LOS for hexes adjacent to the attacker, the target-adjacent unit's LOS for hexes
     * adjacent to the target, the lower of the two for hexes adjacent to both (only possible at distance 2), and
     * the higher of the two for non-adjacent intermediate hexes. Diagrammed uses a smooth linear interpolation
     * matching {@code LosEffects.losElevation = 1 + interp(absHeight)} at line 1404.
     */
    private static double calculateLosLineElevation(LosEffects.AttackInfo attackInfo,
          Coords coords, Coords attackPos, Coords targetPos, LosRuleMode mode) {
        double attLos = attackInfo.attackAbsHeight + 1;
        double tgtLos = attackInfo.targetAbsHeight + 1;

        if (coords.equals(attackPos)) {
            return attLos;
        }
        if (coords.equals(targetPos)) {
            return tgtLos;
        }

        int distFromAttacker = attackPos.distance(coords);
        int distFromTarget = targetPos.distance(coords);
        int totalDistance = distFromAttacker + distFromTarget;

        if (totalDistance == 0) {
            return attLos;
        }

        if (mode == LosRuleMode.DIAGRAMMED) {
            // TacOps Diagrammed LOS: linear interp between LOS levels at the unit positions.
            // weightedHeight is the engine's (absHeight*distFromOpposite) form; +1 of total/totalDistance
            // shifts the result into BMM LOS-level units to match attLos / tgtLos endpoints.
            double weightedHeight = (attackInfo.targetAbsHeight * distFromAttacker)
                  + (attackInfo.attackAbsHeight * distFromTarget);
            return 1 + weightedHeight / totalDistance;
        }

        // STANDARD or DEAD_ZONE: BMM adjacency rule.
        boolean adjAttacker = distFromAttacker == 1;
        boolean adjTarget = distFromTarget == 1;
        if (adjAttacker && adjTarget) {
            // Distance-2 case: hex is adjacent to both ends. BMM says it intervenes if it reaches either
            // unit's LOS level, so the lower level is the binding threshold.
            return Math.min(attLos, tgtLos);
        }
        if (adjAttacker) {
            return attLos;
        }
        if (adjTarget) {
            return tgtLos;
        }
        return Math.max(attLos, tgtLos);
    }

    /**
     * Builds the selected split-aware LOS path, omitting the unselected hex from each split pair.
     */
    private static void appendSelectedSplitPath(List<Coords> splitPath, Set<Coords> normalPathSet,
          Set<Coords> selectedSplitHexSet, List<Coords> pathCoords, Map<Coords, Coords> splitAlternates) {
        if (splitPath.isEmpty()) {
            return;
        }

        pathCoords.add(splitPath.getFirst());
        for (int i = 1; i < splitPath.size() - 2; i += 3) {
            Coords left = splitPath.get(i);
            Coords right = splitPath.get(i + 1);
            Coords selected = selectedSplitHexSet.contains(left) ? left
                  : selectedSplitHexSet.contains(right) ? right
                  : normalPathSet.contains(left) ? left : right;
            Coords alternate = selected.equals(left) ? right : left;
            pathCoords.add(selected);
            splitAlternates.put(selected, alternate);

            int nextPathIndex = i + 2;
            if (nextPathIndex < splitPath.size() - 1) {
                pathCoords.add(splitPath.get(nextPathIndex));
            }
        }

        Coords target = splitPath.getLast();
        if (!pathCoords.getLast().equals(target)) {
            pathCoords.add(target);
        }
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
