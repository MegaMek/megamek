/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.sprite.C3Sprite;
import megamek.client.ui.clientGUI.boardview.sprite.CursorSprite;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.clientGUI.boardview.sprite.FlightPathIndicatorSprite;
import megamek.client.ui.clientGUI.boardview.sprite.MovementEnvelopeSprite;
import megamek.client.ui.clientGUI.boardview.sprite.SensorRangeSprite;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.client.ui.clientGUI.boardview.sprite.StepSprite;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class GpuTacticalTest {
    @Test
    void playbackKeepsObjectiveBoundariesAndLiveMeasurementsWhileHidingUnitOverlays() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            var second = GpuUnitHudTest.addUnits(fixture, 1).getFirst();
            SwingUtilities.invokeAndWait(() -> {
                var zone = new FieldOfFireSprite(fixture.view, Color.GREEN, new Coords(2, 4), 63);
                var cursor = new CursorSprite(fixture.view, Color.CYAN);
                cursor.setHexLocation(new Coords(3, 2));
                fixture.view.addSprites(List.of(zone, cursor));
                fixture.source.refresh();
                var playback = new UnitPlayback();
                playback.present(fixture.source.takeFrame().scene());

                var sprites = overlays(fixture);
                sprites.add(new C3Sprite(fixture.view, fixture.entity, second));
                fixture.view.addSprites(List.of(sprites.getLast()));
                sprites.forEach(sprite -> sprite.setHidden(true));
                cursor.setHexLocation(new Coords(3, 3));
                BoardTactical expected = fixture.view.captureTacticalGeometry();
                sprites.forEach(sprite -> sprite.setHidden(false));
                zone.setHidden(true);
                fixture.view.addSprites(List.of(new FieldOfFireSprite(fixture.view, Color.ORANGE, new Coords(7, 4), 63)));
                fixture.source.refresh();
                var latest = fixture.source.takeFrame().scene();
                var unit = latest.units().getFirst();
                playback.accept(List.of(new BoardScene.Movement(unit.id(), latest.boardId(),
                      List.of(new BoardScene.Waypoint(new Coords(4, 5), 0, 0), unit.location()),
                      EntityMovementType.MOVE_WALK, 0, 1, unit)), latest, ignored -> false);
                var shown = playback.present(latest).tactical();
                assertFalse(expected.fills().isEmpty());
                assertEquals(expected.fills().size(), shown.fills().size());
                assertTrue(shown.fills().containsAll(expected.fills()), "Keep the old zone and current ruler/LOS geometry");
                assertEquals(expected.labels(), shown.labels(), "Movement costs and flight labels must disappear");
                assertTrue(latest.tactical().fills().size() > shown.fills().size());
                assertFalse(latest.tactical().labels().isEmpty());
                playback.advance(0, UnitMotion.Speed.NORMAL);
                playback.advance(playback.motions.get(unit.id()).remainingSeconds() / UnitMotion.Speed.NORMAL.rate,
                      UnitMotion.Speed.NORMAL);
                assertEquals(latest.tactical(), playback.present(latest).tactical());
            });
        }
    }

    static List<Sprite> overlays(GpuBoardFixture fixture) {
        var path = new MovePath(fixture.game, fixture.entity);
        path.addStep(MoveStepType.FORWARDS);
        path.addStep(MoveStepType.TURN_RIGHT);
        List<Sprite> sprites = new ArrayList<>();
        sprites.add(new MovementEnvelopeSprite(fixture.view, Color.MAGENTA, new Coords(4, 4), 63));
        sprites.add(new SensorRangeSprite(fixture.view, SensorRangeSprite.SENSORS, new Coords(6, 4), 63));
        sprites.add(new StepSprite(fixture.view, path.getStepVector().getFirst(), false));
        sprites.add(new StepSprite(fixture.view, path.getStepVector().getLast(), true));
        sprites.add(new FlightPathIndicatorSprite(fixture.view, path.getStepVector(), 0, true));
        fixture.view.addSprites(sprites);
        fixture.view.drawRuler(new Coords(3, 5), new Coords(8, 6), Color.CYAN, Color.YELLOW);
        return sprites;
    }

    @Test
    void shapesAndTextLeaveTheRasterLayerAndRestoreClassicZoom() throws Exception {
        int originalZoom = GUIPreferences.getInstance().getMapZoomIndex();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            var second = GpuUnitHudTest.addUnits(fixture, 1).getFirst();
            SwingUtilities.invokeAndWait(() -> {
                fixture.source.refresh();
                BoardScene before = fixture.source.takeFrame().scene();
                var sprites = overlays(fixture);
                sprites.add(new C3Sprite(fixture.view, fixture.entity, second));
                fixture.view.addSprites(List.of(sprites.getLast()));
                fixture.source.refresh();
                BoardScene captured = fixture.source.takeFrame().scene();
                assertFalse(captured.tactical().fills().isEmpty());
                assertFalse(captured.tactical().labels().isEmpty(), "Movement costs must remain readable labels");
                assertEquals(before.tiles().stream().map(BoardScene.Tile::tactical).toList(),
                      captured.tiles().stream().map(BoardScene.Tile::tactical).toList());
                fixture.view.getComponent();
                fixture.view.zoomOut();
                float zoom = fixture.view.getScale();
                assertEquals(captured.tactical(), fixture.view.captureTacticalGeometry());
                assertEquals(zoom, fixture.view.getScale());
                sprites.forEach(sprite -> sprite.setHidden(true));
                BoardTactical hidden = fixture.view.captureTacticalGeometry();
                assertNotEquals(captured.tactical(), hidden);
                fixture.view.drawRuler(null, null, Color.CYAN, Color.YELLOW);
                assertTrue(fixture.view.captureTacticalGeometry().fills().isEmpty());
                assertFalse(captured.tactical().fills().isEmpty(), "Published geometry must remain immutable");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> GUIPreferences.getInstance().setMapZoomIndex(originalZoom));
        }
    }
}
