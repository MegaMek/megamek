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
import megamek.common.moves.MoveStep;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import megamek.common.ToHitData;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.BombMounted;
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
     * Dave's rule, enforced where the game state is current: a reactive maneuver may only be scored
     * against an enemy that has already moved. Path generation cannot hold this gate - it runs inside
     * Precognition before any enemy commits - so this is the check that keeps the rule alive.
     */
    @Test
    void reactiveManeuversRequireACommittedEnemy() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        MovePath path = maneuverPathAt(new Coords(20, 20), 0, game, board);

        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 0, 2, path, game,
              AerospaceVenue.GROUND_MAP), "no committed enemy, no Split-S - it exploits a known position");
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 1, 2, path, game,
              AerospaceVenue.GROUND_MAP), "one committed enemy opens the reactive set");
    }

    /**
     * The first mover's hedge: an Immelmann before anyone commits ends slow, high, and free-facing -
     * banked energy and nothing for the opponent's reply to exploit. Sensible exactly when moving first,
     * with enemy air present and the odds in hand. ("Only Sith deal in absolutes" - Dave, correcting an
     * absolute reading of the committed-enemy rule.)
     */
    @Test
    void energyHedgesAreSanctionedBeforeAnyoneCommits() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        MovePath path = maneuverPathAt(new Coords(20, 20), 0, game, board);

        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_IMMELMAN, 0, 2, path, game,
              AerospaceVenue.GROUND_MAP), "Immelmann is the first mover's hedge");
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_LOOP, 0, 2, path, game,
              AerospaceVenue.GROUND_MAP), "Loop dumps overshoot velocity before committing");
        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_IMMELMAN, 0, 0, path, game,
              AerospaceVenue.GROUND_MAP), "no enemy air, nothing to hedge against");
        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 0, 2, path, game,
              AerospaceVenue.GROUND_MAP), "a Hammerhead on spec parks a stalled fighter for no reason");
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
        when(fighter.getBasePilotingRoll(org.mockito.ArgumentMatchers.any()))
              .thenReturn(new megamek.common.rolls.PilotingRollData(1, 5, "base"));
        when(fighter.isSpheroid()).thenReturn(false);
        when(fighter.isVSTOL()).thenReturn(false);
        ranker.lastPostAttackAltitudeForTest(3);

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

        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 1, 2, path, game,
              AerospaceVenue.GROUND_MAP), "28% odds is a gamble, not a maneuver - even with a committed enemy");

        Crew veteranCrew = mock(Crew.class);
        when(veteranCrew.getPiloting()).thenReturn(4);
        when(pathFighter.getCrew()).thenReturn(veteranCrew);
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_SPLIT_S, 1, 2, path, game,
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
     * When the path carries a real maneuver step, the odds come from the server's own
     * {@code IAero.checkManeuver} - which sees avionics hits and damaged controls the flat formula
     * cannot. A shot-up fighter must be more reluctant to stunt than a fresh one at the same piloting.
     */
    @Test
    void pathOddsPreferTheEnginesOwnControlRollMath() {
        AeroSpaceFighter fighter = mock(AeroSpaceFighter.class);
        MoveStep maneuverStep = mock(MoveStep.class);
        when(maneuverStep.getType()).thenReturn(megamek.common.enums.MoveStepType.MANEUVER);
        MovePath path = mock(MovePath.class);
        when(path.getEntity()).thenReturn(fighter);
        when(path.getStepVector()).thenReturn(new Vector<>(List.of(maneuverStep)));

        // The engine says the roll is an 11 - say, piloting 6 plus two avionics hits the flat
        // formula never sees. 2d6 >= 11 is 3/36.
        when(fighter.checkManeuver(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
              .thenReturn(new megamek.common.rolls.PilotingRollData(1, 11, "avionics-riddled"));

        assertEquals(3.0 / 36.0,
              AerospacePathRanker.maneuverSuccessChance(path, ManeuverType.MAN_SPLIT_S), 0.001,
              "with a maneuver step present, the engine's target number wins over the flat formula");
    }

    /**
     * A failed maneuver is not just an altitude gamble: the server forces half the remaining velocity
     * flown out straight, unsteerable - which carried a live Cheetah clean off the map. That exit is
     * priced at the odds of failing, so a risky stunt at the edge buries itself while the same roll
     * mid-board stays nearly free.
     */
    @Test
    void aFailedManeuversForcedRunOffTheBoardIsPriced() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        // Velocity 2: a failed roll forces max(2/2, 1) * 16 = 16 straight hexes.
        AeroSpaceFighter atEdge = mock(AeroSpaceFighter.class);
        when(atEdge.getPosition()).thenReturn(new Coords(35, 20));
        when(atEdge.getFacing()).thenReturn(2);
        when(atEdge.getCurrentVelocity()).thenReturn(2);
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(4);
        when(atEdge.getCrew()).thenReturn(crew);
        when(atEdge.isFighter()).thenReturn(true);
        when(atEdge.getArmorRemainingPercent()).thenReturn(1.0);

        MovePath path = mock(MovePath.class);
        when(path.getEntity()).thenReturn(atEdge);
        when(path.getFinalBoardId()).thenReturn(0);
        when(game.getBoard(0)).thenReturn(board);
        double edgeCost = ranker.failedManeuverExitCost(path, game, AerospaceVenue.GROUND_MAP);

        // Same fighter mid-board: 16 forced hexes from (20,20) stay on the 40x40 board - no exit cost.
        when(atEdge.getPosition()).thenReturn(new Coords(20, 20));
        when(atEdge.getFacing()).thenReturn(0);
        double midBoardCost = ranker.failedManeuverExitCost(path, game, AerospaceVenue.GROUND_MAP);

        assertTrue(edgeCost > 0, "a forced run off the board must carry a cost, got " + edgeCost);
        assertEquals(0.0, midBoardCost, 0.0001,
              "the same failed roll mid-board exits nothing and costs nothing");
    }

    /**
     * A cornered fighter with nobody committed does not stunt its way out - flying off and returning is
     * an acceptable answer (Dave, 2026-08-13), and the cheap-disengage pricing below makes it available.
     * With a committed enemy, the escape pair reacts at any odds, green pilot or not.
     */
    @Test
    void aCorneredFighterFliesOffRatherThanStunting() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        MovePath cornered = maneuverPathAt(new Coords(37, 3), 1, game, board);
        Crew greenCrew = mock(Crew.class);
        when(greenCrew.getPiloting()).thenReturn(6);

        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 0, 2, cornered, game,
              AerospaceVenue.GROUND_MAP), "even cornered, a Hammerhead on spec is not the answer - fly off");
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 1, 2, cornered, game,
              AerospaceVenue.GROUND_MAP), "with a committed enemy the escape pair flies at any odds");
        when(cornered.getEntity().getCrew()).thenReturn(greenCrew);
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 1, 2, cornered, game,
              AerospaceVenue.GROUND_MAP), "bad odds do not gate the escape pair");
    }

    /**
     * The lesson of live game 8, where both fighters died to control-roll spirals: 28% odds of reaching
     * an attack position is a 72% chance of a wasted turn and a forced straight run - more likely to
     * fail than to attack. Outside a genuine corner, the escape pair now answers to the same 50% floor
     * as everything else; cornered, any odds still beat the certain fly-off.
     */
    @Test
    void midBoardEscapeManeuversObeyTheOddsFloor() {
        Game game = mock(Game.class);
        Board board = groundBoard(40, 40);
        MovePath midBoard = maneuverPathAt(new Coords(20, 20), 0, game, board);
        Crew greenCrew = mock(Crew.class);
        when(greenCrew.getPiloting()).thenReturn(6);
        when(midBoard.getEntity().getCrew()).thenReturn(greenCrew);

        assertFalse(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 1, 2, midBoard, game,
              AerospaceVenue.GROUND_MAP),
              "a 17%-odds Hammerhead mid-board is a gamble, not an escape - committed enemy or not");

        MovePath cornered = maneuverPathAt(new Coords(37, 3), 1, game, board);
        when(cornered.getEntity().getCrew()).thenReturn(greenCrew);
        assertTrue(ranker.maneuverSanctioned(ManeuverType.MAN_HAMMERHEAD, 1, 2, cornered, game,
              AerospaceVenue.GROUND_MAP),
              "cornered, the same bad odds still beat the certain fly-off");
    }

    /**
     * "Should I do this move" is only answerable at THIS airframe's real odds: the control-risk price
     * must rise with the state of the airframe, because the server's roll does. A fighter whose base
     * piloting roll reads 9 (avionics hit, fresh damage) stalls at far worse odds than one rolling
     * against 6, and the old flat-crew-skill pricing charged them identically.
     */
    @Test
    void controlRiskPricesTheAirframesRealRollNotTheCrewSheet() {
        AeroSpaceFighter healthy = mock(AeroSpaceFighter.class);
        when(healthy.getBasePilotingRoll(org.mockito.ArgumentMatchers.any()))
              .thenReturn(new megamek.common.rolls.PilotingRollData(1, 6, "healthy"));
        AeroSpaceFighter shotUp = mock(AeroSpaceFighter.class);
        when(shotUp.getBasePilotingRoll(org.mockito.ArgumentMatchers.any()))
              .thenReturn(new megamek.common.rolls.PilotingRollData(1, 9, "avionics hit, 40 damage"));

        MovePath healthyStall = stalledPathAtAltitudeThree(healthy);
        MovePath shotUpStall = stalledPathAtAltitudeThree(shotUp);

        double healthyPrice = ranker.controlRiskPenalty(healthyStall);
        double shotUpPrice = ranker.controlRiskPenalty(shotUpStall);

        assertTrue(shotUpPrice > healthyPrice * 1.5,
              "the damaged airframe must pay meaningfully more for the same stall: healthy="
                    + healthyPrice + " shotUp=" + shotUpPrice);
    }

    /**
     * Losing control is never free, at any altitude. The old pricing scaled overthrust risk purely by
     * the odds of the first d6 fall reaching the ground - zero above altitude 6 - so a live Cheetah
     * overthrusted at altitude 7 for free, went out of control, and died in the spiral (no steering,
     * a fall every round, recovery on 7+, the stall at the bottom). Entry to that state now carries
     * its own cost.
     */
    @Test
    void losingControlIsNeverFreeEvenUpHigh() {
        AeroSpaceFighter fighter = mock(AeroSpaceFighter.class);
        when(fighter.getBasePilotingRoll(org.mockito.ArgumentMatchers.any()))
              .thenReturn(new megamek.common.rolls.PilotingRollData(1, 6, "base"));
        when(fighter.isSpheroid()).thenReturn(false);
        when(fighter.isVSTOL()).thenReturn(false);

        MovePath overthrustHigh = mock(MovePath.class);
        when(overthrustHigh.getEntity()).thenReturn(fighter);
        when(overthrustHigh.getMpUsed()).thenReturn(99);
        when(overthrustHigh.getFinalVelocity()).thenReturn(2);
        when(overthrustHigh.getFinalAltitude()).thenReturn(8);
        ranker.lastPostAttackAltitudeForTest(8);

        assertTrue(ranker.controlRiskPenalty(overthrustHigh) > 0,
              "overthrusting at altitude 8 must still cost - out of control is the spiral's entrance");
    }

    private MovePath stalledPathAtAltitudeThree(AeroSpaceFighter fighter) {
        when(fighter.isSpheroid()).thenReturn(false);
        when(fighter.isVSTOL()).thenReturn(false);
        MovePath path = mock(MovePath.class);
        when(path.getEntity()).thenReturn(fighter);
        when(path.getMpUsed()).thenReturn(0);
        when(path.getFinalVelocity()).thenReturn(0);
        when(path.getFinalAltitude()).thenReturn(3);
        // controlRiskPenalty reads the post-attack altitude field; mirror what calculateAerospaceMod
        // sets before the risk terms run.
        ranker.lastPostAttackAltitudeForTest(3);
        return path;
    }

    /**
     * The dive-bomb toll is part of the move: a run credited at altitude 3 exits at 1 after the
     * attack's two-level cost, and every crash-odds term must price that exit, not the flown pose.
     */
    @Test
    void aDiveBombRunIsPricedAtItsExitAltitude() {
        Game game = groundGame();
        Coords mekHex = new Coords(20, 20);
        Entity mek = groundMek(mekHex);

        MovePath run = mock(MovePath.class);
        AeroSpaceFighter bomber = strikeFighterOver(Set.of(mekHex), 3, run);
        BombMounted bomb = mock(BombMounted.class);
        BombType bombType = mock(BombType.class);
        when(bombType.getDamagePerShot()).thenReturn(10);
        when(bombType.getBombType()).thenReturn(BombType.BombTypeEnum.HE);
        when(bomb.getType()).thenReturn(bombType);
        when(bomber.getBombs(AmmoType.F_GROUND_BOMB))
              .thenReturn(new ArrayList<>(List.of(bomb)));
        when(bomber.getWeaponList()).thenReturn(new ArrayList<>());

        ranker.lastPostAttackAltitudeForTest(3);
        ranker.scoreAttackRuns(run, game, List.of(mek), AerospaceVenue.GROUND_MAP);

        assertEquals(1, ranker.lastPostAttackAltitudeForTest(),
              "bombing from altitude 3 means exiting at 1 - the risk terms must know");
    }

    /**
     * The pilot exercise, verbatim: a mek lance in a box formation, two hexes of spacing each way.
     * With cluster bombs (5 damage across all seven hexes, NO falloff), bombing a corner mek's hex
     * wastes the footprint - its neighbors are empty - while the seam hex between two meks delivers
     * full damage to both. The best aim point is a search over the flown line, not a lookup of enemy
     * positions, and the model must find the seam on its own.
     */
    @Test
    void aClusterBombAimsAtTheSeamOfABoxFormation() {
        Game game = groundGame();
        // The box: four meks at the corners, 2-hex spacing.
        List<Entity> lance = List.of(
              groundMek(new Coords(20, 20)), groundMek(new Coords(22, 20)),
              groundMek(new Coords(20, 22)), groundMek(new Coords(22, 22)));

        BombMounted cluster = mock(BombMounted.class);
        BombType clusterType =
              mock(BombType.class);
        when(clusterType.getBombType())
              .thenReturn(BombType.BombTypeEnum.CLUSTER);
        when(cluster.getType()).thenReturn(clusterType);

        // Run A: the line crosses only a corner mek's hex - one target in the footprint.
        MovePath cornerRun = mock(MovePath.class);
        AeroSpaceFighter bomber = strikeFighterOver(
              Set.of(new Coords(20, 19), new Coords(20, 20)), 5, cornerRun);
        when(bomber.getBombs(AmmoType.F_GROUND_BOMB))
              .thenReturn(new ArrayList<>(List.of(cluster)));
        when(bomber.getWeaponList()).thenReturn(new ArrayList<>());
        ranker.lastPostAttackAltitudeForTest(5);
        ranker.scoreAttackRuns(cornerRun, game, lance, AerospaceVenue.GROUND_MAP);
        double cornerCredit = ranker.lastAttackRunCreditForTest();

        // Run B: the line flies the box lengthwise through the seam hex (21,20) - adjacent to both
        // front meks. Same bomb, same bomber, better geometry.
        ranker.resetGroundCountersForTest();
        MovePath seamRun = mock(MovePath.class);
        strikeFighterOver(Set.of(new Coords(21, 19), new Coords(21, 20)), 5, seamRun);
        when(seamRun.getEntity()).thenReturn(bomber);
        ranker.lastPostAttackAltitudeForTest(5);
        ranker.scoreAttackRuns(seamRun, game, lance, AerospaceVenue.GROUND_MAP);
        double seamCredit = ranker.lastAttackRunCreditForTest();

        assertTrue(seamCredit >= cornerCredit * 1.9,
              "the seam splashes two meks where the corner reaches one: corner=" + cornerCredit
                    + " seam=" + seamCredit);
    }

    /**
     * The exposure hazard is the fire actually pointed at you. A flat constant let a 10-bomb attack
     * credit outbid it every turn; the live Chippewa died pressing low passes into an LB 20-X. Under
     * flak-scale incoming fire, low altitude must price several times dearer than under none - and an
     * empty sky prices at zero.
     */
    @Test
    void exposureScalesWithIncomingFire() {
        double emptySky = AerospacePathRanker.exposurePenalty(0, 2);
        double lightFire = AerospacePathRanker.exposurePenalty(10, 2);
        double flakBattery = AerospacePathRanker.exposurePenalty(35, 2);

        assertEquals(0.0, emptySky, 0.0001, "no incoming fire, no exposure hazard");
        assertTrue(flakBattery > lightFire * 2,
              "a flak battery must price low flight far above light fire: light=" + lightFire
                    + " flak=" + flakBattery);
        assertTrue(AerospacePathRanker.exposurePenalty(35, 7) < flakBattery / 2,
              "the same fire prices far cheaper up high");
    }

    /**
     * The mastery credit: the more elite the pilot, the more a maneuver is worth beyond its pose
     * (Dave). Zero at the 50% sanction floor - a marginal pilot gets no style points - and full value
     * for an ace, so skill becomes visible in flying style.
     */
    @Test
    void masteryCreditScalesWithSkillAboveTheFloor() {
        double atFloor = AerospacePathRanker.MANEUVER_MASTERY_CREDIT
              * Math.max(0, 0.50 - AerospacePathRanker.MINIMUM_STUNT_SUCCESS_CHANCE) / 0.5;
        double veteran = AerospacePathRanker.MANEUVER_MASTERY_CREDIT
              * Math.max(0, 0.58 - AerospacePathRanker.MINIMUM_STUNT_SUCCESS_CHANCE) / 0.5;
        double elite = AerospacePathRanker.MANEUVER_MASTERY_CREDIT
              * Math.max(0, 0.83 - AerospacePathRanker.MINIMUM_STUNT_SUCCESS_CHANCE) / 0.5;

        assertEquals(0.0, atFloor, 0.0001, "no style points at the sanction floor");
        assertTrue(elite > veteran * 3,
              "an ace's mastery must dwarf a veteran's: veteran=" + veteran + " elite=" + elite);
    }

    // --- the attack run ------------------------------------------------------------------------------

    private static Entity groundMek(Coords position) {
        Entity mek = mock(Entity.class);
        when(mek.isAirborne()).thenReturn(false);
        when(mek.getPosition()).thenReturn(position);
        when(mek.getBoardId()).thenReturn(0);
        when(mek.isOffBoard()).thenReturn(false);
        return mek;
    }

    private AeroSpaceFighter strikeFighterOver(Set<Coords> flownHexes, int finalAltitude,
          MovePath path) {
        AeroSpaceFighter fighter = mock(AeroSpaceFighter.class);
        when(fighter.getBoardId()).thenReturn(0);
        when(fighter.getBombs(AmmoType.F_GROUND_BOMB))
              .thenReturn(new ArrayList<>());
        when(path.getEntity()).thenReturn(fighter);
        when(path.getFinalAltitude()).thenReturn(finalAltitude);
        when(path.getCoordsSet()).thenReturn(flownHexes);
        return fighter;
    }

    /**
     * The whole air-to-ground mechanic in one credit: every A2G attack requires the target's hex on THIS
     * turn's flown line (passedOver), ground units have already moved when the fighter plans, and the
     * line resets every round. A path over the mek inside the strike window earns the credit; the same
     * path four altitudes higher earns nothing, because no attack is legal from up there. Measured
     * without this term: one bombing pass every fourteen rounds.
     */
    private Game groundGame() {
        Game game = mock(Game.class);
        when(game.getOptions()).thenReturn(new megamek.common.options.GameOptions());
        // The spheroid pricing guard asks the engine whether the mover flies as a spheroid, which
        // reads the game's planetary conditions; standard atmosphere keeps aerodynes aerodyne.
        when(game.getPlanetaryConditions())
              .thenReturn(new megamek.common.planetaryConditions.PlanetaryConditions());
        when(game.onTheSameBoard(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
              .thenReturn(true);
        // isIgnorableEnemy walks board containment and honor state; give both real answers.
        when(game.getBoard(org.mockito.ArgumentMatchers.any(Entity.class))).thenReturn(groundBoard(40, 40));
        IHonorUtil honorUtil = mock(IHonorUtil.class);
        when(ranker.getOwner().getHonorUtil()).thenReturn(honorUtil);
        return game;
    }

    @Test
    void overflyingACommittedGroundTargetInsideTheWindowEarnsTheRun() {
        Game game = groundGame();
        Coords mekHex = new Coords(20, 20);
        Entity mek = groundMek(mekHex);

        MovePath overflight = mock(MovePath.class);
        AeroSpaceFighter fighter = strikeFighterOver(Set.of(new Coords(19, 20), mekHex), 4,
              overflight);
        // Guns that can deliver at close range: use the ranker's own damage table via a real value.
        when(fighter.getWeaponList()).thenReturn(new ArrayList<>());

        ranker.scoreAttackRuns(overflight, game, List.of(mek), AerospaceVenue.GROUND_MAP);
        int overflownInWindow = ranker.lastOverflownTargets;

        ranker.lastGroundTargets = 0;
        ranker.lastOverflownTargets = 0;
        MovePath tooHigh = mock(MovePath.class);
        strikeFighterOver(Set.of(new Coords(19, 20), mekHex), 9, tooHigh);
        ranker.scoreAttackRuns(tooHigh, game, List.of(mek), AerospaceVenue.GROUND_MAP);
        int overflownTooHigh = ranker.lastOverflownTargets;

        assertEquals(1, overflownInWindow, "over the mek at altitude 4, the run is on");
        assertEquals(0, overflownTooHigh, "altitude 9 can attack nothing - no credit however good the line");
    }

    /**
     * Ground targets discipline velocity just as enemy air does: a run must thread the target's exact
     * hex, and at velocity 3 a facing change comes only every 16 hexes. Without this gate two unopposed
     * fighters circled for fourteen rounds between passes.
     */
    @Test
    void groundTargetsAloneActivateVelocityDiscipline() {
        Game game = groundGame();
        Coords mekHex = new Coords(20, 20);
        Entity mek = groundMek(mekHex);

        MovePath path = mock(MovePath.class);
        AeroSpaceFighter fighter = strikeFighterOver(Set.of(new Coords(5, 5)), 5, path);
        when(fighter.getWeaponList()).thenReturn(new ArrayList<>());
        when(path.getFinalVelocity()).thenReturn(3);
        when(path.getGame()).thenReturn(game);

        ranker.scoreAttackRuns(path, game, List.of(mek), AerospaceVenue.GROUND_MAP);

        assertTrue(ranker.lastGroundTargets > 0, "the mek must register as a ground target");
        // The penalty itself scales with the fighter's close-range damage, which is zero for this
        // weaponless mock - what matters is that the gate is OPEN with no enemy air on the board.
        assertEquals(0.0, ranker.velocityPenalty(path, AerospaceVenue.GROUND_MAP), 0.0001,
              "zero damage prices a zero penalty, but the gate no longer requires enemy air");
    }

    /**
     * Flying off is a disengage, not a defeat (Dave, 2026-08-13): the fighter returns some rounds later
     * and is untargetable in the meantime. A healthy fighter still pays the full off-board cost; a mauled
     * one leaves cheap, because staying is how it dies.
     */
    @Test
    void aMauledFighterDisengagesOffBoardCheaply() {
        Game game = mock(Game.class);
        Entity healthy = mock(Entity.class);
        when(healthy.isCrippled()).thenReturn(false);
        when(healthy.getArmorRemainingPercent()).thenReturn(0.9);
        Entity crippled = mock(Entity.class);
        when(crippled.isCrippled()).thenReturn(true);

        MovePath healthyOff = mock(MovePath.class);
        when(healthyOff.fliesOffBoard()).thenReturn(true);
        when(healthyOff.getEntity()).thenReturn(healthy);
        MovePath crippledOff = mock(MovePath.class);
        when(crippledOff.fliesOffBoard()).thenReturn(true);
        when(crippledOff.getEntity()).thenReturn(crippled);

        double healthyCost = ranker.edgePenalty(healthyOff, game, AerospaceVenue.GROUND_MAP);
        double crippledCost = ranker.edgePenalty(crippledOff, game, AerospaceVenue.GROUND_MAP);

        assertTrue(crippledCost < healthyCost / 2,
              "a crippled fighter's exit must cost a fraction of a healthy one's");
        assertTrue(healthyCost >= crippledCost * 4,
              "a healthy fighter still pays full price for wandering off mid-fight");
    }

    /**
     * The disengage rule's time-to-decision test, calibrated on the 150-round stall of 2026-08-14:
     * a bombless Hellcat II (paper maximum ~30, honest delivery ~7.5 a round) against two meks at
     * ~400 combined hit points is a 53-round siege - leave. The same fighter against one crippled
     * straggler at 40 hit points finishes in a handful of rounds - stay. No working guns at all is
     * always done.
     */
    @Test
    void aWinchesterFighterLeavesASiegeButStaysToFinishAStraggler() {
        double hellcatPerRound = 30.0 * AerospacePathRanker.GUN_PASS_DELIVERY_FRACTION;

        assertTrue(AerospacePathRanker.cannotForceADecision(hellcatPerRound, 400),
              "two healthy meks are a 53-round siege - past the horizon, disengage");
        assertFalse(AerospacePathRanker.cannotForceADecision(hellcatPerRound, 40),
              "one crippled straggler dies in rounds - stay and finish it");
        assertTrue(AerospacePathRanker.cannotForceADecision(0.0, 40),
              "no working guns is always combat ineffective");
    }

    /**
     * Winchester flips the fly-off ledger: instead of paying the off-board cost (scaled by health),
     * leaving is CREDITED - the winning move for a healthy fighter that cannot change the outcome.
     * Forced Withdrawal never fires here because it is damage-triggered and the airframe is intact.
     */
    @Test
    void winchesterMakesTheFlyOffTheWinningMove() {
        Game game = mock(Game.class);
        Entity healthy = mock(Entity.class);
        when(healthy.isCrippled()).thenReturn(false);
        when(healthy.getArmorRemainingPercent()).thenReturn(0.9);
        MovePath flyOff = mock(MovePath.class);
        when(flyOff.fliesOffBoard()).thenReturn(true);
        when(flyOff.getEntity()).thenReturn(healthy);

        ranker.lastWinchesterForTest(0);
        double stillFighting = ranker.edgePenalty(flyOff, game, AerospaceVenue.GROUND_MAP);
        ranker.lastWinchesterForTest(1);
        double winchester = ranker.edgePenalty(flyOff, game, AerospaceVenue.GROUND_MAP);

        assertTrue(stillFighting > 0, "a healthy fighter with work left pays to leave");
        assertTrue(winchester < 0, "a Winchester fighter is credited for leaving");
        assertEquals(-AerospacePathRanker.WINCHESTER_DISENGAGE_CREDIT, winchester, 0.001,
              "the credit is the constant, not a health-scaled cost");
    }

    /**
     * The air-cover detection primitive (Dave): an aircraft's ground-attack threat on any given
     * turn is bombs PLUS guns. Bombs count in full - a dive-bomb attack can release the whole rack
     * in one pass - and guns count because an empty-racked fighter with heavy strike guns is still
     * a threat every turn. The future intercept credit and the bot-commands focus modes both key
     * on this number.
     */
    @Test
    void groundAttackThreatIsBombsPlusGunsPerTurn() {
        Game game = mock(Game.class);
        AerospacePathRanker spyRanker = Mockito.spy(ranker);
        // The range-bracket helpers read game options the mock does not carry; both are incidental
        // to what this test pins (the bombs + guns sum), so stub them alongside the gun estimate.
        Mockito.doReturn(false).when(spyRanker).isExtremeRange(game);
        Mockito.doReturn(false).when(spyRanker).isLosRange(game);
        Mockito.doReturn(12.0).when(spyRanker)
              .getMaxDamageAtRange(Mockito.any(Entity.class),
                    Mockito.anyInt(),
                    Mockito.anyBoolean(), Mockito.anyBoolean());

        BombType heType =
              mock(BombType.class);
        when(heType.getDamagePerShot()).thenReturn(10);
        BombMounted bombOne =
              mock(BombMounted.class);
        when(bombOne.getType()).thenReturn(heType);
        BombMounted bombTwo =
              mock(BombMounted.class);
        when(bombTwo.getType()).thenReturn(heType);

        Entity ladenBomber = mock(Entity.class);
        when(ladenBomber.getBombs(AmmoType.F_GROUND_BOMB))
              .thenReturn(new ArrayList<>(List.of(bombOne, bombTwo)));
        Entity emptyRacks = mock(Entity.class);
        when(emptyRacks.getBombs(AmmoType.F_GROUND_BOMB))
              .thenReturn(new ArrayList<>());

        assertEquals(32.0, spyRanker.groundAttackThreatPerTurn(ladenBomber, game), 0.001,
              "two ten-point bombs plus twelve points of guns is a 32-point threat");
        assertEquals(12.0, spyRanker.groundAttackThreatPerTurn(emptyRacks, game), 0.001,
              "an empty-racked fighter is still its guns' worth of threat, not zero");
    }

    /**
     * The air-cover credit (Dave): a laden enemy bomber is worth intercepting in proportion to the
     * ground-attack damage it carries - but only while a friendly ground force exists to protect,
     * discounted by the same certainty the engagement credit uses, and raised under a DEFEND
     * posture (the posture's first consumer). No ground force below means no cover mission: zero.
     */
    @Test
    void interceptCreditScalesWithThreatAndVanishesWithNothingToProtect() {
        double ladenBomber = AerospacePathRanker.interceptCredit(200.0, 1.0, true, false);
        double emptyRacks = AerospacePathRanker.interceptCredit(30.0, 1.0, true, false);

        assertEquals(200.0 * AerospacePathRanker.INTERCEPT_WEIGHT, ladenBomber, 0.001,
              "a 200-point bomber outbids everything an empty fighter offers");
        assertTrue(ladenBomber > emptyRacks * 4,
              "the laden bomber must be a categorically better intercept than the empty one");
        assertEquals(0.0, AerospacePathRanker.interceptCredit(200.0, 1.0, false, false), 0.001,
              "no friendly ground force below means no cover mission");
        assertEquals(ladenBomber * AerospacePathRanker.DEFENSIVE_INTERCEPT_MULTIPLIER,
              AerospacePathRanker.interceptCredit(200.0, 1.0, true, true), 0.001,
              "a DEFEND posture leans harder into cover work");
        assertEquals(ladenBomber * 0.5,
              AerospacePathRanker.interceptCredit(200.0, 0.5, true, false), 0.001,
              "an unmoved enemy's threat carries the same certainty discount as its engagement");
    }

    /**
     * The focus order's arithmetic (Dave: "Focus on Aerospace, Focus on Ground" as bot commands).
     * The favored credit set doubles, the other quarters - a bias, never a gate - and AUTO is the
     * exact identity, so a bot that has never been given an order flies byte-identical doctrine.
     * The measured basis for order-not-default: the tuning run where a standing air priority won
     * the air war and lost the battle 0.71:1.
     */
    @Test
    void aFocusOrderBiasesTheCreditSetsAndAutoChangesNothing() {
        assertEquals(1.0, AerospacePathRanker.focusMultiplier(AerospaceFocus.AUTO, true), 0.0,
              "AUTO must be the exact identity on the air set");
        assertEquals(1.0, AerospacePathRanker.focusMultiplier(AerospaceFocus.AUTO, false), 0.0,
              "AUTO must be the exact identity on the ground set");
        assertEquals(AerospacePathRanker.FOCUS_FAVORED_MULTIPLIER,
              AerospacePathRanker.focusMultiplier(AerospaceFocus.AEROSPACE, true), 0.001,
              "Focus Aerospace doubles the air credit set");
        assertEquals(AerospacePathRanker.FOCUS_SUPPRESSED_MULTIPLIER,
              AerospacePathRanker.focusMultiplier(AerospaceFocus.AEROSPACE, false), 0.001,
              "Focus Aerospace quarters the ground set - suppressed, never zeroed");
        assertEquals(AerospacePathRanker.FOCUS_FAVORED_MULTIPLIER,
              AerospacePathRanker.focusMultiplier(AerospaceFocus.GROUND, false), 0.001,
              "Focus Ground doubles the ground credit set");
        assertEquals(AerospacePathRanker.FOCUS_SUPPRESSED_MULTIPLIER,
              AerospacePathRanker.focusMultiplier(AerospaceFocus.GROUND, true), 0.001,
              "Focus Ground quarters the air set");
        assertTrue(AerospacePathRanker.FOCUS_SUPPRESSED_MULTIPLIER > 0,
              "orders bias, they never blind - the suppressed set must stay positive");
    }

    /**
     * The roll-in direction chooses the armor facing (Dave: "could be as simple as rolling in of a
     * right turn vs a left turn"). The engine resolves every air-to-ground attack on the side
     * table given by the hex the fighter entered the target's hex from, so the astern approach
     * prices half again higher than the head-on one - same target, same line length, different
     * entry direction.
     */
    @Test
    void anAsternRollInOutbidsAHeadOnPassOverTheSameTarget() {
        assertEquals(AerospacePathRanker.REAR_APPROACH_MULTIPLIER,
              AerospacePathRanker.approachMultiplier(ToHitData.SIDE_REAR), 0.001);
        assertEquals(AerospacePathRanker.REAR_APPROACH_MULTIPLIER,
              AerospacePathRanker.approachMultiplier(ToHitData.SIDE_REAR_LEFT), 0.001,
              "the rear quarter arcs price as rear");
        assertEquals(AerospacePathRanker.SIDE_APPROACH_MULTIPLIER,
              AerospacePathRanker.approachMultiplier(ToHitData.SIDE_LEFT), 0.001);
        assertEquals(1.0,
              AerospacePathRanker.approachMultiplier(ToHitData.SIDE_FRONT), 0.001,
              "a head-on pass earns no premium");

        // And through the full attack-run scoring: identical gun runs over the same mek, one
        // entering its hex from astern, one from ahead - the astern run's credit is 1.5x.
        Game game = groundGame();
        Coords mekHex = new Coords(20, 20);
        Coords fromBehind = new Coords(20, 21);
        Coords fromAhead = new Coords(20, 19);
        Entity mek = groundMek(mekHex);
        when(mek.sideTable(fromBehind)).thenReturn(ToHitData.SIDE_REAR);
        when(mek.sideTable(fromAhead)).thenReturn(ToHitData.SIDE_FRONT);

        double asternCredit = gunRunCredit(game, mek, fromBehind, mekHex);
        double headOnCredit = gunRunCredit(game, mek, fromAhead, mekHex);

        assertEquals(AerospacePathRanker.REAR_APPROACH_MULTIPLIER,
              asternCredit / headOnCredit, 0.001,
              "same mek, same line, astern entry must price 1.5x the head-on entry");
    }

    private double gunRunCredit(Game game, Entity mek, Coords entryHex, Coords mekHex) {
        MovePath run = mock(MovePath.class);
        AeroSpaceFighter fighter = strikeFighterOver(
              new LinkedHashSet<>(List.of(entryHex, mekHex)), 3, run);
        when(fighter.getWeaponList()).thenReturn(new ArrayList<>());
        Vector<MoveStep> steps = new Vector<>();
        for (Coords position : List.of(entryHex, mekHex)) {
            MoveStep step = mock(MoveStep.class);
            when(step.getPosition()).thenReturn(position);
            steps.add(step);
        }
        when(run.getStepVector()).thenReturn(steps);

        AerospacePathRanker spyRanker = Mockito.spy(ranker);
        Mockito.doReturn(false).when(spyRanker).isExtremeRange(game);
        Mockito.doReturn(false).when(spyRanker).isLosRange(game);
        Mockito.doReturn(10.0).when(spyRanker)
              .getMaxDamageAtRange(Mockito.any(Entity.class),
                    Mockito.anyInt(),
                    Mockito.anyBoolean(), Mockito.anyBoolean());
        spyRanker.resetGroundCountersForTest();
        spyRanker.lastPostAttackAltitudeForTest(3);
        spyRanker.scoreAttackRuns(run, game, List.of(mek), AerospaceVenue.GROUND_MAP);
        return spyRanker.lastAttackRunCreditForTest();
    }

    /**
     * The legal shapes of a strafing run (TW p.243): straight windows of at most five consecutive
     * hexes on the flown line. A bend in the line ends the window; a five-hex straight run yields
     * every sub-window but nothing longer.
     */
    @Test
    void strafingWindowsAreStraightConsecutiveAndAtMostFive() {
        // A straight six-hex column: windows of length 1-5 exist, no window of 6.
        List<Coords> straight = new ArrayList<>();
        for (int y = 10; y < 16; y++) {
            straight.add(new Coords(20, y));
        }
        List<List<Coords>> windows = AerospacePathRanker.straightWindows(straight, 5);
        int longest = 0;
        for (List<Coords> window : windows) {
            longest = Math.max(longest, window.size());
        }
        assertEquals(5, longest, "a six-hex straight line caps at the five-hex window");

        // A dogleg: 3 hexes south then a bend. No straight window crosses the bend.
        List<Coords> dogleg = List.of(new Coords(20, 10), new Coords(20, 11),
              new Coords(20, 12), new Coords(21, 12));
        for (List<Coords> window : AerospacePathRanker.straightWindows(dogleg, 5)) {
            assertTrue(window.size() <= 3
                        || !(window.contains(new Coords(20, 10)) && window.contains(new Coords(21, 12))),
                  "no straight window may span the bend");
        }
    }

    /**
     * The sliding-sum window scorer the movement ranker uses must agree with the full window
     * enumeration it replaced, on every shape a flown line takes: straight runs longer than the
     * window, bends, and gaps where the line jumps.
     */
    @Test
    void slidingWindowScorerAgreesWithWindowEnumeration() {
        Map<Coords, Double> valueByHex = new HashMap<>();
        valueByHex.put(new Coords(20, 10), 1.5);
        valueByHex.put(new Coords(20, 13), 1.0);
        valueByHex.put(new Coords(20, 15), 1.15);
        valueByHex.put(new Coords(21, 12), 1.0);

        List<Coords> straight = new ArrayList<>();
        for (int y = 9; y < 17; y++) {
            straight.add(new Coords(20, y));
        }
        List<Coords> dogleg = List.of(new Coords(20, 10), new Coords(20, 11),
              new Coords(20, 12), new Coords(21, 12), new Coords(22, 13));
        List<Coords> gapped = List.of(new Coords(20, 10), new Coords(20, 11),
              new Coords(20, 14), new Coords(20, 15));

        for (List<Coords> line : List.of(straight, dogleg, gapped)) {
            double enumerated = 0;
            for (List<Coords> window : AerospacePathRanker.straightWindows(line, 5)) {
                double windowValue = 0;
                for (Map.Entry<Coords, Double> hexValue : valueByHex.entrySet()) {
                    if (window.contains(hexValue.getKey())) {
                        windowValue += hexValue.getValue();
                    }
                }
                enumerated = Math.max(enumerated, windowValue);
            }
            assertEquals(enumerated,
                  AerospacePathRanker.bestStraightWindowValue(line, valueByHex, 5), 0.0001,
                  "sliding sum must match the enumeration on " + line);
        }
    }

    /**
     * The strafe is the third bidder in the attack-run auction, and against the worked column -
     * three meks with a hex between each, spanning exactly a five-hex window - it must outbid the
     * single-target strike: three victims at the 0.55 odds haircut is 1.65x the rifle. All astern
     * entries multiply further, but even head-on the count carries it.
     */
    @Test
    void aStrafeOverTheColumnOutbidsAStrikeOnOneMek() {
        Game game = groundGame();
        // Wasp - gap - Dervish - gap - BattleMaster, walking a column at x=20.
        List<Coords> line = new ArrayList<>();
        for (int y = 9; y < 16; y++) {
            line.add(new Coords(20, y));
        }
        List<Entity> column = new ArrayList<>();
        for (int y : new int[] { 10, 12, 14 }) {
            Entity mek = groundMek(new Coords(20, y));
            when(mek.sideTable(Mockito.any(Coords.class)))
                  .thenReturn(ToHitData.SIDE_FRONT);
            column.add(mek);
        }
        MovePath run = mock(MovePath.class);
        AeroSpaceFighter fighter = strikeFighterOver(new LinkedHashSet<>(line), 3, run);
        Vector<MoveStep> steps = new Vector<>();
        for (Coords position : line) {
            MoveStep step = mock(MoveStep.class);
            when(step.getPosition()).thenReturn(position);
            steps.add(step);
        }
        when(run.getStepVector()).thenReturn(steps);
        // A 10-damage strafe-eligible laser battery: the same guns price the strike.
        WeaponType laserType = mock(WeaponType.class);
        when(laserType.hasFlag(WeaponType.F_DIRECT_FIRE)).thenReturn(true);
        when(laserType.hasFlag(WeaponType.F_LASER)).thenReturn(true);
        when(laserType.getDamage()).thenReturn(10);
        WeaponMounted laser = mock(WeaponMounted.class);
        when(laser.canFire()).thenReturn(true);
        when(laser.isRearMounted()).thenReturn(false);
        when(laser.getLocation()).thenReturn(1);
        when(laser.getType()).thenReturn(laserType);
        when(fighter.getWeaponList()).thenReturn(new ArrayList<>(List.of(laser)));

        AerospacePathRanker spyRanker = Mockito.spy(ranker);
        Mockito.doReturn(false).when(spyRanker).isExtremeRange(game);
        Mockito.doReturn(false).when(spyRanker).isLosRange(game);
        Mockito.doReturn(10.0).when(spyRanker)
              .getMaxDamageAtRange(Mockito.any(Entity.class),
                    Mockito.anyInt(),
                    Mockito.anyBoolean(), Mockito.anyBoolean());
        spyRanker.resetGroundCountersForTest();
        spyRanker.lastPostAttackAltitudeForTest(3);
        spyRanker.scoreAttackRuns(run, game, column, AerospaceVenue.GROUND_MAP);

        double expectedStrafe = 10.0 * 3 * AerospacePathRanker.STRAFE_ODDS_FACTOR
              * AerospacePathRanker.ATTACK_RUN_WEIGHT;
        assertEquals(expectedStrafe, spyRanker.lastAttackRunCreditForTest(), 0.001,
              "three head-on victims at the odds haircut must beat the 10-damage strike rifle");
    }

    /**
     * The pilot-orbiting regression of 2026-08-15, pinned: ejected crews are not targets. The
     * firing half vetoes shooting them (EJECTED_PILOT_DISUTILITY), so the movement half must not
     * price them either - a fighter spent 24 rounds flying "attack runs" over crash-site pilots it
     * would never shoot while two live enemy fighters flew unchallenged.
     */
    @Test
    void anEjectedCrewEarnsNoAttackRunCredit() {
        Game game = groundGame();
        Coords crewHex = new Coords(20, 20);
        Entity crew = mock(megamek.common.units.EjectedCrew.class);
        when(crew.getPosition()).thenReturn(crewHex);
        when(crew.getBoardId()).thenReturn(0);
        when(crew.isAirborne()).thenReturn(false);

        MovePath run = mock(MovePath.class);
        AeroSpaceFighter fighter = strikeFighterOver(Set.of(crewHex), 3, run);
        when(fighter.getWeaponList()).thenReturn(new ArrayList<>());

        AerospacePathRanker spyRanker = Mockito.spy(ranker);
        Mockito.doReturn(false).when(spyRanker).isExtremeRange(game);
        Mockito.doReturn(false).when(spyRanker).isLosRange(game);
        Mockito.doReturn(10.0).when(spyRanker)
              .getMaxDamageAtRange(Mockito.any(Entity.class),
                    Mockito.anyInt(),
                    Mockito.anyBoolean(), Mockito.anyBoolean());
        spyRanker.resetGroundCountersForTest();
        spyRanker.lastPostAttackAltitudeForTest(3);
        spyRanker.scoreAttackRuns(run, game, List.of(crew), AerospaceVenue.GROUND_MAP);

        assertEquals(0.0, spyRanker.lastAttackRunCreditForTest(), 0.001,
              "a crash-site pilot the guns refuse to shoot must earn the movement side nothing");
    }

    /**
     * The mirror image of the crew exclusion: a CRASHED enemy fighter is a prime ground target -
     * immobile, full battle value on the kill, helpless - and must stay in the attack-run target
     * set. Pinned so the crew fix is never "generalized" into excluding grounded aerospace units
     * (the 2026-08-15 game where a crashed Hellcat sat untouched for 33 rounds is the case this
     * exists to end).
     */
    @Test
    void aCrashedEnemyFighterIsAnAttackRunTarget() {
        Game game = groundGame();
        Coords crashSite = new Coords(20, 20);
        Entity crashed = mock(AeroSpaceFighter.class);
        when(crashed.getPosition()).thenReturn(crashSite);
        when(crashed.getBoardId()).thenReturn(0);
        when(crashed.isAirborne()).thenReturn(false);

        MovePath run = mock(MovePath.class);
        AeroSpaceFighter fighter = strikeFighterOver(Set.of(crashSite), 3, run);
        when(fighter.getWeaponList()).thenReturn(new ArrayList<>());

        AerospacePathRanker spyRanker = Mockito.spy(ranker);
        Mockito.doReturn(false).when(spyRanker).isExtremeRange(game);
        Mockito.doReturn(false).when(spyRanker).isLosRange(game);
        Mockito.doReturn(10.0).when(spyRanker)
              .getMaxDamageAtRange(Mockito.any(Entity.class),
                    Mockito.anyInt(),
                    Mockito.anyBoolean(), Mockito.anyBoolean());
        spyRanker.resetGroundCountersForTest();
        spyRanker.lastPostAttackAltitudeForTest(3);
        spyRanker.scoreAttackRuns(run, game, List.of(crashed), AerospaceVenue.GROUND_MAP);

        assertTrue(spyRanker.lastAttackRunCreditForTest() > 0,
              "a helpless crashed fighter under the flown line must earn the attack-run credit");
    }

    /** TW p.243's weapon clause, mirrored: energy yes, ammo no. */
    @Test
    void onlyDirectFireEnergyWeaponsAreStrafeEligible() {
        WeaponType laser = mock(WeaponType.class);
        when(laser.hasFlag(WeaponType.F_DIRECT_FIRE)).thenReturn(true);
        when(laser.hasFlag(WeaponType.F_LASER)).thenReturn(true);
        assertTrue(AerospacePathRanker.isStrafeEligible(laser), "a direct-fire laser strafes");

        WeaponType autocannon = mock(WeaponType.class);
        when(autocannon.hasFlag(WeaponType.F_DIRECT_FIRE)).thenReturn(true);
        assertFalse(AerospacePathRanker.isStrafeEligible(autocannon),
              "an autocannon needs ammo and may not strafe");

        WeaponType flamer = mock(WeaponType.class);
        when(flamer.hasFlag(WeaponType.F_FLAMER)).thenReturn(true);
        assertTrue(AerospacePathRanker.isStrafeEligible(flamer), "flamers are in per TW p.243");
    }

    /**
     * The heat map of the opposition's movement, distilled: the stern-alignment arithmetic that
     * turns per-unit drift into "am I behind them?". Dead astern earns full credit, one hexside
     * off half, anything else nothing - including across the 5-to-0 direction wraparound.
     */
    @Test
    void sternAlignmentIsFullBehindHalfOffAxisAndZeroElsewhere() {
        assertEquals(1.0, AerospacePathRanker.sternAlignment(2, 2), 0.001, "dead astern");
        assertEquals(0.5, AerospacePathRanker.sternAlignment(3, 2), 0.001, "one hexside off");
        assertEquals(0.5, AerospacePathRanker.sternAlignment(5, 0), 0.001,
              "the 5-to-0 wraparound is one hexside, not five");
        assertEquals(0.0, AerospacePathRanker.sternAlignment(0, 3), 0.001,
              "dead ahead of the force earns nothing");
        assertEquals(0.0, AerospacePathRanker.sternAlignment(4, 2), 0.001, "two off is broadside");
    }
}
