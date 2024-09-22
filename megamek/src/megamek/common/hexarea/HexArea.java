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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represents an area composed of hexes. The area can be defined by adding, subtracting
 * or intersecting basic shapes.
 * Note:
 * <BR>- A HexArea can be empty if its shapes result in no valid hexes;
 * <BR>- A HexArea can be infinite; therefore, its hexes can only be retrieved by limiting the results
 * to a rectangle;
 * <BR>- A HexArea can appear empty when its shapes do not contain any hexes within the given rectangle;
 * <BR>- A HexArea does not have to be contiguous;
 * <P>HexArea is immutable.
 */
public class HexArea {

    private final HexAreaShape shape;

    /**
     * Creates a HexArea from the given shape. Note that the shape can have any complexity by being itself
     * constructed from other shapes. For example, the intersection of two circles can be created by
     * calling
     * <pre>{@code
     * new HexArea(new HexAreaIntersection(
     *                 new HexCircleShape(new Coords(20, 5), 14),
     *                 new HexCircleShape(new Coords(0, 5), 14)));}</pre>
     *
     * @param shape The shape
     * @see HexAreaUnion
     * @see HexAreaDifference
     * @see HexAreaIntersection
     */
    public HexArea(HexAreaShape shape) {
        this.shape = Objects.requireNonNull(shape);
    }

    /**
     * Returns a set of the coords of this HexArea that lie on the given board. The board is only
     * used to limit the area to its own size. All returned Coords satisfy Board::contains.
     *
     * @param board A board, the size of which limits the returned coords
     * @return Coords of this shape that lie within the board
     */
    public Set<Coords> getCoords(Board board) {
        return getCoords(new Coords(board.getWidth(), board.getHeight()));
    }

    /**
     * Returns a set of the coords of this HexArea that lie in the rectangle defined by (0, 0) and
     * the given upper right corner. All returned Coords satisfy Board::contains.
     * Note that the x and y value of the upper right corner must be at least 0 for the result to not
     * be empty; for negative values of x or y, use {@link #getCoords(Coords, Coords)} instead.
     *
     * @param upperRight The second corner of the rectangle limiting the results
     * @return Coords of this shape that lie within the rectangle
     */
    public Set<Coords> getCoords(Coords upperRight) {
        return getCoords(new Coords(0, 0), upperRight);
    }

    /**
     * Returns a set of the coords of this HexArea that lie in the rectangle defined by the given lower
     * left and upper right corner. All returned Coords satisfy Board::contains. The lower left corner's
     * x and y should be smaller than the upper right corner's or the result will be empty.
     * All coordinates may be negative.
     *
     * @param lowerLeft The first corner of the rectangle limiting the results
     * @param upperRight The second corner of the rectangle limiting the results
     * @return Coords of this shape that lie within the rectangle
     */
    public Set<Coords> getCoords(Coords lowerLeft, Coords upperRight) {
        if (shape.isSmall()) {
            return shape.getCoords().stream()
                    .filter(c -> isInRectangle(c, lowerLeft, upperRight))
                    .collect(Collectors.toSet());
        } else {
            Set<Coords> result = new HashSet<>();
            for (int y = lowerLeft.getY(); y <= upperRight.getY(); y++) {
                for (int x = lowerLeft.getX(); x <= upperRight.getX(); x++) {
                    Coords coords = new Coords(x, y);
                    if (shape.containsCoords(coords)) {
                        result.add(coords);
                    }
                }
            }
            return result;
        }
    }

    private boolean isInRectangle(Coords coords, Coords upperRight, Coords lowerLeft) {
        return coords.getX() >= lowerLeft.getX() && coords.getX() <= upperRight.getX()
                && coords.getY() >= lowerLeft.getY() && coords.getY() <= upperRight.getY();
    }
}
