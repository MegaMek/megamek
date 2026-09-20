/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Bounded C0 instance/draw-cost probe, not a frame-rate promise for a complete animated board. */
@Tag("on-demand")
class GpuUnitModelBenchmarkSmokeTest {
    @Test
    void measuresSharedBodiesWithIndependentAttachmentInstances() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                ModelBatch batch = new ModelBatch();
                Model body = null;
                Model module = null;
                GLProfiler profiler = new GLProfiler(Gdx.graphics);
                try {
                    body = new G3dModelLoader(new JsonReader()).loadModel(new FileHandle(new File(
                          System.getProperty("megamek.gpu.referenceModels"), "units/meks/warhammer/body.g3dj")));
                    // A twelve-triangle stand-in isolates instance/draw overhead from weapon artwork complexity.
                    module = new ModelBuilder().createBox(2, 4, 2,
                          new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                          Usage.Position | Usage.Normal | Usage.ColorUnpacked);
                    Environment environment = new Environment();
                    environment.set(ColorAttribute.createAmbientLight(.8f, .8f, .8f, 1));
                    StringBuilder results = new StringBuilder("Probe: shared bare Warhammer; optional 12 independent "
                          + "box modules per unit. No terrain, shadows, camo, animation or effects.\n")
                          .append("Renderer: ").append(Gdx.gl.glGetString(GL20.GL_RENDERER)).append('\n')
                          .append("GL: ").append(Gdx.gl.glGetString(GL20.GL_VERSION)).append('\n')
                          .append("CPU: ").append(System.getenv("PROCESSOR_IDENTIFIER")).append('\n')
                          .append("Java: ").append(System.getProperty("java.version")).append('\n')
                          .append("Resolution: ").append(Gdx.graphics.getWidth()).append('x')
                          .append(Gdx.graphics.getHeight()).append('\n')
                          .append("Timing: submit + glFinish, profiler disabled; 12 warmup / 64 samples.\n")
                          .append("units,modulesPerUnit,drawCalls,shaderSwitches,medianMs,p95Ms,p99Ms\n");
                    for (int count : new int[] { 64, 256, 512 }) {
                        int columns = (int) Math.ceil(Math.sqrt(count));
                        OrthographicCamera camera = new OrthographicCamera(columns * 100,
                              columns * 100f * Gdx.graphics.getHeight() / Gdx.graphics.getWidth());
                        camera.position.set(0, -500, 650);
                        camera.up.set(Vector3.Z);
                        camera.near = 1;
                        camera.far = 5000;
                        camera.lookAt(0, 0, 20);
                        camera.update();
                        for (int modules : new int[] { 0, 12 }) {
                            List<ModelInstance> instances = instances(body, module, count, columns, modules);
                            for (int i = 0; i < 12; i++) {
                                frame(batch, camera, environment, instances);
                            }
                            double[] millis = new double[64];
                            for (int i = 0; i < millis.length; i++) {
                                long start = System.nanoTime();
                                frame(batch, camera, environment, instances);
                                millis[i] = (System.nanoTime() - start) / 1e6;
                            }
                            profiler.reset();
                            profiler.enable();
                            frame(batch, camera, environment, instances);
                            int draws = profiler.getDrawCalls();
                            int switches = profiler.getShaderSwitches();
                            profiler.disable();
                            assertTrue(draws >= count);
                            Arrays.sort(millis);
                            results.append(String.format(Locale.ROOT, "%d,%d,%d,%d,%.3f,%.3f,%.3f%n",
                                  count, modules, draws, switches, percentile(millis, .5), percentile(millis, .95),
                                  percentile(millis, .99)));
                        }
                    }
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                    Files.createDirectories(output.toPath());
                    Files.writeString(output.toPath().resolve("unit-model-draw-cost.txt"), results);
                    System.out.print(results);
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    profiler.disable();
                    batch.dispose();
                    if (body != null) {
                        body.dispose();
                    }
                    if (module != null) {
                        module.dispose();
                    }
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static List<ModelInstance> instances(Model body, Model module, int count, int columns, int modules) {
        List<ModelInstance> instances = new ArrayList<>(count * (modules + 1));
        int rows = (int) Math.ceil((double) count / columns);
        for (int i = 0; i < count; i++) {
            float x = (i % columns - (columns - 1) / 2f) * 70;
            float y = (i / columns - (rows - 1) / 2f) * 70;
            ModelInstance chassis = new ModelInstance(body);
            chassis.transform.setToTranslation(x, y, 0).scale(1, 1, 54);
            instances.add(chassis);
            for (int j = 0; j < modules; j++) {
                ModelInstance attachment = new ModelInstance(module);
                attachment.transform.setToTranslation(x + (j % 6 - 2.5f) * 4, y + 8, 30 + (j / 6) * 6);
                instances.add(attachment);
            }
        }
        return instances;
    }

    private static void frame(ModelBatch batch, OrthographicCamera camera, Environment environment,
          List<ModelInstance> instances) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        for (ModelInstance instance : instances) {
            batch.render(instance, environment);
        }
        batch.end();
        Gdx.gl.glFinish();
    }

    private static double percentile(double[] sorted, double fraction) {
        return sorted[(int) Math.ceil(sorted.length * fraction) - 1];
    }
}
