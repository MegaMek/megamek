/**
 * A class for representing board sizes.
 *
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License,
 * version 2, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, it is available online at:
 *   http://www.gnu.org/licenses/gpl-2.0.html
 */
package megamek.common;

import java.io.Serializable;

/**
 * Type-safe, immutable, dimensions class for handling board sizes.
 *
 * @author Edward Cullen
 */
public class BoardDimensions implements Cloneable, Serializable,
        Comparable<BoardDimensions> {

    /**
     * See {@link java.io.Serializable}.
     */
    private static final long serialVersionUID = -3562335656969231217L;

    /**
     * Construct a new BoardDimensions object.
     *
     * @param width
     *            The width.
     * @param height
     *            The height.
     * @throws IllegalArgumentException
     *             If either width or height is less than 1.
     */
    public BoardDimensions(final int width, final int height) {
        if ((width < 1) || (height < 1)) {
            throw new IllegalArgumentException(
                    "width and height must be positive non-zero values");
        }

        w = width;
        h = height;
    }

    /**
     * Copy constructor.
     *
     * @param old
     *            The instance to copy.
     */
    public BoardDimensions(final BoardDimensions old) {
        if (old == null) {
            throw new IllegalArgumentException(
                    "must provide instance to copy constructor");
        }
        w = old.w;
        h = old.h;
    }

    /**
     * The width.
     *
     * @return The width.
     */
    public int width() {
        return w;
    }

    /**
     * The height.
     *
     * @return The height.
     */
    public int height() {
        return h;
    }

    /**
     * The total number of hexes in the board.
     *
     * @return The total number of hexes in the board.
     */
    public long numHexes() {
        return (long) width() * height();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BoardDimensions clone() {
        return new BoardDimensions(this);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + h;
        result = (prime * result) + w;
        return result;
    }

    /**
     * Return the dimensions this object represents in the form "WIDTHxHEIGHT".
     *
     * @return A String!
     */
    @Override
    public String toString() {
        return w + "x" + h;
    }

    /**
     * Compares dimensions based width, falling back on height if the widths 
     * equal
     */
    @Override
    public int compareTo(final BoardDimensions o) {
        int result = 0;

        if (width() < o.width()) {
            result = -1;
        } else if (width() > o.width()) {
            result = 1;
        } else {
            // width is the same, next consider height
            if (height() < o.height()) {
                result = -1;
            } else if (height() > o.height()) {
                result = 1;
            }
        }
        return result;
    }

    private final int w;
    private final int h;
}
