/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/** One wind-advected transmission atlas for surface shadows and atmospheric shafts. GL-thread owned. */
final class GpuClouds implements Disposable {
    /** Fixed Low budget; the transmission shader integrates twelve samples through the layer. */
    private static final int SHADOW_SIZE = 192;
    private static final float DENSITY = 0.25f;
    /** Maximum opacity of a cloud patch, interpolated from sparse cover to full overcast. */
    static final float MIN_SHADOW_STRENGTH = 0.45f;
    static final float MAX_SHADOW_STRENGTH = 0.75f;
    /** Clouds vector motion speed */
    static final float MIN_MOTION_STRENGTH = 0.3f;
    static final float MAX_MOTION_STRENGTH = 1.0f;


    private final ShaderProgram shadowShader;
    private final Texture noise;
    private final FrameBuffer shadow;
    private final Mesh quad;
    private final Matrix4 projection = new Matrix4();
    private final Motion motion = new Motion();
    private final Vector3 sun = new Vector3();
    private final GpuCloudShadow attribute;
    private float base;
    private List<BoardScene.Tile> tiles;
    private float level;
    private float floor;
    private float highest;

    GpuClouds(Mesh quad) {
        this.quad = quad;
        shadowShader = GpuAtmosphere.shader("cloud-transmission.frag");
        try {
            noise = GpuAtmosphere.noise();
        } catch (RuntimeException failure) {
            shadowShader.dispose();
            throw failure;
        }
        try {
            shadow = GpuAtmosphere.buffer(SHADOW_SIZE, SHADOW_SIZE, false);
        } catch (RuntimeException failure) {
            noise.dispose();
            shadowShader.dispose();
            throw failure;
        }
        shadow.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        attribute = new GpuCloudShadow(shadow.getColorBufferTexture(), projection);
    }

    /** Integrate displacement so changing wind direction/speed never teleports the cloud field. */
    static final class Motion {
        final Vector3 offset = new Vector3();

        void advance(BoardAtmosphere.Effects effects, float delta) {
            float step = Float.isFinite(delta) ? MathUtils.clamp(delta, 0, 0.1f) : 0;
            float distance = MathUtils.lerp(MIN_MOTION_STRENGTH, MAX_MOTION_STRENGTH, effects.wind()) * step * 0.18f;
            offset.x = wrap(offset.x + MathUtils.sinDeg(effects.windDirection()) * distance);
            offset.y = wrap(offset.y + MathUtils.cosDeg(effects.windDirection()) * distance);
            offset.z = wrap(offset.z + step * 0.012f);
        }

        private static float wrap(float value) {
            float wrapped = value % 256;
            return wrapped < 0 ? wrapped + 256 : wrapped;
        }
    }

    void update(BoardAtmosphere.Settings settings, BoardAtmosphere.Lighting lighting, BoardScene board, float delta,
          float strength) {
        var profile = BoardAtmosphere.clouds(settings);
        motion.advance(settings.effects(), delta);
        sun.set(lighting.direction()).scl(-1);
        // The immutable terrain snapshot owns these elevations; wind/camera movement cannot change them.
        if (tiles != board.tiles() || level != BoardGeometry.LEVEL) {
            tiles = board.tiles();
            level = BoardGeometry.LEVEL;
            floor = BoardGeometry.weatherBase(board);
            highest = tiles.stream().mapToInt(BoardScene.Tile::elevation).max().orElse(0) * level;
        }
        base = highest + profile.altitude() * BoardGeometry.WIDTH;
        float thickness = profile.thickness() * BoardGeometry.WIDTH;
        float sx = sun.x / sun.z;
        float sy = sun.y / sun.z;
        // Include the whole air column as well as the board: shafts and elevated surfaces sample the same atlas.
        float margin = BoardGeometry.WIDTH * 2;
        float minX = -margin + Math.min(sx * (base - floor), -sx * thickness);
        float minY = -(board.height() + 1) * BoardGeometry.HEIGHT - margin
              + Math.min(sy * (base - floor), -sy * thickness);
        float spanX = (board.width() + 1) * BoardGeometry.WIDTH * 0.75f + margin * 2
              + Math.abs(sx) * (base + thickness - floor);
        float spanY = (board.height() + 2) * BoardGeometry.HEIGHT + margin * 2
              + Math.abs(sy) * (base + thickness - floor);
        projection.idt();
        projection.val[Matrix4.M00] = 1 / spanX;
        projection.val[Matrix4.M02] = -sx / spanX;
        projection.val[Matrix4.M03] = (sx * base - minX) / spanX;
        projection.val[Matrix4.M11] = 1 / spanY;
        projection.val[Matrix4.M12] = -sy / spanY;
        projection.val[Matrix4.M13] = (sy * base - minY) / spanY;
        projection.val[Matrix4.M22] = 1 / thickness;
        projection.val[Matrix4.M23] = -base / thickness;
        shadow.begin();
        GpuAtmosphere.screenState();
        shadowShader.bind();
        noise.bind(0);
        shadowShader.setUniformi("u_cloudNoise", 0);
        shadowShader.setUniformf("u_cloudWeather", settings.clouds(), profile.stratus(),
              profile.density() * DENSITY / BoardGeometry.WIDTH, 1 / (BoardGeometry.WIDTH * 1.6f));
        shadowShader.setUniformf("u_shadowStrength", strength);
        shadowShader.setUniformf("u_cloudLayer", base, thickness);
        shadowShader.setUniformf("u_cloudOffset", motion.offset);
        shadowShader.setUniformf("u_sunDirection", sun);
        shadowShader.setUniformf("u_cloudBounds", minX, minY, spanX, spanY);
        quad.render(shadowShader, GL20.GL_TRIANGLES);
        shadow.end();
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    }

    GpuCloudShadow shadow() {
        return attribute;
    }

    float base() {
        return base;
    }

    void bindShadow(ShaderProgram shader, int unit) {
        attribute.texture.bind(unit);
        shader.setUniformi("u_cloudShadow", unit);
        shader.setUniformMatrix("u_cloudProjection", projection);
    }


    @Override
    public void dispose() {
        shadow.dispose();
        noise.dispose();
        shadowShader.dispose();
    }
}
