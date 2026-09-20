/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("on-demand")
class GpuUnitInstanceSmokeTest {
    @Test
    void coalescedDepthPreservesCoverageAndDepthForPosesDamageAndTransparentParts() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var library = new GpuUnitModels();
                var depth = new ModelBatch(new DepthShaderProvider());
                var profiler = new GLProfiler(Gdx.graphics);
                try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    int totalOriginal = 0, totalMerged = 0, id = 0;
                    for (String file : List.of("Atlas AS7-D.mtf", "Barghest BGS-1T.mtf", "Elemental BA [Laser] (Sqd5).blk", "Union (3055).blk")) {
                        var entity = new MekFileParser(new File("testresources/megamek/common/units", file)).getEntity();
                        var selection = UnitModelSelection.capture(entity, -1, false, tileset);
                        var model = library.get(selection, ++id);
                        var merged = new GpuUnitInstance(model.instance.model);
                        for (boolean damaged : List.of(false, true)) {
                            if (damaged) {
                                UnitDamageDisplay.show(merged, new BoardScene.LocationDamage(Set.of("RA"), Set.of("CT")));
                                merged.materials.first().set(new BlendingAttribute(.7f));
                            }
                            for (float twist : new float[] { 0, 60 }) {
                                model.turnUpperBody(merged, twist);
                                merged.transform.setToRotation(Vector3.Z, 20).scale(.8f, .8f, .9f);
                                var normal = new ModelInstance(merged);
                                for (boolean top : List.of(false, true)) {
                                    renderer.topView = top;
                                    renderer.frame(List.of(merged), new Vector3(), null, "depth-equivalence", id);
                                    renderer.buffer.begin();
                                    Gdx.gl.glDepthMask(true);
                                    Gdx.gl.glClearColor(1, 1, 1, 1);
                                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                                    profiler.enable();
                                    profiler.reset();
                                    depth.begin(renderer.camera);
                                    depth.render(normal);
                                    depth.end();
                                    totalOriginal += profiler.getDrawCalls();
                                    var before = Pixmap.createFromFrameBuffer(0, 0, 640, 480);
                                    Gdx.gl.glDepthMask(true);
                                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                                    profiler.reset();
                                    depth.begin(renderer.camera);
                                    GpuUnitInstance.renderDepth(depth, merged);
                                    depth.end();
                                    totalMerged += profiler.getDrawCalls();
                                    profiler.disable();
                                    var after = Pixmap.createFromFrameBuffer(0, 0, 640, 480);
                                    try { compareDepth(before, after, file + " damage " + damaged + " twist " + twist + " top " + top); }
                                    finally { before.dispose(); after.dispose(); }
                                    renderer.buffer.end();
                                }
                            }
                        }
                    }
                    assertTrue(totalMerged < totalOriginal * .95, totalMerged + " / " + totalOriginal + " depth draws");
                    System.out.println("Depth draws " + totalOriginal + " -> " + totalMerged + "; coverage identical, packed depth within 0.00002.");
                } catch (Throwable error) { failure.set(error); }
                finally { profiler.disable(); depth.dispose(); library.dispose(); Gdx.app.exit(); }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Depth equivalence failed", failure.get()); }
    }

    private static void compareDepth(Pixmap before, Pixmap after, String context) {
        int changed = 0;
        double maximum = 0;
        for (int y = 0; y < before.getHeight(); y++) {
            for (int x = 0; x < before.getWidth(); x++) {
                int a = before.getPixel(x, y), b = after.getPixel(x, y);
                assertEquals(a == -1, b == -1, context + " coverage");
                if (a != b) { changed++; maximum = Math.max(maximum, Math.abs(depth(a) - depth(b))); }
            }
        }
        // Different draw order can select the other face at a near-coplanar shared edge on the GL driver.
        // Require identical coverage, with at most 32 edge pixels and less than 0.002% of the depth range.
        assertTrue(changed <= 32 && maximum < .00002, context + " changed " + changed + ", max depth " + maximum);
    }

    private static double depth(int rgba) {
        return ((rgba >>> 24) + ((rgba >>> 16) & 255) / 255.0 + ((rgba >>> 8) & 255) / 65025.0
              + (rgba & 255) / 16581375.0) / 255;
    }
}
