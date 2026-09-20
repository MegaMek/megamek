/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;
import org.junit.jupiter.api.Test;

class UnitMotionTest {
    private static final BoardScene.Waypoint START = point(0);

    private static BoardScene.Waypoint point(int row) {
        return new BoardScene.Waypoint(new Coords(0, row), 0, 0);
    }

    private static UnitMotion walk(int distance, int mp) {
        var motion = new UnitMotion(START);
        motion.append(List.of(START, point(distance)), EntityMovementType.MOVE_WALK, 0, false, mp);
        return motion;
    }

    @Test
    void normalIsHalfTheOldClockAndSpeedControlsAreActualMultipliers() {
        var normal = walk(4, 4);
        var doubled = walk(4, 4);
        normal.advance(1.4, UnitMotion.Speed.NORMAL.rate);
        doubled.advance(.7, UnitMotion.Speed.DOUBLE.rate);
        assertEquals(.5f, normal.sample().progress(), .001f);
        assertTrue(normal.position().epsilonEquals(doubled.position(), .001f));
        for (var speed : UnitMotion.Speed.values()) {
            var motion = walk(4, 4);
            motion.advance(speed == UnitMotion.Speed.INSTANT ? 0 : 1.4 / speed.rate, speed.rate);
            assertFalse(motion.isMoving(), speed.name());
            assertEquals(BoardGeometry.center(point(4).coords(), 0), motion.position());
        }
        assertEquals(UnitMotion.Speed.DOUBLE, UnitMotion.Speed.NORMAL.next());
        assertEquals(UnitMotion.Speed.HALF, UnitMotion.Speed.INSTANT.next());
    }

    @Test
    void longRoutesTakeLongerAndUnequalLegsKeepTheSamePace() {
        var shortPath = walk(4, 4);
        var longPath = walk(8, 4);
        var subdivided = new UnitMotion(START);
        subdivided.append(List.of(START, point(1), point(4), point(8)), EntityMovementType.MOVE_WALK, 0, false, 4);
        assertEquals(shortPath.remainingSeconds() * 2 - UnitMotion.RAMP_SECONDS, longPath.remainingSeconds(), .001);
        assertEquals(longPath.remainingSeconds(), subdivided.remainingSeconds(), .001);
        for (var motion : List.of(shortPath, longPath, subdivided)) {
            motion.advance(.7, 1);
            assertTrue(motion.position().epsilonEquals(BoardGeometry.center(point(2).coords(), 0), .001f));
        }
    }

    @Test
    void unitCapabilityChangesPaceButExtremeUnitsRespectBothBounds() {
        var heavy = walk(4, 3);
        var fast = walk(4, 12);
        assertTrue(heavy.remainingSeconds() > fast.remainingSeconds() * 2);
        assertEquals(walk(4, 1).remainingSeconds(), walk(4, 2).remainingSeconds(), .001, "Slow cap");
        assertEquals(walk(4, 12).remainingSeconds(), walk(4, 40).remainingSeconds(), .001, "Fast cap");
        assertEquals(walk(4, 40).remainingSeconds(), walk(4, 400).remainingSeconds(), .001);
        assertTrue(Double.isFinite(walk(4, 0).remainingSeconds()));
        heavy.advance(.3, 1);
        fast.advance(.3, 1);
        assertTrue(fast.sample().steps() > heavy.sample().steps(), "Gait follows displayed distance");
    }

    @Test
    void gameCalculatedRunAndSprintMpAreNotMultipliedTwice() {
        var run = new UnitMotion(START);
        var sprint = new UnitMotion(START);
        run.append(List.of(START, point(4)), EntityMovementType.MOVE_RUN, 0, false, 6);
        sprint.append(List.of(START, point(4)), EntityMovementType.MOVE_SPRINT, 0, false, 8);
        assertEquals(walk(4, 6).remainingSeconds(), run.remainingSeconds(), .001);
        assertEquals(walk(4, 8).remainingSeconds(), sprint.remainingSeconds(), .001);
        assertTrue(sprint.remainingSeconds() < run.remainingSeconds());
    }

    @Test
    void turningAndClimbingHaveTimeAndFacingWrapUsesTheShortTurn() {
        var from = new BoardScene.Waypoint(START.coords(), 0, 5);
        var to = new BoardScene.Waypoint(START.coords(), 0, 0);
        var turn = new UnitMotion(from);
        turn.append(List.of(from, to), EntityMovementType.MOVE_WALK, 0);
        turn.advance(turn.remainingSeconds() / 2, 1);
        assertEquals(330, turn.facing(), .001f);
        assertEquals(BoardGeometry.center(START.coords(), 0), turn.position());
        var climb = new UnitMotion(START);
        var high = new BoardScene.Waypoint(point(1).coords(), 4, 0);
        climb.append(List.of(START, high), EntityMovementType.MOVE_WALK, 0);
        assertTrue(climb.remainingSeconds() > walk(1, 4).remainingSeconds());
        climb.advance(climb.remainingSeconds() / 2, 1);
        assertEquals(BoardGeometry.LEVEL * 2, climb.position().z, .001f);
    }

    @Test
    void frameRateAndSpeedChangesDoNotChangeThePathOrLoseExcessTime() {
        var slow = walk(4, 4);
        var fast = walk(4, 4);
        slow.advance(.3, 1);
        for (int i = 0; i < 30; i++) {
            fast.advance(.01, 1);
        }
        assertTrue(slow.position().epsilonEquals(fast.position(), .001f));
        slow.advance(.1, .5);
        fast.advance(.025, 2);
        assertTrue(slow.position().epsilonEquals(fast.position(), .001f));
        slow.append(List.of(point(4), point(5)), EntityMovementType.MOVE_WALK, 0);
        slow.advance(slow.remainingSeconds() + .25, 1);
        assertFalse(slow.isMoving());
        assertEquals(BoardGeometry.center(point(5).coords(), 0), slow.position());
        assertEquals(.25f, slow.sample().settledSeconds(), .001f);
    }

    @Test
    void queuedMovementDoesNotPauseAtADuplicatedBoundary() {
        var motion = walk(1, 4);
        double first = motion.remainingSeconds();
        motion.append(List.of(point(1), point(2)), EntityMovementType.MOVE_WALK, 0, false, 4);
        motion.advance(first * 1.5, 1);
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(point(1).coords(), 0)
              .lerp(BoardGeometry.center(point(2).coords(), 0), .5f), .001f));
    }

    @Test
    void postureFollowsResolvedWaypointsAndReverseTravelRetainsDirection() {
        var south = new BoardScene.Waypoint(point(1).coords(), 0, 3, ProneCause.NONE);
        var fallen = new BoardScene.Waypoint(START.coords(), 0, 3, ProneCause.FORCED);
        var recovered = new BoardScene.Waypoint(START.coords(), 0, 3, ProneCause.NONE);
        var motion = new UnitMotion(south);
        motion.append(List.of(south, fallen), EntityMovementType.MOVE_WALK, 0);
        double first = motion.remainingSeconds();
        motion.append(List.of(fallen, recovered), EntityMovementType.MOVE_WALK, 0);
        motion.advance(first / 2, 1);
        assertEquals(-1, motion.sample().forward(), .001f);
        assertEquals(ProneCause.NONE, motion.sample().proneCause());
        motion.advance(first / 2 + .01, 1);
        assertEquals(ProneCause.FORCED, motion.sample().proneCause());
        motion.finish();
        assertFalse(motion.isMoving());
        assertEquals(BoardGeometry.center(START.coords(), 0), motion.position());
    }

    @Test
    void jumpExhaustUsesActualDurationAndFadesAfterTheRealApex() {
        var landing = new BoardScene.Waypoint(point(2).coords(), 3, 0);
        var motion = new UnitMotion(START);
        motion.append(List.of(START, landing), EntityMovementType.MOVE_JUMP, 5);
        double duration = motion.remainingSeconds();
        long sequence = motion.sample().jets().sequence();
        motion.advance(duration * .55, 1);
        assertEquals(1, motion.sample().jets().flame());
        assertEquals(1 - motion.sample().progress(), motion.sample().jets().smoke(), .001f);
        assertEquals(duration * .55, motion.sample().jets().seconds(), .001);
        assertEquals(duration, motion.sample().jets().duration(), .001);
        motion.advance(duration * .3, 1);
        assertTrue(motion.sample().jets().flame() < .5f);
        motion.advance(duration * .15, 1);
        assertNull(motion.sample().jets());
        motion.append(List.of(landing, START), EntityMovementType.MOVE_JUMP, 3);
        assertTrue(motion.sample().jets().sequence() > sequence);
        motion.finish();
        assertNull(motion.sample().jets());
    }

    @Test
    void jumpsRespectHeightAndCapabilityAndFinishAtTheExactDestination() {
        for (float height : new float[] { -4, 0, 1, 2 }) {
            var landing = new BoardScene.Waypoint(point(2).coords(), height, 1);
            var motion = new UnitMotion(START);
            motion.append(List.of(START, landing), EntityMovementType.MOVE_JUMP, 2);
            double duration = motion.remainingSeconds();
            float apex = Math.max(0, Math.min(2, height + 3));
            for (int i = 0; i < 100; i++) {
                motion.advance(duration / 100, 1);
                assertTrue(motion.position().z <= apex * BoardGeometry.LEVEL + .001f);
            }
            assertEquals(BoardGeometry.center(landing.coords(), height), motion.position());
            assertEquals(60, motion.facing());
        }
    }

    @Test
    void longerAndHigherJumpsReceiveMoreTime() {
        var shortJump = new UnitMotion(START);
        var longJump = new UnitMotion(START);
        var highJump = new UnitMotion(START);
        shortJump.append(List.of(START, point(1)), EntityMovementType.MOVE_JUMP, 3);
        longJump.append(List.of(START, point(12)), EntityMovementType.MOVE_JUMP, 3);
        highJump.append(List.of(START, new BoardScene.Waypoint(point(1).coords(), 6, 0), point(2)), EntityMovementType.MOVE_JUMP, 6);
        assertTrue(longJump.remainingSeconds() > shortJump.remainingSeconds());
        assertTrue(highJump.remainingSeconds() > shortJump.remainingSeconds());
    }

    @Test
    void boardingAndGearNeverStealTimeFromTravel() {
        var bare = walk(4, 4);
        var transport = new UnitMotion(START);
        transport.append(List.of(START, point(4)), EntityMovementType.MOVE_WALK, 0, true, 4);
        assertEquals(bare.remainingSeconds() + UnitMotion.BOARD_SECONDS + UnitMotion.UNLOAD_SECONDS,
              transport.remainingSeconds(), .001);
        transport.advance(UnitMotion.BOARD_SECONDS / 2, 1);
        assertEquals(BoardGeometry.center(START.coords(), 0), transport.position());
        transport.advance(UnitMotion.BOARD_SECONDS / 2 + bare.remainingSeconds(), 1);
        assertEquals(UnitMotion.Stage.UNLOAD, transport.sample().boarding().stage());
        var stopped = transport.position().cpy();
        transport.advance(UnitMotion.UNLOAD_SECONDS / 2, 1);
        assertEquals(stopped, transport.position());
        var ground = START.withAeroState(BoardScene.AeroState.LANDED).withFootprint(List.of(START.coords(), point(1).coords()));
        var air = point(4).withAeroState(BoardScene.AeroState.AIRBORNE);
        var takeoff = new UnitMotion(ground);
        takeoff.append(List.of(ground, air), EntityMovementType.MOVE_SAFE_THRUST, 0, false, 4);
        assertEquals(bare.remainingSeconds() + UnitMotion.LANDING_GEAR_SECONDS, takeoff.remainingSeconds(), .001);
        takeoff.advance(UnitMotion.LANDING_GEAR_SECONDS / 2, 1);
        assertEquals(.5f, takeoff.sample().gear().deployment(), .001f);
        assertEquals(BoardGeometry.center(ground.coords(), 0), takeoff.position());
        var finalUnit = new BoardScene.Unit(1, -1, "Ship", air, null, false, null, 4, true);
        assertEquals(ground.footprint(), takeoff.sample().placement(finalUnit).footprint());
        assertFalse(takeoff.sample().airborne(finalUnit));
    }

    @Test
    void landingDeploysAfterTravelThenSkipUsesTheFinalState() {
        var air = point(4).withAeroState(BoardScene.AeroState.AIRBORNE);
        var ground = START.withAeroState(BoardScene.AeroState.LANDED);
        var parked = new BoardScene.Unit(1, -1, "Ship", ground, null, false, null, 4, false);
        var motion = new UnitMotion(air);
        motion.append(List.of(air, ground), EntityMovementType.MOVE_SAFE_THRUST, 0);
        motion.advance(motion.remainingSeconds() - UnitMotion.LANDING_GEAR_SECONDS - .01, 1);
        assertTrue(motion.sample().airborne(parked));
        motion.advance(.01 + UnitMotion.LANDING_GEAR_SECONDS / 2, 1);
        assertFalse(motion.sample().airborne(parked));
        assertEquals(.5f, motion.sample().gear().deployment(), .001f);
        assertEquals(BoardGeometry.center(ground.coords(), 0), motion.position());
        motion.advance(0, 0);
        assertFalse(motion.isMoving());
        assertFalse(motion.sample().airborne(parked));
    }

    @Test
    void stateOnlyTransitionsQueueButRepeatedSnapshotsAndRevealDoNotReplayThem() {
        var ground = START.withAeroState(BoardScene.AeroState.LANDED);
        var air = point(4).withAeroState(BoardScene.AeroState.AIRBORNE);
        var motion = new UnitMotion(ground);
        motion.observe(air);
        double first = motion.remainingSeconds();
        motion.observe(air);
        assertEquals(first, motion.remainingSeconds());
        motion.observe(ground);
        assertTrue(motion.remainingSeconds() > first);
        motion.advance(motion.remainingSeconds(), 1);
        motion.observe(ground);
        assertFalse(motion.isMoving());
        var revealed = new UnitMotion(air);
        revealed.observe(air);
        assertFalse(revealed.isMoving());
    }

    @Test
    void observedTakeoffJoinsAFlightPathWithoutAStationaryDuplicateStep() {
        var ground = START.withAeroState(BoardScene.AeroState.LANDED);
        var air = point(1).withAeroState(BoardScene.AeroState.AIRBORNE);
        var end = point(2).withAeroState(BoardScene.AeroState.AIRBORNE);
        var motion = new UnitMotion(ground);
        motion.observe(air.withFootprint(List.of(air.coords())));
        double first = motion.remainingSeconds();
        motion.append(List.of(air, end), EntityMovementType.MOVE_SAFE_THRUST, 0);
        double second = motion.remainingSeconds() - first;
        motion.observe(end);
        motion.advance(first + second / 2, 1);
        assertTrue(motion.position().epsilonEquals(BoardGeometry.center(air.coords(), 0)
              .lerp(BoardGeometry.center(end.coords(), 0), .5f), .001f));
    }

    @Test
    void accelerationAndBrakingAreSharedByTravelAndJumpWithoutStoppingAtWaypoints() {
        for (var type : List.of(EntityMovementType.MOVE_WALK, EntityMovementType.MOVE_RUN,
              EntityMovementType.MOVE_SPRINT, EntityMovementType.MOVE_JUMP, EntityMovementType.MOVE_SAFE_THRUST)) {
            var motion = new UnitMotion(START);
            motion.append(List.of(START, point(1), point(4)), type, 3);
            double duration = motion.remainingSeconds(), delta = Math.min(.04, duration / 10);
            motion.advance(delta, 1);
            float first = motion.position().dst(BoardGeometry.center(START.coords(), 0));
            var before = motion.position().cpy();
            motion.advance(delta, 1);
            assertTrue(motion.position().dst(before) > first, type + " accelerates");
            motion.advance(duration - 4 * delta, 1);
            before.set(motion.position());
            motion.advance(delta, 1);
            float penultimate = motion.position().dst(before);
            before.set(motion.position());
            motion.advance(delta, 1);
            assertTrue(motion.position().dst(before) < penultimate, type + " brakes");
            assertFalse(motion.isMoving());
        }
        var motion = new UnitMotion(START);
        motion.append(List.of(START, point(1), point(4)), EntityMovementType.MOVE_WALK, 0, false, 4);
        motion.advance(.4, 1);
        assertEquals(BoardGeometry.center(point(1).coords(), 0).y, motion.position().y, .001f);
        motion.advance(.03, 1);
        assertEquals(BoardGeometry.HEIGHT * .1f, Math.abs(motion.position().y - BoardGeometry.center(point(1).coords(), 0).y), .001f);
    }

    @Test
    void instantAndCancellationLeaveNoQueuedTravelOrJumpEffects() {
        var motion = walk(4, 4);
        motion.append(List.of(point(4), START), EntityMovementType.MOVE_JUMP, 3);
        motion.advance(0, 0);
        assertFalse(motion.isMoving());
        assertNull(motion.sample().jets());
        assertEquals(BoardGeometry.center(START.coords(), 0), motion.position());
        motion.append(List.of(START, point(4)), EntityMovementType.MOVE_WALK, 0);
        motion.snap(point(2));
        motion.advance(1, 1);
        assertFalse(motion.isMoving());
        assertEquals(BoardGeometry.center(point(2).coords(), 0), motion.position());
    }
}
