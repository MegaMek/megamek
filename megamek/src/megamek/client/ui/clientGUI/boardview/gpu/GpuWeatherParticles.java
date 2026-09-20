/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/** A bounded, GPU-animated particle pool. Shares the world's depth buffer and never covers the tactical UI. */
final class GpuWeatherParticles implements Disposable {
    private static final int BASE_PARTICLES = 768;
    // 61,440 vertices keeps the shared pool within unsigned-short mesh indices.
    private static final int MAX_DENSITY = 20;
    private static final int[] DENSITY_MULTIPLIERS = { 6, 6, 4, MAX_DENSITY };
    private static final int PARTICLES = BASE_PARTICLES * MAX_DENSITY;
    private final ShaderProgram shader;
    private final Mesh mesh;
    private final Vector3 right = new Vector3();
    private final Vector3 origin = new Vector3();
    private final Vector3 extent = new Vector3();
    private List<BoardScene.Tile> tiles;
    private float level;
    private float bottom;
    private float top;

    GpuWeatherParticles() {
        String path = "megamek/client/ui/clientGUI/boardview/gpu/weather-particles";
        shader = new ShaderProgram(Gdx.files.classpath(path + ".vert"), Gdx.files.classpath(path + ".frag"));
        if (!shader.isCompiled()) {
            String log = shader.getLog();
            shader.dispose();
            throw new IllegalStateException("GPU precipitation shader: " + log);
        }
        float[] vertices = new float[PARTICLES * 4 * 5];
        short[] indices = new short[PARTICLES * 6];
        Random random = new Random(20260918);
        int[] corners = { 0, 1, 2, 2, 3, 0 };
        for (int particle = 0; particle < PARTICLES; particle++) {
            float x = random.nextFloat(), y = random.nextFloat(), z = random.nextFloat();
            for (int corner = 0; corner < 4; corner++) {
                int index = (particle * 4 + corner) * 5;
                vertices[index] = x;
                vertices[index + 1] = y;
                vertices[index + 2] = z;
                vertices[index + 3] = corner == 1 || corner == 2 ? 1 : -1;
                vertices[index + 4] = corner >= 2 ? 1 : -1;
            }
            for (int index = 0; index < 6; index++) {
                indices[particle * 6 + index] = (short) (particle * 4 + corners[index]);
            }
        }
        mesh = new Mesh(true, PARTICLES * 4, indices.length, VertexAttribute.Position(), VertexAttribute.TexCoords(0));
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
    }

    void render(Camera camera, BoardScene scene, BoardAtmosphere.Effects effects, Color light, float clock) {
        if (!effects.hasParticles() || !bounds(camera, scene)) {
            return;
        }
        right.set(camera.direction).crs(camera.up).nor();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        try {
            shader.bind();
            shader.setUniformMatrix("u_projView", camera.combined);
            shader.setUniformf("u_origin", origin);
            shader.setUniformf("u_extent", extent);
            shader.setUniformf("u_right", right);
            shader.setUniformf("u_up", camera.up);
            shader.setUniformf("u_clock", clock);
            shader.setUniformf("u_level", BoardGeometry.LEVEL);
            // Give subpixel sand a filtered footprint without turning distant grains into large flakes.
            shader.setUniformf("u_pixelSize", camera.frustum.planePoints[0].dst(camera.frustum.planePoints[1])
                  / Math.max(1, HdpiUtils.toBackBufferX((int) camera.viewportWidth)));
            shader.setUniformf("u_light", Math.min(1, light.r + 0.25f), Math.min(1, light.g + 0.25f),
                  Math.min(1, light.b + 0.25f));
            float[] strengths = { effects.rain(), effects.snow(), effects.hail(), effects.sand() };
            for (int kind = 0; kind < strengths.length; kind++) {
                if (strengths[kind] > 0) {
                    shader.setUniformf("u_kind", kind);
                    // Sand needs a fast horizontal stream even when the shared wind slider is at zero.
                    float wind = kind == 3 ? 14 + effects.wind() * 20 : effects.wind() * 5;
                    shader.setUniformf("u_wind", MathUtils.sinDeg(effects.windDirection()) * wind,
                          MathUtils.cosDeg(effects.windDirection()) * wind);
                    // Blowing sand needs a continuous drift even at the normal half-strength setting.
                    // Keep the curved response for light rain, snow and hail.
                    float strength = strengths[kind];
                    float density = kind == 3 ? strength * MAX_DENSITY
                          : strength * (1 + (DENSITY_MULTIPLIERS[kind] - 1) * strength * strength);
                    int count = Math.max(1, Math.round(BASE_PARTICLES * density));
                    mesh.render(shader, GL20.GL_TRIANGLES, 0, count * 6);
                }
            }
        } finally {
            Gdx.gl.glDepthMask(true);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    /** Bound the volume to the board and the camera's footprint at both ends of the weather layer. */
    private boolean bounds(Camera camera, BoardScene scene) {
        if (tiles != scene.tiles() || level != BoardGeometry.LEVEL) {
            tiles = scene.tiles();
            level = BoardGeometry.LEVEL;
            bottom = BoardGeometry.weatherBase(scene);
            top = Float.NEGATIVE_INFINITY;
            for (BoardScene.Tile tile : tiles) {
                float roof = tile.elevation();
                for (BoardScene.Feature feature : tile.features()) {
                    roof = Math.max(roof, tile.elevation() + feature.elevation() + feature.height());
                }
                top = Math.max(top, (roof + 12) * level);
            }
        }
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            Vector3 near = camera.frustum.planePoints[corner];
            for (float height : new float[] { bottom, top }) {
                float distance = (height - near.z) / camera.direction.z;
                float x = near.x + distance * camera.direction.x;
                float y = near.y + distance * camera.direction.y;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        minX = Math.max(-BoardGeometry.WIDTH, minX);
        minY = Math.max(-(scene.height() + 1) * BoardGeometry.HEIGHT, minY);
        maxX = Math.min((scene.width() + 1) * BoardGeometry.WIDTH * 0.75f, maxX);
        maxY = Math.min(BoardGeometry.HEIGHT, maxY);
        if (maxX <= minX || maxY <= minY) {
            return false;
        }
        origin.set(minX, minY, bottom);
        extent.set(maxX - minX, maxY - minY, top - bottom);
        return true;
    }

    @Override
    public void dispose() {
        mesh.dispose();
        shader.dispose();
    }
}
