/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class UnitMotionTest {
    private final BoardScene.Waypoint start = new BoardScene.Waypoint(new Coords(0, 0), 0, 5);
    private final BoardScene.Waypoint end = new BoardScene.Waypoint(new Coords(1, 0), 2, 0);

    @Test
    void interpolatesAcrossStaggeredColumnsElevationAndFacingWrap() {
        UnitMotion motion = new UnitMotion(start);
        motion.append(List.of(start, end), EntityMovementType.MOVE_WALK, 0);
        motion.advance(UnitMotion.WALK_SECONDS / 2, 1);
        assertTrue(motion.position().epsilonEquals(
              BoardGeometry.center(start.coords(), 0).lerp(BoardGeometry.center(end.coords(), 2), 0.5f), 0.001f));
        assertEquals(330, motion.facing(), 0.001);
        assertTrue(motion.isMoving());
    }

    @Test
    void reachesSamePositionRegardlessOfFrameRateAndCarriesExcessTime() {
        BoardScene.Waypoint third = new BoardScene.Waypoint(new Coords(2, 1), 1, 2);
        UnitMotion slow = new UnitMotion(start);
        UnitMotion fast = new UnitMotion(start);
        slow.append(List.of(start, end, third), EntityMovementType.MOVE_WALK, 0);
        fast.append(List.of(start, end, third), EntityMovementType.MOVE_WALK, 0);
        slow.advance(0.9, 1);
        for (int frame = 0; frame < 30; frame++) {
            fast.advance(0.03, 1);
        }
        assertTrue(slow.position().epsilonEquals(fast.position(), 0.001f));
        assertEquals(slow.facing(), fast.facing(), 0.001);
        slow.advance(10, 1);
        assertFalse(slow.isMoving());
        assertTrue(slow.position().epsilonEquals(BoardGeometry.center(third.coords(), third.elevation()), 0.001f));
    }

    @Test
    void instantPlaybackAndCancellationLeaveNoQueuedSteps() {
        UnitMotion motion = new UnitMotion(start);
        motion.append(List.of(start, end), EntityMovementType.MOVE_WALK, 0);
        motion.advance(0, 0);
        assertFalse(motion.isMoving());
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(end.coords(), end.elevation()), 0.001f));
        motion.append(List.of(end, start), EntityMovementType.MOVE_RUN, 0);
        motion.snap(end);
        motion.advance(1, 1);
        assertFalse(motion.isMoving());
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(end.coords(), end.elevation()), 0.001f));
    }

    @Test
    void queuedMovementDoesNotPauseAtADuplicatedBoundary() {
        UnitMotion motion = new UnitMotion(start);
        BoardScene.Waypoint third = new BoardScene.Waypoint(new Coords(2, 0), 2, 1);
        motion.append(List.of(start, end), EntityMovementType.MOVE_WALK, 0);
        motion.advance(UnitMotion.WALK_SECONDS / 2, 1);
        motion.append(List.of(end, third), EntityMovementType.MOVE_RUN, 0);
        motion.advance(UnitMotion.WALK_SECONDS / 2 + UnitMotion.RUN_SECONDS / 2, 1);
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(end.coords(), end.elevation())
              .lerp(BoardGeometry.center(third.coords(), third.elevation()), 0.5f), 0.001f));
    }

    @Test
    void longerPathsTravelFasterWithinTheSameBudget() {
        UnitMotion shortPath = new UnitMotion(start);
        UnitMotion longPath = new UnitMotion(start);
        BoardScene.Waypoint middle = new BoardScene.Waypoint(new Coords(2, 0), 0, 0);
        BoardScene.Waypoint far = new BoardScene.Waypoint(new Coords(4, 0), 0, 0);
        shortPath.append(List.of(start, end), EntityMovementType.MOVE_WALK, 0);
        longPath.append(List.of(start, end, middle, far), EntityMovementType.MOVE_WALK, 0);
        shortPath.advance(UnitMotion.WALK_SECONDS / 2, 1);
        longPath.advance(UnitMotion.WALK_SECONDS / 2, 1);
        assertTrue(longPath.position().x > shortPath.position().x);
        shortPath.advance(UnitMotion.WALK_SECONDS / 2, 1);
        longPath.advance(UnitMotion.WALK_SECONDS / 2, 1);
        assertFalse(shortPath.isMoving());
        assertFalse(longPath.isMoving());
    }

    @Test
    void jumpHasOneArcAndLandsAtExactElevationAndFacing() {
        UnitMotion motion = new UnitMotion(start);
        BoardScene.Waypoint ridge = new BoardScene.Waypoint(new Coords(1, 0), 4, 0);
        BoardScene.Waypoint landing = new BoardScene.Waypoint(new Coords(2, 0), 1, 1);
        motion.append(List.of(start, ridge, landing), EntityMovementType.MOVE_JUMP, 4);
        motion.advance(UnitMotion.JUMP_SECONDS / 2, 1);
        assertEquals(BoardGeometry.center(ridge.coords(), 0).x, motion.position().x, 0.001f);
        assertEquals(4 * BoardGeometry.LEVEL, motion.position().z, 0.001f);
        assertTrue(motion.isMoving());
        motion.advance(UnitMotion.JUMP_SECONDS / 2, 1);
        assertFalse(motion.isMoving());
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(landing.coords(), landing.elevation()), 0.001f));
        assertEquals(60, motion.facing());
    }

    @Test
    void skipFinishesQueuedJumpAndWalkWithoutResidualArc() {
        UnitMotion motion = new UnitMotion(start);
        motion.append(List.of(start, end), EntityMovementType.MOVE_JUMP, 3);
        motion.advance(UnitMotion.JUMP_SECONDS / 2, 1);
        motion.append(List.of(end, start), EntityMovementType.MOVE_WALK, 0);
        motion.finish();
        motion.advance(10, 1);
        assertFalse(motion.isMoving());
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(start.coords(), start.elevation()), 0.001f));
        assertEquals(300, motion.facing());
    }

    @Test
    void playbackSpeedScalesTimeWithoutChangingPath() {
        UnitMotion normal = new UnitMotion(start);
        UnitMotion faster = new UnitMotion(start);
        normal.append(List.of(start, end), EntityMovementType.MOVE_RUN, 0);
        faster.append(List.of(start, end), EntityMovementType.MOVE_RUN, 0);
        normal.advance(UnitMotion.RUN_SECONDS / 2, 1);
        faster.advance(UnitMotion.RUN_SECONDS / 4, 2);
        assertTrue(normal.position().epsilonEquals(faster.position(), 0.001f));
    }

    @Test
    void jumpApexPrefersDestinationPlusThreeWithinTakeoffCapability() {
        for (int jumpMP : new int[] { 1, 2, 3, 8 }) {
            UnitMotion motion = new UnitMotion(start);
            BoardScene.Waypoint landing = new BoardScene.Waypoint(new Coords(2, 0), 0, 0);
            motion.append(List.of(start, landing), EntityMovementType.MOVE_JUMP, jumpMP);
            motion.advance(UnitMotion.JUMP_SECONDS / 2, 1);
            assertEquals(Math.min(3, jumpMP) * BoardGeometry.LEVEL, motion.position().z, 0.001f);
        }
    }

    @Test
    void unequalLandingHeightDoesNotOvershootTheApex() {
        for (float destination : new float[] { -4, 1, 2 }) {
            UnitMotion motion = new UnitMotion(start);
            BoardScene.Waypoint landing = new BoardScene.Waypoint(new Coords(2, 0), destination, 0);
            motion.append(List.of(start, landing), EntityMovementType.MOVE_JUMP, 2);
            float apex = Math.max(0, Math.min(2, destination + 3));
            for (int frame = 0; frame < 100; frame++) {
                motion.advance(UnitMotion.JUMP_SECONDS / 100, 1);
                assertTrue(motion.position().z <= apex * BoardGeometry.LEVEL + 0.001f);
            }
            assertEquals(destination * BoardGeometry.LEVEL, motion.position().z, 0.001f);
        }
    }
}
