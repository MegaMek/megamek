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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.client.bot.princess.EmplacementDeployment.SiteProblem;
import megamek.common.GameBoardTestCase;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Sites an emplacement on a small town with real line of sight: a solid block of three-level buildings with one street
 * a hex wide running north to south through it, and an open square at each end.
 */
class EmplacementCityBoardTest extends GameBoardTestCase {

    private static final String TOWN = "EMPLACEMENT_TOWN";
    private static final int WIDTH = 15;
    private static final int HEIGHT = 16;
    private static final int STREET_COLUMN = 7;
    private static final int FIRST_BUILDING_ROW = 3;
    private static final int LAST_BUILDING_ROW = 12;

    static {
        StringBuilder data = new StringBuilder("size " + WIDTH + " " + HEIGHT + "\n");
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                boolean isBuilding = (row >= FIRST_BUILDING_ROW) && (row <= LAST_BUILDING_ROW)
                      && (column != STREET_COLUMN);
                data.append(String.format("hex %02d%02d 0 \"%s\" \"\"\n", column + 1, row + 1,
                      isBuilding ? "bldg_elev:3;building:2;bldg_cf:100" : ""));
            }
        }
        data.append("end");
        initializeBoard(TOWN, data.toString());
    }

    private Entity emplacement;
    private Board board;

    @BeforeEach
    void setUp() {
        setBoard(TOWN);
        board = getGame().getBoard();
        emplacement = mock(Entity.class);
        when(emplacement.getShortName()).thenReturn("Test Emplacement");
    }

    private static boolean isInTheStreet(Coords hex) {
        return (hex.getX() == STREET_COLUMN) && (hex.getY() >= FIRST_BUILDING_ROW)
              && (hex.getY() <= LAST_BUILDING_ROW);
    }

    private List<Coords> openGround() {
        List<Coords> open = new ArrayList<>();
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                Coords hex = new Coords(column, row);
                if (!board.getHex(hex).containsTerrain(Terrains.BUILDING)) {
                    open.add(hex);
                }
            }
        }
        return open;
    }

    @Test
    void theSquareAtTheStreetMouthLooksDownTheStreet() {
        Coords streetMouth = new Coords(STREET_COLUMN, 1);
        Map<Coords, Integer> covered = EmplacementDeployment.groundCovered(getGame(), board, streetMouth,
              emplacement, EmplacementApproach.plan(board, openGround(), null), Set.of());
        boolean seesIntoTheStreet = false;
        for (Coords hex : covered.keySet()) {
            seesIntoTheStreet = seesIntoTheStreet || isInTheStreet(hex);
        }
        assertTrue(seesIntoTheStreet, "a hex in line with the street covers it without standing in it");
    }

    @Test
    void aHexOffToTheSideCannotSeeIntoTheStreet() {
        Coords corner = new Coords(1, 1);
        Map<Coords, Integer> covered = EmplacementDeployment.groundCovered(getGame(), board, corner,
              emplacement, EmplacementApproach.plan(board, openGround(), null), Set.of());
        for (Coords hex : covered.keySet()) {
            assertFalse(isInTheStreet(hex), "the buildings hide " + hex.toFriendlyString());
        }
    }

    @Test
    void theEnemysRouteIntoTownRunsDownTheStreet() {
        EmplacementApproach approach = EmplacementApproach.plan(board, openGround(),
              new Coords(STREET_COLUMN, HEIGHT - 1));
        assertTrue(isInTheStreet(approach.defended()) || board.getHex(approach.defended())
              .containsTerrain(Terrains.BUILDING), "the place defended is the middle of the buildings");
        assertTrue(approach.routes().contains(new Coords(STREET_COLUMN, LAST_BUILDING_ROW)),
              "the south end of the street is on the way in");
        assertFalse(approach.routes().contains(new Coords(1, 1)), "the far corner is not");
    }

    @Test
    void groundOnTheRouteInIsWorthMoreThanGroundBesideIt() {
        EmplacementApproach approach = EmplacementApproach.plan(board, openGround(),
              new Coords(STREET_COLUMN, HEIGHT - 1));
        Coords onTheRoute = new Coords(STREET_COLUMN, LAST_BUILDING_ROW);
        Coords farCorner = new Coords(0, 0);
        assertTrue(approach.weightPercent(onTheRoute) > approach.weightPercent(farCorner));
        assertEquals(100 * EmplacementApproach.ROUTE_WEIGHT, approach.weightPercent(onTheRoute));
    }

    @Test
    void hexesNearTheRouteInAreLookedAtBeforeHexesFarFromIt() {
        EmplacementApproach approach = EmplacementApproach.plan(board, openGround(),
              new Coords(STREET_COLUMN, HEIGHT - 1));
        Coords besideTheStreetMouth = new Coords(STREET_COLUMN - 1, LAST_BUILDING_ROW + 1);
        Coords farCorner = new Coords(0, 0);
        List<Coords> sorted = EmplacementDeployment.mostPromisingFirst(List.of(farCorner, besideTheStreetMouth),
              board, emplacement, approach, EmplacementFirepower.DEFAULT_STANDOFF);
        assertEquals(List.of(besideTheStreetMouth, farCorner), sorted);
    }

    @Test
    void aHexFarFromTheTownIsNotPartOfItsDefence() {
        EmplacementApproach approach = new EmplacementApproach(new Coords(7, 7), 3, Set.of(), Set.of());
        Coords nearTown = new Coords(7, 7 + 3 + EmplacementApproach.FORWARD_MARGIN);
        Coords farOut = new Coords(7, 7 + 3 + EmplacementApproach.FORWARD_MARGIN + 1);
        assertEquals(List.of(nearTown), approach.withinTheDefence(List.of(nearTown, farOut)));
        assertEquals(List.of(farOut), approach.withinTheDefence(List.of(farOut)),
              "with nothing nearer, the far hex is still better than no hex");
    }

    @Test
    void theFrontLineIsWhereTheRouteReachesTheEdgeOfTown() {
        EmplacementApproach approach = EmplacementApproach.plan(board, openGround(),
              new Coords(STREET_COLUMN, HEIGHT - 1));
        assertFalse(approach.front().isEmpty());
        for (Coords frontHex : approach.front()) {
            assertTrue(approach.routes().contains(frontHex), frontHex.toFriendlyString() + " is not on a route");
            int fromTheMiddle = frontHex.distance(approach.defended());
            assertTrue((fromTheMiddle >= (approach.radius() - 1)) && (fromTheMiddle <= (approach.radius() + 1)),
                  frontHex.toFriendlyString() + " is not at the edge of town");
        }
    }

    @Test
    void anEmplacementLosesScoreForEveryHexOutOfItsPlaceBehindTheFront() {
        Coords frontHex = new Coords(10, 10);
        EmplacementApproach approach = new EmplacementApproach(new Coords(10, 4), 6, Set.of(frontHex),
              Set.of(frontHex));
        int standoff = 3;
        assertEquals(100, approach.depthPercent(new Coords(10, 10 - standoff), standoff));
        assertEquals(100 - EmplacementApproach.DEPTH_PERCENT_PER_HEX,
              approach.depthPercent(new Coords(10, 10 - standoff - 1), standoff));
        assertEquals(EmplacementApproach.MINIMUM_DEPTH_PERCENT, approach.depthPercent(new Coords(10, 40), standoff));
    }

    @Test
    void withNoFrontLineDepthDoesNotMatter() {
        EmplacementApproach approach = new EmplacementApproach(new Coords(7, 7), 3, Set.of(), Set.of());
        assertEquals(100, approach.depthPercent(new Coords(0, 0), 3));
    }

    @Test
    void aMissileEmplacementCountsSpottedGroundItCannotSee() {
        WeaponType rack = mock(WeaponType.class);
        when(rack.hasIndirectFire()).thenReturn(true);
        when(rack.getDamage()).thenReturn(10);
        when(rack.getShortRange()).thenReturn(7);
        when(rack.getMediumRange()).thenReturn(14);
        when(rack.getLongRange()).thenReturn(21);
        WeaponMounted mounted = mock(WeaponMounted.class);
        when(mounted.getType()).thenReturn(rack);
        when(emplacement.getWeaponList()).thenReturn(List.of(mounted));

        EmplacementApproach approach = EmplacementApproach.plan(board, openGround(),
              new Coords(STREET_COLUMN, HEIGHT - 1));
        Coords corner = new Coords(1, 1);
        Coords inTheStreet = new Coords(STREET_COLUMN, 4);
        assertTrue(EmplacementDeployment.isSampleHex(inTheStreet), "the test needs a hex the rule samples");

        Map<Coords, Integer> unaided = EmplacementDeployment.groundCovered(getGame(), board, corner, emplacement,
              approach, Set.of());
        Map<Coords, Integer> withASpotter = EmplacementDeployment.groundCovered(getGame(), board, corner,
              emplacement, approach, Set.of(inTheStreet));

        assertFalse(unaided.containsKey(inTheStreet), "the buildings hide the street from the corner");
        assertTrue(withASpotter.containsKey(inTheStreet), "a spotter lets the rack fire over them");
    }

    @Test
    void sitingAWholeGarrisonKeepsEveryEmplacementOutOfTheStreet() {
        Coords enemyCentre = new Coords(STREET_COLUMN, HEIGHT - 1);
        List<Entity> placed = new ArrayList<>();
        for (int number = 0; number < 3; number++) {
            Entity next = mock(Entity.class);
            when(next.getShortName()).thenReturn("Emplacement " + number);
            Coords chosen = EmplacementDeployment.site(getGame(), board, next, openGround(), placed, enemyCentre);
            assertNotNull(chosen);
            assertFalse(isInTheStreet(chosen), "built in the street at " + chosen.toFriendlyString());
            for (Entity other : placed) {
                assertTrue(chosen.distance(other.getPosition()) >= EmplacementDeployment.MINIMUM_SPACING_HEXES,
                      "too close to " + other.getPosition().toFriendlyString());
            }
            when(next.getPosition()).thenReturn(chosen);
            placed.add(next);
        }
    }

    @Test
    void everyStreetHexIsABottleneck() {
        Coords midStreet = new Coords(STREET_COLUMN, 7);
        assertEquals(SiteProblem.BLOCKS_PASSAGE, EmplacementDeployment.siteProblem(board, midStreet, List.of()));
    }

    @Test
    void theEmplacementIsBuiltInASquareNotInTheStreet() {
        EmplacementApproach approach = EmplacementApproach.plan(board, openGround(),
              new Coords(STREET_COLUMN, HEIGHT - 1));
        Coords chosen = EmplacementDeployment.choose(emplacement, openGround(), board, List.of(),
              site -> EmplacementDeployment.groundCovered(getGame(), board, site, emplacement, approach, Set.of()), site -> 100);
        assertNotNull(chosen);
        assertFalse(isInTheStreet(chosen), "built in the street at " + chosen.toFriendlyString());
        assertEquals(SiteProblem.NONE, EmplacementDeployment.siteProblem(board, chosen, List.of()));
    }

    @Test
    void aSecondEmplacementCoversGroundTheFirstDoesNot() {
        EmplacementApproach approach = EmplacementApproach.plan(board, openGround(),
              new Coords(STREET_COLUMN, HEIGHT - 1));
        Coords first = EmplacementDeployment.choose(emplacement, openGround(), board, List.of(),
              site -> EmplacementDeployment.groundCovered(getGame(), board, site, emplacement, approach, Set.of()), site -> 100);
        assertNotNull(first);
        Coords second = EmplacementDeployment.choose(emplacement, openGround(), board, List.of(first),
              site -> EmplacementDeployment.groundCovered(getGame(), board, site, emplacement, approach, Set.of()), site -> 100);
        assertNotNull(second);
        assertFalse(isInTheStreet(second), "built in the street at " + second.toFriendlyString());
        assertTrue(first.distance(second) >= EmplacementDeployment.MINIMUM_SPACING_HEXES,
              "the two stand too close: " + first.toFriendlyString() + " and " + second.toFriendlyString());

        Map<Coords, Integer> firstGround = EmplacementDeployment.groundCovered(getGame(), board, first,
              emplacement, approach, Set.of());
        Map<Coords, Integer> secondGround = EmplacementDeployment.groundCovered(getGame(), board, second,
              emplacement, approach, Set.of());
        assertFalse(firstGround.keySet().containsAll(secondGround.keySet()),
              "the second emplacement only repeats the first one's ground");
    }
}
