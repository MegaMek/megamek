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
package megamek.common.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.Test;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponMounted;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.mockito.Mockito;

/**
 * Covers the altitude choices the doctrine finder offers, which the stock finder does not offer at all - it
 * drives every path to {@code OPTIMAL_STRIKE_ALTITUDE}.
 */
class AeroGroundDoctrinePathFinderTest {

    private static final int BOARD_WIDTH = 40;
    private static final int BOARD_HEIGHT = 40;

    private Game game;
    private AeroGroundDoctrinePathFinder finder;
    private Entity mover;

    @BeforeEach
    void beforeEach() {
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        board.setBoardType(BoardType.GROUND);

        game = new Game();
        game.setBoard(board);

        Player us = new Player(1, "Us");
        us.setTeam(1);
        Player them = new Player(2, "Them");
        them.setTeam(2);
        game.addPlayer(1, us);
        game.addPlayer(2, them);

        mover = fighter(10, new Coords(5, 5), 5, us);
        finder = AeroGroundDoctrinePathFinder.getInstance(game);
    }

    private Entity fighter(int id, Coords position, int altitude, Player owner) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setId(id);
        fighter.setGame(game);
        fighter.setOwner(owner);
        fighter.setPosition(position);
        fighter.setAltitude(altitude);
        fighter.setDeployed(true);
        game.addEntity(fighter);
        return fighter;
    }

    private Entity enemyFighter(int id, Coords position, int altitude, boolean hasMoved) {
        Entity enemy = fighter(id, position, altitude, game.getPlayer(2));
        enemy.setDone(hasMoved);
        return enemy;
    }

    @Test
    void withNoEnemiesTheStrikeAltitudeIsStillOffered() {
        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertTrue(candidates.contains(AeroGroundPathFinder.OPTIMAL_STRIKE_ALTITUDE),
              "the stock choice must always remain available");
    }

    /**
     * The point of the whole change: an opponent that has committed to an altitude gives the bot something
     * to match. The stock finder cannot express this, because it only ever generates one altitude.
     */
    @Test
    void anEnemyThatHasMovedContributesItsAltitude() {
        enemyFighter(20, new Coords(20, 20), 3, true);

        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertTrue(candidates.contains(3), "should offer to meet the committed enemy at altitude 3");
        assertEquals(3, candidates.getFirst(), "and should weigh it ahead of the default");
    }

    @Test
    void anEnemyThatHasNotMovedContributesNothing() {
        enemyFighter(20, new Coords(20, 20), 3, false);

        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertFalse(candidates.contains(3),
              "an enemy still to move may be anywhere in its band; matching it now is a guess, not a read");
    }

    /**
     * The strafe-window candidate (the first strafe hunt's first wall): a fighter with
     * strafe-eligible energy guns is offered altitude 3 - inside both the strafe and dive-bomb
     * windows - because the ranker's strafe bid can only buy an altitude generation offers. A
     * gunless airframe is not (the bare fixture mover already proves the negative in the
     * unmoved-enemy test above).
     */
    @Test
    void aFighterWithEnergyGunsIsOfferedTheStrafeWindow() {
        WeaponType laserType =
              mock(WeaponType.class);
        when(laserType.hasFlag(WeaponType.F_DIRECT_FIRE))
              .thenReturn(true);
        when(laserType.hasFlag(WeaponType.F_LASER))
              .thenReturn(true);
        WeaponMounted laser =
              mock(WeaponMounted.class);
        when(laser.canFire()).thenReturn(true);
        when(laser.getType()).thenReturn(laserType);
        Entity armedMover = mock(Entity.class);
        when(armedMover.getWeaponList())
              .thenReturn(new ArrayList<>(List.of(laser)));
        when(armedMover.getBoardId()).thenReturn(0);
        when(armedMover.getPosition()).thenReturn(new Coords(10, 10));
        when(armedMover.getAltitude()).thenReturn(5);
        when(armedMover.getGame()).thenReturn(game);

        List<Integer> candidates = finder.candidateAltitudes(armedMover);

        assertTrue(candidates.contains(AeroGroundDoctrinePathFinder.STRAFE_WINDOW_ALTITUDE),
              "energy guns must buy the strafe-window altitude");
    }

    @Test
    void enemiesOnAnotherBoardAreIgnored() {
        Entity offBoardEnemy = enemyFighter(20, new Coords(20, 20), 3, true);
        offBoardEnemy.setBoardId(7);

        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertFalse(candidates.contains(3), "an enemy over a different map says nothing about this one");
    }

    /**
     * NoE is never offered: dive bombing is illegal below altitude 3 and the bot cannot strafe - the
     * one thing NoE serves. A live Cheetah loitered at altitude 1 on the old bombs-aboard candidate,
     * where any failed control roll is the ground, and died there.
     */
    @Test
    void napOfTheEarthIsNeverOffered() {
        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertFalse(candidates.contains(AeroGroundPathFinder.NAP_OF_THE_EARTH),
              "NoE serves only strafing, which the bot cannot fly");
    }

    /**
     * Climbing to gain altitude for future things is good flying (Dave): the climb-out candidate is
     * always on offer, two levels up capped at the safe-recovery ceiling, so the porpoise profile -
     * climb between runs, drop into the window, climb out - is generatable at all.
     */
    @Test
    void aClimbOutCandidateIsAlwaysOffered() {
        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertTrue(candidates.contains(Math.min(mover.getAltitude() + 2,
                    AeroGroundDoctrinePathFinder.CLIMB_OUT_CEILING)),
              "the fighter must be able to generate climbing paths between attack runs");
    }

    @Test
    void theCandidateListStaysShort() {
        enemyFighter(20, new Coords(20, 20), 2, true);
        enemyFighter(21, new Coords(21, 21), 4, true);
        enemyFighter(22, new Coords(22, 22), 6, true);
        enemyFighter(23, new Coords(23, 23), 8, true);

        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertTrue(candidates.size() <= AeroGroundDoctrinePathFinder.MAXIMUM_CANDIDATE_ALTITUDES,
              "path count multiplies with every candidate, so the list is capped");
    }

    @Test
    void theNearestCommittedEnemyIsWeighedFirst() {
        enemyFighter(20, new Coords(30, 30), 2, true);
        enemyFighter(21, new Coords(7, 7), 8, true);

        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertEquals(8, candidates.getFirst(), "the closer threat is the one worth matching");
    }

    // --- the cost of offering a choice ---------------------------------------------------------------

    /**
     * Guards this change's main risk. Offering altitudes multiplies an already large path set, and the stock
     * finder already carries an {@code OutOfMemoryError} catch. Two things hold it down: the candidate list is
     * capped, and paths are deduplicated by the altitude they actually reach - the stock adjustment climbs
     * only as far as thrust allows and descends one level per turn, so several wishes collapse onto one
     * outcome.
     */
    @Test
    void offeringAltitudesDoesNotMultiplyThePathSetWithoutBound() {
        mover.setOriginalWalkMP(6);
        ((AeroSpaceFighter) mover).setSI(6);
        ((AeroSpaceFighter) mover).setCurrentVelocity(1);
        enemyFighter(20, new Coords(20, 20), 3, true);
        enemyFighter(21, new Coords(22, 22), 1, true);

        AeroGroundPathFinder stock = AeroGroundPathFinder.getInstance(game);
        stock.run(new MovePath(game, mover, null));
        int stockPaths = stock.getAllComputedPathsUncategorized().size();

        AeroGroundDoctrinePathFinder doctrine = AeroGroundDoctrinePathFinder.getInstance(game);
        doctrine.run(new MovePath(game, mover, null));
        int doctrinePaths = doctrine.getAllComputedPathsUncategorized().size();

        assertTrue(stockPaths > 0, "the stock finder should produce paths to compare against");
        assertTrue(doctrinePaths >= stockPaths, "the doctrine finder should not lose options");
        // Altitude candidates multiply the stock set; maneuver roots (both enemies here have moved) add
        // their own bounded subtrees on top. The bound is deliberately generous - the point is to catch
        // unbounded growth, not to freeze the exact count.
        assertTrue(doctrinePaths <= (stockPaths * AeroGroundDoctrinePathFinder.MAXIMUM_CANDIDATE_ALTITUDES) + 250,
              "path count grew from " + stockPaths + " to " + doctrinePaths
                    + ", beyond the candidate cap plus the maneuver allowance");
    }

    @Test
    void moreThanOneAltitudeIsActuallyReached() {
        mover.setOriginalWalkMP(6);
        ((AeroSpaceFighter) mover).setSI(6);
        ((AeroSpaceFighter) mover).setCurrentVelocity(1);
        enemyFighter(20, new Coords(20, 20), 3, true);

        AeroGroundDoctrinePathFinder doctrine = AeroGroundDoctrinePathFinder.getInstance(game);
        doctrine.run(new MovePath(game, mover, null));

        Set<Integer> altitudes = new java.util.HashSet<>();
        for (MovePath path : doctrine.getAllComputedPathsUncategorized()) {
            altitudes.add(path.getFinalAltitude());
        }

        assertTrue(altitudes.size() > 1,
              "the whole point is that the ranker gets a choice; reached only " + altitudes);
    }

    // --- special maneuvers (TW p.85) -------------------------------------------------------------------

    private Entity armedFlyer(Coords position, int facing, int velocity) {
        mover.setPosition(position);
        mover.setFacing(facing);
        mover.setSecondaryFacing(facing);
        mover.setOriginalWalkMP(6);
        ((AeroSpaceFighter) mover).setSI(7);
        ((AeroSpaceFighter) mover).setCurrentVelocity(velocity);
        return mover;
    }

    private long maneuverRootedPaths(AeroGroundDoctrinePathFinder finder) {
        finder.run(new MovePath(game, mover, null));
        long count = 0;
        for (MovePath path : finder.getAllComputedPathsUncategorized()) {
            if (!path.getStepVector().isEmpty()
                  && path.getStepVector().get(0).getType() == megamek.common.enums.MoveStepType.MANEUVER) {
                count++;
            }
        }
        return count;
    }

    /**
     * The live hang, replayed: a fighter cornered with its velocity committed toward the edge, whose every
     * ordinary path leaves the board. The maneuver set must hand it a way to stay in the game - this is the
     * escape gate, and it deliberately needs no enemy at all.
     */
    @Test
    void aCorneredFighterGetsManeuverEscapePaths() {
        armedFlyer(new Coords(37, 3), 1, 3);

        AeroGroundDoctrinePathFinder finder = AeroGroundDoctrinePathFinder.getInstance(game);
        finder.run(new MovePath(game, mover, null));

        boolean maneuverOnBoard = false;
        for (MovePath path : finder.getAllComputedPathsUncategorized()) {
            if (path.getStepVector().isEmpty()) {
                continue;
            }
            if (path.getStepVector().get(0).getType() == megamek.common.enums.MoveStepType.MANEUVER
                  && !path.fliesOffBoard()) {
                maneuverOnBoard = true;
                break;
            }
        }
        assertTrue(maneuverOnBoard,
              "a cornered fighter must be offered at least one maneuver path that stays on the board");
    }

    /**
     * Generation deliberately ignores enemy state: this finder runs inside Precognition before any enemy
     * has moved, so an enemy-state gate here would never open live (7,317 ranked paths, zero maneuvers, in
     * the first probe game). The committed-enemy rule is enforced at rank time -
     * {@code AerospacePathRanker.maneuverSanctioned}.
     */
    /**
     * An out-of-control aircraft cannot fly a maneuver; offering one anyway produced live turns whose
     * only candidate was a doctrine-buried Hammerhead the server would never accept.
     */
    @Test
    void anOutOfControlFighterGetsNoManeuverRoots() {
        armedFlyer(new Coords(20, 20), 0, 3);
        ((megamek.common.units.AeroSpaceFighter) mover).setOutControl(true);
        enemyFighter(20, new Coords(20, 28), 5, true);

        assertEquals(0, maneuverRootedPaths(AeroGroundDoctrinePathFinder.getInstance(game)),
              "no maneuver roots while out of control - the server will not accept them");
    }

    @Test
    void maneuversAreGeneratedEvenBeforeEnemiesCommit() {
        armedFlyer(new Coords(20, 20), 0, 3);
        enemyFighter(20, new Coords(20, 28), 5, false);

        assertTrue(maneuverRootedPaths(AeroGroundDoctrinePathFinder.getInstance(game)) > 0,
              "maneuver roots must be generated before enemies commit - the ranker owns the gate");
    }

    /** Once the enemy has committed, the maneuver set opens. */
    @Test
    void aCommittedEnemyUnlocksOffensiveManeuvers() {
        armedFlyer(new Coords(20, 20), 0, 3);
        enemyFighter(20, new Coords(20, 28), 5, true);

        assertTrue(maneuverRootedPaths(AeroGroundDoctrinePathFinder.getInstance(game)) > 0,
              "a committed enemy should unlock maneuver-rooted paths");
    }

    /**
     * PathEnumerator discards any aero path that is not fully legal or ends with velocity unspent
     * ({@code isLegalAeroMove}). A maneuver set that only exists upstream of that filter is invisible in
     * real games - which is exactly what happened first time: 7,317 ranked paths in a live probe game,
     * none with a maneuver. At least one maneuver-rooted path must survive the same checks the enumerator
     * applies.
     */
    @Test
    void maneuverPathsSurviveTheEnumeratorLegalityFilter() {
        armedFlyer(new Coords(20, 20), 0, 3);
        enemyFighter(20, new Coords(20, 28), 5, true);

        AeroGroundDoctrinePathFinder finder = AeroGroundDoctrinePathFinder.getInstance(game);
        finder.run(new MovePath(game, mover, null));

        int maneuverPaths = 0;
        int illegal = 0;
        int velocityLeft = 0;
        int survivors = 0;
        for (MovePath path : finder.getAllComputedPathsUncategorized()) {
            if (path.getStepVector().isEmpty()
                  || path.getStepVector().get(0).getType() != megamek.common.enums.MoveStepType.MANEUVER) {
                continue;
            }
            maneuverPaths++;
            if (!path.isMoveLegal()) {
                illegal++;
            } else if ((path.getLastStep() != null) && (path.getLastStep().getVelocityLeft() != 0)) {
                velocityLeft++;
            } else {
                survivors++;
            }
        }
        assertTrue(survivors > 0, "no maneuver path survives the enumerator's filter: "
              + maneuverPaths + " generated, " + illegal + " fail isMoveLegal, "
              + velocityLeft + " end with velocity unspent");
    }
}
