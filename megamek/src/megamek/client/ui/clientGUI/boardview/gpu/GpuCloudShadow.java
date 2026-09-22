/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Matrix4;

/** Borrowed cloud transmission, shared by every lit surface. GpuClouds owns the texture and updates the projection. */
final class GpuCloudShadow extends Attribute {
    static final long TYPE = register("boardCloudShadow");
    private static final String MAIN = "void main() {";
    final Texture texture;
    final Matrix4 projection;

    GpuCloudShadow(Texture texture, Matrix4 projection) {
        super(TYPE);
        this.texture = texture;
        this.projection = projection;
    }

    static String prefix(Renderable renderable, DefaultShader.Config config) {
        return DefaultShader.createPrefix(renderable, config)
              + (renderable.environment != null && renderable.environment.has(TYPE) ? "#define cloudShadowFlag\n" : "");
    }

    static void register(DefaultShader shader) {
        shader.register("u_cloudShadow", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                GpuCloudShadow cloud = attributes.get(GpuCloudShadow.class, TYPE);
                if (cloud != null) { target.set(id, cloud.texture); }
            }
        });
        shader.register("u_cloudProjection", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                GpuCloudShadow cloud = attributes.get(GpuCloudShadow.class, TYPE);
                if (cloud != null) { target.set(id, cloud.projection); }
            }
        });
    }

    static String vertex(String source) {
        source = insert(source, MAIN, "varying vec3 v_cloudPosition;\n" + MAIN);
        String position = "gl_Position = u_projViewTrans * pos;";
        return insert(source, position, position + "\nv_cloudPosition = pos.xyz;\n");
    }

    static String fragment(String source, boolean ground) {
        String declarations = Gdx.files.classpath("megamek/client/ui/clientGUI/boardview/gpu/cloud-shadow.glsl")
              .readString("UTF-8");
        source = insert(source, MAIN, declarations + "\n" + MAIN);
        if (ground) {
            return insert(source, "albedo *= ambient + direct;",
                  "#ifdef cloudShadowFlag\nfloat cloudLight = cloudTransmission(v_cloudPosition);\n"
                        + "direct *= cloudLight;\nsheen *= cloudLight;\n#endif\n"
                        + "albedo *= ambient + direct;");
        }
        int start = source.indexOf(MAIN) + MAIN.length();
        String body = source.substring(start).replace("v_lightDiffuse", "cloudDiffuse")
              .replace("v_lightSpecular", "cloudSpecular");
        // Ambient remains separate in libGDX's shadow-enabled scene shaders. Emissive materials are unaffected.
        return source.substring(0, start) + "\n#ifdef lightingFlag\n"
              + "float cloudLight = 1.0;\n#ifdef cloudShadowFlag\n"
              + "cloudLight = cloudTransmission(v_cloudPosition);\n#endif\n"
              + "vec3 cloudDiffuse = v_lightDiffuse * cloudLight;\n#ifdef specularFlag\n"
              + "vec3 cloudSpecular = v_lightSpecular * cloudLight;\n#endif\n#endif\n" + body;
    }

    private static String insert(String source, String anchor, String replacement) {
        int index = source.indexOf(anchor);
        if (index < 0 || source.indexOf(anchor, index + anchor.length()) >= 0) {
            throw new IllegalStateException("Incompatible cloud shader insertion point: " + anchor);
        }
        return source.substring(0, index) + replacement + source.substring(index + anchor.length());
    }

    @Override
    public Attribute copy() {
        return new GpuCloudShadow(texture, projection);
    }

    @Override
    public int compareTo(Attribute other) {
        if (type != other.type) { return Long.compare(type, other.type); }
        return Integer.compare(texture.getTextureObjectHandle(), ((GpuCloudShadow) other).texture.getTextureObjectHandle());
    }
}
