/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.util.List;
import java.util.Vector;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.units.Entity;

/**
 * Manages the center of a swarm of friendly units.
 *
 * @author Luana Coppio
 */
public class SwarmCenterManager {

    private final List<Coords> historicalCenters = new Vector<>();
    private final Princess owner;

    // Example momentum scaling (0.3 = 30% of trend)
    private static final double momentumScale = 0.3;

    public SwarmCenterManager(Princess princess) {
        this.owner = princess;
    }

    /**
     * Calculates the center of the swarm of friendly units.
     *
     * @return Center coordinate
     */
    public Coords calculateCenter() {
        List<Entity> friends = owner.getFriendEntities();
        return getAdjustedCenter(friends, owner.getBoard());
    }

    /**
     * Calculates the center of the swarm of friendly units keeping the momentum and avoiding map borders.
     *
     * @param friends List of friendly units
     * @param board   Game board
     *
     * @return Center coordinate adjusted for momentum and map borders
     */
    private Coords getAdjustedCenter(List<Entity> friends, Board board) {
        Coords rawCenter = calculateStrategicSwarmCenter(friends);
        if (rawCenter == null) {
            return null;
        }
        // Adjust for map borders
        rawCenter = pushFromBorders(rawCenter, board);

        // Maintain momentum if moving directionally
        if (historicalCenters.size() >= 3) {
            int[] movementVector = calculateMovementTrend();
            // Convert XY vector to hex direction/distance
            HexTranslation translation = convertVectorToHexTranslation(
                  movementVector[0],
                  movementVector[1]
            );

            // Apply scaled translation
            rawCenter = rawCenter.translated(
                  translation.direction,
                  (int) Math.round(translation.distance * momentumScale)
            );
        }
        rawCenter = clipToMap(rawCenter, board);

        historicalCenters.add(new Coords(rawCenter));
        return rawCenter;
    }

    private HexTranslation convertVectorToHexTranslation(double dx, double dy) {
        if (dx == 0 && dy == 0) {
            return new HexTranslation(0, 0);
        }

        // Convert vector to polar coordinates
        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);

        // Adjust for hex grid orientation (0° = East in standard math, needs to align with 0 = North)
        angleDeg = (angleDeg - 30 + 360) % 360; // Rotate so 0° points North-East

        // Map to hex directions (60° per direction)
        int direction = (int) Math.round(angleDeg / 60) % 6;

        // Calculate hex distance (approximate)
        int distance = new Coords(0, 0).distance((int) dx, (int) dy);

        return new HexTranslation(direction, distance);
    }

    private record HexTranslation(int direction, int distance) {}

    private int[] calculateMovementTrend() {
        if (historicalCenters.size() < 2) {
            return new int[] { 0, 0 };
        }

        // Get last 2 centers for direction
        Coords current = historicalCenters.get(historicalCenters.size() - 1);
        Coords previous = historicalCenters.get(historicalCenters.size() - 2);

        // Calculate vector between last two centers
        return new int[] {
              current.getX() - previous.getX(),
              current.getY() - previous.getY()
        };
    }

    private Coords pushFromBorders(Coords center, Board board) {
        int pushDistance = 3;
        int borderMargin = 5;

        // Calculate distance to nearest edge
        int xDist = Math.min(center.getX(), board.getWidth() - center.getX());
        int yDist = Math.min(center.getY(), board.getHeight() - center.getY());

        if (xDist < borderMargin || yDist < borderMargin) {
            // Push towards center
            return new Coords(
                  center.getX() + (center.getX() < borderMargin ? pushDistance : -pushDistance),
                  center.getY() + (center.getY() < borderMargin ? pushDistance : -pushDistance)
            );
        }
        return center;
    }

    private @Nullable Coords calculateStrategicSwarmCenter(List<Entity> friends) {
        if (friends.isEmpty()) {
            return null;
        }
        // Using cube coordinates for weighted average
        // we can get more precise results than with offset coordinates
        // without incurring in worse performance
        double totalWeight = 0;
        double qSum = 0;
        double rSum = 0;
        double sSum = 0;

        for (Entity unit : friends) {
            CubeCoords cube = unit.getPosition().toCube();
            double weight = calculateUnitWeight(unit);

            qSum += cube.q() * weight;
            rSum += cube.r() * weight;
            sSum += cube.s() * weight;
            totalWeight += weight;
        }

        CubeCoords weightedCube = new CubeCoords(
              qSum / totalWeight,
              rSum / totalWeight,
              sSum / totalWeight
        );

        return weightedCube.roundToNearestHex().toOffset();
    }

    private double calculateUnitWeight(Entity unit) {
        int unitDamageLevel = unit.getDamageLevel();
        // Example weights based on role and status
        double unitRoleWeight = switch (unit.getRole()) {
            case BRAWLER, SKIRMISHER -> 1.5;
            case JUGGERNAUT -> 2.0;
            case SCOUT -> 0.2;
            case MISSILE_BOAT, SNIPER -> 0.5;
            default -> 1.0;
        };

        return unitRoleWeight * (1.0 / unitDamageLevel);
    }

    private Coords clipToMap(Coords coords, Board board) {
        int x = MathUtility.clamp(coords.getX(), 0, board.getWidth() - 1);
        int y = MathUtility.clamp(coords.getY(), 0, board.getHeight() - 1);
        return new Coords(x, y);
    }
}
