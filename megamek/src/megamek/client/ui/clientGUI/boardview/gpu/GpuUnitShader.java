/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;

/** Unit paint and damage over libGDX's lighting/shadows. The ModelBatch owns the provider and its shaders. */
final class GpuUnitShader extends DefaultShader {
    private static final String SHADERS = "megamek/client/ui/clientGUI/boardview/gpu/";
    private static final String MAIN = "void main() {";

    private final int rotation = register("u_camoRotation");
    private final int camoEnabled = register("u_camoEnabled");
    private final int camoImageSize = register("u_camoImageSize");
    private final int paintTransform = register("u_paintTransform");
    private final int paintNormalMatrix = register("u_paintNormalMatrix");
    private final int markerTexture = register("u_markerTexture");
    private final int markerEnabled = register("u_markerEnabled");
    private final int damageTexture = register("u_damageTexture");
    private final int damageEnabled = register("u_damageEnabled");
    private final int damageTransform = register("u_damageTransform");

    private GpuUnitShader(Renderable renderable, Config config) {
        super(renderable, config);
    }

    static DefaultShaderProvider provider() {
        return new DefaultShaderProvider(vertexSource(getDefaultVertexShader()), fragmentSource(getDefaultFragmentShader())) {
            @Override
            protected Shader createShader(Renderable renderable) {
                return new GpuUnitShader(renderable, config);
            }
        };
    }

    // These four insertion points are the source contract with the pinned libGDX version.
    // Keep lighting/shadow code upstream, and fail explicitly if an upgrade changes this contract.
    static String vertexSource(String source) {
        String declarations = Gdx.files.classpath(SHADERS + "unit-material.vert").readString("UTF-8");
        source = replaceOnce(source, MAIN, declarations + "\n" + MAIN + "\n    unitMaterialCoordinates();\n", "vertex");
        return replaceOnce(source, "v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;",
              "v_diffuseUV = unitDiffuseUV();", "vertex");
    }

    static String fragmentSource(String source) {
        String declarations = Gdx.files.classpath(SHADERS + "unit-material.frag").readString("UTF-8");
        source = replaceOnce(source, MAIN, declarations + "\n" + MAIN, "fragment");
        String emissive = "#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)";
        return replaceOnce(source, emissive, "diffuse.rgb = unitOverlays(diffuse.rgb);\n" + emissive, "fragment");
    }

    private static String replaceOnce(String source, String anchor, String replacement, String stage) {
        int index = source.indexOf(anchor);
        if (index < 0 || source.indexOf(anchor, index + anchor.length()) >= 0) {
            throw new IllegalStateException("Incompatible libGDX unit " + stage
                  + " shader: expected exactly one insertion point: " + anchor);
        }
        return source.substring(0, index) + replacement + source.substring(index + anchor.length());
    }

    @Override
    public void render(Renderable part, Attributes attributes) {
        var paint = attributes.get(GpuUnitCamouflage.Paint.class, GpuUnitCamouflage.Paint.TYPE);
        var damage = attributes.get(UnitDamageDisplay.Overlay.class, UnitDamageDisplay.Overlay.TYPE);
        set(damageEnabled, damage == null ? 0f
              : part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX) ? 2f : 1f);
        if (damage != null) {
            set(damageTexture, context.textureBinder.bind(damage.texture));
            set(damageTransform, damage.cos, damage.sin, damage.offsetU, damage.offsetV);
        }
        set(rotation, paint == null ? 1 : paint.cos, paint == null ? 0 : paint.sin);
        var diffuse = attributes.get(TextureAttribute.class, TextureAttribute.Diffuse);
        boolean camo = paint != null && diffuse != null;
        set(camoEnabled, camo ? 1f : 0f);
        if (paint != null) {
            set(paintTransform, paint.transform);
            set(paintNormalMatrix, paint.normalMatrix);
        }
        if (camo) {
            var texture = diffuse.textureDescription.texture;
            set(camoImageSize, (float) texture.getWidth(), (float) texture.getHeight());
        }
        boolean marker = paint != null && paint.marker != null;
        set(markerEnabled, marker ? 1f : 0f);
        if (marker) {
            set(markerTexture, context.textureBinder.bind(paint.marker));
        }
        super.render(part, attributes);
    }
}
