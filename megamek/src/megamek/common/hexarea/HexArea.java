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

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.server.trigger.UnitPositionTrigger;

import java.io.Serializable;
import java.util.Set;

/**
 * This class represents an area composed of hexes. The area can be a basic shape or be defined by adding, subtracting or intersecting basic
 * shapes. Areas can be used to define deployment zones in code using {@link Board#addDeploymentZone(int, HexArea)}, to set a zone where
 * units may flee the board from in {@link Entity#setFleeZone(HexArea)} and in positional triggers for events
 * ({@link UnitPositionTrigger}).
 * <P>Note:
 * <BR>- A HexArea can be empty if its shapes result in no valid hexes;
 * <BR>- A HexArea can be infinite; therefore, its hexes can only be retrieved by limiting the results to a Board;
 * <BR>- A HexArea can be absolute (independent of the board's size and contents) or relative to the board;
 * <BR>- A HexArea can appear empty when its shapes do not contain any hexes within the given board;
 * <BR>- A HexArea does not have to be contiguous;
 * <BR>- HexAreas are typically lightweight as they don't store their hexes (unless ListHexArea is misused to store thousands of hexes),
 * only the rules to create the hexes;
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
     * This area can be used whenever an empty area is required.
     */
    HexArea EMPTY_AREA = new EmptyHexArea();

    /**
     * Returns true if this shape contains the given coords. Returns false when the given coords is null. If this shape is absolute, i.e.
     * does not depend on parameters outside itself, the board does not matter. Some shapes however may be relative to the board size, e.g.
     * a shape that returns the borders of the board; or even board contents, such as terrain.
     *
     * @param coords The coords that are tested if they are part of this shape
     * @param board  The board to limit the area coords to
     * @return True if this shape contains the coords
     */
    boolean containsCoords(@Nullable Coords coords, Board board);

    /**
     * Returns a set of the coords of this area that are part of the given board.
     *
     * @param board The board to limit the results to
     * @return Coords of this shape that lie on the board
     */
    Set<Coords> getCoords(Board board);
}
