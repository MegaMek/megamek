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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Terrains;

/**
 * Labels every hex of a board with its {@link HexProperties}, once per round, so position evaluation is a
 * constant-time lookup per path.
 *
 * <p>The build asks the rules engine, not terrain lists: concealment is what
 * {@link Compute#getTargetTerrainModifier} says fire against a standing Mek in that hex suffers, probed
 * with a reference Mek walked across the board - the engine only grants terrain cover to entity targets,
 * and how much depends on the occupant's height against the foliage. Any terrain the rules make concealing
 * is concealing here without this class knowing its name. Bank labels come from the {@link BankRegions}
 * flood fill. The passes are cheap - a board is about a thousand hexes - and rebuilt per round so anything
 * that changes terrain mid-game cannot leave the labels stale.</p>
 */
final class HexPropertiesMap {

    /** How far the local-dominance window reaches when deciding whether a hex overlooks its ground. */
    private static final int OVERLOOK_WINDOW_RADIUS = 3;

    /** How many levels above the surrounding ground a hex must sit to overlook it. */
    private static final int OVERLOOK_MARGIN = 2;

    private final int width;
    private final int height;
    private final HexProperties[] propertiesByHex;

    private HexPropertiesMap(int width, int height, HexProperties[] propertiesByHex) {
        this.width = width;
        this.height = height;
        this.propertiesByHex = propertiesByHex;
    }

    /**
     * Builds the labels for one board.
     *
     * @param game    the current game, for the rules-engine queries
     * @param board   the board to label
     * @param boardId the board's id in the game
     *
     * @return the labeled map
     */
    static HexPropertiesMap of(Game game, Board board, int boardId) {
        int width = board.getWidth();
        int height = board.getHeight();
        BankRegions banks = BankRegions.of(board, FormationSide.ANY_WATER_DEPTH);

        // First pass: per-hex facts that need no neighbors. The engine only grants terrain cover to
        // entity targets (a hex target is the ground, not something hiding on it), so the probe is a
        // reference Mek stood in each hex in turn - never added to the game, just asked about.
        BipedMek referenceMek = new BipedMek();
        referenceMek.setBoardId(boardId);
        int[] concealment = new int[width * height];
        int[] levels = new int[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Coords coords = new Coords(x, y);
                Hex hex = board.getHex(coords);
                if (hex == null) {
                    continue;
                }
                int index = x * height + y;
                levels[index] = hex.getLevel();
                referenceMek.setPosition(coords);
                concealment[index] = Compute.getTargetTerrainModifier(game, referenceMek).getValue();
            }
        }

        // Second pass: the neighbor-dependent facts, then the record per hex.
        HexProperties[] properties = new HexProperties[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Coords coords = new Coords(x, y);
                Hex hex = board.getHex(coords);
                int index = x * height + y;
                if (hex == null) {
                    properties[index] = HexProperties.NOTHING;
                    continue;
                }
                int waterDepth = hex.containsTerrain(Terrains.WATER) ? hex.terrainLevel(Terrains.WATER) : 0;
                boolean concealmentEdge = (concealment[index] > 0)
                      && hasOpenNeighbor(concealment, coords, width, height);
                boolean overlooks = levels[index]
                      >= medianLevelAround(levels, coords, width, height) + OVERLOOK_MARGIN;
                properties[index] = new HexProperties(
                      banks.regionOf(coords),
                      waterDepth == 1,
                      concealment[index],
                      concealmentEdge,
                      levels[index],
                      overlooks,
                      waterDepth >= 1);
            }
        }
        return new HexPropertiesMap(width, height, properties);
    }

    private static boolean hasOpenNeighbor(int[] concealment, Coords coords, int width, int height) {
        for (Coords neighbor : coords.allAdjacent()) {
            int neighborX = neighbor.getX();
            int neighborY = neighbor.getY();
            if ((neighborX < 0) || (neighborX >= width) || (neighborY < 0) || (neighborY >= height)) {
                continue;
            }
            if (concealment[neighborX * height + neighborY] == 0) {
                return true;
            }
        }
        return false;
    }

    private static int medianLevelAround(int[] levels, Coords coords, int width, int height) {
        List<Integer> window = new ArrayList<>();
        for (int x = coords.getX() - OVERLOOK_WINDOW_RADIUS; x <= coords.getX() + OVERLOOK_WINDOW_RADIUS; x++) {
            for (int y = coords.getY() - OVERLOOK_WINDOW_RADIUS; y <= coords.getY() + OVERLOOK_WINDOW_RADIUS; y++) {
                if ((x < 0) || (x >= width) || (y < 0) || (y >= height)
                      || ((x == coords.getX()) && (y == coords.getY()))) {
                    continue;
                }
                window.add(levels[x * height + y]);
            }
        }
        if (window.isEmpty()) {
            return 0;
        }
        Collections.sort(window);
        return window.get(window.size() / 2);
    }

    /**
     * The properties of a position.
     *
     * @param coords the position, or {@code null}
     *
     * @return its properties; off-board and {@code null} positions report {@link HexProperties#NOTHING}
     */
    HexProperties at(@Nullable Coords coords) {
        if ((coords == null) || (coords.getX() < 0) || (coords.getX() >= width)
              || (coords.getY() < 0) || (coords.getY() >= height)) {
            return HexProperties.NOTHING;
        }
        return propertiesByHex[coords.getX() * height + coords.getY()];
    }
}
