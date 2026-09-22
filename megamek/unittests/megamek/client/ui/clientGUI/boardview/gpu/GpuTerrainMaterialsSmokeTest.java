/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    /** Edge length of the skirt region the run-off check fingerprints, in pixels. */
    private static final int REGION = 64;

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
                int skirtPixel;
                int drySkirtPixel;
                long wetSkirtRegion;
                int concreteRimPixel;
                float delta;

                @Override
                public void create() {
                    try {
                        GpuAssets assets = new GpuAssets();
                        try {
                            for (BoardScene.Surface surface : BoardScene.Surface.values()) {
                                Texture texture = assets.cornice(surface.cornice);
                                // One hex edge wide; the mask's own height is the artwork's business.
                                assertEquals(128, texture.getWidth());
                                // U tiles along the edge; V stays inside the strip and must not wrap.
                                assertEquals(Texture.TextureWrap.Repeat, texture.getUWrap());
                                assertEquals(Texture.TextureWrap.ClampToEdge, texture.getVWrap());
                            }
                            assertEquals(128, assets.material("terrain/water_bed").getWidth());
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
                        terrain.animate(delta, List.of());
                        terrain.renderShadows(List.of());
                        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
                        terrain.render(camera.camera, false);
                        terrain.renderTransparent(camera.camera);
                        if (frame <= 6) {
                            String name = frame == 0 ? "terrain-rims-isometric"
                                  : frame == 5 ? "terrain-concrete-rim-straight"
                                  : "terrain-skirt-" + BoardScene.Surface.values()[frame - 1].name();
                            GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), name + ".png"));
                        }
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        if (frame == 6) {
                            terrain.update(tintScene(0xff32b432));
                            camera.camera.zoom = 0.2f;
                            camera.center(BoardGeometry.center(new Coords(0, 0), 3));
                        } else if (frame == 7) {
                            skirtPixel = rimPixel(camera);
                            // The strip is a mask, so a new top-layer color must retint the skirt hanging from it.
                            terrain.update(tintScene(0xffb43232));
                        } else if (frame == 8) {
                            assertNotEquals(skirtPixel, rimPixel(camera),
                                  "Changing only ground pixels must retint the mask that hangs from them");
                            terrain.update(pavedStep(true));
                        } else if (frame == 9) {
                            concreteRimPixel = rimPixel(camera);
                            terrain.update(pavedStep(false));
                        } else if (frame == 10) {
                            assertEquals(concreteRimPixel, rimPixel(camera),
                                  "The concrete skirt also remains present next to natural terrain");
                            terrain.setWetness(0);
                            terrain.update(tintScene(0xff32b432));
                            // Close on the strip itself, so the dry and wet review frames show the run-off.
                            camera.camera.zoom = 0.07f;
                            camera.center(skirtPoint());
                        } else if (frame == 11) {
                            GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), "terrain-skirt-dry.png"));
                            drySkirtPixel = rimPixel(camera);
                            terrain.setWetness(1);
                        } else if (frame == 12) {
                            GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), "terrain-skirt-wet.png"));
                            assertNotEquals(drySkirtPixel, rimPixel(camera),
                                  "A wet cliff must take the same rain film as the ground it faces");
                            wetSkirtRegion = skirtRegion(camera);
                            // Two seconds of run-off: the rivulets must have travelled down by the next frame.
                            delta = 2;
                        } else if (frame == 13) {
                            assertNotEquals(wetSkirtRegion, skirtRegion(camera),
                                  "Run-off must travel down a wet cliff");
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
        // Both tiles share one top-layer color, so only the neighbours' own skirts can differ.
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

    /** The skirt patch every check samples: mid-edge, clear of the wall and below the cliff top. */
    private static Vector3 skirtPoint() {
        Coords coords = new Coords(0, 0);
        Vector3 point = BoardGeometry.corner(coords, 3, 4).lerp(BoardGeometry.corner(coords, 3, 5), 0.5f);
        return point.add(0, -0.06f, -3);
    }

    private static int rimPixel(BoardCamera camera) {
        Vector3 screen = camera.camera.project(skirtPoint());
        Pixmap pixels = ScreenUtils.getFrameBufferPixmap((int) screen.x, (int) screen.y, 1, 1);
        try {
            return pixels.getPixel(0, 0);
        } finally {
            pixels.dispose();
        }
    }

    /** Fingerprint of the skirt band around the sample point, so no travelling pattern is missed to its phase. */
    private static long skirtRegion(BoardCamera camera) {
        Vector3 screen = camera.camera.project(skirtPoint());
        int originX = Math.max(0, (int) screen.x - REGION / 2), originY = Math.max(0, (int) screen.y - REGION / 2);
        Pixmap pixels = ScreenUtils.getFrameBufferPixmap(originX, originY, REGION, REGION);
        try {
            long hash = 0;
            for (int y = 0; y < REGION; y++) {
                for (int x = 0; x < REGION; x++) {
                    hash = hash * 31 + pixels.getPixel(x, y);
                }
            }
            return hash;
        } finally {
            pixels.dispose();
        }
    }
}
