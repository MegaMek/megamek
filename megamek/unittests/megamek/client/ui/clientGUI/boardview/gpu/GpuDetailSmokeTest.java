/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real GPU ownership and repeated zoom transitions, including the game-state/render-state boundary. */
@Tag("on-demand")
class GpuDetailSmokeTest {
    @Test
    void detailChangesReuseBuffersAndPreserveModelsDamageAndPickingBounds() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try (var audit = new Uploads()) {
                    forest(audit);
                    equipment(audit);
                    depthAndShadows();
                    assertTrue(audit.buffers.isEmpty(), "Every generated buffer must be released");
                    assertTrue(audit.textures.isEmpty(), "Every generated texture must be released");
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) { failure.set(error); }
                finally { Gdx.app.exit(); }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Detail ownership review failed", failure.get()); }
    }

    private static void forest(Uploads audit) {
        var terrain = new GpuTerrain();
        var camera = new BoardCamera();
        var original = BoardGeometry.tuning();
        try {
            var scene = forest();
            terrain.update(scene);
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            camera.setIsometric(true);
            camera.fit(scene);
            camera.camera.zoom = .1f;
            camera.update();
            drawTrees(terrain, camera);
            long initialBytes = audit.bufferBytes.values().stream().mapToLong(Integer::longValue).sum();
            for (float zoom : new float[] { .1f, 1.5f, 10, .1f }) {
                camera.camera.zoom = zoom;
                camera.update();
                drawTrees(terrain, camera);
            }
            int uploads = audit.uploads, buffers = audit.buffers.size();
            long cachedBytes = audit.bufferBytes.values().stream().mapToLong(Integer::longValue).sum();
            // A pure forest must never inspect unit bounds for obsolete occupancy fading.
            List<ModelInstance> unreadUnits = new java.util.AbstractList<>() {
                @Override public int size() { return 1; }
                @Override public ModelInstance get(int index) { throw new AssertionError("Pure forest cannot inspect units"); }
            };
            for (int frame = 0; frame < 24; frame++) {
                camera.camera.zoom = new float[] { .1f, 1.5f, 10 }[frame % 3];
                camera.update();
                BoardGeometry.tune(new BoardGeometry.Tuning(original.hexScale(), frame % 2 == 0 ? 1 : .7f,
                      original.unitHeightScale(), original.levelHeight(), original.gridShade(), original.multiHexUnitScale()));
                terrain.update(scene);
                terrain.animate(0, unreadUnits);
                drawTrees(terrain, camera);
            }
            assertEquals(uploads, audit.uploads, "Cached tree LoD and unit-only tuning must not upload geometry or textures");
            assertEquals(buffers, audit.buffers.size(), "Crossing tree thresholds must reuse the same buffers");
            System.out.println("Forest: 256 heavy-woods hexes, repeated three-level zoom and unit-scale changes: zero uploads after warmup.");
            System.out.println("Forest GPU buffers: initial " + initialBytes / 1048576.0 + " MiB; all levels cached "
                  + cachedBytes / 1048576.0 + " MiB (includes ground and shared source meshes).");
        } finally { terrain.dispose(); BoardGeometry.tune(original); }
    }

    private static void drawTrees(GpuTerrain terrain, BoardCamera camera) {
        terrain.renderShadows(camera.camera, List.of());
        ScreenUtils.clear(.02f, .03f, .04f, 1, true);
        terrain.render(camera.camera, false);
        terrain.renderTransparent(camera.camera);
    }

    private static BoardScene forest() {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(100, 120, 70));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        var pixels = new BoardScene.Pixels(image);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                var hex = new Hex(0);
                hex.addTerrain(new Terrain(Terrains.WOODS, 2));
                hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
                var coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, 0, -1, false, 0, BoardScene.Surface.GRASS, pixels, null, null,
                      BoardFeatures.capture(hex, coords, Map.of()), List.of()));
            }
        }
        return new BoardScene(0, 16, 16, tiles, List.of(), List.of(), -1, "", List.of(), new BoardScene.Light(-24, -30));
    }

    private static void equipment(Uploads audit) throws Exception {
        var library = new GpuUnitModels();
        var batch = new ModelBatch();
        try {
            var tileset = new MekTileset(Configuration.unitImagesDir());
            tileset.loadFromFile("mekset.txt");
            var camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            camera.center(new Vector3(0, 0, 15));
            int id = 0;
            for (String file : List.of("Atlas AS7-D.mtf", "Barghest BGS-1T.mtf", "Elemental BA [Laser] (Sqd5).blk", "Union (3055).blk")) {
                var entity = new MekFileParser(new File("testresources/megamek/common/units", file)).getEntity();
                var selection = UnitModelSelection.capture(entity, -1, false, tileset);
                var model = library.get(selection, ++id);
                var instance = new GpuUnitInstance(model);
                instance.transform.setToScaling(.7f, .7f, .5f);
                var bounds = UnitBounds.world(instance);
                Map<NodePart, Boolean> before = flags(instance.nodes);
                var muzzles = emitters(model, instance);
                for (float zoom : new float[] { .1f, 20, .1f }) {
                    camera.camera.zoom = zoom;
                    camera.update();
                    instance.equipmentDetail(camera.camera, false);
                    batch.begin(camera.camera);
                    batch.render(instance);
                    batch.end();
                }
                int uploads = audit.uploads, buffers = audit.buffers.size();
                for (int step = 0; step < 40; step++) {
                    boolean far = step % 2 == 0;
                    camera.camera.zoom = far ? 20 : .1f;
                    camera.update();
                    instance.equipmentDetail(camera.camera, false);
                    var currentBounds = UnitBounds.world(instance);
                    assertEquals(bounds.min, currentBounds.min, "LoD cannot shrink picking/footprint bounds");
                    assertEquals(bounds.max, currentBounds.max, "LoD cannot shrink picking/footprint bounds");
                    assertEquals(before, flags(instance.nodes), "LoD cannot masquerade as damage or casualties");
                    assertEquals(muzzles, emitters(model, instance), "Hidden equipment keeps its authored firing origins");
                    assertSame(model, library.get(selection, id), "Zoom must never rebuild an assembly");
                    if (far && !model.equipment().isEmpty()) { assertTrue(instance.hiddenEquipment() > 0, file); }
                    if (!far) { assertEquals(0, instance.hiddenEquipment(), file); }
                    batch.begin(camera.camera);
                    batch.render(instance);
                    batch.end();
                }
                assertEquals(uploads, audit.uploads, "Equipment LoD must not upload new geometry or textures");
                assertEquals(buffers, audit.buffers.size());
                float smallest = Float.POSITIVE_INFINITY;
                for (var binding : model.equipment()) {
                    if (!binding.embedded()) {
                        smallest = Math.min(smallest, UnitBounds.subtree(instance.getNode(binding.node()))
                              .getDimensions(new Vector3()).len());
                    }
                }
                if (Float.isFinite(smallest)) {
                    float boundary = smallest * .7f * Gdx.graphics.getBackBufferHeight()
                          / Gdx.graphics.getHeight() / GpuUnitInstance.EQUIPMENT_HIDE_PIXELS;
                    instance.equipmentDetail(camera.camera, true);
                    camera.camera.zoom = boundary;
                    instance.equipmentDetail(camera.camera, false);
                    assertEquals(0, instance.hiddenEquipment(), "Visible detail persists within the hysteresis band");
                    camera.camera.zoom = boundary * 1.3f;
                    instance.equipmentDetail(camera.camera, false);
                    assertTrue(instance.hiddenEquipment() > 0);
                    camera.camera.zoom = boundary;
                    instance.equipmentDetail(camera.camera, false);
                    assertTrue(instance.hiddenEquipment() > 0, "Hidden detail persists within the hysteresis band");
                    camera.camera.zoom = boundary * .8f;
                    instance.equipmentDetail(camera.camera, false);
                    assertEquals(0, instance.hiddenEquipment(), "Closer inspection restores detail");
                }
                UnitDamageDisplay.show(instance, new BoardScene.LocationDamage(Set.of("RA"), Set.of()));
                var damaged = flags(instance.nodes);
                camera.camera.zoom = 20;
                instance.equipmentDetail(camera.camera, false);
                instance.equipmentDetail(camera.camera, true);
                assertEquals(0, instance.hiddenEquipment(), "Selected/firing units can request full detail");
                assertEquals(damaged, flags(instance.nodes), "Showing detail must never restore a lost location");
            }
            System.out.println("Equipment: Atlas, quad, BA and Union; 40 near/far cycles each, no assembly rebuilds or GPU uploads.");
        } finally { batch.dispose(); library.dispose(); }
    }

    private static Map<NodePart, Boolean> flags(Iterable<Node> nodes) {
        Map<NodePart, Boolean> result = new HashMap<>();
        for (Node node : nodes) {
            for (NodePart part : node.parts) { result.put(part, part.enabled); }
            result.putAll(flags(node.getChildren()));
        }
        return result;
    }

    private static List<Vector3> emitters(GpuUnitModel model, ModelInstance instance) {
        List<Vector3> result = new ArrayList<>();
        for (var binding : model.equipment()) {
            for (var emitter : binding.emitters()) {
                var point = new Vector3();
                var direction = new Vector3();
                UnitModelAttachment.emitter(instance, emitter, point, direction);
                result.add(point); result.add(direction);
            }
        }
        return result;
    }

    /** Compare actual depth with the previous full-geometry pass, not just the appearance of one outline. */
    private static void depthAndShadows() throws Exception {
        var terrain = new GpuTerrain();
        var atmosphere = new GpuAtmosphere();
        var library = new GpuUnitModels();
        var config = new DepthShader.Config();
        config.defaultCullFace = GL20.GL_BACK;
        var reference = new ModelBatch(new DepthShaderProvider(config), new GpuOpaqueSorter());
        var profiler = new GLProfiler(Gdx.graphics);
        try {
            var scene = forest();
            terrain.update(scene);
            var camera = new BoardCamera();
            int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight() - 64;
            camera.resize(width, height);
            var tileset = new MekTileset(Configuration.unitImagesDir());
            tileset.loadFromFile("mekset.txt");
            var entity = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
            var model = library.get(UnitModelSelection.capture(entity, -1, false, tileset));
            var instance = new GpuUnitInstance(model);
            List<ModelInstance> units = List.of(instance);
            for (boolean top : new boolean[] { true, false }) {
                camera.setIsometric(!top);
                camera.fit(scene);
                model.place(instance, camera.camera, BoardGeometry.center(new Coords(7, 7), 2), 60, 2, false);
                atmosphere.begin(width, height, 0, true);
                terrain.render(camera.camera, false);
                atmosphere.end(camera.camera, terrain, units, scene, 64);
                var capturedBuffer = (FrameBuffer) GpuMixedUnitBenchmarkSmokeTest.field(atmosphere, "sceneDepth");
                capturedBuffer.begin();
                float[] captured = depth(width, height, 0);
                capturedBuffer.end();
                HdpiUtils.glViewport(0, 64, width, height);
                Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
                Gdx.gl.glColorMask(false, false, false, false);
                terrain.renderDepth(camera.camera, units, reference);
                Gdx.gl.glColorMask(true, true, true, true);
                float[] expected = depth(width, height, 64);
                Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
                profiler.enable();
                profiler.reset();
                atmosphere.restoreDepth(camera.camera, terrain, units);
                assertEquals(1, profiler.getDrawCalls(), "Depth restoration must use one fullscreen draw");
                profiler.disable();
                float[] actual = depth(width, height, 64);
                int geometry = 0;
                int mismatches = 0;
                float maximum = 0;
                for (int pixel = 0; pixel < expected.length; pixel++) {
                    if (expected[pixel] < 1) { geometry++; }
                    if (Math.abs(expected[pixel] - actual[pixel]) >= .000002f) {
                        mismatches++;
                    }
                    maximum = Math.max(maximum, Math.abs(captured[pixel] - actual[pixel]));
                }
                assertTrue(geometry > 1000, "The comparison must contain terrain, trees and a unit");
                assertTrue(maximum < .000002f, "Copied depth must match the captured depth: " + maximum);
                // Moving the viewport origin can change rasterization at exact triangle edges by one pixel.
                assertTrue(mismatches <= expected.length / 10000, "Depth must also agree with the legacy pass away from edges");
                System.out.println("Depth reuse " + (top ? "top" : "isometric") + ": max copy error " + maximum
                      + "; " + mismatches + " edge differences from full geometry; one draw.");
            }
            terrain.setAtmosphere(BoardAtmosphere.lighting(BoardAtmosphere.DEFAULTS));
            terrain.renderShadows(camera.camera, units);
            profiler.enable();
            profiler.reset();
            terrain.setAtmosphere(BoardAtmosphere.lighting(new BoardAtmosphere.Settings(13, .8f, .2f, 2.5f, .1f, 1)));
            terrain.renderShadows(camera.camera, units);
            assertEquals(0, profiler.getDrawCalls(), "Light color, fog and exposure cannot invalidate shadow geometry");
            terrain.setAtmosphere(BoardAtmosphere.lighting(new BoardAtmosphere.Settings(14, .8f, .2f, 2.5f, .1f, 1)));
            terrain.renderShadows(camera.camera, units);
            assertTrue(profiler.getDrawCalls() > 0, "Changing light direction must update shadows");
        } finally {
            profiler.disable(); reference.dispose(); library.dispose(); atmosphere.dispose(); terrain.dispose();
        }
    }

    private static float[] depth(int width, int height, int bottom) {
        int w = HdpiUtils.toBackBufferX(width), h = HdpiUtils.toBackBufferY(height);
        var pixels = BufferUtils.newFloatBuffer(w * h);
        Gdx.gl.glReadPixels(0, HdpiUtils.toBackBufferY(bottom), w, h, GL30.GL_DEPTH_COMPONENT, GL20.GL_FLOAT, pixels);
        float[] values = new float[w * h];
        pixels.get(values);
        return values;
    }

    private static final class Uploads implements AutoCloseable {
        final GL20 original = Gdx.gl;
        final Set<Integer> buffers = new HashSet<>(), textures = new HashSet<>();
        final Map<Integer, Integer> boundBuffers = new HashMap<>(), bufferBytes = new HashMap<>();
        int uploads;

        Uploads() {
            GL20 watched = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[] { GL20.class },
                  (proxy, method, args) -> {
                      if (method.getName().contains("BufferData") || method.getName().contains("BufferSubData")
                            || method.getName().contains("TexImage") || method.getName().contains("TexSubImage")) { uploads++; }
                      try {
                          Object result = method.invoke(original, args);
                          switch (method.getName()) {
                              case "glGenBuffer" -> buffers.add((Integer) result);
                              case "glDeleteBuffer" -> { buffers.remove((Integer) args[0]); bufferBytes.remove((Integer) args[0]); }
                              case "glBindBuffer" -> boundBuffers.put((Integer) args[0], (Integer) args[1]);
                              case "glBufferData" -> bufferBytes.put(boundBuffers.get((Integer) args[0]), (Integer) args[1]);
                              case "glGenTexture" -> textures.add((Integer) result);
                              case "glDeleteTexture" -> textures.remove((Integer) args[0]);
                              default -> { }
                          }
                          return result;
                      } catch (InvocationTargetException error) { throw error.getCause(); }
                  });
            Gdx.graphics.setGL20(watched);
            Gdx.gl = Gdx.gl20 = watched;
        }

        @Override
        public void close() { Gdx.graphics.setGL20(original); Gdx.gl = Gdx.gl20 = original; }
    }
}
