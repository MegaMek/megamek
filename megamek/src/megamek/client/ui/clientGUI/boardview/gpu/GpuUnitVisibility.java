/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/** Occluded parts of visible units and opted-in markers. Owns GL resources; never changes scene materials or depth. */
final class GpuUnitVisibility implements Disposable {
    static final float DEFAULT_OUTLINE_INTENSITY = 0.55f;
    private final ShaderProgram shader;
    private final Mesh quad;
    private final ModelBatch colorBatch;
    private final Rectangle screenBounds = new Rectangle();
    private final Vector3 corner = new Vector3();
    private Texture unitDepth;
    private FrameBuffer unitColors;

    GpuUnitVisibility() {
        shader = GpuAtmosphere.shader("unit-visibility.frag");
        quad = GpuAtmosphere.screenQuad();
        DepthShader.Config config = new DepthShader.Config();
        config.defaultCullFace = GL20.GL_BACK;
        config.depthBufferOnly = true;
        config.fragmentShader = Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/unit-color.frag").readString();
        colorBatch = new ModelBatch(new DepthShaderProvider(config) {
            @Override
            protected Shader createShader(Renderable renderable) {
                return new DepthShader(renderable, this.config) {
                    private final int outlineColor = register("u_outlineColor");

                    @Override
                    public void render(Renderable part, Attributes attributes) {
                        set(outlineColor, part.userData instanceof Color color ? color : Color.LIGHT_GRAY);
                        super.render(part, attributes);
                    }
                };
            }
        }, new GpuOpaqueSorter());
    }

    void render(Camera camera, List<ModelInstance> units, Texture sceneDepth, int bottom, float intensity, float scale) {
        render(camera, units, sceneDepth, bottom, intensity, scale, null);
    }

    void render(Camera camera, List<ModelInstance> units, Texture sceneDepth, int bottom, float intensity, float scale,
          UnitBounds.Frame bounds) {
        if (intensity <= 0 || units.isEmpty()) {
            return;
        }
        screenBounds(camera, units, scale, bounds);
        if (screenBounds.width <= 0 || screenBounds.height <= 0) { return; }
        int width = sceneDepth.getWidth(), height = sceneDepth.getHeight();
        if (unitDepth == null || unitDepth.getWidth() != width || unitDepth.getHeight() != height) {
            disposeBuffers();
            unitColors = GpuAtmosphere.buffer(width, height, false);
            try {
                unitDepth = GpuAtmosphere.attachDepthTexture(unitColors);
            } catch (RuntimeException failure) {
                disposeBuffers();
                throw failure;
            }
        }
        // One capture keeps depth and team color on the same nearest surface, with the depth shader's alpha cutouts.
        unitColors.begin();
        Gdx.gl.glDepthMask(true);
        ScreenUtils.clear(0, 0, 0, 0, true);
        colorBatch.begin(camera);
        units.forEach(unit -> GpuUnitInstance.renderDepth(colorBatch, unit));
        colorBatch.end();
        unitColors.end();

        HdpiUtils.glViewport(0, bottom, (int) camera.viewportWidth, (int) camera.viewportHeight);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor((int) screenBounds.x, bottom + (int) screenBounds.y,
              (int) screenBounds.width, (int) screenBounds.height);
        try {
            shader.bind();
            sceneDepth.bind(0);
            unitDepth.bind(1);
            unitColors.getColorBufferTexture().bind(2);
            shader.setUniformi("u_sceneDepth", 0);
            shader.setUniformi("u_unitDepth", 1);
            shader.setUniformi("u_unitColors", 2);
            // A small world-space tolerance avoids highlighting an exposed surface due to depth rounding.
            shader.setUniformf("u_bias", Math.max(0.0000005f, 0.05f * BoardGeometry.HEX_SCALE / (camera.far - camera.near)));
            shader.setUniformf("u_step", 1.5f * scale / camera.viewportWidth, 1.5f * scale / camera.viewportHeight);
            shader.setUniformf("u_intensity", MathUtils.clamp(intensity, 0, 1));
            quad.render(shader, GL20.GL_TRIANGLES);
        } finally {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            Gdx.gl.glDisable(GL20.GL_BLEND);
            Gdx.gl.glDepthMask(true);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        }
    }

    /** Project a conservative union, including the two-sample halo and rounding on HiDPI displays. */
    private void screenBounds(Camera camera, List<ModelInstance> units, float scale, UnitBounds.Frame bounds) {
        screenBounds.set(0, 0, camera.viewportWidth, camera.viewportHeight);
        if (!(camera instanceof OrthographicCamera)) { return; }
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (ModelInstance unit : units) {
            var box = bounds == null ? UnitBounds.world(unit) : bounds.get(unit);
            for (int i = 0; i < 8; i++) {
                corner.set((i & 1) == 0 ? box.min.x : box.max.x, (i & 2) == 0 ? box.min.y : box.max.y,
                      (i & 4) == 0 ? box.min.z : box.max.z).prj(camera.combined);
                float x = (corner.x + 1) * camera.viewportWidth / 2;
                float y = (corner.y + 1) * camera.viewportHeight / 2;
                minX = Math.min(minX, x); minY = Math.min(minY, y);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            }
        }
        float margin = (float) Math.ceil(3 * scale) + 2;
        minX = Math.max(0, (float) Math.floor(minX - margin));
        minY = Math.max(0, (float) Math.floor(minY - margin));
        maxX = Math.min(camera.viewportWidth, (float) Math.ceil(maxX + margin));
        maxY = Math.min(camera.viewportHeight, (float) Math.ceil(maxY + margin));
        screenBounds.set(minX, minY, maxX - minX, maxY - minY);
    }

    private void disposeBuffers() {
        if (unitDepth != null) { unitDepth.dispose(); unitDepth = null; }
        if (unitColors != null) { unitColors.dispose(); unitColors = null; }
    }

    @Override
    public void dispose() {
        disposeBuffers();
        colorBatch.dispose();
        quad.dispose();
        shader.dispose();
    }
}
