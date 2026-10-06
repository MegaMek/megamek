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

import static megamek.client.bot.princess.FormationGeometry.MINIMUM_SPACING_HEXES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Mutual Support deployment band: a force forms up inside a radius sized from its own average supporting
 * range and tuned by the mutual support setting, no unit stands closer than the minimum spacing, terrain still chooses
 * the hex inside that band, and only units genuinely on the board get a vote on where the formation is.
 */
class MutualSupportDeploymentTest {

    private static final Coords ANCHOR = new Coords(16, 2);

    /** A typical short-range brawler's optimum range, used where the exact figure does not matter. */
    private static final int BRAWLER_RANGE = 4;

    private Game mockGame;
    private Entity deployingUnit;

    @BeforeEach
    void setUp() {
        mockGame = mock(Game.class);
        deployingUnit = mock(Entity.class);
        when(deployingUnit.getId()).thenReturn(1);
        when(mockGame.onTheSameBoard(any(), any())).thenReturn(true);
    }

    private Entity friendAt(int id, Coords position) {
        Entity friend = mock(Entity.class);
        when(friend.getId()).thenReturn(id);
        when(friend.getPosition()).thenReturn(position);
        when(friend.isDeployed()).thenReturn(true);
        when(friend.isOffBoard()).thenReturn(false);
        when(friend.isAirborne()).thenReturn(false);
        return friend;
    }

    /** A whole deployment strip across a 32-wide board, in the shuffled order the bot would receive. */
    private List<Coords> zoneStrip() {
        List<Coords> strip = new ArrayList<>();
        for (int x = 0; x < 32; x++) {
            strip.add(new Coords(x, 1));
            strip.add(new Coords(x, 2));
        }
        return strip;
    }

    private static int forceDiameter(List<Coords> positions) {
        int widest = 0;
        for (Coords first : positions) {
            for (Coords second : positions) {
                widest = Math.max(widest, first.distance(second));
            }
        }
        return widest;
    }

    // ---------------------------------------------------------------- the band

    @Test
    void aHexInsideSupportingRangeCostsNothing() {
        assertEquals(0, MutualSupportDeployment.outOfSupport(ANCHOR, ANCHOR, BRAWLER_RANGE));
        Coords onTheEdge = new Coords(ANCHOR.getX() + BRAWLER_RANGE, ANCHOR.getY());
        assertEquals(0, MutualSupportDeployment.outOfSupport(onTheEdge, ANCHOR, BRAWLER_RANGE));
    }

    @Test
    void aHexBeyondSupportingRangeCostsItsOverrun() {
        Coords wayOut = new Coords(ANCHOR.getX() + BRAWLER_RANGE + 3, ANCHOR.getY());
        assertEquals(3, MutualSupportDeployment.outOfSupport(wayOut, ANCHOR, BRAWLER_RANGE));
    }

    @Test
    void aHexCrowdingAFriendCostsTheSpacingItSteals() {
        List<Coords> friend = List.of(new Coords(10, 1));

        assertEquals(MINIMUM_SPACING_HEXES, MutualSupportDeployment.crowding(new Coords(10, 1), friend));
        assertEquals(1, MutualSupportDeployment.crowding(new Coords(11, 1), friend));
        assertEquals(0, MutualSupportDeployment.crowding(new Coords(12, 1), friend));
        assertEquals(0, MutualSupportDeployment.crowding(new Coords(20, 1), friend));
    }

    @Test
    void crowdingIsJudgedAgainstTheNearestFriendOnly() {
        List<Coords> friends = List.of(new Coords(0, 1), new Coords(10, 1), new Coords(31, 1));

        assertEquals(1,
              MutualSupportDeployment.crowding(new Coords(11, 1), friends),
              "distant friends must not add to a crowding penalty owed to the near one");
    }

    /** A unit with no friends yet cannot be crowded by anybody. */
    @Test
    void theFirstUnitIsNeverCrowded() {
        assertEquals(0, MutualSupportDeployment.crowding(new Coords(5, 1), List.of()));
    }

    // ------------------------------------------------------- the formation radius

    /**
     * The radius is half the force's average supporting range, so the formation's <em>diameter</em> comes out at that
     * average: any two units in it are within supporting range of each other, which is what mutual support means.
     */
    @Test
    void theFormationIsSizedSoAnyTwoUnitsCanSupportEachOther() {
        List<Integer> company = List.of(8, 10, 12, 14);

        assertEquals(6, FormationGeometry.formationRadiusFor(company, 1.0), "mean 11, halved and rounded");
    }

    /** Higher setting, tighter formation: the multiplier divides, so asking for more support pulls the force in. */
    @Test
    void moreMutualSupportMeansATighterFormation() {
        List<Integer> company = List.of(12);

        int loose = FormationGeometry.formationRadiusFor(company, 0.6);
        int standard = FormationGeometry.formationRadiusFor(company, 1.0);
        int tight = FormationGeometry.formationRadiusFor(company, 2.0);

        assertTrue(loose > standard, "a lower setting must spread the force out");
        assertTrue(tight < standard, "a higher setting must pull the force in");
        assertEquals(6, standard);
        assertEquals(3, tight);
    }

    /**
     * At the bottom of the slider the radius outgrows any real deployment zone, so the rule stops constraining
     * anything and deployment falls back to stock scatter.
     */
    @Test
    void theLowestSettingStopsConstrainingDeploymentAtAll() {
        int radius = FormationGeometry.formationRadiusFor(List.of(12), 0.1);

        assertTrue(radius > 32, "at the lowest setting the radius should exceed a whole board, was " + radius);
    }

    @Test
    void aForceWithNothingToShootFallsBackToMinimumSpacing() {
        assertEquals(MINIMUM_SPACING_HEXES, FormationGeometry.formationRadiusFor(List.of(), 1.0));
    }

    @Test
    void theRadiusNeverCollapsesBelowTheMinimumSpacing() {
        assertEquals(MINIMUM_SPACING_HEXES,
              FormationGeometry.formationRadiusFor(List.of(2), 2.0),
              "a point-blank force still needs a band wide enough to stand in");
    }

    @Test
    void aLongRangedUnitMayFormUpFurtherOutThanABrawler() {
        Coords wayOut = new Coords(ANCHOR.getX() + 9, ANCHOR.getY());

        assertEquals(5, MutualSupportDeployment.outOfSupport(wayOut, ANCHOR, BRAWLER_RANGE));
        assertEquals(0,
              MutualSupportDeployment.outOfSupport(wayOut, ANCHOR, 12),
              "a missile boat supports the centre from further out, so the same hex is in position for it");
    }

    // ---------------------------------------------------------------- ordering

    /**
     * The load-bearing property: everything in position ties, so the caller's original order survives and the
     * downstream terrain ranking still gets a free choice among in-position hexes.
     */
    @Test
    void inPositionHexesKeepTheirOriginalOrder() {
        List<Coords> candidates = List.of(new Coords(19, 2), new Coords(14, 1), new Coords(16, 2));

        List<Coords> ordered = MutualSupportDeployment.orderByFormation(candidates, ANCHOR, List.of(), 6);

        assertEquals(candidates, ordered, "all three are in position, so nothing should be reordered");
    }

    @Test
    void outOfPositionHexesArePushedBehindInPositionOnes() {
        Coords nearAnchor = new Coords(16, 1);
        Coords farLeft = new Coords(0, 1);
        Coords farRight = new Coords(31, 1);

        List<Coords> ordered = MutualSupportDeployment.orderByFormation(List.of(farLeft, farRight, nearAnchor),
              ANCHOR,
              List.of(),
              BRAWLER_RANGE);

        assertEquals(nearAnchor, ordered.getFirst(), "the in-position hex must be scanned first");
        assertTrue(ordered.indexOf(farRight) < ordered.indexOf(farLeft),
              "of two out-of-position hexes, the nearer one should be scanned first");
    }

    /**
     * The two ends of the band are ranked, not added. A shallow deployment strip has nowhere near enough room for a
     * company to hold both a tight radius and a clear hex between every pair, so when they conflict concentration wins
     * and spacing does the most it can within it. Adding the two penalties instead let spacing be traded away, and a
     * measured company came out with its units standing shoulder to shoulder.
     */
    @Test
    void spacingBreaksTiesButNeverOutranksSupport() {
        List<Coords> friend = List.of(new Coords(16, 2));
        Coords crowdedButSupported = new Coords(17, 2);
        Coords roomyButUnsupported = new Coords(16 + BRAWLER_RANGE + 1, 2);
        Coords roomyAndSupported = new Coords(16 + BRAWLER_RANGE, 2);

        List<Coords> ordered = MutualSupportDeployment.orderByFormation(
              List.of(crowdedButSupported, roomyButUnsupported, roomyAndSupported),
              ANCHOR,
              friend,
              BRAWLER_RANGE);

        assertEquals(roomyAndSupported, ordered.getFirst(), "in support and uncrowded is the best hex available");
        assertEquals(crowdedButSupported,
              ordered.get(1),
              "a crowded hex inside support still beats a roomy one outside it");
    }

    /**
     * The behaviour this exists to fix: {@code rankDeploymentCoords} scans only the first twenty or so candidates, so a
     * shuffled zone-wide list means each unit effectively draws at random from the whole zone.
     */
    @Test
    void theScannedPrefixLandsInsideTheBand() {
        List<Coords> friends = List.of(new Coords(16, 1));

        List<Coords> ordered = MutualSupportDeployment.orderByFormation(zoneStrip(), ANCHOR, friends, 6);

        for (Coords candidate : ordered.subList(0, 20)) {
            assertTrue(MutualSupportDeployment.isInPosition(candidate, ANCHOR, friends, 6),
                  candidate + " was inside the scanned prefix but out of position");
        }
    }

    /**
     * The reason the upper bound is measured against the centre of mass and never against the nearest friend.
     *
     * <p>Deploying a full company one unit at a time, "stay within supporting range of <em>somebody</em>" is satisfied
     * by a chain that walks right across the map - which is the picket line this rule exists to break. Gathering on the
     * centre keeps the force compact instead of merely connected.</p>
     */
    @Test
    void aWholeCompanyFormsUpCompactRatherThanInAChain() {
        List<Coords> zone = zoneStrip();
        List<Coords> placed = new ArrayList<>();

        for (int unit = 0; unit < 12; unit++) {
            Coords anchor = FormationGeometry.centroid(placed.isEmpty() ? zone : placed);
            List<Coords> ordered = MutualSupportDeployment.orderByFormation(zone, anchor, placed, BRAWLER_RANGE);
            for (Coords candidate : ordered) {
                if (!placed.contains(candidate)) {
                    placed.add(candidate);
                    break;
                }
            }
        }

        assertEquals(12, placed.size());
        assertTrue(forceDiameter(placed) <= 14,
              "a twelve-unit company should form up compact, was " + forceDiameter(placed) + " hexes wide");
        assertTrue(forceDiameter(placed) >= MINIMUM_SPACING_HEXES,
              "the company must not stack itself into a single hex");
    }

    // ---------------------------------------------------------------- the anchor

    @Test
    void theForceGathersOnItsOwnCentreOfMass() {
        List<Entity> friends = List.of(friendAt(2, new Coords(4, 1)), friendAt(3, new Coords(8, 1)));

        List<Coords> positions = MutualSupportDeployment.anchorPositions(deployingUnit, friends, mockGame);

        assertEquals(new Coords(6, 1), FormationGeometry.centroid(positions));
    }

    /** With nothing on the board yet, the first unit seeds the formation on the middle of its own zone. */
    @Test
    void theFirstUnitAnchorsOnTheZoneCentre() {
        List<Coords> positions = MutualSupportDeployment.anchorPositions(deployingUnit, List.of(), mockGame);
        assertTrue(positions.isEmpty());

        Coords anchor = FormationGeometry.centroid(zoneStrip());

        assertNotNull(anchor);
        assertEquals(16, anchor.getX(), "a 0-31 strip should anchor near its middle");
    }

    @Test
    void undeployedAirborneAndOffBoardFriendsDoNotAnchorTheFormation() {
        Entity notYetDeployed = friendAt(2, null);
        when(notYetDeployed.isDeployed()).thenReturn(false);
        Entity airborne = friendAt(3, new Coords(1, 1));
        when(airborne.isAirborne()).thenReturn(true);
        Entity offBoard = friendAt(4, new Coords(2, 1));
        when(offBoard.isOffBoard()).thenReturn(true);
        Entity onTheGround = friendAt(5, new Coords(20, 1));

        List<Coords> positions = MutualSupportDeployment.anchorPositions(deployingUnit,
              List.of(notYetDeployed, airborne, offBoard, onTheGround),
              mockGame);

        assertEquals(List.of(new Coords(20, 1)), positions, "only the deployed ground unit should count");
    }

    @Test
    void aUnitDoesNotAnchorOnItself() {
        Entity self = friendAt(1, new Coords(30, 1));

        assertTrue(MutualSupportDeployment.anchorPositions(deployingUnit, List.of(self), mockGame).isEmpty());
    }

    @Test
    void friendsOnAnotherBoardDoNotAnchorTheFormation() {
        when(mockGame.onTheSameBoard(any(), any())).thenReturn(false);
        List<Entity> friends = List.of(friendAt(2, new Coords(0, 1)));

        assertTrue(MutualSupportDeployment.anchorPositions(deployingUnit, friends, mockGame).isEmpty());
    }

    @Test
    void anEmptyPositionSetHasNoCentre() {
        assertNull(FormationGeometry.centroid(List.of()));
    }

    // ---------------------------------------------------------------- zone shapes

    /**
     * The band is a doctrine figure, not a map figure, so it does not grow with the zone. When the zone is already
     * tighter than a formation there is nothing to gather and the rule must get out of the way entirely.
     */
    @Test
    void aZoneSmallerThanTheBandIsLeftExactlyAsItWas() {
        List<Coords> tinyZone = new ArrayList<>();
        for (int x = 14; x < 19; x++) {
            tinyZone.add(new Coords(x, 1));
        }

        List<Coords> ordered = MutualSupportDeployment.orderByFormation(tinyZone,
              FormationGeometry.centroid(tinyZone),
              List.of(),
              BRAWLER_RANGE);

        assertEquals(tinyZone, ordered, "every hex is in position, so the shuffled order must survive untouched");
    }

    /** The anchor is derived from the candidates themselves, so it tracks whatever zone the scenario hands out. */
    @Test
    void theAnchorFollowsTheZoneItIsGiven() {
        List<Coords> cornerZone = new ArrayList<>();
        for (int x = 24; x < 32; x++) {
            for (int y = 28; y < 34; y++) {
                cornerZone.add(new Coords(x, y));
            }
        }

        Coords anchor = FormationGeometry.centroid(cornerZone);

        assertNotNull(anchor);
        assertTrue(cornerZone.contains(anchor), "a corner zone must anchor inside itself, not at the board centre");
    }

    /**
     * A split zone puts the notional centre between the two halves, where nothing can deploy. Only the first unit ever
     * uses that point; every unit after it gathers on that unit, so the force still ends up in one place rather than
     * split down the middle.
     */
    @Test
    void aSplitZoneStillGathersTheForceInOnePlace() {
        List<Coords> splitZone = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            splitZone.add(new Coords(x, 1));
        }
        for (int x = 28; x < 32; x++) {
            splitZone.add(new Coords(x, 1));
        }

        Coords seedAnchor = FormationGeometry.centroid(splitZone);
        assertFalse(splitZone.contains(seedAnchor), "the notional centre of a split zone falls in the gap");

        List<Coords> firstDeployed = List.of(new Coords(30, 1));
        List<Coords> ordered = MutualSupportDeployment.orderByFormation(splitZone,
              FormationGeometry.centroid(firstDeployed),
              firstDeployed,
              BRAWLER_RANGE);

        assertEquals(new Coords(28, 1),
              ordered.getFirst(),
              "once one unit is down, the rest must gather on it rather than on the empty midpoint");
    }

    // ---------------------------------------------------------------- guardrails

    @Test
    void aSingleCandidateIsHandedBackUntouched() {
        List<Coords> onlyOption = List.of(new Coords(3, 1));

        assertSame(onlyOption,
              MutualSupportDeployment.prioritize(deployingUnit, onlyOption, List.of(), mockGame, BRAWLER_RANGE));
    }

    /**
     * Deployment may reorder where a unit starts, but it must never remove somewhere it could have gone: the standing
     * constraint is that Mutual Support cannot cost either bot its ability to close and fight.
     */
    @Test
    void everyLegalHexSurvivesTheReordering() {
        List<Coords> zone = zoneStrip();

        List<Coords> ordered = MutualSupportDeployment.orderByFormation(zone,
              ANCHOR,
              List.of(new Coords(16, 1)),
              BRAWLER_RANGE);

        assertEquals(zone.size(), ordered.size());
        assertTrue(ordered.containsAll(zone));
    }
}
