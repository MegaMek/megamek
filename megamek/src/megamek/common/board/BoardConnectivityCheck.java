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
     * @return one line per problem found, naming the hex and direction; empty when roads and bridges all join up
     */
    public static List<String> findProblems(Board board) {
        List<String> problems = new ArrayList<>();
        scan(board, problems, new ArrayList<>());
        return problems;
    }

    /**
     * @param board the loaded board to check
     *
     * @return one line per bridge end that plays correctly but looks wrong or bends the rules: a deck exactly one level
     *       off its road, or an end on solid ground with no road
     */
    public static List<String> findNotes(Board board) {
        List<String> notes = new ArrayList<>();
        scan(board, new ArrayList<>(), notes);
        return notes;
    }

    private static void scan(Board board, List<String> problems, List<String> notes) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Hex hex = board.getHex(x, y);
                if (hex == null) {
                    continue;
                }
                if (hex.containsTerrain(Terrains.ROAD)) {
                    checkRoadExits(board, hex, x, y, problems);
                }
                if (hex.containsTerrain(Terrains.BRIDGE)) {
                    checkBridgeEnds(board, hex, x, y, problems, notes);
                }
            }
        }
    }

    private static void checkRoadExits(Board board, Hex hex, int x, int y, List<String> problems) {
        for (int direction = 0; direction < 6; direction++) {
            if (!hex.containsTerrainExit(Terrains.ROAD, direction)) {
                continue;
            }
            Hex neighbour = board.getHexInDir(x, y, direction);
            if (neighbour == null) {
                // A road may run off the edge of the board
                continue;
            }
            int directionBack = (direction + 3) % 6;
            if (neighbour.containsTerrain(Terrains.BRIDGE) && !joinsGroundRoad(hex, neighbour)) {
                checkRoadOntoBridge(hex, neighbour, directionBack, x, y, direction, problems);
            } else if (neighbour.containsTerrain(Terrains.ROAD)) {
                // Includes a road that runs on under an overpass
                if (!neighbour.containsTerrainExit(Terrains.ROAD, directionBack)) {
                    problems.add(String.format("Road at %s exits %s, but the road at %s has no exit back",
                          hexName(x, y), DIRECTION_NAMES[direction], neighbourName(x, y, direction)));
                }
            } else if (!continuesRoad(neighbour)) {
                problems.add(String.format("Road at %s exits %s into %s, which has no road, pavement, bridge or "
                      + "building", hexName(x, y), DIRECTION_NAMES[direction], neighbourName(x, y, direction)));
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
          List<String> problems) {
        if (bridge.containsTerrainExit(Terrains.BRIDGE, directionBack)) {
            return;
        }
        int deckLevel = bridge.getLevel() + bridge.terrainLevel(Terrains.BRIDGE_ELEV);
        int roadLevel = roadSurfaceLevel(road);
        if (Math.abs(deckLevel - roadLevel) > MAX_BRIDGE_END_STEP) {
            problems.add(String.format("Road at %s exits %s onto the bridge at %s, but its deck at level %d is out of "
                  + "reach of the road at level %d", hexName(x, y), DIRECTION_NAMES[direction],
                  neighbourName(x, y, direction), deckLevel, roadLevel));
        }
    }

    private static void checkBridgeEnds(Board board, Hex hex, int x, int y, List<String> problems,
          List<String> notes) {
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
            if (!neighbour.containsAnyTerrainOf(Terrains.ROAD, Terrains.PAVEMENT)) {
                int groundLevel = roadSurfaceLevel(neighbour);
                if (Math.abs(deckLevel - groundLevel) > MAX_BRIDGE_END_STEP) {
                    problems.add(String.format("Bridge at %s exits %s into %s and is left floating: its deck is at "
                          + "level %d and the ground there at level %d", hexName(x, y), DIRECTION_NAMES[direction],
                          neighbourName(x, y, direction), deckLevel, groundLevel));
                } else {
                    notes.add(String.format("Bridge at %s ends on %s without a road hex (TO:AR p.115)", hexName(x, y),
                          neighbourName(x, y, direction)));
                }
                continue;
            }
            int roadLevel = roadSurfaceLevel(neighbour);
            int heightDifference = Math.abs(deckLevel - roadLevel);
            String description = String.format("Bridge at %s has its deck at level %d, but the road it meets at %s is"
                  + " at level %d", hexName(x, y), deckLevel, neighbourName(x, y, direction), roadLevel);
            if (heightDifference > MAX_BRIDGE_END_STEP) {
                problems.add(description);
            } else if (heightDifference == MAX_BRIDGE_END_STEP) {
                notes.add(description + " (a one-level step)");
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
