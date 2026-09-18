/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.JsonReader;
import megamek.common.Configuration;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native integration: atlas updates, unit shadows, authored models, and transparent water/features. */
@Tag("on-demand")
class GpuResourcesSmokeTest {
    @Test
    void rendersModelsWaterAndUnitsInTheCorrectPasses() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkAtlas();
                    checkAssets();
                    checkFeatureAndWaterTransparency();
                    checkMeepleShadows();
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private void checkAssets() {
        GpuAssets assets = new GpuAssets();
        try {
            List<String> names = new ArrayList<>();
            var manifest = new JsonReader().parse(new FileHandle(new File(Configuration.dataDir(), "models/board/manifest.json")));
            manifest.forEach(asset -> names.add(asset.name));
            names.addAll(List.of("buildings/saxarba/building_hard/building_hard_00",
                  "buildings/saxarba/building_hard/building_hard_01",
                  "buildings/saxarba/building_hard/building_hard_09"));
            for (String name : names) {
                var model = assets.model(name);
                assertSame(model, assets.model(name), "Asset geometry is shared");
                assertTrue(model.meshes.first().getNumIndices() / 3 <= 500, name);
                BoundingBox bounds = model.calculateBoundingBox(new BoundingBox());
                assertTrue(bounds.isValid() && bounds.getWidth() > 0 && bounds.getDepth() > 0, name);
            }
            for (int depth = 0; depth <= 4; depth++) {
                assertNotSame(assets.water(depth, 0), assets.water(depth, 0.25f), "GIF frames must advance");
            }
        } finally {
            assets.dispose();
        }
    }

    private void checkFeatureAndWaterTransparency() {
        GpuTerrain terrain = new GpuTerrain();
        ModelBatch units = new ModelBatch();
        var model = new ModelBuilder().createBox(14, 14, 10,
              new Material(ColorAttribute.createDiffuse(Color.RED)),
              VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ModelInstance unit = new ModelInstance(model);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        BoardScene.Pixels art = hexPixels(new java.awt.Color(119, 137, 75));
        Coords target = new Coords(2, 2);
        try {
            for (int depth : new int[] { -1, 0, 1, 2 }) {
                List<BoardScene.Tile> tiles = new ArrayList<>();
                for (int x = 0; x < 5; x++) {
                    for (int y = 0; y < 5; y++) {
                        Coords coords = new Coords(x, y);
                        tiles.add(new BoardScene.Tile(coords, x == 4 ? 2 : 0,
                              coords.equals(target) ? depth : -1, false, 0, BoardScene.Surface.GRASS, art, null, null,
                              coords.equals(target) && depth < 0
                                    ? List.of(new BoardScene.Feature("buildings/saxarba/building_hard/building_hard_00", 0, 0, 0, 1, 3, 0)) : List.of(),
                              List.of()));
                    }
                }
                BoardScene scene = new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of());
                terrain.update(scene);
                camera.setIsometric(false);
                camera.fit(scene);
                Vector3 center = BoardGeometry.center(target, 0);
                float bed = BoardGeometry.groundZ(scene.tile(target));
                if (depth < 0) {
                    Vector3 roof = new Vector3(center.x, center.y, 3 * BoardGeometry.LEVEL);
                    Vector3 eye = new Vector3(roof).add(-90, 0, 60);
                    assertEquals(target, terrain.pick(scene, new Ray(eye, new Vector3(roof).sub(eye))),
                          "Clicking a raised roof must select its own hex, not the ground behind it");
                }
                unit.transform.setToTranslation(center.x, center.y, bed + 5.5f);
                terrain.animate(0, List.of());
                drawMeeple(terrain, camera, units, unit);
                terrain.renderTransparent(camera.camera);
                int obscured = rgba(camera, new Vector3(center.x, center.y, bed + 10));
                if (depth < 0) {
                    GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), "building-opaque.png"));
                }
                terrain.animate(0.1f, List.of(unit));
                drawMeeple(terrain, camera, units, unit);
                terrain.renderTransparent(camera.camera);
                int visible = rgba(camera, new Vector3(center.x, center.y, bed + 10));
                int red = visible >>> 24;
                int green = (visible >>> 16) & 255;
                assertTrue(red > green + 45, "Unit must remain visible inside a feature/water: depth=" + depth);
                if (depth < 0) {
                    GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), "building-faded.png"));
                    assertTrue((obscured >>> 24) < ((obscured >>> 16) & 255) + 10,
                          "The unoccupied roof must occlude the red unit: before="
                          + Integer.toHexString(obscured) + ", after=" + Integer.toHexString(visible));
                    terrain.animate(0, List.of());
                    drawMeeple(terrain, camera, units, unit);
                    assertEquals(obscured, rgba(camera, new Vector3(center.x, center.y, bed + 10)),
                          "Opacity restores when a visible unit leaves");
                    terrain.animate(0, List.of(unit));
                }
                camera.setIsometric(true);
                camera.fit(scene);
                drawMeeple(terrain, camera, units, unit);
                terrain.renderTransparent(camera.camera);
                File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                assertTrue(output.isDirectory() || output.mkdirs());
                GpuBoardTestUi.capture(new File(output, depth < 0 ? "feature-visibility.png" : "water-depth-" + depth + ".png"));
            }
        } finally {
            terrain.dispose();
            units.dispose();
            model.dispose();
        }
    }

    private void checkAtlas() {
        GpuTextures<String> atlas = new GpuTextures<>();
        try {
            for (int[] size : new int[][] { { 2042, 8 }, { 2043, 8 }, { 2048, 8 }, { 8, 2043 },
                  { 3840, 2160 }, { 1280, 800 } }) {
                BoardScene.Pixels pixels = new BoardScene.Pixels(new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB));
                assertTrue(atlas.update(Map.of("hud", pixels)));
                assertEquals(size[0], atlas.region("hud").getRegionWidth());
                assertEquals(size[1], atlas.region("hud").getRegionHeight());
                assertFalse(atlas.update(Map.of("hud", pixels)));
                BoardScene.Pixels changed = new BoardScene.Pixels(new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB));
                assertFalse(atlas.update(Map.of("hud", changed)), "A pixel update must retain the atlas layout");
                assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            }
        } finally {
            atlas.dispose();
        }
    }

    private BoardScene.Pixels hexPixels(java.awt.Color color) {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillPolygon(new int[] { 84, 63, 21, 0, 21, 63 }, new int[] { 36, 0, 0, 36, 72, 72 }, 6);
        graphics.dispose();
        return new BoardScene.Pixels(image);
    }

    private void checkMeepleShadows() {
        BufferedImage artwork = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = artwork.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(30, 24, 24, 24);
        graphics.dispose();
        BoardScene.Pixels tokenPixels = new BoardScene.Pixels(artwork);
        BufferedImage ground = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        graphics = ground.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        GpuTextures<String> atlas = new GpuTextures<>();
        GpuTerrain terrain = new GpuTerrain();
        ModelBatch batch = new ModelBatch();
        atlas.update(Map.of("token", tokenPixels));
        GpuMeeple meeple = new GpuMeeple(tokenPixels, atlas.region("token"));
        ModelInstance unit = new ModelInstance(meeple.instance.model);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        BoardScene.Pixels floor = new BoardScene.Pixels(ground);
        Vector3 position = BoardGeometry.center(new Coords(2, 2), 0);
        try {
            for (float direction : new float[] { 38, -38 }) {
                BoardScene scene = shadowScene(floor, 0, new BoardScene.Light(direction, 0));
                terrain.update(scene);
                camera.setIsometric(false);
                camera.fit(scene);
                meeple.place(unit, camera.camera, position, 0, 2);
                BoundingBox bounds = unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform);
                assertEquals(2 * BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE, bounds.getDepth(), 0.001f);
                assertEquals(0.5f, bounds.min.z, 0.001f);
                assertEquals(24 * BoardGeometry.UNIT_SCALE, bounds.getWidth(), 0.01f);
                assertEquals(24 * BoardGeometry.UNIT_SCALE, bounds.getHeight(), 0.01f,
                      "Token size must follow its artwork, not the hex scale");
                terrain.renderShadows(List.of(unit));
                drawMeeple(terrain, camera, batch, unit);
                int left = brightness(camera, -35);
                int right = brightness(camera, 35);
                assertTrue(direction > 0 ? right < left - 20 : left < right - 20,
                      "Meeple shadow must follow light: left=" + left + ", right=" + right);
            }
            BoardGeometry.Tuning original = unitScaleTuning(BoardGeometry.UNIT_SCALE);
            try {
                BoardGeometry.tune(unitScaleTuning(2f));
                meeple.place(unit, camera.camera, position, 0, 2);
                assertEquals(48, unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform).getWidth(), 0.01f,
                      "The unit scale must size the token footprint");
            } finally {
                BoardGeometry.tune(original);
            }
            meeple.place(unit, camera.camera, position, 0, 1);
            assertEquals(BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE,
                  unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform).getDepth(), 0.001f);
            meeple.place(unit, camera.camera, new Vector3(position).add(0, -100, 0), 0, 2);
            terrain.renderShadows(List.of(unit));
            drawMeeple(terrain, camera, batch, unit);
            assertEquals(brightness(camera, -35), brightness(camera, 35), 3,
                  "Moving a meeple must clear its old shadow");

            camera.setIsometric(true);
            camera.center(position);
            camera.zoom(0.4f);
            meeple.place(unit, camera.camera, position, 0, 2);
            int[] sideBrightness = new int[2];
            int index = 0;
            for (float direction : new float[] { 38, -38 }) {
                terrain.update(shadowScene(floor, 0, new BoardScene.Light(direction, 0)));
                terrain.renderShadows(List.of(unit));
                drawMeeple(terrain, camera, batch, unit);
                sideBrightness[index++] = brightness(camera, new Vector3(position).add(12, 0, BoardGeometry.LEVEL));
            }
            assertTrue(Math.abs(sideBrightness[0] - sideBrightness[1]) > 20,
                  "Meeple side normals must respond to light direction");
            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(output.isDirectory() || output.mkdirs());
            GpuBoardTestUi.capture(new File(output, "meeple-shadow.png"));
        } finally {
            meeple.dispose();
            batch.dispose();
            terrain.dispose();
            atlas.dispose();
        }
    }

    private BoardScene shadowScene(BoardScene.Pixels pixels, int elevation, BoardScene.Light light) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 2 && y == 2 ? elevation : 0, -1, false, 0, BoardScene.Surface.GRASS,
                      pixels, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of(), light);
    }

    private void drawMeeple(GpuTerrain terrain, BoardCamera camera, ModelBatch batch, ModelInstance unit) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        batch.render(unit, terrain.environment());
        batch.end();
    }

    private int rgba(BoardCamera camera, Vector3 position) {
        // Camera.project projects in place, so the caller's vector must not be handed to it.
        Vector3 point = camera.camera.project(new Vector3(position),
              0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Pixmap sample = Pixmap.createFromFrameBuffer((int) point.x, (int) point.y, 1, 1);
        try {
            return sample.getPixel(0, 0);
        } finally {
            sample.dispose();
        }
    }

    private int brightness(BoardCamera camera, float dx) {
        return brightness(camera, BoardGeometry.center(new Coords(2, 2), 0).add(dx, 0, 0));
    }

    private int brightness(BoardCamera camera, Vector3 position) {
        return rgba(camera, position) >>> 24;
    }

    private static BoardGeometry.Tuning unitScaleTuning(float scale) {
        BoardGeometry.Tuning current = BoardGeometry.tuning();
        return new BoardGeometry.Tuning(current.hexScale(), scale, current.unitHeightScale(),
              current.levelHeight(), current.gridShade());
    }
}
