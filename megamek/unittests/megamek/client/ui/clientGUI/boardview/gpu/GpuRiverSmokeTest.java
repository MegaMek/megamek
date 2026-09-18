/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native visual review of a winding river, waterfalls, tree silhouettes and low rubble. */
@Tag("on-demand")
class GpuRiverSmokeTest {
    @Test
    void rendersContinuousChannelsAnimatedDropsAndNatureVariants() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                List<Coords> river = new ArrayList<>();
                Coords next = new Coords(2, 0);
                river.add(next);
                for (int direction : new int[] { 3, 3, 3, 2, 2, 2, 2, 3, 3, 3 }) {
                    next = next.translated(direction);
                    river.add(next);
                }
                Hex[] hexes = new Hex[100];
                for (int y = 0; y < 10; y++) {
                    for (int x = 0; x < 10; x++) {
                        Hex hex = new Hex(Math.max(0, 3 - y / 3));
                        if (river.contains(new Coords(x, y))) {
                            hex.addTerrain(new Terrain(Terrains.WATER, 1));
                        }
                        if (x == 2 && y == 1) {
                            hex.addTerrain(new Terrain(Terrains.BRIDGE, 2, true, 18));
                            hex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, 0));
                            hex.addTerrain(new Terrain(Terrains.BRIDGE_CF, 80));
                        }
                        if (x == 1 && y == 1 || x == 3 && y == 0) {
                            hex.addTerrain(new Terrain(Terrains.ROAD, 1, true, 18));
                        }
                        if ((x == 0 || x == 8) && y % 2 == 1) {
                            hex.addTerrain(new Terrain(Terrains.WOODS, x == 0 && y <= 3 ? 1 : 2));
                            hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
                            if (x == 0 && y == 1) {
                                hex.setTheme("desert");
                            }
                            if (x == 8 && y <= 3) {
                                hex.setTheme("snow");
                                hex.addTerrain(new Terrain(Terrains.SNOW, 1));
                            }
                        }
                        if (y == 1 && x >= 4 && x <= 6 || x == 8 && y == 4) {
                            hex.addTerrain(new Terrain(Terrains.RUBBLE, 4));
                            if (x == 6) {
                                hex.setTheme("snow");
                            }
                        }
                        hexes[y * 10 + x] = hex;
                    }
                }
                fixture.game.setBoard(new Board(10, 10, hexes));
                fixture.source.refresh();
            });
            BoardScene scene = fixture.source.takeFrame().scene();
            new Lwjgl3Application(new ApplicationAdapter() {
                GpuTerrain terrain;
                BoardCamera camera;
                int frames;
                int fallPixels;

                @Override
                public void create() {
                    terrain = new GpuTerrain();
                    terrain.update(scene);
                    camera = new BoardCamera();
                    camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    camera.fit(scene);
                }

                @Override
                public void render() {
                    try {
                        terrain.animate(1f / 12, List.of());
                        terrain.renderShadows(List.of());
                        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
                        terrain.render(camera.camera, false);
                        terrain.renderTransparent(camera.camera);
                        File output = new File(System.getProperty("megamek.gpu.screenshots"));
                        if (++frames == 3) {
                            GpuBoardTestUi.capture(new File(output, "river-channels-top.png"));
                            camera.setIsometric(true);
                            camera.fit(scene);
                        } else if (frames == 6) {
                            GpuBoardTestUi.capture(new File(output, "river-channels-isometric.png"));
                            camera.center(BoardGeometry.center(new Coords(2, 3), 2));
                            camera.zoom(0.4f);
                        } else if (frames == 9) {
                            GpuBoardTestUi.capture(new File(output, "waterfall-closeup.png"));
                            fallPixels = waterfallPixels(camera, scene);
                        } else if (frames == 13) {
                            GpuBoardTestUi.capture(new File(output, "waterfall-flow.png"));
                            assertNotEquals(fallPixels, waterfallPixels(camera, scene), "The vertical waterfall texture must animate");
                            camera.center(BoardGeometry.center(new Coords(6, 2), 3));
                        } else if (frames == 16) {
                            GpuBoardTestUi.capture(new File(output, "nature-and-rubble.png"));
                            camera.center(BoardGeometry.center(new Coords(2, 1), 3));
                        } else if (frames == 19) {
                            GpuBoardTestUi.capture(new File(output, "bridge-road-isometric.png"));
                            camera.setIsometric(false);
                        } else if (frames == 22) {
                            GpuBoardTestUi.capture(new File(output, "bridge-road-top.png"));
                            camera.setIsometric(true);
                            camera.center(BoardGeometry.center(new Coords(0, 1), 3));
                        } else if (frames == 25) {
                            GpuBoardTestUi.capture(new File(output, "woods-light-desert.png"));
                            camera.center(BoardGeometry.center(new Coords(0, 3), 2));
                        } else if (frames == 28) {
                            GpuBoardTestUi.capture(new File(output, "woods-light-grass.png"));
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void dispose() {
                    terrain.dispose();
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static int waterfallPixels(BoardCamera camera, BoardScene scene) {
        BoardSurface.Side drop = new BoardSurface(scene, scene.tile(new Coords(2, 2))).waterfalls.getFirst();
        Vector3 point = new Vector3(drop.a()).lerp(drop.b(), 0.5f);
        point.z = (point.z + drop.lowA()) / 2;
        Vector3 screen = camera.camera.project(point);
        Pixmap pixels = ScreenUtils.getFrameBufferPixmap((int) screen.x - 2, (int) screen.y - 2, 5, 5);
        try {
            int[] colors = new int[25];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = pixels.getPixel(i % 5, i / 5);
            }
            return Arrays.hashCode(colors);
        } finally {
            pixels.dispose();
        }
    }
}
