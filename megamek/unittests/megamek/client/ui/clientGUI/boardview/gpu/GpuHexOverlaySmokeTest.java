/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual terrain, camera and GL resources for the converted board markings. */
@Tag("on-demand")
class GpuHexOverlaySmokeTest {
    @Test
    void rendersBordersEmbeddedBoardsAndEcmInBothCamerasAndReusesMeshes() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (var options = new GpuHexOverlayTest.Options(); GpuBoardFixture fixture = GpuBoardFixture.create()) {
            fixture.addEcm();
            SwingUtilities.invokeAndWait(() -> {
                GUIPreferences.getInstance().setShowMapSheets(true);
                fixture.game.getBoard().setEmbeddedBoard(1, new Coords(11, 6));
                fixture.source.setVisibleArea(new Rectangle(0, 0, 16, 17));
                fixture.source.refresh();
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;
                private GpuTactical probe;
                private long builds;

                @Override
                public void render() {
                    try {
                        super.render();
                        tick++;
                        BoardScene scene = fixture.source.takeFrame().scene();
                        if (tick == 1) {
                            boardCamera.setIsometric(false);
                            boardCamera.fit(scene);
                            probe = new GpuTactical();
                            probe.update(scene);
                            builds = probe.builds();
                            assertFalse(scene.tactical().fills().isEmpty());
                        } else if (tick == 12) {
                            capture("top");
                            boardCamera.setIsometric(true);
                            boardCamera.fit(scene);
                        } else if (tick == 24) {
                            capture("isometric");
                            probe.update(scene);
                            assertEquals(builds, probe.builds(), "Camera changes must reuse the overlay meshes");
                            boardCamera.center(BoardGeometry.center(new Coords(7, 6), 1));
                            boardCamera.zoom(0.58f);
                        } else if (tick == 32) {
                            capture("closeup");
                            SwingUtilities.invokeAndWait(() -> {
                                GUIPreferences.getInstance().setECMTransparency(0);
                                GUIPreferences.getInstance().setShowMapSheets(false);
                                fixture.game.getBoard().embeddedBoardCoords().clear();
                                fixture.source.refresh();
                            });
                        } else if (tick == 40) {
                            probe.update(scene);
                            assertEquals(builds + 1, probe.builds(), "Clearing overlays must release the old geometry");
                            assertEquals(0, scene.tactical().fills().size());
                            capture("cleared");
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private void capture(String camera) {
                    File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                    GpuBoardTestUi.capture(new File(output, "hex-overlays-" + camera + ".png"));
                }

                @Override
                public void dispose() {
                    if (probe != null) {
                        probe.dispose();
                    }
                    super.dispose();
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) {
            throw new AssertionError("Native hex overlay rendering failed", failure.get());
        }
    }
}
