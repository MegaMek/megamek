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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.orders.RouteStyle;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;

/**
 * Plans a unit's whole way to a waypoint at once and finds the turning points on it, so the unit drives straight from
 * one to the next. On a hex map, heading straight at a hill and then along its foot to a pass often costs no more
 * than heading straight for the pass, so a unit choosing turn by turn did the first and looked as if it ran into the
 * hill (HammerGS's playtest, 2026-10-03: a convoy at 1326 went north to 1323, then east along the hill to 1621). A
 * planned way keeps to straight lines between turning points wherever a straight line costs no more.
 */
public final class RoutePlanner {

    /** The most turning points a planned way is given, so a winding way through rough ground stays readable. */
    static final int MOST_TURNING_POINTS = 8;

    /** A turning point this near where the unit sets out is dropped: it would only make the unit stop and turn. */
    private static final int NEAR_START_HEXES = 2;

    private static final int DIRECTIONS = 6;

    /**
     * How many movement points more than the cheapest way a straight line may cost and still be taken. On a hex map
     * the cheapest way often runs straight at a hill, then along its foot: a convoy setting out from 1334 saved one
     * point by driving to 1323 under the hill before turning for the pass (HammerGS's playtest, 2026-10-03). A
     * driver takes the clean line for a point or two.
     */
    static final int STRAIGHT_LINE_ALLOWANCE = 2;

    /** What a movement point over the cheapest way counts against a straight line. */
    static final int OVERRUN_WEIGHT = 2;

    /** What a hex of a straight line running along a hill foot, cliff or bank counts against it. */
    static final int WALL_WEIGHT = 1;

    private RoutePlanner() {
    }

    /**
     * Plans a unit's way from a hex to a waypoint in a style, for the Move Order editor's Auto route and for a leg the
     * bot plans in game.
     *
     * @param mover  the unit; its movement type decides where it can go
     * @param from   where it sets out
     * @param target where it is going
     * @param style  the fastest way, or one keeping to better ground
     *
     * @return the turning points on the way and then the target; the target alone when the way is straight, or when
     *       there is no way
     */
    public static List<Coords> plan(Entity mover, Coords from, Coords target, RouteStyle style) {
        Board board = (mover.getGame() == null) ? null : mover.getGame().getBoard(mover);
        List<Coords> route = new ArrayList<>();
        if ((board != null) && board.contains(from)) {
            Map<Coords, Integer> styleCost = styleCosts(board, style);
            WaypointDistanceField field = WaypointDistanceField.build(mover, target, styleCost);
            route.addAll(turningPoints(mover, field, from, board, styleCost));
        }
        route.add(target);
        return route;
    }

    /**
     * What each hex counts extra on a way planned in a style: a hex with no defensive modifier counts the style's
     * exposed-hex cost, so the way keeps to woods, jungle, buildings and the lee of hills where it costs little more.
     *
     * @return the extra movement points by hex; empty for the fastest way
     */
    static Map<Coords, Integer> styleCosts(Board board, RouteStyle style) {
        Map<Coords, Integer> costs = new HashMap<>();
        if (style.getExposedHexCost() == 0) {
            return costs;
        }
        for (int column = 0; column < board.getWidth(); column++) {
            for (int row = 0; row < board.getHeight(); row++) {
                Coords hex = new Coords(column, row);
                if (!isDefensiveGround(board, hex)) {
                    costs.put(hex, style.getExposedHexCost());
                }
            }
        }
        return costs;
    }

    /**
     * @return {@code true} if a unit in the hex gets a defensive modifier from the ground: woods or jungle, a
     *       building, or the lee of higher ground beside it for partial cover
     */
    static boolean isDefensiveGround(Board board, Coords hex) {
        Hex terrain = board.getHex(hex);
        if ((terrain == null) || terrain.containsTerrain(Terrains.WOODS) || terrain.containsTerrain(Terrains.JUNGLE)
              || terrain.containsTerrain(Terrains.BUILDING)) {
            return terrain != null;
        }
        for (int direction = 0; direction < DIRECTIONS; direction++) {
            Coords neighbor = hex.translated(direction);
            if (board.contains(neighbor) && (board.getHex(neighbor).getLevel() > terrain.getLevel())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param mover the unit
     * @param field the unit's route field to the waypoint
     * @param from  where it sets out
     * @param board its board
     *
     * @return the turning points on the planned way, in order, the waypoint itself left out; empty when the way is a
     *       straight line, or there is no way
     */
    static List<Coords> turningPoints(Entity mover, WaypointDistanceField field, Coords from, Board board) {
        return turningPoints(mover, field, from, board, Map.of());
    }

    /**
     * @param styleCost what each hex counts extra by the route's style, as the field was built with
     */
    static List<Coords> turningPoints(Entity mover, WaypointDistanceField field, Coords from, Board board,
          Map<Coords, Integer> styleCost) {
        List<Coords> points = new ArrayList<>();
        int startCost = field.costFrom(from);
        if ((startCost == WaypointDistanceField.UNREACHABLE) || (startCost == 0)) {
            return points;
        }
        Coords anchor = from;
        while ((field.costFrom(anchor) > 0) && (points.size() <= MOST_TURNING_POINTS)) {
            Coords next = bestStraight(mover, field, anchor, board, styleCost);
            if (next == null) {
                // no straight line makes headway: take the next hex of the cheapest way and look again from there
                List<Coords> way = cheapestWay(field, anchor, board);
                if (way.size() < 2) {
                    return new ArrayList<>();
                }
                next = way.get(1);
            }
            if (field.costFrom(next) == 0) {
                break;
            }
            points.add(next);
            anchor = next;
        }
        List<Coords> kept = new ArrayList<>();
        for (Coords point : points) {
            if ((point.distance(from) > NEAR_START_HEXES) && (kept.size() < MOST_TURNING_POINTS)) {
                kept.add(point);
            }
        }
        return kept;
    }

    /**
     * The best next turning point from the anchor: a hex a straight line reaches for at most
     * {@link #STRAIGHT_LINE_ALLOWANCE} movement points over the cheapest way, scored by how near the waypoint it is,
     * plus {@link #OVERRUN_WEIGHT} a point over the cheapest way, plus {@link #WALL_WEIGHT} a hex of the line that runs
     * along ground the unit cannot climb - the foot of a hill, a cliff, a river bank. Lowest score wins; of equal
     * scores, the furthest. Any hex counts, not only those on one cheapest way, so the unit heads straight for a pass
     * rather than for the hill beside it (HammerGS's playtest, 2026-10-03).
     *
     * @return the hex, or {@code null} when no straight line from the anchor makes headway
     */
    private static Coords bestStraight(Entity mover, WaypointDistanceField field, Coords anchor, Board board,
          Map<Coords, Integer> styleCost) {
        int anchorCost = field.costFrom(anchor);
        Coords best = null;
        int bestScore = Integer.MAX_VALUE;
        int bestDistance = 0;
        for (int column = 0; column < board.getWidth(); column++) {
            for (int row = 0; row < board.getHeight(); row++) {
                Coords candidate = new Coords(column, row);
                int cost = field.costFrom(candidate);
                int distance = anchor.distance(candidate);
                // a line is never cheaper than a movement point a hex, so a hex further off than the saving is out
                if ((cost == WaypointDistanceField.UNREACHABLE) || (cost >= anchorCost) || (distance == 0)
                      || (distance > (anchorCost - cost + STRAIGHT_LINE_ALLOWANCE))) {
                    continue;
                }
                int[] line = lineCostAndWalls(mover, anchor, candidate, board, styleCost);
                int overrun = line[0] - (anchorCost - cost);
                if ((line[0] == WaypointDistanceField.UNREACHABLE) || (overrun > STRAIGHT_LINE_ALLOWANCE)) {
                    continue;
                }
                int score = cost + (OVERRUN_WEIGHT * Math.max(0, overrun)) + (WALL_WEIGHT * line[1]);
                if ((score < bestScore) || ((score == bestScore) && (distance > bestDistance))) {
                    best = candidate;
                    bestScore = score;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    /**
     * @return the movement points to drive the straight line between two hexes, or
     *       {@link WaypointDistanceField#UNREACHABLE}; and how many of its hexes run along ground the unit cannot
     *       step onto
     */
    private static int[] lineCostAndWalls(Entity mover, Coords start, Coords end, Board board,
          Map<Coords, Integer> styleCost) {
        List<Coords> line = Coords.intervening(start, end);
        int lineCost = 0;
        int walls = 0;
        for (int index = 1; index < line.size(); index++) {
            int step = WaypointDistanceField.stepCost(mover, board, line.get(index - 1), line.get(index));
            if (step == WaypointDistanceField.UNREACHABLE) {
                return new int[] { WaypointDistanceField.UNREACHABLE, 0 };
            }
            // a straight line over open ground counts what the style puts on it, or the plan would cut across it
            lineCost += step + styleCost.getOrDefault(line.get(index), 0);
            if (isAlongWall(mover, line.get(index), board)) {
                walls++;
            }
        }
        return new int[] { lineCost, walls };
    }

    private static boolean isAlongWall(Entity mover, Coords hex, Board board) {
        for (int direction = 0; direction < DIRECTIONS; direction++) {
            Coords neighbor = hex.translated(direction);
            if (board.contains(neighbor)
                  && (WaypointDistanceField.stepCost(mover, board, hex, neighbor) == WaypointDistanceField.UNREACHABLE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the cheapest way from a hex to the field's waypoint, a hex at a time, each step to the neighbour nearest
     *       the waypoint by the field; ends early where no neighbour is nearer
     */
    static List<Coords> cheapestWay(WaypointDistanceField field, Coords from, Board board) {
        List<Coords> way = new ArrayList<>();
        way.add(from);
        Coords position = from;
        int cost = field.costFrom(position);
        while ((cost != WaypointDistanceField.UNREACHABLE) && (cost > 0)) {
            Coords best = null;
            int bestCost = cost;
            for (int direction = 0; direction < DIRECTIONS; direction++) {
                Coords next = position.translated(direction);
                int nextCost = board.contains(next) ? field.costFrom(next) : WaypointDistanceField.UNREACHABLE;
                if (nextCost < bestCost) {
                    best = next;
                    bestCost = nextCost;
                }
            }
            if (best == null) {
                break;
            }
            way.add(best);
            position = best;
            cost = bestCost;
        }
        return way;
    }

    /**
     * @return {@code true} if the unit can drive the straight line between two hexes for no more than
     *       {@link #STRAIGHT_LINE_ALLOWANCE} movement points over the cheapest way between them
     */
    static boolean isStraightAsCheap(Entity mover, WaypointDistanceField field, Coords start, Coords end,
          Board board) {
        List<Coords> line = Coords.intervening(start, end);
        int lineCost = 0;
        for (int index = 1; index < line.size(); index++) {
            int step = WaypointDistanceField.stepCost(mover, board, line.get(index - 1), line.get(index));
            if (step == WaypointDistanceField.UNREACHABLE) {
                return false;
            }
            lineCost += step;
        }
        return lineCost <= (field.costFrom(start) - field.costFrom(end) + STRAIGHT_LINE_ALLOWANCE);
    }
}
