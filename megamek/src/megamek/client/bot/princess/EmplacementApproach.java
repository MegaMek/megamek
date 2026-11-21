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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrains;

/**
 * Works out what a set of gun emplacements is there to defend and how the enemy will come at it, so that ground can
 * be weighed by whether it matters and not only by whether it can be hit.
 *
 * <p>Without this an emplacement given a free choice takes the widest view on the board, which is a hilltop outside
 * the town: a fine firing position that defends nothing. Two things tie it down.</p>
 *
 * <ul>
 *     <li><b>The place defended</b> is the built-up area: the middle of the board's building hexes, with a radius that
 *     takes in half of them. On a board with no buildings it is the middle of the hexes the emplacement may
 *     deploy on. Ground counts in full inside that radius, for less the further outside it lies, and for nothing
 *     once it is far enough out, so that no emplacement is built forward on the enemy's own edge of the board.</li>
 *     <li><b>The enemy's routes</b> are the hexes a ground vehicle would drive through to get from the middle of the
 *     enemy's deployment zone to the place defended, by any route no more than {@link #ROUTE_SLACK} hexes longer than
 *     the shortest. These run down the streets and over the bridges, and ground on them counts
 *     {@link #ROUTE_WEIGHT} times over.</li>
 * </ul>
 *
 * @param defended the hex at the middle of the place defended
 * @param radius   how far from it ground still counts in full
 * @param routes   the hexes on the enemy's routes in; empty when the enemy is unknown or cannot drive there
 * @param front    the front line: the route hexes at the edge of the place defended, where the enemy first comes
 *                 under its guns; empty when there are no routes
 */
record EmplacementApproach(Coords defended, int radius, Set<Coords> routes, Set<Coords> front) {

    /** How many hexes longer than the shortest a route may be and still count as a way in. */
    static final int ROUTE_SLACK = 2;

    /** How much more a hex on one of the enemy's routes is worth. */
    static final int ROUTE_WEIGHT = 10;

    /** What each hex beyond the radius costs, in hundredths of the ground's value. */
    static final int FALL_OFF_PERCENT_PER_HEX = 15;

    /** How far outside the radius an emplacement may still stand and count as part of the defence. */
    static final int FORWARD_MARGIN = 4;

    /** What each hex away from its proper distance behind the front costs an emplacement, in hundredths. */
    static final int DEPTH_PERCENT_PER_HEX = 6;

    /** The least an emplacement at the wrong depth keeps of its score, in hundredths. */
    static final int MINIMUM_DEPTH_PERCENT = 50;

    /** How far to look for drivable ground beside a hex that is not drivable itself. */
    private static final int NEAREST_GROUND_SEARCH = 6;

    /**
     * @param board       the board
     * @param candidates  the hexes the emplacement may deploy on
     * @param enemyCentre the middle of the enemy's deployment zones, or {@code null} when unknown
     *
     * @return what is defended on this board and how the enemy reaches it
     */
    static EmplacementApproach plan(Board board, List<Coords> candidates, @Nullable Coords enemyCentre) {
        List<Coords> builtUp = buildingHexes(board);
        List<Coords> ground = builtUp.isEmpty() ? candidates : builtUp;
        if (ground.isEmpty()) {
            return new EmplacementApproach(new Coords(0, 0), Integer.MAX_VALUE, Set.of(), Set.of());
        }
        Coords defended = middleOf(ground);
        int radius = radiusTakingInHalf(defended, ground);
        Set<Coords> routes = (enemyCentre == null) ? Set.of() : routesBetween(board, enemyCentre, defended);
        return new EmplacementApproach(defended, radius, routes, frontOf(routes, defended, radius));
    }

    /**
     * The route hexes on the edge of the place defended. When the enemy's routes never reach that far out, the route
     * hexes furthest from the middle stand in for it.
     */
    static Set<Coords> frontOf(Set<Coords> routes, Coords defended, int radius) {
        int furthest = 0;
        for (Coords route : routes) {
            furthest = Math.max(furthest, route.distance(defended));
        }
        int edge = Math.min(radius, furthest);
        Set<Coords> front = new HashSet<>();
        for (Coords route : routes) {
            int distance = route.distance(defended);
            if ((distance == edge) || (distance == (edge + 1))) {
                front.add(route);
            }
        }
        return front;
    }

    /**
     * How well a hex suits an emplacement that fights best a given distance behind the front line. An emplacement
     * of short-range weapons belongs in the line and one of long-range weapons behind it, so each loses a little of
     * its score for every hex it stands nearer or further than that.
     *
     * @param site     the hex an emplacement might be built on
     * @param standoff how far behind the front it belongs, from {@link EmplacementFirepower#standoff}
     *
     * @return the share of its score the emplacement keeps there, in hundredths; 100 when there is no front
     */
    int depthPercent(Coords site, int standoff) {
        if (front.isEmpty()) {
            return 100;
        }
        int nearest = Integer.MAX_VALUE;
        for (Coords frontHex : front) {
            nearest = Math.min(nearest, site.distance(frontHex));
        }
        int hexesOutOfPlace = Math.abs(nearest - standoff);
        return Math.max(MINIMUM_DEPTH_PERCENT, 100 - (hexesOutOfPlace * DEPTH_PERCENT_PER_HEX));
    }

    /**
     * @param hex a hex an emplacement overlooks
     *
     * @return what the hex is worth to the defence, in hundredths: 100 for ordinary ground inside the radius, 0 for
     *       ground too far out to matter
     */
    int weightPercent(Coords hex) {
        int beyond = Math.max(0, hex.distance(defended) - radius);
        int nearness = Math.max(0, 100 - (beyond * FALL_OFF_PERCENT_PER_HEX));
        return routes.contains(hex) ? (nearness * ROUTE_WEIGHT) : nearness;
    }

    /**
     * Drops the hexes that lie too far from the place defended to be part of its defence, such as a hilltop beside
     * the edge the enemy enters by. When no hex is near enough, as when the deployment zone is away from the town,
     * every hex is kept.
     *
     * @param candidates hexes the emplacement may deploy on
     *
     * @return those within {@link #FORWARD_MARGIN} of the radius, or all of them when there are none
     */
    List<Coords> withinTheDefence(List<Coords> candidates) {
        List<Coords> near = new ArrayList<>();
        for (Coords candidate : candidates) {
            if ((candidate.distance(defended) - radius) <= FORWARD_MARGIN) {
                near.add(candidate);
            }
        }
        return near.isEmpty() ? candidates : near;
    }

    private static List<Coords> buildingHexes(Board board) {
        List<Coords> builtUp = new ArrayList<>();
        for (int column = 0; column < board.getWidth(); column++) {
            for (int row = 0; row < board.getHeight(); row++) {
                Coords coords = new Coords(column, row);
                Hex hex = board.getHex(coords);
                if ((hex != null) && hex.containsTerrain(Terrains.BUILDING)) {
                    builtUp.add(coords);
                }
            }
        }
        return builtUp;
    }

    private static Coords middleOf(List<Coords> hexes) {
        long totalX = 0;
        long totalY = 0;
        for (Coords hex : hexes) {
            totalX += hex.getX();
            totalY += hex.getY();
        }
        return new Coords((int) Math.round((double) totalX / hexes.size()),
              (int) Math.round((double) totalY / hexes.size()));
    }

    /** The distance from the middle that takes in half of the hexes, so outlying farms and suburbs are ignored. */
    private static int radiusTakingInHalf(Coords middle, List<Coords> hexes) {
        List<Integer> distances = new ArrayList<>();
        for (Coords hex : hexes) {
            distances.add(hex.distance(middle));
        }
        Collections.sort(distances);
        return distances.get(distances.size() / 2);
    }

    /**
     * The hexes on every drivable route from one hex to another that is at most {@link #ROUTE_SLACK} longer than the
     * shortest.
     */
    static Set<Coords> routesBetween(Board board, Coords from, Coords to) {
        Coords start = nearestDrivable(board, from);
        Coords end = nearestDrivable(board, to);
        if ((start == null) || (end == null)) {
            return Set.of();
        }
        Map<Coords, Integer> fromStart = drivingDistances(board, start);
        Map<Coords, Integer> fromEnd = drivingDistances(board, end);
        Integer shortest = fromStart.get(end);
        if (shortest == null) {
            return Set.of();
        }
        Set<Coords> routes = new HashSet<>();
        for (Map.Entry<Coords, Integer> reached : fromStart.entrySet()) {
            Integer onward = fromEnd.get(reached.getKey());
            if ((onward != null) && ((reached.getValue() + onward) <= (shortest + ROUTE_SLACK))) {
                routes.add(reached.getKey());
            }
        }
        return routes;
    }

    private static @Nullable Coords nearestDrivable(Board board, Coords hex) {
        for (int distance = 0; distance <= NEAREST_GROUND_SEARCH; distance++) {
            for (Coords near : hex.allAtDistance(distance)) {
                if (board.contains(near) && EmplacementStreetCheck.isDrivable(board, near, Set.of())) {
                    return near;
                }
            }
        }
        return null;
    }

    /** How many hexes a ground vehicle drives to reach each hex it can reach from the start. */
    private static Map<Coords, Integer> drivingDistances(Board board, Coords start) {
        Map<Coords, Integer> distances = new HashMap<>();
        Deque<Coords> frontier = new ArrayDeque<>();
        distances.put(start, 0);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            Coords current = frontier.poll();
            Hex currentHex = board.getHex(current);
            for (int direction = 0; direction < 6; direction++) {
                Coords next = current.translated(direction);
                boolean isNewGround = board.contains(next) && !distances.containsKey(next);
                boolean isReachable = EmplacementStreetCheck.isDrivable(board, next, Set.of())
                      && EmplacementStreetCheck.canStep(currentHex, board.getHex(next));
                if (isNewGround && isReachable) {
                    distances.put(next, distances.get(current) + 1);
                    frontier.add(next);
                }
            }
        }
        return distances;
    }
}
