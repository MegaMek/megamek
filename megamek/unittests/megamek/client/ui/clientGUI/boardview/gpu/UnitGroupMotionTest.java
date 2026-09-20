/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class UnitGroupMotionTest {
    private static final BoardScene.Waypoint START = new BoardScene.Waypoint(new Coords(2, 5), 0, 0);
    private static final BoardScene.Waypoint END = new BoardScene.Waypoint(new Coords(2, 1), 0, 0);

    private static UnitMotion movement(EntityMovementType type, boolean stagger) {
        var motion = new UnitMotion(START);
        motion.append(List.of(START, END), type, 3, false, 4, stagger ? 6 : 0);
        return motion;
    }

    @Test
    void delayedMembersKeepTheirTravelTimeAndFinishBeforeTheGroupCompletes() {
        for (var type : List.of(EntityMovementType.MOVE_WALK, EntityMovementType.MOVE_JUMP)) {
            var single = movement(type, false);
            var group = movement(type, true);
            double duration = single.remainingSeconds();
            assertEquals(duration + UnitMotion.GROUP_START_JITTER_SECONDS + UnitMotion.FORMATION_SETTLE_SECONDS,
                  group.remainingSeconds(), 1e-9);
            group.advance(.08, 1);
            int started = 0;
            var progresses = new HashSet<Float>();
            for (int slot = 1; slot <= 6; slot++) {
                var sample = group.sample().member(10, "trooper-" + slot);
                if (sample.moving()) { started++; }
                progresses.add(sample.progress());
                assertEquals(sample.moving() && type == EntityMovementType.MOVE_JUMP, sample.jets() != null);
            }
            assertTrue(started > 0 && started < 6, "Some troops leave while the others still wait");
            assertTrue(progresses.size() > 2);
            group.advance(duration - .08, 1);
            assertTrue(group.isMoving(), "The last troops must finish before the event's completion hold starts");
            int moving = 0;
            for (int slot = 1; slot <= 6; slot++) {
                if (group.sample().member(10, "trooper-" + slot).moving()) { moving++; }
            }
            assertTrue(moving > 0 && moving < 6);
            group.advance(UnitMotion.GROUP_START_JITTER_SECONDS, 1);
            assertTrue(group.isMoving(), "Arrival orientation must finish before the shared completion buffer");
            group.advance(UnitMotion.FORMATION_SETTLE_SECONDS, 1);
            assertFalse(group.isMoving());
            for (int slot = 1; slot <= 6; slot++) {
                assertFalse(group.sample().member(10, "trooper-" + slot).moving());
                assertTrue(group.sample().group().offset(10, "trooper-" + slot).isZero(.001f));
            }
        }
    }

    @Test
    void frameRateAndPlaybackSpeedDoNotRerollMemberTiming() {
        var slow = movement(EntityMovementType.MOVE_JUMP, true);
        var fast = movement(EntityMovementType.MOVE_JUMP, true);
        for (int i = 0; i < 60; i++) { slow.advance(1.0 / 30, UnitMotion.Speed.NORMAL.rate); }
        for (int i = 0; i < 144; i++) { fast.advance(1.0 / 144, UnitMotion.Speed.DOUBLE.rate); }
        for (int slot = 1; slot <= 6; slot++) {
            String member = "trooper-" + slot;
            assertEquals(slow.sample().member(10, member).progress(), fast.sample().member(10, member).progress(), 1e-6);
            assertTrue(slow.sample().group().offset(10, member).epsilonEquals(fast.sample().group().offset(10, member), .001f));
        }
        slow.finish();
        assertFalse(slow.isMoving());
        assertEquals(BoardGeometry.center(END.coords(), 0), slow.position());
        assertTrue(slow.sample().group() == null && slow.sample().jets() == null, "Skip removes delayed travel and exhaust");
    }
}
