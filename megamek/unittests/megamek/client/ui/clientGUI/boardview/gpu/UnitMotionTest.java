/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class UnitMotionTest {
    private final BoardScene.Waypoint start = new BoardScene.Waypoint(new Coords(0, 0), 0, 5);
    private final BoardScene.Waypoint end = new BoardScene.Waypoint(new Coords(1, 0), 2, 0);

    @Test
    void interpolatesAcrossStaggeredColumnsElevationAndFacingWrap() {
        UnitMotion motion = new UnitMotion(start);
        motion.append(List.of(start, end));
        motion.advance(0.1, 0.2);
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
        slow.append(List.of(start, end, third));
        fast.append(List.of(start, end, third));
        slow.advance(0.3, 0.2);
        for (int frame = 0; frame < 30; frame++) {
            fast.advance(0.01, 0.2);
        }
        assertTrue(slow.position().epsilonEquals(fast.position(), 0.001f));
        assertEquals(slow.facing(), fast.facing(), 0.001);
        slow.advance(10, 0.2);
        assertFalse(slow.isMoving());
        assertTrue(slow.position().epsilonEquals(BoardGeometry.center(third.coords(), third.elevation()), 0.001f));
    }

    @Test
    void instantPlaybackAndCancellationLeaveNoQueuedSteps() {
        UnitMotion motion = new UnitMotion(start);
        motion.append(List.of(start, end));
        motion.advance(0, 0);
        assertFalse(motion.isMoving());
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(end.coords(), end.elevation()), 0.001f));
        motion.append(List.of(end, start));
        motion.snap(end);
        motion.advance(1, 0.2);
        assertFalse(motion.isMoving());
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(end.coords(), end.elevation()), 0.001f));
    }

    @Test
    void queuedMovementDoesNotPauseAtADuplicatedBoundary() {
        UnitMotion motion = new UnitMotion(start);
        BoardScene.Waypoint third = new BoardScene.Waypoint(new Coords(2, 0), 2, 1);
        motion.append(List.of(start, end));
        motion.advance(0.1, 0.2);
        motion.append(List.of(end, third));
        motion.advance(0.2, 0.2);
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(end.coords(), end.elevation())
              .lerp(BoardGeometry.center(third.coords(), third.elevation()), 0.5f), 0.001f));
    }
}
