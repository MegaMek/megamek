/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Visual integration of the authored catalog with actual Saxarba ground captures. */
@Tag("on-demand")
class GpuAssetCatalogSmokeTest {
    @Test
    void rendersTheAuthoredCatalogOnTheSharedBoardScene() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Hex[] hexes = new Hex[8 * 8];
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        Hex hex = new Hex(x >= 6 ? 2 : y == 7 ? 1 : 0);
                        if (x <= 1) {
                            hex.addTerrain(new Terrain(Terrains.WATER, y % 3));
                            if (x == 1 && y == 0) {
                                hex.addTerrain(new Terrain(Terrains.ICE, 1));
                            }
                        }
                        if (x >= 6 && y <= 2) {
                            hex.setTheme("snow");
                            hex.addTerrain(new Terrain(Terrains.SNOW, 1));
                        } else if (x >= 6 && y <= 5) {
                            hex.setTheme("desert");
                            hex.addTerrain(new Terrain(Terrains.SAND, 1));
                        } else if (x == 6 && y == 6) {
                            hex.setTheme("volcano");
                        } else if (x == 7 && y == 6) {
                            hex.setTheme("lunar");
                        } else if (x == 6 && y == 7) {
                            hex.setTheme("mars");
                        } else if (x == 7 && y == 7) {
                            hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1));
                        }
                        if (x == 3 && y >= 1 && y <= 4) {
                            hex.addTerrain(new Terrain(Terrains.BUILDING, 4, true, 0));
                            hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, y));
                            hex.addTerrain(new Terrain(Terrains.BLDG_CF, 100));
                        }
                        if (x == 4 && (y == 5 || y == 6)) {
                            hex.addTerrain(new Terrain(Terrains.BUILDING, 3, true, y == 5 ? 8 : 1));
                            hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, y == 5 ? 2 : 3));
                            hex.addTerrain(new Terrain(Terrains.BLDG_CF, 80));
                        }
                        if (x == 6 && (y == 1 || y == 4 || y == 6)) {
                            hex.addTerrain(new Terrain(Terrains.WOODS, 2));
                            hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
                        }
                        if (x <= 1 && y == 4) {
                            hex.addTerrain(new Terrain(Terrains.BRIDGE, 2, true, x == 0 ? 36 : 18));
                            hex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, 1));
                            hex.addTerrain(new Terrain(Terrains.BRIDGE_CF, 60));
                        }
                        if (x == 2 && y == 6) {
                            hex.addTerrain(new Terrain(Terrains.FUEL_TANK, 2));
                            hex.addTerrain(new Terrain(Terrains.FUEL_TANK_ELEV, 2));
                        }
                        if (x == 2 && y == 7) {
                            hex.addTerrain(new Terrain(Terrains.INDUSTRIAL, 3));
                        }
                        if (x == 5 && y == 2) {
                            hex.addTerrain(new Terrain(Terrains.FIELDS, 1));
                        }
                        if (y == 7 && x >= 3) {
                            hex.addTerrain(new Terrain(Terrains.ROAD, 1, true, (1 << 1) | (1 << 2) | (1 << 4) | (1 << 5)));
                        }
                        if (x == 5 && y == 3) {
                            // This road reaches the higher desert hex, which has no reciprocal road terrain.
                            hex.addTerrain(new Terrain(Terrains.ROAD, 1, true, 1 << 1));
                        }
                        hexes[y * 8 + x] = hex;
                    }
                }
                fixture.game.setBoard(new Board(8, 8, hexes));
                fixture.source.refresh();
            });
            BoardScene captured = fixture.source.takeFrame().scene();
            BoardScene scene = new BoardScene(0, 8, 8, captured.tiles(), List.of(), List.of(), -1, "", List.of(),
                  new BoardScene.Light(-24, -30));
            new Lwjgl3Application(new ApplicationAdapter() {
                GpuTerrain terrain;
                BoardCamera camera;
                int frames;

                @Override
                public void create() {
                    try {
                        terrain = new GpuTerrain();
                        terrain.update(scene);
                        camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        camera.setIsometric(true);
                        camera.fit(scene);
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void render() {
                    if (failure.get() != null) {
                        return;
                    }
                    try {
                        terrain.animate(1f / 60, List.of());
                        terrain.renderShadows(List.of());
                        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
                        terrain.render(camera.camera, false);
                        terrain.renderTransparent(camera.camera);
                        if (++frames == 4) {
                            File output = new File(System.getProperty("megamek.gpu.screenshots"));
                            GpuBoardTestUi.capture(new File(output, "asset-catalog.png"));
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
                    if (terrain != null) {
                        terrain.dispose();
                    }
                }
            }, GpuBoardWindow.configuration(false));
            assertNull(failure.get(), () -> String.valueOf(failure.get()));
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                @Override
                public void render() {
                    try {
                        super.render();
                        File output = new File(System.getProperty("megamek.gpu.screenshots"));
                        if (frames() == 3) {
                            GpuBoardTestUi.capture(new File(output, "building-roofs-top.png"));
                            boardCamera.setIsometric(true);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (frames() == 6) {
                            GpuBoardTestUi.capture(new File(output, "building-roofs-isometric.png"));
                            boardCamera.center(BoardGeometry.center(new Coords(5, 3), 1));
                            boardCamera.zoom(0.34f);
                        } else if (frames() == 9) {
                            GpuBoardTestUi.capture(new File(output, "terrain-edges-closeup.png"));
                            boardCamera.orbit(180, 0);
                        } else if (frames() == 12) {
                            GpuBoardTestUi.capture(new File(output, "terrain-edges-reverse.png"));
                            boardCamera.center(BoardGeometry.center(new Coords(6, 6), 2));
                        } else if (frames() == 15) {
                            GpuBoardTestUi.capture(new File(output, "cornices-volcano-lunar-mars-concrete.png"));
                            boardCamera.orbit(180, 0);
                        } else if (frames() == 18) {
                            GpuBoardTestUi.capture(new File(output, "cornices-volcano-lunar-mars-concrete-reverse.png"));
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
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
