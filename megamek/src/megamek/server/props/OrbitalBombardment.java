/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.props;

/**
 * Represents an orbital bombardment event.
 * x and y are board positions, damageFactor is the damage at impact point times 10, and radius is the blast radius of the explosion with
 * regular/linear damage droppoff.
 *
 * @author Luana Scoppio
 */
public class OrbitalBombardment {

    private final int x;
    private final int y;
    private final int damageFactor;
    private final int radius;

    /**
     * Represents an orbital bombardment event.
     * x and y are board positions, damageFactor is the damage at impact point times 10, and radius is the blast radius of the explosion with
     * regular/linear damage droppoff.
     *
     * @param builder
     */
    private OrbitalBombardment(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.damageFactor = builder.damageFactor;
        this.radius = builder.radius;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDamageFactor() {
        return damageFactor;
    }

    public int getRadius() {
        return radius;
    }

    /**
     * Builder of an orbital bombardment event.
     * x and y are board positions, damageFactor is the damage at impact point times 10, and radius is the blast radius of the explosion with
     * regular/linear damage droppoff.
     *
     */
    public static class Builder {
        private int x;
        private int y;
        private int damageFactor = 10;
        private int radius = 4;

        public Builder x(int x) {
            this.x = x;
            return this;
        }

        public Builder y(int y) {
            this.y = y;
            return this;
        }

        public Builder damageFactor(int damageFactor) {
            this.damageFactor = damageFactor;
            return this;
        }

        public Builder radius(int radius) {
            this.radius = radius;
            return this;
        }

        /**
         * Builds an orbital bombardment.
         *
         * @return an immutable instance of an orbital bombardment.
         */
        public OrbitalBombardment build() {
            return new OrbitalBombardment(this);
        }
    }
}
