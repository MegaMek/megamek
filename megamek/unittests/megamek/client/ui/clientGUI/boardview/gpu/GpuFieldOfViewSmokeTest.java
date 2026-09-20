/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native visual comparison: identical rules and camera, two choices of presentation. */
@Tag("on-demand")
class GpuFieldOfViewSmokeTest {
    private record Brightness(float visible, float blocked) { }

    @Test
    void bothStylesShareVisibilityAndMaskCacheButRenderDifferentObscuration() throws Exception {
        Map<GpuFieldOfView.Style, Brightness> results = new EnumMap<>(GpuFieldOfView.Style.class);
        BoardFieldOfView expected = null;
        try (var options = new GpuFieldOfViewTest.Options()) {
            for (GpuFieldOfView.Style style : GpuFieldOfView.Style.values()) {
                AtomicReference<Throwable> failure = new AtomicReference<>();
                try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
                    SwingUtilities.invokeAndWait(() -> {
                        fixture.view.select(new Coords(1, 6));
                        fixture.source.setVisibleArea(new Rectangle(0, 0, 16, 17));
                        fixture.source.refresh();
                    });
                    BoardFieldOfView mask = fixture.source.takeFrame().scene().fieldOfView();
                    assertTrue(mask.active());
                    if (expected != null) {
                        assertEquals(expected, mask, "Changing style must not change any visibility result");
                    }
                    expected = mask;
                    new Lwjgl3Application(new GpuBattleView(fixture.source, style) {
                        private int tick;
                        private GpuFieldOfView probe;

                        @Override
                        public void render() {
                            try {
                                super.render();
                                tick++;
                                if (tick == 1) {
                                    boardCamera.setIsometric(false);
                                    probe = new GpuFieldOfView(style);
                                    probe.update(mask);
                                    assertEquals(1, probe.uploads());
                                } else if (tick == 12) {
                                    capture("top");
                                    results.put(style, brightness(fixture.source.takeFrame().scene()));
                                    boardCamera.setIsometric(true);
                                    boardCamera.fit(fixture.source.takeFrame().scene());
                                } else if (tick == 24) {
                                    capture("isometric");
                                    probe.update(mask);
                                    assertEquals(1, probe.uploads(), "Camera movement must reuse an unchanged mask");
                                    probe.update(BoardFieldOfView.EMPTY);
                                    assertFalse(probe.active(), "Disabling FoV must immediately stop shading");
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
                            GpuBoardTestUi.capture(new File(output, "fov-" + style.name().toLowerCase(java.util.Locale.ROOT)
                                  + "-" + camera + ".png"));
                        }

                        private Brightness brightness(BoardScene scene) {
                            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0,
                                  Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                            try {
                                float visible = 0, blocked = 0;
                                int visibleCount = 0, blockedCount = 0;
                                float scale = new GpuDisplayScale().read(fixture.source.uiPreferences.scale());
                                for (BoardScene.Tile tile : scene.tiles()) {
                                    if (tile.water() || !tile.features().isEmpty() || tile.coords().equals(fixture.entity.getPosition())) {
                                        continue;
                                    }
                                    Vector3 point = BoardGeometry.center(tile.coords(), tile.elevation())
                                          .add(BoardGeometry.WIDTH * 0.10f, BoardGeometry.HEIGHT * 0.08f, 0);
                                    boardCamera.camera.project(point, 0, Math.round(GpuBoardUi.TURN_HEIGHT * scale),
                                          boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight);
                                    int x = Math.round(point.x * pixels.getWidth() / Gdx.graphics.getWidth());
                                    int y = Math.round(point.y * pixels.getHeight() / Gdx.graphics.getHeight());
                                    if (x < 0 || y < 0 || x >= pixels.getWidth() || y >= pixels.getHeight()) {
                                        continue;
                                    }
                                    int rgb = pixels.getPixel(x, y);
                                    float value = (((rgb >>> 24) & 255) + ((rgb >>> 16) & 255) + ((rgb >>> 8) & 255)) / 765f;
                                    var visibility = GpuFieldOfViewTest.at(scene.fieldOfView(), tile.coords()).visibility();
                                    if (visibility == BoardFieldOfView.Visibility.VISIBLE) {
                                        visible += value;
                                        visibleCount++;
                                    } else if (visibility == BoardFieldOfView.Visibility.BLOCKED) {
                                        blocked += value;
                                        blockedCount++;
                                    }
                                }
                                assertTrue(visibleCount > 10 && blockedCount > 10,
                                      "The comparison must sample both clear and blocked terrain");
                                return new Brightness(visible / visibleCount, blocked / blockedCount);
                            } finally {
                                pixels.dispose();
                            }
                        }

                        @Override
                        public void dispose() {
                            if (probe != null) {
                                probe.dispose();
                            }
                            super.dispose();
                        }
                    }, GpuBoardWindow.configuration(false));
                    if (failure.get() != null) {
                        throw new AssertionError("Native FoV comparison failed for " + style, failure.get());
                    }
                }
            }
        }
        Brightness dimmed = results.get(GpuFieldOfView.Style.DIMMED), fog = results.get(GpuFieldOfView.Style.FOG_OF_WAR);
        assertEquals(dimmed.visible(), fog.visible(), 0.04, "Visible terrain must remain consistent between styles");
        assertTrue(fog.blocked() < dimmed.blocked() * 0.8f, "Fog style must obscure blocked terrain more strongly");
    }
}
