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

import static megamek.client.bot.princess.EmplacementDeployment.EXTRA_GUN_PERCENT;
import static megamek.client.bot.princess.EmplacementDeployment.MINIMUM_SPACING_HEXES;
import static megamek.client.bot.princess.EmplacementDeployment.MUTUAL_SUPPORT_PERCENT;
import static megamek.client.bot.princess.EmplacementDeployment.SECOND_GUN_PERCENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.EmplacementDeployment.SiteProblem;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies where a TO:AR gun emplacement is built: never in water, across a street or on a road while clear ground is
 * legal, spaced from the other emplacements where the zone allows, and otherwise on the hex whose guns are worth the
 * most, counting the crossfire they make with the other emplacements and the cover they give one another.
 */
class EmplacementDeploymentTest {

    private static final Coords OPEN = new Coords(10, 10);
    private static final Coords BOXED_IN = new Coords(14, 10);
    private static final Coords LAKE = new Coords(18, 10);

    /** Where the stubbed views put the ground they overlook: far from every hex the tests build on. */
    private static final int FAR_AWAY = 100;

    /** How close a neighbour must stand to be guarded in the scoring tests. */
    private static final int GUARD_RANGE = 6;

    private Entity emplacement;
    private Board board;
    private Map<Coords, Hex> hexes;

    @BeforeEach
    void setUp() {
        emplacement = mock(Entity.class);
        when(emplacement.getShortName()).thenReturn("Test Emplacement");
        hexes = new HashMap<>();
        board = mock(Board.class);
        when(board.getHex(any(Coords.class))).thenAnswer(invocation -> hexes.getOrDefault(invocation.getArgument(0),
              new Hex(0)));
        hexes.put(LAKE, new Hex(0, "water:2", ""));
    }

    /** A view over the given number of hexes, each worth 1, in a row far from everything else. */
    private static Map<Coords, Integer> view(int hexCount) {
        return view(hexCount, 0);
    }

    /** As {@link #view(int)}, on a row of its own so that two views share no ground. */
    private static Map<Coords, Integer> view(int hexCount, int row) {
        return view(hexCount, row, 1);
    }

    /** As {@link #view(int, int)}, with each hex worth the given value. */
    private static Map<Coords, Integer> view(int hexCount, int row, int value) {
        Map<Coords, Integer> ground = new HashMap<>();
        for (int index = 0; index < hexCount; index++) {
            ground.put(new Coords(FAR_AWAY + index, FAR_AWAY + row), value);
        }
        return ground;
    }

    /** Walls both sides of a north-south street with buildings, leaving the given columns open. */
    private void buildStreet(int westWallColumn, int eastWallColumn) {
        for (int row = 4; row <= 16; row++) {
            hexes.put(new Coords(westWallColumn, row), new Hex(0, "building:2;bldg_elev:2;bldg_cf:40", ""));
            hexes.put(new Coords(eastWallColumn, row), new Hex(0, "building:2;bldg_elev:2;bldg_cf:40", ""));
        }
    }

    // ---------------------------------------------------------------- which units

    @Test
    void anAdvancedBuildingOfTheGunEmplacementClassIsAnEmplacement() {
        AbstractBuildingEntity building = mock(AbstractBuildingEntity.class);
        when(building.getBldgClass()).thenReturn(IBuilding.GUN_EMPLACEMENT);
        assertTrue(EmplacementDeployment.isGunEmplacementBuilding(building));
    }

    @Test
    void otherBuildingsAndUnitsAreNot() {
        AbstractBuildingEntity hangar = mock(AbstractBuildingEntity.class);
        when(hangar.getBldgClass()).thenReturn(IBuilding.HANGAR);
        assertFalse(EmplacementDeployment.isGunEmplacementBuilding(hangar));
        assertFalse(EmplacementDeployment.isGunEmplacementBuilding(mock(Entity.class)));
    }

    // ---------------------------------------------------------------- the site

    @Test
    void waterAndMagmaAreUnsuitable() {
        assertTrue(EmplacementDeployment.isUnsuitableSite(new Hex(0, "water:1", "")));
        assertTrue(EmplacementDeployment.isUnsuitableSite(new Hex(0, "water:2;ice:1", "")));
        assertTrue(EmplacementDeployment.isUnsuitableSite(new Hex(0, "magma:1", "")));
        assertTrue(EmplacementDeployment.isUnsuitableSite(null));
    }

    @Test
    void dryGroundIsSuitable() {
        assertFalse(EmplacementDeployment.isUnsuitableSite(new Hex(0)));
        assertFalse(EmplacementDeployment.isUnsuitableSite(new Hex(2, "rough:1", "")));
        assertFalse(EmplacementDeployment.isUnsuitableSite(new Hex(0, "water:0", "")),
              "depth 0 water is only a puddle");
    }

    @Test
    void neverBuildsInWaterWhileDryGroundIsLegal() {
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(LAKE, BOXED_IN), board, List.of(),
              site -> view(site.equals(LAKE) ? 36 : 2), site -> 100);
        assertEquals(BOXED_IN, chosen, "open water must not win on its field of fire");
    }

    @Test
    void buildsInWaterOnlyWhenNothingElseIsLegal() {
        assertEquals(LAKE, EmplacementDeployment.choose(emplacement, List.of(LAKE), board, List.of(),
              site -> view(0), site -> 100));
    }

    @Test
    void noCandidatesMeansNoHex() {
        assertNull(EmplacementDeployment.choose(emplacement, List.of(), board, List.of(), site -> view(0), site -> 100));
    }

    // ---------------------------------------------------------------- streets

    @Test
    void openGroundHasNoProblem() {
        assertEquals(SiteProblem.NONE, EmplacementDeployment.siteProblem(board, OPEN, List.of()));
    }

    @Test
    void aStreetOneHexWideIsBlockedByAnEmplacementInIt() {
        buildStreet(9, 11);
        assertTrue(EmplacementStreetCheck.blocksPassage(board, OPEN, List.of()));
        assertEquals(SiteProblem.BLOCKS_PASSAGE, EmplacementDeployment.siteProblem(board, OPEN, List.of()));
    }

    @Test
    void aStreetTwoHexesWideStillHasALaneLeft() {
        buildStreet(9, 12);
        assertFalse(EmplacementStreetCheck.blocksPassage(board, OPEN, List.of()));
    }

    @Test
    void theLastOpenLaneOfAStreetIsABottleneck() {
        buildStreet(9, 12);
        List<Coords> emplacementInTheOtherLane = List.of(new Coords(11, 10));
        assertTrue(EmplacementStreetCheck.blocksPassage(board, OPEN, emplacementInTheOtherLane),
              "two emplacements side by side must not close a street between them");
    }

    @Test
    void aDeadEndBlocksNothing() {
        for (int direction = 1; direction < 6; direction++) {
            hexes.put(OPEN.translated(direction), new Hex(0, "building:2;bldg_elev:2;bldg_cf:40", ""));
        }
        assertFalse(EmplacementStreetCheck.blocksPassage(board, OPEN, List.of()));
    }

    @Test
    void aRoadHexIsAPoorSite() {
        hexes.put(OPEN, new Hex(0, "road:1", ""));
        assertEquals(SiteProblem.ON_ROAD, EmplacementDeployment.siteProblem(board, OPEN, List.of()));
    }

    @Test
    void clearGroundBeatsKnockingDownABuilding() {
        Coords house = new Coords(20, 20);
        hexes.put(house, new Hex(0, "building:1;bldg_elev:1;bldg_cf:15", ""));
        assertEquals(SiteProblem.REPLACES_BUILDING, EmplacementDeployment.siteProblem(board, house, List.of()));
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(house, BOXED_IN), board, List.of(),
              site -> view(site.equals(house) ? 40 : 3), site -> 100);
        assertEquals(BOXED_IN, chosen);
    }

    @Test
    void theLotBesideTheStreetBeatsTheStreetItself() {
        buildStreet(9, 11);
        Coords inTheStreet = OPEN;
        Coords besideTheStreet = new Coords(10, 2);
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(inTheStreet, besideTheStreet), board,
              List.of(), site -> view(site.equals(inTheStreet) ? 40 : 10), site -> 100);
        assertEquals(besideTheStreet, chosen, "the long view down the street must not put the emplacement in it");
    }

    @Test
    void buildsInTheStreetOnlyWhenNothingElseIsLegal() {
        buildStreet(9, 11);
        assertEquals(OPEN, EmplacementDeployment.choose(emplacement, List.of(OPEN), board, List.of(),
              site -> view(5), site -> 100));
    }

    @Test
    void aRoadIsStillBetterThanClosingAStreet() {
        buildStreet(9, 11);
        Coords onARoad = new Coords(20, 20);
        hexes.put(onARoad, new Hex(0, "road:1", ""));
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(OPEN, onARoad), board, List.of(),
              site -> view(site.equals(OPEN) ? 40 : 1), site -> 100);
        assertEquals(onARoad, chosen);
    }

    // ---------------------------------------------------------------- ground covered

    @Test
    void theHexWhoseGunsAreWorthMoreWins() {
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(BOXED_IN, OPEN), board, List.of(),
              site -> view(site.equals(OPEN) ? 30 : 4), site -> 100);
        assertEquals(OPEN, chosen);
    }

    @Test
    void higherGroundBreaksATie() {
        hexes.put(BOXED_IN, new Hex(2));
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(OPEN, BOXED_IN), board, List.of(),
              site -> view(10), site -> 100);
        assertEquals(BOXED_IN, chosen);
    }

    @Test
    void groundOneOtherEmplacementCoversMakesACrossfire() {
        Coords site = new Coords(40, 40);
        Coords neighbour = new Coords(60, 60);
        Map<Coords, Integer> ground = Map.of(new Coords(50, 50), 100, new Coords(51, 50), 100);
        Map<Coords, Integer> neighbourSeesOne = Map.of(new Coords(50, 50), 100);

        assertEquals(200, EmplacementDeployment.score(site, ground, Map.of(), GUARD_RANGE));
        assertEquals(100 + SECOND_GUN_PERCENT,
              EmplacementDeployment.score(site, ground, Map.of(neighbour, neighbourSeesOne), GUARD_RANGE),
              "a second gun on the same hex is worth more than a first gun on a fresh one");
    }

    @Test
    void aThirdGunOnTheSameGroundAddsLittle() {
        Coords site = new Coords(40, 40);
        Map<Coords, Integer> ground = Map.of(new Coords(50, 50), 100);
        Map<Coords, Map<Coords, Integer>> twoAlreadyThere = Map.of(
              new Coords(60, 60), Map.of(new Coords(50, 50), 100),
              new Coords(70, 70), Map.of(new Coords(50, 50), 100));

        assertEquals(EXTRA_GUN_PERCENT,
              EmplacementDeployment.score(site, ground, twoAlreadyThere, GUARD_RANGE));
    }

    @Test
    void groundANeighbourOnlySeesIsNotCovered() {
        Coords site = new Coords(40, 40);
        Map<Coords, Integer> ground = Map.of(new Coords(50, 50), 100);
        Map<Coords, Integer> outOfRange = Map.of(new Coords(50, 50), EmplacementDeployment.SEEN_ONLY_VALUE);

        assertEquals(100, EmplacementDeployment.score(site, ground, Map.of(new Coords(60, 60), outOfRange),
              GUARD_RANGE));
    }

    @Test
    void theSecondEmplacementJoinsTheFirstOnItsLane() {
        Coords first = new Coords(30, 30);
        Coords sameLane = new Coords(30, 10);
        Coords newLane = new Coords(10, 30);
        Map<Coords, Map<Coords, Integer>> coveredViews = Map.of(first, view(12, 0, 100), sameLane, view(12, 0, 100),
              newLane, view(8, 1, 100));
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(sameLane, newLane), board,
              List.of(first), coveredViews::get, site -> 100);
        assertEquals(sameLane, chosen, "twelve hexes under a second gun are worth eighteen; eight fresh are worth eight");
    }

    @Test
    void guardingANeighboursFlankEarnsABonus() {
        Coords site = new Coords(48, 50);
        Coords neighbour = new Coords(50, 50);
        Map<Coords, Integer> guarding = Map.of(neighbour.translated(2), 100);
        Map<Coords, Integer> elsewhere = Map.of(new Coords(60, 60), 100);
        Map<Coords, Map<Coords, Integer>> neighbourSeesNothing = Map.of(neighbour, Map.of());

        assertEquals(100 + MUTUAL_SUPPORT_PERCENT,
              EmplacementDeployment.score(site, guarding, neighbourSeesNothing, GUARD_RANGE));
        assertEquals(100, EmplacementDeployment.score(site, elsewhere, neighbourSeesNothing, GUARD_RANGE));
    }

    @Test
    void aNeighbourTooFarAwayIsNotGuarded() {
        Coords neighbour = new Coords(50, 50);
        Coords farSite = new Coords(50, 50 + GUARD_RANGE + 1);
        Map<Coords, Integer> guarding = Map.of(neighbour.translated(2), 100);

        assertEquals(100, EmplacementDeployment.score(farSite, guarding, Map.of(neighbour, Map.of()), GUARD_RANGE),
              "seeing a neighbour's flank at long range does not protect it");
    }

    @Test
    void beingGuardedByANeighbourEarnsABonusToo() {
        Coords site = new Coords(48, 50);
        Coords neighbour = new Coords(50, 50);
        Map<Coords, Integer> ground = Map.of(new Coords(60, 60), 100);
        Map<Coords, Integer> neighbourCoversMyFlank = Map.of(site.translated(0), 100);

        assertEquals(100 + MUTUAL_SUPPORT_PERCENT,
              EmplacementDeployment.score(site, ground, Map.of(neighbour, neighbourCoversMyFlank), GUARD_RANGE));
    }

    @Test
    void theRightDepthBeatsABetterViewAtTheWrongOne() {
        Coords inTheLine = OPEN;
        Coords tooFarBack = BOXED_IN;
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(inTheLine, tooFarBack), board, List.of(),
              site -> view(site.equals(tooFarBack) ? 12 : 10, 0, 100),
              site -> site.equals(tooFarBack) ? 50 : 100);
        assertEquals(inTheLine, chosen, "twelve hexes at half score are worth six; ten at full score are worth ten");
    }

    @Test
    void anArtilleryEmplacementTakesTheHexThatSeesLeast() {
        WeaponType longTom = mock(WeaponType.class);
        when(longTom.hasFlag(WeaponType.F_ARTILLERY)).thenReturn(true);
        WeaponMounted mounted = mock(WeaponMounted.class);
        when(mounted.getType()).thenReturn(longTom);
        when(emplacement.getWeaponList()).thenReturn(List.of(mounted));

        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(OPEN, BOXED_IN), board, List.of(),
              site -> view(site.equals(OPEN) ? 30 : 4), site -> 100);
        assertEquals(BOXED_IN, chosen, "artillery needs no line of sight, so it hides");
    }

    // ---------------------------------------------------------------- spacing

    @Test
    void crowdingIsTheSpacingStolenFromTheNearestEmplacement() {
        List<Coords> other = List.of(new Coords(10, 1));
        assertEquals(MINIMUM_SPACING_HEXES, EmplacementDeployment.crowding(new Coords(10, 1), other));
        assertEquals(MINIMUM_SPACING_HEXES - 1, EmplacementDeployment.crowding(new Coords(11, 1), other));
        assertEquals(0, EmplacementDeployment.crowding(new Coords(10 + MINIMUM_SPACING_HEXES, 1), other));
        assertEquals(0, EmplacementDeployment.crowding(new Coords(10, 1), List.of()));
    }

    @Test
    void aSpacedHexBeatsABetterViewBesideAnotherEmplacement() {
        Coords besideIt = OPEN.translated(0);
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(besideIt, BOXED_IN), board,
              List.of(OPEN), site -> view(site.equals(besideIt) ? 30 : 4), site -> 100);
        assertEquals(BOXED_IN, chosen, "two emplacements side by side cover the same ground");
    }

    @Test
    void spacingGivesWayWhenNoHexHasRoom() {
        Coords north = OPEN.translated(0);
        Coords northEast = OPEN.translated(1);
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(north, northEast), board,
              List.of(OPEN), site -> view(site.equals(northEast) ? 20 : 5), site -> 100);
        assertEquals(northEast, chosen, "when every hex is equally crowded, the view decides");
    }

    // ---------------------------------------------------------------- sample hexes

    @Test
    void oneHexInThreeAlongARowIsASample() {
        int samples = 0;
        for (int column = 0; column < 30; column++) {
            if (EmplacementDeployment.isSampleHex(new Coords(column, 7))) {
                samples++;
            }
        }
        assertEquals(10, samples);
    }

    @Test
    void anUnarmedEmplacementLooksTheDefaultDistance() {
        assertEquals(EmplacementDeployment.DEFAULT_REACH, EmplacementDeployment.reach(emplacement));
    }
}
