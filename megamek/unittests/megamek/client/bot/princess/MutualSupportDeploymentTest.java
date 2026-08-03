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

import static megamek.client.bot.princess.MutualSupportDeployment.FORMATION_RADIUS_HEXES;
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
 * Verifies the Mutual Support deployment rule: a force gathers on its centre of mass, terrain still chooses the hex
 * inside the formation, and only units genuinely on the board get a vote on where the formation is.
 */
class MutualSupportDeploymentTest {

    private static final Coords ANCHOR = new Coords(16, 2);

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

    @Test
    void hexesInsideTheFormationCostNothing() {
        assertEquals(0, MutualSupportDeployment.distanceOutsideFormation(ANCHOR, ANCHOR));
        Coords onTheEdge = new Coords(ANCHOR.getX() + FORMATION_RADIUS_HEXES, ANCHOR.getY());
        assertEquals(0, MutualSupportDeployment.distanceOutsideFormation(ANCHOR, onTheEdge));
    }

    @Test
    void hexesOutsideTheFormationCostTheirOverrun() {
        Coords wayOut = new Coords(ANCHOR.getX() + FORMATION_RADIUS_HEXES + 4, ANCHOR.getY());
        assertEquals(4, MutualSupportDeployment.distanceOutsideFormation(ANCHOR, wayOut));
    }

    /**
     * The load-bearing property: everything inside the formation ties, so the caller's original order survives and the
     * downstream terrain ranking still gets a free choice among in-formation hexes.
     */
    @Test
    void inFormationHexesKeepTheirOriginalOrder() {
        List<Coords> candidates = List.of(new Coords(19, 2), new Coords(14, 1), new Coords(16, 2));
        List<Coords> ordered = MutualSupportDeployment.orderByFormation(candidates, ANCHOR);

        assertEquals(candidates, ordered, "all three are within the radius, so nothing should be reordered");
    }

    @Test
    void outOfPositionHexesArePushedBehindInFormationOnes() {
        Coords nearAnchor = new Coords(16, 1);
        Coords farLeft = new Coords(0, 1);
        Coords farRight = new Coords(31, 1);
        List<Coords> ordered = MutualSupportDeployment.orderByFormation(List.of(farLeft, farRight, nearAnchor), ANCHOR);

        assertEquals(nearAnchor, ordered.getFirst(), "the in-formation hex must be scanned first");
        assertTrue(ordered.indexOf(farRight) < ordered.indexOf(farLeft),
              "of two out-of-position hexes, the nearer one should be scanned first");
    }

    /**
     * The behaviour this exists to fix: {@code rankDeploymentCoords} scans only the first twenty or so candidates, so a
     * shuffled zone-wide list means each unit effectively draws at random from the whole zone.
     */
    @Test
    void theScannedPrefixLandsInsideTheFormation() {
        List<Coords> ordered = MutualSupportDeployment.orderByFormation(zoneStrip(), ANCHOR);

        for (Coords candidate : ordered.subList(0, 20)) {
            assertEquals(0, MutualSupportDeployment.distanceOutsideFormation(ANCHOR, candidate),
                  candidate + " was inside the scanned prefix but outside the formation");
        }
    }

    @Test
    void theForceGathersOnItsOwnCentreOfMass() {
        List<Entity> friends = List.of(friendAt(2, new Coords(4, 1)), friendAt(3, new Coords(8, 1)));

        Coords anchor = MutualSupportDeployment.formationAnchor(deployingUnit, zoneStrip(), friends, mockGame);

        assertEquals(new Coords(6, 1), anchor);
    }

    /** With nothing on the board yet, the first unit seeds the formation on the middle of its own zone. */
    @Test
    void theFirstUnitAnchorsOnTheZoneCentre() {
        Coords anchor = MutualSupportDeployment.formationAnchor(deployingUnit, zoneStrip(), List.of(), mockGame);

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

        Coords anchor = MutualSupportDeployment.formationAnchor(deployingUnit,
              zoneStrip(),
              List.of(notYetDeployed, airborne, offBoard, onTheGround),
              mockGame);

        assertEquals(new Coords(20, 1), anchor, "only the deployed ground unit should count");
    }

    @Test
    void aUnitDoesNotAnchorOnItself() {
        Entity self = friendAt(1, new Coords(30, 1));

        Coords anchor = MutualSupportDeployment.formationAnchor(deployingUnit, zoneStrip(), List.of(self), mockGame);

        assertEquals(16, anchor.getX(), "with itself excluded there are no friends, so the zone centre anchors");
    }

    @Test
    void friendsOnAnotherBoardDoNotAnchorTheFormation() {
        when(mockGame.onTheSameBoard(any(), any())).thenReturn(false);
        List<Entity> friends = List.of(friendAt(2, new Coords(0, 1)));

        Coords anchor = MutualSupportDeployment.formationAnchor(deployingUnit, zoneStrip(), friends, mockGame);

        assertEquals(16, anchor.getX(), "an off-board-map friend should fall back to the zone centre");
    }

    /**
     * The formation radius is a doctrine figure, not a map figure, so it does not grow with the zone. When the zone is
     * already tighter than a formation there is nothing to gather and the rule must get out of the way entirely.
     */
    @Test
    void aZoneSmallerThanTheFormationIsLeftExactlyAsItWas() {
        List<Coords> tinyZone = new ArrayList<>();
        for (int x = 14; x < 19; x++) {
            tinyZone.add(new Coords(x, 1));
        }

        List<Coords> ordered = MutualSupportDeployment.prioritize(deployingUnit, tinyZone, List.of(), mockGame);

        assertEquals(tinyZone, ordered, "every hex ties at zero, so the shuffled order must survive untouched");
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

        Coords anchor = MutualSupportDeployment.formationAnchor(deployingUnit, cornerZone, List.of(), mockGame);

        assertNotNull(anchor);
        assertTrue(cornerZone.contains(anchor), "a corner zone must anchor inside itself, not at the board centre");
    }

    /**
     * A split zone puts the notional centre between the two halves, where nothing can deploy. Only the first unit ever
     * uses that point; it picks whichever real hex is nearest, and every unit after it gathers on that unit, so the
     * force still ends up in one place rather than split down the middle.
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

        Coords seedAnchor = MutualSupportDeployment.formationAnchor(deployingUnit, splitZone, List.of(), mockGame);
        assertFalse(splitZone.contains(seedAnchor), "the notional centre of a split zone falls in the gap");

        Entity firstDeployed = friendAt(2, new Coords(30, 1));
        List<Coords> ordered = MutualSupportDeployment.prioritize(deployingUnit,
              splitZone,
              List.of(firstDeployed),
              mockGame);

        assertEquals(new Coords(28, 1),
              ordered.getFirst(),
              "once one unit is down, the rest must gather on it rather than on the empty midpoint");
    }

    @Test
    void aSingleCandidateIsHandedBackUntouched() {
        List<Coords> onlyOption = List.of(new Coords(3, 1));

        assertSame(onlyOption, MutualSupportDeployment.prioritize(deployingUnit, onlyOption, List.of(), mockGame));
    }

    @Test
    void anEmptyPositionSetHasNoCentre() {
        assertNull(MutualSupportDeployment.centroid(List.of()));
    }

    /**
     * Deployment may reorder where a unit starts, but it must never remove somewhere it could have gone: the standing
     * constraint is that Mutual Support cannot cost either bot its ability to close and fight.
     */
    @Test
    void everyLegalHexSurvivesTheReordering() {
        List<Coords> zone = zoneStrip();
        List<Entity> friends = List.of(friendAt(2, new Coords(4, 1)));

        List<Coords> ordered = MutualSupportDeployment.prioritize(deployingUnit, zone, friends, mockGame);

        assertEquals(zone.size(), ordered.size());
        assertTrue(ordered.containsAll(zone));
        assertFalse(ordered.isEmpty());
    }
}
