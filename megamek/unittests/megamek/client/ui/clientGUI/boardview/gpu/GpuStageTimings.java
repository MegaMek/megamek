/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;

/** Native benchmark only. Polls completed timestamp queries; never stalls to obtain a result. */
final class GpuStageTimings implements AutoCloseable {
    private static final int MAX_STAGES = 24;
    private final boolean gpu;
    private final long timestampMask;
    private final Frame[] frames = new Frame[4];
    private final Map<String, Samples> samples = new LinkedHashMap<>();
    private Frame active;
    private int dropped;

    private static final class Frame {
        final int[] queries = new int[MAX_STAGES + 1];
        final String[] names = new String[MAX_STAGES];
        final long[] cpu = new long[MAX_STAGES];
        int count;
        long start;
        boolean pending;
    }

    private static final class Samples {
        final List<Double> cpu = new ArrayList<>();
        final List<Double> gpu = new ArrayList<>();
    }

    GpuStageTimings() {
        var capabilities = GL.getCapabilities();
        int bits = capabilities.OpenGL33 || capabilities.GL_ARB_timer_query
              ? GL15.glGetQueryi(ARBTimerQuery.GL_TIMESTAMP, GL15.GL_QUERY_COUNTER_BITS) : 0;
        gpu = bits > 0;
        timestampMask = bits == 64 ? -1L : (1L << bits) - 1;
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new Frame();
            if (gpu) {
                for (int j = 0; j < frames[i].queries.length; j++) { frames[i].queries[j] = GL15.glGenQueries(); }
            }
        }
    }

    void beginFrame() {
        poll();
        for (Frame frame : frames) {
            if (!frame.pending) {
                active = frame;
                active.count = 0;
                return;
            }
        }
        dropped++;
    }

    void stage(String name) {
        if (active == null) { return; }
        long now = System.nanoTime();
        if (active.count > 0) { active.cpu[active.count - 1] = now - active.start; }
        if (gpu) { ARBTimerQuery.glQueryCounter(active.queries[active.count], ARBTimerQuery.GL_TIMESTAMP); }
        if (name == null) {
            active.pending = true;
            active = null;
        } else {
            if (active.count == MAX_STAGES) { throw new IllegalStateException("Too many measured render stages"); }
            active.names[active.count++] = name;
            active.start = System.nanoTime();
        }
    }

    void poll() {
        for (Frame frame : frames) {
            if (!frame.pending || (gpu && GL15.glGetQueryObjecti(frame.queries[frame.count], GL15.GL_QUERY_RESULT_AVAILABLE) == 0)) {
                continue;
            }
            long previous = gpu ? ARBTimerQuery.glGetQueryObjectui64(frame.queries[0], GL15.GL_QUERY_RESULT) : 0;
            for (int i = 0; i < frame.count; i++) {
                var result = samples.computeIfAbsent(frame.names[i], ignored -> new Samples());
                result.cpu.add(frame.cpu[i] / 1e6);
                if (gpu) {
                    long next = ARBTimerQuery.glGetQueryObjectui64(frame.queries[i + 1], GL15.GL_QUERY_RESULT);
                    result.gpu.add(((next - previous) & timestampMask) / 1e6);
                    previous = next;
                }
            }
            frame.pending = false;
        }
    }

    void appendReport(StringBuilder report, String scenario) {
        poll();
        report.append(scenario).append("; GPU timestamps: ").append(gpu ? "supported" : "unavailable (CPU only)")
              .append("; dropped samples: ").append(dropped).append('\n');
        report.append("stage,samples,cpuMedianMs,cpuP95Ms,gpuMedianMs,gpuP95Ms\n");
        samples.forEach((name, result) -> report.append(String.format(Locale.ROOT, "%s,%d,%.4f,%.4f,%s,%s%n",
              name, result.cpu.size(), percentile(result.cpu, .5), percentile(result.cpu, .95),
              gpu ? String.format(Locale.ROOT, "%.4f", percentile(result.gpu, .5)) : "n/a",
              gpu ? String.format(Locale.ROOT, "%.4f", percentile(result.gpu, .95)) : "n/a")));
        samples.clear();
        dropped = 0;
    }

    private static double percentile(List<Double> values, double fraction) {
        values.sort(Double::compare);
        return values.get(Math.min(values.size() - 1, (int) (values.size() * fraction)));
    }

    @Override
    public void close() {
        if (gpu) {
            for (Frame frame : frames) {
                for (int query : frame.queries) { GL15.glDeleteQueries(query); }
            }
        }
    }
}
