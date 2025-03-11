/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */

package megamek.client.bot.common;

import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.common.Terrain;
import megamek.common.Terrains;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quick representation of the board for fast AI calculations.
 * This class is used to quickly access information about the board, such as terrain levels, cover, water, etc.
 * @author Luana Coppio
 */
public class BoardQuickRepresentation {

    private static final int NORMALIZED_THREAT_MAP_SIZE = 10;
    public static final int NORMALIZED_THREAT_HEATMAP = NORMALIZED_THREAT_MAP_SIZE * NORMALIZED_THREAT_MAP_SIZE;

    private final int[] boardTerrainLevels;
    private final int[] threatLevelDiscrete;
    private final double[] threatLevel;
    private final double[] alliedThreatLevel;
    private final double[] normalizedThreatLevel;
    private final double[] normalizedAlliedThreatLevel;
    private final BitSet hasWaterLevel;
    private final BitSet woodedTerrain;
    private final BitSet buildings;
    private final BitSet clearTerrain;
    private final BitSet hazardousTerrain;
    private final int boardHeight;
    private final int boardWidth;
    private final int numberOfHexes;
    private final boolean groundBoard;
    private static final int DECAY = 2;


    public BoardQuickRepresentation(Board board) {
        boardWidth = board.getWidth();
        boardHeight = board.getHeight();
        numberOfHexes = boardWidth * boardHeight;
        boardTerrainLevels = new int[numberOfHexes];
        threatLevelDiscrete = new int[numberOfHexes];
        normalizedThreatLevel = new double[NORMALIZED_THREAT_HEATMAP];
        normalizedAlliedThreatLevel = new double[NORMALIZED_THREAT_HEATMAP];
        threatLevel = new double[numberOfHexes];
        alliedThreatLevel = new double[numberOfHexes];
        hazardousTerrain = new BitSet(numberOfHexes);
        woodedTerrain = new BitSet(numberOfHexes);
        buildings = new BitSet(numberOfHexes);
        clearTerrain = new BitSet(numberOfHexes);
        hasWaterLevel = new BitSet(numberOfHexes);
        groundBoard = board.onGround();
        update(board);
    }

    public void update(Board board) {
        int idx;
        int level;
        int temp;
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                idx = y * board.getWidth() + x;
                Hex hex = board.getHex(x, y);
                if (hex == null) {
                    continue;
                }
                level = hex.floor();
                temp = hex.terrainLevel(Terrains.BUILDING);
                if (temp != Terrain.LEVEL_NONE) {
                    level += temp;
                    buildings.set(idx);
                }
                if (hex.containsTerrain(Terrains.WOODS)) {
                    woodedTerrain.set(idx);
                }
                if (hex.containsAnyTerrainOf(Terrains.HAZARDS)) {
                    hazardousTerrain.set(idx);
                }
                if (hex.isClearHex()) {
                    clearTerrain.set(idx);
                }
                if (hex.hasDepth1WaterOrDeeper()) {
                    hasWaterLevel.set(idx);
                }
                boardTerrainLevels[idx] = level;
            }
        }
    }

    public int levelAt(Coords position) {
        return boardTerrainLevels[getIndexFromCoordsClampedToBoard(position)];
    }

    public void updateThreatHeatmap(StructOfUnitArrays enemies, StructOfUnitArrays own) {
        updateThreatHeatmap(enemies, normalizedThreatLevel, threatLevelDiscrete, threatLevel);
        updateThreatHeatmap(own, normalizedAlliedThreatLevel, threatLevelDiscrete, alliedThreatLevel);
    }

    public void updateThreatHeatmap(StructOfUnitArrays units, double[] normalizedHeatmap, int[] discreteHeatmap,
                                    double[] threatHeatmap) {
        int width = boardWidth;
        int height = boardHeight;
        int maxValue = 0;
        int[][] xyr = units.getAllXYMaxRange();

        // first pass of heatmap decay
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                discreteHeatmap[y * width + x] = discreteHeatmap[y * width + x] / DECAY;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double maxThreat = getMaxThreat(xyr, x, y);
                double oldValue = discreteHeatmap[y * width + x];
                int newValue =  (int) Math.round(Math.max(oldValue, maxThreat));
                maxValue =  Math.max(maxValue, newValue);
                discreteHeatmap[y * width + x] = newValue;
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (maxValue == 0) {
                    threatHeatmap[y * width + x] = 0;
                } else {
                    double newValue = discreteHeatmap[y * width + x] / (double) maxValue;
                    threatHeatmap[y * width + x] = newValue;
                }
            }
        }
        resizeHeatmapBiLinear(threatHeatmap, normalizedHeatmap);
    }

    private void resizeHeatmapBiLinear(double[] threatHeatmap, double[] normalizedHeatmap) {
        Arrays.fill(normalizedHeatmap, 0.0);
        int newWidth = NORMALIZED_THREAT_MAP_SIZE;
        int newHeight = NORMALIZED_THREAT_MAP_SIZE;
        for (int newY = 0; newY < newHeight; newY++) {
            double oldY = (newY / (double) (newHeight - 1)) * (boardHeight - 1);
            int y1 = (int) Math.floor(oldY);
            int y2 = Math.min(y1 + 1, boardHeight - 1);
            double dy = oldY - y1;

            for (int newX = 0; newX < newWidth; newX++) {
                double oldX = (newX / (double) (newWidth - 1)) * (boardWidth - 1);
                int x1 = (int) Math.floor(oldX);
                int x2 = Math.min(x1 + 1, boardWidth - 1);
                double dx = oldX - x1;
                double topLeft = threatHeatmap[y1 * boardWidth + x1];
                double topRight = threatHeatmap[y1 * boardWidth + x2];
                double top = topLeft * (1 - dx) + topRight * dx;
                double bottomLeft = threatHeatmap[y2 * boardWidth + x1];
                double bottomRight = threatHeatmap[y2 * boardWidth + x2];
                double bottom = bottomLeft * (1 - dx) + bottomRight * dx;
                double value = top * (1 - dy) + bottom * dy;
                normalizedHeatmap[newY * newWidth + newX] = value;
            }
        }
    }

    public double[] getNormalizedThreatLevelHeatmap() {
        return normalizedThreatLevel;
    }

    public double[] getNormalizedAlliedThreatLevel() {
        return normalizedAlliedThreatLevel;
    }

    private double getMaxThreat(int[][] enemyUnitsXYMaxRange, int x, int y) {
        int enemyX;
        int enemyY;
        int enemyRange;
        double maxThreat = 0.0;
        for (int[] xyr : enemyUnitsXYMaxRange) {
            enemyX = xyr[0];
            enemyY = xyr[1];
            enemyRange = xyr[2];
            double dist = distance(x, y, enemyX, enemyY);
            double fallback = enemyRange - dist;

            if (fallback > maxThreat) {
                maxThreat = fallback;
            }
        }
        return maxThreat;
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
    }

    /**
     * Returns the threat level at a given position. It clamps the position to the board if it is outside of it.
     * @param position the position to get the threat level from
     * @return the threat level at the given position, normalized between 0 and 1
     */
    public double getThreatLevel(Coords position) {
        return threatLevel[getIndexFromCoordsClampedToBoard(position)];
    }

    /**
     * Returns the threat level at a given position. It clamps the position to the board if it is outside of it.
     * @param position the position to get the threat level from
     * @return the threat level at the given position, normalized between 0 and 1
     */
    public double getAlliedThreatLevel(Coords position) {
        return alliedThreatLevel[getIndexFromCoordsClampedToBoard(position)];
    }

    private Coords clampToBoard(Coords position) {
        int x = position.getX();
        int y = position.getY();
        if (x < 0) {
            x = 0;
        } else if (x >= boardWidth) {
            x = boardWidth - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= boardHeight) {
            y = boardHeight - 1;
        }
        return new Coords(x, y);
    }

    public double getThreatLevel(Coords position, int radius) {
        double totalThreat = 0;
        int count = 0;
        List<Coords> coords = position.allAtDistanceOrLess(radius);
        for (Coords coord : coords) {
            int x1 = coord.getX();
            int y1 = coord.getY();
            if (x1 >= 0 && x1 < boardWidth && y1 >= 0 && y1 < boardHeight) {
                totalThreat += threatLevel[y1 * boardWidth + x1];
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return totalThreat / count;
    }

    /**
     * Returns the difference in height between two positions, positive number means it
     * is higher, negative means it is lower.
     * @param from coordinates of the first position
     * @param to coordinates of the second position
     * @return the difference in height between the two positions
     */
    public int levelDifference(Coords from, Coords to) {
        return levelAt(to) - levelAt(from);
    }

    public boolean hasWoods(Coords position) {
        return woodedTerrain.get(getIndexFromCoordsClampedToBoard(position));
    }

    public boolean hasBuilding(Coords position) {
        return buildings.get(getIndexFromCoordsClampedToBoard(position));
    }

    public boolean hasHazard(Coords position) {
        return hazardousTerrain.get(getIndexFromCoordsClampedToBoard(position));
    }

    public boolean hasWater(Coords position) {
        return hasWaterLevel.get(getIndexFromCoordsClampedToBoard(position));
    }

    public boolean isClear(Coords position) {
        return clearTerrain.get(getIndexFromCoordsClampedToBoard(position));
    }

    public int getIndexFromCoords(int x, int y) {
        if (x >= boardWidth || y >= boardHeight || x < 0 || y < 0) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds x=" + x + " y=" + y);
        }
        return y * boardWidth + x;
    }

    public Coords getCoordsFromIndex(int idx) {
        return new Coords(idx % boardWidth, idx / boardWidth);
    }

    private int getIndexFromCoords(Coords c) {
        return getIndexFromCoords(c.getY(), c.getX());
    }

    public int getIndexFromCoordsClampedToBoard(Coords coords) {
        return getIndexFromCoords(clampToBoard(coords));
    }

    public boolean insideBoard(Coords position) {
        int idx = getIndexFromCoords(position);
        return idx < numberOfHexes && idx >= 0;
    }

    public boolean hasPartialCover(Coords position, int baseHeight, int unitHeight) {
        return (boardTerrainLevels[getIndexFromCoordsClampedToBoard(position)] > baseHeight) &&
              (boardTerrainLevels[getIndexFromCoordsClampedToBoard(position)] < baseHeight+unitHeight);
    }

    public boolean hasFullCover(Coords position, int baseHeight, int unitHeight) {
        return boardTerrainLevels[getIndexFromCoordsClampedToBoard(position)] >= baseHeight+unitHeight;
    }

    public boolean onGround() {
        return groundBoard;
    }

    public int getHeight() {
        return boardHeight;
    }

    public int getWidth() {
        return boardWidth;
    }

    public Set<Coords> getRandomClearCoords(int numberOfCoords) {
        Set<Coords> coords = new HashSet<>();
        int idx;
        int antiInfiniteLoopLatch = 500;
        while(coords.size() < numberOfCoords && antiInfiniteLoopLatch > 0) {
            antiInfiniteLoopLatch--;
            idx = (int) (Math.random() * numberOfHexes);
            if (clearTerrain.get(idx)) {
                coords.add(getCoordsFromIndex(idx));
            }
        }
        return coords;
    }

}
