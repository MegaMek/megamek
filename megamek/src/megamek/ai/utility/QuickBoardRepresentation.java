package megamek.ai.utility;

import megamek.client.bot.caspar.ai.utility.tw.context.StructOfArraysEntity;
import megamek.common.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/** this is a static globally shared representation of the board state
 *  that is used by the decision scoring system to evaluate the board state
 *  and make decisions based on that evaluation.
 */
public class QuickBoardRepresentation {
    private static final int NORMALIZED_THREAT_MAP_SIZE = 10;
    public static final int NORMALIZED_THREAT_HEATMAP = NORMALIZED_THREAT_MAP_SIZE * NORMALIZED_THREAT_MAP_SIZE;
    private final int[] boardTerrainLevels;
    private final int[] threatLevelDiscrete;
    private final double[] threatLevel;
    private final double[] normalizedThreatLevel;
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

    public QuickBoardRepresentation(Board board) {
        boardWidth = board.getWidth();
        boardHeight = board.getHeight();
        numberOfHexes = boardWidth * boardHeight;
        boardTerrainLevels = new int[numberOfHexes];
        threatLevelDiscrete = new int[numberOfHexes];
        normalizedThreatLevel = new double[NORMALIZED_THREAT_HEATMAP];
        threatLevel = new double[numberOfHexes];
        hazardousTerrain = new BitSet(numberOfHexes);
        woodedTerrain = new BitSet(numberOfHexes);
        buildings = new BitSet(numberOfHexes);
        clearTerrain = new BitSet(numberOfHexes);
        hasWaterLevel = new BitSet(numberOfHexes);
        groundBoard = board.onGround();
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
        return boardTerrainLevels[getIndexFromCoords(position)];
    }

    public void updateThreatHeatmap(StructOfArraysEntity enemyUnits) {
        Arrays.fill(threatLevel, 0.0);
        if (enemyUnits == null) {
            return;
        }
        int width = boardWidth;
        int height = boardHeight;
        int maxValue = 0;
        int[][] xyr = enemyUnits.getAllXYMaxRange();

        // first pass of heatmap decay
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                threatLevelDiscrete[y * width + x] = threatLevelDiscrete[y * width + x] / DECAY;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double maxThreat = getMaxThreat(xyr, x, y);
                double oldValue = threatLevelDiscrete[y * width + x];
                int newValue =  (int) Math.round(Math.max(oldValue, maxThreat));
                maxValue =  Math.max(maxValue, newValue);
                threatLevelDiscrete[y * width + x] = newValue;
            }
        }
        if (maxValue == 0) {
            return;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double newValue = threatLevelDiscrete[y * width + x] / (double) maxValue;
                threatLevel[y * width + x] = newValue;
            }
        }
        resizeHeatmapBilinear();
    }

    private void resizeHeatmapBilinear() {
        Arrays.fill(normalizedThreatLevel, 0.0);
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
                double topLeft = threatLevel[y1 * boardWidth + x1];
                double topRight = threatLevel[y1 * boardWidth + x2];
                double top = topLeft * (1 - dx) + topRight * dx;
                double bottomLeft = threatLevel[y2 * boardWidth + x1];
                double bottomRight = threatLevel[y2 * boardWidth + x2];
                double bottom = bottomLeft * (1 - dx) + bottomRight * dx;
                double value = top * (1 - dy) + bottom * dy;
                normalizedThreatLevel[newY * newWidth + newX] = value;
            }
        }
    }

    public double[] getNormalizedThreatLevelHeatmap() {
        return normalizedThreatLevel;
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

    public double getThreatLevel(Coords position) {
        return threatLevel[getIndexFromCoords(position)];
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
        return woodedTerrain.get(getIndexFromCoords(position));
    }

    public boolean hasBuilding(Coords position) {
        return buildings.get(getIndexFromCoords(position));
    }

    public boolean hasHazard(Coords position) {
        return hazardousTerrain.get(getIndexFromCoords(position));
    }

    public boolean hasWater(Coords position) {
        return hasWaterLevel.get(getIndexFromCoords(position));
    }

    public boolean isClear(Coords position) {
        return clearTerrain.get(getIndexFromCoords(position));
    }

    public int getIndexFromCoords(int x, int y) {
        return y * boardWidth + x;
    }

    public int getIndexFromCoords(Coords c) {
        return getIndexFromCoords(c.getY(), c.getX());
    }

    public boolean insideBoard(Coords position) {
        int idx = getIndexFromCoords(position);
        return idx < numberOfHexes && idx >= 0;
    }

    public boolean hasPartialCover(Coords position, int baseHeight, int unitHeight) {
        return (boardTerrainLevels[getIndexFromCoords(position)] > baseHeight) &&
            (boardTerrainLevels[getIndexFromCoords(position)] < baseHeight+unitHeight);
    }

    public boolean hasFullCover(Coords position, int baseHeight, int unitHeight) {
        return boardTerrainLevels[getIndexFromCoords(position)] >= baseHeight+unitHeight;
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

}
