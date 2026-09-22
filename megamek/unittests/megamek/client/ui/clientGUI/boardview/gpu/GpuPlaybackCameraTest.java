/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GpuPlaybackCameraTest {
    private static final BoardView.CenterRequest INITIAL_CENTER = new BoardView.CenterRequest(1, new Coords(0, 0));

    @BeforeAll
    static void loadMathNatives() {
        GdxNativesLoader.load();
    }

    @Test
    void initialFitIgnoresOldClassicCenterRequestsButNewRequestsStillWork() {
        var unit = UnitPlaybackTest.unit(1, 0);
        var view = view(true);
        try {
            BoardScene scene = scene(unit.id(), unit);
            view.updateCameraFocus(scene, INITIAL_CENTER);
            for (var tile : scene.tiles()) {
                for (int corner = 0; corner < 6; corner++) {
                    var point = view.boardCamera.camera.project(BoardGeometry.corner(tile.coords(), tile.elevation(), corner),
                          0, 0, 800, 600);
                    assertTrue(point.x > 0 && point.x < 800 && point.y > 0 && point.y < 600,
                          "The initial view must fit the map even if the classic board had centered a unit");
                }
            }
            view.updateCameraFocus(scene, new BoardView.CenterRequest(2, unit.location().coords()));
            assertTrue(view.boardCamera.isFraming());
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertFocus(view, 0);
        } finally {
            view.dispose();
        }
    }

    @Test
    void unitNavigationUsesTheSameTransitionBeforeAndDuringAnActingTurnWithoutRepeatClickSnaps() {
        var first = UnitPlaybackTest.unit(1, 0);
        var next = UnitPlaybackTest.unit(3, 11);
        for (boolean actingTurn : List.of(false, true)) {
            var view = view(true);
            try {
                view.updateCameraFocus(scene(actingTurn ? first.id() : -1, first, next), INITIAL_CENTER);
                view.boardCamera.center(BoardGeometry.center(first.location().coords(), 0));
                var before = view.boardCamera.focus.cpy();
                var latest = scene(actingTurn ? next.id() : -1, first, next);
                view.updateCameraFocus(latest, new BoardView.CenterRequest(2, next.location().coords(), next.id()));
                assertEquals(before, view.boardCamera.focus, "Sidebar navigation must never teleport, even without an acting unit");
                assertTrue(view.boardCamera.isFraming());
                view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 4);
                var halfway = view.boardCamera.focus.cpy();
                view.updateCameraFocus(latest, new BoardView.CenterRequest(3, next.location().coords(), next.id()));
                assertEquals(halfway, view.boardCamera.focus, "A repeated click must not snap an unfinished transition");
                view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS * .75f);
                assertFalse(view.boardCamera.isFraming(), "Repeated clicks must not restart the animation deadline");
                var settled = view.boardCamera.focus.cpy();
                view.updateCameraFocus(latest, new BoardView.CenterRequest(4, next.location().coords(), next.id()));
                assertFalse(view.boardCamera.isFraming());
                assertTrue(settled.epsilonEquals(view.boardCamera.focus, .001f));
                BoardCameraFramingTest.assertVisible(view.boardCamera, 800, next);

                view.boardCamera.pan(300, 300);
                var panned = view.boardCamera.focus.cpy();
                view.updateCameraFocus(latest, new BoardView.CenterRequest(5, next.location().coords(), next.id()));
                assertEquals(panned, view.boardCamera.focus);
                assertTrue(view.boardCamera.isFraming(), "Clicking after manual panning must still navigate to the unit");
                view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                assertTrue(settled.epsilonEquals(view.boardCamera.focus, .001f));
            } finally {
                view.dispose();
            }
        }
    }

    @Test
    void unitIdentityDeterminesFramingInAStackedHexAndMissingUnitsAreNotInferredFromCoordinates() {
        var ground = UnitPlaybackTest.unit(1, 2);
        var raised = new BoardScene.Unit(2, -1, "Raised unit", new BoardScene.Waypoint(ground.location().coords(), 8, 0),
              null, false, null, 5, true);
        var view = view(true);
        try {
            var scene = scene(-1, ground, raised);
            view.updateCameraFocus(scene, INITIAL_CENTER);
            view.updateCameraFocus(scene, new BoardView.CenterRequest(2, raised.location().coords(), raised.id()));
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertEquals(8 * BoardGeometry.LEVEL, view.boardCamera.focus.z, .001f);
            BoardCameraFramingTest.assertVisible(view.boardCamera, 800, raised);
            var settled = view.boardCamera.focus.cpy();
            view.updateCameraFocus(scene, new BoardView.CenterRequest(3, ground.location().coords(), 999));
            assertEquals(settled, view.boardCamera.focus);
            assertFalse(view.boardCamera.isFraming());
            view.updateCameraFocus(scene(999, ground, raised),
                  new BoardView.CenterRequest(4, ground.location().coords(), ground.id()));
            assertTrue(view.boardCamera.isFraming(), "An actor absent from this board must not swallow valid navigation");
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertEquals(0, view.boardCamera.focus.z, .001f);
        } finally {
            view.dispose();
        }
    }

    @Test
    void unitNavigationDuringMovementWaitsForPlaybackEvenAtTheMovesStartingHex() throws Exception {
        var move = movement(1, 0, 2);
        var other = new BoardScene.Unit(2, -1, "Other unit", new BoardScene.Waypoint(new Coords(0, 0), 3, 0),
              null, false, null, 2, true);
        var view = view(true);
        try {
            var playback = playback(view);
            view.updateCameraFocus(scene(-1, UnitPlaybackTest.unit(1, 0), other), INITIAL_CENTER);
            var latest = scene(-1, move.unit(), other);
            playback.accept(List.of(move), latest, ignored -> false);
            playback.advance(0, UnitMotion.Speed.NORMAL, state -> view.preparePlaybackCamera(state, latest));
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            var duringMove = view.boardCamera.focus.cpy();
            var request = new BoardView.CenterRequest(2, other.location().coords(), other.id());
            view.updateCameraFocus(playback.present(latest), request);
            assertEquals(duringMove, view.boardCamera.focus, "Navigation cannot interrupt movement playback");
            playback.finish();
            view.updateCameraFocus(latest, request);
            assertTrue(view.boardCamera.isFraming(), "An explicit unit request is not a legacy move-start auto-center");
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertEquals(3 * BoardGeometry.LEVEL, view.boardCamera.focus.z, .001f);
        } finally {
            view.dispose();
        }
    }

    @Test
    void instantSelectionSnapsAndSwitchingSpeedFinishesAnExistingTransition() throws Exception {
        var first = UnitPlaybackTest.unit(1, 0);
        var next = UnitPlaybackTest.unit(3, 11);
        for (boolean alreadyAnimating : List.of(false, true)) {
            var view = view(false);
            try {
                view.updateCameraFocus(scene(first.id(), first, next), INITIAL_CENTER);
                view.boardCamera.center(BoardGeometry.center(first.location().coords(), 0));
                var latest = scene(next.id(), first, next);
                if (alreadyAnimating) {
                    view.updateCameraFocus(latest, INITIAL_CENTER);
                    view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 4);
                    assertTrue(view.boardCamera.isFraming());
                }
                instant(view);
                view.updateCameraFocus(latest, INITIAL_CENTER);
                assertFalse(view.boardCamera.isFraming());
                BoardCameraFramingTest.assertVisible(view.boardCamera, 800, next);
                var settled = view.boardCamera.focus.cpy();
                view.updateCameraFocus(latest, INITIAL_CENTER);
                assertEquals(settled, view.boardCamera.focus);
            } finally {
                view.dispose();
            }
        }
    }

    @Test
    void instantQueueUsesOnlyTheFinalActionOrNewSelection() throws Exception {
        var selected = UnitPlaybackTest.unit(1, 0);
        var move = movement(2, 4, 7);
        var target = UnitPlaybackTest.unit(3, 11);
        var shot = UnitPlaybackTest.attack(move.unit(), target, ResolvedAttack.Kind.SHOT, true);
        for (boolean selectTarget : List.of(false, true)) {
            var view = view(true);
            var expected = new BoardCamera();
            try {
                var initial = scene(selected.id(), selected, move.unit(), target);
                view.updateCameraFocus(initial, INITIAL_CENTER);
                var before = view.boardCamera.focus.cpy();
                expected.resize(800, 600);
                expected.setIsometric(true);
                expected.camera.zoom = view.boardCamera.camera.zoom;
                expected.center(before);
                var latest = scene(selectTarget ? target.id() : selected.id(), selected, move.unit(), target);
                var playback = playback(view);
                playback.accept(List.of(move, shot, new BoardScene.SceneUpdate(latest)), latest, ignored -> false);
                instant(view);
                var last = playback.lastAction();
                assertEquals(shot, last);
                playback.advance(0, UnitMotion.Speed.INSTANT, state -> {
                    throw new AssertionError("Instant playback must not frame each queued action");
                });
                assertEquals(before, view.boardCamera.focus);
                view.updateCameraFocus(playback.present(latest), INITIAL_CENTER, last);
                if (selectTarget) {
                    expected.frameSelection(target, 800);
                } else {
                    expected.frameAttacks(List.of(new UnitAttack(shot)), 800);
                }
                expected.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                assertFalse(playback.busy());
                assertFalse(view.boardCamera.isFraming());
                assertTrue(expected.focus.epsilonEquals(view.boardCamera.focus, .001f));
                assertEquals(expected.camera.zoom, view.boardCamera.camera.zoom, .001f);
                assertEquals(expected.azimuth(), view.boardCamera.azimuth(), .001f);
                var settled = view.boardCamera.focus.cpy();
                view.updateCameraFocus(latest, INITIAL_CENTER);
                assertEquals(settled, view.boardCamera.focus, "Idle frames must not return to the old selection afterward");
            } finally {
                view.dispose();
            }
        }
    }

    @Test
    void switchingToInstantDuringMoveFramingSettlesAtTheFinalQueuedArrival() throws Exception {
        var first = movement(1, 0, 5);
        var last = movement(2, 7, 11);
        var view = view(false);
        try {
            var initial = scene(1, UnitPlaybackTest.unit(1, 0), UnitPlaybackTest.unit(2, 7));
            view.updateCameraFocus(initial, INITIAL_CENTER);
            var latest = scene(1, first.unit(), last.unit());
            var playback = playback(view);
            playback.accept(List.of(first, last), latest, ignored -> false);
            playback.advance(0, UnitMotion.Speed.NORMAL, state -> view.preparePlaybackCamera(state, latest));
            view.updateCameraFocus(playback.present(latest), INITIAL_CENTER);
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 4);
            instant(view);
            var finalAction = playback.lastAction();
            playback.advance(0, UnitMotion.Speed.INSTANT);
            var request = new BoardView.CenterRequest(2, last.path().getFirst().coords());
            view.updateCameraFocus(playback.present(latest), request, finalAction);
            assertFalse(view.boardCamera.isFraming());
            BoardCameraFramingTest.assertVisible(view.boardCamera, 800, last.unit());
            var settled = view.boardCamera.focus.cpy();
            view.updateCameraFocus(latest, request);
            assertEquals(settled, view.boardCamera.focus, "A legacy move-start request must not undo the final arrival view");
        } finally {
            view.dispose();
        }
    }

    private static void instant(GpuBattleView view) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField("playbackSpeed");
        field.setAccessible(true);
        field.set(view, UnitMotion.Speed.INSTANT);
    }

    @Test
    void classicMovementAutoCenterIsConsumedWithoutReframingAVisibleRouteOnArrival() throws Exception {
        var move = movement(1, 0, 2);
        var view = view(false);
        try {
            var playback = playback(view);
            view.updateCameraFocus(scene(1, UnitPlaybackTest.unit(1, 0)), INITIAL_CENTER);
            view.boardCamera.pan(-30, 10);
            var before = view.boardCamera.focus.cpy();
            var latest = scene(1, move.unit());
            var autoCenter = new BoardView.CenterRequest(2, move.path().getFirst().coords());
            playback.accept(List.of(move), latest, ignored -> false);
            playback.advance(0, UnitMotion.Speed.NORMAL, state -> view.preparePlaybackCamera(state, latest));
            view.updateCameraFocus(playback.present(latest), autoCenter);
            assertFalse(view.boardCamera.isFraming());
            assertEquals(before, view.boardCamera.focus);
            playback.advance(playback.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate
                  + UnitPlayback.COMPLETION_HOLD_SECONDS, UnitMotion.Speed.NORMAL);
            view.updateCameraFocus(playback.present(latest), autoCenter);
            assertFalse(playback.busy());
            assertFalse(view.boardCamera.isFraming());
            assertEquals(before, view.boardCamera.focus, "The legacy start request must not cause an arrival snap");
        } finally {
            view.dispose();
        }
    }

    @Test
    void framesEachQueuedRouteBeforeTravelAndKeepsTheCameraStillDuringTravelAndHolds() throws Exception {
        var first = movement(1, 0, 2);
        var enemy = movement(2, 7, 9);
        var next = UnitPlaybackTest.unit(3, 11);
        for (boolean isometric : List.of(false, true)) {
            for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE,
                  UnitMotion.Speed.QUADRUPLE)) {
                var view = view(isometric);
                try {
                    var playback = playback(view);
                    var initial = scene(1, UnitPlaybackTest.unit(1, 0), UnitPlaybackTest.unit(2, 7), next);
                    view.updateCameraFocus(initial, INITIAL_CENTER);
                    view.boardCamera.center(BoardGeometry.center(first.path().getFirst().coords(), 0));
                    var latest = scene(next.id(), first.unit(), enemy.unit(), next);
                    playback.accept(List.of(first, enemy), latest, ignored -> false);
                    playback.advance(0, speed, state -> view.preparePlaybackCamera(state, latest));
                    focus(view, playback, latest);
                    view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                    var firstFrame = view.boardCamera.focus.cpy();

                    var motion = playback.motions.get(1);
                    playback.advance(motion.remainingSeconds() / (2 * speed.rate), speed);
                    focus(view, playback, latest);
                    assertEquals(firstFrame, view.boardCamera.focus, "A framed route must not chase the moving unit");
                    playback.advance(motion.remainingSeconds() / speed.rate, speed);
                    focus(view, playback, latest);
                    assertEquals(firstFrame, view.boardCamera.focus);
                    playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS - .001, speed);
                    focus(view, playback, latest);
                    assertEquals(firstFrame, view.boardCamera.focus);

                    playback.advance(.001, speed, state -> view.preparePlaybackCamera(state, latest));
                    focus(view, playback, latest);
                    assertEquals(enemy.entityId(), playback.activeEntityId(), "The enemy action takes focus before the live selection");
                    motion = playback.motions.get(enemy.entityId());
                    assertEquals(BoardGeometry.center(enemy.path().getFirst().coords(), 0), motion.position());
                    assertTrue(view.boardCamera.isFraming());
                    playback.advance(10, speed, state -> view.preparePlaybackCamera(state, latest));
                    assertEquals(BoardGeometry.center(enemy.path().getFirst().coords(), 0), motion.position(),
                          "Even a large fast frame must wait for the route to be visible");
                    view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                    var enemyFrame = view.boardCamera.focus.cpy();
                    playback.advance(motion.remainingSeconds() / speed.rate, speed);
                    focus(view, playback, latest);
                    assertEquals(enemyFrame, view.boardCamera.focus);
                    playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS - .001, speed);
                    focus(view, playback, latest);
                    assertEquals(enemyFrame, view.boardCamera.focus);
                    playback.advance(.001, speed);
                    focus(view, playback, latest);
                    assertFalse(playback.busy());
                    view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                    BoardCameraFramingTest.assertVisible(view.boardCamera, 800, next);

                    view.boardCamera.pan(40, 20);
                    var panned = view.boardCamera.focus.cpy();
                    focus(view, playback, latest);
                    assertEquals(panned, view.boardCamera.focus, "Finishing playback must not lock the idle camera to selection");
                } finally {
                    view.dispose();
                }
            }
        }
    }

    @Test
    void firingKeepsBothParticipantsInFrameThroughAPausedHoldThenUsesTheLatestSelection() throws Exception {
        var attacker = UnitPlaybackTest.unit(2, 7);
        var target = UnitPlaybackTest.unit(1, 0);
        var next = UnitPlaybackTest.unit(3, 11);
        var shot = UnitPlaybackTest.attack(attacker, target, ResolvedAttack.Kind.SHOT, true);
        for (boolean isometric : List.of(false, true)) {
            var view = view(isometric);
            try {
                var playback = playback(view);
                var initial = scene(target.id(), target, attacker, next);
                view.updateCameraFocus(initial, INITIAL_CENTER);
                playback.accept(List.of(shot), initial, ignored -> false);
                playback.advance(0, UnitMotion.Speed.NORMAL);
                focus(view, playback, initial);
                view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                BoardCameraFramingTest.assertVisible(view.boardCamera, 800, attacker);
                BoardCameraFramingTest.assertVisible(view.boardCamera, 800, target);
                var framed = view.boardCamera.focus.cpy();
                playback.advance(playback.attack().duration / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
                var latest = scene(next.id(), target, attacker, next);
                focus(view, playback, latest);
                assertEquals(framed, view.boardCamera.focus);
                playback.togglePaused();
                playback.advance(10, UnitMotion.Speed.NORMAL);
                focus(view, playback, latest);
                assertEquals(UnitPlayback.COMPLETION_HOLD_SECONDS, playback.holdSeconds(), 1e-6);
                assertEquals(framed, view.boardCamera.focus);
                playback.togglePaused();
                playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS / 2, UnitMotion.Speed.NORMAL);
                focus(view, playback, latest);
                assertEquals(framed, view.boardCamera.focus);
                // Another selection during the hold supersedes the earlier one, even with a stale center request.
                latest = scene(target.id(), target, attacker, next);
                playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS / 2, UnitMotion.Speed.NORMAL);
                view.updateCameraFocus(playback.present(latest), new BoardView.CenterRequest(2, next.location().coords()));
                assertFalse(playback.busy());
                view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                BoardCameraFramingTest.assertVisible(view.boardCamera, 800, target);
            } finally {
                view.dispose();
            }
        }
    }

    @Test
    void skippingMovementDoesNotStartAnotherCameraMoveWhenSelectionHasNotChanged() throws Exception {
        var selected = UnitPlaybackTest.unit(1, 0);
        var enemy = movement(2, 7, 9);
        for (Consumer<UnitPlayback> skip : List.<Consumer<UnitPlayback>>of(
              playback -> playback.advance(0, UnitMotion.Speed.INSTANT), UnitPlayback::finish)) {
            var view = view(true);
            try {
                var playback = playback(view);
                var latest = scene(selected.id(), selected, enemy.unit());
                view.updateCameraFocus(latest, INITIAL_CENTER);
                playback.accept(List.of(enemy), latest, ignored -> false);
                playback.advance(.1, UnitMotion.Speed.NORMAL);
                focus(view, playback, latest);
                view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
                var framed = view.boardCamera.focus.cpy();
                skip.accept(playback);
                focus(view, playback, latest);
                assertFalse(view.boardCamera.isFraming());
                assertEquals(framed, view.boardCamera.focus);
            } finally {
                view.dispose();
            }
        }
    }

    @Test
    void idleSelectionChangesAnimateOnceAndExplicitCenterRequestsStillTakeControl() {
        var first = UnitPlaybackTest.unit(1, 0);
        var next = UnitPlaybackTest.unit(3, 11);
        var view = view(false);
        try {
            view.updateCameraFocus(scene(first.id(), first, next), INITIAL_CENTER);
            view.boardCamera.center(BoardGeometry.center(first.location().coords(), 0));
            var before = view.boardCamera.focus.cpy();
            var latest = scene(next.id(), first, next);
            view.updateCameraFocus(latest, INITIAL_CENTER);
            assertEquals(before, view.boardCamera.focus, "Selection must not snap");
            assertTrue(view.boardCamera.isFraming());
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 2);
            view.updateCameraFocus(latest, INITIAL_CENTER);
            assertTrue(view.boardCamera.isFraming(), "Unchanged snapshots must not cancel the selection transition");
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS / 2);
            assertFalse(view.boardCamera.isFraming());
            BoardCameraFramingTest.assertVisible(view.boardCamera, 800, next);
            var request = new BoardView.CenterRequest(2, new Coords(0, 5));
            view.updateCameraFocus(latest, request);
            var navigating = view.boardCamera.focus.cpy();
            assertTrue(view.boardCamera.isFraming());
            view.updateCameraFocus(latest, new BoardView.CenterRequest(3, request.coords()));
            assertEquals(navigating, view.boardCamera.focus);
            view.boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS);
            assertFocus(view, 5);
            view.updateCameraFocus(scene(-1, first, next), new BoardView.CenterRequest(3, request.coords()));
            assertFocus(view, 5);
            view.updateCameraFocus(scene(99, first, next), new BoardView.CenterRequest(3, request.coords()));
            assertFocus(view, 5);
        } finally {
            view.dispose();
        }
    }

    private static GpuBattleView view(boolean isometric) {
        var view = new GpuBattleView(mock(GpuBoardSource.class));
        view.boardCamera.resize(800, 600);
        view.boardCamera.setIsometric(isometric);
        return view;
    }

    private static UnitPlayback playback(GpuBattleView view) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField("playback");
        field.setAccessible(true);
        return (UnitPlayback) field.get(view);
    }

    private static void focus(GpuBattleView view, UnitPlayback playback, BoardScene scene) {
        view.updateCameraFocus(playback.present(scene), INITIAL_CENTER);
    }

    private static void assertFocus(GpuBattleView view, int row) {
        assertEquals(BoardGeometry.center(new Coords(0, row), 0), view.boardCamera.focus);
    }

    private static BoardScene.Movement movement(int id, int from, int to) {
        var start = UnitPlaybackTest.unit(id, from);
        var end = UnitPlaybackTest.unit(id, to);
        return new BoardScene.Movement(id, 0, List.of(start.location(), end.location()), EntityMovementType.MOVE_WALK, 0, 2, end);
    }

    private static BoardScene scene(int selectedId, BoardScene.Unit... units) {
        var tiles = IntStream.range(0, 12).mapToObj(row -> new BoardScene.Tile(new Coords(0, row), 0, -1, false, 0,
              BoardScene.Surface.GRASS, null, null, null, List.of(), List.of())).toList();
        return new BoardScene(0, 1, 12, tiles, List.of(units), List.of(), selectedId, "Movement", List.of());
    }
}
