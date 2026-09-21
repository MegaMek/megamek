/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual terrain artwork, paired atlas edits and per-pixel lighting in a native OpenGL context. */
@Tag("on-demand")
class GpuTerrainNormalsSmokeTest {
    @Test
    void rendersPreGeneratedNormalsAndPreviewsRubbleRoughAndGrass() throws Exception {
        Hex[] hexes = new Hex[15 * 9];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 15; x++) {
                Hex hex = new Hex(0);
                hex.setTheme("grass");
                if (x < 10) {
                    hex.addTerrain(new Terrain(x < 5 ? Terrains.RUBBLE : Terrains.ROUGH, 1));
                }
                if (y == 8) {
                    hex.removeAllTerrains();
                    hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1));
                }
                hexes[y * 15 + x] = hex;
            }
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(new Board(15, 9, hexes))) {
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            BoardScene captured = fixture.source.takeFrame().scene();
            BoardScene scene = new BoardScene(0, 15, 9, captured.tiles(), List.of(), List.of(), -1, "", List.of(),
                  new BoardScene.Light(30, -20));
            for (BoardScene.Tile tile : scene.tiles()) {
                assertNotNull(tile.normals(), "Every captured ground image carries its pre-generated normal layer");
                assertTrue(tile.features().stream().allMatch(feature -> feature.kind() == BoardScene.FeatureKind.SCATTER),
                      "Rubble and rough add only sparse decoration to their ground artwork");
                assertNull(tile.decals(), "Painted stones belong in the ground, not an overlay hiding its lighting");
            }
            var configuration = GpuBoardWindow.configuration(false);
            configuration.setWindowedMode(1440, 1000);
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    GpuTerrain terrain = null;
                    GpuBoardSkin skin = null;
                    Stage stage = null;
                    try {
                        checkAtlas();
                        skin = new GpuBoardSkin();
                        GpuBoardTuning tuning = new GpuBoardTuning(skin.skin);
                        CheckBox toggle = tuning.panel().findActor("tuning-normal-maps");
                        assertTrue(tuning.normalMaps(), "Normal maps start enabled");
                        stage = new Stage(new ScreenViewport());
                        stage.addActor(tuning.panel());
                        var dock = new GpuPanelDock(skin.skin, () -> { }, null, tuning.panel());
                        dock.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 0, 0);
                        dock.show(tuning.panel());
                        Gdx.input.setInputProcessor(new InputMultiplexer(stage));
                        stage.act(0);
                        stage.draw();
                        GpuBoardTestUi.click("tuning-normal-maps");
                        assertFalse(tuning.normalMaps(), "The checkbox accepts a real pointer click");
                        GpuBoardTestUi.click("tuning-defaults");
                        assertTrue(tuning.normalMaps(), "Defaults restores normal mapping");
                        terrain = new GpuTerrain();
                        BoardCamera camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        terrain.update(scene);
                        terrain.renderShadows(List.of());
                        BoardScene flat = flatNormals(scene);
                        for (int family = 0; family < 4; family++) {
                            Coords target = family == 3 ? new Coords(7, 8) : new Coords(family * 5 + 2, 4);
                            camera.setIsometric(false);
                            camera.camera.zoom = 0.2f;
                            camera.center(BoardGeometry.center(target, 0));
                            terrain.update(scene);
                            int[] mapped = samples(terrain, camera, target);
                            toggle.setChecked(false);
                            terrain.setNormalMaps(tuning.normalMaps());
                            int[] disabled = samples(terrain, camera, target);
                            toggle.setChecked(true);
                            terrain.setNormalMaps(tuning.normalMaps());
                            assertArrayEquals(mapped, samples(terrain, camera, target),
                                  "Re-enabling restores the same lighting without a scene or atlas update");
                            terrain.update(flat);
                            int[] neutral = samples(terrain, camera, target);
                            assertArrayEquals(neutral, disabled, "Disabling normals matches the flat map reference");
                            if (family == 3) {
                                assertArrayEquals(neutral, mapped, "Pavement must retain exactly flat lighting");
                            } else {
                                int changed = 0;
                                for (int index = 0; index < mapped.length; index++) {
                                    if (mapped[index] != neutral[index]) {
                                        changed++;
                                    }
                                }
                                assertTrue(changed > mapped.length / 4, "Normal textures must affect lighting for family " + family);
                            }
                        }
                        terrain.update(scene);
                        terrain.renderShadows(List.of());
                        preview(terrain, camera, true);
                        toggle.setChecked(false);
                        terrain.setNormalMaps(tuning.normalMaps());
                        preview(terrain, camera, false);
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        if (terrain != null) {
                            terrain.dispose();
                        }
                        if (stage != null) {
                            stage.dispose();
                        }
                        if (skin != null) {
                            skin.dispose();
                        }
                        Gdx.app.exit();
                    }
                }
            }, configuration);
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static void checkAtlas() {
        GpuTextures<String> atlas = new GpuTextures<>(true);
        BoardScene.Pixels color = pixels(0xffdddddd);
        BoardScene.Pixels flat = pixels(0xff8080ff);
        BoardScene.Pixels tilted = pixels(0xff4080ee);
        Map<String, BoardScene.Pixels> colors = Map.of("a", color, "b", color);
        try {
            assertTrue(atlas.update(colors, Map.of("a", flat, "b", flat)));
            assertSame(atlas.region("a"), atlas.region("b"));
            TextureRegion before = atlas.region("a");
            assertFalse(atlas.update(colors, Map.of("a", tilted, "b", tilted)), "Normal-only edits preserve mesh UVs");
            assertSame(before, atlas.region("a"));
            assertNormalPixel(atlas, "a", 0x4080eeff);
            assertTrue(atlas.update(colors, Map.of("a", tilted, "b", flat)), "Normal aliases must split even with identical albedo");
            assertNotSame(atlas.region("a"), atlas.region("b"));
            assertNormalPixel(atlas, "a", 0x4080eeff);
            assertNormalPixel(atlas, "b", 0x8080ffff);
            assertTrue(atlas.update(colors, Map.of("a", flat, "b", flat)));
            assertSame(atlas.region("a"), atlas.region("b"));
            assertTrue(atlas.update(Map.of()));
            assertFalse(atlas.update(Map.of()));
        } finally {
            atlas.dispose();
        }
    }

    private static void assertNormalPixel(GpuTextures<String> atlas, String key, int rgba) {
        TextureRegion color = atlas.region(key);
        Texture normal = atlas.normal(color.getTexture());
        assertEquals(color.getTexture().getWidth(), normal.getWidth());
        Pixmap page = normal.getTextureData().consumePixmap();
        assertEquals(rgba, page.getPixel(color.getRegionX(), color.getRegionY()));
        assertEquals(rgba, page.getPixel(color.getRegionX() - 1, color.getRegionY()), "Refresh the atlas bleed too");
    }

    private static BoardScene flatNormals(BoardScene scene) {
        BoardScene.Pixels flat = pixels(0xff8080ff);
        List<BoardScene.Tile> tiles = scene.tiles().stream().map(tile -> new BoardScene.Tile(tile.coords(), tile.elevation(),
              tile.waterDepth(), tile.frozen(), tile.roadExits(), tile.surface(), tile.ground(), flat, tile.decals(),
              tile.tactical(), tile.features(), tile.text())).toList();
        return new BoardScene(0, scene.width(), scene.height(), tiles, List.of(), List.of(), -1, "", List.of(), scene.light());
    }

    private static BoardScene.Pixels pixels(int argb) {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return new BoardScene.Pixels(image);
    }

    private static int[] samples(GpuTerrain terrain, BoardCamera camera, Coords target) {
        ScreenUtils.clear(0.03f, 0.045f, 0.06f, 1, true);
        terrain.render(camera.camera, false);
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            int[] samples = new int[21 * 21];
            for (int y = 0; y < 21; y++) {
                for (int x = 0; x < 21; x++) {
                    Vector3 screen = camera.camera.project(BoardGeometry.center(target, 0).add(x - 10, y - 10, 0));
                    samples[y * 21 + x] = pixels.getPixel(HdpiUtils.toBackBufferX((int) screen.x),
                          HdpiUtils.toBackBufferY((int) screen.y));
                }
            }
            return samples;
        } finally {
            pixels.dispose();
        }
    }

    private static void preview(GpuTerrain terrain, BoardCamera camera, boolean mapped) {
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        int panelWidth = width / 3, panelHeight = (height - 130) / 2;
        ScreenUtils.clear(0.03f, 0.045f, 0.06f, 1, true);
        for (int row = 0; row < 2; row++) {
            for (int family = 0; family < 3; family++) {
                HdpiUtils.glViewport(family * panelWidth + 8, row * (panelHeight + 35) + 15, panelWidth - 16, panelHeight);
                camera.resize(panelWidth - 16, panelHeight);
                camera.setIsometric(row == 1);
                camera.camera.zoom = row == 1 ? 0.3f : 0.27f;
                camera.center(BoardGeometry.center(new Coords(family * 5 + 2, 4), 0));
                terrain.render(camera.camera, false);
            }
        }
        HdpiUtils.glViewport(0, 0, width, height);
        SpriteBatch labels = new SpriteBatch();
        BitmapFont font = new BitmapFont();
        try {
            font.getData().setScale(1.5f);
            labels.begin();
            String[] names = { "RUBBLE", "DIFFICULT / ROUGH", "GRASSLAND" };
            for (int family = 0; family < 3; family++) {
                font.draw(labels, names[family], family * panelWidth + 22, height - 22);
            }
            font.getData().setScale(1.1f);
            font.draw(labels, mapped ? "ISOMETRIC  /  PRE-GENERATED NORMAL MAPS" : "ISOMETRIC  /  FLAT LIGHTING REFERENCE",
                  22, height - 57);
            font.draw(labels, "TOP VIEW  /  SAME TERRAIN AND LIGHTING", 22, panelHeight + 39);
            labels.end();
            File directory = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(directory.isDirectory() || directory.mkdirs());
            GpuBoardTestUi.capture(new File(directory, mapped ? "terrain-normal-preview.png" : "terrain-flat-reference.png"));
        } finally {
            font.dispose();
            labels.dispose();
        }
    }
}
