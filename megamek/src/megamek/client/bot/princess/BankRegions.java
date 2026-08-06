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

import java.util.ArrayDeque;
import java.util.Deque;

import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * Labels every dry hex of a board with its connected dry region - its bank - so "are these two positions on
 * the same side of the water" is answered by whether a unit could walk from one to the other without getting
 * wet.
 *
 * <p>The straight-line test this replaces ({@code Coords.intervening} checked hex by hex) is wrong on a river
 * that meanders: two positions on the same bank have a chord that clips a bend, so a defender repositioning
 * along its own shore read as crossing the river. The positions that suffered most were exactly the ones a
 * defender wants - firing positions at the water's edge, where almost any move to a neighboring overwatch
 * position chords across a loop of the river. Charged for every step along its own bank, the force drifted
 * back into dead ground where it could see nothing and shoot nothing.</p>
 *
 * <p>Connectivity has no such artifacts: bends, loops and islands come out right by construction, and a jump
 * that lands on the far bank changes region even though it never touched the water. The whole board labels in
 * one flood fill, after which each lookup is constant time - cheaper per path than the line walk it
 * replaces.</p>
 */
final class BankRegions {

    /** The region of hexes that are water (or off the board): not a bank at all. */
    static final int WATER = -1;

    private final int width;
    private final int height;
    private final int[] regionByHex;

    private BankRegions(int width, int height, int[] regionByHex) {
        this.width = width;
        this.height = height;
        this.regionByHex = regionByHex;
    }

    /**
     * Labels the given board's dry hexes with their connected regions.
     *
     * @param board        the board to label
     * @param minimumDepth the shallowest water that counts as wet, in levels - see
     *                     {@link FormationSide#ANY_WATER_DEPTH}
     *
     * @return the labeled regions
     */
    static BankRegions of(Board board, int minimumDepth) {
        int width = board.getWidth();
        int height = board.getHeight();
        int[] regions = new int[width * height];
        java.util.Arrays.fill(regions, WATER);

        int nextRegion = 0;
        Deque<Coords> frontier = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Coords start = new Coords(x, y);
                if ((regions[x * height + y] != WATER) || isWet(board, start, minimumDepth)) {
                    continue;
                }
                int region = nextRegion++;
                regions[x * height + y] = region;
                frontier.push(start);
                while (!frontier.isEmpty()) {
                    for (Coords neighbor : frontier.pop().allAdjacent()) {
                        int neighborX = neighbor.getX();
                        int neighborY = neighbor.getY();
                        if ((neighborX < 0) || (neighborX >= width) || (neighborY < 0) || (neighborY >= height)) {
                            continue;
                        }
                        int index = neighborX * height + neighborY;
                        if ((regions[index] == WATER) && !isWet(board, neighbor, minimumDepth)) {
                            regions[index] = region;
                            frontier.push(neighbor);
                        }
                    }
                }
            }
        }
        return new BankRegions(width, height, regions);
    }

    private static boolean isWet(Board board, Coords coords, int minimumDepth) {
        return FormationSide.isSeparatingWater(board, coords, minimumDepth);
    }

    /**
     * The bank a position stands on.
     *
     * @param coords the position, or {@code null}
     *
     * @return its region label, or {@link #WATER} for water hexes, off-board positions and {@code null}
     */
    int regionOf(@Nullable Coords coords) {
        if ((coords == null) || (coords.getX() < 0) || (coords.getX() >= width)
              || (coords.getY() < 0) || (coords.getY() >= height)) {
            return WATER;
        }
        return regionByHex[coords.getX() * height + coords.getY()];
    }

    /**
     * Whether a unit could walk between the two positions without getting wet.
     *
     * @param from one position
     * @param to   the other
     *
     * @return {@code true} when both are dry and on the same connected bank
     */
    boolean sameBank(@Nullable Coords from, @Nullable Coords to) {
        int fromRegion = regionOf(from);
        return (fromRegion != WATER) && (fromRegion == regionOf(to));
    }
}
