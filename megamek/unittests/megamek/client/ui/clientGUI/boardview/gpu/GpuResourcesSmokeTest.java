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
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
                    checkInteriorCourtyard();
                    checkChunkPicking();
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

    private void checkChunkPicking() {
        BoardScene.Pixels art = hexPixels(java.awt.Color.GREEN);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 17; x++) {
            for (int y = 0; y < 17; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 8 ? 3 : 0, -1, false, 0,
                      BoardScene.Surface.GRASS, art, null, null, List.of(), List.of()));
            }
        }
        BoardScene scene = new BoardScene(0, 17, 17, tiles, List.of(), List.of(), -1, "", List.of());
        GpuTerrain terrain = new GpuTerrain();
        BoardCamera camera = new BoardCamera();
        camera.resize(1000, 700);
        try {
            terrain.update(scene);
            for (boolean isometric : new boolean[] { false, true }) {
                camera.setIsometric(isometric);
                for (int x : new int[] { 0, 7, 8, 15, 16 }) {
                    for (int y : new int[] { 0, 7, 8, 15, 16 }) {
                        Coords coords = new Coords(x, y);
                        Vector3 target = BoardGeometry.center(coords, scene.tile(coords).elevation());
                        Ray ray = new Ray(new Vector3(target).mulAdd(camera.camera.direction, -1000), camera.camera.direction);
                        assertEquals(BoardGeometry.pick(scene, ray), terrain.pick(scene, ray),
                              "Chunk rejection must preserve surface picking at edges, cliffs and partial chunks");
                    }
                }
            }
        } finally {
            terrain.dispose();
        }
    }

    private void checkInteriorCourtyard() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        var mesh = builder.part("roof", GL20.GL_TRIANGLES,
              VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material());
        // Four wings around an open courtyard, with a detached annex outside the main footprint.
        for (float[] rect : new float[][] { { -30, -30, -10, 30 }, { 10, -30, 30, 30 },
              { -10, -30, 10, -10 }, { -10, 10, 10, 30 }, { 40, -8, 56, 8 } }) {
            mesh.rect(rect[0], rect[1], 1, rect[2], rect[1], 1,
                  rect[2], rect[3], 1, rect[0], rect[3], 1, 0, 0, 1);
        }
        Model shell = builder.end();
        Model interior = GpuBuildingInterior.build(shell, 3);
        try {
            List<Vector3> triangles = GpuTerrain.triangles(interior);
            for (float x : new float[] { 0, 35, 60 }) {
                assertFalse(Intersector.intersectRayTriangles(new Ray(new Vector3(x, 0, 2), new Vector3(0, 0, -1)),
                      triangles, new Vector3()), "Floors and struts must leave courtyard, gap and exterior open");
            }
            for (float x : new float[] { -25, 48 }) {
                assertTrue(Intersector.intersectRayTriangles(new Ray(new Vector3(x, 0, 2), new Vector3(0, 0, -1)),
                      triangles, new Vector3()), "Both main building and disconnected wing must have interiors");
            }
        } finally {
            interior.dispose();
            shell.dispose();
        }
    }

    private void checkFeatureAndWaterTransparency() {
        GpuTerrain terrain = new GpuTerrain();
        GpuBoardSkin skin = new GpuBoardSkin();
        GpuBoardTuning tuning = new GpuBoardTuning(skin.skin);
        Slider opacity = tuning.panel().findActor("Building opacity");
        Slider treeOpacity = tuning.panel().findActor("Tree opacity");
        assertEquals(0.5f, tuning.buildingOpacity());
        assertEquals(0.75f, tuning.treeOpacity());
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
                                    ? List.of(new BoardScene.Feature("buildings/saxarba/building_hard/building_hard_00",
                                          0, 0, 0, 1, 3, 0, BoardScene.FeatureKind.BUILDING)) : List.of(),
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
                if (depth < 0) {
                    GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), "building-faded.png"));
                }
                assertTrue(red > green + (depth < 0 ? 20 : 45),
                      "Unit must remain visible inside a feature/water: depth=" + depth + ", pixel=" + Integer.toHexString(visible));
                if (depth < 0) {
                    assertTrue((obscured >>> 24) < ((obscured >>> 16) & 255) + 10,
                          "The unoccupied roof must occlude the red unit: before="
                          + Integer.toHexString(obscured) + ", after=" + Integer.toHexString(visible));
                    int revision = BoardGeometry.revision();
                    int previousContrast = Integer.MAX_VALUE;
                    for (int percent : new int[] { 0, 25, 75, 100 }) {
                        opacity.setValue(percent);
                        terrain.animate(0, List.of(unit), tuning.buildingOpacity(), tuning.treeOpacity());
                        drawMeeple(terrain, camera, units, unit);
                        terrain.renderTransparent(camera.camera);
                        int pixel = rgba(camera, new Vector3(center.x, center.y, bed + 10));
                        int contrast = (pixel >>> 24) - ((pixel >>> 16) & 255);
                        assertTrue(contrast < previousContrast, "A stationary unit must respond to live opacity changes");
                        previousContrast = contrast;
                        if (percent == 0) {
                            assertTrue(contrast > 150, "Walls and upper floors must both fade away");
                        } else if (percent == 100) {
                            assertEquals(obscured, pixel, "100% restores opaque rendering while the unit remains inside");
                        }
                    }
                    assertEquals(revision, BoardGeometry.revision(), "Opacity must not rebuild board geometry");

                    // Sample a column's top in the opaque pass, then move the red unit directly beneath it.
                    Vector3 strutTop = new Vector3(center).add(13.5f, 0, 3 * BoardGeometry.LEVEL);
                    terrain.animate(0, List.of(unit), 0, 0.75f);
                    drawMeeple(terrain, camera, units, unit);
                    int solidStrut = rgba(camera, strutTop);
                    assertTrue(Math.abs((solidStrut >>> 24) - ((solidStrut >>> 16) & 255)) < 20);
                    unit.transform.setToTranslation(strutTop.x, strutTop.y, bed + 5.5f);
                    for (float alpha : new float[] { 0, 0.25f, 0.75f }) {
                        terrain.animate(0, List.of(unit), alpha, 0.75f);
                        drawMeeple(terrain, camera, units, unit);
                        assertEquals(solidStrut, rgba(camera, strutTop), "Struts must stay opaque and occlude units below");
                    }
                    unit.transform.setToTranslation(center.x, center.y, bed + 5.5f);
                    treeOpacity.setValue(25);
                    assertEquals(1, tuning.buildingOpacity(), "Tree tuning must not change the building setting");
                    tuning.panel().findActor("tuning-defaults").fire(new ChangeListener.ChangeEvent());
                    assertEquals(0.5f, tuning.buildingOpacity());
                    assertEquals(0.75f, tuning.treeOpacity());
                    terrain.animate(0, List.of());
                    drawMeeple(terrain, camera, units, unit);
                    assertEquals(obscured, rgba(camera, new Vector3(center.x, center.y, bed + 10)),
                          "Opacity restores when a visible unit leaves");
                    terrain.animate(0, List.of(unit));
                    checkSolidFeatureShadows(scene, camera, units, unit, center);
                }
                camera.setIsometric(true);
                camera.fit(scene);
                drawMeeple(terrain, camera, units, unit);
                terrain.renderTransparent(camera.camera);
                File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                assertTrue(output.isDirectory() || output.mkdirs());
                GpuBoardTestUi.capture(new File(output, depth < 0 ? "feature-visibility.png" : "water-depth-" + depth + ".png"));
                if (depth < 0) {
                    checkTreeOpacity(scene, camera, units, unit, target);
                }
            }
        } finally {
            skin.dispose();
            terrain.dispose();
            units.dispose();
            model.dispose();
        }
    }

    private void checkSolidFeatureShadows(BoardScene scene, BoardCamera camera, ModelBatch units,
          ModelInstance unit, Vector3 center) {
        BoardScene lit = new BoardScene(scene.boardId(), scene.width(), scene.height(), scene.tiles(),
              scene.units(), scene.plannedPath(), scene.selectedId(), scene.phase(), scene.commands(), new BoardScene.Light(38, 0));
        int[] solid = null;
        for (float opacity : new float[] { 1, 0.95f, 0.5f, 0 }) {
            // Start with an occupied, already-faded scene, forcing a fresh shadow map for each setting.
            GpuTerrain terrain = new GpuTerrain();
            try {
                terrain.update(lit);
                terrain.animate(0, List.of(unit), opacity, 0.75f);
                terrain.renderShadows(camera.camera, List.of(unit));
                drawMeeple(terrain, camera, units, unit);
                int[] samples = new int[3];
                for (int index = 0; index < samples.length; index++) {
                    samples[index] = rgba(camera, new Vector3(center).add(50 + index * 10, 0, 0));
                }
                assertTrue((samples[1] >>> 24) < brightness(camera, new Vector3(center).add(-60, 0, 0)) - 20,
                      "The complete building must shade the ground beyond its footprint");
                if (solid == null) {
                    solid = samples;
                } else {
                    assertArrayEquals(solid, samples, "Fading must retain the exact solid-building shadow at opacity " + opacity);
                }
            } finally {
                terrain.dispose();
            }
        }
    }

    private void checkTreeOpacity(BoardScene scene, BoardCamera camera, ModelBatch units, ModelInstance unit, Coords target) {
        List<BoardScene.Tile> tiles = new ArrayList<>(scene.tiles());
        BoardScene.Tile tile = scene.tile(target);
        tiles.set(target.getX() * scene.height() + target.getY(), new BoardScene.Tile(target, 0, -1, false, 0,
              tile.surface(), tile.ground(), null, null,
              List.of(new BoardScene.Feature("tree", 0, 0, 0, 1, 2, 0, BoardScene.FeatureKind.TREE)), List.of()));
        BoardScene trees = new BoardScene(0, scene.width(), scene.height(), tiles, List.of(), List.of(), -1, "", List.of());
        GpuTerrain terrain = new GpuTerrain();
        Vector3 point = BoardGeometry.center(target, 0).add(0, 0, 10);
        try {
            terrain.update(trees);
            camera.setIsometric(false);
            camera.fit(trees);
            terrain.animate(0, List.of(unit), 1, 0);
            drawMeeple(terrain, camera, units, unit);
            terrain.renderTransparent(camera.camera);
            int clear = rgba(camera, point);
            assertTrue((clear >>> 24) > ((clear >>> 16) & 255) + 150, "Tree opacity 0 must expose the unit");
            terrain.animate(0, List.of(unit), 1, 0.75f);
            drawMeeple(terrain, camera, units, unit);
            terrain.renderTransparent(camera.camera);
            int faded = rgba(camera, point);
            assertNotEquals(clear, faded, "Tree opacity must change while a unit is already inside");
            terrain.animate(0, List.of(unit), 0, 0.75f);
            drawMeeple(terrain, camera, units, unit);
            terrain.renderTransparent(camera.camera);
            assertEquals(faded, rgba(camera, point), "Building opacity must not alter trees");
            terrain.animate(0, List.of(unit), 0, 1);
            drawMeeple(terrain, camera, units, unit);
            int opaque = rgba(camera, point);
            terrain.animate(0, List.of());
            drawMeeple(terrain, camera, units, unit);
            assertEquals(opaque, rgba(camera, point), "Tree opacity 100 must restore the normal opaque pass");
        } finally {
            terrain.dispose();
        }
    }

    private void checkAtlas() {
        GpuTextures<String> atlas = new GpuTextures<>();
        try {
            BoardScene.Pixels grass = hexPixels(java.awt.Color.GREEN);
            BoardScene.Pixels matchingGrass = hexPixels(java.awt.Color.GREEN);
            BoardScene.Pixels sand = hexPixels(java.awt.Color.YELLOW);
            assertTrue(atlas.update(Map.of("one", grass, "two", matchingGrass)));
            assertSame(atlas.region("one"), atlas.region("two"), "Identical artwork must occupy one atlas slot");
            assertFalse(atlas.update(Map.of("one", sand, "two", sand)), "Shared slots can update together");
            assertSame(atlas.region("one"), atlas.region("two"));
            assertTrue(atlas.update(Map.of("one", sand, "two", grass)), "Diverging aliases must split before upload");
            assertNotSame(atlas.region("one"), atlas.region("two"));
            assertTrue(atlas.update(Map.of("one", grass, "two", matchingGrass)), "Converging images must merge again");
            assertSame(atlas.region("one"), atlas.region("two"));
            assertTrue(atlas.update(Map.of()));
            assertFalse(atlas.update(Map.of()), "An empty layer must not allocate an atlas every frame");
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
                meeple.place(unit, camera.camera, position, 0, 2, false);
                BoundingBox bounds = unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform);
                assertEquals(2 * BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE, bounds.getDepth(), 0.001f);
                assertEquals(0.5f, bounds.min.z, 0.001f);
                assertEquals(24 * BoardGeometry.UNIT_SCALE, bounds.getWidth(), 0.01f);
                assertEquals(24 * BoardGeometry.UNIT_SCALE, bounds.getHeight(), 0.01f,
                      "Token size must follow its artwork, not the hex scale");
                terrain.renderShadows(camera.camera, List.of(unit));
                drawMeeple(terrain, camera, batch, unit);
                int left = brightness(camera, -35);
                int right = brightness(camera, 35);
                assertTrue(direction > 0 ? right < left - 20 : left < right - 20,
                      "Meeple shadow must follow light: left=" + left + ", right=" + right);
            }
            BoardGeometry.Tuning original = BoardGeometry.tuning();
            try {
                for (float scale : new float[] { 0.6f, 1, 2 }) {
                    for (float heightScale : new float[] { 0.4f, 1.3f }) {
                        BoardGeometry.tune(new BoardGeometry.Tuning(original.hexScale(), scale, heightScale,
                              original.levelHeight(), original.gridShade()));
                        meeple.place(unit, camera.camera, position, 0, 2, false);
                        BoundingBox bounds = unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform);
                        assertEquals(24 * scale, bounds.getWidth(), 0.01f,
                              "The unit scale must size a single-hex token footprint");
                        assertEquals(2 * BoardGeometry.LEVEL * heightScale, bounds.getDepth(), 0.001f);

                        meeple.place(unit, camera.camera, position, 0, 2, true);
                        bounds = unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform);
                        assertEquals(24, bounds.getWidth(), 0.01f,
                              "Multi-hex sections must keep their full artwork width at unit scale " + scale);
                        assertEquals(24, bounds.getHeight(), 0.01f,
                              "Multi-hex sections must keep their full artwork length at unit scale " + scale);
                        assertEquals(2 * BoardGeometry.LEVEL * heightScale, bounds.getDepth(), 0.001f,
                              "Multi-hex sections must still follow the unit height scale");
                    }
                }
            } finally {
                BoardGeometry.tune(original);
            }
            meeple.place(unit, camera.camera, position, 0, 1, false);
            assertEquals(BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE,
                  unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform).getDepth(), 0.001f);
            meeple.place(unit, camera.camera, new Vector3(position).add(0, -100, 0), 0, 2, false);
            terrain.renderShadows(camera.camera, List.of(unit));
            drawMeeple(terrain, camera, batch, unit);
            assertEquals(brightness(camera, -35), brightness(camera, 35), 3,
                  "Moving a meeple must clear its old shadow");

            camera.setIsometric(true);
            camera.center(position);
            camera.zoom(0.4f);
            meeple.place(unit, camera.camera, position, 0, 2, false);
            int[] sideBrightness = new int[2];
            int index = 0;
            for (float direction : new float[] { 38, -38 }) {
                terrain.update(shadowScene(floor, 0, new BoardScene.Light(direction, 0)));
                terrain.renderShadows(camera.camera, List.of(unit));
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
}
