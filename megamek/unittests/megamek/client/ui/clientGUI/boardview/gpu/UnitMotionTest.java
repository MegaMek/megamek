/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
        return walk(distance, mp, UnitMotion.DEFAULT_SPEED_GAIN_PER_HEX);
    }

    private static UnitMotion walk(int distance, int mp, double gain) {
        var motion = new UnitMotion(START);
        motion.append(List.of(START, point(distance)), EntityMovementType.MOVE_WALK, 0, false, mp, 0, gain, 1);
        return motion;
    }

    @Test
    void normalIsHalfTheOldClockAndSpeedControlsAreActualMultipliers() {
        var normal = walk(4, 4);
        var doubled = walk(4, 4);
        double duration = normal.remainingSeconds();
        normal.advance(duration, UnitMotion.Speed.NORMAL.rate);
        doubled.advance(duration / 2, UnitMotion.Speed.DOUBLE.rate);
        assertEquals(.5f, normal.sample().progress(), .001f);
        assertTrue(normal.position().epsilonEquals(doubled.position(), .001f));
        for (var speed : UnitMotion.Speed.values()) {
            var motion = walk(4, 4);
            motion.advance(speed == UnitMotion.Speed.INSTANT ? 0 : duration / speed.rate, speed.rate);
            assertFalse(motion.isMoving(), speed.name());
            assertEquals(BoardGeometry.center(point(4).coords(), 0), motion.position());
        }
        assertEquals(UnitMotion.Speed.DOUBLE, UnitMotion.Speed.NORMAL.next());
        assertEquals(UnitMotion.Speed.HALF, UnitMotion.Speed.INSTANT.next());
    }

    @Test
    void longRoutesTakeLongerAndUnequalLegsKeepTheSamePace() {
        var shortPath = walk(8, 4);
        var longPath = walk(12, 4);
        var subdivided = new UnitMotion(START);
        subdivided.append(List.of(START, point(1), point(4), point(12)), EntityMovementType.MOVE_WALK, 0, false, 4);
        assertTrue(longPath.remainingSeconds() > shortPath.remainingSeconds());
        assertTrue(longPath.remainingSeconds() < shortPath.remainingSeconds() + 4 * UnitMotion.WALK_SECONDS_PER_HEX);
        assertEquals(longPath.remainingSeconds(), subdivided.remainingSeconds(), .001);
        for (int i = 0; i < 20; i++) {
            shortPath.advance(UnitMotion.WALK_SECONDS_PER_HEX / 3, 1);
            longPath.advance(UnitMotion.WALK_SECONDS_PER_HEX / 3, 1);
            subdivided.advance(UnitMotion.WALK_SECONDS_PER_HEX / 3, 1);
            assertTrue(shortPath.position().epsilonEquals(longPath.position(), .001f), "The same distance earns the same acceleration");
            assertTrue(longPath.position().epsilonEquals(subdivided.position(), .001f), "Waypoint subdivision cannot restart easing");
        }
    }

    @Test
    void disablingMiddleGrowthKeepsTheThreeHexLaunchAndBrakingRamps() {
        var motion = new UnitMotion(START);
        motion.append(List.of(START, point(1), point(3), point(5), point(7), point(10)),
              EntityMovementType.MOVE_WALK, 0, false, 4, 0, 0, 1);
        assertEquals(6.4, motion.remainingSeconds() / UnitMotion.Speed.NORMAL.rate, .001);
        motion.advance(.6, 1);
        assertTrue(motion.sample().steps() < 1, "The curved ramp starts slowly");
        motion.advance(.6, 1);
        assertEquals(3, motion.sample().steps(), .001);
        motion.advance(.4, 1);
        assertEquals(5, motion.sample().steps(), .001);
        motion.advance(.4, 1);
        assertEquals(7, motion.sample().steps(), .001);
        motion.advance(.6, 1);
        assertTrue(motion.sample().steps() > 9, "Braking mirrors acceleration");
        motion.advance(.6, 1);
        assertFalse(motion.isMoving());
        assertEquals(BoardGeometry.center(point(10).coords(), 0), motion.position());
    }

    @Test
    void shortMovesCannotReachFullSpeedEvenWithFewerWaypoints() {
        double cruise = peakSpeed(walk(6, 4));
        double one = peakSpeed(walk(1, 4));
        double two = peakSpeed(walk(2, 4));
        assertEquals(1 / UnitMotion.WALK_SECONDS_PER_HEX, cruise, .005);
        assertTrue(one < two);
        assertEquals(.58, two / cruise, .01, "Two hexes only allow about 58% of cruise speed");
        assertTrue(walk(2, 4).remainingSeconds() > 2 * UnitMotion.WALK_SECONDS_PER_HEX);
        assertEquals(walk(2, 4, 0).remainingSeconds(), walk(2, 4, .1).remainingSeconds());
    }

    @Test
    void middleSpeedBuildsContinuouslyByTheConfiguredPercentageThenBrakesInTheLastThreeHexes() {
        for (double gain : new double[] { .03, .06 }) {
            var motion = walk(40, 4, gain);
            double base = 1 / UnitMotion.WALK_SECONDS_PER_HEX;
            for (int distance : new int[] { 3, 4, 10, 20, 36, 37 }) {
                assertEquals(base * (1 + gain * (distance - 3)), speedAtDistance(motion, distance), .04,
                      "Speed gain is continuous and accumulates across the whole middle of the route");
            }
            assertTrue(speedAtDistance(motion, 39) < base * (1 + gain * 34) * .8);
            motion.advance(motion.remainingSeconds(), 1);
            assertFalse(motion.isMoving());
            assertEquals(BoardGeometry.center(point(40).coords(), 0), motion.position());
        }
        assertTrue(walk(40, 4).remainingSeconds() < walk(40, 4, 0).remainingSeconds());
        assertEquals(6.06, walk(10, 4).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, .03);
    }

    private static double speedAtDistance(UnitMotion motion, float distance) {
        double speed = 0;
        while (motion.isMoving() && motion.sample().steps() < distance) {
            var previous = motion.position().cpy();
            motion.advance(.002, 1);
            speed = motion.position().dst(previous) / BoardGeometry.HEIGHT / .002;
        }
        return speed;
    }

    private static double peakSpeed(UnitMotion motion) {
        double step = motion.remainingSeconds() / 1000;
        double peak = 0;
        var previous = motion.position().cpy();
        for (int i = 0; i < 1000; i++) {
            motion.advance(step, 1);
            peak = Math.max(peak, motion.position().dst(previous) / BoardGeometry.HEIGHT / step);
            previous.set(motion.position());
        }
        assertFalse(motion.isMoving());
        return peak;
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
    void stationaryTurnsDoNotConsumeTheFollowingLegsAccelerationDistance() {
        var turned = new BoardScene.Waypoint(START.coords(), 0, 1);
        var end = new BoardScene.Waypoint(point(10).coords(), 0, 1);
        var turn = new UnitMotion(START);
        turn.append(List.of(START, turned), EntityMovementType.MOVE_WALK, 0, false, 4);
        var motion = new UnitMotion(START);
        motion.append(List.of(START, turned, end), EntityMovementType.MOVE_WALK, 0, false, 4);
        assertEquals(turn.remainingSeconds() + walk(10, 4).remainingSeconds(), motion.remainingSeconds(), .001);
        motion.advance(turn.remainingSeconds() / 2, 1);
        assertEquals(30, motion.facing(), .001);
        assertEquals(BoardGeometry.center(START.coords(), 0), motion.position());
        motion.advance(turn.remainingSeconds() / 2 + 1.2, 1);
        assertEquals(3, motion.sample().steps(), .001, "The travel ramp still occupies the first three hexes");
    }

    private static List<BoardScene.Waypoint> cornerPath(int direction, boolean turnSteps, boolean subdivided) {
        var start = new Coords(5, 10);
        var path = new ArrayList<BoardScene.Waypoint>();
        path.add(new BoardScene.Waypoint(start, 0, 0));
        for (int hex = 1; hex <= 5; hex++) {
            if (subdivided || hex == 5) {
                path.add(new BoardScene.Waypoint(start.translated(0, hex), 0, hex == 5 && !turnSteps ? direction : 0));
            }
        }
        var corner = start.translated(0, 5);
        if (turnSteps) {
            for (int facing = 1; facing <= direction; facing++) {
                path.add(new BoardScene.Waypoint(corner, 0, facing));
            }
        }
        for (int hex = 1; hex <= 5; hex++) {
            if (subdivided || hex == 5) {
                path.add(new BoardScene.Waypoint(corner.translated(direction, hex), 0, direction));
            }
        }
        return path;
    }

    private static UnitMotion moving(List<BoardScene.Waypoint> path, boolean transport) {
        var motion = new UnitMotion(path.getFirst());
        motion.append(path, EntityMovementType.MOVE_WALK, 0, transport, 4);
        return motion;
    }

    @Test
    void movingTurnsFollowACurveInsideTheHexAndRetainSpeed() {
        for (int direction : new int[] { 1, 2 }) {
            var path = cornerPath(direction, true, false);
            var motion = moving(path, false);
            var center = BoardGeometry.center(path.get(1).coords(), 0);
            var previous = motion.position().cpy();
            float previousFacing = motion.facing();
            double step = motion.remainingSeconds() / 2000;
            double cruise = 1 / UnitMotion.WALK_SECONDS_PER_HEX;
            double nearest = Double.POSITIVE_INFINITY, cornerSpeed = 0, cornerFacing = 0;
            for (int i = 0; i < 2000; i++) {
                motion.advance(step, 1);
                double speed = motion.position().dst(previous) / BoardGeometry.HEIGHT / step;
                if (motion.sample().progress() > .2 && motion.sample().progress() < .8) {
                    assertTrue(speed > cruise * .45, "A moving corner slows down without stopping");
                }
                double distance = motion.position().dst(center) / BoardGeometry.HEIGHT;
                if (distance < nearest) {
                    nearest = distance;
                    cornerSpeed = speed;
                    cornerFacing = motion.facing();
                }
                if (distance < .35) {
                    assertTrue(BoardGeometry.contains(path.get(1).coords(), motion.position().x, motion.position().y));
                }
                assertTrue(Math.abs(UpperBodyTurn.shortestTurn(motion.facing() - previousFacing)) < 3,
                      "Facing follows the curve without a pivot or a jump at the waypoint");
                previous.set(motion.position());
                previousFacing = motion.facing();
            }
            assertTrue(nearest > .05 && nearest < .2, "The route rounds the corner instead of reaching its center");
            assertEquals((direction == 1 ? .7 : .5) * 1.06, cornerSpeed / cruise, .025);
            assertEquals(direction * 30, cornerFacing, 3);
            assertFalse(motion.isMoving());
            assertEquals(BoardGeometry.center(path.getLast().coords(), 0), motion.position());
            assertEquals(direction * 60, motion.facing());
        }
    }

    @Test
    void turnRecordsAndStraightWaypointSubdivisionDoNotChangeTheCurveOrItsTiming() {
        var explicit = moving(cornerPath(1, true, true), false);
        var compact = moving(cornerPath(1, false, false), false);
        var transport = moving(cornerPath(1, true, true), true);
        assertEquals(compact.remainingSeconds(), explicit.remainingSeconds(), .0001);
        assertEquals(explicit.remainingSeconds() + UnitMotion.BOARD_SECONDS + UnitMotion.UNLOAD_SECONDS,
              transport.remainingSeconds(), .0001);
        transport.advance(UnitMotion.BOARD_SECONDS, 1);
        double step = explicit.remainingSeconds() / 100;
        for (int i = 0; i < 100; i++) {
            explicit.advance(step, 1);
            compact.advance(step, 1);
            transport.advance(step, 1);
            assertTrue(explicit.position().epsilonEquals(compact.position(), .01f));
            assertTrue(explicit.position().epsilonEquals(transport.position(), .01f));
            assertEquals(0, UpperBodyTurn.shortestTurn(explicit.facing() - compact.facing()), .3f);
            if (explicit.isMoving() && compact.isMoving()) {
                assertEquals(explicit.sample().steps(), compact.sample().steps(), .001f);
                assertEquals(explicit.sample().turn(), compact.sample().turn(), .001f, "Subdivision cannot add pivot steps to a curved walking gait");
            }
        }
    }

    @Test
    void consecutiveShortCornersRemainContinuousAndRespectTheShortRouteSpeedLimit() {
        var coords = new Coords(5, 10);
        var path = new ArrayList<BoardScene.Waypoint>();
        path.add(new BoardScene.Waypoint(coords, 0, 0));
        for (int direction : new int[] { 0, 1, 0, 1 }) {
            path.add(new BoardScene.Waypoint(coords, 0, direction));
            coords = coords.translated(direction);
            path.add(new BoardScene.Waypoint(coords, 0, direction));
        }
        var motion = moving(path, false);
        var oneFrame = moving(path, false);
        double duration = motion.remainingSeconds();
        var previous = motion.position().cpy();
        double peak = 0;
        for (int i = 0; i < 1000; i++) {
            motion.advance(duration / 1000, 1);
            double speed = motion.position().dst(previous) / BoardGeometry.HEIGHT / (duration / 1000);
            peak = Math.max(peak, speed);
            if (motion.sample().progress() > .25 && motion.sample().progress() < .75) {
                assertTrue(speed > 2, "Closely spaced corners retain momentum");
            }
            if (i == 499) {
                oneFrame.advance(duration / 2, 1);
                assertTrue(motion.position().epsilonEquals(oneFrame.position(), .001f));
                assertEquals(motion.facing(), oneFrame.facing(), .001f);
                assertEquals(motion.sample().steps(), oneFrame.sample().steps(), .001f);
            }
            previous.set(motion.position());
        }
        assertTrue(peak < 1 / UnitMotion.WALK_SECONDS_PER_HEX);
        assertFalse(motion.isMoving());
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
    void jumpExhaustKeepsAWeakerLandingFlameAndUsesTheSameArcTimelineAsSmoke() {
        var landing = new BoardScene.Waypoint(point(2).coords(), 3, 0);
        var motion = new UnitMotion(START);
        motion.append(List.of(START, landing), EntityMovementType.MOVE_JUMP, 5);
        double duration = motion.remainingSeconds();
        long sequence = motion.sample().jets().sequence();
        var exhaust = motion.sample().jets();
        float previousHeight = motion.position().z;
        float previousFlame = 1;
        float previousSmoke = 1;
        boolean descended = false;
        for (int i = 1; i < 100; i++) {
            motion.advance(duration / 100, 1);
            var sample = motion.sample();
            var jets = sample.jets();
            if (motion.position().z > previousHeight) {
                assertEquals(1, previousFlame, .0001);
            } else if (motion.position().z < previousHeight) {
                assertTrue(jets.flame() >= .45f && jets.flame() < 1);
                descended = true;
            }
            assertEquals(1 - sample.progress(), jets.smoke(), .0001);
            assertEquals(jets.smoke(), exhaust.smoke(duration * i / 100), .0001, "Historical emission samples the same flight");
            assertTrue(jets.smoke() <= previousSmoke);
            assertEquals(duration * i / 100, jets.seconds(), .001);
            assertEquals(duration, jets.duration(), .001);
            previousHeight = motion.position().z;
            previousFlame = jets.flame();
            previousSmoke = jets.smoke();
        }
        assertTrue(descended);
        assertEquals(.45f, motion.sample().jets().flame(), .001);
        motion.advance(duration / 100, 1);
        assertNull(motion.sample().jets());
        motion.append(List.of(landing, START), EntityMovementType.MOVE_JUMP, 3);
        assertTrue(motion.sample().jets().sequence() > sequence);
        motion.finish();
        assertNull(motion.sample().jets());
    }

    @Test
    void jumpsAddOneVisualLevelAndFinishAtTheExactDestination() {
        for (float height : new float[] { -4, 0, 1, 2 }) {
            var landing = new BoardScene.Waypoint(point(2).coords(), height, 1);
            var motion = new UnitMotion(START);
            motion.append(List.of(START, landing), EntityMovementType.MOVE_JUMP, 2);
            double duration = motion.remainingSeconds();
            float apex = Math.max(0, Math.min(2, height + 3)) + 1;
            float highest = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 100; i++) {
                motion.advance(duration / 100, 1);
                assertTrue(motion.position().z <= apex * BoardGeometry.LEVEL + .001f);
                highest = Math.max(highest, motion.position().z);
            }
            assertEquals(apex * BoardGeometry.LEVEL, highest, .05f);
            assertEquals(BoardGeometry.center(landing.coords(), height), motion.position());
            assertEquals(60, motion.facing());
        }
    }

    @Test
    void longerAndHigherJumpsReceiveMoreTime() {
        var shortJump = new UnitMotion(START);
        var longJump = new UnitMotion(START);
        var highJump = new UnitMotion(START);
        shortJump.append(List.of(START, point(1)), EntityMovementType.MOVE_JUMP, 6);
        longJump.append(List.of(START, point(12)), EntityMovementType.MOVE_JUMP, 6);
        highJump.append(List.of(START, new BoardScene.Waypoint(point(1).coords(), 6, 0), point(2)), EntityMovementType.MOVE_JUMP, 6);
        assertTrue(longJump.remainingSeconds() > shortJump.remainingSeconds());
        assertTrue(highJump.remainingSeconds() > shortJump.remainingSeconds());
    }

    @Test
    void horizontalRangeCannotStretchTheGravityDrivenDescent() {
        for (double gravity : new double[] { .5, 1, 2, 0 }) {
            var shortJump = jump(1, 0, gravity);
            var longJump = jump(20, 0, gravity);
            double landing = shortJump.remainingSeconds() / 2;
            shortJump.advance(shortJump.remainingSeconds() - landing, 1);
            longJump.advance(longJump.remainingSeconds() - landing, 1);
            for (int i = 0; i < 100; i++) {
                assertEquals(shortJump.position().z, longJump.position().z, .001,
                      "Equal height and gravity produce the same fall, regardless of horizontal range");
                shortJump.advance(landing / 100, 1);
                longJump.advance(landing / 100, 1);
            }
            assertFalse(shortJump.isMoving());
            assertFalse(longJump.isMoving());
        }
        assertEquals(2.83, jump(1, 0, 1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, .01);
        assertTrue(jump(6, 0, 1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate < 3.5);
    }

    private static UnitMotion jump(int hexes, float elevation, double gravity) {
        var motion = new UnitMotion(START);
        motion.append(List.of(START, new BoardScene.Waypoint(point(hexes).coords(), elevation, 0)),
              EntityMovementType.MOVE_JUMP, 6, false, 0, 0, UnitMotion.DEFAULT_SPEED_GAIN_PER_HEX, gravity);
        return motion;
    }

    @Test
    void jumpLeanIsBoundedSmoothAndFollowsTheSharedClockAtEveryGravity() {
        for (double gravity : new double[] { 0, .5, 1, 2 }) {
            for (int hexes : new int[] { 0, 1, 6 }) {
                var motion = jump(hexes, hexes == 0 ? 1 : 0, gravity);
                double duration = motion.remainingSeconds();
                float previous = 0, peak = 0;
                for (int i = 0; i < 1000; i++) {
                    float tilt = motion.sample().jets().tilt();
                    assertTrue(tilt >= 0 && tilt <= UnitMotion.MAX_JUMP_TILT);
                    assertTrue(Math.abs(tilt - previous) < 1, "Flight attitude blends continuously");
                    peak = Math.max(peak, tilt);
                    previous = tilt;
                    if (i == 250) {
                        var once = jump(hexes, hexes == 0 ? 1 : 0, gravity);
                        once.advance(duration / 4, 1);
                        assertEquals(tilt, once.sample().jets().tilt(), .001, "Attitude cannot depend on frame rate");
                    }
                    motion.advance(duration / 1000, 1);
                }
                assertEquals(0, previous, .001, "Upright before touchdown");
                if (hexes == 0) { assertEquals(0, peak); } else { assertTrue(peak > 1); }
                assertNull(motion.sample().jets());
            }
        }
    }

    @Test
    void jumpAttitudeFollowsTheRenderedArcDespiteIntermediateFacingRecords() {
        var start = new BoardScene.Waypoint(new Coords(2, 5), 0, 0);
        var turn = new BoardScene.Waypoint(new Coords(3, 5), 0, 1);
        var facing = new BoardScene.Waypoint(turn.coords(), 0, 2);
        var end = new BoardScene.Waypoint(new Coords(4, 3), 0, 3);
        var direct = new UnitMotion(start);
        var recorded = new UnitMotion(start);
        direct.append(List.of(start, end), EntityMovementType.MOVE_JUMP, 6);
        recorded.append(List.of(start, turn, facing, end), EntityMovementType.MOVE_JUMP, 6);
        assertEquals(direct.remainingSeconds(), recorded.remainingSeconds(), .0001);
        double step = direct.remainingSeconds() / 100;
        for (int i = 0; i < 99; i++) {
            direct.advance(step, 1);
            recorded.advance(step, 1);
            assertEquals(direct.position(), recorded.position());
            assertEquals(direct.sample().heading(), recorded.sample().heading(), .001);
            assertEquals(direct.sample().forward(), recorded.sample().forward(), .001);
            assertEquals(direct.sample().lateral(), recorded.sample().lateral(), .001);
            assertEquals(direct.sample().steps(), recorded.sample().steps(), .001);
            assertEquals(direct.sample().jets().tilt(), recorded.sample().jets().tilt(), .001);
        }
    }

    @Test
    void gravityChangesHeightAndFallTimeAndZeroGravityStillLands() {
        double previousHeight = Double.POSITIVE_INFINITY, previousDuration = Double.POSITIVE_INFINITY;
        for (double gravity : new double[] { 0, .5, 1, 2 }) {
            var motion = jump(1, 0, gravity);
            double duration = motion.remainingSeconds();
            assertTrue(Double.isFinite(duration) && duration < 6);
            float highest = 0;
            var previous = motion.position().cpy();
            double firstSpeed = 0, peakSpeed = 0, lastSpeed = 0;
            for (int i = 0; i < 1000; i++) {
                motion.advance(duration / 1000, 1);
                highest = Math.max(highest, motion.position().z);
                lastSpeed = motion.position().dst(previous) / (duration / 1000);
                peakSpeed = Math.max(peakSpeed, lastSpeed);
                if (i == 0) { firstSpeed = lastSpeed; }
                previous.set(motion.position());
            }
            assertEquals(4 / Math.max(.25, gravity), highest / BoardGeometry.LEVEL, .001);
            assertTrue(highest < previousHeight && duration < previousDuration);
            assertTrue(firstSpeed < peakSpeed * .01 && lastSpeed < peakSpeed * .01,
                  "Powered launch and landing ease into and out of the ballistic flight");
            assertFalse(motion.isMoving());
            assertEquals(BoardGeometry.center(point(1).coords(), 0), motion.position());
            assertNull(motion.sample().jets());
            previousHeight = highest;
            previousDuration = duration;
        }
    }

    @Test
    void gravityAwareJumpsClearRaisedWaypointsAndLandAboveOrBelowTheirDeparture() {
        for (double gravity : new double[] { 0, .5, 1, 2, 5 }) {
            for (float elevation : new float[] { -4, 4 }) {
                var destination = new BoardScene.Waypoint(point(6).coords(), elevation, 0);
                var obstacle = new BoardScene.Waypoint(point(1).coords(), 5, 0);
                var path = List.of(START, obstacle, point(2), point(3), point(4), point(5), destination);
                var motion = new UnitMotion(START);
                motion.append(path, EntityMovementType.MOVE_JUMP, 6, false, 0, 0, .03, gravity);
                var compact = new UnitMotion(START);
                compact.append(List.of(START, obstacle, destination), EntityMovementType.MOVE_JUMP, 6, false, 0, 0, .03, gravity);
                assertEquals(compact.remainingSeconds(), motion.remainingSeconds(), .001, "Clearance uses traveled distance, not waypoint count");
                double duration = motion.remainingSeconds();
                double nearest = Double.POSITIVE_INFINITY, height = 0;
                for (int frame = 0; frame < 2000; frame++) {
                    motion.advance(duration / 2000, 1);
                    double distance = Math.abs(motion.position().y - BoardGeometry.centerY(obstacle.coords()));
                    if (distance < nearest) { nearest = distance; height = motion.position().z; }
                    assertTrue(Float.isFinite(motion.position().z));
                }
                assertTrue(height >= 5 * BoardGeometry.LEVEL - .1);
                assertFalse(motion.isMoving());
                assertEquals(BoardGeometry.center(destination.coords(), elevation), motion.position());
            }
        }
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
        motion.append(List.of(START, point(3), point(10)), EntityMovementType.MOVE_WALK, 0, false, 4);
        motion.advance(1.2, 1);
        assertEquals(BoardGeometry.center(point(3).coords(), 0).y, motion.position().y, .001f);
        motion.advance(.02, 1);
        assertEquals(BoardGeometry.HEIGHT * .1f, Math.abs(motion.position().y - BoardGeometry.center(point(3).coords(), 0).y), .01f);
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
