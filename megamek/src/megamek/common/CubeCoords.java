/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MegaMek.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a set of cube coordinates in a hex grid. Cube Coordinate allows for more precise manipulation of
 * distances, movements, and other operations.
 *
 * @author Luana Coppio
 */
public record CubeCoords(double q, double r, double s) {

    public static final CubeCoords ORIGIN = new CubeCoords(0, 0, 0);

    /**
     * Enum for the six directions in a hex grid.
     */
    public enum Direction {
        NORTH(new CubeCoords(0, -1, 1)),
        NORTHEAST(new CubeCoords(1, -1, 0)),
        SOUTHEAST(new CubeCoords(1, 0, -1)),
        SOUTH(new CubeCoords(0, 1, -1)),
        SOUTHWEST(new CubeCoords(-1, 1, 0)),
        NORTHWEST(new CubeCoords(-1, 0, 1));

        private final CubeCoords direction;

        Direction(CubeCoords direction) {
            this.direction = direction;
        }

        public CubeCoords getDirection() {
            return direction;
        }
    }

    /**
     * Creates a new CubeCoords object with the given cube coordinates.
     *
     * @param q the q coordinate
     * @param r the r coordinate
     * @param s the s coordinate
     */
    public CubeCoords {
    }

    /**
     * Converts cube coordinates to offset coordinates.
     *
     * @return a new Coords object with the offset coordinates
     */
    public Coords toOffset() {
        int x = (int) q;
        int y = (int) (r + q / 2);
        return new Coords(x, y);
    }

    /**
     * Rounds the cube coordinates to the nearest hex.
     *
     * @return a new CubeCoords object with the rounded coordinates
     */
    public CubeCoords roundToNearestHex() {
        // Implement cube coordinate rounding
        double rx = Math.round(q);
        double ry = Math.round(r);
        double rz = Math.round(s);

        double q_diff = Math.abs(rx - q);
        double r_diff = Math.abs(ry - r);
        double s_diff = Math.abs(rz - s);

        if ((q_diff > r_diff) && (q_diff > s_diff)) {
            rx = -ry - rz;
        } else if (r_diff > s_diff) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }

        return new CubeCoords(rx, ry, rz);
    }

    /**
     * Linearly interpolates between two cube coordinates.
     *
     * @param a the start cube coordinate
     * @param b the end cube coordinate
     * @param t the interpolation parameter, where {@code 0 <= t <= 1}
     *
     * @return a new CubeCoords representing the interpolated coordinate
     */
    public static CubeCoords lerp(CubeCoords a, CubeCoords b, double t) {
        double q = a.q * (1 - t) + b.q * t;
        double r = a.r * (1 - t) + b.r * t;
        double s = a.s * (1 - t) + b.s * t;
        return new CubeCoords(q, r, s);
    }

    /**
     * Gets the cube coordinates offset for the given direction.
     *
     * @param direction the direction to get the offset for
     *
     * @return a CubeCoords representing the offset
     */
    public static CubeCoords getCubeCoordsDirectionOffset(int direction) {
        if (direction < 0 || direction >= Direction.values().length) {
            throw new IllegalArgumentException("Invalid direction: " + direction);
        }
        return Direction.values()[direction].getDirection();
    }

    /**
     * Adds the given cube coordinates to this cube coordinates.
     *
     * @param other the cube coordinates to add
     *
     * @return a new CubeCoords representing the sum
     */
    public CubeCoords add(CubeCoords other) {
        return new CubeCoords(q + other.q, r + other.r, s + other.s);
    }

    /**
     * Subtracts the given cube coordinates from this cube coordinates.
     *
     * @param other the cube coordinates to subtract
     *
     * @return a new CubeCoords representing the difference
     */
    public CubeCoords subtract(CubeCoords other) {
        return new CubeCoords(q - other.q, r - other.r, s - other.s);
    }

    /**
     * Gets the cube coordinate of a neighbor hex around this hex.
     *
     * @param direction the direction to get the neighbor from
     *
     * @return a CubeCoords representing the neighbor
     */
    public CubeCoords neighbor(Direction direction) {
        return add(direction.getDirection());
    }

    /**
     * Gets the cube coordinates of the neighbor hexes around this hex.
     *
     * @param direction the direction to get the neighbor from
     *
     * @return a CubeCoords representing the neighbor
     */
    public CubeCoords neighbor(int direction) {
        return add(getCubeCoordsDirectionOffset(direction));
    }

    /**
     * Gets the cube coordinates of the neighbor hexes around this hex.
     *
     * @return a set of CubeCoords representing the neighbors
     */
    public Set<CubeCoords> neighbors() {
        Set<CubeCoords> neighbors = new HashSet<>();
        for (Direction direction : Direction.values()) {
            neighbors.add(neighbor(direction));
        }
        return neighbors;
    }

    /**
     * Gets the distance between two cube coordinates.
     *
     * @param a the first cube coordinate
     * @param b the second cube coordinate
     *
     * @return the distance between the two coordinates
     */
    public static int distanceBetween(CubeCoords a, CubeCoords b) {
        CubeCoords vec = a.subtract(b);
        return (int) Math.max(Math.abs(vec.q), Math.max(Math.abs(vec.r), Math.abs(vec.s)));
    }

    /**
     * Gets the distance between this cube coordinate and another.
     *
     * @param other the other cube coordinate
     *
     * @return the distance between the two coordinates
     */
    public int distanceTo(CubeCoords other) {
        return distanceBetween(this, other);
    }


    /**
     * Gets the distance between this cube coordinate and another.
     *
     * @return the distance from origin
     */
    public int magnitude() {
        return distanceBetween(this, ORIGIN);
    }

    /**
     * Gets the cube coordinates of a line between two cube coordinates.
     *
     * @param other the other cube coordinate
     *
     * @return a list of CubeCoords representing the line
     */
    public List<CubeCoords> lineTo(CubeCoords other) {
        int N = distanceTo(other);
        List<CubeCoords> results = new ArrayList<>();
        for (int i = 0; i <= N; i++) {
            if (i == N) {
                results.add(other);
            } else {
                results.add(lerp(this, other, 1.0 / N * i).roundToNearestHex());
            }
        }

        return results;
    }

    /**
     * Gets the dot product of this cube coordinate and another.
     *
     * @param other the other cube coordinate
     *
     * @return the dot product of the two coordinates
     */
    public double dot(CubeCoords other) {
        return q * other.q + r * other.r + s * other.s;
    }

    /**
     * Gets the cross product of this cube coordinate and another.
     *
     * @param vector the other cube coordinate
     *
     * @return the cross product of the two coordinates
     */
    public double getCrossMagnitude(CubeCoords vector) {
        double crossQ = (r * vector.s) - (s * vector.r);
        double crossR = (s * vector.q) - (q * vector.s);
        double crossS = (q * vector.r) - (r * vector.q);

        // Magnitude of the cross product
        return Math.sqrt(crossQ * crossQ + crossR * crossR + crossS * crossS);
    }
}
