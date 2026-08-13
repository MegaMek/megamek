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
import org.junit.jupiter.api.Test;

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

    @Test
    void enemiesOnAnotherBoardAreIgnored() {
        Entity offBoardEnemy = enemyFighter(20, new Coords(20, 20), 3, true);
        offBoardEnemy.setBoardId(7);

        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertFalse(candidates.contains(3), "an enemy over a different map says nothing about this one");
    }

    @Test
    void napOfTheEarthIsOnlyOfferedToSomethingCarryingBombs() {
        List<Integer> candidates = finder.candidateAltitudes(mover);

        assertFalse(candidates.contains(AeroGroundPathFinder.NAP_OF_THE_EARTH),
              "with no bombs there is nothing to go down there for");
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
        assertTrue(doctrinePaths <= stockPaths * AeroGroundDoctrinePathFinder.MAXIMUM_CANDIDATE_ALTITUDES,
              "path count grew from " + stockPaths + " to " + doctrinePaths
                    + ", beyond the candidate cap");
    }

    @Test
    void moreThanOneAltitudeIsActuallyReached() {
        mover.setOriginalWalkMP(6);
        ((AeroSpaceFighter) mover).setSI(6);
        ((AeroSpaceFighter) mover).setCurrentVelocity(1);
        enemyFighter(20, new Coords(20, 20), 3, true);

        AeroGroundDoctrinePathFinder doctrine = AeroGroundDoctrinePathFinder.getInstance(game);
        doctrine.run(new MovePath(game, mover, null));

        java.util.Set<Integer> altitudes = new java.util.HashSet<>();
        for (MovePath path : doctrine.getAllComputedPathsUncategorized()) {
            altitudes.add(path.getFinalAltitude());
        }

        assertTrue(altitudes.size() > 1,
              "the whole point is that the ranker gets a choice; reached only " + altitudes);
    }
}
