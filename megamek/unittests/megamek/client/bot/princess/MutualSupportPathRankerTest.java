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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the rules the Mutual Support doctrine must always obey: cohesion never taxes a closing path, its weight is capped
 * below the aggression weight, cover is a bonus among advances, and the closing tempo is uniform across unit
 * speeds.
 */
class MutualSupportPathRankerTest {

    private static final double TOLERANCE = 0.0001;

    /**
     * The most the formation term may ever cost one path: a turn of advance, scaled by the same ceiling factor
     * that bounds the per-hex weight. Written out from the mocked aggression of 2.5 rather than read from the
     * class, so the test fails if the relationship changes rather than following it.
     */
    private static final double MAXIMUM_FORMATION_PENALTY = 15.0 * 2.5 * 0.8;

    private static final Coords CURRENT_POSITION = new Coords(0, 10);
    private static final Coords CLOSING_DESTINATION = new Coords(5, 10);
    private static final Coords HOLDING_DESTINATION = new Coords(0, 11);

    private Princess mockPrincess;
    private BehaviorSettings mockBehavior;
    private Game mockGame;
    private Entity mockMover;
    private Entity mockFriend;
    private UnitBehavior mockBehaviorTracker;
    private MovePath mockPath;
    private MutualSupportPathRanker testRanker;

    @BeforeEach
    void setUp() {
        mockPrincess = mock(Princess.class);
        mockBehavior = mock(BehaviorSettings.class);
        mockGame = mock(Game.class);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        when(mockPrincess.getGame()).thenReturn(mockGame);
        when(mockGame.getCurrentRound()).thenReturn(1);
        when(mockBehavior.getMutualSupportValue()).thenReturn(1.0);
        when(mockBehavior.getHyperAggressionValue()).thenReturn(2.5);
        when(mockBehavior.isExclusiveMutualSupport()).thenReturn(true);
        // consumed by the BasicPathRanker constructor; both reject non-positive values
        when(mockBehavior.getNumberOfEnemiesToConsiderFacing()).thenReturn(3);
        when(mockBehavior.getAllowFacingTolerance()).thenReturn(1);

        mockMover = mock(BipedMek.class);
        when(mockMover.getId()).thenReturn(1);
        when(mockMover.getPosition()).thenReturn(CURRENT_POSITION);

        mockFriend = mock(BipedMek.class);
        when(mockFriend.getId()).thenReturn(2);
        when(mockFriend.isOffBoard()).thenReturn(false);

        // Formation membership: by default the friend is in the line, so it anchors the formation.
        mockBehaviorTracker = mock(UnitBehavior.class);
        when(mockBehaviorTracker.getBehaviorType(any(Entity.class), any(Princess.class)))
              .thenReturn(BehaviorType.Engaged);
        when(mockPrincess.getUnitBehaviorTracker()).thenReturn(mockBehaviorTracker);
        when(mockPrincess.isFallingBack(any(Entity.class))).thenReturn(false);
        when(mockPrincess.getEntitiesOwned()).thenReturn(List.of(mockMover, mockFriend));
        when(mockGame.onTheSameBoard(any(Entity.class), any(Entity.class))).thenReturn(true);

        mockPath = mock(MovePath.class);
        when(mockPath.getEntity()).thenReturn(mockMover);

        testRanker = spy(new MutualSupportPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();
        // every unit's band: peak range 6, supports friends out to 9
        doReturn(new SupportEnvelope(6, 9)).when(testRanker)
              .getSupportEnvelope(any(Entity.class));
        // the force forms up within 9 hexes of its centre of mass
        doReturn(9).when(testRanker).formationRadius(any(Game.class));
    }

    private void setEnemyDistances(double fromCurrent, double fromDestination, Coords destination) {
        doReturn(fromCurrent).when(testRanker)
              .distanceToClosestEnemy(any(Entity.class), eq(CURRENT_POSITION), any(Game.class));
        doReturn(fromDestination).when(testRanker)
              .distanceToClosestEnemy(any(Entity.class), eq(destination), any(Game.class));
        when(mockPath.getFinalCoords()).thenReturn(destination);
    }

    /**
     * The rule, in its current form: ending inside the formation costs nothing. The formation's centre travels
     * with the force, so a company advancing together is never charged however fast it advances.
     */
    @Test
    void advancingWithTheForceIsFree() {
        when(mockFriend.getPosition()).thenReturn(new Coords(0, 14)); // 4 hexes from the destination
        setEnemyDistances(20.0, 15.0, CLOSING_DESTINATION);
        assertEquals(0.0, testRanker.calculateMutualSupportMod(null, mockPath), TOLERANCE);
    }

    /**
     * The behaviour this replaced an exemption with. Previously any closing path was exempt from cohesion entirely,
     * which meant it switched off for the whole approach - the phase where formation actually matters - and a
     * company gave back a formation it had been handed within a few rounds. Closing no longer buys immunity from
     * leaving your own force behind.
     */
    @Test
    void leavingTheForceBehindIsPenalisedEvenWhileClosing() {
        when(mockBehavior.getMutualSupportValue()).thenReturn(25.0);
        when(mockFriend.getPosition()).thenReturn(new Coords(0, 25)); // 17 hexes from the destination
        setEnemyDistances(20.0, 15.0, CLOSING_DESTINATION);

        // 8 hexes outside the 9-hex formation would be 8 * 2.0 * 5.0 = 80 unbounded; the ceiling holds it here.
        assertEquals(MAXIMUM_FORMATION_PENALTY, testRanker.calculateMutualSupportMod(null, mockPath), TOLERANCE);
    }

    @Test
    void testHoldingPathBeyondSupportIsPenalizedAtCappedWeight() {
        // Not closing, and 6 hexes outside the formation. The setting is cranked to 10x the aggression weight,
        // but the ceiling holds the cohesion weight to aggression * 0.8 = 2.0.
        when(mockBehavior.getMutualSupportValue()).thenReturn(25.0);
        when(mockFriend.getPosition()).thenReturn(new Coords(0, 26)); // 15 hexes from destination
        setEnemyDistances(20.0, 20.0, HOLDING_DESTINATION);
        assertEquals(MAXIMUM_FORMATION_PENALTY, testRanker.calculateMutualSupportMod(null, mockPath), TOLERANCE);
    }

    /**
     * The recorded reasoning is written for every path, including the ones this doctrine bows out of, and the
     * fields carrying it are reused across paths. A path that scores nothing must therefore say so: leaving the
     * previous path's figures standing makes the log lie, and a reader cannot tell a stale number from a real one.
     */
    @Test
    void aPathWithNoFriendsRecordsNothingRatherThanTheLastPathsFigures() {
        // First rank a path with a friend present, so the doctrine fields hold real values.
        when(mockFriend.getPosition()).thenReturn(new Coords(0, 26));
        setEnemyDistances(20.0, 20.0, HOLDING_DESTINATION);
        testRanker.calculateMutualSupportMod(null, mockPath);
        assertTrue(testRanker.doctrineScores().get("hexesOutOfFormation") > 0.0,
              "precondition: the first path must leave real figures behind");

        // Now rank one with nobody to form up on. The friends list is cached per mover per round, so the round
        // is advanced to clear it - which is also the real shape of the bug: the recorded fields are ranker
        // state shared by every mover, so one unit's figures outlive it into the next unit's paths.
        when(mockGame.getCurrentRound()).thenReturn(2);
        when(mockPrincess.getEntitiesOwned()).thenReturn(List.of(mockMover));
        testRanker.calculateMutualSupportMod(null, mockPath);

        Map<String, Double> scores = testRanker.doctrineScores();
        assertEquals(0.0, scores.get("hexesOutOfFormation"), TOLERANCE);
        assertEquals(0.0, scores.get("formationRadius"), TOLERANCE);
        assertEquals(0.0, scores.get("coverBonus"), TOLERANCE);
        assertEquals(0.0, scores.get("coveringFriends"), TOLERANCE);
        assertEquals(-1.0, scores.get("formationCentre_x"), TOLERANCE,
              "no formation means no centre, which the log records as -1");
    }

    /**
     * However far out of position a unit is, holding formation must never be worth more to it than a turn's
     * advance. Without this ceiling the penalty grew without limit: a unit far enough out of position would refuse
     * to advance at any price. At the mocked settings here a turn's advance is worth 37.5 and the ceiling is 30.
     */
    @Test
    void theFormationPenaltyNeverExceedsATurnOfAdvance() {
        when(mockBehavior.getMutualSupportValue()).thenReturn(25.0);
        setEnemyDistances(20.0, 20.0, HOLDING_DESTINATION);

        when(mockFriend.getPosition()).thenReturn(new Coords(0, 26));
        double moderatelyOutOfPosition = testRanker.calculateMutualSupportMod(null, mockPath);

        // Far enough out that the unbounded penalty would be several times larger.
        when(mockFriend.getPosition()).thenReturn(new Coords(0, 33));
        double farOutOfPosition = testRanker.calculateMutualSupportMod(null, mockPath);

        assertEquals(moderatelyOutOfPosition, farOutOfPosition, TOLERANCE,
              "the penalty is bounded, so being much further out costs no more");
        assertEquals(MAXIMUM_FORMATION_PENALTY, farOutOfPosition, TOLERANCE);
    }

    /**
     * A unit leaving the line should not take the line's centre of mass with it, but dropping its influence to
     * nothing moves that centre discontinuously - and the jump lands exactly when a force starts taking casualties.
     * Measured over four runs, excluding withdrawers outright cost the units still fighting about 0.3 supporting
     * friends each. Its say fades instead of vanishing.
     */
    @Test
    void aWithdrawingFriendStillInfluencesTheFormationButLess() {
        when(mockPrincess.isFallingBack(mockFriend)).thenReturn(true);
        when(mockFriend.getPosition()).thenReturn(new Coords(0, 26));
        setEnemyDistances(20.0, 20.0, HOLDING_DESTINATION);

        double withFading = testRanker.calculateMutualSupportMod(null, mockPath);

        assertTrue(withFading > 0.0,
              "a withdrawing friend still anchors the formation, so a path far from it is still penalised");
    }

    /**
     * A retreat that is not a formation is a rout. A withdrawing unit is exempt from the fighting line's pull and
     * carries little weight in its centre, so without this it is governed by nothing and runs alone.
     */

    @Test
    void testHoldingInsideSupportIsFree() {
        // Inside the formation, spacing costs nothing - the blob attractor is gone.
        when(mockFriend.getPosition()).thenReturn(new Coords(0, 18)); // 7 hexes from destination
        setEnemyDistances(20.0, 20.0, HOLDING_DESTINATION);
        assertEquals(0.0, testRanker.calculateMutualSupportMod(null, mockPath), TOLERANCE);
    }

    @Test
    void testCoverBonusForAdvancingInsideSetFriendEnvelope() {
        // Closing inside the threat envelope with an already-moved friend covering the destination:
        // the covered advance earns a bonus (negative modifier).
        when(mockFriend.getPosition()).thenReturn(new Coords(5, 15)); // 5 hexes from destination
        when(mockFriend.isDone()).thenReturn(true);
        setEnemyDistances(14.0, 9.0, CLOSING_DESTINATION);
        // One covering friend at the cover bonus. The value is small on purpose: measured over 200 games the
        // bonus changes nothing at any size, because the formation term now does this shaping directly.
        assertEquals(-2.0, testRanker.calculateMutualSupportMod(null, mockPath), TOLERANCE);
    }

    @Test
    void testNoCoverShapingOutOfContact() {
        // Out of the threat envelope the force travels loose and fast: no cover bonus.
        when(mockFriend.getPosition()).thenReturn(new Coords(5, 15));
        when(mockFriend.isDone()).thenReturn(true);
        setEnemyDistances(30.0, 25.0, CLOSING_DESTINATION);
        assertEquals(0.0, testRanker.calculateMutualSupportMod(null, mockPath), TOLERANCE);
    }

    @Test
    void testClosingTempoIsUniformAcrossSpeeds() {
        // A full move's advance is worth the same commit signal for a 3/5 assault and a 6/9 medium:
        // each closes one turn's worth of its remaining gap.
        when(mockMover.getAnyTypeMaxJumpMP()).thenReturn(0);

        // Slow unit: run 5, gap 14 -> 9 hexes beyond its 6-hex band (20 - 6, 15 - 6).
        when(mockMover.getRunMP()).thenReturn(5);
        setEnemyDistances(20.0, 15.0, CLOSING_DESTINATION);
        double slowBefore = testRanker.calculateAggressionMod(mockMover, pathEndingAt(CURRENT_POSITION), mockGame);
        double slowAfter = testRanker.calculateAggressionMod(mockMover, pathEndingAt(CLOSING_DESTINATION), mockGame);
        double slowGain = slowBefore - slowAfter;

        // Fast unit: run 9, closing 9 hexes in one move (24 -> 15 from the enemy).
        when(mockMover.getRunMP()).thenReturn(9);
        doReturn(24.0).when(testRanker)
              .distanceToClosestEnemy(any(Entity.class), eq(CURRENT_POSITION), any(Game.class));
        doReturn(15.0).when(testRanker)
              .distanceToClosestEnemy(any(Entity.class), eq(CLOSING_DESTINATION), any(Game.class));
        double fastBefore = testRanker.calculateAggressionMod(mockMover, pathEndingAt(CURRENT_POSITION), mockGame);
        double fastAfter = testRanker.calculateAggressionMod(mockMover, pathEndingAt(CLOSING_DESTINATION), mockGame);
        double fastGain = fastBefore - fastAfter;

        assertEquals(slowGain, fastGain, TOLERANCE);
    }

    @Test
    void testNoAggressionPullInsideOwnBand() {
        // Inside its own engagement band the unit is not pulled further in - nothing drags a fire-support
        // unit to point-blank range.
        when(mockMover.getRunMP()).thenReturn(5);
        when(mockMover.getAnyTypeMaxJumpMP()).thenReturn(0);
        setEnemyDistances(5.0, 5.0, HOLDING_DESTINATION);
        assertEquals(0.0, testRanker.calculateAggressionMod(mockMover, pathEndingAt(HOLDING_DESTINATION), mockGame),
              TOLERANCE);
    }

    private MovePath pathEndingAt(Coords destination) {
        MovePath path = mock(MovePath.class);
        when(path.getEntity()).thenReturn(mockMover);
        when(path.getFinalCoords()).thenReturn(destination);
        return path;
    }
}
