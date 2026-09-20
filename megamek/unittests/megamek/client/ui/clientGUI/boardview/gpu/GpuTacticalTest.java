/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.BoardTacticalGraphics;
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
    void visualRangeUsesStraightWallsAndRetainsSpriteVisibilityAndPlayback() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                var before = fixture.view.captureTacticalGeometry();
                var first = new SensorRangeSprite(fixture.view, SensorRangeSprite.VISUAL, new Coords(2, 4), 48);
                var second = new SensorRangeSprite(fixture.view, SensorRangeSprite.VISUAL_DARK, new Coords(2, 5), 48);
                fixture.view.addSprites(List.of(first, second));
                fixture.source.refresh();
                var captured = fixture.source.takeFrame().scene().tactical();
                assertEquals(before.fills(), captured.fills(), "The flat band and dashed line must be replaced");
                assertEquals(2, captured.walls().size());
                assertEquals(2, captured.flatWalls().size());
                for (var wall : captured.walls()) {
                    assertEquals(147, wall.a().x(), 0.001f);
                    assertEquals(147, wall.b().x(), 0.001f);
                    assertEquals(72, Math.abs(wall.b().y() - wall.a().y()), 0.001f);
                    assertTrue(wall.height() > 0);
                    assertTrue((wall.argb() >>> 24) > 0 && (wall.argb() >>> 24) < 255);
                    assertEquals(SensorRangeSprite.getColor(SensorRangeSprite.VISUAL).getRGB() & 0xFFFFFF,
                          wall.argb() & 0xFFFFFF);
                }
                assertTrue(captured.duringPlayback(captured, true).walls().isEmpty());
                assertTrue(captured.duringPlayback(captured, true).flatWalls().isEmpty());
                assertEquals(captured.walls(), captured.duringPlayback(captured, false).walls());
                assertEquals(captured.flatWalls(), captured.duringPlayback(captured, false).flatWalls());
                first.setHidden(true);
                second.setHidden(true);
                fixture.source.refresh();
                assertEquals(before, fixture.source.takeFrame().scene().tactical());
                assertEquals(2, captured.walls().size(), "Published geometry remains immutable");

                // Exercise every shared border layout, including lone sides and closed corner paths.
                for (int mode = SensorRangeSprite.SENSORS; mode <= SensorRangeSprite.VISUAL_DARK; mode++) {
                    for (int mask = 1; mask < 64; mask++) {
                        var graphics = new BoardTacticalGraphics();
                        try {
                            new SensorRangeSprite(fixture.view, mode, new Coords(4, 4), mask).drawTactical(graphics);
                            assertEquals(mode >= SensorRangeSprite.VISUAL, graphics.snapshot().fills().isEmpty());
                            assertEquals(mode < SensorRangeSprite.VISUAL, graphics.snapshot().walls().isEmpty());
                            assertEquals(mode < SensorRangeSprite.VISUAL, graphics.snapshot().flatWalls().isEmpty());
                        } finally {
                            graphics.dispose();
                        }
                    }
                }
                var preferences = GUIPreferences.getInstance();
                Color original = preferences.getVisualRangeColor();
                try {
                    Color configured = new Color(35, 150, 200);
                    preferences.setVisualRangeColor(configured);
                    first.setHidden(false);
                    fixture.source.refresh();
                    var recolored = fixture.source.takeFrame().scene().tactical().walls().getFirst();
                    assertEquals(configured.getRGB() & 0xFFFFFF, recolored.argb() & 0xFFFFFF);
                    assertEquals(captured.walls().getFirst().argb() >>> 24, recolored.argb() >>> 24);
                } finally {
                    preferences.setVisualRangeColor(original);
                }
            });
        }
    }

    @Test
    void gpuMapBorderCanBeHiddenWithoutRemovingInteriorRangeBoundaries() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                var board = fixture.view.getBoard();
                for (int mode = SensorRangeSprite.SENSORS; mode <= SensorRangeSprite.VISUAL_DARK; mode++) {
                    for (int x = 0; x < board.getWidth(); x++) {
                        for (int y = 0; y < board.getHeight(); y++) {
                            Coords coords = new Coords(x, y);
                            int mapBorder = 0, interior = 0;
                            for (int direction = 0; direction < 6; direction++) {
                                if (!board.contains(coords.translated(direction))) {
                                    mapBorder |= 1 << direction;
                                } else {
                                    interior = 1 << direction;
                                }
                            }
                            if (mapBorder == 0) {
                                continue;
                            }
                            var perimeter = new SensorRangeSprite(fixture.view, mode, coords, mapBorder);
                            assertEquals(BoardTactical.EMPTY, captureRange(perimeter),
                                  "The default must omit forced map-edge outlines, including at corners");
                            assertEquals(mapBorder, perimeter.getBorders(), "Rendering must not modify the source edges");
                            var mixed = new SensorRangeSprite(fixture.view, mode, coords, mapBorder | interior);
                            var genuine = new SensorRangeSprite(fixture.view, mode, coords, interior);
                            assertNotEquals(BoardTactical.EMPTY, captureRange(genuine));
                            assertEquals(captureRange(genuine), captureRange(mixed),
                                  "Keep a real range boundary in the same hex as a suppressed map edge");
                            var enabled = new SensorRangeSprite(fixture.view, mode, coords, mapBorder) {
                                @Override
                                protected void paintTactical(Graphics2D graph) {
                                    paintRange(graph, true);
                                }
                            };
                            assertNotEquals(BoardTactical.EMPTY, captureRange(enabled),
                                  "Enabling the option must restore the map perimeter for every range mode");
                        }
                    }
                }
            });
        }
    }

    private static BoardTactical captureRange(SensorRangeSprite sprite) {
        var graphics = new BoardTacticalGraphics();
        try {
            sprite.drawTactical(graphics);
            return graphics.snapshot();
        } finally {
            graphics.dispose();
        }
    }

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
