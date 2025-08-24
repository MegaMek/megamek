/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.props;

import java.util.List;

import megamek.common.board.Coords;

/**
 * Represents an orbital bombardment event. x and y are board positions, damageFactor is the damage at impact point
 * times 10, and radius is the blast radius of the explosion with regular/linear damage droppoff.
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
     * Represents an orbital bombardment event. x and y are board positions, damageFactor is the damage at impact point
     * times 10, and radius is the blast radius of the explosion with regular/linear damage droppoff.
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
     * Builder of an orbital bombardment event. x and y are board positions, damageFactor is the damage at impact point
     * times 10, and radius is the blast radius of the explosion with regular/linear damage droppoff.
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
