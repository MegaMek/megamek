/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual full-size geometry, picking, atlases and both camera views; no reduced-resolution fixture. */
@Tag("on-demand")
class GpuLargeMapSmokeTest {
    @Test
    void rendersTheEntire200By200Board() throws Exception {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(GpuLargeMapTest.largeBoard())) {
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private long started;
                private long last;
                private double overviewMillis;
                private double closeupMillis;
                private GLProfiler profiler;
                private int overviewDraws;

                @Override
                public void create() {
                    super.create();
                    profiler = new GLProfiler(Gdx.graphics);
                }

                @Override
                public void render() {
                    try {
                        if (started == 0) {
                            started = System.nanoTime();
                        }
                        // GLProfiler checks each GL call. Keep it outside the measured timing windows.
                        if (frames() == 89 || frames() == 164) {
                            profiler.reset();
                            profiler.enable();
                        }
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        long now = System.nanoTime();
                        if (frames() >= 35 && frames() < 85) {
                            overviewMillis += (now - last) / 1e6;
                        } else if (frames() >= 110 && frames() < 160) {
                            closeupMillis += (now - last) / 1e6;
                        }
                        last = now;
                        if (frames() == 20) {
                            GpuBoardTestUi.capture(new File(output, "large-map-top.png"));
                            boardCamera.setIsometric(true);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        }
                        if (frames() == 90) {
                            overviewDraws = profiler.getDrawCalls();
                            profiler.disable();
                            GpuBoardTestUi.capture(new File(output, "large-map-isometric.png"));
                            boardCamera.center(BoardGeometry.center(new megamek.common.board.Coords(100, 100), 0));
                            boardCamera.zoom(0.03f);
                        }
                        if (frames() == 165) {
                            int closeupDraws = profiler.getDrawCalls();
                            profiler.disable();
                            assertTrue(closeupDraws < overviewDraws / 4,
                                  "Closeups must cull offscreen chunks: " + closeupDraws + " vs " + overviewDraws);
                            GpuBoardTestUi.capture(new File(output, "large-map-closeup.png"));
                            System.out.printf("200x200 native: %.2fs total, overview %.1fms/frame (%d draws), "
                                        + "closeup %.1fms/frame (%d draws), %s%n",
                                  (now - started) / 1e9, overviewMillis / 50, overviewDraws, closeupMillis / 50, closeupDraws,
                                  Gdx.gl.glGetString(GL20.GL_RENDERER));
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        profiler.disable();
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) {
            throw new AssertionError("Full-size GPU board failed", failure.get());
        }
    }
}
