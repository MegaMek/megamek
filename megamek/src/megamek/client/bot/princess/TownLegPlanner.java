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
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;

/**
 * How a lance gets through a town, as a player does it. HammerGS walked four Meks by hand through the Oasis1 town
 * (2026-10-01), from a line south of it to a line north of it, in six rounds without entering a building. The line came
 * apart in the streets and formed again at the far side: each Mek took its own best street - round the near end of a
 * block, single file where two needed the same street - and none strayed out of reach of a friend. These are the rules
 * that walk showed:
 *
 * <ul>
 *   <li>A leg whose way to the next flag runs between buildings is a town leg ({@link #hexesBesideBuildings}).</li>
 *   <li>On a town leg the lance breaks formation: each unit heads straight for its place at the flag, at its own pace,
 *       and the shape forms there.</li>
 *   <li>Which unit takes which place is chosen by the way each would go, the least movement in all and the fewest
 *       crossings ({@link #assignSpots}); in the walk the Griffin and a Stalker swapped ends.</li>
 *   <li>A leash replaces the shape: no unit ends its move beyond its own medium weapon range of every friend, so each
 *       can still support another ({@link #keepWithinReach}).</li>
 *   <li>A unit still coming goes round behind those already in place, not across their front
 *       ({@link #frontOfUnitsInPlace}).</li>
 *   <li>Where two moves are about as good, the one ending in cover - woods, or higher ground - wins
 *       ({@link #isCover}); the Griffin stopped on the wooded hill at 1420 on purpose.</li>
 *   <li>The lance's slowest units keep to the open outer lanes and leave the narrow streets to the faster ones
 *       ({@link #narrowHexes}); both Stalkers went round the edges while the Griffin and the Grasshopper took the
 *       middle street.</li>
 *   <li>Where two or more units need the same way into the streets, they stack up outside it and go through one
 *       after another, the nearest first ({@link #keepPlaceInStack}).</li>
 * </ul>
 *
 * <p>The methods here only decide; {@link UnitOrdersFollower} applies them.</p>
 */
final class TownLegPlanner {

    /** Hexes along the way, beside or in a building, that make a leg a town leg. */
    static final int TOWN_HEXES = 3;

    /** The most units the planner pairs with places by trying every pairing; beyond it, units keep their places. */
    static final int MAXIMUM_UNITS_TO_PAIR = 6;

    /** The shortest leash, for a unit with no ranged weapon. */
    static final int MINIMUM_LEASH = 3;

    /** The extra movement points a hex in front of a unit already in place costs a unit still coming. */
    static final int FRONT_OF_UNIT_COST = 3;

    /** How far in front of a unit in place the hexes cost more. */
    static final int FRONT_DEPTH = 2;

    /** The movement points a move ending in cover counts as saving on a town leg, so cover wins a close call. */
    static final int COVER_DISCOUNT_MP = 1;

    /** The extra movement points a narrow hex costs one of a lance's slowest units on a town leg. */
    static final int NARROW_HEX_COST_FOR_SLOWEST = 1;

    // stands in for a place the unit cannot reach at all, so any reachable pairing beats one that is not
    private static final int UNREACHABLE_PAIRING_COST = 10_000;

    private TownLegPlanner() {
    }

    /**
     * Follows the cheapest way from a hex to a flag, downhill on the given costs, and counts the hexes on it that are
     * in or beside a building. A street between blocks counts every hex; open ground counts none.
     *
     * @param board      the board
     * @param start      where the unit is
     * @param flag       the flag it is heading for
     * @param costToFlag the movement points from a hex to the flag, or {@link WaypointDistanceField#UNREACHABLE}
     *
     * @return the hexes in or beside a building on the way, not counting the start and the flag
     */
    static int hexesBesideBuildings(Board board, Coords start, Coords flag, ToIntFunction<Coords> costToFlag) {
        int count = 0;
        for (Coords hex : wayTo(board, start, flag, costToFlag)) {
            if (!hex.equals(start) && !hex.equals(flag) && isInOrBesideBuilding(board, hex)) {
                count++;
            }
        }
        return count;
    }

    /**
     * The cheapest way from a hex to a goal, downhill on the given costs, hex by hex.
     *
     * @return the hexes from the start to the goal, both included; just the start when the goal cannot be reached
     */
    static List<Coords> wayTo(Board board, Coords start, Coords goal, ToIntFunction<Coords> costToGoal) {
        List<Coords> way = new ArrayList<>();
        way.add(start);
        Coords current = start;
        int currentCost = costToGoal.applyAsInt(current);
        int stepsLeft = board.getWidth() * board.getHeight();
        while (!current.equals(goal) && (currentCost != WaypointDistanceField.UNREACHABLE) && (stepsLeft-- > 0)) {
            Coords next = null;
            int nextCost = currentCost;
            for (int direction = 0; direction < 6; direction++) {
                Coords neighbour = current.translated(direction);
                int cost = board.contains(neighbour) ? costToGoal.applyAsInt(neighbour)
                      : WaypointDistanceField.UNREACHABLE;
                if (cost < nextCost) {
                    nextCost = cost;
                    next = neighbour;
                }
            }
            if (next == null) {
                break;
            }
            current = next;
            currentCost = nextCost;
            way.add(current);
        }
        return way;
    }

    /**
     * The door on a way into a town: the first narrow hex the way enters from open ground. A unit already in a street
     * is through its door and has none.
     *
     * @param way the hexes of the way, the unit's own first
     *
     * @return the door, or {@code null} when the way enters no street or the unit is in one already
     */
    static @Nullable Coords doorOn(Board board, List<Coords> way) {
        if (way.isEmpty() || isNarrow(board, way.get(0))) {
            return null;
        }
        for (Coords hex : way.subList(1, way.size())) {
            if (isNarrow(board, hex)) {
                return hex;
            }
        }
        return null;
    }

    /**
     * Holds a unit in the stack outside a door until those ahead of it are through: it may not end its move in the
     * door, pass through it, or end nearer the door than its place in the stack - the second waits a hex out, the
     * third two. Infantry going through a door queue up the same way (HammerGS, 2026-10-01).
     *
     * @param paths        the unit's candidate moves
     * @param door         the door
     * @param placeInStack how many units are ahead of it, at least 1
     *
     * @return the moves that keep its place, or all of them when none does
     */
    static List<MovePath> keepPlaceInStack(List<MovePath> paths, Coords door, int placeInStack) {
        List<MovePath> kept = new ArrayList<>();
        for (MovePath path : paths) {
            Coords end = path.getFinalCoords();
            if ((end == null) || ((end.distance(door) >= placeInStack) && !passesThrough(path, door))) {
                kept.add(path);
            }
        }
        return kept.isEmpty() ? paths : kept;
    }

    private static boolean passesThrough(MovePath path, Coords hex) {
        for (MoveStep step : path.getStepVector()) {
            if (hex.equals(step.getPosition())) {
                return true;
            }
        }
        return false;
    }

    static boolean isInOrBesideBuilding(Board board, Coords hex) {
        if (hasBuilding(board, hex)) {
            return true;
        }
        for (int direction = 0; direction < 6; direction++) {
            if (hasBuilding(board, hex.translated(direction))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBuilding(Board board, Coords hex) {
        return board.contains(hex) && board.getHex(hex).containsTerrain(Terrains.BUILDING);
    }

    /**
     * Pairs units with places by trying every pairing: the least movement in all wins, and between pairings as cheap,
     * the one whose straight lines from unit to place cross least. A lance of four has 24 pairings, of six 720.
     *
     * @param units  the units to place
     * @param spots  the places, at least as many as the units
     * @param cost   the movement points from a unit to a place, or {@link WaypointDistanceField#UNREACHABLE}
     *
     * @return each unit's place by unit id; empty when there are more units than {@link #MAXIMUM_UNITS_TO_PAIR} or
     *       than places, so the units keep the places they have
     */
    static Map<Integer, Coords> assignSpots(List<Entity> units, List<Coords> spots,
          ToIntBiFunction<Entity, Coords> cost) {
        if (units.isEmpty() || (units.size() > MAXIMUM_UNITS_TO_PAIR) || (units.size() > spots.size())) {
            return new HashMap<>();
        }
        int[][] costs = new int[units.size()][spots.size()];
        for (int unitIndex = 0; unitIndex < units.size(); unitIndex++) {
            for (int spotIndex = 0; spotIndex < spots.size(); spotIndex++) {
                int movement = cost.applyAsInt(units.get(unitIndex), spots.get(spotIndex));
                costs[unitIndex][spotIndex] = (movement == WaypointDistanceField.UNREACHABLE)
                      ? UNREACHABLE_PAIRING_COST : movement;
            }
        }
        Pairing best = new Pairing();
        tryPairings(units, spots, costs, 0, new int[units.size()], new boolean[spots.size()], best);
        Map<Integer, Coords> assignment = new HashMap<>();
        for (int unitIndex = 0; unitIndex < units.size(); unitIndex++) {
            assignment.put(units.get(unitIndex).getId(), spots.get(best.spotOfUnit[unitIndex]));
        }
        return assignment;
    }

    private static final class Pairing {
        private int[] spotOfUnit;
        private int totalCost = Integer.MAX_VALUE;
        private int crossings = Integer.MAX_VALUE;
    }

    private static void tryPairings(List<Entity> units, List<Coords> spots, int[][] costs, int unitIndex,
          int[] chosen, boolean[] taken, Pairing best) {
        if (unitIndex == units.size()) {
            int totalCost = 0;
            for (int index = 0; index < chosen.length; index++) {
                totalCost += costs[index][chosen[index]];
            }
            if (totalCost > best.totalCost) {
                return;
            }
            int crossings = countCrossings(units, spots, chosen);
            if ((totalCost < best.totalCost) || (crossings < best.crossings)) {
                best.totalCost = totalCost;
                best.crossings = crossings;
                best.spotOfUnit = chosen.clone();
            }
            return;
        }
        for (int spotIndex = 0; spotIndex < spots.size(); spotIndex++) {
            if (!taken[spotIndex]) {
                taken[spotIndex] = true;
                chosen[unitIndex] = spotIndex;
                tryPairings(units, spots, costs, unitIndex + 1, chosen, taken, best);
                taken[spotIndex] = false;
            }
        }
    }

    private static int countCrossings(List<Entity> units, List<Coords> spots, int[] chosen) {
        int crossings = 0;
        for (int first = 0; first < units.size(); first++) {
            for (int second = first + 1; second < units.size(); second++) {
                if (segmentsCross(units.get(first).getPosition(), spots.get(chosen[first]),
                      units.get(second).getPosition(), spots.get(chosen[second]))) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean segmentsCross(Coords firstStart, Coords firstEnd, Coords secondStart, Coords secondEnd) {
        if ((firstStart == null) || (secondStart == null)) {
            return false;
        }
        double[] a = centre(firstStart);
        double[] b = centre(firstEnd);
        double[] c = centre(secondStart);
        double[] d = centre(secondEnd);
        double abC = turn(a, b, c);
        double abD = turn(a, b, d);
        double cdA = turn(c, d, a);
        double cdB = turn(c, d, b);
        return (((abC > 0) && (abD < 0)) || ((abC < 0) && (abD > 0)))
              && (((cdA > 0) && (cdB < 0)) || ((cdA < 0) && (cdB > 0)));
    }

    // the centre of a hex on the map, odd columns half a hex lower
    private static double[] centre(Coords hex) {
        return new double[] { hex.getX() * 1.5, (hex.getY() + (((hex.getX() & 1) == 1) ? 0.5 : 0.0)) * Math.sqrt(3) };
    }

    private static double turn(double[] from, double[] to, double[] point) {
        return ((to[0] - from[0]) * (point[1] - from[1])) - ((to[1] - from[1]) * (point[0] - from[0]));
    }

    /**
     * The leash for a unit in town: the medium range of its longest-reaching weapon, so a lance of long-range
     * missile carriers may spread wider than one of brawlers (HammerGS, 2026-10-01).
     *
     * @return the leash in hexes, at least {@link #MINIMUM_LEASH}
     */
    static int leash(Entity unit) {
        int leash = MINIMUM_LEASH;
        for (WeaponMounted weapon : unit.getWeaponList()) {
            if (weapon.getType() instanceof WeaponType weaponType) {
                leash = Math.max(leash, weaponType.getMediumRange());
            }
        }
        return leash;
    }

    /**
     * Drops the moves that leave a unit beyond its leash of every friend, unless the move closes on its nearest
     * friend; a unit already out of reach may always close in.
     *
     * @param unit    the unit about to move
     * @param paths   its candidate moves
     * @param friends where the other units of its lance stand
     * @param leash   how far from a friend it may end
     *
     * @return the moves within reach, or all of them when none is
     */
    static List<MovePath> keepWithinReach(Entity unit, List<MovePath> paths, List<Coords> friends, int leash) {
        if (friends.isEmpty() || (unit.getPosition() == null)) {
            return paths;
        }
        int nearestNow = nearest(unit.getPosition(), friends);
        List<MovePath> kept = new ArrayList<>();
        for (MovePath path : paths) {
            Coords end = path.getFinalCoords();
            if (end == null) {
                kept.add(path);
                continue;
            }
            int nearestAfter = nearest(end, friends);
            if ((nearestAfter <= leash) || (nearestAfter <= nearestNow)) {
                kept.add(path);
            }
        }
        return kept.isEmpty() ? paths : kept;
    }

    static int nearest(Coords position, List<Coords> friends) {
        int nearest = Integer.MAX_VALUE;
        for (Coords friend : friends) {
            nearest = Math.min(nearest, position.distance(friend));
        }
        return nearest;
    }

    /**
     * The hexes in front of units already in their places, which a unit still coming should go round behind: up to
     * {@link #FRONT_DEPTH} hexes ahead along the unit's facing and the hexsides either side of it. In the walk the
     * last Stalker crossed one row behind the Grasshopper and the Griffin, never in front of them.
     *
     * @param unitsInPlace the units standing in their places
     *
     * @return the extra movement points each such hex costs
     */
    static Map<Coords, Integer> frontOfUnitsInPlace(List<Entity> unitsInPlace) {
        Map<Coords, Integer> extraCost = new HashMap<>();
        for (Entity unit : unitsInPlace) {
            Coords position = unit.getPosition();
            if (position == null) {
                continue;
            }
            int facing = unit.getFacing();
            for (int side = -1; side <= 1; side++) {
                int direction = (facing + side + 6) % 6;
                for (int depth = 1; depth <= FRONT_DEPTH; depth++) {
                    extraCost.put(position.translated(direction, depth), FRONT_OF_UNIT_COST);
                }
            }
        }
        return extraCost;
    }

    /**
     * A hex worth stopping in when it costs about the same as one in the open: woods or jungle, or ground higher than
     * where the unit started. A building or water never counts.
     *
     * @param board      the board
     * @param hex        where a move ends
     * @param startLevel the ground level where the unit started its move
     *
     * @return {@code true} if the hex gives cover
     */
    static boolean isCover(Board board, Coords hex, int startLevel) {
        if (!board.contains(hex)) {
            return false;
        }
        Hex terrain = board.getHex(hex);
        if (terrain.containsTerrain(Terrains.BUILDING) || terrain.containsTerrain(Terrains.WATER)) {
            return false;
        }
        return terrain.containsTerrain(Terrains.WOODS) || terrain.containsTerrain(Terrains.JUNGLE)
              || (terrain.getLevel() > startLevel);
    }

    /**
     * The narrow hexes of a town: open hexes with buildings on two sides that are not next to each other - a street
     * or an alley, where a slow unit holds up any faster one behind it.
     *
     * @return each narrow hex, with the extra movement points it costs one of a lance's slowest units
     */
    static Map<Coords, Integer> narrowHexes(Board board) {
        Map<Coords, Integer> narrow = new HashMap<>();
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                Coords hex = new Coords(x, y);
                if (isNarrow(board, hex)) {
                    narrow.put(hex, NARROW_HEX_COST_FOR_SLOWEST);
                }
            }
        }
        return narrow;
    }

    static boolean isNarrow(Board board, Coords hex) {
        if (hasBuilding(board, hex)) {
            return false;
        }
        List<Integer> buildingSides = new ArrayList<>();
        for (int direction = 0; direction < 6; direction++) {
            if (hasBuilding(board, hex.translated(direction))) {
                buildingSides.add(direction);
            }
        }
        for (int first = 0; first < buildingSides.size(); first++) {
            for (int second = first + 1; second < buildingSides.size(); second++) {
                if (UnitOrdersFollower.sidesApart(buildingSides.get(first), buildingSides.get(second)) >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param unit    a unit of the lance
     * @param members the lance's units in action
     * @param pace    each unit's movement points a turn at the lance's pace
     *
     * @return {@code true} if the unit is one of the lance's slowest, and some unit of the lance is faster
     */
    static boolean isSlowest(Entity unit, List<Entity> members, ToIntFunction<Entity> pace) {
        int slowest = Integer.MAX_VALUE;
        int fastest = Integer.MIN_VALUE;
        for (Entity member : members) {
            int movementPoints = pace.applyAsInt(member);
            slowest = Math.min(slowest, movementPoints);
            fastest = Math.max(fastest, movementPoints);
        }
        return (slowest < fastest) && (pace.applyAsInt(unit) == slowest);
    }
}
