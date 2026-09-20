/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BoardCameraFramingTest {
    @BeforeAll
    static void loadMathNatives() {
        GdxNativesLoader.load();
    }

    @Test
    void manualOrbitTiltPanAndPointerZoomKeepTheBoardPivotAfterEveryAutomaticFramingMode() {
        var attacker = unit(1, 18, 14, 3, 2);
        var target = unit(2, 26, 17, 3, 2);
        var move = new BoardScene.Movement(1, 0, List.of(attacker.location(), target.location()),
              EntityMovementType.MOVE_WALK, 0, 4, attacker);
        for (int mode = 0; mode < 3; mode++) {
            var camera = camera(65);
            if (mode == 0) {
                camera.frameAttacks(List.of(shot(attacker, target)), 420);
            } else if (mode == 1) {
                camera.frameSelection(attacker, 420);
            } else {
                camera.frameMovement(move, motion(move), UnitPlaybackTest.scene(attacker), 420);
            }
            camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertEquals(3 * BoardGeometry.LEVEL, camera.focus.z, .002f,
                  "A screen-space fit must not lift the manual orbit pivot away from the board support plane");
            var pivot = camera.focus.cpy();
            assertPivotScreen(camera, pivot, 420);
            camera.orbit(70, 10);
            assertPivotScreen(camera, pivot, 420);
            camera.tilt(-15);
            assertPivotScreen(camera, pivot, 420);
            camera.rotateStep(-1);
            camera.advance(BoardCamera.ROTATION_SECONDS);
            assertPivotScreen(camera, pivot, 420);
            var before = project(camera, pivot);
            camera.pan(45, -25);
            var after = project(camera, pivot);
            assertEquals(45, after.x - before.x, .02f, "Grounded panning follows the horizontal drag");
            assertEquals(25, after.y - before.y, .02f, "Grounded panning follows the vertical drag");
            assertEquals(pivot.z, camera.focus.z);
            var anchored = camera.focus.cpy().add(15, 20, 0);
            before = project(camera, anchored);
            camera.zoomAt(.75f, before.x, before.y);
            after = project(camera, anchored);
            assertEquals(before.x, after.x, .02f, "Pointer zoom must account for the side-panel offset");
            assertEquals(before.y, after.y, .02f);
        }
    }

    @Test
    void changingThePanelWidthMovesTheOrbitPivotWithoutMovingTheVisibleBoard() {
        var camera = camera(55);
        var point = camera.focus.cpy().add(30, 25, 0);
        var before = project(camera, point);
        camera.viewableWidth(400);
        var after = project(camera, point);
        assertEquals(before.x, after.x, .02f);
        assertEquals(before.y, after.y, .02f);
        assertFalse(camera.isFraming());
        assertPivotScreen(camera, camera.focus.cpy(), 400);
        camera.viewableWidth(1200);
        after = project(camera, point);
        assertEquals(before.x, after.x, .02f);
        assertEquals(before.y, after.y, .02f);
    }

    private static Vector3 project(BoardCamera camera, Vector3 point) {
        return camera.camera.project(point.cpy(), 0, 0, camera.camera.viewportWidth, camera.camera.viewportHeight);
    }

    private static void assertPivotScreen(BoardCamera camera, Vector3 pivot, float width) {
        assertEquals(pivot, camera.focus, "Manual orbit and tilt must keep the same world-space pivot");
        var screen = project(camera, pivot);
        assertEquals(width / 2, screen.x, .02f, "Orbit around the clear board area, not behind the side panel");
        assertEquals(camera.camera.viewportHeight / 2, screen.y, .02f);
    }

    @Test
    void visibleTopViewSelectionsAndVolleysNeverMoveOrZoomIn() {
        var attacker = unit(1, 4, 4, 0, 2);
        var target = unit(2, 6, 6, 1, 2);
        for (float tilt : new float[] { 0, 15, BoardCamera.ATTACK_TOP_VIEW_TILT_DEGREES }) {
            var camera = new BoardCamera();
            camera.resize(1200, 800);
            camera.orbit(21, tilt);
            camera.center(BoardGeometry.center(attacker.location().coords(), 0));
            camera.zoom(3);
            camera.viewableWidth(900);
            var pivot = camera.focus.cpy();
            assertVisible(camera, 900, attacker);
            assertVisible(camera, 900, target);
            camera.frameSelection(target, 900);
            assertFalse(camera.isFraming(), "Selecting an already visible unit must leave an overhead camera alone");
            camera.frameAttacks(List.of(shot(attacker, target)), 900);
            camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertFalse(camera.isFraming());
            assertEquals(pivot, camera.focus);
            assertEquals(3, camera.camera.zoom);
            assertEquals(tilt, camera.tilt());
            assertEquals(21, camera.azimuth());
        }
    }

    @Test
    void movementAlreadyInViewDoesNotReframeOrHoldItsClock() {
        var move = movement(List.of(new Coords(0, 5), new Coords(0, 6), new Coords(0, 7)),
              EntityMovementType.MOVE_WALK, 0, 1);
        var motion = motion(move);
        var camera = new BoardCamera();
        camera.resize(1200, 800);
        camera.setIsometric(true);
        camera.center(BoardGeometry.center(new Coords(0, 6), 0));
        camera.zoom(2);
        camera.viewableWidth(900);
        var pivot = camera.focus.cpy();
        double duration = motion.remainingSeconds();
        var scene = UnitPlaybackTest.scene(move.unit());
        camera.frameMovement(move, motion, scene, 900);
        assertFalse(camera.isFraming());
        assertEquals(pivot, camera.focus);
        assertEquals(2, camera.camera.zoom);
        assertTrue(camera.isIsometric());
        assertEquals(duration, motion.remainingSeconds(), "Framing samples must never advance the movement clock");
        assertPathVisible(camera, 900, motion, move.unit(), scene);
    }

    @Test
    void offscreenDetourWithVisibleEndpointsOnlyPansAsFarAsNecessary() {
        var move = movement(List.of(new Coords(0, 5), new Coords(4, 6), new Coords(0, 7)),
              EntityMovementType.MOVE_WALK, 0, 1);
        var motion = motion(move);
        var camera = new BoardCamera();
        camera.resize(1200, 800);
        camera.center(BoardGeometry.center(new Coords(0, 6), 0));
        camera.viewableWidth(700);
        var pivot = camera.focus.cpy();
        for (var point : List.of(move.path().getFirst(), move.path().getLast())) {
            var screen = camera.camera.project(BoardGeometry.center(point.coords(), 0), 0, 0, 1200, 800);
            assertTrue(screen.x > 0 && screen.x < 700 && screen.y > 0 && screen.y < 800);
        }
        var scene = UnitPlaybackTest.scene(move.unit());
        camera.frameMovement(move, motion, scene, 700);
        assertTrue(camera.isFraming(), "The middle of the route is covered by the side panel");
        assertEquals(pivot, camera.focus);
        camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
        assertEquals(1, camera.camera.zoom, "A short offscreen route fits by panning alone");
        assertEquals(pivot.y, camera.focus.y, .001f, "Do not move an axis that already fits");
        assertEquals(0, camera.tilt());
        assertEquals(0, camera.azimuth());
        var rightmost = motion.framingPath(scene, move.unit()).stream()
              .flatMap(pose -> java.util.stream.IntStream.range(0, 6)
                    .mapToObj(corner -> camera.camera.project(pose.outlinePoint(move.unit().location().coords(), corner, 0),
                          0, 0, 1200, 800))).mapToDouble(point -> point.x).max().orElseThrow();
        assertEquals(700 - 64, rightmost, .02, "Pan to the padded edge, not the viewport center");
        assertPathVisible(camera, 700, motion, move.unit(), scene);
    }

    @Test
    void longRoutesAndTheActualLowGravityJumpArcFitWithoutChangingTheViewingAngle() {
        for (var move : List.of(
              movement(List.of(new Coords(0, 5), new Coords(24, 5)), EntityMovementType.MOVE_WALK, 0, 1),
              movement(List.of(new Coords(0, 5), new Coords(0, 6)), EntityMovementType.MOVE_JUMP, 12, .25f))) {
            var camera = new BoardCamera();
            camera.resize(1200, 800);
            camera.setIsometric(true);
            camera.center(BoardGeometry.center(new Coords(0, 5), 0));
            camera.zoom(.5f);
            float bearing = camera.azimuth(), tilt = camera.tilt();
            var motion = motion(move);
            var scene = UnitPlaybackTest.scene(move.unit());
            camera.frameMovement(move, motion, scene, 700);
            assertTrue(camera.isFraming());
            camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertTrue(camera.camera.zoom > .5f, "The route is larger than the available board area");
            assertEquals(bearing, camera.azimuth());
            assertEquals(tilt, camera.tilt());
            assertPathVisible(camera, 700, motion, move.unit(), scene);
        }
    }

    private static BoardScene.Movement movement(List<Coords> route, EntityMovementType type, int jumpMP, float gravity) {
        var path = route.stream().map(coords -> new BoardScene.Waypoint(coords, 0, 0)).toList();
        var end = new BoardScene.Unit(1, -1, "Mover", path.getLast(), null, false, null, 2, false);
        return new BoardScene.Movement(1, 0, path, type, jumpMP, 4, end, gravity);
    }

    private static UnitMotion motion(BoardScene.Movement move) {
        var motion = new UnitMotion(move.path().getFirst());
        motion.append(move.path(), move.type(), move.jumpMP(), false, move.movementMP(), 0,
              UnitMotion.DEFAULT_SPEED_GAIN_PER_HEX, move.gravity());
        return motion;
    }

    private static void assertPathVisible(BoardCamera camera, float width, UnitMotion motion, BoardScene.Unit unit,
          BoardScene scene) {
        double step = motion.remainingSeconds() / 256;
        for (int sample = 0; sample <= 256; sample++) {
            var pose = new UnitFootprint.Pose(unit, motion.surfacePosition(scene), motion.facing());
            for (var coords : unit.footprint()) {
                for (int corner = 0; corner < 6; corner++) {
                    for (float height : new float[] { 0, unit.height() * BoardGeometry.LEVEL }) {
                        var point = camera.camera.project(pose.outlinePoint(coords, corner, 0).add(0, 0, height),
                              0, 0, camera.camera.viewportWidth, camera.camera.viewportHeight);
                        assertTrue(point.x >= 0 && point.x <= width && point.y >= 0 && point.y <= camera.camera.viewportHeight,
                              () -> "Moving unit outside the usable board area: " + point);
                    }
                }
            }
            motion.advance(step, 1);
        }
    }

    @Test
    void topViewsOnlyPanAndZoomIncludingTheThresholdAndNarrowPanelSpace() {
        var attacker = unit(1, 1, 1, 0, 2);
        var targets = List.of(unit(2, 15, 2, 0, 2), unit(3, 3, 18, 7, 5));
        var attacks = targets.stream().map(target -> shot(attacker, target)).toList();
        for (float tilt : new float[] { 0, 15, BoardCamera.ATTACK_TOP_VIEW_TILT_DEGREES }) {
            for (float width : new float[] { 1200, 350 }) {
                var camera = camera(tilt);
                var initialFocus = camera.focus.cpy();
                float initialZoom = camera.camera.zoom;
                float initialBearing = camera.azimuth();
                camera.frameAttacks(attacks, width);
                for (int step = 0; step < 2; step++) {
                    camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 2);
                    assertEquals(tilt, camera.tilt(), .0001f);
                    assertEquals(initialBearing, camera.azimuth(), .0001f, "Top views must not rotate, even partway through");
                }
                assertFalse(camera.isFraming());
                assertNotEquals(initialFocus, camera.focus);
                assertNotEquals(initialZoom, camera.camera.zoom);
                assertVisible(camera, width, attacker);
                targets.forEach(target -> assertVisible(camera, width, target));
            }
        }
    }

    @Test
    void obliqueViewsOrientAndFitRaisedMultiHexUnitsAndHexTargetsAtBothDisplayScales() {
        var attacker = unit(1, 1, 2, -2, 2);
        var large = new BoardScene.Unit(2, -1, "Large target", new BoardScene.Waypoint(new Coords(17, 13), 8, 0),
              null, false, null, 6, true, null, 0, List.of(new Coords(17, 13), new Coords(18, 13), new Coords(18, 14)));
        var hex = unit(3, 2, 18, 4, 0);
        var hexEvent = UnitPlaybackTest.attack(attacker, hex, ResolvedAttack.Kind.SHOT, true);
        var attacks = List.of(shot(attacker, large),
              new UnitAttack(new BoardScene.Combat(hexEvent.result(), attacker, null, hex.location())));
        for (float tilt : new float[] { BoardCamera.ATTACK_TOP_VIEW_TILT_DEGREES + 1, BoardCamera.MAX_TILT }) {
            for (float scale : new float[] { 1, 2 }) {
                for (float width : new float[] { 1200, 350 }) {
                    var camera = camera(tilt);
                    camera.resize(1200, 800, null, scale);
                    float initialBearing = camera.azimuth();
                    camera.frameAttacks(attacks, width);
                    camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                    assertNotEquals(initialBearing, camera.azimuth(), "Orbit is allowed above the top-view threshold");
                    assertTrue(camera.tilt() <= tilt);
                    assertVisible(camera, width, attacker);
                    assertVisible(camera, width, large);
                    assertVisible(camera, width, hex);
                }
            }
        }
    }

    @Test
    void lateTargetsShareTheOriginalDeadlineAndPanelResizingRefitsWithoutAnotherHold() {
        var attacker = unit(1, 0, 0, 0, 2);
        var first = shot(attacker, unit(2, 2, 2, 0, 2));
        var late = shot(attacker, unit(3, 28, 22, 5, 3));
        var camera = camera(65);
        camera.frameAttacks(List.of(first), 1200);
        camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 2);
        var halfway = camera.focus.cpy();
        camera.frameAttacks(List.of(first, late), 1200);
        assertEquals(halfway, camera.focus, "Receiving another target must not snap an unfinished move");
        for (int step = 0; step < 2; step++) {
            camera.frameAttacks(List.of(first, late), 1200);
            camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 4);
        }
        assertFalse(camera.isFraming(), "Additional targets and repeated frames must not extend the deadline");
        assertVisible(camera, 1200, late.event.target());
        camera.frameAttacks(List.of(first, late), 320);
        assertFalse(camera.isFraming(), "A panel opened after framing must not stall an ongoing volley");
        assertVisible(camera, 320, attacker);
        assertVisible(camera, 320, first.event.target());
        assertVisible(camera, 320, late.event.target());
    }

    @Test
    void manualCameraInputTakesControlUntilTheNextVolley() {
        var attacker = unit(1, 0, 0, 0, 2);
        var attacks = List.of(shot(attacker, unit(2, 10, 10, 0, 2)));
        var camera = camera(60);
        camera.frameAttacks(attacks, 1200);
        camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 2);
        camera.orbit(10, 2);
        camera.pan(40, 30);
        camera.zoom(.8f);
        var manual = camera.focus.cpy();
        float zoom = camera.camera.zoom, tilt = camera.tilt(), bearing = camera.azimuth();
        camera.frameAttacks(attacks, 1200);
        camera.advance(10);
        assertFalse(camera.isFraming());
        assertEquals(manual, camera.focus);
        assertEquals(zoom, camera.camera.zoom);
        assertEquals(tilt, camera.tilt());
        assertEquals(bearing, camera.azimuth());
        camera.clearPlaybackFrame();
        camera.frameAttacks(attacks, 1200);
        assertTrue(camera.isFraming());
    }

    @Test
    void sameHexTargetsAndMissingFootprintsStillHaveFiniteFraming() {
        var attacker = unit(1, 3, 3, 0, 2);
        var target = new BoardScene.Unit(2, -1, "Target", attacker.location(), null, false, null, 0, false,
              null, 0, List.of());
        var camera = camera(0);
        camera.frameAttacks(List.of(shot(attacker, target)), 350);
        camera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
        assertTrue(Float.isFinite(camera.camera.zoom));
        assertVisible(camera, 350, attacker);
    }

    private static BoardCamera camera(float tilt) {
        var camera = new BoardCamera();
        camera.resize(1200, 800);
        camera.setIsometric(false);
        camera.orbit(73, tilt);
        camera.center(new Vector3(-1000, 1500, 0));
        camera.zoom(.1f);
        return camera;
    }

    private static BoardScene.Unit unit(int id, int x, int y, float elevation, int height) {
        return new BoardScene.Unit(id, -1, "Unit " + id, new BoardScene.Waypoint(new Coords(x, y), elevation, 0),
              null, false, null, height, elevation > 0);
    }

    private static UnitAttack shot(BoardScene.Unit attacker, BoardScene.Unit target) {
        return new UnitAttack(UnitPlaybackTest.attack(attacker, target, ResolvedAttack.Kind.SHOT, true));
    }

    static void assertVisible(BoardCamera camera, float width, BoardScene.Unit unit) {
        for (var coords : unit.footprint()) {
            for (float elevation : new float[] { unit.location().elevation(), unit.location().elevation() + unit.height() }) {
                for (int corner = 0; corner < 6; corner++) {
                    var point = camera.camera.project(BoardGeometry.corner(coords, elevation, corner),
                          0, 0, camera.camera.viewportWidth, camera.camera.viewportHeight);
                    assertTrue(point.x > 0 && point.x < width && point.y > 0 && point.y < camera.camera.viewportHeight,
                          () -> unit.name() + " outside the usable board area: " + point + ", available width " + width);
                }
            }
        }
    }
}
