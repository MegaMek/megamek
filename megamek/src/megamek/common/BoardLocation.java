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
package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import megamek.common.annotations.Nullable;

/**
 * Represents a location (i.e. Coords) on the game board of a specific ID. With a game having multiple maps, this class
 * needs to replace Coords in many methods to identify a specific position of an Entity or event. The coords cannot
 * be null.
 * <p>
 * BoardLocation is immutable.
 */
public class BoardLocation implements Serializable {

    /**
     * This location represents a location that has null coords or a negative board ID, i.e. is obviously invalid.
     * This location, when checked, will return false for comparisons and empty results for adjacent hexes and the
     * like. Note that a BoardLocation that is not a NO_LOCATION does not necessarily exist either, its board ID may
     * still be invalid or its coords outside of any board.
     * <p>
     * The coords or board ID of NO_LOCATION should not be used directly but if used the coords are at Integer
     * .MIN_VALUE and the board ID is Board.BOARD_NONE.
     */
    public static final BoardLocation NO_LOCATION =
          new BoardLocation(new Coords(Integer.MIN_VALUE, Integer.MIN_VALUE), Board.BOARD_NONE);

    private final Coords coords;
    private final int boardId;
    private final boolean isNoLocation;

    private BoardLocation(Coords coords, int boardId) {
        this(coords, boardId, false);
    }

    private BoardLocation(Coords coords, int boardId, boolean isNoLocation) {
        this.coords = coords;
        this.boardId = boardId;
        this.isNoLocation = isNoLocation;
    }

    /**
     * Returns a BoardLocation with the given data. When coords are null or the boardId negative, NO_LOCATION is
     * returned. This means that the created BoardLocation never has null coords. Still, the returned
     * BoardLocation may not represent a valid location, as the board ID and coords are not checked against existing
     * boards.
     *
     * @param coords The coords
     * @param boardId The board ID
     * @return A BoardLocation representing the given position or NO_LOCATION
     */
    public static BoardLocation of(Coords coords, int boardId) {
        if ((coords == null) || (boardId < 0)) {
            return BoardLocation.NO_LOCATION;
        } else {
            return new BoardLocation(coords, boardId);
        }
    }

    /**
     * @param boardId The board ID to test
     * @return True when this location's board ID is equal to the given board ID, i.e. when this location is on the
     * given board. If this location is a non-location, this will always return false.
     * @see #NO_LOCATION
     */
    public boolean isOn(int boardId) {
        return !isNoLocation && this.boardId == boardId;
    }

    /**
     * @param coords The coords to test
     * @return True when this location's coords are equal to the given coords.
     */
    public boolean isAt(@Nullable Coords coords) {
        return !isNoLocation && this.coords.equals(coords);
    }

    /**
     * Returns a list of all six adjacent coordinates (distance = 1). Does not check
     * if those are on the board or if the board of the present boardId exists. This
     * is equivalent to {@link Coords#allAdjacent()} with the boardId of the present
     * BoardLocation added in. This is also equivalent to calling allAtDistance(1).
     *
     * @return A list of adjacent BoardLocations
     */
    public List<BoardLocation> allAdjacent() {
        return allAtDistance(1);
    }

    /**
     * Returns a list of all coordinates at the given distance dist. Does not check
     * if those are on the board or if the board of the present boardId exists.
     * Returns an empty list for dist of less than 0 and the calling BoardLocation
     * itself for dist == 0. This is equivalent to {@link Coords#allAtDistance(int)}
     * with the boardId of the present BoardLocation added in.
     *
     * @return A list of BoardLocations centered on this BoardLocation and at the
     *         given distance
     */
    public List<BoardLocation> allAtDistance(final int dist) {
        if (isNoLocation) {
            return Collections.emptyList();
        } else {
            return coords.allAtDistance(dist).stream().map(c -> new BoardLocation(c, boardId)).toList();
        }
    }

    /**
     * Returns a list of all coordinates at the given distance dist
     * and anything less than dist as well.
     */
    public List<BoardLocation> allAtDistanceOrLess(int dist) {
        return allAtDistances(0, dist);
    }

    /**
     * Returns a list of all coordinates at the given distance dist
     * and anything less than dist as well.
     */
    public List<BoardLocation> allAtDistances(int minimumDistance, int maximumDistance) {
        if (isNoLocation) {
            return Collections.emptyList();
        } else {
            List<BoardLocation> result = new ArrayList<>();
            for (int radius = minimumDistance; radius <= maximumDistance; radius++) {
                result.addAll(allAtDistance(radius));
            }
            return result;
        }
    }

    /**
     * Returns the coordinate 1 unit in the specified direction dir.
     */
    public BoardLocation translated(int dir) {
        if (isNoLocation) {
            return NO_LOCATION;
        } else {
            return BoardLocation.of(coords.translated(dir, 1), boardId);
        }
    }

    @Override
    public String toString() {
        return coords + "; Map Id: " + boardId;
    }

    public String getBoardNum() {
        if (isNoLocation) {
            return "No Location";
        } else {
            return coords.getBoardNum() + " (Map Id: " + boardId + ")";
        }
    }

    public String toFriendlyString() {
        if (isNoLocation) {
            return "No Location";
        } else {
            return coords.toFriendlyString() + " (Map Id: " + boardId + ")";
        }
    }

    public boolean isSameBoardAs(@Nullable BoardLocation other) {
        return !isNoLocation && (other != null) && !other.isNoLocation && boardId == other.boardId;
    }

    /**
     * Two BoardLocations are equal when their board ID and coords are equal. Two NO_LOCATIONs are equal.
     *
     * @param o The object to compare
     * @return True when the two are equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BoardLocation that)) {
            return false;
        }
        return (boardId == that.boardId) && Objects.equals(coords, that.coords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coords, boardId);
    }

    public int getX() {
        return coords.getX();
    }

    public int getY() {
        return coords.getY();
    }

    public boolean isNoLocation() {
        return isNoLocation;
    }

    public Coords coords() {
        return coords;
    }

    public int boardId() {
        return boardId;
    }
}
