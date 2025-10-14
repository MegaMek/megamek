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

package megamek.common.hexArea;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.server.trigger.UnitPositionTrigger;

/**
 * This class represents an area composed of hexes on a single board. The area can be a basic shape or be defined by
 * adding, subtracting or intersecting basic shapes. Areas can be used to define deployment zones in code using
 * {@link Board#addDeploymentZone(int, HexArea)}, to set a zone where units may flee the board from in
 * {@link Entity#setFleeZone(HexArea)} and in positional triggers for events ({@link UnitPositionTrigger}). HexAreas can
 * be present on some or all boards; on each board that it is present, it has the same shape.
 * <P>Note:
 * <BR>- A HexArea can be empty if its shapes result in no valid hexes;
 * <BR>- A HexArea can be infinite; therefore, its hexes can only be retrieved by limiting the results to a Board;
 * <BR>- A HexArea can be absolute (independent of the board's size and contents) or relative to the board;
 * <BR>- A HexArea can appear empty when its shapes do not contain any hexes within the given board;
 * <BR>- A HexArea does not have to be contiguous;
 * <BR>- HexAreas are typically lightweight as they don't store their hexes (unless ListHexArea is misused to store
 * thousands of hexes), only the rules to create the hexes;
 * <P>HexArea is immutable.
 * <P>Note that the shape can have any complexity by being itself constructed from other shapes. For example, the
 * intersection of two circles can be created by calling
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
     * Returns true if this shape contains the given coords. Returns false when the given coords is null. If this shape
     * is absolute, i.e. does not depend on parameters outside itself, the board does not matter. Note that this means
     * that areas are *not* required to return true only for coords that are on the given board!
     * <p>
     * Some shapes however may be relative to the board size, e.g. a shape that returns the borders of the board; or
     * even relate to board contents, such as terrain.
     *
     * @param coords The coords that are tested if they are part of this shape
     * @param board  The board to limit the area coords to
     *
     * @return True if this shape contains the coords
     */
    boolean containsCoords(@Nullable Coords coords, Board board);

    /**
     * Returns a set of the coords of this area that are part of the given board.
     *
     * @param board The board to limit the results to
     *
     * @return Coords of this shape that lie on the board
     */
    Set<Coords> getCoords(Board board);


    /**
     * Returns true if this HexArea is present on the given board. By default, forwards to {@link #matchesBoardId(int)}.
     * This method will usually not need to be overridden.
     *
     * @return True if this area is present on the given board
     */
    default boolean matchesBoardId(Board board) {
        return matchesBoardId(board.getBoardId());
    }

    /**
     * Returns true if this HexArea is present on the given board.
     * <p>
     * Note: By default, this returns true so that any hex area without board ID information defaults to being present
     * on the one and only board in a single board setup. Override to provide useful multiboard support.
     *
     * @return True if this area is present on the given board
     */
    default boolean matchesBoardId(int boardId) {
        return true;
    }

    /**
     * Replaces the board IDs that this area is on with the given board IDs.
     *
     * @param boardIds The new board IDs to use for this area
     */
    void setBoardIds(List<Integer> boardIds);
}
