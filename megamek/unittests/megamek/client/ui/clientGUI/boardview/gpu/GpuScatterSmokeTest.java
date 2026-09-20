/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Measures the submitted geometry and draws; captures the actual tileset in a native GL context. */
@Tag("on-demand")
class GpuScatterSmokeTest {
    @Test
    void batchesSparseDetailsAndCullsAllPassesWithoutChangingPicking() throws Exception {
        assumeTrue(BoardFeatures.SCATTER_DENSITY_MULTIPLIER > 0, "Scatter is disabled");
        Hex[] hexes = new Hex[16 * 16];
        String[] themes = { "grass", "dirt", "desert", "lunar" };
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Hex hex = new Hex(0);
                hex.setTheme(themes[x / 4]);
                hexes[y * 16 + x] = hex;
            }
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(new Board(16, 16, hexes))) {
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            BoardScene captured = fixture.source.takeFrame().scene();
            BoardScene scene = new BoardScene(0, 16, 16, captured.tiles(), List.of(), List.of(), -1, "", List.of(),
                  new BoardScene.Light(-24, -30));
            BoardScene bare = new BoardScene(0, 16, 16, scene.tiles().stream().map(tile -> new BoardScene.Tile(
                  tile.coords(), tile.elevation(), tile.waterDepth(), tile.frozen(), tile.roadExits(), tile.surface(),
                  tile.ground(), tile.normals(), tile.decals(), tile.decalsWithoutLimbs(), tile.tactical(), List.of(),
                  tile.text())).toList(), List.of(), List.of(), -1, "", List.of(), scene.light());
            var configuration = GpuBoardWindow.configuration(false);
            configuration.setWindowedMode(1440, 1000);
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    GpuTerrain terrain = new GpuTerrain();
                    GpuTerrain plain = new GpuTerrain();
                    ModelBatch depth = new ModelBatch(new DepthShaderProvider());
                    GLProfiler profiler = new GLProfiler(Gdx.graphics);
                    try {
                        terrain.update(scene);
                        plain.update(bare);
                        var features = scene.tiles().stream().flatMap(tile -> tile.features().stream()).toList();
                        assertTrue(!features.isEmpty() && features.size() <= 512,
                              "At most two details per eligible hex, regardless of the density multiplier");
                        int triangles = features.stream().mapToInt(feature -> switch (feature.asset()) {
                            case "scatter-grass", "scatter-dry-grass" -> 6;
                            case "scatter-plant" -> 16;
                            default -> 9;
                        }).sum();
                        float diameter = (float) features.stream().mapToDouble(GpuScatter::diameter).max().orElseThrow();
                        BoardCamera camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        camera.setIsometric(true);
                        camera.center(BoardGeometry.center(new Coords(7, 7), 0));
                        profiler.enable();
                        float[] pixels = { 6, 2.4f, 1.5f, 2.4f, 4 };
                        for (int step = 0; step < pixels.length; step++) {
                            camera.camera.zoom = diameter / pixels[step] * Gdx.graphics.getBackBufferHeight()
                                  / Gdx.graphics.getHeight();
                            camera.update();
                            boolean visible = step < 2 || step == 4;
                            int vertices = visible ? triangles * 3 : 0;
                            int[] background = count(profiler, () -> plain.render(camera.camera, false));
                            int[] decorated = count(profiler, () -> terrain.render(camera.camera, false));
                            assertEquals(vertices, decorated[0] - background[0]);
                            assertEquals(visible ? 1 : 0, decorated[1] - background[1],
                                  "All scatter shares one draw call in this chunk");
                            assertEquals(vertices,
                                  count(profiler, () -> terrain.renderDepth(camera.camera, List.of(), depth))[0]
                                        - count(profiler, () -> plain.renderDepth(camera.camera, List.of(), depth))[0]);
                            assertEquals(vertices,
                                  count(profiler, () -> terrain.renderShadows(camera.camera, List.of()))[0]
                                        - count(profiler, () -> plain.renderShadows(camera.camera, List.of()))[0]);
                            assertEquals(0, count(profiler, () -> terrain.renderShadows(camera.camera, List.of()))[0],
                                  "An unchanged camera reuses the shadow map");
                            for (var tile : scene.tiles()) {
                                for (var feature : tile.features()) {
                                    Vector3 origin = BoardGeometry.center(tile.coords(), 0).add(
                                          feature.x() * BoardGeometry.HEX_SCALE, feature.y() * BoardGeometry.HEX_SCALE, 100);
                                    Ray ray = new Ray(origin, new Vector3(0, 0, -1));
                                    assertEquals(plain.hit(bare, ray), terrain.hit(scene, ray),
                                          "Decoration must not change a board-space hit");
                                }
                            }
                        }
                        BoardScene approach = approach(scene.tiles().getFirst().ground(), List.of(features.getFirst()));
                        terrain.update(approach);
                        plain.update(approach(scene.tiles().getFirst().ground(), List.of()));
                        camera.camera.zoom = .25f;
                        camera.center(BoardGeometry.center(new Coords(0, 0), 0));
                        assertEquals(count(profiler, () -> plain.render(camera.camera, false))[0],
                              count(profiler, () -> terrain.render(camera.camera, false))[0],
                              "A sloped road approach into an unpaved hex must also suppress scatter");
                        terrain.update(scene);
                        profiler.disable();
                        camera.fit(scene);
                        preview(terrain, camera, "scatter-overview");
                        for (BoardScene.Surface surface : List.of(BoardScene.Surface.GRASS, BoardScene.Surface.DIRT,
                              BoardScene.Surface.SAND, BoardScene.Surface.ROCK)) {
                            var tile = scene.tiles().stream().filter(value -> value.surface() == surface
                                  && !value.features().isEmpty()).findFirst().orElseThrow();
                            camera.camera.zoom = .15f;
                            camera.center(BoardGeometry.center(tile.coords(), tile.elevation()));
                            preview(terrain, camera, "scatter-" + surface.name().toLowerCase(java.util.Locale.ROOT));
                        }
                        System.out.println("Scatter: " + features.size() + " objects, " + triangles
                              + " triangles, 1 additional draw per visible pass on 256 hexes; 0 at overview cull.");
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        profiler.disable();
                        depth.dispose();
                        plain.dispose();
                        terrain.dispose();
                        Gdx.app.exit();
                    }
                }
            }, configuration);
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static int[] count(GLProfiler profiler, Runnable draw) {
        profiler.reset();
        ScreenUtils.clear(.035f, .055f, .075f, 1, true);
        draw.run();
        return new int[] { (int) profiler.getVertexCount().total, profiler.getDrawCalls() };
    }

    private static BoardScene approach(BoardScene.Pixels ground, List<BoardScene.Feature> details) {
        var empty = new BoardScene.Tile(new Coords(0, 0), 0, -1, false, 0, BoardScene.Surface.GRASS,
              ground, null, null, details, List.of());
        var road = new BoardScene.Tile(new Coords(0, 1), 1, -1, false, 1, BoardScene.Surface.GRASS,
              ground, null, null, List.of(), List.of());
        return new BoardScene(0, 1, 2, List.of(empty, road), List.of(), List.of(), -1, "", List.of());
    }

    private static void preview(GpuTerrain terrain, BoardCamera camera, String name) {
        ScreenUtils.clear(.035f, .055f, .075f, 1, true);
        terrain.renderShadows(camera.camera, List.of());
        terrain.render(camera.camera, false);
        String directory = System.getProperty("megamek.gpu.screenshots");
        if (directory != null) {
            GpuBoardTestUi.capture(new File(directory, name + ".png"));
        }
    }
}
