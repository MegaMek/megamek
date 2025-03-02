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

/**
 * Represents a set of cube coordinates in a hex grid.
 * Cube Coordinate allows for more precise manipulation of distances, movements, and other operations.
 * @author Luana Coppio
 */
public class CubeCoords {

    public final double q;
    public final double r;
    public final double s;

    /**
     * Creates a new CubeCoords object with the given cube coordinates.
     * @param q the q coordinate
     * @param r the r coordinate
     * @param s the s coordinate
     */
    public CubeCoords(double q, double r, double s) {
        this.q = q;
        this.r = r;
        this.s = s;
    }

    /**
     * Converts cube coordinates to offset coordinates.
     * @return a new Coords object with the offset coordinates
     */
    public Coords toOffset() {
        // Implement your hex grid's conversion logic
        int x = (int) Math.round(q);
        int y = (int) Math.round(r + (q - Math.round(q)) / 2);
        return new Coords(x, y);
    }

    /**
     * Rounds the cube coordinates to the nearest hex.
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
     * @return a new CubeCoords representing the interpolated coordinate
     */
    public static CubeCoords lerp(CubeCoords a, CubeCoords b, double t) {
        double q = a.q * (1 - t) + b.q * t;
        double r = a.r * (1 - t) + b.r * t;
        double s = a.s * (1 - t) + b.s * t;
        return new CubeCoords(q, r, s);
    }
}
