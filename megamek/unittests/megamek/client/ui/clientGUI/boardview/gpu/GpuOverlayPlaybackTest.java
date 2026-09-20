/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.function.Consumer;

import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.BoardTacticalGraphics;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class GpuOverlayPlaybackTest {
    @Test
    void movementDefersOverlaysAndMapChangesWhileMeasurementsAndNotesStayLiveAtEverySpeed() {
        for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE, UnitMotion.Speed.QUADRUPLE)) {
            var playback = new UnitPlayback();
            var settled = snapshot("old");
            assertSame(settled, playback.present(settled));
            var latest = snapshot("destination");
            playback.accept(List.of(move(1)), latest, ignored -> false);
            assertDeferred(playback.present(latest), latest, settled);
            playback.advance(0, speed);
            var motion = playback.motions.get(1);
            playback.advance(motion.remainingSeconds() / (2 * speed.rate), speed);
            assertTrue(motion.isMoving());
            playback.togglePaused();
            latest = snapshot("next selection");
            playback.accept(List.of(), latest, ignored -> false);
            playback.advance(100, speed);
            assertDeferred(playback.present(latest), latest, settled);
            assertEquals("old", settled.markers().get(1).label(), "Published snapshots must remain immutable");
            playback.togglePaused();
            playback.advance(motion.remainingSeconds() / speed.rate, speed);
            assertFalse(motion.isMoving());
            assertTrue(playback.busy(), "The completion hold must not delay restoring current overlays");
            assertCurrent(playback.present(latest), latest);
        }
    }

    @Test
    void markersStayHeldAcrossQueuedMovesAndCombatAndRefreshAfterTheLastArrival() {
        var playback = new UnitPlayback();
        var settled = snapshot("old");
        playback.present(settled);
        var latest = snapshot("new");
        var combat = UnitPlaybackTest.attack(UnitPlaybackTest.unit(1, 4), UnitPlaybackTest.unit(2, 4),
              ResolvedAttack.Kind.SHOT, true);
        playback.accept(List.of(move(1), combat, move(2)), latest, ignored -> false);
        playback.advance(0, UnitMotion.Speed.NORMAL);
        playback.advance(playback.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        assertDeferred(playback.present(latest), latest, settled);
        playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS, UnitMotion.Speed.NORMAL);
        assertDeferred(playback.present(latest), latest, settled);
        playback.advance(playback.attack().duration / UnitMotion.Speed.NORMAL.rate + UnitPlayback.COMPLETION_HOLD_SECONDS,
              UnitMotion.Speed.NORMAL);
        assertTrue(playback.motions.get(2).isMoving());
        assertDeferred(playback.present(latest), latest, settled);
        playback.advance(playback.motions.get(2).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        assertCurrent(playback.present(latest), latest);

        // The next movement must retain the newly displayed marker state, not the first move's history.
        settled = latest;
        latest = snapshot("third");
        playback.accept(List.of(move(1)), latest, ignored -> false);
        assertDeferred(playback.present(latest), latest, settled);
    }

    @Test
    void combatAndSkippingKeepCurrentOverlaysAndClearingDropsOldBoardHistory() {
        var latest = snapshot("new");
        var playback = new UnitPlayback();
        playback.accept(List.of(UnitPlaybackTest.attack(UnitPlaybackTest.unit(1, 4), UnitPlaybackTest.unit(2, 4),
              ResolvedAttack.Kind.SHOT, true)), latest, ignored -> false);
        playback.advance(.1, UnitMotion.Speed.NORMAL);
        assertCurrent(playback.present(latest), latest);
        for (Consumer<UnitPlayback> finish : List.<Consumer<UnitPlayback>>of(
              value -> value.advance(0, UnitMotion.Speed.INSTANT), UnitPlayback::finish, UnitPlayback::clear)) {
            playback.clear();
            var settled = snapshot("old");
            playback.present(settled);
            playback.accept(List.of(move(1)), latest, ignored -> false);
            playback.advance(.1, UnitMotion.Speed.NORMAL);
            assertDeferred(playback.present(latest), latest, settled);
            finish.accept(playback);
            assertSame(latest, playback.present(latest));
        }
        playback.clear();
        playback.accept(List.of(move(1)), latest, ignored -> false);
        var moving = playback.present(latest);
        assertEquals(List.of(latest.markers().get(3)), moving.markers(), "No previous-board markers or future markers may leak in");
        assertEquals(List.of(latest.tactical().fills().get(2)), moving.tactical().fills());
        assertEquals(List.of(latest.tactical().labels().get(2)), moving.tactical().labels());
        playback.finish();
        assertCurrent(playback.present(latest), latest);
    }

    private static void assertDeferred(BoardScene moving, BoardScene latest, BoardScene settled) {
        assertTrue(moving.rangeBorders().isEmpty());
        assertTrue(moving.rangeLabels().isEmpty());
        assertTrue(moving.firingLines().isEmpty());
        assertTrue(moving.plannedPath().isEmpty());
        assertSame(BoardFieldOfView.EMPTY, moving.fieldOfView());
        assertEquals(List.of(settled.markers().get(1), settled.markers().get(2), settled.markers().get(4),
              latest.markers().get(3)), moving.markers(), "Keep old cargo, objective and minefield; update the note and hide warnings");
        assertEquals(List.of(settled.tactical().fills().get(1), latest.tactical().fills().get(2)), moving.tactical().fills());
        assertEquals(List.of(settled.tactical().labels().get(1), latest.tactical().labels().get(2)), moving.tactical().labels());
    }

    private static void assertCurrent(BoardScene shown, BoardScene latest) {
        assertEquals(latest.markers(), shown.markers());
        assertEquals(latest.tactical(), shown.tactical());
        assertEquals(latest.fieldOfView(), shown.fieldOfView());
        assertEquals(latest.rangeBorders(), shown.rangeBorders());
        assertEquals(latest.rangeLabels(), shown.rangeLabels());
        assertEquals(latest.firingLines(), shown.firingLines());
        assertEquals(latest.plannedPath(), shown.plannedPath());
    }

    private static BoardScene.Movement move(int id) {
        return new BoardScene.Movement(id, 0, List.of(UnitPlaybackTest.unit(id, 0).location(), UnitPlaybackTest.unit(id, 4).location()),
              EntityMovementType.MOVE_WALK, 0, 4, UnitPlaybackTest.unit(id, 4));
    }

    private static BoardScene snapshot(String version) {
        var scene = UnitPlaybackTest.scene(UnitPlaybackTest.unit(1, 4), UnitPlaybackTest.unit(2, 4));
        var coords = new Coords(0, 0);
        BoardTacticalGraphics graphics = new BoardTacticalGraphics();
        BoardTactical tactical;
        try {
            for (var timing : List.of(BoardTactical.Playback.HIDE_DURING_MOVEMENT,
                  BoardTactical.Playback.HOLD_DURING_MOVEMENT, BoardTactical.Playback.LIVE)) {
                BoardTacticalGraphics.draw(graphics, timing, layer -> {
                    Graphics2D local = BoardTacticalGraphics.at(layer, new Point(0, 0));
                    try {
                        local.setColor(new Color(version.hashCode() & 0xFFFFFF));
                        local.fillRect(0, 0, 20, 20);
                        local.drawString(version + timing, 0, 20);
                    } finally {
                        local.dispose();
                    }
                });
            }
            tactical = graphics.snapshot();
        } finally {
            graphics.dispose();
        }
        var markers = List.of(marker(BoardMarker.Kind.COLLAPSE_WARNING, version), marker(BoardMarker.Kind.CARGO, version),
              marker(BoardMarker.Kind.OBJECTIVE, version), marker(BoardMarker.Kind.PLAYER_NOTE, version),
              marker(version.equals("old") ? BoardMarker.Kind.MINEFIELD : BoardMarker.Kind.BRIDGE_BUILD, version));
        return new BoardScene(0, 1, 1, scene.tiles(), scene.units(), move(1).path(), 1, "MOVEMENT", List.of(), null,
              List.of(new BoardScene.FiringLine(UnitPlaybackTest.unit(1, 0).location(), UnitPlaybackTest.unit(2, 4).location(), 0, false)),
              List.of(new BoardScene.RangeBorder(coords, 63, 0, "S")), markers, tactical,
              List.of(new BoardScene.RangeLabel(coords, 0, "S")),
              new BoardFieldOfView(1, 1, List.of(new BoardFieldOfView.Hex(BoardFieldOfView.Visibility.ORIGIN, 0x80008000)),
                    128, 128, true, false, false));
    }

    private static BoardMarker marker(BoardMarker.Kind kind, String label) {
        return new BoardMarker(kind, new Coords(0, 0), 0, kind.rgb(), label);
    }
}
