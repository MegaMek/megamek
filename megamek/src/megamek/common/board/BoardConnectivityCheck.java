/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.board;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Hex;
import megamek.common.units.Terrains;

/**
 * Finds roads and bridges on a board that do not join up. These are data problems a map maker would not see in the
 * board editor but that change how units can move:
 * <ul>
 *     <li>a road exit that points at a hex with no road, pavement, bridge or building</li>
 *     <li>a road exit whose neighbouring road has no exit pointing back</li>
 *     <li>a road exit onto a bridge hex whose deck is two or more levels above or below the road, when the bridge
 *     itself has no exit back toward the road (otherwise the bridge-end check below reports it)</li>
 *     <li>a bridge that ends floating: it exits into a hex whose ground is more than one level from its deck, so
 *     nothing can get on or off</li>
 *     <li>a bridge end whose deck is two or more levels above or below the road or pavement it meets. A bridge may
 *     change height by only one level per hex (TO:AR p.115), and a ground vehicle cannot climb or drop more than
 *     that onto or off it.</li>
 * </ul>
 * {@link #findNotes(Board)} separately lists things that play correctly but are worth a map maker's look: a bridge deck
 * exactly one level off its road, drawn with a visible step in the isometric view, and a bridge that meets solid ground
 * at a reachable height but without the road hex TO:AR p.115 calls for. The checks read the exits as loaded, so a
 * board must be fully initialized first.
 */
public final class BoardConnectivityCheck {

    /** The largest height difference between a bridge deck and its road that a ground vehicle can cross. */
    static final int MAX_BRIDGE_END_STEP = 1;

    private static final String[] DIRECTION_NAMES = { "N", "NE", "SE", "S", "SW", "NW" };

    private BoardConnectivityCheck() {}

    /**
     * @param board the loaded board to check
     *
     * @return one issue per problem found, naming the hex and direction; empty when roads and bridges all join up
     */
    public static List<BoardIssue> findProblems(Board board) {
        List<BoardIssue> problems = new ArrayList<>();
        scan(board, problems, new ArrayList<>());
        return problems;
    }

    /**
     * @param board the loaded board to check
     *
     * @return one issue per bridge end that plays correctly but looks wrong or bends the rules: a deck exactly one level
     *       off its road, or an end on solid ground with no road
     */
    public static List<BoardIssue> findNotes(Board board) {
        List<BoardIssue> notes = new ArrayList<>();
        scan(board, new ArrayList<>(), notes);
        return notes;
    }

    /**
     * Checks a single hex, for a tooltip. Issues are reported at the road or bridge hex whose exit causes them.
     *
     * @param board  the loaded board
     * @param coords the hex to check
     *
     * @return the problems and then the notes for that hex; empty if it has none or is off the board
     */
    public static List<BoardIssue> findIssuesAt(Board board, Coords coords) {
        List<BoardIssue> problems = new ArrayList<>();
        List<BoardIssue> notes = new ArrayList<>();
        Hex hex = board.getHex(coords);
        if (hex != null) {
            checkHex(board, hex, coords.getX(), coords.getY(), problems, notes);
        }
        problems.addAll(notes);
        return problems;
    }

    private static void scan(Board board, List<BoardIssue> problems, List<BoardIssue> notes) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Hex hex = board.getHex(x, y);
                if (hex != null) {
                    checkHex(board, hex, x, y, problems, notes);
                }
            }
        }
    }

    private static void checkHex(Board board, Hex hex, int x, int y, List<BoardIssue> problems,
          List<BoardIssue> notes) {
        if (hex.containsTerrain(Terrains.ROAD)) {
            checkRoadExits(board, hex, x, y, problems);
        }
        if (hex.containsTerrain(Terrains.BRIDGE)) {
            checkBridgeEnds(board, hex, x, y, problems, notes);
        }
    }

    private static void checkRoadExits(Board board, Hex hex, int x, int y, List<BoardIssue> problems) {
        for (int direction = 0; direction < 6; direction++) {
            if (!hex.containsTerrainExit(Terrains.ROAD, direction)) {
                continue;
            }
            Hex neighbour = board.getHexInDir(x, y, direction);
            if (neighbour == null) {
                // A road may run off the edge of the board
                continue;
            }
            String here = hexName(x, y);
            String there = neighbourName(x, y, direction);
            String side = DIRECTION_NAMES[direction];
            int directionBack = (direction + 3) % 6;
            if (neighbour.containsTerrain(Terrains.BRIDGE) && !joinsGroundRoad(hex, neighbour)) {
                checkRoadOntoBridge(hex, neighbour, directionBack, x, y, direction, problems);
            } else if (neighbour.containsTerrain(Terrains.ROAD)) {
                // Includes a road that runs on under an overpass
                if (!neighbour.containsTerrainExit(Terrains.ROAD, directionBack)) {
                    problems.add(new BoardIssue(new Coords(x, y),
                          String.format("Road at %s exits %s, but the road at %s has no exit back", here, side, there),
                          String.format("Add the road exit at %s that points back to %s, or remove this road's %s "
                                + "exit", there, here, side)));
                }
            } else if (!continuesRoad(neighbour)) {
                problems.add(new BoardIssue(new Coords(x, y),
                      String.format("Road at %s exits %s into %s, which has no road, pavement, bridge or building", here,
                            side, there),
                      String.format("Remove this road's %s exit, or continue the road into %s", side, there)));
            }
        }
    }

    /**
     * A bridge hex can also hold a road on the ground beneath it, as at an overpass. The road joins that ground road,
     * rather than the deck, when it is within reach of the ground road and not of the deck.
     */
    private static boolean joinsGroundRoad(Hex road, Hex bridgeHex) {
        if (!bridgeHex.containsAnyTerrainOf(Terrains.ROAD, Terrains.PAVEMENT)) {
            return false;
        }
        int roadLevel = roadSurfaceLevel(road);
        int deckLevel = bridgeHex.getLevel() + bridgeHex.terrainLevel(Terrains.BRIDGE_ELEV);
        boolean reachesDeck = Math.abs(deckLevel - roadLevel) <= MAX_BRIDGE_END_STEP;
        boolean reachesGround = Math.abs(bridgeHex.getLevel() - roadLevel) <= MAX_BRIDGE_END_STEP;
        return reachesGround && !reachesDeck;
    }

    /**
     * Reports a road that runs onto a bridge deck it cannot reach. When the bridge has an exit back toward the road,
     * {@link #checkBridgeEnds} checks the height from the bridge side instead, so nothing is reported twice. A road
     * that meets a bridge at a reachable height is fine, whichever side carries the exit.
     */
    private static void checkRoadOntoBridge(Hex road, Hex bridge, int directionBack, int x, int y, int direction,
          List<BoardIssue> problems) {
        if (bridge.containsTerrainExit(Terrains.BRIDGE, directionBack)) {
            return;
        }
        int deckLevel = bridge.getLevel() + bridge.terrainLevel(Terrains.BRIDGE_ELEV);
        int roadLevel = roadSurfaceLevel(road);
        if (Math.abs(deckLevel - roadLevel) > MAX_BRIDGE_END_STEP) {
            String there = neighbourName(x, y, direction);
            problems.add(new BoardIssue(new Coords(x, y),
                  String.format("Road at %s exits %s onto the bridge at %s, but its deck at level %d is out of reach of "
                        + "the road at level %d", hexName(x, y), DIRECTION_NAMES[direction], there, deckLevel,
                        roadLevel),
                  String.format("Bring the deck within one level of the road: change the bridge elevation at %s, or "
                        + "the level of this hex", there)));
        }
    }

    private static void checkBridgeEnds(Board board, Hex hex, int x, int y, List<BoardIssue> problems,
          List<BoardIssue> notes) {
        int deckLevel = hex.getLevel() + hex.terrainLevel(Terrains.BRIDGE_ELEV);
        for (int direction = 0; direction < 6; direction++) {
            if (!hex.containsTerrainExit(Terrains.BRIDGE, direction)) {
                continue;
            }
            Hex neighbour = board.getHexInDir(x, y, direction);
            if ((neighbour == null) || neighbour.containsTerrain(Terrains.BRIDGE)) {
                // The bridge runs off the edge of the board, or its span continues
                continue;
            }
            String here = hexName(x, y);
            String there = neighbourName(x, y, direction);
            int groundLevel = roadSurfaceLevel(neighbour);
            int heightDifference = Math.abs(deckLevel - groundLevel);
            if (!neighbour.containsAnyTerrainOf(Terrains.ROAD, Terrains.PAVEMENT)) {
                if (heightDifference > MAX_BRIDGE_END_STEP) {
                    problems.add(new BoardIssue(new Coords(x, y),
                          String.format("Bridge at %s exits %s into %s and is left floating: its deck is at level %d "
                                + "and the ground there at level %d", here, DIRECTION_NAMES[direction], there,
                                deckLevel, groundLevel),
                          String.format("Add a road hex at %s level with the deck, or change this bridge's elevation",
                                there)));
                } else {
                    notes.add(new BoardIssue(new Coords(x, y),
                          String.format("Bridge at %s ends on %s without a road hex (TO:AR p.115)", here, there),
                          String.format("Add a road to %s", there)));
                }
                continue;
            }
            String description = String.format("Bridge at %s has its deck at level %d, but the road it meets at %s is"
                  + " at level %d", here, deckLevel, there, groundLevel);
            if (heightDifference > MAX_BRIDGE_END_STEP) {
                problems.add(new BoardIssue(new Coords(x, y), description,
                      String.format("Change this bridge's elevation so its deck is within one level of the road at %s, "
                            + "or raise or lower that road hex (a bridge may change height by one level per hex, "
                            + "TO:AR p.115)", there)));
            } else if (heightDifference == MAX_BRIDGE_END_STEP) {
                notes.add(new BoardIssue(new Coords(x, y), description + " (a one-level step)",
                      "This plays correctly. To remove the step, set the bridge elevation so the deck matches the "
                            + "road"));
            }
        }
    }

    /**
     * @return the level a unit in this hex stands at, on top of any building
     */
    private static int roadSurfaceLevel(Hex hex) {
        return hex.getLevel() + Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
    }

    /**
     * @return {@code true} if a road may end against this hex: pavement, a bridge or a building
     */
    private static boolean continuesRoad(Hex hex) {
        return hex.containsAnyTerrainOf(Terrains.PAVEMENT, Terrains.BRIDGE, Terrains.BUILDING, Terrains.FUEL_TANK);
    }

    private static String hexName(int x, int y) {
        return new Coords(x, y).getBoardNum();
    }

    private static String neighbourName(int x, int y, int direction) {
        return hexName(Coords.xInDir(x, y, direction), Coords.yInDir(x, y, direction));
    }
}
