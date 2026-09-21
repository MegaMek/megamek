/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/** Scene grading, bounded fog/shafts and wind-driven cloud shadows. Owns its GL resources. */
final class GpuAtmosphere implements Disposable {
    /** Maximum local deviation from the shared ground-layer height, in terrain levels. */
    static final float FOG_HEIGHT_VARIATION = 1.25f;
    /** Thins the spaces between fog banks; 0 gives a uniform layer, 1 permits clear gaps. */
    static final float FOG_DENSITY_VARIATION = 0.85f;
    /** Gentle intrinsic fog travel in hex widths per second when the scenario wind is calm. */
    static final float FOG_CALM_DRIFT = 0.06f;
    static final float MAX_SAND_OPACITY = 0.25f;

    /** Local visual controls; weather and pressure still come from the scenario snapshot. */
    record Options(float rays, boolean fixedSun, float minCloudShadow, float maxCloudShadow, float sunGlare,
          float fogHeightVariation, float fogDensityVariation, float moonShadowContrast, float taintStrength, float fogCalmDrift) {
        static final Options DEFAULTS = new Options(0.5f, false);

        Options(float rays, boolean fixedSun) {
            this(rays, fixedSun, GpuClouds.MIN_SHADOW_STRENGTH, GpuClouds.MAX_SHADOW_STRENGTH);
        }

        Options(float rays, boolean fixedSun, float minCloudShadow, float maxCloudShadow) {
            this(rays, fixedSun, minCloudShadow, maxCloudShadow, 0.35f);
        }

        Options(float rays, boolean fixedSun, float minCloudShadow, float maxCloudShadow, float sunGlare) {
            this(rays, fixedSun, minCloudShadow, maxCloudShadow, sunGlare, FOG_HEIGHT_VARIATION, FOG_DENSITY_VARIATION);
        }

        Options(float rays, boolean fixedSun, float minCloudShadow, float maxCloudShadow, float sunGlare,
              float fogHeightVariation, float fogDensityVariation) {
            this(rays, fixedSun, minCloudShadow, maxCloudShadow, sunGlare, fogHeightVariation, fogDensityVariation,
                  BoardAtmosphere.MOONLIGHT_SHADOW_CONTRAST);
        }

        Options(float rays, boolean fixedSun, float minCloudShadow, float maxCloudShadow, float sunGlare,
              float fogHeightVariation, float fogDensityVariation, float moonShadowContrast) {
            this(rays, fixedSun, minCloudShadow, maxCloudShadow, sunGlare, fogHeightVariation, fogDensityVariation,
                  moonShadowContrast, BoardAtmosphere.DEFAULT_TAINT_STRENGTH);
        }

        Options(float rays, boolean fixedSun, float minCloudShadow, float maxCloudShadow, float sunGlare,
              float fogHeightVariation, float fogDensityVariation, float moonShadowContrast, float taintStrength) {
            this(rays, fixedSun, minCloudShadow, maxCloudShadow, sunGlare, fogHeightVariation, fogDensityVariation,
                  moonShadowContrast, taintStrength, FOG_CALM_DRIFT);
        }

        Options {
            if (!Float.isFinite(rays) || !Float.isFinite(minCloudShadow) || !Float.isFinite(maxCloudShadow)
                  || !Float.isFinite(sunGlare) || !Float.isFinite(fogHeightVariation) || !Float.isFinite(fogDensityVariation)
                  || !Float.isFinite(moonShadowContrast) || !Float.isFinite(taintStrength) || !Float.isFinite(fogCalmDrift)) {
                throw new IllegalArgumentException("Atmosphere controls must be finite");
            }
            rays = MathUtils.clamp(rays, 0, 2);
            minCloudShadow = MathUtils.clamp(minCloudShadow, 0, 1);
            maxCloudShadow = MathUtils.clamp(maxCloudShadow, minCloudShadow, 1);
            sunGlare = MathUtils.clamp(sunGlare, 0, 1);
            fogHeightVariation = MathUtils.clamp(fogHeightVariation, 0, 4);
            fogDensityVariation = MathUtils.clamp(fogDensityVariation, 0, 1);
            moonShadowContrast = MathUtils.clamp(moonShadowContrast, 0, 1);
            taintStrength = MathUtils.clamp(taintStrength, 0, 2);
            fogCalmDrift = MathUtils.clamp(fogCalmDrift, 0, 0.3f);
        }

        float cloudShadowStrength(float cover) {
            return cover <= 0 ? 0 : MathUtils.lerp(minCloudShadow, maxCloudShadow, MathUtils.clamp(cover, 0, 1));
        }
    }

    private static final String SHADERS = "megamek/client/ui/clientGUI/boardview/gpu/";
    private final Mesh quad;
    private final ShaderProgram fogShader;
    private final ShaderProgram compositeShader;
    private FrameBuffer sceneColor;
    private Texture sceneDepth;
    private FrameBuffer fog;
    private GpuWeatherParticles particles;
    private GpuClouds clouds;
    private boolean cloudsActive;
    private BoardAtmosphere.Settings settings;
    /** Cached world lighting and its render-owned camera-relative result; neither is game state. */
    private BoardAtmosphere.Lighting worldLighting;
    private BoardAtmosphere.Lighting lighting;
    private final Vector3 glareRight = new Vector3();
    private float clock;
    private float stormClock;
    private Options options = Options.DEFAULTS;
    private List<BoardScene.Tile> groundTiles;
    private float groundLevel;
    private float groundBase;
    private Texture groundNoise;
    private final GroundMotion groundMotion = new GroundMotion();
    private final Vector3 groundProjection = new Vector3();
    private final float[] layerScreenBounds = new float[4];

    /** Render-owned integrated travel in periodic noise cells, shared by both board cameras. */
    static final class GroundMotion {
        final Vector2 fog = new Vector2();
        final Vector2 sand = new Vector2();
        float grains;
        private double fogSeconds;

        void advance(BoardAtmosphere.Effects effects, float delta) {
            advance(effects, delta, FOG_CALM_DRIFT);
        }

        void advance(BoardAtmosphere.Effects effects, float delta, float calmDrift) {
            float elapsed = Float.isFinite(delta) ? MathUtils.clamp(delta, 0, 0.1f) : 0;
            float x = MathUtils.sinDeg(effects.windDirection()), y = MathUtils.cosDeg(effects.windDirection());
            // Slowly bending calm flow, blended into the chosen wind vector by light-gale strength.
            // Integrate at the frame midpoint, in the existing noise coordinates: no new shader work.
            float angle = 35 + 30 * (float) Math.sin((fogSeconds + elapsed * 0.5) * Math.PI * 2 / 48);
            fogSeconds = (fogSeconds + elapsed) % 48;
            float windBlend = MathUtils.clamp(effects.wind() / 0.2f, 0, 1);
            float fogX = MathUtils.lerp(MathUtils.sinDeg(angle) * calmDrift, x * effects.wind() * 0.3f, windBlend);
            float fogY = MathUtils.lerp(MathUtils.cosDeg(angle) * calmDrift, y * effects.wind() * 0.3f, windBlend);
            move(fog, fogX, fogY, elapsed * 0.5f);
            float travel = elapsed * (0.65f + 1.5f * effects.wind());
            move(sand, x, y, travel * 0.5f);
            grains = wrap(grains + travel * 24);
        }

        private static void move(Vector2 offset, float x, float y, float distance) {
            offset.set(wrap(offset.x + x * distance), wrap(offset.y + y * distance));
        }

        private static float wrap(float value) {
            return value - (float) Math.floor(value / 256) * 256;
        }
    }

    GpuAtmosphere() {
        fogShader = shader("atmosphere-fog.frag");
        try {
            compositeShader = shader("atmosphere-composite.frag");
        } catch (RuntimeException failure) {
            fogShader.dispose();
            throw failure;
        }
        quad = screenQuad();
        configure(BoardAtmosphere.DEFAULTS);
    }

    static ShaderProgram shader(String fragment) {
        String source = Gdx.files.classpath(SHADERS + fragment).readString("UTF-8");
        if (source.contains("// CLOUD_SHADOW")) {
            source = source.replace("// CLOUD_SHADOW", Gdx.files.classpath(SHADERS + "cloud-shadow.glsl").readString("UTF-8"));
        }
        if (source.contains("// SCATTERING_PHASE")) {
            source = source.replace("// SCATTERING_PHASE", Gdx.files.classpath(SHADERS + "scattering-phase.glsl").readString("UTF-8"));
        }
        if (source.contains("// GROUND_LAYER")) {
            source = source.replace("// GROUND_LAYER", Gdx.files.classpath(SHADERS + "ground-layer.glsl").readString("UTF-8"));
        }
        String vertex = Gdx.files.classpath(SHADERS + "atmosphere.vert").readString("UTF-8");
        String prefix = "";
        if (source.contains("// SUN_VISIBILITY")) {
            String visibility = Gdx.files.classpath(SHADERS + "sun-visibility.glsl").readString("UTF-8");
            vertex = vertex.replace("// SUN_VISIBILITY", visibility);
            source = source.replace("// SUN_VISIBILITY", visibility);
            var units = BufferUtils.newIntBuffer(1);
            Gdx.gl.glGetIntegerv(GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, units);
            if (units.get(0) > 0) { prefix = "#define VERTEX_SUN_VISIBILITY\n"; }
        }
        ShaderProgram result = new ShaderProgram(prefix + vertex, prefix + source);
        if (!result.isCompiled()) {
            String log = result.getLog();
            result.dispose();
            throw new IllegalStateException("GPU atmosphere shader " + fragment + ": " + log);
        }
        return result;
    }

    /** Periodic two-channel noise, used by clouds and ground weather. The caller owns the texture. */
    static Texture noise() {
        Random random = new Random(0x4d4d434c);
        int[] values = new int[256 * 256];
        for (int i = 0; i < values.length; i++) { values[i] = random.nextInt(256); }
        Pixmap pixels = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        try {
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    int adjacent = values[((y + 17) % 256) * 256 + (x + 37) % 256];
                    pixels.drawPixel(x, y, (values[y * 256 + x] << 24) | (adjacent << 16) | 255);
                }
            }
            Texture result = new Texture(pixels);
            result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            result.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            return result;
        } finally {
            pixels.dispose();
        }
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
            worldLighting = BoardAtmosphere.lighting(next, options.moonShadowContrast(), options.taintStrength());
            lighting = worldLighting;
        }
    }

    BoardAtmosphere.Lighting lighting() {
        return lighting;
    }

    /** Apply after the camera's final pose, before either geometry shadows or cloud transmission. */
    void updateLight(Camera camera) {
        lighting = options.fixedSun() ? worldLighting.relativeTo(camera) : worldLighting;
    }

    void setOptions(Options options) {
        if (settings != null && (this.options.moonShadowContrast() != options.moonShadowContrast()
              || this.options.taintStrength() != options.taintStrength())) {
            worldLighting = BoardAtmosphere.lighting(settings, options.moonShadowContrast(), options.taintStrength());
            lighting = worldLighting;
        }
        this.options = options;
    }

    /** Prepare clouds and surface weather before scene capture; cameras share their field and wind timeline. */
    void prepareClouds(GpuTerrain terrain, BoardScene board, float delta) {
        terrain.setWetness(BoardAtmosphere.wetness(settings));
        cloudsActive = settings.clouds() > 0 && lighting.hasDirectLight();
        if (cloudsActive) {
            if (clouds == null) { clouds = new GpuClouds(quad); }
            clouds.update(settings, lighting, board, delta, options.cloudShadowStrength(settings.clouds()));
            terrain.environment().set(clouds.shadow());
        } else {
            terrain.environment().remove(GpuCloudShadow.TYPE);
        }
    }

    void begin(int width, int height, float delta) {
        int pixelsWide = Math.max(1, HdpiUtils.toBackBufferX(width));
        int pixelsHigh = Math.max(1, HdpiUtils.toBackBufferY(height));
        if (sceneColor == null || sceneColor.getWidth() != pixelsWide || sceneColor.getHeight() != pixelsHigh) {
            disposeBuffers();
            sceneColor = buffer(pixelsWide, pixelsHigh, false);
            try {
                sceneDepth = attachDepthTexture(sceneColor);
            } catch (RuntimeException failure) {
                disposeBuffers();
                throw failure;
            }
        }
        if (hasScattering() && fog == null) {
            fog = buffer(Math.max(1, pixelsWide / 4), Math.max(1, pixelsHigh / 4), false);
        }
        clock = (clock + Math.min(delta, 0.1f)) % 3600;
        stormClock = settings.effects().lightning() > 0 ? (stormClock + Math.min(delta, 0.1f)) % 7 : 0;
        groundMotion.advance(settings.effects(), delta, options.fogCalmDrift());
        sceneColor.begin();
        Gdx.gl.glDepthMask(true);
        // Background alpha distinguishes the sky from transparent scene color.
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

    /** Caller owns the returned depth texture separately from the target; uses the existing desktop GL20 context. */
    static Texture attachDepthTexture(FrameBuffer target) {
        Texture depth = new Texture(new GLOnlyTextureData(target.getWidth(), target.getHeight(), 0,
              GL30.GL_DEPTH_COMPONENT24, GL20.GL_DEPTH_COMPONENT, GL20.GL_UNSIGNED_INT));
        try {
            depth.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            depth.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            target.bind();
            Gdx.gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT,
                  GL20.GL_TEXTURE_2D, depth.getTextureObjectHandle(), 0);
            if (Gdx.gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER) != GL20.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("GPU scene depth attachment is incomplete");
            }
            return depth;
        } catch (RuntimeException failure) {
            depth.dispose();
            throw failure;
        } finally {
            FrameBuffer.unbind();
        }
    }

    /** Composite color and opaque depth together; the scene's geometry is submitted only once. */
    void end(Camera camera, GpuTerrain terrain, BoardScene board, int bottom) {
        end(camera, terrain, board, bottom, null);
    }

    void end(Camera camera, GpuTerrain terrain, BoardScene board, int bottom,
          GpuFieldOfView fieldOfView) {
        boolean fovActive = fieldOfView != null && fieldOfView.active();
        sceneColor.end();
        if (hasScattering()) {
            renderFog(camera, terrain, board);
        }
        HdpiUtils.glViewport(0, bottom, (int) camera.viewportWidth, (int) camera.viewportHeight);
        screenState();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_ALWAYS);
        Gdx.gl.glDepthMask(true);
        compositeShader.bind();
        sceneColor.getColorBufferTexture().bind(0);
        sceneDepth.bind(1);
        if (hasScattering()) {
            fog.getColorBufferTexture().bind(2);
        }
        compositeShader.setUniformf("u_fovEnabled", fovActive ? 1 : 0);
        if (fovActive) {
            fieldOfView.bind(compositeShader, camera);
        }
        compositeShader.setUniformi("u_scene", 0);
        compositeShader.setUniformi("u_depth", 1);
        compositeShader.setUniformi("u_fog", hasScattering() ? 2 : 0);
        compositeShader.setUniformf("u_fogEnabled", hasScattering() ? 1 : 0);
        if (hasScattering()) {
            // renderFog just projected its volume. Sand may subsequently bind a different, lower volume.
            compositeShader.setUniform4fv("u_scatteringBounds", layerScreenBounds, 0, 4);
        }
        compositeShader.setUniformf("u_fogSize", hasScattering() ? fog.getWidth() : 1, hasScattering() ? fog.getHeight() : 1);
        compositeShader.setUniformf("u_depthRange", camera.far - camera.near);
        compositeShader.setUniformf("u_edgeScale", BoardGeometry.LEVEL);
        compositeShader.setUniformf("u_exposure", lighting.exposureScale(settings.exposure()));
        compositeShader.setUniformf("u_tint", lighting.tint().r, lighting.tint().g, lighting.tint().b);
        compositeShader.setUniformf("u_saturation", lighting.saturation());
        compositeShader.setUniformf("u_sky", lighting.sky().r, lighting.sky().g, lighting.sky().b);
        compositeShader.setUniformf("u_horizon", lighting.horizon().r, lighting.horizon().g, lighting.horizon().b);
        compositeShader.setUniformMatrix("u_inverseView", camera.invProjectionView);
        compositeShader.setUniformf("u_groundBoard", board.width(), board.height(), BoardGeometry.WIDTH, BoardGeometry.HEIGHT);
        bindSand(camera, board);
        bindSunGlare(camera);
        // Strike promptly when enabled, then at seven-second intervals, with a quick attack and longer decay.
        float attack = MathUtils.clamp((stormClock - 0.35f) / 0.06f, 0, 1);
        float decay = 1 - MathUtils.clamp((stormClock - 0.41f) / 0.54f, 0, 1);
        compositeShader.setUniformf("u_lightning", attack * decay * decay * settings.effects().lightning() * 1.1f);
        quad.render(compositeShader, GL20.GL_TRIANGLES);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
    }

    /** Angular projection for the orthographic board: panning/zooming cannot move a distant light source. */
    private void bindSunGlare(Camera camera) {
        float aspect = camera.viewportWidth / Math.max(1, camera.viewportHeight);
        float forward = -camera.direction.dot(lighting.direction());
        float energy = Math.max(worldLighting.direct().r, Math.max(worldLighting.direct().g, worldLighting.direct().b));
        if (options.sunGlare() == 0 || !lighting.sunlight() || energy <= 0 || forward <= 0.15f) {
            compositeShader.setUniformf("u_sunGlare", 0, 0, 0, aspect);
            return;
        }
        glareRight.set(camera.direction).crs(camera.up).nor();
        // An approximately 80-degree vertical angular field keeps a low sun near the tilted board's upper edge.
        float projection = 1.7f * forward;
        float x = 0.5f - glareRight.dot(lighting.direction()) / (projection * aspect);
        float y = 0.5f - camera.up.dot(lighting.direction()) / projection;
        float facing = MathUtils.clamp((forward - 0.15f) / 0.6f, 0, 1);
        facing *= facing * (3 - 2 * facing);
        float outside = Math.max(Math.max(-x, x - 1) * aspect, Math.max(-y, y - 1));
        float edge = 1 - MathUtils.clamp(outside / 0.25f, 0, 1);
        edge *= edge * (3 - 2 * edge);
        // Use world-source energy/elevation even in fixed-sun mode, whose surface-light compensation varies with tilt.
        float horizon = 0.35f + 0.65f * (1 - Math.abs(worldLighting.direction().z));
        float clear = 1 - settings.clouds() * (settings.pressure().isThin() ? 0.35f : 0.95f);
        float visibility = clear * clear * (1 - settings.fog() * 0.8f) * (1 - settings.haze() * 0.4f);
        compositeShader.setUniformf("u_sunGlare", x, y,
              options.sunGlare() * energy * horizon * facing * edge * visibility, aspect);
        compositeShader.setUniformf("u_glareColor", worldLighting.direct().r / energy,
              worldLighting.direct().g / energy, worldLighting.direct().b / energy);
    }

    /** Borrowed hardware camera depth in the red channel, valid after end(). */
    Texture depthTexture() {
        return sceneDepth;
    }

    private boolean hasFog() {
        return settings.fog() > 0 || settings.haze() > 0;
    }

    private boolean hasRays() {
        return cloudsActive && lighting.sunlight() && options.rays() > 0;
    }

    private boolean hasScattering() {
        return hasFog() || hasRays();
    }

    private void updateGroundBase(BoardScene board) {
        if (groundTiles != board.tiles() || groundLevel != BoardGeometry.LEVEL) {
            groundTiles = board.tiles();
            groundLevel = BoardGeometry.LEVEL;
            groundBase = BoardGeometry.weatherBase(board);
        }
    }

    private void bindGroundLayer(ShaderProgram shader, Camera camera, BoardScene board, float top, int noiseUnit) {
        shader.setUniformMatrix("u_inverseView", camera.invProjectionView);
        shader.setUniformf("u_groundBoard", board.width(), board.height(), BoardGeometry.WIDTH, BoardGeometry.HEIGHT);
        shader.setUniformf("u_direction", camera.direction);
        shader.setUniformf("u_boundsMin", -BoardGeometry.WIDTH, -(board.height() + 1) * BoardGeometry.HEIGHT,
              groundBase);
        shader.setUniformf("u_boundsMax", (board.width() + 1) * BoardGeometry.WIDTH * 0.75f, BoardGeometry.HEIGHT, top);
        layerScreenBounds[0] = Float.POSITIVE_INFINITY;
        layerScreenBounds[1] = Float.POSITIVE_INFINITY;
        layerScreenBounds[2] = Float.NEGATIVE_INFINITY;
        layerScreenBounds[3] = Float.NEGATIVE_INFINITY;
        // Orthographic projection preserves the convex box: outside these bounds the air column is empty.
        for (int corner = 0; corner < 8; corner++) {
            groundProjection.set((corner & 1) == 0 ? -BoardGeometry.WIDTH : (board.width() + 1) * BoardGeometry.WIDTH * 0.75f,
                  (corner & 2) == 0 ? -(board.height() + 1) * BoardGeometry.HEIGHT : BoardGeometry.HEIGHT,
                  (corner & 4) == 0 ? groundBase : top);
            camera.project(groundProjection, 0, 0, 1, 1);
            layerScreenBounds[0] = Math.min(layerScreenBounds[0], groundProjection.x);
            layerScreenBounds[1] = Math.min(layerScreenBounds[1], groundProjection.y);
            layerScreenBounds[2] = Math.max(layerScreenBounds[2], groundProjection.x);
            layerScreenBounds[3] = Math.max(layerScreenBounds[3], groundProjection.y);
        }
        // Include the full reduced-resolution reconstruction footprint at the volume's outer edge.
        float padX = 8f / sceneColor.getWidth(), padY = 8f / sceneColor.getHeight();
        layerScreenBounds[0] -= padX;
        layerScreenBounds[1] -= padY;
        layerScreenBounds[2] += padX;
        layerScreenBounds[3] += padY;
        shader.setUniform4fv("u_layerScreenBounds", layerScreenBounds, 0, 4);
        if (groundNoise == null) {
            // Texture construction binds its active unit. Do not replace the already-bound scene/depth sampler.
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + noiseUnit);
            groundNoise = noise();
        }
        groundNoise.bind(noiseUnit);
        shader.setUniformi("u_layerNoise", noiseUnit);
    }

    private void bindSand(Camera camera, BoardScene board) {
        float strength = settings.effects().sand();
        if (strength > 0) { updateGroundBase(board); }
        float height = settings.groundLayerHeight() * BoardGeometry.LEVEL;
        compositeShader.setUniformf("u_sand", strength, height, groundBase, 1 / BoardGeometry.LEVEL);
        compositeShader.setUniformf("u_sandMaxOpacity", MAX_SAND_OPACITY);
        if (strength <= 0) { return; }
        bindGroundLayer(compositeShader, camera, board, groundBase + height * 3, 4);
        compositeShader.setUniformf("u_sandWind", MathUtils.sinDeg(settings.effects().windDirection()),
              MathUtils.cosDeg(settings.effects().windDirection()), groundMotion.grains, 1 / BoardGeometry.WIDTH);
        compositeShader.setUniformf("u_sandOffset", groundMotion.sand);
        // Borrow scene illumination so dust does not glow on moonless maps.
        compositeShader.setUniformf("u_sandLight", lighting.ambient().r + lighting.direct().r * 0.35f,
              lighting.ambient().g + lighting.direct().g * 0.35f, lighting.ambient().b + lighting.direct().b * 0.35f);
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

    private void renderFog(Camera camera, GpuTerrain terrain, BoardScene board) {
        updateGroundBase(board);
        float base = groundBase;
        float height = settings.groundLayerHeight() * BoardGeometry.LEVEL;
        fog.begin();
        screenState();
        fogShader.bind();
        sceneDepth.bind(0);
        fogShader.setUniformi("u_depth", 0);
        float variation = options.fogHeightVariation() * BoardGeometry.LEVEL;
        float top = base + Math.max((height + variation) * 3, settings.haze() > 0 ? height * 6 : 0);
        bindGroundLayer(fogShader, camera, board,
              Math.max(top, hasRays() ? clouds.base() : base), 3);
        fogShader.setUniformf("u_fog", settings.fog() * 0.7f / BoardGeometry.LEVEL, height, base);
        fogShader.setUniformf("u_haze", settings.haze() * 0.035f / BoardGeometry.WIDTH);
        fogShader.setUniformf("u_fogVariation", variation, options.fogDensityVariation(), BoardGeometry.LEVEL * 0.25f);
        fogShader.setUniformf("u_noiseScale", 0.5f / BoardGeometry.WIDTH);
        fogShader.setUniformf("u_fogOffset", groundMotion.fog);
        fogShader.setUniformf("u_fogColor", lighting.fog().r, lighting.fog().g, lighting.fog().b);
        fogShader.setUniformf("u_maxOpacity", BoardAtmosphere.MAX_FOG_OPACITY);
        fogShader.setUniformf("u_rays", hasRays()
              ? BoardAtmosphere.clouds(settings).scattering() * options.rays() / BoardGeometry.WIDTH : 0);
        fogShader.setUniformf("u_sunColor", lighting.direct().r, lighting.direct().g, lighting.direct().b);
        fogShader.setUniformf("u_sunDirection", -lighting.direction().x, -lighting.direction().y, -lighting.direction().z);
        fogShader.setUniformi("u_cloudShadow", 0);
        fogShader.setUniformi("u_geometryShadow", 0);
        if (hasRays()) {
            clouds.bindShadow(fogShader, 1);
            var geometryShadow = terrain.environment().shadowMap;
            fogShader.setUniformf("u_hasGeometryShadow", geometryShadow == null ? 0 : 1);
            if (geometryShadow != null) {
                geometryShadow.getDepthMap().texture.bind(2);
                fogShader.setUniformi("u_geometryShadow", 2);
                fogShader.setUniformMatrix("u_geometryProjection", geometryShadow.getProjViewTrans());
            }
        }
        quad.render(fogShader, GL20.GL_TRIANGLES);
        fog.end();
    }

    static void screenState() {
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
        if (clouds != null) { clouds.dispose(); }
        if (groundNoise != null) { groundNoise.dispose(); }
        quad.dispose();
        fogShader.dispose();
        compositeShader.dispose();
    }
}
