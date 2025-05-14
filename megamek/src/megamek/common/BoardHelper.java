/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BoardHelper {

    public static int enclosingBoardId(Game game, BoardLocation boardLocation) {
        return game.getBoard(boardLocation).getEnclosingBoardId();
    }

    public static @Nullable Board enclosingBoard(Game game, BoardLocation boardLocation) {
        return game.getBoard(enclosingBoardId(game, boardLocation));
    }

    public static @Nullable Board enclosingBoard(Game game, int boardId) {
        return game.getBoard(game.getBoard(boardId).getEnclosingBoardId());
    }

    public static @Nullable Board enclosingBoard(Game game, Board board) {
        return game.getBoard(board.getEnclosingBoardId());
    }

    public static @Nullable Coords positionOnEnclosingBoard(Game game, Board board) {
        Board enclosingBoard = enclosingBoard(game, board);
        if (enclosingBoard != null) {
            return enclosingBoard.embeddedBoardPosition(board.getBoardId());
        } else {
            return null;
        }
    }

    public static @Nullable Coords positionOnEnclosingBoard(Game game, int boardId) {
        Board enclosingBoard = enclosingBoard(game, boardId);
        if (enclosingBoard != null) {
            return enclosingBoard.embeddedBoardPosition(boardId);
        } else {
            return null;
        }
    }

    public static boolean onDifferentGroundMaps(Game game, Entity attacker, Targetable target) {
        return game.isOnGroundMap(attacker) && game.isOnGroundMap(target) && !game.onTheSameBoard(attacker, target);
    }

    public static boolean isBoardEdge(Board board, Coords coords) {
        return (coords != null) && ((coords.getX() == 0) || (coords.getX() == board.getWidth() - 1) || (coords.getY()
              == 0) || (coords.getY() == board.getHeight() - 1));
    }

    public static List<Coords> topEdge(Board board) {
        return coordsRow(board, 0);
    }

    public static List<Coords> bottomEdge(Board board) {
        return coordsRow(board, board.getHeight() - 1);
    }

    public static List<Coords> leftEdge(Board board) {
        return coordsColumn(board, 0);
    }

    public static List<Coords> rightEdge(Board board) {
        return coordsColumn(board, board.getWidth() - 1);
    }

    /**
     * Returns the hexes of a straight line that has the given direction (facing) on the given Board and crosses the hex
     * at the given hexToCross position. If the board is null or does not contain hexToCross, an empty list is
     * returned. The coords are ordered so that each next hex lies in the given facing direction from the previous
     * (e.g., if facing is 3 = south, the first hex in the resulting line will be on the northern board edge and the
     * last hex on the southern board edge.)
     *
     * @param board      The Board on which the line is located; its size determines that start and end of the line
     * @param hexToCross Coords that the line crosses
     * @param facing     The direction of the line; also determines the order of the coords
     *
     * @return A Coords list forming a straight line across the board
     */
    public static List<Coords> coordsLine(Board board, Coords hexToCross, int facing) {
        List<Coords> positions = new ArrayList<>();
        if (board == null || !board.contains(hexToCross)) {
            return positions;
        }
        // traverse hexes in reverse direction from the chosen position to find the first hex off the board
        int reverseFacing = (facing + 3) % 6;
        Coords current = hexToCross;
        while (board.contains(current)) {
            current = current.translated(reverseFacing);
        }
        // now traverse hexes in the right direction to the board edge; these form the flight path
        current = current.translated(facing);
        while (board.contains(current)) {
            positions.add(current);
            current = current.translated(facing);
        }
        return positions;
    }

    public static List<Coords> coordsRow(Board board, int y) {
        List<Coords> result = new ArrayList<>();
        for (int x = 0; x < board.getWidth(); x++) {
            result.add(new Coords(x, y));
        }
        return result;
    }

    public static List<Coords> coordsColumn(Board board, int x) {
        List<Coords> result = new ArrayList<>();
        for (int y = 0; y < board.getHeight(); y++) {
            result.add(new Coords(x, y));
        }
        return result;
    }

    /**
     * Returns true when the given Coords is a hex of any of the atmospheric rows 1-4 (or 1-7 with very high atmosphere
     * conditions) on a high-altitude map, false otherwise. Returns false for ground row hexes and for hexes of the
     * space-atmosphere interface. Also returns false when planetary conditions indicate the atmosphere is "vacuum".
     *
     * @param coords The position on the board to test
     *
     * @return True for hexes of the atmospheric rows on a high-altitude map
     */
    public static boolean isAtmosphericRow(Game game, Board board, Coords coords) {
        return isAtmosphericRow(game, board, coords.getX());
    }

    /**
     * Returns true when the given X position is a hex of any of the atmospheric rows 1-4 (or 1-7 with very high
     * atmosphere conditions) on a high-altitude map, false otherwise. Returns false for ground row hexes and for hexes
     * of the space-atmosphere interface. Also returns false when planetary conditions indicate the atmosphere is
     * "vacuum".
     *
     * @param x The X position on the board to test (Coords.getX())
     *
     * @return True for hexes of the atmospheric rows on a high-altitude map
     */
    public static boolean isAtmosphericRow(Game game, Board board, int x) {
        return board.isHighAltitude() && (x >= 1) && (x < spaceAtmosphereInterfacePosition(game));
    }

    /**
     * Returns true when the given BoardLocation indicates a hex of any of the atmospheric rows 1-4 (or 1-7 with very
     * high atmosphere conditions) on a high-altitude map, false otherwise. Returns false for ground row hexes and for
     * hexes of the space-atmosphere interface. Also returns false when planetary conditions indicate the atmosphere is
     * "vacuum".
     *
     * @param boardLocation The location to test
     *
     * @return True for hexes of the atmospheric rows on a high-altitude map
     */
    public static boolean isAtmosphericRow(Game game, BoardLocation boardLocation) {
        return isAtmosphericRow(game, game.getBoard(boardLocation), boardLocation.coords().getX());
    }

    /**
     * Returns true when the given Coords is a hex of any of the atmospheric rows 1-4 or the ground row on a
     * high-altitude map, false if it is in the space/atmosphere interface or in true space. Returns false for all hexes
     * if this board is not a high-altitude board or when planetary conditions indicate the atmosphere is "vacuum".
     *
     * @param coords The position on the board to test
     *
     * @return true for hexes of atmospheric rows 1-4 and the ground row on a high-altitude map
     */
    public static boolean isBelowSpaceAtmosphereInterface(Game game, Board board, Coords coords) {
        return board.isHighAltitude() && (coords.getX() < spaceAtmosphereInterfacePosition(game));
    }

    /**
     * Returns the number of the atmospheric row (i.e. 1 to 4 or 1 to 7 for very high atmosphere) of the given Coords on
     * a high-altitude map (see TW p.79). For coords not in an atmospheric row or not on a high-altitude map, returns
     * -1.
     *
     * @param coords The position on the board to test
     *
     * @return The number of the atmospheric row on a high-altitude map
     */
    public static int atmosphericRowNumber(Game game, Board board, Coords coords) {
        return isAtmosphericRow(game, board, coords) ? coords.getX() : -1;
    }

    /**
     * Returns the number of the coords' atmospheric row (i.e. 1 to 4 or 1 to 7 for very high atmosphere) that is to be
     * effectively used for rule purposes on a high-altitude map (see TW p.79). This takes into account atmosphere
     * density, see Trace Atmosphere, TO:AR, p.52; in these conditions, it is not equal to
     * {@link #atmosphericRowNumber(Game, Board, Coords)}. For coords not in an atmospheric row or not on a
     * high-altitude map, returns -1.
     *
     * @param coords The position on the board to test
     *
     * @return The effective number of the atmospheric row on a high-altitude map
     */
    public static int effectiveAtmosphericRowNumber(Game game, Board board, Coords coords) {
        return effectiveAtmosphericRowNumber(game, board, coords.getX());
    }

    /**
     * Returns the number of the X position's atmospheric row (i.e. 1 to 4 or 1 to 7 for very high atmosphere) that is
     * to be effectively used for rule purposes on a high-altitude map (see TW p.79). This takes into account atmosphere
     * density, see Trace Atmosphere, TO:AR, p.52; in these conditions, it is not equal to
     * {@link #atmosphericRowNumber(Game, Board, Coords)}. For coords not in an atmospheric row or not on a
     * high-altitude map, returns -1.
     *
     * @param x The X position on the board to test (Coords.getX())
     *
     * @return The effective number of the atmospheric row on a high-altitude map
     */
    public static int effectiveAtmosphericRowNumber(Game game, Board board, int x) {
        if (game.getPlanetaryConditions().getAtmosphere().isTrace()) {
            return isAtmosphericRow(game, board, x) ? 4 : -1;
        } else {
            return isAtmosphericRow(game, board, x) ? x : -1;
        }
    }

    /**
     * Returns true when the given Coords is a hex of the space-atmosphere interface on a high-altitude map, false
     * otherwise.
     *
     * @param coords The position on the board to test
     *
     * @return true for hexes of the space-atmosphere interface on a high-atmosphere map
     */
    public static boolean isSpaceAtmosphereInterface(Game game, Board board, Coords coords) {
        return board.isHighAltitude() && (coords.getX() == spaceAtmosphereInterfacePosition(game));
    }

    /**
     * Returns true when the given Coords is a ground row hex on a high-atmospheric map, i.e. the lowermost row of
     * hexes. Returns false for atmospheric and ground maps as well as for a space map that is not high-altitude. This
     * is true for ground row hexes even if planetary conditions indicate the atmosphere is "vacuum".
     *
     * @param coords The position on the board to test
     *
     * @return true for the ground row hexes on a high atmospheric map
     */
    public static boolean isGroundRowHex(Board board, Coords coords) {
        return isGroundRowHex(board, coords.getX());
    }

    /**
     * Returns true when the given X position is a ground row hex on a high-atmospheric map, i.e. the lowermost row of
     * hexes. Returns false for atmospheric and ground maps as well as for a space map that is not high-altitude. This
     * is true for ground row hexes even if planetary conditions indicate the atmosphere is "vacuum".
     *
     * @param x The X position on the board to test
     *
     * @return true for the ground row hexes on a high atmospheric map
     */
    public static boolean isGroundRowHex(Board board, int x) {
        return board.isHighAltitude() && (x == 0);
    }

    /**
     * Returns true when the given Coords is a hex that is true space on a high-altitude map, i.e. beyond the
     * space-atmosphere interface, or for any hex of a space map that is not a high-altitude map. Returns false for
     * ground, atmospheric row and space-atmosphere interface hexes as well as for any hex of atmospheric and ground
     * maps.
     *
     * @param coords The position on the board to test
     *
     * @return true for true space hexes on a space map
     */
    public static boolean isTrueSpaceHex(Game game, Board board, Coords coords) {
        return board.isSpace() && (!board.isHighAltitude() || (coords.getX() > spaceAtmosphereInterfacePosition(game)));
    }

    /**
     * @return The coordinate of the space-atmosphere interface in the given game (i.e., for the atmospheric pressure).
     *       Returns -1 for vacuum, 8 in very high atmospheric pressure and 5 otherwise. When game is null, the standard
     *       value of 5 is returned. When comparing with Coords (x, y), compare with the x value.
     */
    public static int spaceAtmosphereInterfacePosition(@Nullable Game game) {
        if (game == null) {
            return 5;
        } else {
            return switch (game.getPlanetaryConditions().getAtmosphere()) {
                case VACUUM -> -1;
                case VERY_HIGH -> 8;
                default -> 5;
            };
        }
    }

    public static int highAltAtmoRowRangeIncrease(Game game) {
        return switch (game.getPlanetaryConditions().getAtmosphere()) {
            case VACUUM -> 0;
            case TRACE -> 1;
            case THIN -> 3;
            case HIGH -> 7;
            case VERY_HIGH -> 9;
            default -> 5;
        };
    }

    public static int highAltSpaceAtmoRangeIncrease(Game game) {
        return switch (game.getPlanetaryConditions().getAtmosphere()) {
            case VACUUM, TRACE, THIN -> 0;
            case HIGH -> 3;
            case VERY_HIGH -> 5;
            default -> 2;
        };
    }

    /**
     * Returns true when a path between the two given positions on the given board crosses the space/atmosphere
     * interface and the board is actually a high-altitude map. Returns false when the board is not a high-altitude
     * board or when one or both positions are on the space/atmosphere interface or both positions are in ground
     * row/atmospheric hexes or both positions are in true space.
     *
     * @param game      The game
     * @param board     The board to check
     * @param position1 The first position
     * @param position2 The second position
     *
     * @return True when a path between the two positions crosses the space/atmosphere interface
     */
    public static boolean crossesSpaceAtmosphereInterface(Game game, Board board, Coords position1, Coords position2) {
        return board.isHighAltitude() && ((isTrueSpaceHex(game, board, position1) && isBelowSpaceAtmosphereInterface(
              game,
              board,
              position2)) || (isTrueSpaceHex(game, board, position2) && isBelowSpaceAtmosphereInterface(game,
              board,
              position1)));
        // @@MultiBoardTODO: Make this an IMPOSSIBLE reason in loseffects for non-cap weapons
    }

    private BoardHelper() {
    }
}
