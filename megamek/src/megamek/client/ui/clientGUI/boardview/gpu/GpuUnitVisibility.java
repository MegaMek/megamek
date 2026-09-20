/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/** Occluded parts of visible units and opted-in markers. Owns GL resources; never changes scene materials or depth. */
final class GpuUnitVisibility implements Disposable {
    static final float DEFAULT_OUTLINE_INTENSITY = 0.55f;
    private final ShaderProgram shader;
    private final Mesh quad;
    private final ModelBatch depthBatch;
    private final ModelBatch colorBatch;
    private FrameBuffer unitDepth;
    private FrameBuffer unitColors;

    GpuUnitVisibility() {
        shader = GpuAtmosphere.shader("unit-visibility.frag");
        quad = GpuAtmosphere.screenQuad();
        DepthShader.Config config = new DepthShader.Config();
        config.defaultCullFace = GL20.GL_BACK;
        depthBatch = new ModelBatch(new DepthShaderProvider(config), new GpuOpaqueSorter());
        colorBatch = new ModelBatch(new DefaultShaderProvider(DefaultShader.getDefaultVertexShader(),
              Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/unit-color.frag").readString()) {
            @Override
            protected Shader createShader(Renderable renderable) {
                return new DefaultShader(renderable, this.config) {
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
        if (intensity <= 0 || units.isEmpty()) {
            return;
        }
        int width = sceneDepth.getWidth(), height = sceneDepth.getHeight();
        if (unitDepth == null || unitDepth.getWidth() != width || unitDepth.getHeight() != height) {
            if (unitDepth != null) {
                unitDepth.dispose();
                unitColors.dispose();
            }
            unitDepth = GpuAtmosphere.buffer(width, height, true);
            unitColors = GpuAtmosphere.buffer(width, height, true);
        }
        unitDepth.begin();
        Gdx.gl.glDepthMask(true);
        ScreenUtils.clear(1, 1, 1, 1, true);
        // The nearest unit surface prevents rear faces and overlapping limbs from highlighting an exposed unit.
        depthBatch.begin(camera);
        units.forEach(unit -> GpuUnitInstance.renderDepth(depthBatch, unit));
        depthBatch.end();
        unitDepth.end();

        // Keep the nearest unit's color at every pixel, including where different teams overlap.
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
        try {
            shader.bind();
            sceneDepth.bind(0);
            unitDepth.getColorBufferTexture().bind(1);
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
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            Gdx.gl.glDisable(GL20.GL_BLEND);
            Gdx.gl.glDepthMask(true);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        }
    }

    @Override
    public void dispose() {
        if (unitDepth != null) {
            unitDepth.dispose();
            unitColors.dispose();
        }
        depthBatch.dispose();
        colorBatch.dispose();
        quad.dispose();
        shader.dispose();
    }
}
