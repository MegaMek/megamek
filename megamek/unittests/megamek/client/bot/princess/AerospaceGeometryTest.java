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

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Pins the bot's dead-zone prediction to the rules engine's.
 *
 * <p>Built on a real {@link Game} with real boards and real fighters rather than mocks, following
 * {@code HexPropertiesMapTest}: the whole point is that {@link AerospaceGeometry} agrees with
 * {@code Compute.inDeadZone}, and a mocked engine would only assert the test agrees with itself.</p>
 */
class AerospaceGeometryTest {

    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 80;

    /** Distance from here to {@code new Coords(0, n)} is exactly {@code n}, which lets us hit boundaries. */
    private static final Coords ORIGIN = new Coords(0, 0);

    // --- harness -------------------------------------------------------------------------------------

    private record Engagement(Game game, Entity attacker, Entity target, AerospaceVenue venue) {

        void place(int separation, int attackerAltitude, int targetAltitude) {
            attacker.setPosition(ORIGIN);
            target.setPosition(new Coords(0, separation));
            attacker.setAltitude(attackerAltitude);
            target.setAltitude(targetAltitude);
        }

        boolean engineSaysDeadZone() {
            return Compute.inDeadZone(game, attacker, target);
        }

        boolean botSaysDeadZone() {
            return AerospaceGeometry.inDeadZone(venue, attacker.getPosition(), attacker.getAltitude(),
                  target.getPosition(), target.getAltitude());
        }
    }

    /**
     * The two-argument {@link Board} constructor leaves the hexes null, and the engine's range code walks
     * them looking for buildings, so the array constructor is the one to use here.
     */
    private static Board board(BoardType boardType) {
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        board.setBoardType(boardType);
        return board;
    }

    private static Engagement engagement(BoardType boardType) {
        Board board = board(boardType);
        Game game = new Game();
        game.setBoard(board);

        Entity attacker = fighter(game, 1);
        Entity target = fighter(game, 2);
        AerospaceVenue venue = boardType.isGround() ? AerospaceVenue.GROUND_MAP : AerospaceVenue.LOW_ALTITUDE;
        return new Engagement(game, attacker, target, venue);
    }

    private static Entity fighter(Game game, int id) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setId(id);
        fighter.setGame(game);
        fighter.setPosition(ORIGIN);
        fighter.setAltitude(5);
        // Without this the engine reports the unit as not on a ground map at all, and the x16 range
        // conversion never runs - see IGame.isOnGroundMap(Targetable).
        fighter.setDeployed(true);
        game.addEntity(fighter);
        return fighter;
    }

    // --- the load-bearing agreement sweeps -----------------------------------------------------------

    @Test
    void botAgreesWithEngineOnGroundMapAcrossAltitudesAndDistances() {
        Engagement engagement = engagement(BoardType.GROUND);
        assertEquals(AerospaceVenue.GROUND_MAP, engagement.venue());

        for (int attackerAltitude = 1; attackerAltitude <= 8; attackerAltitude++) {
            for (int targetAltitude = 1; targetAltitude <= 8; targetAltitude++) {
                for (int separation = 0; separation <= 70; separation++) {
                    engagement.place(separation, attackerAltitude, targetAltitude);
                    assertEquals(engagement.engineSaysDeadZone(), engagement.botSaysDeadZone(),
                          "ground map, altitudes " + attackerAltitude + " vs " + targetAltitude
                                + ", separation " + separation);
                }
            }
        }
    }

    @Test
    void botAgreesWithEngineAtLowAltitudeAcrossAltitudesAndDistances() {
        Engagement engagement = engagement(BoardType.SKY);
        assertEquals(AerospaceVenue.LOW_ALTITUDE, engagement.venue());

        for (int attackerAltitude = 1; attackerAltitude <= 8; attackerAltitude++) {
            for (int targetAltitude = 1; targetAltitude <= 8; targetAltitude++) {
                for (int separation = 0; separation <= 20; separation++) {
                    engagement.place(separation, attackerAltitude, targetAltitude);
                    assertEquals(engagement.engineSaysDeadZone(), engagement.botSaysDeadZone(),
                          "low altitude, altitudes " + attackerAltitude + " vs " + targetAltitude
                                + ", separation " + separation);
                }
            }
        }
    }

    // --- the x16 boundary, where integer division and ceil disagree ----------------------------------

    @Test
    void oneAltitudeApartOnAGroundMapBlocksFireInsideSeventeenHexes() {
        Engagement engagement = engagement(BoardType.GROUND);

        // 16 ground hexes rounds up to 1 low-altitude hex, which one altitude of separation covers.
        engagement.place(16, 5, 4);
        assertTrue(engagement.botSaysDeadZone(), "16 hexes at one altitude apart should be blocked");
        assertTrue(engagement.engineSaysDeadZone());

        // 17 rounds up to 2, which clears the cone. Integer division would still say 1 and block the shot.
        engagement.place(17, 5, 4);
        assertFalse(engagement.botSaysDeadZone(), "17 hexes at one altitude apart should be a legal shot");
        assertFalse(engagement.engineSaysDeadZone());
    }

    @Test
    void twoAltitudesApartOnAGroundMapBlocksFireInsideThirtyThreeHexes() {
        Engagement engagement = engagement(BoardType.GROUND);

        engagement.place(32, 5, 3);
        assertTrue(engagement.botSaysDeadZone(), "32 hexes at two altitudes apart should be blocked");
        assertTrue(engagement.engineSaysDeadZone());

        engagement.place(33, 5, 3);
        assertFalse(engagement.botSaysDeadZone(), "33 hexes at two altitudes apart should be a legal shot");
        assertFalse(engagement.engineSaysDeadZone());
    }

    /**
     * Matching altitude is what makes an engagement possible at all, which is the doctrine this whole change
     * rests on. The sweep starts one hex out because the engine treats two units sharing a hex at the same
     * altitude as a dead zone too - {@code 0 >= 0} - and this test is about approach geometry, not stacking.
     */
    @Test
    void matchedAltitudeIsNeverBlocked() {
        Engagement engagement = engagement(BoardType.GROUND);
        for (int separation = 1; separation <= 40; separation++) {
            engagement.place(separation, 5, 5);
            assertFalse(engagement.botSaysDeadZone(), "matched altitude at " + separation + " hexes");
            assertEquals(engagement.engineSaysDeadZone(), engagement.botSaysDeadZone());
        }
    }

    /** TW p.241: one altitude apart needs two hexes, two altitudes needs three, and so on. */
    @Test
    void lowAltitudeMinimumSeparationMatchesTheBook() {
        Engagement engagement = engagement(BoardType.SKY);

        for (int altitudeDifference = 1; altitudeDifference <= 6; altitudeDifference++) {
            int requiredSeparation = altitudeDifference + 1;

            engagement.place(requiredSeparation - 1, 7, 7 - altitudeDifference);
            assertTrue(engagement.botSaysDeadZone(),
                  altitudeDifference + " altitudes apart at " + (requiredSeparation - 1) + " hexes is blocked");

            engagement.place(requiredSeparation, 7, 7 - altitudeDifference);
            assertFalse(engagement.botSaysDeadZone(),
                  altitudeDifference + " altitudes apart at " + requiredSeparation + " hexes is legal");
        }
    }

    // --- effective range ------------------------------------------------------------------------------

    /** TW p.241's own worked example: ten hexes apart at altitudes 3 and 5 is an effective twelve. */
    @Test
    void effectiveRangeAddsOneHexPerAltitudeLevel() {
        assertEquals(12, AerospaceGeometry.effectiveRange(AerospaceVenue.LOW_ALTITUDE, ORIGIN, 5,
              new Coords(0, 10), 3));
    }

    @Test
    void effectiveRangeConvertsGroundHexesBeforeAddingAltitude() {
        // 32 ground hexes is 2 low-altitude hexes, plus 2 levels of separation.
        assertEquals(4, AerospaceGeometry.effectiveRange(AerospaceVenue.GROUND_MAP, ORIGIN, 5,
              new Coords(0, 32), 3));
    }

    // --- the spheroid exemption ----------------------------------------------------------------------

    /** TW p.241: spheroids fire nose weapons up and aft weapons down, so geometry never bars them. */
    @Test
    void spheroidsAreNotBlockedByTheirOwnDeadZone() {
        Coords targetPosition = new Coords(0, 1);
        assertTrue(AerospaceGeometry.inDeadZone(AerospaceVenue.LOW_ALTITUDE, ORIGIN, 5, targetPosition, 3),
              "geometry still reports the cone");
        assertTrue(AerospaceGeometry.deadZoneBlocksAttack(AerospaceVenue.LOW_ALTITUDE, ORIGIN, 5, false,
              targetPosition, 3), "an aerodyne is barred");
        assertFalse(AerospaceGeometry.deadZoneBlocksAttack(AerospaceVenue.LOW_ALTITUDE, ORIGIN, 5, true,
              targetPosition, 3), "a spheroid is not");
    }

    // --- reachable altitude band ---------------------------------------------------------------------

    @Test
    void reachableBandIsAsymmetricBecauseDivingIsFree() {
        Game game = new Game();
        game.setBoard(board(BoardType.SKY));
        Entity fighter = fighter(game, 1);
        fighter.setAltitude(5);

        AerospaceGeometry.AltitudeBand band = AerospaceGeometry.reachableAltitudeBand(fighter);

        assertEquals(3, band.lowest(), "a fighter will shed two levels for free");
        assertTrue(band.highest() >= 5, "and can hold or climb from where it is");
        assertTrue(band.contains(5));
        assertFalse(band.contains(2));
    }

    @Test
    void reachableBandStaysWithinLegalAltitudes() {
        Game game = new Game();
        game.setBoard(board(BoardType.SKY));
        Entity fighter = fighter(game, 1);
        fighter.setAltitude(1);

        AerospaceGeometry.AltitudeBand band = AerospaceGeometry.reachableAltitudeBand(fighter);

        assertEquals(AerospaceGeometry.MINIMUM_ALTITUDE, band.lowest(), "cannot descend below altitude 1");
        assertTrue(band.highest() <= AerospaceGeometry.MAXIMUM_ALTITUDE);
    }

    // --- distance to the board edge --------------------------------------------------------------------

    /** The edge walk that prices off-board headings: straight-line hexes remaining before leaving the board. */
    @Test
    void hexesUntilOffBoardCountsTheStraightRun() {
        Board board = board(BoardType.GROUND);

        // Facing 0 is north: from y=5 there are 5 hexes before y goes negative.
        assertEquals(5, AerospaceGeometry.hexesUntilOffBoard(new Coords(5, 5), 0, board, 50));
        // Facing 3 is south on a 80-tall board: capped by the maximum, not the edge.
        assertEquals(20, AerospaceGeometry.hexesUntilOffBoard(new Coords(5, 5), 3, board, 20));
        // Null-safety returns the cap rather than a crash mid-ranking.
        assertEquals(9, AerospaceGeometry.hexesUntilOffBoard(null, 0, board, 9));
    }

    /** The lateral companion: distance to the nearest edge, whatever the pose points at. */
    @Test
    void hexesToNearestEdgeMeasuresTheClosestBoundary() {
        Board board = board(BoardType.GROUND);

        // On the west column: hugging an edge even when facing along it.
        assertEquals(0, AerospaceGeometry.hexesToNearestEdge(new Coords(0, 37), board));
        // Mid-board on a 10-wide board: the near side is 4 columns away.
        assertEquals(4, AerospaceGeometry.hexesToNearestEdge(new Coords(4, 40), board));
        // Corner-adjacent: the closer of the two axes wins.
        assertEquals(1, AerospaceGeometry.hexesToNearestEdge(new Coords(1, 3), board));
    }
}
