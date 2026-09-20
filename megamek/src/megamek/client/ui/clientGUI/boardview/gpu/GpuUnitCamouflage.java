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
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
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

    static final class Paint extends Attribute {
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
