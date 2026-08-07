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

import java.util.List;

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrains;

/**
 * Decides which friendly units are on the mover's side of a water obstacle, so a force split by a river forms up
 * with the units it can actually reach.
 *
 * <p>A formation's centre is the mean position of the force. When a river runs between the two halves of that
 * force, the mean lands in the water - a hex no unit can stand in, measured on a river crossing as 18% of the
 * distinct centre positions in a run. Every unit is then held to a point in the middle of the river, which is the
 * one place the formation cannot be.</p>
 *
 * <p>The rule here is deliberately narrow: a friend counts toward the formation only if the straight line to it
 * does not cross deep water. Units form up with their own bank, the far bank forms up with itself, and as units
 * cross, the near group shrinks and the far group grows until the force is whole again on the far side. That is
 * how a force actually crosses a river - it assembles on the far bank - rather than trying to hold one shape
 * across an obstacle it can only pass a few units at a time.</p>
 *
 * <p><b>Depth matters.</b> Only water deep enough to force a unit to wade separates a formation
 * ({@link #SEPARATING_DEPTH}). A shallow ford costs movement but does not break a force in two, and treating
 * every puddle as a barrier would fragment formations on wet maps for no reason.</p>
 *
 * @see MutualSupportPathRanker the doctrine that uses this to place its formation centre
 */
public final class FormationSide {

    /**
     * Minimum water depth that counts as separating a force, in levels.
     *
     * <p>Depth 1 is a ford: a Mek wades it, pays movement for it, and the force stays a force. Depth 2 and beyond
     * submerges legs and forces the piloting rolls that actually break a crossing up into single units, so that is
     * where a formation stops being one body.</p>
     */
    static final int SEPARATING_DEPTH = 2;

    /**
     * Minimum water depth that counts as a defensive line: any water at all.
     *
     * <p>The formation rule above deliberately ignores fords, because a ford does not break a force in two. A
     * defender's river is the opposite case: a fordable river is still the line the defender holds - more so,
     * because the enemy can cross it anywhere. Measured the hard way: a defending company on a river of
     * depth-1 fords crossed at will for a whole game, because its crossing charge only counted depth 2.</p>
     */
    static final int ANY_WATER_DEPTH = 1;

    private FormationSide() {
    }

    /**
     * Whether two positions are on the same side of any water obstacle deep enough to split a formation.
     *
     * @param board  the board both positions are on
     * @param from   the mover's position
     * @param to     the friend's position
     *
     * @return {@code true} when the straight line between them crosses no separating water, and so the two can
     *       form up together
     */
    public static boolean sameSide(@Nullable Board board, @Nullable Coords from, @Nullable Coords to) {
        return sameSide(board, from, to, SEPARATING_DEPTH);
    }

    /**
     * Whether two positions are on the same side of any water of at least the given depth.
     *
     * @param board        the board both positions are on
     * @param from         the mover's position
     * @param to           the other position
     * @param minimumDepth the shallowest water that counts as a divide, in levels
     *
     * @return {@code true} when the straight line between them crosses no such water
     */
    public static boolean sameSide(@Nullable Board board, @Nullable Coords from, @Nullable Coords to,
          int minimumDepth) {
        if ((board == null) || (from == null) || (to == null)) {
            return true;
        }
        if (from.equals(to)) {
            return true;
        }
        for (Coords step : Coords.intervening(from, to)) {
            if (isSeparatingWater(board, step, minimumDepth)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a hex holds water deep enough to split a formation.
     *
     * @param board  the board to read
     * @param coords the hex to test
     *
     * @return {@code true} when the hex is on the board and holds water at {@link #SEPARATING_DEPTH} or deeper
     */
    static boolean isSeparatingWater(Board board, @Nullable Coords coords) {
        return isSeparatingWater(board, coords, SEPARATING_DEPTH);
    }

    /**
     * Whether a hex holds water at least the given depth.
     *
     * @param board        the board to read
     * @param coords       the hex to test
     * @param minimumDepth the shallowest water that counts, in levels
     *
     * @return {@code true} when the hex is on the board and holds water at that depth or deeper
     */
    static boolean isSeparatingWater(Board board, @Nullable Coords coords, int minimumDepth) {
        if ((coords == null) || !board.contains(coords)) {
            return false;
        }
        Hex hex = board.getHex(coords);
        if ((hex == null) || !hex.containsTerrain(Terrains.WATER)) {
            return false;
        }
        return hex.terrainLevel(Terrains.WATER) >= minimumDepth;
    }

    /**
     * Whether any separating water lies between the mover and the given friends at all.
     *
     * <p>Lets a caller skip the per-friend filtering entirely on the overwhelmingly common case of a dry map,
     * where the answer is always the whole force.</p>
     *
     * @param board     the board the force is on
     * @param from      the mover's position
     * @param positions the friends' positions
     *
     * @return {@code true} when at least one friend is cut off from the mover by water
     */
    public static boolean isForceSplit(@Nullable Board board, @Nullable Coords from, List<Coords> positions) {
        if ((board == null) || (from == null)) {
            return false;
        }
        for (Coords position : positions) {
            if (!sameSide(board, from, position)) {
                return true;
            }
        }
        return false;
    }
}
