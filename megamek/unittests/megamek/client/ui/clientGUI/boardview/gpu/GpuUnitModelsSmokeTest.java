/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Frozen reference artwork only. Runtime deployment and dynamic assembly are covered by the modular smoke test. */
@Tag("on-demand")
class GpuUnitModelsSmokeTest {
    @Test
    void loadsTheWholeCatalogAndRendersMeksAndFormations() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                File referenceRoot = new File(System.getProperty("megamek.gpu.referenceModels"));
                GpuUnitModels library = new GpuUnitModels(referenceRoot.toPath());
                ModelBatch batch = new ModelBatch();
                try {
                    File root = new File(referenceRoot, "units");
                    var manifest = new JsonReader().parse(new FileHandle(new File(root, "manifest.json")));
                    for (var entry : manifest.get("models")) {
                        var data = new G3dModelLoader(new JsonReader()).loadModelData(new FileHandle(new File(root, entry.name)));
                        Model model = new Model(data);
                        try {
                            int triangles = 0;
                            for (var part : model.meshParts) {
                                triangles += part.size / 3;
                            }
                            assertEquals(entry.getInt("triangles"), triangles, entry.name);
                        } finally {
                            model.dispose();
                        }
                    }
                    var variants = manifest.get("variants");
                    var selections = List.of(
                          new BoardScene.UnitModel("units/meks/atlas/model.json", "units/fallback/biped.json", variants.get("Atlas AS7-D").getString("variantKey"), 1),
                          new BoardScene.UnitModel("units/meks/locust/model.json", "units/fallback/biped.json", variants.get("Locust LCT-1V").getString("variantKey"), 1),
                          new BoardScene.UnitModel("units/meks/warhammer/model.json", "units/fallback/biped.json", variants.get("Warhammer WHM-6R").getString("variantKey"), 1),
                          new BoardScene.UnitModel("units/meks/mad-cat/model.json", "units/fallback/biped.json", variants.get("Mad Cat (Timber Wolf) Prime").getString("variantKey"), 1),
                          new BoardScene.UnitModel("units/infantry/model.json", null, "Rifle platoon", 6),
                          new BoardScene.UnitModel("units/battle-armor/model.json", null, "Battle Armor", 3));
                    assertSame(library.get(selections.getFirst()), library.get(selections.getFirst()));
                    assertNotNull(library.get(new BoardScene.UnitModel("units/missing.json", "units/fallback/biped.json", "Unknown", 1)));
                    assertNull(library.get(new BoardScene.UnitModel("../outside.json", null, "Unknown", 1)));
                    GpuMeeple empty = library.get(new BoardScene.UnitModel("units/infantry/model.json", null, "Empty", 0));
                    assertNotNull(empty);
                    assertEquals(0, empty.instance.model.meshParts.size);
                    for (var movement : Map.of("INF_MOTORIZED", "motorized", "TRACKED", "tracked",
                          "WHEELED", "wheeled", "HOVER", "hover", "INF_JUMP", "jump").entrySet()) {
                        for (int slots = 0; slots <= 6; slots++) {
                            var selection = new BoardScene.UnitModel("units/infantry/model.json", null,
                                  movement.getKey(), slots);
                            GpuMeeple asset = library.get(selection);
                            assertNotNull(asset, movement.getKey());
                            assertSame(asset, library.get(selection));
                            int triangles = 0;
                            for (var part : asset.instance.model.meshParts) {
                                triangles += part.size / 3;
                            }
                            String path = "infantry/" + movement.getValue() + "/squad-" + slots + ".g3dj";
                            assertEquals(manifest.get("models").get(path).getInt("triangles"), triangles, path);
                        }
                    }
                    var mobileInfantry = List.of("INF_MOTORIZED", "TRACKED", "WHEELED", "HOVER", "INF_JUMP", "INF_LEG")
                          .stream().map(mode -> new BoardScene.UnitModel("units/infantry/model.json", null, mode, 6)).toList();
                    Environment environment = new Environment();
                    environment.set(ColorAttribute.createAmbientLight(.7f, .7f, .7f, 1));
                    environment.add(new DirectionalLight().set(.8f, .8f, .8f, -.4f, -.7f, -1));
                    OrthographicCamera camera = new OrthographicCamera(300, 187.5f);
                    camera.near = 1;
                    camera.far = 1000;
                    File output = new File(System.getProperty("megamek.gpu.screenshots"));
                    assertTrue(output.isDirectory() || output.mkdirs());
                    for (var page : List.of(selections, mobileInfantry)) {
                        for (boolean top : new boolean[] { false, true }) {
                            camera.position.set(top ? new Vector3(0, 0, 350) : new Vector3(90, 240, 190));
                            camera.up.set(top ? Vector3.Y : Vector3.Z);
                            camera.lookAt(0, 0, 10);
                            camera.update();
                            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                            Gdx.gl.glClearColor(.15f, .19f, .23f, 1);
                            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                            batch.begin(camera);
                            for (int i = 0; i < page.size(); i++) {
                                GpuMeeple asset = library.get(page.get(i));
                                assertNotNull(asset, page.get(i).asset());
                                ModelInstance instance = new ModelInstance(asset.instance.model);
                                Vector3 position = page == mobileInfantry
                                      ? new Vector3((1 - i % 3) * 90, (0.5f - i / 3) * 65, 0)
                                      : new Vector3(i < 4 ? (1.5f-i)*68 : (4.5f-i)*90, i < 4 ? 15 : -48, 0);
                                asset.place(instance, camera, position, 0, page == selections && i < 4 ? 2 : 1, false);
                                batch.render(instance, environment);
                            }
                            batch.end();
                            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                            try {
                                int changed = 0;
                                int background = pixels.getPixel(0, 0);
                                for (int y = 0; y < pixels.getHeight(); y += 3) {
                                    for (int x = 0; x < pixels.getWidth(); x += 3) {
                                        if (pixels.getPixel(x, y) != background) {
                                            changed++;
                                        }
                                    }
                                }
                                assertTrue(changed > 1000, "Unit meshes must render visible geometry");
                                String name = "unit-models-" + (page == mobileInfantry ? "infantry-" : "")
                                      + (top ? "top" : "isometric") + ".png";
                                PixmapIO.writePNG(new FileHandle(new File(output, name)), pixels, -1, true);
                            } finally {
                                pixels.dispose();
                            }
                        }
                    }
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    batch.dispose();
                    library.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }
}
