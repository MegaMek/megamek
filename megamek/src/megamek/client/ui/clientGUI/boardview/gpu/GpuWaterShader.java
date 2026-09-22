/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Vector3;

/** Water shading controls and immutable impact geometry, derived when a terrain chunk is rebuilt. */
final class GpuWaterShader extends Attribute {
    /** True replaces the authored GIF colors with procedural patterns; both modes retain all surface effects. */
    static final boolean USE_PROCEDURAL_WATER = false;
    static final float SURFACE_OPACITY = 0.48f;
    static final long TYPE = register("boardWaterSurface");

    record Impact(Vector3 center, Vector3 inward, float halfWidth, float radius) { }

    private final float[] material;
    private final float[] motion;
    private final Color palette;
    final List<Impact> impacts;

    GpuWaterShader(BoardScene scene, BoardSurface surface, boolean procedural, boolean falling, BoardFlow.Current current) {
        super(TYPE);
        BoardScene.Tile tile = surface.tile;
        int theme = switch (tile.liquid().theme()) { case "mars" -> 1; case "volcano" -> 2; default -> 0; };
        material = new float[] { Math.clamp(tile.waterDepth(), 0, 4), theme, tile.liquid().rapids(), procedural ? 1 : 0 };
        Color middle = new Color(0.200f, 0.404f, 0.475f, 1);
        palette = switch (theme) {
            case 1 -> new Color(0.792f, 0.576f, 0.212f, 1);
            case 2 -> new Color(0.455f, 0.220f, 0.255f, 1);
            default -> material[0] < 2 ? new Color(0.408f, 0.620f, 0.549f, 1).lerp(middle, material[0] / 2)
                  : middle.lerp(new Color(0.180f, 0.259f, 0.373f, 1), (material[0] - 2) / 2);
        };
        motion = new float[] { -current.u(), current.v() * BoardGeometry.HEIGHT / BoardGeometry.WIDTH,
              falling ? 1 : 0, BoardGeometry.waterZ(tile) / BoardGeometry.WIDTH };
        List<Impact> hits = new ArrayList<>();
        if (!falling) {
            int segments = surface.water.size() / 6;
            for (int edge = 0; edge < 6; edge++) {
                BoardScene.Tile upstream = scene.tile(tile.coords().translated(BoardGeometry.edgeDirection(edge)));
                if (upstream == null || upstream.frozen() || !tile.liquid().connects(upstream.liquid())
                      || upstream.elevation() <= tile.elevation()) { continue; }
                Vector3 a = surface.water.get(edge * segments), b = surface.water.get(((edge + 1) % 6) * segments);
                Vector3 center = new Vector3(a).lerp(b, 0.5f);
                Vector3 inward = new Vector3(a).sub(b).crs(Vector3.Z).nor();
                float radius = (0.12f + 0.05f * (float) Math.sqrt(Math.min(4, upstream.elevation() - tile.elevation())))
                      * BoardGeometry.WIDTH;
                hits.add(new Impact(center, inward, a.dst(b) / 2, radius));
            }
        }
        impacts = List.copyOf(hits);
        // A fall keeps its own surface height for the pattern's continuity across the lip; spray needs the pool's.
        if (!falling && impacts.isEmpty()) { motion[3] = 0; }
    }

    private GpuWaterShader(GpuWaterShader original) {
        super(TYPE);
        // These arrays and vectors are read-only after construction; copied materials share their snapshot.
        material = original.material;
        motion = original.motion;
        palette = original.palette;
        impacts = original.impacts;
    }

    static void register(DefaultShader shader) {
        shader.register("u_waterBedColor", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                ColorAttribute bed = attributes.get(ColorAttribute.class, ColorAttribute.Ambient);
                if (bed != null) { target.set(id, bed.color.r, bed.color.g, bed.color.b); }
            }
        });
        shader.register("u_waterPalette", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                var water = attributes.get(GpuWaterShader.class, TYPE);
                if (water != null) { target.set(id, water.palette.r, water.palette.g, water.palette.b); }
            }
        });
        shader.register("u_waterMaterial", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                var water = attributes.get(GpuWaterShader.class, TYPE);
                if (water != null) { target.set(id, SURFACE_OPACITY, water.material[1], water.material[2], water.material[3]); }
            }
        });
        shader.register("u_waterMotion", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                var water = attributes.get(GpuWaterShader.class, TYPE);
                if (water != null) { target.set(id, water.motion[0], water.motion[1], water.motion[2], water.motion[3]); }
            }
        });
        shader.register("u_splashCount", new BaseShader.LocalSetter() {
            @Override
            public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                var water = attributes.get(GpuWaterShader.class, TYPE);
                if (water != null) { target.set(id, water.impacts.size()); }
            }
        });
        for (int index = 0; index < 6; index++) {
            final int impactIndex = index;
            shader.register("u_splashEdges[" + index + "]", new BaseShader.LocalSetter() {
                @Override
                public void set(BaseShader target, int id, Renderable renderable, Attributes attributes) {
                    var water = attributes.get(GpuWaterShader.class, TYPE);
                    if (water != null && impactIndex < water.impacts.size()) {
                        Impact impact = water.impacts.get(impactIndex);
                        float scale = 1 / BoardGeometry.WIDTH;
                        target.set(id, impact.center().x * scale, impact.center().y * scale,
                              impact.inward().x * impact.radius() * scale, impact.inward().y * impact.radius() * scale);
                    }
                }
            });
        }
    }

    @Override
    public GpuWaterShader copy() { return new GpuWaterShader(this); }

    @Override
    public int compareTo(Attribute other) {
        if (type != other.type) { return Long.compare(type, other.type); }
        GpuWaterShader water = (GpuWaterShader) other;
        int comparison = Arrays.compare(material, water.material);
        if (comparison == 0) { comparison = Arrays.compare(motion, water.motion); }
        if (comparison == 0) { comparison = Integer.compare(impacts.size(), water.impacts.size()); }
        for (int i = 0; comparison == 0 && i < impacts.size(); i++) {
            Impact a = impacts.get(i), b = water.impacts.get(i);
            comparison = Float.compare(a.center().x, b.center().x);
            if (comparison == 0) { comparison = Float.compare(a.center().y, b.center().y); }
            if (comparison == 0) { comparison = Float.compare(a.inward().x, b.inward().x); }
            if (comparison == 0) { comparison = Float.compare(a.inward().y, b.inward().y); }
            if (comparison == 0) { comparison = Float.compare(a.halfWidth(), b.halfWidth()); }
            if (comparison == 0) { comparison = Float.compare(a.radius(), b.radius()); }
        }
        return comparison;
    }
}
