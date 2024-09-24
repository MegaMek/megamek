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
package megamek.common.hexarea;

import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.annotations.Nullable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

//TODO: add flee from ... field to entity, fallback to player, fallback to team -> game method
//TODO: check fleefrom in movementdisplay

/**
 * This class represents an area composed of hexes. The area can be a basic shape or be defined by adding, subtracting or intersecting basic
 * shapes. The area can be used to define a deployment zone in code using {@link Board#setDeploymentZone(int, HexArea)}
 * <P>Note:
 * <BR>- A HexArea can be empty if its shapes result in no valid hexes;
 * <BR>- A HexArea can be infinite; therefore, its hexes can only be retrieved by limiting the results to a rectangle, e.g. a Board;
 * <BR>- A HexArea can be absolute (independent of the rectangle) or relative to the rectangle that limit the results;
 * <BR>- A HexArea can appear empty when its shapes do not contain any hexes within the given rectangle;
 * <BR>- A HexArea does not have to be contiguous;
 * <BR>- HexAreas are typically lightweight as they don't store their hexes (unless ListHexArea is misused to store thousands of hexes),
 * only the rules to create the hexes
 * <P>HexArea is immutable.
 * <P>Note that the shape can have any complexity by being itself constructed from other shapes. For example, the intersection of two
 * circles can be created by calling
 * <pre>{@code
 * new HexAreaIntersection(
 *       new HexCircleShape(new Coords(20, 5), 14),
 *       new HexCircleShape(new Coords(0, 5), 14));}</pre>
 *
 * @see HexAreaUnion
 * @see HexAreaDifference
 * @see HexAreaIntersection
 * @see BorderHexArea
 */
public interface HexArea extends Serializable {

    /**
     * Returns true if this shape contains the given coords. Returns false when the given coords is null. The given rectangle is the area
     * (e.g. the board) that is looked at currently. If this shape is absolute, i.e. does not depend on parameters outside itself, the
     * rectangle does not matter. Some shapes however may be relative to the rectangle, e.g. a shape that returns the borders of the board.
     * Note that the corner coordinates can be given in any order, i.e. x1 <= x2 or x1 > x2 and likewise for y1 and y2.
     *
     * @param coords The coords that are tested if they are part of this shape
     * @param x1     The first corner x of the rectangle limiting the results
     * @param y1     The first corner y of the rectangle limiting the results
     * @param x2     The second corner x of the rectangle limiting the results
     * @param y2     The second corner y of the rectangle limiting the results
     * @return True if this shape contains the coords
     */
    boolean containsCoords(@Nullable Coords coords, int x1, int y1, int x2, int y2);

    /**
     * Returns true if this shape contains the given coords. Returns false when the given coords is null. If this shape is absolute, i.e.
     * does not depend on parameters outside itself, the board does not matter. Some shapes however may be relative to the board size, e.g.
     * a shape that returns the borders of the board.
     *
     * @param coords The coords that are tested if they are part of this shape
     * @param board  The board to limit the area coords to
     * @return True if this shape contains the coords
     */
    default boolean containsCoords(@Nullable Coords coords, Board board) {
        return containsCoords(coords, 0, 0, board.getWidth(), board.getHeight());
    }

    /**
     * @return True if this shape is, by itself, finite and small enough and absolute (independent of a board) that its coords can be given
     * directly. If false, its coords cannot be retrieved, only {@link #containsCoords(Coords, int, int, int, int)} can be used. Always call
     * this method and only if it returns true, call {@link #getCoords()}.
     * <BR><BR>
     * Implementation note: By default, this method returns false.
     * <BR>
     * This method may be overridden to return true for finite, small shapes, such as a hex circle of diameter 4. In that case, getCoords
     * must also be overriden to return the coords of this shape.
     */
    default boolean isSmall() {
        // Some shapes, even if finite, have 10000 or more Coords. It may be good to avoid retrieving
        // those to find the resulting Coords on a small board.
        // On the other hand, the board may have 10000 hexes and this shape may only have a handful,
        // making it better to process these coords directly rather than cycle the whole board.
        // This method exists so both cases can be dealt with as efficiently as possible.
        return false;
    }

    /**
     * Returns all coords of this shape, if it is finite and small enough and an absolute shape. Only use this when {@link #isSmall()}
     * returns true - it will throw an exception otherwise.
     * <BR><BR>Implementation note: Throws an exception by default. Override together with {@link #isSmall()}
     * for small absolute shapes.
     *
     * @return All Coords of this shape
     * @throws IllegalStateException when this method is called on a shape where {@link #isSmall()} returns false
     * @see #getCoords(int, int, int, int)
     */
    default Set<Coords> getCoords() {
        throw new IllegalStateException("Can only be used on small, finite shapes.");
    }

    /**
     * Returns a set of the coords of this Shape that lie on the given board. This method should not be overridden.
     *
     * @param board The board to limit the results to
     * @return Coords of this shape that lie within the board
     */
    default Set<Coords> getCoords(Board board) {
        return getCoords(0, 0, board.getWidth(), board.getHeight());
    }

    /**
     * Returns a set of the coords of this HexArea that lie in the rectangle defined by the given lower left and upper right corner. This
     * method should not be overridden.
     *
     * @param x1 The first corner x of the rectangle limiting the results
     * @param y1 The first corner y of the rectangle limiting the results
     * @param x2 The second corner x of the rectangle limiting the results
     * @param y2 The second corner y of the rectangle limiting the results
     * @return Coords of this shape that lie within the rectangle
     */
    default Set<Coords> getCoords(int x1, int y1, int x2, int y2) {
        int xl = Math.min(x1, x2);
        int xu = Math.max(x1, x2);
        int yl = Math.min(y1, y2);
        int yu = Math.max(y1, y2);
        if (isSmall()) {
            return getCoords().stream()
                .filter(coords -> isInRectangle(coords, xl, yl, xu, yu))
                .collect(Collectors.toSet());
        } else {
            Set<Coords> result = new HashSet<>();
            for (int y = yl; y <= yu; y++) {
                for (int x = xl; x <= xu; x++) {
                    Coords coords = new Coords(x, y);
                    if (containsCoords(coords, xl, yl, xu, yu)) {
                        result.add(coords);
                    }
                }
            }
            return result;
        }
    }

    /**
     * @return True if the coords is in the rectangle given by the corner values which must be sorted so x1 <= x2 and y1 <= y2. Coords equal
     * to any of the x and y values are counted as in the rectangle.
     */
    private boolean isInRectangle(Coords coords, int x1, int y1, int x2, int y2) {
        return coords.getX() >= x1 && coords.getX() <= x2
            && coords.getY() >= y1 && coords.getY() <= y2;
    }
}
