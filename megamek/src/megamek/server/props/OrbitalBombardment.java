/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.props;

import megamek.common.Coords;

import java.util.List;

/**
 * Represents an orbital bombardment event.
 * x and y are board positions, damageFactor is the damage at impact point times 10, and radius is the blast radius of the explosion with
 * regular/linear damage droppoff.
 *
 * @author Luana Coppio
 */
public class OrbitalBombardment {

    private final int x;
    private final int y;
    private final int damage;
    private final int radius;
    private final Coords coords;
    /**
     * Represents an orbital bombardment event.
     * x and y are board positions, damageFactor is the damage at impact point times 10, and radius is the blast radius of the explosion with
     * regular/linear damage droppoff.
     *
     */
    private OrbitalBombardment(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.damage = builder.damage;
        this.radius = builder.radius;
        this.coords = new Coords(x, y);
    }

    public Coords getCoords() {
        return coords;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDamage() {
        return damage;
    }

    public int getRadius() {
        return radius;
    }

    public int getXOffset() {
        return x - radius;
    }

    public int getYOffset() {
        return y - radius;
    }

    public String getImageSignature(Coords boardPosition) {
        var offsetX = boardPosition.getX() - getXOffset();
        var offsetY = boardPosition.getY() - getYOffset();
        var modifier = offsetX % 2 == 0 ? "" : "_odd";
        var imageSig = String.format("col_%d_row_%d%s.png", offsetX, offsetY, modifier);
        return imageSig;
    }

    public List<Coords> getAllAffectedCoords() {
        return coords.allAtDistanceOrLess(radius);
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
        private int damage = 10;
        private int radius = 4;

        public Builder x(int x) {
            this.x = x;
            return this;
        }

        public Builder y(int y) {
            this.y = y;
            return this;
        }

        public Builder damage(int damage) {
            this.damage = damage;
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
