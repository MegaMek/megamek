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
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.common.Hex;
import megamek.common.RangeType;
import megamek.common.board.Coords;
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
        try (GpuBoardFixture fixture = GpuBoardFixture.create(GpuFiringCaptureTest.board())) {
            List<FieldOfFireSprite> borders = new ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getBoard().setHex(new Coords(4, 4), new Hex(1));
                GpuFiringCaptureTest.attacks(fixture);
                Set<Coords> coverage = new HashSet<>();
                Coords source = fixture.entity.getPosition();
                for (Coords coords : source.allAtDistanceOrLess(5)) {
                    if (fixture.game.getBoard().contains(coords) && coords.getX() >= source.getX()
                          && !coords.equals(source)) {
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
                        borders.add(new FieldOfFireSprite(fixture.view, RangeType.RANGE_SHORT, coords, edges));
                    }
                }
                fixture.view.addSprites(borders);
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
                            GpuBoardTestUi.capture(new File(output, "firing-volumes-isometric.png"));
                            boardCamera.setIsometric(false);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (tick == 10) {
                            GpuBoardTestUi.capture(new File(output, "firing-volumes-top.png"));
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.view.clearAllAttacks();
                                fixture.view.removeSprites(borders);
                                fixture.source.refresh();
                            });
                        } else if (tick == 12) {
                            assertTrue(fixture.source.takeFrame().scene().firingLines().isEmpty());
                            assertTrue(fixture.source.takeFrame().scene().rangeBorders().isEmpty());
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
