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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

class TownLegPlannerTest {

    private static final int WIDTH = 12;
    private static final int HEIGHT = 12;
    private static final int NORTH = 0;

    private static Board openBoard() {
        Hex[] hexes = new Hex[WIDTH * HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        return new Board(WIDTH, HEIGHT, hexes);
    }

    /** Open ground with a street up column 4 between two rows of buildings, columns 3 and 5, rows 3 to 8. */
    private static Board boardWithStreet() {
        Board board = openBoard();
        for (int y = 3; y <= 8; y++) {
            board.getHex(3, y).addTerrain(new Terrain(Terrains.BUILDING, 1));
            board.getHex(5, y).addTerrain(new Terrain(Terrains.BUILDING, 1));
        }
        return board;
    }

    private static Entity unitAt(int id, Coords position) {
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getPosition()).thenReturn(position);
        return unit;
    }

    private static MovePath moveEndingAt(Coords end) {
        MovePath path = mock(MovePath.class);
        when(path.getFinalCoords()).thenReturn(end);
        when(path.getStepVector()).thenReturn(new Vector<>());
        return path;
    }

    @Test
    void aWayUpAStreetBetweenBuildingsIsATownLeg() {
        Board board = boardWithStreet();
        Coords flag = new Coords(4, 1);

        int townHexes = TownLegPlanner.hexesBesideBuildings(board, new Coords(4, 10), flag, flag::distance);

        assertTrue(townHexes >= TownLegPlanner.TOWN_HEXES, "counted " + townHexes);
    }

    @Test
    void aWayAcrossOpenGroundIsNotATownLeg() {
        Coords flag = new Coords(4, 1);

        assertEquals(0, TownLegPlanner.hexesBesideBuildings(openBoard(), new Coords(4, 10), flag, flag::distance));
    }

    @Test
    void eachUnitTakesThePlaceNearestItsOwnWayRatherThanItsPlaceInTheLine() {
        // HammerGS's town walk: the Griffin and a Stalker swapped ends so each could take the quickest way through
        Entity startedLeft = unitAt(1, new Coords(8, 9));
        Entity startedRight = unitAt(2, new Coords(2, 9));
        Coords leftPlace = new Coords(2, 2);
        Coords rightPlace = new Coords(8, 2);

        Map<Integer, Coords> spots = TownLegPlanner.assignSpots(List.of(startedLeft, startedRight),
              List.of(leftPlace, rightPlace), (unit, spot) -> unit.getPosition().distance(spot), unit -> 4);

        assertEquals(rightPlace, spots.get(1));
        assertEquals(leftPlace, spots.get(2));
    }

    @Test
    void betweenPairingsAsShortTheOneThatDoesNotCrossWins() {
        Entity west = unitAt(1, new Coords(4, 8));
        Entity east = unitAt(2, new Coords(6, 8));
        Coords westPlace = new Coords(4, 4);
        Coords eastPlace = new Coords(6, 4);

        // every pairing costs the same, so only the crossing tells them apart
        Map<Integer, Coords> spots = TownLegPlanner.assignSpots(List.of(west, east), List.of(eastPlace, westPlace),
              (unit, spot) -> 1, unit -> 4);

        assertEquals(westPlace, spots.get(1));
        assertEquals(eastPlace, spots.get(2));
    }

    @Test
    void theSlowestUnitGetsTheNearerPlaceSoTheLastArrivesSoonest() {
        // HammerGS's playtest: paired by least movement in all, the slow Stalker drew the far place beyond the water
        // and came in three rounds after the rest (2026-10-01)
        Entity stalker = unitAt(1, new Coords(5, 9));
        Entity griffin = unitAt(2, new Coords(5, 9));
        Coords near = new Coords(5, 6);
        Coords far = new Coords(5, 1);
        Map<Entity, Integer> walk = Map.of(stalker, 3, griffin, 5);

        Map<Integer, Coords> spots = TownLegPlanner.assignSpots(List.of(stalker, griffin), List.of(far, near),
              (unit, spot) -> unit.getPosition().distance(spot), walk::get);

        assertEquals(near, spots.get(1));
        assertEquals(far, spots.get(2));
    }

    @Test
    void aLanceTooBigToPairEveryWayKeepsItsPlaces() {
        List<Entity> units = new ArrayList<>();
        List<Coords> spots = new ArrayList<>();
        for (int id = 0; id <= TownLegPlanner.MAXIMUM_UNITS_TO_PAIR; id++) {
            units.add(unitAt(id, new Coords(id, 10)));
            spots.add(new Coords(id, 1));
        }

        assertTrue(TownLegPlanner.assignSpots(units, spots, (unit, spot) -> 1, unit -> 4).isEmpty());
    }

    @Test
    void theLeashIsTheMediumRangeOfTheLongestReachingWeapon() {
        // HammerGS: a lance of missile carriers may spread wider than one of brawlers (2026-10-01)
        Entity stalker = mock(Entity.class);
        List<WeaponMounted> weapons = List.of(weapon(6), weapon(14));
        when(stalker.getWeaponList()).thenReturn(weapons);

        assertEquals(14, TownLegPlanner.leash(stalker));

        Entity unarmed = mock(Entity.class);
        when(unarmed.getWeaponList()).thenReturn(List.of());
        assertEquals(TownLegPlanner.MINIMUM_LEASH, TownLegPlanner.leash(unarmed));
    }

    private static WeaponMounted weapon(int mediumRange) {
        WeaponType type = mock(WeaponType.class);
        when(type.getMediumRange()).thenReturn(mediumRange);
        WeaponMounted weapon = mock(WeaponMounted.class);
        when(weapon.getType()).thenReturn(type);
        return weapon;
    }

    @Test
    void aMoveThatLeavesEveryFriendOutOfReachIsDropped() {
        Entity unit = unitAt(1, new Coords(5, 5));
        List<Coords> friends = List.of(new Coords(5, 3));
        MovePath staysClose = moveEndingAt(new Coords(5, 4));
        MovePath runsOff = moveEndingAt(new Coords(5, 10));

        assertEquals(List.of(staysClose),
              TownLegPlanner.keepWithinReach(unit, List.of(staysClose, runsOff), friends, 4));
    }

    @Test
    void aUnitAlreadyOutOfReachMayStillCloseIn() {
        Entity straggler = unitAt(1, new Coords(5, 11));
        List<Coords> friends = List.of(new Coords(5, 2));
        MovePath closesIn = moveEndingAt(new Coords(5, 9));
        MovePath fallsBack = moveEndingAt(new Coords(1, 11));

        assertEquals(List.of(closesIn),
              TownLegPlanner.keepWithinReach(straggler, List.of(closesIn, fallsBack), friends, 4));
    }

    @Test
    void theHexesInFrontOfAUnitInPlaceCostMoreButNotThoseBehindIt() {
        // HammerGS's town walk: the last Stalker crossed one row behind the Meks already in place, never in front
        Entity inPlace = unitAt(1, new Coords(5, 5));
        when(inPlace.getFacing()).thenReturn(NORTH);

        Map<Coords, Integer> extraCost = TownLegPlanner.frontOfUnitsInPlace(List.of(inPlace));

        assertEquals(TownLegPlanner.FRONT_OF_UNIT_COST, extraCost.get(new Coords(5, 4)));
        assertEquals(TownLegPlanner.FRONT_OF_UNIT_COST, extraCost.get(new Coords(5, 3)));
        assertFalse(extraCost.containsKey(new Coords(5, 6)), "behind it is free");
    }

    @Test
    void woodsAndHigherGroundAreCoverButBuildingsAndWaterAreNot() {
        // HammerGS's town walk: the Griffin stopped on the wooded hill at 1420 on purpose (2026-10-01)
        Board board = openBoard();
        board.getHex(2, 2).addTerrain(new Terrain(Terrains.WOODS, 1));
        board.getHex(3, 3).setLevel(1);
        board.getHex(4, 4).addTerrain(new Terrain(Terrains.BUILDING, 1));
        board.getHex(5, 5).addTerrain(new Terrain(Terrains.WATER, 1));

        assertTrue(TownLegPlanner.isCover(board, new Coords(2, 2), 0));
        assertTrue(TownLegPlanner.isCover(board, new Coords(3, 3), 0));
        assertFalse(TownLegPlanner.isCover(board, new Coords(3, 3), 1), "level with where it started");
        assertFalse(TownLegPlanner.isCover(board, new Coords(4, 4), 0));
        assertFalse(TownLegPlanner.isCover(board, new Coords(5, 5), 0));
        assertFalse(TownLegPlanner.isCover(board, new Coords(7, 7), 0));
    }

    @Test
    void aStreetBetweenBuildingsIsNarrowButOpenGroundAndACornerAreNot() {
        Board board = boardWithStreet();

        assertTrue(TownLegPlanner.isNarrow(board, new Coords(4, 5)));
        assertFalse(TownLegPlanner.isNarrow(board, new Coords(8, 5)), "open ground");
        assertFalse(TownLegPlanner.isNarrow(board, new Coords(3, 5)), "a building is not a street");
        assertTrue(TownLegPlanner.narrowHexes(board).containsKey(new Coords(4, 6)));
    }

    @Test
    void onlyTheSlowestOfALanceWithAFasterUnitKeepToTheOuterLanes() {
        // HammerGS's town walk: both Stalkers went round the edges; the Griffin and the Grasshopper took the middle
        Entity stalker = unitAt(1, new Coords(1, 1));
        Entity otherStalker = unitAt(2, new Coords(2, 1));
        Entity grasshopper = unitAt(3, new Coords(3, 1));
        Entity griffin = unitAt(4, new Coords(4, 1));
        Map<Entity, Integer> walk = Map.of(stalker, 3, otherStalker, 3, grasshopper, 4, griffin, 5);
        List<Entity> lance = List.of(stalker, otherStalker, grasshopper, griffin);

        assertTrue(TownLegPlanner.isSlowest(stalker, lance, walk::get));
        assertTrue(TownLegPlanner.isSlowest(otherStalker, lance, walk::get));
        assertFalse(TownLegPlanner.isSlowest(grasshopper, lance, walk::get));
        assertFalse(TownLegPlanner.isSlowest(stalker, List.of(stalker, otherStalker), walk::get),
              "a lance all of one speed has no one to make way for");
    }

    @Test
    void theDoorIsWhereTheWayFirstEntersAStreet() {
        Board board = boardWithStreet();
        Coords flag = new Coords(4, 1);
        List<Coords> way = TownLegPlanner.wayTo(board, new Coords(4, 11), flag, flag::distance);

        Coords door = TownLegPlanner.doorOn(board, way);

        assertTrue(TownLegPlanner.isNarrow(board, door), "door " + door);
        assertFalse(TownLegPlanner.isNarrow(board, way.get(way.indexOf(door) - 1)), "the hex before it is open");
    }

    @Test
    void aUnitAlreadyInTheStreetHasNoDoor() {
        Board board = boardWithStreet();
        Coords flag = new Coords(4, 1);
        List<Coords> way = TownLegPlanner.wayTo(board, new Coords(4, 6), flag, flag::distance);

        assertEquals(null, TownLegPlanner.doorOn(board, way));
    }

    @Test
    void theSecondInTheStackWaitsAHexOutsideTheDoorAndTheThirdTwo() {
        // HammerGS: "when infantry are going through a door, they queue up then go in" (2026-10-01)
        Coords door = new Coords(4, 5);
        MovePath intoTheDoor = moveThrough(door);
        MovePath aHexOut = moveThrough(new Coords(4, 6));
        MovePath twoHexesOut = moveThrough(new Coords(4, 7));
        MovePath throughAndBeyond = moveThrough(door, new Coords(4, 3));

        assertEquals(List.of(aHexOut, twoHexesOut), TownLegPlanner.keepPlaceInStack(
              List.of(intoTheDoor, aHexOut, twoHexesOut, throughAndBeyond), door, 1));
        assertEquals(List.of(twoHexesOut), TownLegPlanner.keepPlaceInStack(
              List.of(intoTheDoor, aHexOut, twoHexesOut, throughAndBeyond), door, 2));
    }

    private static MovePath moveThrough(Coords... hexes) {
        MovePath path = moveEndingAt(hexes[hexes.length - 1]);
        Vector<MoveStep> steps = new Vector<>();
        for (Coords hex : hexes) {
            MoveStep step = mock(MoveStep.class);
            when(step.getPosition()).thenReturn(hex);
            steps.add(step);
        }
        when(path.getStepVector()).thenReturn(steps);
        return path;
    }
}
