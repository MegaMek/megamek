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

import megamek.common.Hex;
import megamek.common.ManeuverType;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the two decisions that make the aerospace doctrine react rather than guess: telling a committed
 * opponent from one still to move, and pricing a control roll by how far there is to fall.
 */
class AerospacePathRankerTest {

    private AerospacePathRanker ranker;

    @BeforeEach
    void beforeEach() {
        // The base ranker reads behaviour settings while constructing, so a bare mock is not enough.
        Princess princess = mock(Princess.class);
        when(princess.getBehaviorSettings()).thenReturn(new BehaviorSettings());
        ranker = new AerospacePathRanker(princess);
    }

    private static Entity airborneFighter(boolean stillToMove) {
        Entity fighter = mock(Entity.class);
        when(fighter.isAero()).thenReturn(true);
        when(fighter.isAirborne()).thenReturn(true);
        when(fighter.isImmobile()).thenReturn(false);
        when(fighter.isSelectableThisTurn()).thenReturn(stillToMove);
        return fighter;
    }

    // --- the reaction fix ---------------------------------------------------------------------------

    /**
     * The defect this whole doctrine hangs off. The stock ranker adds {@code isAirborneAeroOnGroundMap()} to
     * this test, so an enemy fighter reports as committed before it has moved and the bot matches a stale
     * altitude. Reverting the override makes this case fail.
     */
    @Test
    void anEnemyFighterThatHasNotMovedIsNotTreatedAsCommitted() {
        Entity enemy = airborneFighter(true);
        when(enemy.isAirborneAeroOnGroundMap()).thenReturn(true);

        assertFalse(ranker.evaluateAsMoved(enemy),
              "a fighter still to move this turn has not committed to an altitude");
    }

    @Test
    void anEnemyFighterThatHasMovedIsTreatedAsCommitted() {
        Entity enemy = airborneFighter(false);
        when(enemy.isAirborneAeroOnGroundMap()).thenReturn(true);

        assertTrue(ranker.evaluateAsMoved(enemy), "a fighter that has moved has committed");
    }

    @Test
    void anImmobileFighterCountsAsCommittedWhereverItIs() {
        Entity enemy = airborneFighter(true);
        when(enemy.isImmobile()).thenReturn(true);

        assertTrue(ranker.evaluateAsMoved(enemy), "an immobile fighter is going nowhere");
    }

    @Test
    void groundUnitsStillUseTheStockRule() {
        Entity mek = mock(Entity.class);
        when(mek.isAero()).thenReturn(false);
        when(mek.isAirborne()).thenReturn(false);
        when(mek.isSelectableThisTurn()).thenReturn(false);
        when(mek.isImmobile()).thenReturn(false);
        when(mek.isAirborneAeroOnGroundMap()).thenReturn(false);

        assertTrue(ranker.evaluateAsMoved(mek), "a ground unit that has moved is still evaluated as moved");
    }

    // --- control-roll risk, graded by altitude -------------------------------------------------------

    /**
     * A failed control roll costs 1d6 altitude, so the risk of it ending on the ground is the chance the die
     * comes up at least the current altitude. The stock code treats that risk as the same everywhere.
     */
    @Test
    void theOddsOfHittingTheGroundFallAwayWithAltitude() {
        assertEquals(1.0, AerospacePathRanker.oddsOfReachingTheGround(1), 0.0001,
              "at altitude 1 any roll reaches the ground");
        assertEquals(5.0 / 6.0, AerospacePathRanker.oddsOfReachingTheGround(2), 0.0001);
        assertEquals(3.0 / 6.0, AerospacePathRanker.oddsOfReachingTheGround(4), 0.0001);
        assertEquals(1.0 / 6.0, AerospacePathRanker.oddsOfReachingTheGround(6), 0.0001);
    }

    @Test
    void aboveSixAltitudesADieCannotReachTheGround() {
        assertEquals(0.0, AerospacePathRanker.oddsOfReachingTheGround(7), 0.0001);
        assertEquals(0.0, AerospacePathRanker.oddsOfReachingTheGround(10), 0.0001);
    }

    @Test
    void theRiskGradingIsMonotonic() {
        for (int altitude = 1; altitude < AerospaceGeometry.MAXIMUM_ALTITUDE; altitude++) {
            assertTrue(AerospacePathRanker.oddsOfReachingTheGround(altitude)
                        >= AerospacePathRanker.oddsOfReachingTheGround(altitude + 1),
                  "risk must never rise with altitude, checked at " + altitude);
        }
    }

    // --- the maneuver doctrine gate -----------------------------------------------------------------

    private static MovePath maneuverPathAt(Coords position, int facing, Game game, Board board) {
        Entity fighter = mock(Entity.class);
        when(fighter.getPosition()).thenReturn(position);
        when(fighter.getFacing()).thenReturn(facing);
        // A competent crew by default, so the odds gate stays out of tests that are not about it.
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(4);
        when(fighter.getCrew()).thenReturn(crew);
        when(fighter.isFighter()).thenReturn(true);
        MovePath path = mock(MovePath.class);
        when(path.getEntity()).thenReturn(fighter);
        when(path.getFinalBoardId()).thenReturn(0);
        when(game.getBoard(0)).thenReturn(board);
        return path;
    }

    private static Board groundBoard(int width, int height) {
        Hex[] hexes = new Hex[width * height];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        return new Board(width, height, hexes);
    }

    /**
     * Dave's rule, enforced where the game state is current: an offensive maneuver may only be scored
     * against an enemy that has already moved. Path generation cannot hold this gate - it runs inside
     * Precognition before any enemy commits - so this is the check that keeps the rule alive.
     */
    @Test
    void offensiveManeuversRequireACommittedEnemy() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        MovePath path = maneuverPathAt(new Coords(20, 20), 0, game, board);

        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 0, path, game,
              AerospaceVenue.GROUND_MAP), "no committed enemy, no Split-S");
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 1, path, game,
              AerospaceVenue.GROUND_MAP), "one committed enemy opens the offensive set");
    }

    /**
     * The ground fall machinery must never price an airborne aerospace path. Its two consumers - the
     * fall-tolerance cull and fallShame, up to UNIT_DESTRUCTION_FACTOR at probability zero - buried every
     * maneuver path by hundreds of points (a post-Hammerhead stall read as certain destruction), which is
     * why CASPAR ranked maneuvers across four live games and never flew one. Control risk for aeros is
     * priced at crash scale by controlRiskPenalty and maneuverRiskPenalty instead.
     */
    @Test
    void airborneAeroPathsIgnoreTheGroundFallMachinery() {
        Entity fighter = mock(Entity.class);
        when(fighter.isAero()).thenReturn(true);
        when(fighter.isAirborne()).thenReturn(true);
        when(fighter.isSpaceborne()).thenReturn(false);
        MovePath path = mock(MovePath.class);
        when(path.getEntity()).thenReturn(fighter);

        assertEquals(1.0, ranker.getMovePathSuccessProbability(path), 0.0001,
              "control risk is priced by the aero terms, not by fallShame");
    }

    /**
     * With the fall machinery switched off, the stall must be priced honestly here instead: an aerodyne
     * ending its move at velocity zero rolls against piloting + 2 + 2 and loses altitude on a failure
     * (TW p.81).
     */
    @Test
    void endingAtVelocityZeroIsPricedAsAStall() {
        AeroSpaceFighter fighter = mock(AeroSpaceFighter.class);
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(4);
        when(fighter.getCrew()).thenReturn(crew);
        when(fighter.isSpheroid()).thenReturn(false);
        when(fighter.isVSTOL()).thenReturn(false);

        MovePath stalled = mock(MovePath.class);
        when(stalled.getEntity()).thenReturn(fighter);
        when(stalled.getMpUsed()).thenReturn(0);
        when(stalled.getFinalVelocity()).thenReturn(0);
        when(stalled.getFinalAltitude()).thenReturn(3);

        MovePath flying = mock(MovePath.class);
        when(flying.getEntity()).thenReturn(fighter);
        when(flying.getMpUsed()).thenReturn(0);
        when(flying.getFinalVelocity()).thenReturn(1);
        when(flying.getFinalAltitude()).thenReturn(3);

        assertTrue(ranker.controlRiskPenalty(stalled) > 0, "a stall low down is a real control risk");
        assertEquals(0.0, ranker.controlRiskPenalty(flying), 0.0001,
              "the same pose with velocity on the clock carries no stall risk");
    }

    /**
     * A stunt at bad odds is gambling, not flying. Live game 6: a piloting-6 pilot opened round 1 with a
     * Split-S it would fail 72% of the time, because the ranker scores the pose a maneuver reaches and a
     * failed roll never reaches it. Below an even chance the offensive set is off the table; a piloting-4
     * veteran (58% on a Split-S) keeps it. Escapes stay exempt - see the cornered test below.
     */
    @Test
    void greenPilotsDoNotGetOffensiveManeuvers() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        MovePath path = maneuverPathAt(new Coords(20, 20), 0, game, board);
        Crew greenCrew = mock(Crew.class);
        when(greenCrew.getPiloting()).thenReturn(6);
        Entity pathFighter = path.getEntity();
        when(pathFighter.getCrew()).thenReturn(greenCrew);
        when(pathFighter.isFighter()).thenReturn(true);

        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 1, path, game,
              AerospaceVenue.GROUND_MAP), "28% odds is a gamble, not a maneuver - even with a committed enemy");

        Crew veteranCrew = mock(Crew.class);
        when(veteranCrew.getPiloting()).thenReturn(4);
        when(pathFighter.getCrew()).thenReturn(veteranCrew);
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 1, path, game,
              AerospaceVenue.GROUND_MAP), "a veteran at 58% keeps the offensive toolbox");
    }

    /** The success chance must match the server's target number, including the fighter's -1. */
    @Test
    void maneuverOddsMatchTheServerTargetNumber() {
        Entity fighter = mock(Entity.class);
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(6);
        when(fighter.getCrew()).thenReturn(crew);
        when(fighter.isFighter()).thenReturn(true);

        // Live game 6: "Needs 9 [6 + 2 - 1 + 2 (Split S maneuver)]" - 2d6 >= 9 is 27.78%.
        assertEquals(10.0 / 36.0, AerospacePathRanker.maneuverSuccessChance(fighter, ManeuverType.MAN_SPLIT_S),
              0.001, "target must be piloting + 2 - 1 + maneuver mod, as the server rolls it");
    }

    /**
     * The escape carve-out: a fighter whose pose has the board edge inside its 8-hex unsteerable straight
     * run gets Hammerhead and Immelmann regardless of who has moved - the alternative is flying off the map.
     */
    @Test
    void escapeManeuversAreSanctionedByGeometryAlone() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        MovePath cornered = maneuverPathAt(new Coords(37, 3), 1, game, board);
        MovePath midBoard = maneuverPathAt(new Coords(20, 20), 0, game, board);

        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 0, cornered, game,
              AerospaceVenue.GROUND_MAP), "a cornered fighter escapes without waiting on anyone");
        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 0, midBoard, game,
              AerospaceVenue.GROUND_MAP), "mid-board there is nothing to escape from");
    }
}
