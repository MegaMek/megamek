/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Paired full-composite GPU timings and pixel comparisons against the frozen original glare function. */
@Tag("on-demand")
class GpuSunGlareBenchmarkSmokeTest {
    private static final String VERTEX_VISIBILITY = "#define VERTEX_SUN_VISIBILITY\n";
    private static final String[] VARIANTS = { "Reference", "Vertex only", "Bounds only", "Optimized" };
    private final File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));

    private record Probe(String name, float x, float y, float strength, float exposure, int occlusion, boolean black) { }

    @Test
    void pairedGlareCostsAndImages() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    compare();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError(failure.get()); }
    }

    private void compare() throws Exception {
        assertTrue(output.isDirectory() || output.mkdirs());
        Gdx.graphics.setVSync(false);
        ShaderProgram[] programs = new ShaderProgram[4];
        Mesh quad = GpuAtmosphere.screenQuad();
        StringBuilder timing = new StringBuilder("Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER)
              + "\nFull composite, RGBA8 + depth24; same source and destination for every variant.\n"
              + "24 warmup rounds, 120 measured rounds; rotate variant order each round.\n"
              + "GPU intervals contain one quad draw; uniform setup, readback, scene/UI and glFinish excluded.\n");
        StringBuilder images = new StringBuilder("Pixel errors against original glare, in 8-bit display levels\n");
        try {
            programs[3] = GpuAtmosphere.shader("atmosphere-composite.frag");
            String vertex = programs[3].getVertexShaderSource();
            String fragment = programs[3].getFragmentShaderSource();
            boolean vertexTextures = vertex.contains(VERTEX_VISIBILITY);
            timing.append("Vertex texture sampling: ").append(vertexTextures).append('\n');
            String original = Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/sun-glare-reference.glsl")
                  .readString("UTF-8");
            String plainVertex = vertex.replace(VERTEX_VISIBILITY, "");
            String plainFragment = fragment.replace(VERTEX_VISIBILITY, "");
            programs[0] = compile(plainVertex, replaceGlare(plainFragment, original));
            String vertexOnly = original;
            if (vertexTextures) {
                vertexOnly = original.substring(0, original.indexOf("    vec2 footprint"))
                      + "    float visible = v_sunVisibility;\n"
                      + original.substring(original.indexOf("    if (visible <= 0.0)"));
            }
            programs[1] = compile(vertex, replaceGlare(fragment, vertexOnly));
            programs[2] = compile(plainVertex, plainFragment);
            for (int[] size : new int[][] { { 1280, 800 }, { 1920, 1080 } }) {
                int width = size[0], height = size[1];
                FrameBuffer source = GpuAtmosphere.buffer(width, height, false);
                Texture depth = GpuAtmosphere.attachDepthTexture(source);
                FrameBuffer target = GpuAtmosphere.buffer(width, height, true);
                try {
                    for (int occlusion : new int[] { 0, 1 }) {
                        var probe = new Probe(occlusion == 0 ? "Visible" : "Occluded", 0.5f, 0.75f, 0.35f, 1, occlusion, false);
                        prepare(source, depth, probe);
                        target.begin();
                        measure(programs, quad, probe, width, height, timing);
                        target.end();
                    }
                    Probe[] probes = {
                          new Probe("Default", 0.5f, 0.75f, 0.35f, 1, 0, false),
                          new Probe("Left", 0.1f, 0.75f, 1, 1, 0, false),
                          new Probe("Right", 0.9f, 0.75f, 1, 1, 0, false),
                          new Probe("Center", 0.5f, 0.5f, 1, 1, 0, false),
                          new Probe("Outside left", -0.05f, 0.85f, 1, 1, 0, false),
                          new Probe("Outside right", 1.05f, 0.85f, 1, 1, 0, false),
                          new Probe("Above", 0.5f, 1.1f, 1, 1, 0, false),
                          new Probe("Occluded", 0.5f, 0.75f, 1, 1, 1, false),
                          new Probe("Partial vertical", 0.5f, 0.75f, 1, 1, 2, false),
                          new Probe("Partial horizontal", 0.5f, 0.5f, 1, 1, 3, false),
                          new Probe("Black maximum", 0.5f, 0.75f, 1, 8, 0, true),
                          new Probe("Disabled", 0.5f, 0.75f, 0, 1, 0, false)
                    };
                    for (Probe probe : probes) {
                        prepare(source, depth, probe);
                        target.begin();
                        comparePixels(programs, quad, probe, width, height, images);
                        target.end();
                    }
                } finally {
                    target.dispose();
                    depth.dispose();
                    source.dispose();
                }
            }
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            Files.writeString(new File(output, "sun-glare-comparison.txt").toPath(), timing);
            Files.writeString(new File(output, "sun-glare-image-comparison.txt").toPath(), images);
        } finally {
            for (ShaderProgram program : programs) { if (program != null) { program.dispose(); } }
            quad.dispose();
        }
    }

    private static ShaderProgram compile(String vertex, String fragment) {
        var program = new ShaderProgram(vertex, fragment);
        if (!program.isCompiled()) {
            String log = program.getLog();
            program.dispose();
            throw new AssertionError(log);
        }
        return program;
    }

    private static String replaceGlare(String fragment, String glare) {
        int start = fragment.indexOf("vec3 sunGlare() {");
        int end = fragment.indexOf("\nvoid main()", start);
        assertTrue(start >= 0 && end > start);
        return fragment.substring(0, start) + glare + fragment.substring(end);
    }

    private static void prepare(FrameBuffer source, Texture depth, Probe probe) {
        source.begin();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClearDepthf(probe.occlusion() == 1 ? 0.5f : 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        if (probe.occlusion() >= 2) {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(0, 0, probe.occlusion() == 2 ? source.getWidth() / 2 : source.getWidth(),
                  probe.occlusion() == 3 ? source.getHeight() / 2 : source.getHeight());
            Gdx.gl.glClearDepthf(0.5f);
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }
        Gdx.gl.glClearDepthf(1);
        source.end();
        source.getColorBufferTexture().bind(0);
        depth.bind(1);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_ALWAYS);
    }

    private static void bind(ShaderProgram program, Probe probe, int width, int height, float strength) {
        program.bind();
        program.setUniformi("u_scene", 0);
        program.setUniformi("u_depth", 1);
        program.setUniformf("u_sunGlare", probe.x(), probe.y(), strength, width / (float) height);
        program.setUniformf("u_glareColor", 1, 0.74f, 0.64f);
        program.setUniformf("u_exposure", probe.exposure());
        float background = probe.black() ? 0 : 1;
        program.setUniformf("u_sky", 0.12f * background, 0.26f * background, 0.45f * background);
        program.setUniformf("u_horizon", 0.42f * background, 0.56f * background, 0.69f * background);
    }

    private static void measure(ShaderProgram[] programs, Mesh quad, Probe probe, int width, int height, StringBuilder report) {
        int[] variant = { 0, 3, 0, 1, 2, 3 };
        String[] name = { "Reference off", "Optimized off", "Reference glare", "Vertex only", "Bounds only", "Optimized glare" };
        try (var timing = new GpuStageTimings()) {
            for (int round = 0; round < 144; round++) {
                for (int slot = 0; slot < variant.length; slot++) {
                    int index = (slot + round) % variant.length;
                    ShaderProgram program = programs[variant[index]];
                    bind(program, probe, width, height, index < 2 ? 0 : probe.strength());
                    if (round >= 24) {
                        timing.beginFrame();
                        timing.stage(name[index]);
                    }
                    quad.render(program, GL20.GL_TRIANGLES);
                    if (round >= 24) { timing.stage(null); }
                    Gdx.gl.glFinish();
                }
            }
            timing.appendReport(report, width + "x" + height + " " + probe.name());
        }
    }

    private static void comparePixels(ShaderProgram[] programs, Mesh quad, Probe probe, int width, int height,
          StringBuilder report) {
        bind(programs[0], probe, width, height, probe.strength());
        quad.render(programs[0], GL20.GL_TRIANGLES);
        Pixmap reference = Pixmap.createFromFrameBuffer(0, 0, width, height);
        try {
            for (int variant = 1; variant < programs.length; variant++) {
                bind(programs[variant], probe, width, height, probe.strength());
                quad.render(programs[variant], GL20.GL_TRIANGLES);
                Pixmap actual = Pixmap.createFromFrameBuffer(0, 0, width, height);
                try {
                    ByteBuffer before = reference.getPixels(), after = actual.getPixels();
                    int maximum = 0;
                    long sum = 0;
                    for (int index = 0; index < width * height * 4; index++) {
                        int error = Math.abs((before.get(index) & 255) - (after.get(index) & 255));
                        maximum = Math.max(maximum, error);
                        sum += error;
                    }
                    double mean = sum / (width * height * 3.0);
                    String label = width + "x" + height + " " + probe.name() + " " + VARIANTS[variant];
                    assertTrue(maximum <= 1 && mean < 0.01, label + ": max=" + maximum + ", mean=" + mean);
                    report.append(String.format(Locale.ROOT, "%s: max=%d, mean=%.6f%n", label, maximum, mean));
                } finally {
                    actual.dispose();
                }
            }
        } finally {
            reference.dispose();
        }
    }
}
