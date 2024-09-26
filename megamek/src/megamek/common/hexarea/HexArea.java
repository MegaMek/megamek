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
import java.util.Set;

//TODO: read flee area for player
//TODO: show flee area in some way
//TODO: positional trigger: read yaml
//TODO: positional trigger: test
//TODO: positional trigger: write how to
//TODO: Terrain-type HexArea?


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
     * Returns true if this shape contains the given coords. Returns false when the given coords is null. If this shape is absolute, i.e.
     * does not depend on parameters outside itself, the board does not matter. Some shapes however may be relative to the board size, e.g.
     * a shape that returns the borders of the board.
     *
     * @param coords The coords that are tested if they are part of this shape
     * @param board  The board to limit the area coords to
     * @return True if this shape contains the coords
     */
    boolean containsCoords(@Nullable Coords coords, Board board);

    /**
     * Returns a set of the coords of this Shape that lie on the given board. This method should not be overridden.
     *
     * @param board The board to limit the results to
     * @return Coords of this shape that lie within the board
     */
    Set<Coords> getCoords(Board board);
}
