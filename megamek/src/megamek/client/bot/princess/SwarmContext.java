package megamek.client.bot.princess;

import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.common.Terrains;

import java.util.*;

public class SwarmContext {
    // Tracks enemies targeted by the swarm
    private final Map<Integer, Integer> enemyTargetCounts = new HashMap<>();
    // Tracks planned destinations of units
    private final Map<Coords, Integer> positionDensity = new HashMap<>();
    // Strategic goals (e.g., capture points, cover terrain, etc)
    private final List<Coords> strategicGoals = new Vector<>();

    private int quadrantHeight = 0;
    private int quadrantWidth = 0;
    private int offsetX = 0;
    private int offsetY = 0;

    private final Princess princess;

    public SwarmContext(Princess princess) {
        this.princess = princess;
    }

    public void recordEnemyTarget(int enemyId) {
        enemyTargetCounts.put(enemyId, enemyTargetCounts.getOrDefault(enemyId, 0) + 1);
    }

    public int getEnemyTargetCount(int enemyId) {
        return enemyTargetCounts.getOrDefault(enemyId, 0);
    }

    public void resetEnemyTargets() {
        enemyTargetCounts.clear();
    }

    public void resetPositionDensity() {
        positionDensity.clear();
    }

    public void recordPlannedPosition(Coords coords) {
        positionDensity.put(coords, positionDensity.getOrDefault(coords, 0) + 1);
    }

    public int getPositionDensity(Coords coords) {
        return positionDensity.getOrDefault(coords, 0);
    }

    public int getPositionDensity(Coords coords, int radius) {
        int accumulatedDensity = 0;
        for (var c : coords.allAtDistanceOrLess(radius)) {
            accumulatedDensity += positionDensity.getOrDefault(c, 0);
        }
        return accumulatedDensity;
    }

    public void addStrategicGoal(Coords coords) {
        strategicGoals.add(coords);
    }

    public void removeStrategicGoal(Coords coords) {
        strategicGoals.remove(coords);
    }

    public void removeStrategicGoal(Coords coords, int radius) {
        for (var c : coords.allAtDistanceOrLess(radius)) {
            strategicGoals.remove(c);
        }
    }

    public Double getQuadrantDiagonal() {
        return Math.sqrt(Math.pow(quadrantWidth, 2) + Math.pow(quadrantHeight, 2));
    }

    public List<Coords> getStrategicGoalsOnCoordsQuadrant(Coords coords) {
        QuadrantParameters quadrant = getQuadrantParameters(coords);
        Coords coord;
        List<Coords> goals = new Vector<>();
        for (int i = quadrant.startX(); i < quadrant.endX(); i++) {
            for (int j = quadrant.startY(); j < quadrant.endY(); j++) {
                coord = new Coords(i, j);
                if (strategicGoals.contains(coord)) {
                    goals.add(coord);
                }
            }
        }
        return goals;
    }

    private QuadrantParameters getQuadrantParameters(Coords coords) {
        int x = coords.getX();
        int y = coords.getY();
        int startX = offsetX + (x / quadrantWidth) * quadrantWidth;
        int startY = offsetY + (y / quadrantHeight) * quadrantHeight;
        int endX = startX + quadrantWidth;
        int endY = startY + quadrantHeight;
        return new QuadrantParameters(startX, startY, endX, endY);
    }

    private record QuadrantParameters(int startX, int startY, int endX, int endY) {
    }

    public void removeAllStrategicGoalsOnCoordsQuadrant(Coords coords) {
        QuadrantParameters quadrant = getQuadrantParameters(coords);
        for (int i = quadrant.startX(); i < quadrant.endX(); i++) {
            for (int j = quadrant.startY(); j < quadrant.endY(); j++) {
                strategicGoals.remove(new Coords(i, j));
            }
        }
    }

    public void initializeStrategicGoals(Board board, int quadrantWidth, int quadrantHeight) {
        strategicGoals.clear();
        this.quadrantWidth = quadrantWidth;
        this.quadrantHeight = quadrantHeight;

        int boardWidth = board.getWidth();
        int boardHeight = board.getHeight();

        // Calculate extra space and offsets to center the quadrants
        int extraX = boardWidth % quadrantWidth;
        int extraY = boardHeight % quadrantHeight;
        offsetX = extraX / 2;
        offsetY = extraY / 2;

        // Iterate over each quadrant using the offsets
        for (int i = 0; i < (boardWidth - offsetX); i += quadrantWidth) {
            for (int j = 0; j < (boardHeight - offsetY); j += quadrantHeight) {
                int startX = offsetX + i;
                int startY = offsetY + j;
                int endX = Math.min(startX + quadrantWidth, boardWidth);
                int endY = Math.min(startY + quadrantHeight, boardHeight);

                var xMidPoint = (startX + endX) / 2;
                var yMidPoint = (startY + endY) / 2;
                for (var coords : new Coords(xMidPoint, yMidPoint).allAtDistanceOrLess(3)) {
                    var hex = board.getHex(coords);
                    if (hex.isClearHex() && hasNoHazards(hex)) {
                        addStrategicGoal(coords);
                        break;
                    }
                }
            }
        }
    }

    private static final Set<Integer> HAZARDS = new HashSet<>(Arrays.asList(Terrains.FIRE,
        Terrains.MAGMA,
        Terrains.ICE,
        Terrains.WATER,
        Terrains.BUILDING,
        Terrains.BRIDGE,
        Terrains.BLACK_ICE,
        Terrains.SNOW,
        Terrains.SWAMP,
        Terrains.MUD,
        Terrains.TUNDRA));

    private boolean hasNoHazards(Hex hex) {
        var hazards = hex.getTerrainTypesSet();
        // Black Ice can appear if the conditions are favorable
        hazards.retainAll(HAZARDS);
        return hazards.isEmpty();
    }

}
