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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrains;

/**
 * Tells whether building a gun emplacement on a hex would get in the way of ground traffic.
 *
 * <p>An emplacement is an armoured building, and only infantry may enter one, so an emplacement standing in a street
 * is a wall across it for every tank and for every 'Mek that cannot jump over. A defender covers a street by fire from
 * beside it; standing in it blocks the defender's own units as surely as the enemy's.</p>
 *
 * <p>Two things are checked. A hex that carries a road or a bridge is never a good site. And a hex is a bottleneck
 * when the open hexes around it cannot reach one another without passing through it or taking a detour of more than
 * {@link #DETOUR_RADIUS} hexes, which is what a street one hex wide looks like, or the last open lane of a wider
 * street whose other lane already holds an emplacement.</p>
 */
final class EmplacementStreetCheck {

    /** A way round the emplacement must stay within this many hexes of it to count. */
    static final int DETOUR_RADIUS = 3;

    /** The steepest change of level between two hexes that a ground vehicle can drive. */
    static final int MAX_LEVEL_STEP = 1;

    private EmplacementStreetCheck() {
    }

    /**
     * @param hex the hex to check; may be {@code null}
     *
     * @return {@code true} if the hex carries a road or a bridge
     */
    static boolean isOnRoad(@Nullable Hex hex) {
        return (hex != null) && (hex.containsTerrain(Terrains.ROAD) || hex.containsTerrain(Terrains.BRIDGE));
    }

    /**
     * @param board             the board
     * @param site              the hex an emplacement might be built on
     * @param otherEmplacements hexes that already hold an emplacement, which traffic cannot use either
     *
     * @return {@code true} if an emplacement here would cut the ground route between two of the hexes beside it
     */
    static boolean blocksPassage(Board board, Coords site, Collection<Coords> otherEmplacements) {
        Hex siteHex = board.getHex(site);
        if (siteHex == null) {
            return false;
        }
        List<Coords> waysOut = new ArrayList<>();
        for (int direction = 0; direction < 6; direction++) {
            Coords neighbour = site.translated(direction);
            if (isDrivable(board, neighbour, otherEmplacements) && canStep(siteHex, board.getHex(neighbour))) {
                waysOut.add(neighbour);
            }
        }
        if (waysOut.size() < 2) {
            // a dead end: nothing passes through here on its way somewhere else
            return false;
        }
        Set<Coords> reached = reachableWithout(board, site, waysOut.getFirst(), otherEmplacements);
        return !reached.containsAll(waysOut);
    }

    /**
     * Floods outward from a hex over drivable ground, never entering the site and never straying further than
     * {@link #DETOUR_RADIUS} from it.
     */
    private static Set<Coords> reachableWithout(Board board, Coords site, Coords start,
          Collection<Coords> otherEmplacements) {
        Set<Coords> reached = new HashSet<>();
        Deque<Coords> frontier = new ArrayDeque<>();
        reached.add(start);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            Coords current = frontier.poll();
            Hex currentHex = board.getHex(current);
            for (int direction = 0; direction < 6; direction++) {
                Coords next = current.translated(direction);
                boolean isNewGround = !next.equals(site) && !reached.contains(next);
                boolean isShortDetour = next.distance(site) <= DETOUR_RADIUS;
                boolean isReachable = isDrivable(board, next, otherEmplacements)
                      && canStep(currentHex, board.getHex(next));
                if (isNewGround && isShortDetour && isReachable) {
                    reached.add(next);
                    frontier.add(next);
                }
            }
        }
        return reached;
    }

    /** Whether a ground vehicle could stand on the hex: on the board, dry, and not a building or an emplacement. */
    static boolean isDrivable(Board board, Coords coords, Collection<Coords> otherEmplacements) {
        Hex hex = board.getHex(coords);
        if ((hex == null) || otherEmplacements.contains(coords)) {
            return false;
        }
        boolean isSolid = hex.containsTerrain(Terrains.BUILDING) || hex.containsTerrain(Terrains.IMPASSABLE);
        boolean isBridged = hex.containsTerrain(Terrains.BRIDGE);
        return !isSolid && (isBridged || !EmplacementDeployment.isUnsuitableSite(hex));
    }

    static boolean canStep(@Nullable Hex from, @Nullable Hex to) {
        return (from != null) && (to != null) && (Math.abs(from.getLevel() - to.getLevel()) <= MAX_LEVEL_STEP);
    }
}
