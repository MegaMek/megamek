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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WoodsClearingTracker}.
 *
 * <p>Per TM pp.241-243, a chainsaw or dual saw takes 2 turns to reduce a wooded hex
 * one level. Two units clearing the same hex reduce this to 1 turn. Work accumulates per hex and persists across
 * rounds (even if no entity clears in a given round) until a woods level is completely cleared.</p>
 */
class WoodsClearingTrackerTest {

    private WoodsClearingTracker tracker;
    private BoardLocation hexA;
    private BoardLocation hexB;

    @BeforeEach
    void setUp() {
        tracker = new WoodsClearingTracker();
        hexA = BoardLocation.of(new Coords(5, 5), 0);
        hexB = BoardLocation.of(new Coords(6, 6), 0);
    }

    @Nested
    @DisplayName("Single Saw Clearing (2 turns)")
    class SingleSawTests {

        @Test
        @DisplayName("One saw takes 2 turns to complete clearing")
        void singleSawTwoTurns() {
            // Turn 1: entity 1 declares clearing hex A
            tracker.declareClearing(1, hexA);
            List<BoardLocation> completed = tracker.processNewRound();
            assertTrue(completed.isEmpty(), "Should not complete after 1 turn with single saw");

            // Turn 2: entity 1 declares clearing hex A again
            tracker.declareClearing(1, hexA);
            completed = tracker.processNewRound();
            assertEquals(1, completed.size(), "Should complete after 2 turns with single saw");
            assertEquals(hexA, completed.get(0));
        }

        @Test
        @DisplayName("Clearing is not complete after only 1 turn")
        void singleSawOneTurnNotComplete() {
            tracker.declareClearing(1, hexA);
            List<BoardLocation> completed = tracker.processNewRound();
            assertTrue(completed.isEmpty(), "Should not complete after only 1 turn");
        }
    }

    @Nested
    @DisplayName("Multiple Saws Clearing (1 turn)")
    class MultipleSawTests {

        @Test
        @DisplayName("Two saws on same hex complete in 1 turn")
        void twoSawsOneTurn() {
            // Turn 1: two entities declare clearing hex A
            tracker.declareClearing(1, hexA);
            tracker.declareClearing(2, hexA);
            List<BoardLocation> completed = tracker.processNewRound();
            assertEquals(1, completed.size(), "Two saws on same hex should complete in 1 turn");
            assertEquals(hexA, completed.get(0));
        }

        @Test
        @DisplayName("Three saws on same hex also complete in 1 turn")
        void threeSawsOneTurn() {
            tracker.declareClearing(1, hexA);
            tracker.declareClearing(2, hexA);
            tracker.declareClearing(3, hexA);
            List<BoardLocation> completed = tracker.processNewRound();
            assertEquals(1, completed.size(), "Three saws should also complete in 1 turn");
        }

        @Test
        @DisplayName("Two saws on different hexes do not combine")
        void twoSawsDifferentHexes() {
            tracker.declareClearing(1, hexA);
            tracker.declareClearing(2, hexB);
            List<BoardLocation> completed = tracker.processNewRound();
            assertTrue(completed.isEmpty(),
                  "Two saws on different hexes should not complete either in 1 turn");
        }
    }

    @Nested
    @DisplayName("Work Accumulation and Persistence")
    class WorkAccumulationTests {

        @Test
        @DisplayName("Work persists when no entity clears for a round")
        void workPersistsOnPause() {
            // Turn 1: entity 1 declares clearing hex A
            tracker.declareClearing(1, hexA);
            tracker.processNewRound();

            // Turn 2: no one clears hex A (entity stopped) - work persists
            List<BoardLocation> completed = tracker.processNewRound();
            assertTrue(completed.isEmpty(), "No completions when no one clears");
            assertTrue(tracker.hasAccumulatedWork(hexA), "Work should persist on the hex");

            // Turn 3: entity 1 resumes - completes since 1 turn of work was already done
            tracker.declareClearing(1, hexA);
            completed = tracker.processNewRound();
            assertEquals(1, completed.size(),
                  "Should complete after resuming - prior work was preserved");
        }

        @Test
        @DisplayName("Work persists across multiple idle rounds")
        void workPersistsAcrossMultipleIdleRounds() {
            // Turn 1: entity 1 declares clearing hex A
            tracker.declareClearing(1, hexA);
            tracker.processNewRound();

            // Turns 2-4: no one works the hex
            tracker.processNewRound();
            tracker.processNewRound();
            tracker.processNewRound();
            assertTrue(tracker.hasAccumulatedWork(hexA), "Work should survive idle rounds");

            // Turn 5: entity 1 resumes - completes
            tracker.declareClearing(1, hexA);
            List<BoardLocation> completed = tracker.processNewRound();
            assertEquals(1, completed.size(),
                  "Should complete after gap - accumulated work was not lost");
        }

        @Test
        @DisplayName("Different entity can continue work on same hex")
        void differentEntityContinuesWork() {
            // Turn 1: entity 1 declares clearing hex A
            tracker.declareClearing(1, hexA);
            tracker.processNewRound();

            // Turn 2: entity 2 continues clearing hex A (entity 1 moved away)
            tracker.declareClearing(2, hexA);
            List<BoardLocation> completed = tracker.processNewRound();
            assertEquals(1, completed.size(),
                  "Different entity should be able to continue accumulated work");
        }

        @Test
        @DisplayName("Multiple hexes can be cleared independently")
        void multipleHexesIndependent() {
            // Turn 1: clear both hexes
            tracker.declareClearing(1, hexA);
            tracker.declareClearing(2, hexB);
            tracker.processNewRound();

            // Turn 2: continue both
            tracker.declareClearing(1, hexA);
            tracker.declareClearing(2, hexB);
            List<BoardLocation> completed = tracker.processNewRound();
            assertEquals(2, completed.size(), "Both hexes should complete independently");
        }

        @Test
        @DisplayName("hasAccumulatedWork returns false for fresh hex")
        void noAccumulatedWorkOnFreshHex() {
            assertFalse(tracker.hasAccumulatedWork(hexA), "Fresh hex should have no work");
        }

        @Test
        @DisplayName("hasAccumulatedWork returns true after one round of work")
        void hasAccumulatedWorkAfterOneRound() {
            tracker.declareClearing(1, hexA);
            tracker.processNewRound();
            assertTrue(tracker.hasAccumulatedWork(hexA), "Hex should have accumulated work");
        }
    }

    @Nested
    @DisplayName("Entity State Queries")
    class EntityStateTests {

        @Test
        @DisplayName("wasClearingLastRound returns true for entities that cleared last round")
        void wasClearingLastRound() {
            tracker.declareClearing(1, hexA);
            tracker.processNewRound();

            // Entity 1 was clearing last round (now in tracker's lastRound set)
            assertTrue(tracker.wasClearingLastRound(1),
                  "Entity 1 should be marked as clearing from last round");
            assertFalse(tracker.wasClearingLastRound(2),
                  "Entity 2 was not clearing");
        }

        @Test
        @DisplayName("isClearingThisRound returns true for entities declared this round")
        void isClearingThisRound() {
            tracker.declareClearing(1, hexA);
            assertTrue(tracker.isClearingThisRound(1));
            assertFalse(tracker.isClearingThisRound(2));
        }

        @Test
        @DisplayName("getClearingTarget returns correct hex for entity")
        void getClearingTarget() {
            tracker.declareClearing(1, hexA);
            tracker.declareClearing(2, hexB);

            assertEquals(hexA, tracker.getClearingTarget(1));
            assertEquals(hexB, tracker.getClearingTarget(2));
            assertNull(tracker.getClearingTarget(3), "Entity not clearing should return null");
        }

        @Test
        @DisplayName("getAllClearingEntities returns all entities involved")
        void getAllClearingEntities() {
            tracker.declareClearing(1, hexA);
            tracker.processNewRound();
            tracker.declareClearing(2, hexA);

            var entities = tracker.getAllClearingEntities();
            assertTrue(entities.contains(1), "Entity 1 should be in last round set");
            assertTrue(entities.contains(2), "Entity 2 should be in this round set");
        }
    }

    @Nested
    @DisplayName("Clear and Reset")
    class ClearTests {

        @Test
        @DisplayName("clear() removes all operations")
        void clearRemovesAll() {
            tracker.declareClearing(1, hexA);
            tracker.declareClearing(2, hexB);
            tracker.clear();

            assertFalse(tracker.isClearingThisRound(1));
            assertFalse(tracker.isClearingThisRound(2));
            assertTrue(tracker.getAllClearingEntities().isEmpty());
        }
    }

    @Nested
    @DisplayName("Stale Entry Cleanup")
    class StaleEntryTests {

        @Test
        @DisplayName("Removes entry when hex lookup returns null")
        void removesEntryWhenHexIsNull() {
            tracker.declareClearing(1, hexA);
            tracker.removeStaleEntries(loc -> null);

            assertFalse(tracker.isClearingThisRound(1),
                  "Entry should be removed when hex lookup returns null");
        }

        @Test
        @DisplayName("Removes entry when hex has no woods or jungle")
        void removesEntryWhenNoWoods() {
            tracker.declareClearing(1, hexA);
            Hex roughHex = new Hex();
            roughHex.addTerrain(new Terrain(Terrains.ROUGH, 1));
            tracker.removeStaleEntries(loc -> roughHex);

            assertFalse(tracker.isClearingThisRound(1),
                  "Entry should be removed when hex has no woods or jungle");
        }

        @Test
        @DisplayName("Retains entry when hex still has woods")
        void retainsEntryWithWoods() {
            tracker.declareClearing(1, hexA);
            Hex woodsHex = new Hex();
            woodsHex.addTerrain(new Terrain(Terrains.WOODS, 1));
            tracker.removeStaleEntries(loc -> woodsHex);

            assertTrue(tracker.isClearingThisRound(1),
                  "Entry should be retained when hex still has woods");
        }

        @Test
        @DisplayName("Retains entry when hex still has jungle")
        void retainsEntryWithJungle() {
            tracker.declareClearing(1, hexA);
            Hex jungleHex = new Hex();
            jungleHex.addTerrain(new Terrain(Terrains.JUNGLE, 2));
            tracker.removeStaleEntries(loc -> jungleHex);

            assertTrue(tracker.isClearingThisRound(1),
                  "Entry should be retained when hex still has jungle");
        }
    }
}
