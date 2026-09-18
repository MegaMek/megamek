/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual Saxarba themes at several cliff heights, with shallow/deep beds and all six small rim maps. */
@Tag("on-demand")
class GpuTerrainMaterialsSmokeTest {
    @Test
    void rendersSmallRimMaterialsAndRiverbedsAcrossThemes() throws Exception {
        String[] themes = { "grass", "mars", "desert", "lunar", "grass", "snow" };
        Hex[] hexes = new Hex[18 * 6];
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 18; x++) {
                Hex hex = new Hex(Math.max(0, 3 - y));
                hex.setTheme(themes[x / 3]);
                if (y >= 4) {
                    hex.addTerrain(new Terrain(Terrains.WATER, y - 4));
                } else if (x / 3 == 4) {
                    hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1));
                } else if (x / 3 == 5) {
                    hex.addTerrain(new Terrain(Terrains.SNOW, 2));
                }
                hexes[y * 18 + x] = hex;
            }
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(new Board(18, 6, hexes))) {
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            BoardScene scene = fixture.source.takeFrame().scene();
            for (int family = 0; family < 6; family++) {
                assertEquals(BoardScene.Surface.values()[family], scene.tile(new Coords(family * 3 + 1, 2)).surface());
            }
            new Lwjgl3Application(new ApplicationAdapter() {
                GpuTerrain terrain;
                BoardCamera camera;
                int frame;
                int concreteRimPixel;

                @Override
                public void create() {
                    try {
                        GpuAssets assets = new GpuAssets();
                        try {
                            for (BoardScene.Surface surface : BoardScene.Surface.values()) {
                                Texture texture = assets.material(surface.rim);
                                assertEquals(128, texture.getWidth());
                                assertEquals(128, texture.getHeight());
                                assertEquals(Texture.TextureWrap.Repeat, texture.getVWrap());
                            }
                            assertEquals(128, assets.material("bed").getWidth());
                        } finally {
                            assets.dispose();
                        }
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
                        terrain.animate(0, List.of());
                        terrain.renderShadows(List.of());
                        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
                        terrain.render(camera.camera, false);
                        terrain.renderTransparent(camera.camera);
                        if (frame <= 6) {
                            String name = frame == 0 ? "terrain-rims-isometric"
                                  : frame == 5 ? "terrain-concrete-rim-straight"
                                  : "terrain-" + BoardScene.Surface.values()[frame - 1].rim;
                            GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), name + ".png"));
                        }
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        if (frame == 6) {
                            terrain.update(tintScene(0xff32b432));
                            camera.camera.zoom = 0.2f;
                            camera.center(BoardGeometry.center(new Coords(0, 0), 3));
                        } else if (frame == 7) {
                            int pixel = rimPixel(camera);
                            assertTrue(((pixel >>> 16) & 255) > (pixel >>> 24) * 2, "The rim starts green");
                            terrain.update(tintScene(0xffb43232));
                        } else if (frame == 8) {
                            int pixel = rimPixel(camera);
                            assertTrue((pixel >>> 24) > ((pixel >>> 16) & 255) * 2,
                                  "Changing only ground pixels must refresh the rim's tint without a layout change");
                            terrain.update(pavedStep(true));
                        } else if (frame == 9) {
                            concreteRimPixel = rimPixel(camera);
                            assertTrue(((concreteRimPixel >>> 16) & 255) > (concreteRimPixel >>> 24) * 2,
                                  "Concrete retains its ground-tinted rim even between paved hexes");
                            terrain.update(pavedStep(false));
                        } else if (frame == 10) {
                            assertEquals(concreteRimPixel, rimPixel(camera),
                                  "The concrete rim also remains present next to natural terrain");
                            Gdx.app.exit();
                        } else if (frame < 6) {
                            boolean concretePreview = frame == 4;
                            camera.camera.zoom = concretePreview ? 0.2f : 0.35f;
                            camera.center(BoardGeometry.center(new Coords(frame * 3 + 1, concretePreview ? 1 : 2),
                                  concretePreview ? 2 : 1));
                        }
                        frame++;
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
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static BoardScene tintScene(int argb) {
        BoardScene.Tile tile = new BoardScene.Tile(new Coords(0, 0), 3, -1, false, 0, BoardScene.Surface.GRASS,
              solidPixels(argb), null, null, List.of(), List.of());
        return new BoardScene(0, 1, 1, List.of(tile), List.of(), List.of(), -1, "", List.of());
    }

    private static BoardScene pavedStep(boolean pavedNeighbor) {
        // Green top artwork makes the rim distinguishable from the neutral concrete side.
        BoardScene.Pixels pixels = solidPixels(0xff32b432);
        BoardScene.Tile high = new BoardScene.Tile(new Coords(0, 0), 3, -1, false, 0, BoardScene.Surface.CONCRETE,
              pixels, null, null, List.of(), List.of());
        BoardScene.Tile low = new BoardScene.Tile(new Coords(0, 1), 0, -1, false, 0,
              pavedNeighbor ? BoardScene.Surface.CONCRETE : BoardScene.Surface.GRASS,
              pixels, null, null, List.of(), List.of());
        return new BoardScene(0, 1, 2, List.of(high, low), List.of(), List.of(), -1, "", List.of());
    }

    private static BoardScene.Pixels solidPixels(int argb) {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return new BoardScene.Pixels(image);
    }

    private static int rimPixel(BoardCamera camera) {
        Coords coords = new Coords(0, 0);
        Vector3 point = BoardGeometry.corner(coords, 3, 4).lerp(BoardGeometry.corner(coords, 3, 5), 0.5f);
        point.add(0, -0.06f, -3);
        Vector3 screen = camera.camera.project(point);
        Pixmap pixels = ScreenUtils.getFrameBufferPixmap((int) screen.x, (int) screen.y, 1, 1);
        try {
            return pixels.getPixel(0, 0);
        } finally {
            pixels.dispose();
        }
    }
}
