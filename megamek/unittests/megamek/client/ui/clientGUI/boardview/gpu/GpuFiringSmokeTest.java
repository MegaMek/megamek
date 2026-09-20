/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.clientGUI.boardview.sprite.TextMarkerSprite;
import megamek.common.Hex;
import megamek.common.RangeType;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual capture, shared scene and native renderer over a stepped board in both camera modes. */
@Tag("on-demand")
class GpuFiringSmokeTest {
    @Test
    void renderDirectIndirectAndTransparentRangeVolumesThenClearThem() throws Exception {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        var board = GpuFiringCaptureTest.board();
        // A clear paved surface keeps cosmetic props out of this firing/lettering review.
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.getHex(x, y).addTerrain(new Terrain(Terrains.PAVEMENT, 1));
            }
        }
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board)) {
            List<FieldOfFireSprite> borders = new ArrayList<>();
            List<TextMarkerSprite> labels = new ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getBoard().setHex(new Coords(4, 4), new Hex(1, "pavement:1", ""));
                fixture.game.getBoard().setHex(new Coords(2, 2), new Hex(0, "water:3", ""));
                GpuFiringCaptureTest.attacks(fixture);
                Coords source = fixture.entity.getPosition();
                for (int bracket = RangeType.RANGE_MINIMUM; bracket <= RangeType.RANGE_EXTREME; bracket++) {
                    Set<Coords> coverage = new HashSet<>();
                    for (Coords coords : source.allAtDistance(bracket + 1)) {
                        if (fixture.game.getBoard().contains(coords) && coords.getX() >= source.getX()) {
                            coverage.add(coords);
                        }
                    }
                    for (Coords coords : coverage) {
                        int edges = 0;
                        for (int direction = 0; direction < 6; direction++) {
                            if (!coverage.contains(coords.translated(direction))) {
                                edges |= 1 << direction;
                            }
                        }
                        if (edges != 0) {
                            borders.add(new FieldOfFireSprite(fixture.view, bracket, coords, edges));
                        }
                    }
                    labels.add(new TextMarkerSprite(fixture.view, source.translated(1, bracket + 1), bracket));
                }
                fixture.view.addSprites(borders);
                labels.add(new TextMarkerSprite(fixture.view, new Coords(2, 2), RangeType.RANGE_SHORT));
                fixture.view.addSprites(labels);
                fixture.source.refresh();
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;

                @Override
                public void render() {
                    try {
                        super.render();
                        tick++;
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        if (tick == 1) {
                            boardCamera.setIsometric(true);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (tick == 6) {
                            assertEquals(2, fixture.source.takeFrame().scene().firingLines().size());
                            assertEquals(borders.size(), fixture.source.takeFrame().scene().rangeBorders().size());
                            if (!BoardView.GPU_SCROLLING_RANGE_LABELS) {
                                assertEquals(labels.size(), fixture.source.takeFrame().scene().rangeLabels().size());
                            }
                            GpuBoardTestUi.capture(new File(output, "firing-volumes-isometric.png"));
                            boardCamera.orbit(120, 0);
                        } else if (tick == 10) {
                            GpuBoardTestUi.capture(new File(output, "firing-volumes-rotated.png"));
                            boardCamera.setIsometric(false);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (tick == 14) {
                            GpuBoardTestUi.capture(new File(output, "firing-volumes-top.png"));
                            boardCamera.orbit(120, 0);
                        } else if (tick == 18) {
                            GpuBoardTestUi.capture(new File(output, "firing-volumes-top-rotated.png"));
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.view.clearAllAttacks();
                                fixture.view.removeSprites(borders);
                                fixture.view.removeSprites(labels);
                                fixture.source.refresh();
                            });
                        } else if (tick == 20) {
                            assertTrue(fixture.source.takeFrame().scene().firingLines().isEmpty());
                            assertTrue(fixture.source.takeFrame().scene().rangeBorders().isEmpty());
                            assertTrue(fixture.source.takeFrame().scene().rangeLabels().isEmpty());
                            GpuBoardTestUi.capture(new File(output, "firing-volumes-cleared.png"));
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }
}
