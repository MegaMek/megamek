/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/** Scene grading and bounded analytic height fog. Owns its GL resources. */
final class GpuAtmosphere implements Disposable {
    private static final String SHADERS = "megamek/client/ui/clientGUI/boardview/gpu/";
    private final Mesh quad;
    private final ModelBatch depthBatch;
    private final ShaderProgram fogShader;
    private final ShaderProgram compositeShader;
    private FrameBuffer sceneColor;
    private FrameBuffer sceneDepth;
    private FrameBuffer fog;
    private GpuWeatherParticles particles;
    private BoardAtmosphere.Settings settings;
    private BoardAtmosphere.Lighting lighting;
    private float clock;
    private float stormClock;
    private boolean captureDepth;

    GpuAtmosphere() {
        fogShader = shader("atmosphere-fog.frag");
        try {
            compositeShader = shader("atmosphere-composite.frag");
        } catch (RuntimeException failure) {
            fogShader.dispose();
            throw failure;
        }
        quad = screenQuad();
        DepthShader.Config depthConfig = new DepthShader.Config();
        // Shadow shaders cull front faces by default. Camera depth must use the visible front surface.
        depthConfig.defaultCullFace = GL20.GL_BACK;
        depthBatch = new ModelBatch(new DepthShaderProvider(depthConfig));
        configure(BoardAtmosphere.DEFAULTS);
    }

    static ShaderProgram shader(String fragment) {
        ShaderProgram result = new ShaderProgram(Gdx.files.classpath(SHADERS + "atmosphere.vert"),
              Gdx.files.classpath(SHADERS + fragment));
        if (!result.isCompiled()) {
            String log = result.getLog();
            result.dispose();
            throw new IllegalStateException("GPU atmosphere shader " + fragment + ": " + log);
        }
        return result;
    }

    /** Shared screen-space geometry; the caller owns the returned mesh. */
    static Mesh screenQuad() {
        Mesh result = new Mesh(true, 4, 6, VertexAttribute.Position(), VertexAttribute.TexCoords(0));
        result.setVertices(new float[] { -1, -1, 0, 0, 0, 1, -1, 0, 1, 0,
              1, 1, 0, 1, 1, -1, 1, 0, 0, 1 });
        result.setIndices(new short[] { 0, 1, 2, 0, 2, 3 });
        return result;
    }

    void configure(BoardAtmosphere.Settings next) {
        if (!next.equals(settings)) {
            if (settings == null || settings.effects().lightning() == 0) {
                stormClock = 0;
            }
            settings = next;
            lighting = BoardAtmosphere.lighting(next);
        }
    }

    BoardAtmosphere.Lighting lighting() {
        return lighting;
    }

    void begin(int width, int height, float delta) {
        begin(width, height, delta, false);
    }

    void begin(int width, int height, float delta, boolean requireSceneDepth) {
        captureDepth = hasFog() || requireSceneDepth;
        int pixelsWide = Math.max(1, HdpiUtils.toBackBufferX(width));
        int pixelsHigh = Math.max(1, HdpiUtils.toBackBufferY(height));
        if (sceneColor == null || sceneColor.getWidth() != pixelsWide || sceneColor.getHeight() != pixelsHigh) {
            disposeBuffers();
            sceneColor = buffer(pixelsWide, pixelsHigh, true);
        }
        if (captureDepth && sceneDepth == null) {
            sceneDepth = buffer(pixelsWide, pixelsHigh, true);
        }
        if (hasFog() && fog == null) {
            fog = buffer(Math.max(1, pixelsWide / 4), Math.max(1, pixelsHigh / 4), false);
        }
        clock = (clock + Math.min(delta, 0.1f)) % 3600;
        stormClock = settings.effects().lightning() > 0 ? (stormClock + Math.min(delta, 0.1f)) % 7 : 0;
        sceneColor.begin();
        Gdx.gl.glDepthMask(true);
        // Background alpha replaces a depth lookup when fog is disabled.
        ScreenUtils.clear(lighting.sky().r, lighting.sky().g, lighting.sky().b, 0, true);
    }

    static FrameBuffer buffer(int width, int height, boolean depth) {
        GLFrameBuffer.FrameBufferBuilder builder = new GLFrameBuffer.FrameBufferBuilder(width, height);
        builder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
        if (depth) {
            // Small decal offsets need the same 24-bit precision as the native board window.
            builder.addDepthRenderBuffer(GL30.GL_DEPTH_COMPONENT24);
        }
        FrameBuffer result = builder.build();
        result.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return result;
    }

    /** Ends scene capture, derives depth using the same geometry, then composites only the board viewport. */
    void end(Camera camera, GpuTerrain terrain, List<ModelInstance> units, BoardScene board, int bottom) {
        end(camera, terrain, units, board, bottom, null);
    }

    void end(Camera camera, GpuTerrain terrain, List<ModelInstance> units, BoardScene board, int bottom,
          GpuFieldOfView fieldOfView) {
        boolean fovActive = fieldOfView != null && fieldOfView.active();
        sceneColor.end();
        if (captureDepth) {
            sceneDepth.begin();
            Gdx.gl.glDepthMask(true);
            ScreenUtils.clear(1, 1, 1, 1, true);
            terrain.renderDepth(camera, units, depthBatch);
            sceneDepth.end();
        }
        if (hasFog()) {
            renderFog(camera, board);
        }
        HdpiUtils.glViewport(0, bottom, (int) camera.viewportWidth, (int) camera.viewportHeight);
        screenState();
        compositeShader.bind();
        sceneColor.getColorBufferTexture().bind(0);
        if (hasFog() || fovActive) {
            sceneDepth.getColorBufferTexture().bind(1);
        }
        if (hasFog()) {
            fog.getColorBufferTexture().bind(2);
        }
        compositeShader.setUniformf("u_fovEnabled", fovActive ? 1 : 0);
        if (fovActive) {
            fieldOfView.bind(compositeShader, camera);
        }
        compositeShader.setUniformi("u_scene", 0);
        compositeShader.setUniformi("u_depth", hasFog() || fovActive ? 1 : 0);
        compositeShader.setUniformi("u_fog", hasFog() ? 2 : 0);
        compositeShader.setUniformf("u_fogEnabled", hasFog() ? 1 : 0);
        compositeShader.setUniformf("u_fogSize", hasFog() ? fog.getWidth() : 1, hasFog() ? fog.getHeight() : 1);
        compositeShader.setUniformf("u_depthRange", camera.far - camera.near);
        compositeShader.setUniformf("u_edgeScale", BoardGeometry.LEVEL);
        compositeShader.setUniformf("u_exposure", lighting.exposureScale(settings.exposure()));
        compositeShader.setUniformf("u_tint", lighting.tint().r, lighting.tint().g, lighting.tint().b);
        compositeShader.setUniformf("u_saturation", lighting.saturation());
        compositeShader.setUniformf("u_sky", lighting.sky().r, lighting.sky().g, lighting.sky().b);
        compositeShader.setUniformf("u_horizon", lighting.fog().r, lighting.fog().g, lighting.fog().b);
        // Strike promptly when enabled, then at seven-second intervals, with a quick attack and longer decay.
        float attack = MathUtils.clamp((stormClock - 0.35f) / 0.06f, 0, 1);
        float decay = 1 - MathUtils.clamp((stormClock - 0.41f) / 0.54f, 0, 1);
        compositeShader.setUniformf("u_lightning", attack * decay * decay * settings.effects().lightning() * 1.1f);
        quad.render(compositeShader, GL20.GL_TRIANGLES);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glDepthMask(true);
    }

    /** Borrowed packed camera depth, valid after end() when a scene effect requested it. */
    Texture depthTexture() {
        return captureDepth ? sceneDepth.getColorBufferTexture() : null;
    }

    private boolean hasFog() {
        return settings.fog() > 0 || settings.haze() > 0;
    }

    /** Draw after restoring opaque depth, before tactical markings and screen annotations. */
    void renderWeather(Camera camera, BoardScene board) {
        if (settings.effects().hasParticles()) {
            if (particles == null) {
                particles = new GpuWeatherParticles();
            }
            particles.render(camera, board, settings.effects(), lighting.ambient(), clock);
        }
    }

    /** Re-establish opaque depth for crisp, ungraded board markings after compositing. */
    void restoreDepth(Camera camera, GpuTerrain terrain, List<ModelInstance> units) {
        Gdx.gl.glColorMask(false, false, false, false);
        Gdx.gl.glDepthMask(true);
        try {
            terrain.renderDepth(camera, units, depthBatch);
        } finally {
            Gdx.gl.glColorMask(true, true, true, true);
        }
    }

    private void renderFog(Camera camera, BoardScene board) {
        float base = BoardGeometry.weatherBase(board);
        float height = settings.fogHeight() * BoardGeometry.LEVEL;
        fog.begin();
        screenState();
        fogShader.bind();
        sceneDepth.getColorBufferTexture().bind(0);
        fogShader.setUniformi("u_depth", 0);
        fogShader.setUniformMatrix("u_inverseView", camera.invProjectionView);
        fogShader.setUniformf("u_direction", camera.direction);
        fogShader.setUniformf("u_boundsMin", -BoardGeometry.WIDTH, -(board.height() + 1) * BoardGeometry.HEIGHT,
              base - BoardGeometry.LEVEL);
        fogShader.setUniformf("u_boundsMax", (board.width() + 1) * BoardGeometry.WIDTH * 0.75f,
              BoardGeometry.HEIGHT, base + height * 6);
        fogShader.setUniformf("u_fog", settings.fog() * 0.7f / BoardGeometry.LEVEL, height, base);
        fogShader.setUniformf("u_haze", settings.haze() * 0.035f / BoardGeometry.WIDTH);
        fogShader.setUniformf("u_noiseScale", 1 / (BoardGeometry.WIDTH * 1.8f));
        fogShader.setUniformf("u_clock", clock);
        fogShader.setUniformf("u_fogColor", lighting.fog().r, lighting.fog().g, lighting.fog().b);
        fogShader.setUniformf("u_maxOpacity", BoardAtmosphere.MAX_FOG_OPACITY);
        quad.render(fogShader, GL20.GL_TRIANGLES);
        fog.end();
    }

    private static void screenState() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDepthMask(false);
    }

    private void disposeBuffers() {
        if (sceneColor != null) {
            sceneColor.dispose();
            sceneColor = null;
        }
        if (sceneDepth != null) {
            sceneDepth.dispose();
            sceneDepth = null;
        }
        if (fog != null) {
            fog.dispose();
            fog = null;
        }
    }

    @Override
    public void dispose() {
        disposeBuffers();
        if (particles != null) {
            particles.dispose();
        }
        quad.dispose();
        depthBatch.dispose();
        fogShader.dispose();
        compositeShader.dispose();
    }
}
