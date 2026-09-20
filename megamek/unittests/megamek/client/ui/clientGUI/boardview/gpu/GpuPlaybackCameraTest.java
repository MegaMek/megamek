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
            assertFocus(view, 5);
            view.updateCameraFocus(scene(-1, first, next), request);
            assertFocus(view, 5);
            view.updateCameraFocus(scene(99, first, next), request);
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
