/*
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Type-safe, immutable, dimensions class for handling board sizes.
 *
 * @author Edward Cullen
 * @author Simon (Juliez)
 */
public class BoardDimensions implements Serializable, Comparable<BoardDimensions> {

    private static final long serialVersionUID = -3562335656969231217L;

    /**
     * Construct a new BoardDimensions object.
     *
     * @param width  The width
     * @param height The height
     *
     * @throws IllegalArgumentException If either width or height is less than 1.
     */
    public BoardDimensions(final int width, final int height) {
        if ((width < 1) || (height < 1)) {
            throw new IllegalArgumentException("width and height must be positive non-zero values");
        }
        w = width;
        h = height;
    }

    /** @return The board width. */
    public int width() {
        return w;
    }

    /** @return The board height. */
    public int height() {
        return h;
    }

    /** @return The total number of hexes in the board. */
    public long numHexes() {
        return (long) width() * height();
    }

    @Override
    public boolean equals(final Object obj) {
        boolean result;
        if (this == obj) {
            result = true;
        } else if ((obj == null) || (getClass() != obj.getClass())) {
            result = false;
        } else {
            BoardDimensions other = (BoardDimensions) obj;
            result = (other.w == w) && (other.h == h);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(w, h);
    }

    @Override
    public String toString() {
        return w + " x " + h;
    }

    /**
     * Compares BoardDimensions based on width, falling back on height if the widths are equal
     */
    @Override
    public int compareTo(final BoardDimensions o) {
        return (width() - o.width() != 0) ? width() - o.width() : height() - o.height();
    }

    private final int w;
    private final int h;
}
