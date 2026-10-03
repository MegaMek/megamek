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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.orders.LanceRole;

/**
 * Where an escort lance keeps round its convoy: a place ahead of the convoy's head, beside its middle, or behind its
 * tail, with "ahead" pointing at the convoy's next waypoint, so the places swing round as the convoy turns at each
 * waypoint (HammerGS, 2026-10-02).
 */
final class EscortPlanner {

    private static final int DIRECTIONS = 6;
    private static final int BACKWARD = 3;
    // a second escort sharing a place keeps this far behind the first, so they do not queue for one hex
    private static final int SHARED_PLACE_GAP = 2;

    private EscortPlanner() {
    }

    /**
     * @param convoy   the convoy
     * @param position where round it
     * @param distance how many hexes out
     * @param board    the board, to keep the place on it
     *
     * @return the place: ahead of the head, beside the middle, or behind the tail; pulled in toward the convoy where
     *       it would be off the board
     */
    static Coords place(ConvoyTracker.Shape convoy, LanceRole.Position position, int distance, Board board) {
        int heading = convoy.heading();
        Coords anchor = switch (position) {
            case LEAD -> convoy.head();
            case REAR -> convoy.tail();
            case LEFT, RIGHT -> convoy.middle();
        };
        Coords place = switch (position) {
            case LEAD -> anchor.translated(heading, distance);
            case REAR -> anchor.translated((heading + BACKWARD) % DIRECTIONS, distance);
            case LEFT -> sideways(anchor, heading, true, distance);
            case RIGHT -> sideways(anchor, heading, false, distance);
        };
        return keepOnBoard(place, anchor, board);
    }

    /**
     * Square to the heading: hex sides are 60 degrees apart, so the steps alternate between the sides 60 and 120
     * degrees off the heading, which comes out square.
     */
    static Coords sideways(Coords from, int heading, boolean isLeft, int distance) {
        int nearSide = isLeft ? (heading + 5) % DIRECTIONS : (heading + 1) % DIRECTIONS;
        int farSide = isLeft ? (heading + 4) % DIRECTIONS : (heading + 2) % DIRECTIONS;
        Coords place = from;
        for (int step = 0; step < distance; step++) {
            place = place.translated(((step % 2) == 0) ? nearSide : farSide);
        }
        return place;
    }

    private static Coords keepOnBoard(Coords place, Coords anchor, Board board) {
        if (!board.contains(anchor)) {
            return ConvoyTracker.clampToBoard(place, board);
        }
        Coords onBoard = place;
        while (!board.contains(onBoard)) {
            onBoard = onBoard.translated(onBoard.direction(anchor));
        }
        return onBoard;
    }

    /**
     * Gives each escort a place. Every place is filled before any gets a second unit; a place the role leaves out is
     * never used. An escort keeps the place it had while that place is still one of the role's, so the lance does not
     * swap round each turn; a new or freed escort takes the nearest open place.
     *
     * @param escortPositions the escorts' units by id, where each stands now
     * @param positions       the places the role sets, in order: Lead, Left, Right, Rear
     * @param places          the hex of each place this turn
     * @param previous        the place each escort had last turn
     *
     * @return the place of each escort, by unit id
     */
    static Map<Integer, LanceRole.Position> assign(Map<Integer, Coords> escortPositions,
          List<LanceRole.Position> positions, Map<LanceRole.Position, Coords> places,
          Map<Integer, LanceRole.Position> previous) {
        Map<Integer, LanceRole.Position> assigned = new HashMap<>();
        Map<LanceRole.Position, Integer> unitsAt = new EnumMap<>(LanceRole.Position.class);
        int fairShare = (positions.isEmpty()) ? 0 : (escortPositions.size() + positions.size() - 1) / positions.size();
        for (Map.Entry<Integer, LanceRole.Position> kept : previous.entrySet()) {
            LanceRole.Position position = kept.getValue();
            if (escortPositions.containsKey(kept.getKey()) && positions.contains(position)
                  && (unitsAt.getOrDefault(position, 0) < fairShare)) {
                assigned.put(kept.getKey(), position);
                unitsAt.merge(position, 1, Integer::sum);
            }
        }
        List<Integer> waiting = new ArrayList<>();
        for (int unitId : escortPositions.keySet()) {
            if (!assigned.containsKey(unitId)) {
                waiting.add(unitId);
            }
        }
        waiting.sort(Integer::compare);
        while (!waiting.isEmpty()) {
            // the least-filled place first, in the role's order, then the waiting escort nearest it
            LanceRole.Position emptiest = positions.get(0);
            for (LanceRole.Position position : positions) {
                if (unitsAt.getOrDefault(position, 0) < unitsAt.getOrDefault(emptiest, 0)) {
                    emptiest = position;
                }
            }
            Coords placeHex = places.get(emptiest);
            int nearest = waiting.get(0);
            for (int unitId : waiting) {
                if (escortPositions.get(unitId).distance(placeHex)
                      < escortPositions.get(nearest).distance(placeHex)) {
                    nearest = unitId;
                }
            }
            assigned.put(nearest, emptiest);
            unitsAt.merge(emptiest, 1, Integer::sum);
            waiting.remove(Integer.valueOf(nearest));
        }
        return assigned;
    }

    /**
     * @param place   a place's hex
     * @param heading the convoy's heading
     * @param order   0 for the first escort at the place, 1 for a second
     *
     * @return the hex for that escort: the place itself, or a little behind it for a second escort sharing it
     */
    static Coords sharedPlace(Coords place, int heading, int order) {
        return place.translated((heading + BACKWARD) % DIRECTIONS, SHARED_PLACE_GAP * order);
    }
}
