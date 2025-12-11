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
package megamek.common.board;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Represents a set of cube coordinates in a hex grid. Cube Coordinate allows for more precise manipulation of
 * distances, movements, and other operations.
 *
 * @author Luana Coppio
 */
public record CubeCoords(double q, double r, double s) implements Serializable {

    public static final CubeCoords ZERO = new CubeCoords(0, 0, 0);

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
     * Adds another CubeCoords to this one and returns a new CubeCoords object.
     *
     * @param other the CubeCoords to add
     *
     * @return a new CubeCoords object with the sum of the coordinates
     */
    public CubeCoords add(CubeCoords other) {
        return new CubeCoords(q() + other.q(), r() + other.r(), s() + other.s());
    }

    /**
     * Subtracts another CubeCoords from this one and returns a new CubeCoords object.
     *
     * @param other the CubeCoords to subtract
     *
     * @return a new CubeCoords object with the difference of the coordinates
     */
    public CubeCoords subtract(CubeCoords other) {
        return new CubeCoords(q() - other.q(), r() - other.r(), s() - other.s());
    }

    /**
     * Gets the cube coordinate of a neighbor hex around this hex.
     *
     * @param direction the direction to get the neighbor from
     *
     * @return a CubeCoords representing the neighbor
     */
    public CubeCoords neighbor(CardinalDirection direction) {
        return add(direction.getDirection());
    }

    /**
     * Gets the cube coordinates of the neighbor hexes around this hex.
     *
     * @return an unordered set of CubeCoords representing the neighbors
     */
    public Set<CubeCoords> neighbors() {
        Set<CubeCoords> neighbors = new HashSet<>();
        for (CardinalDirection direction : CardinalDirection.values()) {
            neighbors.add(neighbor(direction));
        }
        return neighbors;
    }

    /**
     * Averages a collection of CubeCoords and returns the average as a new CubeCoords object.
     *
     * @param positions the collection of CubeCoords to average
     *
     * @return an Optional containing the average CubeCoords, or empty if the collection is empty
     */
    public static Optional<CubeCoords> mean(Collection<CubeCoords> positions) {
        if (positions.isEmpty()) {
            return Optional.empty();
        }
        double qSum = 0, rSum = 0, sSum = 0;
        for (CubeCoords pos : positions) {
            qSum += pos.q();
            rSum += pos.r();
            sSum += pos.s();
        }
        int count = positions.size();
        return Optional.of(new CubeCoords(qSum / count, rSum / count, sSum / count));
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
     * Gets the dot product of this cube coordinate and another.
     *
     * @param other the other cube coordinate
     *
     * @return the dot product of the two coordinates
     */
    public double dot(CubeCoords other) {
        return q() * other.q() + r() * other.r() + s() * other.s();
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

    /**
     * Gets the cube coordinates of a line between two cube coordinates.
     *
     * @param other the other cube coordinate
     *
     * @return a list of CubeCoords representing the line
     */
    public List<CubeCoords> intervening(CubeCoords other) {
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

    @Override
    public String toString() {
        return new StringJoiner(", ", CubeCoords.class.getSimpleName() + "[", "]")
              .add("q=" + q)
              .add("r=" + r)
              .add("s=" + s)
              .add("offset=" + toOffset())
              .toString();
    }
}
