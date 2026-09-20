/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;

/** View-owned textures and per-instance paint. Rigid rest-space coordinates keep patterns attached during animation. */
final class GpuUnitCamouflage implements Disposable {
    private record ImageKey(BoardScene.Pixels pixels, boolean repeat) { }

    private final Map<ImageKey, Texture> textures = new HashMap<>();

    /** Applied to a fresh rest instance after damage; subsequent posing never changes UVs or material parameters. */
    void apply(ModelInstance instance, ModelInstance rest, UnitModelState.Appearance appearance) {
        var bounds = UnitBounds.local(rest);
        for (Node root : instance.nodes) {
            var effective = appearance;
            BoundingBox area = bounds;
            for (var member : appearance.fighters().entrySet()) {
                if (root.id.equals("fighter-" + member.getKey())) {
                    effective = member.getValue();
                    area = rest.getNode(root.id).extendBoundingBox(new BoundingBox(), true);
                    break;
                }
            }
            var normalization = new Matrix4().setToScaling(1 / Math.max(1, area.getWidth()),
                  -1 / Math.max(1, area.getHeight()), 1).translate(-area.min.x, -area.max.y, 0);
            paint(root, effective.camo(), normalization);
        }
    }

    private void paint(Node node, UnitModelState.Camo camo, Matrix4 normalization) {
        Color color = new Color(Color.WHITE);
        if (camo != null) {
            Color.rgb888ToColor(color, camo.rgb());
        }
        for (var part : node.parts) {
            if (!"paint".equals(part.material.id)) {
                continue;
            }
            var material = part.material.copy();
            material.set(ColorAttribute.createDiffuse(color));
            if (camo != null && camo.image() != null
                  && part.meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates) != null) {
                var diffuse = TextureAttribute.createDiffuse(texture(camo.image(), true));
                float density = 10f / Math.max(1, camo.scale());
                diffuse.scaleU = density;
                diffuse.scaleV = density;
                diffuse.offsetU = diffuse.offsetV = (1 - density) * .5f;
                material.set(diffuse);
            }
            Texture marker = camo == null || camo.marker() == null || camo.marker().image() == null
                  ? null : texture(camo.marker().image(), false);
            material.set(new Paint(camo == null ? 0 : camo.rotation(), marker,
                  marker == null ? new Matrix4() : new Matrix4(normalization).mul(node.globalTransform),
                  node.globalTransform.getScale(new Vector3())));
            part.material = material;
        }
        node.getChildren().forEach(child -> paint(child, camo, normalization));
    }

    private Texture texture(BoardScene.Pixels pixels, boolean repeat) {
        return textures.computeIfAbsent(new ImageKey(pixels, repeat), key -> {
            var pixmap = new Pixmap(pixels.width(), pixels.height(), Pixmap.Format.RGBA8888);
            try {
                pixmap.setBlending(Pixmap.Blending.None);
                for (int y = 0; y < pixels.height(); y++) {
                    for (int x = 0; x < pixels.width(); x++) {
                        pixmap.drawPixel(x, y, pixels.rgba(y * pixels.width() + x));
                    }
                }
                Texture texture = new Texture(pixmap, true);
                var wrap = repeat ? Texture.TextureWrap.MirroredRepeat : Texture.TextureWrap.ClampToEdge;
                texture.setWrap(wrap, wrap);
                texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                return texture;
            } finally {
                pixmap.dispose();
            }
        });
    }

    void retain(List<BoardScene.Unit> units) {
        Set<ImageKey> used = new HashSet<>();
        for (var unit : units) {
            if (!unit.sensorContact() && unit.model() != null && unit.model().state() != null) {
                collect(unit.model().state().appearance(), used);
            }
        }
        textures.entrySet().removeIf(entry -> {
            if (used.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().dispose();
            return true;
        });
    }

    private static void collect(UnitModelState.Appearance appearance, Set<ImageKey> used) {
        var camo = appearance.camo();
        if (camo != null) {
            if (camo.image() != null) {
                used.add(new ImageKey(camo.image(), true));
            }
            if (camo.marker() != null && camo.marker().image() != null) {
                used.add(new ImageKey(camo.marker().image(), false));
            }
        }
        appearance.fighters().values().forEach(member -> collect(member, used));
    }

    int textureCount() {
        return textures.size();
    }

    @Override
    public void dispose() {
        textures.values().forEach(Texture::dispose);
        textures.clear();
    }

    /** Standard libGDX lighting/shadows, with only diffuse UV rotation and the existing asset marker added. */
    static DefaultShaderProvider shaders() {
        String vertex = DefaultShader.getDefaultVertexShader().replace("void main() {", """
              uniform vec2 u_camoRotation;
              uniform vec3 u_camoRestScale;
              uniform mat4 u_markerTransform;
              varying vec2 v_markerUV;
              varying vec2 v_damageUV;
              void main() {
                  v_markerUV = (u_markerTransform * vec4(a_position, 1.0)).xy;
                  // Rest-space projection stays fixed to each rigid part, including unpainted metal.
                  v_damageUV = a_position.xy * 0.04;
                  #ifdef normalFlag
                      vec3 damageNormal = abs(a_normal);
                      if (damageNormal.x > damageNormal.y && damageNormal.x > damageNormal.z) {
                          v_damageUV = a_position.yz * 0.04;
                      } else if (damageNormal.y > damageNormal.z) {
                          v_damageUV = a_position.xz * 0.04;
                      }
                  #endif
              """).replace("v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;", """
              vec2 restScale = vec2(1.0);
              #ifdef normalFlag
                  // Match the exporter's two least-normal axes, including its X/Y/Z tie order.
                  vec3 n = abs(a_normal);
                  if (n.x <= n.y && n.x <= n.z) {
                      restScale = vec2(u_camoRestScale.x, n.y <= n.z ? u_camoRestScale.y : u_camoRestScale.z);
                  } else if (n.y <= n.z) {
                      restScale = vec2(u_camoRestScale.y, n.x <= n.z ? u_camoRestScale.x : u_camoRestScale.z);
                  } else {
                      restScale = vec2(u_camoRestScale.z, n.x <= n.y ? u_camoRestScale.x : u_camoRestScale.y);
                  }
              #endif
              vec2 centeredUV = a_texCoord0 * restScale - vec2(0.5);
              vec2 rotatedUV = vec2(u_camoRotation.x * centeredUV.x - u_camoRotation.y * centeredUV.y,
                                   u_camoRotation.y * centeredUV.x + u_camoRotation.x * centeredUV.y);
              v_diffuseUV = u_diffuseUVTransform.xy + (rotatedUV + vec2(0.5)) * u_diffuseUVTransform.zw;
              """);
        String fragment = DefaultShader.getDefaultFragmentShader().replace("void main() {", """
              uniform sampler2D u_markerTexture;
              uniform float u_markerEnabled;
              varying vec2 v_markerUV;
              uniform sampler2D u_damageTexture;
              uniform float u_damageEnabled;
              varying vec2 v_damageUV;
              void main() {
              """).replace("#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)", """
              if (u_markerEnabled > 0.5) {
                  vec4 marker = texture2D(u_markerTexture, v_markerUV);
                  diffuse.rgb = mix(diffuse.rgb, marker.rgb, marker.a);
              }
              if (u_damageEnabled > 0.5) {
                  vec4 damage = texture2D(u_damageTexture, v_damageUV);
                  diffuse.rgb = mix(diffuse.rgb, damage.rgb, damage.a);
              }
              #if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
              """);
        return new DefaultShaderProvider(vertex, fragment) {
            @Override
            protected Shader createShader(Renderable renderable) {
                return new DefaultShader(renderable, config) {
                    private final int rotation = register("u_camoRotation");
                    private final int restScale = register("u_camoRestScale");
                    private final int markerTransform = register("u_markerTransform");
                    private final int markerTexture = register("u_markerTexture");
                    private final int markerEnabled = register("u_markerEnabled");
                    private final int damageTexture = register("u_damageTexture");
                    private final int damageEnabled = register("u_damageEnabled");

                    @Override
                    public void render(Renderable part, Attributes attributes) {
                        Paint paint = attributes.get(Paint.class, Paint.TYPE);
                        var damage = attributes.get(UnitDamageDisplay.Overlay.class, UnitDamageDisplay.Overlay.TYPE);
                        set(damageEnabled, damage == null ? 0f : 1f);
                        if (damage != null) { set(damageTexture, context.textureBinder.bind(damage.texture)); }
                        set(rotation, paint == null ? 1 : paint.cos, paint == null ? 0 : paint.sin);
                        set(restScale, paint == null ? 1 : paint.scale.x, paint == null ? 1 : paint.scale.y,
                              paint == null ? 1 : paint.scale.z);
                        boolean marker = paint != null && paint.marker != null;
                        set(markerEnabled, marker ? 1f : 0f);
                        if (marker) {
                            set(markerTransform, paint.transform);
                            set(markerTexture, context.textureBinder.bind(paint.marker));
                        }
                        super.render(part, attributes);
                    }
                };
            }
        };
    }

    private static final class Paint extends Attribute {
        static final long TYPE = register("unitCamouflage");
        final float cos, sin;
        final Texture marker;
        final Matrix4 transform;
        final Vector3 scale;

        Paint(float degrees, Texture marker, Matrix4 transform, Vector3 scale) {
            this(MathUtils.cosDeg(degrees), MathUtils.sinDeg(degrees), marker, transform, scale);
        }

        private Paint(float cos, float sin, Texture marker, Matrix4 transform, Vector3 scale) {
            super(TYPE);
            this.cos = cos;
            this.sin = sin;
            this.marker = marker;
            this.transform = new Matrix4(transform);
            this.scale = new Vector3(scale);
        }

        @Override
        public Attribute copy() {
            return new Paint(cos, sin, marker, transform, scale);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, cos, sin, marker, scale, Arrays.hashCode(transform.val));
        }

        @Override
        public int compareTo(Attribute other) {
            if (type != other.type) {
                return Long.compare(type, other.type);
            }
            Paint paint = (Paint) other;
            int comparison = Float.compare(cos, paint.cos);
            if (comparison == 0) {
                comparison = Float.compare(sin, paint.sin);
            }
            if (comparison == 0) {
                comparison = Float.compare(scale.x, paint.scale.x);
            }
            if (comparison == 0) {
                comparison = Float.compare(scale.y, paint.scale.y);
            }
            if (comparison == 0) {
                comparison = Float.compare(scale.z, paint.scale.z);
            }
            if (comparison == 0) {
                comparison = Integer.compare(marker == null ? 0 : marker.getTextureObjectHandle(),
                      paint.marker == null ? 0 : paint.marker.getTextureObjectHandle());
            }
            return comparison == 0 ? Arrays.compare(transform.val, paint.transform.val) : comparison;
        }
    }
}
