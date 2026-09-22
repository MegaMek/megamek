/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;

/** Interpolates authored liquid frames in place; the fallback displays each frame for its original duration. */
final class GpuLiquidShader {
    /** Set false and rebuild to use the original GIF frame timing without interpolation. */
    static final boolean USE_SHADER_ANIMATION = true;

    static final class Frame extends TextureAttribute {
        static final long TYPE = register("boardLiquidNextFrame");
        static final long BLEND = register("boardLiquidBlend");

        static { Mask |= TYPE; }

        Frame(Texture texture) { super(TYPE, texture); }
    }

    private GpuLiquidShader() { }

    static String fragment(String source) {
        String functions = Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/liquid-animation.glsl")
              .readString("UTF-8");
        return source.replace("texture2D(u_diffuseTexture, v_diffuseUV)", "liquidColor")
              .replace("texture2D(u_emissiveTexture, v_emissiveUV)", "liquidColor")
              .replace("void main() {", functions + "\nvoid main() {\nvec4 liquidColor = liquidSample(v_diffuseUV);\n");
    }

    static void register(DefaultShader shader) {
        shader.register("u_liquidNextFrame", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                TextureAttribute frame = attributes.get(TextureAttribute.class, Frame.TYPE);
                if (frame != null) { target.set(id, frame.textureDescription); }
            }
        });
        shader.register("u_liquidBlend", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                FloatAttribute blend = attributes.get(FloatAttribute.class, Frame.BLEND);
                if (blend != null) { target.set(id, blend.value); }
            }
        });
    }
}
