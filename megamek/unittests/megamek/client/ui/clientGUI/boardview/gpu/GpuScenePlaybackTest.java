/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;

import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class GpuScenePlaybackTest {
    @Test
    void eachArrivalReleasesItsOwnTerrainArtworkAndContactsAtEverySpeed() {
        for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE, UnitMotion.Speed.QUADRUPLE)) {
            var start = UnitPlaybackTest.unit(1, 0);
            var middle = UnitPlaybackTest.unit(1, 2);
            var end = UnitPlaybackTest.unit(1, 4);
            var before = snapshot(0, start, contact(2, 1));
            var arrived = snapshot(1, middle, contact(2, 3), contact(3, 2));
            var latest = snapshot(2, end, contact(3, 4));
            var playback = new UnitPlayback();
            playback.accept(List.of(new BoardScene.SceneUpdate(before), move(start, middle),
                  new BoardScene.SceneUpdate(arrived), move(middle, end), new BoardScene.SceneUpdate(latest)), latest, ignored -> false);
            assertWorld(before, playback.present(latest));
            playback.advance(0, speed);
            playback.advance(playback.motions.get(1).remainingSeconds() / (2 * speed.rate), speed);
            assertWorld(before, playback.present(latest));
            assertTrue(playback.present(latest).rangeBorders().isEmpty());
            playback.advance(playback.motions.get(1).remainingSeconds() / speed.rate, speed);
            assertWorld(arrived, playback.present(latest));
            assertEquals(1, playback.holdSeconds(), 1e-6);
            playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS, speed);
            assertWorld(arrived, playback.present(latest));
            playback.advance(playback.motions.get(1).remainingSeconds() / speed.rate, speed);
            assertWorld(latest, playback.present(latest));
            assertEquals(latest.rangeBorders(), playback.present(latest).rangeBorders());
            assertEquals(1, playback.holdSeconds(), 1e-6, "Checkpoints do not add holds or delay the arrival state");
            assertEquals(0, before.tiles().getFirst().elevation(), "Retained snapshots are immutable");
        }
    }

    @Test
    void eachAttackAppliesDestructionAndVisibilityAtImpactBeforeRecoveryOrTheNextAttack() {
        for (var kind : List.of(ResolvedAttack.Kind.SHOT, ResolvedAttack.Kind.KICK)) {
            var attacker = UnitPlaybackTest.unit(1, 0);
            var victim = UnitPlaybackTest.unit(2, 4);
            var before = snapshot(0, attacker, victim, contact(3, 1));
            var firstImpact = snapshot(1, attacker, victim, contact(3, 3));
            var latest = snapshot(2, attacker, contact(4, 2));
            var playback = new UnitPlayback();
            playback.accept(List.of(new BoardScene.SceneUpdate(before), UnitPlaybackTest.attack(attacker, victim, kind, true),
                  new BoardScene.SceneUpdate(firstImpact), UnitPlaybackTest.attack(attacker, victim, kind, true),
                  new BoardScene.SceneUpdate(latest)), latest, ignored -> false);
            playback.advance(0, UnitMotion.Speed.NORMAL);
            assertWorld(before, playback.present(latest));
            assertTrue(playback.present(latest).units().contains(victim), "A removed victim remains visible until its received attack");
            var attack = playback.attack();
            playback.advance((attack.contactSeconds - .001) / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
            assertWorld(before, playback.present(latest));
            playback.advance(.002 / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
            assertWorld(firstImpact, playback.present(latest));
            assertTrue(playback.attack().seconds < playback.attack().duration);
            playback.advance((attack.duration - attack.seconds) / UnitMotion.Speed.NORMAL.rate + UnitPlayback.COMPLETION_HOLD_SECONDS,
                  UnitMotion.Speed.NORMAL);
            assertWorld(firstImpact, playback.present(latest));
            playback.advance((playback.attack().contactSeconds + .001) / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
            assertWorld(latest, playback.present(latest));
            assertFalse(playback.present(latest).units().contains(victim));
        }
    }

    @Test
    void lateCheckpointsWaitWhilePausedAndReleaseWithoutAnotherGameFrame() {
        var start = UnitPlaybackTest.unit(1, 0);
        var end = UnitPlaybackTest.unit(1, 4);
        var before = snapshot(0, start, UnitPlaybackTest.unit(2, 1));
        var latest = snapshot(1, end, contact(2, 3));
        var playback = new UnitPlayback();
        playback.accept(List.of(new BoardScene.SceneUpdate(before), move(start, end)), before, ignored -> false);
        playback.advance(.1, UnitMotion.Speed.NORMAL);
        playback.togglePaused();
        playback.accept(List.of(new BoardScene.SceneUpdate(latest)), latest, ignored -> false);
        playback.advance(100, UnitMotion.Speed.NORMAL);
        assertWorld(before, playback.present(latest));
        assertFalse(playback.present(latest).units().get(1).sensorContact());
        playback.togglePaused();
        playback.advance(playback.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        assertWorld(latest, playback.present(latest));
        assertTrue(playback.present(latest).units().get(1).sensorContact());
    }

    @Test
    void instantLargeFramesAndBoardResetDoNotStrandCheckpoints() {
        var start = UnitPlaybackTest.unit(1, 0);
        var end = UnitPlaybackTest.unit(1, 4);
        var before = snapshot(0, start);
        var latest = snapshot(1, end);
        for (var speed : List.of(UnitMotion.Speed.NORMAL, UnitMotion.Speed.INSTANT)) {
            var playback = new UnitPlayback();
            playback.accept(List.of(new BoardScene.SceneUpdate(before), move(start, end), new BoardScene.SceneUpdate(latest)),
                  latest, ignored -> false);
            playback.advance(1000, speed);
            assertSame(latest, playback.present(latest));
            assertFalse(playback.busy());
            playback.accept(List.of(new BoardScene.SceneUpdate(before), move(start, end)), latest, ignored -> false);
            playback.clear();
            assertSame(latest, playback.present(latest));
            assertTrue(playback.motions.isEmpty());
        }
    }

    private static void assertWorld(BoardScene expected, BoardScene shown) {
        assertSame(expected.tiles(), shown.tiles(), "Terrain and every bitmap layer share the event's checkpoint");
        assertEquals(expected.markers(), shown.markers());
        assertEquals(expected.tactical(), shown.tactical());
        assertEquals(expected.units().stream().filter(BoardScene.Unit::sensorContact).toList(),
              shown.units().stream().filter(BoardScene.Unit::sensorContact).toList());
    }

    private static BoardScene.Unit contact(int id, int row) {
        return new BoardScene.Unit(id, -1, "?", new BoardScene.Waypoint(new Coords(0, row), 0, 0), null, true, null, 1, false);
    }

    private static BoardScene.Movement move(BoardScene.Unit start, BoardScene.Unit end) {
        return new BoardScene.Movement(start.id(), 0, List.of(start.location(), end.location()), EntityMovementType.MOVE_WALK, 0, 4, end);
    }

    private static BoardScene snapshot(int version, BoardScene.Unit... units) {
        var coords = new Coords(0, 0);
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF000000 | version);
        var pixels = new BoardScene.Pixels(image);
        var feature = new BoardScene.Feature("building-" + version, 0, 0, 0, 1, version, 0, BoardScene.FeatureKind.BUILDING);
        var tile = new BoardScene.Tile(coords, version, -1, false, 0, BoardScene.Surface.GRASS, pixels, pixels, pixels,
              List.of(feature), List.of());
        var marker = new BoardMarker(BoardMarker.Kind.CARGO, coords, 0, 0xFFFF00, "Cargo " + version);
        var text = new BoardTactical.Text("Objective " + version, new Font(Font.SANS_SERIF, Font.PLAIN, 12), 0, 0, 0xFFFF0000);
        var tactical = new BoardTactical(List.of(), List.of(new BoardTactical.Label(new BoardTactical.Point(0, 0), text,
              BoardTactical.Playback.HOLD_DURING_PLAYBACK)));
        return new BoardScene(0, 1, 1, List.of(tile), List.of(units), List.of(), 1, "MOVEMENT", List.of(), null,
              List.of(), List.of(new BoardScene.RangeBorder(coords, 63, 0x44FF88, "S")), List.of(marker), tactical);
    }
}
