/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import megamek.common.ResolvedAttack;

/** Four triangles per missile; all bodies and smoke are batched. Trails sample the same immutable flight curve. */
final class GpuMissileEffects implements Disposable {
    static final float LAUNCH_JITTER_SECONDS = .06f;
    static final int SMOKE_BUDGET = 7680;
    static final int SMOKE_PER_MISSILE = 20;
    static final float SMOKE_LIFE_SECONDS = .3f;
    private static final int MISSILES_PER_BATCH = 512;
    private static final int STRIDE = 7;
    record Launch(UnitAttack attack, Vector3[] origins, Vector3[] targets, int missiles, int hits, boolean indirect, int seed,
          ResolvedAttack.Shot profile) {
        boolean hit(int missile) { return Math.floorMod(seed - missile, missiles) < hits; }
    }

    static Launch capture(UnitAttack attack, Vector3[] origins, ModelInstance target, int missiles, int hits,
          boolean indirect, int seed) {
        return capture(attack, origins, target, missiles, hits, indirect, seed, attack.event.result().shot());
    }

    static Launch capture(UnitAttack attack, Vector3[] origins, ModelInstance target, int missiles, int hits,
          boolean indirect, int seed, ResolvedAttack.Shot profile) {
        var launch = new Launch(attack, origins, new Vector3[missiles], missiles, MathUtils.clamp(hits, 0, missiles), indirect, seed, profile);
        int hitOrdinal = 0;
        for (int missile = 0; missile < missiles; missile++) {
            Vector3 origin = origins[missile % origins.length];
            // Observed artillery landings and counterfire retain their own targets rather than a body surface.
            launch.targets[missile] = launch.hit(missile) && !attack.defensive() && (profile == null || profile.impact() == null)
                  ? attack.hitEndpoint(target, origin, missile + seed, hitOrdinal++, launch.hits, new Vector3())
                  : attack.endpoint(target, origin, !launch.hit(missile), missile + seed, new Vector3());
        }
        return launch;
    }
    private static final class Puff {
        final Vector3 center = new Vector3();
        float radius, alpha, depth;
    }

    private final List<Launch> launches = new ArrayList<>();
    private final GpuExhaustBatch exhaust = new GpuExhaustBatch(SMOKE_BUDGET + MISSILES_PER_BATCH);
    private final Puff[] smoke = new Puff[SMOKE_BUDGET];
    private final Vector3 point = new Vector3(), direction = new Vector3(), side = new Vector3(), up = new Vector3();
    private final Vector3 tip = new Vector3(), a = new Vector3(), b = new Vector3(), c = new Vector3();
    private final Vector3 right = new Vector3(), width = new Vector3(), length = new Vector3();
    private float[] vertices;
    private Mesh mesh;
    private ShaderProgram shader;
    private int offset, smokeCount, missileCount;

    void begin() { launches.clear(); smokeCount = 0; missileCount = 0; }
    void add(Launch launch) { launches.add(launch); }
    int missileCount() { return missileCount; }
    int smokeCount() { return smokeCount; }

    private static float launchDelay(Launch launch, int missile) {
        return Math.min(LAUNCH_JITTER_SECONDS, (launch.attack.contactSeconds - UnitAttack.ANTICIPATION_SECONDS) * .25f)
              * noise(launch.seed + missile * 7919);
    }

    static float progress(Launch launch, int missile, float time) {
        float start = UnitAttack.ANTICIPATION_SECONDS + launchDelay(launch, missile);
        return (time - start) / (launch.attack.contactSeconds - start);
    }

    /** Includes a small fan-out, then converges on the authorized hit or a shared safe miss area. */
    static Vector3 position(Launch launch, int missile, float progress, Vector3 result) {
        float t = MathUtils.clamp(progress, 0, 1);
        Vector3 origin = launch.origins[missile % launch.origins.length];
        Vector3 target = launch.targets[missile];
        float dx = target.x - origin.x, dy = target.y - origin.y;
        float horizontal = Math.max(.001f, (float) Math.hypot(dx, dy));
        float fan = (noise(launch.seed + missile * 31) - .5f) * 9 * MathUtils.sin(t * MathUtils.PI) * (1 - t);
        UnitAttack.projectile(origin, target, t, launch.indirect, result);
        result.x += dy / horizontal * fan;
        result.y -= dx / horizontal * fan;
        result.z += MathUtils.sin(t * MathUtils.PI) * ((launch.indirect ? 0 : 2) + noise(missile + launch.seed) * 3);
        return result;
    }

    void render(Camera camera) {
        if (launches.isEmpty()) { return; }
        int total = launches.stream().mapToInt(Launch::missiles).sum();
        int perMissile = Math.max(1, Math.min(SMOKE_PER_MISSILE, SMOKE_BUDGET / Math.max(1, total)));
        int smokeStride = Math.max(1, (total + SMOKE_BUDGET - 1) / SMOKE_BUDGET);
        right.set(camera.direction).crs(camera.up).nor();
        offset = 0;
        smokeCount = 0;
        missileCount = 0;
        int ordinal = 0;
        for (var launch : launches) {
            float time = launch.attack.seconds;
            for (int missile = 0; missile < launch.missiles; missile++, ordinal++) {
                float t = progress(launch, missile, time);
                if (t >= 0 && t < 1) {
                    position(launch, missile, t, point);
                    position(launch, missile, Math.min(1, t + .002f), direction).sub(point).nor();
                    missile(camera, point, direction);
                    missileCount++;
                }
                if (ordinal % smokeStride != 0) { continue; }
                float interval = SMOKE_LIFE_SECONDS / perMissile;
                float newest = (float) Math.floor(time / interval) * interval;
                for (int puff = 0; puff < perMissile && smokeCount < SMOKE_BUDGET; puff++) {
                    float birth = newest - puff * interval;
                    float at = progress(launch, missile, birth);
                    if (at < 0 || at >= 1) { continue; }
                    float age = time - birth, remaining = Math.max(0, 1 - age / SMOKE_LIFE_SECONDS);
                    Puff sample = smoke[smokeCount];
                    if (sample == null) { sample = new Puff(); smoke[smokeCount] = sample; }
                    position(launch, missile, at, sample.center).add(0, 0, age * 3);
                    sample.radius = .65f + age * 4;
                    sample.alpha = .28f * remaining * remaining;
                    sample.depth = sample.center.dot(camera.direction);
                    smokeCount++;
                }
            }
        }
        flush(camera);
        Arrays.sort(smoke, 0, smokeCount, Comparator.comparingDouble((Puff puff) -> puff.depth).reversed());
        exhaust.begin();
        for (int index = 0; index < smokeCount; index++) {
            var puff = smoke[index];
            width.set(right).scl(puff.radius);
            length.set(camera.up).scl(puff.radius);
            exhaust.quad(puff.center, width, length, -1, 0, puff.alpha);
        }
        int drawnSmoke = exhaust.size();
        int flames = 0;
        for (var launch : launches) {
            for (int missile = 0; missile < launch.missiles && flames < MISSILES_PER_BATCH; missile++) {
                float t = progress(launch, missile, launch.attack.seconds);
                if (t < 0 || t >= 1) { continue; }
                position(launch, missile, t, point);
                position(launch, missile, Math.max(0, t - .003f), direction).sub(point).nor();
                width.set(right).scl(.5f);
                length.set(direction).scl(2.5f);
                exhaust.quad(point, width, length, 0, 1, .8f);
                flames++;
            }
        }
        exhaust.render(camera, drawnSmoke);
    }

    private void missile(Camera camera, Vector3 center, Vector3 forward) {
        if (mesh == null) { create(); }
        if (offset == vertices.length) { flush(camera); }
        side.set(forward).crs(Vector3.Z);
        if (side.isZero(.001f)) { side.set(Vector3.X); }
        side.nor().scl(.45f);
        up.set(side).crs(forward).nor().scl(.45f);
        tip.set(center).mulAdd(forward, 2.4f);
        a.set(center).add(up);
        b.set(center).mulAdd(up, -.5f).mulAdd(side, .866f);
        c.set(center).mulAdd(up, -.5f).mulAdd(side, -.866f);
        triangle(tip, a, b, .9f); triangle(tip, b, c, .62f); triangle(tip, c, a, .76f); triangle(a, c, b, .4f);
    }

    private void triangle(Vector3 a, Vector3 b, Vector3 c, float shade) {
        vertex(a, shade); vertex(b, shade); vertex(c, shade);
    }

    private void vertex(Vector3 value, float shade) {
        vertices[offset++] = value.x; vertices[offset++] = value.y; vertices[offset++] = value.z;
        vertices[offset++] = shade; vertices[offset++] = shade; vertices[offset++] = shade; vertices[offset++] = 1;
    }

    private void flush(Camera camera) {
        if (offset == 0) { return; }
        mesh.setVertices(vertices, 0, offset);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        shader.bind();
        shader.setUniformMatrix("u_projView", camera.combined);
        mesh.render(shader, GL20.GL_TRIANGLES, 0, offset / STRIDE);
        offset = 0;
    }

    private void create() {
        shader = new ShaderProgram("""
              attribute vec3 a_position;
              attribute vec4 a_color;
              uniform mat4 u_projView;
              varying vec4 v_color;
              void main() { v_color = a_color; gl_Position = u_projView * vec4(a_position, 1.0); }
              """, """
              #ifdef GL_ES
              precision mediump float;
              #endif
              varying vec4 v_color;
              void main() { gl_FragColor = v_color; }
              """);
        if (!shader.isCompiled()) {
            String error = shader.getLog();
            shader.dispose();
            shader = null;
            throw new IllegalStateException(error);
        }
        vertices = new float[MISSILES_PER_BATCH * 12 * STRIDE];
        mesh = new Mesh(false, MISSILES_PER_BATCH * 12, 0, VertexAttribute.Position(), VertexAttribute.ColorUnpacked());
    }

    private static float noise(int value) {
        value ^= value >>> 16; value *= 0x45d9f3b; value ^= value >>> 16;
        return (value & 0xFFFFFF) / 16777216f;
    }

    @Override
    public void dispose() {
        begin();
        exhaust.dispose();
        if (mesh != null) { mesh.dispose(); mesh = null; }
        if (shader != null) { shader.dispose(); shader = null; }
        vertices = null;
        Arrays.fill(smoke, null);
    }
}
