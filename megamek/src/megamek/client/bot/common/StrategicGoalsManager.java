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
import megamek.logging.MMLogger;

import java.util.List;
import java.util.Vector;

/**
 * Manages the strategic goals for the bot, the strategic goals are used to distribute the map evenly across the units
 * inside the army to cover more ground and find the enemy faster
 * @author Luana Coppio
 */
public class StrategicGoalsManager {
    private static final MMLogger logger = MMLogger.create(StrategicGoalsManager.class);

    private record QuadrantParameters(int startX, int startY, int endX, int endY) { }
    private final List<Coords> strategicGoals = new Vector<>();
    private int quadrantHeight = 0;
    private int quadrantWidth = 0;
    private int offsetX = 0;
    private int offsetY = 0;

    /**
     * Add a strategic goal to the list of goals, a strategic goal is simply a coordinate which we want to move towards,
     * it's mainly used for double-blind games where we don't know the enemy positions, the strategic goals help
     * distribute the map evenly across the units inside the army to cover more ground and find the enemy faster
     * @param coords  The coordinates to add
     */
    public void addStrategicGoal(Coords coords) {
        strategicGoals.add(coords);
    }

    /**
     * Get the strategic goals
     * @return A list of strategic goals
     */
    public List<Coords> getStrategicGoals() {
        return strategicGoals;
    }

    /**
     * Get the strategic goals on the quadrant of the given coordinates
     * @param coords The coordinates to check
     * @return A list of strategic goals on the quadrant
     */
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

    /**
     * Remove all strategic goals on the quadrant of the given coordinates
     * @param coords The coordinates to check
     */
    public void removeAllStrategicGoalsOnCoordsQuadrant(Coords coords) {
        QuadrantParameters quadrant = getQuadrantParameters(coords);
        for (int i = quadrant.startX(); i < quadrant.endX(); i++) {
            for (int j = quadrant.startY(); j < quadrant.endY(); j++) {
                strategicGoals.remove(new Coords(i, j));
                logger.debug("Removed strategic goal at: {}", new Coords(i, j));
            }
        }
    }

    /**
     * Initialize the strategic goals for the board
     * @param board The board to initialize the goals on
     * @param quadrantWidth The width of the quadrants
     * @param quadrantHeight The height of the quadrants
     */
    public void initializeStrategicGoals(Board board, int quadrantWidth, int quadrantHeight) {
        strategicGoals.clear();
        this.quadrantWidth = quadrantWidth;
        this.quadrantHeight = quadrantHeight;
        int radius = Math.max(quadrantWidth / 2, 3);
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
                for (var coords : new Coords(xMidPoint, yMidPoint).allAtDistanceOrLess(radius)) {
                    var hex = board.getHex(coords);
                    if (hex == null || hex.isClearHex()) {
                        addStrategicGoal(coords);
                        break;
                    }
                }
            }
        }
    }
}
